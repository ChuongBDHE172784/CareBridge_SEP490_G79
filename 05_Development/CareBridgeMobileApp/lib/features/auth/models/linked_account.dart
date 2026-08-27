import 'package:universal_io/io.dart';

import 'package:http/http.dart' as http;

import '../../../../core/network/api_client.dart';
import 'federated_auth_failure.dart';

class LinkedAccount {
  const LinkedAccount({
    required this.provider,
    required this.linked,
    this.email,
    this.linkedAt,
  });

  final String provider;
  final bool linked;
  final String? email;
  final DateTime? linkedAt;

  factory LinkedAccount.fromJson(Map<String, dynamic> json) {
    final rawEmail = json['email']?.toString().trim();
    final rawLinkedAt = json['linkedAt']?.toString();
    return LinkedAccount(
      provider: json['provider']?.toString() ?? 'GOOGLE',
      linked: json['linked'] == true,
      email: rawEmail == null || rawEmail.isEmpty ? null : rawEmail,
      linkedAt: rawLinkedAt == null ? null : DateTime.tryParse(rawLinkedAt),
    );
  }
}

enum LinkedAccountFailureKind {
  canceled,
  conflict,
  invalidCredential,
  configuration,
  rateLimited,
  serviceUnavailable,
  connectivity,
  unexpected,
}

class LinkedAccountFailure {
  const LinkedAccountFailure(this.kind, this.userMessage);

  final LinkedAccountFailureKind kind;
  final String userMessage;

  bool get isCanceled => kind == LinkedAccountFailureKind.canceled;

  static const canceled = LinkedAccountFailure(
    LinkedAccountFailureKind.canceled,
    '',
  );
  static const conflict = LinkedAccountFailure(
    LinkedAccountFailureKind.conflict,
    'Tài khoản Google này đã được liên kết, hoặc tài khoản CareBridge của bạn đã liên kết với một tài khoản Google khác.',
  );
  static const invalidCredential = LinkedAccountFailure(
    LinkedAccountFailureKind.invalidCredential,
    'CareBridge không thể xác minh tài khoản Google này. Vui lòng chọn lại tài khoản.',
  );
  static const configuration = LinkedAccountFailure(
    LinkedAccountFailureKind.configuration,
    'Tính năng liên kết Google chưa được cấu hình đúng. Vui lòng liên hệ hỗ trợ.',
  );
  static const rateLimited = LinkedAccountFailure(
    LinkedAccountFailureKind.rateLimited,
    'Bạn đã thử quá nhiều lần. Vui lòng đợi một lúc rồi thử lại.',
  );
  static const serviceUnavailable = LinkedAccountFailure(
    LinkedAccountFailureKind.serviceUnavailable,
    'Dịch vụ liên kết Google đang tạm thời gián đoạn. Vui lòng thử lại sau.',
  );
  static const connectivity = LinkedAccountFailure(
    LinkedAccountFailureKind.connectivity,
    'Không thể kết nối để liên kết tài khoản. Hãy kiểm tra mạng và thử lại.',
  );
  static const unexpected = LinkedAccountFailure(
    LinkedAccountFailureKind.unexpected,
    'Không thể hoàn tất liên kết tài khoản Google. Vui lòng thử lại.',
  );

  factory LinkedAccountFailure.from(Object error) {
    if (error is LinkedAccountException) return error.failure;

    if (error is FederatedSignInException) {
      return _fromFederatedFailure(error.failure);
    }

    if (error is ApiException) {
      if (error.statusCode == 409) return conflict;
      if (error.statusCode == 429) return rateLimited;
      if (error.statusCode == 400 || error.statusCode == 401) {
        return invalidCredential;
      }
      if (error.statusCode >= 500) return serviceUnavailable;
      return unexpected;
    }

    if (error is SocketException || error is http.ClientException) {
      return connectivity;
    }

    return _fromFederatedFailure(FederatedAuthFailure.from(error));
  }

  static LinkedAccountFailure _fromFederatedFailure(
    FederatedAuthFailure failure,
  ) {
    switch (failure.kind) {
      case FederatedAuthFailureKind.canceled:
        return canceled;
      case FederatedAuthFailureKind.configuration:
        return configuration;
      case FederatedAuthFailureKind.invalidCredential:
      case FederatedAuthFailureKind.disabledAccount:
      case FederatedAuthFailureKind.backendRejected:
        return invalidCredential;
      case FederatedAuthFailureKind.accountConflict:
        return conflict;
      case FederatedAuthFailureKind.rateLimited:
        return rateLimited;
      case FederatedAuthFailureKind.serviceUnavailable:
        return serviceUnavailable;
      case FederatedAuthFailureKind.connectivity:
        return connectivity;
      case FederatedAuthFailureKind.unexpected:
        return unexpected;
    }
  }
}

class LinkedAccountException implements Exception {
  const LinkedAccountException(this.failure);

  factory LinkedAccountException.from(Object error) =>
      LinkedAccountException(LinkedAccountFailure.from(error));

  final LinkedAccountFailure failure;

  @override
  String toString() => 'LinkedAccountException(${failure.kind.name})';
}
