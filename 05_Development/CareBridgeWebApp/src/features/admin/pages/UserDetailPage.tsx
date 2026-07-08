import { useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { updateUserStatus } from '../services/adminUserApi';
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

const TABS = ['Quyền hạn', 'Phiên đăng nhập', 'Lịch sử báo cáo', 'Hoạt động'] as const;
type Tab = (typeof TABS)[number];

export default function UserDetailPage() {
  const { userId } = useParams<{ userId: string }>();
  const location = useLocation();
  const navigate = useNavigate();

  // No GET-by-id endpoint exists for UC114 (AdminUserController only exposes list-search).
  // Real data is passed via router state from UserListPage's row click; a direct/refreshed
  // visit has no way to re-fetch a single record and shows a graceful fallback instead of
  // fabricating data.
  const initialUser = (location.state as { user?: AdminUserSummary } | null)?.user;
  const [user, setUser] = useState<AdminUserSummary | undefined>(initialUser);
  const [activeTab, setActiveTab] = useState<Tab>('Quyền hạn');
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!user) {
    return (
      <div className="bg-white rounded-xl shadow-[0px_4px_20px_rgba(90,70,63,0.06)] p-12 text-center border border-surface-container">
        <p className="font-body-lg text-body-lg text-on-surface-variant mb-4">
          Không có dữ liệu người dùng cho ID <code>{userId}</code>. Vui lòng quay lại danh sách và chọn người dùng.
        </p>
        <button
          onClick={() => navigate('/admin/users')}
          className="font-button text-button text-primary"
        >
          ← Quay lại Quản lý Người dùng
        </button>
      </div>
    );
  }

  async function toggleLock() {
    if (!user) return;
    setIsSubmitting(true);
    try {
      const updated = await updateUserStatus(user.id, {
        locked: !user.locked,
        reason: user.locked ? 'Reactivated by admin' : 'Suspended by admin',
      });
      setUser(updated);
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div>
      <button onClick={() => navigate('/admin/users')} className="flex items-center gap-2 font-button text-button text-on-surface-variant mb-4">
        <span className="material-symbols-outlined text-lg">arrow_back</span>
        Chi tiết người dùng
      </button>

      <div className="bg-white rounded-xl shadow-[0px_4px_20px_rgba(90,70,63,0.06)] p-6 border border-surface-container mb-6">
        <div className="flex items-center gap-4 mb-4">
          <div className="w-16 h-16 rounded-full bg-primary-container flex items-center justify-center font-headline-md text-headline-md text-on-primary-container">
            {user.name.charAt(0).toUpperCase()}
          </div>
          <div>
            <h1 className="font-headline-lg text-headline-lg text-on-surface m-0">{user.name}</h1>
            <p className="font-body-md text-body-md text-on-surface-variant">
              ID: {user.id} • Tham gia: {new Date(user.createdAt).toLocaleDateString('vi-VN')}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <span className="inline-flex items-center px-3 py-1 rounded-full bg-surface-variant text-on-surface-variant font-label-md text-label-md">
            {ROLE_LABELS[user.role]}
          </span>
          {user.locked ? (
            <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full bg-error-container text-error font-label-md text-label-md">Đình chỉ</span>
          ) : (
            <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full bg-primary-container/20 text-primary font-label-md text-label-md">
              {user.enabled ? 'Hoạt động' : 'Vô hiệu hóa'}
            </span>
          )}
        </div>
        <div className="flex gap-3 mt-5">
          <button
            onClick={() => navigate(`/admin/users/${user.id}/role`, { state: { user } })}
            className="h-11 px-5 rounded-full bg-primary-container text-on-primary-container font-button text-button flex items-center gap-2"
          >
            <span className="material-symbols-outlined text-lg">edit</span>
            Cập nhật quyền
          </button>
          <button
            onClick={toggleLock}
            disabled={isSubmitting}
            className="h-11 px-5 rounded-full bg-error-container text-error font-button text-button flex items-center gap-2 disabled:opacity-50"
          >
            <span className="material-symbols-outlined text-lg">{user.locked ? 'lock_open' : 'block'}</span>
            {user.locked ? 'Kích hoạt lại' : 'Đình chỉ'}
          </button>
        </div>
      </div>

      <div className="flex gap-6 border-b border-surface-container-high mb-6">
        {TABS.map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={
              activeTab === tab
                ? 'font-button text-button text-primary border-b-2 border-primary pb-2 px-1 whitespace-nowrap'
                : 'font-button text-button text-on-surface-variant hover:text-on-surface pb-2 px-1 whitespace-nowrap'
            }
          >
            {tab}
          </button>
        ))}
      </div>

      {activeTab === 'Quyền hạn' ? (
        <div className="bg-white rounded-xl shadow-[0px_4px_20px_rgba(90,70,63,0.06)] p-6 border border-surface-container">
          <h2 className="font-headline-md text-headline-md text-on-surface mb-4">Vai trò &amp; Trạng thái Truy cập</h2>
          <div className="flex justify-between items-center py-3 border-b border-surface-container-high">
            <div>
              <p className="font-body-md text-body-md text-on-surface">Vai trò hiện tại</p>
              <p className="font-body-md text-body-md text-on-surface-variant opacity-50">Quyết định các chức năng người dùng được truy cập</p>
            </div>
            <span className="font-body-lg text-body-lg font-medium text-on-surface">{ROLE_LABELS[user.role]}</span>
          </div>
          <div className="flex justify-between items-center py-3">
            <div>
              <p className="font-body-md text-body-md text-on-surface">Khóa truy cập</p>
              <p className="font-body-md text-body-md text-on-surface-variant opacity-50">Ngăn người dùng đăng nhập vào hệ thống</p>
            </div>
            <span className="font-body-lg text-body-lg font-medium text-on-surface">{user.locked ? 'Đã khóa' : 'Không khóa'}</span>
          </div>
          <p className="font-body-md text-body-md text-on-surface-variant mt-4">
            Bấm "Cập nhật quyền" ở trên để thay đổi vai trò hoặc khóa/mở quyền truy cập.
          </p>
        </div>
      ) : (
        <div className="bg-surface-container-low rounded-lg p-6 text-center">
          <p className="font-body-md text-body-md text-on-surface-variant">
            Tính năng "{activeTab}" sắp ra mắt — chưa có API hỗ trợ cho tab này.
          </p>
        </div>
      )}
    </div>
  );
}
