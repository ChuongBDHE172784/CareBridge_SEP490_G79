import { useMemo } from 'react';
import { Link } from 'react-router-dom';
import { Clock3, LifeBuoy, ShieldOff } from 'lucide-react';
import { clearBlockedAccountState, loadBlockedAccountState } from '../models/blockedAccount';
import { SUPPORT_EMAIL, SUPPORT_PHONE } from '../../../shared/config/support';

export default function BlockedAccountPage() {
  const state = useMemo(() => loadBlockedAccountState(), []);

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

  // Every blocked state now resolves through customer support; only a temporary
  // lock clears on its own, and that case already shows a retry time.
  const showSupportContact = state?.code !== 'ACCOUNT_TEMPORARILY_LOCKED';

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

        {showSupportContact && (
          <div className="mt-7 border-t border-surface-container-highest pt-6">
            <div className="flex items-start gap-3">
              <LifeBuoy size={20} className="mt-0.5 shrink-0 text-primary" aria-hidden="true" />
              <div>
                <p className="m-0 text-sm font-semibold text-on-surface">Cần mở lại tài khoản?</p>
                <p className="mb-0 mt-2 text-sm leading-6 text-on-surface-variant">
                  Vui lòng liên hệ bộ phận chăm sóc khách hàng để được kiểm tra và xử lý. Hãy cung cấp
                  email hoặc số điện thoại đăng ký để nhân viên tra cứu nhanh hơn.
                </p>
                <ul className="mb-0 mt-3 list-none space-y-1 p-0 text-sm">
                  <li>
                    <a href={`mailto:${SUPPORT_EMAIL}`} className="font-semibold text-primary no-underline hover:underline">
                      {SUPPORT_EMAIL}
                    </a>
                  </li>
                  {SUPPORT_PHONE && (
                    <li>
                      <a href={`tel:${SUPPORT_PHONE}`} className="font-semibold text-primary no-underline hover:underline">
                        {SUPPORT_PHONE}
                      </a>
                    </li>
                  )}
                </ul>
              </div>
            </div>
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
