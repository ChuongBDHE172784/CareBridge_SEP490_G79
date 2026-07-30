import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../../shared/auth/useAuth';
import {
  getUser,
  getUserActivity,
  getUserSessions,
  updateUserStatus,
} from '../services/adminUserApi';
import type {
  AdminUserActivity,
  AdminUserSession,
  AdminUserSummary,
  StaffRole,
} from '../models/adminUser';
import type { UserRole } from '../../../shared/auth/authStore';
import ConfirmDialog from '../../../shared/components/ConfirmDialog';

const ROLE_LABELS: Record<UserRole, string> = {
  MOTHER: 'Mẹ',
  FAMILY: 'Gia đình',
  EXPERT: 'Chuyên gia',
  MODERATOR: 'Kiểm duyệt viên',
  CONTENT_ADMIN: 'Quản trị nội dung',
  SYSTEM_ADMIN: 'Quản trị hệ thống',
  PARTNER: 'Đối tác',
};

const ACTIVITY_LABELS: Record<AdminUserActivity['action'], string> = {
  USER_ACCOUNT_STATUS_CHANGED: 'Thay đổi trạng thái tài khoản',
  STAFF_ACCOUNT_CREATED: 'Tạo tài khoản nhân viên',
  ROLE_PERMISSION_UPDATED: 'Cập nhật vai trò và quyền',
};

const STAFF_ROLES: readonly StaffRole[] = ['MODERATOR', 'CONTENT_ADMIN', 'SYSTEM_ADMIN'];
const TABS = ['Quyền hạn', 'Phiên đăng nhập', 'Hoạt động quản trị'] as const;
type Tab = (typeof TABS)[number];

function formatDate(value: string | null): string {
  if (!value) return 'Chưa có';
  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

export default function UserDetailPage() {
  const { userId } = useParams<{ userId: string }>();
  const navigate = useNavigate();
  const { user: currentUser } = useAuth();
  const [user, setUser] = useState<AdminUserSummary | null>(null);
  const [sessions, setSessions] = useState<AdminUserSession[]>([]);
  const [activity, setActivity] = useState<AdminUserActivity[]>([]);
  const [activeTab, setActiveTab] = useState<Tab>('Quyền hạn');
  const [isLoading, setIsLoading] = useState(true);
  const [isTabLoading, setIsTabLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [tabError, setTabError] = useState<string | null>(null);
  const [showLockDialog, setShowLockDialog] = useState(false);

  const isSelf = Boolean(user && currentUser?.id === user.id);
  const canManageRole = Boolean(user && STAFF_ROLES.includes(user.role as StaffRole));

  const loadUser = useCallback(async () => {
    if (!userId) return;
    setIsLoading(true);
    setError(null);
    try {
      setUser(await getUser(userId));
    } catch {
      setError('Không thể tải thông tin người dùng hoặc tài khoản không còn tồn tại.');
    } finally {
      setIsLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    void loadUser();
  }, [loadUser]);

  useEffect(() => {
    if (!userId || activeTab === 'Quyền hạn') return;
    let cancelled = false;

    async function loadTab() {
      setIsTabLoading(true);
      setTabError(null);
      try {
        if (activeTab === 'Phiên đăng nhập') {
          const result = await getUserSessions(userId!, 0, 50);
          if (!cancelled) setSessions(result.content);
        } else {
          const result = await getUserActivity(userId!, 0, 50);
          if (!cancelled) setActivity(result.content);
        }
      } catch {
        if (!cancelled) setTabError('Không thể tải dữ liệu theo dõi. Vui lòng thử lại.');
      } finally {
        if (!cancelled) setIsTabLoading(false);
      }
    }

    void loadTab();
    return () => {
      cancelled = true;
    };
  }, [activeTab, userId]);

  async function updateAccess(request: { enabled?: boolean; locked?: boolean; reason: string }): Promise<boolean> {
    if (!user || isSelf) return false;
    setIsSubmitting(true);
    setError(null);
    try {
      setUser(await updateUserStatus(user.id, request));
      return true;
    } catch {
      setError('Không thể cập nhật trạng thái tài khoản. Vui lòng kiểm tra quyền và thử lại.');
      return false;
    } finally {
      setIsSubmitting(false);
    }
  }

  if (isLoading) {
    return (
      <div className="p-6 md:p-8 font-sans">
        <div className="h-40 animate-pulse rounded-2xl bg-surface-container-low" />
        <div className="mt-6 h-64 animate-pulse rounded-2xl bg-surface-container-low" />
      </div>
    );
  }

  if (!user) {
    return (
      <div className="p-6 md:p-8 font-sans">
        <div className="rounded-2xl border border-error-container bg-error-container/60 p-8 text-center text-error">
          <p className="font-bold text-base">{error}</p>
          <button
            type="button"
            onClick={() => navigate('/admin/users')}
            className="mt-4 py-2 px-5 rounded-full bg-surface border border-outline-variant text-sm font-semibold text-on-surface-variant hover:bg-surface-container-low cursor-pointer"
          >
            Quay lại quản lý người dùng
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 md:p-8 font-sans">
      {/* Back Button */}
      <div className="mb-6 flex items-center gap-3">
        <button
          type="button"
          onClick={() => navigate('/admin/users')}
          className="inline-flex items-center gap-2 py-1.5 px-3 rounded-full text-xs font-semibold bg-surface border border-outline-variant text-on-surface-variant hover:bg-surface-container-low cursor-pointer"
        >
          <span className="material-symbols-outlined text-base">arrow_back</span>
          Quản lý người dùng
        </button>
      </div>

      {error && (
        <div className="mb-6 rounded-2xl border border-error-container bg-error-container/60 p-4 text-sm text-error">
          {error}
        </div>
      )}

      {/* Profile Header Banner */}
      <section className="mb-6 rounded-2xl border border-surface-container-highest bg-surface p-6 shadow-md">
        <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
          <div className="flex min-w-0 items-center gap-5">
            <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-full bg-primary-container text-on-primary-container font-bold text-2xl shadow-inner">
              {user.name.charAt(0).toUpperCase()}
            </div>
            <div className="min-w-0">
              <div className="flex items-center gap-3">
                <h1 className="m-0 truncate text-2xl font-bold text-on-surface">{user.name}</h1>
                <span className="inline-flex items-center gap-1 py-1 px-3 rounded-full bg-surface-container-low text-primary text-xs font-semibold">
                  {ROLE_LABELS[user.role]}
                </span>
              </div>
              <p className="truncate text-sm text-on-surface-variant mt-1">{user.email}</p>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-3 self-start lg:self-auto">
            {user.locked ? (
              <span className="inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold bg-error-container text-error">
                Đã khóa quyền
              </span>
            ) : user.enabled ? (
              <span className="inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold bg-emerald-100 text-emerald-700">
                Đang hoạt động
              </span>
            ) : (
              <span className="inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold bg-amber-100 text-amber-700">
                Đã vô hiệu hóa
              </span>
            )}

            {canManageRole && (
              <button
                type="button"
                onClick={() => navigate(`/admin/users/${user.id}/role`)}
                disabled={isSelf}
                className="py-2.5 px-5 rounded-full bg-primary text-on-primary text-sm font-semibold hover:bg-primary/90 disabled:opacity-50 cursor-pointer inline-flex items-center gap-2"
              >
                <span className="material-symbols-outlined text-lg">manage_accounts</span>
                Cập nhật vai trò
              </button>
            )}

            <button
              type="button"
              onClick={() => {
                if (user.locked) {
                  void updateAccess({ locked: false, reason: 'System Admin mở khóa trực tiếp' });
                } else {
                  setShowLockDialog(true);
                }
              }}
              disabled={isSelf || isSubmitting}
              className={`py-2.5 px-5 rounded-full border text-sm font-semibold inline-flex items-center gap-2 disabled:opacity-50 ${
                user.locked
                  ? 'border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100 cursor-pointer'
                  : 'border-error-container bg-surface text-error hover:bg-error-container cursor-pointer'
              }`}
            >
              <span className="material-symbols-outlined text-lg">
                {user.locked ? 'lock_open' : 'lock'}
              </span>
              {user.locked ? 'Mở khóa' : 'Khóa tài khoản'}
            </button>

            <button
              type="button"
              onClick={() => void updateAccess({
                enabled: !user.enabled,
                reason: user.enabled ? 'System Admin vô hiệu hóa tài khoản' : 'System Admin kích hoạt lại tài khoản',
              })}
              disabled={isSelf || isSubmitting}
              className="py-2.5 px-5 rounded-full border border-outline-variant bg-surface text-sm font-semibold text-on-surface-variant inline-flex items-center gap-2 hover:bg-surface-container-low disabled:opacity-50"
            >
              <span className="material-symbols-outlined text-lg">{user.enabled ? 'block' : 'check_circle'}</span>
              {user.enabled ? 'Vô hiệu hóa' : 'Kích hoạt lại'}
            </button>
          </div>
        </div>

        {isSelf && (
          <div className="mt-5 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-xs text-amber-800 flex items-center gap-2">
            <span className="material-symbols-outlined text-base">info</span>
            Đây là tài khoản đang đăng nhập. Hệ thống đã vô hiệu hóa thao tác tự khóa, tự vô hiệu hóa và tự đổi vai trò để tránh mất quyền quản trị.
          </div>
        )}
      </section>

      {/* Tabs Navigation */}
      <div className="mb-6 flex items-center gap-2 border-b border-surface-container-highest pb-3">
        {TABS.map((tab) => (
          <button
            key={tab}
            type="button"
            onClick={() => setActiveTab(tab)}
            className={`py-2 px-5 rounded-full text-sm font-semibold cursor-pointer transition-colors flex items-center gap-2 ${
              activeTab === tab
                ? 'bg-primary text-on-primary shadow-sm'
                : 'bg-surface border border-outline-variant text-on-surface-variant hover:bg-surface-container-low'
            }`}
          >
            <span className="material-symbols-outlined text-lg">
              {tab === 'Quyền hạn' ? 'admin_panel_settings' : tab === 'Phiên đăng nhập' ? 'devices' : 'history'}
            </span>
            {tab}
          </button>
        ))}
      </div>

      {/* Tab Content Cards */}
      <div className="bg-surface rounded-2xl p-6 shadow-md border border-surface-container-highest">
        {activeTab === 'Quyền hạn' && (
          <div className="space-y-6">
            <div>
              <h3 className="text-base font-bold text-on-surface m-0 mb-1">Thông tin truy cập & Quyền hạn</h3>
              <p className="text-xs text-on-surface-variant">Chi tiết trạng thái tài khoản và vai trò trong hệ thống CareBridge.</p>
            </div>

            {user.locked && user.lockType === 'ADMIN' && user.lockReason && (
              <div className="rounded-2xl border border-error-container bg-error-container/35 p-4">
                <span className="block text-xs font-semibold uppercase tracking-wider text-error">Lý do khóa bởi System Admin</span>
                <p className="mb-0 mt-2 text-sm leading-6 text-on-surface">{user.lockReason}</p>
                <p className="mb-0 mt-2 text-xs text-on-surface-variant">Khóa lúc: {formatDate(user.lockedAt)}</p>
              </div>
            )}

            <div className="grid gap-4 md:grid-cols-2">
              <div className="rounded-2xl border border-surface-container-highest bg-surface-bright p-4">
                <span className="text-xs text-outline font-semibold uppercase tracking-wider block mb-1">Vai trò hiện tại</span>
                <span className="text-sm font-bold text-on-surface block">{ROLE_LABELS[user.role]}</span>
                <p className="text-xs text-on-surface-variant mt-1 m-0">Mã phân quyền: {user.role}</p>
              </div>

              <div className="rounded-2xl border border-surface-container-highest bg-surface-bright p-4">
                <span className="text-xs text-outline font-semibold uppercase tracking-wider block mb-1">Trạng thái đăng nhập</span>
                <span className="text-sm font-bold text-on-surface block">
                  {user.locked ? 'Đã bị khóa' : user.enabled ? 'Được phép đăng nhập' : 'Vô hiệu hóa'}
                </span>
                <p className="text-xs text-on-surface-variant mt-1 m-0">
                  Ngày khởi tạo: {formatDate(user.createdAt)}
                </p>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'Phiên đăng nhập' && (
          <div>
            <div className="mb-4">
              <h3 className="text-base font-bold text-on-surface m-0 mb-1">Danh sách phiên làm việc active</h3>
              <p className="text-xs text-on-surface-variant">Theo dõi thiết bị và địa chỉ IP đăng nhập vào hệ thống.</p>
            </div>

            {isTabLoading ? (
              <div className="py-12 text-center text-outline">Đang tải lịch sử phiên...</div>
            ) : tabError ? (
              <div className="rounded-2xl border border-error-container bg-error-container/60 p-4 text-sm text-error">{tabError}</div>
            ) : sessions.length === 0 ? (
              <div className="py-12 text-center text-outline">Không có phiên đăng nhập nào được ghi nhận.</div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full border-collapse">
                  <thead>
                    <tr className="border-b-2 border-surface-container-highest text-left">
                      {['THIẾT BỊ / TRÌNH DUYỆT', 'TRẠNG THÁI', 'LẦN HOẠT ĐỘNG CUỐI', 'HẠN PHIÊN'].map((h) => (
                        <th key={h} className="py-3 px-3 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {sessions.map((s) => (
                      <tr key={s.id} className="border-b border-surface-container-highest hover:bg-surface-bright">
                        <td className="py-3.5 px-3 text-sm font-semibold text-on-surface">{s.deviceName || 'Thiết bị không tên'}</td>
                        <td className="py-3.5 px-3 text-xs">
                          <span className="inline-flex items-center rounded-full px-2.5 py-0.5 font-semibold bg-emerald-100 text-emerald-700">
                            {s.status}
                          </span>
                        </td>
                        <td className="py-3.5 px-3 text-xs text-on-surface-variant">{formatDate(s.lastActivityAt || s.issuedAt)}</td>
                        <td className="py-3.5 px-3 text-xs text-on-surface-variant">{formatDate(s.expiresAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {activeTab === 'Hoạt động quản trị' && (
          <div>
            <div className="mb-4">
              <h3 className="text-base font-bold text-on-surface m-0 mb-1">Lịch sử tác động & Audit log</h3>
              <p className="text-xs text-on-surface-variant">Nhật ký thay đổi trạng thái và vai trò của tài khoản này.</p>
            </div>

            {isTabLoading ? (
              <div className="py-12 text-center text-outline">Đang tải lịch sử hoạt động...</div>
            ) : tabError ? (
              <div className="rounded-2xl border border-error-container bg-error-container/60 p-4 text-sm text-error">{tabError}</div>
            ) : activity.length === 0 ? (
              <div className="py-12 text-center text-outline">Chưa có nhật ký hoạt động quản trị.</div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full border-collapse">
                  <thead>
                    <tr className="border-b-2 border-surface-container-highest text-left">
                      {['HÀNH ĐỘNG', 'MÃ ACTOR', 'CHI TIẾT', 'THỜI GIAN'].map((h) => (
                        <th key={h} className="py-3 px-3 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {activity.map((act) => (
                      <tr key={act.id} className="border-b border-surface-container-highest hover:bg-surface-bright">
                        <td className="py-3.5 px-3 text-sm font-semibold text-on-surface">
                          {ACTIVITY_LABELS[act.action] || act.action}
                        </td>
                        <td className="py-3.5 px-3 text-xs font-mono text-on-surface-variant">{act.actorUserId || 'Hệ thống'}</td>
                        <td className="py-3.5 px-3 text-xs text-on-surface-variant">{act.details || '—'}</td>
                        <td className="py-3.5 px-3 text-xs text-on-surface-variant">{formatDate(act.timestamp)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}
      </div>

      <ConfirmDialog
        key={`lock-${user.id}-${showLockDialog ? 'open' : 'closed'}`}
        open={showLockDialog}
        title={`Khóa tài khoản ${user.name}?`}
        description="Mọi phiên đăng nhập đang hoạt động sẽ bị thu hồi. Sau khi nhập đúng mật khẩu, người dùng sẽ thấy lý do này và có thể gửi khiếu nại mở khóa."
        icon="lock"
        tone="danger"
        confirmLabel="Xác nhận khóa"
        reasonLabel="Lý do khóa bắt buộc"
        reasonPlaceholder="Mô tả rõ căn cứ khóa tài khoản..."
        submitting={isSubmitting}
        errorText={error ?? undefined}
        onCancel={() => setShowLockDialog(false)}
        onConfirm={(reason) => {
          if (!reason) return;
          void updateAccess({ locked: true, reason }).then((updated) => {
            if (updated) setShowLockDialog(false);
          });
        }}
      />
    </div>
  );
}
