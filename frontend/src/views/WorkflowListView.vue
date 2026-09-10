<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useWorkflowStore } from '@/stores/workflowStore'
import type { WorkflowItem } from '@/types/workflow'

// Components
import WorkflowHeader from '@/components/workflow/WorkflowHeader.vue'
import WorkflowFilterBar from '@/components/workflow/WorkflowFilterBar.vue'
import WorkflowTable from '@/components/workflow/WorkflowTable.vue'
import CreateWorkflowModal from '@/components/workflow/CreateWorkflowModal.vue'
import DeleteConfirmModal from '@/components/workflow/DeleteConfirmModal.vue'
import VersionHistoryDrawer from '@/components/workflow/VersionHistoryDrawer.vue'
import Toast from '@/components/common/Toast.vue'

const router = useRouter()
const store = useWorkflowStore()

const createModalTab = ref<'blank' | 'template'>('blank')

onMounted(() => {
  store.fetchWorkflows()
})

const handleCreate = () => {
  createModalTab.value = 'blank'
  store.isCreateModalOpen = true
}

const handleOpenTemplates = () => {
  createModalTab.value = 'template'
  store.isCreateModalOpen = true
}

const handleEditWorkflow = (workflow: WorkflowItem) => {
  router.push({
    path: `/workflows/${workflow.id}/editor`,
    query: { name: workflow.name },
  })
}

const handleViewInstances = (workflow: WorkflowItem) => {
  router.push(`/tickets?workflowId=${workflow.id}`)
}
</script>

<template>
  <div class="workflow-dashboard-view">
    <!-- Top Header -->
    <WorkflowHeader
      @create="handleCreate"
      @open-templates="handleOpenTemplates"
    />

    <!-- Search & Filters -->
    <WorkflowFilterBar />

    <!-- Workflow Data Table -->
    <WorkflowTable
      @edit="handleEditWorkflow"
      @view-instances="handleViewInstances"
    />

    <!-- Modals & Drawers -->
    <CreateWorkflowModal :initial-tab="createModalTab" />
    <DeleteConfirmModal />
    <VersionHistoryDrawer />

    <!-- Global Toast -->
    <Toast :message="store.toastMessage" />
  </div>
</template>

<style scoped>
.workflow-dashboard-view {
  max-width: 1440px;
  margin: 0 auto;
  padding: 2rem 1.5rem;
  min-height: 100vh;
}
</style>
