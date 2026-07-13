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
        'TÃƒÂ i khoÃ¡ÂºÂ£n cÃ¡Â»Â§a bÃ¡ÂºÂ¡n Ã„â€˜ÃƒÂ£ bÃ¡Â»â€¹ vÃƒÂ´ hiÃ¡Â»â€¡u hÃƒÂ³a. Vui lÃƒÂ²ng liÃƒÂªn hÃ¡Â»â€¡ Ã„â€˜Ã¡Â»â„¢i hÃ¡Â»â€” trÃ¡Â»Â£ Ã„â€˜Ã¡Â»Æ’ Ã„â€˜Ã†Â°Ã¡Â»Â£c giÃ¡ÂºÂ£i quyÃ¡ÂºÂ¿t.',
      'ACCOUNT_LOCKED' =>
        'TÃƒÂ i khoÃ¡ÂºÂ£n cÃ¡Â»Â§a bÃ¡ÂºÂ¡n tÃ¡ÂºÂ¡m thÃ¡Â»Âi bÃ¡Â»â€¹ khÃƒÂ³a do nhiÃ¡Â»Âu lÃ¡ÂºÂ§n Ã„â€˜Ã„Æ’ng nhÃ¡ÂºÂ­p thÃ¡ÂºÂ¥t bÃ¡ÂºÂ¡i. Vui lÃƒÂ²ng thÃ¡Â»Â­ lÃ¡ÂºÂ¡i sau 15 phÃƒÂºt.',
      _ =>
        'TÃƒÂ i khoÃ¡ÂºÂ£n cÃ¡Â»Â§a bÃ¡ÂºÂ¡n hiÃ¡Â»â€¡n khÃƒÂ´ng thÃ¡Â»Æ’ Ã„â€˜Ã„Æ’ng nhÃ¡ÂºÂ­p. Vui lÃƒÂ²ng liÃƒÂªn hÃ¡Â»â€¡ Ã„â€˜Ã¡Â»â„¢i hÃ¡Â»â€” trÃ¡Â»Â£.',
    };
  }

  String _titleForReason(String code) {
    return switch (code) {
      'ACCOUNT_DISABLED' => 'Tài khoản bị vô hiệu hoá',
      'ACCOUNT_LOCKED' => 'Tài khoản bị khoá tạm thời',
      _ => 'Tài khoản bị hạn chế',
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
                  'LiÃƒÂªn hÃ¡Â»â€¡ hÃ¡Â»â€” trÃ¡Â»Â£',
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
