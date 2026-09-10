<script setup lang="ts">
import { computed } from 'vue'
import { useWorkflowStore } from '@/stores/workflowStore'
import Modal from '@/components/common/Modal.vue'

const store = useWorkflowStore()
const workflow = computed(() => store.workflowToDelete)

const hasActiveInstances = computed(() => {
  return (workflow.value?.activeInstances ?? 0) > 0
})

const handleClose = () => {
  store.isDeleteModalOpen = false
  store.workflowToDelete = null
}
</script>

<template>
  <Modal
    :is-open="store.isDeleteModalOpen"
    title="Xác nhận xóa Workflow"
    max-width="480px"
    @close="handleClose"
  >
    <div v-if="workflow" class="delete-content">
      <!-- Warning if active instances > 0 -->
      <div v-if="hasActiveInstances" class="alert-block alert-danger">
        <div class="alert-icon">⚠️</div>
        <div class="alert-text">
          <strong>Không thể xóa quy trình này!</strong>
          <p>
            Quy trình "<strong>{{ workflow.name }}</strong>" hiện đang có
            <strong>{{ workflow.activeInstances }}</strong> yêu cầu (instances) đang trong tiến trình chạy.
          </p>
          <p class="hint-text">
            * Theo quy định hệ thống, bạn chỉ có thể xóa khi không còn instance nào đang chạy hoặc chuyển sang trạng thái <strong>Tạm ngưng (Suspended)</strong>.
          </p>
        </div>
      </div>

      <!-- Safe to delete -->
      <div v-else class="safe-delete-box">
        <div class="delete-icon-wrapper">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="3 6 5 6 21 6"></polyline>
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
          </svg>
        </div>
        <div class="delete-text">
          <p>
            Bạn có chắc chắn muốn xóa quy trình <strong>"{{ workflow.name }}"</strong> (Mã: <code>{{ workflow.code }}</code>)?
          </p>
          <p class="sub-text">
            Quy trình này sẽ được chuyển sang trạng thái <em>Soft Delete</em> và không thể tiếp tục nhận yêu cầu mới.
          </p>
        </div>
      </div>
    </div>

    <!-- Footer Actions -->
    <template #footer>
      <button class="btn btn-secondary" :disabled="store.isSubmitting" @click="handleClose">Đóng</button>
      <button
        v-if="!hasActiveInstances"
        class="btn btn-danger"
        :disabled="store.isSubmitting"
        @click="store.executeDelete"
      >
        {{ store.isSubmitting ? 'Đang xóa...' : 'Xác nhận xóa' }}
      </button>
    </template>
  </Modal>
</template>

<style scoped>
.delete-content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.alert-block {
  display: flex;
  gap: 0.75rem;
  padding: 1rem;
  border-radius: var(--radius-md);
  font-size: 0.8125rem;
  line-height: 1.4;
}

.alert-danger {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #991b1b;
}

.alert-icon {
  font-size: 1.5rem;
}

.hint-text {
  margin-top: 0.5rem;
  color: #dc2626;
  font-size: 0.75rem;
}

.safe-delete-box {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
}

.delete-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #fee2e2;
  color: #dc2626;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.delete-text p {
  font-size: 0.875rem;
  color: var(--text-primary);
  margin-bottom: 0.375rem;
}

.delete-text .sub-text {
  font-size: 0.8125rem;
  color: var(--text-secondary);
}

code {
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #e2e8f0;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'JetBrains Mono', monospace;
}

.btn {
  padding: 0.625rem 1.125rem;
  border-radius: var(--radius-md);
  font-size: 0.875rem;
  font-weight: 600;
}

.btn-secondary {
  background: #ffffff;
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
}

.btn-secondary:hover {
  background: var(--bg-card-hover);
  color: var(--text-primary);
}

.btn-danger {
  background: #dc2626;
  color: #ffffff;
}

.btn-danger:hover {
  background: #b91c1c;
}
</style>
