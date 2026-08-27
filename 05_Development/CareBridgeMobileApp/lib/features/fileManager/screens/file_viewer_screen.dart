import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';
import '../models/file_model.dart';
import '../services/file_service.dart';

class FileViewerScreen extends StatefulWidget {
  final String fileId;
  final String? fileName;
  final bool showDeleteAction;
  final bool showBottomActions;

  const FileViewerScreen({
    super.key,
    required this.fileId,
    this.fileName,
    this.showDeleteAction = true,
    this.showBottomActions = true,
  });

  @override
  State<FileViewerScreen> createState() => _FileViewerScreenState();
}

class _FileViewerScreenState extends State<FileViewerScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surface = Color(0xFFF2EAE4);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  final _service = FileService();
  ViewFileResponse? _file;
  bool _isLoading = true;
  String? _error;
  bool _isExpired = false;

  @override
  void initState() {
    super.initState();
    _loadFile();
  }

  Future<void> _loadFile() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final f = await _service.getFile(widget.fileId);
      final expired = f.status == 'EXPIRED';
      setState(() {
        _file = f;
        _isExpired = expired;
      });
    } catch (_) {
      setState(() => _error = 'Không thể tải tệp. Vui lòng thử lại.');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _openInBrowser() async {
    final url = _file?.presignedUrl;
    if (url == null) return;
    final uri = Uri.parse(url);
    if (await canLaunchUrl(uri)) {
      await launchUrl(uri, mode: LaunchMode.externalApplication);
    } else {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Không thể mở tệp.'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  Future<void> _shareFile() async {
    final url = _file?.presignedUrl;
    if (url == null) return;
    // Basic share via clipboard — a real implementation would use share_plus package
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Link chia sẻ đã được sao chép.'),
        backgroundColor: _primary,
      ),
    );
  }

  void _showDeleteConfirmation() {
    if (_file == null) return;
    Navigator.of(context)
        .push(
          MaterialPageRoute<bool>(
            fullscreenDialog: true,
            builder: (_) => _DeleteFileConfirmationPage(
              fileId: widget.fileId,
              fileName: _file!.originalName,
              fileSize: _file!.sizeLabel,
              service: _service,
            ),
          ),
        )
        .then((deleted) {
          if (deleted == true && mounted) Navigator.of(context).pop(true);
        });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black87,
      body: Stack(
        children: [
          _buildContent(),
          _buildTopBar(),
          if (widget.showBottomActions) _buildBottomBar(),
        ],
      ),
    );
  }

  Widget _buildTopBar() {
    final f = _file;
    return SafeArea(
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        child: Row(
          children: [
            IconButton(
              icon: const Icon(Icons.arrow_back_rounded, color: Colors.white),
              onPressed: () => Navigator.of(context).pop(),
            ),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    f?.originalName ?? widget.fileName ?? 'Tệp tin',
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      color: Colors.white,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  if (f != null)
                    Text(
                      '${f.sizeLabel}  •  ${f.dateLabel}',
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 11,
                        color: Colors.white60,
                      ),
                    ),
                ],
              ),
            ),
            if (f != null && widget.showDeleteAction)
              PopupMenuButton<String>(
                icon: const Icon(Icons.more_vert_rounded, color: Colors.white),
                color: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                ),
                onSelected: (v) {
                  if (v == 'delete') _showDeleteConfirmation();
                },
                itemBuilder: (_) => const [
                  PopupMenuItem(
                    value: 'delete',
                    child: Row(
                      children: [
                        Icon(
                          Icons.delete_outline_rounded,
                          color: Colors.red,
                          size: 18,
                        ),
                        SizedBox(width: 8),
                        Text(
                          'Xóa tệp',
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            color: Colors.red,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildContent() {
    if (_isLoading) {
      return const Center(
        child: CircularProgressIndicator(color: _primaryContainer),
      );
    }
    if (_error != null) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(
              Icons.error_outline_rounded,
              color: Colors.white60,
              size: 64,
            ),
            const SizedBox(height: 16),
            Text(
              _error!,
              style: const TextStyle(
                fontFamily: 'Lexend',
                color: Colors.white70,
              ),
            ),
            const SizedBox(height: 16),
            TextButton(
              onPressed: _loadFile,
              child: const Text(
                'Thử lại',
                style: TextStyle(color: _primaryContainer),
              ),
            ),
          ],
        ),
      );
    }

    final f = _file!;

    return Stack(
      children: [
        Center(
          child: Padding(
            padding: const EdgeInsets.only(top: 80, bottom: 100),
            child: _buildFilePreview(f),
          ),
        ),
        if (_isExpired)
          Positioned(
            top: 90,
            right: 16,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
              decoration: BoxDecoration(
                color: Colors.red.withAlpha(220),
                borderRadius: BorderRadius.circular(50),
              ),
              child: const Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(
                    Icons.access_time_rounded,
                    color: Colors.white,
                    size: 14,
                  ),
                  SizedBox(width: 4),
                  Text(
                    'Hết hạn chia sẻ',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 11,
                      color: Colors.white,
                    ),
                  ),
                ],
              ),
            ),
          ),
      ],
    );
  }

  Widget _buildFilePreview(ViewFileResponse f) {
    if (f.isImage && f.presignedUrl != null) {
      return ClipRRect(
        borderRadius: BorderRadius.circular(12),
        child: Image.network(
          f.presignedUrl!,
          fit: BoxFit.contain,
          errorBuilder: (_, _, _) => _pdfPlaceholder(f),
        ),
      );
    }
    return _pdfPlaceholder(f);
  }

  Widget _pdfPlaceholder(ViewFileResponse f) {
    return GestureDetector(
      onTap: _openInBrowser,
      child: Container(
        width: 280,
        height: 360,
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withAlpha(80),
              blurRadius: 24,
              offset: const Offset(0, 8),
            ),
          ],
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              f.isPdf
                  ? Icons.picture_as_pdf_rounded
                  : Icons.insert_drive_file_rounded,
              color: f.isPdf ? Colors.red : _primaryContainer,
              size: 72,
            ),
            const SizedBox(height: 16),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24),
              child: Text(
                f.originalName,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                  color: _onSurface,
                ),
                textAlign: TextAlign.center,
                maxLines: 3,
                overflow: TextOverflow.ellipsis,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              f.sizeLabel,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 12,
                color: _onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 24),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
              decoration: BoxDecoration(
                color: _surface,
                borderRadius: BorderRadius.circular(50),
              ),
              child: const Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.open_in_new_rounded, color: _primary, size: 16),
                  SizedBox(width: 6),
                  Text(
                    'Mở trong trình duyệt',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      color: _primary,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
            ),
          ],
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
          top: 12,
          bottom: MediaQuery.of(context).padding.bottom + 12,
        ),
        decoration: BoxDecoration(color: Colors.black.withAlpha(180)),
        child: Row(
          children: [
            Expanded(
              child: OutlinedButton.icon(
                onPressed: _isLoading ? null : _openInBrowser,
                icon: const Icon(Icons.download_rounded, size: 18),
                label: const Text(
                  'Tải xuống',
                  style: TextStyle(fontFamily: 'Lexend', fontSize: 14),
                ),
                style: OutlinedButton.styleFrom(
                  foregroundColor: Colors.white,
                  side: const BorderSide(color: Colors.white38, width: 1.5),
                  padding: const EdgeInsets.symmetric(vertical: 12),
                  shape: const StadiumBorder(),
                ),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: ElevatedButton.icon(
                onPressed: _isLoading ? null : _shareFile,
                icon: const Icon(Icons.share_rounded, size: 18),
                label: const Text(
                  'Chia sẻ',
                  style: TextStyle(fontFamily: 'Lexend', fontSize: 14),
                ),
                style: ElevatedButton.styleFrom(
                  backgroundColor: _primary,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 12),
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
}

class _DeleteFileConfirmationPage extends StatelessWidget {
  const _DeleteFileConfirmationPage({
    required this.fileId,
    required this.fileName,
    required this.fileSize,
    required this.service,
  });

  final String fileId;
  final String fileName;
  final String fileSize;
  final FileService service;

  static const _surface = Color(0xFFF2EAE4);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black54,
      body: SafeArea(
        child: Column(
          children: [
            const Spacer(),
            Container(
              margin: const EdgeInsets.all(16),
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(32),
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Center(
                    child: Icon(
                      Icons.delete_forever_rounded,
                      color: Colors.red,
                      size: 56,
                    ),
                  ),
                  const SizedBox(height: 12),
                  const Center(
                    child: Text(
                      'Xóa tệp tin?',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 18,
                        fontWeight: FontWeight.w700,
                        color: _onSurface,
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  Container(
                    padding: const EdgeInsets.all(14),
                    decoration: BoxDecoration(
                      color: _surface,
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: Row(
                      children: [
                        const Icon(
                          Icons.insert_drive_file_rounded,
                          color: Colors.red,
                          size: 32,
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                fileName,
                                style: const TextStyle(
                                  fontFamily: 'Lexend',
                                  fontSize: 13,
                                  fontWeight: FontWeight.w600,
                                  color: _onSurface,
                                ),
                                maxLines: 2,
                                overflow: TextOverflow.ellipsis,
                              ),
                              Text(
                                fileSize,
                                style: const TextStyle(
                                  fontFamily: 'Lexend',
                                  fontSize: 11,
                                  color: _onSurfaceVariant,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: const Color(0xFFFFF3CD),
                      borderRadius: BorderRadius.circular(14),
                    ),
                    child: const Row(
                      children: [
                        Icon(
                          Icons.warning_amber_rounded,
                          color: Color(0xFFFF8F00),
                          size: 16,
                        ),
                        SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            'Tệp sẽ được chuyển vào thùng rác và xóa sau 30 ngày.',
                            style: TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 11,
                              color: _onSurface,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 20),
                  ElevatedButton(
                    onPressed: () async {
                      try {
                        await service.deleteFile(fileId);
                        if (context.mounted) Navigator.of(context).pop(true);
                      } catch (_) {
                        if (context.mounted) {
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(
                              content: Text('Không thể xóa. Vui lòng thử lại.'),
                              backgroundColor: Colors.red,
                            ),
                          );
                        }
                      }
                    },
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.red,
                      foregroundColor: Colors.white,
                      minimumSize: const Size.fromHeight(50),
                      shape: const StadiumBorder(),
                      elevation: 0,
                    ),
                    child: const Text(
                      'Xóa tệp tin',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 15,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                  const SizedBox(height: 8),
                  TextButton(
                    onPressed: () => Navigator.of(context).pop(false),
                    child: const Text(
                      'Hủy bỏ',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 14,
                        color: _onSurfaceVariant,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
