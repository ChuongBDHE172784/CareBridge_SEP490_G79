import { useEffect, useState } from 'react';
import ModPortalSidebar from '../../moderation/components/ModPortalSidebar';
import {
  fetchSystemConfiguration,
  saveSystemConfiguration,
  type SystemConfiguration,
} from '../services/systemConfigurationApi';

const inputClass = 'w-full mt-1.5 py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none font-sans';

function Toggle({ checked, disabled, label, description, onChange }: { checked: boolean; disabled?: boolean; label: string; description: string; onChange: (value: boolean) => void }) {
  return (
    <div className="flex items-center justify-between gap-4 rounded-2xl border border-surface-container-highest bg-surface-bright p-4 transition-colors">
      <div>
        <h3 className="m-0 text-sm font-bold text-on-surface">{label}</h3>
        <p className="mt-1 text-xs text-on-surface-variant m-0">{description}</p>
        {label.includes('bảo trì') && <p className="mt-1.5 text-xs font-bold text-error m-0">⚠️ Rủi ro cao — Chặn truy cập người dùng cuối</p>}
      </div>
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        aria-label={label}
        disabled={disabled}
        onClick={() => onChange(!checked)}
        className={`relative h-7 w-12 shrink-0 cursor-pointer rounded-full transition-colors ${checked ? 'bg-primary' : 'bg-outline-variant'} disabled:cursor-not-allowed disabled:opacity-50`}
      >
        <span className={`absolute top-1 h-5 w-5 rounded-full bg-surface shadow-md transition-all ${checked ? 'left-6' : 'left-1'}`} />
      </button>
    </div>
  );
}

export default function SystemConfigurationPage() {
  const [configuration, setConfiguration] = useState<SystemConfiguration | null>(null);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    fetchSystemConfiguration().then(setConfiguration).catch(() => setError('Không thể tải cấu hình hệ thống.'));
  }, []);

  if (!configuration) {
    return (
      <div className="portal-page font-sans">
        <ModPortalSidebar />
        <main className="portal-content grid place-items-center min-h-screen">
          {error ? (
            <p className="rounded-2xl border border-error-container bg-error-container/60 p-4 text-sm text-error">{error}</p>
          ) : (
            <span className="h-10 w-10 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          )}
        </main>
      </div>
    );
  }

  const update = <K extends keyof SystemConfiguration>(key: K, value: SystemConfiguration[K]) =>
    setConfiguration((current) => (current ? { ...current, [key]: value } : current));

  const submit = async () => {
    setSaving(true);
    setSaved(false);
    setError('');
    try {
      const savedConfiguration = await saveSystemConfiguration(configuration);
      setConfiguration(savedConfiguration);
      setSaved(true);
    } catch {
      setError('Không thể lưu cấu hình hệ thống. Vui lòng thử lại.');
    } finally {
      setSaving(false);
    }
  };

  const reset = async () => {
    try {
      setConfiguration(await fetchSystemConfiguration());
      setSaved(false);
      setError('');
    } catch {
      setError('Không thể tải lại cấu hình hệ thống.');
    }
  };

  const updateMaintenanceMode = (value: boolean) => {
    if (value && !window.confirm('Bật chế độ bảo trì sẽ chặn truy cập người dùng cuối. Bạn có chắc chắn muốn tiếp tục?')) return;
    update('maintenanceModeEnabled', value);
  };

  return (
    <div className="portal-page font-sans">
      <ModPortalSidebar />
      <main className="portal-content pb-28">
        <div className="p-8">
          {/* Header */}
          <div className="mb-6 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            <div>
              <h1 className="text-[26px] font-bold text-on-surface m-0">Cấu hình hệ thống</h1>
              <p className="text-on-surface-variant text-sm mt-1">
                Quản lý giới hạn hệ thống, tính năng và thông báo mặc định.
                <span className="font-semibold text-error"> Thay đổi có thể ảnh hưởng đến toàn bộ hệ thống.</span>
              </p>
            </div>
            <div className="flex items-center gap-2 self-start md:self-auto">
              <button
                type="button"
                onClick={() => void reset()}
                className="inline-flex items-center gap-2 py-2.5 px-5 rounded-full bg-surface border border-outline-variant text-on-surface-variant text-sm font-semibold cursor-pointer hover:bg-surface-container-low"
              >
                <span className="material-symbols-outlined text-lg">refresh</span>
                Làm mới
              </button>
            </div>
          </div>

          {error && (
            <div className="mb-4 rounded-2xl border border-error-container bg-error-container/60 p-4 text-sm text-error">
              {error}
            </div>
          )}

          <div className="grid gap-6 lg:grid-cols-3">
            <div className="space-y-6 lg:col-span-2">
              {/* Giới hạn hệ thống Card */}
              <section className="bg-surface rounded-2xl p-6 shadow-md border border-surface-container-highest">
                <div className="flex items-center gap-2 mb-4 pb-3 border-b border-surface-container-highest">
                  <span className="material-symbols-outlined text-primary text-xl">speed</span>
                  <h2 className="text-base font-bold text-on-surface m-0">Giới hạn hệ thống</h2>
                </div>
                <div className="grid gap-4 md:grid-cols-2">
                  <div>
                    <label className="block text-xs font-bold text-on-surface uppercase tracking-wider mb-1">Tải trọng tối đa API (req/s)</label>
                    <input
                      type="number"
                      min="1"
                      value={configuration.apiRateLimit}
                      onChange={(event) => update('apiRateLimit', Number(event.target.value))}
                      className={inputClass}
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-on-surface uppercase tracking-wider mb-1">Thời gian chờ kết nối (ms)</label>
                    <input
                      type="number"
                      min="1"
                      value={configuration.connectionTimeoutMs}
                      onChange={(event) => update('connectionTimeoutMs', Number(event.target.value))}
                      className={inputClass}
                    />
                  </div>
                  <div className="md:col-span-2">
                    <label className="block text-xs font-bold text-on-surface uppercase tracking-wider mb-1">Giới hạn kích thước tệp tải lên (MB)</label>
                    <input
                      type="number"
                      min="1"
                      value={configuration.maxUploadSizeMb}
                      onChange={(event) => update('maxUploadSizeMb', Number(event.target.value))}
                      className={inputClass}
                    />
                  </div>
                </div>
              </section>

              {/* Cờ tính năng Card */}
              <section className="bg-surface rounded-2xl p-6 shadow-md border border-surface-container-highest">
                <div className="flex items-center gap-2 mb-4 pb-3 border-b border-surface-container-highest">
                  <span className="material-symbols-outlined text-primary text-xl">toggle_on</span>
                  <h2 className="text-base font-bold text-on-surface m-0">Cờ tính năng (Feature Flags)</h2>
                </div>
                <div className="space-y-3">
                  <Toggle
                    label="Kiểm duyệt tự động AI"
                    description="Bật/tắt hệ thống phân loại AI bước 1 cho bài viết và câu hỏi."
                    checked={configuration.aiModerationEnabled}
                    onChange={(value) => update('aiModerationEnabled', value)}
                  />
                  <Toggle
                    label="Chế độ bảo trì hệ thống"
                    description="Chặn truy cập người dùng cuối và hiển thị màn hình bảo trì."
                    checked={configuration.maintenanceModeEnabled}
                    onChange={updateMaintenanceMode}
                  />
                </div>
              </section>
            </div>

            <aside className="space-y-6">
              {/* Thông báo mặc định Card */}
              <section className="bg-surface rounded-2xl p-6 shadow-md border border-surface-container-highest">
                <div className="flex items-center gap-2 mb-4 pb-3 border-b border-surface-container-highest">
                  <span className="material-symbols-outlined text-primary text-xl">notifications</span>
                  <h2 className="text-base font-bold text-on-surface m-0">Thông báo mặc định</h2>
                </div>

                <div className="mb-4">
                  <label className="block text-xs font-bold text-on-surface uppercase tracking-wider mb-1">Email quản trị viên chính</label>
                  <input
                    type="email"
                    value={configuration.administratorEmail}
                    onChange={(event) => update('administratorEmail', event.target.value)}
                    className={inputClass}
                  />
                </div>

                <div className="space-y-3 pt-2">
                  <label className="flex items-center gap-3 cursor-pointer p-2 rounded-xl hover:bg-surface-container-low transition-colors">
                    <input
                      checked={configuration.emailAlerts}
                      onChange={(event) => update('emailAlerts', event.target.checked)}
                      type="checkbox"
                      className="h-4 w-4 accent-primary cursor-pointer rounded"
                    />
                    <span className="text-sm font-semibold text-on-surface">Email Cảnh báo</span>
                  </label>
                  <label className="flex items-center gap-3 cursor-pointer p-2 rounded-xl hover:bg-surface-container-low transition-colors">
                    <input
                      checked={configuration.smsAlerts}
                      onChange={(event) => update('smsAlerts', event.target.checked)}
                      type="checkbox"
                      className="h-4 w-4 accent-primary cursor-pointer rounded"
                    />
                    <span className="text-sm font-semibold text-on-surface">SMS (Việt Nam)</span>
                  </label>
                  <label className="flex items-center gap-3 cursor-pointer p-2 rounded-xl hover:bg-surface-container-low transition-colors">
                    <input
                      checked={configuration.webhookAlerts}
                      onChange={(event) => update('webhookAlerts', event.target.checked)}
                      type="checkbox"
                      className="h-4 w-4 accent-primary cursor-pointer rounded"
                    />
                    <span className="text-sm font-semibold text-on-surface">Slack / Teams Webhook</span>
                  </label>
                </div>
              </section>

              {/* Status Info Card */}
              <section className="bg-primary rounded-2xl p-6 shadow-md text-on-primary">
                <div className="flex items-center gap-2 mb-2">
                  <span className="material-symbols-outlined text-xl">admin_panel_settings</span>
                  <h2 className="text-base font-bold m-0">Quyền truy cập</h2>
                </div>
                <p className="text-xs text-primary-fixed leading-relaxed m-0 mt-2">
                  Cấu hình được lưu và đồng bộ tức thì trên toàn hệ thống API backend.
                </p>
                <div className="mt-4 flex items-center gap-2 text-xs font-semibold bg-white/10 p-2.5 rounded-xl backdrop-blur-sm">
                  <span className="material-symbols-outlined text-base">verified</span>
                  Chỉ System Admin có quyền thay đổi
                </div>
              </section>
            </aside>
          </div>
        </div>

        {/* Floating Footer */}
        <footer className="fixed bottom-0 left-0 right-0 z-20 flex items-center justify-between gap-3 border-t border-surface-container-highest bg-surface/95 px-6 py-4 backdrop-blur-md md:left-64 shadow-lg">
          <span className="text-sm font-semibold text-emerald-600 flex items-center gap-1">
            {saved && (
              <>
                <span className="material-symbols-outlined text-base">check_circle</span>
                Đã lưu cấu hình hệ thống thành công.
              </>
            )}
          </span>
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={reset}
              className="py-2.5 px-5 rounded-full border border-outline-variant bg-surface text-xs font-semibold text-on-surface-variant cursor-pointer hover:bg-surface-container-low"
            >
              Hủy thay đổi
            </button>
            <button
              type="button"
              onClick={submit}
              disabled={saving}
              className="py-2.5 px-6 rounded-full bg-primary text-on-primary text-xs font-semibold cursor-pointer hover:bg-primary/90 disabled:opacity-50 inline-flex items-center gap-2 shadow-sm"
            >
              <span className="material-symbols-outlined text-base">save</span>
              {saving ? 'Đang lưu...' : 'Lưu toàn bộ cấu hình'}
            </button>
          </div>
        </footer>
      </main>
    </div>
  );
}
