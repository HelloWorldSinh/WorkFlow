import { request } from './apiClient'
import type { ApiResponse } from '@/types/workflow'
import type { FormSchema } from '@/types/form'

export interface CreateFormPayload {
  name: string
  description?: string
  schema: FormSchema
  createdById?: number
}

export interface UpdateFormPayload {
  name: string
  description?: string
  schema: FormSchema
  isActive?: boolean
}

export interface FormBackendResponseDTO {
  id: number
  name: string
  description: string
  schema: FormSchema
  createdById?: number
  createdByName?: string
  createdAt?: string
  isActive: boolean
  usageCount?: number
}

export const formApi = {
  /**
   * Lấy danh sách toàn bộ biểu mẫu từ Backend (hỗ trợ tìm kiếm theo từ khóa)
   */
  async getAllForms(keyword?: string): Promise<ApiResponse<FormBackendResponseDTO[]>> {
    const query = keyword && keyword.trim() ? `?keyword=${encodeURIComponent(keyword.trim())}` : ''
    return request<FormBackendResponseDTO[]>(`/forms${query}`, {
      method: 'GET',
    })
  },

  /**
   * Xem chi tiết 1 biểu mẫu theo ID
   */
  async getFormById(id: number): Promise<ApiResponse<FormBackendResponseDTO>> {
    return request<FormBackendResponseDTO>(`/forms/${id}`, {
      method: 'GET',
    })
  },

  /**
   * Tạo mới một biểu mẫu trên Backend
   */
  async createForm(payload: CreateFormPayload): Promise<ApiResponse<FormBackendResponseDTO>> {
    return request<FormBackendResponseDTO>('/forms', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  /**
   * Cập nhật biểu mẫu theo ID
   */
  async updateForm(id: number, payload: UpdateFormPayload): Promise<ApiResponse<FormBackendResponseDTO>> {
    return request<FormBackendResponseDTO>(`/forms/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload),
    })
  },

  /**
   * Xóa biểu mẫu (xóa mềm trên Backend)
   */
  async deleteForm(id: number): Promise<ApiResponse<void>> {
    return request<void>(`/forms/${id}`, {
      method: 'DELETE',
    })
  },
}
