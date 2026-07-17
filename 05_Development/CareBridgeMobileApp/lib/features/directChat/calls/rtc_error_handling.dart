import 'package:flutter/foundation.dart';

enum RtcSetupStage {
  permissions,
  engineInitialization,
  canvasCreation,
  roomLogin,
  mediaSetup,
  mediaPublish,
  mediaPlay,
}

enum RtcFailureCategory {
  permissionDenied,
  webWrapperMissing,
  engineInitialization,
  roomLogin,
  mediaSetup,
  mediaPublish,
  mediaPlay,
  unknown,
}

@immutable
class RtcFailurePresentation {
  const RtcFailurePresentation({
    required this.category,
    required this.userMessage,
  });

  final RtcFailureCategory category;
  final String userMessage;
}

class RtcSetupException implements Exception {
  const RtcSetupException({
    required this.category,
    required this.userMessage,
    this.errorCode,
  });

  final RtcFailureCategory category;
  final String userMessage;
  final int? errorCode;

  @override
  String toString() =>
      'RtcSetupException(category: ${category.name}, errorCode: $errorCode)';
}

RtcFailurePresentation classifyRtcSetupFailure(
  Object error, {
  required RtcSetupStage stage,
  required bool isWeb,
}) {
  if (error is RtcSetupException) {
    return RtcFailurePresentation(
      category: error.category,
      userMessage: error.userMessage,
    );
  }

  if (isWeb && _looksLikeMissingWebWrapper(error)) {
    return const RtcFailurePresentation(
      category: RtcFailureCategory.webWrapperMissing,
      userMessage:
          'Thành phần cuộc gọi trên trình duyệt chưa tải. '
          'Hãy tải lại trang và thử lại.',
    );
  }

  return switch (stage) {
    RtcSetupStage.permissions => const RtcFailurePresentation(
      category: RtcFailureCategory.permissionDenied,
      userMessage:
          'CareBridge chưa được cấp quyền micro/camera. '
          'Hãy kiểm tra quyền của trình duyệt và thử lại.',
    ),
    RtcSetupStage.engineInitialization => const RtcFailurePresentation(
      category: RtcFailureCategory.engineInitialization,
      userMessage:
          'Không thể khởi tạo cuộc gọi. '
          'Hãy kiểm tra thiết bị và kết nối mạng.',
    ),
    RtcSetupStage.canvasCreation => const RtcFailurePresentation(
      category: RtcFailureCategory.mediaSetup,
      userMessage:
          'Không thể mở phần hiển thị video. '
          'Hãy kiểm tra camera và thử lại.',
    ),
    RtcSetupStage.roomLogin => const RtcFailurePresentation(
      category: RtcFailureCategory.roomLogin,
      userMessage:
          'Không thể tham gia phòng cuộc gọi. '
          'Hãy kiểm tra kết nối mạng và thử lại.',
    ),
    RtcSetupStage.mediaSetup => const RtcFailurePresentation(
      category: RtcFailureCategory.mediaSetup,
      userMessage:
          'Không thể thiết lập micro/camera cho cuộc gọi. '
          'Hãy kiểm tra thiết bị và thử lại.',
    ),
    RtcSetupStage.mediaPublish => const RtcFailurePresentation(
      category: RtcFailureCategory.mediaPublish,
      userMessage:
          'Không thể gửi âm thanh hoặc video. '
          'Hãy kiểm tra thiết bị và kết nối mạng.',
    ),
    RtcSetupStage.mediaPlay => const RtcFailurePresentation(
      category: RtcFailureCategory.mediaPlay,
      userMessage:
          'Không thể nhận âm thanh hoặc video từ cuộc gọi. '
          'Hãy kiểm tra kết nối mạng.',
    ),
  };
}

bool _looksLikeMissingWebWrapper(Object error) {
  final text = error.toString().toLowerCase();
  return text.contains('zegoexpresswebflutterwrapper') ||
      text.contains('zegoflutterengine') ||
      (text.contains('referenceerror') &&
          (text.contains('zego') || text.contains('not defined')));
}

String buildRtcDebugDiagnostic({
  required Object error,
  required StackTrace stackTrace,
  required RtcSetupStage stage,
  required RtcFailureCategory category,
  Iterable<String> sensitiveValues = const [],
}) {
  final diagnostic =
      '[RTC][${category.name}] stage=${stage.name} '
      'errorType=${error.runtimeType} error=$error\n$stackTrace';
  return redactRtcDiagnostic(diagnostic, sensitiveValues: sensitiveValues);
}

String redactRtcDiagnostic(
  String diagnostic, {
  Iterable<String> sensitiveValues = const [],
}) {
  var redacted = diagnostic;
  final values =
      sensitiveValues.where((value) => value.trim().isNotEmpty).toSet().toList()
        ..sort((left, right) => right.length.compareTo(left.length));
  for (final value in values) {
    redacted = redacted.replaceAll(value, '[REDACTED]');
  }

  redacted = redacted.replaceAll(
    RegExp(r'Bearer\s+[^\s,;]+', caseSensitive: false),
    'Bearer [REDACTED]',
  );
  redacted = redacted.replaceAllMapped(
    RegExp(
      r'\b(token|firebaseToken|appSign|serverSecret|authorization)'
      r'\b\s*[:=]\s*[^\s,;}\]]+',
      caseSensitive: false,
    ),
    (match) => '${match.group(1)}=[REDACTED]',
  );
  redacted = redacted.replaceAll(
    RegExp(r'\beyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\b'),
    '[REDACTED_JWT]',
  );
  return redacted;
}

void debugLogRtcFailure({
  required Object error,
  required StackTrace stackTrace,
  required RtcSetupStage stage,
  required RtcFailureCategory category,
  Iterable<String> sensitiveValues = const [],
}) {
  if (!kDebugMode) return;
  debugPrint(
    buildRtcDebugDiagnostic(
      error: error,
      stackTrace: stackTrace,
      stage: stage,
      category: category,
      sensitiveValues: sensitiveValues,
    ),
  );
}

void debugLogRtcSdkError({
  required RtcFailureCategory category,
  required int errorCode,
}) {
  if (!kDebugMode) return;
  debugPrint('[RTC][${category.name}] errorCode=$errorCode');
}
