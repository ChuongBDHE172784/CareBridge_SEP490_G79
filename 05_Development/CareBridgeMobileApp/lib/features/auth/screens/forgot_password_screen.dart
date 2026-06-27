import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';
import 'reset_password_screen.dart';

class ForgotPasswordScreen extends StatefulWidget {
  const ForgotPasswordScreen({super.key});

  @override
  State<ForgotPasswordScreen> createState() => _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends State<ForgotPasswordScreen> {
  static const _bgColor = Color(0xFFFFF8F6);
  static const _primaryColor = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);
  static const _surfaceContainerLowest = Color(0xFFFFFFFF);

  final _contactController = TextEditingController();
  final _tokenController = TextEditingController();
  bool _isSubmitting = false;
  String? _error;
  bool _sent = false;

  Future<void> _submit() async {
    final contact = _contactController.text.trim();
    if (contact.isEmpty) {
      setState(() => _error = 'Vui lòng nhập email hoặc số điện thoại.');
      return;
    }
    setState(() { _isSubmitting = true; _error = null; });
    try {
      await apiPost('/api/v1/auth/forgot-password', {'contact': contact});
      if (mounted) setState(() { _sent = true; _isSubmitting = false; });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _error = e.statusCode == 404 ? 'Tài khoản không tồn tại.' : 'Đã xảy ra lỗi. Vui lòng thử lại.';
        _isSubmitting = false;
      });
    } catch (_) {
      if (mounted) setState(() { _error = 'Không thể kết nối. Vui lòng thử lại.'; _isSubmitting = false; });
    }
  }

  void _goToReset() {
    final token = _tokenController.text.trim();
    if (token.isEmpty) {
      setState(() => _error = 'Vui lòng nhập mã xác nhận từ email.');
      return;
    }
    Navigator.of(context).pushReplacement(
      MaterialPageRoute(builder: (_) => ResetPasswordScreen(token: token)),
    );
  }

  @override
  void dispose() { _contactController.dispose(); _tokenController.dispose(); super.dispose(); }

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
              Row(
                children: [
                  IconButton(onPressed: () => Navigator.of(context).pop(), icon: const Icon(Icons.arrow_back, color: _primaryColor)),
                  const Expanded(child: Text('CareBridge', textAlign: TextAlign.center, style: TextStyle(fontFamily: 'Lexend', fontSize: 20, fontWeight: FontWeight.w700, color: _primaryColor))),
                  const SizedBox(width: 48),
                ],
              ),
              const SizedBox(height: 32),
              const Text('Quên mật khẩu', style: TextStyle(fontFamily: 'Lexend', fontSize: 32, fontWeight: FontWeight.w700, color: _onSurface)),
              const SizedBox(height: 12),
              Text(
                _sent ? 'Mã xác nhận đã được gửi. Vui lòng kiểm tra email hoặc tin nhắn.' : 'Vui lòng nhập email hoặc số điện thoại đã đăng ký. Chúng tôi sẽ gửi mã xác nhận để đặt lại mật khẩu.',
                style: const TextStyle(fontFamily: 'Lexend', fontSize: 16, color: _onSurfaceVariant, height: 1.5),
              ),
              const SizedBox(height: 32),
              if (!_sent) ...[
                TextField(
                  controller: _contactController,
                  style: const TextStyle(fontFamily: 'Lexend', fontSize: 16, color: _onSurface),
                  keyboardType: TextInputType.emailAddress,
                  decoration: InputDecoration(
                    hintText: 'Email hoặc Số điện thoại',
                    hintStyle: TextStyle(fontFamily: 'Lexend', color: _outlineVariant),
                    prefixIcon: Icon(Icons.alternate_email, color: _outlineVariant),
                    filled: true, fillColor: _surfaceContainerLowest,
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: BorderSide(color: _outlineVariant)),
                    enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: BorderSide(color: _outlineVariant)),
                    focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: BorderSide(color: _primaryContainer, width: 2)),
                  ),
                ),
              ] else ...[
                TextField(
                  controller: _tokenController,
                  style: const TextStyle(fontFamily: 'Lexend', fontSize: 16, color: _onSurface),
                  decoration: InputDecoration(
                    hintText: 'Dán mã xác nhận từ email',
                    hintStyle: TextStyle(fontFamily: 'Lexend', color: _outlineVariant),
                    prefixIcon: Icon(Icons.key, color: _outlineVariant),
                    filled: true, fillColor: _surfaceContainerLowest,
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: BorderSide(color: _outlineVariant)),
                    enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: BorderSide(color: _outlineVariant)),
                    focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: BorderSide(color: _primaryContainer, width: 2)),
                  ),
                ),
              ],
              if (_error != null) ...[
                const SizedBox(height: 12),
                Text(_error!, style: const TextStyle(fontFamily: 'Lexend', fontSize: 13, color: Color(0xFFBA1A1A))),
              ],
              const Spacer(),
              SizedBox(
                width: double.infinity, height: 52,
                child: FilledButton(
                  onPressed: _isSubmitting ? null : (_sent ? _goToReset : _submit),
                  style: FilledButton.styleFrom(backgroundColor: _primaryContainer, disabledBackgroundColor: _primaryContainer.withValues(alpha: 0.6), shape: const StadiumBorder()),
                  child: _isSubmitting
                      ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                      : Text(_sent ? 'Tiếp tục đặt lại mật khẩu' : 'Gửi mã', style: const TextStyle(fontFamily: 'Lexend', fontSize: 16, fontWeight: FontWeight.w600, color: Colors.white)),
                ),
              ),
              if (!_sent) ...[
                const SizedBox(height: 16),
                Center(
                  child: TextButton(
                    onPressed: () => Navigator.of(context).pop(),
                    child: const Text('Quay lại đăng nhập', style: TextStyle(fontFamily: 'Lexend', fontSize: 14, color: _onSurfaceVariant)),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
