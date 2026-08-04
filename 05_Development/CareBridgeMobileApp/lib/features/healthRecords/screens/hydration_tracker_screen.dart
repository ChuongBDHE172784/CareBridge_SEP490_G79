import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../models/health_metric_model.dart';
import '../services/health_metric_service.dart';

/// Daily drink tracker for pregnancy. It records drinks, not total water from food.
class HydrationTrackerScreen extends StatefulWidget {
  const HydrationTrackerScreen({super.key, required this.journeyId});

  final String journeyId;

  @override
  State<HydrationTrackerScreen> createState() => _HydrationTrackerScreenState();
}

class _HydrationTrackerScreenState extends State<HydrationTrackerScreen> {
  static const _primary = Color(0xFF845143);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Colors.white;
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _defaultGoalMl = 2300.0;

  final _service = HealthMetricService();
  static const _storage = FlutterSecureStorage();
  List<MetricDataPoint> _entries = const [];
  MetricCapability? _capability;
  double _goalMl = _defaultGoalMl;
  bool _loading = true;
  bool _saving = false;
  String? _error;

  double get _totalMl =>
      _entries.fold(0, (sum, entry) => sum + entry.valueNumeric);
  double get _progress => (_totalMl / _goalMl).clamp(0, 1);
  double get _remainingMl => (_goalMl - _totalMl).clamp(0, _goalMl);

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
    final now = DateTime.now();
    final start = DateTime(now.year, now.month, now.day);
    try {
      final results = await Future.wait([
        _service.getCapabilities(widget.journeyId),
        _service.getMetricTrend(
          journeyId: widget.journeyId,
          metricType: 'HYDRATION',
          from: start,
          to: start.add(const Duration(days: 1)),
        ),
      ]);
      final savedGoal = await _storage.read(key: _goalStorageKey);
      if (!mounted) return;
      final capabilities = results[0] as List<MetricCapability>;
      final trend = results[1] as MetricTrend;
      final capability = capabilities
          .where((item) => item.metricCode == 'HYDRATION')
          .firstOrNull;
      if (capability?.manualEntrySupported != true) {
        throw StateError('HYDRATION is not enabled');
      }
      final entries =
          trend.dataPoints
              .where(
                (entry) => entry.measuredAt.isAfter(
                  start.subtract(const Duration(microseconds: 1)),
                ),
              )
              .toList()
            ..sort((a, b) => b.measuredAt.compareTo(a.measuredAt));
      setState(() {
        _capability = capability;
        _entries = entries;
        _goalMl = double.tryParse(savedGoal ?? '') ?? _defaultGoalMl;
      });
    } catch (_) {
      if (mounted) _error = 'Chưa thể tải nhật ký nước. Vui lòng thử lại.';
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  String get _goalStorageKey => 'hydration_goal_ml_${widget.journeyId}';

  Future<void> _addDrink(double amount) async {
    if (_saving || _capability == null) return;
    setState(() => _saving = true);
    try {
      await _service.addMetric(
        widget.journeyId,
        AddMetricRequest(
          metricType: 'HYDRATION',
          valueNumeric: amount,
          unit: 'ml',
          measuredAt: DateTime.now(),
          definitionVersion: _capability!.version,
        ),
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Đã ghi nhận ${amount.toStringAsFixed(0)} ml nước'),
          backgroundColor: _primary,
        ),
      );
      await _load();
    } catch (_) {
      if (mounted) _showError('Không thể lưu lượng nước. Vui lòng thử lại.');
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  Future<void> _showCustomAmountSheet() async {
    final amount = await showModalBottomSheet<double>(
      context: context,
      isScrollControlled: true,
      builder: (_) => const _HydrationNumberSheet(
        title: 'Thêm lượng nước',
        description: 'Nhập dung tích vừa uống (ml).',
        label: 'Lượng nước (ml)',
        submitLabel: 'Ghi nhận',
        minimum: 1,
        maximum: 2000,
      ),
    );
    if (amount != null) await _addDrink(amount);
  }

  Future<void> _editGoal() async {
    final goal = await showModalBottomSheet<double>(
      context: context,
      isScrollControlled: true,
      builder: (_) => _HydrationNumberSheet(
        title: 'Điều chỉnh mục tiêu',
        description:
            'Mục tiêu mặc định là 2.300 ml đồ uống/ngày. Chỉ thay đổi theo tư vấn của bác sĩ hoặc nữ hộ sinh.',
        label: 'Mục tiêu mỗi ngày (ml)',
        submitLabel: 'Lưu mục tiêu',
        initialValue: _goalMl.toStringAsFixed(0),
        minimum: 1000,
        maximum: 4000,
      ),
    );
    if (goal == null) return;
    await _storage.write(key: _goalStorageKey, value: goal.toStringAsFixed(0));
    if (mounted) setState(() => _goalMl = goal);
  }

  void _showError(String message) => ScaffoldMessenger.of(context).showSnackBar(
    SnackBar(content: Text(message), backgroundColor: const Color(0xFFBA1A1A)),
  );

  @override
  Widget build(BuildContext context) => Scaffold(
    backgroundColor: _canvas,
    appBar: AppBar(
      backgroundColor: _canvas,
      elevation: 0,
      foregroundColor: _onSurface,
      title: const Text(
        'Theo dõi nước uống',
        style: TextStyle(
          fontFamily: 'Lexend',
          fontSize: 18,
          fontWeight: FontWeight.w700,
        ),
      ),
    ),
    body: _loading
        ? const Center(child: CircularProgressIndicator(color: _primary))
        : _error != null
        ? Center(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(_error!, textAlign: TextAlign.center),
                  const SizedBox(height: 12),
                  OutlinedButton(
                    onPressed: _load,
                    child: const Text('Thử lại'),
                  ),
                ],
              ),
            ),
          )
        : RefreshIndicator(
            color: _primary,
            onRefresh: _load,
            child: ListView(
              padding: const EdgeInsets.all(20),
              children: [
                _buildProgressCard(),
                const SizedBox(height: 20),
                const Text(
                  'Ghi nhanh',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 18,
                    fontWeight: FontWeight.w700,
                    color: _onSurface,
                  ),
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    for (final amount in [150.0, 250.0, 350.0])
                      Expanded(
                        child: Padding(
                          padding: EdgeInsets.only(
                            right: amount == 350 ? 0 : 8,
                          ),
                          child: OutlinedButton(
                            onPressed: _saving ? null : () => _addDrink(amount),
                            child: Text('+${amount.toStringAsFixed(0)} ml'),
                          ),
                        ),
                      ),
                  ],
                ),
                const SizedBox(height: 8),
                TextButton.icon(
                  onPressed: _saving ? null : _showCustomAmountSheet,
                  icon: const Icon(Icons.add_circle_outline),
                  label: const Text('Nhập lượng khác'),
                ),
                const SizedBox(height: 20),
                _buildGuidanceCard(),
                const SizedBox(height: 20),
                Text(
                  'Hôm nay',
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 18,
                    fontWeight: FontWeight.w700,
                    color: _onSurface,
                  ),
                ),
                const SizedBox(height: 8),
                if (_entries.isEmpty)
                  const Card(
                    child: Padding(
                      padding: EdgeInsets.all(16),
                      child: Text(
                        'Chưa có lần uống nào. Hãy bắt đầu bằng một cốc nước nhỏ.',
                      ),
                    ),
                  )
                else
                  ..._entries.map(
                    (entry) => Card(
                      child: ListTile(
                        leading: const Icon(
                          Icons.water_drop_outlined,
                          color: _primary,
                        ),
                        title: Text(
                          '${entry.valueNumeric.toStringAsFixed(0)} ml',
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        subtitle: Text(
                          TimeOfDay.fromDateTime(
                            entry.measuredAt,
                          ).format(context),
                        ),
                      ),
                    ),
                  ),
              ],
            ),
          ),
  );

  Widget _buildProgressCard() => Card(
    color: _surface,
    child: Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Align(
            alignment: Alignment.centerRight,
            child: TextButton.icon(
              onPressed: _editGoal,
              icon: const Icon(Icons.tune_rounded, size: 18),
              label: const Text('Điều chỉnh mục tiêu'),
            ),
          ),
          Text(
            '${_totalMl.toStringAsFixed(0)} / ${_goalMl.toStringAsFixed(0)} ml',
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 28,
              fontWeight: FontWeight.w700,
              color: _primary,
            ),
          ),
          const SizedBox(height: 10),
          LinearProgressIndicator(
            value: _progress,
            minHeight: 10,
            borderRadius: BorderRadius.circular(6),
            color: _primary,
            backgroundColor: const Color(0xFFF2EAE4),
          ),
          const SizedBox(height: 10),
          Text(
            _remainingMl > 0
                ? 'Còn ${_remainingMl.toStringAsFixed(0)} ml để đạt mục tiêu hôm nay.'
                : 'Mẹ đã đạt mục tiêu hôm nay.',
            style: const TextStyle(color: _onSurfaceVariant),
          ),
        ],
      ),
    ),
  );

  Widget _buildGuidanceCard() => const Card(
    child: Padding(
      padding: EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Gợi ý an toàn',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontWeight: FontWeight.w700,
              color: _onSurface,
            ),
          ),
          SizedBox(height: 6),
          Text(
            'Mục tiêu khởi đầu 2.300 ml/ngày là để theo dõi đồ uống, không tính nước từ thức ăn và không thay thế chỉ định riêng của bác sĩ. Uống rải đều trong ngày; nhu cầu có thể tăng khi trời nóng hoặc vận động.',
          ),
          SizedBox(height: 8),
          Text(
            'Nếu mẹ nôn nhiều, không giữ được nước, tiểu rất ít/sẫm màu, chóng mặt hoặc có chỉ định hạn chế dịch, hãy liên hệ cơ sở y tế.',
            style: TextStyle(
              color: Color(0xFF845143),
              fontWeight: FontWeight.w600,
            ),
          ),
        ],
      ),
    ),
  );
}

class _HydrationNumberSheet extends StatefulWidget {
  const _HydrationNumberSheet({
    required this.title,
    required this.description,
    required this.label,
    required this.submitLabel,
    required this.minimum,
    required this.maximum,
    this.initialValue = '',
  });

  final String title;
  final String description;
  final String label;
  final String submitLabel;
  final double minimum;
  final double maximum;
  final String initialValue;

  @override
  State<_HydrationNumberSheet> createState() => _HydrationNumberSheetState();
}

class _HydrationNumberSheetState extends State<_HydrationNumberSheet> {
  late final TextEditingController _controller = TextEditingController(
    text: widget.initialValue,
  );
  String? _error;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _submit() {
    final value = double.tryParse(_controller.text);
    if (value == null || value < widget.minimum || value > widget.maximum) {
      setState(
        () => _error =
            'Nhập giá trị từ ${widget.minimum.toStringAsFixed(0)} đến ${widget.maximum.toStringAsFixed(0)} ml.',
      );
      return;
    }
    Navigator.pop(context, value);
  }

  @override
  Widget build(BuildContext context) => Padding(
    padding: EdgeInsets.fromLTRB(
      24,
      24,
      24,
      MediaQuery.viewInsetsOf(context).bottom + 24,
    ),
    child: Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          widget.title,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 20,
            fontWeight: FontWeight.w700,
          ),
        ),
        const SizedBox(height: 8),
        Text(
          widget.description,
          style: const TextStyle(
            color: _HydrationTrackerScreenState._onSurfaceVariant,
          ),
        ),
        const SizedBox(height: 16),
        TextField(
          controller: _controller,
          autofocus: true,
          keyboardType: TextInputType.number,
          inputFormatters: [FilteringTextInputFormatter.digitsOnly],
          decoration: InputDecoration(
            labelText: widget.label,
            errorText: _error,
            border: const OutlineInputBorder(),
          ),
          onSubmitted: (_) => _submit(),
        ),
        const SizedBox(height: 16),
        ElevatedButton(
          onPressed: _submit,
          style: ElevatedButton.styleFrom(
            backgroundColor: _HydrationTrackerScreenState._primary,
            foregroundColor: Colors.white,
          ),
          child: Text(widget.submitLabel),
        ),
      ],
    ),
  );
}
