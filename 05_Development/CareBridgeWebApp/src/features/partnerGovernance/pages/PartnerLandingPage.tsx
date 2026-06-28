import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

const MATERIAL_SYMBOLS_URL =
  'https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@24,400,0,0';

export default function PartnerLandingPage() {
  const navigate = useNavigate();

  useEffect(() => {
    if (!document.querySelector(`link[href="${MATERIAL_SYMBOLS_URL}"]`)) {
      const link = document.createElement('link');
      link.rel = 'stylesheet';
      link.href = MATERIAL_SYMBOLS_URL;
      document.head.appendChild(link);
    }
  }, []);

  return (
    <div className="min-h-screen bg-[#F6F1EC] font-sans">
      {/* ── Top Nav Bar ── */}
      <nav className="flex items-center justify-between py-4 px-12 bg-white shadow-[0_4px_20px_rgba(90,70,63,0.06)] sticky top-0 z-[100]">
        <div className="flex items-center gap-8">
          <span
            className="text-[22px] font-bold text-primary cursor-pointer"
            onClick={() => navigate('/partner')}
          >
            CareBridge
          </span>
          <div className="flex gap-6">
            {['Sản phẩm', 'Giải pháp', 'Về chúng tôi', 'Liên hệ'].map((label) => (
              <span
                key={label}
                className={`text-sm font-medium cursor-pointer ${
                  label === 'Giải pháp'
                    ? 'text-primary underline underline-offset-4'
                    : 'text-on-surface-variant'
                }`}
              >
                {label}
              </span>
            ))}
          </div>
        </div>

        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => navigate('/login')}
            className="px-6 py-2 rounded-full border border-outline-variant bg-transparent text-on-surface font-sans text-sm font-medium cursor-pointer"
          >
            Đăng nhập
          </button>
          <button
            type="button"
            onClick={() => navigate('/partner/register')}
            className="px-6 py-2 rounded-full border-none bg-primary-container text-white font-sans text-sm font-medium cursor-pointer"
          >
            Đăng ký đối tác
          </button>
        </div>
      </nav>

      {/* ── Hero Section ── */}
      <section className="flex justify-center pt-16 px-6 pb-12">
        <div className="bg-white rounded-3xl py-12 px-14 max-w-[680px] w-full text-center shadow-[0_4px_20px_rgba(90,70,63,0.06)]">
          {/* Icon */}
          <div className="w-16 h-16 rounded-full bg-primary-container flex items-center justify-center mx-auto mb-6">
            <span className="material-symbols-outlined text-white" style={{ fontSize: 32 }}>
              handshake
            </span>
          </div>

          <h1 className="text-[32px] font-bold text-on-surface mb-4">
            Cổng thông tin Đối tác CareBridge
          </h1>

          <p className="text-base leading-relaxed text-on-surface-variant mb-8 max-w-[520px] mx-auto">
            Hợp tác vì sức khỏe Mẹ và Bé. Cùng chúng tôi xây dựng một mạng lưới chăm sóc
            chuyên nghiệp, tận tâm và đáng tin cậy.
          </p>

          <div className="flex justify-center gap-4">
            <button
              type="button"
              onClick={() => navigate('/partner/register')}
              className="px-8 py-3 rounded-full border-none bg-primary-container text-white font-sans text-[15px] font-semibold cursor-pointer"
            >
              Đăng ký đối tác
            </button>
            <button
              type="button"
              onClick={() => navigate('/login')}
              className="px-8 py-3 rounded-full border border-outline-variant bg-transparent text-on-surface font-sans text-[15px] font-semibold cursor-pointer"
            >
              Đăng nhập
            </button>
          </div>
        </div>
      </section>

      {/* ── Features Grid ── */}
      <section className="flex justify-center px-6 pb-16">
        <div className="grid grid-cols-3 gap-6 max-w-[900px] w-full">
          {[
            {
              icon: 'schedule',
              title: 'Lịch trình linh hoạt',
              desc: 'Quản lý lịch hẹn và dịch vụ một cách linh hoạt, phù hợp với mọi quy mô tổ chức.',
            },
            {
              icon: 'trending_up',
              title: 'Thống kê chi tiết',
              desc: 'Theo dõi hiệu suất, doanh thu và các chỉ số quan trọng qua bảng điều khiển trực quan.',
            },
            {
              icon: 'support_agent',
              title: 'Hỗ trợ 24/7',
              desc: 'Đội ngũ hỗ trợ luôn sẵn sàng giúp bạn giải quyết mọi vấn đề nhanh chóng.',
            },
          ].map((feature) => (
            <div
              key={feature.icon}
              className="bg-white rounded-[20px] py-8 px-6 text-center shadow-[0_4px_20px_rgba(90,70,63,0.06)]"
            >
              <div className="w-[52px] h-[52px] rounded-full bg-primary-container flex items-center justify-center mx-auto mb-4">
                <span className="material-symbols-outlined text-white" style={{ fontSize: 26 }}>
                  {feature.icon}
                </span>
              </div>
              <h3 className="text-base font-semibold text-on-surface mb-2">{feature.title}</h3>
              <p className="text-sm leading-[1.5] text-on-surface-variant m-0">{feature.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ── Footer ── */}
      <footer className="bg-white py-8 px-12 flex items-center justify-between border-t border-outline-variant">
        <div>
          <span className="text-lg font-bold text-primary">CareBridge</span>
          <p className="text-[13px] text-outline mt-1">
            &copy; 2024 CareBridge Vietnam. Tất cả quyền được bảo lưu.
          </p>
        </div>
        <div className="flex gap-6">
          {['Điều khoản sử dụng', 'Chính sách bảo mật', 'Quy chuẩn đối tác', 'Hỗ trợ'].map(
            (label) => (
              <span key={label} className="text-[13px] text-on-surface-variant cursor-pointer">
                {label}
              </span>
            ),
          )}
        </div>
      </footer>
    </div>
  );
}
