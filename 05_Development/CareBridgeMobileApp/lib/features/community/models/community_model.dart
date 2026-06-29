class CommunityTopic {
  final String id;
  final String name;
  final String description;
  final String icon;
  final bool isHidden;
  final int sortOrder;

  CommunityTopic({
    required this.id,
    required this.name,
    required this.description,
    required this.icon,
    required this.isHidden,
    required this.sortOrder,
  });

  factory CommunityTopic.fromJson(Map<String, dynamic> json) => CommunityTopic(
    id: json['id'] as String,
    name: json['name'] as String,
    description: json['description'] as String? ?? '',
    icon: json['icon'] as String? ?? 'topic',
    isHidden: json['isHidden'] as bool? ?? false,
    sortOrder: json['sortOrder'] as int? ?? 0,
  );
}

class CommunityFeedItem {
  final String id;
  final String title;
  final String topicName;
  final String authorDisplay;
  final String stage;
  final String urgency;
  final int answerCount;
  final int likeCount;
  final bool hasExpertAnswer;
  final String createdAt;

  CommunityFeedItem({
    required this.id,
    required this.title,
    required this.topicName,
    required this.authorDisplay,
    required this.stage,
    required this.urgency,
    required this.answerCount,
    required this.likeCount,
    required this.hasExpertAnswer,
    required this.createdAt,
  });

  factory CommunityFeedItem.fromJson(Map<String, dynamic> json) => CommunityFeedItem(
    id: json['id'] as String,
    title: json['title'] as String,
    topicName: json['topicName'] as String? ?? '',
    authorDisplay: json['authorDisplay'] as String? ?? 'Ẩn danh',
    stage: json['stage'] as String? ?? '',
    urgency: json['urgency'] as String? ?? 'NORMAL',
    answerCount: json['answerCount'] as int? ?? 0,
    likeCount: json['likeCount'] as int? ?? 0,
    hasExpertAnswer: json['hasExpertAnswer'] as bool? ?? false,
    createdAt: json['createdAt'] as String? ?? '',
  );
}
