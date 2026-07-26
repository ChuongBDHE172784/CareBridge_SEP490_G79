import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/checklist/services/user_checklist_service.dart';

void main() {
  test(
    'lifecycle import omits journey and baby context for server resolution',
    () async {
      late String capturedPath;
      late Map<String, dynamic> capturedBody;
      final service = UserChecklistService(
        postRequest: (path, body) async {
          capturedPath = path;
          capturedBody = body;
          return {'data': const []};
        },
      );

      await service.importFromTemplate(templateItemIds: const ['item-69']);

      expect(capturedPath, '/api/v1/user-checklist-items/import');
      expect(capturedBody['templateItemIds'], const ['item-69']);
      expect(capturedBody, isNot(contains('journeyId')));
      expect(capturedBody, isNot(contains('babyId')));
    },
  );

  test('baby import sends only baby context', () async {
    late Map<String, dynamic> capturedBody;
    final service = UserChecklistService(
      postRequest: (_, body) async {
        capturedBody = body;
        return {'data': const []};
      },
    );

    await service.importFromTemplate(
      templateItemIds: const ['item-69'],
      babyId: 'baby-69',
    );

    expect(capturedBody['babyId'], 'baby-69');
    expect(capturedBody, isNot(contains('journeyId')));
  });

  test('journey and baby contexts remain mutually exclusive', () async {
    var calls = 0;
    final service = UserChecklistService(
      postRequest: (_, _) async {
        calls++;
        return {'data': const []};
      },
    );

    expect(
      () => service.importFromTemplate(
        templateItemIds: const ['item-69'],
        journeyId: 'journey-69',
        babyId: 'baby-69',
      ),
      throwsArgumentError,
    );
    expect(calls, 0);
  });
}
