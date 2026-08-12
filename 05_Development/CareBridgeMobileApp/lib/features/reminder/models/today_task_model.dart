import 'reminder_model.dart';
import 'today_task_support_function.dart';

enum TodayTaskKind { checklist, reminder, careTask, unknown }

enum TodayTaskOrigin { systemTemplate, userCreated, unknown }

enum TodayTaskTarget { mother, baby, unknown }

enum TodayTaskStatus {
  pending,
  inProgress,
  completed,
  skipped,
  cancelled,
  unknown,
}

enum TodayTimeBucket { overdue, today, upcoming, unscheduled, unknown }

enum TodayTaskAction { complete, skip, reopen }

enum TodayTaskSkipReason { notApplicable, userChoice, lifecycleChanged }

enum TodayTaskSourceType { reminder, careTask, checklist, unknown }

enum TodaySequenceState {
  active,
  readyToAdvance,
  sequenceComplete,
  configurationBlocked,
  unknown,
}

class TodaySequenceNextSet {
  final String? name;
  final int? position;

  const TodaySequenceNextSet({this.name, this.position});

  factory TodaySequenceNextSet.fromJson(Map<String, dynamic> json) =>
      TodaySequenceNextSet(
        name: json['name'] as String?,
        position: (json['position'] as num?)?.toInt(),
      );
}

class TodaySequenceProjection {
  final TodaySequenceState state;
  final String? currentInstanceId;
  final String? currentTemplateVersionId;
  final String? currentSetName;
  final int? currentPosition;
  final int? totalPositions;
  final int? qualifiedPositions;
  final bool advanceAvailable;
  final TodaySequenceNextSet? nextSet;
  final bool sequenceComplete;
  final String? blockedReasonCode;

  const TodaySequenceProjection({
    required this.state,
    this.currentInstanceId,
    this.currentTemplateVersionId,
    this.currentSetName,
    this.currentPosition,
    this.totalPositions,
    this.qualifiedPositions,
    required this.advanceAvailable,
    this.nextSet,
    required this.sequenceComplete,
    this.blockedReasonCode,
  });

  bool get readyToAdvance => state == TodaySequenceState.readyToAdvance;

  factory TodaySequenceProjection.fromJson(Map<String, dynamic> json) {
    final raw = (json['sequenceState'] as String? ?? '').toUpperCase();
    final state = switch (raw) {
      'ACTIVE' => TodaySequenceState.active,
      'READY_TO_ADVANCE' => TodaySequenceState.readyToAdvance,
      'SEQUENCE_COMPLETE' => TodaySequenceState.sequenceComplete,
      'CONFIGURATION_BLOCKED' => TodaySequenceState.configurationBlocked,
      _ => TodaySequenceState.unknown,
    };
    final next = json['nextSet'];
    return TodaySequenceProjection(
      state: state,
      currentInstanceId: json['currentInstanceId'] as String?,
      currentTemplateVersionId: json['currentTemplateVersionId'] as String?,
      currentSetName: json['currentSetName'] as String?,
      currentPosition: (json['currentPosition'] as num?)?.toInt(),
      totalPositions: (json['totalPositions'] as num?)?.toInt(),
      qualifiedPositions: (json['qualifiedPositions'] as num?)?.toInt(),
      advanceAvailable: json['advanceAvailable'] == true,
      nextSet: next is Map
          ? TodaySequenceNextSet.fromJson(Map<String, dynamic>.from(next))
          : null,
      sequenceComplete: json['sequenceComplete'] == true,
      blockedReasonCode: json['blockedReasonCode'] as String?,
    );
  }
}

T _enumFromApi<T>(String? raw, Map<String, T> values, T fallback) =>
    values[raw?.trim().toUpperCase()] ?? fallback;

extension TodayTaskKindApi on TodayTaskKind {
  static TodayTaskKind fromApi(String? value) => _enumFromApi(value, const {
    'CHECKLIST': TodayTaskKind.checklist,
    'REMINDER': TodayTaskKind.reminder,
    'CARE_TASK': TodayTaskKind.careTask,
  }, TodayTaskKind.unknown);

  String get apiValue => switch (this) {
    TodayTaskKind.checklist => 'CHECKLIST',
    TodayTaskKind.reminder => 'REMINDER',
    TodayTaskKind.careTask => 'CARE_TASK',
    TodayTaskKind.unknown => 'UNKNOWN',
  };
}

extension TodayTaskSourceTypeExtension on TodayTaskSourceType {
  static TodayTaskSourceType fromApi(String? value) =>
      switch (TodayTaskKindApi.fromApi(value)) {
        TodayTaskKind.reminder => TodayTaskSourceType.reminder,
        TodayTaskKind.careTask => TodayTaskSourceType.careTask,
        TodayTaskKind.checklist => TodayTaskSourceType.checklist,
        TodayTaskKind.unknown => TodayTaskSourceType.unknown,
      };
}

extension TodayTaskActionApi on TodayTaskAction {
  static TodayTaskAction? fromApi(String? value) =>
      switch (value?.toUpperCase()) {
        'COMPLETE' => TodayTaskAction.complete,
        'SKIP' => TodayTaskAction.skip,
        'REOPEN' => TodayTaskAction.reopen,
        _ => null,
      };

  String get apiValue => switch (this) {
    TodayTaskAction.complete => 'COMPLETE',
    TodayTaskAction.skip => 'SKIP',
    TodayTaskAction.reopen => 'REOPEN',
  };
}

extension TodayTaskSkipReasonApi on TodayTaskSkipReason {
  String get apiValue => switch (this) {
    TodayTaskSkipReason.notApplicable => 'NOT_APPLICABLE',
    TodayTaskSkipReason.userChoice => 'USER_CHOICE',
    TodayTaskSkipReason.lifecycleChanged => 'LIFECYCLE_CHANGED',
  };
}

class TodayTask {
  final String id;
  final TodayTaskKind kind;
  final TodayTaskSourceType sourceType;
  final ReminderType type;
  final String title;
  final String? description;
  final TodayTaskSupportFunction? supportFunction;
  final DateTime? scheduledAt;
  final DateTime? dueAt;
  final DateTime? snoozedUntil;
  final ReminderStatus status;
  final TodayTaskStatus taskStatus;
  final int priority;
  final String? instanceId;
  final String? templateVersionId;
  final String? careGroupId;
  final String? careGroupLabel;
  final String? careContextType;
  final String? careContextId;
  final String? careContextLabel;
  final TodayTaskTarget target;
  final TodayTaskOrigin origin;
  final TodayTimeBucket bucket;
  final Set<TodayTaskAction> allowedActions;

  const TodayTask({
    required this.id,
    required this.kind,
    required this.sourceType,
    required this.type,
    required this.title,
    this.description,
    this.supportFunction,
    this.scheduledAt,
    this.dueAt,
    this.snoozedUntil,
    required this.status,
    required this.taskStatus,
    required this.priority,
    this.instanceId,
    this.templateVersionId,
    this.careGroupId,
    this.careGroupLabel,
    this.careContextType,
    this.careContextId,
    this.careContextLabel,
    required this.target,
    required this.origin,
    required this.bucket,
    required this.allowedActions,
  });

  bool get isReminder => kind == TodayTaskKind.reminder;
  bool get isCareTask => kind == TodayTaskKind.careTask;
  bool get isChecklist => kind == TodayTaskKind.checklist;
  bool get isPending => taskStatus == TodayTaskStatus.pending;
  bool get isCompleted => taskStatus == TodayTaskStatus.completed;
  bool get isSnoozed => status == ReminderStatus.snoozed;
  bool get isSkipped => taskStatus == TodayTaskStatus.skipped;
  bool get isTerminal =>
      isCompleted || isSkipped || taskStatus == TodayTaskStatus.cancelled;

  String get originLabel => switch (origin) {
    TodayTaskOrigin.systemTemplate => 'System template',
    TodayTaskOrigin.userCreated => 'My care',
    TodayTaskOrigin.unknown => 'My care',
  };

  String get targetLabel => switch (target) {
    TodayTaskTarget.baby => 'Baby',
    TodayTaskTarget.mother => 'My care',
    // V2 checklist tasks intentionally have no target subject.  Keep that
    // state neutral instead of presenting it as a mother-targeted task.
    TodayTaskTarget.unknown => isChecklist ? 'Khuyến nghị' : 'My care',
  };

  String get statusLabel => switch (taskStatus) {
    TodayTaskStatus.pending => 'Đang chờ',
    TodayTaskStatus.inProgress => 'Đang thực hiện',
    TodayTaskStatus.completed => 'Đã hoàn tất',
    TodayTaskStatus.skipped => 'Đã bỏ qua',
    TodayTaskStatus.cancelled => 'Đã hủy',
    TodayTaskStatus.unknown => 'Chưa xác định',
  };

  factory TodayTask.fromJson(Map<String, dynamic> json) {
    final kind = TodayTaskKindApi.fromApi(
      (json['taskKind'] ?? json['sourceType']) as String?,
    );
    final scheduledAt = _date(json['scheduledAt']);
    final dueAt = _date(json['dueAt']);
    final statusRaw = json['status'] as String?;
    final actions = (json['allowedActions'] as List? ?? const [])
        .map((value) => TodayTaskActionApi.fromApi(value?.toString()))
        .whereType<TodayTaskAction>()
        .toSet();
    return TodayTask(
      id: (json['taskId'] ?? json['id']) as String,
      kind: kind,
      sourceType: TodayTaskSourceTypeExtension.fromApi(
        (json['taskKind'] ?? json['sourceType']) as String?,
      ),
      type: ReminderTypeExtension.fromApi(
        (json['type'] ?? json['reminderType']) as String?,
      ),
      title: json['title'] as String? ?? '',
      description: json['description'] as String?,
      supportFunction: TodayTaskSupportFunction.fromJson(
        json['supportFunction'],
      ),
      scheduledAt: scheduledAt ?? dueAt,
      dueAt: dueAt ?? scheduledAt,
      snoozedUntil: _date(json['snoozedUntil']),
      status: ReminderStatusExtension.fromApi(statusRaw),
      taskStatus: _enumFromApi(statusRaw, const {
        'PENDING': TodayTaskStatus.pending,
        'IN_PROGRESS': TodayTaskStatus.inProgress,
        'COMPLETED': TodayTaskStatus.completed,
        'DONE': TodayTaskStatus.completed,
        'SKIPPED': TodayTaskStatus.skipped,
        'CANCELLED': TodayTaskStatus.cancelled,
      }, TodayTaskStatus.unknown),
      priority: (json['priority'] as num?)?.toInt() ?? 99,
      instanceId: json['instanceId'] as String?,
      templateVersionId: json['templateVersionId'] as String?,
      careGroupId: json['careGroupId'] as String?,
      careGroupLabel:
          (json['careGroupLabel'] ?? json['careGroupName']) as String?,
      careContextType: json['careContextType'] as String?,
      careContextId: json['careContextId'] as String?,
      careContextLabel:
          (json['careContextLabel'] ?? json['contextLabel']) as String?,
      target: _enumFromApi(json['targetSubject'] as String?, const {
        'MOTHER': TodayTaskTarget.mother,
        'BABY': TodayTaskTarget.baby,
      }, TodayTaskTarget.unknown),
      origin: _enumFromApi(json['origin'] as String?, const {
        'SYSTEM_TEMPLATE': TodayTaskOrigin.systemTemplate,
        'USER_CREATED': TodayTaskOrigin.userCreated,
      }, TodayTaskOrigin.unknown),
      bucket: _enumFromApi(json['timeBucket'] as String?, const {
        'OVERDUE': TodayTimeBucket.overdue,
        'TODAY': TodayTimeBucket.today,
        'UPCOMING': TodayTimeBucket.upcoming,
        'UNSCHEDULED': TodayTimeBucket.unscheduled,
      }, TodayTimeBucket.unknown),
      allowedActions: actions,
    );
  }

  static DateTime? _date(dynamic value) {
    if (value is! String || value.isEmpty) return null;
    return DateTime.tryParse(value)?.toLocal();
  }
}

class TodayTaskSections {
  final List<TodayTask> overdue;
  final List<TodayTask> today;
  final List<TodayTask> upcoming;
  final List<TodayTask> unscheduled;

  const TodayTaskSections({
    required this.overdue,
    required this.today,
    required this.upcoming,
    required this.unscheduled,
  });

  factory TodayTaskSections.fromJson(Map<String, dynamic> json) =>
      TodayTaskSections(
        overdue: _tasks(json['overdue']),
        today: _tasks(json['today']),
        upcoming: _tasks(json['upcoming']),
        unscheduled: _tasks(json['unscheduled']),
      );

  static List<TodayTask> _tasks(dynamic value) => (value as List? ?? const [])
      .whereType<Map>()
      .map((item) {
        final json = Map<String, dynamic>.from(item);
        if (!json.containsKey('taskKind') && !json.containsKey('sourceType')) {
          json['taskKind'] = 'CHECKLIST';
        }
        return TodayTask.fromJson(json);
      })
      .toList(growable: false);

  Iterable<TodayTask> get all sync* {
    yield* overdue;
    yield* today;
    yield* upcoming;
    yield* unscheduled;
  }
}

class TodayTasksSnapshot {
  final DateTime asOf;
  final String zoneId;
  final int horizonDays;
  final TodayTaskSections sections;
  final String correlationId;
  final TodaySequenceProjection? sequence;

  const TodayTasksSnapshot({
    required this.asOf,
    required this.zoneId,
    required this.horizonDays,
    required this.sections,
    required this.correlationId,
    this.sequence,
  });

  factory TodayTasksSnapshot.fromJson(Map<String, dynamic> json) =>
      TodayTasksSnapshot(
        asOf: DateTime.parse(json['asOf'] as String).toLocal(),
        zoneId: json['zoneId'] as String? ?? 'Asia/Ho_Chi_Minh',
        horizonDays: (json['horizonDays'] as num?)?.toInt() ?? 7,
        sections: TodayTaskSections.fromJson(
          Map<String, dynamic>.from(json['sections'] as Map? ?? const {}),
        ),
        correlationId: json['correlationId'] as String? ?? '',
        sequence: json['sequence'] is Map
            ? TodaySequenceProjection.fromJson(
                Map<String, dynamic>.from(json['sequence'] as Map),
              )
            : json['sequenceState'] is String
            ? TodaySequenceProjection.fromJson(json)
            : null,
      );

  int get totalCount => sections.all.length;
}
