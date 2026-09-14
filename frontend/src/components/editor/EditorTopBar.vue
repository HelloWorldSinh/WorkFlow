<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useWorkflowEditorStore } from '@/stores/workflowEditorStore'

const router = useRouter()
const editorStore = useWorkflowEditorStore()

const handleBack = () => {
  router.push('/')
}

const handleOpenMatch = () => {
  if (editorStore.workflowId) {
    router.push({
      path: `/workflows/${editorStore.workflowId}/match-form`,
      query: { workflowId: editorStore.workflowId },
    })
  } else {
    router.push('/match-form')
  }
}
</script>

<template>
  <header class="editor-topbar">
    <!-- LEFT: Back, Workflow Title, Code, Status -->
    <div class="topbar-left">
      <button class="btn-back" title="Quay lại danh sách quy trình" @click="handleBack">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <line x1="19" y1="12" x2="5" y2="12"></line>
          <polyline points="12 19 5 12 12 5"></polyline>
        </svg>
      </button>

      <div class="title-meta-group">
        <div class="title-row">
          <input
            v-model="editorStore.workflowName"
            type="text"
            class="workflow-name-input"
            placeholder="Nhập tên workflow..."
            title="Nhấp để đổi tên quy trình"
            @input="editorStore.isDirty = true"
          />
          <span class="workflow-code-badge">{{ editorStore.workflowCode }}</span>
        </div>

        <div class="status-meta-row">
          <span class="status-indicator" :class="editorStore.workflowStatus.toLowerCase()">
            <span class="status-pulse-dot"></span>
            <span>{{ editorStore.workflowStatus === 'Published' ? 'Đang hoạt động (Published)' : 'Bản nháp (Draft)' }}</span>
          </span>
          <span v-if="editorStore.isDirty" class="unsaved-badge">● Chưa lưu thay đổi</span>
        </div>
      </div>
    </div>

    <!-- RIGHT: Action Buttons (Match Form, Validate, Save Draft, Publish) -->
    <div class="topbar-right">
      <!-- Match Form Button -->
      <button
        type="button"
        class="action-btn btn-match"
        title="Mở màn hình ghép Biểu mẫu và Quy trình (Data Mapping & Permissions)"
        @click="handleOpenMatch"
      >
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
          <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"></path>
          <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"></path>
        </svg>
        <span>Ghép Biểu Mẫu</span>
      </button>

      <!-- Validate Workflow -->
      <button
        type="button"
        class="action-btn btn-validate"
        title="Kiểm tra cấu trúc và tính hợp lệ của quy trình"
        @click="editorStore.validateWorkflow(true)"
      >
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
          <polyline points="22 4 12 14.01 9 11.01"></polyline>
        </svg>
        <span>Validate</span>
      </button>

      <!-- Save Draft -->
      <button
        type="button"
        class="action-btn btn-draft"
        :disabled="editorStore.isSaving"
        @click="editorStore.saveDraft"
      >
        <svg v-if="!editorStore.isSaving" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
          <polyline points="17 21 17 13 7 13 7 21"></polyline>
          <polyline points="7 3 7 8 15 8"></polyline>
        </svg>
        <span v-else class="btn-spinner-mini"></span>
        <span>Save Draft</span>
      </button>

      <!-- Publish Workflow -->
      <button
        type="button"
        class="action-btn btn-publish"
        :disabled="editorStore.isSaving"
        @click="editorStore.publishWorkflow"
      >
        <svg v-if="!editorStore.isSaving" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
          <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon>
        </svg>
        <span v-else class="btn-spinner-mini white"></span>
        <span>Publish Workflow</span>
      </button>
    </div>
  </header>
</template>

<style scoped>
.editor-topbar {
  height: 64px;
  background: linear-gradient(135deg, #0284c7 0%, #0369a1 50%, #075985 100%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.15);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 1.5rem;
  gap: 1rem;
  box-shadow: 0 4px 20px -2px rgba(2, 132, 199, 0.3), 0 2px 6px rgba(0, 0, 0, 0.15);
  position: relative;
  z-index: 50;
  user-select: none;
  color: #ffffff;
}

/* ==========================================================
   LEFT SECTION
   ========================================================== */
.topbar-left {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  min-width: 280px;
}

.btn-back {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.25);
  background: rgba(255, 255, 255, 0.12);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  backdrop-filter: blur(8px);
}

.btn-back:hover {
  background: rgba(255, 255, 255, 0.25);
  color: #ffffff;
  border-color: rgba(255, 255, 255, 0.4);
  transform: translateX(-2px);
}

.title-meta-group {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.workflow-name-input {
  font-size: 0.9375rem;
  font-weight: 700;
  color: #ffffff;
  border: 1px solid transparent;
  background: transparent;
  padding: 2px 6px;
  border-radius: 4px;
  max-width: 280px;
  transition: all 0.2s ease;
}

.workflow-name-input:hover {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.25);
}

.workflow-name-input:focus {
  background: rgba(255, 255, 255, 0.2);
  border-color: #38bdf8;
  box-shadow: 0 0 0 3px rgba(56, 189, 248, 0.3);
}

.workflow-code-badge {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.6875rem;
  font-weight: 600;
  color: #e0f2fe;
  background: rgba(12, 74, 110, 0.6);
  padding: 2px 8px;
  border-radius: 6px;
  border: 1px solid rgba(56, 189, 248, 0.3);
}

.status-meta-row {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding-left: 6px;
}

.status-indicator {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 0.6875rem;
  font-weight: 600;
}

.status-indicator.published {
  color: #a7f3d0;
}

.status-indicator.draft {
  color: #fde68a;
}

.status-pulse-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.unsaved-badge {
  font-size: 0.6875rem;
  color: #fca5a5;
  font-weight: 600;
}

/* ==========================================================
   RIGHT ACTION BUTTONS
   ========================================================== */
.topbar-right {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  min-width: 280px;
  justify-content: flex-end;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  height: 36px;
  padding: 0 0.875rem;
  border-radius: 8px;
  font-size: 0.8125rem;
  font-weight: 600;
  transition: all 0.2s ease;
  cursor: pointer;
}

.btn-match {
  background: rgba(16, 185, 129, 0.2);
  border: 1px solid rgba(16, 185, 129, 0.5);
  color: #a7f3d0;
  backdrop-filter: blur(6px);
}

.btn-match:hover:not(:disabled) {
  background: rgba(16, 185, 129, 0.35);
  color: #ffffff;
  border-color: #34d399;
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(16, 185, 129, 0.25);
}

.btn-validate {
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.25);
  color: #ffffff;
  backdrop-filter: blur(6px);
}

.btn-validate:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.25);
  border-color: #38bdf8;
  color: #ffffff;
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(56, 189, 248, 0.25);
}

.btn-draft {
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: #ffffff;
  backdrop-filter: blur(6px);
}

.btn-draft:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.28);
  border-color: rgba(255, 255, 255, 0.5);
  color: #ffffff;
  transform: translateY(-1px);
}

.btn-publish {
  background: #ffffff;
  color: #0284c7;
  border: none;
  font-weight: 700;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.btn-publish:hover:not(:disabled) {
  background: #f0f9ff;
  color: #0369a1;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
}

.btn-spinner-mini {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(0, 0, 0, 0.2);
  border-top-color: #4f46e5;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.btn-spinner-mini.white {
  border-color: rgba(255, 255, 255, 0.3);
  border-top-color: #ffffff;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
