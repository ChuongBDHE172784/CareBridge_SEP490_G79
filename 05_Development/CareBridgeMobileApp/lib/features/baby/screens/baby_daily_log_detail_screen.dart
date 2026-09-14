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
  static const _primary = Color(0xFFC98C7B);
  static const _bg = Color(0xFFF6F1EC);
  static const _text = Color(0xFF5A463F);
  static const _muted = Color(0xFF9C857C);
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _bg,
      appBar: AppBar(
        backgroundColor: _bg,
        elevation: 0,
        centerTitle: true,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: _text),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'Chi tiết nhật ký',
          style: TextStyle(
            color: _text,
            fontWeight: FontWeight.bold,
            fontFamily: 'Quicksand',
            fontSize: 20,
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
            const Icon(Icons.error_outline, size: 48, color: _error),
            const SizedBox(height: 12),
            Text(
              _errorMessage!,
              textAlign: TextAlign.center,
              style: const TextStyle(color: _muted, fontFamily: 'Quicksand'),
            ),
            const SizedBox(height: 16),
            TextButton(
              onPressed: _fetchLogDetail,
              child: const Text('Thử lại'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDetail() {
    final log = _log!;
    return SingleChildScrollView(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
      child: Column(
        children: [
          Container(
            width: 80,
            height: 80,
            decoration: const BoxDecoration(
              color: Color(0xFFF6DACF),
              shape: BoxShape.circle,
            ),
            child: Icon(
              _iconFor(log.logType, log.rawLogType),
              color: _primary,
              size: 40,
            ),
          ),
          const SizedBox(height: 16),
          Text(
            log.displayTypeLabel,
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontSize: 24,
              fontWeight: FontWeight.bold,
              color: _text,
              fontFamily: 'Quicksand',
            ),
          ),
          const SizedBox(height: 4),
          Text(
            _formatDateTime(log.startedAt),
            style: const TextStyle(
              fontSize: 14,
              color: _muted,
              fontFamily: 'Quicksand',
            ),
          ),
          const SizedBox(height: 32),
          _buildInfoCard(
            icon: Icons.straighten,
            title: 'Giá trị',
            child: Text(
              _formatQuantity(log),
              style: const TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
                color: _text,
              ),
            ),
          ),
          const SizedBox(height: 16),
          _buildInfoCard(
            icon: Icons.schedule,
            title: 'Thời gian',
            trailing: Text(
              _formatDateTime(log.startedAt),
              style: const TextStyle(
                fontSize: 15,
                fontWeight: FontWeight.w600,
                color: _text,
              ),
            ),
          ),
          const SizedBox(height: 16),
          _buildInfoCard(
            icon: Icons.notes,
            title: 'Ghi chú',
            child: Text(
              (log.note == null || log.note!.trim().isEmpty)
                  ? 'Không có ghi chú'
                  : log.note!,
              style: const TextStyle(fontSize: 16, color: _text),
            ),
          ),
          const SizedBox(height: 32),
          ElevatedButton.icon(
            onPressed: _openEdit,
            icon: const Icon(Icons.edit),
            label: const Text('Chỉnh sửa'),
            style: ElevatedButton.styleFrom(
              backgroundColor: _primary,
              foregroundColor: Colors.white,
              minimumSize: const Size(double.infinity, 56),
              shape: const StadiumBorder(),
            ),
          ),
          const SizedBox(height: 12),
          OutlinedButton.icon(
            onPressed: _isDeleting ? null : _confirmDelete,
            icon: _isDeleting
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: _error,
                    ),
                  )
                : const Icon(Icons.delete, color: _error),
            label: Text(
              _isDeleting ? 'Đang xóa...' : 'Xóa nhật ký',
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
                color: _error,
              ),
            ),
            style: OutlinedButton.styleFrom(
              side: const BorderSide(color: Color(0xFFFFDAD6), width: 2),
              minimumSize: const Size(double.infinity, 56),
              shape: const StadiumBorder(),
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
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.03),
            blurRadius: 20,
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
              Row(
                children: [
                  Icon(icon, color: _primary, size: 22),
                  const SizedBox(width: 12),
                  Text(
                    title,
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      color: _muted,
                    ),
                  ),
                ],
              ),
              ?trailing,
            ],
          ),
          if (child != null) ...[
            const SizedBox(height: 16),
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
        return Icons.thermostat;
      case 'VOMITING':
        return Icons.sick;
      case 'MEDICINE':
        return Icons.medication;
    }
    switch (type) {
      case LogType.feeding:
        return Icons.restaurant;
      case LogType.sleep:
        return Icons.bedtime;
      case LogType.diaper:
        return Icons.cleaning_services;
      case LogType.fever:
        return Icons.thermostat;
      case LogType.vomiting:
        return Icons.sick;
      case LogType.medicine:
        return Icons.medication;
      case LogType.symptom:
        return Icons.health_and_safety;
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
    if (log.logType == LogType.sleep || log.rawLogType?.toUpperCase() == 'SLEEP') {
      final unit = log.unit?.trim().toLowerCase();
      final totalMin = (unit == 'giờ' || unit == 'h' || unit == 'hours' || unit == 'hour')
          ? (log.quantity! * 60).round()
          : log.quantity!.round();
      return formatSleepMinutes(totalMin);
    }
    final value = log.quantity!.toStringAsFixed(log.quantity! % 1 == 0 ? 0 : 1);
    final unit = log.unit?.trim();
    return unit == null || unit.isEmpty ? value : '$value $unit';
  }
}

class _DeleteConfirmSheet extends StatelessWidget {
  const _DeleteConfirmSheet({required this.onConfirm});

  final VoidCallback onConfirm;

  static const _primary = Color(0xFF845143);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(24, 20, 24, 16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: const Color(0xFFE0D8D5),
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            const SizedBox(height: 20),
            Container(
              width: 64,
              height: 64,
              decoration: BoxDecoration(
                color: const Color(0xFFFFEDEA),
                borderRadius: BorderRadius.circular(20),
              ),
              child: const Icon(
                Icons.error_outline_rounded,
                color: Colors.red,
                size: 32,
              ),
            ),
            const SizedBox(height: 16),
            const Text(
              'Xóa nhật ký này?',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 18,
                fontWeight: FontWeight.w700,
                color: _onSurface,
              ),
            ),
            const SizedBox(height: 8),
            const Text(
              'Hành động này không thể hoàn tác. Nhật ký sẽ bị xóa vĩnh viễn.',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 13,
                color: _onSurfaceVariant,
                height: 1.5,
              ),
            ),
            const SizedBox(height: 24),
            ElevatedButton(
              onPressed: onConfirm,
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.red,
                foregroundColor: Colors.white,
                minimumSize: const Size.fromHeight(52),
                shape: const StadiumBorder(),
                elevation: 0,
              ),
              child: const Text(
                'Xác nhận xóa',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 15,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
            const SizedBox(height: 8),
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text(
                'Hủy bỏ',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  color: _primary,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
