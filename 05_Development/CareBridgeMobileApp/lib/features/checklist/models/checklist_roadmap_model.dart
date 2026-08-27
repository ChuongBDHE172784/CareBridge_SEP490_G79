class ChecklistRoadmapTask {
  final String id;
  final String title;
  final String? description;
  final String category;
  final bool isRequired;
  final bool completed;
  final int? dueWeek;

  const ChecklistRoadmapTask({
    required this.id,
    required this.title,
    this.description,
    this.category = 'Chung',
    this.isRequired = false,
    this.completed = false,
    this.dueWeek,
  });

  factory ChecklistRoadmapTask.fromJson(Map<String, dynamic> json) {
    return ChecklistRoadmapTask(
      id: json['id'] as String? ?? json['itemId'] as String? ?? '',
      title: json['title'] as String? ?? json['itemText'] as String? ?? json['name'] as String? ?? '',
      description: json['description'] as String?,
      category: json['category'] as String? ?? 'Chung',
      isRequired: json['isRequired'] as bool? ?? json['required'] as bool? ?? false,
      completed: json['completed'] as bool? ?? false,
      dueWeek: (json['dueWeek'] as num?)?.toInt(),
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'title': title,
    'description': description,
    'category': category,
    'isRequired': isRequired,
    'completed': completed,
    'dueWeek': dueWeek,
  };
}

enum ChecklistMilestoneStatus {
  completed,
  current,
  upcoming,
}

class ChecklistRoadmapMilestone {
  final String id;
  final String title;
  final String? description;
  final String stage;
  final int startWeek;
  final int endWeek;
  final ChecklistMilestoneStatus status;
  final List<ChecklistRoadmapTask> tasks;

  const ChecklistRoadmapMilestone({
    required this.id,
    required this.title,
    this.description,
    required this.stage,
    required this.startWeek,
    required this.endWeek,
    required this.status,
    required this.tasks,
  });

  int get completedTaskCount => tasks.where((t) => t.completed).length;
  int get totalTaskCount => tasks.length;
  int get progressPercent => totalTaskCount > 0
      ? ((completedTaskCount / totalTaskCount) * 100).round()
      : 0;

  String get weekRangeLabel {
    if (startWeek == endWeek) {
      return 'Tuần $startWeek';
    }
    return 'Tuần $startWeek - $endWeek';
  }
}
