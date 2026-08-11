import 'package:flutter/material.dart';

import '../models/health_metric_model.dart';
import '../services/health_metric_service.dart';

class FetalMovementTrackerScreen extends StatefulWidget {
  const FetalMovementTrackerScreen({super.key, required this.journeyId});

  final String journeyId;

  @override
  State<FetalMovementTrackerScreen> createState() =>
      _FetalMovementTrackerScreenState();
}

class _FetalMovementTrackerScreenState
    extends State<FetalMovementTrackerScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFF8F5F1);
  static const _surface = Color(0xFFFFFCF9);
  static const _surfaceContainerHigh = Color(0xFFF1E6E0);
  static const _surfaceContainerLow = Color(0xFFF8EEE9);
  static const _onSurface = Color(0xFF2A211D);
  static const _onSurfaceVariant = Color(0xFF655650);
  final _service = HealthMetricService();
  final List<MetricDataPoint> _today = [];
  DateTime _historyDate = DateTime.now();
  bool _loading = true;
  String? _savingType;

  static const _movementTypes = [
    ('KICK', 'Đạp', Icons.directions_run_rounded),
    ('ROLL', 'Xoay người', Icons.rotate_right_rounded),
    ('STRETCH', 'Co duỗi', Icons.open_with_rounded),
    ('HICCUP', 'Nấc cụt', Icons.favorite_rounded),
  ];

  @override
  void initState() {
    super.initState();
    _loadToday();
  }

  Future<void> _loadToday() async {
    final start = DateTime(
      _historyDate.year,
      _historyDate.month,
      _historyDate.day,
    );
    try {
      final trend = await _service.getMetricTrend(
        journeyId: widget.journeyId,
        metricType: 'FETAL_MOVEMENT_COUNT',
        from: start,
        to: start.add(const Duration(days: 1)),
      );
      if (mounted) {
        setState(() {
          _today
            ..clear()
            ..addAll(trend.dataPoints);
          _loading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _record(String type, String label) async {
    if (_savingType != null) return;
    setState(() => _savingType = type);
    final now = DateTime.now();
    try {
      final saved = await _service.addMetric(
        widget.journeyId,
        AddMetricRequest(
          metricType: 'FETAL_MOVEMENT_SESSION',
          valueNumeric: 1,
          unit: 'count',
          measuredAt: now,
          note: type,
          periodStart: now.subtract(const Duration(minutes: 1)),
          periodEnd: now,
          context: const {
            'protocolCode': 'MATERNAL_AWARENESS',
            'completionStatus': 'IN_PROGRESS',
            'gestationalAgeSnapshot': 'NOT_RECORDED',
          },
        ),
      );
      if (!mounted) return;
      setState(
        () => _today.insert(
          0,
          MetricDataPoint(
            metricId: saved.id,
            measuredAt: saved.measuredAt,
            valueNumeric: 1,
            note: type,
          ),
        ),
      );
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Đã ghi nhận: $label lúc ${TimeOfDay.fromDateTime(now).format(context)}',
          ),
        ),
      );
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Không thể ghi nhận cử động. Vui lòng thử lại.'),
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _savingType = null);
    }
  }

  String _labelFor(String? code) {
    for (final item in _movementTypes) {
      if (item.$1 == code) return item.$2;
    }
    return 'Cử động';
  }

  int _countFor(String code) =>
      _today.where((point) => point.note == code).length;

  Future<void> _pickHistoryDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _historyDate,
      firstDate: DateTime.now().subtract(const Duration(days: 365)),
      lastDate: DateTime.now(),
    );
    if (picked != null) {
      setState(() => _historyDate = picked);
      await _loadToday();
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    backgroundColor: _canvas,
    appBar: AppBar(
      backgroundColor: _canvas,
      elevation: 0,
      foregroundColor: _primary,
      title: const Text(
        'Theo dõi cử động thai',
        style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w700),
      ),
    ),
    body: RefreshIndicator(
      color: _primaryContainer,
      onRefresh: _loadToday,
      child: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          Text(
            '${DateUtils.isSameDay(_historyDate, DateTime.now()) ? 'Hôm nay' : 'Ngày đã chọn'}: ${_today.length} cử động đã ghi nhận',
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 22,
              fontWeight: FontWeight.w700,
              color: _onSurface,
              letterSpacing: -0.3,
            ),
          ),
          const SizedBox(height: 6),
          const Text(
            'Ghi nhận khi bạn cảm nhận bé cử động. Điều quan trọng nhất là nhận biết thay đổi so với nhịp cử động quen thuộc của bé.',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 13,
              height: 1.45,
              color: _onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 20),
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: _surfaceContainerLow,
              borderRadius: BorderRadius.circular(20),
              border: Border.all(color: _surfaceContainerHigh),
            ),
            child: const Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(Icons.info_outline_rounded, color: _primary),
                SizedBox(width: 12),
                Expanded(
                  child: Text(
                    'Nếu bé cử động ít hơn thường ngày, thay đổi rõ rệt hoặc bạn không cảm nhận được cử động, hãy liên hệ cơ sở sản khoa ngay — không chờ đến ngày mai.',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 13,
                      height: 1.45,
                      color: _onSurface,
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),
          GridView.count(
            crossAxisCount: 2,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            mainAxisSpacing: 12,
            crossAxisSpacing: 12,
            childAspectRatio: 1.45,
            children: _movementTypes
                .map(
                  (item) => FilledButton.tonalIcon(
                    onPressed: _savingType == null
                        ? () => _record(item.$1, item.$2)
                        : null,
                    icon: _savingType == item.$1
                        ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : Icon(item.$3),
                    label: Text(
                      '${item.$2}\n${_countFor(item.$1)} lần',
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    style: FilledButton.styleFrom(
                      backgroundColor: _surface,
                      foregroundColor: _primary,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(20),
                        side: const BorderSide(color: _surfaceContainerHigh),
                      ),
                      padding: const EdgeInsets.all(12),
                    ),
                  ),
                )
                .toList(growable: false),
          ),
          const SizedBox(height: 24),
          Row(
            children: [
              const Expanded(
                child: Text(
                  'Lịch sử',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 18,
                    fontWeight: FontWeight.w700,
                    color: _onSurface,
                    letterSpacing: -0.2,
                  ),
                ),
              ),
              TextButton.icon(
                onPressed: _pickHistoryDate,
                icon: const Icon(Icons.calendar_month_rounded, color: _primary),
                label: Text(
                  '${_historyDate.day.toString().padLeft(2, '0')}/${_historyDate.month.toString().padLeft(2, '0')}',
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontWeight: FontWeight.w600,
                    color: _primary,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          if (_loading)
            const Center(
              child: CircularProgressIndicator(color: _primaryContainer),
            )
          else if (_today.isEmpty)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 20),
              child: Text(
                'Chưa có cử động nào trong ngày đã chọn.',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  color: _onSurfaceVariant,
                ),
              ),
            )
          else
            ..._today.map(
              (point) => Container(
                margin: const EdgeInsets.only(bottom: 8),
                decoration: BoxDecoration(
                  color: _surface,
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: _surfaceContainerHigh),
                ),
                child: ListTile(
                  leading: Container(
                    width: 36,
                    height: 36,
                    decoration: const BoxDecoration(
                      color: _surfaceContainerLow,
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(Icons.schedule_rounded, color: _primary, size: 18),
                  ),
                  title: Text(
                    _labelFor(point.note),
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontWeight: FontWeight.w600,
                      color: _onSurface,
                    ),
                  ),
                  trailing: Text(
                    TimeOfDay.fromDateTime(point.measuredAt).format(context),
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      color: _onSurfaceVariant,
                    ),
                  ),
                ),
              ),
            ),
        ],
      ),
    ),
  );
}
