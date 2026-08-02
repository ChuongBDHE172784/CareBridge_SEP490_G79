import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../shared/auth/useAuth';

// Keep these roles aligned with the ProtectedRoute guards in app/router/index.tsx.
// Navigation is only a presentation concern; the guards remain authoritative.
const NAV_ITEMS = [
  { label: 'Tổng quan', icon: 'dashboard', path: '/content/dashboard', roles: ['CONTENT_ADMIN'] },
  { label: 'Thư viện nội dung', icon: 'folder', path: '/content/list', roles: ['CONTENT_ADMIN'] },
  { label: 'Bài viết', icon: 'article', path: '/content/articles', roles: ['CONTENT_ADMIN'] },
  { label: 'FAQ', icon: 'help', path: '/content/faq', roles: ['CONTENT_ADMIN'] },
  { label: 'Checklist', icon: 'checklist', path: '/content/checklists', roles: ['CONTENT_ADMIN'] },
  { label: 'Bài tập thai kỳ', icon: 'fitness_center', path: '/content/exercises', roles: ['CONTENT_ADMIN'] },
  { label: 'Chủ đề cộng đồng', icon: 'topic', path: '/content/topics', roles: ['CONTENT_ADMIN', 'MODERATOR'] },
  { label: 'Thông báo', icon: 'notifications', path: '/content/notifications', roles: ['CONTENT_ADMIN'] },
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
    <aside className="fixed left-0 top-0 z-20 hidden h-screen w-64 flex-col border-r border-outline-variant/70 bg-surface md:flex">
      <div className="border-b border-outline-variant/70 p-4">
        <div className="flex items-center gap-2 mb-1">
          <span className="material-symbols-outlined text-xl text-primary">article</span>
          <span className="text-sm font-semibold leading-none text-on-surface">ContentPortal</span>
        </div>
        <p className="ml-7 text-[11px] text-outline">Quản lý nội dung</p>
      </div>
      {hasRole('MODERATOR') && (
        <NavLink
          to="/moderator/reports"
          className="flex items-center gap-2 border-b border-outline-variant/70 px-4 py-2 text-xs font-semibold text-primary hover:bg-surface-container-low"
        >
          <span className="material-symbols-outlined text-[18px]">arrow_back</span>
          Về Kiểm duyệt
        </NavLink>
      )}
      <nav className="flex-1 space-y-0.5 overflow-y-auto p-3">
        {visibleItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) =>
              `flex items-center gap-2.5 rounded-md px-3 py-2 text-xs font-medium transition-colors ${
                isActive
                  ? 'bg-primary-container text-primary'
                  : 'text-on-surface-variant hover:bg-surface-container-low hover:text-on-surface'
              }`
            }
          >
            <span className="material-symbols-outlined text-[18px]">{item.icon}</span>
            {item.label}
          </NavLink>
        ))}
      </nav>
      <div className="space-y-2.5 border-t border-outline-variant/70 p-3">
        <div className="px-1 text-[11px] text-outline">
          <p className="truncate font-semibold text-on-surface-variant">{user?.name ?? user?.phone ?? 'Content Admin'}</p>
          <p>{user?.role ?? 'CONTENT_ADMIN'}</p>
        </div>
        <button
          type="button"
          onClick={handleLogout}
          className="flex w-full items-center justify-center gap-2 rounded-md border border-outline-variant bg-surface px-3 py-1.5 text-xs font-semibold text-on-surface-variant hover:bg-surface-container-low hover:text-on-surface"
        >
          <span className="material-symbols-outlined text-[16px]">logout</span>
          Đăng xuất
        </button>
        <p className="text-center text-[10px] text-outline">CareBridge © 2026</p>
      </div>
    </aside>
  );
}


