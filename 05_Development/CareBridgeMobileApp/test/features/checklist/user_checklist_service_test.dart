import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/checklist/services/checklist_assignment_refresh_bus.dart';
import 'package:untitled/features/checklist/services/user_checklist_service.dart';

void main() {
  test(
    'user-created task sends V2 identity and explicit mother target',
    () async {
      var refreshEvents = 0;
      final subscription = ChecklistAssignmentRefreshBus.events.listen(
        (_) => refreshEvents++,
      );
      addTearDown(subscription.cancel);
      late String capturedPath;
      late Map<String, dynamic> capturedBody;
      final service = UserChecklistService(
        postRequest: (path, body) async {
          capturedPath = path;
          capturedBody = body;
          return {
            'data': {
              'itemId': 'task-29',
              'itemText': 'Pack water',
              'category': 'GENERAL',
              'completed': false,
              'itemOrder': 2,
              'targetSubject': 'MOTHER',
              'origin': 'USER_CREATED',
            },
          };
        },
      );

      final result = await service.addItem(
        itemText: 'Pack water',
        targetSubject: 'MOTHER',
        clientTaskId: 'client-task-29',
        journeyId: 'journey-29',
        itemOrder: 2,
      );

      expect(capturedPath, '/api/v1/user-checklist-items');
      expect(capturedBody['targetSubject'], 'MOTHER');
      expect(capturedBody['clientTaskId'], 'client-task-29');
      expect(capturedBody['journeyId'], 'journey-29');
      expect(capturedBody, isNot(contains('babyId')));
      expect(result.itemId, 'task-29');
      expect(result.targetSubject, 'MOTHER');
      expect(result.origin, 'USER_CREATED');
      expect(refreshEvents, 1);
    },
  );

  test('baby-targeted task sends only the owned baby context', () async {
    late Map<String, dynamic> capturedBody;
    final service = UserChecklistService(
      postRequest: (_, body) async {
        capturedBody = body;
        return {
          'data': {
            'itemId': 'task-baby',
            'itemText': 'Prepare bottle',
            'category': 'BABY_CARE',
            'completed': false,
            'itemOrder': 0,
            'targetSubject': 'BABY',
            'origin': 'USER_CREATED',
          },
        };
      },
    );

    await service.addItem(
      itemText: 'Prepare bottle',
      targetSubject: 'BABY',
      clientTaskId: 'client-baby',
      babyId: 'baby-29',
    );

    expect(capturedBody['targetSubject'], 'BABY');
    expect(capturedBody['babyId'], 'baby-29');
    expect(capturedBody, isNot(contains('journeyId')));
  });

  test(
    'optional template uses the canonical V2 self-assignment route',
    () async {
      late String capturedPath;
      late Map<String, dynamic> capturedBody;
      final service = UserChecklistService(
        postRequest: (path, body) async {
          capturedPath = path;
          capturedBody = body;
          return {
            'data': {'createdInstances': 1, 'createdTasks': 2},
          };
        },
      );

      final result = await service.addTemplate(
        templateId: 'template-optional',
        journeyId: 'journey-pre-pregnancy',
      );

      expect(capturedPath, '/api/v1/user-checklist-items/from-template');
      expect(capturedBody, {
        'templateId': 'template-optional',
        'journeyId': 'journey-pre-pregnancy',
      });
      expect(result.createdTasks, 2);
      expect(result.hasAssignedTasks, isTrue);
    },
  );

  test('baby-care optional template omits maternal journey context', () async {
    late Map<String, dynamic> capturedBody;
    final service = UserChecklistService(
      postRequest: (_, body) async {
        capturedBody = body;
        return {
          'data': {'createdTasks': 1, 'existingTasks': 0},
        };
      },
    );

    final result = await service.addTemplate(templateId: 'baby-template');

    expect(capturedBody, {'templateId': 'baby-template'});
    expect(result.hasAssignedTasks, isTrue);
  });

  test('optional template rejects a zero-task success envelope', () async {
    var refreshEvents = 0;
    final subscription = ChecklistAssignmentRefreshBus.events.listen(
      (_) => refreshEvents++,
    );
    addTearDown(subscription.cancel);
    final service = UserChecklistService(
      postRequest: (_, _) async => {
        'data': {'createdTasks': 0, 'existingTasks': 0},
      },
    );

    await expectLater(
      service.addTemplate(templateId: 'empty-template'),
      throwsA(isA<FormatException>()),
    );
    expect(refreshEvents, 0);
  });
}
