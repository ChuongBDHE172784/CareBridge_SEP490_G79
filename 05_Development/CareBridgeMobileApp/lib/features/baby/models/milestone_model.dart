enum MilestoneType { roll, crawl, walk, talk, tooth, solids, other }

extension MilestoneTypeExtension on MilestoneType {
  String get displayLabel {
    switch (this) {
      case MilestoneType.roll:
        return 'Lẫy';
      case MilestoneType.crawl:
        return 'Bò';
      case MilestoneType.walk:
        return 'Đi';
      case MilestoneType.talk:
        return 'Nói';
      case MilestoneType.tooth:
        return 'Mọc răng';
      case MilestoneType.solids:
        return 'Ăn dặm';
      case MilestoneType.other:
        return 'Mốc khác';
    }
  }

  /// Values must match the backend's accepted milestone types.
  String toApiValue() {
    switch (this) {
      case MilestoneType.roll:
        return 'ROLLING';
      case MilestoneType.crawl:
        return 'CRAWLING';
      case MilestoneType.walk:
        return 'WALKING';
      case MilestoneType.talk:
        return 'SPEAKING';
      case MilestoneType.tooth:
        return 'TEETHING';
      case MilestoneType.solids:
        return 'WEANING';
      case MilestoneType.other:
        return 'OTHER';
    }
  }

  static MilestoneType fromApi(String? v) {
    switch (v?.trim().toUpperCase()) {
      case 'ROLLING':
      case 'ROLL':
        return MilestoneType.roll;
      case 'CRAWLING':
      case 'CRAWL':
        return MilestoneType.crawl;
      case 'WALKING':
      case 'WALK':
        return MilestoneType.walk;
      case 'SPEAKING':
      case 'TALK':
        return MilestoneType.talk;
      case 'TEETHING':
      case 'TOOTH':
        return MilestoneType.tooth;
      case 'WEANING':
      case 'SOLIDS':
        return MilestoneType.solids;
      case 'OTHER':
      case 'FIRST_SMILE':
      case 'SITTING':
      case 'STANDING':
      default:
        return MilestoneType.other;
    }
  }
}

class Milestone {
  final String id;
  final String babyId;
  final MilestoneType milestoneType;
  final DateTime achievedDate;
  final String? note;
  final String? sourceType;
  final DateTime? createdAt;

  const Milestone({
    required this.id,
    required this.babyId,
    required this.milestoneType,
    required this.achievedDate,
    this.note,
    this.sourceType,
    this.createdAt,
  });

  factory Milestone.fromJson(Map<String, dynamic> json) {
    return Milestone(
      id: json['milestoneId']?.toString() ?? '',
      babyId: json['babyId']?.toString() ?? '',
      milestoneType: MilestoneTypeExtension.fromApi(
        json['milestoneType'] as String?,
      ),
      achievedDate: json['achievedDate'] != null
          ? DateTime.parse(json['achievedDate'] as String)
          : DateTime.now(),
      note: json['note'] as String?,
      sourceType: json['sourceType'] as String?,
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'] as String)
          : null,
    );
  }
}

class AddMilestoneRequest {
  final MilestoneType milestoneType;
  final DateTime achievedDate;
  final String? note;

  const AddMilestoneRequest({
    required this.milestoneType,
    required this.achievedDate,
    this.note,
  });

  Map<String, dynamic> toJson() {
    final d = achievedDate;
    final dateStr =
        '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
    return {
      'milestoneType': milestoneType.toApiValue(),
      'achievedDate': dateStr,
      if (note != null && note!.isNotEmpty) 'note': note,
      'sourceType': 'MOTHER_INPUT',
    };
  }
}
