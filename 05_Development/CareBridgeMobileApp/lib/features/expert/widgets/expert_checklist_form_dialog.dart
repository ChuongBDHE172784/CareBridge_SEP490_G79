import 'package:flutter/material.dart';

import '../../directChat/models/checklist_share_data.dart';
import '../../reminder/models/today_task_support_function.dart';

enum ExpertChecklistFormMode { add, edit }

class ExpertChecklistFormDialog extends StatefulWidget {
  final ExpertChecklistFormMode mode;
  final ChecklistItemShareData? initialItem;
  final String initialTargetGroup;
  final Future<void> Function({
    required String text,
    required String targetGroup,
    required String category,
    required String? timeLabel,
    required String? doctorNote,
    required String? supportFunction,
    required bool completed,
    required String? sourceUrl,
  }) onSave;

  const ExpertChecklistFormDialog({
    super.key,
    required this.mode,
    this.initialItem,
    this.initialTargetGroup = 'CURRENT',
    required this.onSave,
  });

  static Future<void> show(
    BuildContext context, {
    required ExpertChecklistFormMode mode,
    ChecklistItemShareData? initialItem,
    String initialTargetGroup = 'CURRENT',
    required Future<void> Function({
      required String text,
      required String targetGroup,
      required String category,
      required String? timeLabel,
      required String? doctorNote,
      required String? supportFunction,
      required bool completed,
      required String? sourceUrl,
    }) onSave,
  }) {
    return showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (ctx) => Padding(
        padding: EdgeInsets.only(
          bottom: MediaQuery.of(ctx).viewInsets.bottom,
        ),
        child: ExpertChecklistFormDialog(
          mode: mode,
          initialItem: initialItem,
          initialTargetGroup: initialTargetGroup,
          onSave: onSave,
        ),
      ),
    );
  }

  @override
  State<ExpertChecklistFormDialog> createState() => _ExpertChecklistFormDialogState();
}

class _ExpertChecklistFormDialogState extends State<ExpertChecklistFormDialog> {
  static const _primary = Color(0xFF845143);
  static const _onSurface = Color(0xFF2A211D);
  static const _outlineVariant = Color(0xFFE5D3CA);

  static const _categories = [
    'Khám thai & Y tế',
    'Dinh dưỡng & Bổ sung',
    'Xét nghiệm & Sàng lọc',
    'Tiêm chủng',
    'Vận động & Lối sống',
    'Chuẩn bị sinh & Hậu cần',
    'Chăm sóc sau sinh',
  ];

  late TextEditingController _textController;
  late TextEditingController _timeLabelController;
  late TextEditingController _doctorNoteController;
  late TextEditingController _sourceUrlController;

  late String _targetGroup;
  late String _category;
  String? _supportFunction;
  bool _completed = false;

  bool _isSubmitting = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    final item = widget.initialItem;
    _textController = TextEditingController(text: item?.text ?? '');
    _timeLabelController = TextEditingController(text: item?.timeLabel ?? '');
    _doctorNoteController = TextEditingController(text: item?.doctorNote ?? '');
    _sourceUrlController = TextEditingController(text: item?.sourceUrl ?? '');

    _targetGroup = widget.initialTargetGroup;
    _category = item?.category != null && _categories.contains(item!.category)
        ? item.category!
        : _categories.first;
    _supportFunction = item?.supportFunction;
    _completed = item?.completed ?? false;
  }

  @override
  void dispose() {
    _textController.dispose();
    _timeLabelController.dispose();
    _doctorNoteController.dispose();
    _sourceUrlController.dispose();
    super.dispose();
  }

  Future<void> _handleSubmit() async {
    final text = _textController.text.trim();
    if (text.isEmpty) {
      setState(() => _errorMessage = 'Vui lòng nhập tên công việc cần làm.');
      return;
    }

    setState(() {
      _isSubmitting = true;
      _errorMessage = null;
    });

    try {
      await widget.onSave(
        text: text,
        targetGroup: _targetGroup,
        category: _category,
        timeLabel: _timeLabelController.text.trim().isNotEmpty
            ? _timeLabelController.text.trim()
            : null,
        doctorNote: _doctorNoteController.text.trim().isNotEmpty
            ? _doctorNoteController.text.trim()
            : null,
        supportFunction: _supportFunction,
        completed: _completed,
        sourceUrl: _sourceUrlController.text.trim().isNotEmpty
            ? _sourceUrlController.text.trim()
            : null,
      );
      if (mounted) Navigator.of(context).pop();
    } catch (e) {
      if (mounted) {
        setState(() {
          _isSubmitting = false;
          _errorMessage = 'Lỗi lưu công việc: $e';
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final isAdd = widget.mode == ExpertChecklistFormMode.add;

    return Container(
      constraints: BoxConstraints(
        maxHeight: MediaQuery.of(context).size.height * 0.85,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // Drag handle
          Container(
            margin: const EdgeInsets.symmetric(vertical: 10),
            width: 40,
            height: 4,
            decoration: BoxDecoration(
              color: Colors.grey.shade300,
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          // Title bar
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 4),
            child: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: const Color(0xFFE0F2F1),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: const Icon(
                    Icons.medical_services_outlined,
                    color: Color(0xFF00695C),
                    size: 20,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        isAdd
                            ? 'Bác sĩ chỉ định việc cần làm'
                            : 'Chỉnh sửa chỉ định của Bác sĩ',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 16,
                          fontWeight: FontWeight.w700,
                          color: _onSurface,
                        ),
                      ),
                      const Text(
                        'Việc này sẽ được đồng bộ trực tiếp vào lộ trình của mẹ bầu',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 11,
                          color: Color(0xFF7A6F6C),
                        ),
                      ),
                    ],
                  ),
                ),
                IconButton(
                  icon: const Icon(Icons.close, color: Color(0xFF7A6F6C)),
                  onPressed: () => Navigator.of(context).pop(),
                ),
              ],
            ),
          ),
          const Divider(color: _outlineVariant, height: 1),
          // Form content
          Flexible(
            child: ListView(
              padding: const EdgeInsets.fromLTRB(20, 16, 20, 16),
              children: [
                if (_errorMessage != null)
                  Container(
                    margin: const EdgeInsets.only(bottom: 12),
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: const Color(0xFFFFEBEE),
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: const Color(0xFFFFCDD2)),
                    ),
                    child: Text(
                      _errorMessage!,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        color: Color(0xFFC62828),
                      ),
                    ),
                  ),
                // Tên công việc
                const Text(
                  'Tên công việc / Hướng dẫn y khoa *',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: _onSurface,
                  ),
                ),
                const SizedBox(height: 6),
                TextField(
                  controller: _textController,
                  style: const TextStyle(fontFamily: 'Lexend', fontSize: 13),
                  decoration: InputDecoration(
                    hintText: 'VD: Siêu âm 4D hình thái học thai nhi',
                    hintStyle: const TextStyle(color: Colors.black38, fontSize: 13),
                    contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: const BorderSide(color: _outlineVariant),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: const BorderSide(color: _primary, width: 1.5),
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                // Nhóm mục tiêu
                const Text(
                  'Giai đoạn thực hiện',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: _onSurface,
                  ),
                ),
                const SizedBox(height: 6),
                SegmentedButton<String>(
                  segments: const [
                    ButtonSegment(value: 'HISTORY', label: Text('Đã làm')),
                    ButtonSegment(value: 'CURRENT', label: Text('Hiện tại')),
                    ButtonSegment(value: 'FUTURE', label: Text('Tương lai')),
                  ],
                  selected: {_targetGroup},
                  onSelectionChanged: (set) {
                    setState(() => _targetGroup = set.first);
                  },
                  style: ButtonStyle(
                    textStyle: WidgetStateProperty.all(
                      const TextStyle(fontFamily: 'Lexend', fontSize: 12, fontWeight: FontWeight.w600),
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                // Danh mục
                const Text(
                  'Phân loại danh mục',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: _onSurface,
                  ),
                ),
                const SizedBox(height: 6),
                DropdownButtonFormField<String>(
                  initialValue: _category,
                  style: const TextStyle(fontFamily: 'Lexend', fontSize: 13, color: _onSurface),
                  decoration: InputDecoration(
                    contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: const BorderSide(color: _outlineVariant),
                    ),
                  ),
                  items: _categories.map((c) {
                    return DropdownMenuItem(value: c, child: Text(c));
                  }).toList(),
                  onChanged: (val) {
                    if (val != null) setState(() => _category = val);
                  },
                ),
                const SizedBox(height: 16),
                // Nhãn thời gian
                const Text(
                  'Nhãn thời gian / Tuần thai đề xuất',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: _onSurface,
                  ),
                ),
                const SizedBox(height: 6),
                TextField(
                  controller: _timeLabelController,
                  style: const TextStyle(fontFamily: 'Lexend', fontSize: 13),
                  decoration: InputDecoration(
                    hintText: 'VD: Tuần 22, hoặc Khẩn cấp',
                    hintStyle: const TextStyle(color: Colors.black38, fontSize: 13),
                    contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: const BorderSide(color: _outlineVariant),
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                // Lời dặn Bác sĩ
                const Text(
                  'Lời dặn của Bác sĩ (hiển thị nổi bật)',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: Color(0xFF00695C),
                  ),
                ),
                const SizedBox(height: 6),
                TextField(
                  controller: _doctorNoteController,
                  maxLines: 2,
                  style: const TextStyle(fontFamily: 'Lexend', fontSize: 13),
                  decoration: InputDecoration(
                    hintText: 'VD: Nhịn ăn sáng trước khi làm xét nghiệm dung nạp đường huyết.',
                    hintStyle: const TextStyle(color: Colors.black38, fontSize: 13),
                    contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: const BorderSide(color: _outlineVariant),
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                // Tính năng hỗ trợ CareBridge
                const Text(
                  'Liên kết tính năng CareBridge (tùy chọn)',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: _onSurface,
                  ),
                ),
                const SizedBox(height: 6),
                DropdownButtonFormField<String?>(
                  initialValue: _supportFunction,
                  style: const TextStyle(fontFamily: 'Lexend', fontSize: 13, color: _onSurface),
                  decoration: InputDecoration(
                    contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: const BorderSide(color: _outlineVariant),
                    ),
                  ),
                  items: [
                    const DropdownMenuItem(value: null, child: Text('Không liên kết')),
                    ...TodayTaskSupportFunction.values.map((f) {
                      return DropdownMenuItem(
                        value: f.apiValue,
                        child: Text(f.label),
                      );
                    }),
                  ],
                  onChanged: (val) => setState(() => _supportFunction = val),
                ),
                if (!isAdd) ...[
                  const SizedBox(height: 12),
                  CheckboxListTile(
                    title: const Text(
                      'Đánh dấu mẹ bầu đã hoàn thành việc này',
                      style: TextStyle(fontFamily: 'Lexend', fontSize: 13),
                    ),
                    value: _completed,
                    activeColor: _primary,
                    contentPadding: EdgeInsets.zero,
                    controlAffinity: ListTileControlAffinity.leading,
                    onChanged: (val) => setState(() => _completed = val ?? false),
                  ),
                ],
              ],
            ),
          ),
          // Actions
          Container(
            padding: const EdgeInsets.fromLTRB(20, 12, 20, 20),
            decoration: const BoxDecoration(
              border: Border(top: BorderSide(color: _outlineVariant)),
            ),
            child: Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: _isSubmitting ? null : () => Navigator.of(context).pop(),
                    style: OutlinedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      side: const BorderSide(color: _outlineVariant),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                    ),
                    child: const Text(
                      'Hủy',
                      style: TextStyle(fontFamily: 'Lexend', color: Color(0xFF7A6F6C)),
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: ElevatedButton(
                    onPressed: _isSubmitting ? null : _handleSubmit,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: _primary,
                      foregroundColor: Colors.white,
                      elevation: 0,
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                    ),
                    child: _isSubmitting
                        ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: Colors.white,
                            ),
                          )
                        : Text(
                            isAdd ? 'Thêm vào lộ trình' : 'Lưu thay đổi',
                            style: const TextStyle(
                              fontFamily: 'Lexend',
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
