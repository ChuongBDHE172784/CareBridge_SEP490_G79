import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/aiTriage/models/triage_entry_context.dart';
import 'package:untitled/features/aiTriage/models/triage_session.dart';
import 'package:untitled/features/aiTriage/screens/symptom_intake_screen.dart';
import 'package:untitled/features/aiTriage/services/triage_service.dart';

const _sessionId = '11111111-1111-1111-1111-111111111111';

class _CanonicalFixtureService extends TriageSessionService {
  _CanonicalFixtureService(this.turns);

  final List<TriageSession> turns;
  final List<Map<String, dynamic>> starts = [];
  final List<List<TriageAnswer>> answers = [];
  final List<String> continuationMessages = [];
  int _index = 0;

  @override
  Future<TriageSession> start({
    required String message,
    required String selectedTarget,
    required String selectedStage,
    String? profileId,
    Map<String, dynamic> lifecycleBinding = const {},
  }) async {
    starts.add({
      'message': message,
      'target': selectedTarget,
      'stage': selectedStage,
      'profileId': profileId,
      ...lifecycleBinding,
    });
    return turns[_index++];
  }

  @override
  Future<TriageSession> continueSession({
    required TriageSession session,
    required String message,
    List<TriageAnswer> answers = const [],
  }) async {
    this.answers.add(List.of(answers));
    continuationMessages.add(message);
    return turns[_index++];
  }
}

TriageSession _session({
  int version = 1,
  String target = 'BABY',
  String stage = 'INFANT_0_12M',
  String outcome = 'NEEDS_MORE_INFO',
  String action = 'ASK_CLARIFYING_QUESTIONS',
  bool stop = false,
  List<String> questions = const ['Q_GLOBAL_DANGER'],
}) => TriageSession(
  sessionId: _sessionId,
  stateVersion: version,
  target: target,
  intent: 'SYMPTOM_TRIAGE',
  stage: stage,
  outcome: outcome,
  action: action,
  stop: stop,
  questionIds: questions,
  questionDetails: questions.map(_question).toList(growable: false),
  scope: 'IN_SCOPE',
  pendingRisks: const [],
  citations: const [],
  disclaimer: 'Khong thay the chan doan y khoa.',
  readiness: const {'technicalStatus': 'READY'},
);

TriageQuestion _question(String id) {
  if (id == 'Q_GLOBAL_DANGER') {
    return const TriageQuestion(
      id: 'Q_GLOBAL_DANGER',
      text: 'Hiện tại có dấu hiệu nào sau đây không?',
      answerType: 'SINGLE_CHOICE',
      options: [
        TriageQuestionOption(
          optionCode: 'DANGER_NONE',
          displayText: 'Không có dấu hiệu nào',
        ),
      ],
    );
  }
  if (id == 'Q_BABY_AGE_MONTHS') {
    return const TriageQuestion(
      id: 'Q_BABY_AGE_MONTHS',
      text: 'Bé hiện được bao nhiêu tháng tuổi?',
      answerType: 'NUMBER',
      options: [],
    );
  }
  return TriageQuestion(
    id: id,
    text: 'Bé bú hoặc uống như thế nào so với bình thường?',
    answerType: 'SINGLE_CHOICE',
    options: const [
      TriageQuestionOption(
        optionCode: 'FEEDING_NORMAL',
        displayText: 'Bú/uống như bình thường',
      ),
    ],
  );
}

Future<void> _pump(
  WidgetTester tester,
  _CanonicalFixtureService service, {
  TriageEntryContext context = const TriageEntryContext(),
}) => tester.pumpWidget(
  MaterialApp(
    home: SymptomIntakeScreen(
      triageSessionService: service,
      entryContext: context,
    ),
  ),
);

Future<void> _send(WidgetTester tester, String message) async {
  await tester.enterText(find.byType(TextField).first, message);
  await tester.tap(find.byKey(const Key('triage-chat-send')));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('all stages use the canonical session start', (tester) async {
    final service = _CanonicalFixtureService([_session()]);
    await _pump(tester, service);

    await _send(tester, 'Be bo bu');

    expect(service.starts, hasLength(1));
    expect(service.starts.single['target'], 'BABY');
    expect(service.starts.single['stage'], 'INFANT_0_12M');
    expect(find.textContaining('dấu hiệu nào'), findsWidgets);
  });

  testWidgets('planned option continues the same versioned session', (
    tester,
  ) async {
    final service = _CanonicalFixtureService([
      _session(questions: const ['Q_GLOBAL_DANGER']),
      _session(version: 2, questions: const ['Q_BABY_FEEDING']),
    ]);
    await _pump(tester, service);
    await _send(tester, 'Be sot');

    final option = find.widgetWithText(ChoiceChip, 'Không có dấu hiệu nào');
    await tester.ensureVisible(option);
    await tester.tap(option);
    await tester.pump();
    final submit = find.byIcon(Icons.arrow_upward_rounded).last;
    await tester.ensureVisible(submit);
    await tester.tap(submit);
    await tester.pumpAndSettle();

    expect(service.answers, hasLength(1));
    expect(service.answers.single.single.questionId, 'Q_GLOBAL_DANGER');
    expect(service.answers.single.single.optionCode, 'DANGER_NONE');
    expect(
      service.continuationMessages.single,
      contains('Không có dấu hiệu nào'),
    );
    expect(service.continuationMessages.single, isNot(contains('DANGER_NONE')));
    expect(find.textContaining('bú hoặc uống'), findsWidgets);
  });

  testWidgets('RED renders emergency action and never a safe result', (
    tester,
  ) async {
    final service = _CanonicalFixtureService([
      _session(
        outcome: 'RED',
        action: 'IMMEDIATE_EMERGENCY_ASSESSMENT',
        stop: true,
        questions: const [],
      ),
    ]);
    await _pump(tester, service);
    await _send(tester, 'Be khong tho duoc');

    expect(find.textContaining('Mức rủi ro: Đỏ'), findsOneWidget);
    expect(find.byKey(const Key('triage-emergency-cta')), findsOneWidget);
    expect(find.textContaining('GREEN'), findsNothing);
  });

  testWidgets('mandatory disclaimer is always visible', (tester) async {
    await _pump(tester, _CanonicalFixtureService([_session()]));

    expect(
      find.byKey(const Key('triage-mandatory-disclaimer')),
      findsOneWidget,
    );
    expect(
      find.text(
        'Thông tin từ AI chỉ mang tính chất tham khảo, bạn cần tham vấn trực tiếp Bác sĩ/Chuyên gia Y tế khi có triệu chứng bất thường.',
      ),
      findsOneWidget,
    );
  });

  testWidgets('implausible Celsius never reaches the service', (tester) async {
    final service = _CanonicalFixtureService([_session()]);
    await _pump(tester, service);

    await _send(tester, 'Bé sốt 100 độ C');

    expect(service.starts, isEmpty);
    expect(find.textContaining('30–45°C'), findsOneWidget);
  });

  testWidgets('negative baby age never continues the session', (tester) async {
    final service = _CanonicalFixtureService([
      _session(questions: const ['Q_BABY_AGE_MONTHS']),
    ]);
    await _pump(tester, service);
    await _send(tester, 'Bé khó chịu');

    await tester.enterText(find.byType(TextField).last, '-1');
    await tester.tap(find.text('Gửi câu trả lời'));
    await tester.pumpAndSettle();

    expect(service.answers, isEmpty);
    expect(find.textContaining('0 đến 23'), findsOneWidget);
  });

  testWidgets('valid baby age is sent as a structured numeric answer', (
    tester,
  ) async {
    final service = _CanonicalFixtureService([
      _session(questions: const ['Q_BABY_AGE_MONTHS']),
      _session(version: 2, questions: const ['Q_GLOBAL_DANGER']),
    ]);
    await _pump(tester, service);
    await _send(tester, 'Bé khó chịu');

    await tester.enterText(find.byType(TextField).last, '2');
    await tester.tap(find.text('Gửi câu trả lời'));
    await tester.pumpAndSettle();

    expect(service.answers.single.single.questionId, 'Q_BABY_AGE_MONTHS');
    expect(service.answers.single.single.numericValue, 2);
    expect(service.answers.single.single.optionCode, isNull);
  });

  testWidgets('trusted maternal lifecycle origin reaches canonical boundary', (
    tester,
  ) async {
    final service = _CanonicalFixtureService([
      _session(
        target: 'MOTHER',
        stage: 'PREGNANCY',
        questions: const ['Q_GLOBAL_DANGER'],
      ),
    ]);
    const journey = '68000000-0000-0000-0000-000000000002';
    await _pump(
      tester,
      service,
      context: const TriageEntryContext.locked(
        stage: TriageStageIntent.pregnancy,
        origin: TriageOriginIntent.motherJourney,
        journeyId: journey,
        originReferenceId: journey,
      ),
    );
    await _send(tester, 'Toi dau dau');

    expect(service.starts.single['target'], 'MOTHER');
    expect(service.starts.single['stage'], 'PREGNANCY');
    expect(service.starts.single['journeyId'], journey);
    expect(service.starts.single['originDashboard'], 'MOTHER_JOURNEY');
  });
}
