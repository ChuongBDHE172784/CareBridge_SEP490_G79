import { useEffect, useState, useCallback } from 'react';
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
import type { PendingContentItem, ModerationHistoryItem, ModerationContentDetail, ReportTargetType } from '../models/moderation';
import { TARGET_TYPE_LABELS, ACTION_TYPE_LABELS, UNDOABLE_ACTION_TYPES } from '../models/moderation';

type PendingActionType = 'HIDE' | 'REQUEST_REVISION';

const PENDING_ACTION_CONFIG: Record<PendingActionType, { title: string; reasonLabel: string; reasonPlaceholder: string; tone: 'default' | 'danger' }> = {
  HIDE: {
    title: 'Ẩn nội dung này?',
    reasonLabel: 'Lý do ẩn nội dung (bắt buộc)',
    reasonPlaceholder: 'Nhập lý do ẩn nội dung này...',
    tone: 'danger',
  },
  REQUEST_REVISION: {
    title: 'Yêu cầu tác giả chỉnh sửa?',
    reasonLabel: 'Nội dung cần chỉnh sửa (bắt buộc)',
    reasonPlaceholder: 'Mô tả nội dung cần tác giả chỉnh sửa...',
    tone: 'default',
  },
};

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });
}

type Tab = 'QUESTION' | 'ANSWER' | 'HISTORY';

const TABS: { label: string; value: Tab }[] = [
  { label: 'Câu hỏi mới', value: 'QUESTION' },
  { label: 'Câu trả lời mới', value: 'ANSWER' },
  { label: 'Đã xử lý', value: 'HISTORY' },
];

export default function PendingContentQueuePage() {
  const [tab, setTab] = useState<Tab>('QUESTION');
  const [items, setItems] = useState<PendingContentItem[]>([]);
  const [historyItems, setHistoryItems] = useState<ModerationHistoryItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [actioningId, setActioningId] = useState<string | null>(null);
  const [pendingAction, setPendingAction] = useState<{ item: PendingContentItem; type: PendingActionType } | null>(null);
  const [dialogError, setDialogError] = useState('');

  const [detailTarget, setDetailTarget] = useState<{ targetId: string; targetType: ReportTargetType } | null>(null);
  const [detailData, setDetailData] = useState<ModerationContentDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState('');
  // Set only when "Xem chi tiết" is opened from the "Đã xử lý" tab — carries the reason/moderator
  // info that ModerationContentDetail (the content itself) doesn't have, since that's action-level data.
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
        const page = await fetchModerationHistory({ size: 50 });
        setHistoryItems(page.content);
      } else {
        const page = await fetchPendingContentQueue({ targetType: tab as ReportTargetType, size: 50 });
        setItems(page.content);
      }
    } catch {
      setError(
        tab === 'HISTORY'
          ? 'Không tải được lịch sử xử lý.'
          : 'Không tải được danh sách nội dung chờ duyệt.',
      );
      setItems([]);
      setHistoryItems([]);
    } finally {
      setIsLoading(false);
    }
  }, [tab]);

  useEffect(() => { load(); }, [load]);

  const handleApprove = async (item: PendingContentItem) => {
    setActioningId(item.targetId);
    try {
      await moderateContentDirect(item.targetId, item.targetType, 'APPROVE');
      setItems((prev) => prev.filter((i) => i.targetId !== item.targetId));
    } catch {
      setError('Duyệt nội dung thất bại, vui lòng thử lại.');
    } finally {
      setActioningId(null);
    }
  };

  // First-time review may either reject unsafe content (HIDE) or return it to its author for revision.
  const openPendingAction = (item: PendingContentItem, type: PendingActionType) => {
    setDialogError('');
    setPendingAction({ item, type });
  };

  // CB-MOD-IMP-008: full (non-truncated) detail — used by "Xem chi tiết" on all 3 tabs. `historyItem`
  // is only passed from the "Đã xử lý" tab, to also surface reason/moderatorName in the dialog.
  const openDetail = async (targetId: string, targetType: ReportTargetType, historyItem?: ModerationHistoryItem) => {
    setDetailTarget({ targetId, targetType });
    setDetailData(null);
    setDetailError('');
    setDetailHistoryItem(historyItem ?? null);
    setDetailLoading(true);
    try {
      const detail = await fetchContentDetail(targetType, targetId);
      setDetailData(detail);
    } catch {
      setDetailError('Không tải được nội dung chi tiết. Vui lòng thử lại.');
    } finally {
      setDetailLoading(false);
    }
  };

  // CB-MOD-IMP-009: reverts a direct APPROVE/HIDE/LOCK action back to PENDING. Backend rejects
  // (409) if this isn't the most recent action for its target, or the status was already superseded.
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

  const confirmPendingAction = async (reason?: string) => {
    if (!pendingAction || !reason) return;
    const { item, type } = pendingAction;
    setActioningId(item.targetId);
    setDialogError('');
    try {
      await moderateContentDirect(item.targetId, item.targetType, type, reason);
      setItems((prev) => prev.filter((i) => i.targetId !== item.targetId));
      setPendingAction(null);
    } catch {
      setDialogError('Thao tác thất bại, vui lòng thử lại.');
    } finally {
      setActioningId(null);
    }
  };

  // Lock only appears for a previously approved QUESTION in audit history. ANSWER has no LOCKED
  // state, and a pending item is not a discussion that can be meaningfully locked.
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

  return (
    <div className="portal-page">
      <ModPortalSidebar />
      <main className="portal-content">
        <div className="portal-contained">
        <div className="portal-header">
          <div>
            <p className="portal-eyebrow">Kiểm duyệt</p>
            <h1 className="portal-title">Nội dung chờ duyệt lần đầu</h1>
            <p className="portal-subtitle">
              Câu hỏi và câu trả lời mới đăng, chưa từng bị báo cáo, cần duyệt trước khi hiển thị công khai.
            </p>
          </div>
        </div>

        <div className="portal-toolbar">
          {TABS.map((t) => (
            <button
              key={t.value}
              type="button"
              onClick={() => setTab(t.value)}
              className={`rounded-md px-3 py-2 text-sm font-semibold transition-colors ${
                tab === t.value ? 'bg-primary text-on-primary' : 'bg-surface text-on-surface-variant hover:bg-surface-container-low'
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>

        {isLoading ? (
          <div className="portal-empty">Đang tải...</div>
        ) : error ? (
          <div className="portal-error">{error}</div>
        ) : tab === 'HISTORY' ? (
          <div className="portal-table-card">
            {historyActionError && (
              <div className="portal-error m-4">{historyActionError}</div>
            )}
            <div className="overflow-x-auto">
              <table className="w-full min-w-[860px]">
                <thead>
                  <tr>
                    {['LOẠI', 'NỘI DUNG', 'HÀNH ĐỘNG', 'NGƯỜI XỬ LÝ', 'THỜI GIAN', ''].map((h) => (
                      <th key={h}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {historyItems.map((item) => (
                    <tr key={item.actionId}>
                      <td className="text-on-surface-variant">{TARGET_TYPE_LABELS[item.targetType]}</td>
                      <td className="max-w-[320px] truncate text-on-surface">{item.contentPreview ?? '—'}</td>
                      <td>
                        <span
                          className={`rounded-md px-2.5 py-1 text-xs font-semibold ${
                            item.actionType === 'APPROVE'
                              ? 'bg-primary-container text-on-primary-container'
                              : 'bg-error-container text-error'
                          }`}
                        >
                          {ACTION_TYPE_LABELS[item.actionType]}
                        </span>
                      </td>
                      <td className="whitespace-nowrap text-on-surface-variant">{item.moderatorName ?? '—'}</td>
                      <td className="whitespace-nowrap text-on-surface-variant">{formatDateTime(item.actionAt)}</td>
                      <td>
                        <div className="flex gap-2 flex-nowrap">
                          <button
                            type="button"
                            onClick={() => openDetail(item.targetId, item.targetType, item)}
                            className="rounded-md bg-surface-container-high px-3 py-1.5 text-xs font-semibold text-on-surface whitespace-nowrap"
                          >
                            Xem chi tiết
                          </button>
                          {UNDOABLE_ACTION_TYPES.has(item.actionType) && (
                            <button
                              type="button"
                              onClick={() => { setUndoError(''); setUndoTarget(item); }}
                              className="rounded-md bg-surface-container-highest px-3 py-1.5 text-xs font-semibold text-on-surface whitespace-nowrap"
                            >
                              Hoàn tác
                            </button>
                          )}
                          {item.targetType === 'QUESTION' && item.actionType === 'APPROVE' && (
                            <button
                              type="button"
                              disabled={lockLoadingId === item.actionId}
                              onClick={() => void openLockDialog(item)}
                              className="rounded-md bg-surface-container-highest px-3 py-1.5 text-xs font-semibold text-on-surface disabled:opacity-50 whitespace-nowrap"
                            >
                              {lockLoadingId === item.actionId ? 'Đang kiểm tra...' : 'Khóa thảo luận'}
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                  {historyItems.length === 0 && (
                    <tr><td colSpan={6} className="text-center text-outline">Chưa có nội dung nào được xử lý.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        ) : (
          <div className="portal-table-card">
            <div className="overflow-x-auto">
              <table className="w-full min-w-[720px]">
                <thead>
                  <tr>
                    {['LOẠI', 'NỘI DUNG XEM TRƯỚC', 'THỜI GIAN ĐĂNG', ''].map((h) => (
                      <th key={h}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {items.map((item) => (
                    <tr key={item.targetId}>
                      <td className="text-on-surface-variant">{TARGET_TYPE_LABELS[item.targetType]}</td>
                      <td className="max-w-[420px] truncate text-on-surface">{item.contentPreview}</td>
                      <td className="whitespace-nowrap text-on-surface-variant">{formatDateTime(item.createdAt)}</td>
                      <td>
                        <div className="flex gap-2 flex-nowrap">
                          <button
                            type="button"
                            onClick={() => openDetail(item.targetId, item.targetType)}
                            className="rounded-md bg-surface-container-high px-3 py-1.5 text-xs font-semibold text-on-surface whitespace-nowrap"
                          >
                            Xem chi tiết
                          </button>
                          <button
                            type="button"
                            disabled={actioningId === item.targetId}
                            onClick={() => handleApprove(item)}
                            className="rounded-md bg-primary px-3 py-1.5 text-xs font-semibold text-on-primary disabled:opacity-50 whitespace-nowrap"
                          >
                            {actioningId === item.targetId ? 'Đang xử lý...' : 'Duyệt'}
                          </button>
                          <button
                            type="button"
                            disabled={actioningId === item.targetId}
                            onClick={() => openPendingAction(item, 'HIDE')}
                            className="rounded-md bg-error px-3 py-1.5 text-xs font-semibold text-on-error disabled:opacity-50 whitespace-nowrap"
                          >
                            {actioningId === item.targetId ? 'Đang xử lý...' : 'Ẩn'}
                          </button>
                          <button
                            type="button"
                            disabled={actioningId === item.targetId}
                            onClick={() => openPendingAction(item, 'REQUEST_REVISION')}
                            className="rounded-md bg-surface-container-high px-3 py-1.5 text-xs font-semibold text-on-surface disabled:opacity-50 whitespace-nowrap"
                          >
                            {actioningId === item.targetId ? 'Đang xử lý...' : 'Yêu cầu sửa'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                  {items.length === 0 && (
                    <tr><td colSpan={4} className="text-center text-outline">Không có nội dung nào đang chờ duyệt lần đầu.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}
        </div>
      </main>

      <ConfirmDialog
        key={pendingAction ? `${pendingAction.item.targetId}-${pendingAction.type}` : 'none'}
        open={pendingAction !== null}
        title={pendingAction ? PENDING_ACTION_CONFIG[pendingAction.type].title : ''}
        icon={pendingAction?.type === 'HIDE' ? 'visibility_off' : 'edit_note'}
        tone={pendingAction ? PENDING_ACTION_CONFIG[pendingAction.type].tone : 'default'}
        reasonLabel={pendingAction ? PENDING_ACTION_CONFIG[pendingAction.type].reasonLabel : ''}
        reasonPlaceholder={pendingAction ? PENDING_ACTION_CONFIG[pendingAction.type].reasonPlaceholder : ''}
        confirmLabel="Xác nhận"
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
            ? `Nội dung sẽ quay lại hàng đợi chờ duyệt (${TARGET_TYPE_LABELS[undoTarget.targetType]} — "${ACTION_TYPE_LABELS[undoTarget.actionType]}").`
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
