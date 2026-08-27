import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/auth/models/registration_draft.dart';
import 'package:untitled/features/auth/screens/phone_verification_screen.dart';
import 'package:untitled/features/auth/screens/registration_verification_method_screen.dart';
import 'package:untitled/features/auth/services/auth_service.dart';

void main() {
  const draft = RegistrationDraft(
    name: 'Mother Test',
    email: 'mother@example.com',
    phone: '+84912345678',
    password: 'Password@123',
  );

  testWidgets('SMS choice starts Firebase only after explicit confirmation', (
    tester,
  ) async {
    final gateway = _RecordingPhoneGateway();
    var backendCalls = 0;
    final service = AuthService.forTesting(
      postRequest: (_, _) async {
        backendCalls++;
        throw StateError('backend must not run before Firebase proof');
      },
      phoneAuthGateway: gateway,
      tokenPersister: (_) async {},
      postLoginAction: () async {},
    );

    await tester.pumpWidget(
      MaterialApp(
        home: RegistrationVerificationMethodScreen(
          draft: draft,
          authService: service,
        ),
      ),
    );

    expect(gateway.startCalls, 0);
    expect(backendCalls, 0);

    await tester.tap(find.text('SMS'));
    await tester.pump();
    expect(gateway.startCalls, 0);

    await tester.tap(
      find.byKey(const Key('registration-verification-continue')),
    );
    await tester.pump();
    await tester.pump();

    expect(find.byType(PhoneVerificationScreen), findsOneWidget);
    expect(gateway.startCalls, 1);
    expect(backendCalls, 0);

    await tester.pumpWidget(const SizedBox.shrink());
    await tester.pump();
  });
}

class _RecordingPhoneGateway implements PhoneAuthGateway {
  int startCalls = 0;

  @override
  Future<void> verifyPhoneNumber({
    required String phoneNumber,
    int? forceResendingToken,
    required Future<void> Function(String idToken) verificationCompleted,
    required void Function(PhoneVerificationFailure error) verificationFailed,
    required void Function(String verificationId, int? resendToken) codeSent,
    required void Function(String verificationId) codeAutoRetrievalTimeout,
  }) async {
    startCalls++;
    codeSent('verification-id', 7);
  }

  @override
  Future<String> confirmSmsCode({
    required String verificationId,
    required String smsCode,
  }) async => 'unused-token';
}
