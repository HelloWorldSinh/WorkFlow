import type { ApiResponse } from '@/types/workflow'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

export class ApiError extends Error {
  constructor(
    public status: number,
    public message: string,
    public data?: any
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

export async function request<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  const url = `${BASE_URL}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`

  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    Accept: 'application/json',
    ...options.headers,
  }

  try {
    const response = await fetch(url, {
      ...options,
      headers,
    })

    const contentType = response.headers.get('content-type')
    const isJson = contentType && contentType.includes('application/json')
    const data: ApiResponse<T> = isJson ? await response.json() : null

    if (!response.ok) {
      const errorMessage = data?.message || `Yêu cầu thất bại với mã lỗi HTTP ${response.status}`
      throw new ApiError(response.status, errorMessage, data)
    }

    return data
  } catch (err: any) {
    if (err instanceof ApiError) {
      throw err
    }
    throw new ApiError(500, err?.message || 'Không thể kết nối đến máy chủ Backend')
  }
}
