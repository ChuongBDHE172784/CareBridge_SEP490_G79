import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';
import '../../../core/auth/auth_state.dart';

class OtpVerifyScreen extends StatefulWidget {
  const OtpVerifyScreen({super.key, required this.phone});

  final String phone;

  @override
  State<OtpVerifyScreen> createState() => _OtpVerifyScreenState();
}

class _OtpVerifyScreenState extends State<OtpVerifyScreen> {
  final _otpController = TextEditingController();
  bool _loading = false;
  String? _error;

  @override
  void dispose() {
    _otpController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    setState(() { _loading = true; _error = null; });
    try {
      final result = await apiPost('/api/v1/auth/verify-otp', {
        'phone': widget.phone,
        'otp': _otpController.text.trim(),
      });

      final data = result['data'] as Map<String, dynamic>;
      final accessToken = data['accessToken'] as String;
      final refreshToken = data['refreshToken'] as String;

      final profileResult = await apiGet('/api/v1/auth/profile', token: accessToken);
      final profile = profileResult['data'] as Map<String, dynamic>;

      await AuthState.instance.setTokens(
        accessToken: accessToken,
        refreshToken: refreshToken,
        userId: profile['id'].toString(),
        role: profile['role'] as String,
      );
    } on ApiException catch (e) {
      setState(() { _error = e.statusCode == 401 ? 'Invalid or expired OTP.' : 'Verification failed.'; });
    } catch (_) {
      setState(() { _error = 'Network error. Check your connection.'; });
    } finally {
      if (mounted) setState(() { _loading = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Enter OTP')),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 32),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Code sent to ${widget.phone}',
                style: const TextStyle(fontSize: 15, color: Colors.black54),
              ),
              const SizedBox(height: 24),
              if (_error != null) ...[
                Text(_error!, style: const TextStyle(color: Colors.red, fontSize: 14)),
                const SizedBox(height: 12),
              ],
              TextField(
                controller: _otpController,
                keyboardType: TextInputType.number,
                maxLength: 6,
                textAlign: TextAlign.center,
                style: const TextStyle(fontSize: 24, letterSpacing: 12),
                decoration: const InputDecoration(
                  labelText: 'OTP code',
                  border: OutlineInputBorder(),
                  counterText: '',
                ),
              ),
              const SizedBox(height: 24),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: _loading ? null : _submit,
                  child: _loading
                      ? const SizedBox(
                          height: 20, width: 20,
                          child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                        )
                      : const Text('Verify', style: TextStyle(fontSize: 16)),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
