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
    href: '/security/incidents',
    tone: 'bg-error-container text-error',
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
    <div className="min-h-screen bg-background p-5 font-sans text-on-surface md:p-6">
      <div className="mx-auto max-w-5xl">
        <section className="rounded-lg border border-outline-variant/70 bg-surface p-5">
          <div className="flex flex-col gap-5 md:flex-row md:items-center md:justify-between">
            <div className="max-w-2xl">
              <div className="mb-3 flex h-9 w-9 items-center justify-center rounded-md bg-primary-container text-primary">
                <span className="material-symbols-outlined text-lg">admin_panel_settings</span>
              </div>
              <p className="text-[11px] font-semibold uppercase tracking-[0.02em] text-outline">CareBridge Admin</p>
              <h1 className="mt-1 text-xl font-semibold">Trung tâm điều hành hệ thống</h1>
              <p className="mt-2 text-sm leading-relaxed text-on-surface-variant">
                Truy cập nhanh các tác vụ cần quyền System Admin. Số liệu cộng đồng và chỉ số vận hành được xem trong Hệ thống kiểm duyệt.
              </p>
            </div>
            <button
              type="button"
              onClick={() => navigate('/moderator/dashboard')}
              className="flex h-9 shrink-0 items-center justify-center gap-2 rounded-md bg-primary px-3.5 text-xs font-semibold text-on-primary hover:bg-primary/90"
            >
              <span className="material-symbols-outlined text-lg">monitoring</span>
              Xem tổng quan vận hành
            </button>
          </div>
        </section>

        <section className="mt-6">
          <div className="mb-3 flex items-center gap-2">
            <span className="h-2 w-2 rounded-full bg-primary" />
            <h2 className="text-xs font-semibold uppercase tracking-[0.02em] text-on-surface-variant">Tác vụ ưu tiên</h2>
          </div>
          <div className="grid gap-4 md:grid-cols-3">
            {PRIORITY_ACTIONS.map((action) => (
              <button
                key={action.title}
                type="button"
                onClick={() => navigate(action.href)}
                className="group rounded-lg border border-outline-variant/70 bg-surface p-4 text-left transition-colors duration-150 hover:border-primary/40 hover:bg-surface-container-lowest"
              >
                <div className={`flex h-9 w-9 items-center justify-center rounded-md ${action.tone}`}>
                  <span className="material-symbols-outlined text-lg">{action.icon}</span>
                </div>
                <h3 className="mt-3 text-sm font-semibold text-on-surface">{action.title}</h3>
                <p className="mt-1.5 min-h-[36px] text-xs leading-relaxed text-on-surface-variant">{action.description}</p>
                <span className="mt-3 inline-flex items-center gap-1 text-xs font-semibold text-primary">
                  Mở tác vụ <span className="material-symbols-outlined text-base">arrow_forward</span>
                </span>
              </button>
            ))}
          </div>
        </section>

        <section className="mt-6 rounded-lg border border-outline-variant/70 bg-surface p-5">
          <div className="flex flex-col gap-2 border-b border-outline-variant/60 pb-4 md:flex-row md:items-end md:justify-between">
            <div>
              <h2 className="text-sm font-semibold text-on-surface">Khu vực quản trị</h2>
              <p className="mt-0.5 text-xs text-on-surface-variant">Các khu vực được phân quyền riêng cho System Admin.</p>
            </div>
            <span className="text-xs font-semibold text-outline">{ADMIN_AREAS.length} khu vực sẵn sàng</span>
          </div>
          <div className="mt-4 grid gap-3 md:grid-cols-2">
            {ADMIN_AREAS.map((area) => (
              <button
                key={area.title}
                type="button"
                onClick={() => navigate(area.href)}
                className="flex items-center gap-3 rounded-md border border-outline-variant/60 bg-surface-container-low p-3 text-left transition-colors duration-150 hover:border-primary/30 hover:bg-surface-container"
              >
                <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-md border border-outline-variant/60 bg-surface text-primary">
                  <span className="material-symbols-outlined text-lg">{area.icon}</span>
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block text-xs font-semibold text-on-surface">{area.title}</span>
                  <span className="mt-0.5 block truncate text-[11px] text-on-surface-variant">{area.detail}</span>
                </span>
                <span className="material-symbols-outlined shrink-0 text-outline text-lg">chevron_right</span>
              </button>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}
