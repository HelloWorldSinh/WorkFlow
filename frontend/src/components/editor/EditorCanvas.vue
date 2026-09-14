<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useWorkflowEditorStore } from '@/stores/workflowEditorStore'
import type { EditorNodeType, WorkflowEditorNode, WorkflowEditorEdge } from '@/types/editor'

const editorStore = useWorkflowEditorStore()
const canvasContainerRef = ref<HTMLDivElement | null>(null)

// Dragging Node State
const isDraggingNode = ref(false)
const draggedNodeId = ref<string | null>(null)
const dragStartMousePos = ref({ x: 0, y: 0 })
const dragStartNodePos = ref({ x: 0, y: 0 })
const hasDraggedNode = ref(false)

// Canvas Panning State
const isPanningCanvas = ref(false)
const panStartMousePos = ref({ x: 0, y: 0 })
const panStartPos = ref({ x: 0, y: 0 })

// Connection Drawing State (Dragging from port)
const isDrawingEdge = ref(false)
const edgeSourceNodeId = ref<string | null>(null)
const edgeSourceBranch = ref<'approved' | 'rejected' | 'condition' | 'else' | null>(null)
const edgeMousePos = ref({ x: 0, y: 0 })

// Node Dimensions
const DEFAULT_NODE_WIDTH = 205
const BRANCHING_NODE_WIDTH = 56

function isBranchingNode(type: EditorNodeType): boolean {
  return type === 'condition' || type === 'parallel' || type === 'join'
}

function getNodeWidth(type: EditorNodeType): number {
  return isBranchingNode(type) ? BRANCHING_NODE_WIDTH : DEFAULT_NODE_WIDTH
}

// Port Vertical Offsets for Approval & Condition Nodes
const PORT_APPROVAL_Y_APPROVED = 20
const PORT_APPROVAL_Y_REJECTED = 48
const PORT_CONDITION_Y_IF = 20
const PORT_CONDITION_Y_ELSE = 48

function getNodeHeight(type: EditorNodeType): number {
  return type === 'approval' || type === 'condition' ? 68 : 52
}

// Convert Screen coordinates to Canvas internal coordinates (accounting for pan & zoom)
function screenToCanvas(screenX: number, screenY: number) {
  if (!canvasContainerRef.value) return { x: 0, y: 0 }
  const rect = canvasContainerRef.value.getBoundingClientRect()
  const rawX = screenX - rect.left
  const rawY = screenY - rect.top

  return {
    x: (rawX - editorStore.pan.x) / editorStore.zoom,
    y: (rawY - editorStore.pan.y) / editorStore.zoom,
  }
}

// ==========================================================
// DRAG & DROP FROM TOPBAR PALETTE
// ==========================================================
const handleCanvasDragOver = (e: DragEvent) => {
  e.preventDefault()
  if (e.dataTransfer) {
    e.dataTransfer.dropEffect = 'copy'
  }
}

const handleCanvasDrop = (e: DragEvent) => {
  e.preventDefault()
  if (!e.dataTransfer) return

  const dataStr = e.dataTransfer.getData('application/json')
  if (!dataStr) return

  try {
    const data = JSON.parse(dataStr)
    if (data?.type) {
      const pos = screenToCanvas(e.clientX, e.clientY)
      const h = getNodeHeight(data.type as EditorNodeType)
      // Center node on drop
      editorStore.addNode(data.type as EditorNodeType, {
        x: Math.round(pos.x - getNodeWidth(data.type as EditorNodeType) / 2),
        y: Math.round(pos.y - h / 2),
      })
    }
  } catch (err) {
    console.error('Drop error:', err)
  }
}

// ==========================================================
// CANVAS PANNING (BACKGROUND DRAG)
// ==========================================================
const handleCanvasMouseDown = (e: MouseEvent) => {
  // Only pan if clicking on empty canvas background
  if ((e.target as HTMLElement).classList.contains('canvas-background') || (e.target as HTMLElement).tagName === 'svg') {
    isPanningCanvas.value = true
    panStartMousePos.value = { x: e.clientX, y: e.clientY }
    panStartPos.value = { ...editorStore.pan }
    editorStore.closePropertiesPanel()
  }
}

// ==========================================================
// NODE DRAGGING & CLICK SELECTION
// ==========================================================
const handleNodeMouseDown = (e: MouseEvent, node: WorkflowEditorNode) => {
  // Don't start drag if clicking on connection port, dot, or reconnect handle
  const target = e.target as HTMLElement
  if (
    target.closest('.port-handle') ||
    target.closest('.port-dot') ||
    target.closest('.reconnect-handle')
  ) {
    return
  }

  e.stopPropagation()
  isDraggingNode.value = true
  hasDraggedNode.value = false
  draggedNodeId.value = node.id
  dragStartMousePos.value = { x: e.clientX, y: e.clientY }
  dragStartNodePos.value = { ...node.position }
}

// ==========================================================
// EDGE / CONNECTION DRAWING & RECONNECTING
// ==========================================================
// Edge Reconnection State
const isReconnectingEdge = ref(false)
const reconnectingEdgeId = ref<string | null>(null)
const reconnectMode = ref<'source' | 'target' | null>(null)
const reconnectFixedNodeId = ref<string | null>(null)
const reconnectMousePos = ref({ x: 0, y: 0 })

const startReconnectingEdge = (e: MouseEvent, edge: WorkflowEditorEdge, mode: 'source' | 'target') => {
  e.stopPropagation()
  // Ensure we don't accidentally start node dragging
  isDraggingNode.value = false
  draggedNodeId.value = null
  hasDraggedNode.value = false

  isReconnectingEdge.value = true
  reconnectingEdgeId.value = edge.id
  reconnectMode.value = mode
  reconnectFixedNodeId.value = mode === 'target' ? edge.fromNodeId : edge.toNodeId
  reconnectMousePos.value = screenToCanvas(e.clientX, e.clientY)
  editorStore.selectEdge(edge.id)
}

const isApprovalBranchEdge = (edge: WorkflowEditorEdge, branch: 'approved' | 'rejected'): boolean => {
  if (edge.branchType === branch) return true
  if (branch === 'approved') {
    return (
      (edge.label?.toLowerCase().includes('approv') ?? false) ||
      (edge.label?.toLowerCase().includes('duyệt') ?? false) ||
      (edge.conditions?.some((c) => c.compareValue === 'APPROVED') ?? false)
    )
  } else {
    return (
      (edge.label?.toLowerCase().includes('reject') ?? false) ||
      (edge.label?.toLowerCase().includes('từ chối') ?? false) ||
      (edge.conditions?.some((c) => c.compareValue === 'REJECTED') ?? false)
    )
  }
}

const hasApprovalBranchEdge = (nodeId: string, branch: 'approved' | 'rejected'): boolean => {
  return editorStore.edges.some((e) => e.fromNodeId === nodeId && isApprovalBranchEdge(e, branch))
}

const handlePortMouseDown = (
  e: MouseEvent,
  sourceNodeId: string,
  branch?: 'approved' | 'rejected' | 'condition' | 'else'
) => {
  e.stopPropagation()

  // Chặn kéo thêm nếu node này đã có 1 luồng đầu ra (ngoại trừ condition và parallel)
  const sourceNode = editorStore.nodes.find((n) => n.id === sourceNodeId)
  if (sourceNode && sourceNode.type !== 'condition' && sourceNode.type !== 'parallel') {
    const outgoingCount = editorStore.edges.filter((e) => e.fromNodeId === sourceNodeId).length
    if (outgoingCount >= 1) {
      editorStore.showToast(
        `Bước "${sourceNode.name || sourceNode.type}" chỉ được phép có 1 luồng đầu ra. Hãy dùng Node Rẽ Nhánh (Condition / Parallel) nếu muốn chia nhiều nhánh.`,
        'error'
      )
      return
    }
  }

  // Chặn không cho kéo thêm nếu cổng approval này đã có 1 đường nối ra
  if (branch && (branch === 'approved' || branch === 'rejected') && hasApprovalBranchEdge(sourceNodeId, branch)) {
    const branchLabel = branch === 'approved' ? 'Phê duyệt (Approved)' : 'Từ chối (Rejected)'
    editorStore.showToast(`Đầu ra "${branchLabel}" đã có đường nối. Mỗi đầu ra chỉ được kéo 1 điều kiện.`, 'error')
    return
  }

  // Ensure we don't accidentally start node dragging
  isDraggingNode.value = false
  draggedNodeId.value = null
  hasDraggedNode.value = false

  isDrawingEdge.value = true
  edgeSourceNodeId.value = sourceNodeId
  edgeSourceBranch.value = branch || null
  const pos = screenToCanvas(e.clientX, e.clientY)
  edgeMousePos.value = pos
}

const handlePortMouseUp = (e: MouseEvent, targetNodeId: string) => {
  e.stopPropagation()
  if (isDrawingEdge.value && edgeSourceNodeId.value) {
    if (edgeSourceNodeId.value !== targetNodeId) {
      if (edgeSourceBranch.value === 'approved') {
        editorStore.addEdge(
          edgeSourceNodeId.value,
          targetNodeId,
          'Approved',
          [{ fieldKey: 'action', operator: 'EQUALS', compareValue: 'APPROVED', dataType: 'STRING', logicOp: 'AND' }],
          'CUSTOM',
          'approved'
        )
      } else if (edgeSourceBranch.value === 'rejected') {
        editorStore.addEdge(
          edgeSourceNodeId.value,
          targetNodeId,
          'Rejected',
          [{ fieldKey: 'action', operator: 'EQUALS', compareValue: 'REJECTED', dataType: 'STRING', logicOp: 'AND' }],
          'CUSTOM',
          'rejected'
        )
      } else if (edgeSourceBranch.value === 'else') {
        editorStore.addEdge(
          edgeSourceNodeId.value,
          targetNodeId,
          'ELSE',
          [{ fieldKey: 'fallback', operator: 'EQUALS', compareValue: 'ELSE', dataType: 'STRING' }],
          'CUSTOM',
          'default'
        )
      } else if (edgeSourceBranch.value === 'condition') {
        editorStore.addEdge(
          edgeSourceNodeId.value,
          targetNodeId,
          'Nhánh điều kiện',
          [],
          'ALWAYS',
          'condition'
        )
      } else {
        editorStore.addEdge(edgeSourceNodeId.value, targetNodeId, 'Chuyển tiếp')
      }
    }
  } else if (isReconnectingEdge.value && reconnectingEdgeId.value && reconnectMode.value === 'target') {
    if (reconnectFixedNodeId.value !== targetNodeId) {
      editorStore.updateEdge(reconnectingEdgeId.value, { toNodeId: targetNodeId })
      const targetNode = editorStore.nodes.find((n) => n.id === targetNodeId)
      editorStore.showToast(`Đã chuyển điểm kết thúc đến bước "${targetNode?.name || targetNodeId}"`, 'success')
    }
  }
}

const handleOutputPortMouseUp = (
  e: MouseEvent,
  sourceNodeId: string,
  branch?: 'approved' | 'rejected' | 'condition' | 'else'
) => {
  e.stopPropagation()
  if (isReconnectingEdge.value && reconnectingEdgeId.value && reconnectMode.value === 'source') {
    if (reconnectFixedNodeId.value !== sourceNodeId) {
      const sourceNode = editorStore.nodes.find((n) => n.id === sourceNodeId)
      if (sourceNode?.type === 'approval' && (branch === 'approved' || branch === 'rejected')) {
        const branchExists = editorStore.edges.some(
          (edge) =>
            edge.id !== reconnectingEdgeId.value &&
            edge.fromNodeId === sourceNodeId &&
            isApprovalBranchEdge(edge, branch)
        )
        if (branchExists) {
          const branchLabel = branch === 'approved' ? 'Phê duyệt (Approved)' : 'Từ chối (Rejected)'
          editorStore.showToast(`Đầu ra "${branchLabel}" đã có đường nối! Mỗi đầu ra chỉ được kết nối 1 điều kiện.`, 'error')
          return
        }
      }

      editorStore.updateEdge(reconnectingEdgeId.value, {
        fromNodeId: sourceNodeId,
        ...(branch ? { branchType: branch } : {}),
      })
      editorStore.showToast(`Đã chuyển điểm bắt đầu sang bước "${sourceNode?.name || sourceNodeId}"`, 'success')
    }
  }
}

const handleNodeMouseUp = (e: MouseEvent, node: WorkflowEditorNode) => {
  if (isDrawingEdge.value && edgeSourceNodeId.value) {
    if (edgeSourceNodeId.value !== node.id && node.type !== 'start') {
      if (edgeSourceBranch.value === 'approved') {
        editorStore.addEdge(
          edgeSourceNodeId.value,
          node.id,
          'Approved',
          [{ fieldKey: 'action', operator: 'EQUALS', compareValue: 'APPROVED', dataType: 'STRING', logicOp: 'AND' }],
          'CUSTOM',
          'approved'
        )
      } else if (edgeSourceBranch.value === 'rejected') {
        editorStore.addEdge(
          edgeSourceNodeId.value,
          node.id,
          'Rejected',
          [{ fieldKey: 'action', operator: 'EQUALS', compareValue: 'REJECTED', dataType: 'STRING', logicOp: 'AND' }],
          'CUSTOM',
          'rejected'
        )
      } else if (edgeSourceBranch.value === 'else') {
        editorStore.addEdge(
          edgeSourceNodeId.value,
          node.id,
          'ELSE',
          [{ fieldKey: 'fallback', operator: 'EQUALS', compareValue: 'ELSE', dataType: 'STRING' }],
          'CUSTOM',
          'default'
        )
      } else if (edgeSourceBranch.value === 'condition') {
        editorStore.addEdge(
          edgeSourceNodeId.value,
          node.id,
          'Nhánh điều kiện',
          [],
          'ALWAYS',
          'condition'
        )
      } else {
        editorStore.addEdge(edgeSourceNodeId.value, node.id, 'Chuyển tiếp')
      }
    }
  } else if (isReconnectingEdge.value && reconnectingEdgeId.value) {
    if (reconnectMode.value === 'target' && node.type !== 'start') {
      if (reconnectFixedNodeId.value !== node.id) {
        editorStore.updateEdge(reconnectingEdgeId.value, { toNodeId: node.id })
        editorStore.showToast(`Đã chuyển điểm kết thúc đến bước "${node.name}"`, 'success')
      }
    } else if (reconnectMode.value === 'source' && node.type !== 'end') {
      if (reconnectFixedNodeId.value !== node.id) {
        editorStore.updateEdge(reconnectingEdgeId.value, { fromNodeId: node.id })
        editorStore.showToast(`Đã chuyển điểm bắt đầu sang bước "${node.name}"`, 'success')
      }
    }
  }
}

// ==========================================================
// GLOBAL MOUSE MOVE & UP (WINDOW LEVEL)
// ==========================================================
const handleGlobalMouseMove = (e: MouseEvent) => {
  // CRITICAL SAFETY CHECK: If no mouse button is held and dragging a node, reset immediately
  if (e.buttons === 0 && isDraggingNode.value) {
    handleGlobalMouseUp()
    return
  }
  // Handle Panning
  if (isPanningCanvas.value) {
    const dx = e.clientX - panStartMousePos.value.x
    const dy = e.clientY - panStartMousePos.value.y
    editorStore.pan = {
      x: panStartPos.value.x + dx,
      y: panStartPos.value.y + dy,
    }
  }

  // Handle Node Dragging
  if (isDraggingNode.value && draggedNodeId.value) {
    const rawDx = e.clientX - dragStartMousePos.value.x
    const rawDy = e.clientY - dragStartMousePos.value.y
    const moveDist = Math.hypot(rawDx, rawDy)

    // Nếu khoảng cách di chuyển > 3px thì tính là đang kéo node
    if (moveDist > 3) {
      hasDraggedNode.value = true
    }

    const dx = rawDx / editorStore.zoom
    const dy = rawDy / editorStore.zoom
    editorStore.updateNodePosition(draggedNodeId.value, {
      x: Math.round(dragStartNodePos.value.x + dx),
      y: Math.round(dragStartNodePos.value.y + dy),
    })
  }

  // Handle Drawing Connection Line
  if (isDrawingEdge.value) {
    edgeMousePos.value = screenToCanvas(e.clientX, e.clientY)
  }

  // Handle Reconnecting Connection Line
  if (isReconnectingEdge.value) {
    reconnectMousePos.value = screenToCanvas(e.clientX, e.clientY)
  }
}

const handleGlobalMouseUp = () => {
  // Nếu có nhấn vào node
  if (isDraggingNode.value && draggedNodeId.value) {
    // Chỉ mở bảng thuộc tính khi người dùng click tĩnh (không kéo di chuyển)
    if (!hasDraggedNode.value) {
      editorStore.selectNode(draggedNodeId.value)
    }
  }

  isPanningCanvas.value = false
  isDraggingNode.value = false
  draggedNodeId.value = null
  hasDraggedNode.value = false
  isDrawingEdge.value = false
  edgeSourceNodeId.value = null
  edgeSourceBranch.value = null
  isReconnectingEdge.value = false
  reconnectingEdgeId.value = null
  reconnectMode.value = null
  reconnectFixedNodeId.value = null
}

// Zoom with Mouse Wheel
const handleWheel = (e: WheelEvent) => {
  e.preventDefault()
  if (e.ctrlKey || e.metaKey) {
    // Zoom
    if (e.deltaY < 0) {
      editorStore.zoomIn()
    } else {
      editorStore.zoomOut()
    }
  } else {
    // Pan
    editorStore.pan = {
      x: editorStore.pan.x - e.deltaX * 0.8,
      y: editorStore.pan.y - e.deltaY * 0.8,
    }
  }
}

onMounted(() => {
  window.addEventListener('mousemove', handleGlobalMouseMove)
  window.addEventListener('mouseup', handleGlobalMouseUp)
})

onUnmounted(() => {
  window.removeEventListener('mousemove', handleGlobalMouseMove)
  window.removeEventListener('mouseup', handleGlobalMouseUp)
})

// ==========================================================
// SVG PATH CALCULATION FOR BEZIER CURVES
// ==========================================================
interface EdgePathData {
  edge: WorkflowEditorEdge
  d: string
  labelX: number
  labelY: number
  sx: number
  sy: number
  tx: number
  ty: number
  isApproved: boolean
  isRejected: boolean
}

const computedEdges = computed<EdgePathData[]>(() => {
  const result: EdgePathData[] = []

  editorStore.edges.forEach((edge) => {
    const fromNode = editorStore.nodes.find((n) => n.id === edge.fromNodeId)
    const toNode = editorStore.nodes.find((n) => n.id === edge.toNodeId)

    if (!fromNode || !toNode) return

    const fromH = getNodeHeight(fromNode.type)
    const toH = getNodeHeight(toNode.type)

    const isApproved =
      edge.branchType === 'approved' ||
      (edge.label?.toLowerCase().includes('approv') ?? false) ||
      (edge.label?.toLowerCase().includes('duyệt') ?? false) ||
      (edge.conditions?.some((c) => c.compareValue === 'APPROVED') ?? false)

    const isRejected =
      edge.branchType === 'rejected' ||
      (edge.label?.toLowerCase().includes('reject') ?? false) ||
      (edge.label?.toLowerCase().includes('từ chối') ?? false) ||
      (edge.conditions?.some((c) => c.compareValue === 'REJECTED') ?? false)

    const isElse =
      edge.branchType === 'else' ||
      edge.branchType === 'default' ||
      (edge.label?.trim().toUpperCase() === 'ELSE') ||
      (edge.conditions?.some((c) => c.compareValue === 'ELSE') ?? false)

    // Source port (Right side of source node)
    const sx = fromNode.position.x + getNodeWidth(fromNode.type)
    let sy = fromNode.position.y + fromH / 2
    if (fromNode.type === 'approval') {
      if (isApproved) {
        sy = fromNode.position.y + PORT_APPROVAL_Y_APPROVED
      } else if (isRejected) {
        sy = fromNode.position.y + PORT_APPROVAL_Y_REJECTED
      }
    } else if (fromNode.type === 'condition') {
      if (isElse) {
        sy = fromNode.position.y + PORT_CONDITION_Y_ELSE
      } else {
        sy = fromNode.position.y + PORT_CONDITION_Y_IF
      }
    }

    // Target port (Left side of target node)
    const tx = toNode.position.x
    const ty = toNode.position.y + toH / 2

    // Control points for smooth horizontal S-curve
    const dx = Math.abs(tx - sx) * 0.5
    const cx1 = sx + Math.max(40, dx)
    const cy1 = sy
    const cx2 = tx - Math.max(40, dx)
    const cy2 = ty

    const d = `M ${sx} ${sy} C ${cx1} ${cy1}, ${cx2} ${cy2}, ${tx} ${ty}`

    // Label position at midpoint
    const labelX = (sx + tx) / 2
    const labelY = (sy + ty) / 2 - 12

    result.push({
      edge,
      d,
      labelX,
      labelY,
      sx,
      sy,
      tx,
      ty,
      isApproved,
      isRejected,
    })
  })

  return result
})

// Live temporary connection line while dragging from port
const liveDrawingPath = computed(() => {
  if (!isDrawingEdge.value || !edgeSourceNodeId.value) return ''

  const sourceNode = editorStore.nodes.find((n) => n.id === edgeSourceNodeId.value)
  if (!sourceNode) return ''

  const sx = sourceNode.position.x + getNodeWidth(sourceNode.type)
  let sy = sourceNode.position.y + getNodeHeight(sourceNode.type) / 2
  if (sourceNode.type === 'approval') {
    if (edgeSourceBranch.value === 'approved') {
      sy = sourceNode.position.y + PORT_APPROVAL_Y_APPROVED
    } else if (edgeSourceBranch.value === 'rejected') {
      sy = sourceNode.position.y + PORT_APPROVAL_Y_REJECTED
    }
  } else if (sourceNode.type === 'condition') {
    if (edgeSourceBranch.value === 'else') {
      sy = sourceNode.position.y + PORT_CONDITION_Y_ELSE
    } else {
      sy = sourceNode.position.y + PORT_CONDITION_Y_IF
    }
  }

  const tx = edgeMousePos.value.x
  const ty = edgeMousePos.value.y

  const dx = Math.abs(tx - sx) * 0.5
  const cx1 = sx + Math.max(40, dx)
  const cy1 = sy
  const cx2 = tx - Math.max(40, dx)
  const cy2 = ty

  return `M ${sx} ${sy} C ${cx1} ${cy1}, ${cx2} ${cy2}, ${tx} ${ty}`
})

// Live temporary line when reconnecting an existing connection
const liveReconnectingPath = computed(() => {
  if (!isReconnectingEdge.value || !reconnectingEdgeId.value || !reconnectFixedNodeId.value) return ''

  const fixedNode = editorStore.nodes.find((n) => n.id === reconnectFixedNodeId.value)
  if (!fixedNode) return ''

  let sx: number, sy: number, tx: number, ty: number

  if (reconnectMode.value === 'target') {
    // Fixed node is source
    const edgeObj = editorStore.edges.find((e) => e.id === reconnectingEdgeId.value)
    const isApp = edgeObj?.branchType === 'approved' || (edgeObj?.label?.toLowerCase().includes('approv') ?? false)
    const isRej = edgeObj?.branchType === 'rejected' || (edgeObj?.label?.toLowerCase().includes('reject') ?? false)

    sx = fixedNode.position.x + getNodeWidth(fixedNode.type)
    if (fixedNode.type === 'approval') {
      sy = fixedNode.position.y + (isApp ? PORT_APPROVAL_Y_APPROVED : isRej ? PORT_APPROVAL_Y_REJECTED : 34)
    } else {
      sy = fixedNode.position.y + getNodeHeight(fixedNode.type) / 2
    }
    tx = reconnectMousePos.value.x
    ty = reconnectMousePos.value.y
  } else {
    // Fixed node is target
    sx = reconnectMousePos.value.x
    sy = reconnectMousePos.value.y
    tx = fixedNode.position.x
    ty = fixedNode.position.y + getNodeHeight(fixedNode.type) / 2
  }

  const dx = Math.abs(tx - sx) * 0.5
  const cx1 = sx + Math.max(40, dx)
  const cy1 = sy
  const cx2 = tx - Math.max(40, dx)
  const cy2 = ty

  return `M ${sx} ${sy} C ${cx1} ${cy1}, ${cx2} ${cy2}, ${tx} ${ty}`
})

// Helper for palette metadata
function getNodeTypeMeta(type: EditorNodeType) {
  return (
    editorStore.nodePalette.find((p) => p.type === type) || {
      label: type,
      title: type,
      icon: '⚙',
      color: '#64748b',
      bg: '#f8fafc',
    }
  )
}

function getApprovalNodeSubtitle(node: WorkflowEditorNode): string {
  const cfg = node.config as any
  if (!cfg) return 'Phê duyệt'

  if (cfg.approvalMode === 'multi') {
    const isUsers = cfg.multiAssigneeType !== 'roles'
    const count = isUsers ? (cfg.approverUserIds?.length || 0) : (cfg.approverRoles?.length || 0)
    const unit = isUsers ? 'người' : 'vai trò'

    let ruleText = '100% duyệt'
    if (cfg.multiApprovalRule === 'half') ruleText = '≥ 50%'
    else if (cfg.multiApprovalRule === 'majority') ruleText = 'Quá bán'
    else if (cfg.multiApprovalRule === 'threshold') ruleText = `≥ ${cfg.approvalThreshold || 1}`
    else if (cfg.multiApprovalRule === 'any') ruleText = 'Bất kỳ 1 ai'

    return `Hội đồng (${count} ${unit} • ${ruleText})`
  }

  if (cfg.approverRole) return cfg.approverRole
  if (cfg.approverType === 'manager') return 'Quản lý trực tiếp'
  if (cfg.approverType === 'user') return 'Người chỉ định'
  return 'Phê duyệt'
}
</script>

<template>
  <div
    ref="canvasContainerRef"
    class="editor-canvas-container"
    :class="{ 'is-panning': isPanningCanvas }"
    @mousedown="handleCanvasMouseDown"
    @dragover="handleCanvasDragOver"
    @drop="handleCanvasDrop"
    @wheel="handleWheel"
  >
    <!-- GRID CANVAS STAGE (SCALED & TRANSLATED) -->
    <div
      class="canvas-stage"
      :style="{
        transform: `translate(${editorStore.pan.x}px, ${editorStore.pan.y}px) scale(${editorStore.zoom})`,
      }"
    >
      <!-- Background SVG with Dot Grid & Connections -->
      <svg class="canvas-svg-layer">
        <defs>
          <!-- Grid Pattern (Engineering Blueprint Dots) -->
          <pattern id="canvas-grid" width="24" height="24" patternUnits="userSpaceOnUse">
            <circle cx="2" cy="2" r="1.15" fill="#94a3b8" opacity="0.45" />
          </pattern>

          <!-- Marker Arrow Normal -->
          <marker
            id="edge-arrow"
            viewBox="0 0 10 10"
            refX="8"
            refY="5"
            markerWidth="6"
            markerHeight="6"
            orient="auto-start-reverse"
          >
            <path d="M 0 1.5 L 8 5 L 0 8.5 z" fill="#6366f1" />
          </marker>

          <!-- Marker Arrow Selected -->
          <marker
            id="edge-arrow-selected"
            viewBox="0 0 10 10"
            refX="8"
            refY="5"
            markerWidth="6"
            markerHeight="6"
            orient="auto-start-reverse"
          >
            <path d="M 0 1.5 L 8 5 L 0 8.5 z" fill="#4f46e5" />
          </marker>

          <!-- Marker Arrow Approved (Green) -->
          <marker
            id="edge-arrow-approved"
            viewBox="0 0 10 10"
            refX="8"
            refY="5"
            markerWidth="6"
            markerHeight="6"
            orient="auto-start-reverse"
          >
            <path d="M 0 1.5 L 8 5 L 0 8.5 z" fill="#10b981" />
          </marker>

          <!-- Marker Arrow Rejected (Red) -->
          <marker
            id="edge-arrow-rejected"
            viewBox="0 0 10 10"
            refX="8"
            refY="5"
            markerWidth="6"
            markerHeight="6"
            orient="auto-start-reverse"
          >
            <path d="M 0 1.5 L 8 5 L 0 8.5 z" fill="#ef4444" />
          </marker>
        </defs>

        <!-- Dot Grid Fill -->
        <rect class="canvas-background" x="-5000" y="-5000" width="10000" height="10000" fill="url(#canvas-grid)" />

        <!-- Rendered Edges / Connections -->
        <g class="edges-group">
          <g
            v-for="item in computedEdges"
            :key="item.edge.id"
            class="edge-item-group"
            :class="{
              selected: editorStore.selectedEdgeId === item.edge.id,
              'is-being-reconnected': isReconnectingEdge && reconnectingEdgeId === item.edge.id,
              'is-edge-approved': item.isApproved,
              'is-edge-rejected': item.isRejected,
            }"
            @click.stop="editorStore.selectEdge(item.edge.id)"
          >
            <!-- Invisible wide stroke for easier clicking -->
            <path :d="item.d" class="edge-click-area" />

            <!-- Visible Edge Line -->
            <path
              :d="item.d"
              class="edge-path"
              :class="{
                'edge-approved': item.isApproved,
                'edge-rejected': item.isRejected,
              }"
              :marker-end="
                editorStore.selectedEdgeId === item.edge.id
                  ? 'url(#edge-arrow-selected)'
                  : item.isApproved
                  ? 'url(#edge-arrow-approved)'
                  : item.isRejected
                  ? 'url(#edge-arrow-rejected)'
                  : 'url(#edge-arrow)'
              "
            />

            <!-- Edge Condition Label Pill -->
            <g v-if="item.edge.label" :transform="`translate(${item.labelX}, ${item.labelY})`" class="edge-label-group">
              <rect
                x="-48"
                y="-12"
                width="96"
                height="24"
                rx="12"
                class="edge-label-bg"
                :class="{
                  'label-bg-approved': item.isApproved,
                  'label-bg-rejected': item.isRejected,
                }"
              />
              <text
                x="0"
                y="4"
                text-anchor="middle"
                class="edge-label-text"
                :class="{
                  'label-text-approved': item.isApproved,
                  'label-text-rejected': item.isRejected,
                }"
              >
                {{ item.edge.label }}
              </text>
            </g>

            <!-- Interactive Endpoint Reconnect Handles (Visible when edge selected) -->
            <g v-if="editorStore.selectedEdgeId === item.edge.id && !isReconnectingEdge" class="edge-endpoint-handles">
              <!-- Source Handle (Kéo để đổi Node xuất phát) -->
              <circle
                :cx="item.sx"
                :cy="item.sy"
                r="6"
                class="reconnect-handle source-handle"
                title="Nhấp giữ & kéo để chuyển điểm bắt đầu sang bước khác"
                @mousedown.stop="startReconnectingEdge($event, item.edge, 'source')"
              />

              <!-- Target Handle (Kéo để đổi Node đích đến) -->
              <circle
                :cx="item.tx"
                :cy="item.ty"
                r="6"
                class="reconnect-handle target-handle"
                title="Nhấp giữ & kéo để chuyển điểm kết thúc sang bước khác"
                @mousedown.stop="startReconnectingEdge($event, item.edge, 'target')"
              />
            </g>
          </g>

          <!-- Live Temporary Line while connecting -->
          <path
            v-if="liveDrawingPath"
            :d="liveDrawingPath"
            class="live-drawing-edge"
            :class="{
              'is-branch-approved': edgeSourceBranch === 'approved',
              'is-branch-rejected': edgeSourceBranch === 'rejected',
            }"
          />

          <!-- Live Temporary Line while reconnecting existing edge -->
          <path
            v-if="liveReconnectingPath"
            :d="liveReconnectingPath"
            class="live-reconnecting-edge"
            marker-end="url(#edge-arrow-selected)"
          />
        </g>
      </svg>

      <!-- Rendered Step Nodes -->
      <div class="nodes-layer">
        <div
          v-for="node in editorStore.nodes"
          :key="node.id"
          class="workflow-node-card"
          :class="[
            node.type,
            {
              selected: editorStore.selectedNodeId === node.id,
              'is-branching-card': isBranchingNode(node.type),
            },
          ]"
          :style="{
            left: `${node.position.x}px`,
            top: `${node.position.y}px`,
            width: `${getNodeWidth(node.type)}px`,
            height: `${getNodeHeight(node.type)}px`,
            '--node-accent': getNodeTypeMeta(node.type).color,
            '--node-bg': getNodeTypeMeta(node.type).bg,
          }"
          @mousedown="handleNodeMouseDown($event, node)"
          @mouseup="handleNodeMouseUp($event, node)"
        >
          <!-- Left Input Port (Not on Start Node) -->
          <div
            v-if="node.type !== 'start'"
            class="port-handle port-input"
            title="Kéo đến đây để kết nối"
            @mousedown.stop
            @mouseup="handlePortMouseUp($event, node.id)"
          >
            <div class="port-dot"></div>
          </div>

          <!-- Node Header & Icon -->
          <div class="node-accent-bar"></div>
          <div class="node-body" :class="{ 'body-approval': node.type === 'approval' }">
            <div class="node-icon-box">
              <span>{{ getNodeTypeMeta(node.type).icon }}</span>
            </div>

            <div v-if="!isBranchingNode(node.type)" class="node-info-col">
              <span
                class="node-card-label"
                :title="node.name || getNodeTypeMeta(node.type).label || node.type"
              >
                {{ node.name || getNodeTypeMeta(node.type).label || node.type }}
              </span>
              <span v-if="node.type === 'approval'" class="node-sub-meta" :title="getApprovalNodeSubtitle(node)">
                {{ getApprovalNodeSubtitle(node) }}
              </span>
            </div>
          </div>

          <!-- SPECIAL FOR APPROVAL: 2 Output Heads (1 Approved, 1 Rejected) -->
          <template v-if="node.type === 'approval'">
            <!-- Approved Output Head (Top-Right) -->
            <div
              class="port-handle port-approval-head port-approved-head"
              :class="{ 'is-connected': hasApprovalBranchEdge(node.id, 'approved') }"
              :title="
                hasApprovalBranchEdge(node.id, 'approved')
                  ? 'Đầu ra Phê duyệt đã có kết nối (mỗi đầu ra chỉ được kéo 1 điều kiện)'
                  : 'Kéo từ đây sang bước tiếp theo khi DUYỆT (Approved)'
              "
              @mousedown="handlePortMouseDown($event, node.id, 'approved')"
              @mouseup="handleOutputPortMouseUp($event, node.id, 'approved')"
            >
              <div class="port-dot dot-approved"></div>
            </div>

            <!-- Rejected Output Head (Bottom-Right) -->
            <div
              class="port-handle port-approval-head port-rejected-head"
              :class="{ 'is-connected': hasApprovalBranchEdge(node.id, 'rejected') }"
              :title="
                hasApprovalBranchEdge(node.id, 'rejected')
                  ? 'Đầu ra Từ chối đã có kết nối (mỗi đầu ra chỉ được kéo 1 điều kiện)'
                  : 'Kéo từ đây sang bước tiếp theo khi TỪ CHỐI (Rejected)'
              "
              @mousedown="handlePortMouseDown($event, node.id, 'rejected')"
              @mouseup="handleOutputPortMouseUp($event, node.id, 'rejected')"
            >
              <div class="port-dot dot-rejected"></div>
            </div>
          </template>

          <!-- SPECIAL FOR CONDITION: 2 Output Ports (Condition Branch & ELSE Fallback) -->
          <template v-else-if="node.type === 'condition'">
            <!-- Top Output Port: Nhánh điều kiện -->
            <div
              class="port-handle port-condition-head port-if-head"
              title="Nhánh điều kiện"
              @mousedown="handlePortMouseDown($event, node.id, 'condition')"
              @mouseup="handleOutputPortMouseUp($event, node.id, 'condition')"
            >
              <div class="port-dot dot-condition-if"></div>
            </div>

            <!-- Bottom Output Port: ELSE (Fallback) -->
            <div
              class="port-handle port-condition-head port-else-head"
              title="Port dưới: ELSE (Fallback xử lý khi không có điều kiện nào thỏa mãn)"
              @mousedown="handlePortMouseDown($event, node.id, 'else')"
              @mouseup="handleOutputPortMouseUp($event, node.id, 'else')"
            >
              <div class="port-dot dot-condition-else"></div>
            </div>
          </template>

          <!-- Standard Right Output Port for other nodes (Not on End Node) -->
          <div
            v-else-if="node.type !== 'end'"
            class="port-handle port-output"
            title="Kéo từ đây sang bước kế tiếp"
            @mousedown="handlePortMouseDown($event, node.id)"
            @mouseup="handleOutputPortMouseUp($event, node.id)"
          >
            <div class="port-dot"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- BOTTOM-LEFT: MINIMAP -->
    <div class="canvas-minimap-card">
      <div class="minimap-header">MINIMAP</div>
      <div class="minimap-viewport">
        <div
          v-for="n in editorStore.nodes"
          :key="n.id"
          class="minimap-node-dot"
          :style="{
            left: `${(n.position.x / 1600) * 120}px`,
            top: `${(n.position.y / 1000) * 80}px`,
            background: getNodeTypeMeta(n.type).color,
          }"
        ></div>
      </div>
    </div>

    <!-- BOTTOM-RIGHT: FLOATING ZOOM CONTROLS -->
    <div class="canvas-controls-bar">
      <!-- Zoom Out -->
      <button class="ctrl-btn" title="Thu nhỏ (Zoom out)" @click="editorStore.zoomOut">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8"></circle>
          <line x1="8" y1="11" x2="14" y2="11"></line>
        </svg>
      </button>

      <!-- Zoom Value / Reset -->
      <button class="ctrl-btn zoom-val" title="Đặt lại 100%" @click="editorStore.resetZoom">
        {{ Math.round(editorStore.zoom * 100) }}%
      </button>

      <!-- Zoom In -->
      <button class="ctrl-btn" title="Phóng to (Zoom in)" @click="editorStore.zoomIn">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8"></circle>
          <line x1="11" y1="8" x2="11" y2="14"></line>
          <line x1="8" y1="11" x2="14" y2="11"></line>
        </svg>
      </button>

      <div class="ctrl-separator"></div>

      <!-- Fit to Screen -->
      <button class="ctrl-btn" title="Hiển thị toàn bộ sơ đồ (Fit to Screen)" @click="editorStore.fitToScreen">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="15 3 21 3 21 9"></polyline>
          <polyline points="9 21 3 21 3 15"></polyline>
          <line x1="21" y1="3" x2="14" y2="10"></line>
          <line x1="3" y1="21" x2="10" y2="14"></line>
        </svg>
      </button>
    </div>
  </div>
</template>

<style scoped>
.editor-canvas-container {
  flex: 1;
  height: 100%;
  position: relative;
  overflow: hidden;
  background-color: #f1f5f9;
  background-image: radial-gradient(#e2e8f0 1px, transparent 1px);
  background-size: 24px 24px;
  cursor: grab;
  user-select: none;
}

.editor-canvas-container.is-panning {
  cursor: grabbing;
}

.canvas-stage {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  transform-origin: 0 0;
}

/* SVG Layer */
.canvas-svg-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: visible;
  pointer-events: auto;
}

.canvas-background {
  cursor: grab;
}

/* Edges / Connections */
.edge-item-group {
  cursor: pointer;
}

.edge-click-area {
  fill: none;
  stroke: transparent;
  stroke-width: 20;
}

.edge-path {
  fill: none;
  stroke: #94a3b8;
  stroke-width: 2.2;
  transition: stroke 0.2s ease, stroke-width 0.2s ease;
}

.edge-item-group:hover .edge-path {
  stroke: #6366f1;
  stroke-width: 3;
}

.edge-item-group.selected .edge-path {
  stroke: #4f46e5;
  stroke-width: 3.5;
}

.edge-path.edge-approved {
  stroke: #10b981;
  stroke-width: 2.2;
}

.edge-item-group:hover .edge-path.edge-approved {
  stroke: #059669;
  stroke-width: 3;
}

.edge-item-group.selected .edge-path.edge-approved {
  stroke: #047857;
  stroke-width: 3.5;
}

.edge-path.edge-rejected {
  stroke: #ef4444;
  stroke-width: 2.2;
}

.edge-item-group:hover .edge-path.edge-rejected {
  stroke: #dc2626;
  stroke-width: 3;
}

.edge-item-group.selected .edge-path.edge-rejected {
  stroke: #b91c1c;
  stroke-width: 3.5;
}

.edge-label-bg {
  fill: #ffffff;
  stroke: #cbd5e1;
  stroke-width: 1;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.edge-item-group:hover .edge-label-bg,
.edge-item-group.selected .edge-label-bg {
  stroke: #6366f1;
}

.edge-label-bg.label-bg-approved {
  fill: #ecfdf5;
  stroke: #10b981;
  stroke-width: 1.5;
}

.edge-label-bg.label-bg-rejected {
  fill: #fef2f2;
  stroke: #ef4444;
  stroke-width: 1.5;
}

.edge-label-text {
  font-size: 10px;
  font-weight: 600;
  fill: #475569;
  font-family: inherit;
  pointer-events: none;
}

.edge-label-text.label-text-approved {
  fill: #065f46;
  font-weight: 700;
}

.edge-label-text.label-text-rejected {
  fill: #991b1b;
  font-weight: 700;
}

.live-drawing-edge {
  fill: none;
  stroke: #6366f1;
  stroke-width: 2.5;
  stroke-dasharray: 6, 6;
  animation: dash 1s linear infinite;
}

.live-drawing-edge.is-branch-approved {
  stroke: #10b981;
}

.live-drawing-edge.is-branch-rejected {
  stroke: #ef4444;
}

/* Reconnect Live Line & Handles */
.live-reconnecting-edge {
  fill: none;
  stroke: #4f46e5;
  stroke-width: 3;
  stroke-dasharray: 6, 4;
  animation: dash 1s linear infinite;
}

.edge-item-group.is-being-reconnected .edge-path,
.edge-item-group.is-being-reconnected .edge-label-group {
  opacity: 0.25;
}

.reconnect-handle {
  fill: #ffffff;
  stroke: #4f46e5;
  stroke-width: 2.5;
  cursor: grab;
  transition: transform 0.15s ease, fill 0.15s ease;
  filter: drop-shadow(0 2px 5px rgba(0, 0, 0, 0.25));
}

.reconnect-handle:hover {
  fill: #4f46e5;
  stroke: #ffffff;
  transform: scale(1.35);
  cursor: grabbing;
}

@keyframes dash {
  to {
    stroke-dashoffset: -12;
  }
}

/* ==========================================================
   NODE CARDS
   ========================================================== */
.nodes-layer {
  position: absolute;
  top: 0;
  left: 0;
  pointer-events: none;
}

.workflow-node-card {
  position: absolute;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  box-shadow: 0 3px 10px rgba(15, 23, 42, 0.05);
  pointer-events: auto;
  cursor: move;
  display: flex;
  flex-direction: column;
  overflow: visible;
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
}

.workflow-node-card:hover {
  border-color: var(--node-accent);
  box-shadow: 0 6px 18px rgba(99, 102, 241, 0.15);
}

.workflow-node-card.selected {
  border-color: var(--node-accent);
  box-shadow: 0 0 0 2.5px var(--node-accent), 0 8px 20px rgba(0, 0, 0, 0.12);
}

.workflow-node-card.is-branching-card .node-body {
  padding: 0;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
}

.workflow-node-card.is-branching-card .node-icon-box {
  margin: 0 auto;
}

.node-accent-bar {
  height: 3px;
  background: var(--node-accent);
  border-radius: 10px 10px 0 0;
}

.node-body {
  flex: 1;
  padding: 0 0.75rem;
  display: flex;
  align-items: center;
  gap: 0.625rem;
  min-width: 0;
  overflow: hidden;
}

.node-info-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 1px;
}

.node-sub-meta {
  font-size: 0.6875rem;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.node-icon-box {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: var(--node-bg);
  color: var(--node-accent);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
  font-weight: 700;
  flex-shrink: 0;
}

.node-card-label {
  flex: 1;
  min-width: 0;
  font-size: 0.8125rem;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Ports / Connector Handles */
.port-handle {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: crosshair;
  z-index: 10;
}

.port-input {
  left: -11px;
}

.port-output {
  right: -11px;
}

/* Ports for Approval Head (Dual Approved & Rejected Buttons matching Image 1) */
.port-approval-head {
  position: absolute;
  right: -11px;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: crosshair;
  z-index: 10;
}

.port-approved-head {
  top: 20px;
  bottom: auto;
  transform: translateY(-50%);
}

.port-rejected-head {
  top: 48px;
  bottom: auto;
  transform: translateY(-50%);
}

.port-dot.dot-approved {
  border-color: #10b981;
  background: #ffffff;
}

.port-approval-head:hover .dot-approved {
  background: #10b981;
  transform: scale(1.35);
}

.port-dot.dot-rejected {
  border-color: #ef4444;
  background: #ffffff;
}

.port-approval-head:hover .dot-rejected {
  background: #ef4444;
  transform: scale(1.35);
}

/* When approval output port is already connected */
.port-approval-head.is-connected {
  cursor: not-allowed;
}

.port-approval-head.is-connected .dot-approved {
  background: #10b981;
}

.port-approval-head.is-connected .dot-rejected {
  background: #ef4444;
}

.port-approval-head.is-connected:hover .dot-approved {
  transform: scale(1.1);
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.35);
}

.port-approval-head.is-connected:hover .dot-rejected {
  transform: scale(1.1);
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.35);
}

/* Ports for Condition Node (Top: Nhánh điều kiện, Bottom: ELSE) */
.port-condition-head {
  position: absolute;
  right: -11px;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: crosshair;
  z-index: 10;
}

.port-if-head {
  top: 20px;
  bottom: auto;
  transform: translateY(-50%);
}

.port-else-head {
  top: 48px;
  bottom: auto;
  transform: translateY(-50%);
}

.port-dot.dot-condition-if {
  border-color: #ec4899;
  background: #ffffff;
}

.port-condition-head:hover .dot-condition-if {
  background: #ec4899;
  transform: scale(1.35);
}

.port-dot.dot-condition-else {
  border-color: #f59e0b;
  background: #ffffff;
}

.port-condition-head:hover .dot-condition-else {
  background: #f59e0b;
  transform: scale(1.35);
}

.port-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #ffffff;
  border: 2px solid var(--node-accent, #6366f1);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.15);
  transition: all 0.15s ease;
}

.port-handle:hover .port-dot {
  background: var(--node-accent, #6366f1);
  transform: scale(1.35);
}

/* ==========================================================
   MINIMAP (BOTTOM-LEFT)
   ========================================================== */
.canvas-minimap-card {
  position: absolute;
  bottom: 1.25rem;
  left: 1.25rem;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  padding: 0.5rem;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  user-select: none;
}

.minimap-header {
  font-size: 0.625rem;
  font-weight: 800;
  color: #94a3b8;
  letter-spacing: 0.08em;
  margin-bottom: 0.35rem;
}

.minimap-viewport {
  width: 140px;
  height: 90px;
  background: #f1f5f9;
  border-radius: 6px;
  position: relative;
  overflow: hidden;
  border: 1px dashed #cbd5e1;
}

.minimap-node-dot {
  position: absolute;
  width: 16px;
  height: 8px;
  border-radius: 2px;
}

/* ==========================================================
   FLOATING ZOOM CONTROLS (BOTTOM-RIGHT)
   ========================================================== */
.canvas-controls-bar {
  position: absolute;
  bottom: 1.25rem;
  right: 1.25rem;
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  padding: 3px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  z-index: 20;
}

.ctrl-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: #475569;
  transition: all 0.15s ease;
}

.ctrl-btn:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.ctrl-btn.zoom-val {
  width: auto;
  padding: 0 0.5rem;
  font-size: 0.75rem;
  font-weight: 700;
  font-family: 'JetBrains Mono', monospace;
}

.ctrl-separator {
  width: 1px;
  height: 18px;
  background: #e2e8f0;
  margin: 0 2px;
}
</style>
