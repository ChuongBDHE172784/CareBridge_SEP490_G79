import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  searchSecurityIncidents,
  type SecurityEvent,
} from '../services/securityIncidentApi';

const PAGE_SIZE = 10;
const SEVERITY_BADGE: Record<string, { cls: string; label: string; icon: string }> = {
  CRITICAL: { cls: 'bg-error-container text-error', label: 'Nghiêm trọng', icon: 'error' },
  HIGH: { cls: 'bg-error-container text-error', label: 'Cao', icon: 'warning' },
  MEDIUM: { cls: 'bg-surface-container-high text-primary', label: 'Trung bình', icon: 'info' },
  LOW: { cls: 'bg-surface-container-low text-on-surface-variant', label: 'Thấp', icon: 'check_circle' },
};
const STATUS_META: Record<string, { label: string; icon: string }> = {
  OPEN: { label: 'Đang mở', icon: 'radio_button_checked' },
  UNDER_REVIEW: { label: 'Đang điều tra', icon: 'manage_search' },
  RESOLVED: { label: 'Đã giải quyết', icon: 'task_alt' },
  FALSE_POSITIVE: { label: 'Không phải sự cố', icon: 'cancel' },
};

export default function SecurityIncidentListPage() {
  const navigate = useNavigate();
  const [events, setEvents] = useState<SecurityEvent[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [severity, setSeverity] = useState('');
  const [status, setStatus] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  const fetchData = useCallback(async () => {
    setIsLoading(true);
    setError('');
    try {
      const result = await searchSecurityIncidents({ page, size: PAGE_SIZE, severity, status });
      setEvents(result.content ?? []);
      setTotal(result.totalElements ?? 0);
    } catch {
      setError('Không thể tải danh sách sự cố. Vui lòng thử lại.');
    } finally {
      setIsLoading(false);
    }
  }, [page, severity, status]);

  useEffect(() => { void fetchData(); }, [fetchData]);

  const visibleEvents = useMemo(() => {
    const keyword = search.trim().toLocaleLowerCase('vi');
    if (!keyword) return events;
    return events.filter((event) =>
      [`SEC-${event.id}`, event.eventType, event.ipAddress, event.status]
        .filter(Boolean).some((value) => String(value).toLocaleLowerCase('vi').includes(keyword)),
    );
  }, [events, search]);

  const openEvent = (event: SecurityEvent) => {
    const destination = event.status === 'UNDER_REVIEW'
      ? `/admin/security/incidents/${event.id}/resolve`
      : `/admin/security/incidents/${event.id}/investigate`;
    navigate(destination);
  };

  return (
    <div className="portal-page px-5 py-5 md:px-6 md:py-6">
      <div className="portal-contained">
        <div className="portal-header">
          <div>
            <p className="portal-eyebrow">An toàn hệ thống</p>
            <h1 className="portal-title">Sự cố bảo mật</h1>
            <p className="portal-subtitle">Rà soát cảnh báo, điều tra theo timeline và lưu đầy đủ dấu vết xử lý.</p>
          </div>
          <button className="portal-secondary-button" onClick={() => void fetchData()} disabled={isLoading}>
            <span className="material-symbols-outlined text-lg">refresh</span> Làm mới
          </button>
        </div>

        <div className="portal-toolbar mb-4">
          <label className="relative w-full sm:w-72">
            <span className="sr-only">Tìm kiếm sự cố</span>
            <input value={search} onChange={(e) => setSearch(e.target.value)}
              placeholder="Tìm Case ID, loại, IP..." className="portal-field w-full" />
          </label>
          <select aria-label="Lọc mức độ" value={severity} onChange={(e) => { setSeverity(e.target.value); setPage(0); }} className="portal-field">
            <option value="">Mức độ: Tất cả</option><option value="CRITICAL">Nghiêm trọng</option>
            <option value="HIGH">Cao</option><option value="MEDIUM">Trung bình</option><option value="LOW">Thấp</option>
          </select>
          <select aria-label="Lọc trạng thái" value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }} className="portal-field">
            <option value="">Trạng thái: Tất cả</option><option value="OPEN">Đang mở</option>
            <option value="UNDER_REVIEW">Đang điều tra</option><option value="RESOLVED">Đã giải quyết</option>
            <option value="FALSE_POSITIVE">Không phải sự cố</option>
          </select>
        </div>

        <div className="portal-table-card overflow-x-auto">
          {isLoading ? <div className="portal-empty" role="status">Đang tải sự cố...</div>
            : error ? <div className="portal-empty"><p className="text-error">{error}</p><button className="portal-secondary-button mt-3" onClick={() => void fetchData()}>Thử lại</button></div>
            : visibleEvents.length === 0 ? <div className="portal-empty">Không có sự cố phù hợp với bộ lọc hiện tại.</div>
            : <>
              <table className="w-full min-w-[900px]">
                <thead><tr>{['Case ID', 'Mức độ', 'Loại sự cố', 'Trạng thái', 'Phạm vi', 'Thời gian mở', 'Thao tác'].map((h) => <th key={h}>{h}</th>)}</tr></thead>
                <tbody>{visibleEvents.map((event) => {
                  const sev = SEVERITY_BADGE[event.severity] ?? SEVERITY_BADGE.LOW;
                  const state = STATUS_META[event.status] ?? { label: event.status, icon: 'help' };
                  return <tr key={event.id}>
                    <td className="font-semibold text-primary">SEC-{event.id}</td>
                    <td><span className={`inline-flex items-center gap-1 rounded-md px-2.5 py-1 text-xs font-semibold ${sev.cls}`}><span className="material-symbols-outlined text-sm">{sev.icon}</span>{sev.label}</span></td>
                    <td>{humanize(event.eventType)}</td>
                    <td><span className="inline-flex items-center gap-1.5"><span className="material-symbols-outlined text-base">{state.icon}</span>{state.label}</span></td>
                    <td>{event.ipAddress || 'Nội bộ / chưa xác định'}</td>
                    <td>{formatDateTime(event.occurredAt)}</td>
                    <td><button className="font-semibold text-primary hover:underline" onClick={() => openEvent(event)}>{event.status === 'UNDER_REVIEW' ? 'Giải quyết' : 'Mở điều tra'}</button></td>
                  </tr>;
                })}</tbody>
              </table>
              <div className="flex items-center justify-between border-t border-outline-variant px-4 py-3 text-sm text-on-surface-variant">
                <span>{total === 0 ? '0 sự cố' : `Hiển thị ${page * PAGE_SIZE + 1}-${Math.min((page + 1) * PAGE_SIZE, total)} trên ${total} sự cố`}</span>
                <div className="flex gap-2"><button aria-label="Trang trước" onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0} className="portal-secondary-button h-9 px-3">‹</button><button aria-label="Trang sau" onClick={() => setPage((p) => p + 1)} disabled={(page + 1) * PAGE_SIZE >= total} className="portal-secondary-button h-9 px-3">›</button></div>
              </div>
            </>}
        </div>
      </div>
    </div>
  );
}

function humanize(value: string) { return value.split('_').map((part) => part.charAt(0) + part.slice(1).toLowerCase()).join(' '); }
function formatDateTime(iso: string) { return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(iso)); }
