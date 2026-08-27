import 'package:flutter_test/flutter_test.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/features/auth/models/linked_account.dart';
import 'package:untitled/features/auth/services/auth_service.dart';

void main() {
  group('LinkedAccount', () {
    test('parses an unlinked Google identity', () {
      final account = LinkedAccount.fromJson({
        'provider': 'GOOGLE',
        'linked': false,
      });

      expect(account.provider, 'GOOGLE');
      expect(account.linked, isFalse);
      expect(account.email, isNull);
      expect(account.linkedAt, isNull);
    });

    test('parses a linked Google identity', () {
      final account = LinkedAccount.fromJson({
        'provider': 'GOOGLE',
        'linked': true,
        'email': 'member@example.com',
        'linkedAt': '2026-07-17T08:00:00Z',
      });

      expect(account.linked, isTrue);
      expect(account.email, 'member@example.com');
      expect(account.linkedAt, DateTime.utc(2026, 7, 17, 8));
    });
  });

  test('maps a raw Google picker cancellation to a silent link failure', () {
    final failure = LinkedAccountFailure.from(
      const GoogleSignInException(code: GoogleSignInExceptionCode.canceled),
    );

    expect(failure, same(LinkedAccountFailure.canceled));
    expect(failure.userMessage, isEmpty);
  });

  test(
    'linkGoogleAccount posts fresh proof without replacing app tokens',
    () async {
      final authState = AuthState.instance;
      final accessBefore = authState.accessToken;
      final refreshBefore = authState.refreshToken;
      String? requestedPath;
      Map<String, dynamic>? requestedBody;

      var sharedAcquirerCalls = 0;
      final service = AuthService.forTesting(
        googleIdTokenAcquirer: GoogleIdTokenAcquirer(
          isWeb: true,
          webProvider: () async {
            sharedAcquirerCalls++;
            return 'fresh-firebase-token';
          },
          nativeProvider: () async => throw StateError(
            'Native Google provider must not run for the Web link flow',
          ),
        ),
        postRequest: (path, body) async {
          requestedPath = path;
          requestedBody = body;
          return {
            'data': {
              'provider': 'GOOGLE',
              'linked': true,
              'email': 'member@example.com',
            },
          };
        },
      );

      final result = await service.linkGoogleAccount();

      expect(requestedPath, '/api/v1/auth/identities/google');
      expect(requestedBody, {'idToken': 'fresh-firebase-token'});
      expect(sharedAcquirerCalls, 1);
      expect(result.linked, isTrue);
      expect(authState.accessToken, accessBefore);
      expect(authState.refreshToken, refreshBefore);
    },
  );

  test('getLinkedGoogleAccount rejects a response without data', () async {
    final service = AuthService.forTesting(getRequest: (_) async => {});

    await expectLater(
      service.getLinkedGoogleAccount(),
      throwsA(isA<FormatException>()),
    );
  });

  test(
    'linkGoogleAccount rejects a response that does not confirm linking',
    () async {
      final service = AuthService.forTesting(
        googleIdTokenProvider: () async => 'fresh-firebase-token',
        postRequest: (_, _) async => {
          'data': {'provider': 'GOOGLE', 'linked': false},
        },
      );

      await expectLater(
        service.linkGoogleAccount(),
        throwsA(
          isA<LinkedAccountException>().having(
            (error) => error.failure.kind,
            'failure kind',
            LinkedAccountFailureKind.unexpected,
          ),
        ),
      );
    },
  );
}
