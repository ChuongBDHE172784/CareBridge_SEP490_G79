import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/healthRecords/models/health_metric_model.dart';

void main() {
  group('MetricTypeExtension', () {
    test('maps quick-note observation types from the API', () {
      expect(MetricTypeExtension.fromApi('HYDRATION'), MetricType.hydration);
      expect(MetricTypeExtension.fromApi('MOOD'), MetricType.mood);
      expect(
        MetricTypeExtension.fromApi('FETAL_MOVEMENT_COUNT'),
        MetricType.fetalMovement,
      );
    });

    test('uses Vietnamese quick-note labels', () {
      expect(MetricType.hydration.displayLabel, 'Nước');
      expect(MetricType.mood.displayLabel, 'Tâm trạng');
    });
  });
}
