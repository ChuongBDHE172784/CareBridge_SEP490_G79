import { useState } from 'react';
import { ArrowLeft, ArrowRight, Eye, EyeOff, KeyRound, Mail } from 'lucide-react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import logo from '../../../assets/logo.png';
import { forgotPassword, resetPassword } from '../services/authApi';

type RecoveryStep = 'request' | 'reset';

const passwordRequirements =
  'Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường, chữ số và ký tự đặc biệt.';

function errorMessage(error: unknown, fallback: string): string {
  const candidate = error as { response?: { status?: number; data?: { message?: string } } };
  if (candidate.response?.status === 429) {
    return 'Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau ít phút.';
  }
  return candidate.response?.data?.message ?? fallback;
}

export default function ForgotPasswordPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const initialToken = searchParams.get('token')?.trim() ?? '';
  const [step, setStep] = useState<RecoveryStep>(initialToken ? 'reset' : 'request');
  const [contact, setContact] = useState('');
  const [token, setToken] = useState(initialToken);
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const requestReset = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const normalizedContact = contact.trim();
    if (!normalizedContact) {
      setError('Vui lòng nhập email hoặc số điện thoại.');
      return;
    }

    setIsSubmitting(true);
    setError(null);
    try {
      await forgotPassword({ contact: normalizedContact });
      setNotice('Nếu tài khoản tồn tại và đang hoạt động, hướng dẫn đặt lại mật khẩu đã được gửi. Mã có hiệu lực trong 15 phút.');
      setStep('reset');
    } catch (requestError) {
      setError(errorMessage(requestError, 'Không thể gửi yêu cầu đặt lại mật khẩu. Vui lòng thử lại.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const submitReset = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!token.trim()) {
      setError('Vui lòng nhập mã đặt lại mật khẩu.');
      return;
    }
    if (newPassword.length < 8) {
      setError(passwordRequirements);
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('Mật khẩu xác nhận không khớp.');
      return;
    }

    setIsSubmitting(true);
    setError(null);
    try {
      await resetPassword({ token: token.trim(), newPassword, confirmPassword });
      navigate('/login', {
        replace: true,
        state: { successMessage: 'Đặt lại mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới.' },
      });
    } catch (resetError) {
      setError(errorMessage(resetError, 'Mã đặt lại không hợp lệ hoặc đã hết hạn. Vui lòng yêu cầu mã mới.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-[#F6F1EC] p-6 font-sans antialiased">
      <div className="pointer-events-none absolute left-[-5%] top-[-10%] h-[40%] w-[40%] rounded-full bg-surface-container-highest opacity-40 blur-[100px]" />
      <div className="pointer-events-none absolute bottom-[-10%] right-[-5%] h-[50%] w-[50%] rounded-full bg-[#f8ddd2] opacity-30 blur-[120px]" />

      <main className="relative z-10 w-full max-w-[520px] rounded-3xl border border-[rgba(214,194,189,0.3)] bg-white p-8 shadow-[0_10px_40px_-10px_rgba(132,81,67,0.1)] sm:p-12">
        <div className="mb-8 flex flex-col items-center gap-3 text-center">
          <img src={logo} alt="CareBridge Logo" className="h-16 w-16 rounded-2xl object-cover shadow-sm" />
          <div>
            <h1 className="m-0 text-2xl font-semibold tracking-tight text-primary">
              {step === 'request' ? 'Quên mật khẩu' : 'Đặt lại mật khẩu'}
            </h1>
            <p className="mb-0 mt-2 text-sm leading-5 text-on-surface-variant">
              {step === 'request'
                ? 'Nhập email hoặc số điện thoại đã đăng ký với CareBridge.'
                : 'Nhập mã đã nhận và tạo mật khẩu mới cho tài khoản.'}
            </p>
          </div>
        </div>

        {error && <div role="alert" className="mb-5 rounded-xl bg-[#ffdad6] p-4 text-sm text-[#93000a]">{error}</div>}
        {notice && <div role="status" className="mb-5 rounded-xl bg-emerald-50 p-4 text-sm leading-5 text-emerald-800">{notice}</div>}

        {step === 'request' ? (
          <form className="space-y-6" onSubmit={requestReset}>
            <label className="block space-y-2" htmlFor="recovery-contact">
              <span className="text-xs font-medium uppercase tracking-[0.05em] text-on-background">Email hoặc số điện thoại</span>
              <span className="relative block">
                <Mail size={20} className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-on-surface-variant" />
                <input
                  id="recovery-contact"
                  value={contact}
                  onChange={(event) => setContact(event.target.value)}
                  className="h-12 w-full rounded-xl border border-outline-variant bg-white pl-11 pr-4 text-sm outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                  placeholder="email@example.com hoặc 0901234567"
                  autoComplete="username"
                />
              </span>
            </label>
            <button disabled={isSubmitting} className="flex h-12 w-full items-center justify-center gap-2 rounded-full border-0 bg-primary-container font-semibold text-on-primary hover:bg-primary disabled:cursor-not-allowed disabled:opacity-60">
              {isSubmitting ? 'Đang gửi...' : 'Gửi hướng dẫn'}
              {!isSubmitting && <ArrowRight size={20} />}
            </button>
          </form>
        ) : (
          <form className="space-y-5" onSubmit={submitReset}>
            <label className="block space-y-2" htmlFor="reset-token">
              <span className="text-xs font-medium uppercase tracking-[0.05em] text-on-background">Mã đặt lại mật khẩu</span>
              <span className="relative block">
                <KeyRound size={20} className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-on-surface-variant" />
                <input id="reset-token" value={token} onChange={(event) => setToken(event.target.value)} className="h-12 w-full rounded-xl border border-outline-variant pl-11 pr-4 text-sm outline-none focus:border-primary focus:ring-1 focus:ring-primary" autoComplete="one-time-code" />
              </span>
            </label>
            <PasswordInput id="new-password" label="Mật khẩu mới" value={newPassword} onChange={setNewPassword} visible={showPassword} onToggle={() => setShowPassword((value) => !value)} />
            <PasswordInput id="confirm-password" label="Xác nhận mật khẩu mới" value={confirmPassword} onChange={setConfirmPassword} visible={showPassword} onToggle={() => setShowPassword((value) => !value)} />
            <p className="m-0 text-xs leading-5 text-on-surface-variant">{passwordRequirements}</p>
            <button disabled={isSubmitting} className="flex h-12 w-full items-center justify-center gap-2 rounded-full border-0 bg-primary-container font-semibold text-on-primary hover:bg-primary disabled:cursor-not-allowed disabled:opacity-60">
              {isSubmitting ? 'Đang cập nhật...' : 'Đặt lại mật khẩu'}
              {!isSubmitting && <ArrowRight size={20} />}
            </button>
            <button type="button" onClick={() => { setStep('request'); setNotice(null); setError(null); }} className="w-full border-0 bg-transparent text-sm font-semibold text-primary hover:underline">
              Gửi lại mã khác
            </button>
          </form>
        )}

        <Link to="/login" className="mt-8 flex items-center justify-center gap-2 border-t border-outline-variant/40 pt-6 text-sm font-semibold text-primary no-underline hover:underline">
          <ArrowLeft size={18} /> Quay lại đăng nhập
        </Link>
      </main>
    </div>
  );
}

interface PasswordInputProps {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  visible: boolean;
  onToggle: () => void;
}

function PasswordInput({ id, label, value, onChange, visible, onToggle }: PasswordInputProps) {
  return (
    <label className="block space-y-2" htmlFor={id}>
      <span className="text-xs font-medium uppercase tracking-[0.05em] text-on-background">{label}</span>
      <span className="relative block">
        <KeyRound size={20} className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-on-surface-variant" />
        <input id={id} type={visible ? 'text' : 'password'} value={value} onChange={(event) => onChange(event.target.value)} className="h-12 w-full rounded-xl border border-outline-variant pl-11 pr-12 text-sm outline-none focus:border-primary focus:ring-1 focus:ring-primary" autoComplete="new-password" />
        <button type="button" onClick={onToggle} className="absolute right-4 top-1/2 flex -translate-y-1/2 border-0 bg-transparent p-0 text-on-surface-variant" aria-label={visible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}>
          {visible ? <Eye size={20} /> : <EyeOff size={20} />}
        </button>
      </span>
    </label>
  );
}
