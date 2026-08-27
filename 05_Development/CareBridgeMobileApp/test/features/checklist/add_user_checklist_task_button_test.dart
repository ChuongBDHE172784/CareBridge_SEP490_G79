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
      postV2Request: (path, body, headers) async {
        expect(path, '/api/v1/user-checklist-items');
        expect(headers, {'X-Checklist-Contract-Version': '2'});
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
    await tester.tap(find.byKey(const Key('save-user-checklist-task')));
    await tester.pumpAndSettle();

    expect(requests, hasLength(1));
    expect(requests.single['itemText'], 'Chuẩn bị quần áo cho bé');
    expect(requests.single.containsKey('targetSubject'), isFalse);
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
      postV2Request: (_, body, _) async {
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
    await tester.ensureVisible(find.byKey(const Key('save-user-checklist-task')));
    await tester.tap(find.byKey(const Key('save-user-checklist-task')));
    await tester.pumpAndSettle();
    expect(find.text('Không thể thêm việc. Vui lòng thử lại.'), findsOneWidget);

    await tester.ensureVisible(find.byKey(const Key('save-user-checklist-task')));
    await tester.tap(find.byKey(const Key('save-user-checklist-task')));
    await tester.pumpAndSettle();

    expect(clientTaskIds, hasLength(2));
    expect(clientTaskIds[1], clientTaskIds[0]);
  });

  testWidgets('family personal task sends the selected care group scope', (
    tester,
  ) async {
    Map<String, dynamic>? request;
    final service = UserChecklistService(
      postV2Request: (path, body, _) async {
        expect(path, '/api/v1/user-checklist-items');
        request = Map<String, dynamic>.from(body);
        return _createdItem(body);
      },
    );

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: AddUserChecklistTaskButton(
            careGroupId: '44444444-4444-4444-8444-444444444444',
            service: service,
          ),
        ),
      ),
    );

    expect(find.byKey(const Key('add-user-checklist-task')), findsOneWidget);
    await tester.tap(find.byKey(const Key('add-user-checklist-task')));
    await tester.pumpAndSettle();
    await tester.enterText(
      find.byKey(const Key('user-checklist-task-text')),
      'Nhắc mẹ uống nước',
    );
    await tester.tap(find.byKey(const Key('save-user-checklist-task')));
    await tester.pumpAndSettle();

    expect(request?['careGroupId'], '44444444-4444-4444-8444-444444444444');
    expect(request?.containsKey('journeyId'), isFalse);
    expect(request?.containsKey('babyId'), isFalse);
  });

  testWidgets('does not render a dead create entry without one context', (
    tester,
  ) async {
    await tester.pumpWidget(
      const MaterialApp(home: Scaffold(body: AddUserChecklistTaskButton())),
    );

    expect(find.byKey(const Key('add-user-checklist-task')), findsNothing);
  });

  testWidgets('does not render when contexts are ambiguous', (tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: Scaffold(
          body: AddUserChecklistTaskButton(
            careGroupId: '44444444-4444-4444-8444-444444444444',
            journeyId: '22222222-2222-4222-8222-222222222222',
          ),
        ),
      ),
    );

    expect(find.byKey(const Key('add-user-checklist-task')), findsNothing);
  });

  testWidgets('renders new fields (name, description, recurrence, duration) and excludes category', (
    tester,
  ) async {
    final requests = <Map<String, dynamic>>[];
    final service = UserChecklistService(
      postV2Request: (path, body, headers) async {
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
          ),
        ),
      ),
    );

    await tester.tap(find.byKey(const Key('add-user-checklist-task')));
    await tester.pumpAndSettle();

    // Verify fields presence
    expect(find.byKey(const Key('user-checklist-task-text')), findsOneWidget);
    expect(find.byKey(const Key('user-checklist-task-description')), findsOneWidget);
    expect(find.byKey(const Key('user-checklist-task-recurrence')), findsOneWidget);
    expect(find.byKey(const Key('user-checklist-task-duration')), findsOneWidget);
    // Verify "Nhóm công việc" is removed
    expect(find.text('Nhóm công việc'), findsNothing);

    // Enter name & description
    await tester.enterText(
      find.byKey(const Key('user-checklist-task-text')),
      'Uống vitamin bầu',
    );
    await tester.enterText(
      find.byKey(const Key('user-checklist-task-description')),
      'Uống sau bữa ăn sáng 30 phút',
    );

    await tester.ensureVisible(find.byKey(const Key('save-user-checklist-task')));
    await tester.tap(find.byKey(const Key('save-user-checklist-task')));
    await tester.pumpAndSettle();

    expect(requests, hasLength(1));
    expect(
      requests.single['itemText'],
      'Uống vitamin bầu\nUống sau bữa ăn sáng 30 phút',
    );
  });
}
