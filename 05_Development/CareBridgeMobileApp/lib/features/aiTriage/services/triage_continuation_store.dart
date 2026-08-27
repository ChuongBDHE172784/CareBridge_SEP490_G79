import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../models/triage_continuation.dart';

abstract interface class TriageContinuationStore {
  int generationFor(String userId);

  Future<PendingTriageContinuation?> read(String userId);

  Future<void> save({
    required String userId,
    required PendingTriageContinuation continuation,
    required int generation,
  });

  Future<void> invalidateUser(String userId);
}

/// Account-scoped encrypted persistence for restart-safe triage continuation.
///
/// Operations are serialized per account. Invalidation advances its generation
/// synchronously, so writes queued by an earlier authenticated session cannot
/// restore retired state.
class SecureTriageContinuationStore implements TriageContinuationStore {
  SecureTriageContinuationStore({FlutterSecureStorage? storage})
    : _storage = storage ?? _defaultStorage;

  static const _defaultStorage = FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );
  static final Map<String, Future<void>> _queues = {};
  static final Map<String, int> _generations = {};

  final FlutterSecureStorage _storage;

  static String storageKeyFor(String userId) =>
      'cb_triage_continuation_$userId';

  @override
  int generationFor(String userId) => _generations[userId] ?? 0;

  @override
  Future<PendingTriageContinuation?> read(String userId) async {
    await (_queues[userId] ?? Future<void>.value()).catchError((_) {});
    final encoded = await _storage.read(key: storageKeyFor(userId));
    if (encoded == null) return null;

    try {
      return PendingTriageContinuation.fromJson(
        jsonDecode(encoded) as Map<String, dynamic>,
      );
    } on FormatException {
      return null;
    } on TypeError {
      return null;
    }
  }

  @override
  Future<void> save({
    required String userId,
    required PendingTriageContinuation continuation,
    required int generation,
  }) => _enqueue(userId, () async {
    if (generation != generationFor(userId)) return;
    await _storage.write(
      key: storageKeyFor(userId),
      value: jsonEncode(continuation.toJson()),
    );
  });

  @override
  Future<void> invalidateUser(String userId) {
    _generations[userId] = generationFor(userId) + 1;
    return _enqueue(userId, () => _storage.delete(key: storageKeyFor(userId)));
  }

  Future<void> _enqueue(String userId, Future<void> Function() operation) {
    final previous = _queues[userId] ?? Future<void>.value();
    final next = previous.catchError((_) {}).then((_) => operation());
    _queues[userId] = next;
    return next.whenComplete(() {
      if (identical(_queues[userId], next)) _queues.remove(userId);
    });
  }
}
