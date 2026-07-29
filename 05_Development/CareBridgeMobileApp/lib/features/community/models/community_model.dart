class CommunityTopic {
  final String id;
  final String name;
  final String description;
  final String icon;
  final String type;
  final String? parentId;
  final bool isHidden;
  final int sortOrder;
  final bool isFollowed;
  // Real count of APPROVED questions under this topic (ADR-COM-015 in
  // CommunityTopicManagement_TDS.md) — replaces the previous sortOrder*100 fake value.
  final int questionCount;

  CommunityTopic({
    required this.id,
    required this.name,
    required this.description,
    required this.icon,
    this.type = 'TOPIC',
    this.parentId,
    required this.isHidden,
    required this.sortOrder,
    this.isFollowed = false,
    this.questionCount = 0,
  });

  factory CommunityTopic.fromJson(Map<String, dynamic> json) => CommunityTopic(
    id: json['id'] as String,
    name: json['name'] as String,
    description: json['description'] as String? ?? '',
    icon: json['icon'] as String? ?? 'topic',
    type: json['type'] as String? ?? 'TOPIC',
    parentId: json['parentId'] as String?,
    isHidden: json['isHidden'] as bool? ?? false,
    sortOrder: json['sortOrder'] as int? ?? 0,
    isFollowed: json['isFollowed'] as bool? ?? false,
    questionCount: json['questionCount'] as int? ?? 0,
  );

  CommunityTopic copyWith({bool? isFollowed}) => CommunityTopic(
    id: id,
    name: name,
    description: description,
    icon: icon,
    type: type,
    parentId: parentId,
    isHidden: isHidden,
    sortOrder: sortOrder,
    isFollowed: isFollowed ?? this.isFollowed,
    questionCount: questionCount,
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
  final bool bookmarked;
  final bool liked;
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
    required this.bookmarked,
    required this.liked,
    required this.createdAt,
  });

  factory CommunityFeedItem.fromJson(Map<String, dynamic> json) =>
      CommunityFeedItem(
        id: json['id'] as String,
        title: json['title'] as String,
        topicName: json['topicName'] as String? ?? '',
        authorDisplay: json['authorDisplay'] as String? ?? 'Ẩn danh',
        stage: json['stage'] as String? ?? '',
        urgency: json['urgency'] as String? ?? 'NORMAL',
        answerCount: json['answerCount'] as int? ?? 0,
        likeCount: json['likeCount'] as int? ?? 0,
        hasExpertAnswer: json['hasExpertAnswer'] as bool? ?? false,
        bookmarked: json['bookmarked'] as bool? ?? false,
        liked: json['liked'] as bool? ?? false,
        createdAt: json['createdAt'] as String? ?? '',
      );

  CommunityFeedItem copyWith({bool? bookmarked, bool? liked, int? likeCount}) =>
      CommunityFeedItem(
        id: id,
        title: title,
        topicName: topicName,
        authorDisplay: authorDisplay,
        stage: stage,
        urgency: urgency,
        answerCount: answerCount,
        likeCount: likeCount ?? this.likeCount,
        hasExpertAnswer: hasExpertAnswer,
        bookmarked: bookmarked ?? this.bookmarked,
        liked: liked ?? this.liked,
        createdAt: createdAt,
      );
}

// UC-199: Question detail with answers
class CommunityAnswer {
  final String id;
  final String questionId;
  final String? authorId;
  final String? authorDisplay;
  final String body;
  final List<String> imageUrls;
  final bool personalExperience;
  final bool expertLabeled;
  final String? expertProfileId;
  final String status;
  final int likeCount;
  final bool liked;
  final String createdAt;

  CommunityAnswer({
    required this.id,
    required this.questionId,
    this.authorId,
    this.authorDisplay,
    required this.body,
    this.imageUrls = const [],
    required this.personalExperience,
    required this.expertLabeled,
    this.expertProfileId,
    required this.status,
    required this.likeCount,
    required this.liked,
    required this.createdAt,
  });

  factory CommunityAnswer.fromJson(Map<String, dynamic> json) =>
      CommunityAnswer(
        id: json['id'] as String,
        questionId: json['questionId'] as String? ?? '',
        authorId: json['authorId'] as String?,
        authorDisplay: json['authorDisplay'] as String?,
        body: json['body'] as String? ?? '',
        imageUrls: ((json['imageUrls'] as List?) ?? const [])
            .whereType<String>()
            .toList(),
        personalExperience: json['personalExperience'] as bool? ?? false,
        expertLabeled: json['expertLabeled'] as bool? ?? false,
        expertProfileId: json['expertProfileId'] as String?,
        status: json['status'] as String? ?? 'PENDING',
        likeCount: json['likeCount'] as int? ?? 0,
        liked: json['liked'] as bool? ?? false,
        createdAt: json['createdAt'] as String? ?? '',
      );

  CommunityAnswer copyWith({bool? liked, int? likeCount}) => CommunityAnswer(
    id: id,
    questionId: questionId,
    authorId: authorId,
    authorDisplay: authorDisplay,
    body: body,
    imageUrls: imageUrls,
    personalExperience: personalExperience,
    expertLabeled: expertLabeled,
    expertProfileId: expertProfileId,
    status: status,
    likeCount: likeCount ?? this.likeCount,
    liked: liked ?? this.liked,
    createdAt: createdAt,
  );
}

class QuestionDetail {
  final String id;
  final String? topicId;
  final String topicName;
  final String title;
  final String body;
  final List<String> imageUrls;
  final String stage;
  final int? pregnancyWeek;
  final int? babyAgeMonths;
  final String urgency;
  final bool anonymous;
  final String? authorId;
  final String? authorDisplay;
  final String status;
  final int answerCount;
  final int likeCount;
  final bool isBookmarked;
  final bool isLiked;
  final String createdAt;
  final String updatedAt;
  final List<CommunityAnswer> answers;

  QuestionDetail({
    required this.id,
    this.topicId,
    required this.topicName,
    required this.title,
    required this.body,
    this.imageUrls = const [],
    required this.stage,
    this.pregnancyWeek,
    this.babyAgeMonths,
    required this.urgency,
    required this.anonymous,
    this.authorId,
    this.authorDisplay,
    required this.status,
    required this.answerCount,
    required this.likeCount,
    required this.isBookmarked,
    required this.isLiked,
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
    imageUrls: ((json['imageUrls'] as List?) ?? const [])
        .whereType<String>()
        .toList(),
    stage: json['stage'] as String? ?? '',
    pregnancyWeek: json['pregnancyWeek'] as int?,
    babyAgeMonths: json['babyAgeMonths'] as int?,
    urgency: json['urgency'] as String? ?? 'NORMAL',
    anonymous: json['anonymous'] as bool? ?? false,
    authorId: json['authorId'] as String?,
    authorDisplay: json['authorDisplay'] as String?,
    status: json['status'] as String? ?? 'APPROVED',
    answerCount: json['answerCount'] as int? ?? 0,
    likeCount: json['likeCount'] as int? ?? 0,
    isBookmarked: json['isBookmarked'] as bool? ?? false,
    isLiked: json['isLiked'] as bool? ?? false,
    createdAt: json['createdAt'] as String? ?? '',
    updatedAt: json['updatedAt'] as String? ?? '',
    answers: ((json['answers'] as List?) ?? [])
        .map((e) => CommunityAnswer.fromJson(e as Map<String, dynamic>))
        .toList(),
  );
}

class MyCommunityQuestion {
  final String id;
  final String title;
  final String body;
  final List<String> imageUrls;
  final String status;
  final int answerCount;
  final int likeCount;
  final String createdAt;
  final String updatedAt;

  const MyCommunityQuestion({
    required this.id,
    required this.title,
    required this.body,
    required this.imageUrls,
    required this.status,
    required this.answerCount,
    required this.likeCount,
    required this.createdAt,
    required this.updatedAt,
  });

  factory MyCommunityQuestion.fromJson(Map<String, dynamic> json) =>
      MyCommunityQuestion(
        id: json['id'] as String,
        title: json['title'] as String? ?? '',
        body: json['body'] as String? ?? '',
        imageUrls: ((json['imageUrls'] as List?) ?? const [])
            .whereType<String>()
            .toList(),
        status: json['status'] as String? ?? 'PENDING',
        answerCount: json['answerCount'] as int? ?? 0,
        likeCount: json['likeCount'] as int? ?? 0,
        createdAt: json['createdAt'] as String? ?? '',
        updatedAt: json['updatedAt'] as String? ?? '',
      );

  MyCommunityQuestion copyWith({String? status}) => MyCommunityQuestion(
    id: id,
    title: title,
    body: body,
    imageUrls: imageUrls,
    status: status ?? this.status,
    answerCount: answerCount,
    likeCount: likeCount,
    createdAt: createdAt,
    updatedAt: updatedAt,
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

  factory LikeToggleResult.fromJson(Map<String, dynamic> json) =>
      LikeToggleResult(
        liked: json['liked'] as bool? ?? false,
        likeCount: json['likeCount'] as int? ?? 0,
      );
}

// Question like toggle response
class QuestionLikeToggleResult {
  final bool liked;
  final int likeCount;

  QuestionLikeToggleResult({required this.liked, required this.likeCount});

  factory QuestionLikeToggleResult.fromJson(Map<String, dynamic> json) =>
      QuestionLikeToggleResult(
        liked: json['liked'] as bool? ?? false,
        likeCount: json['likeCount'] as int? ?? 0,
      );
}
