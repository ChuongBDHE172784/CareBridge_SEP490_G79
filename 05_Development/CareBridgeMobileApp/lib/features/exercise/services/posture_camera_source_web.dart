// ignore_for_file: avoid_web_libraries_in_flutter, deprecated_member_use

import 'dart:async';
import 'dart:html' as html;
import 'dart:js_interop';
import 'dart:ui_web' as ui_web;

import 'package:flutter/material.dart';

import 'posture_camera_js.dart' as js_util;
import 'posture_overlay_renderer.dart';

/// Browser camera + MediaPipe Pose source used by the Exercise-Correction demo.
///
/// Raw video never leaves the browser. Only normalized named landmarks are
/// emitted to the Flutter transport, which samples and sends them to Spring.
class PostureCameraSource {
  PostureCameraSource()
    : _viewType = 'carebridge-posture-camera-${_nextViewId++}',
      _video = html.VideoElement(),
      _canvas = html.CanvasElement(width: 640, height: 480),
      _previewRoot = html.DivElement() {
    _video
      ..autoplay = true
      ..muted = true
      ..setAttribute('playsinline', 'true')
      ..style.position = 'absolute'
      ..style.left = '0'
      ..style.top = '0'
      ..style.width = '100%'
      ..style.height = '100%'
      ..style.objectFit = 'cover'
      ..style.transform = 'none';

    _canvas
      ..style.position = 'absolute'
      ..style.left = '0'
      ..style.top = '0'
      ..style.width = '100%'
      ..style.height = '100%'
      ..style.objectFit = 'cover'
      ..style.setProperty('pointer-events', 'none')
      ..setAttribute('aria-hidden', 'true');

    _previewRoot
      ..style.position = 'relative'
      ..style.display = 'block'
      ..style.width = '100%'
      ..style.height = '100%'
      ..style.overflow = 'hidden'
      ..style.backgroundColor = '#000'
      // Keep the visual mirror identical for video and overlay. Landmark
      // payloads remain in MediaPipe's canonical, unmirrored coordinates.
      ..style.transform = 'scaleX(-1)'
      ..style.transformOrigin = 'center';
    _previewRoot.children.addAll(<html.Element>[_video, _canvas]);

    ui_web.platformViewRegistry.registerViewFactory(
      _viewType,
      (int _) => _previewRoot,
    );
  }

  static int _nextViewId = 0;
  static Future<void>? _poseScriptLoadFuture;

  // Pin the legacy MediaPipe bundle so a CDN update cannot silently break the
  // browser constructor or its model assets during a demo.
  static const _poseVersion = '0.5.1675469404';
  static const _posePackageBase =
      'https://cdn.jsdelivr.net/npm/@mediapipe/pose@$_poseVersion/';
  static const _poseScript = '${_posePackageBase}pose.js';
  static const _poseModelBase = _posePackageBase;

  static const _landmarkNames = <String>[
    'nose',
    'left_eye_inner',
    'left_eye',
    'left_eye_outer',
    'right_eye_inner',
    'right_eye',
    'right_eye_outer',
    'left_ear',
    'right_ear',
    'mouth_left',
    'mouth_right',
    'left_shoulder',
    'right_shoulder',
    'left_elbow',
    'right_elbow',
    'left_wrist',
    'right_wrist',
    'left_pinky',
    'right_pinky',
    'left_index',
    'right_index',
    'left_thumb',
    'right_thumb',
    'left_hip',
    'right_hip',
    'left_knee',
    'right_knee',
    'left_ankle',
    'right_ankle',
    'left_heel',
    'right_heel',
    'left_foot_index',
    'right_foot_index',
  ];

  final String _viewType;
  final html.VideoElement _video;
  final html.CanvasElement _canvas;
  final html.DivElement _previewRoot;
  final _framesController = StreamController<Map<String, dynamic>>.broadcast();
  final _errorsController = StreamController<String>.broadcast();

  html.MediaStream? _mediaStream;
  Object? _pose;
  Timer? _frameTimer;
  bool _running = false;
  bool _starting = false;
  int _runGeneration = 0;
  int? _sendInFlightGeneration;
  String? _lastError;

  Stream<Map<String, dynamic>> get frames => _framesController.stream;

  Stream<String> get errors => _errorsController.stream;

  bool get isSupported => true;

  bool get isRunning => _running;

  String? get lastError => _lastError;

  @visibleForTesting
  html.DivElement get debugPreviewElement => _previewRoot;

  Widget buildPreview() => HtmlElementView(viewType: _viewType);

  Future<void> start() async {
    if (_running || _starting) return;
    _starting = true;
    final runGeneration = ++_runGeneration;
    _lastError = null;
    _clearOverlay();
    try {
      _requireTrustedCameraContext();
      final mediaDevices = html.window.navigator.mediaDevices;
      if (mediaDevices == null) {
        throw StateError('CAMERA_API_UNAVAILABLE');
      }
      final mediaStream = await mediaDevices.getUserMedia(<String, Object?>{
        'audio': false,
        'video': <String, Object?>{
          'width': <String, int>{'ideal': 640},
          'height': <String, int>{'ideal': 480},
          'facingMode': <String, String>{'ideal': 'user'},
        },
      });
      if (runGeneration != _runGeneration) {
        _stopTracks(mediaStream);
        return;
      }

      _mediaStream = mediaStream;
      _video.srcObject = mediaStream;
      await _video.play();
      if (runGeneration != _runGeneration) return;

      _syncCanvasDimensions();
      _running = true;
      // Camera preview must not be blocked by a CDN/model failure. Pose is
      // initialized in the background after the stream is visible.
      unawaited(_startPoseAnalysis(runGeneration));
    } catch (error, stackTrace) {
      // A stop/dispose can invalidate a pending getUserMedia/play request.
      // Its late completion must not surface an error for a newer run.
      if (runGeneration != _runGeneration) return;
      debugPrint('Posture camera startup failed: $error\n$stackTrace');
      await stop();
      _setError(_cameraErrorMessage(error));
      rethrow;
    } finally {
      if (runGeneration == _runGeneration) {
        _starting = false;
      }
    }
  }

  Future<void> stop() async {
    _runGeneration++;
    _starting = false;
    _running = false;
    _clearOverlay();
    _frameTimer?.cancel();
    _frameTimer = null;
    _sendInFlightGeneration = null;

    final pose = _pose;
    _pose = null;
    if (pose != null) {
      try {
        js_util.callMethod(pose, 'close', const <Object?>[]);
      } catch (_) {
        // MediaPipe may already have closed after a browser context loss.
      }
    }

    final stream = _mediaStream;
    _mediaStream = null;
    if (stream != null) {
      _stopTracks(stream);
    }
    _video.srcObject = null;
  }

  Future<void> dispose() async {
    await stop();
    await _framesController.close();
    await _errorsController.close();
  }

  Future<void> _ensurePoseScript() async {
    final existing = js_util.getProperty<Object?>(js_util.globalThis, 'Pose');
    if (existing != null) return;

    final inFlight = _poseScriptLoadFuture;
    if (inFlight != null) {
      await inFlight;
      return;
    }

    final loadFuture = _loadPoseScript();
    _poseScriptLoadFuture = loadFuture;
    try {
      await loadFuture;
    } finally {
      if (identical(_poseScriptLoadFuture, loadFuture)) {
        _poseScriptLoadFuture = null;
      }
    }
  }

  Future<void> _loadPoseScript() async {
    final script = html.ScriptElement()
      ..src = _poseScript
      ..crossOrigin = 'anonymous';
    final loaded = Completer<void>();
    script.onLoad.listen((_) {
      if (!loaded.isCompleted) loaded.complete();
    });
    script.onError.listen((_) {
      if (!loaded.isCompleted) {
        script.remove();
        loaded.completeError(StateError('Unable to load MediaPipe Pose CDN'));
      }
    });
    html.document.head?.append(script);
    await loaded.future;
  }

  Future<void> _startPoseAnalysis(int runGeneration) async {
    try {
      await _ensurePoseScript();
      if (!_isCurrentRun(runGeneration)) return;

      final poseConstructor = js_util.getProperty<Object?>(
        js_util.globalThis,
        'Pose',
      );
      if (poseConstructor == null) {
        throw StateError('MEDIAPIPE_POSE_NOT_EXPOSED');
      }

      final options = js_util.jsify(<String, Object?>{
        'locateFile': js_util.allowInteropString(
          (String file) => '$_poseModelBase$file',
        ),
      });
      final pose = js_util.callConstructor<Object?>(poseConstructor, <Object?>[
        options,
      ]);
      if (pose == null) {
        throw StateError('MEDIAPIPE_POSE_CONSTRUCTION_FAILED');
      }
      js_util.callMethod(pose, 'setOptions', <Object?>[
        js_util.jsify(<String, Object?>{
          'modelComplexity': 1,
          'smoothLandmarks': true,
          'enableSegmentation': false,
          'minDetectionConfidence': 0.5,
          'minTrackingConfidence': 0.5,
        }),
      ]);
      js_util.callMethod(pose, 'onResults', <Object?>[
        js_util.allowInteropResults(
          (results) => _onResults(runGeneration, results),
        ),
      ]);
      if (!_isCurrentRun(runGeneration)) {
        js_util.callMethod(pose, 'close', const <Object?>[]);
        return;
      }

      _pose = pose;
      _frameTimer = Timer.periodic(const Duration(milliseconds: 66), (_) {
        unawaited(_sendFrame(runGeneration, pose));
      });
    } catch (error, stackTrace) {
      debugPrint('Posture analysis startup failed: $error\n$stackTrace');
      if (_isCurrentRun(runGeneration)) {
        _setError(_poseErrorMessage(error));
      }
    }
  }

  Future<void> _sendFrame(int runGeneration, Object pose) async {
    if (!_isCurrentRun(runGeneration) ||
        _sendInFlightGeneration == runGeneration ||
        !identical(_pose, pose) ||
        _video.readyState < 2) {
      return;
    }
    _sendInFlightGeneration = runGeneration;
    try {
      final result = js_util.callMethod(pose, 'send', <Object?>[
        js_util.jsify(<String, Object?>{'image': _video}),
      ]);
      if (result != null) {
        await js_util.promiseToFuture<Object?>(result);
      }
    } catch (error, stackTrace) {
      debugPrint('Posture frame analysis failed: $error\n$stackTrace');
      if (_isCurrentRun(runGeneration)) {
        _setError(
          'Camera đã kết nối nhưng chưa thể phân tích tư thế. '
          'Hãy thử lại camera.',
        );
      }
    } finally {
      if (_sendInFlightGeneration == runGeneration) {
        _sendInFlightGeneration = null;
      }
    }
  }

  void _onResults(int runGeneration, JSAny? results) {
    if (!_isCurrentRun(runGeneration)) return;
    _clearOverlay();
    try {
      if (results == null) return;
      final rawLandmarks = js_util.getProperty<Object?>(
        results,
        'poseLandmarks',
      );
      if (rawLandmarks == null) return;

      final rawLength = js_util.getProperty<Object?>(rawLandmarks, 'length');
      final length = rawLength is num ? rawLength.toInt() : 0;
      if (length == 0) return;

      _syncCanvasDimensions();
      final overlayPoints = <PostureOverlayPoint?>[];

      final frame = <String, dynamic>{};
      final count = length < _landmarkNames.length
          ? length
          : _landmarkNames.length;
      for (var index = 0; index < count; index++) {
        final landmark = _arrayItem(rawLandmarks, index);
        if (landmark == null) {
          overlayPoints.add(null);
          continue;
        }
        final x = _finiteNumber(landmark, 'x');
        final y = _finiteNumber(landmark, 'y');
        final visibility = _finiteNumber(landmark, 'visibility');
        overlayPoints.add(
          x == null || y == null || visibility == null
              ? null
              : PostureOverlayPoint(x: x, y: y, visibility: visibility),
        );
        frame[_landmarkNames[index]] = <String, double>{
          'x': _number(landmark, 'x'),
          'y': _number(landmark, 'y'),
          'z': _number(landmark, 'z'),
          'visibility': _number(landmark, 'visibility'),
        };
      }
      PostureOverlayRenderer.draw(_canvas.context2D, overlayPoints);
      if (frame.isNotEmpty && !_framesController.isClosed) {
        _framesController.add(frame);
      }
    } catch (error, stackTrace) {
      debugPrint('Posture result handling failed: $error\n$stackTrace');
      if (_isCurrentRun(runGeneration)) {
        _setError(_poseErrorMessage(error));
      }
    }
  }

  Object? _arrayItem(Object array, int index) {
    if (array is List<Object?> && index < array.length) return array[index];
    return js_util.getProperty<Object?>(array, '$index');
  }

  double _number(Object object, String property) {
    final value = js_util.getProperty<Object?>(object, property);
    return value is num && value.isFinite ? value.toDouble() : 0.0;
  }

  double? _finiteNumber(Object object, String property) {
    final value = js_util.getProperty<Object?>(object, property);
    return value is num && value.isFinite ? value.toDouble() : null;
  }

  void _syncCanvasDimensions() {
    final width = _video.videoWidth;
    final height = _video.videoHeight;
    if (width <= 0 || height <= 0) return;
    if (_canvas.width != width) _canvas.width = width;
    if (_canvas.height != height) _canvas.height = height;
  }

  void _clearOverlay() {
    PostureOverlayRenderer.clear(_canvas.context2D);
  }

  void _setError(String message) {
    _lastError = message;
    if (!_errorsController.isClosed) _errorsController.add(message);
  }

  bool _isCurrentRun(int runGeneration) =>
      _running && runGeneration == _runGeneration;

  void _stopTracks(html.MediaStream stream) {
    for (final track in stream.getTracks()) {
      track.stop();
    }
  }

  void _requireTrustedCameraContext() {
    final location = html.window.location;
    final protocol = location.protocol;
    final host = (location.hostname ?? '').toLowerCase();
    final trustedLocalHost =
        host == 'localhost' || host == '127.0.0.1' || host == '::1';
    if (protocol != 'https:' && !trustedLocalHost) {
      throw StateError('INSECURE_CAMERA_CONTEXT');
    }
  }

  String _cameraErrorMessage(Object error) {
    final text = error.toString().toLowerCase();
    if (text.contains('insecure_camera_context') ||
        text.contains('securityerror')) {
      return 'Camera chỉ hoạt động trên HTTPS, localhost hoặc 127.0.0.1.';
    }
    if (text.contains('notallowed') ||
        text.contains('permission') ||
        text.contains('permissiondenied')) {
      return 'Camera bị từ chối. Hãy cấp quyền camera rồi thử lại.';
    }
    if (text.contains('notfound') || text.contains('device')) {
      return 'Không tìm thấy camera trên thiết bị này.';
    }
    if (text.contains('notreadable') ||
        text.contains('trackstarterror') ||
        text.contains('could not start video source')) {
      return 'Không thể đọc camera. Hãy đóng ứng dụng khác đang dùng camera '
          'và kiểm tra quyền Camera trong Windows.';
    }
    if (text.contains('overconstrained')) {
      return 'Camera không hỗ trợ cấu hình yêu cầu. Hãy thử lại camera.';
    }
    if (text.contains('camera_api_unavailable')) {
      return 'Trình duyệt không cung cấp API camera. Hãy dùng Chrome/Edge '
          'trên localhost hoặc HTTPS.';
    }
    return 'Không thể khởi động camera realtime. Hãy đóng ứng dụng khác '
        'đang dùng camera rồi thử lại.';
  }

  String _poseErrorMessage(Object error) {
    final text = error.toString().toLowerCase();
    if (text.contains('unable to load mediapipe')) {
      return 'Camera đã kết nối nhưng không tải được MediaPipe Pose. '
          'Hãy kiểm tra kết nối Internet/CDN rồi thử lại.';
    }
    return 'Camera đã kết nối nhưng bộ phân tích tư thế chưa khởi động. '
        'Hãy thử lại camera.';
  }
}

PostureCameraSource createPostureCameraSource() => PostureCameraSource();
