class GrowthMeasurement {
  final String id;
  final DateTime measuredAt;
  final double? weightKg;
  final double? heightCm;
  final double? headCircumferenceCm;
  final String? note;
  final String? sourceType;
  final String recordedBy;
  final String? recorderName;
  final int? ageInMonths;
  final int? ageInDays;

  const GrowthMeasurement({
    required this.id,
    required this.measuredAt,
    this.weightKg,
    this.heightCm,
    this.headCircumferenceCm,
    this.note,
    this.sourceType,
    required this.recordedBy,
    this.recorderName,
    this.ageInMonths,
    this.ageInDays,
  });

  factory GrowthMeasurement.fromJson(Map<String, dynamic> json) {
    return GrowthMeasurement(
      id:
          (json['growthMeasurementId'] ??
                  json['id'] ??
                  json['measurementId'] ??
                  '')
              .toString(),
      measuredAt: DateTime.tryParse(
            (json['measuredDate'] ?? json['measuredAt'])?.toString() ?? '',
          ) ??
          DateTime.fromMillisecondsSinceEpoch(0, isUtc: true),
      weightKg: (json['weightKg'] as num?)?.toDouble(),
      heightCm: (json['heightCm'] as num?)?.toDouble(),
      headCircumferenceCm: (json['headCircumferenceCm'] as num?)?.toDouble(),
      note: json['note'] as String?,
      sourceType: json['sourceType'] as String?,
      recordedBy: (json['recordedBy'] ?? '').toString(),
      recorderName: json['recorderName'] as String?,
      ageInMonths: json['ageInMonths'] as int?,
      ageInDays: json['ageInDays'] as int?,
    );
  }
}
