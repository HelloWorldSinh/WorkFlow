<script setup lang="ts">
import type { FormField } from '@/types/form'
import type { WorkflowEditorNode } from '@/types/editor'
import type { NodePermissionMatrix, FieldPermissionType } from '@/types/mapping'

const props = defineProps<{
  nodes: WorkflowEditorNode[]
  formFields: FormField[]
  permissions: NodePermissionMatrix
}>()

const emit = defineEmits<{
  (e: 'update:permission', nodeId: string, fieldKey: string, val: FieldPermissionType): void
  (e: 'apply:preset', preset: 'start_only' | 'all_readonly' | 'all_editable'): void
}>()

const getNodeTypeBadge = (type: string) => {
  switch (type) {
    case 'start':
      return { label: 'Bắt đầu', bg: '#ecfdf5', color: '#059669' }
    case 'approval':
      return { label: 'Phê duyệt', bg: '#eef2ff', color: '#4f46e5' }
    case 'review':
      return { label: 'Soát xét', bg: '#eff6ff', color: '#2563eb' }
    case 'assignment':
      return { label: 'Giao việc', bg: '#fff7ed', color: '#c2410c' }
    case 'notification':
      return { label: 'Thông báo', bg: '#fdf2f8', color: '#db2777' }
    case 'system_action':
      return { label: 'Hệ thống', bg: '#f5f3ff', color: '#7c3aed' }
    case 'end':
      return { label: 'Kết thúc', bg: '#f1f5f9', color: '#475569' }
    default:
      return { label: type, bg: '#f8fafc', color: '#64748b' }
  }
}

const getPermission = (nodeId: string, fieldKey: string): FieldPermissionType => {
  return props.permissions[nodeId]?.[fieldKey] || 'readonly'
}

const setPermission = (nodeId: string, fieldKey: string, val: FieldPermissionType) => {
  emit('update:permission', nodeId, fieldKey, val)
}
</script>

<template>
  <div class="permission-matrix-container">
    <!-- TOP TOOLBAR: PRESET QUICK ACTIONS -->
    <div class="matrix-toolbar">
      <div class="toolbar-title-group">
        <h3 class="toolbar-title">Ma trận phân quyền trường dữ liệu theo từng bước</h3>
        <p class="toolbar-sub">
          Quy định quyền truy cập của người xử lý tại mỗi bước trong quy trình đối với từng trường dữ liệu biểu mẫu.
        </p>
      </div>

      <div class="preset-buttons">
        <span class="preset-label">Thiết lập nhanh:</span>
        <button
          type="button"
          class="btn-preset"
          @click="emit('apply:preset', 'start_only')"
        >
          ✏️ Bước đầu được sửa, các bước sau chỉ xem
        </button>
        <button
          type="button"
          class="btn-preset"
          @click="emit('apply:preset', 'all_readonly')"
        >
          🔒 Tất cả chỉ xem
        </button>
        <button
          type="button"
          class="btn-preset"
          @click="emit('apply:preset', 'all_editable')"
        >
          📝 Mở quyền sửa toàn bộ
        </button>
      </div>
    </div>

    <!-- LEGEND -->
    <div class="matrix-legend">
      <span class="legend-item"><span class="legend-dot dot-edit"></span> <strong>Cho phép sửa (Editable):</strong> Người xử lý được điền/chỉnh sửa nội dung trường.</span>
      <span class="legend-item"><span class="legend-dot dot-read"></span> <strong>Chỉ xem (Readonly):</strong> Khóa dữ liệu, người xử lý chỉ đọc thông tin đã điền.</span>
      <span class="legend-item"><span class="legend-dot dot-hide"></span> <strong>Ẩn trường (Hidden):</strong> Không hiển thị trường này tại bước đó (bảo mật/nhạy cảm).</span>
    </div>

    <!-- MATRIX TABLE -->
    <div class="table-responsive">
      <table class="permission-table">
        <thead>
          <tr>
            <th class="th-field-info">Trường dữ liệu Form \ Bước Quy Trình</th>
            <th
              v-for="node in nodes"
              :key="node.id"
              class="th-node"
            >
              <div class="node-header-wrap">
                <span
                  class="node-badge"
                  :style="{ backgroundColor: getNodeTypeBadge(node.type).bg, color: getNodeTypeBadge(node.type).color }"
                >
                  {{ getNodeTypeBadge(node.type).label }}
                </span>
                <span class="node-name-label" :title="node.name">{{ node.name }}</span>
              </div>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="field in formFields" :key="field.id">
            <td class="td-field">
              <div class="field-meta-box">
                <span class="f-label">{{ field.label }}</span>
                <span class="f-key"><code>{{ field.key }}</code> ({{ field.type }})</span>
              </div>
            </td>

            <td
              v-for="node in nodes"
              :key="node.id"
              class="td-cell"
            >
              <select
                class="perm-select"
                :class="`perm-${getPermission(node.id, field.key)}`"
                :value="getPermission(node.id, field.key)"
                @change="setPermission(node.id, field.key, ($event.target as HTMLSelectElement).value as FieldPermissionType)"
              >
                <option value="editable">✏️ Cho phép sửa</option>
                <option value="readonly">🔒 Chỉ xem</option>
                <option value="hidden">🚫 Ẩn trường</option>
              </select>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.permission-matrix-container {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  padding: 1.5rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);
}

.matrix-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
  padding-bottom: 1rem;
  border-bottom: 1px solid #e2e8f0;
}

.toolbar-title {
  font-size: 1.125rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.toolbar-sub {
  font-size: 0.8125rem;
  color: #64748b;
  margin: 0.25rem 0 0 0;
}

.preset-buttons {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.preset-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: #64748b;
}

.btn-preset {
  font-size: 0.75rem;
  font-weight: 600;
  padding: 0.4rem 0.75rem;
  background: #f8fafc;
  color: #334155;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-preset:hover {
  background: #0284c7;
  color: #ffffff;
  border-color: #0284c7;
  transform: translateY(-1px);
}

.matrix-legend {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  font-size: 0.75rem;
  color: #475569;
  background: #f8fafc;
  padding: 0.6rem 1rem;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.dot-edit {
  background: #10b981;
}

.dot-read {
  background: #3b82f6;
}

.dot-hide {
  background: #94a3b8;
}

/* ==========================================================
   TABLE
   ========================================================== */
.table-responsive {
  overflow-x: auto;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.permission-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.th-field-info {
  background: #f1f5f9;
  color: #334155;
  font-size: 0.8125rem;
  font-weight: 700;
  padding: 1rem;
  min-width: 220px;
  position: sticky;
  left: 0;
  z-index: 10;
  border-right: 1px solid #cbd5e1;
}

.th-node {
  background: #f8fafc;
  padding: 0.75rem 1rem;
  min-width: 180px;
  border-right: 1px solid #e2e8f0;
}

.node-header-wrap {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.node-badge {
  display: inline-block;
  font-size: 0.625rem;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 4px;
  width: fit-content;
}

.node-name-label {
  font-size: 0.8125rem;
  font-weight: 700;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 170px;
}

.td-field {
  padding: 0.875rem 1rem;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  border-right: 1px solid #cbd5e1;
  position: sticky;
  left: 0;
  z-index: 5;
}

.field-meta-box {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.f-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: #1e293b;
}

.f-key {
  font-size: 0.6875rem;
  color: #64748b;
}

.f-key code {
  font-family: 'JetBrains Mono', monospace;
  color: #0369a1;
}

.td-cell {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #e2e8f0;
  border-right: 1px solid #e2e8f0;
}

.perm-select {
  width: 100%;
  height: 34px;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
  padding: 0 0.5rem;
  outline: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.perm-editable {
  background: #ecfdf5;
  color: #047857;
  border: 1.5px solid #a7f3d0;
}

.perm-readonly {
  background: #eff6ff;
  color: #1d4ed8;
  border: 1.5px solid #bfdbfe;
}

.perm-hidden {
  background: #f1f5f9;
  color: #64748b;
  border: 1.5px solid #cbd5e1;
}
</style>
