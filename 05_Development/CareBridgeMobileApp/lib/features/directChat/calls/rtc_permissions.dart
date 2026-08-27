import 'package:permission_handler/permission_handler.dart';

typedef RtcPermissionRequester =
    Future<PermissionStatus> Function(Permission permission);

Future<PermissionStatus> _requestPermission(Permission permission) =>
    permission.request();

Future<String?> requestRtcPermissions({
  required bool isVideo,
  RtcPermissionRequester request = _requestPermission,
}) async {
  final microphone = await request(Permission.microphone);
  if (!microphone.isGranted) {
    return 'Microphone chưa được cấp quyền cho CareBridge.';
  }
  if (!isVideo) return null;

  final camera = await request(Permission.camera);
  if (!camera.isGranted) {
    return 'Camera chưa được cấp quyền cho CareBridge.';
  }
  return null;
}
