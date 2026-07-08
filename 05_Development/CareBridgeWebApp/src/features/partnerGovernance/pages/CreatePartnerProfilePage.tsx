import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createPartnerProfile } from '../services/partnerApi';
import { ORG_TYPE_LABELS } from '../models/partner';
import type { OrganizationType } from '../models/partner';

interface FormState {
  name: string;
  type: OrganizationType | '';
  address: string;
  city: string;
  phone: string;
  email: string;
  website: string;
  description: string;
}

interface FormErrors {
  name?: string;
  type?: string;
  address?: string;
  city?: string;
  phone?: string;
  email?: string;
}

const DEFAULT_FORM: FormState = {
  name: '',
  type: '',
  address: '',
  city: '',
  phone: '',
  email: '',
  website: '',
  description: '',
};

function validate(form: FormState): FormErrors {
  const errors: FormErrors = {};
  if (!form.name.trim()) errors.name = 'Vui lòng nhập tên tổ chức';
  if (!form.type) errors.type = 'Vui lòng chọn loại hình tổ chức';
  if (!form.address.trim()) errors.address = 'Vui lòng nhập địa chỉ';
  if (!form.city.trim()) errors.city = 'Vui lòng nhập thành phố';
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
  return errors;
}

// CB-099 — Partner Profile Setup (UC-118). The design mockup shows a 5-step wizard
// (Tổ chức/Đại diện/Cơ sở/Dịch vụ/Tài liệu), but the real backend contract
// (CreatePartnerProfileRequest) only accepts one flat set of fields — no representative,
// facility, service, or document sub-resources exist yet. This form collects exactly
// what /api/v1/partner/profile accepts rather than fabricating steps with no endpoint.
export default function CreatePartnerProfilePage() {
  const navigate = useNavigate();
  const [form, setForm] = useState<FormState>(DEFAULT_FORM);
  const [errors, setErrors] = useState<FormErrors>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

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
      await createPartnerProfile({
        name: form.name.trim(),
        type: form.type as OrganizationType,
        address: form.address.trim(),
        city: form.city.trim(),
        phone: form.phone.trim(),
        email: form.email.trim(),
        website: form.website.trim(),
        description: form.description.trim(),
      });
      navigate('/partner/dashboard', { replace: true });
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } } };
      setServerError(error.response?.data?.message ?? 'Đã xảy ra lỗi. Vui lòng thử lại sau.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const inputWrapperCls = (hasError: boolean) =>
    `flex items-center border rounded-2xl px-4 bg-white transition-[border-color] duration-200 ${hasError ? 'border-error' : 'border-outline-variant'}`;
  const inputCls =
    'flex-1 border-none outline-none py-3.5 pl-3 pr-0 font-sans text-sm text-on-surface bg-transparent';
  const labelCls = (hasError: boolean) =>
    `block text-xs font-semibold mb-[6px] tracking-[0.5px] ${hasError ? 'text-error' : 'text-on-surface-variant'}`;
  const errorTextCls = 'text-xs text-error mt-1';

  return (
    <div className="min-h-screen bg-[#F6F1EC] font-sans">
      <nav className="flex items-center justify-between py-4 px-12 bg-white shadow-[0_4px_20px_rgba(90,70,63,0.06)]">
        <span className="text-[22px] font-bold text-primary">CareBridge</span>
        <span className="text-xs font-semibold text-on-surface-variant bg-surface-container-low px-4 py-2 rounded-full">
          Thiết lập hồ sơ đối tác
        </span>
      </nav>

      <section className="flex justify-center pt-12 px-6 pb-16">
        <div className="bg-white rounded-3xl py-10 px-12 max-w-[640px] w-full shadow-[0_4px_20px_rgba(90,70,63,0.06)]">
          <div className="w-14 h-14 rounded-full bg-primary-container flex items-center justify-center mx-auto mb-5">
            <span className="material-symbols-outlined text-white text-[28px]">business</span>
          </div>

          <h1 className="text-2xl font-bold text-on-surface text-center mb-2">Chào mừng đối tác mới</h1>
          <p className="text-sm text-on-surface-variant text-center mb-8">
            Vui lòng cung cấp thông tin để CareBridge xác thực dịch vụ của bạn.
          </p>

          {serverError && (
            <div className="bg-[#FDECEA] border border-error rounded-xl px-4 py-3 mb-6 flex items-center gap-2">
              <span className="material-symbols-outlined text-error text-xl">error</span>
              <span className="text-[13px] text-error">{serverError}</span>
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <div className="mb-4">
              <label className={labelCls(!!errors.name)}>TÊN TỔ CHỨC / PHÒNG KHÁM *</label>
              <div className={inputWrapperCls(!!errors.name)}>
                <input
                  type="text"
                  placeholder="Nhập tên tổ chức..."
                  value={form.name}
                  onChange={(e) => updateField('name', e.target.value)}
                  className={inputCls}
                />
              </div>
              {errors.name && <p className={errorTextCls}>{errors.name}</p>}
            </div>

            <div className="mb-4">
              <label className={labelCls(!!errors.type)}>LOẠI HÌNH TỔ CHỨC *</label>
              <div className="flex flex-wrap gap-3">
                {(Object.entries(ORG_TYPE_LABELS) as [OrganizationType, string][]).map(([value, label]) => (
                  <button
                    type="button"
                    key={value}
                    onClick={() => updateField('type', value)}
                    className={`px-4 py-2.5 rounded-full border text-sm font-medium transition-colors ${
                      form.type === value
                        ? 'bg-primary-container border-primary-container text-white'
                        : 'bg-white border-outline-variant text-on-surface'
                    }`}
                  >
                    {label}
                  </button>
                ))}
              </div>
              {errors.type && <p className={errorTextCls}>{errors.type}</p>}
            </div>

            <div className="grid grid-cols-2 gap-4 mb-4">
              <div>
                <label className={labelCls(!!errors.address)}>ĐỊA CHỈ *</label>
                <div className={inputWrapperCls(!!errors.address)}>
                  <input
                    type="text"
                    placeholder="Số nhà, đường..."
                    value={form.address}
                    onChange={(e) => updateField('address', e.target.value)}
                    className={inputCls}
                  />
                </div>
                {errors.address && <p className={errorTextCls}>{errors.address}</p>}
              </div>
              <div>
                <label className={labelCls(!!errors.city)}>THÀNH PHỐ *</label>
                <div className={inputWrapperCls(!!errors.city)}>
                  <input
                    type="text"
                    placeholder="TP. Hồ Chí Minh"
                    value={form.city}
                    onChange={(e) => updateField('city', e.target.value)}
                    className={inputCls}
                  />
                </div>
                {errors.city && <p className={errorTextCls}>{errors.city}</p>}
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4 mb-4">
              <div>
                <label className={labelCls(!!errors.phone)}>SỐ ĐIỆN THOẠI *</label>
                <div className={inputWrapperCls(!!errors.phone)}>
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
              <div>
                <label className={labelCls(!!errors.email)}>EMAIL LIÊN HỆ *</label>
                <div className={inputWrapperCls(!!errors.email)}>
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
            </div>

            <div className="mb-4">
              <label className={labelCls(false)}>WEBSITE (TÙY CHỌN)</label>
              <div className={inputWrapperCls(false)}>
                <input
                  type="url"
                  placeholder="https://..."
                  value={form.website}
                  onChange={(e) => updateField('website', e.target.value)}
                  className={inputCls}
                />
              </div>
            </div>

            <div className="mb-6">
              <label className={labelCls(false)}>MÔ TẢ NGẮN GỌN</label>
              <textarea
                placeholder="Giới thiệu về dịch vụ và triết lý chăm sóc của bạn..."
                value={form.description}
                onChange={(e) => updateField('description', e.target.value)}
                rows={4}
                className="w-full border border-outline-variant rounded-2xl px-4 py-3.5 font-sans text-sm text-on-surface bg-white outline-none resize-none focus:border-primary-container"
              />
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className={`w-full py-[14px] rounded-full border-none text-white font-sans text-[15px] font-semibold flex items-center justify-center gap-2 ${isSubmitting ? 'bg-outline-variant cursor-not-allowed' : 'bg-primary-container cursor-pointer'}`}
            >
              {isSubmitting ? 'Đang gửi...' : 'Gửi hồ sơ xác thực'}
              {!isSubmitting && <span className="material-symbols-outlined text-white text-xl">arrow_forward</span>}
            </button>
          </form>
        </div>
      </section>
    </div>
  );
}
