import { useNavigate } from 'react-router-dom';

const STAT_CARDS = [
  { label: 'Hồ sơ chuyên gia', value: 'Chờ duyệt', icon: 'person_edit', color: 'text-orange-600 bg-orange-50', href: '/admin/expert-verification-queue' },
  { label: 'Nội dung chờ', value: '3', icon: 'edit_note', color: 'text-blue-600 bg-blue-50', href: '/admin/content-approval-queue' },
  { label: 'Vi phạm mới', value: '1', icon: 'report', color: 'text-red-600 bg-red-50', href: '/moderator/reports' },
];

export default function AdminDashboardPage() {
  const navigate = useNavigate();

  return (
    <div className="mx-auto max-w-6xl p-8 md:p-10">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-on-surface">Tổng quan quản trị</h1>
        <p className="mt-2 text-sm text-on-surface-variant">Theo dõi các công việc quản trị, an toàn và vận hành hệ thống.</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-8">
        {STAT_CARDS.map((card) => (
          <button
            key={card.label}
            onClick={() => navigate(card.href)}
            className="flex items-center gap-4 p-5 bg-white rounded-2xl shadow-sm border border-outline-variant/20 hover:shadow-md transition-shadow text-left"
          >
            <span className={`flex items-center justify-center w-12 h-12 rounded-full ${card.color}`}>
              <span className="material-symbols-outlined text-2xl">{card.icon}</span>
            </span>
            <div>
              <p className="text-sm text-on-surface-variant">{card.label}</p>
              <p className="text-xl font-semibold text-primary mt-0.5">{card.value}</p>
            </div>
          </button>
        ))}
      </div>

      <div className="bg-white rounded-2xl border border-outline-variant/20 p-6">
        <h2 className="text-lg font-semibold text-on-surface mb-4">Truy cập nhanh</h2>
        <div className="flex flex-wrap gap-3">
          <button
            onClick={() => navigate('/admin/expert-verification-queue')}
            className="px-5 py-2.5 bg-primary text-white rounded-full font-semibold hover:bg-primary/90 transition-colors"
          >
            Duyệt hồ sơ chuyên gia
          </button>
          <button
            onClick={() => navigate('/admin/content-approval-queue')}
            className="px-5 py-2.5 border border-outline-variant rounded-full font-semibold text-on-surface-variant hover:bg-surface-variant/30 transition-colors"
          >
            Duyệt nội dung
          </button>
          <button
            onClick={() => navigate('/moderator/reports')}
            className="px-5 py-2.5 border border-outline-variant rounded-full font-semibold text-on-surface-variant hover:bg-surface-variant/30 transition-colors"
          >
            Xử lý vi phạm
          </button>
        </div>
      </div>
    </div>
  );
}
