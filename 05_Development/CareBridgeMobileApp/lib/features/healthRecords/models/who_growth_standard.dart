enum WhoGrowthSex { male, female }

enum WhoGrowthMetric { weight, length, headCircumference }

class WhoGrowthReferencePoint {
  final DateTime measuredAt;
  final double value;

  const WhoGrowthReferencePoint({
    required this.measuredAt,
    required this.value,
  });
}

class _WhoMedianRow {
  final double weightKg;
  final double lengthCm;
  final double headCircumferenceCm;

  const _WhoMedianRow(this.weightKg, this.lengthCm, this.headCircumferenceCm);

  double valueFor(WhoGrowthMetric metric) => switch (metric) {
    WhoGrowthMetric.weight => weightKg,
    WhoGrowthMetric.length => lengthCm,
    WhoGrowthMetric.headCircumference => headCircumferenceCm,
  };
}

/// WHO Child Growth Standards median (P50/Z=0), birth through 24 months.
///
/// Source snapshot compiled 2026-07-16 from WHO weight-for-age,
/// length-for-age, and head-circumference-for-age tables:
/// https://www.who.int/tools/child-growth-standards/standards
///
/// Values between source-month rows are linearly interpolated by the number
/// of calendar days between consecutive birth-date anniversaries. This
/// reference is for visual comparison only and is not a diagnosis.
abstract final class WhoGrowthStandard {
  static const List<_WhoMedianRow> _male = [
    _WhoMedianRow(3.3, 49.9, 34.5),
    _WhoMedianRow(4.5, 54.7, 37.3),
    _WhoMedianRow(5.6, 58.4, 39.1),
    _WhoMedianRow(6.4, 61.4, 40.5),
    _WhoMedianRow(7.0, 63.9, 41.6),
    _WhoMedianRow(7.5, 65.9, 42.6),
    _WhoMedianRow(7.9, 67.6, 43.3),
    _WhoMedianRow(8.3, 69.2, 44.0),
    _WhoMedianRow(8.6, 70.6, 44.5),
    _WhoMedianRow(8.9, 72.0, 45.0),
    _WhoMedianRow(9.2, 73.3, 45.4),
    _WhoMedianRow(9.4, 74.5, 45.8),
    _WhoMedianRow(9.6, 75.7, 46.1),
    _WhoMedianRow(9.9, 76.9, 46.3),
    _WhoMedianRow(10.1, 78.0, 46.6),
    _WhoMedianRow(10.3, 79.1, 46.8),
    _WhoMedianRow(10.5, 80.2, 47.0),
    _WhoMedianRow(10.7, 81.2, 47.2),
    _WhoMedianRow(10.9, 82.3, 47.4),
    _WhoMedianRow(11.1, 83.2, 47.5),
    _WhoMedianRow(11.3, 84.2, 47.7),
    _WhoMedianRow(11.5, 85.1, 47.8),
    _WhoMedianRow(11.8, 86.0, 48.0),
    _WhoMedianRow(12.0, 86.9, 48.1),
    _WhoMedianRow(12.2, 87.8, 48.3),
  ];

  static const List<_WhoMedianRow> _female = [
    _WhoMedianRow(3.2, 49.1, 33.9),
    _WhoMedianRow(4.2, 53.7, 36.5),
    _WhoMedianRow(5.1, 57.1, 38.3),
    _WhoMedianRow(5.8, 59.8, 39.5),
    _WhoMedianRow(6.4, 62.1, 40.6),
    _WhoMedianRow(6.9, 64.0, 41.5),
    _WhoMedianRow(7.3, 65.7, 42.2),
    _WhoMedianRow(7.6, 67.3, 42.8),
    _WhoMedianRow(7.9, 68.7, 43.4),
    _WhoMedianRow(8.2, 70.1, 43.8),
    _WhoMedianRow(8.5, 71.5, 44.2),
    _WhoMedianRow(8.7, 72.8, 44.6),
    _WhoMedianRow(8.9, 74.0, 44.9),
    _WhoMedianRow(9.2, 75.2, 45.2),
    _WhoMedianRow(9.4, 76.4, 45.4),
    _WhoMedianRow(9.6, 77.5, 45.7),
    _WhoMedianRow(9.8, 78.6, 45.9),
    _WhoMedianRow(10.0, 79.7, 46.1),
    _WhoMedianRow(10.2, 80.7, 46.2),
    _WhoMedianRow(10.4, 81.7, 46.4),
    _WhoMedianRow(10.6, 82.7, 46.6),
    _WhoMedianRow(10.9, 83.7, 46.7),
    _WhoMedianRow(11.1, 84.6, 46.9),
    _WhoMedianRow(11.3, 85.5, 47.0),
    _WhoMedianRow(11.5, 86.4, 47.2),
  ];

  static double? medianAtDate({
    required WhoGrowthSex sex,
    required WhoGrowthMetric metric,
    required DateTime birthDate,
    required DateTime measuredAt,
  }) {
    final birth = _dateOnly(birthDate);
    final measured = _dateOnly(measuredAt);
    if (measured.isBefore(birth)) return null;

    var completedMonths =
        (measured.year - birth.year) * 12 + measured.month - birth.month;
    if (completedMonths < 0) return null;
    if (measured.isBefore(_addMonthsClamped(birth, completedMonths))) {
      completedMonths--;
    }
    if (completedMonths < 0 || completedMonths > 24) return null;

    final rows = sex == WhoGrowthSex.male ? _male : _female;
    final lowerValue = rows[completedMonths].valueFor(metric);
    if (completedMonths == 24) {
      return measured == _addMonthsClamped(birth, 24) ? lowerValue : null;
    }

    final lowerDate = _addMonthsClamped(birth, completedMonths);
    final upperDate = _addMonthsClamped(birth, completedMonths + 1);
    final intervalDays = upperDate.difference(lowerDate).inDays;
    final elapsedDays = measured.difference(lowerDate).inDays;
    final fraction = intervalDays == 0 ? 0.0 : elapsedDays / intervalDays;
    final upperValue = rows[completedMonths + 1].valueFor(metric);
    return lowerValue + (upperValue - lowerValue) * fraction;
  }

  static List<WhoGrowthReferencePoint> monthlySeries({
    required WhoGrowthSex sex,
    required WhoGrowthMetric metric,
    required DateTime birthDate,
  }) {
    final birth = _dateOnly(birthDate);
    final rows = sex == WhoGrowthSex.male ? _male : _female;
    return List<WhoGrowthReferencePoint>.unmodifiable(
      List.generate(
        rows.length,
        (month) => WhoGrowthReferencePoint(
          measuredAt: _addMonthsClamped(birth, month),
          value: rows[month].valueFor(metric),
        ),
      ),
    );
  }

  static DateTime _dateOnly(DateTime value) =>
      DateTime(value.year, value.month, value.day);

  static DateTime _addMonthsClamped(DateTime date, int months) {
    final zeroBasedMonth = date.month - 1 + months;
    final year = date.year + zeroBasedMonth ~/ 12;
    final month = zeroBasedMonth % 12 + 1;
    final lastDay = DateTime(year, month + 1, 0).day;
    return DateTime(year, month, date.day > lastDay ? lastDay : date.day);
  }
}
