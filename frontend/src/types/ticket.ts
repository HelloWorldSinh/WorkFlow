import type { WorkflowGraphResponseDTO } from './workflow'

export type TicketStatus = 'Running' | 'Completed' | 'Rejected' | 'Cancelled'
export type TaskActionType = 'APPROVE' | 'REJECT'

export interface UserSummary {
  id: number
  fullName: string
  email: string
  role?: string
}

export interface TicketItem {
  id: number
  requestCode: string
  workflowId: number
  workflowName: string
  workflowCode: string
  title: string
  creator: UserSummary | null
  status: TicketStatus
  currentNodeName?: string
  currentNodeType?: string
  currentAssignee?: UserSummary | null
  startTime: string
  endTime?: string | null
}

export interface TaskDetail {
  id: number
  nodeId: number
  clientNodeId: string
  nodeName: string
  nodeType: string
  assignedUser: UserSummary | null
  status: 'Pending' | 'Approved' | 'Rejected'
  dueDate?: string | null
  completedAt?: string | null
  comment?: string
  submittedData?: Record<string, any>
}

export interface TicketDetail {
  id: number
  requestCode: string
  workflowId: number
  workflowName: string
  workflowCode: string
  title: string
  creator: UserSummary | null
  status: TicketStatus
  startTime: string
  endTime?: string | null
  formId?: number | null
  formName?: string | null
  variables: Record<string, any>
  activeNodeClientId?: string | null
  completedNodeClientIds: string[]
  taskHistory: TaskDetail[]
  graph: WorkflowGraphResponseDTO
}

export interface TaskSummary {
  taskId: number
  ticketId: number
  requestCode: string
  ticketTitle: string
  workflowName: string
  nodeName: string
  nodeType: string
  creator: UserSummary | null
  status: 'Pending' | 'Approved' | 'Rejected'
  dueDate?: string | null
  createdAt: string
}

export interface TicketStats {
  total: number
  running: number
  completed: number
  rejected: number
  pendingMyTasks: number
}

export interface CreateTicketRequest {
  workflowId: number
  title?: string
  creatorId?: number
  formData: Record<string, any>
}

export interface TaskActionRequest {
  action: 'APPROVE' | 'REJECT'
  comment?: string
  userId?: number
  submittedData?: Record<string, any>
}

export interface TicketFilter {
  keyword?: string
  status?: TicketStatus | 'ALL'
  workflowId?: number
  creatorId?: number
}
