import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../models/baby_daily_log_model.dart';
import '../services/baby_log_service.dart';

class BabyDailyLogDetailScreen extends StatefulWidget {
  final String babyId;
  final String logId;

  const BabyDailyLogDetailScreen({
    super.key,
    required this.babyId,
    required this.logId,
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

  final _service = BabyLogService();
  BabyDailyLog? _log;
  bool _isLoading = true;
  bool _isDeleting = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _fetchLogDetail();
  }

  Future<void> _fetchLogDetail() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });
    try {
      final log = await _service.getDailyLogDetail(widget.babyId, widget.logId);
      if (mounted) {
        setState(() {
          _log = log;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
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
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Không thể xóa nhật ký. $e')),
        );
      }
    } finally {
      if (mounted) setState(() => _isDeleting = false);
    }
  }

  Future<void> _openEdit() async {
    await context.push(
      '/babies/${widget.babyId}/daily-logs/${widget.logId}/edit',
      extra: _log,
    );
    if (mounted) _fetchLogDetail();
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
            child: Icon(_iconFor(log.logType), color: _primary, size: 40),
          ),
          const SizedBox(height: 16),
          Text(
            log.logType.displayLabel,
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
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                _durationSide('Bắt đầu', _formatDateTime(log.startedAt)),
                _durationSide('Kết thúc', _formatDateTime(log.endedAt)),
                _durationSide('Tổng', _formatDuration(log)),
              ],
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
            onPressed: _isDeleting ? null : _deleteLog,
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
    required Widget child,
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
          const SizedBox(height: 16),
          child,
        ],
      ),
    );
  }

  Widget _durationSide(String label, String value) {
    return Flexible(
      child: Column(
        children: [
          Text(
            label.toUpperCase(),
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.bold,
              color: _muted,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            value,
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.w700,
              color: _text,
            ),
          ),
        ],
      ),
    );
  }

  IconData _iconFor(LogType type) {
    switch (type) {
      case LogType.feeding:
        return Icons.restaurant;
      case LogType.sleep:
        return Icons.bedtime;
      case LogType.diaper:
        return Icons.cleaning_services;
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
    final value = log.quantity!.toStringAsFixed(log.quantity! % 1 == 0 ? 0 : 1);
    final unit = log.unit?.trim();
    return unit == null || unit.isEmpty ? value : '$value $unit';
  }

  String _formatDuration(BabyDailyLog log) {
    if (log.startedAt == null || log.endedAt == null) return '-';
    final minutes = log.endedAt!.difference(log.startedAt!).inMinutes;
    if (minutes <= 0) return '-';
    final hours = minutes ~/ 60;
    final mins = minutes % 60;
    if (hours == 0) return '${mins}m';
    return '${hours}h ${mins}m';
  }
}
