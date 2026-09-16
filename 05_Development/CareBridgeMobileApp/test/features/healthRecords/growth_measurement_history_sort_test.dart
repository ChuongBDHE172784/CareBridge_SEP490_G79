import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/baby/models/baby_model.dart';
import 'package:untitled/features/healthRecords/models/growth_measurement_model.dart';
import 'package:untitled/features/healthRecords/screens/growth_measurement_history_screen.dart';

void main() {
  testWidgets('GrowthMeasurementHistoryScreen sort button toggles between newest and oldest', (tester) async {
    tester.view.physicalSize = const Size(1080, 2400);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.reset);

    // Initial server order: newest first
    final measurements = [
      GrowthMeasurement(
        id: 'growth-new',
        measuredAt: DateTime(2026, 8, 1),
        heightCm: 65.0,
        weightKg: 7.0,
        recordedBy: 'mother-1',
      ),
      GrowthMeasurement(
        id: 'growth-mid',
        measuredAt: DateTime(2026, 7, 1),
        heightCm: 62.0,
        weightKg: 6.5,
        recordedBy: 'mother-1',
      ),
      GrowthMeasurement(
        id: 'growth-old',
        measuredAt: DateTime(2026, 6, 1),
        heightCm: 60.0,
        weightKg: 6.0,
        recordedBy: 'mother-1',
      ),
    ];

    await tester.pumpWidget(
      MaterialApp(
        home: GrowthMeasurementHistoryScreen(
          babyId: 'baby-1',
          loadAvatarImage: false,
          historyLoader: (_) async => measurements,
          profileLoader: (_) async => BabyProfile(
            id: 'baby-1',
            nickname: 'Bé',
            birthDate: DateTime(2026, 1, 1),
            gender: BabyGender.female,
            isActive: true,
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    // Initial button text is 'Sắp xếp'
    expect(find.byKey(const Key('growth-history-sort-button')), findsOneWidget);
    expect(find.text('Sắp xếp'), findsOneWidget);

    final recordCardsNew = find.byKey(const ValueKey('growth-history-record-growth-new'));
    final recordCardsOld = find.byKey(const ValueKey('growth-history-record-growth-old'));
    expect(recordCardsNew, findsOneWidget);
    expect(recordCardsOld, findsOneWidget);

    // Initially: growth-new is above growth-old
    final topNewInitial = tester.getTopLeft(recordCardsNew).dy;
    final topOldInitial = tester.getTopLeft(recordCardsOld).dy;
    expect(topNewInitial, lessThan(topOldInitial));

    // Tap sort button to toggle to Oldest first
    await tester.tap(find.byKey(const Key('growth-history-sort-button')));
    await tester.pumpAndSettle();

    expect(find.text('Cũ nhất'), findsOneWidget);

    // growth-old should now appear above growth-new
    final topNewAfter = tester.getTopLeft(recordCardsNew).dy;
    final topOldAfter = tester.getTopLeft(recordCardsOld).dy;
    expect(topOldAfter, lessThan(topNewAfter));

    // Tap sort button again to toggle back to Newest first
    await tester.tap(find.byKey(const Key('growth-history-sort-button')));
    await tester.pumpAndSettle();

    expect(find.text('Mới nhất'), findsOneWidget);
    final topNewFinal = tester.getTopLeft(recordCardsNew).dy;
    final topOldFinal = tester.getTopLeft(recordCardsOld).dy;
    expect(topNewFinal, lessThan(topOldFinal));
  });
}
