import 'dart:math';
import 'package:flutter/material.dart';
import '../models/journey_model.dart';
import '../services/journey_service.dart';
import 'package:go_router/go_router.dart';
import '../../../core/network/api_client.dart';

/// CB-009 — Mother Journey (UC-23, UC-25, UC-26, UC-27, UC-28, UC-51, UC-52, UC-53)
/// Phase-based journey view: week hero card, circular progress, due date card,
/// next appointment, metric quick-add buttons, and weight bar chart.
/// Data: GET /api/v1/journeys/me/dashboard (UC-24), mock appointment + metrics.
class MotherJourneyScreen extends StatefulWidget {
  const MotherJourneyScreen({super.key});

  @override
  State<MotherJourneyScreen> createState() => _MotherJourneyScreenState();
}

class _MotherJourneyScreenState extends State<MotherJourneyScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surfaceContainerHigh = Color(0xFFFFE2D9);
  static const _surfaceContainerLow = Color(0xFFFFF1EC);
  static const _surfaceContainerLowest = Color(0xFFFFF8F6);
  static const _surfaceContainerHighest = Color(0xFFFADCD3);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);
  static const _surface = Color(0xFFFFF8F6);

  final _journeyService = JourneyService();
  JourneyDashboard? _dashboard;
  bool _loading = true;
  int _selectedPhase = 0; // 0=Mang thai, 1=Sau sinh, 2=Nuôi con

  static const _phases = ['Mang thai', 'Sau sinh', 'Nuôi con'];

  // Mock weight data: 6 weekly measurements (kg)
  static const _weightData = [64.0, 64.8, 65.2, 65.8, 66.1, 66.5];
  static const _weightLabels = ['T1', 'T2', 'T3', 'T4', 'T5', 'T6'];

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final d = await _journeyService.getDashboard();
      if (mounted) setState(() { _dashboard = d; _loading = false; });
    } on ApiException {
      if (mounted) setState(() {
        _dashboard = JourneyDashboard(
          journeyId: 'mock-journey-1',
          journeyType: 'PREGNANCY',
          status: 'ACTIVE_PREGNANCY',
          pregnancyWeek: 24,
          trimester: 2,
          daysUntilDue: 112,
          estimatedDueDate: DateTime(2024, 10, 15),
        );
        _loading = false;
      });
    } catch (_) {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
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
                _buildPhaseTabs(),
                const SizedBox(height: 24),
                if (_loading)
                  const Center(child: CircularProgressIndicator(color: _primaryContainer))
                else ...[
                  _buildHeroCard(),
                  const SizedBox(height: 16),
                  _buildDueDateCard(),
                  const SizedBox(height: 16),
                  _buildNextAppointmentCard(),
                  const SizedBox(height: 16),
                  _buildVaccinationCard(),
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
      padding: const EdgeInsets.fromLTRB(24, 16, 24, 0),
      child: const Text(
        'Hành trình của Mẹ',
        style: TextStyle(fontFamily: 'Lexend', fontSize: 24, fontWeight: FontWeight.w600, color: _onSurface),
      ),
    );
  }

  Widget _buildPhaseTabs() {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: List.generate(_phases.length, (i) {
          final active = i == _selectedPhase;
          return Padding(
            padding: EdgeInsets.only(right: i < _phases.length - 1 ? 12 : 0),
            child: GestureDetector(
              onTap: () => setState(() => _selectedPhase = i),
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 200),
                padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
                decoration: BoxDecoration(
                  color: active ? _primaryContainer : _surfaceContainerLowest,
                  borderRadius: BorderRadius.circular(99),
                  border: Border.all(color: active ? _primaryContainer : _outlineVariant),
                ),
                child: Text(
                  _phases[i],
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    fontWeight: FontWeight.w500,
                    color: active ? Colors.white : _onSurfaceVariant,
                  ),
                ),
              ),
            ),
          );
        }),
      ),
    );
  }

  Widget _buildHeroCard() {
    final d = _dashboard;
    final week = d?.pregnancyWeek ?? 24;
    final progress = d?.pregnancyProgress ?? 0.60;

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
          BoxShadow(color: const Color(0xFF845143).withAlpha(20), blurRadius: 24, offset: const Offset(0, 8)),
        ],
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          // Left: week info
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(color: Colors.white.withAlpha(178), borderRadius: BorderRadius.circular(99)),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: const [
                      Icon(Icons.favorite, size: 14, color: _primary),
                      SizedBox(width: 4),
                      Text('Thai kỳ bình thường', style: TextStyle(fontFamily: 'Lexend', fontSize: 11, color: _primary, fontWeight: FontWeight.w500)),
                    ],
                  ),
                ),
                const SizedBox(height: 12),
                Text('Tuần $week',
                    style: const TextStyle(fontFamily: 'Lexend', fontSize: 36, fontWeight: FontWeight.w700, color: _onSurface, letterSpacing: -0.5)),
                const SizedBox(height: 8),
                const Text('Bé đang phát triển\ntốt và khỏe mạnh.',
                    style: TextStyle(fontFamily: 'Lexend', fontSize: 14, color: _onSurfaceVariant, height: 1.5)),
              ],
            ),
          ),
          const SizedBox(width: 24),
          // Right: circular progress
          _CircularProgressWidget(progress: progress, week: week),
        ],
      ),
    );
  }

  Widget _buildDueDateCard() {
    final d = _dashboard;
    final dueDate = d?.estimatedDueDate;
    final days = d?.daysUntilDue ?? 112;
    final monthLabel = dueDate != null ? 'THG ${dueDate.month.toString().padLeft(2, '0')}' : 'THG 10';
    final dayLabel = dueDate != null ? '${dueDate.day}' : '15';

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surfaceContainerLowest,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [BoxShadow(color: const Color(0xFF5A463F).withAlpha(15), blurRadius: 20, offset: const Offset(0, 4))],
      ),
      child: Row(
        children: [
          // Calendar block
          Container(
            width: 64, height: 64,
            decoration: BoxDecoration(color: _surfaceContainerHigh, borderRadius: BorderRadius.circular(16)),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(monthLabel, style: const TextStyle(fontFamily: 'Lexend', fontSize: 11, fontWeight: FontWeight.w500, color: _primary)),
                Text(dayLabel, style: const TextStyle(fontFamily: 'Lexend', fontSize: 22, fontWeight: FontWeight.w700, color: _onSurface)),
              ],
            ),
          ),
          const SizedBox(width: 16),
          // Due date info
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    const Expanded(
                      child: Text('Ngày dự sinh',
                          style: TextStyle(fontFamily: 'Lexend', fontSize: 16, fontWeight: FontWeight.w600, color: _onSurface)),
                    ),
                    GestureDetector(
                      onTap: () {}, // TODO: update due date (UC-23)
                      child: const Icon(Icons.edit_outlined, size: 18, color: _onSurfaceVariant),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                Text('Còn lại $days ngày',
                    style: const TextStyle(fontFamily: 'Lexend', fontSize: 14, color: _onSurfaceVariant)),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildNextAppointmentCard() {
    return Container(
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
                  children: const [
                    Text('Lịch hẹn tiếp theo',
                        style: TextStyle(fontFamily: 'Lexend', fontSize: 16, fontWeight: FontWeight.w600, color: _onSurface)),
                    SizedBox(height: 2),
                    Text('Khám thai định kỳ lần 6',
                        style: TextStyle(fontFamily: 'Lexend', fontSize: 14, color: _onSurfaceVariant)),
                  ],
                ),
              ),
              Container(
                width: 40, height: 40,
                decoration: const BoxDecoration(color: _surface, shape: BoxShape.circle),
                child: const Icon(Icons.calendar_today, color: _primary, size: 20),
              ),
            ],
          ),
          const SizedBox(height: 12),
          const Divider(color: Color(0xFFD6C2BD), thickness: 1),
          const SizedBox(height: 12),
          Row(
            children: const [
              Icon(Icons.schedule, size: 16, color: _onSurfaceVariant),
              SizedBox(width: 8),
              Text('09:00 — 20/07/2024',
                  style: TextStyle(fontFamily: 'Lexend', fontSize: 14, color: _onSurfaceVariant)),
            ],
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
    ];
    return Row(
      children: metrics.asMap().entries.map((e) {
        final i = e.key;
        final m = e.value;
        return Expanded(
          child: Padding(
            padding: EdgeInsets.only(right: i < metrics.length - 1 ? 12 : 0),
            child: GestureDetector(
              onTap: () => context.push(m.$3),
              child: Container(
                padding: const EdgeInsets.symmetric(vertical: 16),
                decoration: BoxDecoration(
                  color: _surfaceContainerLowest,
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: _outlineVariant),
                ),
                child: Column(
                  children: [
                    Container(
                      width: 40, height: 40,
                      decoration: const BoxDecoration(color: _surfaceContainerHigh, shape: BoxShape.circle),
                      child: Icon(m.$1, color: _primary, size: 20),
                    ),
                    const SizedBox(height: 8),
                    Text(m.$2,
                        textAlign: TextAlign.center,
                        style: const TextStyle(fontFamily: 'Lexend', fontSize: 11, fontWeight: FontWeight.w500, color: _onSurfaceVariant)),
                  ],
                ),
              ),
            ),
          ),
        );
      }).toList(),
    );
  }

  Widget _buildVaccinationCard() {
    return GestureDetector(
      onTap: () => context.push('/vaccination/vax-01'),
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
              width: 40, height: 40,
              decoration: const BoxDecoration(color: _surface, shape: BoxShape.circle),
              child: const Icon(Icons.vaccines, color: _primary, size: 20),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: const [
                  Text('Thông tin tiêm phòng',
                      style: TextStyle(fontFamily: 'Lexend', fontSize: 16, fontWeight: FontWeight.w600, color: _onSurface)),
                  SizedBox(height: 2),
                  Text('Uốn ván mũi 2 (Đã lên lịch)',
                      style: TextStyle(fontFamily: 'Lexend', fontSize: 14, color: _onSurfaceVariant)),
                ],
              ),
            ),
            const Icon(Icons.chevron_right, color: _onSurfaceVariant),
          ],
        ),
      ),
    );
  }

  Widget _buildWeightChart() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surfaceContainerLowest,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [BoxShadow(color: const Color(0xFF5A463F).withAlpha(15), blurRadius: 20, offset: const Offset(0, 4))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text('Biểu đồ cân nặng',
                  style: TextStyle(fontFamily: 'Lexend', fontSize: 16, fontWeight: FontWeight.w600, color: _onSurface)),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(color: _surfaceContainerHigh, borderRadius: BorderRadius.circular(99)),
                child: Row(
                  children: const [
                    Text('4 tuần', style: TextStyle(fontFamily: 'Lexend', fontSize: 12, color: _onSurfaceVariant)),
                    SizedBox(width: 4),
                    Icon(Icons.expand_more, size: 16, color: _onSurfaceVariant),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Container(
            height: 160,
            width: double.infinity,
            decoration: BoxDecoration(color: _surfaceContainerHigh, borderRadius: BorderRadius.circular(12)),
            padding: const EdgeInsets.fromLTRB(12, 16, 12, 8),
            child: CustomPaint(
              painter: _BarChartPainter(
                data: _weightData,
                labels: _weightLabels,
                primaryColor: _primary,
                barColor: _primaryContainer,
                labelColor: _onSurfaceVariant,
              ),
            ),
          ),
          const SizedBox(height: 8),
          const Text('Đơn vị: kg',
              style: TextStyle(fontFamily: 'Lexend', fontSize: 11, color: _onSurfaceVariant)),
        ],
      ),
    );
  }
}

// ─── Circular progress widget ─────────────────────────────────────────────────
class _CircularProgressWidget extends StatelessWidget {
  final double progress;
  final int week;

  const _CircularProgressWidget({required this.progress, required this.week});

  @override
  Widget build(BuildContext context) {
    final pct = (progress * 100).round();
    return SizedBox(
      width: 110, height: 110,
      child: CustomPaint(
        painter: _CircleProgressPainter(progress: progress),
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text('$pct%',
                  style: const TextStyle(fontFamily: 'Lexend', fontSize: 20, fontWeight: FontWeight.w700, color: Color(0xFF271812))),
              const Text('Thai kỳ',
                  style: TextStyle(fontFamily: 'Lexend', fontSize: 11, color: Color(0xFF524440))),
            ],
          ),
        ),
      ),
    );
  }
}

class _CircleProgressPainter extends CustomPainter {
  final double progress;

  const _CircleProgressPainter({required this.progress});

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
    canvas.drawArc(rect, -pi / 2, 2 * pi * progress.clamp(0.0, 1.0), false, progressPaint);
  }

  @override
  bool shouldRepaint(_CircleProgressPainter old) => old.progress != progress;
}

// ─── Bar chart painter ────────────────────────────────────────────────────────
class _BarChartPainter extends CustomPainter {
  final List<double> data;
  final List<String> labels;
  final Color primaryColor;
  final Color barColor;
  final Color labelColor;

  const _BarChartPainter({
    required this.data,
    required this.labels,
    required this.primaryColor,
    required this.barColor,
    required this.labelColor,
  });

  @override
  void paint(Canvas canvas, Size size) {
    if (data.isEmpty) return;
    const labelHeight = 20.0;
    const barGap = 8.0;
    final chartHeight = size.height - labelHeight;
    final barWidth = (size.width - barGap * (data.length - 1)) / data.length;

    final minVal = data.reduce(min);
    final maxVal = data.reduce(max);
    final range = (maxVal - minVal).clamp(0.5, double.infinity);

    final labelPainter = TextPainter(textDirection: TextDirection.ltr);

    for (var i = 0; i < data.length; i++) {
      final fraction = (data[i] - minVal) / range;
      final barH = (chartHeight * 0.2) + (chartHeight * 0.7 * fraction);
      final x = i * (barWidth + barGap);
      final top = chartHeight - barH;

      final paint = Paint()
        ..color = (i == data.length - 1) ? primaryColor : barColor
        ..style = PaintingStyle.fill;

      final rrect = RRect.fromRectAndCorners(
        Rect.fromLTWH(x, top, barWidth, barH),
        topLeft: const Radius.circular(4),
        topRight: const Radius.circular(4),
      );
      canvas.drawRRect(rrect, paint);

      // Label below
      labelPainter
        ..text = TextSpan(
          text: labels[i],
          style: TextStyle(fontFamily: 'Lexend', fontSize: 10, color: labelColor),
        )
        ..layout(maxWidth: barWidth + barGap);
      labelPainter.paint(
        canvas,
        Offset(x + (barWidth - labelPainter.width) / 2, chartHeight + 4),
      );
    }
  }

  @override
  bool shouldRepaint(_BarChartPainter old) => old.data != data;
}
