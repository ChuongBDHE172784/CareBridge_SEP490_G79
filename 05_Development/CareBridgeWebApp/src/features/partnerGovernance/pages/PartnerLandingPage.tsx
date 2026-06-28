import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

const FONT_FAMILY = "'Lexend', sans-serif";

const COLORS = {
  primary: '#845143',
  primaryContainer: '#C98C7B',
  surface: '#FFF8F6',
  bodyBg: '#F6F1EC',
  onSurface: '#271812',
  onSurfaceVariant: '#524440',
  outline: '#84736f',
  outlineVariant: '#D6C2BD',
  cardShadow: '0 4px 20px rgba(90,70,63,0.06)',
};

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
    <div style={{ minHeight: '100vh', backgroundColor: COLORS.bodyBg, fontFamily: FONT_FAMILY }}>
      {/* ── Top Nav Bar ── */}
      <nav
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '16px 48px',
          backgroundColor: '#fff',
          boxShadow: COLORS.cardShadow,
          position: 'sticky',
          top: 0,
          zIndex: 100,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 32 }}>
          <span
            style={{ fontSize: 22, fontWeight: 700, color: COLORS.primary, cursor: 'pointer' }}
            onClick={() => navigate('/partner')}
          >
            CareBridge
          </span>
          <div style={{ display: 'flex', gap: 24 }}>
            {['Sản phẩm', 'Giải pháp', 'Về chúng tôi', 'Liên hệ'].map((label) => (
              <span
                key={label}
                style={{
                  fontSize: 14,
                  fontWeight: 500,
                  color: label === 'Giải pháp' ? COLORS.primary : COLORS.onSurfaceVariant,
                  textDecoration: label === 'Giải pháp' ? 'underline' : 'none',
                  textUnderlineOffset: 4,
                  cursor: 'pointer',
                }}
              >
                {label}
              </span>
            ))}
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <button
            type="button"
            onClick={() => navigate('/login')}
            style={{
              padding: '8px 24px',
              borderRadius: 9999,
              border: `1px solid ${COLORS.outlineVariant}`,
              backgroundColor: 'transparent',
              color: COLORS.onSurface,
              fontFamily: FONT_FAMILY,
              fontSize: 14,
              fontWeight: 500,
              cursor: 'pointer',
            }}
          >
            Đăng nhập
          </button>
          <button
            type="button"
            onClick={() => navigate('/partner/register')}
            style={{
              padding: '8px 24px',
              borderRadius: 9999,
              border: 'none',
              backgroundColor: COLORS.primaryContainer,
              color: '#fff',
              fontFamily: FONT_FAMILY,
              fontSize: 14,
              fontWeight: 500,
              cursor: 'pointer',
            }}
          >
            Đăng ký đối tác
          </button>
        </div>
      </nav>

      {/* ── Hero Section ── */}
      <section
        style={{
          display: 'flex',
          justifyContent: 'center',
          padding: '64px 24px 48px',
        }}
      >
        <div
          style={{
            backgroundColor: '#fff',
            borderRadius: 24,
            padding: '48px 56px',
            maxWidth: 680,
            width: '100%',
            textAlign: 'center',
            boxShadow: COLORS.cardShadow,
          }}
        >
          {/* Icon */}
          <div
            style={{
              width: 64,
              height: 64,
              borderRadius: '50%',
              backgroundColor: COLORS.primaryContainer,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 24px',
            }}
          >
            <span className="material-symbols-outlined" style={{ fontSize: 32, color: '#fff' }}>
              handshake
            </span>
          </div>

          <h1
            style={{
              fontSize: 32,
              fontWeight: 700,
              color: COLORS.onSurface,
              margin: '0 0 16px',
            }}
          >
            Cổng thông tin Đối tác CareBridge
          </h1>

          <p
            style={{
              fontSize: 16,
              lineHeight: 1.6,
              color: COLORS.onSurfaceVariant,
              margin: '0 0 32px',
              maxWidth: 520,
              marginLeft: 'auto',
              marginRight: 'auto',
            }}
          >
            Hợp tác vì sức khỏe Mẹ và Bé. Cùng chúng tôi xây dựng một mạng lưới chăm sóc
            chuyên nghiệp, tận tâm và đáng tin cậy.
          </p>

          <div style={{ display: 'flex', justifyContent: 'center', gap: 16 }}>
            <button
              type="button"
              onClick={() => navigate('/partner/register')}
              style={{
                padding: '12px 32px',
                borderRadius: 9999,
                border: 'none',
                backgroundColor: COLORS.primaryContainer,
                color: '#fff',
                fontFamily: FONT_FAMILY,
                fontSize: 15,
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              Đăng ký đối tác
            </button>
            <button
              type="button"
              onClick={() => navigate('/login')}
              style={{
                padding: '12px 32px',
                borderRadius: 9999,
                border: `1px solid ${COLORS.outlineVariant}`,
                backgroundColor: 'transparent',
                color: COLORS.onSurface,
                fontFamily: FONT_FAMILY,
                fontSize: 15,
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              Đăng nhập
            </button>
          </div>
        </div>
      </section>

      {/* ── Features Grid ── */}
      <section
        style={{
          display: 'flex',
          justifyContent: 'center',
          padding: '0 24px 64px',
        }}
      >
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(3, 1fr)',
            gap: 24,
            maxWidth: 900,
            width: '100%',
          }}
        >
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
              style={{
                backgroundColor: '#fff',
                borderRadius: 20,
                padding: '32px 24px',
                textAlign: 'center',
                boxShadow: COLORS.cardShadow,
              }}
            >
              <div
                style={{
                  width: 52,
                  height: 52,
                  borderRadius: '50%',
                  backgroundColor: COLORS.primaryContainer,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  margin: '0 auto 16px',
                }}
              >
                <span className="material-symbols-outlined" style={{ fontSize: 26, color: '#fff' }}>
                  {feature.icon}
                </span>
              </div>
              <h3
                style={{
                  fontSize: 16,
                  fontWeight: 600,
                  color: COLORS.onSurface,
                  margin: '0 0 8px',
                }}
              >
                {feature.title}
              </h3>
              <p
                style={{
                  fontSize: 14,
                  lineHeight: 1.5,
                  color: COLORS.onSurfaceVariant,
                  margin: 0,
                }}
              >
                {feature.desc}
              </p>
            </div>
          ))}
        </div>
      </section>

      {/* ── Footer ── */}
      <footer
        style={{
          backgroundColor: '#fff',
          padding: '32px 48px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          borderTop: `1px solid ${COLORS.outlineVariant}`,
        }}
      >
        <div>
          <span style={{ fontSize: 18, fontWeight: 700, color: COLORS.primary }}>CareBridge</span>
          <p
            style={{
              fontSize: 13,
              color: COLORS.outline,
              margin: '4px 0 0',
            }}
          >
            &copy; 2024 CareBridge Vietnam. Tất cả quyền được bảo lưu.
          </p>
        </div>
        <div style={{ display: 'flex', gap: 24 }}>
          {['Điều khoản sử dụng', 'Chính sách bảo mật', 'Quy chuẩn đối tác', 'Hỗ trợ'].map(
            (label) => (
              <span
                key={label}
                style={{
                  fontSize: 13,
                  color: COLORS.onSurfaceVariant,
                  cursor: 'pointer',
                }}
              >
                {label}
              </span>
            ),
          )}
        </div>
      </footer>
    </div>
  );
}
