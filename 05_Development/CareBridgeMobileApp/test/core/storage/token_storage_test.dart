import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/storage/token_storage.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  String retiredBabyDraftKey(String accountId, String journeyId) =>
      ['cb', 'baby', 'create', 'intent', accountId, journeyId].join('_');

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
        'cb_postpartum_log_draft_account-a_journey-1': '{"pain":"3"}',
        'cb_postpartum_log_draft_account-b_journey-2': '{"pain":"4"}',
        retiredBabyDraftKey('account-a', 'journey-1'): '{"submissionId":"a"}',
        retiredBabyDraftKey('account-b', 'journey-2'): '{"submissionId":"b"}',
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
      expect(
        await secure.read(key: 'cb_postpartum_log_draft_account-a_journey-1'),
        isNull,
      );
      expect(
        await secure.read(key: 'cb_postpartum_log_draft_account-b_journey-2'),
        isNotNull,
      );
      expect(
        await secure.read(key: retiredBabyDraftKey('account-a', 'journey-1')),
        isNull,
      );
      expect(
        await secure.read(key: retiredBabyDraftKey('account-b', 'journey-2')),
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
      'cb_postpartum_log_draft_account-a_journey-1': '{"pain":"3"}',
      retiredBabyDraftKey('account-a', 'journey-1'): '{"submissionId":"a"}',
    });

    await SecureTokenStorage().clear();

    const secure = FlutterSecureStorage();
    expect(await secure.read(key: 'cb_user_id'), isNull);
    expect(
      await secure.read(key: 'cb_journey_onboarding_draft_account-a'),
      isNull,
    );
    expect(
      await secure.read(key: 'cb_postpartum_log_draft_account-a_journey-1'),
      isNull,
    );
    expect(
      await secure.read(key: retiredBabyDraftKey('account-a', 'journey-1')),
      isNull,
    );
  });

  test('startup purges retired baby drafts for the active account', () async {
    FlutterSecureStorage.setMockInitialValues({
      'cb_user_id': 'account-a',
      'cb_access_token': 'access-a',
      'cb_refresh_token': 'refresh-a',
      'cb_role': 'MOTHER',
      'cb_postpartum_log_draft_account-a_journey-1': '{"pain":"3"}',
      retiredBabyDraftKey('account-a', 'journey-1'): '{"submissionId":"a"}',
      retiredBabyDraftKey('account-b', 'journey-2'): '{"submissionId":"b"}',
    });

    final loaded = await SecureTokenStorage().load();

    const secure = FlutterSecureStorage();
    expect(loaded['userId'], 'account-a');
    expect(
      await secure.read(key: retiredBabyDraftKey('account-a', 'journey-1')),
      isNull,
    );
    expect(
      await secure.read(key: retiredBabyDraftKey('account-b', 'journey-2')),
      isNotNull,
    );
    expect(
      await secure.read(key: 'cb_postpartum_log_draft_account-a_journey-1'),
      isNotNull,
    );
  });

  test(
    'partial credential bundle invalidates the captured account continuation',
    () async {
      FlutterSecureStorage.setMockInitialValues({
        'cb_user_id': 'account-a',
        'cb_access_token': 'partial-access',
        'cb_journey_onboarding_draft_account-a': '{"goal":"A"}',
      });
      final storage = SecureTokenStorage();
      await storage.saveTriageContinuation(
        userId: 'account-a',
        token: 'continuation-a',
        intakeSessionId: 'intake-a',
        expiresAt: DateTime.now().add(const Duration(hours: 1)),
      );

      final loaded = await storage.load();

      expect(loaded.values, everyElement(isNull));
      expect(await storage.loadTriageContinuation('account-a'), isNull);
      expect(
        await const FlutterSecureStorage().read(
          key: 'cb_journey_onboarding_draft_account-a',
        ),
        isNull,
      );
    },
  );

  test(
    'account invalidation cannot be undone by a queued draft write',
    () async {
      const key = 'cb_postpartum_log_draft_account-a_journey-late';
      final generation = PostpartumDraftStorageCoordinator.generationFor(
        'account-a',
      );

      final lateWrite = PostpartumDraftStorageCoordinator.write(
        userId: 'account-a',
        key: key,
        value: '{"pain":"9"}',
        generation: generation,
      );
      final cleanup = PostpartumDraftStorageCoordinator.invalidateUser(
        'account-a',
      );
      await Future.wait([lateWrite, cleanup]);

      expect(await const FlutterSecureStorage().read(key: key), isNull);
    },
  );

  test('payload deletion still runs when marker invalidation fails', () async {
    var payloadDeleted = false;
    var accountCleaned = false;

    await expectLater(
      clearCredentialBundleFailClosed(
        expectedUserId: 'account-a',
        invalidateCommitMarker: () async {
          throw StateError('marker delete and fallback failed');
        },
        deleteCredentialPayload: () async {
          payloadDeleted = true;
        },
        clearAccountState: (userId) async {
          expect(userId, 'account-a');
          accountCleaned = true;
        },
      ),
      throwsA(isA<StateError>()),
    );

    expect(payloadDeleted, isTrue);
    expect(accountCleaned, isTrue);
  });

  test('trusted account ID bypasses failed storage lookup', () async {
    var readCalls = 0;
    String? cleanedUserId;

    await clearCredentialBundleFailClosed(
      expectedUserId: 'account-a',
      readStoredUserId: () async {
        readCalls++;
        throw StateError('unavailable');
      },
      invalidateCommitMarker: () async {},
      deleteCredentialPayload: () async {},
      clearAccountState: (userId) async {
        cleanedUserId = userId;
      },
    );

    expect(readCalls, 0);
    expect(cleanedUserId, 'account-a');
  });

  test('lookup failure does not skip credential cleanup', () async {
    var markerInvalidated = false;
    var payloadDeleted = false;

    await expectLater(
      clearCredentialBundleFailClosed(
        readStoredUserId: () async => throw StateError('read failed'),
        invalidateCommitMarker: () async {
          markerInvalidated = true;
        },
        deleteCredentialPayload: () async {
          payloadDeleted = true;
        },
        clearAccountState: (_) async {},
      ),
      throwsA(isA<StateError>()),
    );

    expect(markerInvalidated, isTrue);
    expect(payloadDeleted, isTrue);
  });
}
