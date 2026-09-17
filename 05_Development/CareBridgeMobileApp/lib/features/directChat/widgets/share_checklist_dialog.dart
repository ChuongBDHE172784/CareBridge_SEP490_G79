import 'package:flutter/material.dart';
import '../../baby/services/baby_service.dart';
import '../../checklist/models/checklist_roadmap_model.dart';
import '../../checklist/models/user_checklist_item_model.dart';
import '../../checklist/services/user_checklist_service.dart';
import '../../checklist/services/checklist_roadmap_service.dart';
import '../../reminder/models/today_task_model.dart';
import '../../reminder/services/today_task_service.dart';
import '../../journey/services/journey_service.dart';
import 'checklist_message_card.dart';

class ShareChecklistDialog extends StatefulWidget {
  const ShareChecklistDialog({
    super.key,
    this.initialStage,
    this.initialGestationalWeek,
    this.initialBabyName,
  });

  final String? initialStage;
  final int? initialGestationalWeek;
  final String? initialBabyName;

  static Future<ChecklistShareData?> show(
    BuildContext context, {
    String? initialStage,
    int? initialGestationalWeek,
    String? initialBabyName,
  }) {
    return showModalBottomSheet<ChecklistShareData>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (ctx) => ShareChecklistDialog(
        initialStage: initialStage,
        initialGestationalWeek: initialGestationalWeek,
        initialBabyName: initialBabyName,
      ),
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
  final bool isBaby;
  final String? babyLabel;

  _ChecklistShareItem({
    required this.id,
    required this.text,
    required this.completed,
    required this.category,
    required this.timeLabel,
    required this.section,
    this.origin = 'SYSTEM',
    this.createdBy = 'SYSTEM',
    this.isBaby = false,
    this.babyLabel,
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
  String _targetFilter = 'ALL'; // ALL, MOTHER, BABY

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

  bool _isBabyRoadmapTask(ChecklistRoadmapTask t) {
    final lowerId = t.id.toLowerCase();
    final lowerCat = t.category.toLowerCase();
    return lowerId.startsWith('baby_') ||
        lowerCat.contains('bé') ||
        lowerCat.contains('baby') ||
        lowerCat == 'chăm sóc em bé';
  }

  String _formatBabyMilestoneTime(ChecklistRoadmapTask t, String defaultLabel) {
    final id = t.id.toUpperCase();
    if (id.contains('0_28D')) return 'Bé · Sơ sinh (0–28 ngày)';
    if (id.contains('1_2M')) return 'Bé · 1–2 tháng';
    if (id.contains('2_3M')) return 'Bé · 2–3 tháng';
    if (id.contains('4_6M')) return 'Bé · 4–6 tháng';
    if (id.contains('7_9M')) return 'Bé · 7–9 tháng';
    if (id.contains('10_12M')) return 'Bé · 10–12 tháng';
    if (id.contains('12M')) return 'Bé · 1 tuổi';
    if (id.contains('13_18M')) return 'Bé · 13–18 tháng';
    if (id.contains('19_24M')) return 'Bé · 19–24 tháng';
    return defaultLabel;
  }

  bool _isBabyTodayTask(TodayTask t) {
    if (t.careContextType?.toUpperCase() == 'JOURNEY' ||
        t.target == TodayTaskTarget.mother ||
        t.stage == TodayChecklistStage.pregnancy ||
        t.stage == TodayChecklistStage.prePregnancy) {
      return false;
    }
    if (t.stage == TodayChecklistStage.babyCare ||
        t.target == TodayTaskTarget.baby ||
        t.careContextType?.toUpperCase() == 'BABY') {
      return true;
    }
    final label = (t.careContextLabel ?? '').trim().toLowerCase();
    if (label.contains('mang thai') ||
        label.contains('thai kỳ') ||
        label.contains('chuẩn bị') ||
        label.contains('mẹ')) {
      return false;
    }
    final title = t.title.toLowerCase();
    return title.contains('sơ sinh') ||
        title.contains('chăm bé') ||
        title.contains('em bé') ||
        title.contains('trẻ sơ sinh');
  }

  Future<void> _loadAllChecklistData() async {
    int currentWk = 1;
    if (widget.initialStage != null) {
      _stage = widget.initialStage!.toUpperCase();
      if (_stage == 'PRE_PREGNANCY') {
        _stageLabel = 'Chuẩn bị mang thai';
        _gestationalWeek = null;
        currentWk = 1;
      } else if (_stage == 'BABY_CARE') {
        _stageLabel = 'Chăm sóc bé';
        _gestationalWeek = null;
        currentWk = 1;
      } else if (_stage == 'POSTPARTUM') {
        _stageLabel = 'Sau sinh & Chăm bé';
        _gestationalWeek = null;
        currentWk = 1;
      } else {
        _stage = 'PREGNANCY';
        _gestationalWeek = widget.initialGestationalWeek ?? 12;
        _stageLabel = 'Tuần thai thứ $_gestationalWeek';
        currentWk = _gestationalWeek ?? 12;
      }
    } else {
      try {
        final dashboard = await JourneyService().getDashboard();
        _journeyId = dashboard.journeyId;
        final rawStage = (dashboard.journeyType ?? '').toUpperCase();
        final rawStatus = (dashboard.status ?? '').toUpperCase();
        if (dashboard.isPrePregnancy || rawStage == 'PRE_PREGNANCY') {
          _stage = 'PRE_PREGNANCY';
          _stageLabel = 'Chuẩn bị mang thai';
          _gestationalWeek = null;
          currentWk = 1;
        } else if (rawStage == 'BABY_CARE' || rawStatus == 'BABY_CARE') {
          _stage = 'BABY_CARE';
          _stageLabel = 'Chăm sóc bé';
          _gestationalWeek = null;
          currentWk = 1;
        } else if (dashboard.isPostpartum || rawStage == 'POSTPARTUM' || rawStatus == 'ACTIVE_POSTPARTUM') {
          _stage = 'POSTPARTUM';
          _stageLabel = 'Sau sinh & Chăm bé';
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
    }

    bool hasBaby = _stage == 'BABY_CARE' ||
        _stage == 'POSTPARTUM' ||
        widget.initialBabyName != null;
    String? primaryBabyName = widget.initialBabyName;
    if (primaryBabyName == null) {
      try {
        final babies = await BabyService().listBabyProfiles();
        if (babies.isNotEmpty) {
          hasBaby = true;
          primaryBabyName = babies.first.nickname;
        }
      } catch (_) {}
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
                        : (_stage == 'BABY_CARE'
                            ? _formatBabyMilestoneTime(t, 'Đã hoàn thành')
                            : 'Tuần ${t.dueWeek ?? (currentWk - 4)}')),
                section: 'HISTORY',
                origin: 'SYSTEM',
                createdBy: 'SYSTEM',
                isBaby: _stage == 'BABY_CARE' || _isBabyRoadmapTask(t),
                babyLabel: (_stage == 'BABY_CARE' || _isBabyRoadmapTask(t)) ? primaryBabyName : null,
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
                        : (_stage == 'BABY_CARE'
                            ? _formatBabyMilestoneTime(t, 'Chăm bé (Hiện tại)')
                            : 'Tuần $currentWk (Hiện tại)')),
                section: 'CURRENT',
                origin: 'SYSTEM',
                createdBy: 'SYSTEM',
                isBaby: _stage == 'BABY_CARE' || _isBabyRoadmapTask(t),
                babyLabel: (_stage == 'BABY_CARE' || _isBabyRoadmapTask(t)) ? primaryBabyName : null,
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
                        : (_stage == 'BABY_CARE'
                            ? _formatBabyMilestoneTime(t, 'Kế hoạch tiếp theo')
                            : 'Tuần ${t.dueWeek ?? (currentWk + 4)} (Tương lai)')),
                section: 'FUTURE',
                origin: 'SYSTEM',
                createdBy: 'SYSTEM',
                isBaby: _stage == 'BABY_CARE' || _isBabyRoadmapTask(t),
                babyLabel: (_stage == 'BABY_CARE' || _isBabyRoadmapTask(t)) ? primaryBabyName : null,
              ))
          .toList();

      // Nếu mẹ ở giai đoạn sau sinh hoặc có hồ sơ em bé, tải thêm mốc chăm sóc bé BABY_CARE
      if (hasBaby && _stage != 'BABY_CARE') {
        try {
          final babyCategorized = await ChecklistRoadmapService.instance
              .loadCategorizedTasks(currentWeek: 1, stage: 'BABY_CARE');

          final babyHist = babyCategorized['history'] ?? [];
          final babyCurr = babyCategorized['current'] ?? [];
          final babyFut = babyCategorized['future'] ?? [];

          for (final t in babyHist) {
            _historyItems.add(_ChecklistShareItem(
              id: t.id,
              text: t.title,
              completed: true,
              category: t.category.isNotEmpty ? t.category : 'Chăm sóc bé',
              timeLabel: _formatBabyMilestoneTime(t, 'Đã hoàn thành'),
              section: 'HISTORY',
              origin: 'SYSTEM',
              createdBy: 'SYSTEM',
              isBaby: true,
              babyLabel: primaryBabyName,
            ));
          }

          for (final t in babyCurr) {
            _currentItems.add(_ChecklistShareItem(
              id: t.id,
              text: t.title,
              completed: t.completed,
              category: t.category.isNotEmpty ? t.category : 'Chăm sóc bé',
              timeLabel: _formatBabyMilestoneTime(t, 'Bé · Sơ sinh (0–28 ngày)'),
              section: 'CURRENT',
              origin: 'SYSTEM',
              createdBy: 'SYSTEM',
              isBaby: true,
              babyLabel: primaryBabyName,
            ));
          }

          for (final t in babyFut) {
            _futureItems.add(_ChecklistShareItem(
              id: t.id,
              text: t.title,
              completed: false,
              category: t.category.isNotEmpty ? t.category : 'Chăm sóc bé',
              timeLabel: _formatBabyMilestoneTime(t, 'Kế hoạch phát triển bé'),
              section: 'FUTURE',
              origin: 'SYSTEM',
              createdBy: 'SYSTEM',
              isBaby: true,
              babyLabel: primaryBabyName,
            ));
          }
        } catch (_) {}
      }
    } catch (_) {}

    // Tập hợp tất cả tiêu đề thuộc lộ trình chuẩn CareBridge
    final roadmapTitleSet = {
      for (final c in _currentItems) c.text.trim().toLowerCase(),
      for (final h in _historyItems) h.text.trim().toLowerCase(),
      for (final f in _futureItems) f.text.trim().toLowerCase(),
    };

    // 2. Load live today tasks from TodayTaskService (chỉ đồng bộ trạng thái cho lộ trình, không bỏ sót việc của bé)
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
          final isBaby = _isBabyTodayTask(t) || (existing != null && existing.isBaby);

          // Loại bỏ tuyệt đối việc cá nhân tự tạo khi chia sẻ cho chuyên gia, nhưng giữ lại việc hệ thống gợi ý cho mẹ & bé
          final isPersonalTask = (!isRoadmapItem && !isBaby && t.origin != TodayTaskOrigin.systemTemplate) ||
              t.origin == TodayTaskOrigin.userCreated;

          if (isPersonalTask) continue;

          final babyName = isBaby
              ? ((t.careContextType?.toUpperCase() == 'BABY' &&
                      t.careContextLabel != null &&
                      t.careContextLabel!.trim().isNotEmpty)
                  ? t.careContextLabel!.trim()
                  : (existing?.babyLabel ?? primaryBabyName))
              : null;
          final babyCategory = babyName != null && babyName.isNotEmpty
              ? 'Chăm bé · $babyName'
              : 'Chăm sóc bé';
          final babyTimeLabel = babyName != null && babyName.isNotEmpty
              ? 'Bé $babyName (Hôm nay)'
              : 'Chăm bé (Hôm nay)';

          updatedCurrent.add(_ChecklistShareItem(
            id: t.id,
            text: t.title,
            completed: t.isCompleted,
            category: existing?.category ?? (isBaby ? babyCategory : 'Khám thai & Y tế'),
            timeLabel: existing?.timeLabel ??
                (isBaby
                    ? babyTimeLabel
                    : (_stage == 'PRE_PREGNANCY'
                        ? 'Chuẩn bị mang thai'
                        : (_stage == 'POSTPARTUM'
                            ? 'Sau sinh'
                            : (_stage == 'BABY_CARE'
                                ? 'Chăm sóc bé'
                                : 'Tuần $currentWk (Hiện tại)')))),
            section: 'CURRENT',
            origin: 'SYSTEM',
            createdBy: 'SYSTEM',
            isBaby: isBaby,
            babyLabel: babyName,
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

    // 3. Synchronize with UserChecklistService: cập nhật trạng thái completed và nạp thêm việc checklist của bé
    try {
      final serverItems = await UserChecklistService.instance.listItems();
      if (serverItems.isNotEmpty) {
        for (final si in serverItems) {
          final key = si.itemText.trim().toLowerCase();
          final isBabySi = si.category == ChecklistCategory.babyCare ||
              si.targetSubject?.toUpperCase() == 'BABY';
          final idx = _currentItems.indexWhere((c) => c.text.trim().toLowerCase() == key);
          if (idx >= 0 && (roadmapTitleSet.contains(key) || isBabySi)) {
            final cur = _currentItems[idx];
            final isBabyResult = cur.isBaby || isBabySi;
            _currentItems[idx] = _ChecklistShareItem(
              id: si.itemId,
              text: cur.text,
              completed: si.completed,
              category: cur.category,
              timeLabel: cur.timeLabel,
              section: cur.section,
              origin: cur.origin,
              createdBy: cur.createdBy,
              isBaby: isBabyResult,
              babyLabel: isBabyResult ? (cur.babyLabel ?? primaryBabyName) : null,
            );
          } else if (isBabySi &&
              si.origin != 'USER' &&
              si.origin != 'USER_CREATED' &&
              !_currentItems.any((c) => c.text.trim().toLowerCase() == key)) {
            _currentItems.add(_ChecklistShareItem(
              id: si.itemId,
              text: si.itemText,
              completed: si.completed,
              category: 'Chăm sóc bé',
              timeLabel: 'Checklist bé',
              section: 'CURRENT',
              origin: si.origin ?? 'SYSTEM',
              createdBy: si.origin ?? 'SYSTEM',
              isBaby: true,
              babyLabel: primaryBabyName,
            ));
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
    final targetHistory = _historyItems.where((i) {
      if (_targetFilter == 'MOTHER') return !i.isBaby;
      if (_targetFilter == 'BABY') return i.isBaby;
      return true;
    }).toList();

    final targetCurrent = _currentItems.where((i) {
      if (_targetFilter == 'MOTHER') return !i.isBaby;
      if (_targetFilter == 'BABY') return i.isBaby;
      return true;
    }).toList();

    final targetFuture = _futureItems.where((i) {
      if (_targetFilter == 'MOTHER') return !i.isBaby;
      if (_targetFilter == 'BABY') return i.isBaby;
      return true;
    }).toList();

    final totalCount =
        targetHistory.length + targetCurrent.length + targetFuture.length;

    if (totalCount == 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Không có việc cần làm nào để chia sẻ')),
      );
      return;
    }

    final completedCount = targetHistory.where((i) => i.completed).length +
        targetCurrent.where((i) => i.completed).length +
        targetFuture.where((i) => i.completed).length;
    final percent =
        totalCount > 0 ? ((completedCount / totalCount) * 100).round() : 0;

    String title;
    String stage = _stage;
    String stageLabel = _stageLabel;

    if (_targetFilter == 'BABY') {
      stage = 'BABY_CARE';
      stageLabel = 'Chăm sóc bé';
      title = 'Danh sách việc chăm sóc bé';
    } else if (_targetFilter == 'MOTHER') {
      if (_stage == 'BABY_CARE') {
        stage = 'POSTPARTUM';
        stageLabel = 'Sau sinh';
      }
      title = _stage == 'PRE_PREGNANCY'
          ? 'Lộ trình chuẩn bị mang thai'
          : (_stage == 'POSTPARTUM' || _stage == 'BABY_CARE'
              ? 'Lộ trình chăm sóc sau sinh'
              : 'Danh sách việc cần làm của mẹ');
    } else {
      final hasBaby = [..._historyItems, ..._currentItems, ..._futureItems].any((i) => i.isBaby);
      final hasMother = [..._historyItems, ..._currentItems, ..._futureItems].any((i) => !i.isBaby);
      title = _stage == 'PRE_PREGNANCY'
          ? 'Lộ trình chuẩn bị mang thai'
          : (_stage == 'POSTPARTUM'
              ? (hasBaby && hasMother
                  ? 'Danh sách việc cần làm (Mẹ & Bé)'
                  : 'Lộ trình chăm sóc sau sinh & bé')
              : (_stage == 'BABY_CARE'
                  ? 'Danh sách việc chăm sóc bé'
                  : (hasBaby && hasMother
                      ? 'Danh sách việc cần làm (Mẹ & Bé)'
                      : (hasBaby
                          ? 'Danh sách việc chăm sóc bé'
                          : 'Danh sách việc cần làm (Checklist)'))));
    }

    final shareData = ChecklistShareData(
      title: title,
      gestationalWeek: _targetFilter == 'BABY' ? null : _gestationalWeek,
      stage: stage,
      stageLabel: stageLabel,
      journeyId: _journeyId,
      isLiveSync: true,
      completedCount: completedCount,
      totalCount: totalCount,
      progressPercent: percent,
      note: _noteController.text.trim().isEmpty
          ? null
          : _noteController.text.trim(),
      historyItems: targetHistory
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
      currentItems: targetCurrent
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
      futureItems: targetFuture
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

      // Filter by target (Mother vs Baby)
      if (_targetFilter == 'MOTHER' && i.isBaby) return false;
      if (_targetFilter == 'BABY' && !i.isBaby) return false;

      return true;
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    final targetHistoryItems = _historyItems.where((i) {
      if (_targetFilter == 'MOTHER') return !i.isBaby;
      if (_targetFilter == 'BABY') return i.isBaby;
      return true;
    }).toList();

    final targetCurrentItems = _currentItems.where((i) {
      if (_targetFilter == 'MOTHER') return !i.isBaby;
      if (_targetFilter == 'BABY') return i.isBaby;
      return true;
    }).toList();

    final targetFutureItems = _futureItems.where((i) {
      if (_targetFilter == 'MOTHER') return !i.isBaby;
      if (_targetFilter == 'BABY') return i.isBaby;
      return true;
    }).toList();

    final totalHistory = targetHistoryItems.length;
    final totalCurrent = targetCurrentItems.length;
    final totalFuture = targetFutureItems.length;

    final allItemsList = [..._historyItems, ..._currentItems, ..._futureItems];
    final totalAllItems = allItemsList.length;

    final selectedTargetTotal = totalHistory + totalCurrent + totalFuture;
    final selectedTargetCompleted = targetHistoryItems.where((i) => i.completed).length +
        targetCurrentItems.where((i) => i.completed).length +
        targetFutureItems.where((i) => i.completed).length;
    final selectedTargetPending = selectedTargetTotal - selectedTargetCompleted;

    final totalMother = allItemsList.where((i) => !i.isBaby).length;
    final totalBaby = allItemsList.where((i) => i.isBaby).length;
    final hasBothTargets = totalMother > 0 && totalBaby > 0;

    return Padding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).viewInsets.bottom,
        left: 20,
        right: 20,
        top: 16,
      ),
      child: SafeArea(
        child: SizedBox(
          height: MediaQuery.of(context).size.height * 0.90,
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

              // Target Scope Selector: Cho phép mẹ chọn gửi checklist của mẹ hoặc của bé
              if (hasBothTargets) ...[
                const Row(
                  children: [
                    Icon(Icons.tune_rounded, size: 14, color: _textMuted),
                    SizedBox(width: 4),
                    Text(
                      'Chọn nội dung gửi:',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 11,
                        fontWeight: FontWeight.w600,
                        color: _textMuted,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  child: Row(
                    children: [
                      _buildTargetChip('ALL', 'Tất cả ($totalAllItems)', Icons.people_outline_rounded),
                      const SizedBox(width: 6),
                      _buildTargetChip('MOTHER', 'Của mẹ ($totalMother)', Icons.person_outline_rounded),
                      const SizedBox(width: 6),
                      _buildTargetChip('BABY', 'Dành cho bé ($totalBaby)', Icons.child_care_rounded),
                    ],
                  ),
                ),
                const SizedBox(height: 8),
              ],

              // Scope Info Bar (thông tin nội dung sẽ được gửi)
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                decoration: BoxDecoration(
                  color: _targetFilter == 'BABY' ? const Color(0xFFFEF3C7) : const Color(0xFFF7F2F0),
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(
                    color: _targetFilter == 'BABY' ? const Color(0xFFFDE68A) : const Color(0xFFE8D5CE),
                  ),
                ),
                child: Row(
                  children: [
                    Icon(
                      _targetFilter == 'BABY'
                          ? Icons.child_care_rounded
                          : (_targetFilter == 'MOTHER'
                              ? Icons.person_outline_rounded
                              : Icons.all_inclusive_rounded),
                      size: 16,
                      color: _targetFilter == 'BABY' ? const Color(0xFFD97706) : _primary,
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        _targetFilter == 'BABY'
                            ? 'Sẽ gửi $selectedTargetTotal việc chăm sóc bé (Đã xong: $selectedTargetCompleted, Chờ làm: $selectedTargetPending)'
                            : (_targetFilter == 'MOTHER'
                                ? 'Sẽ gửi $selectedTargetTotal việc của mẹ (Đã xong: $selectedTargetCompleted, Chờ làm: $selectedTargetPending)'
                                : 'Mặc định gửi toàn bộ $selectedTargetTotal việc Mẹ & Bé (Đã xong: $selectedTargetCompleted, Chờ làm: $selectedTargetPending)'),
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 12,
                          fontWeight: FontWeight.w700,
                          color: _targetFilter == 'BABY' ? const Color(0xFFB45309) : _primary,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 8),

              // Filter: Trạng thái xem trước
              SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: Row(
                  children: [
                    _buildStatusChip('ALL', 'Tất cả ($selectedTargetTotal)'),
                    const SizedBox(width: 6),
                    _buildStatusChip('COMPLETED', 'Đã xong ($selectedTargetCompleted)'),
                    const SizedBox(width: 6),
                    _buildStatusChip('PENDING', 'Chờ làm ($selectedTargetPending)'),
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

              // Send button: Gửi toàn bộ hoặc theo đối tượng đã chọn
              FilledButton.icon(
                key: const Key('share-all-checklist-btn'),
                onPressed: _loading ? null : _onConfirm,
                icon: const Icon(Icons.send_rounded, size: 18),
                label: Text(
                  _targetFilter == 'BABY'
                      ? 'Chia sẻ việc chăm sóc bé ($selectedTargetTotal việc)'
                      : (_targetFilter == 'MOTHER'
                          ? 'Chia sẻ việc của mẹ ($selectedTargetTotal việc)'
                          : 'Chia sẻ toàn bộ việc cần làm ($totalAllItems việc)'),
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontWeight: FontWeight.bold,
                    fontSize: 13,
                  ),
                ),
                style: FilledButton.styleFrom(
                  backgroundColor: _targetFilter == 'BABY'
                      ? const Color(0xFFD97706)
                      : _primary,
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

  Widget _buildTargetChip(String key, String label, IconData icon) {
    final selected = _targetFilter == key;
    return ChoiceChip(
      avatar: Icon(
        icon,
        size: 14,
        color: selected
            ? Colors.white
            : (key == 'BABY' ? const Color(0xFFD97706) : _primary),
      ),
      label: Text(label),
      selected: selected,
      selectedColor: key == 'BABY' ? const Color(0xFFD97706) : _primary,
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
          color: selected
              ? (key == 'BABY' ? const Color(0xFFD97706) : _primary)
              : const Color(0xFFE8D5CE),
        ),
      ),
      onSelected: (val) {
        if (val) {
          setState(() => _targetFilter = key);
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
                          // Badge phân loại Dành cho bé
                          if (item.isBaby)
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                              margin: const EdgeInsets.only(right: 6),
                              decoration: BoxDecoration(
                                color: const Color(0xFFFEF3C7),
                                borderRadius: BorderRadius.circular(4),
                                border: Border.all(color: const Color(0xFFFDE68A)),
                              ),
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  const Icon(Icons.child_care_rounded, size: 10, color: Color(0xFFD97706)),
                                  const SizedBox(width: 2),
                                  Text(
                                    item.babyLabel != null && item.babyLabel!.isNotEmpty
                                        ? 'Dành cho bé · ${item.babyLabel}'
                                        : 'Dành cho bé',
                                    style: const TextStyle(
                                      fontFamily: 'Lexend',
                                      fontSize: 9,
                                      fontWeight: FontWeight.bold,
                                      color: Color(0xFFB45309),
                                    ),
                                  ),
                                ],
                              ),
                            ),
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
