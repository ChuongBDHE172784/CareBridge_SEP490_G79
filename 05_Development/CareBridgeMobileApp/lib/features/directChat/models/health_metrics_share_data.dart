import 'dart:convert';

class HealthMetricMeasurementRecord {
  final String measuredAt;
  final String value;
  final String unit;
  final String status; // NORMAL, WARNING, CRITICAL
  final String? note;

  const HealthMetricMeasurementRecord({
    required this.measuredAt,
    required this.value,
    this.unit = '',
    this.status = 'NORMAL',
    this.note,
  });

  factory HealthMetricMeasurementRecord.fromJson(Map<String, dynamic> json) =>
      HealthMetricMeasurementRecord(
        measuredAt: json['measuredAt']?.toString() ?? '',
        value: json['value']?.toString() ?? '',
        unit: json['unit']?.toString() ?? '',
        status: (json['status'] as String? ?? 'NORMAL').toUpperCase(),
        note: json['note'] as String?,
      );

  Map<String, dynamic> toJson() => {
    'measuredAt': measuredAt,
    'value': value,
    'unit': unit,
    'status': status,
    if (note != null && note!.isNotEmpty) 'note': note,
  };
}

class HealthMetricItemData {
  final String code;
  final String name;
  final String value;
  final String unit;
  final String status; // NORMAL, WARNING, CRITICAL
  final String? icon;
  final String? measuredTime;
  final List<HealthMetricMeasurementRecord> history;

  const HealthMetricItemData({
    required this.code,
    required this.name,
    required this.value,
    this.unit = '',
    this.status = 'NORMAL',
    this.icon,
    this.measuredTime,
    this.history = const [],
  });

  factory HealthMetricItemData.fromJson(Map<String, dynamic> json) {
    final historyList = (json['history'] as List? ?? [])
        .map(
          (h) =>
              HealthMetricMeasurementRecord.fromJson(h as Map<String, dynamic>),
        )
        .toList();

    return HealthMetricItemData(
      code: json['code'] as String? ?? '',
      name: json['name'] as String? ?? 'Chỉ số',
      value: json['value']?.toString() ?? '',
      unit: json['unit']?.toString() ?? '',
      status: (json['status'] as String? ?? 'NORMAL').toUpperCase(),
      icon: json['icon'] as String?,
      measuredTime: json['measuredTime'] as String?,
      history: historyList,
    );
  }

  Map<String, dynamic> toJson() => {
    'code': code,
    'name': name,
    'value': value,
    'unit': unit,
    'status': status,
    'icon': icon,
    if (measuredTime != null) 'measuredTime': measuredTime,
    if (history.isNotEmpty)
      'history': history.map((h) => h.toJson()).toList(),
  };
}

class HealthMetricsShareData {
  final String title;
  final int? gestationalWeek;
  final String? measuredDate;
  final String? timeRangeLabel;
  final String? journeyId;
  final bool isLiveSync;
  final String? note;
  final List<HealthMetricItemData> metrics;

  const HealthMetricsShareData({
    this.title = 'Lịch sử chỉ số sức khỏe',
    this.gestationalWeek,
    this.measuredDate,
    this.timeRangeLabel,
    this.journeyId,
    this.isLiveSync = true,
    this.note,
    required this.metrics,
  });

  static const String tag = '[CAREBRIDGE_HEALTH_SHARE]';

  static bool isHealthShareMessage(String? body) {
    if (body == null) return false;
    return body.trim().startsWith(tag);
  }

  static HealthMetricsShareData? parse(String? body) {
    if (body == null || !isHealthShareMessage(body)) return null;
    try {
      final jsonStr = body.replaceFirst(tag, '').trim();
      final decoded = jsonDecode(jsonStr) as Map<String, dynamic>;
      final metricsList = (decoded['metrics'] as List? ?? [])
          .map((m) => HealthMetricItemData.fromJson(m as Map<String, dynamic>))
          .toList();
      return HealthMetricsShareData(
        title: decoded['title'] as String? ?? 'Lịch sử chỉ số sức khỏe',
        gestationalWeek: (decoded['gestationalWeek'] as num?)?.toInt(),
        measuredDate: decoded['measuredDate'] as String?,
        timeRangeLabel: decoded['timeRangeLabel'] as String?,
        journeyId: decoded['journeyId'] as String?,
        isLiveSync: decoded['isLiveSync'] as bool? ?? true,
        note: decoded['note'] as String?,
        metrics: metricsList,
      );
    } catch (_) {
      return null;
    }
  }

  String serialize() => '$tag\n${jsonEncode({
    'title': title,
    'gestationalWeek': gestationalWeek,
    'measuredDate': measuredDate,
    'timeRangeLabel': timeRangeLabel,
    'journeyId': journeyId,
    'isLiveSync': isLiveSync,
    'note': note,
    'metrics': metrics.map((m) => m.toJson()).toList(),
  })}';
}
