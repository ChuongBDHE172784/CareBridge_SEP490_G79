import { Smartphone } from 'lucide-react';
import { useAuth } from '../../../shared/auth/useAuth';

// Placeholder — shown to MOTHER / FAMILY roles who have no web portal access
export default function NoWebAccessPage() {
  const { logout } = useAuth();

  return (
    <div style={{
      fontFamily: "'Lexend', sans-serif",
      background: '#F6F1EC',
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
    }}>
      <div style={{
        background: '#fff',
        borderRadius: 24,
        padding: 48,
        maxWidth: 400,
        width: '100%',
        textAlign: 'center',
        boxShadow: '0 10px 40px -10px rgba(132,81,67,0.1)',
      }}>
        <div style={{
          width: 64, height: 64, borderRadius: '50%',
          background: '#fff1ec', display: 'flex',
          alignItems: 'center', justifyContent: 'center',
          margin: '0 auto 24px',
        }}>
          <Smartphone size={28} color="#845143" />
        </div>
        <h2 style={{ fontSize: 20, fontWeight: 600, color: '#271812', margin: '0 0 12px' }}>
          Vui lòng dùng ứng dụng mobile
        </h2>
        <p style={{ fontSize: 14, color: '#524440', lineHeight: 1.6, margin: '0 0 32px' }}>
          Tài khoản của bạn không có quyền truy cập vào cổng web. Vui lòng sử dụng ứng dụng CareBridge trên điện thoại.
        </p>
        <button
          onClick={() => logout()}
          style={{
            padding: '14px 32px',
            background: '#c98c7b', color: '#fff', border: 'none',
            borderRadius: 9999, fontFamily: "'Lexend', sans-serif",
            fontWeight: 600, fontSize: 14, cursor: 'pointer',
          }}
        >
          Đăng xuất
        </button>
      </div>
    </div>
  );
}
