import 'package:flutter/material.dart';
import '../../../../core/auth/auth_state.dart';

class BlockedAccountScreen extends StatelessWidget {
  const BlockedAccountScreen({super.key});

  static const _canvasColor = Color(0xFFF6F1EC);
  static const _textColor = Color(0xFF5A463F);
  static const _primaryColor = Color(0xFFC98C7B);

  String _humanReadableReason(String code) {
    return switch (code) {
      'ACCOUNT_DISABLED' =>
        'Tài khoản của bạn đã bị vô hiệu hóa. Vui lòng liên hệ đội hỗ trợ để được giải quyết.',
      'ACCOUNT_LOCKED' =>
        'Tài khoản của bạn tạm thời bị khóa do nhiều lần đăng nhập thất bại. Vui lòng thử lại sau 15 phút.',
      _ =>
        'Tài khoản của bạn hiện không thể đăng nhập. Vui lòng liên hệ đội hỗ trợ.',
    };
  }

  String _titleForReason(String code) {
    return switch (code) {
      'ACCOUNT_DISABLED' => 'Tài khoản bị vô hiệu hoá',
      'ACCOUNT_LOCKED' => 'Tài khoản bị khoá tạm thời',
      _ => 'Tài khoản bị hạn chế',
    };
  }

  @override
  Widget build(BuildContext context) {
    final reason = AuthState.instance.blockedReason ?? 'ACCOUNT_DISABLED';

    return Scaffold(
      backgroundColor: _canvasColor,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Spacer(),
              Center(
                child: Container(
                  width: 96,
                  height: 96,
                  decoration: BoxDecoration(
                    color: const Color(0xFFC98C7B).withValues(alpha: 0.12),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(
                    Icons.lock_outline,
                    size: 48,
                    color: Color(0xFF93000A),
                  ),
                ),
              ),
              const SizedBox(height: 32),
              Text(
                _titleForReason(reason),
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.w600,
                  color: _textColor,
                ),
              ),
              const SizedBox(height: 16),
              Text(
                _humanReadableReason(reason),
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w400,
                  color: const Color(0xFF524440),
                  height: 1.6,
                ),
              ),
              const Spacer(),
              SizedBox(
                height: 52,
                child: FilledButton(
                  onPressed: () => AuthState.instance.clearBlockedReason(),
                  style: FilledButton.styleFrom(
                    backgroundColor: _primaryColor,
                    foregroundColor: Colors.white,
                    shape: const StadiumBorder(),
                  ),
                  child: const Text(
                    'Quay lại đăng nhập',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
                  ),
                ),
              ),
              const SizedBox(height: 16),
              OutlinedButton(
                onPressed: () {}, // TODO: open support link or email
                style: OutlinedButton.styleFrom(
                  foregroundColor: _textColor,
                  side: const BorderSide(color: _primaryColor, width: 1.5),
                  shape: const StadiumBorder(),
                  minimumSize: const Size.fromHeight(52),
                ),
                child: const Text(
                  'Liên hệ hỗ trợ',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
                ),
              ),
              const SizedBox(height: 8),
            ],
          ),
        ),
      ),
    );
  }
}
