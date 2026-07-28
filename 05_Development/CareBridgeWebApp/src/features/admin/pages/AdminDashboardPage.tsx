import { useNavigate } from 'react-router-dom';

const PRIORITY_ACTIONS = [
  {
    title: 'Xét duyệt chuyên gia',
    description: 'Rà soát hồ sơ, bằng cấp và trạng thái xác minh chuyên môn.',
    icon: 'verified_user',
    href: '/admin/expert-verification-queue',
    tone: 'bg-primary-container text-primary',
  },
  {
    title: 'Duyệt nội dung',
    description: 'Xử lý các phiên bản nội dung đang chờ quyết định quản trị.',
    icon: 'fact_check',
    href: '/admin/content-approval-queue',
    tone: 'bg-secondary-container text-on-secondary-container',
  },
  {
    title: 'Sự cố bảo mật',
    description: 'Theo dõi, điều tra và xử lý các sự cố an toàn hệ thống.',
    icon: 'security',
    href: '/admin/security/incidents',
    tone: 'bg-error-container text-error',
  },
];

const ADMIN_AREAS = [
  { title: 'Chuyên gia', detail: 'Xác minh và quản lý độ tin cậy', icon: 'medical_services', href: '/admin/expert-trust-management' },
  { title: 'Đối tác', detail: 'Duyệt hồ sơ đối tác', icon: 'handshake', href: '/admin/partners/verification' },
  { title: 'Phân tích tư thế', detail: 'Quản lý cấu hình AI an toàn', icon: 'settings_accessibility', href: '/admin/posture-configs' },
  { title: 'Hệ thống kiểm duyệt', detail: 'Quy tắc AI và cấu hình vận hành', icon: 'shield', href: '/admin/safety-rules' },
];

export default function AdminDashboardPage() {
  const navigate = useNavigate();

  return (
    <div className="p-8 font-sans">
      {/* Header */}
      <div className="mb-6 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <h1 className="text-[26px] font-bold text-on-surface m-0">Trung tâm điều hành hệ thống</h1>
          <p className="text-on-surface-variant text-sm mt-1">
            Truy cập nhanh các tác vụ cần quyền System Admin.
          </p>
        </div>
        <button
          type="button"
          onClick={() => navigate('/admin/safety-rules')}
          className="flex items-center gap-2 py-3 px-6 rounded-full bg-primary text-on-primary border-0 text-sm font-semibold cursor-pointer whitespace-nowrap self-start md:self-auto hover:bg-primary/90"
        >
          <span className="material-symbols-outlined text-lg">rule</span>
          Mở quy tắc an toàn
        </button>
      </div>

      {/* Priority Actions */}
      <section className="mb-6">
        <div className="mb-3 flex items-center gap-2">
          <span className="h-2 w-2 rounded-full bg-primary" />
          <h2 className="text-xs font-semibold uppercase tracking-[0.05em] text-outline">Tác vụ ưu tiên</h2>
        </div>
        <div className="grid gap-4 md:grid-cols-3">
          {PRIORITY_ACTIONS.map((action) => (
            <button
              key={action.title}
              type="button"
              onClick={() => navigate(action.href)}
              className="group bg-surface rounded-2xl p-6 shadow-sm border border-surface-container-highest text-left cursor-pointer transition-all hover:shadow-md hover:border-primary/40"
            >
              <div className={`flex h-10 w-10 items-center justify-center rounded-xl ${action.tone}`}>
                <span className="material-symbols-outlined text-xl">{action.icon}</span>
              </div>
              <h3 className="mt-4 text-base font-bold text-on-surface m-0">{action.title}</h3>
              <p className="mt-1.5 min-h-[36px] text-xs leading-relaxed text-on-surface-variant">{action.description}</p>
              <span className="mt-4 inline-flex items-center gap-1 text-xs font-semibold text-primary group-hover:translate-x-0.5 transition-transform">
                Mở tác vụ <span className="material-symbols-outlined text-base">arrow_forward</span>
              </span>
            </button>
          ))}
        </div>
      </section>

      {/* Admin Areas */}
      <section className="bg-surface rounded-2xl p-6 shadow-md border border-surface-container-highest">
        <div className="flex flex-col gap-2 border-b border-surface-container-highest pb-4 md:flex-row md:items-end md:justify-between">
          <div>
            <h2 className="text-sm font-semibold text-on-surface m-0">Khu vực quản trị</h2>
            <p className="mt-1 text-xs text-on-surface-variant">Các khu vực được phân quyền riêng cho System Admin.</p>
          </div>
          <span className="text-xs font-semibold text-outline">{ADMIN_AREAS.length} khu vực sẵn sàng</span>
        </div>
        <div className="mt-5 grid gap-4 md:grid-cols-2">
          {ADMIN_AREAS.map((area) => (
            <button
              key={area.title}
              type="button"
              onClick={() => navigate(area.href)}
              className="flex items-center gap-3.5 rounded-2xl border border-surface-container-highest bg-surface-container-low p-4 text-left cursor-pointer transition-colors hover:border-primary/40 hover:bg-surface-bright"
            >
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-outline-variant/60 bg-surface text-primary">
                <span className="material-symbols-outlined text-xl">{area.icon}</span>
              </span>
              <span className="min-w-0 flex-1">
                <span className="block text-sm font-semibold text-on-surface">{area.title}</span>
                <span className="mt-0.5 block truncate text-xs text-on-surface-variant">{area.detail}</span>
              </span>
              <span className="material-symbols-outlined shrink-0 text-outline text-lg">chevron_right</span>
            </button>
          ))}
        </div>
      </section>
    </div>
  );
}
