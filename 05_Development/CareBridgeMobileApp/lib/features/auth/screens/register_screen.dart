import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/network/api_client.dart';
import '../models/federated_auth_failure.dart';
import '../services/auth_service.dart';
import 'otp_verification_screen.dart';
import 'login_screen.dart';
import 'role_selection_screen.dart';

/// CB-002 — Register Account (UC-01)
/// Collects name, email/phone, password → calls POST /api/v1/auth/register → navigates to OTP screen.
class RegisterScreen extends StatefulWidget {
  final bool isExpert;
  final Future<void> Function()? onGoogleSignIn;
  final AuthService? authService;

  const RegisterScreen({
    super.key,
    this.isExpert = false,
    this.onGoogleSignIn,
    this.authService,
  });

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

  bool _obscurePassword = true;
  bool _obscureConfirm = true;
  bool _termsAccepted = false;
  bool _isLoading = false;
  String? _errorMessage;
  String? _confirmPasswordError;

  @override
  void dispose() {
    _nameCtrl.dispose();
    _identifierCtrl.dispose();
    _passwordCtrl.dispose();
    _confirmPasswordCtrl.dispose();
    super.dispose();
  }

  bool get _isEmailInput => _identifierCtrl.text.contains('@');

  bool get _hasValidEmail => RegExp(
    r'^[^@\s]+@[^@\s]+\.[^@\s]+$',
  ).hasMatch(_identifierCtrl.text.trim());

  bool get _hasMinLength => _passwordCtrl.text.length >= 8;
  bool get _hasUppercase => RegExp(r'[A-Z]').hasMatch(_passwordCtrl.text);
  bool get _hasSpecialChar =>
      RegExp(r'[@#$%^&*!]').hasMatch(_passwordCtrl.text);

  Future<void> _submit() async {
    final name = _nameCtrl.text.trim();
    final identifier = _identifierCtrl.text.trim();
    final password = _passwordCtrl.text;
    final confirmPassword = _confirmPasswordCtrl.text;

    if (name.isEmpty || identifier.isEmpty || password.isEmpty) {
      setState(() => _errorMessage = 'Vui lòng nhập đầy đủ thông tin.');
      return;
    }
    if (widget.isExpert && !_hasValidEmail) {
      setState(
        () => _errorMessage =
            'Tài khoản chuyên gia cần đăng ký bằng địa chỉ email hợp lệ.',
      );
      return;
    }
    if (name.length < 2 || name.length > 120) {
      setState(() => _errorMessage = 'Họ và tên phải có từ 2 đến 120 ký tự.');
      return;
    }
    if (password != confirmPassword) {
      setState(() => _confirmPasswordError = 'Mật khẩu không khớp');
      return;
    }
    if (!_hasMinLength || !_hasSpecialChar || !_hasUppercase) {
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
      await (widget.authService ?? AuthService.instance).register(
        name: name,
        email: _isEmailInput ? identifier : null,
        phone: !_isEmailInput ? identifier : null,
        password: password,
        role: widget.isExpert ? 'EXPERT' : null,
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
      if (e.errorCode == 'AUTH_ACCOUNT_EXISTS' || e.statusCode == 409) {
        msg = widget.isExpert
            ? 'Email này đã được đăng ký.'
            : 'Tài khoản đã tồn tại';
      } else if (e.statusCode == 400) {
        msg = 'Thông tin không hợp lệ. Vui lòng kiểm tra lại.';
      } else {
        msg = 'Đăng ký thất bại. Vui lòng thử lại sau.';
      }
      if (!mounted) return;
      setState(() => _errorMessage = msg);
    } catch (_) {
      if (!mounted) return;
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
                    if (!widget.isExpert) ...[
                      const SizedBox(height: 20),
                      _buildFederatedRegistrationActions(),
                    ],
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
              widget.isExpert ? 'Đăng ký Chuyên gia' : 'Tạo tài khoản',
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
        Text(
          widget.isExpert ? 'Đăng ký Chuyên gia Y tế 🩺' : 'Chào mừng bạn! 👋',
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 24,
            fontWeight: FontWeight.w600,
            color: _accentPrimary,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          widget.isExpert
              ? 'Tạo tài khoản chuyên gia để bắt đầu xác thực hồ sơ chuyên môn.'
              : 'Bắt đầu hành trình chăm sóc tuyệt vời cùng CareBridge.',
          style: const TextStyle(
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
            label: widget.isExpert ? 'Email' : 'Email hoặc Số điện thoại',
            controller: _identifierCtrl,
            hint: widget.isExpert ? 'bacsi@example.com' : '09xx xxx xxx',
            keyboardType: TextInputType.emailAddress,
          ),
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

  Future<void> _federatedPhoneRegistration() async {
    final phone = _identifierCtrl.text.trim();
    if (phone.isEmpty) {
      setState(
        () =>
            _errorMessage = 'Enter a phone number including the country code.',
      );
      return;
    }
    setState(() => _isLoading = true);
    try {
      final verificationId = await AuthService.instance.beginPhoneVerification(
        phone,
      );
      if (!mounted) return;
      final code = await _requestSmsCode();
      if (code == null || code.isEmpty) return;
      await AuthService.instance.confirmPhoneVerification(verificationId, code);
      if (mounted) {
        Navigator.of(context).pushReplacement(
          MaterialPageRoute(builder: (_) => const RoleSelectionScreen()),
        );
      }
    } catch (_) {
      if (mounted) {
        setState(() => _errorMessage = 'Unable to verify this phone number.');
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<String?> _requestSmsCode() async {
    final controller = TextEditingController();
    final value = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Enter SMS code'),
        content: TextField(
          controller: controller,
          keyboardType: TextInputType.number,
          autofocus: true,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, controller.text.trim()),
            child: const Text('Verify'),
          ),
        ],
      ),
    );
    controller.dispose();
    return value;
  }

  Future<void> _federatedGoogleRegistration() async {
    if (_isLoading) return;
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });
    try {
      final callback = widget.onGoogleSignIn;
      if (callback != null) {
        await callback();
      } else {
        await AuthService.instance.federatedGoogle();
      }
      if (mounted) context.go('/auth-landing');
    } on FederatedSignInException catch (error) {
      if (mounted && !error.failure.isCanceled) {
        setState(() => _errorMessage = error.failure.userMessage);
      }
    } catch (error) {
      final failure = FederatedAuthFailure.from(error);
      if (mounted && !failure.isCanceled) {
        setState(() => _errorMessage = failure.userMessage);
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Widget _buildFederatedRegistrationActions() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        _buildFederatedIconButton(
          key: const Key('federated-google-register'),
          tooltip: 'Đăng ký với Google',
          onPressed: _isLoading ? null : _federatedGoogleRegistration,
          child: const Text(
            'G',
            style: TextStyle(
              fontSize: 22,
              fontWeight: FontWeight.w700,
              color: _accentPrimary,
            ),
          ),
        ),
        const SizedBox(width: 16),
        _buildFederatedIconButton(
          key: const Key('federated-phone-register'),
          tooltip: 'Đăng ký với số điện thoại',
          onPressed: _isLoading ? null : _federatedPhoneRegistration,
          child: const Icon(Icons.phone_rounded, size: 22),
        ),
      ],
    );
  }

  Widget _buildFederatedIconButton({
    required Key key,
    required String tooltip,
    required VoidCallback? onPressed,
    required Widget child,
  }) {
    return Material(
      color: _surfaceColor,
      elevation: onPressed == null ? 0 : 2,
      shadowColor: _textColor.withValues(alpha: 0.12),
      shape: const CircleBorder(),
      child: IconButton(
        key: key,
        tooltip: tooltip,
        onPressed: onPressed,
        style: IconButton.styleFrom(
          fixedSize: const Size.square(48),
          foregroundColor: _accentPrimary,
          disabledForegroundColor: _mutedColor.withValues(alpha: 0.4),
          side: BorderSide(color: _borderColor),
          shape: const CircleBorder(),
        ),
        icon: child,
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
          _buildCheckItem('Chứa chữ cái viết hoa', _hasUppercase),
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
