export type EditorNodeType = 
  | 'start'
  | 'approval'
  | 'review'
  | 'assignment'
  | 'notification'
  | 'system_action'
  | 'condition'
  | 'parallel'
  | 'join'
  | 'end'

export interface EditorNodePosition {
  x: number
  y: number
}

export interface ApprovalConfig {
  approvalMode?: 'single' | 'multi'
  approverType: 'role' | 'user' | 'manager' | 'dynamic'
  approverRole?: string
  approverUserId?: number | string
  approverName?: string
  multiAssigneeType?: 'users' | 'roles'
  approverUserIds?: number[]
  approverUserNames?: string[]
  approverRoles?: string[]
  dynamicApprover?: 'creator_manager' | 'department_head' | 'project_manager'
  slaHours: number
  approvalStrategy?: 'single' | 'consensus' | 'majority'
  multiApprovalRule?: 'all' | 'half' | 'majority' | 'threshold' | 'any'
  approvalThreshold?: number
  multiRejectionRule?: 'any' | 'majority'
  escalationRule?: 'remind' | 'escalate' | 'auto_reject'
  allowRequestChanges?: boolean
  requireNoteOnReject?: boolean
}

export interface ReviewConfig {
  reviewerType: 'role' | 'user' | 'department_lead' | 'manager'
  reviewerRole?: string
  reviewerUserId?: number
  reviewerName?: string
  slaHours: number
  allowRequestChanges: boolean
}

export interface AssignmentConfig {
  assigneeType: 'role' | 'user'
  assigneeRole?: string
  assigneeUserId?: number
  assigneeName?: string
  taskTitle: string
  taskDescription?: string
  dueDays: number
  formRequired?: boolean
}

export interface NotificationConfig {
  channels: ('email' | 'teams' | 'zalo' | 'in_app')[]
  recipientType: 'workflow_owner' | 'current_assignee' | 'custom'
  customRecipients?: string
  subject: string
  contentTemplate: string
}

export interface SystemActionConfig {
  actionType: 'webhook' | 'db_update' | 'erp_sync'
  endpointUrl?: string
  httpMethod?: 'GET' | 'POST' | 'PUT'
  payloadTemplate?: string
  retryCount?: number
}

export interface StartConfig {
  triggerType: 'manual' | 'form_submission' | 'schedule'
  formName?: string
}

export interface EndConfig {
  outcome: 'completed' | 'rejected' | 'cancelled'
  closingNote?: string
}

export interface ConditionConfig {
  defaultBranch?: string
}

export interface ParallelConfig {
  joinMode?: 'all' | 'any'
}

export interface JoinConfig {
  joinStrategy?: 'wait_all' | 'first_come' | 'n_of_m'
  requiredCount?: number
}

import type { NodeFormBinding } from './form'

export interface WorkflowEditorNode {
  id: string
  type: EditorNodeType
  name: string
  description?: string
  position: EditorNodePosition
  formBinding?: NodeFormBinding
  config: Partial<
    ApprovalConfig &
    ReviewConfig &
    AssignmentConfig &
    NotificationConfig &
    SystemActionConfig &
    StartConfig &
    EndConfig &
    ConditionConfig &
    ParallelConfig &
    JoinConfig
  >
}

export type ConditionOperator =
  | 'EQUALS'
  | 'NOT_EQUALS'
  | 'GREATER_THAN'
  | 'GREATER_THAN_OR_EQUAL'
  | 'LESS_THAN'
  | 'LESS_THAN_OR_EQUAL'
  | 'CONTAINS'

export type ConditionDataType = 'STRING' | 'NUMBER' | 'BOOLEAN' | 'DATE'

export type ConditionMatchType = 'AND' | 'OR' | 'ALWAYS' | 'CUSTOM'

export interface TransitionRule {
  id?: string | number
  fieldKey: string
  operator: ConditionOperator
  compareValue: string
  dataType: ConditionDataType
  logicOp?: 'AND' | 'OR'
}

export interface WorkflowEditorEdge {
  id: string
  fromNodeId: string
  toNodeId: string
  label?: string
  priority?: number
  conditionExpression?: string
  matchType?: ConditionMatchType
  conditions?: TransitionRule[]
  branchType?: 'approved' | 'rejected' | 'condition' | 'else' | 'default'
}
