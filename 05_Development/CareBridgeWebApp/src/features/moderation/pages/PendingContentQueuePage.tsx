import { useCallback, useEffect, useMemo, useState } from 'react';
import ModPortalSidebar from '../components/ModPortalSidebar';
import ConfirmDialog from '../../../shared/components/ConfirmDialog';
import ContentDetailDialog from '../../../shared/components/ContentDetailDialog';
import {
  fetchPendingContentQueue,
  fetchModerationHistory,
  moderateContentDirect,
  fetchContentDetail,
  undoModerationAction,
} from '../services/moderationApi';
import type {
  ModerationContentDetail,
  ModerationHistoryItem,
  PendingContentItem,
  ReportTargetType,
} from '../models/moderation';
import { ACTION_TYPE_LABELS, TARGET_TYPE_LABELS, UNDOABLE_ACTION_TYPES } from '../models/moderation';

type PendingActionType = 'APPROVE' | 'HIDE' | 'REQUEST_REVISION';
type Tab = 'QUESTION' | 'ANSWER' | 'HISTORY';

const PAGE_SIZE_OPTIONS = [10, 20, 50] as const;

const PENDING_ACTION_CONFIG: Record<PendingActionType, {
  title: string;
  description: string;
  confirmLabel: string;
  icon: string;
  tone: 'default' | 'danger';
  reasonLabel?: string;
  reasonPlaceholder?: string;
}> = {
  APPROVE: {
    title: 'Duyệt nội dung này?',
    description: 'Nội dung sẽ chuyển sang trạng thái đã duyệt và có thể hiển thị trong cộng đồng.',
    confirmLabel: 'Duyệt',
    icon: 'check_circle',
    tone: 'default',
  },
  HIDE: {
    title: 'Ẩn nội dung này?',
    description: 'Nội dung sẽ bị ẩn khỏi cộng đồng. Hãy ghi lý do rõ ràng để phục vụ audit.',
    confirmLabel: 'Ẩn nội dung',
    icon: 'visibility_off',
    tone: 'danger',
    reasonLabel: 'Lý do ẩn nội dung (bắt buộc)',
    reasonPlaceholder: 'Nhập lý do ẩn nội dung này...',
  },
  REQUEST_REVISION: {
    title: 'Yêu cầu tác giả chỉnh sửa?',
    description: 'Nội dung sẽ quay lại tác giả để chỉnh sửa trước khi được duyệt lại.',
    confirmLabel: 'Yêu cầu sửa',
    icon: 'edit_note',
    tone: 'default',
    reasonLabel: 'Nội dung cần chỉnh sửa (bắt buộc)',
    reasonPlaceholder: 'Mô tả nội dung cần tác giả chỉnh sửa...',
  },
};

const TABS: { label: string; value: Tab; icon: string }[] = [
  { label: 'Câu hỏi mới', value: 'QUESTION', icon: 'forum' },
  { label: 'Câu trả lời mới', value: 'ANSWER', icon: 'quickreply' },
  { label: 'Đã xử lý', value: 'HISTORY', icon: 'history' },
];

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });
}

function matchesText(value: string | null | undefined, query: string): boolean {
  return (value ?? '').toLowerCase().includes(query);
}

export default function PendingContentQueuePage() {
  const [tab, setTab] = useState<Tab>('QUESTION');
  const [items, setItems] = useState<PendingContentItem[]>([]);
  const [historyItems, setHistoryItems] = useState<ModerationHistoryItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [actionFilter, setActionFilter] = useState<'ALL' | PendingActionType>('ALL');
  const [pageSize, setPageSize] = useState<(typeof PAGE_SIZE_OPTIONS)[number]>(10);
  const [page, setPage] = useState(0);
  const [actioningId, setActioningId] = useState<string | null>(null);
  const [pendingAction, setPendingAction] = useState<{ item: PendingContentItem; type: PendingActionType } | null>(null);
  const [dialogError, setDialogError] = useState('');

  const [detailTarget, setDetailTarget] = useState<{ targetId: string; targetType: ReportTargetType } | null>(null);
  const [detailData, setDetailData] = useState<ModerationContentDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState('');
  const [detailHistoryItem, setDetailHistoryItem] = useState<ModerationHistoryItem | null>(null);

  const [undoTarget, setUndoTarget] = useState<ModerationHistoryItem | null>(null);
  const [undoSubmitting, setUndoSubmitting] = useState(false);
  const [undoError, setUndoError] = useState('');
  const [lockTarget, setLockTarget] = useState<ModerationHistoryItem | null>(null);
  const [lockSubmitting, setLockSubmitting] = useState(false);
  const [lockError, setLockError] = useState('');
  const [lockLoadingId, setLockLoadingId] = useState<string | null>(null);
  const [historyActionError, setHistoryActionError] = useState('');

  const load = useCallback(async () => {
    setIsLoading(true);
    setError('');
    try {
      if (tab === 'HISTORY') {
        const result = await fetchModerationHistory({ size: 50 });
        setHistoryItems(result.content);
      } else {
        const result = await fetchPendingContentQueue({ targetType: tab as ReportTargetType, size: 50 });
        setItems(result.content);
      }
    } catch {
      setError(tab === 'HISTORY' ? 'Không tải được lịch sử xử lý.' : 'Không tải được danh sách nội dung chờ duyệt.');
      setItems([]);
      setHistoryItems([]);
    } finally {
      setIsLoading(false);
    }
  }, [tab]);

  useEffect(() => { void load(); }, [load]);
  useEffect(() => { setPage(0); }, [search, actionFilter, pageSize, tab]);

  const stats = useMemo(() => {
    const historyApprove = historyItems.filter((item) => item.actionType === 'APPROVE').length;
    const historyHide = historyItems.filter((item) => item.actionType === 'HIDE').length;
    const historyRevision = historyItems.filter((item) => item.actionType === 'REQUEST_REVISION').length;
    return {
      visiblePending: items.length,
      processed: historyItems.length,
      approved: historyApprove,
      hiddenOrRevision: historyHide + historyRevision,
    };
  }, [historyItems, items]);

  const filteredPending = useMemo(() => {
    const query = search.trim().toLowerCase();
    return items.filter((item) => {
      return query.length === 0
        || matchesText(item.contentPreview, query)
        || matchesText(TARGET_TYPE_LABELS[item.targetType], query);
    });
  }, [items, search]);

  const filteredHistory = useMemo(() => {
    const query = search.trim().toLowerCase();
    return historyItems.filter((item) => {
      const matchesAction = actionFilter === 'ALL' || item.actionType === actionFilter;
      const matchesQuery = query.length === 0
        || matchesText(item.contentPreview, query)
        || matchesText(item.reason, query)
        || matchesText(item.moderatorName, query)
        || matchesText(ACTION_TYPE_LABELS[item.actionType], query)
        || matchesText(TARGET_TYPE_LABELS[item.targetType], query);
      return matchesAction && matchesQuery;
    });
  }, [actionFilter, historyItems, search]);

  const filteredRows = tab === 'HISTORY' ? filteredHistory : filteredPending;
  const totalPages = Math.max(1, Math.ceil(filteredRows.length / pageSize));
  const currentPage = Math.min(page, totalPages - 1);
  const pagedRows = filteredRows.slice(currentPage * pageSize, currentPage * pageSize + pageSize);
  const pageStart = filteredRows.length === 0 ? 0 : currentPage * pageSize + 1;
  const pageEnd = Math.min((currentPage + 1) * pageSize, filteredRows.length);

  const openPendingAction = (item: PendingContentItem, type: PendingActionType) => {
    setDialogError('');
    setPendingAction({ item, type });
  };

  const openDetail = async (targetId: string, targetType: ReportTargetType, historyItem?: ModerationHistoryItem) => {
    setDetailTarget({ targetId, targetType });
    setDetailData(null);
    setDetailError('');
    setDetailHistoryItem(historyItem ?? null);
    setDetailLoading(true);
    try {
      setDetailData(await fetchContentDetail(targetType, targetId));
    } catch {
      setDetailError('Không tải được nội dung chi tiết. Vui lòng thử lại.');
    } finally {
      setDetailLoading(false);
    }
  };

  const confirmPendingAction = async (reason?: string) => {
    if (!pendingAction) return;
    const { item, type } = pendingAction;
    setActioningId(item.targetId);
    setDialogError('');
    try {
      await moderateContentDirect(item.targetId, item.targetType, type, reason);
      setItems((prev) => prev.filter((entry) => entry.targetId !== item.targetId));
      setPendingAction(null);
    } catch {
      setDialogError('Thao tác thất bại, vui lòng thử lại.');
    } finally {
      setActioningId(null);
    }
  };

  const confirmUndo = async () => {
    if (!undoTarget) return;
    setUndoSubmitting(true);
    setUndoError('');
    try {
      await undoModerationAction(undoTarget.actionId);
      setUndoTarget(null);
      await load();
    } catch (err: unknown) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setUndoError(message || 'Hoàn tác thất bại, vui lòng thử lại.');
    } finally {
      setUndoSubmitting(false);
    }
  };

  const confirmLock = async (reason?: string) => {
    if (!lockTarget || !reason) return;
    setLockSubmitting(true);
    setLockError('');
    try {
      await moderateContentDirect(lockTarget.targetId, 'QUESTION', 'LOCK', reason);
      setLockTarget(null);
      await load();
    } catch (err: unknown) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setLockError(message || 'Khóa thảo luận thất bại, vui lòng thử lại.');
    } finally {
      setLockSubmitting(false);
    }
  };

  const openLockDialog = async (item: ModerationHistoryItem) => {
    setLockLoadingId(item.actionId);
    setHistoryActionError('');
    try {
      const detail = await fetchContentDetail('QUESTION', item.targetId);
      if (detail.status !== 'APPROVED') {
        setHistoryActionError('Câu hỏi này không còn ở trạng thái đã duyệt nên không thể khóa.');
        return;
      }
      setLockError('');
      setLockTarget(item);
    } catch {
      setHistoryActionError('Không kiểm tra được trạng thái câu hỏi trước khi khóa. Vui lòng thử lại.');
    } finally {
      setLockLoadingId(null);
    }
  };

  const pendingConfig = pendingAction ? PENDING_ACTION_CONFIG[pendingAction.type] : null;

  return (
    <div className="portal-page">
      <ModPortalSidebar />
      <main className="portal-content font-sans">
        <div className="p-8">
          {/* Header */}
          <div className="mb-6 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            <div>
              <h1 className="text-[26px] font-bold text-on-surface m-0">Nội dung chờ duyệt lần đầu</h1>
              <p className="text-on-surface-variant text-sm mt-1">
                Câu hỏi và câu trả lời mới đăng, chưa từng bị báo cáo, cần duyệt trước khi hiển thị công khai.
              </p>
            </div>
            <button
              type="button"
              onClick={() => void load()}
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
              { label: 'Đang chờ trong tab', value: stats.visiblePending, icon: 'pending_actions' },
              { label: 'Đã xử lý gần đây', value: stats.processed, icon: 'history' },
              { label: 'Đã duyệt', value: stats.approved, icon: 'check_circle' },
              { label: 'Ẩn / yêu cầu sửa', value: stats.hiddenOrRevision, icon: 'rule' },
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
            <div className="flex flex-col md:flex-row items-center gap-3">
              <div className="flex-1 w-full relative">
                <span className="material-symbols-outlined text-outline absolute left-[14px] top-1/2 -translate-y-1/2 text-xl">search</span>
                <input
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                  placeholder="Tìm kiếm nội dung, lý do, người xử lý..."
                  className="w-full py-2.5 pr-[14px] pl-[42px] rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface outline-none font-sans"
                />
              </div>

              {tab === 'HISTORY' && (
                <select
                  value={actionFilter}
                  onChange={(event) => setActionFilter(event.target.value as typeof actionFilter)}
                  className="w-full md:w-auto py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface-variant cursor-pointer font-sans"
                >
                  <option value="ALL">Tất cả hành động</option>
                  <option value="APPROVE">Duyệt</option>
                  <option value="HIDE">Ẩn</option>
                  <option value="REQUEST_REVISION">Yêu cầu sửa</option>
                </select>
              )}

              <select
                value={pageSize}
                onChange={(event) => setPageSize(Number(event.target.value) as typeof pageSize)}
                className="w-full md:w-auto py-2.5 px-4 rounded-2xl border border-outline-variant bg-surface text-sm text-on-surface-variant cursor-pointer font-sans"
              >
                {PAGE_SIZE_OPTIONS.map((size) => (
                  <option key={size} value={size}>{size} / trang</option>
                ))}
              </select>

              {(search || actionFilter !== 'ALL') && (
                <button
                  type="button"
                  onClick={() => { setSearch(''); setActionFilter('ALL'); }}
                  className="py-2.5 px-4 rounded-full border border-outline-variant bg-surface text-xs font-semibold text-on-surface-variant cursor-pointer hover:bg-surface-container-low flex items-center gap-1 whitespace-nowrap"
                >
                  <span className="material-symbols-outlined text-base">filter_alt_off</span>
                  Xóa lọc
                </button>
              )}
            </div>
          </div>

          {historyActionError && (
            <div className="mb-4 rounded-2xl border border-error-container bg-error-container/60 p-4 text-sm text-error">
              {historyActionError}
            </div>
          )}

          {/* Table Container */}
          <div className="bg-surface rounded-2xl p-6 shadow-md border border-surface-container-highest">
            {isLoading ? (
              <div className="py-12 text-center text-outline">Đang tải dữ liệu kiểm duyệt...</div>
            ) : error ? (
              <div className="py-12 text-center text-error">{error}</div>
            ) : (
              <>
                <div className="overflow-x-auto">
                  {tab === 'HISTORY' ? (
                    <table className="w-full border-collapse">
                      <thead>
                        <tr className="border-b-2 border-surface-container-highest text-left">
                          {['LOẠI', 'NỘI DUNG', 'HÀNH ĐỘNG', 'NGƯỜI XỬ LÝ', 'LÝ DO', 'THỜI GIAN', 'THAO TÁC'].map((heading) => (
                            <th key={heading} className="py-3 px-2 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">{heading}</th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        {(pagedRows as ModerationHistoryItem[]).map((item) => (
                          <tr key={item.actionId} className="border-b border-surface-container-highest hover:bg-surface-bright">
                            <td className="py-3.5 px-2">
                              <span className="inline-flex items-center gap-1 py-1 px-3 rounded-full bg-surface-container-low text-primary text-xs font-semibold">
                                {TARGET_TYPE_LABELS[item.targetType]}
                              </span>
                            </td>
                            <td className="py-3.5 px-2 max-w-[280px]">
                              <div className="font-semibold text-sm text-on-surface truncate">{item.contentPreview ?? '—'}</div>
                            </td>
                            <td className="py-3.5 px-2">
                              <span className={`py-1 px-3 rounded-full text-xs font-semibold ${
                                item.actionType === 'APPROVE' ? 'bg-[#E6F4EA] text-[#137333]' : 'bg-[#FCE8E6] text-[#C5221F]'
                              }`}>
                                {ACTION_TYPE_LABELS[item.actionType]}
                              </span>
                            </td>
                            <td className="py-3.5 px-2 text-[13px] text-on-surface-variant whitespace-nowrap">{item.moderatorName ?? '—'}</td>
                            <td className="py-3.5 px-2 text-[13px] text-on-surface-variant max-w-[200px] truncate">{item.reason ?? '—'}</td>
                            <td className="py-3.5 px-2 text-[13px] text-outline whitespace-nowrap">{formatDateTime(item.actionAt)}</td>
                            <td className="py-3.5 px-2">
                              <div className="flex items-center gap-1.5 justify-end">
                                <button
                                  type="button"
                                  onClick={() => openDetail(item.targetId, item.targetType, item)}
                                  className="h-8 py-1 px-3 rounded-lg border border-outline-variant bg-transparent cursor-pointer text-xs font-semibold text-primary flex items-center gap-1 hover:bg-surface-container-low"
                                  title="Xem chi tiết"
                                >
                                  <span className="material-symbols-outlined text-base">visibility</span>
                                  Xem
                                </button>
                                {UNDOABLE_ACTION_TYPES.has(item.actionType) && (
                                  <button
                                    type="button"
                                    onClick={() => { setUndoError(''); setUndoTarget(item); }}
                                    className="h-8 py-1 px-3 rounded-lg border border-outline-variant bg-transparent cursor-pointer text-xs font-semibold text-on-surface-variant flex items-center gap-1 hover:bg-surface-container-low"
                                  >
                                    <span className="material-symbols-outlined text-base">undo</span>
                                    Hoàn tác
                                  </button>
                                )}
                                {item.targetType === 'QUESTION' && item.actionType === 'APPROVE' && (
                                  <button
                                    type="button"
                                    disabled={lockLoadingId === item.actionId}
                                    onClick={() => void openLockDialog(item)}
                                    className="h-8 py-1 px-3 rounded-lg border border-outline-variant bg-transparent cursor-pointer text-xs font-semibold text-on-surface-variant flex items-center gap-1 hover:bg-surface-container-low disabled:opacity-50"
                                  >
                                    <span className="material-symbols-outlined text-base">lock</span>
                                    {lockLoadingId === item.actionId ? 'Đang thử...' : 'Khóa'}
                                  </button>
                                )}
                              </div>
                            </td>
                          </tr>
                        ))}
                        {pagedRows.length === 0 && (
                          <tr><td colSpan={7} className="py-12 text-center text-outline">Không có lịch sử phù hợp bộ lọc.</td></tr>
                        )}
                      </tbody>
                    </table>
                  ) : (
                    <table className="w-full border-collapse">
                      <thead>
                        <tr className="border-b-2 border-surface-container-highest text-left">
                          {['LOẠI', 'NỘI DUNG XEM TRƯỚC', 'THỜI GIAN ĐĂNG', 'THAO TÁC'].map((heading) => (
                            <th key={heading} className="py-3 px-2 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">{heading}</th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        {(pagedRows as PendingContentItem[]).map((item) => (
                          <tr key={item.targetId} className="border-b border-surface-container-highest hover:bg-surface-bright">
                            <td className="py-3.5 px-2 whitespace-nowrap">
                              <span className="inline-flex items-center gap-1 py-1 px-3 rounded-full bg-surface-container-low text-primary text-xs font-semibold">
                                {TARGET_TYPE_LABELS[item.targetType]}
                              </span>
                            </td>
                            <td className="py-3.5 px-2 max-w-[420px]">
                              <div className="font-semibold text-sm text-on-surface line-clamp-2">{item.contentPreview}</div>
                            </td>
                            <td className="py-3.5 px-2 text-[13px] text-outline whitespace-nowrap">{formatDateTime(item.createdAt)}</td>
                            <td className="py-3.5 px-2">
                              <div className="flex items-center gap-1.5 justify-end">
                                <button
                                  type="button"
                                  onClick={() => openDetail(item.targetId, item.targetType)}
                                  className="h-8 py-1 px-3 rounded-lg border border-outline-variant bg-transparent cursor-pointer text-xs font-semibold text-primary flex items-center gap-1 hover:bg-surface-container-low"
                                  title="Xem chi tiết"
                                >
                                  <span className="material-symbols-outlined text-base">visibility</span>
                                  Xem
                                </button>
                                <button
                                  type="button"
                                  disabled={actioningId === item.targetId}
                                  onClick={() => openPendingAction(item, 'APPROVE')}
                                  className="h-8 py-1 px-4 rounded-full bg-primary text-on-primary border-0 text-xs font-semibold cursor-pointer flex items-center gap-1 hover:bg-primary/90 disabled:opacity-50"
                                >
                                  <span className="material-symbols-outlined text-base">check_circle</span>
                                  Duyệt
                                </button>
                                <button
                                  type="button"
                                  disabled={actioningId === item.targetId}
                                  onClick={() => openPendingAction(item, 'HIDE')}
                                  className="h-8 py-1 px-4 rounded-full bg-error text-on-error border-0 text-xs font-semibold cursor-pointer flex items-center gap-1 hover:bg-error/90 disabled:opacity-50"
                                >
                                  <span className="material-symbols-outlined text-base">visibility_off</span>
                                  Ẩn
                                </button>
                                <button
                                  type="button"
                                  disabled={actioningId === item.targetId}
                                  onClick={() => openPendingAction(item, 'REQUEST_REVISION')}
                                  className="h-8 py-1 px-3 rounded-lg border border-outline-variant bg-surface text-on-surface-variant text-xs font-semibold cursor-pointer flex items-center gap-1 hover:bg-surface-container-low disabled:opacity-50"
                                >
                                  <span className="material-symbols-outlined text-base">edit_note</span>
                                  Yêu cầu sửa
                                </button>
                              </div>
                            </td>
                          </tr>
                        ))}
                        {pagedRows.length === 0 && (
                          <tr><td colSpan={4} className="py-12 text-center text-outline">Không có nội dung phù hợp bộ lọc.</td></tr>
                        )}
                      </tbody>
                    </table>
                  )}
                </div>

                {/* Pagination */}
                <div className="flex justify-between items-center mt-5 pt-4 border-t border-surface-container-highest">
                  <span className="text-[13px] text-outline">
                    Hiển thị {filteredRows.length === 0 ? 0 : pageStart}-{pageEnd} trong {filteredRows.length} kết quả
                  </span>
                  <div className="flex gap-1">
                    <button
                      type="button"
                      onClick={() => setPage(p => Math.max(0, p - 1))}
                      disabled={currentPage === 0}
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
                      onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                      disabled={currentPage >= totalPages - 1}
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

      <ConfirmDialog
        key={pendingAction ? `${pendingAction.item.targetId}-${pendingAction.type}` : 'none'}
        open={pendingAction !== null}
        title={pendingConfig?.title ?? ''}
        description={pendingAction && pendingConfig ? `${pendingConfig.description} Nội dung: "${pendingAction.item.contentPreview}".` : undefined}
        icon={pendingConfig?.icon ?? 'help'}
        tone={pendingConfig?.tone ?? 'default'}
        reasonLabel={pendingConfig?.reasonLabel}
        reasonPlaceholder={pendingConfig?.reasonPlaceholder}
        confirmLabel={pendingConfig?.confirmLabel ?? 'Xác nhận'}
        submitting={pendingAction !== null && actioningId === pendingAction.item.targetId}
        errorText={dialogError}
        onConfirm={confirmPendingAction}
        onCancel={() => setPendingAction(null)}
      />

      <ConfirmDialog
        key={lockTarget ? lockTarget.actionId : 'none'}
        open={lockTarget !== null}
        title="Khóa thảo luận này?"
        description="Chỉ khóa câu hỏi đã được duyệt; bình luận mới sẽ không thể được thêm vào."
        icon="lock"
        tone="default"
        reasonLabel="Lý do khóa thảo luận (bắt buộc)"
        reasonPlaceholder="Nhập lý do khóa thảo luận..."
        confirmLabel="Khóa thảo luận"
        submitting={lockSubmitting}
        errorText={lockError}
        onConfirm={confirmLock}
        onCancel={() => setLockTarget(null)}
      />

      <ConfirmDialog
        key={undoTarget ? undoTarget.actionId : 'none'}
        open={undoTarget !== null}
        title="Hoàn tác hành động này?"
        description={
          undoTarget
            ? `Nội dung sẽ quay lại hàng đợi chờ duyệt (${TARGET_TYPE_LABELS[undoTarget.targetType]} - "${ACTION_TYPE_LABELS[undoTarget.actionType]}").`
            : undefined
        }
        icon="undo"
        tone="default"
        confirmLabel="Hoàn tác"
        submitting={undoSubmitting}
        errorText={undoError}
        onConfirm={confirmUndo}
        onCancel={() => setUndoTarget(null)}
      />

      <ContentDetailDialog
        open={detailTarget !== null}
        targetTypeLabel={detailTarget ? TARGET_TYPE_LABELS[detailTarget.targetType] : ''}
        statusLabel={detailData ? detailData.status : undefined}
        loading={detailLoading}
        errorText={detailError}
        detail={detailData}
        moderationContext={
          detailHistoryItem
            ? {
                actionTypeLabel: ACTION_TYPE_LABELS[detailHistoryItem.actionType],
                reason: detailHistoryItem.reason,
                moderatorName: detailHistoryItem.moderatorName,
                actionAt: detailHistoryItem.actionAt,
              }
            : undefined
        }
        onClose={() => { setDetailTarget(null); setDetailHistoryItem(null); }}
      />
    </div>
  );
}
