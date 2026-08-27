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
  ResetPasswordRequest,
  ResetPasswordResponse,
  ChangePasswordRequest,
  ExpertRegisterRequest,
  RegisterRequest,
  PhoneAuthRequest,
  PhoneRegisterRequest,
  FederatedAuthResponse,
} from '../models/auth';
import { useAuthStore } from '../../../shared/auth/authStore';
import type { UserProfile } from '../models/user';
import type { UserRole } from '../../../shared/auth/authStore';

export async function registerExpert(request: Omit<ExpertRegisterRequest, 'role'>): Promise<OtpSendResponse> {
  const { data } = await apiClient.post<ApiResponse<OtpSendResponse>>('/api/v1/auth/register', {
    ...request,
    role: 'EXPERT',
    verificationMethod: 'EMAIL',
  });
  return data.data;
}

export async function federatedAuthenticate(idToken: string, deviceInfo = navigator.userAgent): Promise<FederatedAuthResponse> {
  const { data } = await apiClient.post<ApiResponse<FederatedAuthResponse>>('/api/v1/auth/federated', {
    idToken,
    deviceInfo,
  });
  return data.data;
}

export async function registerUser(request: RegisterRequest): Promise<OtpSendResponse> {
  const { data } = await apiClient.post<ApiResponse<OtpSendResponse>>('/api/v1/auth/register', {
    ...request,
  });
  return data.data;
}

function requireCompleteAuthResponse<T extends AuthResponse>(auth: T): T {
  if (!auth?.accessToken?.trim() || !auth.refreshToken?.trim() || !auth.user?.id?.trim()) {
    throw new Error('Login response is incomplete');
  }
  return auth;
}

export async function registerWithPhone(
  request: PhoneRegisterRequest,
): Promise<AuthResponse> {
  const { data } = await apiClient.post<ApiResponse<AuthResponse>>('/api/v1/auth/phone/register', {
    ...request,
    deviceInfo: request.deviceInfo ?? navigator.userAgent,
  });
  return requireCompleteAuthResponse(data.data);
}

export async function loginWithPhone(request: PhoneAuthRequest): Promise<FederatedAuthResponse> {
  const { data } = await apiClient.post<ApiResponse<FederatedAuthResponse>>('/api/v1/auth/phone/login', {
    ...request,
    deviceInfo: request.deviceInfo ?? navigator.userAgent,
  });
  return requireCompleteAuthResponse(data.data);
}

// UC-03: Password login returns the canonical token-backed session.
export async function login(request: LoginRequest): Promise<AuthResponse> {
  const { data } = await apiClient.post<ApiResponse<AuthResponse>>('/api/v1/auth/login', request);
  return requireCompleteAuthResponse(data.data);
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
    role: user.role,
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

export async function selectRole(role: Extract<UserRole, 'MOTHER' | 'FAMILY' | 'EXPERT'>): Promise<UserProfile> {
  const { data } = await apiClient.put<ApiResponse<UserProfile>>('/api/v1/auth/role', { role });
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
	await apiClient.patch<ApiResponse<unknown>>('/api/v1/profile', body);
	// The profile aggregate returns a different DTO shape from the auth profile
	// endpoint. Re-fetch the canonical auth profile so callers retain id, role,
	// email, and verification flags after an edit.
	return fetchProfile();
}

// UC-05: Forgot password
export async function forgotPassword(request: ForgotPasswordRequest): Promise<ForgotPasswordResponse> {
  const { data } = await apiClient.post<ApiResponse<ForgotPasswordResponse>>('/api/v1/auth/forgot-password', request);
  return data.data;
}

export async function resetPassword(request: ResetPasswordRequest): Promise<ResetPasswordResponse> {
  const { data } = await apiClient.post<ApiResponse<ResetPasswordResponse>>('/api/v1/auth/reset-password', request);
  return data.data;
}

export async function changePassword(request: ChangePasswordRequest): Promise<void> {
  await apiClient.put('/api/v1/auth/change-password', request);
}

// Token refresh (internal)
export async function refreshAccessToken(request: RefreshTokenRequest): Promise<AuthResponse> {
  const { data } = await apiClient.post<ApiResponse<AuthResponse>>('/api/v1/auth/refresh', request);
  useAuthStore.getState().setTokens(data.data.accessToken, data.data.refreshToken);
  return data.data;
}
