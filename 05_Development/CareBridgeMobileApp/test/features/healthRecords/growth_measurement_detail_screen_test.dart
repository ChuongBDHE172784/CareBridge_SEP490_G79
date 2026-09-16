import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/healthRecords/models/growth_measurement_model.dart';
import 'package:untitled/features/healthRecords/screens/growth_measurement_detail_screen.dart';

void main() {
  testWidgets('GrowthMeasurementDetailScreen shows Chi tiết đo lường in AppBar and removes CareBridge and avatar', (tester) async {
    final measurement = GrowthMeasurement(
      id: 'growth-1',
      measuredAt: DateTime(2026, 9, 14),
      heightCm: 65.5,
      weightKg: 7.2,
      headCircumferenceCm: 42.0,
      recordedBy: 'mother-1',
      recorderName: 'Mẹ Lan',
      sourceType: 'Bác sĩ',
      note: 'Bé tăng trưởng đều',
      createdAt: DateTime(2026, 9, 14, 15, 45),
    );

    await tester.pumpWidget(
      MaterialApp(
        home: GrowthMeasurementDetailScreen(
          babyId: 'baby-1',
          measurement: measurement,
        ),
      ),
    );

    // Verify AppBar
    expect(find.text('Chi tiết đo lường'), findsOneWidget);
    expect(find.text('CareBridge'), findsNothing);
    expect(find.byType(CircleAvatar), findsNothing);

    // Verify time is formatted from createdAt (15:45) and not stuck at 00:00
    expect(find.text('15:45'), findsOneWidget);
    expect(find.text('00:00'), findsNothing);

    // Verify metrics
    expect(find.text('65.5'), findsWidgets);
    expect(find.text('7.2 kg'), findsOneWidget);
    expect(find.text('42.0 cm'), findsOneWidget);
    expect(find.text('Bé tăng trưởng đều'), findsOneWidget);
    expect(find.text('NGUỒN ĐO'), findsNothing);
    expect(find.text('NGƯỜI GHI NHẬN'), findsNothing);
  });

  testWidgets('GrowthMeasurementDetailScreen displays --:-- when neither measuredAt has time nor createdAt is present', (tester) async {
    final measurement = GrowthMeasurement(
      id: 'growth-2',
      measuredAt: DateTime(2026, 9, 14, 0, 0),
      heightCm: 60.0,
      recordedBy: 'mother-1',
    );

    await tester.pumpWidget(
      MaterialApp(
        home: GrowthMeasurementDetailScreen(
          babyId: 'baby-1',
          measurement: measurement,
        ),
      ),
    );

    expect(find.text('Chi tiết đo lường'), findsOneWidget);
    expect(find.text('--:--'), findsOneWidget);
    expect(find.text('00:00'), findsNothing);
  });
}
