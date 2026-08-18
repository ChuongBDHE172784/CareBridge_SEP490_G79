import 'dart:async';
import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';
import '../models/posture_event_model.dart';
import '../services/exercise_service.dart';
import '../services/exercise_feedback_analyzer.dart';
import '../services/exercise_voice_feedback.dart';
import '../services/posture_camera_source.dart';
import '../services/posture_event_streamer.dart';
import 'exercise_session_result_screen.dart';

class ExerciseSessionScreen extends StatefulWidget {
  final String exerciseId;
  final String exerciseTitle;
  final String instruction;
  final String? mediaUrl;
  final String? safetyWarning;
  final int durationMinutes;
  final String sessionId;
  final String initialStatus;
  final DateTime initialStartedAt;

  /// Optional output from a camera/pose extractor.
  final Stream<Map<String, dynamic>>? postureLandmarkFrames;

  /// Starts the platform-selected camera source when no injected frame stream
  /// is provided. The caller should set this only after camera consent.
  final bool enableRealtimePostureCamera;

  /// Injectable source for deterministic tests or another pose provider.
  final PostureCameraSource? postureCameraSource;

  /// Injectable boundary so widget tests never invoke the platform TTS plugin.
  final ExerciseVoiceFeedback? voiceFeedback;

  const ExerciseSessionScreen({
    super.key,
    required this.exerciseId,
    required this.exerciseTitle,
    required this.instruction,
    this.mediaUrl,
    this.safetyWarning,
    required this.durationMinutes,
    required this.sessionId,
    this.initialStatus = 'IN_PROGRESS',
    required this.initialStartedAt,
    this.postureLandmarkFrames,
    this.enableRealtimePostureCamera = false,
    this.postureCameraSource,
    this.voiceFeedback,
  });

  @override
  State<ExerciseSessionScreen> createState() => _ExerciseSessionScreenState();
}

class _ExerciseSessionScreenState extends State<ExerciseSessionScreen> {
  static const _canvas = Color(0xFFFFF8F6);
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _surfaceLowest = Color(0xFFFFFFFF);
  static const _surfaceVariant = Color(0xFFFADCD3);

  Timer? _timer;
  int _elapsedSeconds = 0;
  bool _isPaused = false;
  bool _isCompleting = false;
  String _postureStatus = 'Chưa có dữ liệu tư thế';
  bool _postureGood = false;
  PostureEventStreamer? _postureStreamer;
  StreamSubscription<Map<String, dynamic>>? _postureFramesSubscription;
  PostureCameraSource? _cameraSource;
  StreamSubscription<String>? _cameraErrorsSubscription;
  String? _cameraError;
  bool _cameraStarted = false;
  bool _cameraStarting = false;
  bool _pauseChanging = false;
  bool _isSwitchingCamera = false;
  late final ExerciseFeedbackAnalyzer _feedbackAnalyzer;
  late final ExerciseVoiceFeedbackAnnouncer _voiceFeedbackAnnouncer;
  ExerciseFeedbackMetrics? _latestFeedbackMetrics;
  DateTime? _lastMetricsUiUpdateAt;
  bool _hasValidMetricsFrame = false;

  int get _totalSeconds => widget.durationMinutes * 60;

  String _fmt(int s) {
    final m = (s ~/ 60).toString().padLeft(2, '0');
    final sec = (s % 60).toString().padLeft(2, '0');
    return '$m:$sec';
  }

  @override
  void initState() {
    super.initState();
    _feedbackAnalyzer = ExerciseFeedbackAnalyzer(
      exerciseId: widget.exerciseId,
      exerciseTitle: widget.exerciseTitle,
    );
    _voiceFeedbackAnnouncer = ExerciseVoiceFeedbackAnnouncer(
      voice: widget.voiceFeedback ?? SystemExerciseVoiceFeedback(),
    );
    _isPaused = widget.initialStatus.toUpperCase() == 'PAUSED';
    _elapsedSeconds = DateTime.now()
        .difference(widget.initialStartedAt)
        .inSeconds
        .clamp(0, _totalSeconds)
        .toInt();
    final frames = widget.postureLandmarkFrames;
    if (frames != null) {
      _startPostureTransport(frames);
    } else if (widget.enableRealtimePostureCamera) {
      unawaited(_startCameraSource());
    }
    _startTimer();
  }

  Future<void> _startCameraSource() async {
    if (_cameraStarting) return;
    _cameraStarting = true;
    final source =
        _cameraSource ??
        widget.postureCameraSource ??
        createPostureCameraSource();
    if (_cameraSource == null) {
      _cameraSource = source;
      _cameraErrorsSubscription = source.errors.listen((message) {
        if (!mounted) return;
        setState(() {
          _cameraError = message;
          // MediaPipe errors are non-fatal for the local camera preview.
          _cameraStarted = source.isRunning;
          if (!source.isRunning) {
            _postureGood = false;
            _postureStatus = 'Chưa có dữ liệu camera';
          }
        });
      });
    }

    if (mounted) {
      setState(() {
        _cameraError = null;
        _cameraStarted = source.isRunning;
      });
    }

    if (!source.isSupported) {
      if (mounted) {
        setState(() {
          _cameraError =
              'Phân tích camera realtime hiện chỉ hỗ trợ trên Flutter Web.';
        });
      }
      _cameraStarting = false;
      return;
    }

    try {
      await source.start();
      if (!mounted) {
        await source.stop();
        return;
      }
      setState(() {
        _cameraStarted = source.isRunning;
        _postureStatus = 'Đang nhận diện tư thế...';
      });
      if (_postureStreamer == null) {
        _startPostureTransport(source.frames);
      }
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _cameraError = source.lastError ?? _postureErrorText(error);
        _cameraStarted = false;
      });
    } finally {
      _cameraStarting = false;
    }
  }

  Future<void> _retryCameraSource() async {
    if (_cameraStarting) return;
    await _cameraSource?.stop();
    if (!mounted) return;
    setState(() {
      _cameraError = null;
      _cameraStarted = false;
      _postureGood = false;
      _postureStatus = 'Đang kết nối camera...';
    });
    _cameraSource?.setFeedbackError(false);
    await _startCameraSource();
  }

  /// [BƯỚC 1: Khởi động đường truyền Stream dữ liệu tư thế lên Backend]
  /// Sử dụng PostureEventStreamer để điều phối gửi mốc cơ thể (Landmarks) với tần suất tối đa 10 req/s,
  /// tự động bỏ qua (drop) các frame cũ bị chậm để tránh tắc nghẽn mạng.
  void _startPostureTransport(Stream<Map<String, dynamic>> frames) {
    final streamer = PostureEventStreamer(
      send: (eventTimeMs, landmarks) =>
          ExerciseService.instance.analyzePostureEvent(
            sessionId: widget.sessionId,
            eventTimeMs: eventTimeMs,
            landmarks: landmarks,
          ),
      onFeedback: _applyPostureFeedback,
      onError: (error, _) {
        if (!mounted) return;
        setState(() {
          _postureGood = false;
          _postureStatus = _postureErrorText(error);
        });
      },
    )..start();
    _postureStreamer = streamer;

    // Lắng nghe từng frame ảnh từ Camera MediaPipe
    _postureFramesSubscription = frames.listen((rawFrame) {
      if (!mounted || _isPaused || _isCompleting) return;
      final landmarks = _parseLandmarks(rawFrame);
      if (landmarks.isEmpty) return;

      // Phân tích nhanh cục bộ các chỉ số góc khớp để vẽ khung xương thời gian thực
      final metrics = _feedbackAnalyzer.analyze(landmarks);
      if (!mounted || _isPaused || _isCompleting) return;
      _latestFeedbackMetrics = metrics;
      _hasValidMetricsFrame =
          _hasValidMetricsFrame || metrics.hasVisibleLandmarks;
      final now = DateTime.now();

      // Giới hạn tần suất cập nhật UI cục bộ mỗi 100ms
      final lastUiUpdate = _lastMetricsUiUpdateAt;
      if (lastUiUpdate == null ||
          now.difference(lastUiUpdate) >= const Duration(milliseconds: 100)) {
        _lastMetricsUiUpdateAt = now;
        setState(() {});
      }

      // Đẩy mốc cơ thể vào hàng đợi gửi Backend
      streamer.push(
        eventTimeMs: DateTime.now().millisecondsSinceEpoch,
        landmarks: landmarks,
      );
    });
  }

  /// Phân tích mốc tọa độ chuẩn hóa MediaPipe từ JSON của camera
  Map<String, PostureLandmark> _parseLandmarks(Map<String, dynamic> rawFrame) {
    final parsed = <String, PostureLandmark>{};
    for (final entry in rawFrame.entries) {
      final value = entry.value;
      if (value is! Map<String, dynamic>) continue;
      try {
        parsed[entry.key] = PostureLandmark.fromPoseJson(value);
      } on FormatException {
        // Bỏ qua mốc không hợp lệ / không nhận diện được
      }
    }
    return parsed;
  }

  /// [BƯỚC 5: Hứng phản hồi từ Backend & Cập nhật State giao diện/Giọng nói]
  void _applyPostureFeedback(PostureFeedback feedback) {
    if (!mounted || _isPaused || _isCompleting) return;
    final severity = feedback.severity.toUpperCase();
    final code = feedback.postureCode.toUpperCase();
    _feedbackAnalyzer.applyFeedback(feedback);

    // Xác định tư thế có đang đúng chuẩn hay không
    final isGood = code == 'C' ||
        code.endsWith('/C') ||
        code.contains('GOOD_FORM') ||
        code.contains('CORRECT') ||
        code == 'UP' ||
        code == 'DOWN';

    // Cảnh báo nếu mức độ nghiêm trọng là WARNING / CRITICAL
    final isWarning = !isGood &&
        (severity == 'CRITICAL' ||
            (severity == 'WARNING' && !code.contains('MODEL_UNAVAILABLE')) ||
            _feedbackAnalyzer.hasFeedbackError);

    // Cập nhật màu viền camera (Đỏ nếu sai tư thế, Xanh nếu đúng)
    _cameraSource?.setFeedbackError(isWarning);
    final feedbackText = feedback.feedbackText?.trim();

    final statusText = feedbackText?.isNotEmpty == true
        ? feedbackText!
        : (isWarning ? 'Tư thế chưa chuẩn, hãy điều chỉnh' : 'Tư thế tốt');

    // Cập nhật State UI
    setState(() {
      _postureGood = !isWarning;
      _postureStatus = statusText;
    });

    // Phát âm thanh hướng dẫn bằng giọng nói (TTS) tiếng Việt nếu có cảnh báo lỗi tư thế
    if (isWarning && feedbackText != null && feedbackText.isNotEmpty) {
      unawaited(_voiceFeedbackAnnouncer.announce(feedbackText));
    }
  }

  String _postureErrorText(Object error) {
    if (error is ApiException) {
      return 'Không thể nhận phản hồi tư thế (${error.statusCode}).';
    }
    return 'Không thể kết nối phân tích tư thế.';
  }

  void _startTimer() {
    _timer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (_isPaused) return;
      setState(() => _elapsedSeconds++);
      if (_elapsedSeconds >= _totalSeconds) {
        _timer?.cancel();
        _completeSession();
      }
    });
  }

  Future<void> _togglePause() async {
    if (_pauseChanging || _isCompleting) return;
    final pausing = !_isPaused;
    _pauseChanging = true;
    if (mounted) setState(() {});

    final streamer = _postureStreamer;
    try {
      // Invalidate any in-flight response before a pause can be followed by a
      // resume. PostureEventStreamer suppresses callbacks after stop(); the
      // same instance can be started again once the server accepts resume.
      if (pausing) {
        await _voiceFeedbackAnnouncer.stop();
        await streamer?.stop();
        await ExerciseService.instance.pauseSession(widget.sessionId);
        if (mounted) {
          setState(() => _isPaused = true);
        }
      } else {
        await ExerciseService.instance.resumeSession(widget.sessionId);
        if (mounted) {
          setState(() => _isPaused = false);
        }
        if (!_isCompleting) streamer?.start();
      }
    } catch (error) {
      if (pausing && !_isCompleting) {
        streamer?.start();
      } else if (!pausing) {
        await streamer?.stop();
      }
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              pausing
                  ? 'Không thể tạm dừng bài tập. Vui lòng thử lại.'
                  : 'Không thể tiếp tục bài tập. Vui lòng thử lại.',
              style: const TextStyle(fontFamily: 'Lexend'),
            ),
            duration: const Duration(seconds: 2),
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _pauseChanging = false);
      } else {
        _pauseChanging = false;
      }
    }
  }

  Future<void> _stopSession() async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text(
          'Dừng bài tập?',
          style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w600),
        ),
        content: const Text(
          'Bài tập sẽ được tính là chưa hoàn thành.',
          style: TextStyle(fontFamily: 'Lexend'),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Tiếp tục tập'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            style: FilledButton.styleFrom(backgroundColor: _primary),
            child: const Text(
              'Dừng lại',
              style: TextStyle(color: Colors.white, fontFamily: 'Lexend'),
            ),
          ),
        ],
      ),
    );
    if (confirm != true) return;
    _timer?.cancel();
    await _completeSession();
  }

  Future<void> _completeSession() async {
    if (_isCompleting) return;
    if (mounted) {
      setState(() => _isCompleting = true);
    }
    await _voiceFeedbackAnnouncer.stop();
    _postureStreamer?.dispose();
    await _postureFramesSubscription?.cancel();
    _postureFramesSubscription = null;
    await _cameraSource?.stop();
    _cameraStarted = false;
    try {
      final result = await ExerciseService.instance.completeSession(
        widget.sessionId,
      );
      if (!mounted) return;
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(
          builder: (_) => ExerciseSessionResultScreen(
            result: result,
            feedbackSnapshot: _hasValidMetricsFrame
                ? _feedbackAnalyzer.snapshot
                : null,
          ),
        ),
      );
    } on ApiException {
      if (mounted) {
        setState(() => _isCompleting = false);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Không thể hoàn thành bài tập. Thử lại.'),
          ),
        );
      }
    }
  }

  @override
  void dispose() {
    _timer?.cancel();
    _postureStreamer?.dispose();
    _postureFramesSubscription?.cancel();
    _cameraErrorsSubscription?.cancel();
    unawaited(_cameraSource?.dispose());
    unawaited(_voiceFeedbackAnnouncer.dispose());
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final progress = (_totalSeconds > 0)
        ? (_elapsedSeconds / _totalSeconds).clamp(0.0, 1.0)
        : 0.0;

    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: Column(
          children: [
            _buildAppBar(),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: Column(
                  children: [
                    const SizedBox(height: 12),
                    _buildProgressCard(progress),
                    const SizedBox(height: 16),
                    _buildMediaCard(),
                    if (_latestFeedbackMetrics != null)
                      _buildMetricsCard(_latestFeedbackMetrics!),
                    const SizedBox(height: 24),
                  ],
                ),
              ),
            ),
            _buildControls(),
          ],
        ),
      ),
    );
  }

  Widget _buildMetricsCard(ExerciseFeedbackMetrics metrics) {
    // The upstream plank detector is a hold/error classifier, not a
    // repetition/angle detector. Keep the screen from presenting a misleading
    // empty metric card for it.
    if (!metrics.isSupported ||
        metrics.exercise == ExerciseFeedbackExercise.plank) {
      return const SizedBox.shrink();
    }
    final angleEntries = metrics.angles.entries.toList(growable: false);
    final isBicep = metrics.exercise == ExerciseFeedbackExercise.bicepCurl;
    final countLabel = isBicep ? 'SỐ LẦN (TAY)' : 'SỐ LẦN';
    final count = metrics.repetitions;
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.only(top: 16),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: _surfaceLowest,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: _surfaceVariant),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.insights_outlined, size: 18, color: _primary),
              const SizedBox(width: 8),
              const Expanded(
                child: Text(
                  'Chỉ số realtime',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 15,
                    fontWeight: FontWeight.w700,
                    color: _onSurface,
                  ),
                ),
              ),
              if (metrics.feedbackError)
                const Icon(
                  Icons.warning_amber_rounded,
                  size: 18,
                  color: Colors.orange,
                ),
            ],
          ),
          if (!metrics.hasVisibleLandmarks) ...[
            const SizedBox(height: 8),
            const Text(
              'Đưa đủ khớp vào khung hình để xem chỉ số.',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 12,
                color: _onSurfaceVariant,
              ),
            ),
          ] else ...[
            const SizedBox(height: 12),
            Wrap(
              spacing: 10,
              runSpacing: 8,
              children: [
                _metricChip(label: countLabel, value: '$count'),
                if (isBicep) ...[
                  _metricChip(
                    label: 'TAY TRÁI',
                    value: '${metrics.leftBicepRepetitions}',
                  ),
                  _metricChip(
                    label: 'TAY PHẢI',
                    value: '${metrics.rightBicepRepetitions}',
                  ),
                ],
                for (final entry in angleEntries)
                  _metricChip(
                    label: _angleLabel(entry.key),
                    value: '${entry.value.round()}°',
                  ),
              ],
            ),
          ],
        ],
      ),
    );
  }

  Widget _metricChip({required String label, required String value}) {
    return DecoratedBox(
      decoration: BoxDecoration(
        color: _canvas,
        borderRadius: BorderRadius.circular(10),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              label,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 10,
                letterSpacing: 0.4,
                color: _onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 2),
            Text(
              value,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 17,
                fontWeight: FontWeight.w700,
                color: _primary,
              ),
            ),
          ],
        ),
      ),
    );
  }

  String _angleLabel(String key) {
    switch (key) {
      case 'left_elbow':
        return 'KHUỶU TRÁI';
      case 'right_elbow':
        return 'KHUỶU PHẢI';
      case 'left_knee':
        return 'GỐI TRÁI';
      case 'right_knee':
        return 'GỐI PHẢI';
      default:
        return 'GÓC';
    }
  }

  Widget _buildAppBar() {
    return Container(
      height: 56,
      padding: const EdgeInsets.symmetric(horizontal: 8),
      color: _canvas,
      child: Row(
        children: [
          SizedBox(
            width: 48,
            height: 48,
            child: IconButton(
              onPressed: () => Navigator.of(context).pop(),
              icon: const Icon(Icons.arrow_back, color: _primary),
              padding: EdgeInsets.zero,
            ),
          ),
          const Expanded(
            child: Text(
              'Ứng dụng Mẹ và Bé',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 18,
                fontWeight: FontWeight.w600,
                color: _primary,
              ),
            ),
          ),
          SizedBox(
            width: 48,
            height: 48,
            child: IconButton(
              onPressed: _stopSession,
              icon: const Icon(Icons.close, color: _onSurfaceVariant),
              padding: EdgeInsets.zero,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildProgressCard(double progress) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surfaceLowest,
        borderRadius: BorderRadius.circular(16),
        boxShadow: const [
          BoxShadow(
            color: Color.fromRGBO(90, 70, 63, 0.06),
            blurRadius: 20,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            widget.exerciseTitle,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 20,
              fontWeight: FontWeight.w700,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            widget.instruction,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 16),
          Center(
            child: Text(
              _fmt(_elapsedSeconds),
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 40,
                fontWeight: FontWeight.w700,
                color: _primaryContainer,
              ),
            ),
          ),
          const SizedBox(height: 8),
          ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: LinearProgressIndicator(
              value: progress,
              minHeight: 6,
              backgroundColor: const Color(0xFFEDE0DB),
              valueColor: const AlwaysStoppedAnimation<Color>(
                _primaryContainer,
              ),
            ),
          ),
          const SizedBox(height: 8),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                _fmt(_elapsedSeconds),
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 12,
                  color: _onSurfaceVariant,
                ),
              ),
              Text(
                _fmt(_totalSeconds),
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 12,
                  color: _onSurfaceVariant,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildMediaCard() {
    final source = _cameraSource;
    final showCamera =
        source?.isSupported == true &&
        (_cameraStarted || source!.isRunning || _cameraError == null);
    final hasSafetyWarning =
        widget.safetyWarning != null && widget.safetyWarning!.isNotEmpty;
    return ClipRRect(
      borderRadius: BorderRadius.circular(20),
      child: Stack(
        children: [
          // Background image / placeholder
          Container(
            width: double.infinity,
            height: 360,
            color: const Color(0xFFD6C2BD),
            child: showCamera
                ? SizedBox.expand(child: _cameraSource!.buildPreview())
                : widget.mediaUrl != null
                ? Image.network(
                    widget.mediaUrl!,
                    fit: BoxFit.cover,
                    errorBuilder: (_, _, _) => const Center(
                      child: Icon(
                        Icons.image_not_supported,
                        color: Colors.white54,
                        size: 64,
                      ),
                    ),
                  )
                : const Center(
                    child: Icon(
                      Icons.sports_gymnastics,
                      color: Colors.white54,
                      size: 80,
                    ),
                  ),
          ),

          if (_cameraError != null)
            Positioned(
              left: 16,
              right: 16,
              bottom: hasSafetyWarning ? 68 : 16,
              child: DecoratedBox(
                decoration: BoxDecoration(
                  color: Colors.black.withValues(alpha: 0.72),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Row(
                    children: [
                      Expanded(
                        child: Text(
                          _cameraError!,
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 12,
                            color: Colors.white,
                          ),
                        ),
                      ),
                      const SizedBox(width: 8),
                      TextButton(
                        onPressed: _cameraStarting
                            ? null
                            : () => unawaited(_retryCameraSource()),
                        child: Text(
                          _cameraStarting ? 'Đang mở...' : 'Thử lại',
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            color: Colors.white,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),

          // Posture status badge
          Positioned(
            top: 16,
            left: 16,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
              decoration: BoxDecoration(
                color: Colors.white.withValues(alpha: 0.9),
                borderRadius: BorderRadius.circular(9999),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(
                    _postureGood
                        ? Icons.check_circle_outline
                        : Icons.warning_amber_outlined,
                    color: _postureGood ? Colors.green : Colors.orange,
                    size: 16,
                  ),
                  const SizedBox(width: 6),
                  Text(
                    _postureStatus,
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                      color: _postureGood ? _primary : Colors.orange.shade800,
                    ),
                  ),
                ],
              ),
            ),
          ),

          // Camera flip button
          Positioned(
            top: 16,
            right: 16,
            child: Material(
              color: Colors.transparent,
              child: InkWell(
                onTap: _isSwitchingCamera
                    ? null
                    : () async {
                        setState(() => _isSwitchingCamera = true);
                        try {
                          await _cameraSource?.switchCamera();
                        } catch (_) {
                        } finally {
                          if (mounted) {
                            setState(() => _isSwitchingCamera = false);
                          }
                        }
                      },
                borderRadius: BorderRadius.circular(9999),
                child: Container(
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: _isSwitchingCamera ? 0.5 : 0.9),
                    shape: BoxShape.circle,
                  ),
                  child: _isSwitchingCamera
                      ? const Padding(
                          padding: EdgeInsets.all(10),
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: _primary,
                          ),
                        )
                      : const Icon(
                          Icons.flip_camera_ios_outlined,
                          color: _onSurface,
                          size: 20,
                        ),
                ),
              ),
            ),
          ),


          // Safety tip overlay at bottom
          if (hasSafetyWarning)
            Positioned(
              bottom: 0,
              left: 0,
              right: 0,
              child: Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 12,
                ),
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.bottomCenter,
                    end: Alignment.topCenter,
                    colors: [
                      Colors.black.withValues(alpha: 0.5),
                      Colors.transparent,
                    ],
                  ),
                ),
                child: Row(
                  children: [
                    const Icon(
                      Icons.info_outline,
                      color: Colors.white70,
                      size: 16,
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        widget.safetyWarning!,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 13,
                          color: Colors.white,
                          height: 1.4,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildControls() {
    return Container(
      padding: const EdgeInsets.fromLTRB(24, 16, 24, 32),
      color: _canvas,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          // Stop button
          SizedBox(
            width: 52,
            height: 52,
            child: Material(
              color: _surfaceVariant,
              shape: const CircleBorder(),
              child: InkWell(
                customBorder: const CircleBorder(),
                onTap: _stopSession,
                child: const Icon(
                  Icons.stop_rounded,
                  color: Color(0xFFBA1A1A),
                  size: 24,
                ),
              ),
            ),
          ),

          // Pause/Resume button (large center)
          SizedBox(
            width: 72,
            height: 72,
            child: Material(
              color: _primaryContainer,
              shape: const CircleBorder(),
              child: InkWell(
                customBorder: const CircleBorder(),
                onTap: _isCompleting || _pauseChanging ? null : _togglePause,
                child: _isCompleting || _pauseChanging
                    ? const Padding(
                        padding: EdgeInsets.all(20),
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Colors.white,
                        ),
                      )
                    : Icon(
                        _isPaused
                            ? Icons.play_arrow_rounded
                            : Icons.pause_rounded,
                        color: Colors.white,
                        size: 36,
                      ),
              ),
            ),
          ),

          // Skip button
          SizedBox(
            width: 52,
            height: 52,
            child: Material(
              color: _surfaceLowest,
              shape: const CircleBorder(),
              child: InkWell(
                customBorder: const CircleBorder(),
                onTap: _completeSession,
                child: const Icon(
                  Icons.skip_next_rounded,
                  color: _onSurface,
                  size: 24,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
