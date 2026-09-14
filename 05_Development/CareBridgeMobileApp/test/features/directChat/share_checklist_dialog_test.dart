import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/directChat/widgets/share_checklist_dialog.dart';
import 'package:untitled/features/directChat/widgets/checklist_message_card.dart';

void main() {
  group('ShareChecklistDialog Tests - Default Send All Without Selection', () {
    testWidgets('renders dialog and send button sends all items by default without checkboxes', (
      tester,
    ) async {
      ChecklistShareData? result;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Builder(
              builder: (context) => ElevatedButton(
                onPressed: () async {
                  result = await ShareChecklistDialog.show(context);
                },
                child: const Text('Open Dialog'),
              ),
            ),
          ),
        ),
      );

      // Open dialog
      await tester.tap(find.text('Open Dialog'));
      await tester.pumpAndSettle();

      // Verify title & default send all scope banner
      expect(find.text('Chia sẻ việc cần làm'), findsOneWidget);
      expect(find.textContaining('Mặc định gửi toàn bộ'), findsAtLeastNWidgets(1));

      // Verify no checkboxes exist for item selection
      expect(find.byType(Checkbox), findsNothing);
      expect(find.byType(CheckboxListTile), findsNothing);

      // Verify "Bỏ chọn hết" or "Chọn tất cả" action bar is removed
      expect(find.text('Bỏ chọn hết'), findsNothing);
      expect(find.text('Chọn tất cả'), findsNothing);

      // Verify TabBar exists and has 3 tabs
      expect(find.byType(TabBar), findsOneWidget);
      expect(find.byType(Tab), findsNWidgets(3));

      // Tap send button
      final sendBtn = find.byKey(const Key('share-all-checklist-btn'));
      expect(sendBtn, findsOneWidget);
      await tester.tap(sendBtn);
      await tester.pumpAndSettle();

      // Verify dialog popped
      expect(find.text('Chia sẻ việc cần làm'), findsNothing);
      expect(result, isNotNull);
      expect(result!.totalCount, greaterThan(0));
    });
  });
}
