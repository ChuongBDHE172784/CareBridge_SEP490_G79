class ContentListItem {
  final String id;
  final String type;
  final String title;
  final String stage;
  final String topicId;
  final String? publishedAt;

  ContentListItem({
    required this.id,
    required this.type,
    required this.title,
    required this.stage,
    required this.topicId,
    this.publishedAt,
  });

  factory ContentListItem.fromJson(Map<String, dynamic> json) =>
      ContentListItem(
        id: json['id'] as String,
        type: json['type'] as String? ?? 'ARTICLE',
        title: json['title'] as String,
        stage: json['stage'] as String? ?? '',
        topicId: json['topicId'] as String? ?? '',
        publishedAt: json['publishedAt'] as String?,
      );
}

class ContentDetail {
  final String id;
  final String type;
  final String title;
  final String body;
  final String stage;
  final String topicId;
  final int version;
  final String? publishedAt;

  ContentDetail({
    required this.id,
    required this.type,
    required this.title,
    required this.body,
    required this.stage,
    required this.topicId,
    required this.version,
    this.publishedAt,
  });

  factory ContentDetail.fromJson(Map<String, dynamic> json) => ContentDetail(
    id: json['id'] as String,
    type: json['type'] as String? ?? 'ARTICLE',
    title: json['title'] as String,
    body: json['body'] as String? ?? '',
    stage: json['stage'] as String? ?? '',
    topicId: json['topicId'] as String? ?? '',
    version: json['version'] as int? ?? 1,
    publishedAt: json['publishedAt'] as String?,
  );
}

class ChecklistTemplate {
  final String id;
  final String name;
  final String stage;
  final String description;
  final List<ChecklistItem> items;

  ChecklistTemplate({
    required this.id,
    required this.name,
    required this.stage,
    required this.description,
    required this.items,
  });

  factory ChecklistTemplate.fromJson(Map<String, dynamic> json) =>
      ChecklistTemplate(
        id: json['id'] as String,
        name: json['name'] as String,
        stage: json['stage'] as String? ?? '',
        description: json['description'] as String? ?? '',
        items: (json['items'] as List? ?? [])
            .map((e) => ChecklistItem.fromJson(e as Map<String, dynamic>))
            .toList(),
      );
}

class ChecklistItem {
  final String id;
  final String itemText;
  final int order;
  final bool isRequired;

  ChecklistItem({
    required this.id,
    required this.itemText,
    required this.order,
    required this.isRequired,
  });

  factory ChecklistItem.fromJson(Map<String, dynamic> json) => ChecklistItem(
    id: json['id'] as String,
    itemText: json['itemText'] as String,
    order: json['order'] as int? ?? 0,
    isRequired: json['isRequired'] as bool? ?? false,
  );
}
