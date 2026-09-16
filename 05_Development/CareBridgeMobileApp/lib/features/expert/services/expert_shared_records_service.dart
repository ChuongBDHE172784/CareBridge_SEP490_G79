import 'package:uuid/uuid.dart';

import '../../../core/network/api_client.dart';
import '../../directChat/models/direct_conversation.dart';
import '../../directChat/models/timeline_page.dart';
import '../../directChat/services/direct_chat_service.dart';
import '../../directChat/models/checklist_share_data.dart';
import '../../directChat/models/health_metrics_share_data.dart';
import '../models/expert_shared_record_model.dart';

abstract class ExpertSharedRecordsApi {
  Future<List<DirectConversationSummary>> listConversations();
  Future<TimelinePage> getTimeline(String conversationId, {int limit});
  Future<dynamic> sendMessage(
    String conversationId, {
    required String clientMessageId,
    required String messageBody,
    String messageType,
  });
  Future<dynamic> get(String path);
}

class _DefaultExpertSharedRecordsApi implements ExpertSharedRecordsApi {
  @override
  Future<List<DirectConversationSummary>> listConversations() =>
      DirectChatService.instance.listMyConversations();

  @override
  Future<TimelinePage> getTimeline(String conversationId, {int limit = 50}) =>
      DirectChatService.instance.getTimeline(conversationId, limit: limit);

  @override
  Future<dynamic> sendMessage(
    String conversationId, {
    required String clientMessageId,
    required String messageBody,
    String messageType = 'TEXT',
  }) => DirectChatService.instance.sendMessage(
    conversationId,
    clientMessageId: clientMessageId,
    messageBody: messageBody,
    messageType: messageType,
  );

  @override
  Future<dynamic> get(String path) => apiGet(path);
}

class ExpertSharedRecordsService {
  static ExpertSharedRecordsService instance = ExpertSharedRecordsService();

  final ExpertSharedRecordsApi api;
  final Uuid _uuid;

  ExpertSharedRecordsService({ExpertSharedRecordsApi? api, Uuid? uuid})
    : api = api ?? _DefaultExpertSharedRecordsApi(),
      _uuid = uuid ?? const Uuid();

  Future<List<SharedRecordEntry>> fetchExpertSharedRecords() async {
    try {
      final conversations = await api.listConversations();
      final records = <SharedRecordEntry>[];

      final results = await Future.wait(
        conversations.map((c) async {
          try {
            final timeline = await api.getTimeline(c.conversationId, limit: 50);
            return {'conversation': c, 'timeline': timeline};
          } catch (_) {
            return null;
          }
        }),
      );

      for (final res in results) {
        if (res == null) continue;
        final conversation = res['conversation'] as DirectConversationSummary;
        final timeline = res['timeline'] as TimelinePage;

        final counterpartId = conversation.counterpartUserId;
        final motherDisplayName =
            conversation.counterpartDisplayName ??
            'Mẹ bầu (${counterpartId.length >= 8 ? counterpartId.substring(0, 8) : counterpartId})';

        for (final item in timeline.items) {
          if (item.kind != 'MESSAGE' ||
              item.recalledAt != null ||
              item.messageBody == null) {
            continue;
          }

          final rawHealth = HealthMetricsShareData.parse(item.messageBody);
          if (rawHealth != null) {
            final healthData = await syncLiveHealthMetrics(rawHealth);
            var alertLevel = SharedRecordAlertLevel.normal;
            for (final m in healthData.metrics) {
              if (m.status.toUpperCase() == 'CRITICAL') {
                alertLevel = SharedRecordAlertLevel.critical;
                break;
              }
              if (m.status.toUpperCase() == 'WARNING') {
                alertLevel = SharedRecordAlertLevel.warning;
              }
            }

            records.add(
              SharedRecordEntry(
                id: item.messageId ?? item.clientMessageId ?? 'rec-${DateTime.now().millisecondsSinceEpoch}',
                conversationId: conversation.conversationId,
                conversationStatus: conversation.conversationStatus,
                motherUserId: counterpartId,
                motherName: motherDisplayName,
                motherAvatar: conversation.counterpartAvatarUrl,
                createdAt: item.createdAt ?? DateTime.now(),
                type: SharedRecordType.healthMetrics,
                healthData: healthData,
                alertLevel: alertLevel,
                status: 'PENDING_REVIEW',
              ),
            );
            continue;
          }

          final rawChecklist = ChecklistShareData.parse(item.messageBody);
          if (rawChecklist != null) {
            final checklistData = await syncLiveChecklist(rawChecklist, counterpartId);
            final alertLevel =
                checklistData.progressPercent < 50
                    ? SharedRecordAlertLevel.warning
                    : SharedRecordAlertLevel.normal;

            records.add(
              SharedRecordEntry(
                id: item.messageId ?? item.clientMessageId ?? 'rec-${DateTime.now().millisecondsSinceEpoch}',
                conversationId: conversation.conversationId,
                conversationStatus: conversation.conversationStatus,
                motherUserId: counterpartId,
                motherName: motherDisplayName,
                motherAvatar: conversation.counterpartAvatarUrl,
                createdAt: item.createdAt ?? DateTime.now(),
                type: SharedRecordType.checklist,
                checklistData: checklistData,
                alertLevel: alertLevel,
                status: 'PENDING_REVIEW',
              ),
            );
          }
        }
      }

      records.sort((a, b) => b.createdAt.compareTo(a.createdAt));
      return records;
    } catch (_) {
      return [];
    }
  }

  static String evaluatePointStatus(String code, Map<String, dynamic> point) {
    final num? val = point['valueNumeric'] as num?;
    final num? sec = point['valueSecondary'] as num?;
    if (val == null) return (point['status'] as String? ?? 'NORMAL').toUpperCase();

    if (code == 'BLOOD_PRESSURE') {
      final sys = val;
      final dia = sec ?? 80;
      if (sys >= 140 || dia >= 90) return 'CRITICAL';
      if (sys >= 130 || dia >= 85 || sys < 90 || dia < 60) return 'WARNING';
      return 'NORMAL';
    }
    if (code == 'BLOOD_GLUCOSE') {
      if (val > 140) return 'CRITICAL';
      if (val > 95 || val < 60) return 'WARNING';
      return 'NORMAL';
    }
    if (code == 'HEART_RATE') {
      if (val > 120 || val < 50) return 'CRITICAL';
      if (val > 100 || val < 60) return 'WARNING';
      return 'NORMAL';
    }
    if (code == 'TEMPERATURE') {
      if (val >= 38.5) return 'CRITICAL';
      if (val >= 37.5 || val < 35.5) return 'WARNING';
      return 'NORMAL';
    }
    if (code == 'FETAL_MOVEMENT_SESSION') {
      if (val < 4) return 'CRITICAL';
      if (val < 10) return 'WARNING';
      return 'NORMAL';
    }
    if (code == 'SPO2') {
      if (val < 92) return 'CRITICAL';
      if (val < 95) return 'WARNING';
      return 'NORMAL';
    }
    return (point['status'] as String? ?? 'NORMAL').toUpperCase();
  }

  Future<HealthMetricsShareData> syncLiveHealthMetrics(
    HealthMetricsShareData healthData,
  ) async {
    if (healthData.journeyId == null || !healthData.isLiveSync) {
      return healthData;
    }

    try {
      final updatedMetrics = await Future.wait(
        healthData.metrics.map((metric) async {
          try {
            final res = await api.get(
              '/api/v1/journeys/${healthData.journeyId}/metrics?metricType=${metric.code}',
            );
            final dataPoints =
                (res['data']?['dataPoints'] as List?)
                    ?.cast<Map<String, dynamic>>() ??
                [];
            if (dataPoints.isEmpty) return metric;

            dataPoints.sort((a, b) {
              final da = DateTime.tryParse(a['measuredAt'] ?? '') ?? DateTime(0);
              final db = DateTime.tryParse(b['measuredAt'] ?? '') ?? DateTime(0);
              return db.compareTo(da);
            });

            final unit = res['data']?['unit'] as String? ?? metric.unit;
            final history = dataPoints.map((p) {
              final rawDt = DateTime.tryParse(p['measuredAt'] ?? '');
              final timeStr = rawDt != null
                  ? '${rawDt.day.toString().padLeft(2, '0')}/${rawDt.month.toString().padLeft(2, '0')} ${rawDt.hour.toString().padLeft(2, '0')}:${rawDt.minute.toString().padLeft(2, '0')}'
                  : (p['measuredAt']?.toString() ?? '');
              final valStr = p['valueSecondary'] != null
                  ? '${p['valueNumeric']}/${p['valueSecondary']}'
                  : '${p['valueNumeric'] ?? ''}';
              return HealthMetricMeasurementRecord(
                measuredAt: timeStr,
                value: valStr,
                unit: unit,
                status: evaluatePointStatus(metric.code, p),
                note: p['note'] as String?,
              );
            }).toList();

            final latest = dataPoints.first;
            final latestVal =
                latest['valueSecondary'] != null
                    ? '${latest['valueNumeric']}/${latest['valueSecondary']}'
                    : '${latest['valueNumeric']}';
            final latestDt = DateTime.tryParse(latest['measuredAt'] ?? '');
            final latestTime = latestDt != null
                ? '${latestDt.day.toString().padLeft(2, '0')}/${latestDt.month.toString().padLeft(2, '0')} ${latestDt.hour.toString().padLeft(2, '0')}:${latestDt.minute.toString().padLeft(2, '0')}'
                : metric.measuredTime;

            return HealthMetricItemData(
              code: metric.code,
              name: metric.name,
              value: latestVal,
              unit: unit,
              status: history.isNotEmpty ? history.first.status : metric.status,
              icon: metric.icon,
              measuredTime: latestTime,
              history: history,
            );
          } catch (_) {
            return metric;
          }
        }),
      );

      return HealthMetricsShareData(
        title: healthData.title,
        gestationalWeek: healthData.gestationalWeek,
        measuredDate: healthData.measuredDate,
        timeRangeLabel: healthData.timeRangeLabel,
        journeyId: healthData.journeyId,
        isLiveSync: healthData.isLiveSync,
        note: healthData.note,
        metrics: updatedMetrics,
      );
    } catch (_) {
      return healthData;
    }
  }

  Future<ChecklistShareData> syncLiveChecklist(
    ChecklistShareData checklistData,
    String? motherUserId,
  ) async {
    try {
      dynamic res;
      if (checklistData.journeyId != null && checklistData.journeyId!.isNotEmpty) {
        res = await api.get(
          '/api/v1/checklists/journeys/${checklistData.journeyId}/tasks',
        );
      } else if (motherUserId != null && motherUserId.isNotEmpty) {
        res = await api.get('/api/v1/checklists/users/$motherUserId/tasks');
      }

      final sections = res?['data']?['sections'] as Map<String, dynamic>?;
      if (sections != null) {
        final allTasks = [
          ...(sections['overdue'] as List? ?? []),
          ...(sections['today'] as List? ?? []),
          ...(sections['upcoming'] as List? ?? []),
          ...(sections['unscheduled'] as List? ?? []),
        ].cast<Map<String, dynamic>>();

        if (allTasks.isNotEmpty) {
          final taskStatusMap = <String, bool>{};
          for (final t in allTasks) {
            final title = (t['title'] as String? ?? '').trim().toLowerCase();
            final status = (t['taskStatus'] ?? t['status'] ?? '').toString();
            final isDone = status == 'COMPLETED' || status == 'DONE';
            taskStatusMap[title] = isDone;
          }

          final updatedCurrent = checklistData.currentItems.map((item) {
            final key = item.text.trim().toLowerCase();
            return ChecklistItemShareData(
              text: item.text,
              completed: taskStatusMap.containsKey(key) ? taskStatusMap[key]! : item.completed,
              category: item.category,
              timeLabel: item.timeLabel,
              origin: item.origin,
              createdBy: item.createdBy,
              isExpertCustom: item.isExpertCustom,
              replacesText: item.replacesText,
              doctorNote: item.doctorNote,
              sourceUrl: item.sourceUrl,
              supportFunction: item.supportFunction,
            );
          }).toList();

          final allItems = [
            ...checklistData.historyItems,
            ...updatedCurrent,
            ...checklistData.futureItems,
          ];
          final completedCount = allItems.where((i) => i.completed).length;
          final totalCount = allItems.length;
          final progressPercent =
              totalCount > 0 ? ((completedCount / totalCount) * 100).round() : 0;

          return ChecklistShareData(
            title: checklistData.title,
            gestationalWeek: checklistData.gestationalWeek,
            stage: checklistData.stage,
            stageLabel: checklistData.stageLabel,
            journeyId: checklistData.journeyId,
            isLiveSync: true,
            completedCount: completedCount,
            totalCount: totalCount,
            progressPercent: progressPercent,
            note: checklistData.note,
            historyItems: checklistData.historyItems,
            currentItems: updatedCurrent,
            futureItems: checklistData.futureItems,
            removedItems: checklistData.removedItems,
          );
        }
      }
    } catch (_) {}
    return checklistData;
  }

  Future<ChecklistShareData> savePersonalizedChecklist(
    String conversationId,
    ChecklistShareData updatedChecklist, {
    String? doctorActionNote,
  }) async {
    final removedSet = updatedChecklist.removedItems
        .map((r) => r.trim().toLowerCase())
        .toSet();

    ChecklistItemShareData normalizeItem(ChecklistItemShareData item) {
      final isExp =
          item.isExpertCustom || item.origin == 'EXPERT' || item.createdBy == 'EXPERT';
      return ChecklistItemShareData(
        text: item.text.trim(),
        completed: item.completed,
        category: item.category,
        timeLabel: item.timeLabel,
        origin: isExp ? 'EXPERT' : (item.origin ?? 'SYSTEM'),
        createdBy: isExp ? 'EXPERT' : (item.createdBy ?? 'SYSTEM'),
        isExpertCustom: isExp,
        replacesText: item.replacesText,
        doctorNote: item.doctorNote,
        sourceUrl: item.sourceUrl,
        supportFunction: item.supportFunction,
      );
    }

    var historyList = updatedChecklist.historyItems.map(normalizeItem).toList();
    var currentList = updatedChecklist.currentItems.map(normalizeItem).toList();
    var futureList = updatedChecklist.futureItems.map(normalizeItem).toList();

    if (removedSet.isNotEmpty) {
      historyList = historyList
          .where((h) => !removedSet.contains(h.text.toLowerCase()))
          .toList();
      currentList = currentList
          .where((c) => !removedSet.contains(c.text.toLowerCase()))
          .toList();
      futureList = futureList
          .where((f) => !removedSet.contains(f.text.toLowerCase()))
          .toList();
    }

    // Deduplicate within history
    final seenHistory = <String>{};
    historyList = historyList
        .where((h) => seenHistory.add(h.text.toLowerCase()))
        .toList();

    // Deduplicate within current & eliminate history overlap
    final seenCurrent = <String>{};
    currentList = currentList
        .where((c) =>
            !seenHistory.contains(c.text.toLowerCase()) &&
            seenCurrent.add(c.text.toLowerCase()))
        .toList();

    // Deduplicate within future & eliminate current/history overlap
    final seenFuture = <String>{};
    futureList = futureList
        .where((f) =>
            !seenHistory.contains(f.text.toLowerCase()) &&
            !seenCurrent.contains(f.text.toLowerCase()) &&
            seenFuture.add(f.text.toLowerCase()))
        .toList();

    final allItems = [...currentList, ...historyList, ...futureList];
    final completedCount = allItems.where((i) => i.completed).length;
    final totalCount = allItems.length;
    final progressPercent =
        totalCount > 0 ? ((completedCount / totalCount) * 100).round() : 0;

    final payload = ChecklistShareData(
      title: updatedChecklist.title,
      gestationalWeek: updatedChecklist.gestationalWeek,
      stage: updatedChecklist.stage,
      stageLabel: updatedChecklist.stageLabel,
      journeyId: updatedChecklist.journeyId,
      isLiveSync: true,
      completedCount: completedCount,
      totalCount: totalCount,
      progressPercent: progressPercent,
      note: doctorActionNote ?? updatedChecklist.note,
      historyItems: historyList,
      currentItems: currentList,
      futureItems: futureList,
      removedItems: updatedChecklist.removedItems,
    );

    final messageBody = payload.serialize();
    final clientMessageId = _uuid.v4();

    await api.sendMessage(
      conversationId,
      clientMessageId: clientMessageId,
      messageBody: messageBody,
      messageType: 'TEXT',
    );

    return payload;
  }

  Future<ChecklistShareData> addChecklistItemToSharedRecord(
    String conversationId,
    ChecklistShareData currentChecklist,
    ChecklistItemShareData newItem,
    String targetGroup, {
    String? doctorNote,
  }) async {
    final itemToSave = ChecklistItemShareData(
      text: newItem.text.trim(),
      completed: newItem.completed,
      category: newItem.category ?? 'Khám thai & Y tế',
      timeLabel: newItem.timeLabel,
      origin: 'EXPERT',
      createdBy: 'EXPERT',
      isExpertCustom: true,
      replacesText: newItem.replacesText,
      doctorNote: doctorNote ?? newItem.doctorNote,
      sourceUrl: newItem.sourceUrl,
      supportFunction: newItem.supportFunction,
    );

    final removedItems = currentChecklist.removedItems
        .where((r) => r.trim().toLowerCase() != itemToSave.text.toLowerCase())
        .toList();

    final currentItems = List<ChecklistItemShareData>.from(currentChecklist.currentItems);
    final historyItems = List<ChecklistItemShareData>.from(currentChecklist.historyItems);
    final futureItems = List<ChecklistItemShareData>.from(currentChecklist.futureItems);

    if (targetGroup == 'CURRENT') {
      currentItems.add(itemToSave);
    } else if (targetGroup == 'FUTURE') {
      futureItems.add(itemToSave);
    } else {
      historyItems.add(itemToSave);
    }

    final updated = ChecklistShareData(
      title: currentChecklist.title,
      gestationalWeek: currentChecklist.gestationalWeek,
      stage: currentChecklist.stage,
      stageLabel: currentChecklist.stageLabel,
      journeyId: currentChecklist.journeyId,
      isLiveSync: currentChecklist.isLiveSync,
      completedCount: currentChecklist.completedCount,
      totalCount: currentChecklist.totalCount + 1,
      progressPercent: currentChecklist.progressPercent,
      note: currentChecklist.note,
      historyItems: historyItems,
      currentItems: currentItems,
      futureItems: futureItems,
      removedItems: removedItems,
    );

    return savePersonalizedChecklist(conversationId, updated, doctorActionNote: doctorNote);
  }

  Future<ChecklistShareData> editChecklistItemInSharedRecord(
    String conversationId,
    ChecklistShareData currentChecklist,
    String targetGroup,
    int itemIndex,
    ChecklistItemShareData updatedItem, {
    String? doctorNote,
    String? originalItemText,
  }) async {
    final removedItems = List<String>.from(currentChecklist.removedItems);
    final currentItems = List<ChecklistItemShareData>.from(currentChecklist.currentItems);
    final historyItems = List<ChecklistItemShareData>.from(currentChecklist.historyItems);
    final futureItems = List<ChecklistItemShareData>.from(currentChecklist.futureItems);

    final targetList = targetGroup == 'CURRENT'
        ? currentItems
        : targetGroup == 'FUTURE'
            ? futureItems
            : historyItems;

    var targetIdx = itemIndex;
    if (originalItemText != null) {
      final found = targetList.indexWhere(
        (i) => i.text.trim().toLowerCase() == originalItemText.trim().toLowerCase(),
      );
      if (found >= 0) targetIdx = found;
    }

    final existingItem =
        targetIdx >= 0 && targetIdx < targetList.length ? targetList[targetIdx] : null;
    final originalText = originalItemText ?? existingItem?.text;
    final isRenamed = originalText != null &&
        originalText.trim().toLowerCase() != updatedItem.text.trim().toLowerCase();

    if (isRenamed && !removedItems.contains(originalText.trim())) {
      removedItems.add(originalText.trim());
    }

    final replacesText = updatedItem.replacesText ??
        (isRenamed ? originalText.trim() : existingItem?.replacesText);

    final itemToSave = ChecklistItemShareData(
      text: updatedItem.text.trim(),
      completed: updatedItem.completed,
      category: updatedItem.category ?? 'Khám thai & Y tế',
      timeLabel: updatedItem.timeLabel ?? existingItem?.timeLabel,
      origin: 'EXPERT',
      createdBy: 'EXPERT',
      isExpertCustom: true,
      replacesText: replacesText,
      doctorNote: doctorNote ?? updatedItem.doctorNote ?? existingItem?.doctorNote,
      sourceUrl: updatedItem.sourceUrl ?? existingItem?.sourceUrl,
      supportFunction: updatedItem.supportFunction ?? existingItem?.supportFunction,
    );

    if (targetIdx >= 0 && targetIdx < targetList.length) {
      targetList[targetIdx] = itemToSave;
    } else {
      targetList.add(itemToSave);
    }

    final updated = ChecklistShareData(
      title: currentChecklist.title,
      gestationalWeek: currentChecklist.gestationalWeek,
      stage: currentChecklist.stage,
      stageLabel: currentChecklist.stageLabel,
      journeyId: currentChecklist.journeyId,
      isLiveSync: currentChecklist.isLiveSync,
      completedCount: currentChecklist.completedCount,
      totalCount: currentChecklist.totalCount,
      progressPercent: currentChecklist.progressPercent,
      note: currentChecklist.note,
      historyItems: historyItems,
      currentItems: currentItems,
      futureItems: futureItems,
      removedItems: removedItems,
    );

    return savePersonalizedChecklist(conversationId, updated, doctorActionNote: doctorNote);
  }

  Future<ChecklistShareData> deleteChecklistItemFromSharedRecord(
    String conversationId,
    ChecklistShareData currentChecklist,
    String targetGroup,
    int itemIndex, {
    String? doctorNote,
    String? itemText,
  }) async {
    final currentItems = List<ChecklistItemShareData>.from(currentChecklist.currentItems);
    final historyItems = List<ChecklistItemShareData>.from(currentChecklist.historyItems);
    final futureItems = List<ChecklistItemShareData>.from(currentChecklist.futureItems);

    final targetListOriginal = targetGroup == 'CURRENT'
        ? currentItems
        : targetGroup == 'FUTURE'
            ? futureItems
            : historyItems;

    final textToDelete = itemText ??
        (itemIndex >= 0 && itemIndex < targetListOriginal.length
            ? targetListOriginal[itemIndex].text
            : null);

    final removedItems = List<String>.from(currentChecklist.removedItems);
    if (textToDelete != null && !removedItems.contains(textToDelete.trim())) {
      removedItems.add(textToDelete.trim());
    }

    var targetIdx = itemIndex;
    if (textToDelete != null) {
      final found = targetListOriginal.indexWhere(
        (i) => i.text.trim().toLowerCase() == textToDelete.trim().toLowerCase(),
      );
      if (found >= 0) targetIdx = found;
    }

    if (targetIdx >= 0 && targetIdx < targetListOriginal.length) {
      targetListOriginal.removeAt(targetIdx);
    }

    final updated = ChecklistShareData(
      title: currentChecklist.title,
      gestationalWeek: currentChecklist.gestationalWeek,
      stage: currentChecklist.stage,
      stageLabel: currentChecklist.stageLabel,
      journeyId: currentChecklist.journeyId,
      isLiveSync: currentChecklist.isLiveSync,
      completedCount: currentChecklist.completedCount,
      totalCount: (currentChecklist.totalCount - 1).clamp(0, 9999),
      progressPercent: currentChecklist.progressPercent,
      note: currentChecklist.note,
      historyItems: historyItems,
      currentItems: currentItems,
      futureItems: futureItems,
      removedItems: removedItems,
    );

    return savePersonalizedChecklist(conversationId, updated, doctorActionNote: doctorNote);
  }
}
