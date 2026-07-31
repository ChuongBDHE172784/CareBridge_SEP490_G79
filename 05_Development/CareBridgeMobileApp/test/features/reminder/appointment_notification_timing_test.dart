import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/reminder/models/appointment_notification_timing.dart';

void main() {
  test('normalizes signed offsets into chronological order', () {
    expect(AppointmentNotificationTiming.normalize([15, -30, 0, -1440, -30]), [
      -1440,
      -30,
      0,
      15,
    ]);
  });

  test('accepts an empty list to disable appointment notifications', () {
    expect(AppointmentNotificationTiming.normalize(const []), isEmpty);
  });

  test('rejects offsets outside the supported range', () {
    expect(
      () => AppointmentNotificationTiming.normalize([-43201]),
      throwsA(isA<FormatException>()),
    );
    expect(
      () => AppointmentNotificationTiming.normalize([10081]),
      throwsA(isA<FormatException>()),
    );
  });

  test('renders Vietnamese before, exact, and after labels', () {
    expect(AppointmentNotificationTiming.label(-1440), '1 ngày trước');
    expect(AppointmentNotificationTiming.label(-30), '30 phút trước');
    expect(AppointmentNotificationTiming.label(0), 'Đúng giờ');
    expect(AppointmentNotificationTiming.label(15), '15 phút sau');
  });
}
