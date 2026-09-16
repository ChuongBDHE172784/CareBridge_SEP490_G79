import 'dart:convert';

import 'package:flutter/material.dart';
import '../../baby/widgets/growth_trend_chart.dart';
import '../../healthRecords/models/growth_measurement_model.dart';
import '../../healthRecords/services/growth_measurement_service.dart';

typedef GrowthMeasurementsLoader =
    Future<List<GrowthMeasurement>> Function(String babyId);

String _isoDate(DateTime d) =>
    '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

String _displayDate(DateTime d) =>
    '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';

String _displayValue(double? value, String unit) =>
    value == null ? '—' : '${value.toStringAsFixed(1)} $unit';

String _displayValues(double? weightKg, double? heightCm, double? headCm) =>
    '${_displayValue(weightKg, 'kg')} · ${_displayValue(heightCm, 'cm')} · ${_displayValue(headCm, 'cm')}';

class BabyGrowthLatestSnapshot {
  const BabyGrowthLatestSnapshot({
    required this.measuredDate,
    this.weightKg,
    this.heightCm,
    this.headCircumferenceCm,
  });

  factory BabyGrowthLatestSnapshot.fromMeasurement(GrowthMeasurement m) =>
      BabyGrowthLatestSnapshot(
        measuredDate: m.measuredAt,
        weightKg: m.weightKg,
        heightCm: m.heightCm,
        headCircumferenceCm: m.headCircumferenceCm,
      );

  static BabyGrowthLatestSnapshot? fromJson(Object? json) {
    if (json is! Map) return null;
    final date = DateTime.tryParse(json['measuredDate']?.toString() ?? '');
    if (date == null) return null;
    return BabyGrowthLatestSnapshot(
      measuredDate: date,
      weightKg: (json['weightKg'] as num?)?.toDouble(),
      heightCm: (json['heightCm'] as num?)?.toDouble(),
      headCircumferenceCm: (json['headCircumferenceCm'] as num?)?.toDouble(),
    );
  }

  final DateTime measuredDate;
  final double? weightKg;
  final double? heightCm;
  final double? headCircumferenceCm;

  Map<String, dynamic> toJson() => {
    'measuredDate': _isoDate(measuredDate),
    'weightKg': weightKg,
    'heightCm': heightCm,
    'headCircumferenceCm': headCircumferenceCm,
  };
}

/// Reference payload for a baby growth share. The body never embeds the
/// measurement history (direct_messages_body_ck caps bodies at 2000 chars);
/// cards load all measurements live from GET /babies/{babyId}/growth-chart.
class BabyGrowthShareData {
  const BabyGrowthShareData({
    this.title = 'Phát triển của bé',
    required this.babyId,
    required this.babyNickname,
    required this.birthDate,
    required this.measurementCount,
    this.latest,
    this.isLiveSync = true,
    this.note,
  });

  static const String tag = '[CAREBRIDGE_BABY_GROWTH_SHARE]';
  static const int maxBodyLength = 2000;
  static const int maxNoteLength = 500;

  final String title;
  final String babyId;
  final String babyNickname;
  final DateTime birthDate;
  final int measurementCount;
  final BabyGrowthLatestSnapshot? latest;
  final bool isLiveSync;
  final String? note;

  static bool isBabyGrowthShareMessage(String? body) =>
      body != null && body.trim().startsWith(tag);

  static BabyGrowthShareData? parse(String? body) {
    if (body == null || !isBabyGrowthShareMessage(body)) return null;
    try {
      final decoded = jsonDecode(body.trim().substring(tag.length).trim());
      if (decoded is! Map<String, dynamic>) return null;
      final babyId = decoded['babyId'];
      if (babyId is! String || babyId.isEmpty) return null;
      return BabyGrowthShareData(
        title: decoded['title'] as String? ?? 'Phát triển của bé',
        babyId: babyId,
        babyNickname: decoded['babyNickname'] as String? ?? 'Bé',
        birthDate:
            DateTime.tryParse(decoded['birthDate']?.toString() ?? '') ??
            DateTime(2000),
        measurementCount: (decoded['measurementCount'] as num?)?.toInt() ?? 0,
        latest: BabyGrowthLatestSnapshot.fromJson(decoded['latest']),
        isLiveSync: decoded['isLiveSync'] as bool? ?? true,
        note: decoded['note'] as String?,
      );
    } catch (_) {
      return null;
    }
  }

  static bool fitsMessageLimit(String body) =>
      body.trim().length <= maxBodyLength;

  String serialize() => '$tag\n${jsonEncode({
    'title': title,
    'babyId': babyId,
    'babyNickname': babyNickname,
    'birthDate': _isoDate(birthDate),
    'measurementCount': measurementCount,
    'latest': latest?.toJson(),
    'isLiveSync': isLiveSync,
    'note': note,
  })}';
}

class _GrowthSeries {
  const _GrowthSeries({
    required this.key,
    required this.title,
    required this.unit,
    required this.emptyLabel,
    required this.extractor,
    required this.accentColor,
    required this.dotColor,
  });

  final String key;
  final String title;
  final String unit;
  final String emptyLabel;
  final double? Function(GrowthMeasurement) extractor;
  final Color accentColor;
  final Color dotColor;
}

final List<_GrowthSeries> _growthSeries = [
  _GrowthSeries(
    key: 'weight',
    title: 'Xu hướng cân nặng',
    unit: 'kg',
    emptyLabel: 'Chưa có dữ liệu cân nặng.',
    extractor: (m) => m.weightKg,
    accentColor: const Color(0xFFC98C7B),
    dotColor: const Color(0xFF845143),
  ),
  _GrowthSeries(
    key: 'height',
    title: 'Xu hướng chiều cao',
    unit: 'cm',
    emptyLabel: 'Chưa có dữ liệu chiều cao.',
    extractor: (m) => m.heightCm,
    accentColor: const Color(0xFF5B8E7D),
    dotColor: const Color(0xFF2C5E4E),
  ),
  _GrowthSeries(
    key: 'head',
    title: 'Xu hướng vòng đầu',
    unit: 'cm',
    emptyLabel: 'Chưa có dữ liệu vòng đầu.',
    extractor: (m) => m.headCircumferenceCm,
    accentColor: const Color(0xFFD48B47),
    dotColor: const Color(0xFF9E5C25),
  ),
];

class BabyGrowthMessageCard extends StatefulWidget {
  const BabyGrowthMessageCard({
    super.key,
    required this.data,
    required this.isOwnMessage,
    this.loadMeasurements,
  });

  final BabyGrowthShareData data;
  final bool isOwnMessage;
  final GrowthMeasurementsLoader? loadMeasurements;

  @override
  State<BabyGrowthMessageCard> createState() => _BabyGrowthMessageCardState();
}

class _BabyGrowthMessageCardState extends State<BabyGrowthMessageCard> {
  static const _primary = Color(0xFFC98C7B);
  static const _textDark = Color(0xFF2C2523);
  static const _textMuted = Color(0xFF7A6F6C);
  static const _border = Color(0xFFECE4E1);

  List<GrowthMeasurement>? _measurements;
  bool _loading = true;
  bool _failed = false;
  bool _historyExpanded = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void didUpdateWidget(covariant BabyGrowthMessageCard oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.data.babyId != widget.data.babyId) {
      _load();
    }
  }

  Future<void> _load() async {
    if (mounted && !_loading) setState(() => _loading = true);
    final loader =
        widget.loadMeasurements ??
        GrowthMeasurementService().getGrowthChartMeasurements;
    try {
      final result = List<GrowthMeasurement>.from(
        await loader(widget.data.babyId),
      )..sort((a, b) => a.measuredAt.compareTo(b.measuredAt));
      if (!mounted) return;
      setState(() {
        _measurements = result;
        _loading = false;
        _failed = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _failed = true;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final data = widget.data;
    final count = _failed || _measurements == null
        ? data.measurementCount
        : _measurements!.length;

    return Container(
      width: 310,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: widget.isOwnMessage ? Colors.white70 : const Color(0xFFE8D5CE),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        mainAxisSize: MainAxisSize.min,
        children: [
          _buildHeader(data, count),
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 10, 12, 10),
            child: _buildBody(data),
          ),
          if (data.note != null && data.note!.trim().isNotEmpty)
            _buildNote(data.note!),
        ],
      ),
    );
  }

  Widget _buildHeader(BabyGrowthShareData data, int count) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: BoxDecoration(
        color: _primary.withValues(alpha: 0.1),
        borderRadius: const BorderRadius.vertical(top: Radius.circular(15)),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(6),
            decoration: BoxDecoration(
              color: _primary,
              borderRadius: BorderRadius.circular(8),
            ),
            child: const Icon(
              Icons.child_care_rounded,
              color: Colors.white,
              size: 18,
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        data.title,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontWeight: FontWeight.w700,
                          fontSize: 13,
                          color: _textDark,
                        ),
                      ),
                    ),
                    if (data.isLiveSync)
                      InkWell(
                        onTap: _loading ? null : _load,
                        borderRadius: BorderRadius.circular(6),
                        child: Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 5,
                            vertical: 1.5,
                          ),
                          decoration: BoxDecoration(
                            color: const Color(0xFFE8F5E9),
                            borderRadius: BorderRadius.circular(6),
                          ),
                          child: const Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Icon(
                                Icons.bolt_rounded,
                                size: 11,
                                color: Color(0xFF2E7D32),
                              ),
                              SizedBox(width: 2),
                              Text(
                                'Live',
                                style: TextStyle(
                                  fontFamily: 'Lexend',
                                  fontSize: 9,
                                  fontWeight: FontWeight.bold,
                                  color: Color(0xFF2E7D32),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                  ],
                ),
                Text(
                  [
                    data.babyNickname,
                    formatBabyAgeAt(data.birthDate, DateTime.now()),
                    '$count lần đo',
                  ].join(' · '),
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 10.5,
                    color: _textMuted,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBody(BabyGrowthShareData data) {
    if (_loading) {
      return const SizedBox(
        height: 120,
        child: Center(
          child: SizedBox(
            width: 24,
            height: 24,
            child: CircularProgressIndicator(strokeWidth: 2, color: _primary),
          ),
        ),
      );
    }
    if (_failed) return _buildFallback(data);

    final measurements = _measurements ?? const <GrowthMeasurement>[];
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        for (final series in _growthSeries)
          _buildSeriesSection(series, measurements),
        if (measurements.isNotEmpty) ...[
          Align(
            alignment: Alignment.centerLeft,
            child: TextButton.icon(
              onPressed: () =>
                  setState(() => _historyExpanded = !_historyExpanded),
              icon: Icon(
                _historyExpanded
                    ? Icons.keyboard_arrow_up_rounded
                    : Icons.keyboard_arrow_down_rounded,
                size: 18,
                color: _primary,
              ),
              label: Text(
                _historyExpanded
                    ? 'Thu gọn lịch sử'
                    : 'Xem lịch sử đo (${measurements.length})',
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                  color: _primary,
                ),
              ),
            ),
          ),
          if (_historyExpanded) _buildHistory(data, measurements),
        ],
      ],
    );
  }

  Widget _buildSeriesSection(
    _GrowthSeries series,
    List<GrowthMeasurement> measurements,
  ) {
    final points = measurements
        .where((m) => series.extractor(m) != null)
        .toList(growable: false);
    final values = points.map((m) => series.extractor(m)!).toList();

    return Container(
      key: Key('baby-growth-chart-${series.key}'),
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: const Color(0xFFFAF7F6),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: _border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            series.title,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 12,
              fontWeight: FontWeight.w600,
              color: Color(0xFF423734),
            ),
          ),
          const SizedBox(height: 6),
          if (points.isEmpty)
            SizedBox(
              height: 40,
              child: Center(
                child: Text(
                  series.emptyLabel,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 11,
                    color: _textMuted,
                  ),
                ),
              ),
            )
          else ...[
            GrowthTrendChart(
              measurements: points,
              valueExtractor: series.extractor,
              unit: series.unit,
              accentColor: series.accentColor,
              dotColor: series.dotColor,
              height: 100,
            ),
            const SizedBox(height: 4),
            Text(
              values.length == 1
                  ? '${values.first.toStringAsFixed(1)} ${series.unit}'
                  : '${values.first.toStringAsFixed(1)} ${series.unit} – '
                        '${values.last.toStringAsFixed(1)} ${series.unit}',
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 11,
                color: _textMuted,
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildHistory(
    BabyGrowthShareData data,
    List<GrowthMeasurement> measurements,
  ) {
    final newestFirst = measurements.reversed.toList(growable: false);
    return Container(
      padding: const EdgeInsets.all(8),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: _border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          for (final m in newestFirst)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 4),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '${_displayDate(m.measuredAt)} · ${formatBabyAgeAt(data.birthDate, m.measuredAt)}',
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 11,
                      color: Color(0xFF6E5F5C),
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    _displayValues(m.weightKg, m.heightCm, m.headCircumferenceCm),
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      fontWeight: FontWeight.w700,
                      color: _textDark,
                    ),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildFallback(BabyGrowthShareData data) {
    final latest = data.latest;
    return Container(
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF8F1),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: const Color(0xFFF1D9C2)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Không thể tải dữ liệu tăng trưởng mới nhất',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 12,
              fontWeight: FontWeight.w600,
              color: Color(0xFF9E5C25),
            ),
          ),
          if (latest != null) ...[
            const SizedBox(height: 6),
            Text(
              'Lần đo gần nhất khi chia sẻ: ${_displayDate(latest.measuredDate)}',
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 11,
                color: _textMuted,
              ),
            ),
            Text(
              _displayValues(
                latest.weightKg,
                latest.heightCm,
                latest.headCircumferenceCm,
              ),
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 12,
                fontWeight: FontWeight.w700,
                color: _textDark,
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildNote(String note) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      margin: const EdgeInsets.only(left: 10, right: 10, bottom: 10),
      decoration: BoxDecoration(
        color: const Color(0xFFF9F5F4),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: _border),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(
            Icons.chat_bubble_outline_rounded,
            size: 14,
            color: _textMuted,
          ),
          const SizedBox(width: 6),
          Expanded(
            child: Text(
              note,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 12,
                color: _textDark,
                fontStyle: FontStyle.italic,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
