import 'dart:convert';

class ChecklistItemShareData {
  final String text;
  final bool completed;
  final String? category;
  final String? timeLabel; // ví dụ: "Tuần 12", "Đã xong", "Tuần 32 (Sắp tới)"
  final String? origin;
  final String? createdBy;
  final bool isExpertCustom;
  final String? replacesText;
  final String? doctorNote;
  final String? sourceUrl;
  final String? supportFunction;

  const ChecklistItemShareData({
    required this.text,
    this.completed = false,
    this.category,
    this.timeLabel,
    this.origin,
    this.createdBy,
    this.isExpertCustom = false,
    this.replacesText,
    this.doctorNote,
    this.sourceUrl,
    this.supportFunction,
  });

  factory ChecklistItemShareData.fromJson(Map<String, dynamic> json) =>
      ChecklistItemShareData(
        text: json['text'] as String? ?? '',
        completed: json['completed'] as bool? ?? false,
        category: json['category'] as String?,
        timeLabel: json['timeLabel'] as String?,
        origin: json['origin'] as String?,
        createdBy: json['createdBy'] as String?,
        isExpertCustom: json['isExpertCustom'] as bool? ??
            (json['origin'] == 'EXPERT' || json['createdBy'] == 'EXPERT'),
        replacesText: json['replacesText'] as String?,
        doctorNote: json['doctorNote'] as String?,
        sourceUrl: json['sourceUrl'] as String?,
        supportFunction: json['supportFunction'] as String?,
      );

  Map<String, dynamic> toJson({bool compact = false}) => {
    'text': text,
    'completed': completed,
    if (!compact && category != null) 'category': category,
    if (!compact && timeLabel != null) 'timeLabel': timeLabel,
    if (origin != null && origin != 'SYSTEM') 'origin': origin,
    if (createdBy != null && createdBy != 'SYSTEM') 'createdBy': createdBy,
    if (isExpertCustom) 'isExpertCustom': isExpertCustom,
    if (replacesText != null) 'replacesText': replacesText,
    if (doctorNote != null) 'doctorNote': doctorNote,
    if (sourceUrl != null) 'sourceUrl': sourceUrl,
    if (supportFunction != null) 'supportFunction': supportFunction,
  };

  bool get isPersonal =>
      origin == 'USER' ||
      origin == 'USER_CREATED' ||
      createdBy == 'USER' ||
      createdBy == 'USER_CREATED';
  bool get isCareBridgeSuggestion => !isPersonal;
}

class ChecklistShareData {
  final String title;
  final int? gestationalWeek;
  final String? stage;
  final String? stageLabel;
  final String? journeyId;
  final bool isLiveSync;
  final int completedCount;
  final int totalCount;
  final int progressPercent;
  final String? note;
  final List<ChecklistItemShareData> historyItems;
  final List<ChecklistItemShareData> currentItems;
  final List<ChecklistItemShareData> futureItems;
  final List<String> removedItems;

  ChecklistShareData({
    this.title = 'Hồ sơ Checklist Toàn diện (Lịch sử & Tương lai)',
    this.gestationalWeek,
    this.stage,
    this.stageLabel,
    this.journeyId,
    this.isLiveSync = true,
    required this.completedCount,
    required this.totalCount,
    required this.progressPercent,
    this.note,
    this.historyItems = const [],
    List<ChecklistItemShareData> currentItems = const [],
    this.futureItems = const [],
    this.removedItems = const [],
    List<ChecklistItemShareData>? items,
  }) : currentItems = (items != null && items.isNotEmpty && currentItems.isEmpty)
            ? items
            : currentItems;

  List<ChecklistItemShareData> get allItems => [
    ...historyItems,
    ...currentItems,
    ...futureItems,
  ];

  static const String tag = '[CAREBRIDGE_CHECKLIST_SHARE]';

  static bool isChecklistShareMessage(String? body) {
    if (body == null) return false;
    return body.trim().startsWith(tag);
  }

  static ChecklistShareData? parse(String? body) {
    if (body == null || !isChecklistShareMessage(body)) return null;
    try {
      final jsonStr = body.replaceFirst(tag, '').trim();
      final decoded = jsonDecode(jsonStr) as Map<String, dynamic>;

      final removedList = (decoded['removedItems'] as List? ?? [])
          .map((item) => item.toString())
          .toList();
      final removedSet = removedList.map((r) => r.trim().toLowerCase()).toSet();

      bool isNotPersonal(ChecklistItemShareData item) =>
          item.origin != 'USER' &&
          item.origin != 'USER_CREATED' &&
          item.createdBy != 'USER' &&
          item.createdBy != 'USER_CREATED' &&
          !removedSet.contains(item.text.trim().toLowerCase());

      final historyList = (decoded['historyItems'] as List? ?? [])
          .map((item) => ChecklistItemShareData.fromJson(item as Map<String, dynamic>))
          .where(isNotPersonal)
          .toList();

      final currentList = (decoded['currentItems'] as List? ?? decoded['items'] as List? ?? [])
          .map((item) => ChecklistItemShareData.fromJson(item as Map<String, dynamic>))
          .where(isNotPersonal)
          .toList();

      final futureList = (decoded['futureItems'] as List? ?? [])
          .map((item) => ChecklistItemShareData.fromJson(item as Map<String, dynamic>))
          .where(isNotPersonal)
          .toList();

      final total = historyList.length + currentList.length + futureList.length;
      final completed = historyList.where((i) => i.completed).length +
          currentList.where((i) => i.completed).length +
          futureList.where((i) => i.completed).length;
      final percent = total > 0 ? ((completed / total) * 100).round() : 0;

      final rawStage = decoded['stage'] as String?;
      final rawWeek = (decoded['gestationalWeek'] as num?)?.toInt();
      final inferredStage = rawStage ?? (rawWeek != null ? 'PREGNANCY' : 'PRE_PREGNANCY');
      final inferredStageLabel = decoded['stageLabel'] as String? ??
          (inferredStage == 'PRE_PREGNANCY'
              ? 'Chuẩn bị mang thai'
              : inferredStage == 'POSTPARTUM'
                  ? 'Sau sinh'
                  : inferredStage == 'BABY_CARE'
                      ? 'Chăm sóc bé'
                      : rawWeek != null
                          ? 'Tuần thai thứ $rawWeek'
                          : 'Chuẩn bị mang thai');

      return ChecklistShareData(
        title: decoded['title'] as String? ?? 'Hồ sơ Checklist Toàn diện',
        gestationalWeek: rawWeek,
        stage: inferredStage,
        stageLabel: inferredStageLabel,
        journeyId: decoded['journeyId'] as String?,
        isLiveSync: decoded['isLiveSync'] as bool? ?? true,
        completedCount: (decoded['completedCount'] as num?)?.toInt() ?? completed,
        totalCount: (decoded['totalCount'] as num?)?.toInt() ?? total,
        progressPercent: (decoded['progressPercent'] as num?)?.toInt() ?? percent,
        note: decoded['note'] as String?,
        historyItems: historyList,
        currentItems: currentList,
        futureItems: futureList,
        removedItems: removedList,
      );
    } catch (_) {
      return null;
    }
  }

  String serialize() {
    Map<String, dynamic> payload(bool compact, [int? maxItems]) {
      var h = historyItems;
      var c = currentItems;
      var f = futureItems;
      if (maxItems != null) {
        c = c.take(maxItems).toList();
        final rem = maxItems - c.length;
        if (rem > 0) {
          h = h.take(rem ~/ 2).toList();
          f = f.take(rem - h.length).toList();
        } else {
          h = const [];
          f = const [];
        }
      }
      return {
        'title': title,
        if (gestationalWeek != null) 'gestationalWeek': gestationalWeek,
        if (stage != null) 'stage': stage,
        if (stageLabel != null) 'stageLabel': stageLabel,
        'journeyId': journeyId,
        'isLiveSync': isLiveSync,
        'completedCount': completedCount,
        'totalCount': totalCount,
        'progressPercent': progressPercent,
        'note': note,
        if (removedItems.isNotEmpty) 'removedItems': removedItems,
        'historyItems': h.map((i) => i.toJson(compact: compact)).toList(),
        'currentItems': c.map((i) => i.toJson(compact: compact)).toList(),
        'futureItems': f.map((i) => i.toJson(compact: compact)).toList(),
      };
    }

    String encoded = '$tag\n${jsonEncode(payload(false))}';
    if (encoded.length <= 1950) return encoded;

    encoded = '$tag\n${jsonEncode(payload(true))}';
    if (encoded.length <= 1950) return encoded;

    for (int max = 25; max >= 5; max -= 5) {
      encoded = '$tag\n${jsonEncode(payload(true, max))}';
      if (encoded.length <= 1950) return encoded;
    }

    return encoded;
  }
}
