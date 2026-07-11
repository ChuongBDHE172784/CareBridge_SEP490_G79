import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ModPortalSidebar from '../components/ModPortalSidebar';
import ConfirmDialog from '../../../shared/components/ConfirmDialog';
import { fetchModerationQueue, resolveReport } from '../services/moderationApi';
import type { ModerationQueueItem } from '../models/moderation';
import { TARGET_TYPE_LABELS, canEnforceAccount, canHideTarget } from '../models/moderation';
import type { ResolutionOutcome } from '../models/moderation';

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });
}

// Outcomes that mutate content/account state get a confirmation step before firing —
// APPROVE and DISMISS stay immediate since they're non-destructive.
const CONFIRM_CONFIG: Partial<Record<ResolutionOutcome, { title: string; icon: string; tone: 'default' | 'danger' }>> = {
  HIDE: { title: 'Ẩn/xóa nội dung này?', icon: 'visibility_off', tone: 'danger' },
  LOCK: { title: 'Khóa thảo luận này?', icon: 'lock', tone: 'default' },
  REQUEST_REVISION: { title: 'Yêu cầu tác giả chỉnh sửa?', icon: 'edit_note', tone: 'default' },
  WARN: { title: 'Cảnh cáo người dùng này?', icon: 'warning', tone: 'default' },
  RESTRICT: { title: 'Hạn chế đăng bài 7 ngày?', icon: 'speaker_notes_off', tone: 'danger' },
  SUSPEND: { title: 'Đình chỉ tài khoản 7 ngày?', icon: 'person_off', tone: 'danger' },
};

export default function ContentReportDetailPage() {
  const { reportId } = useParams<{ reportId: string }>();
  const navigate = useNavigate();
  const [item, setItem] = useState<ModerationQueueItem | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [reason, setReason] = useState('');
  const [submitting, setSubmitting] = useState<string | null>(null);
  const [actionError, setActionError] = useState('');
  const [confirmingOutcome, setConfirmingOutcome] = useState<ResolutionOutcome | null>(null);

  const loadItem = useCallback(async () => {
    if (!reportId) return;
    setIsLoading(true);
    setError('');
    try {
      const page = await fetchModerationQueue({ targetType: 'CONTENT', size: 50 });
      let found = page.content.find((i) => i.id === reportId);
      if (!found) {
        const questionAnswerPage = await fetchModerationQueue({ size: 50 });
        found = questionAnswerPage.content.find((i) => i.id === reportId);
      }
      if (!found) setError('Không tìm thấy báo cáo này trong hàng đợi hiện tại.');
      setItem(found ?? null);
    } catch {
      setError('Không tải được dữ liệu báo cáo. Vui lòng thử lại.');
    } finally {
      setIsLoading(false);
    }
  }, [reportId]);

  useEffect(() => { loadItem(); }, [loadItem]);

  // Outcomes in CONFIRM_CONFIG open a ConfirmDialog first (misclick protection on destructive
  // actions); the rest (APPROVE/DISMISS) execute immediately as before.
  const handleAction = (outcome: ResolutionOutcome) => {
    if (!item) return;
    if (['HIDE', 'LOCK', 'REQUEST_REVISION', 'WARN', 'SUSPEND', 'RESTRICT'].includes(outcome) && !reason.trim()) {
      setActionError('Cần nhập ghi chú/lý do cho hành động này.');
      return;
    }
    setActionError('');
    if (CONFIRM_CONFIG[outcome]) {
      setConfirmingOutcome(outcome);
      return;
    }
    void executeAction(outcome);
  };

  const executeAction = async (outcome: ResolutionOutcome) => {
    if (!item) return;
    const expiresAt = outcome === 'SUSPEND' || outcome === 'RESTRICT'
      ? new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString()
      : undefined;
    setSubmitting(outcome);
    setActionError('');
    try {
      await resolveReport(item.id, outcome, reason || undefined, expiresAt);
      navigate('/moderator/reports');
    } catch (err: unknown) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setActionError(message || 'Xử lý thất bại. Vui lòng thử lại.');
      setConfirmingOutcome(null);
    } finally {
      setSubmitting(null);
    }
  };

  return (
    <div className="min-h-screen bg-background">
      <ModPortalSidebar />
      <div className="ml-64 min-h-screen p-8 font-sans">
        <button
          onClick={() => navigate('/moderator/reports')}
          className="inline-flex items-center gap-1.5 text-sm font-semibold text-on-surface cursor-pointer mb-4"
        >
          <span className="material-symbols-outlined text-lg">arrow_back</span>
          Trở lại danh sách
        </button>

        {isLoading ? (
          <div className="py-16 text-center text-outline">Đang tải...</div>
        ) : error || !item ? (
          <div className="bg-error-container rounded-2xl p-6 text-error text-sm">
            {error || 'Không tìm thấy báo cáo.'}
          </div>
        ) : (
          <>
            <div className="flex items-center gap-3 mb-1">
              <span className="py-1 px-3 rounded-full bg-error-container text-error text-xs font-semibold">
                {item.reportReason}
              </span>
              <span className="text-xs text-outline">ID: #{item.id.slice(0, 8).toUpperCase()}</span>
            </div>
            <h1 className="text-2xl font-bold text-on-surface mt-1 mb-6">Chi tiết báo cáo</h1>

            <div className="grid grid-cols-[1fr_360px] gap-6">
              <div>
                <div className="bg-surface rounded-2xl p-6 shadow-md mb-5">
                  <div className="flex items-center gap-2 mb-3">
                    <span className="material-symbols-outlined text-primary text-xl">flag</span>
                    <h2 className="text-base font-bold text-on-surface m-0">Lý do báo cáo</h2>
                  </div>
                  <p className="text-sm text-on-surface-variant mb-4">{item.reportReason}</p>
                  <div className="bg-surface-container-low rounded-2xl p-4">
                    <p className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-1">
                      Chi tiết từ người dùng
                    </p>
                    <p className="text-sm text-on-surface italic">"{item.reportReason}"</p>
                  </div>
                  <div className="flex justify-between items-center mt-4 text-xs text-outline">
                    <span>Người báo cáo: Ẩn danh</span>
                    <span>Báo cáo lúc: {formatDateTime(item.reportedAt)}</span>
                  </div>
                </div>

                <div className="bg-surface rounded-2xl p-6 shadow-md">
                  <div className="flex items-center gap-2 mb-3">
                    <span className="material-symbols-outlined text-primary text-xl">article</span>
                    <h2 className="text-base font-bold text-on-surface m-0">Nội dung bị báo cáo</h2>
                  </div>
                  <div className="bg-surface-container-low rounded-2xl p-4 border border-outline-variant">
                    <p className="text-sm text-outline mb-1">{TARGET_TYPE_LABELS[item.targetType]}</p>
                    <p className="text-[15px] leading-7 text-on-surface whitespace-pre-wrap">{item.contentPreview}</p>
                  </div>
                </div>
              </div>

              <div className="flex flex-col gap-4">
                <div className="bg-surface rounded-2xl p-5 shadow-md">
                  <p className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-3">
                    Xử lý vi phạm
                  </p>

                  <button
                    onClick={() => handleAction('APPROVE')}
                    disabled={!canHideTarget(item.targetType) || submitting !== null}
                    title={
                      !canHideTarget(item.targetType)
                        ? `Backend không hỗ trợ duyệt cho loại ${TARGET_TYPE_LABELS[item.targetType]}`
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
                    title={!canHideTarget(item.targetType) ? `Backend không hỗ trợ xoá cho loại ${TARGET_TYPE_LABELS[item.targetType]}` : 'Xoá khỏi hệ thống (thực thi qua outcome HIDE)'}
                    className="w-full py-3.5 mb-2.5 rounded-2xl bg-error text-on-error border-0 text-sm font-semibold cursor-pointer flex items-center justify-center gap-2 disabled:opacity-40 disabled:cursor-not-allowed"
                  >
                    <span className="material-symbols-outlined text-lg">delete</span>
                    {submitting === 'HIDE' ? 'Đang xử lý...' : 'Xóa bài viết'}
                  </button>

                  <button
                    onClick={() => handleAction('HIDE')}
                    disabled={!canHideTarget(item.targetType) || submitting !== null}
                    title={!canHideTarget(item.targetType) ? `Backend không hỗ trợ ẩn cho loại ${TARGET_TYPE_LABELS[item.targetType]}` : 'Ẩn khỏi hệ thống (thực thi qua outcome HIDE — cùng hành động với Xóa bài viết)'}
                    className="w-full py-3 mb-2.5 rounded-2xl bg-primary-container text-on-primary-container border-0 text-sm font-semibold cursor-pointer flex items-center justify-center gap-2 disabled:opacity-40 disabled:cursor-not-allowed"
                  >
                    <span className="material-symbols-outlined text-lg">visibility_off</span>
	                    Ẩn nội dung
	                  </button>

		                  <button
		                    onClick={() => handleAction('LOCK')}
	                    disabled={item.targetType !== 'QUESTION' || submitting !== null}
	                    title={item.targetType !== 'QUESTION' ? 'Chỉ câu hỏi cộng đồng hỗ trợ khóa thảo luận' : 'Khóa câu hỏi và đóng báo cáo'}
	                    className="w-full py-3 mb-2.5 rounded-2xl bg-surface-container-highest border border-outline-variant text-on-surface text-sm font-semibold cursor-pointer flex items-center justify-center gap-2 disabled:opacity-40 disabled:cursor-not-allowed"
	                  >
	                    <span className="material-symbols-outlined text-lg">lock</span>
		                    {submitting === 'LOCK' ? 'Đang xử lý...' : 'Khóa thảo luận'}
		                  </button>

		                  <button
		                    onClick={() => handleAction('REQUEST_REVISION')}
		                    disabled={!canHideTarget(item.targetType) || submitting !== null}
		                    title={!canHideTarget(item.targetType) ? `Backend không hỗ trợ yêu cầu sửa cho loại ${TARGET_TYPE_LABELS[item.targetType]}` : 'Yêu cầu tác giả sửa lại; nội dung giữ/chuyển về trạng thái PENDING'}
		                    className="w-full py-3 mb-2.5 rounded-2xl bg-surface-container-high border border-outline-variant text-on-surface text-sm font-semibold cursor-pointer flex items-center justify-center gap-2 disabled:opacity-40 disabled:cursor-not-allowed"
		                  >
		                    <span className="material-symbols-outlined text-lg">edit_note</span>
		                    {submitting === 'REQUEST_REVISION' ? 'Đang xử lý...' : 'Yêu cầu sửa'}
		                  </button>

		                  <button
	                    onClick={() => handleAction('WARN')}
	                    disabled={!canEnforceAccount(item.targetType) || submitting !== null}
	                    title={!canEnforceAccount(item.targetType) ? 'Loại báo cáo này không có tài khoản chịu xử lý' : 'Cảnh cáo tài khoản liên quan và đóng báo cáo'}
	                    className="w-full py-3 mb-2.5 rounded-2xl bg-transparent border border-outline-variant text-on-surface text-sm font-semibold cursor-pointer flex items-center justify-center gap-2 disabled:opacity-40 disabled:cursor-not-allowed"
	                  >
	                    <span className="material-symbols-outlined text-lg">warning</span>
	                    {submitting === 'WARN' ? 'Đang xử lý...' : 'Cảnh cáo người dùng'}
	                  </button>

	                  <button
	                    onClick={() => handleAction('RESTRICT')}
	                    disabled={!canEnforceAccount(item.targetType) || submitting !== null}
	                    title={!canEnforceAccount(item.targetType) ? 'Loại báo cáo này không có tài khoản chịu xử lý' : 'Hạn chế đăng cộng đồng trong 7 ngày'}
	                    className="w-full py-3 mb-2.5 rounded-2xl bg-primary-container text-on-primary-container border-0 text-sm font-semibold cursor-pointer flex items-center justify-center gap-2 disabled:opacity-40 disabled:cursor-not-allowed"
	                  >
	                    <span className="material-symbols-outlined text-lg">speaker_notes_off</span>
	                    {submitting === 'RESTRICT' ? 'Đang xử lý...' : 'Hạn chế đăng 7 ngày'}
	                  </button>

	                  <button
	                    onClick={() => handleAction('SUSPEND')}
	                    disabled={!canEnforceAccount(item.targetType) || submitting !== null}
	                    title={!canEnforceAccount(item.targetType) ? 'Loại báo cáo này không có tài khoản chịu xử lý' : 'Đình chỉ tài khoản trong 7 ngày'}
	                    className="w-full py-3 mb-2.5 rounded-2xl bg-error-container text-error border-0 text-sm font-semibold cursor-pointer flex items-center justify-center gap-2 disabled:opacity-40 disabled:cursor-not-allowed"
	                  >
	                    <span className="material-symbols-outlined text-lg">person_off</span>
	                    {submitting === 'SUSPEND' ? 'Đang xử lý...' : 'Đình chỉ 7 ngày'}
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
                    title="Backend chưa có outcome chuyển tuyến (escalate)"
                    className="w-full py-3 mb-4 rounded-2xl bg-transparent border border-outline-variant text-on-surface-variant text-sm font-semibold flex items-center justify-center gap-2 opacity-40 cursor-not-allowed"
                  >
                    <span className="material-symbols-outlined text-lg">forward</span>
                    Chuyển tuyến
                  </button>

                  <textarea
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    placeholder="Ghi chú xử lý (tuỳ chọn)..."
                    rows={3}
                    className="w-full text-sm border border-outline-variant rounded-2xl p-3 resize-none outline-none font-sans"
                  />
                  {actionError && <p className="text-error text-xs mt-2">{actionError}</p>}
                </div>

                <div className="bg-surface rounded-2xl p-5 shadow-md">
                  <p className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-3">
                    Lịch sử báo cáo
                  </p>
                  <p className="text-sm text-outline">
                    Tổng số lượt báo cáo trên mục tiêu này: <strong>{item.reportCount}</strong>
                  </p>
                  <p className="text-xs text-outline mt-2">
                    Chi tiết từng báo cáo tương tự chưa được backend cung cấp.
                  </p>
                </div>
              </div>
            </div>
          </>
        )}
      </div>

      <ConfirmDialog
        open={confirmingOutcome !== null}
        title={confirmingOutcome ? CONFIRM_CONFIG[confirmingOutcome]!.title : ''}
        description={reason.trim() ? `Ghi chú: "${reason.trim()}"` : undefined}
        icon={confirmingOutcome ? CONFIRM_CONFIG[confirmingOutcome]!.icon : 'help'}
        tone={confirmingOutcome ? CONFIRM_CONFIG[confirmingOutcome]!.tone : 'default'}
        confirmLabel="Xác nhận"
        submitting={confirmingOutcome !== null && submitting === confirmingOutcome}
        errorText={confirmingOutcome !== null ? actionError : ''}
        onConfirm={() => confirmingOutcome && executeAction(confirmingOutcome)}
        onCancel={() => setConfirmingOutcome(null)}
      />
    </div>
  );
}
