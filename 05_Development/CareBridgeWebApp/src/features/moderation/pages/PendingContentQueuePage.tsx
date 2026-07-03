import { useEffect, useState, useCallback } from 'react';
import ModPortalSidebar from '../components/ModPortalSidebar';
import { fetchPendingContentQueue, moderateContentDirect } from '../services/moderationApi';
import type { PendingContentItem, ReportTargetType } from '../models/moderation';
import { TARGET_TYPE_LABELS } from '../models/moderation';

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });
}

const TABS: { label: string; value: ReportTargetType }[] = [
  { label: 'Câu hỏi mới', value: 'QUESTION' },
  { label: 'Câu trả lời mới', value: 'ANSWER' },
];

export default function PendingContentQueuePage() {
  const [tab, setTab] = useState<ReportTargetType>('QUESTION');
  const [items, setItems] = useState<PendingContentItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [actioningId, setActioningId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setIsLoading(true);
    setError('');
    try {
      const page = await fetchPendingContentQueue({ targetType: tab, size: 50 });
      setItems(page.content);
    } catch {
      setError('Không tải được danh sách nội dung chờ duyệt.');
      setItems([]);
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
                      <button
                        type="button"
                        disabled={actioningId === item.targetId}
                        onClick={() => handleApprove(item)}
                        className="px-3 py-1.5 rounded-xl bg-primary text-on-primary text-xs font-semibold disabled:opacity-50"
                      >
                        {actioningId === item.targetId ? 'Đang duyệt...' : 'Duyệt'}
                      </button>
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
