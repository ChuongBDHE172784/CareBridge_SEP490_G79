import { useState, useEffect } from 'react';
import apiClient from '../../../shared/api/apiClient';

interface ModerationItem {
  id: number;
  reportedContentType: 'QUESTION' | 'ANSWER' | 'CONTENT';
  contentPreview: string;
  reportCount: number;
  reason: string;
  reportedAt: string;
}

interface PageResponse {
  content: ModerationItem[];
  totalPages: number;
  number: number;
}

const styles = {
  page: { minHeight: '100vh', background: '#F6F1EC', padding: '32px 24px', fontFamily: '"Nunito", system-ui, sans-serif' } as React.CSSProperties,
  header: { fontSize: 24, fontWeight: 900, color: '#5A463F', marginBottom: 24 } as React.CSSProperties,
  card: { background: '#FFFFFF', border: '1px solid rgba(232,221,214,0.5)', borderRadius: 32, boxShadow: '0 12px 32px rgba(90,70,63,0.06)', padding: 24, marginBottom: 16 } as React.CSSProperties,
  filterRow: { display: 'flex', gap: 12, marginBottom: 24, alignItems: 'center' } as React.CSSProperties,
  select: { padding: '10px 16px', borderRadius: 20, border: '2px solid rgba(232,221,214,0.6)', background: '#F2EAE4', fontSize: 14, color: '#5A463F', outline: 'none', cursor: 'pointer' } as React.CSSProperties,
  label: { fontSize: 11, color: '#9C857C', fontWeight: 700, marginRight: 8, textTransform: 'uppercase' as const, letterSpacing: 1 } as React.CSSProperties,
  badge: (type: string) => ({
    display: 'inline-block', padding: '4px 14px', borderRadius: 9999, fontSize: 12, fontWeight: 700,
    background: type === 'QUESTION' ? 'rgba(201,140,123,0.12)' : type === 'ANSWER' ? 'rgba(90,70,63,0.10)' : 'rgba(201,140,123,0.08)',
    color: type === 'QUESTION' ? '#C98C7B' : type === 'ANSWER' ? '#5A463F' : '#9C857C',
  } as React.CSSProperties),
  itemTitle: { fontSize: 15, fontWeight: 700, color: '#5A463F', marginBottom: 6 } as React.CSSProperties,
  itemMeta: { fontSize: 13, color: '#9C857C' } as React.CSSProperties,
  paginationRow: { display: 'flex', gap: 8, justifyContent: 'center', marginTop: 24, alignItems: 'center' } as React.CSSProperties,
  btn: { padding: '10px 24px', borderRadius: 9999, border: 'none', background: '#C98C7B', color: 'white', fontWeight: 700, cursor: 'pointer', fontSize: 14, boxShadow: '0 8px 24px rgba(201,140,123,0.25)', transition: 'all 0.3s' } as React.CSSProperties,
  btnDisabled: { padding: '10px 24px', borderRadius: 9999, border: 'none', background: '#F2EAE4', color: '#9C857C', fontWeight: 700, cursor: 'not-allowed', fontSize: 14 } as React.CSSProperties,
  empty: { textAlign: 'center' as const, color: '#9C857C', padding: 40, fontSize: 15 },
  error: { background: '#F2EAE4', border: '1px solid rgba(201,140,123,0.3)', borderRadius: 20, padding: '12px 16px', color: '#C98C7B', marginBottom: 16, fontSize: 14, fontWeight: 600 },
};

export default function ModerationQueuePage() {
  const [items, setItems] = useState<ModerationItem[]>([]);
  const [contentType, setContentType] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const fetchQueue = async (p: number, ct: string) => {
    setLoading(true);
    setError('');
    try {
      const params: Record<string, string | number> = { page: p, size: 20 };
      if (ct) params.contentType = ct;
      const { data } = await apiClient.get<PageResponse>('/api/v1/admin/moderation/queue', { params });
      setItems(data.content);
      setTotalPages(data.totalPages);
    } catch {
      setError('Không thể tải hàng đợi kiểm duyệt.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchQueue(page, contentType);
  }, [page, contentType]);

  const handleTypeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setContentType(e.target.value === 'ALL' ? '' : e.target.value);
    setPage(0);
  };

  return (
    <div style={styles.page}>
      <div style={styles.header}>Hàng đợi kiểm duyệt</div>
      {error && <div style={styles.error}>{error}</div>}
      <div style={styles.filterRow}>
        <span style={styles.label}>Loại nội dung:</span>
        <select style={styles.select} onChange={handleTypeChange} defaultValue="ALL">
          <option value="ALL">Tất cả</option>
          <option value="QUESTION">Câu hỏi</option>
          <option value="ANSWER">Câu trả lời</option>
          <option value="CONTENT">Nội dung</option>
        </select>
      </div>
      {loading ? (
        <div style={styles.empty}>Đang tải...</div>
      ) : items.length === 0 ? (
        <div style={styles.empty}>Không có mục nào trong hàng đợi.</div>
      ) : (
        items.map((item) => (
          <div key={item.id} style={styles.card}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 8 }}>
              <span style={styles.badge(item.reportedContentType)}>{item.reportedContentType}</span>
              <span style={{ fontSize: 12, color: '#9C857C' }}>{new Date(item.reportedAt).toLocaleDateString('vi-VN')}</span>
            </div>
            <div style={styles.itemTitle}>{item.contentPreview}</div>
            <div style={styles.itemMeta}>
              <span>Lượt báo cáo: <strong>{item.reportCount}</strong></span>
              <span style={{ marginLeft: 16 }}>Lý do: {item.reason}</span>
            </div>
          </div>
        ))
      )}
      {totalPages > 1 && (
        <div style={styles.paginationRow}>
          <button style={page === 0 ? styles.btnDisabled : styles.btn} onClick={() => setPage(p => p - 1)} disabled={page === 0}>Trước</button>
          <span style={{ lineHeight: '36px', color: '#9C857C', fontSize: 14 }}>Trang {page + 1} / {totalPages}</span>
          <button style={page >= totalPages - 1 ? styles.btnDisabled : styles.btn} onClick={() => setPage(p => p + 1)} disabled={page >= totalPages - 1}>Tiếp theo</button>
        </div>
      )}
    </div>
  );
}
