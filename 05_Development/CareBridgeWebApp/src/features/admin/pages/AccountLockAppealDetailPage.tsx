import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import type { AccountLockAppeal } from '../models/adminUser';
import { getAccountLockAppeal, reviewAccountLockAppeal } from '../services/adminUserApi';

function formatDate(value: string | null): string {
  if (!value) return 'Chưa có';
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
}

export default function AccountLockAppealDetailPage() {
  const { appealId } = useParams<{ appealId: string }>();
  const navigate = useNavigate();
  const [appeal, setAppeal] = useState<AccountLockAppeal | null>(null);
  const [reviewNote, setReviewNote] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

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

  async function review(decision: 'APPROVE' | 'REJECT') {
    if (!appealId || !appeal || appeal.status !== 'PENDING') return;
    if (decision === 'REJECT' && !reviewNote.trim()) {
      setError('Vui lòng nhập lý do từ chối để bảo đảm khả năng kiểm toán.');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const updated = await reviewAccountLockAppeal(appealId, decision, reviewNote.trim() || undefined);
      setAppeal(updated);
      setReviewNote('');
    } catch (requestError: unknown) {
      const message = (requestError as { response?: { data?: { message?: string } } }).response?.data?.message;
      setError(message ?? 'Không thể xử lý khiếu nại. Trạng thái đơn có thể đã được thay đổi.');
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <div className="p-8 text-center text-outline">Đang tải chi tiết khiếu nại...</div>;
  if (!appeal) return <div className="p-8"><p className="text-error">{error}</p><Link to="/admin/account-lock-appeals" className="font-semibold text-primary">Quay lại danh sách</Link></div>;

  return (
    <div className="p-6 md:p-8 font-sans">
      <button type="button" onClick={() => navigate('/admin/account-lock-appeals')} className="mb-6 inline-flex items-center gap-2 rounded-full border border-outline-variant px-4 py-2 text-sm font-semibold text-on-surface-variant">
        <span className="material-symbols-outlined text-lg">arrow_back</span>
        Danh sách khiếu nại
      </button>

      <div className="mx-auto max-w-4xl space-y-6">
        <section className="rounded-2xl border border-surface-container-highest bg-surface p-6 shadow-sm">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <h1 className="m-0 text-2xl font-bold text-on-surface">Xem xét khiếu nại khóa tài khoản</h1>
              <p className="mt-2 text-sm text-on-surface-variant">{appeal.userName ?? 'Người dùng'} · {appeal.userEmail ?? 'Không có email'}</p>
            </div>
            <span className={`rounded-full px-3 py-1 text-xs font-semibold ${appeal.status === 'PENDING' ? 'bg-amber-100 text-amber-800' : appeal.status === 'APPROVED' ? 'bg-emerald-100 text-emerald-800' : 'bg-surface-container-low text-on-surface-variant'}`}>
              {appeal.status === 'PENDING' ? 'Chờ xử lý' : appeal.status === 'APPROVED' ? 'Đã mở khóa' : appeal.status === 'REJECTED' ? 'Đã từ chối' : 'Đã đóng'}
            </span>
          </div>
        </section>

        {error && <div className="rounded-2xl border border-error-container bg-error-container/40 p-4 text-sm text-error">{error}</div>}

        <section className="grid gap-5 md:grid-cols-2">
          <div className="rounded-2xl border border-error-container bg-error-container/25 p-5">
            <p className="m-0 text-xs font-semibold uppercase tracking-wider text-error">Lý do System Admin khóa</p>
            <p className="mb-0 mt-3 text-sm leading-6 text-on-surface">{appeal.lockReason || 'Không có dữ liệu lý do khóa.'}</p>
          </div>
          <div className="rounded-2xl border border-primary/20 bg-primary-container/25 p-5">
            <p className="m-0 text-xs font-semibold uppercase tracking-wider text-primary">Nội dung người dùng khiếu nại</p>
            <p className="mb-0 mt-3 text-sm leading-6 text-on-surface">{appeal.reason}</p>
          </div>
        </section>

        <section className="rounded-2xl border border-surface-container-highest bg-surface p-6 shadow-sm">
          <dl className="grid gap-4 text-sm md:grid-cols-2">
            <div><dt className="text-xs font-semibold uppercase text-outline">Gửi lúc</dt><dd className="m-0 mt-1 text-on-surface">{formatDate(appeal.submittedAt)}</dd></div>
            <div><dt className="text-xs font-semibold uppercase text-outline">Xử lý lúc</dt><dd className="m-0 mt-1 text-on-surface">{formatDate(appeal.reviewedAt)}</dd></div>
            {appeal.reviewNote && <div className="md:col-span-2"><dt className="text-xs font-semibold uppercase text-outline">Ghi chú xử lý</dt><dd className="m-0 mt-1 text-on-surface">{appeal.reviewNote}</dd></div>}
          </dl>

          {appeal.status === 'PENDING' && (
            <div className="mt-6 border-t border-surface-container-highest pt-6">
              <label htmlFor="review-note" className="mb-2 block text-sm font-semibold text-on-surface">Ghi chú xem xét</label>
              <textarea id="review-note" value={reviewNote} onChange={(event) => setReviewNote(event.target.value)} rows={4} maxLength={1000} placeholder="Nhập căn cứ duyệt hoặc lý do từ chối..." className="w-full resize-none rounded-2xl border border-outline-variant p-4 text-sm leading-6 outline-none focus:border-primary focus:ring-2 focus:ring-primary/20" />
              <div className="mt-4 flex flex-wrap justify-end gap-3">
                <button type="button" disabled={submitting} onClick={() => void review('REJECT')} className="rounded-full border border-error px-5 py-2.5 text-sm font-semibold text-error disabled:opacity-50">Từ chối khiếu nại</button>
                <button type="button" disabled={submitting} onClick={() => void review('APPROVE')} className="rounded-full bg-primary px-5 py-2.5 text-sm font-semibold text-on-primary disabled:opacity-50">Duyệt và mở khóa</button>
              </div>
              <p className="mb-0 mt-3 text-xs leading-5 text-on-surface-variant">Duyệt sẽ mở khóa tài khoản trong cùng giao dịch. Từ chối sẽ giữ nguyên trạng thái khóa.</p>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
