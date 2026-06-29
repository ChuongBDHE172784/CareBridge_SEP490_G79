import { useEffect, useState, useCallback } from 'react';
import apiClient from '../../../shared/api/apiClient';

interface NotificationRecord {
  id: string;
  userId: string;
  type: string;
  title: string;
  body: string;
  referenceId?: string;
  referenceType?: string;
  status: string;
  createdAt: string;
  sentAt?: string;
}

interface PageResponse {
  content: NotificationRecord[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

const FILTERS = ['Tất cả', 'Chưa đọc'] as const;
const TYPE_OPTIONS = [
  { label: 'Tất cả', value: '' },
  { label: 'Bảo mật', value: 'SECURITY' },
  { label: 'Đối tác', value: 'PARTNER' },
  { label: 'Audit', value: 'AUDIT' },
  { label: 'Hệ thống', value: 'SYSTEM' },
];

export default function NotificationCenterPage() {
  const [notifications, setNotifications] = useState<NotificationRecord[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [activeFilter, setActiveFilter] = useState(0);
  const [typeFilter, setTypeFilter] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchNotifications = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({
        page: String(page),
        size: String(pageSize),
      });
      if (typeFilter) params.set('type', typeFilter);
      const res = await apiClient.get(`/api/v1/notifications/me?${params}`);
      const data: PageResponse = res.data.data;
      setNotifications(data.content);
      setTotalElements(data.totalElements);
      setTotalPages(data.totalPages);
    } catch {
      setError('Không thể tải thông báo.');
    } finally {
      setIsLoading(false);
    }
  }, [page, pageSize, typeFilter]);

  useEffect(() => {
    fetchNotifications();
  }, [fetchNotifications]);

  const handleMarkAllRead = async () => {
    // TODO: wire to backend when mark-all-as-read endpoint is implemented
    fetchNotifications();
  };

  const unreadCount = notifications.filter((n) => n.status !== 'READ').length;

  const filteredNotifications =
    activeFilter === 1
      ? notifications.filter((n) => n.status !== 'READ')
      : notifications;

  return (
    <div>
      <div className="flex justify-between items-start mb-6">
        <div>
          <h1 className="text-[28px] font-bold text-on-surface m-0">Trung tâm thông báo</h1>
          <p className="text-on-surface-variant text-sm mt-1">
            Quản lý và theo dõi các cảnh báo hệ thống và cập nhật trạng thái.
          </p>
        </div>
        <button
          onClick={handleMarkAllRead}
          className="flex items-center gap-1 bg-transparent border-none text-primary text-sm font-semibold cursor-pointer"
        >
          <span className="material-symbols-outlined" style={{ fontSize: 18 }}>done_all</span>
          Đánh dấu tất cả đã đọc
        </button>
      </div>

      <div className="bg-surface rounded-2xl p-6 shadow-sm">
        <div className="flex justify-between items-center mb-5">
          <div className="flex gap-2">
            {FILTERS.map((f, i) => (
              <button
                key={f}
                onClick={() => setActiveFilter(i)}
                className={`flex items-center gap-1.5 px-5 py-2 rounded-full text-sm font-semibold cursor-pointer transition-colors ${
                  activeFilter === i
                    ? 'bg-primary text-on-primary border-none'
                    : 'bg-surface text-on-surface-variant border border-outline-variant'
                }`}
              >
                {f}
                {i === 1 && unreadCount > 0 && (
                  <span
                    className={`rounded-full px-2 py-0.5 text-xs font-bold ${
                      activeFilter === 1 ? 'bg-surface text-primary' : 'bg-primary text-on-primary'
                    }`}
                  >
                    {unreadCount}
                  </span>
                )}
              </button>
            ))}
          </div>
          <div className="flex items-center gap-2">
            <span className="text-sm text-on-surface-variant">Loại:</span>
            <select
              value={typeFilter}
              onChange={(e) => {
                setTypeFilter(e.target.value);
                setPage(0);
              }}
              className="px-4 py-2 rounded-lg border border-outline-variant text-sm text-on-surface bg-surface cursor-pointer"
            >
              {TYPE_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        {isLoading ? (
          <div className="text-center p-12 text-outline">Đang tải...</div>
        ) : error ? (
          <div className="text-center p-12 text-error">{error}</div>
        ) : filteredNotifications.length === 0 ? (
          <div className="text-center p-12 text-outline">Không có thông báo nào.</div>
        ) : (
          <>
            <table className="w-full border-collapse">
              <thead>
                <tr className="border-b border-surface-container-highest text-left">
                  <th className="px-2 py-3 text-[13px] font-semibold text-outline uppercase tracking-[0.05em]">Thông báo</th>
                  <th className="px-2 py-3 text-[13px] font-semibold text-outline uppercase tracking-[0.05em]">Thời gian</th>
                  <th className="px-2 py-3 text-[13px] font-semibold text-outline uppercase tracking-[0.05em]">Tài nguyên liên quan</th>
                  <th className="px-2 py-3 text-[13px] font-semibold text-outline uppercase tracking-[0.05em] text-center">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {filteredNotifications.map((n) => (
                  <NotificationRow key={n.id} notification={n} />
                ))}
              </tbody>
            </table>
            <div className="flex justify-between items-center mt-4 text-sm text-on-surface-variant">
              <span>
                Hiển thị {page * pageSize + 1}-{Math.min((page + 1) * pageSize, totalElements)} của{' '}
                {totalElements} thông báo
              </span>
              <div className="flex gap-2">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className={`w-9 h-9 rounded-lg border border-outline-variant flex items-center justify-center ${
                    page === 0 ? 'bg-surface-container-low text-outline-variant cursor-default' : 'bg-surface text-on-surface-variant cursor-pointer'
                  }`}
                >
                  <span className="material-symbols-outlined" style={{ fontSize: 18 }}>chevron_left</span>
                </button>
                <button
                  onClick={() => setPage((p) => p + 1)}
                  disabled={page >= totalPages - 1}
                  className={`w-9 h-9 rounded-lg border border-outline-variant flex items-center justify-center ${
                    page >= totalPages - 1 ? 'bg-surface-container-low text-outline-variant cursor-default' : 'bg-surface text-on-surface-variant cursor-pointer'
                  }`}
                >
                  <span className="material-symbols-outlined" style={{ fontSize: 18 }}>chevron_right</span>
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

function NotificationRow({ notification: n }: { notification: NotificationRecord }) {
  const isUnread = n.status !== 'READ';
  const typeLabel = getTypeLabel(n.referenceType || n.type);
  const badgeCls = getTypeBadgeClass(n.referenceType || n.type);

  return (
    <tr className="border-b border-surface-container-highest cursor-pointer">
      <td className="px-2 py-4 flex items-start gap-3">
        {isUnread && (
          <span className="w-2 h-2 rounded-full bg-error mt-2 shrink-0" />
        )}
        <span
          className="material-symbols-outlined text-primary bg-surface-container-high rounded-full w-9 h-9 flex items-center justify-center shrink-0"
          style={{ fontSize: 20 }}
        >
          {getTypeIcon(n.referenceType || n.type)}
        </span>
        <div>
          <div className={`text-sm text-on-surface ${isUnread ? 'font-bold' : 'font-normal'}`}>
            {n.title}
          </div>
          <div className="text-[13px] text-on-surface-variant mt-0.5 max-w-[400px] truncate">
            {n.body}
          </div>
        </div>
      </td>
      <td className="px-2 py-4 text-sm text-on-surface-variant whitespace-nowrap">
        {formatTime(n.createdAt)}
      </td>
      <td className="px-2 py-4">
        <span className={`inline-flex items-center gap-1 px-3 py-1 rounded-full border text-xs font-medium ${badgeCls}`}>
          <span className="material-symbols-outlined" style={{ fontSize: 14 }}>
            {getTypeIcon(n.referenceType || n.type)}
          </span>
          {typeLabel}
        </span>
      </td>
      <td className="px-2 py-4 text-center">
        <button className="bg-transparent border-none cursor-pointer text-primary">
          <span className="material-symbols-outlined" style={{ fontSize: 20 }}>open_in_new</span>
        </button>
      </td>
    </tr>
  );
}

function getTypeLabel(type: string): string {
  switch (type?.toUpperCase()) {
    case 'SECURITY': return 'Bảo mật';
    case 'PARTNER': return 'Đối tác';
    case 'AUDIT': return 'Audit';
    case 'SYSTEM': return 'Hệ thống';
    default: return type || 'Chung';
  }
}

function getTypeBadgeClass(type: string): string {
  switch (type?.toUpperCase()) {
    case 'SECURITY': return 'text-error border-error/30';
    case 'PARTNER': return 'text-primary border-primary/30';
    case 'AUDIT': return 'text-on-surface-variant border-outline-variant';
    default: return 'text-outline border-outline-variant';
  }
}

function getTypeIcon(type: string): string {
  switch (type?.toUpperCase()) {
    case 'SECURITY': return 'security';
    case 'PARTNER': return 'handshake';
    case 'AUDIT': return 'schedule';
    default: return 'info';
  }
}

function formatTime(iso: string): string {
  const date = new Date(iso);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMin = Math.floor(diffMs / 60000);
  if (diffMin < 1) return 'Vừa xong';
  if (diffMin < 60) return `${diffMin} phút trước`;
  const diffHours = Math.floor(diffMin / 60);
  if (diffHours < 24) return `${diffHours} giờ trước`;
  const diffDays = Math.floor(diffHours / 24);
  if (diffDays === 1) return 'Hôm qua';
  return `Hôm qua, ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
}
