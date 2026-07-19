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
    <div className="portal-page px-5 py-5 md:px-6 md:py-6">
      <div className="portal-contained">
      <div className="portal-header">
        <div>
          <p className="portal-eyebrow">Thông báo</p>
          <h1 className="portal-title">Trung tâm thông báo</h1>
          <p className="portal-subtitle">
            Quản lý và theo dõi các cảnh báo hệ thống và cập nhật trạng thái.
          </p>
        </div>
        <button
          onClick={handleMarkAllRead}
          className="portal-secondary-button"
        >
          <span className="material-symbols-outlined text-lg">done_all</span>
          Đánh dấu tất cả đã đọc
        </button>
      </div>

      <div className="portal-table-card">
        <div className="portal-toolbar border-b border-outline-variant p-4">
          <div className="flex flex-wrap gap-2">
            {FILTERS.map((f, i) => (
              <button
                key={f}
                onClick={() => setActiveFilter(i)}
                className={`flex items-center gap-1.5 rounded-md px-3 py-2 text-sm font-semibold transition-colors ${
                  activeFilter === i
                    ? 'border border-transparent bg-primary text-on-primary'
                    : 'border border-outline-variant bg-surface text-on-surface-variant'
                }`}
              >
                {f}
                {i === 1 && unreadCount > 0 && (
                  <span
                    className={`rounded-md px-2 py-0.5 text-xs font-bold ${
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
              className="portal-field"
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
          <div className="portal-empty">Đang tải...</div>
        ) : error ? (
          <div className="portal-error m-4">{error}</div>
        ) : filteredNotifications.length === 0 ? (
          <div className="portal-empty">Không có thông báo nào.</div>
        ) : (
          <>
            <table className="w-full">
              <thead>
                <tr>
                  <th>Thông báo</th>
                  <th>Thời gian</th>
                  <th>Tài nguyên liên quan</th>
                  <th className="text-center">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {filteredNotifications.map((n) => (
                  <NotificationRow key={n.id} notification={n} />
                ))}
              </tbody>
            </table>
            <div className="flex items-center justify-between border-t border-outline-variant px-4 py-3 text-sm text-on-surface-variant">
              <span>
                Hiển thị {page * pageSize + 1}-{Math.min((page + 1) * pageSize, totalElements)} của{' '}
                {totalElements} thông báo
              </span>
              <div className="flex gap-2">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className={`flex h-8 w-8 items-center justify-center rounded-md border border-outline-variant ${
                    page === 0 ? 'bg-surface-container-low text-outline-variant cursor-default' : 'bg-surface text-on-surface-variant cursor-pointer'
                  }`}
                >
                  <span className="material-symbols-outlined text-lg">chevron_left</span>
                </button>
                <button
                  onClick={() => setPage((p) => p + 1)}
                  disabled={page >= totalPages - 1}
                  className={`flex h-8 w-8 items-center justify-center rounded-md border border-outline-variant ${
                    page >= totalPages - 1 ? 'bg-surface-container-low text-outline-variant cursor-default' : 'bg-surface text-on-surface-variant cursor-pointer'
                  }`}
                >
                  <span className="material-symbols-outlined text-lg">chevron_right</span>
                </button>
              </div>
            </div>
          </>
        )}
      </div>
      </div>
    </div>
  );
}

function NotificationRow({ notification: n }: { notification: NotificationRecord }) {
  const isUnread = n.status !== 'READ';
  const typeLabel = getTypeLabel(n.referenceType || n.type);
  const badgeCls = getTypeBadgeClass(n.referenceType || n.type);

  return (
    <tr className="cursor-pointer">
      <td className="flex items-start gap-3">
        {isUnread && (
          <span className="mt-2 h-2 w-2 shrink-0 rounded-full bg-error" />
        )}
        <span
          className="material-symbols-outlined portal-icon"
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
      <td className="whitespace-nowrap text-on-surface-variant">
        {formatTime(n.createdAt)}
      </td>
      <td>
        <span className={`inline-flex items-center gap-1 rounded-md border px-2.5 py-1 text-xs font-medium ${badgeCls}`}>
          <span className="material-symbols-outlined text-sm">
            {getTypeIcon(n.referenceType || n.type)}
          </span>
          {typeLabel}
        </span>
      </td>
      <td className="text-center">
        <button className="bg-transparent border-none cursor-pointer text-primary">
          <span className="material-symbols-outlined text-xl">open_in_new</span>
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
