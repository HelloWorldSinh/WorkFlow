import { request } from './apiClient'
import type {
  ApiResponse,
  PageResponse,
  WorkflowResponseDTO,
  CreateWorkflowRequest,
  UpdateWorkflowRequest,
  DeleteWorkflowResponseDTO,
  SaveWorkflowGraphRequest,
  WorkflowGraphResponseDTO,
} from '@/types/workflow'

export interface WorkflowQueryParams {
  keyword?: string
  status?: string
  ownerId?: number
  includeDeleted?: boolean
  page?: number
  size?: number
  sort?: string
}

export interface LoginPayload {
  email: string
  password: string
}

export interface UserProfileResponse {
  id: number
  email: string
  fullName: string
  role: string
  departmentId: number
  token?: string
}

export const authApi = {
  /**
   * API Đăng nhập
   */
  async login(payload: LoginPayload): Promise<ApiResponse<UserProfileResponse>> {
    return request<UserProfileResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  /**
   * Lấy thông tin user
   */
  async getProfile(userId: number): Promise<ApiResponse<UserProfileResponse>> {
    return request<UserProfileResponse>(`/auth/profile/${userId}`, {
      method: 'GET',
    })
  },
}

export interface UserSummary {
  id: number
  email: string
  fullName?: string
  departmentId?: number
  role?: string
  isActive?: boolean
}

export const userApi = {
  /**
   * Tìm kiếm người dùng theo keyword và roles
   */
  async searchUsers(keyword: string = '', roles?: string[]): Promise<ApiResponse<UserSummary[]>> {
    const query = new URLSearchParams()
    if (keyword && keyword.trim()) {
      query.append('keyword', keyword.trim())
    }
    if (roles && roles.length > 0) {
      query.append('roles', roles.join(','))
    }
    const queryString = query.toString()
    const endpoint = `/users/search${queryString ? `?${queryString}` : ''}`
    return request<UserSummary[]>(endpoint, { method: 'GET' })
  },

  /**
   * Lấy danh sách người dùng có vai trò phê duyệt (Admin, Approver, WorkflowOwner)
   */
  async getApprovers(keyword: string = ''): Promise<ApiResponse<UserSummary[]>> {
    const query = new URLSearchParams()
    if (keyword && keyword.trim()) {
      query.append('keyword', keyword.trim())
    }
    const queryString = query.toString()
    const endpoint = `/users/approvers${queryString ? `?${queryString}` : ''}`
    return request<UserSummary[]>(endpoint, { method: 'GET' })
  },

  /**
   * Lấy thông tin người dùng theo ID
   */
  async getUserById(id: number | string): Promise<ApiResponse<UserSummary>> {
    return request<UserSummary>(`/users/${id}`, { method: 'GET' })
  },
}

export const workflowApi = {
  /**
   * Lấy danh sách workflow có phân trang, tìm kiếm và lọc
   */
  async getWorkflows(params: WorkflowQueryParams = {}): Promise<ApiResponse<PageResponse<WorkflowResponseDTO>>> {
    const query = new URLSearchParams()

    if (params.keyword && params.keyword.trim()) {
      query.append('keyword', params.keyword.trim())
    }
    if (params.status && params.status !== 'all') {
      const normalizedStatus = params.status.charAt(0).toUpperCase() + params.status.slice(1).toLowerCase()
      query.append('status', normalizedStatus)
    }
    if (params.ownerId !== undefined && params.ownerId !== null) {
      query.append('ownerId', params.ownerId.toString())
    }
    if (params.includeDeleted !== undefined) {
      query.append('includeDeleted', String(params.includeDeleted))
    }
    if (params.page !== undefined) {
      query.append('page', params.page.toString())
    }
    if (params.size !== undefined) {
      query.append('size', params.size.toString())
    }
    if (params.sort) {
      query.append('sort', params.sort)
    }

    const queryString = query.toString()
    const endpoint = `/workflows${queryString ? `?${queryString}` : ''}`
    return request<PageResponse<WorkflowResponseDTO>>(endpoint, { method: 'GET' })
  },

  /**
   * Xem chi tiết một workflow theo ID
   */
  async getWorkflowById(id: number | string): Promise<ApiResponse<WorkflowResponseDTO>> {
    return request<WorkflowResponseDTO>(`/workflows/${id}`, { method: 'GET' })
  },

  /**
   * Tạo mới một workflow
   */
  async createWorkflow(payload: CreateWorkflowRequest): Promise<ApiResponse<WorkflowResponseDTO>> {
    return request<WorkflowResponseDTO>('/workflows', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  /**
   * Cập nhật thông tin workflow
   */
  async updateWorkflow(id: number | string, payload: UpdateWorkflowRequest): Promise<ApiResponse<WorkflowResponseDTO>> {
    return request<WorkflowResponseDTO>(`/workflows/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },

  /**
   * Xóa workflow (Tự động xử lý xóa mềm hoặc xóa cứng phía backend)
   */
  async deleteWorkflow(id: number | string): Promise<ApiResponse<DeleteWorkflowResponseDTO>> {
    return request<DeleteWorkflowResponseDTO>(`/workflows/${id}`, {
      method: 'DELETE',
    })
  },

  /**
   * Khôi phục workflow đã bị xóa mềm
   */
  async restoreWorkflow(id: number | string): Promise<ApiResponse<WorkflowResponseDTO>> {
    return request<WorkflowResponseDTO>(`/workflows/${id}/restore`, {
      method: 'POST',
    })
  },

  /**
   * Lấy dữ liệu sơ đồ quy trình (Graph) từ CSDL backend
   */
  async getWorkflowGraph(id: number | string): Promise<ApiResponse<WorkflowGraphResponseDTO>> {
    return request<WorkflowGraphResponseDTO>(`/workflows/${id}/graph`, {
      method: 'GET',
    })
  },

  /**
   * Lưu dữ liệu sơ đồ quy trình (Nodes, Edges, Config) xuống CSDL backend
   */
  async saveWorkflowGraph(id: number | string, payload: SaveWorkflowGraphRequest): Promise<ApiResponse<WorkflowGraphResponseDTO>> {
    return request<WorkflowGraphResponseDTO>(`/workflows/${id}/graph`, {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },
}
