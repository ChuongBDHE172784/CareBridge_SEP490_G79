export interface LoginRequest {
  email?: string;
  phone?: string;
  password: string;
}

export interface ExpertRegisterRequest {
  name: string;
  email: string;
  phone?: string;
  password: string;
  role: 'EXPERT';
}

export interface OtpSendResponse {
  message: string;
  expiresIn: number;
  userId: string | null;
  otpExpiresAt: string | null;
  auth: AuthResponse | null;
}

export interface VerifyOtpRequest {
  email?: string;
  phone?: string;
  otp: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: AuthUserProfile;
}

export interface AuthUserProfile {
  id: string;
  name: string | null;
  email: string | null;
  phone: string | null;
  avatarUrl: string | null;
  role: UserRole | null;
  accountStatus: string;
  emailVerified: boolean;
  phoneVerified: boolean;
  createdAt: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface ForgotPasswordRequest {
  email?: string;
  phone?: string;
}

export interface ForgotPasswordResponse {
  message: string;
}
import type { UserRole } from '../../../shared/auth/authStore';
