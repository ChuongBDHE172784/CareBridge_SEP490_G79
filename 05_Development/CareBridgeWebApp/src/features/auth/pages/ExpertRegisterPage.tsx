import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AlertCircle, ArrowRight, BadgeCheck } from 'lucide-react';
import { registerExpert } from '../services/authApi';

type FormState = {
  name: string;
  email: string;
  phone: string;
  password: string;
  confirmPassword: string;
};

const initialForm: FormState = { name: '', email: '', phone: '', password: '', confirmPassword: '' };

export default function ExpertRegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState(initialForm);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const update = (key: keyof FormState) => (event: React.ChangeEvent<HTMLInputElement>) =>
    setForm((current) => ({ ...current, [key]: event.target.value }));

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    if (!form.name.trim() || !form.email.trim()) {
      setError('Vui lòng nhập họ tên và email.');
      return;
    }
    if (form.password.length < 8) {
      setError('Mật khẩu phải có ít nhất 8 ký tự.');
      return;
    }
    if (form.password !== form.confirmPassword) {
      setError('Mật khẩu xác nhận không khớp.');
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
      const apiError = caught as { response?: { data?: { message?: string } } };
      setError(apiError.response?.data?.message ?? 'Không thể tạo tài khoản chuyên gia. Vui lòng thử lại.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#f7f2ee] px-5 py-10 font-sans">
      <main className="mx-auto grid max-w-5xl overflow-hidden rounded-3xl bg-white shadow-xl lg:grid-cols-[0.9fr_1.1fr]">
        <section className="bg-primary p-10 text-white">
          <BadgeCheck size={52} />
          <h1 className="mt-8 text-3xl font-semibold">Đăng ký trở thành chuyên gia</h1>
          <p className="mt-4 leading-7 text-white/85">
            Tạo tài khoản trước, sau đó hoàn thành hồ sơ, chụp ảnh chân dung, hai mặt CCCD và nộp chứng chỉ chuyên môn.
          </p>
          <div className="mt-8 rounded-2xl bg-white/10 p-5 text-sm leading-6">
            Ảnh định danh được lưu riêng tư. Kết quả đối chiếu khuôn mặt chỉ hỗ trợ quản trị viên đánh giá và không tự động phê duyệt hồ sơ.
          </div>
        </section>

        <section className="p-8 sm:p-12">
          <h2 className="text-2xl font-semibold text-on-surface">Thông tin tài khoản</h2>
          <p className="mt-2 text-sm text-on-surface-variant">Các trường có dấu * là bắt buộc.</p>
          {error && (
            <div className="mt-5 flex gap-3 rounded-xl bg-red-50 p-4 text-sm text-red-700" role="alert">
              <AlertCircle size={20} className="shrink-0" />
              {error}
            </div>
          )}
          <form className="mt-6 grid gap-5" onSubmit={submit}>
            <Field label="Họ và tên *" value={form.name} onChange={update('name')} autoComplete="name" />
            <Field label="Email *" type="email" value={form.email} onChange={update('email')} autoComplete="email" />
            <Field label="Số điện thoại" type="tel" value={form.phone} onChange={update('phone')} autoComplete="tel" />
            <div className="grid gap-5 sm:grid-cols-2">
              <Field label="Mật khẩu *" type="password" value={form.password} onChange={update('password')} autoComplete="new-password" />
              <Field label="Nhập lại mật khẩu *" type="password" value={form.confirmPassword} onChange={update('confirmPassword')} autoComplete="new-password" />
            </div>
            <button className="mt-2 flex h-12 items-center justify-center gap-2 rounded-full bg-primary font-semibold text-white disabled:opacity-60" disabled={submitting}>
              {submitting ? 'Đang tạo tài khoản...' : 'Tạo tài khoản chuyên gia'}
              {!submitting && <ArrowRight size={19} />}
            </button>
          </form>
          <p className="mt-6 text-center text-sm text-on-surface-variant">
            Đã có tài khoản? <Link className="font-semibold text-primary" to="/login">Đăng nhập</Link>
          </p>
        </section>
      </main>
    </div>
  );
}

function Field(props: React.InputHTMLAttributes<HTMLInputElement> & { label: string }) {
  const { label, ...inputProps } = props;
  return (
    <label className="grid gap-2 text-sm font-medium text-on-surface">
      {label}
      <input
        {...inputProps}
        required={label.includes('*')}
        className="h-12 rounded-xl border border-outline-variant px-4 font-normal outline-none focus:border-primary focus:ring-1 focus:ring-primary"
      />
    </label>
  );
}
