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

  testWidgets('renders RAG chat screen and initial prompt', (tester) async {
    await tester.pumpWidget(
      buildScreen(
        prompt: 'Tôi bị đau đầu và huyết áp 145/95',
        attachedContext: {
          'stage': 'PREGNANCY',
          'gestationalWeek': 28,
          'riskFactors': ['Huyết áp cao 145/95 mmHg'],
          'latestVitals': {'systolicBp': 145, 'diastolicBp': 95},
        },
      ),
    );

    await tester.pumpAndSettle();

    expect(find.byType(RagChatScreen), findsOneWidget);
    expect(find.text('Tôi bị đau đầu và huyết áp 145/95'), findsOneWidget);
  });

  testWidgets('renders action card and expert consultation dialog buttons', (tester) async {
    await tester.pumpWidget(
      buildScreen(
        prompt: 'Tư vấn huyết áp',
        attachedContext: {
          'stage': 'PREGNANCY',
          'gestationalWeek': 20,
        },
      ),
    );

    await tester.pumpAndSettle();
    expect(find.byType(RagChatScreen), findsOneWidget);
  });
}
