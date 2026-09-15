import 'package:flutter_test/flutter_test.dart';
import 'package:uuid/uuid.dart';

import 'package:untitled/features/directChat/models/direct_conversation.dart';
import 'package:untitled/features/directChat/models/timeline_item.dart';
import 'package:untitled/features/directChat/models/timeline_page.dart';
import 'package:untitled/features/directChat/widgets/checklist_message_card.dart';
import 'package:untitled/features/directChat/widgets/health_metrics_message_card.dart';
import 'package:untitled/features/expert/models/expert_shared_record_model.dart';
import 'package:untitled/features/expert/services/expert_shared_records_service.dart';

class MockExpertSharedRecordsApi implements ExpertSharedRecordsApi {
  List<DirectConversationSummary> conversations = [];
  Map<String, TimelinePage> timelines = {};
  List<Map<String, dynamic>> sentMessages = [];
  Map<String, dynamic> apiGetResponses = {};

  @override
  Future<List<DirectConversationSummary>> listConversations() async => conversations;

  @override
  Future<TimelinePage> getTimeline(String conversationId, {int limit = 50}) async =>
      timelines[conversationId] ??
      const TimelinePage(items: [], nextCursor: null, previousCursor: null, hasMoreOlder: false, hasMoreNewer: false);

  @override
  Future<dynamic> sendMessage(
    String conversationId, {
    required String clientMessageId,
    required String messageBody,
    String messageType = 'TEXT',
  }) async {
    final msg = {
      'conversationId': conversationId,
      'clientMessageId': clientMessageId,
      'messageBody': messageBody,
      'messageType': messageType,
    };
    sentMessages.add(msg);
    return {'id': 'msg-123', ...msg};
  }

  @override
  Future<dynamic> get(String path) async {
    return apiGetResponses[path] ?? {};
  }
}

void main() {
  group('ExpertSharedRecordsService Tests', () {
    late MockExpertSharedRecordsApi mockApi;
    late ExpertSharedRecordsService service;

    setUp(() {
      mockApi = MockExpertSharedRecordsApi();
      service = ExpertSharedRecordsService(
        api: mockApi,
        uuid: const Uuid(),
      );
    });

    test('fetchExpertSharedRecords parses health and checklist shares from conversations', () async {
      mockApi.conversations = [
        DirectConversationSummary(
          conversationId: 'conv-1',
          counterpartUserId: 'user-mother-1',
          counterpartDisplayName: 'Nguyễn Thị Hoa',
          counterpartRole: 'MOTHER',
          lastMessagePreview: 'Chia sẻ checklist',
          lastMessageAt: DateTime(2026, 9, 15, 10, 0),
          expertAvailable: true,
          unreadCount: 0,
        ),
      ];

      final sampleChecklist = ChecklistShareData(
        title: 'Checklist Tuần 12',
        gestationalWeek: 12,
        stage: 'PREGNANCY',
        stageLabel: 'Mang thai',
        journeyId: 'journey-1',
        isLiveSync: true,
        completedCount: 1,
        totalCount: 2,
        progressPercent: 50,
        currentItems: const [
          ChecklistItemShareData(text: 'Uống vitamin tổng hợp', completed: true),
          ChecklistItemShareData(text: 'Khám thai định kỳ 12 tuần', completed: false),
        ],
      );

      final sampleHealth = HealthMetricsShareData(
        title: 'Chỉ số sức khỏe tuần 12',
        journeyId: 'journey-1',
        metrics: const [
          HealthMetricItemData(code: 'BLOOD_PRESSURE', name: 'Huyết áp', value: '118/75', unit: 'mmHg', status: 'NORMAL'),
        ],
      );

      mockApi.timelines['conv-1'] = TimelinePage(
        items: [
          TimelineItem(
            kind: 'MESSAGE',
            messageId: 'm1',
            clientMessageId: 'c1',
            senderUserId: 'user-mother-1',
            messageBody: sampleHealth.serialize(),
            createdAt: DateTime(2026, 9, 15, 9, 30),
          ),
          TimelineItem(
            kind: 'MESSAGE',
            messageId: 'm2',
            clientMessageId: 'c2',
            senderUserId: 'user-mother-1',
            messageBody: sampleChecklist.serialize(),
            createdAt: DateTime(2026, 9, 15, 10, 0),
          ),
        ],
        nextCursor: null,
        previousCursor: null,
        hasMoreOlder: false,
        hasMoreNewer: false,
      );

      final records = await service.fetchExpertSharedRecords();

      expect(records.length, 2);
      final healthRecord = records.firstWhere((r) => r.type == SharedRecordType.healthMetrics);
      final checklistRecord = records.firstWhere((r) => r.type == SharedRecordType.checklist);

      expect(healthRecord.conversationId, 'conv-1');
      expect(healthRecord.motherName, 'Nguyễn Thị Hoa');
      expect(healthRecord.healthData, isNotNull);
      expect(healthRecord.healthData!.metrics.length, 1);

      expect(checklistRecord.conversationId, 'conv-1');
      expect(checklistRecord.motherName, 'Nguyễn Thị Hoa');
      expect(checklistRecord.checklistData, isNotNull);
      expect(checklistRecord.checklistData!.title, 'Checklist Tuần 12');
      expect(checklistRecord.checklistData!.currentItems.length, 2);
    });

    test('addChecklistItemToSharedRecord adds new item and sends updated chat share message', () async {
      final initialChecklist = ChecklistShareData(
        title: 'Checklist Tuần 20',
        completedCount: 0,
        totalCount: 1,
        progressPercent: 0,
        currentItems: const [
          ChecklistItemShareData(text: 'Uống canxi', completed: false),
        ],
      );

      final newItem = const ChecklistItemShareData(
        text: 'Xét nghiệm dung nạp đường huyết (OGTT)',
        isExpertCustom: true,
        doctorNote: 'Làm buổi sáng khi đói',
      );

      final updatedChecklist = await service.addChecklistItemToSharedRecord(
        'conv-2',
        initialChecklist,
        newItem,
        'CURRENT',
      );

      expect(updatedChecklist.currentItems.length, 2);
      expect(updatedChecklist.currentItems.any((i) => i.text.contains('OGTT')), isTrue);
      expect(mockApi.sentMessages.length, 1);
      expect(mockApi.sentMessages.first['conversationId'], 'conv-2');
      expect(mockApi.sentMessages.first['messageBody'].toString().contains('[CAREBRIDGE_CHECKLIST_SHARE]'), isTrue);
    });

    test('editChecklistItemInSharedRecord modifies item and sends updated message', () async {
      final initialChecklist = ChecklistShareData(
        title: 'Checklist',
        completedCount: 0,
        totalCount: 1,
        progressPercent: 0,
        currentItems: const [
          ChecklistItemShareData(text: 'Đi bộ 15 phút', completed: false),
        ],
      );

      final updatedItem = const ChecklistItemShareData(
        text: 'Đi bộ nhẹ nhàng 20-30 phút sau ăn',
        isExpertCustom: true,
        doctorNote: 'Không đi quá sức',
      );

      final updatedChecklist = await service.editChecklistItemInSharedRecord(
        'conv-3',
        initialChecklist,
        'CURRENT',
        0,
        updatedItem,
      );

      expect(updatedChecklist.currentItems.first.text, 'Đi bộ nhẹ nhàng 20-30 phút sau ăn');
      expect(updatedChecklist.currentItems.first.doctorNote, 'Không đi quá sức');
      expect(mockApi.sentMessages.length, 1);
    });

    test('deleteChecklistItemFromSharedRecord removes item and updates removedItems list', () async {
      final initialChecklist = ChecklistShareData(
        title: 'Checklist',
        completedCount: 1,
        totalCount: 2,
        progressPercent: 50,
        currentItems: const [
          ChecklistItemShareData(text: 'Việc cần xoá', completed: false),
          ChecklistItemShareData(text: 'Việc giữ lại', completed: true),
        ],
      );

      final updatedChecklist = await service.deleteChecklistItemFromSharedRecord(
        'conv-4',
        initialChecklist,
        'CURRENT',
        0,
      );

      expect(updatedChecklist.currentItems.length, 1);
      expect(updatedChecklist.currentItems.first.text, 'Việc giữ lại');
      expect(updatedChecklist.removedItems, contains('Việc cần xoá'));
      expect(mockApi.sentMessages.length, 1);
    });
  });
}
