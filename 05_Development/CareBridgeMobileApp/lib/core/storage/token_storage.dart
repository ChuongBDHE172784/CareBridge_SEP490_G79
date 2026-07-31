import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../../features/aiTriage/models/triage_continuation.dart';
import '../../features/aiTriage/services/triage_continuation_store.dart';

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
  static final _triageContinuationStore = SecureTriageContinuationStore();

  @override
  Future<void> save({
    required String accessToken,
    required String refreshToken,
    required String userId,
    required String role,
  }) async {
    try {
      final previousUserId = await _store.read(key: _keyUserId);
      if (previousUserId != null && previousUserId != userId) {
        await _store.delete(key: _onboardingDraftKey(previousUserId));
        await Future.wait([
          PostpartumDraftStorageCoordinator.invalidateUser(previousUserId),
          _triageContinuationStore.invalidateUser(previousUserId),
        ]);
      }

      // userId is the commit marker. Readers never accept a partially-written
      // credential bundle while an account switch is in progress.
      await _store.delete(key: _keyUserId);
      await Future.wait([
        _store.write(key: _keyAccess, value: accessToken),
        _store.write(key: _keyRefresh, value: refreshToken),
        _store.write(key: _keyRole, value: role),
      ]);
      await _store.write(key: _keyUserId, value: userId);
    } catch (error, stackTrace) {
      try {
        await _deleteCredentialKeys();
      } catch (_) {
        // Preserve the account cleanup/persistence failure that caused the
        // rollback. AuthState performs another best-effort durable clear.
      }
      Error.throwWithStackTrace(error, stackTrace);
    }
  }

  @override
  Future<Map<String, String?>> load() async {
    final results = await Future.wait([
      _store.read(key: _keyAccess),
      _store.read(key: _keyRefresh),
      _store.read(key: _keyUserId),
      _store.read(key: _keyRole),
    ]);
    if (results.any((value) => value == null || value.isEmpty)) {
      if (results.any((value) => value != null)) {
        await _deleteCredentialKeys();
      }
      return {
        'accessToken': null,
        'refreshToken': null,
        'userId': null,
        'role': null,
      };
    }
    await PostpartumDraftStorageCoordinator.deleteRetiredBabyCreateDrafts(
      results[2]!,
    );
    return {
      'accessToken': results[0],
      'refreshToken': results[1],
      'userId': results[2],
      'role': results[3],
    };
  }

  @override
  Future<void> clear() async {
    final userId = await _store.read(key: _keyUserId);
    await _deleteCredentialKeys();
    if (userId != null) {
      await Future.wait([
        PostpartumDraftStorageCoordinator.invalidateUser(userId),
        _triageContinuationStore.invalidateUser(userId),
        _store.delete(key: _onboardingDraftKey(userId)),
      ]);
    }
  }

  static String _onboardingDraftKey(String userId) =>
      'cb_journey_onboarding_draft_$userId';

  static Future<void> _deleteCredentialKeys() => Future.wait([
    _store.delete(key: _keyAccess),
    _store.delete(key: _keyRefresh),
    _store.delete(key: _keyUserId),
    _store.delete(key: _keyRole),
  ]);

  int triageContinuationGenerationFor(String userId) =>
      _triageContinuationStore.generationFor(userId);

  Future<void> saveTriageContinuation({
    required String userId,
    required String token,
    required String intakeSessionId,
    required DateTime expiresAt,
    int? generation,
  }) => _triageContinuationStore.save(
    userId: userId,
    continuation: PendingTriageContinuation(
      token: token,
      intakeSessionId: intakeSessionId,
      expiresAt: expiresAt,
    ),
    generation: generation ?? _triageContinuationStore.generationFor(userId),
  );

  Future<PendingTriageContinuation?> loadTriageContinuation(String userId) =>
      _triageContinuationStore.read(userId);

  Future<void> invalidateTriageContinuation(String userId) =>
      _triageContinuationStore.invalidateUser(userId);
}

/// Serializes encrypted postpartum-draft operations per account.
///
/// Invalidating an account advances its generation synchronously. Writes that
/// were queued by the previous session are skipped, while an already-running
/// write is followed by the queued cleanup before this method completes.
class PostpartumDraftStorageCoordinator {
  PostpartumDraftStorageCoordinator._();

  static const _store = FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );
  static final Map<String, Future<void>> _queues = {};
  static final Map<String, int> _generations = {};

  static int generationFor(String userId) => _generations[userId] ?? 0;

  static Future<String?> read(String userId, String key) async {
    await (_queues[userId] ?? Future<void>.value()).catchError((_) {});
    return _store.read(key: key);
  }

  static Future<void> write({
    required String userId,
    required String key,
    required String value,
    required int generation,
  }) => _enqueue(userId, () async {
    if (generation != generationFor(userId)) return;
    await _store.write(key: key, value: value);
  });

  static Future<void> delete(String userId, String key) =>
      _enqueue(userId, () => _store.delete(key: key));

  static Future<void> deletePrefix(String userId, String prefix) =>
      _enqueue(userId, () async {
        final values = await _store.readAll();
        final keys = values.keys
            .where((key) => key.startsWith(prefix))
            .toList(growable: false);
        await Future.wait(keys.map((key) => _store.delete(key: key)));
      });

  static Future<void> invalidateUser(String userId) {
    _generations[userId] = generationFor(userId) + 1;
    return _enqueue(userId, () async {
      final prefixes = [
        'cb_postpartum_log_draft_${userId}_',
        '${_retiredBabyCreateDraftPrefix(userId)}_',
      ];
      final values = await _store.readAll();
      final matchingKeys = values.keys
          .where((key) => prefixes.any(key.startsWith))
          .toList(growable: false);
      await Future.wait(matchingKeys.map((key) => _store.delete(key: key)));
    });
  }

  static Future<void> deleteRetiredBabyCreateDrafts(String userId) =>
      deletePrefix(userId, '${_retiredBabyCreateDraftPrefix(userId)}_');

  static String _retiredBabyCreateDraftPrefix(String userId) =>
      ['cb', 'baby', 'create', 'intent', userId].join('_');

  static Future<void> _enqueue(
    String userId,
    Future<void> Function() operation,
  ) {
    final previous = _queues[userId] ?? Future<void>.value();
    final next = previous.catchError((_) {}).then((_) => operation());
    _queues[userId] = next;
    return next.whenComplete(() {
      if (identical(_queues[userId], next)) _queues.remove(userId);
    });
  }
}
