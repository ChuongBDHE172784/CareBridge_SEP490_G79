import 'package:flutter/material.dart';
import '../../../../core/auth/auth_state.dart';
import '../../../../core/network/api_client.dart';
import '../../../../core/notifications/fcm_service.dart';
import '../../journey/services/pregnancy_outcome_draft_store.dart';
import '../../recommendation/services/recommendation_service.dart';
import '../services/auth_service.dart';

@visibleForTesting
Future<void> clearLocalSessionAfterLogout({
  required String? accountId,
  required bool Function() isCapturedSessionCurrent,
  required Future<void> Function(String accountId) clearDraft,
  Future<void> Function(String accountId)? clearRecommendationDraft,
  required Future<void> Function() clearAuth,
}) async {
  try {
    if (accountId != null) {
      try {
        await clearDraft(accountId);
      } catch (_) {
        debugPrint('[Logout] pregnancy outcome draft cleanup failed');
      }
      if (clearRecommendationDraft != null) {
        try {
          await clearRecommendationDraft(accountId);
        } catch (_) {
          debugPrint('[Logout] recommendation draft cleanup failed');
        }
      }
    }
  } finally {
    if (isCapturedSessionCurrent()) await clearAuth();
  }
}

Future<void> showLogoutConfirmationSheet(BuildContext context) async {
  final auth = AuthState.instance;
  final accountId = auth.userId;
  final sessionGeneration = auth.sessionGeneration;
  if (accountId == null || auth.accessToken == null) return;

  bool isCapturedSessionCurrent() =>
      auth.matchesSession(generation: sessionGeneration, userId: accountId);

  final confirmed = await showModalBottomSheet<bool>(
    context: context,
    isScrollControlled: true,
    isDismissible: false,
    enableDrag: false,
    backgroundColor: Colors.transparent,
    barrierColor: Colors.black.withValues(alpha: 0.5),
    builder: (_) => LogoutConfirmationSheet(
      onLogout: () {
        if (!isCapturedSessionCurrent()) {
          throw ApiException(401, '{"error":"AUTH_SESSION_CHANGED"}');
        }
        return _logoutAndDeregister(
          auth: auth,
          accountId: accountId,
          isCapturedSessionCurrent: isCapturedSessionCurrent,
        );
      },
    ),
  );

  if (confirmed != true) return;

  // Let the popup route finish leaving the Navigator before AuthState notifies
  // GoRouter and redirects to the unauthenticated landing route.
  await WidgetsBinding.instance.endOfFrame;
  await clearLocalSessionAfterLogout(
    accountId: accountId,
    isCapturedSessionCurrent: isCapturedSessionCurrent,
    clearDraft: SecurePregnancyOutcomeDraftStore.instance.clearForAccount,
    clearRecommendationDraft: RecommendationService().clearDraftFor,
    clearAuth: () async {
      await auth.clearIfCurrentSession(
        generation: sessionGeneration,
        userId: accountId,
      );
    },
  );
}

Future<void> _logoutAndDeregister({
  required AuthState auth,
  required String accountId,
  required bool Function() isCapturedSessionCurrent,
}) async {
  try {
    await FcmService.instance.deregisterToken();
  } catch (error) {
    debugPrint('[Logout] notification token cleanup failed: ${error.runtimeType}');
  }
  if (!isCapturedSessionCurrent()) {
    throw ApiException(401, '{"error":"AUTH_SESSION_CHANGED"}');
  }
  await AuthService.instance.logout(
    token: auth.accessToken,
    expectedAccountId: accountId,
    refreshToken: auth.refreshToken,
  );
}

/// CB-116 - Logout Confirmation (UC-04)
/// Confirms logout, calls the real backend contract, then clears local auth state.
class LogoutConfirmationSheet extends StatefulWidget {
  const LogoutConfirmationSheet({required this.onLogout, super.key});

  final Future<void> Function() onLogout;

  @override
  State<LogoutConfirmationSheet> createState() =>
      _LogoutConfirmationSheetState();
}

class _LogoutConfirmationSheetState extends State<LogoutConfirmationSheet> {
  static const _canvasColor = Color(0xFFF6F1EC);
  static const _surfaceColor = Color(0xFFFFF8F6);
  static const _primaryColor = Color(0xFFC98C7B);
  static const _textColor = Color(0xFF271812);
  static const _secondaryTextColor = Color(0xFF524440);
  static const _outlineColor = Color(0xFFD6C2BD);

  bool _isSubmitting = false;
  String? _errorMessage;

  Future<void> _confirmLogout() async {
    if (_isSubmitting) return;

    setState(() {
      _isSubmitting = true;
      _errorMessage = null;
    });

    try {
      await widget.onLogout();
      if (mounted) Navigator.of(context).pop(true);
    } on ApiException catch (error) {
      if (error.statusCode == 401) {
        if (mounted) Navigator.of(context).pop(true);
        return;
      }

      if (!mounted) return;
      setState(() {
        _errorMessage = 'Không thể đăng xuất lúc này. Vui lòng thử lại.';
        _isSubmitting = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _errorMessage =
            'Không thể kết nối đến máy chủ. Kiểm tra mạng và thử lại.';
        _isSubmitting = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final bottomInset = MediaQuery.of(context).viewInsets.bottom;

    return PopScope(
      canPop: !_isSubmitting,
      child: AnimatedPadding(
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
              borderRadius: BorderRadius.vertical(top: Radius.circular(32)),
              boxShadow: [
                BoxShadow(
                  color: Color.fromRGBO(90, 70, 63, 0.12),
                  blurRadius: 32,
                  offset: Offset(0, -8),
                ),
              ],
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  width: 52,
                  height: 6,
                  decoration: BoxDecoration(
                    color: _outlineColor,
                    borderRadius: BorderRadius.circular(999),
                  ),
                ),
                const SizedBox(height: 28),
                _buildIconCluster(),
                const SizedBox(height: 28),
                const Text(
                  'Đăng xuất',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 24,
                    fontWeight: FontWeight.w700,
                    color: _textColor,
                  ),
                ),
                const SizedBox(height: 12),
                const Text(
                  'Bạn có chắc chắn muốn đăng xuất?',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 16,
                    fontWeight: FontWeight.w400,
                    color: _secondaryTextColor,
                    height: 1.4,
                  ),
                ),
                const SizedBox(height: 18),
                _buildAccountPill(),
                if (_errorMessage != null) ...[
                  const SizedBox(height: 16),
                  _buildErrorBanner(),
                ],
                const SizedBox(height: 28),
                _buildPrimaryButton(),
                const SizedBox(height: 14),
                _buildSecondaryButton(),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildIconCluster() {
    return Stack(
      clipBehavior: Clip.none,
      children: [
        Container(
          width: 96,
          height: 96,
          decoration: BoxDecoration(
            color: _primaryColor.withValues(alpha: 0.10),
            shape: BoxShape.circle,
          ),
          child: Center(
            child: Container(
              width: 82,
              height: 82,
              decoration: BoxDecoration(
                color: _primaryColor.withValues(alpha: 0.18),
                shape: BoxShape.circle,
              ),
              child: Center(
                child: Container(
                  width: 66,
                  height: 66,
                  decoration: const BoxDecoration(
                    color: _primaryColor,
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(
                    Icons.logout_rounded,
                    color: Colors.white,
                    size: 34,
                  ),
                ),
              ),
            ),
          ),
        ),
        Positioned(
          right: -4,
          bottom: -2,
          child: Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(
              color: Colors.white,
              shape: BoxShape.circle,
              border: Border.all(color: _outlineColor.withValues(alpha: 0.35)),
              boxShadow: const [
                BoxShadow(
                  color: Color.fromRGBO(90, 70, 63, 0.10),
                  blurRadius: 16,
                  offset: Offset(0, 6),
                ),
              ],
            ),
            child: const Icon(
              Icons.exit_to_app_rounded,
              color: Color(0xFF845143),
              size: 22,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildAccountPill() {
    final accountName = _resolveAccountName();
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
      decoration: BoxDecoration(
        color: _canvasColor,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(
            Icons.account_circle_outlined,
            color: Color(0xFF845143),
            size: 24,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: RichText(
              overflow: TextOverflow.ellipsis,
              text: TextSpan(
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 15,
                  fontWeight: FontWeight.w400,
                  color: _secondaryTextColor,
                ),
                children: [
                  const TextSpan(text: 'Tài khoản: '),
                  TextSpan(
                    text: accountName,
                    style: const TextStyle(
                      fontWeight: FontWeight.w700,
                      color: Color(0xFF845143),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  String _resolveAccountName() {
    return switch (AuthState.instance.role?.trim().toUpperCase()) {
      'MOTHER' => 'Mẹ',
      'FAMILY' => 'Gia đình',
      'EXPERT' => 'Chuyên gia',
      _ => 'CareBridge',
    };
  }

  Widget _buildErrorBanner() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: const Color(0xFFFFDAD6),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        children: [
          const Icon(
            Icons.error_outline_rounded,
            color: Color(0xFF93000A),
            size: 20,
          ),
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

  Widget _buildPrimaryButton() {
    return SizedBox(
      width: double.infinity,
      height: 56,
      child: FilledButton(
        onPressed: _isSubmitting ? null : _confirmLogout,
        style: FilledButton.styleFrom(
          backgroundColor: _primaryColor,
          foregroundColor: Colors.white,
          disabledBackgroundColor: _primaryColor.withValues(alpha: 0.65),
          shape: const StadiumBorder(),
          elevation: 0,
        ),
        child: _isSubmitting
            ? const SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  color: Colors.white,
                ),
              )
            : const Text(
                'Đăng xuất',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                ),
              ),
      ),
    );
  }

  Widget _buildSecondaryButton() {
    return SizedBox(
      width: double.infinity,
      height: 56,
      child: OutlinedButton(
        onPressed: _isSubmitting ? null : () => Navigator.of(context).pop(),
        style: OutlinedButton.styleFrom(
          backgroundColor: _canvasColor.withValues(alpha: 0.55),
          foregroundColor: _textColor,
          side: BorderSide(color: _outlineColor.withValues(alpha: 0.6)),
          shape: const StadiumBorder(),
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
