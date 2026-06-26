import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import apiClient from '../../../shared/api/apiClient';
import { useAuthStore } from '../../../shared/auth/authStore';
import type { AuthUser } from '../../../shared/auth/authStore';
import type { UserProfile, ApiResponse } from '../models/user';

interface OtpResponse {
  accessToken: string;
  refreshToken: string;
}

export default function OtpPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const phone = (location.state as { phone?: string })?.phone ?? '';
  const { setTokens, setUser } = useAuthStore();

  const [otp, setOtp] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const { data: otpData } = await apiClient.post<ApiResponse<OtpResponse>>(
        '/api/v1/auth/verify-otp',
        { phone, otp }
      );
      setTokens(otpData.data.accessToken, otpData.data.refreshToken);

      const { data: profileData } = await apiClient.get<ApiResponse<UserProfile>>(
        '/api/v1/auth/profile'
      );
      const p = profileData.data;
      const authUser: AuthUser = {
        id: p.id,
        phone: p.phone,
        name: p.name,
        avatarUrl: p.avatarUrl,
        role: p.role,
      };
      setUser(authUser);
      navigate('/');
    } catch {
      setError('Invalid or expired OTP. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <h2 style={{ margin: 0, fontSize: 22, color: '#333' }}>Enter OTP</h2>
      <p style={{ margin: 0, fontSize: 14, color: '#666' }}>
        A one-time code was sent to <strong>{phone}</strong>.
      </p>
      {error && <p style={{ color: '#c0392b', margin: 0, fontSize: 14 }}>{error}</p>}
      <label style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: 14 }}>
        OTP code
        <input
          type="text"
          inputMode="numeric"
          value={otp}
          onChange={(e) => setOtp(e.target.value)}
          maxLength={6}
          required
          style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid #ddd', fontSize: 22, letterSpacing: 8, textAlign: 'center' }}
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
        {loading ? 'Verifying…' : 'Verify'}
      </button>
    </form>
  );
}
