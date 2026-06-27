import '../../../../core/network/api_client.dart';
import '../../../../core/auth/auth_state.dart';
import '../models/auth_model.dart';

class AuthService {
  static final AuthService instance = AuthService._();
  AuthService._();

  // UC-01: Register — sends OTP; tokens not issued until OTP is verified
  Future<OtpSendResponse> register({
    String? email,
    String? phone,
    required String password,
    required String role,
  }) async {
    final body = <String, dynamic>{'password': password, 'role': role};
    if (email != null && email.isNotEmpty) body['email'] = email;
    if (phone != null && phone.isNotEmpty) body['phone'] = phone;
    final res = await apiPost('/api/v1/auth/register', body);
    return OtpSendResponse.fromJson(res['data'] as Map<String, dynamic>);
  }

  // UC-03: Login — sends OTP; tokens not issued until OTP is verified
  Future<OtpSendResponse> login({
    String? email,
    String? phone,
    required String password,
  }) async {
    final body = <String, dynamic>{'password': password};
    if (email != null && email.isNotEmpty) body['email'] = email;
    if (phone != null && phone.isNotEmpty) body['phone'] = phone;
    final res = await apiPost('/api/v1/auth/login', body);
    return OtpSendResponse.fromJson(res['data'] as Map<String, dynamic>);
  }

  // Dev/test: Login without OTP — returns tokens directly
  Future<AuthResponse> loginDirect({
    String? email,
    String? phone,
    required String password,
  }) async {
    final body = <String, dynamic>{'password': password};
    if (email != null && email.isNotEmpty) body['email'] = email;
    if (phone != null && phone.isNotEmpty) body['phone'] = phone;
    final res = await apiPost('/api/v1/auth/login-direct', body);
    final auth = AuthResponse.fromJson(res['data'] as Map<String, dynamic>);
    await AuthState.instance.setTokens(
      accessToken: auth.accessToken,
      refreshToken: auth.refreshToken,
      userId: auth.user.id,
      role: auth.user.role,
    );
    return auth;
  }

  // UC-02: Verify OTP — completes login/registration and persists tokens
  Future<AuthResponse> verifyOtp({
    String? email,
    String? phone,
    required String otp,
  }) async {
    final body = <String, dynamic>{'otp': otp};
    if (email != null && email.isNotEmpty) body['email'] = email;
    if (phone != null && phone.isNotEmpty) body['phone'] = phone;
    final res = await apiPost('/api/v1/auth/verify-otp', body);
    final auth = AuthResponse.fromJson(res['data'] as Map<String, dynamic>);
    await AuthState.instance.setTokens(
      accessToken: auth.accessToken,
      refreshToken: auth.refreshToken,
      userId: auth.user.id,
      role: auth.user.role,
    );
    return auth;
  }

  // Resend OTP — rate limited to 1 request per 60 seconds per identifier
  Future<void> resendOtp({String? email, String? phone}) async {
    final body = <String, dynamic>{};
    if (email != null && email.isNotEmpty) body['email'] = email;
    if (phone != null && phone.isNotEmpty) body['phone'] = phone;
    await apiPost('/api/v1/auth/resend-otp', body);
  }

  // Current authenticated user profile for account-facing UI.
  Future<UserProfile> getProfile() async {
    final res = await apiGet('/api/v1/auth/profile');
    return UserProfile.fromJson(res['data'] as Map<String, dynamic>? ?? {});
  }

  // UC-04: Logout current session using the backend's real contract.
  // The request body may be empty because the backend can revoke by JWT sid.
  Future<void> logout({String? refreshToken}) async {
    final body = <String, dynamic>{};
    if (refreshToken != null && refreshToken.isNotEmpty) {
      body['refreshToken'] = refreshToken;
    }
    await apiPost('/api/v1/auth/logout', body);
  }
}
