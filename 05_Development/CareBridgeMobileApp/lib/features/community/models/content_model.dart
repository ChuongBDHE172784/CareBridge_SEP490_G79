import '../../../core/constants/content_stages.dart';

enum ContentBrowseMode { generic, lifecycle, family }

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
    final rawStage = data['stage'];
    final stage = rawStage is String
        ? tryNormalizeContentStage(rawStage)
        : null;
    if (stage == null) {
      throw const FormatException('Lifecycle response stage is invalid');
    }
    return LifecycleEnvelope<T>(
      stage: stage,
      payload: parsePayload(data['payload']),
    );
  }
}

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
        stage: normalizeContentStage(json['stage'] as String?, fallback: ''),
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
      return 2;
    default:
      return -1;
  }
}

class ContentSource {
  final String title;
  final String? url;
  final String? publisher;

  const ContentSource({
    required this.title,
    this.url,
    this.publisher,
  });

  factory ContentSource.fromJson(Map<String, dynamic> json) => ContentSource(
        title: json['title'] as String? ?? '',
        url: json['url'] as String?,
        publisher: json['publisher'] as String?,
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
  final String? sourceLabel;
  final List<ContentSource>? sources;

  ContentDetail({
    required this.id,
    required this.type,
    required this.title,
    required this.body,
    required this.stage,
    required this.topicId,
    required this.version,
    this.publishedAt,
    this.sourceLabel,
    this.sources,
  });

  factory ContentDetail.fromJson(Map<String, dynamic> json) => ContentDetail(
    id: json['id'] as String,
    type: json['type'] as String? ?? 'ARTICLE',
    title: json['title'] as String,
    body: json['body'] as String? ?? '',
    stage: normalizeContentStage(json['stage'] as String?, fallback: ''),
    topicId: json['topicId'] as String? ?? '',
    version: json['version'] as int? ?? 1,
    publishedAt: json['publishedAt'] as String?,
    sourceLabel: json['sourceLabel'] as String?,
    sources: json['sources'] is List
        ? (json['sources'] as List)
            .whereType<Map>()
            .map((e) => ContentSource.fromJson(Map<String, dynamic>.from(e)))
            .toList(growable: false)
        : null,
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
