import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/network/api_client.dart';
import 'package:untitled/features/aiTriage/models/triage_v2_session.dart';
import 'package:untitled/features/aiTriage/services/triage_v2_service.dart';

void main() {
  group('TriageV2Session', () {
    test('keeps only hash-pinned SOURCE_VERIFIED BM25 citations', () {
      final json = _responseJson(
        citations: [_citation('SOURCE_VERIFIED'), _citation('PENDING')],
      );

      final session = TriageV2Session.fromJson(json);

      expect(session.citations, hasLength(1));
      expect(session.citations.single.sourceId, 'WHO_TEST');
    });

    test('never accepts public GREEN', () {
      expect(
        () => TriageV2Session.fromJson(_responseJson(outcome: 'GREEN')),
        throwsFormatException,
      );
    });
  });

  group('TriageV2Service', () {
    test('retry reuses messageId and requestId for the same start', () async {
      final bodies = <Map<String, dynamic>>[];
      var calls = 0;
      final service = TriageV2Service(
        postRequest: (path, body) async {
          bodies.add(Map<String, dynamic>.from(body));
          calls++;
          if (calls == 1) throw ApiException(503, '{}');
          return {'data': _responseJson()};
        },
      );

      await expectLater(
        service.start(message: 'Ä‘au bá»¥ng', selectedTarget: 'MOTHER'),
        throwsA(isA<TriageV2UnavailableFailure>()),
      );
      await service.start(message: 'Ä‘au bá»¥ng', selectedTarget: 'MOTHER');

      expect(bodies[0]['requestId'], bodies[1]['requestId']);
      expect(bodies[0]['messageId'], bodies[1]['messageId']);
      expect(bodies[0]['consentContext'], isEmpty);
      expect(
        (bodies[0]['consentContext'] as Map).containsKey('disclaimerVersion'),
        isFalse,
      );
    });

    test(
      'overlapping failed starts retain independent retry identities',
      () async {
        final firstBodies = <String, Map<String, dynamic>>{};
        final blockers = <String, Completer<dynamic>>{};
        var firstWave = true;
        final service = TriageV2Service(
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

        final firstA = service.start(message: 'A', selectedTarget: 'MOTHER');
        final firstB = service.start(message: 'B', selectedTarget: 'BABY');
        blockers['A']!.completeError(StateError('offline'));
        blockers['B']!.completeError(StateError('offline'));
        await expectLater(firstA, throwsA(isA<TriageV2UnavailableFailure>()));
        await expectLater(firstB, throwsA(isA<TriageV2UnavailableFailure>()));

        firstWave = false;
        await service.start(message: 'A', selectedTarget: 'MOTHER');
        await service.start(message: 'B', selectedTarget: 'BABY');
      },
    );

    test('maps a stale version conflict without treating it as safe', () async {
      final service = TriageV2Service(
        postRequest: (_, _) async => throw ApiException(
          409,
          '{"error":{"code":"TRIAGE_V2_STATE_VERSION_CONFLICT"}}',
        ),
      );

      await expectLater(
        service.continueSession(
          session: TriageV2Session.fromJson(_responseJson()),
          message: 'tráº£ lá»i',
        ),
        throwsA(isA<TriageV2StaleVersionFailure>()),
      );
    });

    test('cancel sends the authoritative expected state version', () async {
      String? deletePath;
      final service = TriageV2Service(
        deleteRequest: (path) async {
          deletePath = path;
          return {'data': _responseJson(stop: true)};
        },
      );

      await service.cancel(TriageV2Session.fromJson(_responseJson(version: 7)));

      expect(deletePath, contains('expectedStateVersion=7'));
    });
  });

  test('mobile catalogue renders every canonical V2 question', () {
    const ids = <String>[
      'Q_BLEEDING_AMOUNT',
      'Q_CLOTS',
      'Q_DIZZINESS',
      'Q_VISUAL_CHANGE',
      'Q_BP_IF_KNOWN',
      'Q_EPIGASTRIC_PAIN',
      'Q_SWELLING',
      'Q_PAIN_SEVERITY',
      'Q_PREGNANCY_TEST',
      'Q_GESTATIONAL_WEEK',
      'Q_POSTPARTUM_DAY',
      'Q_CLARIFY_TARGET_ENTITY',
      'Q_CLARIFY_TARGET_FIRST',
      'Q_CLARIFY_INTENT',
      'Q_BABY_AGE_MONTHS',
      'Q_CLARIFY_STAGE',
    ];

    for (final id in ids) {
      final question = TriageV2Question.fromId(id);
      expect(question.text, isNot(contains(id)));
      expect(question.optionCodes, isNotEmpty);
    }
    expect(TriageV2Question.fromId('Q_GESTATIONAL_WEEK').answerType, 'NUMBER');
  });
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
      ? 'SEEK_EMERGENCY_CARE_NOW'
      : 'ASK_CLARIFYING_QUESTIONS',
  'stop': stop,
  'questions': const <String>[],
  'scope': 'IN_SCOPE',
  'pendingRisks': const <String>[],
  'citations': citations,
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
