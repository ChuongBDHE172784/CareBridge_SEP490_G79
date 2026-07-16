import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/directChat/calls/rtc_error_handling.dart';

void main() {
  test('missing Zego Web wrapper gets a safe browser-specific message', () {
    final failure = classifyRtcSetupFailure(
      StateError('ReferenceError: ZegoExpressWebFlutterWrapper is not defined'),
      stage: RtcSetupStage.engineInitialization,
      isWeb: true,
    );

    expect(failure.category, RtcFailureCategory.webWrapperMissing);
    expect(failure.userMessage, contains('tải lại trang'));
    expect(failure.userMessage, isNot(contains('ReferenceError')));
  });

  test('room login and media stages map to safe messages', () {
    final roomFailure = classifyRtcSetupFailure(
      StateError('login failed with internal data'),
      stage: RtcSetupStage.roomLogin,
      isWeb: false,
    );
    final publishFailure = classifyRtcSetupFailure(
      StateError('publish failed with internal data'),
      stage: RtcSetupStage.mediaPublish,
      isWeb: false,
    );

    expect(roomFailure.category, RtcFailureCategory.roomLogin);
    expect(roomFailure.userMessage, contains('tham gia phòng'));
    expect(roomFailure.userMessage, isNot(contains('internal data')));
    expect(publishFailure.category, RtcFailureCategory.mediaPublish);
    expect(publishFailure.userMessage, contains('Không thể gửi'));
  });

  test('debug diagnostics redact credentials and common secret formats', () {
    const zegoToken = 'zego-token-value';
    const firebaseToken = 'firebase-secret-value';
    const serverSecret = 'server-secret-value';
    const roomId = 'cb_private_room';
    const jwt = 'eyJhbGciOiJIUzI1NiJ9.payload.signature';

    final diagnostic = buildRtcDebugDiagnostic(
      error: StateError(
        'token=$zegoToken firebaseToken=$firebaseToken '
        'serverSecret=$serverSecret room=$roomId jwt=$jwt',
      ),
      stackTrace: StackTrace.fromString('authorization=Bearer $zegoToken'),
      stage: RtcSetupStage.engineInitialization,
      category: RtcFailureCategory.engineInitialization,
      sensitiveValues: const [zegoToken, firebaseToken, serverSecret, roomId],
    );

    expect(diagnostic, isNot(contains(zegoToken)));
    expect(diagnostic, isNot(contains(firebaseToken)));
    expect(diagnostic, isNot(contains(serverSecret)));
    expect(diagnostic, isNot(contains(roomId)));
    expect(diagnostic, isNot(contains(jwt)));
    expect(diagnostic, contains('[REDACTED]'));
  });
}
