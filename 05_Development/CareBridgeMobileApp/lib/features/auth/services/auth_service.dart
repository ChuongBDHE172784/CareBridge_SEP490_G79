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
typedef AuthSessionPersistenceGuard = bool Function();
typedef WebPhoneVerificationStarter =
    Future<WebPhoneConfirmation> Function(String phoneNumber);
typedef NativePhoneVerificationStarter =
    Future<void> Function({
      required String phoneNumber,
      int? forceResendingToken,
      required Future<void> Function(String idToken) verificationCompleted,
      required void Function(PhoneVerificationFailure error) verificationFailed,
      required void Function(String verificationId, int? resendToken) codeSent,
      required void Function(String verificationId) codeAutoRetrievalTimeout,
    });

enum AuthVerificationMethod {
  email('EMAIL'),
  phone('PHONE');

  const AuthVerificationMethod(this.apiValue);

  final String apiValue;
}

class PhoneVerificationFailure implements Exception {
  const PhoneVerificationFailure(this.code);

  factory PhoneVerificationFailure.fromFirebase(
    firebase.FirebaseAuthException error,
  ) {
    return PhoneVerificationFailure(error.code);
  }

  final String code;

  String get userMessage {
    return switch (code) {
      'invalid-phone-number' =>
        'Số điện thoại không hợp lệ. Hãy nhập theo định dạng quốc tế, ví dụ +84912345678.',
      'too-many-requests' =>
        'Bạn đã yêu cầu quá nhiều mã. Vui lòng thử lại sau.',
      'quota-exceeded' => 'Dịch vụ SMS đang quá tải. Vui lòng thử lại sau.',
      'invalid-verification-code' =>
        'Mã xác thực không đúng. Vui lòng kiểm tra và nhập lại.',
      'session-expired' => 'Mã xác thực đã hết hạn. Vui lòng gửi lại mã mới.',
      'network-request-failed' =>
        'Không thể kết nối dịch vụ xác thực. Vui lòng kiểm tra mạng.',
      _ => 'Không thể xác thực số điện thoại. Vui lòng thử lại.',
    };
  }

  @override
  String toString() => 'PhoneVerificationFailure($code)';
}

abstract interface class PhoneAuthGateway {
  Future<void> verifyPhoneNumber({
    required String phoneNumber,
    int? forceResendingToken,
    required Future<void> Function(String idToken) verificationCompleted,
    required void Function(PhoneVerificationFailure error) verificationFailed,
    required void Function(String verificationId, int? resendToken) codeSent,
    required void Function(String verificationId) codeAutoRetrievalTimeout,
  });

  Future<String> confirmSmsCode({
    required String verificationId,
    required String smsCode,
  });
}

abstract interface class WebPhoneConfirmation {
  Future<String> confirmSmsCode(String smsCode);
}

class _FirebaseWebPhoneConfirmation implements WebPhoneConfirmation {
  const _FirebaseWebPhoneConfirmation(this._confirmation);

  final firebase.ConfirmationResult _confirmation;

  @override
  Future<String> confirmSmsCode(String smsCode) async {
    final user = (await _confirmation.confirm(smsCode)).user;
    final idToken = await user?.getIdToken(true);
    if (idToken == null || idToken.trim().isEmpty) {
      throw StateError('Firebase phone authentication returned no ID token');
    }
    return idToken;
  }
}

class FirebasePhoneAuthGateway implements PhoneAuthGateway {
  FirebasePhoneAuthGateway({
    firebase.FirebaseAuth? firebaseAuth,
    bool? isWeb,
    WebPhoneVerificationStarter? webPhoneVerificationStarter,
    NativePhoneVerificationStarter? nativePhoneVerificationStarter,
  }) : _firebaseAuth = firebaseAuth,
       _isWeb = isWeb ?? kIsWeb,
       _webPhoneVerificationStarter = webPhoneVerificationStarter,
       _nativePhoneVerificationStarter = nativePhoneVerificationStarter;

  final firebase.FirebaseAuth? _firebaseAuth;
  final bool _isWeb;
  final WebPhoneVerificationStarter? _webPhoneVerificationStarter;
  final NativePhoneVerificationStarter? _nativePhoneVerificationStarter;
  final Map<String, WebPhoneConfirmation> _webConfirmations = {};
  int _latestWebStartGeneration = 0;

  firebase.FirebaseAuth get _auth =>
      _firebaseAuth ?? firebase.FirebaseAuth.instance;

  @override
  Future<void> verifyPhoneNumber({
    required String phoneNumber,
    int? forceResendingToken,
    required Future<void> Function(String idToken) verificationCompleted,
    required void Function(PhoneVerificationFailure error) verificationFailed,
    required void Function(String verificationId, int? resendToken) codeSent,
    required void Function(String verificationId) codeAutoRetrievalTimeout,
  }) async {
    if (_isWeb) {
      final startGeneration = ++_latestWebStartGeneration;
      try {
        final confirmation =
            await (_webPhoneVerificationStarter ?? _startWebPhoneVerification)(
              phoneNumber,
            );
        if (startGeneration != _latestWebStartGeneration) return;
        final challengeToken = 'web-phone-$startGeneration';
        _webConfirmations
          ..clear()
          ..[challengeToken] = confirmation;
        codeSent(challengeToken, null);
      } on firebase.FirebaseAuthException catch (error) {
        if (startGeneration == _latestWebStartGeneration) {
          verificationFailed(PhoneVerificationFailure.fromFirebase(error));
        }
      } on PhoneVerificationFailure catch (error) {
        if (startGeneration == _latestWebStartGeneration) {
          verificationFailed(error);
        }
      } catch (_) {
        if (startGeneration == _latestWebStartGeneration) {
          verificationFailed(const PhoneVerificationFailure('unknown'));
        }
      }
      return;
    }

    try {
      await (_nativePhoneVerificationStarter ?? _verifyPhoneNumberNatively)(
        phoneNumber: phoneNumber,
        forceResendingToken: forceResendingToken,
        verificationCompleted: verificationCompleted,
        verificationFailed: verificationFailed,
        codeSent: codeSent,
        codeAutoRetrievalTimeout: codeAutoRetrievalTimeout,
      );
    } on firebase.FirebaseAuthException catch (error) {
      verificationFailed(PhoneVerificationFailure.fromFirebase(error));
    } on PhoneVerificationFailure catch (error) {
      verificationFailed(error);
    } catch (_) {
      verificationFailed(const PhoneVerificationFailure('unknown'));
    }
  }

  Future<WebPhoneConfirmation> _startWebPhoneVerification(
    String phoneNumber,
  ) async {
    final confirmation = await _auth.signInWithPhoneNumber(phoneNumber);
    return _FirebaseWebPhoneConfirmation(confirmation);
  }

  Future<void> _verifyPhoneNumberNatively({
    required String phoneNumber,
    int? forceResendingToken,
    required Future<void> Function(String idToken) verificationCompleted,
    required void Function(PhoneVerificationFailure error) verificationFailed,
    required void Function(String verificationId, int? resendToken) codeSent,
    required void Function(String verificationId) codeAutoRetrievalTimeout,
  }) {
    return _auth.verifyPhoneNumber(
      phoneNumber: phoneNumber,
      forceResendingToken: forceResendingToken,
      verificationCompleted: (credential) async {
        try {
          final idToken = await _signInAndGetFreshIdToken(credential);
          await verificationCompleted(idToken);
        } on firebase.FirebaseAuthException catch (error) {
          verificationFailed(PhoneVerificationFailure.fromFirebase(error));
        } catch (_) {
          verificationFailed(const PhoneVerificationFailure('unknown'));
        }
      },
      verificationFailed: (error) {
        verificationFailed(PhoneVerificationFailure.fromFirebase(error));
      },
      codeSent: codeSent,
      codeAutoRetrievalTimeout: codeAutoRetrievalTimeout,
    );
  }

  @override
  Future<String> confirmSmsCode({
    required String verificationId,
    required String smsCode,
  }) async {
    try {
      if (_isWeb) {
        final confirmation = _webConfirmations[verificationId];
        if (confirmation == null) {
          throw const PhoneVerificationFailure('session-expired');
        }
        final idToken = await confirmation.confirmSmsCode(smsCode);
        _webConfirmations.remove(verificationId);
        return idToken;
      }
      final credential = firebase.PhoneAuthProvider.credential(
        verificationId: verificationId,
        smsCode: smsCode,
      );
      return await _signInAndGetFreshIdToken(credential);
    } on firebase.FirebaseAuthException catch (error) {
      throw PhoneVerificationFailure.fromFirebase(error);
    }
  }

  Future<String> _signInAndGetFreshIdToken(
    firebase.AuthCredential credential,
  ) async {
    final user = (await _auth.signInWithCredential(credential)).user;
    final idToken = await user?.getIdToken(true);
    if (idToken == null || idToken.trim().isEmpty) {
      throw StateError('Firebase phone authentication returned no ID token');
    }
    return idToken;
  }
}

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

  static const String _defaultServerClientId =
      '772548995876-07kb6bsci0s1joi0q81l8qgkmh62d6ak.apps.googleusercontent.com';
  static const String _defaultIosClientId =
      '772548995876-uurp64uddnp6s2b2t7h245c6ia8523ts.apps.googleusercontent.com';

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
    await GoogleSignIn.instance.initialize(
      serverClientId: _defaultServerClientId,
      clientId: defaultTargetPlatform == TargetPlatform.iOS
          ? _defaultIosClientId
          : null,
    );
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
    PhoneAuthGateway? phoneAuthGateway,
  }) : _getRequest = getRequest,
       _postRequest = postRequest,
       _googleIdTokenProvider =
           googleIdTokenProvider ??
           (googleIdTokenAcquirer ?? GoogleIdTokenAcquirer()).acquire,
       _tokenPersister = tokenPersister ?? _persistTokens,
       _postLoginAction = postLoginAction ?? FcmService.instance.registerToken,
       _phoneAuthGateway = phoneAuthGateway ?? FirebasePhoneAuthGateway();

  @visibleForTesting
  factory AuthService.forTesting({
    AuthApiGet getRequest = apiGet,
    AuthApiPost postRequest = apiPost,
    GoogleIdTokenProvider? googleIdTokenProvider,
    GoogleIdTokenAcquirer? googleIdTokenAcquirer,
    AuthTokenPersister? tokenPersister,
    AuthPostLoginAction? postLoginAction,
    PhoneAuthGateway? phoneAuthGateway,
  }) {
    return AuthService._(
      getRequest: getRequest,
      postRequest: postRequest,
      googleIdTokenProvider: googleIdTokenProvider,
      googleIdTokenAcquirer: googleIdTokenAcquirer,
      tokenPersister: tokenPersister,
      postLoginAction: postLoginAction,
      phoneAuthGateway: phoneAuthGateway,
    );
  }

  final AuthApiGet _getRequest;
  final AuthApiPost _postRequest;
  final GoogleIdTokenProvider _googleIdTokenProvider;
  final AuthTokenPersister _tokenPersister;
  final AuthPostLoginAction _postLoginAction;
  final PhoneAuthGateway _phoneAuthGateway;

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
    unawaited(_postLoginAction());
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

  Future<void> beginPhoneVerification({
    required String phoneNumber,
    int? forceResendingToken,
    required Future<void> Function(String idToken) verificationCompleted,
    required void Function(PhoneVerificationFailure error) verificationFailed,
    required void Function(String verificationId, int? resendToken) codeSent,
    required void Function(String verificationId) codeAutoRetrievalTimeout,
  }) {
    return _phoneAuthGateway.verifyPhoneNumber(
      phoneNumber: phoneNumber,
      forceResendingToken: forceResendingToken,
      verificationCompleted: verificationCompleted,
      verificationFailed: verificationFailed,
      codeSent: codeSent,
      codeAutoRetrievalTimeout: codeAutoRetrievalTimeout,
    );
  }

  Future<String> confirmPhoneSmsCode({
    required String verificationId,
    required String smsCode,
  }) {
    return _phoneAuthGateway.confirmSmsCode(
      verificationId: verificationId,
      smsCode: smsCode,
    );
  }

  // UC-01: Register — sends OTP; tokens not issued until OTP is verified
  Future<OtpSendResponse> register({
    required String name,
    String? email,
    String? phone,
    required String password,
    String? role,
    AuthVerificationMethod verificationMethod = AuthVerificationMethod.email,
  }) async {
    final body = <String, dynamic>{
      'name': name,
      'password': password,
      'verificationMethod': verificationMethod.apiValue,
    };
    if (email != null && email.isNotEmpty) body['email'] = email;
    if (phone != null && phone.isNotEmpty) body['phone'] = phone;
    if (role != null && role.isNotEmpty) body['role'] = role;
    final res = await _postRequest('/api/v1/auth/register', body);
    return OtpSendResponse.fromJson(res['data'] as Map<String, dynamic>);
  }

  Future<AuthResponse> registerWithPhoneIdToken({
    required String idToken,
    required String name,
    String? email,
    required String phone,
    required String password,
    String? role,
    AuthSessionPersistenceGuard? shouldPersistSession,
  }) async {
    final body = <String, dynamic>{
      'idToken': idToken,
      'name': name,
      'phone': phone,
      'password': password,
      'deviceInfo': 'CareBridge Flutter',
    };
    if (email != null && email.trim().isNotEmpty) {
      body['email'] = email.trim();
    }
    if (role != null && role.trim().isNotEmpty) body['role'] = role;
    final response = await _postRequest('/api/v1/auth/phone/register', body);
    return _parseAndPersistSession(
      response,
      shouldPersistSession: shouldPersistSession,
    );
  }

  Future<AuthResponse> loginWithPhoneIdToken(
    String idToken, {
    AuthSessionPersistenceGuard? shouldPersistSession,
  }) async {
    final response = await _postRequest('/api/v1/auth/phone/login', {
      'idToken': idToken,
      'deviceInfo': 'CareBridge Flutter',
    });
    return _parseAndPersistSession(
      response,
      shouldPersistSession: shouldPersistSession,
    );
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
    return _parseAndPersistSession(res);
  }

  Future<AuthResponse> _parseAndPersistSession(
    dynamic response, {
    AuthSessionPersistenceGuard? shouldPersistSession,
  }) async {
    final auth = AuthResponse.fromJson(
      response['data'] as Map<String, dynamic>? ?? const <String, dynamic>{},
    );
    if (auth.accessToken.trim().isEmpty ||
        auth.refreshToken.trim().isEmpty ||
        auth.user.id.trim().isEmpty) {
      throw const FormatException('Login response is incomplete');
    }
    if (shouldPersistSession != null && !shouldPersistSession()) return auth;
    await _tokenPersister(auth);
    unawaited(_postLoginAction());
    return auth;
  }

  Future<void> submitAccountLockAppeal({
    required String appealToken,
    required String reason,
  }) async {
    await _postRequest('/api/v1/auth/lock-appeals', {
      'appealToken': appealToken,
      'reason': reason,
    });
  }

  // UC-02: Verify OTP — completes login/registration and persists tokens
  Future<AuthResponse> verifyOtp({
    String? email,
    String? phone,
    required String otp,
    AuthSessionPersistenceGuard? shouldPersistSession,
  }) async {
    final body = <String, dynamic>{'otp': otp};
    if (email != null && email.isNotEmpty) body['email'] = email;
    if (phone != null && phone.isNotEmpty) body['phone'] = phone;
    final response = await _postRequest('/api/v1/auth/verify-otp', body);
    return _parseAndPersistSession(
      response,
      shouldPersistSession: shouldPersistSession,
    );
  }

  // Resend OTP — rate limited to 1 request per 60 seconds per identifier
  Future<void> resendOtp({String? email, String? phone}) async {
    final body = <String, dynamic>{};
    if (email != null && email.isNotEmpty) body['email'] = email;
    if (phone != null && phone.isNotEmpty) body['phone'] = phone;
    await _postRequest('/api/v1/auth/resend-otp', body);
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
    unawaited(_postLoginAction());
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
  Future<void> logout({
    String? refreshToken,
    String? token,
    String? expectedAccountId,
  }) async {
    final body = <String, dynamic>{};
    if (refreshToken != null && refreshToken.isNotEmpty) {
      body['refreshToken'] = refreshToken;
    }
    await apiPost(
      '/api/v1/auth/logout',
      body,
      token: token,
      expectedAccountId: expectedAccountId,
    );
  }

  // UC-15: Deactivate account — all sessions revoked, data preserved.
  // This is the only account-closure path; the 30-day deletion queue (UC-156)
  // was retired together with the account_deletion_requests table.
  Future<void> deactivateAccount(
    String confirmPassword, {
    String? reason,
  }) async {
    await apiDelete(
      '/api/v1/auth/deactivate',
      body: {
        'confirmPassword': confirmPassword,
        if (reason != null && reason.trim().isNotEmpty) 'reason': reason.trim(),
      },
    );
  }
}
