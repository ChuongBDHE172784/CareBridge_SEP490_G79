import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchContentList } from '../services/contentApi';
import type { ContentListItem } from '../models/content';
import { TYPE_LABELS, STAGE_LABELS } from '../models/content';

/* ------------------------------------------------------------------ */
/*  Mock data (fallback when backend stats endpoint is unavailable)   */
/* ------------------------------------------------------------------ */
const MOCK_STATS = {
  drafts: 12,
  pending: 5,
  published: 87,
  updateRequired: 3,
  faqCount: 42,
  checklistCount: 18,
  categoryIssues: 2,
};

const MOCK_RECENT: ContentListItem[] = [
  { id: '1', type: 'ARTICLE', title: 'Dinh duong trong 3 thang dau thai ky', stage: 'PREGNANCY', topicId: 't1', publishedAt: '2026-06-25T10:00:00Z' },
  { id: '2', type: 'FAQ', title: 'Khi nao can gap bac si san khoa?', stage: 'PREGNANCY', topicId: 't2', publishedAt: null },
  { id: '3', type: 'CHECKLIST', title: 'Checklist chuan bi do so sinh', stage: 'POSTPARTUM', topicId: 't3', publishedAt: '2026-06-20T08:30:00Z' },
  { id: '4', type: 'ARTICLE', title: 'Cac bai tap an toan cho ba bau', stage: 'PREGNANCY', topicId: 't1', publishedAt: '2026-06-18T14:00:00Z' },
  { id: '5', type: 'ARTICLE', title: 'Tam ly sau sinh va cach vuot qua', stage: 'POSTPARTUM', topicId: 't4', publishedAt: null },
];

/* ------------------------------------------------------------------ */
/*  Helpers                                                            */
/* ------------------------------------------------------------------ */
function statusFromPublished(publishedAt: string | null): { label: string; bg: string; color: string } {
  if (publishedAt) return { label: 'Da xuat ban', bg: '#E6F4EA', color: '#137333' };
  return { label: 'Ban nhap', bg: '#FADCD3', color: '#845143' };
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
/*  StatCard                                                           */
/* ------------------------------------------------------------------ */
function StatCard({ icon, label, value, accent }: { icon: string; label: string; value: number; accent: string }) {
  return (
    <div style={{ background: '#fff', borderRadius: 24, padding: 24, boxShadow: '0 4px 20px rgba(90,70,63,0.06)', display: 'flex', alignItems: 'center', gap: 16 }}>
      <div style={{ width: 48, height: 48, borderRadius: '50%', background: accent + '18', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <span className="material-symbols-outlined" style={{ fontSize: 24, color: accent }}>{icon}</span>
      </div>
      <div>
        <div style={{ fontSize: 28, fontWeight: 700, color: '#271812' }}>{value}</div>
        <div style={{ fontSize: 13, color: '#84736f', marginTop: 2 }}>{label}</div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------ */
/*  Page Component                                                     */
/* ------------------------------------------------------------------ */
export default function ContentDashboardPage() {
  const navigate = useNavigate();
  const [recentItems, setRecentItems] = useState<ContentListItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // TODO: wire to backend stats endpoint when available
  const stats = MOCK_STATS;

  const loadRecent = useCallback(async () => {
    setIsLoading(true);
    try {
      const data = await fetchContentList({ page: 0, size: 5 });
      setRecentItems(data.content);
    } catch {
      // Fallback to mock data when endpoint is unavailable
      setRecentItems(MOCK_RECENT);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => { loadRecent(); }, [loadRecent]);

  return (
    <div style={{ padding: 32, fontFamily: "'Lexend', sans-serif" }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 28 }}>
        <div>
          <h1 style={{ fontSize: 26, fontWeight: 700, color: '#271812', margin: 0 }}>Tong quan noi dung</h1>
          <p style={{ color: '#524440', fontSize: 14, marginTop: 4 }}>Quan ly va theo doi toan bo noi dung tren he thong CareBridge</p>
        </div>
        <button
          onClick={() => navigate('/content/create')}
          style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '12px 24px', borderRadius: 9999, background: '#C98C7B', color: '#fff', border: 'none', fontSize: 14, fontWeight: 600, cursor: 'pointer' }}
        >
          <span className="material-symbols-outlined" style={{ fontSize: 18 }}>add</span>
          Tao noi dung moi
        </button>
      </div>

      {/* Summary cards row 1 */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 16 }}>
        <StatCard icon="edit_note" label="Ban nhap" value={stats.drafts} accent="#845143" />
        <StatCard icon="hourglass_top" label="Cho duyet" value={stats.pending} accent="#E65100" />
        <StatCard icon="check_circle" label="Da xuat ban" value={stats.published} accent="#137333" />
        <StatCard icon="sync_problem" label="Yeu cau cap nhat" value={stats.updateRequired} accent="#BA1A1A" />
      </div>

      {/* Summary cards row 2 */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 28 }}>
        <StatCard icon="quiz" label="FAQ" value={stats.faqCount} accent="#845143" />
        <StatCard icon="fact_check" label="Checklist" value={stats.checklistCount} accent="#6e5a52" />
        <StatCard icon="warning" label="Van de danh muc" value={stats.categoryIssues} accent="#BA1A1A" />
      </div>

      {/* Recent content table */}
      <div style={{ background: '#fff', borderRadius: 16, padding: 24, boxShadow: '0 4px 20px rgba(90,70,63,0.06)' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <h2 style={{ fontSize: 18, fontWeight: 600, color: '#271812', margin: 0 }}>Noi dung gan day</h2>
          <button
            onClick={() => navigate('/content/list')}
            style={{ padding: '8px 20px', borderRadius: 9999, border: '1px solid #d6c2bd', background: 'transparent', color: '#845143', fontSize: 13, fontWeight: 600, cursor: 'pointer' }}
          >
            Xem tat ca
          </button>
        </div>

        {isLoading ? (
          <div style={{ padding: 48, textAlign: 'center', color: '#84736f' }}>Dang tai...</div>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '2px solid #fadcd3', textAlign: 'left' }}>
                {['TIEU DE', 'LOAI', 'CHU DE', 'TRANG THAI', 'CAP NHAT LAN CUOI'].map(h => (
                  <th key={h} style={{ padding: '12px 8px', fontSize: 11, fontWeight: 600, color: '#84736f', textTransform: 'uppercase' as const, letterSpacing: '0.05em' }}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {recentItems.map(item => {
                const status = statusFromPublished(item.publishedAt);
                return (
                  <tr
                    key={item.id}
                    onClick={() => navigate(`/content/${item.id}`)}
                    style={{ borderBottom: '1px solid #fadcd3', cursor: 'pointer' }}
                    onMouseEnter={e => { (e.currentTarget as HTMLElement).style.background = '#FFF8F6'; }}
                    onMouseLeave={e => { (e.currentTarget as HTMLElement).style.background = 'transparent'; }}
                  >
                    <td style={{ padding: '14px 8px' }}>
                      <div style={{ fontWeight: 600, fontSize: 14, color: '#271812' }}>{item.title}</div>
                    </td>
                    <td style={{ padding: '14px 8px' }}>
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4, padding: '4px 12px', borderRadius: 9999, background: '#FFF1EC', color: '#845143', fontSize: 12, fontWeight: 600 }}>
                        <span className="material-symbols-outlined" style={{ fontSize: 14 }}>
                          {item.type === 'ARTICLE' ? 'article' : item.type === 'FAQ' ? 'quiz' : 'fact_check'}
                        </span>
                        {TYPE_LABELS[item.type]}
                      </span>
                    </td>
                    <td style={{ padding: '14px 8px', fontSize: 13, color: '#524440' }}>
                      {STAGE_LABELS[item.stage]}
                    </td>
                    <td style={{ padding: '14px 8px' }}>
                      <span style={{ padding: '4px 14px', borderRadius: 9999, background: status.bg, color: status.color, fontSize: 12, fontWeight: 600 }}>
                        {status.label}
                      </span>
                    </td>
                    <td style={{ padding: '14px 8px', fontSize: 13, color: '#84736f' }}>
                      {timeAgo(item.publishedAt)}
                    </td>
                  </tr>
                );
              })}
              {recentItems.length === 0 && (
                <tr><td colSpan={5} style={{ padding: 32, textAlign: 'center', color: '#84736f' }}>Chua co noi dung nao.</td></tr>
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
