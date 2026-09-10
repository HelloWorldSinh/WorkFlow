<script setup lang="ts">
import { ref, computed } from 'vue'
import { useTicketStore } from '@/stores/ticketStore'
import type { TaskDetail, TicketDetail } from '@/types/ticket'

const props = defineProps<{
  ticket: TicketDetail
}>()

const ticketStore = useTicketStore()
const comment = ref('')

// Tìm task đang chờ xử lý (ưu tiên task của user hiện tại)
const activeTask = computed<TaskDetail | null>(() => {
  if (props.ticket.status !== 'Running') return null
  const tasks = props.ticket.taskHistory || []
  const pendings = tasks.filter((t) => t.status === 'Pending')
  if (pendings.length === 0) return null

  // Ưu tiên task pending của user đang đăng nhập
  const myPendingTask = pendings.find((t) => t.assignedUser?.id === ticketStore.currentUserId)
  if (myPendingTask) return myPendingTask

  // Fallback về task pending đầu tiên
  return pendings[0] || null
})

// Danh sách các task thuộc node hiện tại
const currentNodeTasks = computed<TaskDetail[]>(() => {
  if (!activeTask.value) return []
  return (props.ticket.taskHistory || []).filter((t) => t.nodeId === activeTask.value?.nodeId)
})

// Kiểm tra xem bước hiện tại có phải duyệt Hội đồng (nhiều người cùng duyệt)
const isCouncilStep = computed(() => {
  return currentNodeTasks.value.length > 1
})

// Thống kê tiến độ hội đồng
const councilStats = computed(() => {
  const all = currentNodeTasks.value
  const approved = all.filter((t) => t.status === 'Approved').length
  const rejected = all.filter((t) => t.status === 'Rejected').length
  const pending = all.filter((t) => t.status === 'Pending').length
  const percent = all.length > 0 ? Math.round((approved / all.length) * 100) : 0
  return {
    total: all.length,
    approved,
    rejected,
    pending,
    percent,
  }
})

// Kiểm tra xem user hiện tại đã hoàn thành biểu quyết của mình chưa
const hasCurrentUserVoted = computed(() => {
  if (!isCouncilStep.value) return false
  const currentUserId = ticketStore.currentUserId
  const finished = currentNodeTasks.value.find(
    (t) => t.assignedUser?.id === currentUserId && (t.status === 'Approved' || t.status === 'Rejected')
  )
  return !!finished
})

// Kiểm tra quyền thao tác: user hiện tại được gán task active hoặc là admin
const canAct = computed(() => {
  if (!activeTask.value || activeTask.value.status !== 'Pending') return false
  const currentUserId = ticketStore.currentUserId
  if (!activeTask.value.assignedUser) return true // Cho phép nếu chưa gán cụ thể
  return activeTask.value.assignedUser.id === currentUserId || currentUserId === 1
})

const handleApprove = async () => {
  if (!activeTask.value) return
  await ticketStore.processTask(activeTask.value.id, 'APPROVE', comment.value)
  comment.value = ''
}

const handleReject = async () => {
  if (!activeTask.value) return
  if (!comment.value.trim()) {
    ticketStore.showToast('Vui lòng nhập lý do từ chối vào ô nhận xét bên dưới', 'error')
    return
  }
  await ticketStore.processTask(activeTask.value.id, 'REJECT', comment.value)
  comment.value = ''
}
</script>

<template>
  <div v-if="activeTask" class="action-box-card" :class="{ 'is-council': isCouncilStep }">
    <!-- Header -->
    <div class="action-box-header">
      <div class="header-badge" :class="{ 'badge-council': isCouncilStep }">
        <span class="badge-pulse"></span>
        <span>{{ isCouncilStep ? 'Hội đồng Phê duyệt Đa người' : 'Yêu cầu xử lý nghiệp vụ' }}</span>
      </div>
      <h3 class="step-title">{{ activeTask.nodeName }}</h3>
      
      <p v-if="!isCouncilStep" class="step-subtitle">
        Người phụ trách: <strong>{{ activeTask.assignedUser?.fullName || 'Chưa chỉ định cụ thể' }}</strong>
        <span v-if="activeTask.dueDate" class="due-tag">
          • Hạn duyệt: {{ new Date(activeTask.dueDate).toLocaleDateString() }}
        </span>
      </p>

      <p v-else class="step-subtitle">
        Tiến độ hội đồng: <strong>{{ councilStats.approved }}/{{ councilStats.total }} người đã duyệt</strong>
        <span v-if="councilStats.rejected > 0" class="reject-tag">• {{ councilStats.rejected }} từ chối</span>
        <span v-if="activeTask.dueDate" class="due-tag">
          • Hạn chót: {{ new Date(activeTask.dueDate).toLocaleDateString() }}
        </span>
      </p>
    </div>

    <!-- Council Progress & Member List Panel -->
    <div v-if="isCouncilStep" class="council-panel">
      <div class="council-progress-row">
        <div class="progress-bar-wrap">
          <div class="progress-bar-fill" :style="{ width: `${councilStats.percent}%` }"></div>
        </div>
        <span class="progress-percent-label">{{ councilStats.percent }}% đồng thuận</span>
      </div>

      <div class="council-members-list">
        <div
          v-for="task in currentNodeTasks"
          :key="task.id"
          class="council-member-badge"
          :class="{
            'is-approved': task.status === 'Approved',
            'is-rejected': task.status === 'Rejected',
            'is-pending': task.status === 'Pending',
            'is-me': task.assignedUser?.id === ticketStore.currentUserId,
          }"
        >
          <span class="member-status-icon">
            {{ task.status === 'Approved' ? '✓' : task.status === 'Rejected' ? '✕' : '⏳' }}
          </span>
          <span class="member-name">
            {{ task.assignedUser?.fullName || 'Thành viên' }}
            <span v-if="task.assignedUser?.id === ticketStore.currentUserId" class="me-tag">(Bạn)</span>
          </span>
          <span class="member-state-text">
            {{ task.status === 'Approved' ? 'Đã duyệt' : task.status === 'Rejected' ? 'Từ chối' : 'Chờ biểu quyết' }}
          </span>
        </div>
      </div>
    </div>

    <!-- User already completed vote banner -->
    <div v-if="hasCurrentUserVoted" class="voted-notice">
      <span class="notice-icon">✓</span>
      <div>
        <div class="voted-title">Bạn đã hoàn thành phần phê duyệt của mình</div>
        <div class="voted-desc">Hệ thống đang tiếp tục ghi nhận biểu quyết từ các thành viên khác trong hội đồng.</div>
      </div>
    </div>

    <!-- Permission Warning if not assigned & not voted -->
    <div v-else-if="!canAct" class="permission-notice">
      <span class="notice-icon">ℹ️</span>
      <p v-if="isCouncilStep">
        Bước phê duyệt này dành cho các thành viên trong hội đồng. Bạn đang ở chế độ xem tiến độ.
      </p>
      <p v-else>
        Nhiệm vụ này được giao cho <strong>{{ activeTask.assignedUser?.fullName }}</strong>. Bạn đang ở chế độ xem.
      </p>
    </div>

    <!-- Action Form for assignees who haven't voted -->
    <div v-else class="action-form">
      <div v-if="isCouncilStep" class="my-turn-banner">
        <span>✍️ Đến lượt bạn biểu quyết với tư cách thành viên hội đồng:</span>
      </div>

      <div class="input-group">
        <label class="input-label">Ý kiến nhận xét / Ghi chú phê duyệt:</label>
        <textarea
          v-model="comment"
          rows="3"
          class="comment-textarea"
          placeholder="Nhập nội dung góp ý, căn cứ duyệt hoặc lý do từ chối (bắt buộc khi từ chối)..."
          :disabled="ticketStore.isProcessingAction"
        ></textarea>
      </div>

      <div class="action-buttons">
        <!-- Reject Button -->
        <button
          type="button"
          class="btn-reject"
          :disabled="ticketStore.isProcessingAction"
          @click="handleReject"
        >
          <span v-if="ticketStore.isProcessingAction" class="btn-spinner"></span>
          <span v-else>✕</span>
          <span>Từ Chối (Reject)</span>
        </button>

        <!-- Approve Button -->
        <button
          type="button"
          class="btn-approve"
          :disabled="ticketStore.isProcessingAction"
          @click="handleApprove"
        >
          <span v-if="ticketStore.isProcessingAction" class="btn-spinner white"></span>
          <span v-else>✓</span>
          <span>Phê Duyệt (Approve)</span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.action-box-card {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border: 1.5px solid #cbd5e1;
  border-radius: 14px;
  padding: 1.5rem;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.05);
}

.action-box-card.is-council {
  border-color: #93c5fd;
  background: linear-gradient(135deg, #f0f9ff 0%, #f8fafc 100%);
}

.action-box-header {
  margin-bottom: 1.25rem;
}

.header-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  font-weight: 700;
  color: #0284c7;
  background: #e0f2fe;
  padding: 2px 10px;
  border-radius: 9999px;
  margin-bottom: 0.5rem;
}

.header-badge.badge-council {
  color: #4338ca;
  background: #e0e7ff;
}

.badge-pulse {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.3);
}

.step-title {
  font-size: 1.125rem;
  font-weight: 700;
  color: #0f172a;
}

.step-subtitle {
  font-size: 0.8125rem;
  color: #475569;
  margin-top: 0.25rem;
}

.due-tag {
  color: #f59e0b;
  font-weight: 600;
}

.reject-tag {
  color: #ef4444;
  font-weight: 600;
}

/* Council Panel */
.council-panel {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 1rem;
  margin-bottom: 1.25rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.council-progress-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.875rem;
}

.progress-bar-wrap {
  flex: 1;
  height: 8px;
  background: #e2e8f0;
  border-radius: 9999px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #10b981);
  transition: width 0.4s ease;
}

.progress-percent-label {
  font-size: 0.75rem;
  font-weight: 700;
  color: #0f172a;
  white-space: nowrap;
}

.council-members-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.council-member-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.625rem;
  border-radius: 8px;
  font-size: 0.75rem;
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
  color: #334155;
  transition: all 0.2s ease;
}

.council-member-badge.is-approved {
  background: #ecfdf5;
  border-color: #a7f3d0;
  color: #065f46;
}

.council-member-badge.is-rejected {
  background: #fef2f2;
  border-color: #fecaca;
  color: #991b1b;
}

.council-member-badge.is-pending {
  background: #fffbeb;
  border-color: #fde68a;
  color: #92400e;
}

.council-member-badge.is-me {
  box-shadow: 0 0 0 2px #6366f1;
  font-weight: 600;
}

.member-name {
  font-weight: 600;
}

.me-tag {
  color: #4f46e5;
  font-weight: 700;
  margin-left: 2px;
}

.member-state-text {
  font-size: 0.7rem;
  opacity: 0.85;
}

/* Notices */
.voted-notice {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  border-radius: 8px;
  color: #065f46;
}

.voted-title {
  font-weight: 700;
  font-size: 0.875rem;
}

.voted-desc {
  font-size: 0.8125rem;
  margin-top: 0.125rem;
  color: #047857;
}

.permission-notice {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  background: #fefce8;
  border: 1px solid #fef08a;
  border-radius: 8px;
  color: #854d0e;
  font-size: 0.8125rem;
}

.notice-icon {
  font-size: 1.125rem;
}

.my-turn-banner {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #1e40af;
  background: #eff6ff;
  border-radius: 6px;
  padding: 0.5rem 0.75rem;
  margin-bottom: 0.25rem;
}

.action-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.input-label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #334155;
}

.comment-textarea {
  width: 100%;
  padding: 0.75rem 0.875rem;
  border: 1.5px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.875rem;
  color: #0f172a;
  background: #ffffff;
  resize: vertical;
  transition: all 0.2s ease;
}

.comment-textarea:focus {
  outline: none;
  border-color: #0284c7;
  box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.15);
}

.action-buttons {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
}

.btn-reject {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.25rem;
  background: #ffffff;
  color: #dc2626;
  border: 1.5px solid #fecaca;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-reject:hover:not(:disabled) {
  background: #fef2f2;
  border-color: #ef4444;
}

.btn-approve {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.5rem;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.35);
  transition: all 0.2s ease;
}

.btn-approve:hover:not(:disabled) {
  background: linear-gradient(135deg, #059669 0%, #047857 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.45);
}

.btn-approve:disabled,
.btn-reject:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid #dc2626;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.btn-spinner.white {
  border-color: #ffffff;
  border-top-color: transparent;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
