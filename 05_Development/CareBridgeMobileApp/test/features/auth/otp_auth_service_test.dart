import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/auth/models/auth_model.dart';
import 'package:untitled/features/auth/services/auth_service.dart';

void main() {
  test(
    'email OTP uses the injected gateway and honors persistence guard',
    () async {
      final requests = <({String path, Map<String, dynamic> body})>[];
      final persisted = <AuthResponse>[];
      final service = AuthService.forTesting(
        postRequest: (path, body) async {
          requests.add((path: path, body: Map.of(body)));
          return {
            'data': {
              'accessToken': 'access-token',
              'refreshToken': 'refresh-token',
              'user': {'id': 'user-1', 'role': ''},
            },
          };
        },
        tokenPersister: (auth) async => persisted.add(auth),
        postLoginAction: () async {},
      );

      final response = await service.verifyOtp(
        email: 'mother@example.com',
        otp: '123456',
        shouldPersistSession: () => false,
      );

      expect(response.user.id, 'user-1');
      expect(requests.single.path, '/api/v1/auth/verify-otp');
      expect(requests.single.body, {
        'email': 'mother@example.com',
        'otp': '123456',
      });
      expect(persisted, isEmpty);
    },
  );

  test('email OTP resend uses the injected gateway', () async {
    String? path;
    Map<String, dynamic>? body;
    final service = AuthService.forTesting(
      postRequest: (requestPath, requestBody) async {
        path = requestPath;
        body = Map.of(requestBody);
        return const <String, dynamic>{};
      },
      tokenPersister: (_) async {},
      postLoginAction: () async {},
    );

    await service.resendOtp(email: 'mother@example.com');

    expect(path, '/api/v1/auth/resend-otp');
    expect(body, {'email': 'mother@example.com'});
  });
}
