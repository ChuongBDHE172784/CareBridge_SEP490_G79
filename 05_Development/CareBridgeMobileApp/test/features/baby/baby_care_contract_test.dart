import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/baby/models/baby_daily_log_model.dart';
import 'package:untitled/features/baby/models/baby_care_composite_model.dart';
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

  group('Baby care composite API contract', () {
    test('parses overview, timeline, and preparation read models', () {
      final overview = BabyCareOverview.fromJson({
        'babyId': 'baby-1',
        'nickname': 'Bông',
        'journalCount': 4,
        'growthMeasurementCount': 2,
        'milestoneCount': 1,
        'vaccinationRecordCount': 3,
        'notice': 'For observation',
      });
      final timeline = BabyCareTimeline.fromJson({
        'babyId': 'baby-1',
        'events': [
          {
            'sourceType': 'JOURNAL',
            'sourceId': 'log-1',
            'occurredAt': '2026-08-05T02:30:00Z',
            'displayLabel': 'FEEDING',
          },
        ],
        'nextCursor': null,
      });
      final preparation = AppointmentPreparationSummary.fromJson({
        'babyId': 'baby-1',
        'facts': ['Baby: Bông'],
        'dueItems': ['BCG (scheduled)'],
        'notice': 'For observation',
      });

      expect(overview.journalCount, 4);
      expect(timeline.events.single.sourceId, 'log-1');
      expect(preparation.dueItems, ['BCG (scheduled)']);
    });

    test('rejects malformed composite counts and event timestamps', () {
      expect(
        () => BabyCareOverview.fromJson({
          'babyId': 'baby-1',
          'nickname': 'Bông',
          'journalCount': -1,
          'growthMeasurementCount': 0,
          'milestoneCount': 0,
          'vaccinationRecordCount': 0,
        }),
        throwsFormatException,
      );
      expect(
        () => BabyCareTimelineEvent.fromJson({
          'sourceType': 'JOURNAL',
          'sourceId': 'log-1',
          'occurredAt': 'not-a-date',
          'displayLabel': 'FEEDING',
        }),
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
