import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/checklist/screens/checklist_detail_screen.dart';
import 'package:untitled/features/checklist/services/user_checklist_service.dart';
import 'package:untitled/features/community/models/content_model.dart';

void main() {
  late ChecklistTemplate testTemplate;

  setUp(() {
    testTemplate = ChecklistTemplate(
      id: 'tmpl-101',
      name: 'Checklist Chuẩn bị Đi sinh',
      stage: 'PREGNANCY',
      description: 'Danh sách các vật dụng và giấy tờ cần mang khi đi sinh.',
      templateType: 'MANDATORY',
      items: [
        ChecklistItem(
          id: 'item-1',
          itemText: 'Giấy tờ tùy thân và sổ khám thai',
          order: 1,
          isRequired: true,
        ),
        ChecklistItem(
          id: 'item-2',
          itemText: 'Quần áo sơ sinh cho bé',
          order: 2,
          isRequired: false,
        ),
      ],
    );
  });

  testWidgets('renders checklist template detail correctly', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: ChecklistDetailScreen(
          template: testTemplate,
          journeyId: 'journey-123',
        ),
      ),
    );

    expect(find.text('Chi tiết Checklist'), findsOneWidget);
    expect(find.text('Checklist Chuẩn bị Đi sinh'), findsOneWidget);
    expect(
      find.text('Danh sách các vật dụng và giấy tờ cần mang khi đi sinh.'),
      findsOneWidget,
    );
    expect(find.text('Giấy tờ tùy thân và sổ khám thai'), findsOneWidget);
    expect(find.text('Quần áo sơ sinh cho bé'), findsOneWidget);
    expect(find.text('Bắt buộc'), findsOneWidget);
    expect(find.text('Thêm vào danh sách việc cần làm'), findsOneWidget);
  });

  testWidgets('triggers addTemplate on action button tap', (
    WidgetTester tester,
  ) async {
    bool addTemplateCalled = false;
    final mockService = UserChecklistService(
      postRequest: (path, body) async {
        if (path == '/api/v1/user-checklist-items/from-template') {
          addTemplateCalled = true;
          return {
            'success': true,
            'data': {
              'createdTasks': 2,
              'existingTasks': 0,
              'deniedRecipients': 0,
              'conflicts': 0,
              'failures': 0,
            },
          };
        }
        return {};
      },
    );

    await tester.pumpWidget(
      MaterialApp(
        home: ChecklistDetailScreen(
          template: testTemplate,
          journeyId: 'journey-123',
          userChecklistService: mockService,
        ),
      ),
    );

    final button = find.text('Thêm vào danh sách việc cần làm');
    expect(button, findsOneWidget);
    await tester.tap(button);
    await tester.pumpAndSettle();

    expect(addTemplateCalled, isTrue);
    expect(
      find.text('Đã thêm 2 việc vào danh sách công việc!'),
      findsOneWidget,
    );
  });

  testWidgets('renders V2 requiredness from each checklist item', (
    WidgetTester tester,
  ) async {
    final v2 = ChecklistTemplate(
      id: 'tmpl-v2',
      name: 'WHO Plan 2',
      stage: 'PREGNANCY',
      description: 'Mô tả khuyến nghị',
      templateType: 'MANDATORY',
      checklistContractVersion: 2,
      planNumber: 2,
      section: 'WEEKLY',
      scheduleType: 'WEEKLY',
      materializationPolicy: 'EACH_WEEK',
      eligibilityStartInclusive: 20,
      eligibilityEndInclusive: 24,
      items: [
        ChecklistItem(
          id: 'v2-item',
          itemText: 'Duy trì hành vi hằng ngày',
          order: 1,
          isRequired: true,
        ),
        ChecklistItem(
          id: 'v2-optional-item',
          itemText: 'Tham khảo thêm nội dung',
          order: 2,
          isRequired: false,
        ),
      ],
    );

    await tester.pumpWidget(
      MaterialApp(
        home: ChecklistDetailScreen(template: v2, journeyId: 'journey-123'),
      ),
    );

    expect(find.text('Mô tả khuyến nghị'), findsOneWidget);
    expect(find.text('Plan 2 · WEEKLY'), findsOneWidget);
    expect(find.text('Tuần 21–25'), findsOneWidget);
    expect(find.text('Theo tuần'), findsWidgets);
    expect(find.text('Cần thiết'), findsOneWidget);
    expect(find.text('Khuyến nghị'), findsNothing);
  });

  testWidgets(
    'shows warning and disables button if journey is required but missing',
    (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: ChecklistDetailScreen(
            template: testTemplate,
            journeyId: null,
            isLifecycleMode: false,
          ),
        ),
      );

      final finder = find.text(
        'Hãy thiết lập hành trình (Journey) trước khi thêm checklist.',
      );
      await tester.scrollUntilVisible(
        finder,
        50.0,
        scrollable: find.byType(Scrollable).first,
      );
      expect(finder, findsOneWidget);

      // The action bar uses ElevatedButton.icon, whose factory returns the private
      // _ElevatedButtonWithIcon subclass. find.byType matches the exact runtime type,
      // so it never sees that button — match on the supertype instead.
      final buttonWidget = tester.widget<ElevatedButton>(
        find.ancestor(
          of: find.text('Thêm vào danh sách việc cần làm'),
          matching: find.byWidgetPredicate(
            (widget) => widget is ElevatedButton,
          ),
        ),
      );
      expect(buttonWidget.onPressed, isNull);
    },
  );
}
