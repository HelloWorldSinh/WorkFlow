<script setup lang="ts">
import { ref, watch } from 'vue'
import { useFormStore } from '@/stores/formStore'
import FormRenderer from './FormRenderer.vue'

const formStore = useFormStore()
const testData = ref<Record<string, any>>({})

watch(
  () => formStore.previewingForm,
  () => {
    testData.value = {}
  }
)
</script>

<template>
  <div v-if="formStore.isPreviewModalOpen && formStore.previewingForm" class="modal-overlay">
    <div class="modal-container preview-modal-container">
      <div class="modal-header">
        <div class="header-badge-group">
          <span class="badge-tag">Xem Trước Biểu Mẫu</span>
          <h2 class="modal-title">{{ formStore.previewingForm.name }}</h2>
          <p v-if="formStore.previewingForm.description" class="modal-subtitle">
            {{ formStore.previewingForm.description }}
          </p>
        </div>
        <button type="button" class="btn-close" @click="formStore.closePreviewModal">✕</button>
      </div>

      <div class="modal-body">
        <div class="form-preview-paper">
          <FormRenderer
            :schema="formStore.previewingForm.schema"
            :model-value="testData"
            @update:model-value="testData = $event"
          />
        </div>
      </div>

      <div class="modal-footer">
        <span class="footer-hint">Giao diện này phản ánh chính xác biểu mẫu khi người dùng mở để nhập dữ liệu.</span>
        <button type="button" class="btn-close-action" @click="formStore.closePreviewModal">
          Đóng cửa sổ
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
  padding: 1.5rem;
}

.preview-modal-container {
  background: #ffffff;
  width: 100%;
  max-width: 680px;
  max-height: 85vh;
  border-radius: 16px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  background: #f8fafc;
}

.badge-tag {
  font-size: 0.6875rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--primary);
}

.modal-title {
  font-size: 1.25rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0.25rem 0 0.2rem 0;
}

.modal-subtitle {
  font-size: 0.8125rem;
  color: #64748b;
  margin: 0;
}

.btn-close {
  background: none;
  border: none;
  font-size: 1.25rem;
  color: #94a3b8;
  cursor: pointer;
  padding: 0.25rem 0.5rem;
  border-radius: 6px;
}

.btn-close:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 1.75rem 1.5rem;
  background: #f1f5f9;
}

.form-preview-paper {
  background: #ffffff;
  padding: 1.5rem;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.875rem 1.5rem;
  background: #ffffff;
  border-top: 1px solid #e2e8f0;
}

.footer-hint {
  font-size: 0.75rem;
  color: #94a3b8;
}

.btn-close-action {
  background: #1e293b;
  color: #ffffff;
  border: none;
  padding: 0.5rem 1.25rem;
  border-radius: 6px;
  font-size: 0.84rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s ease;
}

.btn-close-action:hover {
  background: #0f172a;
}
</style>
