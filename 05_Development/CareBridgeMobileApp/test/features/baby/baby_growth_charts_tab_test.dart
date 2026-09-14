import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/baby/models/baby_model.dart';
import 'package:untitled/features/baby/screens/baby_profile_detail_screen.dart';
import 'package:untitled/features/healthRecords/models/growth_measurement_model.dart';

void main() {
  testWidgets('BabyProfileDetailScreen growth tab shows weight, height, and head circumference charts and no disclaimer', (tester) async {
    final profile = BabyProfile(
      id: 'baby-test-1',
      nickname: 'Bé Miu',
      birthDate: DateTime(2026, 6, 1),
      gender: BabyGender.female,
      isActive: true,
    );

    final measurements = [
      GrowthMeasurement(
        id: 'm1',
        measuredAt: DateTime(2026, 6, 1),
        weightKg: 3.2,
        heightCm: 50.0,
        headCircumferenceCm: 34.0,
        recordedBy: 'mother',
      ),
      GrowthMeasurement(
        id: 'm2',
        measuredAt: DateTime(2026, 7, 1),
        weightKg: 4.5,
        heightCm: 55.5,
        headCircumferenceCm: 36.8,
        recordedBy: 'mother',
      ),
    ];

    tester.view.physicalSize = const Size(400 * 2, 1200 * 2);
    tester.view.devicePixelRatio = 2.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: SingleChildScrollView(
            child: BabyProfileDetailScreen(
              babyId: profile.id,
              embedded: true,
              loadData: false,
              initialProfile: profile,
              initialGrowthMeasurements: measurements,
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    // Verify default tab is "Xu hướng cân nặng"
    expect(find.text('Xu hướng cân nặng'), findsOneWidget);
    expect(find.text('3.2 kg – 4.5 kg'), findsOneWidget);

    // Verify the disclaimer sentence is completely deleted
    expect(find.text('Dữ liệu đo lường được hiển thị theo nguồn đã ghi nhận.'), findsNothing);

    // Verify the 3 metric tabs exist
    expect(find.text('Cân nặng'), findsWidgets);
    expect(find.text('Chiều cao'), findsWidgets);
    expect(find.text('Vòng đầu'), findsWidgets);

    // Verify the 3 summary stat cards show latest values
    expect(find.text('4.5 kg'), findsOneWidget);
    expect(find.text('55.5 cm'), findsOneWidget);
    expect(find.text('36.8 cm'), findsOneWidget);

    // Tap "Chiều cao" tab
    await tester.tap(find.text('Chiều cao').first);
    await tester.pumpAndSettle();

    expect(find.text('Xu hướng chiều cao'), findsOneWidget);
    expect(find.text('50.0 cm – 55.5 cm'), findsOneWidget);

    // Tap "Vòng đầu" tab
    await tester.tap(find.text('Vòng đầu').first);
    await tester.pumpAndSettle();

    expect(find.text('Xu hướng vòng đầu'), findsOneWidget);
    expect(find.text('34.0 cm – 36.8 cm'), findsOneWidget);

    // Tap "Cân nặng" summary card to switch back to weight
    await tester.tap(find.text('4.5 kg'));
    await tester.pumpAndSettle();

    expect(find.text('Xu hướng cân nặng'), findsOneWidget);
    expect(find.text('3.2 kg – 4.5 kg'), findsOneWidget);
  });
}
