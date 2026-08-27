import { type FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createStaffAccount } from '../services/adminUserApi';
import type { CreateStaffAccountRequest, StaffAccountResult, StaffRole } from '../models/adminUser';

const STAFF_ROLES: Array<{ value: StaffRole; label: string; description: string; icon: string }> = [
  { value: 'MODERATOR', label: 'Kiểm duyệt viên', description: 'Xử lý báo cáo và nội dung vi phạm trên cộng đồng.', icon: 'shield' },
  { value: 'CONTENT_ADMIN', label: 'Quản trị nội dung', description: 'Quản lý bài viết và kho kiến thức chăm sóc sức khỏe.', icon: 'article' },
  { value: 'SYSTEM_ADMIN', label: 'Quản trị hệ thống', description: 'Toàn quyền quản trị hệ thống và phân quyền.', icon: 'admin_panel_settings' },
];

export default function CreateStaffAccountPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState<CreateStaffAccountRequest>({ email: '', phone: '', name: '', role: 'MODERATOR' });
  const [result, setResult] = useState<StaffAccountResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setIsSubmitting(true);
    setError(null);
    try {
      setResult(await createStaffAccount({ ...form, phone: form.phone?.trim() || undefined }));
    } catch {
      setError('Không thể tạo tài khoản. Email hoặc số điện thoại có thể đã tồn tại, hoặc email thông tin đăng nhập không gửi được.');
    } finally {
      setIsSubmitting(false);
    }
  }

  if (result) {
    return (
      <div className="p-6 md:p-8 font-sans">
        <section className="mx-auto max-w-2xl rounded-2xl border border-emerald-200 bg-emerald-50/50 p-8 text-center shadow-md">
          <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-emerald-100 text-emerald-700">
            <span className="material-symbols-outlined text-3xl">mark_email_read</span>
          </div>
          <h1 className="mt-4 text-2xl font-bold text-on-surface m-0">Đã tạo tài khoản nhân viên</h1>
          <p className="mt-2 text-sm text-on-surface-variant">
            Thông tin đăng nhập tạm thời đã được gửi trực tiếp đến email của nhân viên. Quản trị viên không được xem hoặc chia sẻ mật khẩu tạm thời.
          </p>

          <div className="mt-6 rounded-2xl border border-surface-container-highest bg-surface p-5 text-left space-y-3">
            <div className="flex justify-between items-center text-sm">
              <span className="text-outline font-semibold">Email công việc</span>
              <span className="font-mono font-bold text-on-surface">{result.email}</span>
            </div>
            <div className="flex justify-between items-center text-sm border-t border-surface-container-highest pt-3">
              <span className="text-outline font-semibold">Vai trò được cấp</span>
              <span className="font-semibold text-primary">{result.role}</span>
            </div>
            <div className="flex justify-between items-center text-sm border-t border-surface-container-highest pt-3">
              <span className="text-outline font-semibold">Đổi mật khẩu khi đăng nhập</span>
              <span className="font-semibold text-emerald-700">{result.mustChangePassword ? 'Bắt buộc' : 'Không'}</span>
            </div>
          </div>

          <div className="mt-6 flex justify-center gap-3">
            <button
              type="button"
              onClick={() => navigate('/admin/users')}
              className="py-2.5 px-5 rounded-full border border-outline-variant bg-surface text-sm font-semibold text-on-surface-variant hover:bg-surface-container-low cursor-pointer"
            >
              Về danh sách
            </button>
            <button
              type="button"
              onClick={() => navigate(`/admin/users/${result.id}`)}
              className="py-2.5 px-5 rounded-full bg-primary text-on-primary text-sm font-semibold hover:bg-primary/90 cursor-pointer"
            >
              Xem tài khoản vừa tạo
            </button>
          </div>
        </section>
      </div>
    );
  }

  return (
    <div className="p-6 md:p-8 font-sans">
      <div className="mb-6 flex items-center gap-3">
        <button
          type="button"
          onClick={() => navigate('/admin/users')}
          className="inline-flex items-center gap-2 py-1.5 px-3 rounded-full text-xs font-semibold bg-surface border border-outline-variant text-on-surface-variant hover:bg-surface-container-low cursor-pointer"
        >
          <span className="material-symbols-outlined text-base">arrow_back</span>
          Quản lý người dùng
        </button>
      </div>

      <div className="mx-auto max-w-3xl">
        <h1 className="text-[26px] font-bold text-on-surface m-0">Tạo tài khoản nhân viên</h1>
        <p className="text-on-surface-variant text-sm mt-1">
          Hệ thống tự sinh thông tin đăng nhập và gửi qua email để đảm bảo bí mật thông tin xác thực.
        </p>

        <form onSubmit={handleSubmit} className="mt-6 space-y-6 rounded-2xl border border-surface-container-highest bg-surface p-6 md:p-8 shadow-md">
          {error && (
            <div className="rounded-2xl border border-error-container bg-error-container/60 p-4 text-sm text-error">
              {error}
            </div>
          )}

          <div className="grid gap-5 md:grid-cols-2">
            <label className="grid gap-2 text-sm font-bold text-on-surface">
              Họ và tên nhân viên
              <input
                required
                maxLength={120}
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                placeholder="Nguyễn Văn A"
                className="rounded-2xl border border-outline-variant bg-surface px-4 py-2.5 font-normal text-sm outline-none focus:border-primary"
              />
            </label>
            <label className="grid gap-2 text-sm font-bold text-on-surface">
              Email công việc
              <input
                required
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                placeholder="staff@carebridge.dev"
                className="rounded-2xl border border-outline-variant bg-surface px-4 py-2.5 font-normal text-sm outline-none focus:border-primary"
              />
            </label>
          </div>

          <label className="grid gap-2 text-sm font-bold text-on-surface">
            Số điện thoại Việt Nam (không bắt buộc)
            <input
              value={form.phone}
              onChange={(e) => setForm({ ...form, phone: e.target.value })}
              placeholder="0912345678"
              className="rounded-2xl border border-outline-variant bg-surface px-4 py-2.5 font-normal text-sm outline-none focus:border-primary"
            />
          </label>

          <fieldset className="border-none p-0 m-0">
            <legend className="mb-3 text-sm font-bold text-on-surface">Chọn vai trò nhân viên</legend>
            <div className="grid gap-3 md:grid-cols-3">
              {STAFF_ROLES.map((role) => {
                const isSelected = form.role === role.value;
                return (
                  <label
                    key={role.value}
                    className={`cursor-pointer rounded-2xl p-4 transition-all border ${
                      isSelected
                        ? 'border-primary bg-primary/5 shadow-sm'
                        : 'border-surface-container-highest bg-surface hover:bg-surface-bright'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-2">
                      <div className="flex items-center gap-2">
                        <span className={`material-symbols-outlined text-xl ${isSelected ? 'text-primary' : 'text-outline'}`}>
                          {role.icon}
                        </span>
                        <span className="font-bold text-sm text-on-surface">{role.label}</span>
                      </div>
                      <input
                        type="radio"
                        name="role"
                        value={role.value}
                        checked={isSelected}
                        onChange={() => setForm({ ...form, role: role.value })}
                        className="accent-primary"
                      />
                    </div>
                    <span className="block text-xs text-on-surface-variant leading-relaxed">{role.description}</span>
                  </label>
                );
              })}
            </div>
          </fieldset>

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-surface-container-highest">
            <button
              type="button"
              onClick={() => navigate('/admin/users')}
              className="py-2.5 px-5 rounded-full border border-outline-variant bg-surface text-sm font-semibold text-on-surface-variant hover:bg-surface-container-low cursor-pointer"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="py-2.5 px-6 rounded-full bg-primary text-on-primary text-sm font-semibold hover:bg-primary/90 disabled:opacity-50 cursor-pointer inline-flex items-center gap-2"
            >
              {isSubmitting ? (
                <>
                  <span className="material-symbols-outlined text-lg animate-spin">progress_activity</span>
                  Đang tạo...
                </>
              ) : (
                <>
                  <span className="material-symbols-outlined text-lg">send</span>
                  Tạo và gửi email
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
