import 'package:flutter/material.dart';

class EnablePostureCameraSheet extends StatelessWidget {
  const EnablePostureCameraSheet({super.key});

  static Future<bool> show(BuildContext context) async {
    final result = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => const EnablePostureCameraSheet(),
    );
    return result == true;
  }

  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surfaceLowest = Color(0xFFFFFFFF);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: _surfaceLowest,
        borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
      ),
      padding: EdgeInsets.fromLTRB(
        24,
        16,
        24,
        24 + MediaQuery.of(context).viewInsets.bottom,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // Drag handle
          Container(
            width: 40,
            height: 4,
            decoration: BoxDecoration(
              color: _outlineVariant,
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          const SizedBox(height: 28),

          // Camera icon circle
          Container(
            width: 72,
            height: 72,
            decoration: const BoxDecoration(
              color: Color(0xFFFFE2D9),
              shape: BoxShape.circle,
            ),
            child: const Icon(Icons.camera_alt_outlined,
                color: _primaryContainer, size: 32),
          ),
          const SizedBox(height: 20),

          // Title
          const Text(
            'Cấp quyền Camera',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 24,
              fontWeight: FontWeight.w700,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 10),

          // Subtitle
          const Text(
            'Ứng dụng cần sử dụng camera để phân tích tư thế của bạn trong các bài tập.',
            textAlign: TextAlign.center,
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              fontWeight: FontWeight.w400,
              height: 1.5,
              color: _onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 24),

          // Info items
          _infoCard(
            icon: Icons.shield_outlined,
            title: 'Bảo mật tối đa',
            body: 'Hình ảnh chỉ được xử lý trực tiếp trên thiết bị của bạn.',
          ),
          const SizedBox(height: 12),
          _infoCard(
            icon: Icons.cloud_off_outlined,
            title: 'Không lưu trữ',
            body: 'Chúng tôi cam kết không quay video hay lưu trữ bất kỳ hình ảnh nào.',
          ),
          const SizedBox(height: 28),

          // Allow button
          SizedBox(
            width: double.infinity,
            height: 52,
            child: FilledButton(
              onPressed: () => Navigator.of(context).pop(true),
              style: FilledButton.styleFrom(
                backgroundColor: _primary,
                shape: const StadiumBorder(),
              ),
              child: const Text(
                'Cho phép',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: Colors.white,
                ),
              ),
            ),
          ),
          const SizedBox(height: 12),

          // Cancel button
          SizedBox(
            width: double.infinity,
            height: 52,
            child: OutlinedButton(
              onPressed: () => Navigator.of(context).pop(false),
              style: OutlinedButton.styleFrom(
                side: const BorderSide(color: _primary),
                shape: const StadiumBorder(),
              ),
              child: const Text(
                'Hủy bỏ',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: _primary,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _infoCard({
    required IconData icon,
    required String title,
    required String body,
  }) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF8F6),
        border: Border.all(color: const Color(0xFFD6C2BD)),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: _primaryContainer, size: 22),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: _onSurface,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  body,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 13,
                    fontWeight: FontWeight.w400,
                    height: 1.4,
                    color: _onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
