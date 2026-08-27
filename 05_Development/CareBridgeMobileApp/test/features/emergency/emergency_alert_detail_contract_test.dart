import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/emergency/models/emergency_alert_model.dart';

void main() {
  test(
    'family alert detail parses real contact location and acknowledgement',
    () {
      final alert = EmergencyAlert.fromDetailJson({
        'sessionId': '5d80bd66-5d66-4c5b-bd23-6b3eb96a52b9',
        'motherName': 'Mother Test',
        'motherPhone': '0901234567',
        'triggerSource': 'FALL_DETECTION',
        'latitude': 10.762622,
        'longitude': 106.660172,
        'acknowledged': true,
        'acknowledgedAt': '2026-08-10T11:00:00Z',
        'createdAt': '2026-08-10T10:59:00Z',
      });

      expect(alert.personName, 'Mother Test');
      expect(alert.phoneNumber, '0901234567');
      expect(alert.latitude, 10.762622);
      expect(alert.longitude, 106.660172);
      expect(alert.acknowledged, isTrue);
      expect(alert.acknowledgedAt, DateTime.parse('2026-08-10T11:00:00Z'));
    },
  );
}
