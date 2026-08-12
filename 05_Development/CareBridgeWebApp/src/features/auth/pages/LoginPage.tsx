import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AlertCircle, ArrowRight, Eye, EyeOff, LoaderCircle, Lock, Smartphone, User } from 'lucide-react';
import {
  clearPhoneVerification,
  confirmPhoneVerificationCode,
  googleIdToken,
  sendPhoneVerificationCode,
} from '../services/firebaseAuth';
import { federatedAuthenticate, login, loginWithPhone } from '../services/authApi';
import type { AuthResponse, FederatedAuthResponse } from '../models/auth';
import { useAuthStore } from '../../../shared/auth/authStore';
import { getDefaultRouteForRole } from '../../../shared/auth/roleRoutes';
import logo from '../../../assets/logo.png';
import { parseBlockedAccountError, saveBlockedAccountState } from '../models/blockedAccount';

type LoginMethod = 'EMAIL' | 'PHONE';
const COOLDOWN_SECONDS = 60;
const VIETNAMESE_PHONE_PATTERN = /^\+84[35789]\d{8}$/;

export default function LoginPage() {
  const navigate = useNavigate();
  const [method, setMethod] = useState<LoginMethod>('EMAIL');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [code, setCode] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [step, setStep] = useState<'form' | 'code'>('form');
  const [cooldown, setCooldown] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);

  useEffect(() => {
    if (cooldown <= 0) return undefined;
    const timer = window.setInterval(() => setCooldown((value) => Math.max(0, value - 1)), 1000);
    return () => window.clearInterval(timer);
  }, [cooldown]);

  useEffect(() => () => clearPhoneVerification(), []);

  const completeLogin = (auth: AuthResponse | FederatedAuthResponse) => {
    useAuthStore.getState().setTokens(auth.accessToken, auth.refreshToken);
    const needsProfile = !auth.user.role
      || ('profileCompleted' in auth && !auth.profileCompleted)
      || ('newUser' in auth && auth.newUser);
    if (needsProfile) {
      useAuthStore.getState().setUser(null);
      navigate('/account/profile', { replace: true });
      return;
    }
    useAuthStore.getState().setUser({
      id: auth.user.id,
      phone: auth.user.phone ?? '',
      name: auth.user.name,
      avatarUrl: auth.user.avatarUrl,
      role: auth.user.role as ReturnType<typeof useAuthStore.getState>['user'] extends { role: infer R } ? R : never,
    });
    const role = auth.user.role;
    navigate(role ? getDefaultRouteForRole(role as Parameters<typeof getDefaultRouteForRole>[0]) : '/account/profile', { replace: true });
  };

  const handleError = (err: unknown) => {
    const error = err as { code?: string; response?: { status?: number; data?: { message?: string; error?: string } } };
    const status = error.response?.status;
    const blockedState = parseBlockedAccountError(err);
    if (blockedState) { saveBlockedAccountState(blockedState); navigate('/account-blocked', { replace: true }); return; }
    if (err instanceof Error && err.message === 'Login response is incomplete') setServerError('Phản hồi đăng nhập không hợp lệ. Vui lòng thử lại sau.');
    else if (error.code === 'auth/invalid-verification-code') setServerError('Mã SMS không đúng. Vui lòng kiểm tra và nhập lại.');
    else if (error.code === 'auth/code-expired' || error.code === 'auth/session-expired') setServerError('Mã SMS đã hết hạn. Vui lòng gửi lại mã mới.');
    else if (error.code === 'auth/too-many-requests' || error.code === 'auth/quota-exceeded') setServerError('Bạn đã yêu cầu quá nhiều mã SMS. Vui lòng thử lại sau.');
    else if (error.code === 'auth/invalid-phone-number') setServerError('Số điện thoại không hợp lệ. Hãy kiểm tra mã quốc gia và thử lại.');
    else if (status === undefined) setServerError('Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.');
    else if (status === 401) setServerError('Thông tin đăng nhập không chính xác. Vui lòng thử lại.');
    else if (status === 429) setServerError('Bạn đã thử quá nhiều lần. Vui lòng thử lại sau ít phút.');
    else setServerError(error.response?.data?.message ?? 'Đã xảy ra lỗi. Vui lòng thử lại sau.');
  };

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setServerError(null);
    if (method === 'EMAIL') {
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) { setServerError('Vui lòng nhập email hợp lệ.'); return; }
      if (!password) { setServerError('Vui lòng nhập mật khẩu.'); return; }
      setSubmitting(true);
      try { completeLogin(await login({ email: email.trim(), password })); } catch (err) { handleError(err); } finally { setSubmitting(false); }
      return;
    }
    if (!VIETNAMESE_PHONE_PATTERN.test(phone.trim())) { setServerError('Số điện thoại cần là số di động Việt Nam dạng +84xxxxxxxxx.'); return; }
    setSubmitting(true);
    try { await sendPhoneVerificationCode(phone.trim()); setCode(''); setCooldown(COOLDOWN_SECONDS); setStep('code'); }
    catch (err) { handleError(err); }
    finally { setSubmitting(false); }
  };

  const confirmCode = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!/^\d{6}$/.test(code)) { setServerError('Vui lòng nhập đủ 6 chữ số trong SMS.'); return; }
    setSubmitting(true); setServerError(null);
    try { completeLogin(await loginWithPhone({ idToken: await confirmPhoneVerificationCode(code) })); }
    catch (err) { handleError(err); }
    finally { setSubmitting(false); }
  };

  const resend = async () => {
    if (cooldown > 0 || submitting) return;
    setSubmitting(true); setServerError(null);
    try { await sendPhoneVerificationCode(phone.trim()); setCooldown(COOLDOWN_SECONDS); setCode(''); }
    catch { setServerError('Không thể gửi lại mã SMS. Vui lòng thử lại sau.'); }
    finally { setSubmitting(false); }
  };

  const googleLogin = async () => {
    setSubmitting(true); setServerError(null);
    try {
      const result = await federatedAuthenticate(await googleIdToken());
      completeLogin(result);
    } catch (err) {
      if (err instanceof Error && (err.message.includes('popup-closed') || err.message === 'AUTH_CANCELLED')) return;
      handleError(err);
    } finally { setSubmitting(false); }
  };

  return (
    <div className="min-h-screen bg-[#F6F1EC] px-4 py-8 font-sans text-[#5A463F] sm:px-6">
      <main className="mx-auto flex min-h-[calc(100dvh-4rem)] w-full max-w-[480px] items-center">
        <section className="w-full rounded-3xl border border-[rgba(214,194,189,0.3)] bg-white p-6 shadow-[0_18px_60px_rgba(132,81,67,0.10)] sm:p-10">
          <div className="mb-8 flex flex-col items-center gap-3 text-center">
            <img src={logo} alt="CareBridge" className="h-16 w-16 rounded-2xl object-cover shadow-sm" />
            <h1 className="m-0 text-2xl font-semibold tracking-tight text-[#845143]">Đăng nhập CareBridge</h1>
            <p className="m-0 text-sm leading-5 text-[#524440]">Truy cập không gian chăm sóc của bạn</p>
          </div>

          {serverError && <div role="alert" className="mb-6 flex items-start gap-3 rounded-xl bg-[#ffdad6] p-4 text-sm leading-5 text-[#93000a]"><AlertCircle size={20} className="mt-0.5 shrink-0" /><p className="m-0">{serverError}</p></div>}
          <p role="status" aria-live="polite" className="sr-only">{submitting ? 'Đang xử lý' : ''}</p>
          <div id="firebase-recaptcha" aria-hidden="true" />

          {step === 'form' ? <>
            <div className="mb-6 grid grid-cols-2 gap-2 rounded-xl bg-[#f8eee9] p-1" role="group" aria-label="Phương thức đăng nhập">
              <button type="button" aria-pressed={method === 'EMAIL'} className={`min-h-11 rounded-lg px-3 text-sm font-semibold transition focus:outline-none focus:ring-2 focus:ring-[#845143] ${method === 'EMAIL' ? 'bg-white text-[#845143] shadow-sm' : 'text-[#524440]'}`} onClick={() => { setMethod('EMAIL'); setServerError(null); }}>Email</button>
              <button type="button" aria-label="Continue with phone" aria-pressed={method === 'PHONE'} className={`min-h-11 rounded-lg px-3 text-sm font-semibold transition focus:outline-none focus:ring-2 focus:ring-[#845143] ${method === 'PHONE' ? 'bg-white text-[#845143] shadow-sm' : 'text-[#524440]'}`} onClick={() => { setMethod('PHONE'); setServerError(null); }}>Số điện thoại</button>
            </div>

            <form className="grid gap-5" onSubmit={submit} noValidate>
              {method === 'EMAIL' ? <>
                <Field id="identifier" label="Email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} autoComplete="username" placeholder="email@example.com" required icon={<User size={20} aria-hidden="true" />} />
                <div className="relative"><Field id="password" label="Mật khẩu" type={showPassword ? 'text' : 'password'} value={password} onChange={(event) => setPassword(event.target.value)} autoComplete="current-password" placeholder="Nhập mật khẩu" required icon={<Lock size={20} aria-hidden="true" />} /><button type="button" className="absolute right-3 top-[2.2rem] flex min-h-10 min-w-10 items-center justify-center rounded-lg text-[#524440] hover:text-[#845143] focus:outline-none focus:ring-2 focus:ring-[#845143]" onClick={() => setShowPassword((value) => !value)} aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}>{showPassword ? <Eye size={20} /> : <EyeOff size={20} />}</button></div>
              </> : <>
                <Field id="phone-login-number" label="Số điện thoại" type="tel" value={phone} onChange={(event) => setPhone(event.target.value)} autoComplete="tel" placeholder="+84901234567" required icon={<Smartphone size={20} aria-hidden="true" />} />
                <p className="m-0 rounded-xl bg-[#f8eee9] p-3 text-sm leading-5 text-[#524440]">
                  Nếu số điện thoại chưa có tài khoản, CareBridge sẽ tạo một tài khoản mới và yêu cầu bạn hoàn thiện hồ sơ.
                </p>
              </>}
              <button type="submit" className="mt-2 flex min-h-12 w-full items-center justify-center gap-2 rounded-full bg-[#C98C7B] px-5 text-base font-semibold text-white transition hover:bg-[#845143] focus:outline-none focus:ring-4 focus:ring-[#C98C7B]/30 active:scale-[0.99] disabled:cursor-not-allowed disabled:opacity-60" disabled={submitting}>{submitting ? <LoaderCircle className="animate-spin" size={20} aria-hidden="true" /> : <>{method === 'PHONE' ? 'Tiếp tục bằng số điện thoại' : 'Đăng nhập'}<ArrowRight size={20} aria-hidden="true" /></>}</button>
            </form>

            <div className="my-6 flex items-center gap-3 text-xs text-[#524440]/70"><span className="h-px flex-1 bg-[#d6c2bd]" /><span>hoặc</span><span className="h-px flex-1 bg-[#d6c2bd]" /></div>
            <button type="button" aria-label="Continue with Google" onClick={googleLogin} disabled={submitting} className="min-h-12 w-full rounded-full border border-[#d6c2bd] bg-white px-5 text-sm font-semibold text-[#524440] transition hover:border-[#845143] focus:outline-none focus:ring-4 focus:ring-[#C98C7B]/20 disabled:opacity-60">Tiếp tục với Google</button>
          </> : <form className="grid gap-5" onSubmit={confirmCode}>
            <div className="rounded-xl bg-[#f8eee9] p-4 text-sm leading-6"><Smartphone size={20} className="mb-2 text-[#845143]" aria-hidden="true" /><p className="m-0">Nhập mã 6 chữ số đã gửi đến <strong>{phone}</strong>.</p></div>
            <label className="grid gap-2 text-sm font-semibold" htmlFor="phone-login-code">Mã xác thực SMS<input id="phone-login-code" name="phoneCode" type="text" inputMode="numeric" autoComplete="one-time-code" maxLength={6} value={code} onChange={(event) => setCode(event.target.value.replace(/\D/g, '').slice(0, 6))} className="min-h-14 rounded-xl border border-[#d6c2bd] px-4 text-center text-2xl tracking-[0.45em] outline-none transition focus:border-[#845143] focus:ring-2 focus:ring-[#845143]" autoFocus /></label>
            <button type="submit" className="min-h-12 rounded-full bg-[#C98C7B] px-5 text-base font-semibold text-white transition hover:bg-[#845143] focus:outline-none focus:ring-4 focus:ring-[#C98C7B]/30 disabled:cursor-not-allowed disabled:opacity-60" disabled={submitting || code.length !== 6}>{submitting ? 'Đang xác minh…' : 'Xác nhận và tiếp tục'}</button>
            <div className="text-center text-sm text-[#524440]">{cooldown > 0 ? <>Gửi lại sau <strong>{cooldown}s</strong></> : <button type="button" className="font-semibold text-[#845143] underline focus:outline-none focus:ring-2 focus:ring-[#845143]" onClick={resend}>Gửi lại mã SMS</button>}</div>
            <button type="button" className="text-sm font-medium text-[#845143] underline focus:outline-none focus:ring-2 focus:ring-[#845143]" onClick={() => { clearPhoneVerification(); setStep('form'); setCode(''); }}>Thay đổi số điện thoại</button>
          </form>}

          <p className="mt-8 text-center text-sm text-[#524440]">Chưa có tài khoản? <Link className="font-semibold text-[#845143] underline focus:outline-none focus:ring-2 focus:ring-[#845143]" to="/register">Đăng ký ngay</Link></p>
        </section>
      </main>
    </div>
  );
}

function Field(props: React.InputHTMLAttributes<HTMLInputElement> & { label: string; icon?: React.ReactNode }) {
  const { label, icon, id, ...inputProps } = props;
  return <label className="grid gap-2 text-sm font-semibold text-[#271812]" htmlFor={id}>{label}{props.required && <span aria-hidden="true"> *</span>}<span className="relative"><span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[#524440]">{icon}</span><input id={id} {...inputProps} className="min-h-12 w-full rounded-xl border border-[#d6c2bd] bg-white px-4 pl-11 text-sm font-normal outline-none transition placeholder:text-[#524440]/60 focus:border-[#845143] focus:ring-2 focus:ring-[#845143]" /></span></label>;
}
