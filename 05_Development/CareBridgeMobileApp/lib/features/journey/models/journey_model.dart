/// Dashboard response from GET /api/v1/journeys/me/dashboard (UC-24)
class JourneyDashboard {
  final String? journeyId;
  final String?
  journeyType; // PREGNANCY | POSTPARTUM | BABY_CARE | PRE_PREGNANCY
  final String?
  status; // ACTIVE_PREGNANCY | ACTIVE_POSTPARTUM | BABY_CARE | NO_JOURNEY
  final int? pregnancyWeek;
  /// Zero-based completed gestational week retained for legacy dashboard clients.
  /// This is deliberately distinct from [sourceWeekNumber], which is the
  /// one-based week used to select the checklist plan.
  final int? completedGestationalWeek;
  /// Remainder days (0 to 6) in the current completed gestational week.
  final int? completedGestationalDays;
  /// One-based source week returned by the server for checklist selection.
  final int? sourceWeekNumber;
  /// Server-selected WHO checklist plan for [sourceWeekNumber].
  final int? plan;
  final int? trimester;
  final int? daysUntilDue;
  final DateTime? estimatedDueDate;
  final DateTime? lastMenstrualDate;
  final DateTime? startDate;
  final int? version;
  final String? dateSource;
  final String? dateConfidence;
  /// Canonical dating authority: LMP or EDD.
  final String? datingBasis;
  /// Non-null when the server quarantined dating data pending confirmation.
  final String? datingQuarantineReason;
  final DateTime? canonicalLmp;
  final int? gestationalDatingRevision;
  final DateTime? gestationalDatingEffectiveAt;
  final PregnancyOutcome? pregnancyOutcome;
  final DateTime? pregnancyOutcomeDate;

  const JourneyDashboard({
    this.journeyId,
    this.journeyType,
    this.status,
    this.pregnancyWeek,
    this.completedGestationalWeek,
    this.completedGestationalDays,
    this.sourceWeekNumber,
    this.plan,
    this.trimester,
    this.daysUntilDue,
    this.estimatedDueDate,
    this.lastMenstrualDate,
    this.startDate,
    this.version,
    this.dateSource,
    this.dateConfidence,
    this.datingBasis,
    this.datingQuarantineReason,
    this.canonicalLmp,
    this.gestationalDatingRevision,
    this.gestationalDatingEffectiveAt,
    this.pregnancyOutcome,
    this.pregnancyOutcomeDate,
  });

  static int? calculatePregnancyWeek({
    DateTime? lastMenstrualDate,
    DateTime? estimatedDueDate,
    DateTime? today,
  }) {
    final referenceDate = today ?? DateTime.now();
    final normalizedToday = DateTime(
      referenceDate.year,
      referenceDate.month,
      referenceDate.day,
    );
    if (lastMenstrualDate != null) {
      final normalizedLmp = DateTime(
        lastMenstrualDate.year,
        lastMenstrualDate.month,
        lastMenstrualDate.day,
      );
      final days = normalizedToday.difference(normalizedLmp).inDays;
      if (days < 0) return null;
      return ((days / 7).floor() + 1).clamp(1, 42);
    }
    if (estimatedDueDate != null) {
      final normalizedDueDate = DateTime(
        estimatedDueDate.year,
        estimatedDueDate.month,
        estimatedDueDate.day,
      );
      final daysUntilDue = normalizedDueDate.difference(normalizedToday).inDays;
      final daysSinceLmp = 280 - daysUntilDue;
      if (daysSinceLmp < 0) return null;
      return ((daysSinceLmp / 7).floor() + 1).clamp(1, 42);
    }
    return null;
  }

  bool get hasActiveJourney => journeyId != null && status != 'NO_JOURNEY';
  bool get isPregnancy => journeyType == 'PREGNANCY';
  bool get isPrePregnancy => journeyType == 'PRE_PREGNANCY';
  bool get isPostpartum => journeyType == 'POSTPARTUM';
  bool get isMaternalLifecycle => isPregnancy || isPrePregnancy || isPostpartum;

  DateTime get _today {
    final now = DateTime.now();
    return DateTime(now.year, now.month, now.day);
  }

  int? get calculatedDaysUntilDue {
    if (datingQuarantineReason != null) return null;
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

  int? get effectiveDaysUntilDue => calculatedDaysUntilDue;

  int? get displayPregnancyWeek {
    // A quarantined response is explicitly fail-closed. Do not infer a week
    // from raw legacy dates while the server withholds its dating authority.
    if (datingQuarantineReason != null) return null;
    // Prefer sourceWeekNumber if available (1-based), then server-supplied pregnancyWeek (1-based)
    if (sourceWeekNumber != null) return sourceWeekNumber;
    if (pregnancyWeek != null) return pregnancyWeek;
    if (isPregnancy && datingBasis == null &&
        (completedGestationalWeek == null || sourceWeekNumber == null) &&
        plan == null && (lastMenstrualDate != null || estimatedDueDate != null)) {
      // The server intentionally omitted a dating authority for this
      // pregnancy. Raw dates are not sufficient to select a clinical week.
      return null;
    }
    // When any V2 dating metadata is present, a missing source week means the
    // server has not resolved dating; legacy date-only payloads may still use
    // the compatibility calculation below.
    if ((datingBasis != null || completedGestationalWeek != null || plan != null) &&
        sourceWeekNumber == null) {
      return null;
    }
    if (!isPregnancy) return null;

    return calculatePregnancyWeek(
      lastMenstrualDate: lastMenstrualDate,
      estimatedDueDate: estimatedDueDate,
      today: _today,
    );
  }

  int? get effectivePregnancyWeek => displayPregnancyWeek;

  /// Checklist-facing week. Unlike [displayPregnancyWeek], this value is
  /// never sourced from the legacy zero-based `pregnancyWeek` field.
  int? get displaySourceWeekNumber => sourceWeekNumber;

  /// Checklist-facing week. No fallback to legacy `pregnancyWeek` is allowed:
  /// the two fields have different semantics and only the server-owned source
  /// week may select a checklist plan.
  int? get effectiveSourceWeekNumber => sourceWeekNumber;

  /// Human-readable dating authority for UI and diagnostics.
  String? get gestationalDatingBasis => datingBasis;

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
        return 'Hậu sản';
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
      completedGestationalWeek: (json['completedGestationalWeek'] as num?)?.toInt(),
      completedGestationalDays: (json['completedGestationalDays'] as num?)?.toInt(),
      sourceWeekNumber: (json['sourceWeekNumber'] as num?)?.toInt(),
      plan: (json['plan'] as num?)?.toInt(),
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
      version: (json['version'] as num?)?.toInt(),
      dateSource: json['dateSource'] as String?,
      dateConfidence: json['dateConfidence'] as String?,
      datingBasis:
          (json['datingBasis'] ?? json['gestationalDatingBasis'])?.toString(),
      datingQuarantineReason:
          (json['datingQuarantineReason'] ??
                  json['gestationalDatingQuarantineReasonCode'] ??
                  json['gestationalDatingQuarantineReason'])
              ?.toString(),
      canonicalLmp: _parseDate(json['canonicalLmp']),
      gestationalDatingRevision:
          (json['gestationalDatingRevision'] as num?)?.toInt(),
      gestationalDatingEffectiveAt:
          _parseDateTime(json['gestationalDatingEffectiveAt']),
      pregnancyOutcome: json['pregnancyOutcome'] == null
          ? null
          : PregnancyOutcome.fromApiValue(json['pregnancyOutcome'] as String),
      pregnancyOutcomeDate: json['pregnancyOutcomeDate'] == null
          ? null
          : DateTime.parse(json['pregnancyOutcomeDate'] as String),
    );
  }

  static DateTime? _parseDate(dynamic value) {
    if (value == null) return null;
    return DateTime.tryParse(value.toString());
  }

  static DateTime? _parseDateTime(dynamic value) => _parseDate(value);
}

enum JourneyType {
  prePregnancy,
  pregnancy,
  postpartum,
  babyCare;

  bool get isMaternalLifecycle =>
      this == JourneyType.prePregnancy ||
      this == JourneyType.pregnancy ||
      this == JourneyType.postpartum;

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
  final String? datingBasis;
  final String? dateSource;
  final String? dateConfidence;
  final String? changeReason;
  final String? effectiveAt;
  final String? notes;

  const CreateJourneyRequest({
    required this.journeyType,
    required this.startDate,
    this.lastMenstrualDate,
    this.estimatedDueDate,
    this.datingBasis,
    this.dateSource,
    this.dateConfidence,
    this.changeReason,
    this.effectiveAt,
    this.notes,
  });

  Map<String, dynamic> toJson() {
    final hasDatingDate = lastMenstrualDate != null || estimatedDueDate != null;
    return {
      'journeyType': journeyType.toApiValue(),
      'startDate': startDate,
      if (lastMenstrualDate != null) 'lastMenstrualDate': lastMenstrualDate,
      if (estimatedDueDate != null) 'estimatedDueDate': estimatedDueDate,
      if (datingBasis != null) 'datingBasis': datingBasis,
      if (hasDatingDate)
        'dateSource': dateSource ?? 'SELF_REPORTED'
      else if (dateSource != null)
        'dateSource': dateSource,
      if (hasDatingDate)
        'dateConfidence': dateConfidence ?? 'ESTIMATED'
      else if (dateConfidence != null)
        'dateConfidence': dateConfidence,
      if (hasDatingDate)
        'changeReason': changeReason ?? 'INITIAL_SETUP'
      else if (changeReason != null)
        'changeReason': changeReason,
      if (hasDatingDate || effectiveAt != null)
        'effectiveAt': effectiveAt ?? DateTime.now().toUtc().toIso8601String(),
      if (notes != null) 'notes': notes,
    };
  }
}

class CreateJourneyResponse {
  final String id;
  final String journeyType;
  final String status;
  final String startDate;
  final String? lastMenstrualDate;
  final String? estimatedDueDate;
  final String? notes;
  final String createdAt;
  final int? version;
  final String? dateSource;
  final String? dateConfidence;
  final String? datingBasis;
  final String? canonicalLmp;
  final int? completedGestationalWeek;
  final int? completedGestationalDays;
  final int? sourceWeekNumber;
  final int? plan;
  final String? datingQuarantineReason;

  const CreateJourneyResponse({
    required this.id,
    required this.journeyType,
    required this.status,
    required this.startDate,
    this.lastMenstrualDate,
    this.estimatedDueDate,
    this.notes,
    required this.createdAt,
    this.version,
    this.dateSource,
    this.dateConfidence,
    this.datingBasis,
    this.canonicalLmp,
    this.completedGestationalWeek,
    this.completedGestationalDays,
    this.sourceWeekNumber,
    this.plan,
    this.datingQuarantineReason,
  });

  factory CreateJourneyResponse.fromJson(Map<String, dynamic> json) {
    return CreateJourneyResponse(
      id: json['id'] as String,
      journeyType: json['journeyType'] as String,
      status: json['status'] as String,
      startDate: json['startDate'] as String,
      lastMenstrualDate: json['lastMenstrualDate'] as String?,
      estimatedDueDate: json['estimatedDueDate'] as String?,
      notes: json['notes'] as String?,
      createdAt: json['createdAt'] as String? ?? '',
      version: (json['version'] as num?)?.toInt(),
      dateSource: json['dateSource'] as String?,
      dateConfidence: json['dateConfidence'] as String?,
      datingBasis:
          (json['datingBasis'] ?? json['gestationalDatingBasis'])?.toString(),
      canonicalLmp: json['canonicalLmp']?.toString(),
      completedGestationalWeek:
          (json['completedGestationalWeek'] as num?)?.toInt(),
      completedGestationalDays:
          (json['completedGestationalDays'] as num?)?.toInt(),
      sourceWeekNumber: (json['sourceWeekNumber'] as num?)?.toInt(),
      plan: (json['plan'] as num?)?.toInt(),
      datingQuarantineReason:
          (json['datingQuarantineReason'] ??
                  json['gestationalDatingQuarantineReasonCode'] ??
                  json['gestationalDatingQuarantineReason'])
              ?.toString(),
    );
  }
}

class UpdateJourneyRequest {
  final JourneyType? journeyType;
  final String? startDate;
  final String? lastMenstrualDate;
  final String? estimatedDueDate;
  final String? datingBasis;
  final String? dateSource;
  final String? dateConfidence;
  final String? changeReason;
  final String? effectiveAt;
  final String? notes;

  const UpdateJourneyRequest({
    this.journeyType,
    this.startDate,
    this.lastMenstrualDate,
    this.estimatedDueDate,
    this.datingBasis,
    this.dateSource,
    this.dateConfidence,
    this.changeReason,
    this.effectiveAt,
    this.notes,
  });

  Map<String, dynamic> toJson() {
    final hasDatingDate = lastMenstrualDate != null || estimatedDueDate != null;
    return {
      if (journeyType != null) 'journeyType': journeyType!.toApiValue(),
      if (startDate != null) 'startDate': startDate,
      if (lastMenstrualDate != null) 'lastMenstrualDate': lastMenstrualDate,
      if (estimatedDueDate != null) 'estimatedDueDate': estimatedDueDate,
      if (datingBasis != null) 'datingBasis': datingBasis,
      if (hasDatingDate)
        'dateSource': dateSource ?? 'SELF_REPORTED'
      else if (dateSource != null)
        'dateSource': dateSource,
      if (hasDatingDate)
        'dateConfidence': dateConfidence ?? 'ESTIMATED'
      else if (dateConfidence != null)
        'dateConfidence': dateConfidence,
      if (hasDatingDate)
        'changeReason': changeReason ?? 'DATE_CORRECTION'
      else if (changeReason != null)
        'changeReason': changeReason,
      if (hasDatingDate || effectiveAt != null)
        'effectiveAt': effectiveAt ?? DateTime.now().toUtc().toIso8601String(),
      if (notes != null) 'notes': notes,
    };
  }
}

class JourneyTransition {
  final String transitionId;
  final String eventType;
  final String? fromStage;
  final String? toStage;
  final List<String> changedFields;
  final String? source;
  final String? confidence;
  final String? reason;
  final DateTime effectiveAt;
  final DateTime recordedAt;
  final int journeyVersion;

  const JourneyTransition({
    required this.transitionId,
    required this.eventType,
    this.fromStage,
    this.toStage,
    required this.changedFields,
    this.source,
    this.confidence,
    this.reason,
    required this.effectiveAt,
    required this.recordedAt,
    required this.journeyVersion,
  });

  factory JourneyTransition.fromJson(Map<String, dynamic> json) {
    return JourneyTransition(
      transitionId: json['transitionId'] as String,
      eventType: json['eventType'] as String,
      fromStage: json['fromStage'] as String?,
      toStage: json['toStage'] as String?,
      changedFields:
          (json['changedFields'] as List<dynamic>?)
              ?.map((field) => field.toString())
              .toList(growable: false) ??
          const [],
      source: json['source'] as String?,
      confidence: json['confidence'] as String?,
      reason: json['reason'] as String?,
      effectiveAt: DateTime.parse(json['effectiveAt'] as String),
      recordedAt: DateTime.parse(json['recordedAt'] as String),
      journeyVersion: (json['journeyVersion'] as num).toInt(),
    );
  }
}

class JourneyTimelineItem {
  const JourneyTimelineItem({
    required this.itemType,
    required this.itemId,
    required this.occurredAt,
    required this.recordedAt,
    this.eventType,
    this.fromStage,
    this.toStage,
    this.reason,
    this.riskLevel,
    this.stage,
    this.sourceIntakeId,
    this.sourceEmergencyId,
    this.originAction,
  });

  final String itemType;
  final String itemId;
  final DateTime occurredAt;
  final DateTime recordedAt;
  final String? eventType;
  final String? fromStage;
  final String? toStage;
  final String? reason;
  final String? riskLevel;
  final String? stage;
  final String? sourceIntakeId;
  final String? sourceEmergencyId;
  final String? originAction;

  bool get isSafetyOutcome => itemType == 'SAFETY_OUTCOME';

  factory JourneyTimelineItem.fromJson(Map<String, dynamic> json) {
    final itemType = json['itemType']?.toString() ?? '';
    final itemId = json['itemId']?.toString() ?? '';
    final occurredAt = DateTime.tryParse(json['occurredAt']?.toString() ?? '');
    final recordedAt = DateTime.tryParse(json['recordedAt']?.toString() ?? '');
    if (!{'LIFECYCLE_TRANSITION', 'SAFETY_OUTCOME'}.contains(itemType) ||
        itemId.isEmpty ||
        occurredAt == null ||
        recordedAt == null) {
      throw const FormatException('Invalid journey timeline item');
    }
    return JourneyTimelineItem(
      itemType: itemType,
      itemId: itemId,
      occurredAt: occurredAt,
      recordedAt: recordedAt,
      eventType: json['eventType']?.toString(),
      fromStage: json['fromStage']?.toString(),
      toStage: json['toStage']?.toString(),
      reason: json['reason']?.toString(),
      riskLevel: json['riskLevel']?.toString(),
      stage: json['stage']?.toString(),
      sourceIntakeId: json['sourceIntakeId']?.toString(),
      sourceEmergencyId: json['sourceEmergencyId']?.toString(),
      originAction: json['originAction']?.toString(),
    );
  }
}

class JourneyTimelinePage {
  const JourneyTimelinePage({
    required this.items,
    required this.page,
    required this.size,
    required this.totalElements,
    required this.totalPages,
  });

  final List<JourneyTimelineItem> items;
  final int page;
  final int size;
  final int totalElements;
  final int totalPages;

  factory JourneyTimelinePage.fromJson(Map<String, dynamic> json) {
    final items = (json['items'] as List<dynamic>? ?? const [])
        .whereType<Map<String, dynamic>>()
        .map(JourneyTimelineItem.fromJson)
        .toList(growable: false);
    return JourneyTimelinePage(
      items: List.unmodifiable(items),
      page: (json['page'] as num?)?.toInt() ?? 0,
      size: (json['size'] as num?)?.toInt() ?? items.length,
      totalElements: (json['totalElements'] as num?)?.toInt() ?? items.length,
      totalPages: (json['totalPages'] as num?)?.toInt() ?? 1,
    );
  }
}

enum PregnancyOutcome {
  ongoing,
  liveBirth,
  pregnancyLoss;

  String get apiValue => switch (this) {
    PregnancyOutcome.ongoing => 'ONGOING',
    PregnancyOutcome.liveBirth => 'LIVE_BIRTH',
    PregnancyOutcome.pregnancyLoss => 'PREGNANCY_LOSS',
  };

  String get displayLabel => switch (this) {
    PregnancyOutcome.ongoing => 'Thai kỳ vẫn đang tiếp diễn',
    PregnancyOutcome.liveBirth => 'Em bé đã chào đời',
    PregnancyOutcome.pregnancyLoss => 'Thai kỳ đã kết thúc',
  };

  bool get requiresDate => this == PregnancyOutcome.liveBirth;

  bool get transitionsToPostpartum =>
      this == PregnancyOutcome.liveBirth ||
      this == PregnancyOutcome.pregnancyLoss;

  static PregnancyOutcome fromApiValue(String value) => switch (value) {
    'ONGOING' => PregnancyOutcome.ongoing,
    'LIVE_BIRTH' => PregnancyOutcome.liveBirth,
    'PREGNANCY_LOSS' => PregnancyOutcome.pregnancyLoss,
    'UNKNOWN' => PregnancyOutcome.ongoing,
    'STILLBIRTH' => PregnancyOutcome.pregnancyLoss,
    _ => throw FormatException('Unsupported pregnancy outcome: $value'),
  };
}

class RecordPregnancyOutcomeRequest {
  const RecordPregnancyOutcomeRequest({
    required this.submissionId,
    required this.expectedJourneyVersion,
    required this.outcomeType,
    this.outcomeDate,
    required this.source,
    required this.reason,
    required this.effectiveAt,
    this.correction = false,
  });

  final String submissionId;
  final int expectedJourneyVersion;
  final PregnancyOutcome outcomeType;
  final DateTime? outcomeDate;
  final String source;
  final String reason;
  final DateTime effectiveAt;
  final bool correction;

  Map<String, dynamic> toJson() => {
    'submissionId': submissionId,
    'expectedJourneyVersion': expectedJourneyVersion,
    'outcomeType': outcomeType.apiValue,
    if (outcomeDate != null) 'outcomeDate': _formatOutcomeDate(outcomeDate!),
    'source': source,
    'reason': reason,
    'effectiveAt': effectiveAt.toUtc().toIso8601String(),
    'correction': correction,
  };

  static String _formatOutcomeDate(DateTime value) {
    final year = value.year.toString().padLeft(4, '0');
    final month = value.month.toString().padLeft(2, '0');
    final day = value.day.toString().padLeft(2, '0');
    return '$year-$month-$day';
  }
}

class PregnancyOutcomeResult {
  const PregnancyOutcomeResult({
    required this.evidenceId,
    required this.journeyId,
    required this.outcomeType,
    this.outcomeDate,
    required this.journeyType,
    required this.journeyVersion,
    this.transitionId,
    required this.revisionNumber,
  });

  final String evidenceId;
  final String journeyId;
  final PregnancyOutcome outcomeType;
  final DateTime? outcomeDate;
  final String journeyType;
  final int journeyVersion;
  final String? transitionId;
  final int revisionNumber;

  factory PregnancyOutcomeResult.fromJson(Map<String, dynamic> json) {
    return PregnancyOutcomeResult(
      evidenceId: json['evidenceId'] as String,
      journeyId: json['journeyId'] as String,
      outcomeType: PregnancyOutcome.fromApiValue(json['outcomeType'] as String),
      outcomeDate: json['outcomeDate'] == null
          ? null
          : DateTime.parse(json['outcomeDate'] as String),
      journeyType: json['journeyType'] as String,
      journeyVersion: (json['journeyVersion'] as num).toInt(),
      transitionId: json['transitionId'] as String?,
      revisionNumber: (json['revisionNumber'] as num).toInt(),
    );
  }
}
