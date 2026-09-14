import type { ConditionDataType, ConditionOperator } from './editor'
import type { FormFieldType } from './form'

export interface WorkflowVariable {
  key: string
  label: string
  dataType: ConditionDataType
  isRequired?: boolean
  description?: string
  defaultValue?: any
  usedInEdges?: {
    edgeId: string
    edgeLabel?: string
    fromNodeName?: string
    toNodeName?: string
    operator: ConditionOperator
    compareValue: string
  }[]
  isSystem?: boolean
}

export type MappingStatus = 'matched' | 'type_mismatch' | 'unmapped'

export interface VariableMappingItem {
  variableKey: string
  variableLabel: string
  variableDataType: ConditionDataType
  formFieldKey: string
  formFieldLabel?: string
  formFieldType?: FormFieldType
  status: MappingStatus
  isAutoMatched?: boolean
  customFallbackValue?: any
}

export type FieldPermissionType = 'editable' | 'readonly' | 'hidden'

export type NodePermissionMatrix = Record<string, Record<string, FieldPermissionType>>

export interface WorkflowFormMapping {
  id?: string | number
  workflowId: number | string
  workflowName?: string
  formId: number | string
  formName?: string
  variableMappings: VariableMappingItem[]
  nodePermissions: NodePermissionMatrix
  matchScore: number
  status: 'ready' | 'partial' | 'unmatched'
  updatedAt?: string
}

export interface SimulationResult {
  evaluatedVariables: Record<string, any>
  activeEdges: {
    edgeId: string
    fromNodeId: string
    toNodeId: string
    label?: string
    passed: boolean
    reason: string
  }[]
  targetNodes: {
    nodeId: string
    nodeName: string
    nodeType: string
  }[]
}
