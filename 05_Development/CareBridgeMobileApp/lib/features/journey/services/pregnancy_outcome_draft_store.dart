import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../models/journey_model.dart';

class PregnancyOutcomeDraft {
  const PregnancyOutcomeDraft({
    required this.submissionId,
    required this.outcome,
    this.outcomeDate,
    this.effectiveAt,
    this.correction = false,
  });

  final String submissionId;
  final PregnancyOutcome? outcome;
  final DateTime? outcomeDate;
  final DateTime? effectiveAt;
  final bool correction;

  Map<String, dynamic> toJson() => {
    'submissionId': submissionId,
    if (outcome != null) 'outcomeType': outcome!.apiValue,
    if (outcomeDate != null)
      'outcomeDate': outcomeDate!.toIso8601String().split('T').first,
    if (effectiveAt != null)
      'effectiveAt': effectiveAt!.toUtc().toIso8601String(),
    'correction': correction,
  };

  factory PregnancyOutcomeDraft.fromJson(Map<String, dynamic> json) {
    final rawOutcome = json['outcomeType'] as String?;
    return PregnancyOutcomeDraft(
      submissionId: json['submissionId'] as String,
      outcome: rawOutcome == null
          ? null
          : PregnancyOutcome.values.firstWhere(
              (value) => value.apiValue == rawOutcome,
            ),
      outcomeDate: DateTime.tryParse(json['outcomeDate'] as String? ?? ''),
      effectiveAt: DateTime.tryParse(json['effectiveAt'] as String? ?? ''),
      correction: json['correction'] as bool? ?? false,
    );
  }
}

abstract interface class PregnancyOutcomeDraftStore {
  Future<PregnancyOutcomeDraft?> read(String accountId, String journeyId);
  Future<void> write(
    String accountId,
    String journeyId,
    PregnancyOutcomeDraft draft,
  );
  Future<void> clear(String accountId, String journeyId);
  Future<void> clearForAccount(String accountId);
}

class SecurePregnancyOutcomeDraftStore implements PregnancyOutcomeDraftStore {
  const SecurePregnancyOutcomeDraftStore();

  static const instance = SecurePregnancyOutcomeDraftStore();
  static const _storage = FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );
  static const _indexPrefix = 'cb_pregnancy_outcome_draft_index';
  static const _draftPrefix = 'cb_pregnancy_outcome_draft';

  String _draftKey(String accountId, String journeyId) =>
      '${_draftPrefix}_${accountId}_$journeyId';
  String _indexKey(String accountId) => '${_indexPrefix}_$accountId';

  @override
  Future<PregnancyOutcomeDraft?> read(
    String accountId,
    String journeyId,
  ) async {
    final raw = await _storage.read(key: _draftKey(accountId, journeyId));
    if (raw == null) return null;
    try {
      final json = jsonDecode(raw) as Map<String, dynamic>;
      if (json['accountId'] != accountId || json['journeyId'] != journeyId) {
        await clear(accountId, journeyId);
        return null;
      }
      return PregnancyOutcomeDraft.fromJson(json);
    } catch (_) {
      await clear(accountId, journeyId);
      return null;
    }
  }

  @override
  Future<void> write(
    String accountId,
    String journeyId,
    PregnancyOutcomeDraft draft,
  ) async {
    final key = _draftKey(accountId, journeyId);
    await _storage.write(
      key: key,
      value: jsonEncode({
        'accountId': accountId,
        'journeyId': journeyId,
        ...draft.toJson(),
      }),
    );
    final indexKey = _indexKey(accountId);
    final existing = await _readIndex(indexKey);
    await _storage.write(key: indexKey, value: jsonEncode({...existing, key}));
  }

  @override
  Future<void> clear(String accountId, String journeyId) async {
    final key = _draftKey(accountId, journeyId);
    await _storage.delete(key: key);
    final indexKey = _indexKey(accountId);
    final existing = await _readIndex(indexKey)
      ..remove(key);
    if (existing.isEmpty) {
      await _storage.delete(key: indexKey);
    } else {
      await _storage.write(key: indexKey, value: jsonEncode(existing));
    }
  }

  @override
  Future<void> clearForAccount(String accountId) async {
    final indexKey = _indexKey(accountId);
    for (final key in await _readIndex(indexKey)) {
      await _storage.delete(key: key);
    }
    await _storage.delete(key: indexKey);
  }

  Future<Set<String>> _readIndex(String key) async {
    try {
      final raw = await _storage.read(key: key);
      if (raw == null) return <String>{};
      return (jsonDecode(raw) as List<dynamic>).cast<String>().toSet();
    } catch (_) {
      return <String>{};
    }
  }
}
