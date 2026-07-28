import 'package:flutter/material.dart';

import '../../fileManager/screens/file_viewer_screen.dart';
import '../models/health_record_model.dart';

class HealthRecordAttachmentDetailScreen extends StatelessWidget {
  const HealthRecordAttachmentDetailScreen({
    super.key,
    required this.record,
    required this.attachment,
  });

  final HealthRecordDetail record;
  final FileAttachment attachment;

  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Color(0xFFFFFFFF);
  static const _surfaceVariant = Color(0xFFFFF1EC);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  bool get _isPdf => attachment.isPdf;

  String get _attachmentType {
    if (_isPdf) return 'PDF';
    if (attachment.mimeType?.startsWith('image/') == true) return 'Ảnh';
    return attachment.mimeType ?? 'Tệp đính kèm';
  }

  DateTime get _uploadedAt =>
      attachment.createdAt ?? record.createdAt ?? record.recordDate;

  String _formatDate(DateTime date) {
    final day = date.day.toString().padLeft(2, '0');
    final month = date.month.toString().padLeft(2, '0');
    final year = date.year.toString();
    return '$day/$month/$year';
  }

  void _openFile(BuildContext context) {
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

  @override
  Widget build(BuildContext context) {
    final facilityName = record.facilityName?.trim();
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: Column(
          children: [
            _buildAppBar(context),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(20, 12, 20, 28),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _buildInfoSection(
                      children: [
                        _InfoRow(
                          icon: Icons.title_rounded,
                          label: 'Tiêu đề',
                          value: record.title,
                        ),
                        _InfoRow(
                          icon: Icons.medical_information_rounded,
                          label: 'Loại hồ sơ',
                          value: record.recordType.displayLabel,
                        ),
                        _InfoRow(
                          icon: Icons.event_rounded,
                          label: 'Ngày upload',
                          value: _formatDate(_uploadedAt),
                        ),
                        _InfoRow(
                          icon: Icons.local_hospital_rounded,
                          label: 'Cơ sở y tế',
                          value: facilityName == null || facilityName.isEmpty
                              ? 'Chưa cập nhật'
                              : facilityName,
                        ),
                      ],
                    ),
                    const SizedBox(height: 18),
                    const Text(
                      'Tệp đính kèm',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                        color: _onSurface,
                      ),
                    ),
                    const SizedBox(height: 10),
                    _AttachmentDetailTile(
                      attachment: attachment,
                      attachmentType: _attachmentType,
                      isPdf: _isPdf,
                      onOpen: () => _openFile(context),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAppBar(BuildContext context) {
    return SizedBox(
      height: 52,
      child: Row(
        children: [
          IconButton(
            onPressed: () => Navigator.of(context).pop(),
            icon: const Icon(Icons.arrow_back, color: _onSurfaceVariant),
          ),
          const Expanded(
            child: Text(
              'Chi tiết tệp hồ sơ',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 22,
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

  Widget _buildInfoSection({required List<Widget> children}) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(18),
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
          const SizedBox(height: 14),
          ...children,
        ],
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  const _InfoRow({
    required this.icon,
    required this.label,
    required this.value,
  });

  final IconData icon;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 14),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 34,
            height: 34,
            decoration: BoxDecoration(
              color: HealthRecordAttachmentDetailScreen._surfaceVariant,
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(
              icon,
              size: 18,
              color: HealthRecordAttachmentDetailScreen._primaryContainer,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    color: HealthRecordAttachmentDetailScreen._onSurfaceVariant,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  value,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                    color: HealthRecordAttachmentDetailScreen._onSurface,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _AttachmentDetailTile extends StatelessWidget {
  const _AttachmentDetailTile({
    required this.attachment,
    required this.attachmentType,
    required this.isPdf,
    required this.onOpen,
  });

  final FileAttachment attachment;
  final String attachmentType;
  final bool isPdf;
  final VoidCallback onOpen;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: HealthRecordAttachmentDetailScreen._surface,
      borderRadius: BorderRadius.circular(18),
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(18),
          border: Border.all(color: const Color(0xFFE8DAD6)),
        ),
        child: Row(
          children: [
            Container(
              width: 48,
              height: 48,
              decoration: BoxDecoration(
                color: HealthRecordAttachmentDetailScreen._surfaceVariant,
                borderRadius: BorderRadius.circular(14),
              ),
              child: Icon(
                isPdf ? Icons.picture_as_pdf_rounded : Icons.image_rounded,
                color: isPdf
                    ? const Color(0xFFBA1A1A)
                    : HealthRecordAttachmentDetailScreen._primaryContainer,
                size: 26,
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
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 14,
                      fontWeight: FontWeight.w700,
                      color: HealthRecordAttachmentDetailScreen._onSurface,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    attachmentType,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      color:
                          HealthRecordAttachmentDetailScreen._onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),
            IconButton.filledTonal(
              tooltip: 'Xem file',
              onPressed: onOpen,
              icon: const Icon(Icons.visibility_rounded),
              style: IconButton.styleFrom(
                foregroundColor: HealthRecordAttachmentDetailScreen._primary,
                backgroundColor: const Color(0xFFFFE2D9),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
