import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/checklist/services/user_checklist_service.dart';
import 'package:untitled/features/checklist/widgets/add_user_checklist_task_button.dart';

Map<String, dynamic> _createdItem(Map<String, dynamic> body) => {
  'data': {
    'itemId': '11111111-1111-4111-8111-111111111111',
    'itemText': body['itemText'],
    'category': body['category'],
    'completed': false,
    'itemOrder': body['itemOrder'] ?? 0,
    'targetSubject': body['targetSubject'],
    'origin': 'USER_CREATED',
  },
};

void main() {
  testWidgets('creates a BABY task in the explicit journey context', (
    tester,
  ) async {
    final requests = <Map<String, dynamic>>[];
    var refreshCount = 0;
    final service = UserChecklistService(
      postRequest: (path, body) async {
        expect(path, '/api/v1/user-checklist-items');
        requests.add(Map<String, dynamic>.from(body));
        return _createdItem(body);
      },
    );

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: AddUserChecklistTaskButton(
            journeyId: '22222222-2222-4222-8222-222222222222',
            service: service,
            onCreated: () async => refreshCount++,
          ),
        ),
      ),
    );

    await tester.tap(find.byKey(const Key('add-user-checklist-task')));
    await tester.pumpAndSettle();
    await tester.enterText(
      find.byKey(const Key('user-checklist-task-text')),
      'Chuẩn bị quần áo cho bé',
    );
    await tester.tap(
      find.byKey(const Key('user-checklist-target-baby')),
    );
    await tester.tap(find.byKey(const Key('save-user-checklist-task')));
    await tester.pumpAndSettle();

    expect(requests, hasLength(1));
    expect(requests.single['itemText'], 'Chuẩn bị quần áo cho bé');
    expect(requests.single['targetSubject'], 'BABY');
    expect(
      requests.single['journeyId'],
      '22222222-2222-4222-8222-222222222222',
    );
    expect(requests.single.containsKey('babyId'), isFalse);
    expect(requests.single['clientTaskId'], isNotEmpty);
    expect(refreshCount, 1);
    expect(find.text('Đã thêm việc vào hôm nay.'), findsOneWidget);
  });

  testWidgets('retry reuses the idempotency key for an unchanged payload', (
    tester,
  ) async {
    final clientTaskIds = <String>[];
    var attempts = 0;
    final service = UserChecklistService(
      postRequest: (_, body) async {
        attempts++;
        clientTaskIds.add(body['clientTaskId'] as String);
        if (attempts == 1) throw Exception('offline');
        return _createdItem(body);
      },
    );

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: AddUserChecklistTaskButton(
            babyId: '33333333-3333-4333-8333-333333333333',
            service: service,
          ),
        ),
      ),
    );

    await tester.tap(find.byKey(const Key('add-user-checklist-task')));
    await tester.pumpAndSettle();
    await tester.enterText(
      find.byKey(const Key('user-checklist-task-text')),
      'Đặt lịch tiêm cho bé',
    );
    await tester.tap(
      find.byKey(const Key('user-checklist-target-baby')),
    );
    await tester.tap(find.byKey(const Key('save-user-checklist-task')));
    await tester.pumpAndSettle();
    expect(find.text('Không thể thêm việc. Vui lòng thử lại.'), findsOneWidget);

    await tester.tap(find.byKey(const Key('save-user-checklist-task')));
    await tester.pumpAndSettle();

    expect(clientTaskIds, hasLength(2));
    expect(clientTaskIds[1], clientTaskIds[0]);
  });

  testWidgets('does not render a dead create entry without one context', (
    tester,
  ) async {
    await tester.pumpWidget(
      const MaterialApp(home: Scaffold(body: AddUserChecklistTaskButton())),
    );

    expect(find.byKey(const Key('add-user-checklist-task')), findsNothing);
  });
}
