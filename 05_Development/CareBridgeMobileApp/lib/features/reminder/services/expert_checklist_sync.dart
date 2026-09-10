import 'package:uuid/uuid.dart';

import '../../directChat/services/direct_chat_service.dart';
import '../../directChat/widgets/checklist_message_card.dart';

class ExpertChecklistSync {
  /// Toggles completion status of an expert task represented by [taskId]
  /// (format: `expert-task-{conversationId}-{itemIndex}`).
  ///
  /// Fetches the latest [ChecklistShareData] message in the conversation, updates the
  /// item's `completed` flag, recalculates progress statistics, and sends the updated
  /// payload back via [DirectChatService].
  static Future<bool> toggleTaskStatus({
    required String taskId,
    required bool completed,
    DirectChatService? directChatService,
    String? taskTitle,
  }) async {
    final chatService = directChatService ?? DirectChatService.instance;
    final match = RegExp(r'^expert-task-(.+)-(\d+)$').firstMatch(taskId);
    if (match == null) return false;

    final conversationId = match.group(1)!;
    final itemIndex = int.tryParse(match.group(2)!);
    if (itemIndex == null) return false;

    final timeline = await chatService.getTimeline(conversationId, limit: 50);
    ChecklistShareData? latestShare;
    for (final item in timeline.items.reversed) {
      if (item.kind == 'MESSAGE' &&
          item.recalledAt == null &&
          ChecklistShareData.isChecklistShareMessage(item.messageBody)) {
        final parsed = ChecklistShareData.parse(item.messageBody);
        if (parsed != null) {
          latestShare = parsed;
          break;
        }
      }
    }

    if (latestShare == null) return false;

    // Resolve target index in currentItems: either by itemIndex or matching text
    int targetIdx = itemIndex;
    if (targetIdx < 0 || targetIdx >= latestShare.currentItems.length) {
      if (taskTitle != null && taskTitle.trim().isNotEmpty) {
        final norm = taskTitle.trim().toLowerCase();
        targetIdx = latestShare.currentItems.indexWhere(
          (i) => i.text.trim().toLowerCase() == norm,
        );
      }
    }

    if (targetIdx < 0 || targetIdx >= latestShare.currentItems.length) {
      return false;
    }

    final oldItem = latestShare.currentItems[targetIdx];
    final updatedItem = ChecklistItemShareData(
      text: oldItem.text,
      completed: completed,
      category: oldItem.category,
      timeLabel: oldItem.timeLabel,
      origin: oldItem.origin,
      createdBy: oldItem.createdBy,
      isExpertCustom: oldItem.isExpertCustom,
      replacesText: oldItem.replacesText,
      doctorNote: oldItem.doctorNote,
      sourceUrl: oldItem.sourceUrl,
      supportFunction: oldItem.supportFunction,
    );

    final newCurrentItems = List<ChecklistItemShareData>.from(
      latestShare.currentItems,
    );
    newCurrentItems[targetIdx] = updatedItem;

    final newTotal =
        latestShare.historyItems.length +
        newCurrentItems.length +
        latestShare.futureItems.length;
    final newCompleted =
        latestShare.historyItems.where((i) => i.completed).length +
        newCurrentItems.where((i) => i.completed).length;
    final newPercent =
        newTotal > 0 ? ((newCompleted / newTotal) * 100).round() : 0;

    final updatedPayload = ChecklistShareData(
      title: latestShare.title,
      gestationalWeek: latestShare.gestationalWeek,
      journeyId: latestShare.journeyId,
      isLiveSync: true,
      completedCount: newCompleted,
      totalCount: newTotal,
      progressPercent: newPercent,
      note: latestShare.note,
      historyItems: latestShare.historyItems,
      currentItems: newCurrentItems,
      futureItems: latestShare.futureItems,
      removedItems: latestShare.removedItems,
    );

    await chatService.sendMessage(
      conversationId,
      clientMessageId: const Uuid().v4(),
      messageBody: updatedPayload.serialize(),
    );

    return true;
  }
}
