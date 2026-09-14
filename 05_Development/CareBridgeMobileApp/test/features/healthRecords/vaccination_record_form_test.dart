import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/healthRecords/models/vaccination_model.dart';
import 'package:untitled/features/healthRecords/screens/vaccination_detail_screen.dart';
import 'package:untitled/features/healthRecords/screens/vaccination_record_form_screen.dart';

void main() {
  final sampleRecord = VaccinationRecord(
    vaccinationId: 'vax-123',
    vaccineName: 'Phế cầu Synflorix',
    status: VaccinationStatus.scheduled,
    plannedDate: DateTime(2026, 9, 20),
    doseNumber: 2,
    babyId: 'baby-1',
    facilityName: 'Trung tâm tiêm chủng VNVC',
  );

  testWidgets('VaccinationDetailScreen does not show Dời lịch button', (tester) async {
    tester.view.physicalSize = const Size(400 * 2, 1200 * 2);
    tester.view.devicePixelRatio = 2.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      MaterialApp(
        home: VaccinationDetailScreen(
          babyId: 'baby-1',
          vaccinationId: 'vax-123',
          initialRecord: sampleRecord,
        ),
      ),
    );
    await tester.pumpAndSettle();

    // Verify vaccine title
    expect(find.text('Phế cầu Synflorix'), findsOneWidget);

    // Verify "Dời lịch" button does not exist
    expect(find.text('Dời lịch'), findsNothing);

    // Verify Action buttons
    expect(find.text('Cập nhật thông tin'), findsOneWidget);
    expect(find.text('Nhắc mũi tiếp theo'), findsOneWidget);
    expect(find.text('Xóa hồ sơ tiêm chủng'), findsOneWidget);
  });

  testWidgets('VaccinationRecordFormScreen renders synchronized UI cards in edit mode', (tester) async {
    tester.view.physicalSize = const Size(400 * 2, 1200 * 2);
    tester.view.devicePixelRatio = 2.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      MaterialApp(
        home: VaccinationRecordFormScreen(
          babyId: 'baby-1',
          recordId: 'vax-123',
          initialRecord: sampleRecord,
        ),
      ),
    );
    await tester.pumpAndSettle();

    // Verify AppBar Title for editing
    expect(find.text('Sửa hồ sơ tiêm chủng'), findsOneWidget);

    // Verify card titles
    expect(find.text('Thông tin vắc xin'), findsOneWidget);
    expect(find.text('Thời gian & Địa điểm'), findsOneWidget);

    // Verify prepopulated values
    expect(find.text('Phế cầu Synflorix'), findsOneWidget);
    expect(find.text('2'), findsOneWidget);
    expect(find.text('Trung tâm tiêm chủng VNVC'), findsOneWidget);

    // Verify Save Button text
    expect(find.text('Lưu thay đổi'), findsOneWidget);
  });

  testWidgets('VaccinationRecordFormScreen renders synchronized UI cards in add mode', (tester) async {
    tester.view.physicalSize = const Size(400 * 2, 1200 * 2);
    tester.view.devicePixelRatio = 2.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      const MaterialApp(
        home: VaccinationRecordFormScreen(
          babyId: 'baby-1',
        ),
      ),
    );
    await tester.pumpAndSettle();

    // Verify AppBar Title for adding
    expect(find.text('Thêm hồ sơ tiêm chủng'), findsOneWidget);

    // Verify Save Button text
    expect(find.text('Lưu hồ sơ tiêm'), findsOneWidget);
  });
}
