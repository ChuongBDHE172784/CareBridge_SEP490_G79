import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/baby/models/baby_daily_log_model.dart';
import 'package:untitled/features/healthRecords/models/growth_measurement_model.dart';

void main() {
  group('Daily log API contract', () {
    test('serializes caller-entered log without identity fields', () {
      final request = AddBabyDailyLogRequest(
        logType: LogType.feeding,
        startedAt: DateTime.parse('2026-07-15T02:30:00Z'),
        quantity: 120,
        unit: 'ml',
        note: 'Morning feed',
      );

      expect(request.toJson(), {
        'logType': 'FEEDING',
        'startedAt': '2026-07-15T02:30:00.000Z',
        'quantity': 120.0,
        'unit': 'ml',
        'note': 'Morning feed',
      });
      expect(request.toJson(), isNot(contains('recordedBy')));
      expect(request.toJson(), isNot(contains('ownerUserId')));
    });

    test('parses canonical backend daily-log response fields', () {
      final log = BabyDailyLog.fromJson({
        'babyLogId': 'log-1',
        'babyId': 'baby-1',
        'logType': 'DIAPER',
        'startedAt': '2026-07-15T02:30:00Z',
        'createdAt': '2026-07-15T02:31:00Z',
        'recordedBy': 'caregiver-1',
      });

      expect(log.id, 'log-1');
      expect(log.babyId, 'baby-1');
      expect(log.logType, LogType.diaper);
      expect(log.startedAt, DateTime.parse('2026-07-15T02:30:00Z'));
      expect(log.createdAt, DateTime.parse('2026-07-15T02:31:00Z'));
      expect(log.displayTypeLabel, 'Thay tã');
    });

    test('preserves legacy daily-log type labels from the API', () {
      final log = BabyDailyLog.fromJson({
        'babyLogId': 'log-2',
        'babyId': 'baby-1',
        'logType': 'MEDICINE',
      });

      expect(log.displayTypeLabel, 'Thuốc');
    });

    test('does not turn an unknown daily-log type into feeding', () {
      final log = BabyDailyLog.fromJson({
        'babyLogId': 'log-3',
        'babyId': 'baby-1',
        'logType': 'NEW_SERVER_TYPE',
      });

      expect(log.displayTypeLabel, 'Khác');
    });

    test('rejects malformed summary counts instead of showing fake zeros', () {
      expect(
        () => LogTypeSummary.fromJson({'count': -1}),
        throwsFormatException,
      );
      expect(
        () => LogTypeSummary.fromJson({'count': 1.5}),
        throwsFormatException,
      );
    });
  });

  group('Growth API contract', () {
    test('parses canonical backend growth fields', () {
      final measurement = GrowthMeasurement.fromJson({
        'growthMeasurementId': 'growth-1',
        'measuredDate': '2026-07-14',
        'weightKg': 6.2,
        'heightCm': 61.5,
        'sourceType': 'HOME',
        'ageInDays': 92,
      });

      expect(measurement.id, 'growth-1');
      expect(measurement.measuredAt, DateTime.parse('2026-07-14'));
      expect(measurement.weightKg, 6.2);
      expect(measurement.sourceType, 'HOME');
      expect(measurement.ageInDays, 92);
    });

    test('uses a safe sentinel without a measurement date', () {
      final measurement = GrowthMeasurement.fromJson({
        'growthMeasurementId': 'growth-1',
        'weightKg': 6.2,
      });
      expect(
        measurement.measuredAt,
        DateTime.fromMillisecondsSinceEpoch(0, isUtc: true),
      );
    });
  });
}
