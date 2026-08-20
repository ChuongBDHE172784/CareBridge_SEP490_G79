import 'package:flutter/material.dart';
import 'package:uuid/uuid.dart';

import '../models/user_checklist_item_model.dart';
import '../services/user_checklist_service.dart';

typedef UserChecklistTaskCreated = Future<void> Function();

/// Opens the personal-task composer for a canonical V2 user-created task.
///
/// The entry is hidden unless exactly one authorized care context is supplied,
/// so callers never expose a button that can only fail server validation.
class AddUserChecklistTaskButton extends StatelessWidget {
  const AddUserChecklistTaskButton({
    super.key,
    this.journeyId,
    this.babyId,
    this.careGroupId,
    this.service,
    this.onCreated,
    this.clientTaskIdFactory,
  });

  final String? journeyId;
  final String? babyId;
  final String? careGroupId;
  final UserChecklistService? service;
  final UserChecklistTaskCreated? onCreated;
  final String Function()? clientTaskIdFactory;

  bool get _hasOneContext {
    final contexts = [
      careGroupId,
      journeyId,
      babyId,
    ].where((value) => value != null && value.isNotEmpty).length;
    return contexts == 1;
  }

  @override
  Widget build(BuildContext context) {
    if (!_hasOneContext) return const SizedBox.shrink();

    return Semantics(
      button: true,
      label: 'Thêm việc do tôi tạo',
      child: IconButton(
        key: const Key('add-user-checklist-task'),
        tooltip: 'Thêm việc',
        onPressed: () => _openComposer(context),
        icon: const Icon(Icons.add_rounded),
        color: const Color(0xFF845143),
      ),
    );
  }

  Future<void> _openComposer(BuildContext context) async {
    final created = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      barrierColor: const Color(0x665A463F),
      builder: (_) => _UserChecklistTaskSheet(
        journeyId: journeyId,
        babyId: babyId,
        careGroupId: careGroupId,
        service: service ?? UserChecklistService.instance,
        clientTaskIdFactory: clientTaskIdFactory ?? const Uuid().v4,
      ),
    );
    if (created != true || !context.mounted) return;

    await onCreated?.call();
    if (!context.mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Đã thêm việc vào hôm nay.'),
        behavior: SnackBarBehavior.floating,
        shape: StadiumBorder(),
        backgroundColor: Color(0xFF5A463F),
      ),
    );
  }
}

enum TaskRecurrence { none, daily, weekly, monthly }

extension TaskRecurrenceExtension on TaskRecurrence {
  String get label {
    switch (this) {
      case TaskRecurrence.none:
        return 'Không lặp';
      case TaskRecurrence.daily:
        return 'Hàng ngày';
      case TaskRecurrence.weekly:
        return 'Hàng tuần';
      case TaskRecurrence.monthly:
        return 'Hàng tháng';
    }
  }

  IconData get icon {
    switch (this) {
      case TaskRecurrence.none:
        return Icons.event_busy_rounded;
      case TaskRecurrence.daily:
        return Icons.today_rounded;
      case TaskRecurrence.weekly:
        return Icons.date_range_rounded;
      case TaskRecurrence.monthly:
        return Icons.calendar_month_rounded;
    }
  }
}

enum TaskDurationOption { oneDay, oneWeek, oneMonth, fullJourney, custom }

extension TaskDurationOptionExtension on TaskDurationOption {
  String get label {
    switch (this) {
      case TaskDurationOption.oneDay:
        return 'Hôm nay (1 ngày)';
      case TaskDurationOption.oneWeek:
        return '1 tuần';
      case TaskDurationOption.oneMonth:
        return '1 tháng';
      case TaskDurationOption.fullJourney:
        return 'Suốt thai kỳ';
      case TaskDurationOption.custom:
        return 'Tùy chọn ngày kết thúc...';
    }
  }
}

class _UserChecklistTaskSheet extends StatefulWidget {
  const _UserChecklistTaskSheet({
    required this.journeyId,
    required this.babyId,
    required this.careGroupId,
    required this.service,
    required this.clientTaskIdFactory,
  });

  final String? journeyId;
  final String? babyId;
  final String? careGroupId;
  final UserChecklistService service;
  final String Function() clientTaskIdFactory;

  @override
  State<_UserChecklistTaskSheet> createState() =>
      _UserChecklistTaskSheetState();
}

class _UserChecklistTaskSheetState extends State<_UserChecklistTaskSheet> {
  final _formKey = GlobalKey<FormState>();
  final _titleController = TextEditingController();
  final _descriptionController = TextEditingController();
  TaskRecurrence _recurrence = TaskRecurrence.none;
  TaskDurationOption _durationOption = TaskDurationOption.oneDay;
  DateTime? _customEndDate;
  bool _saving = false;
  String? _errorMessage;
  String? _lastPayload;
  String? _clientTaskId;

  @override
  void dispose() {
    _titleController.dispose();
    _descriptionController.dispose();
    super.dispose();
  }

  Future<void> _pickCustomEndDate() async {
    final now = DateTime.now();
    final initial = _customEndDate ?? now.add(const Duration(days: 7));
    final picked = await showDatePicker(
      context: context,
      initialDate: initial.isBefore(now) ? now : initial,
      firstDate: now,
      lastDate: now.add(const Duration(days: 365 * 2)),
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            colorScheme: const ColorScheme.light(
              primary: Color(0xFFC98C7B),
              onPrimary: Colors.white,
              surface: Colors.white,
              onSurface: Color(0xFF5A463F),
            ),
          ),
          child: child!,
        );
      },
    );
    if (picked != null) {
      setState(() {
        _customEndDate = picked;
        _durationOption = TaskDurationOption.custom;
      });
    }
  }

  String _formatDate(DateTime dt) {
    return '${dt.day.toString().padLeft(2, '0')}/${dt.month.toString().padLeft(2, '0')}/${dt.year}';
  }

  @override
  Widget build(BuildContext context) {
    final keyboardInset = MediaQuery.viewInsetsOf(context).bottom;
    return SafeArea(
      top: false,
      child: AnimatedPadding(
        duration: const Duration(milliseconds: 200),
        padding: EdgeInsets.only(bottom: keyboardInset),
        child: Container(
          decoration: const BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.vertical(top: Radius.circular(32)),
            boxShadow: [
              BoxShadow(
                color: Color(0x145A463F),
                blurRadius: 32,
                offset: Offset(0, -12),
              ),
            ],
          ),
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(24, 12, 24, 28),
            child: Form(
              key: _formKey,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Center(
                    child: SizedBox(
                      width: 48,
                      height: 6,
                      child: DecoratedBox(
                        decoration: BoxDecoration(
                          color: Color(0xFFE8DDD6),
                          borderRadius: BorderRadius.all(Radius.circular(99)),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(height: 20),
                  const Text(
                    'Thêm việc của tôi',
                    style: TextStyle(
                      fontFamily: 'Quicksand',
                      fontSize: 22,
                      fontWeight: FontWeight.w800,
                      color: Color(0xFF5A463F),
                    ),
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    'Việc này sẽ được đánh dấu là “Do tôi tạo” và hiển thị trong Việc hôm nay.',
                    style: TextStyle(
                      fontSize: 15,
                      height: 1.4,
                      color: Color(0xFF9C857C),
                    ),
                  ),
                  const SizedBox(height: 20),

                  // 1. Tên công việc
                  TextFormField(
                    key: const Key('user-checklist-task-text'),
                    controller: _titleController,
                    enabled: !_saving,
                    maxLength: 200,
                    textCapitalization: TextCapitalization.sentences,
                    decoration: InputDecoration(
                      labelText: 'Tên công việc *',
                      hintText: 'Ví dụ: Đi bộ 20 phút, Uống sữa hạt...',
                      filled: true,
                      fillColor: const Color(0xFFF6F1EC),
                      prefixIcon: const Icon(
                        Icons.edit_note_rounded,
                        color: Color(0xFF845143),
                      ),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(16),
                        borderSide: BorderSide.none,
                      ),
                      focusedBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(16),
                        borderSide: const BorderSide(
                          color: Color(0xFFC98C7B),
                          width: 2,
                        ),
                      ),
                    ),
                    validator: (value) => value == null || value.trim().isEmpty
                        ? 'Vui lòng nhập tên công việc.'
                        : null,
                  ),
                  const SizedBox(height: 16),

                  // 2. Mô tả công việc
                  TextFormField(
                    key: const Key('user-checklist-task-description'),
                    controller: _descriptionController,
                    enabled: !_saving,
                    maxLength: 500,
                    minLines: 2,
                    maxLines: 4,
                    textCapitalization: TextCapitalization.sentences,
                    decoration: InputDecoration(
                      labelText: 'Mô tả chi tiết (không bắt buộc)',
                      hintText: 'Thêm ghi chú, hướng dẫn hoặc lưu ý khi thực hiện...',
                      filled: true,
                      fillColor: const Color(0xFFF6F1EC),
                      alignLabelWithHint: true,
                      prefixIcon: const Padding(
                        padding: EdgeInsets.only(bottom: 36),
                        child: Icon(
                          Icons.description_outlined,
                          color: Color(0xFF845143),
                        ),
                      ),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(16),
                        borderSide: BorderSide.none,
                      ),
                      focusedBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(16),
                        borderSide: const BorderSide(
                          color: Color(0xFFC98C7B),
                          width: 2,
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),

                  // 3. Nhịp lặp
                  DropdownButtonFormField<TaskRecurrence>(
                    key: const Key('user-checklist-task-recurrence'),
                    initialValue: _recurrence,
                    decoration: InputDecoration(
                      labelText: 'Nhịp lặp',
                      filled: true,
                      fillColor: const Color(0xFFF6F1EC),
                      prefixIcon: Icon(
                        _recurrence.icon,
                        color: const Color(0xFF845143),
                      ),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(16),
                        borderSide: BorderSide.none,
                      ),
                      focusedBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(16),
                        borderSide: const BorderSide(
                          color: Color(0xFFC98C7B),
                          width: 2,
                        ),
                      ),
                    ),
                    items: TaskRecurrence.values
                        .map(
                          (rec) => DropdownMenuItem(
                            value: rec,
                            child: Row(
                              children: [
                                Icon(rec.icon, size: 18, color: const Color(0xFF5A463F)),
                                const SizedBox(width: 8),
                                Text(
                                  rec.label,
                                  style: const TextStyle(
                                    fontSize: 14,
                                    fontWeight: FontWeight.w500,
                                    color: Color(0xFF5A463F),
                                  ),
                                ),
                              ],
                            ),
                          ),
                        )
                        .toList(growable: false),
                    onChanged: _saving
                        ? null
                        : (value) {
                            if (value != null) {
                              setState(() => _recurrence = value);
                            }
                          },
                  ),
                  const SizedBox(height: 16),

                  // 4. Thời gian tồn tại
                  DropdownButtonFormField<TaskDurationOption>(
                    key: const Key('user-checklist-task-duration'),
                    initialValue: _durationOption,
                    decoration: InputDecoration(
                      labelText: 'Thời gian tồn tại',
                      filled: true,
                      fillColor: const Color(0xFFF6F1EC),
                      prefixIcon: const Icon(
                        Icons.timelapse_rounded,
                        color: Color(0xFF845143),
                      ),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(16),
                        borderSide: BorderSide.none,
                      ),
                      focusedBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(16),
                        borderSide: const BorderSide(
                          color: Color(0xFFC98C7B),
                          width: 2,
                        ),
                      ),
                    ),
                    items: TaskDurationOption.values
                        .map(
                          (opt) => DropdownMenuItem(
                            value: opt,
                            child: Text(
                              opt == TaskDurationOption.custom && _customEndDate != null
                                  ? 'Đến ngày ${_formatDate(_customEndDate!)}'
                                  : opt.label,
                              style: const TextStyle(
                                fontSize: 14,
                                fontWeight: FontWeight.w500,
                                color: Color(0xFF5A463F),
                              ),
                            ),
                          ),
                        )
                        .toList(growable: false),
                    onChanged: _saving
                        ? null
                        : (value) {
                            if (value != null) {
                              setState(() => _durationOption = value);
                              if (value == TaskDurationOption.custom) {
                                _pickCustomEndDate();
                              }
                            }
                          },
                  ),

                  if (_durationOption == TaskDurationOption.custom) ...[
                    const SizedBox(height: 10),
                    InkWell(
                      onTap: _saving ? null : _pickCustomEndDate,
                      borderRadius: BorderRadius.circular(12),
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                        decoration: BoxDecoration(
                          color: const Color(0xFFF2EAE4),
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(color: const Color(0xFFC98C7B).withAlpha(100)),
                        ),
                        child: Row(
                          children: [
                            const Icon(Icons.event_available_rounded, size: 18, color: Color(0xFF845143)),
                            const SizedBox(width: 8),
                            Expanded(
                              child: Text(
                                _customEndDate != null
                                    ? 'Ngày kết thúc: ${_formatDate(_customEndDate!)}'
                                    : 'Bấm để chọn ngày kết thúc',
                                style: const TextStyle(
                                  fontSize: 13,
                                  fontWeight: FontWeight.w600,
                                  color: Color(0xFF5A463F),
                                ),
                              ),
                            ),
                            const Icon(Icons.arrow_forward_ios_rounded, size: 14, color: Color(0xFF9C857C)),
                          ],
                        ),
                      ),
                    ),
                  ],

                  if (_errorMessage != null) ...[
                    const SizedBox(height: 16),
                    Semantics(
                      liveRegion: true,
                      child: Container(
                        width: double.infinity,
                        padding: const EdgeInsets.all(14),
                        decoration: BoxDecoration(
                          color: const Color(0xFFF2EAE4),
                          borderRadius: BorderRadius.circular(16),
                          border: const Border(
                            left: BorderSide(
                              color: Color(0xFFC98C7B),
                              width: 4,
                            ),
                          ),
                        ),
                        child: Text(
                          _errorMessage!,
                          style: const TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.w600,
                            color: Color(0xFF5A463F),
                          ),
                        ),
                      ),
                    ),
                  ],
                  const SizedBox(height: 24),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton.icon(
                      key: const Key('save-user-checklist-task'),
                      onPressed: _saving ? null : _submit,
                      icon: _saving
                          ? const SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(
                                strokeWidth: 2,
                                color: Colors.white,
                              ),
                            )
                          : const Icon(Icons.add_task_rounded),
                      label: Text(
                        _saving ? 'Đang thêm...' : 'Thêm vào hôm nay',
                      ),
                      style: FilledButton.styleFrom(
                        minimumSize: const Size.fromHeight(52),
                        backgroundColor: const Color(0xFFC98C7B),
                        foregroundColor: Colors.white,
                        shape: const StadiumBorder(),
                        textStyle: const TextStyle(
                          fontFamily: 'Quicksand',
                          fontSize: 16,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    final title = _titleController.text.trim();
    final description = _descriptionController.text.trim();
    final itemText = description.isNotEmpty ? '$title\n$description' : title;
    final payload = [
      itemText,
      _recurrence.name,
      _durationOption.name,
      _customEndDate?.toIso8601String() ?? '',
    ].join('\u0000');

    if (_lastPayload != payload || _clientTaskId == null) {
      _lastPayload = payload;
      _clientTaskId = widget.clientTaskIdFactory();
    }

    setState(() {
      _saving = true;
      _errorMessage = null;
    });
    try {
      await widget.service.addItemV2(
        itemText: itemText,
        clientTaskId: _clientTaskId!,
        category: ChecklistCategory.general,
        journeyId: widget.journeyId,
        babyId: widget.babyId,
        careGroupId: widget.careGroupId,
      );
      if (!mounted) return;
      Navigator.of(context).pop(true);
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _saving = false;
        _errorMessage = 'Không thể thêm việc. Vui lòng thử lại.';
      });
    }
  }
}
