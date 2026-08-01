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

const ROLE_LABELS: Record<string, string> = {
  SYSTEM_ADMIN: 'Quản trị viên',
  MODERATOR: 'Kiểm duyệt viên',
  EXPERT: 'Chuyên gia Y tế',
  CONTENT_ADMIN: 'Quản lý Nội dung',
  PARTNER: 'Đối tác CareBridge',
};

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
  const isPartner = user?.role === 'PARTNER';

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

  const portalIcon = isModerator ? 'shield' : isPartner ? 'handshake' : 'admin_panel_settings';
  const portalSubtitle = isModerator
    ? 'Cổng Kiểm duyệt Nội dung'
    : isPartner
      ? 'Cổng Đối tác CareBridge'
      : 'Cổng Quản trị Hệ thống';

  return (
    <div className="flex min-h-screen bg-background font-sans text-on-surface">
      <aside className="fixed left-0 top-0 z-20 hidden h-screen w-64 flex-col border-r border-outline-variant/70 bg-surface md:flex">
        {/* Brand Header */}
        <div className="border-b border-outline-variant/70 p-4">
          <div className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary text-on-primary font-bold shadow-sm shrink-0">
              <span className="material-symbols-outlined text-xl">{portalIcon}</span>
            </div>
            <div>
              <span className="text-sm font-bold leading-none text-on-surface block">CareBridge</span>
              <span className="text-[11px] text-outline mt-0.5 block font-medium">{portalSubtitle}</span>
            </div>
          </div>
        </div>

        {/* Nav Items */}
        <nav className="flex-1 space-y-1 overflow-y-auto p-3">
          {visibleLinks.map((l) => {
            if (l.type === 'group') {
              const groupOpen = openGroups[l.label] ?? true;
              const groupActive = l.children.some((child) => location.pathname.startsWith(child.to));

              return (
                <div key={l.label} className="space-y-1">
                  <button
                    type="button"
                    onClick={() => setOpenGroups((groups) => ({ ...groups, [l.label]: !groupOpen }))}
                    aria-expanded={groupOpen}
                    className={`flex w-full items-center gap-2.5 rounded-xl px-3.5 py-2.5 text-xs font-semibold transition-all ${
                      groupActive
                        ? 'bg-primary-container text-primary font-bold shadow-sm'
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
                    <div className="space-y-1 pl-4">
                      {l.children.filter(isVisible).map((child) => (
                        <NavLink
                          key={child.to}
                          to={child.to}
                          className={({ isActive }) =>
                            `flex items-center gap-2 rounded-xl px-3 py-2 text-xs font-semibold transition-all ${
                              isActive
                                ? 'bg-primary-container text-primary font-bold shadow-sm'
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
                  `flex items-center gap-2.5 rounded-xl px-3.5 py-2.5 text-xs font-semibold transition-all ${
                    isActive
                      ? 'bg-primary-container text-primary font-bold shadow-sm'
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

        {/* Footer User Profile Card */}
        <div className="space-y-3 border-t border-outline-variant/70 p-3">
          <div className="flex items-center gap-2.5 rounded-2xl bg-surface-container-low p-2.5 border border-outline-variant/40">
            <div className="flex h-9 w-9 items-center justify-center rounded-full bg-primary-container text-primary font-bold text-xs shrink-0">
              {user?.name?.charAt(0)?.toUpperCase() || user?.phone?.charAt(0) || 'A'}
            </div>
            <div className="overflow-hidden text-[11px] text-outline flex-1 min-w-0">
              <p className="truncate font-bold text-xs text-on-surface">{user?.name || user?.phone || 'Quản trị viên'}</p>
              <span className="inline-block py-0.5 px-2 rounded-full bg-surface-container text-primary font-semibold text-[10px] mt-0.5">
                {ROLE_LABELS[user?.role || ''] || user?.role || 'SYSTEM_ADMIN'}
              </span>
            </div>
          </div>

          <button
            type="button"
            onClick={handleLogout}
            className="flex w-full items-center justify-center gap-2 rounded-full border border-outline-variant bg-surface py-2 text-xs font-semibold text-on-surface-variant hover:bg-surface-container-low hover:text-on-surface cursor-pointer transition-colors"
          >
            <span className="material-symbols-outlined text-[16px]">logout</span>
            Đăng xuất
          </button>
          <p className="text-center text-[10px] text-outline font-medium">CareBridge © 2026</p>
        </div>
      </aside>
      <main className="min-h-screen flex-1 overflow-auto bg-background md:ml-64">
        <Outlet />
      </main>
    </div>
  );
}

