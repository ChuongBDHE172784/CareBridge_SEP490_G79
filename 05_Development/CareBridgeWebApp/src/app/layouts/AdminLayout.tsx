import { useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../shared/auth/useAuth';

type NavRole = 'SYSTEM_ADMIN' | 'EXPERT' | 'MODERATOR' | 'CONTENT_ADMIN' | 'PARTNER';
type NavLinkItem = {
  type?: 'link';
  to: string;
  label: string;
  icon: string;
  roles: readonly NavRole[];
};
type NavGroupItem = {
  type: 'group';
  label: string;
  icon: string;
  roles: readonly NavRole[];
  children: readonly NavLinkItem[];
};
type NavItem = NavLinkItem | NavGroupItem;

// Keep navigation icons and colors consistent across the administration portal.
const NAV_LINKS: readonly NavItem[] = [
  { to: '/admin/dashboard', label: 'Dashboard', icon: 'dashboard', roles: ['SYSTEM_ADMIN'] },
  { to: '/admin/users', label: 'Quản lý người dùng', icon: 'manage_accounts', roles: ['SYSTEM_ADMIN'] },
  { to: '/admin/account-lock-appeals', label: 'Khiếu nại khóa tài khoản', icon: 'gavel', roles: ['SYSTEM_ADMIN'] },
  {
    type: 'group',
    label: 'Quản lý chuyên gia',
    icon: 'medical_services',
    roles: ['SYSTEM_ADMIN'],
    children: [
      { to: '/admin/expert-verification-queue', label: 'Xét duyệt chuyên gia', icon: 'verified_user', roles: ['SYSTEM_ADMIN'] },
      { to: '/admin/expert-trust-management', label: 'Duyệt bài chuyên gia', icon: 'article', roles: ['SYSTEM_ADMIN'] },
    ],
  },
  {
    type: 'group',
    label: 'Hệ thống kiểm duyệt',
    icon: 'shield',
    roles: ['SYSTEM_ADMIN'],
    children: [
      { to: '/admin/safety-rules', label: 'AI & An toàn', icon: 'rule', roles: ['SYSTEM_ADMIN'] },
      { to: '/admin/system-configuration', label: 'Cấu hình hệ thống', icon: 'tune', roles: ['SYSTEM_ADMIN'] },
    ],
  },
  { to: '/admin/content-approval-queue', label: 'Duyệt nội dung', icon: 'fact_check', roles: ['SYSTEM_ADMIN'] },
  { to: '/admin/posture-configs', label: 'Cấu hình tư thế', icon: 'settings_accessibility', roles: ['SYSTEM_ADMIN'] },
  { to: '/admin/notifications', label: 'Thông báo', icon: 'notifications', roles: ['SYSTEM_ADMIN'] },
  { to: '/admin/settings/privacy', label: 'Quyền riêng tư', icon: 'privacy_tip', roles: ['SYSTEM_ADMIN'] },
  {
    type: 'group',
    label: 'Bảo mật',
    icon: 'security',
    roles: ['SYSTEM_ADMIN'],
    children: [
      { to: '/admin/security/incidents', label: 'Sự cố bảo mật', icon: 'security', roles: ['SYSTEM_ADMIN'] },
      { to: '/admin/security/events', label: 'Sự kiện bảo mật', icon: 'policy', roles: ['SYSTEM_ADMIN'] },
    ],
  },
  { to: '/expert/dashboard', label: 'Expert', icon: 'stethoscope', roles: ['EXPERT'] },
  // route guard is PARTNER-only (see router/index.tsx) — SYSTEM_ADMIN has no access here.
  { to: '/partner/dashboard', label: 'Partner', icon: 'handshake', roles: ['PARTNER'] },
  { to: '/moderator/moderator-dashboard', label: 'Tổng quan', icon: 'dashboard', roles: ['MODERATOR'] },
  { to: '/moderator/pending-content', label: 'Nội dung mới', icon: 'fact_check', roles: ['MODERATOR'] },
  { to: '/moderator/community-content', label: 'Theo dõi cộng đồng', icon: 'visibility', roles: ['MODERATOR'] },
  { to: '/moderator/reports', label: 'Báo cáo', icon: 'flag', roles: ['MODERATOR'] },
  { to: '/moderator/violations', label: 'Vi phạm', icon: 'gavel', roles: ['MODERATOR'] },
];

export default function AdminLayout() {
  const { user, logout, hasAnyRole } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const isModerator = user?.role === 'MODERATOR';
  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>(() => ({
    'Quản lý chuyên gia': location.pathname.startsWith('/admin/expert-'),
    'Hệ thống kiểm duyệt': [
      '/admin/safety-rules',
      '/admin/system-configuration',
    ].some((path) => location.pathname.startsWith(path)),
    'Bảo mật': location.pathname.startsWith('/admin/security/'),
  }));

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isVisible = (item: NavItem) => hasAnyRole(...(item.roles as Parameters<typeof hasAnyRole>));
  const visibleLinks = NAV_LINKS.filter(isVisible);

  if (location.pathname.startsWith('/content')) {
    return <Outlet />;
  }

  return (
    <div className="flex min-h-screen bg-background font-sans text-on-surface">
      <aside className="fixed left-0 top-0 z-20 hidden h-screen w-64 flex-col border-r border-outline-variant/70 bg-surface md:flex">
        <div className="border-b border-outline-variant/70 p-4">
          <div className="flex items-center gap-2 mb-1">
            <span className="material-symbols-outlined text-xl text-primary">
              {isModerator ? 'shield' : 'admin_panel_settings'}
            </span>
            <span className="text-sm font-semibold leading-none text-on-surface">CareBridge</span>
          </div>
          <p className="ml-7 text-[11px] text-outline">
            {isModerator ? 'Cổng kiểm duyệt nội dung' : 'Cổng quản trị hệ thống'}
          </p>
        </div>
        <nav className="flex-1 space-y-0.5 overflow-y-auto p-3">
          {visibleLinks.map((l) => {
            if (l.type === 'group') {
              const groupOpen = openGroups[l.label] ?? true;
              const groupActive = l.children.some((child) => location.pathname.startsWith(child.to));

              return (
                <div key={l.label} className="space-y-0.5">
                  <button
                    type="button"
                    onClick={() => setOpenGroups((groups) => ({ ...groups, [l.label]: !groupOpen }))}
                    aria-expanded={groupOpen}
                    className={`flex w-full items-center gap-2.5 rounded-md px-3 py-2 text-xs font-medium transition-colors ${
                      groupActive
                        ? 'bg-primary-container text-primary'
                        : 'text-on-surface-variant hover:bg-surface-container-low hover:text-on-surface'
                    }`}
                  >
                    <span className="material-symbols-outlined text-[18px]">{l.icon}</span>
                    <span className="min-w-0 flex-1 truncate text-left">{l.label}</span>
                    <span className="material-symbols-outlined text-[18px] transition-transform duration-200">
                      {groupOpen ? 'expand_less' : 'expand_more'}
                    </span>
                  </button>
                  {groupOpen && (
                    <div className="space-y-0.5 pl-5">
                      {l.children.filter(isVisible).map((child) => (
                        <NavLink
                          key={child.to}
                          to={child.to}
                          className={({ isActive }) =>
                            `flex items-center gap-2 rounded-md px-3 py-2 text-xs font-medium transition-colors ${
                              isActive
                                ? 'bg-primary-container text-primary'
                                : 'text-on-surface-variant hover:bg-surface-container-low hover:text-on-surface'
                            }`
                          }
                        >
                          <span className="material-symbols-outlined text-[16px]">{child.icon}</span>
                          <span className="truncate">{child.label}</span>
                        </NavLink>
                      ))}
                    </div>
                  )}
                </div>
              );
            }

            return (
              <NavLink
                key={l.to}
                to={l.to}
                className={({ isActive }) =>
                  `flex items-center gap-2.5 rounded-md px-3 py-2 text-xs font-medium transition-colors ${
                    isActive
                      ? 'bg-primary-container text-primary'
                      : 'text-on-surface-variant hover:bg-surface-container-low hover:text-on-surface'
                  }`
                }
              >
                <span className="material-symbols-outlined text-[18px]">{l.icon}</span>
                <span className="truncate">{l.label}</span>
              </NavLink>
            );
          })}
        </nav>
        <div className="space-y-2.5 border-t border-outline-variant/70 p-3">
          <div className="px-1 text-[11px] text-outline">
            <p className="truncate font-semibold text-on-surface-variant">{user?.name ?? user?.phone}</p>
            <p>{user?.role}</p>
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
      <main className="min-h-screen flex-1 overflow-auto bg-background md:ml-64">
        <Outlet />
      </main>
    </div>
  );
}
