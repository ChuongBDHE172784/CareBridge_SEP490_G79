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

  String? get accessToken => _accessToken;
  String? get refreshToken => _refreshToken;
  String? get role => _role;
  String? get userId => _userId;
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
        debugPrint('[AuthState] init: valid credentials restored');
      } else if (refresh != null) {
        _refreshToken = refresh;
        _userId = tokens['userId'];
        _role = tokens['role'];
        _accessToken = 'expired';
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
  }) async {
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
    notifyListeners();
    debugPrint('[AuthState] setTokens: authenticated session published');
  }

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
    notifyListeners();
  }

  /// Full logout: clear state + erase secure storage.
  /// Also resets any blocked reason so a normal 401 logout never strands
  /// the user on the blocked screen.
  Future<void> clear() async {
    _blockedAccount = null;
    clearState();
    await _storage.clear();
  }

  /// Sets structured restriction state before clearing credentials so routing
  /// transitions directly to BlockedAccountScreen.
  Future<void> clearWithBlockedAccount(BlockedAccountState state) async {
    _blockedAccount = state;
    clearState();
    unawaited(_storage.clear());
  }

  /// Compatibility entry point for older tests and callers.
  Future<void> clearWithReason(String reason) =>
      clearWithBlockedAccount(BlockedAccountState(code: reason));

  void clearBlockedReason() {
    _blockedAccount = null;
    notifyListeners();
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
