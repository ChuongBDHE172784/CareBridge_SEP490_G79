import 'dart:async';

import 'package:firebase_auth/firebase_auth.dart' as firebase;
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/auth/models/auth_model.dart';
import 'package:untitled/features/auth/screens/phone_verification_screen.dart';
import 'package:untitled/features/auth/services/auth_service.dart';

void main() {
  const session = {
    'data': {
      'accessToken': 'access-token',
      'refreshToken': 'refresh-token',
      'user': {'id': 'user-1', 'role': 'MOTHER'},
    },
  };

  test('phone login sends Firebase proof to canonical endpoint', () async {
    final requests = <({String path, Map<String, dynamic> body})>[];
    final persisted = <AuthResponse>[];
    final service = AuthService.forTesting(
      postRequest: (path, body) async {
        requests.add((path: path, body: Map.of(body)));
        return session;
      },
      phoneAuthGateway: _FakePhoneAuthGateway(),
      tokenPersister: (auth) async => persisted.add(auth),
      postLoginAction: () async {},
    );

    await service.loginWithPhoneIdToken('fresh-phone-token');

    expect(requests.single.path, '/api/v1/auth/phone/login');
    expect(requests.single.body, {
      'idToken': 'fresh-phone-token',
      'deviceInfo': 'CareBridge Flutter',
    });
    expect(persisted, hasLength(1));
  });

  test(
    'new phone session may remain roleless for the role-selection gate',
    () async {
      final persisted = <AuthResponse>[];
      final service = AuthService.forTesting(
        postRequest: (_, _) async => {
          'data': {
            'accessToken': 'access-token',
            'refreshToken': 'refresh-token',
            'newUser': true,
            'user': {'id': 'new-user', 'role': ''},
          },
        },
        phoneAuthGateway: _FakePhoneAuthGateway(),
        tokenPersister: (auth) async => persisted.add(auth),
        postLoginAction: () async {},
      );

      final response = await service.loginWithPhoneIdToken('new-phone-token');

      expect(response.user.role, isEmpty);
      expect(persisted.single.user.role, isEmpty);
    },
  );

  test('phone registration keeps email and phone separate', () async {
    late String path;
    late Map<String, dynamic> body;
    final service = AuthService.forTesting(
      postRequest: (requestPath, requestBody) async {
        path = requestPath;
        body = Map.of(requestBody);
        return session;
      },
      phoneAuthGateway: _FakePhoneAuthGateway(),
      tokenPersister: (_) async {},
      postLoginAction: () async {},
    );

    await service.registerWithPhoneIdToken(
      idToken: 'fresh-phone-token',
      name: 'Mother Test',
      email: 'mother@example.com',
      phone: '+84912345678',
      password: 'Password@123',
      role: 'MOTHER',
    );

    expect(path, '/api/v1/auth/phone/register');
    expect(body, {
      'idToken': 'fresh-phone-token',
      'name': 'Mother Test',
      'email': 'mother@example.com',
      'phone': '+84912345678',
      'password': 'Password@123',
      'role': 'MOTHER',
      'deviceInfo': 'CareBridge Flutter',
    });
  });

  test(
    'inactive phone exchanges return response without persisting a session',
    () async {
      final persisted = <AuthResponse>[];
      var postLoginCalls = 0;
      final service = AuthService.forTesting(
        postRequest: (_, _) async => session,
        phoneAuthGateway: _FakePhoneAuthGateway(),
        tokenPersister: (auth) async => persisted.add(auth),
        postLoginAction: () async => postLoginCalls++,
      );

      final login = await service.loginWithPhoneIdToken(
        'login-token',
        shouldPersistSession: () => false,
      );
      final registration = await service.registerWithPhoneIdToken(
        idToken: 'registration-token',
        name: 'Mother Test',
        phone: '+84912345678',
        password: 'Password@123',
        shouldPersistSession: () => false,
      );

      expect(login.accessToken, 'access-token');
      expect(registration.accessToken, 'access-token');
      expect(persisted, isEmpty);
      expect(postLoginCalls, 0);
    },
  );

  test(
    'phone gateway exposes code-sent and fresh-token confirmation',
    () async {
      final gateway = _FakePhoneAuthGateway();
      final service = AuthService.forTesting(
        phoneAuthGateway: gateway,
        tokenPersister: (_) async {},
        postLoginAction: () async {},
      );
      String? verificationId;
      int? resendToken;

      await service.beginPhoneVerification(
        phoneNumber: '+84912345678',
        verificationCompleted: (_) async {},
        verificationFailed: (_) {},
        codeSent: (id, token) {
          verificationId = id;
          resendToken = token;
        },
        codeAutoRetrievalTimeout: (_) {},
      );
      final idToken = await service.confirmPhoneSmsCode(
        verificationId: verificationId!,
        smsCode: '123456',
      );

      expect(gateway.phoneNumber, '+84912345678');
      expect(verificationId, 'verification-id');
      expect(resendToken, 7);
      expect(gateway.smsCode, '123456');
      expect(idToken, 'fresh-id-token');
    },
  );

  test(
    'Firebase gateway uses web confirmation result and an opaque challenge token',
    () async {
      final confirmation = _FakeWebPhoneConfirmation('web-id-token');
      String? startedPhone;
      String? challengeToken;
      int? resendToken;
      var nativeCalls = 0;
      final gateway = FirebasePhoneAuthGateway(
        isWeb: true,
        webPhoneVerificationStarter: (phoneNumber) async {
          startedPhone = phoneNumber;
          return confirmation;
        },
        nativePhoneVerificationStarter:
            ({
              required phoneNumber,
              forceResendingToken,
              required verificationCompleted,
              required verificationFailed,
              required codeSent,
              required codeAutoRetrievalTimeout,
            }) async {
              nativeCalls++;
            },
      );

      await gateway.verifyPhoneNumber(
        phoneNumber: '+84912345678',
        verificationCompleted: (_) async {},
        verificationFailed: (error) => fail(error.toString()),
        codeSent: (token, resend) {
          challengeToken = token;
          resendToken = resend;
        },
        codeAutoRetrievalTimeout: (_) {},
      );
      final idToken = await gateway.confirmSmsCode(
        verificationId: challengeToken!,
        smsCode: '123456',
      );

      expect(startedPhone, '+84912345678');
      expect(challengeToken, startsWith('web-phone-'));
      expect(challengeToken, isNot(contains('+84912345678')));
      expect(resendToken, isNull);
      expect(nativeCalls, 0);
      expect(confirmation.smsCodes, ['123456']);
      expect(idToken, 'web-id-token');
    },
  );

  test(
    'Firebase gateway keeps prior web challenge when resend fails',
    () async {
      final confirmation = _FakeWebPhoneConfirmation('previous-id-token');
      var starts = 0;
      String? previousChallenge;
      PhoneVerificationFailure? resendFailure;
      final gateway = FirebasePhoneAuthGateway(
        isWeb: true,
        webPhoneVerificationStarter: (_) async {
          starts++;
          if (starts == 1) return confirmation;
          throw const PhoneVerificationFailure('network-request-failed');
        },
      );

      await gateway.verifyPhoneNumber(
        phoneNumber: '+84912345678',
        verificationCompleted: (_) async {},
        verificationFailed: (error) => fail(error.toString()),
        codeSent: (token, _) => previousChallenge = token,
        codeAutoRetrievalTimeout: (_) {},
      );
      await gateway.verifyPhoneNumber(
        phoneNumber: '+84912345678',
        forceResendingToken: 7,
        verificationCompleted: (_) async {},
        verificationFailed: (error) => resendFailure = error,
        codeSent: (_, _) => fail('failed resend must not replace challenge'),
        codeAutoRetrievalTimeout: (_) {},
      );

      expect(resendFailure?.code, 'network-request-failed');
      expect(
        await gateway.confirmSmsCode(
          verificationId: previousChallenge!,
          smsCode: '654321',
        ),
        'previous-id-token',
      );
      expect(confirmation.smsCodes, ['654321']);
    },
  );

  test(
    'successful web resend retires the old challenge and confirm retires the new one',
    () async {
      final confirmations = [
        _FakeWebPhoneConfirmation('old-id-token'),
        _FakeWebPhoneConfirmation('new-id-token'),
      ];
      var starts = 0;
      final challenges = <String>[];
      final gateway = FirebasePhoneAuthGateway(
        isWeb: true,
        webPhoneVerificationStarter: (_) async => confirmations[starts++],
      );

      Future<void> startChallenge() => gateway.verifyPhoneNumber(
        phoneNumber: '+84912345678',
        verificationCompleted: (_) async {},
        verificationFailed: (error) => fail(error.toString()),
        codeSent: (token, _) => challenges.add(token),
        codeAutoRetrievalTimeout: (_) {},
      );

      await startChallenge();
      await startChallenge();

      await expectLater(
        gateway.confirmSmsCode(
          verificationId: challenges.first,
          smsCode: '123456',
        ),
        throwsA(
          isA<PhoneVerificationFailure>().having(
            (error) => error.code,
            'code',
            'session-expired',
          ),
        ),
      );
      expect(
        await gateway.confirmSmsCode(
          verificationId: challenges.last,
          smsCode: '654321',
        ),
        'new-id-token',
      );
      await expectLater(
        gateway.confirmSmsCode(
          verificationId: challenges.last,
          smsCode: '654321',
        ),
        throwsA(
          isA<PhoneVerificationFailure>().having(
            (error) => error.code,
            'code',
            'session-expired',
          ),
        ),
      );
    },
  );

  test(
    'failed web confirmation keeps the active challenge for retry',
    () async {
      final confirmation = _FailOnceWebPhoneConfirmation();
      String? challenge;
      final gateway = FirebasePhoneAuthGateway(
        isWeb: true,
        webPhoneVerificationStarter: (_) async => confirmation,
      );
      await gateway.verifyPhoneNumber(
        phoneNumber: '+84912345678',
        verificationCompleted: (_) async {},
        verificationFailed: (error) => fail(error.toString()),
        codeSent: (token, _) => challenge = token,
        codeAutoRetrievalTimeout: (_) {},
      );

      await expectLater(
        gateway.confirmSmsCode(verificationId: challenge!, smsCode: '000000'),
        throwsA(
          isA<PhoneVerificationFailure>().having(
            (error) => error.code,
            'code',
            'invalid-verification-code',
          ),
        ),
      );
      expect(
        await gateway.confirmSmsCode(
          verificationId: challenge!,
          smsCode: '123456',
        ),
        'retried-id-token',
      );
    },
  );

  test(
    'Firebase gateway keeps native verifyPhoneNumber callback flow',
    () async {
      String? nativePhone;
      int? nativeResendToken;
      String? verificationId;
      final gateway = FirebasePhoneAuthGateway(
        isWeb: false,
        webPhoneVerificationStarter: (_) async =>
            throw StateError('web flow must not run'),
        nativePhoneVerificationStarter:
            ({
              required phoneNumber,
              forceResendingToken,
              required verificationCompleted,
              required verificationFailed,
              required codeSent,
              required codeAutoRetrievalTimeout,
            }) async {
              nativePhone = phoneNumber;
              nativeResendToken = forceResendingToken;
              codeSent('native-verification-id', 11);
            },
      );

      await gateway.verifyPhoneNumber(
        phoneNumber: '+84912345678',
        forceResendingToken: 9,
        verificationCompleted: (_) async {},
        verificationFailed: (error) => fail(error.toString()),
        codeSent: (id, _) => verificationId = id,
        codeAutoRetrievalTimeout: (_) {},
      );

      expect(nativePhone, '+84912345678');
      expect(nativeResendToken, 9);
      expect(verificationId, 'native-verification-id');
    },
  );

  test(
    'out-of-order web start completion cannot replace the newest challenge',
    () async {
      final firstStart = Completer<WebPhoneConfirmation>();
      final secondStart = Completer<WebPhoneConfirmation>();
      final newestConfirmation = _FakeWebPhoneConfirmation('newest-id-token');
      var starts = 0;
      final challenges = <String>[];
      final gateway = FirebasePhoneAuthGateway(
        isWeb: true,
        webPhoneVerificationStarter: (_) {
          starts++;
          return starts == 1 ? firstStart.future : secondStart.future;
        },
      );

      Future<void> start() => gateway.verifyPhoneNumber(
        phoneNumber: '+84912345678',
        verificationCompleted: (_) async {},
        verificationFailed: (error) => fail(error.toString()),
        codeSent: (token, _) => challenges.add(token),
        codeAutoRetrievalTimeout: (_) {},
      );

      final staleStart = start();
      final newestStart = start();
      secondStart.complete(newestConfirmation);
      await newestStart;
      firstStart.complete(_FakeWebPhoneConfirmation('stale-id-token'));
      await staleStart;

      expect(challenges, ['web-phone-2']);
      expect(
        await gateway.confirmSmsCode(
          verificationId: challenges.single,
          smsCode: '123456',
        ),
        'newest-id-token',
      );
      expect(newestConfirmation.smsCodes, ['123456']);
    },
  );

  test('native starter maps an immediate Firebase error code', () async {
    PhoneVerificationFailure? failure;
    final gateway = FirebasePhoneAuthGateway(
      isWeb: false,
      nativePhoneVerificationStarter:
          ({
            required phoneNumber,
            forceResendingToken,
            required verificationCompleted,
            required verificationFailed,
            required codeSent,
            required codeAutoRetrievalTimeout,
          }) async {
            throw firebase.FirebaseAuthException(
              code: 'invalid-app-credential',
            );
          },
    );

    await gateway.verifyPhoneNumber(
      phoneNumber: '+84912345678',
      verificationCompleted: (_) async {},
      verificationFailed: (error) => failure = error,
      codeSent: (_, _) => fail('codeSent must not run'),
      codeAutoRetrievalTimeout: (_) {},
    );

    expect(failure?.code, 'invalid-app-credential');
  });

  testWidgets(
    'native setup returning without callbacks keeps SMS request pending',
    (tester) async {
      final service = AuthService.forTesting(
        phoneAuthGateway: _ImmediateReturnPhoneAuthGateway(),
        tokenPersister: (_) async {},
        postLoginAction: () async {},
      );
      await tester.pumpWidget(
        MaterialApp(
          home: PhoneVerificationScreen.login(
            phoneNumber: '+84912345678',
            authService: service,
          ),
        ),
      );
      await tester.pump();
      await tester.pump();

      expect(find.textContaining('Đang gửi mã 6 số'), findsOneWidget);
      expect(
        tester
            .widget<TextButton>(find.byKey(const Key('phone-sms-resend')))
            .onPressed,
        isNull,
      );
    },
  );

  testWidgets('late native completion after dispose does not call backend', (
    tester,
  ) async {
    final gateway = _LateCompletionPhoneAuthGateway();
    var backendCalls = 0;
    final service = AuthService.forTesting(
      postRequest: (_, _) async {
        backendCalls++;
        return session;
      },
      phoneAuthGateway: gateway,
      tokenPersister: (_) async {},
      postLoginAction: () async {},
    );
    await tester.pumpWidget(
      MaterialApp(
        home: PhoneVerificationScreen.login(
          phoneNumber: '+84912345678',
          authService: service,
        ),
      ),
    );
    await tester.pump();
    await tester.pumpWidget(const MaterialApp(home: SizedBox()));

    await gateway.completeVerification('late-id-token');
    await tester.pump();

    expect(backendCalls, 0);
  });

  testWidgets('back is blocked while backend session exchange is in flight', (
    tester,
  ) async {
    final backendResponse = Completer<dynamic>();
    final service = AuthService.forTesting(
      postRequest: (_, _) => backendResponse.future,
      phoneAuthGateway: _CountingConfirmationPhoneAuthGateway(),
      tokenPersister: (_) async {},
      postLoginAction: () async {},
    );
    await tester.pumpWidget(
      MaterialApp(
        home: Builder(
          builder: (context) => Scaffold(
            body: ElevatedButton(
              key: const Key('open-phone-verification'),
              onPressed: () => Navigator.push<void>(
                context,
                MaterialPageRoute(
                  builder: (_) => PhoneVerificationScreen.login(
                    phoneNumber: '+84912345678',
                    authService: service,
                  ),
                ),
              ),
              child: const Text('Open verification'),
            ),
          ),
        ),
      ),
    );
    await tester.tap(find.byKey(const Key('open-phone-verification')));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('phone-sms-code')), '123456');
    await tester.pump();
    await tester.pump();

    await tester.tap(find.byTooltip('Quay lại'));
    await tester.pump();
    expect(find.byKey(const Key('phone-sms-code')), findsOneWidget);
    await tester.binding.handlePopRoute();
    await tester.pump();
    expect(find.byKey(const Key('phone-sms-code')), findsOneWidget);

    backendResponse.completeError(Exception('backend unavailable'));
    await tester.pump();
    await tester.pump();
    await tester.binding.handlePopRoute();
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('open-phone-verification')), findsOneWidget);
  });

  testWidgets('screen waits for codeSent before claiming SMS was sent', (
    tester,
  ) async {
    final gateway = _DelayedPhoneAuthGateway();
    final service = AuthService.forTesting(
      phoneAuthGateway: gateway,
      tokenPersister: (_) async {},
      postLoginAction: () async {},
    );

    await tester.pumpWidget(
      MaterialApp(
        home: PhoneVerificationScreen.login(
          phoneNumber: '+84912345678',
          authService: service,
        ),
      ),
    );
    await tester.pump();

    expect(find.textContaining('đã được gửi'), findsNothing);
    expect(find.textContaining('Đang gửi mã 6 số'), findsOneWidget);
    expect(
      tester.widget<TextField>(find.byKey(const Key('phone-sms-code'))).enabled,
      isFalse,
    );

    gateway.completeCodeSent();
    await tester.pump();

    expect(find.textContaining('đã được gửi'), findsOneWidget);
    expect(
      tester.widget<TextField>(find.byKey(const Key('phone-sms-code'))).enabled,
      isTrue,
    );
  });

  testWidgets('screen logs only the safe Firebase failure code', (
    tester,
  ) async {
    final previousDebugPrint = debugPrint;
    final logs = <String>[];
    debugPrint = (message, {wrapWidth}) {
      if (message != null) logs.add(message);
    };
    try {
      final service = AuthService.forTesting(
        phoneAuthGateway: _ImmediateFailurePhoneAuthGateway(),
        tokenPersister: (_) async {},
        postLoginAction: () async {},
      );
      await tester.pumpWidget(
        MaterialApp(
          home: PhoneVerificationScreen.login(
            phoneNumber: '+84912345678',
            authService: service,
          ),
        ),
      );
      await tester.pump();

      expect(logs, [
        'Firebase phone verification failed: code=invalid-app-credential',
      ]);
      expect(logs.single, isNot(contains('+84912345678')));
      expect(logs.single, isNot(contains('123456')));
      expect(logs.single, isNot(contains('id-token')));
    } finally {
      debugPrint = previousDebugPrint;
    }
  });

  testWidgets('backend retry reuses the confirmed Firebase ID token', (
    tester,
  ) async {
    final gateway = _CountingConfirmationPhoneAuthGateway();
    var backendCalls = 0;
    final service = AuthService.forTesting(
      postRequest: (_, _) async {
        backendCalls++;
        throw Exception('backend unavailable');
      },
      phoneAuthGateway: gateway,
      tokenPersister: (_) async {},
      postLoginAction: () async {},
    );
    await tester.pumpWidget(
      MaterialApp(
        home: PhoneVerificationScreen.login(
          phoneNumber: '+84912345678',
          authService: service,
        ),
      ),
    );
    await tester.pump();

    await tester.enterText(find.byKey(const Key('phone-sms-code')), '123456');
    await tester.pump();
    await tester.pump();
    expect(gateway.confirmCalls, 1);
    expect(backendCalls, 1);

    await tester.tap(find.byKey(const Key('phone-sms-submit')));
    await tester.pump();
    await tester.pump();

    expect(gateway.confirmCalls, 1);
    expect(backendCalls, 2);
  });

  testWidgets(
    'native auto-verification before codeSent keeps token and enables backend retry',
    (tester) async {
      final gateway = _NativeAutoVerificationPhoneAuthGateway();
      final backendTokens = <String>[];
      final service = AuthService.forTesting(
        postRequest: (_, body) async {
          backendTokens.add(body['idToken'] as String);
          throw Exception('backend unavailable');
        },
        phoneAuthGateway: gateway,
        tokenPersister: (_) async {},
        postLoginAction: () async {},
      );
      await tester.pumpWidget(
        MaterialApp(
          home: PhoneVerificationScreen.login(
            phoneNumber: '+84912345678',
            authService: service,
          ),
        ),
      );
      await tester.pump();
      await tester.pump();

      expect(backendTokens, ['native-auto-id-token']);
      expect(gateway.confirmCalls, 0);

      await tester.tap(find.byKey(const Key('phone-sms-submit')));
      await tester.pump();
      await tester.pump();

      expect(backendTokens, ['native-auto-id-token', 'native-auto-id-token']);
      expect(gateway.confirmCalls, 0);
    },
  );

  testWidgets(
    'successful resend clears cached proof and confirms the new challenge',
    (tester) async {
      final gateway = _SuccessfulResendPhoneAuthGateway();
      final backendTokens = <String>[];
      final service = AuthService.forTesting(
        postRequest: (_, body) async {
          backendTokens.add(body['idToken'] as String);
          throw Exception('backend unavailable');
        },
        phoneAuthGateway: gateway,
        tokenPersister: (_) async {},
        postLoginAction: () async {},
      );
      await tester.pumpWidget(
        MaterialApp(
          home: PhoneVerificationScreen.login(
            phoneNumber: '+84912345678',
            authService: service,
          ),
        ),
      );
      await tester.pump();

      await tester.enterText(find.byKey(const Key('phone-sms-code')), '111111');
      await tester.pump();
      await tester.pump();
      expect(gateway.confirmedVerificationIds, ['initial-verification-id']);
      expect(backendTokens, ['initial-id-token']);

      await tester.pump(const Duration(seconds: 61));
      await tester.tap(find.byKey(const Key('phone-sms-resend')));
      await tester.pump();
      await tester.enterText(find.byKey(const Key('phone-sms-code')), '222222');
      await tester.pump();
      await tester.pump();

      expect(gateway.confirmedVerificationIds, [
        'initial-verification-id',
        'resent-verification-id',
      ]);
      expect(gateway.smsCodes, ['111111', '222222']);
      expect(backendTokens, ['initial-id-token', 'resent-id-token']);
    },
  );

  testWidgets('failed resend preserves the previous Firebase challenge', (
    tester,
  ) async {
    final gateway = _ResendFailurePhoneAuthGateway();
    final service = AuthService.forTesting(
      phoneAuthGateway: gateway,
      tokenPersister: (_) async {},
      postLoginAction: () async {},
    );

    await tester.pumpWidget(
      MaterialApp(
        home: PhoneVerificationScreen.login(
          phoneNumber: '+84912345678',
          authService: service,
        ),
      ),
    );
    await tester.pump();
    await tester.pump(const Duration(seconds: 61));

    await tester.tap(find.byKey(const Key('phone-sms-resend')));
    await tester.pump();
    await tester.enterText(find.byKey(const Key('phone-sms-code')), '123456');
    await tester.pump();

    expect(gateway.confirmedVerificationId, 'initial-verification-id');
  });
}

class _FakePhoneAuthGateway implements PhoneAuthGateway {
  String? phoneNumber;
  String? smsCode;

  @override
  Future<void> verifyPhoneNumber({
    required String phoneNumber,
    int? forceResendingToken,
    required Future<void> Function(String idToken) verificationCompleted,
    required void Function(PhoneVerificationFailure error) verificationFailed,
    required void Function(String verificationId, int? resendToken) codeSent,
    required void Function(String verificationId) codeAutoRetrievalTimeout,
  }) async {
    this.phoneNumber = phoneNumber;
    codeSent('verification-id', 7);
  }

  @override
  Future<String> confirmSmsCode({
    required String verificationId,
    required String smsCode,
  }) async {
    this.smsCode = smsCode;
    return 'fresh-id-token';
  }
}

class _FakeWebPhoneConfirmation implements WebPhoneConfirmation {
  _FakeWebPhoneConfirmation(this.idToken);

  final String idToken;
  final smsCodes = <String>[];

  @override
  Future<String> confirmSmsCode(String smsCode) async {
    smsCodes.add(smsCode);
    return idToken;
  }
}

class _FailOnceWebPhoneConfirmation implements WebPhoneConfirmation {
  var attempts = 0;

  @override
  Future<String> confirmSmsCode(String smsCode) async {
    attempts++;
    if (attempts == 1) {
      throw const PhoneVerificationFailure('invalid-verification-code');
    }
    return 'retried-id-token';
  }
}

class _DelayedPhoneAuthGateway implements PhoneAuthGateway {
  void Function(String, int?)? _codeSent;
  final _started = Completer<void>();

  void completeCodeSent() {
    _codeSent?.call('delayed-verification-id', null);
    if (!_started.isCompleted) _started.complete();
  }

  @override
  Future<void> verifyPhoneNumber({
    required String phoneNumber,
    int? forceResendingToken,
    required Future<void> Function(String idToken) verificationCompleted,
    required void Function(PhoneVerificationFailure error) verificationFailed,
    required void Function(String verificationId, int? resendToken) codeSent,
    required void Function(String verificationId) codeAutoRetrievalTimeout,
  }) {
    _codeSent = codeSent;
    return _started.future;
  }

  @override
  Future<String> confirmSmsCode({
    required String verificationId,
    required String smsCode,
  }) async => 'unused';
}

class _ImmediateReturnPhoneAuthGateway implements PhoneAuthGateway {
  @override
  Future<void> verifyPhoneNumber({
    required String phoneNumber,
    int? forceResendingToken,
    required Future<void> Function(String idToken) verificationCompleted,
    required void Function(PhoneVerificationFailure error) verificationFailed,
    required void Function(String verificationId, int? resendToken) codeSent,
    required void Function(String verificationId) codeAutoRetrievalTimeout,
  }) async {}

  @override
  Future<String> confirmSmsCode({
    required String verificationId,
    required String smsCode,
  }) async => 'unused';
}

class _LateCompletionPhoneAuthGateway implements PhoneAuthGateway {
  Future<void> Function(String)? _verificationCompleted;

  Future<void> completeVerification(String idToken) async {
    final callback = _verificationCompleted;
    if (callback != null) await callback(idToken);
  }

  @override
  Future<void> verifyPhoneNumber({
    required String phoneNumber,
    int? forceResendingToken,
    required Future<void> Function(String idToken) verificationCompleted,
    required void Function(PhoneVerificationFailure error) verificationFailed,
    required void Function(String verificationId, int? resendToken) codeSent,
    required void Function(String verificationId) codeAutoRetrievalTimeout,
  }) async {
    _verificationCompleted = verificationCompleted;
  }

  @override
  Future<String> confirmSmsCode({
    required String verificationId,
    required String smsCode,
  }) async => 'unused';
}

class _ImmediateFailurePhoneAuthGateway implements PhoneAuthGateway {
  @override
  Future<void> verifyPhoneNumber({
    required String phoneNumber,
    int? forceResendingToken,
    required Future<void> Function(String idToken) verificationCompleted,
    required void Function(PhoneVerificationFailure error) verificationFailed,
    required void Function(String verificationId, int? resendToken) codeSent,
    required void Function(String verificationId) codeAutoRetrievalTimeout,
  }) async {
    verificationFailed(
      const PhoneVerificationFailure('invalid-app-credential'),
    );
  }

  @override
  Future<String> confirmSmsCode({
    required String verificationId,
    required String smsCode,
  }) async => 'unused';
}

class _CountingConfirmationPhoneAuthGateway implements PhoneAuthGateway {
  var confirmCalls = 0;

  @override
  Future<void> verifyPhoneNumber({
    required String phoneNumber,
    int? forceResendingToken,
    required Future<void> Function(String idToken) verificationCompleted,
    required void Function(PhoneVerificationFailure error) verificationFailed,
    required void Function(String verificationId, int? resendToken) codeSent,
    required void Function(String verificationId) codeAutoRetrievalTimeout,
  }) async {
    codeSent('verification-id', null);
  }

  @override
  Future<String> confirmSmsCode({
    required String verificationId,
    required String smsCode,
  }) async {
    confirmCalls++;
    return 'confirmed-id-token';
  }
}

class _NativeAutoVerificationPhoneAuthGateway implements PhoneAuthGateway {
  var confirmCalls = 0;

  @override
  Future<void> verifyPhoneNumber({
    required String phoneNumber,
    int? forceResendingToken,
    required Future<void> Function(String idToken) verificationCompleted,
    required void Function(PhoneVerificationFailure error) verificationFailed,
    required void Function(String verificationId, int? resendToken) codeSent,
    required void Function(String verificationId) codeAutoRetrievalTimeout,
  }) async {
    await verificationCompleted('native-auto-id-token');
  }

  @override
  Future<String> confirmSmsCode({
    required String verificationId,
    required String smsCode,
  }) async {
    confirmCalls++;
    return 'unexpected-confirmation-token';
  }
}

class _SuccessfulResendPhoneAuthGateway implements PhoneAuthGateway {
  var starts = 0;
  final confirmedVerificationIds = <String>[];
  final smsCodes = <String>[];

  @override
  Future<void> verifyPhoneNumber({
    required String phoneNumber,
    int? forceResendingToken,
    required Future<void> Function(String idToken) verificationCompleted,
    required void Function(PhoneVerificationFailure error) verificationFailed,
    required void Function(String verificationId, int? resendToken) codeSent,
    required void Function(String verificationId) codeAutoRetrievalTimeout,
  }) async {
    starts++;
    codeSent(
      starts == 1 ? 'initial-verification-id' : 'resent-verification-id',
      starts,
    );
  }

  @override
  Future<String> confirmSmsCode({
    required String verificationId,
    required String smsCode,
  }) async {
    confirmedVerificationIds.add(verificationId);
    smsCodes.add(smsCode);
    return verificationId == 'initial-verification-id'
        ? 'initial-id-token'
        : 'resent-id-token';
  }
}

class _ResendFailurePhoneAuthGateway implements PhoneAuthGateway {
  var calls = 0;
  String? confirmedVerificationId;

  @override
  Future<void> verifyPhoneNumber({
    required String phoneNumber,
    int? forceResendingToken,
    required Future<void> Function(String idToken) verificationCompleted,
    required void Function(PhoneVerificationFailure error) verificationFailed,
    required void Function(String verificationId, int? resendToken) codeSent,
    required void Function(String verificationId) codeAutoRetrievalTimeout,
  }) async {
    calls++;
    if (calls == 1) {
      codeSent('initial-verification-id', 7);
    } else {
      verificationFailed(
        const PhoneVerificationFailure('network-request-failed'),
      );
    }
  }

  @override
  Future<String> confirmSmsCode({
    required String verificationId,
    required String smsCode,
  }) async {
    confirmedVerificationId = verificationId;
    throw const PhoneVerificationFailure('invalid-verification-code');
  }
}
