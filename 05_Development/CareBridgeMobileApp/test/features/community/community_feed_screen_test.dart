import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/features/community/screens/community_feed_screen.dart';
import 'package:untitled/features/community/screens/my_questions_screen.dart';

void main() {
  testWidgets(
    'CommunityFeedScreen renders title, search bar, and filter options',
    (WidgetTester tester) async {
      await tester.pumpWidget(const MaterialApp(home: CommunityFeedScreen()));

      // Initial pump to build layout
      await tester.pump();

      // Verify presence of main title
      expect(find.text('Cộng đồng'), findsOneWidget);

      // Verify search bar hint text
      expect(
        find.text('Tìm câu hỏi, chủ đề hoặc chuyên gia...'),
        findsOneWidget,
      );

      // Verify Dropdown filter headers
      expect(find.text('Chủ đề'), findsOneWidget);
      expect(find.text('Giai đoạn'), findsOneWidget);
      expect(find.text('Chuyên gia'), findsOneWidget);

      // The standalone topic directory was removed; question topic filtering remains.
      expect(find.byTooltip('Thư viện chủ đề'), findsNothing);
      expect(find.byTooltip('Bài viết đã lưu'), findsOneWidget);
    },
  );

  testWidgets('Family can create and manage own community questions', (
    WidgetTester tester,
  ) async {
    FlutterSecureStorage.setMockInitialValues({});
    await AuthState.instance.clear();
    await AuthState.instance.setTokens(
      accessToken: 'family-access',
      refreshToken: 'family-refresh',
      userId: 'family-user',
      role: 'FAMILY',
    );
    addTearDown(AuthState.instance.clear);

    await tester.pumpWidget(const MaterialApp(home: CommunityFeedScreen()));
    await tester.pump();

    expect(
      find.byKey(const Key('community-create-question-fab')),
      findsOneWidget,
    );
    expect(find.text('Đặt câu hỏi'), findsWidgets);
    expect(find.byTooltip('Câu hỏi của tôi'), findsOneWidget);

    await tester.tap(find.byTooltip('Câu hỏi của tôi'));
    await tester.pumpAndSettle();

    expect(find.byType(MyQuestionsScreen), findsOneWidget);
  });
}
