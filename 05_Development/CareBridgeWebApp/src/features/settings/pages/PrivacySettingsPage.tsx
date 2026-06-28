import { useEffect, useState, useCallback } from 'react';
import apiClient from '../../../shared/api/apiClient';

interface PrivacySettings {
  id: string;
  userId: string;
  profileVisibility: string;
  locationSharingEnabled: boolean;
  analyticsConsent: boolean;
  dataExportOptOut: boolean;
  updatedAt: string;
}

interface ConsentGrant {
  id: number;
  userId: string;
  dataType: string;
  purpose: string;
  recipient: string;
  scope: string;
  consentGivenAt: string;
  expiryAt: string | null;
  revokedAt: string | null;
}

export default function PrivacySettingsPage() {
  const [settings, setSettings] = useState<PrivacySettings | null>(null);
  const [consents, setConsents] = useState<ConsentGrant[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [hasChanges, setHasChanges] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  const fetchData = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [settingsRes, consentsRes] = await Promise.all([
        apiClient.get('/api/v1/privacy-settings/me'),
        apiClient.get('/api/v1/consent/grants'),
      ]);
      setSettings(settingsRes.data.data);
      setConsents(
        (consentsRes.data.data as ConsentGrant[]).filter(
          (c) => c.revokedAt === null
        )
      );
    } catch {
      setError('Không thể tải cài đặt quyền riêng tư.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const updateField = (field: keyof PrivacySettings, value: boolean | string) => {
    if (!settings) return;
    setSettings({ ...settings, [field]: value });
    setHasChanges(true);
  };

  const handleSave = async () => {
    if (!settings) return;
    setIsSaving(true);
    try {
      const res = await apiClient.put('/api/v1/privacy-settings/me', {
        profileVisibility: settings.profileVisibility,
        locationSharingEnabled: settings.locationSharingEnabled,
        analyticsConsent: settings.analyticsConsent,
        dataExportOptOut: settings.dataExportOptOut,
      });
      setSettings(res.data.data);
      setHasChanges(false);
    } catch {
      setError('Lưu thất bại. Vui lòng thử lại.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleRevoke = async (consentId: number) => {
    try {
      await apiClient.delete(`/api/v1/consent/grants/${consentId}`);
      setConsents((prev) => prev.filter((c) => c.id !== consentId));
    } catch {
      setError('Không thể thu hồi quyền truy cập.');
    }
  };

  if (isLoading) {
    return (
      <div className="text-center p-12 text-outline">Đang tải...</div>
    );
  }

  if (error && !settings) {
    return (
      <div className="text-center p-12 text-error">{error}</div>
    );
  }

  return (
    <div className="relative pb-20">
      <h1 className="text-2xl font-bold text-on-surface m-0">Cài đặt Quyền riêng tư</h1>

      <div className="mt-6">
        <div className="bg-background rounded-2xl p-6 mb-2">
          <h2 className="text-lg font-bold text-on-surface mt-0 mb-2">
            Quản lý Dữ liệu Bệnh nhân &amp; Tuân thủ
          </h2>
          <p className="text-sm text-on-surface-variant mt-0 mb-4 leading-relaxed">
            Thiết lập các cấp độ bảo mật cho hệ thống CareBridge. Những cài đặt này áp dụng trên
            toàn bộ cổng thông tin tổ chức của bạn, đảm bảo tuân thủ các quy định về bảo vệ dữ
            liệu y tế (HIPAA/GDPR).
          </p>
          <button className="inline-flex items-center gap-2 px-5 py-2.5 rounded-full border border-outline-variant bg-surface text-primary text-sm font-semibold cursor-pointer">
            <span className="material-symbols-outlined" style={{ fontSize: 18 }}>security</span>
            Xem chính sách bảo mật
          </button>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-6 mt-6">
        {/* Left column */}
        <div className="flex flex-col gap-6">
          <SettingsCard title="Hồ sơ công khai" icon="public">
            <ToggleRow
              label="Hiển thị thông tin liên hệ"
              description="Cho phép đối tác trong mạng lưới thấy email và số điện thoại."
              value={settings?.profileVisibility === 'PUBLIC'}
              onChange={(v) => updateField('profileVisibility', v ? 'PUBLIC' : 'PRIVATE')}
            />
            <Divider />
            <ToggleRow
              label="Tìm kiếm trong danh bạ"
              description="Cho phép tìm thấy tài khoản này thông qua thanh tìm kiếm chung."
              value={settings?.analyticsConsent ?? false}
              onChange={(v) => updateField('analyticsConsent', v)}
            />
          </SettingsCard>

          <SettingsCard title="Thông báo" icon="notifications">
            <ToggleRow
              label="Ẩn chi tiết trên màn hình khóa"
              description="Không hiển thị tên bệnh nhân trong thông báo đẩy (Push)."
              value={settings?.locationSharingEnabled ?? false}
              onChange={(v) => updateField('locationSharingEnabled', v)}
            />
          </SettingsCard>

          <SettingsCard title="Bảo mật &amp; Phiên" icon="shield">
            <LinkRow
              icon="devices"
              label="Quản lý thiết bị"
              description="3 thiết bị đang đăng nhập"
            />
            <Divider />
            <LinkRow
              icon="password"
              label="Xác thực 2 yếu tố (2FA)"
              description="Đang bật (Yêu cầu)"
              descriptionClass="text-primary"
            />
          </SettingsCard>
        </div>

        {/* Right column */}
        <div>
          <SettingsCard title="Quản lý sự chấp thuận dữ liệu" icon="verified_user">
            <p className="text-sm text-on-surface-variant mt-0 mb-4">
              Kiểm soát các bên thứ 3 và phòng ban có quyền truy cập dữ liệu.
            </p>
            <button className="inline-flex items-center gap-1.5 px-5 py-2.5 rounded-full bg-primary text-on-primary border-none text-sm font-semibold cursor-pointer float-right mb-4">
              <span className="material-symbols-outlined" style={{ fontSize: 16 }}>add</span>
              Cấp quyền mới
            </button>
            <div className="clear-both" />

            <table className="w-full border-collapse text-[13px]">
              <thead>
                <tr className="border-b border-surface-container-highest text-left">
                  <th className="px-2 py-3 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">NGƯỜI NHẬN</th>
                  <th className="px-2 py-3 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">PHẠM VI</th>
                  <th className="px-2 py-3 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">MỤC ĐÍCH</th>
                  <th className="px-2 py-3 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">HẾT HẠN</th>
                  <th className="px-2 py-3 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">TRẠNG THÁI</th>
                </tr>
              </thead>
              <tbody>
                {consents.map((c) => (
                  <tr key={c.id} className="border-b border-surface-container-highest">
                    <td className="px-2 py-3.5 text-on-surface">
                      <div className="flex items-center gap-2">
                        <span className="bg-primary-container text-on-primary rounded-md px-2 py-1 text-[11px] font-bold">
                          {(c.recipient || '').substring(0, 3).toUpperCase()}
                        </span>
                        <span>{c.recipient}</span>
                      </div>
                    </td>
                    <td className="px-2 py-3.5 text-on-surface">{c.scope || c.dataType}</td>
                    <td className="px-2 py-3.5 text-on-surface">{c.purpose}</td>
                    <td className="px-2 py-3.5 text-on-surface">
                      {c.expiryAt
                        ? new Date(c.expiryAt).toLocaleDateString('vi-VN')
                        : '—'}
                    </td>
                    <td className="px-2 py-3.5">
                      <span
                        className={`inline-block px-3 py-1 rounded-full text-xs font-medium border ${
                          isExpired(c.expiryAt)
                            ? 'text-outline border-outline-variant bg-surface-container-low'
                            : 'text-primary border-primary-container bg-surface-container-highest'
                        }`}
                      >
                        {isExpired(c.expiryAt) ? 'Đã hết hạn' : 'Đang hiệu lực'}
                      </span>
                    </td>
                    <td className="px-2 py-3.5">
                      <button
                        onClick={() => handleRevoke(c.id)}
                        className="px-3 py-1 rounded-full text-xs font-medium border border-error text-error hover:bg-error-container transition-colors"
                      >
                        Thu hồi
                      </button>
                    </td>
                  </tr>
                ))}
                {consents.length === 0 && (
                  <tr>
                    <td colSpan={5} className="px-2 py-3.5 text-on-surface text-center text-outline">
                      Chưa có quyền truy cập nào.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </SettingsCard>
        </div>
      </div>

      {hasChanges && (
        <div className="fixed bottom-0 left-0 right-0 bg-background border-t border-outline-variant px-12 py-4 flex justify-center items-center gap-4 z-50 shadow-[0_-4px_20px_rgba(90,70,63,0.06)]">
          <span className="text-sm text-on-surface-variant">Bạn có thay đổi chưa lưu</span>
          <button
            onClick={() => {
              setHasChanges(false);
              fetchData();
            }}
            className="px-6 py-2.5 rounded-lg border border-outline-variant bg-surface text-on-surface text-sm font-semibold cursor-pointer"
          >
            Hủy
          </button>
          <button
            onClick={handleSave}
            disabled={isSaving}
            className="px-6 py-2.5 rounded-lg bg-primary-container text-on-primary border-none text-sm font-semibold cursor-pointer disabled:opacity-60"
          >
            {isSaving ? 'Đang lưu...' : 'Lưu thay đổi'}
          </button>
        </div>
      )}
    </div>
  );
}

function SettingsCard({
  title,
  icon,
  children,
}: {
  title: string;
  icon: string;
  children: React.ReactNode;
}) {
  return (
    <div className="bg-surface rounded-2xl p-6 shadow-sm">
      <div className="flex items-center gap-2 mb-5">
        <span className="material-symbols-outlined text-primary" style={{ fontSize: 20 }}>{icon}</span>
        <h3 className="text-lg font-semibold text-on-surface m-0">{title}</h3>
      </div>
      {children}
    </div>
  );
}

function ToggleRow({
  label,
  description,
  value,
  onChange,
}: {
  label: string;
  description: string;
  value: boolean;
  onChange: (v: boolean) => void;
}) {
  return (
    <div className="flex justify-between items-start py-3">
      <div className="flex-1 mr-4">
        <div className="text-sm font-semibold text-on-surface">{label}</div>
        <div className="text-[13px] text-on-surface-variant mt-1">{description}</div>
      </div>
      <label className="relative inline-block w-12 h-6 shrink-0 mt-0.5">
        <input
          type="checkbox"
          checked={value}
          onChange={(e) => onChange(e.target.checked)}
          className="opacity-0 w-0 h-0"
        />
        <span
          className={`absolute cursor-pointer inset-0 rounded-[24px] transition-colors duration-200 ${value ? 'bg-primary-container' : 'bg-outline-variant'}`}
        >
          <span
            className={`absolute h-5 w-5 bottom-0.5 bg-white rounded-full shadow-sm transition-all duration-200 ${value ? 'left-[26px]' : 'left-0.5'}`}
          />
        </span>
      </label>
    </div>
  );
}

function LinkRow({
  icon,
  label,
  description,
  descriptionClass = 'text-on-surface-variant',
}: {
  icon: string;
  label: string;
  description: string;
  descriptionClass?: string;
}) {
  return (
    <div className="flex items-center py-4 cursor-pointer">
      <span className="material-symbols-outlined text-on-surface-variant mr-3" style={{ fontSize: 20 }}>{icon}</span>
      <div className="flex-1">
        <div className="text-sm font-semibold text-on-surface">{label}</div>
        <div className={`text-[13px] mt-0.5 ${descriptionClass}`}>{description}</div>
      </div>
      <span className="material-symbols-outlined text-outline" style={{ fontSize: 18 }}>chevron_right</span>
    </div>
  );
}

function Divider() {
  return <hr className="border-0 border-t border-surface-container-highest m-0" />;
}

function isExpired(expiryAt: string | null): boolean {
  if (!expiryAt) return false;
  return new Date(expiryAt) < new Date();
}
