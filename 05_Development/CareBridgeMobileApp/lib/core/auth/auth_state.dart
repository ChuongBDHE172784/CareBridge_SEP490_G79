import 'dart:async';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import '../storage/token_storage.dart';

class AuthState extends ChangeNotifier {
  AuthState._({TokenStorage? storage})
    : _storage = storage ?? SecureTokenStorage();

  static final AuthState instance = AuthState._();

  final TokenStorage _storage;

  String? _accessToken;
  String? _refreshToken;
  String? _userId;
  String? _role;
  bool _isRestoring = true;
  String? _blockedReason;

  String? get accessToken => _accessToken;
  String? get refreshToken => _refreshToken;
  String? get role => _role;
  String? get userId => _userId;
  bool get isRestoring => _isRestoring;
  bool get isAuthenticated => _accessToken != null && !_isRestoring;
  String? get blockedReason => _blockedReason;

  /// Hydrate auth state from secure storage on app start.
  /// Validates the stored access token locally (JWT expiry check).
  /// No network call — avoids circular dependency with api_client.
  Future<void> init() async {
    try {
      final tokens = await _storage.load();
      final access = tokens['accessToken'];
      final refresh = tokens['refreshToken'];
      debugPrint('[AuthState] init: storage loaded. access=${access != null ? 'present' : 'null'} refresh=${refresh != null ? 'present' : 'null'}');
      if (access != null && !_isJwtExpired(access)) {
        _accessToken = access;
        _refreshToken = refresh;
        _userId = tokens['userId'];
        _role = tokens['role'];
        debugPrint('[AuthState] init: access token valid → authenticated userId=$_userId role=$_role');
      } else if (refresh != null) {
        _refreshToken = refresh;
        _userId = tokens['userId'];
        _role = tokens['role'];
        _accessToken = 'expired';
        debugPrint('[AuthState] init: access expired, refresh present → sentinel set userId=$_userId role=$_role');
      } else {
        debugPrint('[AuthState] init: no tokens → clearing, redirect to login');
        unawaited(_storage.clear());
      }
    } catch (e) {
      debugPrint('[AuthState] init: error → $e → clearing storage');
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
    debugPrint('[AuthState] setTokens: accessLen=${accessToken.length} tokenStart=${accessToken.substring(0, accessToken.length > 20 ? 20 : accessToken.length)}... userId=$userId role=$role');
    _accessToken = accessToken;
    _refreshToken = refreshToken;
    _userId = userId;
    _role = role;
    notifyListeners();
    debugPrint('[AuthState] setTokens: notifyListeners done, about to save to storage');
    await _storage.save(
      accessToken: accessToken,
      refreshToken: refreshToken,
      userId: userId,
      role: role,
    );
    debugPrint('[AuthState] setTokens: storage.save completed');
  }

  /// Clear in-memory state immediately (synchronous).
  /// Called by api_client on 401; storage clear is fire-and-forget.
  void clearState() {
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
    _blockedReason = null;
    clearState();
    await _storage.clear();
  }

  /// Called when the backend returns 403 ACCOUNT_DISABLED or ACCOUNT_LOCKED.
  /// Sets the reason BEFORE clearState() so the first rebuild routes to
  /// BlockedAccountScreen instead of LoginScreen.
  Future<void> clearWithReason(String reason) async {
    _blockedReason = reason;
    clearState();
    unawaited(_storage.clear());
  }

  /// Called when the user taps "Return to Login" on BlockedAccountScreen.
  /// Clears the reason so the ListenableBuilder transitions to LoginScreen.
  void clearBlockedReason() {
    _blockedReason = null;
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
