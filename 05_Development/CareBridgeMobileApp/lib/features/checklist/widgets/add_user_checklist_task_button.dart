import 'package:flutter/material.dart';
import 'package:uuid/uuid.dart';

import '../models/user_checklist_item_model.dart';
import '../services/user_checklist_service.dart';

typedef UserChecklistTaskCreated = Future<void> Function();

/// Opens the Mother-only composer for a canonical V2 user-created task.
///
/// The entry is hidden unless exactly one authorized care context is supplied,
/// so callers never expose a button that can only fail server validation.
class AddUserChecklistTaskButton extends StatelessWidget {
  const AddUserChecklistTaskButton({
    super.key,
    this.journeyId,
    this.babyId,
    this.service,
    this.onCreated,
    this.clientTaskIdFactory,
  });

  final String? journeyId;
  final String? babyId;
  final UserChecklistService? service;
  final UserChecklistTaskCreated? onCreated;
  final String Function()? clientTaskIdFactory;

  bool get _hasOneContext =>
      (journeyId != null && journeyId!.isNotEmpty) !=
      (babyId != null && babyId!.isNotEmpty);

  @override
  Widget build(BuildContext context) {
    if (!_hasOneContext) return const SizedBox.shrink();

    return Semantics(
      button: true,
      label: 'Thêm việc do tôi tạo',
      child: OutlinedButton.icon(
        key: const Key('add-user-checklist-task'),
        onPressed: () => _openComposer(context),
        icon: const Icon(Icons.add_rounded),
        label: const Text('Thêm việc'),
        style: OutlinedButton.styleFrom(
          minimumSize: const Size(48, 48),
          foregroundColor: const Color(0xFF845143),
          backgroundColor: Colors.white,
          side: const BorderSide(color: Color(0xFFD6C2BD)),
          shape: const StadiumBorder(),
          textStyle: const TextStyle(
            fontFamily: 'Quicksand',
            fontWeight: FontWeight.w800,
          ),
        ),
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

class _UserChecklistTaskSheet extends StatefulWidget {
  const _UserChecklistTaskSheet({
    required this.journeyId,
    required this.babyId,
    required this.service,
    required this.clientTaskIdFactory,
  });

  final String? journeyId;
  final String? babyId;
  final UserChecklistService service;
  final String Function() clientTaskIdFactory;

  @override
  State<_UserChecklistTaskSheet> createState() =>
      _UserChecklistTaskSheetState();
}

class _UserChecklistTaskSheetState extends State<_UserChecklistTaskSheet> {
  final _formKey = GlobalKey<FormState>();
  final _textController = TextEditingController();
  ChecklistCategory _category = ChecklistCategory.general;
  String _targetSubject = 'MOTHER';
  bool _saving = false;
  String? _errorMessage;
  String? _lastPayload;
  String? _clientTaskId;

  @override
  void dispose() {
    _textController.dispose();
    super.dispose();
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
                      fontSize: 16,
                      height: 1.4,
                      color: Color(0xFF9C857C),
                    ),
                  ),
                  const SizedBox(height: 20),
                  TextFormField(
                    key: const Key('user-checklist-task-text'),
                    controller: _textController,
                    enabled: !_saving,
                    maxLength: 500,
                    minLines: 2,
                    maxLines: 4,
                    textCapitalization: TextCapitalization.sentences,
                    decoration: InputDecoration(
                      labelText: 'Nội dung công việc',
                      filled: true,
                      fillColor: const Color(0xFFF6F1EC),
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
                        ? 'Vui lòng nhập nội dung công việc.'
                        : null,
                  ),
                  const SizedBox(height: 12),
                  const Text(
                    'Dành cho',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w800,
                      color: Color(0xFF5A463F),
                    ),
                  ),
                  const SizedBox(height: 10),
                  Wrap(
                    spacing: 10,
                    runSpacing: 10,
                    children: [
                      _targetChip('MOTHER', 'Mẹ', Icons.pregnant_woman),
                      _targetChip('BABY', 'Bé', Icons.child_care_rounded),
                    ],
                  ),
                  const SizedBox(height: 18),
                  DropdownButtonFormField<ChecklistCategory>(
                    initialValue: _category,
                    decoration: InputDecoration(
                      labelText: 'Nhóm công việc',
                      filled: true,
                      fillColor: const Color(0xFFF6F1EC),
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(16),
                        borderSide: BorderSide.none,
                      ),
                    ),
                    items: ChecklistCategory.values
                        .map(
                          (category) => DropdownMenuItem(
                            value: category,
                            child: Text(category.label),
                          ),
                        )
                        .toList(growable: false),
                    onChanged: _saving
                        ? null
                        : (value) {
                            if (value != null) {
                              setState(() => _category = value);
                            }
                          },
                  ),
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
                            fontSize: 16,
                            fontWeight: FontWeight.w600,
                            color: Color(0xFF5A463F),
                          ),
                        ),
                      ),
                    ),
                  ],
                  const SizedBox(height: 22),
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

  Widget _targetChip(String value, String label, IconData icon) {
    final selected = _targetSubject == value;
    return Semantics(
      selected: selected,
      label: 'Đối tượng $label',
      child: ChoiceChip(
        key: Key('user-checklist-target-${value.toLowerCase()}'),
        label: Text(label),
        avatar: Icon(
          icon,
          size: 20,
          color: selected ? Colors.white : const Color(0xFF845143),
        ),
        selected: selected,
        onSelected: _saving
            ? null
            : (_) => setState(() => _targetSubject = value),
        selectedColor: const Color(0xFFC98C7B),
        backgroundColor: const Color(0xFFF2EAE4),
        labelStyle: TextStyle(
          fontWeight: FontWeight.w800,
          color: selected ? Colors.white : const Color(0xFF5A463F),
        ),
        side: const BorderSide(color: Color(0xFFE8DDD6)),
        shape: const StadiumBorder(),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      ),
    );
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    final itemText = _textController.text.trim();
    final payload = [
      itemText,
      _targetSubject,
      _category.apiValue,
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
      await widget.service.addItem(
        itemText: itemText,
        targetSubject: _targetSubject,
        clientTaskId: _clientTaskId!,
        category: _category,
        journeyId: widget.journeyId,
        babyId: widget.babyId,
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
