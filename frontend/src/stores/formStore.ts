import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { FormItem, FormField } from '@/types/form'
import { formApi, type FormBackendResponseDTO } from '@/services/formApi'

const STORAGE_KEY = 'worker_builder_forms_v1'

const DEFAULT_FORMS: FormItem[] = [
  {
    id: 1,
    name: 'Ticket Hỗ Trợ Kỹ Thuật IT',
    description: 'Biểu mẫu tiếp nhận sự cố phần cứng, phần mềm, tài khoản mạng cho cán bộ nhân viên.',
    createdAt: '2026-08-20T08:30:00Z',
    updatedAt: '2026-09-01T10:15:00Z',
    createdBy: 'IT Helpdesk Lead',
    usageCount: 14,
    schema: {
      fields: [
        {
          id: 'f_it_title',
          key: 'title',
          label: 'Tiêu đề sự cố / yêu cầu',
          type: 'text',
          placeholder: 'Ví dụ: Không thể kết nối máy in phòng Kế toán',
          required: true,
          helpText: 'Tóm tắt ngắn gọn vấn đề cần hỗ trợ',
        },
        {
          id: 'f_it_category',
          key: 'category',
          label: 'Phân loại sự cố',
          type: 'select',
          required: true,
          defaultValue: 'software',
          options: [
            { label: 'Phần mềm / Ứng dụng', value: 'software' },
            { label: 'Phần cứng / Thiết bị', value: 'hardware' },
            { label: 'Mạng nội bộ & VPN', value: 'network' },
            { label: 'Cấp quyền & Tài khoản', value: 'access' },
          ],
        },
        {
          id: 'f_it_priority',
          key: 'priority',
          label: 'Mức độ ưu tiên',
          type: 'select',
          required: true,
          defaultValue: 'medium',
          options: [
            { label: 'Thấp (Không gấp)', value: 'low' },
            { label: 'Bình thường', value: 'medium' },
            { label: 'Cao (Ảnh hưởng công việc)', value: 'high' },
            { label: 'Khẩn cấp (Hệ thống ngưng trệ)', value: 'urgent' },
          ],
        },
        {
          id: 'f_it_device',
          key: 'device_code',
          label: 'Mã máy tính / Thiết bị',
          type: 'text',
          placeholder: 'Ví dụ: PC-IT-029',
          required: false,
        },
        {
          id: 'f_it_desc',
          key: 'description',
          label: 'Mô tả chi tiết sự cố',
          type: 'textarea',
          placeholder: 'Mô tả hiện tượng lỗi, các bước tái hiện, thông báo lỗi nếu có...',
          required: true,
        },
        {
          id: 'f_it_file',
          key: 'attachment',
          label: 'Ảnh chụp màn hình / Tài liệu đính kèm',
          type: 'file',
          required: false,
          helpText: 'Định dạng .png, .jpg, .pdf tối đa 10MB',
        },
      ],
    },
  },
  {
    id: 2,
    name: 'Đơn Đề Xuất Mua Sắm Thiết Bị',
    description: 'Biểu mẫu đề xuất trang cấp hoặc thay mới máy móc, thiết bị văn phòng, công cụ làm việc.',
    createdAt: '2026-08-15T09:00:00Z',
    updatedAt: '2026-08-28T14:20:00Z',
    createdBy: 'Procurement Specialist',
    usageCount: 8,
    schema: {
      fields: [
        {
          id: 'f_req_title',
          key: 'title',
          label: 'Tên đề xuất mua sắm',
          type: 'text',
          placeholder: 'Ví dụ: Đề xuất cấp màn hình 27 inch cho Dev',
          required: true,
        },
        {
          id: 'f_req_category',
          key: 'category',
          label: 'Nhóm thiết bị',
          type: 'select',
          required: true,
          defaultValue: 'monitor',
          options: [
            { label: 'Màn hình máy tính', value: 'monitor' },
            { label: 'Laptop / Máy trạm', value: 'pc' },
            { label: 'Bàn ghế công thái học', value: 'furniture' },
            { label: 'Thiết bị mạng & lưu trữ', value: 'network' },
            { label: 'Khác', value: 'other' },
          ],
        },
        {
          id: 'f_req_qty',
          key: 'quantity',
          label: 'Số lượng cần mua',
          type: 'number',
          placeholder: '1',
          required: true,
          defaultValue: 1,
          min: 1,
        },
        {
          id: 'f_req_cost',
          key: 'estimated_cost',
          label: 'Kinh phí ước tính (VNĐ)',
          type: 'number',
          placeholder: '5000000',
          required: true,
          min: 0,
          helpText: 'Kinh phí làm căn cứ để rẽ nhánh phê duyệt ngân sách',
        },
        {
          id: 'f_req_justification',
          key: 'justification',
          label: 'Lý do & mục đích sử dụng',
          type: 'textarea',
          placeholder: 'Giải trình sự cần thiết cho công việc hiện tại...',
          required: true,
        },
        {
          id: 'f_req_quote',
          key: 'quotation_url',
          label: 'Link tham khảo / Báo giá',
          type: 'text',
          placeholder: 'https://...',
          required: false,
        },
      ],
    },
  },
  {
    id: 3,
    name: 'Đơn Xin Nghỉ Phép & Vắng Mặt',
    description: 'Biểu mẫu đăng ký nghỉ phép năm, nghỉ ốm, nghỉ chế độ hoặc làm việc từ xa (WFH).',
    createdAt: '2026-08-10T11:00:00Z',
    updatedAt: '2026-08-25T16:45:00Z',
    createdBy: 'HR Officer',
    usageCount: 25,
    schema: {
      fields: [
        {
          id: 'f_lv_type',
          key: 'leave_type',
          label: 'Loại hình nghỉ',
          type: 'select',
          required: true,
          defaultValue: 'annual',
          options: [
            { label: 'Nghỉ phép năm', value: 'annual' },
            { label: 'Nghỉ việc riêng / Ốm đau', value: 'sick' },
            { label: 'Làm việc từ xa (WFH)', value: 'wfh' },
            { label: 'Nghỉ không hưởng lương', value: 'unpaid' },
          ],
        },
        {
          id: 'f_lv_from',
          key: 'from_date',
          label: 'Từ ngày',
          type: 'date',
          required: true,
        },
        {
          id: 'f_lv_to',
          key: 'to_date',
          label: 'Đến hết ngày',
          type: 'date',
          required: true,
        },
        {
          id: 'f_lv_handover',
          key: 'handover_colleague',
          label: 'Người nhận bàn giao công việc',
          type: 'text',
          placeholder: 'Tên hoặc Email đồng nghiệp...',
          required: true,
        },
        {
          id: 'f_lv_reason',
          key: 'reason',
          label: 'Lý do xin nghỉ',
          type: 'textarea',
          placeholder: 'Ghi rõ lý do vắng mặt...',
          required: true,
        },
      ],
    },
  },
]

export const useFormStore = defineStore('formStore', () => {
  // Load from local storage or fall back to default forms
  const loadSavedForms = (): FormItem[] => {
    try {
      const data = localStorage.getItem(STORAGE_KEY)
      if (data) {
        const parsed = JSON.parse(data)
        if (Array.isArray(parsed) && parsed.length > 0) {
          return parsed
        }
      }
    } catch (e) {
      console.error('Error loading forms from localStorage:', e)
    }
    return DEFAULT_FORMS
  }

  const forms = ref<FormItem[]>(loadSavedForms())
  const isLoading = ref(false)
  const isSaving = ref(false)
  const errorMessage = ref<string | null>(null)

  // Modals state
  const isBuilderModalOpen = ref(false)
  const isPreviewModalOpen = ref(false)
  const isSimulationModalOpen = ref(false)

  // Current active form for editing or previewing
  const editingForm = ref<FormItem | null>(null)
  const previewingForm = ref<FormItem | null>(null)
  const simulationForm = ref<FormItem | null>(null)
  const simulationPermissions = ref<Record<string, 'editable' | 'readonly' | 'hidden'>>({})

  const saveToStorage = () => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(forms.value))
    } catch (e) {
      console.error('Error saving forms to localStorage:', e)
    }
  }

  const mapBackendDtoToFormItem = (dto: FormBackendResponseDTO): FormItem => {
    return {
      id: dto.id,
      name: dto.name,
      description: dto.description || '',
      schema: {
        fields: Array.isArray(dto.schema?.fields)
          ? dto.schema.fields.map((f: any, idx: number) => ({
              id: f.id !== undefined && f.id !== null ? f.id : `field_${dto.id}_${idx}`,
              key: f.key,
              label: f.label || f.key,
              type: f.type || 'text',
              placeholder: f.placeholder || '',
              required: Boolean(f.required),
              defaultValue: f.defaultValue,
              helpText: f.helpText || '',
              options: Array.isArray(f.options) ? f.options : [],
              min: f.min !== undefined && f.min !== null ? Number(f.min) : undefined,
              max: f.max !== undefined && f.max !== null ? Number(f.max) : undefined,
              orderIndex: f.orderIndex !== undefined ? Number(f.orderIndex) : idx,
            }))
          : [],
      },
      createdAt: dto.createdAt || new Date().toISOString(),
      updatedAt: dto.createdAt || new Date().toISOString(),
      createdBy: dto.createdByName || 'Hệ thống',
      usageCount: Number(dto.usageCount || 0),
    }
  }

  const fetchForms = async (keyword?: string) => {
    isLoading.value = true
    errorMessage.value = null
    try {
      const res = await formApi.getAllForms(keyword)
      if (res.success && Array.isArray(res.data)) {
        forms.value = res.data.map(mapBackendDtoToFormItem)
        saveToStorage()
      }
    } catch (err: any) {
      console.warn('Không thể tải forms từ backend, sử dụng LocalStorage:', err?.message)
      if (forms.value.length === 0) {
        forms.value = loadSavedForms()
      }
    } finally {
      isLoading.value = false
    }
  }

  const fetchFormById = async (id: number): Promise<FormItem | null> => {
    try {
      const res = await formApi.getFormById(id)
      if (res.success && res.data) {
        const item = mapBackendDtoToFormItem(res.data)
        const idx = forms.value.findIndex((f) => String(f.id) === String(id))
        if (idx >= 0) {
          forms.value[idx] = item
        } else {
          forms.value.push(item)
        }
        return item
      }
    } catch (e) {
      console.warn(`Không thể lấy chi tiết form ID ${id} từ backend:`, e)
    }
    return getFormById(id) || null
  }

  const getFormById = (id: number | string): FormItem | undefined => {
    return forms.value.find((f) => String(f.id) === String(id))
  }

  const openCreateModal = () => {
    editingForm.value = {
      id: Date.now(),
      name: '',
      description: '',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      usageCount: 0,
      schema: {
        fields: [
          {
            id: `field_${Date.now()}_1`,
            key: 'title',
            label: 'Tiêu đề yêu cầu',
            type: 'text',
            placeholder: 'Nhập tiêu đề tóm tắt...',
            required: true,
          },
        ],
      },
    }
    isBuilderModalOpen.value = true
  }

  const openEditModal = (form: FormItem) => {
    // Deep clone to avoid mutating directly before save
    editingForm.value = JSON.parse(JSON.stringify(form))
    isBuilderModalOpen.value = true
  }

  const closeBuilderModal = () => {
    isBuilderModalOpen.value = false
    editingForm.value = null
  }

  const saveForm = async (formToSave: FormItem): Promise<boolean> => {
    isSaving.value = true
    errorMessage.value = null

    try {
      // Kiểm tra nếu form đã có id trong database backend (id số nhỏ hơn 1 tỷ)
      const isExistingOnBackend =
        typeof formToSave.id === 'number' &&
        formToSave.id < 1000000000 &&
        forms.value.some((f) => f.id === formToSave.id)

      let savedItem: FormItem

      if (isExistingOnBackend) {
        // Cập nhật biểu mẫu (PUT /api/forms/{id})
        const res = await formApi.updateForm(Number(formToSave.id), {
          name: formToSave.name,
          description: formToSave.description,
          schema: formToSave.schema,
        })
        if (res.success && res.data) {
          savedItem = mapBackendDtoToFormItem(res.data)
        } else {
          throw new Error(res.message || 'Lỗi khi cập nhật biểu mẫu trên server')
        }
      } else {
        // Tạo mới biểu mẫu (POST /api/forms)
        const res = await formApi.createForm({
          name: formToSave.name,
          description: formToSave.description,
          schema: formToSave.schema,
        })
        if (res.success && res.data) {
          savedItem = mapBackendDtoToFormItem(res.data)
        } else {
          throw new Error(res.message || 'Lỗi khi tạo mới biểu mẫu trên server')
        }
      }

      // Cập nhật danh sách forms
      const index = forms.value.findIndex(
        (f) => String(f.id) === String(formToSave.id) || String(f.id) === String(savedItem.id)
      )
      if (index >= 0) {
        forms.value[index] = savedItem
      } else {
        forms.value.unshift(savedItem)
      }

      saveToStorage()
      closeBuilderModal()
      return true
    } catch (err: any) {
      console.warn('Backend chưa sẵn sàng hoặc lỗi lưu API, lưu tạm vào LocalStorage:', err?.message)
      // Fallback lưu cục bộ
      formToSave.updatedAt = new Date().toISOString()
      const index = forms.value.findIndex((f) => String(f.id) === String(formToSave.id))
      if (index >= 0) {
        forms.value[index] = { ...formToSave }
      } else {
        forms.value.unshift({ ...formToSave })
      }
      saveToStorage()
      closeBuilderModal()
      return true
    } finally {
      isSaving.value = false
    }
  }

  const deleteForm = async (id: number | string) => {
    try {
      if (typeof id === 'number' && id < 1000000000) {
        await formApi.deleteForm(id)
      }
    } catch (err: any) {
      console.warn('Lỗi gọi API xóa form, tiến hành xóa cục bộ:', err?.message)
    }
    forms.value = forms.value.filter((f) => String(f.id) !== String(id))
    saveToStorage()
  }

  const duplicateForm = async (form: FormItem) => {
    const newName = `${form.name} (Bản sao)`
    try {
      const res = await formApi.createForm({
        name: newName,
        description: form.description,
        schema: form.schema,
      })
      if (res.success && res.data) {
        const item = mapBackendDtoToFormItem(res.data)
        forms.value.unshift(item)
        saveToStorage()
        return
      }
    } catch (err: any) {
      console.warn('Tạo bản sao qua API thất bại, nhân bản cục bộ:', err?.message)
    }

    const duplicated: FormItem = {
      ...JSON.parse(JSON.stringify(form)),
      id: Date.now(),
      name: newName,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      usageCount: 0,
    }
    forms.value.unshift(duplicated)
    saveToStorage()
  }

  const openPreviewModal = (form: FormItem) => {
    previewingForm.value = form
    isPreviewModalOpen.value = true
  }

  const closePreviewModal = () => {
    isPreviewModalOpen.value = false
    previewingForm.value = null
  }

  const openSimulationModal = (form: FormItem, permissions: Record<string, 'editable' | 'readonly' | 'hidden'> = {}) => {
    simulationForm.value = form
    simulationPermissions.value = permissions
    isSimulationModalOpen.value = true
  }

  const closeSimulationModal = () => {
    isSimulationModalOpen.value = false
    simulationForm.value = null
  }

  return {
    forms,
    isLoading,
    isSaving,
    errorMessage,
    isBuilderModalOpen,
    isPreviewModalOpen,
    isSimulationModalOpen,
    editingForm,
    previewingForm,
    simulationForm,
    simulationPermissions,
    fetchForms,
    fetchFormById,
    getFormById,
    openCreateModal,
    openEditModal,
    closeBuilderModal,
    saveForm,
    deleteForm,
    duplicateForm,
    openPreviewModal,
    closePreviewModal,
    openSimulationModal,
    closeSimulationModal,
  }
})
