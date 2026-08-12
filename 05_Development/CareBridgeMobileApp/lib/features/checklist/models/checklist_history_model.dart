enum ChecklistHistoryTargetSubject { mother, baby }

extension ChecklistHistoryTargetSubjectApi on ChecklistHistoryTargetSubject {
  String get apiValue {
    switch (this) {
      case ChecklistHistoryTargetSubject.mother:
        return 'MOTHER';
      case ChecklistHistoryTargetSubject.baby:
        return 'BABY';
    }
  }

  String get label {
    switch (this) {
      case ChecklistHistoryTargetSubject.mother:
        return 'Mẹ';
      case ChecklistHistoryTargetSubject.baby:
        return 'Bé';
    }
  }

  static ChecklistHistoryTargetSubject? fromApi(String? value) {
    switch (value?.trim().toUpperCase()) {
      case 'MOTHER':
        return ChecklistHistoryTargetSubject.mother;
      case 'BABY':
        return ChecklistHistoryTargetSubject.baby;
      default:
        return null;
    }
  }
}

class ChecklistHistoryPage {
  const ChecklistHistoryPage({
    required this.items,
    required this.page,
    required this.size,
    required this.totalElements,
    required this.totalPages,
  });

  final List<ChecklistHistoryItem> items;
  final int page;
  final int size;
  final int totalElements;
  final int totalPages;

  bool get hasNextPage => page + 1 < totalPages;

  factory ChecklistHistoryPage.fromJson(Map<String, dynamic> json) {
    final rawItems = json['items'] as List? ?? const [];
    return ChecklistHistoryPage(
      items: rawItems
          .whereType<Map>()
          .map(
            (item) =>
                ChecklistHistoryItem.fromJson(Map<String, dynamic>.from(item)),
          )
          .toList(growable: false),
      page: (json['page'] as num?)?.toInt() ?? 0,
      size: (json['size'] as num?)?.toInt() ?? rawItems.length,
      totalElements:
          (json['totalElements'] as num?)?.toInt() ?? rawItems.length,
      totalPages: (json['totalPages'] as num?)?.toInt() ?? 0,
    );
  }
}

class ChecklistHistoryItem {
  const ChecklistHistoryItem({
    required this.checklistInstanceId,
    this.templateVersionId,
    this.templateName,
    this.stage,
    this.targetSubject,
    this.careContextType,
    this.careContextId,
    this.careContextLabel,
    this.windowStart,
    this.windowEnd,
    this.historicalAt,
    this.historyReasonCode,
    required this.tasks,
  });

  final String checklistInstanceId;
  final String? templateVersionId;
  final String? templateName;
  final String? stage;
  final ChecklistHistoryTargetSubject? targetSubject;
  final String? careContextType;
  final String? careContextId;
  final String? careContextLabel;
  final DateTime? windowStart;
  final DateTime? windowEnd;
  final DateTime? historicalAt;
  final String? historyReasonCode;
  final List<ChecklistHistoryTask> tasks;

  int get completedCount =>
      tasks.where((task) => task.status == 'COMPLETED').length;

  int get pendingCount => tasks.length - completedCount;

  String get stageLabel {
    switch (stage?.trim().toUpperCase()) {
      case 'PRE_PREGNANCY':
        return 'Chuẩn bị mang thai';
      case 'PREGNANCY':
        return 'Mang thai';
      case 'POSTPARTUM':
        return 'Hậu sản';
      case 'BABY_CARE':
        return 'Chăm sóc bé';
      default:
        return 'Checklist cũ';
    }
  }

  /// V2 checklist leaves are targetless.  Keep their history label neutral;
  /// the legacy fallback "Mẹ & bé" implied a target that is not present.
  String get subjectLabel => targetSubject?.label ?? 'Khuyến nghị';

  factory ChecklistHistoryItem.fromJson(Map<String, dynamic> json) {
    final rawTasks = json['tasks'] as List? ?? const [];
    return ChecklistHistoryItem(
      checklistInstanceId: json['checklistInstanceId']?.toString() ?? '',
      templateVersionId: json['templateVersionId']?.toString(),
      templateName: json['templateName']?.toString(),
      stage: json['stage']?.toString(),
      targetSubject: ChecklistHistoryTargetSubjectApi.fromApi(
        json['targetSubject']?.toString(),
      ),
      careContextType: json['careContextType']?.toString(),
      careContextId: json['careContextId']?.toString(),
      careContextLabel: json['careContextLabel']?.toString(),
      windowStart: _parseDate(json['windowStart']),
      windowEnd: _parseDate(json['windowEnd']),
      historicalAt: _parseDateTime(json['historicalAt']),
      historyReasonCode: json['historyReasonCode']?.toString(),
      tasks: rawTasks
          .whereType<Map>()
          .map(
            (task) =>
                ChecklistHistoryTask.fromJson(Map<String, dynamic>.from(task)),
          )
          .toList(growable: false),
    );
  }
}

class ChecklistHistoryTask {
  const ChecklistHistoryTask({
    required this.taskId,
    required this.title,
    required this.status,
    this.completedAt,
    this.skippedAt,
    this.cancelledAt,
    required this.displayOrder,
    required this.required,
  });

  final String taskId;
  final String title;
  final String status;
  final DateTime? completedAt;
  final DateTime? skippedAt;
  final DateTime? cancelledAt;
  final int displayOrder;
  final bool required;

  bool get isCompleted => status == 'COMPLETED';

  String get statusLabel {
    switch (status) {
      case 'COMPLETED':
        return 'Đã hoàn thành';
      case 'IN_PROGRESS':
        return 'Đang làm';
      case 'SKIPPED':
        return 'Đã bỏ qua';
      case 'CANCELLED':
        return 'Đã hủy';
      default:
        return 'Chưa hoàn thành';
    }
  }

  factory ChecklistHistoryTask.fromJson(Map<String, dynamic> json) {
    return ChecklistHistoryTask(
      taskId: json['taskId']?.toString() ?? '',
      title: json['title']?.toString() ?? '',
      status: json['status']?.toString() ?? 'PENDING',
      completedAt: _parseDateTime(json['completedAt']),
      skippedAt: _parseDateTime(json['skippedAt']),
      cancelledAt: _parseDateTime(json['cancelledAt']),
      displayOrder: (json['displayOrder'] as num?)?.toInt() ?? 0,
      required: json['required'] as bool? ?? false,
    );
  }
}

DateTime? _parseDate(Object? value) {
  if (value == null) return null;
  return DateTime.tryParse(value.toString());
}

DateTime? _parseDateTime(Object? value) {
  if (value == null) return null;
  return DateTime.tryParse(value.toString());
}
