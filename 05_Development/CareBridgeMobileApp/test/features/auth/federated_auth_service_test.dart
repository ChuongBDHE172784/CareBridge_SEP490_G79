import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/network/api_client.dart';
import 'package:untitled/features/auth/models/auth_model.dart';
import 'package:untitled/features/auth/models/federated_auth_failure.dart';
import 'package:untitled/features/auth/services/auth_service.dart';

void main() {
  group('GoogleIdTokenAcquirer', () {
    test('uses the Web popup provider only on Web', () async {
      var webCalls = 0;
      var nativeCalls = 0;
      final acquirer = GoogleIdTokenAcquirer(
        isWeb: true,
        webProvider: () async {
          webCalls++;
          return 'web-firebase-token';
        },
        nativeProvider: () async {
          nativeCalls++;
          return 'native-firebase-token';
        },
      );

      expect(await acquirer.acquire(), 'web-firebase-token');
      expect(webCalls, 1);
      expect(nativeCalls, 0);
    });

    test('preserves the native Google credential path off Web', () async {
      var webCalls = 0;
      var nativeCalls = 0;
      final acquirer = GoogleIdTokenAcquirer(
        isWeb: false,
        platform: TargetPlatform.android,
        webProvider: () async {
          webCalls++;
          return 'web-firebase-token';
        },
        nativeProvider: () async {
          nativeCalls++;
          return 'native-firebase-token';
        },
      );

      expect(await acquirer.acquire(), 'native-firebase-token');
      expect(webCalls, 0);
      expect(nativeCalls, 1);
    });

    test('rejects unsupported desktop without invoking a provider', () {
      var webCalls = 0;
      var nativeCalls = 0;
      final acquirer = GoogleIdTokenAcquirer(
        isWeb: false,
        platform: TargetPlatform.windows,
        webProvider: () async {
          webCalls++;
          return 'web-firebase-token';
        },
        nativeProvider: () async {
          nativeCalls++;
          return 'native-firebase-token';
        },
      );

      expect(
        () => acquirer.acquire(),
        throwsA(
          isA<FederatedSignInException>().having(
            (error) => error.failure.kind,
            'failure kind',
            FederatedAuthFailureKind.configuration,
          ),
        ),
      );
      expect(webCalls, 0);
      expect(nativeCalls, 0);
    });
  });

  group('AuthService Google handoff', () {
    test(
      'posts one acquired Firebase token and persists backend tokens',
      () async {
        var providerCalls = 0;
        var postCalls = 0;
        AuthResponse? persisted;
        final service = AuthService.forTesting(
          googleIdTokenProvider: () async {
            providerCalls++;
            return 'fresh-firebase-id-token';
          },
          postRequest: (path, body) async {
            postCalls++;
            expect(path, '/api/v1/auth/federated');
            expect(body, {
              'idToken': 'fresh-firebase-id-token',
              'deviceInfo': 'CareBridge Flutter',
            });
            return _authResponseJson(role: 'MOTHER');
          },
          tokenPersister: (auth) async => persisted = auth,
        );

        final response = await service.federatedGoogle();

        expect(providerCalls, 1);
        expect(postCalls, 1);
        expect(response.user.role, 'MOTHER');
        expect(persisted, same(response));
      },
    );

    test('cancellation never calls backend or persists a session', () async {
      var postCalls = 0;
      var persistCalls = 0;
      final service = AuthService.forTesting(
        googleIdTokenProvider: () async =>
            throw const FederatedSignInException(FederatedAuthFailure.canceled),
        postRequest: (_, _) async {
          postCalls++;
          return _authResponseJson();
        },
        tokenPersister: (_) async => persistCalls++,
      );

      await expectLater(
        service.federatedGoogle(),
        throwsA(
          isA<FederatedSignInException>().having(
            (error) => error.failure.kind,
            'failure kind',
            FederatedAuthFailureKind.canceled,
          ),
        ),
      );
      expect(postCalls, 0);
      expect(persistCalls, 0);
    });

    test('backend rejection does not persist a partial session', () async {
      var persistCalls = 0;
      final service = AuthService.forTesting(
        googleIdTokenProvider: () async => 'fresh-firebase-id-token',
        postRequest: (_, _) async => throw ApiException(401, 'raw response'),
        tokenPersister: (_) async => persistCalls++,
      );

      await expectLater(
        service.federatedGoogle(),
        throwsA(
          isA<FederatedSignInException>().having(
            (error) => error.failure.kind,
            'failure kind',
            FederatedAuthFailureKind.invalidCredential,
          ),
        ),
      );
      expect(persistCalls, 0);
    });

    test(
      'incomplete success response does not persist a partial session',
      () async {
        var persistCalls = 0;
        final service = AuthService.forTesting(
          googleIdTokenProvider: () async => 'fresh-firebase-id-token',
          postRequest: (_, _) async => {
            'data': {
              'accessToken': '   ',
              'refreshToken': 'carebridge-refresh-token',
              'user': {'id': 'user-1', 'role': 'MOTHER'},
            },
          },
          tokenPersister: (_) async => persistCalls++,
        );

        await expectLater(
          service.federatedGoogle(),
          throwsA(
            isA<FederatedSignInException>().having(
              (error) => error.failure.kind,
              'failure kind',
              FederatedAuthFailureKind.unexpected,
            ),
          ),
        );
        expect(persistCalls, 0);
      },
    );
  });
}

Map<String, dynamic> _authResponseJson({String role = 'UNASSIGNED'}) {
  return {
    'data': {
      'accessToken': 'carebridge-access-token',
      'refreshToken': 'carebridge-refresh-token',
      'user': {'id': 'user-1', 'role': role},
    },
  };
}
