import { useEffect, useState, useCallback } from 'react';
import apiClient from '../../../shared/api/apiClient';

interface SecurityEvent {
  id: number;
  eventType: string;
  userId: string | null;
  ipAddress: string;
  severity: string;
  status: string;
  details: string;
  correlationId: string | null;
  reviewedBy: string | null;
  reviewedAt: string | null;
  occurredAt: string;
}

const SEVERITY_COLORS: Record<string, { bg: string; text: string; label: string }> = {
  HIGH: { bg: '#ffdad6', text: '#ba1a1a', label: 'Cao' },
  MEDIUM: { bg: '#ffe9e3', text: '#845143', label: 'Trung bình' },
  LOW: { bg: '#fff1ec', text: '#6e5a52', label: 'Thấp' },
};

const STATUS_LABELS: Record<string, string> = {
  DETECTED: 'Đang mở',
  INVESTIGATING: 'Đang điều tra',
  RESOLVED: 'Đã giải quyết',
  DISMISSED: 'Đã bỏ qua',
};

export default function SecurityIncidentListPage() {
  const [events, setEvents] = useState<SecurityEvent[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [severity, setSeverity] = useState('');
  const [status, setStatus] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  const fetchData = useCallback(async () => {
    setIsLoading(true);
    try {
      const params = new URLSearchParams({ page: String(page), size: '10' });
      if (severity) params.set('severity', severity);
      if (status) params.set('status', status);
      const res = await apiClient.get(`/api/v1/admin/security-events?${params}`);
      setEvents(res.data.data.content);
      setTotal(res.data.data.totalElements);
    } catch { /* handled */ } finally { setIsLoading(false); }
  }, [page, severity, status]);

  useEffect(() => { fetchData(); }, [fetchData]);

  return (
    <div style={{ fontFamily: "'Lexend', sans-serif" }}>
      <h1 style={{ fontSize: 28, fontWeight: 700, color: '#271812', margin: 0 }}>Danh sách Sự cố Bảo mật</h1>
      <p style={{ color: '#524440', fontSize: 14, marginTop: 4, marginBottom: 24 }}>Theo dõi và quản lý các cảnh báo an toàn hệ thống (CB-151).</p>

      <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginBottom: 20 }}>
        <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Tìm theo Case ID, Loại..." style={inputStyle} />
        <select value={severity} onChange={e => { setSeverity(e.target.value); setPage(0); }} style={selectStyle}>
          <option value="">Mức độ: Tất cả</option>
          <option value="HIGH">Cao</option>
          <option value="MEDIUM">Trung bình</option>
          <option value="LOW">Thấp</option>
        </select>
        <select value={status} onChange={e => { setStatus(e.target.value); setPage(0); }} style={selectStyle}>
          <option value="">Trạng thái: Tất cả</option>
          <option value="DETECTED">Đang mở</option>
          <option value="INVESTIGATING">Đang điều tra</option>
          <option value="RESOLVED">Đã giải quyết</option>
        </select>
      </div>

      <div style={{ background: '#fff', borderRadius: 16, overflow: 'hidden', boxShadow: '0 4px 20px rgba(90,70,63,0.06)' }}>
        {isLoading ? (
          <div style={{ padding: 48, textAlign: 'center', color: '#84736f' }}>Đang tải...</div>
        ) : (
          <>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ borderBottom: '2px solid #fadcd3', textAlign: 'left' }}>
                  {['Case ID', 'Mức độ', 'Loại sự cố', 'Trạng thái', 'Phạm vi', 'Thời gian mở', 'SLA'].map(h => (
                    <th key={h} style={thStyle}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {events.map(e => {
                  const sev = SEVERITY_COLORS[e.severity] ?? SEVERITY_COLORS.LOW;
                  return (
                    <tr key={e.id} style={{ borderBottom: '1px solid #fadcd3', cursor: 'pointer' }}>
                      <td style={{ ...tdStyle, color: '#845143', fontWeight: 600 }}>SEC-{e.id}</td>
                      <td style={tdStyle}>
                        <span style={{ padding: '4px 12px', borderRadius: 9999, background: sev.bg, color: sev.text, fontSize: 12, fontWeight: 600 }}>
                          {sev.label}
                        </span>
                      </td>
                      <td style={tdStyle}>{e.eventType}</td>
                      <td style={tdStyle}>
                        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
                          <span style={{ width: 8, height: 8, borderRadius: '50%', background: e.status === 'RESOLVED' ? '#6e5a52' : '#ba1a1a' }} />
                          {STATUS_LABELS[e.status] ?? e.status}
                        </span>
                      </td>
                      <td style={tdStyle}>{e.ipAddress || '—'}</td>
                      <td style={tdStyle}>{formatDateTime(e.occurredAt)}</td>
                      <td style={{ ...tdStyle, color: e.severity === 'HIGH' ? '#ba1a1a' : '#524440', fontWeight: e.severity === 'HIGH' ? 600 : 400 }}>
                        {e.status === 'RESOLVED' ? '—' : '—'}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '16px 24px', fontSize: 14, color: '#524440' }}>
              <span>Hiển thị {page * 10 + 1}-{Math.min((page + 1) * 10, total)} trên tổng số {total} sự cố</span>
              <div style={{ display: 'flex', gap: 8 }}>
                <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0} style={pgBtn(page === 0)}>&lt;</button>
                <button onClick={() => setPage(p => p + 1)} disabled={(page + 1) * 10 >= total} style={pgBtn((page + 1) * 10 >= total)}>&gt;</button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

const thStyle: React.CSSProperties = { padding: '14px 16px', fontSize: 13, fontWeight: 600, color: '#84736f', textTransform: 'uppercase', letterSpacing: '0.04em' };
const tdStyle: React.CSSProperties = { padding: '16px', fontSize: 14, color: '#271812' };
const inputStyle: React.CSSProperties = { padding: '10px 16px', borderRadius: 8, border: '1px solid #d6c2bd', fontSize: 14, fontFamily: "'Lexend'", width: 260 };
const selectStyle: React.CSSProperties = { padding: '10px 16px', borderRadius: 8, border: '1px solid #d6c2bd', fontSize: 14, fontFamily: "'Lexend'", background: '#fff', cursor: 'pointer' };

function pgBtn(disabled: boolean): React.CSSProperties {
  return { width: 36, height: 36, borderRadius: 8, border: '1px solid #d6c2bd', background: disabled ? '#f6f1ec' : '#fff', color: disabled ? '#d6c2bd' : '#524440', cursor: disabled ? 'default' : 'pointer', fontSize: 16 };
}

function formatDateTime(iso: string): string {
  const d = new Date(iso);
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')} ${d.getDate().toString().padStart(2, '0')}/${(d.getMonth() + 1).toString().padStart(2, '0')}/${d.getFullYear()}`;
}
