import 'dart:async';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/core/storage/token_storage.dart';
import 'package:untitled/features/aiTriage/models/triage_continuation.dart';
import 'package:untitled/features/aiTriage/services/triage_continuation_restore_coordinator.dart';
import 'package:untitled/features/aiTriage/services/triage_continuation_store.dart';
import 'package:untitled/features/aiTriage/services/triage_service.dart';
import 'package:untitled/features/auth/screens/logout_confirmation_screen.dart';

class _MemoryContinuationStore implements TriageContinuationStore {
  final Map<String, PendingTriageContinuation> values = {};
  final Map<String, int> generations = {};

  @override
  int generationFor(String userId) => generations[userId] ?? 0;

  @override
  Future<PendingTriageContinuation?> read(String userId) async =>
      values[userId];

  @override
  Future<void> save({
    required String userId,
    required PendingTriageContinuation continuation,
    required int generation,
  }) async {
    if (generation == generationFor(userId)) values[userId] = continuation;
  }

  @override
  Future<void> invalidateUser(String userId) async {
    generations[userId] = generationFor(userId) + 1;
    values.remove(userId);
  }
}

class _FailingContinuationStore extends _MemoryContinuationStore {
  @override
  Future<void> save({
    required String userId,
    required PendingTriageContinuation continuation,
    required int generation,
  }) async {
    throw StateError('synthetic secure-storage failure');
  }
}

class _BlockedContinuationStore extends _MemoryContinuationStore {
  final saveStarted = Completer<void>();
  final releaseSave = Completer<void>();

  @override
  Future<void> save({
    required String userId,
    required PendingTriageContinuation continuation,
    required int generation,
  }) {
    if (!saveStarted.isCompleted) saveStarted.complete();
    return releaseSave.future;
  }
}

class _NeverResolveContinuationGateway implements TriageContinuationGateway {
  int resolveCalls = 0;

  @override
  Future<void> acknowledge(String token) async {}

  @override
  Future<TriageContinuationResolution> resolve(String token) async {
    resolveCalls++;
    throw StateError('ASK_MORE must not be restart-restored');
  }
}

Map<String, dynamic> _completedResultPayload({
  required String sessionId,
  required String token,
  String stage = 'PREGNANCY',
}) => {
  'data': {
    'sessionId': sessionId,
    'status': 'COMPLETED',
    'stage': stage,
    'riskLevel': 'GREEN',
    'continuationToken': token,
    'continuationExpiresAt': '2026-07-29T12:00:00Z',
  },
};

Map<String, dynamic> _consentStatusPayload({String status = 'ACCEPTED'}) => {
  'data': {
    'status': status,
    'currentVersion': 'v1',
    'acceptedVersion': status == 'ACCEPTED' ? 'v1' : null,
    'disclaimerText': 'disclaimer',
  },
};

Map<String, dynamic> _completedFlowPayload({
  required String sessionId,
  required String token,
  String stage = 'PREGNANCY',
}) => {
  'data': {
    'status': 'TRIAGE_COMPLETE',
    'intakeSessionId': sessionId,
    'stage': stage,
    'mergedIntake': {'stage': stage},
    'round': 2,
    'triageResult': {'stage': stage, 'riskLevel': 'GREEN'},
    'continuationToken': token,
    'continuationExpiresAt': '2026-07-29T12:00:00Z',
  },
};

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    FlutterSecureStorage.setMockInitialValues({});
  });

  group('Story 6.7 account-scoped encrypted continuation persistence', () {
    test(
      'persists minimum continuation state for only the owning account',
      () async {
        final dynamic storage = SecureTokenStorage();
        final expiresAt = DateTime.utc(2026, 7, 29, 12);

        await storage.saveTriageContinuation(
          userId: 'account-a',
          token: '30000000-0000-4000-8000-000000000001',
          intakeSessionId: '40000000-0000-0000-0000-000000000001',
          expiresAt: expiresAt,
        );

        final dynamic own = await storage.loadTriageContinuation('account-a');
        final dynamic foreign = await storage.loadTriageContinuation(
          'account-b',
        );
        expect(own.token, '30000000-0000-4000-8000-000000000001');
        expect(own.intakeSessionId, '40000000-0000-0000-0000-000000000001');
        expect(own.expiresAt, expiresAt);
        expect(foreign, isNull);
        expect(
          await const FlutterSecureStorage().read(
            key: 'cb_triage_continuation_account-a',
          ),
          isNotNull,
        );
      },
    );

    test('account switch removes the previous continuation only', () async {
      FlutterSecureStorage.setMockInitialValues({
        'cb_user_id': 'account-a',
        'cb_triage_continuation_account-a':
            '{"token":"token-a","intakeSessionId":"session-a"}',
        'cb_triage_continuation_account-b':
            '{"token":"token-b","intakeSessionId":"session-b"}',
      });

      await SecureTokenStorage().save(
        accessToken: 'access-b',
        refreshToken: 'refresh-b',
        userId: 'account-b',
        role: 'MOTHER',
      );

      const secure = FlutterSecureStorage();
      expect(
        await secure.read(key: 'cb_triage_continuation_account-a'),
        isNull,
      );
      expect(
        await secure.read(key: 'cb_triage_continuation_account-b'),
        isNotNull,
      );
    });

    test('logout removes the current continuation', () async {
      FlutterSecureStorage.setMockInitialValues({
        'cb_user_id': 'account-a',
        'cb_access_token': 'access-a',
        'cb_refresh_token': 'refresh-a',
        'cb_role': 'MOTHER',
        'cb_triage_continuation_account-a':
            '{"token":"token-a","intakeSessionId":"session-a"}',
      });

      await SecureTokenStorage().clear();

      expect(
        await const FlutterSecureStorage().read(
          key: 'cb_triage_continuation_account-a',
        ),
        isNull,
      );
    });

    test('queued write cannot resurrect invalidated account state', () async {
      final dynamic storage = SecureTokenStorage();
      final int generation = storage.triageContinuationGenerationFor(
        'account-a',
      );
      final writeEntered = Completer<void>();
      final releaseWrite = Completer<void>();

      Future<void> controlledLateWrite() async {
        writeEntered.complete();
        await releaseWrite.future;
        await storage.saveTriageContinuation(
          userId: 'account-a',
          token: 'late-token-a',
          intakeSessionId: 'late-session-a',
          expiresAt: DateTime.utc(2026, 7, 29, 12),
          generation: generation,
        );
      }

      final lateWrite = controlledLateWrite();
      await writeEntered.future;
      final Future<void> cleanup = storage.invalidateTriageContinuation(
        'account-a',
      );
      releaseWrite.complete();
      await Future.wait([lateWrite, cleanup]);

      expect(await storage.loadTriageContinuation('account-a'), isNull);
    });

    test(
      'late response from account A cannot persist its token under account B',
      () async {
        await AuthState.instance.clear();
        await AuthState.instance.setTokens(
          accessToken: 'access-a',
          refreshToken: 'refresh-a',
          userId: 'account-a',
          role: 'MOTHER',
        );
        addTearDown(AuthState.instance.clear);
        final response = Completer<dynamic>();
        final requestStarted = Completer<void>();
        final store = _MemoryContinuationStore();
        final service = TriageService(
          continuationStore: store,
          getRequest: (_) {
            requestStarted.complete();
            return response.future;
          },
        );

        final result = service.getResult('session-a');
        await requestStarted.future;
        await AuthState.instance.setTokens(
          accessToken: 'access-b',
          refreshToken: 'refresh-b',
          userId: 'account-b',
          role: 'MOTHER',
        );
        response.complete({
          'data': {
            'sessionId': 'session-a',
            'status': 'COMPLETED',
            'stage': 'PREGNANCY',
            'riskLevel': 'GREEN',
            'continuationToken': 'token-owned-by-a',
            'continuationExpiresAt': '2026-07-29T12:00:00Z',
          },
        });

        await expectLater(result, throwsA(isA<StateError>()));
        expect(store.values['account-a'], isNull);
        expect(store.values['account-b'], isNull);
      },
    );

    test(
      'same-account stale getResult cannot replace the newer continuation',
      () async {
        await AuthState.instance.clear();
        await AuthState.instance.setTokens(
          accessToken: 'access-a',
          refreshToken: 'refresh-a',
          userId: 'account-a',
          role: 'MOTHER',
        );
        addTearDown(AuthState.instance.clear);
        final oldResponse = Completer<dynamic>();
        final newResponse = Completer<dynamic>();
        final store = _MemoryContinuationStore();
        final service = TriageService(
          continuationStore: store,
          getRequest: (path) => path.endsWith('session-old')
              ? oldResponse.future
              : newResponse.future,
        );

        final oldRequest = service.getResult('session-old');
        final newRequest = service.getResult('session-new');
        newResponse.complete(
          _completedResultPayload(sessionId: 'session-new', token: 'new-token'),
        );
        await newRequest;
        oldResponse.complete(
          _completedResultPayload(sessionId: 'session-old', token: 'old-token'),
        );

        await expectLater(oldRequest, throwsA(isA<StateError>()));
        expect(store.values['account-a']?.token, 'new-token');
        expect(store.values['account-a']?.intakeSessionId, 'session-new');
      },
    );

    test(
      'same-account stale start cannot replace the newer continuation',
      () async {
        await AuthState.instance.clear();
        await AuthState.instance.setTokens(
          accessToken: 'access-a',
          refreshToken: 'refresh-a',
          userId: 'account-a',
          role: 'MOTHER',
        );
        addTearDown(AuthState.instance.clear);
        final responses = <Completer<dynamic>>[
          Completer<dynamic>(),
          Completer<dynamic>(),
        ];
        var call = 0;
        final store = _MemoryContinuationStore();
        final service = TriageService(
          continuationStore: store,
          postRequest: (_, _) => responses[call++].future,
        );

        final oldRequest = service.startConversation(
          initialText: 'synthetic-old',
          currentIntake: {'stage': 'PREGNANCY'},
        );
        final newRequest = service.startConversation(
          initialText: 'synthetic-new',
          currentIntake: {'stage': 'PREGNANCY'},
        );
        responses[1].complete(
          _completedFlowPayload(sessionId: 'session-new', token: 'new-token'),
        );
        await newRequest;
        responses[0].complete(
          _completedFlowPayload(sessionId: 'session-old', token: 'old-token'),
        );

        await expectLater(oldRequest, throwsA(isA<StateError>()));
        expect(store.values['account-a']?.token, 'new-token');
      },
    );

    test(
      'same-account stale continue cannot replace the newer continuation',
      () async {
        await AuthState.instance.clear();
        await AuthState.instance.setTokens(
          accessToken: 'access-a',
          refreshToken: 'refresh-a',
          userId: 'account-a',
          role: 'MOTHER',
        );
        addTearDown(AuthState.instance.clear);
        final responses = <Completer<dynamic>>[
          Completer<dynamic>(),
          Completer<dynamic>(),
        ];
        var call = 0;
        final store = _MemoryContinuationStore();
        final service = TriageService(
          continuationStore: store,
          postRequest: (_, _) => responses[call++].future,
        );

        final oldRequest = service.continueConversation(
          intakeSessionId: 'session-shared',
          currentIntake: {'stage': 'PREGNANCY'},
          newAnswers: const {'answer': 'old'},
          round: 1,
        );
        final newRequest = service.continueConversation(
          intakeSessionId: 'session-shared',
          currentIntake: {'stage': 'PREGNANCY'},
          newAnswers: const {'answer': 'new'},
          round: 2,
        );
        responses[1].complete(
          _completedFlowPayload(
            sessionId: 'session-shared',
            token: 'new-token',
          ),
        );
        await newRequest;
        responses[0].complete(
          _completedFlowPayload(
            sessionId: 'session-shared',
            token: 'old-token',
          ),
        );

        await expectLater(oldRequest, throwsA(isA<StateError>()));
        expect(store.values['account-a']?.token, 'new-token');
      },
    );

    // Regression: the staleness sequence used to be tracked per-userId only,
    // shared across every TriageService operation. An unrelated background
    // getConsentStatus/listHistory/getResult call racing a start/continue
    // mutation would bump that shared counter and make the mutation's own
    // (successful) response look "stale", surfacing a false
    // "Không thể gửi triệu chứng" error to the user even though the server
    // had already processed the request. Fixed by scoping the sequence per
    // "userId::operation" instead of per-userId.
    test(
      'unrelated concurrent getConsentStatus does not stale-fail startConversation',
      () async {
        await AuthState.instance.clear();
        await AuthState.instance.setTokens(
          accessToken: 'access-a',
          refreshToken: 'refresh-a',
          userId: 'account-a',
          role: 'MOTHER',
        );
        addTearDown(AuthState.instance.clear);
        final startResponse = Completer<dynamic>();
        final consentResponse = Completer<dynamic>();
        final store = _MemoryContinuationStore();
        final service = TriageService(
          continuationStore: store,
          postRequest: (_, _) => startResponse.future,
          getRequest: (_) => consentResponse.future,
        );

        final startRequest = service.startConversation(
          initialText: 'symptom text',
          currentIntake: {'stage': 'PREGNANCY'},
        );
        final consentRequest = service.getConsentStatus();

        // The unrelated background consent check resolves first...
        consentResponse.complete(_consentStatusPayload());
        await consentRequest;
        // ...then the actual mutation resolves. It must NOT be treated as
        // stale just because a different-purpose call finished in between.
        startResponse.complete(
          _completedFlowPayload(sessionId: 'session-a', token: 'start-token'),
        );

        await expectLater(startRequest, completes);
        expect(store.values['account-a']?.token, 'start-token');
      },
    );

    test(
      'ASK_MORE token is not persisted or restored after process death',
      () async {
        await AuthState.instance.clear();
        await AuthState.instance.setTokens(
          accessToken: 'access-a',
          refreshToken: 'refresh-a',
          userId: 'account-a',
          role: 'MOTHER',
        );
        addTearDown(AuthState.instance.clear);
        final store = _MemoryContinuationStore();
        final gateway = _NeverResolveContinuationGateway();
        final service = TriageService(
          continuationStore: store,
          postRequest: (_, _) async => {
            'data': {
              'status': 'ASK_MORE',
              'intakeSessionId': 'session-ask-more',
              'stage': 'INFANT',
              'mergedIntake': {'stage': 'INFANT'},
              'questions': const [],
              'round': 1,
              'continuationToken': 'nonterminal-token',
              'continuationExpiresAt': '2026-07-29T12:00:00Z',
            },
          },
        );

        await service.startConversation(
          initialText: 'synthetic',
          currentIntake: {'stage': 'INFANT'},
        );
        final coordinator = TriageContinuationRestoreCoordinator(
          store: store,
          gateway: gateway,
        );
        final decision = await coordinator.restoreForUser('account-a');

        expect(store.values['account-a'], isNull);
        expect(decision.destination, TriageContinuationDestination.none);
        expect(gateway.resolveCalls, 0);
      },
    );

    test('invalid response cannot persist a continuation token', () async {
      await AuthState.instance.clear();
      await AuthState.instance.setTokens(
        accessToken: 'access-a',
        refreshToken: 'refresh-a',
        userId: 'account-a',
        role: 'MOTHER',
      );
      addTearDown(AuthState.instance.clear);
      final store = _MemoryContinuationStore();
      final service = TriageService(
        continuationStore: store,
        getRequest: (_) async => {
          'data': {
            'sessionId': '',
            'status': 'COMPLETED',
            'stage': 'PREGNANCY',
            'riskLevel': 'GREEN',
            'continuationToken': 'must-not-persist',
            'continuationExpiresAt': '2026-07-29T12:00:00Z',
          },
        },
      );

      await expectLater(service.getResult('invalid'), throwsFormatException);
      expect(store.values, isEmpty);
    });

    test(
      'secure-storage failure cannot hide an already validated terminal result',
      () async {
        await AuthState.instance.clear();
        await AuthState.instance.setTokens(
          accessToken: 'access-a',
          refreshToken: 'refresh-a',
          userId: 'account-a',
          role: 'MOTHER',
        );
        addTearDown(AuthState.instance.clear);
        final persistenceFailure = Completer<Object>();
        final service = TriageService(
          continuationStore: _FailingContinuationStore(),
          getRequest: (_) async => _completedResultPayload(
            sessionId: 'validated-session',
            token: 'validated-token',
          ),
          onContinuationPersistenceFailure: (error, _) {
            if (!persistenceFailure.isCompleted) {
              persistenceFailure.complete(error);
            }
          },
        );

        final result = await service.getResult('validated-session');

        expect(result.sessionId, 'validated-session');
        expect(result.status, 'COMPLETED');
        expect(await persistenceFailure.future, isA<StateError>());
      },
    );

    test('continuation persistence never delays a terminal response', () async {
      await AuthState.instance.clear();
      await AuthState.instance.setTokens(
        accessToken: 'access-a',
        refreshToken: 'refresh-a',
        userId: 'account-a',
        role: 'MOTHER',
      );
      addTearDown(AuthState.instance.clear);
      final store = _BlockedContinuationStore();
      final service = TriageService(
        continuationStore: store,
        getRequest: (_) async => _completedResultPayload(
          sessionId: 'emergency-session',
          token: 'emergency-token',
        ),
      );

      final result = await service
          .getResult('emergency-session')
          .timeout(const Duration(milliseconds: 250));

      expect(result.sessionId, 'emergency-session');
      await store.saveStarted.future;
      store.releaseSave.complete();
    });

    test(
      'pregnancy draft cleanup failure cannot skip recommendation or auth cleanup',
      () async {
        var recommendationCleared = false;
        var authCleared = false;

        await clearLocalSessionAfterLogout(
          accountId: 'account-a',
          isCapturedSessionCurrent: () => true,
          clearDraft: (_) async => throw StateError('draft storage failed'),
          clearRecommendationDraft: (accountId) async {
            expect(accountId, 'account-a');
            recommendationCleared = true;
          },
          clearAuth: () async {
            authCleared = true;
          },
        );

        expect(recommendationCleared, isTrue);
        expect(authCleared, isTrue);
      },
    );

    test(
      'recommendation draft cleanup failure cannot skip auth cleanup',
      () async {
        var pregnancyDraftCleared = false;
        var authCleared = false;

        await clearLocalSessionAfterLogout(
          accountId: 'account-a',
          isCapturedSessionCurrent: () => true,
          clearDraft: (accountId) async {
            expect(accountId, 'account-a');
            pregnancyDraftCleared = true;
          },
          clearRecommendationDraft: (_) async =>
              throw StateError('recommendation storage failed'),
          clearAuth: () async {
            authCleared = true;
          },
        );

        expect(pregnancyDraftCleared, isTrue);
        expect(authCleared, isTrue);
      },
    );

    test('missing account skips draft cleanup but still clears auth', () async {
      var pregnancyDraftCalls = 0;
      var recommendationDraftCalls = 0;
      var authCleared = false;

      await clearLocalSessionAfterLogout(
        accountId: null,
        isCapturedSessionCurrent: () => true,
        clearDraft: (_) async {
          pregnancyDraftCalls++;
        },
        clearRecommendationDraft: (_) async {
          recommendationDraftCalls++;
        },
        clearAuth: () async {
          authCleared = true;
        },
      );

      expect(pregnancyDraftCalls, 0);
      expect(recommendationDraftCalls, 0);
      expect(authCleared, isTrue);
    });
  });
}
