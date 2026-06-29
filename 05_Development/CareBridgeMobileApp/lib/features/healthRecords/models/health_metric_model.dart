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
      case MetricType.weight: return 'Cân nặng';
      case MetricType.bloodPressure: return 'Huyết áp';
      case MetricType.bloodSugar: return 'Đường huyết';
      case MetricType.temperature: return 'Nhiệt độ';
      case MetricType.heartRate: return 'Nhịp tim';
      case MetricType.fetalMovement: return 'Cử động thai';
      case MetricType.other: return 'Khác';
    }
  }

  static MetricType fromApi(String? value) {
    switch (value) {
      case 'WEIGHT': return MetricType.weight;
      case 'BLOOD_PRESSURE': return MetricType.bloodPressure;
      case 'BLOOD_SUGAR': return MetricType.bloodSugar;
      case 'TEMPERATURE': return MetricType.temperature;
      case 'HEART_RATE': return MetricType.heartRate;
      case 'FETAL_MOVEMENT': return MetricType.fetalMovement;
      default: return MetricType.other;
    }
  }
}

enum SourceType { manual, device, sync }

extension SourceTypeExtension on SourceType {
  String get displayLabel {
    switch (this) {
      case SourceType.manual: return 'Nhập thủ công';
      case SourceType.device: return 'Thiết bị đo';
      case SourceType.sync: return 'Đồng bộ tự động';
    }
  }

  static SourceType fromApi(String? value) {
    switch (value) {
      case 'DEVICE': return SourceType.device;
      case 'SYNC': return SourceType.sync;
      default: return SourceType.manual;
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
      id: json['id'] as String,
      journeyId: json['journeyId'] as String,
      metricType: MetricTypeExtension.fromApi(json['metricType'] as String?),
      valueNumeric: (json['valueNumeric'] as num).toDouble(),
      valueSecondary: (json['valueSecondary'] as num?)?.toDouble(),
      unit: json['unit'] as String? ?? '',
      measuredAt: DateTime.parse(json['measuredAt'] as String),
      sourceType: SourceTypeExtension.fromApi(json['sourceType'] as String?),
      note: json['note'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
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
