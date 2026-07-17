import apiClient from '../../../shared/api/apiClient';
import type { ApiResponse } from '../models/user';
import type {
  LoginRequest,
  OtpSendResponse,
  VerifyOtpRequest,
  AuthResponse,
  RefreshTokenRequest,
  ForgotPasswordRequest,
  ForgotPasswordResponse,
  ExpertRegisterRequest,
} from '../models/auth';
import { useAuthStore } from '../../../shared/auth/authStore';
import type { UserProfile } from '../models/user';

export async function registerExpert(request: Omit<ExpertRegisterRequest, 'role'>): Promise<OtpSendResponse> {
  const { data } = await apiClient.post<ApiResponse<OtpSendResponse>>('/api/v1/auth/register', {
    ...request,
    phone: request.phone || undefined,
    role: 'EXPERT',
  });
  return data.data;
}

// UC-03: Login — sends OTP, does NOT return tokens yet
export async function login(request: LoginRequest): Promise<OtpSendResponse> {
  const { data } = await apiClient.post<ApiResponse<OtpSendResponse>>('/api/v1/auth/login', request);
  return data.data;
}

// UC-02: Verify OTP — completes login, returns tokens + user profile
export async function verifyOtp(request: VerifyOtpRequest): Promise<AuthResponse> {
  const { data } = await apiClient.post<ApiResponse<AuthResponse>>('/api/v1/auth/verify-otp', request);
  const { accessToken, refreshToken, user } = data.data;
  useAuthStore.getState().setTokens(accessToken, refreshToken);
  useAuthStore.getState().setUser({
    id: user.id,
    phone: user.phone ?? '',
    name: user.name,
    avatarUrl: user.avatarUrl,
    role: user.role as ReturnType<typeof useAuthStore.getState>['user'] extends { role: infer R } ? R : never,
  });
  return data.data;
}

// UC-02: Resend OTP
export async function resendOtp(identifier: { phone?: string; email?: string }): Promise<OtpSendResponse> {
  const { data } = await apiClient.post<ApiResponse<OtpSendResponse>>('/api/v1/auth/resend-otp', identifier);
  return data.data;
}

// UC-04: Logout
export async function logout(refreshToken?: string): Promise<void> {
  await apiClient.post('/api/v1/auth/logout', refreshToken ? { refreshToken } : {});
  useAuthStore.getState().logout();
}

// UC-08: Fetch current user profile
export async function fetchProfile(): Promise<UserProfile> {
  const { data } = await apiClient.get<ApiResponse<UserProfile>>('/api/v1/auth/profile');
  return data.data;
}

// UC-08: Update current user profile (avatarUrl, phone, displayName, etc.)
export async function updateUserProfile(body: {
	displayName?: string;
	avatarUrl?: string | null;
	phoneNumber?: string;
	dateOfBirth?: string;
	area?: string;
}): Promise<UserProfile> {
	const { data } = await apiClient.patch<ApiResponse<UserProfile>>('/api/v1/profile', body);
	return data.data;
}

// UC-05: Forgot password
export async function forgotPassword(request: ForgotPasswordRequest): Promise<ForgotPasswordResponse> {
  const { data } = await apiClient.post<ApiResponse<ForgotPasswordResponse>>('/api/v1/auth/forgot-password', request);
  return data.data;
}

// Token refresh (internal)
export async function refreshAccessToken(request: RefreshTokenRequest): Promise<AuthResponse> {
  const { data } = await apiClient.post<ApiResponse<AuthResponse>>('/api/v1/auth/refresh', request);
  useAuthStore.getState().setTokens(data.data.accessToken, data.data.refreshToken);
  return data.data;
}
