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
      <div className="portal-page px-5 py-5 md:px-6 md:py-6"><div className="portal-contained"><div className="portal-empty">Đang tải...</div></div></div>
    );
  }

  if (error && !settings) {
    return (
      <div className="portal-page px-5 py-5 md:px-6 md:py-6"><div className="portal-contained"><div className="portal-error">{error}</div></div></div>
    );
  }

  return (
    <div className="portal-page px-5 py-5 pb-20 md:px-6 md:py-6 md:pb-20">
      <div className="portal-contained">
      <div className="portal-header">
        <div>
          <p className="portal-eyebrow">Quyền riêng tư</p>
          <h1 className="portal-title">Cài đặt quyền riêng tư</h1>
          <p className="portal-subtitle">Quản lý dữ liệu bệnh nhân, đồng ý chia sẻ và các thiết lập tuân thủ.</p>
        </div>
      </div>

      <div className="portal-card-padded mb-5">
          <h2 className="m-0 mb-2 text-base font-semibold text-on-surface">
            Quản lý Dữ liệu Bệnh nhân &amp; Tuân thủ
          </h2>
          <p className="m-0 mb-4 text-sm leading-relaxed text-on-surface-variant">
            Thiết lập các cấp độ bảo mật cho hệ thống CareBridge. Những cài đặt này áp dụng trên
            toàn bộ cổng thông tin tổ chức của bạn, đảm bảo tuân thủ các quy định về bảo vệ dữ
            liệu y tế (HIPAA/GDPR).
          </p>
          <button className="portal-secondary-button">
            <span className="material-symbols-outlined text-lg">security</span>
            Xem chính sách bảo mật
          </button>
      </div>

      <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
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
            <button className="portal-primary-button float-right mb-4">
              <span className="material-symbols-outlined text-base">add</span>
              Cấp quyền mới
            </button>
            <div className="clear-both" />

            <div className="portal-table-card">
            <table className="w-full text-[13px]">
              <thead>
                <tr>
                  <th>NGƯỜI NHẬN</th>
                  <th>PHẠM VI</th>
                  <th>MỤC ĐÍCH</th>
                  <th>HẾT HẠN</th>
                  <th>TRẠNG THÁI</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {consents.map((c) => (
                  <tr key={c.id}>
                    <td>
                      <div className="flex items-center gap-2">
                        <span className="bg-primary-container text-on-primary rounded-md px-2 py-1 text-[11px] font-bold">
                          {(c.recipient || '').substring(0, 3).toUpperCase()}
                        </span>
                        <span>{c.recipient}</span>
                      </div>
                    </td>
                    <td>{c.scope || c.dataType}</td>
                    <td>{c.purpose}</td>
                    <td>
                      {c.expiryAt
                        ? new Date(c.expiryAt).toLocaleDateString('vi-VN')
                        : '—'}
                    </td>
                    <td>
                      <span
                        className={`inline-block rounded-md border px-2.5 py-1 text-xs font-medium ${
                          isExpired(c.expiryAt)
                            ? 'text-outline border-outline-variant bg-surface-container-low'
                            : 'text-primary border-primary-container bg-surface-container-highest'
                        }`}
                      >
                        {isExpired(c.expiryAt) ? 'Đã hết hạn' : 'Đang hiệu lực'}
                      </span>
                    </td>
                    <td>
                      <button
                        onClick={() => handleRevoke(c.id)}
                        className="rounded-md border border-error px-3 py-1 text-xs font-medium text-error transition-colors hover:bg-error-container"
                      >
                        Thu hồi
                      </button>
                    </td>
                  </tr>
                ))}
                {consents.length === 0 && (
                  <tr>
                    <td colSpan={6} className="text-center text-outline">
                      Chưa có quyền truy cập nào.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
            </div>
          </SettingsCard>
        </div>
      </div>

      {hasChanges && (
        <div className="fixed bottom-0 left-0 right-0 z-50 flex items-center justify-center gap-4 border-t border-outline-variant bg-background px-6 py-4 shadow-[0_-4px_20px_rgba(90,70,63,0.06)] md:left-64">
          <span className="text-sm text-on-surface-variant">Bạn có thay đổi chưa lưu</span>
          <button
            onClick={() => {
              setHasChanges(false);
              fetchData();
            }}
            className="portal-secondary-button"
          >
            Hủy
          </button>
          <button
            onClick={handleSave}
            disabled={isSaving}
            className="portal-primary-button disabled:opacity-60"
          >
            {isSaving ? 'Đang lưu...' : 'Lưu thay đổi'}
          </button>
        </div>
      )}
      </div>
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
    <div className="portal-card-padded">
      <div className="mb-5 flex items-center gap-2">
        <span className="material-symbols-outlined text-primary text-xl">{icon}</span>
        <h3 className="m-0 text-base font-semibold text-on-surface">{title}</h3>
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
          className={`absolute cursor-pointer inset-0 rounded-full transition-colors duration-200 ${value ? 'bg-primary-container' : 'bg-outline-variant'}`}
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
      <span className="material-symbols-outlined text-on-surface-variant mr-3 text-xl">{icon}</span>
      <div className="flex-1">
        <div className="text-sm font-semibold text-on-surface">{label}</div>
        <div className={`text-[13px] mt-0.5 ${descriptionClass}`}>{description}</div>
      </div>
      <span className="material-symbols-outlined text-outline text-lg">chevron_right</span>
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
