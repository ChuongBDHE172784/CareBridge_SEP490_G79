import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/aiTriage/models/triage_intake_flow_model.dart';
import 'package:untitled/features/aiTriage/models/triage_result_model.dart';

void main() {
  test(
    'canonical envelope identity stage and status override nested result',
    () {
      final response = IntakeFlowResponse.fromJson({
        'status': 'TRIAGE_COMPLETE',
        'intakeSessionId': 'spring-canonical-id',
        'stage': 'POSTPARTUM',
        'mergedIntake': {'stage': 'POSTPARTUM'},
        'round': 3,
        'triageResult': {
          'sessionId': 'python-client-id',
          'stage': 'INFANT',
          'status': 'FAILED',
          'riskLevel': 'RED',
          'emergencyActionRequired': true,
        },
      });

      expect(response.triageResult?.sessionId, 'spring-canonical-id');
      expect(response.triageResult?.stage, 'POSTPARTUM');
      expect(response.triageResult?.status, 'COMPLETED');
      expect(response.triageResult?.triageStatus, 'TRIAGE_COMPLETE');
    },
  );

  test('legacy GET ASK_MORE maps to canonical non-terminal status', () {
    final result = TriageResult.fromJson({
      'sessionId': 'spring-canonical-id',
      'stage': 'INFANT',
      'status': 'ASK_MORE',
      'triageStatus': 'ASK_MORE',
      'questions': ['Bé hiện bao nhiêu tháng tuổi?'],
    });

    expect(result.status, 'NEED_MORE_INFO');
    expect(result.triageStatus, 'ASK_MORE');
    expect(result.questions, isNotEmpty);
  });

  test('legacy nested stage and status survive an incomplete envelope', () {
    final response = IntakeFlowResponse.fromJson({
      'intakeSessionId': 'spring-canonical-id',
      'mergedIntake': <String, dynamic>{},
      'round': 3,
      'triageResult': {
        'stage': 'POSTPARTUM',
        'status': 'TRIAGE_COMPLETE',
        'riskLevel': 'YELLOW',
      },
    });

    expect(response.triageResult?.stage, 'POSTPARTUM');
    expect(response.triageResult?.status, 'COMPLETED');
  });

  test('claims retain only their citation references and stage', () {
    final result = TriageResult.fromJson({
      'sessionId': 'spring-canonical-id',
      'stage': 'POSTPARTUM',
      'status': 'COMPLETED',
      'riskLevel': 'YELLOW',
      'citations': [
        {
          'sourceId': 'WHO-1',
          'title': 'Official guidance',
          'organization': 'WHO',
          'url': 'https://www.who.int/example',
          'domain': 'who.int',
          'excerpt': 'Short relevant excerpt',
          'retrievedAt': '2026-07-22T00:00:00Z',
          'sourceStatus': 'APPROVED',
          'sourceVersion': '1',
          'retrievalMode': 'LOCAL',
          'matchedSymptoms': ['fever'],
          'matchedRules': ['YELLOW_MONITOR'],
        },
      ],
      'claims': [
        {
          'claimId': 'CLAIM-1',
          'text': 'Monitor symptoms.',
          'evidenceIds': ['WHO-1'],
        },
      ],
    });

    expect(result.stage, 'POSTPARTUM');
    expect(result.citations.single.id, 'WHO-1');
    expect(result.claims.single.evidenceIds, ['WHO-1']);
  });
}
