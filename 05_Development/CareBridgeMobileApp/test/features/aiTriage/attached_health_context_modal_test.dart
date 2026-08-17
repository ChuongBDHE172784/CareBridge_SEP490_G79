import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/aiTriage/screens/rag_chat_screen.dart';
import 'package:untitled/core/auth/auth_state.dart';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

void main() {
  setUp(() {
    FlutterSecureStorage.setMockInitialValues({});
    AuthState.instance.setTokens(
      accessToken: 'test_token',
      refreshToken: 'test_refresh',
      userId: 'user-test',
      role: 'MOTHER',
    );
  });

  Widget buildScreen({Map<String, dynamic>? attachedContext, String? prompt}) {
    return MaterialApp(
      home: RagChatScreen(
        initialPrompt: prompt,
        attachedHealthContext: attachedContext,
        autoSendInitialPrompt: false,
      ),
    );
  }

  testWidgets('renders attachment banner and opens modal with full context', (tester) async {
    tester.view.physicalSize = const Size(1170, 2532); // iPhone 13 / 14 / 15 / 16 size
    tester.view.devicePixelRatio = 3.0;
    addTearDown(() => tester.view.resetPhysicalSize());

    final contextData = <String, dynamic>{
      'metricLabel': 'Huyết áp',
      'displayValue': '135/88 mmHg',
      'gestationalAge': 24,
      'stage': 'PREGNANCY',
      'riskFactors': ['Tiền tăng huyết áp (135/88 mmHg)'],
      'latestVitals': {
        'Huyết áp': '135/88 mmHg',
        'Thân nhiệt': '37.0 °C',
      },
      'surveyRiskConditions': ['CHRONIC_HYPERTENSION'],
      'note': 'Hơi mệt và đau đầu nhẹ',
    };

    await tester.pumpWidget(buildScreen(attachedContext: contextData, prompt: 'Câu hỏi'));
    await tester.pumpAndSettle();

    // Verify banner is visible
    expect(find.text('Hồ sơ đính kèm: Huyết áp'), findsOneWidget);
    expect(find.text('Tuần 24'), findsOneWidget);
    expect(find.text('Chạm để xem chi tiết sinh hiệu, survey...'), findsOneWidget);

    // Tap the banner to open modal
    await tester.ensureVisible(find.byKey(const Key('attachment_context_banner_tap')));
    await tester.tap(find.byKey(const Key('attachment_context_banner_tap')));
    await tester.pumpAndSettle();

    // Verify modal is open and shows expected content
    expect(find.text('Hồ sơ Sức khỏe Đính kèm'), findsOneWidget);
    expect(find.text('Tuần thai hiện tại: Tuần 24'), findsOneWidget);
    expect(find.text('Tam cá nguyệt 2 (3 tháng giữa)'), findsOneWidget);
    expect(find.text('Chỉ số sinh hiệu vừa ghi nhận'), findsOneWidget);
    expect(find.text('135/88 mmHg'), findsAtLeastNWidgets(1));
    expect(find.text('Gỡ đính kèm'), findsOneWidget);
    expect(find.text('Đã hiểu'), findsOneWidget);

    // Tap "Đã hiểu" to close modal
    await tester.tap(find.text('Đã hiểu'));
    await tester.pumpAndSettle();

    expect(find.text('Hồ sơ Sức khỏe Đính kèm'), findsNothing);
    expect(find.text('Hồ sơ đính kèm: Huyết áp'), findsOneWidget);
  });

  testWidgets('opens modal for Preconception stage', (tester) async {
    final contextData = <String, dynamic>{
      'metricLabel': 'Chỉ số BMI',
      'displayValue': '21.5 kg/m²',
      'stage': 'PRECONCEPTION',
      'journeyType': 'PRE_PREGNANCY',
      'surveyRiskConditions': <String>[],
    };

    await tester.pumpWidget(buildScreen(attachedContext: contextData));
    await tester.pumpAndSettle();

    expect(find.text('Chuẩn bị mang thai'), findsOneWidget);

    await tester.tap(find.byKey(const Key('attachment_context_banner_tap')));
    await tester.pumpAndSettle();

    expect(find.text('Giai đoạn: Chuẩn bị mang thai'), findsOneWidget);
    expect(find.text('Kế hoạch thụ thai, bổ sung vi chất & sàng lọc tiền sản'), findsOneWidget);
  });

  testWidgets('opens modal for Postpartum stage and removes attachment', (tester) async {
    final contextData = <String, dynamic>{
      'metricLabel': 'Điểm EPDS',
      'displayValue': '8/30',
      'stage': 'POSTPARTUM',
      'journeyType': 'POSTPARTUM',
    };

    await tester.pumpWidget(buildScreen(attachedContext: contextData));
    await tester.pumpAndSettle();

    expect(find.text('Hậu sản & Chăm bé'), findsOneWidget);

    await tester.tap(find.byKey(const Key('attachment_context_banner_tap')));
    await tester.pumpAndSettle();

    expect(find.text('Giai đoạn: Hậu sản & Chăm sóc bé'), findsOneWidget);
    expect(find.text('Phục hồi thể chất, nuôi con bằng sữa mẹ & sức khỏe tinh thần'), findsOneWidget);

    // Tap "Gỡ đính kèm"
    await tester.tap(find.text('Gỡ đính kèm'));
    await tester.pumpAndSettle();

    // Modal closed and banner removed
    expect(find.text('Hồ sơ Sức khỏe Đính kèm'), findsNothing);
    expect(find.text('Hồ sơ đính kèm: Điểm EPDS'), findsNothing);
  });
}
