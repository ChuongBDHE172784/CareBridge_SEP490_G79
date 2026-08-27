import '../../../core/network/api_client.dart';
import '../../healthRecords/models/health_metric_model.dart';
import '../../journey/models/journey_model.dart';

class FamilyHomeService {
  static final FamilyHomeService instance = FamilyHomeService._();

  FamilyHomeService._();

  Future<FamilyHomeSnapshot> loadSnapshot({String? selectedCareGroupId}) async {
    final response = await apiGet(
      '/api/v1/family/dashboard',
      queryParams: {'selectedCareGroupId': ?selectedCareGroupId},
    );
    return FamilyHomeSnapshot.fromJson(_requiredMap(response, 'data'));
  }

  Future<MetricTrend> loadQuickNoteHistory({
    required String careGroupId,
    required String metricType,
    required DateTime from,
    required DateTime to,
  }) async {
    final response = await apiGet(
      '/api/v1/care-groups/$careGroupId/quick-notes',
      queryParams: {
        'metricType': metricType,
        'from': from.toUtc().toIso8601String(),
        'to': to.toUtc().toIso8601String(),
      },
    );
    return MetricTrend.fromJson(_requiredMap(response, 'data'));
  }
}

class FamilyHomeSnapshot {
  const FamilyHomeSnapshot({
    required this.groups,
    required this.globalAggregate,
    required this.selectedCareGroupId,
    required this.selectedGroupDetail,
  });

  final List<FamilyHomeGroup> groups;
  final FamilyHomeAggregate globalAggregate;
  final String? selectedCareGroupId;
  final FamilyHomeGroupDetail? selectedGroupDetail;

  factory FamilyHomeSnapshot.fromJson(Map<String, dynamic> json) {
    final selectedDetail = json['selectedGroupDetail'];
    return FamilyHomeSnapshot(
      groups: _requiredMapList(
        json,
        'groups',
      ).map(FamilyHomeGroup.fromJson).toList(growable: false),
      globalAggregate: FamilyHomeAggregate.fromJson(
        _requiredMap(json, 'globalAggregate'),
      ),
      selectedCareGroupId: _optionalString(json, 'selectedCareGroupId'),
      selectedGroupDetail: selectedDetail == null
          ? null
          : FamilyHomeGroupDetail.fromJson(
              _asMap(selectedDetail, 'selectedGroupDetail'),
            ),
    );
  }
}

class FamilyHomeAggregate {
  const FamilyHomeAggregate({
    required this.overdue,
    required this.dueSoon,
    required this.inProgress,
    required this.alerts,
  });

  final int overdue;
  final int dueSoon;
  final int inProgress;
  final int alerts;

  factory FamilyHomeAggregate.fromJson(Map<String, dynamic> json) {
    return FamilyHomeAggregate(
      overdue: _requiredInt(json, 'overdue'),
      dueSoon: _requiredInt(json, 'dueSoon'),
      inProgress: _requiredInt(json, 'inProgress'),
      alerts: _requiredInt(json, 'alerts'),
    );
  }
}

class FamilyHomeGroup {
  const FamilyHomeGroup({
    required this.id,
    required this.name,
    required this.joinedAt,
    required this.lastActivityAt,
    required this.relationshipRole,
    required this.customRelationshipRole,
    required this.permissionScope,
    required this.aggregate,
  });

  final String id;
  final String name;
  final DateTime? joinedAt;
  final DateTime? lastActivityAt;
  final String? relationshipRole;
  final String? customRelationshipRole;
  final FamilyHomePermission permissionScope;
  final FamilyHomeAggregate aggregate;

  factory FamilyHomeGroup.fromJson(Map<String, dynamic> json) {
    return FamilyHomeGroup(
      id: _requiredString(json, 'id'),
      name: _requiredString(json, 'name'),
      joinedAt: _optionalDateTime(json, 'joinedAt'),
      lastActivityAt: _optionalDateTime(json, 'lastActivityAt'),
      relationshipRole: _optionalString(json, 'relationshipRole'),
      customRelationshipRole: _optionalString(json, 'customRelationshipRole'),
      permissionScope: FamilyHomePermission.fromJson(
        _requiredMap(json, 'permissionScope'),
      ),
      aggregate: FamilyHomeAggregate.fromJson(_requiredMap(json, 'aggregate')),
    );
  }
}

class FamilyMotherJourney {
  const FamilyMotherJourney({
    this.journeyId,
    this.journeyType,
    this.status,
    this.estimatedDueDate,
    this.lastMenstrualDate,
    this.startDate,
    this.pregnancyWeek,
    this.completedGestationalWeek,
    this.completedGestationalDays,
    this.sourceWeekNumber,
    this.plan,
    this.trimester,
    this.daysUntilDue,
    this.dateSource,
    this.dateConfidence,
    this.datingBasis,
    this.datingQuarantineReason,
    this.canonicalLmp,
    this.gestationalDatingRevision,
    this.gestationalDatingEffectiveAt,
  });

  final String? journeyId;
  final String? journeyType;
  final String? status;
  final DateTime? estimatedDueDate;
  final DateTime? lastMenstrualDate;
  final DateTime? startDate;
  final int? pregnancyWeek;
  final int? completedGestationalWeek;
  final int? completedGestationalDays;
  final int? sourceWeekNumber;
  final int? plan;
  final int? trimester;
  final int? daysUntilDue;
  final String? dateSource;
  final String? dateConfidence;
  final String? datingBasis;
  final String? datingQuarantineReason;
  final DateTime? canonicalLmp;
  final int? gestationalDatingRevision;
  final DateTime? gestationalDatingEffectiveAt;

  factory FamilyMotherJourney.fromJson(Map<String, dynamic> json) {
    final dashboard = JourneyDashboard.fromJson(json);
    return FamilyMotherJourney(
      journeyId: dashboard.journeyId,
      journeyType: dashboard.journeyType,
      status: dashboard.status,
      estimatedDueDate: dashboard.estimatedDueDate,
      lastMenstrualDate: dashboard.lastMenstrualDate,
      startDate: dashboard.startDate,
      pregnancyWeek: dashboard.pregnancyWeek,
      completedGestationalWeek: dashboard.completedGestationalWeek,
      completedGestationalDays: dashboard.completedGestationalDays,
      sourceWeekNumber: dashboard.sourceWeekNumber,
      plan: dashboard.plan,
      trimester: dashboard.trimester,
      daysUntilDue: dashboard.daysUntilDue,
      dateSource: dashboard.dateSource,
      dateConfidence: dashboard.dateConfidence,
      datingBasis: dashboard.datingBasis,
      datingQuarantineReason: dashboard.datingQuarantineReason,
      canonicalLmp: dashboard.canonicalLmp,
      gestationalDatingRevision: dashboard.gestationalDatingRevision,
      gestationalDatingEffectiveAt: dashboard.gestationalDatingEffectiveAt,
    );
  }

  JourneyDashboard toJourneyDashboard() {
    return JourneyDashboard(
      journeyId: journeyId,
      journeyType: journeyType ?? 'PREGNANCY',
      status: status ?? 'ACTIVE_PREGNANCY',
      estimatedDueDate: estimatedDueDate,
      lastMenstrualDate: lastMenstrualDate,
      startDate: startDate,
      pregnancyWeek: pregnancyWeek,
      completedGestationalWeek: completedGestationalWeek,
      completedGestationalDays: completedGestationalDays,
      sourceWeekNumber: sourceWeekNumber,
      plan: plan,
      trimester: trimester,
      daysUntilDue: daysUntilDue,
      dateSource: dateSource,
      dateConfidence: dateConfidence,
      datingBasis: datingBasis,
      datingQuarantineReason: datingQuarantineReason,
      canonicalLmp: canonicalLmp,
      gestationalDatingRevision: gestationalDatingRevision,
      gestationalDatingEffectiveAt: gestationalDatingEffectiveAt,
    );
  }
}

class FamilyHomeGroupDetail {
  const FamilyHomeGroupDetail({
    required this.careGroupId,
    required this.motherDisplayName,
    this.motherJourney,
    required this.todayReminders,
    required this.alerts,
    required this.memberCount,
    required this.members,
    required this.relationshipRole,
    required this.customRelationshipRole,
    required this.permissionScope,
    required this.sharedDataSummary,
    this.healthMetricSummaries = const [],
  });

  final String careGroupId;
  final String motherDisplayName;
  final FamilyMotherJourney? motherJourney;
  final List<FamilyHomeTodayReminder> todayReminders;
  final List<FamilyHomeAlert> alerts;
  final int memberCount;
  final List<FamilyHomeMember> members;
  final String? relationshipRole;
  final String? customRelationshipRole;
  final FamilyHomePermission permissionScope;
  final FamilyHomeSharedDataSummary sharedDataSummary;
  final List<FamilyHomeHealthMetricSummary> healthMetricSummaries;

  factory FamilyHomeGroupDetail.fromJson(Map<String, dynamic> json) {
    final rawJourney = json['motherJourney'];
    return FamilyHomeGroupDetail(
      careGroupId: _requiredString(json, 'careGroupId'),
      motherDisplayName: _requiredString(json, 'motherDisplayName'),
      motherJourney: rawJourney == null
          ? null
          : FamilyMotherJourney.fromJson(_asMap(rawJourney, 'motherJourney')),
      todayReminders: _requiredMapList(
        json,
        'todayReminders',
      ).map(FamilyHomeTodayReminder.fromJson).toList(growable: false),
      alerts: _requiredMapList(
        json,
        'alerts',
      ).map(FamilyHomeAlert.fromJson).toList(growable: false),
      memberCount: _requiredInt(json, 'memberCount'),
      members: _requiredMapList(
        json,
        'members',
      ).map(FamilyHomeMember.fromJson).toList(growable: false),
      relationshipRole: _optionalString(json, 'relationshipRole'),
      customRelationshipRole: _optionalString(json, 'customRelationshipRole'),
      permissionScope: FamilyHomePermission.fromJson(
        _requiredMap(json, 'permissionScope'),
      ),
      sharedDataSummary: FamilyHomeSharedDataSummary.fromJson(
        _requiredMap(json, 'sharedDataSummary'),
      ),
      healthMetricSummaries: _optionalMapList(
        json,
        'healthMetricSummaries',
      ).map(FamilyHomeHealthMetricSummary.fromJson).toList(growable: false),
    );
  }
}

class FamilyHomeTodayReminder {
  const FamilyHomeTodayReminder({
    required this.id,
    required this.title,
    required this.type,
    required this.status,
    required this.scheduledAt,
    required this.dueAt,
    required this.snoozedUntil,
    required this.priority,
  });

  final String id;
  final String title;
  final String type;
  final String status;
  final DateTime? scheduledAt;
  final DateTime? dueAt;
  final DateTime? snoozedUntil;
  final int priority;

  factory FamilyHomeTodayReminder.fromJson(Map<String, dynamic> json) {
    return FamilyHomeTodayReminder(
      id: _requiredString(json, 'id'),
      title: _requiredString(json, 'title'),
      type: _requiredString(json, 'type'),
      status: _requiredString(json, 'status'),
      scheduledAt: _optionalDateTime(json, 'scheduledAt'),
      dueAt: _optionalDateTime(json, 'dueAt'),
      snoozedUntil: _optionalDateTime(json, 'snoozedUntil'),
      priority: _requiredInt(json, 'priority'),
    );
  }
}

class FamilyHomeAlert {
  const FamilyHomeAlert({
    required this.id,
    required this.careGroupId,
    required this.title,
    required this.body,
    required this.createdAt,
    required this.read,
  });

  final String id;
  final String careGroupId;
  final String title;
  final String body;
  final DateTime? createdAt;
  final bool read;

  factory FamilyHomeAlert.fromJson(Map<String, dynamic> json) {
    return FamilyHomeAlert(
      id: _requiredString(json, 'id'),
      careGroupId: _requiredString(json, 'careGroupId'),
      title: _requiredString(json, 'title'),
      body: _requiredString(json, 'body'),
      createdAt: _optionalDateTime(json, 'createdAt'),
      read: _requiredBool(json, 'read'),
    );
  }
}

class FamilyHomeMember {
  const FamilyHomeMember({
    required this.memberId,
    required this.userId,
    required this.displayName,
    this.avatarUrl,
    required this.systemRole,
    required this.relationshipRole,
    required this.customRelationshipRole,
    required this.joinedAt,
  });

  final String memberId;
  final String userId;
  final String displayName;
  final String? avatarUrl;
  final String? systemRole;
  final String? relationshipRole;
  final String? customRelationshipRole;
  final DateTime? joinedAt;

  factory FamilyHomeMember.fromJson(Map<String, dynamic> json) {
    return FamilyHomeMember(
      memberId: _requiredString(json, 'memberId'),
      userId: _requiredString(json, 'userId'),
      displayName: _requiredString(json, 'displayName'),
      avatarUrl:
          _optionalString(json, 'avatarUrl') ??
          _optionalString(json, 'profilePictureUrl'),
      systemRole: _optionalString(json, 'systemRole'),
      relationshipRole: _optionalString(json, 'relationshipRole'),
      customRelationshipRole: _optionalString(json, 'customRelationshipRole'),
      joinedAt: _optionalDateTime(json, 'joinedAt'),
    );
  }
}

class FamilyHomePermission {
  const FamilyHomePermission({
    required this.calendar,
    required this.logs,
    required this.alerts,
    this.checklistView = false,
    required this.records,
    this.quickNotes = false,
    this.quickNoteWeight = false,
    this.quickNoteHydration = false,
    this.quickNoteEpds = false,
    this.quickNoteFetalMovement = false,
    this.quickNoteBloodPressure = false,
    this.quickNoteBloodGlucose = false,
  });

  final bool calendar;
  final bool logs;
  final bool alerts;
  final bool checklistView;
  final bool records;
  final bool quickNotes;
  final bool quickNoteWeight;
  final bool quickNoteHydration;
  final bool quickNoteEpds;
  final bool quickNoteFetalMovement;
  final bool quickNoteBloodPressure;
  final bool quickNoteBloodGlucose;

  int get sharedHealthMetricCount {
    if (!quickNotes) return 0;
    return [
      quickNoteWeight,
      quickNoteFetalMovement,
      quickNoteBloodPressure,
      quickNoteHydration,
      quickNoteEpds,
      quickNoteBloodGlucose,
    ].where((shared) => shared).length;
  }

  factory FamilyHomePermission.fromJson(Map<String, dynamic> json) {
    return FamilyHomePermission(
      calendar: _requiredBool(json, 'calendar'),
      logs: _requiredBool(json, 'logs'),
      alerts: _requiredBool(json, 'alerts'),
      checklistView: json['checklistView'] as bool? ?? false,
      records: _requiredBool(json, 'records'),
      quickNotes: json['quickNotes'] as bool? ?? false,
      quickNoteWeight: json['quickNoteWeight'] as bool? ?? false,
      quickNoteHydration: json['quickNoteHydration'] as bool? ?? false,
      quickNoteEpds: json['quickNoteEpds'] as bool? ?? false,
      quickNoteFetalMovement: json['quickNoteFetalMovement'] as bool? ?? false,
      quickNoteBloodPressure: json['quickNoteBloodPressure'] as bool? ?? false,
      quickNoteBloodGlucose: json['quickNoteBloodGlucose'] as bool? ?? false,
    );
  }
}

class FamilyHomeHealthMetricSummary {
  const FamilyHomeHealthMetricSummary({
    required this.metricType,
    required this.valueNumeric,
    required this.valueSecondary,
    required this.unit,
    required this.measuredAt,
    required this.measurementContext,
    required this.recordCount,
  });

  final String metricType;
  final double? valueNumeric;
  final double? valueSecondary;
  final String? unit;
  final DateTime? measuredAt;
  final String? measurementContext;
  final int recordCount;

  bool get hasData => valueNumeric != null && measuredAt != null;

  String? get valueDisplay {
    final primary = valueNumeric;
    if (primary == null) return null;
    if (valueSecondary != null) {
      return '${primary.toStringAsFixed(0)}/${valueSecondary!.toStringAsFixed(0)}';
    }
    return primary % 1 == 0
        ? primary.toStringAsFixed(0)
        : primary.toStringAsFixed(1);
  }

  factory FamilyHomeHealthMetricSummary.fromJson(Map<String, dynamic> json) {
    return FamilyHomeHealthMetricSummary(
      metricType: _requiredString(json, 'metricType'),
      valueNumeric: _optionalDouble(json, 'valueNumeric'),
      valueSecondary: _optionalDouble(json, 'valueSecondary'),
      unit: _optionalString(json, 'unit'),
      measuredAt: _optionalDateTime(json, 'measuredAt'),
      measurementContext: _optionalString(json, 'measurementContext'),
      recordCount: _optionalInt(json, 'recordCount') ?? 0,
    );
  }
}

class FamilyHomeSharedDataSummary {
  const FamilyHomeSharedDataSummary({
    required this.totalItems,
    required this.categories,
  });

  final int totalItems;
  final List<FamilyHomeSharedDataCategory> categories;

  factory FamilyHomeSharedDataSummary.fromJson(Map<String, dynamic> json) {
    return FamilyHomeSharedDataSummary(
      totalItems: _requiredInt(json, 'totalItems'),
      categories: _requiredMapList(
        json,
        'categories',
      ).map(FamilyHomeSharedDataCategory.fromJson).toList(growable: false),
    );
  }
}

class FamilyHomeSharedDataCategory {
  const FamilyHomeSharedDataCategory({
    required this.category,
    required this.permitted,
    required this.itemCount,
  });

  final String category;
  final bool permitted;
  final int itemCount;

  factory FamilyHomeSharedDataCategory.fromJson(Map<String, dynamic> json) {
    return FamilyHomeSharedDataCategory(
      category: _requiredString(json, 'category'),
      permitted: _requiredBool(json, 'permitted'),
      itemCount: _requiredInt(json, 'itemCount'),
    );
  }
}

Map<String, dynamic> _requiredMap(Map<String, dynamic> json, String key) {
  return _asMap(json[key], key);
}

Map<String, dynamic> _asMap(Object? value, String key) {
  if (value is Map<String, dynamic>) return value;
  throw FormatException('Expected object for "$key".');
}

List<Map<String, dynamic>> _requiredMapList(
  Map<String, dynamic> json,
  String key,
) {
  final value = json[key];
  if (value is! List) throw FormatException('Expected list for "$key".');
  return value.map((item) => _asMap(item, key)).toList(growable: false);
}

List<Map<String, dynamic>> _optionalMapList(
  Map<String, dynamic> json,
  String key,
) {
  final value = json[key];
  if (value == null) return const [];
  if (value is! List) throw FormatException('Expected list for "$key".');
  return value.map((item) => _asMap(item, key)).toList(growable: false);
}

String _requiredString(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is String) return value;
  throw FormatException('Expected string for "$key".');
}

String? _optionalString(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value == null) return null;
  if (value is String) return value;
  throw FormatException('Expected nullable string for "$key".');
}

int _requiredInt(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is num) return value.toInt();
  throw FormatException('Expected number for "$key".');
}

int? _optionalInt(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value == null) return null;
  if (value is num) return value.toInt();
  throw FormatException('Expected nullable number for "$key".');
}

double? _optionalDouble(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value == null) return null;
  if (value is num) return value.toDouble();
  throw FormatException('Expected nullable number for "$key".');
}

bool _requiredBool(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value is bool) return value;
  throw FormatException('Expected boolean for "$key".');
}

DateTime? _optionalDateTime(Map<String, dynamic> json, String key) {
  final value = json[key];
  if (value == null) return null;
  if (value is! String) {
    throw FormatException('Expected nullable date string for "$key".');
  }
  final parsed = DateTime.tryParse(value);
  if (parsed == null) throw FormatException('Invalid date for "$key".');
  return parsed;
}
