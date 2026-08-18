import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_tts/flutter_tts.dart';
import 'package:url_launcher/url_launcher.dart';

import '../models/safety_config_model.dart';

/// Các loại hành động phản hồi trong phiên đếm ngược cảnh báo té ngã.
enum SafetyCountdownAction {
  /// Người dùng bấm "Tôi vẫn ổn" -> Xác nhận an toàn và đóng cảnh báo.
  safe,

  /// Người dùng báo phát hiện nhầm (báo động giả).
  falsePositive,

  /// Người dùng bấm nút yêu cầu cứu hộ khẩn cấp.
  help,

  /// Hết thời gian đếm ngược (thường là 30 giây) mà không có phản hồi -> Kích hoạt gửi cảnh báo khẩn cấp tới gia đình.
  timeout,
}

/// Kết quả phản hồi từ màn hình đếm ngược cảnh báo té ngã.
class SafetyCountdownResult {
  const SafetyCountdownResult._(this.action, {this.reasonCode, this.reason});

  const SafetyCountdownResult.safe() : this._(SafetyCountdownAction.safe);

  const SafetyCountdownResult.falsePositive({
    required String reasonCode,
    required String reason,
  }) : this._(
         SafetyCountdownAction.falsePositive,
         reasonCode: reasonCode,
         reason: reason,
       );

  const SafetyCountdownResult.help() : this._(SafetyCountdownAction.help);

  const SafetyCountdownResult.timeout() : this._(SafetyCountdownAction.timeout);

  final SafetyCountdownAction action;
  final String? reasonCode;
  final String? reason;
}

/// Giao diện điều khiển phản hồi xúc giác (Haptic) và âm thanh báo động (TTS/Sound).
abstract class SafetyCountdownFeedback {
  /// Bắt đầu phát âm thanh/rung cảnh báo.
  void start();

  /// Phát xung rung và chuông theo nhịp giây đếm ngược còn lại.
  void pulse(int remainingSeconds);

  /// Dừng phát âm thanh/rung khi kết thúc cảnh báo.
  void stop();
}

class SystemSafetyCountdownFeedback implements SafetyCountdownFeedback {
  static final FlutterTts _sharedTts = FlutterTts();
  static bool _ttsInitialized = false;
  static Timer? _speechTimer;
  static bool _active = false;

  static Future<void> _initTts() async {
    if (_ttsInitialized) return;
    try {
      try {
        await _sharedTts.setLanguage('vi-VN');
      } catch (_) {}
      try {
        await _sharedTts.setSpeechRate(0.48);
      } catch (_) {}
      try {
        await _sharedTts.setVolume(1.0);
      } catch (_) {}

      // Android specific attributes (ignored on iOS)
      try {
        await _sharedTts.setAudioAttributesForNavigation();
      } catch (_) {}

      // iOS specific audio category to ensure loud speaker output even in silent mode
      try {
        await _sharedTts.setIosAudioCategory(
          IosTextToSpeechAudioCategory.playback,
          [
            IosTextToSpeechAudioCategoryOptions.defaultToSpeaker,
            IosTextToSpeechAudioCategoryOptions.duckOthers,
          ],
          IosTextToSpeechAudioMode.defaultMode,
        );
      } catch (_) {}

      _ttsInitialized = true;
    } catch (_) {
      _ttsInitialized = false;
    }
  }

  static Future<void> _speakPhrase() async {
    if (!_active) return;
    try {
      if (!_ttsInitialized) {
        await _initTts();
      }
      if (_active) {
        await _sharedTts.speak('Bạn có ổn không');
      }
    } catch (_) {}
  }

  static void _triggerStrongVibration() {
    try {
      unawaited(HapticFeedback.vibrate());
      unawaited(HapticFeedback.heavyImpact());
      Future.delayed(const Duration(milliseconds: 150), () {
        if (_active) {
          unawaited(HapticFeedback.vibrate());
          unawaited(HapticFeedback.heavyImpact());
        }
      });
      Future.delayed(const Duration(milliseconds: 300), () {
        if (_active) {
          unawaited(HapticFeedback.vibrate());
          unawaited(HapticFeedback.heavyImpact());
        }
      });
    } catch (_) {
      unawaited(HapticFeedback.vibrate());
    }
  }

  @override
  void start() {
    _active = true;
    _speechTimer?.cancel();
    unawaited(_speakPhrase());
    _speechTimer = Timer.periodic(const Duration(milliseconds: 2500), (_) {
      if (_active) {
        unawaited(_speakPhrase());
      } else {
        _speechTimer?.cancel();
        _speechTimer = null;
      }
    });
  }

  @override
  void pulse(int remainingSeconds) {
    if (!_active || !_shouldPulse(remainingSeconds)) return;
    _triggerStrongVibration();
    unawaited(SystemSound.play(SystemSoundType.alert));
  }

  bool _shouldPulse(int remainingSeconds) {
    if (remainingSeconds <= 10) return true;
    if (remainingSeconds <= 20) return remainingSeconds.isEven;
    return remainingSeconds % 5 == 0;
  }

  @override
  void stop() {
    stopAll();
  }

  static void stopAll() {
    _active = false;
    _speechTimer?.cancel();
    _speechTimer = null;
    try {
      _sharedTts.stop().catchError((_) => null);
    } catch (_) {}
  }
}

/// Global guard ensuring that at most ONE SafetyCountdownSheet is presented across the app.
class SafetyCountdownGuard {
  static bool isShowing = false;
  static String? activeEventId;

  static void reset() {
    isShowing = false;
    activeEventId = null;
  }
}

class SafetyCountdownSheet extends StatefulWidget {
  SafetyCountdownSheet({
    super.key,
    required this.event,
    SafetyCountdownFeedback? feedback,
    DateTime Function()? now,
    this.simulated = false,
    this.presentAsRealAlert = false,
    this.onTimeout,
    this.uriLauncher,
  }) : feedback = feedback ?? SystemSafetyCountdownFeedback(),
       now = now ?? DateTime.now;

  final SafetyEvent event;
  final SafetyCountdownFeedback feedback;
  final DateTime Function() now;
  final bool simulated;
  final bool presentAsRealAlert;
  final VoidCallback? onTimeout;
  final Future<bool> Function(Uri uri)? uriLauncher;

  @override
  State<SafetyCountdownSheet> createState() => _SafetyCountdownSheetState();
}

class _SafetyCountdownSheetState extends State<SafetyCountdownSheet> {
  static const _falsePositiveReasons = <_FalsePositiveReason>[
    _FalsePositiveReason(
      code: 'DROPPED_ON_SOFT_SURFACE',
      label: 'Điện thoại rơi lên nệm hoặc bề mặt mềm',
      keyName: 'soft-surface',
    ),
    _FalsePositiveReason(
      code: 'EXERCISE',
      label: 'Đang tập thể thao hoặc vận động mạnh',
      keyName: 'exercise',
    ),
    _FalsePositiveReason(
      code: 'PHONE_STRUCK',
      label: 'Điện thoại bị va hoặc đá trúng',
      keyName: 'phone-struck',
    ),
    _FalsePositiveReason(code: 'OTHER', label: 'Lý do khác', keyName: 'other'),
  ];

  Timer? _timer;
  int _remainingSeconds = 0;
  bool _timedOut = false;
  bool _dismissed = false;
  bool _safeConfirmationInProgress = false;
  bool _feedbackStopped = false;
  BuildContext? _reasonDialogContext;
  BuildContext? _safeDialogContext;

  @override
  void initState() {
    super.initState();
    SafetyCountdownGuard.isShowing = true;
    SafetyCountdownGuard.activeEventId = widget.event.id;
    if (_usesProductionPresentation) widget.feedback.start();
    _tick();
    _timer = Timer.periodic(const Duration(seconds: 1), (_) => _tick());
  }

  void _stopFeedback() {
    if (_feedbackStopped) return;
    _feedbackStopped = true;
    if (_usesProductionPresentation) widget.feedback.stop();
  }

  void _tick() {
    if (_dismissed || _safeConfirmationInProgress) return;
    final deadline = widget.event.countdownDeadlineAt;
    final remaining = deadline == null
        ? Duration.zero
        : deadline.difference(widget.now().toUtc());
    if (remaining <= Duration.zero) {
      _completeTimeout();
      return;
    }
    final remainingSeconds =
        (remaining.inMicroseconds / Duration.microsecondsPerSecond).ceil();
    if (_usesProductionPresentation && !_timedOut) {
      widget.feedback.pulse(remainingSeconds);
    }
    if (mounted) setState(() => _remainingSeconds = remainingSeconds);
  }

  void _dismiss(SafetyCountdownResult result) {
    if (_dismissed) return;
    _dismissed = true;
    _timer?.cancel();
    _stopFeedback();
    if (mounted) Navigator.of(context).pop(result);
  }

  void _completeTimeout() {
    if (_timedOut || _dismissed || _safeConfirmationInProgress) return;
    _timedOut = true;
    _timer?.cancel();
    _stopFeedback();
    widget.onTimeout?.call();
    final dialogContext = _reasonDialogContext;
    if (dialogContext != null && dialogContext.mounted) {
      Navigator.of(dialogContext).pop();
    }
    final safeDialogContext = _safeDialogContext;
    if (safeDialogContext != null && safeDialogContext.mounted) {
      Navigator.of(safeDialogContext).pop(false);
    }
    if (mounted) {
      setState(() => _remainingSeconds = 0);
    }
  }

  Future<void> _confirmSafe() async {
    if (_dismissed || _safeConfirmationInProgress) return;
    _safeConfirmationInProgress = true;
    final confirmed = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) {
        _safeDialogContext = dialogContext;
        return AlertDialog(
          title: const Text('Xác nhận an toàn'),
          content: const Text(
            'Bạn xác nhận bản thân vẫn an toàn và muốn tắt cảnh báo ngã này?',
          ),
          actions: [
            TextButton(
              key: const Key('safety-countdown-cancel-safe-button'),
              onPressed: () => Navigator.of(dialogContext).pop(false),
              child: const Text('Hủy'),
            ),
            FilledButton(
              key: const Key('safety-countdown-confirm-safe-button'),
              onPressed: () => Navigator.of(dialogContext).pop(true),
              child: const Text('Xác nhận an toàn'),
            ),
          ],
        );
      },
    );
    _safeDialogContext = null;
    if (_dismissed || !mounted) return;
    if (confirmed == true) {
      _dismiss(const SafetyCountdownResult.safe());
      return;
    }
    _safeConfirmationInProgress = false;
    _tick();
  }

  Future<void> _selectFalsePositiveReason() async {
    final reason = await showDialog<_FalsePositiveReason>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) {
        _reasonDialogContext = dialogContext;
        return SimpleDialog(
          title: const Text('Vì sao đây là cảnh báo nhầm?'),
          children: [
            for (final reason in _falsePositiveReasons)
              SimpleDialogOption(
                key: Key('false-positive-reason-${reason.keyName}'),
                onPressed: () => Navigator.of(dialogContext).pop(reason),
                child: Padding(
                  padding: const EdgeInsets.symmetric(vertical: 8),
                  child: Text(reason.label),
                ),
              ),
          ],
        );
      },
    );
    _reasonDialogContext = null;
    if (_dismissed || reason == null || !mounted) return;
    _dismiss(
      SafetyCountdownResult.falsePositive(
        reasonCode: reason.code,
        reason: reason.label,
      ),
    );
  }

  @override
  void dispose() {
    _timer?.cancel();
    _stopFeedback();
    if (SafetyCountdownGuard.activeEventId == widget.event.id) {
      SafetyCountdownGuard.reset();
    }
    super.dispose();
  }

  bool get _usesProductionPresentation =>
      !widget.simulated || widget.presentAsRealAlert;

  bool get _isSensorSelfTest => widget.event.eventType == 'SENSOR_SELF_TEST';

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(24, 24, 24, 32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (_isSensorSelfTest) ...[
              Container(
                key: const Key('sensor-self-test-countdown-banner'),
                width: double.infinity,
                padding: const EdgeInsets.symmetric(
                  horizontal: 14,
                  vertical: 10,
                ),
                decoration: BoxDecoration(
                  color: const Color(0xFFFFE9E3),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: const Text(
                  'DIỄN TẬP AN TOÀN · KHÔNG GỬI CẢNH BÁO THẬT',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontWeight: FontWeight.w800,
                    color: Color(0xFF845143),
                  ),
                ),
              ),
              const SizedBox(height: 12),
            ],
            if (widget.simulated && !widget.presentAsRealAlert) ...[
              Container(
                key: const Key('safety-countdown-simulation-banner'),
                padding: const EdgeInsets.symmetric(
                  horizontal: 14,
                  vertical: 8,
                ),
                decoration: BoxDecoration(
                  color: const Color(0xFFFFE9E3),
                  borderRadius: BorderRadius.circular(999),
                ),
                child: const Text(
                  'MÔ PHỎNG AN TOÀN',
                  style: TextStyle(
                    fontWeight: FontWeight.w800,
                    color: Color(0xFF845143),
                  ),
                ),
              ),
              const SizedBox(height: 12),
            ],
            const Icon(
              Icons.warning_amber_rounded,
              size: 52,
              color: Color(0xFFBA1A1A),
            ),
            const SizedBox(height: 12),
            Text(
              _usesProductionPresentation
                  ? (_remainingSeconds > 0
                        ? 'CareBridge phát hiện dấu hiệu nghi ngờ ngã'
                        : 'Đã chuyển sang hỗ trợ khẩn cấp!')
                  : 'Kiểm thử luồng phát hiện ngã bằng dữ liệu mô phỏng',
              textAlign: TextAlign.center,
              style: const TextStyle(fontSize: 20, fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 8),
            if (widget.simulated && !widget.presentAsRealAlert) ...[
              const Text(
                'Thao tác ở đây chỉ chạy trên thiết bị, không gửi cảnh báo, '
                'không gọi SOS và không ghi lịch sử.',
                textAlign: TextAlign.center,
                style: TextStyle(fontWeight: FontWeight.w600),
              ),
              const SizedBox(height: 8),
            ],
            Text(
              _usesProductionPresentation
                  ? (_remainingSeconds > 0
                        ? 'Bạn có ổn không? Hãy phản hồi trước khi hết thời gian.'
                        : 'Hệ thống đã gửi thông báo đến người thân. Hãy gọi cấp cứu 115 nếu cần trợ giúp ngay.')
                  : 'Mô phỏng sẽ tự kết thúc sau $_remainingSeconds giây.',
              textAlign: TextAlign.center,
            ),
            if (_usesProductionPresentation) ...[
              const SizedBox(height: 16),
              if (_remainingSeconds > 0) ...[
                Container(
                  key: const Key('safety-countdown-large-timer'),
                  width: double.infinity,
                  padding: const EdgeInsets.symmetric(vertical: 18),
                  decoration: BoxDecoration(
                    color: const Color(0xFFFFDAD6),
                    borderRadius: BorderRadius.circular(24),
                    border: Border.all(color: const Color(0xFFFFB4AB)),
                  ),
                  child: Column(
                    children: [
                      Text(
                        '$_remainingSeconds',
                        style: const TextStyle(
                          fontSize: 64,
                          height: 1,
                          fontWeight: FontWeight.w900,
                          color: Color(0xFF93000A),
                        ),
                      ),
                      const SizedBox(height: 6),
                      const Text(
                        'GIÂY TRƯỚC KHI CHUYỂN SANG HỖ TRỢ KHẨN CẤP',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w800,
                          color: Color(0xFF93000A),
                        ),
                      ),
                    ],
                  ),
                ),
              ] else ...[
                SizedBox(
                  width: double.infinity,
                  child: FilledButton.icon(
                    key: const Key('safety-countdown-call-115-button'),
                    style: FilledButton.styleFrom(
                      backgroundColor: const Color(0xFFBA1A1A),
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(
                        vertical: 20,
                        horizontal: 16,
                      ),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(24),
                      ),
                      elevation: 4,
                    ),
                    onPressed: () async {
                      final uri = Uri.parse('tel:115');
                      if (widget.uriLauncher != null) {
                        await widget.uriLauncher!(uri);
                      } else if (await canLaunchUrl(uri)) {
                        await launchUrl(uri);
                      }
                    },
                    icon: const Icon(Icons.phone_in_talk, size: 32),
                    label: const Text(
                      'GỌI 115 NGAY',
                      style: TextStyle(
                        fontSize: 22,
                        fontWeight: FontWeight.w900,
                        letterSpacing: 1.1,
                      ),
                    ),
                  ),
                ),
              ],
              const SizedBox(height: 14),
              _EmergencyStep(
                number: '1',
                text: _isSensorSelfTest
                    ? 'Khi ngã thật: CareBridge gửi cảnh báo cho người thân.'
                    : (_remainingSeconds > 0
                          ? 'Không phản hồi: CareBridge gửi cảnh báo cho người thân.'
                          : 'CareBridge đã gửi cảnh báo khẩn cấp tới người thân của bạn.'),
              ),
              const SizedBox(height: 8),
              _EmergencyStep(
                number: '2',
                text: _isSensorSelfTest
                    ? 'Diễn tập dừng an toàn; luồng thật mới chuyển sang bước gọi 115.'
                    : 'Bấm nút GỌI 115 NGAY ở trên để trực tiếp kết nối cấp cứu 115.',
              ),
            ],
            const SizedBox(height: 20),
            FilledButton.icon(
              key: const Key('safety-countdown-safe'),
              onPressed: _confirmSafe,
              icon: const Icon(Icons.check_circle_outline),
              label: const Text('Tôi vẫn ổn — tắt cảnh báo'),
              style: FilledButton.styleFrom(
                minimumSize: const Size.fromHeight(48),
              ),
            ),
            const SizedBox(height: 10),
            OutlinedButton.icon(
              key: const Key('safety-countdown-false-positive'),
              onPressed: _selectFalsePositiveReason,
              icon: const Icon(Icons.report_gmailerrorred_outlined),
              label: const Text('Báo phát hiện nhầm'),
              style: OutlinedButton.styleFrom(
                minimumSize: const Size.fromHeight(48),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _EmergencyStep extends StatelessWidget {
  const _EmergencyStep({required this.number, required this.text});

  final String number;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: 28,
          height: 28,
          alignment: Alignment.center,
          decoration: const BoxDecoration(
            color: Color(0xFF93000A),
            shape: BoxShape.circle,
          ),
          child: Text(
            number,
            style: const TextStyle(
              color: Colors.white,
              fontWeight: FontWeight.w800,
            ),
          ),
        ),
        const SizedBox(width: 10),
        Expanded(
          child: Padding(
            padding: const EdgeInsets.only(top: 4),
            child: Text(
              text,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
          ),
        ),
      ],
    );
  }
}

class _FalsePositiveReason {
  const _FalsePositiveReason({
    required this.code,
    required this.label,
    required this.keyName,
  });

  final String code;
  final String label;
  final String keyName;
}
