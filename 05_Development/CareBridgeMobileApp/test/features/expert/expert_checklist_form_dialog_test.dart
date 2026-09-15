import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:untitled/features/directChat/widgets/checklist_message_card.dart';
import 'package:untitled/features/expert/widgets/expert_checklist_form_dialog.dart';

void main() {
  testWidgets('ExpertChecklistFormDialog renders in ADD mode and validates empty input', (tester) async {
    tester.view.physicalSize = const Size(1080, 2400);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: Builder(
            builder: (context) => ElevatedButton(
              onPressed: () => ExpertChecklistFormDialog.show(
                context,
                mode: ExpertChecklistFormMode.add,
                initialTargetGroup: 'CURRENT',
                onSave: ({
                  required text,
                  required targetGroup,
                  required category,
                  required timeLabel,
                  required doctorNote,
                  required supportFunction,
                  required completed,
                  required sourceUrl,
                }) async {},
              ),
              child: const Text('Open Dialog'),
            ),
          ),
        ),
      ),
    );

    // Open dialog
    await tester.tap(find.text('Open Dialog'));
    await tester.pumpAndSettle();

    expect(find.text('Bác sĩ chỉ định việc cần làm'), findsOneWidget);
    expect(find.text('Tên công việc / Hướng dẫn y khoa *'), findsOneWidget);

    // Tap Save without entering text
    await tester.tap(find.text('Thêm vào lộ trình'));
    await tester.pumpAndSettle();

    // Error message appears
    expect(find.text('Vui lòng nhập tên công việc cần làm.'), findsOneWidget);
  });

  testWidgets('ExpertChecklistFormDialog submits valid input in ADD mode', (tester) async {
    tester.view.physicalSize = const Size(1080, 2400);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });

    String? savedText;
    String? savedNote;
    String? savedGroup;

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: Builder(
            builder: (context) => ElevatedButton(
              onPressed: () => ExpertChecklistFormDialog.show(
                context,
                mode: ExpertChecklistFormMode.add,
                initialTargetGroup: 'CURRENT',
                onSave: ({
                  required text,
                  required targetGroup,
                  required category,
                  required timeLabel,
                  required doctorNote,
                  required supportFunction,
                  required completed,
                  required sourceUrl,
                }) async {
                  savedText = text;
                  savedNote = doctorNote;
                  savedGroup = targetGroup;
                },
              ),
              child: const Text('Open Dialog'),
            ),
          ),
        ),
      ),
    );

    await tester.tap(find.text('Open Dialog'));
    await tester.pumpAndSettle();

    // Enter item text in the first TextField
    await tester.enterText(find.byType(TextField).at(0), 'Xét nghiệm NIPT');

    // Enter doctor note in the 3rd TextField
    await tester.enterText(find.byType(TextField).at(2), 'Làm ở tuần 10 trở đi');

    await tester.pumpAndSettle();

    // Save
    await tester.tap(find.text('Thêm vào lộ trình'));
    await tester.pumpAndSettle();

    expect(savedText, 'Xét nghiệm NIPT');
    expect(savedNote, 'Làm ở tuần 10 trở đi');
    expect(savedGroup, 'CURRENT');
  });

  testWidgets('ExpertChecklistFormDialog renders in EDIT mode with initial values', (tester) async {
    tester.view.physicalSize = const Size(1080, 2400);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });

    const initialItem = ChecklistItemShareData(
      text: 'Tiêm uốn ván mũi 1',
      completed: true,
      category: 'Tiêm chủng & Thuốc',
      timeLabel: 'Tuần 22',
      doctorNote: 'Tiêm tại trạm y tế phường',
      supportFunction: 'APPOINTMENTS',
    );

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: Builder(
            builder: (context) => ElevatedButton(
              onPressed: () => ExpertChecklistFormDialog.show(
                context,
                mode: ExpertChecklistFormMode.edit,
                initialItem: initialItem,
                initialTargetGroup: 'FUTURE',
                onSave: ({
                  required text,
                  required targetGroup,
                  required category,
                  required timeLabel,
                  required doctorNote,
                  required supportFunction,
                  required completed,
                  required sourceUrl,
                }) async {},
              ),
              child: const Text('Open Dialog'),
            ),
          ),
        ),
      ),
    );

    await tester.tap(find.text('Open Dialog'));
    await tester.pumpAndSettle();

    expect(find.text('Chỉnh sửa chỉ định của Bác sĩ'), findsOneWidget);
    expect(tester.widget<TextField>(find.byType(TextField).at(0)).controller?.text, 'Tiêm uốn ván mũi 1');
    expect(tester.widget<TextField>(find.byType(TextField).at(2)).controller?.text, 'Tiêm tại trạm y tế phường');
    expect(find.text('Lưu thay đổi'), findsOneWidget);
  });
}
