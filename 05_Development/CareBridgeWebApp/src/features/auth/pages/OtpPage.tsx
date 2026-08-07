import { useState, useEffect, useEffectEvent, useRef, useCallback } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { ShieldCheck, RotateCcw, AlertCircle, Smartphone } from 'lucide-react';
import { verifyOtp, resendOtp } from '../services/authApi';
import { getDefaultRouteForRole } from '../../../shared/auth/roleRoutes';

const OTP_LENGTH = 6;
const RESEND_COOLDOWN = 60;
type OtpCredentialEvent = Event & { credential?: { code?: string } };

export default function OtpPage() {
 const navigate = useNavigate();
 const location = useLocation();
 const state = location.state as { userId: string; otpExpiresAt: string; identifier: string } | null;

 const [digits, setDigits] = useState<string[]>(Array(OTP_LENGTH).fill(''));
 const [serverError, setServerError] = useState<string | null>(null);
 const [isSubmitting, setIsSubmitting] = useState(false);
 const [cooldown, setCooldown] = useState(RESEND_COOLDOWN);
 const [autoFilled, setAutoFilled] = useState(false);
 const inputRefs = useRef<(HTMLInputElement | null)[]>([]);
 const otpInputRef = useRef<HTMLInputElement | null>(null);

 const autoSubmit = useCallback((code: string) => {
  if (!code || !state?.identifier || isSubmitting) return;
  setServerError(null);
  setIsSubmitting(true);
  verifyOtp(
   state.identifier.includes('@') ? { email: state.identifier, otp: code } : { phone: state.identifier, otp: code }
  )
   .then((result) => {
    const route = getDefaultRouteForRole(
     result.user.role as Parameters<typeof getDefaultRouteForRole>[0],
    );
    navigate(route, { replace: true });
   })
   .catch((err: unknown) => {
    const e = err as { response?: { data?: { message?: string } } };
    setServerError(e.response?.data?.message ?? 'Mã OTP không đúng hoặc đã hết hạn.');
   })
   .finally(() => setIsSubmitting(false));
 }, [isSubmitting, navigate, state?.identifier]);

 const autoSubmitFromOtpEvent = useEffectEvent((code: string) => {
  autoSubmit(code);
 });

 // ── Web OTP API: auto-fill từ SMS ──
 useEffect(() => {
  if (!('OTPCredential' in window)) return;
  const input = otpInputRef.current;
  if (!input || !state?.identifier) return;
  let autoSubmitTimer: ReturnType<typeof setTimeout> | undefined;

  const handler = (event: Event) => {
   const code = (event as OtpCredentialEvent).credential?.code;
   if (code && /^\d{6}$/.test(code)) {
    event.preventDefault();
    setDigits(code.split(''));
    setAutoFilled(true);
    if (autoSubmitTimer) clearTimeout(autoSubmitTimer);
    autoSubmitTimer = setTimeout(() => autoSubmitFromOtpEvent(code), 300);
   }
  };

  input.addEventListener('otpcredentialreceived', handler as EventListener);
  return () => {
   input.removeEventListener('otpcredentialreceived', handler as EventListener);
   if (autoSubmitTimer) clearTimeout(autoSubmitTimer);
  };
 }, [state?.identifier]);

 useEffect(() => {
  if (!state?.identifier) navigate('/login', { replace: true });
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
  setAutoFilled(false);
  if (cleaned && index < OTP_LENGTH - 1) inputRefs.current[index + 1]?.focus();
 };

 const handleKeyDown = (index: number, e: React.KeyboardEvent<HTMLInputElement>) => {
  if (e.key === 'Backspace' && !digits[index] && index > 0) inputRefs.current[index - 1]?.focus();
 };

 const handlePaste = (e: React.ClipboardEvent) => {
  const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, OTP_LENGTH);
  if (!pasted) return;
  e.preventDefault();
  setDigits(Array.from({ length: OTP_LENGTH }, (_, i) => pasted[i] ?? ''));
  setAutoFilled(false);
  inputRefs.current[Math.min(pasted.length, OTP_LENGTH - 1)]?.focus();
 };

 const handleSubmit = async (e: React.FormEvent) => {
  e.preventDefault();
  const otp = digits.join('');
  if (otp.length < OTP_LENGTH) {
   setServerError('Vui lòng nhập đầy đủ 6 chữ số OTP.');
   return;
  }
  autoSubmit(otp);
 };

 const handleResend = async () => {
  if (cooldown > 0 || !state?.identifier) return;
  setServerError(null);
  try {
   const isEmail = state.identifier.includes('@');
   await resendOtp(isEmail ? { email: state.identifier } : { phone: state.identifier });
   setCooldown(RESEND_COOLDOWN);
   setDigits(Array(OTP_LENGTH).fill(''));
   setAutoFilled(false);
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

   {/* Hidden input cho Web OTP API (không hiển thị) */}
   <input
    ref={otpInputRef}
    type="text"
    inputMode="numeric"
    autoComplete="one-time-code"
    className="absolute opacity-0 pointer-events-none h-0 w-0"
    aria-hidden="true"
    readOnly
   />

   <main className="relative z-10 w-full max-w-[480px] p-6">
    <div className="bg-white rounded-3xl p-12 shadow-[0_10px_40px_-10px_rgba(132,81,67,0.1)] border border-[rgba(214,194,189,0.3)] flex flex-col gap-8">
     <div className="text-center flex flex-col items-center gap-4">
      <div className="w-16 h-16 bg-primary-container text-primary-container rounded-full flex items-center justify-center relative">
       <ShieldCheck size={28} strokeWidth={2} />
       {autoFilled && (
        <span className="absolute -bottom-1 -right-1 flex h-5 w-5 items-center justify-center rounded-full bg-green-600 text-white">
         <Smartphone size={12} />
        </span>
       )}
      </div>
      <div>
       <h1 className="text-2xl font-semibold leading-8 text-primary tracking-tight m-0">Xác minh OTP</h1>
       <p className="text-sm font-normal leading-5 text-on-surface-variant mt-1 mb-0">
        Nhập mã 6 chữ số đã gửi đến <strong className="text-primary">{maskedIdentifier}</strong>
        {autoFilled && <span className="ml-1 text-green-600 text-xs">(Đã tự động lấp đầy)</span>}
       </p>
      </div>
     </div>

     {autoFilled && !serverError && (
      <p className="text-center text-xs text-green-700 bg-green-50 rounded-lg py-2">
       ✓ Mã OTP đã được tự động đọc — đang xác minh...
      </p>
     )}

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
        <RotateCcw size={16} /> Gửi lại mã OTP
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
