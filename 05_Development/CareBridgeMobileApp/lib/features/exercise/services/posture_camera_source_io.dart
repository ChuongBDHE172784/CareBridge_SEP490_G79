import 'dart:async';
import 'dart:io';
import 'package:camera/camera.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:google_mlkit_pose_detection/google_mlkit_pose_detection.dart';

import 'pose_skeleton_painter.dart';

/// Native (Android / iOS) camera & pose detection source powered by Google ML Kit.
class PostureCameraSource {
  static const List<PoseLandmarkType> _orderedLandmarkTypes = <PoseLandmarkType>[
    PoseLandmarkType.nose,
    PoseLandmarkType.leftEyeInner,
    PoseLandmarkType.leftEye,
    PoseLandmarkType.leftEyeOuter,
    PoseLandmarkType.rightEyeInner,
    PoseLandmarkType.rightEye,
    PoseLandmarkType.rightEyeOuter,
    PoseLandmarkType.leftEar,
    PoseLandmarkType.rightEar,
    PoseLandmarkType.leftMouth,
    PoseLandmarkType.rightMouth,
    PoseLandmarkType.leftShoulder,
    PoseLandmarkType.rightShoulder,
    PoseLandmarkType.leftElbow,
    PoseLandmarkType.rightElbow,
    PoseLandmarkType.leftWrist,
    PoseLandmarkType.rightWrist,
    PoseLandmarkType.leftPinky,
    PoseLandmarkType.rightPinky,
    PoseLandmarkType.leftIndex,
    PoseLandmarkType.rightIndex,
    PoseLandmarkType.leftThumb,
    PoseLandmarkType.rightThumb,
    PoseLandmarkType.leftHip,
    PoseLandmarkType.rightHip,
    PoseLandmarkType.leftKnee,
    PoseLandmarkType.rightKnee,
    PoseLandmarkType.leftAnkle,
    PoseLandmarkType.rightAnkle,
    PoseLandmarkType.leftHeel,
    PoseLandmarkType.rightHeel,
    PoseLandmarkType.leftFootIndex,
    PoseLandmarkType.rightFootIndex,
  ];

  static const Map<PoseLandmarkType, String> _landmarkNames = <PoseLandmarkType, String>{
    PoseLandmarkType.nose: 'nose',
    PoseLandmarkType.leftEyeInner: 'left_eye_inner',
    PoseLandmarkType.leftEye: 'left_eye',
    PoseLandmarkType.leftEyeOuter: 'left_eye_outer',
    PoseLandmarkType.rightEyeInner: 'right_eye_inner',
    PoseLandmarkType.rightEye: 'right_eye',
    PoseLandmarkType.rightEyeOuter: 'right_eye_outer',
    PoseLandmarkType.leftEar: 'left_ear',
    PoseLandmarkType.rightEar: 'right_ear',
    PoseLandmarkType.leftMouth: 'mouth_left',
    PoseLandmarkType.rightMouth: 'mouth_right',
    PoseLandmarkType.leftShoulder: 'left_shoulder',
    PoseLandmarkType.rightShoulder: 'right_shoulder',
    PoseLandmarkType.leftElbow: 'left_elbow',
    PoseLandmarkType.rightElbow: 'right_elbow',
    PoseLandmarkType.leftWrist: 'left_wrist',
    PoseLandmarkType.rightWrist: 'right_wrist',
    PoseLandmarkType.leftPinky: 'left_pinky',
    PoseLandmarkType.rightPinky: 'right_pinky',
    PoseLandmarkType.leftIndex: 'left_index',
    PoseLandmarkType.rightIndex: 'right_index',
    PoseLandmarkType.leftThumb: 'left_thumb',
    PoseLandmarkType.rightThumb: 'right_thumb',
    PoseLandmarkType.leftHip: 'left_hip',
    PoseLandmarkType.rightHip: 'right_hip',
    PoseLandmarkType.leftKnee: 'left_knee',
    PoseLandmarkType.rightKnee: 'right_knee',
    PoseLandmarkType.leftAnkle: 'left_ankle',
    PoseLandmarkType.rightAnkle: 'right_ankle',
    PoseLandmarkType.leftHeel: 'left_heel',
    PoseLandmarkType.rightHeel: 'right_heel',
    PoseLandmarkType.leftFootIndex: 'left_foot_index',
    PoseLandmarkType.rightFootIndex: 'right_foot_index',
  };

  final _framesController = StreamController<Map<String, dynamic>>.broadcast();
  final _errorsController = StreamController<String>.broadcast();
  final ValueNotifier<List<PosePoint?>> _overlayPointsNotifier =
      ValueNotifier<List<PosePoint?>>(<PosePoint?>[]);
  final ValueNotifier<bool> _errorNotifier = ValueNotifier<bool>(false);
  final ValueNotifier<CameraController?> _cameraControllerNotifier =
      ValueNotifier<CameraController?>(null);

  CameraController? _cameraController;
  PoseDetector? _poseDetector;
  CameraDescription? _cameraDescription;
  bool _isProcessingFrame = false;
  bool _running = false;
  bool _starting = false;
  bool _isSwitching = false;
  String? _lastError;
  bool _feedbackError = false;

  Stream<Map<String, dynamic>> get frames => _framesController.stream;
  Stream<String> get errors => _errorsController.stream;
  bool get isSupported => true;
  bool get isRunning => _running;
  bool get isSwitching => _isSwitching;
  String? get lastError => _lastError;
  bool get hasFeedbackError => _feedbackError;
  bool get isFrontCamera =>
      _cameraDescription?.lensDirection == CameraLensDirection.front;

  void setFeedbackError(bool value) {
    _feedbackError = value;
    _errorNotifier.value = value;
  }

  void setFeedbackWarning(bool value) => setFeedbackError(value);

  Future<void> start() async {
    if (_running || _starting) return;
    _starting = true;
    _lastError = null;

    try {
      final cameras = await availableCameras();
      if (cameras.isEmpty) {
        throw Exception('Không tìm thấy camera trên thiết bị.');
      }

      // Prioritize front camera for exercise posture checking
      final frontCamera = cameras.firstWhere(
        (c) => c.lensDirection == CameraLensDirection.front,
        orElse: () => cameras.first,
      );
      _cameraDescription = frontCamera;

      final controller = CameraController(
        frontCamera,
        ResolutionPreset.medium,
        enableAudio: false,
        imageFormatGroup: Platform.isAndroid
            ? ImageFormatGroup.nv21
            : ImageFormatGroup.bgra8888,
      );
      _cameraController = controller;
      await controller.initialize();
      _cameraControllerNotifier.value = controller;

      _poseDetector = PoseDetector(
        options: PoseDetectorOptions(
          mode: PoseDetectionMode.stream,
          model: PoseDetectionModel.base,
        ),
      );

      _running = true;
      _starting = false;

      await controller.startImageStream(_processImageStream);
    } catch (e) {
      _starting = false;
      _running = false;
      final msg = 'Lỗi khởi động camera: ${e.toString()}';
      _lastError = msg;
      if (!_errorsController.isClosed) {
        _errorsController.add(msg);
      }
    }
  }

  Future<void> switchCamera() async {
    if (_starting || _isSwitching) return;
    _isSwitching = true;
    try {
      final cameras = await availableCameras();
      if (cameras.length < 2) return;

      final currentLens = _cameraDescription?.lensDirection;
      final targetLens = currentLens == CameraLensDirection.front
          ? CameraLensDirection.back
          : CameraLensDirection.front;

      final nextCamera = cameras.firstWhere(
        (c) => c.lensDirection == targetLens,
        orElse: () => cameras.firstWhere(
          (c) => c != _cameraDescription,
          orElse: () => cameras.first,
        ),
      );

      if (nextCamera == _cameraDescription) return;

      _overlayPointsNotifier.value = <PosePoint?>[];

      // Safely detach old controller so UI unmounts old CameraPreview texture
      final oldController = _cameraController;
      _cameraController = null;
      _cameraControllerNotifier.value = null;

      if (oldController != null) {
        if (oldController.value.isStreamingImages) {
          try {
            await oldController.stopImageStream();
          } catch (_) {}
        }
        // Small delay to ensure any in-flight _processImageStream finishes
        while (_isProcessingFrame) {
          await Future<void>.delayed(const Duration(milliseconds: 10));
        }
        try {
          await oldController.dispose();
        } catch (_) {}
      }

      _cameraDescription = nextCamera;

      final controller = CameraController(
        nextCamera,
        ResolutionPreset.medium,
        enableAudio: false,
        imageFormatGroup: Platform.isAndroid
            ? ImageFormatGroup.nv21
            : ImageFormatGroup.bgra8888,
      );
      await controller.initialize();
      _cameraController = controller;
      _cameraControllerNotifier.value = controller;

      if (_running) {
        await controller.startImageStream(_processImageStream);
      }
    } catch (e) {
      debugPrint('Error switching camera: $e');
    } finally {
      _isSwitching = false;
    }
  }

  void _processImageStream(CameraImage image) async {
    if (!_running || _isProcessingFrame || _poseDetector == null) return;
    _isProcessingFrame = true;

    try {
      final inputImage = _inputImageFromCameraImage(image);
      if (inputImage == null) return;

      final poses = await _poseDetector!.processImage(inputImage);
      if (!_running) return;

      if (poses.isEmpty) {
        _overlayPointsNotifier.value = <PosePoint?>[];
        return;
      }

      final pose = poses.first;
      
      // Calculate normalized image dimensions considering camera sensor rotation.
      // On portrait phones, sensor orientation is usually 90 or 270 degrees.
      // ML Kit outputs landmark (x, y) in the rotated coordinate space.
      final sensorOrientation = _cameraDescription?.sensorOrientation ?? 0;
      final rotation = InputImageRotationValue.fromRawValue(sensorOrientation) ??
          InputImageRotation.rotation0deg;
      final bool isRotated = rotation == InputImageRotation.rotation90deg ||
          rotation == InputImageRotation.rotation270deg;

      final double imageWidth =
          isRotated ? image.height.toDouble() : image.width.toDouble();
      final double imageHeight =
          isRotated ? image.width.toDouble() : image.height.toDouble();

      final frame = <String, dynamic>{};
      final overlayPoints = <PosePoint?>[];

      for (final landmarkType in _orderedLandmarkTypes) {
        final landmark = pose.landmarks[landmarkType];
        final name = _landmarkNames[landmarkType];

        if (landmark == null || name == null) {
          overlayPoints.add(null);
          continue;
        }

        final normX = (landmark.x / imageWidth).clamp(0.0, 1.0);
        final normY = (landmark.y / imageHeight).clamp(0.0, 1.0);
        final normZ = landmark.z / imageWidth;
        final visibility = landmark.likelihood;

        overlayPoints.add(
          PosePoint(
            x: normX,
            y: normY,
            visibility: visibility,
          ),
        );

        frame[name] = <String, double>{
          'x': normX,
          'y': normY,
          'z': normZ,
          'visibility': visibility,
        };
      }

      _overlayPointsNotifier.value = overlayPoints;

      if (frame.isNotEmpty && !_framesController.isClosed) {
        _framesController.add(frame);
      }
    } catch (e) {
      debugPrint('Posture ML Kit frame analysis failed: $e');
    } finally {
      _isProcessingFrame = false;
    }
  }

  InputImage? _inputImageFromCameraImage(CameraImage image) {
    if (_cameraDescription == null) return null;

    final sensorOrientation = _cameraDescription!.sensorOrientation;
    final rotation = InputImageRotationValue.fromRawValue(sensorOrientation) ??
        InputImageRotation.rotation0deg;

    final format = InputImageFormatValue.fromRawValue(image.format.raw) ??
        (Platform.isAndroid ? InputImageFormat.nv21 : InputImageFormat.bgra8888);

    final plane = image.planes.first;

    // Concatenate bytes for multi-plane image formats on Android if necessary
    Uint8List bytes;
    if (image.planes.length == 1) {
      bytes = plane.bytes;
    } else {
      final allBytes = WriteBuffer();
      for (final p in image.planes) {
        allBytes.putUint8List(p.bytes);
      }
      bytes = allBytes.done().buffer.asUint8List();
    }

    return InputImage.fromBytes(
      bytes: bytes,
      metadata: InputImageMetadata(
        size: Size(image.width.toDouble(), image.height.toDouble()),
        rotation: rotation,
        format: format,
        bytesPerRow: plane.bytesPerRow,
      ),
    );
  }

  Future<void> stop() async {
    _running = false;
    _starting = false;
    _isSwitching = false;
    _overlayPointsNotifier.value = <PosePoint?>[];

    final controller = _cameraController;
    _cameraController = null;
    _cameraControllerNotifier.value = null;

    if (controller != null) {
      try {
        if (controller.value.isStreamingImages) {
          await controller.stopImageStream();
        }
      } catch (_) {}

      try {
        await controller.dispose();
      } catch (_) {}
    }

    try {
      await _poseDetector?.close();
    } catch (_) {}
    _poseDetector = null;
  }

  Future<void> dispose() async {
    await stop();
    _cameraControllerNotifier.dispose();
    _overlayPointsNotifier.dispose();
    _errorNotifier.dispose();
    if (!_framesController.isClosed) {
      await _framesController.close();
    }
    if (!_errorsController.isClosed) {
      await _errorsController.close();
    }
  }

  Widget buildPreview() {
    return ValueListenableBuilder<CameraController?>(
      valueListenable: _cameraControllerNotifier,
      builder: (context, controller, _) {
        if (controller == null || !controller.value.isInitialized) {
          return const ColoredBox(
            color: Colors.black,
            child: Center(
              child: CircularProgressIndicator(color: Colors.white70),
            ),
          );
        }

        final size = controller.value.previewSize;
        // On portrait mobile devices, the camera sensor width is the longer dimension (e.g. 1280)
        // and height is the shorter dimension (e.g. 720).
        final isSensorLandscape = size != null && size.width > size.height;
        final double previewWidth =
            size != null ? (isSensorLandscape ? size.height : size.width) : 720;
        final double previewHeight =
            size != null ? (isSensorLandscape ? size.width : size.height) : 1280;

        return ClipRect(
          child: Container(
            color: Colors.black,
            alignment: Alignment.center,
            child: FittedBox(
              fit: BoxFit.cover,
              child: SizedBox(
                width: previewWidth,
                height: previewHeight,
                child: Stack(
                  fit: StackFit.expand,
                  children: [
                    CameraPreview(
                      controller,
                      key: ValueKey<int>(controller.cameraId),
                    ),
                    ValueListenableBuilder<bool>(
                      valueListenable: _errorNotifier,
                      builder: (context, hasError, _) {
                        return ValueListenableBuilder<List<PosePoint?>>(
                          valueListenable: _overlayPointsNotifier,
                          builder: (context, points, _) {
                            return CustomPaint(
                              painter: PoseSkeletonPainter(
                                points: points,
                                hasError: hasError,
                                isFrontCamera: isFrontCamera,
                              ),
                            );
                          },
                        );
                      },
                    ),
                  ],
                ),
              ),
            ),
          ),
        );
      },
    );
  }
}

PostureCameraSource createPostureCameraSource() => PostureCameraSource();
