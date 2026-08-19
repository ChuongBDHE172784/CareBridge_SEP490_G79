import 'dart:convert';
import 'package:flutter/material.dart';

class ChecklistItemShareData {
  final String text;
  final bool completed;
  final String? category;

  const ChecklistItemShareData({
    required this.text,
    this.completed = false,
    this.category,
  });

  factory ChecklistItemShareData.fromJson(Map<String, dynamic> json) =>
      ChecklistItemShareData(
        text: json['text'] as String? ?? '',
        completed: json['completed'] as bool? ?? false,
        category: json['category'] as String?,
      );

  Map<String, dynamic> toJson() => {
    'text': text,
    'completed': completed,
    'category': category,
  };
}

class ChecklistShareData {
  final String title;
  final int? gestationalWeek;
  final int completedCount;
  final int totalCount;
  final int progressPercent;
  final String? note;
  final List<ChecklistItemShareData> items;

  const ChecklistShareData({
    this.title = 'Danh sách việc cần làm (Checklist)',
    this.gestationalWeek,
    required this.completedCount,
    required this.totalCount,
    required this.progressPercent,
    this.note,
    required this.items,
  });

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
      final itemsList = (decoded['items'] as List? ?? [])
          .map((item) => ChecklistItemShareData.fromJson(item as Map<String, dynamic>))
          .toList();
      return ChecklistShareData(
        title: decoded['title'] as String? ?? 'Danh sách việc cần làm (Checklist)',
        gestationalWeek: (decoded['gestationalWeek'] as num?)?.toInt(),
        completedCount: (decoded['completedCount'] as num?)?.toInt() ?? 0,
        totalCount: (decoded['totalCount'] as num?)?.toInt() ?? itemsList.length,
        progressPercent: (decoded['progressPercent'] as num?)?.toInt() ?? 0,
        note: decoded['note'] as String?,
        items: itemsList,
      );
    } catch (_) {
      return null;
    }
  }

  String serialize() => '$tag\n${jsonEncode({
    'title': title,
    'gestationalWeek': gestationalWeek,
    'completedCount': completedCount,
    'totalCount': totalCount,
    'progressPercent': progressPercent,
    'note': note,
    'items': items.map((i) => i.toJson()).toList(),
  })}';
}

class ChecklistMessageCard extends StatelessWidget {
  const ChecklistMessageCard({
    super.key,
    required this.data,
    required this.isOwnMessage,
  });

  final ChecklistShareData data;
  final bool isOwnMessage;

  @override
  Widget build(BuildContext context) {
    const primary = Color(0xFFC98C7B);
    const surface = Colors.white;
    const textDark = Color(0xFF2C2523);
    const textMuted = Color(0xFF7A6F6C);

    return Container(
      width: 290,
      decoration: BoxDecoration(
        color: surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: isOwnMessage ? Colors.white70 : const Color(0xFFE8D5CE),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        mainAxisSize: MainAxisSize.min,
        children: [
          // Header
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
            decoration: BoxDecoration(
              color: primary.withValues(alpha: 0.1),
              borderRadius: const BorderRadius.vertical(top: Radius.circular(15)),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(6),
                      decoration: BoxDecoration(
                        color: primary,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: const Icon(
                        Icons.checklist_rtl_rounded,
                        color: Colors.white,
                        size: 18,
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            data.title,
                            style: const TextStyle(
                              fontFamily: 'Lexend',
                              fontWeight: FontWeight.w700,
                              fontSize: 13,
                              color: textDark,
                            ),
                          ),
                          if (data.gestationalWeek != null)
                            Text(
                              'Giai đoạn / Tuần thai: ${data.gestationalWeek}',
                              style: const TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 11,
                                color: textMuted,
                              ),
                            ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                // Progress bar
                Row(
                  children: [
                    Expanded(
                      child: ClipRRect(
                        borderRadius: BorderRadius.circular(4),
                        child: LinearProgressIndicator(
                          value: data.totalCount > 0
                              ? data.completedCount / data.totalCount
                              : 0.0,
                          backgroundColor: const Color(0xFFE8D5CE),
                          valueColor: const AlwaysStoppedAnimation<Color>(
                            Color(0xFF2E7D32),
                          ),
                          minHeight: 6,
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      '${data.completedCount}/${data.totalCount} (${data.progressPercent}%)',
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 11,
                        fontWeight: FontWeight.w700,
                        color: Color(0xFF2E7D32),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),

          // Items list
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
            child: Column(
              children: data.items.map((item) {
                return Padding(
                  padding: const EdgeInsets.symmetric(vertical: 4),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Icon(
                        item.completed
                            ? Icons.check_circle_rounded
                            : Icons.radio_button_unchecked_rounded,
                        size: 16,
                        color: item.completed
                            ? const Color(0xFF2E7D32)
                            : const Color(0xFFB0A4A1),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          item.text,
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 12,
                            color: item.completed
                                ? textDark
                                : const Color(0xFF6E605D),
                            decoration: item.completed
                                ? null
                                : null,
                          ),
                        ),
                      ),
                    ],
                  ),
                );
              }).toList(),
            ),
          ),

          // Note if present
          if (data.note != null && data.note!.trim().isNotEmpty)
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              margin: const EdgeInsets.only(left: 10, right: 10, bottom: 10),
              decoration: BoxDecoration(
                color: const Color(0xFFF9F5F4),
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: const Color(0xFFECE4E1)),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(
                    Icons.chat_bubble_outline_rounded,
                    size: 14,
                    color: textMuted,
                  ),
                  const SizedBox(width: 6),
                  Expanded(
                    child: Text(
                      data.note!,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        color: textDark,
                        fontStyle: FontStyle.italic,
                      ),
                    ),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }
}
