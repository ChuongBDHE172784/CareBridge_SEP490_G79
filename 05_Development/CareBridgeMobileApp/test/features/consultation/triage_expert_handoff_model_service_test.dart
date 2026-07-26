import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/consultation/services/triage_expert_handoff_service.dart';

const _intakeId = '68000000-0000-0000-0000-000000000101';
const _requestId = '68000000-0000-0000-0000-000000000501';
const _clientRequestId = '68000000-0000-0000-0000-000000000301';
const _expertId = '68000000-0000-0000-0000-000000000401';
const _policy = 'YELLOW_EXPERT_CONTEXT_V1';

Map<String, dynamic> _safeContextJson() => {
  'riskLevel': 'YELLOW',
  'stage': 'POSTPARTUM',
  'riskSummary': 'Synthetic sanitized YELLOW summary.',
  'citations': [
    {
      'evidenceSourceId': '68000000-0000-0000-0000-000000000201',
      'organization': 'Synthetic approved authority',
      'baseUrl': 'https://approved.example',
      'reviewedAt': '2026-07-01T00:00:00Z',
    },
  ],
};

void main() {
  test('preview parses only the approved consent allowlist contract', () async {
    final service = TriageExpertHandoffService(
      getRequest: (path) async {
        expect(path, '/api/v1/triage/intake/$_intakeId/expert-handoff-preview');
        return {
          'data': {
            'intakeSessionId': _intakeId,
            'consentPolicyVersion': _policy,
            ..._safeContextJson(),
            'sharedFields': [
              'YELLOW risk',
              'Lifecycle stage',
              'Risk summary',
              'Approved source metadata',
            ],
            'excludedFields': [
              'Raw answers or symptoms',
              'Normalized symptoms',
              'Red flags',
              'Claims',
              'Health notes',
              'AI payload',
              'Identifiers or tokens',
              'Route or origin data',
              'Pending or unreviewed sources',
              'Surplus health data',
            ],
            'ownerId': 'must-not-be-modeled',
            'continuationToken': 'must-not-be-modeled',
          },
        };
      },
    );

    final preview = await service.getPreview(_intakeId);

    expect(preview.intakeSessionId, _intakeId);
    expect(preview.consentPolicyVersion, _policy);
    expect(preview.context.riskLevel, 'YELLOW');
    expect(preview.context.citations, hasLength(1));
    expect(preview.sharedFields, hasLength(4));
    expect(preview.excludedFields, hasLength(10));
  });

  test('preview rejects incomplete or stale consent disclosures', () async {
    final service = TriageExpertHandoffService(
      getRequest: (_) async => {
        'data': {
          'intakeSessionId': _intakeId,
          'consentPolicyVersion': _policy,
          ..._safeContextJson(),
          'sharedFields': const <String>[],
          'excludedFields': const <String>[],
        },
      },
    );

    await expectLater(service.getPreview(_intakeId), throwsFormatException);
  });

  test(
    'create posts exactly the approved four-field body and parses replay',
    () async {
      String? capturedPath;
      Map<String, dynamic>? capturedBody;
      final service = TriageExpertHandoffService(
        postRequest: (path, body) async {
          capturedPath = path;
          capturedBody = Map<String, dynamic>.from(body);
          return {
            'data': {
              'consultationRequestId': _requestId,
              'requestStatus': 'PENDING',
              'replayed': true,
              'sharedAt': '2026-07-23T00:00:00Z',
              'context': _safeContextJson(),
            },
          };
        },
      );

      final result = await service.create(
        intakeSessionId: _intakeId,
        clientRequestId: _clientRequestId,
        expertProfileId: _expertId,
        consentAccepted: true,
        consentPolicyVersion: _policy,
      );

      expect(capturedPath, '/api/v1/triage/intake/$_intakeId/expert-handoffs');
      expect(capturedBody?.keys.toSet(), {
        'clientRequestId',
        'expertProfileId',
        'consentAccepted',
        'consentPolicyVersion',
      });
      expect(capturedBody, {
        'clientRequestId': _clientRequestId,
        'expertProfileId': _expertId,
        'consentAccepted': true,
        'consentPolicyVersion': _policy,
      });
      expect(result.consultationRequestId, _requestId);
      expect(result.replayed, isTrue);
      expect(result.context.riskSummary, 'Synthetic sanitized YELLOW summary.');
    },
  );

  test('create times out so an ambiguous request can be retried', () async {
    final pending = Completer<dynamic>();
    final service = TriageExpertHandoffService(
      postRequest: (_, _) => pending.future,
      requestTimeout: const Duration(milliseconds: 10),
    );

    await expectLater(
      service.create(
        intakeSessionId: _intakeId,
        clientRequestId: _clientRequestId,
        expertProfileId: _expertId,
        consentAccepted: true,
        consentPolicyVersion: _policy,
      ),
      throwsA(isA<TimeoutException>()),
    );
  });

  test(
    'participant context uses the protected request-scoped endpoint',
    () async {
      final service = TriageExpertHandoffService(
        getRequest: (path) async {
          expect(
            path,
            '/api/v1/consultation-requests/$_requestId/triage-context',
          );
          return {
            'data': {
              'consultationRequestId': _requestId,
              'requestStatus': 'PENDING',
              'sharedAt': '2026-07-23T00:00:00Z',
              'context': _safeContextJson(),
            },
          };
        },
      );

      final result = await service.getContext(_requestId);

      expect(result.consultationRequestId, _requestId);
      expect(result.context.riskLevel, 'YELLOW');
      expect(
        result.context.citations.single.baseUrl,
        'https://approved.example',
      );
    },
  );
}
