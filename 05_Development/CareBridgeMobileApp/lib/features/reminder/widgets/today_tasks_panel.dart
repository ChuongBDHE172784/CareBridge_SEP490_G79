import 'package:flutter/material.dart';
import 'package:uuid/uuid.dart';

import '../../checklist/services/user_checklist_service.dart';
import '../models/reminder_model.dart';
import '../models/today_task_model.dart';
import '../services/today_task_service.dart';

enum TodayTasksAudience { mother, family }

enum TodayTasksLayout { timeBuckets, sourceGroups }

class TodayTasksPanelController {
  Object? _owner;
  Future<void> Function()? _refresh;
  Future<void> Function()? _clear;

  Future<void> refresh() => _refresh?.call() ?? Future<void>.value();
  Future<void> clear() => _clear?.call() ?? Future<void>.value();

  void _attach(
    Object owner,
    Future<void> Function() refresh,
    Future<void> Function() clear,
  ) {
    _owner = owner;
    _refresh = refresh;
    _clear = clear;
  }

  void _detach(Object owner) {
    if (identical(_owner, owner)) {
      _owner = null;
      _refresh = null;
      _clear = null;
    }
  }
}

class TodayTasksPanel extends StatefulWidget {
  const TodayTasksPanel({
    super.key,
    this.service,
    this.checklistService,
    this.audience = TodayTasksAudience.mother,
    this.layout = TodayTasksLayout.timeBuckets,
    this.showHeading = true,
    this.controller,
    this.headingAction,
  });

  final TodayTaskService? service;
  final UserChecklistService? checklistService;
  final TodayTasksAudience audience;
  final TodayTasksLayout layout;
  final bool showHeading;
  final TodayTasksPanelController? controller;
  final Widget? headingAction;

  @override
  State<TodayTasksPanel> createState() => _TodayTasksPanelState();
}

class _TodayTasksPanelState extends State<TodayTasksPanel> {
  static const _text = Color(0xFF5A463F);

  late TodayTaskService _service;
  late UserChecklistService _checklistService;
  TodayTasksSnapshot? _snapshot;
  TodayTasksFailure? _failure;
  bool _loading = true;
  int _loadGeneration = 0;
  final Set<String> _acting = {};
  String? _announcement;
  bool _advancingSequence = false;
  String? _sequenceRequestId;

  int _selectedSourceTabIndex = 0;

  int get _activeSourceTabIndex =>
      (_selectedSourceTabIndex as dynamic) is int ? _selectedSourceTabIndex : 0;

  @override
  void initState() {
    super.initState();
    _service = widget.service ?? TodayTaskService.instance;
    _checklistService =
        widget.checklistService ?? UserChecklistService.instance;
    widget.controller?._attach(this, _load, _clear);
    _load();
  }

  @override
  void didUpdateWidget(covariant TodayTasksPanel oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.service != widget.service) {
      _service = widget.service ?? TodayTaskService.instance;
      _snapshot = null;
      _failure = null;
      _load();
    }
    if (oldWidget.checklistService != widget.checklistService) {
      _checklistService =
          widget.checklistService ?? UserChecklistService.instance;
    }
    if (oldWidget.controller != widget.controller) {
      oldWidget.controller?._detach(this);
      widget.controller?._attach(this, _load, _clear);
    }
  }

  @override
  void dispose() {
    widget.controller?._detach(this);
    super.dispose();
  }

  Future<void> _load() async {
    final generation = ++_loadGeneration;
    if (mounted) {
      setState(() {
        _loading = _snapshot == null;
        _failure = null;
      });
    }
    try {
      final snapshot = await _service.loadToday();
      if (!mounted || generation != _loadGeneration) return;
      setState(() {
        _snapshot = snapshot;
        _loading = false;
      });
    } on TodayTasksFailure catch (failure) {
      if (!mounted || generation != _loadGeneration) return;
      setState(() {
        _failure = failure;
        _loading = false;
      });
    }
  }

  Future<void> _clear() async {
    ++_loadGeneration;
    if (!mounted) return;
    setState(() {
      _snapshot = null;
      _failure = null;
      _loading = true;
      _announcement = null;
      _acting.clear();
      _advancingSequence = false;
      _sequenceRequestId = null;
    });
  }

  Future<void> _act(TodayTask task, TodayTaskAction action) async {
    if (_acting.contains(task.id)) return;
    setState(() => _acting.add(task.id));
    try {
      await _service.performAction(
        taskKind: task.kind,
        taskId: task.id,
        action: action,
        reason: action == TodayTaskAction.skip
            ? TodayTaskSkipReason.userChoice
            : null,
      );
      await _load();
      if (!mounted) return;
      setState(() {
        _announcement = switch (action) {
          TodayTaskAction.complete => 'Đã hoàn tất ${task.title}',
          TodayTaskAction.reopen => 'Đã chuyển ${task.title} về chưa hoàn tất',
          TodayTaskAction.skip => 'Đã bỏ qua ${task.title}',
        };
      });
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Không thể cập nhật công việc. Vui lòng thử lại.'),
          backgroundColor: _text,
          behavior: SnackBarBehavior.floating,
          shape: StadiumBorder(),
        ),
      );
    } finally {
      if (mounted) setState(() => _acting.remove(task.id));
    }
  }

  Future<void> _delete(TodayTask task) async {
    if (_acting.contains(task.id) ||
        !task.isChecklist ||
        task.origin != TodayTaskOrigin.userCreated) {
      return;
    }
    setState(() => _acting.add(task.id));
    try {
      await _checklistService.deleteItem(task.id);
      await _load();
      if (!mounted) return;
      setState(() {
        _announcement = 'Đã xoá ${task.title}';
      });
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Không thể xoá công việc. Vui lòng thử lại.'),
          backgroundColor: _text,
          behavior: SnackBarBehavior.floating,
          shape: StadiumBorder(),
        ),
      );
    } finally {
      if (mounted) setState(() => _acting.remove(task.id));
    }
  }

  Future<void> _advanceSequence(TodaySequenceProjection sequence) async {
    final instanceId = sequence.currentInstanceId;
    if (instanceId == null || _advancingSequence) return;
    final requestId = _sequenceRequestId ??= _newRequestId();
    setState(() => _advancingSequence = true);
    try {
      await _service.advanceSequence(
        currentInstanceId: instanceId,
        clientRequestId: requestId,
      );
      await _load();
      if (!mounted) return;
      setState(() {
        // Keep the same idempotency key if the follow-up refresh failed; a
        // retry must replay the committed advance instead of issuing a new
        // command against the now-historical predecessor.
        if (_failure == null) {
          _sequenceRequestId = null;
          _announcement = 'Đã chuyển sang checklist tiếp theo';
        }
      });
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Không thể chuyển checklist. Vui lòng thử lại.'),
          backgroundColor: _text,
          behavior: SnackBarBehavior.floating,
          shape: StadiumBorder(),
        ),
      );
    } finally {
      if (mounted) setState(() => _advancingSequence = false);
    }
  }

  String _newRequestId() => const Uuid().v4();

  List<TodayTask> get _sourceGroupedTasks {
    if (_snapshot == null) return const [];
    final tasks = _snapshot!.sections.all
        .where((task) => task.type != ReminderType.appointment)
        .toList();
    tasks.sort(_compareNewestFirst);
    return tasks;
  }

  static int _compareNewestFirst(TodayTask left, TodayTask right) {
    final leftTime = left.dueAt ?? left.scheduledAt;
    final rightTime = right.dueAt ?? right.scheduledAt;
    if (leftTime == null && rightTime != null) return 1;
    if (leftTime != null && rightTime == null) return -1;
    if (leftTime != null && rightTime != null) {
      final timeOrder = rightTime.compareTo(leftTime);
      if (timeOrder != 0) return timeOrder;
    }
    return left.id.compareTo(right.id);
  }

  @override
  Widget build(BuildContext context) {
    final activeTabIndex = _activeSourceTabIndex;
    final sourceGroupedTasks = widget.layout == TodayTasksLayout.sourceGroups
        ? _sourceGroupedTasks
        : const <TodayTask>[];
    final hasVisibleTasks = widget.layout == TodayTasksLayout.sourceGroups
        ? sourceGroupedTasks.isNotEmpty
        : (_snapshot?.totalCount ?? 0) > 0 || _snapshot?.sequence != null;

    final systemTasks = sourceGroupedTasks
        .where((task) => task.origin == TodayTaskOrigin.systemTemplate)
        .toList(growable: false);
    final userTasks = sourceGroupedTasks
        .where((task) => task.origin != TodayTaskOrigin.systemTemplate)
        .toList(growable: false);

    return Semantics(
      container: true,
      label: widget.audience == TodayTasksAudience.family
          ? 'Việc cần làm của gia đình'
          : 'Việc cần làm của tôi',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (_announcement != null)
            Semantics(
              key: const Key('today-action-result'),
              liveRegion: true,
              label: _announcement,
              child: const SizedBox.shrink(),
            ),
          if (widget.showHeading) ...[
            Row(
              children: [
                const _RoundIcon(icon: Icons.today_rounded),
                const SizedBox(width: 12),
                const Expanded(
                  child: Text(
                    'Việc cần làm',
                    style: TextStyle(
                      fontFamily: 'Quicksand',
                      fontSize: 22,
                      fontWeight: FontWeight.w800,
                      color: _text,
                    ),
                  ),
                ),
                if (widget.headingAction != null) ...[
                  const SizedBox(width: 12),
                  widget.headingAction!,
                ],
              ],
            ),
            const SizedBox(height: 16),
          ],
          if (_loading)
            const _LoadingState()
          else if (_failure != null && _snapshot == null)
            _FailureState(failure: _failure!, onRetry: _load)
          else ...[
            if (_failure?.kind == TodayFailureKind.offline)
              const _OfflineBanner(),
            if (_failure?.kind == TodayFailureKind.retryable)
              _StaleRetryBanner(onRetry: _load),
            if (widget.audience == TodayTasksAudience.mother &&
                _snapshot?.sequence != null)
              _SequencePanel(
                sequence: _snapshot!.sequence!,
                busy: _advancingSequence,
                onAdvance: () => _advanceSequence(_snapshot!.sequence!),
              ),
            if (!hasVisibleTasks)
              const _EmptyState()
            else if (widget.layout == TodayTasksLayout.sourceGroups) ...[
              _SourceTabBar(
                selectedIndex: activeTabIndex,
                systemCount: systemTasks.length,
                userCount: userTasks.length,
                onTabSelected: (index) {
                  setState(() => _selectedSourceTabIndex = index);
                },
              ),
              if (activeTabIndex == 0) ...[
                if (systemTasks.isNotEmpty)
                  _Section(
                    key: const Key('today-system-tasks'),
                    title: 'Gợi ý CareBridge',
                    icon: Icons.auto_awesome_rounded,
                    tasks: systemTasks,
                    acting: _acting,
                    onAction: _act,
                    onDelete: _delete,
                    allowDelete: widget.audience == TodayTasksAudience.mother,
                    showTitle: false,
                  )
                else
                  const _EmptyTabState(
                    icon: Icons.auto_awesome_outlined,
                    message:
                        'Chưa có gợi ý nào từ lộ trình CareBridge cho hôm nay.',
                  ),
              ] else ...[
                if (userTasks.isNotEmpty)
                  _Section(
                    key: const Key('today-user-tasks'),
                    title: 'Việc cá nhân',
                    icon: Icons.person_outline_rounded,
                    tasks: userTasks,
                    acting: _acting,
                    onAction: _act,
                    onDelete: _delete,
                    allowDelete: widget.audience == TodayTasksAudience.mother,
                    showTitle: false,
                  )
                else
                  const _EmptyTabState(
                    icon: Icons.playlist_add_check_rounded,
                    message: 'Bạn chưa tạo công việc cá nhân nào.',
                  ),
              ],
            ] else ...[
              _Section(
                title: 'Quá hạn',
                icon: Icons.error_outline_rounded,
                tasks: _snapshot!.sections.overdue,
                acting: _acting,
                onAction: _act,
                onDelete: _delete,
                allowDelete: widget.audience == TodayTasksAudience.mother,
              ),
              _Section(
                title: 'Hôm nay',
                icon: Icons.wb_sunny_outlined,
                tasks: _snapshot!.sections.today,
                acting: _acting,
                onAction: _act,
                onDelete: _delete,
                allowDelete: widget.audience == TodayTasksAudience.mother,
              ),
              _Section(
                title: '7 ngày tới',
                icon: Icons.date_range_rounded,
                tasks: _snapshot!.sections.upcoming,
                acting: _acting,
                onAction: _act,
                onDelete: _delete,
                allowDelete: widget.audience == TodayTasksAudience.mother,
              ),
              _Section(
                title: 'Chưa xếp lịch',
                icon: Icons.event_busy_rounded,
                tasks: _snapshot!.sections.unscheduled,
                acting: _acting,
                onAction: _act,
                onDelete: _delete,
                allowDelete: widget.audience == TodayTasksAudience.mother,
              ),
            ],
          ],
        ],
      ),
    );
  }
}

class _SequencePanel extends StatelessWidget {
  const _SequencePanel({
    required this.sequence,
    required this.busy,
    required this.onAdvance,
  });

  final TodaySequenceProjection sequence;
  final bool busy;
  final VoidCallback onAdvance;

  @override
  Widget build(BuildContext context) {
    if (sequence.state == TodaySequenceState.configurationBlocked) {
      return Padding(
        padding: const EdgeInsets.only(bottom: 12),
        child: Semantics(
          key: const Key('sequence-configuration-blocked'),
          liveRegion: true,
          child: const _StateCard(
            icon: Icons.warning_amber_rounded,
            message: 'Checklist chuẩn bị đang tạm thời chưa sẵn sàng.',
          ),
        ),
      );
    }
    final position = sequence.currentPosition;
    final total = sequence.totalPositions;
    final name = sequence.currentSetName ?? 'Checklist hiện tại';
    final title = sequence.sequenceComplete
        ? 'Bạn đã hoàn thành toàn bộ checklist chuẩn bị.'
        : sequence.readyToAdvance
        ? 'Bạn đã hoàn thành $name${position == null ? '' : ' (bộ $position)'}.'
        : name;
    final message = sequence.sequenceComplete
        ? 'Bạn đã hoàn thành toàn bộ các bộ checklist.'
        : sequence.readyToAdvance
        ? 'Bạn có thể chuyển sang checklist tiếp theo khi sẵn sàng.'
        : 'Hoàn thành các mục bắt buộc để mở checklist tiếp theo.';
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: Semantics(
        key: const Key('sequence-status-panel'),
        liveRegion: true,
        container: true,
        label: message,
        child: Container(
          width: double.infinity,
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: const Color(0xFFFFF7F1),
            borderRadius: BorderRadius.circular(20),
            border: Border.all(color: const Color(0xFFE8CFC2)),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  const Icon(Icons.route_rounded, color: Color(0xFFC98C7B)),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      title,
                      style: const TextStyle(
                        fontFamily: 'Quicksand',
                        fontWeight: FontWeight.w800,
                        color: Color(0xFF5A463F),
                      ),
                    ),
                  ),
                  if (position != null && total != null)
                    _InfoPill(
                      icon: Icons.layers_outlined,
                      text: '$position/$total',
                    ),
                ],
              ),
              const SizedBox(height: 8),
              Text(
                message,
                style: const TextStyle(
                  fontFamily: 'Quicksand',
                  color: Color(0xFF735E56),
                ),
              ),
              if (sequence.readyToAdvance && sequence.advanceAvailable) ...[
                const SizedBox(height: 12),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton.icon(
                    key: const Key('sequence-advance-button'),
                    onPressed: busy ? null : onAdvance,
                    icon: busy
                        ? const SizedBox.square(
                            dimension: 16,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.arrow_forward_rounded),
                    label: Text(
                      sequence.nextSet?.name == null
                          ? 'Chuyển sang checklist tiếp theo'
                          : 'Chuyển sang ${sequence.nextSet!.name}',
                    ),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class _SourceTabBar extends StatelessWidget {
  const _SourceTabBar({
    required this.selectedIndex,
    required this.systemCount,
    required this.userCount,
    required this.onTabSelected,
  });

  final int selectedIndex;
  final int systemCount;
  final int userCount;
  final ValueChanged<int> onTabSelected;

  int get _safeIndex => (selectedIndex as dynamic) is int ? selectedIndex : 0;
  int get _safeSystemCount => (systemCount as dynamic) is int ? systemCount : 0;
  int get _safeUserCount => (userCount as dynamic) is int ? userCount : 0;

  @override
  Widget build(BuildContext context) {
    final active = _safeIndex;
    return Container(
      height: 50,
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        color: const Color(0xFFF3EBE5),
        borderRadius: BorderRadius.circular(25),
        border: Border.all(
          color: const Color(0xFFE8DDD6).withValues(alpha: .5),
        ),
      ),
      child: Row(
        children: [
          Expanded(
            child: _TabButton(
              key: const Key('tab-system-tasks'),
              title: 'Gợi ý CareBridge',
              count: _safeSystemCount,
              icon: Icons.auto_awesome_rounded,
              isSelected: active == 0,
              onTap: () => onTabSelected(0),
            ),
          ),
          Expanded(
            child: _TabButton(
              key: const Key('tab-user-tasks'),
              title: 'Việc cá nhân',
              count: _safeUserCount,
              icon: Icons.person_outline_rounded,
              isSelected: active == 1,
              onTap: () => onTabSelected(1),
            ),
          ),
        ],
      ),
    );
  }
}

class _TabButton extends StatelessWidget {
  const _TabButton({
    super.key,
    required this.title,
    required this.count,
    required this.icon,
    required this.isSelected,
    required this.onTap,
  });

  final String title;
  final int count;
  final IconData icon;
  final bool isSelected;
  final VoidCallback onTap;

  int get _safeCount => (count as dynamic) is int ? count : 0;

  @override
  Widget build(BuildContext context) {
    const primaryColor = Color(0xFFC98C7B);
    const textActive = Color(0xFF4A3831);
    const textInactive = Color(0xFF8C746A);
    final displayCount = _safeCount;

    return Semantics(
      button: true,
      selected: isSelected,
      label: '$title, $displayCount công việc',
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(21),
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 200),
            curve: Curves.easeInOut,
            padding: const EdgeInsets.symmetric(horizontal: 10),
            decoration: BoxDecoration(
              color: isSelected ? Colors.white : Colors.transparent,
              borderRadius: BorderRadius.circular(21),
              boxShadow: isSelected
                  ? [
                      BoxShadow(
                        color: const Color(0xFF5A463F).withValues(alpha: .1),
                        blurRadius: 12,
                        offset: const Offset(0, 3),
                      ),
                    ]
                  : null,
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(
                  icon,
                  size: 16,
                  color: isSelected ? primaryColor : const Color(0xFF9E877C),
                ),
                const SizedBox(width: 5),
                Flexible(
                  child: Text(
                    title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      fontFamily: 'Quicksand',
                      fontSize: 13.5,
                      fontWeight: isSelected
                          ? FontWeight.w800
                          : FontWeight.w600,
                      color: isSelected ? textActive : textInactive,
                    ),
                  ),
                ),
                if (displayCount > 0) ...[
                  const SizedBox(width: 5),
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 7,
                      vertical: 2,
                    ),
                    decoration: BoxDecoration(
                      color: isSelected
                          ? const Color(0xFFFFECE5)
                          : const Color(0xFFE8DDD6).withValues(alpha: .6),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Text(
                      '$displayCount',
                      style: TextStyle(
                        fontFamily: 'Quicksand',
                        fontSize: 11,
                        fontWeight: FontWeight.w700,
                        color: isSelected ? primaryColor : textInactive,
                      ),
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _EmptyTabState extends StatelessWidget {
  const _EmptyTabState({required this.icon, required this.message});

  final IconData icon;
  final String message;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(vertical: 32, horizontal: 24),
      margin: const EdgeInsets.only(bottom: 20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(
          color: const Color(0xFFE8DDD6).withValues(alpha: .6),
        ),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF5A463F).withValues(alpha: .04),
            blurRadius: 16,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: Column(
        children: [
          Container(
            width: 54,
            height: 54,
            decoration: const BoxDecoration(
              color: Color(0xFFFFF3EE),
              shape: BoxShape.circle,
            ),
            child: Icon(icon, size: 26, color: const Color(0xFFC98C7B)),
          ),
          const SizedBox(height: 12),
          Text(
            message,
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontFamily: 'Quicksand',
              fontSize: 14,
              fontWeight: FontWeight.w600,
              color: Color(0xFF7A655C),
              height: 1.4,
            ),
          ),
        ],
      ),
    );
  }
}

class _Section extends StatelessWidget {
  const _Section({
    super.key,
    required this.title,
    required this.icon,
    required this.tasks,
    required this.acting,
    required this.onAction,
    required this.onDelete,
    required this.allowDelete,
    this.showTitle = true,
  });

  final String title;
  final IconData icon;
  final List<TodayTask> tasks;
  final Set<String> acting;
  final Future<void> Function(TodayTask, TodayTaskAction) onAction;
  final Future<void> Function(TodayTask) onDelete;
  final bool allowDelete;
  final bool showTitle;

  @override
  Widget build(BuildContext context) {
    if (tasks.isEmpty) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.only(bottom: 20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (showTitle) ...[
            Row(
              children: [
                Icon(icon, size: 20, color: const Color(0xFFC98C7B)),
                const SizedBox(width: 8),
                Text(
                  title,
                  style: const TextStyle(
                    fontFamily: 'Quicksand',
                    fontSize: 17,
                    fontWeight: FontWeight.w800,
                    color: Color(0xFF5A463F),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),
          ],
          ...tasks.map(
            (task) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: _TodayTaskCard(
                task: task,
                busy: acting.contains(task.id),
                onAction: (action) => onAction(task, action),
                onDelete: () => onDelete(task),
                allowDelete: allowDelete,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _TodayTaskCard extends StatelessWidget {
  const _TodayTaskCard({
    required this.task,
    required this.busy,
    required this.onAction,
    required this.onDelete,
    required this.allowDelete,
  });

  final TodayTask task;
  final bool busy;
  final ValueChanged<TodayTaskAction> onAction;
  final VoidCallback onDelete;
  final bool allowDelete;

  @override
  Widget build(BuildContext context) {
    final isCompleted = task.isCompleted;
    final tapAction = _tapAction;
    final canDelete =
        allowDelete &&
        task.isChecklist &&
        task.origin == TodayTaskOrigin.userCreated;

    return Semantics(
      container: true,
      selected: isCompleted,
      label:
          '${task.title}, ${task.originLabel}, ${task.targetLabel}, ${task.statusLabel}',
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          key: Key('task-item-${task.id}'),
          onTap: busy || tapAction == null ? null : () => onAction(tapAction!),
          borderRadius: BorderRadius.circular(20),
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 200),
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
            decoration: BoxDecoration(
              color: isCompleted
                  ? const Color(0xFFF9F5F2).withValues(alpha: .8)
                  : Colors.white,
              borderRadius: BorderRadius.circular(20),
              border: Border.all(
                color: isCompleted
                    ? const Color(0xFFE8DDD6).withValues(alpha: .5)
                    : const Color(0xFFE5D9D2).withValues(alpha: .8),
              ),
              boxShadow: isCompleted
                  ? null
                  : [
                      BoxShadow(
                        color: const Color(0xFF5A463F).withValues(alpha: .05),
                        blurRadius: 16,
                        offset: const Offset(0, 4),
                      ),
                    ],
            ),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Semantics(
                  button: true,
                  label: isCompleted ? 'Đã hoàn tất' : 'Chưa hoàn tất',
                  child: Padding(
                    padding: const EdgeInsets.only(top: 2),
                    child: busy
                        ? const SizedBox.square(
                            dimension: 22,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: Color(0xFFC98C7B),
                            ),
                          )
                        : Icon(
                            isCompleted
                                ? Icons.check_circle_rounded
                                : Icons.radio_button_unchecked_rounded,
                            size: 24,
                            color: isCompleted
                                ? const Color(0xFFC98C7B)
                                : const Color(0xFFB8A29A),
                          ),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Icon(
                            task.target == TodayTaskTarget.baby
                                ? Icons.child_care_rounded
                                : Icons.pregnant_woman_rounded,
                            color: isCompleted
                                ? const Color(0xFF9C857C)
                                : const Color(0xFFC98C7B),
                            size: 18,
                          ),
                          const SizedBox(width: 6),
                          Expanded(
                            child: Text(
                              task.title,
                              style: TextStyle(
                                fontFamily: 'Quicksand',
                                fontSize: 15.5,
                                fontWeight: isCompleted
                                    ? FontWeight.w600
                                    : FontWeight.w700,
                                color: isCompleted
                                    ? const Color(0xFF9C857C)
                                    : const Color(0xFF4A3831),
                                decoration: isCompleted
                                    ? TextDecoration.lineThrough
                                    : TextDecoration.none,
                                decorationColor: const Color(0xFF9C857C),
                                height: 1.35,
                              ),
                            ),
                          ),
                        ],
                      ),
                      if (task.careGroupLabel != null ||
                          task.careContextLabel != null) ...[
                        const SizedBox(height: 6),
                        Wrap(
                          spacing: 12,
                          runSpacing: 4,
                          children: [
                            if (task.careGroupLabel != null)
                              _ContextLabel(
                                icon: Icons.groups_outlined,
                                text: task.careGroupLabel!,
                              ),
                            if (task.careContextLabel != null)
                              _ContextLabel(
                                icon: Icons.link_rounded,
                                text: task.careContextLabel!,
                              ),
                          ],
                        ),
                      ],
                    ],
                  ),
                ),
                if (canDelete)
                  IconButton(
                    key: Key('delete-task-${task.id}'),
                    onPressed: busy ? null : onDelete,
                    tooltip: 'Xoá việc',
                    icon: const Icon(Icons.delete_outline_rounded),
                    color: const Color(0xFFB06E62),
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  TodayTaskAction? get _tapAction {
    if (task.isChecklist) {
      if (task.isCompleted &&
          task.allowedActions.contains(TodayTaskAction.reopen)) {
        return TodayTaskAction.reopen;
      }
      if (!task.isCompleted &&
          task.allowedActions.contains(TodayTaskAction.complete)) {
        return TodayTaskAction.complete;
      }
      return null;
    }
    if (task.isCompleted || task.isSkipped) return null;
    if (task.allowedActions.contains(TodayTaskAction.complete)) {
      return TodayTaskAction.complete;
    }
    if (task.allowedActions.contains(TodayTaskAction.skip)) {
      return TodayTaskAction.skip;
    }
    return null;
  }
}

class _InfoPill extends StatelessWidget {
  const _InfoPill({
    required this.icon,
    required this.text,
    this.backgroundColor,
  });

  final IconData icon;
  final String text;
  final Color? backgroundColor;

  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
    decoration: BoxDecoration(
      color: backgroundColor ?? const Color(0xFFF9F4F0),
      borderRadius: BorderRadius.circular(20),
      border: Border.all(color: const Color(0xFFEDE4DC).withValues(alpha: .7)),
    ),
    child: Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 15, color: const Color(0xFFC98C7B)),
        const SizedBox(width: 5),
        Flexible(
          child: Text(
            text,
            style: const TextStyle(
              fontFamily: 'Quicksand',
              fontSize: 12,
              fontWeight: FontWeight.w600,
              color: Color(0xFF6E584F),
            ),
          ),
        ),
      ],
    ),
  );
}

class _ContextLabel extends StatelessWidget {
  const _ContextLabel({required this.icon, required this.text});
  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) => Row(
    mainAxisSize: MainAxisSize.min,
    children: [
      Icon(icon, size: 17, color: const Color(0xFF9C857C)),
      const SizedBox(width: 5),
      Flexible(
        child: Text(
          text,
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
          style: const TextStyle(
            fontFamily: 'Quicksand',
            fontSize: 14,
            fontWeight: FontWeight.w600,
            color: Color(0xFF735E56),
          ),
        ),
      ),
    ],
  );
}

class _RoundIcon extends StatelessWidget {
  const _RoundIcon({required this.icon});
  final IconData icon;

  @override
  Widget build(BuildContext context) => Container(
    width: 48,
    height: 48,
    decoration: BoxDecoration(
      color: const Color(0xFFC98C7B).withValues(alpha: .15),
      shape: BoxShape.circle,
    ),
    child: Icon(icon, color: const Color(0xFFC98C7B)),
  );
}

class _LoadingState extends StatelessWidget {
  const _LoadingState();

  @override
  Widget build(BuildContext context) => Semantics(
    key: const Key('today-loading'),
    liveRegion: true,
    label: 'Đang tải việc hôm nay',
    child: Column(
      children: List.generate(
        2,
        (_) => Container(
          height: 112,
          margin: const EdgeInsets.only(bottom: 12),
          decoration: BoxDecoration(
            color: const Color(0xFFE8DDD6),
            borderRadius: BorderRadius.circular(24),
          ),
        ),
      ),
    ),
  );
}

class _EmptyState extends StatelessWidget {
  const _EmptyState();

  @override
  Widget build(BuildContext context) => Semantics(
    key: const Key('today-empty'),
    label: 'Không có việc nào trong 7 ngày tới',
    child: const _StateCard(
      icon: Icons.check_circle_outline_rounded,
      message: 'Không có việc nào trong 7 ngày tới.',
    ),
  );
}

class _FailureState extends StatelessWidget {
  const _FailureState({required this.failure, required this.onRetry});
  final TodayTasksFailure failure;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    if (failure.kind == TodayFailureKind.offline) {
      return const _OfflineBanner();
    }
    final terminal = failure.kind == TodayFailureKind.terminal;
    return Semantics(
      key: Key(terminal ? 'today-terminal-error' : 'today-retryable-error'),
      liveRegion: true,
      child: _StateCard(
        icon: terminal
            ? Icons.lock_outline_rounded
            : Icons.sync_problem_rounded,
        message: terminal
            ? 'Bạn không có quyền xem danh sách này.'
            : 'Chưa thể tải việc hôm nay.',
        action: terminal
            ? null
            : TextButton(onPressed: onRetry, child: const Text('Thử lại')),
      ),
    );
  }
}

class _OfflineBanner extends StatelessWidget {
  const _OfflineBanner();
  @override
  Widget build(BuildContext context) => Semantics(
    key: const Key('today-offline'),
    liveRegion: true,
    child: const _StateCard(
      icon: Icons.cloud_off_rounded,
      message: 'Bạn đang ngoại tuyến. Dữ liệu sẽ được tải lại khi có mạng.',
    ),
  );
}

class _StaleRetryBanner extends StatelessWidget {
  const _StaleRetryBanner({required this.onRetry});
  final VoidCallback onRetry;
  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(bottom: 12),
    child: _StateCard(
      icon: Icons.sync_problem_rounded,
      message: 'Đang hiển thị dữ liệu gần nhất.',
      action: TextButton(onPressed: onRetry, child: const Text('Thử lại')),
    ),
  );
}

class _StateCard extends StatelessWidget {
  const _StateCard({required this.icon, required this.message, this.action});
  final IconData icon;
  final String message;
  final Widget? action;

  @override
  Widget build(BuildContext context) => Container(
    width: double.infinity,
    padding: const EdgeInsets.all(18),
    decoration: BoxDecoration(
      color: const Color(0xFFF2EAE4),
      borderRadius: BorderRadius.circular(24),
      border: const Border(
        left: BorderSide(color: Color(0xFFC98C7B), width: 4),
      ),
    ),
    child: Row(
      children: [
        Icon(icon, color: const Color(0xFFC98C7B)),
        const SizedBox(width: 12),
        Expanded(
          child: Text(
            message,
            style: const TextStyle(
              fontFamily: 'Quicksand',
              fontSize: 16,
              fontWeight: FontWeight.w700,
              color: Color(0xFF5A463F),
            ),
          ),
        ),
        ?action,
      ],
    ),
  );
}
