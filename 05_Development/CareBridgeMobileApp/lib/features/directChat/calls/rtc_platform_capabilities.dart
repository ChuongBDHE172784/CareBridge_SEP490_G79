import 'package:flutter/foundation.dart';

typedef SpeakerRouteSetter = Future<void> Function(bool enabled);

@immutable
class RtcPlatformCapabilities {
  const RtcPlatformCapabilities({required this.isWeb});

  const RtcPlatformCapabilities.web() : isWeb = true;

  const RtcPlatformCapabilities.native() : isWeb = false;

  factory RtcPlatformCapabilities.current() =>
      RtcPlatformCapabilities(isWeb: kIsWeb);

  final bool isWeb;

  bool get supportsSpeakerRouting => !isWeb;

  bool get showsSpeakerRouteControl => supportsSpeakerRouting;

  Future<void> setSpeakerRoute({
    required bool enabled,
    required SpeakerRouteSetter setter,
  }) async {
    if (!supportsSpeakerRouting) return;
    await setter(enabled);
  }
}
