import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/directChat/calls/rtc_platform_capabilities.dart';

void main() {
  test('Web neither calls speaker routing nor shows its control', () async {
    const capabilities = RtcPlatformCapabilities.web();
    final routedValues = <bool>[];

    await capabilities.setSpeakerRoute(
      enabled: true,
      setter: (enabled) async => routedValues.add(enabled),
    );

    expect(capabilities.supportsSpeakerRouting, isFalse);
    expect(capabilities.showsSpeakerRouteControl, isFalse);
    expect(routedValues, isEmpty);
  });

  test('native platforms preserve speaker routing', () async {
    const capabilities = RtcPlatformCapabilities.native();
    final routedValues = <bool>[];

    await capabilities.setSpeakerRoute(
      enabled: true,
      setter: (enabled) async => routedValues.add(enabled),
    );

    expect(capabilities.supportsSpeakerRouting, isTrue);
    expect(capabilities.showsSpeakerRouteControl, isTrue);
    expect(routedValues, [true]);
  });
}
