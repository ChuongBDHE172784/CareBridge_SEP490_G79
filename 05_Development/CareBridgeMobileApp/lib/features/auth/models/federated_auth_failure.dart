import 'dart:io';

import 'package:firebase_auth/firebase_auth.dart';
import 'package:google_sign_in/google_sign_in.dart';
import 'package:http/http.dart' as http;

import '../../../../core/network/api_client.dart';

enum FederatedAuthFailureKind {
  canceled,
  configuration,
  invalidCredential,
  disabledAccount,
  accountConflict,
  rateLimited,
  backendRejected,
  serviceUnavailable,
  connectivity,
  unexpected,
}

class FederatedAuthFailure {
  const FederatedAuthFailure(this.kind, this.userMessage);

  final FederatedAuthFailureKind kind;
  final String userMessage;

  bool get isCanceled => kind == FederatedAuthFailureKind.canceled;

  static const canceled = FederatedAuthFailure(
    FederatedAuthFailureKind.canceled,
    '',
  );
  static const configuration = FederatedAuthFailure(
    FederatedAuthFailureKind.configuration,
    'Đăng nhập bằng Google chưa được cấu hình đúng. Vui lòng liên hệ hỗ trợ.',
  );
  static const invalidCredential = FederatedAuthFailure(
    FederatedAuthFailureKind.invalidCredential,
    'Thông tin đăng nhập Google không hợp lệ hoặc đã hết hạn. Vui lòng thử lại.',
  );
  static const disabledAccount = FederatedAuthFailure(
    FederatedAuthFailureKind.disabledAccount,
    'Tài khoản này đã bị vô hiệu hóa. Vui lòng liên hệ hỗ trợ.',
  );
  static const accountConflict = FederatedAuthFailure(
    FederatedAuthFailureKind.accountConflict,
    'Email này đã được liên kết với một phương thức đăng nhập khác.',
  );
  static const rateLimited = FederatedAuthFailure(
    FederatedAuthFailureKind.rateLimited,
    'Bạn đã thử quá nhiều lần. Vui lòng đợi một lúc rồi thử lại.',
  );
  static const backendRejected = FederatedAuthFailure(
    FederatedAuthFailureKind.backendRejected,
    'CareBridge không thể xác minh tài khoản Google này. Vui lòng thử tài khoản khác.',
  );
  static const serviceUnavailable = FederatedAuthFailure(
    FederatedAuthFailureKind.serviceUnavailable,
    'Dịch vụ đăng nhập đang tạm thời gián đoạn. Vui lòng thử lại sau.',
  );
  static const connectivity = FederatedAuthFailure(
    FederatedAuthFailureKind.connectivity,
    'Không thể kết nối đến dịch vụ đăng nhập. Vui lòng kiểm tra mạng và thử lại.',
  );
  static const unexpected = FederatedAuthFailure(
    FederatedAuthFailureKind.unexpected,
    'Không thể hoàn tất đăng nhập. Vui lòng thử lại hoặc liên hệ hỗ trợ.',
  );

  factory FederatedAuthFailure.from(Object error) {
    if (error is FederatedSignInException) return error.failure;

    if (error is GoogleSignInException) {
      switch (error.code) {
        case GoogleSignInExceptionCode.canceled:
          return canceled;
        case GoogleSignInExceptionCode.clientConfigurationError:
        case GoogleSignInExceptionCode.providerConfigurationError:
        case GoogleSignInExceptionCode.userMismatch:
          return configuration;
        case GoogleSignInExceptionCode.interrupted:
        case GoogleSignInExceptionCode.uiUnavailable:
          return serviceUnavailable;
        default:
          return unexpected;
      }
    }

    if (error is FirebaseAuthException) {
      switch (error.code) {
        case 'invalid-credential':
        case 'invalid-id-token':
        case 'expired-id-token':
          return invalidCredential;
        case 'user-disabled':
          return disabledAccount;
        case 'account-exists-with-different-credential':
        case 'credential-already-in-use':
        case 'email-already-in-use':
          return accountConflict;
        case 'too-many-requests':
          return rateLimited;
        case 'operation-not-allowed':
        case 'unauthorized-domain':
          return configuration;
        case 'network-request-failed':
          return connectivity;
        default:
          return unexpected;
      }
    }

    if (error is ApiException) {
      if (error.statusCode == 429) return rateLimited;
      if (error.statusCode >= 500) return serviceUnavailable;
      if (error.statusCode == 401) return invalidCredential;
      if (error.statusCode == 409) return accountConflict;
      if (error.statusCode >= 400) return backendRejected;
      return unexpected;
    }

    if (error is SocketException || error is http.ClientException) {
      return connectivity;
    }

    return unexpected;
  }
}

class FederatedSignInException implements Exception {
  const FederatedSignInException(this.failure);

  factory FederatedSignInException.from(Object error) =>
      FederatedSignInException(FederatedAuthFailure.from(error));

  final FederatedAuthFailure failure;

  @override
  String toString() => 'FederatedSignInException(${failure.kind.name})';
}
