<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useWorkflowStore } from '@/stores/workflowStore'
import type { WorkflowItem } from '@/types/workflow'
import Badge from '@/components/common/Badge.vue'

const router = useRouter()
const store = useWorkflowStore()

const emit = defineEmits<{
  (e: 'edit', workflow: WorkflowItem): void
  (e: 'view-instances', workflow: WorkflowItem): void
}>()

// Dropdown context menu state
const activeMenuId = ref<string | null>(null)

const toggleMenu = (id: string, e: MouseEvent) => {
  e.stopPropagation()
  activeMenuId.value = activeMenuId.value === id ? null : id
}

// Close menu when clicking outside
window.addEventListener('click', () => {
  activeMenuId.value = null
})

// Format ngày giờ: Giờ:Phút, Ngày/Tháng/Năm
const formatDate = (dateStr?: string | Date) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return String(dateStr)

  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const year = d.getFullYear()

  return `${hours}:${minutes}, ${day}/${month}/${year}`
}
</script>

<template>
  <div class="table-card">
    <div class="table-responsive">
      <table class="workflow-table">
        <thead>
          <tr>
            <th class="th-code">ID</th>
            <th class="th-name">Workflow</th>
            <th class="th-version">Phiên bản</th>
            <th class="th-owner">Người sở hữu</th>
            <th class="th-status">Trạng thái</th>
            <th class="th-instances">Đang chạy</th>
            <th class="th-date">Cập nhật</th>
            <th class="th-actions text-right">Tác vụ</th>
          </tr>
        </thead>
        <tbody>
          <!-- Loading State -->
          <tr v-if="store.isLoading">
            <td colspan="8" class="empty-cell">
              <div class="empty-state">
                <div class="loading-spinner"></div>
                <h4>Đang tải danh sách workflow...</h4>
                <p>Vui lòng đợi trong giây lát</p>
              </div>
            </td>
          </tr>

          <!-- Empty State -->
          <tr v-else-if="store.filteredWorkflows.length === 0">
            <td colspan="8" class="empty-cell">
              <div class="empty-state">
                <div class="empty-icon">📂</div>
                <h4>Không tìm thấy quy trình nào</h4>
                <p>Thử điều chỉnh bộ lọc hoặc tạo mới quy trình đầu tiên của bạn.</p>
                <button class="btn-create-empty" @click="store.isCreateModalOpen = true">
                  + Tạo Workflow mới
                </button>
              </div>
            </td>
          </tr>

          <!-- Data Rows -->
          <tr v-else v-for="wf in store.filteredWorkflows" :key="wf.id" class="table-row">
            <!-- Code -->
            <td class="td-code">
              <span class="wf-code">{{ wf.code }}</span>
            </td>

            <!-- Name -->
            <td class="td-name">
              <div class="wf-info">
                <span class="wf-name" @click="emit('edit', wf)">{{ wf.name }}</span>
              </div>
            </td>

            <!-- Version -->
            <td>
              <Badge variant="version" :label="wf.version || 'v1.0'" />
            </td>

            <!-- Owner -->
            <td>
              <div class="owner-cell" :title="`${wf.owner?.name} (${wf.owner?.email})`">
                <div class="owner-text">
                  <span class="owner-name">{{ wf.owner?.name }}</span>
                  <span class="owner-role">{{ wf.owner?.role || 'Workflow Owner' }}</span>
                </div>
              </div>
            </td>

            <!-- Status -->
            <td>
              <Badge variant="status" :status="wf.status" />
            </td>

            <!-- Active Instances -->
            <td>
              <span
                class="instances-badge"
                :class="{ 'has-active': wf.activeInstances > 0 }"
                @click="emit('view-instances', wf)"
              >
                {{ wf.activeInstances }} đang chạy
              </span>
            </td>

            <!-- Updated At -->
            <td class="td-date">
              <span class="date-text">{{ formatDate(wf.updatedAt || wf.createdAt) }}</span>
            </td>

            <!-- Actions -->
            <td class="td-actions">
              <div class="actions-wrapper">
                <!-- Edit Action -->
                <button
                  class="action-btn"
                  title="Chỉnh sửa luồng (Visual Builder)"
                  @click="emit('edit', wf)"
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                  </svg>
                </button>

                <!-- Match Form Action -->
                <button
                  class="action-btn"
                  title="Ghép Biểu Mẫu (Match Form)"
                  @click="router.push(`/workflows/${wf.id}/match-form`)"
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"></path>
                    <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"></path>
                  </svg>
                </button>

                <!-- History Log Action -->
                <button
                  class="action-btn"
                  title="Lịch sử phiên bản & Audit Log"
                  @click="store.openVersionHistory(wf)"
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"></circle>
                    <polyline points="12 6 12 12 16 14"></polyline>
                  </svg>
                </button>

                <!-- Status Switch Toggle -->
                <button
                  class="status-toggle-btn"
                  :class="{ active: wf.status === 'published' }"
                  :title="wf.status === 'published' ? 'Nhấp để Tạm ngưng' : 'Nhấp để Kích hoạt'"
                  @click="store.toggleStatus(wf)"
                >
                  <span class="toggle-slider"></span>
                </button>

                <!-- More Menu (3 dots) -->
                <div class="menu-container">
                  <button class="action-btn menu-btn" title="Tùy chọn khác" @click="toggleMenu(String(wf.id), $event)">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                      <circle cx="12" cy="5" r="2"></circle>
                      <circle cx="12" cy="12" r="2"></circle>
                      <circle cx="12" cy="19" r="2"></circle>
                    </svg>
                  </button>

                  <!-- Context Dropdown -->
                  <div v-if="activeMenuId === String(wf.id)" class="dropdown-menu">
                    <button class="dropdown-item" @click="router.push(`/workflows/${wf.id}/match-form`)">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"></path>
                        <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"></path>
                      </svg>
                      <span>Ghép Biểu Mẫu (Match Form)</span>
                    </button>

                    <button class="dropdown-item" @click="emit('view-instances', wf)">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                        <circle cx="12" cy="12" r="3"></circle>
                      </svg>
                      <span>Xem Instances</span>
                    </button>

                    <!-- Nút Khôi phục nếu workflow đã bị xóa mềm -->
                    <button
                      v-if="wf.status === 'deleted' || wf.deletedAt"
                      class="dropdown-item item-restore"
                      @click="store.restoreWorkflow(wf)"
                    >
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="1 4 1 10 7 10"></polyline>
                        <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"></path>
                      </svg>
                      <span>Khôi phục quy trình</span>
                    </button>

                    <div class="dropdown-divider"></div>

                    <button
                      v-if="wf.status !== 'deleted' && !wf.deletedAt"
                      class="dropdown-item item-danger"
                      @click="store.confirmDelete(wf)"
                    >
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="3 6 5 6 21 6"></polyline>
                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                      </svg>
                      <span>Xóa quy trình</span>
                    </button>
                  </div>
                </div>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Table Footer / Pagination -->
    <div class="table-footer">
      <div class="footer-info">
        Hiển thị <strong>{{ store.filteredWorkflows.length }}</strong> trên tổng số <strong>{{ store.totalElements }}</strong> quy trình
      </div>
      <div class="pagination-controls" v-if="store.totalPages > 0">
        <button
          class="page-btn"
          :disabled="store.currentPage === 0"
          @click="store.prevPage"
        >
          ‹
        </button>
        <button
          v-for="p in store.totalPages"
          :key="p"
          class="page-btn"
          :class="{ active: store.currentPage === p - 1 }"
          @click="store.setPage(p - 1)"
        >
          {{ p }}
        </button>
        <button
          class="page-btn"
          :disabled="store.currentPage >= store.totalPages - 1"
          @click="store.nextPage"
        >
          ›
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.table-card {
  background: #ffffff;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.table-responsive {
  width: 100%;
  overflow-x: auto;
}

.workflow-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

th {
  background: #f8fafc;
  padding: 0.875rem 1rem;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  border-bottom: 1px solid var(--border-color);
}

.th-code {
  min-width: 140px;
}

.th-name {
  min-width: 240px;
}

.text-right {
  text-align: right;
}

td {
  padding: 1rem;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
  font-size: 0.875rem;
}

.table-row {
  transition: var(--transition);
}

.table-row:hover {
  background-color: #f8fafc;
}

/* Workflow info column */
.wf-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.wf-title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.wf-name {
  font-weight: 600;
  color: var(--text-primary);
  cursor: pointer;
  transition: var(--transition);
}

.wf-name:hover {
  color: var(--primary);
}

.wf-code {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.6875rem;
  font-weight: 500;
  padding: 2px 6px;
  border-radius: var(--radius-sm);
  background: #f1f5f9;
  color: var(--text-secondary);
  border: 1px solid #e2e8f0;
}

.wf-desc {
  font-size: 0.8125rem;
  color: var(--text-secondary);
  line-height: 1.35;
}

/* Owner cell */
.owner-cell {
  display: flex;
  align-items: center;
  gap: 0.625rem;
}

.owner-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #e2e8f0;
}

.owner-text {
  display: flex;
  flex-direction: column;
}

.owner-name {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--text-primary);
}

.owner-role {
  font-size: 0.6875rem;
  color: var(--text-muted);
}

/* Instances badge */
.instances-badge {
  font-size: 0.75rem;
  padding: 0.25rem 0.5rem;
  border-radius: var(--radius-sm);
  background: #f1f5f9;
  color: var(--text-secondary);
  cursor: pointer;
  transition: var(--transition);
  border: 1px solid #e2e8f0;
}

.instances-badge.has-active {
  background: #eef2ff;
  color: var(--primary);
  border-color: #c7d2fe;
  font-weight: 600;
}

.instances-badge:hover {
  background: #e0e7ff;
}

.date-text {
  font-size: 0.8125rem;
  color: var(--text-muted);
  font-family: 'JetBrains Mono', monospace;
}

/* Actions wrapper */
.actions-wrapper {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.5rem;
  position: relative;
}

.action-btn {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.action-btn:hover {
  color: var(--text-primary);
  background: #f1f5f9;
  border-color: #cbd5e1;
  transform: translateY(-1px);
}

/* Status switch toggle */
.status-toggle-btn {
  width: 36px;
  height: 20px;
  background: #cbd5e1;
  border-radius: var(--radius-full);
  position: relative;
  transition: var(--transition);
  padding: 2px;
}

.status-toggle-btn.active {
  background: #059669;
}

.toggle-slider {
  display: block;
  width: 16px;
  height: 16px;
  background: #ffffff;
  border-radius: 50%;
  transition: var(--transition);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.15);
}

.status-toggle-btn.active .toggle-slider {
  transform: translateX(16px);
}

/* Context Menu */
.menu-container {
  position: relative;
}

.dropdown-menu {
  position: absolute;
  right: 0;
  top: calc(100% + 6px);
  background: #ffffff;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  min-width: 180px;
  padding: 0.375rem;
  z-index: 50;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  font-size: 0.8125rem;
  color: var(--text-secondary);
  border-radius: var(--radius-sm);
  text-align: left;
  width: 100%;
}

.dropdown-item:hover {
  background: #f1f5f9;
  color: var(--text-primary);
}

.dropdown-divider {
  height: 1px;
  background: var(--border-color);
  margin: 0.25rem 0;
}

.item-restore {
  color: #059669;
  font-weight: 500;
}

.item-restore:hover {
  background: #ecfdf5;
  color: #047857;
}

.item-danger {
  color: #dc2626;
}

.item-danger:hover {
  background: #fef2f2;
  color: #b91c1c;
}

/* Empty state */
.empty-cell {
  padding: 4rem 1rem;
  text-align: center;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
}

.empty-icon {
  font-size: 2.5rem;
}

.empty-state h4 {
  font-size: 1.125rem;
  color: var(--text-primary);
}

.empty-state p {
  color: var(--text-muted);
  font-size: 0.875rem;
}

.btn-create-empty {
  margin-top: 0.5rem;
  padding: 0.5rem 1rem;
  background: var(--primary);
  color: #ffffff;
  border-radius: var(--radius-md);
  font-weight: 600;
  font-size: 0.875rem;
}

/* Footer & Pagination */
.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.875rem 1.25rem;
  border-top: 1px solid var(--border-color);
  background: #f8fafc;
  font-size: 0.8125rem;
  color: var(--text-secondary);
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.page-btn {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
  background: #ffffff;
}

.page-btn.active {
  background: var(--primary);
  color: #ffffff;
  border-color: var(--primary);
  font-weight: 600;
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.loading-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid rgba(59, 130, 246, 0.2);
  border-top-color: var(--primary, #3b82f6);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 0.75rem;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
