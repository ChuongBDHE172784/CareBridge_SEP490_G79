import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../../shared/auth/authStore';
import { claimReport, fetchModerationQueue, releaseReport } from '../services/moderationApi';
import type { CasePriority, ModerationQueueItem, ReportSource, ReportTargetType } from '../models/moderation';
import {
  CASE_PRIORITY_LABELS,
  CASE_PRIORITY_STYLES,
  formatReportReason,
  REPORT_SOURCE_LABELS,
  REPORT_STATUS_LABELS,
  TARGET_TYPE_LABELS,
} from '../models/moderation';
import { SortableTableHeader, type SortDirection } from '../../contentManagement/components/SortableTableHeader';
import { nextSortDirection, sortRows } from '../../contentManagement/utils/tableSorting';

type Tab = 'PENDING' | 'PROCESSED';
type ReportSortKey = 'reason' | 'source' | 'targetType' | 'contentPreview' | 'authorName' | 'status' | 'reportedAt';
type SourceFilter = 'ALL' | ReportSource;
type PriorityFilter = 'ALL' | CasePriority;
type TargetFilter = 'ALL' | ReportTargetType;

const PAGE_SIZE_OPTIONS = [10, 20, 50] as const;
const TABS: { label: string; value: Tab; icon: string }[] = [
  { label: 'Báo cáo đang mở', value: 'PENDING', icon: 'flag' },
  { label: 'Đã xử lý', value: 'PROCESSED', icon: 'task_alt' },
];

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });
}

function matchesText(value: string | null | undefined, query: string): boolean {
  return (value ?? '').toLowerCase().includes(query);
}

export default function ReportsQueuePage() {
  const navigate = useNavigate();
  const currentUserId = useAuthStore((s) => s.user?.id ?? null);
  const [tab, setTab] = useState<Tab>('PENDING');
  const [sourceFilter, setSourceFilter] = useState<SourceFilter>('ALL');
  const [priorityFilter, setPriorityFilter] = useState<PriorityFilter>('ALL');
  const [targetFilter, setTargetFilter] = useState<TargetFilter>('ALL');
  const [search, setSearch] = useState('');
  const [pageSize, setPageSize] = useState<(typeof PAGE_SIZE_OPTIONS)[number]>(10);
  const [page, setPage] = useState(0);
  const [sortKey, setSortKey] = useState<ReportSortKey>('reportedAt');
  const [sortDirection, setSortDirection] = useState<SortDirection>('desc');
  const [items, setItems] = useState<ModerationQueueItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [claimingId, setClaimingId] = useState<string | null>(null);

  const loadReports = useCallback(async () => {
    setIsLoading(true);
    setError('');
    try {
      const source = sourceFilter === 'ALL' ? undefined : sourceFilter;
      const priority = priorityFilter === 'ALL' ? undefined : priorityFilter;
      const targetType = targetFilter === 'ALL' ? undefined : targetFilter;
      if (tab === 'PENDING') {
        const [pending, inReview] = await Promise.all([
          fetchModerationQueue({ status: 'PENDING', source, priority, targetType, size: 50 }),
          fetchModerationQueue({ status: 'IN_REVIEW', source, priority, targetType, size: 50 }),
        ]);
        setItems([...pending.content, ...inReview.content].sort(
          (a, b) => new Date(b.reportedAt).getTime() - new Date(a.reportedAt).getTime(),
        ));
      } else {
        const [resolved, dismissed] = await Promise.all([
          fetchModerationQueue({ status: 'RESOLVED', source, priority, targetType, size: 50 }),
          fetchModerationQueue({ status: 'DISMISSED', source, priority, targetType, size: 50 }),
        ]);
        setItems([...resolved.content, ...dismissed.content].sort(
          (a, b) => new Date(b.reportedAt).getTime() - new Date(a.reportedAt).getTime(),
        ));
      }
    } catch {
      setError('Không tải được danh sách báo cáo.');
      setItems([]);
    } finally {
      setIsLoading(false);
    }
  }, [priorityFilter, sourceFilter, tab, targetFilter]);

  useEffect(() => { void loadReports(); }, [loadReports]);
  useEffect(() => { setPage(0); }, [pageSize, priorityFilter, search, sourceFilter, tab, targetFilter]);

  const stats = useMemo(() => ({
    total: items.length,
    ai: items.filter((item) => item.reportSource === 'AUTOMATED').length,
    urgent: items.filter((item) => item.priority === 'URGENT' || item.priority === 'HIGH').length,
    claimedByMe: items.filter((item) => item.status === 'IN_REVIEW' && item.assignedModeratorId === currentUserId).length,
  }), [currentUserId, items]);

  const filteredItems = useMemo(() => {
    const query = search.trim().toLowerCase();
    return items.filter((item) => {
      return query.length === 0
        || matchesText(item.contentPreview, query)
        || matchesText(item.targetTitle, query)
        || matchesText(item.authorName, query)
        || matchesText(item.authorEmail, query)
        || matchesText(item.authorPhone, query)
        || matchesText(formatReportReason(item.reportReason), query)
        || matchesText(TARGET_TYPE_LABELS[item.targetType], query)
        || matchesText(REPORT_SOURCE_LABELS[item.reportSource], query)
        || matchesText(REPORT_STATUS_LABELS[item.status], query);
    });
  }, [items, search]);

  const sortedItems = useMemo(() => sortRows(filteredItems, sortDirection, (item) => {
    switch (sortKey) {
      case 'reason': return `${formatReportReason(item.reportReason)} ${CASE_PRIORITY_LABELS[item.priority]}`;
      case 'source': return REPORT_SOURCE_LABELS[item.reportSource];
      case 'targetType': return TARGET_TYPE_LABELS[item.targetType];
      case 'contentPreview': return item.contentPreview;
      case 'authorName': return item.authorName ?? '';
      case 'status': return tab === 'PENDING'
        ? (item.status === 'IN_REVIEW' ? 'Đang xem xét' : item.reportCount)
        : REPORT_STATUS_LABELS[item.status];
      case 'reportedAt': return new Date(item.reportedAt).getTime();
    }
  }), [filteredItems, sortDirection, sortKey, tab]);

  const totalPages = Math.max(1, Math.ceil(sortedItems.length / pageSize));
  const currentPage = Math.min(page, totalPages - 1);
  const pagedItems = sortedItems.slice(currentPage * pageSize, currentPage * pageSize + pageSize);
  const pageStart = sortedItems.length === 0 ? 0 : currentPage * pageSize + 1;
  const pageEnd = Math.min((currentPage + 1) * pageSize, sortedItems.length);

  const changeSort = (key: ReportSortKey) => {
    setSortDirection(nextSortDirection(sortKey, key, sortDirection));
    setSortKey(key);
    setPage(0);
  };

  const goToDetail = (item: ModerationQueueItem) => {
    if (item.targetType === 'ACCOUNT' || item.targetType === 'USER' || item.targetType === 'EXPERT') {
      navigate(`/moderator/reports/account/${item.id}`);
    } else {
      navigate(`/moderator/reports/${item.id}`);
    }
  };

  const handleClaim = async (item: ModerationQueueItem) => {
    setActionError('');
    setClaimingId(item.id);
    try {
      await claimReport(item.id);
      await loadReports();
    } catch (err: unknown) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setActionError(message || 'Không thể nhận xử lý báo cáo này (có thể đã có người nhận).');
      await loadReports();
    } finally {
      setClaimingId(null);
    }
  };

  const handleRelease = async (item: ModerationQueueItem) => {
    setActionError('');
    setClaimingId(item.id);
    try {
      await releaseReport(item.id);
      await loadReports();
    } catch (err: unknown) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setActionError(message || 'Không thể trả lại báo cáo này.');
    } finally {
      setClaimingId(null);
    }
  };

  const resetFilters = () => {
    setSearch('');
    setSourceFilter('ALL');
    setPriorityFilter('ALL');
    setTargetFilter('ALL');
  };

  return (
    <div className="portal-page">
      <main className="font-sans">
        <div className="p-8">
          {/* Header */}
          <div className="mb-6 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            <div>
              <h1 className="text-[26px] font-bold text-on-surface m-0">Danh sách báo cáo vi phạm</h1>
              <p className="text-on-surface-variant text-sm mt-1">
                Theo dõi báo cáo do người dùng gửi và các trường hợp do AI phát hiện. AI chỉ hỗ trợ đánh giá; quyết định cuối cùng thuộc về kiểm duyệt viên.
              </p>
            </div>
            <button
              type="button"
              onClick={() => void loadReports()}
              disabled={isLoading}
              className="inline-flex items-center gap-2 py-2.5 px-5 rounded-full bg-surface border border-outline-variant text-on-surface-variant text-sm font-semibold cursor-pointer hover:bg-surface-container-low disabled:opacity-50 self-start md:self-auto"
            >
              <span className="material-symbols-outlined text-lg">refresh</span>
              Làm mới
            </button>
          </div>

          {/* Stats Bar */}
          <div className="mb-6 grid gap-4 md:grid-cols-4">
            {[
              { label: tab === 'PENDING' ? 'Đang mở' : 'Đã xử lý', value: stats.total, icon: 'flag' },
              { label: 'AI phát hiện', value: stats.ai, icon: 'smart_toy' },
              { label: 'Ưu tiên cao', value: stats.urgent, icon: 'priority_high' },
              { label: 'Bạn đang nhận', value: stats.claimedByMe, icon: 'how_to_reg' },
            ].map((stat) => (
              <div key={stat.label} className="bg-surface rounded-2xl p-5 shadow-sm border border-surface-container-highest flex items-center justify-between">
                <div>
                  <span className="text-xs font-semibold text-outline uppercase tracking-wider block mb-1">{stat.label}</span>
                  <p className="text-2xl font-bold text-on-surface m-0">{stat.value}</p>
                </div>
                <span className="material-symbols-outlined text-3xl text-primary/70">{stat.icon}</span>
              </div>
            ))}
          </div>

          {/* Action & Filter Bar */}
          <div className="bg-surface rounded-2xl p-4 shadow-sm border border-surface-container-highest mb-6 space-y-4">
            {/* Tabs */}
            <div className="flex flex-wrap gap-2 border-b border-surface-container-highest pb-3">
              {TABS.map((tabItem) => (
                <button
                  key={tabItem.value}
                  type="button"
                  onClick={() => setTab(tabItem.value)}
                  className={`inline-flex items-center gap-2 py-2 px-4 rounded-full text-xs font-semibold cursor-pointer transition-colors ${
                    tab === tabItem.value
                      ? 'bg-primary text-on-primary shadow-sm'
                      : 'bg-surface-container-low text-on-surface-variant hover:bg-surface-container-highest'
                  }`}
                >
                  <span className="material-symbols-outlined text-base">{tabItem.icon}</span>
                  {tabItem.label}
                </button>
              ))}
            </div>

            {/* Filter controls */}
            <div className="flex flex-col xl:flex-row items-center gap-3">
              <div className="flex-1 w-full relative">
                <span className="material-symbols-outlined text-outline absolute left-[14px] top-1/2 -translate-y-1/2 text-xl">search</span>
                <input
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                  placeholder="Tìm lý do, nội dung, trạng thái..."
                  className="w-full py-2.5 pr-[14px] pl-[42px] rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none font-sans"
                />
              </div>

              <div className="flex flex-wrap md:flex-nowrap items-center gap-2 w-full xl:w-auto">
                <select
                  value={sourceFilter}
                  onChange={(event) => setSourceFilter(event.target.value as SourceFilter)}
                  className="py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface-variant cursor-pointer font-sans"
                >
                  <option value="ALL">Tất cả nguồn</option>
                  <option value="USER">Người dùng</option>
                  <option value="AUTOMATED">AI phát hiện</option>
                </select>

                <select
                  value={targetFilter}
                  onChange={(event) => setTargetFilter(event.target.value as TargetFilter)}
                  className="py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface-variant cursor-pointer font-sans"
                >
                  <option value="ALL">Tất cả loại</option>
                  <option value="QUESTION">Câu hỏi</option>
                  <option value="ANSWER">Câu trả lời</option>
                  <option value="ACCOUNT">Tài khoản</option>
                  <option value="EXPERT">Chuyên gia</option>
                  <option value="USER">Người dùng</option>
                </select>

                <select
                  value={priorityFilter}
                  onChange={(event) => setPriorityFilter(event.target.value as PriorityFilter)}
                  className="py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface-variant cursor-pointer font-sans"
                >
                  <option value="ALL">Tất cả ưu tiên</option>
                  <option value="NORMAL">Bình thường</option>
                  <option value="HIGH">Ưu tiên cao</option>
                  <option value="URGENT">Khẩn</option>
                </select>

                <select
                  value={pageSize}
                  onChange={(event) => setPageSize(Number(event.target.value) as typeof pageSize)}
                  className="py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface-variant cursor-pointer font-sans"
                >
                  {PAGE_SIZE_OPTIONS.map((size) => (
                    <option key={size} value={size}>{size} / trang</option>
                  ))}
                </select>

                {(search || sourceFilter !== 'ALL' || priorityFilter !== 'ALL' || targetFilter !== 'ALL') && (
                  <button
                    type="button"
                    onClick={resetFilters}
                    className="py-2.5 px-4 rounded-full border border-outline-variant bg-surface text-xs font-semibold text-on-surface-variant cursor-pointer hover:bg-surface-container-low flex items-center gap-1 whitespace-nowrap"
                  >
                    <span className="material-symbols-outlined text-base">filter_alt_off</span>
                    Xóa lọc
                  </button>
                )}
              </div>
            </div>
          </div>

          {actionError && (
            <div className="mb-4 rounded-2xl border border-error-container bg-error-container/60 p-4 text-sm text-error">
              {actionError}
            </div>
          )}

          {/* Data Table */}
          <div className="bg-surface rounded-2xl p-6 shadow-md border border-surface-container-highest">
            {isLoading ? (
              <div className="py-12 text-center text-outline">Đang tải danh sách báo cáo...</div>
            ) : error ? (
              <div className="py-12 text-center text-error">{error}</div>
            ) : (
              <>
                <div className="overflow-x-auto">
                  <table className="w-full border-collapse">
                    <thead>
                      <tr className="border-b-2 border-surface-container-highest text-left">
                        {([
                          ['reason', 'LÝ DO'],
                          ['source', 'NGUỒN'],
                          ['targetType', 'LOẠI'],
                          ['contentPreview', 'NỘI DUNG / TIÊU ĐỀ'],
                          ['authorName', 'TÀI KHOẢN ĐĂNG'],
                          ['status', tab === 'PENDING' ? 'TRẠNG THÁI / LƯỢT' : 'KẾT QUẢ'],
                          ['reportedAt', 'THỜI GIAN'],
                        ] as const).map(([key, label]) => (
                          <SortableTableHeader
                            key={key}
                            label={label}
                            active={sortKey === key}
                            direction={sortDirection}
                            onClick={() => changeSort(key)}
                          />
                        ))}
                        <th scope="col" className="py-3 px-2 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">THAO TÁC</th>
                      </tr>
                    </thead>
                    <tbody>
                      {pagedItems.map((item) => (
                        <tr key={item.id} className="border-b border-surface-container-highest hover:bg-surface-bright">
                          <td className="py-3.5 px-2 cursor-pointer" onClick={() => goToDetail(item)}>
                            <div className="flex flex-wrap items-center gap-1.5">
                              <span className="rounded-full bg-[#FCE8E6] px-3 py-1 text-xs font-semibold text-[#C5221F]">
                                {formatReportReason(item.reportReason)}
                              </span>
                              {item.priority !== 'NORMAL' && (
                                <span className={`rounded-full px-2.5 py-0.5 text-[11px] font-semibold ${CASE_PRIORITY_STYLES[item.priority]}`}>
                                  {CASE_PRIORITY_LABELS[item.priority]}
                                </span>
                              )}
                            </div>
                          </td>
                          <td className="py-3.5 px-2 cursor-pointer" onClick={() => goToDetail(item)}>
                            {item.reportSource === 'AUTOMATED' ? (
                              <span className="inline-flex items-center gap-1 rounded-full bg-secondary-container px-3 py-0.5 text-xs font-semibold text-on-secondary-container">
                                <span className="material-symbols-outlined text-sm leading-none">smart_toy</span>
                                AI
                              </span>
                            ) : (
                              <span className="text-xs text-on-surface-variant font-medium">{REPORT_SOURCE_LABELS.USER}</span>
                            )}
                          </td>
                          <td className="py-3.5 px-2 cursor-pointer" onClick={() => goToDetail(item)}>
                            <span className="inline-flex items-center gap-1 py-1 px-3 rounded-full bg-surface-container-low text-primary text-xs font-semibold">
                              {TARGET_TYPE_LABELS[item.targetType]}
                            </span>
                          </td>
                          <td className="py-3.5 px-2 max-w-[340px] cursor-pointer" onClick={() => goToDetail(item)}>
                            {item.targetTitle && (
                              <div className="font-semibold text-sm text-on-surface mb-0.5 flex items-center gap-1.5">
                                <span className="material-symbols-outlined text-[15px] text-primary shrink-0">help_outline</span>
                                <span className="truncate">{item.targetTitle}</span>
                              </div>
                            )}
                            <div className="text-xs text-on-surface-variant line-clamp-2">{item.contentPreview}</div>
                          </td>
                          <td className="py-3.5 px-2 cursor-pointer" onClick={() => goToDetail(item)}>
                            <div className="flex flex-col gap-0.5 max-w-[200px]">
                              <div className="text-xs font-semibold text-on-surface flex items-center gap-1.5 truncate">
                                <span className="material-symbols-outlined text-base text-outline shrink-0">account_circle</span>
                                <span className="truncate">{item.authorName || item.authorEmail || item.authorPhone || '—'}</span>
                              </div>
                              {(item.authorEmail || item.authorPhone) && (
                                <div className="text-[11px] text-on-surface-variant flex flex-col gap-0.5 pl-5 truncate">
                                  {item.authorEmail && (
                                    <span className="truncate flex items-center gap-1 text-outline" title={item.authorEmail}>
                                      <span className="material-symbols-outlined text-[12px] shrink-0">mail</span>
                                      <span className="truncate">{item.authorEmail}</span>
                                    </span>
                                  )}
                                  {item.authorPhone && (
                                    <span className="truncate flex items-center gap-1 text-outline" title={item.authorPhone}>
                                      <span className="material-symbols-outlined text-[12px] shrink-0">phone</span>
                                      <span className="truncate">{item.authorPhone}</span>
                                    </span>
                                  )}
                                </div>
                              )}
                            </div>
                          </td>
                          <td className="py-3.5 px-2 cursor-pointer whitespace-nowrap" onClick={() => goToDetail(item)}>
                            {tab === 'PENDING' ? (
                              item.status === 'IN_REVIEW' ? (
                                <span className="rounded-full bg-tertiary-container px-3 py-1 text-xs font-semibold text-on-tertiary-container">
                                  {item.assignedModeratorId === currentUserId ? 'Bạn đang xem xét' : 'Đang xem xét'}
                                </span>
                              ) : (
                                <span className="text-xs font-semibold text-on-surface-variant bg-surface-container-low px-3 py-1 rounded-full">{item.reportCount} lượt</span>
                              )
                            ) : (
                              <span className={`rounded-full px-3 py-1 text-xs font-semibold ${
                                item.status === 'RESOLVED' ? 'bg-[#E6F4EA] text-[#137333]' : 'bg-surface-container-high text-on-surface-variant'
                              }`}>
                                {REPORT_STATUS_LABELS[item.status]}
                              </span>
                            )}
                          </td>
                          <td className="py-3.5 px-2 cursor-pointer whitespace-nowrap text-[13px] text-outline" onClick={() => goToDetail(item)}>
                            {formatDateTime(item.reportedAt)}
                          </td>
                          <td className="py-3.5 px-2">
                            <div className="flex items-center gap-1.5 justify-end">
                              <button
                                type="button"
                                onClick={() => goToDetail(item)}
                                className="h-8 py-1 px-3 rounded-lg border border-outline-variant bg-transparent cursor-pointer text-xs font-semibold text-primary flex items-center gap-1 hover:bg-surface-container-low"
                                title="Xem chi tiết"
                              >
                                <span className="material-symbols-outlined text-base">visibility</span>
                                Xem
                              </button>
                              {tab === 'PENDING' && item.status === 'PENDING' && (
                                <button
                                  type="button"
                                  disabled={claimingId === item.id}
                                  onClick={() => handleClaim(item)}
                                  className="h-8 py-1 px-4 rounded-full bg-primary text-on-primary border-0 text-xs font-semibold cursor-pointer flex items-center gap-1 hover:bg-primary/90 disabled:opacity-60"
                                >
                                  <span className="material-symbols-outlined text-base">how_to_reg</span>
                                  {claimingId === item.id ? 'Đang nhận...' : 'Nhận xử lý'}
                                </button>
                              )}
                              {tab === 'PENDING' && item.status === 'IN_REVIEW' && item.assignedModeratorId === currentUserId && (
                                <button
                                  type="button"
                                  disabled={claimingId === item.id}
                                  onClick={() => handleRelease(item)}
                                  className="h-8 py-1 px-3 rounded-lg border border-outline-variant bg-surface text-on-surface-variant text-xs font-semibold cursor-pointer flex items-center gap-1 hover:bg-surface-container-low disabled:opacity-60"
                                >
                                  <span className="material-symbols-outlined text-base">logout</span>
                                  Trả lại
                                </button>
                              )}
                            </div>
                          </td>
                        </tr>
                      ))}
                      {pagedItems.length === 0 && (
                        <tr>
                          <td colSpan={8} className="py-12 text-center text-outline">
                            {tab === 'PENDING' ? 'Không có báo cáo nào phù hợp bộ lọc.' : 'Không có báo cáo đã xử lý phù hợp bộ lọc.'}
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>

                {/* Pagination */}
                <div className="flex justify-between items-center mt-5 pt-4 border-t border-surface-container-highest">
                  <span className="text-[13px] text-outline">
                    Hiển thị {filteredItems.length === 0 ? 0 : pageStart}-{pageEnd} trong {filteredItems.length} kết quả
                  </span>
                  <div className="flex gap-1">
                    <button
                      type="button"
                      disabled={currentPage === 0}
                      onClick={() => setPage((value) => Math.max(0, value - 1))}
                      className={`w-9 h-9 rounded-full border border-outline-variant bg-surface flex items-center justify-center ${currentPage === 0 ? 'opacity-40 cursor-default' : 'cursor-pointer'}`}
                    >
                      <span className="material-symbols-outlined text-primary text-lg">chevron_left</span>
                    </button>
                    {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
                      const startPage = Math.max(0, Math.min(currentPage - 2, totalPages - 5));
                      const p = startPage + i;
                      if (p >= totalPages) return null;
                      return (
                        <button
                          key={p}
                          type="button"
                          onClick={() => setPage(p)}
                          className={`w-9 h-9 rounded-full text-sm font-semibold cursor-pointer flex items-center justify-center ${currentPage === p ? 'border-0 bg-primary text-on-primary' : 'border border-outline-variant bg-surface text-on-surface-variant'}`}
                        >
                          {p + 1}
                        </button>
                      );
                    })}
                    <button
                      type="button"
                      disabled={currentPage >= totalPages - 1}
                      onClick={() => setPage((value) => Math.min(totalPages - 1, value + 1))}
                      className={`w-9 h-9 rounded-full border border-outline-variant bg-surface flex items-center justify-center ${currentPage >= totalPages - 1 ? 'opacity-40 cursor-default' : 'cursor-pointer'}`}
                    >
                      <span className="material-symbols-outlined text-primary text-lg">chevron_right</span>
                    </button>
                  </div>
                </div>
              </>
            )}
          </div>
        </div>
      </main>

    </div>
  );
}
