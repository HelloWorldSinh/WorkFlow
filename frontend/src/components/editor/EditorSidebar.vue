<script setup lang="ts">
import { ref } from 'vue'
import { useWorkflowEditorStore } from '@/stores/workflowEditorStore'
import type { EditorNodeType } from '@/types/editor'

const editorStore = useWorkflowEditorStore()
const isCollapsed = ref(false)

const handleDragStart = (event: DragEvent, type: EditorNodeType) => {
  if (event.dataTransfer) {
    event.dataTransfer.setData('application/json', JSON.stringify({ type }))
    event.dataTransfer.effectAllowed = 'copy'
  }
}

const handleAddNode = (type: EditorNodeType) => {
  editorStore.addNode(type)
}
</script>

<template>
  <aside class="editor-sidebar" :class="{ 'is-collapsed': isCollapsed }">
    <!-- SIDEBAR HEADER -->
    <div class="sidebar-header">
      <div v-if="!isCollapsed" class="header-title-group">
        <div class="header-icon-box">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="7" height="7" rx="1.5"></rect>
            <rect x="14" y="3" width="7" height="7" rx="1.5"></rect>
            <rect x="14" y="14" width="7" height="7" rx="1.5"></rect>
            <rect x="3" y="14" width="7" height="7" rx="1.5"></rect>
          </svg>
        </div>
        <h3 class="sidebar-title">Thư viện Node</h3>
      </div>

      <!-- Toggle Collapse Button -->
      <button
        type="button"
        class="btn-toggle-collapse"
        :title="isCollapsed ? 'Mở rộng thư viện Node' : 'Thu gọn thư viện Node'"
        @click="isCollapsed = !isCollapsed"
      >
        <svg
          width="16"
          height="16"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2.5"
          :class="{ 'rotate-180': isCollapsed }"
        >
          <polyline points="15 18 9 12 15 6"></polyline>
        </svg>
      </button>
    </div>

    <!-- NODE PALETTE LIST -->
    <div class="sidebar-node-list">
      <div
        v-for="item in editorStore.nodePalette"
        :key="item.type"
        class="palette-node-item"
        :style="{ '--palette-color': item.color, '--palette-bg': item.bg }"
        draggable="true"
        :title="`${item.label.toUpperCase()} - Bấm hoặc kéo thả vào Canvas`"
        @click="handleAddNode(item.type)"
        @dragstart="handleDragStart($event, item.type)"
      >
        <!-- Drag Grip Handle -->
        <div v-if="!isCollapsed" class="node-grip-handle" title="Kéo thả vào Canvas">
          <svg width="10" height="14" viewBox="0 0 10 14" fill="none">
            <circle cx="3" cy="3" r="1.2" fill="currentColor" />
            <circle cx="7" cy="3" r="1.2" fill="currentColor" />
            <circle cx="3" cy="7" r="1.2" fill="currentColor" />
            <circle cx="7" cy="7" r="1.2" fill="currentColor" />
            <circle cx="3" cy="11" r="1.2" fill="currentColor" />
            <circle cx="7" cy="11" r="1.2" fill="currentColor" />
          </svg>
        </div>

        <!-- Node Icon Box -->
        <div class="node-icon-box">
          <span class="node-icon-symbol">{{ item.icon }}</span>
        </div>

        <!-- Node Label (Uppercase like APPROVAL) -->
        <span v-if="!isCollapsed" class="node-label">{{ item.label.toUpperCase() }}</span>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.editor-sidebar {
  width: 230px;
  height: 100%;
  background: #ffffff;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  user-select: none;
  z-index: 10;
  box-shadow: 2px 0 10px rgba(0, 0, 0, 0.02);
}

.editor-sidebar.is-collapsed {
  width: 64px;
}

/* ==========================================================
   HEADER
   ========================================================== */
.sidebar-header {
  height: 52px;
  padding: 0 0.875rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #f1f5f9;
  background: #ffffff;
}

.is-collapsed .sidebar-header {
  padding: 0 0.5rem;
  justify-content: center;
}

.header-title-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.header-icon-box {
  width: 26px;
  height: 26px;
  border-radius: 6px;
  background: #f0f9ff;
  color: #0284c7;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sidebar-title {
  font-size: 0.8125rem;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.2;
}

.btn-toggle-collapse {
  width: 26px;
  height: 26px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-toggle-collapse:hover {
  background: #f1f5f9;
  color: #0f172a;
  border-color: #cbd5e1;
}

.rotate-180 {
  transform: rotate(180deg);
}

/* ==========================================================
   NODE LIST
   ========================================================== */
.sidebar-node-list {
  flex: 1;
  overflow-y: auto;
  padding: 0.75rem 0.625rem;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.is-collapsed .sidebar-node-list {
  padding: 0.75rem 0.5rem;
  align-items: center;
}

.palette-node-item {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.45rem 0.625rem;
  border-radius: 8px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  cursor: grab;
  position: relative;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
}

.is-collapsed .palette-node-item {
  width: 44px;
  height: 44px;
  padding: 0;
  justify-content: center;
}

.palette-node-item:hover {
  border-color: var(--palette-color);
  background: #ffffff;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05), 0 0 0 1px var(--palette-color);
}

.palette-node-item:hover .node-label {
  color: var(--palette-color);
}

.palette-node-item:active {
  cursor: grabbing;
}

/* Grip handle */
.node-grip-handle {
  color: #cbd5e1;
  display: flex;
  align-items: center;
  transition: color 0.2s ease;
  flex-shrink: 0;
}

.palette-node-item:hover .node-grip-handle {
  color: var(--palette-color);
}

/* Icon Box */
.node-icon-box {
  width: 30px;
  height: 30px;
  border-radius: 6px;
  background: var(--palette-bg);
  border: 1px solid rgba(0, 0, 0, 0.04);
  color: var(--palette-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 0.8125rem;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.palette-node-item:hover .node-icon-box {
  transform: scale(1.05);
}

/* Node Label */
.node-label {
  font-size: 0.75rem;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: 0.03em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.2s ease;
}
</style>
