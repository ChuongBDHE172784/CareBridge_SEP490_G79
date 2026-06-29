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
    <div>
      <h1 className="text-[28px] font-bold text-on-surface m-0">Danh sách Sự cố Bảo mật</h1>
      <p className="text-on-surface-variant text-sm mt-1 mb-6">Theo dõi và quản lý các cảnh báo an toàn hệ thống (CB-151).</p>

      <div className="flex gap-3 flex-wrap mb-5">
        <input
          value={search}
          onChange={e => setSearch(e.target.value)}
          placeholder="Tìm theo Case ID, Loại..."
          className="px-4 py-2.5 rounded-lg border border-outline-variant text-sm w-64"
        />
        <select
          value={severity}
          onChange={e => { setSeverity(e.target.value); setPage(0); }}
          className="px-4 py-2.5 rounded-lg border border-outline-variant text-sm bg-surface cursor-pointer"
        >
          <option value="">Mức độ: Tất cả</option>
          <option value="HIGH">Cao</option>
          <option value="MEDIUM">Trung bình</option>
          <option value="LOW">Thấp</option>
        </select>
        <select
          value={status}
          onChange={e => { setStatus(e.target.value); setPage(0); }}
          className="px-4 py-2.5 rounded-lg border border-outline-variant text-sm bg-surface cursor-pointer"
        >
          <option value="">Trạng thái: Tất cả</option>
          <option value="DETECTED">Đang mở</option>
          <option value="INVESTIGATING">Đang điều tra</option>
          <option value="RESOLVED">Đã giải quyết</option>
        </select>
      </div>

      <div className="bg-surface rounded-2xl overflow-hidden shadow-sm">
        {isLoading ? (
          <div className="p-12 text-center text-outline">Đang tải...</div>
        ) : (
          <>
            <table className="w-full border-collapse">
              <thead>
                <tr className="border-b-2 border-surface-container-highest text-left">
                  {['Case ID', 'Mức độ', 'Loại sự cố', 'Trạng thái', 'Phạm vi', 'Thời gian mở', 'SLA'].map(h => (
                    <th key={h} className="px-4 py-3.5 text-[13px] font-semibold text-outline uppercase tracking-[0.04em]">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {events.map(e => {
                  const sev = SEVERITY_BADGE[e.severity] ?? SEVERITY_BADGE.LOW;
                  return (
                    <tr key={e.id} className="border-b border-surface-container-highest cursor-pointer">
                      <td className="p-4 text-sm text-primary font-semibold">SEC-{e.id}</td>
                      <td className="p-4 text-sm">
                        <span className={`px-3 py-1 rounded-full text-xs font-semibold ${sev.cls}`}>
                          {sev.label}
                        </span>
                      </td>
                      <td className="p-4 text-sm text-on-surface">{e.eventType}</td>
                      <td className="p-4 text-sm text-on-surface">
                        <span className="inline-flex items-center gap-1.5">
                          <span className={`w-2 h-2 rounded-full ${e.status === 'RESOLVED' ? 'bg-outline' : 'bg-error'}`} />
                          {STATUS_LABELS[e.status] ?? e.status}
                        </span>
                      </td>
                      <td className="p-4 text-sm text-on-surface">{e.ipAddress || '—'}</td>
                      <td className="p-4 text-sm text-on-surface">{formatDateTime(e.occurredAt)}</td>
                      <td className={`p-4 text-sm ${e.severity === 'HIGH' ? 'text-error font-semibold' : 'text-on-surface-variant'}`}>
                        {e.status === 'RESOLVED' ? '—' : '—'}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
            <div className="flex justify-between items-center px-6 py-4 text-sm text-on-surface-variant">
              <span>Hiển thị {page * 10 + 1}-{Math.min((page + 1) * 10, total)} trên tổng số {total} sự cố</span>
              <div className="flex gap-2">
                <button
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className={`w-9 h-9 rounded-lg border border-outline-variant text-base ${page === 0 ? 'bg-surface-container-low text-outline-variant cursor-default' : 'bg-surface text-on-surface-variant cursor-pointer'}`}
                >&lt;</button>
                <button
                  onClick={() => setPage(p => p + 1)}
                  disabled={(page + 1) * 10 >= total}
                  className={`w-9 h-9 rounded-lg border border-outline-variant text-base ${(page + 1) * 10 >= total ? 'bg-surface-container-low text-outline-variant cursor-default' : 'bg-surface text-on-surface-variant cursor-pointer'}`}
                >&gt;</button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

function formatDateTime(iso: string): string {
  const d = new Date(iso);
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')} ${d.getDate().toString().padStart(2, '0')}/${(d.getMonth() + 1).toString().padStart(2, '0')}/${d.getFullYear()}`;
}
