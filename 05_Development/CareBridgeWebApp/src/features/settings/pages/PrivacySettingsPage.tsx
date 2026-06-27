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
      <div style={{ textAlign: 'center', padding: 48, color: '#84736f', fontFamily: "'Lexend'" }}>
        Đang tải...
      </div>
    );
  }

  if (error && !settings) {
    return (
      <div style={{ textAlign: 'center', padding: 48, color: '#ba1a1a', fontFamily: "'Lexend'" }}>
        {error}
      </div>
    );
  }

  return (
    <div style={{ fontFamily: "'Lexend', sans-serif", position: 'relative', paddingBottom: 80 }}>
      <h1 style={{ fontSize: 24, fontWeight: 700, color: '#271812', margin: 0 }}>
        Cài đặt Quyền riêng tư
      </h1>

      <div style={{ marginTop: 24 }}>
        <div style={{ background: '#fff8f6', borderRadius: 16, padding: 24, marginBottom: 8 }}>
          <h2 style={{ fontSize: 18, fontWeight: 700, color: '#271812', margin: '0 0 8px' }}>
            Quản lý Dữ liệu Bệnh nhân & Tuân thủ
          </h2>
          <p style={{ fontSize: 14, color: '#524440', margin: '0 0 16px', lineHeight: 1.6 }}>
            Thiết lập các cấp độ bảo mật cho hệ thống CareBridge. Những cài đặt này áp dụng trên
            toàn bộ cổng thông tin tổ chức của bạn, đảm bảo tuân thủ các quy định về bảo vệ dữ
            liệu y tế (HIPAA/GDPR).
          </p>
          <button style={outlineBtn}>
            <span className="material-symbols-outlined" style={{ fontSize: 18 }}>security</span>
            Xem chính sách bảo mật
          </button>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24, marginTop: 24 }}>
        {/* Left column */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
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

          <SettingsCard title="Bảo mật & Phiên" icon="shield">
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
              descriptionColor="#845143"
            />
          </SettingsCard>
        </div>

        {/* Right column */}
        <div>
          <SettingsCard title="Quản lý sự chấp thuận dữ liệu" icon="verified_user">
            <p style={{ fontSize: 14, color: '#524440', margin: '0 0 16px' }}>
              Kiểm soát các bên thứ 3 và phòng ban có quyền truy cập dữ liệu.
            </p>
            <button
              style={{
                ...primaryBtn,
                marginBottom: 16,
                float: 'right',
              }}
            >
              <span className="material-symbols-outlined" style={{ fontSize: 16 }}>add</span>
              Cấp quyền mới
            </button>
            <div style={{ clear: 'both' }} />

            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
              <thead>
                <tr style={{ borderBottom: '1px solid #fadcd3', textAlign: 'left' }}>
                  <th style={thStyle2}>NGƯỜI NHẬN</th>
                  <th style={thStyle2}>PHẠM VI</th>
                  <th style={thStyle2}>MỤC ĐÍCH</th>
                  <th style={thStyle2}>HẾT HẠN</th>
                  <th style={thStyle2}>TRẠNG THÁI</th>
                </tr>
              </thead>
              <tbody>
                {consents.map((c) => (
                  <tr key={c.id} style={{ borderBottom: '1px solid #fadcd3' }}>
                    <td style={tdStyle}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <span
                          style={{
                            background: '#c98c7b',
                            color: '#fff',
                            borderRadius: 6,
                            padding: '4px 8px',
                            fontSize: 11,
                            fontWeight: 700,
                          }}
                        >
                          {(c.recipient || '').substring(0, 3).toUpperCase()}
                        </span>
                        <span>{c.recipient}</span>
                      </div>
                    </td>
                    <td style={tdStyle}>{c.scope || c.dataType}</td>
                    <td style={tdStyle}>{c.purpose}</td>
                    <td style={tdStyle}>
                      {c.expiryAt
                        ? new Date(c.expiryAt).toLocaleDateString('vi-VN')
                        : '—'}
                    </td>
                    <td style={tdStyle}>
                      <span
                        style={{
                          display: 'inline-block',
                          padding: '4px 12px',
                          borderRadius: 9999,
                          fontSize: 12,
                          fontWeight: 500,
                          border: '1px solid',
                          ...(isExpired(c.expiryAt)
                            ? { color: '#84736f', borderColor: '#d6c2bd', background: '#f6f1ec' }
                            : { color: '#845143', borderColor: '#c98c7b40', background: '#ffdbd1' }),
                        }}
                      >
                        {isExpired(c.expiryAt) ? 'Đã hết hạn' : 'Đang hiệu lực'}
                      </span>
                    </td>
                  </tr>
                ))}
                {consents.length === 0 && (
                  <tr>
                    <td colSpan={5} style={{ ...tdStyle, textAlign: 'center', color: '#84736f' }}>
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
        <div
          style={{
            position: 'fixed',
            bottom: 0,
            left: 0,
            right: 0,
            background: '#fff8f6',
            borderTop: '1px solid #d6c2bd',
            padding: '16px 48px',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            gap: 16,
            zIndex: 50,
            boxShadow: '0 -4px 20px rgba(90,70,63,0.06)',
          }}
        >
          <span style={{ fontSize: 14, color: '#524440' }}>Bạn có thay đổi chưa lưu</span>
          <button
            onClick={() => {
              setHasChanges(false);
              fetchData();
            }}
            style={{
              padding: '10px 24px',
              borderRadius: 8,
              border: '1px solid #d6c2bd',
              background: '#fff',
              color: '#271812',
              fontSize: 14,
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            Hủy
          </button>
          <button onClick={handleSave} disabled={isSaving} style={saveBtn}>
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
    <div
      style={{
        background: '#fff',
        borderRadius: 16,
        padding: 24,
        boxShadow: '0 4px 20px rgba(90,70,63,0.06)',
      }}
    >
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          marginBottom: 20,
        }}
      >
        <span
          className="material-symbols-outlined"
          style={{ fontSize: 20, color: '#845143' }}
        >
          {icon}
        </span>
        <h3 style={{ fontSize: 18, fontWeight: 600, color: '#271812', margin: 0 }}>{title}</h3>
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
    <div
      style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        padding: '12px 0',
      }}
    >
      <div style={{ flex: 1, marginRight: 16 }}>
        <div style={{ fontSize: 14, fontWeight: 600, color: '#271812' }}>{label}</div>
        <div style={{ fontSize: 13, color: '#524440', marginTop: 4 }}>{description}</div>
      </div>
      <label style={{ position: 'relative', display: 'inline-block', width: 48, height: 24, flexShrink: 0, marginTop: 2 }}>
        <input
          type="checkbox"
          checked={value}
          onChange={(e) => onChange(e.target.checked)}
          style={{ opacity: 0, width: 0, height: 0 }}
        />
        <span
          style={{
            position: 'absolute',
            cursor: 'pointer',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: value ? '#c98c7b' : '#d6c2bd',
            borderRadius: 24,
            transition: 'background 0.2s',
          }}
        >
          <span
            style={{
              position: 'absolute',
              height: 20,
              width: 20,
              left: value ? 26 : 2,
              bottom: 2,
              background: '#fff',
              borderRadius: '50%',
              transition: 'left 0.2s',
              boxShadow: '0 1px 3px rgba(0,0,0,0.15)',
            }}
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
  descriptionColor,
}: {
  icon: string;
  label: string;
  description: string;
  descriptionColor?: string;
}) {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        padding: '16px 0',
        cursor: 'pointer',
      }}
    >
      <span
        className="material-symbols-outlined"
        style={{ fontSize: 20, color: '#524440', marginRight: 12 }}
      >
        {icon}
      </span>
      <div style={{ flex: 1 }}>
        <div style={{ fontSize: 14, fontWeight: 600, color: '#271812' }}>{label}</div>
        <div style={{ fontSize: 13, color: descriptionColor || '#524440', marginTop: 2 }}>
          {description}
        </div>
      </div>
      <span className="material-symbols-outlined" style={{ fontSize: 18, color: '#84736f' }}>
        chevron_right
      </span>
    </div>
  );
}

function Divider() {
  return <hr style={{ border: 'none', borderTop: '1px solid #fadcd3', margin: 0 }} />;
}

function isExpired(expiryAt: string | null): boolean {
  if (!expiryAt) return false;
  return new Date(expiryAt) < new Date();
}

const outlineBtn: React.CSSProperties = {
  display: 'inline-flex',
  alignItems: 'center',
  gap: 8,
  padding: '10px 20px',
  borderRadius: 9999,
  border: '1px solid #d6c2bd',
  background: '#fff',
  color: '#845143',
  fontSize: 14,
  fontWeight: 600,
  cursor: 'pointer',
};

const primaryBtn: React.CSSProperties = {
  display: 'inline-flex',
  alignItems: 'center',
  gap: 6,
  padding: '10px 20px',
  borderRadius: 9999,
  border: 'none',
  background: '#845143',
  color: '#fff',
  fontSize: 14,
  fontWeight: 600,
  cursor: 'pointer',
};

const saveBtn: React.CSSProperties = {
  padding: '10px 24px',
  borderRadius: 8,
  border: 'none',
  background: '#c98c7b',
  color: '#fff',
  fontSize: 14,
  fontWeight: 600,
  cursor: 'pointer',
};

const thStyle2: React.CSSProperties = {
  padding: '12px 8px',
  fontSize: 11,
  fontWeight: 600,
  color: '#84736f',
  textTransform: 'uppercase',
  letterSpacing: '0.05em',
};

const tdStyle: React.CSSProperties = {
  padding: '14px 8px',
  fontSize: 13,
  color: '#271812',
};
