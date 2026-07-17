import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ModPortalSidebar from '../components/ModPortalSidebar';
import ConfirmDialog from '../../../shared/components/ConfirmDialog';
import { fetchModerationQueue, fetchRelatedReports, resolveReport } from '../services/moderationApi';
import type { ModerationQueueItem } from '../models/moderation';
import type { RelatedReportItem } from '../models/moderation';
import { formatReportReason } from '../models/moderation';
import RelatedReportsCard from '../components/RelatedReportsCard';

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

  const loadItem = useCallback(async () => {
    if (!reportId) return;
    setIsLoading(true);
    setError('');
    try {
      const page = await fetchModerationQueue({ size: 50 });
      const found = page.content.find((i) => i.id === reportId);
      if (!found) setError('Không tìm thấy báo cáo tài khoản này trong hàng đợi hiện tại.');
      setItem(found ?? null);
    } catch {
      setError('Không tải được dữ liệu báo cáo. Vui lòng thử lại.');
    } finally {
      setIsLoading(false);
    }
  }, [reportId]);

  useEffect(() => { loadItem(); }, [loadItem]);

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

  return (
    <div className="min-h-screen bg-background">
      <ModPortalSidebar />
      <div className="ml-64 min-h-screen p-8 font-sans">
        <div className="flex items-center gap-2 text-[13px] text-outline mb-4">
          <span className="cursor-pointer" onClick={() => navigate('/moderator/reports')}>Báo cáo</span>
          <span className="material-symbols-outlined text-base">chevron_right</span>
          <span className="text-on-surface-variant">Chi tiết Báo cáo Tài khoản</span>
        </div>

        {isLoading ? (
          <div className="py-16 text-center text-outline">Đang tải...</div>
        ) : error || !item ? (
          <div className="bg-error-container rounded-2xl p-6 text-error text-sm">
            {error || 'Không tìm thấy báo cáo.'}
          </div>
        ) : (
          <>
            <div className="flex items-center justify-between mb-6">
              <div>
                <h1 className="text-2xl font-bold text-on-surface m-0">
                  Chi tiết Báo cáo #{item.id.slice(0, 8).toUpperCase()}
                </h1>
                <p className="text-sm text-outline mt-1">Được tạo lúc {formatDateTime(item.reportedAt)}.</p>
              </div>
              <span className="py-1.5 px-3.5 rounded-full bg-error-container text-error text-xs font-semibold">
                Ưu tiên cao
              </span>
            </div>

            <div className="grid grid-cols-[1fr_360px] gap-6">
              <div className="flex flex-col gap-5">
                <div className="bg-surface rounded-2xl p-6 shadow-md">
                  <div className="flex items-center gap-2 mb-4">
                    <span className="material-symbols-outlined text-primary text-xl">person</span>
                    <h2 className="text-base font-bold text-on-surface m-0">Thông tin Tài khoản Bị báo cáo</h2>
                  </div>
	                  <div className="bg-surface-container-low rounded-2xl p-4 text-sm text-outline">
	                    <p className="m-0 text-on-surface-variant">
	                      Mục tiêu: <strong>{item.contentPreview || item.targetId || '—'}</strong>
	                    </p>
	                    <p className="m-0 mt-2 text-xs">Target ID: {item.targetId ?? '—'}</p>
	                    <p className="m-0 mt-1 text-xs">Reporter ID: {item.reporterUserId ?? '—'}</p>
	                  </div>
                </div>

                <div className="bg-surface rounded-2xl p-6 shadow-md">
                  <div className="flex items-center gap-2 mb-4">
                    <span className="material-symbols-outlined text-primary text-xl">description</span>
                    <h2 className="text-base font-bold text-on-surface m-0">Chi tiết Báo cáo & Bằng chứng</h2>
                  </div>
                  <p className="text-sm font-semibold text-on-surface mb-1">Lý do báo cáo:</p>
                  <div className="bg-surface-container-low rounded-2xl p-4 mb-4">
                    <p className="text-sm text-on-surface-variant">{formatReportReason(item.reportReason)}</p>
                  </div>
                  <p className="text-sm font-semibold text-on-surface mb-2">Nội dung liên quan:</p>
                  <div className="bg-surface-container-low rounded-2xl p-4 border border-outline-variant">
                    <p className="text-[15px] leading-7 text-on-surface whitespace-pre-wrap">{item.contentPreview}</p>
                  </div>
                </div>
              </div>

              <div className="flex flex-col gap-4">
                <RelatedReportsCard items={relatedReports} totalElements={relatedTotal} page={relatedPage} size={20} loading={relatedLoading} error={relatedError} onPageChange={setRelatedPage} />
                <div className="bg-surface rounded-2xl p-5 shadow-md">
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
	                    className="w-full text-sm border border-outline-variant rounded-2xl p-3 resize-none outline-none font-sans mb-2"
	                  />
	                  {actionError && <p className="text-error text-xs mb-3">{actionError}</p>}

	                  <button
	                    disabled={submitting}
	                    onClick={handleApply}
	                    className="w-full py-3.5 mb-2.5 rounded-2xl bg-primary text-on-primary border-0 text-sm font-semibold disabled:opacity-40 disabled:cursor-not-allowed"
	                  >
	                    {submitting ? 'Đang xử lý...' : 'Áp dụng Xử lý'}
	                  </button>
                  <button
                    disabled
                    title="Backend chưa có endpoint chuyển tuyến báo cáo lên cấp trên"
                    className="w-full py-3 rounded-2xl bg-transparent border border-outline-variant text-on-surface-variant text-sm font-semibold opacity-40 cursor-not-allowed"
                  >
                    Báo cáo Cấp trên
                  </button>
                </div>
              </div>
            </div>
          </>
        )}
      </div>

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
    </div>
  );
}
