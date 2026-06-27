import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Heart, User, Lock, EyeOff, Eye, ArrowRight, AlertCircle } from 'lucide-react';
import { login } from '../services/authApi';
import './LoginPage.css';

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
      if (status === 429) {
        setServerError('Tài khoản tạm khóa do đăng nhập sai nhiều lần. Vui lòng thử lại sau 15 phút.');
      } else if (status === 403) {
        const code = error.response?.data?.error;
        if (code === 'ACCOUNT_DISABLED' || code === 'ACCOUNT_LOCKED') return;
        setServerError('Tài khoản không có quyền truy cập.');
      } else {
        setServerError(error.response?.data?.message ?? 'Email hoặc mật khẩu không chính xác. Vui lòng thử lại.');
      }
    }
  };

  const fieldError = errors.identifier?.message || errors.password?.message;

  return (
    <div className="login-wrapper">
      <div className="login-blob-top" />
      <div className="login-blob-bottom" />

      <main className="login-card">
        <div className="login-card-inner">
          {/* Logo Header */}
          <div className="login-header">
            <div className="login-logo">
              <Heart size={28} strokeWidth={2} />
            </div>
            <div>
              <h1 className="login-title">CareBridge</h1>
              <p className="login-subtitle">Hệ thống quản lý y tế</p>
            </div>
          </div>

          {/* Error Banner */}
          {(serverError || fieldError) && (
            <div className="login-error">
              <AlertCircle size={20} />
              <p>{serverError || fieldError}</p>
            </div>
          )}

          {/* Login Form */}
          <form className="login-form" onSubmit={handleSubmit(onSubmit)}>
            <div className="login-fields">
              {/* Email/Phone */}
              <div className="login-field">
                <label className="login-label" htmlFor="identifier">
                  Email hoặc Số điện thoại
                </label>
                <div className="login-input-wrapper">
                  <User size={20} className="login-input-icon" />
                  <input
                    id="identifier"
                    type="text"
                    className="login-input"
                    placeholder="Nhập email hoặc SĐT"
                    autoComplete="username"
                    {...register('identifier')}
                  />
                </div>
              </div>

              {/* Password */}
              <div className="login-field">
                <label className="login-label" htmlFor="password">
                  Mật khẩu
                </label>
                <div className="login-input-wrapper">
                  <Lock size={20} className="login-input-icon" />
                  <input
                    id="password"
                    type={showPassword ? 'text' : 'password'}
                    className="login-input login-input--password"
                    placeholder="Nhập mật khẩu"
                    autoComplete="current-password"
                    {...register('password')}
                  />
                  <button
                    type="button"
                    className="login-toggle-password"
                    onClick={() => setShowPassword((v) => !v)}
                    aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  >
                    {showPassword ? <Eye size={20} /> : <EyeOff size={20} />}
                  </button>
                </div>
              </div>
            </div>

            {/* Remember Me & Forgot */}
            <div className="login-options">
              <label className="login-remember">
                <input type="checkbox" />
                <span>Ghi nhớ tôi</span>
              </label>
              <a href="/forgot-password" className="login-forgot">
                Quên mật khẩu?
              </a>
            </div>

            {/* Submit */}
            <button type="submit" className="login-submit" disabled={isSubmitting}>
              {isSubmitting ? 'Đang xử lý...' : 'Đăng nhập'}
              {!isSubmitting && <ArrowRight size={20} />}
            </button>
          </form>

          {/* Footer */}
          <div className="login-footer">
            <p>
              Cần trợ giúp? <a href="#">Hỗ trợ kỹ thuật</a>
            </p>
          </div>
        </div>
      </main>
    </div>
  );
}
