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

// UC-199: Question detail with answers
class CommunityAnswer {
  final String id;
  final String questionId;
  final String? authorId;
  final String body;
  final bool personalExperience;
  final bool expertLabeled;
  final String status;
  final String createdAt;

  CommunityAnswer({
    required this.id,
    required this.questionId,
    this.authorId,
    required this.body,
    required this.personalExperience,
    required this.expertLabeled,
    required this.status,
    required this.createdAt,
  });

  factory CommunityAnswer.fromJson(Map<String, dynamic> json) => CommunityAnswer(
    id: json['id'] as String,
    questionId: json['questionId'] as String? ?? '',
    authorId: json['authorId'] as String?,
    body: json['body'] as String? ?? '',
    personalExperience: json['personalExperience'] as bool? ?? false,
    expertLabeled: json['expertLabeled'] as bool? ?? false,
    status: json['status'] as String? ?? 'PENDING',
    createdAt: json['createdAt'] as String? ?? '',
  );
}

class QuestionDetail {
  final String id;
  final String? topicId;
  final String topicName;
  final String title;
  final String body;
  final String stage;
  final int? pregnancyWeek;
  final int? babyAgeMonths;
  final String urgency;
  final bool anonymous;
  final String? authorId;
  final String status;
  final int answerCount;
  final int likeCount;
  final String createdAt;
  final String updatedAt;
  final List<CommunityAnswer> answers;

  QuestionDetail({
    required this.id,
    this.topicId,
    required this.topicName,
    required this.title,
    required this.body,
    required this.stage,
    this.pregnancyWeek,
    this.babyAgeMonths,
    required this.urgency,
    required this.anonymous,
    this.authorId,
    required this.status,
    required this.answerCount,
    required this.likeCount,
    required this.createdAt,
    required this.updatedAt,
    required this.answers,
  });

  factory QuestionDetail.fromJson(Map<String, dynamic> json) => QuestionDetail(
    id: json['id'] as String,
    topicId: json['topicId'] as String?,
    topicName: json['topicName'] as String? ?? '',
    title: json['title'] as String,
    body: json['body'] as String? ?? '',
    stage: json['stage'] as String? ?? '',
    pregnancyWeek: json['pregnancyWeek'] as int?,
    babyAgeMonths: json['babyAgeMonths'] as int?,
    urgency: json['urgency'] as String? ?? 'NORMAL',
    anonymous: json['anonymous'] as bool? ?? false,
    authorId: json['authorId'] as String?,
    status: json['status'] as String? ?? 'APPROVED',
    answerCount: json['answerCount'] as int? ?? 0,
    likeCount: json['likeCount'] as int? ?? 0,
    createdAt: json['createdAt'] as String? ?? '',
    updatedAt: json['updatedAt'] as String? ?? '',
    answers: ((json['answers'] as List?) ?? [])
        .map((e) => CommunityAnswer.fromJson(e as Map<String, dynamic>))
        .toList(),
  );
}

// UC-58: Bookmark toggle response
class BookmarkToggleResult {
  final bool bookmarked;

  BookmarkToggleResult({required this.bookmarked});

  factory BookmarkToggleResult.fromJson(Map<String, dynamic> json) =>
      BookmarkToggleResult(bookmarked: json['bookmarked'] as bool? ?? false);
}

// UC-59: Answer like toggle response
class LikeToggleResult {
  final bool liked;
  final int likeCount;

  LikeToggleResult({required this.liked, required this.likeCount});

  factory LikeToggleResult.fromJson(Map<String, dynamic> json) => LikeToggleResult(
    liked: json['liked'] as bool? ?? false,
    likeCount: json['likeCount'] as int? ?? 0,
  );
}
