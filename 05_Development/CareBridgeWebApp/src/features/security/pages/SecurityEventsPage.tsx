import { useEffect, useState, useCallback } from 'react';
import apiClient from '../../../shared/api/apiClient';

interface SecurityEvent {
  id: number; eventType: string; userId: string | null; ipAddress: string;
  severity: string; status: string; details: string; occurredAt: string;
  correlationId: string | null; reviewedBy: string | null;
}

export default function SecurityEventsPage() {
  const [events, setEvents] = useState<SecurityEvent[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [severityFilter, setSeverityFilter] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  const fetchData = useCallback(async () => {
    setIsLoading(true);
    try {
      const params = new URLSearchParams({ page: String(page), size: '20' });
      if (severityFilter) params.set('severity', severityFilter);
      const res = await apiClient.get(`/api/v1/admin/security-events?${params}`);
      setEvents(res.data.data.content);
      setTotal(res.data.data.totalElements);
    } catch { /* */ } finally { setIsLoading(false); }
  }, [page, severityFilter]);

  useEffect(() => { fetchData(); }, [fetchData]);

  const highCount = events.filter(e => e.severity === 'HIGH').length;

  return (
    <div className="portal-page px-5 py-5 md:px-6 md:py-6">
      <div className="portal-contained">
      <div className="portal-header">
        <div>
          <p className="portal-eyebrow">CB-142</p>
          <h1 className="portal-title">Sự kiện bảo mật</h1>
          <p className="portal-subtitle">Theo dõi và giám sát các hoạt động bảo mật hệ thống CareBridge.</p>
        </div>
        <button className="portal-primary-button">
          <span className="material-symbols-outlined text-lg">download</span> Xuất báo cáo (CSV)
        </button>
      </div>

      <div className="mb-5 grid grid-cols-1 gap-3 md:grid-cols-2 lg:grid-cols-4">
        <StatCard icon="error" label="Sự cố High Severity" value={String(highCount)} accentClass="text-error" />
        <StatCard icon="shield" label="Đã xử lý" value={String(total)} accentClass="text-primary" />
        <StatCard icon="group" label="Admin hoạt động" value="—" accentClass="text-on-surface-variant" />
        <StatCard icon="monitor_heart" label="Sức khỏe hệ thống" value="Ổn định" accentClass="text-primary" />
      </div>

      <div className="portal-table-card">
        <div className="portal-toolbar border-b border-outline-variant p-4">
          <h2 className="m-0 text-base font-semibold text-on-surface">Log sự kiện</h2>
          <div className="flex flex-wrap gap-2">
            {['HIGH', 'MEDIUM', 'LOW'].map(s => (
              <button
                key={s}
                onClick={() => { setSeverityFilter(severityFilter === s ? '' : s); setPage(0); }}
                className={`rounded-md border px-3 py-1.5 text-[13px] font-semibold transition-colors ${severityFilter === s ? 'border-transparent bg-primary text-on-primary' : 'border-outline-variant bg-surface text-on-surface-variant'}`}
              >
                {s === 'HIGH' ? 'High' : s === 'MEDIUM' ? 'Medium' : 'Low'}
              </button>
            ))}
          </div>
        </div>

        {isLoading ? (
          <div className="portal-empty">Đang tải...</div>
        ) : (
          <table className="w-full">
            <thead>
              <tr>
                {['MỨC ĐỘ', 'LOẠI SỰ KIỆN', 'TÁC NHÂN (ACTOR)', 'ĐỐI TƯỢNG (TARGET)', 'THỜI GIAN', 'TRẠNG THÁI'].map(h => (
                  <th key={h}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {events.map(e => (
                <tr key={e.id} className="cursor-pointer">
                  <td>
                    <span className={`font-semibold text-xs ${e.severity === 'HIGH' ? 'text-error' : e.severity === 'MEDIUM' ? 'text-primary' : 'text-on-surface-variant'}`}>
                      {e.severity}
                    </span>
                  </td>
                  <td>
                    <div className="font-semibold text-sm text-on-surface">{e.eventType}</div>
                    <div className="text-xs text-on-surface-variant mt-0.5">{e.details?.substring(0, 50)}</div>
                  </td>
                  <td>{e.userId || '—'}</td>
                  <td className="font-mono text-xs text-outline">{e.ipAddress}</td>
                  <td className="text-on-surface-variant">{formatTime(e.occurredAt)}</td>
                  <td>
                    <span className={`rounded-md border border-outline-variant px-2.5 py-1 text-xs font-medium ${e.status === 'RESOLVED' ? 'text-on-surface-variant' : 'text-primary'}`}>
                      {e.status === 'DETECTED' ? 'Detected' : e.status === 'INVESTIGATING' ? 'Investigating' : 'Resolved'}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
      </div>
    </div>
  );
}

function StatCard({ icon, label, value, accentClass }: { icon: string; label: string; value: string; accentClass: string }) {
  return (
    <div className="portal-stat-card">
      <div className="mb-2 flex items-center gap-2">
        <span className={`material-symbols-outlined text-lg ${accentClass}`}>{icon}</span>
        <span className="text-[13px] text-on-surface-variant">{label}</span>
      </div>
      <div className="portal-metric">{value}</div>
    </div>
  );
}

function formatTime(iso: string): string {
  const d = new Date(iso);
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}:${d.getSeconds().toString().padStart(2, '0')}\n${d.getDate().toString().padStart(2, '0')}/${(d.getMonth()+1).toString().padStart(2, '0')}/${d.getFullYear()}`;
}
