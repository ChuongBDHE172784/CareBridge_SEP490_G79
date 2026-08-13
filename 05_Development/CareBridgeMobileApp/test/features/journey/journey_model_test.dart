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

    test('keeps server source week separate from legacy pregnancy week', () {
      final dashboard = JourneyDashboard(
        journeyType: 'PREGNANCY',
        pregnancyWeek: 20,
        sourceWeekNumber: 21,
        plan: 2,
        datingBasis: 'EDD',
      );

      expect(dashboard.displayPregnancyWeek, 20);
      expect(dashboard.effectiveSourceWeekNumber, 21);
      expect(dashboard.plan, 2);
      expect(dashboard.gestationalDatingBasis, 'EDD');
    });

    test('does not infer a checklist source week from legacy pregnancy week', () {
      final dashboard = JourneyDashboard(
        journeyType: 'PREGNANCY',
        pregnancyWeek: 20,
      );

      expect(dashboard.effectiveSourceWeekNumber, isNull);
    });

    test('parses dashboard dating and plan metadata', () {
      final dashboard = JourneyDashboard.fromJson({
        'journeyType': 'PREGNANCY',
        'pregnancyWeek': 20,
        'completedGestationalWeek': 20,
        'completedGestationalDays': 4,
        'sourceWeekNumber': 21,
        'plan': 2,
        'gestationalDatingBasis': 'EDD',
        'gestationalDatingQuarantineReasonCode': 'DATING_DISCREPANCY',
        'canonicalLmp': '2026-01-01',
        'gestationalDatingRevision': 4,
      });

      expect(dashboard.completedGestationalWeek, 20);
      expect(dashboard.completedGestationalDays, 4);
      expect(dashboard.sourceWeekNumber, 21);
      expect(dashboard.plan, 2);
      expect(dashboard.datingBasis, 'EDD');
      expect(dashboard.datingQuarantineReason, 'DATING_DISCREPANCY');
      expect(dashboard.canonicalLmp, DateTime(2026, 1, 1));
      expect(dashboard.gestationalDatingRevision, 4);
    });
  });
}
