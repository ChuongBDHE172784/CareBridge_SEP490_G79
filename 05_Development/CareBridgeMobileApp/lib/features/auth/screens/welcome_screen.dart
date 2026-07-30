import 'package:flutter/material.dart';
import 'login_screen.dart';
import 'register_screen.dart';

/// CB-001 — Mobile Welcome (UC-01, UC-03)
/// Entry point when not authenticated. Routes to Register or Login.
class WelcomeScreen extends StatelessWidget {
  const WelcomeScreen({super.key});

  static const _primaryColor = Color(0xFFC98C7B);
  static const _canvasColor = Color(0xFFF6F1EC);
  static const _textColor = Color(0xFF5A463F);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvasColor,
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, constraints) => SingleChildScrollView(
            padding: const EdgeInsets.symmetric(vertical: 24),
            child: ConstrainedBox(
              constraints: BoxConstraints(
                minHeight: constraints.maxHeight - 48,
              ),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.end,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // Brand block
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 24),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'CareBridge',
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 32,
                            fontWeight: FontWeight.w700,
                            color: _textColor,
                            letterSpacing: -0.64,
                          ),
                        ),
                        const SizedBox(height: 8),
                        Text(
                          'Đồng hành cùng mẹ trong hành trình làm mẹ tuyệt vời.',
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 16,
                            fontWeight: FontWeight.w400,
                            color: _textColor.withValues(alpha: 0.8),
                            height: 1.5,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 48),
                  // Action buttons
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 24),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        SizedBox(
                          height: 52,
                          child: FilledButton(
                            onPressed: () => Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (_) => const RegisterScreen(),
                              ),
                            ),
                            style: FilledButton.styleFrom(
                              backgroundColor: _textColor,
                              foregroundColor: Colors.white,
                              shape: const StadiumBorder(),
                              elevation: 0,
                              shadowColor: Colors.transparent,
                            ),
                            child: const Text(
                              'Tạo tài khoản',
                              style: TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 16,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ),
                        ),
                        const SizedBox(height: 12),
                        SizedBox(
                          height: 52,
                          child: OutlinedButton(
                            onPressed: () => Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (_) => const LoginScreen(),
                              ),
                            ),
                            style: OutlinedButton.styleFrom(
                              foregroundColor: _textColor,
                              side: BorderSide(
                                color: _textColor.withValues(alpha: 0.4),
                                width: 1.5,
                              ),
                              shape: const StadiumBorder(),
                            ),
                            child: const Text(
                              'Đăng nhập',
                              style: TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 16,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ),
                        ),
                        const SizedBox(height: 12),
                        TextButton.icon(
                          onPressed: () => Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (_) =>
                                  const RegisterScreen(isExpert: true),
                            ),
                          ),
                          icon: const Icon(
                            Icons.medical_services_outlined,
                            size: 18,
                          ),
                          label: const Text('Đăng ký Chuyên gia y tế'),
                          style: TextButton.styleFrom(
                            foregroundColor: _primaryColor,
                            textStyle: const TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 14,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 24),
                  // Footer
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 24),
                    child: Column(
                      children: [
                        Wrap(
                          alignment: WrapAlignment.center,
                          crossAxisAlignment: WrapCrossAlignment.center,
                          spacing: 32,
                          children: [
                            Text(
                              'Điều khoản',
                              style: TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 12,
                                fontWeight: FontWeight.w500,
                                color: _textColor,
                                letterSpacing: 0.6,
                              ),
                            ),
                            const Text(
                              '•',
                              style: TextStyle(color: _textColor),
                            ),
                            Text(
                              'Quyền riêng tư',
                              style: TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 12,
                                fontWeight: FontWeight.w500,
                                color: _textColor,
                                letterSpacing: 0.6,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 8),
                        Text(
                          'Ứng dụng cung cấp thông tin tham khảo, không thay thế tư vấn y tế chuyên nghiệp.',
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 16,
                            fontWeight: FontWeight.w500,
                            color: _textColor,
                            height: 1.45,
                          ),
                          textAlign: TextAlign.center,
                        ),
                        const SizedBox(height: 24),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
