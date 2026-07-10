import { useEffect, useState, useCallback } from 'react';
import ModPortalSidebar from '../components/ModPortalSidebar';
import ConfirmDialog from '../../../shared/components/ConfirmDialog';
import ContentDetailDialog from '../../../shared/components/ContentDetailDialog';
import {
  fetchPendingContentQueue,
  fetchModerationHistory,
  moderateContentDirect,
  fetchContentDetail,
} from '../services/moderationApi';
import type { PendingContentItem, ModerationHistoryItem, ModerationContentDetail, ReportTargetType } from '../models/moderation';
import { TARGET_TYPE_LABELS, ACTION_TYPE_LABELS } from '../models/moderation';

type PendingActionType = 'HIDE' | 'LOCK' | 'REQUEST_REVISION';

const PENDING_ACTION_CONFIG: Record<PendingActionType, { title: string; reasonLabel: string; reasonPlaceholder: string; tone: 'default' | 'danger' }> = {
  HIDE: {
    title: 'Ẩn nội dung này?',
    reasonLabel: 'Lý do ẩn nội dung (bắt buộc)',
    reasonPlaceholder: 'Nhập lý do ẩn nội dung này...',
    tone: 'danger',
  },
  LOCK: {
    title: 'Khóa thảo luận này?',
    reasonLabel: 'Lý do khóa thảo luận (bắt buộc)',
    reasonPlaceholder: 'Nhập lý do khóa thảo luận này...',
    tone: 'default',
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

  // Backend (C6, ADR-006 của UC-100) bắt buộc lý do khi HIDE/LOCK/REQUEST_REVISION — thu thập
  // qua ConfirmDialog (shared/components/ConfirmDialog.tsx) thay vì window.prompt().
  const openPendingAction = (item: PendingContentItem, type: PendingActionType) => {
    setDialogError('');
    setPendingAction({ item, type });
  };

  // CB-MOD-IMP-008: full (non-truncated) detail — used by "Xem chi tiết" on all 3 tabs.
  const openDetail = async (targetId: string, targetType: ReportTargetType) => {
    setDetailTarget({ targetId, targetType });
    setDetailData(null);
    setDetailError('');
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

  return (
    <div className="min-h-screen bg-background">
      <ModPortalSidebar />
      <div className="ml-64 min-h-screen p-8 font-sans">
        <div className="flex items-center gap-2 mb-1">
          <span className="material-symbols-outlined text-primary text-2xl">fact_check</span>
          <h1 className="text-2xl font-bold text-on-surface m-0">Nội dung chờ duyệt lần đầu</h1>
        </div>
        <p className="text-sm text-outline ml-8 mb-6">
          Câu hỏi và câu trả lời mới đăng, chưa từng bị báo cáo — cần duyệt trước khi hiển thị công khai trên trang cộng đồng.
        </p>

        <div className="flex gap-2 mb-4">
          {TABS.map((t) => (
            <button
              key={t.value}
              type="button"
              onClick={() => setTab(t.value)}
              className={`px-4 py-2 rounded-xl text-sm font-semibold transition-colors ${
                tab === t.value ? 'bg-primary text-on-primary' : 'bg-surface text-on-surface-variant hover:bg-surface-container-low'
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>

        {isLoading ? (
          <div className="py-16 text-center text-outline">Đang tải...</div>
        ) : error ? (
          <div className="bg-error-container rounded-2xl p-6 text-error text-sm">{error}</div>
        ) : tab === 'HISTORY' ? (
          <div className="bg-surface rounded-2xl shadow-md overflow-hidden">
            <table className="w-full border-collapse">
              <thead>
                <tr className="border-b-2 border-surface-container-highest text-left bg-surface-container-low">
                  {['LOẠI', 'NỘI DUNG', 'HÀNH ĐỘNG', 'LÝ DO', 'NGƯỜI XỬ LÝ', 'THỜI GIAN', ''].map((h) => (
                    <th key={h} className="py-3 px-4 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {historyItems.map((item) => (
                  <tr key={item.actionId} className="border-b border-surface-container-highest hover:bg-surface-container-low">
                    <td className="py-3.5 px-4 text-sm text-on-surface-variant">{TARGET_TYPE_LABELS[item.targetType]}</td>
                    <td className="py-3.5 px-4 text-sm text-on-surface max-w-[320px] truncate">{item.contentPreview ?? '—'}</td>
                    <td className="py-3.5 px-4">
                      <span
                        className={`py-1 px-3 rounded-full text-xs font-semibold ${
                          item.actionType === 'APPROVE'
                            ? 'bg-primary-container text-on-primary-container'
                            : 'bg-error-container text-error'
                        }`}
                      >
                        {ACTION_TYPE_LABELS[item.actionType]}
                      </span>
                    </td>
                    <td className="py-3.5 px-4 text-sm text-on-surface-variant max-w-[240px] truncate">{item.reason ?? '—'}</td>
                    <td className="py-3.5 px-4 text-sm text-on-surface-variant whitespace-nowrap">{item.moderatorName ?? '—'}</td>
                    <td className="py-3.5 px-4 text-sm text-on-surface-variant whitespace-nowrap">{formatDateTime(item.actionAt)}</td>
                    <td className="py-3.5 px-4">
                      <button
                        type="button"
                        onClick={() => openDetail(item.targetId, item.targetType)}
                        className="px-3 py-1.5 rounded-xl bg-surface-container-high text-on-surface text-xs font-semibold"
                      >
                        Xem chi tiết
                      </button>
                    </td>
                  </tr>
                ))}
                {historyItems.length === 0 && (
                  <tr><td colSpan={7} className="py-12 text-center text-outline">Chưa có nội dung nào được xử lý.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="bg-surface rounded-2xl shadow-md overflow-hidden">
            <table className="w-full border-collapse">
              <thead>
                <tr className="border-b-2 border-surface-container-highest text-left bg-surface-container-low">
                  {['LOẠI', 'NỘI DUNG XEM TRƯỚC', 'THỜI GIAN ĐĂNG', ''].map((h) => (
                    <th key={h} className="py-3 px-4 text-[11px] font-semibold text-outline uppercase tracking-[0.05em]">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr key={item.targetId} className="border-b border-surface-container-highest hover:bg-surface-container-low">
                    <td className="py-3.5 px-4 text-sm text-on-surface-variant">{TARGET_TYPE_LABELS[item.targetType]}</td>
                    <td className="py-3.5 px-4 text-sm text-on-surface max-w-[420px] truncate">{item.contentPreview}</td>
                    <td className="py-3.5 px-4 text-sm text-on-surface-variant whitespace-nowrap">{formatDateTime(item.createdAt)}</td>
                    <td className="py-3.5 px-4">
                      <div className="flex gap-2">
                        <button
                          type="button"
                          onClick={() => openDetail(item.targetId, item.targetType)}
                          className="px-3 py-1.5 rounded-xl bg-surface-container-high text-on-surface text-xs font-semibold"
                        >
                          Xem chi tiết
                        </button>
                        <button
                          type="button"
                          disabled={actioningId === item.targetId}
                          onClick={() => handleApprove(item)}
                          className="px-3 py-1.5 rounded-xl bg-primary text-on-primary text-xs font-semibold disabled:opacity-50"
                        >
                          {actioningId === item.targetId ? 'Đang xử lý...' : 'Duyệt'}
                        </button>
                        <button
                          type="button"
                          disabled={actioningId === item.targetId}
                          onClick={() => openPendingAction(item, 'HIDE')}
                          className="px-3 py-1.5 rounded-xl bg-error text-on-error text-xs font-semibold disabled:opacity-50"
                        >
                          {actioningId === item.targetId ? 'Đang xử lý...' : 'Ẩn'}
                        </button>
                        {item.targetType === 'QUESTION' && (
                          <button
                            type="button"
                            disabled={actioningId === item.targetId}
                            onClick={() => openPendingAction(item, 'LOCK')}
                            className="px-3 py-1.5 rounded-xl bg-surface-container-highest text-on-surface text-xs font-semibold disabled:opacity-50"
                          >
                            {actioningId === item.targetId ? 'Đang xử lý...' : 'Khóa'}
                          </button>
                        )}
                        <button
                          type="button"
                          disabled={actioningId === item.targetId}
                          onClick={() => openPendingAction(item, 'REQUEST_REVISION')}
                          className="px-3 py-1.5 rounded-xl bg-surface-container-high text-on-surface text-xs font-semibold disabled:opacity-50"
                        >
                          {actioningId === item.targetId ? 'Đang xử lý...' : 'Yêu cầu sửa'}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {items.length === 0 && (
                  <tr><td colSpan={4} className="py-12 text-center text-outline">Không có nội dung nào đang chờ duyệt lần đầu.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <ConfirmDialog
        key={pendingAction ? `${pendingAction.item.targetId}-${pendingAction.type}` : 'none'}
        open={pendingAction !== null}
        title={pendingAction ? PENDING_ACTION_CONFIG[pendingAction.type].title : ''}
        icon={pendingAction?.type === 'HIDE' ? 'visibility_off' : pendingAction?.type === 'LOCK' ? 'lock' : 'edit_note'}
        tone={pendingAction ? PENDING_ACTION_CONFIG[pendingAction.type].tone : 'default'}
        reasonLabel={pendingAction ? PENDING_ACTION_CONFIG[pendingAction.type].reasonLabel : ''}
        reasonPlaceholder={pendingAction ? PENDING_ACTION_CONFIG[pendingAction.type].reasonPlaceholder : ''}
        confirmLabel="Xác nhận"
        submitting={pendingAction !== null && actioningId === pendingAction.item.targetId}
        errorText={dialogError}
        onConfirm={confirmPendingAction}
        onCancel={() => setPendingAction(null)}
      />

      <ContentDetailDialog
        open={detailTarget !== null}
        targetTypeLabel={detailTarget ? TARGET_TYPE_LABELS[detailTarget.targetType] : ''}
        statusLabel={detailData ? detailData.status : undefined}
        loading={detailLoading}
        errorText={detailError}
        detail={detailData}
        onClose={() => setDetailTarget(null)}
      />
    </div>
  );
}
