import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { registerPartnerAccount } from '../services/partnerApi';
import { ORG_TYPE_LABELS } from '../models/partner';
import type { OrganizationType } from '../models/partner';

const MATERIAL_SYMBOLS_URL =
  'https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@24,400,0,0';

interface FormState {
  name: string;
  phone: string;
  email: string;
  orgName: string;
  orgType: OrganizationType | '';
  password: string;
  confirmPassword: string;
  termsAccepted: boolean;
}

interface FormErrors {
  name?: string;
  phone?: string;
  email?: string;
  orgName?: string;
  orgType?: string;
  password?: string;
  confirmPassword?: string;
  terms?: string;
}

function validate(form: FormState): FormErrors {
  const errors: FormErrors = {};

  if (!form.name.trim()) errors.name = 'Vui lòng nhập họ và tên';
  if (!form.phone.trim()) {
    errors.phone = 'Vui lòng nhập số điện thoại';
  } else if (!/^(0|\+84)(3|5|7|8|9)\d{8}$/.test(form.phone.trim())) {
    errors.phone = 'Số điện thoại không hợp lệ';
  }
  if (!form.email.trim()) {
    errors.email = 'Vui lòng nhập email';
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
    errors.email = 'Email không hợp lệ';
  }
  if (!form.orgName.trim()) errors.orgName = 'Vui lòng nhập tên tổ chức';
  if (!form.orgType) errors.orgType = 'Vui lòng chọn loại tổ chức';
  if (!form.password) {
    errors.password = 'Vui lòng nhập mật khẩu';
  } else if (form.password.length < 8) {
    errors.password = 'Mật khẩu phải có ít nhất 8 ký tự';
  }
  if (!form.confirmPassword) {
    errors.confirmPassword = 'Vui lòng xác nhận mật khẩu';
  } else if (form.password !== form.confirmPassword) {
    errors.confirmPassword = 'Mật khẩu không khớp';
  }
  if (!form.termsAccepted) errors.terms = 'Bạn cần đồng ý với điều khoản';

  return errors;
}

export default function RegisterPartnerPage() {
  const navigate = useNavigate();

  const [form, setForm] = useState<FormState>({
    name: '',
    phone: '',
    email: '',
    orgName: '',
    orgType: '',
    password: '',
    confirmPassword: '',
    termsAccepted: false,
  });

  const [errors, setErrors] = useState<FormErrors>({});
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (!document.querySelector(`link[href="${MATERIAL_SYMBOLS_URL}"]`)) {
      const link = document.createElement('link');
      link.rel = 'stylesheet';
      link.href = MATERIAL_SYMBOLS_URL;
      document.head.appendChild(link);
    }
  }, []);

  const updateField = <K extends keyof FormState>(key: K, value: FormState[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }));
    if (errors[key as keyof FormErrors]) {
      setErrors((prev) => {
        const next = { ...prev };
        delete next[key as keyof FormErrors];
        return next;
      });
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setServerError(null);

    const validationErrors = validate(form);
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    setIsSubmitting(true);
    try {
      await registerPartnerAccount({
        phone: form.phone.trim(),
        password: form.password,
        name: form.name.trim(),
        email: form.email.trim(),
      });
      navigate('/login/otp', {
        state: { identifier: form.phone.trim() },
      });
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      setServerError(
        error.response?.data?.message ?? 'Đã xảy ra lỗi. Vui lòng thử lại sau.',
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  /* ── Shared class builders ── */
  const inputWrapperCls = (hasError: boolean) =>
    `flex items-center border rounded-2xl px-4 bg-white transition-[border-color] duration-200 ${hasError ? 'border-error' : 'border-outline-variant'}`;

  const inputCls =
    'flex-1 border-none outline-none py-3.5 pl-3 pr-0 font-sans text-sm text-on-surface bg-transparent';

  const labelCls = (hasError: boolean) =>
    `block text-xs font-semibold mb-[6px] tracking-[0.5px] ${hasError ? 'text-error' : 'text-on-surface-variant'}`;

  const errorTextCls = 'text-xs text-error mt-1';

  return (
    <div className="min-h-screen bg-[#F6F1EC] font-sans">
      {/* ── Top Nav ── */}
      <nav className="flex items-center justify-between py-4 px-12 bg-white shadow-[0_4px_20px_rgba(90,70,63,0.06)]">
        <div className="flex items-center gap-8">
          <span
            className="text-[22px] font-bold text-primary cursor-pointer"
            onClick={() => navigate('/partner')}
          >
            CareBridge
          </span>
          <div className="flex gap-6">
            {['Dịch vụ', 'Về chúng tôi', 'Liên hệ'].map((label) => (
              <span
                key={label}
                className="text-sm font-medium text-on-surface-variant cursor-pointer"
              >
                {label}
              </span>
            ))}
          </div>
        </div>

        <div className="flex items-center gap-3">
          <span
            className="text-sm font-medium text-on-surface-variant cursor-pointer"
            onClick={() => navigate('/login')}
          >
            Đăng nhập
          </span>
          <button
            type="button"
            onClick={() => navigate('/partner/register')}
            className="px-6 py-2 rounded-full border-none bg-primary-container text-white font-sans text-sm font-medium cursor-pointer"
          >
            Đăng ký
          </button>
        </div>
      </nav>

      {/* ── Registration Form ── */}
      <section className="flex justify-center pt-12 px-6 pb-16">
        <div className="bg-white rounded-3xl py-10 px-12 max-w-[600px] w-full shadow-[0_4px_20px_rgba(90,70,63,0.06)]">
          {/* Header icon */}
          <div className="w-14 h-14 rounded-full bg-primary-container flex items-center justify-center mx-auto mb-5">
            <span className="material-symbols-outlined text-white" style={{ fontSize: 28 }}>
              handshake
            </span>
          </div>

          <h1 className="text-2xl font-bold text-on-surface text-center mb-2">
            Đăng ký Đối tác
          </h1>
          <p className="text-sm text-on-surface-variant text-center mb-8">
            Tham gia mạng lưới chăm sóc sức khỏe uy tín.
          </p>

          {/* Server error banner */}
          {serverError && (
            <div className="bg-[#FDECEA] border border-error rounded-xl px-4 py-3 mb-6 flex items-center gap-2">
              <span className="material-symbols-outlined text-error" style={{ fontSize: 20 }}>
                error
              </span>
              <span className="text-[13px] text-error">{serverError}</span>
            </div>
          )}

          <form onSubmit={handleSubmit}>
            {/* Row 1: Name + Phone */}
            <div className="grid grid-cols-2 gap-4 mb-4">
              <div>
                <label className={labelCls(!!errors.name)}>HỌ VÀ TÊN</label>
                <div className={inputWrapperCls(!!errors.name)}>
                  <span className="material-symbols-outlined text-outline" style={{ fontSize: 20 }}>
                    person
                  </span>
                  <input
                    type="text"
                    placeholder="Nguyễn Văn A"
                    value={form.name}
                    onChange={(e) => updateField('name', e.target.value)}
                    className={inputCls}
                  />
                </div>
                {errors.name && <p className={errorTextCls}>{errors.name}</p>}
              </div>
              <div>
                <label className={labelCls(!!errors.phone)}>SỐ ĐIỆN THOẠI</label>
                <div className={inputWrapperCls(!!errors.phone)}>
                  <span className="material-symbols-outlined text-outline" style={{ fontSize: 20 }}>
                    phone
                  </span>
                  <input
                    type="tel"
                    placeholder="09xx xxx xxx"
                    value={form.phone}
                    onChange={(e) => updateField('phone', e.target.value)}
                    className={inputCls}
                  />
                </div>
                {errors.phone && <p className={errorTextCls}>{errors.phone}</p>}
              </div>
            </div>

            {/* Row 2: Email */}
            <div className="mb-4">
              <label className={labelCls(!!errors.email)}>EMAIL CÔNG VIỆC</label>
              <div className={inputWrapperCls(!!errors.email)}>
                <span className="material-symbols-outlined text-outline" style={{ fontSize: 20 }}>
                  mail
                </span>
                <input
                  type="email"
                  placeholder="contact@example.com"
                  value={form.email}
                  onChange={(e) => updateField('email', e.target.value)}
                  className={inputCls}
                />
              </div>
              {errors.email && <p className={errorTextCls}>{errors.email}</p>}
            </div>

            {/* Row 3: Org name + Org type */}
            <div className="grid grid-cols-2 gap-4 mb-4">
              <div>
                <label className={labelCls(!!errors.orgName)}>TÊN TỔ CHỨC</label>
                <div className={inputWrapperCls(!!errors.orgName)}>
                  <span className="material-symbols-outlined text-outline" style={{ fontSize: 20 }}>
                    business
                  </span>
                  <input
                    type="text"
                    placeholder="Tên tổ chức"
                    value={form.orgName}
                    onChange={(e) => updateField('orgName', e.target.value)}
                    className={inputCls}
                  />
                </div>
                {errors.orgName && <p className={errorTextCls}>{errors.orgName}</p>}
              </div>
              <div>
                <label className={labelCls(!!errors.orgType)}>LOẠI TỔ CHỨC</label>
                <div className={inputWrapperCls(!!errors.orgType)}>
                  <span className="material-symbols-outlined text-outline" style={{ fontSize: 20 }}>
                    add_business
                  </span>
                  <select
                    value={form.orgType}
                    onChange={(e) => updateField('orgType', e.target.value as OrganizationType | '')}
                    className={`${inputCls} appearance-none cursor-pointer ${form.orgType ? 'text-on-surface' : 'text-outline'}`}
                  >
                    <option value="">Chọn loại</option>
                    {(Object.entries(ORG_TYPE_LABELS) as [OrganizationType, string][]).map(
                      ([value, label]) => (
                        <option key={value} value={value}>
                          {label}
                        </option>
                      ),
                    )}
                  </select>
                </div>
                {errors.orgType && <p className={errorTextCls}>{errors.orgType}</p>}
              </div>
            </div>

            {/* Divider */}
            <hr className="border-0 border-t border-outline-variant my-6" />

            {/* Row 4: Password + Confirm */}
            <div className="grid grid-cols-2 gap-4 mb-6">
              <div>
                <label className={labelCls(!!errors.password)}>MẬT KHẨU</label>
                <div className={inputWrapperCls(!!errors.password)}>
                  <span className="material-symbols-outlined text-outline" style={{ fontSize: 20 }}>
                    lock
                  </span>
                  <input
                    type={showPassword ? 'text' : 'password'}
                    placeholder="Tối thiểu 8 ký tự"
                    value={form.password}
                    onChange={(e) => updateField('password', e.target.value)}
                    className={inputCls}
                  />
                  <span
                    className="material-symbols-outlined text-outline cursor-pointer"
                    style={{ fontSize: 20 }}
                    onClick={() => setShowPassword((v) => !v)}
                  >
                    {showPassword ? 'visibility' : 'visibility_off'}
                  </span>
                </div>
                {errors.password && <p className={errorTextCls}>{errors.password}</p>}
              </div>
              <div>
                <label className={labelCls(!!errors.confirmPassword)}>XÁC NHẬN MẬT KHẨU</label>
                <div className={inputWrapperCls(!!errors.confirmPassword)}>
                  <span className="material-symbols-outlined text-outline" style={{ fontSize: 20 }}>
                    lock
                  </span>
                  <input
                    type={showConfirmPassword ? 'text' : 'password'}
                    placeholder="Nhập lại mật khẩu"
                    value={form.confirmPassword}
                    onChange={(e) => updateField('confirmPassword', e.target.value)}
                    className={inputCls}
                  />
                  <span
                    className="material-symbols-outlined text-outline cursor-pointer"
                    style={{ fontSize: 20 }}
                    onClick={() => setShowConfirmPassword((v) => !v)}
                  >
                    {showConfirmPassword ? 'visibility' : 'visibility_off'}
                  </span>
                </div>
                {errors.confirmPassword && (
                  <p className={errorTextCls}>{errors.confirmPassword}</p>
                )}
              </div>
            </div>

            {/* Terms checkbox */}
            <div className="mb-6">
              <label className="flex items-start gap-[10px] text-[13px] text-on-surface-variant cursor-pointer leading-[1.5]">
                <input
                  type="checkbox"
                  checked={form.termsAccepted}
                  onChange={(e) => updateField('termsAccepted', e.target.checked)}
                  className="mt-[3px] accent-primary-container"
                />
                <span>
                  Tôi đồng ý với các{' '}
                  <span className="text-primary font-semibold cursor-pointer">
                    Điều khoản sử dụng
                  </span>{' '}
                  và{' '}
                  <span className="text-primary font-semibold cursor-pointer">
                    Chính sách bảo mật
                  </span>{' '}
                  của CareBridge dành cho đối tác.
                </span>
              </label>
              {errors.terms && <p className={errorTextCls}>{errors.terms}</p>}
            </div>

            {/* Submit button */}
            <button
              type="submit"
              disabled={isSubmitting}
              className={`w-full py-[14px] rounded-full border-none text-white font-sans text-[15px] font-semibold flex items-center justify-center gap-2 ${isSubmitting ? 'bg-outline-variant cursor-not-allowed' : 'bg-primary-container cursor-pointer'}`}
            >
              {isSubmitting ? 'Đang xử lý...' : 'Tiếp tục'}
              {!isSubmitting && (
                <span className="material-symbols-outlined text-white" style={{ fontSize: 20 }}>
                  arrow_forward
                </span>
              )}
            </button>
          </form>

          {/* Login link */}
          <p className="text-center text-sm text-on-surface-variant mt-6 mb-0">
            Đã có tài khoản?{' '}
            <span
              className="text-primary font-bold cursor-pointer"
              onClick={() => navigate('/login')}
            >
              Đăng nhập ngay
            </span>
          </p>
        </div>
      </section>
    </div>
  );
}
