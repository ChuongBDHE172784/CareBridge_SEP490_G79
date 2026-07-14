import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class BabyProfileSelectionStorage {
  static const _lastOpenedBabyProfileIdKey = 'last_opened_baby_profile_id';

  final FlutterSecureStorage _storage;

  BabyProfileSelectionStorage({FlutterSecureStorage? storage})
    : _storage =
          storage ??
          const FlutterSecureStorage(
            aOptions: AndroidOptions(encryptedSharedPreferences: true),
          );

  Future<String?> readLastOpenedBabyProfileId() {
    return _storage.read(key: _lastOpenedBabyProfileIdKey);
  }

  Future<void> saveLastOpenedBabyProfileId(String babyProfileId) {
    return _storage.write(
      key: _lastOpenedBabyProfileIdKey,
      value: babyProfileId,
    );
  }
}
