export interface LoginRequest {
  email?: string;
  phone?: string;
  password: string;
}

export interface OtpSendResponse {
  message: string;
  expiresIn: number;
  userId: string;
  otpExpiresAt: string;
}

export interface VerifyOtpRequest {
  userId: string;
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
  role: string;
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
