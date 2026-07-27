import { useEffect, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchStaffContentList } from '../services/contentApi';
import type { ContentDetail, ContentType } from '../models/content';
import { STATUS_LABELS, TYPE_LABELS } from '../models/content';
import ReviewFeedbackNotice from '../components/ReviewFeedbackNotice';


/* ------------------------------------------------------------------ */
/*  Helpers                                                            */
/* ------------------------------------------------------------------ */
function statusBadge(item: ContentDetail): { label: string; className: string } {
  if (item.latestReviewFeedback) return { label: 'Cần chỉnh sửa', className: 'bg-error-container text-error' };
  if (item.status === 'APPROVED') return { label: STATUS_LABELS.APPROVED, className: 'bg-[#E6F4EA] text-[#137333]' };
  if (item.status === 'PENDING_REVIEW') return { label: STATUS_LABELS.PENDING_REVIEW, className: 'bg-[#FFF3E0] text-[#E65100]' };
  return { label: STATUS_LABELS[item.status], className: 'bg-[#F5F5F5] text-[#616161]' };
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
  if (mins < 60) return `${mins} phút trước`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours} giờ trước`;
  const days = Math.floor(hours / 24);
  return `${days} ngày trước`;
}

/* ------------------------------------------------------------------ */
/*  Page Component                                                     */
/* ------------------------------------------------------------------ */
export default function ContentListPage() {
  const navigate = useNavigate();
  const [items, setItems] = useState<ContentDetail[]>([]);
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
      const data = await fetchStaffContentList({
        keyword: keyword || undefined,
        type: typeFilter || undefined,
        page,
        size: pageSize,
      });
      setItems(data.content);
      setTotal(data.totalElements);
    } catch {
      setItems([]);
      setTotal(0);
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
    <div className="p-8 font-sans">
      {/* Header */}
      <div className="mb-6">
        <h1 className="text-[26px] font-bold text-on-surface m-0">Danh sách nội dung</h1>
        <p className="text-on-surface-variant text-sm mt-1">Quản lý và duyệt các bài viết, tài liệu cho thư viện</p>
      </div>

      {/* Action bar */}
      <div className="flex items-center gap-3 mb-5">
        <div className="flex-1 relative">
          <span className="material-symbols-outlined text-outline absolute left-[14px] top-1/2 -translate-y-1/2 text-xl">search</span>
          <input
            value={searchInput}
            onChange={e => setSearchInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Tìm kiếm theo tiêu đề, tác giả..."
            className="w-full py-3 pr-[14px] pl-[42px] rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none font-sans"
          />
        </div>

        {/* Type filter dropdown */}
        <select
          value={typeFilter}
          onChange={e => { setTypeFilter(e.target.value as ContentType | ''); setPage(0); }}
          className="py-3 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface-variant cursor-pointer font-sans"
        >
          <option value="">Bộ lọc</option>
          <option value="ARTICLE">Bài viết</option>
          <option value="FAQ">FAQ</option>
          <option value="CHECKLIST">Checklist</option>
        </select>

        <button
          onClick={() => navigate('/content/create')}
          className="flex items-center gap-2 py-3 px-6 rounded-full bg-primary text-on-primary border-0 text-sm font-semibold cursor-pointer whitespace-nowrap"
        >
          <span className="material-symbols-outlined text-lg">add</span>
          Tạo nội dung mới
        </button>
      </div>

      {/* Data table */}
      <div className="bg-surface rounded-2xl p-6 shadow-md">
        {isLoading ? (
          <div className="py-12 text-center text-outline">Đang tải...</div>
        ) : (
          <>
            <table className="w-full border-collapse">
              <thead>
                <tr className="border-b-2 border-surface-container-highest text-left">
                  {['TIÊU ĐỀ', 'PHÂN LOẠI', 'TRẠNG THÁI', 'PHIÊN BẢN', 'CẬP NHẬT BỞI', 'THAO TÁC'].map(h => (
                    <th key={h} className="py-3 px-2 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {items.map(item => {
                  const status = statusBadge(item);
                  return (
                    <tr
                      key={item.id}
                      className="border-b border-surface-container-highest hover:bg-surface-bright"
                    >
                      <td className="py-3.5 px-2 max-w-[320px]">
                        <div className="font-semibold text-sm text-on-surface">{item.title}</div>
                        <ReviewFeedbackNotice feedback={item.latestReviewFeedback} compact />
                        <div className="text-xs text-outline mt-0.5">Cập nhật: {timeAgo(item.publishedAt)}</div>
                      </td>
                      <td className="py-3.5 px-2">
                        <span className="inline-flex items-center gap-1 py-1 px-3 rounded-full bg-surface-container-low text-primary text-xs font-semibold">
                          <span className="material-symbols-outlined text-sm">{typeIcon(item.type)}</span>
                          {TYPE_LABELS[item.type]}
                        </span>
                      </td>
                      <td className="py-3.5 px-2">
                        <span className={`py-1 px-3.5 rounded-full text-xs font-semibold ${status.className}`}>
                          {status.label}
                        </span>
                      </td>
                      <td className="py-3.5 px-2 text-[13px] text-on-surface-variant">v1</td>
                      <td className="py-3.5 px-2 text-[13px] text-on-surface-variant">Content Admin</td>
                      <td className="py-3.5 px-2">
                        <div className="flex gap-1">
                          <button
                            onClick={() => navigate(`/content/${item.id}`)}
                            className="w-8 h-8 rounded-lg border border-outline-variant bg-transparent cursor-pointer flex items-center justify-center"
                            title="Xem chi tiết"
                          >
                            <span className="material-symbols-outlined text-primary text-base">visibility</span>
                          </button>
                          <button
                            onClick={() => navigate(`/content/${item.id}/edit`)}
                            className="w-8 h-8 rounded-lg border border-outline-variant bg-transparent cursor-pointer flex items-center justify-center"
                            title="Chỉnh sửa"
                          >
                            <span className="material-symbols-outlined text-primary text-base">edit</span>
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
                {items.length === 0 && (
                  <tr><td colSpan={6} className="py-12 text-center text-outline">Không tìm thấy nội dung nào.</td></tr>
                )}
              </tbody>
            </table>

            {/* Pagination */}
            <div className="flex justify-between items-center mt-5 pt-4 border-t border-surface-container-highest">
              <span className="text-[13px] text-outline">
                Hiển thị {total === 0 ? 0 : page * pageSize + 1}-{Math.min((page + 1) * pageSize, total)} trong {total} kết quả
              </span>
              <div className="flex gap-1">
                <button
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className={`w-9 h-9 rounded-full border border-outline-variant bg-surface flex items-center justify-center ${page === 0 ? 'opacity-40 cursor-default' : 'cursor-pointer'}`}
                >
                  <span className="material-symbols-outlined text-primary text-lg">chevron_left</span>
                </button>
                {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
                  const startPage = Math.max(0, Math.min(page - 2, totalPages - 5));
                  const p = startPage + i;
                  if (p >= totalPages) return null;
                  return (
                    <button
                      key={p}
                      onClick={() => setPage(p)}
                      className={`w-9 h-9 rounded-full text-sm font-semibold cursor-pointer flex items-center justify-center ${page === p ? 'border-0 bg-primary text-on-primary' : 'border border-outline-variant bg-surface text-on-surface-variant'}`}
                    >
                      {p + 1}
                    </button>
                  );
                })}
                <button
                  onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                  className={`w-9 h-9 rounded-full border border-outline-variant bg-surface flex items-center justify-center ${page >= totalPages - 1 ? 'opacity-40 cursor-default' : 'cursor-pointer'}`}
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
