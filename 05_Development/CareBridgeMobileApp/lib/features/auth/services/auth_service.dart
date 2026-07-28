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
typedef AuthTokenPersister = Future<void> Function(AuthResponse response);
typedef AuthPostLoginAction = Future<void> Function();

class GoogleIdTokenAcquirer {
  GoogleIdTokenAcquirer({
    bool? isWeb,
    TargetPlatform? platform,
    GoogleIdTokenProvider? webProvider,
    GoogleIdTokenProvider? nativeProvider,
  }) : _isWeb = isWeb ?? kIsWeb,
       _platform = platform ?? defaultTargetPlatform,
       _webProvider = webProvider,
       _nativeProvider = nativeProvider;

  final bool _isWeb;
  final TargetPlatform _platform;
  final GoogleIdTokenProvider? _webProvider;
  final GoogleIdTokenProvider? _nativeProvider;

  Future<String> acquire() {
    if (_isWeb) {
      return (_webProvider ?? _acquireWebToken)();
    }
    if (_platform != TargetPlatform.android &&
        _platform != TargetPlatform.iOS) {
      throw const FederatedSignInException(FederatedAuthFailure.configuration);
    }
    return (_nativeProvider ?? _acquireNativeToken)();
  }

  static Future<String> _acquireWebToken() async {
    final credential = await firebase.FirebaseAuth.instance.signInWithPopup(
      firebase.GoogleAuthProvider(),
    );
    return _freshIdToken(credential.user);
  }

  static Future<String> _acquireNativeToken() async {
    await GoogleSignIn.instance.initialize();
    final googleUser = await GoogleSignIn.instance.authenticate();
    final googleAuth = googleUser.authentication;
    final credential = firebase.GoogleAuthProvider.credential(
      idToken: googleAuth.idToken,
    );
    final result = await firebase.FirebaseAuth.instance.signInWithCredential(
      credential,
    );
    return _freshIdToken(result.user);
  }

  static Future<String> _freshIdToken(firebase.User? user) async {
    if (user == null) {
      throw StateError('Firebase authentication returned no user');
    }
    final idToken = await user.getIdToken(true);
    if (idToken == null || idToken.isEmpty) {
      throw StateError('Firebase authentication returned no ID token');
    }
    return idToken;
  }
}

class AuthService {
  static final AuthService instance = AuthService._();

  AuthService._({
    AuthApiGet getRequest = apiGet,
    AuthApiPost postRequest = apiPost,
    GoogleIdTokenProvider? googleIdTokenProvider,
    GoogleIdTokenAcquirer? googleIdTokenAcquirer,
    AuthTokenPersister? tokenPersister,
    AuthPostLoginAction? postLoginAction,
  }) : _getRequest = getRequest,
       _postRequest = postRequest,
       _googleIdTokenProvider =
           googleIdTokenProvider ??
           (googleIdTokenAcquirer ?? GoogleIdTokenAcquirer()).acquire,
       _tokenPersister = tokenPersister ?? _persistTokens,
       _postLoginAction = postLoginAction ?? FcmService.instance.registerToken;

  @visibleForTesting
  factory AuthService.forTesting({
    AuthApiGet getRequest = apiGet,
    AuthApiPost postRequest = apiPost,
    GoogleIdTokenProvider? googleIdTokenProvider,
    GoogleIdTokenAcquirer? googleIdTokenAcquirer,
    AuthTokenPersister? tokenPersister,
    AuthPostLoginAction? postLoginAction,
  }) {
    return AuthService._(
      getRequest: getRequest,
      postRequest: postRequest,
      googleIdTokenProvider: googleIdTokenProvider,
      googleIdTokenAcquirer: googleIdTokenAcquirer,
      tokenPersister: tokenPersister,
      postLoginAction: postLoginAction,
    );
  }

  final AuthApiGet _getRequest;
  final AuthApiPost _postRequest;
  final GoogleIdTokenProvider _googleIdTokenProvider;
  final AuthTokenPersister _tokenPersister;
  final AuthPostLoginAction _postLoginAction;

  Future<AuthResponse> federatedGoogle() async {
    try {
      final idToken = await _acquireGoogleIdToken();
      // Keep the backend handoff inside this try/catch so asynchronous API
      // failures are normalized to the same safe, typed error contract as
      // provider and Firebase failures.
      return await federatedWithIdToken(idToken);
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
    return _googleIdTokenProvider();
  }

  Future<AuthResponse> federatedWithIdToken(String idToken) async {
    final res = await _postRequest('/api/v1/auth/federated', {
      'idToken': idToken,
      'deviceInfo': 'CareBridge Flutter',
    });
    final auth = AuthResponse.fromJson(res['data'] as Map<String, dynamic>);
    if (auth.accessToken.trim().isEmpty ||
        auth.refreshToken.trim().isEmpty ||
        auth.user.id.trim().isEmpty) {
      throw const FormatException('Federated response is incomplete');
    }
    await _tokenPersister(auth);
    return auth;
  }

  static Future<void> _persistTokens(AuthResponse auth) async {
    await AuthState.instance.setTokens(
      accessToken: auth.accessToken,
      refreshToken: auth.refreshToken,
      userId: auth.user.id,
      role: auth.user.role,
    );
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

  // UC-03: Password login — returns and persists a token-backed session.
  Future<AuthResponse> login({
    String? email,
    String? phone,
    required String password,
  }) async {
    final body = <String, dynamic>{'password': password};
    if (email != null && email.isNotEmpty) body['email'] = email;
    if (phone != null && phone.isNotEmpty) body['phone'] = phone;
    final res = await _postRequest('/api/v1/auth/login', body);
    final auth = AuthResponse.fromJson(
      res['data'] as Map<String, dynamic>? ?? const <String, dynamic>{},
    );
    if (auth.accessToken.trim().isEmpty ||
        auth.refreshToken.trim().isEmpty ||
        auth.user.id.trim().isEmpty) {
      throw const FormatException('Login response is incomplete');
    }
    await _tokenPersister(auth);
    unawaited(_postLoginAction());
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
