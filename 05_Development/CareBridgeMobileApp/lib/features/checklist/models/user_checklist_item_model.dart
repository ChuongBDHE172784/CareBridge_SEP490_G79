enum ChecklistCategory { delivery, paperwork, babyCare, general }

extension ChecklistCategoryApi on ChecklistCategory {
  String get apiValue {
    switch (this) {
      case ChecklistCategory.delivery:
        return 'DELIVERY';
      case ChecklistCategory.paperwork:
        return 'PAPERWORK';
      case ChecklistCategory.babyCare:
        return 'BABY_CARE';
      case ChecklistCategory.general:
        return 'GENERAL';
    }
  }

  String get label {
    switch (this) {
      case ChecklistCategory.delivery:
        return 'Đi sinh';
      case ChecklistCategory.paperwork:
        return 'Giấy tờ';
      case ChecklistCategory.babyCare:
        return 'Chăm sóc bé';
      case ChecklistCategory.general:
        return 'Chung';
    }
  }

  static ChecklistCategory fromApi(String? value) {
    switch (value) {
      case 'DELIVERY':
        return ChecklistCategory.delivery;
      case 'PAPERWORK':
        return ChecklistCategory.paperwork;
      case 'BABY_CARE':
        return ChecklistCategory.babyCare;
      default:
        return ChecklistCategory.general;
    }
  }
}

class UserChecklistItem {
  final String itemId;
  final String? templateItemId;
  final String? templateName;
  final bool? required;
  final String itemText;
  final ChecklistCategory category;
  final bool completed;
  final DateTime? completedAt;
  final int itemOrder;
  final DateTime? createdAt;

  const UserChecklistItem({
    required this.itemId,
    this.templateItemId,
    this.templateName,
    this.required,
    required this.itemText,
    required this.category,
    required this.completed,
    this.completedAt,
    required this.itemOrder,
    this.createdAt,
  });

  factory UserChecklistItem.fromJson(Map<String, dynamic> json) {
    return UserChecklistItem(
      itemId: json['itemId'] as String,
      templateItemId: json['templateItemId'] as String?,
      templateName: json['templateName'] as String?,
      required: json['required'] as bool?,
      itemText: json['itemText'] as String? ?? '',
      category: ChecklistCategoryApi.fromApi(json['category'] as String?),
      completed: json['completed'] as bool? ?? false,
      completedAt: json['completedAt'] == null
          ? null
          : DateTime.parse(json['completedAt'] as String),
      itemOrder: (json['itemOrder'] as num?)?.toInt() ?? 0,
      createdAt: json['createdAt'] == null
          ? null
          : DateTime.parse(json['createdAt'] as String),
    );
  }
}
