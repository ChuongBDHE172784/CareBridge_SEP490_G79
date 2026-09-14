import 'package:flutter/material.dart';
import '../../checklist/services/user_checklist_service.dart';
import '../../checklist/services/checklist_roadmap_service.dart';
import '../../reminder/models/today_task_model.dart';
import '../../reminder/services/today_task_service.dart';
import '../../journey/services/journey_service.dart';
import 'checklist_message_card.dart';

class ShareChecklistDialog extends StatefulWidget {
  const ShareChecklistDialog({super.key});

  static Future<ChecklistShareData?> show(BuildContext context) {
    return showModalBottomSheet<ChecklistShareData>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (ctx) => const ShareChecklistDialog(),
    );
  }

  @override
  State<ShareChecklistDialog> createState() => _ShareChecklistDialogState();
}

class _ChecklistShareItem {
  final String id;
  final String text;
  final bool completed;
  final String category;
  final String timeLabel;
  final String section; // 'HISTORY', 'CURRENT', 'FUTURE'
  final String origin; // 'SYSTEM' | 'USER' | 'EXPERT'
  final String createdBy;

  _ChecklistShareItem({
    required this.id,
    required this.text,
    required this.completed,
    required this.category,
    required this.timeLabel,
    required this.section,
    this.origin = 'SYSTEM',
    this.createdBy = 'SYSTEM',
  });

  bool get isExpertCustom => origin == 'EXPERT' || createdBy == 'EXPERT';
  String? get doctorNote => null;

  bool get isPersonal =>
      origin == 'USER' ||
      origin == 'USER_CREATED' ||
      createdBy == 'USER' ||
      createdBy == 'USER_CREATED';
  bool get isCareBridgeSuggestion => !isPersonal;
}

class _ShareChecklistDialogState extends State<ShareChecklistDialog>
    with SingleTickerProviderStateMixin {
  static const _primary = Color(0xFF845143);
  static const _accent = Color(0xFFC98C7B);
  static const _textDark = Color(0xFF2C2523);
  static const _textMuted = Color(0xFF7A6F6C);

  final TextEditingController _noteController = TextEditingController();
  late TabController _tabController;
  bool _loading = true;
  String _stage = 'PRE_PREGNANCY';
  String _stageLabel = 'Chuẩn bị mang thai';
  int? _gestationalWeek;
  String? _journeyId;
  String _statusFilter = 'ALL'; // ALL, COMPLETED, PENDING

  List<_ChecklistShareItem> _historyItems = [];
  List<_ChecklistShareItem> _currentItems = [];
  List<_ChecklistShareItem> _futureItems = [];

  @override
  void initState() {
    super.initState();
    // Mặc định hiển thị tab "Hiện tại" (initialIndex: 1)
    _tabController = TabController(length: 3, vsync: this, initialIndex: 1);
    _loadAllChecklistData();
  }

  @override
  void dispose() {
    _noteController.dispose();
    _tabController.dispose();
    super.dispose();
  }

  Future<void> _loadAllChecklistData() async {
    int currentWk = 1;
    try {
      final dashboard = await JourneyService().getDashboard();
      _journeyId = dashboard.journeyId;
      final rawStage = (dashboard.journeyType ?? '').toUpperCase();
      if (dashboard.isPrePregnancy || rawStage == 'PRE_PREGNANCY') {
        _stage = 'PRE_PREGNANCY';
        _stageLabel = 'Chuẩn bị mang thai';
        _gestationalWeek = null;
        currentWk = 1;
      } else if (dashboard.isPostpartum || rawStage == 'POSTPARTUM') {
        _stage = 'POSTPARTUM';
        _stageLabel = 'Sau sinh';
        _gestationalWeek = null;
        currentWk = 1;
      } else {
        _stage = 'PREGNANCY';
        _gestationalWeek = dashboard.effectivePregnancyWeek ??
            dashboard.completedGestationalWeek ??
            12;
        _stageLabel = 'Tuần thai thứ $_gestationalWeek';
        currentWk = _gestationalWeek ?? 12;
      }
    } catch (_) {
      _stage = 'PRE_PREGNANCY';
      _stageLabel = 'Chuẩn bị mang thai';
      _gestationalWeek = null;
      currentWk = 1;
    }

    // 1. Load categorized roadmap tasks for history and future (Gợi ý CareBridge / System templates)
    try {
      final categorized = await ChecklistRoadmapService.instance
          .loadCategorizedTasks(currentWeek: currentWk, stage: _stage);

      final hist = categorized['history'] ?? [];
      final curr = categorized['current'] ?? [];
      final fut = categorized['future'] ?? [];

      _historyItems = hist
          .map((t) => _ChecklistShareItem(
                id: t.id,
                text: t.title,
                completed: true,
                category: t.category,
                timeLabel: _stage == 'PRE_PREGNANCY'
                    ? 'Đã chuẩn bị'
                    : (_stage == 'POSTPARTUM'
                        ? 'Đã thực hiện'
                        : 'Tuần ${t.dueWeek ?? (currentWk - 4)}'),
                section: 'HISTORY',
                origin: 'SYSTEM',
                createdBy: 'SYSTEM',
              ))
          .toList();

      _currentItems = curr
          .map((t) => _ChecklistShareItem(
                id: t.id,
                text: t.title,
                completed: t.completed,
                category: t.category,
                timeLabel: _stage == 'PRE_PREGNANCY'
                    ? 'Chuẩn bị mang thai'
                    : (_stage == 'POSTPARTUM'
                        ? 'Sau sinh'
                        : 'Tuần $currentWk (Hiện tại)'),
                section: 'CURRENT',
                origin: 'SYSTEM',
                createdBy: 'SYSTEM',
              ))
          .toList();

      _futureItems = fut
          .map((t) => _ChecklistShareItem(
                id: t.id,
                text: t.title,
                completed: false,
                category: t.category,
                timeLabel: _stage == 'PRE_PREGNANCY'
                    ? 'Kế hoạch tiếp theo'
                    : (_stage == 'POSTPARTUM'
                        ? 'Kế hoạch tiếp theo'
                        : 'Tuần ${t.dueWeek ?? (currentWk + 4)} (Tương lai)'),
                section: 'FUTURE',
                origin: 'SYSTEM',
                createdBy: 'SYSTEM',
              ))
          .toList();
    } catch (_) {}

    // Tập hợp tất cả tiêu đề thuộc lộ trình chuẩn CareBridge
    final roadmapTitleSet = {
      for (final c in _currentItems) c.text.trim().toLowerCase(),
      for (final h in _historyItems) h.text.trim().toLowerCase(),
      for (final f in _futureItems) f.text.trim().toLowerCase(),
    };

    // 2. Load live today tasks from TodayTaskService (chỉ đồng bộ trạng thái cho lộ trình, loại bỏ việc cá nhân)
    try {
      final snapshot = await TodayTaskService.instance.loadToday();
      final liveTasks = snapshot.sections.all.toList();
      if (liveTasks.isNotEmpty) {
        final existingMap = {
          for (final c in _currentItems) c.text.trim().toLowerCase(): c
        };

        final updatedCurrent = <_ChecklistShareItem>[];
        for (final t in liveTasks) {
          final key = t.title.trim().toLowerCase();
          final existing = existingMap[key];
          final isRoadmapItem = roadmapTitleSet.contains(key);
          final isPersonalTask = !isRoadmapItem ||
              t.origin == TodayTaskOrigin.userCreated ||
              t.isCareTask;

          // Loại bỏ tuyệt đối việc cá nhân khi chia sẻ cho chuyên gia
          if (isPersonalTask) continue;

          updatedCurrent.add(_ChecklistShareItem(
            id: t.id,
            text: t.title,
            completed: t.isCompleted,
            category: existing?.category ?? 'Khám thai & Y tế',
            timeLabel: existing?.timeLabel ?? 'Tuần $currentWk (Hiện tại)',
            section: 'CURRENT',
            origin: 'SYSTEM',
            createdBy: 'SYSTEM',
          ));
        }

        // Add roadmap current items not in today tasks
        for (final c in _currentItems) {
          if (!updatedCurrent.any((u) => u.text.trim().toLowerCase() == c.text.trim().toLowerCase())) {
            updatedCurrent.add(c);
          }
        }

        _currentItems = updatedCurrent;
      }
    } catch (_) {}

    // 3. Synchronize with UserChecklistService: chỉ cập nhật trạng thái completed cho việc thuộc lộ trình
    try {
      final serverItems = await UserChecklistService.instance.listItems();
      if (serverItems.isNotEmpty) {
        for (final si in serverItems) {
          final key = si.itemText.trim().toLowerCase();
          final idx = _currentItems.indexWhere((c) => c.text.trim().toLowerCase() == key);
          if (idx >= 0 && roadmapTitleSet.contains(key)) {
            final cur = _currentItems[idx];
            _currentItems[idx] = _ChecklistShareItem(
              id: si.itemId,
              text: cur.text,
              completed: si.completed,
              category: cur.category,
              timeLabel: cur.timeLabel,
              section: cur.section,
              origin: cur.origin,
              createdBy: cur.createdBy,
            );
          }
        }
      }
    } catch (_) {}

    // 4. Lọc bỏ phòng vệ: Không để sót việc cá nhân nào
    _historyItems = _historyItems.where((i) => !i.isPersonal).toList();
    _currentItems = _currentItems.where((i) => !i.isPersonal).toList();
    _futureItems = _futureItems.where((i) => !i.isPersonal).toList();

    // Lọc bỏ tuyệt đối: Các mục thuộc "Lịch sử đã qua" không được hiển thị ở "Tuần hiện tại"
    final historyTextSet = _historyItems.map((h) => h.text.trim().toLowerCase()).toSet();
    _currentItems = _currentItems.where((c) => !historyTextSet.contains(c.text.trim().toLowerCase())).toList();

    // Loại bỏ các mục trùng lặp trong _currentItems
    final seen = <String>{};
    _currentItems = _currentItems.where((c) => seen.add(c.text.trim().toLowerCase())).toList();

    // Đảm bảo tương lai không trùng với hiện tại
    final currentTextSet = _currentItems.map((c) => c.text.trim().toLowerCase()).toSet();
    _futureItems = _futureItems.where((f) => !currentTextSet.contains(f.text.trim().toLowerCase())).toList();

    if (mounted) {
      setState(() => _loading = false);
    }
  }

  void _onConfirm() {
    final totalCount =
        _historyItems.length + _currentItems.length + _futureItems.length;

    if (totalCount == 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Không có việc cần làm nào để chia sẻ')),
      );
      return;
    }

    final completedCount = _historyItems.where((i) => i.completed).length +
        _currentItems.where((i) => i.completed).length +
        _futureItems.where((i) => i.completed).length;
    final percent =
        totalCount > 0 ? ((completedCount / totalCount) * 100).round() : 0;

    final shareData = ChecklistShareData(
      title: _stage == 'PRE_PREGNANCY'
          ? 'Lộ trình chuẩn bị mang thai'
          : (_stage == 'POSTPARTUM'
              ? 'Lộ trình chăm sóc sau sinh'
              : 'Danh sách việc cần làm (Checklist)'),
      gestationalWeek: _gestationalWeek,
      stage: _stage,
      stageLabel: _stageLabel,
      journeyId: _journeyId,
      isLiveSync: true,
      completedCount: completedCount,
      totalCount: totalCount,
      progressPercent: percent,
      note: _noteController.text.trim().isEmpty
          ? null
          : _noteController.text.trim(),
      historyItems: _historyItems
          .map((i) => ChecklistItemShareData(
                text: i.text,
                completed: i.completed,
                category: i.category,
                timeLabel: i.timeLabel,
                origin: i.origin,
                createdBy: i.createdBy,
                isExpertCustom: i.isExpertCustom,
                doctorNote: i.doctorNote,
              ))
          .toList(),
      currentItems: _currentItems
          .map((i) => ChecklistItemShareData(
                text: i.text,
                completed: i.completed,
                category: i.category,
                timeLabel: i.timeLabel,
                origin: i.origin,
                createdBy: i.createdBy,
                isExpertCustom: i.isExpertCustom,
                doctorNote: i.doctorNote,
              ))
          .toList(),
      futureItems: _futureItems
          .map((i) => ChecklistItemShareData(
                text: i.text,
                completed: i.completed,
                category: i.category,
                timeLabel: i.timeLabel,
                origin: i.origin,
                createdBy: i.createdBy,
                isExpertCustom: i.isExpertCustom,
                doctorNote: i.doctorNote,
              ))
          .toList(),
    );

    Navigator.of(context).pop(shareData);
  }

  List<_ChecklistShareItem> _filterItems(List<_ChecklistShareItem> items) {
    return items.where((i) {
      // Filter by completion status
      if (_statusFilter == 'COMPLETED' && !i.completed) return false;
      if (_statusFilter == 'PENDING' && i.completed) return false;

      return true;
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    final totalHistory = _historyItems.length;
    final totalCurrent = _currentItems.length;
    final totalFuture = _futureItems.length;

    final allItemsList = [..._historyItems, ..._currentItems, ..._futureItems];
    final totalAllItems = allItemsList.length;
    final totalCompleted = allItemsList.where((i) => i.completed).length;
    final totalPending = totalAllItems - totalCompleted;

    return Padding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).viewInsets.bottom,
        left: 20,
        right: 20,
        top: 16,
      ),
      child: SafeArea(
        child: SizedBox(
          height: MediaQuery.of(context).size.height * 0.85,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Handle bar
              Center(
                child: Container(
                  width: 40,
                  height: 4,
                  decoration: BoxDecoration(
                    color: Colors.grey.shade300,
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              const SizedBox(height: 12),

              // Title Header
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: _accent.withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: const Icon(
                      Icons.checklist_rtl_rounded,
                      color: _primary,
                      size: 22,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            const Text(
                              'Chia sẻ việc cần làm',
                              style: TextStyle(
                                fontFamily: 'Quicksand',
                                fontSize: 17,
                                fontWeight: FontWeight.w800,
                                color: _textDark,
                              ),
                            ),
                            const SizedBox(width: 6),
                            Container(
                              padding: const EdgeInsets.symmetric(
                                  horizontal: 6, vertical: 1.5),
                              decoration: BoxDecoration(
                                color: const Color(0xFFE8F5E9),
                                borderRadius: BorderRadius.circular(6),
                              ),
                              child: const Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Icon(Icons.bolt_rounded,
                                      size: 11, color: Color(0xFF2E7D32)),
                                  SizedBox(width: 2),
                                  Text(
                                    'Live Sync',
                                    style: TextStyle(
                                      fontFamily: 'Lexend',
                                      fontSize: 10,
                                      fontWeight: FontWeight.bold,
                                      color: Color(0xFF2E7D32),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                        Text(
                          _gestationalWeek != null
                              ? 'Mặc định gửi toàn bộ lộ trình cho chuyên gia · Tuần thai $_gestationalWeek'
                              : 'Mặc định gửi toàn bộ lộ trình cho chuyên gia · $_stageLabel',
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 11,
                            color: _textMuted,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),

              // Privacy notice banner
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                decoration: BoxDecoration(
                  color: const Color(0xFFF0FDF4),
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: const Color(0xFFBBF7D0)),
                ),
                child: const Row(
                  children: [
                    Icon(Icons.shield_outlined, size: 16, color: Color(0xFF16A34A)),
                    SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        'Chỉ chia sẻ việc theo dõi y tế & lộ trình chuẩn. Việc cá nhân của mẹ luôn được bảo mật riêng tư.',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 11,
                          color: Color(0xFF15803D),
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 8),

              // Scope Info Bar (Mặc định toàn bộ)
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                decoration: BoxDecoration(
                  color: const Color(0xFFF7F2F0),
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: const Color(0xFFE8D5CE)),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.all_inclusive_rounded, size: 16, color: _primary),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        'Mặc định gửi toàn bộ $totalAllItems việc (Đã xong: $totalCompleted, Chờ làm: $totalPending)',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 12,
                          fontWeight: FontWeight.w700,
                          color: _primary,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 8),

              // Filter: Trạng thái xem trước
              Row(
                children: [
                  _buildStatusChip('ALL', 'Tất cả ($totalAllItems)'),
                  const SizedBox(width: 6),
                  _buildStatusChip('COMPLETED', 'Đã xong ($totalCompleted)'),
                  const SizedBox(width: 6),
                  _buildStatusChip('PENDING', 'Chờ làm ($totalPending)'),
                ],
              ),
              const SizedBox(height: 6),

              // Tabs
              TabBar(
                controller: _tabController,
                labelColor: _primary,
                unselectedLabelColor: _textMuted,
                indicatorColor: _primary,
                labelStyle: const TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.bold, fontSize: 12),
                unselectedLabelStyle: const TextStyle(fontFamily: 'Lexend', fontSize: 12),
                tabs: [
                  Tab(text: 'Lịch sử ($totalHistory)'),
                  Tab(text: 'Hiện tại ($totalCurrent)'),
                  Tab(text: 'Tương lai ($totalFuture)'),
                ],
              ),
              const SizedBox(height: 8),

              // Tab Views
              if (_loading)
                const Expanded(
                  child: Center(child: CircularProgressIndicator(color: _primary)),
                )
              else
                Expanded(
                  child: TabBarView(
                    controller: _tabController,
                    children: [
                      _buildChecklistPreviewList(_filterItems(_historyItems), 'Chưa có lịch sử việc cần làm nào.'),
                      _buildChecklistPreviewList(_filterItems(_currentItems), 'Không có việc nào trong tuần này.'),
                      _buildChecklistPreviewList(_filterItems(_futureItems), 'Chưa có lộ trình tương lai.'),
                    ],
                  ),
                ),

              const SizedBox(height: 10),

              // Note field
              TextField(
                controller: _noteController,
                maxLines: 2,
                style: const TextStyle(fontFamily: 'Lexend', fontSize: 13),
                decoration: InputDecoration(
                  hintText: 'Thêm câu hỏi hoặc ghi chú cho Bác sĩ (tùy chọn)...',
                  hintStyle: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    color: Color(0xFF9E8E8A),
                  ),
                  contentPadding: const EdgeInsets.all(12),
                  filled: true,
                  fillColor: const Color(0xFFFAF7F6),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: const BorderSide(color: Color(0xFFE8D5CE)),
                  ),
                  enabledBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: const BorderSide(color: Color(0xFFE8D5CE)),
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: const BorderSide(color: _primary, width: 1.5),
                  ),
                ),
              ),
              const SizedBox(height: 12),

              // Send button: Gửi toàn bộ
              FilledButton.icon(
                key: const Key('share-all-checklist-btn'),
                onPressed: _loading ? null : _onConfirm,
                icon: const Icon(Icons.send_rounded, size: 18),
                label: Text(
                  'Chia sẻ toàn bộ việc cần làm ($totalAllItems việc)',
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontWeight: FontWeight.bold,
                    fontSize: 13,
                  ),
                ),
                style: FilledButton.styleFrom(
                  backgroundColor: _primary,
                  minimumSize: const Size.fromHeight(48),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
              ),
              const SizedBox(height: 8),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildStatusChip(String key, String label) {
    final selected = _statusFilter == key;
    return ChoiceChip(
      label: Text(label),
      selected: selected,
      selectedColor: _primary,
      backgroundColor: const Color(0xFFFAF7F6),
      labelStyle: TextStyle(
        fontFamily: 'Lexend',
        fontSize: 11,
        fontWeight: selected ? FontWeight.bold : FontWeight.w500,
        color: selected ? Colors.white : _textDark,
      ),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
        side: BorderSide(
          color: selected ? _primary : const Color(0xFFE8D5CE),
        ),
      ),
      onSelected: (val) {
        if (val) {
          setState(() => _statusFilter = key);
        }
      },
    );
  }

  Widget _buildChecklistPreviewList(List<_ChecklistShareItem> items, String emptyMessage) {
    if (items.isEmpty) {
      return Center(
        child: Text(
          emptyMessage,
          style: const TextStyle(fontFamily: 'Lexend', fontSize: 12, color: Color(0xFF9E8E8A)),
        ),
      );
    }

    return Material(
      color: const Color(0xFFFAF7F6),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
        side: const BorderSide(color: Color(0xFFE8D5CE)),
      ),
      clipBehavior: Clip.antiAlias,
      child: ListView.separated(
        padding: const EdgeInsets.symmetric(vertical: 4),
        itemCount: items.length,
        separatorBuilder: (_, _) => const Divider(height: 1, color: Color(0xFFECE4E1)),
        itemBuilder: (ctx, idx) {
          final item = items[idx];
          return Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Padding(
                  padding: const EdgeInsets.only(top: 2),
                  child: Icon(
                    item.completed
                        ? Icons.check_circle_rounded
                        : Icons.radio_button_unchecked_rounded,
                    size: 18,
                    color: item.completed
                        ? const Color(0xFF2E7D32)
                        : const Color(0xFFC98C7B),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Expanded(
                            child: Text(
                              item.text,
                              style: TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 13,
                                fontWeight: FontWeight.w600,
                                color: item.completed
                                    ? const Color(0xFF5A4E4B)
                                    : _textDark,
                              ),
                            ),
                          ),
                          const SizedBox(width: 8),
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                            decoration: BoxDecoration(
                              color: item.completed
                                  ? const Color(0xFFE8F5E9)
                                  : const Color(0xFFFFF3E0),
                              borderRadius: BorderRadius.circular(6),
                            ),
                            child: Text(
                              item.completed ? 'Đã xong' : 'Chờ làm',
                              style: TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 10,
                                fontWeight: FontWeight.bold,
                                color: item.completed
                                    ? const Color(0xFF2E7D32)
                                    : const Color(0xFFE65100),
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 4),
                      Row(
                        children: [
                          // Badge phân loại Gợi ý CareBridge vs Bác sĩ chỉ định
                          if (item.isExpertCustom || item.origin == 'EXPERT')
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                              margin: const EdgeInsets.only(right: 6),
                              decoration: BoxDecoration(
                                color: const Color(0xFFCCFBF1),
                                borderRadius: BorderRadius.circular(4),
                                border: Border.all(color: const Color(0xFF99F6E4)),
                              ),
                              child: const Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Icon(Icons.medical_services_rounded, size: 10, color: Color(0xFF0F766E)),
                                  SizedBox(width: 2),
                                  Text(
                                    'Bác sĩ chỉ định',
                                    style: TextStyle(
                                      fontFamily: 'Lexend',
                                      fontSize: 9,
                                      fontWeight: FontWeight.bold,
                                      color: Color(0xFF0F766E),
                                    ),
                                  ),
                                ],
                              ),
                            )
                          else
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                              margin: const EdgeInsets.only(right: 6),
                              decoration: BoxDecoration(
                                color: const Color(0xFFE0F2FE),
                                borderRadius: BorderRadius.circular(4),
                                border: Border.all(color: const Color(0xFFBAE6FD)),
                              ),
                              child: const Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Icon(Icons.auto_awesome_rounded, size: 10, color: Color(0xFF0284C7)),
                                  SizedBox(width: 2),
                                  Text(
                                    'Gợi ý CareBridge',
                                    style: TextStyle(
                                      fontFamily: 'Lexend',
                                      fontSize: 9,
                                      fontWeight: FontWeight.bold,
                                      color: Color(0xFF0369A1),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          Expanded(
                            child: Text(
                              '${item.timeLabel} · ${item.category}',
                              style: const TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 11,
                                color: Color(0xFF9E8E8A),
                              ),
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }
}
