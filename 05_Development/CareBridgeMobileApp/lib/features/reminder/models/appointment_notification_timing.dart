class AppointmentNotificationTiming {
  static const List<int> systemDefaults = [-1440, -30, 0, 15];
  static const List<int> presets = [-1440, -60, -30, -15, 0, 15, 30];
  static const int minOffsetMinutes = -43200;
  static const int maxOffsetMinutes = 10080;
  static const int maxRules = 10;
  static const String appTimeZone = 'Asia/Ho_Chi_Minh';

  static List<int> normalize(Iterable<int> values) {
    final result = values.toSet().toList()..sort();
    if (result.length > maxRules) {
      throw const FormatException('Chỉ được đặt tối đa 10 mốc thông báo.');
    }
    if (result.any(
      (value) => value < minOffsetMinutes || value > maxOffsetMinutes,
    )) {
      throw const FormatException(
        'Thời gian nhắc phải từ 30 ngày trước đến 7 ngày sau lịch hẹn.',
      );
    }
    return List.unmodifiable(result);
  }

  static String label(int offsetMinutes) {
    if (offsetMinutes == 0) return 'Đúng giờ';
    final direction = offsetMinutes < 0 ? 'trước' : 'sau';
    var remaining = offsetMinutes.abs();
    final days = remaining ~/ 1440;
    remaining %= 1440;
    final hours = remaining ~/ 60;
    final minutes = remaining % 60;
    final parts = <String>[];
    if (days > 0) parts.add('$days ngày');
    if (hours > 0) parts.add('$hours giờ');
    if (minutes > 0) parts.add('$minutes phút');
    return '${parts.join(' ')} $direction';
  }
}
