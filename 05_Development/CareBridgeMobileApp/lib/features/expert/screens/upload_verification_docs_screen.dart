import 'dart:typed_data';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';
import 'package:intl/intl.dart';
import 'package:mime/mime.dart';

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
  State<UploadVerificationDocsScreen> createState() =>
      _UploadVerificationDocsScreenState();
}

class _UploadVerificationDocsScreenState
    extends State<UploadVerificationDocsScreen> {
  static const _commonIssuers = [
    'Bộ Y tế',
    'Sở Y tế TP. Hồ Chí Minh',
    'Sở Y tế Hà Nội',
    'Sở Y tế Đà Nẵng',
    'Sở Y tế Cần Thơ',
    'Sở Y tế Hải Phòng',
    'Đại học Y Dược TP.HCM',
    'Đại học Y Hà Nội',
  ];

  final _formKey = GlobalKey<FormState>();
  final _number = TextEditingController();
  final _customIssuer = TextEditingController();
  final _issuedDate = TextEditingController();
  final _expiryDate = TextEditingController();
  String _type = 'MEDICAL_LICENSE';
  String? _issuer;
  ExpertEvidenceImage? _file;
  bool _loading = false;
  String? _error;

  bool get _isDegree => _type == 'DEGREE';
  bool get _isImage =>
      _file != null && {'image/jpeg', 'image/png'}.contains(_file!.mimeType);

  @override
  void dispose() {
    _number.dispose();
    _customIssuer.dispose();
    _issuedDate.dispose();
    _expiryDate.dispose();
    super.dispose();
  }

  Future<void> _pickImage(ImageSource source) async {
    try {
      final image = await (widget.capture ?? ImagePickerExpertCapture())
          .capture(ExpertEvidenceKind.credential, source: source);
      if (image != null && mounted) {
        setState(() {
          _file = image;
          _error = null;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _error = 'Không thể chọn ảnh giấy tờ.');
    }
  }

  Future<void> _pickDocument() async {
    try {
      final result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: const ['pdf', 'doc', 'docx', 'jpg', 'jpeg', 'png'],
        withData: true,
      );
      final selected = result?.files.single;
      final bytes = selected?.bytes;
      if (selected == null || bytes == null) return;
      final mimeType =
          lookupMimeType(selected.name, headerBytes: bytes) ??
          _mimeFromExtension(selected.extension);
      if (!mounted) return;
      setState(() {
        _file = ExpertEvidenceImage(
          bytes: bytes,
          fileName: selected.name,
          mimeType: mimeType,
        );
        _error = null;
      });
    } catch (_) {
      if (mounted) setState(() => _error = 'Không thể đọc tệp đã chọn.');
    }
  }

  String _mimeFromExtension(String? extension) {
    switch ((extension ?? '').toLowerCase()) {
      case 'pdf':
        return 'application/pdf';
      case 'doc':
        return 'application/msword';
      case 'docx':
        return 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
      case 'png':
        return 'image/png';
      default:
        return 'image/jpeg';
    }
  }

  Future<void> _pickDate(TextEditingController controller) async {
    final now = DateTime.now();
    final current = DateTime.tryParse(controller.text.trim());
    final isExpiry = identical(controller, _expiryDate);
    final issued = DateTime.tryParse(_issuedDate.text.trim());
    final firstDate = isExpiry
        ? (issued?.add(const Duration(days: 1)) ?? now)
        : DateTime(1950);
    final initial = current ?? (isExpiry ? firstDate : now);
    final selected = await showDatePicker(
      context: context,
      initialDate: initial.isBefore(firstDate) ? firstDate : initial,
      firstDate: firstDate,
      lastDate: isExpiry ? DateTime(now.year + 80) : now,
    );
    if (selected != null) {
      controller.text = DateFormat('yyyy-MM-dd').format(selected);
      setState(() => _error = null);
    }
  }

  String? _finalIssuer() {
    if (_issuer == 'OTHER') return _customIssuer.text.trim();
    return _issuer?.trim();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    if (_file == null) {
      setState(() => _error = 'Vui lòng chọn tệp giấy tờ chuyên môn.');
      return;
    }

    final issued = DateTime.tryParse(_issuedDate.text.trim());
    final expiry = DateTime.tryParse(_expiryDate.text.trim());
    final today = DateUtils.dateOnly(DateTime.now());
    if (issued == null) {
      setState(() => _error = 'Ngày cấp không hợp lệ.');
      return;
    }
    if (issued.isAfter(today)) {
      setState(() => _error = 'Ngày cấp không được vượt quá ngày hiện tại.');
      return;
    }
    if (!_isDegree && expiry == null) {
      setState(() => _error = 'Loại chứng chỉ này yêu cầu ngày hết hạn.');
      return;
    }
    if (expiry != null) {
      if (!expiry.isAfter(issued)) {
        setState(() => _error = 'Ngày hết hạn phải lớn hơn ngày cấp.');
        return;
      }
      if (!expiry.isAfter(today)) {
        setState(
          () => _error = 'Ngày hết hạn phải là một ngày trong tương lai.',
        );
        return;
      }
    }

    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      await (widget.service ?? ExpertOnboardingService.instance)
          .submitCredential(
            credentialType: _type,
            credentialNumber: _number.text.trim(),
            issuer: _finalIssuer()!,
            issuedDate: _issuedDate.text.trim(),
            expiryDate: _isDegree ? null : _expiryDate.text.trim(),
            file: _file!,
          );
      ExpertOnboardingStore.instance.invalidate();
      if (mounted) context.go('/expert-onboarding');
    } on ArgumentError catch (e) {
      if (mounted) setState(() => _error = e.message.toString());
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _error = switch (e.statusCode) {
          413 => 'Tệp vượt quá giới hạn 10 MB.',
          409 => 'Giấy tờ này đã được gửi hoặc đang được xét duyệt.',
          400 => 'Thông tin giấy tờ không hợp lệ. Vui lòng kiểm tra lại.',
          _ => 'Không thể gửi giấy tờ. Vui lòng thử lại.',
        };
      });
    } catch (_) {
      if (mounted) setState(() => _error = 'Không thể kết nối đến máy chủ.');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  String? _required(String? value) =>
      (value ?? '').trim().isEmpty ? 'Thông tin này là bắt buộc' : null;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF7F1ED),
      appBar: AppBar(
        title: const Text('Giấy tờ chuyên môn'),
        backgroundColor: Colors.transparent,
        foregroundColor: const Color(0xFF2C221E),
        surfaceTintColor: Colors.transparent,
      ),
      body: SafeArea(
        child: Form(
          key: _formKey,
          child: ListView(
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 32),
            children: [
              const Text(
                'Bước 3/3',
                style: TextStyle(
                  color: Color(0xFFC06F5A),
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 6),
              const Text(
                'Chứng minh chuyên môn',
                style: TextStyle(
                  fontSize: 28,
                  fontWeight: FontWeight.w800,
                  color: Color(0xFF5A463F),
                ),
              ),
              const SizedBox(height: 8),
              const Text(
                'Cần ít nhất một giấy phép, bằng cấp hoặc chứng chỉ hợp lệ trước khi được duyệt.',
                style: TextStyle(height: 1.5, color: Color(0xFF75635C)),
              ),
              const SizedBox(height: 22),
              DropdownButtonFormField<String>(
                initialValue: _type,
                decoration: _decoration('Loại giấy tờ *'),
                items: const [
                  DropdownMenuItem(
                    value: 'MEDICAL_LICENSE',
                    child: Text('Giấy phép hành nghề y'),
                  ),
                  DropdownMenuItem(value: 'DEGREE', child: Text('Bằng cấp')),
                  DropdownMenuItem(
                    value: 'CERTIFICATE',
                    child: Text('Chứng chỉ'),
                  ),
                  DropdownMenuItem(
                    value: 'PROFESSIONAL_LICENSE',
                    child: Text('Giấy phép chuyên môn'),
                  ),
                  DropdownMenuItem(
                    value: 'IDENTITY_DOCUMENT',
                    child: Text('Giấy tờ định danh y tế bổ sung'),
                  ),
                ],
                onChanged: (value) {
                  setState(() {
                    _type = value!;
                    if (_isDegree) _expiryDate.clear();
                    _error = null;
                  });
                },
              ),
              const SizedBox(height: 14),
              TextFormField(
                controller: _number,
                validator: _required,
                decoration: _decoration('Số hiệu giấy tờ *'),
              ),
              const SizedBox(height: 14),
              DropdownButtonFormField<String>(
                key: ValueKey('issuer-$_issuer'),
                initialValue: _issuer,
                decoration: _decoration('Đơn vị cấp *'),
                items: [
                  ..._commonIssuers.map(
                    (issuer) => DropdownMenuItem(
                      value: issuer,
                      child: Text(issuer, overflow: TextOverflow.ellipsis),
                    ),
                  ),
                  const DropdownMenuItem(value: 'OTHER', child: Text('Khác')),
                ],
                validator: (value) =>
                    value == null ? 'Thông tin này là bắt buộc' : null,
                onChanged: (value) => setState(() {
                  _issuer = value;
                  _error = null;
                }),
              ),
              if (_issuer == 'OTHER') ...[
                const SizedBox(height: 14),
                TextFormField(
                  controller: _customIssuer,
                  validator: _required,
                  decoration: _decoration('Tên đơn vị cấp *'),
                ),
              ],
              const SizedBox(height: 14),
              _dateField(_issuedDate, 'Ngày cấp *'),
              const SizedBox(height: 14),
              if (_isDegree)
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: const Text(
                    'Bằng cấp chuyên môn không có thời hạn.',
                    style: TextStyle(
                      color: Color(0xFF75635C),
                      fontStyle: FontStyle.italic,
                    ),
                  ),
                )
              else
                _dateField(_expiryDate, 'Ngày hết hạn *'),
              const SizedBox(height: 18),
              _fileCard(),
              if (_error != null) ...[
                const SizedBox(height: 14),
                Text(_error!, style: const TextStyle(color: Color(0xFF93000A))),
              ],
              const SizedBox(height: 20),
              SizedBox(
                height: 54,
                child: FilledButton(
                  onPressed: _loading ? null : _submit,
                  style: FilledButton.styleFrom(
                    backgroundColor: const Color(0xFFC98C7B),
                    shape: const StadiumBorder(),
                  ),
                  child: _loading
                      ? const CircularProgressIndicator(color: Colors.white)
                      : const Text(
                          'Gửi hồ sơ xét duyệt',
                          style: TextStyle(fontWeight: FontWeight.w700),
                        ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _dateField(TextEditingController controller, String label) =>
      TextFormField(
        controller: controller,
        readOnly: true,
        validator: _required,
        onTap: () => _pickDate(controller),
        decoration: _decoration(
          label,
        ).copyWith(suffixIcon: const Icon(Icons.calendar_today_outlined)),
      );

  Widget _fileCard() => Container(
    padding: const EdgeInsets.all(16),
    decoration: BoxDecoration(
      color: Colors.white,
      borderRadius: BorderRadius.circular(20),
      border: Border.all(color: const Color(0xFFE8DDD6)),
    ),
    child: Column(
      children: [
        if (_file == null)
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 18),
            child: Icon(
              Icons.workspace_premium_outlined,
              size: 54,
              color: Color(0xFFC98C7B),
            ),
          )
        else if (_isImage)
          ClipRRect(
            borderRadius: BorderRadius.circular(14),
            child: Image.memory(
              Uint8List.fromList(_file!.bytes),
              height: 180,
              width: double.infinity,
              fit: BoxFit.cover,
            ),
          )
        else
          ListTile(
            leading: const Icon(
              Icons.description_outlined,
              size: 42,
              color: Color(0xFFC98C7B),
            ),
            title: Text(
              _file!.fileName,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
            subtitle: Text(
              '${_file!.mimeType} • ${(_file!.bytes.length / 1024).toStringAsFixed(1)} KB',
            ),
          ),
        const SizedBox(height: 12),
        SizedBox(
          width: double.infinity,
          child: OutlinedButton.icon(
            onPressed: _pickDocument,
            icon: const Icon(Icons.attach_file_rounded),
            label: Text(_file == null ? 'Chọn tệp' : 'Chọn tệp khác'),
          ),
        ),
        Row(
          children: [
            Expanded(
              child: TextButton.icon(
                onPressed: () => _pickImage(ImageSource.camera),
                icon: const Icon(Icons.camera_alt_outlined),
                label: const Text('Chụp ảnh'),
              ),
            ),
            Expanded(
              child: TextButton.icon(
                onPressed: () => _pickImage(ImageSource.gallery),
                icon: const Icon(Icons.photo_library_outlined),
                label: const Text('Thư viện'),
              ),
            ),
          ],
        ),
        const Text(
          'PDF, DOC, DOCX, JPEG hoặc PNG; tối đa 10 MB.',
          style: TextStyle(fontSize: 12, color: Color(0xFF75635C)),
        ),
      ],
    ),
  );

  InputDecoration _decoration(String label) => InputDecoration(
    labelText: label,
    filled: true,
    fillColor: Colors.white,
    border: OutlineInputBorder(borderRadius: BorderRadius.circular(16)),
  );
}
