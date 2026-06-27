import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../../core/network/api_client.dart';
import '../services/auth_service.dart';

/// CB-003 — Verify OTP (UC-02)
/// Shared screen for both login and register flows.
/// Calls POST /api/v1/auth/verify-otp → stores tokens in AuthState → main.dart routes to MainShell.
class OtpVerificationScreen extends StatefulWidget {
  final String identifier;
  final bool isEmail;

  const OtpVerificationScreen({
    super.key,
    required this.identifier,
    required this.isEmail,
  });

  @override
  State<OtpVerificationScreen> createState() => _OtpVerificationScreenState();
}

class _OtpVerificationScreenState extends State<OtpVerificationScreen> {
  static const _primaryColor = Color(0xFFC98C7B);
  static const _canvasColor = Color(0xFFF6F1EC);
  static const _textColor = Color(0xFF5A463F);
  static const _mutedColor = Color(0xFF524440);
  static const _surfaceLowest = Colors.white;
  static const _borderColor = Color(0xFFD6C2BD);
  static const _errorColor = Color(0xFFBA1A1A);

  static const int _otpLength = 6;
  static const int _resendCooldownSeconds = 60;

  final List<TextEditingController> _cellCtrls =
      List.generate(_otpLength, (_) => TextEditingController());
  final List<FocusNode> _focusNodes =
      List.generate(_otpLength, (_) => FocusNode());

  bool _isLoading = false;
  bool _isResending = false;
  String? _errorMessage;
  int _resendCountdown = _resendCooldownSeconds;
  Timer? _countdownTimer;

  @override
  void initState() {
    super.initState();
    _startCountdown();
  }

  @override
  void dispose() {
    for (final c in _cellCtrls) c.dispose();
    for (final f in _focusNodes) f.dispose();
    _countdownTimer?.cancel();
    super.dispose();
  }

  void _startCountdown() {
    _resendCountdown = _resendCooldownSeconds;
    _countdownTimer?.cancel();
    _countdownTimer = Timer.periodic(const Duration(seconds: 1), (t) {
      if (!mounted) { t.cancel(); return; }
      setState(() {
        if (_resendCountdown > 0) {
          _resendCountdown--;
        } else {
          t.cancel();
        }
      });
    });
  }

  String get _maskedIdentifier {
    final id = widget.identifier;
    if (widget.isEmail) {
      final atIdx = id.indexOf('@');
      if (atIdx <= 1) return id;
      return '${id[0]}***${id.substring(atIdx)}';
    } else {
      if (id.length <= 4) return id;
      return '${id.substring(0, 2)}***${id.substring(id.length - 2)}';
    }
  }

  String get _otpValue => _cellCtrls.map((c) => c.text).join();

  void _onCellChanged(int index, String value) {
    if (value.isEmpty) {
      if (index > 0) _focusNodes[index - 1].requestFocus();
      return;
    }
    // Accept only one digit per cell
    if (value.length > 1) {
      _cellCtrls[index].text = value[value.length - 1];
      _cellCtrls[index].selection = TextSelection.collapsed(
        offset: _cellCtrls[index].text.length,
      );
    }
    setState(() => _errorMessage = null);
    if (index < _otpLength - 1) {
      _focusNodes[index + 1].requestFocus();
    } else {
      _focusNodes[index].unfocus();
    }
  }

  void _onKeyEvent(int index, KeyEvent event) {
    if (event is KeyDownEvent &&
        event.logicalKey.keyLabel == 'Backspace' &&
        _cellCtrls[index].text.isEmpty &&
        index > 0) {
      _focusNodes[index - 1].requestFocus();
    }
  }

  Future<void> _verify() async {
    final otp = _otpValue;
    if (otp.length < _otpLength) {
      setState(() => _errorMessage = 'Vui lòng nhập đủ 6 số.');
      return;
    }

    setState(() { _isLoading = true; _errorMessage = null; });

    try {
      await AuthService.instance.verifyOtp(
        email: widget.isEmail ? widget.identifier : null,
        phone: !widget.isEmail ? widget.identifier : null,
        otp: otp,
      );
      // AuthState.setTokens() calls notifyListeners() → main.dart switches to MainShell
      if (!mounted) return;
      Navigator.of(context).popUntil((route) => route.isFirst);
    } on ApiException catch (e) {
      String msg;
      if (e.statusCode == 400) {
        msg = 'Mã xác thực không hợp lệ hoặc đã hết hạn.';
      } else if (e.statusCode == 404) {
        msg = 'Không tìm thấy yêu cầu OTP. Vui lòng thử lại.';
      } else {
        msg = 'Xác thực thất bại. Vui lòng thử lại.';
      }
      setState(() => _errorMessage = msg);
      for (final c in _cellCtrls) c.clear();
      if (mounted) _focusNodes[0].requestFocus();
    } catch (_) {
      setState(() => _errorMessage = 'Không thể kết nối đến máy chủ.');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _resend() async {
    if (_resendCountdown > 0 || _isResending) return;
    setState(() => _isResending = true);
    try {
      await AuthService.instance.resendOtp(
        email: widget.isEmail ? widget.identifier : null,
        phone: !widget.isEmail ? widget.identifier : null,
      );
      _startCountdown();
      for (final c in _cellCtrls) c.clear();
      if (mounted) _focusNodes[0].requestFocus();
      setState(() => _errorMessage = null);
    } catch (_) {
      setState(() => _errorMessage = 'Không thể gửi lại mã. Vui lòng thử lại.');
    } finally {
      if (mounted) setState(() => _isResending = false);
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
                    const SizedBox(height: 32),
                    _buildContextText(),
                    const SizedBox(height: 32),
                    _buildOtpCells(),
                    if (_errorMessage != null) ...[
                      const SizedBox(height: 12),
                      _buildErrorRow(_errorMessage!),
                    ],
                    const SizedBox(height: 32),
                    _buildResendRow(),
                    const SizedBox(height: 32),
                  ],
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(24, 8, 24, 32),
              child: SizedBox(
                height: 52,
                child: FilledButton(
                  onPressed: _isLoading ? null : _verify,
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
                          'Xác nhận',
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 16,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAppBar(BuildContext context) {
    return SizedBox(
      height: 48,
      child: Row(
        children: [
          IconButton(
            onPressed: () => Navigator.pop(context),
            icon: const Icon(Icons.arrow_back),
            color: _textColor,
          ),
          Expanded(
            child: Text(
              'Xác thực mã OTP',
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

  Widget _buildContextText() {
    return Column(
      children: [
        Text(
          'Chúng tôi đã gửi mã xác thực đến',
          textAlign: TextAlign.center,
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 16,
            color: _mutedColor,
          ),
        ),
        const SizedBox(height: 8),
        Text(
          _maskedIdentifier,
          textAlign: TextAlign.center,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 20,
            fontWeight: FontWeight.w600,
            color: _textColor,
          ),
        ),
      ],
    );
  }

  Widget _buildOtpCells() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: List.generate(_otpLength, (i) {
        return Padding(
          padding: EdgeInsets.only(right: i < _otpLength - 1 ? 8 : 0),
          child: _buildCell(i),
        );
      }),
    );
  }

  Widget _buildCell(int index) {
    final isFocused = _focusNodes[index].hasFocus;
    final hasError = _errorMessage != null;

    return KeyboardListener(
      focusNode: FocusNode(),
      onKeyEvent: (e) => _onKeyEvent(index, e),
      child: SizedBox(
        width: 48,
        height: 56,
        child: TextField(
          controller: _cellCtrls[index],
          focusNode: _focusNodes[index],
          keyboardType: TextInputType.number,
          maxLength: 1,
          textAlign: TextAlign.center,
          onChanged: (v) => _onCellChanged(index, v),
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 24,
            fontWeight: FontWeight.w600,
            color: _textColor,
          ),
          decoration: InputDecoration(
            counterText: '',
            filled: true,
            fillColor: _surfaceLowest,
            contentPadding: EdgeInsets.zero,
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: BorderSide(
                color: hasError ? _errorColor : _borderColor,
                width: hasError ? 1.5 : 1,
              ),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: BorderSide(
                color: hasError ? _errorColor : _primaryColor,
                width: 2,
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildErrorRow(String message) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        const Icon(Icons.error_outline, size: 16, color: _errorColor),
        const SizedBox(width: 4),
        Text(
          message,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 12,
            fontWeight: FontWeight.w500,
            color: _errorColor,
            letterSpacing: 0.6,
          ),
        ),
      ],
    );
  }

  Widget _buildResendRow() {
    if (_resendCountdown > 0) {
      final mm = (_resendCountdown ~/ 60).toString().padLeft(2, '0');
      final ss = (_resendCountdown % 60).toString().padLeft(2, '0');
      return Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Text(
            'Gửi lại mã sau ',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _mutedColor.withOpacity(0.7),
            ),
          ),
          Text(
            '$mm:$ss',
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 20,
              fontWeight: FontWeight.w600,
              color: _textColor,
            ),
          ),
        ],
      );
    }

    return Center(
      child: TextButton(
        onPressed: _isResending ? null : _resend,
        child: _isResending
            ? const SizedBox(
                width: 16,
                height: 16,
                child: CircularProgressIndicator(strokeWidth: 2, color: _primaryColor),
              )
            : const Text(
                'Gửi lại mã',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                  color: Color(0xFF845143),
                ),
              ),
      ),
    );
  }
}
