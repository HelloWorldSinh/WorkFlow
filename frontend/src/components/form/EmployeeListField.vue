<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { userApi, type UserSummary } from '@/services/workflowApi'

export interface SelectedEmployee {
  id: number | string
  fullName: string
  email: string
  account: string
  role?: string
  departmentId?: number
}

const props = withDefaults(
  defineProps<{
    modelValue?: SelectedEmployee[]
    disabled?: boolean
    placeholder?: string
    required?: boolean
  }>(),
  {
    modelValue: () => [],
    disabled: false,
    placeholder: 'Nhập email hoặc account nhân viên để tìm kiếm...',
    required: false,
  }
)

const emit = defineEmits<{
  (e: 'update:modelValue', val: SelectedEmployee[]): void
}>()

// Dữ liệu mẫu nhân viên fallback khi backend chưa có hoặc rỗng
const MOCK_EMPLOYEES: UserSummary[] = [
  { id: 101, fullName: 'Nguyễn Văn An', email: 'an.nv@company.com', role: 'Nhân viên IT' },
  { id: 102, fullName: 'Trần Thị Bình', email: 'binh.tt@company.com', role: 'Nhân viên Marketing' },
  { id: 103, fullName: 'Lê Hoàng Cường', email: 'cuong.lh@company.com', role: 'Nhân viên Kỹ thuật' },
  { id: 104, fullName: 'Phạm Minh Đức', email: 'duc.pm@company.com', role: 'Nhân viên Kinh doanh' },
  { id: 105, fullName: 'Vũ Thị Hoa', email: 'hoa.vt@company.com', role: 'Nhân viên Kế toán' },
  { id: 106, fullName: 'Đặng Quốc Huy', email: 'huy.dq@company.com', role: 'Nhân viên Nhân sự' },
  { id: 107, fullName: 'Ngô Thanh Tùng', email: 'tung.nt@company.com', role: 'Nhân viên Vận hành' },
]

const searchKeyword = ref('')
const isSearching = ref(false)
const rawResults = ref<UserSummary[]>([])
const hasSearched = ref(false)
const isDropdownOpen = ref(false)

const selectedEmployees = computed<SelectedEmployee[]>({
  get: () => (Array.isArray(props.modelValue) ? props.modelValue : []),
  set: (val) => emit('update:modelValue', val),
})

const selectedIds = computed(() => new Set(selectedEmployees.value.map((e) => String(e.id))))
const selectedEmails = computed(() => new Set(selectedEmployees.value.map((e) => e.email.toLowerCase())))

// Lọc chỉ chọn NHÂN VIÊN (Loại trừ quản trị viên Admin) và loại trừ những ai đã được chọn trước đó
const filteredResults = computed(() => {
  return rawResults.value.filter((user) => {
    // 1. Loại trừ Admin
    const roleStr = (user.role || '').toLowerCase()
    if (roleStr === 'admin') return false

    // 2. Loại trừ những người đã chọn vào danh sách
    if (selectedIds.value.has(String(user.id))) return false
    if (user.email && selectedEmails.value.has(user.email.toLowerCase())) return false

    return true
  })
})

const extractAccount = (user: UserSummary): string => {
  if (user.email && user.email.includes('@')) {
    const parts = user.email.split('@')
    if (parts[0]) return parts[0]
  }
  return `user_${user.id}`
}

const handleSearch = async () => {
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw) {
    rawResults.value = []
    hasSearched.value = false
    isDropdownOpen.value = false
    return
  }

  isSearching.value = true
  hasSearched.value = true
  isDropdownOpen.value = true

  const filterMock = (list: UserSummary[]) =>
    list.filter((u) => {
      const email = (u.email || '').toLowerCase()
      const name = (u.fullName || '').toLowerCase()
      const account = email.split('@')[0] || ''
      return email.includes(kw) || name.includes(kw) || account.includes(kw)
    })

  try {
    const res = await userApi.searchUsers(kw)
    if (res.success && Array.isArray(res.data) && res.data.length > 0) {
      rawResults.value = res.data
    } else {
      // Fallback tìm trong mock data nếu backend chưa có user tương ứng
      rawResults.value = filterMock(MOCK_EMPLOYEES)
    }
  } catch (err) {
    // Fallback tìm trong mock
    rawResults.value = filterMock(MOCK_EMPLOYEES)
  } finally {
    isSearching.value = false
  }
}

const addEmployee = (user: UserSummary) => {
  if (props.disabled) return

  const email = user.email || ''
  const emailPrefix = email.includes('@') ? (email.split('@')[0] || '') : ''
  const fullName = user.fullName || emailPrefix || 'Nhân viên'

  const newEmp: SelectedEmployee = {
    id: user.id,
    fullName,
    email,
    account: extractAccount(user),
    role: user.role || 'Nhân viên',
    departmentId: (user as any).departmentId,
  }

  selectedEmployees.value = [...selectedEmployees.value, newEmp]
  searchKeyword.value = ''
  rawResults.value = []
  hasSearched.value = false
  isDropdownOpen.value = false
}

const removeEmployee = (index: number) => {
  if (props.disabled) return
  const updated = [...selectedEmployees.value]
  updated.splice(index, 1)
  selectedEmployees.value = updated
}

const getAvatarInitial = (name?: string, email?: string): string => {
  if (name && name.trim()) {
    const parts = name.trim().split(/\s+/)
    const last = parts[parts.length - 1]
    if (last) return last.charAt(0).toUpperCase()
  }
  return email ? email.charAt(0).toUpperCase() : 'U'
}
</script>

<template>
  <div class="employee-list-field-wrapper" :class="{ 'is-disabled': disabled }">
    <!-- SEARCH & PICK BAR (Ẩn khi ở chế độ readonly) -->
    <div v-if="!disabled" class="search-employee-container">
      <div class="search-input-group">
        <div class="search-input-box">
          <svg class="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
          <input
            v-model="searchKeyword"
            type="text"
            class="search-emp-input"
            :placeholder="placeholder"
            @input="handleSearch"
            @keydown.enter.prevent="handleSearch"
          />
          <button
            v-if="searchKeyword"
            type="button"
            class="btn-clear-kw"
            title="Xóa từ khóa"
            @click="searchKeyword = ''; isDropdownOpen = false; hasSearched = false"
          >
            ✕
          </button>
        </div>

        <button
          type="button"
          class="btn-search-trigger"
          :disabled="isSearching"
          @click="handleSearch"
        >
          <span v-if="isSearching" class="spinner-sm"></span>
          <span v-else>Tìm nhân viên</span>
        </button>
      </div>

      <div class="field-sub-note">
        <span>ℹ️ Nhập email (vd: <code>an.nv@company.com</code>) hoặc account (vd: <code>an.nv</code>) để thêm nhân viên vào danh sách.</span>
      </div>

      <!-- SEARCH RESULTS DROPDOWN -->
      <div v-if="isDropdownOpen && hasSearched" class="search-results-dropdown">
        <div class="dropdown-header">
          <span>Kết quả tìm kiếm (Chỉ hiển thị nhân viên)</span>
          <button type="button" class="btn-close-dropdown" @click="isDropdownOpen = false">✕</button>
        </div>

        <div v-if="filteredResults.length > 0" class="results-list-scroll">
          <div
            v-for="user in filteredResults"
            :key="user.id"
            class="emp-result-card"
            @click="addEmployee(user)"
          >
            <div class="emp-avatar">
              {{ getAvatarInitial(user.fullName, user.email) }}
            </div>

            <div class="emp-info">
              <div class="emp-name-line">
                <span class="emp-name">{{ user.fullName || user.email }}</span>
                <span class="emp-role-tag">{{ user.role || 'Nhân viên' }}</span>
              </div>
              <div class="emp-meta-line">
                <span class="emp-email">{{ user.email }}</span>
                <span class="emp-meta-dot">•</span>
                <span class="emp-account">Account: <code>{{ extractAccount(user) }}</code></span>
              </div>
            </div>

            <button type="button" class="btn-select-emp" @click.stop="addEmployee(user)">
              + Chọn
            </button>
          </div>
        </div>

        <div v-else class="no-emp-found">
          <span class="no-emp-icon">👤</span>
          <span>Không tìm thấy nhân viên nào khớp với "<strong>{{ searchKeyword }}</strong>".</span>
          <small>Lưu ý: Quản trị viên (Admin) không được hiển thị trong danh sách nhân viên.</small>
        </div>
      </div>
    </div>

    <!-- SELECTED EMPLOYEES LIST -->
    <div class="selected-employees-panel">
      <div class="selected-panel-header">
        <div class="sph-left">
          <span class="sph-icon">👥</span>
          <span class="sph-title">Danh sách nhân viên đã chọn</span>
        </div>
        <span class="sph-badge">
          <strong>{{ selectedEmployees.length }}</strong> nhân sự
        </span>
      </div>

      <div v-if="selectedEmployees.length > 0" class="selected-cards-list">
        <div
          v-for="(emp, idx) in selectedEmployees"
          :key="emp.id"
          class="selected-emp-row"
        >
          <span class="row-index">#{{ idx + 1 }}</span>

          <div class="selected-avatar">
            {{ getAvatarInitial(emp.fullName, emp.email) }}
          </div>

          <div class="selected-main-info">
            <div class="smi-top">
              <span class="selected-name">{{ emp.fullName }}</span>
              <span class="selected-role-badge">{{ emp.role || 'Nhân viên' }}</span>
            </div>
            <div class="smi-bottom">
              <span class="selected-email" title="Email nhân sự">✉️ {{ emp.email }}</span>
              <span class="selected-account" title="Account hệ thống">👤 <code>{{ emp.account }}</code></span>
            </div>
          </div>

          <button
            v-if="!disabled"
            type="button"
            class="btn-remove-emp"
            title="Xóa nhân viên này khỏi danh sách"
            @click="removeEmployee(idx)"
          >
            ✕
          </button>
        </div>
      </div>

      <div v-else class="empty-selection-box">
        <span class="empty-icon">📂</span>
        <span class="empty-text">Chưa có nhân viên nào được chọn vào danh sách.</span>
        <small v-if="!disabled">Tìm kiếm theo email hoặc account ở khung trên để thêm nhân viên.</small>
      </div>
    </div>
  </div>
</template>

<style scoped>
.employee-list-field-wrapper {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
  background: #ffffff;
  border: 1.5px solid #e2e8f0;
  border-radius: 12px;
  padding: 1rem;
  position: relative;
  transition: all 0.2s ease;
}

.employee-list-field-wrapper:focus-within {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.employee-list-field-wrapper.is-disabled {
  background: #f8fafc;
  border-color: #e2e8f0;
}

/* SEARCH BAR */
.search-employee-container {
  position: relative;
}

.search-input-group {
  display: flex;
  gap: 0.5rem;
}

.search-input-box {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
  background: #f8fafc;
  border: 1.5px solid #cbd5e1;
  border-radius: 8px;
  padding: 0.55rem 0.875rem;
  transition: all 0.2s ease;
}

.search-input-box:focus-within {
  background: #ffffff;
  border-color: #3b82f6;
  box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.15);
}

.search-icon {
  color: #94a3b8;
  flex-shrink: 0;
}

.search-emp-input {
  border: none;
  background: transparent;
  outline: none;
  width: 100%;
  font-size: 0.84rem;
  color: #1e293b;
}

.btn-clear-kw {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 0.2rem;
  font-size: 0.875rem;
}

.btn-clear-kw:hover {
  color: #ef4444;
}

.btn-search-trigger {
  background: #0066f5;
  color: #ffffff;
  border: none;
  padding: 0 1.1rem;
  border-radius: 8px;
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 0.4rem;
  white-space: nowrap;
  transition: all 0.2s ease;
}

.btn-search-trigger:hover:not(:disabled) {
  background: #0052cc;
}

.spinner-sm {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.field-sub-note {
  font-size: 0.72rem;
  color: #64748b;
  margin-top: 0.35rem;
}

.field-sub-note code {
  background: #f1f5f9;
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
  color: #0369a1;
  font-family: monospace;
}

/* DROPDOWN RESULTS */
.search-results-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.15), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
  z-index: 50;
  overflow: hidden;
  max-height: 320px;
  display: flex;
  flex-direction: column;
}

.dropdown-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.625rem 0.875rem;
  background: #f1f5f9;
  border-bottom: 1px solid #e2e8f0;
  font-size: 0.75rem;
  font-weight: 700;
  color: #475569;
}

.btn-close-dropdown {
  background: none;
  border: none;
  color: #64748b;
  cursor: pointer;
  font-size: 0.875rem;
  padding: 0.2rem;
}

.results-list-scroll {
  overflow-y: auto;
  padding: 0.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.emp-result-card {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.6rem 0.75rem;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s ease;
  border: 1px solid transparent;
}

.emp-result-card:hover {
  background: #eff6ff;
  border-color: #bfdbfe;
}

.emp-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, #0066f5, #0284c7);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
  font-weight: 700;
  flex-shrink: 0;
}

.emp-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.emp-name-line {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.emp-name {
  font-size: 0.84rem;
  font-weight: 700;
  color: #0f172a;
}

.emp-role-tag {
  font-size: 0.6875rem;
  background: #f1f5f9;
  color: #475569;
  padding: 0.1rem 0.45rem;
  border-radius: 4px;
  font-weight: 600;
}

.emp-meta-line {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.75rem;
  color: #64748b;
}

.emp-meta-dot {
  color: #cbd5e1;
}

.emp-account code {
  font-family: monospace;
  color: #0284c7;
}

.btn-select-emp {
  background: #e0f2fe;
  color: #0369a1;
  border: 1px solid #bae6fd;
  border-radius: 6px;
  padding: 0.35rem 0.65rem;
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-select-emp:hover {
  background: #0284c7;
  color: #ffffff;
  border-color: #0284c7;
}

.no-emp-found {
  padding: 2rem 1rem;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.4rem;
  color: #64748b;
  font-size: 0.8125rem;
}

.no-emp-icon {
  font-size: 1.75rem;
  margin-bottom: 0.25rem;
}

.no-emp-found small {
  font-size: 0.72rem;
  color: #94a3b8;
}

/* SELECTED EMPLOYEES PANEL */
.selected-employees-panel {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.75rem;
}

.selected-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.625rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px dashed #e2e8f0;
}

.sph-left {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.sph-icon {
  font-size: 1rem;
}

.sph-title {
  font-size: 0.8125rem;
  font-weight: 700;
  color: #334155;
}

.sph-badge {
  font-size: 0.72rem;
  background: #e0f2fe;
  color: #0284c7;
  padding: 0.15rem 0.5rem;
  border-radius: 9999px;
  font-weight: 600;
}

.selected-cards-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.selected-emp-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.6rem 0.85rem;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
  transition: all 0.2s ease;
}

.selected-emp-row:hover {
  border-color: #cbd5e1;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.05);
}

.row-index {
  font-size: 0.72rem;
  font-weight: 700;
  color: #94a3b8;
  width: 20px;
}

.selected-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #eff6ff;
  color: #0066f5;
  border: 1px solid #bfdbfe;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8125rem;
  font-weight: 700;
  flex-shrink: 0;
}

.selected-main-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.smi-top {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.selected-name {
  font-size: 0.84rem;
  font-weight: 700;
  color: #0f172a;
}

.selected-role-badge {
  font-size: 0.6875rem;
  background: #ecfdf5;
  color: #059669;
  border: 1px solid #a7f3d0;
  padding: 0.08rem 0.4rem;
  border-radius: 4px;
  font-weight: 600;
}

.smi-bottom {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.75rem;
  color: #64748b;
  flex-wrap: wrap;
}

.selected-account code {
  font-family: monospace;
  color: #0284c7;
  background: #f1f5f9;
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
}

.btn-remove-emp {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 0.35rem 0.5rem;
  border-radius: 6px;
  font-size: 0.875rem;
  transition: all 0.2s ease;
}

.btn-remove-emp:hover {
  background: #fee2e2;
  color: #dc2626;
}

.empty-selection-box {
  padding: 1.5rem 1rem;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.3rem;
  color: #64748b;
}

.empty-icon {
  font-size: 1.75rem;
  margin-bottom: 0.2rem;
}

.empty-text {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #475569;
}

.empty-selection-box small {
  font-size: 0.72rem;
  color: #94a3b8;
}
</style>
