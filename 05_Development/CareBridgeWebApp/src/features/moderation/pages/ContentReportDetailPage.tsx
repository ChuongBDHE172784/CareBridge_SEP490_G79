import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ConfirmDialog from '../../../shared/components/ConfirmDialog';
import { claimReport, fetchAiAssessment, fetchModerationQueue, fetchRelatedReports, releaseReport, resolveReport } from '../services/moderationApi';
import type { AiAssessment, ModerationQueueItem } from '../models/moderation';
import type { RelatedReportItem } from '../models/moderation';
import { formatReportReason, REPORT_SOURCE_LABELS, REPORT_STATUS_LABELS, TARGET_TYPE_LABELS, canEnforceAccount, canHideTarget } from '../models/moderation';
import type { ResolutionOutcome } from '../models/moderation';
import RelatedReportsCard from '../components/RelatedReportsCard';
import AiAssessmentCard from '../components/AiAssessmentCard';
import { useAuthStore } from '../../../shared/auth/authStore';

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
  ESCALATE: { title: 'Chuyển tuyến xử lý lên Quản trị viên hệ thống (System Admin)?', icon: 'forward', tone: 'default' },
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
  const [relatedReports, setRelatedReports] = useState<RelatedReportItem[]>([]);
  const [relatedTotal, setRelatedTotal] = useState(0);
  const [relatedPage, setRelatedPage] = useState(0);
  const [relatedLoading, setRelatedLoading] = useState(false);
  const [relatedError, setRelatedError] = useState(false);

  const currentUserId = useAuthStore((state) => state.user?.id ?? null);
  const [assessment, setAssessment] = useState<AiAssessment | null>(null);
  const [claimBusy, setClaimBusy] = useState(false);

  // A report can be PENDING, RESOLVED, or DISMISSED by the time this page is opened (e.g. from the
  // "Đã xử lý" tab) — the backend defaults `status` to PENDING when omitted, so all 3 must be
  // queried explicitly to find the report regardless of its current state.
  const loadItem = useCallback(async () => {
    if (!reportId) return;
    setIsLoading(true);
    setError('');
    try {
      const [pending, inReview, resolved, dismissed] = await Promise.all([
        fetchModerationQueue({ status: 'PENDING', size: 50 }),
        fetchModerationQueue({ status: 'IN_REVIEW', size: 50 }),
        fetchModerationQueue({ status: 'RESOLVED', size: 50 }),
        fetchModerationQueue({ status: 'DISMISSED', size: 50 }),
      ]);
      const found = [...pending.content, ...inReview.content, ...resolved.content, ...dismissed.content]
        .find((i) => i.id === reportId);
      if (!found) setError('Không tìm thấy báo cáo này trong hàng đợi hiện tại.');
      setItem(found ?? null);
    } catch {
      setError('Không tải được dữ liệu báo cáo. Vui lòng thử lại.');
    } finally {
      setIsLoading(false);
    }
  }, [reportId]);

  useEffect(() => { loadItem(); }, [loadItem]);

  // CB-MOD-IMP-016: latest AI assessment for this report (null = purely user-reported case)
  useEffect(() => {
    if (!reportId) return;
    let active = true;
    void fetchAiAssessment(reportId)
      .then((result) => { if (active) setAssessment(result); })
      .catch(() => { if (active) setAssessment(null); });
    return () => { active = false; };
  }, [reportId]);

  const handleClaim = async () => {
    if (!item) return;
    setClaimBusy(true);
    setActionError('');
    try {
      await claimReport(item.id);
      await loadItem();
    } catch (err: unknown) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setActionError(message || 'Không thể nhận xử lý báo cáo này (có thể đã có người nhận).');
      await loadItem();
    } finally {
      setClaimBusy(false);
    }
  };

  const handleRelease = async () => {
    if (!item) return;
    setClaimBusy(true);
    setActionError('');
    try {
      await releaseReport(item.id);
      await loadItem();
    } catch (err: unknown) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setActionError(message || 'Không thể trả lại báo cáo này.');
    } finally {
      setClaimBusy(false);
    }
  };

  const claimedByMe = item?.status === 'IN_REVIEW' && item.assignedModeratorId === currentUserId;
  const claimedByOther = item?.status === 'IN_REVIEW' && item.assignedModeratorId !== currentUserId;

  useEffect(() => {
    if (!reportId) return;
    let active = true;
    setRelatedLoading(true);
    setRelatedError(false);
    void fetchRelatedReports(reportId, { page: relatedPage }).then((result) => {
      if (!active) return;
      setRelatedReports(result.content);
      setRelatedTotal(result.totalElements);
    }).catch(() => { if (active) setRelatedError(true); }).finally(() => { if (active) setRelatedLoading(false); });
    return () => { active = false; };
  }, [reportId, relatedPage]);

  // Outcomes in CONFIRM_CONFIG open a ConfirmDialog first (misclick protection on destructive
  // actions); the rest (APPROVE/DISMISS) execute immediately as before.
  const handleAction = (outcome: ResolutionOutcome) => {
    if (!item) return;
    if (['HIDE', 'LOCK', 'REQUEST_REVISION', 'WARN', 'SUSPEND', 'RESTRICT', 'ESCALATE'].includes(outcome) && !reason.trim()) {
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
    <div className="portal-page">
      <main className="font-sans">
        <div className="p-8">
        {isLoading ? (
          <div className="portal-empty">Đang tải...</div>
        ) : error || !item ? (
          <div className="portal-error">
            {error || 'Không tìm thấy báo cáo.'}
          </div>
        ) : (
          <>
            <div className="mb-6 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
              <div>
                <div className="mb-2 flex items-center gap-3">
                  <span className="rounded-full bg-error-container px-3 py-1 text-xs font-semibold text-error">
                    {formatReportReason(item.reportReason)}
                  </span>
                  {item.reportSource === 'AUTOMATED' && (
                    <span className="inline-flex items-center gap-1 rounded-full bg-secondary-container px-3 py-1 text-xs font-semibold text-on-secondary-container">
                      <span className="material-symbols-outlined text-sm leading-none">smart_toy</span>
                      {REPORT_SOURCE_LABELS.AUTOMATED}
                    </span>
                  )}
                  <span className="text-xs font-mono text-outline">ID: #{item.id.slice(0, 8).toUpperCase()}</span>
                </div>
                <h1 className="text-[26px] font-bold text-on-surface m-0">Chi tiết báo cáo vi phạm</h1>
              </div>
              <button
                type="button"
                onClick={() => navigate('/moderator/reports')}
                className="inline-flex items-center gap-2 py-2.5 px-5 rounded-full bg-surface border border-outline-variant text-on-surface-variant text-sm font-semibold cursor-pointer hover:bg-surface-container-low self-start md:self-auto"
              >
                <span className="material-symbols-outlined text-lg">arrow_back</span>
                Trở lại danh sách
              </button>
            </div>

            <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
              <div>
                <div className="bg-surface rounded-2xl p-6 shadow-sm border border-surface-container-highest mb-6">
                  <div className="flex items-center gap-2 mb-3">
                    <span className="material-symbols-outlined text-primary text-xl">flag</span>
                    <h2 className="text-base font-bold text-on-surface m-0">Lý do báo cáo</h2>
                  </div>
                  <p className="text-sm text-on-surface-variant mb-4">{formatReportReason(item.reportReason)}</p>
                  <div className="rounded-xl bg-surface-container-low p-4">
                    <p className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-1">
                      Chi tiết từ người dùng
                    </p>
                    <p className="text-sm text-on-surface italic">"{formatReportReason(item.reportReason)}"</p>
                  </div>
                  <div className="flex justify-between items-center mt-4 text-xs text-outline">
                    <span>
                      {item.reportSource === 'AUTOMATED' && !item.reporterUserId
                        ? 'Nguồn: Hệ thống AI phát hiện (không có người báo cáo)'
                        : 'Người báo cáo: Ẩn danh'}
                    </span>
                    <span>Báo cáo lúc: {formatDateTime(item.reportedAt)}</span>
                  </div>
                </div>

                <div className="bg-surface rounded-2xl p-6 shadow-sm border border-surface-container-highest">
                  <div className="flex items-center gap-2 mb-3">
                    <span className="material-symbols-outlined text-primary text-xl">article</span>
                    <h2 className="text-base font-bold text-on-surface m-0">Nội dung bị báo cáo</h2>
                  </div>
                  <div className="rounded-xl border border-outline-variant bg-surface-container-low p-4">
                    <div className="flex flex-wrap items-center justify-between gap-2 border-b border-outline-variant/60 pb-3 mb-3">
                      <span className="inline-flex items-center gap-1 py-0.5 px-2.5 rounded-full bg-surface text-primary text-xs font-semibold">
                        {TARGET_TYPE_LABELS[item.targetType]}
                      </span>
                      {(item.authorName || item.authorEmail || item.authorPhone) && (
                        <div className="text-xs text-on-surface-variant flex flex-wrap items-center gap-2 font-medium">
                          <span className="material-symbols-outlined text-sm text-outline">person</span>
                          <span>Tài khoản: <strong className="text-on-surface font-semibold">{item.authorName || '—'}</strong></span>
                          {item.authorEmail && (
                            <span className="text-outline">({item.authorEmail})</span>
                          )}
                          {item.authorPhone && (
                            <span className="text-outline">• SĐT: {item.authorPhone}</span>
                          )}
                        </div>
                      )}
                    </div>
                    {item.targetTitle && (
                      <h3 className="text-base font-bold text-on-surface mb-2 mt-0">
                        {item.targetTitle}
                      </h3>
                    )}
                    <p className="text-[15px] leading-7 text-on-surface whitespace-pre-wrap m-0">{item.contentPreview}</p>
                  </div>
                </div>
              </div>

              <div className="flex flex-col gap-4">
                {item.status === 'PENDING' && (
                  <button
                    type="button"
                    disabled={claimBusy}
                    onClick={handleClaim}
                    className="flex h-11 w-full items-center justify-center gap-2 rounded-2xl bg-primary text-sm font-semibold text-on-primary shadow-sm hover:opacity-95 disabled:opacity-60 cursor-pointer"
                  >
                    <span className="material-symbols-outlined text-lg">assignment_ind</span>
                    {claimBusy ? 'Đang nhận...' : 'Nhận xử lý báo cáo này'}
                  </button>
                )}
                {claimedByMe && (
                  <button
                    type="button"
                    disabled={claimBusy}
                    onClick={handleRelease}
                    className="flex h-11 w-full items-center justify-center gap-2 rounded-2xl bg-surface-container-highest text-sm font-semibold text-on-surface hover:bg-surface-container-high disabled:opacity-60 cursor-pointer"
                  >
                    <span className="material-symbols-outlined text-lg">assignment_return</span>
                    {claimBusy ? 'Đang trả...' : 'Trả lại hàng đợi'}
                  </button>
                )}
                {claimedByOther ? (
                  <div className="bg-surface rounded-2xl p-6 shadow-sm border border-surface-container-highest">
                    <p className="mb-2 text-[11px] font-semibold uppercase tracking-[0.05em] text-outline">Đang xem xét</p>
                    <p className="m-0 text-sm text-on-surface-variant">
                      Báo cáo này đang được kiểm duyệt viên khác xem xét
                      {item.claimedAt ? ` (từ ${formatDateTime(item.claimedAt)})` : ''}. Bạn không thể xử lý cho đến khi họ trả lại hàng đợi.
                    </p>
                  </div>
                ) : item.status !== 'PENDING' && item.status !== 'IN_REVIEW' ? (
                  <div className="bg-surface rounded-2xl p-6 shadow-sm border border-surface-container-highest">
                    <p className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-3">
                      Đã xử lý
                    </p>
                    <div className="rounded-xl bg-surface-container-low p-4 text-sm text-on-surface-variant">
                      <p className="m-0">
                        Trạng thái: <strong className="text-on-surface">{REPORT_STATUS_LABELS[item.status]}</strong>
                      </p>
                      {item.resolvedAt && (
                        <p className="m-0 mt-2 text-xs">Xử lý lúc: {formatDateTime(item.resolvedAt)}</p>
                      )}
                      {item.assignedModeratorId && (
                        <p className="m-0 mt-1 text-xs">Người xử lý (ID): {item.assignedModeratorId.slice(0, 8).toUpperCase()}</p>
                      )}
                    </div>
                  </div>
                ) : (
                <div className="bg-surface rounded-2xl p-6 shadow-sm border border-surface-container-highest">
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
                    className="mb-2.5 flex h-9 w-full cursor-pointer items-center justify-center gap-2 rounded-md border-0 bg-emerald-600 px-3.5 text-xs font-semibold text-white disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    <span className="material-symbols-outlined text-lg">task_alt</span>
                    {submitting === 'APPROVE' ? 'Đang xử lý...' : 'Duyệt nội dung'}
                  </button>

	                  <button
	                    onClick={() => handleAction('HIDE')}
                    disabled={!canHideTarget(item.targetType) || submitting !== null}
                    title={!canHideTarget(item.targetType) ? `Backend không hỗ trợ xoá cho loại ${TARGET_TYPE_LABELS[item.targetType]}` : 'Xoá khỏi hệ thống (thực thi qua outcome HIDE)'}
                    className="mb-2.5 flex h-9 w-full cursor-pointer items-center justify-center gap-2 rounded-md border-0 bg-error px-3.5 text-xs font-semibold text-on-error disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    <span className="material-symbols-outlined text-lg">delete</span>
                    {submitting === 'HIDE' ? 'Đang xử lý...' : 'Xóa bài viết'}
                  </button>

                  <button
                    onClick={() => handleAction('HIDE')}
                    disabled={!canHideTarget(item.targetType) || submitting !== null}
                    title={!canHideTarget(item.targetType) ? `Backend không hỗ trợ ẩn cho loại ${TARGET_TYPE_LABELS[item.targetType]}` : 'Ẩn khỏi hệ thống (thực thi qua outcome HIDE — cùng hành động với Xóa bài viết)'}
                    className="mb-2.5 flex h-9 w-full cursor-pointer items-center justify-center gap-2 rounded-md border-0 bg-primary-container px-3.5 text-xs font-semibold text-on-primary-container disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    <span className="material-symbols-outlined text-lg">visibility_off</span>
	                    Ẩn nội dung
	                  </button>

		                  <button
		                    onClick={() => handleAction('LOCK')}
	                    disabled={item.targetType !== 'QUESTION' || submitting !== null}
	                    title={item.targetType !== 'QUESTION' ? 'Chỉ câu hỏi cộng đồng hỗ trợ khóa thảo luận' : 'Khóa câu hỏi và đóng báo cáo'}
	                    className="mb-2.5 flex h-9 w-full cursor-pointer items-center justify-center gap-2 rounded-md border border-outline-variant bg-surface-container-highest px-3.5 text-xs font-semibold text-on-surface disabled:cursor-not-allowed disabled:opacity-40"
	                  >
	                    <span className="material-symbols-outlined text-lg">lock</span>
		                    {submitting === 'LOCK' ? 'Đang xử lý...' : 'Khóa thảo luận'}
		                  </button>

		                  <button
		                    onClick={() => handleAction('REQUEST_REVISION')}
		                    disabled={!canHideTarget(item.targetType) || submitting !== null}
		                    title={!canHideTarget(item.targetType) ? `Backend không hỗ trợ yêu cầu sửa cho loại ${TARGET_TYPE_LABELS[item.targetType]}` : 'Yêu cầu tác giả sửa lại; nội dung giữ/chuyển về trạng thái PENDING'}
		                    className="mb-2.5 flex h-9 w-full cursor-pointer items-center justify-center gap-2 rounded-md border border-outline-variant bg-surface-container-high px-3.5 text-xs font-semibold text-on-surface disabled:cursor-not-allowed disabled:opacity-40"
		                  >
		                    <span className="material-symbols-outlined text-lg">edit_note</span>
		                    {submitting === 'REQUEST_REVISION' ? 'Đang xử lý...' : 'Yêu cầu sửa'}
		                  </button>

		                  <button
	                    onClick={() => handleAction('WARN')}
	                    disabled={!canEnforceAccount(item.targetType) || submitting !== null}
	                    title={!canEnforceAccount(item.targetType) ? 'Loại báo cáo này không có tài khoản chịu xử lý' : 'Cảnh cáo tài khoản liên quan và đóng báo cáo'}
	                    className="mb-2.5 flex h-9 w-full cursor-pointer items-center justify-center gap-2 rounded-md border border-outline-variant bg-transparent px-3.5 text-xs font-semibold text-on-surface disabled:cursor-not-allowed disabled:opacity-40"
	                  >
	                    <span className="material-symbols-outlined text-lg">warning</span>
	                    {submitting === 'WARN' ? 'Đang xử lý...' : 'Cảnh cáo người dùng'}
	                  </button>

	                  <button
	                    onClick={() => handleAction('RESTRICT')}
	                    disabled={!canEnforceAccount(item.targetType) || submitting !== null}
	                    title={!canEnforceAccount(item.targetType) ? 'Loại báo cáo này không có tài khoản chịu xử lý' : 'Hạn chế đăng cộng đồng trong 7 ngày'}
	                    className="mb-2.5 flex h-9 w-full cursor-pointer items-center justify-center gap-2 rounded-md border-0 bg-primary-container px-3.5 text-xs font-semibold text-on-primary-container disabled:cursor-not-allowed disabled:opacity-40"
	                  >
	                    <span className="material-symbols-outlined text-lg">speaker_notes_off</span>
	                    {submitting === 'RESTRICT' ? 'Đang xử lý...' : 'Hạn chế đăng 7 ngày'}
	                  </button>

	                  <button
	                    onClick={() => handleAction('SUSPEND')}
	                    disabled={!canEnforceAccount(item.targetType) || submitting !== null}
	                    title={!canEnforceAccount(item.targetType) ? 'Loại báo cáo này không có tài khoản chịu xử lý' : 'Đình chỉ tài khoản trong 7 ngày'}
	                    className="mb-4 flex h-9 w-full cursor-pointer items-center justify-center gap-2 rounded-md border-0 bg-error-container px-3.5 text-xs font-semibold text-error disabled:cursor-not-allowed disabled:opacity-40"
	                  >
	                    <span className="material-symbols-outlined text-lg">person_off</span>
	                    {submitting === 'SUSPEND' ? 'Đang xử lý...' : 'Đình chỉ 7 ngày'}
	                  </button>

                  <textarea
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    placeholder="Ghi chú xử lý (tuỳ chọn)..."
                    rows={3}
                    className="w-full resize-none rounded-md border border-outline-variant p-3 font-sans text-sm outline-none"
                  />
                  {actionError && <p className="text-error text-xs mt-2">{actionError}</p>}
                </div>
                )}

                {assessment && <AiAssessmentCard assessment={assessment} />}

                <RelatedReportsCard items={relatedReports} totalElements={relatedTotal} page={relatedPage} size={20} loading={relatedLoading} error={relatedError} onPageChange={setRelatedPage} />
              </div>
            </div>
          </>
        )}
        </div>
      </main>

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
