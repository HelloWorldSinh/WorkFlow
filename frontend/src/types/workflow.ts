import type { NodeFormBinding } from './form'

export type WorkflowStatus = 'published' | 'draft' | 'suspended' | 'deleted' | 'Draft' | 'Published' | 'Suspended' | 'Deleted'

export type WorkflowModule = 'HR' | 'Procurement' | 'Finance' | 'IT' | 'Legal' | 'Operations'

export type WorkflowType = 'single_approval' | 'multi_approval' | 'automation' | 'hybrid'

export interface WorkflowOwner {
  id: string | number
  name: string
  email: string
  avatar?: string
  role?: string
}

export interface WorkflowItem {
  id: string | number
  code?: string
  name: string
  description: string
  module?: WorkflowModule
  type?: WorkflowType
  version?: string
  owner: WorkflowOwner
  status: WorkflowStatus
  activeInstances: number
  totalInstances?: number
  totalSteps?: number
  updatedAt?: string
  createdAt?: string
  deletedAt?: string | null
}

export interface WorkflowFilter {
  search: string
  status: WorkflowStatus | 'all'
  module: WorkflowModule | 'all'
  sortBy: 'updatedAt' | 'createdAt' | 'name' | 'version' | 'status'
  sortOrder: 'asc' | 'desc'
  includeDeleted?: boolean
}

export interface WorkflowMetrics {
  total: number
  published: number
  draft: number
  suspended: number
}

export interface WorkflowTemplate {
  id: string
  title: string
  description: string
  module: WorkflowModule
  estimatedSteps: number
  type: WorkflowType
  icon: string
}

export interface WorkflowVersionLog {
  version: string
  updatedBy: string
  updatedAt: string
  isCurrent: boolean
  changes: string[]
}

// Backend API Types
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
  timestamp: string
}

export interface PageResponse<T> {
  items: T[]
  pageNumber: number
  pageSize: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface WorkflowResponseDTO {
  id: number
  name: string
  description: string
  owner: {
    id: number
    fullName: string
    email: string
  } | null
  status: 'Draft' | 'Published' | 'Suspended' | 'Deleted'
  activeInstances: number
  totalInstances: number
  createdAt: string
  deletedAt: string | null
}

export interface CreateWorkflowRequest {
  name: string
  description?: string
  ownerId: number
  status?: 'Draft' | 'Published' | 'Suspended' | 'Deleted'
}

export interface UpdateWorkflowRequest {
  name: string
  description?: string
  ownerId?: number
  status?: 'Draft' | 'Published' | 'Suspended' | 'Deleted'
}

export interface DeleteWorkflowResponseDTO {
  workflowId: number
  deleteType: 'HARD_DELETE' | 'SOFT_DELETE' | string
  message: string
}

export interface SaveWorkflowGraphRequest {
  name?: string
  description?: string
  status?: string
  nodes: {
    id: string
    type: string
    name: string
    description?: string
    position?: { x: number; y: number }
    config?: Record<string, any>
    formId?: number | string
    formBinding?: NodeFormBinding
  }[]
  edges: {
    id: string
    fromNodeId: string
    toNodeId: string
    label?: string
    conditionExpression?: string
    matchType?: string
    conditions?: any[]
    branchType?: string
  }[]
}

export interface WorkflowGraphResponseDTO {
  workflowId: number
  workflowName: string
  workflowCode: string
  status: string
  nodes: {
    id: string
    type: string
    name: string
    description?: string
    position: { x: number; y: number }
    config?: Record<string, any>
    formId?: number | string
    formBinding?: NodeFormBinding
  }[]
  edges: {
    id: string
    fromNodeId: string
    toNodeId: string
    label?: string
    conditionExpression?: string
    matchType?: string
    conditions?: any[]
    branchType?: string
  }[]
}

