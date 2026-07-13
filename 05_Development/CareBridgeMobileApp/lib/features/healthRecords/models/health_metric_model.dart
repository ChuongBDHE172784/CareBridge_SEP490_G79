// UC-26 update request DTO (mirrors backend UpdateMetricRequest)
DateTime _parseMetricDateTime(Object? value) {
  return DateTime.parse(value as String).toLocal();
}

class UpdateMetricRequest {
  final double? valueNumeric;
  final double? valueSecondary;
  final String? unit;
  final DateTime? measuredAt;
  final String? note;

  const UpdateMetricRequest({
    this.valueNumeric,
    this.valueSecondary,
    this.unit,
    this.measuredAt,
    this.note,
  });

  Map<String, dynamic> toJson() => {
    if (valueNumeric != null) 'valueNumeric': valueNumeric,
    if (valueSecondary != null) 'valueSecondary': valueSecondary,
    if (unit != null) 'unit': unit,
    if (measuredAt != null) 'measuredAt': measuredAt!.toUtc().toIso8601String(),
    if (note != null) 'note': note,
  };
}

// UC-25 add request DTO (mirrors backend AddMetricRequest)
class AddMetricRequest {
  final String metricType;
  final double valueNumeric;
  final double? valueSecondary;
  final String unit;
  final DateTime measuredAt;
  final String sourceType;
  final String? note;

  const AddMetricRequest({
    required this.metricType,
    required this.valueNumeric,
    this.valueSecondary,
    required this.unit,
    required this.measuredAt,
    this.sourceType = 'MANUAL',
    this.note,
  });

  Map<String, dynamic> toJson() => {
    'metricType': metricType,
    'valueNumeric': valueNumeric,
    if (valueSecondary != null) 'valueSecondary': valueSecondary,
    'unit': unit,
    'measuredAt': measuredAt.toUtc().toIso8601String(),
    'sourceType': sourceType,
    if (note != null) 'note': note,
  };
}

// UC-27 trend data point (mirrors backend MetricDataPoint)
class MetricDataPoint {
  final String? metricId;
  final DateTime measuredAt;
  final double valueNumeric;
  final double? valueSecondary;
  final SourceType sourceType;
  final String? note;

  const MetricDataPoint({
    this.metricId,
    required this.measuredAt,
    required this.valueNumeric,
    this.valueSecondary,
    this.sourceType = SourceType.manual,
    this.note,
  });

  factory MetricDataPoint.fromJson(Map<String, dynamic> json) =>
      MetricDataPoint(
        metricId: (json['metricId'] ?? json['metric_id'] ?? json['id'])
            ?.toString(),
        measuredAt: _parseMetricDateTime(json['measuredAt']),
        valueNumeric: (json['valueNumeric'] as num).toDouble(),
        valueSecondary: (json['valueSecondary'] as num?)?.toDouble(),
        sourceType: SourceTypeExtension.fromApi(json['sourceType'] as String?),
        note: json['note'] as String?,
      );

  String get valueDisplay {
    if (valueSecondary != null) {
      return '${valueNumeric.toStringAsFixed(0)}/${valueSecondary!.toStringAsFixed(0)}';
    }
    return valueNumeric % 1 == 0
        ? valueNumeric.toStringAsFixed(0)
        : valueNumeric.toStringAsFixed(1);
  }
}

// UC-27 trend response (mirrors backend MetricTrendResponse)
class MetricTrend {
  final String metricType;
  final String? unit;
  final List<MetricDataPoint> dataPoints;

  const MetricTrend({
    required this.metricType,
    this.unit,
    required this.dataPoints,
  });

  factory MetricTrend.fromJson(Map<String, dynamic> json) => MetricTrend(
    metricType: json['metricType'] as String? ?? '',
    unit: json['unit'] as String?,
    dataPoints: (json['dataPoints'] as List<dynamic>? ?? [])
        .map((e) => MetricDataPoint.fromJson(e as Map<String, dynamic>))
        .toList(),
  );

  double get average {
    if (dataPoints.isEmpty) return 0;
    return dataPoints.map((p) => p.valueNumeric).reduce((a, b) => a + b) /
        dataPoints.length;
  }

  double? get trend {
    if (dataPoints.length < 2) return null;
    final first = dataPoints.first.valueNumeric;
    final last = dataPoints.last.valueNumeric;
    return first == 0 ? null : (last - first) / first * 100;
  }
}

enum MetricType {
  weight,
  bloodPressure,
  bloodSugar,
  temperature,
  heartRate,
  fetalMovement,
  other,
}

extension MetricTypeExtension on MetricType {
  String get displayLabel {
    switch (this) {
      case MetricType.weight:
        return 'Cân nặng';
      case MetricType.bloodPressure:
        return 'Huyết áp';
      case MetricType.bloodSugar:
        return 'Đường huyết';
      case MetricType.temperature:
        return 'Nhiệt độ';
      case MetricType.heartRate:
        return 'Nhịp tim';
      case MetricType.fetalMovement:
        return 'Cử động thai';
      case MetricType.other:
        return 'Khác';
    }
  }

  static MetricType fromApi(String? value) {
    switch (value) {
      case 'WEIGHT':
        return MetricType.weight;
      case 'BLOOD_PRESSURE':
      case 'BLOOD_PRESSURE_SYSTOLIC':
      case 'BLOOD_PRESSURE_DIASTOLIC':
        return MetricType.bloodPressure;
      case 'BLOOD_SUGAR':
        return MetricType.bloodSugar;
      case 'BLOOD_GLUCOSE':
        return MetricType.bloodSugar;
      case 'TEMPERATURE':
        return MetricType.temperature;
      case 'HEART_RATE':
        return MetricType.heartRate;
      case 'FETAL_MOVEMENT':
        return MetricType.fetalMovement;
      case 'FETAL_MOVEMENT_COUNT':
        return MetricType.fetalMovement;
      default:
        return MetricType.other;
    }
  }
}

enum SourceType { manual, device, sync }

extension SourceTypeExtension on SourceType {
  String get displayLabel {
    switch (this) {
      case SourceType.manual:
        return 'Nhập thủ công';
      case SourceType.device:
        return 'Thiết bị đo';
      case SourceType.sync:
        return 'Đồng bộ tự động';
    }
  }

  static SourceType fromApi(String? value) {
    switch (value) {
      case 'DEVICE':
        return SourceType.device;
      case 'SYNC':
      case 'IMPORTED':
        return SourceType.sync;
      default:
        return SourceType.manual;
    }
  }
}

class HealthMetricDetail {
  final String id;
  final String journeyId;
  final MetricType metricType;
  final double valueNumeric;
  final double? valueSecondary;
  final String unit;
  final DateTime measuredAt;
  final SourceType sourceType;
  final String? note;
  final DateTime createdAt;

  const HealthMetricDetail({
    required this.id,
    required this.journeyId,
    required this.metricType,
    required this.valueNumeric,
    this.valueSecondary,
    required this.unit,
    required this.measuredAt,
    required this.sourceType,
    this.note,
    required this.createdAt,
  });

  factory HealthMetricDetail.fromJson(Map<String, dynamic> json) {
    return HealthMetricDetail(
      id: (json['id'] ?? json['metricId']) as String,
      journeyId: json['journeyId'] as String,
      metricType: MetricTypeExtension.fromApi(json['metricType'] as String?),
      valueNumeric: (json['valueNumeric'] as num).toDouble(),
      valueSecondary: (json['valueSecondary'] as num?)?.toDouble(),
      unit: json['unit'] as String? ?? '',
      measuredAt: _parseMetricDateTime(json['measuredAt']),
      sourceType: SourceTypeExtension.fromApi(json['sourceType'] as String?),
      note: json['note'] as String?,
      createdAt: _parseMetricDateTime(
        json['createdAt'] ?? json['updatedAt'] ?? json['measuredAt'],
      ),
    );
  }

  String get valueDisplay {
    if (valueSecondary != null) {
      return '${valueNumeric.toStringAsFixed(0)}/${valueSecondary!.toStringAsFixed(0)}';
    }
    final str = valueNumeric.toString();
    return str.endsWith('.0') ? str.substring(0, str.length - 2) : str;
  }
}
