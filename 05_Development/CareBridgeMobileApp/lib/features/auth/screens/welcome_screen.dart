import 'package:flutter/material.dart';

import '../widgets/auth_ui.dart';
import 'login_screen.dart';
import 'register_screen.dart';

/// CB-001 - Mobile Welcome (UC-01, UC-03)
/// Entry point when not authenticated. Routes to Register or Login.
class WelcomeScreen extends StatelessWidget {
  const WelcomeScreen({super.key});

  static const _tagline =
      'Đồng hành cùng mẹ trong hành trình làm mẹ tuyệt vời.';
  static const _disclaimer =
      'Ứng dụng cung cấp thông tin tham khảo, không thay thế tư vấn y tế chuyên nghiệp.';

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: AuthBackground(
        child: SafeArea(
          child: LayoutBuilder(
            builder: (context, constraints) => SingleChildScrollView(
              padding: const EdgeInsets.fromLTRB(24, 20, 24, 24),
              child: ConstrainedBox(
                constraints: BoxConstraints(
                  minHeight: constraints.maxHeight - 44,
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    _buildBrandHeader(),
                    const SizedBox(height: 32),
                    _buildWelcomePanel(),
                    const SizedBox(height: 28),
                    _buildActions(context),
                    const SizedBox(height: 28),
                    _buildFooter(),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildBrandHeader() {
    return LayoutBuilder(
      builder: (context, constraints) {
        final identity = Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            const CareBridgeMark(size: 46, compact: true),
            const SizedBox(width: 12),
            const Text(
              'CareBridge',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 20,
                fontWeight: FontWeight.w800,
                color: AuthPalette.ink,
                letterSpacing: -0.45,
              ),
            ),
          ],
        );
        final safetyBadge = Container(
          padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 8),
          decoration: BoxDecoration(
            color: AuthPalette.surface.withValues(alpha: 0.78),
            borderRadius: BorderRadius.circular(10),
            border: Border.all(color: AuthPalette.line),
          ),
          child: const Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                Icons.verified_user_outlined,
                size: 15,
                color: AuthPalette.accentDeep,
              ),
              SizedBox(width: 6),
              Text(
                'An toàn trước tiên',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 11,
                  fontWeight: FontWeight.w600,
                  color: AuthPalette.mutedStrong,
                ),
              ),
            ],
          ),
        );

        if (constraints.maxWidth < 900) {
          return Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [identity, const SizedBox(height: 14), safetyBadge],
          );
        }

        return Row(children: [identity, const Spacer(), safetyBadge]);
      },
    );
  }

  Widget _buildWelcomePanel() {
    return AuthSurface(
      padding: const EdgeInsets.fromLTRB(22, 26, 22, 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const CareBridgeMark(size: 82),
          const SizedBox(height: 24),
          const AuthIntro(
            eyebrow: 'MỘT NƠI ĐỂ ĐƯỢC ĐỒNG HÀNH',
            title: 'Chăm sóc rõ ràng hơn, mỗi ngày.',
            subtitle: _tagline,
          ),
          const SizedBox(height: 22),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 11),
            decoration: BoxDecoration(
              color: const Color(0xFFF4EAE3),
              borderRadius: BorderRadius.circular(12),
            ),
            child: const Row(
              children: [
                Icon(
                  Icons.favorite_border_rounded,
                  size: 18,
                  color: AuthPalette.accentDeep,
                ),
                SizedBox(width: 9),
                Expanded(
                  child: Text(
                    'Theo dõi điều quan trọng. Nhận hướng dẫn vừa đủ cho từng giai đoạn.',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      height: 1.45,
                      color: AuthPalette.mutedStrong,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildActions(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        AuthPrimaryButton(
          label: 'Tạo tài khoản',
          onPressed: () => Navigator.push(
            context,
            MaterialPageRoute(builder: (_) => const RegisterScreen()),
          ),
        ),
        const SizedBox(height: 12),
        SizedBox(
          height: 56,
          child: OutlinedButton(
            onPressed: () => Navigator.push(
              context,
              MaterialPageRoute(builder: (_) => const LoginScreen()),
            ),
            style: OutlinedButton.styleFrom(
              foregroundColor: AuthPalette.ink,
              side: const BorderSide(color: AuthPalette.ink, width: 1.2),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
            ),
            child: const Text(
              'Đăng nhập',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 15,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ),
        const SizedBox(height: 10),
        TextButton.icon(
          onPressed: () => Navigator.push(
            context,
            MaterialPageRoute(
              builder: (_) => const RegisterScreen(isExpert: true),
            ),
          ),
          icon: const Icon(Icons.medical_services_outlined, size: 18),
          label: const Text('Đăng ký Chuyên gia y tế'),
          style: TextButton.styleFrom(
            minimumSize: const Size.fromHeight(48),
            foregroundColor: AuthPalette.accentDeep,
            textStyle: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 13,
              fontWeight: FontWeight.w700,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildFooter() {
    return Column(
      children: [
        Wrap(
          alignment: WrapAlignment.center,
          crossAxisAlignment: WrapCrossAlignment.center,
          spacing: 18,
          children: [
            _buildLegalText('Điều khoản'),
            const Text('•', style: TextStyle(color: AuthPalette.muted)),
            _buildLegalText('Quyền riêng tư'),
          ],
        ),
        const SizedBox(height: 10),
        const Text(
          _disclaimer,
          textAlign: TextAlign.center,
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 12,
            height: 1.5,
            color: AuthPalette.mutedStrong,
          ),
        ),
      ],
    );
  }

  Widget _buildLegalText(String label) {
    return Text(
      label,
      style: const TextStyle(
        fontFamily: 'Lexend',
        fontSize: 12,
        fontWeight: FontWeight.w600,
        color: AuthPalette.mutedStrong,
      ),
    );
  }
}
