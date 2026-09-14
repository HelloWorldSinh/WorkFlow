<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useWorkflowEditorStore } from '@/stores/workflowEditorStore'
import { useFormStore } from '@/stores/formStore'
import type { TransitionRule, ConditionOperator, ConditionDataType, ConditionMatchType } from '@/types/editor'

import { userApi, type UserSummary } from '@/services/workflowApi'

const editorStore = useWorkflowEditorStore()
const formStore = useFormStore()
const systemUsers = ref<UserSummary[]>([])


onMounted(async () => {
  if (formStore.forms.length === 0) {
    formStore.fetchForms()
  }
  try {
    const res = await userApi.getApprovers()
    if (res.success && res.data) {
      systemUsers.value = res.data
    }
  } catch (e) {
    try {
      const fallbackRes = await userApi.searchUsers()
      if (fallbackRes.success && fallbackRes.data) {
        systemUsers.value = fallbackRes.data.filter(u => isApproverRole(u.role))
      }
    } catch (err) {
      // ignore
    }
  }
})

const node = computed(() => editorStore.selectedNode)
const edge = computed(() => editorStore.selectedEdge)


const nodeTypeMeta = computed(() => {
  if (!node.value) return null
  return (
    editorStore.nodePalette.find((p) => p.type === node.value?.type) || {
      title: node.value?.type,
      color: '#6366f1',
      bg: '#eef2ff',
    }
  )
})

const handleDelete = () => {
  if (node.value) {
    editorStore.deleteNode(node.value.id)
  } else if (edge.value) {
    editorStore.deleteEdge(edge.value.id)
  }
}


// ==========================================================
// MULTI-APPROVER HELPERS
// ==========================================================
const userSearchFilter = ref('')

// Chỉ hiển thị các user có role: Admin, Approver (Approval), WorkflowOwner
const isApproverRole = (role?: string): boolean => {
  if (!role) return false
  const r = role.toLowerCase().replace(/[\s_-]+/g, '')
  return r === 'admin' || r === 'approver' || r === 'approval' || r === 'workflowowner'
}

const approverUsers = computed(() => {
  return systemUsers.value.filter((u) => isApproverRole(u.role))
})

const filteredUsers = computed(() => {
  const baseUsers = approverUsers.value
  if (!userSearchFilter.value.trim()) return baseUsers
  const kw = userSearchFilter.value.trim().toLowerCase()
  return baseUsers.filter(
    (u) =>
      u.fullName?.toLowerCase().includes(kw) ||
      u.email.toLowerCase().includes(kw) ||
      u.role?.toLowerCase().includes(kw)
  )
})

const isUserSelected = (userId: number) => {
  if (!node.value?.config) return false
  const ids = node.value.config.approverUserIds || []
  return ids.includes(userId)
}

const toggleUserSelection = (userId: number) => {
  if (!node.value?.config) return
  if (!node.value.config.approverUserIds) {
    node.value.config.approverUserIds = []
  }
  const idx = node.value.config.approverUserIds.indexOf(userId)
  if (idx >= 0) {
    node.value.config.approverUserIds.splice(idx, 1)
  } else {
    node.value.config.approverUserIds.push(userId)
  }
  node.value.config.approverUserNames = approverUsers.value
    .filter((u) => node.value?.config?.approverUserIds?.includes(u.id))
    .map((u) => u.fullName || u.email)

  editorStore.isDirty = true
}

const switchToSingleMode = () => {
  if (!node.value?.config) return
  node.value.config.approvalMode = 'single'
  editorStore.isDirty = true
}

const switchToMultiMode = () => {
  if (!node.value?.config) return
  node.value.config.approvalMode = 'multi'
  if (!node.value.config.multiApprovalRule) node.value.config.multiApprovalRule = 'all'
  node.value.config.multiAssigneeType = 'users'
  if (!node.value.config.approverUserIds) node.value.config.approverUserIds = []
  editorStore.isDirty = true
}

const multiApprovalSummary = computed(() => {
  if (!node.value?.config) return ''
  const cfg = node.value.config
  const count = cfg.approverUserIds?.length || 0

  const rule = cfg.multiApprovalRule || 'all'

  if (count === 0) return 'Chưa chọn nhân sự nào tham gia phê duyệt.'

  if (rule === 'all') {
    return `Bắt buộc tất cả ${count}/${count} người đồng ý thì bước này mới được thông qua.`
  }
  if (rule === 'half') {
    const req = Math.ceil(count / 2)
    return `Chỉ cần từ 1 nửa số người phê duyệt đồng ý (tối thiểu ${req}/${count} người) là thông qua.`
  }
  if (rule === 'majority') {
    const req = Math.floor(count / 2) + 1
    return `Cần quá bán số người phê duyệt đồng ý (tối thiểu ${req}/${count} người) để thông qua.`
  }
  if (rule === 'threshold') {
    const th = cfg.approvalThreshold || Math.min(2, count)
    return `Cần tối thiểu ${th}/${count} người phê duyệt đồng ý để bước này được thông qua.`
  }
  if (rule === 'any') {
    return `Bất kỳ 1 trong số ${count} người phê duyệt trước là hoàn tất bước ngay lập tức.`
  }
  return ''
})

// Notification channel helpers
const isInAppChecked = computed(() => {
  if (!node.value?.config?.channels) return true
  return node.value.config.channels.includes('in_app')
})

const toggleInAppChannel = (e: Event) => {
  if (!node.value) return
  if (!node.value.config) node.value.config = {}
  const target = e.target as HTMLInputElement
  if (target.checked) {
    node.value.config.channels = ['in_app']
  } else {
    node.value.config.channels = []
  }
  editorStore.isDirty = true
}

// ==========================================================
// TRANSITION CONDITION BUILDER LOGIC
// ==========================================================

const fromNode = computed(() => {
  if (!edge.value?.fromNodeId) return null
  return editorStore.nodes.find((n) => n.id === edge.value?.fromNodeId) || null
})

const startNode = computed(() => {
  return editorStore.nodes.find((n) => n.type === 'start') || null
})

export interface ConditionFieldOption {
  key: string
  label: string
  type: string
  dataType: ConditionDataType
  sourceGroup: string
  options?: { label: string; value: string }[]
}

const availableConditionFields = computed<ConditionFieldOption[]>(() => {
  const list: ConditionFieldOption[] = []

  // 1. Fields from bound form (source node or start node)
  const targetFormId = fromNode.value?.formBinding?.formId || startNode.value?.formBinding?.formId
  if (targetFormId) {
    const form = formStore.getFormById(targetFormId)
    if (form && form.schema && Array.isArray(form.schema.fields)) {
      form.schema.fields.forEach((f) => {
        let dt: ConditionDataType = 'STRING'
        if (f.type === 'number') dt = 'NUMBER'
        else if (f.type === 'date') dt = 'DATE'
        else if (f.type === 'checkbox') dt = 'BOOLEAN'

        list.push({
          key: f.key,
          label: `${f.label} (${f.key})`,
          type: f.type,
          dataType: dt,
          sourceGroup: `Biểu mẫu: ${form.name}`,
          options: f.options,
        })
      })
    }
  }

  // 2. Approval Action output (if from approval / review node)
  if (fromNode.value?.type === 'approval' || fromNode.value?.type === 'review') {
    list.push({
      key: 'action',
      label: 'Quyết định phê duyệt (Action)',
      type: 'select',
      dataType: 'STRING',
      sourceGroup: 'Kết quả bước duyệt',
      options: [
        { label: 'Đồng ý / Chấp thuận (APPROVED)', value: 'APPROVED' },
        { label: 'Từ chối (REJECTED)', value: 'REJECTED' },
      ],
    })
  }

  // 3. Biến quy trình độc lập (Data Contract - Cho phép định nghĩa trước khi Match Form)
  list.push(
    {
      key: 'amount',
      label: 'Tổng kinh phí / Số tiền (amount)',
      type: 'number',
      dataType: 'NUMBER',
      sourceGroup: 'Biến quy trình độc lập (Data Contract)',
    },
    {
      key: 'expense_type',
      label: 'Loại chi phí (expense_type)',
      type: 'select',
      dataType: 'STRING',
      sourceGroup: 'Biến quy trình độc lập (Data Contract)',
      options: [
        { label: 'Thiết bị IT', value: 'it_hardware' },
        { label: 'Văn phòng phẩm', value: 'stationery' },
        { label: 'Công tác phí', value: 'travel' },
        { label: 'Đào tạo', value: 'training' },
        { label: 'Khác', value: 'other' },
      ],
    },
    {
      key: 'leave_days',
      label: 'Số ngày nghỉ phép (leave_days)',
      type: 'number',
      dataType: 'NUMBER',
      sourceGroup: 'Biến quy trình độc lập (Data Contract)',
    },
    {
      key: 'urgent_flag',
      label: 'Đơn khẩn cấp (urgent_flag)',
      type: 'checkbox',
      dataType: 'BOOLEAN',
      sourceGroup: 'Biến quy trình độc lập (Data Contract)',
      options: [
        { label: 'Khẩn cấp (true)', value: 'true' },
        { label: 'Bình thường (false)', value: 'false' },
      ],
    }
  )

  // 4. Common ticket system variables
  list.push(
    {
      key: 'ticket_priority',
      label: 'Mức độ ưu tiên (Priority)',
      type: 'select',
      dataType: 'STRING',
      sourceGroup: 'Thông tin Hệ thống',
      options: [
        { label: 'Khẩn cấp (urgent)', value: 'urgent' },
        { label: 'Cao (high)', value: 'high' },
        { label: 'Bình thường (medium)', value: 'medium' },
        { label: 'Thấp (low)', value: 'low' },
      ],
    },
    {
      key: 'department',
      label: 'Phòng ban người gửi (Department)',
      type: 'text',
      dataType: 'STRING',
      sourceGroup: 'Thông tin Hệ thống',
    }
  )

  // 5. Thêm các biến tùy chỉnh người dùng đã nhập trên Edge (nếu có)
  if (edge.value?.conditions) {
    edge.value.conditions.forEach((r) => {
      if (r.fieldKey && !list.some((item) => item.key === r.fieldKey)) {
        list.push({
          key: r.fieldKey,
          label: `Biến tùy chỉnh: ${r.fieldKey}`,
          type: r.dataType === 'NUMBER' ? 'number' : 'text',
          dataType: r.dataType || 'STRING',
          sourceGroup: 'Biến quy trình tùy chỉnh',
        })
      }
    })
  }

  return list
})

const groupedConditionFields = computed(() => {
  const groups: { sourceGroup: string; fields: ConditionFieldOption[] }[] = []
  for (const f of availableConditionFields.value) {
    let grp = groups.find((g) => g.sourceGroup === f.sourceGroup)
    if (!grp) {
      grp = { sourceGroup: f.sourceGroup, fields: [] }
      groups.push(grp)
    }
    grp.fields.push(f)
  }
  return groups
})

const CONDITION_OPERATORS: { value: ConditionOperator; label: string }[] = [
  { value: 'EQUALS', label: '=' },
  { value: 'NOT_EQUALS', label: '≠' },
  { value: 'GREATER_THAN', label: '>' },
  { value: 'GREATER_THAN_OR_EQUAL', label: '≥' },
  { value: 'LESS_THAN', label: '<' },
  { value: 'LESS_THAN_OR_EQUAL', label: '≤' },
  { value: 'CONTAINS', label: 'CONTAINS' },
]

const ensureEdgeConditions = () => {
  if (!edge.value) return
  if (!edge.value.conditions) edge.value.conditions = []
  if (edge.value.conditions.length > 0) {
    edge.value.matchType = 'CUSTOM'
  } else {
    edge.value.matchType = 'ALWAYS'
  }
  edge.value.conditions.forEach((r) => {
    if (!r.logicOp) r.logicOp = 'AND'
  })
}

watch(
  () => edge.value?.id,
  () => {
    ensureEdgeConditions()
  },
  { immediate: true }
)

// Auto track dirty changes when user modifies node or edge properties
watch(
  () => node.value,
  (newVal, oldVal) => {
    if (newVal && oldVal && newVal.id === oldVal.id) {
      editorStore.isDirty = true
    }
  },
  { deep: true }
)

watch(
  () => edge.value,
  (newVal, oldVal) => {
    if (newVal && oldVal && newVal.id === oldVal.id) {
      editorStore.isDirty = true
    }
  },
  { deep: true }
)

const setRuleLogicOp = (rule: TransitionRule, op: 'AND' | 'OR') => {
  rule.logicOp = op
  editorStore.isDirty = true
}

const addConditionRule = (preferredLogicOp: 'AND' | 'OR' = 'AND') => {
  if (!edge.value) return
  if (!edge.value.conditions) edge.value.conditions = []
  edge.value.matchType = 'CUSTOM'

  const defaultField = availableConditionFields.value[0]
  const defaultVal =
    defaultField?.options && defaultField.options.length > 0
      ? defaultField.options[0]?.value || ''
      : ''

  edge.value.conditions.push({
    fieldKey: defaultField?.key || 'action',
    operator: 'EQUALS',
    compareValue: defaultVal,
    dataType: defaultField?.dataType || 'STRING',
    logicOp: preferredLogicOp,
  })
  editorStore.isDirty = true
}

const removeConditionRule = (index: number) => {
  if (!edge.value?.conditions) return
  edge.value.conditions.splice(index, 1)
  if (edge.value.conditions.length === 0) {
    edge.value.matchType = 'ALWAYS'
  }
  editorStore.isDirty = true
}

const getFieldMeta = (fieldKey: string): ConditionFieldOption | undefined => {
  return availableConditionFields.value.find((f) => f.key === fieldKey)
}

const onRuleFieldChange = (rule: TransitionRule) => {
  const meta = getFieldMeta(rule.fieldKey)
  if (meta) {
    rule.dataType = meta.dataType
    if (meta.options && meta.options.length > 0) {
      rule.compareValue = meta.options[0]?.value || ''
    } else {
      rule.compareValue = ''
    }
  }
  editorStore.isDirty = true
}

const autoGenerateEdgeLabel = () => {
  if (!edge.value) return
  if (edge.value.matchType === 'ALWAYS' || !edge.value.conditions || edge.value.conditions.length === 0) {
    edge.value.label = 'Mặc định'
    return
  }

  let labelStr = ''
  edge.value.conditions.forEach((r, idx) => {
    const fieldName = r.fieldKey
    let opSymbol = '='
    if (r.operator === 'EQUALS') opSymbol = '='
    else if (r.operator === 'NOT_EQUALS') opSymbol = '≠'
    else if (r.operator === 'GREATER_THAN') opSymbol = '>'
    else if (r.operator === 'GREATER_THAN_OR_EQUAL') opSymbol = '≥'
    else if (r.operator === 'LESS_THAN') opSymbol = '<'
    else if (r.operator === 'LESS_THAN_OR_EQUAL') opSymbol = '≤'
    else if (r.operator === 'CONTAINS') opSymbol = 'contains'

    const ruleText = `${fieldName} ${opSymbol} ${r.compareValue || '...'}`
    if (idx === 0) {
      labelStr = ruleText
    } else {
      const op = r.logicOp || (edge.value?.matchType === 'OR' ? 'OR' : 'AND')
      labelStr += ` ${op} ${ruleText}`
    }
  })

  edge.value.label = labelStr
}
</script>

<template>
  <aside
    class="properties-panel"
    :class="{ 'is-open': editorStore.isPropertiesPanelOpen }"
  >
    <!-- PANEL HEADER -->
    <div class="panel-header">
      <div class="header-title-group">
        <div class="header-badge-row">
          <span
            v-if="node"
            class="type-pill"
            :style="{
              color: nodeTypeMeta?.color,
              background: nodeTypeMeta?.bg,
            }"
          >
            {{ nodeTypeMeta?.title }}
          </span>
          <span v-else-if="edge" class="type-pill edge-pill">
            Đường Nối (Connection)
          </span>
          <span class="element-id-tag">#{{ node?.id || edge?.id }}</span>
        </div>
        <h3 class="panel-heading">
          {{ node ? 'Thuộc tính Bước' : 'Thuộc tính Điều kiện' }}
        </h3>
      </div>

      <button
        type="button"
        class="btn-close-panel"
        title="Đóng bảng thuộc tính"
        @click="editorStore.closePropertiesPanel"
      >
        ✕
      </button>
    </div>

    <!-- PANEL BODY WITH DYNAMIC FORMS -->
    <div class="panel-body">
      <!-- ==========================================================
           FORM FOR SELECTED NODE
           ========================================================== -->
      <div v-if="node" class="dynamic-form">
        <!-- 1. Common Fields: Name & Description -->
        <div class="form-section">
          <div class="section-title">Thông tin chung</div>
          
          <div class="form-group">
            <label class="form-label required">Tên bước xử lý</label>
            <input
              v-model="node.name"
              type="text"
              class="form-control"
              placeholder="Nhập tên bước..."
            />
          </div>

          <div class="form-group">
            <label class="form-label">Mô tả hướng dẫn</label>
            <textarea
              v-model="node.description"
              rows="2"
              class="form-control"
              placeholder="Mô tả tiêu chuẩn hoặc hướng dẫn cho người xử lý..."
            ></textarea>
          </div>
        </div>

        <!-- 2. Specific Form for APPROVAL NODE -->
        <div v-if="node.type === 'approval'" class="form-section">
          <div class="section-title">Cấu hình Phê duyệt (Approval)</div>

          <!-- Chế độ phê duyệt: Đơn người vs Nhiều người -->
          <div class="form-group">
            <label class="form-label required">Chế độ phê duyệt</label>
            <div class="mode-toggle-group">
              <button
                type="button"
                class="btn-mode-tab"
                :class="{ active: !node?.config?.approvalMode || node?.config?.approvalMode === 'single' }"
                @click="switchToSingleMode"
              >
                <span class="mode-icon">👤</span>
                <span class="mode-text">Một người duyệt</span>
              </button>
              <button
                type="button"
                class="btn-mode-tab"
                :class="{ active: node?.config?.approvalMode === 'multi' }"
                @click="switchToMultiMode"
              >
                <span class="mode-icon">👥</span>
                <span class="mode-text">Nhiều người duyệt</span>
              </button>
            </div>
          </div>

          <!-- === CHẾ ĐỘ 1: ĐƠN NGƯỜI DUYỆT (SINGLE) === -->
          <template v-if="!node.config.approvalMode || node.config.approvalMode === 'single'">
            <div class="form-group">
              <label class="form-label required">Hình thức chỉ định</label>
              <select v-model="node.config.approverType" class="form-control">
                <option value="role">Theo Vai trò (Role-based)</option>
                <option value="manager">Quản lý trực tiếp người tạo (Direct Manager)</option>
                <option value="dynamic">Người duyệt động theo đơn vị (Dynamic)</option>
                <option value="user">Chỉ định cụ thể người dùng (Specific User)</option>
              </select>
            </div>

            <!-- If Role-based -->
            <div v-if="node.config.approverType === 'role'" class="form-group">
              <label class="form-label required">Vai trò phê duyệt</label>
              <select v-model="node.config.approverRole" class="form-control">
                <option value="Trưởng phòng ban">Trưởng phòng ban</option>
                <option value="Trưởng phòng Mua sắm">Trưởng phòng Mua sắm</option>
                <option value="Trưởng phòng Nhân sự">Trưởng phòng Nhân sự</option>
                <option value="Kế toán trưởng">Kế toán trưởng</option>
                <option value="IT Lead">IT Lead</option>
                <option value="Trưởng ban Pháp chế">Trưởng ban Pháp chế</option>
                <option value="Giám đốc Khối (Director)">Giám đốc Khối (Director)</option>
                <option value="Tổng Giám Đốc (CEO)">Tổng Giám Đốc (CEO)</option>
                <option value="Ban Giám Đốc (Board)">Ban Giám Đốc (Board)</option>
              </select>
            </div>

            <!-- If Dynamic Approver -->
            <div v-if="node.config.approverType === 'dynamic'" class="form-group">
              <label class="form-label required">Quy tắc người duyệt động</label>
              <select v-model="node.config.dynamicApprover" class="form-control">
                <option value="creator_manager">Quản lý trực tiếp của người nộp đơn</option>
                <option value="department_head">Trưởng phòng ban của người nộp đơn</option>
                <option value="project_manager">Quản lý dự án liên quan (Project Manager)</option>
              </select>
            </div>

            <!-- If Specific User -->
            <div v-if="node.config.approverType === 'user'" class="form-group">
              <label class="form-label required">Người duyệt cụ thể</label>
              <select
                v-model="node.config.approverUserId"
                class="form-control"
                @change="() => {
                  const u = approverUsers.find(su => su.id === Number(node?.config?.approverUserId))
                  if (u && node?.config) node.config.approverName = u.fullName || u.email
                  editorStore.isDirty = true
                }"
              >
                <option :value="undefined">-- Chọn nhân sự từ hệ thống --</option>
                <option v-for="u in approverUsers" :key="u.id" :value="u.id">
                  {{ u.fullName || u.email }} ({{ u.role || 'Thành viên' }})
                </option>
              </select>
            </div>
          </template>

          <!-- === CHẾ ĐỘ 2: NHIỀU NGƯỜI DUYỆT (MULTI-APPROVER) === -->
          <template v-else>
            <!-- Multi-select Users -->
            <div class="form-group multi-user-picker">
              <div class="picker-header">
                <label class="form-label required">
                  Danh sách người phê duyệt
                  <span class="count-badge">({{ (node.config.approverUserIds || []).length }} đã chọn)</span>
                </label>
              </div>

              <!-- Filter input -->
              <div class="search-user-input-box">
                <input
                  v-model="userSearchFilter"
                  type="text"
                  class="form-control form-control-sm"
                  placeholder="🔍 Tìm theo tên, email, vai trò..."
                />
              </div>

              <!-- User selection list -->
              <div class="user-checkbox-list">
                <div
                  v-for="u in filteredUsers"
                  :key="u.id"
                  class="user-checkbox-item"
                  :class="{ selected: isUserSelected(u.id) }"
                  @click="toggleUserSelection(u.id)"
                >
                  <div class="item-checkbox">
                    <input
                      type="checkbox"
                      :checked="isUserSelected(u.id)"
                      @click.stop="toggleUserSelection(u.id)"
                    />
                  </div>
                  <div class="item-user-info">
                    <div class="user-main-name">
                      {{ u.fullName || u.email }}
                    </div>
                    <div class="user-sub-details">
                      <span class="user-role-tag">{{ u.role || 'Member' }}</span>
                      <span class="user-email-text">{{ u.email }}</span>
                    </div>
                  </div>
                </div>
                <div v-if="filteredUsers.length === 0" class="empty-users-hint">
                  Không tìm thấy nhân sự phù hợp
                </div>
              </div>
            </div>

            <!-- QUY TẮC PHÊ DUYỆT (MULTI-APPROVAL RULE) -->
            <div class="form-group rule-config-box">
              <label class="form-label required">Quy tắc điều kiện thông qua</label>
              <select v-model="node.config.multiApprovalRule" class="form-control select-rule-accent" @change="editorStore.isDirty = true">
                <option value="all">100% Đồng thuận</option>
                <option value="half">>= 50% số người</option>
                <option value="majority">< 50% số người</option>
                <option value="any">1 người bất kỳ</option>
              </select>
            </div>

            <!-- If Threshold -->
            <div v-if="node.config.multiApprovalRule === 'threshold'" class="form-group">
              <label class="form-label required">Số người tối thiểu cần phê duyệt</label>
              <div class="threshold-input-wrap">
                <input
                  v-model.number="node.config.approvalThreshold"
                  type="number"
                  min="1"
                  :max="node.config.multiAssigneeType === 'roles' ? (node.config.approverRoles?.length || 10) : (node.config.approverUserIds?.length || 10)"
                  class="form-control"
                  placeholder="2"
                  @input="editorStore.isDirty = true"
                />
                <span class="input-suffix-label">người</span>
              </div>
            </div>

            <!-- Quy tắc từ chối -->
            <div class="form-group">
              <label class="form-label">Quy tắc khi có người Từ chối (Rejection)</label>
              <select v-model="node.config.multiRejectionRule" class="form-control" @change="editorStore.isDirty = true">
                <option value="any">❌ 1 người từ chối là dừng và từ chối yêu cầu</option>
                <option value="majority">📉 Quá bán từ chối thì mới từ chối</option>
              </select>
            </div>

            <!-- Box tóm tắt quy tắc thông minh -->
            <div class="multi-rule-summary-banner">
              <div class="banner-icon">💡</div>
              <div class="banner-content">
                <strong>Quy tắc áp dụng:</strong>
                <span>{{ multiApprovalSummary }}</span>
              </div>
            </div>
          </template>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label required">Thời hạn SLA (Giờ)</label>
              <input
                v-model.number="node.config.slaHours"
                type="number"
                min="1"
                class="form-control"
                placeholder="24"
              />
            </div>

            <div class="form-group">
              <label class="form-label">Quy tắc quá hạn SLA</label>
              <select v-model="node.config.escalationRule" class="form-control">
                <option value="remind">🔔 Gửi thông báo nhắc nhở</option>
                <option value="escalate">⬆️ Tự động chuyển cấp cao hơn</option>
                <option value="auto_reject">❌ Tự động từ chối</option>
              </select>
            </div>
          </div>

          <!-- SLA Escalation Rule -->
          <div class="form-group">
            <label class="form-label">Quy tắc khi quá hạn SLA</label>
            <select v-model="node.config.escalationRule" class="form-control">
              <option value="remind">🔔 Gửi thông báo nhắc nhở (Email / In-app)</option>
              <option value="escalate">⬆️ Tự động chuyển cấp phê duyệt cao hơn</option>
              <option value="auto_reject">❌ Tự động từ chối yêu cầu</option>
            </select>
          </div>

          <div class="checkbox-option-row">
            <label class="custom-checkbox">
              <input v-model="node.config.allowRequestChanges" type="checkbox" />
              <span class="checkmark-box"></span>
              <span class="opt-label">Cho phép yêu cầu người tạo bổ sung/sửa đổi hồ sơ</span>
            </label>
          </div>

          <div class="checkbox-option-row">
            <label class="custom-checkbox">
              <input v-model="node.config.requireNoteOnReject" type="checkbox" />
              <span class="checkmark-box"></span>
              <span class="opt-label">Bắt buộc nhập lý do giải trình khi Từ chối</span>
            </label>
          </div>
        </div>

        <!-- 3. Specific Form for REVIEW NODE -->
        <div v-if="node.type === 'review'" class="form-section">
          <div class="section-title">Cấu hình Soát xét (Review)</div>

          <div class="form-group">
            <label class="form-label required">Bộ phận soát xét</label>
            <select v-model="node.config.reviewerRole" class="form-control">
              <option value="Trưởng bộ phận">Trưởng bộ phận liên quan</option>
              <option value="Chuyên viên Pháp chế">Chuyên viên Pháp chế</option>
              <option value="Chuyên viên Tài chính">Chuyên viên Tài chính</option>
              <option value="Bảo mật IT">Bảo mật IT</option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">Thời hạn SLA (Giờ)</label>
            <input
              v-model.number="node.config.slaHours"
              type="number"
              min="1"
              class="form-control"
              placeholder="24"
            />
          </div>

          <div class="checkbox-option-row">
            <label class="custom-checkbox">
              <input v-model="node.config.allowRequestChanges" type="checkbox" />
              <span class="checkmark-box"></span>
              <span class="opt-label">Cho phép yêu cầu bổ sung/sửa đổi hồ sơ</span>
            </label>
          </div>
        </div>

        <!-- 4. Specific Form for ASSIGNMENT NODE -->
        <div v-if="node.type === 'assignment'" class="form-section">
          <div class="section-title">Cấu hình Giao việc (Assignment)</div>

          <div class="form-group">
            <label class="form-label required">Tiêu đề nhiệm vụ</label>
            <input
              v-model="node.config.taskTitle"
              type="text"
              class="form-control"
              placeholder="Ví dụ: Lập hợp đồng mua sắm..."
            />
          </div>

          <div class="form-group">
            <label class="form-label required">Người thực hiện</label>
            <select v-model="node.config.assigneeRole" class="form-control">
              <option value="Chuyên viên Mua sắm">Chuyên viên Mua sắm</option>
              <option value="Chuyên viên Nhân sự">Chuyên viên Nhân sự</option>
              <option value="Kỹ sư Hệ thống IT">Kỹ sư Hệ thống IT</option>
              <option value="Chuyên viên Pháp chế">Chuyên viên Pháp chế</option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">Hạn xử lý (Số ngày)</label>
            <input
              v-model.number="node.config.dueDays"
              type="number"
              min="1"
              class="form-control"
              placeholder="3"
            />
          </div>
        </div>

        <!-- 5. Specific Form for NOTIFICATION NODE -->
        <div v-if="node.type === 'notification'" class="form-section">
          <div class="section-title">Cấu hình Thông báo (Notification)</div>

          <div class="form-group">
            <label class="form-label required">Kênh gửi thông báo</label>
            <div class="channels-checkbox-grid">
              <label class="channel-chip disabled" title="Kênh Email tạm thời chưa khả dụng">
                <input type="checkbox" disabled />
                <span>📧 Email</span>
              </label>
              <label class="channel-chip disabled" title="Kênh MS Teams tạm thời chưa khả dụng">
                <input type="checkbox" disabled />
                <span>💬 MS Teams</span>
              </label>
              <label class="channel-chip selected" title="Kênh thông báo trực tiếp trên ứng dụng">
                <input
                  type="checkbox"
                  :checked="isInAppChecked"
                  @change="toggleInAppChannel"
                />
                <span>🔔 In-app Web</span>
              </label>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label required">Tiêu đề thông báo</label>
            <input
              v-model="node.config.subject"
              type="text"
              class="form-control"
              placeholder="[Thông báo] Quy trình..."
            />
          </div>

          <div class="form-group">
            <label class="form-label">Nội dung mẫu (Template)</label>
            <textarea
              v-model="node.config.contentTemplate"
              rows="3"
              class="form-control"
              placeholder="Xin chào, yêu cầu của bạn đã được cập nhật..."
            ></textarea>
          </div>
        </div>

        <!-- 6. Specific Form for SYSTEM ACTION NODE -->
        <div v-if="node.type === 'system_action'" class="form-section">
          <div class="section-title">Cấu hình Tác vụ Hệ thống</div>

          <div class="form-group">
            <label class="form-label required">Loại tác vụ</label>
            <select v-model="node.config.actionType" class="form-control">
              <option value="webhook">Gọi Webhook REST API</option>
              <option value="erp_sync">Đồng bộ Hệ thống ERP (SAP/Oracle)</option>
              <option value="db_update">Cập nhật Trực tiếp Cơ sở dữ liệu</option>
            </select>
          </div>

          <div class="form-row">
            <div class="form-group" style="flex: 0.4;">
              <label class="form-label">Method</label>
              <select v-model="node.config.httpMethod" class="form-control">
                <option value="POST">POST</option>
                <option value="GET">GET</option>
                <option value="PUT">PUT</option>
              </select>
            </div>
            <div class="form-group" style="flex: 1;">
              <label class="form-label required">Endpoint URL</label>
              <input
                v-model="node.config.endpointUrl"
                type="text"
                class="form-control"
                placeholder="https://api.erp.company.com/v1/sync"
              />
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Request Body Payload (JSON)</label>
            <textarea
              v-model="node.config.payloadTemplate"
              rows="3"
              class="form-control code-textarea"
              placeholder='{\n  "id": "{{request_id}}"\n}'
            ></textarea>
          </div>
        </div>

        <!-- 7. Specific Form for START NODE -->
        <div v-if="node.type === 'start'" class="form-section">
          <div class="section-title">Khởi động Quy trình (Start Trigger)</div>

          <div class="form-group">
            <label class="form-label required">Hình thức kích hoạt</label>
            <select v-model="node.config.triggerType" class="form-control">
              <option value="form_submission">Người dùng gửi biểu mẫu yêu cầu (Ticket / Form)</option>
              <option value="manual">Khởi chạy thủ công bởi Quản trị viên</option>
              <option value="schedule">Tự động theo lịch định kỳ (Cron Schedule)</option>
            </select>
          </div>

        </div>

        <!-- 8. Specific Form for END NODE -->
        <div v-if="node.type === 'end'" class="form-section">
          <div class="section-title">Kết thúc Quy trình</div>

          <div class="form-group">
            <label class="form-label required">Trạng thái kết thúc (Outcome)</label>
            <select v-model="node.config.outcome" class="form-control">
              <option value="completed">Hoàn tất thành công (Completed)</option>
              <option value="rejected">Bị từ chối / Hủy bỏ (Rejected)</option>
              <option value="cancelled">Dừng luồng khẩn cấp (Cancelled)</option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">Ghi chú kết thúc</label>
            <textarea
              v-model="node.config.closingNote"
              rows="2"
              class="form-control"
              placeholder="Ghi chú hoàn tất quy trình..."
            ></textarea>
          </div>
        </div>

        <!-- 9. Specific Form for CONDITION NODE -->
        <div v-if="node.type === 'condition'" class="form-section">
          <div class="section-title">Cấu hình Rẽ nhánh Điều kiện (If/Else)</div>

          <div class="form-group">
            <label class="form-label">Cổng đầu ra (Output Ports)</label>
            <div class="mode-toggle-group" style="flex-direction: column; gap: 0.5rem;">
              <div class="btn-mode-tab active" style="justify-content: flex-start; gap: 0.5rem; cursor: default;">
                <span class="mode-icon" style="color: #ec4899;">●</span>
                <span class="mode-text">Port trên: <strong>Nhánh điều kiện (If)</strong></span>
              </div>
              <div class="btn-mode-tab active" style="justify-content: flex-start; gap: 0.5rem; cursor: default; background: #fffbeb; border-color: #fde68a;">
                <span class="mode-icon" style="color: #f59e0b;">●</span>
                <span class="mode-text" style="color: #92400e;">Port dưới: <strong>ELSE (Fallback mặc định)</strong></span>
              </div>
            </div>
            <span class="form-help-text" style="margin-top: 0.5rem; display: block; font-size: 0.75rem; color: #64748b;">
              💡 <strong>Hướng dẫn:</strong> Kéo từ Port dưới (ELSE) đến bước tiếp theo để xử lý fallback khi không có điều kiện nào thỏa mãn.
            </span>
          </div>
        </div>

        <!-- 10. Specific Form for PARALLEL NODE -->
        <div v-if="node.type === 'parallel'" class="form-section">
          <div class="section-title">Cấu hình Rẽ nhánh Song song</div>

          <div class="form-group">
            <label class="form-label required">Chế độ phân nhánh</label>
            <select v-model="node.config.joinMode" class="form-control" @change="editorStore.isDirty = true">
              <option value="all">Kích hoạt tất cả các luồng đầu ra đồng thời (Fork Parallel)</option>
            </select>
            <span class="form-help-text" style="margin-top: 0.5rem; display: block; font-size: 0.75rem; color: #64748b;">
              ⚡ Tất cả các bước nối từ node này sẽ được kích hoạt xử lý đồng thời.
            </span>
          </div>
        </div>

        <!-- 11. Specific Form for JOIN NODE -->
        <div v-if="node.type === 'join'" class="form-section">
          <div class="section-title">Cấu hình Hợp luồng Song song (Join / Merge)</div>

          <div class="form-group">
            <label class="form-label required">Quy tắc hợp luồng (Join Strategy)</label>
            <select v-model="node.config.joinStrategy" class="form-control" @change="editorStore.isDirty = true">
              <option value="wait_all">Chờ tất cả các luồng song song hoàn tất (Wait All Threads)</option>
              <option value="first_come">Tiếp tục ngay khi luồng đầu tiên hoàn tất (First Come / Any)</option>
              <option value="n_of_m">Chờ tối thiểu N luồng hoàn tất (N of M)</option>
            </select>
            <span class="form-help-text" style="margin-top: 0.5rem; display: block; font-size: 0.75rem; color: #64748b;">
              ⇶ Điểm hợp luồng sẽ thu gom các luồng xử lý song song và đồng bộ hóa lại thành 1 luồng duy nhất để đi tiếp.
            </span>
          </div>

          <div v-if="node.config.joinStrategy === 'n_of_m'" class="form-group">
            <label class="form-label required">Số luồng tối thiểu cần hoàn tất (N)</label>
            <input
              v-model.number="node.config.requiredCount"
              type="number"
              min="1"
              class="form-control"
              placeholder="2"
              @change="editorStore.isDirty = true"
            />
          </div>
        </div>
      </div>

      <!-- ==========================================================
           FORM FOR SELECTED EDGE (CONNECTION)
           ========================================================== -->
      <div v-else-if="edge" class="dynamic-form">
        <div class="form-section">
          <div class="section-title">Cấu hình Đường Nối & Điều kiện</div>

          <div class="form-group">
            <label class="form-label required">Điểm bắt đầu (Từ bước)</label>
            <select v-model="edge.fromNodeId" class="form-control">
              <option
                v-for="n in editorStore.nodes.filter((item) => item.id !== edge?.toNodeId && item.type !== 'end')"
                :key="n.id"
                :value="n.id"
              >
                {{ n.name }} ({{ n.type.toUpperCase() }})
              </option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label required">Điểm kết thúc (Đến bước)</label>
            <select v-model="edge.toNodeId" class="form-control">
              <option
                v-for="n in editorStore.nodes.filter((item) => item.id !== edge?.fromNodeId && item.type !== 'start')"
                :key="n.id"
                :value="n.id"
              >
                {{ n.name }} ({{ n.type.toUpperCase() }})
              </option>
            </select>
          </div>


          <div class="form-group">
            <label class="form-label">Nhãn hiển thị trên đường nối</label>
            <input
              v-model="edge.label"
              type="text"
              class="form-control"
              placeholder="Ví dụ: Đồng ý, Từ chối, Giá trị > 10M..."
            />
          </div>

          <!-- TRANSITION CONDITIONS BUILDER -->
          <div class="form-section conditions-section">
            <div class="section-title-row">
              <span class="section-title">Điều kiện rẽ nhánh</span>
              <span v-if="edge.conditions && edge.conditions.length > 0" class="rules-badge">{{ edge.conditions.length }} điều kiện</span>
            </div>

            <!-- Khi chưa có điều kiện: hiển thị thông báo và nút thêm điều kiện mới -->
            <div v-if="!edge.conditions || edge.conditions.length === 0" class="empty-conditions-box">
              <span class="empty-conditions-text">Chưa thiết lập điều kiện nào (Mặc định đường nối luôn được đi qua)</span>
              <button
                type="button"
                class="btn-add-condition-primary"
                @click="addConditionRule('AND')"
              >
                + Thêm điều kiện mới
              </button>
            </div>

            <!-- Khi đã có điều kiện: hiển thị danh sách -->
            <div v-else class="rules-container">
              <div class="rules-list">
                <template
                  v-for="(rule, rIdx) in (edge.conditions || [])"
                  :key="rIdx"
                >
                  <!-- Connector between rules -->
                  <div v-if="rIdx > 0" class="rule-logic-connector">
                    <div class="connector-line"></div>
                    <div class="connector-pill">
                      <button
                        type="button"
                        class="logic-op-btn"
                        :class="{ active: rule.logicOp === 'AND' || !rule.logicOp }"
                        @click="setRuleLogicOp(rule, 'AND')"
                      >
                        AND
                      </button>
                      <button
                        type="button"
                        class="logic-op-btn"
                        :class="{ active: rule.logicOp === 'OR' }"
                        @click="setRuleLogicOp(rule, 'OR')"
                      >
                        OR
                      </button>
                    </div>
                    <div class="connector-line"></div>
                  </div>

                  <div class="rule-card">
                    <div class="rule-card-top">
                      <span class="rule-index">#{{ rIdx + 1 }}</span>
                      <span class="rule-type-badge">{{ rule.dataType }}</span>
                      <button
                        type="button"
                        class="btn-delete-rule"
                        title="Xóa điều kiện này"
                        @click="removeConditionRule(rIdx)"
                      >
                        ✕
                      </button>
                    </div>

                    <!-- Field Selection -->
                    <div class="rule-field-group">
                      <div class="rule-field-header">
                        <label class="rule-label">Trường dữ liệu kiểm tra</label>
                        <span
                          v-if="getFieldMeta(rule.fieldKey)?.sourceGroup"
                          class="rule-source-badge"
                          :title="'Nguồn: ' + getFieldMeta(rule.fieldKey)?.sourceGroup"
                        >
                          {{ getFieldMeta(rule.fieldKey)?.sourceGroup }}
                        </span>
                      </div>
                      <select
                        v-model="rule.fieldKey"
                        class="form-control select-field"
                        @change="onRuleFieldChange(rule)"
                      >
                        <optgroup
                          v-for="group in groupedConditionFields"
                          :key="group.sourceGroup"
                          :label="group.sourceGroup"
                        >
                          <option
                            v-for="f in group.fields"
                            :key="f.key"
                            :value="f.key"
                          >
                            {{ f.label || f.key }}
                          </option>
                        </optgroup>
                      </select>
                    </div>

                    <!-- Operator & Value -->
                    <div class="rule-row-compare">
                      <div class="rule-op-col">
                        <label class="rule-label">Toán tử</label>
                        <select v-model="rule.operator" class="form-control select-op">
                          <option
                            v-for="op in CONDITION_OPERATORS"
                            :key="op.value"
                            :value="op.value"
                          >
                            {{ op.label }}
                          </option>
                        </select>
                      </div>

                      <div class="rule-val-col">
                        <label class="rule-label">Giá trị so sánh</label>
                        <!-- If dropdown select field -->
                        <select
                          v-if="getFieldMeta(rule.fieldKey)?.options?.length"
                          v-model="rule.compareValue"
                          class="form-control select-val"
                        >
                          <option
                            v-for="opt in getFieldMeta(rule.fieldKey)?.options"
                            :key="opt.value"
                            :value="opt.value"
                          >
                            {{ opt.label }}
                          </option>
                        </select>

                        <!-- If number -->
                        <input
                          v-else-if="getFieldMeta(rule.fieldKey)?.dataType === 'NUMBER'"
                          v-model="rule.compareValue"
                          type="number"
                          class="form-control input-val"
                          placeholder="Nhập số..."
                        />

                        <!-- If date -->
                        <input
                          v-else-if="getFieldMeta(rule.fieldKey)?.dataType === 'DATE'"
                          v-model="rule.compareValue"
                          type="date"
                          class="form-control input-val"
                        />

                        <!-- Default text -->
                        <input
                          v-else
                          v-model="rule.compareValue"
                          type="text"
                          class="form-control input-val"
                          placeholder="Nhập giá trị..."
                        />
                      </div>
                    </div>
                  </div>
                </template>
              </div>

              <!-- Button Add Single Rule -->
              <button
                type="button"
                class="btn-add-rule-single"
                @click="addConditionRule('AND')"
              >
                + Thêm điều kiện mới
              </button>
            </div>

          </div>
        </div>
      </div>
    </div>

    <!-- PANEL FOOTER: DELETE BUTTON -->
    <div class="panel-footer">
      <button
        type="button"
        class="btn-delete-element"
        @click="handleDelete"
      >
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="3 6 5 6 21 6"></polyline>
          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
        </svg>
        <span>{{ node ? 'Xóa Bước Này (Delete Node)' : 'Xóa Đường Nối (Delete Connection)' }}</span>
      </button>
    </div>
  </aside>
</template>

<style scoped>
.properties-panel {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  height: 100%;
  width: 380px;
  background: #ffffff;
  border-left: 1px solid #e2e8f0;
  box-shadow: -4px 0 24px rgba(15, 23, 42, 0.08);
  display: flex;
  flex-direction: column;
  transform: translateX(100%);
  transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 40;
  user-select: none;
}

.properties-panel.is-open {
  transform: translateX(0);
}

/* Header */
.panel-header {
  padding: 1.125rem 1.25rem;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.75rem;
}

.header-title-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.header-badge-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.type-pill {
  font-size: 0.6875rem;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 9999px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.edge-pill {
  background: #f1f5f9;
  color: #475569;
}

.element-id-tag {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.6875rem;
  color: #94a3b8;
}

.panel-heading {
  font-size: 1.0625rem;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.01em;
}

.btn-close-panel {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  font-size: 0.875rem;
  transition: all 0.15s ease;
  background: transparent;
  border: none;
  cursor: pointer;
}

.btn-close-panel:hover {
  background: #f1f5f9;
  color: #0f172a;
}

/* Body */
.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 1.25rem;
}

.dynamic-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
  padding-bottom: 1.25rem;
  border-bottom: 1px solid #f1f5f9;
}

.form-section:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.section-title {
  font-size: 0.75rem;
  font-weight: 800;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.form-row {
  display: flex;
  gap: 0.75rem;
}

.form-label {
  font-size: 0.75rem;
  font-weight: 700;
  color: #334155;
}

.form-label.required::after {
  content: ' *';
  color: #ef4444;
}

.form-control {
  width: 100%;
  padding: 0.5rem 0.75rem;
  font-size: 0.8125rem;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #ffffff;
  color: #0f172a;
}

.form-control:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 2px var(--border-glow);
  outline: none;
}

.code-textarea {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.75rem;
  background: #f8fafc;
}

.hint-text {
  font-size: 0.6875rem;
  color: #94a3b8;
}

/* Custom Checkbox */
.checkbox-option-row {
  margin-top: 0.25rem;
}

.custom-checkbox {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  font-size: 0.75rem;
  font-weight: 600;
  color: #334155;
}

.custom-checkbox input {
  accent-color: var(--primary);
  width: 15px;
  height: 15px;
}

/* Channel Checkbox Grid */
.channels-checkbox-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
}

.channel-chip {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.35rem 0.5rem;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #f8fafc;
  font-size: 0.75rem;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
  transition: all 0.15s ease;
}

.channel-chip input {
  accent-color: #6366f1;
}

.channel-chip.disabled {
  opacity: 0.55;
  cursor: not-allowed;
  background: #f1f5f9;
  color: #94a3b8;
  border-color: #e2e8f0;
}

.channel-chip.disabled input {
  cursor: not-allowed;
}

.channel-chip.selected {
  border-color: #a5b4fc;
  background: #eef2ff;
  color: #4338ca;
}

/* Footer */
.panel-footer {
  padding: 1rem 1.25rem;
  border-top: 1px solid #f1f5f9;
  background: #ffffff;
}

.btn-delete-element {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  height: 38px;
  border: 1px solid #fca5a5;
  background: #fef2f2;
  color: #dc2626;
  font-size: 0.8125rem;
  font-weight: 700;
  border-radius: 8px;
  transition: all 0.15s ease;
  cursor: pointer;
}

.btn-delete-element:hover {
  background: #dc2626;
  color: #ffffff;
  border-color: #dc2626;
  box-shadow: 0 2px 8px rgba(220, 38, 38, 0.25);
}

/* FORM BINDING STYLES */
.form-binding-box {
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
  background: #f8fafc;
  padding: 0.875rem;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  margin-top: 0.5rem;
}

.binding-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.45rem;
}

.binding-label {
  margin-bottom: 0 !important;
  white-space: nowrap;
  font-size: 0.8125rem;
  font-weight: 700;
  color: #334155;
  flex-shrink: 0;
}

.binding-header-actions {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  white-space: nowrap;
  flex-shrink: 0;
}

.btn-refresh-forms {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #64748b;
  padding: 0;
  transition: all 0.2s ease;
}

.btn-refresh-forms:hover {
  background: #f1f5f9;
  color: var(--primary);
  border-color: var(--primary);
}

.btn-refresh-forms.is-spinning svg {
  animation: spin-refresh 0.6s linear infinite;
}

@keyframes spin-refresh {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.link-manage-forms {
  font-size: 0.72rem;
  color: var(--primary);
  text-decoration: none;
  font-weight: 600;
  white-space: nowrap;
}

.link-manage-forms:hover {
  text-decoration: underline;
}

.select-form-accent {
  font-weight: 600;
  color: #1e293b;
  border-color: #bae6fd;
}

.bound-form-details {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-top: 0.25rem;
}

.form-actions-inline {
  display: flex;
  gap: 0.5rem;
}

.btn-inline-action {
  flex: 1;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #334155;
  font-size: 0.75rem;
  font-weight: 600;
  padding: 0.4rem 0.5rem;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-inline-action:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.btn-sim-inline {
  background: #ecfdf5;
  color: #059669;
  border-color: #a7f3d0;
}

.btn-sim-inline:hover {
  background: #d1fae5;
}

.permissions-matrix-wrap {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.625rem;
}

.matrix-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.72rem;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 0.5rem;
  padding-bottom: 0.35rem;
  border-bottom: 1px solid #f1f5f9;
}

.matrix-hint {
  font-size: 0.65rem;
  font-weight: normal;
  color: #94a3b8;
}

.matrix-table {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  max-height: 200px;
  overflow-y: auto;
}

.matrix-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0.25rem 0.35rem;
  background: #f8fafc;
  border-radius: 6px;
}

.matrix-field-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  max-width: 150px;
}

.m-label {
  font-size: 0.72rem;
  font-weight: 600;
  color: #334155;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.m-key {
  font-size: 0.625rem;
  font-family: monospace;
  color: #94a3b8;
}

.matrix-select {
  font-size: 0.6875rem;
  padding: 0.2rem 0.4rem;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  background: #ffffff;
  color: #1e293b;
  outline: none;
}

.binding-placeholder-note {
  font-size: 0.72rem;
  color: #64748b;
  line-height: 1.4;
  padding: 0.25rem 0;
}

/* ========================================================== */
/* VISUAL CONDITION BUILDER STYLES */
/* ========================================================== */
.conditions-section {
  border-top: 1px solid #e2e8f0;
  padding-top: 1rem;
  margin-top: 1rem;
}

.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.75rem;
}

/* EMPTY CONDITIONS BOX */
.empty-conditions-box {
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  padding: 1.25rem 1rem;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
}

.empty-conditions-text {
  font-size: 0.75rem;
  color: #64748b;
  line-height: 1.4;
}

.btn-add-condition-primary {
  background: #4f46e5;
  color: #ffffff;
  border: none;
  font-size: 0.75rem;
  font-weight: 600;
  padding: 0.5rem 1rem;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 1px 3px rgba(79, 70, 229, 0.3);
}

.btn-add-condition-primary:hover {
  background: #4338ca;
  box-shadow: 0 2px 6px rgba(79, 70, 229, 0.4);
}

.btn-add-rule-single {
  background: #ffffff;
  border: 1px dashed #6366f1;
  color: #4f46e5;
  font-size: 0.75rem;
  font-weight: 600;
  padding: 0.5rem 0.75rem;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: center;
  width: 100%;
}

.btn-add-rule-single:hover {
  background: #eef2ff;
  border-color: #4f46e5;
}


/* RULES CONTAINER */
.rules-container {
  margin-top: 0.875rem;
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
}

.rules-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.rules-heading {
  font-size: 0.75rem;
  font-weight: 700;
  color: #334155;
}

.rules-badge {
  font-size: 0.65rem;
  background: #e0f2fe;
  color: var(--primary);
  font-weight: 700;
  padding: 0.1rem 0.4rem;
  border-radius: 9999px;
}

.rules-list {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.rule-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.65rem 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  transition: all 0.2s ease;
}

.rule-card:hover {
  border-color: #cbd5e1;
  background: #ffffff;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.04);
}

.rule-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.rule-index {
  font-size: 0.72rem;
  font-weight: 700;
  color: #0284c7;
}

.rule-type-badge {
  font-size: 0.625rem;
  font-family: monospace;
  background: #e2e8f0;
  color: #475569;
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
}

.btn-delete-rule {
  background: none;
  border: none;
  color: #94a3b8;
  font-size: 0.8rem;
  cursor: pointer;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.btn-delete-rule:hover {
  background: #fee2e2;
  color: #dc2626;
}

.rule-field-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
}

.rule-field-header .rule-label {
  margin-bottom: 0;
}

.rule-source-badge {
  font-size: 0.625rem;
  font-weight: 700;
  color: #4338ca;
  background: #e0e7ff;
  border: 1px solid #c7d2fe;
  padding: 0.1rem 0.45rem;
  border-radius: 4px;
  max-width: 170px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  letter-spacing: 0.02em;
}

.rule-label {
  font-size: 0.6875rem;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 0.2rem;
  display: block;
}

.select-field {
  font-size: 0.75rem;
  padding: 0.3rem 0.5rem;
  height: auto;
}

.select-field optgroup {
  font-weight: 700;
  color: #3730a3;
  background-color: #f1f5f9;
}

.select-field optgroup option {
  font-weight: 500;
  color: #0f172a;
  background-color: #ffffff;
}

.rule-row-compare {
  display: flex;
  gap: 0.5rem;
}

.rule-op-col {
  width: 42%;
}

.rule-val-col {
  width: 58%;
}

.select-op,
.select-val,
.input-val {
  font-size: 0.75rem;
  padding: 0.3rem 0.5rem;
  height: auto;
}

/* RULE LOGIC CONNECTOR */
.rule-logic-connector {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin: 0.2rem 0;
}

.connector-line {
  flex: 1;
  height: 1px;
  background: #e2e8f0;
}

.connector-pill {
  display: inline-flex;
  background: #f1f5f9;
  padding: 2px;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
}

.logic-op-btn {
  padding: 0.15rem 0.55rem;
  font-size: 0.6875rem;
  font-weight: 700;
  border: none;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.15s ease;
}

.logic-op-btn:hover {
  color: #0f172a;
}

.logic-op-btn.active {
  background: #4f46e5;
  color: #ffffff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.add-rules-btn-group {
  display: flex;
  gap: 0.5rem;
  margin-top: 0.25rem;
}

.add-rules-btn-group .btn-add-rule {
  flex: 1;
  font-size: 0.72rem;
  padding: 0.45rem 0.5rem;
}

.btn-add-and {
  border-color: #c7d2fe;
  color: #4338ca;
  background: #eef2ff;
}

.btn-add-and:hover {
  background: #e0e7ff;
  border-color: #6366f1;
}

.btn-add-or {
  border-color: #fed7aa;
  color: #c2410c;
  background: #fff7ed;
}

.btn-add-or:hover {
  background: #ffedd5;
  border-color: #f97316;
}

.btn-add-rule {
  background: #ffffff;
  border: 1px dashed #cbd5e1;
  color: var(--primary);
  font-size: 0.75rem;
  font-weight: 600;
  padding: 0.45rem 0.75rem;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: center;
}

.btn-add-rule:hover {
  border-color: var(--primary);
  background: #f0f9ff;
}

/* ==========================================================
   MULTI-APPROVER CONFIG STYLES
   ========================================================== */
.mode-toggle-group {
  display: flex;
  background: #f1f5f9;
  padding: 3px;
  border-radius: 8px;
  gap: 3px;
}

.btn-mode-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.4rem;
  padding: 0.5rem 0.6rem;
  background: transparent;
  border: none;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-mode-tab:hover {
  color: #1e293b;
}

.btn-mode-tab.active {
  background: #ffffff;
  color: #4f46e5;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.multi-user-picker {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 0.75rem;
}

.picker-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}

.picker-header .form-label {
  margin-bottom: 0;
}

.count-badge {
  color: #4f46e5;
  font-weight: 700;
  font-size: 0.75rem;
}

.search-user-input-box {
  margin-bottom: 0.5rem;
}

.user-checkbox-list {
  max-height: 180px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  padding: 0.35rem;
}

.user-checkbox-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.4rem 0.5rem;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.user-checkbox-item:hover {
  background: #f1f5f9;
}

.user-checkbox-item.selected {
  background: #eef2ff;
}

.item-user-info {
  flex: 1;
  min-width: 0;
}

.user-main-name {
  font-size: 0.75rem;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-sub-details {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.6875rem;
}

.user-role-tag {
  background: #e2e8f0;
  color: #475569;
  padding: 1px 4px;
  border-radius: 4px;
  font-size: 0.625rem;
}

.user-email-text {
  color: #94a3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.empty-users-hint {
  text-align: center;
  color: #94a3b8;
  font-size: 0.75rem;
  padding: 1rem 0;
}

.threshold-input-wrap {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.threshold-input-wrap .form-control {
  max-width: 100px;
}

.input-suffix-label {
  font-size: 0.8125rem;
  color: #64748b;
  font-weight: 600;
}

.select-rule-accent {
  font-weight: 600;
  color: #1e293b;
}

.multi-rule-summary-banner {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  background: linear-gradient(135deg, #eef2ff 0%, #f0fdf4 100%);
  border: 1px solid #c7d2fe;
  border-radius: 8px;
  padding: 0.625rem 0.75rem;
  font-size: 0.75rem;
  color: #334155;
  line-height: 1.4;
  margin-top: 0.5rem;
}

.banner-icon {
  font-size: 1rem;
  flex-shrink: 0;
}

.banner-content strong {
  color: #3730a3;
  display: block;
  margin-bottom: 2px;
}
</style>
