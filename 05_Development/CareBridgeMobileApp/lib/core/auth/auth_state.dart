import 'dart:async';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import '../storage/token_storage.dart';
import 'blocked_account_state.dart';

class AuthState extends ChangeNotifier {
  AuthState._({TokenStorage? storage})
    : _storage = storage ?? SecureTokenStorage();

  @visibleForTesting
  AuthState.forTesting({required TokenStorage storage}) : _storage = storage {
    _isRestoring = false;
  }

  static final AuthState instance = AuthState._();

  final TokenStorage _storage;

  String? _accessToken;
  String? _refreshToken;
  String? _userId;
  String? _role;
  bool _isRestoring = true;
  BlockedAccountState? _blockedAccount;
  Completer<void>? _credentialMutationLock;
  int _sessionGeneration = 0;

  String? get accessToken => _accessToken;
  String? get refreshToken => _refreshToken;
  String? get role => _role;
  String? get userId => _userId;
  int get sessionGeneration => _sessionGeneration;
  bool get isRestoring => _isRestoring;
  bool get isAuthenticated => _accessToken != null && !_isRestoring;
  BlockedAccountState? get blockedAccount => _blockedAccount;
  String? get blockedReason => _blockedAccount?.code;

  /// Hydrate auth state from secure storage on app start.
  /// Validates the stored access token locally (JWT expiry check).
  /// No network call — avoids circular dependency with api_client.
  Future<void> init() async {
    try {
      final tokens = await _storage.load();
      final access = tokens['accessToken'];
      final refresh = tokens['refreshToken'];
      debugPrint(
        '[AuthState] init: storage loaded. access=${access != null ? 'present' : 'null'} refresh=${refresh != null ? 'present' : 'null'}',
      );
      if (access != null && !_isJwtExpired(access)) {
        _accessToken = access;
        _refreshToken = refresh;
        _userId = tokens['userId'];
        _role = tokens['role'];
        _sessionGeneration++;
        debugPrint('[AuthState] init: valid credentials restored');
      } else if (refresh != null) {
        _refreshToken = refresh;
        _userId = tokens['userId'];
        _role = tokens['role'];
        _accessToken = 'expired';
        _sessionGeneration++;
        debugPrint('[AuthState] init: refresh required');
      } else {
        debugPrint('[AuthState] init: no tokens → clearing, redirect to login');
        unawaited(_storage.clear());
      }
    } catch (_) {
      debugPrint('[AuthState] init: credential restore failed; clearing');
      unawaited(_storage.clear());
    } finally {
      _isRestoring = false;
      notifyListeners();
    }
  }

  /// Set tokens after successful OTP verification.
  Future<void> setTokens({
    required String accessToken,
    required String refreshToken,
    required String userId,
    required String role,
  }) => _serializeCredentialMutation(() async {
    debugPrint('[AuthState] setTokens: persisting authenticated session');
    try {
      await _storage.save(
        accessToken: accessToken,
        refreshToken: refreshToken,
        userId: userId,
        role: role,
      );
    } catch (error, stackTrace) {
      debugPrint('[AuthState] setTokens: persistence failed; clearing session');
      try {
        await _storage.clear();
      } catch (clearError) {
        debugPrint(
          '[AuthState] setTokens: durable rollback failed: '
          '${clearError.runtimeType}',
        );
      }
      _clearCredentialsInMemory();
      Error.throwWithStackTrace(error, stackTrace);
    }

    _accessToken = accessToken;
    _refreshToken = refreshToken;
    _userId = userId;
    _role = role;
    _sessionGeneration++;
    notifyListeners();
    debugPrint('[AuthState] setTokens: authenticated session published');
  });

  /// Publishes refreshed credentials only while the initiating session is
  /// still current. A completed refresh must never replace a later login.
  Future<bool> setTokensIfCurrent({
    required int expectedGeneration,
    required String expectedAccessToken,
    required String? expectedRefreshToken,
    required String expectedUserId,
    required String accessToken,
    required String refreshToken,
    required String role,
  }) => _serializeCredentialMutation(() async {
    if (!matchesCredentials(
      generation: expectedGeneration,
      accessToken: expectedAccessToken,
      refreshToken: expectedRefreshToken,
      userId: expectedUserId,
      role: role,
    )) {
      return false;
    }

    try {
      await _storage.save(
        accessToken: accessToken,
        refreshToken: refreshToken,
        userId: expectedUserId,
        role: role,
      );
    } catch (error, stackTrace) {
      try {
        await _storage.save(
          accessToken: expectedAccessToken,
          refreshToken: expectedRefreshToken ?? '',
          userId: expectedUserId,
          role: role,
        );
      } catch (_) {
        _clearCredentialsInMemory();
        try {
          await _storage.clear();
        } catch (_) {}
      }
      Error.throwWithStackTrace(error, stackTrace);
    }

    _accessToken = accessToken;
    _refreshToken = refreshToken;
    _userId = expectedUserId;
    _role = role;
    notifyListeners();
    return true;
  });

  bool matchesSession({required int generation, required String userId}) {
    return _sessionGeneration == generation && _userId == userId;
  }

  bool matchesCredentials({
    required int generation,
    required String accessToken,
    required String? refreshToken,
    required String userId,
    required String role,
  }) {
    return matchesSession(generation: generation, userId: userId) &&
        _accessToken == accessToken &&
        _refreshToken == refreshToken &&
        _role == role;
  }

  Future<bool> clearIfCurrentSession({
    required int generation,
    required String userId,
  }) => _serializeCredentialMutation(() async {
    if (!matchesSession(generation: generation, userId: userId)) {
      return false;
    }
    _blockedAccount = null;
    _clearCredentialsInMemory();
    await _storage.clear();
    return true;
  });

  /// Clear in-memory state immediately (synchronous).
  /// Called by api_client on 401; storage clear is fire-and-forget.
  void clearState() {
    _clearCredentialsInMemory();
  }

  void _clearCredentialsInMemory() {
    _accessToken = null;
    _refreshToken = null;
    _userId = null;
    _role = null;
    _sessionGeneration++;
    notifyListeners();
  }

  /// Full logout: clear state + erase secure storage.
  /// Also resets any blocked reason so a normal 401 logout never strands
  /// the user on the blocked screen.
  Future<void> clear() => _serializeCredentialMutation(() async {
    _blockedAccount = null;
    _clearCredentialsInMemory();
    await _storage.clear();
  });

  /// Sets structured restriction state before clearing credentials so routing
  /// transitions directly to BlockedAccountScreen.
  Future<void> clearWithBlockedAccount(BlockedAccountState state) async {
    _blockedAccount = state;
    _clearCredentialsInMemory();
    unawaited(_storage.clear());
  }

  Future<bool> clearWithBlockedAccountIfCurrentSession({
    required int generation,
    required String userId,
    required BlockedAccountState state,
  }) => _serializeCredentialMutation(() async {
    if (!matchesSession(generation: generation, userId: userId)) return false;
    _blockedAccount = state;
    _clearCredentialsInMemory();
    await _storage.clear();
    return true;
  });

  /// Compatibility entry point for older tests and callers.
  Future<void> clearWithReason(String reason) =>
      clearWithBlockedAccount(BlockedAccountState(code: reason));

  void clearBlockedReason() {
    _blockedAccount = null;
    notifyListeners();
  }

  Future<T> _serializeCredentialMutation<T>(
    Future<T> Function() operation,
  ) async {
    late Completer<void> acquiredLock;
    while (true) {
      final pending = _credentialMutationLock;
      if (pending == null) {
        acquiredLock = Completer<void>();
        _credentialMutationLock = acquiredLock;
        break;
      }
      await pending.future;
    }
    try {
      return await operation();
    } finally {
      if (identical(_credentialMutationLock, acquiredLock)) {
        _credentialMutationLock = null;
      }
      acquiredLock.complete();
    }
  }

  static bool _isJwtExpired(String token) {
    try {
      final parts = token.split('.');
      if (parts.length != 3) return true;
      // Pad Base64URL to standard Base64
      var payload = parts[1];
      payload = payload.replaceAll('-', '+').replaceAll('_', '/');
      final pad = payload.length % 4;
      if (pad != 0) payload = payload.padRight(payload.length + (4 - pad), '=');
      final decoded = utf8.decode(base64Decode(payload));
      final json = jsonDecode(decoded) as Map<String, dynamic>;
      final exp = json['exp'] as int;
      return DateTime.now().millisecondsSinceEpoch > exp * 1000;
    } catch (_) {
      return true;
    }
  }
}
