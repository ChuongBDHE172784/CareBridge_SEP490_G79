import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../shared/auth/useAuth';

// roles must mirror the ProtectedRoute guards in app/router/index.tsx for each path.
// ModerationController (queue/pending-content/reports/violations/safety-cases) is
// @PreAuthorize("hasRole('MODERATOR')") on every endpoint on the backend — SYSTEM_ADMIN
// is NOT accepted there, so it must not see those entries (it would 403 on data load).
// RedFlagRuleController / CommunityDashboardController / ImpactReportController are the
// reverse: SYSTEM_ADMIN-only on the backend, so MODERATOR must not see those entries.
const NAV_ITEMS = [
  { label: 'Tổng quan', icon: 'dashboard', path: '/moderator/dashboard', roles: ['SYSTEM_ADMIN'] },
  { label: 'Hàng đợi', icon: 'queue', path: '/moderator/queue', roles: ['MODERATOR'] },
  { label: 'Nội dung mới', icon: 'fact_check', path: '/moderator/pending-content', roles: ['MODERATOR'] },
  { label: 'Báo cáo', icon: 'flag', path: '/moderator/reports', roles: ['MODERATOR'] },
  { label: 'Vi phạm', icon: 'gavel', path: '/moderator/violations', roles: ['MODERATOR'] },
  { label: 'Ca an toàn', icon: 'health_and_safety', path: '/moderator/safety-cases', roles: ['MODERATOR'] },
  { label: 'Quy tắc AI', icon: 'rule', path: '/moderator/safety-rules', roles: ['SYSTEM_ADMIN'] },
  { label: 'Tác động & vận hành', icon: 'insights', path: '/moderator/impact-report', roles: ['SYSTEM_ADMIN'] },
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
    <aside className="w-64 fixed left-0 top-0 h-screen bg-white border-r border-[#FFE2D9] flex flex-col z-20">
      <div className="p-6 border-b border-[#FFE2D9]">
        <div className="flex items-center gap-2 mb-1">
          <span className="material-symbols-outlined text-[#845143] text-2xl">shield</span>
          <span className="font-bold text-[#845143] text-lg leading-none">ModPortal</span>
        </div>
        <p className="text-xs text-[#84736F] ml-8">Hệ thống kiểm duyệt</p>
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
          <p className="truncate font-medium text-[#524440]">{user?.name ?? user?.phone ?? 'Moderator'}</p>
          <p>{user?.role ?? 'MODERATOR'}</p>
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
