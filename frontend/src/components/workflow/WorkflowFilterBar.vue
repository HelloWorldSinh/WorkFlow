<script setup lang="ts">
import { useWorkflowStore } from '@/stores/workflowStore'
import type { WorkflowStatus, WorkflowModule } from '@/types/workflow'

const store = useWorkflowStore()

const statusOptions: { label: string; value: WorkflowStatus | 'all' }[] = [
  { label: 'Tất cả trạng thái', value: 'all' },
  { label: 'Published', value: 'published' },
  { label: 'Draft', value: 'draft' },
  { label: 'Suspended', value: 'suspended' },
]

const moduleOptions: { label: string; value: WorkflowModule | 'all' }[] = [
  { label: 'Tất cả Module', value: 'all' },
  { label: 'HR (Nhân sự)', value: 'HR' },
  { label: 'Procurement (Mua sắm)', value: 'Procurement' },
  { label: 'Finance (Tài chính)', value: 'Finance' },
  { label: 'IT Support', value: 'IT' },
  { label: 'Legal (Pháp chế)', value: 'Legal' },
]

const clearSearch = () => {
  store.filter.search = ''
}
</script>

<template>
  <div class="filter-container">
    <!-- Search Box -->
    <div class="search-box">
      <svg class="search-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="11" cy="11" r="8"></circle>
        <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
      </svg>
      <input
        v-model="store.filter.search"
        type="text"
        placeholder="Tìm kiếm theo tên, mã quy trình, người sở hữu..."
        class="search-input"
      />
      <button v-if="store.filter.search" class="btn-clear" @click="clearSearch">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="18" y1="6" x2="6" y2="18"></line>
          <line x1="6" y1="6" x2="18" y2="18"></line>
        </svg>
      </button>
    </div>

    <!-- Filter Dropdowns & Pills -->
    <div class="filters-group">
      <!-- Status Pills -->
      <div class="status-pills">
        <button
          v-for="opt in statusOptions"
          :key="opt.value"
          class="pill-btn"
          :class="{ active: store.filter.status === opt.value }"
          @click="store.filter.status = opt.value"
        >
          {{ opt.label }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.filter-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.25rem;
  flex-wrap: wrap;
}

.search-box {
  position: relative;
  flex: 1;
  min-width: 280px;
  max-width: 460px;
}

.search-icon {
  position: absolute;
  left: 0.875rem;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-muted);
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding-left: 2.5rem;
  padding-right: 2rem;
  background: #ffffff;
  height: 42px;
  box-shadow: var(--shadow-sm);
}

.btn-clear {
  position: absolute;
  right: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-muted);
  padding: 4px;
  border-radius: 50%;
}

.btn-clear:hover {
  color: var(--text-primary);
  background: #e2e8f0;
}

.filters-group {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.status-pills {
  display: flex;
  background: #ffffff;
  padding: 3px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
}

.pill-btn {
  padding: 0.375rem 0.75rem;
  border-radius: var(--radius-sm);
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--text-secondary);
}

.pill-btn:hover {
  color: var(--text-primary);
  background: #f1f5f9;
}

.pill-btn.active {
  background: var(--primary);
  color: #ffffff;
  font-weight: 600;
}

.select-wrapper {
  position: relative;
}

.filter-select {
  height: 42px;
  min-width: 170px;
  background: #ffffff;
  box-shadow: var(--shadow-sm);
}
</style>
