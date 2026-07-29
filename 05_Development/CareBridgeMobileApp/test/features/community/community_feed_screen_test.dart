import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/community/screens/community_feed_screen.dart';

void main() {
  testWidgets('CommunityFeedScreen renders title, search bar, and filter options', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: CommunityFeedScreen(),
      ),
    );

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
  });
}
