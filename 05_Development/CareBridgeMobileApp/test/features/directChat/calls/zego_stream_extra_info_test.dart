import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/directChat/calls/zego_stream_extra_info.dart';

void main() {
  test('publishes the media status contract expected by Zego UIKit', () {
    final extraInfo = jsonDecode(
      buildZegoUIKitStreamExtraInfo(
        isCameraOn: true,
        isMicrophoneOn: true,
        hasVideo: true,
      ),
    );

    expect(extraInfo, {
      'isCameraOn': true,
      'isMicrophoneOn': true,
      'hasAudio': true,
      'hasVideo': true,
    });
  });

  test('keeps video capability while reporting a temporarily muted camera', () {
    final extraInfo = jsonDecode(
      buildZegoUIKitStreamExtraInfo(
        isCameraOn: false,
        isMicrophoneOn: false,
        hasVideo: true,
      ),
    );

    expect(extraInfo['isCameraOn'], isFalse);
    expect(extraInfo['isMicrophoneOn'], isFalse);
    expect(extraInfo['hasVideo'], isTrue);
  });
}
