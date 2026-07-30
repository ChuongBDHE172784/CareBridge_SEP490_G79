import 'package:flutter/material.dart';
import '../../../../core/auth/auth_state.dart';
import '../../../../core/auth/blocked_account_state.dart';
import '../services/auth_service.dart';

class BlockedAccountScreen extends StatefulWidget {
  const BlockedAccountScreen({super.key});

  @override
  State<BlockedAccountScreen> createState() => _BlockedAccountScreenState();
}

class _BlockedAccountScreenState extends State<BlockedAccountScreen> {
  static const _canvasColor = Color(0xFFF6F1EC);
  static const _textColor = Color(0xFF5A463F);
  static const _primaryColor = Color(0xFFC98C7B);
  final _appealController = TextEditingController();
  bool _submitting = false;
  bool _submitted = false;
  String? _error;

  @override
  void dispose() {
    _appealController.dispose();
    super.dispose();
  }

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
      'System Admin đã khóa quyền truy cập tài khoản. Bạn có thể gửi khiếu nại nếu cho rằng quyết định này cần được xem xét lại.',
    'ACCOUNT_TEMPORARILY_LOCKED' || 'ACCOUNT_LOCKED' =>
      'Tài khoản tạm thời bị khóa do có nhiều lần đăng nhập không thành công. Vui lòng thử lại sau 15 phút.',
    'ACCOUNT_DISABLED' =>
      'Tài khoản đã bị vô hiệu hóa và hiện không thể đăng nhập. Vui lòng liên hệ hỗ trợ nếu bạn cho rằng đây là nhầm lẫn.',
    'ACCOUNT_SUSPENDED' =>
      'Tài khoản đang bị tạm ngưng theo quyết định kiểm duyệt.',
    _ => 'Tài khoản hiện không thể đăng nhập.',
  };

  Future<void> _submitAppeal(BlockedAccountState state) async {
    final reason = _appealController.text.trim();
    if (!state.canAppeal || reason.isEmpty) {
      setState(() => _error = 'Vui lòng nhập nội dung khiếu nại.');
      return;
    }
    setState(() {
      _submitting = true;
      _error = null;
    });
    try {
      await AuthService.instance.submitAccountLockAppeal(
        appealToken: state.appealToken!,
        reason: reason,
      );
      if (!mounted) return;
      setState(() => _submitted = true);
    } catch (_) {
      if (!mounted) return;
      setState(
        () => _error =
            'Không thể gửi khiếu nại. Vui lòng đăng nhập lại để nhận quyền khiếu nại mới.',
      );
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

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
            if (state.appealStatus == 'REJECTED') ...[
              const SizedBox(height: 28),
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: const Color(0xFFFFDAD6),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: const Text(
                  'Khiếu nại mở khóa đã bị từ chối. Tài khoản vẫn bị khóa theo quyết định của quản trị viên.',
                  style: TextStyle(color: Color(0xFF93000A), height: 1.5),
                ),
              ),
            ] else if (state.appealPending) ...[
              const SizedBox(height: 28),
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: const Color(0xFFE2F4E7),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: const Text(
                  'Khiếu nại đã được gửi và đang chờ System Admin xem xét.',
                  style: TextStyle(color: Color(0xFF245D34), height: 1.5),
                ),
              ),
            ] else if (state.canAppeal) ...[
              const SizedBox(height: 28),
              if (_submitted)
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: const Color(0xFFE2F4E7),
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: const Text(
                    'Khiếu nại đã được gửi. System Admin sẽ xem xét trong quy trình quản trị tài khoản.',
                    style: TextStyle(color: Color(0xFF245D34), height: 1.5),
                  ),
                )
              else ...[
                TextField(
                  controller: _appealController,
                  maxLength: 1000,
                  minLines: 4,
                  maxLines: 6,
                  decoration: InputDecoration(
                    labelText: 'Nội dung khiếu nại',
                    hintText: 'Trình bày lý do đề nghị xem xét mở khóa...',
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                    errorText: _error,
                  ),
                ),
                const SizedBox(height: 12),
                FilledButton(
                  onPressed: _submitting ? null : () => _submitAppeal(state),
                  style: FilledButton.styleFrom(
                    backgroundColor: _primaryColor,
                    foregroundColor: Colors.white,
                    minimumSize: const Size.fromHeight(52),
                    shape: const StadiumBorder(),
                  ),
                  child: Text(
                    _submitting ? 'Đang gửi...' : 'Gửi khiếu nại mở khóa',
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ],
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
