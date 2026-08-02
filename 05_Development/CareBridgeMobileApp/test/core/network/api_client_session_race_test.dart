import 'dart:async';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/core/network/api_client.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() async {
    FlutterSecureStorage.setMockInitialValues({});
    await AuthState.instance.clear();
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
    'late implicit 401 is typed and cannot clear a replacement account',
    () async {
      final delayedResponse = Completer<http.Response>();
      var resourceRequests = 0;
      var refreshRequests = 0;
      final client = MockClient((request) {
        if (request.url.path == '/api/v1/triage/consent/status') {
          resourceRequests++;
          return delayedResponse.future;
        }
        if (request.url.path == '/api/v1/auth/refresh') {
          refreshRequests++;
        }
        throw StateError('Unexpected request: ${request.url.path}');
      });
      addTearDown(client.close);

      final request = http.runWithClient(
        () => apiGet('/api/v1/triage/consent/status'),
        () => client,
      );
      await Future<void>.delayed(Duration.zero);
      expect(resourceRequests, 1);

      await _replaceWithAccountB();
      delayedResponse.complete(http.Response('{"error":"TOKEN_EXPIRED"}', 401));

      await expectLater(request, throwsA(_sessionChangedException));
      expect(resourceRequests, 1);
      expect(refreshRequests, 0);
      _expectAccountBIsCurrent();
    },
  );

  test(
    'account switch while refresh is pending discards refresh and retry',
    () async {
      final refreshResponse = Completer<http.Response>();
      var resourceRequests = 0;
      var refreshRequests = 0;
      final client = MockClient((request) async {
        if (request.url.path == '/api/v1/triage/intake') {
          resourceRequests++;
          return http.Response('{"error":"TOKEN_EXPIRED"}', 401);
        }
        if (request.url.path == '/api/v1/auth/refresh') {
          refreshRequests++;
          expect(request.body, contains('refresh-a'));
          return refreshResponse.future;
        }
        throw StateError('Unexpected request: ${request.url.path}');
      });
      addTearDown(client.close);

      final request = http.runWithClient(
        () =>
            apiPost('/api/v1/triage/intake', const {'symptoms': 'mild fever'}),
        () => client,
      );
      while (refreshRequests == 0) {
        await Future<void>.delayed(Duration.zero);
      }

      await _replaceWithAccountB();
      refreshResponse.complete(
        http.Response(
          '{"data":{"accessToken":"access-a-new","refreshToken":"refresh-a-new"}}',
          200,
        ),
      );

      await expectLater(request, throwsA(_sessionChangedException));
      expect(resourceRequests, 1);
      expect(refreshRequests, 1);
      _expectAccountBIsCurrent();
    },
  );

  test('late 401 protection applies to every implicit JSON verb', () async {
    final invocations = <Future<dynamic> Function()>[
      () => apiPost('/late-post', const {'value': 1}),
      () => apiPut('/late-put', const {'value': 1}),
      () => apiPatch('/late-patch', const {'value': 1}),
      () => apiDelete('/late-delete', body: const {'value': 1}),
    ];

    for (final invoke in invocations) {
      await AuthState.instance.setTokens(
        accessToken: 'access-a',
        refreshToken: 'refresh-a',
        userId: 'account-a',
        role: 'MOTHER',
      );
      final delayedResponse = Completer<http.Response>();
      var calls = 0;
      final client = MockClient((request) {
        calls++;
        return delayedResponse.future;
      });
      final request = http.runWithClient(invoke, () => client);
      await Future<void>.delayed(Duration.zero);
      await _replaceWithAccountB();
      delayedResponse.complete(http.Response('', 401));

      await expectLater(request, throwsA(_sessionChangedException));
      expect(calls, 1);
      _expectAccountBIsCurrent();
      client.close();
    }
  });

  test(
    'late multipart 401 cannot refresh or clear a replacement account',
    () async {
      final delayedResponse = Completer<http.Response>();
      var uploadRequests = 0;
      var refreshRequests = 0;
      final client = MockClient((request) {
        if (request.url.path == '/api/v1/evidence') {
          uploadRequests++;
          expect(request.headers['authorization'], 'Bearer access-a');
          return delayedResponse.future;
        }
        if (request.url.path == '/api/v1/auth/refresh') {
          refreshRequests++;
        }
        throw StateError('Unexpected request: ${request.url.path}');
      });
      addTearDown(client.close);

      final request = http.runWithClient(
        () => apiMultipart(
          '/api/v1/evidence',
          const {'kind': 'triage'},
          files: const [
            MultipartUploadFile(
              fieldName: 'file',
              bytes: [1, 2, 3],
              fileName: 'evidence.jpg',
              mimeType: 'image/jpeg',
            ),
          ],
        ),
        () => client,
      );
      await Future<void>.delayed(Duration.zero);
      expect(uploadRequests, 1);

      await _replaceWithAccountB();
      delayedResponse.complete(http.Response('', 401));

      await expectLater(request, throwsA(_sessionChangedException));
      expect(uploadRequests, 1);
      expect(refreshRequests, 0);
      _expectAccountBIsCurrent();
    },
  );

  test('late success from a replaced account is rejected', () async {
    final delayedResponse = Completer<http.Response>();
    final client = MockClient((_) => delayedResponse.future);
    addTearDown(client.close);

    final request = http.runWithClient(
      () => apiGet('/api/v1/triage/consent/status'),
      () => client,
    );
    await Future<void>.delayed(Duration.zero);
    await _replaceWithAccountB();
    delayedResponse.complete(http.Response('{"data":{"granted":true}}', 200));

    await expectLater(request, throwsA(_sessionChangedException));
    _expectAccountBIsCurrent();
  });

  test(
    'same-session token rotation does not reject a valid late success',
    () async {
      final delayedResponse = Completer<http.Response>();
      final client = MockClient((_) => delayedResponse.future);
      addTearDown(client.close);
      final generation = AuthState.instance.sessionGeneration;

      final request = http.runWithClient(
        () => apiGet('/api/v1/triage/consent/status'),
        () => client,
      );
      await Future<void>.delayed(Duration.zero);
      final published = await AuthState.instance.setTokensIfCurrent(
        expectedGeneration: generation,
        expectedAccessToken: 'access-a',
        expectedRefreshToken: 'refresh-a',
        expectedUserId: 'account-a',
        accessToken: 'access-a-new',
        refreshToken: 'refresh-a-new',
        role: 'MOTHER',
      );
      expect(published, isTrue);
      delayedResponse.complete(http.Response('{"data":{"granted":true}}', 200));

      final result = await request as Map<String, dynamic>;
      expect(result['data'], {'granted': true});
      expect(AuthState.instance.accessToken, 'access-a-new');
    },
  );

  test('multipart second 401 clears the refreshed initiating session', () async {
    var uploadRequests = 0;
    final client = MockClient((request) async {
      if (request.url.path == '/api/v1/evidence') {
        uploadRequests++;
        return http.Response('{"error":"TOKEN_EXPIRED"}', 401);
      }
      if (request.url.path == '/api/v1/auth/refresh') {
        return http.Response(
          '{"data":{"accessToken":"access-a-new","refreshToken":"refresh-a-new"}}',
          200,
        );
      }
      throw StateError('Unexpected request: ${request.url.path}');
    });
    addTearDown(client.close);

    final request = http.runWithClient(
      () => apiMultipart(
        '/api/v1/evidence',
        const {'kind': 'triage'},
        files: const [
          MultipartUploadFile(
            fieldName: 'file',
            bytes: [1, 2, 3],
            fileName: 'evidence.jpg',
            mimeType: 'image/jpeg',
          ),
        ],
      ),
      () => client,
    );

    await expectLater(request, throwsA(isA<ApiException>()));
    expect(uploadRequests, 2);
    expect(AuthState.instance.isAuthenticated, isFalse);
  });
}

final Matcher _sessionChangedException = isA<ApiException>()
    .having((error) => error.statusCode, 'statusCode', 401)
    .having((error) => error.errorCode, 'errorCode', 'AUTH_SESSION_CHANGED');

Future<void> _replaceWithAccountB() => AuthState.instance.setTokens(
  accessToken: 'access-b',
  refreshToken: 'refresh-b',
  userId: 'account-b',
  role: 'FAMILY',
);

void _expectAccountBIsCurrent() {
  expect(AuthState.instance.userId, 'account-b');
  expect(AuthState.instance.accessToken, 'access-b');
  expect(AuthState.instance.refreshToken, 'refresh-b');
}
