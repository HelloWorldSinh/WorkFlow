<script setup lang="ts">
interface Props {
  message: {
    text: string
    type: 'success' | 'error' | 'info'
  } | null
}

defineProps<Props>()
</script>

<template>
  <Teleport to="body">
    <Transition name="toast-slide">
      <div v-if="message" :class="['toast-box', `toast-${message.type}`]">
        <span class="toast-icon">
          <svg v-if="message.type === 'success'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <polyline points="20 6 9 17 4 12"></polyline>
          </svg>
          <svg v-else-if="message.type === 'error'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
          </svg>
          <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="16" x2="12" y2="12"></line>
            <line x1="12" y1="8" x2="12.01" y2="8"></line>
          </svg>
        </span>
        <span class="toast-text">{{ message.text }}</span>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.toast-box {
  position: fixed;
  bottom: 2rem;
  right: 2rem;
  z-index: 1000;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.875rem 1.25rem;
  border-radius: var(--radius-lg);
  font-size: 0.875rem;
  font-weight: 500;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(12px);
}

.toast-success {
  background: rgba(16, 185, 129, 0.9);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.toast-error {
  background: rgba(239, 68, 68, 0.9);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.toast-info {
  background: rgba(99, 102, 241, 0.9);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.toast-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

/* Transitions */
.toast-slide-enter-active,
.toast-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.toast-slide-enter-from,
.toast-slide-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.95);
}
</style>
