import 'package:flutter/material.dart';
import '../models/file_model.dart';
import '../services/file_service.dart';

class DeleteFileConfirmationSheet extends StatefulWidget {
  final String fileId;
  final String fileName;
  final String fileSize;

  const DeleteFileConfirmationSheet({
    super.key,
    required this.fileId,
    required this.fileName,
    required this.fileSize,
  });

  static Future<bool?> show(
    BuildContext context, {
    required String fileId,
    required String fileName,
    required String fileSize,
  }) {
    return showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => DeleteFileConfirmationSheet(
        fileId: fileId,
        fileName: fileName,
        fileSize: fileSize,
      ),
    );
  }

  static Future<bool?> showFromFile(
    BuildContext context,
    ViewFileResponse file,
  ) {
    return show(
      context,
      fileId: file.fileId,
      fileName: file.originalName,
      fileSize: file.sizeLabel,
    );
  }

  @override
  State<DeleteFileConfirmationSheet> createState() =>
      _DeleteFileConfirmationSheetState();
}

class _DeleteFileConfirmationSheetState
    extends State<DeleteFileConfirmationSheet> {
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surface = Color(0xFFF2EAE4);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  final _service = FileService();
  bool _isDeleting = false;

  Future<void> _delete() async {
    setState(() => _isDeleting = true);
    try {
      await _service.deleteFile(widget.fileId);
      if (mounted) Navigator.of(context).pop(true);
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Không thể xóa tệp. Vui lòng thử lại.'),
            backgroundColor: Colors.red,
          ),
        );
        setState(() => _isDeleting = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return DraggableScrollableSheet(
      initialChildSize: 0.72,
      minChildSize: 0.5,
      maxChildSize: 0.85,
      builder: (_, scrollCtrl) => Container(
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.vertical(top: Radius.circular(32)),
        ),
        child: Column(
          children: [
            const SizedBox(height: 12),
            Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: const Color(0xFFE0D8D5),
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            Expanded(
              child: ListView(
                controller: scrollCtrl,
                padding: const EdgeInsets.fromLTRB(24, 20, 24, 0),
                children: [
                  _buildHeader(),
                  const SizedBox(height: 20),
                  _buildFileCard(),
                  const SizedBox(height: 14),
                  _buildTrashNotice(),
                  const SizedBox(height: 10),
                  _buildRetentionNotice(),
                  const SizedBox(height: 24),
                  _buildDeleteButton(),
                  const SizedBox(height: 8),
                  _buildCancelButton(),
                  SizedBox(height: MediaQuery.of(context).padding.bottom + 8),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Column(
      children: [
        Container(
          width: 64,
          height: 64,
          decoration: BoxDecoration(
            color: const Color(0xFFFFEBEE),
            borderRadius: BorderRadius.circular(20),
          ),
          child: const Icon(
            Icons.delete_forever_rounded,
            color: Colors.red,
            size: 32,
          ),
        ),
        const SizedBox(height: 14),
        const Text(
          'Xóa tệp tin?',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 20,
            fontWeight: FontWeight.w700,
            color: _onSurface,
          ),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 6),
        const Text(
          'Tệp sẽ được chuyển vào thùng rác.',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 13,
            color: _onSurfaceVariant,
          ),
          textAlign: TextAlign.center,
        ),
      ],
    );
  }

  Widget _buildFileCard() {
    final isPdf = widget.fileName.toLowerCase().endsWith('.pdf');
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Row(
        children: [
          Container(
            width: 52,
            height: 52,
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(14),
              boxShadow: [
                BoxShadow(color: Colors.black.withAlpha(15), blurRadius: 6),
              ],
            ),
            child: Icon(
              isPdf
                  ? Icons.picture_as_pdf_rounded
                  : Icons.insert_drive_file_rounded,
              color: isPdf ? Colors.red : _primaryContainer,
              size: 28,
            ),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  widget.fileName,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: _onSurface,
                  ),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 4),
                Text(
                  widget.fileSize,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    color: _onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTrashNotice() {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF8E1),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFFFFE082), width: 1),
      ),
      child: const Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(
            Icons.restore_from_trash_rounded,
            color: Color(0xFFFF8F00),
            size: 20,
          ),
          SizedBox(width: 10),
          Expanded(
            child: Text(
              'Tệp sẽ được lưu trong thùng rác 30 ngày. Bạn có thể khôi phục trong thời gian này.',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 12,
                color: _onSurface,
                height: 1.5,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildRetentionNotice() {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(16),
      ),
      child: const Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.lock_outline_rounded, color: _primaryContainer, size: 18),
          SizedBox(width: 10),
          Expanded(
            child: Text(
              'Tệp đã được chia sẻ với chuyên gia sẽ bị thu hồi quyền truy cập sau khi xóa.',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 12,
                color: _onSurfaceVariant,
                height: 1.5,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDeleteButton() {
    return ElevatedButton(
      onPressed: _isDeleting ? null : _delete,
      style: ElevatedButton.styleFrom(
        backgroundColor: Colors.red,
        foregroundColor: Colors.white,
        minimumSize: const Size.fromHeight(52),
        shape: const StadiumBorder(),
        elevation: 0,
      ),
      child: _isDeleting
          ? const SizedBox(
              width: 20,
              height: 20,
              child: CircularProgressIndicator(
                strokeWidth: 2.5,
                color: Colors.white,
              ),
            )
          : const Text(
              'Xóa tệp tin',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 15,
                fontWeight: FontWeight.w700,
              ),
            ),
    );
  }

  Widget _buildCancelButton() {
    return TextButton(
      onPressed: () => Navigator.of(context).pop(false),
      child: const Text(
        'Hủy bỏ',
        style: TextStyle(
          fontFamily: 'Lexend',
          fontSize: 14,
          color: _onSurfaceVariant,
        ),
      ),
    );
  }
}
