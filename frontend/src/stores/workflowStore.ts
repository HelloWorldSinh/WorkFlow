import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { workflowApi } from '@/services/workflowApi'
import type {
  WorkflowItem,
  WorkflowFilter,
  WorkflowMetrics,
  WorkflowTemplate,
  WorkflowVersionLog,
  WorkflowModule,
  WorkflowType,
  WorkflowResponseDTO,
} from '@/types/workflow'

export const useWorkflowStore = defineStore('workflow', () => {
  // State
  const workflows = ref<WorkflowItem[]>([])
  const isLoading = ref<boolean>(false)
  const isSubmitting = ref<boolean>(false)

  // Pagination State
  const currentPage = ref<number>(0)
  const pageSize = ref<number>(10)
  const totalElements = ref<number>(0)
  const totalPages = ref<number>(0)

  // Filters State
  const filter = ref<WorkflowFilter>({
    search: '',
    status: 'all',
    module: 'all',
    sortBy: 'createdAt',
    sortOrder: 'desc',
    includeDeleted: false,
  })

  // Selected state for modals & drawers
  const selectedWorkflow = ref<WorkflowItem | null>(null)
  const isHistoryDrawerOpen = ref(false)
  const isCreateModalOpen = ref(false)
  const isDeleteModalOpen = ref(false)
  const workflowToDelete = ref<WorkflowItem | null>(null)
  const toastMessage = ref<{ text: string; type: 'success' | 'error' | 'info' } | null>(null)

  // Template Library
  const templates = ref<WorkflowTemplate[]>([
    {
      id: 'tpl-1',
      title: 'Phê duyệt Yêu cầu Mua sắm (Tiêu chuẩn)',
      description: 'Quy trình 3 cấp: Trưởng bộ phận -> Kế toán trưởng -> Giám đốc ký duyệt',
      module: 'Procurement',
      estimatedSteps: 4,
      type: 'multi_approval',
      icon: '🛒',
    },
    {
      id: 'tpl-2',
      title: 'Đơn xin Nghỉ phép & Nghỉ bù',
      description: 'Quy trình xét duyệt nghỉ phép theo cấp bậc quản lý trực tiếp (Dynamic Approver)',
      module: 'HR',
      estimatedSteps: 3,
      type: 'single_approval',
      icon: '🏖️',
    },
    {
      id: 'tpl-3',
      title: 'Thanh toán Hóa đơn & Hoàn ứng Chi phí',
      description: 'Tích hợp điều kiện kiểm tra hạn mức và thông báo qua Teams/Email',
      module: 'Finance',
      estimatedSteps: 5,
      type: 'multi_approval',
      icon: '💳',
    },
    {
      id: 'tpl-4',
      title: 'Cấp phát Tài khoản & Thiết bị Nhân viên mới',
      description: 'Tự động gửi webhook tạo email và tạo task bàn giao laptop',
      module: 'IT',
      estimatedSteps: 6,
      type: 'automation',
      icon: '💻',
    },
  ])

  // Version Logs Mock
  const versionLogs = ref<Record<string, WorkflowVersionLog[]>>({
    '1': [
      {
        version: 'v1.0',
        updatedBy: 'Admin',
        updatedAt: '2026-08-24 10:00',
        isCurrent: true,
        changes: ['Khởi tạo phiên bản quy trình ban đầu'],
      },
    ],
  })

  // Toast Helper
  function showToast(text: string, type: 'success' | 'error' | 'info' = 'success') {
    toastMessage.value = { text, type }
    setTimeout(() => {
      toastMessage.value = null
    }, 4000)
  }

  // Mapper from Backend DTO to Frontend Item
  function mapDtoToWorkflowItem(dto: WorkflowResponseDTO): WorkflowItem {
    const statusNormalized = dto.status.toLowerCase() as any
    const code = `WF-${String(dto.id).padStart(3, '0')}`

    return {
      id: dto.id,
      code,
      name: dto.name,
      description: dto.description || 'Chưa có mô tả chi tiết',
      module: 'HR',
      type: 'multi_approval',
      version: 'v1.0',
      owner: {
        id: dto.owner?.id || 1,
        name: dto.owner?.fullName || dto.owner?.email || 'Hệ thống',
        email: dto.owner?.email || 'admin@company.com',
        avatar: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=120&h=120&q=80',
        role: 'Workflow Owner',
      },
      status: statusNormalized,
      activeInstances: dto.activeInstances || 0,
      totalInstances: dto.totalInstances || 0,
      totalSteps: 4,
      createdAt: dto.createdAt || new Date().toISOString().slice(0, 16).replace('T', ' '),
      updatedAt: dto.createdAt || new Date().toISOString().slice(0, 16).replace('T', ' '),
      deletedAt: dto.deletedAt,
    }
  }

  // Getters & Metrics
  const metrics = computed<WorkflowMetrics>(() => {
    const list = workflows.value.filter((w) => w.status !== 'deleted')
    return {
      total: totalElements.value || list.length,
      published: list.filter((w) => w.status === 'published').length,
      draft: list.filter((w) => w.status === 'draft').length,
      suspended: list.filter((w) => w.status === 'suspended').length,
    }
  })

  const filteredWorkflows = computed(() => {
    // Return workflows loaded from backend
    return workflows.value
  })

  // API Actions
  async function fetchWorkflows() {
    isLoading.value = true
    try {
      const response = await workflowApi.getWorkflows({
        keyword: filter.value.search.trim() || undefined,
        status: filter.value.status !== 'all' ? filter.value.status : undefined,
        includeDeleted: filter.value.includeDeleted,
        page: currentPage.value,
        size: pageSize.value,
        sort: `${filter.value.sortBy},${filter.value.sortOrder}`,
      })

      if (response.success && response.data) {
        workflows.value = response.data.items.map(mapDtoToWorkflowItem)
        totalElements.value = response.data.totalElements
        totalPages.value = response.data.totalPages
        currentPage.value = response.data.pageNumber
      }
    } catch (err: any) {
      console.error('Lỗi khi tải danh sách workflow từ backend:', err)
      showToast(err?.message || 'Không thể kết nối đến máy chủ backend', 'error')
    } finally {
      isLoading.value = false
    }
  }

  async function createWorkflow(payload: {
    name: string
    description: string
    module?: WorkflowModule
    type?: WorkflowType
    templateId?: string
    ownerId?: number
  }) {
    isSubmitting.value = true
    try {
      // Lấy user ID từ payload hoặc user đang đăng nhập
      let finalOwnerId = payload.ownerId
      if (!finalOwnerId) {
        try {
          const authUserStr = localStorage.getItem('auth_user')
          if (authUserStr) {
            const authUser = JSON.parse(authUserStr)
            if (authUser?.id) finalOwnerId = authUser.id
          }
        } catch (e) {
          // ignore
        }
      }
      if (!finalOwnerId) finalOwnerId = 1

      const response = await workflowApi.createWorkflow({
        name: payload.name.trim(),
        description: payload.description?.trim(),
        ownerId: finalOwnerId,
        status: 'Draft',
      })

      if (response.success) {
        showToast(response.message || `Đã tạo mới workflow "${payload.name}" thành công!`, 'success')
        isCreateModalOpen.value = false
        await fetchWorkflows()
        return response.data
      }
    } catch (err: any) {
      console.error('Lỗi khi tạo workflow:', err)
      showToast(err?.message || 'Tạo workflow thất bại', 'error')
      throw err
    } finally {
      isSubmitting.value = false
    }
  }

  async function toggleStatus(workflow: WorkflowItem) {
    let nextStatus: 'Draft' | 'Published' | 'Suspended' | 'Deleted' = 'Published'

    if (workflow.status === 'published') {
      nextStatus = 'Suspended'
    } else if (workflow.status === 'suspended') {
      nextStatus = 'Published'
    } else if (workflow.status === 'draft') {
      nextStatus = 'Published'
    }

    try {
      const response = await workflowApi.updateWorkflow(workflow.id, {
        name: workflow.name,
        description: workflow.description,
        status: nextStatus,
      })

      if (response.success) {
        workflow.status = nextStatus.toLowerCase() as any
        showToast(`Đã chuyển trạng thái workflow thành "${nextStatus}"`, 'success')
      }
    } catch (err: any) {
      console.error('Lỗi khi cập nhật trạng thái workflow:', err)
      showToast(err?.message || 'Cập nhật trạng thái thất bại', 'error')
    }
  }

  function confirmDelete(workflow: WorkflowItem) {
    workflowToDelete.value = workflow
    isDeleteModalOpen.value = true
  }

  async function executeDelete() {
    if (!workflowToDelete.value) return

    const targetId = workflowToDelete.value.id
    isSubmitting.value = true

    try {
      const response = await workflowApi.deleteWorkflow(targetId)

      if (response.success) {
        const deleteType = response.data?.deleteType
        const msg = response.data?.message || 'Xóa workflow thành công!'

        showToast(msg, deleteType === 'HARD_DELETE' ? 'success' : 'info')
        isDeleteModalOpen.value = false
        workflowToDelete.value = null
        await fetchWorkflows()
      }
    } catch (err: any) {
      console.error('Lỗi khi xóa workflow:', err)
      showToast(err?.message || 'Không thể xóa workflow', 'error')
    } finally {
      isSubmitting.value = false
    }
  }

  function openVersionHistory(workflow: WorkflowItem) {
    selectedWorkflow.value = workflow
    isHistoryDrawerOpen.value = true
  }

  // Pagination Helpers
  function setPage(page: number) {
    if (page >= 0 && page < totalPages.value) {
      currentPage.value = page
      fetchWorkflows()
    }
  }

  function nextPage() {
    if (currentPage.value < totalPages.value - 1) {
      setPage(currentPage.value + 1)
    }
  }

  function prevPage() {
    if (currentPage.value > 0) {
      setPage(currentPage.value - 1)
    }
  }

  // Auto reload when search or status filter changes (with debounce on search)
  let searchTimeout: any = null
  watch(
    () => filter.value.search,
    () => {
      clearTimeout(searchTimeout)
      searchTimeout = setTimeout(() => {
        currentPage.value = 0
        fetchWorkflows()
      }, 350)
    }
  )

  watch(
    () => [filter.value.status, filter.value.includeDeleted, filter.value.sortBy, filter.value.sortOrder],
    () => {
      currentPage.value = 0
      fetchWorkflows()
    }
  )

  async function restoreWorkflow(workflow: WorkflowItem) {
    try {
      const response = await workflowApi.restoreWorkflow(workflow.id)
      if (response.success) {
        showToast(`Đã khôi phục thành công workflow "${workflow.name}"`, 'success')
        await fetchWorkflows()
      }
    } catch (err: any) {
      console.error('Lỗi khi khôi phục workflow:', err)
      showToast(err?.message || 'Khôi phục workflow thất bại', 'error')
    }
  }

  return {
    workflows,
    templates,
    versionLogs,
    filter,
    selectedWorkflow,
    isHistoryDrawerOpen,
    isCreateModalOpen,
    isDeleteModalOpen,
    workflowToDelete,
    toastMessage,
    isLoading,
    isSubmitting,
    currentPage,
    pageSize,
    totalElements,
    totalPages,
    metrics,
    filteredWorkflows,
    showToast,
    fetchWorkflows,
    createWorkflow,
    toggleStatus,
    confirmDelete,
    executeDelete,
    restoreWorkflow,
    openVersionHistory,
    setPage,
    nextPage,
    prevPage,
  }
})
