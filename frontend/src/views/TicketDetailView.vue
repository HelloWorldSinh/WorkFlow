<script setup lang="ts">
import { onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTicketStore } from '@/stores/ticketStore'
import TicketStepTracker from '@/components/ticket/TicketStepTracker.vue'
import TaskActionBox from '@/components/ticket/TaskActionBox.vue'
import TicketTimeline from '@/components/ticket/TicketTimeline.vue'
import Toast from '@/components/common/Toast.vue'

const route = useRoute()
const router = useRouter()
const ticketStore = useTicketStore()

const ticketId = computed(() => Number(route.params.id))

onMounted(() => {
  if (ticketId.value) {
    ticketStore.fetchTicketDetail(ticketId.value)
  }
})

watch(
  () => route.params.id,
  (newId) => {
    if (newId) {
      ticketStore.fetchTicketDetail(Number(newId))
    }
  }
)

const ticket = computed(() => ticketStore.currentTicket)

const formatDateTime = (dateStr?: string | null) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  const hours = d.getHours().toString().padStart(2, '0')
  const mins = d.getMinutes().toString().padStart(2, '0')
  return `${hours}:${mins}, ${d.getDate()}/${d.getMonth() + 1}/${d.getFullYear()}`
}

const formatValue = (key: string, val: any) => {
  if (val === null || val === undefined) return '-'
  if (typeof val === 'number') {
    // Nếu là tiền tệ (> 1000)
    if (val >= 1000 && (key.toLowerCase().includes('amount') || key.toLowerCase().includes('cost') || key.toLowerCase().includes('price'))) {
      return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
    }
    return val.toLocaleString('vi-VN')
  }
  if (typeof val === 'boolean') {
    return val ? 'Có' : 'Không'
  }
  return String(val)
}

// Helper kiểm tra dữ liệu dạng bảng (mảng các đối tượng)
const isTableData = (val: any): boolean => {
  return Array.isArray(val) && val.length > 0 && typeof val[0] === 'object' && val[0] !== null
}

const isCurrencyField = (key: string): boolean => {
  const k = key.toLowerCase()
  return k.includes('cost') || k.includes('price') || k.includes('amount')
}

const formatColumnLabel = (key: string): string => {
  const map: Record<string, string> = {
    name: 'Họ tên nhân sự',
    fullName: 'Họ tên nhân sự',
    email: 'Email liên hệ',
    account: 'Tài khoản (Account)',
    role: 'Vị trí / Chức danh',
    position: 'Vị trí / Chức danh',
    device_model: 'Dòng máy / Cấu hình',
    device_type: 'Loại máy',
    config: 'Cấu hình đề xuất',
    cost: 'Đơn giá dự toán',
    price: 'Đơn giá dự toán',
    amount: 'Đơn giá',
    note: 'Ghi chú',
    department: 'Phòng ban'
  }
  return map[key] || key.replace(/_/g, ' ').replace(/\b\w/g, (l) => l.toUpperCase())
}

const getTableColumns = (rows: any[]): { key: string; label: string }[] => {
  if (!rows || rows.length === 0) return []
  const first = rows[0]
  return Object.keys(first)
    .filter((k) => !k.startsWith('__') && k !== 'id' && k !== 'departmentId')
    .map((k) => ({
      key: k,
      label: formatColumnLabel(k),
    }))
}

const calculateArrayTotal = (rows: any[], key: string = 'cost'): number => {
  return rows.reduce((acc, r) => {
    const v = Number(r[key] || r['price'] || r['amount'] || 0)
    return acc + (isNaN(v) ? 0 : v)
  }, 0)
}

// Lọc các biến thông thường (scalar)
const scalarVariables = computed(() => {
  if (!ticket.value?.variables) return []
  return Object.entries(ticket.value.variables)
    .filter(([key, val]) => !key.startsWith('__') && !isTableData(val))
    .map(([key, val]) => ({
      key,
      label: formatColumnLabel(key),
      value: val,
    }))
})

// Lọc các biến dạng bảng kê (table / dynamic list)
const tableVariables = computed(() => {
  if (!ticket.value?.variables) return []
  return Object.entries(ticket.value.variables)
    .filter(([key, val]) => !key.startsWith('__') && isTableData(val))
    .map(([key, val]) => ({
      key,
      label: formatColumnLabel(key),
      value: val as any[],
    }))
})
</script>

<template>
  <div class="ticket-detail-view">
    <!-- Top Back & Actions -->
    <div class="top-nav-bar">
      <button class="btn-back" @click="router.push('/tickets')">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <line x1="19" y1="12" x2="5" y2="12"></line>
          <polyline points="12 19 5 12 12 5"></polyline>
        </svg>
        <span>Danh sách Yêu cầu</span>
      </button>

      <div class="top-actions">
        <button class="btn-refresh" @click="ticketStore.fetchTicketDetail(ticketId)">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="23 4 23 10 17 10"></polyline>
            <polyline points="1 20 1 14 7 14"></polyline>
            <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path>
          </svg>
          <span>Làm mới tiến trình</span>
        </button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="ticketStore.isLoadingDetail" class="detail-loading">
      <div class="spinner-large"></div>
      <h4>Đang nạp dữ liệu chi tiết Ticket...</h4>
    </div>

    <!-- Ticket Detail Content -->
    <div v-else-if="ticket" class="ticket-container">
      <!-- Main Title Header Card -->
      <div class="ticket-header-card">
        <div class="header-left-col">
          <div class="code-badge-row">
            <span class="request-code">{{ ticket.requestCode }}</span>
            <span class="status-pill" :class="ticket.status.toLowerCase()">
              <span class="pulse-dot"></span>
              <span>{{ ticket.status === 'Running' ? 'Đang xử lý (Running)' : ticket.status === 'Completed' ? 'Hoàn tất (Completed)' : 'Bị từ chối (Rejected)' }}</span>
            </span>
          </div>

          <h1 class="ticket-main-title">{{ ticket.title }}</h1>

          <div class="ticket-meta-row">
            <span class="meta-item">
              <span class="meta-label">Quy trình:</span>
              <router-link
                :to="{ path: `/workflows/${ticket.workflowId}/editor`, query: { name: ticket.workflowName } }"
                target="_blank"
                rel="noopener noreferrer"
                class="meta-link"
                title="Mở trình chỉnh sửa quy trình trong tab mới"
              >
                <span>{{ ticket.workflowName }} ({{ ticket.workflowCode }})</span>
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" class="external-icon">
                  <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"></path>
                  <polyline points="15 3 21 3 21 9"></polyline>
                  <line x1="10" y1="14" x2="21" y2="3"></line>
                </svg>
              </router-link>
            </span>
            <span class="meta-divider">•</span>
            <span class="meta-item">
              <span class="meta-label">Người tạo:</span>
              <strong>{{ ticket.creator?.fullName || 'N/A' }}</strong>
            </span>
            <span class="meta-divider">•</span>
            <span class="meta-item">
              <span class="meta-label">Thời gian tạo:</span>
              <span>{{ formatDateTime(ticket.startTime) }}</span>
            </span>
          </div>
        </div>
      </div>

      <!-- 1. LIVE STEP TRACKER -->
      <TicketStepTracker :ticket="ticket" />

      <!-- 2. MAIN CONTENT (ACTION BOX, FORM DATA, AUDIT TRAIL) -->
      <div class="main-column">
        <!-- Approval Action Box (shown if pending task exists) -->
        <TaskActionBox :ticket="ticket" />

        <!-- Form Data Card -->
        <div class="card form-data-card">
            <div class="card-header">
              <h3 class="card-title">Dữ liệu Biểu mẫu Yêu cầu (Form Data)</h3>
              <span v-if="ticket.formName" class="form-tag">Mẫu: {{ ticket.formName }}</span>
            </div>

            <!-- 1. BIẾN THÔNG THƯỜNG (SCALAR FIELDS) -->
            <div v-if="scalarVariables.length > 0" class="data-grid">
              <div v-for="item in scalarVariables" :key="item.key" class="data-item">
                <span class="data-label">{{ item.label }}</span>
                <span class="data-value">{{ formatValue(item.key, item.value) }}</span>
              </div>
            </div>

            <!-- 2. BẢNG KÊ DỮ LIỆU ĐỘNG (TABLE / REPEATER) -->
            <div v-if="tableVariables.length > 0" class="tables-container">
              <div v-for="tbl in tableVariables" :key="tbl.key" class="table-block">
                <div class="table-block-header">
                  <div class="tbl-title-row">
                    <span class="tbl-icon">📋</span>
                    <h4 class="tbl-title">{{ tbl.label }}</h4>
                    <span class="tbl-badge">{{ tbl.value.length }} nhân sự</span>
                  </div>
                </div>

                <div class="table-responsive">
                  <table class="batch-data-table">
                    <thead>
                      <tr>
                        <th style="width: 45px">#</th>
                        <th v-for="col in getTableColumns(tbl.value)" :key="col.key">
                          {{ col.label }}
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(row, rIdx) in tbl.value" :key="rIdx">
                        <td class="col-index">{{ rIdx + 1 }}</td>
                        <td v-for="col in getTableColumns(tbl.value)" :key="col.key">
                          <template v-if="isCurrencyField(col.key)">
                            <span class="price-highlight">{{ Number(row[col.key] || 0).toLocaleString('vi-VN') }} VNĐ</span>
                          </template>
                          <template v-else>
                            {{ row[col.key] || '-' }}
                          </template>
                        </td>
                      </tr>
                    </tbody>
                    <tfoot>
                      <tr>
                        <td :colspan="getTableColumns(tbl.value).length + 1" class="tbl-summary-footer">
                          <span>Tổng cộng: <strong>{{ tbl.value.length }}</strong> nhân sự</span>
                          <span v-if="calculateArrayTotal(tbl.value) > 0" class="tbl-sum-total">
                            • Tổng kinh phí dự toán: <strong>{{ calculateArrayTotal(tbl.value).toLocaleString('vi-VN') }} VNĐ</strong>
                          </span>
                        </td>
                      </tr>
                    </tfoot>
                  </table>
                </div>
              </div>
            </div>

            <div v-if="scalarVariables.length === 0 && tableVariables.length === 0" class="empty-form-data">
              <p>Không có dữ liệu trường biểu mẫu bổ sung.</p>
            </div>
          </div>

          <!-- Audit Trail & Timeline -->
          <TicketTimeline :ticket="ticket" />
        </div>
      </div>

    <!-- Global Toast -->
    <Toast :message="ticketStore.toast" />
  </div>
</template>

<style scoped>
.ticket-detail-view {
  max-width: 1440px;
  margin: 0 auto;
  padding: 1.5rem;
  min-height: 100vh;
}

.top-nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
}

.btn-back {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.875rem;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.8125rem;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-back:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.btn-refresh {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.875rem;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.8125rem;
  font-weight: 600;
  color: #0284c7;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-refresh:hover {
  background: #f0f9ff;
  border-color: #0284c7;
}

.detail-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 5rem 1rem;
  color: #64748b;
  gap: 1rem;
}

.spinner-large {
  width: 48px;
  height: 48px;
  border: 4px solid #e2e8f0;
  border-top-color: #0284c7;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.ticket-container {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* Header Card */
.ticket-header-card {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  padding: 1.5rem 1.75rem;
  box-shadow: 0 4px 16px -2px rgba(0, 0, 0, 0.05);
}

.code-badge-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.5rem;
}

.request-code {
  font-family: monospace;
  font-size: 0.9375rem;
  font-weight: 800;
  color: #0284c7;
  background: #e0f2fe;
  padding: 3px 10px;
  border-radius: 6px;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 9999px;
}

.status-pill.running {
  background: #e0f2fe;
  color: #0284c7;
}

.status-pill.completed {
  background: #ecfdf5;
  color: #059669;
}

.status-pill.rejected {
  background: #fef2f2;
  color: #dc2626;
}

.pulse-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.ticket-main-title {
  font-size: 1.5rem;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.3;
  margin-bottom: 0.75rem;
}

.ticket-meta-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.75rem;
  font-size: 0.8125rem;
  color: #475569;
}

.meta-link {
  color: #0284c7;
  font-weight: 600;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.meta-link:hover {
  text-decoration: underline;
}

.meta-link .external-icon {
  opacity: 0.7;
  transition: opacity 0.2s ease;
}

.meta-link:hover .external-icon {
  opacity: 1;
}

.meta-divider {
  color: #cbd5e1;
}

/* Layout */
.main-column {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  width: 100%;
}

/* Common Card */
.card {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  padding: 1.5rem;
  box-shadow: 0 4px 16px -2px rgba(0, 0, 0, 0.05);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.25rem;
}

.card-title {
  font-size: 1.0625rem;
  font-weight: 700;
  color: #0f172a;
}

.form-tag {
  font-size: 0.75rem;
  font-weight: 600;
  color: #0284c7;
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  padding: 2px 8px;
  border-radius: 6px;
}

/* Form Data Grid */
.data-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 1rem;
}

.data-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  padding: 0.75rem;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #f1f5f9;
}

.data-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: #64748b;
  text-transform: capitalize;
}

.data-value {
  font-size: 0.9375rem;
  font-weight: 600;
  color: #0f172a;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Batch / Table Data Styles in Ticket Detail */
.tables-container {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  margin-top: 1rem;
}

.table-block {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  overflow: hidden;
  background: #ffffff;
}

.table-block-header {
  padding: 0.75rem 1rem;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.tbl-title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.tbl-icon {
  font-size: 1.1rem;
}

.tbl-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.tbl-badge {
  font-size: 0.75rem;
  font-weight: 600;
  color: #0284c7;
  background: #e0f2fe;
  padding: 2px 8px;
  border-radius: 9999px;
  margin-left: auto;
}

.batch-data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.8125rem;
}

.batch-data-table th {
  background: #f8fafc;
  color: #475569;
  font-weight: 700;
  padding: 0.625rem 0.875rem;
  text-align: left;
  border-bottom: 1.5px solid #cbd5e1;
  white-space: nowrap;
}

.batch-data-table td {
  padding: 0.625rem 0.875rem;
  border-bottom: 1px solid #f1f5f9;
  color: #1e293b;
  vertical-align: middle;
}

.price-highlight {
  color: #047857;
  font-weight: 700;
}

.tbl-summary-footer {
  padding: 0.75rem 1rem;
  background: #f8fafc;
  border-top: 1.5px solid #cbd5e1;
  color: #334155;
  font-size: 0.8125rem;
  font-weight: 600;
}

.tbl-sum-total {
  color: #047857;
  margin-left: 0.5rem;
}
</style>
