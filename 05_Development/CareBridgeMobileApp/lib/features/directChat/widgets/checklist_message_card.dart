import 'dart:convert';
import 'package:flutter/material.dart';
import '../../checklist/services/user_checklist_service.dart';
import '../../reminder/services/today_task_service.dart';

class ChecklistItemShareData {
  final String text;
  final bool completed;
  final String? category;
  final String? timeLabel; // ví dụ: "Tuần 12", "Đã xong", "Tuần 32 (Sắp tới)"
  final String? origin;
  final String? createdBy;
  final bool isExpertCustom;
  final String? doctorNote;
  final String? sourceUrl;
  final String? supportFunction;

  const ChecklistItemShareData({
    required this.text,
    this.completed = false,
    this.category,
    this.timeLabel,
    this.origin,
    this.createdBy,
    this.isExpertCustom = false,
    this.doctorNote,
    this.sourceUrl,
    this.supportFunction,
  });

  factory ChecklistItemShareData.fromJson(Map<String, dynamic> json) =>
      ChecklistItemShareData(
        text: json['text'] as String? ?? '',
        completed: json['completed'] as bool? ?? false,
        category: json['category'] as String?,
        timeLabel: json['timeLabel'] as String?,
        origin: json['origin'] as String?,
        createdBy: json['createdBy'] as String?,
        isExpertCustom: json['isExpertCustom'] as bool? ??
            (json['origin'] == 'EXPERT' || json['createdBy'] == 'EXPERT'),
        doctorNote: json['doctorNote'] as String?,
        sourceUrl: json['sourceUrl'] as String?,
        supportFunction: json['supportFunction'] as String?,
      );

  Map<String, dynamic> toJson() => {
    'text': text,
    'completed': completed,
    'category': category,
    if (timeLabel != null) 'timeLabel': timeLabel,
    if (origin != null) 'origin': origin,
    if (createdBy != null) 'createdBy': createdBy,
    'isExpertCustom': isExpertCustom,
    if (doctorNote != null) 'doctorNote': doctorNote,
    if (sourceUrl != null) 'sourceUrl': sourceUrl,
    if (supportFunction != null) 'supportFunction': supportFunction,
  };
}

class ChecklistShareData {
  final String title;
  final int? gestationalWeek;
  final String? journeyId;
  final bool isLiveSync;
  final int completedCount;
  final int totalCount;
  final int progressPercent;
  final String? note;
  final List<ChecklistItemShareData> historyItems;
  final List<ChecklistItemShareData> currentItems;
  final List<ChecklistItemShareData> futureItems;

  ChecklistShareData({
    this.title = 'Hồ sơ Checklist Toàn diện (Lịch sử & Tương lai)',
    this.gestationalWeek,
    this.journeyId,
    this.isLiveSync = true,
    required this.completedCount,
    required this.totalCount,
    required this.progressPercent,
    this.note,
    this.historyItems = const [],
    List<ChecklistItemShareData> currentItems = const [],
    this.futureItems = const [],
    List<ChecklistItemShareData>? items,
  }) : currentItems = (items != null && items.isNotEmpty && currentItems.isEmpty)
            ? items
            : currentItems;

  List<ChecklistItemShareData> get allItems => [
    ...historyItems,
    ...currentItems,
    ...futureItems,
  ];

  static const String tag = '[CAREBRIDGE_CHECKLIST_SHARE]';

  static bool isChecklistShareMessage(String? body) {
    if (body == null) return false;
    return body.trim().startsWith(tag);
  }

  static ChecklistShareData? parse(String? body) {
    if (body == null || !isChecklistShareMessage(body)) return null;
    try {
      final jsonStr = body.replaceFirst(tag, '').trim();
      final decoded = jsonDecode(jsonStr) as Map<String, dynamic>;

      final historyList = (decoded['historyItems'] as List? ?? [])
          .map((item) => ChecklistItemShareData.fromJson(item as Map<String, dynamic>))
          .toList();

      final currentList = (decoded['currentItems'] as List? ?? decoded['items'] as List? ?? [])
          .map((item) => ChecklistItemShareData.fromJson(item as Map<String, dynamic>))
          .toList();

      final futureList = (decoded['futureItems'] as List? ?? [])
          .map((item) => ChecklistItemShareData.fromJson(item as Map<String, dynamic>))
          .toList();

      final total = (decoded['totalCount'] as num?)?.toInt() ??
          (historyList.length + currentList.length + futureList.length);
      final completed = (decoded['completedCount'] as num?)?.toInt() ??
          historyList.where((i) => i.completed).length +
              currentList.where((i) => i.completed).length;
      final percent = total > 0 ? ((completed / total) * 100).round() : 0;

      return ChecklistShareData(
        title: decoded['title'] as String? ?? 'Hồ sơ Checklist Toàn diện',
        gestationalWeek: (decoded['gestationalWeek'] as num?)?.toInt(),
        journeyId: decoded['journeyId'] as String?,
        isLiveSync: decoded['isLiveSync'] as bool? ?? true,
        completedCount: completed,
        totalCount: total,
        progressPercent: (decoded['progressPercent'] as num?)?.toInt() ?? percent,
        note: decoded['note'] as String?,
        historyItems: historyList,
        currentItems: currentList,
        futureItems: futureList,
      );
    } catch (_) {
      return null;
    }
  }

  String serialize() => '$tag\n${jsonEncode({
    'title': title,
    'gestationalWeek': gestationalWeek,
    'journeyId': journeyId,
    'isLiveSync': isLiveSync,
    'completedCount': completedCount,
    'totalCount': totalCount,
    'progressPercent': progressPercent,
    'note': note,
    'historyItems': historyItems.map((i) => i.toJson()).toList(),
    'currentItems': currentItems.map((i) => i.toJson()).toList(),
    'futureItems': futureItems.map((i) => i.toJson()).toList(),
  })}';
}

class ChecklistMessageCard extends StatefulWidget {
  const ChecklistMessageCard({
    super.key,
    required this.data,
    required this.isOwnMessage,
  });

  final ChecklistShareData data;
  final bool isOwnMessage;

  @override
  State<ChecklistMessageCard> createState() => _ChecklistMessageCardState();
}

class _ChecklistMessageCardState extends State<ChecklistMessageCard> {
  late List<ChecklistItemShareData> _historyItems;
  late List<ChecklistItemShareData> _currentItems;
  late List<ChecklistItemShareData> _futureItems;
  bool _isRefreshing = false;

  @override
  void initState() {
    super.initState();
    _historyItems = List.from(widget.data.historyItems);
    _currentItems = List.from(widget.data.currentItems);
    _futureItems = List.from(widget.data.futureItems);
    if (widget.data.isLiveSync == true) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) _refreshChecklistStatus();
      });
    }
  }

  @override
  void didUpdateWidget(covariant ChecklistMessageCard oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.data != oldWidget.data) {
      _historyItems = List.from(widget.data.historyItems);
      _currentItems = List.from(widget.data.currentItems);
      _futureItems = List.from(widget.data.futureItems);
    }
    if (widget.data.isLiveSync == true) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) _refreshChecklistStatus();
      });
    }
  }

  Future<void> _refreshChecklistStatus() async {
    if (_isRefreshing) return;
    setState(() => _isRefreshing = true);
    try {
      final snapshot = await TodayTaskService.instance.loadToday();
      final liveTasks = snapshot.sections.all.toList();
      if (liveTasks.isNotEmpty && mounted) {
        final taskMap = {
          for (final t in liveTasks) t.title.trim().toLowerCase(): t.isCompleted
        };
        setState(() {
          _currentItems = _currentItems.map((i) {
            final key = i.text.trim().toLowerCase();
            if (taskMap.containsKey(key)) {
              return ChecklistItemShareData(
                text: i.text,
                completed: taskMap[key]!,
                category: i.category,
                timeLabel: i.timeLabel,
                origin: i.origin,
                createdBy: i.createdBy,
                isExpertCustom: i.isExpertCustom,
                doctorNote: i.doctorNote,
              );
            }
            return i;
          }).toList();
          _isRefreshing = false;
        });
        return;
      }
    } catch (_) {}

    try {
      final serverItems = await UserChecklistService.instance.listItems();
      if (serverItems.isNotEmpty && mounted) {
        final serverMap = {
          for (final item in serverItems) item.itemText.trim().toLowerCase(): item.completed
        };
        setState(() {
          _currentItems = _currentItems.map((i) {
            final key = i.text.trim().toLowerCase();
            if (serverMap.containsKey(key)) {
              return ChecklistItemShareData(
                text: i.text,
                completed: serverMap[key]!,
                category: i.category,
                timeLabel: i.timeLabel,
                origin: i.origin,
                createdBy: i.createdBy,
                isExpertCustom: i.isExpertCustom,
                doctorNote: i.doctorNote,
              );
            }
            return i;
          }).toList();
          _isRefreshing = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() => _isRefreshing = false);
      }
    }
  }

  int get _liveCompletedCount {
    if (_historyItems.isEmpty && _futureItems.isEmpty && _currentItems.length < widget.data.totalCount) {
      return widget.data.completedCount;
    }
    return _historyItems.where((i) => i.completed).length +
        _currentItems.where((i) => i.completed).length +
        _futureItems.where((i) => i.completed).length;
  }

  int get _liveTotalCount {
    if (_historyItems.isEmpty && _futureItems.isEmpty && _currentItems.length < widget.data.totalCount) {
      return widget.data.totalCount;
    }
    final total = _historyItems.length + _currentItems.length + _futureItems.length;
    return total > 0 ? total : widget.data.totalCount;
  }

  int get _livePercent {
    if (_historyItems.isEmpty && _futureItems.isEmpty && _currentItems.length < widget.data.totalCount) {
      return widget.data.progressPercent;
    }
    final total = _liveTotalCount;
    if (total == 0) return widget.data.progressPercent;
    return ((_liveCompletedCount / total) * 100).round();
  }

  List<ChecklistItemShareData> get _liveAllItems => [
    ..._historyItems,
    ..._currentItems,
    ..._futureItems,
  ];

  void _showFullDetailModal(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (ctx) => DefaultTabController(
        length: 3,
        child: Container(
          height: MediaQuery.of(context).size.height * 0.8,
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
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
              Row(
                children: [
                  const Icon(Icons.assignment_turned_in_rounded, color: Color(0xFF845143), size: 24),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      widget.data.title,
                      style: const TextStyle(
                        fontFamily: 'Quicksand',
                        fontWeight: FontWeight.w800,
                        fontSize: 16,
                        color: Color(0xFF2C2523),
                      ),
                    ),
                  ),
                ],
              ),
              if (widget.data.gestationalWeek != null)
                Padding(
                  padding: const EdgeInsets.only(top: 4),
                  child: Text(
                    'Giai đoạn theo dõi: Tuần thai thứ ${widget.data.gestationalWeek}',
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      color: Color(0xFF7A6F6C),
                    ),
                  ),
                ),
              const SizedBox(height: 12),
              TabBar(
                labelColor: const Color(0xFF845143),
                unselectedLabelColor: const Color(0xFF7A6F6C),
                indicatorColor: const Color(0xFF845143),
                labelStyle: const TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.bold, fontSize: 12),
                unselectedLabelStyle: const TextStyle(fontFamily: 'Lexend', fontSize: 12),
                tabs: [
                  Tab(text: 'Đã làm (${_historyItems.length})'),
                  Tab(text: 'Hiện tại (${_currentItems.length})'),
                  Tab(text: 'Tương lai (${_futureItems.length})'),
                ],
              ),
              const SizedBox(height: 8),
              Expanded(
                child: TabBarView(
                  children: [
                    _buildItemList(_historyItems, emptyText: 'Chưa có lịch sử checklist nào.'),
                    _buildItemList(_currentItems, emptyText: 'Không có checklist cho tuần này.'),
                    _buildItemList(_futureItems, emptyText: 'Không có kế hoạch tương lai.'),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildItemList(List<ChecklistItemShareData> items, {required String emptyText}) {
    if (items.isEmpty) {
      return Center(
        child: Text(
          emptyText,
          style: const TextStyle(fontFamily: 'Lexend', fontSize: 12, color: Color(0xFF9E8E8A)),
        ),
      );
    }
    return ListView.separated(
      padding: const EdgeInsets.symmetric(vertical: 8),
      itemCount: items.length,
      separatorBuilder: (_, _) => const Divider(height: 1, color: Color(0xFFECE4E1)),
      itemBuilder: (ctx, idx) {
        final item = items[idx];
        return Padding(
          padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 4),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Icon(
                item.completed ? Icons.check_circle_rounded : Icons.radio_button_unchecked_rounded,
                size: 18,
                color: item.completed ? const Color(0xFF2E7D32) : const Color(0xFFC98C7B),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Wrap(
                      crossAxisAlignment: WrapCrossAlignment.center,
                      spacing: 6,
                      runSpacing: 2,
                      children: [
                        Text(
                          item.text,
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 13,
                            fontWeight: FontWeight.w500,
                            color: item.completed ? const Color(0xFF6E605D) : const Color(0xFF2C2523),
                          ),
                        ),
                        if (item.isExpertCustom || item.origin == 'EXPERT' || item.createdBy == 'EXPERT')
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1.5),
                            decoration: BoxDecoration(
                              color: const Color(0xFFE0F2F1),
                              borderRadius: BorderRadius.circular(4),
                              border: Border.all(color: const Color(0xFF80CBC4)),
                            ),
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: const [
                                Icon(Icons.medical_services_outlined, size: 10, color: Color(0xFF00695C)),
                                SizedBox(width: 3),
                                Text(
                                  'Bác sĩ chỉ định',
                                  style: TextStyle(
                                    fontFamily: 'Lexend',
                                    fontSize: 9,
                                    fontWeight: FontWeight.bold,
                                    color: Color(0xFF00695C),
                                  ),
                                ),
                              ],
                            ),
                          )
                        else if (item.origin == 'USER' || item.createdBy == 'USER')
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1.5),
                            decoration: BoxDecoration(
                              color: const Color(0xFFF3E5F5),
                              borderRadius: BorderRadius.circular(4),
                              border: Border.all(color: const Color(0xFFCE93D8)),
                            ),
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: const [
                                Icon(Icons.person_outline_rounded, size: 10, color: Color(0xFF6A1B9A)),
                                SizedBox(width: 3),
                                Text(
                                  'Mẹ tự tạo',
                                  style: TextStyle(
                                    fontFamily: 'Lexend',
                                    fontSize: 9,
                                    fontWeight: FontWeight.bold,
                                    color: Color(0xFF6A1B9A),
                                  ),
                                ),
                              ],
                            ),
                          )
                        else
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1.5),
                            decoration: BoxDecoration(
                              color: const Color(0xFFE0F2FE),
                              borderRadius: BorderRadius.circular(4),
                              border: Border.all(color: const Color(0xFFBAE6FD)),
                            ),
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: const [
                                Icon(Icons.auto_awesome_rounded, size: 10, color: Color(0xFF0284C7)),
                                SizedBox(width: 3),
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
                      ],
                    ),
                    if (item.category != null || item.timeLabel != null)
                      Padding(
                        padding: const EdgeInsets.only(top: 2),
                        child: Text(
                          [item.timeLabel, item.category].where((e) => e != null).join(' · '),
                          style: const TextStyle(fontFamily: 'Lexend', fontSize: 10, color: Color(0xFF9E8E8A)),
                        ),
                      ),
                    if (item.doctorNote != null && item.doctorNote!.trim().isNotEmpty)
                      Padding(
                        padding: const EdgeInsets.only(top: 2),
                        child: Text(
                          '💬 Lời dặn: ${item.doctorNote}',
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 10,
                            fontStyle: FontStyle.italic,
                            color: Color(0xFF00695C),
                          ),
                        ),
                      ),
                  ],
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    const primary = Color(0xFF845143);
    const surface = Colors.white;
    const textDark = Color(0xFF2C2523);
    const textMuted = Color(0xFF7A6F6C);

    final historyCount = _historyItems.length;
    final currentCount = _currentItems.length;
    final futureCount = _futureItems.length;

    return Container(
      width: 300,
      decoration: BoxDecoration(
        color: surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: widget.isOwnMessage ? Colors.white70 : const Color(0xFFE8D5CE),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        mainAxisSize: MainAxisSize.min,
        children: [
          // Header
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
            decoration: BoxDecoration(
              color: primary.withValues(alpha: 0.08),
              borderRadius: const BorderRadius.vertical(top: Radius.circular(15)),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(6),
                      decoration: BoxDecoration(
                        color: primary,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: const Icon(
                        Icons.checklist_rtl_rounded,
                        color: Colors.white,
                        size: 18,
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Expanded(
                                child: Text(
                                  widget.data.title,
                                  style: const TextStyle(
                                    fontFamily: 'Lexend',
                                    fontWeight: FontWeight.w700,
                                    fontSize: 13,
                                    color: textDark,
                                  ),
                                ),
                              ),
                              if (widget.data.isLiveSync)
                                InkWell(
                                  onTap: _refreshChecklistStatus,
                                  borderRadius: BorderRadius.circular(6),
                                  child: Container(
                                    padding: const EdgeInsets.symmetric(
                                      horizontal: 5,
                                      vertical: 1.5,
                                    ),
                                    decoration: BoxDecoration(
                                      color: const Color(0xFFE8F5E9),
                                      borderRadius: BorderRadius.circular(6),
                                    ),
                                    child: Row(
                                      mainAxisSize: MainAxisSize.min,
                                      children: [
                                        _isRefreshing
                                            ? const SizedBox(
                                                width: 10,
                                                height: 10,
                                                child: CircularProgressIndicator(
                                                  strokeWidth: 1.5,
                                                  color: Color(0xFF2E7D32),
                                                ),
                                              )
                                            : const Icon(
                                                Icons.bolt_rounded,
                                                size: 11,
                                                color: Color(0xFF2E7D32),
                                              ),
                                        const SizedBox(width: 2),
                                        const Text(
                                          'Live',
                                          style: TextStyle(
                                            fontFamily: 'Lexend',
                                            fontSize: 9,
                                            fontWeight: FontWeight.bold,
                                            color: Color(0xFF2E7D32),
                                          ),
                                        ),
                                      ],
                                    ),
                                  ),
                                ),
                            ],
                          ),
                          if (widget.data.gestationalWeek != null)
                            Text(
                              'Giai đoạn: Tuần thai ${widget.data.gestationalWeek}',
                              style: const TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 11,
                                color: textMuted,
                              ),
                            ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                // Badge category pills
                Wrap(
                  spacing: 4,
                  runSpacing: 4,
                  children: [
                    if (historyCount > 0)
                      _buildPill('Đã xong: $historyCount', const Color(0xFFE8F5E9), const Color(0xFF2E7D32)),
                    if (currentCount > 0)
                      _buildPill('Hiện tại: $currentCount', const Color(0xFFE3F2FD), const Color(0xFF1565C0)),
                    if (futureCount > 0)
                      _buildPill('Tương lai: $futureCount', const Color(0xFFF3E5F5), const Color(0xFF7B1FA2)),
                  ],
                ),
                const SizedBox(height: 8),
                // Progress bar
                Row(
                  children: [
                    Expanded(
                      child: ClipRRect(
                        borderRadius: BorderRadius.circular(4),
                        child: LinearProgressIndicator(
                          value: _liveTotalCount > 0 ? _liveCompletedCount / _liveTotalCount : 0.0,
                          backgroundColor: const Color(0xFFE8D5CE),
                          valueColor: const AlwaysStoppedAnimation<Color>(Color(0xFF2E7D32)),
                          minHeight: 6,
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      '$_liveCompletedCount/$_liveTotalCount ($_livePercent%)',
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 11,
                        fontWeight: FontWeight.w700,
                        color: Color(0xFF2E7D32),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),

          // Preview items list (up to 4 items)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
            child: Column(
              children: [
                ..._liveAllItems.take(4).map((item) {
                  return Padding(
                    padding: const EdgeInsets.symmetric(vertical: 3),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Icon(
                          item.completed ? Icons.check_circle_rounded : Icons.radio_button_unchecked_rounded,
                          size: 15,
                          color: item.completed ? const Color(0xFF2E7D32) : const Color(0xFFC98C7B),
                        ),
                        const SizedBox(width: 6),
                        Expanded(
                          child: Row(
                            children: [
                              Expanded(
                                child: Text(
                                  item.text,
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                  style: TextStyle(
                                    fontFamily: 'Lexend',
                                    fontSize: 12,
                                    color: item.completed ? textMuted : textDark,
                                  ),
                                ),
                              ),
                              if (item.isExpertCustom || item.origin == 'EXPERT' || item.createdBy == 'EXPERT')
                                Container(
                                  margin: const EdgeInsets.only(left: 4),
                                  padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 1),
                                  decoration: BoxDecoration(
                                    color: const Color(0xFFE0F2F1),
                                    borderRadius: BorderRadius.circular(3),
                                    border: Border.all(color: const Color(0xFF80CBC4)),
                                  ),
                                  child: const Text(
                                    '🩺 BS chỉ định',
                                    style: TextStyle(
                                      fontFamily: 'Lexend',
                                      fontSize: 8.5,
                                      fontWeight: FontWeight.bold,
                                      color: Color(0xFF00695C),
                                    ),
                                  ),
                                ),
                              if (item.origin == 'USER' || item.createdBy == 'USER')
                                Container(
                                  margin: const EdgeInsets.only(left: 4),
                                  padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 1),
                                  decoration: BoxDecoration(
                                    color: const Color(0xFFF3E5F5),
                                    borderRadius: BorderRadius.circular(3),
                                    border: Border.all(color: const Color(0xFFCE93D8)),
                                  ),
                                  child: const Text(
                                    '👤 Mẹ tạo',
                                    style: TextStyle(
                                      fontFamily: 'Lexend',
                                      fontSize: 8.5,
                                      fontWeight: FontWeight.bold,
                                      color: Color(0xFF6A1B9A),
                                    ),
                                  ),
                                ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  );
                }),
                if (_liveAllItems.length > 4)
                  Padding(
                    padding: const EdgeInsets.only(top: 4),
                    child: Text(
                      '+ thêm ${_liveAllItems.length - 4} việc khác...',
                      style: const TextStyle(fontFamily: 'Lexend', fontSize: 11, color: textMuted, fontStyle: FontStyle.italic),
                    ),
                  ),
              ],
            ),
          ),

          // Note if present
          if (widget.data.note != null && widget.data.note!.trim().isNotEmpty)
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              margin: const EdgeInsets.only(left: 10, right: 10, bottom: 8),
              decoration: BoxDecoration(
                color: const Color(0xFFF9F5F4),
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: const Color(0xFFECE4E1)),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(Icons.chat_bubble_outline_rounded, size: 14, color: textMuted),
                  const SizedBox(width: 6),
                  Expanded(
                    child: Text(
                      widget.data.note!,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 11,
                        color: textDark,
                        fontStyle: FontStyle.italic,
                      ),
                    ),
                  ),
                ],
              ),
            ),

          // View detail action button
          InkWell(
            onTap: () => _showFullDetailModal(context),
            borderRadius: const BorderRadius.vertical(bottom: Radius.circular(15)),
            child: Container(
              padding: const EdgeInsets.symmetric(vertical: 10),
              decoration: const BoxDecoration(
                border: Border(top: BorderSide(color: Color(0xFFECE4E1))),
              ),
              alignment: Alignment.center,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                mainAxisSize: MainAxisSize.min,
                children: const [
                  Flexible(
                    child: Text(
                      'Xem Lịch sử & Tương lai',
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        fontWeight: FontWeight.bold,
                        color: primary,
                      ),
                    ),
                  ),
                  SizedBox(width: 4),
                  Icon(Icons.arrow_forward_ios_rounded, size: 12, color: primary),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPill(String text, Color bg, Color textCol) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(
        text,
        style: TextStyle(
          fontFamily: 'Lexend',
          fontSize: 10,
          fontWeight: FontWeight.bold,
          color: textCol,
        ),
      ),
    );
  }
}
