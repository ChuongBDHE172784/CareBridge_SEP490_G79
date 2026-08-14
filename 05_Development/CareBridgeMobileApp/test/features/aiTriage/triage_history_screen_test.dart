import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/features/aiTriage/models/triage_history_model.dart';
import 'package:untitled/features/aiTriage/screens/triage_history_screen.dart';

void main() {
  const sessionId = '10000000-0000-0000-0000-000000000001';

  testWidgets('opens a saved result from AI Triage history', (tester) async {
    final router = GoRouter(
      initialLocation: '/history',
      routes: [
        GoRoute(
          path: '/history',
          builder: (_, _) => TriageHistoryScreen(
            historyLoader: () async => [
              TriageHistoryItem(
                sessionId: sessionId,
                stage: 'PREGNANCY',
                status: 'COMPLETED',
                riskLevel: 'YELLOW',
                createdAt: DateTime(2026, 8, 3, 10),
              ),
            ],
          ),
        ),
        GoRoute(
          path: '/triage/result/:sessionId',
          builder: (_, state) => Scaffold(
            body: Text('result ${state.pathParameters['sessionId']}'),
          ),
        ),
      ],
    );
    addTearDown(router.dispose);

    await tester.pumpWidget(MaterialApp.router(routerConfig: router));
    await tester.pumpAndSettle();

    expect(find.text('Lịch sử AI Triage'), findsOneWidget);
    expect(find.text('Đang mang thai'), findsOneWidget);
    expect(find.text('Kết quả Vàng'), findsOneWidget);
    expect(find.textContaining('YELLOW'), findsNothing);

    await tester.tap(find.byKey(const ValueKey('triage-history-$sessionId')));
    await tester.pumpAndSettle();

    expect(find.text('result $sessionId'), findsOneWidget);
  });

  testWidgets('shows an empty state before the first completed check', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: TriageHistoryScreen(historyLoader: () async => const []),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Chưa có lịch sử kiểm tra'), findsOneWidget);
    expect(
      find.text('Các kết quả AI Triage sau khi hoàn tất sẽ được lưu tại đây.'),
      findsOneWidget,
    );
  });
}
