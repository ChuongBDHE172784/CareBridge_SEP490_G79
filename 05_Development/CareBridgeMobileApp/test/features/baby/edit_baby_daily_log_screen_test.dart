import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/baby/models/baby_daily_log_model.dart';
import 'package:untitled/features/baby/screens/edit_baby_daily_log_screen.dart';

void main() {
  group('EditBabyDailyLogScreen tests', () {
    testWidgets('Feeding log shows Lượng sữa (ml) and no end time', (tester) async {
      final log = BabyDailyLog(
        id: 'log-1',
        babyId: 'baby-1',
        logType: LogType.feeding,
        startedAt: DateTime(2026, 8, 5, 9, 30),
        quantity: 180,
        unit: 'ml',
        note: 'Bé bú ngoan',
      );

      await tester.pumpWidget(
        MaterialApp(
          home: EditBabyDailyLogScreen(
            babyId: 'baby-1',
            logId: 'log-1',
            initialLog: log,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Chỉnh sửa nhật ký'), findsOneWidget);
      expect(find.text('Cho bé ăn'), findsOneWidget);
      expect(find.text('Lượng sữa (ml)'), findsWidgets);
      expect(find.text('Thời gian bắt đầu'), findsOneWidget);
      expect(find.text('Thời gian kết thúc'), findsNothing);
      expect(find.text('Kết thúc'), findsNothing);
      expect(find.text('180'), findsOneWidget);
      expect(find.text('Bé bú ngoan'), findsOneWidget);
    });

    testWidgets('Sleep log shows sleep timer picker and no end time', (tester) async {
      final log = BabyDailyLog(
        id: 'log-2',
        babyId: 'baby-1',
        logType: LogType.sleep,
        startedAt: DateTime(2026, 8, 5, 12, 0),
        quantity: 90,
        unit: 'phút',
        note: 'Ngủ sâu giấc',
      );

      await tester.pumpWidget(
        MaterialApp(
          home: EditBabyDailyLogScreen(
            babyId: 'baby-1',
            logId: 'log-2',
            initialLog: log,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Ngủ nghỉ'), findsOneWidget);
      expect(find.text('Thời gian ngủ:'), findsOneWidget);
      expect(find.text('1 giờ 30 phút'), findsOneWidget);
      expect(find.text('Thời gian bắt đầu'), findsOneWidget);
      expect(find.text('Thời gian kết thúc'), findsNothing);
      expect(find.text('Kết thúc'), findsNothing);
    });

    testWidgets('Diaper log shows stepper counter and no unit field', (tester) async {
      final log = BabyDailyLog(
        id: 'log-3',
        babyId: 'baby-1',
        logType: LogType.diaper,
        startedAt: DateTime(2026, 8, 5, 14, 0),
        quantity: 2,
        unit: 'lần',
      );

      await tester.pumpWidget(
        MaterialApp(
          home: EditBabyDailyLogScreen(
            babyId: 'baby-1',
            logId: 'log-3',
            initialLog: log,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Thay tã'), findsOneWidget);
      expect(find.text('Số lần thay tã'), findsOneWidget);
      expect(find.text('2'), findsOneWidget);
      expect(find.byIcon(Icons.remove), findsOneWidget);
      expect(find.byIcon(Icons.add), findsOneWidget);
      expect(find.text('Thời gian kết thúc'), findsNothing);
    });

    testWidgets('Medicine log shows Liều lượng field and no unit field', (tester) async {
      final log = BabyDailyLog(
        id: 'log-4',
        babyId: 'baby-1',
        logType: LogType.medicine,
        startedAt: DateTime(2026, 8, 5, 16, 0),
        quantity: 1,
        unit: 'liều',
        note: 'Uống sau ăn',
      );

      await tester.pumpWidget(
        MaterialApp(
          home: EditBabyDailyLogScreen(
            babyId: 'baby-1',
            logId: 'log-4',
            initialLog: log,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Thuốc'), findsOneWidget);
      expect(find.text('Liều lượng'), findsWidgets);
      expect(find.text('1'), findsOneWidget);
      expect(find.text('Uống sau ăn'), findsOneWidget);
      expect(find.text('Thời gian kết thúc'), findsNothing);
    });

    testWidgets('Symptom log parses symptom note into dropdown and description', (tester) async {
      final log = BabyDailyLog(
        id: 'log-5',
        babyId: 'baby-1',
        logType: LogType.symptom,
        startedAt: DateTime(2026, 8, 5, 18, 0),
        note: 'Sốt: 38.5 độ C lúc chiều',
      );

      await tester.pumpWidget(
        MaterialApp(
          home: EditBabyDailyLogScreen(
            babyId: 'baby-1',
            logId: 'log-5',
            initialLog: log,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Sức khỏe'), findsOneWidget);
      expect(find.text('Sốt'), findsOneWidget);
      expect(find.text('38.5 độ C lúc chiều'), findsOneWidget);
      expect(find.text('Thời gian kết thúc'), findsNothing);
    });
  });
}
