import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/exercise/services/exercise_service.dart';

void main() {
  group('ExerciseService.getHistory', () {
    test('decodes the canonical top-level data list', () async {
      final service = ExerciseService.forTesting(
        get: (_) async => <String, dynamic>{
          'data': <dynamic>[_historyJson()],
          'page': 0,
          'size': 20,
          'totalElements': 1,
          'totalPages': 1,
        },
      );

      final history = await service.getHistory();

      expect(history, hasLength(1));
      expect(history.single.sessionId, '73000000-0000-0000-0000-000000000001');
      expect(history.single.exerciseTitle, 'Yoga bầu 20 phút');
    });

    test('decodes an empty canonical list as empty history', () async {
      final service = ExerciseService.forTesting(
        get: (_) async => <String, dynamic>{'data': <dynamic>[]},
      );

      expect(await service.getHistory(), isEmpty);
    });

    test('supports the nested content compatibility shape', () async {
      final service = ExerciseService.forTesting(
        get: (_) async => <String, dynamic>{
          'data': <String, dynamic>{
            'content': <dynamic>[_historyJson()],
          },
        },
      );

      expect(
        (await service.getHistory()).single.exerciseTitle,
        'Yoga bầu 20 phút',
      );
    });

    test('rejects unsupported data shapes', () async {
      final service = ExerciseService.forTesting(
        get: (_) async => <String, dynamic>{
          'data': <String, dynamic>{'items': <dynamic>[]},
        },
      );

      await expectLater(service.getHistory(), throwsA(isA<FormatException>()));
    });

    test('sends the exact supported history query path', () async {
      String? capturedPath;
      final service = ExerciseService.forTesting(
        get: (path) async {
          capturedPath = path;
          return <String, dynamic>{'data': <dynamic>[]};
        },
      );

      await service.getHistory(trimesterScope: 'SECOND', page: 2, size: 10);

      expect(
        capturedPath,
        '/api/v1/exercises/sessions/history?page=2&size=10&trimesterScope=SECOND',
      );
    });
  });
}

Map<String, dynamic> _historyJson() => <String, dynamic>{
  'exerciseSessionId': '73000000-0000-0000-0000-000000000001',
  'exerciseId': '60000000-0000-0000-0000-000000000003',
  'exerciseTitle': 'Yoga bầu 20 phút',
  'sessionStatus': 'COMPLETED',
  'startedAt': '2026-07-26T17:00:00+07:00',
  'actualDurationSeconds': 1200,
  'completionPercent': 100.0,
  'warningCount': 0,
};
