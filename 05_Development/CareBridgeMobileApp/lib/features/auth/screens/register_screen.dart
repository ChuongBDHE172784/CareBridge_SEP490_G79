import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../models/federated_auth_failure.dart';
import '../models/registration_draft.dart';
import '../services/auth_service.dart';
import '../widgets/auth_ui.dart';
import 'login_screen.dart';
import 'phone_verification_screen.dart';
import 'registration_verification_method_screen.dart';

/// CB-002 — Register Account (UC-01)
/// Collects both contacts and account details before the user chooses Email or
/// Firebase SMS verification on the next screen.
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
  final _nameCtrl = TextEditingController();
  final _emailCtrl = TextEditingController();
  final _phoneCtrl = TextEditingController();
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
    _emailCtrl.dispose();
    _phoneCtrl.dispose();
    _passwordCtrl.dispose();
    _confirmPasswordCtrl.dispose();
    super.dispose();
  }

  bool get _hasValidEmail =>
      RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(_emailCtrl.text.trim());

  bool get _hasValidPhone {
    String phone = _phoneCtrl.text.trim();
    if (phone.startsWith('0') && phone.length == 10) {
      phone = '+84${phone.substring(1)}';
    }
    return RegExp(r'^\+84[35789]\d{8}$').hasMatch(phone);
  }

  bool get _hasMinLength => _passwordCtrl.text.length >= 8;
  bool get _hasUppercase => RegExp(r'[A-Z]').hasMatch(_passwordCtrl.text);
  bool get _hasLowercase => RegExp(r'[a-z]').hasMatch(_passwordCtrl.text);
  bool get _hasDigit => RegExp(r'\d').hasMatch(_passwordCtrl.text);
  bool get _hasSpecialChar =>
      RegExp(r'[@#$%^&*!]').hasMatch(_passwordCtrl.text);

  Future<void> _submit() async {
    if (_isLoading) return;

    final name = _nameCtrl.text.trim();
    final email = _emailCtrl.text.trim();
    var phone = _phoneCtrl.text.trim();
    if (phone.startsWith('0') && phone.length == 10) {
      phone = '+84${phone.substring(1)}';
    }
    final password = _passwordCtrl.text;
    final confirmPassword = _confirmPasswordCtrl.text;

    if (name.isEmpty ||
        email.isEmpty ||
        phone.isEmpty ||
        password.isEmpty ||
        confirmPassword.isEmpty) {
      setState(() => _errorMessage = 'Vui lòng nhập đầy đủ thông tin.');
      return;
    }
    if (!_hasValidEmail) {
      setState(() => _errorMessage = 'Email không đúng định dạng.');
      return;
    }
    if (!_hasValidPhone) {
      setState(
        () => _errorMessage =
            'Số điện thoại cần là số di động Việt Nam (ví dụ: 0912345678 hoặc +84912345678).',
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
    if (!_hasMinLength ||
        !_hasSpecialChar ||
        !_hasUppercase ||
        !_hasLowercase ||
        !_hasDigit) {
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

    final draft = RegistrationDraft(
      name: name,
      email: email,
      phone: phone,
      password: password,
      role: widget.isExpert ? 'EXPERT' : null,
    );
    RegistrationDraftStore.set(draft);
    try {
      await Navigator.of(context).push(
        MaterialPageRoute<void>(
          builder: (_) => RegistrationVerificationMethodScreen(
            authService: widget.authService ?? AuthService.instance,
          ),
        ),
      );
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: AuthBackground(
        child: SafeArea(
          child: Column(
            children: [
              AuthTopBar(
                title: widget.isExpert ? 'Đăng ký Chuyên gia' : 'Tạo tài khoản',
                onBack: () => Navigator.pop(context),
              ),
              Expanded(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.fromLTRB(24, 8, 24, 28),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      _buildHeading(),
                      const SizedBox(height: 22),
                      if (_errorMessage != null) ...[
                        AuthErrorBanner(message: _errorMessage!),
                        const SizedBox(height: 16),
                      ],
                      _buildFormCard(),
                      if (!widget.isExpert) ...[
                        const SizedBox(height: 24),
                        _buildFederatedRegistrationActions(),
                      ],
                      const SizedBox(height: 20),
                      AuthInfoCard(
                        message: widget.isExpert
                            ? 'Hồ sơ chuyên gia sẽ được xác thực trước khi bạn bắt đầu tư vấn trên CareBridge.'
                            : 'Bạn có thể cập nhật hồ sơ và cài đặt chăm sóc riêng tư sau khi tạo tài khoản.',
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
      bottomNavigationBar: _buildStickyFooter(context),
    );
  }

  Widget _buildHeading() {
    return AuthIntro(
      eyebrow: widget.isExpert
          ? 'XÁC MINH CHUYÊN MÔN'
          : 'BẮT ĐẦU VỚI CAREBRIDGE',
      title: widget.isExpert
          ? 'Đăng ký Chuyên gia Y tế'
          : 'Tạo không gian chăm sóc của bạn.',
      subtitle: widget.isExpert
          ? 'Tạo tài khoản chuyên gia để bắt đầu xác thực hồ sơ chuyên môn.'
          : 'Bắt đầu hành trình chăm sóc rõ ràng hơn cùng CareBridge.',
    );
  }

  Widget _buildFormCard() {
    return AuthSurface(
      padding: const EdgeInsets.fromLTRB(18, 20, 18, 18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            widget.isExpert ? 'Hồ sơ chuyên gia' : 'Thông tin của bạn',
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 15,
              fontWeight: FontWeight.w700,
              color: AuthPalette.ink,
            ),
          ),
          const SizedBox(height: 18),
          AuthTextField(
            controller: _nameCtrl,
            label: 'Họ và tên',
            hint: 'Nguyễn Thùy Linh',
            keyboardType: TextInputType.name,
            textInputAction: TextInputAction.next,
          ),
          const SizedBox(height: 18),
          AuthTextField(
            key: const Key('register-email-field'),
            controller: _emailCtrl,
            label: 'Email',
            hint: widget.isExpert ? 'bacsi@example.com' : 'ban@example.com',
            keyboardType: TextInputType.emailAddress,
            textInputAction: TextInputAction.next,
          ),
          const SizedBox(height: 18),
          AuthTextField(
            key: const Key('register-phone-field'),
            controller: _phoneCtrl,
            label: 'Số điện thoại',
            hint: '+84912345678',
            keyboardType: TextInputType.phone,
            textInputAction: TextInputAction.next,
          ),
          const SizedBox(height: 18),
          AuthTextField(
            controller: _passwordCtrl,
            label: 'Mật khẩu',
            hint: 'Tạo mật khẩu bảo mật',
            obscureText: _obscurePassword,
            textInputAction: TextInputAction.next,
            onChanged: (_) => setState(() {}),
            suffixIcon: IconButton(
              tooltip: _obscurePassword ? 'Hiện mật khẩu' : 'Ẩn mật khẩu',
              onPressed: () =>
                  setState(() => _obscurePassword = !_obscurePassword),
              icon: Icon(
                _obscurePassword
                    ? Icons.visibility_off_outlined
                    : Icons.visibility_outlined,
                size: 20,
                color: AuthPalette.muted,
              ),
            ),
          ),
          const SizedBox(height: 18),
          AuthTextField(
            controller: _confirmPasswordCtrl,
            label: 'Xác nhận mật khẩu',
            hint: 'Nhập lại mật khẩu',
            obscureText: _obscureConfirm,
            textInputAction: TextInputAction.done,
            errorText: _confirmPasswordError,
            onChanged: (_) {
              if (_confirmPasswordError != null) {
                setState(() => _confirmPasswordError = null);
              }
            },
            suffixIcon: IconButton(
              tooltip: _obscureConfirm
                  ? 'Hiện mật khẩu xác nhận'
                  : 'Ẩn mật khẩu xác nhận',
              onPressed: () =>
                  setState(() => _obscureConfirm = !_obscureConfirm),
              icon: Icon(
                _obscureConfirm
                    ? Icons.visibility_off_outlined
                    : Icons.visibility_outlined,
                size: 20,
                color: AuthPalette.muted,
              ),
            ),
          ),
          const SizedBox(height: 18),
          _buildPasswordChecklist(),
          const SizedBox(height: 16),
          _buildTermsCheckbox(),
        ],
      ),
    );
  }

  Widget _buildPasswordChecklist() {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AuthPalette.surfaceSoft,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: AuthPalette.line),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Mật khẩu cần có',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 12,
              fontWeight: FontWeight.w700,
              color: AuthPalette.mutedStrong,
            ),
          ),
          const SizedBox(height: 10),
          _buildCheckItem('Ít nhất 8 ký tự', _hasMinLength),
          const SizedBox(height: 7),
          _buildCheckItem('Chứa chữ cái viết hoa', _hasUppercase),
          const SizedBox(height: 7),
          _buildCheckItem('Chứa chữ cái viết thường', _hasLowercase),
          const SizedBox(height: 7),
          _buildCheckItem('Chứa ít nhất một chữ số', _hasDigit),
          const SizedBox(height: 7),
          _buildCheckItem(
            'Chứa ký tự đặc biệt (@, #, ký hiệu, ...)',
            _hasSpecialChar,
          ),
        ],
      ),
    );
  }

  Widget _buildCheckItem(String text, bool met) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(top: 1),
          child: Icon(
            met ? Icons.check_circle_rounded : Icons.circle_outlined,
            size: 16,
            color: met ? AuthPalette.accentDeep : AuthPalette.muted,
          ),
        ),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            text,
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 12,
              height: 1.35,
              fontWeight: met ? FontWeight.w600 : FontWeight.w400,
              color: met ? AuthPalette.accentDeep : AuthPalette.mutedStrong,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildTermsCheckbox() {
    return Semantics(
      container: true,
      checked: _termsAccepted,
      label: 'Tôi đồng ý với Điều khoản và Chính sách quyền riêng tư',
      child: InkWell(
        onTap: () => setState(() => _termsAccepted = !_termsAccepted),
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 4),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SizedBox(
                width: 48,
                height: 32,
                child: Checkbox(
                  value: _termsAccepted,
                  onChanged: (value) =>
                      setState(() => _termsAccepted = value ?? false),
                  activeColor: AuthPalette.accentDeep,
                  side: const BorderSide(color: AuthPalette.muted, width: 1.5),
                  materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                ),
              ),
              const SizedBox(width: 4),
              const Expanded(
                child: Padding(
                  padding: EdgeInsets.only(top: 4),
                  child: Text.rich(
                    TextSpan(
                      text: 'Tôi đồng ý với ',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        height: 1.45,
                        color: AuthPalette.mutedStrong,
                      ),
                      children: [
                        TextSpan(
                          text: 'Điều khoản',
                          style: TextStyle(
                            fontWeight: FontWeight.w700,
                            color: AuthPalette.accentDeep,
                          ),
                        ),
                        TextSpan(text: ' và '),
                        TextSpan(
                          text: 'Chính sách quyền riêng tư',
                          style: TextStyle(
                            fontWeight: FontWeight.w700,
                            color: AuthPalette.accentDeep,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _openPhoneVerificationFromSmsButton() {
    final name = _nameCtrl.text.trim();
    final email = _emailCtrl.text.trim();
    final rawPhone = _phoneCtrl.text.trim();
    final password = _passwordCtrl.text;

    String phone = rawPhone;
    if (phone.startsWith('0') && phone.length == 10) {
      phone = '+84${phone.substring(1)}';
    }

    if (!RegExp(r'^\+84[35789]\d{8}$').hasMatch(phone)) {
      setState(
        () => _errorMessage =
            'Vui lòng nhập số điện thoại hợp lệ vào ô bên trên để đăng ký SMS (ví dụ: +84912345678 hoặc 0912345678).',
      );
      return;
    }

    final draft = RegistrationDraft(
      name: name.isNotEmpty ? name : 'Người dùng CareBridge',
      email: email,
      phone: phone,
      password: password.isNotEmpty ? password : 'CareBridge@123',
      role: widget.isExpert ? 'EXPERT' : null,
    );
    RegistrationDraftStore.set(draft);

    setState(() => _errorMessage = null);
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => PhoneVerificationScreen.registration(
          phoneNumber: phone,
          authService: widget.authService ?? AuthService.instance,
        ),
      ),
    );
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
        await (widget.authService ?? AuthService.instance).federatedGoogle();
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
    return Column(
      children: [
        const AuthDivider(label: 'hoặc đăng ký nhanh'),
        const SizedBox(height: 14),
        Row(
          children: [
            Expanded(
              child: _buildFederatedIconButton(
                key: const Key('federated-google-register'),
                tooltip: 'Đăng ký với Google',
                onPressed: _isLoading ? null : _federatedGoogleRegistration,
                child: const Text(
                  'G',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 22,
                    fontWeight: FontWeight.w800,
                    color: AuthPalette.accentDeep,
                  ),
                ),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: _buildFederatedIconButton(
                key: const Key('federated-phone-register'),
                tooltip: 'Đăng ký bằng SMS',
                onPressed: _isLoading ? null : _openPhoneVerificationFromSmsButton,
                child: const Icon(
                  Icons.sms_outlined,
                  size: 22,
                  color: AuthPalette.accentDeep,
                ),
              ),
            ),
          ],
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
    return SizedBox(
      height: 50,
      child: Material(
        color: AuthPalette.surface,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(14),
          side: const BorderSide(color: AuthPalette.line),
        ),
        child: IconButton(
          key: key,
          tooltip: tooltip,
          onPressed: onPressed,
          style: IconButton.styleFrom(
            foregroundColor: AuthPalette.accentDeep,
            disabledForegroundColor: AuthPalette.muted.withValues(alpha: 0.45),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(14),
            ),
          ),
          icon: child,
        ),
      ),
    );
  }

  Widget _buildStickyFooter(BuildContext context) {
    return SafeArea(
      top: false,
      child: Container(
        padding: const EdgeInsets.fromLTRB(24, 14, 24, 10),
        decoration: BoxDecoration(
          color: AuthPalette.canvas,
          border: Border(
            top: BorderSide(color: AuthPalette.line.withValues(alpha: 0.8)),
          ),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            AuthPrimaryButton(
              onPressed: _submit,
              label: 'Tạo tài khoản',
              isLoading: _isLoading,
            ),
            const SizedBox(height: 4),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Text(
                  'Đã có tài khoản?',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    color: AuthPalette.muted,
                  ),
                ),
                TextButton(
                  onPressed: () => Navigator.pushReplacement(
                    context,
                    MaterialPageRoute(builder: (_) => const LoginScreen()),
                  ),
                  style: TextButton.styleFrom(
                    minimumSize: const Size(44, 44),
                    padding: const EdgeInsets.symmetric(horizontal: 6),
                    foregroundColor: AuthPalette.accentDeep,
                    textStyle: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  child: const Text('Đăng nhập'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
