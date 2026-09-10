<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useFormStore } from '@/stores/formStore'
import FormRenderer from './FormRenderer.vue'

const formStore = useFormStore()

const isSubmitted = ref(false)
const ticketData = ref<Record<string, any>>({})
const generatedTicketCode = ref('')
const submissionTime = ref('')

watch(
  () => formStore.isSimulationModalOpen,
  (open) => {
    if (open) {
      isSubmitted.value = false
      ticketData.value = {}
      generatedTicketCode.value = `TK-${new Date().getFullYear()}${String(new Date().getMonth() + 1).padStart(2, '0')}-${Math.floor(1000 + Math.random() * 9000)}`
      submissionTime.value = ''
    }
  }
)

const form = computed(() => formStore.simulationForm)

const handleSubmit = () => {
  if (!form.value) return
  // Simple check for required fields
  for (const field of form.value.schema.fields) {
    if (field.required && (ticketData.value[field.key] === undefined || ticketData.value[field.key] === '')) {
      alert(`Vui lòng điền trường bắt buộc: "${field.label}"`)
      return
    }
  }

  submissionTime.value = new Date().toLocaleString('vi-VN')
  isSubmitted.value = true
}

const handleReset = () => {
  ticketData.value = {}
  isSubmitted.value = false
  generatedTicketCode.value = `TK-${new Date().getFullYear()}${String(new Date().getMonth() + 1).padStart(2, '0')}-${Math.floor(1000 + Math.random() * 9000)}`
}
</script>

<template>
  <div v-if="formStore.isSimulationModalOpen && form" class="modal-overlay">
    <div class="modal-container simulation-modal">
      <!-- HEADER -->
      <div class="modal-header">
        <div class="header-meta">
          <span class="badge-sim">Mô Phỏng Trải Nghiệm Người Dùng (End-User)</span>
          <h2 class="modal-title">
            {{ isSubmitted ? 'Ticket Đã Được Khởi Tạo Thành Công' : (form.name || 'Tạo Yêu Cầu Mới') }}
          </h2>
          <span v-if="!isSubmitted" class="ticket-ref-tag">Mã dự kiến: {{ generatedTicketCode }}</span>
        </div>
        <button type="button" class="btn-close" @click="formStore.closeSimulationModal">✕</button>
      </div>

      <!-- BODY -->
      <div class="modal-body">
        <!-- FORM FILLING VIEW (BEFORE SUBMISSION) -->
        <div v-if="!isSubmitted" class="form-content-wrap">
          <div class="user-instruction-box">
            <div class="instruction-icon">📋</div>
            <div class="instruction-text">
              <strong>Khởi tạo tiến trình Workflow từ Form:</strong><br />
              Vui lòng hoàn thành các trường thông tin bên dưới để gửi yêu cầu xử lý. Dữ liệu sau khi gửi sẽ kích hoạt các bước phê duyệt tiếp theo trong quy trình.
            </div>
          </div>

          <div class="form-card">
            <FormRenderer
              :schema="form.schema"
              :model-value="ticketData"
              :permissions="formStore.simulationPermissions"
              @update:model-value="ticketData = $event"
            />
          </div>
        </div>

        <!-- SUCCESS VIEW (AFTER SUBMISSION) -->
        <div v-else class="submission-success-wrap">
          <div class="success-icon-banner">
            <div class="success-circle">✓</div>
            <h3 class="success-title">Yêu cầu đã được tiếp nhận!</h3>
            <p class="success-desc">
              Một tiến trình <strong>Workflow Instance</strong> mới đã bắt đầu chạy với dữ liệu biểu mẫu bạn vừa gửi.
            </p>
          </div>

          <div class="ticket-summary-card">
            <div class="summary-header">
              <span class="sum-label">MÃ TICKET TIẾP NHẬN:</span>
              <span class="sum-code">{{ generatedTicketCode }}</span>
            </div>
            
            <div class="summary-grid">
              <div class="sum-item">
                <span class="item-title">Thời gian tạo:</span>
                <span class="item-val">{{ submissionTime }}</span>
              </div>
              <div class="sum-item">
                <span class="item-title">Trạng thái luồng:</span>
                <span class="item-val status-running">● Đang chạy (Running)</span>
              </div>
              <div class="sum-item">
                <span class="item-title">Bước hiện tại:</span>
                <span class="item-val">Bước 2: Phê duyệt (Approval Node)</span>
              </div>
              <div class="sum-item">
                <span class="item-title">Người đang xử lý:</span>
                <span class="item-val">Trưởng bộ phận liên quan</span>
              </div>
            </div>

            <!-- JSON VARIABLES DISPLAY -->
            <div class="payload-box">
              <div class="payload-title">
                <span>Dữ liệu biến tích lũy vào hệ thống (<code>workflow_instances.variables</code>):</span>
              </div>
              <pre class="json-code">{{ JSON.stringify(ticketData, null, 2) }}</pre>
            </div>
          </div>
        </div>
      </div>

      <!-- FOOTER -->
      <div class="modal-footer">
        <template v-if="!isSubmitted">
          <button type="button" class="btn-cancel" @click="formStore.closeSimulationModal">
            Hủy bỏ
          </button>
          <button type="button" class="btn-submit" @click="handleSubmit">
            🚀 Gửi yêu cầu & Tạo Ticket
          </button>
        </template>
        <template v-else>
          <button type="button" class="btn-secondary" @click="handleReset">
            + Tạo thử ticket khác
          </button>
          <button type="button" class="btn-primary" @click="formStore.closeSimulationModal">
            Hoàn tất & Đóng
          </button>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.65);
  backdrop-filter: blur(5px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1060;
  padding: 1.5rem;
}

.simulation-modal {
  background: #ffffff;
  width: 100%;
  max-width: 720px;
  max-height: 90vh;
  border-radius: 16px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  padding: 1.25rem 1.75rem;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  background: #f8fafc;
}

.badge-sim {
  font-size: 0.6875rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #059669;
  background: #ecfdf5;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  display: inline-block;
  margin-bottom: 0.35rem;
}

.modal-title {
  font-size: 1.25rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.ticket-ref-tag {
  font-size: 0.75rem;
  font-weight: 600;
  color: #64748b;
  font-family: monospace;
}

.btn-close {
  background: none;
  border: none;
  font-size: 1.25rem;
  color: #94a3b8;
  cursor: pointer;
  padding: 0.25rem 0.5rem;
  border-radius: 6px;
}

.btn-close:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 1.5rem 1.75rem;
  background: #f8fafc;
}

.user-instruction-box {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  padding: 0.875rem 1rem;
  border-radius: 8px;
  margin-bottom: 1.25rem;
}

.instruction-icon {
  font-size: 1.25rem;
}

.instruction-text {
  font-size: 0.8125rem;
  color: #1e40af;
  line-height: 1.4;
}

.form-card {
  background: #ffffff;
  padding: 1.5rem;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.03);
}

/* SUCCESS STYLES */
.submission-success-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.25rem;
}

.success-icon-banner {
  text-align: center;
}

.success-circle {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #10b981;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.75rem;
  font-weight: 800;
  margin: 0 auto 0.75rem auto;
  box-shadow: 0 10px 20px -5px rgba(16, 185, 129, 0.4);
}

.success-title {
  font-size: 1.35rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0 0 0.35rem 0;
}

.success-desc {
  font-size: 0.84rem;
  color: #64748b;
  margin: 0;
  max-width: 480px;
}

.ticket-summary-card {
  width: 100%;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 1.25rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}

.summary-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 0.875rem;
  border-bottom: 1px solid #f1f5f9;
  margin-bottom: 1rem;
}

.sum-label {
  font-size: 0.75rem;
  font-weight: 700;
  color: #64748b;
}

.sum-code {
  font-size: 1.15rem;
  font-weight: 800;
  color: var(--primary);
  font-family: monospace;
}

.summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.875rem;
  margin-bottom: 1.25rem;
}

.sum-item {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.item-title {
  font-size: 0.72rem;
  color: #64748b;
}

.item-val {
  font-size: 0.84rem;
  font-weight: 600;
  color: #1e293b;
}

.status-running {
  color: #2563eb;
}

.payload-box {
  background: #0f172a;
  border-radius: 8px;
  padding: 0.875rem 1rem;
  color: #f8fafc;
}

.payload-title {
  font-size: 0.75rem;
  color: #94a3b8;
  margin-bottom: 0.5rem;
}

.payload-title code {
  color: #7dd3fc;
}

.json-code {
  font-family: monospace;
  font-size: 0.75rem;
  margin: 0;
  color: #38bdf8;
  max-height: 140px;
  overflow-y: auto;
}

/* FOOTER */
.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1rem 1.75rem;
  background: #ffffff;
  border-top: 1px solid #e2e8f0;
}

.btn-cancel, .btn-secondary {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  padding: 0.6rem 1.25rem;
  border-radius: 8px;
  font-size: 0.84rem;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
}

.btn-submit, .btn-primary {
  background: #10b981;
  color: #ffffff;
  border: none;
  padding: 0.6rem 1.5rem;
  border-radius: 8px;
  font-size: 0.84rem;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(16, 185, 129, 0.35);
  transition: all 0.2s ease;
}

.btn-submit:hover, .btn-primary:hover {
  background: #059669;
  transform: translateY(-1px);
}
</style>
