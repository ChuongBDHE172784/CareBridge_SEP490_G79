import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/aiTriage/models/triage_entry_context.dart';
import 'package:untitled/features/aiTriage/models/triage_intake_flow_model.dart';
import 'package:untitled/features/aiTriage/screens/symptom_intake_screen.dart';
import 'package:untitled/features/aiTriage/services/triage_service.dart';

/// The stage chips and the send guard must agree.
///
/// `_selectedStage` is seeded with a display default (INFANT) so the rest of the screen
/// has something to render, but an unscoped floating entry has not chosen a stage yet and
/// `_start()` refuses to send. Ticking a chip during that window told the user the stage
/// was already picked, so their first send was rejected for a reason the screen appeared
/// to contradict — and it only worked once they touched a chip.
class _RecordingTriageService extends TriageService {
  final List<Map<String, dynamic>> calls = [];

  @override
  Future<IntakeFlowResponse> startConversation({
    required String initialText,
    required Map<String, dynamic> currentIntake,
  }) async {
    calls.add(Map<String, dynamic>.from(currentIntake));
    return IntakeFlowResponse(
      status: 'ASK_MORE',
      intakeSessionId: '11111111-1111-1111-1111-111111111111',
      stage: currentIntake['stage'] as String? ?? 'UNKNOWN',
      mergedIntake: currentIntake,
      round: 2,
      triageResult: null,
    );
  }
}

Future<void> _pumpUnscopedEntry(
  WidgetTester tester,
  _RecordingTriageService triage,
) => tester.pumpWidget(
  MaterialApp(
    home: SymptomIntakeScreen(
      triageService: triage,
      entryContext: const TriageEntryContext(requiresStageSelection: true),
    ),
  ),
);

const _stageError = 'Vui lòng chọn giai đoạn sức khỏe';

IconButton _sendButton(WidgetTester tester) =>
    tester.widget<IconButton>(find.byKey(const Key('triage-chat-send')));

List<String> _tickedChipLabels(WidgetTester tester) => [
  for (final chip in tester.widgetList<ChoiceChip>(find.byType(ChoiceChip)))
    if (chip.selected) (chip.label as Text).data ?? '',
];

void main() {
  testWidgets('no stage chip reads as chosen before the user picks one', (
    tester,
  ) async {
    await _pumpUnscopedEntry(tester, _RecordingTriageService());

    expect(_tickedChipLabels(tester), isEmpty);
  });

  testWidgets('picking a stage lets the very first send through', (
    tester,
  ) async {
    final triage = _RecordingTriageService();
    await _pumpUnscopedEntry(tester, triage);

    await tester.tap(find.widgetWithText(ChoiceChip, 'Đang mang thai'));
    await tester.pump();
    expect(_tickedChipLabels(tester), ['Đang mang thai']);

    await tester.enterText(find.byType(TextField).first, 'Tôi bị đau bụng');
    await tester.tap(find.byKey(const Key('triage-chat-send')));
    await tester.pump();

    expect(find.textContaining(_stageError), findsNothing);
    expect(triage.calls, hasLength(1));
    expect(triage.calls.single['stage'], 'PREGNANCY');
  });

  testWidgets('sending without picking a stage is still refused', (
    tester,
  ) async {
    final triage = _RecordingTriageService();
    await _pumpUnscopedEntry(tester, triage);

    await tester.enterText(find.byType(TextField).first, 'Tôi bị đau bụng');
    await tester.tap(find.byKey(const Key('triage-chat-send')));
    await tester.pump();

    // The refusal is now stated up front and the send is inert, so the after-the-fact error
    // never has to appear. What must not change is that nothing reaches the service.
    expect(triage.calls, isEmpty);
    expect(find.byKey(const Key('triage-stage-required-hint')), findsOneWidget);
  });

  testWidgets('the missing step is stated before the user types, not after', (
    tester,
  ) async {
    await _pumpUnscopedEntry(tester, _RecordingTriageService());

    expect(find.byKey(const Key('triage-stage-required-hint')), findsOneWidget);
    expect(_sendButton(tester).onPressed, isNull);
    expect(find.textContaining(_stageError), findsNothing);

    await tester.tap(find.widgetWithText(ChoiceChip, 'Bé 0-12 tháng'));
    await tester.pump();

    expect(find.byKey(const Key('triage-stage-required-hint')), findsNothing);
    expect(_sendButton(tester).onPressed, isNotNull);
  });

  testWidgets('an entry with a trusted stage is never blocked', (tester) async {
    final triage = _RecordingTriageService();
    await tester.pumpWidget(
      MaterialApp(
        home: SymptomIntakeScreen(
          triageService: triage,
          entryContext: const TriageEntryContext.postpartum(),
        ),
      ),
    );

    expect(find.byKey(const Key('triage-stage-required-hint')), findsNothing);

    await tester.enterText(find.byType(TextField).first, 'Tôi thấy chóng mặt');
    await tester.tap(find.byKey(const Key('triage-chat-send')));
    await tester.pump();

    expect(triage.calls.single['stage'], 'POSTPARTUM');
  });
}
