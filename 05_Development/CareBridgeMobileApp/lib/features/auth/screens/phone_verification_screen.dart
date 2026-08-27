import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';

import '../models/registration_draft.dart';
import '../services/auth_service.dart';
import '../widgets/auth_ui.dart';

/// A native-feeling SMS verification surface shared by phone login and sign-up.
///
/// Firebase owns the SMS challenge. This screen only receives a verification
/// id, exchanges the code for a fresh Firebase ID token, and hands that token
/// to the CareBridge API through [AuthService].
class PhoneVerificationScreen extends StatefulWidget {
  const PhoneVerificationScreen.login({
    super.key,
    required this.phoneNumber,
    this.authService,
  }) : _isRegistration = false;

  const PhoneVerificationScreen.registration({
    super.key,
    required this.phoneNumber,
    this.authService,
  }) : _isRegistration = true;

  final String phoneNumber;
  final AuthService? authService;
  final bool _isRegistration;

  @override
  State<PhoneVerificationScreen> createState() =>
      _PhoneVerificationScreenState();
}

class _PhoneVerificationScreenState extends State<PhoneVerificationScreen> {
  static const _cooldownSeconds = 60;
  final _codeController = TextEditingController();
  final _codeFocusNode = FocusNode();
  Timer? _cooldownTimer;

  String? _verificationId;
  String? _confirmedIdToken;
  int? _resendToken;
  int _cooldown = 0;
  String? _errorMessage;
  bool _isSending = false;
  bool _isVerifying = false;
  bool _completionStarted = false;
  bool _hasCodeBeenSent = false;

  AuthService get _service => widget.authService ?? AuthService.instance;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _sendCode());
  }

  @override
  void dispose() {
    if (widget._isRegistration && !_completionStarted) {
      RegistrationDraftStore.clear();
    }
    _cooldownTimer?.cancel();
    _codeController.dispose();
    _codeFocusNode.dispose();
    super.dispose();
  }

  Future<void> _sendCode({bool resend = false}) async {
    if (_isSending || _isVerifying || (resend && _cooldown > 0)) return;
    setState(() {
      _isSending = true;
      _errorMessage = null;
    });

    try {
      await _service.beginPhoneVerification(
        phoneNumber: widget.phoneNumber,
        forceResendingToken: resend ? _resendToken : null,
        verificationCompleted: _completeWithIdToken,
        verificationFailed: _handleVerificationFailure,
        codeSent: (verificationId, resendToken) =>
            _handleCodeSent(verificationId, resendToken, isResend: resend),
        codeAutoRetrievalTimeout: _handleTimeout,
      );
    } on PhoneVerificationFailure catch (error) {
      _handleVerificationFailure(error);
    } catch (_) {
      _handleVerificationFailure(const PhoneVerificationFailure('unknown'));
    }
  }

  void _handleCodeSent(
    String verificationId,
    int? resendToken, {
    required bool isResend,
  }) {
    if (!mounted) return;
    setState(() {
      _verificationId = verificationId;
      if (isResend) _confirmedIdToken = null;
      _resendToken = resendToken;
      _hasCodeBeenSent = true;
      _isSending = false;
      _errorMessage = null;
    });
    _codeController.clear();
    _startCooldown();
    _codeFocusNode.requestFocus();
  }

  void _handleTimeout(String verificationId) {
    if (!mounted) return;
    setState(() {
      _verificationId ??= verificationId;
      _isSending = false;
      _errorMessage =
          'Mã vẫn còn hiệu lực. Bạn có thể nhập mã SMS hoặc gửi lại mã mới.';
    });
  }

  void _handleVerificationFailure(PhoneVerificationFailure error) {
    debugPrint('Firebase phone verification failed: code=${error.code}');
    if (!mounted) return;
    setState(() {
      _isSending = false;
      _errorMessage = error.userMessage;
    });
  }

  void _startCooldown() {
    _cooldownTimer?.cancel();
    setState(() => _cooldown = _cooldownSeconds);
    _cooldownTimer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (!mounted) {
        timer.cancel();
        return;
      }
      if (_cooldown <= 1) {
        timer.cancel();
        setState(() => _cooldown = 0);
      } else {
        setState(() => _cooldown--);
      }
    });
  }

  void _onCodeChanged(String value) {
    if (value.length == 6 && !_isVerifying) {
      unawaited(_verifyCode());
    }
  }

  Future<void> _verifyCode() async {
    final confirmedIdToken = _confirmedIdToken;
    if (confirmedIdToken != null && confirmedIdToken.trim().isNotEmpty) {
      setState(() {
        _isVerifying = true;
        _errorMessage = null;
      });
      await _completeWithIdToken(confirmedIdToken);
      return;
    }

    final code = _codeController.text.trim();
    if (!RegExp(r'^\d{6}$').hasMatch(code)) {
      setState(
        () => _errorMessage = 'Vui lòng nhập đủ 6 chữ số trong tin nhắn.',
      );
      return;
    }
    final verificationId = _verificationId;
    if (verificationId == null || verificationId.isEmpty) {
      setState(
        () => _errorMessage = 'Mã xác thực chưa sẵn sàng. Vui lòng gửi lại mã.',
      );
      return;
    }
    setState(() {
      _isVerifying = true;
      _errorMessage = null;
    });
    try {
      final idToken = await _service.confirmPhoneSmsCode(
        verificationId: verificationId,
        smsCode: code,
      );
      await _completeWithIdToken(idToken);
    } on PhoneVerificationFailure catch (error) {
      if (mounted) {
        setState(() {
          _isVerifying = false;
          _errorMessage = error.userMessage;
        });
        _codeController.clear();
        _codeFocusNode.requestFocus();
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _isVerifying = false;
          _errorMessage = 'Không thể xác thực mã. Vui lòng thử lại.';
        });
        _codeController.clear();
        _codeFocusNode.requestFocus();
      }
    }
  }

  Future<void> _completeWithIdToken(String idToken) async {
    if (!mounted) return;
    final confirmedIdToken = idToken.trim();
    if (confirmedIdToken.isEmpty) return;
    _confirmedIdToken = confirmedIdToken;
    if (_completionStarted) return;
    _completionStarted = true;
    if (mounted) {
      setState(() {
        _isSending = false;
        _isVerifying = true;
        _errorMessage = null;
      });
    }
    try {
      if (widget._isRegistration) {
        final draft = RegistrationDraftStore.active;
        if (draft == null) {
          throw const FormatException('Registration draft is unavailable');
        }
        await _service.registerWithPhoneIdToken(
          idToken: confirmedIdToken,
          name: draft.name,
          email: draft.email,
          phone: draft.phone,
          password: draft.password,
          role: draft.role,
          shouldPersistSession: () => mounted,
        );
        RegistrationDraftStore.clear();
      } else {
        await _service.loginWithPhoneIdToken(
          confirmedIdToken,
          shouldPersistSession: () => mounted,
        );
      }
      if (mounted) context.go('/auth-landing');
    } catch (error) {
      _completionStarted = false;
      if (!mounted) return;
      setState(() {
        _isVerifying = false;
        _errorMessage = _messageForApiError(error);
      });
    }
  }

  String _messageForApiError(Object error) {
    final text = error.toString();
    if (text.contains('409') || text.contains('ACCOUNT_EXISTS')) {
      return 'Tài khoản đã tồn tại. Hãy đăng nhập hoặc dùng thông tin khác.';
    }
    if (text.contains('401') || text.contains('403')) {
      return 'Không thể tiếp tục với số điện thoại này hoặc tài khoản đang bị khóa.';
    }
    if (error is FormatException) {
      return 'Phản hồi đăng nhập không hợp lệ. Vui lòng thử lại sau.';
    }
    return 'Không thể kết nối đến máy chủ. Kiểm tra kết nối mạng.';
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: !_completionStarted,
      child: Scaffold(
        body: AuthBackground(
          child: SafeArea(
            child: Column(
              children: [
                AuthTopBar(
                  title: widget._isRegistration
                      ? 'Xác minh đăng ký'
                      : 'Xác minh số điện thoại',
                  onBack: () {
                    if (!_completionStarted) Navigator.pop(context);
                  },
                ),
                Expanded(
                  child: SingleChildScrollView(
                    padding: const EdgeInsets.fromLTRB(24, 22, 24, 28),
                    child: AuthSurface(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          const Icon(
                            Icons.sms_rounded,
                            size: 44,
                            color: AuthPalette.accentDeep,
                          ),
                          const SizedBox(height: 18),
                          Text(
                            'Nhập mã SMS',
                            textAlign: TextAlign.center,
                            style: Theme.of(context).textTheme.headlineSmall
                                ?.copyWith(
                                  fontFamily: 'Lexend',
                                  fontWeight: FontWeight.w800,
                                  color: AuthPalette.ink,
                                ),
                          ),
                          const SizedBox(height: 10),
                          Text(
                            _hasCodeBeenSent
                                ? 'Mã 6 số đã được gửi tới ${widget.phoneNumber}.'
                                : (_isSending
                                      ? 'Đang gửi mã 6 số tới ${widget.phoneNumber}...'
                                      : 'Mã SMS chưa được gửi. Vui lòng thử gửi lại.'),
                            textAlign: TextAlign.center,
                            style: const TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 13,
                              height: 1.45,
                              color: AuthPalette.mutedStrong,
                            ),
                          ),
                          const SizedBox(height: 24),
                          Semantics(
                            textField: true,
                            label: 'Mã xác thực SMS gồm 6 chữ số',
                            child: TextField(
                              key: const Key('phone-sms-code'),
                              controller: _codeController,
                              focusNode: _codeFocusNode,
                              autofocus: _hasCodeBeenSent,
                              enabled: _hasCodeBeenSent && !_isVerifying,
                              keyboardType: TextInputType.number,
                              textInputAction: TextInputAction.done,
                              autofillHints: const [AutofillHints.oneTimeCode],
                              inputFormatters: [
                                FilteringTextInputFormatter.digitsOnly,
                                LengthLimitingTextInputFormatter(6),
                              ],
                              textAlign: TextAlign.center,
                              style: const TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 26,
                                letterSpacing: 8,
                                fontWeight: FontWeight.w700,
                                color: AuthPalette.ink,
                              ),
                              decoration: const InputDecoration(
                                labelText: 'Mã SMS',
                                hintText: '000000',
                                counterText: '',
                              ),
                              onChanged: _onCodeChanged,
                              onSubmitted: (_) => _verifyCode(),
                            ),
                          ),
                          if (_errorMessage != null) ...[
                            const SizedBox(height: 16),
                            AuthErrorBanner(message: _errorMessage!),
                          ],
                          const SizedBox(height: 22),
                          AuthPrimaryButton(
                            buttonKey: const Key('phone-sms-submit'),
                            label: _isVerifying
                                ? 'Đang xác minh...'
                                : 'Xác minh',
                            onPressed:
                                _isVerifying ||
                                    (!_hasCodeBeenSent &&
                                        (_confirmedIdToken == null ||
                                            _confirmedIdToken!.isEmpty))
                                ? null
                                : _verifyCode,
                            isLoading: _isVerifying,
                          ),
                          const SizedBox(height: 12),
                          TextButton(
                            key: const Key('phone-sms-resend'),
                            onPressed:
                                _isSending || _isVerifying || _cooldown > 0
                                ? null
                                : () => _sendCode(resend: true),
                            style: TextButton.styleFrom(
                              minimumSize: const Size(0, 44),
                            ),
                            child: Text(
                              _cooldown > 0
                                  ? 'Gửi lại mã sau $_cooldown giây'
                                  : (_isSending
                                        ? 'Đang gửi mã...'
                                        : 'Gửi lại mã'),
                            ),
                          ),
                          if (!_hasCodeBeenSent && _isSending) ...[
                            const SizedBox(height: 8),
                            const Center(child: CircularProgressIndicator()),
                          ],
                        ],
                      ),
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
