enum LogType { feeding, sleep, diaper, fever, vomiting, medicine, symptom }

extension LogTypeExtension on LogType {
  String get displayLabel {
    switch (this) {
      case LogType.feeding:
        return 'Cho bé ăn';
      case LogType.sleep:
        return 'Ngủ nghỉ';
      case LogType.diaper:
        return 'Thay tã';
      case LogType.fever:
        return 'Sốt';
      case LogType.vomiting:
        return 'Nôn trớ';
      case LogType.medicine:
        return 'Thuốc';
      case LogType.symptom:
        return 'Sức khỏe';
    }
  }

  String toApiValue() {
    switch (this) {
      case LogType.feeding:
        return 'FEEDING';
      case LogType.sleep:
        return 'SLEEP';
      case LogType.diaper:
        return 'DIAPER';
      case LogType.fever:
        return 'FEVER';
      case LogType.vomiting:
        return 'VOMITING';
      case LogType.medicine:
        return 'MEDICINE';
      case LogType.symptom:
        return 'SYMPTOM';
    }
  }

  static LogType fromApi(String? v) {
    switch (v?.trim().toUpperCase()) {
      case 'FEEDING':
        return LogType.feeding;
      case 'SLEEP':
        return LogType.sleep;
      case 'DIAPER':
        return LogType.diaper;
      case 'FEVER':
        return LogType.fever;
      case 'VOMITING':
        return LogType.vomiting;
      case 'MEDICINE':
        return LogType.medicine;
      case 'SYMPTOM':
        return LogType.symptom;
      default:
        return LogType.symptom;
    }
  }
}

class BabyDailyLog {
  final String id;
  final String babyId;
  final LogType logType;

  /// Preserves the server value so legacy types (for example FEVER/MEDICINE)
  /// are not rendered as the unrelated FEEDING type.
  final String? rawLogType;
  final DateTime? startedAt;
  final DateTime? endedAt;
  final DateTime? createdAt;
  final DateTime? updatedAt;
  final double? quantity;
  final String? unit;
  final String? note;

  const BabyDailyLog({
    required this.id,
    required this.babyId,
    required this.logType,
    this.rawLogType,
    this.startedAt,
    this.endedAt,
    this.createdAt,
    this.updatedAt,
    this.quantity,
    this.unit,
    this.note,
  });

  factory BabyDailyLog.fromJson(Map<String, dynamic> json) {
    final rawLogType = json['logType']?.toString();
    final id = json['babyLogId']?.toString() ?? json['id']?.toString() ?? '';
    final babyId = json['babyId']?.toString() ?? '';
    if (id.trim().isEmpty || babyId.trim().isEmpty) {
      throw const FormatException('Invalid baby daily-log identity');
    }
    return BabyDailyLog(
      id: id,
      babyId: babyId,
      logType: LogTypeExtension.fromApi(rawLogType),
      rawLogType: rawLogType,
      startedAt: json['startedAt'] != null
          ? DateTime.parse(json['startedAt'] as String)
          : null,
      endedAt: json['endedAt'] != null
          ? DateTime.parse(json['endedAt'] as String)
          : null,
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'] as String)
          : null,
      updatedAt: json['updatedAt'] != null
          ? DateTime.parse(json['updatedAt'] as String)
          : null,
      quantity: (json['quantity'] as num?)?.toDouble(),
      unit: json['unit'] as String?,
      note: json['note'] as String?,
    );
  }

  String get displayTypeLabel => displayLogTypeLabel(rawLogType);
}

String displayLogTypeLabel(String? value) {
  switch (value?.trim().toUpperCase()) {
    case 'FEEDING':
      return 'Cho bé ăn';
    case 'SLEEP':
      return 'Ngủ nghỉ';
    case 'DIAPER':
      return 'Thay tã';
    case 'FEVER':
      return 'Sốt';
    case 'VOMITING':
      return 'Nôn trớ';
    case 'MEDICINE':
      return 'Thuốc';
    case 'SYMPTOM':
      return 'Sức khỏe';
    default:
      return 'Khác';
  }
}

class UpdateBabyDailyLogRequest {
  final DateTime? startedAt;
  final DateTime? endedAt;
  final double? quantity;
  final String? unit;
  final String? note;

  const UpdateBabyDailyLogRequest({
    this.startedAt,
    this.endedAt,
    this.quantity,
    this.unit,
    this.note,
  });

  Map<String, dynamic> toJson() => {
    if (startedAt != null) 'startedAt': startedAt!.toUtc().toIso8601String(),
    if (endedAt != null) 'endedAt': endedAt!.toUtc().toIso8601String(),
    if (quantity != null) 'quantity': quantity,
    if (unit != null) 'unit': unit,
    if (note != null) 'note': note,
  };
}

class AddBabyDailyLogRequest {
  final LogType logType;
  final DateTime? startedAt;
  final DateTime? endedAt;
  final double? quantity;
  final String? unit;
  final String? note;

  const AddBabyDailyLogRequest({
    required this.logType,
    this.startedAt,
    this.endedAt,
    this.quantity,
    this.unit,
    this.note,
  });

  Map<String, dynamic> toJson() => {
    'logType': logType.toApiValue(),
    if (startedAt != null) 'startedAt': startedAt!.toUtc().toIso8601String(),
    if (endedAt != null) 'endedAt': endedAt!.toUtc().toIso8601String(),
    if (quantity != null) 'quantity': quantity,
    if (unit != null) 'unit': unit,
    if (note != null) 'note': note,
  };
}

class LogTypeSummary {
  final int count;
  final double? totalQuantity;
  final String? unit;
  final double? maxValue;
  final String? latestNote;

  const LogTypeSummary({
    required this.count,
    this.totalQuantity,
    this.unit,
    this.maxValue,
    this.latestNote,
  });

  factory LogTypeSummary.fromJson(Map<String, dynamic> json) {
    final rawCount = json['count'];
    if (rawCount is! num ||
        !rawCount.isFinite ||
        rawCount < 0 ||
        rawCount != rawCount.toInt()) {
      throw const FormatException('Invalid daily-log summary count');
    }
    return LogTypeSummary(
      count: rawCount.toInt(),
      totalQuantity: (json['totalQuantity'] as num?)?.toDouble(),
      unit: json['unit'] as String?,
      maxValue: (json['maxValue'] as num?)?.toDouble(),
      latestNote: json['latestNote'] as String?,
    );
  }
}

String formatSleepDuration(LogTypeSummary? summary) {
  if (summary == null) return '—';
  if (summary.count == 0) return '0h';

  final quantity = summary.totalQuantity;
  final unit = summary.unit?.trim().toLowerCase();
  if (quantity == null || unit == null || unit.isEmpty) return '—';

  final hours = switch (unit) {
    'h' || 'hr' || 'hrs' || 'hour' || 'hours' => quantity,
    'm' || 'min' || 'mins' || 'minute' || 'minutes' => quantity / 60,
    _ => null,
  };
  if (hours == null) return '—';
  final value = hours == hours.roundToDouble()
      ? hours.toStringAsFixed(0)
      : hours.toStringAsFixed(1);
  return '${value}h';
}

class BabyLogSummaryResponse {
  final String babyId;
  final String period;
  final DateTime? fromDate;
  final DateTime? toDate;
  final Map<String, LogTypeSummary> summaries;
  final String? aiInsight;

  const BabyLogSummaryResponse({
    required this.babyId,
    required this.period,
    this.fromDate,
    this.toDate,
    required this.summaries,
    this.aiInsight,
  });

  factory BabyLogSummaryResponse.fromJson(Map<String, dynamic> json) {
    final raw = json['summaries'] as Map<String, dynamic>? ?? {};
    final summaries = raw.map(
      (k, v) => MapEntry(
        k.toUpperCase(),
        LogTypeSummary.fromJson(v as Map<String, dynamic>),
      ),
    );
    return BabyLogSummaryResponse(
      babyId: json['babyId']?.toString() ?? '',
      period: json['period'] as String? ?? '24h',
      fromDate: json['fromDate'] != null
          ? DateTime.parse(json['fromDate'] as String)
          : null,
      toDate: json['toDate'] != null
          ? DateTime.parse(json['toDate'] as String)
          : null,
      summaries: summaries,
      aiInsight: json['aiInsight'] as String?,
    );
  }

  LogTypeSummary? get feeding => summaries['FEEDING'];
  LogTypeSummary? get sleep => summaries['SLEEP'];
  LogTypeSummary? get diaper => summaries['DIAPER'];
  LogTypeSummary? get symptom => summaries['SYMPTOM'];
}
