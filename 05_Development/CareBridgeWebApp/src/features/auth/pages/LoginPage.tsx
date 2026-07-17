import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { User, Lock, EyeOff, Eye, ArrowRight, AlertCircle } from 'lucide-react';
import { federatedAuthenticate, login } from '../services/authApi';
import { googleIdToken, phoneIdToken } from '../services/firebaseAuth';
import { useAuthStore } from '../../../shared/auth/authStore';
import { getDefaultRouteForRole } from '../../../shared/auth/roleRoutes';
import logo from '../../../assets/logo.png';

const loginSchema = z.object({
  identifier: z.string().min(1, 'Vui lòng nhập email hoặc số điện thoại'),
  password: z.string().min(1, 'Vui lòng nhập mật khẩu'),
});

type LoginFormData = z.infer<typeof loginSchema>;

function isEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

export default function LoginPage() {
  const navigate = useNavigate();
  const [showPassword, setShowPassword] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const [federatedLoading, setFederatedLoading] = useState<'google' | 'phone' | null>(null);

  const completeFederated = async (provider: 'google' | 'phone') => {
    setServerError(null);
    setFederatedLoading(provider);
    try {
      const idToken = provider === 'google'
        ? await googleIdToken()
        : await phoneIdToken(window.prompt('Phone number including country code (for example +84)') ?? '');
      const result = await federatedAuthenticate(idToken);
      useAuthStore.getState().setTokens(result.accessToken, result.refreshToken);
      if (result.user.role) {
        useAuthStore.getState().setUser({
          id: result.user.id,
          phone: result.user.phone ?? '',
          name: result.user.name,
          avatarUrl: result.user.avatarUrl,
          role: result.user.role,
        });
      }
      navigate(result.profileCompleted ? getDefaultRouteForRole(result.user.role) : '/account/profile', { replace: true });
    } catch (error) {
      if (error instanceof Error && (error.message.includes('popup-closed') || error.message === 'AUTH_CANCELLED')) return;
      setServerError('Unable to sign in. Please try again without sharing your verification code.');
    } finally {
      setFederatedLoading(null);
    }
  };

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginFormData) => {
    setServerError(null);
    try {
      const identifier = data.identifier.trim();
      const result = await login(
        isEmail(identifier) ? { email: identifier, password: data.password } : { phone: identifier, password: data.password },
      );
      if (result.auth) {
        const { accessToken, refreshToken, user } = result.auth;
        useAuthStore.getState().setTokens(accessToken, refreshToken);
        useAuthStore.getState().setUser({
          id: user.id,
          phone: user.phone ?? '',
          name: user.name,
          avatarUrl: user.avatarUrl,
          role: user.role as ReturnType<typeof useAuthStore.getState>['user'] extends { role: infer R } ? R : never,
        });
        navigate(getDefaultRouteForRole(user.role as Parameters<typeof getDefaultRouteForRole>[0]), { replace: true });
        return;
      }
      navigate('/login/otp', {
        state: {
          userId: result.userId,
          otpExpiresAt: result.otpExpiresAt,
          identifier,
        },
      });
    } catch (err: unknown) {
      const error = err as { response?: { status?: number; data?: { message?: string; error?: string } } };
      const status = error.response?.status;
      const code = error.response?.data?.error;

      if (status === undefined) {
        setServerError('Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng hoặc thử lại sau.');
      } else if (code === 'ACCOUNT_LOCKED') {
        navigate('/account-blocked?reason=locked', { replace: true });
      } else if (code === 'ACCOUNT_DISABLED') {
        navigate('/account-blocked?reason=disabled', { replace: true });
      } else if (code === 'ACCOUNT_SUSPENDED') {
        navigate('/account-blocked?reason=suspended', { replace: true });
      } else if (status === 401) {
        setServerError('Email hoặc mật khẩu không chính xác. Vui lòng thử lại.');
      } else if (status === 429) {
        setServerError('Bạn đã thử đăng nhập quá nhiều lần. Vui lòng thử lại sau ít phút.');
      } else if (status === 400) {
        setServerError(error.response?.data?.message ?? 'Thông tin đăng nhập không hợp lệ. Vui lòng kiểm tra lại.');
      } else {
        setServerError('Đã xảy ra lỗi. Vui lòng thử lại sau.');
      }
    }
  };

  const fieldError = errors.identifier?.message || errors.password?.message;

  return (
    <div className="font-sans bg-[#F6F1EC] min-h-screen flex items-center justify-center relative overflow-hidden antialiased">
      <div className="absolute top-[-10%] left-[-5%] w-[40%] h-[40%] bg-surface-container-highest rounded-full blur-[100px] opacity-40 pointer-events-none" />
      <div className="absolute bottom-[-10%] right-[-5%] w-[50%] h-[50%] bg-[#f8ddd2] rounded-full blur-[120px] opacity-30 pointer-events-none" />

      <main className="relative z-10 w-full max-w-[480px] p-6">
        <div className="bg-white rounded-3xl p-12 shadow-[0_10px_40px_-10px_rgba(132,81,67,0.1)] border border-[rgba(214,194,189,0.3)] flex flex-col gap-8">
          {/* Logo Header */}
          <div className="text-center flex flex-col items-center gap-4">
            <img src={logo} alt="CareBridge Logo" className="w-16 h-16 rounded-2xl object-cover shadow-sm" />
            <div>
              <h1 className="text-2xl font-semibold leading-8 text-primary tracking-tight m-0">CareBridge</h1>
              <p className="text-sm font-normal leading-5 text-on-surface-variant mt-1 mb-0">Hệ thống quản lý y tế</p>
            </div>
          </div>

          {/* Error Banner */}
          {(serverError || fieldError) && (
            <div className="bg-[#ffdad6] text-[#93000a] p-4 rounded-xl flex items-start gap-3 text-sm leading-5">
              <AlertCircle size={20} className="flex-shrink-0 mt-0.5" />
              <p className="m-0">{serverError || fieldError}</p>
            </div>
          )}
          <p role="status" aria-live="polite" className="sr-only">{serverError ?? ''}</p>

          <div className="flex flex-col gap-3" aria-label="Federated sign in">
            <button type="button" onClick={() => completeFederated('google')} disabled={federatedLoading !== null}
              className="h-12 rounded-full border-2 border-transparent bg-[#C98C7B] text-white font-semibold focus:outline-none focus:ring-4 focus:ring-[#C98C7B]/20 active:scale-95 transition-all">
              {federatedLoading === 'google' ? 'Signing in...' : 'Continue with Google'}
            </button>
            <button type="button" onClick={() => completeFederated('phone')} disabled={federatedLoading !== null}
              className="h-12 rounded-full border-2 border-[#E8DDD6] bg-[#F2EAE4] text-[#5A463F] font-semibold focus:outline-none focus:ring-4 focus:ring-[#C98C7B]/20 active:scale-95 transition-all">
              {federatedLoading === 'phone' ? 'Sending code...' : 'Continue with phone'}
            </button>
            <div id="firebase-recaptcha" />
          </div>

          {/* Login Form */}
          <form className="flex flex-col gap-6" onSubmit={handleSubmit(onSubmit)}>
            <div className="flex flex-col gap-4">
              {/* Email/Phone */}
              <div className="flex flex-col gap-2">
                <label className="text-xs font-medium leading-4 tracking-[0.05em] text-on-background uppercase" htmlFor="identifier">
                  Email hoặc Số điện thoại
                </label>
                <div className="relative">
                  <User size={20} className="absolute left-4 top-1/2 -translate-y-1/2 text-on-surface-variant pointer-events-none" />
                  <input
                    id="identifier"
                    type="text"
                    className="w-full h-12 pl-11 pr-4 rounded-xl border border-outline-variant bg-white font-sans text-sm leading-5 text-on-background outline-none transition-[border-color,box-shadow] duration-200 placeholder:text-on-surface-variant/60 focus:border-primary focus:ring-1 focus:ring-primary"
                    placeholder="Nhập email hoặc SĐT"
                    autoComplete="username"
                    {...register('identifier')}
                  />
                </div>
              </div>

              {/* Password */}
              <div className="flex flex-col gap-2">
                <label className="text-xs font-medium leading-4 tracking-[0.05em] text-on-background uppercase" htmlFor="password">
                  Mật khẩu
                </label>
                <div className="relative">
                  <Lock size={20} className="absolute left-4 top-1/2 -translate-y-1/2 text-on-surface-variant pointer-events-none" />
                  <input
                    id="password"
                    type={showPassword ? 'text' : 'password'}
                    className="w-full h-12 pl-11 pr-11 rounded-xl border border-outline-variant bg-white font-sans text-sm leading-5 text-on-background outline-none transition-[border-color,box-shadow] duration-200 placeholder:text-on-surface-variant/60 focus:border-primary focus:ring-1 focus:ring-primary"
                    placeholder="Nhập mật khẩu"
                    autoComplete="current-password"
                    {...register('password')}
                  />
                  <button
                    type="button"
                    className="absolute right-4 top-1/2 -translate-y-1/2 bg-transparent border-none text-on-surface-variant cursor-pointer flex items-center justify-center p-0 transition-colors duration-200 hover:text-primary"
                    onClick={() => setShowPassword((v) => !v)}
                    aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  >
                    {showPassword ? <Eye size={20} /> : <EyeOff size={20} />}
                  </button>
                </div>
              </div>
            </div>

            {/* Remember Me & Forgot */}
            <div className="flex items-center justify-between">
              <label className="flex items-center gap-2 cursor-pointer">
                <input type="checkbox" className="w-5 h-5 rounded border border-outline-variant accent-primary cursor-pointer" />
                <span className="text-sm leading-5 text-on-surface-variant">Ghi nhớ tôi</span>
              </label>
              <a href="/forgot-password" className="text-sm leading-5 font-medium text-primary no-underline transition-colors duration-200 hover:text-primary-container">
                Quên mật khẩu?
              </a>
            </div>

            {/* Submit */}
            <button
              type="submit"
              className="w-full h-12 bg-primary-container text-on-primary border-none rounded-full font-sans text-base font-semibold leading-5 cursor-pointer flex items-center justify-center gap-2 mt-2 transition-colors duration-200 hover:bg-primary disabled:opacity-60 disabled:cursor-not-allowed"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Đang xử lý...' : 'Đăng nhập'}
              {!isSubmitting && <ArrowRight size={20} />}
            </button>
          </form>

          {/* Footer */}
          <div className="text-center pt-4 border-t border-[rgba(214,194,189,0.3)]">
            <p className="text-sm leading-5 text-on-surface-variant m-0">
              Cần trợ giúp?{' '}
              <a href="#" className="text-primary font-medium no-underline hover:underline">
                Hỗ trợ kỹ thuật
              </a>
            </p>
          </div>
        </div>
      </main>
    </div>
  );
}
