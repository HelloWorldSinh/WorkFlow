<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/services/workflowApi'

const router = useRouter()

// Form State
const form = reactive({
  email: '',
  password: '',
})

const showPassword = ref(false)
const isLoading = ref(false)
const errorMessage = ref('')

const handleSignIn = async () => {
  if (!form.email.trim() || !form.password.trim()) {
    errorMessage.value = 'Vui lòng nhập Email và Password'
    return
  }

  errorMessage.value = ''
  isLoading.value = true

  try {
    const response = await authApi.login({
      email: form.email.trim(),
      password: form.password,
    })

    if (response.success && response.data) {
      // Lưu thông tin người dùng vào localStorage
      localStorage.setItem('auth_user', JSON.stringify(response.data))
      if (response.data.token) {
        localStorage.setItem('auth_token', response.data.token)
      }
      router.push('/')
    } else {
      errorMessage.value = response.message || 'Đăng nhập thất bại'
    }
  } catch (err: any) {
    errorMessage.value = err?.message || 'Email hoặc mật khẩu không chính xác'
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div class="split-auth-container">
    <!-- LEFT SIDE: BLUE HERO WITH 2.5D ILLUSTRATION -->
    <div class="hero-blue-pane">
      <!-- Top Brand Logo -->
      <div class="brand-header">
        <div class="brand-logo-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path
              d="M6 3C4.34315 3 3 4.34315 3 6V14C3 18.9706 7.02944 23 12 23C16.9706 23 21 18.9706 21 14V6C21 4.34315 19.6569 3 18 3C16.3431 3 15 4.34315 15 6V14C15 15.6569 13.6569 17 12 17C10.3431 17 9 15.6569 9 14V6C9 4.34315 7.65685 3 6 3Z"
              fill="white"
            />
          </svg>
        </div>
        <span class="brand-name">WorkerBuilder</span>
      </div>

      <!-- Center Vector Illustration (Task Clipboard & Stylus) -->
      <div class="illustration-wrapper">
        <div class="illustration-art">
          <!-- Background Abstract Blobs -->
          <div class="bg-shape-blob blob-1"></div>
          <div class="bg-shape-blob blob-2"></div>
          <div class="bg-shape-circle circle-1"></div>
          <div class="bg-shape-circle circle-2"></div>
          <div class="cross-icon cross-1">+</div>
          <div class="cross-icon cross-2">+</div>

          <!-- Clipboard Main Board -->
          <div class="clipboard-board">
            <!-- Top Clip -->
            <div class="clipboard-top-clip"></div>

            <!-- Paper Content -->
            <div class="clipboard-paper">
              <!-- Item 1: Check -->
              <div class="check-item">
                <div class="check-mark">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#0066f5" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="20 6 9 17 4 12"></polyline>
                  </svg>
                </div>
                <div class="item-lines">
                  <div class="line line-blue"></div>
                  <div class="line line-light"></div>
                </div>
              </div>

              <!-- Item 2: Cross -->
              <div class="check-item">
                <div class="check-mark cross-mark">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#0066f5" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="18" y1="6" x2="6" y2="18"></line>
                    <line x1="6" y1="6" x2="18" y2="18"></line>
                  </svg>
                </div>
                <div class="item-lines">
                  <div class="line line-blue"></div>
                  <div class="line line-light"></div>
                </div>
              </div>

              <!-- Item 3: Check -->
              <div class="check-item">
                <div class="check-mark">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#0066f5" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="20 6 9 17 4 12"></polyline>
                  </svg>
                </div>
                <div class="item-lines">
                  <div class="line line-blue"></div>
                  <div class="line line-light"></div>
                </div>
              </div>
            </div>
          </div>

          <!-- Stylus Pens -->
          <div class="stylus-pen pen-primary">
            <div class="pen-cap"></div>
            <div class="pen-tip"></div>
          </div>
          <div class="stylus-pen pen-secondary">
            <div class="pen-cap"></div>
            <div class="pen-tip"></div>
          </div>
        </div>
      </div>

      <!-- Bottom Hero Text & Dots -->
      <div class="hero-bottom-text">
        <h1 class="welcome-heading">Welcome!</h1>
        <p class="welcome-subtext">
          Tối ưu hóa và tự động hóa toàn diện các quy trình làm việc, phê duyệt doanh nghiệp theo chuẩn quốc tế.
        </p>

      </div>
    </div>

    <!-- RIGHT SIDE: CLEAN WHITE LOGIN FORM -->
    <div class="form-white-pane">
      <div class="login-box-container">
        <!-- Title -->
        <div class="login-header">
          <h2 class="login-title">Log In</h2>
        </div>

        <!-- Form Elements -->
        <form class="main-login-form" @submit.prevent="handleSignIn">
          <!-- Error message if any -->
          <div v-if="errorMessage" class="error-banner">
            {{ errorMessage }}
          </div>

          <!-- Email Input Field -->
          <div class="clean-field-group">
            <input
              id="emailInput"
              v-model="form.email"
              type="email"
              class="clean-underline-input"
              placeholder="Email"
              autocomplete="email"
            />
            <div class="field-icon-right">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#a0aec0" stroke-width="1.8">
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
                <polyline points="22,6 12,13 2,6"></polyline>
              </svg>
            </div>
          </div>

          <!-- Password Input Field -->
          <div class="clean-field-group">
            <input
              id="passwordInput"
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              class="clean-underline-input"
              placeholder="Password"
              autocomplete="current-password"
            />
            <button
              type="button"
              class="field-icon-btn-right"
              :title="showPassword ? 'Ẩn mật khẩu' : 'Hiển thị mật khẩu'"
              @click="showPassword = !showPassword"
            >
              <!-- Eye Open (when password visible) -->
              <svg v-if="showPassword" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#0066f5" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                <circle cx="12" cy="12" r="3"></circle>
              </svg>
              <!-- Eye Slash / Lock icon (when password hidden) -->
              <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#a0aec0" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                <line x1="1" y1="1" x2="23" y2="23"></line>
              </svg>
            </button>
          </div>

          <!-- Button & Remember Password Row -->
          <div class="action-row">
            <button type="submit" class="blue-btn-signin" :disabled="isLoading">
              <span v-if="isLoading" class="btn-spinner"></span>
              <span v-else>Sign in</span>
            </button>

          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ==========================================================
   PAGE CONTAINER & FULLSCREEN SPLIT
   ========================================================== */
.split-auth-container {
  display: flex;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  background: #ffffff;
}

/* ==========================================================
   LEFT SIDE: BLUE HERO PANE (#0066f5)
   ========================================================== */
.hero-blue-pane {
  flex: 1.15;
  background: #0066f5;
  color: #ffffff;
  padding: 3.5rem 4.5rem;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  position: relative;
  overflow: hidden;
}

/* Brand Header */
.brand-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  z-index: 10;
}

.brand-logo-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-name {
  font-size: 1.375rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: #ffffff;
}

/* 2.5D Illustration Area */
.illustration-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 1.5rem 0;
  position: relative;
  z-index: 5;
}

.illustration-art {
  position: relative;
  width: 340px;
  height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Background Abstract Geometric Shapes */
.bg-shape-blob {
  position: absolute;
  background: #2a82fc;
  border-radius: 9999px;
  opacity: 0.7;
}

.blob-1 {
  width: 260px;
  height: 34px;
  bottom: 80px;
  left: 20px;
}

.blob-2 {
  width: 200px;
  height: 24px;
  bottom: 125px;
  left: -20px;
}

.bg-shape-circle {
  position: absolute;
  border-radius: 50%;
  background: #4696fd;
}

.circle-1 {
  width: 14px;
  height: 14px;
  right: 40px;
  bottom: 110px;
}

.circle-2 {
  width: 8px;
  height: 8px;
  left: 25px;
  top: 60px;
}

.cross-icon {
  position: absolute;
  color: #6ba8fe;
  font-size: 1.25rem;
  font-weight: bold;
}

.cross-1 {
  top: 30px;
  right: 50px;
}

.cross-2 {
  bottom: 40px;
  left: 30px;
}

/* Clipboard Board */
.clipboard-board {
  width: 165px;
  height: 210px;
  background: #194ba3;
  border-radius: 18px;
  transform: rotate(-10deg);
  position: relative;
  box-shadow: -15px 20px 30px rgba(0, 35, 110, 0.35);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding-bottom: 10px;
}

.clipboard-top-clip {
  position: absolute;
  top: -12px;
  width: 60px;
  height: 20px;
  background: #5097fa;
  border-radius: 6px;
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.2);
}

.clipboard-paper {
  width: 145px;
  height: 185px;
  background: #ffffff;
  border-radius: 12px;
  padding: 18px 12px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.check-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.check-mark {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.item-lines {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.line {
  height: 4px;
  border-radius: 4px;
}

.line-blue {
  width: 70%;
  background: #0066f5;
}

.line-light {
  width: 45%;
  background: #b9d8fd;
}

/* Stylus Pens */
.stylus-pen {
  position: absolute;
  width: 26px;
  height: 70px;
  border-radius: 8px 8px 4px 4px;
  transform: rotate(35deg);
  box-shadow: -5px 10px 15px rgba(0, 20, 80, 0.3);
}

.pen-primary {
  background: #00b4d8;
  bottom: 25px;
  left: 60px;
  z-index: 8;
}

.pen-secondary {
  background: #2086fc;
  bottom: 45px;
  left: 35px;
  z-index: 7;
}

.pen-cap {
  width: 100%;
  height: 16px;
  background: rgba(0, 0, 0, 0.15);
  border-radius: 8px 8px 0 0;
}

.pen-tip {
  position: absolute;
  bottom: -6px;
  left: 8px;
  width: 10px;
  height: 6px;
  background: #0f172a;
  clip-path: polygon(50% 100%, 0 0, 100% 0);
}

/* Bottom Hero Text */
.hero-bottom-text {
  z-index: 10;
  max-width: 460px;
}

.welcome-heading {
  font-size: 2.75rem;
  font-weight: 700;
  letter-spacing: -0.03em;
  margin-bottom: 0.75rem;
  color: #ffffff;
}

.welcome-subtext {
  font-size: 1rem;
  line-height: 1.6;
  color: #dbeafe;
  margin-bottom: 2rem;
  opacity: 0.95;
}

/* Carousel Dots */
.carousel-dots {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  border: 1.5px solid #ffffff;
  background: transparent;
  cursor: pointer;
  transition: all 0.2s ease;
}

.dot.active {
  background: #ffffff;
}

/* ==========================================================
   RIGHT SIDE: CLEAN WHITE LOGIN FORM
   ========================================================== */
.form-white-pane {
  flex: 1;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4rem;
}

.login-box-container {
  width: 100%;
  max-width: 380px;
}

/* Header */
.login-header {
  margin-bottom: 2.75rem;
}

.login-title {
  font-size: 2.5rem;
  font-weight: 600;
  color: #0066f5;
  letter-spacing: -0.03em;
  margin-bottom: 0.875rem;
}

.account-subtitle {
  font-size: 0.875rem;
  color: #718096;
  margin-bottom: 0.25rem;
}

.create-acc-link {
  color: #0066f5;
  font-weight: 600;
  text-decoration: none;
}

.create-acc-link:hover {
  text-decoration: underline;
}

.hint-subtitle {
  font-size: 0.8125rem;
  color: #a0aec0;
}

/* Form Styles */
.main-login-form {
  display: flex;
  flex-direction: column;
}

.error-banner {
  background: #fff5f5;
  border: 1px solid #fed7d7;
  color: #c53030;
  font-size: 0.8125rem;
  padding: 0.625rem 0.875rem;
  border-radius: 6px;
  margin-bottom: 1.5rem;
}

/* Clean Underline Input Groups */
.clean-field-group {
  position: relative;
  display: flex;
  align-items: center;
  margin-bottom: 2.25rem;
}

.clean-underline-input {
  width: 100% !important;
  border: none !important;
  border-bottom: 1px solid #e2e8f0 !important;
  border-radius: 0 !important;
  padding: 0.625rem 2rem 0.625rem 0 !important;
  font-size: 0.9375rem !important;
  color: #2d3748 !important;
  background: transparent !important;
  outline: none !important;
  box-shadow: none !important;
  transition: border-color 0.2s ease;
}

.clean-underline-input:focus {
  border-bottom-color: #0066f5 !important;
}

.clean-underline-input::placeholder {
  color: #a0aec0;
}

.field-icon-right {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.field-icon-btn-right {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.field-icon-btn-right:hover svg {
  stroke: #0066f5;
}

/* Button & Checkbox Action Row */
.action-row {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  margin-top: 0.75rem;
  margin-bottom: 2.5rem;
}

.blue-btn-signin {
  min-width: 120px;
  height: 42px;
  background: #0066f5;
  color: #ffffff;
  font-size: 0.9375rem;
  font-weight: 600;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 102, 245, 0.25);
  transition: all 0.2s ease;
}

.blue-btn-signin:hover:not(:disabled) {
  background: #0056d6;
  box-shadow: 0 6px 16px rgba(0, 102, 245, 0.35);
}

.blue-btn-signin:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Remember Password Checkbox */
.remember-checkbox-wrapper {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  user-select: none;
}

.remember-checkbox-wrapper input {
  display: none;
}

.custom-check-box {
  width: 16px;
  height: 16px;
  border: 1.5px solid #0066f5;
  border-radius: 3px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: transparent;
  transition: all 0.2s ease;
}

.remember-checkbox-wrapper input:checked ~ .custom-check-box {
  background: #0066f5;
  color: #ffffff;
}

.remember-label-text {
  font-size: 0.8125rem;
  color: #718096;
}

/* Forgot Password Centered */
.forgot-container {
  text-align: center;
}

.forgot-pwd-link {
  font-size: 0.875rem;
  color: #0066f5;
  font-weight: 500;
  text-decoration: none;
  transition: opacity 0.2s ease;
}

.forgot-pwd-link:hover {
  text-decoration: underline;
}

/* ==========================================================
   RESPONSIVE DESIGN
   ========================================================== */
@media (max-width: 960px) {
  .split-auth-container {
    flex-direction: column;
    height: auto;
    min-height: 100vh;
  }

  .hero-blue-pane {
    padding: 3rem 2rem;
    min-height: 380px;
  }

  .form-white-pane {
    padding: 3rem 2rem;
  }
}
</style>
