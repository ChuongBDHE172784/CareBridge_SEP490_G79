// UC117 View Audit Logs — mirrors backend AuditLogResponse.
// `action` is intentionally typed as `string` (not a closed union) because the
// backend AuditAction enum has 70+ values across every domain in the system and
// grows independently of this screen — matching it exactly here would rot immediately.
export interface AuditLogEntry {
  id: string;
  timestamp: string;
  userId: string | null;
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

// Labels for the audit actions most relevant to admin/security screens (Sprint 3 scope).
// Unlisted actions fall back to the raw enum value.
export const AUDIT_ACTION_LABELS: Record<string, string> = {
  USER_ACCOUNT_STATUS_CHANGED: 'Đổi trạng thái tài khoản',
  STAFF_ACCOUNT_CREATED: 'Tạo tài khoản nhân viên',
  ROLE_PERMISSION_UPDATED: 'Thay đổi quyền hạn',
  VIEW_AUDIT_LOG: 'Xem nhật ký hệ thống',
  LOGIN: 'Đăng nhập',
  LOGOUT: 'Đăng xuất',
  SECURITY_INCIDENT_INVESTIGATED: 'Điều tra sự cố bảo mật',
  SECURITY_EVENT_REVIEWED: 'Xét duyệt sự kiện bảo mật',
  CONTENT_HIDDEN: 'Ẩn nội dung',
  CONTENT_REPORTED: 'Báo cáo nội dung',
};
