import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../../shared/auth/useAuth';

const NAV_ITEMS = [
  { label: 'Tổng quan', icon: 'dashboard', path: '/expert/dashboard' },
  { label: 'Yêu cầu tư vấn', icon: 'contact_support', path: '/expert/consultation-requests' },
  { label: 'Hồ sơ chuyên môn', icon: 'person', path: '/expert/profile' },
  { label: 'Chứng chỉ & Giấy tờ', icon: 'description', path: '/expert/credentials' },
  { label: 'Lịch rảnh', icon: 'calendar_month', path: '/expert/calendar' },
  { label: 'Câu hỏi từ cộng đồng', icon: 'forum', path: '/expert/question-queue' },
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
        <div className="border-b border-outline-variant/70 p-4">
          <div className="flex items-center gap-2 mb-1">
            <span className="material-symbols-outlined text-xl text-primary">medical_services</span>
            <span className="text-sm font-semibold leading-none text-on-surface">Cổng Chuyên Gia</span>
          </div>
          <p className="ml-7 text-[11px] text-outline">CareBridge Expert</p>
        </div>
        
        <nav className="flex-1 space-y-0.5 overflow-y-auto p-3">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) =>
                `group flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all duration-200 ${
                  isActive
                    ? 'bg-primary/10 text-primary'
                    : 'text-on-surface-variant hover:bg-surface-container hover:text-on-surface'
                }`
              }
            >
              <span
                className={`material-symbols-outlined text-[20px] transition-transform duration-300 group-hover:scale-110 ${
                  false ? 'text-primary' : 'text-outline'
                }`}
              >
                {item.icon}
              </span>
              {item.label}
            </NavLink>
          ))}
        </nav>
        
        <div className="border-t border-outline-variant/70 p-4">
          <div className="mb-4 flex items-center gap-3 rounded-xl bg-surface-container-lowest p-3 border border-outline-variant/50">
            <div className="flex h-9 w-9 items-center justify-center rounded-full bg-primary/20 text-primary font-bold">
              {user?.name?.charAt(0)?.toUpperCase() || 'E'}
            </div>
            <div className="flex-1 overflow-hidden">
              <p className="truncate text-sm font-semibold text-on-surface">{user?.name || 'Chuyên gia'}</p>
              <p className="truncate text-[11px] text-on-surface-variant">{user?.phone}</p>
            </div>
          </div>
          
          <button
            onClick={handleLogout}
            className="flex w-full items-center justify-center gap-2 rounded-xl border border-outline-variant bg-surface px-4 py-2 text-sm font-semibold text-error transition hover:bg-error/10 hover:border-error"
          >
            <span className="material-symbols-outlined text-[18px]">logout</span>
            Đăng xuất và Quay lại
          </button>
        </div>
      </aside>
      
      <main className="ml-64 min-h-screen bg-background overflow-auto flex-1">
        <Outlet />
      </main>
    </div>
  );
}
