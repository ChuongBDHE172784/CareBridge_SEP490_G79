import 'package:flutter/material.dart';
import '../models/baby_model.dart';
import '../services/baby_service.dart';
import '../../../core/network/api_client.dart';

/// Add Baby Profile screen — UC-31
/// Stub: collects nickname + birthDate + gender, calls POST /api/v1/babies.
class AddBabyScreen extends StatefulWidget {
  const AddBabyScreen({super.key});

  @override
  State<AddBabyScreen> createState() => _AddBabyScreenState();
}

class _AddBabyScreenState extends State<AddBabyScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFF6F1EC);
  static const _onSurface = Color(0xFF271812);

  final _nicknameCtrl = TextEditingController();
  DateTime? _birthDate;
  BabyGender _gender = BabyGender.unknown;
  bool _loading = false;
  String? _error;

  final _service = BabyService();

  @override
  void dispose() {
    _nicknameCtrl.dispose();
    super.dispose();
  }

  String _formatDate(DateTime d) =>
      '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

  Future<void> _submit() async {
    if (_nicknameCtrl.text.trim().isEmpty || _birthDate == null) {
      setState(() => _error = 'Vui lòng nhập tên và ngày sinh.');
      return;
    }
    setState(() { _loading = true; _error = null; });
    try {
      await _service.createBabyProfile(CreateBabyRequest(
        nickname: _nicknameCtrl.text.trim(),
        birthDate: _formatDate(_birthDate!),
        gender: _gender,
      ));
      if (!mounted) return;
      Navigator.pop(context);
    } on ApiException catch (e) {
      setState(() => _error = 'Tạo hồ sơ thất bại (${e.statusCode}).') ;
    } catch (_) {
      setState(() => _error = 'Lỗi kết nối.');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      appBar: AppBar(
        backgroundColor: _canvas,
        elevation: 0,
        leading: IconButton(
          onPressed: () => Navigator.pop(context),
          icon: const Icon(Icons.arrow_back, color: _onSurface),
        ),
        title: const Text(
          'Thêm hồ sơ bé',
          style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w600, color: _primary),
        ),
      ),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (_error != null) ...[
              Text(_error!, style: const TextStyle(color: Color(0xFFBA1A1A), fontFamily: 'Lexend')),
              const SizedBox(height: 12),
            ],
            TextField(
              controller: _nicknameCtrl,
              decoration: const InputDecoration(
                labelText: 'Tên bé',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 16),
            // TODO: full form with birthDate picker, gender, weight, height
            const SizedBox(height: 32),
            SizedBox(
              width: double.infinity,
              height: 52,
              child: FilledButton(
                onPressed: _loading ? null : _submit,
                style: FilledButton.styleFrom(
                  backgroundColor: _primaryContainer,
                  shape: const StadiumBorder(),
                ),
                child: _loading
                    ? const CircularProgressIndicator(color: Colors.white, strokeWidth: 2)
                    : const Text('Lưu hồ sơ',
                        style: TextStyle(fontFamily: 'Lexend', fontSize: 16, fontWeight: FontWeight.w600)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
