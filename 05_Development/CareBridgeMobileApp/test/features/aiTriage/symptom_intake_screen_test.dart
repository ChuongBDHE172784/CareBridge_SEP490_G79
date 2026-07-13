import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/features/aiTriage/models/triage_intake_flow_model.dart';
import 'package:untitled/features/aiTriage/models/triage_result_model.dart';
import 'package:untitled/features/aiTriage/screens/symptom_intake_screen.dart';
import 'package:untitled/features/aiTriage/services/triage_service.dart';
import 'package:untitled/features/emergency/models/emergency_session_model.dart';
import 'package:untitled/features/emergency/services/emergency_service.dart';

const _sessionId = '11111111-1111-1111-1111-111111111111';

IntakeFlowResponse _complete(TriageResult result) => IntakeFlowResponse(
  status: 'TRIAGE_COMPLETE',
  intakeSessionId: _sessionId,
  mergedIntake: const {'persisted': true},
  round: 2,
  triageResult: result,
);

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

class _StaticTriageService extends TriageService {
  _StaticTriageService(this.response);
  final IntakeFlowResponse response;

  @override
  Future<IntakeFlowResponse> startConversation({
    required String initialText,
    required Map<String, dynamic> currentIntake,
  }) async => response;
}

class _AskMoreTriageService extends TriageService {
  String? continuedSessionId;
  int? continuedRound;
  Map<String, dynamic>? continuedIntake;
  Map<String, dynamic>? continuedAnswers;

  @override
  Future<IntakeFlowResponse> startConversation({
    required String initialText,
    required Map<String, dynamic> currentIntake,
  }) async => const IntakeFlowResponse(
    status: 'ASK_MORE',
    intakeSessionId: _sessionId,
    mergedIntake: {'serverState': 'kept'},
    assistantMessage: 'Can them tuoi cua be',
    questions: [
      IntakeQuestion(
        questionKey: 'childAgeMonths',
        text: 'Be bao nhieu thang?',
        answerType: 'SINGLE_CHOICE',
        options: ['24'],
      ),
    ],
    round: 3,
  );

  @override
  Future<IntakeFlowResponse> continueConversation({
    required String intakeSessionId,
    required Map<String, dynamic> currentIntake,
    required Map<String, dynamic> newAnswers,
    required int round,
  }) async {
    continuedSessionId = intakeSessionId;
    continuedRound = round;
    continuedIntake = currentIntake;
    continuedAnswers = newAnswers;
    return _complete(_result('GREEN'));
  }
}

class _ThrowingTriageService extends TriageService {
  @override
  Future<IntakeFlowResponse> startConversation({
    required String initialText,
    required Map<String, dynamic> currentIntake,
  }) => Future.error(Exception('RAW_SECRET_BACKEND_DETAIL'));
}

class _PendingTriageService extends TriageService {
  final completer = Completer<IntakeFlowResponse>();

  @override
  Future<IntakeFlowResponse> startConversation({
    required String initialText,
    required Map<String, dynamic> currentIntake,
  }) => completer.future;
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
    return Future.error(Exception('RAW_EMERGENCY_FAILURE'));
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
        builder: (_, _) => SymptomIntakeScreen(
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
  return router;
}

Future<void> _submitInitial(WidgetTester tester) async {
  await tester.enterText(find.byType(TextField).first, 'be kho tho');
  await tester.tap(find.byIcon(Icons.send));
  await tester.pump();
}

void main() {
  testWidgets('ASK_MORE continues the same server-owned conversation', (
    tester,
  ) async {
    final triage = _AskMoreTriageService();
    await _pumpScreen(tester, triage: triage);
    await _submitInitial(tester);
    await tester.pumpAndSettle();

    expect(find.text('Be bao nhieu thang?'), findsWidgets);
    await tester.tap(find.text('24'));
    await tester.tap(find.text('Gui cau tra loi'));
    await tester.pumpAndSettle();

    expect(triage.continuedSessionId, _sessionId);
    expect(triage.continuedRound, 3);
    expect(triage.continuedIntake, {'serverState': 'kept'});
    expect(triage.continuedAnswers, {'childAgeMonths': '24'});
    expect(find.text('Risk: GREEN'), findsOneWidget);
  });

  testWidgets('RED inline result ignores duplicate emergency tap', (
    tester,
  ) async {
    final emergency = _DelayedEmergencyService();
    await _pumpScreen(
      tester,
      triage: _StaticTriageService(_complete(_result('RED', emergency: true))),
      emergency: emergency,
    );
    await _submitInitial(tester);

    final cta = find.byKey(const Key('triage-emergency-cta'));
    expect(cta, findsOneWidget);
    await tester.tap(cta);
    await tester.tap(cta);
    expect(emergency.calls, 1);

    emergency.completer.complete(
      const EmergencySession(
        sessionId: 'e1',
        userId: 'u1',
        status: 'ACTIVE',
        triggerSource: 'AI_TRIAGE',
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Emergency map'), findsOneWidget);
  });

  testWidgets(
    'failed RED emergency request has no false success and offers safe map fallback',
    (tester) async {
      final emergency = _FailingEmergencyService();
      await _pumpScreen(
        tester,
        triage: _StaticTriageService(
          _complete(_result('RED', emergency: true)),
        ),
        emergency: emergency,
      );
      await _submitInitial(tester);
      await tester.tap(find.byKey(const Key('triage-emergency-cta')));
      await tester.pumpAndSettle();

      expect(
        find.textContaining('Khong the kich hoat ho tro khan cap'),
        findsOneWidget,
      );
      expect(find.textContaining('RAW_EMERGENCY_FAILURE'), findsNothing);
      expect(find.textContaining('Da kich hoat'), findsNothing);
      final fallback = find.byKey(const Key('triage-emergency-fallback-map'));
      expect(fallback, findsOneWidget);
      tester.widget<TextButton>(fallback).onPressed!();
      await tester.pumpAndSettle();
      expect(find.text('Emergency map'), findsOneWidget);
    },
  );

  testWidgets('only whitelisted HTTPS citations are interactive', (
    tester,
  ) async {
    const allowed = TriageCitation(
      id: 'allowed',
      title: 'WHO guidance',
      source: 'WHO',
      url: 'https://www.who.int/example',
      excerpt: 'Evidence',
      retrievedAt: '2026-07-13',
    );
    const insecure = TriageCitation(
      id: 'insecure',
      title: 'Insecure',
      source: 'WHO',
      url: 'http://who.int/example',
      excerpt: 'Evidence',
      retrievedAt: '2026-07-13',
    );
    const lookalike = TriageCitation(
      id: 'lookalike',
      title: 'Lookalike',
      source: 'Unknown',
      url: 'https://who.int.attacker.example/e',
      excerpt: 'Evidence',
      retrievedAt: '2026-07-13',
    );
    await _pumpScreen(
      tester,
      triage: _StaticTriageService(
        _complete(
          _result('GREEN', citations: const [allowed, insecure, lookalike]),
        ),
      ),
    );
    await _submitInitial(tester);

    final allowedInk = tester.widget<InkWell>(
      find.byKey(const Key('triage-citation-allowed')),
    );
    final insecureInk = tester.widget<InkWell>(
      find.byKey(const Key('triage-citation-insecure')),
    );
    final lookalikeInk = tester.widget<InkWell>(
      find.byKey(const Key('triage-citation-lookalike')),
    );
    expect(allowedInk.onTap, isNotNull);
    expect(insecureInk.onTap, isNull);
    expect(lookalikeInk.onTap, isNull);
  });

  testWidgets('PENDING_REVIEW citation displays governance label', (
    tester,
  ) async {
    const citation = TriageCitation(
      id: 'pending',
      title: 'Realtime official source',
      source: 'MOH',
      url: 'https://moh.gov.vn/example',
      excerpt: 'Evidence',
      retrievedAt: '2026-07-13',
      sourceStatus: 'PENDING_REVIEW',
    );
    await _pumpScreen(
      tester,
      triage: _StaticTriageService(
        _complete(_result('YELLOW', citations: const [citation])),
      ),
    );
    await _submitInitial(tester);

    expect(find.textContaining('dang cho kiem duyet noi bo'), findsOneWidget);
  });

  testWidgets('raw triage exception is never rendered', (tester) async {
    await _pumpScreen(tester, triage: _ThrowingTriageService());
    await _submitInitial(tester);
    await tester.pumpAndSettle();

    expect(find.textContaining('Khong the gui trieu chung'), findsOneWidget);
    expect(find.textContaining('RAW_SECRET_BACKEND_DETAIL'), findsNothing);
  });

  for (final risk in ['GREEN', 'YELLOW']) {
    testWidgets('$risk result preserves content without emergency CTA', (
      tester,
    ) async {
      await _pumpScreen(
        tester,
        triage: _StaticTriageService(_complete(_result(risk))),
      );
      await _submitInitial(tester);

      expect(find.text('Risk: $risk'), findsOneWidget);
      expect(find.text('Theo doi huong dan'), findsOneWidget);
      expect(find.text('Khong thay the chan doan y khoa'), findsOneWidget);
      expect(find.byKey(const Key('triage-emergency-cta')), findsNothing);
    });
  }

  testWidgets('pending response is ignored after intake screen is disposed', (
    tester,
  ) async {
    final triage = _PendingTriageService();
    final router = await _pumpScreen(tester, triage: triage);
    await _submitInitial(tester);
    await tester.pumpWidget(const MaterialApp(home: SizedBox.shrink()));

    triage.completer.complete(_complete(_result('GREEN')));
    await tester.pump();

    expect(tester.takeException(), isNull);
    router.dispose();
  });

  test('legacy result and citation payloads receive safe defaults', () {
    final result = TriageResult.fromJson({
      'sessionId': 'legacy-session',
      'status': 'COMPLETED',
      'riskLevel': 'GREEN',
    });
    final citation = TriageCitation.fromJson({
      'id': 'WHO_LEGACY',
      'title': 'WHO',
      'source': 'WHO',
      'url': 'https://who.int/example',
      'excerpt': 'evidence',
    });

    expect(result.sessionId, 'legacy-session');
    expect(result.status, 'COMPLETED');
    expect(result.emergencyActionRequired, isFalse);
    expect(citation.id, 'WHO_LEGACY');
    expect(citation.sourceVersion, isNull);
    expect(citation.retrievalMode, 'LOCAL');
    expect(citation.matchedRules, isEmpty);
  });
}
