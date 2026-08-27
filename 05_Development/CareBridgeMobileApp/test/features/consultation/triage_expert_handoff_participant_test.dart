import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/features/consultation/models/consultation_request.dart';
import 'package:untitled/features/consultation/screens/consultation_request_detail_screen.dart';
import 'package:untitled/features/consultation/services/consultation_request_service.dart';
import 'package:untitled/features/consultation/services/triage_expert_handoff_service.dart';

const _requestId = '68000000-0000-0000-0000-000000000501';

ConsultationRequestDetail _request() => ConsultationRequestDetail(
  id: _requestId,
  expertProfileId: '68000000-0000-0000-0000-000000000401',
  counterpartDisplayName: 'Synthetic expert',
  topic: 'YELLOW triage expert support',
  description:
      'Consented minimum YELLOW triage context is available in the protected context view.',
  status: 'PENDING',
  expiresAt: DateTime.utc(2026, 7, 30),
  createdAt: DateTime.utc(2026, 7, 23),
);

Map<String, dynamic> _contextEnvelope() => {
  'data': {
    'consultationRequestId': _requestId,
    'requestStatus': 'PENDING',
    'sharedAt': '2026-07-23T00:00:00Z',
    'context': {
      'riskLevel': 'YELLOW',
      'stage': 'POSTPARTUM',
      'riskSummary': 'Synthetic participant-safe summary.',
      'citations': [
        {
          'evidenceSourceId': '68000000-0000-0000-0000-000000000201',
          'organization': 'Synthetic approved authority',
          'baseUrl': 'https://approved.example',
          'reviewedAt': '2026-07-01T00:00:00Z',
        },
      ],
    },
  },
};

class _ConsultationService extends ConsultationRequestService {
  _ConsultationService(this.future);
  final Future<ConsultationRequestDetail> future;

  @override
  Future<ConsultationRequestDetail> getById(String id) => future;
}

void main() {
  late ConsultationRequestService originalConsultation;
  late TriageExpertHandoffService originalHandoff;

  setUp(() async {
    originalConsultation = ConsultationRequestService.instance;
    originalHandoff = TriageExpertHandoffService.instance;
    FlutterSecureStorage.setMockInitialValues({});
    await AuthState.instance.clear();
    await AuthState.instance.setTokens(
      accessToken: 'synthetic-access-a',
      refreshToken: 'synthetic-refresh-a',
      userId: 'mother-a',
      role: 'MOTHER',
    );
  });

  tearDown(() async {
    ConsultationRequestService.instance = originalConsultation;
    TriageExpertHandoffService.instance = originalHandoff;
    await AuthState.instance.clear();
  });

  testWidgets('participant detail renders only the protected context', (
    tester,
  ) async {
    ConsultationRequestService.instance = _ConsultationService(
      Future.value(_request()),
    );
    TriageExpertHandoffService.instance = TriageExpertHandoffService(
      getRequest: (_) async => _contextEnvelope(),
    );

    await tester.pumpWidget(
      const MaterialApp(
        home: ConsultationRequestDetailScreen(requestId: _requestId),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Synthetic participant-safe summary.'), findsOneWidget);
    expect(find.text('Synthetic approved authority'), findsOneWidget);
    expect(find.text('https://approved.example'), findsOneWidget);
    expect(find.textContaining('ownerId'), findsNothing);
    expect(find.textContaining('continuationToken'), findsNothing);
  });

  testWidgets('late account-A detail is discarded after switching to B', (
    tester,
  ) async {
    final request = Completer<ConsultationRequestDetail>();
    final context = Completer<dynamic>();
    ConsultationRequestService.instance = _ConsultationService(request.future);
    TriageExpertHandoffService.instance = TriageExpertHandoffService(
      getRequest: (_) => context.future,
    );
    await tester.pumpWidget(
      const MaterialApp(
        home: ConsultationRequestDetailScreen(requestId: _requestId),
      ),
    );
    await tester.pump();

    await AuthState.instance.setTokens(
      accessToken: 'synthetic-access-b',
      refreshToken: 'synthetic-refresh-b',
      userId: 'mother-b',
      role: 'MOTHER',
    );
    request.complete(_request());
    context.complete(_contextEnvelope());
    await tester.pumpAndSettle();

    expect(find.text('YELLOW triage expert support'), findsNothing);
    expect(find.text('Synthetic participant-safe summary.'), findsNothing);
    expect(find.textContaining('Phiên đăng nhập'), findsOneWidget);
  });
}
