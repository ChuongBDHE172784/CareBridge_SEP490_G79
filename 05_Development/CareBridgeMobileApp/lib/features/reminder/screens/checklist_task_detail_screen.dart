import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../models/today_task_model.dart';
import '../services/today_task_service.dart';

class ChecklistTaskDetailScreen extends StatefulWidget {
  const ChecklistTaskDetailScreen({
    super.key,
    required this.task,
    this.service,
  });

  final TodayTask task;
  final TodayTaskService? service;

  @override
  State<ChecklistTaskDetailScreen> createState() =>
      _ChecklistTaskDetailScreenState();
}

class _ChecklistTaskDetailScreenState extends State<ChecklistTaskDetailScreen> {
  static const _background = Color(0xFFF8F4F1);
  static const _surface = Colors.white;
  static const _primary = Color(0xFF845143);
  static const _accent = Color(0xFFC98C7B);
  static const _text = Color(0xFF4A3831);
  static const _muted = Color(0xFF765F55);
  static const _border = Color(0xFFE7DCD5);
  static const _error = Color(0xFFBA1A1A);

  late TodayTaskService _service;
  bool _processing = false;

  @override
  void initState() {
    super.initState();
    _service = widget.service ?? TodayTaskService.instance;
  }

  @override
  void didUpdateWidget(covariant ChecklistTaskDetailScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.service != widget.service) {
      _service = widget.service ?? TodayTaskService.instance;
    }
  }

  TodayTaskAction? get _statusAction {
    final task = widget.task;
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

  String get _description {
    final description = widget.task.description?.trim();
    if (description == null || description.isEmpty) {
      return 'Chưa có nội dung chi tiết cho việc này.';
    }
    return description;
  }

  String get _targetLabel => switch (widget.task.target) {
    TodayTaskTarget.mother => 'Mẹ',
    TodayTaskTarget.baby => 'Bé',
    TodayTaskTarget.unknown => widget.task.isChecklist ? 'Khuyến nghị' : 'Cá nhân',
  };

  Future<void> _performStatusAction() async {
    final action = _statusAction;
    if (action == null || _processing) return;

    setState(() => _processing = true);
    try {
      final task = widget.task;
      if (task.isChecklist) {
        await _service.performChecklistAction(
          taskId: task.id,
          action: action,
          careGroupId: task.careGroupId,
        );
      } else {
        await _service.performAction(
          taskKind: task.kind,
          taskId: task.id,
          action: action,
        );
      }
      if (!mounted) return;
      Navigator.of(context).pop(true);
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Không thể cập nhật việc cần làm. Vui lòng thử lại.'),
          backgroundColor: _error,
          behavior: SnackBarBehavior.floating,
        ),
      );
    } finally {
      if (mounted) setState(() => _processing = false);
    }
  }

  Future<void> _openSupportFunction() async {
    final supportFunction = widget.task.supportFunction;
    if (supportFunction == null) return;
    final route = supportFunction.routeFor(
      careContextType: widget.task.careContextType,
      careContextId: widget.task.careContextId,
    );
    if (route == null) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Chưa có hành trình để mở chỉ số sức khỏe.')),
      );
      return;
    }
    await context.push<void>(route);
  }

  @override
  Widget build(BuildContext context) {
    final task = widget.task;
    final supportFunction = task.supportFunction;
    final statusAction = _statusAction;

    return Scaffold(
      backgroundColor: _background,
      appBar: AppBar(
        backgroundColor: _background,
        foregroundColor: _text,
        elevation: 0,
        centerTitle: true,
        title: const Text(
          'Chi tiết việc cần làm',
          style: TextStyle(
            fontFamily: 'Quicksand',
            fontSize: 20,
            fontWeight: FontWeight.w800,
          ),
        ),
      ),
      body: SafeArea(
        top: false,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 12, 20, 28),
          children: [
            _TaskHeaderCard(task: task, targetLabel: _targetLabel),
            const SizedBox(height: 20),
            _DetailSection(description: _description),
            if (supportFunction != null) ...[
              const SizedBox(height: 20),
              _SupportFunctionCard(
                label: supportFunction.label,
                onOpen: _openSupportFunction,
              ),
            ],
          ],
        ),
      ),
      bottomNavigationBar: statusAction == null
          ? null
          : SafeArea(
              minimum: const EdgeInsets.fromLTRB(20, 10, 20, 16),
              child: SizedBox(
                height: 54,
                child: FilledButton.icon(
                  key: const Key('task-detail-status-action'),
                  onPressed: _processing ? null : _performStatusAction,
                  style: FilledButton.styleFrom(
                    backgroundColor: _primary,
                    foregroundColor: Colors.white,
                    disabledBackgroundColor: _primary.withValues(alpha: .55),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                  ),
                  icon: _processing
                      ? const SizedBox.square(
                          dimension: 20,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: Colors.white,
                          ),
                        )
                      : Icon(
                          statusAction == TodayTaskAction.reopen
                              ? Icons.refresh_rounded
                              : Icons.check_rounded,
                        ),
                  label: Text(
                    statusAction == TodayTaskAction.reopen
                        ? 'Mở lại việc'
                        : 'Đánh dấu hoàn tất',
                    style: const TextStyle(
                      fontFamily: 'Quicksand',
                      fontSize: 16,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
              ),
            ),
    );
  }
}

class _TaskHeaderCard extends StatelessWidget {
  const _TaskHeaderCard({required this.task, required this.targetLabel});

  final TodayTask task;
  final String targetLabel;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _ChecklistTaskDetailScreenState._surface,
        borderRadius: BorderRadius.circular(22),
        border: Border.all(color: _ChecklistTaskDetailScreenState._border),
        boxShadow: [
          BoxShadow(
            color: _ChecklistTaskDetailScreenState._text.withValues(alpha: .06),
            blurRadius: 18,
            offset: const Offset(0, 5),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 48,
            height: 48,
            decoration: const BoxDecoration(
              color: Color(0xFFFFEEE8),
              shape: BoxShape.circle,
            ),
            child: Icon(
              task.target == TodayTaskTarget.baby
                  ? Icons.child_care_rounded
                  : Icons.playlist_add_check_circle_rounded,
              color: _ChecklistTaskDetailScreenState._primary,
              size: 25,
            ),
          ),
          const SizedBox(height: 16),
          Text(
            task.title,
            key: const Key('task-detail-title'),
            style: const TextStyle(
              fontFamily: 'Quicksand',
              fontSize: 24,
              fontWeight: FontWeight.w800,
              height: 1.25,
              color: _ChecklistTaskDetailScreenState._text,
            ),
          ),
          const SizedBox(height: 16),
          Wrap(
            spacing: 10,
            runSpacing: 10,
            children: [
              _MetadataPill(
                icon: task.target == TodayTaskTarget.baby
                    ? Icons.child_care_outlined
                    : task.isChecklist && task.target == TodayTaskTarget.unknown
                        ? Icons.checklist_outlined
                        : Icons.favorite_outline_rounded,
                label: 'Đối tượng',
                value: targetLabel,
              ),
              _MetadataPill(
                icon: task.isCompleted
                    ? Icons.check_circle_outline_rounded
                    : Icons.schedule_rounded,
                label: 'Trạng thái',
                value: task.statusLabel,
              ),
            ],
          ),
          if (task.careContextLabel?.trim().isNotEmpty == true) ...[
            const SizedBox(height: 14),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Padding(
                  padding: EdgeInsets.only(top: 2),
                  child: Icon(
                    Icons.link_rounded,
                    size: 18,
                    color: _ChecklistTaskDetailScreenState._accent,
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    task.careContextLabel!.trim(),
                    style: const TextStyle(
                      fontFamily: 'Quicksand',
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      color: _ChecklistTaskDetailScreenState._muted,
                      height: 1.4,
                    ),
                  ),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }
}

class _MetadataPill extends StatelessWidget {
  const _MetadataPill({
    required this.icon,
    required this.label,
    required this.value,
  });

  final IconData icon;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: '$label: $value',
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 9),
        decoration: BoxDecoration(
          color: const Color(0xFFF8F1ED),
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: _ChecklistTaskDetailScreenState._border),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              icon,
              size: 17,
              color: _ChecklistTaskDetailScreenState._accent,
            ),
            const SizedBox(width: 7),
            Text(
              value,
              style: const TextStyle(
                fontFamily: 'Quicksand',
                fontSize: 13,
                fontWeight: FontWeight.w700,
                color: _ChecklistTaskDetailScreenState._muted,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _DetailSection extends StatelessWidget {
  const _DetailSection({required this.description});

  final String description;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _ChecklistTaskDetailScreenState._surface,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: _ChecklistTaskDetailScreenState._border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(
                Icons.notes_rounded,
                size: 21,
                color: _ChecklistTaskDetailScreenState._accent,
              ),
              SizedBox(width: 9),
              Text(
                'Nội dung chi tiết',
                style: TextStyle(
                  fontFamily: 'Quicksand',
                  fontSize: 17,
                  fontWeight: FontWeight.w800,
                  color: _ChecklistTaskDetailScreenState._text,
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Text(
            description,
            key: const Key('task-detail-description'),
            style: const TextStyle(
              fontFamily: 'Quicksand',
              fontSize: 15.5,
              fontWeight: FontWeight.w500,
              color: _ChecklistTaskDetailScreenState._muted,
              height: 1.6,
            ),
          ),
        ],
      ),
    );
  }
}

class _SupportFunctionCard extends StatelessWidget {
  const _SupportFunctionCard({required this.label, required this.onOpen});

  final String label;
  final VoidCallback onOpen;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF0EA),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: const Color(0xFFE8C7BA)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Chức năng hỗ trợ',
            style: TextStyle(
              fontFamily: 'Quicksand',
              fontSize: 14,
              fontWeight: FontWeight.w700,
              color: _ChecklistTaskDetailScreenState._muted,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            label,
            key: const Key('task-support-function-label'),
            style: const TextStyle(
              fontFamily: 'Quicksand',
              fontSize: 18,
              fontWeight: FontWeight.w800,
              color: _ChecklistTaskDetailScreenState._text,
            ),
          ),
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            height: 52,
            child: FilledButton.icon(
              key: const Key('task-support-function-button'),
              onPressed: onOpen,
              style: FilledButton.styleFrom(
                backgroundColor: _ChecklistTaskDetailScreenState._primary,
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(15),
                ),
              ),
              icon: const Icon(Icons.open_in_new_rounded),
              label: const Text(
                'Mở chức năng hỗ trợ',
                style: TextStyle(
                  fontFamily: 'Quicksand',
                  fontSize: 15,
                  fontWeight: FontWeight.w800,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
