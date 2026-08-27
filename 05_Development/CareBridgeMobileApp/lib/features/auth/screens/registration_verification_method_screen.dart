import 'package:flutter/material.dart';

import '../../../../core/network/api_client.dart';
import '../models/registration_draft.dart';
import '../services/auth_service.dart';
import '../widgets/auth_ui.dart';
import 'otp_verification_screen.dart';
import 'phone_verification_screen.dart';

class RegistrationVerificationMethodScreen extends StatefulWidget {
  const RegistrationVerificationMethodScreen({
    super.key,
    this.draft,
    this.authService,
  });

  /// Optional test seam. Production navigation reads the draft from the
  /// in-memory store so the password is not carried in route arguments.
  final RegistrationDraft? draft;
  final AuthService? authService;

  @override
  State<RegistrationVerificationMethodScreen> createState() =>
      _RegistrationVerificationMethodScreenState();
}

class _RegistrationVerificationMethodScreenState
    extends State<RegistrationVerificationMethodScreen> {
  AuthVerificationMethod _method = AuthVerificationMethod.email;
  bool _isStarting = false;
  bool _challengeLocked = false;
  String? _errorMessage;

  AuthService get _service => widget.authService ?? AuthService.instance;

  RegistrationDraft? get _draft => widget.draft ?? RegistrationDraftStore.active;

  Future<void> _continue() async {
    if (_isStarting || _challengeLocked) return;
    setState(() {
      _isStarting = true;
      _challengeLocked = true;
      _errorMessage = null;
    });

    final draft = _draft;
    if (draft == null) {
      if (mounted) {
        setState(() {
          _isStarting = false;
          _challengeLocked = false;
          _errorMessage = 'Phiên đăng ký đã hết hạn. Vui lòng nhập lại thông tin.';
        });
      }
      return;
    }
    if (_method == AuthVerificationMethod.phone) {
      if (!mounted) return;
      await Navigator.of(context).pushReplacement(
        MaterialPageRoute<void>(
          builder: (_) => PhoneVerificationScreen.registration(
            phoneNumber: draft.phone,
            authService: _service,
          ),
        ),
      );
      return;
    }

    try {
      await _service.register(
        name: draft.name,
        email: draft.email,
        phone: draft.phone,
        password: draft.password,
        role: draft.role,
        verificationMethod: AuthVerificationMethod.email,
      );
      RegistrationDraftStore.clear();
      if (!mounted) return;
      await Navigator.of(context).pushReplacement(
        MaterialPageRoute<void>(
          builder: (_) => OtpVerificationScreen(
            identifier: draft.email,
            isEmail: true,
            authService: _service,
          ),
        ),
      );
    } on ApiException catch (error) {
      if (!mounted) return;
      setState(() {
        _isStarting = false;
        _challengeLocked = false;
        _errorMessage = _registrationError(error);
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _isStarting = false;
        _challengeLocked = false;
        _errorMessage =
            'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.';
      });
    }
  }

  String _registrationError(ApiException error) {
    if (error.errorCode == 'AUTH_ACCOUNT_EXISTS' || error.statusCode == 409) {
      return (_draft?.isExpert ?? false)
          ? 'Email này đã được đăng ký.'
          : 'Tài khoản đã tồn tại';
    }
    if (error.statusCode == 400) {
      return 'Thông tin không hợp lệ. Vui lòng kiểm tra lại.';
    }
    return 'Đăng ký thất bại. Vui lòng thử lại sau.';
  }

  String get _maskedEmail {
    final email = _draft?.email ?? '';
    final separator = email.indexOf('@');
    if (separator <= 1) return email;
    return '${email[0]}***${email.substring(separator)}';
  }

  String get _maskedPhone {
    final phone = _draft?.phone ?? '';
    if (phone.length <= 6) return phone;
    return '${phone.substring(0, 3)}***${phone.substring(phone.length - 3)}';
  }

  @override
  Widget build(BuildContext context) {
    final draft = _draft;
    if (draft == null) {
      return Scaffold(
        body: Center(
          child: AuthErrorBanner(
            message: 'Phiên đăng ký đã hết hạn. Vui lòng quay lại và nhập lại thông tin.',
          ),
        ),
      );
    }
    final expert = draft.isExpert;
    return PopScope(
      canPop: !_isStarting,
      child: Scaffold(
        body: AuthBackground(
          child: SafeArea(
            child: Column(
              children: [
                AuthTopBar(
                  title: 'Xác thực tài khoản',
                  onBack: () {
                    if (!_isStarting) {
                      RegistrationDraftStore.clear();
                      Navigator.pop(context);
                    }
                  },
                ),
                Expanded(
                  child: SingleChildScrollView(
                    padding: const EdgeInsets.fromLTRB(24, 18, 24, 28),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        const AuthIntro(
                          eyebrow: 'BƯỚC CUỐI CÙNG',
                          title: 'Bạn muốn nhận mã bằng cách nào?',
                          subtitle:
                              'Chọn một phương thức để xác thực thông tin đăng ký của bạn.',
                        ),
                        const SizedBox(height: 22),
                        if (_errorMessage != null) ...[
                          Semantics(
                            liveRegion: true,
                            child: AuthErrorBanner(message: _errorMessage!),
                          ),
                          const SizedBox(height: 16),
                        ],
                        AuthSurface(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.stretch,
                            children: [
                              Semantics(
                                container: true,
                                label: 'Chọn phương thức nhận mã xác thực',
                                child: SegmentedButton<AuthVerificationMethod>(
                                  key: const Key(
                                    'registration-verification-method',
                                  ),
                                  segments: [
                                    const ButtonSegment(
                                      value: AuthVerificationMethod.email,
                                      icon: Icon(Icons.mail_outline_rounded),
                                      label: Text('Email'),
                                    ),
                                    ButtonSegment(
                                      value: AuthVerificationMethod.phone,
                                      enabled: !expert,
                                      icon: const Icon(Icons.sms_outlined),
                                      label: const Text('SMS'),
                                    ),
                                  ],
                                  selected: {_method},
                                  onSelectionChanged:
                                      _isStarting || _challengeLocked
                                      ? null
                                      : (selection) => setState(() {
                                          _method = selection.first;
                                          _errorMessage = null;
                                        }),
                                  style: const ButtonStyle(
                                    minimumSize: WidgetStatePropertyAll(
                                      Size(0, 48),
                                    ),
                                  ),
                                ),
                              ),
                              const SizedBox(height: 18),
                              _ContactSummary(
                                icon: _method == AuthVerificationMethod.email
                                    ? Icons.alternate_email_rounded
                                    : Icons.phone_iphone_rounded,
                                label: _method == AuthVerificationMethod.email
                                    ? 'Mã sẽ được gửi đến email'
                                    : 'Mã SMS sẽ được gửi đến số điện thoại',
                                value: _method == AuthVerificationMethod.email
                                    ? _maskedEmail
                                    : _maskedPhone,
                              ),
                              if (expert) ...[
                                const SizedBox(height: 12),
                                const Text(
                                  'Tài khoản chuyên gia được xác minh bằng email.',
                                  style: TextStyle(
                                    fontFamily: 'Lexend',
                                    fontSize: 12,
                                    height: 1.4,
                                    color: AuthPalette.muted,
                                  ),
                                ),
                              ],
                            ],
                          ),
                        ),
                        const SizedBox(height: 22),
                        AuthPrimaryButton(
                          buttonKey: const Key(
                            'registration-verification-continue',
                          ),
                          label: _method == AuthVerificationMethod.email
                              ? 'Gửi mã qua email'
                              : 'Gửi mã SMS',
                          onPressed: _isStarting ? null : _continue,
                          isLoading: _isStarting,
                        ),
                        const SizedBox(height: 12),
                        const AuthInfoCard(
                          message:
                              'Mỗi lần đăng ký chỉ dùng một phương thức xác thực. Chúng tôi chỉ gửi mã sau khi bạn xác nhận lựa chọn.',
                        ),
                      ],
                    ),
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

class _ContactSummary extends StatelessWidget {
  const _ContactSummary({
    required this.icon,
    required this.label,
    required this.value,
  });

  final IconData icon;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: '$label: $value',
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: AuthPalette.accentDeep, size: 22),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    color: AuthPalette.mutedStrong,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  value,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 15,
                    fontWeight: FontWeight.w700,
                    color: AuthPalette.ink,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
