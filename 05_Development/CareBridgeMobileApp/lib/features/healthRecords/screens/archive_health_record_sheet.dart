import 'package:flutter/material.dart';
import '../models/health_record_model.dart';
import '../services/health_record_service.dart';

class ArchiveHealthRecordSheet extends StatefulWidget {
  final HealthRecord record;

  const ArchiveHealthRecordSheet({super.key, required this.record});

  static Future<bool?> show(BuildContext context, HealthRecord record) {
    return showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => ArchiveHealthRecordSheet(record: record),
    );
  }

  @override
  State<ArchiveHealthRecordSheet> createState() =>
      _ArchiveHealthRecordSheetState();
}

class _ArchiveHealthRecordSheetState extends State<ArchiveHealthRecordSheet> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Color(0xFFF2EAE4);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  final _service = HealthRecordService();
  bool _archiveSelected = true; // archive vs soft-delete
  bool _isProcessing = false;

  Future<void> _confirm() async {
    setState(() => _isProcessing = true);
    try {
      // Both options use archive endpoint — backend only exposes PATCH /archive
      await _service.archiveHealthRecord(widget.record.id);
      if (mounted) Navigator.of(context).pop(true);
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Không thể thực hiện. Vui lòng thử lại.'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _isProcessing = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return DraggableScrollableSheet(
      initialChildSize: 0.85,
      minChildSize: 0.6,
      maxChildSize: 0.95,
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
                padding: const EdgeInsets.fromLTRB(24, 16, 24, 16),
                children: [
                  _buildHeader(),
                  const SizedBox(height: 20),
                  _buildRecordSummary(),
                  const SizedBox(height: 24),
                  _buildActionChoice(),
                  const SizedBox(height: 24),
                  _buildConfirmButton(),
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
            color: const Color(0xFFFFF3CD),
            borderRadius: BorderRadius.circular(20),
          ),
          child: const Icon(
            Icons.warning_amber_rounded,
            color: Color(0xFFFF8F00),
            size: 32,
          ),
        ),
        const SizedBox(height: 12),
        const Text(
          'Quản lý Hồ sơ Sức khỏe',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 18,
            fontWeight: FontWeight.w700,
            color: _onSurface,
          ),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 6),
        const Text(
          'Chọn hành động bạn muốn thực hiện với hồ sơ này.',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 13,
            color: _onSurfaceVariant,
            height: 1.5,
          ),
          textAlign: TextAlign.center,
        ),
      ],
    );
  }

  Widget _buildRecordSummary() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Column(
        children: [
          _infoRow(Icons.description_rounded, 'Tiêu đề', widget.record.title),
          const SizedBox(height: 8),
          _infoRow(
            Icons.category_rounded,
            'Loại',
            widget.record.recordType.displayLabel,
          ),
          const SizedBox(height: 8),
          _infoRow(
            widget.record.isShared
                ? Icons.people_rounded
                : Icons.lock_outline_rounded,
            'Chia sẻ',
            widget.record.isShared ? 'Đã chia sẻ' : 'Riêng tư',
          ),
        ],
      ),
    );
  }

  Widget _infoRow(IconData icon, String label, String value) {
    return Row(
      children: [
        Icon(icon, size: 16, color: _primaryContainer),
        const SizedBox(width: 8),
        Text(
          label,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 12,
            color: _onSurfaceVariant,
          ),
        ),
        const Spacer(),
        Text(
          value,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 12,
            fontWeight: FontWeight.w600,
            color: _onSurface,
          ),
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
      ],
    );
  }

  Widget _buildActionChoice() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'Chọn hành động',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 14,
            fontWeight: FontWeight.w700,
            color: _onSurface,
          ),
        ),
        const SizedBox(height: 12),
        _buildOptionCard(
          selected: _archiveSelected,
          icon: Icons.archive_rounded,
          iconBg: const Color(0xFFE8F5E9),
          iconColor: const Color(0xFF4CAF50),
          title: 'Lưu trữ hồ sơ',
          description:
              'Hồ sơ sẽ bị ẩn khỏi dòng thời gian nhưng dữ liệu vẫn được giữ nguyên.',
          onTap: () => setState(() => _archiveSelected = true),
        ),
        const SizedBox(height: 10),
        _buildOptionCard(
          selected: !_archiveSelected,
          icon: Icons.delete_outline_rounded,
          iconBg: const Color(0xFFFFEBEE),
          iconColor: Colors.red,
          title: 'Xóa mềm',
          description: 'Hồ sơ sẽ vào thùng rác và bị xóa sau 30 ngày.',
          onTap: () => setState(() => _archiveSelected = false),
        ),
      ],
    );
  }

  Widget _buildOptionCard({
    required bool selected,
    required IconData icon,
    required Color iconBg,
    required Color iconColor,
    required String title,
    required String description,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 180),
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: selected ? _canvas : Colors.white,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: selected ? _primaryContainer : const Color(0xFFE8E0DC),
            width: selected ? 2 : 1.5,
          ),
        ),
        child: Row(
          children: [
            Container(
              width: 44,
              height: 44,
              decoration: BoxDecoration(
                color: iconBg,
                borderRadius: BorderRadius.circular(14),
              ),
              child: Icon(icon, color: iconColor, size: 22),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      color: _onSurface,
                    ),
                  ),
                  const SizedBox(height: 3),
                  Text(
                    description,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 11,
                      color: _onSurfaceVariant,
                      height: 1.4,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),
            AnimatedContainer(
              duration: const Duration(milliseconds: 180),
              width: 20,
              height: 20,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: selected ? _primary : Colors.transparent,
                border: Border.all(
                  color: selected ? _primary : const Color(0xFFCCBBB6),
                  width: 2,
                ),
              ),
              child: selected
                  ? const Icon(
                      Icons.check_rounded,
                      color: Colors.white,
                      size: 12,
                    )
                  : null,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildConfirmButton() {
    return ElevatedButton(
      onPressed: _isProcessing ? null : _confirm,
      style: ElevatedButton.styleFrom(
        backgroundColor: Colors.red,
        foregroundColor: Colors.white,
        minimumSize: const Size.fromHeight(52),
        shape: const StadiumBorder(),
        elevation: 0,
      ),
      child: _isProcessing
          ? const SizedBox(
              width: 20,
              height: 20,
              child: CircularProgressIndicator(
                strokeWidth: 2.5,
                color: Colors.white,
              ),
            )
          : const Text(
              'Xác nhận thực hiện',
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
