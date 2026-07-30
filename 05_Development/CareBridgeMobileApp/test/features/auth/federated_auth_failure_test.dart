import 'dart:io';

import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:http/http.dart' as http;
import 'package:untitled/core/network/api_client.dart';
import 'package:untitled/features/auth/models/federated_auth_failure.dart';

void main() {
  group('FederatedAuthFailure.from', () {
    test('maps every Google provider error code', () {
      const expectedKinds =
          <GoogleSignInExceptionCode, FederatedAuthFailureKind>{
            GoogleSignInExceptionCode.unknownError:
                FederatedAuthFailureKind.unexpected,
            GoogleSignInExceptionCode.canceled:
                FederatedAuthFailureKind.canceled,
            GoogleSignInExceptionCode.interrupted:
                FederatedAuthFailureKind.serviceUnavailable,
            GoogleSignInExceptionCode.clientConfigurationError:
                FederatedAuthFailureKind.configuration,
            GoogleSignInExceptionCode.providerConfigurationError:
                FederatedAuthFailureKind.configuration,
            GoogleSignInExceptionCode.uiUnavailable:
                FederatedAuthFailureKind.serviceUnavailable,
            GoogleSignInExceptionCode.userMismatch:
                FederatedAuthFailureKind.configuration,
          };

      expect(expectedKinds, hasLength(GoogleSignInExceptionCode.values.length));
      for (final code in GoogleSignInExceptionCode.values) {
        final failure = FederatedAuthFailure.from(
          GoogleSignInException(
            code: code,
            description: 'sensitive OAuth client detail',
          ),
        );
        expect(failure.kind, expectedKinds[code], reason: code.name);
        expect(
          failure.userMessage,
          isNot(contains('OAuth')),
          reason: code.name,
        );
      }

      expect(
        FederatedAuthFailure.from(
          const GoogleSignInException(code: GoogleSignInExceptionCode.canceled),
        ).userMessage,
        isEmpty,
      );
    });

    test('maps Firebase credential, account, and rate-limit failures', () {
      expect(
        FederatedAuthFailure.from(
          FirebaseAuthException(code: 'invalid-credential'),
        ).kind,
        FederatedAuthFailureKind.invalidCredential,
      );
      expect(
        FederatedAuthFailure.from(
          FirebaseAuthException(code: 'user-disabled'),
        ).kind,
        FederatedAuthFailureKind.disabledAccount,
      );
      expect(
        FederatedAuthFailure.from(
          FirebaseAuthException(
            code: 'account-exists-with-different-credential',
          ),
        ).kind,
        FederatedAuthFailureKind.accountConflict,
      );
      expect(
        FederatedAuthFailure.from(
          FirebaseAuthException(code: 'too-many-requests'),
        ).kind,
        FederatedAuthFailureKind.rateLimited,
      );
    });

    test('maps Web popup cancellation and configuration failures', () {
      for (final code in [
        'popup-closed-by-user',
        'cancelled-popup-request',
        'web-context-canceled',
      ]) {
        final failure = FederatedAuthFailure.from(
          FirebaseAuthException(code: code, message: 'raw popup detail'),
        );
        expect(failure.kind, FederatedAuthFailureKind.canceled, reason: code);
        expect(failure.userMessage, isEmpty, reason: code);
      }

      for (final code in ['operation-not-allowed', 'unauthorized-domain']) {
        final failure = FederatedAuthFailure.from(
          FirebaseAuthException(code: code, message: 'raw project detail'),
        );
        expect(
          failure.kind,
          FederatedAuthFailureKind.configuration,
          reason: code,
        );
        expect(failure.userMessage, isNot(contains('raw')), reason: code);
      }
    });

    test('maps backend rejection, throttling, and service failures', () {
      expect(
        FederatedAuthFailure.from(ApiException(400, 'raw backend body')).kind,
        FederatedAuthFailureKind.backendRejected,
      );
      expect(
        FederatedAuthFailure.from(ApiException(401, 'raw backend body')).kind,
        FederatedAuthFailureKind.invalidCredential,
      );
      expect(
        FederatedAuthFailure.from(ApiException(409, 'raw backend body')).kind,
        FederatedAuthFailureKind.accountConflict,
      );
      expect(
        FederatedAuthFailure.from(ApiException(429, 'raw backend body')).kind,
        FederatedAuthFailureKind.rateLimited,
      );
      final serviceFailure = FederatedAuthFailure.from(
        ApiException(503, 'raw backend body'),
      );
      expect(serviceFailure.kind, FederatedAuthFailureKind.serviceUnavailable);
      expect(serviceFailure.userMessage, isNot(contains('raw backend body')));
    });

    test('maps socket and HTTP client transport failures', () {
      expect(
        FederatedAuthFailure.from(const SocketException('host detail')).kind,
        FederatedAuthFailureKind.connectivity,
      );
      expect(
        FederatedAuthFailure.from(http.ClientException('URI detail')).kind,
        FederatedAuthFailureKind.connectivity,
      );
    });

    test('falls back safely for unknown errors', () {
      final failure = FederatedAuthFailure.from(
        StateError('token and internal detail'),
      );

      expect(failure.kind, FederatedAuthFailureKind.unexpected);
      expect(failure.userMessage, isNot(contains('token')));
    });
  });
}
