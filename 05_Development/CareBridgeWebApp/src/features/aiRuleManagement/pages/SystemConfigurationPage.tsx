import { useEffect, useState } from 'react';
import ModPortalSidebar from '../../moderation/components/ModPortalSidebar';
import {
  fetchSystemConfiguration,
  saveSystemConfiguration,
  type SystemConfiguration,
} from '../services/systemConfigurationApi';

const inputClass = 'portal-field mt-2 w-full';

function Toggle({ checked, disabled, label, description, onChange }: { checked: boolean; disabled?: boolean; label: string; description: string; onChange: (value: boolean) => void }) {
  return <div className="flex items-center justify-between gap-4 rounded-md border border-outline-variant bg-surface-container-lowest p-4">
    <div><h3 className="m-0 text-sm font-semibold text-on-surface">{label}</h3><p className="mt-1 text-sm text-on-surface-variant">{description}</p>{label.includes('bảo trì') && <p className="mt-2 text-xs font-semibold text-error">Rủi ro cao</p>}</div>
    <button type="button" role="switch" aria-checked={checked} aria-label={label} disabled={disabled} onClick={() => onChange(!checked)} className={`relative h-7 w-12 rounded-full transition ${checked ? 'bg-primary' : 'bg-outline-variant'} disabled:cursor-not-allowed disabled:opacity-50`}><span className={`absolute top-1 h-5 w-5 rounded-full bg-surface shadow transition ${checked ? 'left-6' : 'left-1'}`} /></button>
  </div>;
}

export default function SystemConfigurationPage() {
  const [configuration, setConfiguration] = useState<SystemConfiguration | null>(null);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => { fetchSystemConfiguration().then(setConfiguration).catch(() => setError('Không thể tải cấu hình hệ thống.')); }, []);
  if (!configuration) return <div className="portal-page"><ModPortalSidebar /><main className="portal-content grid place-items-center">{error ? <p className="portal-error">{error}</p> : <span className="h-10 w-10 animate-spin rounded-full border-4 border-primary border-t-transparent" />}</main></div>;

  const update = <K extends keyof SystemConfiguration>(key: K, value: SystemConfiguration[K]) => setConfiguration((current) => current ? { ...current, [key]: value } : current);
  const submit = async () => { setSaving(true); setSaved(false); setError(''); try { const savedConfiguration = await saveSystemConfiguration(configuration); setConfiguration(savedConfiguration); setSaved(true); } catch { setError('Không thể lưu cấu hình hệ thống. Vui lòng thử lại.'); } finally { setSaving(false); } };
  const reset = async () => { try { setConfiguration(await fetchSystemConfiguration()); setSaved(false); setError(''); } catch { setError('Không thể tải lại cấu hình hệ thống.'); } };
  const updateMaintenanceMode = (value: boolean) => { if (value && !window.confirm('Bật chế độ bảo trì sẽ chặn truy cập người dùng cuối. Bạn có chắc chắn muốn tiếp tục?')) return; update('maintenanceModeEnabled', value); };

  return (
    <div className="portal-page">
      <ModPortalSidebar />
      <main className="portal-content pb-28">
        <div className="portal-contained">
          <div className="portal-header">
            <div>
              <p className="portal-eyebrow">System Admin</p>
              <h1 className="portal-title">Cấu hình hệ thống</h1>
              <p className="portal-subtitle">
                Quản lý giới hạn hệ thống, tính năng và thông báo mặc định.
                <span className="font-semibold text-error"> Thay đổi có thể ảnh hưởng đến toàn bộ hệ thống.</span>
              </p>
            </div>
          </div>
          {error && <p className="portal-error mb-4">{error}</p>}

          <div className="grid gap-5 lg:grid-cols-3">
            <div className="space-y-5 lg:col-span-2">
              <section className="portal-card-padded">
                <h2 className="m-0 flex items-center gap-2 text-base font-semibold text-on-surface">
                  <span className="material-symbols-outlined portal-icon">speed</span>
                  Giới hạn hệ thống
                </h2>
                <div className="mt-5 grid gap-4 md:grid-cols-2">
                  <label className="portal-label">Tải trọng tối đa API (req/s)<input type="number" min="1" value={configuration.apiRateLimit} onChange={(event) => update('apiRateLimit', Number(event.target.value))} className={inputClass} /></label>
                  <label className="portal-label">Thời gian chờ kết nối (ms)<input type="number" min="1" value={configuration.connectionTimeoutMs} onChange={(event) => update('connectionTimeoutMs', Number(event.target.value))} className={inputClass} /></label>
                  <label className="portal-label md:col-span-2">Giới hạn kích thước tệp tải lên (MB)<input type="number" min="1" value={configuration.maxUploadSizeMb} onChange={(event) => update('maxUploadSizeMb', Number(event.target.value))} className={inputClass} /></label>
                </div>
              </section>

              <section className="portal-card-padded">
                <h2 className="m-0 flex items-center gap-2 text-base font-semibold text-on-surface">
                  <span className="material-symbols-outlined portal-icon">toggle_on</span>
                  Cờ tính năng
                </h2>
                <div className="mt-5 space-y-3">
                  <Toggle label="Kiểm duyệt tự động AI" description="Bật/tắt hệ thống phân loại AI bước 1." checked={configuration.aiModerationEnabled} onChange={(value) => update('aiModerationEnabled', value)} />
                  <Toggle label="Chế độ bảo trì hệ thống" description="Chặn truy cập người dùng cuối, hiển thị trang bảo trì." checked={configuration.maintenanceModeEnabled} onChange={updateMaintenanceMode} />
                </div>
              </section>
            </div>

            <aside className="space-y-5">
              <section className="portal-card-padded">
                <h2 className="m-0 flex items-center gap-2 text-base font-semibold text-on-surface">
                  <span className="material-symbols-outlined portal-icon">notifications</span>
                  Thông báo mặc định
                </h2>
                <label className="portal-label mt-5 block">Email quản trị viên chính<input type="email" value={configuration.administratorEmail} onChange={(event) => update('administratorEmail', event.target.value)} className={inputClass} /></label>
                <div className="mt-5 space-y-3 text-sm">
                  <label className="flex items-center gap-3"><input checked={configuration.emailAlerts} onChange={(event) => update('emailAlerts', event.target.checked)} type="checkbox" className="h-4 w-4 accent-primary" />Email</label>
                  <label className="flex items-center gap-3"><input checked={configuration.smsAlerts} onChange={(event) => update('smsAlerts', event.target.checked)} type="checkbox" className="h-4 w-4 accent-primary" />SMS (Việt Nam)</label>
                  <label className="flex items-center gap-3"><input checked={configuration.webhookAlerts} onChange={(event) => update('webhookAlerts', event.target.checked)} type="checkbox" className="h-4 w-4 accent-primary" />Slack / Teams Webhook</label>
                </div>
              </section>

              <section className="portal-card-padded border-primary/25 bg-primary/95 text-on-primary">
                <h2 className="m-0 text-base font-semibold">Trạng thái cấu hình</h2>
                <p className="mt-3 text-sm text-primary-fixed">Cấu hình được lưu và đồng bộ qua API hệ thống.</p>
                <p className="mt-5 flex items-center gap-2 text-sm"><span className="material-symbols-outlined text-base">verified</span>Chỉ System Admin được phép thay đổi</p>
              </section>
            </aside>
          </div>
        </div>

        <footer className="fixed bottom-0 left-0 right-0 z-20 flex items-center justify-end gap-3 border-t border-outline-variant bg-surface/95 px-5 py-3 md:left-64 md:px-6">
          <span className="mr-auto text-sm text-primary">{saved ? 'Đã lưu cấu hình hệ thống.' : ''}</span>
          <button onClick={reset} className="portal-secondary-button">Hủy</button>
          <button onClick={submit} disabled={saving} className="portal-primary-button disabled:opacity-50"><span className="material-symbols-outlined mr-2 align-middle text-base">save</span>{saving ? 'Đang lưu...' : 'Lưu toàn bộ cấu hình'}</button>
        </footer>
      </main>
    </div>
  );
}
