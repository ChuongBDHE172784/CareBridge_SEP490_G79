import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../../../core/storage/token_storage.dart';
import '../models/baby_model.dart';

class BabyCreateIntent {
  const BabyCreateIntent({
    required this.submissionId,
    required this.nickname,
    required this.birthDate,
    required this.gender,
    this.birthWeightKg,
    this.birthLengthCm,
  });

  final String submissionId;
  final String nickname;
  final String birthDate;
  final BabyGender gender;
  final double? birthWeightKg;
  final double? birthLengthCm;

  Map<String, dynamic> toJson() => {
    'submissionId': submissionId,
    'nickname': nickname,
    'birthDate': birthDate,
    'gender': gender.toApiValue(),
    if (birthWeightKg != null) 'birthWeightKg': birthWeightKg,
    if (birthLengthCm != null) 'birthLengthCm': birthLengthCm,
  };

  factory BabyCreateIntent.fromJson(Map<String, dynamic> json) =>
      BabyCreateIntent(
        submissionId: json['submissionId'] as String,
        nickname: json['nickname'] as String,
        birthDate: json['birthDate'] as String,
        gender: BabyGenderExtension.fromApi(json['gender'] as String?),
        birthWeightKg: (json['birthWeightKg'] as num?)?.toDouble(),
        birthLengthCm: (json['birthLengthCm'] as num?)?.toDouble(),
      );

  bool hasSamePayload(BabyCreateIntent other) =>
      nickname == other.nickname &&
      birthDate == other.birthDate &&
      gender == other.gender &&
      birthWeightKg == other.birthWeightKg &&
      birthLengthCm == other.birthLengthCm;

  @override
  bool operator ==(Object other) =>
      other is BabyCreateIntent &&
      submissionId == other.submissionId &&
      nickname == other.nickname &&
      birthDate == other.birthDate &&
      gender == other.gender &&
      birthWeightKg == other.birthWeightKg &&
      birthLengthCm == other.birthLengthCm;

  @override
  int get hashCode => Object.hash(
    submissionId,
    nickname,
    birthDate,
    gender,
    birthWeightKg,
    birthLengthCm,
  );
}

abstract interface class BabyCreateIntentStorage {
  Future<String?> read(String key);
  Future<void> write(String key, String value);
  Future<void> delete(String key);
  Future<Map<String, String>> readAll();
}

class SecureBabyCreateIntentStorage implements BabyCreateIntentStorage {
  const SecureBabyCreateIntentStorage();

  static const _storage = FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );

  @override
  Future<String?> read(String key) => _storage.read(key: key);

  @override
  Future<void> write(String key, String value) =>
      _storage.write(key: key, value: value);

  @override
  Future<void> delete(String key) => _storage.delete(key: key);

  @override
  Future<Map<String, String>> readAll() => _storage.readAll();
}

class InMemoryBabyCreateIntentStorage implements BabyCreateIntentStorage {
  final Map<String, String> _values = {};

  @override
  Future<String?> read(String key) async => _values[key];

  @override
  Future<void> write(String key, String value) async => _values[key] = value;

  @override
  Future<void> delete(String key) async => _values.remove(key);

  @override
  Future<Map<String, String>> readAll() async => Map.of(_values);
}

class BabyCreateIntentStore {
  BabyCreateIntentStore({BabyCreateIntentStorage? storage})
    : _storage = storage ?? const SecureBabyCreateIntentStorage();

  static const _prefix = 'cb_baby_create_intent';
  final BabyCreateIntentStorage _storage;

  int generationFor(String accountId) =>
      PostpartumDraftStorageCoordinator.generationFor(accountId);

  String _key(String accountId, String journeyId) =>
      '${_prefix}_${accountId}_$journeyId';

  Future<BabyCreateIntent?> read(String accountId, String journeyId) async {
    final key = _key(accountId, journeyId);
    final raw = _storage is SecureBabyCreateIntentStorage
        ? await PostpartumDraftStorageCoordinator.read(accountId, key)
        : await _storage.read(key);
    if (raw == null) return null;
    try {
      final json = jsonDecode(raw) as Map<String, dynamic>;
      if (json['accountId'] != accountId || json['journeyId'] != journeyId) {
        await _storage.delete(key);
        return null;
      }
      return BabyCreateIntent.fromJson(json);
    } catch (_) {
      await _storage.delete(key);
      return null;
    }
  }

  Future<void> write(
    String accountId,
    String journeyId,
    BabyCreateIntent intent, {
    int? expectedGeneration,
  }) {
    final key = _key(accountId, journeyId);
    final value = jsonEncode({
      'accountId': accountId,
      'journeyId': journeyId,
      ...intent.toJson(),
    });
    if (_storage is SecureBabyCreateIntentStorage) {
      return PostpartumDraftStorageCoordinator.write(
        userId: accountId,
        key: key,
        value: value,
        generation: expectedGeneration ?? generationFor(accountId),
      );
    }
    return _storage.write(key, value);
  }

  Future<void> clear(String accountId, String journeyId) {
    final key = _key(accountId, journeyId);
    return _storage is SecureBabyCreateIntentStorage
        ? PostpartumDraftStorageCoordinator.delete(accountId, key)
        : _storage.delete(key);
  }

  Future<void> clearForAccount(String accountId) async {
    final prefix = '${_prefix}_${accountId}_';
    if (_storage is SecureBabyCreateIntentStorage) {
      await PostpartumDraftStorageCoordinator.deletePrefix(accountId, prefix);
      return;
    }
    final keys = (await _storage.readAll()).keys.where(
      (key) => key.startsWith(prefix),
    );
    await Future.wait(keys.map(_storage.delete));
  }
}
