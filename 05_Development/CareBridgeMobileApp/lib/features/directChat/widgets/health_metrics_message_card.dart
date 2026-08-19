import 'dart:convert';
import 'package:flutter/material.dart';

class HealthMetricMeasurementRecord {
  final String measuredAt;
  final String value;
  final String unit;
  final String status; // NORMAL, WARNING, CRITICAL
  final String? note;

  const HealthMetricMeasurementRecord({
    required this.measuredAt,
    required this.value,
    this.unit = '',
    this.status = 'NORMAL',
    this.note,
  });

  factory HealthMetricMeasurementRecord.fromJson(Map<String, dynamic> json) =>
      HealthMetricMeasurementRecord(
        measuredAt: json['measuredAt']?.toString() ?? '',
        value: json['value']?.toString() ?? '',
        unit: json['unit']?.toString() ?? '',
        status: (json['status'] as String? ?? 'NORMAL').toUpperCase(),
        note: json['note'] as String?,
      );

  Map<String, dynamic> toJson() => {
    'measuredAt': measuredAt,
    'value': value,
    'unit': unit,
    'status': status,
    if (note != null && note!.isNotEmpty) 'note': note,
  };
}

class HealthMetricItemData {
  final String code;
  final String name;
  final String value;
  final String unit;
  final String status; // NORMAL, WARNING, CRITICAL
  final String? icon;
  final String? measuredTime;
  final List<HealthMetricMeasurementRecord> history;

  const HealthMetricItemData({
    required this.code,
    required this.name,
    required this.value,
    this.unit = '',
    this.status = 'NORMAL',
    this.icon,
    this.measuredTime,
    this.history = const [],
  });

  factory HealthMetricItemData.fromJson(Map<String, dynamic> json) {
    final historyList = (json['history'] as List? ?? [])
        .map(
          (h) =>
              HealthMetricMeasurementRecord.fromJson(h as Map<String, dynamic>),
        )
        .toList();

    return HealthMetricItemData(
      code: json['code'] as String? ?? '',
      name: json['name'] as String? ?? 'Chỉ số',
      value: json['value']?.toString() ?? '',
      unit: json['unit']?.toString() ?? '',
      status: (json['status'] as String? ?? 'NORMAL').toUpperCase(),
      icon: json['icon'] as String?,
      measuredTime: json['measuredTime'] as String?,
      history: historyList,
    );
  }

  Map<String, dynamic> toJson() => {
    'code': code,
    'name': name,
    'value': value,
    'unit': unit,
    'status': status,
    'icon': icon,
    if (measuredTime != null) 'measuredTime': measuredTime,
    if (history.isNotEmpty)
      'history': history.map((h) => h.toJson()).toList(),
  };
}

class HealthMetricsShareData {
  final String title;
  final int? gestationalWeek;
  final String? measuredDate;
  final String? note;
  final List<HealthMetricItemData> metrics;

  const HealthMetricsShareData({
    this.title = 'Lịch sử chỉ số sức khỏe',
    this.gestationalWeek,
    this.measuredDate,
    this.note,
    required this.metrics,
  });

  static const String tag = '[CAREBRIDGE_HEALTH_SHARE]';

  static bool isHealthShareMessage(String? body) {
    if (body == null) return false;
    return body.trim().startsWith(tag);
  }

  static HealthMetricsShareData? parse(String? body) {
    if (body == null || !isHealthShareMessage(body)) return null;
    try {
      final jsonStr = body.replaceFirst(tag, '').trim();
      final decoded = jsonDecode(jsonStr) as Map<String, dynamic>;
      final metricsList = (decoded['metrics'] as List? ?? [])
          .map((m) => HealthMetricItemData.fromJson(m as Map<String, dynamic>))
          .toList();
      return HealthMetricsShareData(
        title: decoded['title'] as String? ?? 'Lịch sử chỉ số sức khỏe',
        gestationalWeek: (decoded['gestationalWeek'] as num?)?.toInt(),
        measuredDate: decoded['measuredDate'] as String?,
        note: decoded['note'] as String?,
        metrics: metricsList,
      );
    } catch (_) {
      return null;
    }
  }

  String serialize() => '$tag\n${jsonEncode({
    'title': title,
    'gestationalWeek': gestationalWeek,
    'measuredDate': measuredDate,
    'note': note,
    'metrics': metrics.map((m) => m.toJson()).toList(),
  })}';
}

class HealthMetricsMessageCard extends StatefulWidget {
  const HealthMetricsMessageCard({
    super.key,
    required this.data,
    required this.isOwnMessage,
  });

  final HealthMetricsShareData data;
  final bool isOwnMessage;

  @override
  State<HealthMetricsMessageCard> createState() =>
      _HealthMetricsMessageCardState();
}

class _HealthMetricsMessageCardState extends State<HealthMetricsMessageCard> {
  final Set<String> _expandedMetrics = {};

  int get _totalHistoryPoints {
    var count = 0;
    for (final m in widget.data.metrics) {
      count += m.history.isNotEmpty ? m.history.length : 1;
    }
    return count;
  }

  @override
  Widget build(BuildContext context) {
    const primary = Color(0xFFC98C7B);
    const surface = Colors.white;
    const textDark = Color(0xFF2C2523);
    const textMuted = Color(0xFF7A6F6C);

    return Container(
      width: 310,
      decoration: BoxDecoration(
        color: surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: widget.isOwnMessage
              ? Colors.white70
              : const Color(0xFFE8D5CE),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        mainAxisSize: MainAxisSize.min,
        children: [
          // Header
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
            decoration: BoxDecoration(
              color: primary.withValues(alpha: 0.1),
              borderRadius:
                  const BorderRadius.vertical(top: Radius.circular(15)),
            ),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(6),
                  decoration: BoxDecoration(
                    color: primary,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: const Icon(
                    Icons.monitor_heart_outlined,
                    color: Colors.white,
                    size: 18,
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        widget.data.title,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontWeight: FontWeight.w700,
                          fontSize: 13,
                          color: textDark,
                        ),
                      ),
                      Text(
                        [
                          if (widget.data.gestationalWeek != null)
                            'Tuần thai: ${widget.data.gestationalWeek}',
                          '$_totalHistoryPoints bản ghi đo',
                        ].join(' · '),
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 11,
                          color: textMuted,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),

          // Metrics list
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
            child: Column(
              children: widget.data.metrics.map((metric) {
                return _buildMetricGroup(metric);
              }).toList(),
            ),
          ),

          // Note if present
          if (widget.data.note != null && widget.data.note!.trim().isNotEmpty)
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              margin: const EdgeInsets.only(left: 10, right: 10, bottom: 10),
              decoration: BoxDecoration(
                color: const Color(0xFFF9F5F4),
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: const Color(0xFFECE4E1)),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(
                    Icons.chat_bubble_outline_rounded,
                    size: 14,
                    color: textMuted,
                  ),
                  const SizedBox(width: 6),
                  Expanded(
                    child: Text(
                      widget.data.note!,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        color: textDark,
                        fontStyle: FontStyle.italic,
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

  Widget _buildMetricGroup(HealthMetricItemData metric) {
    final statusColor = _statusColor(metric.status);
    final statusLabel = _statusLabel(metric.status);
    final hasHistory = metric.history.length > 1;
    final isExpanded = _expandedMetrics.contains(metric.code);

    return Container(
      margin: const EdgeInsets.only(bottom: 6),
      decoration: BoxDecoration(
        color: const Color(0xFFFAF7F6),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: const Color(0xFFECE4E1)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Header summary row
          InkWell(
            onTap: hasHistory
                ? () {
                    setState(() {
                      if (isExpanded) {
                        _expandedMetrics.remove(metric.code);
                      } else {
                        _expandedMetrics.add(metric.code);
                      }
                    });
                  }
                : null,
            borderRadius: BorderRadius.circular(10),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
              child: Row(
                children: [
                  Icon(
                    _metricIcon(metric.code),
                    size: 16,
                    color: const Color(0xFFC98C7B),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          metric.name,
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 12,
                            fontWeight: FontWeight.w600,
                            color: Color(0xFF423734),
                          ),
                        ),
                        if (metric.measuredTime != null)
                          Text(
                            'Gần nhất: ${metric.measuredTime}',
                            style: const TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 10,
                              color: Color(0xFF8C7D79),
                            ),
                          ),
                      ],
                    ),
                  ),
                  RichText(
                    text: TextSpan(
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 13,
                        fontWeight: FontWeight.w700,
                        color: Color(0xFF2C2523),
                      ),
                      children: [
                        TextSpan(text: metric.value),
                        if (metric.unit.isNotEmpty)
                          TextSpan(
                            text: ' ${metric.unit}',
                            style: const TextStyle(
                              fontSize: 10,
                              fontWeight: FontWeight.normal,
                              color: Color(0xFF7A6F6C),
                            ),
                          ),
                      ],
                    ),
                  ),
                  if (metric.status != 'NORMAL') ...[
                    const SizedBox(width: 6),
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 5,
                        vertical: 2,
                      ),
                      decoration: BoxDecoration(
                        color: statusColor.withValues(alpha: 0.15),
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: Text(
                        statusLabel,
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 9,
                          fontWeight: FontWeight.w600,
                          color: statusColor,
                        ),
                      ),
                    ),
                  ],
                  if (hasHistory) ...[
                    const SizedBox(width: 4),
                    Icon(
                      isExpanded
                          ? Icons.keyboard_arrow_up_rounded
                          : Icons.keyboard_arrow_down_rounded,
                      size: 18,
                      color: const Color(0xFF8C7D79),
                    ),
                  ],
                ],
              ),
            ),
          ),

          // Collapsible historical measurement logs
          if (hasHistory && isExpanded)
            Container(
              padding: const EdgeInsets.only(
                left: 10,
                right: 10,
                bottom: 8,
                top: 4,
              ),
              decoration: const BoxDecoration(
                border: Border(
                  top: BorderSide(color: Color(0xFFECE4E1), width: 0.8),
                ),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 4),
                    child: Text(
                      'Lịch sử tất cả các lần đo (${metric.history.length}):',
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 10,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFF8C7D79),
                      ),
                    ),
                  ),
                  ...metric.history.map((record) {
                    final recColor = _statusColor(record.status);
                    return Container(
                      padding: const EdgeInsets.symmetric(vertical: 3),
                      child: Row(
                        children: [
                          Icon(
                            Icons.history_toggle_off_rounded,
                            size: 12,
                            color: Colors.grey.shade400,
                          ),
                          const SizedBox(width: 4),
                          Text(
                            record.measuredAt,
                            style: const TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 11,
                              color: Color(0xFF6E5F5C),
                            ),
                          ),
                          const Spacer(),
                          Text(
                            '${record.value} ${record.unit}'.trim(),
                            style: TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 11,
                              fontWeight: FontWeight.bold,
                              color: recColor,
                            ),
                          ),
                          if (record.status != 'NORMAL') ...[
                            const SizedBox(width: 4),
                            Container(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 4,
                                vertical: 1,
                              ),
                              decoration: BoxDecoration(
                                color: recColor.withValues(alpha: 0.12),
                                borderRadius: BorderRadius.circular(3),
                              ),
                              child: Text(
                                _statusLabel(record.status),
                                style: TextStyle(
                                  fontFamily: 'Lexend',
                                  fontSize: 8,
                                  fontWeight: FontWeight.w600,
                                  color: recColor,
                                ),
                              ),
                            ),
                          ],
                        ],
                      ),
                    );
                  }),
                ],
              ),
            ),
        ],
      ),
    );
  }

  Color _statusColor(String status) {
    switch (status) {
      case 'CRITICAL':
        return const Color(0xFFBA1A1A);
      case 'WARNING':
        return const Color(0xFFD97706);
      default:
        return const Color(0xFF2E7D32);
    }
  }

  String _statusLabel(String status) {
    switch (status) {
      case 'CRITICAL':
        return 'Nguy hiểm';
      case 'WARNING':
        return 'Cần lưu ý';
      default:
        return 'Bình thường';
    }
  }

  IconData _metricIcon(String code) {
    switch (code) {
      case 'BLOOD_PRESSURE':
        return Icons.favorite_rounded;
      case 'BLOOD_GLUCOSE':
        return Icons.water_drop_rounded;
      case 'BMI':
      case 'WEIGHT':
        return Icons.monitor_weight_rounded;
      case 'MATERNAL_HEART_RATE':
      case 'HEART_RATE':
        return Icons.monitor_heart_rounded;
      case 'TEMPERATURE':
        return Icons.thermostat_rounded;
      case 'FETAL_MOVEMENT':
      case 'FETAL_MOVEMENT_SESSION':
        return Icons.child_care_rounded;
      default:
        return Icons.health_and_safety_rounded;
    }
  }
}
