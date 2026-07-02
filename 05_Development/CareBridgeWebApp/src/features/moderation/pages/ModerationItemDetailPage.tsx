import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ModPortalSidebar from '../components/ModPortalSidebar';
import { fetchModerationQueue, resolveReport } from '../services/moderationApi';
import type { ModerationQueueItem } from '../models/moderation';
import { TARGET_TYPE_LABELS, canHideTarget } from '../models/moderation';

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });
}

export default function ModerationItemDetailPage() {
  const { reportId } = useParams<{ reportId: string }>();
  const navigate = useNavigate();
  const [item, setItem] = useState<ModerationQueueItem | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [note, setNote] = useState('');
  const [submitting, setSubmitting] = useState<string | null>(null);
  const [actionError, setActionError] = useState('');

  const loadItem = useCallback(async () => {
    if (!reportId) return;
    setIsLoading(true);
    setError('');
    try {
      const page = await fetchModerationQueue({ size: 50 });
      const found = page.content.find((i) => i.id === reportId);
      if (!found) {
        setError('Không tìm thấy mục kiểm duyệt này trong hàng đợi hiện tại.');
      }
      setItem(found ?? null);
    } catch {
      setError('Không tải được dữ liệu kiểm duyệt. Vui lòng thử lại.');
    } finally {
      setIsLoading(false);
    }
  }, [reportId]);

  useEffect(() => { loadItem(); }, [loadItem]);

  const handleAction = async (outcome: 'APPROVE' | 'HIDE' | 'DISMISS') => {
    if (!item) return;
    setSubmitting(outcome);
    setActionError('');
    try {
      await resolveReport(item.id, outcome, note || undefined);
      navigate('/moderator/queue');
    } catch (err: unknown) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setActionError(message || 'Xử lý thất bại. Vui lòng thử lại.');
    } finally {
      setSubmitting(null);
    }
  };

  return (
    <div className="min-h-screen bg-background">
      <ModPortalSidebar />
      <div className="ml-64 min-h-screen p-8 font-sans">
        {isLoading ? (
          <div className="py-16 text-center text-outline">Đang tải...</div>
        ) : error || !item ? (
          <div className="bg-error-container rounded-2xl p-6 text-error text-sm">
            {error || 'Không tìm thấy mục kiểm duyệt.'}
          </div>
        ) : (
          <div className="grid grid-cols-[1fr_360px] gap-6">
            <div>
              <div className="flex items-center gap-3 mb-1">
                <h1 className="text-2xl font-bold text-on-surface m-0">
                  Chi tiết báo cáo #{item.id.slice(0, 8).toUpperCase()}
                </h1>
                <span className="py-1 px-3 rounded-full bg-error-container text-error text-xs font-semibold">
                  {item.reportReason}
                </span>
              </div>
              <p className="text-sm text-outline mb-5">
                {formatDateTime(item.reportedAt)} · {TARGET_TYPE_LABELS[item.targetType]} · {item.reportCount} lượt báo cáo
              </p>

              <div className="bg-surface rounded-2xl p-6 shadow-md mb-5">
                <p className="text-[15px] leading-7 text-on-surface whitespace-pre-wrap">{item.contentPreview}</p>
              </div>

              <div className="bg-surface-container-low rounded-2xl p-5 border border-outline-variant">
                <p className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-2">
                  Bối cảnh hội thoại
                </p>
                <p className="text-sm text-outline">
                  Chưa có dữ liệu — backend hiện chưa trả về bối cảnh hội thoại đầy đủ cho mục kiểm duyệt.
                </p>
              </div>
            </div>

            <div className="flex flex-col gap-4">
              <div className="bg-surface rounded-2xl p-5 shadow-md">
                <p className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-3">Hành động</p>

                <button
                  onClick={() => handleAction('APPROVE')}
                  disabled={!canHideTarget(item.targetType) || submitting !== null}
                  title={
                    !canHideTarget(item.targetType)
                      ? `Backend không hỗ trợ duyệt nội dung loại ${TARGET_TYPE_LABELS[item.targetType]}`
                      : 'Duyệt nội dung — nội dung sẽ hiển thị công khai (status = APPROVED) và báo cáo được đóng'
                  }
                  className="w-full py-3.5 mb-2.5 rounded-2xl bg-emerald-600 text-white border-0 text-sm font-semibold cursor-pointer flex items-center justify-center gap-2 disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  <span className="material-symbols-outlined text-lg">task_alt</span>
                  {submitting === 'APPROVE' ? 'Đang xử lý...' : 'Duyệt nội dung'}
                </button>

                <button
                  onClick={() => handleAction('HIDE')}
                  disabled={!canHideTarget(item.targetType) || submitting !== null}
                  title={!canHideTarget(item.targetType) ? `Backend không hỗ trợ ẩn nội dung loại ${TARGET_TYPE_LABELS[item.targetType]}` : undefined}
                  className="w-full py-3.5 mb-2.5 rounded-2xl bg-primary text-on-primary border-0 text-sm font-semibold cursor-pointer flex items-center justify-center gap-2 disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  <span className="material-symbols-outlined text-lg">visibility_off</span>
                  {submitting === 'HIDE' ? 'Đang xử lý...' : 'Ẩn nội dung'}
                </button>

                <button
                  disabled
                  title="Cần targetUserId từ backend để cảnh báo người dùng — endpoint hiện chưa hỗ trợ lấy id này từ hàng đợi"
                  className="w-full py-3.5 mb-2.5 rounded-2xl bg-error text-on-error border-0 text-sm font-semibold flex items-center justify-center gap-2 opacity-40 cursor-not-allowed"
                >
                  <span className="material-symbols-outlined text-lg">warning</span>
                  Cảnh báo người dùng
                </button>

                <button
                  onClick={() => handleAction('DISMISS')}
                  disabled={submitting !== null}
                  title="Đóng báo cáo mà không đổi trạng thái nội dung — khác với Duyệt: nếu nội dung đang PENDING, nó vẫn giữ nguyên PENDING"
                  className="w-full py-3 mb-2.5 rounded-2xl bg-transparent border border-outline-variant text-primary text-sm font-semibold cursor-pointer flex items-center justify-center gap-2 disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  <span className="material-symbols-outlined text-lg">block</span>
                  {submitting === 'DISMISS' ? 'Đang xử lý...' : 'Bỏ qua báo cáo (không đổi trạng thái)'}
                </button>

                <button
                  disabled
                  title="Backend chưa có outcome nâng cấp xử lý (escalate)"
                  className="w-full py-3 mb-4 rounded-2xl bg-transparent border border-outline-variant text-on-surface-variant text-sm font-semibold flex items-center justify-center gap-2 opacity-40 cursor-not-allowed"
                >
                  <span className="material-symbols-outlined text-lg">upgrade</span>
                  Nâng cấp xử lý
                </button>

                <p className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-2">
                  Ghi chú kiểm duyệt (Tuỳ chọn)
                </p>
                <textarea
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                  placeholder="Nhập lý do hoặc ghi chú nội bộ..."
                  rows={3}
                  className="w-full text-sm border border-outline-variant rounded-2xl p-3 resize-none outline-none font-sans"
                />
                {actionError && <p className="text-error text-xs mt-2">{actionError}</p>}
              </div>

              <div className="bg-surface rounded-2xl p-5 shadow-md">
                <p className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-3">
                  Lịch sử tài khoản
                </p>
                <p className="text-sm text-outline">
                  Chưa có dữ liệu — backend chưa cung cấp endpoint lịch sử vi phạm theo tài khoản.
                </p>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
