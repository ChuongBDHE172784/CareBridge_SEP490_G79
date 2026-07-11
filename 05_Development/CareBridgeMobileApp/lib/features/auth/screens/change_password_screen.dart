import 'dart:convert';

import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';

class ChangePasswordScreen extends StatefulWidget {
  const ChangePasswordScreen({super.key});

  @override
  State<ChangePasswordScreen> createState() => _ChangePasswordScreenState();
}

class _ChangePasswordScreenState extends State<ChangePasswordScreen> {
  static const _bgColor = Color(0xFFFFF8F6);
  static const _primaryColor = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);
  static const _surfaceContainerLowest = Color(0xFFFFFFFF);
  static const _surfaceContainerLow = Color(0xFFFFF1EC);

  final _currentController = TextEditingController();
  final _newController = TextEditingController();
  final _confirmController = TextEditingController();
  bool _obscureCurrent = true;
  bool _obscureNew = true;
  bool _obscureConfirm = true;
  bool _isSubmitting = false;
  String? _error;

  bool get _hasMinLength => _newController.text.length >= 8;
  bool get _hasUppercase => _newController.text.contains(RegExp(r'[A-Z]'));
  bool get _hasLowercase => _newController.text.contains(RegExp(r'[a-z]'));
  bool get _hasDigit => _newController.text.contains(RegExp(r'\d'));
  bool get _hasSpecial => _newController.text.contains(RegExp(r'[^a-zA-Z0-9]'));
  bool get _notSameAsOld =>
      _newController.text.isNotEmpty &&
      _newController.text != _currentController.text;
  bool get _canSubmit =>
      _currentController.text.isNotEmpty &&
      _hasMinLength &&
      _hasUppercase &&
      _hasLowercase &&
      _hasDigit &&
      _hasSpecial &&
      _notSameAsOld &&
      _confirmController.text == _newController.text;

  String _messageForApiError(ApiException error) {
    String? code;
    String? serverMessage;
    try {
      final body = jsonDecode(error.message) as Map<String, dynamic>;
      code = body['error'] as String?;
      serverMessage = body['message'] as String?;
    } catch (_) {
      serverMessage = error.message;
    }

    switch (code) {
      case 'AUTH-071':
        return 'Mật khẩu hiện tại không đúng.';
      case 'AUTH-072':
        return 'Mật khẩu xác nhận không khớp.';
      case 'AUTH-073':
        return 'Mật khẩu mới chưa đáp ứng yêu cầu bảo mật.';
      case 'AUTH-074':
        return 'Mật khẩu mới không được trùng với mật khẩu hiện tại.';
    }

    if (serverMessage?.contains('Current password is incorrect') == true) {
      return 'Mật khẩu hiện tại không đúng.';
    }
    if (error.statusCode == 401) {
      return 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.';
    }
    if (error.statusCode == 400) {
      return 'Thông tin mật khẩu chưa hợp lệ. Vui lòng kiểm tra lại.';
    }
    return 'Đã xảy ra lỗi. Vui lòng thử lại.';
  }

  Future<void> _submit() async {
    if (!_canSubmit || _isSubmitting) return;
    setState(() {
      _isSubmitting = true;
      _error = null;
    });
    try {
      await apiPut('/api/v1/auth/change-password', {
        'oldPassword': _currentController.text,
        'newPassword': _newController.text,
        'confirmPassword': _confirmController.text,
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Đổi mật khẩu thành công!')),
        );
        Navigator.of(context).pop();
      }
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _error = _messageForApiError(e);
        _isSubmitting = false;
      });
    } catch (_) {
      if (mounted) {
        setState(() {
          _error = 'Không thể kết nối.';
          _isSubmitting = false;
        });
      }
    }
  }

  @override
  void dispose() {
    _currentController.dispose();
    _newController.dispose();
    _confirmController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _bgColor,
      body: SafeArea(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: SizedBox(
                height: 48,
                child: Row(
                  children: [
                    IconButton(
                      onPressed: () => Navigator.of(context).pop(),
                      icon: const Icon(Icons.arrow_back, color: _primaryColor),
                    ),
                    const Expanded(
                      child: Text(
                        'Đổi mật khẩu',
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
            ),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.fromLTRB(24, 16, 24, 32),
                children: [
                  const Text(
                    'Vui lòng nhập mật khẩu hiện tại và tạo mật khẩu mới để bảo mật tài khoản của bạn.',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 16,
                      color: _onSurfaceVariant,
                      height: 1.5,
                    ),
                  ),
                  const SizedBox(height: 24),
                  _buildLabel('Mật khẩu hiện tại'),
                  const SizedBox(height: 8),
                  _buildPwdField(
                    _currentController,
                    'Nhập mật khẩu hiện tại',
                    _obscureCurrent,
                    () => setState(() => _obscureCurrent = !_obscureCurrent),
                  ),
                  const SizedBox(height: 20),
                  _buildLabel('Mật khẩu mới'),
                  const SizedBox(height: 8),
                  _buildPwdField(
                    _newController,
                    'Nhập mật khẩu mới',
                    _obscureNew,
                    () => setState(() => _obscureNew = !_obscureNew),
                  ),
                  const SizedBox(height: 20),
                  _buildLabel('Xác nhận mật khẩu mới'),
                  const SizedBox(height: 8),
                  _buildPwdField(
                    _confirmController,
                    'Nhập lại mật khẩu mới',
                    _obscureConfirm,
                    () => setState(() => _obscureConfirm = !_obscureConfirm),
                  ),
                  const SizedBox(height: 24),
                  _buildRequirements(),
                  if (_error != null) ...[
                    const SizedBox(height: 12),
                    Text(
                      _error!,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 13,
                        color: Color(0xFFBA1A1A),
                      ),
                    ),
                  ],
                  const SizedBox(height: 32),
                  SizedBox(
                    width: double.infinity,
                    height: 52,
                    child: FilledButton(
                      onPressed: _canSubmit && !_isSubmitting ? _submit : null,
                      style: FilledButton.styleFrom(
                        backgroundColor: _primaryContainer,
                        disabledBackgroundColor: _primaryContainer.withValues(
                          alpha: 0.4,
                        ),
                        shape: const StadiumBorder(),
                      ),
                      child: _isSubmitting
                          ? const SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(
                                strokeWidth: 2,
                                color: Colors.white,
                              ),
                            )
                          : const Text(
                              'Cập nhật mật khẩu',
                              style: TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 16,
                                fontWeight: FontWeight.w600,
                                color: Colors.white,
                              ),
                            ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildLabel(String t) => Text(
    t,
    style: const TextStyle(
      fontFamily: 'Lexend',
      fontSize: 14,
      fontWeight: FontWeight.w500,
      color: _onSurface,
    ),
  );

  Widget _buildPwdField(
    TextEditingController c,
    String hint,
    bool obs,
    VoidCallback toggle,
  ) {
    return TextField(
      controller: c,
      obscureText: obs,
      onChanged: (_) => setState(() {}),
      style: const TextStyle(
        fontFamily: 'Lexend',
        fontSize: 16,
        color: _onSurface,
      ),
      decoration: InputDecoration(
        hintText: hint,
        hintStyle: TextStyle(fontFamily: 'Lexend', color: _outlineVariant),
        suffixIcon: IconButton(
          icon: Icon(
            obs ? Icons.visibility_off : Icons.visibility,
            color: _outlineVariant,
          ),
          onPressed: toggle,
        ),
        filled: true,
        fillColor: _surfaceContainerLowest,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide(color: _outlineVariant),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide(color: _outlineVariant),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide(color: _primaryContainer, width: 2),
        ),
      ),
    );
  }

  Widget _buildRequirements() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surfaceContainerLow,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'YÊU CẦU MẬT KHẨU',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 12,
              fontWeight: FontWeight.w600,
              letterSpacing: 0.6,
              color: _onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 12),
          _req('Ít nhất 8 ký tự', _hasMinLength),
          const SizedBox(height: 8),
          _req('Có chữ hoa và chữ thường', _hasUppercase && _hasLowercase),
          const SizedBox(height: 8),
          _req('Có ít nhất một chữ số', _hasDigit),
          const SizedBox(height: 8),
          _req('Có ít nhất một ký tự đặc biệt', _hasSpecial),
          const SizedBox(height: 8),
          _req('Không trùng mật khẩu cũ', _notSameAsOld),
        ],
      ),
    );
  }

  Widget _req(String t, bool met) => Row(
    children: [
      Icon(
        met ? Icons.check_circle : Icons.circle_outlined,
        color: met ? _primaryContainer : _outlineVariant,
        size: 20,
      ),
      const SizedBox(width: 8),
      Text(
        t,
        style: TextStyle(
          fontFamily: 'Lexend',
          fontSize: 14,
          color: met ? _onSurface : _onSurfaceVariant,
        ),
      ),
    ],
  );
}
