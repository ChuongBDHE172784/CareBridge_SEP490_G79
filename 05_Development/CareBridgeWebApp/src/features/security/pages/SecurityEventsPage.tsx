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
    <div style={{ fontFamily: "'Lexend', sans-serif" }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <div>
          <h1 style={{ fontSize: 24, fontWeight: 700, color: '#271812', margin: 0 }}>Security Events (CB-142)</h1>
          <p style={{ color: '#524440', fontSize: 14, marginTop: 4 }}>Theo dõi và giám sát các hoạt động bảo mật hệ thống CareBridge.</p>
        </div>
        <button style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '10px 20px', borderRadius: 9999, background: '#845143', color: '#fff', border: 'none', fontSize: 14, fontWeight: 600, cursor: 'pointer' }}>
          <span className="material-symbols-outlined" style={{ fontSize: 18 }}>download</span> Xuất báo cáo (CSV)
        </button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 24 }}>
        <StatCard icon="error" label="Sự cố High Severity" value={String(highCount)} accent="#ba1a1a" />
        <StatCard icon="shield" label="Đã xử lý" value={String(total)} accent="#845143" />
        <StatCard icon="group" label="Admin hoạt động" value="—" accent="#6e5a52" />
        <StatCard icon="monitor_heart" label="Sức khỏe hệ thống" value="Ổn định" accent="#845143" />
      </div>

      <div style={{ background: '#fff', borderRadius: 16, padding: 24, boxShadow: '0 4px 20px rgba(90,70,63,0.06)' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <h2 style={{ fontSize: 18, fontWeight: 600, color: '#271812', margin: 0 }}>Log sự kiện</h2>
          <div style={{ display: 'flex', gap: 8 }}>
            {['HIGH', 'MEDIUM', 'LOW'].map(s => (
              <button key={s} onClick={() => { setSeverityFilter(severityFilter === s ? '' : s); setPage(0); }}
                style={{ padding: '6px 16px', borderRadius: 9999, border: '1px solid #d6c2bd', background: severityFilter === s ? '#845143' : '#fff', color: severityFilter === s ? '#fff' : '#524440', fontSize: 13, fontWeight: 600, cursor: 'pointer' }}>
                {s === 'HIGH' ? 'High' : s === 'MEDIUM' ? 'Medium' : 'Low'}
              </button>
            ))}
          </div>
        </div>

        {isLoading ? <div style={{ padding: 48, textAlign: 'center', color: '#84736f' }}>Đang tải...</div> : (
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '2px solid #fadcd3', textAlign: 'left' }}>
                {['MỨC ĐỘ', 'LOẠI SỰ KIỆN', 'TÁC NHÂN (ACTOR)', 'ĐỐI TƯỢNG (TARGET)', 'THỜI GIAN', 'TRẠNG THÁI'].map(h => (
                  <th key={h} style={{ padding: '12px 8px', fontSize: 11, fontWeight: 600, color: '#84736f', textTransform: 'uppercase', letterSpacing: '0.05em' }}>{h}</th>
                ))}
              </tr>
              {events.map(e => (
                <tr key={e.id} style={{ borderBottom: '1px solid #fadcd3', cursor: 'pointer' }}>
                  <td style={{ padding: '14px 8px', fontSize: 13 }}>
                    <span style={{ color: e.severity === 'HIGH' ? '#ba1a1a' : e.severity === 'MEDIUM' ? '#845143' : '#6e5a52', fontWeight: 600, fontSize: 12 }}>
                      {e.severity}
                    </span>
                  </td>
                  <td style={{ padding: '14px 8px' }}>
                    <div style={{ fontWeight: 600, fontSize: 14, color: '#271812' }}>{e.eventType}</div>
                    <div style={{ fontSize: 12, color: '#524440', marginTop: 2 }}>{e.details?.substring(0, 50)}</div>
                  </td>
                  <td style={{ padding: '14px 8px', fontSize: 13, color: '#271812' }}>{e.userId || '—'}</td>
                  <td style={{ padding: '14px 8px', fontSize: 12, color: '#84736f', fontFamily: 'monospace' }}>{e.ipAddress}</td>
                  <td style={{ padding: '14px 8px', fontSize: 13, color: '#524440' }}>{formatTime(e.occurredAt)}</td>
                  <td style={{ padding: '14px 8px' }}>
                    <span style={{ padding: '4px 12px', borderRadius: 9999, border: '1px solid #d6c2bd', fontSize: 12, fontWeight: 500, color: e.status === 'RESOLVED' ? '#6e5a52' : '#845143' }}>
                      {e.status === 'DETECTED' ? 'Detected' : e.status === 'INVESTIGATING' ? 'Investigating' : 'Resolved'}
                    </span>
                  </td>
                </tr>
              ))}
            </thead>
          </table>
        )}
      </div>
    </div>
  );
}

function StatCard({ icon, label, value, accent }: { icon: string; label: string; value: string; accent: string }) {
  return (
    <div style={{ background: '#fff', borderRadius: 16, padding: 20, boxShadow: '0 4px 20px rgba(90,70,63,0.06)' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
        <span className="material-symbols-outlined" style={{ fontSize: 20, color: accent }}>{icon}</span>
        <span style={{ fontSize: 13, color: '#524440' }}>{label}</span>
      </div>
      <div style={{ fontSize: 28, fontWeight: 700, color: '#271812' }}>{value}</div>
    </div>
  );
}

function formatTime(iso: string): string {
  const d = new Date(iso);
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}:${d.getSeconds().toString().padStart(2, '0')}\n${d.getDate().toString().padStart(2, '0')}/${(d.getMonth()+1).toString().padStart(2, '0')}/${d.getFullYear()}`;
}
