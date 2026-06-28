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
function statusFromPublished(publishedAt: string | null): { label: string; className: string } {
  if (publishedAt) return { label: 'Đã xuất bản', className: 'bg-[#E6F4EA] text-[#137333]' };
  return { label: 'Bản nháp', className: 'bg-surface-container-highest text-primary' };
}

function timeAgo(iso: string | null): string {
  if (!iso) return '—';
  const diff = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 60) return `${mins} phút trước`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours} giờ trước`;
  const days = Math.floor(hours / 24);
  return `${days} ngày trước`;
}

/* ------------------------------------------------------------------ */
/*  StatCard                                                           */
/* ------------------------------------------------------------------ */
function StatCard({ icon, label, value, accent }: { icon: string; label: string; value: number; accent: string }) {
  return (
    <div className="bg-surface rounded-3xl p-6 shadow-md flex items-center gap-4">
      <div
        className="w-12 h-12 rounded-full flex items-center justify-center shrink-0"
        style={{ background: accent + '18' }}
      >
        <span className="material-symbols-outlined" style={{ fontSize: 24, color: accent }}>{icon}</span>
      </div>
      <div>
        <div className="text-[28px] font-bold text-on-surface">{value}</div>
        <div className="text-[13px] text-outline mt-0.5">{label}</div>
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
    <div className="p-8 font-sans">
      {/* Header */}
      <div className="flex justify-between items-start mb-7">
        <div>
          <h1 className="text-[26px] font-bold text-on-surface m-0">Tổng quan nội dung</h1>
          <p className="text-on-surface-variant text-sm mt-1">Quản lý và theo dõi toàn bộ nội dung trên hệ thống CareBridge</p>
        </div>
        <button
          onClick={() => navigate('/content/create')}
          className="flex items-center gap-2 py-3 px-6 rounded-full bg-primary-container text-on-primary border-0 text-sm font-semibold cursor-pointer"
        >
          <span className="material-symbols-outlined" style={{ fontSize: 18 }}>add</span>
          Tạo nội dung mới
        </button>
      </div>

      {/* Summary cards row 1 */}
      <div className="grid grid-cols-4 gap-4 mb-4">
        <StatCard icon="edit_note" label="Bản nháp" value={stats.drafts} accent="#845143" />
        <StatCard icon="hourglass_top" label="Chờ duyệt" value={stats.pending} accent="#E65100" />
        <StatCard icon="check_circle" label="Đã xuất bản" value={stats.published} accent="#137333" />
        <StatCard icon="sync_problem" label="Yêu cầu cập nhật" value={stats.updateRequired} accent="#BA1A1A" />
      </div>

      {/* Summary cards row 2 */}
      <div className="grid grid-cols-3 gap-4 mb-7">
        <StatCard icon="quiz" label="FAQ" value={stats.faqCount} accent="#845143" />
        <StatCard icon="fact_check" label="Checklist" value={stats.checklistCount} accent="#6e5a52" />
        <StatCard icon="warning" label="Vấn đề danh mục" value={stats.categoryIssues} accent="#BA1A1A" />
      </div>

      {/* Recent content table */}
      <div className="bg-surface rounded-2xl p-6 shadow-md">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-lg font-semibold text-on-surface m-0">Nội dung gần đây</h2>
          <button
            onClick={() => navigate('/content/list')}
            className="py-2 px-5 rounded-full border border-outline-variant bg-transparent text-primary text-[13px] font-semibold cursor-pointer"
          >
            Xem tất cả
          </button>
        </div>

        {isLoading ? (
          <div className="py-12 text-center text-outline">Đang tải...</div>
        ) : (
          <table className="w-full border-collapse">
            <thead>
              <tr className="border-b-2 border-surface-container-highest text-left">
                {['TIÊU ĐỀ', 'LOẠI', 'CHỦ ĐỀ', 'TRẠNG THÁI', 'CẬP NHẬT LẦN CUỐI'].map(h => (
                  <th key={h} className="py-3 px-2 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">{h}</th>
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
                    className="border-b border-surface-container-highest cursor-pointer hover:bg-surface-bright"
                  >
                    <td className="py-3.5 px-2">
                      <div className="font-semibold text-sm text-on-surface">{item.title}</div>
                    </td>
                    <td className="py-3.5 px-2">
                      <span className="inline-flex items-center gap-1 py-1 px-3 rounded-full bg-surface-container-low text-primary text-xs font-semibold">
                        <span className="material-symbols-outlined" style={{ fontSize: 14 }}>
                          {item.type === 'ARTICLE' ? 'article' : item.type === 'FAQ' ? 'quiz' : 'fact_check'}
                        </span>
                        {TYPE_LABELS[item.type]}
                      </span>
                    </td>
                    <td className="py-3.5 px-2 text-[13px] text-on-surface-variant">
                      {STAGE_LABELS[item.stage]}
                    </td>
                    <td className="py-3.5 px-2">
                      <span className={`py-1 px-3.5 rounded-full text-xs font-semibold ${status.className}`}>
                        {status.label}
                      </span>
                    </td>
                    <td className="py-3.5 px-2 text-[13px] text-outline">
                      {timeAgo(item.publishedAt)}
                    </td>
                  </tr>
                );
              })}
              {recentItems.length === 0 && (
                <tr><td colSpan={5} className="py-8 text-center text-outline">Chưa có nội dung nào.</td></tr>
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
