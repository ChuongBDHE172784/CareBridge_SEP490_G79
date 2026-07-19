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

const SEVERITY_BADGE: Record<string, { cls: string; label: string }> = {
  HIGH: { cls: 'bg-error-container text-error', label: 'Cao' },
  MEDIUM: { cls: 'bg-surface-container-high text-primary', label: 'Trung bình' },
  LOW: { cls: 'bg-surface-container-low text-on-surface-variant', label: 'Thấp' },
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
    <div className="portal-page px-5 py-5 md:px-6 md:py-6">
      <div className="portal-contained">
      <div className="portal-header">
        <div>
          <p className="portal-eyebrow">CB-151</p>
          <h1 className="portal-title">Danh sách sự cố bảo mật</h1>
          <p className="portal-subtitle">Theo dõi và quản lý các cảnh báo an toàn hệ thống.</p>
        </div>
      </div>

      <div className="portal-toolbar mb-4">
        <input
          value={search}
          onChange={e => setSearch(e.target.value)}
          placeholder="Tìm theo Case ID, Loại..."
          className="portal-field w-full sm:w-64"
        />
        <select
          value={severity}
          onChange={e => { setSeverity(e.target.value); setPage(0); }}
          className="portal-field w-full sm:w-auto"
        >
          <option value="">Mức độ: Tất cả</option>
          <option value="HIGH">Cao</option>
          <option value="MEDIUM">Trung bình</option>
          <option value="LOW">Thấp</option>
        </select>
        <select
          value={status}
          onChange={e => { setStatus(e.target.value); setPage(0); }}
          className="portal-field w-full sm:w-auto"
        >
          <option value="">Trạng thái: Tất cả</option>
          <option value="DETECTED">Đang mở</option>
          <option value="INVESTIGATING">Đang điều tra</option>
          <option value="RESOLVED">Đã giải quyết</option>
        </select>
      </div>

      <div className="portal-table-card">
        {isLoading ? (
          <div className="portal-empty">Đang tải...</div>
        ) : (
          <>
            <table className="w-full">
              <thead>
                <tr>
                  {['Case ID', 'Mức độ', 'Loại sự cố', 'Trạng thái', 'Phạm vi', 'Thời gian mở', 'SLA'].map(h => (
                    <th key={h}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {events.map(e => {
                  const sev = SEVERITY_BADGE[e.severity] ?? SEVERITY_BADGE.LOW;
                  return (
                    <tr key={e.id} className="cursor-pointer">
                      <td className="font-semibold text-primary">SEC-{e.id}</td>
                      <td>
                        <span className={`rounded-md px-2.5 py-1 text-xs font-semibold ${sev.cls}`}>
                          {sev.label}
                        </span>
                      </td>
                      <td>{e.eventType}</td>
                      <td>
                        <span className="inline-flex items-center gap-1.5">
                          <span className={`h-2 w-2 rounded-full ${e.status === 'RESOLVED' ? 'bg-outline' : 'bg-error'}`} />
                          {STATUS_LABELS[e.status] ?? e.status}
                        </span>
                      </td>
                      <td>{e.ipAddress || '—'}</td>
                      <td>{formatDateTime(e.occurredAt)}</td>
                      <td className={e.severity === 'HIGH' ? 'font-semibold text-error' : 'text-on-surface-variant'}>
                        {e.status === 'RESOLVED' ? '—' : '—'}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
            <div className="flex items-center justify-between border-t border-outline-variant px-4 py-3 text-sm text-on-surface-variant">
              <span>Hiển thị {page * 10 + 1}-{Math.min((page + 1) * 10, total)} trên tổng số {total} sự cố</span>
              <div className="flex gap-2">
                <button
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className={`h-8 w-8 rounded-md border border-outline-variant text-base ${page === 0 ? 'cursor-default bg-surface-container-low text-outline-variant' : 'cursor-pointer bg-surface text-on-surface-variant'}`}
                >&lt;</button>
                <button
                  onClick={() => setPage(p => p + 1)}
                  disabled={(page + 1) * 10 >= total}
                  className={`h-8 w-8 rounded-md border border-outline-variant text-base ${(page + 1) * 10 >= total ? 'cursor-default bg-surface-container-low text-outline-variant' : 'cursor-pointer bg-surface text-on-surface-variant'}`}
                >&gt;</button>
              </div>
            </div>
          </>
        )}
      </div>
      </div>
    </div>
  );
}

function formatDateTime(iso: string): string {
  const d = new Date(iso);
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')} ${d.getDate().toString().padStart(2, '0')}/${(d.getMonth() + 1).toString().padStart(2, '0')}/${d.getFullYear()}`;
}
