import 'package:flutter/material.dart';
import '../../checklist/models/user_checklist_item_model.dart';
import '../../checklist/services/user_checklist_service.dart';
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

class _ChecklistItemState {
  final String id;
  final String text;
  final bool completed;
  final String category;
  bool isSelected;

  _ChecklistItemState({
    required this.id,
    required this.text,
    required this.completed,
    required this.category,
    this.isSelected = true,
  });
}

class _ShareChecklistDialogState extends State<ShareChecklistDialog> {
  final TextEditingController _noteController = TextEditingController();
  bool _loading = true;
  int? _gestationalWeek;
  List<_ChecklistItemState> _items = [];

  @override
  void initState() {
    super.initState();
    _loadChecklist();
  }

  @override
  void dispose() {
    _noteController.dispose();
    super.dispose();
  }

  Future<void> _loadChecklist() async {
    try {
      final dashboard = await JourneyService().getDashboard();
      _gestationalWeek = dashboard.effectivePregnancyWeek ?? dashboard.completedGestationalWeek;

      final serverItems = await UserChecklistService.instance.listItems(
        journeyId: dashboard.journeyId,
      );

      if (serverItems.isNotEmpty) {
        if (mounted) {
          setState(() {
            _items = serverItems.map((item) {
              return _ChecklistItemState(
                id: item.itemId,
                text: item.itemText,
                completed: item.completed,
                category: item.category.label,
                isSelected: true,
              );
            }).toList();
            _loading = false;
          });
        }
      } else {
        _useDefaultItems();
      }
    } catch (_) {
      _useDefaultItems();
    }
  }

  void _useDefaultItems() {
    if (!mounted) return;
    setState(() {
      _items = [
        _ChecklistItemState(
          id: '1',
          text: 'Khám thai định kỳ & đo tim thai tuần ${_gestationalWeek ?? 28}',
          completed: true,
          category: 'Khám thai',
        ),
        _ChecklistItemState(
          id: '2',
          text: 'Xét nghiệm nghiệm pháp dung nạp đường huyết (OGTT)',
          completed: true,
          category: 'Xét nghiệm',
        ),
        _ChecklistItemState(
          id: '3',
          text: 'Tiêm ngừa uốn ván (VAT)',
          completed: true,
          category: 'Tiêm chủng',
        ),
        _ChecklistItemState(
          id: '4',
          text: 'Bổ sung viên sắt, canxi và DHA hàng ngày',
          completed: true,
          category: 'Dinh dưỡng',
        ),
        _ChecklistItemState(
          id: '5',
          text: 'Đếm cử động thai (thai máy) mỗi ngày 2 lần',
          completed: false,
          category: 'Theo dõi',
        ),
        _ChecklistItemState(
          id: '6',
          text: 'Chuẩn bị giỏ đồ đi sinh và hồ sơ đăng ký sinh',
          completed: false,
          category: 'Đi sinh',
        ),
      ];
      _loading = false;
    });
  }

  void _onConfirm() {
    final selected = _items.where((i) => i.isSelected).toList();

    if (selected.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Vui lòng chọn ít nhất 1 việc để chia sẻ')),
      );
      return;
    }

    final total = selected.length;
    final completedCount = selected.where((i) => i.completed).length;
    final percent = total > 0 ? ((completedCount / total) * 100).round() : 0;

    final shareData = ChecklistShareData(
      title: 'Danh sách việc cần làm (Checklist)',
      gestationalWeek: _gestationalWeek,
      completedCount: completedCount,
      totalCount: total,
      progressPercent: percent,
      note: _noteController.text.trim().isEmpty ? null : _noteController.text.trim(),
      items: selected
          .map(
            (i) => ChecklistItemShareData(
              text: i.text,
              completed: i.completed,
              category: i.category,
            ),
          )
          .toList(),
    );

    Navigator.of(context).pop(shareData);
  }

  @override
  Widget build(BuildContext context) {
    const primary = Color(0xFFC98C7B);

    final selectedCount = _items.where((i) => i.isSelected).length;
    final completedCount = _items.where((i) => i.isSelected && i.completed).length;

    return Padding(
      padding: EdgeInsets.only(
        bottom: MediaQuery.of(context).viewInsets.bottom,
        left: 20,
        right: 20,
        top: 20,
      ),
      child: SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
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
            const SizedBox(height: 16),

            // Title
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: primary.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: const Icon(
                    Icons.checklist_rtl_rounded,
                    color: primary,
                    size: 22,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Chia sẻ danh sách việc cần làm',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 17,
                          fontWeight: FontWeight.bold,
                          color: Color(0xFF2C2523),
                        ),
                      ),
                      Text(
                        _gestationalWeek != null
                            ? 'Tuần thai $_gestationalWeek · Đã xong $completedCount/$selectedCount việc'
                            : 'Gửi tiến độ chăm sóc cho chuyên gia',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 12,
                          color: Color(0xFF7A6F6C),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),

            if (_loading)
              const Center(
                child: Padding(
                  padding: EdgeInsets.all(32),
                  child: CircularProgressIndicator(color: primary),
                ),
              )
            else ...[
              // Checklist items
              Flexible(
                child: Container(
                  constraints: const BoxConstraints(maxHeight: 280),
                  decoration: BoxDecoration(
                    color: const Color(0xFFFAF7F6),
                    borderRadius: BorderRadius.circular(14),
                    border: Border.all(color: const Color(0xFFE8D5CE)),
                  ),
                  child: ListView.separated(
                    shrinkWrap: true,
                    itemCount: _items.length,
                    separatorBuilder: (ctx, index) => const Divider(
                      height: 1,
                      color: Color(0xFFECE4E1),
                    ),
                    itemBuilder: (ctx, idx) {
                      final item = _items[idx];
                      return CheckboxListTile(
                        value: item.isSelected,
                        activeColor: primary,
                        dense: true,
                        contentPadding: const EdgeInsets.symmetric(horizontal: 12),
                        title: Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Icon(
                              item.completed
                                  ? Icons.check_circle_rounded
                                  : Icons.radio_button_unchecked_rounded,
                              size: 15,
                              color: item.completed
                                  ? const Color(0xFF2E7D32)
                                  : const Color(0xFFB0A4A1),
                            ),
                            const SizedBox(width: 6),
                            Expanded(
                              child: Text(
                                item.text,
                                style: TextStyle(
                                  fontFamily: 'Lexend',
                                  fontSize: 12,
                                  fontWeight: FontWeight.w500,
                                  color: item.completed
                                      ? const Color(0xFF2C2523)
                                      : const Color(0xFF5A4E4B),
                                ),
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
                ),
              ),
              const SizedBox(height: 12),

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
                    borderSide: const BorderSide(color: primary, width: 1.5),
                  ),
                ),
              ),
              const SizedBox(height: 16),

              // Send button
              FilledButton.icon(
                onPressed: _onConfirm,
                icon: const Icon(Icons.send_rounded, size: 18),
                label: const Text(
                  'Gửi checklist cho chuyên gia',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontWeight: FontWeight.bold,
                    fontSize: 14,
                  ),
                ),
                style: FilledButton.styleFrom(
                  backgroundColor: primary,
                  minimumSize: const Size.fromHeight(48),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
              ),
              const SizedBox(height: 12),
            ],
          ],
        ),
      ),
    );
  }
}
