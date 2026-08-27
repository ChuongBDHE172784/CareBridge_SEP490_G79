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
    'delayed explicit-token 401 cannot clear a replacement account',
    () async {
      final delayedResponse = Completer<http.Response>();
      var babyRequests = 0;
      var refreshRequests = 0;
      final client = MockClient((request) {
        if (request.url.path == '/api/v1/babies') {
          babyRequests++;
          expect(
            request.headers.entries
                .singleWhere(
                  (entry) => entry.key.toLowerCase() == 'authorization',
                )
                .value,
            'Bearer access-a',
          );
          return delayedResponse.future;
        }
        if (request.url.path == '/api/v1/auth/refresh') {
          refreshRequests++;
        }
        throw StateError('Unexpected request: ${request.url.path}');
      });
      addTearDown(client.close);

      final request = apiPost(
        '/api/v1/babies',
        const {'nickname': 'Baby Bean'},
        token: 'access-a',
        expectedAccountId: 'account-a',
        client: client,
      );
      await Future<void>.delayed(Duration.zero);
      expect(babyRequests, 1);

      await AuthState.instance.setTokens(
        accessToken: 'access-b',
        refreshToken: 'refresh-b',
        userId: 'account-b',
        role: 'MOTHER',
      );
      delayedResponse.complete(http.Response('{"error":"TOKEN_EXPIRED"}', 401));

      await expectLater(
        request,
        throwsA(
          isA<ApiException>()
              .having((error) => error.statusCode, 'statusCode', 401)
              .having(
                (error) => error.message,
                'message',
                contains('AUTH_SESSION_CHANGED'),
              ),
        ),
      );
      await Future<void>.delayed(Duration.zero);

      expect(babyRequests, 1);
      expect(refreshRequests, 0);
      expect(AuthState.instance.userId, 'account-b');
      expect(AuthState.instance.accessToken, 'access-b');
      expect(AuthState.instance.refreshToken, 'refresh-b');
    },
  );

  test(
    'delayed explicit-token blocked 403 cannot clear a replacement account',
    () async {
      final delayedResponse = Completer<http.Response>();
      final client = MockClient((request) {
        expect(request.url.path, '/api/v1/firebase/custom-token');
        return delayedResponse.future;
      });
      addTearDown(client.close);

      final request = apiPost(
        '/api/v1/firebase/custom-token',
        const {},
        token: 'access-a',
        expectedAccountId: 'account-a',
        client: client,
      );
      await Future<void>.delayed(Duration.zero);

      await AuthState.instance.setTokens(
        accessToken: 'access-b',
        refreshToken: 'refresh-b',
        userId: 'account-b',
        role: 'MOTHER',
      );
      delayedResponse.complete(
        http.Response('{"error":"ACCOUNT_DISABLED"}', 403),
      );

      await expectLater(
        request,
        throwsA(
          isA<ApiException>()
              .having((error) => error.statusCode, 'statusCode', 403)
              .having(
                (error) => error.message,
                'message',
                contains('AUTH_SESSION_CHANGED'),
              ),
        ),
      );
      await Future<void>.delayed(Duration.zero);

      expect(AuthState.instance.userId, 'account-b');
      expect(AuthState.instance.accessToken, 'access-b');
      expect(AuthState.instance.refreshToken, 'refresh-b');
    },
  );

  test('ordinary explicit-token expiry preserves the same session', () async {
    var babyRequests = 0;
    var refreshRequests = 0;
    final client = MockClient((request) async {
      if (request.url.path == '/api/v1/babies') {
        babyRequests++;
        return http.Response('{"error":"TOKEN_EXPIRED"}', 401);
      }
      if (request.url.path == '/api/v1/auth/refresh') {
        refreshRequests++;
      }
      throw StateError('Unexpected request: ${request.url.path}');
    });
    addTearDown(client.close);

    await expectLater(
      apiPost(
        '/api/v1/babies',
        const {'nickname': 'Baby Bean'},
        token: 'access-a',
        expectedAccountId: 'account-a',
        client: client,
      ),
      throwsA(
        isA<ApiException>().having(
          (error) => error.statusCode,
          'statusCode',
          401,
        ),
      ),
    );
    await Future<void>.delayed(Duration.zero);

    expect(babyRequests, 1);
    expect(refreshRequests, 0);
    expect(AuthState.instance.userId, 'account-a');
    expect(AuthState.instance.accessToken, 'access-a');
    expect(AuthState.instance.refreshToken, 'refresh-a');
  });
}
