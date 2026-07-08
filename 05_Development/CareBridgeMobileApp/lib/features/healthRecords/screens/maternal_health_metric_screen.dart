import 'package:flutter/material.dart';
import '../models/health_metric_model.dart';
import '../services/health_metric_service.dart';
import '../../../core/network/api_client.dart';

/// CB-157 — Maternal Health Metric Detail (UC-187, UC-188)
/// Shows full detail for a single health metric: value card, detail table,
/// mini bar chart trend, edit/delete actions.
/// Calls GET /api/v1/health-metrics/{metricId} and DELETE /api/v1/health-metrics/{metricId}.
class MaternalHealthMetricScreen extends StatefulWidget {
  final String metricId;

  const MaternalHealthMetricScreen({super.key, required this.metricId});

  @override
  State<MaternalHealthMetricScreen> createState() =>
      _MaternalHealthMetricScreenState();
}

class _MaternalHealthMetricScreenState
    extends State<MaternalHealthMetricScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFF6F1EC);
  static const _surface = Color(0xFFFFF8F6);
  static const _surfaceContainerHigh = Color(0xFFFFE2D9);
  static const _surfaceContainerHighest = Color(0xFFFADCD3);
  static const _surfaceContainerLow = Color(0xFFFFF1EC);
  static const _surfaceVariant = Color(0xFFFADCD3);
  static const _secondary = Color(0xFF6E5A52);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _errorContainer = Color(0xFFFFDAD6);
  static const _onErrorContainer = Color(0xFF93000A);

  final _service = HealthMetricService();
  HealthMetricDetail? _metric;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
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
      // Fallback mock for development
      if (mounted) {
        setState(() {
          final isBp = widget.metricId == 'blood_pressure';
          _metric = HealthMetricDetail(
            id: widget.metricId,
            journeyId: 'mock-journey-1',
            metricType: isBp ? MetricType.bloodPressure : MetricType.weight,
            valueNumeric: isBp ? 120 : 62.5,
            valueSecondary: isBp ? 80 : null,
            unit: isBp ? 'mmHg' : 'kg',
            measuredAt: DateTime.now().subtract(const Duration(hours: 2)),
            sourceType: SourceType.manual,
            note: isBp
                ? 'Huyết áp bình thường, đo sau khi nghỉ ngơi.'
                : 'Cân nặng tăng nhẹ, ổn định.',
            createdAt: DateTime.now(),
          );
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

  String _formatDate(DateTime dt) {
    final d = dt.day.toString().padLeft(2, '0');
    final mo = dt.month.toString().padLeft(2, '0');
    final y = dt.year;
    return '$d/$mo/$y';
  }

  String _formatTime(DateTime dt) {
    final h = dt.hour.toString().padLeft(2, '0');
    final mi = dt.minute.toString().padLeft(2, '0');
    final ampm = dt.hour < 12 ? 'AM' : 'PM';
    final hour12 = dt.hour == 0
        ? 12
        : dt.hour > 12
        ? dt.hour - 12
        : dt.hour;
    return '${hour12.toString().padLeft(2, '0')}:$mi $ampm';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: _loading
            ? const Center(
                child: CircularProgressIndicator(color: _primaryContainer),
              )
            : _error != null
            ? _buildErrorState()
            : _buildContent(_metric!),
      ),
    );
  }

  Widget _buildErrorState() {
    return Center(
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
    );
  }

  Widget _buildContent(HealthMetricDetail m) {
    return Column(
      children: [
        _buildAppBar(),
        Expanded(
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(20, 16, 20, 24),
            child: Column(
              children: [
                _buildMetricCard(m),
                const SizedBox(height: 8),
                _buildDetailsCard(m),
                const SizedBox(height: 8),
                _buildTrendCard(),
                const SizedBox(height: 24),
                _buildActionButtons(),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildAppBar() {
    return Container(
      color: _surface,
      height: 64,
      child: Row(
        children: [
          IconButton(
            onPressed: () => Navigator.pop(context),
            icon: const Icon(Icons.arrow_back, color: _onSurfaceVariant),
          ),
          const Expanded(
            child: Text(
              'Chi tiết chỉ số',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 24,
                fontWeight: FontWeight.w700,
                color: _primary,
                letterSpacing: -0.24,
              ),
            ),
          ),
          IconButton(
            onPressed: () {
              // TODO: more options (history, share) UC-187
            },
            icon: const Icon(Icons.more_vert, color: _onSurfaceVariant),
          ),
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
                child: const Icon(
                  Icons.monitor_weight,
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
                  Text(
                    'Hôm nay, ${_formatTime(m.measuredAt)}',
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 14,
                      color: _onSurfaceVariant,
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
                m.valueDisplay,
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
          const SizedBox(height: 12),
          _TrendBadge(),
        ],
      ),
    );
  }

  Widget _buildDetailsCard(HealthMetricDetail m) {
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
          _DetailRow(
            icon: Icons.devices_outlined,
            label: 'Nguồn',
            value: m.sourceType.displayLabel,
            showDivider: m.note != null && m.note!.isNotEmpty,
          ),
          if (m.note != null && m.note!.isNotEmpty) _NoteRow(note: m.note!),
        ],
      ),
    );
  }

  Widget _buildTrendCard() {
    return _Card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Xu hướng (Tháng này)',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 20,
                  fontWeight: FontWeight.w600,
                  color: _onSurface,
                ),
              ),
              const Icon(Icons.insights_outlined, color: _primary, size: 20),
            ],
          ),
          const SizedBox(height: 16),
          // TODO: wire to GET /api/v1/health-metrics?journeyId=X&type=X&period=month (UC-187)
          SizedBox(
            height: 128,
            child: CustomPaint(
              painter: _BarChartPainter(),
              size: Size.infinite,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildActionButtons() {
    return Row(
      children: [
        Expanded(
          child: SizedBox(
            height: 48,
            child: OutlinedButton.icon(
              onPressed: () {
                // TODO: navigate to edit metric screen (UC-187)
              },
              style: OutlinedButton.styleFrom(
                foregroundColor: _primary,
                side: const BorderSide(color: _primary, width: 2),
                shape: const StadiumBorder(),
              ),
              icon: const Icon(Icons.edit_outlined, size: 20),
              label: const Text(
                'Sửa',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
          ),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: SizedBox(
            height: 48,
            child: FilledButton.icon(
              onPressed: _confirmDelete,
              style: FilledButton.styleFrom(
                backgroundColor: _errorContainer,
                foregroundColor: _onErrorContainer,
                shape: const StadiumBorder(),
              ),
              icon: const Icon(Icons.delete_outline, size: 20),
              label: const Text(
                'Xóa',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
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
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF5A463F).withAlpha(20),
            blurRadius: 20,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: child,
    );
  }
}

class _TrendBadge extends StatelessWidget {
  const _TrendBadge();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
      decoration: BoxDecoration(
        color: const Color(0xFFFFE2D9),
        borderRadius: BorderRadius.circular(99),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: const [
          Icon(Icons.trending_up, size: 16, color: Color(0xFF845143)),
          SizedBox(width: 4),
          Text(
            '+0.5kg so với tuần trước',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 12,
              fontWeight: FontWeight.w700,
              letterSpacing: 0.06,
              color: Color(0xFF845143),
            ),
          ),
        ],
      ),
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

// Simple bar chart painter (4 bars, last is active/current month)
class _BarChartPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    const barHeights = [0.40, 0.45, 0.50, 0.65];
    const labels = ['T1', 'T2', 'T3', 'HT'];
    final barCount = barHeights.length;
    final totalGap = size.width * 0.35;
    final barWidth = (size.width - totalGap) / barCount;
    final gap = totalGap / (barCount + 1);
    const bottomPad = 24.0;
    final chartHeight = size.height - bottomPad;

    final inactivePaint = Paint()
      ..color = const Color(0xFFFADCD3)
      ..style = PaintingStyle.fill;
    final activePaint = Paint()
      ..color = const Color(0xFFC98C7B)
      ..style = PaintingStyle.fill;

    final textStyle = const TextStyle(
      fontFamily: 'Lexend',
      fontSize: 10,
      color: Color(0xFF524440),
    );

    for (int i = 0; i < barCount; i++) {
      final x = gap * (i + 1) + barWidth * i;
      final barH = chartHeight * barHeights[i];
      final rect = RRect.fromRectAndCorners(
        Rect.fromLTWH(x, chartHeight - barH, barWidth, barH),
        topLeft: const Radius.circular(4),
        topRight: const Radius.circular(4),
      );
      canvas.drawRRect(rect, i == barCount - 1 ? activePaint : inactivePaint);

      // Label
      final tp = TextPainter(
        text: TextSpan(
          text: labels[i],
          style: textStyle.copyWith(
            fontWeight: i == barCount - 1 ? FontWeight.bold : FontWeight.normal,
            color: i == barCount - 1
                ? const Color(0xFF845143)
                : const Color(0xFF524440),
          ),
        ),
        textDirection: TextDirection.ltr,
      )..layout();
      tp.paint(canvas, Offset(x + (barWidth - tp.width) / 2, chartHeight + 6));

      // Tooltip on last bar
      if (i == barCount - 1) {
        final tooltipPaint = Paint()
          ..color = const Color(0xFF271812)
          ..style = PaintingStyle.fill;
        final tooltipY = chartHeight - barH - 28;
        const tooltipText = '62.5';
        final ttp = TextPainter(
          text: TextSpan(
            text: tooltipText,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 10,
              color: Colors.white,
            ),
          ),
          textDirection: TextDirection.ltr,
        )..layout();
        final tooltipRect = RRect.fromRectAndRadius(
          Rect.fromLTWH(x - 4, tooltipY, ttp.width + 16, 20),
          const Radius.circular(6),
        );
        canvas.drawRRect(tooltipRect, tooltipPaint);
        ttp.paint(canvas, Offset(x + 4, tooltipY + 4));
      }
    }

    // Grid lines
    final gridPaint = Paint()
      ..color = const Color(0xFFFADCD3)
      ..strokeWidth = 1;
    canvas.drawLine(
      Offset(0, chartHeight * 0.35),
      Offset(size.width, chartHeight * 0.35),
      gridPaint,
    );
    canvas.drawLine(
      Offset(0, chartHeight),
      Offset(size.width, chartHeight),
      gridPaint,
    );
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
