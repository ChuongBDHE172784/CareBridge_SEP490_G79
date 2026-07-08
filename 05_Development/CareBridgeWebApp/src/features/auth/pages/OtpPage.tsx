import { useState, useEffect, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { ShieldCheck, RotateCcw, AlertCircle } from 'lucide-react';
import { verifyOtp, resendOtp } from '../services/authApi';
import { getDefaultRouteForRole } from '../../../shared/auth/roleRoutes';

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
    if (!state?.identifier) {
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
      const isEmail = state!.identifier.includes('@');
      const result = await verifyOtp(
        isEmail ? { email: state!.identifier, otp } : { phone: state!.identifier, otp }
      );
      // UC-118: a newly registered PARTNER has no organization profile yet — send them to
      // profile setup (CB-099) instead of the dashboard. Regular logins never hit this page
      // (OTP verification only happens once, at registration), so this only fires the first time.
      const route =
        result.user.role === 'PARTNER'
          ? '/partner/profile-setup'
          : getDefaultRouteForRole(result.user.role as Parameters<typeof getDefaultRouteForRole>[0]);
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

  const maskedIdentifier = state?.identifier
    ? state.identifier.includes('@')
      ? state.identifier.replace(/(.{2}).+(@.+)/, '$1****$2')
      : state.identifier.replace(/(\d{3})\d+(\d{3})/, '$1****$2')
    : '';

  return (
    <div className="font-sans bg-[#F6F1EC] min-h-screen flex items-center justify-center relative overflow-hidden">
      <div className="absolute top-[-10%] left-[-5%] w-[40%] h-[40%] bg-surface-container-highest rounded-full blur-[100px] opacity-40 pointer-events-none" />
      <div className="absolute bottom-[-10%] right-[-5%] w-[50%] h-[50%] bg-[#f8ddd2] rounded-full blur-[120px] opacity-30 pointer-events-none" />

      <main className="relative z-10 w-full max-w-[480px] p-6">
        <div className="bg-white rounded-3xl p-12 shadow-[0_10px_40px_-10px_rgba(132,81,67,0.1)] border border-[rgba(214,194,189,0.3)] flex flex-col gap-8">
          <div className="text-center flex flex-col items-center gap-4">
            <div className="w-16 h-16 bg-primary-container text-primary-container rounded-full flex items-center justify-center">
              <ShieldCheck size={28} strokeWidth={2} />
            </div>
            <div>
              <h1 className="text-2xl font-semibold leading-8 text-primary tracking-tight m-0">Xác minh OTP</h1>
              <p className="text-sm font-normal leading-5 text-on-surface-variant mt-1 mb-0">
                Nhập mã 6 chữ số đã gửi đến{' '}
                <strong className="text-primary">{maskedIdentifier}</strong>
              </p>
            </div>
          </div>

          {serverError && (
            <div className="bg-[#ffdad6] text-[#93000a] p-4 rounded-xl flex items-start gap-3 text-sm leading-5">
              <AlertCircle size={20} className="flex-shrink-0 mt-0.5" />
              <p className="m-0">{serverError}</p>
            </div>
          )}

          <form className="flex flex-col gap-6" onSubmit={handleSubmit}>
            <div className="flex gap-3 justify-center" onPaste={handlePaste}>
              {digits.map((d, i) => (
                <input
                  key={i}
                  ref={(el) => { inputRefs.current[i] = el; }}
                  type="text"
                  inputMode="numeric"
                  maxLength={1}
                  value={d}
                  className={`w-[52px] h-[60px] rounded-xl border font-sans text-2xl font-semibold text-on-background text-center outline-none transition-[border-color,box-shadow] duration-200 [caret-color:transparent] focus:border-primary focus:ring-1 focus:ring-primary ${
                    d ? 'bg-surface-container-low border-primary-container' : 'bg-white border-outline-variant'
                  }`}
                  onChange={(e) => handleDigitChange(i, e.target.value)}
                  onKeyDown={(e) => handleKeyDown(i, e)}
                  aria-label={`Chữ số OTP thứ ${i + 1}`}
                />
              ))}
            </div>

            <button
              type="submit"
              className="w-full h-12 bg-primary-container text-on-primary border-none rounded-full font-sans text-base font-semibold leading-5 cursor-pointer flex items-center justify-center gap-2 mt-2 transition-colors duration-200 hover:bg-primary disabled:opacity-60 disabled:cursor-not-allowed"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Đang xác minh...' : 'Xác nhận'}
            </button>
          </form>

          <div className="text-center">
            {cooldown > 0 ? (
              <p className="text-sm text-on-surface-variant m-0">
                Gửi lại sau <strong>{cooldown}s</strong>
              </p>
            ) : (
              <button
                className="inline-flex items-center gap-1.5 bg-transparent border-none font-sans text-sm font-medium text-primary cursor-pointer p-0 transition-colors duration-200 hover:text-primary-container"
                onClick={handleResend}
              >
                <RotateCcw size={16} />
                Gửi lại mã OTP
              </button>
            )}
          </div>

          <div className="text-center pt-4 border-t border-[rgba(214,194,189,0.3)]">
            <p className="text-sm leading-5 text-on-surface-variant m-0">
              <a href="/login" className="text-primary font-medium no-underline">
                ← Quay lại đăng nhập
              </a>
            </p>
          </div>
        </div>
      </main>
    </div>
  );
}
