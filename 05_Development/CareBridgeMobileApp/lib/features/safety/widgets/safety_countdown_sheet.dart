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
  }) : feedback = feedback ?? SystemSafetyCountdownFeedback(),
       now = now ?? DateTime.now;

  final SafetyEvent event;
  final SafetyCountdownFeedback feedback;
  final DateTime Function() now;

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
    widget.feedback.start();
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
    widget.feedback.pulse(remainingSeconds);
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
    widget.feedback.stop();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(24, 24, 24, 32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(
              Icons.warning_amber_rounded,
              size: 52,
              color: Color(0xFFBA1A1A),
            ),
            const SizedBox(height: 12),
            const Text(
              'CareBridge ghi nhận dấu hiệu nghi ngờ ngã hoặc va chạm',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 8),
            Text(
              'Cảnh báo sẽ được chuyển sang hỗ trợ khẩn cấp sau '
              '$_remainingSeconds giây nếu bạn không phản hồi.',
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 20),
            FilledButton.icon(
              key: const Key('safety-countdown-safe'),
              onPressed: () => _complete(const SafetyCountdownResult.safe()),
              icon: const Icon(Icons.check_circle_outline),
              label: const Text('Tôi an toàn'),
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
