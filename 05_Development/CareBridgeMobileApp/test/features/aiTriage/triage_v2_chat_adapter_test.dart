import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/aiTriage/models/triage_v2_chat_adapter.dart';
import 'package:untitled/features/aiTriage/models/triage_v2_session.dart';

TriageV2Session session({
  String outcome = 'NEEDS_MORE_INFO',
  String action = 'ROUTE_TO_HEALTHCARE_WORKER',
  String stage = 'PREGNANCY',
  bool stop = false,
  List<String> questionIds = const [],
  List<String> pendingRisks = const [],
  List<TriageV2Citation> citations = const [],
}) {
  return TriageV2Session(
    sessionId: '11111111-1111-4111-8111-111111111111',
    stateVersion: 1,
    target: 'MOTHER',
    intent: 'SYMPTOM_TRIAGE',
    stage: stage,
    outcome: outcome,
    action: action,
    stop: stop,
    questionIds: questionIds,
    scope: 'IN_SCOPE',
    pendingRisks: pendingRisks,
    citations: citations,
    disclaimer: 'Thông tin tham khảo, không phải chẩn đoán.',
    readiness: const {'technicalStatus': 'READY'},
  );
}

void main() {
  group('stage translation', () {
    test('maps V2 stages onto the vocabulary the chat keys off', () {
      expect(TriageV2ChatAdapter.stageForChat(session(stage: 'POSTPARTUM_MOTHER'),
          fallback: 'PREGNANCY'), 'POSTPARTUM');
      expect(TriageV2ChatAdapter.stageForChat(session(stage: 'INFANT_0_12M'),
          fallback: 'PREGNANCY'), 'INFANT');
      expect(TriageV2ChatAdapter.stageForChat(session(stage: 'TODDLER_12_24M'),
          fallback: 'PREGNANCY'), 'TODDLER');
    });

    test('an unresolved stage falls back rather than inventing one', () {
      expect(TriageV2ChatAdapter.stageForChat(session(stage: 'UNKNOWN'),
          fallback: 'PREGNANCY'), 'PREGNANCY');
      expect(TriageV2ChatAdapter.stageForChat(session(stage: 'CONFLICTED'),
          fallback: 'POSTPARTUM'), 'POSTPARTUM');
    });
  });

  group('questions', () {
    test('planned ids become answerable questions with their option codes', () {
      final questions = TriageV2ChatAdapter.questions(
        session(questionIds: ['Q_GLOBAL_DANGER']),
      );

      expect(questions, hasLength(1));
      expect(questions.single.questionKey, 'Q_GLOBAL_DANGER');
      expect(questions.single.answerType, 'SINGLE_CHOICE');
      expect(questions.single.options, contains('DANGER_SEIZURE'));
      expect(questions.single.text, isNotEmpty);
    });

    test('a question with no options degrades to free text, never an empty choice list', () {
      final questions = TriageV2ChatAdapter.questions(
        session(questionIds: ['Q_NOT_IN_THE_CATALOGUE']),
      );

      expect(questions.single.answerType, 'TEXT');
      expect(questions.single.options, isEmpty);
    });
  });

  group('result', () {
    test('no result while the conversation is still gathering information', () {
      expect(TriageV2ChatAdapter.result(session(stop: false), chatStage: 'PREGNANCY'), isNull);
    });

    test('a stopped RED turn becomes an emergency result', () {
      final result = TriageV2ChatAdapter.result(
        session(outcome: 'RED', action: 'IMMEDIATE_EMERGENCY_ASSESSMENT', stop: true),
        chatStage: 'PREGNANCY',
      );

      expect(result, isNotNull);
      expect(result!.riskLevel, 'RED');
      expect(result.emergencyActionRequired, isTrue);
      expect(result.disclaimer, isNotEmpty);
    });

    test('a self-harm disposition also demands immediate action', () {
      final result = TriageV2ChatAdapter.result(
        session(outcome: 'RED', action: 'IMMEDIATE_SAFETY_SUPPORT', stop: true),
        chatStage: 'POSTPARTUM',
      );

      expect(result!.emergencyActionRequired, isTrue);
    });

    test('the adapter can never produce GREEN', () {
      // V2's release gate rewrites a would-be GREEN to NEEDS_MORE_INFO, so no session can carry
      // it. Asserting here stops a future edit from reintroducing a reassuring outcome.
      for (final outcome in TriageV2Session.outcomes) {
        final result = TriageV2ChatAdapter.result(
          session(outcome: outcome, stop: true),
          chatStage: 'PREGNANCY',
        );
        expect(result?.riskLevel, isNot('GREEN'), reason: outcome);
      }
    });

    test('an unrecognised outcome yields no result rather than a guess', () {
      expect(
        TriageV2ChatAdapter.result(session(outcome: 'GREEN', stop: true), chatStage: 'PREGNANCY'),
        isNull,
      );
    });
  });

  group('assistant message', () {
    test('asks for answers whenever questions are pending', () {
      final message = TriageV2ChatAdapter.assistantMessage(
        session(questionIds: ['Q_GLOBAL_DANGER'], action: 'ASK_CLARIFYING_QUESTIONS'),
      );
      expect(message, contains('câu hỏi'));
    });

    test('never states a diagnosis or a medicine', () {
      for (final action in [
        'IMMEDIATE_EMERGENCY_ASSESSMENT',
        'IMMEDIATE_SAFETY_SUPPORT',
        'EARLY_CLINICAL_ASSESSMENT',
        'ROUTE_TO_HEALTHCARE_WORKER',
        'OUT_OF_SCOPE_REDIRECT',
        'SOMETHING_UNEXPECTED',
      ]) {
        final message = TriageV2ChatAdapter.assistantMessage(session(action: action));
        expect(message, isNotEmpty, reason: action);
        for (final forbidden in ['chẩn đoán là', 'thuốc', 'liều', 'kê đơn']) {
          expect(message.toLowerCase(), isNot(contains(forbidden)), reason: '$action/$forbidden');
        }
      }
    });
  });

  group('flow projection', () {
    test('carries the chat stage into the merged intake so stage guards agree', () {
      final response = TriageV2ChatAdapter.toFlowResponse(
        session(stage: 'POSTPARTUM_MOTHER'),
        fallbackStage: 'PREGNANCY',
        round: 2,
        mergedIntake: const {'stage': 'PREGNANCY'},
      );

      expect(response.stage, 'POSTPARTUM');
      expect(response.mergedIntake['stage'], 'POSTPARTUM');
      expect(response.round, 2);
      expect(response.status, 'NEED_MORE_INFO');
    });

    test('a stopped turn reports completion', () {
      final response = TriageV2ChatAdapter.toFlowResponse(
        session(outcome: 'RED', action: 'IMMEDIATE_EMERGENCY_ASSESSMENT', stop: true),
        fallbackStage: 'PREGNANCY',
        round: 1,
        mergedIntake: const {},
      );

      expect(response.status, 'TRIAGE_COMPLETE');
      expect(response.triageResult?.riskLevel, 'RED');
    });
  });
}
