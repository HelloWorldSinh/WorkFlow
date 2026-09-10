<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useFormStore } from '@/stores/formStore'
import FormBuilderModal from '@/components/form/FormBuilderModal.vue'
import FormPreviewModal from '@/components/form/FormPreviewModal.vue'
import TicketSimulationModal from '@/components/form/TicketSimulationModal.vue'
import type { FormItem } from '@/types/form'

const formStore = useFormStore()

// State quản lý menu tác vụ 3 chấm (... More Actions)
const activeMenuId = ref<string | number | null>(null)

const toggleMenu = (id: string | number, e: MouseEvent) => {
  e.stopPropagation()
  activeMenuId.value = activeMenuId.value === id ? null : id
}

const closeMenu = () => {
  activeMenuId.value = null
}

onMounted(() => {
  formStore.fetchForms()
  window.addEventListener('click', closeMenu)
})

onUnmounted(() => {
  window.removeEventListener('click', closeMenu)
})

const searchQuery = ref('')

const filteredForms = computed(() => {
  if (!searchQuery.value.trim()) return formStore.forms
  const q = searchQuery.value.toLowerCase()
  return formStore.forms.filter(
    (f) =>
      f.name.toLowerCase().includes(q) ||
      (f.description && f.description.toLowerCase().includes(q))
  )
})

const handleCreateNew = () => {
  formStore.openCreateModal()
}

const handleEdit = (form: FormItem) => {
  formStore.openEditModal(form)
}

const handlePreview = (form: FormItem) => {
  formStore.openPreviewModal(form)
}

const handleSimulate = (form: FormItem) => {
  formStore.openSimulationModal(form)
}

const handleDuplicate = (form: FormItem) => {
  formStore.duplicateForm(form)
}

const handleDelete = (id: number | string, name: string) => {
  if (confirm(`Bạn có chắc muốn xóa biểu mẫu "${name}"?`)) {
    formStore.deleteForm(id)
  }
}
</script>

<template>
  <div class="forms-management-view">
    <!-- TOP HEADER -->
    <header class="forms-header">
      <div class="header-left">
        <div class="breadcrumb">
          <router-link to="/" class="breadcrumb-link">Workflows</router-link>
          <span class="breadcrumb-separator">/</span>
          <span class="breadcrumb-current">Thư viện Biểu mẫu</span>
        </div>
        <h1 class="page-title">Quản lý Biểu mẫu Tập trung</h1>
        <p class="page-subtitle">
          Thiết kế và quản lý các biểu mẫu dùng chung (Start Form, Approval Form) để gắn vào các bước trong quy trình Workflow.
        </p>
      </div>

      <div class="header-actions">
        <button class="btn btn-primary" @click="handleCreateNew">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          <span>Tạo mới Biểu mẫu</span>
        </button>
      </div>
    </header>

    <!-- SEARCH & FILTER TOOLBAR -->
    <div class="forms-filter-bar">
      <div class="search-box">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8"></circle>
          <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
        </svg>
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Tìm kiếm biểu mẫu theo tên hoặc nội dung..."
          class="search-input"
        />
        <button v-if="searchQuery" class="btn-clear-search" @click="searchQuery = ''">✕</button>
      </div>

      <div class="filter-stats">
        Hiển thị <strong>{{ filteredForms.length }}</strong> biểu mẫu
      </div>
    </div>

    <!-- FORMS GRID -->
    <div v-if="filteredForms.length > 0" class="forms-grid">
      <div
        v-for="form in filteredForms"
        :key="form.id"
        class="form-card"
        :class="{ 'menu-open': activeMenuId === String(form.id) }"
      >
        <div class="form-card-header">
          <div class="form-icon-pill">
            📋
          </div>
        </div>

        <div class="form-card-body">
          <h3 class="form-title" :title="form.name">{{ form.name }}</h3>
          <p class="form-description">
            {{ form.description || 'Không có mô tả chi tiết cho biểu mẫu này.' }}
          </p>
        </div>

        <!-- ACTIONS -->
        <div class="form-card-footer">
          <div class="footer-left-actions">
            <!-- PREVIEW -->
            <button
              type="button"
              class="card-btn btn-view"
              title="Xem trước giao diện biểu mẫu"
              @click="handlePreview(form)"
            >
              👁️ Xem trước
            </button>

            <!-- SIMULATE TICKET -->
            <button
              type="button"
              class="card-btn btn-sim"
              title="Chạy thử tạo ticket từ biểu mẫu này"
              @click="handleSimulate(form)"
            >
              🚀 Chạy thử Ticket
            </button>
          </div>

          <div class="footer-right-actions">
            <!-- CHỈNH SỬA (Tác vụ chính) -->
            <button
              type="button"
              class="card-icon-btn"
              title="Chỉnh sửa biểu mẫu"
              @click="handleEdit(form)"
            >
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
              </svg>
            </button>

            <!-- MENU 3 CHẤM (... MORE ACTIONS) -->
            <div class="menu-dropdown-wrapper">
              <button
                type="button"
                class="card-icon-btn"
                :class="{ 'is-active': activeMenuId === String(form.id) }"
                title="Tác vụ khác"
                @click="toggleMenu(String(form.id), $event)"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                  <circle cx="12" cy="5" r="2"></circle>
                  <circle cx="12" cy="12" r="2"></circle>
                  <circle cx="12" cy="19" r="2"></circle>
                </svg>
              </button>

              <!-- DROPDOWN OPTIONS -->
              <div
                v-if="activeMenuId === String(form.id)"
                class="card-dropdown-menu"
                @click.stop
              >
                <button
                  type="button"
                  class="card-dropdown-item"
                  @click="handleDuplicate(form); activeMenuId = null"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                    <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                  </svg>
                  <span>Nhân bản biểu mẫu</span>
                </button>

                <div class="card-dropdown-divider"></div>

                <button
                  type="button"
                  class="card-dropdown-item item-danger"
                  @click="handleDelete(form.id, form.name); activeMenuId = null"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="3 6 5 6 21 6"></polyline>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                    <line x1="10" y1="11" x2="10" y2="17"></line>
                    <line x1="14" y1="11" x2="14" y2="17"></line>
                  </svg>
                  <span>Xóa biểu mẫu</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- EMPTY STATE -->
    <div v-else class="empty-state">
      <div class="empty-icon">📂</div>
      <h3>Không tìm thấy biểu mẫu nào</h3>
      <p>Thử tìm kiếm với từ khóa khác hoặc tạo một biểu mẫu mới.</p>
      <button class="btn btn-primary" @click="handleCreateNew">
        + Tạo mới Biểu mẫu
      </button>
    </div>

    <!-- MODALS -->
    <FormBuilderModal />
    <FormPreviewModal />
    <TicketSimulationModal />
  </div>
</template>

<style scoped>
.forms-management-view {
  max-width: 1440px;
  margin: 0 auto;
  padding: 2rem 1.5rem;
  min-height: 100vh;
  font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* HEADER */
.forms-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid #e2e8f0;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8125rem;
  color: #64748b;
  margin-bottom: 0.25rem;
}

.breadcrumb-link {
  color: #64748b;
  text-decoration: none;
  font-weight: 500;
}

.breadcrumb-link:hover {
  color: var(--primary);
}

.breadcrumb-separator {
  color: #cbd5e1;
}

.breadcrumb-current {
  color: var(--primary);
  font-weight: 600;
}

.page-title {
  font-size: 1.75rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: #0f172a;
  margin: 0 0 0.25rem 0;
}

.page-subtitle {
  font-size: 0.84rem;
  color: #64748b;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.625rem 1.125rem;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-primary {
  background: var(--accent-gradient);
  color: #ffffff;
  border: none;
  box-shadow: 0 2px 8px rgba(2, 132, 199, 0.35);
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(2, 132, 199, 0.5);
}

/* FILTER BAR */
.forms-filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
  gap: 1rem;
  flex-wrap: wrap;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 0.55rem 0.875rem;
  width: 100%;
  max-width: 440px;
  color: #64748b;
  transition: all 0.2s ease;
}

.search-box:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--border-glow);
}

.search-input {
  border: none;
  outline: none;
  width: 100%;
  font-size: 0.84rem;
  color: #1e293b;
}

.btn-clear-search {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  font-size: 0.875rem;
}

.filter-stats {
  font-size: 0.8125rem;
  color: #64748b;
}

/* FORMS GRID */
.forms-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 1.5rem;
}

.form-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.03);
  transition: all 0.25s ease;
  position: relative;
}

.form-card.menu-open {
  z-index: 40;
  border-color: #cbd5e1;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.08);
}

.form-card:hover {
  transform: translateY(-2px);
  border-color: #cbd5e1;
  box-shadow: 0 10px 20px -5px rgba(0, 0, 0, 0.08);
}

.form-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 1.25rem 0.75rem 1.25rem;
}

.form-icon-pill {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: var(--primary-light);
  color: var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.field-count-badge {
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--primary);
  background: var(--primary-light);
  padding: 0.2rem 0.6rem;
  border-radius: 9999px;
}

.usage-count-tag {
  font-size: 0.72rem;
  font-weight: 600;
  color: #059669;
  background: #ecfdf5;
  padding: 0.2rem 0.6rem;
  border-radius: 9999px;
}

.form-card-body {
  flex: 1;
  padding: 0 1.25rem 1rem 1.25rem;
  display: flex;
  flex-direction: column;
}

.form-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 0.35rem 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.form-description {
  font-size: 0.8125rem;
  color: #64748b;
  margin: 0 0 1rem 0;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 2.35rem;
}

/* FOOTER */
.form-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.875rem 1.25rem;
  background: #f8fafc;
  border-top: 1px solid #e2e8f0;
}

.footer-left-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.card-btn {
  border: none;
  font-size: 0.75rem;
  font-weight: 600;
  padding: 0.35rem 0.65rem;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-view {
  background: #ffffff;
  color: #475569;
  border: 1px solid #cbd5e1;
}

.btn-view:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.btn-sim {
  background: #ecfdf5;
  color: #059669;
  border: 1px solid #a7f3d0;
}

.btn-sim:hover {
  background: #d1fae5;
}

.footer-right-actions {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.card-icon-btn {
  background: none;
  border: none;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #64748b;
  font-size: 0.84rem;
  transition: background 0.2s ease;
}

.card-icon-btn:hover {
  background: #e2e8f0;
  color: #0f172a;
}

.card-icon-btn.is-active {
  background: #e2e8f0;
  color: #0f172a;
}

.card-icon-btn.btn-del:hover {
  background: #fee2e2;
  color: #dc2626;
}

/* MENU DROPDOWN */
.menu-dropdown-wrapper {
  position: relative;
}

.card-dropdown-menu {
  position: absolute;
  right: 0;
  bottom: calc(100% + 8px);
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.12), 0 8px 10px -6px rgba(0, 0, 0, 0.08);
  min-width: 180px;
  padding: 0.375rem;
  z-index: 70;
  display: flex;
  flex-direction: column;
  gap: 2px;
  animation: menuFadeIn 0.15s ease-out;
}

@keyframes menuFadeIn {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.card-dropdown-item {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.5rem 0.75rem;
  font-size: 0.8125rem;
  font-weight: 500;
  color: #334155;
  background: transparent;
  border: none;
  border-radius: 6px;
  text-align: left;
  width: 100%;
  cursor: pointer;
  transition: all 0.15s ease;
}

.card-dropdown-item:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.card-dropdown-divider {
  height: 1px;
  background: #e2e8f0;
  margin: 0.25rem 0;
}

.card-dropdown-item.item-danger {
  color: #dc2626;
}

.card-dropdown-item.item-danger:hover {
  background: #fee2e2;
  color: #b91c1c;
}

/* EMPTY STATE */
.empty-state {
  text-align: center;
  padding: 4rem 2rem;
  background: #ffffff;
  border-radius: 16px;
  border: 1px dashed #cbd5e1;
  max-width: 500px;
  margin: 3rem auto;
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.empty-state h3 {
  font-size: 1.25rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 0.5rem 0;
}

.empty-state p {
  font-size: 0.875rem;
  color: #64748b;
  margin: 0 0 1.5rem 0;
}
</style>
