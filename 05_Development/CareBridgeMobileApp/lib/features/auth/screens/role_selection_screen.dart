import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/network/api_client.dart';
import '../services/auth_service.dart';

/// Role selection gate for newly verified accounts before role-specific setup.
class RoleSelectionScreen extends StatefulWidget {
  const RoleSelectionScreen({super.key});

  @override
  State<RoleSelectionScreen> createState() => _RoleSelectionScreenState();
}

class _RoleSelectionScreenState extends State<RoleSelectionScreen> {
  static const _primary = Color(0xFFC98C7B);
  static const _primaryDark = Color(0xFF845143);
  static const _canvas = Color(0xFFF6F1EC);
  static const _text = Color(0xFF5A463F);
  static const _errorBg = Color(0xFFFFDAD6);
  static const _errorText = Color(0xFF93000A);

  String? _selectedRole;
  bool _loading = false;
  String? _error;

  bool get _canContinue => _selectedRole != null && !_loading;

  Future<void> _continue() async {
    final role = _selectedRole;
    if (role == null) return;
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      await AuthService.instance.selectRole(role);
      await AuthService.instance.refreshSession();
      if (!mounted) return;
      if (role == 'MOTHER') {
        context.go('/journey-onboarding');
      } else {
        context.go('/');
      }
    } on ApiException catch (e) {
      setState(() {
        _error = e.statusCode == 400
            ? 'Vai trò đã được thiết lập hoặc không hợp lệ.'
            : 'Không thể lưu vai trò. Vui lòng thử lại.';
      });
    } catch (_) {
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
              padding: const EdgeInsets.fromLTRB(24, 32, 24, 140),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _buildHeader(),
                  const SizedBox(height: 28),
                  _RoleCard(
                    icon: Icons.favorite_rounded,
                    title: 'Mẹ bầu',
                    subtitle:
                        'Theo dõi thai kỳ, lịch chăm sóc và hành trình sức khỏe.',
                    selected: _selectedRole == 'MOTHER',
                    onTap: () => setState(() {
                      _selectedRole = 'MOTHER';
                      _error = null;
                    }),
                  ),
                  const SizedBox(height: 16),
                  _RoleCard(
                    icon: Icons.groups_rounded,
                    title: 'Người thân',
                    subtitle:
                        'Đồng hành cùng mẹ qua nhóm chăm sóc và cảnh báo gia đình.',
                    selected: _selectedRole == 'FAMILY',
                    onTap: () => setState(() {
                      _selectedRole = 'FAMILY';
                      _error = null;
                    }),
                  ),
                  const SizedBox(height: 16),
                  _RoleCard(
                    icon: Icons.medical_services_rounded,
                    title: 'Chuyên gia',
                    subtitle:
                        'Hỗ trợ tư vấn, phản hồi câu hỏi và theo dõi yêu cầu chuyên môn.',
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
          width: 56,
          height: 56,
          decoration: BoxDecoration(
            color: _primary.withValues(alpha: 0.15),
            shape: BoxShape.circle,
          ),
          child: const Icon(Icons.route_rounded, color: _primary, size: 28),
        ),
        const SizedBox(height: 20),
        const Text(
          'Chọn vai trò',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 32,
            fontWeight: FontWeight.w700,
            color: _primaryDark,
            height: 1.2,
          ),
        ),
        const SizedBox(height: 12),
        const Text(
          'CareBridge sẽ mở đúng hành trình thiết lập cho vai trò của bạn.',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 16,
            color: _text,
            height: 1.5,
          ),
        ),
      ],
    );
  }

  Widget _buildBottomBar() {
    return Container(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.bottomCenter,
          end: Alignment.topCenter,
          colors: [_canvas, _canvas, Colors.transparent],
          stops: [0.0, 0.72, 1.0],
        ),
      ),
      padding: const EdgeInsets.fromLTRB(24, 48, 24, 28),
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
                borderRadius: BorderRadius.circular(18),
              ),
              child: Text(
                _error!,
                textAlign: TextAlign.center,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  color: _errorText,
                  height: 1.4,
                ),
              ),
            ),
          ],
          SizedBox(
            width: double.infinity,
            height: 54,
            child: FilledButton(
              onPressed: _canContinue ? _continue : null,
              style: FilledButton.styleFrom(
                backgroundColor: _primary,
                foregroundColor: Colors.white,
                disabledBackgroundColor: _primary.withValues(alpha: 0.48),
                shape: const StadiumBorder(),
                elevation: 0,
              ),
              child: _loading
                  ? const SizedBox(
                      width: 22,
                      height: 22,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : const Text(
                      'Tiếp tục',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                      ),
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
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.selected,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final bool selected;
  final VoidCallback onTap;

  static const _primary = Color(0xFFC98C7B);
  static const _surface = Colors.white;
  static const _surfaceLow = Color(0xFFF2EAE4);
  static const _text = Color(0xFF5A463F);
  static const _muted = Color(0xFF9C857C);
  static const _border = Color(0xFFE8DDD6);

  @override
  Widget build(BuildContext context) {
    return AnimatedContainer(
      duration: const Duration(milliseconds: 240),
      decoration: BoxDecoration(
        color: selected ? _surfaceLow : _surface,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(
          color: selected ? _primary : _border.withValues(alpha: 0.6),
          width: 2,
        ),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF5A463F).withValues(alpha: 0.06),
            blurRadius: 28,
            offset: const Offset(0, 12),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(24),
        child: InkWell(
          borderRadius: BorderRadius.circular(24),
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  width: 52,
                  height: 52,
                  decoration: BoxDecoration(
                    color: selected ? _primary : _surfaceLow,
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    icon,
                    color: selected ? Colors.white : _primary,
                    size: 28,
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        title,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 20,
                          fontWeight: FontWeight.w700,
                          color: _text,
                          height: 1.25,
                        ),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        subtitle,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 14,
                          color: _muted,
                          height: 1.45,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 12),
                AnimatedOpacity(
                  opacity: selected ? 1 : 0,
                  duration: const Duration(milliseconds: 160),
                  child: const Icon(
                    Icons.check_circle_rounded,
                    color: _primary,
                    size: 24,
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
