import 'package:flutter/material.dart';
import '../models/health_record_model.dart';
import '../services/health_record_service.dart';

class EditHealthRecordScreen extends StatefulWidget {
  final String recordId;

  const EditHealthRecordScreen({super.key, required this.recordId});

  @override
  State<EditHealthRecordScreen> createState() => _EditHealthRecordScreenState();
}

class _EditHealthRecordScreenState extends State<EditHealthRecordScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Color(0xFFF2EAE4);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  final _service = HealthRecordService();
  final _titleCtrl = TextEditingController();
  final _sourceNameCtrl = TextEditingController();

  HealthRecordDetail? _record;
  String _recordType = 'EXAMINATION_RESULT';
  DateTime _recordDate = DateTime.now();
  bool _subjectIsMother = true;

  bool _isLoading = true;
  bool _isSaving = false;
  bool _showSuccess = false;
  String? _error;

  static const _recordTypes = [
    ('ULTRASOUND', 'Siêu âm'),
    ('LAB_RESULT', 'Kết quả xét nghiệm'),
    ('PRESCRIPTION', 'Đơn thuốc'),
    ('VACCINATION_FORM', 'Phiếu tiêm chủng'),
    ('EXAMINATION_RESULT', 'Kết quả khám'),
    ('NOTE', 'Ghi chú'),
  ];

  @override
  void initState() {
    super.initState();
    _loadRecord();
  }

  @override
  void dispose() {
    _titleCtrl.dispose();
    _sourceNameCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadRecord() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final record = await _service.getHealthRecord(widget.recordId);
      setState(() {
        _record = record;
        _titleCtrl.text = record.title;
        _recordType = _typeToApiValue(record.recordType);
        _recordDate = record.recordDate;
        _sourceNameCtrl.text = record.facilityName ?? '';
      });
    } catch (_) {
      setState(() => _error = 'Không thể tải hồ sơ. Vui lòng thử lại.');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  String _typeToApiValue(RecordType t) {
    switch (t) {
      case RecordType.vaccination:
        return 'VACCINATION_FORM';
      case RecordType.metric:
        return 'LAB_RESULT';
      case RecordType.prescription:
        return 'PRESCRIPTION';
      case RecordType.checkup:
        return 'EXAMINATION_RESULT';
      default:
        return 'NOTE';
    }
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

  Future<void> _save() async {
    if (_titleCtrl.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Vui lòng nhập tiêu đề hồ sơ.'),
          backgroundColor: _primary,
        ),
      );
      return;
    }
    setState(() => _isSaving = true);
    try {
      await _service.updateHealthRecord(
        widget.recordId,
        UpdateHealthRecordRequest(
          title: _titleCtrl.text.trim(),
          recordType: _recordType,
          recordDate: _recordDate,
          sourceName: _sourceNameCtrl.text.trim().isEmpty
              ? null
              : _sourceNameCtrl.text.trim(),
          sourceType: _subjectIsMother ? 'MOTHER_SELF' : 'BABY',
        ),
      );
      setState(() => _showSuccess = true);
      await Future.delayed(const Duration(seconds: 2));
      if (mounted) Navigator.of(context).pop(true);
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Không thể lưu. Vui lòng thử lại.'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _isSaving = false);
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
          icon: const Icon(Icons.arrow_back_rounded, color: _onSurface),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: const Text(
          'Chỉnh sửa hồ sơ',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 18,
            fontWeight: FontWeight.w700,
            color: _onSurface,
          ),
        ),
      ),
      body: Stack(
        children: [
          if (_isLoading)
            const Center(
              child: CircularProgressIndicator(color: _primaryContainer),
            )
          else if (_error != null)
            Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(
                    Icons.error_outline_rounded,
                    color: _primaryContainer,
                    size: 48,
                  ),
                  const SizedBox(height: 12),
                  Text(
                    _error!,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 13,
                      color: _onSurfaceVariant,
                    ),
                  ),
                  const SizedBox(height: 12),
                  TextButton(
                    onPressed: _loadRecord,
                    child: const Text(
                      'Thử lại',
                      style: TextStyle(color: _primary),
                    ),
                  ),
                ],
              ),
            )
          else
            Column(
              children: [
                Expanded(
                  child: SingleChildScrollView(
                    padding: const EdgeInsets.fromLTRB(16, 12, 16, 100),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        _buildFilePreviewCard(),
                        const SizedBox(height: 16),
                        _buildOwnerInfoCard(),
                        const SizedBox(height: 16),
                        _buildFormCard(),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          if (_showSuccess) _buildSuccessToast(),
          _buildBottomBar(),
        ],
      ),
    );
  }

  Widget _buildFilePreviewCard() {
    final attachments = _record?.attachments ?? [];
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: _primary.withAlpha(15),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
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
          if (attachments.isEmpty)
            Container(
              height: 80,
              decoration: BoxDecoration(
                color: _surface,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(
                  color: _surface,
                  width: 2,
                  style: BorderStyle.solid,
                ),
              ),
              child: const Center(
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(
                      Icons.attach_file_rounded,
                      color: _onSurfaceVariant,
                      size: 18,
                    ),
                    SizedBox(width: 8),
                    Text(
                      'Chưa có tệp đính kèm',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        color: _onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
            )
          else
            ...attachments.map((a) => _buildAttachmentRow(a)),
          const SizedBox(height: 12),
          OutlinedButton.icon(
            onPressed: () {},
            icon: const Icon(Icons.upload_file_rounded, size: 18),
            label: Text(
              attachments.isEmpty ? 'Tải lên tệp' : 'Thay thế tệp',
              style: const TextStyle(fontFamily: 'Lexend', fontSize: 13),
            ),
            style: OutlinedButton.styleFrom(
              foregroundColor: _primary,
              side: const BorderSide(color: _primaryContainer, width: 1.5),
              shape: const StadiumBorder(),
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAttachmentRow(FileAttachment a) {
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(14),
      ),
      child: Row(
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(
              a.isPdf ? Icons.picture_as_pdf_rounded : Icons.image_rounded,
              color: _primaryContainer,
              size: 22,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              a.originalName,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 12,
                color: _onSurface,
              ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildOwnerInfoCard() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: _primary.withAlpha(15),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        children: [
          _buildInfoRow('Nguồn gốc', _record?.facilityName ?? '—'),
          const Divider(height: 20, color: Color(0xFFF0E8E5)),
          _buildInfoRow('Trạng thái', _record?.status ?? 'Hoạt động'),
          const Divider(height: 20, color: Color(0xFFF0E8E5)),
          _buildInfoRow('Đối tượng', _subjectIsMother ? 'Mẹ' : 'Bé'),
        ],
      ),
    );
  }

  Widget _buildInfoRow(String label, String value) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          label,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 12,
            color: _onSurfaceVariant,
          ),
        ),
        Text(
          value,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 12,
            fontWeight: FontWeight.w600,
            color: _onSurface,
          ),
        ),
      ],
    );
  }

  Widget _buildFormCard() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: _primary.withAlpha(15),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
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
          TextFormField(
            controller: _titleCtrl,
            maxLength: 255,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _onSurface,
            ),
            decoration: _inputDeco('Tiêu đề hồ sơ *'),
          ),
          const SizedBox(height: 14),
          _buildTypeDropdown(),
          const SizedBox(height: 14),
          _buildDateField(),
          const SizedBox(height: 14),
          _buildSubjectToggle(),
          const SizedBox(height: 14),
          TextFormField(
            controller: _sourceNameCtrl,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _onSurface,
            ),
            decoration: _inputDeco(
              'Cơ sở y tế / Nguồn',
              hint: 'Bệnh viện Từ Dũ...',
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTypeDropdown() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: _surface, width: 2),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Loại hồ sơ',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 11,
              color: _onSurfaceVariant,
            ),
          ),
          DropdownButtonHideUnderline(
            child: DropdownButton<String>(
              value: _recordType,
              isExpanded: true,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                color: _onSurface,
              ),
              dropdownColor: Colors.white,
              borderRadius: BorderRadius.circular(16),
              items: _recordTypes
                  .map(
                    (pair) =>
                        DropdownMenuItem(value: pair.$1, child: Text(pair.$2)),
                  )
                  .toList(),
              onChanged: (v) {
                if (v != null) setState(() => _recordType = v);
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDateField() {
    return GestureDetector(
      onTap: _pickDate,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: _surface, width: 2),
        ),
        child: Row(
          children: [
            const Icon(
              Icons.calendar_today_rounded,
              color: _primaryContainer,
              size: 18,
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Ngày',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 11,
                      color: _onSurfaceVariant,
                    ),
                  ),
                  Text(
                    '${_recordDate.day.toString().padLeft(2, '0')}/${_recordDate.month.toString().padLeft(2, '0')}/${_recordDate.year}',
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      color: _onSurface,
                    ),
                  ),
                ],
              ),
            ),
            const Icon(
              Icons.chevron_right_rounded,
              color: _onSurfaceVariant,
              size: 18,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSubjectToggle() {
    return Container(
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(50),
      ),
      child: Row(
        children: [
          Expanded(child: _buildSubjectOption('Mẹ', true)),
          Expanded(child: _buildSubjectOption('Bé', false)),
        ],
      ),
    );
  }

  Widget _buildSubjectOption(String label, bool isMother) {
    final selected = _subjectIsMother == isMother;
    return GestureDetector(
      onTap: () => setState(() => _subjectIsMother = isMother),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 180),
        padding: const EdgeInsets.symmetric(vertical: 10),
        decoration: BoxDecoration(
          color: selected ? _primary : Colors.transparent,
          borderRadius: BorderRadius.circular(50),
        ),
        alignment: Alignment.center,
        child: Text(
          label,
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 14,
            fontWeight: FontWeight.w600,
            color: selected ? Colors.white : _onSurfaceVariant,
          ),
        ),
      ),
    );
  }

  Widget _buildBottomBar() {
    return Positioned(
      left: 0,
      right: 0,
      bottom: 0,
      child: Container(
        padding: EdgeInsets.only(
          left: 16,
          right: 16,
          bottom: MediaQuery.of(context).padding.bottom + 16,
          top: 12,
        ),
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
                onPressed: () => Navigator.of(context).pop(),
                style: OutlinedButton.styleFrom(
                  foregroundColor: _onSurfaceVariant,
                  backgroundColor: _surface,
                  side: BorderSide.none,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: const StadiumBorder(),
                ),
                child: const Text(
                  'Hủy',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              flex: 2,
              child: ElevatedButton(
                onPressed: (_isSaving || _isLoading) ? null : _save,
                style: ElevatedButton.styleFrom(
                  backgroundColor: _primary,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: const StadiumBorder(),
                  elevation: 0,
                ),
                child: _isSaving
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(
                          strokeWidth: 2.5,
                          color: Colors.white,
                        ),
                      )
                    : const Text(
                        'Lưu hồ sơ',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 15,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSuccessToast() {
    return Positioned.fill(
      child: Container(
        color: Colors.black.withAlpha(100),
        child: Center(
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 20),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(24),
              boxShadow: [
                BoxShadow(color: Colors.black.withAlpha(40), blurRadius: 20),
              ],
            ),
            child: const Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(Icons.check_circle_rounded, color: _primary, size: 48),
                SizedBox(height: 12),
                Text(
                  'Đã cập nhật hồ sơ!',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                    color: _onSurface,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  InputDecoration _inputDeco(String label, {String? hint}) => InputDecoration(
    labelText: label,
    hintText: hint,
    labelStyle: const TextStyle(
      fontFamily: 'Lexend',
      fontSize: 12,
      color: _onSurfaceVariant,
    ),
    hintStyle: const TextStyle(
      fontFamily: 'Lexend',
      fontSize: 13,
      color: Color(0xFFBBA9A4),
    ),
    filled: true,
    fillColor: Colors.white,
    counterText: '',
    contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
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
