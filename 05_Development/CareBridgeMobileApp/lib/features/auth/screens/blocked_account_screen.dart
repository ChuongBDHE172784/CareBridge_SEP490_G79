import 'package:flutter/material.dart';
import '../../../../core/auth/auth_state.dart';
import '../../../../core/auth/blocked_account_state.dart';
import '../../../../core/constants/support_contact.dart';

class BlockedAccountScreen extends StatelessWidget {
  const BlockedAccountScreen({super.key});

  static const _canvasColor = Color(0xFFF6F1EC);
  static const _textColor = Color(0xFF5A463F);
  static const _primaryColor = Color(0xFFC98C7B);

  String _title(BlockedAccountState state) => switch (state.code) {
    'ACCOUNT_ADMIN_LOCKED' => 'Tài khoản bị khóa bởi quản trị viên',
    'ACCOUNT_TEMPORARILY_LOCKED' ||
    'ACCOUNT_LOCKED' => 'Tài khoản bị khóa tạm thời',
    'ACCOUNT_DISABLED' => 'Tài khoản đã bị vô hiệu hóa',
    'ACCOUNT_SUSPENDED' => 'Tài khoản đang bị tạm ngưng',
    _ => 'Tài khoản bị hạn chế',
  };

  String _message(BlockedAccountState state) => switch (state.code) {
    'ACCOUNT_ADMIN_LOCKED' =>
      'System Admin đã khóa quyền truy cập tài khoản. Vui lòng liên hệ bộ phận chăm sóc khách hàng nếu bạn cho rằng quyết định này cần được xem xét lại.',
    'ACCOUNT_TEMPORARILY_LOCKED' || 'ACCOUNT_LOCKED' =>
      'Tài khoản tạm thời bị khóa do có nhiều lần đăng nhập không thành công. Vui lòng thử lại sau 15 phút.',
    'ACCOUNT_DISABLED' =>
      'Tài khoản đã bị vô hiệu hóa và hiện không thể đăng nhập. Vui lòng liên hệ hỗ trợ nếu bạn cho rằng đây là nhầm lẫn.',
    'ACCOUNT_SUSPENDED' =>
      'Tài khoản đang bị tạm ngưng theo quyết định kiểm duyệt.',
    _ => 'Tài khoản hiện không thể đăng nhập.',
  };

  @override
  Widget build(BuildContext context) {
    final state =
        AuthState.instance.blockedAccount ??
        const BlockedAccountState(code: 'ACCOUNT_DISABLED');

    return Scaffold(
      backgroundColor: _canvasColor,
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(24),
          children: [
            const SizedBox(height: 48),
            Center(
              child: Container(
                width: 96,
                height: 96,
                decoration: BoxDecoration(
                  color: _primaryColor.withValues(alpha: 0.12),
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
              _title(state),
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.w600,
                color: _textColor,
              ),
            ),
            const SizedBox(height: 16),
            Text(
              _message(state),
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontSize: 14,
                color: Color(0xFF524440),
                height: 1.6,
              ),
            ),
            if (state.reason != null && state.reason!.isNotEmpty) ...[
              const SizedBox(height: 24),
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: const Color(0xFFFFDAD6),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'LÝ DO KHÓA TÀI KHOẢN',
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w700,
                        color: Color(0xFF93000A),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      state.reason!,
                      style: const TextStyle(height: 1.5, color: _textColor),
                    ),
                  ],
                ),
              ),
            ],
            if (state.retryAt != null) ...[
              const SizedBox(height: 20),
              Text(
                'Có thể thử lại sau ${MaterialLocalizations.of(context).formatFullDate(state.retryAt!.toLocal())} ${TimeOfDay.fromDateTime(state.retryAt!.toLocal()).format(context)}.',
                textAlign: TextAlign.center,
                style: const TextStyle(color: _textColor),
              ),
            ],
            if (state.needsSupportContact) ...[
              const SizedBox(height: 28),
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: _primaryColor.withValues(alpha: 0.10),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'CẦN MỞ LẠI TÀI KHOẢN?',
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w700,
                        color: _textColor,
                      ),
                    ),
                    const SizedBox(height: 8),
                    const Text(
                      'Vui lòng liên hệ bộ phận chăm sóc khách hàng để được kiểm tra và xử lý. '
                      'Hãy cung cấp email hoặc số điện thoại đăng ký để được tra cứu nhanh hơn.',
                      style: TextStyle(height: 1.5, color: _textColor),
                    ),
                    const SizedBox(height: 12),
                    SelectableText(
                      supportEmail,
                      style: const TextStyle(
                        fontWeight: FontWeight.w600,
                        color: _primaryColor,
                      ),
                    ),
                    if (supportPhone.isNotEmpty)
                      SelectableText(
                        supportPhone,
                        style: const TextStyle(
                          fontWeight: FontWeight.w600,
                          color: _primaryColor,
                        ),
                      ),
                  ],
                ),
              ),
            ],
            const SizedBox(height: 32),
            OutlinedButton(
              onPressed: () => AuthState.instance.clearBlockedReason(),
              style: OutlinedButton.styleFrom(
                foregroundColor: _textColor,
                side: const BorderSide(color: _primaryColor, width: 1.5),
                shape: const StadiumBorder(),
                minimumSize: const Size.fromHeight(52),
              ),
              child: const Text(
                'Quay lại màn hình đăng nhập',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
