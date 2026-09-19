import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../models/baby_daily_log_model.dart';
import '../services/baby_log_service.dart';

class BabyDailyLogDetailScreen extends StatefulWidget {
  final String babyId;
  final String logId;
  final BabyDailyLog? initialLog;
  final BabyLogService? logService;

  const BabyDailyLogDetailScreen({
    super.key,
    required this.babyId,
    required this.logId,
    this.initialLog,
    this.logService,
  });

  @override
  State<BabyDailyLogDetailScreen> createState() =>
      _BabyDailyLogDetailScreenState();
}

class _BabyDailyLogDetailScreenState extends State<BabyDailyLogDetailScreen> {
  static const _canvas = Color(0xFFFEF8F4);
  static const _primary = Color(0xFF845143);
  static const _primaryLight = Color(0xFFF7F1EE);
  static const _cardBorder = Color(0xFFF0EAE6);
  static const _onSurface = Color(0xFF3D2E28);
  static const _onSurfaceVariant = Color(0xFF7A655C);
  static const _error = Color(0xFFBA1A1A);

  late final BabyLogService _service;
  BabyDailyLog? _log;
  bool _isLoading = true;
  bool _isDeleting = false;
  String? _errorMessage;
  int _fetchGeneration = 0;

  @override
  void initState() {
    super.initState();
    _service = widget.logService ?? BabyLogService();
    if (widget.initialLog != null) {
      _log = widget.initialLog;
      _isLoading = false;
    } else {
      _fetchLogDetail();
    }
  }

  Future<void> _fetchLogDetail() async {
    final generation = ++_fetchGeneration;
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });
    try {
      final log = await _service.getDailyLogDetail(widget.babyId, widget.logId);
      if (log.babyId != widget.babyId) {
        throw const FormatException('Baby daily-log scope mismatch');
      }
      if (mounted && generation == _fetchGeneration) {
        setState(() {
          _log = log;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted && generation == _fetchGeneration) {
        setState(() {
          _errorMessage = 'Không thể tải chi tiết nhật ký. $e';
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _deleteLog() async {
    setState(() => _isDeleting = true);
    try {
      await _service.deleteDailyLog(widget.babyId, widget.logId);
      if (mounted) Navigator.pop(context, true);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Không thể xóa nhật ký. $e')));
      }
    } finally {
      if (mounted) setState(() => _isDeleting = false);
    }
  }

  Future<void> _confirmDelete() async {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(32)),
      ),
      builder: (_) => _DeleteConfirmSheet(
        onConfirm: () async {
          Navigator.of(context).pop();
          await _deleteLog();
        },
      ),
    );
  }

  Future<void> _openEdit() async {
    final result = await context.push(
      '/babies/${widget.babyId}/daily-logs/${widget.logId}/edit',
      extra: _log,
    );
    if (!mounted) return;
    if (result == 'deleted') {
      Navigator.of(context).pop(true);
      return;
    }
    if (result == true) await _fetchLogDetail();
  }

  (Color, Color) _colorsForLog(LogType type, [String? rawType]) {
    final raw = rawType?.toUpperCase();
    if (raw == 'FEVER' ||
        raw == 'SYMPTOM' ||
        raw == 'VOMITING' ||
        type == LogType.fever ||
        type == LogType.vomiting ||
        type == LogType.symptom) {
      return (const Color(0xFFFFEBEE), const Color(0xFFC62828));
    }
    if (raw == 'MEDICINE' || type == LogType.medicine) {
      return (const Color(0xFFEDE7F6), const Color(0xFF6A1B9A));
    }
    return switch (type) {
      LogType.feeding => (const Color(0xFFFFF3E0), const Color(0xFFD97706)),
      LogType.sleep => (const Color(0xFFEDE7F6), const Color(0xFF5E35B1)),
      LogType.diaper => (const Color(0xFFE3F2FD), const Color(0xFF0284C7)),
      _ => (const Color(0xFFFAF4EE), const Color(0xFF845143)),
    };
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      appBar: AppBar(
        backgroundColor: _canvas,
        elevation: 0,
        centerTitle: true,
        leading: IconButton(
          icon: Container(
            width: 36,
            height: 36,
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(10),
              border: Border.all(color: _cardBorder),
              boxShadow: const [
                BoxShadow(
                  color: Color(0x08000000),
                  blurRadius: 8,
                  offset: Offset(0, 2),
                ),
              ],
            ),
            child: const Icon(
              Icons.arrow_back_rounded,
              color: _onSurface,
              size: 18,
            ),
          ),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'Chi tiết nhật ký',
          style: TextStyle(
            color: _onSurface,
            fontWeight: FontWeight.w700,
            fontFamily: 'Lexend',
            fontSize: 18,
          ),
        ),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: _primary))
          : _errorMessage != null
          ? _buildErrorState()
          : _buildDetail(),
    );
  }

  Widget _buildErrorState() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 64,
              height: 64,
              decoration: BoxDecoration(
                color: const Color(0xFFFFEBEE),
                borderRadius: BorderRadius.circular(20),
              ),
              child: const Icon(
                Icons.error_outline_rounded,
                size: 32,
                color: _error,
              ),
            ),
            const SizedBox(height: 16),
            Text(
              _errorMessage!,
              textAlign: TextAlign.center,
              style: const TextStyle(
                color: _onSurfaceVariant,
                fontFamily: 'Lexend',
                fontSize: 14,
                height: 1.5,
              ),
            ),
            const SizedBox(height: 20),
            ElevatedButton(
              onPressed: _fetchLogDetail,
              style: ElevatedButton.styleFrom(
                backgroundColor: _primary,
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(14),
                ),
                elevation: 0,
              ),
              child: const Text(
                'Thử lại',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDetail() {
    final log = _log!;
    final colors = _colorsForLog(log.logType, log.rawLogType);
    final bgAccent = colors.$1;
    final iconAccent = colors.$2;

    return SingleChildScrollView(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 20),
      child: Column(
        children: [
          // Hero Log Type Card
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(vertical: 24, horizontal: 20),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(24),
              border: Border.all(color: _cardBorder),
              boxShadow: const [
                BoxShadow(
                  color: Color(0x08000000),
                  blurRadius: 16,
                  offset: Offset(0, 4),
                ),
              ],
            ),
            child: Column(
              children: [
                Container(
                  width: 76,
                  height: 76,
                  decoration: BoxDecoration(
                    color: bgAccent,
                    shape: BoxShape.circle,
                    border: Border.all(
                      color: iconAccent.withAlpha(50),
                      width: 2,
                    ),
                    boxShadow: [
                      BoxShadow(
                        color: iconAccent.withAlpha(25),
                        blurRadius: 12,
                        offset: const Offset(0, 4),
                      ),
                    ],
                  ),
                  child: Icon(
                    _iconFor(log.logType, log.rawLogType),
                    color: iconAccent,
                    size: 38,
                  ),
                ),
                const SizedBox(height: 16),
                Text(
                  log.displayTypeLabel,
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    fontSize: 22,
                    fontWeight: FontWeight.w700,
                    color: _onSurface,
                    fontFamily: 'Lexend',
                  ),
                ),
                const SizedBox(height: 8),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 12,
                    vertical: 5,
                  ),
                  decoration: BoxDecoration(
                    color: const Color(0xFFFAF4EE),
                    borderRadius: BorderRadius.circular(99),
                    border: Border.all(color: const Color(0xFFE8DDD6)),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(
                        Icons.access_time_rounded,
                        size: 13,
                        color: _primary,
                      ),
                      const SizedBox(width: 5),
                      Text(
                        _formatDateTime(log.startedAt),
                        style: const TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w600,
                          color: _onSurfaceVariant,
                          fontFamily: 'Lexend',
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // Giá trị Card
          _buildInfoCard(
            icon: Icons.straighten_rounded,
            title: 'Giá trị',
            child: Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              decoration: BoxDecoration(
                color: const Color(0xFFFAF7F5),
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: _cardBorder),
              ),
              child: Text(
                _formatQuantity(log),
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 24,
                  fontWeight: FontWeight.w800,
                  color: _primary,
                ),
              ),
            ),
          ),
          const SizedBox(height: 16),

          // Thời gian Card
          _buildInfoCard(
            icon: Icons.schedule_rounded,
            title: 'Thời gian',
            trailing: Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
              decoration: BoxDecoration(
                color: const Color(0xFFFAF4EE),
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: const Color(0xFFE8DDD6)),
              ),
              child: Text(
                _formatDateTime(log.startedAt),
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 13,
                  fontWeight: FontWeight.w700,
                  color: _onSurface,
                ),
              ),
            ),
          ),
          const SizedBox(height: 16),

          // Ghi chú Card
          _buildInfoCard(
            icon: Icons.notes_rounded,
            title: 'Ghi chú',
            child: Container(
              width: double.infinity,
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: const Color(0xFFFAF7F5),
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: _cardBorder),
              ),
              child: Text(
                (log.note == null || log.note!.trim().isEmpty)
                    ? 'Không có ghi chú'
                    : log.note!,
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  height: 1.5,
                  color: (log.note == null || log.note!.trim().isEmpty)
                      ? const Color(0xFFA89890)
                      : _onSurface,
                  fontStyle: (log.note == null || log.note!.trim().isEmpty)
                      ? FontStyle.italic
                      : null,
                ),
              ),
            ),
          ),
          const SizedBox(height: 28),

          // Nút Chỉnh sửa
          ElevatedButton.icon(
            onPressed: _openEdit,
            icon: const Icon(Icons.edit_rounded, size: 18),
            label: const Text(
              'Chỉnh sửa',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 15,
                fontWeight: FontWeight.w700,
              ),
            ),
            style: ElevatedButton.styleFrom(
              backgroundColor: _primary,
              foregroundColor: Colors.white,
              minimumSize: const Size(double.infinity, 52),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
              elevation: 0,
            ),
          ),
          const SizedBox(height: 12),

          // Nút Xóa nhật ký
          OutlinedButton.icon(
            onPressed: _isDeleting ? null : _confirmDelete,
            icon: _isDeleting
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: _error,
                    ),
                  )
                : const Icon(
                    Icons.delete_outline_rounded,
                    size: 18,
                    color: _error,
                  ),
            label: Text(
              _isDeleting ? 'Đang xóa...' : 'Xóa nhật ký',
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 15,
                fontWeight: FontWeight.w700,
                color: _error,
              ),
            ),
            style: OutlinedButton.styleFrom(
              backgroundColor: const Color(0xFFFFF8F7),
              side: const BorderSide(color: Color(0xFFFFCDD2), width: 1.5),
              minimumSize: const Size(double.infinity, 52),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildInfoCard({
    required IconData icon,
    required String title,
    Widget? trailing,
    Widget? child,
  }) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: _cardBorder),
        boxShadow: const [
          BoxShadow(
            color: Color(0x08000000),
            blurRadius: 16,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  Container(
                    width: 38,
                    height: 38,
                    decoration: BoxDecoration(
                      color: _primaryLight,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Icon(icon, color: _primary, size: 18),
                  ),
                  const SizedBox(width: 12),
                  Text(
                    title,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 15,
                      fontWeight: FontWeight.w700,
                      color: _onSurface,
                    ),
                  ),
                ],
              ),
              ?trailing,
            ],
          ),
          if (child != null) ...[
            const SizedBox(height: 14),
            child,
          ],
        ],
      ),
    );
  }

  IconData _iconFor(LogType type, [String? rawType]) {
    switch (rawType?.toUpperCase()) {
      case 'FEVER':
      case 'SYMPTOM':
        return Icons.thermostat_rounded;
      case 'VOMITING':
        return Icons.sick_rounded;
      case 'MEDICINE':
        return Icons.medication_rounded;
    }
    switch (type) {
      case LogType.feeding:
        return Icons.restaurant_rounded;
      case LogType.sleep:
        return Icons.bedtime_rounded;
      case LogType.diaper:
        return Icons.baby_changing_station_rounded;
      case LogType.fever:
        return Icons.thermostat_rounded;
      case LogType.vomiting:
        return Icons.sick_rounded;
      case LogType.medicine:
        return Icons.medication_rounded;
      case LogType.symptom:
        return Icons.health_and_safety_rounded;
    }
  }

  String _formatDateTime(DateTime? value) {
    if (value == null) return '-';
    final local = value.toLocal();
    final h = local.hour.toString().padLeft(2, '0');
    final m = local.minute.toString().padLeft(2, '0');
    return '${local.day}/${local.month}/${local.year} $h:$m';
  }

  String _formatQuantity(BabyDailyLog log) {
    if (log.quantity == null) return '-';
    if (log.logType == LogType.sleep ||
        log.rawLogType?.toUpperCase() == 'SLEEP') {
      final unit = log.unit?.trim().toLowerCase();
      final totalMin =
          (unit == 'giờ' || unit == 'h' || unit == 'hours' || unit == 'hour')
              ? (log.quantity! * 60).round()
              : log.quantity!.round();
      return formatSleepMinutes(totalMin);
    }
    final value = log.quantity!.toStringAsFixed(
      log.quantity! % 1 == 0 ? 0 : 1,
    );
    final unit = log.unit?.trim();
    return unit == null || unit.isEmpty ? value : '$value $unit';
  }
}

class _DeleteConfirmSheet extends StatelessWidget {
  const _DeleteConfirmSheet({required this.onConfirm});

  final VoidCallback onConfirm;

  static const _onSurface = Color(0xFF3D2E28);
  static const _onSurfaceVariant = Color(0xFF7A655C);
  static const _error = Color(0xFFBA1A1A);

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(24, 16, 24, 20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Handle bar
            Container(
              width: 44,
              height: 4,
              decoration: BoxDecoration(
                color: const Color(0xFFE8DDD6),
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            const SizedBox(height: 24),
            // Danger icon in soft clay circle/squircle
            Container(
              width: 68,
              height: 68,
              decoration: BoxDecoration(
                color: const Color(0xFFFFEBEE),
                borderRadius: BorderRadius.circular(22),
                border: Border.all(color: const Color(0xFFFFCDD2), width: 1.5),
                boxShadow: const [
                  BoxShadow(
                    color: Color(0x10BA1A1A),
                    blurRadius: 16,
                    offset: Offset(0, 4),
                  ),
                ],
              ),
              child: const Icon(
                Icons.delete_outline_rounded,
                color: _error,
                size: 34,
              ),
            ),
            const SizedBox(height: 18),
            const Text(
              'Xóa nhật ký này?',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 20,
                fontWeight: FontWeight.w700,
                color: _onSurface,
              ),
            ),
            const SizedBox(height: 10),
            const Text(
              'Hành động này không thể hoàn tác. Nhật ký sẽ bị xóa vĩnh viễn.',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                color: _onSurfaceVariant,
                height: 1.5,
              ),
            ),
            const SizedBox(height: 28),
            // Confirm delete button
            ElevatedButton.icon(
              onPressed: onConfirm,
              icon: const Icon(Icons.delete_forever_rounded, size: 18),
              label: const Text(
                'Xác nhận xóa',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 15,
                  fontWeight: FontWeight.w700,
                ),
              ),
              style: ElevatedButton.styleFrom(
                backgroundColor: _error,
                foregroundColor: Colors.white,
                minimumSize: const Size(double.infinity, 52),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                ),
                elevation: 0,
              ),
            ),
            const SizedBox(height: 12),
            // Cancel button
            OutlinedButton(
              onPressed: () => Navigator.of(context).pop(),
              style: OutlinedButton.styleFrom(
                backgroundColor: const Color(0xFFFAF4EE),
                foregroundColor: const Color(0xFF524440),
                side: const BorderSide(color: Color(0xFFE8DDD6)),
                minimumSize: const Size(double.infinity, 50),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                ),
              ),
              child: const Text(
                'Hủy bỏ',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 15,
                  fontWeight: FontWeight.w600,
                  color: Color(0xFF524440),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
