import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchContentList, searchContent } from '../services/contentApi';
import type { ContentListItem, ContentSearchItem, ContentType } from '../models/content';
import { TYPE_LABELS, STAGE_LABELS } from '../models/content';

/* ------------------------------------------------------------------ */
/*  Mock data fallback                                                 */
/* ------------------------------------------------------------------ */
const MOCK_ITEMS: ContentListItem[] = [
  { id: '1', type: 'ARTICLE', title: 'Dinh duong trong 3 thang dau thai ky', stage: 'PREGNANCY', topicId: 't1', publishedAt: '2026-06-25T10:00:00Z' },
  { id: '2', type: 'FAQ', title: 'Khi nao can gap bac si san khoa?', stage: 'PREGNANCY', topicId: 't2', publishedAt: null },
  { id: '3', type: 'CHECKLIST', title: 'Checklist chuan bi do so sinh', stage: 'POSTPARTUM', topicId: 't3', publishedAt: '2026-06-20T08:30:00Z' },
  { id: '4', type: 'ARTICLE', title: 'Cac bai tap an toan cho ba bau', stage: 'PREGNANCY', topicId: 't1', publishedAt: '2026-06-18T14:00:00Z' },
  { id: '5', type: 'ARTICLE', title: 'Tam ly sau sinh va cach vuot qua', stage: 'POSTPARTUM', topicId: 't4', publishedAt: null },
  { id: '6', type: 'FAQ', title: 'Bo sung sat nhu the nao trong thai ky?', stage: 'PREGNANCY', topicId: 't2', publishedAt: '2026-06-22T09:15:00Z' },
  { id: '7', type: 'CHECKLIST', title: 'Danh sach xet nghiem can lam', stage: 'PRE_PREGNANCY', topicId: 't5', publishedAt: '2026-06-15T07:00:00Z' },
  { id: '8', type: 'ARTICLE', title: 'Huong dan tam be so sinh dung cach', stage: 'BABY_CARE', topicId: 't6', publishedAt: '2026-06-24T11:30:00Z' },
  { id: '9', type: 'FAQ', title: 'Tre so sinh can nang bao nhieu la binh thuong?', stage: 'BABY_CARE', topicId: 't6', publishedAt: null },
  { id: '10', type: 'ARTICLE', title: 'Cham soc vet mo sau sinh', stage: 'POSTPARTUM', topicId: 't4', publishedAt: '2026-06-21T16:45:00Z' },
];

/* ------------------------------------------------------------------ */
/*  Helpers                                                            */
/* ------------------------------------------------------------------ */
function statusBadge(publishedAt: string | null): { label: string; bg: string; color: string } {
  if (publishedAt) return { label: 'Da xuat ban', bg: '#E6F4EA', color: '#137333' };
  return { label: 'Nhap', bg: '#F5F5F5', color: '#616161' };
}

function typeIcon(type: ContentType): string {
  if (type === 'ARTICLE') return 'article';
  if (type === 'FAQ') return 'quiz';
  return 'fact_check';
}

function timeAgo(iso: string | null): string {
  if (!iso) return '—';
  const diff = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 60) return `${mins} phut truoc`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours} gio truoc`;
  const days = Math.floor(hours / 24);
  return `${days} ngay truoc`;
}

/* ------------------------------------------------------------------ */
/*  Page Component                                                     */
/* ------------------------------------------------------------------ */
export default function ContentListPage() {
  const navigate = useNavigate();
  const [items, setItems] = useState<(ContentListItem | ContentSearchItem)[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [typeFilter, setTypeFilter] = useState<ContentType | ''>('');
  const [keyword, setKeyword] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  const loadData = useCallback(async () => {
    setIsLoading(true);
    try {
      if (keyword) {
        const data = await searchContent({
          keyword,
          type: typeFilter || undefined,
          page,
          size: pageSize,
        });
        setItems(data.content);
        setTotal(data.totalElements);
      } else {
        const data = await fetchContentList({
          type: typeFilter || undefined,
          page,
          size: pageSize,
        });
        setItems(data.content);
        setTotal(data.totalElements);
      }
    } catch {
      // Fallback to mock data
      let filtered = MOCK_ITEMS;
      if (typeFilter) filtered = filtered.filter(i => i.type === typeFilter);
      if (keyword) filtered = filtered.filter(i => i.title.toLowerCase().includes(keyword.toLowerCase()));
      setItems(filtered.slice(page * pageSize, (page + 1) * pageSize));
      setTotal(filtered.length);
    } finally {
      setIsLoading(false);
    }
  }, [keyword, typeFilter, page, pageSize]);

  useEffect(() => { loadData(); }, [loadData]);

  const totalPages = Math.ceil(total / pageSize);

  const handleSearch = () => {
    setKeyword(searchInput);
    setPage(0);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleSearch();
  };

  return (
    <div style={{ padding: 32, fontFamily: "'Lexend', sans-serif" }}>
      {/* Header */}
      <div style={{ marginBottom: 24 }}>
        <h1 style={{ fontSize: 26, fontWeight: 700, color: '#271812', margin: 0 }}>Danh sach noi dung</h1>
        <p style={{ color: '#524440', fontSize: 14, marginTop: 4 }}>Quan ly va duyet cac bai viet, tai lieu cho thu vien</p>
      </div>

      {/* Action bar */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 20 }}>
        <div style={{ flex: 1, position: 'relative' }}>
          <span className="material-symbols-outlined" style={{ position: 'absolute', left: 14, top: '50%', transform: 'translateY(-50%)', fontSize: 20, color: '#84736f' }}>search</span>
          <input
            value={searchInput}
            onChange={e => setSearchInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Tim kiem theo tieu de, tac gia..."
            style={{ width: '100%', padding: '12px 14px 12px 42px', borderRadius: 16, border: '1px solid #d6c2bd', background: '#fff', fontSize: 14, color: '#271812', outline: 'none', fontFamily: "'Lexend', sans-serif" }}
          />
        </div>

        {/* Type filter dropdown */}
        <select
          value={typeFilter}
          onChange={e => { setTypeFilter(e.target.value as ContentType | ''); setPage(0); }}
          style={{ padding: '12px 16px', borderRadius: 16, border: '1px solid #d6c2bd', background: '#fff', fontSize: 14, color: '#524440', cursor: 'pointer', fontFamily: "'Lexend', sans-serif" }}
        >
          <option value="">Bo loc</option>
          <option value="ARTICLE">Bai viet</option>
          <option value="FAQ">FAQ</option>
          <option value="CHECKLIST">Checklist</option>
        </select>

        <button
          onClick={() => navigate('/content/create')}
          style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '12px 24px', borderRadius: 9999, background: '#C98C7B', color: '#fff', border: 'none', fontSize: 14, fontWeight: 600, cursor: 'pointer', whiteSpace: 'nowrap' }}
        >
          <span className="material-symbols-outlined" style={{ fontSize: 18 }}>add</span>
          Tao noi dung moi
        </button>
      </div>

      {/* Data table */}
      <div style={{ background: '#fff', borderRadius: 16, padding: 24, boxShadow: '0 4px 20px rgba(90,70,63,0.06)' }}>
        {isLoading ? (
          <div style={{ padding: 48, textAlign: 'center', color: '#84736f' }}>Dang tai...</div>
        ) : (
          <>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ borderBottom: '2px solid #fadcd3', textAlign: 'left' }}>
                  {['TIEU DE', 'PHAN LOAI', 'TRANG THAI', 'PHIEN BAN', 'CAP NHAT BOI', 'THAO TAC'].map(h => (
                    <th key={h} style={{ padding: '12px 8px', fontSize: 11, fontWeight: 600, color: '#84736f', textTransform: 'uppercase' as const, letterSpacing: '0.05em' }}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {items.map(item => {
                  const status = statusBadge(item.publishedAt);
                  return (
                    <tr
                      key={item.id}
                      style={{ borderBottom: '1px solid #fadcd3' }}
                      onMouseEnter={e => { (e.currentTarget as HTMLElement).style.background = '#FFF8F6'; }}
                      onMouseLeave={e => { (e.currentTarget as HTMLElement).style.background = 'transparent'; }}
                    >
                      <td style={{ padding: '14px 8px', maxWidth: 320 }}>
                        <div style={{ fontWeight: 600, fontSize: 14, color: '#271812' }}>{item.title}</div>
                        <div style={{ fontSize: 12, color: '#84736f', marginTop: 2 }}>Cap nhat: {timeAgo(item.publishedAt)}</div>
                      </td>
                      <td style={{ padding: '14px 8px' }}>
                        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4, padding: '4px 12px', borderRadius: 9999, background: '#FFF1EC', color: '#845143', fontSize: 12, fontWeight: 600 }}>
                          <span className="material-symbols-outlined" style={{ fontSize: 14 }}>{typeIcon(item.type)}</span>
                          {TYPE_LABELS[item.type]}
                        </span>
                      </td>
                      <td style={{ padding: '14px 8px' }}>
                        <span style={{ padding: '4px 14px', borderRadius: 9999, background: status.bg, color: status.color, fontSize: 12, fontWeight: 600 }}>
                          {status.label}
                        </span>
                      </td>
                      <td style={{ padding: '14px 8px', fontSize: 13, color: '#524440' }}>v1</td>
                      <td style={{ padding: '14px 8px', fontSize: 13, color: '#524440' }}>Content Admin</td>
                      <td style={{ padding: '14px 8px' }}>
                        <div style={{ display: 'flex', gap: 4 }}>
                          <button
                            onClick={() => navigate(`/content/${item.id}`)}
                            style={{ width: 32, height: 32, borderRadius: 8, border: '1px solid #d6c2bd', background: 'transparent', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                            title="Xem chi tiet"
                          >
                            <span className="material-symbols-outlined" style={{ fontSize: 16, color: '#845143' }}>visibility</span>
                          </button>
                          <button
                            onClick={() => navigate(`/content/${item.id}/edit`)}
                            style={{ width: 32, height: 32, borderRadius: 8, border: '1px solid #d6c2bd', background: 'transparent', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                            title="Chinh sua"
                          >
                            <span className="material-symbols-outlined" style={{ fontSize: 16, color: '#845143' }}>edit</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
                {items.length === 0 && (
                  <tr><td colSpan={6} style={{ padding: 48, textAlign: 'center', color: '#84736f' }}>Khong tim thay noi dung nao.</td></tr>
                )}
              </tbody>
            </table>

            {/* Pagination */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 20, paddingTop: 16, borderTop: '1px solid #fadcd3' }}>
              <span style={{ fontSize: 13, color: '#84736f' }}>
                Hien thi {total === 0 ? 0 : page * pageSize + 1}-{Math.min((page + 1) * pageSize, total)} trong {total} ket qua
              </span>
              <div style={{ display: 'flex', gap: 4 }}>
                <button
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0}
                  style={{ width: 36, height: 36, borderRadius: 9999, border: '1px solid #d6c2bd', background: '#fff', cursor: page === 0 ? 'default' : 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', opacity: page === 0 ? 0.4 : 1 }}
                >
                  <span className="material-symbols-outlined" style={{ fontSize: 18, color: '#845143' }}>chevron_left</span>
                </button>
                {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
                  const startPage = Math.max(0, Math.min(page - 2, totalPages - 5));
                  const p = startPage + i;
                  if (p >= totalPages) return null;
                  return (
                    <button
                      key={p}
                      onClick={() => setPage(p)}
                      style={{ width: 36, height: 36, borderRadius: 9999, border: page === p ? 'none' : '1px solid #d6c2bd', background: page === p ? '#C98C7B' : '#fff', color: page === p ? '#fff' : '#524440', fontSize: 14, fontWeight: 600, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                    >
                      {p + 1}
                    </button>
                  );
                })}
                <button
                  onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                  style={{ width: 36, height: 36, borderRadius: 9999, border: '1px solid #d6c2bd', background: '#fff', cursor: page >= totalPages - 1 ? 'default' : 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', opacity: page >= totalPages - 1 ? 0.4 : 1 }}
                >
                  <span className="material-symbols-outlined" style={{ fontSize: 18, color: '#845143' }}>chevron_right</span>
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
