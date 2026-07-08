import { Link } from 'react-router-dom';

const navItems = [
  { to: '/expert/profile', label: 'Hồ sơ chuyên môn', icon: '👤', desc: 'CB-055: Xem & cập nhật thông tin cá nhân' },
  { to: '/expert/credentials', label: 'Chứng chỉ & Giấy tờ', icon: '📄', desc: 'CB-056: Tải lên & theo dõi trạng thái xét duyệt' },
  { to: '/expert/calendar', label: 'Lịch rảnh', icon: '📅', desc: 'CB-057: Quản lý khung giờ hoạt động' },
  { to: '/expert/question-queue', label: 'Hàng đợi câu hỏi', icon: '💬', desc: 'CB-063: Trả lời câu hỏi từ cộng đồng' },
];

export default function ExpertDashboardPage() {
  return (
    <div className="max-w-3xl mx-auto p-6">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-on-surface">Chào mừng, Chuyên gia</h1>
        <p className="text-gray-500 mt-1">Quản lý hồ sơ, chứng chỉ và hoạt động của bạn</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {navItems.map((item) => (
          <Link key={item.to} to={item.to}
            className="flex flex-col gap-2 p-5 bg-white rounded-lg border border-gray-200 shadow-sm hover:border-primary/50 hover:shadow-md transition">
            <span className="text-3xl">{item.icon}</span>
            <span className="font-semibold text-gray-900">{item.label}</span>
            <span className="text-sm text-gray-500">{item.desc}</span>
          </Link>
        ))}
      </div>

      <div className="mt-8 p-5 bg-white rounded-lg border border-gray-200 shadow-sm">
        <h2 className="font-semibold text-gray-900 mb-2">Trạng thái xác minh</h2>
        <div className="flex items-center gap-3">
          <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-yellow-50 text-yellow-700 border border-yellow-200 text-sm">
            ⏳ Đang chờ xét duyệt
          </span>
        </div>
        <p className="text-sm text-gray-500 mt-3">
          Chuyên gia cần được xác minh bởi quản trị viên trước khi có thể nhận câu hỏi và tư vấn.
        </p>
      </div>
    </div>
  );
}
