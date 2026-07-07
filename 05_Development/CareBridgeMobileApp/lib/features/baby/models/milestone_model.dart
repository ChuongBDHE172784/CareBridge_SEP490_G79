enum MilestoneType { roll, crawl, walk, talk, tooth, solids, other }

extension MilestoneTypeExtension on MilestoneType {
  String get displayLabel {
    switch (this) {
      case MilestoneType.roll: return 'Lẫy';
      case MilestoneType.crawl: return 'Bò';
      case MilestoneType.walk: return 'Đi';
      case MilestoneType.talk: return 'Nói';
      case MilestoneType.tooth: return 'Mọc răng';
      case MilestoneType.solids: return 'Ăn dặm';
      case MilestoneType.other: return 'Mốc khác';
    }
  }

  String toApiValue() {
    switch (this) {
      case MilestoneType.roll: return 'ROLL';
      case MilestoneType.crawl: return 'CRAWL';
      case MilestoneType.walk: return 'WALK';
      case MilestoneType.talk: return 'TALK';
      case MilestoneType.tooth: return 'TOOTH';
      case MilestoneType.solids: return 'SOLIDS';
      case MilestoneType.other: return 'OTHER';
    }
  }

  static MilestoneType fromApi(String? v) {
    switch (v) {
      case 'ROLL': return MilestoneType.roll;
      case 'CRAWL': return MilestoneType.crawl;
      case 'WALK': return MilestoneType.walk;
      case 'TALK': return MilestoneType.talk;
      case 'TOOTH': return MilestoneType.tooth;
      case 'SOLIDS': return MilestoneType.solids;
      default: return MilestoneType.other;
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
      milestoneType: MilestoneTypeExtension.fromApi(json['milestoneType'] as String?),
      achievedDate: json['achievedDate'] != null
          ? DateTime.parse(json['achievedDate'] as String)
          : DateTime.now(),
      note: json['note'] as String?,
      sourceType: json['sourceType'] as String?,
      createdAt: json['createdAt'] != null ? DateTime.parse(json['createdAt'] as String) : null,
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
