import { useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { Clock3, ShieldOff } from 'lucide-react';
import { clearBlockedAccountState, loadBlockedAccountState } from '../models/blockedAccount';
import { submitAccountLockAppeal } from '../services/accountLockAppealApi';

export default function BlockedAccountPage() {
  const state = useMemo(loadBlockedAccountState, []);
  const [appealReason, setAppealReason] = useState('');
  const [appealError, setAppealError] = useState<string | null>(null);
  const [appealSubmitted, setAppealSubmitted] = useState(
    state?.appealPending === true || state?.appealStatus === 'PENDING',
  );
  const appealRejected = state?.appealStatus === 'REJECTED';
  const [submitting, setSubmitting] = useState(false);

  const title = state?.code === 'ACCOUNT_ADMIN_LOCKED'
    ? 'Tài khoản bị khóa bởi quản trị viên'
    : state?.code === 'ACCOUNT_TEMPORARILY_LOCKED'
      ? 'Tài khoản bị khóa tạm thời'
      : state?.code === 'ACCOUNT_DISABLED'
        ? 'Tài khoản đã bị vô hiệu hóa'
        : state?.code === 'ACCOUNT_SUSPENDED'
          ? 'Tài khoản đang bị tạm ngưng'
          : 'Không có thông tin hạn chế tài khoản';

  const message = state?.code === 'ACCOUNT_ADMIN_LOCKED'
    ? 'Quản trị viên hệ thống đã khóa quyền truy cập tài khoản này.'
    : state?.code === 'ACCOUNT_TEMPORARILY_LOCKED'
      ? 'Tài khoản tạm thời bị khóa do có nhiều lần đăng nhập không thành công.'
      : state?.code === 'ACCOUNT_DISABLED'
        ? 'Tài khoản đã bị vô hiệu hóa và hiện không thể đăng nhập. Vui lòng liên hệ hỗ trợ nếu bạn cho rằng đây là nhầm lẫn.'
        : state?.code === 'ACCOUNT_SUSPENDED'
          ? 'Tài khoản đang bị tạm ngưng theo quyết định kiểm duyệt.'
          : 'Vui lòng quay lại đăng nhập để thử lại.';

  async function submitAppeal(event: FormEvent) {
    event.preventDefault();
    if (!state?.appealToken || !appealReason.trim()) {
      setAppealError('Vui lòng nhập nội dung khiếu nại.');
      return;
    }
    setSubmitting(true);
    setAppealError(null);
    try {
      await submitAccountLockAppeal(state.appealToken, appealReason.trim());
      setAppealSubmitted(true);
      clearBlockedAccountState();
    } catch (error: unknown) {
      const message = (error as { response?: { data?: { message?: string } } }).response?.data?.message;
      setAppealError(message ?? 'Không thể gửi khiếu nại. Vui lòng đăng nhập lại để nhận quyền khiếu nại mới.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="font-sans bg-[#F6F1EC] min-h-screen flex items-center justify-center p-6">
      <div className="bg-white rounded-3xl p-8 sm:p-10 max-w-[520px] w-full shadow-[0_10px_40px_-10px_rgba(132,81,67,0.1)]">
        <div className="w-16 h-16 rounded-full bg-[#ffdad6] flex items-center justify-center mx-auto mb-6">
          <ShieldOff size={28} color="#93000a" />
        </div>
        <h1 className="text-xl font-semibold text-on-background text-center m-0 mb-3">{title}</h1>
        <p className="text-sm text-on-surface-variant leading-relaxed text-center m-0">{message}</p>

        {state?.reason && (
          <div className="mt-6 rounded-2xl border border-error-container bg-error-container/35 p-4">
            <p className="m-0 text-xs font-semibold uppercase tracking-wide text-error">Lý do khóa tài khoản</p>
            <p className="mb-0 mt-2 text-sm leading-6 text-on-surface">{state.reason}</p>
          </div>
        )}

        {state?.retryAt && (
          <div className="mt-5 flex items-start gap-3 rounded-2xl bg-surface-container-low p-4 text-sm text-on-surface-variant">
            <Clock3 size={18} className="mt-0.5 shrink-0" />
            <span>Có thể thử lại sau {new Date(state.retryAt).toLocaleString('vi-VN')}.</span>
          </div>
        )}

        {state?.code === 'ACCOUNT_ADMIN_LOCKED' && (state.appealAllowed || state.appealPending || appealRejected) && (
          <div className="mt-7 border-t border-surface-container-highest pt-6">
            {appealSubmitted ? (
              <div className="rounded-2xl bg-emerald-50 p-4 text-sm leading-6 text-emerald-800">
                Khiếu nại đã được gửi. Quản trị viên hệ thống sẽ xem xét và phản hồi trong quy trình quản trị tài khoản.
              </div>
            ) : appealRejected ? (
              <div className="rounded-2xl bg-error-container/35 p-4 text-sm leading-6 text-on-surface">
                Khiếu nại mở khóa đã bị từ chối. Tài khoản vẫn bị khóa theo quyết định của quản trị viên.
              </div>
            ) : (
              <form onSubmit={submitAppeal}>
                <label htmlFor="appeal-reason" className="mb-2 block text-sm font-semibold text-on-surface">
                  Nội dung khiếu nại
                </label>
                <textarea
                  id="appeal-reason"
                  value={appealReason}
                  onChange={(event) => setAppealReason(event.target.value)}
                  rows={5}
                  maxLength={1000}
                  placeholder="Trình bày lý do bạn đề nghị xem xét mở khóa..."
                  className="w-full resize-none rounded-2xl border border-outline-variant p-4 text-sm leading-6 outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
                />
                {appealError && <p className="mt-2 text-sm text-error">{appealError}</p>}
                <button type="submit" disabled={submitting} className="mt-4 w-full rounded-full bg-primary px-5 py-3 text-sm font-semibold text-on-primary disabled:opacity-50">
                  {submitting ? 'Đang gửi...' : 'Gửi khiếu nại mở khóa'}
                </button>
              </form>
            )}
          </div>
        )}

        <Link
          to="/login"
          onClick={clearBlockedAccountState}
          className="mt-6 block text-center text-sm font-semibold text-primary no-underline hover:underline"
        >
          Quay lại đăng nhập
        </Link>
      </div>
    </div>
  );
}
