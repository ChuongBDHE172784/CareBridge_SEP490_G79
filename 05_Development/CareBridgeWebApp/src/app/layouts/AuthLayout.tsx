import { Outlet } from 'react-router-dom';

export default function AuthLayout() {
  return (
    <div style={{
      minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: 'linear-gradient(135deg, #fff8f4 0%, #ffeedd 100%)',
    }}>
      <div style={{
        background: '#fff', borderRadius: 16, padding: '40px 36px', width: '100%', maxWidth: 400,
        boxShadow: '0 8px 32px rgba(255,120,60,0.12)',
      }}>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <span style={{ fontSize: 28, fontWeight: 800, color: '#ff6b35' }}>CareBridge</span>
        </div>
        <Outlet />
      </div>
    </div>
  );
}
