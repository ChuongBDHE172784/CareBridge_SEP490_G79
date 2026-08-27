import 'package:flutter_test/flutter_test.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:untitled/features/directChat/calls/rtc_permissions.dart';

void main() {
  test('voice requests microphone only', () async {
    final requested = <Permission>[];

    final error = await requestRtcPermissions(
      isVideo: false,
      request: (permission) async {
        requested.add(permission);
        return PermissionStatus.granted;
      },
    );

    expect(error, isNull);
    expect(requested, [Permission.microphone]);
  });

  test('microphone denial blocks voice and video setup', () async {
    final error = await requestRtcPermissions(
      isVideo: true,
      request: (_) async => PermissionStatus.denied,
    );

    expect(error, 'Microphone chưa được cấp quyền cho CareBridge.');
  });

  test('video requires camera after microphone is granted', () async {
    final requested = <Permission>[];

    final error = await requestRtcPermissions(
      isVideo: true,
      request: (permission) async {
        requested.add(permission);
        return permission == Permission.microphone
            ? PermissionStatus.granted
            : PermissionStatus.denied;
      },
    );

    expect(error, 'Camera chưa được cấp quyền cho CareBridge.');
    expect(requested, [Permission.microphone, Permission.camera]);
  });
}
