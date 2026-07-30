import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/features/community/models/content_model.dart';
import 'package:untitled/features/community/screens/community_feed_screen.dart';
import 'package:untitled/features/community/screens/view_content_screen.dart';

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

  testWidgets('Family does not see Mother-only create-question actions', (
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
      findsNothing,
    );
    expect(find.text('Đặt câu hỏi'), findsNothing);

    await tester.tap(find.byIcon(Icons.verified_outlined));
    await tester.pumpAndSettle();

    final browse = tester.widget<ViewContentScreen>(
      find.byType(ViewContentScreen),
    );
    expect(browse.mode, ContentBrowseMode.family);
  });
}
