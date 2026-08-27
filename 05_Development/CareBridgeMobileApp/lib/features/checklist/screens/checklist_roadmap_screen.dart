import 'package:flutter/material.dart';
import '../../journey/services/journey_service.dart';
import '../../directChat/widgets/share_checklist_dialog.dart';
import '../models/checklist_roadmap_model.dart';
import '../services/checklist_roadmap_service.dart';

class ChecklistRoadmapScreen extends StatefulWidget {
  const ChecklistRoadmapScreen({
    super.key,
    this.service,
    this.journeyService,
  });

  final ChecklistRoadmapService? service;
  final JourneyService? journeyService;

  @override
  State<ChecklistRoadmapScreen> createState() => _ChecklistRoadmapScreenState();
}

class _ChecklistRoadmapScreenState extends State<ChecklistRoadmapScreen> {
  static const _background = Color(0xFFF6F1EC);
  static const _surface = Color(0xFFFFF8F6);
  static const _primary = Color(0xFF845143);
  static const _accent = Color(0xFFC98C7B);
  static const _muted = Color(0xFF5A463F);
  static const _subtle = Color(0xFF9C857C);

  late final ChecklistRoadmapService _service;
  late final JourneyService _journeyService;
  bool _loading = true;
  int _currentWeek = 24;
  List<ChecklistRoadmapMilestone> _milestones = [];
  String _filter = 'ALL'; // ALL, CURRENT, UPCOMING, COMPLETED

  @override
  void initState() {
    super.initState();
    _service = widget.service ?? ChecklistRoadmapService.instance;
    _journeyService = widget.journeyService ?? JourneyService();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _loading = true);
    try {
      final dashboard = await _journeyService.getDashboard();
      _currentWeek = dashboard.effectivePregnancyWeek ??
          dashboard.completedGestationalWeek ??
          24;
    } catch (_) {
      _currentWeek = 24;
    }

    try {
      final list = await _service.loadRoadmap(currentWeek: _currentWeek);
      if (mounted) {
        setState(() {
          _milestones = list;
          _loading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }

  List<ChecklistRoadmapMilestone> get _filteredMilestones {
    switch (_filter) {
      case 'CURRENT':
        return _milestones.where((m) => m.status == ChecklistMilestoneStatus.current).toList();
      case 'UPCOMING':
        return _milestones.where((m) => m.status == ChecklistMilestoneStatus.upcoming).toList();
      case 'COMPLETED':
        return _milestones.where((m) => m.status == ChecklistMilestoneStatus.completed).toList();
      default:
        return _milestones;
    }
  }

  void _openShareDialog() {
    ShareChecklistDialog.show(context);
  }

  @override
  Widget build(BuildContext context) {
    final upcomingCount = _milestones.where((m) => m.status == ChecklistMilestoneStatus.upcoming).length;
    final totalTasks = _milestones.fold<int>(0, (sum, m) => sum + m.totalTaskCount);
    final completedTasks = _milestones.fold<int>(0, (sum, m) => sum + m.completedTaskCount);

    return Scaffold(
      key: const Key('checklist-roadmap-screen'),
      backgroundColor: _background,
      appBar: AppBar(
        backgroundColor: _background,
        foregroundColor: _muted,
        elevation: 0,
        title: const Text(
          'Lộ trình checklist',
          style: TextStyle(
            fontFamily: 'Quicksand',
            fontWeight: FontWeight.w800,
            color: _muted,
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.share_outlined, color: _primary),
            tooltip: 'Chia sẻ với Bác sĩ',
            onPressed: _openShareDialog,
          ),
        ],
      ),
      body: SafeArea(
        child: _loading
            ? const Center(child: CircularProgressIndicator(color: _primary))
            : RefreshIndicator(
                onRefresh: _loadData,
                color: _primary,
                child: ListView(
                  padding: const EdgeInsets.fromLTRB(20, 12, 20, 32),
                  children: [
                    // Header Overview Card
                    _buildOverviewCard(totalTasks, completedTasks, upcomingCount),
                    const SizedBox(height: 16),

                    // Filter Chips
                    _buildFilterTabs(upcomingCount),
                    const SizedBox(height: 16),

                    // Milestones List
                    if (_filteredMilestones.isEmpty)
                      _buildEmptyFilterState()
                    else
                      ..._filteredMilestones.map((m) => _buildMilestoneCard(m)),
                  ],
                ),
              ),
      ),
      bottomNavigationBar: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Colors.white,
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.05),
              blurRadius: 10,
              offset: const Offset(0, -4),
            ),
          ],
        ),
        child: SafeArea(
          child: FilledButton.icon(
            onPressed: _openShareDialog,
            icon: const Icon(Icons.share_rounded, size: 18),
            label: const Text(
              'Chia sẻ Toàn bộ Lộ trình cho Bác sĩ',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontWeight: FontWeight.bold,
                fontSize: 14,
              ),
            ),
            style: FilledButton.styleFrom(
              backgroundColor: _primary,
              foregroundColor: Colors.white,
              minimumSize: const Size.fromHeight(48),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(14),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildOverviewCard(int totalTasks, int completedTasks, int upcomingCount) {
    final progressPercent = totalTasks > 0 ? ((completedTasks / totalTasks) * 100).round() : 0;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: _accent.withValues(alpha: 0.3)),
        boxShadow: [
          BoxShadow(
            color: _primary.withValues(alpha: 0.04),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: _accent.withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Icon(Icons.calendar_month_rounded, color: _primary, size: 22),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Thai kỳ: Tuần thứ $_currentWeek',
                      style: const TextStyle(
                        fontFamily: 'Quicksand',
                        fontWeight: FontWeight.w800,
                        fontSize: 17,
                        color: _muted,
                      ),
                    ),
                    Text(
                      'Hệ thống đã chuẩn bị $upcomingCount mốc checklist tương lai',
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        color: _subtle,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              Expanded(
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(6),
                  child: LinearProgressIndicator(
                    value: totalTasks > 0 ? completedTasks / totalTasks : 0.0,
                    backgroundColor: _background,
                    valueColor: const AlwaysStoppedAnimation<Color>(_accent),
                    minHeight: 8,
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Text(
                '$completedTasks/$totalTasks việc ($progressPercent%)',
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontWeight: FontWeight.bold,
                  fontSize: 12,
                  color: _muted,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildFilterTabs(int upcomingCount) {
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: [
          _buildFilterChip('ALL', 'Tất cả (${_milestones.length})', key: const Key('roadmap-filter-all')),
          const SizedBox(width: 8),
          _buildFilterChip('UPCOMING', 'Tương lai ($upcomingCount)', key: const Key('roadmap-filter-upcoming')),
          const SizedBox(width: 8),
          _buildFilterChip('CURRENT', 'Tuần hiện tại', key: const Key('roadmap-filter-current')),
          const SizedBox(width: 8),
          _buildFilterChip('COMPLETED', 'Đã qua', key: const Key('roadmap-filter-completed')),
        ],
      ),
    );
  }

  Widget _buildFilterChip(String filterKey, String label, {Key? key}) {
    final selected = _filter == filterKey;
    return ChoiceChip(
      key: key,
      label: Text(label),
      selected: selected,
      selectedColor: _primary,
      backgroundColor: _surface,
      labelStyle: TextStyle(
        fontFamily: 'Lexend',
        fontSize: 12,
        fontWeight: selected ? FontWeight.bold : FontWeight.w500,
        color: selected ? Colors.white : _muted,
      ),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(10),
        side: BorderSide(
          color: selected ? _primary : _accent.withValues(alpha: 0.25),
        ),
      ),
      onSelected: (_) => setState(() => _filter = filterKey),
    );
  }

  Widget _buildMilestoneCard(ChecklistRoadmapMilestone milestone) {
    Color badgeColor;
    Color badgeTextColor;
    String badgeText;
    IconData badgeIcon;

    switch (milestone.status) {
      case ChecklistMilestoneStatus.completed:
        badgeColor = const Color(0xFFE8F5E9);
        badgeTextColor = const Color(0xFF2E7D32);
        badgeText = 'Đã hoàn thành';
        badgeIcon = Icons.check_circle_rounded;
        break;
      case ChecklistMilestoneStatus.current:
        badgeColor = const Color(0xFFE3F2FD);
        badgeTextColor = const Color(0xFF1565C0);
        badgeText = 'Giai đoạn hiện tại';
        badgeIcon = Icons.timelapse_rounded;
        break;
      case ChecklistMilestoneStatus.upcoming:
        badgeColor = const Color(0xFFF3E5F5);
        badgeTextColor = const Color(0xFF7B1FA2);
        badgeText = 'Kế hoạch tương lai';
        badgeIcon = Icons.upcoming_rounded;
        break;
    }

    return Container(
      margin: const EdgeInsets.only(bottom: 14),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(18),
        border: Border.all(
          color: milestone.status == ChecklistMilestoneStatus.current
              ? _accent
              : const Color(0xFFEADFD9),
          width: milestone.status == ChecklistMilestoneStatus.current ? 1.5 : 1.0,
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.02),
            blurRadius: 6,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Material(
        color: _surface,
        borderRadius: BorderRadius.circular(18),
        clipBehavior: Clip.antiAlias,
        child: Theme(
          data: Theme.of(context).copyWith(dividerColor: Colors.transparent),
          child: ExpansionTile(
          initiallyExpanded: milestone.status == ChecklistMilestoneStatus.current ||
              milestone.status == ChecklistMilestoneStatus.upcoming,
          tilePadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          childrenPadding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
          leading: Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: badgeColor,
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(badgeIcon, color: badgeTextColor, size: 20),
          ),
          title: Text(
            milestone.title,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontWeight: FontWeight.w700,
              fontSize: 14,
              color: _muted,
            ),
          ),
          subtitle: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const SizedBox(height: 4),
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                    decoration: BoxDecoration(
                      color: badgeColor,
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Text(
                      milestone.weekRangeLabel,
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontWeight: FontWeight.bold,
                        fontSize: 11,
                        color: badgeTextColor,
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Text(
                    badgeText,
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 11,
                      fontWeight: FontWeight.w600,
                      color: badgeTextColor,
                    ),
                  ),
                ],
              ),
              if (milestone.description != null) ...[
                const SizedBox(height: 6),
                Text(
                  milestone.description!,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    color: _subtle,
                  ),
                ),
              ],
            ],
          ),
          children: [
            const Divider(height: 20, color: Color(0xFFECE4E1)),
            ...milestone.tasks.map((task) {
              return Padding(
                padding: const EdgeInsets.symmetric(vertical: 4),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Icon(
                      task.completed
                          ? Icons.check_circle_rounded
                          : Icons.radio_button_unchecked_rounded,
                      size: 18,
                      color: task.completed
                          ? const Color(0xFF2E7D32)
                          : _accent,
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            task.title,
                            style: TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 13,
                              fontWeight: FontWeight.w500,
                              color: task.completed
                                  ? const Color(0xFF6E605D)
                                  : _muted,
                            ),
                          ),
                          if (task.category.isNotEmpty && task.category != 'Chung')
                            Text(
                              task.category,
                              style: const TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 10,
                                color: _subtle,
                              ),
                            ),
                        ],
                      ),
                    ),
                  ],
                ),
              );
            }),
          ],
        ),
      ),
    ),
  );
}

  Widget _buildEmptyFilterState() {
    return Container(
      padding: const EdgeInsets.all(32),
      alignment: Alignment.center,
      child: Column(
        children: const [
          Icon(Icons.checklist_rounded, size: 48, color: _subtle),
          SizedBox(height: 12),
          Text(
            'Không có mốc checklist nào trong bộ lọc này.',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 13,
              color: _subtle,
            ),
          ),
        ],
      ),
    );
  }
}
