import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/features/aiTriage/models/triage_entry_context.dart';
import 'package:untitled/features/aiTriage/models/triage_intake_flow_model.dart';
import 'package:untitled/features/aiTriage/models/triage_result_model.dart';
import 'package:untitled/features/aiTriage/screens/risk_triage_result_screen.dart';
import 'package:untitled/features/aiTriage/screens/symptom_intake_screen.dart';
import 'package:untitled/features/aiTriage/services/triage_service.dart';

const _intakeSessionId = '68000000-0000-0000-0000-000000000001';
const _journeyId = '68000000-0000-0000-0000-000000000002';
const _originReferenceId = '68000000-0000-0000-0000-000000000003';
const _yellowSummary = 'Synthetic lifecycle-bound YELLOW result';
const _handoffPath = '/triage/expert-handoff';

TriageResult _yellowResult() => const TriageResult(
  sessionId: _intakeSessionId,
  status: 'COMPLETED',
  stage: 'PREGNANCY',
  riskLevel: 'YELLOW',
  summary: _yellowSummary,
  journeyId: _journeyId,
  originDashboard: 'MOTHER_JOURNEY',
  originReferenceId: _originReferenceId,
);

class _InlineYellowService extends TriageService {
  @override
  Future<IntakeFlowResponse> startConversation({
    required String initialText,
    required Map<String, dynamic> currentIntake,
  }) async => IntakeFlowResponse(
    status: 'TRIAGE_COMPLETE',
    intakeSessionId: _intakeSessionId,
    stage: 'PREGNANCY',
    mergedIntake: const {
      'stage': 'PREGNANCY',
      'journeyId': _journeyId,
      'originDashboard': 'MOTHER_JOURNEY',
      'originReferenceId': _originReferenceId,
    },
    round: 1,
    triageResult: _yellowResult(),
    journeyId: _journeyId,
    originDashboard: 'MOTHER_JOURNEY',
    originReferenceId: _originReferenceId,
  );
}

class _RoutedYellowService extends TriageService {
  @override
  Future<TriageResult> getResult(String sessionId) async => _yellowResult();
}

void main() {
  testWidgets(
    'RED: inline terminal YELLOW exposes the typed expert handoff CTA',
    (tester) async {
      Object? receivedExtra;
      Uri? receivedUri;
      final router = GoRouter(
        routes: [
          GoRoute(
            path: '/',
            builder: (_, _) => SymptomIntakeScreen(
              triageService: _InlineYellowService(),
              entryContext: const TriageEntryContext.locked(
                stage: TriageStageIntent.pregnancy,
                origin: TriageOriginIntent.motherJourney,
                journeyId: _journeyId,
                originReferenceId: _originReferenceId,
              ),
            ),
          ),
          GoRoute(
            path: _handoffPath,
            builder: (_, state) {
              receivedExtra = state.extra;
              receivedUri = state.uri;
              return const Scaffold(body: Text('Story 6.8 handoff target'));
            },
          ),
        ],
      );
      addTearDown(router.dispose);
      await tester.pumpWidget(MaterialApp.router(routerConfig: router));

      await tester.enterText(
        find.byType(TextField).first,
        'Synthetic input for a YELLOW fixture',
      );
      await tester.tap(find.byKey(const Key('triage-chat-send')));
      await tester.pumpAndSettle();

      expect(find.text(_yellowSummary), findsWidgets);
      final handoffCta = find.byKey(
        const Key('triage-inline-yellow-expert-handoff-cta'),
      );
      expect(handoffCta, findsOneWidget);

      await tester.ensureVisible(handoffCta);
      await tester.tap(handoffCta);
      await tester.pumpAndSettle();

      expect(receivedUri?.path, _handoffPath);
      expect(receivedUri?.queryParameters, isEmpty);
      expect(receivedExtra, _intakeSessionId);
    },
  );

  testWidgets(
    'RED: routed terminal YELLOW replaces placeholders with typed expert handoff',
    (tester) async {
      Object? receivedExtra;
      Uri? receivedUri;
      final router = GoRouter(
        initialLocation: '/triage/result/$_intakeSessionId',
        routes: [
          GoRoute(
            path: '/triage/result/:sessionId',
            builder: (_, state) => RiskTriageResultScreen(
              sessionId: state.pathParameters['sessionId']!,
              triageService: _RoutedYellowService(),
            ),
          ),
          GoRoute(
            path: _handoffPath,
            builder: (_, state) {
              receivedExtra = state.extra;
              receivedUri = state.uri;
              return const Scaffold(body: Text('Story 6.8 handoff target'));
            },
          ),
        ],
      );
      addTearDown(router.dispose);
      await tester.pumpWidget(MaterialApp.router(routerConfig: router));
      await tester.pumpAndSettle();

      expect(find.text(_yellowSummary), findsOneWidget);
      final handoffCta = find.byKey(
        const Key('risk-result-yellow-expert-handoff-cta'),
      );
      expect(handoffCta, findsOneWidget);
      expect(find.byKey(const Key('risk-result-doctor-cta')), findsNothing);
      expect(find.byKey(const Key('risk-result-clinic-cta')), findsNothing);

      await tester.ensureVisible(handoffCta);
      await tester.tap(handoffCta);
      await tester.pumpAndSettle();

      expect(receivedUri?.path, _handoffPath);
      expect(receivedUri?.queryParameters, isEmpty);
      expect(receivedExtra, _intakeSessionId);
    },
  );
}
