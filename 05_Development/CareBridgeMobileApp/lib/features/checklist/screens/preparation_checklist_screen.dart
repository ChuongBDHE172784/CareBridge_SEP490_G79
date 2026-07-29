import 'package:flutter/material.dart';

import '../models/user_checklist_item_model.dart';
import '../services/user_checklist_service.dart';

class PreparationChecklistScreen extends StatefulWidget {
  const PreparationChecklistScreen({super.key, this.journeyId});

  final String? journeyId;

  @override
  State<PreparationChecklistScreen> createState() =>
      _PreparationChecklistScreenState();
}

class _PreparationChecklistScreenState
    extends State<PreparationChecklistScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Colors.white;
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _error = Color(0xFFBA1A1A);

  final _service = UserChecklistService.instance;
  final _itemController = TextEditingController();
  ChecklistCategory _selectedCategory = ChecklistCategory.general;
  List<UserChecklistItem> _items = [];
  final Set<String> _togglingIds = {};
  bool _loading = true;
  bool _saving = false;
  String? _errorText;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _itemController.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _errorText = null;
    });
    try {
      final items = await _service.listItems(journeyId: widget.journeyId);
      if (!mounted) return;
      setState(() {
        _items = items;
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _errorText = 'Không thể tải checklist.';
        _loading = false;
      });
    }
  }

  Future<void> _addItem() async {
    final text = _itemController.text.trim();
    if (text.isEmpty) return;
    setState(() => _saving = true);
    try {
      final created = await _service.addItem(
        itemText: text,
        category: _selectedCategory,
        journeyId: widget.journeyId,
        itemOrder: _items.length + 1,
      );
      if (!mounted) return;
      setState(() {
        _items = [..._items, created]..sort(_sortItems);
        _itemController.clear();
      });
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Không thể thêm mục: $e'),
          backgroundColor: _error,
        ),
      );
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  Future<void> _toggle(UserChecklistItem item) async {
    if (_togglingIds.contains(item.itemId)) return;
    setState(() => _togglingIds.add(item.itemId));
    try {
      final updated = await _service.toggleComplete(item.itemId);
      if (!mounted) return;
      setState(() {
        _items =
            _items
                .map(
                  (current) =>
                      current.itemId == updated.itemId ? updated : current,
                )
                .toList()
              ..sort(_sortItems);
      });
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Không thể cập nhật mục: $e'),
          backgroundColor: _error,
        ),
      );
    } finally {
      if (mounted) setState(() => _togglingIds.remove(item.itemId));
    }
  }

  int _sortItems(UserChecklistItem a, UserChecklistItem b) {
    final categoryCompare = a.category.apiValue.compareTo(b.category.apiValue);
    if (categoryCompare != 0) return categoryCompare;
    if (a.completed != b.completed) return a.completed ? 1 : -1;
    return a.itemOrder.compareTo(b.itemOrder);
  }

  @override
  Widget build(BuildContext context) {
    final completedCount = _items.where((item) => item.completed).length;
    return Scaffold(
      backgroundColor: _canvas,
      appBar: AppBar(
        backgroundColor: _canvas,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded, color: _onSurface),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'Checklist chuẩn bị',
          style: TextStyle(
            fontFamily: 'Lexend',
            color: _onSurface,
            fontWeight: FontWeight.w800,
          ),
        ),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator(color: _primary))
          : RefreshIndicator(
              onRefresh: _load,
              color: _primary,
              child: ListView(
                padding: const EdgeInsets.fromLTRB(20, 8, 20, 28),
                children: [
                  _ProgressHeader(
                    total: _items.length,
                    completed: completedCount,
                  ),
                  const SizedBox(height: 16),
                  if (_errorText != null)
                    _InlineError(message: _errorText!, onRetry: _load)
                  else if (_items.isEmpty)
                    const _EmptyState()
                  else
                    ..._buildGroupedSections(),
                  const SizedBox(height: 20),
                  _buildAddItemForm(),
                ],
              ),
            ),
    );
  }

  Widget? _buildCategorySection(ChecklistCategory category) {
    final items = _items
        .where((item) => item.templateName == null && item.category == category)
        .toList();
    if (items.isEmpty) {
      return null;
    }
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            category.label,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 15,
              fontWeight: FontWeight.w800,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 8),
          ...items.map(
            (item) => _ChecklistTile(
              item: item,
              busy: _togglingIds.contains(item.itemId),
              onChanged: () => _toggle(item),
            ),
          ),
        ],
      ),
    );
  }

  List<Widget> _buildGroupedSections() {
    final sections = <Widget>[];
    final imported = _items.where((item) => item.templateName != null).toList();
    final names = imported.map((item) => item.templateName!).toSet();
    for (final name in names) {
      final items = imported
          .where((item) => item.templateName == name)
          .toList();
      sections.add(_buildNamedSection(name, items));
    }
    for (final category in ChecklistCategory.values) {
      final section = _buildCategorySection(category);
      if (section != null) {
        sections.add(section);
      }
    }
    return sections;
  }

  Widget _buildNamedSection(String title, List<UserChecklistItem> items) =>
      Padding(
        padding: const EdgeInsets.only(bottom: 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 15,
                fontWeight: FontWeight.w800,
                color: _onSurface,
              ),
            ),
            const SizedBox(height: 8),
            ...items.map(
              (item) => _ChecklistTile(
                item: item,
                busy: _togglingIds.contains(item.itemId),
                onChanged: () => _toggle(item),
              ),
            ),
          ],
        ),
      );

  Widget _buildAddItemForm() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(18),
        boxShadow: [
          BoxShadow(
            color: _primary.withAlpha(16),
            blurRadius: 16,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Text(
            'Thêm mục checklist',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 16,
              fontWeight: FontWeight.w800,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 12),
          DropdownButtonFormField<ChecklistCategory>(
            initialValue: _selectedCategory,
            decoration: _inputDecoration('Danh mục'),
            items: ChecklistCategory.values
                .map(
                  (category) => DropdownMenuItem(
                    value: category,
                    child: Text(category.label),
                  ),
                )
                .toList(),
            onChanged: _saving
                ? null
                : (category) => setState(
                    () => _selectedCategory =
                        category ?? ChecklistCategory.general,
                  ),
          ),
          const SizedBox(height: 10),
          TextField(
            controller: _itemController,
            enabled: !_saving,
            minLines: 1,
            maxLines: 3,
            textInputAction: TextInputAction.done,
            decoration: _inputDecoration('Nội dung'),
            onSubmitted: (_) => _addItem(),
          ),
          const SizedBox(height: 12),
          FilledButton.icon(
            onPressed: _saving ? null : _addItem,
            icon: _saving
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: Colors.white,
                    ),
                  )
                : const Icon(Icons.add_task_rounded),
            label: const Text(
              'Thêm mục',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontWeight: FontWeight.w800,
              ),
            ),
            style: FilledButton.styleFrom(
              backgroundColor: _primary,
              minimumSize: const Size.fromHeight(50),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(15),
              ),
            ),
          ),
        ],
      ),
    );
  }

  InputDecoration _inputDecoration(String label) => InputDecoration(
    labelText: label,
    labelStyle: const TextStyle(fontFamily: 'Lexend', color: _onSurfaceVariant),
    filled: true,
    fillColor: const Color(0xFFFFF8F6),
    border: OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: BorderSide(color: _primaryContainer.withAlpha(70)),
    ),
    enabledBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: BorderSide(color: _primaryContainer.withAlpha(70)),
    ),
    focusedBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: const BorderSide(color: _primary, width: 1.5),
    ),
  );
}

class _ProgressHeader extends StatelessWidget {
  final int total;
  final int completed;

  const _ProgressHeader({required this.total, required this.completed});

  @override
  Widget build(BuildContext context) {
    final progress = total == 0 ? 0.0 : completed / total;
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(18),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.checklist_rounded, color: Color(0xFF845143)),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  '$completed/$total đã hoàn thành',
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontWeight: FontWeight.w800,
                    color: Color(0xFF271812),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          ClipRRect(
            borderRadius: BorderRadius.circular(999),
            child: LinearProgressIndicator(
              minHeight: 8,
              value: progress,
              backgroundColor: const Color(0xFFF2EAE4),
              color: const Color(0xFFC98C7B),
            ),
          ),
        ],
      ),
    );
  }
}

class _ChecklistTile extends StatelessWidget {
  final UserChecklistItem item;
  final bool busy;
  final VoidCallback onChanged;

  const _ChecklistTile({
    required this.item,
    required this.busy,
    required this.onChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: const Color(0xFFE8D8D2)),
      ),
      child: CheckboxListTile(
        value: item.completed,
        onChanged: busy ? null : (_) => onChanged(),
        activeColor: const Color(0xFF845143),
        checkboxShape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(5),
        ),
        title: Text(
          item.itemText,
          style: TextStyle(
            fontFamily: 'Lexend',
            color: const Color(0xFF271812),
            fontWeight: FontWeight.w700,
            decoration: item.completed ? TextDecoration.lineThrough : null,
          ),
        ),
        controlAffinity: ListTileControlAffinity.leading,
      ),
    );
  }
}

class _InlineError extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;

  const _InlineError({required this.message, required this.onRetry});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        children: [
          const Icon(Icons.error_outline_rounded, color: Color(0xFFBA1A1A)),
          const SizedBox(height: 8),
          Text(message, style: const TextStyle(fontFamily: 'Lexend')),
          TextButton(onPressed: onRetry, child: const Text('Thử lại')),
        ],
      ),
    );
  }
}

class _EmptyState extends StatelessWidget {
  const _EmptyState();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(18),
      ),
      child: const Column(
        children: [
          Icon(Icons.checklist_rtl_rounded, size: 40, color: Color(0xFF845143)),
          SizedBox(height: 10),
          Text(
            'Chưa có mục checklist nào.',
            style: TextStyle(
              fontFamily: 'Lexend',
              color: Color(0xFF524440),
              fontWeight: FontWeight.w700,
            ),
          ),
        ],
      ),
    );
  }
}
