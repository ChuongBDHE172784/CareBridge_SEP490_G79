import 'package:flutter_secure_storage/flutter_secure_storage.dart';

abstract class TokenStorage {
  Future<void> save({
    required String accessToken,
    required String refreshToken,
    required String userId,
    required String role,
  });
  Future<Map<String, String?>> load();
  Future<void> clear();
}

class SecureTokenStorage implements TokenStorage {
  static const _store = FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );
  static const _keyAccess = 'cb_access_token';
  static const _keyRefresh = 'cb_refresh_token';
  static const _keyUserId = 'cb_user_id';
  static const _keyRole = 'cb_role';

  @override
  Future<void> save({
    required String accessToken,
    required String refreshToken,
    required String userId,
    required String role,
  }) async {
    await Future.wait([
      _store.write(key: _keyAccess, value: accessToken),
      _store.write(key: _keyRefresh, value: refreshToken),
      _store.write(key: _keyUserId, value: userId),
      _store.write(key: _keyRole, value: role),
    ]);
  }

  @override
  Future<Map<String, String?>> load() async {
    final results = await Future.wait([
      _store.read(key: _keyAccess),
      _store.read(key: _keyRefresh),
      _store.read(key: _keyUserId),
      _store.read(key: _keyRole),
    ]);
    return {
      'accessToken': results[0],
      'refreshToken': results[1],
      'userId': results[2],
      'role': results[3],
    };
  }

  @override
  Future<void> clear() => _store.deleteAll();
}
