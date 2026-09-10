<script setup lang="ts">
import { useWorkflowStore } from '@/stores/workflowStore'

const store = useWorkflowStore()

const emit = defineEmits<{
  (e: 'create'): void
  (e: 'open-templates'): void
}>()
</script>

<template>
  <header class="workflow-header">
    <div class="header-left">
      <div class="breadcrumb">
        <span class="breadcrumb-root">Workflows</span>
        <span class="breadcrumb-separator">/</span>
        <span class="breadcrumb-current">Danh sách quy trình</span>
      </div>
      <h1 class="page-title">Quản lý Workflow</h1>
    </div>

    <div class="header-actions">
      <button class="btn btn-primary" @click="emit('create')">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <line x1="12" y1="5" x2="12" y2="19"></line>
          <line x1="5" y1="12" x2="19" y2="12"></line>
        </svg>
        <span>Tạo mới Workflow</span>
      </button>
    </div>
  </header>
</template>

<style scoped>
.workflow-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8125rem;
  color: var(--text-muted);
  margin-bottom: 0.25rem;
}

.breadcrumb-root {
  color: var(--text-secondary);
}

.breadcrumb-separator {
  color: var(--border-color);
}

.breadcrumb-current {
  color: var(--primary);
  font-weight: 600;
}

.page-title {
  font-size: 1.75rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--text-primary);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.125rem;
  border-radius: var(--radius-md);
  font-size: 0.875rem;
  font-weight: 600;
  transition: var(--transition);
}

.btn-primary {
  background: var(--accent-gradient);
  color: #ffffff;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.35);
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.5);
}

.btn-secondary {
  background: #ffffff;
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
}

.btn-secondary:hover {
  background: var(--bg-card-hover);
  color: var(--text-primary);
  border-color: #cbd5e1;
  transform: translateY(-1px);
}
</style>
