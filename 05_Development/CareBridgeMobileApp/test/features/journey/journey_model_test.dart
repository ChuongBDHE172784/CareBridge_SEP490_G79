import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/journey/models/journey_model.dart';

void main() {
  group('JourneyDashboard pregnancy week calculation', () {
    test('uses explicit pregnancyWeek when backend provides it', () {
      final dashboard = JourneyDashboard(
        pregnancyWeek: 18,
        estimatedDueDate: DateTime(2026, 12, 26),
      );

      expect(dashboard.effectivePregnancyWeek, 18);
    });

    test('calculates week from last menstrual date', () {
      final week = JourneyDashboard.calculatePregnancyWeek(
        lastMenstrualDate: DateTime(2026, 1, 1),
        today: DateTime(2026, 4, 2),
      );

      expect(week, 13);
    });

    test('calculates week from estimated due date', () {
      final week = JourneyDashboard.calculatePregnancyWeek(
        estimatedDueDate: DateTime(2026, 10, 8),
        today: DateTime(2026, 4, 2),
      );

      expect(week, 13);
    });
  });
}
