import 'package:flutter/material.dart';
import '../../checklist/services/user_checklist_service.dart';
import '../../checklist/models/user_checklist_item_model.dart';
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

class _SelectableItem {
  final String id;
  final String text;
  final bool completed;
  final String category;
  final String timeLabel;
  final String section; // 'HISTORY', 'CURRENT', 'FUTURE'
  final String origin; // 'SYSTEM' | 'USER' | 'EXPERT'
  final String createdBy;
  bool isSelected;

  _SelectableItem({
    required this.id,
    required this.text,
    required this.completed,
    required this.category,
    required this.timeLabel,
    required this.section,
    this.origin = 'SYSTEM',
    this.createdBy = 'SYSTEM',
    this.isSelected = true,
  });

  bool get isExpertCustom => origin == 'EXPERT' || createdBy == 'EXPERT';
  String? get doctorNote => null;

  bool get isPersonal =>
      origin == 'USER' ||
      origin == 'EXPERT' ||
      createdBy == 'USER' ||
      createdBy == 'EXPERT';
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
  int? _gestationalWeek;
  String? _journeyId;
  String _statusFilter = 'ALL'; // ALL, COMPLETED, PENDING
  String _originFilter = 'ALL'; // ALL, CAREBRIDGE, PERSONAL

  List<_SelectableItem> _historyItems = [];
  List<_SelectableItem> _currentItems = [];
  List<_SelectableItem> _futureItems = [];

  @override
  void initState() {
    super.initState();
    // Mặc định pop-up hiển thị tab "Hiện tại" (initialIndex: 1)
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
    try {
      final dashboard = await JourneyService().getDashboard();
      _gestationalWeek = dashboard.effectivePregnancyWeek ??
          dashboard.completedGestationalWeek ??
          24;
      _journeyId = dashboard.journeyId;
    } catch (_) {
      _gestationalWeek = 24;
    }

    final currentWk = _gestationalWeek ?? 24;

    // 1. Load categorized roadmap tasks for history and future (Gợi ý CareBridge / System templates)
    try {
      final categorized = await ChecklistRoadmapService.instance
          .loadCategorizedTasks(currentWeek: currentWk);

      final hist = categorized['history'] ?? [];
      final curr = categorized['current'] ?? [];
      final fut = categorized['future'] ?? [];

      _historyItems = hist
          .map((t) => _SelectableItem(
                id: t.id,
                text: t.title,
                completed: true,
                category: t.category,
                timeLabel: 'Tuần ${t.dueWeek ?? (currentWk - 4)}',
                section: 'HISTORY',
                origin: 'SYSTEM',
                createdBy: 'SYSTEM',
                isSelected: false,
              ))
          .toList();

      _currentItems = curr
          .map((t) => _SelectableItem(
                id: t.id,
                text: t.title,
                completed: t.completed,
                category: t.category,
                timeLabel: 'Tuần $currentWk (Hiện tại)',
                section: 'CURRENT',
                origin: 'SYSTEM',
                createdBy: 'SYSTEM',
                isSelected: true,
              ))
          .toList();

      _futureItems = fut
          .map((t) => _SelectableItem(
                id: t.id,
                text: t.title,
                completed: false,
                category: t.category,
                timeLabel: 'Tuần ${t.dueWeek ?? (currentWk + 4)} (Tương lai)',
                section: 'FUTURE',
                origin: 'SYSTEM',
                createdBy: 'SYSTEM',
                isSelected: false,
              ))
          .toList();
    } catch (_) {}

    // Tập hợp tất cả tiêu đề thuộc lộ trình chuẩn CareBridge
    final roadmapTitleSet = {
      for (final c in _currentItems) c.text.trim().toLowerCase(),
      for (final h in _historyItems) h.text.trim().toLowerCase(),
      for (final f in _futureItems) f.text.trim().toLowerCase(),
    };

    // 2. Load live today tasks from TodayTaskService (phân loại chính xác Gợi ý vs Việc cá nhân)
    try {
      final snapshot = await TodayTaskService.instance.loadToday();
      final liveTasks = snapshot.sections.all.toList();
      if (liveTasks.isNotEmpty) {
        final existingMap = {
          for (final c in _currentItems) c.text.trim().toLowerCase(): c
        };

        final updatedCurrent = <_SelectableItem>[];
        for (final t in liveTasks) {
          final key = t.title.trim().toLowerCase();
          final existing = existingMap[key];
          final isRoadmapItem = roadmapTitleSet.contains(key);
          final isPersonalTask = !isRoadmapItem ||
              t.origin == TodayTaskOrigin.userCreated ||
              t.isCareTask;
          final origin = isPersonalTask ? 'USER' : 'SYSTEM';

          updatedCurrent.add(_SelectableItem(
            id: t.id,
            text: t.title,
            completed: t.isCompleted,
            category: existing?.category ?? 'Khám thai & Y tế',
            timeLabel: existing?.timeLabel ?? 'Tuần $currentWk (Hiện tại)',
            section: 'CURRENT',
            origin: origin,
            createdBy: isPersonalTask ? 'USER' : 'SYSTEM',
            isSelected: existing?.isSelected ?? true,
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

    // 3. Synchronize with UserChecklistService for custom personal items (Mẹ tự tạo)
    try {
      final serverItems = await UserChecklistService.instance.listItems();
      if (serverItems.isNotEmpty) {
        for (final si in serverItems) {
          final key = si.itemText.trim().toLowerCase();
          final idx = _currentItems.indexWhere((c) => c.text.trim().toLowerCase() == key);
          if (idx >= 0) {
            // Cập nhật trạng thái completed, nhưng giữ nguyên origin SYSTEM nếu thuộc roadmap
            final cur = _currentItems[idx];
            final isRoadmapItem = roadmapTitleSet.contains(key);
            _currentItems[idx] = _SelectableItem(
              id: si.itemId,
              text: cur.text,
              completed: si.completed,
              category: cur.category,
              timeLabel: cur.timeLabel,
              section: cur.section,
              origin: isRoadmapItem ? 'SYSTEM' : 'USER',
              createdBy: isRoadmapItem ? 'SYSTEM' : 'USER',
              isSelected: cur.isSelected,
            );
          } else {
            // Việc cá nhân do mẹ tạo ngoài roadmap
            _currentItems.add(_SelectableItem(
              id: si.itemId,
              text: si.itemText,
              completed: si.completed,
              category: si.category.label,
              timeLabel: 'Tuần $currentWk (Hiện tại)',
              section: 'CURRENT',
              origin: 'USER',
              createdBy: 'USER',
              isSelected: true,
            ));
          }
        }
      }
    } catch (_) {}

    // 4. Lọc bỏ tuyệt đối: Các mục thuộc "Lịch sử đã qua" không được hiển thị ở "Tuần hiện tại"
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

  void _selectAllTabs(bool select) {
    setState(() {
      for (final i in _historyItems) {
        i.isSelected = select;
      }
      for (final i in _currentItems) {
        i.isSelected = select;
      }
      for (final i in _futureItems) {
        i.isSelected = select;
      }
    });
  }

  void _selectByType(String type) {
    setState(() {
      for (final i in [..._historyItems, ..._currentItems, ..._futureItems]) {
        if (type == 'CAREBRIDGE') {
          i.isSelected = i.isCareBridgeSuggestion;
        } else if (type == 'PERSONAL') {
          i.isSelected = i.isPersonal;
        } else {
          i.isSelected = true;
        }
      }
    });
  }

  void _onConfirm() {
    final selectedHist = _historyItems.where((i) => i.isSelected).toList();
    final selectedCurr = _currentItems.where((i) => i.isSelected).toList();
    final selectedFut = _futureItems.where((i) => i.isSelected).toList();

    final totalSelected =
        selectedHist.length + selectedCurr.length + selectedFut.length;

    if (totalSelected == 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Vui lòng chọn ít nhất 1 việc để chia sẻ')),
      );
      return;
    }

    final completedCount = selectedHist.where((i) => i.completed).length +
        selectedCurr.where((i) => i.completed).length +
        selectedFut.where((i) => i.completed).length;
    final percent =
        totalSelected > 0 ? ((completedCount / totalSelected) * 100).round() : 0;

    final shareData = ChecklistShareData(
      title: 'Danh sách việc cần làm (Checklist)',
      gestationalWeek: _gestationalWeek,
      journeyId: _journeyId,
      isLiveSync: true,
      completedCount: completedCount,
      totalCount: totalSelected,
      progressPercent: percent,
      note: _noteController.text.trim().isEmpty
          ? null
          : _noteController.text.trim(),
      historyItems: selectedHist
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
      currentItems: selectedCurr
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
      futureItems: selectedFut
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

  List<_SelectableItem> _filterItems(List<_SelectableItem> items) {
    return items.where((i) {
      // Filter by origin/type
      if (_originFilter == 'CAREBRIDGE' && !i.isCareBridgeSuggestion) return false;
      if (_originFilter == 'PERSONAL' && !i.isPersonal) return false;

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
    final totalCbItems = allItemsList.where((i) => i.isCareBridgeSuggestion).length;
    final totalPersonalItems = allItemsList.where((i) => i.isPersonal).length;

    final selectedItemsList = allItemsList.where((i) => i.isSelected).toList();
    final totalSelected = selectedItemsList.length;
    final selectedCbItems = selectedItemsList.where((i) => i.isCareBridgeSuggestion).length;
    final selectedPersonalItems = selectedItemsList.where((i) => i.isPersonal).length;

    final selectedCompleted = selectedItemsList.where((i) => i.completed).length;
    final selectedPending = totalSelected - selectedCompleted;

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
                              ? 'Mặc định tuần thai $_gestationalWeek (Hiện tại) · Đồng bộ trạng thái thực'
                              : 'Gửi danh sách việc đã làm và chưa làm cho chuyên gia',
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

              // Filter Row 1: Phân loại Nguồn việc (CareBridge vs Cá nhân)
              SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: Row(
                  children: [
                    _buildOriginChip('ALL', 'Tất cả ($totalAllItems)'),
                    const SizedBox(width: 6),
                    _buildOriginChip('CAREBRIDGE', '✨ Gợi ý CareBridge ($totalCbItems)'),
                    const SizedBox(width: 6),
                    _buildOriginChip('PERSONAL', '👤 Việc cá nhân ($totalPersonalItems)'),
                  ],
                ),
              ),
              const SizedBox(height: 6),

              // Filter Row 2: Trạng thái hoàn thành
              Row(
                children: [
                  _buildStatusChip('ALL', 'Tất cả trạng thái'),
                  const SizedBox(width: 6),
                  _buildStatusChip('COMPLETED', 'Đã xong ($selectedCompleted)'),
                  const SizedBox(width: 6),
                  _buildStatusChip('PENDING', 'Chờ làm ($selectedPending)'),
                ],
              ),
              const SizedBox(height: 8),

              // Quick Selective Action Bar
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                decoration: BoxDecoration(
                  color: const Color(0xFFF7F2F0),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(
                          'Đã chọn: $totalSelected việc ($selectedCbItems Gợi ý · $selectedPersonalItems Cá nhân)',
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 11,
                            fontWeight: FontWeight.bold,
                            color: _primary,
                          ),
                        ),
                        TextButton(
                          onPressed: () => _selectAllTabs(false),
                          style: TextButton.styleFrom(
                            padding: const EdgeInsets.symmetric(horizontal: 4),
                            minimumSize: Size.zero,
                            tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                          ),
                          child: const Text(
                            'Bỏ chọn hết',
                            style: TextStyle(fontFamily: 'Lexend', fontSize: 11, color: _textMuted),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 4),
                    Row(
                      children: [
                        Expanded(
                          child: OutlinedButton.icon(
                            onPressed: () => _selectByType('CAREBRIDGE'),
                            icon: const Icon(Icons.auto_awesome_rounded, size: 13, color: Color(0xFF0284C7)),
                            label: const Text(
                              'Chỉ chọn Gợi ý',
                              style: TextStyle(fontFamily: 'Lexend', fontSize: 11, color: Color(0xFF0369A1)),
                            ),
                            style: OutlinedButton.styleFrom(
                              padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 6),
                              side: const BorderSide(color: Color(0xFFBAE6FD)),
                              backgroundColor: const Color(0xFFF0F9FF),
                              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                            ),
                          ),
                        ),
                        const SizedBox(width: 6),
                        Expanded(
                          child: OutlinedButton.icon(
                            onPressed: () => _selectByType('PERSONAL'),
                            icon: const Icon(Icons.person_rounded, size: 13, color: Color(0xFF7E22CE)),
                            label: const Text(
                              'Chỉ chọn Cá nhân',
                              style: TextStyle(fontFamily: 'Lexend', fontSize: 11, color: Color(0xFF6B21A8)),
                            ),
                            style: OutlinedButton.styleFrom(
                              padding: const EdgeInsets.symmetric(vertical: 4, horizontal: 6),
                              side: const BorderSide(color: Color(0xFFE9D5FF)),
                              backgroundColor: const Color(0xFFFAF5FF),
                              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                            ),
                          ),
                        ),
                        const SizedBox(width: 6),
                        TextButton(
                          onPressed: () => _selectAllTabs(true),
                          style: TextButton.styleFrom(
                            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 4),
                            backgroundColor: _primary.withValues(alpha: 0.1),
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                          ),
                          child: const Text(
                            'Chọn tất cả',
                            style: TextStyle(fontFamily: 'Lexend', fontSize: 11, fontWeight: FontWeight.bold, color: _primary),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
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
                      _buildChecklistSelectionList(_filterItems(_historyItems), 'Chưa có lịch sử checklist nào.'),
                      _buildChecklistSelectionList(_filterItems(_currentItems), 'Không có việc nào trong tuần này.'),
                      _buildChecklistSelectionList(_filterItems(_futureItems), 'Chưa có lộ trình tương lai.'),
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

              // Send button
              FilledButton.icon(
                onPressed: _onConfirm,
                icon: const Icon(Icons.send_rounded, size: 18),
                label: Text(
                  'Chia sẻ $totalSelected việc ($selectedCbItems gợi ý · $selectedPersonalItems cá nhân)',
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

  Widget _buildOriginChip(String key, String label) {
    final selected = _originFilter == key;
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
          setState(() => _originFilter = key);
        }
      },
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

  Widget _buildChecklistSelectionList(List<_SelectableItem> items, String emptyMessage) {
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
          return CheckboxListTile(
            value: item.isSelected,
            activeColor: _primary,
            dense: true,
            contentPadding: const EdgeInsets.symmetric(horizontal: 10),
            title: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(
                  item.completed
                      ? Icons.check_circle_rounded
                      : Icons.radio_button_unchecked_rounded,
                  size: 16,
                  color: item.completed
                      ? const Color(0xFF2E7D32)
                      : const Color(0xFFC98C7B),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              item.text,
                              style: TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 12,
                                fontWeight: FontWeight.w500,
                                color: item.completed
                                    ? const Color(0xFF5A4E4B)
                                    : _textDark,
                              ),
                            ),
                          ),
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                            decoration: BoxDecoration(
                              color: item.completed
                                  ? const Color(0xFFE8F5E9)
                                  : const Color(0xFFFFF3E0),
                              borderRadius: BorderRadius.circular(4),
                            ),
                            child: Text(
                              item.completed ? 'Đã xong' : 'Chờ làm',
                              style: TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 9,
                                fontWeight: FontWeight.bold,
                                color: item.completed
                                    ? const Color(0xFF2E7D32)
                                    : const Color(0xFFE65100),
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 3),
                      Row(
                        children: [
                          // Badge phân loại Gợi ý CareBridge vs Việc cá nhân
                          if (item.isCareBridgeSuggestion)
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 4.5, vertical: 0.5),
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
                            )
                          else if (item.isExpertCustom || item.origin == 'EXPERT')
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 4.5, vertical: 0.5),
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
                              padding: const EdgeInsets.symmetric(horizontal: 4.5, vertical: 0.5),
                              margin: const EdgeInsets.only(right: 6),
                              decoration: BoxDecoration(
                                color: const Color(0xFFF3E8FF),
                                borderRadius: BorderRadius.circular(4),
                                border: Border.all(color: const Color(0xFFE9D5FF)),
                              ),
                              child: const Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Icon(Icons.person_rounded, size: 10, color: Color(0xFF7E22CE)),
                                  SizedBox(width: 2),
                                  Text(
                                    'Mẹ tự tạo',
                                    style: TextStyle(
                                      fontFamily: 'Lexend',
                                      fontSize: 9,
                                      fontWeight: FontWeight.bold,
                                      color: Color(0xFF6B21A8),
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
                                fontSize: 10,
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
            onChanged: (val) {
              setState(() {
                item.isSelected = val ?? false;
              });
            },
          );
        },
      ),
    );
  }
}
