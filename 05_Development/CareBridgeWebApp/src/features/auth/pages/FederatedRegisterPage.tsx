import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AlertCircle, ArrowRight, CheckCircle2, LoaderCircle } from 'lucide-react';
import { googleIdToken } from '../services/firebaseAuth';
import { federatedAuthenticate } from '../services/authApi';
import type { RegistrationDraft } from '../models/auth';
import { useAuthStore } from '../../../shared/auth/authStore';
import { getDefaultRouteForRole } from '../../../shared/auth/roleRoutes';
import {
  clearRegistrationDraft,
  getRegistrationDraft,
  setRegistrationDraft,
} from '../services/registrationVerificationCoordinator';

type FormState = Omit<RegistrationDraft, 'role'> & {
  confirmPassword: string;
  role: 'MOTHER' | 'FAMILY';
};

const VIETNAMESE_PHONE_PATTERN = /^\+84[35789]\d{8}$/;
const isStrongPassword = (password: string) =>
  password.length >= 8
  && /[A-Z]/.test(password)
  && /[a-z]/.test(password)
  && /\d/.test(password)
  && /[^A-Za-z0-9]/.test(password);

const initialForm: FormState = {
  name: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: '',
  role: 'MOTHER',
};

export default function FederatedRegisterPage() {
  const navigate = useNavigate();
  const handoffDraft = useRef(false);
  const [form, setForm] = useState<FormState>(() => {
    const draft = getRegistrationDraft();
    return draft && (draft.role === 'MOTHER' || draft.role === 'FAMILY')
      ? { ...draft, role: draft.role, confirmPassword: '' }
      : initialForm;
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => () => {
    if (!handoffDraft.current) clearRegistrationDraft();
  }, []);

  const update = (key: keyof FormState) => (
    event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>,
  ) => {
    setForm((current) => ({ ...current, [key]: event.target.value }));
    setError(null);
  };

  const validate = () => {
    if (!form.name.trim()) return 'Vui lòng nhập họ và tên.';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
      return 'Vui lòng nhập email hợp lệ.';
    }
    if (!VIETNAMESE_PHONE_PATTERN.test(form.phone.trim())) {
      return 'Số điện thoại cần là số di động Việt Nam dạng +84xxxxxxxxx.';
    }
    if (!isStrongPassword(form.password)) {
      return 'Mật khẩu cần có ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.';
    }
    if (form.password !== form.confirmPassword) return 'Mật khẩu xác nhận không khớp.';
    return null;
  };

  const submitForm = (event: React.FormEvent) => {
    event.preventDefault();
    if (handoffDraft.current) return;
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }
    const draft: RegistrationDraft = {
      name: form.name.trim(),
      email: form.email.trim().toLowerCase(),
      phone: form.phone.trim(),
      password: form.password,
      role: form.role,
    };
    setError(null);
    setSubmitting(true);
    setRegistrationDraft(draft);
    handoffDraft.current = true;
    navigate('/register/verify');
  };

  const registerWithGoogle = async () => {
    setSubmitting(true);
    setError(null);
    try {
      const auth = await federatedAuthenticate(await googleIdToken());
      clearRegistrationDraft();
      useAuthStore.getState().setTokens(auth.accessToken, auth.refreshToken);
      useAuthStore.getState().setUser(null);
      if (auth.user.role) {
        useAuthStore.getState().setUser({
          id: auth.user.id,
          phone: auth.user.phone ?? '',
          name: auth.user.name,
          avatarUrl: auth.user.avatarUrl,
          role: auth.user.role,
        });
      }
      navigate(
        auth.profileCompleted && auth.user.role
          ? getDefaultRouteForRole(auth.user.role)
          : '/account/profile',
        { replace: true },
      );
    } catch (caught) {
      if (caught instanceof Error && caught.message.includes('popup-closed')) return;
      setError('Không thể đăng ký bằng Google. Vui lòng thử lại hoặc dùng email.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="min-h-screen bg-[#F6F1EC] px-4 py-8 font-sans text-[#5A463F] sm:px-6">
      <section className="mx-auto grid w-full max-w-5xl overflow-hidden rounded-[2rem] bg-white shadow-[0_18px_60px_rgba(90,70,63,0.10)] lg:grid-cols-[0.9fr_1.1fr]">
        <aside className="relative hidden flex-col justify-between bg-[#845143] p-10 text-white lg:flex">
          <div>
            <div className="inline-flex rounded-2xl bg-white/10 p-3">
              <CheckCircle2 size={28} aria-hidden="true" />
            </div>
            <h1 className="mt-8 text-3xl font-bold leading-tight tracking-tight">Tạo tài khoản CareBridge</h1>
            <p className="mt-4 max-w-sm text-sm leading-6 text-white/80">
              Dùng cả email và số điện thoại để bảo vệ tài khoản và hỗ trợ khôi phục khi cần.
            </p>
          </div>
          <p className="text-xs text-white/65">Thông tin được mã hóa và chỉ dùng cho việc chăm sóc của bạn.</p>
        </aside>

        <section className="p-6 sm:p-10">
          <div className="mb-7">
            <h2 className="text-2xl font-bold text-[#271812]">Đăng ký</h2>
            <p className="mt-2 text-sm leading-6 text-[#524440]">
              Nhập cả email và số điện thoại. Bạn sẽ chọn kênh nhận mã ở bước tiếp theo.
            </p>
          </div>

          {error && (
            <div role="alert" className="mb-6 flex items-start gap-3 rounded-xl bg-[#ffdad6] p-4 text-sm leading-5 text-[#93000a]">
              <AlertCircle size={20} className="mt-0.5 shrink-0" />
              <span>{error}</span>
            </div>
          )}
          <p role="status" aria-live="polite" className="sr-only">{submitting ? 'Đang xử lý' : ''}</p>

          <form className="grid gap-5" onSubmit={submitForm} noValidate>
            <Field id="register-name" name="name" label="Họ và tên" value={form.name} onChange={update('name')} autoComplete="name" required />
            <div className="grid gap-5 sm:grid-cols-2">
              <Field id="register-email" name="email" label="Email" type="email" value={form.email} onChange={update('email')} autoComplete="email" required />
              <Field id="register-phone" name="phone" label="Số điện thoại" type="tel" value={form.phone} onChange={update('phone')} autoComplete="tel" placeholder="+84901234567" required />
            </div>

            <label className="grid gap-2 text-sm font-semibold" htmlFor="register-role">Vai trò</label>
            <select id="register-role" className="min-h-12 rounded-xl border border-[#d6c2bd] bg-white px-4 text-sm outline-none transition focus:border-[#845143] focus:ring-2 focus:ring-[#845143]" value={form.role} onChange={update('role')}>
              <option value="MOTHER">Mẹ bầu</option>
              <option value="FAMILY">Thành viên gia đình</option>
            </select>

            <div className="grid gap-5 sm:grid-cols-2">
              <Field id="register-password" name="password" label="Mật khẩu" type="password" value={form.password} onChange={update('password')} autoComplete="new-password" required />
              <Field id="register-confirm-password" name="confirmPassword" label="Nhập lại mật khẩu" type="password" value={form.confirmPassword} onChange={update('confirmPassword')} autoComplete="new-password" required />
            </div>

            <button type="submit" className="mt-2 flex min-h-12 w-full items-center justify-center gap-2 rounded-full bg-[#C98C7B] px-5 text-base font-semibold text-white transition hover:bg-[#845143] focus:outline-none focus:ring-4 focus:ring-[#C98C7B]/30 active:scale-[0.99] disabled:cursor-not-allowed disabled:opacity-60" disabled={submitting}>
              {submitting ? <LoaderCircle className="animate-spin" size={20} aria-hidden="true" /> : <>Tiếp tục xác thực<ArrowRight size={20} aria-hidden="true" /></>}
            </button>
            <div className="flex items-center gap-3 text-xs text-[#524440]/70">
              <span className="h-px flex-1 bg-[#d6c2bd]" /><span>hoặc</span><span className="h-px flex-1 bg-[#d6c2bd]" />
            </div>
            <button type="button" aria-label="Sign up with Google" onClick={registerWithGoogle} disabled={submitting} className="min-h-12 w-full rounded-full border border-[#d6c2bd] bg-white px-5 text-sm font-semibold text-[#524440] transition hover:border-[#845143] focus:outline-none focus:ring-4 focus:ring-[#C98C7B]/20 disabled:opacity-60">
              Đăng ký bằng Google
            </button>
          </form>

          <p className="mt-8 text-center text-sm text-[#524440]">
            Đã có tài khoản? <Link className="font-semibold text-[#845143] underline focus:outline-none focus:ring-2 focus:ring-[#845143]" to="/login">Đăng nhập</Link>
          </p>
        </section>
      </section>
    </main>
  );
}

function Field(props: React.InputHTMLAttributes<HTMLInputElement> & { label: string }) {
  const { label, id, ...inputProps } = props;
  return (
    <label className="grid gap-2 text-sm font-semibold text-[#271812]" htmlFor={id}>
      {label}{props.required && <span aria-hidden="true"> *</span>}
      <input id={id} {...inputProps} className="min-h-12 rounded-xl border border-[#d6c2bd] bg-white px-4 text-sm font-normal outline-none transition placeholder:text-[#524440]/60 focus:border-[#845143] focus:ring-2 focus:ring-[#845143]" />
    </label>
  );
}
