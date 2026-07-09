import 'dart:math';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_client.dart';
import '../../healthRecords/models/health_metric_model.dart';
import '../../healthRecords/services/health_metric_service.dart';
import '../../reminder/models/reminder_model.dart';
import '../../reminder/services/reminder_service.dart';
import '../models/journey_model.dart';
import '../services/journey_service.dart';

/// CB-009 - Mother Journey (UC-23, UC-24, UC-25, UC-26, UC-27, UC-28)
/// Shows the active mother journey from GET /api/v1/journeys/me/dashboard.
class MotherJourneyScreen extends StatefulWidget {
  const MotherJourneyScreen({super.key});

  @override
  State<MotherJourneyScreen> createState() => _MotherJourneyScreenState();
}

class _MotherJourneyScreenState extends State<MotherJourneyScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surfaceContainerHigh = Color(0xFFFFE2D9);
  static const _surfaceContainerLowest = Color(0xFFFFF8F6);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);

  final _journeyService = JourneyService();
  final _reminderService = ReminderService.instance;
  final _healthMetricService = HealthMetricService();
  JourneyDashboard? _dashboard;
  List<Reminder> _reminders = [];
  MetricTrend? _weightTrend;
  bool _loading = true;
  String? _error;

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

    try {
      final dashboard = await _journeyService.getDashboard();
      final reminders = await _loadReminders();
      final weightTrend =
          dashboard.hasActiveJourney && dashboard.journeyId != null
          ? await _loadWeightTrend(dashboard.journeyId!)
          : null;
      if (!mounted) return;
      setState(() {
        _dashboard = dashboard;
        _reminders = reminders;
        _weightTrend = weightTrend;
        _loading = false;
      });
    } on ApiException catch (_) {
      if (!mounted) return;
      setState(() {
        _dashboard = null;
        _error = 'Không thể tải dữ liệu hành trình.';
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _dashboard = null;
        _error = 'Lỗi kết nối. Vui lòng kéo để thử lại.';
        _loading = false;
      });
    }
  }

  Future<List<Reminder>> _loadReminders() async {
    try {
      return await _reminderService.listTodayReminders();
    } catch (_) {
      return [];
    }
  }

  Future<MetricTrend?> _loadWeightTrend(String journeyId) async {
    try {
      return await _healthMetricService.getMetricTrend(
        journeyId: journeyId,
        metricType: 'WEIGHT',
        from: DateTime.now().subtract(const Duration(days: 28)),
        to: DateTime.now(),
      );
    } catch (_) {
      return null;
    }
  }

  Reminder? _nearestReminder(ReminderType type) {
    final pending =
        _reminders
            .where(
              (r) =>
                  r.reminderType == type && r.status == ReminderStatus.pending,
            )
            .toList()
          ..sort((a, b) => a.scheduledAt.compareTo(b.scheduledAt));
    return pending.firstOrNull;
  }

  String _formatDate(DateTime? date) {
    if (date == null) return 'Chưa có';
    return '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year}';
  }

  String _formatDateTime(DateTime date) {
    final day = date.day.toString().padLeft(2, '0');
    final month = date.month.toString().padLeft(2, '0');
    final hour = date.hour.toString().padLeft(2, '0');
    final minute = date.minute.toString().padLeft(2, '0');
    return '$hour:$minute - $day/$month/${date.year}';
  }

  String _daysUntilDueLabel(JourneyDashboard dashboard) {
    final days = dashboard.calculatedDaysUntilDue;
    if (days == null) return 'Chưa có ngày dự sinh';
    if (days == 0) return 'Dự sinh hôm nay';
    if (days > 0) return 'Còn lại $days ngày';
    return 'Quá ngày dự sinh ${days.abs()} ngày';
  }

  @override
  Widget build(BuildContext context) {
    final dashboard = _dashboard;
    final hasJourney = dashboard?.hasActiveJourney == true;

    return RefreshIndicator(
      color: _primaryContainer,
      onRefresh: _load,
      child: CustomScrollView(
        slivers: [
          SliverToBoxAdapter(child: _buildHeader()),
          SliverPadding(
            padding: const EdgeInsets.symmetric(horizontal: 24),
            sliver: SliverList(
              delegate: SliverChildListDelegate([
                if (_loading)
                  const Padding(
                    padding: EdgeInsets.only(top: 80),
                    child: Center(
                      child: CircularProgressIndicator(
                        color: _primaryContainer,
                      ),
                    ),
                  )
                else if (!hasJourney)
                  _buildEmptyJourneyCard()
                else ...[
                  _buildPhaseChip(dashboard!),
                  const SizedBox(height: 24),
                  _buildHeroCard(dashboard),
                  const SizedBox(height: 16),
                  _buildDueDateCard(dashboard),
                  const SizedBox(height: 16),
                  _buildNextAppointmentCard(),
                  const SizedBox(height: 16),
                  _buildVaccinationCard(),
                  const SizedBox(height: 16),
                  _buildSetupSourceCard(dashboard),
                  const SizedBox(height: 24),
                  _buildMetricButtons(),
                  const SizedBox(height: 24),
                  _buildWeightChart(),
                  const SizedBox(height: 24),
                ],
              ]),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildHeader() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 16, 8, 16),
      child: Row(
        children: [
          const Expanded(
            child: Text(
              'Hành trình của Mẹ',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 24,
                fontWeight: FontWeight.w700,
                color: _onSurface,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPhaseChip(JourneyDashboard dashboard) {
    return Align(
      alignment: Alignment.centerLeft,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
        decoration: BoxDecoration(
          color: _primaryContainer,
          borderRadius: BorderRadius.circular(99),
        ),
        child: Text(
          dashboard.phaseLabel,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 14,
            fontWeight: FontWeight.w700,
            color: Colors.white,
          ),
        ),
      ),
    );
  }

  Widget _buildEmptyJourneyCard() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(24),
      decoration: _cardDecoration(),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 48,
            height: 48,
            decoration: const BoxDecoration(
              color: _surfaceContainerHigh,
              shape: BoxShape.circle,
            ),
            child: Icon(
              _error == null ? Icons.route_rounded : Icons.wifi_off_rounded,
              color: _primary,
            ),
          ),
          const SizedBox(height: 16),
          Text(
            _error ?? 'Chưa có hành trình thai kỳ',
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 20,
              fontWeight: FontWeight.w700,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 8),
          const Text(
            'Thiết lập ngày dự sinh hoặc ngày chu kỳ để CareBridge hiển thị dữ liệu cá nhân của mẹ.',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _onSurfaceVariant,
              height: 1.45,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildHeroCard(JourneyDashboard dashboard) {
    final week = dashboard.displayPregnancyWeek;
    final weekTitle = week != null ? 'Tuần $week' : dashboard.phaseLabel;
    final statusLabel = dashboard.displayTrimester != null
        ? 'Tam cá nguyệt ${dashboard.displayTrimester}'
        : dashboard.phaseLabel;

    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFFFFF1EC), Color(0xFFFFE2D9)],
        ),
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: Colors.white.withAlpha(128)),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF845143).withAlpha(20),
            blurRadius: 24,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 4,
                  ),
                  decoration: BoxDecoration(
                    color: Colors.white.withAlpha(178),
                    borderRadius: BorderRadius.circular(99),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(Icons.favorite, size: 14, color: _primary),
                      const SizedBox(width: 4),
                      Text(
                        statusLabel,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 11,
                          color: _primary,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 12),
                Text(
                  weekTitle,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 36,
                    fontWeight: FontWeight.w700,
                    color: _onSurface,
                    letterSpacing: -0.5,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  week != null
                      ? 'Bé đang lớn bằng ${dashboard.fruitName}, ${dashboard.fruitSizeNote}.'
                      : 'CareBridge đang theo dõi hành trình từ dữ liệu mẹ đã thiết lập.',
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    color: _onSurfaceVariant,
                    height: 1.5,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 24),
          _CircularProgressWidget(
            progress: dashboard.pregnancyProgress,
            week: week,
          ),
        ],
      ),
    );
  }

  Widget _buildDueDateCard(JourneyDashboard dashboard) {
    final dueDate = dashboard.estimatedDueDate;
    final monthLabel = dueDate != null
        ? 'THG ${dueDate.month.toString().padLeft(2, '0')}'
        : '--';
    final dayLabel = dueDate != null ? '${dueDate.day}' : '--';

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: _cardDecoration(),
      child: Row(
        children: [
          Container(
            width: 64,
            height: 64,
            decoration: BoxDecoration(
              color: _surfaceContainerHigh,
              borderRadius: BorderRadius.circular(16),
            ),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  monthLabel,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 11,
                    fontWeight: FontWeight.w600,
                    color: _primary,
                  ),
                ),
                Text(
                  dayLabel,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 22,
                    fontWeight: FontWeight.w700,
                    color: _onSurface,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Ngày dự sinh',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                    color: _onSurface,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  _daysUntilDueLabel(dashboard),
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    color: _onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
          IconButton(
            onPressed: () =>
                context.push('/journey-setup').then((_) => _load()),
            icon: const Icon(Icons.edit_outlined, color: _onSurfaceVariant),
            tooltip: 'Cập nhật ngày dự sinh',
          ),
        ],
      ),
    );
  }

  Widget _buildNextAppointmentCard() {
    final appointment = _nearestReminder(ReminderType.appointment);
    final hasData = appointment != null;

    return GestureDetector(
      onTap: hasData
          ? () => context.push('/reminders/detail/${appointment.id}')
          : null,
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: const Color(0xFFF2EAE4),
          borderRadius: BorderRadius.circular(24),
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
                      const Text(
                        'Lịch hẹn tiếp theo',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 16,
                          fontWeight: FontWeight.w700,
                          color: _onSurface,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        appointment?.title ?? 'Chưa có lịch khám sắp tới',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 14,
                          color: _onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                ),
                Container(
                  width: 40,
                  height: 40,
                  decoration: const BoxDecoration(
                    color: _surfaceContainerLowest,
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(
                    Icons.calendar_today,
                    color: _primary,
                    size: 20,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            const Divider(color: Color(0xFFD6C2BD), thickness: 1),
            const SizedBox(height: 12),
            Row(
              children: [
                const Icon(Icons.schedule, size: 16, color: _onSurfaceVariant),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    appointment != null
                        ? [
                            _formatDateTime(appointment.scheduledAt),
                            if (appointment.location != null)
                              appointment.location!,
                          ].join(' • ')
                        : 'Chưa có dữ liệu',
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 14,
                      color: _onSurfaceVariant,
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildVaccinationCard() {
    final vaccination = _nearestReminder(ReminderType.vaccination);
    final hasData = vaccination != null;

    return GestureDetector(
      onTap: hasData
          ? () => context.push('/reminders/detail/${vaccination.id}')
          : null,
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: const Color(0xFFF2EAE4),
          borderRadius: BorderRadius.circular(24),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Container(
              width: 40,
              height: 40,
              decoration: const BoxDecoration(
                color: _surfaceContainerLowest,
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.vaccines, color: _primary, size: 20),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Thông tin tiêm phòng',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 16,
                      fontWeight: FontWeight.w700,
                      color: _onSurface,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    vaccination != null
                        ? '${vaccination.title} (${_formatDateTime(vaccination.scheduledAt)})'
                        : 'Chưa có lịch tiêm phòng',
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 14,
                      color: _onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
            Icon(
              hasData ? Icons.chevron_right : Icons.info_outline_rounded,
              color: _onSurfaceVariant,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSetupSourceCard(JourneyDashboard dashboard) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: const Color(0xFFF2EAE4),
        borderRadius: BorderRadius.circular(24),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Dữ liệu đã thiết lập',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 16,
              fontWeight: FontWeight.w700,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 14),
          _InfoRow(
            icon: Icons.today_rounded,
            label: 'Ngày bắt đầu',
            value: _formatDate(dashboard.startDate),
          ),
          const SizedBox(height: 10),
          _InfoRow(
            icon: Icons.calendar_month_rounded,
            label: 'Ngày đầu chu kỳ',
            value: _formatDate(dashboard.lastMenstrualDate),
          ),
          const SizedBox(height: 10),
          _InfoRow(
            icon: Icons.event_available_rounded,
            label: 'Ngày dự sinh',
            value: _formatDate(dashboard.estimatedDueDate),
          ),
        ],
      ),
    );
  }

  Widget _buildMetricButtons() {
    final metrics = [
      (Icons.monitor_weight, 'Cân nặng', '/health-metrics/weight'),
      (Icons.favorite_border, 'Huyết áp', '/health-metrics/blood_pressure'),
      (Icons.history_edu, 'Hồ sơ sức khỏe', '/health-records'),
      (Icons.psychology_alt_outlined, 'Kiểm tra triệu chứng', '/triage/intake'),
      (Icons.health_and_safety_outlined, 'Giám sát an toàn', '/safety'),
    ];

    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: metrics.asMap().entries.map((entry) {
          final i = entry.key;
          final metric = entry.value;
          return Padding(
            padding: EdgeInsets.only(right: i < metrics.length - 1 ? 12 : 0),
            child: GestureDetector(
              onTap: () => context.push(metric.$3),
              child: Container(
                width: 104,
                padding: const EdgeInsets.symmetric(
                  vertical: 16,
                  horizontal: 8,
                ),
                decoration: BoxDecoration(
                  color: _surfaceContainerLowest,
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: _outlineVariant),
                ),
                child: Column(
                  children: [
                    Container(
                      width: 40,
                      height: 40,
                      decoration: const BoxDecoration(
                        color: _surfaceContainerHigh,
                        shape: BoxShape.circle,
                      ),
                      child: Icon(metric.$1, color: _primary, size: 20),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      metric.$2,
                      textAlign: TextAlign.center,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 11,
                        fontWeight: FontWeight.w600,
                        color: _onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          );
        }).toList(),
      ),
    );
  }

  Widget _buildWeightChart() {
    final points =
        (_weightTrend?.dataPoints ?? [])
            .where((p) => p.valueNumeric.isFinite)
            .toList()
          ..sort((a, b) => a.measuredAt.compareTo(b.measuredAt));
    final chartPoints = points.length > 6
        ? points.sublist(points.length - 6)
        : points;
    final values = chartPoints.map((p) => p.valueNumeric).toList();
    final labels = chartPoints
        .map((p) => '${p.measuredAt.day}/${p.measuredAt.month}')
        .toList();
    final unit = _weightTrend?.unit?.isNotEmpty == true
        ? _weightTrend!.unit!
        : 'kg';

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: _cardDecoration(),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Biểu đồ cân nặng',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  fontWeight: FontWeight.w700,
                  color: _onSurface,
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 6,
                ),
                decoration: BoxDecoration(
                  color: _surfaceContainerHigh,
                  borderRadius: BorderRadius.circular(99),
                ),
                child: const Text(
                  '4 tuần',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    color: _onSurfaceVariant,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Container(
            height: 160,
            width: double.infinity,
            decoration: BoxDecoration(
              color: _surfaceContainerHigh,
              borderRadius: BorderRadius.circular(12),
            ),
            padding: const EdgeInsets.fromLTRB(12, 16, 12, 8),
            child: values.isEmpty
                ? const Center(
                    child: Text(
                      'Chưa có dữ liệu cân nặng',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 14,
                        color: _onSurfaceVariant,
                      ),
                    ),
                  )
                : CustomPaint(
                    painter: _BarChartPainter(
                      data: values,
                      labels: labels,
                      primaryColor: _primary,
                      barColor: _primaryContainer,
                      labelColor: _onSurfaceVariant,
                    ),
                  ),
          ),
          const SizedBox(height: 8),
          Text(
            'Đơn vị: $unit',
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 11,
              color: _onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }

  BoxDecoration _cardDecoration() {
    return BoxDecoration(
      color: _surfaceContainerLowest,
      borderRadius: BorderRadius.circular(24),
      boxShadow: [
        BoxShadow(
          color: const Color(0xFF5A463F).withAlpha(15),
          blurRadius: 20,
          offset: const Offset(0, 4),
        ),
      ],
    );
  }
}

class _InfoRow extends StatelessWidget {
  const _InfoRow({
    required this.icon,
    required this.label,
    required this.value,
  });

  final IconData icon;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Container(
          width: 36,
          height: 36,
          decoration: const BoxDecoration(
            color: Color(0xFFFFF8F6),
            shape: BoxShape.circle,
          ),
          child: Icon(icon, size: 18, color: const Color(0xFF845143)),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Text(
            label,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 13,
              color: Color(0xFF524440),
            ),
          ),
        ),
        Text(
          value,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 13,
            fontWeight: FontWeight.w700,
            color: Color(0xFF271812),
          ),
        ),
      ],
    );
  }
}

class _CircularProgressWidget extends StatelessWidget {
  const _CircularProgressWidget({required this.progress, required this.week});

  final double progress;
  final int? week;

  @override
  Widget build(BuildContext context) {
    final pct = (progress * 100).round();
    return SizedBox(
      width: 110,
      height: 110,
      child: CustomPaint(
        painter: _CircleProgressPainter(progress: progress),
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                '$pct%',
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 20,
                  fontWeight: FontWeight.w700,
                  color: Color(0xFF271812),
                ),
              ),
              Text(
                week != null ? 'Thai kỳ' : 'Thiết lập',
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 11,
                  color: Color(0xFF524440),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _BarChartPainter extends CustomPainter {
  const _BarChartPainter({
    required this.data,
    required this.labels,
    required this.primaryColor,
    required this.barColor,
    required this.labelColor,
  });

  final List<double> data;
  final List<String> labels;
  final Color primaryColor;
  final Color barColor;
  final Color labelColor;

  @override
  void paint(Canvas canvas, Size size) {
    if (data.isEmpty) return;

    const bottomLabelHeight = 24.0;
    final chartHeight = size.height - bottomLabelHeight;
    final maxValue = data.reduce(max);
    final minValue = data.reduce(min);
    final range = (maxValue - minValue).abs() < 0.1 ? 1.0 : maxValue - minValue;
    final slotWidth = size.width / data.length;
    final barWidth = min(28.0, slotWidth * 0.46);

    final gridPaint = Paint()
      ..color = const Color(0xFFF2EAE4)
      ..strokeWidth = 1;
    final barPaint = Paint()
      ..color = barColor
      ..style = PaintingStyle.fill;
    final capPaint = Paint()
      ..color = primaryColor
      ..style = PaintingStyle.fill;

    for (var i = 0; i < 4; i++) {
      final y = chartHeight * i / 3;
      canvas.drawLine(Offset(0, y), Offset(size.width, y), gridPaint);
    }

    for (var i = 0; i < data.length; i++) {
      final normalized = ((data[i] - minValue) / range).clamp(0.0, 1.0);
      final barHeight = 28 + normalized * max(0, chartHeight - 40);
      final left = i * slotWidth + (slotWidth - barWidth) / 2;
      final top = chartHeight - barHeight;
      final rect = RRect.fromRectAndRadius(
        Rect.fromLTWH(left, top, barWidth, barHeight),
        const Radius.circular(10),
      );

      canvas.drawRRect(rect, barPaint);
      canvas.drawCircle(Offset(left + barWidth / 2, top), 4, capPaint);

      final label = i < labels.length ? labels[i] : '';
      if (label.isNotEmpty) {
        final textPainter = TextPainter(
          text: TextSpan(
            text: label,
            style: TextStyle(
              color: labelColor,
              fontFamily: 'Lexend',
              fontSize: 10,
              fontWeight: FontWeight.w600,
            ),
          ),
          textDirection: TextDirection.ltr,
          textAlign: TextAlign.center,
        )..layout(maxWidth: slotWidth);
        textPainter.paint(
          canvas,
          Offset(
            i * slotWidth + (slotWidth - textPainter.width) / 2,
            chartHeight + 8,
          ),
        );
      }
    }
  }

  @override
  bool shouldRepaint(_BarChartPainter oldDelegate) {
    return oldDelegate.data != data ||
        oldDelegate.labels != labels ||
        oldDelegate.primaryColor != primaryColor ||
        oldDelegate.barColor != barColor ||
        oldDelegate.labelColor != labelColor;
  }
}

class _CircleProgressPainter extends CustomPainter {
  const _CircleProgressPainter({required this.progress});

  final double progress;

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final radius = (size.width / 2) - 8;
    const strokeWidth = 8.0;

    final bgPaint = Paint()
      ..color = const Color(0xFFF2EAE4)
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth;
    canvas.drawCircle(center, radius, bgPaint);

    final progressPaint = Paint()
      ..color = const Color(0xFFC98C7B)
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth
      ..strokeCap = StrokeCap.round;

    final rect = Rect.fromCircle(center: center, radius: radius);
    canvas.drawArc(
      rect,
      -pi / 2,
      2 * pi * progress.clamp(0.0, 1.0),
      false,
      progressPaint,
    );
  }

  @override
  bool shouldRepaint(_CircleProgressPainter old) => old.progress != progress;
}
