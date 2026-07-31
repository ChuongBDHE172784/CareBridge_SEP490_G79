DateTime _parseMetricDateTime(Object? value) {
  return DateTime.parse(value as String).toLocal();
}

DateTime? _parseOptionalMetricDateTime(Object? value) {
  if (value == null) return null;
  return DateTime.parse(value as String).toLocal();
}

class MetricCapability {
  final String metricCode;
  final int version;
  final String displayName;
  final String observationShape;
  final bool manualEntrySupported;
  final bool deviceImportSupported;
  final String canonicalUnit;
  final List<String> acceptedInputUnits;
  final Map<String, dynamic> requiredContextSchema;

  const MetricCapability({
    required this.metricCode,
    required this.version,
    required this.displayName,
    required this.observationShape,
    required this.manualEntrySupported,
    required this.deviceImportSupported,
    required this.canonicalUnit,
    required this.acceptedInputUnits,
    required this.requiredContextSchema,
  });

  factory MetricCapability.fromJson(Map<String, dynamic> json) {
    return MetricCapability(
      metricCode: json['metricCode'] as String,
      version: (json['version'] as num).toInt(),
      displayName: json['displayName'] as String,
      observationShape: json['observationShape'] as String,
      manualEntrySupported: json['manualEntrySupported'] as bool? ?? false,
      deviceImportSupported: json['deviceImportSupported'] as bool? ?? false,
      canonicalUnit: json['canonicalUnit'] as String? ?? '',
      acceptedInputUnits: (json['acceptedInputUnits'] as List<dynamic>? ?? [])
          .map((value) => value.toString())
          .toList(),
      requiredContextSchema:
          (json['requiredContextSchema'] as Map<String, dynamic>?) ?? const {},
    );
  }
}

class UpdateMetricRequest {
  final double? valueNumeric;
  final double? valueSecondary;
  final String? unit;
  final DateTime? measuredAt;
  final String? note;
  final Map<String, dynamic>? context;
  final DateTime? periodStart;
  final DateTime? periodEnd;

  const UpdateMetricRequest({
    this.valueNumeric,
    this.valueSecondary,
    this.unit,
    this.measuredAt,
    this.note,
    this.context,
    this.periodStart,
    this.periodEnd,
  });

  Map<String, dynamic> toJson() => {
    if (valueNumeric != null) 'valueNumeric': valueNumeric,
    if (valueSecondary != null) 'valueSecondary': valueSecondary,
    if (unit != null) 'unit': unit,
    if (measuredAt != null) 'measuredAt': measuredAt!.toUtc().toIso8601String(),
    if (note != null) 'note': note,
    if (context != null) 'context': context,
    if (periodStart != null)
      'periodStart': periodStart!.toUtc().toIso8601String(),
    if (periodEnd != null) 'periodEnd': periodEnd!.toUtc().toIso8601String(),
  };
}

class AddMetricRequest {
  final String metricType;
  final double valueNumeric;
  final double? valueSecondary;
  final String unit;
  final DateTime measuredAt;
  final String sourceType;
  final String? note;
  final Map<String, dynamic> context;
  final DateTime? periodStart;
  final DateTime? periodEnd;
  final int? definitionVersion;

  const AddMetricRequest({
    required this.metricType,
    required this.valueNumeric,
    this.valueSecondary,
    required this.unit,
    required this.measuredAt,
    this.sourceType = 'MANUAL',
    this.note,
    this.context = const {},
    this.periodStart,
    this.periodEnd,
    this.definitionVersion,
  });

  Map<String, dynamic> toJson() => {
    'metricType': metricType,
    'valueNumeric': valueNumeric,
    if (valueSecondary != null) 'valueSecondary': valueSecondary,
    'unit': unit,
    'measuredAt': measuredAt.toUtc().toIso8601String(),
    'sourceType': sourceType,
    if (note != null) 'note': note,
    if (context.isNotEmpty) 'context': context,
    if (periodStart != null)
      'periodStart': periodStart!.toUtc().toIso8601String(),
    if (periodEnd != null) 'periodEnd': periodEnd!.toUtc().toIso8601String(),
    if (definitionVersion != null) 'definitionVersion': definitionVersion,
  };
}

class MetricDataPoint {
  final String? metricId;
  final DateTime measuredAt;
  final double valueNumeric;
  final double? valueSecondary;
  final SourceType sourceType;
  final String? note;
  final Map<String, dynamic> context;
  final DateTime? periodStart;
  final DateTime? periodEnd;
  final String? qualityLabel;

  const MetricDataPoint({
    this.metricId,
    required this.measuredAt,
    required this.valueNumeric,
    this.valueSecondary,
    this.sourceType = SourceType.manual,
    this.note,
    this.context = const {},
    this.periodStart,
    this.periodEnd,
    this.qualityLabel,
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
        context: (json['context'] as Map<String, dynamic>?) ?? const {},
        periodStart: _parseOptionalMetricDateTime(json['periodStart']),
        periodEnd: _parseOptionalMetricDateTime(json['periodEnd']),
        qualityLabel: json['qualityLabel'] as String?,
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

class MetricTrend {
  final String metricType;
  final String? unit;
  final List<MetricDataPoint> dataPoints;
  final String? disclaimer;

  const MetricTrend({
    required this.metricType,
    this.unit,
    required this.dataPoints,
    this.disclaimer,
  });

  factory MetricTrend.fromJson(Map<String, dynamic> json) => MetricTrend(
    metricType: json['metricType'] as String? ?? '',
    unit: json['unit'] as String?,
    dataPoints: (json['dataPoints'] as List<dynamic>? ?? [])
        .map((e) => MetricDataPoint.fromJson(e as Map<String, dynamic>))
        .toList(),
    disclaimer: json['disclaimer'] as String?,
  );

  double? get average {
    if (dataPoints.isEmpty || metricType == 'BLOOD_PRESSURE') return null;
    if (metricType == 'BLOOD_GLUCOSE') {
      final contexts = dataPoints
          .map((point) => point.context['measurementContext'])
          .whereType<String>()
          .toSet();
      if (contexts.length > 1) return null;
    }
    return dataPoints.map((p) => p.valueNumeric).reduce((a, b) => a + b) /
        dataPoints.length;
  }

  double? get trend {
    if (dataPoints.length < 2 || metricType == 'BLOOD_PRESSURE') return null;
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
        return 'Không hỗ trợ';
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
      case 'BLOOD_GLUCOSE':
        return MetricType.bloodSugar;
      case 'TEMPERATURE':
        return MetricType.temperature;
      case 'HEART_RATE':
      case 'MATERNAL_HEART_RATE':
        return MetricType.heartRate;
      case 'FETAL_MOVEMENT':
      case 'FETAL_MOVEMENT_COUNT':
      case 'FETAL_MOVEMENT_SESSION':
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
  final String metricCode;
  final double valueNumeric;
  final double? valueSecondary;
  final String unit;
  final DateTime measuredAt;
  final SourceType sourceType;
  final String? note;
  final DateTime createdAt;
  final Map<String, dynamic> context;
  final DateTime? periodStart;
  final DateTime? periodEnd;
  final String? qualityLabel;
  final String? disclaimer;
  final int? definitionVersion;

  const HealthMetricDetail({
    required this.id,
    required this.journeyId,
    required this.metricType,
    String? metricCode,
    required this.valueNumeric,
    this.valueSecondary,
    required this.unit,
    required this.measuredAt,
    required this.sourceType,
    this.note,
    required this.createdAt,
    this.context = const {},
    this.periodStart,
    this.periodEnd,
    this.qualityLabel,
    this.disclaimer,
    this.definitionVersion,
  }) : metricCode = metricCode ?? '';

  factory HealthMetricDetail.fromJson(Map<String, dynamic> json) {
    final code = json['metricType'] as String? ?? '';
    return HealthMetricDetail(
      id: (json['id'] ?? json['metricId']).toString(),
      journeyId: json['journeyId'].toString(),
      metricType: MetricTypeExtension.fromApi(code),
      metricCode: code,
      valueNumeric: (json['valueNumeric'] as num).toDouble(),
      valueSecondary: (json['valueSecondary'] as num?)?.toDouble(),
      unit: json['unit'] as String? ?? '',
      measuredAt: _parseMetricDateTime(json['measuredAt']),
      sourceType: SourceTypeExtension.fromApi(json['sourceType'] as String?),
      note: json['note'] as String?,
      createdAt: _parseMetricDateTime(
        json['createdAt'] ?? json['updatedAt'] ?? json['measuredAt'],
      ),
      context: (json['context'] as Map<String, dynamic>?) ?? const {},
      periodStart: _parseOptionalMetricDateTime(json['periodStart']),
      periodEnd: _parseOptionalMetricDateTime(json['periodEnd']),
      qualityLabel: json['qualityLabel'] as String?,
      disclaimer: json['disclaimer'] as String?,
      definitionVersion: (json['definitionVersion'] as num?)?.toInt(),
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
