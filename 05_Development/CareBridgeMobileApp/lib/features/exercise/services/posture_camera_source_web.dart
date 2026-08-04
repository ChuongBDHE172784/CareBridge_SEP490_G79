// ignore_for_file: avoid_web_libraries_in_flutter, deprecated_member_use

import 'dart:async';
import 'dart:html' as html;
import 'dart:js_interop';
import 'dart:ui_web' as ui_web;

import 'package:flutter/material.dart';

import 'posture_camera_js.dart' as js_util;

/// Browser camera + MediaPipe Pose source used by the Exercise-Correction demo.
///
/// Raw video never leaves the browser. Only normalized named landmarks are
/// emitted to the Flutter transport, which samples and sends them to Spring.
class PostureCameraSource {
  PostureCameraSource()
    : _viewType = 'carebridge-posture-camera-${_nextViewId++}',
      _video = html.VideoElement() {
    _video
      ..autoplay = true
      ..muted = true
      ..setAttribute('playsinline', 'true')
      ..style.width = '100%'
      ..style.height = '100%'
      ..style.objectFit = 'cover'
      ..style.transform = 'scaleX(-1)';

    ui_web.platformViewRegistry.registerViewFactory(
      _viewType,
      (int _) => _video,
    );
  }

  static int _nextViewId = 0;

  static const _poseScript =
      'https://cdn.jsdelivr.net/npm/@mediapipe/pose/pose.js';
  static const _poseModelBase = 'https://cdn.jsdelivr.net/npm/@mediapipe/pose/';

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
  final _framesController = StreamController<Map<String, dynamic>>.broadcast();
  final _errorsController = StreamController<String>.broadcast();

  html.MediaStream? _mediaStream;
  Object? _pose;
  Timer? _frameTimer;
  bool _running = false;
  bool _starting = false;
  bool _sendInFlight = false;
  String? _lastError;

  Stream<Map<String, dynamic>> get frames => _framesController.stream;

  Stream<String> get errors => _errorsController.stream;

  bool get isSupported => true;

  bool get isRunning => _running;

  String? get lastError => _lastError;

  Widget buildPreview() => HtmlElementView(viewType: _viewType);

  Future<void> start() async {
    if (_running || _starting) return;
    _starting = true;
    _lastError = null;
    try {
      await _ensurePoseScript();
      final poseConstructor = js_util.getProperty<Object?>(
        js_util.globalThis,
        'Pose',
      );
      if (poseConstructor == null) {
        throw StateError('MediaPipe Pose script did not expose Pose');
      }

      final options = js_util.jsify(<String, Object?>{
        'locateFile': js_util.allowInteropString(
          (String file) => '$_poseModelBase$file',
        ),
      });
      _pose = js_util.callConstructor(poseConstructor, <Object?>[options]);
      js_util.callMethod<void>(_pose!, 'setOptions', <Object?>[
        js_util.jsify(<String, Object?>{
          'modelComplexity': 1,
          'smoothLandmarks': true,
          'enableSegmentation': false,
          'minDetectionConfidence': 0.5,
          'minTrackingConfidence': 0.5,
        }),
      ]);
      js_util.callMethod<void>(_pose!, 'onResults', <Object?>[
        js_util.allowInteropResults(_onResults),
      ]);

      final mediaDevices = html.window.navigator.mediaDevices;
      if (mediaDevices == null) {
        throw StateError('Browser camera APIs are unavailable');
      }
      _mediaStream = await mediaDevices.getUserMedia(<String, Object?>{
        'audio': false,
        'video': <String, Object?>{
          'width': 640,
          'height': 480,
          'facingMode': 'user',
        },
      });
      _video.srcObject = _mediaStream;
      await _video.play();

      _running = true;
      _frameTimer = Timer.periodic(const Duration(milliseconds: 66), (_) {
        unawaited(_sendFrame());
      });
    } catch (error) {
      await stop();
      _setError(_cameraErrorMessage(error));
      rethrow;
    } finally {
      _starting = false;
    }
  }

  Future<void> stop() async {
    _running = false;
    _frameTimer?.cancel();
    _frameTimer = null;
    _sendInFlight = false;

    final pose = _pose;
    _pose = null;
    if (pose != null) {
      try {
        js_util.callMethod<void>(pose, 'close', const <Object?>[]);
      } catch (_) {
        // MediaPipe may already have closed after a browser context loss.
      }
    }

    final stream = _mediaStream;
    _mediaStream = null;
    if (stream != null) {
      for (final track in stream.getTracks()) {
        track.stop();
      }
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

    final script = html.ScriptElement()
      ..src = _poseScript
      ..crossOrigin = 'anonymous';
    final loaded = Completer<void>();
    script.onLoad.listen((_) {
      if (!loaded.isCompleted) loaded.complete();
    });
    script.onError.listen((_) {
      if (!loaded.isCompleted) {
        loaded.completeError(StateError('Unable to load MediaPipe Pose CDN'));
      }
    });
    html.document.head?.append(script);
    await loaded.future;
  }

  Future<void> _sendFrame() async {
    if (!_running || _sendInFlight || _pose == null || _video.readyState < 2) {
      return;
    }
    _sendInFlight = true;
    try {
      final result = js_util.callMethod<Object?>(_pose!, 'send', <Object?>[
        js_util.jsify(<String, Object?>{'image': _video}),
      ]);
      if (result != null) {
        await js_util.promiseToFuture<Object?>(result);
      }
    } catch (error) {
      _setError('Không thể phân tích camera realtime. Hãy thử tải lại camera.');
    } finally {
      _sendInFlight = false;
    }
  }

  void _onResults(JSAny? results) {
    if (!_running) return;
    if (results == null) return;
    final rawLandmarks = js_util.getProperty<Object?>(results, 'poseLandmarks');
    if (rawLandmarks == null) return;

    final rawLength = js_util.getProperty<Object?>(rawLandmarks, 'length');
    final length = rawLength is num ? rawLength.toInt() : 0;
    if (length == 0) return;

    final frame = <String, dynamic>{};
    final count = length < _landmarkNames.length
        ? length
        : _landmarkNames.length;
    for (var index = 0; index < count; index++) {
      final landmark = _arrayItem(rawLandmarks, index);
      if (landmark == null) continue;
      frame[_landmarkNames[index]] = <String, double>{
        'x': _number(landmark, 'x'),
        'y': _number(landmark, 'y'),
        'z': _number(landmark, 'z'),
        'visibility': _number(landmark, 'visibility'),
      };
    }
    if (frame.isNotEmpty && !_framesController.isClosed) {
      _framesController.add(frame);
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

  void _setError(String message) {
    _lastError = message;
    if (!_errorsController.isClosed) _errorsController.add(message);
  }

  String _cameraErrorMessage(Object error) {
    final text = error.toString().toLowerCase();
    if (text.contains('notallowed') || text.contains('permission')) {
      return 'Camera bị từ chối. Hãy cấp quyền camera rồi thử lại.';
    }
    if (text.contains('notfound') || text.contains('device')) {
      return 'Không tìm thấy camera trên thiết bị này.';
    }
    return 'Không thể khởi động camera realtime. Hãy kiểm tra HTTPS và quyền camera.';
  }
}

PostureCameraSource createPostureCameraSource() => PostureCameraSource();
