import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../shared/auth/useAuth';

const NAV_LINKS = [
  { to: '/admin/dashboard', label: 'Dashboard', roles: ['SYSTEM_ADMIN'] },
  // '/moderator' redirects to '/moderator/safety-cases', which both roles can access —
  // '/moderator/dashboard' itself is SYSTEM_ADMIN-only (CommunityDashboardPage), so it
  // must not be the link target for MODERATOR here.
  { to: '/moderator', label: 'Moderation Queue', roles: ['MODERATOR', 'SYSTEM_ADMIN'] },
  { to: '/content/dashboard', label: 'Tổng quan CMS', roles: ['CONTENT_ADMIN', 'SYSTEM_ADMIN'] },
  { to: '/content/list', label: 'Thư viện', roles: ['CONTENT_ADMIN', 'SYSTEM_ADMIN'] },
  { to: '/content/faq', label: 'FAQ', roles: ['CONTENT_ADMIN', 'SYSTEM_ADMIN'] },
  { to: '/content/checklists', label: 'Checklist', roles: ['CONTENT_ADMIN', 'SYSTEM_ADMIN'] },
  { to: '/content/topics', label: 'Danh mục', roles: ['MODERATOR', 'CONTENT_ADMIN'] },
  { to: '/expert/dashboard', label: 'Expert', roles: ['EXPERT'] },
  // route guard is PARTNER-only (see router/index.tsx) — SYSTEM_ADMIN has no access here.
  { to: '/partner/dashboard', label: 'Partner', roles: ['PARTNER'] },
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
  if (location.pathname.startsWith('/moderator')) {
    return <Outlet />;
  }

  return (
    <div className="flex min-h-screen font-sans">
      <aside className="w-64 fixed left-0 top-0 h-screen bg-surface flex flex-col py-6 px-3 border-r border-outline-variant">
        <div className="px-4 pb-5 text-lg font-extrabold text-primary">
          CareBridge
        </div>
        <nav className="flex-1">
          {visibleLinks.map((l) => (
            <NavLink
              key={l.to}
              to={l.to}
              className={({ isActive }) =>
                `block px-4 py-2.5 text-sm no-underline transition-colors rounded-xl ${
                  isActive
                    ? 'bg-surface-container-low text-primary font-semibold'
                    : 'text-on-surface-variant hover:bg-surface-container-low font-normal'
                }`
              }
            >
              {l.label}
            </NavLink>
          ))}
        </nav>
        <div className="px-4 pt-3 border-t border-outline-variant">
          <div className="text-xs text-outline mb-1.5">
            {user?.name ?? user?.phone} · {user?.role}
          </div>
          <button
            onClick={handleLogout}
            className="w-full py-2 rounded-xl border border-outline-variant bg-surface text-primary cursor-pointer text-sm font-semibold hover:bg-surface-container-low transition-colors"
          >
            Sign out
          </button>
        </div>
      </aside>
      <main className="ml-64 min-h-screen bg-background overflow-auto flex-1">
        <Outlet />
      </main>
    </div>
  );
}
