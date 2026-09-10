 export type FormFieldType = 
  | 'text' 
  | 'number' 
  | 'textarea' 
  | 'select' 
  | 'date' 
  | 'file' 
  | 'checkbox'
  | 'table'
  | 'list'

export interface FormFieldOption {
  label: string
  value: string
}

export interface TableColumn {
  key: string
  label: string
  type: 'text' | 'number' | 'select'
  placeholder?: string
  options?: FormFieldOption[]
  required?: boolean
  width?: string
}

export interface FormField {
  id: string | number
  key: string
  label: string
  type: FormFieldType
  placeholder?: string
  required: boolean
  defaultValue?: any
  options?: FormFieldOption[]
  columns?: TableColumn[]
  summaryFieldKey?: string
  addBtnText?: string
  min?: number
  max?: number
  helpText?: string
  orderIndex?: number
}

export interface FormSchema {
  fields: FormField[]
}

export interface FormItem {
  id: number | string
  name: string
  description?: string
  schema: FormSchema
  createdAt: string
  updatedAt?: string
  createdBy?: string
  usageCount?: number
}

export type FieldPermission = 'editable' | 'readonly' | 'hidden'

export interface NodeFormBinding {
  formId?: number | string
  formName?: string
  fieldPermissions: Record<string, FieldPermission>
}
