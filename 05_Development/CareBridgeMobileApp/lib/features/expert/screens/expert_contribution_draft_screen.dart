import 'dart:io';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';
import 'package:file_picker/file_picker.dart';

import '../models/contribution_model.dart';
import '../services/expert_contribution_service.dart';
import '../widgets/contribution_status_chip.dart';

class ExpertContributionDraftScreen extends StatefulWidget {
  const ExpertContributionDraftScreen({super.key, this.contributionId});

  final String? contributionId;

  @override
  State<ExpertContributionDraftScreen> createState() =>
      _ExpertContributionDraftScreenState();
}

class _ExpertContributionDraftScreenState
    extends State<ExpertContributionDraftScreen> {
  final _service = ExpertContributionService.instance;
  final _formKey = GlobalKey<FormState>();
  final _titleController = TextEditingController();
  final _contentController = TextEditingController();

  bool _isEditing = false;
  bool _loading = true;
  bool _saving = false;
  bool _eligible = false;
  String? _error;

  String? _specialtyId;
  String? _hospitalId;

  final List<AttachmentRequest> _attachments = [];
  final Set<String> _uploadingFileIds = {};
  final Map<String, _FilePreview> _filePreviews = {};

  @override
  void initState() {
    super.initState();
    _isEditing = widget.contributionId != null;
    _loadInitial();
  }

  @override
  void dispose() {
    _titleController.dispose();
    _contentController.dispose();
    super.dispose();
  }

  Future<void> _loadInitial() async {
    setState(() => _loading = true);
    try {
      await Future.wait([_loadEligibility(), _loadMasterData()]);
      if (_isEditing) {
        await _loadContribution();
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _loadEligibility() async {
    try {
      final eligible = await _service.checkEligibility();
      if (mounted) setState(() => _eligible = eligible);
    } catch (_) {
      if (mounted) setState(() => _eligible = false);
    }
  }

  Future<void> _loadMasterData() async {
    // In production, fetch from master data API
    // For now, we'll just use the input fields for specialtyId/hospitalId as strings
  }

  Future<void> _loadContribution() async {
    if (widget.contributionId == null) return;
    try {
      final c = await _service.getContribution(widget.contributionId!);
      _titleController.text = c.title;
      _contentController.text = c.content;
      _specialtyId = c.specialtyId;
      _hospitalId = c.hospitalId;
      if (c.attachments != null) {
        for (final att in c.attachments!) {
          _attachments.add(AttachmentRequest(
            fileId: att.fileId,
            kind: att.kind,
            purpose: att.purpose,
            accessMode: att.accessMode,
            displayOrder: att.displayOrder,
          ));
          if (att.presignedUrl != null && att.originalName != null) {
            _filePreviews[att.fileId] = _FilePreview(
              fileName: att.originalName!,
              previewUrl: att.presignedUrl!,
              kind: att.kind,
            );
          }
        }
      }
      if (mounted) setState(() {});
    } catch (e) {
      if (mounted) {
        setState(() => _error = 'Không thể tải bài viết: $e');
      }
    }
  }

  Future<void> _pickImages() async {
    final picker = ImagePicker();
    final images = await picker.pickMultiImage();
    if (images.isEmpty) return;

    for (final img in images) {
      await _uploadFile(img.path, img.name, 'IMAGE');
    }
  }

  Future<void> _pickDocuments() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: ['pdf', 'doc', 'docx'],
      allowMultiple: true,
    );
    if (result == null || result.files.isEmpty) return;

    for (final file in result.files) {
      if (file.path != null) {
        await _uploadFile(file.path!, file.name, 'DOCUMENT');
      } else if (file.bytes != null) {
        await _uploadBytes(file.bytes!, file.name, 'DOCUMENT');
      }
    }
  }

  Future<void> _uploadFile(String path, String name, String kind) async {
    final fileId = 'temp_${DateTime.now().millisecondsSinceEpoch}_${_attachments.length}';
    setState(() {
      _uploadingFileIds.add(fileId);
      _filePreviews[fileId] = _FilePreview(
        fileName: name,
        previewUrl: kind == 'IMAGE' ? path : '',
        kind: kind,
        uploading: true,
      );
    });

    try {
      final bytes = await File(path).readAsBytes();
      final mimeType = _getMimeType(name, kind);
      final purpose = kind == 'IMAGE'
          ? 'MEDICAL_CONTRIBUTION_IMAGE'
          : 'MEDICAL_CONTRIBUTION_DOCUMENT';
      final accessMode = 'AUTHENTICATED';

      final uploadedFileId = await _service.uploadContributionFile(
        bytes: bytes,
        fileName: name,
        mimeType: mimeType,
        kind: kind,
        purpose: purpose,
        accessMode: accessMode,
      );

      _attachments.add(AttachmentRequest(
        fileId: uploadedFileId,
        kind: kind,
        purpose: purpose,
        accessMode: accessMode,
        displayOrder: _attachments.length,
      ));

      if (mounted) {
        setState(() {
          _uploadingFileIds.remove(fileId);
          _filePreviews[uploadedFileId] = _FilePreview(
            fileName: name,
            previewUrl: kind == 'IMAGE' ? path : '',
            kind: kind,
          );
          _filePreviews.remove(fileId);
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _uploadingFileIds.remove(fileId);
          _filePreviews.remove(fileId);
          _error = 'Tải file thất bại: $e';
        });
      }
    }
  }

  Future<void> _uploadBytes(List<int> bytes, String name, String kind) async {
    final fileId = 'temp_${DateTime.now().millisecondsSinceEpoch}_${_attachments.length}';
    setState(() {
      _uploadingFileIds.add(fileId);
      _filePreviews[fileId] = _FilePreview(
        fileName: name,
        previewUrl: kind == 'IMAGE' ? '' : '',
        kind: kind,
        uploading: true,
      );
    });

    try {
      final mimeType = _getMimeType(name, kind);
      final purpose = kind == 'IMAGE'
          ? 'MEDICAL_CONTRIBUTION_IMAGE'
          : 'MEDICAL_CONTRIBUTION_DOCUMENT';
      final accessMode = 'AUTHENTICATED';

      final uploadedFileId = await _service.uploadContributionFile(
        bytes: bytes,
        fileName: name,
        mimeType: mimeType,
        kind: kind,
        purpose: purpose,
        accessMode: accessMode,
      );

      _attachments.add(AttachmentRequest(
        fileId: uploadedFileId,
        kind: kind,
        purpose: purpose,
        accessMode: accessMode,
        displayOrder: _attachments.length,
      ));

      if (mounted) {
        setState(() {
          _uploadingFileIds.remove(fileId);
          _filePreviews[uploadedFileId] = _FilePreview(
            fileName: name,
            previewUrl: '',
            kind: kind,
          );
          _filePreviews.remove(fileId);
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _uploadingFileIds.remove(fileId);
          _filePreviews.remove(fileId);
          _error = 'Tải file thất bại: $e';
        });
      }
    }
  }

  String _getMimeType(String fileName, String kind) {
    final ext = fileName.toLowerCase().split('.').last;
    if (kind == 'IMAGE') {
      switch (ext) {
        case 'jpg':
        case 'jpeg':
          return 'image/jpeg';
        case 'png':
          return 'image/png';
        case 'webp':
          return 'image/webp';
        case 'heic':
          return 'image/heic';
        case 'gif':
          return 'image/gif';
        default:
          return 'application/octet-stream';
      }
    } else {
      switch (ext) {
        case 'pdf':
          return 'application/pdf';
        case 'doc':
          return 'application/msword';
        case 'docx':
          return 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
        default:
          return 'application/octet-stream';
      }
    }
  }

  void _removeAttachment(String fileId) {
    setState(() {
      _attachments.removeWhere((a) => a.fileId == fileId);
      final preview = _filePreviews.remove(fileId);
      if (preview != null && preview.previewUrl.isNotEmpty && preview.previewUrl.startsWith('file://')) {
        // Clean up local file if needed
      }
    });
  }

  bool _validateForm() {
    if (!_formKey.currentState!.validate()) return false;
    if (_titleController.text.trim().isEmpty) {
      setState(() => _error = 'Tiêu đề là bắt buộc');
      return false;
    }
    if (_contentController.text.trim().isEmpty) {
      setState(() => _error = 'Nội dung là bắt buộc');
      return false;
    }
    setState(() => _error = null);
    return true;
  }

  Future<void> _save() async {
    if (!_validateForm()) return;
    setState(() {
      _saving = true;
      _error = null;
    });

    try {
      final request = CreateContributionRequest(
        title: _titleController.text.trim(),
        content: _contentController.text.trim(),
        specialtyId: _specialtyId?.isNotEmpty == true ? _specialtyId : null,
        hospitalId: _hospitalId?.isNotEmpty == true ? _hospitalId : null,
        attachments: _attachments.isNotEmpty
            ? _attachments
                .asMap()
                .entries
                .map((e) => AttachmentRequest(
                      fileId: e.value.fileId,
                      kind: e.value.kind,
                      purpose: e.value.purpose,
                      accessMode: e.value.accessMode,
                      displayOrder: e.key,
                    ))
                .toList()
            : null,
      );

      if (_isEditing) {
        await _service.updateContribution(widget.contributionId!, UpdateContributionRequest(
          title: request.title,
          content: request.content,
          specialtyId: request.specialtyId,
          hospitalId: request.hospitalId,
          attachments: request.attachments,
        ));
      } else {
        await _service.createContribution(request);
      }

      if (mounted) {
        context.pop(true);
      }
    } catch (e) {
      if (mounted) {
        setState(() => _error = 'Lưu thất bại: $e');
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  Future<void> _saveAndSubmit() async {
    if (!_validateForm()) return;
    if (!_isEditing) {
      await _save();
      return;
    }
    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      await _service.submitContribution(widget.contributionId!);
      if (mounted) {
        context.pop(true);
      }
    } catch (e) {
      if (mounted) {
        setState(() => _error = 'Gửi duyệt thất bại: $e');
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final cs = theme.colorScheme;

    if (_loading) {
      return Scaffold(
        appBar: AppBar(title: const Text('Đang tải...')),
        body: const Center(child: CircularProgressIndicator()),
      );
    }

    if (!_eligible && !_isEditing) {
      return Scaffold(
        appBar: AppBar(title: const Text('Tạo bài viết mới')),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.lock_outline, size: 64, color: Colors.amber),
                const SizedBox(height: 16),
                Text(
                  'Chưa đủ điều kiện',
                  style: theme.textTheme.headlineSmall?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  'Bạn cần được xác minh (APPROVED) và có trạng thái trust ACTIVE để tạo bài viết.',
                  textAlign: TextAlign.center,
                  style: theme.textTheme.bodyMedium?.copyWith(
                    color: cs.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: Text(_isEditing
            ? (_saving || _error != null ? 'Chỉnh sửa bài viết' : 'Chỉnh sửa bài viết')
            : 'Tạo bài viết mới'),
        backgroundColor: cs.surface,
        elevation: 0,
      ),
      body: Form(
        key: _formKey,
        child: Column(
          children: [
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Error banner
                    if (_error != null)
                      Container(
                        width: double.infinity,
                        padding: const EdgeInsets.all(12),
                        margin: const EdgeInsets.only(bottom: 16),
                        decoration: BoxDecoration(
                          color: cs.errorContainer,
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: Text(
                          _error!,
                          style: TextStyle(color: cs.onErrorContainer),
                        ),
                      ),

                    // Basic Info
                    Text('Thông tin cơ bản',
                        style: theme.textTheme.titleLarge
                            ?.copyWith(fontWeight: FontWeight.w600)),
                    const SizedBox(height: 16),

                    TextFormField(
                      controller: _titleController,
                      decoration: const InputDecoration(
                        labelText: 'Tiêu đề *',
                        hintText: 'VD: Hướng dẫn chăm sóc sơ sinh đối với mẹ lần đầu',
                        border: OutlineInputBorder(),
                      ),
                      maxLength: 255,
                      validator: (v) => v == null || v.trim().isEmpty
                          ? 'Tiêu đề là bắt buộc'
                          : null,
                    ),
                    const SizedBox(height: 16),

                    TextFormField(
                      controller: _contentController,
                      decoration: const InputDecoration(
                        labelText: 'Nội dung *',
                        hintText: 'Viết nội dung chi tiết bài viết y khoa tại đây...',
                        border: OutlineInputBorder(),
                        alignLabelWithHint: true,
                      ),
                      maxLines: 10,
                      maxLength: 10000,
                      validator: (v) => v == null || v.trim().isEmpty
                          ? 'Nội dung là bắt buộc'
                          : null,
                    ),
                    const SizedBox(height: 16),

                    Row(
                      children: [
                        Expanded(
                          child: TextFormField(
                            initialValue: _specialtyId,
                            decoration: const InputDecoration(
                              labelText: 'Chuyên khoa (ID)',
                              hintText: 'VD: OBG, PED...',
                              border: OutlineInputBorder(),
                            ),
                            onChanged: (v) => _specialtyId =
                                v.isEmpty ? null : v,
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: TextFormField(
                            initialValue: _hospitalId,
                            decoration: const InputDecoration(
                              labelText: 'Bệnh viện (ID)',
                              hintText: 'VD: HOSP001, BVTW...',
                              border: OutlineInputBorder(),
                            ),
                            onChanged: (v) =>
                                _hospitalId = v.isEmpty ? null : v,
                          ),
                        ),
                      ],
                    ),

                    const SizedBox(height: 24),

                    // Image Upload Zone
                    _UploadZone(
                      kind: 'IMAGE',
                      title: 'Vùng tải ảnh',
                      subtitle: 'JPG, PNG, WebP, HEIC, GIF · ≤ 20MB · Upload lên Cloudinary',
                      icon: Icons.image_outlined,
                      color: Colors.blue,
                      attachments: _attachments
                          .where((a) => a.kind == 'IMAGE')
                          .toList(),
                      previews: _filePreviews,
                      uploading: _uploadingFileIds,
                      onPick: _pickImages,
                      onRemove: _removeAttachment,
                    ),

                    const SizedBox(height: 16),

                    // Document Upload Zone
                    _UploadZone(
                      kind: 'DOCUMENT',
                      title: 'Vùng tải tài liệu',
                      subtitle: 'PDF, DOC, DOCX · ≤ 20MB · Private (R2, 15-min presigned URL)',
                      icon: Icons.description_outlined,
                      color: Colors.orange,
                      attachments: _attachments
                          .where((a) => a.kind == 'DOCUMENT')
                          .toList(),
                      previews: _filePreviews,
                      uploading: _uploadingFileIds,
                      onPick: _pickDocuments,
                      onRemove: _removeAttachment,
                    ),
                  ],
                ),
              ),
            ),

            // Actions
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: cs.surface,
                border: Border(top: BorderSide(color: cs.outlineVariant)),
              ),
              child: SafeArea(
                top: false,
                child: Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  alignment: WrapAlignment.end,
                  children: [
                    OutlinedButton(
                      onPressed: _saving ? null : () => context.pop(),
                      child: const Text('Hủy'),
                    ),
                    if (_isEditing) ...[
                      OutlinedButton.icon(
                        onPressed: _saving ? null : _saveAndSubmit,
                        icon: const Icon(Icons.send_outlined, size: 18),
                        label: Text(_saving ? 'Đang gửi...' : 'Gửi duyệt'),
                        style: OutlinedButton.styleFrom(
                          foregroundColor: cs.primary,
                          side: BorderSide(color: cs.primary),
                        ),
                      ),
                      FilledButton(
                        onPressed: _saving ? null : _save,
                        child: Text(_saving ? 'Đang lưu...' : 'Cập nhật'),
                      ),
                    ] else ...[
                      FilledButton.icon(
                        onPressed: _saving ? null : _save,
                        icon: const Icon(Icons.save_outlined, size: 18),
                        label: Text(_saving ? 'Đang lưu...' : 'Lưu bản nháp'),
                      ),
                      OutlinedButton.icon(
                        onPressed: _saving ? null : _saveAndSubmit,
                        icon: const Icon(Icons.send_outlined, size: 18),
                        label: const Text('Lưu và gửi duyệt ngay'),
                        style: OutlinedButton.styleFrom(
                          foregroundColor: cs.primary,
                          side: BorderSide(color: cs.primary),
                        ),
                      ),
                    ],
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _FilePreview {
  final String fileName;
  final String previewUrl;
  final String kind;
  final bool uploading;

  _FilePreview({
    required this.fileName,
    required this.previewUrl,
    required this.kind,
    this.uploading = false,
  });
}

class _UploadZone extends StatelessWidget {
  const _UploadZone({
    required this.kind,
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.color,
    required this.attachments,
    required this.previews,
    required this.uploading,
    required this.onPick,
    required this.onRemove,
  });

  final String kind;
  final String title;
  final String subtitle;
  final IconData icon;
  final Color color;
  final List<AttachmentRequest> attachments;
  final Map<String, _FilePreview> previews;
  final Set<String> uploading;
  final VoidCallback onPick;
  final void Function(String) onRemove;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final cs = theme.colorScheme;

    return Container(
      decoration: BoxDecoration(
        border: Border.all(color: cs.outlineVariant),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: color.withValues(alpha: 0.05),
              borderRadius: const BorderRadius.vertical(top: Radius.circular(12)),
            ),
            child: Row(
              children: [
                Icon(icon, color: color, size: 24),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(title,
                          style: theme.textTheme.titleMedium?.copyWith(
                            fontWeight: FontWeight.w600,
                            color: color,
                          )),
                      Text(subtitle,
                          style: theme.textTheme.bodySmall?.copyWith(
                            color: cs.onSurfaceVariant,
                          )),
                    ],
                  ),
                ),
                FilledButton.icon(
                  onPressed: onPick,
                  icon: const Icon(Icons.add, size: 18),
                  label: const Text('Chọn file'),
                  style: FilledButton.styleFrom(
                    backgroundColor: color,
                    foregroundColor: Colors.white,
                  ),
                ),
              ],
            ),
          ),

          if (attachments.isNotEmpty) ...[
            const Divider(height: 1),
            Padding(
              padding: const EdgeInsets.all(12),
              child: Wrap(
                spacing: 8,
                runSpacing: 8,
                children: attachments.map((att) {
                  final preview = previews[att.fileId];
                  final isUploading = uploading.contains(att.fileId);

                  return Container(
                    width: 100,
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: cs.surfaceContainerHighest,
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: cs.outlineVariant),
                    ),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        if (kind == 'IMAGE' && preview != null && preview.previewUrl.isNotEmpty)
                          ClipRRect(
                            borderRadius: BorderRadius.circular(4),
                            child: Image.network(
                              preview.previewUrl,
                              width: 64,
                              height: 64,
                              fit: BoxFit.cover,
                              errorBuilder: (_, __, ___) =>
                                  _buildDocIcon(cs),
                            ),
                          )
                        else
                          _buildDocIcon(cs),
                        const SizedBox(height: 6),
                        Text(
                          preview?.fileName ?? '${kind.toLowerCase()} ${attachments.indexOf(att) + 1}',
                          style: theme.textTheme.labelSmall,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          textAlign: TextAlign.center,
                        ),
                        const SizedBox(height: 2),
                        Text(
                          isUploading ? 'Đang tải...' : 'Đã sẵn sàng',
                          style: theme.textTheme.labelSmall?.copyWith(
                            color: isUploading
                                ? cs.primary
                                : cs.onSurfaceVariant,
                          ),
                        ),
                        const SizedBox(height: 6),
                        if (!isUploading)
                          InkWell(
                            onTap: () => onRemove(att.fileId),
                            child: Icon(
                              Icons.close,
                              size: 18,
                              color: Colors.red,
                            ),
                          ),
                      ],
                    ),
                  );
                }).toList(),
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildDocIcon(ColorScheme cs) {
    return Container(
      width: 64,
      height: 64,
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Icon(color == Colors.blue ? Icons.image : Icons.description,
          color: color, size: 32),
    );
  }
}