import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/healthRecords/models/who_growth_standard.dart';

void main() {
  group('WHO P50 growth standards', () {
    test('returns exact male values at 0, 12, and 24 months', () {
      final birthDate = DateTime(2024, 1, 15);

      expect(
        WhoGrowthStandard.medianAtDate(
          sex: WhoGrowthSex.male,
          metric: WhoGrowthMetric.weight,
          birthDate: birthDate,
          measuredAt: birthDate,
        ),
        3.3,
      );
      expect(
        WhoGrowthStandard.medianAtDate(
          sex: WhoGrowthSex.male,
          metric: WhoGrowthMetric.length,
          birthDate: birthDate,
          measuredAt: DateTime(2025, 1, 15),
        ),
        75.7,
      );
      expect(
        WhoGrowthStandard.medianAtDate(
          sex: WhoGrowthSex.male,
          metric: WhoGrowthMetric.headCircumference,
          birthDate: birthDate,
          measuredAt: DateTime(2026, 1, 15),
        ),
        48.3,
      );
    });

    test('uses the female table and correct metric columns', () {
      final birthDate = DateTime(2025, 1, 1);
      final measuredAt = DateTime(2026, 1, 1);

      expect(
        WhoGrowthStandard.medianAtDate(
          sex: WhoGrowthSex.female,
          metric: WhoGrowthMetric.weight,
          birthDate: birthDate,
          measuredAt: measuredAt,
        ),
        8.9,
      );
      expect(
        WhoGrowthStandard.medianAtDate(
          sex: WhoGrowthSex.female,
          metric: WhoGrowthMetric.length,
          birthDate: birthDate,
          measuredAt: measuredAt,
        ),
        74.0,
      );
      expect(
        WhoGrowthStandard.medianAtDate(
          sex: WhoGrowthSex.female,
          metric: WhoGrowthMetric.headCircumference,
          birthDate: birthDate,
          measuredAt: measuredAt,
        ),
        44.9,
      );
    });

    test('interpolates using calendar-month anniversaries', () {
      final value = WhoGrowthStandard.medianAtDate(
        sex: WhoGrowthSex.male,
        metric: WhoGrowthMetric.weight,
        birthDate: DateTime(2024, 1, 31),
        measuredAt: DateTime(2024, 2, 14),
      );

      expect(value, closeTo(3.3 + (4.5 - 3.3) * 14 / 29, 0.0001));
    });

    test('does not clamp dates outside 0 to 24 months', () {
      final birthDate = DateTime(2024, 1, 15);

      expect(
        WhoGrowthStandard.medianAtDate(
          sex: WhoGrowthSex.male,
          metric: WhoGrowthMetric.weight,
          birthDate: birthDate,
          measuredAt: DateTime(2024, 1, 14),
        ),
        isNull,
      );
      expect(
        WhoGrowthStandard.medianAtDate(
          sex: WhoGrowthSex.male,
          metric: WhoGrowthMetric.weight,
          birthDate: birthDate,
          measuredAt: DateTime(2026, 1, 16),
        ),
        isNull,
      );
    });

    test('monthly series has all 25 source points', () {
      final points = WhoGrowthStandard.monthlySeries(
        sex: WhoGrowthSex.female,
        metric: WhoGrowthMetric.length,
        birthDate: DateTime(2024, 2, 29),
      );

      expect(points, hasLength(25));
      expect(points.first.value, 49.1);
      expect(points.last.value, 86.4);
      expect(points.last.measuredAt, DateTime(2026, 2, 28));
    });
  });
}
