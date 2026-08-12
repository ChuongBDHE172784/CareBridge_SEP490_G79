import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/network/api_client.dart';
import '../services/auth_service.dart';

/// Redesigned Role selection screen — UC-02 Role Gate.
/// Enables newly verified users to select their system role: MOTHER, FAMILY, or EXPERT.
class RoleSelectionScreen extends StatefulWidget {
  const RoleSelectionScreen({super.key, this.authService});

  final AuthService? authService;

  @override
  State<RoleSelectionScreen> createState() => _RoleSelectionScreenState();
}

class _RoleSelectionScreenState extends State<RoleSelectionScreen> {
  static const _primary = Color(0xFF845143);
  static const _canvas = Color(0xFFFFF8F6);
  static const _textDark = Color(0xFF2E211C);
  static const _textMuted = Color(0xFF6B5850);
  static const _errorBg = Color(0xFFFFDAD6);
  static const _errorText = Color(0xFF93000A);

  String? _selectedRole;
  bool _loading = false;
  String? _error;

  AuthService get _service => widget.authService ?? AuthService.instance;

  bool get _canContinue => _selectedRole != null && !_loading;

  String get _selectedRoleLabel {
    switch (_selectedRole) {
      case 'MOTHER':
        return 'Mẹ bầu';
      case 'FAMILY':
        return 'Người thân';
      case 'EXPERT':
        return 'Chuyên gia y tế';
      default:
        return '';
    }
  }

  Future<void> _continue() async {
    if (_loading) return;
    final role = _selectedRole;
    if (role == null) return;
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      var effectiveRole = role;
      try {
        final selected = await _service.selectRole(role);
        if (selected.role.isNotEmpty) effectiveRole = selected.role;
      } on ApiException catch (selectionError) {
        // The role write may have committed even when its response was lost.
        // Re-read the profile so retrying is idempotent instead of leaving the
        // user stuck on a "role already assigned" error.
        try {
          final recovered = await _service.getProfile();
          if (recovered.role.isEmpty) throw selectionError;
          effectiveRole = recovered.role;
        } catch (_) {
          rethrow;
        }
      }
      await _service.refreshSession();
      if (!mounted) return;
      if (effectiveRole == 'MOTHER') {
        context.go('/mother-stage-selection');
      } else if (effectiveRole == 'EXPERT') {
        context.go('/expert-onboarding');
      } else {
        context.go('/');
      }
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _error = e.statusCode == 400
            ? 'Vai trò đã được thiết lập hoặc không hợp lệ.'
            : 'Không thể lưu vai trò. Vui lòng thử lại.';
      });
    } catch (_) {
      if (!mounted) return;
      setState(() => _error = 'Không thể kết nối đến máy chủ.');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: Stack(
          children: [
            SingleChildScrollView(
              padding: const EdgeInsets.fromLTRB(24, 28, 24, 160),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _buildHeader(),
                  const SizedBox(height: 28),
                  _RoleCard(
                    roleKey: 'MOTHER',
                    badge: 'PHỔ BIẾN NHẤT',
                    icon: Icons.favorite_rounded,
                    title: 'Mẹ bầu',
                    subtitle:
                        'Theo dõi thai kỳ, nhật ký bé, lịch chăm sóc và chỉ số phát triển.',
                    accentColor: const Color(0xFFC98C7B),
                    selectedBg: const Color(0xFFFAF2EF),
                    selected: _selectedRole == 'MOTHER',
                    onTap: () => setState(() {
                      _selectedRole = 'MOTHER';
                      _error = null;
                    }),
                  ),
                  const SizedBox(height: 16),
                  _RoleCard(
                    roleKey: 'FAMILY',
                    badge: 'GIA ĐÌNH',
                    icon: Icons.family_restroom_rounded,
                    title: 'Người thân',
                    subtitle:
                        'Đồng hành cùng mẹ qua nhóm chăm sóc gia đình và nhận cảnh báo sức khỏe.',
                    accentColor: const Color(0xFFD89B6A),
                    selectedBg: const Color(0xFFFAF5EE),
                    selected: _selectedRole == 'FAMILY',
                    onTap: () => setState(() {
                      _selectedRole = 'FAMILY';
                      _error = null;
                    }),
                  ),
                  const SizedBox(height: 16),
                  _RoleCard(
                    roleKey: 'EXPERT',
                    badge: 'Y TẾ & TƯ VẤN',
                    icon: Icons.medical_services_rounded,
                    title: 'Chuyên gia',
                    subtitle:
                        'Tư vấn chuyên môn, giải đáp thắc mắc và đồng hành cùng sản phụ.',
                    accentColor: const Color(0xFF4A8B88),
                    selectedBg: const Color(0xFFEFF7F6),
                    selected: _selectedRole == 'EXPERT',
                    onTap: () => setState(() {
                      _selectedRole = 'EXPERT';
                      _error = null;
                    }),
                  ),
                ],
              ),
            ),
            Positioned(left: 0, right: 0, bottom: 0, child: _buildBottomBar()),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
          decoration: BoxDecoration(
            color: _primary.withValues(alpha: 0.1),
            borderRadius: BorderRadius.circular(20),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.explore_outlined, size: 14, color: _primary),
              const SizedBox(width: 6),
              Text(
                'BƯỚC 1 / 2 • XÁC THỰC VAI TRÒ',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 11,
                  fontWeight: FontWeight.w700,
                  letterSpacing: 0.8,
                  color: _primary,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        const Text(
          'Chọn vai trò',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 32,
            fontWeight: FontWeight.w800,
            color: _textDark,
            height: 1.2,
            letterSpacing: -0.5,
          ),
        ),
        const SizedBox(height: 8),
        const Text(
          'CareBridge sẽ cá nhân hóa hành trình và các tính năng phù hợp nhất với bạn.',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 15,
            color: _textMuted,
            height: 1.5,
          ),
        ),
      ],
    );
  }

  Widget _buildBottomBar() {
    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.bottomCenter,
          end: Alignment.topCenter,
          colors: [
            _canvas,
            _canvas.withValues(alpha: 0.95),
            _canvas.withValues(alpha: 0.0),
          ],
          stops: const [0.0, 0.75, 1.0],
        ),
      ),
      padding: const EdgeInsets.fromLTRB(24, 32, 24, 24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (_error != null) ...[
            Container(
              width: double.infinity,
              margin: const EdgeInsets.only(bottom: 12),
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              decoration: BoxDecoration(
                color: _errorBg,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: _errorText.withValues(alpha: 0.2)),
              ),
              child: Row(
                children: [
                  const Icon(Icons.error_outline, color: _errorText, size: 20),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      _error!,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 13,
                        fontWeight: FontWeight.w500,
                        color: _errorText,
                        height: 1.4,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ] else if (_selectedRole != null) ...[
            Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.check_circle, size: 16, color: _primary),
                  const SizedBox(width: 6),
                  Text(
                    'Đã chọn: $_selectedRoleLabel',
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                      color: _primary,
                    ),
                  ),
                ],
              ),
            ),
          ] else ...[
            const Padding(
              padding: EdgeInsets.only(bottom: 10),
              child: Text(
                'Vui lòng chọn 1 vai trò để tiếp tục',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 13,
                  color: _textMuted,
                ),
              ),
            ),
          ],
          SizedBox(
            width: double.infinity,
            height: 56,
            child: FilledButton(
              onPressed: _canContinue ? _continue : null,
              style: FilledButton.styleFrom(
                backgroundColor: _primary,
                foregroundColor: Colors.white,
                disabledBackgroundColor: _primary.withValues(alpha: 0.40),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(28),
                ),
                elevation: _canContinue ? 4 : 0,
                shadowColor: _primary.withValues(alpha: 0.35),
              ),
              child: _loading
                  ? const SizedBox(
                      width: 24,
                      height: 24,
                      child: CircularProgressIndicator(
                        strokeWidth: 2.5,
                        color: Colors.white,
                      ),
                    )
                  : Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: const [
                        Text(
                          'Tiếp tục',
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 16,
                            fontWeight: FontWeight.w700,
                            letterSpacing: 0.3,
                          ),
                        ),
                        SizedBox(width: 8),
                        Icon(Icons.arrow_forward_rounded, size: 20),
                      ],
                    ),
            ),
          ),
        ],
      ),
    );
  }
}

class _RoleCard extends StatelessWidget {
  const _RoleCard({
    required this.roleKey,
    required this.badge,
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.accentColor,
    required this.selectedBg,
    required this.selected,
    required this.onTap,
  });

  final String roleKey;
  final String badge;
  final IconData icon;
  final String title;
  final String subtitle;
  final Color accentColor;
  final Color selectedBg;
  final bool selected;
  final VoidCallback onTap;

  static const _textDark = Color(0xFF2E211C);
  static const _textMuted = Color(0xFF7A6860);
  static const _borderNormal = Color(0xFFE8DDD6);

  @override
  Widget build(BuildContext context) {
    return AnimatedContainer(
      duration: const Duration(milliseconds: 220),
      curve: Curves.easeInOut,
      decoration: BoxDecoration(
        color: selected ? selectedBg : Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(
          color: selected ? accentColor : _borderNormal.withValues(alpha: 0.8),
          width: selected ? 2.5 : 1.2,
        ),
        boxShadow: [
          BoxShadow(
            color: selected
                ? accentColor.withValues(alpha: 0.18)
                : const Color(0xFF5A463F).withValues(alpha: 0.05),
            blurRadius: selected ? 24 : 16,
            offset: Offset(0, selected ? 10 : 6),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(24),
        child: InkWell(
          borderRadius: BorderRadius.circular(24),
          onTap: onTap,
          splashColor: accentColor.withValues(alpha: 0.12),
          highlightColor: accentColor.withValues(alpha: 0.06),
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Container(
                      width: 48,
                      height: 48,
                      decoration: BoxDecoration(
                        color: selected
                            ? accentColor
                            : accentColor.withValues(alpha: 0.14),
                        borderRadius: BorderRadius.circular(16),
                      ),
                      child: Icon(
                        icon,
                        color: selected ? Colors.white : accentColor,
                        size: 26,
                      ),
                    ),
                    const SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 8,
                              vertical: 3,
                            ),
                            decoration: BoxDecoration(
                              color: accentColor.withValues(alpha: 0.12),
                              borderRadius: BorderRadius.circular(8),
                            ),
                            child: Text(
                              badge,
                              style: TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 10,
                                fontWeight: FontWeight.w700,
                                letterSpacing: 0.6,
                                color: accentColor,
                              ),
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            title,
                            style: TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 20,
                              fontWeight: FontWeight.w700,
                              color: _textDark,
                              height: 1.2,
                            ),
                          ),
                        ],
                      ),
                    ),
                    AnimatedContainer(
                      duration: const Duration(milliseconds: 180),
                      width: 28,
                      height: 28,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: selected ? accentColor : Colors.transparent,
                        border: Border.all(
                          color: selected
                              ? accentColor
                              : _borderNormal.withValues(alpha: 0.9),
                          width: selected ? 0 : 2,
                        ),
                      ),
                      child: selected
                          ? const Icon(
                              Icons.check_rounded,
                              color: Colors.white,
                              size: 18,
                            )
                          : null,
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                Text(
                  subtitle,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    color: _textMuted,
                    height: 1.45,
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
