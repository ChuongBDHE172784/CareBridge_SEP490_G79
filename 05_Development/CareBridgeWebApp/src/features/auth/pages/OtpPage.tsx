import { useState, useEffect, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { ShieldCheck, RotateCcw, AlertCircle } from 'lucide-react';
import { verifyOtp, resendOtp } from '../services/authApi';
import { getDefaultRouteForRole } from '../../../shared/auth/roleRoutes';
import { useAuthStore } from '../../../shared/auth/authStore';
import './OtpPage.css';

const OTP_LENGTH = 6;
const RESEND_COOLDOWN = 60;

export default function OtpPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const state = location.state as { userId: string; otpExpiresAt: string; identifier: string } | null;

  const [digits, setDigits] = useState<string[]>(Array(OTP_LENGTH).fill(''));
  const [serverError, setServerError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [cooldown, setCooldown] = useState(RESEND_COOLDOWN);
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

  useEffect(() => {
    if (!state?.userId) {
      navigate('/login', { replace: true });
    }
    inputRefs.current[0]?.focus();
  }, [state, navigate]);

  useEffect(() => {
    if (cooldown <= 0) return;
    const id = setInterval(() => setCooldown((c) => c - 1), 1000);
    return () => clearInterval(id);
  }, [cooldown]);

  const handleDigitChange = (index: number, value: string) => {
    const cleaned = value.replace(/\D/g, '').slice(-1);
    const next = [...digits];
    next[index] = cleaned;
    setDigits(next);
    if (cleaned && index < OTP_LENGTH - 1) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index: number, e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace' && !digits[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  const handlePaste = (e: React.ClipboardEvent) => {
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, OTP_LENGTH);
    if (!pasted) return;
    e.preventDefault();
    const next = Array(OTP_LENGTH).fill('');
    pasted.split('').forEach((ch, i) => { next[i] = ch; });
    setDigits(next);
    inputRefs.current[Math.min(pasted.length, OTP_LENGTH - 1)]?.focus();
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const otp = digits.join('');
    if (otp.length < OTP_LENGTH) {
      setServerError('Vui lòng nhập đầy đủ 6 chữ số OTP.');
      return;
    }
    setServerError(null);
    setIsSubmitting(true);
    try {
      const result = await verifyOtp({ userId: state!.userId, otp });
      const route = getDefaultRouteForRole(result.user.role as Parameters<typeof getDefaultRouteForRole>[0]);
      navigate(route, { replace: true });
    } catch (err: unknown) {
      const error = err as { response?: { status?: number; data?: { message?: string } } };
      if (error.response?.status === 400) {
        setServerError(error.response.data?.message ?? 'Mã OTP không đúng hoặc đã hết hạn.');
      } else {
        setServerError('Có lỗi xảy ra. Vui lòng thử lại.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleResend = async () => {
    if (cooldown > 0 || !state?.identifier) return;
    setServerError(null);
    try {
      const isEmail = state.identifier.includes('@');
      await resendOtp(isEmail ? { email: state.identifier } : { phone: state.identifier });
      setCooldown(RESEND_COOLDOWN);
    } catch {
      setServerError('Không thể gửi lại OTP. Vui lòng thử lại sau.');
    }
  };

  const user = useAuthStore((s) => s.user);
  const maskedIdentifier = state?.identifier
    ? state.identifier.includes('@')
      ? state.identifier.replace(/(.{2}).+(@.+)/, '$1****$2')
      : state.identifier.replace(/(\d{3})\d+(\d{3})/, '$1****$2')
    : '';

  return (
    <div className="otp-wrapper">
      <div className="login-blob-top" />
      <div className="login-blob-bottom" />

      <main className="login-card">
        <div className="login-card-inner">
          <div className="login-header">
            <div className="login-logo">
              <ShieldCheck size={28} strokeWidth={2} />
            </div>
            <div>
              <h1 className="login-title">Xác minh OTP</h1>
              <p className="login-subtitle">
                Nhập mã 6 chữ số đã gửi đến{' '}
                <strong style={{ color: '#845143' }}>{maskedIdentifier}</strong>
              </p>
            </div>
          </div>

          {serverError && (
            <div className="login-error">
              <AlertCircle size={20} />
              <p>{serverError}</p>
            </div>
          )}

          <form className="otp-form" onSubmit={handleSubmit}>
            <div className="otp-inputs" onPaste={handlePaste}>
              {digits.map((d, i) => (
                <input
                  key={i}
                  ref={(el) => { inputRefs.current[i] = el; }}
                  type="text"
                  inputMode="numeric"
                  maxLength={1}
                  value={d}
                  className={`otp-digit${d ? ' otp-digit--filled' : ''}`}
                  onChange={(e) => handleDigitChange(i, e.target.value)}
                  onKeyDown={(e) => handleKeyDown(i, e)}
                  aria-label={`Chữ số OTP thứ ${i + 1}`}
                />
              ))}
            </div>

            <button type="submit" className="login-submit" disabled={isSubmitting}>
              {isSubmitting ? 'Đang xác minh...' : 'Xác nhận'}
            </button>
          </form>

          <div className="otp-resend">
            {cooldown > 0 ? (
              <p className="otp-resend-cooldown">
                Gửi lại sau <strong>{cooldown}s</strong>
              </p>
            ) : (
              <button className="otp-resend-btn" onClick={handleResend}>
                <RotateCcw size={16} />
                Gửi lại mã OTP
              </button>
            )}
          </div>

          <div className="login-footer">
            <p>
              <a href="/login" style={{ color: '#845143', fontWeight: 500, textDecoration: 'none' }}>
                ← Quay lại đăng nhập
              </a>
            </p>
          </div>
        </div>
      </main>
    </div>
  );
}
