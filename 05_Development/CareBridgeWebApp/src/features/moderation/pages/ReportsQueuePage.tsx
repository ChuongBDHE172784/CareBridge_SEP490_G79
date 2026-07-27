import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ModPortalSidebar from '../components/ModPortalSidebar';
import ConfirmDialog from '../../../shared/components/ConfirmDialog';
import { useAuthStore } from '../../../shared/auth/authStore';
import { claimReport, fetchModerationQueue, releaseReport, revertReport } from '../services/moderationApi';
import type { CasePriority, ModerationQueueItem, ReportSource, ReportTargetType } from '../models/moderation';
import {
  CASE_PRIORITY_LABELS,
  CASE_PRIORITY_STYLES,
  formatReportReason,
  REPORT_SOURCE_LABELS,
  REPORT_STATUS_LABELS,
  TARGET_TYPE_LABELS,
} from '../models/moderation';

type Tab = 'PENDING' | 'PROCESSED';
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
  const [items, setItems] = useState<ModerationQueueItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionError, setActionError] = useState('');
  const [claimingId, setClaimingId] = useState<string | null>(null);

  const [revertTarget, setRevertTarget] = useState<ModerationQueueItem | null>(null);
  const [revertSubmitting, setRevertSubmitting] = useState(false);
  const [revertError, setRevertError] = useState('');

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
        || matchesText(formatReportReason(item.reportReason), query)
        || matchesText(TARGET_TYPE_LABELS[item.targetType], query)
        || matchesText(REPORT_SOURCE_LABELS[item.reportSource], query)
        || matchesText(REPORT_STATUS_LABELS[item.status], query);
    });
  }, [items, search]);

  const totalPages = Math.max(1, Math.ceil(filteredItems.length / pageSize));
  const currentPage = Math.min(page, totalPages - 1);
  const pagedItems = filteredItems.slice(currentPage * pageSize, currentPage * pageSize + pageSize);
  const pageStart = filteredItems.length === 0 ? 0 : currentPage * pageSize + 1;
  const pageEnd = Math.min((currentPage + 1) * pageSize, filteredItems.length);

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

  const confirmRevert = async (reason?: string) => {
    if (!revertTarget) return;
    setRevertSubmitting(true);
    setRevertError('');
    try {
      await revertReport(revertTarget.id, reason);
      setRevertTarget(null);
      await loadReports();
    } catch (err: unknown) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setRevertError(message || 'Hoàn tác thất bại, vui lòng thử lại.');
    } finally {
      setRevertSubmitting(false);
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
      <ModPortalSidebar />
      <main className="portal-content">
        <div className="portal-contained">
          <div className="portal-header">
            <div>
              <p className="portal-eyebrow">Kiểm duyệt</p>
              <h1 className="portal-title">Báo cáo</h1>
              <p className="portal-subtitle max-w-3xl">
                Theo dõi báo cáo do người dùng gửi và các trường hợp do AI phát hiện. AI chỉ hỗ trợ đánh giá; quyết định cuối cùng thuộc về kiểm duyệt viên.
              </p>
            </div>
            <button type="button" onClick={() => void loadReports()} className="portal-secondary-button" disabled={isLoading}>
              <span className="material-symbols-outlined text-base">refresh</span>
              Làm mới
            </button>
          </div>

          <section className="mb-5 grid gap-3 md:grid-cols-4">
            {[
              { label: tab === 'PENDING' ? 'Đang mở' : 'Đã xử lý', value: stats.total, icon: 'flag' },
              { label: 'AI phát hiện', value: stats.ai, icon: 'smart_toy' },
              { label: 'Ưu tiên cao', value: stats.urgent, icon: 'priority_high' },
              { label: 'Bạn đang nhận', value: stats.claimedByMe, icon: 'how_to_reg' },
            ].map((stat) => (
              <div key={stat.label} className="portal-card-padded">
                <div className="flex items-center justify-between gap-3">
                  <span className="text-xs font-semibold text-on-surface-variant">{stat.label}</span>
                  <span className="material-symbols-outlined text-[18px] text-outline">{stat.icon}</span>
                </div>
                <p className="portal-metric mt-2">{stat.value}</p>
              </div>
            ))}
          </section>

          <section className="portal-card-padded mb-4">
            <div className="flex flex-col gap-3 xl:flex-row xl:items-end xl:justify-between">
              <div className="flex flex-wrap gap-2">
                {TABS.map((tabItem) => (
                  <button
                    key={tabItem.value}
                    type="button"
                    onClick={() => setTab(tabItem.value)}
                    className={`inline-flex h-9 items-center gap-2 rounded-md px-3 text-xs font-semibold ${
                      tab === tabItem.value
                        ? 'bg-primary text-on-primary'
                        : 'bg-surface-container-low text-on-surface-variant hover:bg-surface-container'
                    }`}
                  >
                    <span className="material-symbols-outlined text-base">{tabItem.icon}</span>
                    {tabItem.label}
                  </button>
                ))}
              </div>
              <div className="grid flex-1 gap-3 lg:grid-cols-[1.2fr_0.7fr_0.7fr_0.7fr_0.45fr_auto]">
                <label>
                  <span className="portal-label">Tìm kiếm</span>
                  <div className="relative">
                    <span className="material-symbols-outlined pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[18px] text-outline">search</span>
                    <input value={search} onChange={(event) => setSearch(event.target.value)} className="portal-field w-full pl-9" placeholder="Tìm lý do, nội dung, trạng thái..." />
                  </div>
                </label>
                <label>
                  <span className="portal-label">Nguồn</span>
                  <select value={sourceFilter} onChange={(event) => setSourceFilter(event.target.value as SourceFilter)} className="portal-field w-full">
                    <option value="ALL">Tất cả</option>
                    <option value="USER">Người dùng</option>
                    <option value="AUTOMATED">AI phát hiện</option>
                  </select>
                </label>
                <label>
                  <span className="portal-label">Loại</span>
                  <select value={targetFilter} onChange={(event) => setTargetFilter(event.target.value as TargetFilter)} className="portal-field w-full">
                    <option value="ALL">Tất cả</option>
                    <option value="QUESTION">Câu hỏi</option>
                    <option value="ANSWER">Câu trả lời</option>
                    <option value="ACCOUNT">Tài khoản</option>
                    <option value="EXPERT">Chuyên gia</option>
                    <option value="USER">Người dùng</option>
                  </select>
                </label>
                <label>
                  <span className="portal-label">Ưu tiên</span>
                  <select value={priorityFilter} onChange={(event) => setPriorityFilter(event.target.value as PriorityFilter)} className="portal-field w-full">
                    <option value="ALL">Tất cả</option>
                    <option value="NORMAL">Bình thường</option>
                    <option value="HIGH">Ưu tiên cao</option>
                    <option value="URGENT">Khẩn</option>
                  </select>
                </label>
                <label>
                  <span className="portal-label">Mỗi trang</span>
                  <select value={pageSize} onChange={(event) => setPageSize(Number(event.target.value) as typeof pageSize)} className="portal-field w-full">
                    {PAGE_SIZE_OPTIONS.map((size) => <option key={size} value={size}>{size}</option>)}
                  </select>
                </label>
                <button type="button" onClick={resetFilters} className="portal-secondary-button self-end">
                  <span className="material-symbols-outlined text-base">filter_alt_off</span>
                  Xóa lọc
                </button>
              </div>
            </div>
          </section>

          {actionError && <div className="portal-error mb-4">{actionError}</div>}

          {isLoading ? (
            <div className="portal-empty">Đang tải danh sách báo cáo...</div>
          ) : error ? (
            <div className="portal-error">{error}</div>
          ) : (
            <section className="portal-table-card">
              <div className="flex flex-col gap-2 border-b border-outline-variant/70 p-4 md:flex-row md:items-center md:justify-between">
                <div>
                  <h2 className="text-sm font-semibold text-on-surface">{tab === 'PENDING' ? 'Báo cáo cần xử lý' : 'Báo cáo đã đóng'}</h2>
                  <p className="mt-1 text-xs text-on-surface-variant">Hiển thị {pageStart}-{pageEnd} trong {filteredItems.length} báo cáo phù hợp.</p>
                </div>
                <span className="rounded-md bg-surface-container-low px-2.5 py-1 text-xs font-semibold text-on-surface-variant">
                  {tab === 'PENDING' ? 'PENDING + IN_REVIEW' : 'RESOLVED + DISMISSED'}
                </span>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[1160px]">
                  <thead>
                    <tr>
                      {['Lý do', 'Nguồn', 'Loại', 'Nội dung xem trước', tab === 'PENDING' ? 'Trạng thái / số lượt' : 'Kết quả', 'Thời gian', ''].map((heading) => <th key={heading}>{heading}</th>)}
                    </tr>
                  </thead>
                  <tbody>
                    {pagedItems.map((item) => (
                      <tr key={item.id}>
                        <td className="cursor-pointer" onClick={() => goToDetail(item)}>
                          <div className="flex flex-wrap items-center gap-1.5">
                            <span className="rounded-md bg-error-container px-2.5 py-1 text-xs font-semibold text-error">{formatReportReason(item.reportReason)}</span>
                            {item.priority !== 'NORMAL' && (
                              <span className={`rounded-md px-2 py-0.5 text-xs font-semibold ${CASE_PRIORITY_STYLES[item.priority]}`}>{CASE_PRIORITY_LABELS[item.priority]}</span>
                            )}
                          </div>
                        </td>
                        <td className="cursor-pointer" onClick={() => goToDetail(item)}>
                          {item.reportSource === 'AUTOMATED' ? (
                            <span className="inline-flex items-center gap-1 rounded-md bg-secondary-container px-2 py-0.5 text-xs font-semibold text-on-secondary-container">
                              <span className="material-symbols-outlined text-sm leading-none">smart_toy</span>
                              AI
                            </span>
                          ) : (
                            <span className="text-xs text-on-surface-variant">{REPORT_SOURCE_LABELS.USER}</span>
                          )}
                        </td>
                        <td className="cursor-pointer text-on-surface-variant" onClick={() => goToDetail(item)}>{TARGET_TYPE_LABELS[item.targetType]}</td>
                        <td className="max-w-[360px] cursor-pointer truncate font-medium text-on-surface" onClick={() => goToDetail(item)}>{item.contentPreview}</td>
                        <td className="cursor-pointer" onClick={() => goToDetail(item)}>
                          {tab === 'PENDING' ? (
                            item.status === 'IN_REVIEW' ? (
                              <span className="rounded-md bg-tertiary-container px-2 py-0.5 text-xs font-semibold text-on-tertiary-container">
                                {item.assignedModeratorId === currentUserId ? 'Bạn đang xem xét' : 'Đang xem xét'}
                              </span>
                            ) : (
                              <span className="text-on-surface-variant">{item.reportCount} lượt</span>
                            )
                          ) : (
                            <span className={`rounded-md px-2.5 py-1 text-xs font-semibold ${
                              item.status === 'RESOLVED' ? 'bg-primary-container text-on-primary-container' : 'bg-surface-container-high text-on-surface-variant'
                            }`}>
                              {REPORT_STATUS_LABELS[item.status]}
                            </span>
                          )}
                        </td>
                        <td className="cursor-pointer whitespace-nowrap text-on-surface-variant" onClick={() => goToDetail(item)}>{formatDateTime(item.reportedAt)}</td>
                        <td>
                          <div className="flex justify-end gap-2">
                            <button type="button" onClick={() => goToDetail(item)} className="portal-secondary-button h-8 whitespace-nowrap">Xem</button>
                            {tab === 'PENDING' && item.status === 'PENDING' && (
                              <button type="button" disabled={claimingId === item.id} onClick={() => handleClaim(item)} className="portal-primary-button h-8 whitespace-nowrap disabled:opacity-60">
                                {claimingId === item.id ? 'Đang nhận...' : 'Nhận xử lý'}
                              </button>
                            )}
                            {tab === 'PENDING' && item.status === 'IN_REVIEW' && item.assignedModeratorId === currentUserId && (
                              <button type="button" disabled={claimingId === item.id} onClick={() => handleRelease(item)} className="portal-secondary-button h-8 whitespace-nowrap disabled:opacity-60">Trả lại</button>
                            )}
                            {tab === 'PROCESSED' && (
                              <button type="button" onClick={() => { setRevertError(''); setRevertTarget(item); }} className="portal-secondary-button h-8 whitespace-nowrap">Hoàn tác</button>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                    {pagedItems.length === 0 && (
                      <tr><td colSpan={7} className="text-center text-outline">{tab === 'PENDING' ? 'Không có báo cáo nào phù hợp bộ lọc.' : 'Không có báo cáo đã xử lý phù hợp bộ lọc.'}</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
              <div className="flex flex-col gap-3 border-t border-outline-variant/70 p-4 md:flex-row md:items-center md:justify-between">
                <p className="text-xs text-on-surface-variant">Trang {currentPage + 1} / {totalPages}</p>
                <div className="flex items-center gap-2">
                  <button type="button" disabled={currentPage === 0} onClick={() => setPage((value) => Math.max(0, value - 1))} className="portal-secondary-button">Trước</button>
                  <button type="button" disabled={currentPage >= totalPages - 1} onClick={() => setPage((value) => Math.min(totalPages - 1, value + 1))} className="portal-secondary-button">Sau</button>
                </div>
              </div>
            </section>
          )}
        </div>
      </main>

      <ConfirmDialog
        key={revertTarget ? revertTarget.id : 'none'}
        open={revertTarget !== null}
        title="Hoàn tác báo cáo này?"
        description={
          revertTarget
            ? `Báo cáo sẽ quay lại hàng đợi để xử lý lại (${TARGET_TYPE_LABELS[revertTarget.targetType]} - ${REPORT_STATUS_LABELS[revertTarget.status]}).`
            : undefined
        }
        icon="undo"
        tone="default"
        confirmLabel="Hoàn tác"
        submitting={revertSubmitting}
        errorText={revertError}
        onConfirm={confirmRevert}
        onCancel={() => setRevertTarget(null)}
      />
    </div>
  );
}
