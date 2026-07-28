import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:mime/mime.dart';

import '../services/health_record_service.dart';

class AddHealthRecordScreen extends StatefulWidget {
  const AddHealthRecordScreen({super.key});

  @override
  State<AddHealthRecordScreen> createState() => _AddHealthRecordScreenState();
}

class _AddHealthRecordScreenState extends State<AddHealthRecordScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Color(0xFFF2EAE4);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  static const _recordTypes = [
    ('ULTRASOUND', 'Siêu âm'),
    ('LAB_RESULT', 'Kết quả xét nghiệm'),
    ('PRESCRIPTION', 'Đơn thuốc'),
    ('VACCINATION_FORM', 'Phiếu tiêm chủng'),
    ('EXAMINATION_RESULT', 'Kết quả khám'),
    ('NOTE', 'Ghi chú'),
  ];

  final _service = HealthRecordService();
  final _titleCtrl = TextEditingController();
  final _facilityCtrl = TextEditingController();

  String _recordType = 'ULTRASOUND';
  DateTime _recordDate = DateTime.now();
  PlatformFile? _selectedFile;
  bool _saving = false;

  @override
  void dispose() {
    _titleCtrl.dispose();
    _facilityCtrl.dispose();
    super.dispose();
  }

  Future<void> _pickDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _recordDate,
      firstDate: DateTime(2000),
      lastDate: DateTime.now(),
      builder: (ctx, child) => Theme(
        data: Theme.of(ctx).copyWith(
          colorScheme: const ColorScheme.light(
            primary: _primary,
            onPrimary: Colors.white,
            surface: _canvas,
          ),
        ),
        child: child!,
      ),
    );
    if (picked != null) setState(() => _recordDate = picked);
  }

  Future<void> _pickFile() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: ['jpg', 'jpeg', 'png', 'webp', 'heic', 'gif', 'pdf'],
      allowMultiple: false,
      withData: true,
    );
    if (result == null || result.files.isEmpty) return;
    setState(() => _selectedFile = result.files.single);
  }

  String _mimeTypeFor(PlatformFile file) {
    return lookupMimeType(file.name, headerBytes: file.bytes) ??
        (file.extension?.toLowerCase() == 'pdf'
            ? 'application/pdf'
            : 'application/octet-stream');
  }

  Future<void> _save() async {
    final title = _titleCtrl.text.trim();
    final file = _selectedFile;
    if (title.isEmpty) {
      _showSnack('Vui lòng nhập tiêu đề hồ sơ.');
      return;
    }
    if (file == null || file.bytes == null) {
      _showSnack('Vui lòng chọn ảnh hoặc PDF.');
      return;
    }

    setState(() => _saving = true);
    try {
      final fileId = await _service.uploadHealthRecordAttachment(
        bytes: file.bytes!,
        fileName: file.name,
        mimeType: _mimeTypeFor(file),
      );
      await _service.addHealthRecord(
        recordType: _recordType,
        title: title,
        recordDate: _recordDate,
        facilityName: _facilityCtrl.text,
        fileIds: [fileId],
      );
      if (!mounted) return;
      Navigator.of(context).pop(true);
    } catch (e) {
      if (mounted) _showSnack('Không thể lưu hồ sơ. ${_friendlyError(e)}');
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  String _friendlyError(Object error) {
    final text = error.toString();
    if (text.contains('401') || text.contains('403')) {
      return 'Vui lòng đăng nhập lại hoặc kiểm tra quyền tài khoản.';
    }
    if (text.contains('413')) return 'Tệp vượt quá dung lượng cho phép.';
    if (text.contains('415') || text.contains('FILE-001')) {
      return 'Chỉ hỗ trợ ảnh hoặc PDF.';
    }
    return 'Vui lòng thử lại.';
  }

  void _showSnack(String message) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message), backgroundColor: _primary));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      appBar: AppBar(
        backgroundColor: _canvas,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded, color: _onSurface),
          onPressed: () => Navigator.of(context).pop(false),
        ),
        title: const Text(
          'Thêm hồ sơ sức khỏe',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 18,
            fontWeight: FontWeight.w700,
            color: _onSurface,
          ),
        ),
      ),
      body: Column(
        children: [
          Expanded(
            child: SingleChildScrollView(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  _buildFileCard(),
                  const SizedBox(height: 16),
                  _buildFormCard(),
                ],
              ),
            ),
          ),
          _buildBottomBar(),
        ],
      ),
    );
  }

  Widget _buildFileCard() {
    final file = _selectedFile;
    final isPdf = file?.extension?.toLowerCase() == 'pdf';
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: _cardDecoration(),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Tệp đính kèm',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 12),
          InkWell(
            onTap: _saving ? null : _pickFile,
            borderRadius: BorderRadius.circular(18),
            child: Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: _surface,
                borderRadius: BorderRadius.circular(18),
                border: Border.all(color: const Color(0xFFD6C2BD)),
              ),
              child: Row(
                children: [
                  Container(
                    width: 48,
                    height: 48,
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Icon(
                      file == null
                          ? Icons.upload_file_rounded
                          : isPdf
                          ? Icons.picture_as_pdf_rounded
                          : Icons.image_rounded,
                      color: _primaryContainer,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          file?.name ?? 'Chọn ảnh hoặc PDF',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 14,
                            fontWeight: FontWeight.w600,
                            color: _onSurface,
                          ),
                        ),
                        const SizedBox(height: 4),
                        const Text(
                          'JPG, PNG, WebP, HEIC, GIF, PDF',
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 11,
                            color: _onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const Icon(
                    Icons.chevron_right_rounded,
                    color: _onSurfaceVariant,
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFormCard() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: _cardDecoration(),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Thông tin hồ sơ',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _titleCtrl,
            maxLength: 255,
            decoration: _inputDecoration('Tiêu đề *'),
          ),
          const SizedBox(height: 12),
          _buildTypeDropdown(),
          const SizedBox(height: 12),
          _buildDateField(),
          const SizedBox(height: 12),
          TextField(
            controller: _facilityCtrl,
            decoration: _inputDecoration(
              'Cơ sở y tế',
              hint: 'Bệnh viện, phòng khám...',
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTypeDropdown() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
      decoration: _fieldDecoration(),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<String>(
          value: _recordType,
          isExpanded: true,
          borderRadius: BorderRadius.circular(16),
          dropdownColor: Colors.white,
          items: _recordTypes
              .map(
                (pair) =>
                    DropdownMenuItem(value: pair.$1, child: Text(pair.$2)),
              )
              .toList(),
          onChanged: _saving
              ? null
              : (value) {
                  if (value != null) setState(() => _recordType = value);
                },
        ),
      ),
    );
  }

  Widget _buildDateField() {
    return InkWell(
      onTap: _saving ? null : _pickDate,
      borderRadius: BorderRadius.circular(16),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 16),
        decoration: _fieldDecoration(),
        child: Row(
          children: [
            const Icon(
              Icons.calendar_today_rounded,
              color: _primaryContainer,
              size: 18,
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Text(
                '${_recordDate.day.toString().padLeft(2, '0')}/${_recordDate.month.toString().padLeft(2, '0')}/${_recordDate.year}',
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                  color: _onSurface,
                ),
              ),
            ),
            const Icon(Icons.chevron_right_rounded, color: _onSurfaceVariant),
          ],
        ),
      ),
    );
  }

  Widget _buildBottomBar() {
    return SafeArea(
      top: false,
      child: Container(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 16),
        decoration: BoxDecoration(
          color: _canvas,
          boxShadow: [
            BoxShadow(
              color: _primary.withAlpha(20),
              blurRadius: 12,
              offset: const Offset(0, -4),
            ),
          ],
        ),
        child: Row(
          children: [
            Expanded(
              child: OutlinedButton(
                onPressed: _saving
                    ? null
                    : () => Navigator.of(context).pop(false),
                style: OutlinedButton.styleFrom(
                  foregroundColor: _onSurfaceVariant,
                  backgroundColor: _surface,
                  side: BorderSide.none,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: const StadiumBorder(),
                ),
                child: const Text('Hủy'),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              flex: 2,
              child: ElevatedButton.icon(
                onPressed: _saving ? null : _save,
                icon: _saving
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Colors.white,
                        ),
                      )
                    : const Icon(Icons.check_rounded),
                label: Text(_saving ? 'Đang lưu...' : 'Lưu hồ sơ'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: _primary,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: const StadiumBorder(),
                  elevation: 0,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  BoxDecoration _cardDecoration() => BoxDecoration(
    color: Colors.white,
    borderRadius: BorderRadius.circular(24),
    boxShadow: [
      BoxShadow(
        color: _primary.withAlpha(15),
        blurRadius: 12,
        offset: const Offset(0, 4),
      ),
    ],
  );

  BoxDecoration _fieldDecoration() => BoxDecoration(
    color: Colors.white,
    borderRadius: BorderRadius.circular(16),
    border: Border.all(color: _surface, width: 2),
  );

  InputDecoration _inputDecoration(String label, {String? hint}) =>
      InputDecoration(
        labelText: label,
        hintText: hint,
        counterText: '',
        filled: true,
        fillColor: Colors.white,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(color: _surface, width: 2),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(color: _surface, width: 2),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(color: _primaryContainer, width: 2),
        ),
      );
}
