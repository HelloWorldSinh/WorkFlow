import { request } from './apiClient'
import type { ApiResponse, PageResponse } from '@/types/workflow'
import type {
  TicketItem,
  TicketDetail,
  TaskSummary,
  TicketStats,
  CreateTicketRequest,
  TaskActionRequest,
} from '@/types/ticket'

export interface TicketQueryParams {
  keyword?: string
  status?: string
  workflowId?: number
  creatorId?: number
  page?: number
  size?: number
  sort?: string
}

export const ticketApi = {
  /**
   * Tạo mới 1 Ticket
   */
  createTicket(payload: CreateTicketRequest): Promise<ApiResponse<TicketItem>> {
    return request<TicketItem>('/tickets', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  /**
   * Lấy danh sách Ticket (phân trang, lọc, tìm kiếm)
   */
  getAllTickets(params?: TicketQueryParams): Promise<ApiResponse<PageResponse<TicketItem>>> {
    const searchParams = new URLSearchParams()
    if (params) {
      if (params.keyword) searchParams.append('keyword', params.keyword)
      if (params.status && params.status !== 'ALL') searchParams.append('status', params.status)
      if (params.workflowId) searchParams.append('workflowId', String(params.workflowId))
      if (params.creatorId) searchParams.append('creatorId', String(params.creatorId))
      if (params.page !== undefined) searchParams.append('page', String(params.page))
      if (params.size !== undefined) searchParams.append('size', String(params.size))
      if (params.sort) searchParams.append('sort', params.sort)
    }
    const query = searchParams.toString() ? `?${searchParams.toString()}` : ''
    return request<PageResponse<TicketItem>>(`/tickets${query}`, {
      method: 'GET',
    })
  },

  /**
   * Xem chi tiết 1 Ticket kèm toàn bộ tiến trình
   */
  getTicketById(id: number | string): Promise<ApiResponse<TicketDetail>> {
    return request<TicketDetail>(`/tickets/${id}`, {
      method: 'GET',
    })
  },

  /**
   * Xử lý phê duyệt / từ chối 1 nhiệm vụ
   */
  processTaskAction(taskId: number, payload: TaskActionRequest): Promise<ApiResponse<TicketDetail>> {
    return request<TicketDetail>(`/tickets/tasks/${taskId}/action`, {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  /**
   * Lấy danh sách các nhiệm vụ đang chờ người dùng hiện tại xử lý
   */
  getMyPendingTasks(userId?: number): Promise<ApiResponse<TaskSummary[]>> {
    const query = userId ? `?userId=${userId}` : ''
    return request<TaskSummary[]>(`/tickets/my-tasks${query}`, {
      method: 'GET',
    })
  },

  /**
   * Lấy thống kê số lượng Ticket và Tasks
   */
  getTicketStats(userId?: number): Promise<ApiResponse<TicketStats>> {
    const query = userId ? `?userId=${userId}` : ''
    return request<TicketStats>(`/tickets/stats${query}`, {
      method: 'GET',
    })
  },
}
