import 'package:flutter/material.dart';
import '../../../../core/network/api_client.dart';
import '../services/auth_service.dart';
import 'otp_verification_screen.dart';
import 'register_screen.dart';

/// CB-004 — Login (UC-03)
/// Collects email/phone + password, calls POST /api/v1/auth/login → navigates to OTP screen.
class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

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

  bool get _isEmailInput => _identifierCtrl.text.contains('@');

  Future<void> _submit() async {
    final identifier = _identifierCtrl.text.trim();
    final password = _passwordCtrl.text;

    if (identifier.isEmpty || password.isEmpty) {
      setState(() => _errorMessage = 'Vui lòng nhập đầy đủ thông tin.');
      return;
    }

    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      await AuthService.instance.login(
        email: _isEmailInput ? identifier : null,
        phone: !_isEmailInput ? identifier : null,
        password: password,
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
      if (e.statusCode == 403) {
        msg = 'Tài khoản bị khóa. Liên hệ hỗ trợ để mở khóa.';
      } else if (e.statusCode == 429) {
        msg = 'Quá nhiều lần thử. Vui lòng đợi 15 phút.';
      } else {
        msg = 'Email/số điện thoại hoặc mật khẩu không đúng.';
      }
      setState(() => _errorMessage = msg);
    } catch (_) {
      setState(() => _errorMessage = 'Không thể kết nối đến máy chủ. Kiểm tra kết nối mạng.');
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
              color: _textColor.withOpacity(0.06),
              blurRadius: 20,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: const Icon(Icons.volunteer_activism, size: 48, color: _accentPrimary),
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
            onPressed: () => setState(() => _obscurePassword = !_obscurePassword),
            icon: Icon(
              _obscurePassword ? Icons.visibility_off_outlined : Icons.visibility_outlined,
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
              // TODO: navigate to ForgotPasswordScreen (CB-005)
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
            onPressed: _isLoading ? null : _submit,
            style: FilledButton.styleFrom(
              backgroundColor: _primaryColor,
              foregroundColor: Colors.white,
              shape: const StadiumBorder(),
              disabledBackgroundColor: _primaryColor.withOpacity(0.6),
            ),
            child: _isLoading
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
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

  Widget _buildInfoCard() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surfaceColor,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: _borderColor),
        boxShadow: [
          BoxShadow(
            color: _textColor.withOpacity(0.06),
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
              color: const Color(0xFFF6DACF).withOpacity(0.3),
              shape: BoxShape.circle,
            ),
            child: const Icon(Icons.info_outline, size: 24, color: Color(0xFF6E5A52)),
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
