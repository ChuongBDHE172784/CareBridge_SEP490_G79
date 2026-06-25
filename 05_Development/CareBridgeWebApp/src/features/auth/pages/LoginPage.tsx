import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import apiClient from '../../../shared/api/apiClient';

export default function LoginPage() {
  const navigate = useNavigate();
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await apiClient.post('/api/v1/auth/login', { phone, password });
      navigate('/login/otp', { state: { phone } });
    } catch {
      setError('Invalid phone number or password.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <h2 style={{ margin: 0, fontSize: 22, color: '#333' }}>Sign in to CareBridge</h2>
      {error && (
        <p style={{ color: '#c0392b', margin: 0, fontSize: 14 }}>{error}</p>
      )}
      <label style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: 14 }}>
        Phone number
        <input
          type="tel"
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
          placeholder="+84..."
          required
          style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid #ddd', fontSize: 15 }}
        />
      </label>
      <label style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: 14 }}>
        Password
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
          style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid #ddd', fontSize: 15 }}
        />
      </label>
      <button
        type="submit"
        disabled={loading}
        style={{
          padding: '10px 0', borderRadius: 8, border: 'none', cursor: 'pointer',
          background: 'linear-gradient(135deg, #ff9a56, #ff6b35)', color: '#fff',
          fontSize: 15, fontWeight: 600, marginTop: 4,
        }}
      >
        {loading ? 'Sending OTP…' : 'Continue'}
      </button>
    </form>
  );
}
