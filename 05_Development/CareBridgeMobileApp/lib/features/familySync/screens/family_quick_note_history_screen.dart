import 'package:flutter/material.dart';

import '../../healthRecords/models/health_metric_model.dart';
import '../services/family_home_service.dart';

class FamilyQuickNoteHistoryScreen extends StatefulWidget {
  const FamilyQuickNoteHistoryScreen({
    super.key,
    required this.careGroupId,
    required this.metricType,
    this.historyLoader,
  });

  final String careGroupId;
  final String metricType;
  final Future<MetricTrend> Function({
    required String careGroupId,
    required String metricType,
    required DateTime from,
    required DateTime to,
  })?
  historyLoader;

  @override
  State<FamilyQuickNoteHistoryScreen> createState() =>
      _FamilyQuickNoteHistoryScreenState();
}

class _FamilyQuickNoteHistoryScreenState
    extends State<FamilyQuickNoteHistoryScreen> {
  static const _primary = Color(0xFF845143);
  static const _accent = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _ink = Color(0xFF271812);
  static const _muted = Color(0xFF6F5E59);

  DateTime _selectedDate = DateTime.now();
  MetricTrend? _history;
  bool _loading = true;
  Object? _error;

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
    final from = DateTime(
      _selectedDate.year,
      _selectedDate.month,
      _selectedDate.day,
    );
    final to = from
        .add(const Duration(days: 1))
        .subtract(const Duration(milliseconds: 1));
    try {
      final history =
          await (widget.historyLoader ??
              FamilyHomeService.instance.loadQuickNoteHistory)(
            careGroupId: widget.careGroupId,
            metricType: widget.metricType,
            from: from,
            to: to,
          );
      if (!mounted) return;
      setState(() {
        _history = history;
        _loading = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _error = error;
        _loading = false;
      });
    }
  }

  Future<void> _pickDate() async {
    final selected = await showDatePicker(
      context: context,
      initialDate: _selectedDate,
      firstDate: DateTime.now().subtract(const Duration(days: 365)),
      lastDate: DateTime.now(),
      helpText: 'Lọc lịch sử theo ngày',
      cancelText: 'Hủy',
      confirmText: 'Chọn',
    );
    if (selected == null || !mounted) return;
    setState(() => _selectedDate = selected);
    _load();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      appBar: AppBar(
        backgroundColor: _canvas,
        elevation: 0,
        centerTitle: true,
        leading: IconButton(
          tooltip: 'Quay lại',
          onPressed: () => Navigator.of(context).pop(),
          icon: const Icon(Icons.arrow_back_rounded, color: _primary),
        ),
        title: Text(
          _title,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 20,
            fontWeight: FontWeight.w700,
            color: _primary,
          ),
        ),
      ),
      body: RefreshIndicator(
        color: _primary,
        onRefresh: _load,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 12, 20, 32),
          children: [
            _buildReadOnlyNotice(),
            const SizedBox(height: 14),
            _buildDateFilter(),
            const SizedBox(height: 16),
            if (_loading)
              const Padding(
                padding: EdgeInsets.only(top: 80),
                child: Center(child: CircularProgressIndicator(color: _accent)),
              )
            else if (_error != null)
              _buildError()
            else ...[
              _buildSummary(),
              const SizedBox(height: 22),
              const Text(
                'Lịch sử',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 20,
                  fontWeight: FontWeight.w700,
                  color: _ink,
                ),
              ),
              const SizedBox(height: 10),
              if (_history?.dataPoints.isEmpty ?? true)
                _buildEmpty()
              else
                ..._history!.dataPoints.map(_buildHistoryItem),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildReadOnlyNotice() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        color: const Color(0xFFFFE9E3),
        borderRadius: BorderRadius.circular(16),
      ),
      child: const Row(
        children: [
          Icon(Icons.visibility_outlined, color: _primary, size: 20),
          SizedBox(width: 10),
          Expanded(
            child: Text(
              'Dữ liệu do mẹ chia sẻ • Chỉ xem',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: _primary,
              ),
            ),
          ),
          Icon(Icons.lock_outline_rounded, color: _primary, size: 18),
        ],
      ),
    );
  }

  Widget _buildDateFilter() {
    return InkWell(
      key: const Key('family-quick-note-date-filter'),
      onTap: _pickDate,
      borderRadius: BorderRadius.circular(18),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(18),
          border: Border.all(color: const Color(0xFFF0E4DF)),
        ),
        child: Row(
          children: [
            const Icon(Icons.calendar_today_outlined, color: _primary),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Ngày đang xem',
                    style: TextStyle(fontSize: 12, color: _muted),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    _dayLabel(_selectedDate),
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontWeight: FontWeight.w700,
                      color: _ink,
                    ),
                  ),
                ],
              ),
            ),
            const Icon(Icons.expand_more_rounded, color: _primary),
          ],
        ),
      ),
    );
  }

  Widget _buildSummary() {
    final points = _history?.dataPoints ?? const <MetricDataPoint>[];
    final latestPoint = points.isEmpty
        ? null
        : points.reduce(
            (latest, point) =>
                point.measuredAt.isAfter(latest.measuredAt) ? point : latest,
          );
    final total = points.fold<double>(
      0,
      (sum, point) => sum + point.valueNumeric,
    );
    final value = switch (widget.metricType) {
      'HYDRATION' => '${total.toStringAsFixed(0)} ml',
      'FETAL_MOVEMENT_COUNT' => '${total.toStringAsFixed(0)} lần',
      'WEIGHT' =>
        latestPoint == null
            ? '—'
            : '${latestPoint.valueNumeric.toStringAsFixed(1)} kg',
      'EPDS_SCORE' =>
        latestPoint == null
            ? '—'
            : '${latestPoint.valueNumeric.toStringAsFixed(0)}/30',
      'BLOOD_PRESSURE' =>
        latestPoint == null ? '—' : '${latestPoint.valueDisplay} mmHg',
      'BLOOD_GLUCOSE' =>
        latestPoint == null
            ? '—'
            : '${latestPoint.valueDisplay} ${_history?.unit ?? ''}'.trim(),
      _ => '—',
    };
    final label = switch (widget.metricType) {
      'HYDRATION' => 'Tổng lượng nước trong ngày',
      'FETAL_MOVEMENT_COUNT' => 'Tổng cử động trong ngày',
      'WEIGHT' => 'Lần ghi gần nhất trong ngày',
      'EPDS_SCORE' => 'Điểm sàng lọc gần nhất • Không phải chẩn đoán',
      'BLOOD_PRESSURE' => 'Lần đo gần nhất trong ngày',
      'BLOOD_GLUCOSE' => 'Lần đo gần nhất trong ngày',
      _ => 'Tổng quan trong ngày',
    };

    return Container(
      key: const Key('family-quick-note-daily-summary'),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFFFFF1EC), Color(0xFFFFE0D6)],
        ),
        borderRadius: BorderRadius.circular(24),
      ),
      child: Row(
        children: [
          Container(
            width: 52,
            height: 52,
            decoration: BoxDecoration(
              color: Colors.white.withAlpha(190),
              borderRadius: BorderRadius.circular(17),
            ),
            child: Icon(_icon, color: _primary, size: 27),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(label, style: const TextStyle(color: _muted)),
                const SizedBox(height: 3),
                Text(
                  value,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 25,
                    fontWeight: FontWeight.w700,
                    color: _ink,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildHistoryItem(MetricDataPoint point) {
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(18),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0D6A4B40),
            blurRadius: 14,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
        leading: CircleAvatar(
          backgroundColor: const Color(0xFFFFE9E3),
          foregroundColor: _primary,
          child: Icon(_icon, size: 21),
        ),
        title: Text(
          _pointTitle(point),
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontWeight: FontWeight.w700,
            color: _ink,
          ),
        ),
        subtitle: Text(_timeLabel(point.measuredAt)),
        trailing: const Icon(
          Icons.lock_outline_rounded,
          size: 17,
          color: _muted,
        ),
      ),
    );
  }

  Widget _buildEmpty() {
    return Container(
      key: const Key('family-quick-note-empty'),
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 34),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Column(
        children: [
          Icon(_icon, size: 40, color: _accent),
          const SizedBox(height: 10),
          const Text(
            'Chưa có dữ liệu trong ngày này',
            textAlign: TextAlign.center,
            style: TextStyle(fontWeight: FontWeight.w600, color: _muted),
          ),
        ],
      ),
    );
  }

  Widget _buildError() {
    return Container(
      padding: const EdgeInsets.all(28),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Column(
        children: [
          const Icon(Icons.cloud_off_outlined, size: 40, color: _primary),
          const SizedBox(height: 10),
          const Text('Không thể tải lịch sử được chia sẻ.'),
          TextButton.icon(
            onPressed: _load,
            icon: const Icon(Icons.refresh),
            label: const Text('Thử lại'),
          ),
        ],
      ),
    );
  }

  String _pointTitle(MetricDataPoint point) {
    return switch (widget.metricType) {
      'WEIGHT' => '${point.valueNumeric.toStringAsFixed(1)} kg',
      'HYDRATION' => '${point.valueNumeric.toStringAsFixed(0)} ml',
      'EPDS_SCORE' =>
        '${point.valueNumeric.toStringAsFixed(0)}/30 • Điểm sàng lọc',
      'FETAL_MOVEMENT_COUNT' =>
        '${_movementLabel(point.note)} • ${point.valueNumeric.toStringAsFixed(0)} lần',
      'BLOOD_PRESSURE' => '${point.valueDisplay} mmHg',
      'BLOOD_GLUCOSE' =>
        '${point.valueDisplay} ${_history?.unit ?? ''}${_glucoseContext(point.context['measurementContext'])}',
      _ => point.valueDisplay,
    };
  }

  String _glucoseContext(Object? value) {
    return switch (value?.toString().toUpperCase()) {
      'FASTING' => ' • Lúc đói',
      'BEFORE_MEAL' || 'PRE_MEAL' => ' • Trước ăn',
      'AFTER_MEAL' || 'POSTPRANDIAL' => ' • Sau ăn',
      'POST_MEAL_1H' => ' • Sau ăn 1 giờ',
      'POST_MEAL_2H' => ' • Sau ăn 2 giờ',
      'RANDOM' => ' • Ngẫu nhiên',
      'OTHER_APPROVED' => ' • Thời điểm khác',
      _ => '',
    };
  }

  String _movementLabel(String? value) {
    return switch (value?.toUpperCase()) {
      'KICK' || 'ĐẠP' => 'Đạp',
      'ROLL' || 'XOAY NGƯỜI' => 'Xoay người',
      'HICCUP' || 'NẤC CỤT' => 'Nấc cụt',
      'STRETCH' || 'CO DUỖI' => 'Co duỗi',
      null || '' => 'Cử động',
      _ => value!,
    };
  }

  String _dayLabel(DateTime date) {
    final today = DateTime.now();
    final isToday =
        date.year == today.year &&
        date.month == today.month &&
        date.day == today.day;
    final value =
        '${date.day.toString().padLeft(2, '0')}/'
        '${date.month.toString().padLeft(2, '0')}/${date.year}';
    return isToday ? 'Hôm nay, $value' : value;
  }

  String _timeLabel(DateTime date) {
    final local = date.toLocal();
    return '${local.hour.toString().padLeft(2, '0')}:'
        '${local.minute.toString().padLeft(2, '0')}';
  }

  String get _title => switch (widget.metricType) {
    'WEIGHT' => 'Lịch sử cân nặng',
    'HYDRATION' => 'Lịch sử uống nước',
    'EPDS_SCORE' => 'Lịch sử sàng lọc EPDS',
    'FETAL_MOVEMENT_COUNT' => 'Lịch sử cử động thai',
    'BLOOD_PRESSURE' => 'Lịch sử huyết áp',
    'BLOOD_GLUCOSE' => 'Lịch sử đường huyết',
    _ => 'Lịch sử chỉ số sức khỏe',
  };

  IconData get _icon => switch (widget.metricType) {
    'WEIGHT' => Icons.monitor_weight_outlined,
    'HYDRATION' => Icons.water_drop_outlined,
    'EPDS_SCORE' => Icons.psychology_alt_outlined,
    'FETAL_MOVEMENT_COUNT' => Icons.child_friendly_outlined,
    'BLOOD_PRESSURE' => Icons.monitor_heart_outlined,
    'BLOOD_GLUCOSE' => Icons.bloodtype_outlined,
    _ => Icons.sticky_note_2_outlined,
  };
}
