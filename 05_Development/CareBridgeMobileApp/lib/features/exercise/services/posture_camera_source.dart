// Platform-selected realtime camera/pose source.
//
// Flutter Web uses the browser camera and MediaPipe Pose. Native targets use
// the conservative fallback until a native pose plugin is deliberately added;
// the rest of the exercise flow remains usable without camera analysis.
export 'posture_camera_source_stub.dart'
    if (dart.library.html) 'posture_camera_source_web.dart';
