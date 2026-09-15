import '../../directChat/models/checklist_share_data.dart';
import '../../directChat/models/health_metrics_share_data.dart';

SharedRecordAlertLevel evaluateHealthAlertLevel(HealthMetricsShareData data) {
  for (final m in data.metrics) {
    if (m.status.toUpperCase() == 'CRITICAL') {
      return SharedRecordAlertLevel.critical;
    }
  }
  for (final m in data.metrics) {
    if (m.status.toUpperCase() == 'WARNING') {
      return SharedRecordAlertLevel.warning;
    }
  }
  return SharedRecordAlertLevel.normal;
}

enum SharedRecordType {
  healthMetrics,
  checklist,
}

enum SharedRecordAlertLevel {
  normal,
  warning,
  critical,
}

enum SharedRecordTabType {
  all,
  healthMetrics,
  checklist,
}

enum SharedRecordAlertFilter {
  all,
  critical,
  warning,
  normal,
}

class SharedRecordEntry {
  final String id;
  final String conversationId;
  final String? conversationStatus;
  final String motherUserId;
  final String motherName;
  final String? motherAvatar;
  final String? motherPhone;
  final DateTime createdAt;
  final SharedRecordType type;
  final HealthMetricsShareData? healthData;
  final ChecklistShareData? checklistData;
  final SharedRecordAlertLevel alertLevel;
  final String status;
  final String? expertFeedback;

  const SharedRecordEntry({
    required this.id,
    required this.conversationId,
    this.conversationStatus,
    required this.motherUserId,
    required this.motherName,
    this.motherAvatar,
    this.motherPhone,
    required this.createdAt,
    required this.type,
    this.healthData,
    this.checklistData,
    this.alertLevel = SharedRecordAlertLevel.normal,
    this.status = 'PENDING_REVIEW',
    this.expertFeedback,
  });

  SharedRecordEntry copyWith({
    String? id,
    String? conversationId,
    String? conversationStatus,
    String? motherUserId,
    String? motherName,
    String? motherAvatar,
    String? motherPhone,
    DateTime? createdAt,
    SharedRecordType? type,
    HealthMetricsShareData? healthData,
    ChecklistShareData? checklistData,
    SharedRecordAlertLevel? alertLevel,
    String? status,
    String? expertFeedback,
  }) {
    return SharedRecordEntry(
      id: id ?? this.id,
      conversationId: conversationId ?? this.conversationId,
      conversationStatus: conversationStatus ?? this.conversationStatus,
      motherUserId: motherUserId ?? this.motherUserId,
      motherName: motherName ?? this.motherName,
      motherAvatar: motherAvatar ?? this.motherAvatar,
      motherPhone: motherPhone ?? this.motherPhone,
      createdAt: createdAt ?? this.createdAt,
      type: type ?? this.type,
      healthData: healthData ?? this.healthData,
      checklistData: checklistData ?? this.checklistData,
      alertLevel: alertLevel ?? this.alertLevel,
      status: status ?? this.status,
      expertFeedback: expertFeedback ?? this.expertFeedback,
    );
  }
}

class MotherSharedCardData {
  final String motherUserId;
  final String conversationId;
  final String? conversationStatus;
  final String motherName;
  final String? motherAvatar;
  final String? motherPhone;
  final DateTime lastActiveAt;
  final int? gestationalWeek;
  final String? stageLabel;
  final SharedRecordAlertLevel overallAlertLevel;
  final SharedRecordEntry? latestHealthRecord;
  final SharedRecordEntry? latestChecklistRecord;
  final List<SharedRecordEntry> allHealthRecords;
  final List<SharedRecordEntry> allChecklistRecords;

  const MotherSharedCardData({
    required this.motherUserId,
    required this.conversationId,
    this.conversationStatus,
    required this.motherName,
    this.motherAvatar,
    this.motherPhone,
    required this.lastActiveAt,
    this.gestationalWeek,
    this.stageLabel,
    this.overallAlertLevel = SharedRecordAlertLevel.normal,
    this.latestHealthRecord,
    this.latestChecklistRecord,
    this.allHealthRecords = const [],
    this.allChecklistRecords = const [],
  });

  MotherSharedCardData copyWith({
    String? motherUserId,
    String? conversationId,
    String? conversationStatus,
    String? motherName,
    String? motherAvatar,
    String? motherPhone,
    DateTime? lastActiveAt,
    int? gestationalWeek,
    String? stageLabel,
    SharedRecordAlertLevel? overallAlertLevel,
    SharedRecordEntry? latestHealthRecord,
    SharedRecordEntry? latestChecklistRecord,
    List<SharedRecordEntry>? allHealthRecords,
    List<SharedRecordEntry>? allChecklistRecords,
  }) {
    return MotherSharedCardData(
      motherUserId: motherUserId ?? this.motherUserId,
      conversationId: conversationId ?? this.conversationId,
      conversationStatus: conversationStatus ?? this.conversationStatus,
      motherName: motherName ?? this.motherName,
      motherAvatar: motherAvatar ?? this.motherAvatar,
      motherPhone: motherPhone ?? this.motherPhone,
      lastActiveAt: lastActiveAt ?? this.lastActiveAt,
      gestationalWeek: gestationalWeek ?? this.gestationalWeek,
      stageLabel: stageLabel ?? this.stageLabel,
      overallAlertLevel: overallAlertLevel ?? this.overallAlertLevel,
      latestHealthRecord: latestHealthRecord ?? this.latestHealthRecord,
      latestChecklistRecord: latestChecklistRecord ?? this.latestChecklistRecord,
      allHealthRecords: allHealthRecords ?? this.allHealthRecords,
      allChecklistRecords: allChecklistRecords ?? this.allChecklistRecords,
    );
  }
}
