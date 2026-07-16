class ExpertDirectoryItem {
  final String expertProfileId;
  final String? displayName;
  final String? professionalTitle;
  final String? specialty;
  final double? ratingAvg;
  final int? experienceYears;
  final String? avatarUrl;

  const ExpertDirectoryItem({
    required this.expertProfileId,
    this.displayName,
    this.professionalTitle,
    this.specialty,
    this.ratingAvg,
    this.experienceYears,
    this.avatarUrl,
  });

  factory ExpertDirectoryItem.fromJson(Map<String, dynamic> json) {
    return ExpertDirectoryItem(
      expertProfileId: json['expertProfileId'] as String,
      displayName: json['displayName'] as String?,
      professionalTitle: json['professionalTitle'] as String?,
      specialty: json['specialty'] as String?,
      ratingAvg: (json['ratingAvg'] as num?)?.toDouble(),
      experienceYears: json['experienceYears'] as int?,
      avatarUrl: json['avatarUrl'] as String?,
    );
  }
}

/// Mirrors backend `ExpertDirectoryResponse` — real page/size/total for infinite-scroll
/// (ADR-MEDI-001: directory pagination now actually works, not a client-side no-op).
class ExpertDirectoryPage {
  final List<ExpertDirectoryItem> experts;
  final int currentPage;
  final int pageSize;
  final int totalElements;
  final int totalPages;

  const ExpertDirectoryPage({
    required this.experts,
    required this.currentPage,
    required this.pageSize,
    required this.totalElements,
    required this.totalPages,
  });

  bool get hasMore => currentPage + 1 < totalPages;

  factory ExpertDirectoryPage.fromJson(Map<String, dynamic> json) {
    final rows = (json['experts'] as List? ?? [])
        .cast<Map<String, dynamic>>()
        .map(ExpertDirectoryItem.fromJson)
        .toList();
    return ExpertDirectoryPage(
      experts: rows,
      currentPage: json['currentPage'] as int? ?? 0,
      pageSize: json['pageSize'] as int? ?? rows.length,
      totalElements: (json['totalElements'] as num?)?.toInt() ?? rows.length,
      totalPages: json['totalPages'] as int? ?? 1,
    );
  }
}
