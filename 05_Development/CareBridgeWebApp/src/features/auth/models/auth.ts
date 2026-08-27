export interface LoginRequest {
  email?: string;
  phone?: string;
  password: string;
}

export type VerificationMethod = 'EMAIL' | 'PHONE';

export interface RegisterRequest {
  name: string;
  email: string;
  phone: string;
  password: string;
  role?: UserRole;
  verificationMethod: VerificationMethod;
}

export interface RegistrationDraft {
  name: string;
  email: string;
  phone: string;
  password: string;
  role: UserRole;
}

export interface PhoneAuthRequest {
  idToken: string;
  deviceInfo?: string;
}

export interface PhoneRegisterRequest
  extends Omit<RegisterRequest, 'phone' | 'verificationMethod'>,
    PhoneAuthRequest {
  phone: string;
}

export interface ExpertRegisterRequest {
  name: string;
  email: string;
  phone: string;
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

export interface FederatedAuthResponse extends AuthResponse {
  newUser: boolean;
  profileCompleted: boolean;
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
  contact: string;
}

export interface ForgotPasswordResponse {
  message: string;
  expiresIn: number;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
  confirmPassword: string;
}

export interface ResetPasswordResponse {
  message: string;
}

export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}
import type { UserRole } from '../../../shared/auth/authStore';
