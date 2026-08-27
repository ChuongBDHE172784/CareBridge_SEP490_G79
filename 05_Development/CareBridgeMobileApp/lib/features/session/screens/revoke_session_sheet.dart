import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';
import '../models/session_model.dart';
import '../services/session_service.dart';

Future<bool?> showRevokeSessionSheet(
  BuildContext context,
  SessionInfo session,
) {
  return showModalBottomSheet<bool>(
    context: context,
    isScrollControlled: true,
    backgroundColor: Colors.transparent,
    barrierColor: const Color(0xFF3E2C26).withValues(alpha: 0.4),
    builder: (_) => RevokeSessionSheet(session: session),
  );
}

class RevokeSessionSheet extends StatefulWidget {
  final SessionInfo session;

  const RevokeSessionSheet({super.key, required this.session});

  @override
  State<RevokeSessionSheet> createState() => _RevokeSessionSheetState();
}

class _RevokeSessionSheetState extends State<RevokeSessionSheet> {
  static const _surfaceColor = Color(0xFFFFF8F6);
  static const _primaryColor = Color(0xFF845143);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _surfaceVariant = Color(0xFFFADCD3);
  static const _surfaceContainerLow = Color(0xFFFFF1EC);
  static const _surfaceContainer = Color(0xFFFFE9E3);
  static const _errorColor = Color(0xFFBA1A1A);
  static const _errorContainer = Color(0xFFFFDAD6);
  static const _onError = Color(0xFFFFFFFF);

  bool _isSubmitting = false;
  String? _errorMessage;

  Future<void> _revokeSession() async {
    if (_isSubmitting) return;

    setState(() {
      _isSubmitting = true;
      _errorMessage = null;
    });

    try {
      await SessionService.instance.revokeSession(widget.session.sessionId);
      if (!mounted) return;
      Navigator.of(context).pop(true);
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _errorMessage = e.statusCode == 404
            ? 'Phiên đăng nhập không tồn tại.'
            : 'Không thể đăng xuất thiết bị. Vui lòng thử lại.';
        _isSubmitting = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _errorMessage = 'Không thể kết nối. Kiểm tra mạng và thử lại.';
        _isSubmitting = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final bottomInset = MediaQuery.of(context).viewInsets.bottom;

    return AnimatedPadding(
      duration: const Duration(milliseconds: 200),
      curve: Curves.easeOut,
      padding: EdgeInsets.only(bottom: bottomInset),
      child: SafeArea(
        top: false,
        child: Container(
          width: double.infinity,
          padding: const EdgeInsets.fromLTRB(24, 12, 24, 32),
          decoration: const BoxDecoration(
            color: _surfaceColor,
            borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
            boxShadow: [
              BoxShadow(
                color: Color.fromRGBO(90, 70, 63, 0.12),
                blurRadius: 24,
                offset: Offset(0, -8),
              ),
            ],
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 48,
                height: 6,
                decoration: BoxDecoration(
                  color: _surfaceVariant,
                  borderRadius: BorderRadius.circular(999),
                ),
              ),
              const SizedBox(height: 24),
              _buildWarningIcon(),
              const SizedBox(height: 16),
              const Text(
                'Đăng xuất thiết bị này?',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 24,
                  fontWeight: FontWeight.w600,
                  color: _onSurface,
                ),
              ),
              const SizedBox(height: 8),
              const Text(
                'Bạn đang đăng xuất khỏi:',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  fontWeight: FontWeight.w400,
                  color: _onSurfaceVariant,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                widget.session.displayName,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 20,
                  fontWeight: FontWeight.w600,
                  color: _primaryColor,
                ),
              ),
              const SizedBox(height: 16),
              _buildInfoNotice(),
              if (_errorMessage != null) ...[
                const SizedBox(height: 12),
                _buildErrorBanner(),
              ],
              const SizedBox(height: 24),
              _buildRevokeButton(),
              const SizedBox(height: 12),
              _buildCancelButton(),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildWarningIcon() {
    return Container(
      width: 64,
      height: 64,
      decoration: const BoxDecoration(
        color: _errorContainer,
        shape: BoxShape.circle,
      ),
      child: const Icon(Icons.warning_rounded, color: _errorColor, size: 36),
    );
  }

  Widget _buildInfoNotice() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: _surfaceContainerLow,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: _surfaceVariant),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(Icons.info_outline, color: _primaryColor, size: 20),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              'Bạn sẽ cần đăng nhập lại trên thiết bị này nếu muốn tiếp tục sử dụng ứng dụng.',
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                fontWeight: FontWeight.w400,
                color: _onSurfaceVariant,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildErrorBanner() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: _errorContainer,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        children: [
          const Icon(Icons.error_outline, color: Color(0xFF93000A), size: 20),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              _errorMessage!,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 13,
                fontWeight: FontWeight.w400,
                color: Color(0xFF93000A),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildRevokeButton() {
    return SizedBox(
      width: double.infinity,
      height: 52,
      child: FilledButton.icon(
        onPressed: _isSubmitting ? null : _revokeSession,
        icon: _isSubmitting
            ? const SizedBox.shrink()
            : const Icon(Icons.logout, size: 20),
        label: _isSubmitting
            ? const SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  color: _onError,
                ),
              )
            : const Text(
                'Đăng xuất thiết bị',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                ),
              ),
        style: FilledButton.styleFrom(
          backgroundColor: _errorColor,
          foregroundColor: _onError,
          disabledBackgroundColor: _errorColor.withValues(alpha: 0.65),
          shape: const StadiumBorder(),
          elevation: 0,
        ),
      ),
    );
  }

  Widget _buildCancelButton() {
    return SizedBox(
      width: double.infinity,
      height: 52,
      child: FilledButton(
        onPressed: _isSubmitting ? null : () => Navigator.of(context).pop(),
        style: FilledButton.styleFrom(
          backgroundColor: _surfaceContainer,
          foregroundColor: _onSurface,
          shape: const StadiumBorder(),
          elevation: 0,
        ),
        child: const Text(
          'Hủy',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 16,
            fontWeight: FontWeight.w600,
          ),
        ),
      ),
    );
  }
}
