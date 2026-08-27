import { Smartphone } from 'lucide-react';
import { useAuth } from '../../../shared/auth/useAuth';

// Placeholder — shown to MOTHER / FAMILY roles who have no web portal access
export default function NoWebAccessPage() {
  const { logout } = useAuth();

  return (
    <div className="font-sans bg-[#F6F1EC] min-h-screen flex items-center justify-center">
      <div className="bg-white rounded-3xl p-12 max-w-[400px] w-full text-center shadow-[0_10px_40px_-10px_rgba(132,81,67,0.1)]">
        <div className="w-16 h-16 rounded-full bg-surface-container-low flex items-center justify-center mx-auto mb-6">
          <Smartphone size={28} color="#845143" />
        </div>
        <h2 className="text-xl font-semibold text-on-background m-0 mb-3">
          Vui lòng dùng ứng dụng mobile
        </h2>
        <p className="text-sm text-on-surface-variant leading-relaxed m-0 mb-8">
          Tài khoản của bạn không có quyền truy cập vào cổng web. Vui lòng sử dụng ứng dụng CareBridge trên điện thoại.
        </p>
        <button
          onClick={() => logout()}
          className="px-8 py-3.5 bg-primary-container text-on-primary border-none rounded-full font-sans font-semibold text-sm cursor-pointer transition-colors duration-200 hover:bg-primary"
        >
          Đăng xuất
        </button>
      </div>
    </div>
  );
}
