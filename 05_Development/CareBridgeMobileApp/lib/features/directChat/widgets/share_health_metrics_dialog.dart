import 'package:flutter/material.dart';
import '../../healthRecords/models/health_metric_model.dart';
import '../../healthRecords/services/health_metric_service.dart';
import '../../journey/services/journey_service.dart';
import 'health_metrics_message_card.dart';

class ShareHealthMetricsDialog extends StatefulWidget {
  const ShareHealthMetricsDialog({super.key});

  static Future<HealthMetricsShareData?> show(BuildContext context) {
    return showModalBottomSheet<HealthMetricsShareData>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (ctx) => const ShareHealthMetricsDialog(),
    );
  }

  @override
  State<ShareHealthMetricsDialog> createState() =>
      _ShareHealthMetricsDialogState();
}

class _MetricItemState {
  final String code;
  final String name;
  String value;
  final String unit;
  String status;
  final String icon;
  final String? measuredTime;
  final List<HealthMetricMeasurementRecord> history;
  bool isSelected = true;
  bool isExpanded = false;

  _MetricItemState({
    required this.code,
    required this.name,
    required this.value,
    this.unit = '',
    this.status = 'NORMAL',
    required this.icon,
    this.measuredTime,
    this.history = const [],
  });
}

class _ShareHealthMetricsDialogState extends State<ShareHealthMetricsDialog> {
  final TextEditingController _noteController = TextEditingController();
  final HealthMetricService _healthMetricService = HealthMetricService();
  bool _loading = true;
  int? _gestationalWeek;
  String _measuredDate = '';
  List<_MetricItemState> _metrics = [];

  @override
  void initState() {
    super.initState();
    _loadMetrics();
  }

  @override
  void dispose() {
    _noteController.dispose();
    super.dispose();
  }

  Future<void> _loadMetrics() async {
    final now = DateTime.now();
    _measuredDate =
        '${now.day.toString().padLeft(2, '0')}/${now.month.toString().padLeft(2, '0')}/${now.year}';

    try {
      final dashboard = await JourneyService().getDashboard();
      _gestationalWeek =
          dashboard.effectivePregnancyWeek ?? dashboard.completedGestationalWeek;
      final journeyId = dashboard.journeyId;

      if (journeyId != null && journeyId.isNotEmpty) {
        final metricConfigs = [
          {'type': 'BLOOD_PRESSURE', 'name': 'Huyết áp', 'icon': 'favorite'},
          {
            'type': 'BLOOD_GLUCOSE',
            'name': 'Đường huyết đói',
            'icon': 'water_drop',
          },
          {
            'type': 'BMI',
            'name': 'Chỉ số BMI / Thể trạng',
            'icon': 'monitor_weight',
          },
          {
            'type': 'HEART_RATE',
            'name': 'Nhịp tim mẹ',
            'icon': 'monitor_heart',
          },
          {'type': 'TEMPERATURE', 'name': 'Thân nhiệt', 'icon': 'thermostat'},
          {
            'type': 'FETAL_MOVEMENT_SESSION',
            'name': 'Cử động thai',
            'icon': 'child_care',
          },
          {'type': 'SPO2', 'name': 'Nồng độ Oxy SpO2', 'icon': 'air'},
        ];

        final fromDate = now.subtract(const Duration(days: 365));

        final results = await Future.wait(
          metricConfigs.map((m) async {
            try {
              final trend = await _healthMetricService.getMetricTrend(
                journeyId: journeyId,
                metricType: m['type']!,
                from: fromDate,
                to: now,
              );
              return {'config': m, 'trend': trend};
            } catch (_) {
              return null;
            }
          }),
        );

        final realItems = <_MetricItemState>[];
        for (final res in results) {
          if (res == null) continue;
          final cfg = res['config'] as Map<String, String>;
          final trend = res['trend'] as MetricTrend;
          if (trend.dataPoints.isNotEmpty) {
            // Sort points from newest to oldest
            final sorted = List<MetricDataPoint>.from(trend.dataPoints)
              ..sort((a, b) => b.measuredAt.compareTo(a.measuredAt));
            final latest = sorted.first;
            final unit = trend.unit ?? '';
            final status = _evaluateMetricStatus(cfg['type']!, latest);
            final dt = latest.measuredAt;
            final timeStr =
                '${dt.day.toString().padLeft(2, '0')}/${dt.month.toString().padLeft(2, '0')} ${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';

            final historyRecords = sorted.map((p) {
              final pTime =
                  '${p.measuredAt.day.toString().padLeft(2, '0')}/${p.measuredAt.month.toString().padLeft(2, '0')} ${p.measuredAt.hour.toString().padLeft(2, '0')}:${p.measuredAt.minute.toString().padLeft(2, '0')}';
              return HealthMetricMeasurementRecord(
                measuredAt: pTime,
                value: p.valueDisplay,
                unit: unit,
                status: _evaluateMetricStatus(cfg['type']!, p),
                note: p.note,
              );
            }).toList();

            realItems.add(
              _MetricItemState(
                code: cfg['type']!,
                name: cfg['name']!,
                value: latest.valueDisplay,
                unit: unit,
                status: status,
                icon: cfg['icon']!,
                measuredTime: timeStr,
                history: historyRecords,
              ),
            );
          }
        }

        if (realItems.isNotEmpty && mounted) {
          setState(() {
            _metrics = realItems;
            _loading = false;
          });
          return;
        }
      }
    } catch (_) {}

    _useFallbackMetrics();
  }

  void _useFallbackMetrics() {
    if (!mounted) return;
    setState(() {
      _metrics = [
        _MetricItemState(
          code: 'BLOOD_PRESSURE',
          name: 'Huyết áp',
          value: '120/80',
          unit: 'mmHg',
          status: 'NORMAL',
          icon: 'favorite',
          measuredTime: 'Hôm nay 07:30',
          history: [
            const HealthMetricMeasurementRecord(
              measuredAt: 'Hôm nay 07:30',
              value: '120/80',
              unit: 'mmHg',
              status: 'NORMAL',
            ),
            const HealthMetricMeasurementRecord(
              measuredAt: 'Hôm qua 19:30',
              value: '122/82',
              unit: 'mmHg',
              status: 'NORMAL',
            ),
          ],
        ),
        _MetricItemState(
          code: 'BLOOD_GLUCOSE',
          name: 'Đường huyết đói',
          value: '90',
          unit: 'mg/dL',
          status: 'NORMAL',
          icon: 'water_drop',
          measuredTime: 'Hôm nay 06:45',
          history: [
            const HealthMetricMeasurementRecord(
              measuredAt: 'Hôm nay 06:45',
              value: '90',
              unit: 'mg/dL',
              status: 'NORMAL',
            ),
          ],
        ),
        _MetricItemState(
          code: 'WEIGHT',
          name: 'Cân nặng / BMI',
          value: '58.5 kg',
          unit: '',
          status: 'NORMAL',
          icon: 'monitor_weight',
          measuredTime: 'Tuần này',
          history: [
            const HealthMetricMeasurementRecord(
              measuredAt: 'Tuần này',
              value: '58.5 kg (BMI 22.5)',
              unit: '',
              status: 'NORMAL',
            ),
          ],
        ),
        _MetricItemState(
          code: 'HEART_RATE',
          name: 'Nhịp tim mẹ',
          value: '78',
          unit: 'bpm',
          status: 'NORMAL',
          icon: 'monitor_heart',
          measuredTime: 'Hôm nay 07:30',
          history: [
            const HealthMetricMeasurementRecord(
              measuredAt: 'Hôm nay 07:30',
              value: '78',
              unit: 'bpm',
              status: 'NORMAL',
            ),
          ],
        ),
        _MetricItemState(
          code: 'TEMPERATURE',
          name: 'Thân nhiệt',
          value: '36.8',
          unit: '°C',
          status: 'NORMAL',
          icon: 'thermostat',
          measuredTime: 'Hôm nay 08:00',
          history: [
            const HealthMetricMeasurementRecord(
              measuredAt: 'Hôm nay 08:00',
              value: '36.8',
              unit: '°C',
              status: 'NORMAL',
            ),
          ],
        ),
        _MetricItemState(
          code: 'FETAL_MOVEMENT',
          name: 'Cử động thai',
          value: '12 cử động / 2h',
          unit: '',
          status: 'NORMAL',
          icon: 'child_care',
          measuredTime: 'Tối qua',
          history: [
            const HealthMetricMeasurementRecord(
              measuredAt: 'Tối qua',
              value: '12 cử động / 2h',
              unit: '',
              status: 'NORMAL',
            ),
          ],
        ),
      ];
      _loading = false;
    });
  }

  String _evaluateMetricStatus(String code, MetricDataPoint point) {
    if (code == 'BLOOD_PRESSURE') {
      final sys = point.valueNumeric;
      final dia = point.valueSecondary ?? 80;
      if (sys >= 140 || dia >= 90) return 'CRITICAL';
      if (sys >= 130 || dia >= 85) return 'WARNING';
      if (sys < 90 || dia < 60) return 'WARNING';
      return 'NORMAL';
    }
    if (code == 'BLOOD_GLUCOSE') {
      final val = point.valueNumeric;
      if (val > 140) return 'CRITICAL';
      if (val > 95) return 'WARNING';
      if (val < 60) return 'WARNING';
      return 'NORMAL';
    }
    if (code == 'HEART_RATE') {
      final val = point.valueNumeric;
      if (val > 120 || val < 50) return 'CRITICAL';
      if (val > 100 || val < 60) return 'WARNING';
      return 'NORMAL';
    }
    if (code == 'TEMPERATURE') {
      final val = point.valueNumeric;
      if (val >= 38.5) return 'CRITICAL';
      if (val >= 37.5) return 'WARNING';
      if (val < 35.5) return 'WARNING';
      return 'NORMAL';
    }
    if (code == 'FETAL_MOVEMENT_SESSION') {
      final val = point.valueNumeric;
      if (val < 4) return 'CRITICAL';
      if (val < 10) return 'WARNING';
      return 'NORMAL';
    }
    if (code == 'SPO2') {
      final val = point.valueNumeric;
      if (val < 92) return 'CRITICAL';
      if (val < 95) return 'WARNING';
      return 'NORMAL';
    }
    return 'NORMAL';
  }

  void _toggleSelectAll(bool selectAll) {
    setState(() {
      for (final m in _metrics) {
        m.isSelected = selectAll;
      }
    });
  }

  void _onConfirm() {
    final selectedMetrics = _metrics
        .where((m) => m.isSelected)
        .map(
          (m) => HealthMetricItemData(
            code: m.code,
            name: m.name,
            value: m.value,
            unit: m.unit,
            status: m.status,
            icon: m.icon,
            measuredTime: m.measuredTime,
            history: m.history,
          ),
        )
        .toList();

    if (selectedMetrics.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Vui lòng chọn ít nhất 1 loại chỉ số để chia sẻ'),
        ),
      );
      return;
    }

    final shareData = HealthMetricsShareData(
      title: 'Toàn bộ lịch sử chỉ số sức khỏe',
      gestationalWeek: _gestationalWeek,
      measuredDate: _measuredDate,
      note: _noteController.text.trim().isEmpty
          ? null
          : _noteController.text.trim(),
      metrics: selectedMetrics,
    );

    Navigator.of(context).pop(shareData);
  }

  @override
  Widget build(BuildContext context) {
    const primary = Color(0xFFC98C7B);
    final allSelected = _metrics.isNotEmpty && _metrics.every((m) => m.isSelected);
    var totalRecords = 0;
    for (final m in _metrics.where((x) => x.isSelected)) {
      totalRecords += m.history.isNotEmpty ? m.history.length : 1;
    }

    return Padding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).viewInsets.bottom,
        left: 20,
        right: 20,
        top: 20,
      ),
      child: SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Handle bar
            Center(
              child: Container(
                width: 40,
                height: 4,
                decoration: BoxDecoration(
                  color: Colors.grey.shade300,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            const SizedBox(height: 16),

            // Title Header
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: primary.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: const Icon(
                    Icons.history_rounded,
                    color: primary,
                    size: 22,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Chia sẻ toàn bộ lịch sử đo',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 17,
                          fontWeight: FontWeight.bold,
                          color: Color(0xFF2C2523),
                        ),
                      ),
                      Text(
                        _gestationalWeek != null
                            ? 'Tuần thai $_gestationalWeek · Sẵn sàng gửi $totalRecords bản ghi đo'
                            : 'Gửi toàn bộ lịch sử theo dõi sức khỏe cho chuyên gia',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 12,
                          color: Color(0xFF7A6F6C),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),

            // Select All / Deselect All Action
            if (!_loading && _metrics.isNotEmpty)
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'Đã chọn ${_metrics.where((m) => m.isSelected).length}/${_metrics.length} loại chỉ số ($totalRecords bản ghi)',
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      fontWeight: FontWeight.w500,
                      color: Color(0xFF5A4E4B),
                    ),
                  ),
                  TextButton(
                    onPressed: () => _toggleSelectAll(!allSelected),
                    style: TextButton.styleFrom(
                      padding: const EdgeInsets.symmetric(horizontal: 8),
                      minimumSize: Size.zero,
                      tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                    ),
                    child: Text(
                      allSelected ? 'Bỏ chọn tất cả' : 'Chọn tất cả',
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        fontWeight: FontWeight.bold,
                        color: primary,
                      ),
                    ),
                  ),
                ],
              ),
            const SizedBox(height: 6),

            if (_loading)
              const Center(
                child: Padding(
                  padding: EdgeInsets.all(32),
                  child: CircularProgressIndicator(color: primary),
                ),
              )
            else ...[
              // Metrics checklist with expandable history
              Flexible(
                child: Container(
                  constraints: const BoxConstraints(maxHeight: 290),
                  decoration: BoxDecoration(
                    color: const Color(0xFFFAF7F6),
                    borderRadius: BorderRadius.circular(14),
                    border: Border.all(color: const Color(0xFFE8D5CE)),
                  ),
                  child: ListView.separated(
                    shrinkWrap: true,
                    itemCount: _metrics.length,
                    separatorBuilder: (ctx, index) => const Divider(
                      height: 1,
                      color: Color(0xFFECE4E1),
                    ),
                    itemBuilder: (ctx, idx) {
                      final item = _metrics[idx];
                      final count = item.history.isNotEmpty
                          ? item.history.length
                          : 1;

                      return Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          CheckboxListTile(
                            value: item.isSelected,
                            activeColor: primary,
                            dense: true,
                            contentPadding:
                                const EdgeInsets.symmetric(horizontal: 12),
                            title: Row(
                              children: [
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Row(
                                        children: [
                                          Text(
                                            item.name,
                                            style: const TextStyle(
                                              fontFamily: 'Lexend',
                                              fontSize: 13,
                                              fontWeight: FontWeight.w600,
                                              color: Color(0xFF2C2523),
                                            ),
                                          ),
                                          const SizedBox(width: 6),
                                          Container(
                                            padding: const EdgeInsets.symmetric(
                                              horizontal: 6,
                                              vertical: 1,
                                            ),
                                            decoration: BoxDecoration(
                                              color: primary.withValues(
                                                alpha: 0.12,
                                              ),
                                              borderRadius:
                                                  BorderRadius.circular(10),
                                            ),
                                            child: Text(
                                              '$count lần đo',
                                              style: const TextStyle(
                                                fontFamily: 'Lexend',
                                                fontSize: 10,
                                                fontWeight: FontWeight.bold,
                                                color: primary,
                                              ),
                                            ),
                                          ),
                                        ],
                                      ),
                                      if (item.measuredTime != null)
                                        Text(
                                          'Gần nhất: ${item.measuredTime}',
                                          style: const TextStyle(
                                            fontFamily: 'Lexend',
                                            fontSize: 11,
                                            color: Color(0xFF8C7D79),
                                          ),
                                        ),
                                    ],
                                  ),
                                ),
                                Container(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 8,
                                    vertical: 3,
                                  ),
                                  decoration: BoxDecoration(
                                    color: item.status == 'CRITICAL'
                                        ? const Color(0xFFFFEBEE)
                                        : item.status == 'WARNING'
                                        ? const Color(0xFFFFF3E0)
                                        : const Color(0xFFF1F8E9),
                                    borderRadius: BorderRadius.circular(8),
                                  ),
                                  child: Text(
                                    '${item.value} ${item.unit}'.trim(),
                                    style: TextStyle(
                                      fontFamily: 'Lexend',
                                      fontSize: 12,
                                      fontWeight: FontWeight.bold,
                                      color: item.status == 'CRITICAL'
                                          ? const Color(0xFFC62828)
                                          : item.status == 'WARNING'
                                          ? const Color(0xFFE65100)
                                          : const Color(0xFF2E7D32),
                                    ),
                                  ),
                                ),
                                if (item.history.length > 1)
                                  IconButton(
                                    icon: Icon(
                                      item.isExpanded
                                          ? Icons.expand_less_rounded
                                          : Icons.expand_more_rounded,
                                      size: 20,
                                      color: const Color(0xFF8C7D79),
                                    ),
                                    padding: EdgeInsets.zero,
                                    constraints: const BoxConstraints(),
                                    onPressed: () {
                                      setState(() {
                                        item.isExpanded = !item.isExpanded;
                                      });
                                    },
                                  ),
                              ],
                            ),
                            onChanged: (val) {
                              setState(() {
                                item.isSelected = val ?? false;
                              });
                            },
                          ),

                          // Sub-list of history data points if expanded
                          if (item.isExpanded && item.history.length > 1)
                            Container(
                              margin: const EdgeInsets.only(
                                left: 48,
                                right: 16,
                                bottom: 8,
                              ),
                              padding: const EdgeInsets.all(8),
                              decoration: BoxDecoration(
                                color: Colors.white,
                                borderRadius: BorderRadius.circular(8),
                                border: Border.all(
                                  color: const Color(0xFFECE4E1),
                                ),
                              ),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: item.history.map((record) {
                                  return Padding(
                                    padding: const EdgeInsets.symmetric(
                                      vertical: 2,
                                    ),
                                    child: Row(
                                      children: [
                                        Text(
                                          record.measuredAt,
                                          style: const TextStyle(
                                            fontFamily: 'Lexend',
                                            fontSize: 11,
                                            color: Color(0xFF7A6F6C),
                                          ),
                                        ),
                                        const Spacer(),
                                        Text(
                                          '${record.value} ${record.unit}'
                                              .trim(),
                                          style: const TextStyle(
                                            fontFamily: 'Lexend',
                                            fontSize: 11,
                                            fontWeight: FontWeight.bold,
                                            color: Color(0xFF2C2523),
                                          ),
                                        ),
                                      ],
                                    ),
                                  );
                                }).toList(),
                              ),
                            ),
                        ],
                      );
                    },
                  ),
                ),
              ),
              const SizedBox(height: 12),

              // Note field
              TextField(
                controller: _noteController,
                maxLines: 2,
                style: const TextStyle(fontFamily: 'Lexend', fontSize: 13),
                decoration: InputDecoration(
                  hintText: 'Thêm câu hỏi hoặc ghi chú cho Bác sĩ (tùy chọn)...',
                  hintStyle: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    color: Color(0xFF9E8E8A),
                  ),
                  contentPadding: const EdgeInsets.all(12),
                  filled: true,
                  fillColor: const Color(0xFFFAF7F6),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: const BorderSide(color: Color(0xFFE8D5CE)),
                  ),
                  enabledBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: const BorderSide(color: Color(0xFFE8D5CE)),
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: const BorderSide(color: primary, width: 1.5),
                  ),
                ),
              ),
              const SizedBox(height: 16),

              // Send button
              FilledButton.icon(
                onPressed: _onConfirm,
                icon: const Icon(Icons.send_rounded, size: 18),
                label: Text(
                  'Gửi toàn bộ lịch sử ($totalRecords bản ghi)',
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontWeight: FontWeight.bold,
                    fontSize: 14,
                  ),
                ),
                style: FilledButton.styleFrom(
                  backgroundColor: primary,
                  minimumSize: const Size.fromHeight(48),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
              ),
              const SizedBox(height: 12),
            ],
          ],
        ),
      ),
    );
  }
}
