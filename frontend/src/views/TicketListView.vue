<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useTicketStore } from '@/stores/ticketStore'
import { useWorkflowStore } from '@/stores/workflowStore'
import type { TicketStatus, TicketItem } from '@/types/ticket'
import CreateTicketModal from '@/components/ticket/CreateTicketModal.vue'
import Toast from '@/components/common/Toast.vue'

const router = useRouter()
const ticketStore = useTicketStore()
const workflowStore = useWorkflowStore()

// State mở modal chọn workflow để tạo ticket
const isSelectWfModalOpen = ref(false)

onMounted(() => {
  ticketStore.fetchTickets(0)
  ticketStore.fetchStats()
  workflowStore.fetchWorkflows()
})

const handleStatusTab = (status: TicketStatus | 'ALL') => {
  ticketStore.setStatusFilter(status)
}

const handleSearch = () => {
  ticketStore.fetchTickets(0)
}

const handleViewDetail = (ticket: TicketItem) => {
  router.push(`/tickets/${ticket.id}`)
}

const handleOpenCreateTicket = () => {
  // Lọc các workflow đã Published
  const publishedWfs = workflowStore.workflows.filter((w) => w.status.toLowerCase() === 'published')
  if (publishedWfs.length === 1) {
    ticketStore.openCreateModal(publishedWfs[0])
  } else {
    isSelectWfModalOpen.value = true
  }
}

const handlePickWorkflow = (wf: any) => {
  isSelectWfModalOpen.value = false
  ticketStore.openCreateModal(wf)
}

const formatDateTime = (dateStr?: string) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  const hours = d.getHours().toString().padStart(2, '0')
  const mins = d.getMinutes().toString().padStart(2, '0')
  return `${hours}:${mins}, ${d.getDate()}/${d.getMonth() + 1}/${d.getFullYear()}`
}
</script>

<template>
  <div class="ticket-list-view">
    <!-- Top Header -->
    <div class="view-header">
      <div class="header-left">
        <h1 class="header-title">Giám sát Yêu cầu (Ticket Instances)</h1>
        <p class="header-subtitle">
          Theo dõi trạng thái, tiến độ và lịch sử xử lý của tất cả các luồng quy trình trong hệ thống
        </p>
      </div>

      <div class="header-actions">
        <button class="btn-create-ticket" @click="handleOpenCreateTicket">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          <span>Tạo Yêu Cầu Mới</span>
        </button>
      </div>
    </div>

    <!-- Metric Cards -->
    <div class="metrics-grid">
      <div class="metric-card card-total">
        <div class="metric-icon">📊</div>
        <div class="metric-info">
          <span class="metric-val">{{ ticketStore.stats.total }}</span>
          <span class="metric-label">Tổng Yêu Cầu</span>
        </div>
      </div>

      <div class="metric-card card-running" @click="handleStatusTab('Running')" style="cursor: pointer;">
        <div class="metric-icon">⏳</div>
        <div class="metric-info">
          <span class="metric-val text-running">{{ ticketStore.stats.running }}</span>
          <span class="metric-label">Đang Xử Lý (Running)</span>
        </div>
      </div>

      <div class="metric-card card-completed" @click="handleStatusTab('Completed')" style="cursor: pointer;">
        <div class="metric-icon">✅</div>
        <div class="metric-info">
          <span class="metric-val text-completed">{{ ticketStore.stats.completed }}</span>
          <span class="metric-label">Đã Hoàn Tất</span>
        </div>
      </div>

      <div class="metric-card card-rejected" @click="handleStatusTab('Rejected')" style="cursor: pointer;">
        <div class="metric-icon">🛑</div>
        <div class="metric-info">
          <span class="metric-val text-rejected">{{ ticketStore.stats.rejected }}</span>
          <span class="metric-label">Bị Từ Chối</span>
        </div>
      </div>
    </div>

    <!-- Filter Bar -->
    <div class="filter-bar">
      <!-- Status Tabs -->
      <div class="status-tabs">
        <button
          class="tab-btn"
          :class="{ active: ticketStore.filterStatus === 'ALL' }"
          @click="handleStatusTab('ALL')"
        >
          Tất cả ({{ ticketStore.stats.total }})
        </button>
        <button
          class="tab-btn"
          :class="{ active: ticketStore.filterStatus === 'Running' }"
          @click="handleStatusTab('Running')"
        >
          Đang chạy ({{ ticketStore.stats.running }})
        </button>
        <button
          class="tab-btn"
          :class="{ active: ticketStore.filterStatus === 'Completed' }"
          @click="handleStatusTab('Completed')"
        >
          Hoàn thành ({{ ticketStore.stats.completed }})
        </button>
        <button
          class="tab-btn"
          :class="{ active: ticketStore.filterStatus === 'Rejected' }"
          @click="handleStatusTab('Rejected')"
        >
          Từ chối ({{ ticketStore.stats.rejected }})
        </button>
      </div>

      <!-- Search Input -->
      <div class="search-box">
        <svg class="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8"></circle>
          <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
        </svg>
        <input
          v-model="ticketStore.filterKeyword"
          type="text"
          placeholder="Tìm theo mã TK, tên yêu cầu, người tạo..."
          class="search-input"
          @keyup.enter="handleSearch"
        />
      </div>
    </div>

    <!-- Table -->
    <div class="table-container">
      <table class="tickets-table">
        <thead>
          <tr>
            <th class="th-code">Mã Ticket</th>
            <th class="th-title">Tiêu Đề Yêu Cầu</th>
            <th class="th-workflow">Quy Trình</th>
            <th class="th-creator">Người Khởi Tạo</th>
            <th class="th-step">Bước Hiện Tại</th>
            <th class="th-status">Trạng Thái</th>
            <th class="th-time">Thời Gian Tạo</th>
            <th class="th-actions text-right">Tác Vụ</th>
          </tr>
        </thead>
        <tbody>
          <!-- Loading -->
          <tr v-if="ticketStore.isLoading">
            <td colspan="8" class="text-center py-6">
              <div class="loading-spinner"></div>
              <p class="mt-2 text-slate-500">Đang nạp dữ liệu ticket...</p>
            </td>
          </tr>

          <!-- Empty -->
          <tr v-else-if="ticketStore.tickets.length === 0">
            <td colspan="8" class="text-center py-8">
              <div class="empty-icon">📂</div>
              <h4 class="text-base font-bold text-slate-700 mt-2">Chưa có ticket nào phù hợp</h4>
              <p class="text-sm text-slate-500">Hãy khởi tạo yêu cầu mới hoặc thay đổi bộ lọc tìm kiếm.</p>
              <button class="btn-create-empty" @click="handleOpenCreateTicket">
                + Khởi tạo Yêu cầu ngay
              </button>
            </td>
          </tr>

          <!-- Rows -->
          <tr
            v-else
            v-for="t in ticketStore.tickets"
            :key="t.id"
            class="table-row"
            @click="handleViewDetail(t)"
          >
            <!-- Code -->
            <td>
              <span class="ticket-code">{{ t.requestCode }}</span>
            </td>

            <!-- Title -->
            <td>
              <span class="ticket-title" :title="t.title">{{ t.title }}</span>
            </td>

            <!-- Workflow -->
            <td>
              <span class="wf-badge">{{ t.workflowName }}</span>
            </td>

            <!-- Creator -->
            <td>
              <div class="creator-cell">
                <span class="creator-name">{{ t.creator?.fullName || 'N/A' }}</span>
                <span class="creator-email">{{ t.creator?.email }}</span>
              </div>
            </td>

            <!-- Current Step -->
            <td>
              <div class="step-cell">
                <span class="step-badge" :class="{ 'is-running': t.status === 'Running' }">
                  {{ t.currentNodeName || 'Bắt đầu' }}
                </span>
                <span v-if="t.currentAssignee" class="step-user">
                  ({{ t.currentAssignee.fullName }})
                </span>
              </div>
            </td>

            <!-- Status -->
            <td>
              <span class="status-badge" :class="t.status.toLowerCase()">
                <span class="status-dot"></span>
                <span>{{ t.status === 'Running' ? 'Đang xử lý' : t.status === 'Completed' ? 'Hoàn tất' : 'Bị từ chối' }}</span>
              </span>
            </td>

            <!-- Time -->
            <td>
              <span class="time-text">{{ formatDateTime(t.startTime) }}</span>
            </td>

            <!-- Actions -->
            <td class="text-right" @click.stop>
              <button class="btn-detail" @click="handleViewDetail(t)" title="Xem tiến trình trực quan">
                <span>Theo dõi</span>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <polyline points="9 18 15 12 9 6"></polyline>
                </svg>
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Footer Pagination -->
      <div v-if="ticketStore.totalPages > 1" class="table-footer">
        <span class="footer-info">
          Hiển thị <strong>{{ ticketStore.tickets.length }}</strong> trên tổng số <strong>{{ ticketStore.totalElements }}</strong> yêu cầu
        </span>
        <div class="pagination">
          <button
            class="page-btn"
            :disabled="ticketStore.currentPage === 0"
            @click="ticketStore.fetchTickets(ticketStore.currentPage - 1)"
          >
            ‹
          </button>
          <span class="page-current">{{ ticketStore.currentPage + 1 }} / {{ ticketStore.totalPages }}</span>
          <button
            class="page-btn"
            :disabled="ticketStore.currentPage >= ticketStore.totalPages - 1"
            @click="ticketStore.fetchTickets(ticketStore.currentPage + 1)"
          >
            ›
          </button>
        </div>
      </div>
    </div>

    <!-- Modal chọn Workflow để tạo ticket nếu có nhiều workflow -->
    <div v-if="isSelectWfModalOpen" class="custom-modal-backdrop" @click="isSelectWfModalOpen = false">
      <div class="select-wf-modal" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">Chọn Quy Trình Để Khởi Tạo Yêu Cầu</h3>
          <button class="close-btn" @click="isSelectWfModalOpen = false">✕</button>
        </div>
        <div class="wf-list-options">
          <div
            v-for="wf in workflowStore.workflows.filter(w => w.status.toLowerCase() === 'published')"
            :key="wf.id"
            class="wf-option-card"
            @click="handlePickWorkflow(wf)"
          >
            <div class="wf-card-icon">⚡</div>
            <div class="wf-card-info">
              <h4 class="wf-card-title">{{ wf.name }}</h4>
              <p class="wf-card-desc">{{ wf.description || 'Quy trình đã xuất bản và sẵn sàng sử dụng.' }}</p>
            </div>
            <button class="btn-pick-wf">Chọn</button>
          </div>
          <div v-if="workflowStore.workflows.filter(w => w.status.toLowerCase() === 'published').length === 0" class="empty-wf-notice">
            <p>Chưa có quy trình nào ở trạng thái <strong>Published</strong>. Vui lòng vào danh sách quy trình để xuất bản trước!</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Create Ticket Modal -->
    <CreateTicketModal />

    <!-- Toast Notification -->
    <Toast :message="ticketStore.toast" />
  </div>
</template>

<style scoped>
.ticket-list-view {
  max-width: 1440px;
  margin: 0 auto;
  padding: 2rem 1.5rem;
  min-height: 100vh;
}

.view-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 2rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.header-title {
  font-size: 1.625rem;
  font-weight: 800;
  color: #0f172a;
}

.header-subtitle {
  font-size: 0.875rem;
  color: #64748b;
  margin-top: 0.25rem;
}

.btn-create-ticket {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  background: linear-gradient(135deg, #0284c7 0%, #0369a1 100%);
  color: #ffffff;
  font-weight: 700;
  font-size: 0.875rem;
  border-radius: 10px;
  border: none;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(2, 132, 199, 0.35);
  transition: all 0.2s ease;
}

.btn-create-ticket:hover {
  background: linear-gradient(135deg, #0369a1 0%, #075985 100%);
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(2, 132, 199, 0.45);
}

/* Metrics */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1.25rem;
  margin-bottom: 2rem;
}

.metric-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  padding: 1.25rem 1.5rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.2s ease;
}

.metric-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}

.metric-icon {
  font-size: 2rem;
}

.metric-val {
  font-size: 1.75rem;
  font-weight: 800;
  color: #0f172a;
  line-height: 1;
}

.text-running { color: #0284c7; }
.text-completed { color: #10b981; }
.text-rejected { color: #ef4444; }

.metric-label {
  font-size: 0.8125rem;
  color: #64748b;
  font-weight: 600;
  margin-top: 0.25rem;
}

/* Filter Bar */
.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.25rem;
  gap: 1rem;
  flex-wrap: wrap;
}

.status-tabs {
  display: flex;
  background: #f1f5f9;
  padding: 4px;
  border-radius: 10px;
  gap: 4px;
}

.tab-btn {
  padding: 0.5rem 1rem;
  border: none;
  background: transparent;
  font-size: 0.8125rem;
  font-weight: 600;
  color: #64748b;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tab-btn.active {
  background: #ffffff;
  color: #0284c7;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.search-box {
  display: flex;
  align-items: center;
  position: relative;
  min-width: 320px;
}

.search-icon {
  position: absolute;
  left: 12px;
  color: #94a3b8;
}

.search-input {
  width: 100%;
  height: 40px;
  padding-left: 38px;
  padding-right: 12px;
  border: 1.5px solid #cbd5e1;
  border-radius: 10px;
  font-size: 0.875rem;
  background: #ffffff;
}

.search-input:focus {
  outline: none;
  border-color: #0284c7;
  box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.15);
}

/* Table */
.table-container {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 16px -2px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.tickets-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.tickets-table th {
  background: #f8fafc;
  padding: 1rem 1.25rem;
  font-size: 0.75rem;
  font-weight: 700;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border-bottom: 1px solid #e2e8f0;
}

.tickets-table td {
  padding: 1rem 1.25rem;
  border-bottom: 1px solid #f1f5f9;
  font-size: 0.875rem;
}

.table-row {
  cursor: pointer;
  transition: all 0.2s ease;
}

.table-row:hover {
  background: #f0f9ff;
}

.ticket-code {
  font-family: monospace;
  font-weight: 700;
  color: #0284c7;
  background: #e0f2fe;
  padding: 3px 8px;
  border-radius: 6px;
  font-size: 0.8125rem;
}

.ticket-title {
  font-weight: 700;
  color: #0f172a;
}

.wf-badge {
  font-size: 0.8125rem;
  color: #475569;
  font-weight: 500;
}

.creator-cell {
  display: flex;
  flex-direction: column;
}

.creator-name {
  font-weight: 600;
  color: #1e293b;
}

.creator-email {
  font-size: 0.75rem;
  color: #64748b;
}

.step-cell {
  display: flex;
  align-items: center;
  gap: 0.375rem;
}

.step-badge {
  font-size: 0.75rem;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 6px;
  background: #f1f5f9;
  color: #475569;
}

.step-badge.is-running {
  background: #e0f2fe;
  color: #0284c7;
  font-weight: 700;
}

.step-user {
  font-size: 0.75rem;
  color: #64748b;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 9999px;
}

.status-badge.running {
  background: #e0f2fe;
  color: #0284c7;
}

.status-badge.completed {
  background: #ecfdf5;
  color: #059669;
}

.status-badge.rejected {
  background: #fef2f2;
  color: #dc2626;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.time-text {
  font-size: 0.8125rem;
  color: #64748b;
}

.btn-detail {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0.375rem 0.75rem;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  color: #0284c7;
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-detail:hover {
  background: #0284c7;
  color: #ffffff;
  border-color: #0284c7;
}

.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.page-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
  background: #ffffff;
  cursor: pointer;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.custom-modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.select-wf-modal {
  background: #ffffff;
  border-radius: 14px;
  width: 90%;
  max-width: 580px;
  padding: 1.5rem;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.25rem;
}

.modal-title {
  font-size: 1.125rem;
  font-weight: 700;
  color: #0f172a;
}

.close-btn {
  background: transparent;
  border: none;
  font-size: 1.125rem;
  cursor: pointer;
  color: #64748b;
}

.wf-list-options {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  max-height: 60vh;
  overflow-y: auto;
}

.wf-option-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.wf-option-card:hover {
  border-color: #0284c7;
  background: #f0f9ff;
}

.wf-card-icon {
  font-size: 1.5rem;
}

.wf-card-info {
  flex: 1;
}

.wf-card-title {
  font-size: 0.9375rem;
  font-weight: 700;
  color: #0f172a;
}

.wf-card-desc {
  font-size: 0.8125rem;
  color: #64748b;
}

.btn-pick-wf {
  padding: 0.375rem 0.875rem;
  background: #0284c7;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
}

.btn-create-empty {
  margin-top: 1rem;
  padding: 0.625rem 1.25rem;
  background: #0284c7;
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
}
</style>
