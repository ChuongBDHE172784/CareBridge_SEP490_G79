import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import ConfirmDialog from '../../../shared/components/ConfirmDialog';
import {
  fetchPendingContentQueue,
  fetchModerationHistory,
  moderateContentDirect,
  fetchContentDetail,
} from '../services/moderationApi';
import type {
  ModerationHistoryItem,
  PendingContentItem,
  ReportTargetType,
} from '../models/moderation';
import { ACTION_TYPE_LABELS, TARGET_TYPE_LABELS } from '../models/moderation';
import { SortableTableHeader, type SortDirection } from '../../contentManagement/components/SortableTableHeader';
import { nextSortDirection, sortRows } from '../../contentManagement/utils/tableSorting';
import { fetchAiModerationStatus } from '../../aiRuleManagement/services/aiModerationPolicyApi';

type PendingActionType = 'APPROVE' | 'HIDE' | 'REQUEST_REVISION';
type Tab = 'QUESTION' | 'ANSWER' | 'HISTORY';
type PendingSortKey = 'targetType' | 'contentPreview' | 'createdAt';
type HistorySortKey = 'targetType' | 'contentPreview' | 'actionType' | 'moderatorName' | 'reason' | 'actionAt';

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
  const navigate = useNavigate();
  const [tab, setTab] = useState<Tab>('QUESTION');
  const [items, setItems] = useState<PendingContentItem[]>([]);
  const [historyItems, setHistoryItems] = useState<ModerationHistoryItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [actionFilter, setActionFilter] = useState<'ALL' | PendingActionType>('ALL');
  const [pageSize, setPageSize] = useState<(typeof PAGE_SIZE_OPTIONS)[number]>(10);
  const [page, setPage] = useState(0);
  const [pendingSortKey, setPendingSortKey] = useState<PendingSortKey>('createdAt');
  const [pendingSortDirection, setPendingSortDirection] = useState<SortDirection>('desc');
  const [historySortKey, setHistorySortKey] = useState<HistorySortKey>('actionAt');
  const [historySortDirection, setHistorySortDirection] = useState<SortDirection>('desc');
  const [actioningId, setActioningId] = useState<string | null>(null);
  const [pendingAction, setPendingAction] = useState<{ item: PendingContentItem; type: PendingActionType } | null>(null);
  const [dialogError, setDialogError] = useState('');

  const [lockTarget, setLockTarget] = useState<ModerationHistoryItem | null>(null);
  const [lockSubmitting, setLockSubmitting] = useState(false);
  const [lockError, setLockError] = useState('');
  const [lockLoadingId, setLockLoadingId] = useState<string | null>(null);
  const [historyActionError, setHistoryActionError] = useState('');

  const [aiEnabled, setAiEnabled] = useState<boolean | null>(null);

  const load = useCallback(async () => {
    setIsLoading(true);
    setError('');
    fetchAiModerationStatus().then(st => setAiEnabled(st.enabled && st.businessToggleEnabled)).catch(() => setAiEnabled(null));
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
      if (!item.actionType) return false;
      const matchesAction = actionFilter === 'ALL' || item.actionType === actionFilter;
      const actionLabel = ACTION_TYPE_LABELS[item.actionType] ?? item.actionType;
      const matchesQuery = query.length === 0
        || matchesText(item.contentPreview, query)
        || matchesText(item.reason, query)
        || matchesText(item.moderatorName, query)
        || matchesText(actionLabel, query)
        || matchesText(TARGET_TYPE_LABELS[item.targetType], query);
      return matchesAction && matchesQuery;
    });
  }, [actionFilter, historyItems, search]);

  const sortedPending = useMemo(() => sortRows(filteredPending, pendingSortDirection, (item) => {
    switch (pendingSortKey) {
      case 'targetType': return TARGET_TYPE_LABELS[item.targetType];
      case 'contentPreview': return item.contentPreview;
      case 'createdAt': return new Date(item.createdAt).getTime();
    }
  }), [filteredPending, pendingSortDirection, pendingSortKey]);

  const sortedHistory = useMemo(() => sortRows(filteredHistory, historySortDirection, (item) => {
    switch (historySortKey) {
      case 'targetType': return TARGET_TYPE_LABELS[item.targetType];
      case 'contentPreview': return item.contentPreview;
      case 'actionType': return item.actionType ? (ACTION_TYPE_LABELS[item.actionType] ?? item.actionType) : '';
      case 'moderatorName': return item.moderatorName;
      case 'reason': return item.reason;
      case 'actionAt': return new Date(item.actionAt).getTime();
    }
  }), [filteredHistory, historySortDirection, historySortKey]);

  const filteredRows = tab === 'HISTORY' ? sortedHistory : sortedPending;
  const totalPages = Math.max(1, Math.ceil(filteredRows.length / pageSize));
  const currentPage = Math.min(page, totalPages - 1);
  const pagedRows = filteredRows.slice(currentPage * pageSize, currentPage * pageSize + pageSize);
  const pageStart = filteredRows.length === 0 ? 0 : currentPage * pageSize + 1;
  const pageEnd = Math.min((currentPage + 1) * pageSize, filteredRows.length);

  const changePendingSort = (key: PendingSortKey) => {
    setPendingSortDirection(nextSortDirection(pendingSortKey, key, pendingSortDirection));
    setPendingSortKey(key);
    setPage(0);
  };

  const changeHistorySort = (key: HistorySortKey) => {
    setHistorySortDirection(nextSortDirection(historySortKey, key, historySortDirection));
    setHistorySortKey(key);
    setPage(0);
  };

  const openPendingAction = (item: PendingContentItem, type: PendingActionType) => {
    setDialogError('');
    setPendingAction({ item, type });
  };

  const openDetail = (targetId: string, targetType: ReportTargetType, historyItem?: ModerationHistoryItem) => {
    navigate(`/moderator/pending-content/${targetType}/${targetId}`, {
      state: { historyItem },
    });
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
      <main className="font-sans">
        <div className="p-8">
          {/* Header */}
          <div className="mb-6 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            <div>
              <div className="flex flex-wrap items-center gap-3">
                <h1 className="text-[26px] font-bold text-on-surface m-0">Nội dung chờ duyệt thủ công</h1>
                {aiEnabled === false ? (
                  <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-200">
                    <span className="w-2 h-2 rounded-full bg-amber-500" />
                    AI Moderation: Đã tắt
                  </span>
                ) : (
                  <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
                    <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
                    AI Auto-Approve: Tự động phê duyệt
                  </span>
                )}
              </div>
              <p className="text-on-surface-variant text-sm mt-1">
                {aiEnabled === false
                  ? 'Hệ thống AI Moderation đang TẮT. Toàn bộ nội dung mới từ người dùng được giữ lại chờ Moderator duyệt thủ công trước khi xuất bản.'
                  : 'Các nội dung cần duyệt tay trước khi công khai (khi AI đánh giá nội dung ở mức nghi vấn nhẹ).'}
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
              { label: 'Chờ duyệt', value: stats.visiblePending, icon: 'pending_actions' },
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
                  className={`inline-flex items-center gap-2 py-2 px-4 rounded-full text-xs font-semibold cursor-pointer transition-colors ${tab === tabItem.value
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
                          {([
                            ['targetType', 'LOẠI'],
                            ['contentPreview', 'NỘI DUNG'],
                            ['actionType', 'HÀNH ĐỘNG'],
                            ['moderatorName', 'NGƯỜI XỬ LÝ'],
                            ['reason', 'LÝ DO'],
                            ['actionAt', 'THỜI GIAN'],
                          ] as const).map(([key, label]) => (
                            <SortableTableHeader
                              key={key}
                              label={label}
                              active={historySortKey === key}
                              direction={historySortDirection}
                              onClick={() => changeHistorySort(key)}
                            />
                          ))}
                          <th scope="col" className="py-3 px-2 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">THAO TÁC</th>
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
                              <span className={`py-1 px-3 rounded-full text-xs font-semibold ${item.actionType === 'APPROVE' ? 'bg-[#E6F4EA] text-[#137333]' : 'bg-[#FCE8E6] text-[#C5221F]'
                                }`}>
                                {item.actionType ? (ACTION_TYPE_LABELS[item.actionType] ?? item.actionType) : '—'}
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
                          {([
                            ['targetType', 'LOẠI'],
                            ['contentPreview', 'NỘI DUNG XEM TRƯỚC'],
                            ['createdAt', 'THỜI GIAN ĐĂNG'],
                          ] as const).map(([key, label]) => (
                            <SortableTableHeader
                              key={key}
                              label={label}
                              active={pendingSortKey === key}
                              direction={pendingSortDirection}
                              onClick={() => changePendingSort(key)}
                            />
                          ))}
                          <th scope="col" className="py-3 px-2 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">THAO TÁC</th>
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
                              <div className="text-sm text-on-surface line-clamp-2">{item.contentPreview}</div>
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
                          <tr>
                            <td colSpan={4} className="py-14 text-center">
                              {aiEnabled === false ? (
                                <div className="max-w-md mx-auto flex flex-col items-center justify-center">
                                  <div className="w-12 h-12 rounded-full bg-amber-50 text-amber-600 flex items-center justify-center mb-3 border border-amber-200">
                                    <span className="material-symbols-outlined text-2xl">rule</span>
                                  </div>
                                  <h3 className="text-base font-bold text-on-surface mb-1">Không có nội dung chờ duyệt thủ công</h3>
                                  <p className="text-xs text-on-surface-variant leading-relaxed">
                                    Hệ thống AI Moderation đang <strong>TẮT</strong> (chế độ Kiểm duyệt thủ công trước khi đăng). Các bài viết/câu hỏi mới từ người dùng sẽ xuất hiện tại đây để bạn duyệt trước khi công khai.
                                  </p>
                                </div>
                              ) : (
                                <div className="max-w-md mx-auto flex flex-col items-center justify-center">
                                  <div className="w-12 h-12 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center mb-3 border border-emerald-200">
                                    <span className="material-symbols-outlined text-2xl">verified_user</span>
                                  </div>
                                  <h3 className="text-base font-bold text-on-surface mb-1">Không có nội dung chờ duyệt thủ công</h3>
                                  <p className="text-xs text-on-surface-variant leading-relaxed">
                                    Hệ thống AI Moderation đang tự động phê duyệt các bài viết an toàn. Những câu hỏi/bài viết chứa dấu hiệu vi phạm sẽ được AI tạo báo cáo và chuyển sang trang{' '}
                                    <button
                                      type="button"
                                      onClick={() => navigate('/moderator/reports')}
                                      className="text-primary font-semibold underline hover:text-primary/80 cursor-pointer"
                                    >
                                      Báo cáo vi phạm
                                    </button>.
                                  </p>
                                </div>
                              )}
                            </td>
                          </tr>
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

    </div>
  );
}
