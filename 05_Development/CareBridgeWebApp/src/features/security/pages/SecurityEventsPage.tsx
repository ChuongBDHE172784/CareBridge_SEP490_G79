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
    <div>
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-on-surface m-0">Sự kiện Bảo mật (CB-142)</h1>
          <p className="text-on-surface-variant text-sm mt-1">Theo dõi và giám sát các hoạt động bảo mật hệ thống CareBridge.</p>
        </div>
        <button className="flex items-center gap-2 px-5 py-2.5 rounded-full bg-primary text-on-primary border-none text-sm font-semibold cursor-pointer">
          <span className="material-symbols-outlined text-lg">download</span> Xuất báo cáo (CSV)
        </button>
      </div>

      <div className="grid grid-cols-4 gap-4 mb-6">
        <StatCard icon="error" label="Sự cố High Severity" value={String(highCount)} accentClass="text-error" />
        <StatCard icon="shield" label="Đã xử lý" value={String(total)} accentClass="text-primary" />
        <StatCard icon="group" label="Admin hoạt động" value="—" accentClass="text-on-surface-variant" />
        <StatCard icon="monitor_heart" label="Sức khỏe hệ thống" value="Ổn định" accentClass="text-primary" />
      </div>

      <div className="bg-surface rounded-2xl p-6 shadow-sm">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-lg font-semibold text-on-surface m-0">Log sự kiện</h2>
          <div className="flex gap-2">
            {['HIGH', 'MEDIUM', 'LOW'].map(s => (
              <button
                key={s}
                onClick={() => { setSeverityFilter(severityFilter === s ? '' : s); setPage(0); }}
                className={`px-4 py-1.5 rounded-full border text-[13px] font-semibold cursor-pointer transition-colors ${severityFilter === s ? 'bg-primary text-on-primary border-transparent' : 'bg-surface border-outline-variant text-on-surface-variant'}`}
              >
                {s === 'HIGH' ? 'High' : s === 'MEDIUM' ? 'Medium' : 'Low'}
              </button>
            ))}
          </div>
        </div>

        {isLoading ? (
          <div className="p-12 text-center text-outline">Đang tải...</div>
        ) : (
          <table className="w-full border-collapse">
            <thead>
              <tr className="border-b-2 border-surface-container-highest text-left">
                {['MỨC ĐỘ', 'LOẠI SỰ KIỆN', 'TÁC NHÂN (ACTOR)', 'ĐỐI TƯỢNG (TARGET)', 'THỜI GIAN', 'TRẠNG THÁI'].map(h => (
                  <th key={h} className="px-2 py-3 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {events.map(e => (
                <tr key={e.id} className="border-b border-surface-container-highest cursor-pointer">
                  <td className="px-2 py-3.5 text-[13px]">
                    <span className={`font-semibold text-xs ${e.severity === 'HIGH' ? 'text-error' : e.severity === 'MEDIUM' ? 'text-primary' : 'text-on-surface-variant'}`}>
                      {e.severity}
                    </span>
                  </td>
                  <td className="px-2 py-3.5">
                    <div className="font-semibold text-sm text-on-surface">{e.eventType}</div>
                    <div className="text-xs text-on-surface-variant mt-0.5">{e.details?.substring(0, 50)}</div>
                  </td>
                  <td className="px-2 py-3.5 text-[13px] text-on-surface">{e.userId || '—'}</td>
                  <td className="px-2 py-3.5 text-xs text-outline font-mono">{e.ipAddress}</td>
                  <td className="px-2 py-3.5 text-[13px] text-on-surface-variant">{formatTime(e.occurredAt)}</td>
                  <td className="px-2 py-3.5">
                    <span className={`px-3 py-1 rounded-full border border-outline-variant text-xs font-medium ${e.status === 'RESOLVED' ? 'text-on-surface-variant' : 'text-primary'}`}>
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
  );
}

function StatCard({ icon, label, value, accentClass }: { icon: string; label: string; value: string; accentClass: string }) {
  return (
    <div className="bg-surface rounded-2xl p-5 shadow-sm">
      <div className="flex items-center gap-2 mb-2">
        <span className={`material-symbols-outlined text-xl ${accentClass}`}>{icon}</span>
        <span className="text-[13px] text-on-surface-variant">{label}</span>
      </div>
      <div className="text-3xl font-bold text-on-surface">{value}</div>
    </div>
  );
}

function formatTime(iso: string): string {
  const d = new Date(iso);
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}:${d.getSeconds().toString().padStart(2, '0')}\n${d.getDate().toString().padStart(2, '0')}/${(d.getMonth()+1).toString().padStart(2, '0')}/${d.getFullYear()}`;
}
