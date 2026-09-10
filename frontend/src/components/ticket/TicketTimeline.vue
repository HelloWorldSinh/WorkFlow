<script setup lang="ts">
import { computed } from 'vue'
import type { TicketDetail } from '@/types/ticket'

const props = defineProps<{
  ticket: TicketDetail
}>()

interface TimelineEvent {
  id: string | number
  title: string
  subtitle?: string
  actor: string
  actorRole?: string
  time: string
  type: 'create' | 'approve' | 'reject' | 'system' | 'complete'
  comment?: string
}

const events = computed<TimelineEvent[]>(() => {
  const list: TimelineEvent[] = []

  // 1. Sự kiện khởi tạo
  list.push({
    id: 'ev-init',
    title: 'Khởi tạo Yêu cầu (Ticket Created)',
    subtitle: `Bắt đầu quy trình "${props.ticket.workflowName}"`,
    actor: props.ticket.creator?.fullName || 'Người dùng',
    actorRole: 'Người tạo yêu cầu',
    time: props.ticket.startTime,
    type: 'create',
  })

  // 2. Lịch sử các bước xử lý
  const tasks = props.ticket.taskHistory || []
  tasks.forEach((t) => {
    if (t.status === 'Approved') {
      list.push({
        id: `ev-task-${t.id}`,
        title: `Phê duyệt bước "${t.nodeName}"`,
        actor: t.assignedUser?.fullName || 'Người duyệt',
        actorRole: t.assignedUser?.role || 'Approver',
        time: t.completedAt || '',
        type: 'approve',
        comment: t.comment || 'Đã đồng ý phê duyệt.',
      })
    } else if (t.status === 'Rejected') {
      list.push({
        id: `ev-task-${t.id}`,
        title: `Từ chối bước "${t.nodeName}"`,
        actor: t.assignedUser?.fullName || 'Người duyệt',
        actorRole: t.assignedUser?.role || 'Approver',
        time: t.completedAt || '',
        type: 'reject',
        comment: t.comment || 'Từ chối yêu cầu.',
      })
    }
  })

  // 3. Sự kiện kết thúc
  if (props.ticket.status === 'Completed') {
    list.push({
      id: 'ev-end',
      title: 'Hoàn tất Quy trình (Completed)',
      subtitle: 'Tất cả các bước và điều kiện đã được thực hiện thành công.',
      actor: 'WorkerBuilder System',
      actorRole: 'Workflow Engine',
      time: props.ticket.endTime || props.ticket.startTime,
      type: 'complete',
    })
  } else if (props.ticket.status === 'Rejected') {
    list.push({
      id: 'ev-end-reject',
      title: 'Quy trình đã Bị Từ Chối (Rejected)',
      subtitle: 'Yêu cầu bị từ chối và luồng đã dừng lại.',
      actor: 'WorkerBuilder System',
      actorRole: 'Workflow Engine',
      time: props.ticket.endTime || props.ticket.startTime,
      type: 'reject',
    })
  }

  return list
})

const formatTime = (timeStr?: string) => {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  if (isNaN(d.getTime())) return timeStr
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}, ${d.getDate()}/${d.getMonth() + 1}/${d.getFullYear()}`
}
</script>

<template>
  <div class="timeline-card">
    <div class="timeline-header">
      <h3 class="timeline-title">Lịch sử Xử lý & Nhật ký Sự kiện (Audit Trail)</h3>
    </div>

    <div class="timeline-list">
      <div
        v-for="ev in events"
        :key="ev.id"
        class="timeline-item"
        :class="`type-${ev.type}`"
      >
        <!-- Indicator Dot & Icon -->
        <div class="timeline-dot-wrapper">
          <div class="timeline-dot">
            <span v-if="ev.type === 'create'">🚀</span>
            <span v-else-if="ev.type === 'approve'">✓</span>
            <span v-else-if="ev.type === 'reject'">✕</span>
            <span v-else-if="ev.type === 'complete'">🎉</span>
            <span v-else>⚡</span>
          </div>
          <div class="timeline-line"></div>
        </div>

        <!-- Event Body -->
        <div class="timeline-content">
          <div class="content-header">
            <h4 class="event-title">{{ ev.title }}</h4>
            <span class="event-time">{{ formatTime(ev.time) }}</span>
          </div>

          <div class="event-meta">
            <span class="actor-name">{{ ev.actor }}</span>
            <span v-if="ev.actorRole" class="actor-role">({{ ev.actorRole }})</span>
          </div>

          <p v-if="ev.subtitle" class="event-sub">{{ ev.subtitle }}</p>

          <!-- Comment bubble if any -->
          <div v-if="ev.comment" class="comment-bubble" :class="{ is_reject: ev.type === 'reject' }">
            <span class="comment-quote">“</span>
            <span class="comment-text">{{ ev.comment }}</span>
            <span class="comment-quote">”</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.timeline-card {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  padding: 1.5rem;
  box-shadow: 0 4px 16px -2px rgba(0, 0, 0, 0.05);
}

.timeline-header {
  margin-bottom: 1.5rem;
}

.timeline-title {
  font-size: 1.0625rem;
  font-weight: 700;
  color: #0f172a;
}

.timeline-list {
  display: flex;
  flex-direction: column;
}

.timeline-item {
  display: flex;
  gap: 1.25rem;
  position: relative;
  padding-bottom: 1.5rem;
}

.timeline-item:last-child {
  padding-bottom: 0;
}

.timeline-item:last-child .timeline-line {
  display: none;
}

.timeline-dot-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.timeline-dot {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8125rem;
  font-weight: 700;
  z-index: 2;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.timeline-line {
  width: 2px;
  flex: 1;
  background: #e2e8f0;
  margin-top: 4px;
  margin-bottom: -4px;
}

/* Event types */
.type-create .timeline-dot {
  background: #e0f2fe;
  color: #0284c7;
  border: 2px solid #bae6fd;
}

.type-approve .timeline-dot {
  background: #ecfdf5;
  color: #059669;
  border: 2px solid #a7f3d0;
}

.type-reject .timeline-dot {
  background: #fef2f2;
  color: #dc2626;
  border: 2px solid #fecaca;
}

.type-complete .timeline-dot {
  background: #fdf4ff;
  color: #9333ea;
  border: 2px solid #f0abfc;
}

.timeline-content {
  flex: 1;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  border-radius: 10px;
  padding: 0.875rem 1rem;
}

.content-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.25rem;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.event-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: #0f172a;
}

.event-time {
  font-size: 0.75rem;
  color: #64748b;
  font-weight: 500;
}

.event-meta {
  font-size: 0.75rem;
  color: #475569;
  display: flex;
  gap: 0.375rem;
  margin-bottom: 0.375rem;
}

.actor-name {
  font-weight: 600;
  color: #1e293b;
}

.actor-role {
  color: #64748b;
}

.event-sub {
  font-size: 0.8125rem;
  color: #475569;
}

.comment-bubble {
  margin-top: 0.5rem;
  padding: 0.5rem 0.75rem;
  background: #ecfdf5;
  border-left: 3px solid #10b981;
  border-radius: 0 6px 6px 0;
  font-size: 0.8125rem;
  color: #065f46;
  font-style: italic;
}

.comment-bubble.is_reject {
  background: #fef2f2;
  border-left-color: #ef4444;
  color: #991b1b;
}

.comment-quote {
  font-weight: 700;
  font-size: 1rem;
  line-height: 0;
}
</style>
