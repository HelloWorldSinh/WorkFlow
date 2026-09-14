<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useWorkflowMappingStore } from '@/stores/workflowMappingStore'
import { useWorkflowStore } from '@/stores/workflowStore'
import { useFormStore } from '@/stores/formStore'
import { useWorkflowEditorStore } from '@/stores/workflowEditorStore'
import type {
  WorkflowVariable,
  VariableMappingItem,
  NodePermissionMatrix as MatrixType,
  FieldPermissionType,
} from '@/types/mapping'
import type { FormField } from '@/types/form'

import VariableMappingRow from '@/components/mapping/VariableMappingRow.vue'
import NodePermissionMatrix from '@/components/mapping/NodePermissionMatrix.vue'
import Toast from '@/components/common/Toast.vue'

const route = useRoute()
const router = useRouter()

const mappingStore = useWorkflowMappingStore()
const workflowStore = useWorkflowStore()
const formStore = useFormStore()
const editorStore = useWorkflowEditorStore()

// Local State
const localMappings = ref<VariableMappingItem[]>([])
const localPermissions = ref<MatrixType>({})
const toast = ref<{ text: string; type: 'success' | 'error' | 'info' } | null>(null)

// Simulation test form inputs
const simFormValues = ref<Record<string, any>>({})
const simResult = ref<ReturnType<typeof mappingStore.simulateExecution> | null>(null)

const showToast = (text: string, type: 'success' | 'error' | 'info' = 'success') => {
  toast.value = { text, type }
  setTimeout(() => {
    toast.value = null
  }, 3500)
}

// Initialize from route params or defaults
onMounted(async () => {
  if (workflowStore.workflows.length === 0) {
    await workflowStore.fetchWorkflows()
  }
  if (formStore.forms.length === 0) {
    await formStore.fetchForms()
  }

  // Set initial workflow ID
  const routeWfId = route.params.id as string || (route.query.workflowId as string)
  if (routeWfId) {
    mappingStore.selectedWorkflowId = routeWfId
  } else if (editorStore.workflowId) {
    mappingStore.selectedWorkflowId = editorStore.workflowId
  } else if (workflowStore.workflows.length > 0 && workflowStore.workflows[0]?.id) {
    mappingStore.selectedWorkflowId = workflowStore.workflows[0].id
  }

  // Set initial form ID
  const routeFormId = route.query.formId as string
  if (routeFormId) {
    mappingStore.selectedFormId = routeFormId
  } else {
    // Check if there is already an existing mapping
    const existing = mappingStore.currentMapping
    if (existing) {
      mappingStore.selectedFormId = existing.formId
    } else if (formStore.forms.length > 0 && formStore.forms[0]?.id) {
      mappingStore.selectedFormId = formStore.forms[0].id
    }
  }

  syncLocalState()
})

// Variables extracted from current workflow (Data Contract)
const workflowVariables = computed<WorkflowVariable[]>(() => {
  if (!mappingStore.selectedWorkflowId) return []
  return mappingStore.extractWorkflowVariables(mappingStore.selectedWorkflowId)
})

// Current Form Fields
const currentFormFields = computed<FormField[]>(() => {
  if (!mappingStore.currentForm) return []
  return mappingStore.currentForm.schema.fields || []
})

// Current Workflow Graph Nodes
const workflowNodes = computed(() => {
  if (!mappingStore.selectedWorkflowId) return []
  const { nodes } = mappingStore.getWorkflowGraph(mappingStore.selectedWorkflowId)
  return nodes
})

// Match Score
const currentScore = computed(() => {
  return mappingStore.calculateMatchScore(localMappings.value, workflowVariables.value)
})

// Sync local state when workflow or form changes
const syncLocalState = () => {
  if (!mappingStore.selectedWorkflowId || !mappingStore.selectedFormId) {
    localMappings.value = []
    localPermissions.value = {}
    return
  }

  const existing = mappingStore.mappings.find(
    (m) =>
      String(m.workflowId) === String(mappingStore.selectedWorkflowId) &&
      String(m.formId) === String(mappingStore.selectedFormId)
  )

  if (existing) {
    // Clone existing
    localMappings.value = JSON.parse(JSON.stringify(existing.variableMappings))
    localPermissions.value = JSON.parse(JSON.stringify(existing.nodePermissions))

    // Ensure all current variables are present in localMappings
    workflowVariables.value.forEach((v) => {
      const found = localMappings.value.find((m) => m.variableKey === v.key)
      if (!found) {
        localMappings.value.push({
          variableKey: v.key,
          variableLabel: v.label,
          variableDataType: v.dataType,
          formFieldKey: '',
          status: 'unmapped',
        })
      }
    })
  } else {
    // Run auto-match for initial state
    handleAutoMatch(false)
    localPermissions.value = mappingStore.generateDefaultPermissions(
      workflowNodes.value,
      currentFormFields.value
    )
  }

  // Reset simulation values with defaults
  simFormValues.value = {}
  currentFormFields.value.forEach((f) => {
    if (f.type === 'number') simFormValues.value[f.key] = f.defaultValue ?? 10000000
    else if (f.type === 'select' && f.options && f.options.length > 0)
      simFormValues.value[f.key] = f.options[0]?.value ?? ''
    else simFormValues.value[f.key] = f.defaultValue ?? ''
  })
}

// Watchers
watch(
  [() => mappingStore.selectedWorkflowId, () => mappingStore.selectedFormId],
  () => {
    syncLocalState()
  }
)

// Auto-match action
const handleAutoMatch = (notify = true) => {
  if (!currentFormFields.value.length || !workflowVariables.value.length) return
  localMappings.value = mappingStore.autoMatchVariables(
    workflowVariables.value,
    currentFormFields.value
  )
  if (notify) {
    const matchedCount = localMappings.value.filter((m) => m.status === 'matched').length
    showToast(
      `Đã tự động ghép thành công ${matchedCount}/${workflowVariables.value.length} biến quy trình!`,
      'success'
    )
  }
}

// Update single mapping item
const updateMappingField = (varKey: string, fieldKey: string) => {
  const item = localMappings.value.find((m) => m.variableKey === varKey)
  if (!item) return

  item.formFieldKey = fieldKey
  if (!fieldKey) {
    item.status = 'unmapped'
    item.formFieldLabel = ''
    item.formFieldType = undefined
    return
  }

  const field = currentFormFields.value.find((f) => f.key === fieldKey)
  if (field) {
    item.formFieldLabel = field.label
    item.formFieldType = field.type
    item.status = mappingStore.isTypeCompatible(item.variableDataType, field.type)
      ? 'matched'
      : 'type_mismatch'
  }
}

const updateFallbackValue = (varKey: string, val: any) => {
  const item = localMappings.value.find((m) => m.variableKey === varKey)
  if (item) {
    item.customFallbackValue = val
  }
}

// Matrix permission change
const updateNodePermission = (nodeId: string, fieldKey: string, val: FieldPermissionType) => {
  if (!localPermissions.value[nodeId]) {
    localPermissions.value[nodeId] = {}
  }
  localPermissions.value[nodeId]![fieldKey] = val
}

const applyPreset = (preset: 'start_only' | 'all_readonly' | 'all_editable') => {
  workflowNodes.value.forEach((node) => {
    if (!localPermissions.value[node.id]) localPermissions.value[node.id] = {}
    currentFormFields.value.forEach((field) => {
      if (preset === 'start_only') {
        localPermissions.value[node.id]![field.key] = node.type === 'start' ? 'editable' : 'readonly'
      } else if (preset === 'all_readonly') {
        localPermissions.value[node.id]![field.key] = 'readonly'
      } else if (preset === 'all_editable') {
        localPermissions.value[node.id]![field.key] = 'editable'
      }
    })
  })
  showToast('Đã áp dụng mẫu phân quyền!', 'info')
}

// Validate Mapping
const validateMapping = () => {
  const requiredVars = workflowVariables.value.filter((v) => v.isRequired)
  const missing: string[] = []
  const mismatches: string[] = []

  requiredVars.forEach((v) => {
    const m = localMappings.value.find((item) => item.variableKey === v.key)
    if (!m || !m.formFieldKey) {
      missing.push(v.label || v.key)
    } else if (m.status === 'type_mismatch') {
      mismatches.push(`${v.key} (${v.dataType}) ≠ ${m.formFieldLabel} (${m.formFieldType})`)
    }
  })

  if (missing.length > 0) {
    showToast(`Thiếu ánh xạ cho ${missing.length} biến bắt buộc: ${missing.join(', ')}`, 'error')
    return false
  }

  if (mismatches.length > 0) {
    showToast(`Có ${mismatches.length} biến lệch kiểu dữ liệu: ${mismatches.join(', ')}`, 'info')
  } else {
    showToast('Tất cả các biến điều kiện của Workflow đã được ánh xạ chính xác 100%!', 'success')
  }
  return true
}

// Save Mapping
const handleSave = () => {
  if (!mappingStore.selectedWorkflowId || !mappingStore.selectedFormId) {
    showToast('Vui lòng chọn Workflow và Form để lưu cấu hình!', 'error')
    return
  }

  const isOk = validateMapping()
  mappingStore.saveMapping(
    mappingStore.selectedWorkflowId,
    mappingStore.selectedFormId,
    localMappings.value,
    localPermissions.value
  )

  if (isOk) {
    showToast('Đã lưu cấu hình Match Workflow & Form thành công!', 'success')
  }
}

// Run simulation
const handleRunSimulation = () => {
  if (!mappingStore.selectedWorkflowId || !mappingStore.selectedFormId) return
  // Save temp mapping first for simulator to read
  mappingStore.saveMapping(
    mappingStore.selectedWorkflowId,
    mappingStore.selectedFormId,
    localMappings.value,
    localPermissions.value
  )

  simResult.value = mappingStore.simulateExecution(
    mappingStore.selectedWorkflowId,
    mappingStore.selectedFormId,
    simFormValues.value
  )
  showToast('Đã tính toán kết quả rẽ nhánh quy trình!', 'info')
}

// Navigation back or to editor
const goToWorkflowEditor = () => {
  if (mappingStore.selectedWorkflowId) {
    router.push({
      path: `/workflows/${mappingStore.selectedWorkflowId}/editor`,
      query: { name: mappingStore.currentWorkflow?.name },
    })
  } else {
    router.push('/')
  }
}
</script>

<template>
  <div class="match-view-page">
    <!-- TOP HEADER -->
    <header class="match-header">
      <div class="header-left">
        <button class="btn-back" title="Quay lại" @click="goToWorkflowEditor">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <line x1="19" y1="12" x2="5" y2="12"></line>
            <polyline points="12 19 5 12 12 5"></polyline>
          </svg>
        </button>

        <div class="header-titles">
          <div class="title-badge-row">
            <h1 class="page-title">Liên Kết Biểu Mẫu Vào Quy Trình</h1>
          </div>
        </div>
      </div>

      <!-- TOP ACTIONS -->
      <div class="header-actions">
        <button
          type="button"
          class="btn-action btn-auto-map"
          title="Tự động nhận diện và ghép các trường có tên tương đồng"
          @click="handleAutoMatch(true)"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
            <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon>
          </svg>
          <span>Tự Động Khớp</span>
        </button>

        <button
          type="button"
          class="btn-action btn-validate"
          title="Kiểm tra xem tất cả các biến điều kiện đã được gắn trường chưa"
          @click="validateMapping"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
            <polyline points="22 4 12 14.01 9 11.01"></polyline>
          </svg>
          <span>Kiểm Tra Hợp Lệ</span>
        </button>

        <button
          type="button"
          class="btn-action btn-save"
          :disabled="mappingStore.isSaving"
          @click="handleSave"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
            <polyline points="17 21 17 13 7 13 7 21"></polyline>
            <polyline points="7 3 7 8 15 8"></polyline>
          </svg>
          <span>Lưu Cấu Hình</span>
        </button>

        <button
          type="button"
          class="btn-action btn-editor-link"
          title="Mở Visual Canvas của Quy trình"
          @click="goToWorkflowEditor"
        >
          <span>Canvas Editor ↗</span>
        </button>
      </div>
    </header>

    <!-- SELECTORS BAR (WORKFLOW & FORM PAIRING) -->
    <div class="pairing-bar">
      <!-- Workflow Selection -->
      <div class="selector-card wf-selector-card">
        <div class="card-label-row">
          <span class="icon-label">⚡ QUY TRÌNH (WORKFLOW)</span>
          <span v-if="mappingStore.currentWorkflow" class="status-pill status-ready">
            {{ mappingStore.currentWorkflow.status }}
          </span>
        </div>
        <select
          v-model="mappingStore.selectedWorkflowId"
          class="selector-dropdown"
        >
          <option
            v-for="wf in workflowStore.workflows"
            :key="wf.id"
            :value="wf.id"
          >
            {{ wf.name }} (ID: {{ wf.id }})
          </option>
          <!-- Option for editor workflow if not in list -->
          <option
            v-if="editorStore.workflowId && !workflowStore.workflows.some(w => String(w.id) === String(editorStore.workflowId))"
            :value="editorStore.workflowId"
          >
            {{ editorStore.workflowName }} (Đang chỉnh sửa)
          </option>
        </select>
        <div class="selector-meta">
          <span>{{ workflowNodes.length }} Bước</span>
          <span>•</span>
          <span>{{ workflowVariables.length }} Biến điều kiện yêu cầu</span>
        </div>
      </div>

      <!-- MATCH SCORE INDICATOR -->
      <div class="match-score-card">
        <div class="score-circle" :class="{ 'score-100': currentScore === 100, 'score-mid': currentScore > 0 && currentScore < 100 }">
          <span class="score-number">{{ currentScore }}%</span>
        </div>
        <div class="score-info">
          <span class="score-label">Độ tương thích điều kiện</span>
          <span v-if="currentScore === 100" class="score-desc text-green">
            ✓ Sẵn sàng kích hoạt luồng
          </span>
          <span v-else class="score-desc text-amber">
            ⚠️ Còn biến điều kiện chưa map
          </span>
        </div>
      </div>

      <!-- Form Selection -->
      <div class="selector-card form-selector-card">
        <div class="card-label-row">
          <span class="icon-label">📋 BIỂU MẪU (FORM)</span>
          <router-link to="/forms" target="_blank" class="link-manage">
            Quản lý Form ↗
          </router-link>
        </div>
        <select
          v-model="mappingStore.selectedFormId"
          class="selector-dropdown"
        >
          <option
            v-for="f in formStore.forms"
            :key="f.id"
            :value="f.id"
          >
            {{ f.name }} ({{ f.schema?.fields?.length || 0 }} trường)
          </option>
        </select>
        <div class="selector-meta">
          <span>{{ currentFormFields.length }} Trường dữ liệu</span>
          <span>•</span>
          <span>Người tạo: {{ mappingStore.currentForm?.createdBy || 'System' }}</span>
        </div>
      </div>
    </div>

    <!-- MAIN TABS -->
    <div class="tabs-nav-bar">
      <button
        class="tab-btn"
        :class="{ active: mappingStore.activeTab === 'mapping' }"
        @click="mappingStore.activeTab = 'mapping'"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"></path>
          <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"></path>
        </svg>
        <span>Ánh Xạ Biến & Điều Kiện Rẽ Nhánh</span>
        <span class="tab-badge">{{ localMappings.filter(m => m.status === 'matched').length }}/{{ workflowVariables.length }}</span>
      </button>
    </div>

    <!-- TAB 1: DATA MAPPING CONTENT -->
    <section v-if="mappingStore.activeTab === 'mapping'" class="tab-content-pane">

      <!-- Mapping Rows Header -->
      <div class="mapping-table-header">
        <div class="col-head-title">Biến Quy Trình (Data Contract của Workflow)</div>
        <div class="col-head-title text-center">Trạng Thái Khớp</div>
        <div class="col-head-title">Trường Biểu Mẫu Tương Ứng (Form Field)</div>
      </div>

      <!-- Mapping Rows -->
      <div v-if="workflowVariables.length > 0" class="mapping-rows-list">
        <VariableMappingRow
          v-for="v in workflowVariables"
          :key="v.key"
          :variable="v"
          :mapping-item="localMappings.find(m => m.variableKey === v.key) || {
            variableKey: v.key,
            variableLabel: v.label,
            variableDataType: v.dataType,
            formFieldKey: '',
            status: 'unmapped'
          }"
          :form-fields="currentFormFields"
          @update:field="updateMappingField(v.key, $event)"
          @update:fallback="updateFallbackValue(v.key, $event)"
        />
      </div>

      <div v-else class="empty-state-box">
        <span class="empty-icon">🔀</span>
        <h3>Quy trình chưa có điều kiện rẽ nhánh nào</h3>
        <p>Tất cả các bước trong quy trình hiện tại chạy tuần tự hoặc chưa cấu hình Transition Rule.</p>
        <button class="btn-primary" @click="goToWorkflowEditor">
          Mở Canvas để thêm điều kiện
        </button>
      </div>
    </section>

    <!-- TAB 2: NODE PERMISSION MATRIX CONTENT -->
    <section v-if="mappingStore.activeTab === 'permissions'" class="tab-content-pane">
      <NodePermissionMatrix
        :nodes="workflowNodes"
        :form-fields="currentFormFields"
        :permissions="localPermissions"
        @update:permission="updateNodePermission"
        @apply:preset="applyPreset"
      />
    </section>

    <!-- TAB 3: LIVE SIMULATION CONTENT -->
    <section v-if="mappingStore.activeTab === 'simulation'" class="tab-content-pane">
      <div class="simulation-container">
        <!-- SIMULATION INPUT FORM -->
        <div class="sim-card sim-inputs-side">
          <div class="sim-card-header">
            <h3>1. Điền dữ liệu giả lập cho Form</h3>
            <p>Nhập các giá trị thử nghiệm để xem quy trình tính toán rẽ nhánh ra sao</p>
          </div>

          <div class="sim-fields-list">
            <div
              v-for="field in currentFormFields"
              :key="field.id"
              class="sim-field-item"
            >
              <label class="sim-label">
                <span>{{ field.label }}</span>
                <span class="sim-key"><code>{{ field.key }}</code> ({{ field.type }})</span>
              </label>

              <!-- Number input -->
              <input
                v-if="field.type === 'number'"
                v-model.number="simFormValues[field.key]"
                type="number"
                class="sim-input"
                :placeholder="field.placeholder || 'Nhập số...'"
              />

              <!-- Select input -->
              <select
                v-else-if="field.type === 'select'"
                v-model="simFormValues[field.key]"
                class="sim-input"
              >
                <option
                  v-for="opt in field.options || []"
                  :key="opt.value"
                  :value="opt.value"
                >
                  {{ opt.label }} ({{ opt.value }})
                </option>
              </select>

              <!-- Textarea -->
              <textarea
                v-else-if="field.type === 'textarea'"
                v-model="simFormValues[field.key]"
                class="sim-input sim-textarea"
                rows="2"
              ></textarea>

              <!-- Date -->
              <input
                v-else-if="field.type === 'date'"
                v-model="simFormValues[field.key]"
                type="date"
                class="sim-input"
              />

              <!-- Default Text -->
              <input
                v-else
                v-model="simFormValues[field.key]"
                type="text"
                class="sim-input"
                :placeholder="field.placeholder || 'Nhập dữ liệu...'"
              />
            </div>
          </div>

          <div class="sim-actions-bar">
            <button
              type="button"
              class="btn-run-sim"
              @click="handleRunSimulation"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <polygon points="5 3 19 12 5 21 5 3"></polygon>
              </svg>
              <span>Chạy Mô Phỏng Rẽ Nhánh</span>
            </button>
          </div>
        </div>

        <!-- SIMULATION OUTPUT / RESULTS -->
        <div class="sim-card sim-results-side">
          <div class="sim-card-header">
            <h3>2. Kết quả tính toán luồng</h3>
            <p>Đánh giá điều kiện Transitions và xác định bước tiếp theo</p>
          </div>

          <div v-if="simResult" class="sim-results-body">
            <!-- Evaluated Variables Table -->
            <div class="res-section">
              <h4 class="res-title">Biến Quy Trình nhận được:</h4>
              <div class="res-vars-tags">
                <div
                  v-for="(val, vKey) in simResult.evaluatedVariables"
                  :key="vKey"
                  class="res-var-tag"
                >
                  <span class="v-name">{{ vKey }}:</span>
                  <span class="v-val"><strong>{{ val }}</strong></span>
                </div>
              </div>
            </div>

            <!-- Transition evaluation passes -->
            <div class="res-section">
              <h4 class="res-title">Đánh giá các đường chuyển tiếp (Transitions):</h4>
              <div class="edges-evaluation-list">
                <div
                  v-for="edge in simResult.activeEdges"
                  :key="edge.edgeId"
                  class="edge-eval-card"
                  :class="{ 'eval-passed': edge.passed, 'eval-failed': !edge.passed }"
                >
                  <div class="eval-header">
                    <span class="eval-badge" :class="edge.passed ? 'badge-pass' : 'badge-fail'">
                      {{ edge.passed ? '✓ THỎA MÃN' : '✗ KHÔNG ĐẠT' }}
                    </span>
                    <span class="edge-name-text">{{ edge.label || 'Đường nối' }}</span>
                  </div>
                  <div class="eval-reason">{{ edge.reason }}</div>
                </div>
              </div>
            </div>

            <!-- Target Nodes activated -->
            <div class="res-section">
              <h4 class="res-title">Các bước sẽ được phân công xử lý tiếp theo:</h4>
              <div class="target-nodes-list">
                <div
                  v-for="target in simResult.targetNodes"
                  :key="target.nodeId"
                  class="target-node-card"
                >
                  <span class="target-icon">🚀</span>
                  <div class="target-info">
                    <div class="target-name">{{ target.nodeName }}</div>
                    <div class="target-type">Loại bước: {{ target.nodeType }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-else class="sim-placeholder">
            <span class="placeholder-icon">⚡</span>
            <p>Nhấp <strong>"Chạy Mô Phỏng Rẽ Nhánh"</strong> để xem kết quả đánh giá các nhánh điều kiện.</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Global Toast -->
    <Toast :message="toast" />
  </div>
</template>

<style scoped>
.match-view-page {
  max-width: 1440px;
  margin: 0 auto;
  padding: 1.5rem 2rem 3rem 2rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  color: #0f172a;
}

/* ==========================================================
   TOP HEADER
   ========================================================== */
.match-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.5rem;
  flex-wrap: wrap;
  padding-bottom: 1rem;
  border-bottom: 1px solid #e2e8f0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 1.25rem;
}

.btn-back {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: #ffffff;
  border: 1.5px solid #cbd5e1;
  color: #334155;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-back:hover {
  background: #f1f5f9;
  border-color: #0284c7;
  color: #0284c7;
  transform: translateX(-2px);
}

.header-titles {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.title-badge-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
  letter-spacing: -0.5px;
}

.decoupled-badge {
  font-size: 0.6875rem;
  font-weight: 700;
  background: linear-gradient(135deg, #0284c7, #0369a1);
  color: #ffffff;
  padding: 3px 8px;
  border-radius: 9999px;
  letter-spacing: 0.5px;
}

.page-desc {
  font-size: 0.875rem;
  color: #64748b;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.625rem;
}

.btn-action {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  height: 40px;
  padding: 0 1rem;
  border-radius: 8px;
  font-size: 0.8125rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-auto-map {
  background: #f0fdf4;
  border: 1.5px solid #86efac;
  color: #166534;
}

.btn-auto-map:hover {
  background: #dcfce7;
  border-color: #22c55e;
  transform: translateY(-1px);
}

.btn-validate {
  background: #f8fafc;
  border: 1.5px solid #cbd5e1;
  color: #334155;
}

.btn-validate:hover {
  background: #f1f5f9;
  border-color: #94a3b8;
}

.btn-save {
  background: linear-gradient(135deg, #0284c7, #0369a1);
  border: none;
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(2, 132, 199, 0.25);
}

.btn-save:hover {
  background: linear-gradient(135deg, #0369a1, #075985);
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(2, 132, 199, 0.35);
}

.btn-editor-link {
  background: transparent;
  border: 1px solid #cbd5e1;
  color: #0284c7;
}

.btn-editor-link:hover {
  background: #e0f2fe;
  border-color: #0284c7;
}

/* ==========================================================
   PAIRING BAR
   ========================================================== */
.pairing-bar {
  display: grid;
  grid-template-columns: 1fr 200px 1fr;
  align-items: center;
  gap: 1.25rem;
}

.selector-card {
  background: #ffffff;
  border: 1.5px solid #cbd5e1;
  border-radius: 12px;
  padding: 1rem 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.03);
}

.wf-selector-card {
  border-top: 4px solid #0284c7;
}

.form-selector-card {
  border-top: 4px solid #10b981;
}

.card-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.icon-label {
  font-size: 0.6875rem;
  font-weight: 800;
  color: #475569;
  letter-spacing: 0.5px;
}

.status-pill {
  font-size: 0.6875rem;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
}

.status-ready {
  background: #ecfdf5;
  color: #059669;
}

.link-manage {
  font-size: 0.75rem;
  font-weight: 600;
  color: #0284c7;
  text-decoration: none;
}

.selector-dropdown {
  height: 42px;
  border-radius: 8px;
  border: 1.5px solid #cbd5e1;
  padding: 0 0.75rem;
  font-size: 0.9375rem;
  font-weight: 700;
  color: #0f172a;
  background: #ffffff;
  outline: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.selector-dropdown:focus {
  border-color: #0284c7;
  box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.15);
}

.selector-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  color: #64748b;
  font-weight: 500;
}

/* MATCH SCORE CARD */
.match-score-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 0.875rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: 0.5rem;
}

.score-circle {
  width: 58px;
  height: 58px;
  border-radius: 50%;
  border: 4px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 1rem;
  color: #475569;
  transition: all 0.3s ease;
}

.score-circle.score-100 {
  border-color: #10b981;
  color: #059669;
  background: #ecfdf5;
}

.score-circle.score-mid {
  border-color: #f59e0b;
  color: #d97706;
  background: #fffbeb;
}

.score-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.score-label {
  font-size: 0.6875rem;
  font-weight: 700;
  color: #475569;
}

.score-desc {
  font-size: 0.6875rem;
  font-weight: 600;
}

.text-green {
  color: #059669;
}

.text-amber {
  color: #d97706;
}

/* ==========================================================
   TABS NAVIGATION
   ========================================================== */
.tabs-nav-bar {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  border-bottom: 2px solid #e2e8f0;
}

.tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  background: transparent;
  border: none;
  font-size: 0.875rem;
  font-weight: 700;
  color: #64748b;
  cursor: pointer;
  position: relative;
  transition: all 0.2s ease;
}

.tab-btn:hover {
  color: #0284c7;
}

.tab-btn.active {
  color: #0284c7;
}

.tab-btn.active::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  right: 0;
  height: 2px;
  background: #0284c7;
}

.tab-badge {
  font-size: 0.6875rem;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 9999px;
  background: #f1f5f9;
  color: #475569;
}

.tab-btn.active .tab-badge {
  background: #e0f2fe;
  color: #0284c7;
}

/* ==========================================================
   TAB 1: MAPPING
   ========================================================== */
.tab-content-pane {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.banner-explain {
  display: flex;
  align-items: flex-start;
  gap: 0.875rem;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 10px;
  padding: 0.875rem 1.25rem;
  font-size: 0.8125rem;
  color: #1e40af;
  line-height: 1.5;
}

.banner-icon {
  font-size: 1.25rem;
}

.banner-body code {
  font-family: 'JetBrains Mono', monospace;
  background: #dbeafe;
  padding: 2px 5px;
  border-radius: 4px;
  font-weight: 700;
}

.mapping-table-header {
  display: grid;
  grid-template-columns: 1fr 140px 1fr;
  gap: 1.25rem;
  padding: 0.5rem 1.5rem;
  font-size: 0.75rem;
  font-weight: 800;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.mapping-rows-list {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}

.empty-state-box {
  background: #ffffff;
  border: 1px dashed #cbd5e1;
  border-radius: 12px;
  padding: 3rem;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
}

.empty-icon {
  font-size: 2.5rem;
}

.btn-primary {
  margin-top: 0.5rem;
  padding: 0.5rem 1.25rem;
  border-radius: 8px;
  background: #0284c7;
  color: #ffffff;
  border: none;
  font-weight: 700;
  cursor: pointer;
}

/* ==========================================================
   TAB 3: SIMULATION
   ========================================================== */
.simulation-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
}

.sim-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.02);
}

.sim-card-header h3 {
  font-size: 1.125rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.sim-card-header p {
  font-size: 0.8125rem;
  color: #64748b;
  margin: 0.25rem 0 0 0;
}

.sim-fields-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  max-height: 520px;
  overflow-y: auto;
  padding-right: 0.5rem;
}

.sim-field-item {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.sim-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.8125rem;
  font-weight: 600;
  color: #334155;
}

.sim-key code {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.6875rem;
  color: #0369a1;
}

.sim-input {
  height: 38px;
  border: 1.5px solid #cbd5e1;
  border-radius: 6px;
  padding: 0 0.75rem;
  font-size: 0.875rem;
  outline: none;
  transition: all 0.2s ease;
}

.sim-input:focus {
  border-color: #0284c7;
  box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.15);
}

.sim-textarea {
  height: auto;
  padding: 0.5rem 0.75rem;
}

.btn-run-sim {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  width: 100%;
  height: 44px;
  border-radius: 8px;
  background: linear-gradient(135deg, #0284c7, #0369a1);
  color: #ffffff;
  border: none;
  font-size: 0.875rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(2, 132, 199, 0.25);
  transition: all 0.2s ease;
}

.btn-run-sim:hover {
  background: linear-gradient(135deg, #0369a1, #075985);
  transform: translateY(-1px);
}

.sim-results-body {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.res-section {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.res-title {
  font-size: 0.8125rem;
  font-weight: 700;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin: 0;
}

.res-vars-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.res-var-tag {
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 0.75rem;
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.res-var-tag .v-name {
  color: #64748b;
}

.res-var-tag .v-val {
  color: #0f172a;
}

.edges-evaluation-list {
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
}

.edge-eval-card {
  padding: 0.75rem 1rem;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.edge-eval-card.eval-passed {
  background: #f0fdf4;
  border-color: #86efac;
}

.edge-eval-card.eval-failed {
  background: #fef2f2;
  border-color: #fecaca;
}

.eval-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.eval-badge {
  font-size: 0.625rem;
  font-weight: 800;
  padding: 2px 6px;
  border-radius: 4px;
}

.badge-pass {
  background: #10b981;
  color: #ffffff;
}

.badge-fail {
  background: #ef4444;
  color: #ffffff;
}

.edge-name-text {
  font-size: 0.8125rem;
  font-weight: 700;
  color: #1e293b;
}

.eval-reason {
  font-size: 0.75rem;
  color: #475569;
}

.target-nodes-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.target-node-card {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  background: #eff6ff;
  border: 1.5px solid #93c5fd;
  border-radius: 8px;
  padding: 0.75rem 1rem;
}

.target-icon {
  font-size: 1.5rem;
}

.target-name {
  font-size: 0.875rem;
  font-weight: 700;
  color: #1e3a8a;
}

.target-type {
  font-size: 0.6875rem;
  color: #3b82f6;
  font-weight: 600;
}

.sim-placeholder {
  height: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  text-align: center;
  color: #64748b;
}

.placeholder-icon {
  font-size: 2.5rem;
}

@media (max-width: 1024px) {
  .pairing-bar {
    grid-template-columns: 1fr;
  }
  .simulation-container {
    grid-template-columns: 1fr;
  }
}
</style>
