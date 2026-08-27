import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter/foundation.dart';

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
  Future<void> clear({String? expectedUserId});
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
          RetiredDraftStorageCleanup.purgeForUser(previousUserId),
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
        final storedUserId = results[2];
        await clear(
          expectedUserId: storedUserId != null && storedUserId.isNotEmpty
              ? storedUserId
              : null,
        );
      }
      return {
        'accessToken': null,
        'refreshToken': null,
        'userId': null,
        'role': null,
      };
    }
    await RetiredDraftStorageCleanup.purgeForUser(results[2]!);
    return {
      'accessToken': results[0],
      'refreshToken': results[1],
      'userId': results[2],
      'role': results[3],
    };
  }

  @override
  Future<void> clear({String? expectedUserId}) =>
      clearCredentialBundleFailClosed(
        expectedUserId: expectedUserId,
        readStoredUserId: () => _store.read(key: _keyUserId),
        invalidateCommitMarker: _invalidateCredentialCommitMarker,
        deleteCredentialPayload: _deleteCredentialPayloadKeys,
        clearAccountState: (userId) => Future.wait([
          RetiredDraftStorageCleanup.purgeForUser(userId),
          _triageContinuationStore.invalidateUser(userId),
          _store.delete(key: _onboardingDraftKey(userId)),
        ]),
      );

  static String _onboardingDraftKey(String userId) =>
      'cb_journey_onboarding_draft_$userId';

  static Future<void> _deleteCredentialKeys() =>
      clearCredentialBundleFailClosed(
        invalidateCommitMarker: _invalidateCredentialCommitMarker,
        deleteCredentialPayload: _deleteCredentialPayloadKeys,
      );

  /// The user ID is the bundle commit marker. Deleting it first prevents a
  /// partially failed cleanup from ever being accepted by [load]. If delete
  /// itself fails, an empty marker provides the same fail-closed behavior.
  static Future<void> _invalidateCredentialCommitMarker() async {
    try {
      await _store.delete(key: _keyUserId);
    } catch (deleteError, deleteStackTrace) {
      try {
        await _store.write(key: _keyUserId, value: '');
      } catch (_) {
        Error.throwWithStackTrace(deleteError, deleteStackTrace);
      }
    }
  }

  static Future<void> _deleteCredentialPayloadKeys() => Future.wait([
    _store.delete(key: _keyAccess),
    _store.delete(key: _keyRefresh),
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

/// Runs credential invalidation as a fail-closed sequence while preserving the
/// first failure for the caller. Every later cleanup step is still attempted,
/// so one broken secure-storage operation cannot prevent payload deletion or
/// account-scoped AI Triage/draft invalidation.
@visibleForTesting
Future<void> clearCredentialBundleFailClosed({
  String? expectedUserId,
  Future<String?> Function()? readStoredUserId,
  required Future<void> Function() invalidateCommitMarker,
  required Future<void> Function() deleteCredentialPayload,
  Future<void> Function(String userId)? clearAccountState,
}) async {
  Object? firstError;
  StackTrace? firstStackTrace;

  void capture(Object error, StackTrace stackTrace) {
    firstError ??= error;
    firstStackTrace ??= stackTrace;
  }

  var cleanupUserId = expectedUserId;
  if (cleanupUserId == null &&
      readStoredUserId != null &&
      clearAccountState != null) {
    try {
      cleanupUserId = await readStoredUserId();
    } catch (error, stackTrace) {
      capture(error, stackTrace);
    }
  }

  try {
    await invalidateCommitMarker();
  } catch (error, stackTrace) {
    capture(error, stackTrace);
  }

  try {
    await deleteCredentialPayload();
  } catch (error, stackTrace) {
    capture(error, stackTrace);
  }

  if (cleanupUserId != null && clearAccountState != null) {
    try {
      await clearAccountState(cleanupUserId);
    } catch (error, stackTrace) {
      capture(error, stackTrace);
    }
  }

  final error = firstError;
  if (error != null) {
    Error.throwWithStackTrace(error, firstStackTrace!);
  }
}

/// Removes encrypted drafts left by features that are no longer available.
class RetiredDraftStorageCleanup {
  RetiredDraftStorageCleanup._();

  static const _store = FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );
  static final Map<String, Future<void>> _queues = {};

  static Future<void> purgeForUser(String userId) => _enqueue(userId, () async {
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
