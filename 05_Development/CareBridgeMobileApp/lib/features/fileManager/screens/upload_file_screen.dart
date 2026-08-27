import 'package:universal_io/io.dart';
import 'package:flutter/material.dart';
import '../models/file_model.dart';
import '../services/file_service.dart';

/// CB-124 — Upload File (UC-167)
/// Drag-drop zone (simulated pick via file picker TODO), file preview list,
/// form fields: title, category, owner (Bé/Mẹ), visibility toggle, submit.
/// Calls POST /api/v1/files (multipart) via FileService.uploadFile().
/// NOTE: file_picker package not yet in pubspec — uses placeholder tap-to-select.
class UploadFileScreen extends StatefulWidget {
  const UploadFileScreen({super.key});

  @override
  State<UploadFileScreen> createState() => _UploadFileScreenState();
}

class _UploadFileScreenState extends State<UploadFileScreen> {
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFF84736F);

  final _service = FileService();
  final _titleCtrl = TextEditingController();

  File? _pickedFile;
  FileCategory _category = FileCategory.other;
  FileOwner _owner = FileOwner.mother;
  FileVisibility _visibility = FileVisibility.private;
  bool _uploading = false;

  @override
  void dispose() {
    _titleCtrl.dispose();
    super.dispose();
  }

  void _pickFile() {
    // TODO: integrate file_picker package (flutter_pub: file_picker)
    // For now show placeholder dialog
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: const Text(
          'Chọn tệp',
          style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w600),
        ),
        content: const Text(
          'Chức năng chọn tệp cần package "file_picker".\nSẽ tích hợp ở bước tiếp theo.',
          style: TextStyle(fontFamily: 'Lexend'),
        ),
        actions: [
          FilledButton(
            onPressed: () => Navigator.pop(context),
            style: FilledButton.styleFrom(backgroundColor: _primaryContainer),
            child: const Text(
              'Đã hiểu',
              style: TextStyle(fontFamily: 'Lexend'),
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _submit() async {
    if (_pickedFile == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Vui lòng chọn tệp trước khi tải lên.')),
      );
      return;
    }
    if (_titleCtrl.text.trim().isEmpty) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Vui lòng nhập tiêu đề.')));
      return;
    }
    setState(() => _uploading = true);
    try {
      await _service.uploadFile(_pickedFile!, title: _titleCtrl.text.trim());
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Tải lên thành công!')));
      Navigator.pop(context, true);
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Tải lên thất bại. Vui lòng thử lại.')),
      );
    } finally {
      if (mounted) setState(() => _uploading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: Column(
          children: [
            _buildAppBar(),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(20, 0, 20, 32),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const SizedBox(height: 16),
                    _buildDropZone(),
                    if (_pickedFile != null) ...[
                      const SizedBox(height: 12),
                      _buildFilePreview(),
                    ],
                    const SizedBox(height: 24),
                    _buildForm(),
                    const SizedBox(height: 24),
                    _buildSubmitButton(),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAppBar() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(4, 16, 4, 0),
      child: Row(
        children: [
          IconButton(
            onPressed: () => Navigator.pop(context),
            icon: const Icon(Icons.close, color: _onSurfaceVariant),
          ),
          const Expanded(
            child: Text(
              'Tải lên tài liệu',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 20,
                fontWeight: FontWeight.w600,
                color: _onSurface,
              ),
            ),
          ),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildDropZone() {
    return GestureDetector(
      onTap: _pickFile,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(vertical: 40),
        decoration: BoxDecoration(
          color: const Color(0xFFFFF1EC),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(
            color: _primaryContainer.withAlpha(128),
            width: 2,
            style: BorderStyle.solid,
          ),
        ),
        child: Column(
          children: [
            Icon(
              Icons.cloud_upload_outlined,
              size: 52,
              color: _primaryContainer.withAlpha(204),
            ),
            const SizedBox(height: 12),
            const Text(
              'Nhấn để chọn tệp',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 18,
                fontWeight: FontWeight.w600,
                color: Color(0xFF271812),
              ),
            ),
            const SizedBox(height: 4),
            Text(
              'Hỗ trợ: PDF, JPG, PNG, MP4 (tối đa 10 MB)',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 12,
                color: _onSurfaceVariant.withAlpha(179),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildFilePreview() {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF5A463F).withAlpha(13),
            blurRadius: 8,
            offset: const Offset(0, 3),
          ),
        ],
      ),
      child: Row(
        children: [
          const Icon(
            Icons.insert_drive_file,
            color: Color(0xFFC98C7B),
            size: 32,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              _pickedFile!.path.split(Platform.pathSeparator).last,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                color: Color(0xFF271812),
              ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ),
          IconButton(
            icon: const Icon(Icons.close, size: 18, color: Color(0xFF524440)),
            onPressed: () => setState(() => _pickedFile = null),
          ),
        ],
      ),
    );
  }

  Widget _buildForm() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _SectionLabel(label: 'Tiêu đề'),
        const SizedBox(height: 8),
        TextField(
          controller: _titleCtrl,
          style: const TextStyle(fontFamily: 'Lexend'),
          decoration: InputDecoration(
            hintText: 'Nhập tiêu đề tài liệu',
            hintStyle: const TextStyle(
              fontFamily: 'Lexend',
              color: Color(0xFF9C857C),
            ),
            filled: true,
            fillColor: Colors.white,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: BorderSide(color: _outlineVariant.withAlpha(128)),
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: BorderSide(color: _outlineVariant.withAlpha(77)),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: const BorderSide(color: Color(0xFFC98C7B)),
            ),
          ),
        ),
        const SizedBox(height: 20),
        _SectionLabel(label: 'Loại tài liệu'),
        const SizedBox(height: 8),
        DropdownButtonFormField<FileCategory>(
          initialValue: _category,
          decoration: InputDecoration(
            filled: true,
            fillColor: Colors.white,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: BorderSide(color: _outlineVariant.withAlpha(128)),
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: BorderSide(color: _outlineVariant.withAlpha(77)),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: const BorderSide(color: Color(0xFFC98C7B)),
            ),
          ),
          style: const TextStyle(
            fontFamily: 'Lexend',
            color: Color(0xFF271812),
          ),
          items: FileCategory.values
              .map(
                (c) => DropdownMenuItem(
                  value: c,
                  child: Text(
                    c.displayLabel,
                    style: const TextStyle(fontFamily: 'Lexend'),
                  ),
                ),
              )
              .toList(),
          onChanged: (c) => setState(() => _category = c ?? FileCategory.other),
        ),
        const SizedBox(height: 20),
        _SectionLabel(label: 'Thuộc về'),
        const SizedBox(height: 8),
        Row(
          children: FileOwner.values.map((o) {
            final selected = _owner == o;
            return Padding(
              padding: const EdgeInsets.only(right: 12),
              child: GestureDetector(
                onTap: () => setState(() => _owner = o),
                child: Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 20,
                    vertical: 10,
                  ),
                  decoration: BoxDecoration(
                    color: selected ? const Color(0xFFC98C7B) : Colors.white,
                    borderRadius: BorderRadius.circular(99),
                    border: Border.all(
                      color: selected
                          ? const Color(0xFFC98C7B)
                          : const Color(0xFFD6C2BD),
                    ),
                  ),
                  child: Text(
                    o.displayLabel,
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontWeight: FontWeight.w600,
                      color: selected ? Colors.white : const Color(0xFF524440),
                    ),
                  ),
                ),
              ),
            );
          }).toList(),
        ),
        const SizedBox(height: 20),
        _SectionLabel(label: 'Quyền truy cập'),
        const SizedBox(height: 8),
        Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: _outlineVariant.withAlpha(77)),
          ),
          child: Row(
            children: [
              Icon(
                _visibility == FileVisibility.private
                    ? Icons.lock_outline
                    : Icons.share,
                color: _primaryContainer,
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _visibility == FileVisibility.private
                          ? 'Riêng tư'
                          : 'Chia sẻ với nhóm',
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                        color: Color(0xFF271812),
                      ),
                    ),
                    Text(
                      _visibility == FileVisibility.private
                          ? 'Chỉ mình bạn xem được'
                          : 'Nhóm chăm sóc có thể xem',
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        color: Color(0xFF524440),
                      ),
                    ),
                  ],
                ),
              ),
              Switch(
                value: _visibility == FileVisibility.shared,
                activeThumbColor: _primaryContainer,
                onChanged: (v) => setState(
                  () => _visibility = v
                      ? FileVisibility.shared
                      : FileVisibility.private,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildSubmitButton() {
    return SizedBox(
      width: double.infinity,
      height: 56,
      child: FilledButton.icon(
        onPressed: _uploading ? null : _submit,
        style: FilledButton.styleFrom(
          backgroundColor: _primaryContainer,
          disabledBackgroundColor: _primaryContainer.withAlpha(128),
          foregroundColor: Colors.white,
          shape: const StadiumBorder(),
        ),
        icon: _uploading
            ? const SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  color: Colors.white,
                ),
              )
            : const Icon(Icons.cloud_upload),
        label: Text(
          _uploading ? 'Đang tải lên...' : 'Tải lên',
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 20,
            fontWeight: FontWeight.w600,
          ),
        ),
      ),
    );
  }
}

class _SectionLabel extends StatelessWidget {
  final String label;

  const _SectionLabel({required this.label});

  @override
  Widget build(BuildContext context) {
    return Text(
      label,
      style: const TextStyle(
        fontFamily: 'Lexend',
        fontSize: 16,
        fontWeight: FontWeight.w600,
        color: Color(0xFF271812),
      ),
    );
  }
}
