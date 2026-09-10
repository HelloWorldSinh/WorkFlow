import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type {
  TicketItem,
  TicketDetail,
  TaskSummary,
  TicketStats,
  TicketStatus,
  CreateTicketRequest,
  TaskActionRequest,
} from '@/types/ticket'
import { ticketApi } from '@/services/ticketApi'
import type { WorkflowResponseDTO } from '@/types/workflow'

export const useTicketStore = defineStore('ticket', () => {
  // Danh sách Tickets & Phân trang
  const tickets = ref<TicketItem[]>([])
  const totalElements = ref<number>(0)
  const totalPages = ref<number>(0)
  const currentPage = ref<number>(0)
  const pageSize = ref<number>(10)
  const isLoading = ref<boolean>(false)

  // Bộ lọc
  const filterKeyword = ref<string>('')
  const filterStatus = ref<TicketStatus | 'ALL'>('ALL')
  const filterWorkflowId = ref<number | null>(null)

  // Thống kê
  const stats = ref<TicketStats>({
    total: 0,
    running: 0,
    completed: 0,
    rejected: 0,
    pendingMyTasks: 0,
  })

  // Chi tiết 1 Ticket đang xem
  const currentTicket = ref<TicketDetail | null>(null)
  const isLoadingDetail = ref<boolean>(false)

  // Danh sách nhiệm vụ của tôi (My Tasks)
  const myTasks = ref<TaskSummary[]>([])
  const isLoadingTasks = ref<boolean>(false)

  // Modal Tạo Ticket
  const isCreateModalOpen = ref<boolean>(false)
  const createTargetWorkflow = ref<WorkflowResponseDTO | any | null>(null)
  const isSubmittingTicket = ref<boolean>(false)

  // Action đang xử lý
  const isProcessingAction = ref<boolean>(false)

  // Toast
  const toast = ref<{ text: string; type: 'success' | 'error' | 'info' } | null>(null)

  function showToast(text: string, type: 'success' | 'error' | 'info' = 'success') {
    toast.value = { text, type }
    setTimeout(() => {
      toast.value = null
    }, 3500)
  }

  // Current logged in user ID from localStorage
  const currentUserId = computed<number>(() => {
    try {
      const uStr = localStorage.getItem('auth_user')
      if (uStr) {
        const u = JSON.parse(uStr)
        if (u && u.id) return Number(u.id)
      }
    } catch (e) {
      // fallback
    }
    return 1
  })

  // ==========================================
  // ACTIONS
  // ==========================================

  async function fetchTickets(page = 0) {
    isLoading.value = true
    currentPage.value = page
    try {
      const res = await ticketApi.getAllTickets({
        keyword: filterKeyword.value.trim() || undefined,
        status: filterStatus.value !== 'ALL' ? filterStatus.value : undefined,
        workflowId: filterWorkflowId.value || undefined,
        page: currentPage.value,
        size: pageSize.value,
      })
      if (res.success && res.data) {
        tickets.value = res.data.items || []
        totalElements.value = res.data.totalElements || 0
        totalPages.value = res.data.totalPages || 0
      }
    } catch (err: any) {
      console.error('Lỗi tải danh sách tickets:', err)
      showToast(err?.message || 'Không thể tải danh sách ticket', 'error')
    } finally {
      isLoading.value = false
    }
  }

  async function fetchStats() {
    try {
      const res = await ticketApi.getTicketStats(currentUserId.value)
      if (res.success && res.data) {
        stats.value = res.data
      }
    } catch (err) {
      console.error('Lỗi tải thống kê ticket:', err)
    }
  }

  async function fetchTicketDetail(id: number | string) {
    isLoadingDetail.value = true
    try {
      const res = await ticketApi.getTicketById(id)
      if (res.success && res.data) {
        currentTicket.value = res.data
      }
    } catch (err: any) {
      console.error('Lỗi tải chi tiết ticket:', err)
      showToast(err?.message || 'Không tìm thấy thông tin ticket', 'error')
    } finally {
      isLoadingDetail.value = false
    }
  }

  async function fetchMyTasks() {
    isLoadingTasks.value = true
    try {
      const res = await ticketApi.getMyPendingTasks(currentUserId.value)
      if (res.success && res.data) {
        myTasks.value = res.data
      }
    } catch (err: any) {
      console.error('Lỗi tải danh sách My Tasks:', err)
      showToast(err?.message || 'Không thể tải danh sách nhiệm vụ', 'error')
    } finally {
      isLoadingTasks.value = false
    }
  }

  async function createTicket(workflowId: number, formData: Record<string, any>, title?: string) {
    isSubmittingTicket.value = true
    try {
      const payload: CreateTicketRequest = {
        workflowId,
        title,
        creatorId: currentUserId.value,
        formData,
      }
      const res = await ticketApi.createTicket(payload)
      if (res.success && res.data) {
        showToast(`Đã khởi tạo yêu cầu "${res.data.requestCode}" thành công!`, 'success')
        isCreateModalOpen.value = false
        // Cập nhật lại danh sách và thống kê
        fetchTickets(0)
        fetchStats()
        return res.data
      }
    } catch (err: any) {
      console.error('Lỗi tạo ticket:', err)
      showToast(err?.message || 'Không thể khởi tạo ticket', 'error')
      throw err
    } finally {
      isSubmittingTicket.value = false
    }
  }

  async function processTask(taskId: number, action: 'APPROVE' | 'REJECT', comment?: string) {
    isProcessingAction.value = true
    try {
      const payload: TaskActionRequest = {
        action,
        comment,
        userId: currentUserId.value,
      }
      const res = await ticketApi.processTaskAction(taskId, payload)
      if (res.success && res.data) {
        const actionLabel = action === 'APPROVE' ? 'Phê duyệt' : 'Từ chối'
        showToast(`Đã ${actionLabel} nhiệm vụ thành công!`, 'success')
        currentTicket.value = res.data
        // Cập nhật lại My Tasks và thống kê
        fetchMyTasks()
        fetchStats()
        return res.data
      }
    } catch (err: any) {
      console.error('Lỗi xử lý nhiệm vụ:', err)
      showToast(err?.message || 'Lỗi khi xử lý nhiệm vụ', 'error')
      throw err
    } finally {
      isProcessingAction.value = false
    }
  }

  function openCreateModal(workflow: any) {
    createTargetWorkflow.value = workflow
    isCreateModalOpen.value = true
  }

  function closeCreateModal() {
    isCreateModalOpen.value = false
    createTargetWorkflow.value = null
  }

  function setStatusFilter(status: TicketStatus | 'ALL') {
    filterStatus.value = status
    fetchTickets(0)
  }

  return {
    // State
    tickets,
    totalElements,
    totalPages,
    currentPage,
    pageSize,
    isLoading,
    filterKeyword,
    filterStatus,
    filterWorkflowId,
    stats,
    currentTicket,
    isLoadingDetail,
    myTasks,
    isLoadingTasks,
    isCreateModalOpen,
    createTargetWorkflow,
    isSubmittingTicket,
    isProcessingAction,
    toast,
    currentUserId,

    // Actions
    fetchTickets,
    fetchStats,
    fetchTicketDetail,
    fetchMyTasks,
    createTicket,
    processTask,
    openCreateModal,
    closeCreateModal,
    setStatusFilter,
    showToast,
  }
})
