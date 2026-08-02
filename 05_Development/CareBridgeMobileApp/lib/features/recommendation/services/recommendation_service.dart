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
  String get _draftKey => '$_storagePrefix${_schema}_${_userId ?? 'anonymous'}';

  Future<RecommendationProfileResponse> getProfile() async {
    final expectedUser = _userId;
    final response = await apiGet('/api/v1/recommendations/profile');
    _ensureCurrent(expectedUser);
    return RecommendationProfileResponse.fromJson(
      (response['data'] as Map).cast<String, dynamic>(),
    );
  }

  Future<bool> accountHasDateOfBirth() async {
    final expectedUser = _userId;
    final response = await apiGet('/api/v1/profile');
    _ensureCurrent(expectedUser);
    final data = response['data'];
    if (data is! Map) return false;
    final value = data['dateOfBirth'];
    return value is String && value.trim().isNotEmpty;
  }

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
    if (expectedUser != null) await clearDraftFor(expectedUser);
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
    if (_userId == null) return;
    await storage.write(
      key: _draftKey,
      value: jsonEncode({
        'userId': _userId,
        'schemaVersion': _schema,
        'profile': profile,
      }),
    );
  }

  Future<void> clearDraft() => storage.delete(key: _draftKey);

  Future<void> clearDraftFor(String userId) =>
      storage.delete(key: '$_storagePrefix${_schema}_$userId');

  void _ensureCurrent(String? expectedUser) {
    if (expectedUser == null || expectedUser != AuthState.instance.userId) {
      throw StateError(
        'Authenticated account changed during recommendation request',
      );
    }
  }
}
