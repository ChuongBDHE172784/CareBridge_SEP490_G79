import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../../shared/auth/useAuth';

const NAV_ITEMS = [
  { label: 'Tổng quan', icon: 'dashboard', path: '/expert/dashboard' },
  { label: 'Yêu cầu tư vấn', icon: 'contact_support', path: '/expert/consultation-requests' },
  { label: 'Trò chuyện trực tiếp', icon: 'chat', path: '/expert/direct-chats' },
  { label: 'Hồ sơ chuyên môn', icon: 'person', path: '/expert/profile' },
  { label: 'Chứng chỉ & Giấy tờ', icon: 'description', path: '/expert/credentials' },
  { label: 'Lịch rảnh làm việc', icon: 'calendar_month', path: '/expert/calendar' },
  { label: 'Hàng đợi câu hỏi', icon: 'forum', path: '/expert/question-queue' },
] as const;

export default function ExpertLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="flex min-h-screen font-sans">
      <aside className="fixed left-0 top-0 z-20 hidden h-screen w-64 flex-col border-r border-outline-variant/70 bg-surface md:flex">
        {/* Brand Header */}
        <div className="border-b border-outline-variant/70 p-4">
          <div className="flex items-center gap-2 mb-1">
            <span className="material-symbols-outlined text-xl text-primary">medical_services</span>
            <span className="text-sm font-semibold leading-none text-on-surface">ExpertPortal</span>
          </div>
          <p className="ml-7 text-[11px] text-outline">Cổng Chuyên gia CareBridge</p>
        </div>

        {/* Nav Items */}
        <nav className="flex-1 space-y-0.5 overflow-y-auto p-3">
          {NAV_ITEMS.map((item) => (
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

        {/* Footer User Profile Card */}
        <div className="space-y-2.5 border-t border-outline-variant/70 p-3">
          <div className="px-1 text-[11px] text-outline">
            <p className="truncate font-semibold text-on-surface-variant">{user?.name || user?.phone || 'Chuyên gia CareBridge'}</p>
            <p>EXPERT</p>
          </div>

          <button
            type="button"
            onClick={handleLogout}
            className="flex w-full items-center justify-center gap-2 rounded-md border border-outline-variant bg-surface px-3 py-1.5 text-xs font-semibold text-on-surface-variant hover:bg-surface-container-low hover:text-on-surface cursor-pointer"
          >
            <span className="material-symbols-outlined text-[16px]">logout</span>
            Đăng xuất
          </button>
          <p className="text-center text-[10px] text-outline">CareBridge © 2026</p>
        </div>
      </aside>

      <main className="ml-64 min-h-screen bg-background overflow-auto flex-1">
        <Outlet />
      </main>
    </div>
  );
}

