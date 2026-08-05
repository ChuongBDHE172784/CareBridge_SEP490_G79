import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../models/baby_daily_log_model.dart';
import '../models/baby_model.dart';
import '../services/baby_log_service.dart';
import '../services/baby_service.dart';

class BabyLogSummaryScreen extends StatefulWidget {
  final String babyId;
  final BabyLogService? logService;
  final BabyService? babyService;

  const BabyLogSummaryScreen({
    super.key,
    required this.babyId,
    this.logService,
    this.babyService,
  });

  @override
  State<BabyLogSummaryScreen> createState() => _BabyLogSummaryScreenState();
}

const _primary = Color(0xFF845143);
const _primaryContainer = Color(0xFFC98C7B);
const _canvas = Color(0xFFFFF8F6);
const _surface = Color(0xFFF2EAE4);
const _onSurface = Color(0xFF271812);
const _onSurfaceVariant = Color(0xFF524440);

class _BabyLogSummaryScreenState extends State<BabyLogSummaryScreen> {

  late final BabyLogService _logService = widget.logService ?? BabyLogService();
  late final BabyService _babyService = widget.babyService ?? BabyService();

  BabyLogSummaryResponse? _summary;
  List<BabyDailyLog> _logs = const [];
  List<BabyProfile> _babies = [];
  BabyProfile? _selectedBaby;
  String _period = '24h';
  bool _isLoading = true;
  String? _error;
  int _loadGeneration = 0;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData({String? babyId}) async {
    final generation = ++_loadGeneration;
    final requestedBabyId = babyId ?? _selectedBaby?.id ?? widget.babyId;
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final results = await Future.wait([
        _babyService.listBabyProfiles(),
        _logService.getLogSummary(requestedBabyId, period: _period),
        _logService.getDailyLogs(requestedBabyId),
      ]);
      if (!mounted || generation != _loadGeneration) return;
      final babies = results[0] as List<BabyProfile>;
      final summary = results[1] as BabyLogSummaryResponse;
      final logs = results[2] as List<BabyDailyLog>;
      if (summary.babyId != requestedBabyId) {
        throw const FormatException('Baby journal summary scope mismatch');
      }
      setState(() {
        _babies = babies;
        _selectedBaby = babies
            .where((b) => b.id == requestedBabyId)
            .firstOrNull;
        _summary = summary;
        _logs = _scopeLogs(
          logs,
          requestedBabyId,
          fromDate: summary.fromDate,
          toDate: summary.toDate,
        );
      });
    } catch (_) {
      if (!mounted || generation != _loadGeneration) return;
      setState(() => _error = 'Không thể tải dữ liệu. Vui lòng thử lại.');
    } finally {
      if (mounted && generation == _loadGeneration) {
        setState(() => _isLoading = false);
      }
    }
  }

  Future<void> _switchBaby(BabyProfile baby) async {
    final generation = ++_loadGeneration;
    setState(() {
      _selectedBaby = baby;
      _isLoading = true;
      _error = null;
    });
    try {
      final results = await Future.wait([
        _logService.getLogSummary(baby.id, period: _period),
        _logService.getDailyLogs(baby.id),
      ]);
      if (!mounted || generation != _loadGeneration) return;
      final summary = results[0] as BabyLogSummaryResponse;
      if (summary.babyId != baby.id) {
        throw const FormatException('Baby journal summary scope mismatch');
      }
      setState(() {
        _summary = summary;
        _logs = _scopeLogs(
          results[1] as List<BabyDailyLog>,
          baby.id,
          fromDate: summary.fromDate,
          toDate: summary.toDate,
        );
      });
    } catch (_) {
      if (mounted && generation == _loadGeneration) {
        setState(() => _error = 'Không thể tải dữ liệu.');
      }
    } finally {
      if (mounted && generation == _loadGeneration) {
        setState(() => _isLoading = false);
      }
    }
  }

  Future<void> _switchPeriod(String p) async {
    if (_period == p) return;
    final generation = ++_loadGeneration;
    setState(() {
      _period = p;
      _isLoading = true;
      _error = null;
    });
    try {
      final id = _selectedBaby?.id ?? widget.babyId;
      final results = await Future.wait([
        _logService.getLogSummary(id, period: p),
        _logService.getDailyLogs(id),
      ]);
      if (!mounted || generation != _loadGeneration) return;
      final summary = results[0] as BabyLogSummaryResponse;
      if (summary.babyId != id) {
        throw const FormatException('Baby journal summary scope mismatch');
      }
      setState(() {
        _summary = summary;
        _logs = _scopeLogs(
          results[1] as List<BabyDailyLog>,
          id,
          fromDate: summary.fromDate,
          toDate: summary.toDate,
        );
      });
    } catch (_) {
      if (mounted && generation == _loadGeneration) {
        setState(() => _error = 'Không thể tải dữ liệu.');
      }
    } finally {
      if (mounted && generation == _loadGeneration) {
        setState(() => _isLoading = false);
      }
    }
  }

  Future<void> _openLog(BabyDailyLog log) async {
    final babyId = _selectedBaby?.id ?? widget.babyId;
    if (log.babyId != babyId) return;
    await context.push('/babies/$babyId/daily-logs/${log.id}');
    if (mounted) await _loadData(babyId: babyId);
  }

  List<BabyDailyLog> _scopeLogs(
    List<BabyDailyLog> logs,
    String babyId, {
    DateTime? fromDate,
    DateTime? toDate,
  }) {
    return logs
        .where((log) {
          if (log.babyId != babyId) return false;
          final effectiveAt = log.startedAt ?? log.createdAt;
          if (effectiveAt == null || fromDate == null || toDate == null) {
            return true;
          }
          final instant = effectiveAt.toUtc();
          return !instant.isBefore(fromDate.toUtc()) &&
              instant.isBefore(toDate.toUtc());
        })
        .toList(growable: false);
  }

  Future<void> _openAddLogSheet() async {
    final saved = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) => _AddBabyLogSheet(
        babyId: _selectedBaby?.id ?? widget.babyId,
        logService: _logService,
        parentContext: context,
      ),
    );
    if (saved == true && mounted) {
      await _loadData(babyId: _selectedBaby?.id ?? widget.babyId);
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
        key: const Key('baby-log-add'),
        onPressed: _openAddLogSheet,
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
                      border: Border.all(
                        color: _primaryContainer.withAlpha(80),
                        width: 2,
                      ),
                    ),
                    child: const Icon(
                      Icons.child_care_rounded,
                      color: _primaryContainer,
                      size: 26,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          _selectedBaby?.nickname ?? 'Bé yêu',
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 18,
                            fontWeight: FontWeight.w700,
                            color: _onSurface,
                          ),
                        ),
                        Text(
                          _selectedBaby?.ageLabel ?? '',
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 12,
                            color: _onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  ),
                  if (_babies.length > 1)
                    PopupMenuButton<BabyProfile>(
                      icon: const Icon(
                        Icons.expand_more_rounded,
                        color: _primary,
                      ),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(16),
                      ),
                      color: Colors.white,
                      onSelected: _switchBaby,
                      itemBuilder: (_) => _babies
                          .map(
                            (b) => PopupMenuItem(
                              value: b,
                              child: Text(
                                b.nickname,
                                style: const TextStyle(
                                  fontFamily: 'Lexend',
                                  fontSize: 14,
                                  color: _onSurface,
                                ),
                              ),
                            ),
                          )
                          .toList(),
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
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(50),
      ),
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
        child: Center(
          child: CircularProgressIndicator(color: _primaryContainer),
        ),
      );
    }
    if (_error != null) {
      return SizedBox(
        height: 300,
        child: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(
                Icons.error_outline_rounded,
                color: _primaryContainer,
                size: 48,
              ),
              const SizedBox(height: 12),
              Text(
                _error!,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 13,
                  color: _onSurfaceVariant,
                ),
              ),
              const SizedBox(height: 12),
              TextButton(
                onPressed: _loadData,
                child: const Text('Thử lại', style: TextStyle(color: _primary)),
              ),
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
        value: formatSleepDuration(s?.sleep),
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
        boxShadow: [
          BoxShadow(
            color: _primary.withAlpha(12),
            blurRadius: 10,
            offset: const Offset(0, 3),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: item.color,
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(item.icon, color: item.iconColor, size: 20),
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                item.value,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 18,
                  fontWeight: FontWeight.w700,
                  color: _onSurface,
                ),
              ),
              Text(
                item.label,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 11,
                  color: _onSurfaceVariant,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildSimpleBarChart() {
    final entries =
        _summary?.summaries.entries
            .where((entry) => entry.value.count > 0)
            .toList() ??
        const <MapEntry<String, LogTypeSummary>>[];
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: _primary.withAlpha(12),
            blurRadius: 10,
            offset: const Offset(0, 3),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Tần suất theo loại nhật ký',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              fontWeight: FontWeight.w600,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 16),
          if (entries.isEmpty)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 16),
              child: Center(
                child: Text(
                  'Chưa có đủ dữ liệu để vẽ biểu đồ.',
                  key: Key('baby-log-chart-empty'),
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    color: _onSurfaceVariant,
                  ),
                ),
              ),
            )
          else ...[
            SizedBox(
              height: 80,
              child: _SimpleBarChart(
                bars: entries
                    .map(
                      (entry) =>
                          entry.value.count /
                          entries
                              .map((item) => item.value.count)
                              .reduce((a, b) => a > b ? a : b),
                    )
                    .toList(),
              ),
            ),
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: entries
                  .map(
                    (entry) => Expanded(
                      child: Text(
                        displayLogTypeLabel(entry.key),
                        textAlign: TextAlign.center,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 10,
                          color: _onSurfaceVariant,
                        ),
                      ),
                    ),
                  )
                  .toList(),
            ),
          ],
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
        boxShadow: [
          BoxShadow(
            color: _primary.withAlpha(12),
            blurRadius: 10,
            offset: const Offset(0, 3),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Nhật ký gần đây',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              fontWeight: FontWeight.w600,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 12),
          if (_logs.isEmpty)
            const Center(
              child: Padding(
                padding: EdgeInsets.symmetric(vertical: 16),
                child: Text(
                  'Chưa có nhật ký nào.',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 13,
                    color: _onSurfaceVariant,
                  ),
                ),
              ),
            )
          else
            ..._logs.take(20).map(_buildLogTile),
        ],
      ),
    );
  }

  Widget _buildLogTile(BabyDailyLog log) {
    final quantity = log.quantity == null
        ? null
        : '${log.quantity!.toStringAsFixed(log.quantity! % 1 == 0 ? 0 : 1)} ${log.unit ?? ''}'
              .trim();
    final details = [
      _formatLogDate(log.startedAt ?? log.createdAt),
      if (quantity != null && quantity.isNotEmpty) quantity,
      if (log.note?.trim().isNotEmpty == true) log.note!.trim(),
    ].join(' · ');
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Material(
        color: Colors.transparent,
        child: ListTile(
          key: ValueKey('baby-log-${log.id}'),
          contentPadding: EdgeInsets.zero,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          onTap: () => _openLog(log),
          leading: Container(
            width: 36,
            height: 36,
            decoration: BoxDecoration(
              color: _surface,
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(
              _logTypeIcon(log.logType),
              color: _primaryContainer,
              size: 18,
            ),
          ),
          title: Text(
            log.displayTypeLabel,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: _onSurface,
            ),
          ),
          subtitle: Text(
            details,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 11,
              color: _onSurfaceVariant,
            ),
          ),
          trailing: const Icon(Icons.chevron_right_rounded, color: _primary),
        ),
      ),
    );
  }

  String _formatLogDate(DateTime? value) {
    if (value == null) return 'Chưa có thời gian';
    final local = value.toLocal();
    return '${local.day.toString().padLeft(2, '0')}/${local.month.toString().padLeft(2, '0')} ${local.hour.toString().padLeft(2, '0')}:${local.minute.toString().padLeft(2, '0')}';
  }

  IconData _logTypeIcon(LogType type) {
    switch (type) {
      case LogType.feeding:
        return Icons.local_drink_rounded;
      case LogType.sleep:
        return Icons.bedtime_rounded;
      case LogType.diaper:
        return Icons.baby_changing_station_rounded;
      case LogType.fever:
        return Icons.thermostat_rounded;
      case LogType.vomiting:
        return Icons.sick_rounded;
      case LogType.medicine:
        return Icons.medication_rounded;
      case LogType.symptom:
        return Icons.thermostat_rounded;
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
              'Dữ liệu được tổng hợp từ nhật ký của người chăm sóc. Thông tin mang tính quan sát, không thay thế tư vấn chuyên môn.',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 11,
                color: _onSurfaceVariant,
                height: 1.5,
              ),
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
  const _BentoItem({
    required this.icon,
    required this.label,
    required this.value,
    required this.color,
    required this.iconColor,
  });
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
                borderRadius: const BorderRadius.vertical(
                  top: Radius.circular(3),
                ),
              ),
            );
          }).toList(),
        );
      },
    );
  }
}

class _AddBabyLogSheet extends StatefulWidget {
  final String babyId;
  final BabyLogService logService;
  final BuildContext parentContext;

  const _AddBabyLogSheet({
    required this.babyId,
    required this.logService,
    required this.parentContext,
  });

  @override
  State<_AddBabyLogSheet> createState() => _AddBabyLogSheetState();
}

class _AddBabyLogSheetState extends State<_AddBabyLogSheet> {
  late final TextEditingController _quantityController;
  late final TextEditingController _unitController;
  late final TextEditingController _noteController;
  final _formKey = GlobalKey<FormState>();
  LogType _selectedType = LogType.feeding;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _quantityController = TextEditingController();
    _unitController = TextEditingController(text: 'ml');
    _noteController = TextEditingController();
  }

  @override
  void dispose() {
    _quantityController.dispose();
    _unitController.dispose();
    _noteController.dispose();
    super.dispose();
  }

  void _onTypeChanged(LogType? next) {
    if (next == null || _saving) return;
    setState(() {
      _selectedType = next;
      switch (next) {
        case LogType.feeding:
          _unitController.text = 'ml';
        case LogType.sleep:
          _unitController.text = 'hours';
        case LogType.diaper:
        case LogType.fever:
        case LogType.vomiting:
        case LogType.medicine:
        case LogType.symptom:
          _quantityController.clear();
          _unitController.clear();
      }
    });
  }

  Future<void> _submit() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    setState(() => _saving = true);
    try {
      await widget.logService.addDailyLog(
        widget.babyId,
        AddBabyDailyLogRequest(
          logType: _selectedType,
          quantity: double.tryParse(_quantityController.text),
          unit: _unitController.text.trim().isEmpty
              ? null
              : _unitController.text.trim(),
          note: _noteController.text.trim().isEmpty
              ? null
              : _noteController.text.trim(),
          startedAt: DateTime.now(),
        ),
      );
      if (mounted) {
        Navigator.of(context).pop(true);
      }
    } catch (_) {
      if (mounted) {
        setState(() => _saving = false);
      }
      if (widget.parentContext.mounted) {
        ScaffoldMessenger.of(widget.parentContext).showSnackBar(
          const SnackBar(
            content: Text('Không thể lưu nhật ký. Vui lòng thử lại.'),
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.fromLTRB(
        20,
        12,
        20,
        MediaQuery.of(context).viewInsets.bottom + 20,
      ),
      decoration: const BoxDecoration(
        color: _canvas,
        borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
      ),
      child: SingleChildScrollView(
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Center(
                child: Container(
                  width: 42,
                  height: 4,
                  decoration: BoxDecoration(
                    color: _primaryContainer,
                    borderRadius: BorderRadius.circular(4),
                  ),
                ),
              ),
              const SizedBox(height: 16),
              const Text(
                'Thêm nhật ký',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 20,
                  fontWeight: FontWeight.w700,
                  color: _onSurface,
                ),
              ),
              const SizedBox(height: 12),
              DropdownButtonFormField<LogType>(
                value: _selectedType,
                decoration: const InputDecoration(labelText: 'Loại nhật ký'),
                items: LogType.values
                    .map(
                      (type) => DropdownMenuItem(
                        value: type,
                        child: Text(type.displayLabel),
                      ),
                    )
                    .toList(),
                onChanged: _onTypeChanged,
              ),
              const SizedBox(height: 8),
              TextFormField(
                controller: _quantityController,
                enabled: !_saving,
                keyboardType: const TextInputType.numberWithOptions(
                  decimal: true,
                ),
                decoration: const InputDecoration(
                  labelText: 'Số lượng (tuỳ chọn)',
                ),
                validator: (value) {
                  final raw = value?.trim() ?? '';
                  if (raw.isEmpty) return null;
                  final parsed = double.tryParse(raw);
                  if (parsed == null || !parsed.isFinite || parsed <= 0) {
                    return 'Nhập số dương hợp lệ';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 8),
              TextFormField(
                controller: _unitController,
                enabled: !_saving,
                decoration: const InputDecoration(labelText: 'Đơn vị'),
              ),
              const SizedBox(height: 8),
              TextFormField(
                controller: _noteController,
                enabled: !_saving,
                maxLines: 2,
                decoration: const InputDecoration(labelText: 'Ghi chú'),
              ),
              const SizedBox(height: 16),
              FilledButton(
                key: const Key('baby-log-save'),
                style: FilledButton.styleFrom(
                  backgroundColor: _primary,
                  minimumSize: const Size.fromHeight(48),
                ),
                onPressed: _saving ? null : _submit,
                child: Text(_saving ? 'Đang lưu...' : 'Lưu nhật ký'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
