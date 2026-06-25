import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../../shared/auth/useAuth';

const NAV_LINKS = [
  { to: '/admin/dashboard', label: 'Dashboard', roles: ['SYSTEM_ADMIN'] },
  { to: '/moderator/dashboard', label: 'Moderation Queue', roles: ['MODERATOR', 'SYSTEM_ADMIN'] },
  { to: '/content/dashboard', label: 'Content', roles: ['CONTENT_ADMIN', 'SYSTEM_ADMIN'] },
  { to: '/expert/dashboard', label: 'Expert', roles: ['EXPERT'] },
  { to: '/partner/dashboard', label: 'Partner', roles: ['PARTNER', 'SYSTEM_ADMIN'] },
  { to: '/admin/topics', label: 'Topics', roles: ['MODERATOR', 'SYSTEM_ADMIN'] },
] as const;

export default function AdminLayout() {
  const { user, logout, hasAnyRole } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const visibleLinks = NAV_LINKS.filter((l) =>
    hasAnyRole(...(l.roles as unknown as Parameters<typeof hasAnyRole>))
  );

  return (
    <div style={{ display: 'flex', minHeight: '100vh', fontFamily: 'system-ui, sans-serif' }}>
      <aside style={{
        width: 220, background: '#fff8f4', borderRight: '1px solid #ffe4c4',
        display: 'flex', flexDirection: 'column', padding: '24px 0',
      }}>
        <div style={{ padding: '0 20px 20px', fontSize: 18, fontWeight: 800, color: '#ff6b35' }}>
          CareBridge
        </div>
        <nav style={{ flex: 1 }}>
          {visibleLinks.map((l) => (
            <NavLink
              key={l.to}
              to={l.to}
              style={({ isActive }) => ({
                display: 'block', padding: '10px 20px', textDecoration: 'none', fontSize: 14,
                fontWeight: isActive ? 700 : 400,
                color: isActive ? '#ff6b35' : '#555',
                background: isActive ? '#fff0e8' : 'transparent',
                borderLeft: isActive ? '3px solid #ff6b35' : '3px solid transparent',
              })}
            >
              {l.label}
            </NavLink>
          ))}
        </nav>
        <div style={{ padding: '0 20px' }}>
          <div style={{ fontSize: 12, color: '#999', marginBottom: 6 }}>
            {user?.name ?? user?.phone} · {user?.role}
          </div>
          <button
            onClick={handleLogout}
            style={{
              width: '100%', padding: '8px 0', borderRadius: 8, border: '1px solid #ffd0b0',
              background: '#fff', color: '#ff6b35', cursor: 'pointer', fontSize: 13, fontWeight: 600,
            }}
          >
            Sign out
          </button>
        </div>
      </aside>
      <main style={{ flex: 1, background: '#fef9f5', overflow: 'auto' }}>
        <Outlet />
      </main>
    </div>
  );
}
