import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/network/api_client.dart';
import 'package:untitled/features/directChat/services/firebase_token_service.dart';

({String? userId, String? accessToken}) _session() =>
    (userId: 'user-1', accessToken: 'access-token-1');

FirebaseTokenService _service(FirebaseTokenRequest request) =>
    FirebaseTokenService(request: request, sessionLoader: _session);

void main() {
  test('parses the explicit disabled capability without a token', () async {
    String? requestedPath;
    Map<String, dynamic>? requestedBody;
    String? requestedToken;
    String? requestedAccountId;
    final service = _service((path, body, {token, expectedAccountId}) async {
      requestedPath = path;
      requestedBody = body;
      requestedToken = token;
      requestedAccountId = expectedAccountId;
      return {
        'data': {
          'firestoreSignalingEnabled': false,
          'firebaseCustomToken': null,
        },
      };
    });

    final capability = await service.fetchCapability();

    expect(requestedPath, '/api/v1/firebase/custom-token');
    expect(requestedBody, isEmpty);
    expect(requestedToken, 'access-token-1');
    expect(requestedAccountId, 'user-1');
    expect(capability.firestoreSignalingEnabled, isFalse);
    expect(capability.firebaseCustomToken, isNull);
  });

  test('parses an enabled capability with a nonblank token', () async {
    final service = _service(
      (_, _, {token, expectedAccountId}) async => {
        'data': {
          'firestoreSignalingEnabled': true,
          'firebaseCustomToken': ' custom-token ',
        },
      },
    );

    final capability = await service.fetchCapability();

    expect(capability.firestoreSignalingEnabled, isTrue);
    expect(capability.firebaseCustomToken, 'custom-token');
  });

  test('normalizes DCC-012 to a terminal signaling failure', () async {
    final service = _service(
      (_, _, {token, expectedAccountId}) async =>
          throw ApiException(503, '{"success":false,"error":"DCC-012"}'),
    );

    await expectLater(
      service.fetchCapability(),
      throwsA(isA<FirebaseSignalingTerminalException>()),
    );
  });

  test('preserves non-terminal API failures for reconnect handling', () async {
    final transient = ApiException(503, '{"error":"TEMPORARY"}');
    final service = _service(
      (_, _, {token, expectedAccountId}) async => throw transient,
    );

    await expectLater(service.fetchCapability(), throwsA(same(transient)));
  });

  for (final token in <Object?>[null, '', '   ']) {
    test('enabled response with token $token is terminal', () async {
      final service = _service(
        (_, _, {token, expectedAccountId}) async => {
          'data': {
            'firestoreSignalingEnabled': true,
            'firebaseCustomToken': token,
          },
        },
      );

      await expectLater(
        service.fetchCapability(),
        throwsA(isA<FirebaseSignalingTerminalException>()),
      );
    });
  }

  test('missing capability flag is terminal', () async {
    final service = _service(
      (_, _, {token, expectedAccountId}) async => {
        'data': {'firebaseCustomToken': 'token'},
      },
    );

    await expectLater(
      service.fetchCapability(),
      throwsA(isA<FirebaseSignalingTerminalException>()),
    );
  });

  test(
    'normalizes a replaced account response to a terminal failure',
    () async {
      final service = _service(
        (_, _, {token, expectedAccountId}) async =>
            throw ApiException(401, '{"error":"AUTH_SESSION_CHANGED"}'),
      );

      await expectLater(
        service.fetchCapability(),
        throwsA(isA<FirebaseSignalingTerminalException>()),
      );
    },
  );

  for (final token in <Object?>['unexpected-token', null]) {
    test(
      'disabled response with inconsistent token $token is terminal',
      () async {
        final data = <String, dynamic>{'firestoreSignalingEnabled': false};
        if (token != null) data['firebaseCustomToken'] = token;
        final service = _service(
          (_, _, {token, expectedAccountId}) async => {'data': data},
        );

        await expectLater(
          service.fetchCapability(),
          throwsA(isA<FirebaseSignalingTerminalException>()),
        );
      },
    );
  }
}
