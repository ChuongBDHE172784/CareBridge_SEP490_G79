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
          <div className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-primary text-on-primary font-bold shadow-sm shrink-0">
              <span className="material-symbols-outlined text-xl">medical_services</span>
            </div>
            <div>
              <span className="text-sm font-bold leading-none text-on-surface block">CareBridge</span>
              <span className="text-[11px] text-outline mt-0.5 block font-medium">Cổng Chuyên gia Y tế</span>
            </div>
          </div>
        </div>

        {/* Nav Items */}
        <nav className="flex-1 space-y-1 overflow-y-auto p-3">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) =>
                `flex items-center gap-2.5 rounded-xl px-3.5 py-2.5 text-xs font-semibold transition-all ${
                  isActive
                    ? 'bg-primary-container text-primary font-bold shadow-sm'
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
        <div className="space-y-3 border-t border-outline-variant/70 p-3">
          <div className="flex items-center gap-2.5 rounded-2xl bg-surface-container-low p-2.5 border border-outline-variant/40">
            <div className="flex h-9 w-9 items-center justify-center rounded-full bg-primary-container text-primary font-bold text-xs shrink-0">
              {user?.name?.charAt(0)?.toUpperCase() || user?.phone?.charAt(0) || 'E'}
            </div>
            <div className="overflow-hidden text-[11px] text-outline flex-1 min-w-0">
              <p className="truncate font-bold text-xs text-on-surface">{user?.name || user?.phone || 'Chuyên gia Y tế'}</p>
              <span className="inline-block py-0.5 px-2 rounded-full bg-surface-container text-primary font-semibold text-[10px] mt-0.5">
                Chuyên gia Y tế
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

      <main className="ml-64 min-h-screen bg-background overflow-auto flex-1">
        <Outlet />
      </main>
    </div>
  );
}

