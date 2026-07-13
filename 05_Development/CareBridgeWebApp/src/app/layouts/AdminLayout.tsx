import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../shared/auth/useAuth';

// Icon/color palette mirrors ModPortalSidebar (features/moderation/components/ModPortalSidebar.tsx)
// so the Admin and ModPortal sidebars read as one consistent design system.
const NAV_LINKS = [
  { to: '/admin/dashboard', label: 'Dashboard', icon: 'dashboard', roles: ['SYSTEM_ADMIN'] },
  { to: '/admin/expert-verification-queue', label: 'Xét duyệt chuyên gia', icon: 'verified_user', roles: ['SYSTEM_ADMIN'] },
  { to: '/admin/expert-trust-management', label: 'Tin cậy chuyên gia', icon: 'health_and_safety', roles: ['SYSTEM_ADMIN'] },
  { to: '/admin/content-approval-queue', label: 'Duyệt nội dung', icon: 'fact_check', roles: ['SYSTEM_ADMIN'] },
  { to: '/admin/partners/verification', label: 'Xét duyệt đối tác', icon: 'handshake', roles: ['SYSTEM_ADMIN'] },
  { to: '/posture-configs', label: 'Cấu hình tư thế', icon: 'settings_accessibility', roles: ['SYSTEM_ADMIN'] },
  { to: '/moderator/dashboard', label: 'Tổng quan cộng đồng', icon: 'groups', roles: ['SYSTEM_ADMIN'] },
  { to: '/moderator/safety-rules', label: 'Quy tắc AI', icon: 'rule', roles: ['SYSTEM_ADMIN'] },
  { to: '/moderator/impact-report', label: 'Báo cáo tác động', icon: 'insights', roles: ['SYSTEM_ADMIN'] },
  { to: '/moderator/system-configuration', label: 'Cấu hình hệ thống', icon: 'tune', roles: ['SYSTEM_ADMIN'] },
  { to: '/security/incidents', label: 'Sự cố bảo mật', icon: 'security', roles: ['SYSTEM_ADMIN'] },
  { to: '/security/events', label: 'Sự kiện bảo mật', icon: 'policy', roles: ['SYSTEM_ADMIN'] },
  { to: '/notifications', label: 'Thông báo', icon: 'notifications', roles: ['SYSTEM_ADMIN'] },
  { to: '/settings/privacy', label: 'Quyền riêng tư', icon: 'privacy_tip', roles: ['SYSTEM_ADMIN'] },
  { to: '/expert/dashboard', label: 'Expert', icon: 'stethoscope', roles: ['EXPERT'] },
  // route guard is PARTNER-only (see router/index.tsx) — SYSTEM_ADMIN has no access here.
  { to: '/partner/dashboard', label: 'Partner', icon: 'handshake', roles: ['PARTNER'] },
] as const;

export default function AdminLayout() {
  const { user, logout, hasAnyRole } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const visibleLinks = NAV_LINKS.filter((l) =>
    hasAnyRole(...(l.roles as unknown as Parameters<typeof hasAnyRole>))
  );

  // ModPortal pages (/moderator/*) render their own full-page sidebar (ModPortalSidebar) —
  // skip this layout's sidebar/margin here to avoid a double ml-64 offset (empty gap bug).
  if (
    location.pathname.startsWith('/moderator') || location.pathname.startsWith('/content')
  ) {
    return <Outlet />;
  }

  return (
    <div className="flex min-h-screen font-sans">
      <aside className="w-64 fixed left-0 top-0 h-screen bg-white border-r border-[#FFE2D9] flex flex-col z-20">
        <div className="p-6 border-b border-[#FFE2D9]">
          <div className="flex items-center gap-2 mb-1">
            <span className="material-symbols-outlined text-[#845143] text-2xl">admin_panel_settings</span>
            <span className="font-bold text-[#845143] text-lg leading-none">CareBridge</span>
          </div>
          <p className="text-xs text-[#84736F] ml-8">Cổng quản trị hệ thống</p>
        </div>
        <nav className="flex-1 p-4 space-y-1">
          {visibleLinks.map((l) => (
            <NavLink
              key={l.to}
              to={l.to}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-[#FFF1EC] text-[#845143]'
                    : 'text-[#524440] hover:bg-[#FFF8F6] hover:text-[#845143]'
                }`
              }
            >
              <span className="material-symbols-outlined text-[20px]">{l.icon}</span>
              {l.label}
            </NavLink>
          ))}
        </nav>
        <div className="p-4 border-t border-[#FFE2D9] space-y-3">
          <div className="text-xs text-[#84736F]">
            <p className="truncate font-medium text-[#524440]">{user?.name ?? user?.phone}</p>
            <p>{user?.role}</p>
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
      <main className="ml-64 min-h-screen bg-background overflow-auto flex-1">
        <Outlet />
      </main>
    </div>
  );
}
