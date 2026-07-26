import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/features/aiTriage/models/triage_continuation.dart';
import 'package:untitled/features/aiTriage/models/triage_result_model.dart';
import 'package:untitled/features/aiTriage/screens/risk_triage_result_screen.dart';
import 'package:untitled/features/aiTriage/services/triage_continuation_restore_coordinator.dart';
import 'package:untitled/features/aiTriage/services/triage_continuation_store.dart';
import 'package:untitled/features/aiTriage/services/triage_service.dart';
import 'package:untitled/features/emergency/models/emergency_session_model.dart';
import 'package:untitled/features/emergency/services/emergency_service.dart';

const _sessionId = '22222222-2222-2222-2222-222222222222';

TriageResult _result(
  String risk, {
  bool emergency = false,
  String stage = 'INFANT',
  List<TriageCitation> citations = const [],
  String? continuationToken,
}) => TriageResult(
  sessionId: _sessionId,
  stage: stage,
  status: 'COMPLETED',
  riskLevel: risk,
  emergencyActionRequired: emergency,
  summary: 'Ket qua $risk',
  recommendedAction: 'Theo doi huong dan',
  citations: citations,
  disclaimer: 'Khong thay the chan doan y khoa',
  continuationToken: continuationToken,
);

class _RetryContinuationStore implements TriageContinuationStore {
  _RetryContinuationStore(this.pending);

  PendingTriageContinuation? pending;

  @override
  int generationFor(String userId) => 0;

  @override
  Future<PendingTriageContinuation?> read(String userId) async => pending;

  @override
  Future<void> invalidateUser(String userId) async => pending = null;

  @override
  Future<void> save({
    required String userId,
    required PendingTriageContinuation continuation,
    required int generation,
  }) async => pending = continuation;
}

class _OfflineContinuationGateway implements TriageContinuationGateway {
  @override
  Future<void> acknowledge(String token) async {}

  @override
  Future<TriageContinuationResolution> resolve(String token) =>
      Future.error(StateError('offline'));
}

class _StaticResultService extends TriageService {
  _StaticResultService(this.result);
  final TriageResult result;
  int calls = 0;

  @override
  Future<TriageResult> getResult(String sessionId) async {
    calls++;
    return result;
  }
}

class _PendingResultService extends TriageService {
  final completer = Completer<TriageResult>();

  @override
  Future<TriageResult> getResult(String sessionId) => completer.future;
}

class _DelayedEmergencyService extends EmergencyService {
  int activeCalls = 0;
  int openCalls = 0;
  final completer = Completer<EmergencySession?>();

  @override
  Future<EmergencySession?> getActive() {
    activeCalls++;
    return completer.future;
  }

  @override
  Future<EmergencySession> openFlow({
    required String triggerSource,
    double? latitude,
    double? longitude,
  }) {
    openCalls++;
    return Future.error(StateError('RED handoff must not POST a session'));
  }
}

class _FailingEmergencyService extends EmergencyService {
  int activeCalls = 0;
  int openCalls = 0;

  @override
  Future<EmergencySession?> getActive() {
    activeCalls++;
    return Future.error(Exception('RAW_RISK_EMERGENCY_FAILURE'));
  }

  @override
  Future<EmergencySession> openFlow({
    required String triggerSource,
    double? latitude,
    double? longitude,
  }) {
    openCalls++;
    return Future.error(Exception('RAW_RISK_EMERGENCY_FAILURE'));
  }
}

Future<GoRouter> _pumpScreen(
  WidgetTester tester, {
  required TriageService triage,
  EmergencyService? emergency,
  Future<bool> Function()? postpartumEmergencyLauncher,
  TriageContinuationRestoreCoordinator? continuationCoordinator,
}) async {
  final router = GoRouter(
    routes: [
      GoRoute(
        path: '/',
        builder: (_, _) => RiskTriageResultScreen(
          sessionId: _sessionId,
          triageService: triage,
          emergencyService: emergency,
          postpartumEmergencyLauncher: postpartumEmergencyLauncher,
          continuationCoordinator: continuationCoordinator,
        ),
      ),
      GoRoute(
        path: '/emergency/map',
        builder: (_, state) {
          final session = state.extra as EmergencySession?;
          return Scaffold(
            body: Text(
              'Emergency map ${session?.sessionId ?? 'manual'} '
              '${state.uri.queryParameters['mode']} '
              '${state.uri.queryParameters['stage']}',
            ),
          );
        },
      ),
    ],
  );
  await tester.pumpWidget(MaterialApp.router(routerConfig: router));
  await tester.pumpAndSettle();
  return router;
}

Future<Finder> _showEmergencyCta(WidgetTester tester) async {
  final cta = find.byKey(const Key('risk-result-emergency-cta'));
  expect(cta, findsOneWidget);
  await tester.scrollUntilVisible(cta, 300);
  return cta;
}

void main() {
  setUp(() async {
    FlutterSecureStorage.setMockInitialValues({});
    await AuthState.instance.clear();
    await AuthState.instance.setTokens(
      accessToken: 'test-access',
      refreshToken: 'test-refresh',
      userId: 'account-a',
      role: 'MOTHER',
    );
  });

  tearDown(() async => AuthState.instance.clear());

  testWidgets(
    'RED loads, ignores duplicate tap, and navigates to emergency map',
    (tester) async {
      final triage = _StaticResultService(_result('RED', emergency: true));
      final emergency = _DelayedEmergencyService();
      await _pumpScreen(tester, triage: triage, emergency: emergency);

      final cta = await _showEmergencyCta(tester);
      await tester.tap(cta);
      await tester.tap(cta);
      expect(triage.calls, 1);
      expect(emergency.activeCalls, 1);
      expect(emergency.openCalls, 0);

      emergency.completer.complete(
        const EmergencySession(
          sessionId: 'e2',
          userId: 'u2',
          status: 'ACTIVE',
          triggerSource: 'AI_TRIAGE',
        ),
      );
      await tester.pumpAndSettle();
      expect(find.text('Emergency map e2 triage INFANT'), findsOneWidget);
    },
  );

  testWidgets(
    'failed emergency API stays on result and hides raw or false success',
    (tester) async {
      final emergency = _FailingEmergencyService();
      await _pumpScreen(
        tester,
        triage: _StaticResultService(_result('RED', emergency: true)),
        emergency: emergency,
      );

      final cta = await _showEmergencyCta(tester);
      await tester.tap(cta);
      await tester.pumpAndSettle();

      expect(emergency.activeCalls, 1);
      expect(emergency.openCalls, 0);
      expect(find.text('Emergency map'), findsNothing);
      expect(find.textContaining('RAW_RISK_EMERGENCY_FAILURE'), findsNothing);
      expect(
        find.byKey(const Key('risk-result-emergency-status')),
        findsOneWidget,
      );
      expect(find.textContaining('thành công'), findsNothing);
    },
  );

  for (final risk in ['GREEN', 'YELLOW']) {
    testWidgets('$risk loads without RED emergency CTA', (tester) async {
      await _pumpScreen(tester, triage: _StaticResultService(_result(risk)));

      expect(find.text('Ket qua $risk'), findsOneWidget);
      expect(find.byKey(const Key('risk-result-emergency-cta')), findsNothing);
      expect(
        find.byKey(const Key('risk-result-clinic-cta')),
        risk == 'GREEN' ? findsOneWidget : findsNothing,
      );
      expect(
        find.byKey(const Key('risk-result-yellow-expert-handoff-cta')),
        findsNothing,
      );
    });
  }

  testWidgets('POSTPARTUM RED uses maternal copy and the backend session', (
    tester,
  ) async {
    final emergency = _DelayedEmergencyService();
    await _pumpScreen(
      tester,
      triage: _StaticResultService(
        _result('RED', emergency: true, stage: 'POSTPARTUM'),
      ),
      emergency: emergency,
    );

    expect(find.textContaining('bé'), findsNothing);
    expect(
      find.byKey(const Key('risk-result-postpartum-call-115')),
      findsOneWidget,
    );
    final cta = await _showEmergencyCta(tester);
    await tester.tap(cta);
    await tester.pump();
    expect(emergency.activeCalls, 1);
    expect(emergency.openCalls, 0);

    emergency.completer.complete(
      const EmergencySession(
        sessionId: 'postpartum-e2',
        userId: 'mother',
        status: 'ACTIVE',
        triggerSource: 'AI_TRIAGE',
      ),
    );
    await tester.pumpAndSettle();
    expect(
      find.text('Emergency map postpartum-e2 triage POSTPARTUM'),
      findsOneWidget,
    );
  });

  testWidgets('POSTPARTUM RED keeps explicit 115 guidance when handoff fails', (
    tester,
  ) async {
    final emergency = _FailingEmergencyService();
    await _pumpScreen(
      tester,
      triage: _StaticResultService(
        _result('RED', emergency: true, stage: 'POSTPARTUM'),
      ),
      emergency: emergency,
      postpartumEmergencyLauncher: () async =>
          throw StateError('dialer unavailable'),
    );

    final cta = await _showEmergencyCta(tester);
    await tester.tap(cta);
    await tester.pumpAndSettle();

    expect(emergency.activeCalls, 1);
    expect(emergency.openCalls, 0);
    expect(find.textContaining('Phiên có thể'), findsOneWidget);
    final call115 = find.byKey(const Key('risk-result-postpartum-call-115'));
    await tester.scrollUntilVisible(call115, 300);
    await tester.tap(call115);
    await tester.pumpAndSettle();
    expect(find.textContaining('tự gọi 115'), findsOneWidget);
    expect(find.textContaining('Phiên có thể'), findsOneWidget);
    expect(find.textContaining('RAW_RISK_EMERGENCY_FAILURE'), findsNothing);
  });

  testWidgets('POSTPARTUM 115 action does not wait for backend handoff', (
    tester,
  ) async {
    final emergency = _DelayedEmergencyService();
    var dialerCalls = 0;
    await _pumpScreen(
      tester,
      triage: _StaticResultService(
        _result('RED', emergency: true, stage: 'POSTPARTUM'),
      ),
      emergency: emergency,
      postpartumEmergencyLauncher: () async {
        dialerCalls++;
        return true;
      },
    );

    final call115 = find.byKey(const Key('risk-result-postpartum-call-115'));
    await tester.scrollUntilVisible(call115, 300);
    await tester.tap(call115);
    await tester.pump();

    expect(dialerCalls, 1);
    expect(emergency.activeCalls, 0);
    expect(emergency.openCalls, 0);
  });

  testWidgets('pending getResult is ignored after result screen is disposed', (
    tester,
  ) async {
    final triage = _PendingResultService();
    final router = GoRouter(
      routes: [
        GoRoute(
          path: '/',
          builder: (_, _) => RiskTriageResultScreen(
            sessionId: _sessionId,
            triageService: triage,
          ),
        ),
      ],
    );
    await tester.pumpWidget(MaterialApp.router(routerConfig: router));
    await tester.pump();
    await tester.pumpWidget(const MaterialApp(home: SizedBox.shrink()));

    triage.completer.complete(_result('GREEN'));
    await tester.pump();

    expect(tester.takeException(), isNull);
    router.dispose();
  });

  testWidgets(
    'pending account-A result is not rendered after switching to account B',
    (tester) async {
      FlutterSecureStorage.setMockInitialValues({});
      await AuthState.instance.clear();
      await AuthState.instance.setTokens(
        accessToken: 'access-a',
        refreshToken: 'refresh-a',
        userId: 'account-a',
        role: 'MOTHER',
      );
      addTearDown(AuthState.instance.clear);
      final triage = _PendingResultService();
      await tester.pumpWidget(
        MaterialApp(
          home: RiskTriageResultScreen(
            sessionId: _sessionId,
            triageService: triage,
          ),
        ),
      );
      await tester.pump();

      await AuthState.instance.setTokens(
        accessToken: 'access-b',
        refreshToken: 'refresh-b',
        userId: 'account-b',
        role: 'MOTHER',
      );
      triage.completer.complete(_result('GREEN'));
      await tester.pumpAndSettle();

      expect(find.text('Ket qua GREEN'), findsNothing);
      expect(find.byKey(const Key('risk-result-retry')), findsOneWidget);
    },
  );

  testWidgets(
    'offline continuation resolve keeps result visible and retry enabled',
    (tester) async {
      FlutterSecureStorage.setMockInitialValues({});
      await AuthState.instance.clear();
      await AuthState.instance.setTokens(
        accessToken: 'access-a',
        refreshToken: 'refresh-a',
        userId: 'account-a',
        role: 'MOTHER',
      );
      addTearDown(AuthState.instance.clear);
      const token = 'opaque-continuation';
      final store = _RetryContinuationStore(
        PendingTriageContinuation(
          token: token,
          intakeSessionId: _sessionId,
          expiresAt: DateTime.utc(2026, 7, 30),
        ),
      );
      final coordinator = TriageContinuationRestoreCoordinator(
        store: store,
        gateway: _OfflineContinuationGateway(),
      );
      final router = await _pumpScreen(
        tester,
        triage: _StaticResultService(
          _result('GREEN', continuationToken: token),
        ),
        continuationCoordinator: coordinator,
      );
      addTearDown(router.dispose);

      final returnAction = find.byKey(
        const Key('risk-result-return-to-origin'),
      );
      await tester.ensureVisible(returnAction);
      await tester.tap(returnAction);
      await tester.pumpAndSettle();

      expect(find.text('Ket qua GREEN'), findsOneWidget);
      expect(find.textContaining('thử lại'), findsWidgets);
      final retry = tester.widget<FilledButton>(
        find.byKey(const Key('risk-result-return-to-origin')),
      );
      expect(retry.onPressed, isNotNull);
      expect(store.pending?.token, token);
    },
  );

  testWidgets(
    'late authoritative emergency from previous account is discarded',
    (tester) async {
      FlutterSecureStorage.setMockInitialValues({});
      await AuthState.instance.clear();
      await AuthState.instance.setTokens(
        accessToken: 'access-a',
        refreshToken: 'refresh-a',
        userId: 'account-a',
        role: 'MOTHER',
      );
      addTearDown(AuthState.instance.clear);
      final emergency = _DelayedEmergencyService();
      final router = await _pumpScreen(
        tester,
        triage: _StaticResultService(_result('RED', emergency: true)),
        emergency: emergency,
      );
      addTearDown(router.dispose);

      final emergencyAction = find.byKey(
        const Key('risk-result-emergency-cta'),
      );
      await tester.ensureVisible(emergencyAction);
      await tester.tap(emergencyAction);
      await tester.pump();
      await AuthState.instance.setTokens(
        accessToken: 'access-b',
        refreshToken: 'refresh-b',
        userId: 'account-b',
        role: 'MOTHER',
      );
      emergency.completer.complete(
        const EmergencySession(
          sessionId: 'account-a-emergency',
          userId: 'account-a',
          status: 'ACTIVE',
          triggerSource: 'AI_TRIAGE',
        ),
      );
      await tester.pumpAndSettle();

      expect(find.textContaining('Emergency map'), findsNothing);
      expect(find.text('Ket qua RED'), findsOneWidget);
      expect(emergency.openCalls, 0);
      final action = tester.widget<ElevatedButton>(
        find.byKey(const Key('risk-result-emergency-cta')),
      );
      expect(action.onPressed, isNotNull);
    },
  );

  testWidgets('pending citation is labeled and non-whitelist URL is disabled', (
    tester,
  ) async {
    const pending = TriageCitation(
      id: 'pending-risk',
      title: 'Realtime source',
      source: 'Official source',
      url: 'https://who.int.attacker.example/evidence',
      excerpt: 'Evidence',
      retrievedAt: '2026-07-13',
      sourceStatus: 'PENDING_REVIEW',
    );
    await _pumpScreen(
      tester,
      triage: _StaticResultService(
        _result('YELLOW', citations: const [pending]),
      ),
    );

    expect(
      find.byKey(const Key('risk-citation-pending-pending-risk-0')),
      findsOneWidget,
    );
    final link = tester.widget<InkWell>(
      find.byKey(const Key('risk-citation-link-pending-risk-0')),
    );
    expect(link.onTap, isNull);
  });

  testWidgets('duplicate result citation URLs use unique link keys', (
    tester,
  ) async {
    const first = TriageCitation(
      title: 'WHO source one',
      source: 'WHO',
      domain: 'who.int',
      url: 'https://who.int/evidence',
      excerpt: 'Evidence one',
      retrievedAt: '2026-07-13',
    );
    const second = TriageCitation(
      title: 'WHO source two',
      source: 'WHO',
      domain: 'who.int',
      url: 'https://who.int/evidence',
      excerpt: 'Evidence two',
      retrievedAt: '2026-07-13',
    );
    await _pumpScreen(
      tester,
      triage: _StaticResultService(
        _result('YELLOW', citations: const [first, second]),
      ),
    );

    final firstLink = tester.widget<InkWell>(
      find.byKey(const Key('risk-citation-link-https://who.int/evidence-0')),
    );
    final secondLink = tester.widget<InkWell>(
      find.byKey(const Key('risk-citation-link-https://who.int/evidence-1')),
    );
    expect(firstLink.onTap, isNotNull);
    expect(secondLink.onTap, isNotNull);
    expect(tester.takeException(), isNull);
  });
}
