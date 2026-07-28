import axios from 'axios';
import { useCallback, useEffect, useId, useMemo, useRef, useState } from 'react';
import {
  Activity,
  ArrowRight,
  Bot,
  CircleCheck,
  Info,
  ShieldCheck,
  TriangleAlert,
  Wrench,
} from 'lucide-react';
import ModPortalSidebar from '../../moderation/components/ModPortalSidebar';
import {
  fetchSystemConfiguration,
  saveSystemConfiguration,
  type SystemConfiguration,
} from '../services/systemConfigurationApi';

const EDITABLE_KEYS: Array<keyof SystemConfiguration> = [
  'aiModerationEnabled',
  'maintenanceModeEnabled',
];

function editableSnapshot(configuration: SystemConfiguration | null) {
  if (!configuration) return '';
  return JSON.stringify(
    EDITABLE_KEYS.reduce<Record<string, unknown>>((snapshot, key) => {
      snapshot[key] = configuration[key];
      return snapshot;
    }, {}),
  );
}

function errorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError(error)) {
    if (error.response?.status === 409) {
      return 'Cấu hình đã được quản trị viên khác cập nhật. Hãy tải lại dữ liệu trước khi lưu.';
    }
    const message = error.response?.data?.message;
    if (typeof message === 'string' && message.trim()) return message;
  }
  return fallback;
}

function ConfigurationGuide({ title, children, highRisk = false }: {
  title: string;
  children: React.ReactNode;
  highRisk?: boolean;
}) {
  const guideId = useId();
  const [hovered, setHovered] = useState(false);
  const [focused, setFocused] = useState(false);
  const [pinned, setPinned] = useState(false);
  const [dismissed, setDismissed] = useState(false);
  const open = pinned || (!dismissed && (hovered || focused));

  return (
    <span
      className="relative inline-flex"
      onMouseEnter={() => {
        setHovered(true);
        setDismissed(false);
      }}
      onMouseLeave={() => setHovered(false)}
      onKeyDown={(event) => {
        if (event.key === 'Escape') {
          setPinned(false);
          setDismissed(true);
        }
      }}
    >
      <button
        type="button"
        aria-label={`Giải thích: ${title}`}
        aria-describedby={guideId}
        aria-expanded={open}
        onClick={() => {
          setPinned(!pinned);
          setDismissed(pinned);
        }}
        onFocus={() => {
          setFocused(true);
          setDismissed(false);
        }}
        onBlur={() => {
          setFocused(false);
          setPinned(false);
          setDismissed(false);
        }}
        className={`grid h-7 w-7 place-items-center rounded-full border transition-colors focus:outline-none focus:ring-2 focus:ring-primary/30 ${highRisk ? 'border-error/40 bg-error-container/50 text-error' : 'border-primary/30 bg-primary/10 text-primary'}`}
      >
        {highRisk ? <TriangleAlert aria-hidden="true" size={15} /> : <Info aria-hidden="true" size={15} />}
      </button>
      <span
        id={guideId}
        role="tooltip"
        className={`${open ? 'visible opacity-100' : 'invisible opacity-0'} absolute right-0 top-9 z-30 w-72 rounded-xl border border-outline-variant bg-surface-container-lowest p-3 text-left text-xs font-normal leading-relaxed text-on-surface shadow-xl transition-opacity md:left-0 md:right-auto`}
      >
        <strong className="mb-1 block text-sm text-on-surface">{title}</strong>
        {children}
      </span>
    </span>
  );
}

function Toggle({ checked, disabled, label, summary, guide, highRisk, onChange }: {
  checked: boolean;
  disabled?: boolean;
  label: string;
  summary: string;
  guide: React.ReactNode;
  highRisk?: boolean;
  onChange: (value: boolean) => void;
}) {
  return (
    <article className="flex items-center justify-between gap-4 rounded-2xl border border-surface-container-highest bg-surface-bright p-4 transition-colors">
      <div className="min-w-0">
        <div className="flex items-center gap-2">
          <h3 className="m-0 text-sm font-bold text-on-surface">{label}</h3>
          <ConfigurationGuide title={label} highRisk={highRisk}>{guide}</ConfigurationGuide>
        </div>
        <p className="m-0 mt-1 text-xs leading-relaxed text-on-surface-variant">{summary}</p>
        {highRisk && <p className="m-0 mt-1.5 text-xs font-bold text-error">Rủi ro cao — người dùng thông thường sẽ nhận HTTP 503.</p>}
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
    </article>
  );
}

export default function SystemConfigurationPage() {
  const [configuration, setConfiguration] = useState<SystemConfiguration | null>(null);
  const [baseline, setBaseline] = useState<SystemConfiguration | null>(null);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const loadSequence = useRef(0);

  const dirty = useMemo(
    () => editableSnapshot(configuration) !== editableSnapshot(baseline),
    [baseline, configuration],
  );

  const load = useCallback(async (showRefreshState = false) => {
    const requestId = ++loadSequence.current;
    if (showRefreshState) setRefreshing(true);
    setError('');
    setNotice('');
    try {
      const current = await fetchSystemConfiguration();
      if (requestId !== loadSequence.current) return;
      setConfiguration(current);
      setBaseline(current);
    } catch (loadError) {
      if (requestId !== loadSequence.current) return;
      setError(errorMessage(loadError, 'Không thể tải cấu hình hệ thống.'));
    } finally {
      if (showRefreshState && requestId === loadSequence.current) setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const update = <K extends keyof SystemConfiguration>(key: K, value: SystemConfiguration[K]) => {
    setConfiguration((current) => (current ? { ...current, [key]: value } : current));
    setNotice('');
  };

  const submit = async () => {
    if (!configuration || saving || !dirty) return;
    setSaving(true);
    setNotice('');
    setError('');
    try {
      const savedConfiguration = await saveSystemConfiguration({
        aiModerationEnabled: configuration.aiModerationEnabled,
        maintenanceModeEnabled: configuration.maintenanceModeEnabled,
        rowVersion: configuration.rowVersion,
      });
      setConfiguration(savedConfiguration);
      setBaseline(savedConfiguration);
      setNotice('Đã lưu cấu hình hệ thống thành công.');
    } catch (saveError) {
      setError(errorMessage(saveError, 'Không thể lưu cấu hình hệ thống. Vui lòng thử lại.'));
    } finally {
      setSaving(false);
    }
  };

  const discard = () => {
    if (!baseline || saving) return;
    setConfiguration(baseline);
    setError('');
    setNotice('Đã hủy các thay đổi chưa lưu.');
  };

  const refresh = async () => {
    if (dirty && !window.confirm('Các thay đổi chưa lưu sẽ bị mất. Bạn có muốn tải lại cấu hình mới nhất?')) return;
    await load(true);
  };

  const updateMaintenanceMode = (value: boolean) => {
    if (value && !window.confirm('Bật chế độ bảo trì sẽ trả HTTP 503 cho người dùng thông thường. System Admin, đăng nhập/khôi phục và readiness vẫn truy cập được. Bạn có chắc chắn muốn tiếp tục?')) return;
    update('maintenanceModeEnabled', value);
  };

  if (!configuration) {
    return (
      <div className="portal-page font-sans">
        <ModPortalSidebar />
        <main className="portal-content grid min-h-screen place-items-center">
          {error ? (
            <div role="alert" className="max-w-md rounded-2xl border border-error-container bg-error-container/60 p-5 text-center text-sm text-error">
              <p className="m-0">{error}</p>
              <button type="button" onClick={() => void load()} className="mt-4 rounded-full bg-primary px-5 py-2.5 font-semibold text-on-primary">Thử lại</button>
            </div>
          ) : (
            <span aria-label="Đang tải cấu hình" className="h-10 w-10 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          )}
        </main>
      </div>
    );
  }

  const lastUpdated = configuration.updatedAt
    ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(configuration.updatedAt))
    : 'Chưa có thông tin';
  const aiModerationChanged = baseline !== null
    && configuration.aiModerationEnabled !== baseline.aiModerationEnabled;
  const maintenanceModeChanged = baseline !== null
    && configuration.maintenanceModeEnabled !== baseline.maintenanceModeEnabled;
  const pendingChangeCount = Number(aiModerationChanged) + Number(maintenanceModeChanged);

  const aiModerationLabel = (enabled: boolean) => enabled ? 'Đang chạy' : 'Đang tạm dừng';
  const maintenanceModeLabel = (enabled: boolean) => enabled ? 'Đang bảo trì' : 'Hoạt động bình thường';

  return (
    <div className="portal-page font-sans">
      <ModPortalSidebar />
      <main className="portal-content pb-28">
        <div className="p-5 md:p-8">
          <div className="mb-6 flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
            <div>
              <h1 className="m-0 text-[26px] font-bold text-on-surface">Cấu hình hệ thống</h1>
              <p className="mt-1 max-w-3xl text-sm text-on-surface-variant">
                Quản lý các chế độ ảnh hưởng trực tiếp đến hoạt động của CareBridge.
                <span className="font-semibold text-error"> Mọi thay đổi áp dụng toàn hệ thống và được ghi vào nhật ký kiểm toán.</span>
              </p>
            </div>
            <button type="button" onClick={() => void refresh()} disabled={saving || refreshing} className="inline-flex items-center gap-2 self-start rounded-full border border-outline-variant bg-surface px-5 py-2.5 text-sm font-semibold text-on-surface-variant hover:bg-surface-container-low disabled:cursor-not-allowed disabled:opacity-50 md:self-auto">
              <span aria-hidden="true" className={`material-symbols-outlined text-lg ${refreshing ? 'animate-spin' : ''}`}>refresh</span>
              {refreshing ? 'Đang tải...' : 'Tải lại dữ liệu'}
            </button>
          </div>

          {error && <div role="alert" className="mb-4 rounded-2xl border border-error-container bg-error-container/60 p-4 text-sm text-error">{error}</div>}
          {notice && <div role="status" className="mb-4 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm font-semibold text-emerald-700">{notice}</div>}

          <div className="grid gap-6 lg:grid-cols-3">
            <section className="self-start rounded-2xl border border-surface-container-highest bg-surface p-6 shadow-md lg:col-span-2">
              <div className="mb-4 flex items-center gap-2 border-b border-surface-container-highest pb-3">
                <ShieldCheck aria-hidden="true" className="text-primary" size={20} />
                <h2 className="m-0 text-base font-bold text-on-surface">Điều khiển vận hành</h2>
              </div>
              <div className="space-y-3">
                <Toggle
                  label="Kiểm duyệt tự động AI"
                  summary="Bật hoặc tạm dừng tạo mới và xử lý các tác vụ quét AI nền."
                  checked={configuration.aiModerationEnabled}
                  disabled={saving || refreshing}
                  onChange={(value) => update('aiModerationEnabled', value)}
                  guide={<>Khi tắt, nội dung vẫn được tạo nhưng không phát sinh tác vụ quét tự động mới; worker cũng không nhận tác vụ đang chờ. Hàng đợi được giữ nguyên và tiếp tục khi bật lại. Cấu hình hạ tầng Gemini vẫn là điều kiện riêng.</>}
                />
                <Toggle
                  label="Chế độ bảo trì hệ thống"
                  summary="Tạm khóa lưu lượng API của người dùng thông thường bằng phản hồi HTTP 503 có cấu trúc."
                  checked={configuration.maintenanceModeEnabled}
                  disabled={saving || refreshing}
                  highRisk
                  onChange={updateMaintenanceMode}
                  guide={<>Có hiệu lực trong tối đa khoảng 2 giây sau khi lưu. System Admin vẫn truy cập được để khôi phục; đăng nhập, làm mới token, hồ sơ xác thực, logout, cấu hình hệ thống và readiness không bị khóa. Khi cơ sở dữ liệu tạm thời lỗi, bộ lọc giữ trạng thái đã biết gần nhất.</>}
                />
              </div>
            </section>

            <aside className="space-y-6">
              <section aria-labelledby="operational-summary-title" className="rounded-2xl border border-surface-container-highest bg-surface p-6 shadow-md">
                <div className="mb-4 flex items-center gap-2">
                  <Activity aria-hidden="true" className="text-primary" size={19} />
                  <div>
                    <h2 id="operational-summary-title" className="m-0 text-base font-bold text-on-surface">Tóm tắt vận hành</h2>
                    <p className="m-0 mt-0.5 text-[11px] text-on-surface-variant">Trạng thái sẽ áp dụng sau lần lưu tiếp theo</p>
                  </div>
                </div>

                <dl className="space-y-3">
                  <div className={`rounded-xl border p-3 ${aiModerationChanged ? 'border-amber-300 bg-amber-50/80' : 'border-surface-container-highest bg-surface-bright'}`}>
                    <dt className="flex items-center gap-2 text-xs font-semibold text-on-surface-variant">
                      <Bot aria-hidden="true" size={15} /> Kiểm duyệt AI
                    </dt>
                    <dd className="m-0 mt-2 flex flex-wrap items-center gap-2 text-xs font-bold text-on-surface">
                      {aiModerationChanged && baseline ? (
                        <>
                          <span className="text-on-surface-variant line-through">{aiModerationLabel(baseline.aiModerationEnabled)}</span>
                          <ArrowRight aria-hidden="true" size={14} className="text-amber-400" />
                        </>
                      ) : null}
                      <span>{aiModerationLabel(configuration.aiModerationEnabled)}</span>
                      {aiModerationChanged && <span className="rounded-full bg-amber-100 px-2 py-0.5 text-[10px] text-amber-800">Chờ lưu</span>}
                    </dd>
                  </div>

                  <div className={`rounded-xl border p-3 ${maintenanceModeChanged ? 'border-amber-300 bg-amber-50/80' : configuration.maintenanceModeEnabled ? 'border-error/40 bg-error-container/30' : 'border-surface-container-highest bg-surface-bright'}`}>
                    <dt className="flex items-center gap-2 text-xs font-semibold text-on-surface-variant">
                      <Wrench aria-hidden="true" size={15} /> Truy cập hệ thống
                    </dt>
                    <dd className="m-0 mt-2 flex flex-wrap items-center gap-2 text-xs font-bold text-on-surface">
                      {maintenanceModeChanged && baseline ? (
                        <>
                          <span className="text-on-surface-variant line-through">{maintenanceModeLabel(baseline.maintenanceModeEnabled)}</span>
                          <ArrowRight aria-hidden="true" size={14} className="text-amber-400" />
                        </>
                      ) : null}
                      <span>{maintenanceModeLabel(configuration.maintenanceModeEnabled)}</span>
                      {maintenanceModeChanged && <span className="rounded-full bg-amber-100 px-2 py-0.5 text-[10px] text-amber-800">Chờ lưu</span>}
                    </dd>
                  </div>
                </dl>

                <div aria-live="polite" className="mt-4">
                  {!dirty && !configuration.maintenanceModeEnabled && (
                    <div className="flex items-start gap-2 rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-xs leading-relaxed text-emerald-700">
                      <CircleCheck aria-hidden="true" className="mt-0.5 shrink-0" size={15} />
                      Cấu hình trên màn hình đang đồng bộ với máy chủ.
                    </div>
                  )}
                  {dirty && (
                    <div className={`flex items-start gap-2 rounded-xl border p-3 text-xs leading-relaxed ${configuration.maintenanceModeEnabled && maintenanceModeChanged ? 'border-error/40 bg-error-container/40 text-error' : 'border-amber-300 bg-amber-50 text-amber-800'}`}>
                      <TriangleAlert aria-hidden="true" className="mt-0.5 shrink-0" size={15} />
                      <span>
                        <strong className="block">{pendingChangeCount} thay đổi đang chờ lưu.</strong>
                        {configuration.maintenanceModeEnabled && maintenanceModeChanged
                          ? 'Sau khi lưu, người dùng thông thường sẽ bị gián đoạn truy cập.'
                          : maintenanceModeChanged
                            ? 'Sau khi lưu, quyền truy cập bình thường sẽ được khôi phục.'
                            : 'Thay đổi chưa tác động đến hệ thống cho đến khi bạn lưu.'}
                      </span>
                    </div>
                  )}
                  {!dirty && configuration.maintenanceModeEnabled && (
                    <div className="flex items-start gap-2 rounded-xl border border-error/40 bg-error-container/40 p-3 text-xs leading-relaxed text-error">
                      <TriangleAlert aria-hidden="true" className="mt-0.5 shrink-0" size={15} />
                      Hệ thống đang bảo trì; người dùng thông thường hiện không thể truy cập API.
                    </div>
                  )}
                </div>
              </section>

              <section className="rounded-2xl bg-primary p-6 text-on-primary shadow-md">
                <div className="mb-2 flex items-center gap-2">
                  <span aria-hidden="true" className="material-symbols-outlined text-xl">admin_panel_settings</span>
                  <h2 className="m-0 text-base font-bold">Kiểm soát thay đổi</h2>
                </div>
                <p className="m-0 mt-2 text-xs leading-relaxed text-primary-fixed">Hệ thống ngăn ghi đè khi có quản trị viên khác cập nhật, đồng thời ghi lại mọi lần lưu.</p>
                <dl className="mt-4 space-y-2 rounded-xl bg-white/10 p-3 text-xs backdrop-blur-sm">
                  <div className="flex justify-between gap-3"><dt>Phiên bản cấu hình</dt><dd className="m-0 font-bold">#{configuration.rowVersion}</dd></div>
                  <div className="flex justify-between gap-3"><dt>Cập nhật cuối</dt><dd className="m-0 text-right font-bold">{lastUpdated}</dd></div>
                  <div className="flex justify-between gap-3"><dt>Quyền thay đổi</dt><dd className="m-0 font-bold">System Admin</dd></div>
                </dl>
              </section>
            </aside>
          </div>
        </div>

        <footer className="fixed bottom-0 left-0 right-0 z-20 flex flex-wrap items-center justify-between gap-3 border-t border-surface-container-highest bg-surface/95 px-4 py-4 shadow-lg backdrop-blur-md md:left-64 md:px-6">
          <span className={`text-sm font-semibold ${dirty ? 'text-amber-400' : 'text-on-surface-variant'}`}>
            {dirty ? 'Có thay đổi chưa lưu' : 'Không có thay đổi chưa lưu'}
          </span>
          <div className="flex items-center gap-3">
            <button type="button" onClick={discard} disabled={!dirty || saving || refreshing} className="rounded-full border border-outline-variant bg-surface px-5 py-2.5 text-xs font-semibold text-on-surface-variant hover:bg-surface-container-low disabled:cursor-not-allowed disabled:opacity-50">Hủy thay đổi</button>
            <button type="button" onClick={() => void submit()} disabled={!dirty || saving || refreshing} className="inline-flex items-center gap-2 rounded-full bg-primary px-6 py-2.5 text-xs font-semibold text-on-primary shadow-sm hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50">
              <span aria-hidden="true" className="material-symbols-outlined text-base">save</span>
              {saving ? 'Đang lưu...' : 'Lưu toàn bộ cấu hình'}
            </button>
          </div>
        </footer>
      </main>
    </div>
  );
}
