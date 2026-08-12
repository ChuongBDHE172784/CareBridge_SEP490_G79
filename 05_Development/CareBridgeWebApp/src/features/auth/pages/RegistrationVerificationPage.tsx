import { useEffect, useRef, useState } from 'react';
import { ArrowLeft, AtSign, AlertCircle, LoaderCircle, Smartphone } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import type { AuthResponse, VerificationMethod } from '../models/auth';
import { registerUser, registerWithPhone, resendOtp, verifyOtp } from '../services/authApi';
import {
  clearPhoneVerification,
  confirmPhoneVerificationCode,
  sendPhoneVerificationCode,
} from '../services/firebaseAuth';
import {
  clearRegistrationDraft,
  getRegistrationDraft,
} from '../services/registrationVerificationCoordinator';
import { useAuthStore } from '../../../shared/auth/authStore';
import { getDefaultRouteForRole } from '../../../shared/auth/roleRoutes';

const COOLDOWN_SECONDS = 60;

export default function RegistrationVerificationPage() {
  const navigate = useNavigate();
  const preserveDraftOnExit = useRef(false);
  const [draft] = useState(() => getRegistrationDraft());
  const [channel, setChannel] = useState<VerificationMethod | null>(null);
  const [code, setCode] = useState('');
  const [cooldown, setCooldown] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!draft) navigate('/register', { replace: true });
  }, [draft, navigate]);

  useEffect(() => () => {
    clearPhoneVerification();
    if (!preserveDraftOnExit.current) clearRegistrationDraft();
  }, []);

  useEffect(() => {
    if (cooldown <= 0) return undefined;
    const timer = window.setInterval(
      () => setCooldown((value) => Math.max(0, value - 1)),
      1000,
    );
    return () => window.clearInterval(timer);
  }, [cooldown]);

  if (!draft) return null;
  const smsAllowed = draft.role !== 'EXPERT';

  const finish = (auth: AuthResponse) => {
    clearRegistrationDraft();
    clearPhoneVerification();
    useAuthStore.getState().setTokens(auth.accessToken, auth.refreshToken);
    useAuthStore.getState().setUser({
      id: auth.user.id,
      phone: auth.user.phone ?? draft.phone,
      name: auth.user.name ?? draft.name,
      avatarUrl: auth.user.avatarUrl,
      role: auth.user.role,
    });
    navigate(auth.user.role ? getDefaultRouteForRole(auth.user.role) : '/account/profile', {
      replace: true,
    });
  };

  const beginVerification = async (selected: VerificationMethod) => {
    if (submitting || channel) return;
    if (selected === 'PHONE' && !smsAllowed) {
      setError('Tài khoản chuyên gia phải xác thực bằng email.');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      if (selected === 'EMAIL') {
        const result = await registerUser({
          ...draft,
          verificationMethod: 'EMAIL',
        });
        if (result.auth) {
          finish(result.auth);
          return;
        }
      } else {
        await sendPhoneVerificationCode(draft.phone);
      }
      setChannel(selected);
      setCode('');
      setCooldown(COOLDOWN_SECONDS);
    } catch (caught: unknown) {
      setError(messageForError(caught, 'Không thể gửi mã xác thực. Vui lòng thử lại.'));
    } finally {
      setSubmitting(false);
    }
  };

  const confirmCode = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!channel || !/^\d{6}$/.test(code)) {
      setError('Vui lòng nhập đủ 6 chữ số trong mã xác thực.');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      if (channel === 'EMAIL') {
        finish(await verifyOtp({ email: draft.email, otp: code }));
      } else {
        const idToken = await confirmPhoneVerificationCode(code);
        finish(await registerWithPhone({ ...draft, idToken }));
      }
    } catch (caught: unknown) {
      setError(messageForError(caught, 'Mã xác thực không đúng hoặc đã hết hạn.'));
    } finally {
      setSubmitting(false);
    }
  };

  const resend = async () => {
    if (!channel || cooldown > 0 || submitting) return;
    setSubmitting(true);
    setError(null);
    try {
      if (channel === 'EMAIL') await resendOtp({ email: draft.email });
      else await sendPhoneVerificationCode(draft.phone);
      setCode('');
      setCooldown(COOLDOWN_SECONDS);
    } catch (caught: unknown) {
      setError(messageForError(caught, 'Không thể gửi lại mã xác thực. Vui lòng thử lại sau.'));
    } finally {
      setSubmitting(false);
    }
  };

  const editRegistration = () => {
    preserveDraftOnExit.current = true;
    clearPhoneVerification();
    navigate('/register');
  };

  const destination = channel === 'EMAIL' ? maskEmail(draft.email) : maskPhone(draft.phone);

  return (
    <main className="min-h-screen bg-[#F6F1EC] px-4 py-8 font-sans text-[#5A463F] sm:px-6">
      <section className="mx-auto w-full max-w-xl rounded-[2rem] bg-white p-6 shadow-[0_18px_60px_rgba(90,70,63,0.10)] sm:p-10">
        <button
          type="button"
          onClick={channel ? undefined : editRegistration}
          disabled={Boolean(channel) || submitting}
          className="mb-7 inline-flex min-h-11 items-center gap-2 rounded-lg px-2 text-sm font-semibold text-[#845143] transition hover:bg-[#f8eee9] focus:outline-none focus:ring-2 focus:ring-[#845143] disabled:cursor-not-allowed disabled:opacity-50"
        >
          <ArrowLeft size={18} aria-hidden="true" /> Sửa thông tin đăng ký
        </button>

        <div className="mb-7">
          <p className="mb-2 text-sm font-semibold text-[#845143]">Bước 2/2</p>
          <h1 className="m-0 text-2xl font-bold tracking-tight text-[#271812]">Xác thực tài khoản</h1>
          <p className="mt-2 text-sm leading-6 text-[#524440]">
            {channel
              ? <>Nhập mã 6 chữ số đã gửi đến <strong>{destination}</strong>.</>
              : 'Chọn một kênh để nhận mã. Kênh còn lại vẫn được lưu nhưng chưa được dùng để đăng nhập cho tới khi xác minh riêng.'}
          </p>
        </div>

        {error && (
          <div role="alert" className="mb-6 flex items-start gap-3 rounded-xl bg-[#ffdad6] p-4 text-sm leading-5 text-[#93000a]">
            <AlertCircle size={20} className="mt-0.5 shrink-0" />
            <span>{error}</span>
          </div>
        )}
        <p role="status" aria-live="polite" className="sr-only">{submitting ? 'Đang xử lý' : ''}</p>
        <div id="firebase-recaptcha" aria-hidden="true" />

        {!channel ? (
          <div className={`grid gap-4 ${smsAllowed ? 'sm:grid-cols-2' : ''}`} role="group" aria-label="Phương thức xác thực đăng ký">
            <ChannelButton
              icon={<AtSign size={24} aria-hidden="true" />}
              title="Nhận mã qua email"
              description={maskEmail(draft.email)}
              disabled={submitting}
              onClick={() => beginVerification('EMAIL')}
            />
            {smsAllowed && (
              <ChannelButton
                icon={<Smartphone size={24} aria-hidden="true" />}
                title="Nhận mã qua SMS"
                description={maskPhone(draft.phone)}
                disabled={submitting}
                onClick={() => beginVerification('PHONE')}
              />
            )}
            {submitting && (
              <div className="col-span-full flex items-center justify-center gap-2 py-2 text-sm text-[#845143]">
                <LoaderCircle className="animate-spin" size={18} aria-hidden="true" /> Đang gửi mã xác thực…
              </div>
            )}
          </div>
        ) : (
          <form className="grid gap-5" onSubmit={confirmCode}>
            <label className="grid gap-2 text-sm font-semibold" htmlFor="registration-verification-code">
              Mã xác thực
              <input
                id="registration-verification-code"
                name="verificationCode"
                type="text"
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={6}
                value={code}
                onChange={(event) => setCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
                className="min-h-14 rounded-xl border border-[#d6c2bd] px-4 text-center text-2xl tracking-[0.45em] outline-none transition focus:border-[#845143] focus:ring-2 focus:ring-[#845143]"
                autoFocus
              />
            </label>
            <button type="submit" className="min-h-12 rounded-full bg-[#C98C7B] px-5 text-base font-semibold text-white transition hover:bg-[#845143] focus:outline-none focus:ring-4 focus:ring-[#C98C7B]/30 disabled:cursor-not-allowed disabled:opacity-60" disabled={submitting || code.length !== 6}>
              {submitting ? 'Đang xác minh…' : 'Xác nhận và tạo tài khoản'}
            </button>
            <div className="text-center text-sm text-[#524440]">
              {cooldown > 0
                ? <>Gửi lại sau <strong className="tabular-nums">{cooldown}s</strong></>
                : <button type="button" className="min-h-11 font-semibold text-[#845143] underline focus:outline-none focus:ring-2 focus:ring-[#845143]" onClick={resend}>Gửi lại mã</button>}
            </div>
          </form>
        )}
      </section>
    </main>
  );
}

function ChannelButton({
  icon,
  title,
  description,
  disabled,
  onClick,
}: {
  icon: React.ReactNode;
  title: string;
  description: string;
  disabled: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      disabled={disabled}
      onClick={onClick}
      className="grid min-h-32 gap-3 rounded-2xl border border-[#d6c2bd] bg-white p-5 text-left transition hover:-translate-y-0.5 hover:border-[#845143] hover:bg-[#fffaf7] focus:outline-none focus:ring-4 focus:ring-[#C98C7B]/25 active:translate-y-0 disabled:cursor-not-allowed disabled:opacity-60"
    >
      <span className="flex h-11 w-11 items-center justify-center rounded-xl bg-[#f8eee9] text-[#845143]">{icon}</span>
      <span>
        <span className="block font-semibold text-[#271812]">{title}</span>
        <span className="mt-1 block text-sm text-[#524440]">{description}</span>
      </span>
    </button>
  );
}

function messageForError(error: unknown, fallback: string): string {
  const candidate = error as {
    code?: string;
    response?: { status?: number; data?: { message?: string; error?: string } };
  };
  if (candidate.code === 'auth/invalid-verification-code') return 'Mã xác thực không đúng. Vui lòng kiểm tra và nhập lại.';
  if (candidate.code === 'auth/code-expired' || candidate.code === 'auth/session-expired') return 'Mã xác thực đã hết hạn. Vui lòng gửi lại mã mới.';
  if (candidate.code === 'auth/too-many-requests' || candidate.code === 'auth/quota-exceeded') return 'Bạn đã yêu cầu quá nhiều mã. Vui lòng thử lại sau.';
  if (candidate.code === 'auth/invalid-phone-number') return 'Số điện thoại không hợp lệ. Hãy kiểm tra mã quốc gia và thử lại.';
  if (candidate.response?.status === 409) return 'Email hoặc số điện thoại đã được sử dụng. Hãy đăng nhập hoặc dùng thông tin khác.';
  return candidate.response?.data?.message ?? fallback;
}

function maskEmail(email: string): string {
  const [local, domain] = email.split('@');
  return `${local.slice(0, 2)}****@${domain}`;
}

function maskPhone(phone: string): string {
  return phone.replace(/(\+84\d{2})\d+(\d{3})/, '$1****$2');
}
