import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type {
  WorkflowVariable,
  VariableMappingItem,
  WorkflowFormMapping,
  NodePermissionMatrix,
  SimulationResult,
  FieldPermissionType,
} from '@/types/mapping'
import type { FormField } from '@/types/form'
import type { WorkflowEditorNode, WorkflowEditorEdge, ConditionOperator } from '@/types/editor'
import { useFormStore } from './formStore'
import { useWorkflowStore } from './workflowStore'
import { useWorkflowEditorStore } from './workflowEditorStore'
import { WORKFLOW_TEMPLATES } from '@/constants/workflowTemplates'

const STORAGE_KEY = 'worker_builder_workflow_form_mappings_v1'

export const useWorkflowMappingStore = defineStore('workflowMapping', () => {
  const formStore = useFormStore()
  const workflowStore = useWorkflowStore()
  const editorStore = useWorkflowEditorStore()

  // State
  const mappings = ref<WorkflowFormMapping[]>([])
  const selectedWorkflowId = ref<number | string | null>(null)
  const selectedFormId = ref<number | string | null>(null)
  const isSaving = ref<boolean>(false)
  const activeTab = ref<'mapping' | 'permissions' | 'simulation'>('mapping')

  // Load from localStorage
  const loadMappings = () => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (raw) {
        mappings.value = JSON.parse(raw)
      } else {
        // Initial sample mapping for demo/template
        mappings.value = [
          {
            id: 'map-demo-1',
            workflowId: 1,
            workflowName: 'Phê duyệt Yêu cầu Mua sắm (Tiêu chuẩn)',
            formId: 2,
            formName: 'Đơn Đề Xuất Mua Sắm Thiết Bị',
            variableMappings: [
              {
                variableKey: 'amount',
                variableLabel: 'Kinh phí đề xuất (Số tiền)',
                variableDataType: 'NUMBER',
                formFieldKey: 'estimated_cost',
                formFieldLabel: 'Kinh phí ước tính (VNĐ)',
                formFieldType: 'number',
                status: 'matched',
                isAutoMatched: true,
              },
              {
                variableKey: 'category',
                variableLabel: 'Nhóm danh mục thiết bị',
                variableDataType: 'STRING',
                formFieldKey: 'category',
                formFieldLabel: 'Nhóm thiết bị',
                formFieldType: 'select',
                status: 'matched',
                isAutoMatched: true,
              },
              {
                variableKey: 'department',
                variableLabel: 'Phòng ban người gửi',
                variableDataType: 'STRING',
                formFieldKey: 'title',
                formFieldLabel: 'Tên đề xuất mua sắm',
                formFieldType: 'text',
                status: 'matched',
                isAutoMatched: false,
              },
            ],
            nodePermissions: {
              'node-start': {
                title: 'editable',
                category: 'editable',
                quantity: 'editable',
                estimated_cost: 'editable',
                justification: 'editable',
                quotation_url: 'editable',
              },
              'node-review': {
                title: 'readonly',
                category: 'readonly',
                quantity: 'readonly',
                estimated_cost: 'readonly',
                justification: 'readonly',
                quotation_url: 'readonly',
              },
              'node-accountant': {
                title: 'readonly',
                category: 'readonly',
                quantity: 'readonly',
                estimated_cost: 'editable',
                justification: 'readonly',
                quotation_url: 'readonly',
              },
              'node-ceo-approval': {
                title: 'readonly',
                category: 'readonly',
                quantity: 'readonly',
                estimated_cost: 'readonly',
                justification: 'readonly',
                quotation_url: 'readonly',
              },
            },
            matchScore: 100,
            status: 'ready',
            updatedAt: '2026-09-02T10:00:00Z',
          },
        ]
      }
    } catch (e) {
      console.error('Error loading mappings:', e)
      mappings.value = []
    }
  }

  const saveMappingsToStorage = () => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(mappings.value))
    } catch (e) {
      console.error('Error saving mappings to storage:', e)
    }
  }

  // Get current active mapping
  const currentMapping = computed<WorkflowFormMapping | null>(() => {
    if (!selectedWorkflowId.value) return null
    return (
      mappings.value.find(
        (m) =>
          String(m.workflowId) === String(selectedWorkflowId.value) &&
          (selectedFormId.value ? String(m.formId) === String(selectedFormId.value) : true)
      ) || null
    )
  })

  // Selected Workflow Object
  const currentWorkflow = computed(() => {
    if (!selectedWorkflowId.value) return null
    // Check in workflowStore list
    const fromList = workflowStore.workflows.find(
      (w) => String(w.id) === String(selectedWorkflowId.value)
    )
    if (fromList) return fromList

    // Check if active in editorStore
    if (String(editorStore.workflowId) === String(selectedWorkflowId.value)) {
      return {
        id: editorStore.workflowId,
        name: editorStore.workflowName,
        description: 'Đang mở trong trình thiết kế',
        status: editorStore.workflowStatus,
        owner: { id: 1, name: 'Workflow Owner', email: 'owner@fpt.com' },
        activeInstances: 0,
      }
    }

    // Check in template library
    const tpl = WORKFLOW_TEMPLATES[`tpl-${selectedWorkflowId.value}`] || WORKFLOW_TEMPLATES['tpl-1']
    if (tpl) {
      return {
        id: selectedWorkflowId.value,
        name: tpl.name,
        description: 'Mẫu quy trình hệ thống',
        status: 'Draft' as const,
        owner: { id: 1, name: 'System Template', email: 'system@fpt.com' },
        activeInstances: 0,
      }
    }

    return null
  })

  // Selected Form Object
  const currentForm = computed(() => {
    if (!selectedFormId.value) return null
    return formStore.getFormById(selectedFormId.value) || null
  })

  // Extract Workflow Nodes & Edges
  const getWorkflowGraph = (
    wfId: number | string
  ): { nodes: WorkflowEditorNode[]; edges: WorkflowEditorEdge[] } => {
    // If editor currently has this workflow loaded
    if (String(editorStore.workflowId) === String(wfId) && editorStore.nodes.length > 0) {
      return {
        nodes: editorStore.nodes,
        edges: editorStore.edges,
      }
    }

    // Check template blueprints
    const tplKey = `tpl-${wfId}`
    if (WORKFLOW_TEMPLATES[tplKey]) {
      return {
        nodes: WORKFLOW_TEMPLATES[tplKey].nodes,
        edges: WORKFLOW_TEMPLATES[tplKey].edges,
      }
    }

    // Default template 1
    if (WORKFLOW_TEMPLATES['tpl-1']) {
      return {
        nodes: WORKFLOW_TEMPLATES['tpl-1'].nodes,
        edges: WORKFLOW_TEMPLATES['tpl-1'].edges,
      }
    }

    return { nodes: [], edges: [] }
  }

  // Extract Process Variables from Workflow (Data Contract)
  const extractWorkflowVariables = (wfId: number | string): WorkflowVariable[] => {
    const { nodes, edges } = getWorkflowGraph(wfId)
    const varMap = new Map<string, WorkflowVariable>()

    // 1. Quét từ tất cả Transitions/Edges conditions
    edges.forEach((edge) => {
      const fromNode = nodes.find((n) => n.id === edge.fromNodeId)
      const toNode = nodes.find((n) => n.id === edge.toNodeId)

      if (edge.conditions && Array.isArray(edge.conditions)) {
        edge.conditions.forEach((cond) => {
          if (!cond.fieldKey) return
          // Bỏ qua biến action nội bộ của approval node
          if (cond.fieldKey === 'action') return

          const existing = varMap.get(cond.fieldKey)
          const edgeInfo = {
            edgeId: edge.id,
            edgeLabel: edge.label || `${fromNode?.name || 'Node'} → ${toNode?.name || 'Node'}`,
            fromNodeName: fromNode?.name,
            toNodeName: toNode?.name,
            operator: cond.operator,
            compareValue: String(cond.compareValue),
          }

          if (existing) {
            existing.usedInEdges?.push(edgeInfo)
          } else {
            let label = cond.fieldKey
            if (cond.fieldKey === 'amount' || cond.fieldKey === 'cost') {
              label = 'Kinh phí / Số tiền đề xuất'
            } else if (cond.fieldKey === 'department' || cond.fieldKey === 'dept') {
              label = 'Phòng ban người gửi'
            } else if (cond.fieldKey === 'priority' || cond.fieldKey === 'ticket_priority') {
              label = 'Mức độ ưu tiên'
            } else if (cond.fieldKey === 'category') {
              label = 'Phân loại / Danh mục'
            }

            varMap.set(cond.fieldKey, {
              key: cond.fieldKey,
              label,
              dataType: cond.dataType || 'STRING',
              isRequired: true,
              description: `Biến rẽ nhánh tại liên kết [${edgeInfo.edgeLabel}]`,
              usedInEdges: [edgeInfo],
              isSystem: false,
            })
          }
        })
      }
    })

    // 2. Thêm biến nghiệp vụ phổ biến nếu chưa có (ví dụ số tiền cho quy trình mua sắm)
    const wfName = currentWorkflow.value?.name.toLowerCase() || ''
    if (
      (wfName.includes('mua sắm') || wfName.includes('chi phí') || wfName.includes('thanh toán')) &&
      !varMap.has('amount') &&
      !varMap.has('estimated_cost')
    ) {
      varMap.set('amount', {
        key: 'amount',
        label: 'Tổng kinh phí đề xuất (Số tiền)',
        dataType: 'NUMBER',
        isRequired: true,
        description: 'Căn cứ rẽ nhánh duyệt hạn mức tài chính',
        usedInEdges: [],
        isSystem: false,
      })
    }

    // 3. Thêm các biến hệ thống mặc định (Context variables)
    if (!varMap.has('ticket_priority')) {
      varMap.set('ticket_priority', {
        key: 'ticket_priority',
        label: 'Mức độ ưu tiên (Priority)',
        dataType: 'STRING',
        isRequired: false,
        description: 'Biến hệ thống: low | medium | high | urgent',
        usedInEdges: [],
        isSystem: true,
      })
    }

    if (!varMap.has('department')) {
      varMap.set('department', {
        key: 'department',
        label: 'Phòng ban đề xuất',
        dataType: 'STRING',
        isRequired: false,
        description: 'Phòng ban người khởi tạo hoặc đối tượng thụ hưởng',
        usedInEdges: [],
        isSystem: true,
      })
    }

    return Array.from(varMap.values())
  }

  // Synonym dictionary for Auto-Match
  const SYNONYMS: Record<string, string[]> = {
    amount: [
      'amount',
      'cost',
      'price',
      'total',
      'estimated_cost',
      'tong_tien',
      'kinh_phi',
      'chi_phi',
      'so_tien',
      'budget',
    ],
    department: [
      'dept',
      'department',
      'phong_ban',
      'bo_phan',
      'don_vi',
      'team',
    ],
    priority: [
      'priority',
      'ticket_priority',
      'muc_do',
      'do_uu_tien',
      'urgency',
      'level',
    ],
    category: [
      'category',
      'type',
      'loai',
      'danh_muc',
      'phan_loai',
      'nhom',
      'leave_type',
    ],
    title: [
      'title',
      'name',
      'subject',
      'tieu_de',
      'ten',
      'summary',
      'request_title',
    ],
    description: [
      'description',
      'desc',
      'mo_ta',
      'chi_tiet',
      'justification',
      'ly_do',
      'content',
      'note',
    ],
    quantity: [
      'qty',
      'quantity',
      'so_luong',
      'count',
    ],
    date: [
      'date',
      'start_date',
      'from_date',
      'ngay',
      'ngay_tao',
      'due_date',
    ],
  }

  // Check type compatibility
  const isTypeCompatible = (varType: string, formFieldType: string): boolean => {
    if (varType === 'NUMBER') {
      return formFieldType === 'number'
    }
    if (varType === 'STRING') {
      return ['text', 'textarea', 'select'].includes(formFieldType)
    }
    if (varType === 'BOOLEAN') {
      return formFieldType === 'checkbox'
    }
    if (varType === 'DATE') {
      return formFieldType === 'date'
    }
    return true
  }

  // Auto-match algorithm
  const autoMatchVariables = (
    variables: WorkflowVariable[],
    formFields: FormField[]
  ): VariableMappingItem[] => {
    const results: VariableMappingItem[] = []

    for (const v of variables) {
      let matchedField: FormField | null = null
      let isSynonymMatch = false

      // 1. Exact key match
      matchedField =
        formFields.find((f) => f.key.toLowerCase() === v.key.toLowerCase()) || null

      // 2. Check synonyms
      if (!matchedField) {
        for (const [canonical, synonymList] of Object.entries(SYNONYMS)) {
          const varMatchesCanonical =
            v.key.toLowerCase().includes(canonical) ||
            synonymList.some((s) => v.key.toLowerCase().includes(s))

          if (varMatchesCanonical) {
            matchedField =
              formFields.find((f) => {
                const fKey = f.key.toLowerCase()
                return (
                  synonymList.some((s) => fKey.includes(s)) ||
                  fKey.includes(canonical)
                )
              }) || null

            if (matchedField) {
              isSynonymMatch = true
              break
            }
          }
        }
      }

      // 3. Fallback: match by label similarities if still not found
      if (!matchedField) {
        matchedField =
          formFields.find((f) => {
            const vLabel = v.label.toLowerCase()
            const fLabel = f.label.toLowerCase()
            return (
              (vLabel.includes('tiền') && fLabel.includes('tiền')) ||
              (vLabel.includes('phòng ban') && fLabel.includes('phòng')) ||
              (vLabel.includes('ưu tiên') && fLabel.includes('ưu tiên'))
            )
          }) || null
      }

      if (matchedField) {
        const compatible = isTypeCompatible(v.dataType, matchedField.type)
        results.push({
          variableKey: v.key,
          variableLabel: v.label,
          variableDataType: v.dataType,
          formFieldKey: matchedField.key,
          formFieldLabel: matchedField.label,
          formFieldType: matchedField.type,
          status: compatible ? 'matched' : 'type_mismatch',
          isAutoMatched: true,
        })
      } else {
        results.push({
          variableKey: v.key,
          variableLabel: v.label,
          variableDataType: v.dataType,
          formFieldKey: '',
          status: 'unmapped',
          isAutoMatched: false,
        })
      }
    }

    return results
  }

  // Calculate Match Completion Score
  const calculateMatchScore = (
    mappingsList: VariableMappingItem[],
    variables: WorkflowVariable[]
  ): number => {
    if (variables.length === 0) return 100
    const requiredVars = variables.filter((v) => v.isRequired)
    if (requiredVars.length === 0) return 100

    let validCount = 0
    requiredVars.forEach((v) => {
      const m = mappingsList.find((item) => item.variableKey === v.key)
      if (m && m.status === 'matched' && m.formFieldKey) {
        validCount++
      }
    })

    return Math.round((validCount / requiredVars.length) * 100)
  }

  // Generate default Node Permission Matrix
  const generateDefaultPermissions = (
    nodes: WorkflowEditorNode[],
    formFields: FormField[]
  ): NodePermissionMatrix => {
    const matrix: NodePermissionMatrix = {}

    nodes.forEach((node) => {
      matrix[node.id] = {}
      formFields.forEach((field) => {
        // Start node defaults to editable, other nodes default to readonly
        if (node.type === 'start') {
          matrix[node.id]![field.key] = 'editable'
        } else if (node.type === 'review' || node.type === 'approval') {
          matrix[node.id]![field.key] = 'readonly'
        } else {
          matrix[node.id]![field.key] = 'readonly'
        }
      })
    })

    return matrix
  }

  // Create or update mapping
  const saveMapping = (
    workflowId: number | string,
    formId: number | string,
    variableMappings: VariableMappingItem[],
    nodePermissions: NodePermissionMatrix
  ): WorkflowFormMapping => {
    isSaving.value = true
    try {
      const wf = currentWorkflow.value
      const form = formStore.getFormById(formId)
      const variables = extractWorkflowVariables(workflowId)
      const score = calculateMatchScore(variableMappings, variables)
      const status = score === 100 ? 'ready' : score > 0 ? 'partial' : 'unmatched'

      const existingIndex = mappings.value.findIndex(
        (m) =>
          String(m.workflowId) === String(workflowId) &&
          String(m.formId) === String(formId)
      )

      const mappingData: WorkflowFormMapping = {
        id: existingIndex >= 0 ? mappings.value[existingIndex]?.id || `map-${Date.now()}` : `map-${Date.now()}`,
        workflowId,
        workflowName: wf?.name || 'Quy trình',
        formId,
        formName: form?.name || 'Biểu mẫu',
        variableMappings,
        nodePermissions,
        matchScore: score,
        status,
        updatedAt: new Date().toISOString(),
      }

      if (existingIndex >= 0) {
        mappings.value[existingIndex] = mappingData
      } else {
        mappings.value.push(mappingData)
      }

      saveMappingsToStorage()

      // Sync back to editorStore if currently loaded
      if (String(editorStore.workflowId) === String(workflowId)) {
        const startNode = editorStore.nodes.find((n) => n.type === 'start')
        if (startNode) {
          startNode.formBinding = {
            formId: form?.id,
            formName: form?.name,
            fieldPermissions: nodePermissions[startNode.id] || {},
          }
        }
        editorStore.isDirty = true
      }

      return mappingData
    } finally {
      isSaving.value = false
    }
  }

  // Evaluate Simulation with sample Form Values
  const simulateExecution = (
    workflowId: number | string,
    formId: number | string,
    formValues: Record<string, any>
  ): SimulationResult => {
    const mapping = mappings.value.find(
      (m) =>
        String(m.workflowId) === String(workflowId) &&
        String(m.formId) === String(formId)
    )

    const evaluatedVariables: Record<string, any> = {}

    // Translate form field values to workflow variables based on mapping
    if (mapping) {
      mapping.variableMappings.forEach((m) => {
        if (m.formFieldKey && formValues[m.formFieldKey] !== undefined) {
          let val = formValues[m.formFieldKey]
          if (m.variableDataType === 'NUMBER') {
            val = Number(val) || 0
          }
          evaluatedVariables[m.variableKey] = val
        } else if (m.customFallbackValue !== undefined) {
          evaluatedVariables[m.variableKey] = m.customFallbackValue
        }
      })
    }

    // Evaluate outgoing edges
    const { nodes, edges } = getWorkflowGraph(workflowId)
    const activeEdges: SimulationResult['activeEdges'] = []
    const targetNodes: SimulationResult['targetNodes'] = []

    edges.forEach((edge) => {
      let passed = true
      let reason = 'Đường nối mặc định (Không có điều kiện cản trở)'

      if (edge.conditions && edge.conditions.length > 0) {
        let conditionPassed = true
        const reasonParts: string[] = []

        for (const cond of edge.conditions) {
          const varVal = evaluatedVariables[cond.fieldKey]
          let rulePassed = false

          if (varVal === undefined) {
            rulePassed = false
            reasonParts.push(`Biến "${cond.fieldKey}" chưa có giá trị`)
          } else {
            const cmpVal = cond.compareValue
            switch (cond.operator) {
              case 'EQUALS':
                rulePassed = String(varVal) == String(cmpVal)
                break
              case 'NOT_EQUALS':
                rulePassed = String(varVal) != String(cmpVal)
                break
              case 'GREATER_THAN':
                rulePassed = Number(varVal) > Number(cmpVal)
                break
              case 'GREATER_THAN_OR_EQUAL':
                rulePassed = Number(varVal) >= Number(cmpVal)
                break
              case 'LESS_THAN':
                rulePassed = Number(varVal) < Number(cmpVal)
                break
              case 'LESS_THAN_OR_EQUAL':
                rulePassed = Number(varVal) <= Number(cmpVal)
                break
              case 'CONTAINS':
                rulePassed = String(varVal).toLowerCase().includes(String(cmpVal).toLowerCase())
                break
              default:
                rulePassed = true
            }
            reasonParts.push(
              `${cond.fieldKey} (${varVal}) ${cond.operator} ${cmpVal} => ${rulePassed ? 'ĐẠT' : 'KHÔNG ĐẠT'}`
            )
          }

          if (edge.matchType === 'OR') {
            if (rulePassed) {
              conditionPassed = true
              break
            } else {
              conditionPassed = false
            }
          } else {
            // Default AND
            if (!rulePassed) {
              conditionPassed = false
              break
            }
          }
        }

        passed = conditionPassed
        reason = reasonParts.join('; ')
      }

      activeEdges.push({
        edgeId: edge.id,
        fromNodeId: edge.fromNodeId,
        toNodeId: edge.toNodeId,
        label: edge.label,
        passed,
        reason,
      })

      if (passed) {
        const target = nodes.find((n) => n.id === edge.toNodeId)
        if (target && !targetNodes.some((t) => t.nodeId === target.id)) {
          targetNodes.push({
            nodeId: target.id,
            nodeName: target.name,
            nodeType: target.type,
          })
        }
      }
    })

    return {
      evaluatedVariables,
      activeEdges,
      targetNodes,
    }
  }

  // Initialize
  loadMappings()

  return {
    mappings,
    selectedWorkflowId,
    selectedFormId,
    activeTab,
    isSaving,
    currentMapping,
    currentWorkflow,
    currentForm,
    getWorkflowGraph,
    extractWorkflowVariables,
    autoMatchVariables,
    calculateMatchScore,
    generateDefaultPermissions,
    saveMapping,
    simulateExecution,
    isTypeCompatible,
  }
})
