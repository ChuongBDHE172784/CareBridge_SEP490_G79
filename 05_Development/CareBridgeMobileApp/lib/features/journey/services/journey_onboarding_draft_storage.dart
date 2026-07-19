import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../../../core/auth/auth_state.dart';

class JourneyOnboardingDraftStorage {
  static const _storage = FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );
  static const _prefix = 'cb_journey_onboarding_draft';

  Future<Map<String, dynamic>?> read() async {
    final userId = AuthState.instance.userId;
    if (userId == null) return null;
    final raw = await _storage.read(key: _key(userId));
    if (raw == null) return null;
    try {
      final data = jsonDecode(raw) as Map<String, dynamic>;
      if (data['userId'] != userId) {
        await clear();
        return null;
      }
      return data;
    } catch (_) {
      await clear();
      return null;
    }
  }

  Future<void> write(Map<String, dynamic> draft) async {
    final userId = AuthState.instance.userId;
    if (userId == null) return;
    await _storage.write(
      key: _key(userId),
      value: jsonEncode({...draft, 'userId': userId}),
    );
  }

  Future<void> clear() async {
    final userId = AuthState.instance.userId;
    if (userId != null) await _storage.delete(key: _key(userId));
  }

  static String _key(String userId) => '${_prefix}_$userId';
}
