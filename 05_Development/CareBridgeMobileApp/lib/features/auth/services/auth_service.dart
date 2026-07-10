import 'dart:async';
import '../../../../core/network/api_client.dart';
import '../../../../core/auth/auth_state.dart';
import '../../../../core/notifications/fcm_service.dart';
import '../models/auth_model.dart';

class AuthService {
  static final AuthService instance = AuthService._();
  AuthService._();

  // UC-01: Register — sends OTP; tokens not issued until OTP is verified
  Future<OtpSendResponse> register({
    required String name,
    String? email,
    String? phone,
    required String password,
  }) async {
    final body = <String, dynamic>{'name': name, 'password': password};
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
    print('[loginDirect] email=$email phone=$phone pwLen=${password.length}');
    final body = <String, dynamic>{'password': password};
    if (email != null && email.isNotEmpty) body['email'] = email;
    if (phone != null && phone.isNotEmpty) body['phone'] = phone;
    print('[loginDirect] POST body keys: ${body.keys}');
    final res = await apiPost('/api/v1/auth/login-direct', body);
    print('[loginDirect] res type=${res.runtimeType} keys=${res.keys}');
    final data = res['data'];
    print('[loginDirect] data type=${data.runtimeType}');
    final auth = AuthResponse.fromJson(data as Map<String, dynamic>);
    print('[loginDirect] parsed: tokenStart=${auth.accessToken.substring(0, auth.accessToken.length > 20 ? 20 : auth.accessToken.length)}... userId=${auth.user.id} role=${auth.user.role}');
    print('[loginDirect] calling AuthState.setTokens...');
    await AuthState.instance.setTokens(
      accessToken: auth.accessToken,
      refreshToken: auth.refreshToken,
      userId: auth.user.id,
      role: auth.user.role,
    );
    print('[loginDirect] setTokens done. inMemory: access=${AuthState.instance.accessToken != null ? 'set' : 'null'} role=${AuthState.instance.role}');
    unawaited(FcmService.instance.registerToken());
    print('[loginDirect] returning auth');
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
    unawaited(FcmService.instance.registerToken());
    return auth;
  }

  // Resend OTP — rate limited to 1 request per 60 seconds per identifier
  Future<void> resendOtp({String? email, String? phone}) async {
    final body = <String, dynamic>{};
    if (email != null && email.isNotEmpty) body['email'] = email;
    if (phone != null && phone.isNotEmpty) body['phone'] = phone;
    await apiPost('/api/v1/auth/resend-otp', body);
  }

  Future<AuthResponse> refreshSession() async {
    final refreshToken = AuthState.instance.refreshToken;
    if (refreshToken == null || refreshToken.isEmpty) {
      throw ApiException(401, 'Missing refresh token');
    }
    final res = await apiPost('/api/v1/auth/refresh', {
      'refreshToken': refreshToken,
    });
    final auth = AuthResponse.fromJson(res['data'] as Map<String, dynamic>);
    await AuthState.instance.setTokens(
      accessToken: auth.accessToken,
      refreshToken: auth.refreshToken,
      userId: auth.user.id,
      role: auth.user.role,
    );
    return auth;
  }

  Future<UserProfile> selectRole(String role) async {
    final res = await apiPut('/api/v1/auth/role', {'role': role});
    return UserProfile.fromJson(res['data'] as Map<String, dynamic>? ?? {});
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

  // UC-15: Deactivate account — all sessions revoked, data preserved.
  Future<void> deactivateAccount(String confirmPassword) async {
    await apiDelete(
      '/api/v1/auth/deactivate',
      body: {'confirmPassword': confirmPassword},
    );
  }

  // UC-156: Request account deletion — 30-day grace period before permanent deletion.
  Future<void> requestAccountDeletion(String confirmPassword) async {
    await apiPost('/api/v1/account/deletion-request', {
      'confirmPassword': confirmPassword,
    });
  }
}
