import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/features/aiTriage/models/triage_result_model.dart';
import 'package:untitled/features/aiTriage/screens/risk_triage_result_screen.dart';
import 'package:untitled/features/aiTriage/services/triage_service.dart';
import 'package:untitled/features/emergency/models/emergency_session_model.dart';
import 'package:untitled/features/emergency/services/emergency_service.dart';

const _sessionId = '22222222-2222-2222-2222-222222222222';

TriageResult _result(
  String risk, {
  bool emergency = false,
  List<TriageCitation> citations = const [],
}) => TriageResult(
  sessionId: _sessionId,
  status: 'COMPLETED',
  riskLevel: risk,
  emergencyActionRequired: emergency,
  summary: 'Ket qua $risk',
  recommendedAction: 'Theo doi huong dan',
  citations: citations,
  disclaimer: 'Khong thay the chan doan y khoa',
);

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
  int calls = 0;
  final completer = Completer<EmergencySession>();

  @override
  Future<EmergencySession> openFlow({
    required String triggerSource,
    double? latitude,
    double? longitude,
  }) {
    calls++;
    return completer.future;
  }
}

class _FailingEmergencyService extends EmergencyService {
  int calls = 0;

  @override
  Future<EmergencySession> openFlow({
    required String triggerSource,
    double? latitude,
    double? longitude,
  }) {
    calls++;
    return Future.error(Exception('RAW_RISK_EMERGENCY_FAILURE'));
  }
}

Future<GoRouter> _pumpScreen(
  WidgetTester tester, {
  required TriageService triage,
  EmergencyService? emergency,
}) async {
  final router = GoRouter(
    routes: [
      GoRoute(
        path: '/',
        builder: (_, _) => RiskTriageResultScreen(
          sessionId: _sessionId,
          triageService: triage,
          emergencyService: emergency,
        ),
      ),
      GoRoute(
        path: '/emergency/map',
        builder: (_, _) => const Scaffold(body: Text('Emergency map')),
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
      expect(emergency.calls, 1);

      emergency.completer.complete(
        const EmergencySession(
          sessionId: 'e2',
          userId: 'u2',
          status: 'ACTIVE',
          triggerSource: 'AI_TRIAGE',
        ),
      );
      await tester.pumpAndSettle();
      expect(find.text('Emergency map'), findsOneWidget);
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

      expect(emergency.calls, 1);
      expect(find.text('Emergency map'), findsNothing);
      expect(find.textContaining('RAW_RISK_EMERGENCY_FAILURE'), findsNothing);
      expect(find.byType(SnackBar), findsOneWidget);
      expect(find.textContaining('thành công'), findsNothing);
    },
  );

  for (final risk in ['GREEN', 'YELLOW']) {
    testWidgets('$risk loads without RED emergency CTA', (tester) async {
      await _pumpScreen(tester, triage: _StaticResultService(_result(risk)));

      expect(find.text('Ket qua $risk'), findsOneWidget);
      expect(find.byKey(const Key('risk-result-emergency-cta')), findsNothing);
      expect(find.byKey(const Key('risk-result-clinic-cta')), findsOneWidget);
    });
  }

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
