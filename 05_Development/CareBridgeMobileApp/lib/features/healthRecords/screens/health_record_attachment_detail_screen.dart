import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:mime/mime.dart';

import '../../fileManager/screens/file_viewer_screen.dart';
import '../models/health_record_model.dart';
import '../services/health_record_service.dart';

class HealthRecordAttachmentDetailScreen extends StatefulWidget {
  const HealthRecordAttachmentDetailScreen({
    super.key,
    required this.record,
    this.attachment,
    this.readOnly = false,
  });

  final HealthRecordDetail record;
  final FileAttachment? attachment;
  final bool readOnly;

  @override
  State<HealthRecordAttachmentDetailScreen> createState() =>
      _HealthRecordAttachmentDetailScreenState();
}

class _HealthRecordAttachmentDetailScreenState
    extends State<HealthRecordAttachmentDetailScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Color(0xFFFFFFFF);
  static const _surfaceVariant = Color(0xFFFFF1EC);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  static const _recordTypes = [
    ('ULTRASOUND', 'Siêu âm'),
    ('LAB_RESULT', 'Kết quả xét nghiệm'),
    ('PRESCRIPTION', 'Đơn thuốc'),
    ('VACCINATION_FORM', 'Tiêm chủng'),
    ('EXAMINATION_RESULT', 'Kết quả khám'),
    ('NOTE', 'Ghi chú'),
  ];

  final _service = HealthRecordService();
  late final TextEditingController _titleCtrl;
  late final TextEditingController _facilityCtrl;

  late String _recordType;
  late DateTime _recordDate;
  late List<FileAttachment> _existingAttachments;
  final List<PlatformFile> _newFiles = [];

  bool _saving = false;
  bool _deleting = false;

  @override
  void initState() {
    super.initState();
    _titleCtrl = TextEditingController(text: widget.record.title);
    _facilityCtrl =
        TextEditingController(text: widget.record.facilityName ?? '');
    _recordType = widget.record.recordType.apiValue;
    _recordDate = widget.record.recordDate;
    _existingAttachments = List.from(widget.record.attachments);
  }

  @override
  void dispose() {
    _titleCtrl.dispose();
    _facilityCtrl.dispose();
    super.dispose();
  }

  String _formatDate(DateTime date) {
    final day = date.day.toString().padLeft(2, '0');
    final month = date.month.toString().padLeft(2, '0');
    final year = date.year.toString();
    return '$day/$month/$year';
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

  Future<void> _pickNewFiles() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: ['jpg', 'jpeg', 'png', 'webp', 'heic', 'gif', 'pdf'],
      allowMultiple: true,
      withData: true,
    );
    if (result == null || result.files.isEmpty) return;
    setState(() {
      for (final f in result.files) {
        if (!_newFiles.any((existing) => existing.name == f.name)) {
          _newFiles.add(f);
        }
      }
    });
  }

  String _mimeTypeFor(PlatformFile file) {
    return lookupMimeType(file.name, headerBytes: file.bytes) ??
        (file.extension?.toLowerCase() == 'pdf'
            ? 'application/pdf'
            : 'application/octet-stream');
  }

  void _openFile(FileAttachment attachment) {
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => FileViewerScreen(
          fileId: attachment.fileId,
          fileName: attachment.originalName,
          showDeleteAction: false,
          showBottomActions: false,
        ),
      ),
    );
  }

  Future<void> _saveChanges() async {
    final title = _titleCtrl.text.trim();
    if (title.isEmpty) {
      _showSnack('Vui lòng nhập tiêu đề hồ sơ.');
      return;
    }

    setState(() => _saving = true);
    try {
      final List<String> fileIds =
          _existingAttachments.map((a) => a.fileId).toList();

      for (final file in _newFiles) {
        if (file.bytes == null) continue;
        final fileId = await _service.uploadHealthRecordAttachment(
          bytes: file.bytes!,
          fileName: file.name,
          mimeType: _mimeTypeFor(file),
        );
        fileIds.add(fileId);
      }

      final request = UpdateHealthRecordRequest(
        title: title,
        recordType: _recordType,
        recordDate: _recordDate,
        facilityName: _facilityCtrl.text,
        fileIds: fileIds,
      );

      await _service.updateHealthRecord(widget.record.id, request);
      if (!mounted) return;
      _showSnack('Đã cập nhật hồ sơ sức khỏe.');
      Navigator.of(context).pop(true);
    } catch (e) {
      if (mounted) _showSnack('Không thể lưu thay đổi. ${e.toString()}');
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  Future<void> _confirmDeleteRecord() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: Colors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: const Text(
          'Xóa hồ sơ sức khỏe',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 18,
            fontWeight: FontWeight.w700,
            color: _onSurface,
          ),
        ),
        content: const Text(
          'Bạn có chắc chắn muốn xóa hồ sơ này không? Hành động này không thể hoàn tác.',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 14,
            color: _onSurfaceVariant,
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text(
              'Hủy',
              style: TextStyle(fontFamily: 'Lexend', color: _onSurfaceVariant),
            ),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFFBA1A1A),
              foregroundColor: Colors.white,
              shape: const StadiumBorder(),
            ),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text(
              'Xóa',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
        ],
      ),
    );

    if (confirmed != true) return;

    setState(() => _deleting = true);
    try {
      await _service.archiveHealthRecord(widget.record.id);
      if (!mounted) return;
      _showSnack('Đã xóa hồ sơ sức khỏe.');
      Navigator.of(context).pop(true);
    } catch (e) {
      if (mounted) _showSnack('Không thể xóa hồ sơ. ${e.toString()}');
    } finally {
      if (mounted) setState(() => _deleting = false);
    }
  }

  void _showSnack(String message) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message), backgroundColor: _primary));
  }

  @override
  Widget build(BuildContext context) {
    final totalFileCount = _existingAttachments.length + _newFiles.length;

    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: Column(
          children: [
            _buildAppBar(),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(20, 12, 20, 28),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _buildEditableFormCard(),
                    const SizedBox(height: 20),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(
                          'Tệp đính kèm ($totalFileCount)',
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 16,
                            fontWeight: FontWeight.w700,
                            color: _onSurface,
                          ),
                        ),
                        if (!widget.readOnly)
                          TextButton.icon(
                            onPressed: _saving || _deleting ? null : _pickNewFiles,
                            icon: const Icon(
                              Icons.add_circle_outline_rounded,
                              size: 18,
                              color: _primary,
                            ),
                            label: const Text(
                              'Thêm tệp',
                              style: TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 13,
                                fontWeight: FontWeight.w600,
                                color: _primary,
                              ),
                            ),
                          ),
                      ],
                    ),
                    const SizedBox(height: 10),
                    if (totalFileCount == 0)
                      _buildEmptyAttachmentsView()
                    else ...[
                      // List of existing attachments
                      ..._existingAttachments.asMap().entries.map((entry) {
                        final index = entry.key;
                        final attachment = entry.value;
                        return _ExistingAttachmentTile(
                          attachment: attachment,
                          onOpen: () => _openFile(attachment),
                          onDelete: widget.readOnly || _saving || _deleting
                              ? null
                              : () {
                                  setState(() {
                                    _existingAttachments.removeAt(index);
                                  });
                                },
                        );
                      }),
                      // List of new files to be uploaded
                      ..._newFiles.asMap().entries.map((entry) {
                        final index = entry.key;
                        final file = entry.value;
                        return _NewFileTile(
                          file: file,
                          onDelete: widget.readOnly || _saving || _deleting
                              ? null
                              : () {
                                  setState(() {
                                    _newFiles.removeAt(index);
                                  });
                                },
                        );
                      }),
                    ],
                  ],
                ),
              ),
            ),
            _buildBottomBar(),
          ],
        ),
      ),
    );
  }

  Widget _buildAppBar() {
    return SizedBox(
      height: 52,
      child: Row(
        children: [
          IconButton(
            onPressed: () => Navigator.of(context).pop(false),
            icon: const Icon(Icons.arrow_back, color: _onSurfaceVariant),
          ),
          const Expanded(
            child: Text(
              'Chi tiết tệp hồ sơ',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 20,
                fontWeight: FontWeight.w700,
                color: _primary,
              ),
            ),
          ),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildEditableFormCard() {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF5A463F).withAlpha(13),
            blurRadius: 18,
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
              fontSize: 16,
              fontWeight: FontWeight.w700,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _titleCtrl,
            enabled: !widget.readOnly && !_saving && !_deleting,
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
            enabled: !widget.readOnly && !_saving && !_deleting,
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
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFFE8DAD6), width: 1.5),
      ),
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
          onChanged: widget.readOnly || _saving || _deleting
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
      onTap: widget.readOnly || _saving || _deleting ? null : _pickDate,
      borderRadius: BorderRadius.circular(16),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 16),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: const Color(0xFFE8DAD6), width: 1.5),
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
              child: Text(
                _formatDate(_recordDate),
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                  color: _onSurface,
                ),
              ),
            ),
            if (!widget.readOnly)
              const Icon(Icons.chevron_right_rounded, color: _onSurfaceVariant),
          ],
        ),
      ),
    );
  }

  Widget _buildEmptyAttachmentsView() {
    return InkWell(
      onTap: widget.readOnly || _saving || _deleting ? null : _pickNewFiles,
      borderRadius: BorderRadius.circular(18),
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: _surfaceVariant,
          borderRadius: BorderRadius.circular(18),
          border: Border.all(color: const Color(0xFFE8DAD6)),
        ),
        child: Row(
          children: [
            const Icon(Icons.upload_file_rounded, color: _primaryContainer, size: 36),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Chưa có tệp đính kèm',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      color: _onSurface,
                    ),
                  ),
                  if (!widget.readOnly) ...[
                    const SizedBox(height: 2),
                    const Text(
                      'Bấm vào đây để chọn thêm tệp (ảnh hoặc PDF)',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 11,
                        color: _onSurfaceVariant,
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildBottomBar() {
    if (widget.readOnly) return const SizedBox.shrink();

    return Container(
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
            child: OutlinedButton.icon(
              onPressed: _saving || _deleting ? null : _confirmDeleteRecord,
              style: OutlinedButton.styleFrom(
                foregroundColor: const Color(0xFFBA1A1A),
                side: const BorderSide(color: Color(0xFFBA1A1A), width: 1.5),
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: const StadiumBorder(),
              ),
              icon: _deleting
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Color(0xFFBA1A1A),
                      ),
                    )
                  : const Icon(Icons.delete_outline_rounded, size: 20),
              label: Text(
                _deleting ? 'Đang xóa...' : 'Xóa hồ sơ',
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            flex: 2,
            child: ElevatedButton.icon(
              onPressed: _saving || _deleting ? null : _saveChanges,
              icon: _saving
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : const Icon(Icons.save_rounded, size: 20),
              label: Text(
                _saving ? 'Đang lưu...' : 'Lưu thay đổi',
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontWeight: FontWeight.bold,
                ),
              ),
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
    );
  }

  InputDecoration _inputDecoration(String label, {String? hint}) =>
      InputDecoration(
        labelText: label,
        hintText: hint,
        counterText: '',
        filled: true,
        fillColor: Colors.white,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(color: Color(0xFFE8DAD6), width: 1.5),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(color: Color(0xFFE8DAD6), width: 1.5),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(color: _primaryContainer, width: 2),
        ),
      );
}

class _ExistingAttachmentTile extends StatelessWidget {
  const _ExistingAttachmentTile({
    required this.attachment,
    required this.onOpen,
    required this.onDelete,
  });

  final FileAttachment attachment;
  final VoidCallback onOpen;
  final VoidCallback? onDelete;

  bool get _isPdf => attachment.isPdf;

  String get _attachmentType {
    if (_isPdf) return 'PDF';
    if (attachment.mimeType?.startsWith('image/') == true) return 'Ảnh';
    return attachment.mimeType ?? 'Tệp đính kèm';
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: const Color(0xFFE8DAD6)),
      ),
      child: Row(
        children: [
          Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(
              color: const Color(0xFFFFF1EC),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(
              _isPdf ? Icons.picture_as_pdf_rounded : Icons.image_rounded,
              color: _isPdf
                  ? const Color(0xFFBA1A1A)
                  : const Color(0xFFC98C7B),
              size: 24,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  attachment.originalName.isEmpty
                      ? 'Tệp đính kèm'
                      : attachment.originalName,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    fontWeight: FontWeight.w700,
                    color: Color(0xFF271812),
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  _attachmentType,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    color: Color(0xFF524440),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 6),
          // View Detail / Open file Button
          IconButton.filledTonal(
            tooltip: 'Xem tệp',
            onPressed: onOpen,
            icon: const Icon(Icons.visibility_rounded, size: 20),
            style: IconButton.styleFrom(
              foregroundColor: const Color(0xFF845143),
              backgroundColor: const Color(0xFFFFE2D9),
            ),
          ),
          if (onDelete != null) ...[
            const SizedBox(width: 4),
            // Delete file button
            IconButton.filledTonal(
              tooltip: 'Xóa tệp',
              onPressed: onDelete,
              icon: const Icon(Icons.delete_outline_rounded, size: 20),
              style: IconButton.styleFrom(
                foregroundColor: const Color(0xFFBA1A1A),
                backgroundColor: const Color(0xFFFFDAD6),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _NewFileTile extends StatelessWidget {
  const _NewFileTile({required this.file, required this.onDelete});

  final PlatformFile file;
  final VoidCallback? onDelete;

  bool get _isPdf => file.extension?.toLowerCase() == 'pdf';

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF1EC),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: const Color(0xFFE5D5D0)),
      ),
      child: Row(
        children: [
          Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(
              _isPdf ? Icons.picture_as_pdf_rounded : Icons.image_rounded,
              color: _isPdf
                  ? const Color(0xFFBA1A1A)
                  : const Color(0xFFC98C7B),
              size: 24,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Flexible(
                      child: Text(
                        file.name,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 14,
                          fontWeight: FontWeight.w700,
                          color: Color(0xFF271812),
                        ),
                      ),
                    ),
                    const SizedBox(width: 6),
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 6,
                        vertical: 2,
                      ),
                      decoration: BoxDecoration(
                        color: const Color(0xFFC98C7B),
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: const Text(
                        'Mới',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 10,
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                        ),
                      ),
                    ),
                  ],
                ),
                if (file.size > 0) ...[
                  const SizedBox(height: 3),
                  Text(
                    '${(file.size / 1024).toStringAsFixed(1)} KB',
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      color: Color(0xFF524440),
                    ),
                  ),
                ],
              ],
            ),
          ),
          const SizedBox(width: 6),
          if (onDelete != null)
            IconButton.filledTonal(
              tooltip: 'Xóa tệp mới',
              onPressed: onDelete,
              icon: const Icon(Icons.close_rounded, size: 20),
              style: IconButton.styleFrom(
                foregroundColor: const Color(0xFFBA1A1A),
                backgroundColor: const Color(0xFFFFDAD6),
              ),
            ),
        ],
      ),
    );
  }
}
