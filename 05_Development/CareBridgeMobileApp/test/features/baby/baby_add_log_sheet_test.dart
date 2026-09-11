import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/baby/models/baby_daily_log_model.dart';
import 'package:untitled/features/baby/models/baby_model.dart';
import 'package:untitled/features/baby/screens/baby_log_summary_screen.dart';
import 'package:untitled/features/baby/services/baby_log_service.dart';
import 'package:untitled/features/baby/services/baby_service.dart';

class _FakeBabyService extends BabyService {
  _FakeBabyService(this.profile);
  final BabyProfile profile;

  @override
  Future<List<BabyProfile>> listBabyProfiles() async => [profile];
}

class _MockLogService extends BabyLogService {
  AddBabyDailyLogRequest? lastAddRequest;

  @override
  Future<BabyLogSummaryResponse> getLogSummary(
    String babyId, {
    String period = '24h',
  }) async => BabyLogSummaryResponse(
    babyId: babyId,
    period: period,
    summaries: const {},
  );

  @override
  Future<List<BabyDailyLog>> getDailyLogs(String babyId) async => [];

  @override
  Future<BabyDailyLog> addDailyLog(
    String babyId,
    AddBabyDailyLogRequest request,
  ) async {
    lastAddRequest = request;
    return BabyDailyLog(
      id: 'new-log-1',
      babyId: babyId,
      logType: request.logType,
      quantity: request.quantity,
      unit: request.unit,
      note: request.note,
      startedAt: request.startedAt,
    );
  }
}

void main() {
  late _MockLogService mockLogService;
  late _FakeBabyService fakeBabyService;

  setUp(() {
    mockLogService = _MockLogService();
    fakeBabyService = _FakeBabyService(
      BabyProfile(
        id: 'baby-1',
        nickname: 'Bé Bông',
        birthDate: DateTime(2026, 1, 1),
        gender: BabyGender.female,
        isActive: true,
      ),
    );
  });

  Widget buildTestWidget() {
    return MaterialApp(
      home: BabyLogSummaryScreen(
        babyId: 'baby-1',
        logService: mockLogService,
        babyService: fakeBabyService,
      ),
    );
  }

  group('AddBabyLogSheet tests', () {
    testWidgets(
      'excludes fever and vomiting from log type selection and includes standard types',
      (tester) async {
        await tester.pumpWidget(buildTestWidget());
        await tester.pumpAndSettle();

        // Tap FAB to open Add Log sheet
        await tester.tap(find.byKey(const Key('baby-log-add')));
        await tester.pumpAndSettle();

        expect(find.text('Thêm nhật ký'), findsOneWidget);

        // Tap log type dropdown
        await tester.tap(find.byKey(const Key('baby-log-type-select')));
        await tester.pumpAndSettle();

        // Check options: Sốt and Nôn trớ must NOT be in the log type dropdown
        expect(find.text('Cho bé ăn'), findsWidgets);
        expect(find.text('Ngủ nghỉ'), findsOneWidget);
        expect(find.text('Thay tã'), findsOneWidget);
        expect(find.text('Sức khỏe'), findsOneWidget);
        expect(find.text('Thuốc'), findsOneWidget);

        // Fever ('Sốt') and Vomiting ('Nôn trớ') should NOT be standalone log types
        // In the dropdown menu popup:
        final allDropdownItems = tester.widgetList<DropdownMenuItem<LogType>>(
          find.byType(DropdownMenuItem<LogType>),
        );
        final values = allDropdownItems.map((item) => item.value).toSet();
        expect(values.contains(LogType.fever), isFalse);
        expect(values.contains(LogType.vomiting), isFalse);
        expect(values.contains(LogType.symptom), isTrue);
        expect(values.contains(LogType.feeding), isTrue);
        expect(values.contains(LogType.sleep), isTrue);
        expect(values.contains(LogType.diaper), isTrue);
        expect(values.contains(LogType.medicine), isTrue);
      },
    );

    testWidgets(
      'suggests default unit and allows editing unit for feeding, sleep, diaper, medicine',
      (tester) async {
        await tester.pumpWidget(buildTestWidget());
        await tester.pumpAndSettle();

        await tester.tap(find.byKey(const Key('baby-log-add')));
        await tester.pumpAndSettle();

        // 1. Feeding (default): suggested unit is ml and field is editable
        final unitFieldFinder = find.byKey(const Key('baby-log-unit'));
        expect(unitFieldFinder, findsOneWidget);
        expect(find.text('ml'), findsOneWidget);

        // Verify editable by entering custom unit
        await tester.enterText(unitFieldFinder, 'oz');
        expect(find.text('oz'), findsOneWidget);

        // 2. Switch to Sleep -> suggested unit is 'giờ'
        await tester.tap(find.byKey(const Key('baby-log-type-select')));
        await tester.pumpAndSettle();
        await tester.tap(find.text('Ngủ nghỉ').last);
        await tester.pumpAndSettle();

        expect(find.text('giờ'), findsOneWidget);

        // 3. Switch to Diaper -> suggested unit is 'lần'
        await tester.tap(find.byKey(const Key('baby-log-type-select')));
        await tester.pumpAndSettle();
        await tester.tap(find.text('Thay tã').last);
        await tester.pumpAndSettle();

        expect(find.text('lần'), findsOneWidget);

        // 4. Switch to Medicine -> suggested unit is 'liều'
        await tester.tap(find.byKey(const Key('baby-log-type-select')));
        await tester.pumpAndSettle();
        await tester.tap(find.text('Thuốc').last);
        await tester.pumpAndSettle();

        expect(find.text('liều'), findsOneWidget);
      },
    );

    testWidgets(
      'switching to health (Sức khỏe) replaces quantity/unit with symptom select and description',
      (tester) async {
        await tester.pumpWidget(buildTestWidget());
        await tester.pumpAndSettle();

        await tester.tap(find.byKey(const Key('baby-log-add')));
        await tester.pumpAndSettle();

        // Switch to Sức khỏe
        await tester.tap(find.byKey(const Key('baby-log-type-select')));
        await tester.pumpAndSettle();
        await tester.tap(find.text('Sức khỏe').last);
        await tester.pumpAndSettle();

        // Quantity & Unit fields must be hidden
        expect(find.byKey(const Key('baby-log-quantity')), findsNothing);
        expect(find.byKey(const Key('baby-log-unit')), findsNothing);

        // Symptom select and description fields must be visible
        expect(find.byKey(const Key('baby-log-symptom-select')), findsOneWidget);
        expect(
          find.byKey(const Key('baby-log-symptom-description')),
          findsOneWidget,
        );

        // Check that symptoms dropdown contains Sốt, Nôn trớ, Ho, etc. and Khác
        await tester.tap(find.byKey(const Key('baby-log-symptom-select')));
        await tester.pumpAndSettle();

        expect(find.text('Sốt'), findsWidgets);
        expect(find.text('Nôn trớ'), findsWidgets);
        expect(find.text('Ho'), findsWidgets);
        expect(find.text('Sổ mũi / Nghẹt mũi'), findsWidgets);
        expect(find.text('Tiêu chảy'), findsWidgets);
        expect(find.text('Táo bón'), findsWidgets);
        expect(find.text('Phát ban / Nổi mẩn'), findsWidgets);
        expect(find.text('Quấy khóc / Khó chịu'), findsWidgets);
        expect(find.text('Bỏ bú / Biếng ăn'), findsWidgets);
        expect(find.text('Khác'), findsWidgets);
      },
    );

    testWidgets(
      'selecting "Khác" requires description, and submits formatted symptom note',
      (tester) async {
        await tester.pumpWidget(buildTestWidget());
        await tester.pumpAndSettle();

        await tester.tap(find.byKey(const Key('baby-log-add')));
        await tester.pumpAndSettle();

        // Switch to Sức khỏe
        await tester.tap(find.byKey(const Key('baby-log-type-select')));
        await tester.pumpAndSettle();
        await tester.tap(find.text('Sức khỏe').last);
        await tester.pumpAndSettle();

        // Select 'Khác'
        await tester.tap(find.byKey(const Key('baby-log-symptom-select')));
        await tester.pumpAndSettle();
        await tester.tap(find.text('Khác').last);
        await tester.pumpAndSettle();

        // Description label updates to indicate required
        expect(find.text('Mô tả triệu chứng *'), findsOneWidget);

        // Try submitting empty description
        await tester.tap(find.byKey(const Key('baby-log-save')));
        await tester.pumpAndSettle();

        expect(
          find.text('Vui lòng nhập mô tả cho triệu chứng này'),
          findsOneWidget,
        );
        expect(mockLogService.lastAddRequest, isNull);

        // Enter description and submit
        await tester.enterText(
          find.byKey(const Key('baby-log-symptom-description')),
          'Bé bị mắt đỏ và chảy nước mắt',
        );
        await tester.tap(find.byKey(const Key('baby-log-save')));
        await tester.pumpAndSettle();

        expect(mockLogService.lastAddRequest, isNotNull);
        expect(mockLogService.lastAddRequest!.logType, LogType.symptom);
        expect(mockLogService.lastAddRequest!.quantity, isNull);
        expect(mockLogService.lastAddRequest!.unit, isNull);
        expect(
          mockLogService.lastAddRequest!.note,
          'Khác: Bé bị mắt đỏ và chảy nước mắt',
        );
      },
    );

    testWidgets(
      'submitting standard symptom "Sốt" with extra description sends merged note',
      (tester) async {
        await tester.pumpWidget(buildTestWidget());
        await tester.pumpAndSettle();

        await tester.tap(find.byKey(const Key('baby-log-add')));
        await tester.pumpAndSettle();

        // Switch to Sức khỏe (default symptom is 'Sốt')
        await tester.tap(find.byKey(const Key('baby-log-type-select')));
        await tester.pumpAndSettle();
        await tester.tap(find.text('Sức khỏe').last);
        await tester.pumpAndSettle();

        await tester.enterText(
          find.byKey(const Key('baby-log-symptom-description')),
          'Đo được 38.5 độ lúc chiều',
        );
        await tester.tap(find.byKey(const Key('baby-log-save')));
        await tester.pumpAndSettle();

        expect(mockLogService.lastAddRequest, isNotNull);
        expect(mockLogService.lastAddRequest!.logType, LogType.symptom);
        expect(mockLogService.lastAddRequest!.quantity, isNull);
        expect(mockLogService.lastAddRequest!.unit, isNull);
        expect(
          mockLogService.lastAddRequest!.note,
          'Sốt: Đo được 38.5 độ lúc chiều',
        );
      },
    );

    testWidgets('submitting feeding sends quantity and user-edited unit', (
      tester,
    ) async {
      await tester.pumpWidget(buildTestWidget());
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const Key('baby-log-add')));
      await tester.pumpAndSettle();

      // Feeding is default
      await tester.enterText(find.byKey(const Key('baby-log-quantity')), '150');
      // Change unit to oz
      await tester.enterText(find.byKey(const Key('baby-log-unit')), 'oz');
      await tester.enterText(
        find.byKey(const Key('baby-log-note')),
        'Bú bình xong ngủ ngon',
      );

      await tester.tap(find.byKey(const Key('baby-log-save')));
      await tester.pumpAndSettle();

      expect(mockLogService.lastAddRequest, isNotNull);
      expect(mockLogService.lastAddRequest!.logType, LogType.feeding);
      expect(mockLogService.lastAddRequest!.quantity, 150.0);
      expect(mockLogService.lastAddRequest!.unit, 'oz');
      expect(mockLogService.lastAddRequest!.note, 'Bú bình xong ngủ ngon');
    });
  });
}
