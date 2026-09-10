<script setup lang="ts">
import { computed } from 'vue'
import EmployeeListField from './EmployeeListField.vue'
import type { FormSchema, FieldPermission, FormField } from '@/types/form'

const props = withDefaults(
  defineProps<{
    schema: FormSchema
    modelValue: Record<string, any>
    permissions?: Record<string, FieldPermission>
    disabled?: boolean
  }>(),
  {
    permissions: () => ({}),
    disabled: false,
  }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: Record<string, any>): void
}>()

const getPermission = (key: string): FieldPermission => {
  return props.permissions[key] || 'editable'
}

const isVisible = (key: string): boolean => {
  return getPermission(key) !== 'hidden'
}

const isReadOnly = (key: string): boolean => {
  return props.disabled || getPermission(key) === 'readonly'
}

const updateField = (key: string, value: any) => {
  if (isReadOnly(key)) return
  emit('update:modelValue', {
    ...props.modelValue,
    [key]: value,
  })
}

const handleFileUpload = (key: string, event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files && target.files.length > 0) {
    const file = target.files[0]
    if (file) {
      updateField(key, file.name)
    }
  }
}

// ==========================================================
// DYNAMIC TABLE / REPEATER HELPERS
// ==========================================================
const getTableRows = (key: string): any[] => {
  const val = props.modelValue[key]
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

  const payload: Record<string, any> = {
    ...props.modelValue,
    [field.key]: updated,
  }

  if (field.summaryFieldKey) {
    const sum = updated.reduce((acc, r) => acc + (Number(r[field.summaryFieldKey!]) || 0), 0)
    payload['total_cost'] = sum
    payload['estimated_cost'] = sum
    payload['amount'] = sum
  }

  emit('update:modelValue', payload)
}

const removeTableRow = (field: FormField, index: number) => {
  const current = getTableRows(field.key)
  const updated = current.filter((_, idx) => idx !== index)

  const payload: Record<string, any> = {
    ...props.modelValue,
    [field.key]: updated,
  }

  if (field.summaryFieldKey) {
    const sum = updated.reduce((acc, r) => acc + (Number(r[field.summaryFieldKey!]) || 0), 0)
    payload['total_cost'] = sum
    payload['estimated_cost'] = sum
    payload['amount'] = sum
  }

  emit('update:modelValue', payload)
}

const updateTableCell = (field: FormField, rowIndex: number, colKey: string, val: any) => {
  const current = [...getTableRows(field.key)]
  current[rowIndex] = {
    ...current[rowIndex],
    [colKey]: val,
  }

  const payload: Record<string, any> = {
    ...props.modelValue,
    [field.key]: current,
  }

  if (field.summaryFieldKey) {
    const sum = current.reduce((acc, r) => acc + (Number(r[field.summaryFieldKey!]) || 0), 0)
    payload['total_cost'] = sum
    payload['estimated_cost'] = sum
    payload['amount'] = sum
  }

  emit('update:modelValue', payload)
}

const calculateTableTotal = (field: FormField): number => {
  const rows = getTableRows(field.key)
  const sumKey = field.summaryFieldKey || 'cost'
  return rows.reduce((acc, r) => acc + (Number(r[sumKey]) || 0), 0)
}
</script>

<template>
  <div class="dynamic-form-renderer">
    <div
      v-for="field in schema.fields"
      :key="field.id"
      v-show="isVisible(field.key)"
      class="form-field-wrapper"
      :class="{ 'is-readonly': isReadOnly(field.key), 'is-table-field': field.type === 'table' || field.type === 'list' }"
    >
      <div class="field-label-row">
        <label class="field-label">
          {{ field.label }}
          <span v-if="field.required && !isReadOnly(field.key)" class="required-star">*</span>
        </label>
        <span v-if="isReadOnly(field.key)" class="badge-readonly">Chỉ xem</span>
      </div>

      <!-- READ-ONLY VALUE DISPLAY FOR TABLE -->
      <div v-if="isReadOnly(field.key) && field.type === 'table'" class="readonly-table-box">
        <div v-if="getTableRows(field.key).length > 0" class="table-responsive">
          <table class="data-table">
            <thead>
              <tr>
                <th style="width: 45px">#</th>
                <th v-for="col in field.columns || []" :key="col.key">
                  {{ col.label }}
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, rIdx) in getTableRows(field.key)" :key="rIdx">
                <td class="col-index">{{ rIdx + 1 }}</td>
                <td v-for="col in field.columns || []" :key="col.key">
                  <template v-if="col.type === 'number'">
                    {{ Number(row[col.key] || 0).toLocaleString('vi-VN') }} đ
                  </template>
                  <template v-else>
                    {{ row[col.key] || '-' }}
                  </template>
                </td>
              </tr>
            </tbody>
            <tfoot>
              <tr>
                <td :colspan="(field.columns?.length || 1) + 1" class="table-summary-cell">
                  <span>Tổng số: <strong>{{ getTableRows(field.key).length }}</strong> nhân sự</span>
                  <span v-if="field.summaryFieldKey" class="summary-total-price">
                    • Tổng dự toán: <strong>{{ calculateTableTotal(field).toLocaleString('vi-VN') }} VNĐ</strong>
                  </span>
                </td>
              </tr>
            </tfoot>
          </table>
        </div>
        <div v-else class="readonly-empty">(Danh sách trống)</div>
      </div>

      <!-- READ-ONLY VALUE DISPLAY FOR OTHER SCALAR TYPES -->
      <div v-else-if="isReadOnly(field.key) && field.type !== 'list'" class="readonly-display-box">
        <span v-if="modelValue[field.key] !== undefined && modelValue[field.key] !== ''" class="readonly-value">
          <template v-if="field.type === 'select'">
            {{ field.options?.find(opt => opt.value === modelValue[field.key])?.label || modelValue[field.key] }}
          </template>
          <template v-else-if="field.type === 'number'">
            {{ Number(modelValue[field.key]).toLocaleString('vi-VN') }}
          </template>
          <template v-else>
            {{ modelValue[field.key] }}
          </template>
        </span>
        <span v-else class="readonly-empty">(Chưa có dữ liệu)</span>
      </div>

      <!-- EDITABLE INPUTS -->
      <div v-else class="field-control-container">
        <!-- 1. TEXT INPUT -->
        <input
          v-if="field.type === 'text'"
          type="text"
          class="custom-input"
          :placeholder="field.placeholder || 'Nhập ' + field.label.toLowerCase() + '...'"
          :value="modelValue[field.key] ?? field.defaultValue ?? ''"
          @input="updateField(field.key, ($event.target as HTMLInputElement).value)"
        />

        <!-- 2. NUMBER INPUT -->
        <input
          v-else-if="field.type === 'number'"
          type="number"
          class="custom-input"
          :placeholder="field.placeholder || '0'"
          :min="field.min"
          :max="field.max"
          :value="modelValue[field.key] ?? field.defaultValue ?? ''"
          @input="updateField(field.key, Number(($event.target as HTMLInputElement).value))"
        />

        <!-- 3. TEXTAREA -->
        <textarea
          v-else-if="field.type === 'textarea'"
          rows="3"
          class="custom-input custom-textarea"
          :placeholder="field.placeholder || 'Nhập chi tiết ' + field.label.toLowerCase() + '...'"
          :value="modelValue[field.key] ?? field.defaultValue ?? ''"
          @input="updateField(field.key, ($event.target as HTMLTextAreaElement).value)"
        ></textarea>

        <!-- 4. SELECT -->
        <select
          v-else-if="field.type === 'select'"
          class="custom-input custom-select"
          :value="modelValue[field.key] ?? field.defaultValue ?? ''"
          @change="updateField(field.key, ($event.target as HTMLSelectElement).value)"
        >
          <option value="" disabled>-- Vui lòng chọn --</option>
          <option
            v-for="opt in field.options"
            :key="opt.value"
            :value="opt.value"
          >
            {{ opt.label }}
          </option>
        </select>

        <!-- 5. DATE -->
        <input
          v-else-if="field.type === 'date'"
          type="date"
          class="custom-input"
          :value="modelValue[field.key] ?? field.defaultValue ?? ''"
          @input="updateField(field.key, ($event.target as HTMLInputElement).value)"
        />

        <!-- 6. FILE UPLOAD -->
        <div v-else-if="field.type === 'file'" class="file-uploader-box">
          <label class="file-upload-btn">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
              <polyline points="17 8 12 3 7 8"></polyline>
              <line x1="12" y1="3" x2="12" y2="15"></line>
            </svg>
            <span>Tải lên tệp đính kèm</span>
            <input
              type="file"
              class="hidden-file-input"
              @change="handleFileUpload(field.key, $event)"
            />
          </label>
          <span v-if="modelValue[field.key]" class="uploaded-filename">
            📎 {{ modelValue[field.key] }}
          </span>
          <span v-else class="upload-hint">Chưa chọn tệp nào</span>
        </div>

        <!-- 7. CHECKBOX -->
        <label v-else-if="field.type === 'checkbox'" class="checkbox-container">
          <input
            type="checkbox"
            :checked="!!modelValue[field.key]"
            @change="updateField(field.key, ($event.target as HTMLInputElement).checked)"
          />
          <span class="checkmark"></span>
          <span class="checkbox-text">{{ field.placeholder || 'Đồng ý với điều khoản này' }}</span>
        </label>

        <!-- 8. LIST: EMPLOYEE LIST SEARCH & SELECT -->
        <EmployeeListField
          v-else-if="field.type === 'list'"
          :model-value="modelValue[field.key] || []"
          :disabled="isReadOnly(field.key)"
          :placeholder="field.placeholder"
          :required="field.required"
          @update:model-value="updateField(field.key, $event)"
        />

        <!-- 9. TABLE / DYNAMIC ROWS INPUT (FALLBACK) -->
        <div v-else-if="field.type === 'table'" class="table-input-container">
          <div v-if="getTableRows(field.key).length > 0" class="table-responsive">
            <table class="interactive-table">
              <thead>
                <tr>
                  <th style="width: 36px">#</th>
                  <th v-for="col in field.columns || []" :key="col.key" :style="{ width: col.width }">
                    {{ col.label }}
                    <span v-if="col.required" class="required-star">*</span>
                  </th>
                  <th style="width: 44px"></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, rIdx) in getTableRows(field.key)" :key="rIdx">
                  <td class="col-index">{{ rIdx + 1 }}</td>
                  <td v-for="col in field.columns || []" :key="col.key">
                    <!-- Text cell -->
                    <input
                      v-if="col.type === 'text'"
                      type="text"
                      class="table-cell-input"
                      :placeholder="col.placeholder || col.label"
                      :value="row[col.key] ?? ''"
                      @input="updateTableCell(field, rIdx, col.key, ($event.target as HTMLInputElement).value)"
                    />
                    <!-- Number cell -->
                    <input
                      v-else-if="col.type === 'number'"
                      type="number"
                      class="table-cell-input text-right"
                      :placeholder="col.placeholder || '0'"
                      :value="row[col.key] ?? 0"
                      @input="updateTableCell(field, rIdx, col.key, Number(($event.target as HTMLInputElement).value))"
                    />
                    <!-- Select cell -->
                    <select
                      v-else-if="col.type === 'select'"
                      class="table-cell-input custom-select"
                      :value="row[col.key] ?? ''"
                      @change="updateTableCell(field, rIdx, col.key, ($event.target as HTMLSelectElement).value)"
                    >
                      <option value="" disabled>-- Chọn --</option>
                      <option v-for="opt in col.options || []" :key="opt.value" :value="opt.value">
                        {{ opt.label }}
                      </option>
                    </select>
                  </td>
                  <td class="col-action">
                    <button
                      type="button"
                      class="btn-delete-row"
                      title="Xóa dòng này"
                      @click="removeTableRow(field, rIdx)"
                    >
                      ✕
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-else class="empty-table-prompt">
            <span class="prompt-icon">👥</span>
            <span>Chưa có nhân sự nào trong danh sách. Bấm nút bên dưới để thêm nhân sự.</span>
          </div>

          <div class="table-footer-toolbar">
            <button
              type="button"
              class="btn-add-row"
              @click="addTableRow(field)"
            >
              <span>➕</span>
              <span>{{ field.addBtnText || '+ Thêm nhân sự' }}</span>
            </button>

            <div v-if="getTableRows(field.key).length > 0" class="table-summary-badges">
              <span class="badge-count">
                Số lượng: <strong>{{ getTableRows(field.key).length }}</strong> nhân sự
              </span>
              <span v-if="field.summaryFieldKey" class="badge-total">
                Tổng cộng: <strong>{{ calculateTableTotal(field).toLocaleString('vi-VN') }} VNĐ</strong>
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- HELP TEXT -->
      <p v-if="field.helpText && !isReadOnly(field.key)" class="field-help-text">
        ℹ️ {{ field.helpText }}
      </p>
    </div>
  </div>
</template>

<style scoped>
.dynamic-form-renderer {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.form-field-wrapper {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.form-field-wrapper.is-table-field {
  margin-top: 0.5rem;
  margin-bottom: 0.5rem;
}

.field-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.field-label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #334155;
}

.required-star {
  color: #ef4444;
  margin-left: 2px;
}

.badge-readonly {
  font-size: 0.6875rem;
  font-weight: 600;
  color: #64748b;
  background: #f1f5f9;
  padding: 1px 6px;
  border-radius: 4px;
}

.readonly-display-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.625rem 0.875rem;
}

.readonly-value {
  font-size: 0.875rem;
  color: #0f172a;
  font-weight: 500;
}

.readonly-empty {
  font-size: 0.8125rem;
  color: #94a3b8;
  font-style: italic;
}

.field-control-container {
  display: flex;
  flex-direction: column;
}

.custom-input {
  width: 100%;
  padding: 0.625rem 0.875rem;
  font-size: 0.875rem;
  color: #1e293b;
  background: #ffffff;
  border: 1.5px solid #e2e8f0;
  border-radius: 8px;
  outline: none;
  transition: all 0.2s ease;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.custom-input:focus {
  border-color: #0284c7;
  box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.15);
}

.custom-textarea {
  resize: vertical;
  min-height: 80px;
}

.custom-select {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%2364748b' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.75rem center;
  background-size: 1rem;
  padding-right: 2.25rem;
}

.field-help-text {
  font-size: 0.75rem;
  color: #64748b;
  margin: 0;
  line-height: 1.4;
}

/* File Upload */
.file-uploader-box {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.file-upload-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.875rem;
  font-size: 0.8125rem;
  font-weight: 600;
  color: #0284c7;
  background: #e0f2fe;
  border: 1px solid #bae6fd;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.file-upload-btn:hover {
  background: #bae6fd;
}

.hidden-file-input {
  display: none;
}

.uploaded-filename {
  font-size: 0.8125rem;
  font-weight: 500;
  color: #059669;
  background: #ecfdf5;
  padding: 0.35rem 0.625rem;
  border-radius: 6px;
  border: 1px solid #a7f3d0;
}

.upload-hint {
  font-size: 0.75rem;
  color: #94a3b8;
}

/* Checkbox */
.checkbox-container {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  font-size: 0.84rem;
  color: #334155;
  user-select: none;
}

/* ==========================================================
   DYNAMIC TABLE STYLES
   ========================================================== */
.table-input-container {
  border: 1.5px solid #cbd5e1;
  border-radius: 10px;
  overflow: hidden;
  background: #ffffff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.readonly-table-box {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  overflow: hidden;
  background: #ffffff;
}

.table-responsive {
  overflow-x: auto;
}

.interactive-table,
.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.8125rem;
}

.interactive-table th,
.data-table th {
  background: #f1f5f9;
  color: #334155;
  font-weight: 700;
  padding: 0.625rem 0.75rem;
  text-align: left;
  border-bottom: 1.5px solid #cbd5e1;
  white-space: nowrap;
}

.interactive-table td,
.data-table td {
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

.table-summary-cell {
  background: #f8fafc;
  font-weight: 600;
  padding: 0.625rem 0.75rem;
  color: #334155;
}

.summary-total-price {
  color: #047857;
  margin-left: 0.5rem;
}
</style>
