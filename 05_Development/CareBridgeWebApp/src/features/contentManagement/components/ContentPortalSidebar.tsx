import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../shared/auth/useAuth';

// Keep these roles aligned with the ProtectedRoute guards in app/router/index.tsx.
// Navigation is only a presentation concern; the guards remain authoritative.
const NAV_ITEMS = [
  { label: 'Tổng quan', icon: 'dashboard', path: '/content/dashboard', roles: ['CONTENT_ADMIN', 'SYSTEM_ADMIN'] },
  { label: 'Thư viện nội dung', icon: 'folder', path: '/content/list', roles: ['CONTENT_ADMIN', 'SYSTEM_ADMIN'] },
  { label: 'Bài tập thai kỳ', icon: 'fitness_center', path: '/content/exercises', roles: ['CONTENT_ADMIN', 'SYSTEM_ADMIN'] },
  { label: 'FAQ', icon: 'help', path: '/content/faq', roles: ['CONTENT_ADMIN', 'SYSTEM_ADMIN'] },
  { label: 'Checklist', icon: 'checklist', path: '/content/checklists', roles: ['CONTENT_ADMIN', 'SYSTEM_ADMIN'] },
  { label: 'Danh mục', icon: 'category', path: '/content/categories', roles: ['CONTENT_ADMIN', 'SYSTEM_ADMIN'] },
  { label: 'Chủ đề cộng đồng', icon: 'topic', path: '/content/topics', roles: ['CONTENT_ADMIN', 'MODERATOR'] },
  { label: 'Hàng chờ duyệt', icon: 'fact_check', path: '/content/approval-queue', roles: ['SYSTEM_ADMIN'] },
  { label: 'Cấu hình tư thế', icon: 'settings_accessibility', path: '/posture-configs', roles: ['SYSTEM_ADMIN'] },
] as const;

export default function ContentPortalSidebar() {
  const { user, logout, hasAnyRole, hasRole } = useAuth();
  const navigate = useNavigate();

  const visibleItems = NAV_ITEMS.filter((item) =>
    hasAnyRole(...(item.roles as unknown as Parameters<typeof hasAnyRole>))
  );

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <aside className="w-64 fixed left-0 top-0 h-screen bg-white border-r border-[#FFE2D9] flex flex-col z-20">
      <div className="p-6 border-b border-[#FFE2D9]">
        <div className="flex items-center gap-2 mb-1">
          <span className="material-symbols-outlined text-[#845143] text-2xl">article</span>
          <span className="font-bold text-[#845143] text-lg leading-none">ContentPortal</span>
        </div>
        <p className="text-xs text-[#84736F] ml-8">Quản lý nội dung</p>
      </div>
      {hasRole('SYSTEM_ADMIN') && (
        <NavLink
          to="/admin/dashboard"
          className="flex items-center gap-2 px-6 py-3 text-sm font-medium text-[#845143] border-b border-[#FFE2D9] hover:bg-[#FFF8F6] transition-colors"
        >
          <span className="material-symbols-outlined text-[18px]">arrow_back</span>
          Về trang Admin
        </NavLink>
      )}
      {hasRole('MODERATOR') && (
        <NavLink
          to="/moderator"
          className="flex items-center gap-2 px-6 py-3 text-sm font-medium text-[#845143] border-b border-[#FFE2D9] hover:bg-[#FFF8F6] transition-colors"
        >
          <span className="material-symbols-outlined text-[18px]">arrow_back</span>
          Về Kiểm duyệt
        </NavLink>
      )}
      <nav className="flex-1 p-4 space-y-1">
        {visibleItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-colors ${
                isActive
                  ? 'bg-[#FFF1EC] text-[#845143]'
                  : 'text-[#524440] hover:bg-[#FFF8F6] hover:text-[#845143]'
              }`
            }
          >
            <span className="material-symbols-outlined text-[20px]">{item.icon}</span>
            {item.label}
          </NavLink>
        ))}
      </nav>
      <div className="p-4 border-t border-[#FFE2D9] space-y-3">
        <div className="text-xs text-[#84736F]">
          <p className="truncate font-medium text-[#524440]">{user?.name ?? user?.phone ?? 'Content Admin'}</p>
          <p>{user?.role ?? 'CONTENT_ADMIN'}</p>
        </div>
        <button
          type="button"
          onClick={handleLogout}
          className="w-full flex items-center justify-center gap-2 rounded-xl border border-[#FFD5C9] bg-white px-3 py-2 text-sm font-semibold text-[#845143] transition-colors hover:bg-[#FFF1EC]"
        >
          <span className="material-symbols-outlined text-[18px]">logout</span>
          Đăng xuất
        </button>
        <p className="text-xs text-[#84736F] text-center">CareBridge © 2025</p>
      </div>
    </aside>
  );
}
