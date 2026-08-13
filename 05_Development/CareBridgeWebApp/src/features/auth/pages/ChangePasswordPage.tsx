import { useState } from 'react';
import { CheckCircle2, Circle, Eye, EyeOff, KeyRound, ShieldCheck, XCircle } from 'lucide-react';
import { changePassword } from '../services/authApi';

const passwordRequirements =
  'Mật khẩu mới phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường, chữ số và ký tự đặc biệt.';

function getPasswordChecks(password: string) {
  return [
    { label: 'Ít nhất 8 ký tự', met: password.length >= 8 },
    { label: 'Có ít nhất 1 chữ hoa', met: /\p{Lu}/u.test(password) },
    { label: 'Có ít nhất 1 chữ thường', met: /\p{Ll}/u.test(password) },
    { label: 'Có ít nhất 1 chữ số', met: /\p{Nd}/u.test(password) },
    { label: 'Có ít nhất 1 ký tự đặc biệt', met: /[^\p{L}\p{N}]/u.test(password) },
  ];
}

function errorMessage(error: unknown): string {
  const candidate = error as { response?: { data?: { error?: string; message?: string } } };
  const code = candidate.response?.data?.error;
  if (code === 'AUTH-071') return 'Mật khẩu hiện tại không chính xác.';
  if (code === 'AUTH-072') return 'Mật khẩu xác nhận không khớp.';
  if (code === 'AUTH-073') return passwordRequirements;
  if (code === 'AUTH-074') return 'Mật khẩu mới phải khác mật khẩu hiện tại.';
  return candidate.response?.data?.message ?? 'Không thể đổi mật khẩu. Vui lòng thử lại.';
}

export default function ChangePasswordPage() {
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const passwordChecks = getPasswordChecks(newPassword);
  const isNewPasswordValid = passwordChecks.every((check) => check.met);
  const passwordsMatch = confirmPassword.length > 0 && newPassword === confirmPassword;
  const canSubmit = Boolean(oldPassword)
    && isNewPasswordValid
    && passwordsMatch
    && oldPassword !== newPassword
    && !isSubmitting;

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSuccess(false);
    if (!oldPassword) {
      setError('Vui lòng nhập mật khẩu hiện tại.');
      return;
    }
    if (!isNewPasswordValid) {
      setError(passwordRequirements);
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('Mật khẩu xác nhận không khớp.');
      return;
    }
    if (oldPassword === newPassword) {
      setError('Mật khẩu mới phải khác mật khẩu hiện tại.');
      return;
    }

    setIsSubmitting(true);
    setError(null);
    try {
      await changePassword({ oldPassword, newPassword, confirmPassword });
      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setSuccess(true);
    } catch (changeError) {
      setError(errorMessage(changeError));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-background px-5 py-8 sm:px-8 lg:px-12">
      <div className="mx-auto max-w-3xl">
        <div className="mb-7 flex items-center gap-4">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary-container text-primary">
            <ShieldCheck size={26} />
          </div>
          <div>
            <p className="m-0 text-xs font-semibold uppercase tracking-[0.12em] text-outline">Cài đặt tài khoản</p>
            <h1 className="mb-0 mt-1 text-2xl font-bold text-on-surface">Đổi mật khẩu</h1>
          </div>
        </div>

        <section className="rounded-2xl border border-outline-variant/70 bg-surface p-6 shadow-sm sm:p-8">
          <p className="mb-6 mt-0 text-sm leading-6 text-on-surface-variant">
            Xác nhận mật khẩu hiện tại trước khi tạo mật khẩu mới. Sau khi đổi thành công, các phiên đăng nhập khác sẽ bị thu hồi để bảo vệ tài khoản.
          </p>

          {error && <div role="alert" className="mb-5 rounded-xl bg-[#ffdad6] p-4 text-sm text-[#93000a]">{error}</div>}
          {success && (
            <div role="status" className="mb-5 flex items-start gap-2 rounded-xl bg-emerald-50 p-4 text-sm text-emerald-800">
              <CheckCircle2 size={20} className="mt-0.5 shrink-0" />
              <span>Đổi mật khẩu thành công. Các phiên đăng nhập khác đã được thu hồi.</span>
            </div>
          )}

          <form className="space-y-5" onSubmit={submit}>
            <PasswordField id="current-password" label="Mật khẩu hiện tại" value={oldPassword} onChange={setOldPassword} autoComplete="current-password" />
            <PasswordField id="new-password" label="Mật khẩu mới" value={newPassword} onChange={setNewPassword} autoComplete="new-password" />
            <ul aria-label="Yêu cầu mật khẩu mới" className="grid list-none gap-2 p-0 text-xs sm:grid-cols-2" aria-live="polite">
              {passwordChecks.map((check) => (
                <li
                  key={check.label}
                  data-met={check.met}
                  className={`flex items-center gap-2 ${check.met ? 'text-emerald-700' : 'text-on-surface-variant'}`}
                >
                  {check.met
                    ? <CheckCircle2 size={16} className="shrink-0" aria-hidden="true" />
                    : <Circle size={16} className="shrink-0" aria-hidden="true" />}
                  {check.label}
                </li>
              ))}
            </ul>
            <PasswordField id="confirm-password" label="Xác nhận mật khẩu mới" value={confirmPassword} onChange={setConfirmPassword} autoComplete="new-password" />
            {confirmPassword && (
              <p
                data-testid="password-match-status"
                role="status"
                className={`m-0 flex items-center gap-2 text-xs font-medium ${passwordsMatch ? 'text-emerald-700' : 'text-[#ba1a1a]'}`}
              >
                {passwordsMatch
                  ? <CheckCircle2 size={16} aria-hidden="true" />
                  : <XCircle size={16} aria-hidden="true" />}
                {passwordsMatch ? 'Mật khẩu xác nhận đã khớp.' : 'Mật khẩu xác nhận chưa khớp.'}
              </p>
            )}
            <div className="flex justify-end border-t border-outline-variant/50 pt-5">
              <button disabled={!canSubmit} className="inline-flex h-11 items-center justify-center gap-2 rounded-full border-0 bg-primary px-6 font-semibold text-on-primary hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60">
                <KeyRound size={18} />
                {isSubmitting ? 'Đang cập nhật...' : 'Đổi mật khẩu'}
              </button>
            </div>
          </form>
        </section>
      </div>
    </div>
  );
}

interface PasswordFieldProps {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  autoComplete: string;
}

function PasswordField({ id, label, value, onChange, autoComplete }: PasswordFieldProps) {
  const [visible, setVisible] = useState(false);

  return (
    <div className="space-y-2">
      <label className="block text-sm font-semibold text-on-surface" htmlFor={id}>{label}</label>
      <span className="relative block">
        <KeyRound size={19} className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-outline" />
        <input id={id} type={visible ? 'text' : 'password'} value={value} onChange={(event) => onChange(event.target.value)} className="h-12 w-full rounded-xl border border-outline-variant bg-surface py-0 pl-11 pr-12 text-sm outline-none focus:border-primary focus:ring-1 focus:ring-primary" autoComplete={autoComplete} />
        <button
          type="button"
          onClick={() => setVisible((current) => !current)}
          className="absolute right-4 top-1/2 flex -translate-y-1/2 border-0 bg-transparent p-0 text-on-surface-variant transition-colors hover:text-primary"
          aria-label={`${visible ? 'Ẩn' : 'Hiện'} ${label.toLowerCase()}`}
        >
          {visible ? <EyeOff size={19} /> : <Eye size={19} />}
        </button>
      </span>
    </div>
  );
}
