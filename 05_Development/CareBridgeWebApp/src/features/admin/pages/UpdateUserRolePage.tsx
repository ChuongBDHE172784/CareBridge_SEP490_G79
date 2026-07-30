import { type FormEvent, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../../../shared/auth/useAuth';
import { getUser, updateUserRole } from '../services/adminUserApi';
import type { AdminUserSummary, StaffRole } from '../models/adminUser';

const STAFF_ROLES: readonly StaffRole[] = ['MODERATOR', 'CONTENT_ADMIN', 'SYSTEM_ADMIN'];
const ROLE_OPTIONS: Array<{ value: StaffRole; label: string; description: string }> = [
  { value: 'MODERATOR', label: 'Kiểm duyệt viên', description: 'Xử lý báo cáo nội dung vi phạm, bài viết và bình luận.' },
  { value: 'CONTENT_ADMIN', label: 'Quản trị nội dung', description: 'Tạo, biên tập và duyệt kiến thức chăm sóc sức khỏe.' },
  { value: 'SYSTEM_ADMIN', label: 'Quản trị hệ thống', description: 'Toàn quyền quản lý tài khoản, cấu hình và phân quyền.' },
];

function isStaffRole(role: AdminUserSummary['role']): role is StaffRole {
  return STAFF_ROLES.includes(role as StaffRole);
}

export default function UpdateUserRolePage() {
  const { userId } = useParams<{ userId: string }>();
  const navigate = useNavigate();
  const { user: currentUser } = useAuth();
  const [target, setTarget] = useState<AdminUserSummary | null>(null);
  const [newRole, setNewRole] = useState<StaffRole>('MODERATOR');
  const [lockAccessRights, setLockAccessRights] = useState(false);
  const [reason, setReason] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!userId) return;
    getUser(userId)
      .then((user) => {
        setTarget(user);
        if (isStaffRole(user.role)) {
          setNewRole(user.role);
          setLockAccessRights(user.locked);
        }
      })
      .catch(() => setError('Không thể tải tài khoản cần cập nhật.'))
      .finally(() => setIsLoading(false));
  }, [userId]);

  const isSelf = Boolean(target && currentUser?.id === target.id);
  const isEligibleTarget = Boolean(target && isStaffRole(target.role));
  const hasChange = Boolean(target && isEligibleTarget && (newRole !== target.role || lockAccessRights !== target.locked));

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!target || isSelf || !isEligibleTarget || !hasChange) return;
    setIsSubmitting(true);
    setError(null);
    try {
      await updateUserRole(target.id, {
        newRole,
        lockAccessRights,
        reason: reason.trim() || undefined,
      });
      navigate(`/admin/users/${target.id}`, { replace: true });
    } catch {
      setError('Không thể cập nhật vai trò. Yêu cầu có thể vi phạm quy tắc tự bảo vệ hoặc dữ liệu đã thay đổi.');
    } finally {
      setIsSubmitting(false);
    }
  }

  if (isLoading) {
    return (
      <div className="p-6 md:p-8 font-sans">
        <div className="h-64 animate-pulse rounded-2xl bg-surface-container-low" />
      </div>
    );
  }

  if (!target) {
    return (
      <div className="p-6 md:p-8 font-sans">
        <div className="rounded-2xl border border-error-container bg-error-container/60 p-8 text-center text-error">
          <p className="font-bold text-base">{error}</p>
          <button
            type="button"
            onClick={() => navigate('/admin/users')}
            className="mt-4 py-2 px-5 rounded-full bg-surface border border-outline-variant text-sm font-semibold text-on-surface-variant hover:bg-surface-container-low cursor-pointer"
          >
            Quay lại danh sách
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 md:p-8 font-sans">
      <div className="mb-6 flex items-center gap-3">
        <button
          type="button"
          onClick={() => navigate(`/admin/users/${target.id}`)}
          className="inline-flex items-center gap-2 py-1.5 px-3 rounded-full text-xs font-semibold bg-surface border border-outline-variant text-on-surface-variant hover:bg-surface-container-low cursor-pointer"
        >
          <span className="material-symbols-outlined text-base">arrow_back</span>
          Chi tiết người dùng
        </button>
      </div>

      <div className="mx-auto max-w-2xl">
        <h1 className="text-[26px] font-bold text-on-surface m-0">Cập nhật vai trò & Phân quyền</h1>
        <p className="text-on-surface-variant text-sm mt-1">
          Thay đổi vai trò nhân viên và thiết lập quyền truy cập cho {target.name} ({target.email}).
        </p>

        {/* User Summary Strip */}
        <div className="mt-4 rounded-2xl border border-surface-container-highest bg-surface-bright p-4 flex items-center gap-4">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-primary-container text-on-primary-container font-bold text-lg">
            {target.name.charAt(0).toUpperCase()}
          </div>
          <div>
            <div className="font-bold text-sm text-on-surface">{target.name}</div>
            <div className="text-xs text-on-surface-variant">{target.email}</div>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="mt-6 space-y-6 rounded-2xl border border-surface-container-highest bg-surface p-6 md:p-8 shadow-md">
          {error && (
            <div className="rounded-2xl border border-error-container bg-error-container/60 p-4 text-sm text-error">
              {error}
            </div>
          )}

          {!isEligibleTarget && (
            <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-xs text-amber-800">
              Cập nhật vai trò chỉ áp dụng cho tài khoản nhân viên (Kiểm duyệt viên, Quản trị nội dung và Quản trị hệ thống).
            </div>
          )}

          {isSelf && (
            <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-xs text-amber-800">
              Không thể thay đổi vai trò hoặc khóa quyền của chính tài khoản đang đăng nhập.
            </div>
          )}

          {/* Role Selection Options */}
          <div className="space-y-3">
            <label className="block text-sm font-bold text-on-surface">Select Vai trò mới</label>
            <div className="grid gap-3">
              {ROLE_OPTIONS.map((option) => {
                const isSelected = newRole === option.value;
                return (
                  <label
                    key={option.value}
                    className={`cursor-pointer rounded-2xl p-4 transition-all border flex items-start gap-3 ${
                      isSelected
                        ? 'border-primary bg-primary/5 shadow-sm'
                        : 'border-surface-container-highest bg-surface hover:bg-surface-bright'
                    } ${isSelf || !isEligibleTarget ? 'opacity-50 cursor-not-allowed' : ''}`}
                  >
                    <input
                      type="radio"
                      name="newRole"
                      value={option.value}
                      checked={isSelected}
                      disabled={isSelf || !isEligibleTarget}
                      onChange={() => setNewRole(option.value)}
                      className="mt-1 accent-primary"
                    />
                    <div>
                      <div className="font-bold text-sm text-on-surface">{option.label}</div>
                      <div className="text-xs text-on-surface-variant leading-relaxed mt-0.5">{option.description}</div>
                    </div>
                  </label>
                );
              })}
            </div>
          </div>

          {/* Lock Access Rights Option */}
          <label
            className={`flex items-start gap-3 rounded-2xl border p-4 transition-colors cursor-pointer ${
              lockAccessRights ? 'border-error-container bg-error-container/20' : 'border-surface-container-highest bg-surface'
            } ${isSelf || !isEligibleTarget ? 'opacity-50 cursor-not-allowed' : ''}`}
          >
            <input
              type="checkbox"
              checked={lockAccessRights}
              disabled={isSelf || !isEligibleTarget}
              onChange={(e) => setLockAccessRights(e.target.checked)}
              className="mt-1 accent-error"
            />
            <div>
              <span className="block font-bold text-sm text-on-surface">Khóa quyền truy cập sau khi đổi vai trò</span>
              <span className="mt-0.5 block text-xs text-on-surface-variant">
                Dùng khi cần tạm dừng đăng nhập trong thời gian bàn giao công việc hoặc điều tra sự cố.
              </span>
            </div>
          </label>

          {/* Reason input */}
          <label className="grid gap-2 text-sm font-bold text-on-surface">
            Lý do thay đổi (đã lưu vào Audit log)
            <textarea
              value={reason}
              disabled={isSelf || !isEligibleTarget}
              onChange={(e) => setReason(e.target.value)}
              maxLength={500}
              rows={4}
              placeholder="Nhập lý do phân công hoặc điều chỉnh vai trò nhân viên..."
              className="resize-y rounded-2xl border border-outline-variant bg-surface p-3.5 font-normal text-sm outline-none focus:border-primary disabled:opacity-50"
            />
            <span className="text-right text-xs font-normal text-outline">{reason.length}/500</span>
          </label>

          {/* Form Actions */}
          <div className="flex items-center justify-end gap-3 pt-4 border-t border-surface-container-highest">
            <button
              type="button"
              onClick={() => navigate(`/admin/users/${target.id}`)}
              className="py-2.5 px-5 rounded-full border border-outline-variant bg-surface text-sm font-semibold text-on-surface-variant hover:bg-surface-container-low cursor-pointer"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSelf || !isEligibleTarget || !hasChange || isSubmitting}
              className="py-2.5 px-6 rounded-full bg-primary text-on-primary text-sm font-semibold hover:bg-primary/90 disabled:opacity-50 cursor-pointer inline-flex items-center gap-2"
            >
              {isSubmitting ? (
                <>
                  <span className="material-symbols-outlined text-lg animate-spin">progress_activity</span>
                  Đang cập nhật...
                </>
              ) : (
                'Lưu thay đổi'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
