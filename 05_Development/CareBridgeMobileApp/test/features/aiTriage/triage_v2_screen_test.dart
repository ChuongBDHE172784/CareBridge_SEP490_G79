import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/aiTriage/screens/triage_v2_screen.dart';
import 'package:untitled/features/aiTriage/services/triage_v2_service.dart';

void main() {
  testWidgets(
    'shows explicit target selection and stable target option codes',
    (tester) async {
      Map<String, dynamic>? startBody;
      final service = TriageV2Service(
        postRequest: (_, body) async {
          startBody = body;
          return {
            'data': _response(
              target: 'UNKNOWN',
              questions: ['Q_CLARIFY_TARGET_ENTITY'],
            ),
          };
        },
      );
      await tester.pumpWidget(
        MaterialApp(home: TriageV2Screen(service: service)),
      );
      await tester.tap(find.byKey(const Key('triage-v2-target-BABY')));
      await tester.enterText(
        find.byKey(const Key('triage-v2-message')),
        'bé bị sốt',
      );
      await tester.tap(find.byKey(const Key('triage-v2-submit')));
      await tester.pumpAndSettle();

      expect(startBody?['selectedTarget'], 'BABY');
      expect(
        find.byKey(const Key('triage-v2-option-CLARIFY_TARGET_MOTHER')),
        findsOneWidget,
      );
      expect(
        find.byKey(const Key('triage-v2-option-CLARIFY_TARGET_BABY')),
        findsOneWidget,
      );
      expect(
        find.byKey(const Key('triage-v2-option-CLARIFY_TARGET_BOTH')),
        findsOneWidget,
      );
    },
  );

  testWidgets(
    'renders RED action before verified evidence and never diagnosis',
    (tester) async {
      final service = TriageV2Service(
        postRequest: (_, _) async => {
          'data': _response(
            outcome: 'RED',
            stop: true,
            citations: [_verifiedCitation()],
          ),
        },
      );
      await tester.pumpWidget(
        MaterialApp(home: TriageV2Screen(service: service)),
      );
      await tester.enterText(
        find.byKey(const Key('triage-v2-message')),
        'khó thở nghiêm trọng',
      );
      await tester.tap(find.byKey(const Key('triage-v2-submit')));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('triage-v2-red-action')), findsOneWidget);
      expect(
        find.byKey(const Key('triage-v2-source-WHO_TEST')),
        findsOneWidget,
      );
      final redTop = tester
          .getTopLeft(find.byKey(const Key('triage-v2-red-action')))
          .dy;
      final sourceTop = tester
          .getTopLeft(find.byKey(const Key('triage-v2-source-WHO_TEST')))
          .dy;
      expect(redTop, lessThan(sourceTop));
      expect(find.textContaining('chẩn đoán'), findsAtLeastNWidgets(1));
      expect(find.textContaining('thuốc'), findsAtLeastNWidgets(1));
    },
  );

  testWidgets('system failure is controlled unavailable and never GREEN', (
    tester,
  ) async {
    final service = TriageV2Service(
      postRequest: (_, _) async => throw StateError('python down'),
    );
    await tester.pumpWidget(
      MaterialApp(home: TriageV2Screen(service: service)),
    );
    await tester.enterText(
      find.byKey(const Key('triage-v2-message')),
      'không rõ',
    );
    await tester.tap(find.byKey(const Key('triage-v2-submit')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('triage-v2-error')), findsOneWidget);
    expect(find.textContaining('mức an toàn'), findsOneWidget);
    expect(find.textContaining('GREEN'), findsNothing);
  });

  testWidgets('submits every selected answer in one V2 round', (tester) async {
    var call = 0;
    Map<String, dynamic>? continueBody;
    final service = TriageV2Service(
      postRequest: (_, body) async {
        call++;
        if (call == 1) {
          return {
            'data': _response(questions: ['Q_DIZZINESS', 'Q_VISUAL_CHANGE']),
          };
        }
        continueBody = Map<String, dynamic>.from(body);
        return {'data': _response(outcome: 'YELLOW', stop: true)};
      },
    );
    await tester.pumpWidget(
      MaterialApp(home: TriageV2Screen(service: service)),
    );
    await tester.enterText(
      find.byKey(const Key('triage-v2-message')),
      'đau đầu',
    );
    await tester.tap(find.byKey(const Key('triage-v2-submit')));
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('triage-v2-option-DIZZINESS_NO')));
    await tester.tap(
      find.byKey(const Key('triage-v2-option-VISUAL_CHANGE_NO')),
    );
    await tester.tap(find.byKey(const Key('triage-v2-submit')));
    await tester.pumpAndSettle();

    expect(continueBody?['answers'], [
      {'questionId': 'Q_DIZZINESS', 'optionCode': 'DIZZINESS_NO'},
      {'questionId': 'Q_VISUAL_CHANGE', 'optionCode': 'VISUAL_CHANGE_NO'},
    ]);
  });

  testWidgets('NUMBER question keeps a free-text input available', (
    tester,
  ) async {
    await tester.binding.setSurfaceSize(const Size(800, 900));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    var call = 0;
    Map<String, dynamic>? continueBody;
    final service = TriageV2Service(
      postRequest: (_, body) async {
        call++;
        if (call == 1) {
          return {
            'data': _response(questions: ['Q_GESTATIONAL_WEEK']),
          };
        }
        continueBody = Map<String, dynamic>.from(body);
        return {'data': _response(outcome: 'YELLOW', stop: true)};
      },
    );
    await tester.pumpWidget(
      MaterialApp(home: TriageV2Screen(service: service)),
    );
    await tester.enterText(
      find.byKey(const Key('triage-v2-message')),
      'đang mang thai',
    );
    await tester.tap(find.byKey(const Key('triage-v2-submit')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('triage-v2-message')), findsOneWidget);
    await tester.enterText(
      find.byKey(const Key('triage-v2-message')),
      '31 tuần',
    );
    await tester.ensureVisible(find.byKey(const Key('triage-v2-submit')));
    await tester.tap(find.byKey(const Key('triage-v2-submit')));
    await tester.pumpAndSettle();

    expect(continueBody?['message'], '31 tuần');
    expect(continueBody?['answers'], isEmpty);
  });

  testWidgets('mother and baby are handled in separate sessions', (
    tester,
  ) async {
    var call = 0;
    final service = TriageV2Service(
      postRequest: (_, _) async {
        call++;
        return {
          'data': switch (call) {
            1 => _response(
              target: 'UNKNOWN',
              questions: ['Q_CLARIFY_TARGET_ENTITY'],
            ),
            2 => _response(
              target: 'CONFLICTED',
              questions: ['Q_CLARIFY_TARGET_FIRST'],
            ),
            _ => _response(target: 'MOTHER', outcome: 'RED', stop: true),
          },
        };
      },
    );
    await tester.pumpWidget(
      MaterialApp(home: TriageV2Screen(service: service)),
    );
    await tester.enterText(
      find.byKey(const Key('triage-v2-message')),
      'mẹ và bé đều sốt',
    );
    await tester.tap(find.byKey(const Key('triage-v2-submit')));
    await tester.pumpAndSettle();
    await tester.tap(
      find.byKey(const Key('triage-v2-option-CLARIFY_TARGET_BOTH')),
    );
    await tester.tap(find.byKey(const Key('triage-v2-submit')));
    await tester.pumpAndSettle();
    await tester.tap(
      find.byKey(const Key('triage-v2-option-CLARIFY_TARGET_MOTHER')),
    );
    await tester.tap(find.byKey(const Key('triage-v2-submit')));
    await tester.pumpAndSettle();

    expect(
      find.byKey(const Key('triage-v2-start-other-person')),
      findsOneWidget,
    );
    await tester.tap(find.byKey(const Key('triage-v2-start-other-person')));
    await tester.pump();

    final baby = tester.widget<ChoiceChip>(
      find.byKey(const Key('triage-v2-target-BABY')),
    );
    expect(baby.selected, isTrue);
    expect(find.byKey(const Key('triage-v2-context')), findsNothing);
  });
}

Map<String, dynamic> _response({
  String target = 'MOTHER',
  String outcome = 'NEEDS_MORE_INFO',
  bool stop = false,
  List<String> questions = const [],
  List<Map<String, dynamic>> citations = const [],
}) => {
  'sessionId': '10000000-0000-0000-0000-000000000001',
  'stateVersion': 1,
  'target': target,
  'intent': 'SYMPTOM_TRIAGE',
  'stage': target == 'BABY' ? 'INFANT_0_12M' : 'PREGNANCY',
  'outcome': outcome,
  'action': outcome == 'RED'
      ? 'SEEK_EMERGENCY_CARE_NOW'
      : 'ASK_CLARIFYING_QUESTIONS',
  'stop': stop,
  'questions': questions,
  'scope': 'IN_SCOPE',
  'pendingRisks': const <String>[],
  'citations': citations,
  'disclaimer': 'Thông tin tham khảo, không chẩn đoán.',
  'readiness': const {'technicalStatus': 'READY'},
};

Map<String, dynamic> _verifiedCitation() => {
  'sourceId': 'WHO_TEST',
  'title': 'Danger signs',
  'organization': 'World Health Organization',
  'url': 'https://www.who.int/publications/i/item/test',
  'domain': 'who.int',
  'section': 'Danger signs',
  'contentHash': List.filled(64, 'b').join(),
  'sourceStatus': 'SOURCE_VERIFIED',
  'retrievalMode': 'LOCAL_BM25',
  'ruleIds': ['R_TEST'],
};
