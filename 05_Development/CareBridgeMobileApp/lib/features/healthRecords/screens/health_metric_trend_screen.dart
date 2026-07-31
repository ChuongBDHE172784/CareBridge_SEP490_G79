import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

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
  static const _surface = Colors.white;
  static const _surfaceContainer = Color(0xFFF2EAE4);
  static const _surfaceAccent = Color(0xFFF6F1EC);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _error = Color(0xFFBA1A1A);

  static const _fallbackMetricOptions = [
    _MetricOption(
      apiValue: 'WEIGHT',
      label: 'Cân nặng',
      unit: 'kg',
      icon: Icons.monitor_weight_outlined,
    ),
    _MetricOption(
      apiValue: 'BLOOD_PRESSURE',
      label: 'Huyết áp',
      unit: 'mmHg',
      icon: Icons.favorite_border_rounded,
    ),
    _MetricOption(
      apiValue: 'BLOOD_GLUCOSE',
      label: 'Đường huyết',
      unit: 'mg/dL',
      icon: Icons.water_drop_outlined,
    ),
    _MetricOption(
      apiValue: 'FETAL_MOVEMENT_SESSION',
      label: 'Cử động thai',
      unit: 'count',
      icon: Icons.child_friendly_rounded,
    ),
  ];

  final _service = HealthMetricService();
  final _historyScrollController = ScrollController();

  late _MetricOption _selectedMetric;
  List<_MetricOption> _metricOptions = _fallbackMetricOptions;
  bool _isLoading = false;
  bool _isLoadingCapabilities = true;
  String? _errorMsg;
  MetricTrend? _trend;

  List<MetricDataPoint> get _historyPoints {
    final points = [...?_trend?.dataPoints];
    points.sort((a, b) => b.measuredAt.compareTo(a.measuredAt));
    return points;
  }

  @override
  void initState() {
    super.initState();
    _selectedMetric = _fallbackMetricOptions.first;
    _loadCapabilities();
  }

  Future<void> _loadCapabilities() async {
    try {
      final capabilities = await _service.getCapabilities(widget.journeyId);
      if (!mounted) return;
      final options = capabilities
          .where((capability) => capability.manualEntrySupported)
          .map(_optionFromCapability)
          .toList();
      final resolvedOptions = options.isEmpty
          ? _fallbackMetricOptions
          : options;
      final requested = _canonicalMetricType(widget.initialMetricType);
      setState(() {
        _metricOptions = resolvedOptions;
        _selectedMetric = resolvedOptions.firstWhere(
          (option) => option.apiValue == requested,
          orElse: () => resolvedOptions.first,
        );
        _isLoadingCapabilities = false;
      });
      await _loadTrend();
    } catch (_) {
      if (!mounted) return;
      final requested = _canonicalMetricType(widget.initialMetricType);
      setState(() {
        _metricOptions = _fallbackMetricOptions;
        _selectedMetric = _fallbackMetricOptions.firstWhere(
          (option) => option.apiValue == requested,
          orElse: () => _fallbackMetricOptions.first,
        );
        _isLoadingCapabilities = false;
      });
      await _loadTrend();
    }
  }

  _MetricOption _optionFromCapability(MetricCapability capability) {
    final code = _canonicalMetricType(capability.metricCode);
    final fallback = _fallbackMetricOptions.firstWhere(
      (option) => option.apiValue == code,
      orElse: () => const _MetricOption(
        apiValue: 'WEIGHT',
        label: 'Cân nặng',
        unit: 'kg',
        icon: Icons.monitor_weight_outlined,
      ),
    );
    return _MetricOption(
      apiValue: code,
      label: capability.displayName,
      unit: capability.canonicalUnit.isEmpty
          ? fallback.unit
          : capability.canonicalUnit,
      icon: fallback.icon,
    );
  }

  String _canonicalMetricType(String? value) {
    switch (value) {
      case 'BLOOD_PRESSURE_SYSTOLIC':
      case 'BLOOD_PRESSURE_DIASTOLIC':
      case 'BLOOD_PRESSURE':
        return 'BLOOD_PRESSURE';
      case 'FETAL_MOVEMENT':
      case 'FETAL_MOVEMENT_COUNT':
      case 'FETAL_MOVEMENT_SESSION':
        return 'FETAL_MOVEMENT_SESSION';
      case 'HEART_RATE':
        return 'MATERNAL_HEART_RATE';
      default:
        return value ?? 'WEIGHT';
    }
  }

  @override
  void dispose() {
    _historyScrollController.dispose();
    super.dispose();
  }

  Future<void> _loadTrend() async {
    if (!_isSupportedMetric) return;
    setState(() {
      _isLoading = true;
      _errorMsg = null;
    });
    try {
      final result = await _service.getMetricTrend(
        journeyId: widget.journeyId,
        metricType: _selectedMetric.apiValue,
        from: DateTime.now().subtract(const Duration(days: 7)),
      );
      if (mounted) {
        setState(() => _trend = result);
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _trend = null;
          _errorMsg = 'Không thể tải dữ liệu. Vui lòng thử lại.';
        });
      }
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  bool get _isSupportedMetric => _metricOptions.any(
    (option) => option.apiValue == _selectedMetric.apiValue,
  );

  bool get _isBloodPressure => _selectedMetric.apiValue == 'BLOOD_PRESSURE';
  bool get _isGlucose => _selectedMetric.apiValue == 'BLOOD_GLUCOSE';
  bool get _isFetalMovement =>
      _selectedMetric.apiValue == 'FETAL_MOVEMENT_SESSION';

  void _onMetricChanged(_MetricOption? opt) {
    if (opt == null || opt == _selectedMetric) return;
    setState(() => _selectedMetric = opt);
    _loadTrend();
  }

  Future<void> _openAddMetric() async {
    final changed = await context.push<bool>(
      '/journeys/${Uri.encodeComponent(widget.journeyId)}/metrics/add'
      '?metricType=${Uri.encodeQueryComponent(_selectedMetric.apiValue)}',
    );
    if (changed == true && mounted) {
      await _loadTrend();
    }
  }

  Future<void> _openMetricDetail(MetricDataPoint point) async {
    final metricId = point.metricId;
    if (metricId != null && metricId.isNotEmpty) {
      final changed = await context.push<bool>(
        '/health-metrics/${Uri.encodeComponent(metricId)}',
      );
      if (changed == true && mounted) {
        await _loadTrend();
      }
      return;
    }

    await context.push(
      '/health-metrics/${Uri.encodeComponent(_syntheticMetricId(point))}',
      extra: {'metric': _fallbackMetricDetail(point)},
    );
  }

  HealthMetricDetail _fallbackMetricDetail(MetricDataPoint point) {
    return HealthMetricDetail(
      id: _syntheticMetricId(point),
      journeyId: widget.journeyId,
      metricType: _metricTypeFor(_selectedMetric.apiValue),
      metricCode: _selectedMetric.apiValue,
      valueNumeric: point.valueNumeric,
      valueSecondary: point.valueSecondary,
      unit: _trend?.unit ?? _selectedMetric.unit,
      measuredAt: point.measuredAt,
      sourceType: point.sourceType,
      note: point.note,
      createdAt: point.measuredAt,
      context: point.context,
      periodStart: point.periodStart,
      periodEnd: point.periodEnd,
      qualityLabel: point.qualityLabel,
      disclaimer: _trend?.disclaimer,
    );
  }

  String _syntheticMetricId(MetricDataPoint point) {
    return '${_selectedMetric.apiValue}-${point.measuredAt.microsecondsSinceEpoch}';
  }

  MetricType _metricTypeFor(String apiValue) {
    return MetricTypeExtension.fromApi(apiValue);
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
      body: RefreshIndicator(
        color: _primary,
        onRefresh: _loadTrend,
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              _buildMetricSelector(),
              const SizedBox(height: 16),
              if (_isLoadingCapabilities || _isLoading)
                const Center(
                  child: Padding(
                    padding: EdgeInsets.all(48),
                    child: CircularProgressIndicator(color: _primaryContainer),
                  ),
                )
              else if (!_isSupportedMetric)
                _buildUnsupportedMetricCard()
              else if (_errorMsg != null)
                _buildErrorCard()
              else ...[
                _buildChartCard(),
                const SizedBox(height: 16),
                _buildHistoryCard(),
                const SizedBox(height: 20),
                _buildAddButton(),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildMetricSelector() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 2),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: _surfaceContainer, width: 1.5),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<_MetricOption>(
          value: _selectedMetric,
          isExpanded: true,
          icon: const Icon(Icons.expand_more_rounded, color: _onSurfaceVariant),
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 14,
            color: _onSurface,
          ),
          items: _metricOptions
              .map(
                (o) => DropdownMenuItem(
                  value: o,
                  child: Row(
                    children: [
                      Icon(o.icon, size: 18, color: _primary),
                      const SizedBox(width: 8),
                      Text(o.label),
                    ],
                  ),
                ),
              )
              .toList(),
          onChanged: _onMetricChanged,
          dropdownColor: _canvas,
          borderRadius: BorderRadius.circular(20),
        ),
      ),
    );
  }

  Widget _buildUnsupportedMetricCard() {
    return _Card(
      child: Column(
        children: [
          const Icon(Icons.info_outline_rounded, color: _error, size: 44),
          const SizedBox(height: 12),
          const Text(
            'Chỉ số này chưa được hỗ trợ trong hành trình hiện tại.',
            textAlign: TextAlign.center,
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildErrorCard() {
    return _Card(
      child: Column(
        children: [
          const Icon(Icons.error_outline_rounded, color: _error, size: 44),
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
          const SizedBox(height: 12),
          TextButton(
            onPressed: _loadTrend,
            child: const Text(
              'Thử lại',
              style: TextStyle(
                fontFamily: 'Lexend',
                color: _primary,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildChartCard() {
    final points = _historyPoints;
    final avg = _trend?.average;
    final unit = _trend?.unit ?? _selectedMetric.unit;
    final trendPct = _trend?.trend;
    final isDown = (trendPct ?? 0) < 0;
    final hasScalarSummary =
        avg != null && !_isBloodPressure && !_isFetalMovement;

    return _Card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      points.isEmpty
                          ? '-- $unit'
                          : hasScalarSummary
                          ? '${_formatNumber(avg)} $unit'
                          : _summaryLabel(unit),
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 22,
                        fontWeight: FontWeight.w700,
                        color: _onSurface,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      _summaryCaption,
                      style: const TextStyle(
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
                    mainAxisSize: MainAxisSize.min,
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
                  'Chưa có dữ liệu trong 7 ngày gần đây',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 13,
                    color: _onSurfaceVariant,
                  ),
                ),
              ),
            )
          else
            _buildBarChart(points),
        ],
      ),
    );
  }

  Widget _buildBarChart(List<MetricDataPoint> points) {
    final bars = _buildSevenDayBars(points);
    final maxValue = bars.fold<double>(
      0,
      (max, bar) => math.max(max, bar.value ?? 0),
    );
    final effectiveMax = maxValue <= 0 ? 1.0 : maxValue;

    return SizedBox(
      height: 210,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.end,
        children: bars.map((bar) {
          final ratio = ((bar.value ?? 0) / effectiveMax)
              .clamp(0.0, 1.0)
              .toDouble();
          final barHeight = 120.0 * ratio;
          return Expanded(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 4),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  SizedBox(
                    height: 22,
                    child: Text(
                      bar.value == null ? '' : _formatNumber(bar.value!),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 10,
                        fontWeight: FontWeight.w600,
                        color: _primary,
                      ),
                    ),
                  ),
                  const SizedBox(height: 6),
                  Container(
                    height: math.max(4.0, barHeight),
                    decoration: BoxDecoration(
                      color: bar.value == null
                          ? _surfaceContainer
                          : _primaryContainer,
                      borderRadius: const BorderRadius.vertical(
                        top: Radius.circular(8),
                      ),
                    ),
                  ),
                  const SizedBox(height: 10),
                  SizedBox(
                    height: 32,
                    child: Text(
                      _formatShortDate(bar.date),
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 10,
                        fontWeight: FontWeight.w600,
                        color: _onSurfaceVariant,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          );
        }).toList(),
      ),
    );
  }

  String _summaryLabel(String unit) {
    if (_isBloodPressure) return 'Xem theo cặp tâm thu/tâm trương';
    if (_isFetalMovement) return 'Phiên đo trong 7 ngày';
    if (_isGlucose) return 'Chọn cùng bối cảnh để so sánh';
    return '-- $unit';
  }

  String get _summaryCaption {
    if (_isBloodPressure) return 'Không tính trung bình vô hướng';
    if (_isFetalMovement) return 'Lịch sử phiên cử động thai';
    if (_isGlucose) return 'Đường huyết được phân nhóm theo bối cảnh đo';
    return 'Trung bình 7 ngày gần đây';
  }

  List<_ChartBar> _buildSevenDayBars(List<MetricDataPoint> points) {
    final now = DateTime.now();
    final days = List.generate(7, (i) {
      final date = DateTime(
        now.year,
        now.month,
        now.day,
      ).subtract(Duration(days: 6 - i));
      return date;
    });
    return days.map((date) {
      final sameDay = points.where((point) {
        final measuredAt = point.measuredAt;
        return measuredAt.year == date.year &&
            measuredAt.month == date.month &&
            measuredAt.day == date.day;
      }).toList();
      if (sameDay.isEmpty) {
        return _ChartBar(date: date, value: null);
      }
      if (_isBloodPressure) {
        return _ChartBar(date: date, value: null);
      }
      if (_isGlucose) {
        final contexts = sameDay
            .map((point) => point.context['measurementContext'])
            .whereType<String>()
            .toSet();
        if (contexts.length > 1) return _ChartBar(date: date, value: null);
      }
      final sum = sameDay.fold<double>(
        0,
        (total, point) => total + point.valueNumeric,
      );
      return _ChartBar(date: date, value: sum / sameDay.length);
    }).toList();
  }

  Widget _buildHistoryCard() {
    final points = _historyPoints;
    final height = math.min(420.0, math.max(96.0, points.length * 74.0));

    return _Card(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Lịch sử đo',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 18,
              fontWeight: FontWeight.w700,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 12),
          if (points.isEmpty)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 24),
              child: Center(
                child: Text(
                  'Chưa có bản ghi chỉ số.',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 13,
                    color: _onSurfaceVariant,
                  ),
                ),
              ),
            )
          else
            SizedBox(
              height: height,
              child: Scrollbar(
                controller: _historyScrollController,
                thumbVisibility: points.length > 5,
                child: ListView.separated(
                  controller: _historyScrollController,
                  padding: EdgeInsets.zero,
                  itemCount: points.length,
                  itemBuilder: (context, index) {
                    return _HistoryTile(
                      point: points[index],
                      metric: _selectedMetric,
                      unit: _trend?.unit ?? _selectedMetric.unit,
                      onTap: () => _openMetricDetail(points[index]),
                    );
                  },
                  separatorBuilder: (_, _) =>
                      const Divider(height: 1, color: _surfaceContainer),
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildAddButton() {
    return ElevatedButton.icon(
      onPressed: _openAddMetric,
      icon: const Icon(Icons.add_circle_outline_rounded, size: 18),
      label: const Text(
        'Thêm chỉ số',
        style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w700),
      ),
      style: ElevatedButton.styleFrom(
        backgroundColor: _primary,
        foregroundColor: Colors.white,
        padding: const EdgeInsets.symmetric(vertical: 14),
        shape: const StadiumBorder(),
        elevation: 0,
      ),
    );
  }

  String _formatNumber(double value) {
    return value % 1 == 0 ? value.toStringAsFixed(0) : value.toStringAsFixed(1);
  }

  String _formatShortDate(DateTime dt) {
    return '${dt.day.toString().padLeft(2, '0')}/${dt.month.toString().padLeft(2, '0')}';
  }
}

String _formatHistoryDateTime(DateTime dt) {
  final date =
      '${dt.day.toString().padLeft(2, '0')}/${dt.month.toString().padLeft(2, '0')}';
  final time =
      '${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';
  return '$date, $time';
}

String _displayValue(MetricDataPoint point, _MetricOption metric) {
  if (point.valueSecondary != null) {
    if (metric.apiValue == 'BLOOD_PRESSURE_DIASTOLIC') {
      return '${point.valueSecondary!.toStringAsFixed(0)}/${point.valueNumeric.toStringAsFixed(0)}';
    }
    return '${point.valueNumeric.toStringAsFixed(0)}/${point.valueSecondary!.toStringAsFixed(0)}';
  }
  return point.valueDisplay;
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
  bool operator ==(Object other) {
    return other is _MetricOption && other.apiValue == apiValue;
  }

  @override
  int get hashCode => apiValue.hashCode;
}

class _ChartBar {
  final DateTime date;
  final double? value;

  const _ChartBar({required this.date, required this.value});
}

class _Card extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry padding;

  const _Card({required this.child, this.padding = const EdgeInsets.all(20)});

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: padding,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF845143).withAlpha(18),
            blurRadius: 16,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: child,
    );
  }
}

class _HistoryTile extends StatelessWidget {
  final MetricDataPoint point;
  final _MetricOption metric;
  final String unit;
  final VoidCallback onTap;

  const _HistoryTile({
    required this.point,
    required this.metric,
    required this.unit,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: BorderRadius.circular(14),
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 10),
        child: Row(
          children: [
            Container(
              width: 42,
              height: 42,
              decoration: BoxDecoration(
                color: _HealthMetricTrendScreenState._surfaceAccent,
                borderRadius: BorderRadius.circular(14),
              ),
              child: Icon(
                metric.icon,
                color: _HealthMetricTrendScreenState._primary,
                size: 22,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '${_displayValue(point, metric)} $unit',
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 15,
                      fontWeight: FontWeight.w700,
                      color: _HealthMetricTrendScreenState._onSurface,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    _formatHistoryDateTime(point.measuredAt),
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      color: _HealthMetricTrendScreenState._onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
            const Icon(
              Icons.chevron_right_rounded,
              color: _HealthMetricTrendScreenState._onSurfaceVariant,
            ),
          ],
        ),
      ),
    );
  }
}
