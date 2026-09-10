import type { WorkflowEditorNode, WorkflowEditorEdge } from '@/types/editor'

export interface WorkflowTemplateBlueprint {
  id: string
  name: string
  nodes: WorkflowEditorNode[]
  edges: WorkflowEditorEdge[]
}

export const WORKFLOW_TEMPLATES: Record<string, WorkflowTemplateBlueprint> = {
  // 1. Phê duyệt Yêu cầu Mua sắm (Tiêu chuẩn)
  'tpl-1': {
    id: 'tpl-1',
    name: 'Phê duyệt Yêu cầu Mua sắm (Tiêu chuẩn)',
    nodes: [
      {
        id: 'node-start',
        type: 'start',
        name: 'Tạo Yêu cầu Mua sắm',
        description: 'Nhân viên điền biểu mẫu đề xuất mua vật tư/thiết bị',
        position: { x: 80, y: 220 },
        config: {
          triggerType: 'form_submission',
          formName: 'Đơn đề xuất mua sắm thiết bị',
        },
      },
      {
        id: 'node-review',
        type: 'review',
        name: 'Soát xét Trưởng bộ phận',
        description: 'Kiểm tra nhu cầu sử dụng thực tế của phòng ban',
        position: { x: 360, y: 220 },
        config: {
          reviewerType: 'department_lead',
          reviewerRole: 'Trưởng phòng ban',
          slaHours: 24,
          allowRequestChanges: true,
        },
      },
      {
        id: 'node-accountant',
        type: 'review',
        name: 'Kế toán trưởng Thẩm định',
        description: 'Kiểm tra dự toán và tính hợp lệ ngân sách',
        position: { x: 640, y: 220 },
        config: {
          reviewerType: 'role',
          reviewerRole: 'Kế toán trưởng',
          slaHours: 24,
          allowRequestChanges: true,
        },
      },
      {
        id: 'node-ceo-approval',
        type: 'approval',
        name: 'Giám đốc Ký duyệt',
        description: 'Phê duyệt chi ngân sách mua sắm',
        position: { x: 920, y: 220 },
        config: {
          approverType: 'role',
          approverRole: 'Ban Giám Đốc',
          slaHours: 48,
          escalationRule: 'remind',
          approvalStrategy: 'single',
        },
      },
      {
        id: 'node-notify-success',
        type: 'notification',
        name: 'Thông báo Kết quả & Mua hàng',
        description: 'Gửi thông báo phê duyệt tới Người tạo và Phòng Mua sắm',
        position: { x: 1200, y: 220 },
        config: {
          channels: ['email', 'teams'],
          recipientType: 'workflow_owner',
          subject: '[WorkerBuilder] Yêu cầu mua sắm đã được duyệt',
          contentTemplate: 'Yêu cầu {{request_code}} của bạn đã được phê duyệt thành công!',
        },
      },
      {
        id: 'node-end',
        type: 'end',
        name: 'Hoàn tất Quy trình',
        description: 'Chuyển thông tin sang bộ phận mua sắm thực hiện',
        position: { x: 1480, y: 220 },
        config: {
          outcome: 'completed',
          closingNote: 'Hoàn tất phê duyệt mua sắm.',
        },
      },
    ],
    edges: [
      { id: 'edge-1', fromNodeId: 'node-start', toNodeId: 'node-review', label: 'Gửi duyệt' },
      { id: 'edge-2', fromNodeId: 'node-review', toNodeId: 'node-accountant', label: 'Hợp lệ' },
      { id: 'edge-3', fromNodeId: 'node-accountant', toNodeId: 'node-ceo-approval', label: 'Đủ ngân sách' },
      { id: 'edge-4', fromNodeId: 'node-ceo-approval', toNodeId: 'node-notify-success', label: 'Đã duyệt' },
      { id: 'edge-5', fromNodeId: 'node-notify-success', toNodeId: 'node-end', label: 'Kết thúc' },
    ],
  },

  // 2. Đơn xin Nghỉ phép & Nghỉ bù (HR)
  'tpl-2': {
    id: 'tpl-2',
    name: 'Đơn xin Nghỉ phép & Nghỉ bù',
    nodes: [
      {
        id: 'node-start',
        type: 'start',
        name: 'Đăng ký Nghỉ phép',
        description: 'Nhân viên chọn ngày nghỉ và lý do',
        position: { x: 100, y: 220 },
        config: {
          triggerType: 'form_submission',
          formName: 'Đơn xin nghỉ phép',
        },
      },
      {
        id: 'node-manager-approval',
        type: 'approval',
        name: 'Quản lý Trực tiếp Phê duyệt',
        description: 'Xét duyệt kế hoạch bàn giao công việc',
        position: { x: 420, y: 220 },
        config: {
          approverType: 'manager',
          approverRole: 'Quản lý trực tiếp',
          slaHours: 12,
          escalationRule: 'remind',
          approvalStrategy: 'single',
        },
      },
      {
        id: 'node-hr-notification',
        type: 'notification',
        name: 'Cập nhật Chấm công & Gửi Mail',
        description: 'Tự động đồng bộ lịch vắng mặt và gửi xác nhận',
        position: { x: 740, y: 220 },
        config: {
          channels: ['email', 'in_app'],
          recipientType: 'workflow_owner',
          subject: '[HR] Đơn nghỉ phép đã được phê duyệt',
          contentTemplate: 'Đơn xin nghỉ phép từ {{start_date}} đến {{end_date}} của bạn đã được phê duyệt.',
        },
      },
      {
        id: 'node-end',
        type: 'end',
        name: 'Kết thúc',
        description: 'Lưu vào bảng chấm công',
        position: { x: 1060, y: 220 },
        config: {
          outcome: 'completed',
          closingNote: 'Hoàn tất ghi nhận nghỉ phép.',
        },
      },
    ],
    edges: [
      { id: 'edge-1', fromNodeId: 'node-start', toNodeId: 'node-manager-approval', label: 'Gửi đơn' },
      { id: 'edge-2', fromNodeId: 'node-manager-approval', toNodeId: 'node-hr-notification', label: 'Đồng ý' },
      { id: 'edge-3', fromNodeId: 'node-hr-notification', toNodeId: 'node-end', label: 'Hoàn tất' },
    ],
  },

  // 3. Thanh toán Hóa đơn & Hoàn ứng Chi phí (Finance)
  'tpl-3': {
    id: 'tpl-3',
    name: 'Thanh toán Hóa đơn & Hoàn ứng Chi phí',
    nodes: [
      {
        id: 'node-start',
        type: 'start',
        name: 'Tạo Yêu cầu Thanh toán',
        description: 'Tải lên hóa đơn VAT, chứng từ và số tiền hoàn ứng',
        position: { x: 80, y: 220 },
        config: {
          triggerType: 'form_submission',
          formName: 'Đề nghị thanh toán / Hoàn ứng',
        },
      },
      {
        id: 'node-review-manager',
        type: 'review',
        name: 'Quản lý Trực tiếp Xác nhận',
        description: 'Xác nhận tính xác thực của chi phí công tác/tiếp khách',
        position: { x: 380, y: 220 },
        config: {
          reviewerType: 'manager',
          reviewerRole: 'Quản lý trực tiếp',
          slaHours: 24,
          allowRequestChanges: true,
        },
      },
      {
        id: 'node-accounting-check',
        type: 'review',
        name: 'Kế toán Kiểm tra Chứng từ',
        description: 'Soát xét hóa đơn điện tử và quy chuẩn thuế',
        position: { x: 680, y: 220 },
        config: {
          reviewerType: 'role',
          reviewerRole: 'Chuyên viên Kế toán',
          slaHours: 24,
          allowRequestChanges: true,
        },
      },
      {
        id: 'node-finance-approval',
        type: 'approval',
        name: 'Giám đốc Tài chính Phê duyệt',
        description: 'Duyệt lệnh chi tiền ngân hàng',
        position: { x: 980, y: 220 },
        config: {
          approverType: 'role',
          approverRole: 'CFO / Giám đốc Tài chính',
          slaHours: 24,
          escalationRule: 'remind',
          approvalStrategy: 'single',
        },
      },
      {
        id: 'node-bank-action',
        type: 'system_action',
        name: 'Chuyển khoản qua Ngân hàng (ERP API)',
        description: 'Tự động tạo lệnh chi hộ qua cổng kết nối Banking',
        position: { x: 1280, y: 220 },
        config: {
          actionType: 'webhook',
          httpMethod: 'POST',
          endpointUrl: 'https://api.erp.company.com/finance/disbursement',
        },
      },
      {
        id: 'node-end',
        type: 'end',
        name: 'Chi trả Thành công',
        description: 'Hoàn tất giải ngân cho nhân viên',
        position: { x: 1580, y: 220 },
        config: {
          outcome: 'completed',
          closingNote: 'Tiền đã được chuyển vào tài khoản nhân viên.',
        },
      },
    ],
    edges: [
      { id: 'edge-1', fromNodeId: 'node-start', toNodeId: 'node-review-manager', label: 'Trình duyệt' },
      { id: 'edge-2', fromNodeId: 'node-review-manager', toNodeId: 'node-accounting-check', label: 'Hợp lệ' },
      { id: 'edge-3', fromNodeId: 'node-accounting-check', toNodeId: 'node-finance-approval', label: 'Chứng từ đủ' },
      { id: 'edge-4', fromNodeId: 'node-finance-approval', toNodeId: 'node-bank-action', label: 'Duyệt chi' },
      { id: 'edge-5', fromNodeId: 'node-bank-action', toNodeId: 'node-end', label: 'Đã chuyển tiền' },
    ],
  },

  // 4. Cấp phát Tài khoản & Thiết bị Nhân viên mới (IT)
  'tpl-4': {
    id: 'tpl-4',
    name: 'Cấp phát Tài khoản & Thiết bị Nhân viên mới',
    nodes: [
      {
        id: 'node-start',
        type: 'start',
        name: 'Tiếp nhận Nhân sự mới từ HR',
        description: 'Tự động kích hoạt khi có hợp đồng thử việc',
        position: { x: 80, y: 220 },
        config: {
          triggerType: 'form_submission',
          formName: 'Phiếu yêu cầu Onboarding nhân sự',
        },
      },
      {
        id: 'node-it-webhook',
        type: 'system_action',
        name: 'Tự động tạo Email & Active Directory',
        description: 'Gọi API Microsoft 365 / Google Workspace tạo tài khoản',
        position: { x: 380, y: 220 },
        config: {
          actionType: 'webhook',
          httpMethod: 'POST',
          endpointUrl: 'https://idp.company.com/api/v1/users/provision',
        },
      },
      {
        id: 'node-hardware-task',
        type: 'assignment',
        name: 'Giao IT Chuẩn bị Laptop & Thẻ',
        description: 'Cài đặt hệ điều hành và bàn giao trang thiết bị',
        position: { x: 680, y: 220 },
        config: {
          assigneeType: 'role',
          assigneeRole: 'IT Helpdesk',
          taskTitle: 'Chuẩn bị Máy tính & Thẻ ra vào',
          dueDays: 2,
        },
      },
      {
        id: 'node-notify-manager',
        type: 'notification',
        name: 'Gửi Thông tin Đăng nhập cho Quản lý',
        description: 'Thông báo tài khoản và thiết bị đã sẵn sàng',
        position: { x: 980, y: 220 },
        config: {
          channels: ['email', 'teams'],
          recipientType: 'workflow_owner',
          subject: '[IT] Thiết bị & Tài khoản nhân viên mới đã sẵn sàng',
          contentTemplate: 'Tài khoản {{user_email}} đã được tạo thành công.',
        },
      },
      {
        id: 'node-end',
        type: 'end',
        name: 'Hoàn tất Onboarding',
        description: 'Nhân viên mới sẵn sàng làm việc',
        position: { x: 1280, y: 220 },
        config: {
          outcome: 'completed',
          closingNote: 'Hoàn tất cấp phát trang thiết bị.',
        },
      },
    ],
    edges: [
      { id: 'edge-1', fromNodeId: 'node-start', toNodeId: 'node-it-webhook', label: 'Tự động tạo TK' },
      { id: 'edge-2', fromNodeId: 'node-it-webhook', toNodeId: 'node-hardware-task', label: 'Đã tạo xong' },
      { id: 'edge-3', fromNodeId: 'node-hardware-task', toNodeId: 'node-notify-manager', label: 'Đã bàn giao' },
      { id: 'edge-4', fromNodeId: 'node-notify-manager', toNodeId: 'node-end', label: 'Kết thúc' },
    ],
  },
}
