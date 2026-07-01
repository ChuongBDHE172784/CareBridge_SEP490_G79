import 'package:flutter/material.dart';
import '../services/safety_service.dart';

/// CB-130 — Disable Fall Detection Confirmation (UC-135)
/// Modal bottom sheet shown from CB-023 when turning the Fall Detection
/// toggle off. Calls POST /api/v1/safety/fall-detection/disable on confirm.
/// Returns true if fall detection was disabled, false/null if cancelled.
Future<bool?> showDisableFallDetectionSheet(BuildContext context) {
  return showModalBottomSheet<bool>(
    context: context,
    isScrollControlled: true,
    backgroundColor: Colors.transparent,
    builder: (_) => const _DisableFallDetectionSheet(),
  );
}

class _DisableFallDetectionSheet extends StatefulWidget {
  const _DisableFallDetectionSheet();

  @override
  State<_DisableFallDetectionSheet> createState() => _DisableFallDetectionSheetState();
}

class _DisableFallDetectionSheetState extends State<_DisableFallDetectionSheet> {
  static const _primary = Color(0xFF845143);
  static const _surface = Color(0xFFFFF8F6);
  static const _surfaceContainerLow = Color(0xFFFFF1EC);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outline = Color(0xFF84736F);
  static const _error = Color(0xFFBA1A1A);
  static const _errorContainer = Color(0xFFFFDAD6);

  // Reason is informational only — backend's disable() endpoint takes no
  // body, so this selection is not sent anywhere yet (no field exists).
  static const _reasons = ['Cảnh báo sai quá nhiều', 'Muốn tiết kiệm pin', 'Lý do khác'];
  int? _selectedReason;
  bool _submitting = false;

  Future<void> _confirmDisable() async {
    setState(() => _submitting = true);
    try {
      await SafetyService().disableFallDetection();
      if (mounted) Navigator.of(context).pop(true);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Không thể tắt phát hiện ngã: $e'), backgroundColor: _error),
        );
        setState(() => _submitting = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Container(
        decoration: const BoxDecoration(
          color: _surface,
          borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
        ),
        padding: const EdgeInsets.fromLTRB(24, 8, 24, 24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(width: 48, height: 6, margin: const EdgeInsets.only(bottom: 16),
                decoration: BoxDecoration(color: const Color(0xFFD6C2BD).withValues(alpha: 0.4), borderRadius: BorderRadius.circular(99))),
            Container(
              width: 64,
              height: 64,
              decoration: const BoxDecoration(color: _errorContainer, shape: BoxShape.circle),
              child: const Icon(Icons.warning, color: _error, size: 28),
            ),
            const SizedBox(height: 16),
            const Text('Tắt phát hiện ngã?',
                style: TextStyle(fontSize: 20, fontWeight: FontWeight.w600, color: _onSurface)),
            const SizedBox(height: 8),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              decoration: BoxDecoration(
                color: _errorContainer.withValues(alpha: 0.3),
                borderRadius: BorderRadius.circular(99),
                border: Border.all(color: _error.withValues(alpha: 0.1)),
              ),
              child: const Text('Cảnh báo sẽ không được gửi đi',
                  style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: _error)),
            ),
            const SizedBox(height: 16),
            const Text(
              'Hệ thống sẽ ngừng theo dõi các va chạm mạnh. Chúng tôi khuyên bạn nên giữ tính năng này để đảm bảo an toàn cho người thân.',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 14, color: _onSurfaceVariant, height: 1.4),
            ),
            const SizedBox(height: 24),
            Align(
              alignment: Alignment.centerLeft,
              child: Text('LÝ DO TẮT (TÙY CHỌN)',
                  style: TextStyle(fontSize: 12, color: _onSurfaceVariant, letterSpacing: 0.5)),
            ),
            const SizedBox(height: 8),
            ..._reasons.asMap().entries.map((e) => Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: InkWell(
                    onTap: () => setState(() => _selectedReason = e.key),
                    borderRadius: BorderRadius.circular(12),
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                      decoration: BoxDecoration(
                        color: _surfaceContainerLow,
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Text(e.value, style: const TextStyle(fontSize: 14, color: _onSurface)),
                          Icon(
                            _selectedReason == e.key ? Icons.radio_button_checked : Icons.radio_button_unchecked,
                            color: _selectedReason == e.key ? _primary : _outline,
                          ),
                        ],
                      ),
                    ),
                  ),
                )),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              height: 48,
              child: ElevatedButton(
                onPressed: _submitting ? null : _confirmDisable,
                style: ElevatedButton.styleFrom(
                  backgroundColor: _error,
                  foregroundColor: Colors.white,
                  shape: const StadiumBorder(),
                  elevation: 2,
                ),
                child: _submitting
                    ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2))
                    : const Text('Tắt phát hiện ngã', style: TextStyle(fontWeight: FontWeight.w600)),
              ),
            ),
            const SizedBox(height: 8),
            SizedBox(
              width: double.infinity,
              height: 48,
              child: TextButton(
                onPressed: _submitting ? null : () => Navigator.of(context).pop(false),
                style: TextButton.styleFrom(backgroundColor: const Color(0xFFFADCD3), foregroundColor: _primary, shape: const StadiumBorder()),
                child: const Text('Quay lại', style: TextStyle(fontWeight: FontWeight.w600)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
