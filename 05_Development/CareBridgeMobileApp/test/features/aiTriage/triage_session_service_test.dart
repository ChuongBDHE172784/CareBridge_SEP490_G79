import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/network/api_client.dart';
import 'package:untitled/features/aiTriage/models/triage_session.dart';
import 'package:untitled/features/aiTriage/services/triage_service.dart';

void main() {
  group('TriageSession', () {
    test('keeps only hash-pinned SOURCE_VERIFIED BM25 citations', () {
      final json = _responseJson(
        citations: [_citation('SOURCE_VERIFIED'), _citation('PENDING')],
      );

      final session = TriageSession.fromJson(json);

      expect(session.citations, hasLength(1));
      expect(session.citations.single.sourceId, 'WHO_TEST');
      expect(session.evidenceStatus, 'AVAILABLE');
      expect(session.rationale, contains('dữ kiện'));
    });

    test('legacy response without explanation remains readable', () {
      final json = _responseJson();
      json.remove('rationale');
      json.remove('evidenceStatus');

      final session = TriageSession.fromJson(json);

      expect(session.rationale, isEmpty);
      expect(session.evidenceStatus, 'UNAVAILABLE');
    });

    test('drops one malformed citation without losing another valid one', () {
      final malformed = _citation('SOURCE_VERIFIED');
      malformed['ruleIds'] = 'R_TEST';
      final session = TriageSession.fromJson(
        _responseJson(citations: [_citation('SOURCE_VERIFIED'), malformed]),
      );

      expect(session.citations, hasLength(1));
    });

    test('rejects contradictory evidence status and citations', () {
      final json = _responseJson(citations: [_citation('SOURCE_VERIFIED')]);
      json['evidenceStatus'] = 'UNAVAILABLE';

      expect(() => TriageSession.fromJson(json), throwsFormatException);
    });

    test('rejects a RED response that cannot trigger emergency UI', () {
      final json = _responseJson(outcome: 'RED');
      json['stop'] = false;
      json['action'] = 'ROUTE_TO_HEALTHCARE_WORKER';

      expect(() => TriageSession.fromJson(json), throwsFormatException);
    });

    test('answer requires exactly one typed value in release behavior', () {
      expect(() => TriageAnswer(questionId: 'Q_TEST'), throwsArgumentError);
      expect(
        () => TriageAnswer(
          questionId: 'Q_TEST',
          optionCode: 'YES',
          numericValue: 1,
        ),
        throwsArgumentError,
      );
    });

    test('rejects duplicate planned question identifiers', () {
      final json = _responseJson();
      json['questions'] = ['Q_GLOBAL_DANGER', 'Q_GLOBAL_DANGER'];
      json['questionDetails'] = [
        _questionJson('Q_GLOBAL_DANGER'),
        _questionJson('Q_GLOBAL_DANGER'),
      ];

      expect(() => TriageSession.fromJson(json), throwsFormatException);
    });

    test('never accepts public GREEN', () {
      expect(
        () => TriageSession.fromJson(_responseJson(outcome: 'GREEN')),
        throwsFormatException,
      );
    });
  });

  group('TriageSessionService', () {
    test('retry reuses messageId and requestId for the same start', () async {
      final bodies = <Map<String, dynamic>>[];
      final paths = <String>[];
      var calls = 0;
      final service = TriageSessionService(
        postRequest: (path, body) async {
          paths.add(path);
          bodies.add(Map<String, dynamic>.from(body));
          calls++;
          if (calls == 1) throw ApiException(503, '{}');
          return {'data': _responseJson()};
        },
      );

      await expectLater(
        service.start(
          message: 'Ä‘au bá»¥ng',
          selectedTarget: 'MOTHER',
          selectedStage: 'PREGNANCY',
        ),
        throwsA(isA<TriageSessionUnavailableFailure>()),
      );
      await service.start(
        message: 'Ä‘au bá»¥ng',
        selectedTarget: 'MOTHER',
        selectedStage: 'PREGNANCY',
      );

      expect(bodies[0]['requestId'], bodies[1]['requestId']);
      expect(bodies[0]['messageId'], bodies[1]['messageId']);
      expect(paths, everyElement('/api/v1/triage/sessions'));
      expect(bodies[0]['selectedStage'], 'PREGNANCY');
      expect(bodies[0]['consentContext'], isEmpty);
      expect(
        (bodies[0]['consentContext'] as Map).containsKey('disclaimerVersion'),
        isFalse,
      );
    });

    test(
      'sends lifecycle binding only through typed canonical fields',
      () async {
        Map<String, dynamic>? body;
        final service = TriageSessionService(
          postRequest: (_, value) async {
            body = Map<String, dynamic>.from(value);
            return {'data': _responseJson()};
          },
        );

        await service.start(
          message: 'dau dau',
          selectedTarget: 'MOTHER',
          selectedStage: 'PREGNANCY',
          lifecycleBinding: const {
            'journeyId': '68000000-0000-0000-0000-000000000002',
            'originDashboard': 'MOTHER_JOURNEY',
            'originReferenceId': '68000000-0000-0000-0000-000000000002',
          },
        );

        expect(body?['journeyId'], '68000000-0000-0000-0000-000000000002');
        expect(body?['originDashboard'], 'MOTHER_JOURNEY');
        expect(body?['journeyContext'], isEmpty);
      },
    );

    test(
      'overlapping failed starts retain independent retry identities',
      () async {
        final firstBodies = <String, Map<String, dynamic>>{};
        final blockers = <String, Completer<dynamic>>{};
        var firstWave = true;
        final service = TriageSessionService(
          postRequest: (_, body) {
            final message = body['message'] as String;
            if (firstWave) {
              firstBodies[message] = Map<String, dynamic>.from(body);
              return (blockers[message] = Completer<dynamic>()).future;
            }
            expect(body['requestId'], firstBodies[message]!['requestId']);
            expect(body['messageId'], firstBodies[message]!['messageId']);
            return Future.value({'data': _responseJson()});
          },
        );

        final firstA = service.start(
          message: 'A',
          selectedTarget: 'MOTHER',
          selectedStage: 'PREGNANCY',
        );
        final firstB = service.start(
          message: 'B',
          selectedTarget: 'BABY',
          selectedStage: 'INFANT_0_12M',
        );
        blockers['A']!.completeError(StateError('offline'));
        blockers['B']!.completeError(StateError('offline'));
        await expectLater(
          firstA,
          throwsA(isA<TriageSessionUnavailableFailure>()),
        );
        await expectLater(
          firstB,
          throwsA(isA<TriageSessionUnavailableFailure>()),
        );

        firstWave = false;
        await service.start(
          message: 'A',
          selectedTarget: 'MOTHER',
          selectedStage: 'PREGNANCY',
        );
        await service.start(
          message: 'B',
          selectedTarget: 'BABY',
          selectedStage: 'INFANT_0_12M',
        );
      },
    );

    test('maps a stale version conflict without treating it as safe', () async {
      final service = TriageSessionService(
        postRequest: (_, _) async => throw ApiException(
          409,
          '{"error":{"code":"TRIAGE_STATE_VERSION_CONFLICT"}}',
        ),
      );

      await expectLater(
        service.continueSession(
          session: TriageSession.fromJson(_responseJson()),
          message: 'tráº£ lá»i',
        ),
        throwsA(isA<TriageSessionStaleVersionFailure>()),
      );
    });

    test(
      'numeric answers use a typed value, not a fabricated option code',
      () async {
        Map<String, dynamic>? body;
        final service = TriageSessionService(
          postRequest: (_, value) async {
            body = Map<String, dynamic>.from(value);
            return {'data': _responseJson()};
          },
        );

        await service.continueSession(
          session: TriageSession.fromJson(_responseJson()),
          message: 'Tuổi của bé (tháng): 2',
          answers: [
            TriageAnswer(questionId: 'Q_BABY_AGE_MONTHS', numericValue: 2),
          ],
        );

        expect((body?['answers'] as List).single, {
          'questionId': 'Q_BABY_AGE_MONTHS',
          'numericValue': 2,
        });
      },
    );

    test('cancel sends the authoritative expected state version', () async {
      String? deletePath;
      final service = TriageSessionService(
        deleteRequest: (path) async {
          deletePath = path;
          return {'data': _responseJson(stop: true)};
        },
      );

      await service.cancel(TriageSession.fromJson(_responseJson(version: 7)));

      expect(deletePath, contains('expectedStateVersion=7'));
    });
  });

  test(
    'rejects a planned question without server-authored display details',
    () {
      expect(
        () => TriageSession.fromJson({
          ..._responseJson(),
          'questions': ['Q_GLOBAL_DANGER'],
        }),
        throwsFormatException,
      );
    },
  );
}

Map<String, dynamic> _responseJson({
  String outcome = 'NEEDS_MORE_INFO',
  int version = 1,
  bool stop = false,
  List<Map<String, dynamic>> citations = const [],
}) => {
  'sessionId': '10000000-0000-0000-0000-000000000001',
  'stateVersion': version,
  'target': 'MOTHER',
  'intent': 'SYMPTOM_TRIAGE',
  'stage': 'PREGNANCY',
  'outcome': outcome,
  'action': outcome == 'RED'
      ? 'IMMEDIATE_EMERGENCY_ASSESSMENT'
      : 'ASK_CLARIFYING_QUESTIONS',
  'stop': stop,
  'questions': const <String>[],
  'questionDetails': const <Map<String, dynamic>>[],
  'scope': 'IN_SCOPE',
  'pendingRisks': const <String>[],
  'citations': citations,
  'rationale': 'Kết quả được xác định từ các dữ kiện đã cung cấp.',
  'evidenceStatus': citations.isEmpty ? 'UNAVAILABLE' : 'AVAILABLE',
  'disclaimer': 'ThÃ´ng tin tham kháº£o, khÃ´ng cháº©n Ä‘oÃ¡n.',
  'readiness': const {'technicalStatus': 'READY'},
};

Map<String, dynamic> _citation(String status) => {
  'sourceId': 'WHO_TEST',
  'title': 'Danger signs',
  'organization': 'World Health Organization',
  'url': 'https://www.who.int/publications/i/item/test',
  'domain': 'who.int',
  'section': 'Danger signs',
  'contentHash': List.filled(64, 'b').join(),
  'sourceStatus': status,
  'retrievalMode': 'LOCAL_BM25',
  'ruleIds': ['R_TEST'],
};

Map<String, dynamic> _questionJson(String id) => {
  'questionId': id,
  'text': 'Câu hỏi an toàn',
  'answerType': 'SINGLE_CHOICE',
  'options': [
    {'optionCode': 'YES', 'displayText': 'Có'},
  ],
};
