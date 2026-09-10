<script setup lang="ts">
import { computed } from 'vue'
import type { TicketDetail } from '@/types/ticket'

const props = defineProps<{
  ticket: TicketDetail
}>()

const nodes = computed(() => {
  return props.ticket.graph?.nodes || []
})

const edges = computed(() => {
  return props.ticket.graph?.edges || []
})

// Tính toán bounding box để canvas tự co giãn gọn gàng
const viewBox = computed(() => {
  if (nodes.value.length === 0) return '0 0 800 400'

  let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity
  nodes.value.forEach((n) => {
    const x = n.position?.x || 100
    const y = n.position?.y || 100
    minX = Math.min(minX, x)
    minY = Math.min(minY, y)
    maxX = Math.max(maxX, x + 180)
    maxY = Math.max(maxY, y + 90)
  })

  const width = Math.max(700, maxX - minX + 100)
  const height = Math.max(350, maxY - minY + 100)
  return `${minX - 50} ${minY - 50} ${width} ${height}`
})

const getNodeState = (nodeId: string) => {
  if (props.ticket.activeNodeClientId === nodeId) return 'active'
  if (props.ticket.completedNodeClientIds?.includes(nodeId)) return 'completed'
  return 'pending'
}

const getNodeColor = (type: string) => {
  switch (type.toLowerCase()) {
    case 'start': return '#10b981'
    case 'end': return '#ef4444'
    case 'approval': return '#6366f1'
    case 'review': return '#3b82f6'
    case 'assignment': return '#f59e0b'
    case 'notification': return '#06b6d4'
    case 'system_action': return '#8b5cf6'
    default: return '#64748b'
  }
}

// Tính tọa độ đường nối Bezier
const getEdgePath = (fromNodeId: string, toNodeId: string) => {
  const from = nodes.value.find((n) => n.id === fromNodeId)
  const to = nodes.value.find((n) => n.id === toNodeId)
  if (!from || !to) return ''

  const x1 = (from.position?.x || 0) + 160
  const y1 = (from.position?.y || 0) + 40
  const x2 = (to.position?.x || 0)
  const y2 = (to.position?.y || 0) + 40

  const dx = Math.abs(x2 - x1) * 0.5
  return `M ${x1} ${y1} C ${x1 + dx} ${y1}, ${x2 - dx} ${y2}, ${x2} ${y2}`
}
</script>

<template>
  <div v-if="false"></div>
</template>

<style scoped>
.mini-canvas-card {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  padding: 1.5rem;
  box-shadow: 0 4px 16px -2px rgba(0, 0, 0, 0.05);
}

.canvas-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.canvas-title {
  font-size: 1.0625rem;
  font-weight: 700;
  color: #0f172a;
}

.canvas-hint {
  font-size: 0.75rem;
  color: #64748b;
}

.svg-container {
  width: 100%;
  height: 280px;
  background: radial-gradient(#cbd5e1 1px, transparent 1px);
  background-size: 20px 20px;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  overflow: hidden;
}

.workflow-svg {
  width: 100%;
  height: 100%;
}

.edge-path {
  fill: none;
  stroke: #cbd5e1;
  stroke-width: 2;
  transition: all 0.3s ease;
}

.edge-path.edge-active {
  stroke: #0284c7;
  stroke-width: 2.5;
  stroke-dasharray: 6 3;
  animation: dash 1s linear infinite;
}

@keyframes dash {
  to {
    stroke-dashoffset: -9;
  }
}

.node-box {
  fill: #ffffff;
  stroke: #e2e8f0;
  stroke-width: 1.5;
  transition: all 0.3s ease;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.04));
}

.node-text-title {
  font-size: 12px;
  font-weight: 700;
  fill: #0f172a;
  font-family: inherit;
}

.node-text-type {
  font-size: 10px;
  fill: #64748b;
  text-transform: uppercase;
  font-family: inherit;
}

/* Active Node: Glowing pulse halo */
.node-group.active .node-box {
  stroke: #0284c7;
  stroke-width: 2.5;
  filter: drop-shadow(0 0 10px rgba(2, 132, 199, 0.45));
}

.active-badge-pulse {
  animation: pulse-dot 1.5s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.3); }
}

/* Completed Node */
.node-group.completed .node-box {
  stroke: #10b981;
  stroke-width: 1.5;
}

.node-group.pending .node-box {
  opacity: 0.75;
}
</style>
