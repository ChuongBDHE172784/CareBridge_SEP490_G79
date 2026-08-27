import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../../core/auth/auth_state.dart';
import '../../../../core/network/api_client.dart';
import '../models/federated_auth_failure.dart';
import '../services/auth_service.dart';
import '../widgets/auth_ui.dart';
import 'forgot_password_screen.dart';
import 'phone_verification_screen.dart';
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
  static const _mutedColor = AuthPalette.muted;
  static const _surfaceColor = AuthPalette.surface;
  static const _borderColor = AuthPalette.line;
  static const _accentPrimary = AuthPalette.accentDeep;

  final _emailCtrl = TextEditingController();
  final _passwordCtrl = TextEditingController();
  bool _obscurePassword = true;
  bool _isLoading = false;
  String? _errorMessage;

  @override
  void dispose() {
    _emailCtrl.dispose();
    _passwordCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_isLoading) return;
    var identifier = _emailCtrl.text.trim();
    final password = _passwordCtrl.text;

    if (identifier.isEmpty || password.isEmpty) {
      setState(() => _errorMessage = 'Vui lòng nhập đầy đủ thông tin.');
      return;
    }

    if (identifier.startsWith('0') && identifier.length == 10) {
      identifier = '+84${identifier.substring(1)}';
    }

    final isEmail = RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(identifier);
    final isPhone = RegExp(r'^\+84[35789]\d{8}$').hasMatch(identifier);

    if (!isEmail && !isPhone) {
      setState(
        () => _errorMessage = 'Vui lòng nhập địa chỉ email hoặc số điện thoại hợp lệ.',
      );
      return;
    }

    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      if (isEmail) {
        await (widget.authService ?? AuthService.instance).login(
          email: identifier,
          password: password,
        );
      } else {
        await (widget.authService ?? AuthService.instance).login(
          phone: identifier,
          password: password,
        );
      }
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

  void _openPhoneVerificationFromSmsButton() {
    final rawInput = _emailCtrl.text.trim();
    String phone = rawInput;

    if (phone.startsWith('0') && phone.length == 10) {
      phone = '+84${phone.substring(1)}';
    }

    if (!RegExp(r'^\+84[35789]\d{8}$').hasMatch(phone)) {
      setState(
        () => _errorMessage =
            'Vui lòng nhập số điện thoại hợp lệ vào ô bên trên để đăng nhập SMS (ví dụ: +84912345678 hoặc 0912345678).',
      );
      return;
    }

    setState(() => _errorMessage = null);
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => PhoneVerificationScreen.login(
          phoneNumber: phone,
          authService: widget.authService ?? AuthService.instance,
        ),
      ),
    );
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: AuthBackground(
        child: SafeArea(
          child: Column(
            children: [
              _buildAppBar(context),
              Expanded(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.fromLTRB(24, 8, 24, 28),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      _buildHeading(),
                      const SizedBox(height: 24),
                      if (_errorMessage != null) ...[
                        _buildErrorBanner(_errorMessage!),
                        const SizedBox(height: 16),
                      ],
                      AuthSurface(child: _buildForm()),
                      const SizedBox(height: 22),
                      _buildFederatedActions(),
                      const SizedBox(height: 24),
                      _buildInfoCard(),
                    ],
                  ),
                ),
              ),
              _buildFooter(context),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildAppBar(BuildContext context) {
    return AuthTopBar(title: 'Đăng nhập', onBack: () => Navigator.pop(context));
  }

  Widget _buildHeading() {
    return const AuthIntro(
      eyebrow: 'CHÀO MỪNG BẠN TRỞ LẠI',
      title: 'Tiếp tục hành trình chăm sóc.',
      subtitle: 'Đăng nhập để xem những điều đang cần bạn quan tâm hôm nay.',
    );
  }

  Widget _buildErrorBanner(String message) {
    return AuthErrorBanner(message: message);
  }

  Widget _buildForm() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        AuthTextField(
          key: const Key('login-email-field'),
          controller: _emailCtrl,
          label: 'Email hoặc Số điện thoại',
          hint: 'Nhập email hoặc số điện thoại',
          keyboardType: TextInputType.emailAddress,
          textInputAction: TextInputAction.next,
        ),
        const SizedBox(height: 18),
        AuthTextField(
          controller: _passwordCtrl,
          label: 'Mật khẩu',
          hint: 'Nhập mật khẩu',
          obscureText: _obscurePassword,
          suffixIcon: IconButton(
            tooltip: _obscurePassword ? 'Hiện mật khẩu' : 'Ẩn mật khẩu',
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
        const SizedBox(height: 4),
        Align(
          alignment: Alignment.centerRight,
          child: TextButton(
            onPressed: () {
              Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (_) => const ForgotPasswordScreen(),
                ),
              );
            },
            style: TextButton.styleFrom(
              minimumSize: const Size(0, 44),
              padding: const EdgeInsets.symmetric(horizontal: 4),
            ),
            child: const Text(
              'Quên mật khẩu?',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 12,
                fontWeight: FontWeight.w700,
                color: _accentPrimary,
              ),
            ),
          ),
        ),
        const SizedBox(height: 16),
        AuthPrimaryButton(
          buttonKey: const Key('password-login-submit'),
          label: 'Đăng nhập',
          onPressed: _submit,
          isLoading: _isLoading,
        ),
      ],
    );
  }

  Widget _buildFederatedActions() {
    return Column(
      children: [
        const AuthDivider(),
        const SizedBox(height: 14),
        Row(
          children: [
            Expanded(
              child: _buildFederatedIconButton(
                key: const Key('federated-google-login'),
                tooltip: 'Tiếp tục với Google',
                onPressed: _isLoading ? null : _federatedGoogleLogin,
                child: const Text(
                  'G',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 22,
                    fontWeight: FontWeight.w800,
                    color: _accentPrimary,
                  ),
                ),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: _buildFederatedIconButton(
                key: const Key('federated-phone-login'),
                tooltip: 'Đăng nhập SMS',
                onPressed: _isLoading ? null : _openPhoneVerificationFromSmsButton,
                child: const Icon(
                  Icons.sms_outlined,
                  size: 22,
                  color: _accentPrimary,
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
        color: _surfaceColor,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(14),
          side: const BorderSide(color: _borderColor),
        ),
        child: IconButton(
          key: key,
          tooltip: tooltip,
          onPressed: onPressed,
          style: IconButton.styleFrom(
            foregroundColor: _accentPrimary,
            disabledForegroundColor: _mutedColor.withValues(alpha: 0.4),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(14),
            ),
          ),
          icon: child,
        ),
      ),
    );
  }

  Widget _buildInfoCard() {
    return const AuthInfoCard(
      message: 'Bạn sẽ được đưa tới không gian phù hợp sau khi đăng nhập.',
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
          TextButton(
            onPressed: () => Navigator.pushReplacement(
              context,
              MaterialPageRoute(builder: (_) => const RegisterScreen()),
            ),
            style: TextButton.styleFrom(
              minimumSize: const Size(44, 44),
              padding: const EdgeInsets.symmetric(horizontal: 6),
              foregroundColor: _accentPrimary,
              textStyle: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                fontWeight: FontWeight.w700,
              ),
            ),
            child: const Text('Tạo tài khoản'),
          ),
        ],
      ),
    );
  }
}
