<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useRoute, onBeforeRouteLeave } from 'vue-router'
import { useWorkflowEditorStore } from '@/stores/workflowEditorStore'
import EditorTopBar from '@/components/editor/EditorTopBar.vue'
import EditorSidebar from '@/components/editor/EditorSidebar.vue'
import EditorCanvas from '@/components/editor/EditorCanvas.vue'
import PropertiesPanel from '@/components/editor/PropertiesPanel.vue'
import FormPreviewModal from '@/components/form/FormPreviewModal.vue'
import TicketSimulationModal from '@/components/form/TicketSimulationModal.vue'

const route = useRoute()
const editorStore = useWorkflowEditorStore()

// Cảnh báo khi người dùng chuyển hướng sang trang khác nếu quy trình chưa được lưu
onBeforeRouteLeave((to, from) => {
  if (to.path !== from.path && editorStore.isDirty) {
    const answer = window.confirm(
      'Quy trình có những thay đổi chưa được lưu. Bạn có chắc chắn muốn rời khỏi trang không?'
    )
    if (!answer) {
      editorStore.showToast('Đã hủy chuyển hướng. Vui lòng lưu quy trình trước khi rời đi.', 'info')
      return false
    }
    editorStore.discardChanges()
  }
})

// Cảnh báo khi người dùng tải lại trang (F5) hoặc đóng tab trình duyệt
const handleBeforeUnload = (event: BeforeUnloadEvent) => {
  if (editorStore.isDirty) {
    event.preventDefault()
    event.returnValue = ''
    return ''
  }
}

onMounted(() => {
  window.addEventListener('beforeunload', handleBeforeUnload)

  if (route.params.id) {
    const id = Number(route.params.id) || String(route.params.id)
    const name = route.query.name ? String(route.query.name) : undefined
    const templateId = route.query.templateId ? String(route.query.templateId) : undefined
    editorStore.initWorkflow(id, name, templateId)
  }
})

onUnmounted(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
})
</script>

<template>
  <div class="workflow-editor-view">
    <!-- 1. TOP BAR -->
    <EditorTopBar />

    <!-- 2. WORKSPACE: LEFT SIDEBAR + CANVAS + RIGHT PROPERTIES PANEL -->
    <div class="editor-workspace">
      <!-- 2.1 LEFT NODE PALETTE SIDEBAR -->
      <EditorSidebar />

      <!-- 2.2 CENTRAL CANVAS -->
      <EditorCanvas />

      <!-- 2.3 RIGHT PROPERTIES PANEL (SLIDE IN) -->
      <PropertiesPanel />
    </div>

    <!-- FORM & TICKET MODALS -->
    <FormPreviewModal />
    <TicketSimulationModal />


    <!-- TOAST NOTIFICATION -->
    <transition name="toast-fade">
      <div v-if="editorStore.toast" class="editor-toast" :class="editorStore.toast.type">
        <span class="toast-icon">
          {{ editorStore.toast.type === 'success' ? '✓' : editorStore.toast.type === 'error' ? '✕' : 'ℹ' }}
        </span>
        <span class="toast-text">{{ editorStore.toast.text }}</span>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.workflow-editor-view {
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #f8fafc;
  font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.editor-workspace {
  flex: 1;
  display: flex;
  position: relative;
  overflow: hidden;
}

/* Toast */
.editor-toast {
  position: fixed;
  bottom: 2rem;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.75rem 1.25rem;
  border-radius: 9999px;
  font-size: 0.8125rem;
  font-weight: 700;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
  z-index: 999;
}

.editor-toast.success {
  background: #065f46;
  color: #ffffff;
}

.editor-toast.error {
  background: #991b1b;
  color: #ffffff;
}

.editor-toast.info {
  background: #1e293b;
  color: #ffffff;
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: all 0.25s ease;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translate(-50%, 15px);
}
</style>
