import 'package:flutter/material.dart';
import '../models/baby_daily_log_model.dart';
import '../models/baby_model.dart';
import '../services/baby_log_service.dart';
import '../services/baby_service.dart';

class BabyLogSummaryScreen extends StatefulWidget {
  final String babyId;

  const BabyLogSummaryScreen({super.key, required this.babyId});

  @override
  State<BabyLogSummaryScreen> createState() => _BabyLogSummaryScreenState();
}

class _BabyLogSummaryScreenState extends State<BabyLogSummaryScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Color(0xFFF2EAE4);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  final _logService = BabyLogService();
  final _babyService = BabyService();

  BabyLogSummaryResponse? _summary;
  List<BabyProfile> _babies = [];
  BabyProfile? _selectedBaby;
  String _period = '24h';
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() { _isLoading = true; _error = null; });
    try {
      final results = await Future.wait([
        _babyService.listBabyProfiles(),
        _logService.getLogSummary(widget.babyId, period: _period),
      ]);
      final babies = results[0] as List<BabyProfile>;
      final summary = results[1] as BabyLogSummaryResponse;
      setState(() {
        _babies = babies;
        _selectedBaby = babies.firstWhere((b) => b.id == widget.babyId, orElse: () => babies.first);
        _summary = summary;
      });
    } catch (_) {
      setState(() => _error = 'Không thể tải dữ liệu. Vui lòng thử lại.');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _switchBaby(BabyProfile baby) async {
    setState(() { _selectedBaby = baby; _isLoading = true; _error = null; });
    try {
      final summary = await _logService.getLogSummary(baby.id, period: _period);
      setState(() => _summary = summary);
    } catch (_) {
      setState(() => _error = 'Không thể tải dữ liệu.');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _switchPeriod(String p) async {
    if (_period == p) return;
    setState(() { _period = p; _isLoading = true; _error = null; });
    try {
      final id = _selectedBaby?.id ?? widget.babyId;
      final summary = await _logService.getLogSummary(id, period: p);
      setState(() => _summary = summary);
    } catch (_) {
      setState(() => _error = 'Không thể tải dữ liệu.');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: CustomScrollView(
        slivers: [
          _buildHeader(),
          SliverToBoxAdapter(child: _buildContent()),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {},
        backgroundColor: _primary,
        foregroundColor: Colors.white,
        shape: const CircleBorder(),
        child: const Icon(Icons.add_rounded),
      ),
    );
  }

  Widget _buildHeader() {
    return SliverAppBar(
      pinned: true,
      backgroundColor: _canvas,
      elevation: 0,
      expandedHeight: 160,
      leading: IconButton(
        icon: const Icon(Icons.arrow_back_rounded, color: _onSurface),
        onPressed: () => Navigator.of(context).pop(),
      ),
      flexibleSpace: FlexibleSpaceBar(
        background: Container(
          color: _canvas,
          padding: const EdgeInsets.fromLTRB(16, 56, 16, 0),
          child: Column(
            children: [
              Row(
                children: [
                  Container(
                    width: 52,
                    height: 52,
                    decoration: BoxDecoration(
                      color: _surface,
                      shape: BoxShape.circle,
                      border: Border.all(color: _primaryContainer.withAlpha(80), width: 2),
                    ),
                    child: const Icon(Icons.child_care_rounded, color: _primaryContainer, size: 26),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          _selectedBaby?.nickname ?? 'Bé yêu',
                          style: const TextStyle(fontFamily: 'Lexend', fontSize: 18, fontWeight: FontWeight.w700, color: _onSurface),
                        ),
                        Text(
                          _selectedBaby?.ageLabel ?? '',
                          style: const TextStyle(fontFamily: 'Lexend', fontSize: 12, color: _onSurfaceVariant),
                        ),
                      ],
                    ),
                  ),
                  if (_babies.length > 1)
                    PopupMenuButton<BabyProfile>(
                      icon: const Icon(Icons.expand_more_rounded, color: _primary),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                      color: Colors.white,
                      onSelected: _switchBaby,
                      itemBuilder: (_) => _babies.map((b) => PopupMenuItem(
                        value: b,
                        child: Text(b.nickname, style: const TextStyle(fontFamily: 'Lexend', fontSize: 14, color: _onSurface)),
                      )).toList(),
                    ),
                  const SizedBox(width: 8),
                  IconButton(
                    icon: const Icon(Icons.calendar_month_rounded, color: _primary),
                    onPressed: () {},
                  ),
                ],
              ),
              const SizedBox(height: 12),
              _buildPeriodToggle(),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildPeriodToggle() {
    return Container(
      height: 40,
      decoration: BoxDecoration(color: _surface, borderRadius: BorderRadius.circular(50)),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: ['24h', '7 ngày'].map((p) {
          final isSelected = _period == p || (_period == '7d' && p == '7 ngày');
          final apiVal = p == '7 ngày' ? '7d' : p;
          return GestureDetector(
            onTap: () => _switchPeriod(apiVal),
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 200),
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 10),
              decoration: BoxDecoration(
                color: isSelected ? _primary : Colors.transparent,
                borderRadius: BorderRadius.circular(50),
              ),
              child: Text(
                p,
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: isSelected ? Colors.white : _onSurfaceVariant,
                ),
              ),
            ),
          );
        }).toList(),
      ),
    );
  }

  Widget _buildContent() {
    if (_isLoading) {
      return const SizedBox(
        height: 300,
        child: Center(child: CircularProgressIndicator(color: _primaryContainer)),
      );
    }
    if (_error != null) {
      return SizedBox(
        height: 300,
        child: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.error_outline_rounded, color: _primaryContainer, size: 48),
              const SizedBox(height: 12),
              Text(_error!, style: const TextStyle(fontFamily: 'Lexend', fontSize: 13, color: _onSurfaceVariant)),
              const SizedBox(height: 12),
              TextButton(onPressed: _loadData, child: const Text('Thử lại', style: TextStyle(color: _primary))),
            ],
          ),
        ),
      );
    }
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 80),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          _buildBentoGrid(),
          const SizedBox(height: 20),
          _buildSimpleBarChart(),
          const SizedBox(height: 20),
          _buildRecentEvents(),
          const SizedBox(height: 16),
          _buildDisclaimer(),
        ],
      ),
    );
  }

  Widget _buildBentoGrid() {
    final s = _summary;
    final items = [
      _BentoItem(
        icon: Icons.local_drink_rounded,
        label: 'Bú & Ăn',
        value: s?.feeding?.totalQuantity != null
            ? '${s!.feeding!.totalQuantity!.toStringAsFixed(0)} ${s.feeding!.unit ?? 'ml'}'
            : '${s?.feeding?.count ?? 0} lần',
        color: const Color(0xFFE8F4FD),
        iconColor: const Color(0xFF2196F3),
      ),
      _BentoItem(
        icon: Icons.bedtime_rounded,
        label: 'Giấc ngủ',
        value: s?.sleep?.totalQuantity != null
            ? '${(s!.sleep!.totalQuantity! / 60).toStringAsFixed(1)} h'
            : '${s?.sleep?.count ?? 0} lần',
        color: const Color(0xFFF3E8FF),
        iconColor: const Color(0xFF9C27B0),
      ),
      _BentoItem(
        icon: Icons.baby_changing_station_rounded,
        label: 'Tã lót',
        value: '${s?.diaper?.count ?? 0} lần',
        color: const Color(0xFFFFF8E1),
        iconColor: const Color(0xFFFF9800),
      ),
      _BentoItem(
        icon: Icons.thermostat_rounded,
        label: 'Triệu chứng',
        value: s?.symptom?.maxValue != null
            ? '${s!.symptom!.maxValue!.toStringAsFixed(1)}°C'
            : '${s?.symptom?.count ?? 0} lần',
        color: const Color(0xFFFFEBEE),
        iconColor: Colors.red,
      ),
    ];

    return GridView.count(
      crossAxisCount: 2,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      crossAxisSpacing: 12,
      mainAxisSpacing: 12,
      childAspectRatio: 1.5,
      children: items.map(_buildBentoCard).toList(),
    );
  }

  Widget _buildBentoCard(_BentoItem item) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [BoxShadow(color: _primary.withAlpha(12), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(color: item.color, borderRadius: BorderRadius.circular(12)),
            child: Icon(item.icon, color: item.iconColor, size: 20),
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(item.value, style: const TextStyle(fontFamily: 'Lexend', fontSize: 18, fontWeight: FontWeight.w700, color: _onSurface)),
              Text(item.label, style: const TextStyle(fontFamily: 'Lexend', fontSize: 11, color: _onSurfaceVariant)),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildSimpleBarChart() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [BoxShadow(color: _primary.withAlpha(12), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Biểu đồ 24 giờ', style: TextStyle(fontFamily: 'Lexend', fontSize: 14, fontWeight: FontWeight.w600, color: _onSurface)),
          const SizedBox(height: 16),
          SizedBox(
            height: 80,
            child: _SimpleBarChart(
              bars: List.generate(24, (i) {
                // Placeholder bars since we don't have hourly breakdown from API
                return (i % 6 == 0 ? 0.8 : (i % 3 == 0 ? 0.5 : 0.2));
              }),
            ),
          ),
          const SizedBox(height: 8),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: ['0h', '6h', '12h', '18h', '24h'].map((t) => Text(
              t,
              style: const TextStyle(fontFamily: 'Lexend', fontSize: 10, color: _onSurfaceVariant),
            )).toList(),
          ),
        ],
      ),
    );
  }

  Widget _buildRecentEvents() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [BoxShadow(color: _primary.withAlpha(12), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Sự kiện gần đây', style: TextStyle(fontFamily: 'Lexend', fontSize: 14, fontWeight: FontWeight.w600, color: _onSurface)),
          const SizedBox(height: 12),
          if (_summary?.summaries.isEmpty ?? true)
            const Center(
              child: Padding(
                padding: EdgeInsets.symmetric(vertical: 16),
                child: Text('Chưa có nhật ký nào.', style: TextStyle(fontFamily: 'Lexend', fontSize: 13, color: _onSurfaceVariant)),
              ),
            )
          else
            ..._summary!.summaries.entries.map((e) => _buildEventTile(e.key, e.value)),
        ],
      ),
    );
  }

  Widget _buildEventTile(String logType, LogTypeSummary summary) {
    final type = LogTypeExtension.fromApi(logType);
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        children: [
          Container(
            width: 36,
            height: 36,
            decoration: BoxDecoration(color: _surface, borderRadius: BorderRadius.circular(10)),
            child: Icon(_logTypeIcon(type), color: _primaryContainer, size: 18),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(type.displayLabel, style: const TextStyle(fontFamily: 'Lexend', fontSize: 13, fontWeight: FontWeight.w600, color: _onSurface)),
                if (summary.latestNote != null)
                  Text(summary.latestNote!, style: const TextStyle(fontFamily: 'Lexend', fontSize: 11, color: _onSurfaceVariant), maxLines: 1, overflow: TextOverflow.ellipsis),
              ],
            ),
          ),
          Text('${summary.count} lần', style: const TextStyle(fontFamily: 'Lexend', fontSize: 12, fontWeight: FontWeight.w600, color: _primary)),
        ],
      ),
    );
  }

  IconData _logTypeIcon(LogType type) {
    switch (type) {
      case LogType.feeding: return Icons.local_drink_rounded;
      case LogType.sleep: return Icons.bedtime_rounded;
      case LogType.diaper: return Icons.baby_changing_station_rounded;
      case LogType.symptom: return Icons.thermostat_rounded;
    }
  }

  Widget _buildDisclaimer() {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(16),
      ),
      child: const Row(
        children: [
          Icon(Icons.info_outline_rounded, size: 16, color: _onSurfaceVariant),
          SizedBox(width: 8),
          Expanded(
            child: Text(
              'Dữ liệu được tổng hợp từ nhật ký của mẹ. AI cung cấp gợi ý, không thay thế tư vấn y tế.',
              style: TextStyle(fontFamily: 'Lexend', fontSize: 11, color: _onSurfaceVariant, height: 1.5),
            ),
          ),
        ],
      ),
    );
  }
}

class _BentoItem {
  final IconData icon;
  final String label;
  final String value;
  final Color color;
  final Color iconColor;
  const _BentoItem({required this.icon, required this.label, required this.value, required this.color, required this.iconColor});
}

class _SimpleBarChart extends StatelessWidget {
  const _SimpleBarChart({required this.bars});

  final List<double> bars;

  static const _primary = Color(0xFFC98C7B);

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (_, constraints) {
        final barWidth = constraints.maxWidth / bars.length;
        return Row(
          crossAxisAlignment: CrossAxisAlignment.end,
          children: bars.map((h) {
            return Container(
              width: barWidth - 1,
              height: constraints.maxHeight * h,
              margin: const EdgeInsets.symmetric(horizontal: 0.5),
              decoration: BoxDecoration(
                color: _primary.withAlpha((h * 200).toInt()),
                borderRadius: const BorderRadius.vertical(top: Radius.circular(3)),
              ),
            );
          }).toList(),
        );
      },
    );
  }
}
