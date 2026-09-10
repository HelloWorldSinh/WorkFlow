<script setup lang="ts">
import type { WorkflowStatus, WorkflowModule } from '@/types/workflow'

interface Props {
  variant?: 'status' | 'version' | 'module' | 'default'
  status?: WorkflowStatus
  module?: WorkflowModule
  label?: string
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'default',
})

const getStatusConfig = (status?: WorkflowStatus) => {
  switch (status) {
    case 'published':
      return { text: 'Published', class: 'status-published', dot: '#059669' }
    case 'draft':
      return { text: 'Draft', class: 'status-draft', dot: '#d97706' }
    case 'suspended':
      return { text: 'Suspended', class: 'status-suspended', dot: '#64748b' }
    case 'deleted':
      return { text: 'Deleted', class: 'status-deleted', dot: '#dc2626' }
    default:
      return { text: 'Unknown', class: 'status-unknown', dot: '#94a3b8' }
  }
}

const getModuleColor = (mod?: WorkflowModule) => {
  switch (mod) {
    case 'HR':
      return 'module-hr'
    case 'Procurement':
      return 'module-procurement'
    case 'Finance':
      return 'module-finance'
    case 'IT':
      return 'module-it'
    case 'Legal':
      return 'module-legal'
    case 'Operations':
      return 'module-ops'
    default:
      return 'module-default'
  }
}
</script>

<template>
  <!-- Status Variant -->
  <span v-if="variant === 'status'" :class="['badge-base', getStatusConfig(status).class]">
    <span class="status-dot" :style="{ backgroundColor: getStatusConfig(status).dot }"></span>
    <span>{{ label || getStatusConfig(status).text }}</span>
  </span>

  <!-- Version Variant -->
  <span v-else-if="variant === 'version'" class="badge-base badge-version">
    {{ label }}
  </span>

  <!-- Module Variant -->
  <span v-else-if="variant === 'module'" :class="['badge-base', getModuleColor(module)]">
    {{ label || module }}
  </span>

  <!-- Default Variant -->
  <span v-else class="badge-base badge-default">
    {{ label }}
  </span>
</template>

<style scoped>
.badge-base {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.25rem 0.625rem;
  border-radius: var(--radius-full);
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.01em;
  white-space: nowrap;
  transition: var(--transition);
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

/* Status variants for Light Mode */
.status-published {
  background: var(--status-published-bg);
  color: #047857;
  border: 1px solid var(--status-published-border);
}

.status-draft {
  background: var(--status-draft-bg);
  color: #b45309;
  border: 1px solid var(--status-draft-border);
}

.status-suspended {
  background: var(--status-suspended-bg);
  color: #475569;
  border: 1px solid var(--status-suspended-border);
}

.status-deleted {
  background: var(--status-danger-bg);
  color: #b91c1c;
  border: 1px solid var(--status-danger-border);
}

/* Version badge */
.badge-version {
  background: #f1f5f9;
  color: #4338ca;
  border: 1px solid #c7d2fe;
  font-family: 'JetBrains Mono', monospace;
  font-weight: 600;
}

/* Module tags */
.module-hr {
  background: #fdf2f8;
  color: #be185d;
  border: 1px solid #fbcfe8;
}

.module-procurement {
  background: #faf5ff;
  color: #7e22ce;
  border: 1px solid #e9d5ff;
}

.module-finance {
  background: #f0f9ff;
  color: #0369a1;
  border: 1px solid #bae6fd;
}

.module-it {
  background: #f0fdf4;
  color: #15803d;
  border: 1px solid #bbf7d0;
}

.module-legal {
  background: #fff7ed;
  color: #c2410c;
  border: 1px solid #fed7aa;
}

.module-ops {
  background: #f8fafc;
  color: #334155;
  border: 1px solid #cbd5e1;
}

.badge-default {
  background: #f1f5f9;
  color: var(--text-secondary);
}
</style>
