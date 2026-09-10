<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useTicketStore } from '@/stores/ticketStore'
import { useFormStore } from '@/stores/formStore'
import { workflowApi } from '@/services/workflowApi'
import type { FormItem, FormField } from '@/types/form'
import Modal from '@/components/common/Modal.vue'
import EmployeeListField from '@/components/form/EmployeeListField.vue'

const router = useRouter()
const ticketStore = useTicketStore()
const formStore = useFormStore()

const formData = ref<Record<string, any>>({})
const errors = ref<Record<string, string>>({})
const targetForm = ref<FormItem | null>(null)
const isLoadingGraph = ref(false)

// Khi mở modal, xác định form được gán với Start Node của workflow
watch(
  () => ticketStore.isCreateModalOpen,
  async (isOpen) => {
    if (isOpen && ticketStore.createTargetWorkflow) {
      formData.value = {}
      errors.value = {}
      targetForm.value = null

      const wfId = ticketStore.createTargetWorkflow.id
      isLoadingGraph.value = true
      try {
        // Đảm bảo danh sách forms đã nạp
        if (formStore.forms.length === 0) {
          await formStore.fetchForms()
        }

        const res = await workflowApi.getWorkflowGraph(wfId)
        if (res.success && res.data) {
          const startNode = res.data.nodes?.find((n) => n.type === 'start')
          let boundFormId: number | null = null

          if (startNode?.formId) {
            boundFormId = Number(startNode.formId)
          } else if (startNode?.formBinding?.formId) {
            boundFormId = Number(startNode.formBinding.formId)
          }

          if (boundFormId) {
            targetForm.value = formStore.getFormById(boundFormId) ?? null
          }
        }

        // Fallback: nếu workflow chưa gắn form cụ thể, lấy form đầu tiên hoặc form mặc định
        if (!targetForm.value) {
          if (formStore.forms.length > 0 && formStore.forms[0]) {
            targetForm.value = formStore.forms[0]
          } else {
            // Biểu mẫu mẫu dự phòng
            targetForm.value = {
              id: 999,
              name: 'Biểu mẫu Đề xuất Yêu cầu Chuẩn',
              description: 'Biểu mẫu tiêu chuẩn tiếp nhận thông tin yêu cầu và phê duyệt.',
              createdAt: new Date().toISOString(),
              usageCount: 1,
              schema: {
                fields: [
                  {
                    id: 'f_title',
                    key: 'title',
                    label: 'Tiêu đề yêu cầu',
                    type: 'text',
                    placeholder: 'Nhập tiêu đề tóm tắt yêu cầu...',
                    required: true,
                  },
                  {
                    id: 'f_amount',
                    key: 'amount',
                    label: 'Số tiền / Dự toán (VNĐ)',
                    type: 'number',
                    placeholder: 'Ví dụ: 15000000',
                    required: false,
                    helpText: 'Dùng để xét duyệt phân cấp theo định mức tài chính',
                  },
                  {
                    id: 'f_dept',
                    key: 'department',
                    label: 'Phòng ban đề xuất',
                    type: 'select',
                    required: true,
                    options: [
                      { label: 'Phòng Công Nghệ Thông Tin (IT)', value: 'IT' },
                      { label: 'Phòng Kế Toán - Tài Chính', value: 'Finance' },
                      { label: 'Phòng Hành Chính - Nhân Sự (HR)', value: 'HR' },
                      { label: 'Phòng Mua Sắm (Procurement)', value: 'Procurement' },
                    ],
                  },
                  {
                    id: 'f_desc',
                    key: 'description',
                    label: 'Nội dung chi tiết yêu cầu',
                    type: 'textarea',
                    placeholder: 'Mô tả cụ thể mục đích, lý do, kết quả mong đợi...',
                    required: true,
                  },
                ],
              },
            }
          }
        }

        // Khởi tạo giá trị mặc định cho form
        if (targetForm.value?.schema?.fields) {
          targetForm.value.schema.fields.forEach((field) => {
            if (field.defaultValue !== undefined) {
              formData.value[field.key] = field.defaultValue
            } else if (field.type === 'number') {
              formData.value[field.key] = 0
            } else if (field.type === 'select' && field.options && field.options.length > 0 && field.options[0]) {
              formData.value[field.key] = field.options[0].value
            } else if (field.type === 'table' || field.type === 'list') {
              formData.value[field.key] = []
            } else {
              formData.value[field.key] = ''
            }
          })
        }
      } catch (err) {
        console.error('Lỗi khi tải thông tin form cho workflow:', err)
      } finally {
        isLoadingGraph.value = false
      }
    }
  }
)

const fields = computed<FormField[]>(() => {
  return targetForm.value?.schema?.fields || []
})

const hasTableField = computed(() => {
  return fields.value.some((f) => f.type === 'table' || f.type === 'list')
})

const getTableRows = (key: string): any[] => {
  const val = formData.value[key]
  return Array.isArray(val) ? val : []
}

const addTableRow = (field: FormField) => {
  const current = getTableRows(field.key)
  const newRow: Record<string, any> = {}
  if (field.columns && field.columns.length > 0) {
    field.columns.forEach((col) => {
      newRow[col.key] = col.type === 'number' ? 0 : ''
    })
  }
  const updated = [...current, newRow]
  formData.value[field.key] = updated

  if (field.summaryFieldKey) {
    const sum = updated.reduce((acc, r) => acc + (Number(r[field.summaryFieldKey!]) || 0), 0)
    formData.value['total_cost'] = sum
    formData.value['estimated_cost'] = sum
    formData.value['amount'] = sum
  }
}

const removeTableRow = (field: FormField, index: number) => {
  const current = getTableRows(field.key)
  const updated = current.filter((_, idx) => idx !== index)
  formData.value[field.key] = updated

  if (field.summaryFieldKey) {
    const sum = updated.reduce((acc, r) => acc + (Number(r[field.summaryFieldKey!]) || 0), 0)
    formData.value['total_cost'] = sum
    formData.value['estimated_cost'] = sum
    formData.value['amount'] = sum
  }
}

const updateTableCell = (field: FormField, rowIndex: number, colKey: string, val: any) => {
  const current = [...getTableRows(field.key)]
  current[rowIndex] = {
    ...current[rowIndex],
    [colKey]: val,
  }
  formData.value[field.key] = current

  if (field.summaryFieldKey && colKey === field.summaryFieldKey) {
    const sum = current.reduce((acc, r) => acc + (Number(r[field.summaryFieldKey!]) || 0), 0)
    formData.value['total_cost'] = sum
    formData.value['estimated_cost'] = sum
    formData.value['amount'] = sum
  }
}

const calculateTableTotal = (field: FormField): number => {
  const rows = getTableRows(field.key)
  const sumKey = field.summaryFieldKey || 'cost'
  return rows.reduce((acc, r) => acc + (Number(r[sumKey]) || 0), 0)
}

const validate = () => {
  errors.value = {}
  let isValid = true

  fields.value.forEach((f) => {
    const val = formData.value[f.key]
    if (f.required) {
      if (f.type === 'table' || f.type === 'list') {
        if (!Array.isArray(val) || val.length === 0) {
          errors.value[f.key] = `Vui lòng thêm ít nhất 1 nhân sự vào ${f.label.toLowerCase()}`
          isValid = false
        }
      } else if (val === undefined || val === null || String(val).trim() === '') {
        errors.value[f.key] = `${f.label} không được để trống`
        isValid = false
      }
    }
  })

  return isValid
}

const handleSubmit = async () => {
  if (!validate()) return
  if (!ticketStore.createTargetWorkflow) return

  try {
    const titleVal = formData.value['title'] || formData.value['request_title'] || ''
    const createdTicket = await ticketStore.createTicket(
      ticketStore.createTargetWorkflow.id,
      formData.value,
      titleVal
    )

    if (createdTicket && createdTicket.id) {
      router.push(`/tickets/${createdTicket.id}`)
    }
  } catch (e) {
    // Đã có toast thông báo lỗi trong store
  }
}
</script>

<template>
  <Modal
    :is-open="ticketStore.isCreateModalOpen"
    :title="`Khởi tạo Yêu cầu: ${ticketStore.createTargetWorkflow?.name || ''}`"
    :max-width="hasTableField ? '800px' : '650px'"
    @close="ticketStore.closeCreateModal"
  >
    <div class="modal-body-content">
      <!-- Loading State -->
      <div v-if="isLoadingGraph" class="loading-state">
        <div class="spinner"></div>
        <p>Đang tải biểu mẫu quy trình...</p>
      </div>

      <!-- Form Content -->
      <div v-else class="form-container">
        <!-- Form Header Info -->
        <div class="form-meta-box">
          <div class="meta-icon">📋</div>
          <div class="meta-text">
            <h4 class="form-title">{{ targetForm?.name || 'Biểu mẫu Đề xuất' }}</h4>
            <p class="form-desc">{{ targetForm?.description || 'Vui lòng điền đầy đủ các thông tin cần thiết bên dưới để khởi tạo yêu cầu.' }}</p>
          </div>
        </div>

        <form @submit.prevent="handleSubmit" class="dynamic-form">
          <div v-for="field in fields" :key="field.id || field.key" class="form-group">
            <label class="form-label">
              <span>{{ field.label }}</span>
              <span v-if="field.required" class="required-star">*</span>
            </label>

            <!-- Text Input -->
            <input
              v-if="field.type === 'text'"
              v-model="formData[field.key]"
              type="text"
              class="form-control"
              :class="{ 'is-invalid': errors[field.key] }"
              :placeholder="field.placeholder || `Nhập ${field.label.toLowerCase()}...`"
            />

            <!-- Number Input -->
            <input
              v-else-if="field.type === 'number'"
              v-model.number="formData[field.key]"
              type="number"
              class="form-control"
              :class="{ 'is-invalid': errors[field.key] }"
              :placeholder="field.placeholder || '0'"
            />

            <!-- Textarea -->
            <textarea
              v-else-if="field.type === 'textarea'"
              v-model="formData[field.key]"
              rows="3"
              class="form-control textarea"
              :class="{ 'is-invalid': errors[field.key] }"
              :placeholder="field.placeholder || `Mô tả chi tiết...`"
            ></textarea>

            <!-- Select Dropdown -->
            <select
              v-else-if="field.type === 'select'"
              v-model="formData[field.key]"
              class="form-control select"
              :class="{ 'is-invalid': errors[field.key] }"
            >
              <option
                v-for="opt in field.options"
                :key="opt.value"
                :value="opt.value"
              >
                {{ opt.label }}
              </option>
            </select>

            <!-- Employee List / Dynamic List Input -->
            <EmployeeListField
              v-else-if="field.type === 'list' || field.type === 'table'"
              v-model="formData[field.key]"
              :placeholder="field.placeholder || 'Nhập email hoặc account nhân viên để tìm kiếm...'"
              :required="field.required"
              :disabled="false"
            />

            <!-- Date Input -->
            <input
              v-else-if="field.type === 'date'"
              v-model="formData[field.key]"
              type="date"
              class="form-control"
              :class="{ 'is-invalid': errors[field.key] }"
            />

            <!-- Fallback Text -->
            <input
              v-else
              v-model="formData[field.key]"
              type="text"
              class="form-control"
              :class="{ 'is-invalid': errors[field.key] }"
            />

            <span v-if="errors[field.key]" class="error-text">
              {{ errors[field.key] }}
            </span>
            <span v-else-if="field.helpText" class="help-text">
              {{ field.helpText }}
            </span>
          </div>
        </form>
      </div>
    </div>

    <template #footer>
      <button
        type="button"
        class="btn-cancel"
        :disabled="ticketStore.isSubmittingTicket"
        @click="ticketStore.closeCreateModal"
      >
        Hủy bỏ
      </button>
      <button
        type="button"
        class="btn-submit"
        :disabled="ticketStore.isSubmittingTicket || isLoadingGraph"
        @click="handleSubmit"
      >
        <span v-if="ticketStore.isSubmittingTicket" class="spinner-mini"></span>
        <span v-else>🚀</span>
        <span>{{ ticketStore.isSubmittingTicket ? 'Đang khởi tạo...' : 'Gửi Yêu Cầu (Chạy Ticket)' }}</span>
      </button>
    </template>
  </Modal>
</template>

<style scoped>
.modal-body-content {
  padding: 0.5rem 0;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  gap: 1rem;
  color: #64748b;
}

.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #e2e8f0;
  border-top-color: #0284c7;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.form-meta-box {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 1rem;
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 10px;
  margin-bottom: 1.5rem;
}

.meta-icon {
  font-size: 1.75rem;
}

.meta-text {
  flex: 1;
}

.form-title {
  font-size: 1rem;
  font-weight: 700;
  color: #0369a1;
  margin-bottom: 0.25rem;
}

.form-desc {
  font-size: 0.8125rem;
  color: #0284c7;
  line-height: 1.4;
}

.dynamic-form {
  display: flex;
  flex-direction: column;
  gap: 1.125rem;
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 0.25rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.form-label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #334155;
  display: flex;
  align-items: center;
  gap: 4px;
}

.required-star {
  color: #ef4444;
}

.form-control {
  width: 100%;
  height: 40px;
  padding: 0 0.875rem;
  border: 1.5px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.875rem;
  color: #0f172a;
  background: #ffffff;
  transition: all 0.2s ease;
}

.form-control:focus {
  outline: none;
  border-color: #0284c7;
  box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.15);
}

.form-control.textarea {
  height: auto;
  padding: 0.625rem 0.875rem;
  resize: vertical;
}

.form-control.is-invalid {
  border-color: #ef4444;
  background: #fff5f5;
}

.error-text {
  font-size: 0.75rem;
  color: #dc2626;
  font-weight: 500;
}

.help-text {
  font-size: 0.75rem;
  color: #64748b;
}

.btn-cancel {
  padding: 0.625rem 1.125rem;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-cancel:hover:not(:disabled) {
  background: #f1f5f9;
  color: #0f172a;
}

.btn-submit {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.25rem;
  background: linear-gradient(135deg, #0284c7 0%, #0369a1 100%);
  color: #ffffff;
  border: none;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(2, 132, 199, 0.35);
  transition: all 0.2s ease;
}

.btn-submit:hover:not(:disabled) {
  background: linear-gradient(135deg, #0369a1 0%, #075985 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(2, 132, 199, 0.45);
}

.btn-submit:disabled,
.btn-cancel:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.spinner-mini {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Dynamic Table Styles */
.table-input-container {
  border: 1.5px solid #cbd5e1;
  border-radius: 10px;
  overflow: hidden;
  background: #ffffff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.table-responsive {
  overflow-x: auto;
}

.interactive-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.8125rem;
}

.interactive-table th {
  background: #f1f5f9;
  color: #334155;
  font-weight: 700;
  padding: 0.625rem 0.75rem;
  text-align: left;
  border-bottom: 1.5px solid #cbd5e1;
  white-space: nowrap;
}

.interactive-table td {
  padding: 0.5rem 0.625rem;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
}

.col-index {
  text-align: center;
  color: #94a3b8;
  font-weight: 600;
  font-size: 0.75rem;
}

.col-action {
  text-align: center;
}

.table-cell-input {
  width: 100%;
  padding: 0.45rem 0.625rem;
  font-size: 0.8125rem;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  color: #0f172a;
  background: #ffffff;
  outline: none;
  transition: all 0.2s ease;
}

.table-cell-input:focus {
  border-color: #0284c7;
  box-shadow: 0 0 0 2px rgba(2, 132, 199, 0.15);
}

.text-right {
  text-align: right;
}

.btn-delete-row {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 6px;
  border: 1px solid #fecaca;
  background: #ffffff;
  color: #ef4444;
  cursor: pointer;
  font-size: 0.75rem;
  transition: all 0.2s ease;
}

.btn-delete-row:hover {
  background: #fee2e2;
  border-color: #dc2626;
}

.empty-table-prompt {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 1.5rem 1rem;
  color: #64748b;
  font-size: 0.8125rem;
  background: #f8fafc;
}

.prompt-icon {
  font-size: 1.125rem;
}

.table-footer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.625rem 0.875rem;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.btn-add-row {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.45rem 0.875rem;
  font-size: 0.8125rem;
  font-weight: 600;
  color: #0284c7;
  background: #ffffff;
  border: 1.5px dashed #38bdf8;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-add-row:hover {
  background: #e0f2fe;
  border-color: #0284c7;
}

.table-summary-badges {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.badge-count {
  font-size: 0.75rem;
  color: #475569;
}

.badge-total {
  font-size: 0.8125rem;
  color: #047857;
  background: #d1fae5;
  padding: 0.2rem 0.625rem;
  border-radius: 9999px;
  font-weight: 500;
}
</style>
