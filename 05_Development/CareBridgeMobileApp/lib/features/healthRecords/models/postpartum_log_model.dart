class PostpartumLog {
  const PostpartumLog({
    required this.id,
    required this.journeyId,
    required this.logDate,
    this.submissionId,
    this.painLevel,
    this.bleedingLevel,
    this.moodLevel,
    this.sleepHours,
    this.breastfeedingNote,
    this.symptomNote,
    this.createdAt,
    this.aiInsight,
    this.redFlagAlert = false,
  });

  final String id;
  final String journeyId;
  final String? submissionId;
  final DateTime logDate;
  final int? painLevel;
  final String? bleedingLevel;
  final int? moodLevel;
  final double? sleepHours;
  final String? breastfeedingNote;
  final String? symptomNote;
  final DateTime? createdAt;
  final String? aiInsight;
  final bool redFlagAlert;

  factory PostpartumLog.fromJson(Map<String, dynamic> json) => PostpartumLog(
    id: json['postpartumLogId'] as String,
    journeyId: json['journeyId'] as String,
    submissionId: json['submissionId'] as String?,
    logDate: DateTime.parse(json['logDate'] as String),
    painLevel: (json['painLevel'] as num?)?.toInt(),
    bleedingLevel: json['bleedingLevel'] as String?,
    moodLevel: (json['moodLevel'] as num?)?.toInt(),
    sleepHours: (json['sleepHours'] as num?)?.toDouble(),
    breastfeedingNote: json['breastfeedingNote'] as String?,
    symptomNote: json['symptomNote'] as String?,
    createdAt: json['createdAt'] == null
        ? null
        : DateTime.parse(json['createdAt'] as String),
    aiInsight: json['aiInsight'] as String?,
    redFlagAlert: json['redFlagAlert'] as bool? ?? false,
  );
}

class PostpartumLogPage {
  const PostpartumLogPage({
    required this.items,
    required this.page,
    required this.totalPages,
    required this.totalElements,
  });

  final List<PostpartumLog> items;
  final int page;
  final int totalPages;
  final int totalElements;

  bool get hasNext => page + 1 < totalPages;
}

class PostpartumLogDraft {
  const PostpartumLogDraft({
    required this.submissionId,
    required this.logDate,
    this.painLevel,
    this.bleedingLevel,
    this.moodLevel,
    this.sleepHours,
    this.breastfeedingNote,
    this.symptomNote,
  });

  final String submissionId;
  final DateTime logDate;
  final int? painLevel;
  final String? bleedingLevel;
  final int? moodLevel;
  final double? sleepHours;
  final String? breastfeedingNote;
  final String? symptomNote;

  Map<String, dynamic> toJson({
    bool includeSubmissionId = true,
    bool includeEmptyOptionals = false,
  }) => {
    if (includeSubmissionId) 'submissionId': submissionId,
    'logDate': _apiDate(logDate),
    if (painLevel != null) 'painLevel': painLevel,
    if (bleedingLevel != null) 'bleedingLevel': bleedingLevel,
    if (moodLevel != null) 'moodLevel': moodLevel,
    if (sleepHours != null) 'sleepHours': sleepHours,
    if (includeEmptyOptionals)
      'breastfeedingNote': breastfeedingNote?.trim()
    else if (breastfeedingNote?.trim().isNotEmpty == true)
      'breastfeedingNote': breastfeedingNote!.trim(),
    if (includeEmptyOptionals)
      'symptomNote': symptomNote?.trim()
    else if (symptomNote?.trim().isNotEmpty == true)
      'symptomNote': symptomNote!.trim(),
  };

  static String _apiDate(DateTime value) =>
      '${value.year.toString().padLeft(4, '0')}-'
      '${value.month.toString().padLeft(2, '0')}-'
      '${value.day.toString().padLeft(2, '0')}';
}
