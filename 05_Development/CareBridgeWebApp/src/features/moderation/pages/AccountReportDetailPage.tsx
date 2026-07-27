import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ModPortalSidebar from '../components/ModPortalSidebar';
import ConfirmDialog from '../../../shared/components/ConfirmDialog';
import { fetchAiAssessment, fetchModerationQueue, fetchRelatedReports, resolveReport, revertReport } from '../services/moderationApi';
import type { AiAssessment, ModerationQueueItem } from '../models/moderation';
import type { RelatedReportItem } from '../models/moderation';
import { formatReportReason, REPORT_SOURCE_LABELS, REPORT_STATUS_LABELS } from '../models/moderation';
import RelatedReportsCard from '../components/RelatedReportsCard';
import AiAssessmentCard from '../components/AiAssessmentCard';

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });
}

const DURATIONS = [
  { label: '3 Ngày', days: 3 },
  { label: '7 Ngày', days: 7 },
  { label: '14 Ngày', days: 14 },
  { label: '30 Ngày', days: 30 },
];

type ActionChoice = 'WARN' | 'RESTRICT' | 'SUSPEND';

const ACTION_CHOICE_CONFIG: Record<ActionChoice, { title: string; icon: string; tone: 'default' | 'danger' }> = {
  WARN: { title: 'Gửi cảnh cáo tới tài khoản này?', icon: 'warning', tone: 'default' },
  RESTRICT: { title: 'Hạn chế đăng bài/bình luận?', icon: 'speaker_notes_off', tone: 'danger' },
  SUSPEND: { title: 'Đình chỉ tài khoản này?', icon: 'person_off', tone: 'danger' },
};

export default function AccountReportDetailPage() {
  const { reportId } = useParams<{ reportId: string }>();
  const navigate = useNavigate();
  const [item, setItem] = useState<ModerationQueueItem | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [choice, setChoice] = useState<ActionChoice>('RESTRICT');
  const [durationDays, setDurationDays] = useState(DURATIONS[0].days);
  const [note, setNote] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [actionError, setActionError] = useState('');
  const [confirming, setConfirming] = useState(false);
  const [relatedReports, setRelatedReports] = useState<RelatedReportItem[]>([]);
  const [relatedTotal, setRelatedTotal] = useState(0);
  const [relatedPage, setRelatedPage] = useState(0);
  const [relatedLoading, setRelatedLoading] = useState(false);
  const [relatedError, setRelatedError] = useState(false);

  const [revertTarget, setRevertTarget] = useState<ModerationQueueItem | null>(null);
  const [revertSubmitting, setRevertSubmitting] = useState(false);
  const [revertError, setRevertError] = useState('');
  const [assessment, setAssessment] = useState<AiAssessment | null>(null);

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
      if (!found) setError('Không tìm thấy báo cáo tài khoản này trong hàng đợi hiện tại.');
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

  const expiresAtFor = (action: ActionChoice) => {
    if (action === 'WARN') return undefined;
    const expires = new Date();
    expires.setDate(expires.getDate() + durationDays);
    return expires.toISOString();
  };

  const handleApply = () => {
    if (!item || !reportId) return;
    if (!note.trim()) {
      setActionError('Cần nhập lý do xử lý.');
      return;
    }
    setActionError('');
    setConfirming(true);
  };

  const executeApply = async () => {
    if (!item || !reportId) return;
    setSubmitting(true);
    setActionError('');
    try {
      await resolveReport(reportId, choice, note.trim(), expiresAtFor(choice));
      navigate('/moderator/reports');
    } catch (err: unknown) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setActionError(message || 'Áp dụng xử lý thất bại. Vui lòng thử lại.');
      setConfirming(false);
    } finally {
      setSubmitting(false);
    }
  };

  // CB-MOD-IMP-015 follow-up: a RESOLVED/DISMISSED report reopened from the "Đã xử lý" tab has no
  // resolve action to offer — reverting it back to PENDING is the only valid action here.
  const confirmRevert = async () => {
    if (!revertTarget) return;
    setRevertSubmitting(true);
    setRevertError('');
    try {
      await revertReport(revertTarget.id);
      navigate('/moderator/reports');
    } catch (err: unknown) {
      const message = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setRevertError(message || 'Hoàn tác thất bại, vui lòng thử lại.');
    } finally {
      setRevertSubmitting(false);
    }
  };

  return (
    <div className="portal-page">
      <ModPortalSidebar />
      <main className="portal-content font-sans">
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
                <h1 className="text-[26px] font-bold text-on-surface m-0">Chi tiết báo cáo tài khoản</h1>
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

            <div className="grid gap-5 lg:grid-cols-[1fr_360px]">
              <div>
                <div className="portal-card-padded mb-5">
                  <div className="flex items-center gap-2 mb-3">
                    <span className="material-symbols-outlined text-primary text-xl">flag</span>
                    <h2 className="text-base font-bold text-on-surface m-0">Lý do báo cáo</h2>
                  </div>
                  <p className="text-sm text-on-surface-variant mb-4">{formatReportReason(item.reportReason)}</p>
                  <div className="rounded-md bg-surface-container-low p-4">
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

                <div className="portal-card-padded">
                  <div className="flex items-center gap-2 mb-3">
                    <span className="material-symbols-outlined text-primary text-xl">person</span>
                    <h2 className="text-base font-bold text-on-surface m-0">Thông tin Tài khoản Bị báo cáo</h2>
                  </div>
                  <div className="rounded-md border border-outline-variant bg-surface-container-low p-4">
                    <p className="text-sm text-outline mb-1">Tài khoản / Mục tiêu</p>
                    <p className="text-[15px] font-semibold text-on-surface mb-2">{item.contentPreview || item.targetId || '—'}</p>
                    <div className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-outline border-t border-outline-variant/50 pt-2 mt-2">
                      <span>Target ID: {item.targetId ?? '—'}</span>
                      <span>Reporter ID: {item.reporterUserId ?? '—'}</span>
                    </div>
                  </div>
                </div>
              </div>

              <div className="flex flex-col gap-4">
                {assessment && <AiAssessmentCard assessment={assessment} />}
                <RelatedReportsCard items={relatedReports} totalElements={relatedTotal} page={relatedPage} size={20} loading={relatedLoading} error={relatedError} onPageChange={setRelatedPage} />
                {item.status !== 'PENDING' && item.status !== 'IN_REVIEW' ? (
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
                      {item.revertedAt && (
                        <p className="m-0 mt-1 text-xs">Đã từng hoàn tác lúc: {formatDateTime(item.revertedAt)}</p>
                      )}
                    </div>
                    <button
                      type="button"
                      onClick={() => { setRevertError(''); setRevertTarget(item); }}
                      className="mt-4 flex h-10 w-full items-center justify-center gap-2 rounded-xl border-0 bg-surface-container-highest px-3.5 text-xs font-semibold text-on-surface cursor-pointer hover:bg-surface-container-high"
                    >
                      <span className="material-symbols-outlined text-lg">undo</span>
                      Hoàn tác báo cáo
                    </button>
                  </div>
                ) : (
                <div className="bg-surface rounded-2xl p-6 shadow-sm border border-surface-container-highest">
                  <div className="flex items-center gap-2 mb-3">
                    <span className="material-symbols-outlined text-primary text-xl">gavel</span>
                    <p className="text-sm font-bold text-on-surface m-0">Hành động Xử lý</p>
                  </div>

	                  <fieldset disabled={submitting}>
	                    <label className="flex items-start gap-3 p-3 rounded-xl border border-outline-variant mb-2">
	                      <input type="radio" name="account-action" checked={choice === 'WARN'} onChange={() => setChoice('WARN')} className="accent-primary mt-0.5" />
                      <span>
                        <span className="block text-sm font-semibold text-on-surface">Cảnh cáo</span>
                        <span className="block text-xs text-outline">Gửi thông báo nhắc nhở nội quy.</span>
                      </span>
                    </label>

                    <label className="flex items-start gap-3 p-3 rounded-xl border border-outline-variant mb-2">
                      <input type="radio" name="account-action" checked={choice === 'RESTRICT'} onChange={() => setChoice('RESTRICT')} className="accent-primary mt-0.5" />
                      <span className="flex-1">
                        <span className="block text-sm font-semibold text-on-surface">Hạn chế Tạm thời</span>
                        <span className="block text-xs text-outline mb-2">Cấm đăng bài/bình luận.</span>
	                        <select
	                          value={durationDays}
	                          onChange={(e) => setDurationDays(Number(e.target.value))}
	                          className="w-full py-1.5 px-3 rounded-lg border border-outline-variant text-xs"
	                        >
	                          {DURATIONS.map((d) => <option key={d.days} value={d.days}>{d.label}</option>)}
	                        </select>
                      </span>
                    </label>

                    <label className="flex items-start gap-3 p-3 rounded-xl border border-outline-variant mb-4">
                      <input type="radio" name="account-action" checked={choice === 'SUSPEND'} onChange={() => setChoice('SUSPEND')} className="accent-primary mt-0.5" />
	                        <span>
	                          <span className="block text-sm font-semibold text-error">Khóa Tài khoản</span>
	                        <span className="block text-xs text-outline">Đình chỉ tạm thời quyền truy cập.</span>
	                      </span>
	                    </label>
	                  </fieldset>

                  <p className="text-[11px] font-semibold text-outline uppercase tracking-[0.05em] mb-2">
                    Ghi chú Nội bộ (Tùy chọn)
                  </p>
                  <textarea
	                    value={note}
	                    onChange={(e) => setNote(e.target.value)}
	                    placeholder="Nhập lý do xử lý chi tiết..."
	                    rows={3}
	                    disabled={submitting}
                    className="mb-2 w-full resize-none rounded-md border border-outline-variant p-3 font-sans text-sm outline-none"
	                  />
	                  {actionError && <p className="text-error text-xs mb-3">{actionError}</p>}

	                  <button
	                    disabled={submitting}
	                    onClick={handleApply}
	                    className="mb-2.5 h-9 w-full rounded-md border-0 bg-primary px-3.5 text-xs font-semibold text-on-primary disabled:cursor-not-allowed disabled:opacity-40"
	                  >
	                    {submitting ? 'Đang xử lý...' : 'Áp dụng Xử lý'}
	                  </button>
                  <button
                    disabled
                    title="Backend chưa có endpoint chuyển tuyến báo cáo lên cấp trên"
                    className="h-9 w-full cursor-not-allowed rounded-md border border-outline-variant bg-transparent px-3.5 text-xs font-semibold text-on-surface-variant opacity-40"
                  >
                    Báo cáo Cấp trên
                  </button>
                </div>
                )}
              </div>
            </div>
          </>
        )}
        </div>
      </main>

      <ConfirmDialog
        open={confirming}
        title={ACTION_CHOICE_CONFIG[choice].title}
        description={note.trim() ? `Ghi chú: "${note.trim()}"` : undefined}
        icon={ACTION_CHOICE_CONFIG[choice].icon}
        tone={ACTION_CHOICE_CONFIG[choice].tone}
        confirmLabel="Xác nhận"
        submitting={submitting}
        errorText={actionError}
        onConfirm={executeApply}
        onCancel={() => setConfirming(false)}
      />

      <ConfirmDialog
        open={revertTarget !== null}
        title="Hoàn tác báo cáo này?"
        description="Báo cáo sẽ quay lại hàng đợi để xử lý lại."
        icon="undo"
        tone="default"
        confirmLabel="Hoàn tác"
        submitting={revertSubmitting}
        errorText={revertError}
        onConfirm={confirmRevert}
        onCancel={() => setRevertTarget(null)}
      />
    </div>
  );
}
