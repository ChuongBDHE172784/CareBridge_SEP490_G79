import 'dart:math';
import 'package:flutter/material.dart';
import '../models/journey_model.dart';
import '../services/journey_service.dart';
import 'package:go_router/go_router.dart';
import '../../../core/network/api_client.dart';
import '../../healthRecords/models/health_metric_model.dart';
import '../../healthRecords/services/health_metric_service.dart';

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
  final _healthMetricService = HealthMetricService();
  JourneyDashboard? _dashboard;
  MetricTrend? _weightTrend;
  MetricTrend? _heartRateTrend;
  bool _loading = true;
  bool _weightTrendLoading = false;
  bool _weightTrendError = false;
  int _selectedPhase = 0; // 0=Mang thai, 1=Nuôi con

  static const _phases = ['Mang thai', 'Nuôi con'];

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _weightTrendLoading = true;
      _weightTrendError = false;
    });
    try {
      final d = await _journeyService.getDashboard();
      if (mounted) {
        setState(() { _dashboard = d; _loading = false; });
      }
      await _loadWeightTrend(d.journeyId);
    } on ApiException {
      if (mounted) {
        setState(() {
          _loading = false;
          _weightTrendLoading = false;
          _weightTrendError = true;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _loading = false;
          _weightTrendLoading = false;
          _weightTrendError = true;
        });
      }
    }
  }

  Future<void> _loadWeightTrend(String? journeyId) async {
    if (journeyId == null || journeyId.isEmpty) {
      if (mounted) {
        setState(() {
          _weightTrend = null;
          _heartRateTrend = null;
          _weightTrendLoading = false;
          _weightTrendError = false;
        });
      }
      return;
    }

    try {
      final now = DateTime.now();
      final trend = await _healthMetricService.getMetricTrend(
        journeyId: journeyId,
        metricType: 'WEIGHT',
        from: now.subtract(const Duration(days: 28)),
        to: now,
      );
      final hrTrend = await _healthMetricService.getMetricTrend(
        journeyId: journeyId,
        metricType: 'HEART_RATE',
        from: now.subtract(const Duration(days: 28)),
        to: now,
      );
      if (!mounted) return;
      setState(() {
        _weightTrend = trend;
        _heartRateTrend = hrTrend;
        _weightTrendLoading = false;
        _weightTrendError = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _weightTrend = null;
        _weightTrendLoading = false;
        _weightTrendError = true;
      });
    }
  }

  Future<void> _openMetricRoute(String metricType) async {
    final journeyId = _dashboard?.journeyId;
    if (journeyId == null || journeyId.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Chưa có hành trình để xem biểu đồ.')),
      );
      return;
    }

    final route = Uri(
      path: '/journeys/$journeyId/metrics/trend',
      queryParameters: {'metricType': metricType},
    ).toString();
    await context.push(route);
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
                  _buildBentoSummary(),
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
      padding: const EdgeInsets.fromLTRB(24, 16, 8, 0),
      child: Row(
        children: [
          const Expanded(
            child: Text(
              'Hành trình của Mẹ',
              style: TextStyle(fontFamily: 'Lexend', fontSize: 24, fontWeight: FontWeight.w600, color: _onSurface),
            ),
          ),
          IconButton(
            onPressed: () => context.push('/journey-setup'),
            icon: const Icon(Icons.edit_outlined, color: _primary),
            tooltip: 'Cập nhật hành trình',
          ),
        ],
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
    final week = d?.effectivePregnancyWeek;
    final progress = d?.pregnancyProgress ?? 0.0;

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
                Text(week != null ? 'Tuần $week' : 'Chưa có dữ liệu',
                    style: const TextStyle(fontFamily: 'Lexend', fontSize: 36, fontWeight: FontWeight.w700, color: _onSurface, letterSpacing: -0.5)),
                const SizedBox(height: 8),
                const Text('Bé đang phát triển\ntốt và khỏe mạnh.',
                    style: TextStyle(fontFamily: 'Lexend', fontSize: 14, color: _onSurfaceVariant, height: 1.5)),
              ],
            ),
          ),
          const SizedBox(width: 24),
          // Right: circular progress
          _CircularProgressWidget(progress: progress),
        ],
      ),
    );
  }

  Widget _buildDueDateCard() {
    final d = _dashboard;
    final dueDate = d?.estimatedDueDate;
    final days = d?.effectiveDaysUntilDue;
    final monthLabel = dueDate != null ? 'THG ${dueDate.month.toString().padLeft(2, '0')}' : '--';
    final dayLabel = dueDate != null ? '${dueDate.day}' : '--';

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
                  ],
                ),
                const SizedBox(height: 4),
                Text(days != null ? 'Còn lại $days ngày' : 'Chưa có ngày dự sinh',
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
      (Icons.monitor_heart, 'Chỉ số sức khỏe', '/health-metrics/weight'),
      (Icons.history_edu, 'Hồ sơ sức khỏe', '/health-records'),
      (Icons.psychology_alt_outlined, 'Kiểm tra triệu chứng', '/triage/intake'),
      (Icons.health_and_safety_outlined, 'Giám sát an toàn', '/safety'),
    ];
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: metrics.asMap().entries.map((e) {
          final i = e.key;
          final m = e.value;
          return Padding(
            padding: EdgeInsets.only(right: i < metrics.length - 1 ? 12 : 0),
            child: GestureDetector(
              onTap: () async {
                if (m.$3 == '/health-metrics/weight') {
                  await _openMetricRoute('WEIGHT');
                  return;
                }
                context.push(m.$3);
              },
              child: Container(
                width: 100,
                padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 8),
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
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(fontFamily: 'Lexend', fontSize: 11, fontWeight: FontWeight.w500, color: _onSurfaceVariant)),
                  ],
                ),
              ),
            ),
          );
        }).toList(),
      ),
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

  Widget _buildBentoSummary() {
    final weightPoints = _weightTrend?.dataPoints ?? [];
    final hrPoints = _heartRateTrend?.dataPoints ?? [];
    
    final latestWeight = weightPoints.isNotEmpty ? weightPoints.last.valueNumeric.toStringAsFixed(1) : '—';
    final latestHr = hrPoints.isNotEmpty ? hrPoints.last.valueNumeric.toStringAsFixed(0) : '—';

    return Row(
      children: [
        Expanded(child: _buildBentoCard(
          icon: Icons.monitor_weight_outlined,
          label: 'Cân nặng',
          value: latestWeight,
          unit: 'kg',
        )),
        const SizedBox(width: 12),
        Expanded(child: _buildBentoCard(
          icon: Icons.show_chart_rounded,
          label: 'Nhịp tim',
          value: latestHr,
          unit: 'bpm',
        )),
      ],
    );
  }

  Widget _buildBentoCard({
    required IconData icon,
    required String label,
    required String value,
    required String unit,
  }) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFF6F1EC),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: Colors.white, width: 2),
        boxShadow: [
          BoxShadow(color: _primary.withAlpha(12), blurRadius: 8, offset: const Offset(0, 2)),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: _primaryContainer, size: 22),
          const SizedBox(height: 8),
          Text(label, style: const TextStyle(fontFamily: 'Lexend', fontSize: 12, color: _onSurfaceVariant)),
          const SizedBox(height: 4),
          Text(
            '$value $unit',
            style: const TextStyle(fontFamily: 'Lexend', fontSize: 16, fontWeight: FontWeight.w700, color: _onSurface),
          ),
        ],
      ),
    );
  }

  String _formatMetricDate(DateTime value) => '${value.day}/${value.month}';
}

// ─── Circular progress widget ─────────────────────────────────────────────────
class _CircularProgressWidget extends StatelessWidget {
  final double progress;

  const _CircularProgressWidget({required this.progress});

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

