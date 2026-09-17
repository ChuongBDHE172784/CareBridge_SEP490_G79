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

    testWidgets('renders baby care checklists and sends baby checklist data when stage is BABY_CARE', (
      tester,
    ) async {
      tester.view.devicePixelRatio = 1.0;
      tester.view.physicalSize = const Size(800, 1200);
      addTearDown(() {
        tester.view.resetPhysicalSize();
        tester.view.resetDevicePixelRatio();
      });

      ChecklistShareData? result;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Builder(
              builder: (context) => ElevatedButton(
                onPressed: () async {
                  result = await ShareChecklistDialog.show(
                    context,
                    initialStage: 'BABY_CARE',
                    initialBabyName: 'Bé Bơ',
                  );
                },
                child: const Text('Open Baby Dialog'),
              ),
            ),
          ),
        ),
      );

      await tester.tap(find.text('Open Baby Dialog'));
      await tester.pumpAndSettle();

      // Verify stage label
      expect(find.textContaining('Chăm sóc bé'), findsAtLeastNWidgets(1));

      // Verify baby care items rendered with baby badge
      expect(find.textContaining('Dành cho bé'), findsWidgets);
      expect(find.text('Khám và theo dõi sơ sinh'), findsOneWidget);

      // Tap send
      final sendBtn = find.byKey(const Key('share-all-checklist-btn'));
      await tester.tap(sendBtn);
      await tester.pumpAndSettle();

      expect(result, isNotNull);
      expect(result!.stage, 'BABY_CARE');
      expect(result!.title, contains('bé'));
      expect(
        result!.currentItems.any((i) => i.text == 'Khám và theo dõi sơ sinh'),
        isTrue,
      );
    });

    testWidgets('renders mother and baby items with target filter chips for postpartum with baby', (
      tester,
    ) async {
      tester.view.devicePixelRatio = 1.0;
      tester.view.physicalSize = const Size(800, 1200);
      addTearDown(() {
        tester.view.resetPhysicalSize();
        tester.view.resetDevicePixelRatio();
      });

      ChecklistShareData? result;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Builder(
              builder: (context) => ElevatedButton(
                onPressed: () async {
                  result = await ShareChecklistDialog.show(
                    context,
                    initialStage: 'POSTPARTUM',
                    initialBabyName: 'Bé Bơ',
                  );
                },
                child: const Text('Open Postpartum Dialog'),
              ),
            ),
          ),
        ),
      );

      await tester.tap(find.text('Open Postpartum Dialog'));
      await tester.pumpAndSettle();

      // Verify both mother and baby items exist -> target chips shown
      expect(find.textContaining('Của mẹ'), findsAtLeastNWidgets(1));
      expect(find.textContaining('Dành cho bé'), findsAtLeastNWidgets(1));

      // Filter to only baby items
      final babyChip = find.textContaining('Dành cho bé').first;
      await tester.tap(babyChip);
      await tester.pumpAndSettle();

      // Baby item is shown
      expect(find.text('Khám và theo dõi sơ sinh'), findsOneWidget);

      // Filter to only mother items
      final motherChip = find.textContaining('Của mẹ').first;
      await tester.tap(motherChip);
      await tester.pumpAndSettle();

      // Baby item is filtered out
      expect(find.text('Khám và theo dõi sơ sinh'), findsNothing);

      // Switch back to all and send
      final allChip = find.textContaining('Tất cả').first;
      await tester.tap(allChip);
      await tester.pumpAndSettle();

      final sendBtn = find.byKey(const Key('share-all-checklist-btn'));
      await tester.tap(sendBtn);
      await tester.pumpAndSettle();

      expect(result, isNotNull);
      expect(result!.title, 'Danh sách việc cần làm (Mẹ & Bé)');
      expect(
        result!.currentItems.any((i) => i.text == 'Khám và theo dõi sơ sinh'),
        isTrue,
      );
    });

    testWidgets('sends only mother items when "Của mẹ" target is selected', (
      tester,
    ) async {
      tester.view.devicePixelRatio = 1.0;
      tester.view.physicalSize = const Size(800, 1200);
      addTearDown(() {
        tester.view.resetPhysicalSize();
        tester.view.resetDevicePixelRatio();
      });

      ChecklistShareData? result;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Builder(
              builder: (context) => ElevatedButton(
                onPressed: () async {
                  result = await ShareChecklistDialog.show(
                    context,
                    initialStage: 'POSTPARTUM',
                    initialBabyName: 'Bé Bơ',
                  );
                },
                child: const Text('Open Postpartum Dialog'),
              ),
            ),
          ),
        ),
      );

      await tester.tap(find.text('Open Postpartum Dialog'));
      await tester.pumpAndSettle();

      // Tap on "Của mẹ" target chip
      final motherChip = find.textContaining('Của mẹ').first;
      await tester.tap(motherChip);
      await tester.pumpAndSettle();

      // Verify send button text updated to Mother checklist
      expect(find.textContaining('Chia sẻ việc của mẹ'), findsOneWidget);

      // Tap send button
      final sendBtn = find.byKey(const Key('share-all-checklist-btn'));
      await tester.tap(sendBtn);
      await tester.pumpAndSettle();

      expect(result, isNotNull);
      expect(result!.title, 'Lộ trình chăm sóc sau sinh');
      // No baby item should be in currentItems
      expect(
        result!.currentItems.any((i) => i.text == 'Khám và theo dõi sơ sinh'),
        isFalse,
      );
      // Only mother items exist
      expect(
        result!.currentItems.every((i) => !(i.category?.contains('bé') ?? false)),
        isTrue,
      );
    });

    testWidgets('sends only baby items when "Dành cho bé" target is selected', (
      tester,
    ) async {
      tester.view.devicePixelRatio = 1.0;
      tester.view.physicalSize = const Size(800, 1200);
      addTearDown(() {
        tester.view.resetPhysicalSize();
        tester.view.resetDevicePixelRatio();
      });

      ChecklistShareData? result;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Builder(
              builder: (context) => ElevatedButton(
                onPressed: () async {
                  result = await ShareChecklistDialog.show(
                    context,
                    initialStage: 'POSTPARTUM',
                    initialBabyName: 'Bé Bơ',
                  );
                },
                child: const Text('Open Postpartum Dialog'),
              ),
            ),
          ),
        ),
      );

      await tester.tap(find.text('Open Postpartum Dialog'));
      await tester.pumpAndSettle();

      // Tap on "Dành cho bé" target chip
      final babyChip = find.textContaining('Dành cho bé').first;
      await tester.tap(babyChip);
      await tester.pumpAndSettle();

      // Verify send button text updated to Baby checklist
      expect(find.textContaining('Chia sẻ việc chăm sóc bé'), findsOneWidget);

      // Tap send button
      final sendBtn = find.byKey(const Key('share-all-checklist-btn'));
      await tester.tap(sendBtn);
      await tester.pumpAndSettle();

      expect(result, isNotNull);
      expect(result!.title, 'Danh sách việc chăm sóc bé');
      expect(result!.stage, 'BABY_CARE');
      expect(result!.stageLabel, 'Chăm sóc bé');
      // Baby item is present
      expect(
        result!.currentItems.any((i) => i.text == 'Khám và theo dõi sơ sinh'),
        isTrue,
      );
    });

    testWidgets('pregnant mother at week 13 with baby profile retains current pregnancy tasks under "Của mẹ"', (
      tester,
    ) async {
      tester.view.devicePixelRatio = 1.0;
      tester.view.physicalSize = const Size(800, 1200);
      addTearDown(() {
        tester.view.resetPhysicalSize();
        tester.view.resetDevicePixelRatio();
      });

      ChecklistShareData? result;

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Builder(
              builder: (context) => ElevatedButton(
                onPressed: () async {
                  result = await ShareChecklistDialog.show(
                    context,
                    initialStage: 'PREGNANCY',
                    initialGestationalWeek: 13,
                    initialBabyName: 'Bé Miu',
                  );
                },
                child: const Text('Open Pregnancy Dialog'),
              ),
            ),
          ),
        ),
      );

      await tester.tap(find.text('Open Pregnancy Dialog'));
      await tester.pumpAndSettle();

      // Tap on "Của mẹ" target chip
      final motherChip = find.textContaining('Của mẹ').first;
      await tester.tap(motherChip);
      await tester.pumpAndSettle();

      // Verify "Hiện tại" tab has tasks (e.g. Hiện tại (8)), NOT Hiện tại (0)
      expect(find.text('Hiện tại (0)'), findsNothing);

      // Verify pregnancy task is displayed
      expect(find.text('Đi khám thai lần đầu'), findsOneWidget);

      // Tap send
      final sendBtn = find.byKey(const Key('share-all-checklist-btn'));
      await tester.tap(sendBtn);
      await tester.pumpAndSettle();

      expect(result, isNotNull);
      expect(result!.currentItems.isNotEmpty, isTrue);
      expect(
        result!.currentItems.any((i) => i.text == 'Đi khám thai lần đầu'),
        isTrue,
      );
      // All items in currentItems should be mother tasks, not baby tasks
      expect(
        result!.currentItems.every((i) => !(i.category?.contains('bé') ?? false)),
        isTrue,
      );
    });
  });

  group('ChecklistMessageCard Tests - Baby Badge Rendering', () {
    testWidgets('renders baby badge on baby items in message card', (
      tester,
    ) async {
      tester.view.devicePixelRatio = 1.0;
      tester.view.physicalSize = const Size(800, 1200);
      addTearDown(() {
        tester.view.resetPhysicalSize();
        tester.view.resetDevicePixelRatio();
      });

      final data = ChecklistShareData(
        title: 'Danh sách việc cần làm (Mẹ & Bé)',
        stage: 'POSTPARTUM',
        stageLabel: 'Sau sinh & Chăm bé',
        completedCount: 1,
        totalCount: 2,
        progressPercent: 50,
        currentItems: const [
          ChecklistItemShareData(
            text: 'Đánh giá tâm trạng & sàng lọc trầm cảm sau sinh',
            completed: true,
            category: 'Chăm sóc sau sinh',
            timeLabel: 'Sau sinh',
          ),
          ChecklistItemShareData(
            text: 'Khám và theo dõi sơ sinh',
            completed: false,
            category: 'Chăm sóc bé',
            timeLabel: 'Bé · Sơ sinh',
          ),
        ],
      );

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ChecklistMessageCard(
              data: data,
              isOwnMessage: true,
            ),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // Verify card renders title
      expect(find.text('Danh sách việc cần làm (Mẹ & Bé)'), findsOneWidget);

      // Verify baby badge on message card preview
      expect(find.text('👶 Cho bé'), findsOneWidget);
      expect(find.text('Khám và theo dõi sơ sinh'), findsOneWidget);

      // Tap on 'Xem Lịch sử & Tương lai' to open full detail modal
      await tester.tap(find.text('Xem Lịch sử & Tương lai'));
      await tester.pumpAndSettle();

      // In detail modal, verify baby badge is displayed
      expect(find.text('Dành cho bé'), findsOneWidget);
      expect(find.text('Khám và theo dõi sơ sinh'), findsWidgets);
    });
  });
}
