import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:uuid/uuid.dart';

import '../../../core/auth/auth_state.dart';
import '../../../core/network/api_client.dart';
import '../models/recommendation_model.dart';

class RecommendationService {
  RecommendationService({this.storage = const FlutterSecureStorage()});

  static final ValueNotifier<int> profileChangeRevision = ValueNotifier(0);

  static void notifyProfileChanged() => profileChangeRevision.value++;

  final FlutterSecureStorage storage;
  static const _schema = RecommendationProfileDraft.schemaVersion;
  static const _storagePrefix = 'cb_recommendation_profile_draft_v';

  String? get _userId => AuthState.instance.userId;
  // Draft writes/deletes can originate from different service instances
  // (profile, privacy, logout, Home). Queue them per account globally so a
  // late write cannot overtake a clear issued by another instance.
  static final Map<String, Future<void>> _draftOperationTails = {};

  Future<RecommendationProfileResponse> getProfile() async {
    final expectedUser = _userId;
    final response = await apiGet('/api/v1/recommendations/profile');
    _ensureCurrent(expectedUser);
    return RecommendationProfileResponse.fromJson(
      (response['data'] as Map).cast<String, dynamic>(),
    );
  }

  /// Loads the canonical account DOB used to derive the recommendation age
  /// signal.  The recommendation payload intentionally never contains a DOB
  /// or an age number.
  Future<String?> getDateOfBirth() async {
    final expectedUser = _userId;
    final response = await apiGet('/api/v1/profile');
    _ensureCurrent(expectedUser);
    final data = response['data'];
    if (data is! Map) return null;
    final value = data['dateOfBirth'];
    final trimmed = value is String ? value.trim() : '';
    return trimmed.isEmpty ? null : trimmed;
  }

  /// Named alias kept for injectable screen/test seams.
  Future<String?> loadDateOfBirth() => getDateOfBirth();

  /// Named alias kept for callers that describe the account boundary
  /// explicitly.
  Future<String?> loadAccountDateOfBirth() => getDateOfBirth();

  /// Compatibility helper for older callers that only need a presence check.
  Future<bool> accountHasDateOfBirth() async =>
      (await getDateOfBirth()) != null;

  /// Persists only the date-of-birth field on the canonical account profile.
  /// This is deliberately separate from recommendation PUT so a valid DOB
  /// correction is not rolled back when a later recommendation submission
  /// fails.
  Future<void> updateDateOfBirth(String dateOfBirth) async {
    final expectedUser = _userId;
    await apiPatch('/api/v1/profile', {'dateOfBirth': dateOfBirth});
    _ensureCurrent(expectedUser);
  }

  /// Alias used by the questionnaire's direct DOB editor.
  Future<void> patchDateOfBirth(String dateOfBirth) =>
      updateDateOfBirth(dateOfBirth);

  /// Alias matching the account-profile terminology used by some tests.
  Future<void> saveDateOfBirth(String dateOfBirth) =>
      updateDateOfBirth(dateOfBirth);

  Future<RecommendationProfileResponse> putProfile({
    required Map<String, dynamic> profile,
    String? submissionId,
  }) async {
    final expectedUser = _userId;
    final id = submissionId ?? const Uuid().v4();
    final response = await apiPut('/api/v1/recommendations/profile', {
      'submissionId': id,
      'schemaVersion': _schema,
      'policyVersion': RecommendationProfileDraft.policyVersion,
      'consentAccepted': true,
      'profile': profile,
    });
    _ensureCurrent(expectedUser);
    notifyProfileChanged();
    return RecommendationProfileResponse.fromJson(
      (response['data'] as Map).cast<String, dynamic>(),
    );
  }

  Future<RecommendationProfileResponse> decline() async {
    final expectedUser = _userId;
    final response = await apiPut('/api/v1/recommendations/profile', {
      'submissionId': const Uuid().v4(),
      'schemaVersion': _schema,
      'policyVersion': RecommendationProfileDraft.policyVersion,
      'consentAccepted': false,
    });
    _ensureCurrent(expectedUser);
    if (expectedUser != null) {
      try {
        await clearDraftFor(expectedUser);
      } catch (_) {
        // The decline is already committed on the server. A local draft
        // cleanup failure must not turn that success into a retryable error.
      }
      _ensureCurrent(expectedUser);
    }
    notifyProfileChanged();
    return RecommendationProfileResponse.fromJson(
      (response['data'] as Map).cast<String, dynamic>(),
    );
  }

  Future<RecommendationContentResponse> getContent({int limit = 3}) async {
    final expectedUser = _userId;
    final response = await apiGet(
      '/api/v1/recommendations/content',
      queryParams: {'limit': limit},
    );
    _ensureCurrent(expectedUser);
    return RecommendationContentResponse.fromJson(
      (response['data'] as Map).cast<String, dynamic>(),
    );
  }

  Future<Map<String, dynamic>?> readDraft() async {
    final expectedUser = _userId;
    if (expectedUser == null) return null;
    final key = '$_storagePrefix${_schema}_$expectedUser';
    final raw = await storage.read(key: key);
    _ensureCurrent(expectedUser);
    if (raw == null) return null;
    try {
      final value = jsonDecode(raw);
      if (value is! Map ||
          value['userId'] != expectedUser ||
          value['schemaVersion'] != _schema) {
        await clearDraftFor(expectedUser);
        return null;
      }
      final profile = value['profile'];
      return profile is Map ? profile.cast<String, dynamic>() : null;
    } catch (_) {
      await clearDraftFor(expectedUser);
      return null;
    }
  }

  Future<void> saveDraft(Map<String, dynamic> profile) async {
    final userId = _userId;
    if (userId == null) return;
    final key = '$_storagePrefix${_schema}_$userId';
    final value = jsonEncode({
      'userId': userId,
      'schemaVersion': _schema,
      'profile': profile,
    });
    await _enqueueDraftOperation(userId, () async {
      if (_userId != userId) return;
      await storage.write(key: key, value: value);
    });
  }

  Future<void> clearDraft() {
    final userId = _userId;
    if (userId == null) return Future<void>.value();
    return clearDraftFor(userId);
  }

  Future<void> clearDraftFor(String userId) => _enqueueDraftOperation(
    userId,
    () => storage.delete(key: '$_storagePrefix${_schema}_$userId'),
  );

  Future<void> _enqueueDraftOperation(
    String userId,
    Future<void> Function() operation,
  ) {
    final previous = _draftOperationTails[userId] ?? Future<void>.value();
    final next = previous.then((_) => operation());
    _draftOperationTails[userId] = next.catchError((_) {});
    return next;
  }

  void _ensureCurrent(String? expectedUser) {
    if (expectedUser == null || expectedUser != AuthState.instance.userId) {
      throw StateError(
        'Authenticated account changed during recommendation request',
      );
    }
  }
}
