import { useSearchParams } from 'react-router-dom';
import { ShieldOff } from 'lucide-react';

// Placeholder — will be replaced by /build-screen CB-052 or CB-222
export default function BlockedAccountPage() {
  const [params] = useSearchParams();
  const reason = params.get('reason');

  const message =
    reason === 'disabled'
      ? 'Tài khoản của bạn chưa được kích hoạt hoặc đã bị vô hiệu hóa. Vui lòng kiểm tra email xác thực hoặc liên hệ hỗ trợ kỹ thuật.'
      : reason === 'suspended'
        ? 'Tài khoản của bạn đã bị tạm ngưng hoạt động. Vui lòng liên hệ hỗ trợ để biết thêm chi tiết.'
        : 'Tài khoản của bạn đã bị khoá tạm thời do đăng nhập sai nhiều lần. Vui lòng thử lại sau ít phút.';

  return (
    <div className="font-sans bg-[#F6F1EC] min-h-screen flex items-center justify-center">
      <div className="bg-white rounded-3xl p-12 max-w-[400px] w-full text-center shadow-[0_10px_40px_-10px_rgba(132,81,67,0.1)]">
        <div className="w-16 h-16 rounded-full bg-[#ffdad6] flex items-center justify-center mx-auto mb-6">
          <ShieldOff size={28} color="#93000a" />
        </div>
        <h2 className="text-xl font-semibold text-on-background m-0 mb-3">
          Tài khoản bị hạn chế
        </h2>
        <p className="text-sm text-on-surface-variant leading-relaxed m-0 mb-8">
          {message}
        </p>
        <a
          href="/login"
          className="inline-block px-8 py-3.5 bg-primary-container text-on-primary rounded-full font-semibold text-sm no-underline transition-colors duration-200 hover:bg-primary"
        >
          Quay lại đăng nhập
        </a>
      </div>
    </div>
  );
}
