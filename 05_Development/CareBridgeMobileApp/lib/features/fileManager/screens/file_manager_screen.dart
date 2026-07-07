import 'package:flutter/material.dart';
import '../models/file_model.dart';
import '../services/file_service.dart';
import 'upload_file_screen.dart';

/// CB-122 — File Manager (UC-167, UC-168, UC-169)
/// Shows storage usage bar, filter chips by category, file list with
/// icon/thumbnail, visibility badge, more_vert context menu.
/// Data: mock list (TODO: wire to GET /api/v1/files when endpoint available).
class FileManagerScreen extends StatefulWidget {
  const FileManagerScreen({super.key});

  @override
  State<FileManagerScreen> createState() => _FileManagerScreenState();
}

class _FileManagerScreenState extends State<FileManagerScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _surfaceContainer = Color(0xFFFFE9E3);
  static const _surfaceVariant = Color(0xFFFADCD3);
  static const _error = Color(0xFFBA1A1A);

  final _service = FileService();
  List<UserFile> _files = [];
  FileCategory? _activeFilter;
  bool _loading = true;

  // Mock storage: 65 of 100 MB used
  static const double _usedMb = 65;
  static const double _totalMb = 100;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final list = await _service.listMyFiles(filter: _activeFilter);
      if (mounted)
        setState(() {
          _files = list;
          _loading = false;
        });
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _setFilter(FileCategory? c) {
    setState(() => _activeFilter = c);
    _load();
  }

  Future<void> _deleteFile(UserFile f) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: const Text(
          'Xóa tệp?',
          style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w600),
        ),
        content: Text(
          'Xóa "${f.originalName}"? Không thể khôi phục.',
          style: const TextStyle(fontFamily: 'Lexend'),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Hủy', style: TextStyle(fontFamily: 'Lexend')),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            style: FilledButton.styleFrom(backgroundColor: _error),
            child: const Text('Xóa', style: TextStyle(fontFamily: 'Lexend')),
          ),
        ],
      ),
    );
    if (ok != true) return;
    try {
      await _service.deleteFile(f.fileId);
      _load();
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Không thể xóa.')));
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
            _buildStorage(),
            _buildFilterRow(),
            Expanded(
              child: _loading
                  ? const Center(
                      child: CircularProgressIndicator(
                        color: _primaryContainer,
                      ),
                    )
                  : RefreshIndicator(
                      color: _primaryContainer,
                      onRefresh: _load,
                      child: _files.isEmpty ? _buildEmpty() : _buildList(),
                    ),
            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        backgroundColor: _primaryContainer,
        foregroundColor: Colors.white,
        shape: const CircleBorder(),
        onPressed: () async {
          final uploaded = await Navigator.push<bool>(
            context,
            MaterialPageRoute(builder: (_) => const UploadFileScreen()),
          );
          if (uploaded == true) _load();
        },
        child: const Icon(Icons.upload),
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
            icon: const Icon(Icons.arrow_back, color: _onSurfaceVariant),
          ),
          const Expanded(
            child: Text(
              'Hồ sơ & Tài liệu',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 20,
                fontWeight: FontWeight.w600,
                color: _onSurface,
              ),
            ),
          ),
          IconButton(
            onPressed: () {},
            icon: const Icon(Icons.search, color: _onSurfaceVariant),
          ),
        ],
      ),
    );
  }

  Widget _buildStorage() {
    final pct = _usedMb / _totalMb;
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 16, 20, 8),
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          boxShadow: [
            BoxShadow(
              color: const Color(0xFF5A463F).withAlpha(13),
              blurRadius: 12,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  'Bộ nhớ',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: _onSurface,
                  ),
                ),
                Text(
                  '${_usedMb.toInt()} MB / ${_totalMb.toInt()} MB',
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    color: _onSurfaceVariant,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            ClipRRect(
              borderRadius: BorderRadius.circular(4),
              child: LinearProgressIndicator(
                value: pct,
                minHeight: 8,
                backgroundColor: const Color(0xFFFADCD3),
                valueColor: AlwaysStoppedAnimation<Color>(
                  pct > 0.85 ? _error : _primaryContainer,
                ),
              ),
            ),
            const SizedBox(height: 4),
            Text(
              '${(pct * 100).toInt()}% đã dùng',
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 11,
                color: _onSurfaceVariant,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildFilterRow() {
    return SizedBox(
      height: 52,
      child: ListView(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
        children: [
          _FilterChip(
            label: 'Tất cả',
            selected: _activeFilter == null,
            onTap: () => _setFilter(null),
          ),
          const SizedBox(width: 8),
          _FilterChip(
            label: 'Siêu âm',
            selected: _activeFilter == FileCategory.ultrasound,
            onTap: () => _setFilter(FileCategory.ultrasound),
          ),
          const SizedBox(width: 8),
          _FilterChip(
            label: 'Hồ sơ',
            selected: _activeFilter == FileCategory.medicalRecord,
            onTap: () => _setFilter(FileCategory.medicalRecord),
          ),
          const SizedBox(width: 8),
          _FilterChip(
            label: 'Hình ảnh',
            selected: _activeFilter == FileCategory.photo,
            onTap: () => _setFilter(FileCategory.photo),
          ),
        ],
      ),
    );
  }

  Widget _buildList() {
    return ListView.separated(
      padding: const EdgeInsets.fromLTRB(20, 4, 20, 100),
      itemCount: _files.length,
      separatorBuilder: (_, _) => const SizedBox(height: 10),
      itemBuilder: (_, i) =>
          _FileCard(file: _files[i], onDelete: () => _deleteFile(_files[i])),
    );
  }

  Widget _buildEmpty() {
    return const Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.folder_open, size: 64, color: Color(0xFFC98C7B)),
          SizedBox(height: 12),
          Text(
            'Chưa có tệp nào',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 20,
              fontWeight: FontWeight.w600,
              color: Color(0xFF271812),
            ),
          ),
          SizedBox(height: 8),
          Text(
            'Nhấn + để tải lên tài liệu',
            style: TextStyle(fontFamily: 'Lexend', color: Color(0xFF524440)),
          ),
        ],
      ),
    );
  }
}

class _FilterChip extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;

  const _FilterChip({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
        decoration: BoxDecoration(
          color: selected ? const Color(0xFFC98C7B) : Colors.white,
          borderRadius: BorderRadius.circular(99),
          border: selected ? null : Border.all(color: const Color(0xFFD6C2BD)),
          boxShadow: selected
              ? [
                  const BoxShadow(
                    color: Color(0xFFC98C7B),
                    blurRadius: 8,
                    offset: Offset(0, 3),
                  ),
                ]
              : null,
        ),
        child: Text(
          label,
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 13,
            fontWeight: FontWeight.w500,
            color: selected ? Colors.white : const Color(0xFF524440),
          ),
        ),
      ),
    );
  }
}

class _FileCard extends StatelessWidget {
  final UserFile file;
  final VoidCallback onDelete;

  const _FileCard({required this.file, required this.onDelete});

  IconData get _icon {
    final mime = file.mimeType ?? '';
    if (mime.startsWith('image/')) return Icons.image;
    if (mime == 'application/pdf') return Icons.picture_as_pdf;
    return Icons.insert_drive_file;
  }

  Color get _iconColor {
    final mime = file.mimeType ?? '';
    if (mime.startsWith('image/')) return const Color(0xFF845143);
    if (mime == 'application/pdf') return const Color(0xFFBA1A1A);
    return const Color(0xFF6E5A52);
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF5A463F).withAlpha(13),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Row(
        children: [
          Container(
            width: 48,
            height: 48,
            decoration: BoxDecoration(
              color: const Color(0xFFFFE9E3),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(_icon, color: _iconColor),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  file.originalName,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                    color: Color(0xFF271812),
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                Row(
                  children: [
                    if (file.sizeLabel.isNotEmpty) ...[
                      const Icon(
                        Icons.data_usage,
                        size: 12,
                        color: Color(0xFF524440),
                      ),
                      const SizedBox(width: 3),
                      Text(
                        file.sizeLabel,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 11,
                          color: Color(0xFF524440),
                        ),
                      ),
                      const SizedBox(width: 8),
                    ],
                    const Icon(
                      Icons.calendar_today,
                      size: 12,
                      color: Color(0xFF524440),
                    ),
                    const SizedBox(width: 3),
                    Text(
                      file.dateLabel,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 11,
                        color: Color(0xFF524440),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          // Visibility badge
          Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                file.visibility == FileVisibility.private
                    ? Icons.lock_outline
                    : Icons.share,
                size: 16,
                color: file.visibility == FileVisibility.private
                    ? const Color(0xFF524440)
                    : const Color(0xFF845143),
              ),
              Text(
                file.visibility.displayLabel,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 9,
                  color: Color(0xFF524440),
                ),
              ),
            ],
          ),
          PopupMenuButton<String>(
            icon: const Icon(Icons.more_vert, color: Color(0xFF524440)),
            onSelected: (v) {
              if (v == 'delete') onDelete();
            },
            itemBuilder: (_) => [
              const PopupMenuItem(
                value: 'view',
                child: Row(
                  children: [
                    Icon(Icons.visibility, size: 18),
                    SizedBox(width: 8),
                    Text('Xem', style: TextStyle(fontFamily: 'Lexend')),
                  ],
                ),
              ),
              const PopupMenuItem(
                value: 'share',
                child: Row(
                  children: [
                    Icon(Icons.share, size: 18),
                    SizedBox(width: 8),
                    Text('Chia sẻ', style: TextStyle(fontFamily: 'Lexend')),
                  ],
                ),
              ),
              const PopupMenuItem(
                value: 'delete',
                child: Row(
                  children: [
                    Icon(
                      Icons.delete_outline,
                      size: 18,
                      color: Color(0xFFBA1A1A),
                    ),
                    SizedBox(width: 8),
                    Text(
                      'Xóa',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        color: Color(0xFFBA1A1A),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
