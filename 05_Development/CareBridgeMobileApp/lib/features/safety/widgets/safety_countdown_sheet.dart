import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../models/safety_config_model.dart';

enum SafetyCountdownAction { safe, falsePositive, help, timeout }

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

abstract class SafetyCountdownFeedback {
  void start();

  void pulse(int remainingSeconds);

  void stop();
}

class SystemSafetyCountdownFeedback implements SafetyCountdownFeedback {
  var _active = false;

  @override
  void start() => _active = true;

  @override
  void pulse(int remainingSeconds) {
    if (!_active || !_shouldPulse(remainingSeconds)) return;
    unawaited(HapticFeedback.vibrate());
    unawaited(SystemSound.play(SystemSoundType.alert));
  }

  bool _shouldPulse(int remainingSeconds) {
    if (remainingSeconds <= 10) return true;
    if (remainingSeconds <= 20) return remainingSeconds.isEven;
    return remainingSeconds % 5 == 0;
  }

  @override
  void stop() => _active = false;
}

class SafetyCountdownSheet extends StatefulWidget {
  SafetyCountdownSheet({
    super.key,
    required this.event,
    SafetyCountdownFeedback? feedback,
    DateTime Function()? now,
    this.simulated = false,
    this.presentAsRealAlert = false,
  }) : feedback = feedback ?? SystemSafetyCountdownFeedback(),
       now = now ?? DateTime.now;

  final SafetyEvent event;
  final SafetyCountdownFeedback feedback;
  final DateTime Function() now;
  final bool simulated;
  final bool presentAsRealAlert;

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
  bool _completed = false;
  BuildContext? _reasonDialogContext;

  @override
  void initState() {
    super.initState();
    if (_usesProductionPresentation) widget.feedback.start();
    _tick();
    _timer = Timer.periodic(const Duration(seconds: 1), (_) => _tick());
  }

  void _tick() {
    if (_completed) return;
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
    if (_usesProductionPresentation) widget.feedback.pulse(remainingSeconds);
    if (mounted) setState(() => _remainingSeconds = remainingSeconds);
  }

  void _complete(SafetyCountdownResult result) {
    if (_completed) return;
    _completed = true;
    _timer?.cancel();
    if (mounted) Navigator.of(context).pop(result);
  }

  void _completeTimeout() {
    if (_completed) return;
    final dialogContext = _reasonDialogContext;
    if (dialogContext == null) {
      _complete(const SafetyCountdownResult.timeout());
      return;
    }
    _completed = true;
    _timer?.cancel();
    if (dialogContext.mounted) {
      Navigator.of(dialogContext).pop();
    }
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        Navigator.of(context).pop(const SafetyCountdownResult.timeout());
      }
    });
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
    if (_completed || reason == null || !mounted) return;
    _complete(
      SafetyCountdownResult.falsePositive(
        reasonCode: reason.code,
        reason: reason.label,
      ),
    );
  }

  @override
  void dispose() {
    _timer?.cancel();
    if (_usesProductionPresentation) widget.feedback.stop();
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
                  ? 'CareBridge phát hiện dấu hiệu nghi ngờ ngã'
                  : 'Kiểm thử luồng phát hiện ngã bằng dữ liệu mô phỏng',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.w700),
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
                  ? 'Bạn có ổn không? Hãy phản hồi trước khi hết thời gian.'
                  : 'Mô phỏng sẽ tự kết thúc sau $_remainingSeconds giây.',
              textAlign: TextAlign.center,
            ),
            if (_usesProductionPresentation) ...[
              const SizedBox(height: 16),
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
              const SizedBox(height: 14),
              _EmergencyStep(
                number: '1',
                text: _isSensorSelfTest
                    ? 'Khi ngã thật: CareBridge gửi cảnh báo cho người thân.'
                    : 'Không phản hồi: CareBridge gửi cảnh báo cho người thân.',
              ),
              const SizedBox(height: 8),
              _EmergencyStep(
                number: '2',
                text: _isSensorSelfTest
                    ? 'Diễn tập dừng an toàn; luồng thật mới chuyển sang bước gọi 115.'
                    : 'Nếu người thân chưa thể hỗ trợ, chuyển sang bước gọi 115.',
              ),
            ],
            const SizedBox(height: 20),
            FilledButton.icon(
              key: const Key('safety-countdown-safe'),
              onPressed: () => _complete(const SafetyCountdownResult.safe()),
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
            const SizedBox(height: 10),
            FilledButton.icon(
              key: const Key('safety-countdown-help'),
              style: FilledButton.styleFrom(
                backgroundColor: const Color(0xFFBA1A1A),
                minimumSize: const Size.fromHeight(48),
              ),
              onPressed: () => _complete(const SafetyCountdownResult.help()),
              icon: const Icon(Icons.emergency),
              label: const Text('Cần trợ giúp ngay'),
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
