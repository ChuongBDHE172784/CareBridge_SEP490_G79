// Web-only JavaScript interop selected without making native analysis depend
// on the web SDK library.
export 'posture_camera_js_stub.dart'
    if (dart.library.html) 'posture_camera_js_web.dart';
