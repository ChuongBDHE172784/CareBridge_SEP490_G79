import 'dart:math' as math;
import 'package:flutter/material.dart';
import '../models/health_metric_model.dart';
import '../services/health_metric_service.dart';

class HealthMetricTrendScreen extends StatefulWidget {
  final String journeyId;
  final String? initialMetricType;

  const HealthMetricTrendScreen({
    super.key,
    required this.journeyId,
    this.initialMetricType,
  });

  @override
  State<HealthMetricTrendScreen> createState() =>
      _HealthMetricTrendScreenState();
}

class _HealthMetricTrendScreenState extends State<HealthMetricTrendScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surfaceContainer = Color(0xFFF2EAE4);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  static const _metricOptions = [
    _MetricOption(
      apiValue: 'BLOOD_PRESSURE_SYSTOLIC',
      label: 'Huyết áp tâm thu',
      unit: 'mmHg',
      icon: Icons.favorite_border_rounded,
    ),
    _MetricOption(
      apiValue: 'BLOOD_PRESSURE_DIASTOLIC',
      label: 'Huyết áp tâm trương',
      unit: 'mmHg',
      icon: Icons.favorite_border_rounded,
    ),
    _MetricOption(
      apiValue: 'WEIGHT',
      label: 'Cân nặng',
      unit: 'kg',
      icon: Icons.monitor_weight_outlined,
    ),
    _MetricOption(
      apiValue: 'HEART_RATE',
      label: 'Nhịp tim',
      unit: 'bpm',
      icon: Icons.show_chart_rounded,
    ),
    _MetricOption(
      apiValue: 'BLOOD_GLUCOSE',
      label: 'Đường huyết',
      unit: 'mg/dL',
      icon: Icons.water_drop_outlined,
    ),
    _MetricOption(
      apiValue: 'TEMPERATURE',
      label: 'Nhiệt độ',
      unit: '°C',
      icon: Icons.thermostat_rounded,
    ),
  ];

  late _MetricOption _selectedMetric;
  int _periodDays = 7;
  bool _isLoading = false;
  String? _errorMsg;
  MetricTrend? _trend;

  final _service = HealthMetricService();

  @override
  void initState() {
    super.initState();
    _selectedMetric = _metricOptions.firstWhere(
      (o) =>
          o.apiValue == (widget.initialMetricType ?? 'BLOOD_PRESSURE_SYSTOLIC'),
      orElse: () => _metricOptions.first,
    );
    _loadTrend();
  }

  Future<void> _loadTrend() async {
    setState(() {
      _isLoading = true;
      _errorMsg = null;
    });
    try {
      final from = DateTime.now().subtract(Duration(days: _periodDays));
      final result = await _service.getMetricTrend(
        journeyId: widget.journeyId,
        metricType: _selectedMetric.apiValue,
        from: from,
      );
      setState(() => _trend = result);
    } catch (e) {
      setState(() => _errorMsg = 'Không thể tải dữ liệu. Vui lòng thử lại.');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  void _onMetricChanged(_MetricOption? opt) {
    if (opt == null) return;
    setState(() => _selectedMetric = opt);
    _loadTrend();
  }

  void _onPeriodChanged(int days) {
    setState(() => _periodDays = days);
    _loadTrend();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      appBar: AppBar(
        backgroundColor: _canvas,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded, color: _onSurface),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: const Text(
          'Chỉ số sức khỏe',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 18,
            fontWeight: FontWeight.w700,
            color: _primary,
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.close_rounded, color: _onSurfaceVariant),
            onPressed: () => Navigator.of(context).pop(),
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _buildFilterRow(),
            const SizedBox(height: 16),
            if (_isLoading)
              const Center(
                child: Padding(
                  padding: EdgeInsets.all(48),
                  child: CircularProgressIndicator(color: _primaryContainer),
                ),
              )
            else if (_errorMsg != null)
              _buildErrorCard()
            else ...[
              _buildChartCard(),
              const SizedBox(height: 16),
              _buildBentoSummary(),
              const SizedBox(height: 16),
              _buildWarningCard(),
              const SizedBox(height: 20),
              _buildActionButtons(),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildFilterRow() {
    return Row(
      children: [
        Expanded(
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 2),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(20),
              border: Border.all(color: _surfaceContainer, width: 1.5),
            ),
            child: DropdownButtonHideUnderline(
              child: DropdownButton<_MetricOption>(
                value: _selectedMetric,
                isExpanded: true,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  color: _onSurface,
                ),
                items: _metricOptions
                    .map(
                      (o) => DropdownMenuItem(value: o, child: Text(o.label)),
                    )
                    .toList(),
                onChanged: _onMetricChanged,
                dropdownColor: _canvas,
                borderRadius: BorderRadius.circular(20),
              ),
            ),
          ),
        ),
        const SizedBox(width: 12),
        Container(
          padding: const EdgeInsets.all(4),
          decoration: BoxDecoration(
            color: _surfaceContainer,
            borderRadius: BorderRadius.circular(20),
          ),
          child: Row(
            children: [
              _PeriodPill(
                label: '7 ngày',
                active: _periodDays == 7,
                onTap: () => _onPeriodChanged(7),
              ),
              _PeriodPill(
                label: '30 ngày',
                active: _periodDays == 30,
                onTap: () => _onPeriodChanged(30),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildErrorCard() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
      ),
      child: Column(
        children: [
          const Icon(
            Icons.error_outline_rounded,
            color: _primaryContainer,
            size: 48,
          ),
          const SizedBox(height: 12),
          Text(
            _errorMsg!,
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 16),
          TextButton(
            onPressed: _loadTrend,
            child: const Text('Thử lại', style: TextStyle(color: _primary)),
          ),
        ],
      ),
    );
  }

  Widget _buildChartCard() {
    final points = _trend?.dataPoints ?? [];
    final avg = _trend?.average ?? 0;
    final trendPct = _trend?.trend;
    final unit = _trend?.unit ?? _selectedMetric.unit;
    final isDown = (trendPct ?? 0) < 0;

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: _primary.withAlpha(18),
            blurRadius: 16,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '${avg.toStringAsFixed(avg % 1 == 0 ? 0 : 1)} $unit',
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 22,
                        fontWeight: FontWeight.w700,
                        color: _onSurface,
                      ),
                    ),
                    const Text(
                      'Trung bình giai đoạn này',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        color: _onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              if (trendPct != null)
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 6,
                  ),
                  decoration: BoxDecoration(
                    color: _primary.withAlpha(25),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Row(
                    children: [
                      Icon(
                        isDown
                            ? Icons.trending_down_rounded
                            : Icons.trending_up_rounded,
                        color: _primary,
                        size: 16,
                      ),
                      const SizedBox(width: 4),
                      Text(
                        '${trendPct.abs().toStringAsFixed(1)}%',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 12,
                          fontWeight: FontWeight.w600,
                          color: _primary,
                        ),
                      ),
                    ],
                  ),
                ),
            ],
          ),
          const SizedBox(height: 20),
          if (points.isEmpty)
            const Center(
              child: Padding(
                padding: EdgeInsets.all(32),
                child: Text(
                  'Chưa có dữ liệu trong giai đoạn này',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 13,
                    color: _onSurfaceVariant,
                  ),
                ),
              ),
            )
          else ...[
            SizedBox(
              height: 160,
              child: CustomPaint(
                painter: _LineChartPainter(
                  points: points,
                  lineColor: _primaryContainer,
                  gridColor: _surfaceContainer,
                ),
                child: Container(),
              ),
            ),
            const SizedBox(height: 8),
            _buildXAxisLabels(points),
          ],
        ],
      ),
    );
  }

  Widget _buildXAxisLabels(List<MetricDataPoint> points) {
    if (points.isEmpty) return const SizedBox.shrink();
    final count = math.min(7, points.length);
    final step = points.length ~/ count;
    final labels = <String>[];
    for (var i = 0; i < count; i++) {
      final idx = i * step;
      if (idx < points.length) {
        final dt = points[idx].measuredAt;
        labels.add(_dayLabel(dt.weekday));
      }
    }
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: labels
          .map(
            (l) => Text(
              l,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 10,
                color: _onSurfaceVariant,
              ),
            ),
          )
          .toList(),
    );
  }

  String _dayLabel(int weekday) {
    const days = ['Th 2', 'Th 3', 'Th 4', 'Th 5', 'Th 6', 'Th 7', 'CN'];
    return days[(weekday - 1) % 7];
  }

  Widget _buildBentoSummary() {
    return Row(
      children: [
        Expanded(
          child: _buildBentoCard(
            icon: Icons.monitor_weight_outlined,
            label: 'Cân nặng',
            value: '—',
            unit: 'kg',
            trend: null,
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: _buildBentoCard(
            icon: Icons.show_chart_rounded,
            label: 'Nhịp tim',
            value: '—',
            unit: 'bpm',
            trend: null,
          ),
        ),
      ],
    );
  }

  Widget _buildBentoCard({
    required IconData icon,
    required String label,
    required String value,
    required String unit,
    double? trend,
  }) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFF6F1EC),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: Colors.white, width: 2),
        boxShadow: [
          BoxShadow(
            color: _primary.withAlpha(12),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: _primaryContainer, size: 22),
          const SizedBox(height: 8),
          Text(
            label,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 12,
              color: _onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            '$value $unit',
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 16,
              fontWeight: FontWeight.w700,
              color: _onSurface,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildWarningCard() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFFFE9E0),
        borderRadius: BorderRadius.circular(20),
        border: const Border(left: BorderSide(color: _primary, width: 4)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(Icons.info_rounded, color: _primary, size: 20),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Lưu ý từ hệ thống',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: _primary,
                  ),
                ),
                const SizedBox(height: 4),
                const Text(
                  'Nếu các chỉ số có xu hướng bất thường, hãy tham khảo ý kiến bác sĩ ngay.',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    color: _onSurfaceVariant,
                    height: 1.5,
                  ),
                ),
                const SizedBox(height: 8),
                GestureDetector(
                  onTap: () {},
                  child: const Text(
                    'Trò chuyện ngay →',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                      color: _primary,
                    ),
                  ),
                ),
              ],
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
          child: ElevatedButton.icon(
            onPressed: () {},
            icon: const Icon(Icons.add_circle_outline_rounded, size: 18),
            label: const Text(
              'Thêm chỉ số',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontWeight: FontWeight.w600,
              ),
            ),
            style: ElevatedButton.styleFrom(
              backgroundColor: _primary,
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(vertical: 14),
              shape: const StadiumBorder(),
              elevation: 0,
            ),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: OutlinedButton(
            onPressed: () {},
            style: OutlinedButton.styleFrom(
              foregroundColor: _primary,
              padding: const EdgeInsets.symmetric(vertical: 14),
              side: const BorderSide(color: _surfaceContainer, width: 1.5),
              shape: const StadiumBorder(),
            ),
            child: const Text(
              'Xem chi tiết',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _MetricOption {
  final String apiValue;
  final String label;
  final String unit;
  final IconData icon;

  const _MetricOption({
    required this.apiValue,
    required this.label,
    required this.unit,
    required this.icon,
  });

  @override
  bool operator ==(Object other) =>
      other is _MetricOption && other.apiValue == apiValue;

  @override
  int get hashCode => apiValue.hashCode;
}

class _PeriodPill extends StatelessWidget {
  final String label;
  final bool active;
  final VoidCallback onTap;

  const _PeriodPill({
    required this.label,
    required this.active,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        decoration: BoxDecoration(
          color: active ? const Color(0xFFC98C7B) : Colors.transparent,
          borderRadius: BorderRadius.circular(20),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 12,
            fontWeight: FontWeight.w600,
            color: active ? Colors.white : const Color(0xFF524440),
          ),
        ),
      ),
    );
  }
}

class _LineChartPainter extends CustomPainter {
  final List<MetricDataPoint> points;
  final Color lineColor;
  final Color gridColor;

  const _LineChartPainter({
    required this.points,
    required this.lineColor,
    required this.gridColor,
  });

  @override
  void paint(Canvas canvas, Size size) {
    if (points.isEmpty) return;

    // Grid lines
    final gridPaint = Paint()
      ..color = gridColor
      ..strokeWidth = 1
      ..style = PaintingStyle.stroke;
    for (var i = 0; i <= 4; i++) {
      final y = size.height * i / 4;
      canvas.drawLine(Offset(0, y), Offset(size.width, y), gridPaint);
    }

    final values = points.map((p) => p.valueNumeric).toList();
    final minVal = values.reduce(math.min);
    final maxVal = values.reduce(math.max);
    final range = (maxVal - minVal) == 0 ? 1.0 : (maxVal - minVal);

    final n = points.length;
    Offset toOffset(int i) {
      final x = n == 1 ? size.width / 2 : size.width * i / (n - 1);
      final norm = (points[i].valueNumeric - minVal) / range;
      final y = size.height * (1 - norm * 0.8 - 0.1);
      return Offset(x, y);
    }

    final positions = List.generate(n, toOffset);

    // Bezier path
    final path = Path();
    path.moveTo(positions[0].dx, positions[0].dy);
    for (var i = 1; i < positions.length; i++) {
      final prev = positions[i - 1];
      final curr = positions[i];
      final cpX = (prev.dx + curr.dx) / 2;
      path.cubicTo(cpX, prev.dy, cpX, curr.dy, curr.dx, curr.dy);
    }

    final linePaint = Paint()
      ..color = lineColor
      ..strokeWidth = 3
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round;
    canvas.drawPath(path, linePaint);

    // Dots
    final dotPaint = Paint()
      ..color = lineColor
      ..style = PaintingStyle.fill;
    final dotBorder = Paint()
      ..color = Colors.white
      ..strokeWidth = 2
      ..style = PaintingStyle.stroke;
    for (final pos in positions) {
      canvas.drawCircle(
        pos,
        5,
        Paint()
          ..color = Colors.white
          ..style = PaintingStyle.fill,
      );
      canvas.drawCircle(pos, 4, dotPaint);
      canvas.drawCircle(pos, 5, dotBorder);
    }
  }

  @override
  bool shouldRepaint(_LineChartPainter old) => old.points != points;
}
