import 'dart:async';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/auth/auth_state.dart';
import '../../../../core/network/api_client.dart';
import '../models/federated_auth_failure.dart';
import '../services/auth_service.dart';
import 'forgot_password_screen.dart';
import 'register_screen.dart';

/// CB-004 — Login (UC-03)
/// Collects email/phone + password, creates a session, then routes to the authenticated app.
class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key, this.onGoogleSignIn, this.authService});

  final Future<void> Function()? onGoogleSignIn;
  final AuthService? authService;

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  static const _primaryColor = Color(0xFFC98C7B);
  static const _canvasColor = Color(0xFFF6F1EC);
  static const _textColor = Color(0xFF5A463F);
  static const _mutedColor = Color(0xFF524440);
  static const _surfaceColor = Color(0xFFFFF8F6);
  static const _borderColor = Color(0xFFF2EAE4);
  static const _accentPrimary = Color(0xFF845143);

  final _identifierCtrl = TextEditingController();
  final _passwordCtrl = TextEditingController();
  bool _obscurePassword = true;
  bool _isLoading = false;
  String? _errorMessage;

  @override
  void dispose() {
    _identifierCtrl.dispose();
    _passwordCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_isLoading) return;
    final identifier = _identifierCtrl.text.trim();
    final password = _passwordCtrl.text;
    final isEmail = identifier.contains('@');

    if (identifier.isEmpty || password.isEmpty) {
      setState(() => _errorMessage = 'Vui lòng nhập đầy đủ thông tin.');
      return;
    }

    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      await (widget.authService ?? AuthService.instance).login(
        email: isEmail ? identifier : null,
        phone: isEmail ? null : identifier,
        password: password,
      );
      if (!mounted) return;
      _passwordCtrl.clear();
      context.go('/auth-landing');
    } on ApiException catch (e) {
      if (e.statusCode == 403 && AuthState.instance.blockedAccount != null) {
        if (mounted) context.go('/blocked');
        return;
      }
      String msg;
      if (e.statusCode == 401) {
        msg = 'Email/số điện thoại hoặc mật khẩu không đúng.';
      } else if (e.statusCode == 403) {
        msg = 'Tài khoản bị khóa. Liên hệ hỗ trợ để mở khóa.';
      } else if (e.statusCode == 429) {
        msg = 'Quá nhiều lần thử. Vui lòng đợi 15 phút.';
      } else if (e.statusCode == 404 || e.statusCode >= 500) {
        msg = 'Dịch vụ đăng nhập hiện không khả dụng. Vui lòng thử lại sau.';
      } else {
        msg = 'Đăng nhập thất bại. Vui lòng thử lại sau.';
      }
      if (!mounted) return;
      setState(() => _errorMessage = msg);
    } on FormatException {
      if (!mounted) return;
      setState(
        () => _errorMessage =
            'Phản hồi đăng nhập không hợp lệ. Vui lòng thử lại sau.',
      );
    } catch (_) {
      if (!mounted) return;
      setState(
        () => _errorMessage =
            'Không thể kết nối đến máy chủ. Kiểm tra kết nối mạng.',
      );
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _federatedGoogleLogin() async {
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
                    _buildIcon(),
                    const SizedBox(height: 32),
                    _buildHeading(),
                    const SizedBox(height: 32),
                    if (_errorMessage != null) ...[
                      _buildErrorBanner(_errorMessage!),
                      const SizedBox(height: 16),
                    ],
                    _buildForm(),
                    const SizedBox(height: 20),
                    _buildFederatedActions(),
                    const SizedBox(height: 32),
                    _buildInfoCard(),
                    const SizedBox(height: 32),
                  ],
                ),
              ),
            ),
            _buildFooter(context),
          ],
        ),
      ),
    );
  }

  Widget _buildAppBar(BuildContext context) {
    return SizedBox(
      height: 72,
      child: Row(
        children: [
          IconButton(
            onPressed: () => Navigator.pop(context),
            icon: const Icon(Icons.arrow_back),
            color: _textColor,
            iconSize: 28,
          ),
          Expanded(
            child: Text(
              'Đăng nhập',
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

  Widget _buildIcon() {
    return Center(
      child: Container(
        width: 96,
        height: 96,
        decoration: BoxDecoration(
          color: _surfaceColor,
          shape: BoxShape.circle,
          border: Border.all(color: _borderColor),
          boxShadow: [
            BoxShadow(
              color: _textColor.withValues(alpha: 0.06),
              blurRadius: 20,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: const Icon(
          Icons.volunteer_activism,
          size: 48,
          color: _accentPrimary,
        ),
      ),
    );
  }

  Widget _buildHeading() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'Chào mừng bạn trở lại',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 24,
            fontWeight: FontWeight.w600,
            color: _textColor,
          ),
        ),
        const SizedBox(height: 8),
        Text(
          'Đăng nhập để tiếp tục hành trình chăm sóc bé yêu.',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 14,
            fontWeight: FontWeight.w400,
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

  Widget _buildForm() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _buildFieldLabel('Email hoặc Số điện thoại'),
        const SizedBox(height: 8),
        _buildTextField(
          controller: _identifierCtrl,
          hint: 'Nhập email hoặc số điện thoại',
          keyboardType: TextInputType.emailAddress,
        ),
        const SizedBox(height: 20),
        _buildFieldLabel('Mật khẩu'),
        const SizedBox(height: 8),
        _buildTextField(
          controller: _passwordCtrl,
          hint: 'Nhập mật khẩu',
          obscureText: _obscurePassword,
          suffixIcon: IconButton(
            onPressed: () =>
                setState(() => _obscurePassword = !_obscurePassword),
            icon: Icon(
              _obscurePassword
                  ? Icons.visibility_off_outlined
                  : Icons.visibility_outlined,
              size: 20,
              color: const Color(0xFF9C857C),
            ),
          ),
        ),
        const SizedBox(height: 8),
        Align(
          alignment: Alignment.centerRight,
          child: TextButton(
            onPressed: () {
              Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => const ForgotPasswordScreen()),
              );
            },
            style: TextButton.styleFrom(
              padding: EdgeInsets.zero,
              minimumSize: const Size(0, 32),
            ),
            child: const Text(
              'Quên mật khẩu?',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 12,
                fontWeight: FontWeight.w500,
                color: _accentPrimary,
                letterSpacing: 0.6,
              ),
            ),
          ),
        ),
        const SizedBox(height: 24),
        SizedBox(
          height: 52,
          child: FilledButton(
            key: const Key('password-login-submit'),
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
                    'Đăng nhập',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
          ),
        ),
      ],
    );
  }

  Future<void> _federatedPhone() async {
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
      if (mounted) context.go('/auth-landing');
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

  Widget _buildFederatedActions() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        _buildFederatedIconButton(
          key: const Key('federated-google-login'),
          tooltip: 'Tiếp tục với Google',
          onPressed: _isLoading ? null : _federatedGoogleLogin,
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
          key: const Key('federated-phone-login'),
          tooltip: 'Tiếp tục với số điện thoại',
          onPressed: _isLoading ? null : _federatedPhone,
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

  Widget _buildInfoCard() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surfaceColor,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: _borderColor),
        boxShadow: [
          BoxShadow(
            color: _textColor.withValues(alpha: 0.06),
            blurRadius: 20,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: const Color(0xFFF6DACF).withValues(alpha: 0.3),
              shape: BoxShape.circle,
            ),
            child: const Icon(
              Icons.info_outline,
              size: 24,
              color: Color(0xFF6E5A52),
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Text(
              'Bạn sẽ được đưa tới không gian phù hợp sau khi đăng nhập.',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                fontWeight: FontWeight.w400,
                color: _mutedColor,
                height: 1.6,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFooter(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 8, 24, 24),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Text(
            'Chưa có tài khoản? ',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _mutedColor,
            ),
          ),
          GestureDetector(
            onTap: () => Navigator.pushReplacement(
              context,
              MaterialPageRoute(builder: (_) => const RegisterScreen()),
            ),
            child: const Text(
              'Tạo tài khoản',
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
    );
  }

  Widget _buildFieldLabel(String label) {
    return Text(
      label.toUpperCase(),
      style: const TextStyle(
        fontFamily: 'Lexend',
        fontSize: 12,
        fontWeight: FontWeight.w500,
        color: _mutedColor,
        letterSpacing: 0.6,
      ),
    );
  }

  Widget _buildTextField({
    required TextEditingController controller,
    required String hint,
    bool obscureText = false,
    Widget? suffixIcon,
    TextInputType? keyboardType,
  }) {
    return SizedBox(
      height: 56,
      child: TextField(
        controller: controller,
        obscureText: obscureText,
        keyboardType: keyboardType,
        style: const TextStyle(
          fontFamily: 'Lexend',
          fontSize: 16,
          color: _textColor,
        ),
        decoration: InputDecoration(
          hintText: hint,
          hintStyle: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 16,
            color: Color(0xFF9C857C),
          ),
          filled: true,
          fillColor: _surfaceColor,
          contentPadding: const EdgeInsets.symmetric(horizontal: 16),
          suffixIcon: suffixIcon,
          enabledBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(16),
            borderSide: const BorderSide(color: _borderColor),
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(16),
            borderSide: const BorderSide(color: _primaryColor, width: 1.5),
          ),
        ),
      ),
    );
  }
}
