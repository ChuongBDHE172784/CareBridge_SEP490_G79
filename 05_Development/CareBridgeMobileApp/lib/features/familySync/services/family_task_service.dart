import '../../../core/network/api_client.dart';
import '../models/family_task_model.dart';

class FamilyTaskService {
  // UC73: List all care tasks in a group
  Future<List<FamilyTask>> listTasks(String groupId) async {
    final data = await apiGet('/api/v1/care-groups/$groupId/tasks');
    final tasksList = data['data']['tasks'] as List<dynamic>? ?? [];
    return tasksList.map((e) => FamilyTask.fromJson(e as Map<String, dynamic>)).toList();
  }

  // UC73: Assign a care task
  Future<FamilyTask> assignTask(String groupId, String assigneeMemberId, String title, String description, DateTime dueAt) async {
    final data = await apiPost('/api/v1/care-groups/$groupId/tasks', {
      'assigneeMemberId': assigneeMemberId,
      'title': title,
      'description': description,
      'dueAt': dueAt.toUtc().toIso8601String(),
    });
    return FamilyTask.fromJson(data['data'] as Map<String, dynamic>);
  }

  // UC221: View task detail
  Future<FamilyTask> getTaskDetail(String groupId, String taskId) async {
    final data = await apiGet('/api/v1/care-groups/$groupId/tasks/$taskId');
    return FamilyTask.fromJson(data['data'] as Map<String, dynamic>);
  }

  // UC222: Update task content
  Future<FamilyTask> updateTask(String groupId, String taskId, String title, String description, DateTime dueAt) async {
    final data = await apiPatch('/api/v1/care-groups/$groupId/tasks/$taskId', {
      'title': title,
      'description': description,
      'dueAt': dueAt.toUtc().toIso8601String(),
    });
    return FamilyTask.fromJson(data['data'] as Map<String, dynamic>);
  }

  // UC223: Cancel an incomplete task
  Future<void> cancelTask(String groupId, String taskId) async {
    await apiPost('/api/v1/care-groups/$groupId/tasks/$taskId/cancel', const {});
  }

  // UC85: Update assigned task status
  Future<FamilyTask> updateTaskStatus(String groupId, String taskId, String status) async {
    final data = await apiPatch('/api/v1/care-groups/$groupId/tasks/$taskId/status', {
      'status': status,
    });
    return FamilyTask.fromJson(data['data'] as Map<String, dynamic>);
  }
}
