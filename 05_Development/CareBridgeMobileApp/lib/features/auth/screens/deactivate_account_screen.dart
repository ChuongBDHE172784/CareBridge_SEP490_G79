import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';
import '../../../core/auth/auth_state.dart';
import '../services/auth_service.dart';

class DeactivateAccountScreen extends StatefulWidget {
  const DeactivateAccountScreen({super.key});

  @override
  State<DeactivateAccountScreen> createState() =>
      _DeactivateAccountScreenState();
}

class _DeactivateAccountScreenState extends State<DeactivateAccountScreen> {
  static const _canvas = Color(0xFFF6F1EC);
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surfaceLowest = Color(0xFFFFFFFF);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);

  bool _isLoading = false;

  Future<void> _deactivate() async {
    final password = await _showPasswordDialog();
    if (password == null || password.isEmpty) return;

    setState(() => _isLoading = true);
    try {
      await AuthService.instance.deactivateAccount(password);
      if (!mounted) return;
      await AuthState.instance.clear();
      if (mounted) Navigator.of(context).popUntil((r) => r.isFirst);
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            e.statusCode == 400
                ? 'Mật khẩu không đúng. Vui lòng thử lại.'
                : e.statusCode == 403
                ? 'Tài khoản quản trị không thể tự vô hiệu hóa.'
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
          title: const Text(
            'Xác nhận mật khẩu',
            style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w600),
          ),
          content: TextField(
            controller: controller,
            obscureText: obscure,
            decoration: InputDecoration(
              hintText: 'Nhập mật khẩu của bạn',
              hintStyle: const TextStyle(
                fontFamily: 'Lexend',
                color: _onSurfaceVariant,
              ),
              suffixIcon: IconButton(
                icon: Icon(
                  obscure ? Icons.visibility_off : Icons.visibility,
                  color: _onSurfaceVariant,
                ),
                onPressed: () => setDialogState(() => obscure = !obscure),
              ),
            ),
            style: const TextStyle(fontFamily: 'Lexend'),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text(
                'Hủy',
                style: TextStyle(fontFamily: 'Lexend', color: _primary),
              ),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(ctx, controller.text),
              style: FilledButton.styleFrom(backgroundColor: _primaryContainer),
              child: const Text(
                'Xác nhận',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontWeight: FontWeight.w600,
                  color: Colors.white,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: Column(
          children: [
            _buildAppBar(),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(24, 32, 24, 24),
                child: Column(
                  children: [
                    // Pause icon
                    Container(
                      width: 72,
                      height: 72,
                      decoration: const BoxDecoration(
                        color: Color(0xFFFFE2D9),
                        shape: BoxShape.circle,
                      ),
                      child: const Icon(
                        Icons.pause_circle_outline_rounded,
                        color: _primaryContainer,
                        size: 36,
                      ),
                    ),
                    const SizedBox(height: 20),

                    // Title
                    const Text(
                      'Tạm ngưng tài khoản?',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 24,
                        fontWeight: FontWeight.w700,
                        color: _onSurface,
                      ),
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 12),

                    // Subtitle
                    const Text(
                      'Bạn sắp tạm ngưng hoạt động trên CareBridge. Hành động này có thể được hoàn tác bất cứ lúc nào.',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 14,
                        fontWeight: FontWeight.w400,
                        height: 1.6,
                        color: _onSurfaceVariant,
                      ),
                    ),
                    const SizedBox(height: 32),

                    // Info cards
                    _infoCard(
                      icon: Icons.visibility_off_outlined,
                      title: 'Hồ sơ của bạn sẽ bị ẩn',
                      body:
                          'Cộng đồng và người thân sẽ không thể xem thông tin hoặc tương tác với bạn.',
                    ),
                    const SizedBox(height: 12),
                    _infoCard(
                      icon: Icons.notifications_paused_outlined,
                      title: 'Tạm dừng thông báo',
                      body:
                          'Bạn sẽ không nhận được thông báo nhắc nhở lịch tiêm hay nhật ký mới.',
                    ),
                    const SizedBox(height: 12),
                    _infoCard(
                      icon: Icons.restore_rounded,
                      title: 'Dễ dàng khôi phục',
                      body:
                          'Chỉ cần đăng nhập lại, mọi dữ liệu của bạn sẽ được giữ nguyên vẹn.',
                    ),
                    const SizedBox(height: 40),

                    // Back button
                    SizedBox(
                      width: double.infinity,
                      height: 52,
                      child: OutlinedButton(
                        onPressed: () => Navigator.of(context).pop(),
                        style: OutlinedButton.styleFrom(
                          side: const BorderSide(color: _outlineVariant),
                          shape: const StadiumBorder(),
                        ),
                        child: const Text(
                          'Quay lại',
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 16,
                            fontWeight: FontWeight.w600,
                            color: _onSurface,
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: 12),

                    // Deactivate button
                    SizedBox(
                      width: double.infinity,
                      height: 52,
                      child: FilledButton(
                        onPressed: _isLoading ? null : _deactivate,
                        style: FilledButton.styleFrom(
                          backgroundColor: _primaryContainer,
                          shape: const StadiumBorder(),
                        ),
                        child: _isLoading
                            ? const SizedBox(
                                width: 20,
                                height: 20,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2,
                                  color: Colors.white,
                                ),
                              )
                            : const Text(
                                'Tạm ngưng tài khoản',
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
              ),
            ),
          ],
        ),
      ),
    );
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
              icon: const Icon(Icons.arrow_back, color: _onSurface),
              padding: EdgeInsets.zero,
            ),
          ),
          const SizedBox(width: 4),
          const Text(
            'Cài đặt tài khoản',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 18,
              fontWeight: FontWeight.w600,
              color: _onSurface,
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
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
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
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: _primary, size: 22),
          const SizedBox(width: 16),
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
