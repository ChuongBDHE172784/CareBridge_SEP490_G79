import { useEffect, useState, useCallback, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../../shared/auth/useAuth';
import { searchUsers, updateUserStatus } from '../services/adminUserApi';
import type { AdminUserSummary, UpdateUserStatusRequest } from '../models/adminUser';
import type { UserRole } from '../../../shared/auth/authStore';
import ConfirmDialog from '../../../shared/components/ConfirmDialog';
import { SortableTableHeader, type SortDirection } from '../../contentManagement/components/SortableTableHeader';
import { nextSortDirection, sortRows } from '../../contentManagement/utils/tableSorting';

const ROLE_LABELS: Record<UserRole, string> = {
  MOTHER: 'Mẹ',
  FAMILY: 'Gia đình',
  EXPERT: 'Chuyên gia',
  MODERATOR: 'Kiểm duyệt viên',
  CONTENT_ADMIN: 'Quản trị nội dung',
  SYSTEM_ADMIN: 'Quản trị hệ thống',
};

type UserSortKey = 'name' | 'role' | 'status' | 'createdAt';

function maskName(name: string): string {
  const parts = name.trim().split(/\s+/);
  if (parts.length <= 1) return name;
  return parts
    .map((p, i) => (i === parts.length - 1 || i === 0 ? p : `${p[0]}***`))
    .join(' ');
}

function timeAgo(iso: string): string {
  const diffMs = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diffMs / 60000);
  if (mins < 1) return 'Vừa xong';
  if (mins < 60) return `${mins} phút trước`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours} giờ trước`;
  return `${Math.floor(hours / 24)} ngày trước`;
}

export default function UserListPage() {
  const navigate = useNavigate();
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState<AdminUserSummary[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [keyword, setKeyword] = useState('');
  const [roleFilter, setRoleFilter] = useState<UserRole | ''>('');
  const [statusFilter, setStatusFilter] = useState<'' | 'active' | 'disabled' | 'locked'>('');
  const [isLoading, setIsLoading] = useState(true);
  const [updatingAction, setUpdatingAction] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [lockTarget, setLockTarget] = useState<AdminUserSummary | null>(null);
  const [unlockTarget, setUnlockTarget] = useState<AdminUserSummary | null>(null);

  // Sorting state (default: createdAt desc - mới nhất lên đầu)
  const [sortKey, setSortKey] = useState<UserSortKey>('createdAt');
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc');

  // Global system counts (unfiltered)
  const [allUsersCount, setAllUsersCount] = useState<number | null>(null);
  const [activeUsersCount, setActiveUsersCount] = useState<number | null>(null);
  const [lockedUsersCount, setLockedUsersCount] = useState<number | null>(null);
  const [staffUsersCount, setStaffUsersCount] = useState<number | null>(null);

  const fetchStats = useCallback(async () => {
    try {
      const [allRes, activeRes, lockedRes, modRes, contentRes, sysRes] = await Promise.all([
        searchUsers({ page: 0, size: 1 }),
        searchUsers({ enabled: true, locked: false, page: 0, size: 1 }),
        searchUsers({ locked: true, page: 0, size: 1 }),
        searchUsers({ role: 'MODERATOR', page: 0, size: 1 }),
        searchUsers({ role: 'CONTENT_ADMIN', page: 0, size: 1 }),
        searchUsers({ role: 'SYSTEM_ADMIN', page: 0, size: 1 }),
      ]);
      setAllUsersCount(allRes.totalElements);
      setActiveUsersCount(activeRes.totalElements);
      setLockedUsersCount(lockedRes.totalElements);
      setStaffUsersCount(modRes.totalElements + contentRes.totalElements + sysRes.totalElements);
    } catch {
      // fallback silently
    }
  }, []);

  useEffect(() => {
    void fetchStats();
  }, [fetchStats]);

  const fetchData = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const normalizedKeyword = keyword.trim();
      const result = await searchUsers({
        email: normalizedKeyword.includes('@') ? normalizedKeyword : undefined,
        phone: /^\+?[0-9\s-]+$/.test(normalizedKeyword) && normalizedKeyword ? normalizedKeyword : undefined,
        name: normalizedKeyword && !normalizedKeyword.includes('@') && !/^\+?[0-9\s-]+$/.test(normalizedKeyword) ? normalizedKeyword : undefined,
        role: roleFilter || undefined,
        enabled: statusFilter === 'active' ? true : statusFilter === 'disabled' ? false : undefined,
        locked: statusFilter === 'active' ? false : statusFilter === 'locked' ? true : undefined,
        page,
        size: pageSize,
      });
      setUsers(result.content);
      setTotal(result.totalElements);
    } catch {
      setError('Không thể tải danh sách người dùng. Vui lòng thử lại.');
    } finally {
      setIsLoading(false);
    }
  }, [keyword, page, pageSize, roleFilter, statusFilter]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  async function updateAccess(
    target: AdminUserSummary,
    action: 'lock' | 'enabled',
    request: UpdateUserStatusRequest,
  ) {
    if (currentUser?.id === target.id) return;

    setUpdatingAction(`${target.id}:${action}`);
    setError(null);
    try {
      const updated = await updateUserStatus(target.id, request);
      setUsers((current) => current.map((user) => (user.id === updated.id ? updated : user)));
      void fetchStats();
    } catch {
      setError('Không thể cập nhật trạng thái tài khoản. Vui lòng kiểm tra quyền và thử lại.');
    } finally {
      setUpdatingAction(null);
    }
  }

  // Sorting logic matching ContentListPage & tableSorting utils
  const sortedUsers = useMemo(() => {
    return sortRows(users, sortDirection, (item) => {
      switch (sortKey) {
        case 'name':
          return item.name;
        case 'role':
          return ROLE_LABELS[item.role] || item.role;
        case 'status':
          return item.locked ? 'Bị khóa' : item.enabled ? 'Đang hoạt động' : 'Vô hiệu hóa';
        case 'createdAt': {
          const timestamp = new Date(item.createdAt || 0).getTime();
          return Number.isNaN(timestamp) ? 0 : timestamp;
        }
      }
    });
  }, [users, sortDirection, sortKey]);

  const changeSort = (key: UserSortKey) => {
    setSortDirection(nextSortDirection(sortKey, key, sortDirection));
    setSortKey(key);
  };

  const totalPages = Math.ceil(total / pageSize);
  const pageStart = total === 0 ? 0 : page * pageSize + 1;
  const pageEnd = Math.min((page + 1) * pageSize, total);

  return (
    <div className="p-6 md:p-8 font-sans">
      {/* Header */}
      <div className="mb-6 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-[26px] font-bold text-on-surface m-0">Quản lý Người dùng</h1>
          <p className="text-on-surface-variant text-sm mt-1">
            Quản lý tài khoản hệ thống, phân quyền nhân viên và theo dõi trạng thái truy cập của người dùng.
          </p>
        </div>
        <div className="flex items-center gap-2 self-start md:self-auto">
          <button
            type="button"
            onClick={() => { void fetchData(); void fetchStats(); }}
            disabled={isLoading}
            className="inline-flex items-center gap-2 py-2.5 px-5 rounded-full bg-surface border border-outline-variant text-on-surface-variant text-sm font-semibold cursor-pointer hover:bg-surface-container-low disabled:opacity-50"
          >
            <span className="material-symbols-outlined text-lg">refresh</span>
            Làm mới
          </button>
          <button
            type="button"
            onClick={() => navigate('/admin/staff-accounts/create')}
            className="inline-flex items-center gap-2 py-2.5 px-5 rounded-full bg-primary text-on-primary text-sm font-semibold cursor-pointer hover:bg-primary/90"
          >
            <span className="material-symbols-outlined text-lg">person_add</span>
            Tạo tài khoản nhân viên
          </button>
        </div>
      </div>

      {/* Stats Bar */}
      <div className="mb-6 grid gap-4 grid-cols-1 sm:grid-cols-2 lg:grid-cols-4">
        <div className="bg-surface rounded-2xl p-5 shadow-sm border border-surface-container-highest flex items-center justify-between">
          <div>
            <span className="text-xs font-semibold text-outline uppercase tracking-wider block mb-1">Tổng người dùng</span>
            <p className="text-2xl font-bold text-on-surface m-0">{allUsersCount ?? total}</p>
            <p className="mt-0.5 text-xs text-outline m-0">Toàn bộ hệ thống</p>
          </div>
          <span className="material-symbols-outlined text-3xl text-primary/70">group</span>
        </div>

        <div className="bg-surface rounded-2xl p-5 shadow-sm border border-surface-container-highest flex items-center justify-between">
          <div>
            <span className="text-xs font-semibold text-outline uppercase tracking-wider block mb-1">Đang hoạt động</span>
            <p className="text-2xl font-bold text-emerald-700 m-0">{activeUsersCount ?? '—'}</p>
            <p className="mt-0.5 text-xs text-outline m-0">Đủ điều kiện truy cập</p>
          </div>
          <span className="material-symbols-outlined text-3xl text-emerald-600/70">check_circle</span>
        </div>

        <div className="bg-surface rounded-2xl p-5 shadow-sm border border-surface-container-highest flex items-center justify-between">
          <div>
            <span className="text-xs font-semibold text-outline uppercase tracking-wider block mb-1">Bị khóa / Tạm dừng</span>
            <p className="text-2xl font-bold text-error m-0">{lockedUsersCount ?? '—'}</p>
            <p className="mt-0.5 text-xs text-outline m-0">Hạn chế quyền</p>
          </div>
          <span className="material-symbols-outlined text-3xl text-error/70">block</span>
        </div>

        <div className="bg-surface rounded-2xl p-5 shadow-sm border border-surface-container-highest flex items-center justify-between">
          <div>
            <span className="text-xs font-semibold text-outline uppercase tracking-wider block mb-1">Nhân viên hệ thống</span>
            <p className="text-2xl font-bold text-indigo-700 m-0">{staffUsersCount ?? '—'}</p>
            <p className="mt-0.5 text-xs text-outline m-0">Admin / Mod / Content</p>
          </div>
          <span className="material-symbols-outlined text-3xl text-indigo-600/70">badge</span>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="bg-surface rounded-2xl p-4 shadow-sm border border-surface-container-highest mb-6">
        <div className="flex flex-col md:flex-row items-center gap-3">
          <div className="flex-1 w-full relative">
            <span className="material-symbols-outlined text-outline absolute left-[14px] top-1/2 -translate-y-1/2 text-xl">search</span>
            <input
              type="text"
              value={keyword}
              onChange={(e) => { setKeyword(e.target.value); setPage(0); }}
              onKeyDown={(e) => { if (e.key === 'Enter') void fetchData(); }}
              placeholder="Tìm kiếm theo tên, email hoặc số điện thoại..."
              className="w-full pl-11 pr-4 py-2.5 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none focus:border-primary font-sans"
            />
          </div>

          <div className="flex items-center gap-3 w-full md:w-auto">
            <select
              value={roleFilter}
              onChange={(e) => { setRoleFilter(e.target.value as UserRole | ''); setPage(0); }}
              className="py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none cursor-pointer font-sans min-w-[150px]"
            >
              <option value="">Tất cả vai trò</option>
              {(Object.keys(ROLE_LABELS) as UserRole[]).map((r) => (
                <option key={r} value={r}>{ROLE_LABELS[r]}</option>
              ))}
            </select>

            <select
              value={statusFilter}
              onChange={(e) => { setStatusFilter(e.target.value as '' | 'active' | 'disabled' | 'locked'); setPage(0); }}
              className="py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none cursor-pointer font-sans min-w-[160px]"
            >
              <option value="">Tất cả trạng thái</option>
              <option value="active">Đang hoạt động</option>
              <option value="disabled">Vô hiệu hóa</option>
              <option value="locked">Bị khóa</option>
            </select>
          </div>
        </div>
      </div>

      {error && (
        <div className="mb-6 rounded-2xl border border-error-container bg-error-container/60 p-4 text-sm text-error">
          {error}
        </div>
      )}

      {/* User Table Card */}
      <div className="bg-surface rounded-2xl p-6 shadow-md border border-surface-container-highest">
        {isLoading ? (
          <div className="py-16 text-center text-outline">Đang tải danh sách người dùng...</div>
        ) : users.length === 0 ? (
          <div className="py-16 text-center text-outline">
            <span className="material-symbols-outlined text-4xl block mb-2">person_search</span>
            Không tìm thấy người dùng nào phù hợp với bộ lọc.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full border-collapse">
              <thead>
                <tr className="border-b-2 border-surface-container-highest text-left">
                  {(
                    [
                      ['name', 'NGƯỜI DÙNG'],
                      ['role', 'VAI TRÒ'],
                      ['status', 'TRẠNG THÁI'],
                      ['createdAt', 'NGÀY TẠO'],
                    ] as const
                  ).map(([key, label]) => (
                    <SortableTableHeader
                      key={key}
                      label={label}
                      active={sortKey === key}
                      direction={sortDirection}
                      onClick={() => changeSort(key)}
                    />
                  ))}
                  <th scope="col" className="py-3 px-3 text-[11px] font-semibold text-outline uppercase tracking-[0.05em] text-center">
                    THAO TÁC
                  </th>
                </tr>
              </thead>
              <tbody>
                {sortedUsers.map((item) => {
                  const isSelf = currentUser?.id === item.id;
                  const isLocking = updatingAction === `${item.id}:lock`;
                  const isEnabling = updatingAction === `${item.id}:enabled`;

                  return (
                    <tr key={item.id} className="border-b border-surface-container-highest hover:bg-surface-bright transition-colors">
                      <td className="py-3.5 px-3">
                        <div className="flex items-center gap-3">
                          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary-container text-on-primary-container font-bold text-sm">
                            {item.name.charAt(0).toUpperCase()}
                          </div>
                          <div>
                            <div className="font-semibold text-sm text-on-surface">{maskName(item.name)}</div>
                            <div className="text-xs text-on-surface-variant mt-0.5">{item.email}</div>
                          </div>
                        </div>
                      </td>
                      <td className="py-3.5 px-3">
                        <span className="inline-flex items-center gap-1 py-1 px-3 rounded-full bg-surface-container-low text-primary text-xs font-semibold">
                          {ROLE_LABELS[item.role]}
                        </span>
                      </td>
                      <td className="py-3.5 px-3">
                        {item.locked ? (
                          <span className="inline-flex items-center rounded-full px-3 py-0.5 text-xs font-semibold bg-error-container text-error">
                            Bị khóa
                          </span>
                        ) : item.enabled ? (
                          <span className="inline-flex items-center rounded-full px-3 py-0.5 text-xs font-semibold bg-emerald-100 text-emerald-700">
                            Đang hoạt động
                          </span>
                        ) : (
                          <span className="inline-flex items-center rounded-full px-3 py-0.5 text-xs font-semibold bg-amber-100 text-amber-700">
                            Vô hiệu hóa
                          </span>
                        )}
                      </td>
                      <td className="py-3.5 px-3 text-xs text-on-surface-variant">
                        {item.createdAt ? timeAgo(item.createdAt) : 'Chưa có'}
                      </td>
                      <td className="py-3.5 px-3 text-center">
                        <div className="flex items-center gap-1.5 justify-center">
                          <button
                            type="button"
                            onClick={() => navigate(`/admin/users/${item.id}`)}
                            className="h-8 py-1 px-3 rounded-lg border border-outline-variant bg-surface text-xs font-semibold text-primary inline-flex items-center gap-1 hover:bg-surface-container-low cursor-pointer"
                          >
                            <span className="material-symbols-outlined text-base">visibility</span>
                            Xem
                          </button>

                          <button
                            type="button"
                            onClick={() => {
                              if (item.locked) {
                                setUnlockTarget(item);
                              } else {
                                setLockTarget(item);
                              }
                            }}
                            disabled={isSelf || isLocking}
                            title={isSelf ? 'Không thể tự khóa tài khoản' : item.locked ? 'Mở khóa tài khoản' : 'Khóa tài khoản'}
                            className={`h-8 py-1 px-3 rounded-lg border text-xs font-semibold inline-flex items-center gap-1 disabled:cursor-not-allowed disabled:opacity-50 ${
                              item.locked
                                ? 'border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100 cursor-pointer'
                                : 'border-error-container bg-surface text-error hover:bg-error-container cursor-pointer'
                            }`}
                          >
                            <span className="material-symbols-outlined text-base">
                              {item.locked ? 'lock_open' : 'lock'}
                            </span>
                            {item.locked ? 'Mở khóa tài khoản' : 'Khóa tài khoản'}
                          </button>

                          <button
                            type="button"
                            onClick={() =>
                              updateAccess(item, 'enabled', {
                                enabled: !item.enabled,
                                reason: item.enabled ? 'Admin vô hiệu hóa tài khoản' : 'Admin kích hoạt tài khoản',
                              })
                            }
                            disabled={isSelf || isEnabling}
                            title={isSelf ? 'Không thể tự vô hiệu hóa' : item.enabled ? 'Tắt tài khoản' : 'Kích hoạt lại'}
                            className="h-8 py-1 px-3 rounded-lg border border-outline-variant bg-surface text-xs font-semibold text-on-surface-variant inline-flex items-center gap-1 hover:bg-surface-container-low cursor-pointer disabled:cursor-not-allowed disabled:opacity-50"
                          >
                            <span className="material-symbols-outlined text-base">
                              {item.enabled ? 'block' : 'check_circle'}
                            </span>
                            {item.enabled ? 'Vô hiệu hóa' : 'Kích hoạt lại'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        <div className="flex justify-between items-center mt-5 pt-4 border-t border-surface-container-highest">
          <span className="text-[13px] text-outline">
            Hiển thị {total === 0 ? 0 : pageStart}-{pageEnd} trong {total} kết quả
          </span>
          <div className="flex gap-1">
            <button
              type="button"
              disabled={page === 0}
              onClick={() => setPage((v) => Math.max(0, v - 1))}
              className={`w-9 h-9 rounded-full border border-outline-variant bg-surface flex items-center justify-center ${page === 0 ? 'opacity-40 cursor-default' : 'cursor-pointer'}`}
            >
              <span className="material-symbols-outlined text-primary text-lg">chevron_left</span>
            </button>

            {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
              const startPage = Math.max(0, Math.min(page - 2, totalPages - 5));
              const p = startPage + i;
              if (p >= totalPages || p < 0) return null;
              return (
                <button
                  key={p}
                  type="button"
                  onClick={() => setPage(p)}
                  className={`w-9 h-9 rounded-full text-sm font-semibold cursor-pointer flex items-center justify-center ${
                    page === p
                      ? 'border-0 bg-primary text-on-primary'
                      : 'border border-outline-variant bg-surface text-on-surface-variant hover:bg-surface-container-low'
                  }`}
                >
                  {p + 1}
                </button>
              );
            })}

            <button
              type="button"
              disabled={page >= totalPages - 1 || totalPages === 0}
              onClick={() => setPage((v) => Math.min(totalPages - 1, v + 1))}
              className={`w-9 h-9 rounded-full border border-outline-variant bg-surface flex items-center justify-center ${
                page >= totalPages - 1 || totalPages === 0 ? 'opacity-40 cursor-default' : 'cursor-pointer'
              }`}
            >
              <span className="material-symbols-outlined text-primary text-lg">chevron_right</span>
            </button>
          </div>
        </div>
      </div>

      <ConfirmDialog
        key={lockTarget?.id ?? 'lock-user'}
        open={lockTarget !== null}
        title={`Khóa tài khoản ${lockTarget?.name ?? ''}?`}
        description="Người dùng sẽ bị đăng xuất khỏi tất cả phiên, nhìn thấy lý do này sau khi nhập đúng mật khẩu và có thể gửi khiếu nại."
        icon="lock"
        tone="danger"
        confirmLabel="Khóa tài khoản"
        reasonLabel="Lý do khóa"
        reasonPlaceholder="Nhập lý do cụ thể để người dùng và quản trị viên xem xét..."
        submitting={lockTarget ? updatingAction === `${lockTarget.id}:lock` : false}
        onCancel={() => setLockTarget(null)}
        onConfirm={(reason) => {
          if (!lockTarget || !reason) return;
          void updateAccess(lockTarget, 'lock', { locked: true, reason })
            .then(() => setLockTarget(null));
        }}
      />

      <ConfirmDialog
        key={unlockTarget?.id ?? 'unlock-user'}
        open={unlockTarget !== null}
        title={`Mở khóa tài khoản ${unlockTarget?.name ?? ''}?`}
        description="Người dùng sẽ có thể đăng nhập và sử dụng lại hệ thống ngay sau khi mở khóa."
        icon="lock_open"
        confirmLabel="Xác nhận mở khóa"
        submitting={unlockTarget ? updatingAction === `${unlockTarget.id}:lock` : false}
        onCancel={() => setUnlockTarget(null)}
        onConfirm={() => {
          if (!unlockTarget) return;
          void updateAccess(unlockTarget, 'lock', { locked: false })
            .then(() => setUnlockTarget(null));
        }}
      />
    </div>
  );
}
