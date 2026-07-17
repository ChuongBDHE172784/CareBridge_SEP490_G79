import 'dart:async';
import 'package:flutter/foundation.dart';
import '../../../../core/network/api_client.dart';
import '../../../../core/auth/auth_state.dart';
import '../../../../core/notifications/fcm_service.dart';
import '../models/auth_model.dart';
import '../models/federated_auth_failure.dart';
import '../models/linked_account.dart';
import 'package:firebase_auth/firebase_auth.dart' as firebase;
import 'package:google_sign_in/google_sign_in.dart';

typedef AuthApiGet = Future<dynamic> Function(String path);
typedef AuthApiPost =
    Future<dynamic> Function(String path, Map<String, dynamic> body);
typedef GoogleIdTokenProvider = Future<String> Function();

class AuthService {
  static final AuthService instance = AuthService._();

  AuthService._({
    AuthApiGet getRequest = apiGet,
    AuthApiPost postRequest = apiPost,
    GoogleIdTokenProvider? googleIdTokenProvider,
  }) : _getRequest = getRequest,
       _postRequest = postRequest,
       _googleIdTokenProvider = googleIdTokenProvider;

  @visibleForTesting
  factory AuthService.forTesting({
    AuthApiGet getRequest = apiGet,
    AuthApiPost postRequest = apiPost,
    GoogleIdTokenProvider? googleIdTokenProvider,
  }) {
    return AuthService._(
      getRequest: getRequest,
      postRequest: postRequest,
      googleIdTokenProvider: googleIdTokenProvider,
    );
  }

  final AuthApiGet _getRequest;
  final AuthApiPost _postRequest;
  final GoogleIdTokenProvider? _googleIdTokenProvider;

  Future<AuthResponse> federatedGoogle() async {
    try {
      final idToken = await _acquireGoogleIdToken();
      return federatedWithIdToken(idToken);
    } catch (error) {
      final exception = FederatedSignInException.from(error);
      debugPrint(
        'Federated Google sign-in failed: '
        'category=${exception.failure.kind.name}, cause=${error.runtimeType}',
      );
      throw exception;
    }
  }

  Future<String> _acquireGoogleIdToken() async {
    final injectedProvider = _googleIdTokenProvider;
    if (injectedProvider != null) return injectedProvider();

    await GoogleSignIn.instance.initialize();
    final googleUser = await GoogleSignIn.instance.authenticate();
    final googleAuth = googleUser.authentication;
    final credential = firebase.GoogleAuthProvider.credential(
      idToken: googleAuth.idToken,
    );
    final firebaseUser =
        (await firebase.FirebaseAuth.instance.signInWithCredential(
          credential,
        )).user;
    if (firebaseUser == null) {
      throw StateError('Firebase authentication returned no user');
    }
    final idToken = await firebaseUser.getIdToken(true);
    if (idToken == null || idToken.isEmpty) {
      throw StateError('Firebase authentication returned no ID token');
    }
    return idToken;
  }

  Future<AuthResponse> federatedWithIdToken(String idToken) async {
    final res = await apiPost('/api/v1/auth/federated', {
      'idToken': idToken,
      'deviceInfo': 'CareBridge Flutter',
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

  Future<LinkedAccount> getLinkedGoogleAccount() async {
    final response = await _getRequest('/api/v1/auth/identities/google');
    final data = _requireLinkedAccountData(response);
    return LinkedAccount.fromJson(data);
  }

  Future<LinkedAccount> linkGoogleAccount() async {
    try {
      final idToken = await _acquireGoogleIdToken();
      final response = await _postRequest('/api/v1/auth/identities/google', {
        'idToken': idToken,
      });
      final data = _requireLinkedAccountData(response);
      final account = LinkedAccount.fromJson(data);
      if (!account.linked) {
        throw const FormatException('Google link was not confirmed');
      }
      return account;
    } catch (error) {
      final exception = LinkedAccountException.from(error);
      debugPrint(
        'Google account linking failed: '
        'category=${exception.failure.kind.name}, cause=${error.runtimeType}',
      );
      throw exception;
    }
  }

  Map<String, dynamic> _requireLinkedAccountData(dynamic response) {
    if (response is! Map<String, dynamic> ||
        response['data'] is! Map<String, dynamic>) {
      throw const FormatException('Missing linked account data');
    }
    return response['data'] as Map<String, dynamic>;
  }

  Future<String> beginPhoneVerification(String phoneNumber) async {
    final completer = Completer<String>();
    await firebase.FirebaseAuth.instance.verifyPhoneNumber(
      phoneNumber: phoneNumber,
      verificationCompleted: (credential) async {
        await firebase.FirebaseAuth.instance.signInWithCredential(credential);
      },
      verificationFailed: completer.completeError,
      codeSent: (verificationId, _) {
        if (!completer.isCompleted) completer.complete(verificationId);
      },
      codeAutoRetrievalTimeout: (verificationId) {
        if (!completer.isCompleted) completer.complete(verificationId);
      },
    );
    return completer.future;
  }

  Future<AuthResponse> confirmPhoneVerification(
    String verificationId,
    String smsCode,
  ) async {
    final credential = firebase.PhoneAuthProvider.credential(
      verificationId: verificationId,
      smsCode: smsCode,
    );
    final user = (await firebase.FirebaseAuth.instance.signInWithCredential(
      credential,
    )).user;
    final idToken = await user?.getIdToken();
    if (idToken == null) {
      throw StateError('Firebase phone authentication returned no ID token');
    }
    return federatedWithIdToken(idToken);
  }

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
    final body = <String, dynamic>{'password': password};
    if (email != null && email.isNotEmpty) body['email'] = email;
    if (phone != null && phone.isNotEmpty) body['phone'] = phone;
    final res = await apiPost('/api/v1/auth/login-direct', body);
    final data = res['data'];
    final auth = AuthResponse.fromJson(data as Map<String, dynamic>);
    await AuthState.instance.setTokens(
      accessToken: auth.accessToken,
      refreshToken: auth.refreshToken,
      userId: auth.user.id,
      role: auth.user.role,
    );
    unawaited(FcmService.instance.registerToken());
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
