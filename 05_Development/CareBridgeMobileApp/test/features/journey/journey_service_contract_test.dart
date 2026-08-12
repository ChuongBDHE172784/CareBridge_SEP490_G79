import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/features/journey/models/journey_model.dart';
import 'package:untitled/features/journey/services/journey_service.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() async {
    FlutterSecureStorage.setMockInitialValues({});
    await AuthState.instance.setTokens(
      accessToken: 'access-a',
      refreshToken: 'refresh-a',
      userId: 'account-a',
      role: 'MOTHER',
    );
  });

  tearDown(() async {
    await AuthState.instance.clear();
  });

  test(
    'default journey create sends V2 contract and one dating authority',
    () async {
      late http.BaseRequest request;
      final client = MockClient((incoming) async {
        request = incoming;
        return http.Response(
          jsonEncode({
            'data': {
              'id': 'journey-1',
              'journeyType': 'PREGNANCY',
              'status': 'ACTIVE',
              'startDate': '2026-07-18',
              'createdAt': '2026-07-18T03:00:00Z',
              'lastMenstrualDate': '2026-05-01',
              'estimatedDueDate': '2027-02-05',
              'version': 0,
            },
          }),
          201,
          headers: const {'content-type': 'application/json'},
        );
      });
      addTearDown(client.close);

      final service = JourneyService(
        dashboardCacheWriterOverride:
            (_, {bool pendingSync = false, String? expectedUserId}) async {},
        currentUserIdProvider: () => 'account-a',
      );

      await http.runWithClient(
        () => service.createJourney(
          const CreateJourneyRequest(
            journeyType: JourneyType.pregnancy,
            startDate: '2026-07-18',
            lastMenstrualDate: '2026-05-01',
            datingBasis: 'LMP',
          ),
        ),
        () => client,
      );

      expect(request.headers['x-checklist-contract-version'], '2');
      final body =
          jsonDecode((request as http.Request).body) as Map<String, dynamic>;
      expect(body['datingBasis'], 'LMP');
      expect(body['lastMenstrualDate'], '2026-05-01');
      expect(body.containsKey('estimatedDueDate'), isFalse);
    },
  );
}
