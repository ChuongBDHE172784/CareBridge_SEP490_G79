import { useSearchParams } from 'react-router-dom';
import { ShieldOff } from 'lucide-react';

// Placeholder — will be replaced by /build-screen CB-052 or CB-222
export default function BlockedAccountPage() {
  const [params] = useSearchParams();
  const reason = params.get('reason');

  const message =
    reason === 'disabled'
      ? 'Tài khoản của bạn đã bị vô hiệu hóa. Vui lòng liên hệ hỗ trợ kỹ thuật.'
      : 'Tài khoản của bạn đã bị khoá tạm thời. Vui lòng thử lại sau.';

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
          background: '#ffdad6', display: 'flex',
          alignItems: 'center', justifyContent: 'center',
          margin: '0 auto 24px',
        }}>
          <ShieldOff size={28} color="#93000a" />
        </div>
        <h2 style={{ fontSize: 20, fontWeight: 600, color: '#271812', margin: '0 0 12px' }}>
          Tài khoản bị hạn chế
        </h2>
        <p style={{ fontSize: 14, color: '#524440', lineHeight: 1.6, margin: '0 0 32px' }}>
          {message}
        </p>
        <a
          href="/login"
          style={{
            display: 'inline-block', padding: '14px 32px',
            background: '#c98c7b', color: '#fff', borderRadius: 9999,
            fontWeight: 600, fontSize: 14, textDecoration: 'none',
          }}
        >
          Quay lại đăng nhập
        </a>
      </div>
    </div>
  );
}
