import 'dart:convert';

/// Media metadata shared with Zego UIKit clients.
///
/// Zego Express does not add this metadata automatically. UIKit clients use
/// it to decide whether the remote camera/microphone should be rendered.
String buildZegoUIKitStreamExtraInfo({
  required bool isCameraOn,
  required bool isMicrophoneOn,
  required bool hasVideo,
}) {
  return jsonEncode({
    'isCameraOn': isCameraOn,
    'isMicrophoneOn': isMicrophoneOn,
    'hasAudio': true,
    'hasVideo': hasVideo,
  });
}
