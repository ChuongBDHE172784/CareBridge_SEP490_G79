class GrowthMeasurement {
  final String id;
  final DateTime measuredAt;
  final double? weightKg;
  final double? heightCm;
  final double? headCircumferenceCm;
  final String? note;
  final String recordedBy;
  final String? recorderName;
  final int? ageInMonths;

  const GrowthMeasurement({
    required this.id,
    required this.measuredAt,
    this.weightKg,
    this.heightCm,
    this.headCircumferenceCm,
    this.note,
    required this.recordedBy,
    this.recorderName,
    this.ageInMonths,
  });

  factory GrowthMeasurement.fromJson(Map<String, dynamic> json) {
    return GrowthMeasurement(
      id: json['id'] ?? json['measurementId'] ?? '',
      measuredAt: DateTime.tryParse(json['measuredAt']?.toString() ?? '') ?? DateTime.now(),
      weightKg: (json['weightKg'] as num?)?.toDouble(),
      heightCm: (json['heightCm'] as num?)?.toDouble(),
      headCircumferenceCm: (json['headCircumferenceCm'] as num?)?.toDouble(),
      note: json['note'] as String?,
      recordedBy: json['recordedBy'] ?? '',
      recorderName: json['recorderName'] as String?,
      ageInMonths: json['ageInMonths'] as int?,
    );
  }
}
