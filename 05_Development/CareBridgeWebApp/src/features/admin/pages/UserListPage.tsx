import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { searchUsers } from '../services/adminUserApi';
import type { AdminUserSummary } from '../models/adminUser';
import type { UserRole } from '../../../shared/auth/authStore';

const ROLE_LABELS: Record<UserRole, string> = {
  MOTHER: 'Người dùng',
  FAMILY: 'Người dùng',
  EXPERT: 'Chuyên gia',
  MODERATOR: 'Kiểm duyệt viên',
  CONTENT_ADMIN: 'Quản trị nội dung',
  SYSTEM_ADMIN: 'Quản trị hệ thống',
  PARTNER: 'Đối tác',
};

function maskName(name: string): string {
  // Mockup masks names for PII minimization on the list view (e.g. "Nguyễn V*** A*").
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
  const [users, setUsers] = useState<AdminUserSummary[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [roleFilter, setRoleFilter] = useState<UserRole | ''>('');
  const [statusFilter, setStatusFilter] = useState<'' | 'active' | 'locked'>('');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchData = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const result = await searchUsers({
        role: roleFilter || undefined,
        locked: statusFilter === '' ? undefined : statusFilter === 'locked',
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
  }, [page, pageSize, roleFilter, statusFilter]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  return (
    <div>
      <div className="flex justify-between items-start mb-6">
        <div>
          <h1 className="font-headline-lg text-headline-lg text-on-surface m-0">Quản lý Người dùng</h1>
          <p className="font-body-md text-body-md text-on-surface-variant mt-1">Danh sách chi tiết tài khoản hệ thống</p>
        </div>
        <button
          onClick={() => navigate('/admin/staff-accounts/create')}
          className="h-[52px] px-6 rounded-full bg-primary-container text-on-primary-container font-button text-button flex items-center justify-center gap-2 hover:opacity-90 transition-opacity"
        >
          <span className="material-symbols-outlined text-lg">person_add</span>
          Tạo tài khoản nhân viên
        </button>
      </div>

      <div className="bg-surface-container-lowest rounded-lg p-6 mb-6 shadow-[0px_4px_20px_rgba(90,70,63,0.06)] border border-surface-container-low">
        <h2 className="font-headline-md text-headline-md text-on-surface mb-4 flex items-center gap-2">
          <span className="material-symbols-outlined text-primary text-lg">filter_list</span>
          Bộ lọc
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <div>
            <label className="font-label-md text-label-md text-on-surface-variant block mb-1.5">Vai trò</label>
            <select
              value={roleFilter}
              onChange={(e) => { setRoleFilter(e.target.value as UserRole | ''); setPage(0); }}
              className="w-full px-4 py-3 rounded-lg border border-outline-variant text-sm bg-surface cursor-pointer"
            >
              <option value="">Tất cả</option>
              {(Object.keys(ROLE_LABELS) as UserRole[]).map((r) => (
                <option key={r} value={r}>{ROLE_LABELS[r]}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="font-label-md text-label-md text-on-surface-variant block mb-1.5">Trạng thái</label>
            <select
              value={statusFilter}
              onChange={(e) => { setStatusFilter(e.target.value as '' | 'active' | 'locked'); setPage(0); }}
              className="w-full px-4 py-3 rounded-lg border border-outline-variant text-sm bg-surface cursor-pointer"
            >
              <option value="">Tất cả</option>
              <option value="active">Hoạt động</option>
              <option value="locked">Đình chỉ</option>
            </select>
          </div>
        </div>
        <div className="flex justify-end gap-3 mt-4">
          <button
            onClick={() => { setRoleFilter(''); setStatusFilter(''); setPage(0); }}
            className="font-button text-button text-primary"
          >
            Xóa bộ lọc
          </button>
          <button
            onClick={fetchData}
            className="h-11 px-6 rounded-full bg-primary text-on-primary font-button text-button"
          >
            Áp dụng
          </button>
        </div>
      </div>

      <div className="bg-surface-container-lowest rounded-lg overflow-hidden shadow-[0px_4px_20px_rgba(90,70,63,0.06)] border border-surface-container-low">
        {isLoading ? (
          <div className="p-12 text-center text-on-surface-variant">Đang tải...</div>
        ) : error ? (
          <div className="p-12 text-center text-error">{error}</div>
        ) : users.length === 0 ? (
          <div className="p-12 text-center text-on-surface-variant">Không tìm thấy người dùng nào.</div>
        ) : (
          <>
            <table className="w-full border-collapse">
              <thead>
                <tr className="border-b border-outline-variant bg-surface-container-low">
                  {['Người dùng', 'Vai trò', 'Trạng thái', 'Hoạt động cuối', 'Thao tác'].map((h) => (
                    <th key={h} className="font-label-md text-label-md text-on-surface-variant text-left px-6 py-3 uppercase tracking-[0.04em]">
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-outline-variant">
                {users.map((u) => (
                  <tr
                    key={u.id}
                    onClick={() => navigate(`/admin/users/${u.id}`)}
                    className="hover:bg-surface-container-lowest transition-colors cursor-pointer"
                  >
                    <td className="px-6 py-4">
                      <div className="font-body-md text-body-md text-on-surface font-medium">{maskName(u.name)}</div>
                      <div className="font-body-md text-body-md text-on-surface-variant text-sm opacity-80">ID: #{u.id.slice(0, 8).toUpperCase()}</div>
                    </td>
                    <td className="px-6 py-4">
                      <span className="inline-flex items-center px-3 py-1 rounded-full bg-surface-variant text-on-surface-variant font-label-md text-label-md">
                        {ROLE_LABELS[u.role]}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      {u.locked ? (
                        <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full bg-error-container text-error font-label-md text-label-md">
                          <span className="w-1.5 h-1.5 rounded-full bg-error" /> Đình chỉ
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full bg-primary-container/20 text-primary font-label-md text-label-md">
                          <span className="w-1.5 h-1.5 rounded-full bg-primary" /> Hoạt động
                        </span>
                      )}
                    </td>
                    <td className="px-6 py-4 font-body-md text-body-md text-on-surface-variant">{timeAgo(u.createdAt)}</td>
                    <td className="px-6 py-4">
                      <button
                        onClick={(e) => { e.stopPropagation(); navigate(`/admin/users/${u.id}`); }}
                        className="material-symbols-outlined text-on-surface-variant"
                      >
                        more_vert
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="flex justify-between items-center px-6 py-4 font-body-md text-body-md text-on-surface-variant">
              <span>Hiển thị {page * pageSize + 1}-{Math.min((page + 1) * pageSize, total)} của {total} người dùng</span>
              <div className="flex gap-2">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className={`w-9 h-9 rounded-lg border border-outline-variant ${page === 0 ? 'text-outline-variant cursor-default' : 'text-on-surface-variant cursor-pointer'}`}
                >
                  <span className="material-symbols-outlined text-base">chevron_left</span>
                </button>
                <button
                  onClick={() => setPage((p) => p + 1)}
                  disabled={(page + 1) * pageSize >= total}
                  className={`w-9 h-9 rounded-lg border border-outline-variant ${(page + 1) * pageSize >= total ? 'text-outline-variant cursor-default' : 'text-on-surface-variant cursor-pointer'}`}
                >
                  <span className="material-symbols-outlined text-base">chevron_right</span>
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
