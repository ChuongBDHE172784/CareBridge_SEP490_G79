import 'package:flutter/material.dart';
import '../../../../core/network/api_client.dart';
import '../services/auth_service.dart';
import 'otp_verification_screen.dart';
import 'login_screen.dart';

/// CB-002 — Register Account (UC-01)
/// Collects name, email/phone, role, password → calls POST /api/v1/auth/register → navigates to OTP screen.
/// Note: RegisterRequest does not accept 'name'. The name field is collected here for UX
/// but is updated separately via PUT /api/v1/auth/profile (implemented in CB-111 Edit Profile).
class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  static const _primaryColor = Color(0xFFC98C7B);
  static const _canvasColor = Color(0xFFF6F1EC);
  static const _textColor = Color(0xFF5A463F);
  static const _mutedColor = Color(0xFF524440);
  static const _surfaceColor = Color(0xFFFFF8F6);
  static const _surfaceContainerLowest = Colors.white;
  static const _borderColor = Color(0xFFD6C2BD);
  static const _accentPrimary = Color(0xFF845143);
  static const _errorColor = Color(0xFFBA1A1A);

  final _nameCtrl = TextEditingController();
  final _identifierCtrl = TextEditingController();
  final _passwordCtrl = TextEditingController();
  final _confirmPasswordCtrl = TextEditingController();

  String _selectedRole = 'MOTHER';
  bool _obscurePassword = true;
  bool _obscureConfirm = true;
  bool _termsAccepted = false;
  bool _isLoading = false;
  String? _errorMessage;
  String? _confirmPasswordError;

  final _roles = const [
    ('MOTHER', 'Mẹ bầu'),
    ('FAMILY', 'Người thân'),
    ('EXPERT', 'Chuyên gia'),
  ];

  @override
  void dispose() {
    _nameCtrl.dispose();
    _identifierCtrl.dispose();
    _passwordCtrl.dispose();
    _confirmPasswordCtrl.dispose();
    super.dispose();
  }

  bool get _isEmailInput => _identifierCtrl.text.contains('@');

  bool get _hasMinLength => _passwordCtrl.text.length >= 8;
  bool get _hasSpecialChar =>
      RegExp(r'[@#$%^&*!]').hasMatch(_passwordCtrl.text);

  Future<void> _submit() async {
    final identifier = _identifierCtrl.text.trim();
    final password = _passwordCtrl.text;
    final confirmPassword = _confirmPasswordCtrl.text;

    if (identifier.isEmpty || password.isEmpty) {
      setState(() => _errorMessage = 'Vui lòng nhập đầy đủ thông tin.');
      return;
    }
    if (password != confirmPassword) {
      setState(() => _confirmPasswordError = 'Mật khẩu không khớp');
      return;
    }
    if (!_hasMinLength || !_hasSpecialChar) {
      setState(() => _errorMessage = 'Mật khẩu chưa đáp ứng yêu cầu.');
      return;
    }
    if (!_termsAccepted) {
      setState(() => _errorMessage = 'Vui lòng đồng ý với Điều khoản sử dụng.');
      return;
    }

    setState(() {
      _isLoading = true;
      _errorMessage = null;
      _confirmPasswordError = null;
    });

    try {
      await AuthService.instance.register(
        email: _isEmailInput ? identifier : null,
        phone: !_isEmailInput ? identifier : null,
        password: password,
        role: _selectedRole,
      );
      if (!mounted) return;
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) => OtpVerificationScreen(
            identifier: identifier,
            isEmail: _isEmailInput,
          ),
        ),
      );
    } on ApiException catch (e) {
      String msg;
      if (e.statusCode == 409) {
        msg = 'Email hoặc số điện thoại này đã được đăng ký.';
      } else if (e.statusCode == 400) {
        msg = 'Thông tin không hợp lệ. Vui lòng kiểm tra lại.';
      } else {
        msg = 'Đăng ký thất bại. Vui lòng thử lại sau.';
      }
      setState(() => _errorMessage = msg);
    } catch (_) {
      setState(() => _errorMessage = 'Không thể kết nối đến máy chủ.');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvasColor,
      body: SafeArea(
        child: Column(
          children: [
            _buildAppBar(context),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.symmetric(horizontal: 24),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const SizedBox(height: 16),
                    _buildHeading(),
                    const SizedBox(height: 16),
                    if (_errorMessage != null) ...[
                      _buildErrorBanner(_errorMessage!),
                      const SizedBox(height: 12),
                    ],
                    _buildFormCard(),
                    const SizedBox(height: 80),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: _buildStickyFooter(context),
    );
  }

  Widget _buildAppBar(BuildContext context) {
    return Container(
      height: 48,
      padding: const EdgeInsets.symmetric(horizontal: 4),
      decoration: BoxDecoration(color: _canvasColor.withValues(alpha: 0.9)),
      child: Row(
        children: [
          IconButton(
            onPressed: () => Navigator.pop(context),
            icon: const Icon(Icons.arrow_back),
            color: _textColor,
          ),
          Expanded(
            child: Text(
              'Tạo tài khoản',
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 20,
                fontWeight: FontWeight.w600,
                color: _textColor,
              ),
            ),
          ),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildHeading() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'Chào mừng bạn! 👋',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 24,
            fontWeight: FontWeight.w600,
            color: _accentPrimary,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          'Bắt đầu hành trình chăm sóc tuyệt vời cùng CareBridge.',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 14,
            color: _mutedColor,
          ),
        ),
      ],
    );
  }

  Widget _buildErrorBanner(String message) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: const Color(0xFFFFDAD6),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          const Icon(Icons.error_outline, size: 18, color: Color(0xFF93000A)),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              message,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                color: Color(0xFF93000A),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFormCard() {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: _surfaceColor,
        borderRadius: BorderRadius.circular(32),
        boxShadow: [
          BoxShadow(
            color: _textColor.withValues(alpha: 0.06),
            blurRadius: 20,
            offset: const Offset(0, 4),
          ),
        ],
        border: Border.all(color: const Color(0xFFFFF1EC)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Name field
          _buildLabeledInput(
            id: 'name',
            label: 'Họ và tên',
            controller: _nameCtrl,
            hint: 'Nguyễn Thùy Linh',
            keyboardType: TextInputType.name,
          ),
          const SizedBox(height: 16),
          // Identifier field
          _buildLabeledInput(
            id: 'contact',
            label: 'Email hoặc Số điện thoại',
            controller: _identifierCtrl,
            hint: '09xx xxx xxx',
            keyboardType: TextInputType.emailAddress,
          ),
          const SizedBox(height: 16),
          // Role selector
          _buildRoleSelector(),
          const SizedBox(height: 16),
          // Password field
          _buildPasswordInput(
            label: 'Mật khẩu',
            controller: _passwordCtrl,
            hint: '••••••••',
            obscure: _obscurePassword,
            onToggle: () =>
                setState(() => _obscurePassword = !_obscurePassword),
          ),
          const SizedBox(height: 16),
          // Confirm password
          _buildPasswordInput(
            label: 'Xác nhận mật khẩu',
            controller: _confirmPasswordCtrl,
            hint: '••••••••',
            obscure: _obscureConfirm,
            onToggle: () => setState(() => _obscureConfirm = !_obscureConfirm),
            hasError: _confirmPasswordError != null,
            onChanged: (_) => setState(() => _confirmPasswordError = null),
          ),
          if (_confirmPasswordError != null) ...[
            const SizedBox(height: 4),
            Row(
              children: [
                const Icon(Icons.error_outline, size: 16, color: _errorColor),
                const SizedBox(width: 4),
                Text(
                  _confirmPasswordError!,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    color: _errorColor,
                  ),
                ),
              ],
            ),
          ],
          const SizedBox(height: 16),
          // Password checklist
          _buildPasswordChecklist(),
          const SizedBox(height: 16),
          // Terms checkbox
          _buildTermsCheckbox(),
        ],
      ),
    );
  }

  Widget _buildLabeledInput({
    required String id,
    required String label,
    required TextEditingController controller,
    required String hint,
    TextInputType? keyboardType,
  }) {
    return Container(
      height: 64,
      decoration: BoxDecoration(
        color: _surfaceContainerLowest,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: _borderColor.withValues(alpha: 0.5)),
      ),
      child: Stack(
        children: [
          Positioned(
            left: 16,
            top: 8,
            child: Text(
              label,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 12,
                fontWeight: FontWeight.w500,
                color: _mutedColor,
                letterSpacing: 0.6,
              ),
            ),
          ),
          Positioned.fill(
            child: Padding(
              padding: const EdgeInsets.only(
                left: 16,
                right: 16,
                top: 28,
                bottom: 4,
              ),
              child: TextField(
                controller: controller,
                keyboardType: keyboardType,
                decoration: InputDecoration(
                  hintText: hint,
                  hintStyle: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 16,
                    color: _borderColor,
                  ),
                  border: InputBorder.none,
                  isDense: true,
                  contentPadding: EdgeInsets.zero,
                ),
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  color: _textColor,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPasswordInput({
    required String label,
    required TextEditingController controller,
    required String hint,
    required bool obscure,
    required VoidCallback onToggle,
    bool hasError = false,
    ValueChanged<String>? onChanged,
  }) {
    return Container(
      height: 64,
      decoration: BoxDecoration(
        color: _surfaceContainerLowest,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: hasError ? _errorColor : _borderColor.withValues(alpha: 0.5),
        ),
      ),
      child: Stack(
        children: [
          Positioned(
            left: 16,
            top: 8,
            child: Text(
              label,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 12,
                fontWeight: FontWeight.w500,
                color: hasError ? _errorColor : _mutedColor,
                letterSpacing: 0.6,
              ),
            ),
          ),
          Positioned(
            left: 16,
            right: 48,
            top: 28,
            bottom: 4,
            child: TextField(
              controller: controller,
              obscureText: obscure,
              onChanged: onChanged ?? (_) => setState(() {}),
              decoration: InputDecoration(
                hintText: hint,
                hintStyle: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  color: _borderColor,
                ),
                border: InputBorder.none,
                isDense: true,
                contentPadding: EdgeInsets.zero,
              ),
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 16,
                color: _textColor,
              ),
            ),
          ),
          Positioned(
            right: 0,
            top: 0,
            bottom: 0,
            child: IconButton(
              onPressed: onToggle,
              icon: Icon(
                obscure
                    ? Icons.visibility_off_outlined
                    : Icons.visibility_outlined,
                size: 20,
                color: _mutedColor,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildRoleSelector() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Vai trò của bạn',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 12,
            fontWeight: FontWeight.w500,
            color: _mutedColor,
            letterSpacing: 0.6,
          ),
        ),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          children: _roles.map((role) {
            final (value, label) = role;
            final isSelected = _selectedRole == value;
            return GestureDetector(
              onTap: () => setState(() => _selectedRole = value),
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 150),
                padding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 8,
                ),
                decoration: BoxDecoration(
                  color: isSelected ? _primaryColor : _surfaceContainerLowest,
                  borderRadius: BorderRadius.circular(999),
                  border: Border.all(
                    color: isSelected
                        ? _primaryColor
                        : _borderColor.withValues(alpha: 0.5),
                  ),
                ),
                child: Text(
                  label,
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    color: isSelected ? Colors.white : _mutedColor,
                  ),
                ),
              ),
            );
          }).toList(),
        ),
      ],
    );
  }

  Widget _buildPasswordChecklist() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF1EC),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: const Color(0xFFFFE2D9).withValues(alpha: 0.5),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Mật khẩu cần có:',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 12,
              fontWeight: FontWeight.w500,
              color: _mutedColor,
              letterSpacing: 0.6,
            ),
          ),
          const SizedBox(height: 8),
          _buildCheckItem('Ít nhất 8 ký tự', _hasMinLength),
          const SizedBox(height: 6),
          _buildCheckItem(
            'Chứa ký tự đặc biệt (@, #, \$, ...)',
            _hasSpecialChar,
          ),
        ],
      ),
    );
  }

  Widget _buildCheckItem(String text, bool met) {
    return Row(
      children: [
        Icon(
          met ? Icons.check_circle : Icons.check_circle_outline,
          size: 16,
          color: met ? _primaryColor : _mutedColor,
        ),
        const SizedBox(width: 8),
        Text(
          text,
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 13,
            fontWeight: met ? FontWeight.w500 : FontWeight.w400,
            color: met ? _accentPrimary : _mutedColor,
          ),
        ),
      ],
    );
  }

  Widget _buildTermsCheckbox() {
    return GestureDetector(
      onTap: () => setState(() => _termsAccepted = !_termsAccepted),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 20,
            height: 20,
            child: Checkbox(
              value: _termsAccepted,
              onChanged: (v) => setState(() => _termsAccepted = v ?? false),
              activeColor: _primaryColor,
              side: BorderSide(color: _borderColor, width: 2),
              materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: RichText(
              text: TextSpan(
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  color: _mutedColor,
                ),
                children: const [
                  TextSpan(text: 'Tôi đồng ý với '),
                  TextSpan(
                    text: 'Điều khoản',
                    style: TextStyle(
                      color: _accentPrimary,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  TextSpan(text: ' và '),
                  TextSpan(
                    text: 'Chính sách quyền riêng tư',
                    style: TextStyle(
                      color: _accentPrimary,
                      fontWeight: FontWeight.w500,
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

  Widget _buildStickyFooter(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(24, 16, 24, 34),
      decoration: BoxDecoration(
        color: _canvasColor,
        border: Border(
          top: BorderSide(color: _borderColor.withValues(alpha: 0.3)),
        ),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          SizedBox(
            height: 52,
            child: FilledButton(
              onPressed: _isLoading ? null : _submit,
              style: FilledButton.styleFrom(
                backgroundColor: _primaryColor,
                foregroundColor: Colors.white,
                shape: const StadiumBorder(),
                disabledBackgroundColor: _primaryColor.withValues(alpha: 0.6),
              ),
              child: _isLoading
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : const Text(
                      'Tạo tài khoản',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
            ),
          ),
          const SizedBox(height: 16),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                'Đã có tài khoản? ',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  color: _mutedColor,
                ),
              ),
              GestureDetector(
                onTap: () => Navigator.pushReplacement(
                  context,
                  MaterialPageRoute(builder: (_) => const LoginScreen()),
                ),
                child: const Text(
                  'Đăng nhập',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: _accentPrimary,
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
