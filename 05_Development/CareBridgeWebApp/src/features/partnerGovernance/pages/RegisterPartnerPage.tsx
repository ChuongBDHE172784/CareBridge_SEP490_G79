import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { registerPartnerAccount } from '../services/partnerApi';
import { ORG_TYPE_LABELS } from '../models/partner';
import type { OrganizationType } from '../models/partner';

const FONT_FAMILY = "'Lexend', sans-serif";

const COLORS = {
  primary: '#845143',
  primaryContainer: '#C98C7B',
  surface: '#FFF8F6',
  bodyBg: '#F6F1EC',
  onSurface: '#271812',
  onSurfaceVariant: '#524440',
  outline: '#84736f',
  outlineVariant: '#D6C2BD',
  error: '#BA1A1A',
  secondaryContainer: '#F6DACF',
  cardShadow: '0 4px 20px rgba(90,70,63,0.06)',
};

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

  /* ── Shared input styles ── */
  const inputWrapperStyle = (hasError: boolean): React.CSSProperties => ({
    display: 'flex',
    alignItems: 'center',
    border: `1px solid ${hasError ? COLORS.error : COLORS.outlineVariant}`,
    borderRadius: 16,
    padding: '0 16px',
    backgroundColor: '#fff',
    transition: 'border-color 0.2s',
  });

  const inputStyle: React.CSSProperties = {
    flex: 1,
    border: 'none',
    outline: 'none',
    padding: '14px 0 14px 12px',
    fontFamily: FONT_FAMILY,
    fontSize: 14,
    color: COLORS.onSurface,
    backgroundColor: 'transparent',
  };

  const labelStyle = (hasError: boolean): React.CSSProperties => ({
    fontSize: 12,
    fontWeight: 600,
    color: hasError ? COLORS.error : COLORS.onSurfaceVariant,
    marginBottom: 6,
    letterSpacing: 0.5,
  });

  const iconStyle: React.CSSProperties = {
    fontSize: 20,
    color: COLORS.outline,
  };

  const errorTextStyle: React.CSSProperties = {
    fontSize: 12,
    color: COLORS.error,
    marginTop: 4,
  };

  return (
    <div style={{ minHeight: '100vh', backgroundColor: COLORS.bodyBg, fontFamily: FONT_FAMILY }}>
      {/* ── Top Nav ── */}
      <nav
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '16px 48px',
          backgroundColor: '#fff',
          boxShadow: COLORS.cardShadow,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 32 }}>
          <span
            style={{ fontSize: 22, fontWeight: 700, color: COLORS.primary, cursor: 'pointer' }}
            onClick={() => navigate('/partner')}
          >
            CareBridge
          </span>
          <div style={{ display: 'flex', gap: 24 }}>
            {['Dịch vụ', 'Về chúng tôi', 'Liên hệ'].map((label) => (
              <span
                key={label}
                style={{
                  fontSize: 14,
                  fontWeight: 500,
                  color: COLORS.onSurfaceVariant,
                  cursor: 'pointer',
                }}
              >
                {label}
              </span>
            ))}
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <span
            style={{
              fontSize: 14,
              fontWeight: 500,
              color: COLORS.onSurfaceVariant,
              cursor: 'pointer',
            }}
            onClick={() => navigate('/login')}
          >
            Đăng nhập
          </span>
          <button
            type="button"
            onClick={() => navigate('/partner/register')}
            style={{
              padding: '8px 24px',
              borderRadius: 9999,
              border: 'none',
              backgroundColor: COLORS.primaryContainer,
              color: '#fff',
              fontFamily: FONT_FAMILY,
              fontSize: 14,
              fontWeight: 500,
              cursor: 'pointer',
            }}
          >
            Đăng ký
          </button>
        </div>
      </nav>

      {/* ── Registration Form ── */}
      <section
        style={{
          display: 'flex',
          justifyContent: 'center',
          padding: '48px 24px 64px',
        }}
      >
        <div
          style={{
            backgroundColor: '#fff',
            borderRadius: 24,
            padding: '40px 48px',
            maxWidth: 600,
            width: '100%',
            boxShadow: COLORS.cardShadow,
          }}
        >
          {/* Header icon */}
          <div
            style={{
              width: 56,
              height: 56,
              borderRadius: '50%',
              backgroundColor: COLORS.primaryContainer,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 20px',
            }}
          >
            <span className="material-symbols-outlined" style={{ fontSize: 28, color: '#fff' }}>
              handshake
            </span>
          </div>

          <h1
            style={{
              fontSize: 24,
              fontWeight: 700,
              color: COLORS.onSurface,
              textAlign: 'center',
              margin: '0 0 8px',
            }}
          >
            Đăng ký Đối tác
          </h1>
          <p
            style={{
              fontSize: 14,
              color: COLORS.onSurfaceVariant,
              textAlign: 'center',
              margin: '0 0 32px',
            }}
          >
            Tham gia mạng lưới chăm sóc sức khỏe uy tín.
          </p>

          {/* Server error banner */}
          {serverError && (
            <div
              style={{
                backgroundColor: '#FDECEA',
                border: `1px solid ${COLORS.error}`,
                borderRadius: 12,
                padding: '12px 16px',
                marginBottom: 24,
                display: 'flex',
                alignItems: 'center',
                gap: 8,
              }}
            >
              <span className="material-symbols-outlined" style={{ fontSize: 20, color: COLORS.error }}>
                error
              </span>
              <span style={{ fontSize: 13, color: COLORS.error }}>{serverError}</span>
            </div>
          )}

          <form onSubmit={handleSubmit}>
            {/* Row 1: Name + Phone */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 16 }}>
              <div>
                <label style={labelStyle(!!errors.name)}>HỌ VÀ TÊN</label>
                <div style={inputWrapperStyle(!!errors.name)}>
                  <span className="material-symbols-outlined" style={iconStyle}>person</span>
                  <input
                    type="text"
                    placeholder="Nguyễn Văn A"
                    value={form.name}
                    onChange={(e) => updateField('name', e.target.value)}
                    style={inputStyle}
                  />
                </div>
                {errors.name && <p style={errorTextStyle}>{errors.name}</p>}
              </div>
              <div>
                <label style={labelStyle(!!errors.phone)}>SỐ ĐIỆN THOẠI</label>
                <div style={inputWrapperStyle(!!errors.phone)}>
                  <span className="material-symbols-outlined" style={iconStyle}>phone</span>
                  <input
                    type="tel"
                    placeholder="09xx xxx xxx"
                    value={form.phone}
                    onChange={(e) => updateField('phone', e.target.value)}
                    style={inputStyle}
                  />
                </div>
                {errors.phone && <p style={errorTextStyle}>{errors.phone}</p>}
              </div>
            </div>

            {/* Row 2: Email */}
            <div style={{ marginBottom: 16 }}>
              <label style={labelStyle(!!errors.email)}>EMAIL CÔNG VIỆC</label>
              <div style={inputWrapperStyle(!!errors.email)}>
                <span className="material-symbols-outlined" style={iconStyle}>mail</span>
                <input
                  type="email"
                  placeholder="contact@example.com"
                  value={form.email}
                  onChange={(e) => updateField('email', e.target.value)}
                  style={inputStyle}
                />
              </div>
              {errors.email && <p style={errorTextStyle}>{errors.email}</p>}
            </div>

            {/* Row 3: Org name + Org type */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 16 }}>
              <div>
                <label style={labelStyle(!!errors.orgName)}>TÊN TỔ CHỨC</label>
                <div style={inputWrapperStyle(!!errors.orgName)}>
                  <span className="material-symbols-outlined" style={iconStyle}>business</span>
                  <input
                    type="text"
                    placeholder="Tên tổ chức"
                    value={form.orgName}
                    onChange={(e) => updateField('orgName', e.target.value)}
                    style={inputStyle}
                  />
                </div>
                {errors.orgName && <p style={errorTextStyle}>{errors.orgName}</p>}
              </div>
              <div>
                <label style={labelStyle(!!errors.orgType)}>LOẠI TỔ CHỨC</label>
                <div style={inputWrapperStyle(!!errors.orgType)}>
                  <span className="material-symbols-outlined" style={iconStyle}>add_business</span>
                  <select
                    value={form.orgType}
                    onChange={(e) => updateField('orgType', e.target.value as OrganizationType | '')}
                    style={{
                      ...inputStyle,
                      appearance: 'none',
                      cursor: 'pointer',
                      color: form.orgType ? COLORS.onSurface : COLORS.outline,
                    }}
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
                {errors.orgType && <p style={errorTextStyle}>{errors.orgType}</p>}
              </div>
            </div>

            {/* Divider */}
            <hr
              style={{
                border: 'none',
                borderTop: `1px solid ${COLORS.outlineVariant}`,
                margin: '24px 0',
              }}
            />

            {/* Row 4: Password + Confirm */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 24 }}>
              <div>
                <label style={labelStyle(!!errors.password)}>MẬT KHẨU</label>
                <div style={inputWrapperStyle(!!errors.password)}>
                  <span className="material-symbols-outlined" style={iconStyle}>lock</span>
                  <input
                    type={showPassword ? 'text' : 'password'}
                    placeholder="Tối thiểu 8 ký tự"
                    value={form.password}
                    onChange={(e) => updateField('password', e.target.value)}
                    style={inputStyle}
                  />
                  <span
                    className="material-symbols-outlined"
                    style={{ ...iconStyle, cursor: 'pointer' }}
                    onClick={() => setShowPassword((v) => !v)}
                  >
                    {showPassword ? 'visibility' : 'visibility_off'}
                  </span>
                </div>
                {errors.password && <p style={errorTextStyle}>{errors.password}</p>}
              </div>
              <div>
                <label style={labelStyle(!!errors.confirmPassword)}>XÁC NHẬN MẬT KHẨU</label>
                <div style={inputWrapperStyle(!!errors.confirmPassword)}>
                  <span className="material-symbols-outlined" style={iconStyle}>lock</span>
                  <input
                    type={showConfirmPassword ? 'text' : 'password'}
                    placeholder="Nhập lại mật khẩu"
                    value={form.confirmPassword}
                    onChange={(e) => updateField('confirmPassword', e.target.value)}
                    style={inputStyle}
                  />
                  <span
                    className="material-symbols-outlined"
                    style={{ ...iconStyle, cursor: 'pointer' }}
                    onClick={() => setShowConfirmPassword((v) => !v)}
                  >
                    {showConfirmPassword ? 'visibility' : 'visibility_off'}
                  </span>
                </div>
                {errors.confirmPassword && (
                  <p style={errorTextStyle}>{errors.confirmPassword}</p>
                )}
              </div>
            </div>

            {/* Terms checkbox */}
            <div style={{ marginBottom: 24 }}>
              <label
                style={{
                  display: 'flex',
                  alignItems: 'flex-start',
                  gap: 10,
                  fontSize: 13,
                  color: COLORS.onSurfaceVariant,
                  cursor: 'pointer',
                  lineHeight: 1.5,
                }}
              >
                <input
                  type="checkbox"
                  checked={form.termsAccepted}
                  onChange={(e) => updateField('termsAccepted', e.target.checked)}
                  style={{ marginTop: 3, accentColor: COLORS.primaryContainer }}
                />
                <span>
                  Tôi đồng ý với các{' '}
                  <span style={{ color: COLORS.primary, fontWeight: 600, cursor: 'pointer' }}>
                    Điều khoản sử dụng
                  </span>{' '}
                  và{' '}
                  <span style={{ color: COLORS.primary, fontWeight: 600, cursor: 'pointer' }}>
                    Chính sách bảo mật
                  </span>{' '}
                  của CareBridge dành cho đối tác.
                </span>
              </label>
              {errors.terms && <p style={errorTextStyle}>{errors.terms}</p>}
            </div>

            {/* Submit button */}
            <button
              type="submit"
              disabled={isSubmitting}
              style={{
                width: '100%',
                padding: '14px 0',
                borderRadius: 9999,
                border: 'none',
                backgroundColor: isSubmitting ? COLORS.outlineVariant : COLORS.primaryContainer,
                color: '#fff',
                fontFamily: FONT_FAMILY,
                fontSize: 15,
                fontWeight: 600,
                cursor: isSubmitting ? 'not-allowed' : 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 8,
              }}
            >
              {isSubmitting ? 'Đang xử lý...' : 'Tiếp tục'}
              {!isSubmitting && (
                <span className="material-symbols-outlined" style={{ fontSize: 20, color: '#fff' }}>
                  arrow_forward
                </span>
              )}
            </button>
          </form>

          {/* Login link */}
          <p
            style={{
              textAlign: 'center',
              fontSize: 14,
              color: COLORS.onSurfaceVariant,
              marginTop: 24,
              marginBottom: 0,
            }}
          >
            Đã có tài khoản?{' '}
            <span
              style={{ color: COLORS.primary, fontWeight: 700, cursor: 'pointer' }}
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
