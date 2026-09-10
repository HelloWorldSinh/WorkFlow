<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useWorkflowStore } from '@/stores/workflowStore'
import { userApi, type UserSummary } from '@/services/workflowApi'
import type { WorkflowModule, WorkflowType, WorkflowTemplate } from '@/types/workflow'
import Modal from '@/components/common/Modal.vue'

const router = useRouter()
const store = useWorkflowStore()

interface Props {
  initialTab?: 'blank' | 'template'
}

const props = withDefaults(defineProps<Props>(), {
  initialTab: 'blank',
})

const activeTab = ref<'blank' | 'template'>(props.initialTab)

watch(
  () => props.initialTab,
  (val) => {
    activeTab.value = val
  }
)

const form = reactive({
  name: '',
  description: '',
  module: 'HR' as WorkflowModule,
  type: 'multi_approval' as WorkflowType,
  selectedTemplateId: '',
})

const errors = reactive({
  name: '',
  owner: '',
})

// Owner search & selection state
const selectedOwner = ref<UserSummary | null>(null)
const searchKeyword = ref('')
const isSearching = ref(false)
const hasSearched = ref(false)
const searchResults = ref<UserSummary[]>([])

// Tự động load thông tin user đang đăng nhập làm owner mặc định ban đầu
const initDefaultOwner = async () => {
  try {
    const authUserStr = localStorage.getItem('auth_user')
    if (authUserStr) {
      const authUser = JSON.parse(authUserStr)
      if (authUser) {
        selectedOwner.value = {
          id: authUser.id || 1,
          email: authUser.email || 'admin@company.com',
          fullName: authUser.fullName || 'Sinh Nguyen',
          role: authUser.role || 'Admin',
          departmentId: authUser.departmentId || 1,
          isActive: true,
        }
        return
      }
    }
  } catch (e) {
    // fallback
  }

  // Fallback mặc định user id 1
  selectedOwner.value = {
    id: 1,
    email: 'admin@company.com',
    fullName: 'Sinh Nguyen (System Admin)',
    role: 'Admin',
    departmentId: 1,
    isActive: true,
  }
}

onMounted(() => {
  initDefaultOwner()
})

const handleSearchUsers = async () => {
  const keyword = searchKeyword.value.trim()
  if (!keyword) {
    errors.owner = 'Vui lòng nhập tên hoặc email cần tìm kiếm'
    searchResults.value = []
    hasSearched.value = false
    return
  }

  isSearching.value = true
  hasSearched.value = true
  errors.owner = ''

  try {
    const response = await userApi.searchUsers(keyword)
    if (response.success && response.data) {
      searchResults.value = response.data
    } else {
      searchResults.value = []
    }
  } catch (err: any) {
    console.error('Lỗi tìm kiếm user:', err)
    searchResults.value = []
  } finally {
    isSearching.value = false
  }
}

const handleSelectOwner = (user: UserSummary) => {
  selectedOwner.value = user
  searchResults.value = []
  searchKeyword.value = ''
  hasSearched.value = false
  errors.owner = ''
}

const handleClearOwner = () => {
  selectedOwner.value = null
  searchKeyword.value = ''
  searchResults.value = []
  hasSearched.value = false
}

const selectTemplate = (template: WorkflowTemplate) => {
  form.name = template.title
  form.description = template.description
  form.module = template.module
  form.type = template.type
  form.selectedTemplateId = template.id
  activeTab.value = 'blank'
}

const handleClose = () => {
  store.isCreateModalOpen = false
  // Reset form
  form.name = ''
  form.description = ''
  form.module = 'HR'
  form.type = 'multi_approval'
  form.selectedTemplateId = ''
  errors.name = ''
  errors.owner = ''
  searchKeyword.value = ''
  searchResults.value = []
  hasSearched.value = false
  initDefaultOwner()
}

const handleSubmit = async () => {
  if (!form.name.trim()) {
    errors.name = 'Vui lòng nhập tên quy trình'
    return
  }
  errors.name = ''

  if (!selectedOwner.value?.id) {
    errors.owner = 'Vui lòng gán người sở hữu quy trình'
    return
  }
  errors.owner = ''

  try {
    const newWorkflow = await store.createWorkflow({
      name: form.name.trim(),
      description: form.description.trim(),
      module: form.module,
      type: form.type,
      templateId: form.selectedTemplateId || undefined,
      ownerId: selectedOwner.value.id,
    })
    const templateIdToPass = form.selectedTemplateId
    handleClose()

    if (newWorkflow?.id) {
      router.push({
        path: `/workflows/${newWorkflow.id}/editor`,
        query: {
          name: newWorkflow.name,
          ...(templateIdToPass ? { templateId: templateIdToPass } : {}),
        },
      })
    }
  } catch (e) {
    // Error is handled in store with toast
  }
}
</script>

<template>
  <Modal
    :is-open="store.isCreateModalOpen"
    title="Khởi tạo Workflow mới"
    max-width="680px"
    @close="handleClose"
  >
    <!-- Tab Navigation -->
    <div class="tabs-nav">
      <button
        class="tab-btn"
        :class="{ active: activeTab === 'blank' }"
        @click="activeTab = 'blank'"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 20h9"></path>
          <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path>
        </svg>
        <span>Bắt đầu từ đầu</span>
      </button>

      <button
        class="tab-btn"
        :class="{ active: activeTab === 'template' }"
        @click="activeTab = 'template'"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
          <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
        </svg>
        <span>Chọn từ Mẫu có sẵn</span>
      </button>
    </div>

    <!-- Tab 1: Blank Canvas Form -->
    <div v-if="activeTab === 'blank'" class="form-container">
      <div v-if="form.selectedTemplateId" class="selected-template-alert">
        <span>Đang áp dụng mẫu: <strong>{{ form.name }}</strong></span>
        <button class="btn-clear-tpl" @click="form.selectedTemplateId = ''">Bỏ mẫu</button>
      </div>

      <!-- Workflow Name -->
      <div class="form-group">
        <label class="form-label required">Tên Workflow</label>
        <input
          v-model="form.name"
          type="text"
          placeholder="Ví dụ: Quy trình phê duyệt mua sắm thiết bị IT"
          class="form-control"
          :class="{ 'is-invalid': !!errors.name }"
        />
        <span v-if="errors.name" class="error-text">{{ errors.name }}</span>
      </div>

      <!-- Description -->
      <div class="form-group">
        <label class="form-label">Mô tả mục đích</label>
        <textarea
          v-model="form.description"
          rows="2"
          placeholder="Mô tả phạm vi áp dụng, tiêu chí hoặc các bước nghiệp vụ chính..."
          class="form-control"
        ></textarea>
      </div>

      <!-- Owner Search & Assignment Section -->
      <div class="form-group owner-assignment-section">
        <label class="form-label required">Người sở hữu quy trình (Owner)</label>

        <!-- Trạng thái 1: Đã chọn người sở hữu -->
        <div v-if="selectedOwner" class="selected-owner-card">
          <div class="owner-avatar-circle">
            {{ selectedOwner.fullName ? selectedOwner.fullName.charAt(0).toUpperCase() : (selectedOwner.email ? selectedOwner.email.charAt(0).toUpperCase() : 'U') }}
          </div>
          <div class="owner-card-info">
            <div class="owner-name-row">
              <span class="owner-full-name">{{ selectedOwner.fullName || selectedOwner.email }}</span>
              <span class="owner-role-tag">{{ selectedOwner.role || 'Workflow Owner' }}</span>
            </div>
            <span class="owner-email-meta">{{ selectedOwner.email }}</span>
          </div>
          <button type="button" class="btn-change-owner" @click="handleClearOwner">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
            </svg>
            <span>Đổi người khác</span>
          </button>
        </div>

        <!-- Trạng thái 2: Chưa chọn hoặc đang tìm kiếm để gán -->
        <div v-else class="owner-search-box">
          <div class="search-input-group">
            <div class="search-input-wrapper">
              <svg class="search-icon-left" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
              <input
                v-model="searchKeyword"
                type="text"
                class="form-control search-owner-input"
                placeholder="Nhập tên hoặc email người muốn gán..."
                @keydown.enter.prevent="handleSearchUsers"
              />
            </div>
            <button
              type="button"
              class="btn-search-user"
              :disabled="isSearching"
              @click="handleSearchUsers"
            >
              <span v-if="isSearching" class="btn-spinner-sm"></span>
              <svg v-else width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
              <span>Tìm kiếm</span>
            </button>
          </div>
          <span v-if="errors.owner" class="error-text">{{ errors.owner }}</span>

          <!-- Dropdown/List Kết quả tìm kiếm từ DB -->
          <div v-if="hasSearched" class="search-results-panel">
            <div v-if="searchResults.length > 0" class="results-list">
              <div
                v-for="user in searchResults"
                :key="user.id"
                class="user-result-item"
                @click="handleSelectOwner(user)"
              >
                <div class="result-avatar">
                  {{ user.fullName ? user.fullName.charAt(0).toUpperCase() : (user.email ? user.email.charAt(0).toUpperCase() : 'U') }}
                </div>
                <div class="result-details">
                  <div class="result-name-row">
                    <span class="result-full-name">{{ user.fullName || user.email }}</span>
                    <span class="result-role-badge">{{ user.role }}</span>
                  </div>
                  <span class="result-sub-meta">{{ user.email }}</span>
                </div>
                <button type="button" class="btn-select-user">
                  Chọn
                </button>
              </div>
            </div>

            <!-- Trường hợp không tìm thấy -->
            <div v-else class="no-result-box">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="1.8">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
              <span>Không tìm thấy người dùng nào khớp với từ khóa "<strong>{{ searchKeyword }}</strong>" trong CSDL.</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Tab 2: Template Library -->
    <div v-else class="templates-grid">
      <div
        v-for="tpl in store.templates"
        :key="tpl.id"
        class="template-card"
        @click="selectTemplate(tpl)"
      >
        <div class="tpl-icon">{{ tpl.icon }}</div>
        <div class="tpl-content">
          <h4 class="tpl-title">{{ tpl.title }}</h4>
          <p class="tpl-desc">{{ tpl.description }}</p>
          <div class="tpl-meta">
            <span class="tpl-module">{{ tpl.module }}</span>
            <span class="tpl-steps">{{ tpl.estimatedSteps }} bước chuẩn</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal Footer -->
    <template #footer>
      <button class="btn btn-secondary" :disabled="store.isSubmitting" @click="handleClose">Hủy bỏ</button>
      <button
        v-if="activeTab === 'blank'"
        class="btn btn-primary"
        :disabled="store.isSubmitting"
        @click="handleSubmit"
      >
        {{ store.isSubmitting ? 'Đang tạo...' : 'Tạo & Thiết kế luồng' }}
      </button>
    </template>
  </Modal>
</template>

<style scoped>
.tabs-nav {
  display: flex;
  background: #f1f5f9;
  padding: 4px;
  border-radius: var(--radius-md);
  margin-bottom: 1.25rem;
  border: 1px solid var(--border-color);
}

.tab-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.625rem;
  border-radius: var(--radius-sm);
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--text-secondary);
}

.tab-btn.active {
  background: #ffffff;
  color: var(--primary);
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.form-container {
  display: flex;
  flex-direction: column;
  gap: 1.125rem;
}

.selected-template-alert {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #eef2ff;
  border: 1px solid #c7d2fe;
  padding: 0.625rem 0.875rem;
  border-radius: var(--radius-md);
  font-size: 0.8125rem;
  color: #4338ca;
}

.btn-clear-tpl {
  color: #dc2626;
  font-size: 0.75rem;
  text-decoration: underline;
  font-weight: 600;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.form-label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--text-secondary);
}

.form-label.required::after {
  content: ' *';
  color: #ef4444;
}

.form-control {
  width: 100%;
}

.form-control.is-invalid {
  border-color: #ef4444;
}

.error-text {
  font-size: 0.75rem;
  color: #ef4444;
}

/* ==========================================================
   OWNER SEARCH & ASSIGNMENT STYLES
   ========================================================== */
.owner-assignment-section {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: var(--radius-md);
  padding: 0.875rem;
}

/* Selected Owner Card */
.selected-owner-card {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  padding: 0.75rem 1rem;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.owner-avatar-circle {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: linear-gradient(135deg, #0284c7 0%, #0369a1 100%);
  color: #ffffff;
  font-weight: 700;
  font-size: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.owner-card-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.owner-name-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.owner-full-name {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--text-primary);
}

.owner-role-tag {
  font-size: 0.6875rem;
  font-weight: 600;
  padding: 1px 6px;
  background: #eef2ff;
  color: var(--primary);
  border-radius: 4px;
  border: 1px solid #c7d2fe;
}

.owner-email-meta {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.btn-change-owner {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--primary);
  background: #eef2ff;
  border: 1px solid #c7d2fe;
  padding: 0.375rem 0.625rem;
  border-radius: var(--radius-sm);
  transition: all 0.2s ease;
}

.btn-change-owner:hover {
  background: #e0e7ff;
  color: var(--primary-hover);
}

/* Owner Search Input Box */
.owner-search-box {
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
}

.search-input-group {
  display: flex;
  gap: 0.5rem;
  align-items: center;
}

.search-input-wrapper {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
}

.search-icon-left {
  position: absolute;
  left: 0.75rem;
  color: #94a3b8;
  pointer-events: none;
}

.search-owner-input {
  padding-left: 2.25rem !important;
}

.btn-search-user {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  height: 38px;
  padding: 0 1rem;
  background: var(--primary);
  color: #ffffff;
  font-size: 0.8125rem;
  font-weight: 600;
  border-radius: var(--radius-md);
  transition: all 0.2s ease;
  white-space: nowrap;
}

.btn-search-user:hover:not(:disabled) {
  background: var(--primary-hover);
}

.btn-search-user:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-spinner-sm {
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

/* Search Results Panel */
.search-results-panel {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: var(--radius-md);
  max-height: 220px;
  overflow-y: auto;
  box-shadow: var(--shadow-md);
}

.results-list {
  display: flex;
  flex-direction: column;
}

.user-result-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.625rem 0.875rem;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
  transition: background 0.15s ease;
}

.user-result-item:last-child {
  border-bottom: none;
}

.user-result-item:hover {
  background: #f8faff;
}

.result-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e2e8f0;
  color: #475569;
  font-weight: 700;
  font-size: 0.875rem;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.result-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.result-name-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.result-full-name {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.result-role-badge {
  font-size: 0.625rem;
  font-weight: 600;
  padding: 1px 5px;
  background: #f1f5f9;
  color: #475569;
  border-radius: 3px;
}

.result-sub-meta {
  font-size: 0.6875rem;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.btn-select-user {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--primary);
  background: #eef2ff;
  border: 1px solid #c7d2fe;
  padding: 0.25rem 0.625rem;
  border-radius: var(--radius-sm);
  transition: all 0.15s ease;
}

.user-result-item:hover .btn-select-user {
  background: var(--primary);
  color: #ffffff;
  border-color: var(--primary);
}

.no-result-box {
  padding: 1.5rem;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8125rem;
  color: var(--text-muted);
}

/* Templates grid */
.templates-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0.75rem;
}

.template-card {
  display: flex;
  gap: 1rem;
  padding: 1rem;
  background: #ffffff;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: var(--transition);
  box-shadow: var(--shadow-sm);
}

.template-card:hover {
  border-color: var(--primary);
  background: #f8faff;
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.tpl-icon {
  font-size: 2rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tpl-content {
  flex: 1;
}

.tpl-title {
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 0.25rem;
}

.tpl-desc {
  font-size: 0.8125rem;
  color: var(--text-secondary);
  line-height: 1.35;
  margin-bottom: 0.5rem;
}

.tpl-meta {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.tpl-module {
  font-size: 0.6875rem;
  font-weight: 600;
  padding: 2px 8px;
  background: #eef2ff;
  color: var(--primary);
  border-radius: var(--radius-full);
}

.tpl-steps {
  font-size: 0.75rem;
  color: var(--text-muted);
}

/* Buttons */
.btn {
  padding: 0.625rem 1.125rem;
  border-radius: var(--radius-md);
  font-size: 0.875rem;
  font-weight: 600;
}

.btn-primary {
  background: var(--accent-gradient);
  color: #ffffff;
}

.btn-secondary {
  background: #ffffff;
  color: var(--text-secondary);
  border: 1px solid var(--border-color);
}

.btn-secondary:hover {
  background: var(--bg-card-hover);
  color: var(--text-primary);
}
</style>
