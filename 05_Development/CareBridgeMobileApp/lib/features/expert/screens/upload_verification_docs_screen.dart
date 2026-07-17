import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';

import '../../../core/network/api_client.dart';
import '../models/expert_onboarding_model.dart';
import '../services/expert_image_capture.dart';
import '../services/expert_onboarding_service.dart';
import '../services/expert_onboarding_store.dart';

class UploadVerificationDocsScreen extends StatefulWidget {
  const UploadVerificationDocsScreen({super.key, this.capture, this.service});

  final ExpertImageCapture? capture;
  final ExpertOnboardingService? service;

  @override
  State<UploadVerificationDocsScreen> createState() => _UploadVerificationDocsScreenState();
}

class _UploadVerificationDocsScreenState extends State<UploadVerificationDocsScreen> {
  final _formKey = GlobalKey<FormState>();
  final _number = TextEditingController();
  final _issuer = TextEditingController();
  final _issuedDate = TextEditingController();
  final _expiryDate = TextEditingController();
  String _type = 'MEDICAL_LICENSE';
  ExpertEvidenceImage? _file;
  bool _loading = false;
  String? _error;

  @override
  void dispose() {
    _number.dispose();
    _issuer.dispose();
    _issuedDate.dispose();
    _expiryDate.dispose();
    super.dispose();
  }

  Future<void> _pick(ImageSource source) async {
    try {
      final image = await (widget.capture ?? ImagePickerExpertCapture())
          .capture(ExpertEvidenceKind.credential, source: source);
      if (image != null && mounted) setState(() => _file = image);
    } catch (_) {
      if (mounted) setState(() => _error = 'Không thể chọn ảnh giấy tờ.');
    }
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    if (_file == null) {
      setState(() => _error = 'Vui lòng chụp hoặc chọn ảnh giấy tờ chuyên môn.');
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      await (widget.service ?? ExpertOnboardingService.instance).submitCredential(
        credentialType: _type,
        credentialNumber: _number.text.trim(),
        issuer: _issuer.text.trim(),
        issuedDate: _issuedDate.text.trim(),
        expiryDate: _expiryDate.text.trim(),
        file: _file!,
      );
      ExpertOnboardingStore.instance.invalidate();
      if (mounted) context.go('/expert-verification-status');
    } on ArgumentError catch (e) {
      setState(() => _error = e.message.toString());
    } on ApiException {
      setState(() => _error = 'Không thể gửi giấy tờ. Vui lòng kiểm tra dữ liệu và thử lại.');
    } catch (_) {
      setState(() => _error = 'Không thể kết nối đến máy chủ.');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  String? _required(String? value) => (value ?? '').trim().isEmpty ? 'Thông tin này là bắt buộc' : null;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF7F1ED),
      appBar: AppBar(title: const Text('Giấy tờ chuyên môn'), backgroundColor: Colors.transparent),
      body: SafeArea(
        child: Form(
          key: _formKey,
          child: ListView(
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 32),
            children: [
              const Text('Bước 3/3', style: TextStyle(color: Color(0xFFC06F5A), fontWeight: FontWeight.w700)),
              const SizedBox(height: 6),
              const Text('Chứng minh chuyên môn', style: TextStyle(fontSize: 28, fontWeight: FontWeight.w800, color: Color(0xFF5A463F))),
              const SizedBox(height: 8),
              const Text('Cần ít nhất một giấy phép, bằng cấp hoặc chứng chỉ hợp lệ trước khi được duyệt.', style: TextStyle(height: 1.5, color: Color(0xFF75635C))),
              const SizedBox(height: 22),
              DropdownButtonFormField<String>(
                initialValue: _type,
                decoration: _decoration('Loại giấy tờ'),
                items: const [
                  DropdownMenuItem(value: 'MEDICAL_LICENSE', child: Text('Giấy phép hành nghề y')),
                  DropdownMenuItem(value: 'PROFESSIONAL_LICENSE', child: Text('Giấy phép chuyên môn')),
                  DropdownMenuItem(value: 'DEGREE', child: Text('Bằng cấp')),
                  DropdownMenuItem(value: 'CERTIFICATE', child: Text('Chứng chỉ')),
                ],
                onChanged: (value) => setState(() => _type = value!),
              ),
              const SizedBox(height: 14),
              TextFormField(controller: _number, validator: _required, decoration: _decoration('Số hiệu giấy tờ')),
              const SizedBox(height: 14),
              TextFormField(controller: _issuer, validator: _required, decoration: _decoration('Cơ quan cấp')),
              const SizedBox(height: 14),
              TextFormField(controller: _issuedDate, validator: _dateValidator, keyboardType: TextInputType.datetime, decoration: _decoration('Ngày cấp (YYYY-MM-DD)')),
              const SizedBox(height: 14),
              TextFormField(controller: _expiryDate, validator: _optionalDateValidator, keyboardType: TextInputType.datetime, decoration: _decoration('Ngày hết hạn (nếu có)')),
              const SizedBox(height: 18),
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(20), border: Border.all(color: const Color(0xFFE8DDD6))),
                child: Column(children: [
                  if (_file == null)
                    const Padding(
                      padding: EdgeInsets.symmetric(vertical: 18),
                      child: Icon(Icons.workspace_premium_outlined, size: 54, color: Color(0xFFC98C7B)),
                    )
                  else
                    ClipRRect(
                      borderRadius: BorderRadius.circular(14),
                      child: Image.memory(Uint8List.fromList(_file!.bytes), height: 180, width: double.infinity, fit: BoxFit.cover),
                    ),
                  const SizedBox(height: 12),
                  Row(children: [
                    Expanded(child: OutlinedButton.icon(onPressed: () => _pick(ImageSource.camera), icon: const Icon(Icons.camera_alt_outlined), label: Text(_file == null ? 'Chụp giấy tờ' : 'Chụp lại'))),
                    const SizedBox(width: 8),
                    IconButton.filledTonal(onPressed: () => _pick(ImageSource.gallery), icon: const Icon(Icons.photo_library_outlined)),
                  ]),
                ]),
              ),
              if (_error != null) ...[
                const SizedBox(height: 14),
                Text(_error!, style: const TextStyle(color: Color(0xFF93000A))),
              ],
              const SizedBox(height: 20),
              SizedBox(
                height: 54,
                child: FilledButton(
                  onPressed: _loading ? null : _submit,
                  style: FilledButton.styleFrom(backgroundColor: const Color(0xFFC98C7B), shape: const StadiumBorder()),
                  child: _loading ? const CircularProgressIndicator(color: Colors.white) : const Text('Gửi hồ sơ xét duyệt', style: TextStyle(fontWeight: FontWeight.w700)),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  InputDecoration _decoration(String label) => InputDecoration(labelText: label, filled: true, fillColor: Colors.white, border: OutlineInputBorder(borderRadius: BorderRadius.circular(16)));

  String? _dateValidator(String? value) {
    final required = _required(value);
    if (required != null) return required;
    return RegExp(r'^\d{4}-\d{2}-\d{2}$').hasMatch(value!.trim()) ? null : 'Dùng định dạng YYYY-MM-DD';
  }

  String? _optionalDateValidator(String? value) {
    if ((value ?? '').trim().isEmpty) return null;
    return _dateValidator(value);
  }
}
