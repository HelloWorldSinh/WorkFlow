<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useFormStore } from '@/stores/formStore'
import FormRenderer from './FormRenderer.vue'
import type { FormField, FormItem } from '@/types/form'

const formStore = useFormStore()

const localForm = ref<FormItem | null>(null)
const testFormValues = ref<Record<string, any>>({})
const activeTab = ref<'design' | 'preview'>('design')
const selectedFieldId = ref<string | number | null>(null)

const initialSnapshot = ref<string>('')
const showExitConfirmModal = ref(false)

// Initialize local form when modal opens
watch(
  () => formStore.editingForm,
  (newVal) => {
    if (newVal) {
      localForm.value = JSON.parse(JSON.stringify(newVal))
      initialSnapshot.value = JSON.stringify(newVal)
      showExitConfirmModal.value = false
      testFormValues.value = {}
      activeTab.value = 'design'
      if (localForm.value && localForm.value.schema.fields.length > 0) {
        const first = localForm.value.schema.fields[0]
        selectedFieldId.value = first ? first.id : null
      }
    } else {
      localForm.value = null
      initialSnapshot.value = ''
      showExitConfirmModal.value = false
    }
  },
  { immediate: true }
)

const isDirty = computed(() => {
  if (!localForm.value) return false
  return JSON.stringify(localForm.value) !== initialSnapshot.value
})

const handleAttemptExit = () => {
  if (isDirty.value) {
    showExitConfirmModal.value = true
  } else {
    formStore.closeBuilderModal()
  }
}

const handleDiscardAndExit = () => {
  showExitConfirmModal.value = false
  formStore.closeBuilderModal()
}

const handleSaveAndExit = async () => {
  if (!localForm.value?.name.trim()) {
    alert('Vui lòng nhập tên biểu mẫu!')
    return
  }
  await formStore.saveForm(localForm.value)
  showExitConfirmModal.value = false
}

const handleCancelExit = () => {
  showExitConfirmModal.value = false
}

const selectedField = computed(() => {
  if (!localForm.value || selectedFieldId.value === null) return null
  return localForm.value.schema.fields.find((f) => String(f.id) === String(selectedFieldId.value)) || null
})

const addField = () => {
  if (!localForm.value) return
  const id = `field_${Date.now()}`
  const newField: FormField = {
    id,
    key: `field_${localForm.value.schema.fields.length + 1}`,
    label: `Trường mới ${localForm.value.schema.fields.length + 1}`,
    type: 'text',
    placeholder: 'Nhập giá trị...',
    required: false,
    orderIndex: localForm.value.schema.fields.length,
  }
  localForm.value.schema.fields.push(newField)
  selectedFieldId.value = id
}

const removeField = (id: string | number) => {
  if (!localForm.value) return
  localForm.value.schema.fields = localForm.value.schema.fields.filter((f) => String(f.id) !== String(id))
  if (String(selectedFieldId.value) === String(id)) {
    selectedFieldId.value = localForm.value.schema.fields[0]?.id ?? null
  }
}

const moveField = (index: number, direction: 'up' | 'down') => {
  if (!localForm.value) return
  const fields = localForm.value.schema.fields
  const targetIndex = direction === 'up' ? index - 1 : index + 1
  if (targetIndex < 0 || targetIndex >= fields.length) return
  const current = fields[index]
  const target = fields[targetIndex]
  if (current && target) {
    fields[index] = target
    fields[targetIndex] = current
  }
}

const addSelectOption = (field: FormField) => {
  if (!field.options) field.options = []
  const count = field.options.length + 1
  field.options.push({
    label: `Tùy chọn ${count}`,
    value: `opt_${count}`,
  })
}

const removeSelectOption = (field: FormField, index: number) => {
  if (!field.options) return
  field.options.splice(index, 1)
}

const onFieldTypeChange = (field: FormField) => {
  if (field.type === 'list') {
    if (!field.label || field.label.startsWith('Trường mới')) {
      field.label = 'Danh sách nhân viên'
    }
    if (!field.key || field.key.startsWith('field_')) {
      field.key = 'employees'
    }
    if (!field.placeholder) {
      field.placeholder = 'Nhập email hoặc account nhân viên để tìm kiếm...'
    }
  } else if (field.type === 'select') {
    if (!field.options || field.options.length === 0) {
      field.options = [
        { label: 'Tùy chọn 1', value: 'opt_1' },
        { label: 'Tùy chọn 2', value: 'opt_2' },
      ]
    }
  }
}

const handleSave = async () => {
  if (!localForm.value) return
  if (!localForm.value.name.trim()) {
    alert('Vui lòng nhập tên biểu mẫu!')
    return
  }
  await formStore.saveForm(localForm.value)
}
</script>

<template>
  <div v-if="formStore.isBuilderModalOpen && localForm" class="fullscreen-builder-modal">
    <!-- 1. FULLSCREEN TOP NAVIGATION BAR -->
    <header class="studio-topbar">
      <!-- Left: Title & Meta -->
      <div class="topbar-left">
        <button
          type="button"
          class="btn-back-exit"
          title="Quay lại danh sách biểu mẫu"
          @click="handleAttemptExit"
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
            <line x1="19" y1="12" x2="5" y2="12"></line>
            <polyline points="12 19 5 12 12 5"></polyline>
          </svg>
        </button>

        <div class="studio-title-group">
          <div class="badge-row">
            <span class="mode-tag">{{ localForm.id && formStore.getFormById(localForm.id) ? 'Chỉnh sửa' : 'Tạo mới' }}</span>
            <span v-if="isDirty" class="unsaved-badge">● Chưa lưu</span>
          </div>
          <h2 class="form-display-name">{{ localForm.name || 'Biểu mẫu chưa đặt tên' }}</h2>
        </div>
      </div>

      <!-- Center: 2 Main Tabs Switcher -->
      <div class="topbar-center">
        <div class="tab-pill-switcher">
          <button
            type="button"
            class="tab-btn"
            :class="{ 'is-active': activeTab === 'design' }"
            @click="activeTab = 'design'"
          >
            <span class="tab-icon">🛠️</span>
            <span>Chỉnh Sửa Thiết Kế</span>
          </button>

          <button
            type="button"
            class="tab-btn"
            :class="{ 'is-active': activeTab === 'preview' }"
            @click="activeTab = 'preview'"
          >
            <span class="tab-icon">👁️</span>
            <span>Xem Trước Thực Tế</span>
            <span class="live-dot"></span>
          </button>
        </div>
      </div>

      <!-- Right: Actions -->
      <div class="topbar-right">
        <button
          type="button"
          class="btn-save-form"
          :disabled="formStore.isSaving"
          @click="handleSave"
        >
          <svg v-if="!formStore.isSaving" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <polyline points="20 6 9 17 4 12"></polyline>
          </svg>
          <span>{{ formStore.isSaving ? 'Đang lưu...' : 'Lưu Biểu Mẫu' }}</span>
        </button>
      </div>
    </header>

    <!-- 2. TAB CONTENT: TAB 1 - CHỈNH SỬA THIẾT KẾ -->
    <div v-show="activeTab === 'design'" class="tab-view-container tab-design-view">
      <!-- General Meta Info Strip -->
      <div class="general-info-strip">
        <div class="info-group" style="flex: 1.2;">
          <label class="strip-label required">Tên biểu mẫu</label>
          <input
            v-model="localForm.name"
            type="text"
            class="strip-input"
            placeholder="Nhập tên biểu mẫu (ví dụ: Ticket Hỗ Trợ IT, Đơn Đề Xuất...)"
          />
        </div>
        <div class="info-group" style="flex: 2;">
          <label class="strip-label">Mô tả mục đích sử dụng</label>
          <input
            v-model="localForm.description"
            type="text"
            class="strip-input"
            placeholder="Nhập mô tả tóm tắt cho người dùng..."
          />
        </div>
      </div>

      <!-- Main 2-Column Design Studio -->
      <div class="design-studio-body">
        <!-- Column 1: Field List Palette -->
        <aside class="studio-sidebar-fields">
          <div class="sidebar-header">
            <div class="s-title-group">
              <span class="s-title">Các trường dữ liệu</span>
              <span class="s-badge">{{ localForm.schema.fields.length }}</span>
            </div>
            <button type="button" class="btn-add-new-field" @click="addField">
              <span>+ Thêm trường</span>
            </button>
          </div>

          <div class="sidebar-fields-scroll">
            <div
              v-for="(field, index) in localForm.schema.fields"
              :key="field.id"
              class="field-card-item"
              :class="{ 'is-active': field.id === selectedFieldId }"
              @click="selectedFieldId = field.id"
            >
              <div class="f-card-content">
                <div class="f-card-meta">
                  <span class="f-type-tag">{{ field.type }}</span>
                  <span v-if="field.required" class="f-req-tag" title="Trường bắt buộc">*</span>
                  <span class="f-key-tag">{{ field.key }}</span>
                </div>
                <div class="f-card-label">{{ field.label || '(Chưa đặt tên trường)' }}</div>
              </div>

              <div class="f-card-actions">
                <button
                  type="button"
                  class="action-icon-btn"
                  :disabled="index === 0"
                  title="Di chuyển lên"
                  @click.stop="moveField(index, 'up')"
                >
                  ↑
                </button>
                <button
                  type="button"
                  class="action-icon-btn"
                  :disabled="index === localForm.schema.fields.length - 1"
                  title="Di chuyển xuống"
                  @click.stop="moveField(index, 'down')"
                >
                  ↓
                </button>
                <button
                  type="button"
                  class="action-icon-btn btn-trash"
                  title="Xóa trường này"
                  @click.stop="removeField(field.id)"
                >
                  ✕
                </button>
              </div>
            </div>
          </div>
        </aside>

        <!-- Column 2: Detailed Field Properties Editor -->
        <main class="studio-main-properties">
          <div v-if="selectedField" class="field-editor-canvas">
            <div class="editor-section-header">
              <div class="esh-left">
                <span class="esh-badge">Cấu hình thuộc tính</span>
                <h3 class="esh-title">{{ selectedField.label || 'Trường chưa đặt tên' }}</h3>
              </div>
            </div>

            <!-- Configuration Grid -->
            <div class="config-grid-card">
              <div class="form-row-2col">
                <div class="config-group">
                  <label class="config-label required">Tên hiển thị cho người dùng (Label)</label>
                  <input
                    v-model="selectedField.label"
                    type="text"
                    class="config-input"
                    placeholder="Ví dụ: Tiêu đề yêu cầu, Số lượng, Ngày bắt đầu..."
                  />
                </div>

                <div class="config-group">
                  <label class="config-label required">Mã biến hệ thống (Field Key)</label>
                  <input
                    v-model="selectedField.key"
                    type="text"
                    class="config-input key-code-font"
                    placeholder="field_key_name"
                  />
                  <span class="field-hint">Mã biến dùng để thiết lập điều kiện rẽ nhánh và lưu trữ JSON variables.</span>
                </div>
              </div>

              <div class="form-row-2col">
                <div class="config-group">
                  <label class="config-label required">Loại trường dữ liệu (Field Type)</label>
                  <select
                    v-model="selectedField.type"
                    class="config-input config-select"
                    @change="onFieldTypeChange(selectedField)"
                  >
                    <option value="text">Text</option>
                    <option value="number">Number</option>
                    <option value="textarea">Textarea</option>
                    <option value="select">Dropdown Select</option>
                    <option value="date">Date</option>
                    <option value="file">File Attachment</option>
                    <option value="checkbox">Checkbox</option>
                    <option value="list">Danh sách nhân viên</option>
                  </select>
                </div>

                <div class="config-group">
                  <label class="config-label">Gợi ý nhập liệu (Placeholder)</label>
                  <input
                    v-model="selectedField.placeholder"
                    type="text"
                    class="config-input"
                    placeholder="Gợi ý hiển thị mờ trong ô nhập..."
                  />
                </div>
              </div>

              <div class="form-row-full">
                <div class="config-group">
                  <label class="config-label">Ghi chú hướng dẫn (Help Text)</label>
                  <input
                    v-model="selectedField.helpText"
                    type="text"
                    class="config-input"
                    placeholder="Dòng giải thích nhỏ xuất hiện dưới trường nhập liệu..."
                  />
                </div>
              </div>

              <!-- Required Checkbox -->
              <div class="config-checkbox-row">
                <label class="custom-chk-label">
                  <input v-model="selectedField.required" type="checkbox" />
                  <span class="chk-box"></span>
                  <span class="chk-text">
                    <strong>Bắt buộc người dùng phải điền (Required field)</strong>
                    <small>Người dùng không thể gửi Ticket nếu để trống trường này.</small>
                  </span>
                </label>
              </div>

              <!-- Additional Settings for Select Type -->
              <div v-if="selectedField.type === 'select'" class="options-management-box">
                <div class="opt-header">
                  <div class="opt-title-group">
                    <span class="opt-title">Danh Sách Tùy Chọn (Options)</span>
                    <span class="opt-desc">Định nghĩa các mục mà người dùng có thể lựa chọn từ Dropdown</span>
                  </div>
                  <button type="button" class="btn-add-opt" @click="addSelectOption(selectedField)">
                    + Thêm Tùy Chọn
                  </button>
                </div>

                <div class="opt-list-table">
                  <div
                    v-for="(opt, oIdx) in selectedField.options"
                    :key="oIdx"
                    class="opt-item-row"
                  >
                    <span class="opt-index">#{{ oIdx + 1 }}</span>
                    <div class="opt-input-wrap" style="flex: 1.5;">
                      <label class="mini-lbl">Nhãn hiển thị</label>
                      <input v-model="opt.label" type="text" class="opt-input" placeholder="Ví dụ: Khẩn cấp..." />
                    </div>
                    <div class="opt-input-wrap" style="flex: 1;">
                      <label class="mini-lbl">Giá trị biến (Value)</label>
                      <input v-model="opt.value" type="text" class="opt-input key-code-font" placeholder="urgent" />
                    </div>
                    <button
                      type="button"
                      class="btn-del-opt"
                      title="Xóa tùy chọn này"
                      @click="removeSelectOption(selectedField, oIdx)"
                    >
                      ✕
                    </button>
                  </div>
                </div>
              </div>

              <!-- Additional Settings for Number Type -->
              <div v-if="selectedField.type === 'number'" class="number-range-box">
                <div class="form-row-2col">
                  <div class="config-group">
                    <label class="config-label">Giá trị nhỏ nhất (Min)</label>
                    <input v-model.number="selectedField.min" type="number" class="config-input" placeholder="0" />
                  </div>
                  <div class="config-group">
                    <label class="config-label">Giá trị lớn nhất (Max)</label>
                    <input v-model.number="selectedField.max" type="number" class="config-input" placeholder="100000000" />
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-else class="empty-field-state">
            <div class="e-icon">👈</div>
            <h4>Chưa chọn trường dữ liệu nào</h4>
            <p>Vui lòng chọn một trường từ danh sách bên trái hoặc nhấn <strong>+ Thêm trường</strong> để bắt đầu cấu hình.</p>
          </div>
        </main>
      </div>
    </div>

    <!-- 3. TAB CONTENT: TAB 2 - XEM TRƯỚC BIỂU MẪU THỰC TẾ -->
    <div v-show="activeTab === 'preview'" class="tab-view-container tab-preview-view">

      <!-- Centered Document Paper Frame -->
      <div class="preview-scroll-viewport">
        <div class="document-paper">
          <div class="paper-header">
            <div class="paper-badge-row">
              <span class="paper-badge">Biểu Mẫu Tiếp Nhận</span>
              <span class="paper-date">Hệ thống Workflow Builder</span>
            </div>
            <h1 class="paper-title">{{ localForm.name || 'Tiêu Đề Biểu Mẫu' }}</h1>
            <p class="paper-description">{{ localForm.description || 'Mô tả chi tiết mục đích và hướng dẫn biểu mẫu sẽ xuất hiện ở đây.' }}</p>
          </div>

          <div class="paper-body">
            <FormRenderer
              :schema="localForm.schema"
              :model-value="testFormValues"
              @update:model-value="testFormValues = $event"
            />
          </div>

          <div class="paper-footer">
            <button type="button" class="btn-demo-submit" disabled>
              Gửi Yêu Cầu / Tạo Ticket (Bản Xem Trước)
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 4. CONFIRM EXIT DIALOG -->
    <div v-if="showExitConfirmModal" class="confirm-exit-overlay" @click.self="handleCancelExit">
      <div class="confirm-exit-card">
        <div class="confirm-header">
          <h3 class="confirm-title">Xác nhận rời khỏi Studio</h3>
          <button type="button" class="btn-close-confirm" @click="handleCancelExit">✕</button>
        </div>

        <div class="confirm-body">
          <div class="confirm-icon-wrapper">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="8" x2="12" y2="12"></line>
              <line x1="12" y1="16" x2="12.01" y2="16"></line>
            </svg>
          </div>
          <div class="confirm-text">
            <div class="confirm-main-text">Biểu mẫu có những thay đổi chưa được lưu</div>
            <div class="confirm-sub-text">
              Bạn có muốn lưu lại các chỉnh sửa trước khi thoát không? Các thay đổi chưa lưu sẽ bị mất nếu bạn bỏ qua.
            </div>
          </div>
        </div>

        <div class="confirm-footer">
          <button type="button" class="modal-btn btn-discard-exit" @click="handleDiscardAndExit">
            Bỏ thay đổi
          </button>
          <button type="button" class="modal-btn btn-save-exit" :disabled="formStore.isSaving" @click="handleSaveAndExit">
            {{ formStore.isSaving ? 'Đang lưu...' : 'Lưu' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* FULLSCREEN MODAL OVERLAY */
.fullscreen-builder-modal {
  position: fixed;
  inset: 0;
  width: 100vw;
  height: 100vh;
  background: #ffffff;
  z-index: 1050;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 1. TOPBAR */
.studio-topbar {
  height: 64px;
  background: #ffffff;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 1.5rem;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  flex-shrink: 0;
  gap: 1rem;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 1rem;
  min-width: 260px;
}

.btn-back-exit {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: #ffffff;
  color: #475569;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-back-exit:hover {
  background: #f1f5f9;
  color: var(--primary);
  border-color: #cbd5e1;
}

.studio-title-group {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.badge-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.studio-badge {
  font-size: 0.65rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--primary);
}

.mode-tag {
  font-size: 0.65rem;
  font-weight: 600;
  background: #f1f5f9;
  color: #64748b;
  padding: 0.05rem 0.4rem;
  border-radius: 4px;
}

.form-display-name {
  font-size: 1rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  white-space: nowrap;
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* CENTER TAB SWITCHER */
.topbar-center {
  display: flex;
  align-items: center;
  justify-content: center;
}

.tab-pill-switcher {
  display: flex;
  background: #f1f5f9;
  padding: 0.3rem;
  border-radius: 10px;
  border: 1px solid var(--border-color);
  gap: 0.25rem;
}

.tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.45rem 1.1rem;
  border-radius: 7px;
  border: none;
  background: transparent;
  font-size: 0.84rem;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.tab-btn:hover {
  color: #0f172a;
}

.tab-btn.is-active {
  background: #ffffff;
  color: var(--primary);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.tab-icon {
  font-size: 0.95rem;
}

.tab-counter {
  font-size: 0.6875rem;
  background: var(--primary-light);
  color: var(--primary);
  padding: 0.1rem 0.45rem;
  border-radius: 9999px;
  font-weight: 700;
}

.live-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #10b981;
}

/* RIGHT TOPBAR */
.topbar-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.btn-save-form {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: var(--accent-gradient);
  color: #ffffff;
  border: none;
  padding: 0.55rem 1.25rem;
  border-radius: 8px;
  font-size: 0.84rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(2, 132, 199, 0.35);
  transition: all 0.2s ease;
}

.btn-save-form:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(2, 132, 199, 0.5);
}

/* 2. TAB VIEW CONTAINER */
.tab-view-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #f8fafc;
}

/* TAB 1: DESIGN VIEW */
.general-info-strip {
  display: flex;
  gap: 1.5rem;
  padding: 0.875rem 2rem;
  background: #ffffff;
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}

.info-group {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.strip-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: #475569;
}

.strip-label.required::after {
  content: ' *';
  color: #ef4444;
}

.strip-input {
  padding: 0.55rem 0.875rem;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.875rem;
  color: #1e293b;
  outline: none;
  transition: all 0.2s ease;
}

.strip-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--border-glow);
}

.design-studio-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* SIDEBAR FIELDS */
.studio-sidebar-fields {
  width: 380px;
  background: #ffffff;
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--border-color);
}

.s-title-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.s-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: #0f172a;
}

.s-badge {
  font-size: 0.72rem;
  background: var(--primary-light);
  color: var(--primary);
  font-weight: 700;
  padding: 0.15rem 0.5rem;
  border-radius: 9999px;
}

.btn-add-new-field {
  background: var(--accent-gradient);
  color: #ffffff;
  border: none;
  padding: 0.45rem 0.875rem;
  border-radius: 6px;
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-add-new-field:hover {
  transform: translateY(-1px);
}

.sidebar-fields-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
}

.field-card-item {
  background: #ffffff;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 0.75rem;
  display: flex;
  align-items: center;
  gap: 0.625rem;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}

.field-card-item:hover {
  border-color: #bae6fd;
}

.field-card-item.is-active {
  border-color: var(--primary);
  background: #f0f9ff;
  box-shadow: 0 0 0 2px var(--border-glow);
}

.f-card-content {
  flex: 1;
  overflow: hidden;
}

.f-card-meta {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  margin-bottom: 0.25rem;
}

.f-type-tag {
  font-size: 0.65rem;
  font-weight: 700;
  text-transform: uppercase;
  background: #e2e8f0;
  color: #475569;
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
}

.f-req-tag {
  font-size: 0.85rem;
  font-weight: 700;
  color: #ef4444;
  line-height: 1;
}

.f-key-tag {
  font-size: 0.65rem;
  font-family: monospace;
  color: #94a3b8;
}

.f-card-label {
  font-size: 0.84rem;
  font-weight: 600;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.f-card-actions {
  display: flex;
  align-items: center;
  gap: 0.2rem;
}

.action-icon-btn {
  background: none;
  border: none;
  width: 24px;
  height: 24px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #64748b;
  font-size: 0.78rem;
}

.action-icon-btn:hover:not(:disabled) {
  background: #e2e8f0;
  color: #0f172a;
}

.action-icon-btn.btn-trash:hover {
  background: #fee2e2 !important;
  color: #dc2626 !important;
}

/* MAIN PROPERTIES AREA */
.studio-main-properties {
  flex: 1;
  overflow-y: auto;
  padding: 2rem;
  background: #f8fafc;
}

.field-editor-canvas {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.editor-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--border-color);
}

.esh-left {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.esh-badge {
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
  color: var(--primary);
  letter-spacing: 0.05em;
}

.esh-title {
  font-size: 1.35rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

.esh-key-pill {
  font-size: 0.8125rem;
  font-family: monospace;
  background: #ffffff;
  color: #475569;
  border: 1px solid var(--border-color);
  padding: 0.3rem 0.75rem;
  border-radius: 6px;
}

.config-grid-card {
  background: #ffffff;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 1.75rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.03);
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.form-row-2col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
}

.form-row-full {
  display: flex;
  flex-direction: column;
}

.config-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.config-label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #334155;
}

.config-label.required::after {
  content: ' *';
  color: #ef4444;
}

.config-input {
  padding: 0.625rem 0.875rem;
  border: 1.5px solid #cbd5e1;
  border-radius: 8px;
  font-size: 0.875rem;
  color: #1e293b;
  outline: none;
  transition: all 0.2s ease;
}

.config-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--border-glow);
}

.key-code-font {
  font-family: monospace;
  color: #0369a1;
  font-weight: 600;
}

.field-hint {
  font-size: 0.72rem;
  color: #94a3b8;
  margin-top: 0.2rem;
}

.config-select {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%2364748b' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'%3E%3C/polyline%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.75rem center;
  background-size: 1rem;
}

.config-checkbox-row {
  padding-top: 0.5rem;
  border-top: 1px dashed var(--border-color);
}

.custom-chk-label {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  cursor: pointer;
}

.custom-chk-label input {
  accent-color: var(--primary);
  width: 18px;
  height: 18px;
  margin-top: 2px;
}

.chk-text {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.chk-text strong {
  font-size: 0.84rem;
  color: #1e293b;
}

.chk-text small {
  font-size: 0.75rem;
  color: #64748b;
}

/* OPTIONS MANAGEMENT FOR SELECT */
.options-management-box {
  background: #f8fafc;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  padding: 1.25rem;
  margin-top: 0.5rem;
}

.opt-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.opt-title {
  font-size: 0.84rem;
  font-weight: 700;
  color: #0f172a;
}

.opt-desc {
  font-size: 0.72rem;
  color: #64748b;
  display: block;
}

.btn-add-opt {
  background: var(--primary-light);
  color: var(--primary);
  border: 1px solid #bae6fd;
  font-size: 0.75rem;
  font-weight: 600;
  padding: 0.4rem 0.75rem;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-add-opt:hover {
  background: #bae6fd;
}

.opt-list-table {
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
}

.opt-item-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  background: #ffffff;
  padding: 0.625rem 0.875rem;
  border-radius: 8px;
  border: 1px solid var(--border-color);
}

.opt-index {
  font-size: 0.75rem;
  font-weight: 700;
  color: #94a3b8;
  width: 24px;
}

.opt-input-wrap {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.mini-lbl {
  font-size: 0.65rem;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
}

.opt-input {
  padding: 0.4rem 0.6rem;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  font-size: 0.8125rem;
  outline: none;
}

.opt-input:focus {
  border-color: var(--primary);
}

.btn-del-opt {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 0.4rem;
  border-radius: 4px;
  font-size: 0.875rem;
}

.btn-del-opt:hover {
  background: #fee2e2;
  color: #dc2626;
}

/* EMPLOYEE LIST FIELD INFO (KHÔNG CẦN CẤU HÌNH) */
.employee-list-field-info {
  display: flex;
  align-items: flex-start;
  gap: 0.875rem;
  background: #f0fdf4;
  border: 1.5px solid #bbf7d0;
  border-radius: 10px;
  padding: 1.125rem 1.25rem;
  margin-top: 0.5rem;
}

.elf-icon {
  font-size: 1.75rem;
  line-height: 1;
}

.elf-content {
  flex: 1;
}

.elf-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: #166534;
  margin: 0 0 0.25rem 0;
}

.elf-desc {
  font-size: 0.8125rem;
  color: #15803d;
  margin: 0;
  line-height: 1.5;
}

.empty-field-state {
  text-align: center;
  padding: 5rem 2rem;
  max-width: 420px;
  margin: 0 auto;
}

.e-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.empty-field-state h4 {
  font-size: 1.15rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 0.5rem 0;
}

.empty-field-state p {
  font-size: 0.84rem;
  color: #64748b;
  margin: 0;
  line-height: 1.5;
}

/* 3. TAB 2: PREVIEW VIEW */
.tab-preview-view {
  background: #f1f5f9;
}

.preview-scroll-viewport {
  flex: 1;
  overflow-y: auto;
  padding: 2.5rem 1.5rem;
  display: flex;
  justify-content: center;
}

.document-paper {
  background: #ffffff;
  width: 100%;
  max-width: 820px;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(15, 23, 42, 0.06);
  border: 1px solid var(--border-color);
  padding: 2.5rem;
  display: flex;
  flex-direction: column;
  gap: 2rem;
  height: fit-content;
}

.paper-header {
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.paper-badge-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.paper-badge {
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
  color: var(--primary);
  background: var(--primary-light);
  padding: 0.2rem 0.6rem;
  border-radius: 4px;
}

.paper-date {
  font-size: 0.75rem;
  color: #94a3b8;
}

.paper-title {
  font-size: 1.6rem;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.02em;
  margin: 0.25rem 0;
}

.paper-description {
  font-size: 0.875rem;
  color: #64748b;
  margin: 0;
  line-height: 1.5;
}

.paper-footer {
  border-top: 1px solid #f1f5f9;
  padding-top: 1.5rem;
  display: flex;
  justify-content: flex-end;
}

.btn-demo-submit {
  background: #94a3b8;
  color: #ffffff;
  border: none;
  padding: 0.65rem 1.5rem;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: not-allowed;
  opacity: 0.75;
}

/* UNSAVED BADGE */
.unsaved-badge {
  font-size: 0.65rem;
  font-weight: 700;
  color: #ea580c;
  background: #fff7ed;
  border: 1px solid #ffedd5;
  padding: 0.05rem 0.45rem;
  border-radius: 4px;
}

/* CONFIRM EXIT DIALOG STYLES */
.confirm-exit-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1200;
  padding: 1.5rem;
}

.confirm-exit-card {
  background: #ffffff;
  width: 100%;
  max-width: 520px;
  border-radius: 14px;
  box-shadow: 0 20px 40px -15px rgba(15, 23, 42, 0.25);
  border: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: confirmPop 0.18s ease-out;
}

@keyframes confirmPop {
  from {
    opacity: 0;
    transform: scale(0.96) translateY(6px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.confirm-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.15rem 1.5rem;
  border-bottom: 1px solid var(--border-color);
  background: #f8fafc;
}

.confirm-title {
  font-size: 1rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.btn-close-confirm {
  background: none;
  border: none;
  color: #94a3b8;
  font-size: 1.1rem;
  cursor: pointer;
  padding: 0.25rem 0.5rem;
  border-radius: 6px;
  transition: all 0.15s ease;
}

.btn-close-confirm:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.confirm-body {
  display: flex;
  gap: 1rem;
  align-items: flex-start;
  padding: 1.5rem;
}

.confirm-icon-wrapper {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: var(--primary-light);
  color: var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.confirm-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.confirm-main-text {
  font-size: 0.95rem;
  color: #0f172a;
  font-weight: 700;
  line-height: 1.4;
}

.confirm-sub-text {
  font-size: 0.8125rem;
  color: #475569;
  line-height: 1.45;
}

.confirm-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.625rem;
  padding: 1rem 1.5rem;
  background: #f8fafc;
  border-top: 1px solid var(--border-color);
}

.modal-btn {
  padding: 0.5rem 1rem;
  border-radius: 7px;
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-discard-exit {
  background: #fee2e2;
  border: 1px solid #fca5a5;
  color: #b91c1c;
}

.btn-discard-exit:hover {
  background: #fecaca;
  color: #991b1b;
}

.btn-save-exit {
  background: var(--accent-gradient);
  border: none;
  color: #ffffff;
  box-shadow: 0 2px 6px rgba(2, 132, 199, 0.3);
}

.btn-save-exit:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(2, 132, 199, 0.45);
}
</style>
