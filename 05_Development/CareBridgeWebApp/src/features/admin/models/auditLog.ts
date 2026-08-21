// UC117 View Audit Logs — mirrors backend AuditLogResponse.
// `action` is intentionally typed as `string` (not a closed union) because the
// backend AuditAction enum has 70+ values across every domain in the system and
// grows independently of this screen — matching it exactly here would rot immediately.
export interface AuditLogEntry {
  id: string;
  timestamp: string;
  userId: string | null;
  actorName: string | null;
  actorEmail: string | null;
  action: string;
  resourceType: string | null;
  resourceId: string | null;
  details: string | null;
}

export interface AuditLogSearchParams {
  userId?: string;
  action?: string;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
}

// Vietnamese labels for actions commonly surfaced on admin dashboards.
export const AUDIT_ACTION_LABELS: Record<string, string> = {
  USER_ACCOUNT_STATUS_CHANGED: 'Đổi trạng thái tài khoản',
  STAFF_ACCOUNT_CREATED: 'Tạo tài khoản nhân viên',
  ROLE_PERMISSION_UPDATED: 'Thay đổi quyền hạn',
  VIEW_AUDIT_LOG: 'Xem nhật ký hệ thống',
  LOGIN: 'Đăng nhập',
  LOGOUT: 'Đăng xuất',
  SESSION_REVOKED: 'Thu hồi phiên đăng nhập',
  PASSWORD_CHANGED: 'Đổi mật khẩu',
  PASSWORD_RESET_REQUESTED: 'Yêu cầu đặt lại mật khẩu',
  PASSWORD_RESET_COMPLETED: 'Hoàn tất đặt lại mật khẩu',
  SECURITY_INCIDENT_INVESTIGATED: 'Điều tra sự cố bảo mật',
  SECURITY_EVENT_REVIEWED: 'Xét duyệt sự kiện bảo mật',
  SECURITY_NOTE_ADDED: 'Thêm ghi chú bảo mật',
  SECURITY_EVENT: 'Ghi nhận sự kiện bảo mật',
  CONTENT_HIDDEN: 'Ẩn nội dung',
  CONTENT_REPORTED: 'Báo cáo nội dung',
  CONTENT_CREATED: 'Tạo nội dung',
  CONTENT_UPDATED: 'Cập nhật nội dung',
  CONTENT_UNPUBLISHED: 'Gỡ xuất bản nội dung',
  MODERATION_ACTION: 'Thực hiện kiểm duyệt',
  DIRECT_CALL_INITIATED: 'Bắt đầu cuộc gọi trực tiếp',
  DIRECT_CALL_STATE_CHANGED: 'Cập nhật trạng thái cuộc gọi',
  DIRECT_CALL_ACCESS_DENIED: 'Từ chối truy cập cuộc gọi',
  DIRECT_CALL_MISSED_BY_TIMEOUT: 'Cuộc gọi bị nhỡ',
  FIREBASE_CUSTOM_TOKEN_ISSUED: 'Khởi tạo kết nối thông báo',
  SYSTEM_CONFIGURATION_UPDATED: 'Cập nhật cấu hình hệ thống',
  AI_POLICY_CREATED: 'Tạo chính sách kiểm duyệt AI',
  AI_POLICY_UPDATED: 'Cập nhật chính sách kiểm duyệt AI',
  AI_POLICY_STATUS_CHANGED: 'Đổi trạng thái chính sách AI',
  AI_POLICY_TEST_RUN: 'Chạy thử chính sách AI',
};

const AUDIT_ACTION_SUMMARIES: Record<string, string> = {
  VIEW_AUDIT_LOG: 'Đã mở danh sách nhật ký hệ thống.',
  DIRECT_CALL_STATE_CHANGED: 'Đã cập nhật trạng thái cuộc gọi trực tiếp.',
  USER_ACCOUNT_STATUS_CHANGED: 'Đã cập nhật trạng thái của một tài khoản.',
  FIREBASE_CUSTOM_TOKEN_ISSUED: 'Đã khởi tạo kết nối nhận thông báo.',
  SYSTEM_CONFIGURATION_UPDATED: 'Đã thay đổi cấu hình vận hành hệ thống.',
};

const AUDIT_DETAIL_LABELS: Record<string, string> = {
  recordingStatus: 'Trạng thái bản ghi',
  recordingDeleted: 'Bản ghi đã được xóa',
  status: 'Trạng thái',
  severity: 'Mức độ',
  eventType: 'Loại sự kiện',
  from: 'Trước khi thay đổi',
  to: 'Sau khi thay đổi',
  reason: 'Lý do',
  active: 'Đang hoạt động',
  enabled: 'Đã bật',
  locked: 'Đang khóa',
  role: 'Vai trò',
  ip: 'Địa chỉ IP',
  ipAddress: 'Địa chỉ IP',
  userAgent: 'Trình duyệt',
};

const AUDIT_VALUE_LABELS: Record<string, string> = {
  NONE: 'Không có bản ghi',
  ACTIVE: 'Đang hoạt động',
  INACTIVE: 'Ngừng hoạt động',
  ENABLED: 'Đã bật',
  DISABLED: 'Đã tắt',
  LOCKED: 'Đã khóa',
  UNLOCKED: 'Đã mở khóa',
  PENDING: 'Đang chờ xử lý',
  COMPLETED: 'Đã hoàn tất',
  FAILED: 'Thất bại',
  HIGH: 'Cao',
  MEDIUM: 'Trung bình',
  LOW: 'Thấp',
  CRITICAL: 'Nghiêm trọng',
};

const TECHNICAL_DETAIL_KEYS = new Set([
  'userId',
  'resourceId',
  'entityId',
  'correlationId',
  'page',
  'size',
]);

export function getAuditActionLabel(action: string): string {
  return AUDIT_ACTION_LABELS[action] ?? 'Thao tác hệ thống';
}

function formatUserAgent(userAgent: string): string {
  const browser = userAgent.includes('Edg/')
    ? 'Microsoft Edge'
    : userAgent.includes('Chrome/')
      ? 'Google Chrome'
      : userAgent.includes('Firefox/')
        ? 'Mozilla Firefox'
        : userAgent.includes('Safari/')
          ? 'Safari'
          : 'Trình duyệt không xác định';
  const operatingSystem = userAgent.includes('Mac OS X')
    ? 'macOS'
    : userAgent.includes('Windows')
      ? 'Windows'
      : userAgent.includes('Android')
        ? 'Android'
        : /iPhone|iPad/.test(userAgent)
          ? 'iOS'
          : userAgent.includes('Linux')
            ? 'Linux'
            : null;

  return operatingSystem ? `${browser} trên ${operatingSystem}` : browser;
}

function formatAuditValue(key: string, value: unknown): string {
  if (typeof value === 'boolean') return value ? 'Có' : 'Không';
  if (typeof value === 'string') {
    if (key === 'userAgent') return formatUserAgent(value);
    if ((key === 'ip' || key === 'ipAddress') && ['127.0.0.1', '::1', '0:0:0:0:0:0:0:1'].includes(value)) {
      return 'Máy cục bộ';
    }
    return AUDIT_VALUE_LABELS[value] ?? value;
  }
  if (typeof value === 'number') return value.toLocaleString('vi-VN');
  return 'Có dữ liệu cập nhật';
}

export function formatAuditDetails(action: string, details: string | null): string {
  if (action === 'VIEW_AUDIT_LOG') {
    return AUDIT_ACTION_SUMMARIES.VIEW_AUDIT_LOG;
  }
  if (!details?.trim()) {
    return AUDIT_ACTION_SUMMARIES[action] ?? 'Không có ghi chú bổ sung.';
  }

  try {
    const parsed = JSON.parse(details) as Record<string, unknown>;
    if (action === 'DIRECT_CALL_STATE_CHANGED') {
      if (parsed.recordingDeleted === true) return 'Đã xóa bản ghi cuộc gọi.';
      if (parsed.recordingStatus === 'NONE') return 'Cuộc gọi không có bản ghi.';
    }

    const readableDetails = Object.entries(parsed)
      .filter(([key, value]) => value !== null && value !== '' && !TECHNICAL_DETAIL_KEYS.has(key))
      .slice(0, 4)
      .map(([key, value]) => `${AUDIT_DETAIL_LABELS[key] ?? 'Thông tin'}: ${formatAuditValue(key, value)}`);

    return readableDetails.length > 0
      ? readableDetails.join(' • ')
      : AUDIT_ACTION_SUMMARIES[action] ?? 'Không có ghi chú bổ sung.';
  } catch {
    return details;
  }
}
