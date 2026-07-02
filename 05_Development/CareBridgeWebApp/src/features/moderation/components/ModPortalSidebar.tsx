import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../shared/auth/useAuth';

const NAV_ITEMS = [
  { label: 'Tổng quan', icon: 'dashboard', path: '/moderator/dashboard' },
  { label: 'Hàng đợi', icon: 'queue', path: '/moderator/queue' },
  { label: 'Báo cáo', icon: 'flag', path: '/moderator/reports' },
  { label: 'Vi phạm', icon: 'gavel', path: '/moderator/violations' },
  { label: 'Ca an toàn', icon: 'health_and_safety', path: '/moderator/safety-cases' },
  { label: 'Quy tắc AI', icon: 'rule', path: '/moderator/safety-rules' },
  { label: 'Tác động & vận hành', icon: 'insights', path: '/moderator/impact-report' },
] as const;

export default function ModPortalSidebar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <aside className="w-64 fixed left-0 top-0 h-screen bg-white border-r border-[#FFE2D9] flex flex-col z-20">
      <div className="p-6 border-b border-[#FFE2D9]">
        <div className="flex items-center gap-2 mb-1">
          <span className="material-symbols-outlined text-[#845143] text-2xl">shield</span>
          <span className="font-bold text-[#845143] text-lg leading-none">ModPortal</span>
        </div>
        <p className="text-xs text-[#84736F] ml-8">Hệ thống kiểm duyệt</p>
      </div>
      <nav className="flex-1 p-4 space-y-1">
        {NAV_ITEMS.map((item) => (
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
