import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../models/baby_model.dart';
import '../services/baby_profile_selection_storage.dart';
import '../services/baby_service.dart';
import '../../../core/network/api_client.dart';

/// CB-011 — Baby Profile Detail (UC-34, UC-35, UC-36, UC-37, UC-38, UC-192, UC-194–197)
/// Shows full baby profile: avatar, age, weight/height, 24h summary, tabs for
/// growth/milestones/vaccination, and trend chart. Calls GET /api/v1/babies/{babyId}.
class BabyProfileDetailScreen extends StatefulWidget {
  final String babyId;
  final bool embedded;
  final VoidCallback? onSwitchBaby;
  final VoidCallback? onAddBaby;
  final VoidCallback? onProfileChanged;

  const BabyProfileDetailScreen({
    super.key,
    required this.babyId,
    this.embedded = false,
    this.onSwitchBaby,
    this.onAddBaby,
    this.onProfileChanged,
  });

  @override
  State<BabyProfileDetailScreen> createState() =>
      _BabyProfileDetailScreenState();
}

enum _Tab { growth, milestones, vaccination }

class _BabyProfileDetailScreenState extends State<BabyProfileDetailScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surfaceContainer = Color(0xFFFFE9E3);
  static const _secondaryContainer = Color(0xFFF6DACF);
  static const _secondary = Color(0xFF6E5A52);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);

  final _service = BabyService();
  final _selectionStorage = BabyProfileSelectionStorage();
  BabyProfile? _profile;
  bool _loading = true;
  String? _error;
  _Tab _activeTab = _Tab.growth;

  double get _horizontalPadding => widget.embedded ? 0 : 24;

  @override
  void initState() {
    super.initState();
    _selectionStorage.saveLastOpenedBabyProfileId(widget.babyId);
    _loadProfile();
  }

  @override
  void didUpdateWidget(covariant BabyProfileDetailScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.babyId != widget.babyId) {
      _selectionStorage.saveLastOpenedBabyProfileId(widget.babyId);
      _activeTab = _Tab.growth;
      _loadProfile();
    }
  }

  Future<void> _loadProfile() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final p = await _service.getBabyProfile(widget.babyId);
      if (mounted) {
        setState(() {
          _profile = p;
          _loading = false;
        });
      }
    } on ApiException catch (e) {
      if (mounted) {
        setState(() {
          _error = e.statusCode == 403
              ? 'Bạn không có quyền xem hồ sơ này.'
              : 'Không thể tải hồ sơ. Vui lòng thử lại.';
          _loading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _error = 'Lỗi kết nối.';
          _loading = false;
        });
      }
    }
  }

  Future<void> _openEditProfile() async {
    await context.push('/babies/${widget.babyId}/edit');
    if (mounted) {
      await _loadProfile();
      widget.onProfileChanged?.call();
    }
  }

  void _openLogSummary() {
    context.push('/babies/${widget.babyId}/log-summary');
  }

  Future<void> _openAddMilestone() async {
    await context.push('/babies/${widget.babyId}/milestones/add');
    if (mounted) {
      await _loadProfile();
      widget.onProfileChanged?.call();
    }
  }

  @override
  Widget build(BuildContext context) {
    if (widget.embedded) {
      if (_loading) {
        return const Padding(
          padding: EdgeInsets.symmetric(vertical: 56),
          child: Center(
            child: CircularProgressIndicator(color: _primaryContainer),
          ),
        );
      }
      if (_error != null) return _buildErrorState();
      return _buildEmbeddedContent();
    }

    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: _loading
            ? const Center(
                child: CircularProgressIndicator(color: _primaryContainer),
              )
            : _error != null
            ? _buildErrorState()
            : _buildContent(),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _openLogSummary,
        backgroundColor: _primary,
        foregroundColor: Colors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        child: const Icon(Icons.add, size: 28),
      ),
    );
  }

  Widget _buildErrorState() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.error_outline, size: 48, color: Color(0xFFBA1A1A)),
          const SizedBox(height: 12),
          Text(
            _error!,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 16),
          TextButton(
            onPressed: _loadProfile,
            child: const Text(
              'Thử lại',
              style: TextStyle(fontFamily: 'Lexend', color: _primary),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildContent() {
    final p = _profile!;
    return CustomScrollView(
      slivers: [
        SliverToBoxAdapter(child: _buildAppBar(p)),
        SliverToBoxAdapter(child: _buildIdentityHeader(p)),
        SliverToBoxAdapter(child: _buildSummary24h()),
        SliverToBoxAdapter(child: _buildQuickActions()),
        SliverToBoxAdapter(child: _buildTabBar()),
        SliverToBoxAdapter(child: _buildTabContent()),
        const SliverToBoxAdapter(child: SizedBox(height: 100)),
      ],
    );
  }

  Widget _buildEmbeddedContent() {
    final p = _profile!;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _buildEmbeddedToolbar(p),
        const SizedBox(height: 14),
        _buildIdentityHeader(p),
        _buildSummary24h(),
        _buildQuickActions(),
        _buildTabBar(),
        _buildTabContent(),
        const SizedBox(height: 12),
      ],
    );
  }

  Widget _buildEmbeddedToolbar(BabyProfile p) {
    return Row(
      children: [
        Expanded(
          child: Text(
            p.nickname,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 20,
              fontWeight: FontWeight.w700,
              color: _primary,
            ),
          ),
        ),
        if (widget.onAddBaby != null)
          Tooltip(
            message: 'Thêm hồ sơ bé',
            child: IconButton(
              onPressed: widget.onAddBaby,
              icon: const Icon(Icons.add_rounded),
              color: _onSurfaceVariant,
            ),
          ),
        if (widget.onSwitchBaby != null)
          Tooltip(
            message: 'Đổi hồ sơ bé',
            child: IconButton.filled(
              onPressed: widget.onSwitchBaby,
              icon: const Icon(Icons.swap_horiz_rounded),
              style: IconButton.styleFrom(
                backgroundColor: _primary,
                foregroundColor: Colors.white,
              ),
            ),
          ),
      ],
    );
  }

  Widget _buildAppBar(BabyProfile p) {
    return SizedBox(
      height: 72,
      child: Row(
        children: [
          IconButton(
            onPressed: () => Navigator.pop(context),
            icon: const Icon(Icons.arrow_back),
            color: _onSurface,
          ),
          Expanded(
            child: Text(
              p.nickname,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 20,
                fontWeight: FontWeight.w600,
                color: _primary,
              ),
            ),
          ),
          IconButton(
            onPressed: () {
              _openEditProfile();
            },
            icon: const Icon(Icons.edit_outlined),
            color: _onSurfaceVariant,
          ),
        ],
      ),
    );
  }

  Widget _buildIdentityHeader(BabyProfile p) {
    return Padding(
      padding: EdgeInsets.symmetric(horizontal: _horizontalPadding),
      child: Column(
        children: [
          Container(
            width: 128,
            height: 128,
            decoration: BoxDecoration(
              color: _surfaceContainer,
              shape: BoxShape.circle,
              border: Border.all(color: _canvas, width: 4),
              boxShadow: [
                BoxShadow(
                  color: const Color(0xFF5A463F).withAlpha(15),
                  blurRadius: 20,
                  offset: const Offset(0, 4),
                ),
              ],
            ),
            child: const Icon(Icons.child_care, size: 56, color: _primary),
          ),
          const SizedBox(height: 16),
          Text(
            p.ageLabel,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _secondary,
            ),
          ),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              if (p.birthWeightKg != null) ...[
                _StatChip(
                  icon: Icons.monitor_weight,
                  label: '${p.birthWeightKg} kg',
                ),
                const SizedBox(width: 8),
              ],
              if (p.birthLengthCm != null)
                _StatChip(icon: Icons.height, label: '${p.birthLengthCm} cm'),
            ],
          ),
          const SizedBox(height: 24),
        ],
      ),
    );
  }

  Widget _buildSummary24h() {
    return Padding(
      padding: EdgeInsets.symmetric(horizontal: _horizontalPadding),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.schedule, size: 20, color: _primary),
              SizedBox(width: 8),
              Text(
                'Tổng kết 24h qua',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 20,
                  fontWeight: FontWeight.w600,
                  color: _onSurface,
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          // TODO: wire to GET /api/v1/babies/{id}/daily-summary (UC-194/195)
          Row(
            children: const [
              Expanded(
                child: _SummaryCard(
                  icon: Icons.water_drop_outlined,
                  value: '6',
                  label: 'Cữ bú',
                ),
              ),
              SizedBox(width: 12),
              Expanded(
                child: _SummaryCard(
                  icon: Icons.bed_outlined,
                  value: '13h',
                  label: 'Giấc ngủ',
                ),
              ),
              SizedBox(width: 12),
              Expanded(
                child: _SummaryCard(
                  icon: Icons.cleaning_services_outlined,
                  value: '4',
                  label: 'Thay tã',
                ),
              ),
            ],
          ),
          const SizedBox(height: 24),
        ],
      ),
    );
  }

  Widget _buildQuickActions() {
    return Padding(
      padding: EdgeInsets.fromLTRB(
        _horizontalPadding,
        0,
        _horizontalPadding,
        20,
      ),
      child: Row(
        children: [
          Expanded(
            child: OutlinedButton.icon(
              onPressed: _openLogSummary,
              icon: const Icon(Icons.list_alt_outlined, size: 18),
              label: const Text('Log'),
              style: OutlinedButton.styleFrom(
                foregroundColor: _primary,
                side: const BorderSide(color: _outlineVariant),
              ),
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: OutlinedButton.icon(
              onPressed: () {
                _openAddMilestone();
              },
              icon: const Icon(Icons.flag_outlined, size: 18),
              label: const Text('Milestone'),
              style: OutlinedButton.styleFrom(
                foregroundColor: _primary,
                side: const BorderSide(color: _outlineVariant),
              ),
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: OutlinedButton.icon(
              onPressed: () {
                _openEditProfile();
              },
              icon: const Icon(Icons.edit_outlined, size: 18),
              label: const Text('Edit'),
              style: OutlinedButton.styleFrom(
                foregroundColor: _primary,
                side: const BorderSide(color: _outlineVariant),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTabBar() {
    return Container(
      color: _canvas.withAlpha(230),
      padding: EdgeInsets.symmetric(
        horizontal: _horizontalPadding,
        vertical: 4,
      ),
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        child: Row(
          children: [
            _TabChip(
              label: 'Phát triển',
              selected: _activeTab == _Tab.growth,
              onTap: () => setState(() => _activeTab = _Tab.growth),
            ),
            const SizedBox(width: 8),
            _TabChip(
              label: 'Cột mốc',
              selected: _activeTab == _Tab.milestones,
              onTap: () => setState(() => _activeTab = _Tab.milestones),
            ),
            const SizedBox(width: 8),
            _TabChip(
              label: 'Tiêm chủng',
              selected: _activeTab == _Tab.vaccination,
              onTap: () => setState(() => _activeTab = _Tab.vaccination),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTabContent() {
    return Padding(
      padding: EdgeInsets.fromLTRB(
        _horizontalPadding,
        16,
        _horizontalPadding,
        0,
      ),
      child: switch (_activeTab) {
        _Tab.growth => _buildGrowthTab(),
        _Tab.milestones => _buildMilestoneTab(),
        _Tab.vaccination => _buildComingSoon('Lịch tiêm chủng'),
      },
    );
  }

  Widget _buildMilestoneTab() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF5A463F).withAlpha(15),
            blurRadius: 20,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Cot moc phat trien',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 18,
              fontWeight: FontWeight.w600,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 12),
          const Text(
            'Ghi nhan cot moc moi hoac mo chi tiet cot moc tu danh sach khi API danh sach san sang.',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _onSurfaceVariant,
              height: 1.4,
            ),
          ),
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              onPressed: () {
                _openAddMilestone();
              },
              icon: const Icon(Icons.add),
              label: const Text('Ghi nhan cot moc'),
              style: FilledButton.styleFrom(
                backgroundColor: _primaryContainer,
                foregroundColor: Colors.white,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildGrowthTab() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF5A463F).withAlpha(15),
            blurRadius: 20,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Xu hướng cân nặng',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 20,
                  fontWeight: FontWeight.w600,
                  color: _onSurface,
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: _secondaryContainer.withAlpha(77),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: const Text(
                  '1 tháng qua',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    fontWeight: FontWeight.w500,
                    color: _secondary,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          _buildTrendChart(),
          const SizedBox(height: 12),
          const Text(
            'Bé đang phát triển tốt theo chuẩn WHO.',
            textAlign: TextAlign.center,
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _secondary,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTrendChart() {
    return SizedBox(
      height: 160,
      child: CustomPaint(painter: _TrendChartPainter(), size: Size.infinite),
    );
  }

  Widget _buildComingSoon(String label) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(32),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF5A463F).withAlpha(15),
            blurRadius: 20,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        children: [
          const Icon(
            Icons.construction_outlined,
            size: 40,
            color: _primaryContainer,
          ),
          const SizedBox(height: 12),
          Text(
            '$label sẽ sớm ra mắt.',
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }
}

// ─── Sub-widgets ──────────────────────────────────────────────────────────────

class _StatChip extends StatelessWidget {
  final IconData icon;
  final String label;

  const _StatChip({required this.icon, required this.label});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: const Color(0xFFFFE9E3),
        borderRadius: BorderRadius.circular(99),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 16, color: const Color(0xFF524440)),
          const SizedBox(width: 4),
          Text(
            label,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 12,
              fontWeight: FontWeight.w500,
              color: Color(0xFF524440),
            ),
          ),
        ],
      ),
    );
  }
}

class _SummaryCard extends StatelessWidget {
  final IconData icon;
  final String value;
  final String label;

  const _SummaryCard({
    required this.icon,
    required this.value,
    required this.label,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 8),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF5A463F).withAlpha(15),
            blurRadius: 20,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: const Color(0xFFC98C7B).withAlpha(30),
              shape: BoxShape.circle,
            ),
            child: Icon(icon, size: 20, color: const Color(0xFF845143)),
          ),
          const SizedBox(height: 8),
          Text(
            value,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 20,
              fontWeight: FontWeight.w600,
              color: Color(0xFF845143),
            ),
          ),
          const SizedBox(height: 4),
          Text(
            label,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 12,
              fontWeight: FontWeight.w500,
              color: Color(0xFF6E5A52),
            ),
          ),
        ],
      ),
    );
  }
}

class _TabChip extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;

  const _TabChip({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
        decoration: BoxDecoration(
          color: selected ? const Color(0xFF845143) : Colors.white,
          borderRadius: BorderRadius.circular(99),
          border: selected ? null : Border.all(color: const Color(0xFFD6C2BD)),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 16,
            fontWeight: FontWeight.w600,
            color: selected ? Colors.white : const Color(0xFF524440),
          ),
        ),
      ),
    );
  }
}

// Simple line chart painter for weight trend
class _TrendChartPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final linePaint = Paint()
      ..color = const Color(0xFFC98C7B)
      ..strokeWidth = 3
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round;

    final fillPaint = Paint()
      ..shader = LinearGradient(
        begin: Alignment.topCenter,
        end: Alignment.bottomCenter,
        colors: [
          const Color(0xFFC98C7B).withAlpha(51),
          const Color(0xFFC98C7B).withAlpha(0),
        ],
      ).createShader(Rect.fromLTWH(0, 0, size.width, size.height))
      ..style = PaintingStyle.fill;

    // Sample data points (relative: 0.0–1.0)
    final points = [
      Offset(0, size.height * 0.9),
      Offset(size.width * 0.2, size.height * 0.81),
      Offset(size.width * 0.5, size.height * 0.58),
      Offset(size.width * 0.8, size.height * 0.40),
      Offset(size.width, size.height * 0.20),
    ];

    // Draw smooth curve
    final path = Path()..moveTo(points.first.dx, points.first.dy);
    for (int i = 0; i < points.length - 1; i++) {
      final cp1 = Offset((points[i].dx + points[i + 1].dx) / 2, points[i].dy);
      final cp2 = Offset(
        (points[i].dx + points[i + 1].dx) / 2,
        points[i + 1].dy,
      );
      path.cubicTo(
        cp1.dx,
        cp1.dy,
        cp2.dx,
        cp2.dy,
        points[i + 1].dx,
        points[i + 1].dy,
      );
    }

    // Fill under curve
    final fillPath = Path.from(path)
      ..lineTo(size.width, size.height)
      ..lineTo(0, size.height)
      ..close();
    canvas.drawPath(fillPath, fillPaint);

    // Draw line
    canvas.drawPath(path, linePaint);

    // Draw dots
    final dotPaint = Paint()
      ..color = const Color(0xFF845143)
      ..style = PaintingStyle.fill;
    for (final p in points) {
      canvas.drawCircle(p, p == points.last ? 5 : 3, dotPaint);
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
