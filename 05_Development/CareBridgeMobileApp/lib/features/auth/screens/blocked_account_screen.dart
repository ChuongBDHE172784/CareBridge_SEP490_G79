import 'package:flutter/material.dart';
import '../../../core/auth/auth_state.dart';

class BlockedAccountScreen extends StatelessWidget {
  const BlockedAccountScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final reason = AuthState.instance.blockedReason;
    final isDisabled = reason == 'ACCOUNT_DISABLED';

    final title = isDisabled ? 'Tài khoản bị vô hiệu hoá' : 'Tài khoản bị khoá tạm thời';
    final message = isDisabled
        ? 'Tài khoản của bạn đã bị vô hiệu hoá. Vui lòng liên hệ bộ phận hỗ trợ để được trợ giúp.'
        : 'Tài khoản của bạn đang bị khoá tạm thời. Vui lòng thử lại sau hoặc liên hệ bộ phận hỗ trợ.';

    return Scaffold(
      backgroundColor: const Color(0xFFF6F1EC),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Container(
            constraints: const BoxConstraints(maxWidth: 400),
            padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 40),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(32),
              boxShadow: const [
                BoxShadow(
                  color: Color(0x145A463F),
                  blurRadius: 32,
                  offset: Offset(0, 12),
                ),
              ],
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  width: 64,
                  height: 64,
                  decoration: BoxDecoration(
                    color: const Color(0xFFC98C7B).withOpacity(0.12),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(
                    Icons.lock_outline,
                    color: Color(0xFFC98C7B),
                    size: 28,
                  ),
                ),
                const SizedBox(height: 24),
                Text(
                  title,
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.w700,
                    color: Color(0xFF5A463F),
                    height: 1.3,
                  ),
                ),
                const SizedBox(height: 12),
                Text(
                  message,
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    fontSize: 14,
                    color: Color(0xFF9C857C),
                    height: 1.6,
                  ),
                ),
                const SizedBox(height: 32),
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    onPressed: AuthState.instance.clearBlockedReason,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFFC98C7B),
                      foregroundColor: Colors.white,
                      shape: const StadiumBorder(),
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      elevation: 0,
                      shadowColor: const Color(0xFFC98C7B),
                    ),
                    child: const Text(
                      'Quay lại đăng nhập',
                      style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
