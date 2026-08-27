import { useEffect, useState, useCallback } from 'react';
import apiClient from '../../../shared/api/apiClient';
import { useNavigate } from 'react-router-dom';

interface NotificationRecord {
  id: string;
  userId: string;
  type: string;
  title: string;
  body: string;
  referenceId?: string;
  referenceType?: string;
  status: string;
  isRead: boolean;
  createdAt: string;
  sentAt?: string;
  metadata?: Record<string, string>;
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
  { label: 'Audit', value: 'AUDIT' },
  { label: 'Hệ thống', value: 'SYSTEM' },
  { label: 'Duyệt nội dung', value: 'CONTENT_REVIEW' },
];

export default function NotificationCenterPage() {
  const navigate = useNavigate();
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
    setError(null);
    try {
      await apiClient.put('/api/v1/notifications/read-all', {});
      await fetchNotifications();
    } catch {
      setError('Không thể đánh dấu tất cả thông báo đã đọc.');
    }
  };

  const openNotification = async (notification: NotificationRecord) => {
    const route = notification.metadata?.route;
    if (!notification.isRead) {
      try {
        await apiClient.put(`/api/v1/notifications/${notification.id}/read`, {});
      } catch {
        setError('Đã mở nội dung nhưng chưa thể đánh dấu thông báo là đã đọc.');
      }
    }
    if (route?.startsWith('/')) {
      navigate(route);
    } else {
      await fetchNotifications();
    }
  };

  const unreadCount = notifications.filter((n) => !n.isRead).length;

  const filteredNotifications =
    activeFilter === 1
      ? notifications.filter((n) => !n.isRead)
      : notifications;

  return (
    <div className="p-8 font-sans">
      {/* Header */}
      <div className="flex justify-between items-start mb-6">
        <div>
          <h1 className="text-[26px] font-bold text-on-surface m-0">Trung tâm thông báo</h1>
          <p className="text-on-surface-variant text-sm mt-1">
            Quản lý và theo dõi các cảnh báo hệ thống và cập nhật trạng thái
          </p>
        </div>
        <button
          onClick={handleMarkAllRead}
          className="flex items-center gap-2 py-3 px-6 rounded-full bg-primary text-on-primary border-0 text-sm font-semibold cursor-pointer whitespace-nowrap shadow-sm hover:opacity-95 transition-opacity"
        >
          <span className="material-symbols-outlined text-lg">done_all</span>
          Đánh dấu tất cả đã đọc
        </button>
      </div>

      {error && (
        <div className="bg-error-container rounded-2xl p-4 mb-4 text-error text-sm">{error}</div>
      )}

      {/* Filter Tabs & Options */}
      <div className="flex justify-between items-center mb-5 flex-wrap gap-3">
        <div className="flex gap-2">
          {FILTERS.map((f, i) => (
            <button
              key={f}
              onClick={() => {
                setActiveFilter(i);
                setPage(0);
              }}
              className={`flex items-center gap-1.5 py-2 px-[18px] rounded-full text-[13px] font-semibold cursor-pointer transition-colors ${
                activeFilter === i
                  ? 'border-2 border-primary bg-surface-container-low text-primary'
                  : 'border border-outline-variant bg-transparent text-on-surface-variant hover:bg-surface-bright'
              }`}
            >
              {f}
              {i === 1 && unreadCount > 0 && (
                <span
                  className={`rounded-full px-2 py-0.5 text-xs font-bold ${
                    activeFilter === 1 ? 'bg-primary text-on-primary' : 'bg-primary/20 text-primary'
                  }`}
                >
                  {unreadCount}
                </span>
              )}
            </button>
          ))}
        </div>
        <div className="flex items-center gap-2">
          <span className="text-sm font-medium text-on-surface-variant">Loại thông báo:</span>
          <select
            value={typeFilter}
            onChange={(e) => {
              setTypeFilter(e.target.value);
              setPage(0);
            }}
            className="py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface-variant cursor-pointer font-sans outline-none"
          >
            {TYPE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Data Card */}
      <div className="bg-surface rounded-2xl p-6 shadow-md">
        {isLoading ? (
          <div className="py-12 text-center text-outline">Đang tải...</div>
        ) : filteredNotifications.length === 0 ? (
          <div className="py-12 text-center text-outline">Không có thông báo nào.</div>
        ) : (
          <>
            <table className="w-full border-collapse">
              <thead>
                <tr className="border-b-2 border-surface-container-highest text-left">
                  <th className="py-3 px-2 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">
                    THÔNG BÁO
                  </th>
                  <th className="py-3 px-2 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">
                    LOẠI THÔNG BÁO
                  </th>
                  <th className="py-3 px-2 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">
                    THỜI GIAN
                  </th>
                  <th className="py-3 px-2 text-[11px] font-semibold text-outline uppercase tracking-[0.05em] text-center">
                    THAO TÁC
                  </th>
                </tr>
              </thead>
              <tbody>
                {filteredNotifications.map((n) => (
                  <NotificationRow key={n.id} notification={n} onOpen={() => openNotification(n)} />
                ))}
              </tbody>
            </table>

            {/* Pagination */}
            <div className="flex justify-between items-center mt-5 pt-4 border-t border-surface-container-highest">
              <span className="text-[13px] text-outline">
                Hiển thị {totalElements === 0 ? 0 : page * pageSize + 1}-
                {Math.min((page + 1) * pageSize, totalElements)} trong {totalElements} kết quả
              </span>
              <div className="flex gap-1">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className={`w-9 h-9 rounded-full border border-outline-variant bg-surface flex items-center justify-center ${
                    page === 0 ? 'opacity-40 cursor-default' : 'cursor-pointer'
                  }`}
                >
                  <span className="material-symbols-outlined text-primary text-lg">chevron_left</span>
                </button>
                {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
                  const startPage = Math.max(0, Math.min(page - 2, totalPages - 5));
                  const p = startPage + i;
                  if (p >= totalPages || p < 0) return null;
                  return (
                    <button
                      key={p}
                      onClick={() => setPage(p)}
                      className={`w-9 h-9 rounded-full text-sm font-semibold cursor-pointer flex items-center justify-center ${
                        page === p
                          ? 'border-0 bg-primary text-on-primary'
                          : 'border border-outline-variant bg-surface text-on-surface-variant hover:bg-surface-bright'
                      }`}
                    >
                      {p + 1}
                    </button>
                  );
                })}
                <button
                  onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                  className={`w-9 h-9 rounded-full border border-outline-variant bg-surface flex items-center justify-center ${
                    page >= totalPages - 1 ? 'opacity-40 cursor-default' : 'cursor-pointer'
                  }`}
                >
                  <span className="material-symbols-outlined text-primary text-lg">chevron_right</span>
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

function NotificationRow({ notification: n, onOpen }: { notification: NotificationRecord; onOpen: () => void }) {
  const isUnread = !n.isRead;
  const typeLabel = getTypeLabel(n.referenceType || n.type);
  const badgeCls = getTypeBadgeClass(n.referenceType || n.type);

  return (
    <tr
      className="border-b border-surface-container-highest hover:bg-surface-bright cursor-pointer transition-colors"
      onClick={onOpen}
    >
      <td className="py-3.5 px-2 max-w-[480px]">
        <div className="flex items-start gap-3">
          {isUnread ? (
            <span className="mt-1.5 h-2.5 w-2.5 shrink-0 rounded-full bg-primary" title="Chưa đọc" />
          ) : (
            <span className="mt-1.5 h-2.5 w-2.5 shrink-0 rounded-full bg-transparent" />
          )}
          <span className="material-symbols-outlined text-primary text-xl mt-0.5 shrink-0">
            {getTypeIcon(n.referenceType || n.type)}
          </span>
          <div>
            <div className={`text-sm text-on-surface ${isUnread ? 'font-bold' : 'font-normal'}`}>
              {n.title}
            </div>
            <div className="text-[13px] text-on-surface-variant mt-0.5 line-clamp-2">
              {n.body}
            </div>
          </div>
        </div>
      </td>
      <td className="py-3.5 px-2 whitespace-nowrap">
        <span className={`py-1 px-3.5 rounded-full text-xs font-semibold inline-flex items-center gap-1.5 ${badgeCls}`}>
          <span className="material-symbols-outlined text-sm">
            {getTypeIcon(n.referenceType || n.type)}
          </span>
          {typeLabel}
        </span>
      </td>
      <td className="py-3.5 px-2 text-[13px] text-outline whitespace-nowrap">
        {formatTime(n.createdAt)}
      </td>
      <td className="py-3.5 px-2 text-center">
        <button
          type="button"
          onClick={(event) => {
            event.stopPropagation();
            onOpen();
          }}
          className="w-8 h-8 rounded-lg border border-outline-variant bg-transparent cursor-pointer inline-flex items-center justify-center text-primary hover:bg-surface-container-low transition-colors"
          title="Mở nội dung"
        >
          <span className="material-symbols-outlined text-lg">open_in_new</span>
        </button>
      </td>
    </tr>
  );
}

// The Partner programme was retired, so nothing emits PARTNER notifications any
// more; the PARTNER arms below stay so historical rows still render with a label
// instead of falling through to the generic default.
function getTypeLabel(type: string): string {
  switch (type?.toUpperCase()) {
    case 'SECURITY':
      return 'Bảo mật';
    case 'PARTNER':
      return 'Đối tác';
    case 'AUDIT':
      return 'Audit';
    case 'SYSTEM':
      return 'Hệ thống';
    case 'ARTICLE':
      return 'Bài viết cần sửa';
    case 'FAQ':
      return 'FAQ cần sửa';
    case 'CHECKLIST':
      return 'Checklist cần sửa';
    case 'CONTENT_REVIEW':
      return 'Duyệt nội dung';
    default:
      return type || 'Chung';
  }
}

function getTypeBadgeClass(type: string): string {
  switch (type?.toUpperCase()) {
    case 'SECURITY':
      return 'bg-error-container text-error';
    case 'PARTNER':
      return 'bg-[#E3F2FD] text-[#1565C0]';
    case 'AUDIT':
      return 'bg-[#F5F5F5] text-[#616161]';
    case 'ARTICLE':
    case 'FAQ':
    case 'CHECKLIST':
    case 'CONTENT_REVIEW':
      return 'bg-[#FFF3E0] text-[#E65100]';
    default:
      return 'bg-surface-container-highest text-on-surface-variant';
  }
}

function getTypeIcon(type: string): string {
  switch (type?.toUpperCase()) {
    case 'SECURITY':
      return 'security';
    case 'PARTNER':
      return 'handshake';
    case 'AUDIT':
      return 'schedule';
    case 'ARTICLE':
      return 'article';
    case 'FAQ':
      return 'quiz';
    case 'CHECKLIST':
      return 'checklist';
    case 'CONTENT_REVIEW':
      return 'assignment_return';
    default:
      return 'info';
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
  return `${date.getDate().toString().padStart(2, '0')}/${(date.getMonth() + 1).toString().padStart(2, '0')}/${date.getFullYear()} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
}

