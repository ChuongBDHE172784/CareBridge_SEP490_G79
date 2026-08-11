import { useEffect, useState, useCallback, useMemo, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchStaffContentList, archiveContent, updateContent } from '../services/contentApi';
import type { ContentDetail, ContentStage, ContentStatus, ContentType } from '../models/content';
import { STAGE_LABELS, STAGE_OPTIONS, STATUS_LABELS } from '../models/content';
import ReviewFeedbackNotice from '../components/ReviewFeedbackNotice';
import { SortableTableHeader, type SortDirection } from '../components/SortableTableHeader';
import { useDebouncedValue } from '../hooks/useDebouncedValue';
import { nextSortDirection, sortRows } from '../utils/tableSorting';
import ImportContentModal from '../components/ImportContentModal';

/* ------------------------------------------------------------------ */
/*  Helpers                                                            */
/* ------------------------------------------------------------------ */
const STATUS_TABS: { key: string; label: string; status?: ContentStatus }[] = [
  { key: 'all', label: 'Tất cả' },
  { key: 'approved', label: 'Đã duyệt', status: 'APPROVED' },
  { key: 'pending', label: 'Chờ duyệt', status: 'PENDING_REVIEW' },
  { key: 'draft', label: 'Bản nháp', status: 'DRAFT' },
];

function statusBadgeClass(status: ContentStatus, returned: boolean): string {
  if (returned) return 'bg-error-container text-error';
  if (status === 'APPROVED') return 'bg-[#E6F4EA] text-[#137333]';
  if (status === 'PENDING_REVIEW') return 'bg-[#FFF3E0] text-[#E65100]';
  if (status === 'ARCHIVED') return 'bg-[#F5F5F5] text-[#616161]';
  return 'bg-surface-container-highest text-primary';
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
interface ContentTypeListPageProps {
  type: ContentType;
  title: string;
  subtitle: string;
  createLabel: string;
  emptyLabel: string;
}

export default function ContentTypeListPage({ type, title, subtitle, createLabel, emptyLabel }: ContentTypeListPageProps) {
  const navigate = useNavigate();
  const createPath = type === 'ARTICLE' ? '/content/articles/create' : '/content/faq/create';
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);
  const [items, setItems] = useState<ContentDetail[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('all');
  const [stageFilter, setStageFilter] = useState<ContentStage | ''>('');
  const [searchInput, setSearchInput] = useState('');
  const [page, setPage] = useState(0);
  const [total, setTotal] = useState(0);
  const [submittingId, setSubmittingId] = useState<string | null>(null);
  const [actionError, setActionError] = useState('');
  const [sortKey, setSortKey] = useState<'title' | 'stage' | 'status' | 'updatedAt'>('updatedAt');
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc');
  const latestRequestId = useRef(0);
  const debouncedKeyword = useDebouncedValue(searchInput.trim());
  const pageSize = 10;

  useEffect(() => {
    setPage(0);
  }, [debouncedKeyword]);

  const loadData = useCallback(async () => {
    const requestId = latestRequestId.current + 1;
    latestRequestId.current = requestId;
    setIsLoading(true);
    try {
      const tab = STATUS_TABS.find(t => t.key === activeTab);
      const data = await fetchStaffContentList({
        type,
        status: tab?.status,
        stage: stageFilter || undefined,
        keyword: debouncedKeyword || undefined,
        page,
        size: pageSize,
      });
      if (requestId !== latestRequestId.current) return;
      setItems(data.content);
      setTotal(data.totalElements);
    } catch {
      if (requestId !== latestRequestId.current) return;
      setItems([]);
      setTotal(0);
    } finally {
      if (requestId === latestRequestId.current) setIsLoading(false);
    }
  }, [type, activeTab, stageFilter, debouncedKeyword, page]);

  useEffect(() => { loadData(); }, [loadData]);

  const totalPages = Math.ceil(total / pageSize);

  const sortedItems = useMemo(() => sortRows(items, sortDirection, (item) => {
    switch (sortKey) {
      case 'title': return item.title;
      case 'stage': return STAGE_LABELS[item.stage];
      case 'status': return item.latestReviewFeedback ? 'Cần chỉnh sửa' : STATUS_LABELS[item.status];
      case 'updatedAt': {
        const timestamp = new Date(item.updatedAt ?? item.createdAt ?? item.publishedAt ?? 0).getTime();
        return Number.isNaN(timestamp) ? 0 : timestamp;
      }
    }
  }), [items, sortDirection, sortKey]);

  const changeSort = (key: typeof sortKey) => {
    setSortDirection(nextSortDirection(sortKey, key, sortDirection));
    setSortKey(key);
  };

  const handleQuickSubmit = async (item: ContentDetail) => {
    if (submittingId) return;
    setSubmittingId(item.id);
    setActionError('');
    try {
      await updateContent(item.id, {
        title: item.title,
        body: item.body,
        summary: item.summary ?? undefined,
        stage: item.stage,
        topicId: item.topicId ?? undefined,
        tagIds: item.tagIds,
        eligibleFromWeek: item.eligibleFromWeek ?? null,
        eligibleToWeek: item.eligibleToWeek ?? null,
        recommendationPriority: item.recommendationPriority ?? 0,
        status: 'PENDING_REVIEW',
        sourceLabel: item.sourceLabel ?? undefined,
        sources: item.sources,
      });
      await loadData();
    } catch {
      setActionError('Không thể gửi duyệt bài viết. Vui lòng thử lại.');
    } finally {
      setSubmittingId(null);
    }
  };

  const handleDelete = async (item: ContentDetail) => {
    const reason = window.prompt(`Nhập lý do xóa (lưu trữ) "${item.title}":`);
    if (reason === null) return;
    if (!reason.trim()) {
      setActionError('Vui lòng nhập lý do trước khi xóa.');
      return;
    }
    try {
      await archiveContent(item.id, reason.trim());
      setActionError('');
      await loadData();
    } catch {
      setActionError('Không thể xóa nội dung. Vui lòng thử lại.');
    }
  };

  return (
    <div className="p-8 font-sans">
      {/* Header */}
      <div className="flex justify-between items-start mb-6">
        <div>
          <h1 className="text-[26px] font-bold text-on-surface m-0">{title}</h1>
          <p className="text-on-surface-variant text-sm mt-1">{subtitle}</p>
        </div>
        <div className="flex gap-2.5">
          <button
            type="button"
            onClick={() => setIsImportModalOpen(true)}
            className="flex items-center gap-2 py-3 px-5 rounded-full border border-primary/40 bg-surface text-primary text-sm font-semibold cursor-pointer whitespace-nowrap hover:bg-primary-container/20 transition-colors"
          >
            <span className="material-symbols-outlined text-lg">upload_file</span>
            Import từ file
          </button>
          <button
            type="button"
            onClick={() => navigate(createPath)}
            className="flex items-center gap-2 py-3 px-6 rounded-full bg-primary text-on-primary border-0 text-sm font-semibold cursor-pointer whitespace-nowrap"
          >
            <span className="material-symbols-outlined text-lg">add</span>
            {createLabel}
          </button>
        </div>
      </div>

      {actionError && <div className="bg-error-container rounded-2xl p-4 mb-4 text-error text-sm">{actionError}</div>}

      {/* Status tabs + filters */}
      <div className="flex justify-between items-center mb-5 flex-wrap gap-3">
        <div className="flex gap-2">
          {STATUS_TABS.map(tab => (
            <button
              key={tab.key}
              onClick={() => { setActiveTab(tab.key); setPage(0); }}
              className={`flex items-center gap-1.5 py-2 px-[18px] rounded-full text-[13px] font-semibold cursor-pointer ${
                activeTab === tab.key
                  ? 'border-2 border-primary bg-surface-container-low text-primary'
                  : 'border border-outline-variant bg-transparent text-on-surface-variant'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
        <div className="flex gap-2 flex-1 justify-end">
          <div className="relative max-w-[280px] flex-1">
            <span className="material-symbols-outlined text-outline absolute left-[14px] top-1/2 -translate-y-1/2 text-xl">search</span>
            <input
              value={searchInput}
              onChange={e => setSearchInput(e.target.value)}
              placeholder="Tìm kiếm theo tiêu đề..."
              className="w-full py-2.5 pr-[14px] pl-[42px] rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none font-sans"
            />
          </div>
          <select
            value={stageFilter}
            onChange={e => { setStageFilter(e.target.value as ContentStage | ''); setPage(0); }}
            className="py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface-variant cursor-pointer font-sans"
          >
            <option value="">Tất cả giai đoạn</option>
            {STAGE_OPTIONS.map(({ value, label }) => <option key={value} value={value}>{label}</option>)}
          </select>
        </div>
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
                  {[
                    ['title', 'TIÊU ĐỀ'],
                    ['stage', 'GIAI ĐOẠN'],
                    ['status', 'TRẠNG THÁI'],
                    ['updatedAt', 'CẬP NHẬT LẦN CUỐI'],
                  ].map(([key, label]) => (
                    <SortableTableHeader
                      key={key}
                      label={label}
                      active={sortKey === key}
                      direction={sortDirection}
                      onClick={() => changeSort(key as typeof sortKey)}
                    />
                  ))}
                  <th scope="col" className="px-2 py-3 text-[11px] font-semibold uppercase tracking-[0.05em] text-outline">THAO TÁC</th>
                </tr>
              </thead>
              <tbody>
                {sortedItems.map(item => (
                  <tr key={item.id} className="border-b border-surface-container-highest hover:bg-surface-bright">
                    <td className="py-3.5 px-2 max-w-[400px]">
                        <div className="font-semibold text-sm text-on-surface">{item.title}</div>
                        <ReviewFeedbackNotice feedback={item.latestReviewFeedback} compact />
                    </td>
                    <td className="py-3.5 px-2 text-[13px] text-on-surface-variant">{STAGE_LABELS[item.stage]}</td>
                    <td className="py-3.5 px-2">
                      <span className={`py-1 px-3.5 rounded-full text-xs font-semibold ${statusBadgeClass(item.status, Boolean(item.latestReviewFeedback))}`}>
                        {item.latestReviewFeedback ? 'Cần chỉnh sửa' : STATUS_LABELS[item.status]}
                      </span>
                    </td>
                    <td className="py-3.5 px-2 text-[13px] text-outline">{timeAgo(item.updatedAt ?? item.createdAt ?? item.publishedAt)}</td>
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
                          disabled={item.status !== 'DRAFT' && item.status !== 'PENDING_REVIEW'}
                          className="w-8 h-8 rounded-lg border border-outline-variant bg-transparent cursor-pointer flex items-center justify-center disabled:opacity-40 disabled:cursor-not-allowed"
                          title={item.status !== 'DRAFT' && item.status !== 'PENDING_REVIEW'
                            ? 'Nội dung đã xuất bản/lưu trữ không thể chỉnh sửa trực tiếp'
                            : 'Chỉnh sửa'}
                        >
                          <span className="material-symbols-outlined text-primary text-base">edit</span>
                        </button>
                        <button
                          onClick={() => handleQuickSubmit(item)}
                          disabled={item.status !== 'DRAFT' || submittingId === item.id}
                          className="w-8 h-8 rounded-lg border border-outline-variant bg-transparent cursor-pointer flex items-center justify-center disabled:opacity-40 disabled:cursor-not-allowed"
                          title={item.status !== 'DRAFT' ? 'Chỉ bài viết/FAQ dạng Bản nháp mới có thể gửi duyệt' : 'Gửi duyệt nhanh'}
                        >
                          {submittingId === item.id ? (
                            <span className="material-symbols-outlined text-primary text-base animate-spin">sync</span>
                          ) : (
                            <span className="material-symbols-outlined text-primary text-base">send</span>
                          )}
                        </button>
                        <button
                          onClick={() => handleDelete(item)}
                          className="w-8 h-8 rounded-lg border border-outline-variant bg-transparent cursor-pointer flex items-center justify-center"
                          title="Xóa"
                        >
                          <span className="material-symbols-outlined text-error text-base">delete</span>
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {items.length === 0 && (
                  <tr><td colSpan={5} className="py-12 text-center text-outline">{emptyLabel}</td></tr>
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

      <ImportContentModal
        type={type}
        isOpen={isImportModalOpen}
        onClose={() => setIsImportModalOpen(false)}
        onSuccess={loadData}
      />
    </div>
  );
}
