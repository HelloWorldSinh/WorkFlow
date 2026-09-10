<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useTicketStore } from '@/stores/ticketStore'
import type { TaskSummary } from '@/types/ticket'
import Toast from '@/components/common/Toast.vue'

const router = useRouter()
const ticketStore = useTicketStore()

// Quick Approve / Reject modal
const selectedTask = ref<TaskSummary | null>(null)
const actionModalType = ref<'APPROVE' | 'REJECT' | null>(null)
const comment = ref('')

onMounted(() => {
  ticketStore.fetchMyTasks()
})

const handleOpenAction = (task: TaskSummary, action: 'APPROVE' | 'REJECT') => {
  selectedTask.value = task
  actionModalType.value = action
  comment.value = action === 'APPROVE' ? 'Đồng ý phê duyệt' : ''
}

const handleConfirmAction = async () => {
  if (!selectedTask.value || !actionModalType.value) return
  if (actionModalType.value === 'REJECT' && !comment.value.trim()) {
    ticketStore.showToast('Vui lòng nhập lý do từ chối', 'error')
    return
  }

  await ticketStore.processTask(selectedTask.value.taskId, actionModalType.value, comment.value)
  actionModalType.value = null
  selectedTask.value = null
  comment.value = ''
  ticketStore.fetchMyTasks()
}

const handleViewDetail = (task: TaskSummary) => {
  router.push(`/tickets/${task.ticketId}`)
}

const formatDateTime = (dateStr?: string) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}, ${d.getDate()}/${d.getMonth() + 1}/${d.getFullYear()}`
}
</script>

<template>
  <div class="my-tasks-view">
    <div class="view-header">
      <div class="header-left">
        <h1 class="header-title">Nhiệm Vụ Cần Xử Lý (My Tasks)</h1>
        <p class="header-subtitle">
          Danh sách các bước phê duyệt và tác vụ đang chờ bạn tiếp nhận và xử lý
        </p>
      </div>

      <button class="btn-refresh" @click="ticketStore.fetchMyTasks">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="23 4 23 10 17 10"></polyline>
          <polyline points="1 20 1 14 7 14"></polyline>
          <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path>
        </svg>
        <span>Làm mới</span>
      </button>
    </div>

    <!-- Content Table -->
    <div class="tasks-card">
      <div v-if="ticketStore.isLoadingTasks" class="loading-state">
        <div class="spinner"></div>
        <p>Đang tải danh sách nhiệm vụ...</p>
      </div>

      <div v-else-if="ticketStore.myTasks.length === 0" class="empty-state">
        <div class="empty-icon">🎉</div>
        <h4>Bạn không có nhiệm vụ nào cần duyệt!</h4>
        <p>Tất cả các yêu cầu đã được xử lý hoặc chưa có công việc mới được phân công.</p>
        <button class="btn-goto-tickets" @click="router.push('/tickets')">
          Xem Tất Cả Yêu Cầu (Tickets)
        </button>
      </div>

      <div v-else class="table-responsive">
        <table class="tasks-table">
          <thead>
            <tr>
              <th>Mã Yêu Cầu</th>
              <th>Tiêu Đề</th>
              <th>Quy Trình</th>
              <th>Bước Cần Xử Lý</th>
              <th>Người Khởi Tạo</th>
              <th>Hạn Xử Lý (SLA)</th>
              <th class="text-right">Hành Động</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="t in ticketStore.myTasks" :key="t.taskId" class="task-row">
              <td>
                <span class="ticket-code" @click="handleViewDetail(t)">{{ t.requestCode }}</span>
              </td>
              <td>
                <span class="task-title" @click="handleViewDetail(t)">{{ t.ticketTitle }}</span>
              </td>
              <td>
                <span class="wf-name">{{ t.workflowName }}</span>
              </td>
              <td>
                <span class="step-badge">{{ t.nodeName }}</span>
              </td>
              <td>
                <div class="creator-cell">
                  <span class="creator-name">{{ t.creator?.fullName }}</span>
                </div>
              </td>
              <td>
                <span v-if="t.dueDate" class="due-text">{{ formatDateTime(t.dueDate) }}</span>
                <span v-else class="text-slate-400">-</span>
              </td>
              <td class="text-right">
                <div class="actions-group">
                  <button
                    class="btn-action-reject"
                    title="Từ chối yêu cầu này"
                    @click="handleOpenAction(t, 'REJECT')"
                  >
                    Từ chối
                  </button>
                  <button
                    class="btn-action-approve"
                    title="Phê duyệt nhanh"
                    @click="handleOpenAction(t, 'APPROVE')"
                  >
                    Duyệt ngay
                  </button>
                  <button
                    class="btn-action-view"
                    title="Xem chi tiết đầy đủ hồ sơ"
                    @click="handleViewDetail(t)"
                  >
                    Chi tiết
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Quick Action Modal -->
    <div v-if="actionModalType" class="modal-backdrop" @click="actionModalType = null">
      <div class="action-modal" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">
            {{ actionModalType === 'APPROVE' ? 'Xác nhận Phê duyệt' : 'Xác nhận Từ chối Yêu cầu' }}
          </h3>
          <button class="modal-close" @click="actionModalType = null">✕</button>
        </div>

        <div class="modal-body">
          <p class="modal-intro">
            Bạn đang thực hiện thao tác trên yêu cầu <strong>{{ selectedTask?.requestCode }}</strong> - Bước: <strong>{{ selectedTask?.nodeName }}</strong>
          </p>

          <div class="comment-group">
            <label class="comment-label">
              <span>Ý kiến xử lý</span>
              <span v-if="actionModalType === 'REJECT'" class="text-red-500">* (bắt buộc)</span>
            </label>
            <textarea
              v-model="comment"
              rows="3"
              class="comment-box"
              :placeholder="actionModalType === 'APPROVE' ? 'Nhập ý kiến phê duyệt nếu có...' : 'Nhập lý do từ chối yêu cầu...'"
            ></textarea>
          </div>
        </div>

        <div class="modal-footer">
          <button class="btn-cancel" @click="actionModalType = null">Hủy</button>
          <button
            class="btn-confirm"
            :class="{ is_reject: actionModalType === 'REJECT' }"
            :disabled="ticketStore.isProcessingAction"
            @click="handleConfirmAction"
          >
            {{ actionModalType === 'APPROVE' ? 'Xác nhận Duyệt' : 'Xác nhận Từ chối' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Toast -->
    <Toast :message="ticketStore.toast" />
  </div>
</template>

<style scoped>
.my-tasks-view {
  max-width: 1440px;
  margin: 0 auto;
  padding: 2rem 1.5rem;
  min-height: 100vh;
}

.view-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 2rem;
}

.header-title {
  font-size: 1.625rem;
  font-weight: 800;
  color: #0f172a;
}

.header-subtitle {
  font-size: 0.875rem;
  color: #64748b;
  margin-top: 0.25rem;
}

.btn-refresh {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1rem;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.8125rem;
  font-weight: 600;
  color: #0284c7;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-refresh:hover {
  background: #f0f9ff;
  border-color: #0284c7;
}

.tasks-card {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 16px -2px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 1.5rem;
  text-align: center;
  color: #64748b;
}

.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #e2e8f0;
  border-top-color: #0284c7;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 1rem;
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.empty-state h4 {
  font-size: 1.125rem;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 0.5rem;
}

.btn-goto-tickets {
  margin-top: 1.25rem;
  padding: 0.625rem 1.25rem;
  background: #0284c7;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
}

.tasks-table {
  width: 100%;
  border-collapse: collapse;
}

.tasks-table th {
  background: #f8fafc;
  padding: 1rem 1.25rem;
  font-size: 0.75rem;
  font-weight: 700;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border-bottom: 1px solid #e2e8f0;
}

.tasks-table td {
  padding: 1rem 1.25rem;
  border-bottom: 1px solid #f1f5f9;
  font-size: 0.875rem;
}

.task-row:hover {
  background: #f8fafc;
}

.ticket-code {
  font-family: monospace;
  font-weight: 700;
  color: #0284c7;
  cursor: pointer;
}

.ticket-code:hover {
  text-decoration: underline;
}

.task-title {
  font-weight: 700;
  color: #0f172a;
  cursor: pointer;
}

.task-title:hover {
  color: #0284c7;
}

.wf-name {
  font-size: 0.8125rem;
  color: #475569;
}

.step-badge {
  font-size: 0.75rem;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 6px;
  background: #e0f2fe;
  color: #0284c7;
}

.creator-name {
  font-weight: 600;
  color: #1e293b;
}

.due-text {
  font-size: 0.8125rem;
  color: #f59e0b;
  font-weight: 600;
}

.actions-group {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.5rem;
}

.btn-action-reject {
  padding: 0.375rem 0.75rem;
  background: #ffffff;
  border: 1px solid #fecaca;
  color: #dc2626;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-action-reject:hover {
  background: #fef2f2;
}

.btn-action-approve {
  padding: 0.375rem 0.875rem;
  background: #10b981;
  border: none;
  color: #ffffff;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
}

.btn-action-approve:hover {
  background: #059669;
}

.btn-action-view {
  padding: 0.375rem 0.75rem;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #475569;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-action-view:hover {
  background: #f1f5f9;
  color: #0f172a;
}

/* Modal */
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.action-modal {
  background: #ffffff;
  border-radius: 14px;
  width: 90%;
  max-width: 500px;
  padding: 1.5rem;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.modal-title {
  font-size: 1.125rem;
  font-weight: 700;
  color: #0f172a;
}

.modal-close {
  background: transparent;
  border: none;
  font-size: 1.125rem;
  cursor: pointer;
  color: #64748b;
}

.modal-body {
  margin-bottom: 1.5rem;
}

.modal-intro {
  font-size: 0.875rem;
  color: #475569;
  margin-bottom: 1rem;
}

.comment-group {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.comment-label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #334155;
}

.comment-box {
  width: 100%;
  padding: 0.625rem 0.875rem;
  border: 1.5px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.875rem;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.btn-cancel {
  padding: 0.5rem 1rem;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.875rem;
  cursor: pointer;
}

.btn-confirm {
  padding: 0.5rem 1.25rem;
  background: #10b981;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
}

.btn-confirm.is_reject {
  background: #ef4444;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
