import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/auth/services/auth_service.dart';

void main() {
  test(
    'expert registration sends EXPERT role in the register request',
    () async {
      String? path;
      Map<String, dynamic>? body;
      final service = AuthService.forTesting(
        postRequest: (requestPath, requestBody) async {
          path = requestPath;
          body = requestBody;
          return {
            'data': {
              'message': 'OTP sent',
              'expiresIn': 60,
              'userId': 'expert-1',
              'otpExpiresAt': null,
              'auth': null,
            },
          };
        },
        googleIdTokenProvider: () async => 'unused',
        tokenPersister: (_) async {},
        postLoginAction: () async {},
      );

      await service.register(
        name: 'Dr Test',
        email: 'expert@example.com',
        password: 'Password@1',
        role: 'EXPERT',
      );

      expect(path, '/api/v1/auth/register');
      expect(body?['role'], 'EXPERT');
      expect(body?['email'], 'expert@example.com');
    },
  );
}
