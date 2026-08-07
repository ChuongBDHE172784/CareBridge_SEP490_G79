import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/aiTriage/models/triage_intake_flow_model.dart';
import 'package:untitled/features/aiTriage/models/triage_result_model.dart';
import 'package:untitled/features/aiTriage/screens/symptom_intake_screen.dart';
import 'package:untitled/features/aiTriage/services/triage_service.dart';

const _sessionId = '22222222-2222-2222-2222-222222222222';

class _AskMoreDemoService extends TriageService {
  Map<String, dynamic>? receivedIntake;

  @override
  Future<IntakeFlowResponse> startConversation({
    required String initialText,
    required Map<String, dynamic> currentIntake,
  }) async {
    receivedIntake = Map<String, dynamic>.from(currentIntake);
    return const IntakeFlowResponse(
      status: 'ASK_MORE',
      intakeSessionId: _sessionId,
      stage: 'INFANT',
      mergedIntake: {
        'stage': 'INFANT',
        'symptomList': ['cough', 'fever'],
      },
      assistantMessage: 'Mình cần thêm vài thông tin để phân loại an toàn hơn.',
      questions: [
        IntakeQuestion(
          questionKey: 'breathingStatus',
          text: 'Bé có khó thở, rút lõm lồng ngực hoặc tím môi không?',
          answerType: 'SINGLE_CHOICE',
          options: ['Không', 'Khó thở', 'Rút lõm lồng ngực', 'Tím môi'],
        ),
      ],
      round: 2,
    );
  }
}

class _RedDemoService extends TriageService {
  @override
  Future<IntakeFlowResponse> startConversation({
    required String initialText,
    required Map<String, dynamic> currentIntake,
  }) async => const IntakeFlowResponse(
    status: 'TRIAGE_COMPLETE',
    intakeSessionId: _sessionId,
    stage: 'INFANT',
    mergedIntake: {
      'stage': 'INFANT',
      'symptomList': ['difficulty_breathing', 'cyanosis'],
    },
    round: 1,
    triageResult: TriageResult(
      sessionId: _sessionId,
      status: 'COMPLETED',
      riskLevel: 'RED',
      emergencyActionRequired: true,
      summary: 'CareBridge ghi nhận dấu hiệu khó thở và tím môi.',
      recommendedAction:
          'Đưa bé đến cơ sở cấp cứu ngay. Không chờ thêm kết quả trực tuyến.',
      redFlags: ['Khó thở', 'Tím môi'],
      citations: [
        TriageCitation(
          id: 'nch-child-danger-signs',
          title: 'Một số dấu hiệu cha mẹ cần biết để đưa trẻ đi khám sớm',
          source: 'Bệnh viện Nhi Trung ương',
          organization: 'Bệnh viện Nhi Trung ương',
          domain: 'benhviennhitrunguong.gov.vn',
          url:
              'https://benhviennhitrunguong.gov.vn/mot-so-dau-hieu-cha-me-can-biet-de-dua-tre-di-kham-som.html',
          excerpt:
              'Trẻ khó thở, rút lõm lồng ngực hoặc tím môi cần được đưa đi khám sớm.',
          retrievedAt: '2026-07-17',
          matchedSymptoms: ['difficulty_breathing', 'cyanosis'],
          matchedRules: ['RED_BREATHING_DISTRESS'],
          sourceStatus: 'APPROVED',
        ),
      ],
      disclaimer:
          'CareBridge không chẩn đoán bệnh, không kê thuốc và không thay thế bác sĩ hoặc cấp cứu.',
    ),
  );
}

Future<void> _pump(
  WidgetTester tester, {
  required TriageService service,
}) async {
  tester.view.physicalSize = const Size(430, 932);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);
  await tester.pumpWidget(
    MaterialApp(home: SymptomIntakeScreen(triageService: service)),
  );
  await tester.pumpAndSettle();
}

Future<void> _submit(WidgetTester tester, String text) async {
  await tester.enterText(find.byType(TextField).first, text);
  await tester.tap(find.byKey(const Key('triage-chat-send')));
  await tester.pumpAndSettle();
}

void main() {
  // The content assertions and the pixel comparison are split deliberately. They fail for
  // different reasons — a missing stage is a real defect, a pixel diff is usually a font or
  // renderer change — and bundling them meant `--exclude-tags golden` could not skip the
  // brittle half without also dropping the meaningful half.
  testWidgets('mobile stage selector renders all four supported stages', (
    tester,
  ) async {
    await _pump(tester, service: _AskMoreDemoService());

    expect(find.text('Chuẩn bị mang thai'), findsOneWidget);
    expect(find.text('Đang mang thai'), findsOneWidget);
    expect(find.text('Bé 0-12 tháng'), findsOneWidget);
    expect(find.text('Bé 12-24 tháng'), findsOneWidget);
  });

  testWidgets('mobile stage selector matches its golden', (tester) async {
    await _pump(tester, service: _AskMoreDemoService());

    await expectLater(
      find.byType(SymptomIntakeScreen),
      matchesGoldenFile('goldens/triage_stage_selector_mobile.png'),
    );
  }, tags: 'golden');

  testWidgets('ASK_MORE renders deterministic option buttons', (tester) async {
    await _pump(tester, service: _AskMoreDemoService());
    await _submit(tester, 'Bé ho và sốt từ hôm qua');

    expect(find.text('Khó thở'), findsOneWidget);
    expect(find.text('Rút lõm lồng ngực'), findsOneWidget);
    expect(find.text('Tím môi'), findsOneWidget);
    await expectLater(
      find.byType(SymptomIntakeScreen),
      matchesGoldenFile('goldens/triage_ask_more_mobile.png'),
    );
  });

  testWidgets('selected stage is sent explicitly in the start intake', (
    tester,
  ) async {
    final service = _AskMoreDemoService();
    await _pump(tester, service: service);
    await tester.tap(find.text('Đang mang thai'));
    await _submit(tester, 'Tôi cần hỗ trợ khi đang mang thai');

    expect(service.receivedIntake?['stage'], 'PREGNANCY');
  });

  testWidgets('RED result and citation bottom sheet render safely', (
    tester,
  ) async {
    await _pump(tester, service: _RedDemoService());
    await _submit(tester, 'Bé khó thở và môi tím');

    expect(find.text('Mức rủi ro: RED'), findsOneWidget);
    expect(find.byKey(const Key('triage-emergency-cta')), findsOneWidget);
    expect(
      find.byKey(const Key('triage-source-chip-nch-child-danger-signs')),
      findsOneWidget,
    );
    await tester.tap(
      find.byKey(const Key('triage-source-chip-nch-child-danger-signs')),
    );
    await tester.pumpAndSettle();
    expect(find.text('Xem nguồn gốc'), findsOneWidget);
    expect(find.textContaining('RED_BREATHING_DISTRESS'), findsOneWidget);
    await expectLater(
      find.byType(Scaffold).first,
      matchesGoldenFile('goldens/triage_red_citation_sheet_mobile.png'),
    );
  });
}
