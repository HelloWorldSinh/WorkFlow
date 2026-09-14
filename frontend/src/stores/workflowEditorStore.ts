import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import type {
  EditorNodeType,
  WorkflowEditorNode,
  WorkflowEditorEdge,
  TransitionRule,
  ConditionMatchType,
} from '@/types/editor'
import { workflowApi } from '@/services/workflowApi'
import { WORKFLOW_TEMPLATES } from '@/constants/workflowTemplates'
import { useFormStore } from './formStore'

export const useWorkflowEditorStore = defineStore('workflowEditor', () => {
  // Workflow Meta
  const workflowId = ref<number | string | null>(null)
  const workflowName = ref<string>('Quy trình mới')
  const workflowCode = ref<string>('WF-001')
  const workflowStatus = ref<'Draft' | 'Published' | 'Suspended'>('Draft')
  const isSaving = ref<boolean>(false)
  const isDirty = ref<boolean>(false)
  let isInitializing = false

  // Tự động đánh dấu isDirty khi người dùng chỉnh sửa tên workflow
  watch(workflowName, (newVal, oldVal) => {
    if (!isInitializing && oldVal !== undefined && newVal !== oldVal) {
      isDirty.value = true
    }
  })

  // Canvas State
  const zoom = ref<number>(1.0)
  const pan = ref<{ x: number; y: number }>({ x: 0, y: 0 })

  // Selection & Properties Panel
  const selectedNodeId = ref<string | null>(null)
  const selectedEdgeId = ref<string | null>(null)
  const isPropertiesPanelOpen = ref<boolean>(false)

  // Temporary connecting line while dragging connection
  const connectingSourceNodeId = ref<string | null>(null)
  const mouseCanvasPos = ref<{ x: number; y: number }>({ x: 0, y: 0 })

  // Toast / notification
  const toast = ref<{ text: string; type: 'success' | 'error' | 'info' } | null>(null)

  function showToast(text: string, type: 'success' | 'error' | 'info' = 'success') {
    toast.value = { text, type }
    setTimeout(() => {
      toast.value = null
    }, 3500)
  }

  // Nodes & Connections (Edges)
  const nodes = ref<WorkflowEditorNode[]>([])
  const edges = ref<WorkflowEditorEdge[]>([])

  // Selected Node Getter
  const selectedNode = computed(() => {
    if (!selectedNodeId.value) return null
    return nodes.value.find((n) => n.id === selectedNodeId.value) || null
  })

  // Selected Edge Getter
  const selectedEdge = computed(() => {
    if (!selectedEdgeId.value) return null
    return edges.value.find((e) => e.id === selectedEdgeId.value) || null
  })

  // Palette Node definitions
  const nodePalette = [
    {
      type: 'start' as EditorNodeType,
      category: 'functional' as const,
      label: 'Start',
      title: 'Bắt đầu',
      icon: '▶',
      color: '#10b981',
      bg: '#ecfdf5',
      desc: 'Điểm khởi đầu quy trình',
    },
    {
      type: 'approval' as EditorNodeType,
      category: 'functional' as const,
      label: 'Approval',
      title: 'Phê duyệt',
      icon: '✓',
      color: '#6366f1',
      bg: '#eef2ff',
      desc: 'Bước phê duyệt lãnh đạo/quản lý',
    },
    {
      type: 'review' as EditorNodeType,
      category: 'functional' as const,
      label: 'Review',
      title: 'Soát xét',
      icon: '👁',
      color: '#3b82f6',
      bg: '#eff6ff',
      desc: 'Kiểm tra hồ sơ, góp ý',
    },
    {
      type: 'assignment' as EditorNodeType,
      category: 'functional' as const,
      label: 'Assignment',
      title: 'Phân việc',
      icon: '👤',
      color: '#f59e0b',
      bg: '#fffbeb',
      desc: 'Giao tác vụ cho nhân sự',
    },
    {
      type: 'notification' as EditorNodeType,
      category: 'functional' as const,
      label: 'Notification',
      title: 'Thông báo',
      icon: '🔔',
      color: '#06b6d4',
      bg: '#ecfeff',
      desc: 'Gửi Email / Teams / In-app',
    },
    {
      type: 'system_action' as EditorNodeType,
      category: 'functional' as const,
      label: 'System Action',
      title: 'Tác vụ hệ thống',
      icon: '⚡',
      color: '#8b5cf6',
      bg: '#f5f3ff',
      desc: 'Webhook API, CSDL, ERP',
    },
    {
      type: 'end' as EditorNodeType,
      category: 'functional' as const,
      label: 'End',
      title: 'Kết thúc',
      icon: '⏹',
      color: '#ef4444',
      bg: '#fef2f2',
      desc: 'Điểm kết thúc quy trình',
    },
    {
      type: 'condition' as EditorNodeType,
      category: 'branching' as const,
      label: 'Condition',
      title: 'Rẽ nhánh điều kiện',
      icon: '🔀',
      color: '#ec4899',
      bg: '#fdf2f8',
      desc: 'Rẽ nhánh theo điều kiện (If/Else)',
    },
    {
      type: 'parallel' as EditorNodeType,
      category: 'branching' as const,
      label: 'Parallel',
      title: 'Rẽ nhánh song song',
      icon: '║',
      color: '#0284c7',
      bg: '#f0f9ff',
      desc: 'Thực hiện song song nhiều luồng',
    },
    {
      type: 'join' as EditorNodeType,
      category: 'branching' as const,
      label: 'Join',
      title: 'Hợp luồng song song',
      icon: '⇶',
      color: '#0d9488',
      bg: '#ccfbf1',
      desc: 'Gộp các luồng song song về 1 điểm (Wait All)',
    },
  ]

  // Add Node
  function addNode(type: EditorNodeType, pos?: { x: number; y: number }) {
    const paletteItem = nodePalette.find((p) => p.type === type)
    const nodeCount = nodes.value.filter((n) => n.type === type).length + 1
    const newId = `node-${type}-${Date.now().toString().slice(-4)}`

    // Default position if not dragged: arrange smartly on canvas
    let position = pos
    if (!position) {
      const lastNode = nodes.value[nodes.value.length - 1]
      const lastX = lastNode ? lastNode.position.x + 220 : 100
      const lastY = lastNode ? lastNode.position.y : 200
      position = { x: lastX, y: lastY }
    }

    const newNode: WorkflowEditorNode = {
      id: newId,
      type,
      name: `${paletteItem?.title || type} ${nodeCount}`,
      description: paletteItem?.desc || '',
      position,
      config: getDefaultConfigForType(type),
    }

    nodes.value.push(newNode)
    selectNode(newId)
    isDirty.value = true
    showToast(`Đã thêm bước "${newNode.name}" vào Canvas!`, 'info')
  }

  function getDefaultConfigForType(type: EditorNodeType) {
    switch (type) {
      case 'approval':
        return {
          approverType: 'role' as const,
          approverRole: 'Trưởng phòng ban',
          slaHours: 24,
          approvalStrategy: 'single' as const,
          escalationRule: 'remind' as const,
          allowRequestChanges: true,
          requireNoteOnReject: true,
        }
      case 'review':
        return {
          reviewerType: 'department_lead' as const,
          reviewerRole: 'Phòng ban liên quan',
          slaHours: 24,
          allowRequestChanges: true,
        }
      case 'assignment':
        return {
          assigneeType: 'role' as const,
          assigneeRole: 'Chuyên viên xử lý',
          taskTitle: 'Thực hiện xử lý nghiệp vụ',
          dueDays: 3,
        }
      case 'notification':
        return {
          channels: ['in_app' as const],
          recipientType: 'workflow_owner' as const,
          subject: '[Thông báo] Quy trình có cập nhật mới',
          contentTemplate: 'Xin chào, bước xử lý đã được chuyển tiếp thành công.',
        }
      case 'system_action':
        return {
          actionType: 'webhook' as const,
          httpMethod: 'POST' as const,
          endpointUrl: 'https://api.company.com/webhook',
        }
      case 'condition':
        return {
          defaultBranch: 'ELSE',
        }
      case 'parallel':
        return {
          joinMode: 'all' as const,
        }
      case 'join':
        return {
          joinStrategy: 'wait_all' as const,
        }
      case 'start':
        return {
          triggerType: 'form_submission' as const,
          formName: 'Biểu mẫu khởi tạo',
        }
      case 'end':
        return {
          outcome: 'completed' as const,
          closingNote: 'Hoàn tất quy trình.',
        }
    }
  }

  // Delete Node
  function deleteNode(id: string) {
    const nodeIndex = nodes.value.findIndex((n) => n.id === id)
    if (nodeIndex !== -1) {
      const nodeToDelete = nodes.value[nodeIndex]
      const nodeName = nodeToDelete ? nodeToDelete.name : id
      nodes.value.splice(nodeIndex, 1)

      // Xóa tất cả các đường nối liên quan đến node này
      edges.value = edges.value.filter((e) => e.fromNodeId !== id && e.toNodeId !== id)

      if (selectedNodeId.value === id) {
        closePropertiesPanel()
      }
      isDirty.value = true
      showToast(`Đã xóa bước "${nodeName}"`, 'info')
    }
  }

  // Update Node position
  function updateNodePosition(id: string, pos: { x: number; y: number }) {
    const node = nodes.value.find((n) => n.id === id)
    if (node) {
      node.position = pos
      isDirty.value = true
    }
  }

  // Update Node config
  function updateNode(id: string, updates: Partial<WorkflowEditorNode>) {
    const node = nodes.value.find((n) => n.id === id)
    if (node) {
      Object.assign(node, updates)
      isDirty.value = true
    }
  }

  // Add Edge Connection
  function addEdge(
    fromNodeId: string,
    toNodeId: string,
    label: string = '',
    conditions: TransitionRule[] = [],
    matchType: ConditionMatchType = 'ALWAYS',
    branchType?: 'approved' | 'rejected' | 'condition' | 'else' | 'default'
  ) {
    if (fromNodeId === toNodeId) return

    // Kiểm tra xem connection đã tồn tại chưa
    const exists = edges.value.some((e) => e.fromNodeId === fromNodeId && e.toNodeId === toNodeId)
    if (exists) return

    // Kiểm tra giới hạn: Node approval mỗi output (approved / rejected) chỉ được nối tối đa 1 điều kiện
    const fromNode = nodes.value.find((n) => n.id === fromNodeId)
    if (fromNode && fromNode.type === 'approval' && (branchType === 'approved' || branchType === 'rejected')) {
      const branchExists = edges.value.some((e) => {
        if (e.fromNodeId !== fromNodeId) return false
        if (e.branchType === branchType) return true
        if (branchType === 'approved') {
          return (
            (e.label?.toLowerCase().includes('approv') ?? false) ||
            (e.label?.toLowerCase().includes('duyệt') ?? false) ||
            (e.conditions?.some((c) => c.compareValue === 'APPROVED') ?? false)
          )
        } else {
          return (
            (e.label?.toLowerCase().includes('reject') ?? false) ||
            (e.label?.toLowerCase().includes('từ chối') ?? false) ||
            (e.conditions?.some((c) => c.compareValue === 'REJECTED') ?? false)
          )
        }
      })

      if (branchExists) {
        showToast(
          `Đầu ra "${branchType === 'approved' ? 'Phê duyệt' : 'Từ chối'}" đã có đường nối! Mỗi đầu ra chỉ được kéo 1 điều kiện.`,
          'error'
        )
        return
      }
    }

    const newEdge: WorkflowEditorEdge = {
      id: `edge-${Date.now().toString().slice(-4)}`,
      fromNodeId,
      toNodeId,
      label,
      matchType: conditions.length > 0 ? 'CUSTOM' : matchType,
      conditions: conditions.length > 0 ? conditions : [],
      branchType,
    }

    edges.value.push(newEdge)
    isDirty.value = true
    selectEdge(newEdge.id)
    showToast('Đã tạo đường nối giữa 2 bước!', 'info')
  }

  // Delete Edge
  function deleteEdge(id: string) {
    const edgeIndex = edges.value.findIndex((e) => e.id === id)
    if (edgeIndex !== -1) {
      edges.value.splice(edgeIndex, 1)
      if (selectedEdgeId.value === id) {
        closePropertiesPanel()
      }
      isDirty.value = true
      showToast('Đã xóa đường nối', 'info')
    }
  }

  // Update Edge
  function updateEdge(id: string, updates: Partial<WorkflowEditorEdge>) {
    const edge = edges.value.find((e) => e.id === id)
    if (edge) {
      Object.assign(edge, updates)
      isDirty.value = true
    }
  }

  // Selection
  function selectNode(id: string | null) {
    selectedNodeId.value = id
    selectedEdgeId.value = null
    const targetNode = nodes.value.find((n) => n.id === id)
    const isBranching =
      targetNode &&
      (targetNode.type === 'condition' || targetNode.type === 'parallel' || targetNode.type === 'join')
    isPropertiesPanelOpen.value = !!id && !isBranching
  }

  function selectEdge(id: string | null) {
    selectedEdgeId.value = id
    selectedNodeId.value = null
    isPropertiesPanelOpen.value = !!id
  }

  function closePropertiesPanel() {
    selectedNodeId.value = null
    selectedEdgeId.value = null
    isPropertiesPanelOpen.value = false
  }

  // Zoom Controls
  function zoomIn() {
    if (zoom.value < 2.0) {
      zoom.value = Math.min(2.0, +(zoom.value + 0.15).toFixed(2))
    }
  }

  function zoomOut() {
    if (zoom.value > 0.4) {
      zoom.value = Math.max(0.4, +(zoom.value - 0.15).toFixed(2))
    }
  }

  function resetZoom() {
    zoom.value = 1.0
    pan.value = { x: 0, y: 0 }
  }

  function fitToScreen() {
    if (nodes.value.length === 0) {
      resetZoom()
      return
    }

    // Calculate bounding box of all nodes
    let minX = Infinity
    let minY = Infinity
    let maxX = -Infinity
    let maxY = -Infinity

    nodes.value.forEach((n) => {
      minX = Math.min(minX, n.position.x)
      minY = Math.min(minY, n.position.y)
      maxX = Math.max(maxX, n.position.x + 200)
      maxY = Math.max(maxY, n.position.y + 100)
    })

    const width = maxX - minX + 100
    const height = maxY - minY + 100

    // Center in canvas view
    zoom.value = 0.85
    pan.value = {
      x: -minX + 60,
      y: -minY + 60,
    }
  }

  // Action Buttons: Lưu đồ thị xuống Backend CSDL
  async function saveDraft() {
    if (!workflowId.value) {
      showToast('Không xác định được ID quy trình để lưu', 'error')
      return
    }

    isSaving.value = true
    try {
      const payload = {
        name: workflowName.value.trim(),
        status: 'Draft',
        nodes: nodes.value.map((n) => ({
          id: n.id,
          type: n.type,
          name: n.name,
          description: n.description,
          position: { x: n.position.x, y: n.position.y },
          config: n.config,
          formId: n.formBinding?.formId ? Number(n.formBinding.formId) : undefined,
          formBinding: n.formBinding,
        })),
        edges: edges.value.map((e) => ({
          id: e.id,
          fromNodeId: e.fromNodeId,
          toNodeId: e.toNodeId,
          label: e.label,
          conditionExpression: e.conditionExpression,
          matchType: e.matchType,
          conditions: e.conditions,
          branchType: e.branchType,
        })),
      }

      const response = await workflowApi.saveWorkflowGraph(workflowId.value, payload)
      if (response.success) {
        workflowStatus.value = 'Draft'
        isDirty.value = false
        showToast('Đã lưu bản nháp quy trình thành công vào CSDL!', 'success')
      }
    } catch (err: any) {
      console.error('Lỗi khi lưu bản nháp quy trình:', err)
      showToast(err?.message || 'Lỗi khi lưu bản nháp quy trình', 'error')
    } finally {
      isSaving.value = false
    }
  }

  // Validate Workflow Structure & Rules
  function validateWorkflow(showSuccessToast = true): { valid: boolean; errors: string[] } {
    const errors: string[] = []

    // 1. Kiểm tra tên quy trình
    if (!workflowName.value || !workflowName.value.trim()) {
      errors.push('Tên quy trình không được để trống')
    }

    // 2. Kiểm tra số lượng node
    if (nodes.value.length === 0) {
      errors.push('Quy trình chưa có bước nào trên Canvas')
      showToast(errors[0] || 'Quy trình chưa có bước nào', 'error')
      return { valid: false, errors }
    }

    // 3. Kiểm tra Start node
    const startNodes = nodes.value.filter((n) => n.type === 'start')
    if (startNodes.length === 0) {
      errors.push('Quy trình cần có ít nhất 1 bước "Bắt đầu (Start)"')
    } else if (startNodes.length > 1) {
      errors.push('Quy trình chỉ nên có 1 bước "Bắt đầu (Start)" duy nhất')
    }

    // 4. Kiểm tra End node
    const endNodes = nodes.value.filter((n) => n.type === 'end')
    if (endNodes.length === 0) {
      errors.push('Quy trình cần có ít nhất 1 bước "Kết thúc (End)"')
    }

    // 5. Kiểm tra node cô lập (không có dây nối vào/ra)
    if (nodes.value.length > 1) {
      for (const node of nodes.value) {
        const inEdges = edges.value.filter((e) => e.toNodeId === node.id)
        const outEdges = edges.value.filter((e) => e.fromNodeId === node.id)

        if (node.type === 'start') {
          if (outEdges.length === 0) {
            errors.push(`Bước Bắt đầu "${node.name}" chưa được nối đến bước tiếp theo`)
          }
        } else if (node.type === 'end') {
          if (inEdges.length === 0) {
            errors.push(`Bước Kết thúc "${node.name}" chưa có luồng nào nối đến`)
          }
        } else {
          if (inEdges.length === 0 && outEdges.length === 0) {
            errors.push(`Bước "${node.name}" đang bị cô lập (chưa có kết nối nào)`)
          } else if (inEdges.length === 0) {
            errors.push(`Bước "${node.name}" chưa có luồng dẫn vào`)
          } else if (outEdges.length === 0) {
            errors.push(`Bước "${node.name}" chưa có luồng dẫn ra bước tiếp theo`)
          }
        }
      }
    }

    if (errors.length > 0) {
      showToast(errors[0] || 'Phát hiện lỗi trong quy trình', 'error')
      return { valid: false, errors }
    }

    if (showSuccessToast) {
      showToast('✓ Quy trình hợp lệ! Tất cả các bước và liên kết đều chuẩn xác.', 'success')
    }
    return { valid: true, errors: [] }
  }

  async function publishWorkflow() {
    if (!workflowId.value) {
      showToast('Không xác định được ID quy trình để xuất bản', 'error')
      return
    }

    // Validate workflow structure before publishing
    const { valid } = validateWorkflow(false)
    if (!valid) return

    isSaving.value = true
    try {
      const payload = {
        name: workflowName.value.trim(),
        status: 'Published',
        nodes: nodes.value.map((n) => ({
          id: n.id,
          type: n.type,
          name: n.name,
          description: n.description,
          position: { x: n.position.x, y: n.position.y },
          config: n.config,
          formId: n.formBinding?.formId ? Number(n.formBinding.formId) : undefined,
          formBinding: n.formBinding,
        })),
        edges: edges.value.map((e) => ({
          id: e.id,
          fromNodeId: e.fromNodeId,
          toNodeId: e.toNodeId,
          label: e.label,
          conditionExpression: e.conditionExpression,
          matchType: e.matchType,
          conditions: e.conditions,
          branchType: e.branchType,
        })),
      }

      const response = await workflowApi.saveWorkflowGraph(workflowId.value, payload)
      if (response.success) {
        workflowStatus.value = 'Published'
        isDirty.value = false
        showToast('Quy trình đã được xuất bản (Published) và kích hoạt thành công!', 'success')
      }
    } catch (err: any) {
      console.error('Lỗi khi xuất bản quy trình:', err)
      showToast(err?.message || 'Lỗi khi xuất bản quy trình', 'error')
    } finally {
      isSaving.value = false
    }
  }

  async function initWorkflow(id: number | string, name?: string, templateId?: string) {
    isInitializing = true
    try {
      workflowId.value = id
      workflowCode.value = `WF-${String(id).padStart(3, '0')}`
      if (name) {
        workflowName.value = name
      }

      const formStore = useFormStore()
      if (formStore.forms.length === 0) {
        formStore.fetchForms().catch(() => {})
      }

      // Trường hợp 1: Mở với Template Blueprint đã chọn
      if (templateId && WORKFLOW_TEMPLATES[templateId]) {
        const tpl = WORKFLOW_TEMPLATES[templateId]
        nodes.value = JSON.parse(JSON.stringify(tpl.nodes))
        edges.value = JSON.parse(JSON.stringify(tpl.edges))
        if (!name) {
          workflowName.value = tpl.name
        }
        isDirty.value = true
        showToast(`Đã áp dụng mẫu "${tpl.name}" với ${tpl.nodes.length} bước tiêu chuẩn!`, 'success')

        // Tự động lưu ngay sơ đồ mẫu này xuống DB cho workflow
        try {
          await saveDraft()
        } catch (ignored) {}

        setTimeout(() => {
          fitToScreen()
        }, 200)
        return
      }

      // Trường hợp 2: Tải sơ đồ đã lưu từ Backend CSDL
      try {
        const response = await workflowApi.getWorkflowGraph(id)
        if (response.success && response.data) {
          const graphData = response.data
          workflowName.value = graphData.workflowName || workflowName.value
          workflowCode.value = graphData.workflowCode || workflowCode.value
          workflowStatus.value = (graphData.status as any) || 'Draft'

          if (graphData.nodes && graphData.nodes.length > 0) {
            nodes.value = graphData.nodes.map((n: any) => {
              let fb = n.formBinding
              if (!fb && n.formId) {
                const form = formStore.getFormById(n.formId)
                fb = {
                  formId: n.formId,
                  formName: form ? form.name : '',
                  fieldPermissions: {},
                }
              } else if (fb && !fb.formName && fb.formId) {
                const form = formStore.getFormById(fb.formId)
                if (form) fb.formName = form.name
              }
              return {
                id: n.id,
                type: n.type as any,
                name: n.name,
                description: n.description || '',
                position: { x: n.position.x, y: n.position.y },
                config: n.config || {},
                formBinding: fb,
              }
            })

            edges.value = (graphData.edges || []).map((e: any) => ({
              id: e.id,
              fromNodeId: e.fromNodeId,
              toNodeId: e.toNodeId,
              label: e.label || '',
              conditionExpression: e.conditionExpression || '',
              matchType: e.matchType || (e.conditions && e.conditions.length ? 'AND' : 'ALWAYS'),
              conditions: Array.isArray(e.conditions) ? e.conditions : [],
              branchType: e.branchType,
            }))

            isDirty.value = false
            setTimeout(() => {
              fitToScreen()
            }, 200)
            return
          }
        }
      } catch (err) {
        console.error('Không tìm thấy sơ đồ đã lưu trên backend hoặc quy trình trống:', err)
      }

      // Trường hợp 3: Luồng hoàn toàn mới (Blank canvas)
      nodes.value = []
      edges.value = []
      isDirty.value = false
      resetZoom()
    } finally {
      setTimeout(() => {
        isInitializing = false
      }, 300)
    }
  }

  function discardChanges() {
    isDirty.value = false
    showToast('Đã hoàn tác các thay đổi chưa lưu', 'info')
  }

  return {
    // State
    workflowId,
    workflowName,
    workflowCode,
    workflowStatus,
    isSaving,
    isDirty,
    zoom,
    pan,
    nodes,
    edges,
    selectedNodeId,
    selectedEdgeId,
    selectedNode,
    selectedEdge,
    isPropertiesPanelOpen,
    connectingSourceNodeId,
    mouseCanvasPos,
    nodePalette,
    toast,

    // Actions
    initWorkflow,
    addNode,
    deleteNode,
    updateNodePosition,
    updateNode,
    addEdge,
    deleteEdge,
    updateEdge,
    selectNode,
    selectEdge,
    closePropertiesPanel,
    zoomIn,
    zoomOut,
    resetZoom,
    fitToScreen,
    saveDraft,
    publishWorkflow,
    validateWorkflow,
    discardChanges,
    showToast,
  }
})


