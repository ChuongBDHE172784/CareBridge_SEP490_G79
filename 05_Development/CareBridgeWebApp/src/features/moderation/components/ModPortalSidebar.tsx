import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../shared/auth/useAuth';

// roles must mirror the ProtectedRoute guards in app/router/index.tsx for each path.
// ModerationController (pending-content/reports/violations) is
// @PreAuthorize("hasRole('MODERATOR')") on every endpoint on the backend — SYSTEM_ADMIN
// is NOT accepted there, so it must not see those entries (it would 403 on data load).
// RedFlagRuleController / CommunityDashboardController are the
// reverse: SYSTEM_ADMIN-only on the backend, so MODERATOR must not see those entries.
const NAV_ITEMS = [
  { label: 'Tổng quan', icon: 'dashboard', path: '/admin/moderator-dashboard', roles: ['SYSTEM_ADMIN'] },
  { label: 'Nội dung mới', icon: 'fact_check', path: '/admin/pending-content', roles: ['MODERATOR'] },
  { label: 'Báo cáo', icon: 'flag', path: '/admin/reports', roles: ['MODERATOR'] },
  { label: 'Vi phạm', icon: 'gavel', path: '/admin/violations', roles: ['MODERATOR'] },
  { label: 'AI & An toàn', icon: 'rule', path: '/admin/safety-rules', roles: ['SYSTEM_ADMIN'] },
  { label: 'Cấu hình hệ thống', icon: 'tune', path: '/admin/system-configuration', roles: ['SYSTEM_ADMIN'] },
] as const;

export default function ModPortalSidebar() {
  const { user, logout, hasAnyRole, hasRole } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  const visibleItems = NAV_ITEMS.filter((item) =>
    hasAnyRole(...(item.roles as unknown as Parameters<typeof hasAnyRole>))
  );

  return (
    <aside className="fixed left-0 top-0 z-20 hidden h-screen w-64 flex-col border-r border-outline-variant/70 bg-surface md:flex">
      <div className="border-b border-outline-variant/70 p-4">
        <div className="flex items-center gap-2 mb-1">
          <span className="material-symbols-outlined text-xl text-primary">shield</span>
          <span className="text-sm font-semibold leading-none text-on-surface">ModPortal</span>
        </div>
        <p className="ml-7 text-[11px] text-outline">Hệ thống kiểm duyệt</p>
      </div>
      {hasRole('SYSTEM_ADMIN') && (
        <NavLink
          to="/admin/dashboard"
          className="flex items-center gap-2 border-b border-outline-variant/70 px-4 py-2 text-xs font-semibold text-primary hover:bg-surface-container-low"
        >
          <span className="material-symbols-outlined text-[18px]">arrow_back</span>
          Về trang Admin
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
          <p className="truncate font-semibold text-on-surface-variant">{user?.name ?? user?.phone ?? 'Moderator'}</p>
          <p>{user?.role ?? 'MODERATOR'}</p>
        </div>
        <button
          type="button"
          onClick={handleLogout}
          className="flex w-full items-center justify-center gap-2 rounded-md border border-outline-variant bg-surface px-3 py-1.5 text-xs font-semibold text-on-surface-variant hover:bg-surface-container-low hover:text-on-surface"
        >
          <span className="material-symbols-outlined text-[16px]">logout</span>
          Đăng xuất
        </button>
        <p className="text-center text-[10px] text-outline">CareBridge © 2025</p>
      </div>
    </aside>
  );
}
