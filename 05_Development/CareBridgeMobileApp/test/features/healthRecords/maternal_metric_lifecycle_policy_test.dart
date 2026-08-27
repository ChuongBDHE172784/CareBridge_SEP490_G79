import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/healthRecords/models/maternal_metric_lifecycle_policy.dart';

void main() {
  group('maternalMetricEntryRestrictionMessage', () {
    test('fetal movement is available only during pregnancy', () {
      expect(
        maternalMetricEntryRestrictionMessage(
          metricType: 'FETAL_MOVEMENT_SESSION',
          journeyType: 'PREGNANCY',
        ),
        isNull,
      );
      expect(
        maternalMetricEntryRestrictionMessage(
          metricType: 'FETAL_MOVEMENT_COUNT',
          journeyType: 'POSTPARTUM',
        ),
        fetalMovementPregnancyOnlyMessage,
      );
      expect(
        maternalMetricEntryRestrictionMessage(
          metricType: 'FETAL_MOVEMENT',
          journeyType: 'PRE_PREGNANCY',
        ),
        fetalMovementPregnancyOnlyMessage,
      );
    });

    test('EPDS is blocked before pregnancy and allowed afterward', () {
      expect(
        maternalMetricEntryRestrictionMessage(
          metricType: 'EPDS_SCORE',
          journeyType: 'PRE_PREGNANCY',
        ),
        epdsPregnancyOrPostpartumOnlyMessage,
      );
      expect(
        maternalMetricEntryRestrictionMessage(
          metricType: 'EPDS_SCORE',
          journeyType: 'PREGNANCY',
        ),
        isNull,
      );
      expect(
        maternalMetricEntryRestrictionMessage(
          metricType: 'EPDS_SCORE',
          journeyType: 'POSTPARTUM',
        ),
        isNull,
      );
    });

    test('other metrics are unaffected by journey stage', () {
      expect(
        maternalMetricEntryRestrictionMessage(
          metricType: 'BLOOD_PRESSURE',
          journeyType: 'PRE_PREGNANCY',
        ),
        isNull,
      );
    });
  });
}
