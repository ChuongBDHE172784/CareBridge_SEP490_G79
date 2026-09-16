import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/baby/models/baby_daily_log_model.dart';
import 'package:untitled/features/baby/screens/baby_daily_log_detail_screen.dart';

void main() {
  group('BabyDailyLogDetailScreen tests', () {
    testWidgets('Detail screen does not show Kết thúc in Thời gian and shows confirmation modal on delete', (tester) async {
      final log = BabyDailyLog(
        id: 'log-10',
        babyId: 'baby-1',
        logType: LogType.feeding,
        startedAt: DateTime(2026, 8, 5, 10, 0),
        quantity: 120,
        unit: 'ml',
        note: 'Bú tốt',
      );

      await tester.pumpWidget(
        MaterialApp(
          home: BabyDailyLogDetailScreen(
            babyId: 'baby-1',
            logId: 'log-10',
            initialLog: log,
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Ensure 'Kết thúc', 'Bắt đầu', and 'Tổng' are removed
      expect(find.text('KẾT THÚC'), findsNothing);
      expect(find.text('BẮT ĐẦU'), findsNothing);
      expect(find.text('Bắt đầu'), findsNothing);
      expect(find.text('Tổng'), findsNothing);
      expect(find.text('TỔNG'), findsNothing);
      expect(find.text('Thời gian'), findsOneWidget);

      // Scroll and Tap 'Xóa nhật ký'
      final deleteBtn = find.text('Xóa nhật ký');
      await tester.ensureVisible(deleteBtn);
      await tester.tap(deleteBtn);
      await tester.pumpAndSettle();

      // Ensure delete confirmation sheet appears
      expect(find.text('Xóa nhật ký này?'), findsOneWidget);
      expect(find.text('Hành động này không thể hoàn tác. Nhật ký sẽ bị xóa vĩnh viễn.'), findsOneWidget);
      expect(find.text('Xác nhận xóa'), findsOneWidget);
      expect(find.text('Hủy bỏ'), findsOneWidget);

      // Tap 'Hủy bỏ' cancels modal
      await tester.tap(find.text('Hủy bỏ'));
      await tester.pumpAndSettle();

      expect(find.text('Xóa nhật ký này?'), findsNothing);
    });
  });
}
