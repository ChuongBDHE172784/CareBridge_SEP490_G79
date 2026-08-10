import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import '../models/health_metric_model.dart';
import '../services/health_metric_service.dart';
import '../../../core/network/api_client.dart';

/// CB-157 — Maternal Health Metric Detail (UC-187, UC-188)
/// Shows full detail for a single health metric: value card, detail table,
/// mini bar chart trend, edit/delete actions.
/// Calls GET /api/v1/health-metrics/{metricId} and DELETE /api/v1/health-metrics/{metricId}.
class MaternalHealthMetricScreen extends StatefulWidget {
  final String metricId;
  final HealthMetricDetail? initialMetric;

  const MaternalHealthMetricScreen({
    super.key,
    required this.metricId,
    this.initialMetric,
  });

  @override
  State<MaternalHealthMetricScreen> createState() =>
      _MaternalHealthMetricScreenState();
}

class _MaternalHealthMetricScreenState
    extends State<MaternalHealthMetricScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFF8F5F1);
  static const _surface = Color(0xFFFFFCF9);
  static const _onSurface = Color(0xFF2A211D);
  static const _onSurfaceVariant = Color(0xFF655650);

  final _service = HealthMetricService();
  HealthMetricDetail? _metric;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
        statusBarColor: Colors.transparent,
        statusBarIconBrightness: Brightness.dark,
        statusBarBrightness: Brightness.light,
      ));
    });
    if (widget.initialMetric != null) {
      _metric = widget.initialMetric;
      _loading = false;
      return;
    }
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final m = await _service.getMetricDetail(widget.metricId);
      if (mounted) {
        setState(() {
          _metric = m;
          _loading = false;
        });
      }
    } on ApiException {
      if (mounted) {
        setState(() {
          _metric = null;
          _error =
              'Không thể tải chỉ số. Vui lòng kiểm tra quyền truy cập hoặc thử lại.';
          _loading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _error = 'Lỗi kết nối.';
          _loading = false;
        });
      }
    }
  }

  Future<void> _confirmDelete() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: const Text(
          'Xóa chỉ số?',
          style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w600),
        ),
        content: const Text(
          'Hành động này không thể khôi phục.',
          style: TextStyle(fontFamily: 'Lexend', fontSize: 14),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Hủy', style: TextStyle(fontFamily: 'Lexend')),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            style: FilledButton.styleFrom(
              backgroundColor: const Color(0xFFBA1A1A),
            ),
            child: const Text('Xóa', style: TextStyle(fontFamily: 'Lexend')),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;
    try {
      await _service.deleteMetric(widget.metricId);
      if (!mounted) return;
      Navigator.pop(context, true);
    } on ApiException {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Không thể xóa. Vui lòng thử lại.')),
      );
    }
  }

  Future<void> _openEdit(HealthMetricDetail metric) async {
    final updated = await context.push<bool>(
      '/health-metrics/${Uri.encodeComponent(metric.id)}/edit',
      extra: {'journeyId': metric.journeyId, 'metric': metric},
    );
    if (updated == true && mounted) {
      await _load();
    }
  }

  String _formatDate(DateTime dt) {
    final d = dt.day.toString().padLeft(2, '0');
    final mo = dt.month.toString().padLeft(2, '0');
    final y = dt.year;
    return '$d/$mo/$y';
  }

  String _formatTime(DateTime dt) {
    final mi = dt.minute.toString().padLeft(2, '0');
    final ampm = dt.hour < 12 ? 'AM' : 'PM';
    final hour12 = dt.hour == 0
        ? 12
        : dt.hour > 12
        ? dt.hour - 12
        : dt.hour;
    return '${hour12.toString().padLeft(2, '0')}:$mi $ampm';
  }

  String _formatGlucoseContext(String code) {
    switch (code) {
      case 'FASTING':
        return 'Lúc đói (nhịn ăn >= 8h)';
      case 'POST_PRANDIAL_1H':
        return '1 giờ sau ăn';
      case 'POST_PRANDIAL_2H':
        return '2 giờ sau ăn';
      case 'RANDOM':
        return 'Bất kỳ trong ngày';
      case 'BEDTIME':
        return 'Trước khi đi ngủ';
      default:
        return code;
    }
  }

  @override
  Widget build(BuildContext context) {
    final topInset = MediaQuery.of(context).padding.top;
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: const SystemUiOverlayStyle(
        statusBarColor: Colors.transparent,
        statusBarIconBrightness: Brightness.dark,
        statusBarBrightness: Brightness.light,
      ),
      child: Scaffold(
        backgroundColor: _canvas,
        body: _loading
            ? Center(
                child: Padding(
                  padding: EdgeInsets.only(top: topInset),
                  child: const CircularProgressIndicator(color: _primaryContainer),
                ),
              )
            : _error != null
            ? _buildErrorState(topInset)
            : _buildContent(_metric!, topInset),
      ),
    );
  }

  Widget _buildErrorState(double topInset) {
    return Padding(
      padding: EdgeInsets.only(top: topInset),
      child: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, size: 48, color: Color(0xFFBA1A1A)),
            const SizedBox(height: 12),
            Text(
              _error!,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                color: _onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 16),
            TextButton(
              onPressed: _load,
              child: const Text(
                'Thử lại',
                style: TextStyle(fontFamily: 'Lexend', color: _primary),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildContent(HealthMetricDetail m, double topInset) {
    return Column(
      children: [
        _buildAppBar(topInset),
        Expanded(
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(20, 16, 20, 24),
            child: Column(
              children: [
                _buildMetricCard(m),
                const SizedBox(height: 16),
                _buildDetailsCard(m),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildAppBar(double topInset) {
    final m = _metric;
    return Container(
      color: _surface,
      // height = standard 56 + status bar so AppBar fills behind Dynamic Island
      height: 56 + topInset,
      padding: EdgeInsets.only(top: topInset),
      child: Row(
        children: [
          IconButton(
            onPressed: () => Navigator.pop(context),
            icon: const Icon(Icons.arrow_back_rounded, color: _primary),
          ),
          Expanded(
            child: Text(
              m != null ? 'Chi tiết ${m.metricType.displayLabel}' : 'Chi tiết chỉ số',
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 20,
                fontWeight: FontWeight.w700,
                color: _primary,
                letterSpacing: -0.24,
              ),
            ),
          ),
          if (m != null && widget.initialMetric == null)
            PopupMenuButton<String>(
              icon: const Icon(Icons.more_vert, color: _primary),
              onSelected: (value) {
                if (value == 'edit') {
                  _openEdit(m);
                } else if (value == 'delete') {
                  _confirmDelete();
                }
              },
              itemBuilder: (context) {
                final isFetalMovement = m.metricCode == 'FETAL_MOVEMENT_SESSION' ||
                    m.metricCode == 'FETAL_MOVEMENT_COUNT' ||
                    m.metricCode == 'FETAL_MOVEMENT';
                return [
                  if (!isFetalMovement)
                    const PopupMenuItem(
                      value: 'edit',
                      child: Row(
                        children: [
                          Icon(Icons.edit_outlined, color: _primary, size: 18),
                          SizedBox(width: 8),
                          Text('Chỉnh sửa', style: TextStyle(fontFamily: 'Lexend')),
                        ],
                      ),
                    ),
                  const PopupMenuItem(
                    value: 'delete',
                    child: Row(
                      children: [
                        Icon(Icons.delete_outline, color: Color(0xFFBA1A1A), size: 18),
                        SizedBox(width: 8),
                        Text('Xóa chỉ số', style: TextStyle(fontFamily: 'Lexend', color: Color(0xFFBA1A1A))),
                      ],
                    ),
                  ),
                ];
              },
            )
          else
            const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildMetricCard(HealthMetricDetail m) {
    return _Card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: _primaryContainer.withAlpha(51),
                  shape: BoxShape.circle,
                ),
                child: Icon(
                  m.metricCode == 'BMI'
                      ? Icons.calculate_outlined
                      : Icons.monitor_heart_outlined,
                  color: _primary,
                  size: 20,
                ),
              ),
              const SizedBox(width: 12),
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    m.metricType.displayLabel,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 20,
                      fontWeight: FontWeight.w600,
                      color: _onSurface,
                    ),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 16),
          Row(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text(
                _displayMetricValue(m),
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 40,
                  fontWeight: FontWeight.w700,
                  color: _primary,
                  height: 1,
                ),
              ),
              const SizedBox(width: 8),
              Padding(
                padding: const EdgeInsets.only(bottom: 4),
                child: Text(
                  m.unit,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 16,
                    color: _onSurfaceVariant,
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildDetailsCard(HealthMetricDetail m) {
    final bmiWeight = _metricContextNumber(m.context['weightKg']);
    final bmiHeight = _metricContextNumber(m.context['heightCm']);
    return _Card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Thông tin chi tiết',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 20,
              fontWeight: FontWeight.w600,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 16),
          _DetailRow(
            icon: Icons.calendar_today_outlined,
            label: 'Ngày đo',
            value: _formatDate(m.measuredAt),
            showDivider: true,
          ),
          _DetailRow(
            icon: Icons.schedule_outlined,
            label: 'Thời gian',
            value: _formatTime(m.measuredAt),
            showDivider: true,
          ),
          if (m.metricCode == 'BMI' && bmiWeight != null)
            _DetailRow(
              icon: Icons.monitor_weight_outlined,
              label: 'Cân nặng',
              value: '${bmiWeight.toStringAsFixed(1)} kg',
              showDivider: true,
            ),
          if (m.metricCode == 'BMI' && bmiHeight != null)
            _DetailRow(
              icon: Icons.height_rounded,
              label: 'Chiều cao',
              value: '${bmiHeight.toStringAsFixed(1)} cm',
              showDivider: true,
            ),
          if (m.metricCode == 'BLOOD_GLUCOSE' &&
              m.context['measurementContext'] != null &&
              m.context['measurementContext'].toString().isNotEmpty)
            _DetailRow(
              icon: Icons.restaurant_outlined,
              label: 'Bối cảnh đo',
              value: _formatGlucoseContext(m.context['measurementContext'].toString()),
              showDivider: true,
            ),
          _DetailRow(
            icon: Icons.devices_outlined,
            label: 'Nguồn',
            value: m.sourceType.displayLabel,
            showDivider: m.note != null && m.note!.isNotEmpty,
          ),
          if (m.note != null && m.note!.isNotEmpty)
            _NoteRow(note: _displayMetricNote(m.metricCode, m.note!)),
        ],
      ),
    );
  }

}

double? _metricContextNumber(Object? value) {
  if (value is num) return value.toDouble();
  return double.tryParse(value?.toString() ?? '');
}

String _displayMetricValue(HealthMetricDetail metric) {
  if (metric.metricCode == 'FETAL_MOVEMENT_SESSION' ||
      metric.metricCode == 'FETAL_MOVEMENT_COUNT') {
    return '${metric.valueNumeric.toStringAsFixed(0)} cử động';
  }
  return metric.valueDisplay;
}

String _displayMetricNote(String metricCode, String note) {
  if (metricCode != 'FETAL_MOVEMENT_SESSION' &&
      metricCode != 'FETAL_MOVEMENT_COUNT') {
    return note;
  }
  return switch (note.trim().toUpperCase()) {
    'KICK' => 'Bé đạp',
    'ROLL' => 'Bé xoay người',
    'STRETCH' => 'Bé co duỗi',
    'HICCUP' => 'Bé nấc cụt',
    _ => 'Cử động thai',
  };
}

// ─── Sub-widgets ──────────────────────────────────────────────────────────────

class _Card extends StatelessWidget {
  final Widget child;

  const _Card({required this.child});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF8F6),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFFE5D3CA), width: 1),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF5A463F).withAlpha(18),
            blurRadius: 16,
            offset: const Offset(0, 3),
          ),
        ],
      ),
      child: child,
    );
  }
}

class _DetailRow extends StatelessWidget {
  final IconData icon;
  final String label;
  final String value;
  final bool showDivider;

  const _DetailRow({
    required this.icon,
    required this.label,
    required this.value,
    this.showDivider = false,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 8),
          child: Row(
            children: [
              Icon(icon, size: 18, color: const Color(0xFF524440)),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  label,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    fontWeight: FontWeight.w500,
                    color: Color(0xFF524440),
                  ),
                ),
              ),
              Text(
                value,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  fontWeight: FontWeight.w500,
                  color: Color(0xFF271812),
                ),
              ),
            ],
          ),
        ),
        if (showDivider) const Divider(height: 1, color: Color(0xFFFADCD3)),
      ],
    );
  }
}

class _NoteRow extends StatelessWidget {
  final String note;

  const _NoteRow({required this.note});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: const [
              Icon(Icons.edit_note, size: 18, color: Color(0xFF524440)),
              SizedBox(width: 8),
              Text(
                'Ghi chú',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  fontWeight: FontWeight.w500,
                  color: Color(0xFF524440),
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: const Color(0xFFFFF1EC),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Text(
              note,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                fontWeight: FontWeight.w500,
                color: Color(0xFF271812),
                height: 1.5,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
