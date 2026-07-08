import { useEffect, useState, useCallback } from 'react';
import ModPortalSidebar from '../components/ModPortalSidebar';
import { fetchPendingContentQueue, fetchModerationHistory, moderateContentDirect } from '../services/moderationApi';
import type { PendingContentItem, ModerationHistoryItem, ReportTargetType } from '../models/moderation';
import { TARGET_TYPE_LABELS, ACTION_TYPE_LABELS } from '../models/moderation';

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

  // Backend (C6, ADR-006 của UC-100) bắt buộc lý do khi HIDE — dùng window.prompt() theo đúng
  // convention đã có sẵn trong project (window.confirm() ở SafetyRuleManagementPage), không có
  // shared modal component nào để tái dùng.
  const handleHide = async (item: PendingContentItem) => {
    const reason = window.prompt('Nhập lý do ẩn nội dung này (bắt buộc):');
    if (reason === null) return;
    if (!reason.trim()) {
      setError('Cần nhập lý do để ẩn nội dung.');
      return;
    }
    setActioningId(item.targetId);
    try {
      await moderateContentDirect(item.targetId, item.targetType, 'HIDE', reason.trim());
      setItems((prev) => prev.filter((i) => i.targetId !== item.targetId));
    } catch {
      setError('Ẩn nội dung thất bại, vui lòng thử lại.');
    } finally {
      setActioningId(null);
    }
  };

  const handleLock = async (item: PendingContentItem) => {
    const reason = window.prompt('Nhập lý do khóa thảo luận này (bắt buộc):');
    if (reason === null) return;
    if (!reason.trim()) {
      setError('Cần nhập lý do để khóa thảo luận.');
      return;
    }
    setActioningId(item.targetId);
    try {
      await moderateContentDirect(item.targetId, item.targetType, 'LOCK', reason.trim());
      setItems((prev) => prev.filter((i) => i.targetId !== item.targetId));
    } catch {
      setError('Khóa thảo luận thất bại, vui lòng thử lại.');
    } finally {
      setActioningId(null);
    }
  };

  const handleRequestRevision = async (item: PendingContentItem) => {
    const reason = window.prompt('Nhập nội dung cần tác giả chỉnh sửa (bắt buộc):');
    if (reason === null) return;
    if (!reason.trim()) {
      setError('Cần nhập lý do để yêu cầu sửa.');
      return;
    }
    setActioningId(item.targetId);
    try {
      await moderateContentDirect(item.targetId, item.targetType, 'REQUEST_REVISION', reason.trim());
      setItems((prev) => prev.filter((i) => i.targetId !== item.targetId));
    } catch {
      setError('Yêu cầu sửa thất bại, vui lòng thử lại.');
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
                  {['LOẠI', 'NỘI DUNG', 'HÀNH ĐỘNG', 'LÝ DO', 'NGƯỜI XỬ LÝ', 'THỜI GIAN'].map((h) => (
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
                  </tr>
                ))}
                {historyItems.length === 0 && (
                  <tr><td colSpan={6} className="py-12 text-center text-outline">Chưa có nội dung nào được xử lý.</td></tr>
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
                          disabled={actioningId === item.targetId}
                          onClick={() => handleApprove(item)}
                          className="px-3 py-1.5 rounded-xl bg-primary text-on-primary text-xs font-semibold disabled:opacity-50"
                        >
                          {actioningId === item.targetId ? 'Đang xử lý...' : 'Duyệt'}
                        </button>
	                        <button
	                          type="button"
	                          disabled={actioningId === item.targetId}
	                          onClick={() => handleHide(item)}
                          className="px-3 py-1.5 rounded-xl bg-error text-on-error text-xs font-semibold disabled:opacity-50"
                        >
	                          {actioningId === item.targetId ? 'Đang xử lý...' : 'Ẩn'}
	                        </button>
		                        {item.targetType === 'QUESTION' && (
		                          <button
	                            type="button"
	                            disabled={actioningId === item.targetId}
	                            onClick={() => handleLock(item)}
	                            className="px-3 py-1.5 rounded-xl bg-surface-container-highest text-on-surface text-xs font-semibold disabled:opacity-50"
	                          >
	                            {actioningId === item.targetId ? 'Đang xử lý...' : 'Khóa'}
		                          </button>
		                        )}
		                        <button
		                          type="button"
		                          disabled={actioningId === item.targetId}
		                          onClick={() => handleRequestRevision(item)}
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
    </div>
  );
}
