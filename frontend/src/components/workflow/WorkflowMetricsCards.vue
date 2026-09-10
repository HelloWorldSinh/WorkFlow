<script setup lang="ts">
import { computed } from 'vue'
import { useWorkflowStore } from '@/stores/workflowStore'

const store = useWorkflowStore()
const metrics = computed(() => store.metrics)

const cards = computed(() => [
  {
    id: 'total',
    label: 'Tổng số Workflow',
    value: metrics.value.total,
    iconColor: '#6366f1',
    iconBg: '#eef2ff',
    gradientBg: 'linear-gradient(135deg, #ffffff 0%, #f8faff 100%)',
    iconSvg: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"></path></svg>`,
  },
  {
    id: 'published',
    label: 'Đang hoạt động (Published)',
    value: metrics.value.published,
    iconColor: '#059669',
    iconBg: '#ecfdf5',
    gradientBg: 'linear-gradient(135deg, #ffffff 0%, #f0fdf4 100%)',
    iconSvg: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>`,
  },
  {
    id: 'draft',
    label: 'Bản nháp (Draft)',
    value: metrics.value.draft,
    iconColor: '#d97706',
    iconBg: '#fffbeb',
    gradientBg: 'linear-gradient(135deg, #ffffff 0%, #fffdf5 100%)',
    iconSvg: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 20h9"></path><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path></svg>`,
  },
  {
    id: 'suspended',
    label: 'Tạm ngưng (Suspended)',
    value: metrics.value.suspended,
    iconColor: '#64748b',
    iconBg: '#f8fafc',
    gradientBg: 'linear-gradient(135deg, #ffffff 0%, #f8fafc 100%)',
    iconSvg: `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="10" y1="15" x2="10" y2="9"></line><line x1="14" y1="15" x2="14" y2="9"></line></svg>`,
  },
])
</script>

<template>
  <div class="metrics-grid">
    <div
      v-for="card in cards"
      :key="card.id"
      class="metric-card"
      :style="{ background: card.gradientBg }"
    >
      <div class="metric-content">
        <span class="metric-label">{{ card.label }}</span>
        <div class="metric-value">{{ card.value }}</div>
      </div>
      <div
        class="metric-icon"
        :style="{ color: card.iconColor, backgroundColor: card.iconBg }"
        v-html="card.iconSvg"
      ></div>
    </div>
  </div>
</template>

<style scoped>
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.metric-card {
  background: #ffffff;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 1.25rem 1.5rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: var(--shadow-sm);
  transition: var(--transition);
}

.metric-card:hover {
  transform: translateY(-2px);
  border-color: #cbd5e1;
  box-shadow: var(--shadow-md);
}

.metric-label {
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--text-secondary);
  display: block;
  margin-bottom: 0.375rem;
}

.metric-value {
  font-size: 2rem;
  font-weight: 800;
  color: var(--text-primary);
  line-height: 1.1;
  letter-spacing: -0.02em;
}

.metric-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(0, 0, 0, 0.04);
}
</style>
