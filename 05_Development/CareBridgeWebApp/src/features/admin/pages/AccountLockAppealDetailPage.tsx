import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import ConfirmDialog from '../../../shared/components/ConfirmDialog';
import type { AccountLockAppeal } from '../models/adminUser';
import { getAccountLockAppeal, reviewAccountLockAppeal } from '../services/adminUserApi';

function formatDate(value: string | null): string {
  if (!value) return 'Chưa có';
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
}

const STATUS_META: Record<string, { label: string; style: string }> = {
  PENDING: { label: 'Chờ xử lý', style: 'bg-amber-100 text-amber-800' },
  APPROVED: { label: 'Đã mở khóa', style: 'bg-emerald-100 text-emerald-800' },
  REJECTED: { label: 'Đã từ chối', style: 'bg-error-container text-error' },
  CANCELLED: { label: 'Đã đóng', style: 'bg-surface-container-high text-on-surface-variant' },
};

export default function AccountLockAppealDetailPage() {
  const { appealId } = useParams<{ appealId: string }>();
  const navigate = useNavigate();
  const [appeal, setAppeal] = useState<AccountLockAppeal | null>(null);
  const [reviewNote, setReviewNote] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pendingDecision, setPendingDecision] = useState<'APPROVE' | 'REJECT' | null>(null);

  const loadAppeal = useCallback(async () => {
    if (!appealId) return;
    setLoading(true);
    setError(null);
    try {
      setAppeal(await getAccountLockAppeal(appealId));
    } catch {
      setError('Không thể tải chi tiết khiếu nại hoặc đơn không còn tồn tại.');
    } finally {
      setLoading(false);
    }
  }, [appealId]);

  useEffect(() => {
    void loadAppeal();
  }, [loadAppeal]);

  async function review(decision: 'APPROVE' | 'REJECT', note?: string) {
    if (!appealId || !appeal || appeal.status !== 'PENDING') return;
    const finalNote = note?.trim() || reviewNote.trim();

    if (decision === 'REJECT' && !finalNote) {
      setError('Vui lòng nhập lý do từ chối để bảo đảm khả năng kiểm toán.');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const updated = await reviewAccountLockAppeal(appealId, decision, finalNote || undefined);
      setAppeal(updated);
      setReviewNote('');
      setPendingDecision(null);
    } catch (requestError: unknown) {
      const message = (requestError as { response?: { data?: { message?: string } } }).response?.data?.message;
      setError(message ?? 'Không thể xử lý khiếu nại. Trạng thái đơn có thể đã được thay đổi.');
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return (
      <div className="p-6 md:p-8 font-sans">
        <div className="h-40 animate-pulse rounded-2xl bg-surface-container-low" />
        <div className="mt-6 h-64 animate-pulse rounded-2xl bg-surface-container-low" />
      </div>
    );
  }

  if (!appeal) {
    return (
      <div className="p-6 md:p-8 font-sans">
        <div className="rounded-2xl border border-error-container bg-error-container/60 p-8 text-center text-error">
          <p className="font-bold text-base">{error}</p>
          <button
            type="button"
            onClick={() => navigate('/admin/account-lock-appeals')}
            className="mt-4 py-2 px-5 rounded-full bg-surface border border-outline-variant text-sm font-semibold text-on-surface-variant hover:bg-surface-container-low cursor-pointer"
          >
            Quay lại danh sách khiếu nại
          </button>
        </div>
      </div>
    );
  }

  const meta = STATUS_META[appeal.status] || STATUS_META.CANCELLED;

  return (
    <div className="p-6 md:p-8 font-sans">
      {/* Back Button */}
      <div className="mb-6 flex items-center gap-3">
        <button
          type="button"
          onClick={() => navigate('/admin/account-lock-appeals')}
          className="inline-flex items-center gap-2 py-1.5 px-3 rounded-full text-xs font-semibold bg-surface border border-outline-variant text-on-surface-variant hover:bg-surface-container-low cursor-pointer"
        >
          <span className="material-symbols-outlined text-base">arrow_back</span>
          Danh sách khiếu nại
        </button>
      </div>

      <div className="mx-auto max-w-4xl space-y-6">
        {/* Header Banner */}
        <section className="rounded-2xl border border-surface-container-highest bg-surface p-6 shadow-md">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div className="flex items-center gap-4">
              <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-primary-container text-on-primary-container font-bold text-xl shadow-inner">
                {(appeal.userName || 'U').charAt(0).toUpperCase()}
              </div>
              <div>
                <h1 className="m-0 text-2xl font-bold text-on-surface">Xem xét khiếu nại khóa tài khoản</h1>
                <p className="mt-1 text-sm text-on-surface-variant m-0">
                  {appeal.userName ?? 'Người dùng'} · <span className="font-mono text-outline">{appeal.userEmail ?? 'Không có email'}</span>
                </p>
              </div>
            </div>

            <span className={`inline-flex items-center rounded-full px-4 py-1 text-xs font-semibold ${meta.style}`}>
              {meta.label}
            </span>
          </div>
        </section>

        {error && (
          <div className="rounded-2xl border border-error-container bg-error-container/60 p-4 text-sm text-error">
            {error}
          </div>
        )}

        {/* Dual Comparison Cards */}
        <section className="grid gap-5 md:grid-cols-2">
          {/* Card 1: System Admin Lock Reason */}
          <div className="rounded-2xl border border-error-container/40 bg-error-container/20 p-5 space-y-2">
            <div className="flex items-center gap-2">
              <span className="material-symbols-outlined text-error text-xl">lock</span>
              <span className="text-xs font-bold text-error uppercase tracking-wider">Căn cứ System Admin khóa</span>
            </div>
            <p className="text-sm text-on-surface leading-relaxed m-0 pt-1 font-medium">
              {appeal.lockReason || 'Không có dữ liệu chi tiết lý do khóa từ System Admin.'}
            </p>
          </div>

          {/* Card 2: User Appeal Explanation */}
          <div className="rounded-2xl border border-primary/20 bg-primary/5 p-5 space-y-2">
            <div className="flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-xl">gavel</span>
              <span className="text-xs font-bold text-primary uppercase tracking-wider">Nội dung người dùng giải trình</span>
            </div>
            <p className="text-sm text-on-surface leading-relaxed m-0 pt-1">
              {appeal.reason}
            </p>
          </div>
        </section>

        {/* Audit Details & Form Card */}
        <section className="rounded-2xl border border-surface-container-highest bg-surface p-6 shadow-md space-y-6">
          <div>
            <h3 className="text-base font-bold text-on-surface m-0 mb-1">Thông tin chi tiết đơn</h3>
            <p className="text-xs text-on-surface-variant">Lịch sử gửi đơn và trạng thái thẩm định.</p>
          </div>

          <div className="grid gap-4 md:grid-cols-2 rounded-2xl border border-surface-container-highest bg-surface-bright p-4">
            <div>
              <span className="text-xs font-semibold uppercase text-outline block mb-1">Thời gian gửi đơn</span>
              <span className="text-sm font-bold text-on-surface">{formatDate(appeal.submittedAt)}</span>
            </div>
            <div>
              <span className="text-xs font-semibold uppercase text-outline block mb-1">Thời gian xem xét</span>
              <span className="text-sm font-bold text-on-surface">{formatDate(appeal.reviewedAt)}</span>
            </div>
            {appeal.reviewNote && (
              <div className="md:col-span-2 border-t border-surface-container-highest pt-3 mt-1">
                <span className="text-xs font-semibold uppercase text-outline block mb-1">Ghi chú xử lý của Admin</span>
                <span className="text-sm text-on-surface leading-relaxed">{appeal.reviewNote}</span>
              </div>
            )}
          </div>

          {/* Pending Action Form */}
          {appeal.status === 'PENDING' && (
            <div className="border-t border-surface-container-highest pt-6 space-y-4">
              <label htmlFor="review-note" className="block text-sm font-bold text-on-surface">
                Nhập ghi chú xem xét (lưu vết Audit Log)
              </label>
              <textarea
                id="review-note"
                value={reviewNote}
                onChange={(e) => setReviewNote(e.target.value)}
                rows={4}
                maxLength={1000}
                placeholder="Nhập căn cứ chấp nhận mở khóa hoặc lý do từ chối để gửi thông báo cho người dùng..."
                className="w-full resize-none rounded-2xl border border-outline-variant bg-surface p-4 text-sm leading-relaxed outline-none focus:border-primary font-sans"
              />

              <div className="flex flex-wrap items-center justify-between gap-4 pt-2">
                <p className="text-xs text-on-surface-variant m-0">
                  Duyệt sẽ tự động mở khóa tài khoản người dùng ngay lập tức.
                </p>

                <div className="flex items-center gap-3">
                  <button
                    type="button"
                    disabled={submitting}
                    onClick={() => setPendingDecision('REJECT')}
                    className="py-2.5 px-6 rounded-full border border-error-container bg-surface text-error text-sm font-semibold hover:bg-error-container cursor-pointer disabled:opacity-50 inline-flex items-center gap-2"
                  >
                    <span className="material-symbols-outlined text-lg">close</span>
                    Từ chối khiếu nại
                  </button>
                  <button
                    type="button"
                    disabled={submitting}
                    onClick={() => setPendingDecision('APPROVE')}
                    className="py-2.5 px-6 rounded-full bg-primary text-on-primary text-sm font-semibold hover:bg-primary/90 cursor-pointer disabled:opacity-50 inline-flex items-center gap-2"
                  >
                    <span className="material-symbols-outlined text-lg">check</span>
                    Duyệt & Mở khóa tài khoản
                  </button>
                </div>
              </div>
            </div>
          )}
        </section>
      </div>

      {/* Confirmation Dialogs */}
      <ConfirmDialog
        key="confirm-approve"
        open={pendingDecision === 'APPROVE'}
        title={`Chấp nhận khiếu nại & Mở khóa tài khoản?`}
        description={`Tài khoản ${appeal.userName ?? appeal.userEmail} sẽ được tự động chuyển sang trạng thái Đang hoạt động.`}
        icon="lock_open"
        confirmLabel="Xác nhận mở khóa"
        submitting={submitting}
        onCancel={() => setPendingDecision(null)}
        onConfirm={() => void review('APPROVE')}
      />

      <ConfirmDialog
        key="confirm-reject"
        open={pendingDecision === 'REJECT'}
        title={`Từ chối khiếu nại của ${appeal.userName ?? appeal.userEmail}?`}
        description="Bản án khóa tài khoản sẽ tiếp tục duy trì. Bạn bắt buộc phải nhập lý do từ chối."
        icon="block"
        tone="danger"
        confirmLabel="Từ chối khiếu nại"
        reasonLabel="Lý do từ chối"
        reasonPlaceholder="Nêu rõ lý do từ chối khiếu nại..."
        submitting={submitting}
        onCancel={() => setPendingDecision(null)}
        onConfirm={(reasonNote) => {
          if (!reasonNote?.trim()) {
            setError('Vui lòng nhập lý do từ chối để bảo đảm khả năng kiểm toán.');
            return;
          }
          void review('REJECT', reasonNote);
        }}
      />
    </div>
  );
}

