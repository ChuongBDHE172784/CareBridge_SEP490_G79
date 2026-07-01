import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';
import '../../../core/auth/auth_state.dart';
import '../services/auth_service.dart';

class DeleteAccountSheet extends StatefulWidget {
  const DeleteAccountSheet({super.key});

  /// Show this sheet and return true if deletion was successfully requested.
  static Future<bool> show(BuildContext context) async {
    final result = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => const DeleteAccountSheet(),
    );
    return result == true;
  }

  @override
  State<DeleteAccountSheet> createState() => _DeleteAccountSheetState();
}

class _DeleteAccountSheetState extends State<DeleteAccountSheet> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surfaceLowest = Color(0xFFFFFFFF);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _errorContainer = Color(0xFFFFDAD6);
  static const _onError = Color(0xFFBA1A1A);

  bool _understood = false;
  bool _isLoading = false;

  Future<void> _confirmDelete() async {
    final password = await _showPasswordDialog();
    if (password == null || password.isEmpty) return;

    setState(() => _isLoading = true);
    try {
      await AuthService.instance.requestAccountDeletion(password);
      if (!mounted) return;
      await AuthState.instance.clear();
      if (mounted) Navigator.of(context).pop(true);
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            e.statusCode == 400
                ? 'Mật khẩu không đúng. Vui lòng thử lại.'
                : 'Đã xảy ra lỗi (${e.statusCode}). Vui lòng thử lại.',
          ),
        ),
      );
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<String?> _showPasswordDialog() {
    final controller = TextEditingController();
    bool obscure = true;
    return showDialog<String>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setDialogState) => AlertDialog(
          title: const Text('Xác nhận mật khẩu',
              style: TextStyle(
                  fontFamily: 'Lexend', fontWeight: FontWeight.w600)),
          content: TextField(
            controller: controller,
            obscureText: obscure,
            decoration: InputDecoration(
              hintText: 'Nhập mật khẩu của bạn',
              hintStyle:
                  const TextStyle(fontFamily: 'Lexend', color: _onSurfaceVariant),
              suffixIcon: IconButton(
                icon: Icon(
                    obscure ? Icons.visibility_off : Icons.visibility,
                    color: _onSurfaceVariant),
                onPressed: () => setDialogState(() => obscure = !obscure),
              ),
            ),
            style: const TextStyle(fontFamily: 'Lexend'),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('Hủy',
                  style: TextStyle(fontFamily: 'Lexend', color: _primary)),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(ctx, controller.text),
              style: FilledButton.styleFrom(backgroundColor: _onError),
              child: const Text('Xóa',
                  style: TextStyle(
                      fontFamily: 'Lexend',
                      fontWeight: FontWeight.w600,
                      color: Colors.white)),
            ),
          ],
        ),
      ),
    );
  }

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
              color: const Color(0xFFD6C2BD),
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          const SizedBox(height: 24),

          // Warning icon
          Container(
            width: 64,
            height: 64,
            decoration: const BoxDecoration(
              color: Color(0xFFFFEDE9),
              shape: BoxShape.circle,
            ),
            child: const Icon(Icons.warning_amber_rounded,
                color: _onError, size: 32),
          ),
          const SizedBox(height: 16),

          // Title
          const Text(
            'Xóa tài khoản?',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 24,
              fontWeight: FontWeight.w700,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 8),

          // Subtitle
          const Text(
            'Hành động này không thể hoàn tác. Vui lòng đọc kỹ các thông tin dưới đây.',
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

          // Warning card
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: _errorContainer,
              borderRadius: BorderRadius.circular(16),
            ),
            child: Column(
              children: [
                _warningItem(
                  icon: Icons.broken_image_outlined,
                  title: 'Mất dữ liệu vĩnh viễn',
                  body: 'Toàn bộ hồ sơ của bé, nhật ký phát triển và hình ảnh sẽ bị xóa.',
                ),
                const Divider(
                  color: Color(0xFFFFB4AB),
                  height: 24,
                ),
                _warningItem(
                  icon: Icons.group_remove_outlined,
                  title: 'Rời khỏi nhóm chăm sóc',
                  body: 'Bạn sẽ bị xóa khỏi tất cả các nhóm gia đình hiện tại.',
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),

          // Understand checkbox
          GestureDetector(
            onTap: () => setState(() => _understood = !_understood),
            behavior: HitTestBehavior.opaque,
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                SizedBox(
                  width: 24,
                  height: 24,
                  child: Checkbox(
                    value: _understood,
                    onChanged: (v) => setState(() => _understood = v ?? false),
                    shape: const CircleBorder(),
                    side: const BorderSide(color: _onSurfaceVariant),
                    activeColor: _primary,
                  ),
                ),
                const SizedBox(width: 12),
                const Expanded(
                  child: Text(
                    'Tôi hiểu rằng việc xóa tài khoản là vĩnh viễn và không thể khôi phục dữ liệu.',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 14,
                      fontWeight: FontWeight.w400,
                      height: 1.4,
                      color: _onSurface,
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),

          // Delete button
          SizedBox(
            width: double.infinity,
            height: 52,
            child: FilledButton(
              onPressed: (_understood && !_isLoading) ? _confirmDelete : null,
              style: FilledButton.styleFrom(
                backgroundColor: _understood ? _onError : const Color(0xFFF5E6E3),
                disabledBackgroundColor: const Color(0xFFF5E6E3),
                shape: const StadiumBorder(),
              ),
              child: _isLoading
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(
                          strokeWidth: 2, color: Colors.white))
                  : Text(
                      'Xóa tài khoản',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                        color: _understood ? Colors.white : _onSurfaceVariant,
                      ),
                    ),
            ),
          ),
          const SizedBox(height: 12),

          // Cancel button
          SizedBox(
            width: double.infinity,
            height: 52,
            child: FilledButton(
              onPressed: () => Navigator.of(context).pop(false),
              style: FilledButton.styleFrom(
                backgroundColor: _primaryContainer,
                shape: const StadiumBorder(),
              ),
              child: const Text(
                'Hủy',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: Colors.white,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _warningItem({
    required IconData icon,
    required String title,
    required String body,
  }) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, color: _onError, size: 22),
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
                  color: _onError,
                ),
              ),
              const SizedBox(height: 2),
              Text(
                body,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 13,
                  fontWeight: FontWeight.w400,
                  height: 1.4,
                  color: _onError,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
