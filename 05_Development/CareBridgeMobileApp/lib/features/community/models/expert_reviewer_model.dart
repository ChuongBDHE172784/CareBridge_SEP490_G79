class ExpertReviewer {
  final String? expertId;
  final String name;
  final String? professionalTitle;
  final String? specialty;
  final String? workplace;
  final String? bio;
  final String? avatarUrl;
  final DateTime? approvedAt;
  final String? verificationStatus;

  const ExpertReviewer({
    this.expertId,
    required this.name,
    this.professionalTitle,
    this.specialty,
    this.workplace,
    this.bio,
    this.avatarUrl,
    this.approvedAt,
    this.verificationStatus,
  });

  factory ExpertReviewer.fromJson(Map<String, dynamic> json) {
    DateTime? parsedApprovedAt;
    final rawDate = json['approvedAt'] ?? json['approved_at'];
    if (rawDate is String && rawDate.isNotEmpty) {
      parsedApprovedAt = DateTime.tryParse(rawDate)?.toLocal();
    }

    return ExpertReviewer(
      expertId: (json['expertId'] ?? json['expert_id'] ?? json['id'] ?? json['userId'])?.toString(),
      name: (json['name'] ?? json['displayName'] ?? json['fullName'] ?? json['display_name'] ?? 'Bác sĩ chuyên môn').toString(),
      professionalTitle: (json['professionalTitle'] ?? json['professional_title'])?.toString(),
      specialty: json['specialty']?.toString(),
      workplace: json['workplace']?.toString(),
      bio: (json['bio'] ?? json['consultationScope'] ?? json['consultation_scope'])?.toString(),
      avatarUrl: (json['avatarUrl'] ?? json['avatar_url'])?.toString(),
      approvedAt: parsedApprovedAt,
      verificationStatus: (json['verificationStatus'] ?? json['verification_status'] ?? 'Đã kiểm duyệt nội dung').toString(),
    );
  }

  Map<String, dynamic> toJson() => {
    'expertId': expertId,
    'name': name,
    'professionalTitle': professionalTitle,
    'specialty': specialty,
    'workplace': workplace,
    'bio': bio,
    'avatarUrl': avatarUrl,
    'approvedAt': approvedAt?.toIso8601String(),
    'verificationStatus': verificationStatus,
  };

  /// Clean display name with title prefix if not already present
  String get displayFullName {
    final title = professionalTitle?.trim();
    final trimmedName = name.trim();
    if (title != null && title.isNotEmpty && !trimmedName.toLowerCase().startsWith(title.toLowerCase())) {
      return '$title $trimmedName';
    }
    return trimmedName;
  }

  /// Compact affiliation line: e.g. "Sản khoa • Bệnh viện Từ Dũ" or "Bệnh viện Từ Dũ"
  String get affiliationLine {
    final parts = <String>[];
    if (specialty != null && specialty!.trim().isNotEmpty) {
      parts.add(specialty!.trim());
    }
    if (workplace != null && workplace!.trim().isNotEmpty) {
      parts.add(workplace!.trim());
    }
    return parts.isEmpty ? 'Chuyên gia y tế CareBridge' : parts.join(' • ');
  }
}
