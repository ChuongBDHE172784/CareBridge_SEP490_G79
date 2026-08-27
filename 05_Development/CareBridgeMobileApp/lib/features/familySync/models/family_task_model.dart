class FamilyTask {
  final String taskId;
  final String groupId;
  final String title;
  final String description;
  final DateTime dueAt;
  final String status; // TODO, IN_PROGRESS, COMPLETED, CANCELLED
  final String assignedTo;
  final String assignedToName;
  final String assignedBy;
  final String assignedByName;
  final DateTime? completedAt;
  final DateTime createdAt;

  FamilyTask({
    required this.taskId,
    required this.groupId,
    required this.title,
    required this.description,
    required this.dueAt,
    required this.status,
    required this.assignedTo,
    required this.assignedToName,
    required this.assignedBy,
    required this.assignedByName,
    this.completedAt,
    required this.createdAt,
  });

  factory FamilyTask.fromJson(Map<String, dynamic> json) {
    final dueAt = DateTime.tryParse(json['dueAt'] as String? ?? '') ??
        DateTime.fromMillisecondsSinceEpoch(0, isUtc: true);
    final createdAt = DateTime.tryParse(json['createdAt'] as String? ?? '') ?? dueAt;
    return FamilyTask(
      taskId: json['careTaskId'] ?? '',
      groupId: json['careGroupId'] ?? '',
      title: json['title'] ?? '',
      description: json['description'] ?? '',
      dueAt: dueAt,
      status: json['status'] ?? 'TODO',
      assignedTo: json['assignedTo'] ?? '',
      assignedToName: json['assignedToName'] ?? '',
      assignedBy: json['assignedBy'] ?? '',
      assignedByName: json['assignedByName'] ?? '',
      completedAt: json['completedAt'] != null
          ? DateTime.parse(json['completedAt'])
          : null,
      createdAt: createdAt,
    );
  }
}
