<script setup lang="ts">
import { computed } from 'vue'
import type { WorkflowVariable, VariableMappingItem } from '@/types/mapping'
import type { FormField } from '@/types/form'

const props = defineProps<{
  variable: WorkflowVariable
  mappingItem: VariableMappingItem
  formFields: FormField[]
}>()

const emit = defineEmits<{
  (e: 'update:field', formFieldKey: string): void
  (e: 'update:fallback', fallbackVal: any): void
}>()

const selectedField = computed(() => {
  if (!props.mappingItem.formFieldKey) return null
  return props.formFields.find((f) => f.key === props.mappingItem.formFieldKey) || null
})

const handleFieldChange = (event: Event) => {
  const select = event.target as HTMLSelectElement
  emit('update:field', select.value)
}

const getDataTypeBadgeClass = (dt: string) => {
  switch (dt) {
    case 'NUMBER':
      return 'badge-number'
    case 'STRING':
      return 'badge-string'
    case 'BOOLEAN':
      return 'badge-boolean'
    case 'DATE':
      return 'badge-date'
    default:
      return 'badge-default'
  }
}

const getFormFieldTypeBadge = (type: string) => {
  switch (type) {
    case 'number':
      return { text: 'NUMBER', class: 'badge-number' }
    case 'text':
    case 'textarea':
      return { text: 'TEXT', class: 'badge-string' }
    case 'select':
      return { text: 'SELECT', class: 'badge-select' }
    case 'date':
      return { text: 'DATE', class: 'badge-date' }
    case 'checkbox':
      return { text: 'BOOL', class: 'badge-boolean' }
    case 'file':
      return { text: 'FILE', class: 'badge-file' }
    default:
      return { text: type.toUpperCase(), class: 'badge-default' }
  }
}
</script>

<template>
  <div
    class="mapping-row"
    :class="{
      'status-matched': mappingItem.status === 'matched',
      'status-mismatch': mappingItem.status === 'type_mismatch',
      'status-unmapped': mappingItem.status === 'unmapped',
    }"
  >
    <!-- LEFT: WORKFLOW VARIABLE (DATA CONTRACT) -->
    <div class="side-col workflow-var-side">
      <div class="var-main-info">
        <div class="var-title-row">
          <span class="var-key-tag">{{ variable.key }}</span>
          <span class="type-badge" :class="getDataTypeBadgeClass(variable.dataType)">
            {{ variable.dataType }}
          </span>
          <span v-if="variable.isRequired" class="required-pill" title="Bắt buộc phải map để Workflow có thể rẽ nhánh">
            ★ Bắt buộc
          </span>
          <span v-if="variable.isSystem" class="system-pill" title="Biến ngữ cảnh hệ thống">
            Hệ thống
          </span>
        </div>
        <div class="var-label">{{ variable.label }}</div>
        <div v-if="variable.description" class="var-desc">{{ variable.description }}</div>
      </div>

      <!-- Condition usage badges -->
      <div v-if="variable.usedInEdges && variable.usedInEdges.length > 0" class="condition-usage">
        <span class="usage-label">Dùng tại {{ variable.usedInEdges.length }} nhánh rẽ:</span>
        <div class="usage-pills">
          <span
            v-for="(edge, idx) in variable.usedInEdges"
            :key="idx"
            class="edge-pill"
            :title="`Điều kiện: ${variable.key} ${edge.operator} ${edge.compareValue}`"
          >
            🔀 {{ edge.edgeLabel || `${edge.fromNodeName} → ${edge.toNodeName}` }}
            <strong>({{ edge.operator }} {{ edge.compareValue }})</strong>
          </span>
        </div>
      </div>
    </div>

    <!-- MIDDLE: CONNECTION STATUS INDICATOR -->
    <div class="middle-connector">
      <div class="connector-line">
        <div class="connector-arrow">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <line x1="5" y1="12" x2="19" y2="12"></line>
            <polyline points="12 5 19 12 12 19"></polyline>
          </svg>
        </div>
      </div>

      <div class="status-indicator-badge">
        <span v-if="mappingItem.status === 'matched'" class="badge-status-ok" title="Đã kết nối tương thích hoàn hảo">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
            <polyline points="20 6 9 17 4 12"></polyline>
          </svg>
          <span>Khớp</span>
        </span>
        <span v-else-if="mappingItem.status === 'type_mismatch'" class="badge-status-warn" title="Cảnh báo: Kiểu dữ liệu không tương thích, có thể phát sinh lỗi runtime!">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
            <line x1="12" y1="9" x2="12" y2="13"></line>
            <line x1="12" y1="17" x2="12.01" y2="17"></line>
          </svg>
          <span>Lệch Type</span>
        </span>
        <span v-else class="badge-status-empty" title="Chưa liên kết trường form nào">
          <span>Chưa map</span>
        </span>
      </div>

      <span v-if="mappingItem.isAutoMatched" class="auto-tag" title="Hệ thống tự động nhận diện theo từ đồng nghĩa">
        ⚡ Tự khớp
      </span>
    </div>

    <!-- RIGHT: FORM FIELD SELECTOR -->
    <div class="side-col form-field-side">
      <div class="select-field-wrap">
        <select
          :value="mappingItem.formFieldKey"
          class="form-field-select"
          :class="{
            'select-matched': mappingItem.status === 'matched',
            'select-warn': mappingItem.status === 'type_mismatch',
            'select-empty': mappingItem.status === 'unmapped',
          }"
          @change="handleFieldChange"
        >
          <option value="">-- Chọn trường từ biểu mẫu --</option>
          <option
            v-for="field in formFields"
            :key="field.id"
            :value="field.key"
          >
            {{ field.label }} [{{ field.key }}] ({{ field.type }})
          </option>
        </select>
      </div>

      <!-- Preview of bound form field -->
      <div v-if="selectedField" class="selected-field-card">
        <div class="card-header">
          <span class="field-label-text">{{ selectedField.label }}</span>
          <span class="field-type-tag" :class="getFormFieldTypeBadge(selectedField.type).class">
            {{ getFormFieldTypeBadge(selectedField.type).text }}
          </span>
        </div>
        <div class="card-meta">
          <span class="meta-item">Khóa: <code>{{ selectedField.key }}</code></span>
          <span v-if="selectedField.required" class="meta-required">● Bắt buộc điền</span>
        </div>
      </div>

      <!-- If no field is mapped, option to provide fallback default value -->
      <div v-else class="unmapped-fallback-box">
        <span class="fallback-note">💡 Hoặc đặt giá trị cố định mặc định:</span>
        <input
          type="text"
          class="fallback-input"
          :placeholder="`Mặc định cho ${variable.key}...`"
          :value="mappingItem.customFallbackValue || ''"
          @input="emit('update:fallback', ($event.target as HTMLInputElement).value)"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.mapping-row {
  display: grid;
  grid-template-columns: 1fr 140px 1fr;
  align-items: center;
  gap: 1.25rem;
  padding: 1.25rem 1.5rem;
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);
  transition: all 0.25s ease;
  position: relative;
}

.mapping-row:hover {
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.06);
  border-color: #cbd5e1;
}

.mapping-row.status-matched {
  border-left: 4px solid #10b981;
}

.mapping-row.status-mismatch {
  border-left: 4px solid #f59e0b;
  background: #fffbeb;
}

.mapping-row.status-unmapped {
  border-left: 4px solid #94a3b8;
}

/* ==========================================================
   LEFT: WORKFLOW VARIABLE
   ========================================================== */
.workflow-var-side {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.var-title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.var-key-tag {
  font-family: 'JetBrains Mono', monospace;
  font-weight: 700;
  font-size: 0.875rem;
  color: #0f172a;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
}

.type-badge {
  font-size: 0.6875rem;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 6px;
  letter-spacing: 0.5px;
}

.badge-number {
  background: #ecfdf5;
  color: #059669;
  border: 1px solid #a7f3d0;
}

.badge-string {
  background: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
}

.badge-boolean {
  background: #fdf2f8;
  color: #db2777;
  border: 1px solid #fbcfe8;
}

.badge-date {
  background: #faf5ff;
  color: #7e22ce;
  border: 1px solid #e9d5ff;
}

.badge-select {
  background: #fff7ed;
  color: #c2410c;
  border: 1px solid #ffedd5;
}

.badge-file {
  background: #f0fdf4;
  color: #15803d;
  border: 1px solid #bbf7d0;
}

.badge-default {
  background: #f8fafc;
  color: #475569;
  border: 1px solid #e2e8f0;
}

.required-pill {
  font-size: 0.6875rem;
  font-weight: 700;
  color: #b91c1c;
  background: #fef2f2;
  border: 1px solid #fecaca;
  padding: 2px 6px;
  border-radius: 6px;
}

.system-pill {
  font-size: 0.6875rem;
  font-weight: 600;
  color: #475569;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 2px 6px;
  border-radius: 6px;
}

.var-label {
  font-size: 0.9375rem;
  font-weight: 600;
  color: #1e293b;
}

.var-desc {
  font-size: 0.75rem;
  color: #64748b;
  line-height: 1.4;
}

.condition-usage {
  margin-top: 0.25rem;
  padding-top: 0.35rem;
  border-top: 1px dashed #e2e8f0;
}

.usage-label {
  font-size: 0.6875rem;
  color: #64748b;
  font-weight: 600;
  display: block;
  margin-bottom: 0.25rem;
}

.usage-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
}

.edge-pill {
  font-size: 0.6875rem;
  background: #f8fafc;
  color: #334155;
  border: 1px solid #cbd5e1;
  padding: 2px 6px;
  border-radius: 4px;
}

/* ==========================================================
   MIDDLE: CONNECTOR
   ========================================================== */
.middle-connector {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.connector-line {
  position: relative;
  width: 100%;
  height: 2px;
  background: #cbd5e1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.connector-arrow {
  background: #ffffff;
  padding: 0 4px;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
}

.status-indicator-badge span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 0.6875rem;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 9999px;
}

.badge-status-ok {
  background: #ecfdf5;
  color: #059669;
  border: 1px solid #a7f3d0;
}

.badge-status-warn {
  background: #fef3c7;
  color: #d97706;
  border: 1px solid #fde68a;
}

.badge-status-empty {
  background: #f1f5f9;
  color: #64748b;
  border: 1px solid #e2e8f0;
}

.auto-tag {
  font-size: 0.625rem;
  font-weight: 700;
  color: #0284c7;
  background: #e0f2fe;
  padding: 1px 6px;
  border-radius: 4px;
}

/* ==========================================================
   RIGHT: FORM FIELD
   ========================================================== */
.form-field-side {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-field-select {
  width: 100%;
  height: 40px;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 600;
  padding: 0 0.75rem;
  color: #0f172a;
  background: #ffffff;
  border: 1.5px solid #cbd5e1;
  outline: none;
  transition: all 0.2s ease;
}

.form-field-select:focus {
  border-color: #0284c7;
  box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.15);
}

.form-field-select.select-matched {
  border-color: #10b981;
  background: #f0fdf4;
}

.form-field-select.select-warn {
  border-color: #f59e0b;
  background: #fffbeb;
}

.selected-field-card {
  padding: 0.5rem 0.75rem;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.field-label-text {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #334155;
}

.field-type-tag {
  font-size: 0.625rem;
  font-weight: 700;
  padding: 1px 5px;
  border-radius: 4px;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.6875rem;
  color: #64748b;
  margin-top: 0.25rem;
}

.card-meta code {
  font-family: 'JetBrains Mono', monospace;
  color: #0369a1;
  background: #e0f2fe;
  padding: 1px 4px;
  border-radius: 3px;
}

.meta-required {
  color: #e11d48;
  font-weight: 600;
}

.unmapped-fallback-box {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.fallback-note {
  font-size: 0.6875rem;
  color: #64748b;
}

.fallback-input {
  height: 32px;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  padding: 0 0.5rem;
  font-size: 0.8125rem;
  outline: none;
  background: #fafafa;
}

.fallback-input:focus {
  border-color: #0284c7;
  border-style: solid;
  background: #ffffff;
}

@media (max-width: 900px) {
  .mapping-row {
    grid-template-columns: 1fr;
    gap: 1rem;
  }
  .middle-connector {
    flex-direction: row;
  }
  .connector-line {
    display: none;
  }
}
</style>
