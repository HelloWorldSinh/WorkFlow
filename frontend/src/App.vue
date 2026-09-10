<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter, RouterView } from 'vue-router'
import { useTicketStore } from '@/stores/ticketStore'

const route = useRoute()
const router = useRouter()
const ticketStore = useTicketStore()

interface CurrentUser {
  id: number
  email: string
  fullName?: string
  role?: string
  departmentId?: number
  avatar?: string
}

const currentUser = ref<CurrentUser | null>(null)

const loadCurrentUser = () => {
  try {
    const authUserStr = localStorage.getItem('auth_user')
    if (authUserStr) {
      currentUser.value = JSON.parse(authUserStr)
    }
  } catch (e) {
    currentUser.value = null
  }
}

onMounted(() => {
  loadCurrentUser()
  ticketStore.fetchStats()
})

// Tự động cập nhật lại user profile và stats khi đổi route
watch(
  () => route.path,
  () => {
    loadCurrentUser()
    ticketStore.fetchStats()
  }
)

const handleLogout = () => {
  localStorage.removeItem('auth_user')
  localStorage.removeItem('auth_token')
  currentUser.value = null
  router.push('/login')
}
</script>

<template>
  <div class="app-layout">
    <!-- Top Navigation Bar (Hidden on Auth and Editor Layouts) -->
    <header v-if="route.meta.layout !== 'auth' && route.meta.layout !== 'editor'" class="app-navbar">
      <div class="navbar-container">
        <!-- Logo & Brand -->
        <div class="brand-group" @click="router.push('/')" style="cursor: pointer;">
          <div class="brand-logo">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon>
            </svg>
          </div>
          <div class="brand-info">
            <span class="brand-title">WorkerBuilder</span>
          </div>
        </div>

        <!-- Navigation Links -->
        <nav class="nav-links">
          <router-link to="/" class="nav-item" :class="{ active: route.path === '/' }">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="7" height="7"></rect>
              <rect x="14" y="3" width="7" height="7"></rect>
              <rect x="14" y="14" width="7" height="7"></rect>
              <rect x="3" y="14" width="7" height="7"></rect>
            </svg>
            <span>Quy trình</span>
          </router-link>
          <router-link to="/forms" class="nav-item" :class="{ active: route.path === '/forms' }">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
              <polyline points="14 2 14 8 20 8"></polyline>
              <line x1="16" y1="13" x2="8" y2="13"></line>
              <line x1="16" y1="17" x2="8" y2="17"></line>
              <polyline points="10 9 9 9 8 9"></polyline>
            </svg>
            <span>Biểu mẫu</span>
          </router-link>
          <router-link to="/tickets" class="nav-item" :class="{ active: route.path.startsWith('/tickets') }">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline>
            </svg>
            <span>Giám sát</span>
          </router-link>
          <router-link to="/my-tasks" class="nav-item" :class="{ active: route.path === '/my-tasks' }">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
              <polyline points="22 4 12 14.01 9 11.01"></polyline>
            </svg>
            <span>Duyệt việc</span>
            <span v-if="ticketStore.stats.pendingMyTasks > 0" class="nav-badge-pill">
              {{ ticketStore.stats.pendingMyTasks }}
            </span>
          </router-link>
        </nav>

        <!-- Right User Profile -->
        <div class="nav-user-group">
          <button class="nav-icon-btn" title="Thông báo hệ thống">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
              <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
            </svg>
            <span class="notification-badge"></span>
          </button>

          <!-- Current Logged In User Profile -->
          <div class="user-profile-badge">
            <div class="user-avatar-circle">
              {{ (currentUser?.fullName || currentUser?.email || 'U').charAt(0).toUpperCase() }}
            </div>
            <div class="user-meta">
              <span class="user-name">{{ currentUser?.fullName || currentUser?.email || 'Chưa đăng nhập' }}</span>
              <span class="user-role">{{ currentUser?.role || 'Thành viên' }}</span>
            </div>
            <button class="btn-logout" title="Đăng xuất khỏi hệ thống" @click="handleLogout">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                <polyline points="16 17 21 12 16 7"></polyline>
                <line x1="21" y1="12" x2="9" y2="12"></line>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </header>

    <!-- Main Content View -->
    <main class="app-main" :class="{ 'auth-main': route.meta.layout === 'auth' }">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--bg-app);
}

.app-navbar {
  background: #ffffff;
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: var(--shadow-sm);
}

.navbar-container {
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 1.5rem;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.5rem;
}

.brand-group {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  text-decoration: none;
}

.brand-logo {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, #0284c7 0%, #0369a1 50%, #075985 100%);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(2, 132, 199, 0.35);
}

.brand-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.brand-title {
  font-size: 1.125rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--text-primary);
}

.brand-tag {
  font-size: 0.625rem;
  font-weight: 700;
  padding: 2px 6px;
  background: #e0f2fe;
  color: #0284c7;
  border-radius: var(--radius-sm);
  letter-spacing: 0.08em;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 0.375rem;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.875rem;
  border-radius: var(--radius-md);
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-secondary);
  text-decoration: none;
  transition: var(--transition);
}

.nav-item:hover {
  color: #0284c7;
  background: #f0f9ff;
}

.nav-item.active {
  color: #0284c7;
  background: #e0f2fe;
  font-weight: 600;
}

.nav-badge-pill {
  background: #ef4444;
  color: #ffffff;
  font-size: 0.6875rem;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 9999px;
  margin-left: 2px;
}

.nav-user-group {
  display: flex;
  align-items: center;
  gap: 0.875rem;
}

.nav-icon-btn {
  width: 38px;
  height: 38px;
  border-radius: var(--radius-md);
  background: #ffffff;
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  transition: var(--transition);
}

.nav-icon-btn:hover {
  color: #0284c7;
  background: #f0f9ff;
  border-color: #7dd3fc;
  box-shadow: 0 2px 6px rgba(2, 132, 199, 0.15);
}

.notification-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 7px;
  height: 7px;
  background: #ef4444;
  border-radius: 50%;
  border: 1.5px solid #ffffff;
}

.user-profile-badge {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.25rem 0.625rem;
  border-radius: var(--radius-md);
  background: #ffffff;
  border: 1px solid var(--border-color);
  transition: var(--transition);
}

.user-profile-badge:hover {
  border-color: #7dd3fc;
  background: #f0f9ff;
}

.user-avatar-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #0284c7 0%, #0369a1 100%);
  color: #ffffff;
  font-size: 0.875rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(2, 132, 199, 0.3);
}

.user-meta {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.2;
}

.user-role {
  font-size: 0.6875rem;
  color: var(--text-muted);
  line-height: 1.2;
}

.btn-logout {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  color: var(--text-muted);
  border-radius: var(--radius-sm);
  margin-left: 0.25rem;
  transition: var(--transition);
}

.btn-logout:hover {
  color: var(--status-danger);
  background: var(--status-danger-bg);
}

.app-main {
  flex: 1;
}

.auth-main {
  display: flex;
  min-height: 100vh;
}
</style>
