import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../../shared/auth/authStore';

export default function NoWebAccessPage() {
  const navigate = useNavigate();
  const logout = useAuthStore((s) => s.logout);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div style={{
      minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: '#fef9f5', fontFamily: 'system-ui, sans-serif',
    }}>
      <div style={{
        background: '#fff', borderRadius: 16, padding: '48px 40px', maxWidth: 440,
        textAlign: 'center', boxShadow: '0 4px 24px rgba(0,0,0,0.08)',
      }}>
        <div style={{ fontSize: 48, marginBottom: 16 }}>📱</div>
        <h2 style={{ margin: '0 0 12px', fontSize: 22, color: '#333' }}>
          Web Portal Not Available
        </h2>
        <p style={{ margin: '0 0 8px', fontSize: 14, color: '#666', lineHeight: 1.6 }}>
          The CareBridge web portal is intended for healthcare professionals and administrators.
        </p>
        <p style={{ margin: '0 0 28px', fontSize: 14, color: '#666', lineHeight: 1.6 }}>
          Please use the <strong>CareBridge mobile app</strong> to access your account features.
        </p>
        <button
          onClick={handleLogout}
          style={{
            padding: '10px 28px', borderRadius: 8, border: 'none', cursor: 'pointer',
            background: 'linear-gradient(135deg, #ff9a56, #ff6b35)', color: '#fff',
            fontSize: 14, fontWeight: 600,
          }}
        >
          Return to Login
        </button>
      </div>
    </div>
  );
}
