import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';

class ResetPasswordScreen extends StatefulWidget {
  final String token;
  const ResetPasswordScreen({super.key, required this.token});

  @override
  State<ResetPasswordScreen> createState() => _ResetPasswordScreenState();
}

class _ResetPasswordScreenState extends State<ResetPasswordScreen> {
  static const _bgColor = Color(0xFFFFF8F6);
  static const _primaryColor = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);
  static const _surfaceContainerLowest = Color(0xFFFFFFFF);
  static const _surfaceContainerLow = Color(0xFFFFF1EC);

  final _passwordController = TextEditingController();
  final _confirmController = TextEditingController();
  bool _obscurePassword = true;
  bool _obscureConfirm = true;
  bool _isSubmitting = false;
  String? _error;

  bool get _hasMinLength => _passwordController.text.length >= 8;
  bool get _hasDigit => _passwordController.text.contains(RegExp(r'\d'));
  bool get _passwordsMatch => _passwordController.text == _confirmController.text && _confirmController.text.isNotEmpty;

  Future<void> _submit() async {
    if (!_hasMinLength || !_hasDigit || !_passwordsMatch) return;
    setState(() { _isSubmitting = true; _error = null; });
    try {
      await apiPost('/api/v1/auth/reset-password', {
        'token': widget.token,
        'newPassword': _passwordController.text,
        'confirmPassword': _confirmController.text,
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Đặt lại mật khẩu thành công!')));
        Navigator.of(context).popUntil((r) => r.isFirst);
      }
    } on ApiException catch (e) {
      if (mounted) setState(() { _error = 'Lỗi: ${e.statusCode}. Vui lòng thử lại.'; _isSubmitting = false; });
    } catch (_) {
      if (mounted) setState(() { _error = 'Không thể kết nối.'; _isSubmitting = false; });
    }
  }

  @override
  void dispose() { _passwordController.dispose(); _confirmController.dispose(); super.dispose(); }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _bgColor,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              IconButton(onPressed: () => Navigator.of(context).pop(), icon: const Icon(Icons.arrow_back, color: _primaryColor)),
              const SizedBox(height: 16),
              const Text('Đặt lại mật khẩu', style: TextStyle(fontFamily: 'Lexend', fontSize: 32, fontWeight: FontWeight.w700, color: _primaryContainer)),
              const SizedBox(height: 8),
              const Text('Vui lòng tạo một mật khẩu mới để bảo mật tài khoản của bạn.', style: TextStyle(fontFamily: 'Lexend', fontSize: 16, color: _onSurfaceVariant, height: 1.5)),
              const SizedBox(height: 24),
              _buildLabel('Mật khẩu mới'),
              const SizedBox(height: 8),
              _buildPasswordField(_passwordController, 'Nhập mật khẩu mới', _obscurePassword, () => setState(() => _obscurePassword = !_obscurePassword)),
              const SizedBox(height: 16),
              _buildLabel('Xác nhận mật khẩu'),
              const SizedBox(height: 8),
              _buildPasswordField(_confirmController, 'Nhập lại mật khẩu', _obscureConfirm, () => setState(() => _obscureConfirm = !_obscureConfirm)),
              const SizedBox(height: 24),
              _buildRequirements(),
              if (_error != null) ...[
                const SizedBox(height: 12),
                Text(_error!, style: const TextStyle(fontFamily: 'Lexend', fontSize: 13, color: Color(0xFFBA1A1A))),
              ],
              const Spacer(),
              SizedBox(
                width: double.infinity, height: 52,
                child: FilledButton(
                  onPressed: _isSubmitting || !_hasMinLength || !_hasDigit || !_passwordsMatch ? null : _submit,
                  style: FilledButton.styleFrom(backgroundColor: _primaryContainer, disabledBackgroundColor: _primaryContainer.withValues(alpha: 0.4), shape: const StadiumBorder()),
                  child: _isSubmitting
                      ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                      : const Text('Cập nhật mật khẩu', style: TextStyle(fontFamily: 'Lexend', fontSize: 16, fontWeight: FontWeight.w600, color: Colors.white)),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildLabel(String text) => Text(text, style: const TextStyle(fontFamily: 'Lexend', fontSize: 14, fontWeight: FontWeight.w500, color: _onSurface));

  Widget _buildPasswordField(TextEditingController c, String hint, bool obscure, VoidCallback toggle) {
    return TextField(
      controller: c,
      obscureText: obscure,
      onChanged: (_) => setState(() {}),
      style: const TextStyle(fontFamily: 'Lexend', fontSize: 16, color: _onSurface),
      decoration: InputDecoration(
        hintText: hint, hintStyle: TextStyle(fontFamily: 'Lexend', color: _outlineVariant),
        prefixIcon: Icon(Icons.lock_outline, color: _outlineVariant),
        suffixIcon: IconButton(icon: Icon(obscure ? Icons.visibility_off : Icons.visibility, color: _outlineVariant), onPressed: toggle),
        filled: true, fillColor: _surfaceContainerLowest,
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: BorderSide(color: _outlineVariant)),
        enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: BorderSide(color: _outlineVariant)),
        focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: BorderSide(color: _primaryContainer, width: 2)),
      ),
    );
  }

  Widget _buildRequirements() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(color: _surfaceContainerLow, borderRadius: BorderRadius.circular(16)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('YÊU CẦU MẬT KHẨU', style: TextStyle(fontFamily: 'Lexend', fontSize: 12, fontWeight: FontWeight.w600, letterSpacing: 0.6, color: _onSurfaceVariant)),
          const SizedBox(height: 12),
          _requirementRow('Ít nhất 8 ký tự', _hasMinLength),
          const SizedBox(height: 8),
          _requirementRow('Chứa ít nhất 1 chữ số', _hasDigit),
          const SizedBox(height: 8),
          _requirementRow('Mật khẩu khớp nhau', _passwordsMatch),
        ],
      ),
    );
  }

  Widget _requirementRow(String text, bool met) {
    return Row(
      children: [
        Icon(met ? Icons.check_circle_outline : Icons.circle_outlined, color: met ? _primaryContainer : _outlineVariant, size: 20),
        const SizedBox(width: 8),
        Text(text, style: TextStyle(fontFamily: 'Lexend', fontSize: 14, color: met ? _onSurface : _onSurfaceVariant)),
      ],
    );
  }
}
