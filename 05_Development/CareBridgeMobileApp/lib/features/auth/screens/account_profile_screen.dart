import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../models/auth_model.dart';
import '../services/auth_service.dart';
import 'edit_profile_screen.dart';
import 'change_password_screen.dart';
import 'logout_confirmation_screen.dart';
import 'deactivate_account_screen.dart';
import 'linked_accounts_screen.dart';
import '../../../core/auth/auth_state.dart';
import '../../../features/session/screens/login_sessions_screen.dart';
import '../../../features/privacy/screens/privacy_settings_screen.dart';
import '../../../features/familySync/screens/care_groups_screen.dart';
import '../../../features/expert/screens/expert_question_queue_screen.dart';
import '../../../features/fileManager/screens/file_manager_screen.dart';

class AccountProfileScreen extends StatefulWidget {
  const AccountProfileScreen({super.key, this.loadProfile});

  final Future<UserProfile> Function()? loadProfile;

  @override
  State<AccountProfileScreen> createState() => _AccountProfileScreenState();
}

class _AccountProfileScreenState extends State<AccountProfileScreen> {
  static const _bgColor = Color(0xFFFFF8F6);
  static const _primaryColor = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _onSurface = Color(0xFF271812);
  // ignore: unused_field
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);
  static const _surfaceContainerLowest = Color(0xFFFFFFFF);
  static const _surfaceContainerLow = Color(0xFFFFF1EC);
  static const _errorColor = Color(0xFFBA1A1A);
  static const _errorContainer = Color(0xFFFFDAD6);

  UserProfile? _profile;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadProfile();
  }

  Future<void> _loadProfile() async {
    setState(() => _isLoading = true);
    try {
      final loader = widget.loadProfile ?? AuthService.instance.getProfile;
      final profile = await loader();
      if (mounted) {
        setState(() {
          _profile = profile;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _bgColor,
      body: SafeArea(
        child: _isLoading
            ? const Center(
                child: CircularProgressIndicator(color: _primaryContainer),
              )
            : Column(
                children: [
                  _buildAppBar(),
                  Expanded(child: _buildContent()),
                ],
              ),
      ),
    );
  }

  Widget _buildAppBar() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: SizedBox(
        height: 48,
        child: Row(
          children: [
            if (Navigator.of(context).canPop())
              IconButton(
                onPressed: () => Navigator.of(context).pop(),
                icon: const Icon(Icons.arrow_back, color: _primaryColor),
              )
            else
              const SizedBox(width: 48),
            const Expanded(
              child: Text(
                'Hồ sơ tài khoản',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 20,
                  fontWeight: FontWeight.w600,
                  color: _primaryColor,
                ),
              ),
            ),
            const SizedBox(width: 48),
          ],
        ),
      ),
    );
  }

  Widget _buildContent() {
    final p = _profile;
    return RefreshIndicator(
      color: _primaryContainer,
      onRefresh: _loadProfile,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(24, 24, 24, 32),
        children: [
          _buildAvatarSection(p),
          const SizedBox(height: 24),
          _buildMenuCard([
            _menuItem(Icons.person_outline, 'Chỉnh sửa hồ sơ', () async {
              await Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => const EditProfileScreen()),
              );
              _loadProfile();
            }),
            _menuItem(Icons.lock_outline, 'Đổi mật khẩu', () {
              Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => const ChangePasswordScreen()),
              );
            }),
            _menuItem(Icons.notifications_outlined, 'Cài đặt thông báo', () {
              // TODO: navigate to notification preferences (CB-113)
            }),
          ]),
          const SizedBox(height: 16),
          _buildMenuCard([
            _menuItem(Icons.group_outlined, 'Nhóm chăm sóc', () {
              Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => const CareGroupsScreen()),
              );
            }),
            _menuItem(Icons.folder_outlined, 'Quản lý tệp', () {
              Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => const FileManagerScreen()),
              );
            }),
          ]),
          const SizedBox(height: 16),
          _buildMenuCard([
            _menuItem(
              Icons.link_rounded,
              'Tài khoản liên kết',
              () {
                Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (_) => const LinkedAccountsScreen(),
                  ),
                );
              },
              key: const Key('linked-accounts-menu-item'),
            ),
            _menuItem(Icons.devices_outlined, 'Phiên đăng nhập', () {
              Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => const LoginSessionsScreen()),
              );
            }),
            _menuItem(Icons.shield_outlined, 'Quyền riêng tư', () {
              Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (_) => const PrivacySettingsScreen(),
                ),
              );
            }),
            _menuItem(Icons.person_off_outlined, 'Vô hiệu hoá tài khoản', () {
              Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (_) => const DeactivateAccountScreen(),
                ),
              );
            }),
          ]),
          if (AuthState.instance.role == 'EXPERT') ...[
            const SizedBox(height: 16),
            _buildMenuCard([
              _menuItem(
                Icons.question_answer_outlined,
                'Hàng đợi câu hỏi (Expert)',
                () {
                  Navigator.of(context).push(
                    MaterialPageRoute(
                      builder: (_) => ExpertQuestionQueueScreen(
                        expertName: _profile?.name,
                        expertAvatarUrl: _profile?.avatarUrl,
                      ),
                    ),
                  );
                },
              ),
            ]),
          ] else ...[
            const SizedBox(height: 16),
            _buildMenuCard([
              _menuItem(
                Icons.medical_services_outlined,
                'Đăng ký thành Chuyên gia',
                () {
                  context.go('/expert-onboarding');
                },
              ),
            ]),
          ],
          const SizedBox(height: 16),
          _buildLogoutCard(),
        ],
      ),
    );
  }

  Widget _buildAvatarSection(UserProfile? p) {
    return Column(
      children: [
        Stack(
          children: [
            CircleAvatar(
              radius: 56,
              backgroundColor: _surfaceContainerLow,
              backgroundImage: p?.avatarUrl != null
                  ? NetworkImage(p!.avatarUrl!)
                  : null,
              child: p?.avatarUrl == null
                  ? const Icon(Icons.person, size: 48, color: _primaryContainer)
                  : null,
            ),
            Positioned(
              bottom: 0,
              right: 0,
              child: Container(
                width: 32,
                height: 32,
                decoration: BoxDecoration(
                  color: _primaryContainer,
                  shape: BoxShape.circle,
                  border: Border.all(color: Colors.white, width: 2),
                ),
                child: const Icon(Icons.edit, size: 16, color: Colors.white),
              ),
            ),
          ],
        ),
        const SizedBox(height: 16),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(
              p?.name ?? 'Người dùng',
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 24,
                fontWeight: FontWeight.w700,
                color: _onSurface,
              ),
            ),
            const SizedBox(width: 4),
            const Icon(
              Icons.verified_outlined,
              color: _primaryContainer,
              size: 20,
            ),
          ],
        ),
        const SizedBox(height: 8),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(
              Icons.email_outlined,
              color: _onSurfaceVariant,
              size: 18,
            ),
            const SizedBox(width: 6),
            Flexible(
              child: Text(
                p?.email?.trim().isNotEmpty == true
                    ? p!.email!.trim()
                    : 'Chưa có email',
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  fontWeight: FontWeight.w500,
                  color: _onSurfaceVariant,
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
          decoration: BoxDecoration(
            color: _surfaceContainerLow,
            borderRadius: BorderRadius.circular(9999),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.star_outline, color: _primaryColor, size: 16),
              const SizedBox(width: 4),
              Text(
                'NGƯỜI DÙNG VIP',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 12,
                  fontWeight: FontWeight.w500,
                  letterSpacing: 0.6,
                  color: _primaryColor,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildMenuCard(List<Widget> items) {
    return Container(
      decoration: BoxDecoration(
        color: _surfaceContainerLowest,
        borderRadius: BorderRadius.circular(16),
        boxShadow: const [
          BoxShadow(
            color: Color.fromRGBO(90, 70, 63, 0.06),
            blurRadius: 20,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Column(children: items),
    );
  }

  Widget _menuItem(
    IconData icon,
    String label,
    VoidCallback onTap, {
    Key? key,
  }) {
    return InkWell(
      key: key,
      onTap: onTap,
      borderRadius: BorderRadius.circular(16),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 18),
        child: Row(
          children: [
            Container(
              width: 40,
              height: 40,
              decoration: BoxDecoration(
                color: _surfaceContainerLow,
                shape: BoxShape.circle,
              ),
              child: Icon(icon, color: _primaryColor, size: 20),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Text(
                label,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  fontWeight: FontWeight.w400,
                  color: _onSurface,
                ),
              ),
            ),
            const Icon(Icons.chevron_right, color: _outlineVariant, size: 20),
          ],
        ),
      ),
    );
  }

  Widget _buildLogoutCard() {
    return Container(
      decoration: BoxDecoration(
        color: _surfaceContainerLowest,
        borderRadius: BorderRadius.circular(16),
        boxShadow: const [
          BoxShadow(
            color: Color.fromRGBO(90, 70, 63, 0.06),
            blurRadius: 20,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: InkWell(
        onTap: () => showLogoutConfirmationSheet(context),
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 18),
          child: Row(
            children: [
              Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: _errorContainer.withValues(alpha: 0.4),
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.logout, color: _errorColor, size: 20),
              ),
              const SizedBox(width: 16),
              const Text(
                'Đăng xuất',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: _errorColor,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
