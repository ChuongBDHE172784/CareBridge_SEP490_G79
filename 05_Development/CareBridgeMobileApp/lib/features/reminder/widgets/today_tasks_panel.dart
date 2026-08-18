import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:uuid/uuid.dart';

import '../../checklist/services/user_checklist_service.dart';
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
    this.careGroupId,
    this.showHeading = true,
    this.controller,
    this.headingAction,
  });

  final TodayTaskService? service;
  final UserChecklistService? checklistService;
  final TodayTasksAudience audience;
  final TodayTasksLayout layout;
  final String? careGroupId;
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
  final Map<String, String> _actionRequestIds = {};
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
    if (oldWidget.careGroupId != widget.careGroupId) {
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
      final snapshot = await _service.loadToday(
        careGroupId: widget.careGroupId,
      );
      if (!mounted || generation != _loadGeneration) return;
      setState(() {
        _snapshot = snapshot;
        _loading = false;
      });
    } on TodayTasksFailure catch (failure) {
      if (!mounted || generation != _loadGeneration) return;
      setState(() {
        _failure = failure;
        if (failure.kind == TodayFailureKind.terminal) {
          _snapshot = null;
        }
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
      _actionRequestIds.clear();
      _advancingSequence = false;
      _sequenceRequestId = null;
    });
  }

  Future<void> _act(TodayTask task, TodayTaskAction action) async {
    if (_acting.contains(task.id)) return;
    setState(() => _acting.add(task.id));
    final requestKey =
        '${widget.careGroupId ?? ''}:${task.id}:${action.apiValue}';
    final requestId = _actionRequestIds.putIfAbsent(requestKey, _newRequestId);
    try {
      if (task.isChecklist) {
        await _service.performChecklistAction(
          taskId: task.id,
          action: action,
          careGroupId: widget.careGroupId,
          clientRequestId: requestId,
        );
      } else {
        await _service.performAction(
          taskKind: task.kind,
          taskId: task.id,
          action: action,
        );
      }
      await _load();
      if (!mounted) return;
      if (_failure == null) {
        _actionRequestIds.remove(requestKey);
      }
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

  Future<void> _openDetail(TodayTask task) async {
    final audienceParam =
        widget.audience == TodayTasksAudience.family ? '?audience=family' : '';
    final changed = await context.push<bool>(
      '/checklists/task-detail$audienceParam',
      extra: task,
    );
    if (!mounted || changed != true) return;
    await _load();
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
        .where((task) => task.isChecklist)
        .toList();
    tasks.sort(_compareNewestFirst);
    return tasks;
  }

  List<TodayTask> _checklistOnly(List<TodayTask> tasks) =>
      tasks.toList(growable: false);

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
    final checklistCount = _snapshot == null
        ? 0
        : _checklistOnly(_snapshot!.sections.all.toList()).length;
    final hasVisibleTasks = widget.layout == TodayTasksLayout.sourceGroups
        ? sourceGroupedTasks.isNotEmpty
        : checklistCount > 0 || _snapshot?.sequence != null;

    final systemTasks = sourceGroupedTasks
        .where((task) => task.origin == TodayTaskOrigin.systemTemplate)
        .toList(growable: false);
    final userTasks = sourceGroupedTasks
        .where((task) => task.origin != TodayTaskOrigin.systemTemplate)
        .toList(growable: false);
    final postpartumTasks = systemTasks
        .where((task) => task.stage == TodayChecklistStage.postpartum)
        .toList(growable: false);
    final babyCareTasks = systemTasks
        .where(
          (task) =>
              task.stage == TodayChecklistStage.babyCare ||
              (task.stage == TodayChecklistStage.unknown &&
                  task.careContextType?.toUpperCase() == 'BABY'),
        )
        .toList(growable: false);
    final babyCareGroups = <String, List<TodayTask>>{};
    for (final task in babyCareTasks) {
      final groupKey = task.careContextId?.trim().isNotEmpty == true
          ? task.careContextId!
          : 'unknown-baby';
      babyCareGroups.putIfAbsent(groupKey, () => []).add(task);
    }
    final otherSystemTasks = systemTasks
        .where(
          (task) =>
              !postpartumTasks.contains(task) && !babyCareTasks.contains(task),
        )
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
                  Column(
                    key: const Key('today-system-tasks'),
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      if (postpartumTasks.isNotEmpty)
                        _Section(
                          key: const Key('today-postpartum-tasks'),
                          title: 'Hậu sản',
                          icon: Icons.health_and_safety_outlined,
                          tasks: postpartumTasks,
                          acting: _acting,
                          onOpen: _openDetail,
                          onAction: _act,
                          onDelete: _delete,
                          allowDelete:
                              widget.audience == TodayTasksAudience.mother,
                          allowAction:
                              widget.audience == TodayTasksAudience.mother,
                        ),
                      if (babyCareTasks.isNotEmpty)
                        Column(
                          key: const Key('today-baby-care-tasks'),
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: babyCareGroups.entries.map((entry) {
                            final tasks = entry.value;
                            final babyLabel = tasks
                                .map((task) => task.careContextLabel?.trim())
                                .whereType<String>()
                                .firstWhere(
                                  (label) => label.isNotEmpty,
                                  orElse: () => 'Bé',
                                );
                            return _Section(
                              key: Key('today-baby-care-${entry.key}'),
                              title: 'Chăm bé · $babyLabel',
                              icon: Icons.child_care_rounded,
                              tasks: tasks,
                              acting: _acting,
                              onOpen: _openDetail,
                              onAction: _act,
                              onDelete: _delete,
                              allowDelete:
                                  widget.audience == TodayTasksAudience.mother,
                              allowAction:
                                  widget.audience == TodayTasksAudience.mother,
                            );
                          }).toList(growable: false),
                        ),
                      if (otherSystemTasks.isNotEmpty)
                        _Section(
                          key: const Key('today-other-system-tasks'),
                          title: 'Gợi ý CareBridge',
                          icon: Icons.auto_awesome_rounded,
                          tasks: otherSystemTasks,
                          acting: _acting,
                          onOpen: _openDetail,
                          onAction: _act,
                          onDelete: _delete,
                          allowDelete:
                              widget.audience == TodayTasksAudience.mother,
                          allowAction:
                              widget.audience == TodayTasksAudience.mother,
                          showTitle:
                              postpartumTasks.isNotEmpty || babyCareTasks.isNotEmpty,
                        ),
                    ],
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
                    onOpen: _openDetail,
                    onAction: _act,
                    onDelete: _delete,
                    allowDelete: widget.audience == TodayTasksAudience.mother,
                    allowAction: widget.audience == TodayTasksAudience.mother,
                    showTitle: false,
                  )
                else
                  _EmptyTabState(
                    icon: Icons.playlist_add_check_rounded,
                    message: widget.audience == TodayTasksAudience.family
                        ? 'Chưa có việc cá nhân nào được chia sẻ.'
                        : 'Bạn chưa tạo công việc cá nhân nào.',
                  ),
              ],
            ] else ...[
              _Section(
                title: 'Quá hạn',
                icon: Icons.error_outline_rounded,
                tasks: _checklistOnly(_snapshot!.sections.overdue),
                acting: _acting,
                onOpen: _openDetail,
                onAction: _act,
                onDelete: _delete,
                allowDelete: widget.audience == TodayTasksAudience.mother,
                allowAction: widget.audience == TodayTasksAudience.mother,
              ),
              _Section(
                title: 'Hôm nay',
                icon: Icons.wb_sunny_outlined,
                tasks: _checklistOnly(_snapshot!.sections.today),
                acting: _acting,
                onOpen: _openDetail,
                onAction: _act,
                onDelete: _delete,
                allowDelete: widget.audience == TodayTasksAudience.mother,
                allowAction: widget.audience == TodayTasksAudience.mother,
              ),
              _Section(
                title: '7 ngày tới',
                icon: Icons.date_range_rounded,
                tasks: _checklistOnly(_snapshot!.sections.upcoming),
                acting: _acting,
                onOpen: _openDetail,
                onAction: _act,
                onDelete: _delete,
                allowDelete: widget.audience == TodayTasksAudience.mother,
                allowAction: widget.audience == TodayTasksAudience.mother,
              ),
              _Section(
                title: 'Chưa xếp lịch',
                icon: Icons.event_busy_rounded,
                tasks: _checklistOnly(_snapshot!.sections.unscheduled),
                acting: _acting,
                onOpen: _openDetail,
                onAction: _act,
                onDelete: _delete,
                allowDelete: widget.audience == TodayTasksAudience.mother,
                allowAction: widget.audience == TodayTasksAudience.mother,
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
      height: 54,
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [Color(0xFFFAF4EE), Color(0xFFF3EBE5)],
        ),
        borderRadius: BorderRadius.circular(27),
        border: Border.all(
          color: const Color(0xFFE8DDD6).withValues(alpha: .8),
          width: 1.2,
        ),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF5A463F).withValues(alpha: .06),
            blurRadius: 16,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Stack(
        children: [
          // Sliding active card indicator over equal 50% width
          AnimatedAlign(
            duration: const Duration(milliseconds: 240),
            curve: Curves.easeInOutCubic,
            alignment: active == 0
                ? Alignment.centerLeft
                : Alignment.centerRight,
            child: FractionallySizedBox(
              widthFactor: 0.5,
              heightFactor: 1.0,
              child: Container(
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(23),
                  border: Border.all(color: const Color(0xFFF0E4DD), width: 1),
                  boxShadow: [
                    BoxShadow(
                      color: const Color(0xFFC98C7B).withValues(alpha: .15),
                      blurRadius: 12,
                      offset: const Offset(0, 3),
                    ),
                    BoxShadow(
                      color: const Color(0xFF5A463F).withValues(alpha: .04),
                      blurRadius: 4,
                      offset: const Offset(0, 1),
                    ),
                  ],
                ),
              ),
            ),
          ),

          // Interactive equal 50% width tab buttons
          Row(
            children: [
              Expanded(
                child: _TabButton(
                  key: const Key('tab-system-tasks'),
                  title: 'Gợi ý CareBridge',
                  count: _safeSystemCount,
                  icon: Icons.auto_awesome_rounded,
                  isSelected: active == 0,
                  isSystemTab: true,
                  onTap: () => onTabSelected(0),
                ),
              ),
              Expanded(
                child: _TabButton(
                  key: const Key('tab-user-tasks'),
                  title: 'Việc cá nhân',
                  count: _safeUserCount,
                  icon: Icons.person_rounded,
                  isSelected: active == 1,
                  isSystemTab: false,
                  onTap: () => onTabSelected(1),
                ),
              ),
            ],
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
    this.isSystemTab = true,
    required this.onTap,
  });

  final String title;
  final int count;
  final IconData icon;
  final bool isSelected;
  final bool isSystemTab;
  final VoidCallback onTap;

  int get _safeCount => (count as dynamic) is int ? count : 0;

  @override
  Widget build(BuildContext context) {
    const primaryAccent = Color(0xFFC98C7B);
    const systemActiveColor = Color(0xFFD97757);
    const userActiveColor = Color(0xFF6E584F);
    const textActive = Color(0xFF3D2E28);
    const textInactive = Color(0xFF7A655C);
    final displayCount = _safeCount;

    return Semantics(
      button: true,
      selected: isSelected,
      label: '$title, $displayCount công việc',
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(23),
          child: Center(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 8),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.center,
                mainAxisSize: MainAxisSize.min,
                children: [
                  AnimatedContainer(
                    duration: const Duration(milliseconds: 220),
                    width: isSelected ? 26 : 22,
                    height: isSelected ? 26 : 22,
                    decoration: BoxDecoration(
                      color: isSelected
                          ? (isSystemTab
                                ? const Color(0xFFFFF0EC)
                                : const Color(0xFFF4ECE7))
                          : Colors.transparent,
                      shape: BoxShape.circle,
                    ),
                    child: Center(
                      child: Icon(
                        icon,
                        size: isSelected ? 15 : 16,
                        color: isSelected
                            ? (isSystemTab
                                  ? systemActiveColor
                                  : userActiveColor)
                            : const Color(0xFF9E877C),
                      ),
                    ),
                  ),
                  const SizedBox(width: 6),
                  Flexible(
                    child: AnimatedDefaultTextStyle(
                      duration: const Duration(milliseconds: 200),
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontFamily: 'Quicksand',
                        fontSize: isSelected ? 15.0 : 14.0,
                        fontWeight: isSelected
                            ? FontWeight.w800
                            : FontWeight.w700,
                        color: isSelected ? textActive : textInactive,
                        letterSpacing: isSelected ? -0.1 : 0,
                      ),
                      child: Text(
                        title,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        textAlign: TextAlign.center,
                      ),
                    ),
                  ),
                  if (displayCount > 0) ...[
                    const SizedBox(width: 6),
                    AnimatedContainer(
                      duration: const Duration(milliseconds: 220),
                      padding: EdgeInsets.symmetric(
                        horizontal: isSelected ? 9 : 7,
                        vertical: isSelected ? 3 : 2.5,
                      ),
                      decoration: BoxDecoration(
                        color: isSelected
                            ? (isSystemTab
                                  ? const Color(0xFFFFEBE4)
                                  : const Color(0xFFF0E8E2))
                            : const Color(0xFFE0D5CD).withValues(alpha: .65),
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: Text(
                        '$displayCount',
                        style: TextStyle(
                          fontFamily: 'Quicksand',
                          fontSize: isSelected ? 12.0 : 11.5,
                          fontWeight: isSelected
                              ? FontWeight.w800
                              : FontWeight.w700,
                          color: isSelected
                              ? (isSystemTab
                                    ? primaryAccent
                                    : const Color(0xFF5A463F))
                              : textInactive,
                        ),
                      ),
                    ),
                  ],
                ],
              ),
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
    required this.onOpen,
    required this.onAction,
    required this.onDelete,
    required this.allowDelete,
    this.allowAction = true,
    this.showTitle = true,
  });

  final String title;
  final IconData icon;
  final List<TodayTask> tasks;
  final Set<String> acting;
  final ValueChanged<TodayTask> onOpen;
  final Future<void> Function(TodayTask, TodayTaskAction) onAction;
  final Future<void> Function(TodayTask) onDelete;
  final bool allowDelete;
  final bool allowAction;
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
                onOpen: () => onOpen(task),
                onAction: (action) => onAction(task, action),
                onDelete: () => onDelete(task),
                allowDelete: allowDelete,
                allowAction: allowAction,
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
    required this.onOpen,
    required this.onAction,
    required this.onDelete,
    required this.allowDelete,
    this.allowAction = true,
  });

  final TodayTask task;
  final bool busy;
  final VoidCallback onOpen;
  final ValueChanged<TodayTaskAction> onAction;
  final VoidCallback onDelete;
  final bool allowDelete;
  final bool allowAction;

  @override
  Widget build(BuildContext context) {
    final isCompleted = task.isCompleted;
    final tapAction = _tapAction;
    final canDelete =
        allowDelete &&
        task.isChecklist &&
        task.origin == TodayTaskOrigin.userCreated;
    final targetless =
        task.isChecklist && task.target == TodayTaskTarget.unknown;

    return Semantics(
      container: true,
      explicitChildNodes: true,
      selected: isCompleted,
      // A V2 task has no target subject.  Do not announce the legacy
      // user-created "My care" origin as if it were the task target.
      label: targetless
          ? 'Xem chi tiết ${task.title}, ${task.targetLabel}, ${task.statusLabel}${_cadenceAnnouncement(task)}'
          : 'Xem chi tiết ${task.title}, ${task.originLabel}, ${task.targetLabel}, ${task.statusLabel}${_cadenceAnnouncement(task)}',
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          key: Key('task-item-${task.id}'),
          onTap: busy ? null : onOpen,
          borderRadius: BorderRadius.circular(20),
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 200),
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
            decoration: BoxDecoration(
              color: isCompleted ? const Color(0xFFFAF6F3) : Colors.white,
              borderRadius: BorderRadius.circular(20),
              border: Border.all(
                color: isCompleted
                    ? const Color(0xFFEFE6E0)
                    : const Color(0xFFE5D9D2).withValues(alpha: .9),
                width: 1.2,
              ),
              boxShadow: isCompleted
                  ? null
                  : [
                      BoxShadow(
                        color: const Color(0xFF5A463F).withValues(alpha: .06),
                        blurRadius: 16,
                        offset: const Offset(0, 4),
                      ),
                    ],
            ),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                _TaskStatusControl(
                  task: task,
                  busy: busy,
                  action: tapAction,
                  onAction: onAction,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        crossAxisAlignment: CrossAxisAlignment.center,
                        children: [
                          Container(
                            width: 28,
                            height: 28,
                            decoration: BoxDecoration(
                              color: isCompleted
                                  ? const Color(0xFFF5ECE7)
                                  : const Color(0xFFFFF1EB),
                              shape: BoxShape.circle,
                            ),
                            child: Icon(
                              switch (task.target) {
                                TodayTaskTarget.baby =>
                                  Icons.child_care_rounded,
                                TodayTaskTarget.mother =>
                                  Icons.pregnant_woman_rounded,
                                TodayTaskTarget.unknown =>
                                  task.isChecklist
                                      ? Icons.checklist_rounded
                                      : Icons.pregnant_woman_rounded,
                              },
                              color: isCompleted
                                  ? const Color(0xFF9C857C)
                                  : const Color(0xFFC98C7B),
                              size: 16,
                            ),
                          ),
                          const SizedBox(width: 8),
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
                      if (task.cadenceLabel != null) ...[
                        const SizedBox(height: 6),
                        _CadenceLabel(task: task),
                      ],
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
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    if (canDelete)
                      IconButton(
                        key: Key('delete-task-${task.id}'),
                        onPressed: busy ? null : onDelete,
                        tooltip: 'Xoá việc',
                        icon: const Icon(Icons.delete_outline_rounded),
                        color: const Color(0xFFB06E62),
                      ),
                    const ExcludeSemantics(
                      child: Icon(
                        Icons.chevron_right_rounded,
                        size: 24,
                        color: Color(0xFFB7A49B),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  static String _cadenceAnnouncement(TodayTask task) =>
      task.cadenceLabel == null ? '' : ', ${task.cadenceLabel}';

  TodayTaskAction? get _tapAction {
    if (!allowAction) return null;
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
    return null;
  }
}

class _CadenceLabel extends StatelessWidget {
  const _CadenceLabel({required this.task});

  final TodayTask task;

  @override
  Widget build(BuildContext context) {
    final cadence = task.cadence;
    final icon = switch (cadence) {
      TodayTaskCadence.daily => Icons.today_outlined,
      TodayTaskCadence.weekly => Icons.date_range_outlined,
      TodayTaskCadence.once => Icons.event_note_outlined,
      TodayTaskCadence.unknown => Icons.event_note_outlined,
    };
    final label = task.cadenceLabel;
    if (label == null) return const SizedBox.shrink();

    // The surrounding task Semantics node already announces this value.  Keep
    // the visual badge out of the child tree so screen readers do not repeat it.
    return ExcludeSemantics(
      child: _ContextLabel(icon: icon, text: label),
    );
  }
}

class _TaskStatusControl extends StatelessWidget {
  const _TaskStatusControl({
    required this.task,
    required this.busy,
    required this.action,
    required this.onAction,
  });

  final TodayTask task;
  final bool busy;
  final TodayTaskAction? action;
  final ValueChanged<TodayTaskAction> onAction;

  @override
  Widget build(BuildContext context) {
    if (busy) {
      return const SizedBox.square(
        dimension: 48,
        child: Center(
          child: SizedBox.square(
            dimension: 22,
            child: CircularProgressIndicator(
              strokeWidth: 2,
              color: Color(0xFFC98C7B),
            ),
          ),
        ),
      );
    }

    final isCompleted = task.isCompleted;
    final statusIcon = Icon(
      isCompleted
          ? Icons.check_circle_rounded
          : Icons.radio_button_unchecked_rounded,
      size: 25,
      color: isCompleted ? const Color(0xFFC98C7B) : const Color(0xFFBFAAA0),
    );
    if (action == null) {
      return Semantics(
        label: task.statusLabel,
        child: SizedBox.square(dimension: 48, child: Center(child: statusIcon)),
      );
    }

    final resolvedAction = action!;
    final tooltip = switch (resolvedAction) {
      TodayTaskAction.complete => 'Đánh dấu hoàn tất',
      TodayTaskAction.reopen => 'Mở lại việc',
      TodayTaskAction.skip => 'Bỏ qua việc',
    };
    final actionIcon = resolvedAction == TodayTaskAction.skip
        ? const Icon(
            Icons.skip_next_rounded,
            size: 25,
            color: Color(0xFFB06E62),
          )
        : statusIcon;
    return IconButton(
      key: Key('task-status-${task.id}'),
      onPressed: () => onAction(resolvedAction),
      tooltip: tooltip,
      constraints: const BoxConstraints.tightFor(width: 48, height: 48),
      padding: EdgeInsets.zero,
      icon: actionIcon,
    );
  }
}

class _InfoPill extends StatelessWidget {
  const _InfoPill({required this.icon, required this.text});

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
    decoration: BoxDecoration(
      color: const Color(0xFFF9F4F0),
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
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
    decoration: BoxDecoration(
      color: const Color(0xFFF7F2EE),
      borderRadius: BorderRadius.circular(8),
      border: Border.all(color: const Color(0xFFEAE0D9), width: 0.8),
    ),
    child: Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 14, color: const Color(0xFF9C857C)),
        const SizedBox(width: 4),
        Flexible(
          child: Text(
            text,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(
              fontFamily: 'Quicksand',
              fontSize: 12.5,
              fontWeight: FontWeight.w600,
              color: Color(0xFF6E584F),
            ),
          ),
        ),
      ],
    ),
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
