import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/features/aiTriage/models/triage_continuation.dart';
import 'package:untitled/features/aiTriage/services/triage_continuation_restore_coordinator.dart';
import 'package:untitled/features/aiTriage/services/triage_continuation_store.dart';
import 'package:untitled/features/auth/screens/auth_landing_screen.dart';
import 'package:untitled/features/baby/models/baby_model.dart';
import 'package:untitled/features/baby/screens/baby_profile_detail_screen.dart';
import 'package:untitled/features/home/screens/mother_home_screen.dart';
import 'package:untitled/features/journey/models/journey_model.dart';
import 'package:untitled/features/journey/screens/mother_journey_screen.dart';
import 'package:untitled/features/journey/services/journey_service.dart';

class _MemoryContinuationStore implements TriageContinuationStore {
  final Map<String, PendingTriageContinuation> values = {};
  final Map<String, int> generations = {};
  Object? readError;
  Object? invalidateError;
  int invalidateFailuresRemaining = 0;

  @override
  int generationFor(String userId) => generations[userId] ?? 0;

  @override
  Future<PendingTriageContinuation?> read(String userId) async {
    if (readError != null) throw readError!;
    return values[userId];
  }

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
    if (invalidateFailuresRemaining > 0) {
      invalidateFailuresRemaining--;
      generations[userId] = generationFor(userId) + 1;
      throw StateError('secure storage delete failed');
    }
    if (invalidateError != null) {
      generations[userId] = generationFor(userId) + 1;
      throw invalidateError!;
    }
    generations[userId] = generationFor(userId) + 1;
    values.remove(userId);
  }
}

class _FakeContinuationGateway implements TriageContinuationGateway {
  final Map<String, TriageContinuationResolution> resolutions = {};
  final Map<String, Completer<TriageContinuationResolution>> pending = {};
  final Map<String, TriageContinuationFailure> failures = {};
  final Map<String, Object> genericFailures = {};
  int resolveCalls = 0;
  int acknowledgeCalls = 0;
  int acknowledgeFailuresRemaining = 0;

  @override
  Future<TriageContinuationResolution> resolve(String token) async {
    resolveCalls++;
    final failure = failures[token];
    if (failure != null) throw failure;
    final genericFailure = genericFailures[token];
    if (genericFailure != null) throw genericFailure;
    final wait = pending[token];
    if (wait != null) return wait.future;
    return resolutions[token]!;
  }

  @override
  Future<void> acknowledge(String token) async {
    acknowledgeCalls++;
    if (acknowledgeFailuresRemaining > 0) {
      acknowledgeFailuresRemaining--;
      throw StateError('offline');
    }
  }
}

class _PagedJourneyService extends JourneyService {
  _PagedJourneyService(this.journeyId);

  final String journeyId;
  final List<int> requestedPages = [];
  bool failTimeline = false;

  @override
  Future<JourneyDashboard> getDashboard() async => JourneyDashboard(
    journeyId: journeyId,
    journeyType: 'PREGNANCY',
    status: 'ACTIVE_PREGNANCY',
    pregnancyWeek: 20,
  );

  @override
  Future<JourneyTimelinePage> getTimeline(
    String requestedJourneyId, {
    int page = 0,
    int size = 20,
  }) async {
    if (failTimeline) throw StateError('timeline offline');
    requestedPages.add(page);
    return JourneyTimelinePage(
      items: [
        JourneyTimelineItem(
          itemType: 'SAFETY_OUTCOME',
          itemId: 'timeline-$page',
          occurredAt: DateTime.utc(2026, 7, 22, 12 - page),
          recordedAt: DateTime.utc(2026, 7, 22, 12 - page),
          riskLevel: page == 0 ? 'GREEN' : 'YELLOW',
          stage: 'PREGNANCY',
        ),
      ],
      page: page,
      size: size,
      totalElements: 2,
      totalPages: 2,
    );
  }

  @override
  Future<List<JourneyTransition>> getHistory(String journeyId) async => [];
}

PendingTriageContinuation _pending(String token) => PendingTriageContinuation(
  token: token,
  intakeSessionId: '40000000-0000-0000-0000-000000000001',
  expiresAt: DateTime.utc(2026, 7, 29, 12),
);

TriageContinuationResolution _resolution({
  required String token,
  required String risk,
  required String stage,
  required TriageOriginDashboard origin,
  required String referenceId,
}) => TriageContinuationResolution(
  token: token,
  intakeSessionId: '40000000-0000-0000-0000-000000000001',
  status: 'COMPLETED',
  riskLevel: risk,
  stage: stage,
  originDashboard: origin,
  originReferenceId: referenceId,
  originAction: origin == TriageOriginDashboard.motherJourney
      ? TriageOriginAction.returnToMotherJourney
      : TriageOriginAction.returnToBabyProfile,
);

void main() {
  group('Story 6.7 restart-safe continuation coordinator', () {
    late _MemoryContinuationStore store;
    late _FakeContinuationGateway gateway;

    setUp(() {
      store = _MemoryContinuationStore();
      gateway = _FakeContinuationGateway();
    });

    for (final risk in const ['GREEN', 'YELLOW']) {
      test(
        '$risk resolves to exact Mother Journey and confirms recording',
        () async {
          const token = 'mother-token';
          const journeyId = '10000000-0000-0000-0000-000000000002';
          store.values['account-a'] = _pending(token);
          gateway.resolutions[token] = _resolution(
            token: token,
            risk: risk,
            stage: 'PREGNANCY',
            origin: TriageOriginDashboard.motherJourney,
            referenceId: journeyId,
          );
          final coordinator = TriageContinuationRestoreCoordinator(
            store: store,
            gateway: gateway,
          );

          final decision = await coordinator.restoreForUser('account-a');

          expect(
            decision.destination,
            TriageContinuationDestination.motherJourney,
          );
          expect(decision.originReferenceId, journeyId);
          expect(decision.showRecordedConfirmation, isTrue);
          expect(decision.confirmationUsesRiskColorOnly, isFalse);
        },
      );
    }

    test('YELLOW resolves to the exact linked baby profile', () async {
      const token = 'baby-token';
      const babyId = '20000000-0000-0000-0000-000000000004';
      store.values['account-a'] = _pending(token);
      gateway.resolutions[token] = _resolution(
        token: token,
        risk: 'YELLOW',
        stage: 'INFANT',
        origin: TriageOriginDashboard.babyProfile,
        referenceId: babyId,
      );
      final coordinator = TriageContinuationRestoreCoordinator(
        store: store,
        gateway: gateway,
      );

      final decision = await coordinator.restoreForUser('account-a');

      expect(decision.destination, TriageContinuationDestination.babyProfile);
      expect(decision.originReferenceId, babyId);
    });

    test(
      'RED restart loads authoritative emergency and never opens a second one',
      () async {
        const token = 'red-token';
        store.values['account-a'] = _pending(token);
        gateway.resolutions[token] = _resolution(
          token: token,
          risk: 'RED',
          stage: 'POSTPARTUM',
          origin: TriageOriginDashboard.motherJourney,
          referenceId: '10000000-0000-0000-0000-000000000003',
        );
        var activeGets = 0;
        var emergencyPosts = 0;
        final coordinator = TriageContinuationRestoreCoordinator(
          store: store,
          gateway: gateway,
          loadAuthoritativeEmergency: () async {
            activeGets++;
            return '50000000-0000-0000-0000-000000000001';
          },
          openEmergency: () async {
            emergencyPosts++;
            return 'should-not-open';
          },
        );

        final decision = await coordinator.restoreForUser('account-a');

        expect(decision.destination, TriageContinuationDestination.emergency);
        expect(activeGets, 1);
        expect(emergencyPosts, 0);
      },
    );

    test(
      'acknowledge retries harmlessly and clears only after success',
      () async {
        const token = 'ack-token';
        store.values['account-a'] = _pending(token);
        gateway.resolutions[token] = _resolution(
          token: token,
          risk: 'GREEN',
          stage: 'PRECONCEPTION',
          origin: TriageOriginDashboard.motherJourney,
          referenceId: '10000000-0000-0000-0000-000000000001',
        );
        gateway.acknowledgeFailuresRemaining = 1;
        final coordinator = TriageContinuationRestoreCoordinator(
          store: store,
          gateway: gateway,
        );
        final decision = await coordinator.restoreForUser('account-a');

        expect(
          await coordinator.acknowledgeAfterDestinationRendered(
            userId: 'account-a',
            decision: decision,
          ),
          isFalse,
        );
        expect(store.values['account-a'], isNotNull);
        expect(
          await coordinator.acknowledgeAfterDestinationRendered(
            userId: 'account-a',
            decision: decision,
          ),
          isTrue,
        );
        expect(gateway.acknowledgeCalls, 2);
        expect(store.values['account-a'], isNull);
      },
    );

    test(
      'foreign or unknown token clears local state without exposing an origin',
      () async {
        const token = 'foreign-token';
        store.values['account-a'] = _pending(token);
        gateway.failures[token] = const TriageContinuationFailure.notFound(
          code: 'TRIAGE-014',
        );
        final coordinator = TriageContinuationRestoreCoordinator(
          store: store,
          gateway: gateway,
        );

        final decision = await coordinator.restoreForUser('account-a');

        expect(
          decision.destination,
          TriageContinuationDestination.safeDashboard,
        );
        expect(decision.originReferenceId, isNull);
        expect(store.values['account-a'], isNull);
      },
    );

    test(
      'stale consent or origin falls back safely without origin data',
      () async {
        const token = 'stale-token';
        store.values['account-a'] = _pending(token);
        gateway.failures[token] = const TriageContinuationFailure.conflict(
          code: 'TRIAGE-015',
        );
        final coordinator = TriageContinuationRestoreCoordinator(
          store: store,
          gateway: gateway,
        );

        final decision = await coordinator.restoreForUser('account-a');

        expect(
          decision.destination,
          TriageContinuationDestination.safeDashboard,
        );
        expect(decision.originReferenceId, isNull);
        expect(decision.isRecoverable, isTrue);
        expect(
          store.values['account-a'],
          isNull,
          reason: 'TRIAGE-015 must retire stale local continuation state',
        );
      },
    );

    test(
      'secure-store read failure is retryable and does not escape',
      () async {
        store.readError = StateError('secure storage unavailable');
        final coordinator = TriageContinuationRestoreCoordinator(
          store: store,
          gateway: gateway,
        );

        final decision = await coordinator.restoreForUser('account-a');

        expect(decision.destination, TriageContinuationDestination.none);
        expect(decision.requiresRetry, isTrue);
        expect(gateway.resolveCalls, 0);
      },
    );

    test('failed invalidation remains retryable and keeps token', () async {
      const token = 'stale-token-with-store-failure';
      store.values['account-a'] = _pending(token);
      store.invalidateError = StateError('secure storage unavailable');
      gateway.failures[token] = const TriageContinuationFailure.conflict(
        code: 'TRIAGE-015',
      );
      final coordinator = TriageContinuationRestoreCoordinator(
        store: store,
        gateway: gateway,
      );

      final decision = await coordinator.restoreForUser('account-a');

      expect(decision.destination, TriageContinuationDestination.none);
      expect(decision.requiresRetry, isTrue);
      expect(decision.continuationToken, token);
      expect(store.values['account-a']?.token, token);
    });

    test('generic resolve failure keeps token for an in-place retry', () async {
      const token = 'offline-token';
      store.values['account-a'] = _pending(token);
      gateway.genericFailures[token] = StateError('offline');
      final coordinator = TriageContinuationRestoreCoordinator(
        store: store,
        gateway: gateway,
      );

      final decision = await coordinator.restoreForUser('account-a');

      expect(decision.destination, TriageContinuationDestination.none);
      expect(decision.requiresRetry, isTrue);
      expect(decision.continuationToken, token);
      expect(store.values['account-a']?.token, token);
    });

    test(
      'late response from the previous account cannot navigate or restore state',
      () async {
        const token = 'late-account-a-token';
        final completer = Completer<TriageContinuationResolution>();
        store.values['account-a'] = _pending(token);
        gateway.pending[token] = completer;
        final coordinator = TriageContinuationRestoreCoordinator(
          store: store,
          gateway: gateway,
        );

        final lateDecision = coordinator.restoreForUser('account-a');
        await coordinator.onAccountSwitch(
          previousUserId: 'account-a',
          nextUserId: 'account-b',
        );
        completer.complete(
          _resolution(
            token: token,
            risk: 'GREEN',
            stage: 'PREGNANCY',
            origin: TriageOriginDashboard.motherJourney,
            referenceId: '10000000-0000-0000-0000-000000000002',
          ),
        );

        final decision = await lateDecision;
        expect(decision.destination, TriageContinuationDestination.none);
        expect(store.values['account-a'], isNull);
        expect(store.values['account-b'], isNull);
      },
    );

    test('resolver descriptor fails closed on a dashboard/action mismatch', () {
      expect(
        () => TriageContinuationResolution.fromJson({
          'token': 'opaque-token',
          'intakeSessionId': '40000000-0000-0000-0000-000000000001',
          'status': 'COMPLETED',
          'riskLevel': 'GREEN',
          'stage': 'PREGNANCY',
          'originDashboard': 'MOTHER_JOURNEY',
          'originReferenceId': '10000000-0000-0000-0000-000000000001',
          'originAction': 'RETURN_TO_BABY_PROFILE',
        }),
        throwsFormatException,
      );
    });
  });

  testWidgets(
    'authenticated app restart resolves a pending continuation before dashboard routing',
    (tester) async {
      FlutterSecureStorage.setMockInitialValues({});
      await AuthState.instance.init();
      await AuthState.instance.setTokens(
        accessToken: 'test-access-token',
        refreshToken: 'test-refresh-token',
        userId: 'account-a',
        role: 'MOTHER',
      );
      addTearDown(AuthState.instance.clear);

      const token = 'restart-baby-token';
      const babyId = '20000000-0000-0000-0000-000000000004';
      final store = _MemoryContinuationStore()
        ..values['account-a'] = _pending(token);
      final gateway = _FakeContinuationGateway()
        ..resolutions[token] = _resolution(
          token: token,
          risk: 'YELLOW',
          stage: 'INFANT',
          origin: TriageOriginDashboard.babyProfile,
          referenceId: babyId,
        );
      final coordinator = TriageContinuationRestoreCoordinator(
        store: store,
        gateway: gateway,
      );
      final router = GoRouter(
        initialLocation: '/auth-landing',
        routes: [
          GoRoute(
            path: '/auth-landing',
            builder: (_, _) =>
                AuthLandingScreen(continuationCoordinator: coordinator),
          ),
          GoRoute(
            path: '/babies/detail/:id',
            builder: (_, state) => BabyProfileDetailScreen(
              babyId: state.pathParameters['id']!,
              loadData: false,
              loadCareCollectionsData: false,
              initialProfile: BabyProfile(
                id: state.pathParameters['id']!,
                nickname: 'Bé An',
                birthDate: DateTime(2026, 1, 1),
                gender: BabyGender.unknown,
                isActive: true,
              ),
              continuationArrival: state.extra as TriageContinuationArrival?,
            ),
          ),
          GoRoute(
            path: '/mother-home',
            builder: (_, _) =>
                const Text('mother-home', textDirection: TextDirection.ltr),
          ),
        ],
      );
      addTearDown(router.dispose);

      await tester.pumpWidget(MaterialApp.router(routerConfig: router));
      await tester.pumpAndSettle();

      expect(
        find.byKey(const Key('baby-triage-recorded-confirmation')),
        findsOneWidget,
      );
      expect(gateway.resolveCalls, 1);
      expect(gateway.acknowledgeCalls, 1);
      expect(store.values['account-a'], isNull);
    },
  );

  testWidgets(
    'auth landing keeps unresolved continuation and offers retry in place',
    (tester) async {
      FlutterSecureStorage.setMockInitialValues({});
      await AuthState.instance.init();
      await AuthState.instance.setTokens(
        accessToken: 'test-access-token',
        refreshToken: 'test-refresh-token',
        userId: 'account-a',
        role: 'MOTHER',
      );
      addTearDown(AuthState.instance.clear);
      const token = 'restart-offline-token';
      final store = _MemoryContinuationStore()
        ..values['account-a'] = _pending(token);
      final gateway = _FakeContinuationGateway()
        ..genericFailures[token] = StateError('offline');
      final coordinator = TriageContinuationRestoreCoordinator(
        store: store,
        gateway: gateway,
      );
      var reachedHome = false;
      final router = GoRouter(
        initialLocation: '/auth-landing',
        routes: [
          GoRoute(
            path: '/auth-landing',
            builder: (_, _) =>
                AuthLandingScreen(continuationCoordinator: coordinator),
          ),
          GoRoute(
            path: '/mother-home',
            builder: (_, _) {
              reachedHome = true;
              return const SizedBox.shrink();
            },
          ),
        ],
      );
      addTearDown(router.dispose);

      await tester.pumpWidget(MaterialApp.router(routerConfig: router));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('auth-landing-retry')), findsOneWidget);
      expect(reachedHome, isFalse);
      expect(store.values['account-a']?.token, token);
    },
  );

  testWidgets(
    'Mother destination loads every timeline page then confirms and acknowledges',
    (tester) async {
      const token = 'mother-arrival-token';
      const journeyId = '10000000-0000-0000-0000-000000000002';
      final store = _MemoryContinuationStore()
        ..values['account-a'] = _pending(token);
      final gateway = _FakeContinuationGateway()
        ..resolutions[token] = _resolution(
          token: token,
          risk: 'GREEN',
          stage: 'PREGNANCY',
          origin: TriageOriginDashboard.motherJourney,
          referenceId: journeyId,
        );
      final coordinator = TriageContinuationRestoreCoordinator(
        store: store,
        gateway: gateway,
      );
      final decision = await coordinator.restoreForUser('account-a');
      final journeyService = _PagedJourneyService(journeyId);

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: MotherJourneyScreen(
              journeyService: journeyService,
              loadSupportingData: false,
              continuationArrival: TriageContinuationArrival(
                userId: 'account-a',
                decision: decision,
                coordinator: coordinator,
              ),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(journeyService.requestedPages, [0, 1]);
      expect(
        find.byKey(const Key('mother-triage-recorded-confirmation')),
        findsOneWidget,
      );
      expect(gateway.acknowledgeCalls, 1);
      expect(store.values['account-a'], isNull);

      journeyService.failTimeline = true;
      JourneyService.dashboardRevision.value++;
      await tester.pumpAndSettle();

      await tester.scrollUntilVisible(
        find.byKey(const Key('journey-timeline-safety-timeline-1')),
        500,
        scrollable: find.byType(Scrollable).first,
      );
      expect(
        find.byKey(const Key('journey-timeline-safety-timeline-1')),
        findsOneWidget,
      );
      expect(find.byKey(const Key('journey-timeline-warning')), findsOneWidget);
    },
  );

  testWidgets(
    'TRIAGE-015 routes home with an accessible inline recoverable notice',
    (tester) async {
      FlutterSecureStorage.setMockInitialValues({});
      await AuthState.instance.init();
      await AuthState.instance.setTokens(
        accessToken: 'test-access-token',
        refreshToken: 'test-refresh-token',
        userId: 'account-a',
        role: 'MOTHER',
      );
      addTearDown(AuthState.instance.clear);
      const token = 'stale-origin-token';
      final store = _MemoryContinuationStore()
        ..values['account-a'] = _pending(token);
      final gateway = _FakeContinuationGateway()
        ..failures[token] = const TriageContinuationFailure.conflict(
          code: 'TRIAGE-015',
        );
      final coordinator = TriageContinuationRestoreCoordinator(
        store: store,
        gateway: gateway,
      );
      Object? routedNotice;
      final router = GoRouter(
        initialLocation: '/auth-landing',
        routes: [
          GoRoute(
            path: '/auth-landing',
            builder: (_, _) =>
                AuthLandingScreen(continuationCoordinator: coordinator),
          ),
          GoRoute(
            path: '/mother-home',
            builder: (_, state) {
              routedNotice = state.extra;
              final dynamic notice = state.extra;
              final message = notice == null
                  ? ''
                  : notice.message?.toString() ?? '';
              return Scaffold(
                body: Semantics(
                  key: const Key('triage-continuation-recovery-notice'),
                  liveRegion: true,
                  label: message,
                  child: Text(message),
                ),
              );
            },
          ),
        ],
      );
      addTearDown(router.dispose);

      await tester.pumpWidget(MaterialApp.router(routerConfig: router));
      await tester.pumpAndSettle();

      expect(routedNotice, isNotNull);
      expect(
        find.byKey(const Key('triage-continuation-recovery-notice')),
        findsOneWidget,
      );
      final semantics = tester.getSemantics(
        find.byKey(const Key('triage-continuation-recovery-notice')),
      );
      expect(semantics.label, contains('không còn khả dụng'));
    },
  );

  testWidgets('Mother home renders the recoverable notice inline', (
    tester,
  ) async {
    final dynamic home = Function.apply(MotherHomeScreen.new, const [], {
      #recoveryNotice:
          'Điểm quay lại trước đây không còn khả dụng. Bạn đã được đưa về Trang chủ an toàn.',
    });

    await tester.pumpWidget(MaterialApp(home: home as Widget));
    await tester.pump();

    expect(
      find.byKey(const Key('triage-continuation-recovery-notice')),
      findsOneWidget,
    );
    expect(
      tester
          .getSemantics(
            find.byKey(const Key('triage-continuation-recovery-notice')),
          )
          .label,
      contains('không còn khả dụng'),
    );
  });

  testWidgets(
    'Mother destination exposes accessible acknowledgement retry after failure',
    (tester) async {
      const token = 'mother-retry-token';
      const journeyId = '10000000-0000-0000-0000-000000000031';
      final store = _MemoryContinuationStore()
        ..values['account-a'] = _pending(token);
      final gateway = _FakeContinuationGateway()
        ..acknowledgeFailuresRemaining = 1
        ..resolutions[token] = _resolution(
          token: token,
          risk: 'GREEN',
          stage: 'PREGNANCY',
          origin: TriageOriginDashboard.motherJourney,
          referenceId: journeyId,
        );
      final coordinator = TriageContinuationRestoreCoordinator(
        store: store,
        gateway: gateway,
      );
      final decision = await coordinator.restoreForUser('account-a');

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: MotherJourneyScreen(
              journeyService: _PagedJourneyService(journeyId),
              loadSupportingData: false,
              continuationArrival: TriageContinuationArrival(
                userId: 'account-a',
                decision: decision,
                coordinator: coordinator,
              ),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      final retry = find.byKey(
        const Key('mother-triage-acknowledgement-retry'),
      );
      expect(retry, findsOneWidget);
      expect(store.values['account-a'], isNotNull);
      expect(tester.getSemantics(retry).label, contains('Thử lại'));

      await tester.tap(retry);
      await tester.pumpAndSettle();

      expect(gateway.acknowledgeCalls, 2);
      expect(store.values['account-a'], isNull);
      expect(retry, findsNothing);
    },
  );

  testWidgets(
    'Mother acknowledgement retry cleans local state without resending ACK',
    (tester) async {
      const token = 'mother-local-cleanup-token';
      const journeyId = '10000000-0000-0000-0000-000000000041';
      final store = _MemoryContinuationStore()
        ..values['account-a'] = _pending(token);
      final gateway = _FakeContinuationGateway()
        ..resolutions[token] = _resolution(
          token: token,
          risk: 'GREEN',
          stage: 'PREGNANCY',
          origin: TriageOriginDashboard.motherJourney,
          referenceId: journeyId,
        );
      final coordinator = TriageContinuationRestoreCoordinator(
        store: store,
        gateway: gateway,
      );
      final decision = await coordinator.restoreForUser('account-a');
      store.invalidateFailuresRemaining = 1;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: MotherJourneyScreen(
              journeyService: _PagedJourneyService(journeyId),
              loadSupportingData: false,
              continuationArrival: TriageContinuationArrival(
                userId: 'account-a',
                decision: decision,
                coordinator: coordinator,
              ),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      final retry = find.byKey(
        const Key('mother-triage-acknowledgement-retry'),
      );
      expect(retry, findsOneWidget);
      expect(gateway.acknowledgeCalls, 1);
      expect(store.values['account-a']?.token, token);

      await tester.tap(retry);
      await tester.pumpAndSettle();

      expect(gateway.acknowledgeCalls, 1);
      expect(store.values['account-a'], isNull);
      expect(retry, findsNothing);
    },
  );

  testWidgets(
    'Baby destination exposes accessible acknowledgement retry after failure',
    (tester) async {
      const token = 'baby-retry-token';
      const babyId = '20000000-0000-0000-0000-000000000032';
      final store = _MemoryContinuationStore()
        ..values['account-a'] = _pending(token);
      final gateway = _FakeContinuationGateway()
        ..acknowledgeFailuresRemaining = 1
        ..resolutions[token] = _resolution(
          token: token,
          risk: 'YELLOW',
          stage: 'INFANT',
          origin: TriageOriginDashboard.babyProfile,
          referenceId: babyId,
        );
      final coordinator = TriageContinuationRestoreCoordinator(
        store: store,
        gateway: gateway,
      );
      final decision = await coordinator.restoreForUser('account-a');

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: BabyProfileDetailScreen(
              babyId: babyId,
              loadData: false,
              loadCareCollectionsData: false,
              initialProfile: BabyProfile(
                id: babyId,
                nickname: 'Bé An',
                birthDate: DateTime(2026, 1, 1),
                gender: BabyGender.unknown,
                isActive: true,
              ),
              continuationArrival: TriageContinuationArrival(
                userId: 'account-a',
                decision: decision,
                coordinator: coordinator,
              ),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      final retry = find.byKey(const Key('baby-triage-acknowledgement-retry'));
      expect(retry, findsOneWidget);
      expect(store.values['account-a'], isNotNull);
      expect(tester.getSemantics(retry).label, contains('Thử lại'));

      await tester.tap(retry);
      await tester.pumpAndSettle();

      expect(gateway.acknowledgeCalls, 2);
      expect(store.values['account-a'], isNull);
      expect(retry, findsNothing);
    },
  );
}
