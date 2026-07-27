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
      <main className="portal-content">
        <div className="portal-contained">
          <div className="portal-header">
            <div>
              <p className="portal-eyebrow">Kiểm duyệt</p>
              <h1 className="portal-title">Nội dung chờ duyệt lần đầu</h1>
              <p className="portal-subtitle max-w-3xl">
                Câu hỏi và câu trả lời mới đăng, chưa từng bị báo cáo, cần duyệt trước khi hiển thị công khai.
              </p>
            </div>
            <button type="button" onClick={() => void load()} className="portal-secondary-button" disabled={isLoading}>
              <span className="material-symbols-outlined text-base">refresh</span>
              Làm mới
            </button>
          </div>

          <section className="mb-5 grid gap-3 md:grid-cols-4">
            {[
              { label: 'Đang chờ trong tab', value: stats.visiblePending, icon: 'pending_actions' },
              { label: 'Đã xử lý gần đây', value: stats.processed, icon: 'history' },
              { label: 'Đã duyệt', value: stats.approved, icon: 'check_circle' },
              { label: 'Ẩn / yêu cầu sửa', value: stats.hiddenOrRevision, icon: 'rule' },
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
              <div className="grid flex-1 gap-3 md:grid-cols-[1fr_0.7fr_0.5fr_auto]">
                <label>
                  <span className="portal-label">Tìm kiếm</span>
                  <div className="relative">
                    <span className="material-symbols-outlined pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[18px] text-outline">search</span>
                    <input
                      value={search}
                      onChange={(event) => setSearch(event.target.value)}
                      className="portal-field w-full pl-9"
                      placeholder="Tìm nội dung, lý do, người xử lý..."
                    />
                  </div>
                </label>
                <label>
                  <span className="portal-label">Hành động</span>
                  <select
                    value={actionFilter}
                    onChange={(event) => setActionFilter(event.target.value as typeof actionFilter)}
                    className="portal-field w-full"
                    disabled={tab !== 'HISTORY'}
                  >
                    <option value="ALL">Tất cả</option>
                    <option value="APPROVE">Duyệt</option>
                    <option value="HIDE">Ẩn</option>
                    <option value="REQUEST_REVISION">Yêu cầu sửa</option>
                  </select>
                </label>
                <label>
                  <span className="portal-label">Mỗi trang</span>
                  <select value={pageSize} onChange={(event) => setPageSize(Number(event.target.value) as typeof pageSize)} className="portal-field w-full">
                    {PAGE_SIZE_OPTIONS.map((size) => <option key={size} value={size}>{size}</option>)}
                  </select>
                </label>
                <button
                  type="button"
                  onClick={() => { setSearch(''); setActionFilter('ALL'); }}
                  className="portal-secondary-button self-end"
                >
                  <span className="material-symbols-outlined text-base">filter_alt_off</span>
                  Xóa lọc
                </button>
              </div>
            </div>
          </section>

          {historyActionError && <div className="portal-error mb-4">{historyActionError}</div>}

          {isLoading ? (
            <div className="portal-empty">Đang tải dữ liệu kiểm duyệt...</div>
          ) : error ? (
            <div className="portal-error">{error}</div>
          ) : (
            <section className="portal-table-card">
              <div className="flex flex-col gap-2 border-b border-outline-variant/70 p-4 md:flex-row md:items-center md:justify-between">
                <div>
                  <h2 className="text-sm font-semibold text-on-surface">{tab === 'HISTORY' ? 'Lịch sử xử lý' : 'Danh sách cần duyệt'}</h2>
                  <p className="mt-1 text-xs text-on-surface-variant">Hiển thị {pageStart}-{pageEnd} trong {filteredRows.length} mục phù hợp.</p>
                </div>
                <span className="rounded-md bg-surface-container-low px-2.5 py-1 text-xs font-semibold text-on-surface-variant">
                  {tab === 'QUESTION' ? 'Câu hỏi' : tab === 'ANSWER' ? 'Câu trả lời' : 'Đã xử lý'}
                </span>
              </div>
              <div className="overflow-x-auto">
                {tab === 'HISTORY' ? (
                  <table className="w-full min-w-[1080px]">
                    <thead>
                      <tr>
                        {['Loại', 'Nội dung', 'Hành động', 'Người xử lý', 'Lý do', 'Thời gian', ''].map((heading) => <th key={heading}>{heading}</th>)}
                      </tr>
                    </thead>
                    <tbody>
                      {(pagedRows as ModerationHistoryItem[]).map((item) => (
                        <tr key={item.actionId}>
                          <td className="text-on-surface-variant">{TARGET_TYPE_LABELS[item.targetType]}</td>
                          <td className="max-w-[320px] truncate font-medium text-on-surface">{item.contentPreview ?? '—'}</td>
                          <td>
                            <span className={`rounded-md px-2.5 py-1 text-xs font-semibold ${
                              item.actionType === 'APPROVE' ? 'bg-primary-container text-on-primary-container' : 'bg-error-container text-error'
                            }`}>
                              {ACTION_TYPE_LABELS[item.actionType]}
                            </span>
                          </td>
                          <td className="whitespace-nowrap text-on-surface-variant">{item.moderatorName ?? '—'}</td>
                          <td className="max-w-[260px] truncate text-on-surface-variant">{item.reason ?? '—'}</td>
                          <td className="whitespace-nowrap text-on-surface-variant">{formatDateTime(item.actionAt)}</td>
                          <td>
                            <div className="flex justify-end gap-2">
                              <button type="button" onClick={() => openDetail(item.targetId, item.targetType, item)} className="portal-secondary-button h-8 whitespace-nowrap">Xem</button>
                              {UNDOABLE_ACTION_TYPES.has(item.actionType) && (
                                <button type="button" onClick={() => { setUndoError(''); setUndoTarget(item); }} className="portal-secondary-button h-8 whitespace-nowrap">Hoàn tác</button>
                              )}
                              {item.targetType === 'QUESTION' && item.actionType === 'APPROVE' && (
                                <button
                                  type="button"
                                  disabled={lockLoadingId === item.actionId}
                                  onClick={() => void openLockDialog(item)}
                                  className="portal-secondary-button h-8 whitespace-nowrap disabled:opacity-50"
                                >
                                  {lockLoadingId === item.actionId ? 'Đang kiểm tra...' : 'Khóa'}
                                </button>
                              )}
                            </div>
                          </td>
                        </tr>
                      ))}
                      {pagedRows.length === 0 && <tr><td colSpan={7} className="text-center text-outline">Không có lịch sử phù hợp bộ lọc.</td></tr>}
                    </tbody>
                  </table>
                ) : (
                  <table className="w-full min-w-[900px]">
                    <thead>
                      <tr>
                        {['Loại', 'Nội dung xem trước', 'Thời gian đăng', ''].map((heading) => <th key={heading}>{heading}</th>)}
                      </tr>
                    </thead>
                    <tbody>
                      {(pagedRows as PendingContentItem[]).map((item) => (
                        <tr key={item.targetId}>
                          <td className="whitespace-nowrap text-on-surface-variant">{TARGET_TYPE_LABELS[item.targetType]}</td>
                          <td className="max-w-[480px] truncate font-medium text-on-surface">{item.contentPreview}</td>
                          <td className="whitespace-nowrap text-on-surface-variant">{formatDateTime(item.createdAt)}</td>
                          <td>
                            <div className="flex justify-end gap-2">
                              <button type="button" onClick={() => openDetail(item.targetId, item.targetType)} className="portal-secondary-button h-8 whitespace-nowrap">Xem</button>
                              <button
                                type="button"
                                disabled={actioningId === item.targetId}
                                onClick={() => openPendingAction(item, 'APPROVE')}
                                className="portal-primary-button h-8 whitespace-nowrap disabled:opacity-50"
                              >
                                Duyệt
                              </button>
                              <button
                                type="button"
                                disabled={actioningId === item.targetId}
                                onClick={() => openPendingAction(item, 'HIDE')}
                                className="rounded-md bg-error px-3.5 text-xs font-semibold text-on-error disabled:opacity-50"
                              >
                                Ẩn
                              </button>
                              <button
                                type="button"
                                disabled={actioningId === item.targetId}
                                onClick={() => openPendingAction(item, 'REQUEST_REVISION')}
                                className="portal-secondary-button h-8 whitespace-nowrap disabled:opacity-50"
                              >
                                Yêu cầu sửa
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                      {pagedRows.length === 0 && <tr><td colSpan={4} className="text-center text-outline">Không có nội dung phù hợp bộ lọc.</td></tr>}
                    </tbody>
                  </table>
                )}
              </div>
              <div className="flex flex-col gap-3 border-t border-outline-variant/70 p-4 md:flex-row md:items-center md:justify-between">
                <p className="text-xs text-on-surface-variant">Trang {currentPage + 1} / {totalPages}</p>
                <div className="flex items-center gap-2">
                  <button type="button" onClick={() => setPage((value) => Math.max(0, value - 1))} disabled={currentPage === 0} className="portal-secondary-button">Trước</button>
                  <button type="button" onClick={() => setPage((value) => Math.min(totalPages - 1, value + 1))} disabled={currentPage >= totalPages - 1} className="portal-secondary-button">Sau</button>
                </div>
              </div>
            </section>
          )}
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
