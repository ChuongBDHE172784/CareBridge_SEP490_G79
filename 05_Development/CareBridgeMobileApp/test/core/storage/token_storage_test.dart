import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/storage/token_storage.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    FlutterSecureStorage.setMockInitialValues({});
  });

  test(
    'account switch deletes only the previous account onboarding draft',
    () async {
      FlutterSecureStorage.setMockInitialValues({
        'cb_user_id': 'account-a',
        'cb_journey_onboarding_draft_account-a': '{"goal":"A"}',
        'cb_journey_onboarding_draft_account-b': '{"goal":"B"}',
      });
      final storage = SecureTokenStorage();

      await storage.save(
        accessToken: 'access-b',
        refreshToken: 'refresh-b',
        userId: 'account-b',
        role: 'MOTHER',
      );

      const secure = FlutterSecureStorage();
      expect(
        await secure.read(key: 'cb_journey_onboarding_draft_account-a'),
        isNull,
      );
      expect(
        await secure.read(key: 'cb_journey_onboarding_draft_account-b'),
        isNotNull,
      );
    },
  );

  test('logout deletes the current account onboarding draft', () async {
    FlutterSecureStorage.setMockInitialValues({
      'cb_user_id': 'account-a',
      'cb_access_token': 'access-a',
      'cb_refresh_token': 'refresh-a',
      'cb_role': 'MOTHER',
      'cb_journey_onboarding_draft_account-a': '{"goal":"A"}',
    });

    await SecureTokenStorage().clear();

    const secure = FlutterSecureStorage();
    expect(await secure.read(key: 'cb_user_id'), isNull);
    expect(
      await secure.read(key: 'cb_journey_onboarding_draft_account-a'),
      isNull,
    );
  });
}
