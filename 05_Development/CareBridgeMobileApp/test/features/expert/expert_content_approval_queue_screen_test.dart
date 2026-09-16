import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:untitled/features/expert/screens/expert_content_approval_queue_screen.dart';
import 'package:untitled/features/expert/services/expert_content_approval_service.dart';

class _FakeApprovalApi implements ExpertContentApprovalApi {
  List<Map<String, dynamic>> queueItems;
  String? decidedId;
  String? decision;
  String? reason;

  _FakeApprovalApi({required this.queueItems});

  @override
  Future<dynamic> get(String path, {Map<String, dynamic>? queryParams}) async {
    return {
      'data': {
        'content': queueItems,
        'totalElements': queueItems.length,
        'totalPages': 1,
        'size': 20,
        'number': 0,
      },
    };
  }

  @override
  Future<dynamic> post(String path, Map<String, dynamic> body) async {
    decision = body['decision'] as String?;
    reason = body['reason'] as String?;
    return {'status': 'OK'};
  }
}

void main() {
  testWidgets('ExpertContentApprovalQueueScreen displays queue items and triggers approve', (tester) async {
    final fakeApi = _FakeApprovalApi(
      queueItems: [
        {
          'id': 'art-100',
          'kind': 'CONTENT',
          'type': 'ARTICLE',
          'title': 'Chế độ ăn cho mẹ tiểu đường thai kỳ',
          'stage': 'PREGNANCY',
          'status': 'PENDING_REVIEW',
          'summary': 'Kiểm soát đường huyết hiệu quả',
          'assignedAt': '2026-09-15T08:00:00.000Z',
        },
      ],
    );

    final service = ExpertContentApprovalService(api: fakeApi);

    await tester.pumpWidget(
      MaterialApp(
        home: ExpertContentApprovalQueueScreen(service: service),
      ),
    );

    await tester.pumpAndSettle();

    expect(find.text('Thẩm định nội dung (1)'), findsOneWidget);
    expect(find.text('Chế độ ăn cho mẹ tiểu đường thai kỳ'), findsOneWidget);
    expect(find.text('Kiểm soát đường huyết hiệu quả'), findsOneWidget);
    expect(find.text('Phê duyệt'), findsOneWidget);
    expect(find.text('Yêu cầu sửa'), findsOneWidget);

    // Tap quick approve
    await tester.tap(find.text('Phê duyệt'));
    await tester.pumpAndSettle();

    // Confirm dialog opens
    expect(
      find.textContaining(
        'muốn phê duyệt và xuất bản "Chế độ ăn cho mẹ tiểu đường thai kỳ"',
      ),
      findsOneWidget,
    );

    // Click "Phê duyệt" in dialog
    final confirmButtons = find.widgetWithText(ElevatedButton, 'Phê duyệt');
    await tester.tap(confirmButtons.last);
    await tester.pumpAndSettle();

    expect(fakeApi.decision, 'APPROVE');
  });

  testWidgets('ExpertContentApprovalQueueScreen shows empty state when no items', (tester) async {
    final fakeApi = _FakeApprovalApi(queueItems: []);
    final service = ExpertContentApprovalService(api: fakeApi);

    await tester.pumpWidget(
      MaterialApp(
        home: ExpertContentApprovalQueueScreen(service: service),
      ),
    );

    await tester.pumpAndSettle();

    expect(find.text('Không có nội dung nào chờ thẩm định'), findsOneWidget);
  });
}
