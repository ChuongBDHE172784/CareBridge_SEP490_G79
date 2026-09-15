import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/directChat/models/checklist_share_data.dart';
import 'package:untitled/features/directChat/models/direct_conversation.dart';
import 'package:untitled/features/directChat/models/health_metrics_share_data.dart';
import 'package:untitled/features/directChat/models/timeline_item.dart';
import 'package:untitled/features/directChat/models/timeline_page.dart';
import 'package:untitled/features/expert/models/expert_shared_record_model.dart';
import 'package:untitled/features/expert/screens/expert_shared_records_screen.dart';
import 'package:untitled/features/expert/services/expert_shared_records_service.dart';
import 'package:uuid/uuid.dart';

class MockExpertApi implements ExpertSharedRecordsApi {
  List<DirectConversationSummary> conversations = [];
  Map<String, TimelinePage> timelines = {};
  Map<String, dynamic> apiGetResponses = {};

  @override
  Future<List<DirectConversationSummary>> listConversations() async => conversations;

  @override
  Future<TimelinePage> getTimeline(String conversationId, {int limit = 50}) async =>
      timelines[conversationId] ??
      const TimelinePage(
        items: [],
        nextCursor: null,
        previousCursor: null,
        hasMoreOlder: false,
        hasMoreNewer: false,
      );

  @override
  Future<dynamic> sendMessage(
    String conversationId, {
    required String clientMessageId,
    required String messageBody,
    String messageType = 'TEXT',
  }) async =>
      {'id': 'msg-test'};

  @override
  Future<dynamic> get(String path) async => apiGetResponses[path] ?? {};
}

void main() {
  group('Health Metrics Share Data & Alert Level Evaluation', () {
    test('evaluateHealthAlertLevel returns critical if any metric is critical', () {
      const data = HealthMetricsShareData(
        title: 'Chỉ số sức khỏe',
        metrics: [
          HealthMetricItemData(
            code: 'HEART_RATE',
            name: 'Nhịp tim',
            value: '75',
            unit: 'bpm',
            status: 'NORMAL',
          ),
          HealthMetricItemData(
            code: 'BLOOD_PRESSURE',
            name: 'Huyết áp',
            value: '160/100',
            unit: 'mmHg',
            status: 'CRITICAL',
          ),
        ],
      );

      expect(evaluateHealthAlertLevel(data), equals(SharedRecordAlertLevel.critical));
    });

    test('evaluateHealthAlertLevel returns warning if warning exists without critical', () {
      const data = HealthMetricsShareData(
        title: 'Chỉ số sức khỏe',
        metrics: [
          HealthMetricItemData(
            code: 'BLOOD_GLUCOSE',
            name: 'Đường huyết',
            value: '7.2',
            unit: 'mmol/L',
            status: 'WARNING',
          ),
          HealthMetricItemData(
            code: 'WEIGHT',
            name: 'Cân nặng',
            value: '62',
            unit: 'kg',
            status: 'NORMAL',
          ),
        ],
      );

      expect(evaluateHealthAlertLevel(data), equals(SharedRecordAlertLevel.warning));
    });

    test('evaluateHealthAlertLevel returns normal when all metrics are normal', () {
      const data = HealthMetricsShareData(
        title: 'Chỉ số sức khỏe',
        metrics: [
          HealthMetricItemData(
            code: 'SPO2',
            name: 'SpO2',
            value: '99',
            unit: '%',
            status: 'NORMAL',
          ),
        ],
      );

      expect(evaluateHealthAlertLevel(data), equals(SharedRecordAlertLevel.normal));
    });

    test('HealthMetricsShareData parse correctly extracts history measurement points', () {
      final jsonBody = '${HealthMetricsShareData.tag}\n'
          '{"title":"Chỉ số thai kỳ",'
          '"gestationalWeek":26,'
          '"timeRangeLabel":"7 ngày qua",'
          '"note":"Mẹ có dấu hiệu hoa mắt nhẹ",'
          '"metrics":['
          '{"code":"BLOOD_PRESSURE","name":"Huyết áp","value":"135/85","unit":"mmHg","status":"WARNING","measuredTime":"14:30 15/09",'
          '"history":[{"measuredAt":"14:30 15/09","value":"135/85","unit":"mmHg","status":"WARNING","note":"Đo sau khi đi bộ"},'
          '{"measuredAt":"08:00 15/09","value":"120/80","unit":"mmHg","status":"NORMAL"}]}'
          ']}';

      final parsed = HealthMetricsShareData.parse(jsonBody);
      expect(parsed, isNotNull);
      expect(parsed!.title, equals('Chỉ số thai kỳ'));
      expect(parsed.gestationalWeek, equals(26));
      expect(parsed.timeRangeLabel, equals('7 ngày qua'));
      expect(parsed.note, equals('Mẹ có dấu hiệu hoa mắt nhẹ'));
      expect(parsed.metrics.length, equals(1));

      final bp = parsed.metrics.first;
      expect(bp.code, equals('BLOOD_PRESSURE'));
      expect(bp.value, equals('135/85'));
      expect(bp.status, equals('WARNING'));
      expect(bp.history.length, equals(2));
      expect(bp.history[0].measuredAt, equals('14:30 15/09'));
      expect(bp.history[0].note, equals('Đo sau khi đi bộ'));
      expect(bp.history[1].status, equals('NORMAL'));
    });
  });

  group('ExpertSharedRecordsScreen UI & Health Inspection Flow', () {
    late MockExpertApi mockApi;

    setUp(() {
      mockApi = MockExpertApi();

      // Setup conversation
      mockApi.conversations = [
        DirectConversationSummary(
          conversationId: 'conv-101',
          counterpartUserId: 'user-mother-1',
          counterpartDisplayName: 'Nguyễn Thị Hoa',
          counterpartRole: 'MOTHER',
          lastMessagePreview: 'Chia sẻ chỉ số sức khỏe',
          lastMessageAt: DateTime(2026, 9, 15, 14, 30),
          expertAvailable: true,
          unreadCount: 0,
        ),
      ];

      // Setup timeline with both Health Metrics and Checklist shares
      final healthJson = '${HealthMetricsShareData.tag}\n'
          '{"title":"Chỉ số theo dõi thai kỳ",'
          '"gestationalWeek":28,'
          '"timeRangeLabel":"Hôm nay",'
          '"note":"Hôm nay huyết áp ổn định hơn hôm qua",'
          '"metrics":['
          '{"code":"BLOOD_PRESSURE","name":"Huyết áp","value":"120/80","unit":"mmHg","status":"NORMAL","measuredTime":"09:00",'
          '"history":[{"measuredAt":"09:00 15/09","value":"120/80","unit":"mmHg","status":"NORMAL"}]},'
          '{"code":"HEART_RATE","name":"Nhịp tim","value":"78","unit":"bpm","status":"NORMAL","measuredTime":"09:00",'
          '"history":[{"measuredAt":"09:00 15/09","value":"78","unit":"bpm","status":"NORMAL"}]}'
          ']}';

      final checklistJson = '${ChecklistShareData.tag}\n'
          '{"title":"Checklist tuần 28",'
          '"completedCount":2,'
          '"totalCount":4,'
          '"progressPercent":50,'
          '"currentItems":[{"text":"Uống viên sắt","completed":true,"origin":"SYSTEM"}]}';

      mockApi.timelines['conv-101'] = TimelinePage(
        items: [
          TimelineItem(
            kind: 'MESSAGE',
            messageId: 'm-2',
            clientMessageId: 'c-2',
            senderUserId: 'user-mother-1',
            messageBody: checklistJson,
            createdAt: DateTime(2026, 9, 15, 10, 0),
          ),
          TimelineItem(
            kind: 'MESSAGE',
            messageId: 'm-1',
            clientMessageId: 'c-1',
            senderUserId: 'user-mother-1',
            messageBody: healthJson,
            createdAt: DateTime(2026, 9, 15, 9, 0),
          ),
        ],
        nextCursor: null,
        previousCursor: null,
        hasMoreOlder: false,
        hasMoreNewer: false,
      );
    });

    testWidgets('Renders mother card with health sub-tab, metrics grid, and opens inspection sheet', (
      tester,
    ) async {
      final service = ExpertSharedRecordsService(
        api: mockApi,
        uuid: const Uuid(),
      );

      await tester.pumpWidget(
        MaterialApp(
          routes: {
            '/direct-chats/conv-101': (_) => const Scaffold(body: Text('Chat Screen Conv 101')),
          },
          home: ExpertSharedRecordsScreen(service: service),
        ),
      );

      // Loading state
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
      await tester.pumpAndSettle();

      // Mother name and Alert level
      expect(find.text('Nguyễn Thị Hoa'), findsOneWidget);
      expect(find.text('Bình thường'), findsWidgets);

      // Sub-tab switcher should show both tabs
      expect(find.text('Chỉ số sức khỏe'), findsOneWidget);
      expect(find.text('Việc cần làm'), findsOneWidget);

      // In HEALTH sub-tab: Vital metrics cards
      expect(find.text('Huyết áp'), findsOneWidget);
      expect(find.text('120/80'), findsOneWidget);
      expect(find.text('Nhịp tim'), findsOneWidget);
      expect(find.text('78'), findsOneWidget);
      expect(find.text('Hôm nay huyết áp ổn định hơn hôm qua'), findsOneWidget);

      // Buttons in HEALTH sub-tab
      expect(find.text('Lịch sử đo chi tiết'), findsOneWidget);
      expect(find.text('Tư vấn'), findsOneWidget);

      // Switch to CHECKLIST sub-tab
      await tester.tap(find.text('Việc cần làm'));
      await tester.pumpAndSettle();

      // Checklist content visible
      expect(find.text('Checklist tuần 28'), findsOneWidget);
      expect(find.text('Xem & Quản lý Checklist'), findsOneWidget);

      // Switch back to HEALTH sub-tab
      await tester.tap(find.text('Chỉ số sức khỏe'));
      await tester.pumpAndSettle();

      expect(find.text('Lịch sử đo chi tiết'), findsOneWidget);

      // Tap "Lịch sử đo chi tiết" to open _HealthInspectionSheet modal
      await tester.tap(find.text('Lịch sử đo chi tiết'));
      await tester.pumpAndSettle();

      // Inspection sheet contents
      expect(find.text('Lịch sử các lần đo gần đây (1 lần):'), findsNWidgets(2));
      expect(find.text('09:00 15/09'), findsNWidgets(2));
      expect(find.text('Nhắn tin tư vấn cho mẹ'), findsOneWidget);

      // Tap "Nhắn tin tư vấn cho mẹ" navigates to chat route
      await tester.tap(find.text('Nhắn tin tư vấn cho mẹ'));
      await tester.pumpAndSettle();

      expect(find.text('Chat Screen Conv 101'), findsOneWidget);
    });
  });
}
