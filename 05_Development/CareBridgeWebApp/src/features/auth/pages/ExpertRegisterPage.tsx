import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AlertCircle, ArrowRight, ShieldCheck, Stethoscope, CheckCircle2 } from 'lucide-react';
import { registerExpert } from '../services/authApi';

type FormState = {
  name: string;
  email: string;
  phone: string;
  password: string;
  confirmPassword: string;
};

type FieldErrors = Partial<Record<keyof FormState, string>>;

const initialForm: FormState = { name: '', email: '', phone: '', password: '', confirmPassword: '' };

export default function ExpertRegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState(initialForm);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [submitting, setSubmitting] = useState(false);

  const update = (key: keyof FormState) => (event: React.ChangeEvent<HTMLInputElement>) => {
    setForm((current) => ({ ...current, [key]: event.target.value }));
    setFieldErrors((current) => {
      if (!current[key]) return current;
      const next = { ...current };
      delete next[key];
      return next;
    });
    setError(null);
  };

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    setFieldErrors({});
    if (!form.name.trim() || !form.email.trim()) {
      setFieldErrors({
        ...(!form.name.trim() ? { name: 'Vui lòng nhập họ và tên.' } : {}),
        ...(!form.email.trim() ? { email: 'Vui lòng nhập email.' } : {}),
      });
      return;
    }
    if (form.password.length < 8) {
      setFieldErrors({ password: 'Mật khẩu phải có ít nhất 8 ký tự.' });
      return;
    }
    if (form.password !== form.confirmPassword) {
      setFieldErrors({ confirmPassword: 'Mật khẩu xác nhận không khớp.' });
      return;
    }

    setSubmitting(true);
    try {
      const result = await registerExpert({
        name: form.name.trim(),
        email: form.email.trim(),
        phone: form.phone.trim() || undefined,
        password: form.password,
      });
      navigate('/login/otp', {
        state: {
          userId: result.userId,
          otpExpiresAt: result.otpExpiresAt,
          identifier: form.email.trim(),
        },
      });
    } catch (caught: unknown) {
      const apiError = caught as {
        response?: {
          data?: {
            message?: string;
            details?: Array<{ field?: string; message?: string }>;
          };
        };
      };
      const details = apiError.response?.data?.details ?? [];
      const backendFieldErrors = details.reduce<FieldErrors>((result, detail) => {
        if (isFormField(detail.field) && detail.message) {
          result[detail.field] = detail.message;
        }
        return result;
      }, {});
      if (Object.keys(backendFieldErrors).length > 0) {
        setFieldErrors(backendFieldErrors);
      } else {
        setError(apiError.response?.data?.message ?? 'Không thể tạo tài khoản chuyên gia. Vui lòng thử lại.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-background px-4 py-8 font-sans flex items-center justify-center">
      <main className="mx-auto grid w-full max-w-5xl overflow-hidden rounded-[2rem] bg-surface shadow-2xl lg:grid-cols-2">
        {/* Left Section (Hero/Information) */}
        <section className="relative flex flex-col justify-between bg-gradient-to-br from-primary to-primary-fixed-variant p-10 text-on-primary sm:p-12">
          <div className="relative z-10">
            <div className="inline-flex items-center justify-center rounded-2xl bg-white/10 p-3 backdrop-blur-md">
              <Stethoscope size={36} className="text-primary-container" />
            </div>
            <h1 className="mt-8 text-[32px] font-bold leading-tight tracking-tight text-white">
              Gia nhập đội ngũ<br />chuyên gia y tế CareBridge
            </h1>
            <p className="mt-5 text-base leading-relaxed text-white/80">
              Đồng hành cùng CareBridge mang đến sự chăm sóc tốt nhất. Hồ sơ của bạn sẽ được bảo mật và đối soát trực tiếp theo tiêu chuẩn Bộ Y Tế.
            </p>

            <div className="mt-10 space-y-4">
              {[
                'Quản lý lịch tư vấn linh hoạt',
                'Kết nối trực tiếp với bệnh nhân',
                'Bảo mật thông tin & bằng cấp',
              ].map((feature, idx) => (
                <div key={idx} className="flex items-center gap-3">
                  <CheckCircle2 size={20} className="text-primary-container" />
                  <span className="text-sm font-medium text-white/90">{feature}</span>
                </div>
              ))}
            </div>
          </div>
          
          {/* Subtle background decorative shapes */}
          <div className="absolute -bottom-24 -left-24 h-64 w-64 rounded-full bg-white/5 blur-3xl"></div>
          <div className="absolute -right-20 top-20 h-48 w-48 rounded-full bg-primary-container/10 blur-2xl"></div>
        </section>

        {/* Right Section (Form) */}
        <section className="p-8 sm:p-12">
          <div className="mb-8">
            <h2 className="text-2xl font-bold text-on-surface">Tạo tài khoản</h2>
            <p className="mt-2 text-sm text-on-surface-variant">Điền thông tin cơ bản để bắt đầu quy trình xác minh.</p>
          </div>

          {error && (
            <div className="mb-6 flex gap-3 rounded-xl bg-error-container p-4 text-sm text-on-error-container shadow-sm transition-all" role="alert">
              <AlertCircle size={20} className="shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <form className="grid gap-5" onSubmit={submit}>
            <Field name="name" label="Họ và tên" required value={form.name} error={fieldErrors.name} onChange={update('name')} autoComplete="name" placeholder="Nguyễn Văn A" />
            <Field name="email" label="Email" type="email" required value={form.email} error={fieldErrors.email} onChange={update('email')} autoComplete="email" placeholder="email@example.com" />
            <Field name="phone" label="Số điện thoại" type="tel" value={form.phone} error={fieldErrors.phone} onChange={update('phone')} autoComplete="tel" placeholder="09xxxxxxxx (Tùy chọn)" />
            <div className="grid gap-5 sm:grid-cols-2">
              <Field name="password" label="Mật khẩu" type="password" required value={form.password} error={fieldErrors.password} onChange={update('password')} autoComplete="new-password" placeholder="••••••••" />
              <Field name="confirmPassword" label="Nhập lại mật khẩu" type="password" required value={form.confirmPassword} error={fieldErrors.confirmPassword} onChange={update('confirmPassword')} autoComplete="new-password" placeholder="••••••••" />
            </div>

            <div className="mt-4 flex items-start gap-3 rounded-xl bg-surface-variant p-4">
              <ShieldCheck size={20} className="mt-0.5 shrink-0 text-primary" />
              <p className="text-xs leading-5 text-on-surface-variant">
                Bằng việc tiếp tục, bạn đồng ý với Điều khoản và Chính sách bảo mật của CareBridge. Thông tin sẽ được mã hóa an toàn.
              </p>
            </div>

            <button 
              className="group mt-2 flex h-14 w-full items-center justify-center gap-2 rounded-2xl bg-primary text-[15px] font-bold text-on-primary shadow-lg shadow-primary/25 transition-all duration-300 hover:bg-on-primary-fixed-variant hover:shadow-xl hover:shadow-primary/30 active:scale-[0.98] disabled:opacity-60 disabled:pointer-events-none" 
              disabled={submitting}
            >
              {submitting ? 'Đang xử lý...' : 'Đăng ký ngay'}
              {!submitting && <ArrowRight size={19} className="transition-transform group-hover:translate-x-1" />}
            </button>
          </form>

          <p className="mt-8 text-center text-sm font-medium text-on-surface-variant">
            Đã có tài khoản?{' '}
            <Link className="text-primary hover:text-on-primary-fixed-variant hover:underline transition-colors" to="/login">
              Đăng nhập tại đây
            </Link>
          </p>
        </section>
      </main>
    </div>
  );
}

function isFormField(field: string | undefined): field is keyof FormState {
  return field === 'name'
    || field === 'email'
    || field === 'phone'
    || field === 'password'
    || field === 'confirmPassword';
}

function Field(props: React.InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  required?: boolean;
  error?: string;
}) {
  const { label, required, error, name, ...inputProps } = props;
  const fieldId = `expert-register-${name}`;
  const errorId = `${fieldId}-error`;
  return (
    <label className="grid gap-2" htmlFor={fieldId}>
      <span className="text-[13px] font-semibold text-on-surface">
        {label} {required && <span className="text-error">*</span>}
      </span>
      <input
        {...inputProps}
        id={fieldId}
        name={name}
        required={required}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? errorId : undefined}
        className={`h-12 w-full rounded-xl border-2 bg-surface-container-lowest px-4 text-[15px] text-on-surface outline-none transition-all placeholder:text-outline focus:bg-surface focus:ring-4 ${
          error
            ? 'border-error focus:border-error focus:ring-error/10'
            : 'border-outline-variant focus:border-primary focus:ring-primary/10'
        }`}
      />
      {error && (
        <span id={errorId} className="text-xs font-medium text-error">
          {error}
        </span>
      )}
    </label>
  );
}
