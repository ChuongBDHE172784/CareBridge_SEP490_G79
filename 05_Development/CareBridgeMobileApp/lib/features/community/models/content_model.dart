enum ContentBrowseMode { generic, lifecycle }

class LifecycleEnvelope<T> {
  final String stage;
  final T payload;

  const LifecycleEnvelope({required this.stage, required this.payload});

  factory LifecycleEnvelope.fromApiResponse(
    Map<String, dynamic> json,
    T Function(Object? payload) parsePayload,
  ) {
    final data = json['data'];
    if (data is! Map<String, dynamic>) {
      throw const FormatException('Lifecycle response data is missing');
    }
    final stage = data['stage'];
    if (stage is! String || !_canonicalMotherStages.contains(stage)) {
      throw const FormatException('Lifecycle response stage is invalid');
    }
    return LifecycleEnvelope<T>(
      stage: stage,
      payload: parsePayload(data['payload']),
    );
  }
}

const _canonicalMotherStages = <String>{
  'PRE_PREGNANCY',
  'PREGNANCY',
  'POSTPARTUM',
};

class PaginatedContent {
  final List<ContentListItem> data;
  final int page;
  final int size;
  final int totalElements;
  final int totalPages;

  const PaginatedContent({
    required this.data,
    required this.page,
    required this.size,
    required this.totalElements,
    required this.totalPages,
  });

  factory PaginatedContent.fromJson(Map<String, dynamic> json) {
    final rows = json['data'];
    if (rows is! List) {
      throw const FormatException('Paginated content data is missing');
    }
    return PaginatedContent(
      data: rows
          .map(
            (item) => ContentListItem.fromJson(
              Map<String, dynamic>.from(item as Map),
            ),
          )
          .toList(growable: false),
      page: (json['page'] as num?)?.toInt() ?? 0,
      size: (json['size'] as num?)?.toInt() ?? rows.length,
      totalElements: (json['totalElements'] as num?)?.toInt() ?? rows.length,
      totalPages: (json['totalPages'] as num?)?.toInt() ?? 0,
    );
  }

  factory PaginatedContent.fromApiResponse(Map<String, dynamic> json) =>
      PaginatedContent.fromJson(json);
}

class ContentListItem {
  final String id;
  final String type;
  final String title;
  final String? summary;
  final String stage;
  final String topicId;
  final List<String> tagIds;
  final String? publishedAt;

  ContentListItem({
    required this.id,
    required this.type,
    required this.title,
    this.summary,
    required this.stage,
    required this.topicId,
    this.tagIds = const [],
    this.publishedAt,
  });

  factory ContentListItem.fromJson(Map<String, dynamic> json) =>
      ContentListItem(
        id: json['id'] as String,
        type: json['type'] as String? ?? 'ARTICLE',
        title: json['title'] as String,
        summary: json['summary'] as String?,
        stage: json['stage'] as String? ?? '',
        topicId: json['topicId'] as String? ?? '',
        tagIds: (json['tagIds'] as List? ?? const [])
            .whereType<String>()
            .toList(growable: false),
        publishedAt: json['publishedAt'] as String?,
      );
}

int contentStageIndexForJourneyType(String? journeyType) {
  switch (journeyType) {
    case 'PRE_PREGNANCY':
      return 0;
    case 'PREGNANCY':
      return 1;
    case 'POSTPARTUM':
      return 2;
    case 'BABY_CARE':
      return 3;
    default:
      return -1;
  }
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

  /// Image sources are intentionally derived from the server-sanitized body.
  /// The public list contract has no image field, while the detail contract is
  /// the canonical representation of rich article and FAQ content.
  List<String> get imageUrls =>
      RegExp(
            r'''<img\b[^>]*\ssrc\s*=\s*(?:["']([^"']+)["']|([^\s>]+))''',
            caseSensitive: false,
          )
          .allMatches(body)
          .map((match) => match.group(1) ?? match.group(2)!)
          .toList(growable: false);
}

class ChecklistTemplate {
  final String id;
  final String name;
  final String stage;
  final String description;
  final String templateType;
  final List<ChecklistItem> items;

  ChecklistTemplate({
    required this.id,
    required this.name,
    required this.stage,
    required this.description,
    this.templateType = 'OPTIONAL',
    required this.items,
  });

  factory ChecklistTemplate.fromJson(Map<String, dynamic> json) =>
      ChecklistTemplate(
        id: json['id'] as String,
        name: json['name'] as String,
        stage: json['stage'] as String? ?? '',
        description: json['description'] as String? ?? '',
        templateType: json['templateType'] as String? ?? 'OPTIONAL',
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
