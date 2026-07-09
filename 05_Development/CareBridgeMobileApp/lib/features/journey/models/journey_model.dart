/// Dashboard response from GET /api/v1/journeys/me/dashboard (UC-24)
class JourneyDashboard {
  final String? journeyId;
  final String?
  journeyType; // PREGNANCY | POSTPARTUM | BABY_CARE | PRE_PREGNANCY
  final String?
  status; // ACTIVE_PREGNANCY | ACTIVE_POSTPARTUM | BABY_CARE | NO_JOURNEY
  final int? pregnancyWeek;
  final int? trimester;
  final int? daysUntilDue;
  final DateTime? estimatedDueDate;
  final DateTime? lastMenstrualDate;
  final DateTime? startDate;

  const JourneyDashboard({
    this.journeyId,
    this.journeyType,
    this.status,
    this.pregnancyWeek,
    this.trimester,
    this.daysUntilDue,
    this.estimatedDueDate,
    this.lastMenstrualDate,
    this.startDate,
  });

  bool get hasActiveJourney => journeyId != null && status != 'NO_JOURNEY';
  bool get isPregnancy => journeyType == 'PREGNANCY';

  DateTime get _today {
    final now = DateTime.now();
    return DateTime(now.year, now.month, now.day);
  }

  int? get calculatedDaysUntilDue {
    if (daysUntilDue != null) return daysUntilDue;
    final dueDate = estimatedDueDate;
    if (dueDate == null) return null;
    final normalizedDueDate = DateTime(
      dueDate.year,
      dueDate.month,
      dueDate.day,
    );
    return normalizedDueDate.difference(_today).inDays;
  }

  int? get displayPregnancyWeek {
    if (pregnancyWeek != null) return pregnancyWeek;
    if (!isPregnancy) return null;

    if (lastMenstrualDate != null) {
      final lmp = DateTime(
        lastMenstrualDate!.year,
        lastMenstrualDate!.month,
        lastMenstrualDate!.day,
      );
      return (_today.difference(lmp).inDays / 7).floor().clamp(0, 42);
    }

    final daysLeft = calculatedDaysUntilDue;
    if (daysLeft == null) return null;
    return ((280 - daysLeft) / 7).floor().clamp(0, 42);
  }

  int? get displayTrimester {
    if (trimester != null) return trimester;
    final week = displayPregnancyWeek;
    if (week == null) return null;
    if (week <= 13) return 1;
    if (week <= 26) return 2;
    return 3;
  }

  double get pregnancyProgress {
    final week = displayPregnancyWeek;
    return (week != null && week > 0) ? (week / 40.0).clamp(0.0, 1.0) : 0.0;
  }

  String get weekLabel => displayPregnancyWeek != null
      ? 'Tuần $displayPregnancyWeek'
      : 'Chưa có tuần thai';

  String get fruitName {
    final w = displayPregnancyWeek;
    if (w == null) return 'em bé';
    if (w <= 8) return 'quả nho';
    if (w <= 12) return 'quả chanh';
    if (w <= 16) return 'quả bơ';
    if (w <= 20) return 'quả chuối';
    if (w <= 24) return 'quả dưa lưới';
    if (w <= 28) return 'quả cà tím';
    if (w <= 32) return 'quả bí ngô';
    if (w <= 36) return 'trái bưởi';
    return 'trái dưa hấu nhỏ';
  }

  String get fruitSizeNote {
    final w = displayPregnancyWeek;
    if (w == null) return 'đang được theo dõi theo ngày dự sinh của mẹ';
    if (w == 24) return 'dài khoảng 30cm và nặng 600g';
    if (w <= 12) return 'đang phát triển nhanh chóng';
    return 'đang lớn lên từng ngày';
  }

  String get phaseLabel {
    switch (journeyType) {
      case 'POSTPARTUM':
        return 'Sau sinh';
      case 'BABY_CARE':
        return 'Nuôi con';
      case 'PRE_PREGNANCY':
        return 'Chuẩn bị mang thai';
      default:
        return 'Mang thai';
    }
  }

  factory JourneyDashboard.fromJson(Map<String, dynamic> json) {
    return JourneyDashboard(
      journeyId: json['journeyId'] as String?,
      journeyType: json['journeyType'] as String?,
      status: json['status'] as String?,
      pregnancyWeek: json['pregnancyWeek'] as int?,
      trimester: json['trimester'] as int?,
      daysUntilDue: (json['daysUntilDue'] as num?)?.toInt(),
      estimatedDueDate: json['estimatedDueDate'] != null
          ? DateTime.parse(json['estimatedDueDate'] as String)
          : null,
      lastMenstrualDate: json['lastMenstrualDate'] != null
          ? DateTime.parse(json['lastMenstrualDate'] as String)
          : null,
      startDate: json['startDate'] != null
          ? DateTime.parse(json['startDate'] as String)
          : null,
    );
  }
}

enum JourneyType {
  prePregnancy,
  pregnancy,
  postpartum,
  babyCare;

  String toApiValue() {
    switch (this) {
      case JourneyType.prePregnancy:
        return 'PRE_PREGNANCY';
      case JourneyType.pregnancy:
        return 'PREGNANCY';
      case JourneyType.postpartum:
        return 'POSTPARTUM';
      case JourneyType.babyCare:
        return 'BABY_CARE';
    }
  }
}

class CreateJourneyRequest {
  final JourneyType journeyType;
  final String startDate;
  final String? lastMenstrualDate;
  final String? estimatedDueDate;
  final String? notes;

  const CreateJourneyRequest({
    required this.journeyType,
    required this.startDate,
    this.lastMenstrualDate,
    this.estimatedDueDate,
    this.notes,
  });

  Map<String, dynamic> toJson() => {
    'journeyType': journeyType.toApiValue(),
    'startDate': startDate,
    if (lastMenstrualDate != null) 'lastMenstrualDate': lastMenstrualDate,
    if (estimatedDueDate != null) 'estimatedDueDate': estimatedDueDate,
    if (notes != null) 'notes': notes,
  };
}

class CreateJourneyResponse {
  final String id;
  final String journeyType;
  final String status;
  final String startDate;
  final String? estimatedDueDate;
  final String? notes;
  final String createdAt;

  const CreateJourneyResponse({
    required this.id,
    required this.journeyType,
    required this.status,
    required this.startDate,
    this.estimatedDueDate,
    this.notes,
    required this.createdAt,
  });

  factory CreateJourneyResponse.fromJson(Map<String, dynamic> json) {
    return CreateJourneyResponse(
      id: json['id'] as String,
      journeyType: json['journeyType'] as String,
      status: json['status'] as String,
      startDate: json['startDate'] as String,
      estimatedDueDate: json['estimatedDueDate'] as String?,
      notes: json['notes'] as String?,
      createdAt: json['createdAt'] as String? ?? '',
    );
  }
}
