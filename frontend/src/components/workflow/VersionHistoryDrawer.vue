<script setup lang="ts">
import { computed } from 'vue'
import { useWorkflowStore } from '@/stores/workflowStore'
import Badge from '@/components/common/Badge.vue'

const store = useWorkflowStore()
const workflow = computed(() => store.selectedWorkflow)

const logs = computed(() => {
  if (!workflow.value) return []
  return store.versionLogs[workflow.value.id] || [
    {
      version: workflow.value.version,
      updatedBy: workflow.value.owner.name,
      updatedAt: workflow.value.updatedAt,
      isCurrent: true,
      changes: ['Bản cập nhật gần nhất của quy trình'],
    },
  ]
})

const handleClose = () => {
  store.isHistoryDrawerOpen = false
  store.selectedWorkflow = null
}
</script>

<template>
  <Teleport to="body">
    <Transition name="drawer-slide">
      <div v-if="store.isHistoryDrawerOpen" class="drawer-overlay" @click.self="handleClose">
        <div class="drawer-panel">
          <!-- Drawer Header -->
          <div class="drawer-header">
            <div class="drawer-title-group">
              <span class="drawer-subtitle">Lịch sử phiên bản & Audit Log</span>
              <h3 class="drawer-title">{{ workflow?.name }}</h3>
            </div>
            <button class="btn-close" @click="handleClose">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M18 6L6 18M6 6l12 12" />
              </svg>
            </button>
          </div>

          <!-- Drawer Body: Timeline -->
          <div class="drawer-body">
            <div class="version-timeline">
              <div
                v-for="(log, idx) in logs"
                :key="log.version"
                class="timeline-item"
                :class="{ 'is-latest': log.isCurrent }"
              >
                <!-- Timeline Dot -->
                <div class="timeline-dot">
                  <span v-if="log.isCurrent" class="current-pulse"></span>
                </div>

                <!-- Timeline Content -->
                <div class="timeline-content">
                  <div class="timeline-header">
                    <div class="version-tags">
                      <Badge variant="version" :label="log.version" />
                      <span v-if="log.isCurrent" class="tag-current">Hiện tại (Active)</span>
                    </div>
                    <span class="log-date">{{ log.updatedAt }}</span>
                  </div>

                  <div class="log-author">
                    <span class="author-label">Người chỉnh sửa:</span>
                    <strong>{{ log.updatedBy }}</strong>
                  </div>

                  <!-- Change list -->
                  <div class="log-changes">
                    <span class="changes-title">Nội dung thay đổi:</span>
                    <ul class="changes-list">
                      <li v-for="(change, cIdx) in log.changes" :key="cIdx">
                        {{ change }}
                      </li>
                    </ul>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Drawer Footer -->
          <div class="drawer-footer">
            <button class="btn btn-secondary" @click="handleClose">Đóng</button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.drawer-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(4px);
  z-index: 999;
  display: flex;
  justify-content: flex-end;
}

.drawer-panel {
  width: 100%;
  max-width: 480px;
  height: 100%;
  background: #ffffff;
  border-left: 1px solid var(--border-color);
  box-shadow: -10px 0 30px rgba(15, 23, 42, 0.1);
  display: flex;
  flex-direction: column;
}

.drawer-header {
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  background: #ffffff;
}

.drawer-subtitle {
  font-size: 0.75rem;
  color: var(--primary);
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.drawer-title {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--text-primary);
  margin-top: 0.25rem;
  line-height: 1.3;
}

.btn-close {
  color: var(--text-muted);
  padding: 4px;
  border-radius: var(--radius-sm);
}

.btn-close:hover {
  color: var(--text-primary);
  background: var(--bg-card-hover);
}

.drawer-body {
  padding: 1.5rem;
  flex: 1;
  overflow-y: auto;
  background: #f8fafc;
}

/* Timeline */
.version-timeline {
  display: flex;
  flex-direction: column;
  position: relative;
}

.version-timeline::before {
  content: '';
  position: absolute;
  left: 7px;
  top: 10px;
  bottom: 10px;
  width: 2px;
  background: #e2e8f0;
}

.timeline-item {
  display: flex;
  gap: 1.25rem;
  position: relative;
  padding-bottom: 1.5rem;
}

.timeline-item:last-child {
  padding-bottom: 0;
}

.timeline-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #ffffff;
  border: 2px solid #cbd5e1;
  z-index: 2;
  margin-top: 4px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.timeline-item.is-latest .timeline-dot {
  border-color: var(--primary);
  background: var(--primary);
}

.current-pulse {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #ffffff;
}

.timeline-content {
  flex: 1;
  background: #ffffff;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 1rem;
  box-shadow: var(--shadow-sm);
}

.timeline-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.version-tags {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.tag-current {
  font-size: 0.6875rem;
  font-weight: 600;
  color: #047857;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  padding: 2px 6px;
  border-radius: var(--radius-sm);
}

.log-date {
  font-size: 0.75rem;
  color: var(--text-muted);
  font-family: 'JetBrains Mono', monospace;
}

.log-author {
  font-size: 0.8125rem;
  color: var(--text-secondary);
  margin-bottom: 0.75rem;
}

.author-label {
  color: var(--text-muted);
  margin-right: 4px;
}

.changes-title {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--text-secondary);
  display: block;
  margin-bottom: 0.375rem;
}

.changes-list {
  padding-left: 1.25rem;
  font-size: 0.8125rem;
  color: var(--text-secondary);
  line-height: 1.45;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.drawer-footer {
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--border-color);
  display: flex;
  justify-content: flex-end;
  background: #ffffff;
}

.btn {
  padding: 0.5rem 1rem;
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

/* Animations */
.drawer-slide-enter-active,
.drawer-slide-leave-active {
  transition: opacity 0.25s ease;
}

.drawer-slide-enter-from,
.drawer-slide-leave-to {
  opacity: 0;
}

.drawer-slide-enter-active .drawer-panel {
  transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.drawer-slide-enter-from .drawer-panel {
  transform: translateX(100%);
}
</style>
