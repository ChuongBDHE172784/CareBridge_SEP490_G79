import { useNavigate } from 'react-router-dom';

const PRIORITY_ACTIONS = [
  {
    title: 'Xét duyệt chuyên gia',
    description: 'Rà soát hồ sơ, bằng cấp và trạng thái xác minh chuyên môn.',
    icon: 'verified_user',
    href: '/admin/expert-verification-queue',
    tone: 'bg-[#F8E6DE] text-[#B67868]',
  },
  {
    title: 'Duyệt nội dung',
    description: 'Xử lý các phiên bản nội dung đang chờ quyết định quản trị.',
    icon: 'fact_check',
    href: '/admin/content-approval-queue',
    tone: 'bg-[#E8F0E4] text-[#52734D]',
  },
  {
    title: 'Sự cố bảo mật',
    description: 'Theo dõi, điều tra và xử lý các sự cố an toàn hệ thống.',
    icon: 'security',
    href: '/security/incidents',
    tone: 'bg-[#F8E8E6] text-[#B35B50]',
  },
];

const ADMIN_AREAS = [
  { title: 'Chuyên gia', detail: 'Xác minh và quản lý độ tin cậy', icon: 'medical_services', href: '/admin/expert-trust-management' },
  { title: 'Đối tác', detail: 'Duyệt hồ sơ đối tác', icon: 'handshake', href: '/admin/partners/verification' },
  { title: 'Phân tích tư thế', detail: 'Quản lý cấu hình AI an toàn', icon: 'settings_accessibility', href: '/posture-configs' },
  { title: 'Hệ thống kiểm duyệt', detail: 'Số liệu cộng đồng, quy tắc AI và vận hành', icon: 'shield', href: '/moderator/dashboard' },
];

export default function AdminDashboardPage() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-[#F6F1EC] p-5 font-sans text-[#5A463F] md:p-10">
      <div className="mx-auto max-w-6xl">
        <section className="rounded-[32px] border border-[#E8DDD6]/70 bg-white p-7 shadow-[0_12px_32px_rgba(90,70,63,0.06)] md:p-10">
          <div className="flex flex-col gap-6 md:flex-row md:items-center md:justify-between">
            <div className="max-w-2xl">
              <div className="mb-5 flex h-12 w-12 items-center justify-center rounded-full bg-[#C98C7B]/15 text-[#C98C7B]">
                <span className="material-symbols-outlined text-2xl">admin_panel_settings</span>
              </div>
              <p className="text-sm font-bold uppercase tracking-[0.16em] text-[#9C857C]">CareBridge Admin</p>
              <h1 className="mt-2 text-3xl font-black tracking-tight md:text-4xl">Trung tâm điều hành hệ thống</h1>
              <p className="mt-3 text-base leading-relaxed text-[#9C857C]">
                Truy cập nhanh các tác vụ cần quyền System Admin. Số liệu cộng đồng và chỉ số vận hành được xem trong Hệ thống kiểm duyệt.
              </p>
            </div>
            <button
              type="button"
              onClick={() => navigate('/moderator/dashboard')}
              className="flex h-12 shrink-0 items-center justify-center gap-2 rounded-full bg-[#C98C7B] px-6 font-semibold text-white shadow-[0_8px_24px_rgba(201,140,123,0.25)] transition-all duration-300 hover:-translate-y-0.5 hover:bg-[#B67868] active:scale-95"
            >
              <span className="material-symbols-outlined text-lg">monitoring</span>
              Xem tổng quan vận hành
            </button>
          </div>
        </section>

        <section className="mt-8">
          <div className="mb-4 flex items-center gap-3">
            <span className="h-2.5 w-2.5 rounded-full bg-[#C98C7B]" />
            <h2 className="text-lg font-black">Tác vụ ưu tiên</h2>
          </div>
          <div className="grid gap-5 md:grid-cols-3">
            {PRIORITY_ACTIONS.map((action) => (
              <button
                key={action.title}
                type="button"
                onClick={() => navigate(action.href)}
                className="group rounded-[28px] border border-[#E8DDD6]/70 bg-white p-6 text-left shadow-[0_12px_32px_rgba(90,70,63,0.06)] transition-all duration-300 hover:-translate-y-1 hover:shadow-[0_16px_36px_rgba(90,70,63,0.11)] active:scale-[0.98]"
              >
                <div className={`flex h-12 w-12 items-center justify-center rounded-full ${action.tone}`}>
                  <span className="material-symbols-outlined text-2xl">{action.icon}</span>
                </div>
                <h3 className="mt-5 text-lg font-black">{action.title}</h3>
                <p className="mt-2 min-h-12 text-sm leading-relaxed text-[#9C857C]">{action.description}</p>
                <span className="mt-5 inline-flex items-center gap-1 text-sm font-bold text-[#C98C7B]">
                  Mở tác vụ <span className="material-symbols-outlined text-base">arrow_forward</span>
                </span>
              </button>
            ))}
          </div>
        </section>

        <section className="mt-8 rounded-[32px] border border-[#E8DDD6]/70 bg-white p-7 shadow-[0_12px_32px_rgba(90,70,63,0.06)] md:p-8">
          <div className="flex flex-col gap-2 md:flex-row md:items-end md:justify-between">
            <div>
              <h2 className="text-xl font-black">Khu vực quản trị</h2>
              <p className="mt-1 text-sm text-[#9C857C]">Các khu vực được phân quyền riêng cho System Admin.</p>
            </div>
            <span className="text-sm font-semibold text-[#9C857C]">{ADMIN_AREAS.length} khu vực sẵn sàng</span>
          </div>
          <div className="mt-6 grid gap-3 md:grid-cols-2">
            {ADMIN_AREAS.map((area) => (
              <button
                key={area.title}
                type="button"
                onClick={() => navigate(area.href)}
                className="flex items-center gap-4 rounded-2xl border-2 border-transparent bg-[#F6F1EC] p-4 text-left transition-all duration-300 hover:border-[#C98C7B]/20 hover:bg-[#F2EAE4] active:scale-[0.99]"
              >
                <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-white text-[#C98C7B] shadow-sm">
                  <span className="material-symbols-outlined">{area.icon}</span>
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block font-bold">{area.title}</span>
                  <span className="mt-0.5 block truncate text-sm text-[#9C857C]">{area.detail}</span>
                </span>
                <span className="material-symbols-outlined shrink-0 text-[#9C857C]">chevron_right</span>
              </button>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}
