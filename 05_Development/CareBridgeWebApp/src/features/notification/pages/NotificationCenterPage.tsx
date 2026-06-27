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
    <div style={{ fontFamily: "'Lexend', sans-serif" }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-start',
          marginBottom: 24,
        }}
      >
        <div>
          <h1
            style={{
              fontSize: 28,
              fontWeight: 700,
              color: '#271812',
              margin: 0,
            }}
          >
            Trung tâm thông báo
          </h1>
          <p style={{ color: '#524440', fontSize: 14, marginTop: 4 }}>
            Quản lý và theo dõi các cảnh báo hệ thống và cập nhật trạng thái.
          </p>
        </div>
        <button
          onClick={handleMarkAllRead}
          style={{
            background: 'none',
            border: 'none',
            color: '#845143',
            fontSize: 14,
            fontWeight: 600,
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: 4,
          }}
        >
          <span className="material-symbols-outlined" style={{ fontSize: 18 }}>
            done_all
          </span>
          Đánh dấu tất cả đã đọc
        </button>
      </div>

      <div
        style={{
          background: '#fff',
          borderRadius: 16,
          padding: 24,
          boxShadow: '0 4px 20px rgba(90,70,63,0.06)',
        }}
      >
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: 20,
          }}
        >
          <div style={{ display: 'flex', gap: 8 }}>
            {FILTERS.map((f, i) => (
              <button
                key={f}
                onClick={() => setActiveFilter(i)}
                style={{
                  padding: '8px 20px',
                  borderRadius: 9999,
                  border:
                    activeFilter === i
                      ? 'none'
                      : '1px solid #d6c2bd',
                  background: activeFilter === i ? '#845143' : '#fff',
                  color: activeFilter === i ? '#fff' : '#524440',
                  fontSize: 14,
                  fontWeight: 600,
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 6,
                }}
              >
                {f}
                {i === 1 && unreadCount > 0 && (
                  <span
                    style={{
                      background: activeFilter === 1 ? '#fff' : '#845143',
                      color: activeFilter === 1 ? '#845143' : '#fff',
                      borderRadius: 9999,
                      padding: '2px 8px',
                      fontSize: 12,
                      fontWeight: 700,
                    }}
                  >
                    {unreadCount}
                  </span>
                )}
              </button>
            ))}
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span style={{ fontSize: 14, color: '#524440' }}>Loại:</span>
            <select
              value={typeFilter}
              onChange={(e) => {
                setTypeFilter(e.target.value);
                setPage(0);
              }}
              style={{
                padding: '8px 16px',
                borderRadius: 8,
                border: '1px solid #d6c2bd',
                fontSize: 14,
                fontFamily: "'Lexend', sans-serif",
                color: '#271812',
                background: '#fff',
                cursor: 'pointer',
              }}
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
          <div style={{ textAlign: 'center', padding: 48, color: '#84736f' }}>
            Đang tải...
          </div>
        ) : error ? (
          <div style={{ textAlign: 'center', padding: 48, color: '#ba1a1a' }}>
            {error}
          </div>
        ) : filteredNotifications.length === 0 ? (
          <div style={{ textAlign: 'center', padding: 48, color: '#84736f' }}>
            Không có thông báo nào.
          </div>
        ) : (
          <>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr
                  style={{
                    borderBottom: '1px solid #fadcd3',
                    textAlign: 'left',
                  }}
                >
                  <th style={thStyle}>Thông báo</th>
                  <th style={thStyle}>Thời gian</th>
                  <th style={thStyle}>Tài nguyên liên quan</th>
                  <th style={{ ...thStyle, textAlign: 'center' }}>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {filteredNotifications.map((n) => (
                  <NotificationRow key={n.id} notification={n} />
                ))}
              </tbody>
            </table>
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginTop: 16,
                fontSize: 14,
                color: '#524440',
              }}
            >
              <span>
                Hiển thị {page * pageSize + 1}-
                {Math.min((page + 1) * pageSize, totalElements)} của{' '}
                {totalElements} thông báo
              </span>
              <div style={{ display: 'flex', gap: 8 }}>
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  style={paginationBtn(page === 0)}
                >
                  <span className="material-symbols-outlined" style={{ fontSize: 18 }}>
                    chevron_left
                  </span>
                </button>
                <button
                  onClick={() => setPage((p) => p + 1)}
                  disabled={page >= totalPages - 1}
                  style={paginationBtn(page >= totalPages - 1)}
                >
                  <span className="material-symbols-outlined" style={{ fontSize: 18 }}>
                    chevron_right
                  </span>
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
  const typeColor = getTypeColor(n.referenceType || n.type);

  return (
    <tr
      style={{
        borderBottom: '1px solid #fadcd3',
        cursor: 'pointer',
      }}
    >
      <td style={{ padding: '16px 8px', display: 'flex', alignItems: 'start', gap: 12 }}>
        {isUnread && (
          <span
            style={{
              width: 8,
              height: 8,
              borderRadius: '50%',
              background: '#ba1a1a',
              marginTop: 8,
              flexShrink: 0,
            }}
          />
        )}
        <span
          className="material-symbols-outlined"
          style={{
            fontSize: 20,
            color: '#845143',
            background: '#ffe2d9',
            borderRadius: '50%',
            width: 36,
            height: 36,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
        >
          {getTypeIcon(n.referenceType || n.type)}
        </span>
        <div>
          <div
            style={{
              fontWeight: isUnread ? 700 : 400,
              fontSize: 14,
              color: '#271812',
            }}
          >
            {n.title}
          </div>
          <div
            style={{
              fontSize: 13,
              color: '#524440',
              marginTop: 2,
              maxWidth: 400,
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
            }}
          >
            {n.body}
          </div>
        </div>
      </td>
      <td style={{ padding: '16px 8px', fontSize: 14, color: '#524440', whiteSpace: 'nowrap' }}>
        {formatTime(n.createdAt)}
      </td>
      <td style={{ padding: '16px 8px' }}>
        <span
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: 4,
            padding: '4px 12px',
            borderRadius: 9999,
            border: `1px solid ${typeColor}40`,
            color: typeColor,
            fontSize: 12,
            fontWeight: 500,
          }}
        >
          <span className="material-symbols-outlined" style={{ fontSize: 14 }}>
            {getTypeIcon(n.referenceType || n.type)}
          </span>
          {typeLabel}
        </span>
      </td>
      <td style={{ padding: '16px 8px', textAlign: 'center' }}>
        <button
          style={{
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            color: '#845143',
          }}
        >
          <span className="material-symbols-outlined" style={{ fontSize: 20 }}>
            open_in_new
          </span>
        </button>
      </td>
    </tr>
  );
}

const thStyle: React.CSSProperties = {
  padding: '12px 8px',
  fontSize: 13,
  fontWeight: 600,
  color: '#84736f',
  textTransform: 'uppercase',
  letterSpacing: '0.05em',
};

function paginationBtn(disabled: boolean): React.CSSProperties {
  return {
    width: 36,
    height: 36,
    borderRadius: 8,
    border: '1px solid #d6c2bd',
    background: disabled ? '#f6f1ec' : '#fff',
    color: disabled ? '#d6c2bd' : '#524440',
    cursor: disabled ? 'default' : 'pointer',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  };
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

function getTypeColor(type: string): string {
  switch (type?.toUpperCase()) {
    case 'SECURITY': return '#ba1a1a';
    case 'PARTNER': return '#845143';
    case 'AUDIT': return '#6e5a52';
    default: return '#84736f';
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
