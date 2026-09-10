<script setup lang="ts">
import { computed } from 'vue'
import type { TicketDetail, TaskDetail } from '@/types/ticket'

const props = defineProps<{
  ticket: TicketDetail
}>()

interface StepItem {
  id: string
  name: string
  type: string
  status: 'completed' | 'active' | 'rejected' | 'pending'
  assigneeName?: string
  completedAt?: string
  comment?: string
  task?: TaskDetail
}

const steps = computed<StepItem[]>(() => {
  const result: StepItem[] = []
  const graphNodes = props.ticket.graph?.nodes || []
  const tasks = props.ticket.taskHistory || []

  // 1. Bước Start
  const startNode = graphNodes.find((n) => n.type.toLowerCase() === 'start')
  result.push({
    id: startNode?.id || 'start',
    name: startNode?.name || 'Bắt đầu',
    type: 'start',
    status: 'completed',
    completedAt: props.ticket.startTime,
    assigneeName: props.ticket.creator?.fullName || 'Người khởi tạo',
  })

  // 2. Các bước trung gian từ Task History hoặc Graph Nodes
  tasks.forEach((t) => {
    let s: 'completed' | 'active' | 'rejected' | 'pending' = 'pending'
    if (t.status === 'Approved') s = 'completed'
    else if (t.status === 'Rejected') s = 'rejected'
    else if (t.status === 'Pending') s = 'active'

    result.push({
      id: t.clientNodeId,
      name: t.nodeName,
      type: t.nodeType.toLowerCase(),
      status: s,
      assigneeName: t.assignedUser?.fullName || 'Chưa phân công',
      completedAt: t.completedAt || undefined,
      comment: t.comment,
      task: t,
    })
  })

  // 3. Nếu ticket còn đang chạy và có các node tiếp theo trong graph chưa tới
  const completedOrActiveClientIds = [
    ...(props.ticket.completedNodeClientIds || []),
    props.ticket.activeNodeClientId,
  ].filter(Boolean)

  graphNodes.forEach((gn) => {
    const tLower = gn.type.toLowerCase()
    if (tLower === 'start' || tLower === 'end') return
    const isAlreadyInTasks = tasks.some((t) => t.clientNodeId === gn.id)
    if (!isAlreadyInTasks && !completedOrActiveClientIds.includes(gn.id)) {
      result.push({
        id: gn.id,
        name: gn.name,
        type: tLower,
        status: 'pending',
      })
    }
  })

  // 4. Bước End
  const endNode = graphNodes.find((n) => n.type.toLowerCase() === 'end')
  let endStatus: 'completed' | 'rejected' | 'pending' = 'pending'
  if (props.ticket.status === 'Completed') endStatus = 'completed'
  else if (props.ticket.status === 'Rejected') endStatus = 'rejected'

  result.push({
    id: endNode?.id || 'end',
    name: endNode?.name || 'Kết thúc',
    type: 'end',
    status: endStatus,
    completedAt: props.ticket.endTime || undefined,
  })

  return result
})

const formatDate = (dateStr?: string) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')} - ${d.getDate()}/${d.getMonth() + 1}`
}

const getNodeIcon = (type: string, status: string) => {
  if (status === 'completed') return '✓'
  if (status === 'rejected') return '✕'
  if (type === 'start') return '▶'
  if (type === 'end') return '⏹'
  if (type === 'approval') return '📋'
  if (type === 'review') return '👁'
  if (type === 'assignment') return '👤'
  if (type === 'notification') return '🔔'
  if (type === 'system_action') return '⚡'
  return '●'
}
</script>

<template>
  <div class="step-tracker-card">
    <div class="tracker-header">
      <h3 class="tracker-title">Tiến trình Xử lý (Workflow Execution Path)</h3>
      <div class="tracker-badges">
        <span class="legend-item"><span class="legend-dot completed"></span> Đã hoàn tất</span>
        <span class="legend-item"><span class="legend-dot active"></span> Đang chờ duyệt</span>
        <span class="legend-item"><span class="legend-dot pending"></span> Chưa tới</span>
      </div>
    </div>

    <!-- Horizontal Step Bar -->
    <div class="stepper-wrapper">
      <div
        v-for="(step, idx) in steps"
        :key="step.id + idx"
        class="step-item"
        :class="[step.status, `type-${step.type}`]"
      >
        <!-- Connector Line before this step -->
        <div v-if="idx > 0" class="step-line" :class="{ filled: step.status === 'completed' || step.status === 'active' }"></div>

        <!-- Step Node Circle -->
        <div class="step-node-bubble" :title="`${step.name} (${step.status})`">
          <span class="step-icon">{{ getNodeIcon(step.type, step.status) }}</span>
          <span v-if="step.status === 'active'" class="pulse-ring"></span>
        </div>

        <!-- Step Details Text -->
        <div class="step-info">
          <span class="step-name">{{ step.name }}</span>
          <span v-if="step.assigneeName" class="step-assignee">
            {{ step.assigneeName }}
          </span>
          <span v-if="step.completedAt" class="step-time">
            {{ formatDate(step.completedAt) }}
          </span>
          <span v-else-if="step.status === 'active'" class="step-status-tag active">
            Đang xử lý
          </span>
          <span v-else-if="step.status === 'pending'" class="step-status-tag pending">
            Chờ chuyển bước
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.step-tracker-card {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  padding: 1.5rem;
  box-shadow: 0 4px 16px -2px rgba(0, 0, 0, 0.05);
}

.tracker-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.75rem;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.tracker-title {
  font-size: 1.0625rem;
  font-weight: 700;
  color: #0f172a;
}

.tracker-badges {
  display: flex;
  align-items: center;
  gap: 1rem;
  font-size: 0.75rem;
  color: #64748b;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 0.375rem;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.legend-dot.completed { background: #10b981; }
.legend-dot.active { background: #0284c7; }
.legend-dot.pending { background: #cbd5e1; }

/* ==========================================================
   HORIZONTAL STEPPER
   ========================================================== */
.stepper-wrapper {
  display: flex;
  align-items: flex-start;
  position: relative;
  overflow-x: auto;
  padding-bottom: 0.5rem;
}

.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  min-width: 130px;
  flex: 1;
  position: relative;
}

/* Connector Line */
.step-line {
  position: absolute;
  top: 18px;
  right: 50%;
  width: 100%;
  height: 3px;
  background: #e2e8f0;
  z-index: 1;
  transition: all 0.3s ease;
}

.step-line.filled {
  background: #10b981;
}

.step-item.active .step-line {
  background: linear-gradient(90deg, #10b981 0%, #0284c7 100%);
}

.step-node-bubble {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: #ffffff;
  border: 2px solid #cbd5e1;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
  font-weight: 700;
  position: relative;
  z-index: 2;
  transition: all 0.3s ease;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
}

/* Completed Step */
.step-item.completed .step-node-bubble {
  background: #10b981;
  border-color: #059669;
  color: #ffffff;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
}

/* Active Step */
.step-item.active .step-node-bubble {
  background: #0284c7;
  border-color: #0369a1;
  color: #ffffff;
  box-shadow: 0 0 0 4px rgba(2, 132, 199, 0.25);
  transform: scale(1.1);
}

/* Rejected Step */
.step-item.rejected .step-node-bubble {
  background: #ef4444;
  border-color: #dc2626;
  color: #ffffff;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.2);
}

/* Pending Step */
.step-item.pending .step-node-bubble {
  background: #f8fafc;
  border-color: #cbd5e1;
  color: #94a3b8;
}

/* Pulse animation on Active step */
.pulse-ring {
  position: absolute;
  top: -4px;
  left: -4px;
  right: -4px;
  bottom: -4px;
  border-radius: 50%;
  border: 2px solid #0284c7;
  animation: pulse-ring 1.8s cubic-bezier(0.215, 0.61, 0.355, 1) infinite;
}

@keyframes pulse-ring {
  0% {
    transform: scale(0.95);
    opacity: 0.8;
  }
  50% {
    transform: scale(1.35);
    opacity: 0;
  }
  100% {
    transform: scale(1.35);
    opacity: 0;
  }
}

.step-info {
  margin-top: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  max-width: 140px;
}

.step-name {
  font-size: 0.8125rem;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.3;
}

.step-assignee {
  font-size: 0.75rem;
  color: #475569;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.step-time {
  font-size: 0.6875rem;
  color: #10b981;
  font-weight: 600;
}

.step-status-tag {
  display: inline-block;
  font-size: 0.6875rem;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 4px;
  margin: 2px auto 0;
}

.step-status-tag.active {
  background: #e0f2fe;
  color: #0284c7;
}

.step-status-tag.pending {
  background: #f1f5f9;
  color: #94a3b8;
}
</style>
