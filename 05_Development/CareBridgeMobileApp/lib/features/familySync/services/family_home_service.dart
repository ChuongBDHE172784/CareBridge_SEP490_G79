import '../../../core/network/api_client.dart';

class FamilyHomeService {
  static final FamilyHomeService instance = FamilyHomeService._();
  FamilyHomeService._();

  Future<FamilyHomeSnapshot> loadSnapshot() async {
    String? groupId;
    String? groupName;
    FamilyHomeTask? nextTask;
    FamilyHomeSchedule? nextSchedule;
    FamilyHomeAlert? latestAlert;

    try {
      final groupsJson = await apiGet('/api/v1/care-groups');
      final groups = groupsJson['data'] as List? ?? [];
      if (groups.isNotEmpty) {
        final first = groups.first as Map<String, dynamic>;
        groupId = (first['groupId'] ?? first['id'])?.toString();
        groupName = first['groupName'] as String?;
      }
    } catch (_) {
      groupId = null;
    }

    if (groupId != null) {
      try {
        final tasksJson = await apiGet('/api/v1/care-groups/$groupId/tasks');
        final data = tasksJson['data'] as Map<String, dynamic>? ?? {};
        final tasks = data['tasks'] as List? ?? [];
        if (tasks.isNotEmpty) {
          final raw = tasks.cast<Map<String, dynamic>>().firstWhere(
                (task) => task['status']?.toString() != 'COMPLETED',
                orElse: () => tasks.first as Map<String, dynamic>,
              );
          nextTask = FamilyHomeTask.fromJson(raw);
        }
      } catch (_) {}

      try {
        final now = DateTime.now().toUtc();
        final rangeEnd = now.add(const Duration(days: 7));
        final query = Uri(
          queryParameters: {
            'rangeStart': now.toIso8601String(),
            'rangeEnd': rangeEnd.toIso8601String(),
          },
        ).query;
        final calendarJson = await apiGet(
          '/api/v1/care-groups/$groupId/calendar?$query',
        );
        final data = calendarJson['data'] as Map<String, dynamic>? ?? {};
        final items = data['items'] as List? ?? [];
        if (items.isNotEmpty) {
          nextSchedule = FamilyHomeSchedule.fromJson(
            items.first as Map<String, dynamic>,
          );
        }
      } catch (_) {}
    }

    try {
      final alertsJson = await apiGet('/api/v1/family-alerts?page=0&size=1');
      final data = alertsJson['data'] as Map<String, dynamic>? ?? {};
      final alerts = data['alerts'] as List? ?? [];
      if (alerts.isNotEmpty) {
        latestAlert = FamilyHomeAlert.fromJson(
          alerts.first as Map<String, dynamic>,
        );
      }
    } catch (_) {}

    return FamilyHomeSnapshot(
      groupId: groupId,
      groupName: groupName,
      nextTask: nextTask,
      nextSchedule: nextSchedule,
      latestAlert: latestAlert,
    );
  }
}

class FamilyHomeSnapshot {
  final String? groupId;
  final String? groupName;
  final FamilyHomeTask? nextTask;
  final FamilyHomeSchedule? nextSchedule;
  final FamilyHomeAlert? latestAlert;

  const FamilyHomeSnapshot({
    this.groupId,
    this.groupName,
    this.nextTask,
    this.nextSchedule,
    this.latestAlert,
  });
}

class FamilyHomeTask {
  final String id;
  final String title;
  final String category;
  final String status;

  const FamilyHomeTask({
    required this.id,
    required this.title,
    required this.category,
    required this.status,
  });

  factory FamilyHomeTask.fromJson(Map<String, dynamic> json) {
    return FamilyHomeTask(
      id: json['careTaskId']?.toString() ?? '',
      title: json['title'] as String? ?? 'Nhiệm vụ chăm sóc',
      category: _inferTaskCategory(json['title'] as String? ?? ''),
      status: json['status'] as String? ?? 'OPEN',
    );
  }
}

class FamilyHomeSchedule {
  final String title;
  final DateTime? dueAt;

  const FamilyHomeSchedule({required this.title, this.dueAt});

  factory FamilyHomeSchedule.fromJson(Map<String, dynamic> json) {
    return FamilyHomeSchedule(
      title: json['title'] as String? ?? 'Lịch chăm sóc',
      dueAt: DateTime.tryParse(json['dueAt'] as String? ?? ''),
    );
  }
}

class FamilyHomeAlert {
  final String id;
  final String title;
  final String body;
  final DateTime? createdAt;

  const FamilyHomeAlert({
    required this.id,
    required this.title,
    required this.body,
    this.createdAt,
  });

  factory FamilyHomeAlert.fromJson(Map<String, dynamic> json) {
    return FamilyHomeAlert(
      id: json['alertId']?.toString() ?? '',
      title: json['title'] as String? ?? 'Thông báo mới',
      body: json['body'] as String? ?? '',
      createdAt: DateTime.tryParse(json['createdAt'] as String? ?? ''),
    );
  }
}

String _inferTaskCategory(String title) {
  final lower = title.toLowerCase();
  if (lower.contains('mua') || lower.contains('bỉm') || lower.contains('sữa')) {
    return 'Mua sắm';
  }
  if (lower.contains('thuốc') || lower.contains('khám')) return 'Y tế';
  return 'Chăm sóc';
}
