class ExpertApprovalQueueItem {
  final String id;
  final String kind; // 'CONTENT' or 'CHECKLIST'
  final String type; // 'ARTICLE', 'FAQ', 'CHECKLIST'
  final String title;
  final String stage; // 'PRE_PREGNANCY', 'PREGNANCY', 'POSTPARTUM', 'BABY_CARE'
  final String status; // 'PENDING_REVIEW', etc.
  final int? versionNo;
  final int? itemCount;
  final String? summary;
  final String? body;
  final String? sourceLabel;
  final DateTime? assignedAt;
  final DateTime? updatedAt;
  final DateTime? createdAt;

  const ExpertApprovalQueueItem({
    required this.id,
    required this.kind,
    required this.type,
    required this.title,
    required this.stage,
    required this.status,
    this.versionNo,
    this.itemCount,
    this.summary,
    this.body,
    this.sourceLabel,
    this.assignedAt,
    this.updatedAt,
    this.createdAt,
  });

  factory ExpertApprovalQueueItem.fromJson(Map<String, dynamic> json) {
    DateTime? parseDt(dynamic val) {
      if (val == null) return null;
      try {
        return DateTime.parse(val.toString());
      } catch (_) {
        return null;
      }
    }

    return ExpertApprovalQueueItem(
      id: (json['id'] ?? '').toString(),
      kind: (json['kind'] ?? 'CONTENT').toString(),
      type: (json['type'] ?? 'ARTICLE').toString(),
      title: (json['title'] ?? json['name'] ?? '').toString(),
      stage: (json['stage'] ?? 'PREGNANCY').toString(),
      status: (json['status'] ?? 'PENDING_REVIEW').toString(),
      versionNo: json['versionNo'] as int? ?? json['version'] as int?,
      itemCount: json['itemCount'] as int?,
      summary: json['summary'] as String?,
      body: json['body'] as String?,
      sourceLabel: json['sourceLabel'] as String?,
      assignedAt: parseDt(json['assignedAt']),
      updatedAt: parseDt(json['updatedAt']),
      createdAt: parseDt(json['createdAt']),
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'kind': kind,
    'type': type,
    'title': title,
    'stage': stage,
    'status': status,
    'versionNo': versionNo,
    'itemCount': itemCount,
    'summary': summary,
    'body': body,
    'sourceLabel': sourceLabel,
    'assignedAt': assignedAt?.toIso8601String(),
    'updatedAt': updatedAt?.toIso8601String(),
    'createdAt': createdAt?.toIso8601String(),
  };
}

class ReviewFeedback {
  final String reason;
  final DateTime? requestedAt;
  final String? requestedBy;
  final int? versionNo;

  const ReviewFeedback({
    required this.reason,
    this.requestedAt,
    this.requestedBy,
    this.versionNo,
  });

  factory ReviewFeedback.fromJson(Map<String, dynamic> json) {
    DateTime? parseDt(dynamic val) {
      if (val == null) return null;
      try {
        return DateTime.parse(val.toString());
      } catch (_) {
        return null;
      }
    }

    return ReviewFeedback(
      reason: (json['reason'] ?? '').toString(),
      requestedAt: parseDt(json['requestedAt']),
      requestedBy: json['requestedBy'] as String?,
      versionNo: json['versionNo'] as int?,
    );
  }
}

class ContentDetailModel {
  final String id;
  final String type;
  final String title;
  final String body;
  final String? summary;
  final String stage;
  final String? topicId;
  final List<String> tagIds;
  final int? eligibleFromWeek;
  final int? eligibleToWeek;
  final int? recommendationPriority;
  final int version;
  final DateTime? publishedAt;
  final String status;
  final DateTime? createdAt;
  final DateTime? updatedAt;
  final String? sourceLabel;
  final String? assignedExpertId;
  final DateTime? assignedAt;
  final String? approvedBy;
  final DateTime? approvedAt;
  final ReviewFeedback? latestReviewFeedback;

  const ContentDetailModel({
    required this.id,
    required this.type,
    required this.title,
    required this.body,
    this.summary,
    required this.stage,
    this.topicId,
    this.tagIds = const [],
    this.eligibleFromWeek,
    this.eligibleToWeek,
    this.recommendationPriority,
    this.version = 1,
    this.publishedAt,
    required this.status,
    this.createdAt,
    this.updatedAt,
    this.sourceLabel,
    this.assignedExpertId,
    this.assignedAt,
    this.approvedBy,
    this.approvedAt,
    this.latestReviewFeedback,
  });

  factory ContentDetailModel.fromJson(Map<String, dynamic> json) {
    DateTime? parseDt(dynamic val) {
      if (val == null) return null;
      try {
        return DateTime.parse(val.toString());
      } catch (_) {
        return null;
      }
    }

    return ContentDetailModel(
      id: (json['id'] ?? '').toString(),
      type: (json['type'] ?? 'ARTICLE').toString(),
      title: (json['title'] ?? '').toString(),
      body: (json['body'] ?? '').toString(),
      summary: json['summary'] as String?,
      stage: (json['stage'] ?? 'PREGNANCY').toString(),
      topicId: json['topicId'] as String?,
      tagIds: (json['tagIds'] as List? ?? []).map((e) => e.toString()).toList(),
      eligibleFromWeek: json['eligibleFromWeek'] as int?,
      eligibleToWeek: json['eligibleToWeek'] as int?,
      recommendationPriority: json['recommendationPriority'] as int?,
      version: json['version'] as int? ?? 1,
      publishedAt: parseDt(json['publishedAt']),
      status: (json['status'] ?? 'PENDING_REVIEW').toString(),
      createdAt: parseDt(json['createdAt']),
      updatedAt: parseDt(json['updatedAt']),
      sourceLabel: json['sourceLabel'] as String?,
      assignedExpertId: json['assignedExpertId'] as String?,
      assignedAt: parseDt(json['assignedAt']),
      approvedBy: json['approvedBy'] as String?,
      approvedAt: parseDt(json['approvedAt']),
      latestReviewFeedback: json['latestReviewFeedback'] is Map<String, dynamic>
          ? ReviewFeedback.fromJson(json['latestReviewFeedback'] as Map<String, dynamic>)
          : null,
    );
  }
}

class ChecklistTemplateItemModel {
  final String id;
  final String itemText;
  final int order;
  final bool isRequired;
  final String? targetSubject;
  final String? description;
  final String? sourceUrl;
  final String? supportFunction;
  final bool repeatWeekly;
  final bool repeatDaily;

  const ChecklistTemplateItemModel({
    required this.id,
    required this.itemText,
    required this.order,
    this.isRequired = false,
    this.targetSubject,
    this.description,
    this.sourceUrl,
    this.supportFunction,
    this.repeatWeekly = false,
    this.repeatDaily = false,
  });

  factory ChecklistTemplateItemModel.fromJson(Map<String, dynamic> json) {
    return ChecklistTemplateItemModel(
      id: (json['id'] ?? '').toString(),
      itemText: (json['itemText'] ?? json['text'] ?? '').toString(),
      order: json['order'] as int? ?? 0,
      isRequired: json['isRequired'] == true,
      targetSubject: json['targetSubject'] as String?,
      description: json['description'] as String?,
      sourceUrl: json['sourceUrl'] as String?,
      supportFunction: json['supportFunction'] as String?,
      repeatWeekly: json['repeatWeekly'] == true,
      repeatDaily: json['repeatDaily'] == true,
    );
  }
}

class ChecklistTemplateDetailModel {
  final String id;
  final String name;
  final String? stage;
  final String status;
  final String description;
  final String? templateType;
  final List<ChecklistTemplateItemModel> items;
  final ReviewFeedback? latestReviewFeedback;
  final DateTime? createdAt;
  final DateTime? updatedAt;
  final List<String> recipientRoles;

  const ChecklistTemplateDetailModel({
    required this.id,
    required this.name,
    this.stage,
    required this.status,
    required this.description,
    this.templateType,
    this.items = const [],
    this.latestReviewFeedback,
    this.createdAt,
    this.updatedAt,
    this.recipientRoles = const [],
  });

  factory ChecklistTemplateDetailModel.fromJson(Map<String, dynamic> json) {
    DateTime? parseDt(dynamic val) {
      if (val == null) return null;
      try {
        return DateTime.parse(val.toString());
      } catch (_) {
        return null;
      }
    }

    final rawItems = json['items'] as List? ?? [];
    return ChecklistTemplateDetailModel(
      id: (json['id'] ?? '').toString(),
      name: (json['name'] ?? json['title'] ?? '').toString(),
      stage: json['stage'] as String?,
      status: (json['status'] ?? 'PENDING_REVIEW').toString(),
      description: (json['description'] ?? '').toString(),
      templateType: json['templateType'] as String?,
      items: rawItems
          .map((i) => ChecklistTemplateItemModel.fromJson(i as Map<String, dynamic>))
          .toList(),
      latestReviewFeedback: json['latestReviewFeedback'] is Map<String, dynamic>
          ? ReviewFeedback.fromJson(json['latestReviewFeedback'] as Map<String, dynamic>)
          : null,
      createdAt: parseDt(json['createdAt']),
      updatedAt: parseDt(json['updatedAt']),
      recipientRoles: (json['recipientRoles'] as List? ?? [])
          .map((e) => e.toString())
          .toList(),
    );
  }
}

class PaginatedApprovalQueue {
  final List<ExpertApprovalQueueItem> content;
  final int totalElements;
  final int totalPages;
  final int size;
  final int number;

  const PaginatedApprovalQueue({
    required this.content,
    required this.totalElements,
    required this.totalPages,
    required this.size,
    required this.number,
  });

  factory PaginatedApprovalQueue.fromJson(Map<String, dynamic> json) {
    final rawList = json['content'] as List? ?? [];
    return PaginatedApprovalQueue(
      content: rawList
          .map((item) => ExpertApprovalQueueItem.fromJson(item as Map<String, dynamic>))
          .toList(),
      totalElements: json['totalElements'] as int? ?? rawList.length,
      totalPages: json['totalPages'] as int? ?? 1,
      size: json['size'] as int? ?? 20,
      number: json['number'] as int? ?? json['page'] as int? ?? 0,
    );
  }
}

String getApprovalTypeLabel(String type) {
  switch (type.toUpperCase()) {
    case 'ARTICLE':
      return 'Bài viết';
    case 'FAQ':
      return 'Hỏi & Đáp (FAQ)';
    case 'CHECKLIST':
      return 'Checklist sức khỏe';
    default:
      return type;
  }
}

String getApprovalStageLabel(String? stage) {
  if (stage == null) return 'Mọi giai đoạn';
  switch (stage.toUpperCase()) {
    case 'PRE_PREGNANCY':
      return 'Chuẩn bị mang thai';
    case 'PREGNANCY':
      return 'Mang thai';
    case 'POSTPARTUM':
      return 'Sau sinh';
    case 'BABY_CARE':
      return 'Chăm sóc bé';
    default:
      return stage;
  }
}

String getApprovalStatusLabel(String status) {
  switch (status.toUpperCase()) {
    case 'PENDING_REVIEW':
      return 'Chờ thẩm định';
    case 'APPROVED':
      return 'Đã duyệt & xuất bản';
    case 'REJECTED':
      return 'Yêu cầu chỉnh sửa';
    case 'DRAFT':
      return 'Bản nháp';
    case 'ARCHIVED':
      return 'Đã lưu trữ';
    default:
      return status;
  }
}
