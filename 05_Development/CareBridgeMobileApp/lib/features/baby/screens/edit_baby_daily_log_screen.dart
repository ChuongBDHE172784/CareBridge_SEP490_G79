import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../models/baby_daily_log_model.dart';
import '../services/baby_log_service.dart';

class EditBabyDailyLogScreen extends StatefulWidget {
  final String babyId;
  final String logId;
  final BabyDailyLog? initialLog;

  const EditBabyDailyLogScreen({
    super.key,
    required this.babyId,
    required this.logId,
    this.initialLog,
  });

  @override
  State<EditBabyDailyLogScreen> createState() => _EditBabyDailyLogScreenState();
}

class _EditBabyDailyLogScreenState extends State<EditBabyDailyLogScreen>
    with SingleTickerProviderStateMixin {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Color(0xFFF2EAE4);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  late TabController _tabController;
  final _service = BabyLogService();

  LogType _selectedType = LogType.feeding;
  DateTime _startedAt = DateTime.now();
  DateTime? _endedAt;

  // Feeding fields
  final _quantityCtrl = TextEditingController();
  String _feedingUnit = 'ml';

  // Sleep fields
  final _sleepDurationCtrl = TextEditingController();
  String _sleepQuality = 'Tốt';

  // Diaper fields
  String _diaperStatus = 'Khô';

  // Symptom fields
  final _temperatureCtrl = TextEditingController();
  final _symptomsCtrl = TextEditingController();

  // Common
  final _noteCtrl = TextEditingController();

  bool _isSaving = false;
  bool _showSuccess = false;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 4, vsync: this);
    _tabController.addListener(() {
      if (!_tabController.indexIsChanging) {
        setState(() => _selectedType = LogType.values[_tabController.index]);
      }
    });
    _prefillFromLog();
  }

  void _prefillFromLog() {
    final log = widget.initialLog;
    if (log == null) return;
    _selectedType = log.logType;
    _tabController.index = log.logType.index;
    _startedAt = log.startedAt ?? DateTime.now();
    _endedAt = log.endedAt;
    if (log.quantity != null) {
      _quantityCtrl.text = log.quantity!.toStringAsFixed(0);
    }
    if (log.unit != null) _feedingUnit = log.unit!;
    if (log.note != null) _noteCtrl.text = log.note!;
  }

  @override
  void dispose() {
    _tabController.dispose();
    _quantityCtrl.dispose();
    _sleepDurationCtrl.dispose();
    _temperatureCtrl.dispose();
    _symptomsCtrl.dispose();
    _noteCtrl.dispose();
    super.dispose();
  }

  Future<void> _pickDateTime({required bool isStart}) async {
    final now = DateTime.now();
    final initial = isStart ? _startedAt : (_endedAt ?? now);

    final date = await showDatePicker(
      context: context,
      initialDate: initial,
      firstDate: now.subtract(const Duration(days: 30)),
      lastDate: now,
      builder: (ctx, child) => Theme(
        data: Theme.of(ctx).copyWith(
          colorScheme: const ColorScheme.light(
            primary: _primary,
            onPrimary: Colors.white,
            surface: _canvas,
          ),
        ),
        child: child!,
      ),
    );
    if (date == null || !mounted) return;

    final time = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.fromDateTime(initial),
      builder: (ctx, child) => Theme(
        data: Theme.of(ctx).copyWith(
          colorScheme: const ColorScheme.light(
            primary: _primary,
            onPrimary: Colors.white,
            surface: _canvas,
          ),
        ),
        child: child!,
      ),
    );
    if (time == null) return;

    final dt = DateTime(
      date.year,
      date.month,
      date.day,
      time.hour,
      time.minute,
    );
    setState(() {
      if (isStart) {
        _startedAt = dt;
      } else {
        _endedAt = dt;
      }
    });
  }

  Future<void> _save() async {
    setState(() => _isSaving = true);
    try {
      final note = _buildNoteFromFields();
      await _service.updateDailyLog(
        widget.babyId,
        widget.logId,
        UpdateBabyDailyLogRequest(
          startedAt: _startedAt,
          endedAt: _endedAt,
          quantity: _selectedType == LogType.feeding
              ? double.tryParse(_quantityCtrl.text.trim())
              : null,
          unit: _selectedType == LogType.feeding ? _feedingUnit : null,
          note: note.isNotEmpty ? note : _noteCtrl.text.trim(),
        ),
      );
      setState(() => _showSuccess = true);
      await Future.delayed(const Duration(seconds: 2));
      if (mounted) Navigator.of(context).pop(true);
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Không thể lưu. Vui lòng thử lại.'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  String _buildNoteFromFields() {
    switch (_selectedType) {
      case LogType.sleep:
        final parts = <String>[];
        if (_sleepDurationCtrl.text.isNotEmpty) {
          parts.add('Thời gian: ${_sleepDurationCtrl.text} phút');
        }
        parts.add('Chất lượng: $_sleepQuality');
        if (_noteCtrl.text.isNotEmpty) parts.add(_noteCtrl.text);
        return parts.join(' | ');
      case LogType.diaper:
        final parts = ['Trạng thái: $_diaperStatus'];
        if (_noteCtrl.text.isNotEmpty) parts.add(_noteCtrl.text);
        return parts.join(' | ');
      case LogType.symptom:
        final parts = <String>[];
        if (_temperatureCtrl.text.isNotEmpty) {
          parts.add('Nhiệt độ: ${_temperatureCtrl.text}°C');
        }
        if (_symptomsCtrl.text.isNotEmpty) {
          parts.add('Triệu chứng: ${_symptomsCtrl.text}');
        }
        if (_noteCtrl.text.isNotEmpty) parts.add(_noteCtrl.text);
        return parts.join(' | ');
      default:
        return _noteCtrl.text.trim();
    }
  }

  Future<void> _confirmDelete() async {
    showModalBottomSheet<void>(
      context: context,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(32)),
      ),
      builder: (_) => _DeleteConfirmSheet(
        onConfirm: () async {
          Navigator.of(context).pop();
          try {
            await _service.deleteDailyLog(widget.babyId, widget.logId);
            if (mounted) Navigator.of(context).pop('deleted');
          } catch (_) {
            if (mounted) {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  content: Text('Không thể xóa. Vui lòng thử lại.'),
                  backgroundColor: Colors.red,
                ),
              );
            }
          }
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      appBar: AppBar(
        backgroundColor: _canvas,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded, color: _onSurface),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: const Text(
          'Chỉnh sửa nhật ký',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 18,
            fontWeight: FontWeight.w700,
            color: _onSurface,
          ),
        ),
      ),
      body: Stack(
        children: [
          Column(
            children: [
              _buildTabBar(),
              Expanded(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.fromLTRB(16, 16, 16, 40),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      _buildDateTimeSection(),
                      const SizedBox(height: 16),
                      _buildDynamicFields(),
                      const SizedBox(height: 16),
                      _buildNotesField(),
                      const SizedBox(height: 24),
                      _buildActionButtons(),
                    ],
                  ),
                ),
              ),
            ],
          ),
          if (_showSuccess) _buildSuccessToast(),
        ],
      ),
    );
  }

  Widget _buildTabBar() {
    return Container(
      color: _canvas,
      child: TabBar(
        controller: _tabController,
        labelStyle: const TextStyle(
          fontFamily: 'Lexend',
          fontSize: 11,
          fontWeight: FontWeight.w600,
        ),
        unselectedLabelStyle: const TextStyle(
          fontFamily: 'Lexend',
          fontSize: 11,
        ),
        labelColor: _primary,
        unselectedLabelColor: _onSurfaceVariant,
        indicatorColor: _primary,
        indicatorWeight: 2.5,
        tabs: LogType.values.map((t) => Tab(text: t.displayLabel)).toList(),
      ),
    );
  }

  Widget _buildDateTimeSection() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: _primary.withAlpha(15),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Thời gian',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: _DateTimeChip(
                  label: 'Bắt đầu',
                  dateTime: _startedAt,
                  onTap: () => _pickDateTime(isStart: true),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _DateTimeChip(
                  label: 'Kết thúc',
                  dateTime: _endedAt,
                  onTap: () => _pickDateTime(isStart: false),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildDynamicFields() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: _primary.withAlpha(15),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: AnimatedSwitcher(
        duration: const Duration(milliseconds: 200),
        child: KeyedSubtree(
          key: ValueKey(_selectedType),
          child: _fieldsByType(),
        ),
      ),
    );
  }

  Widget _fieldsByType() {
    switch (_selectedType) {
      case LogType.feeding:
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Lượng bú / ăn',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: _onSurface,
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  flex: 2,
                  child: TextFormField(
                    controller: _quantityCtrl,
                    keyboardType: TextInputType.number,
                    inputFormatters: [
                      FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
                    ],
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 15,
                      color: _onSurface,
                    ),
                    decoration: _inputDeco('Lượng', hint: '150'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _ChipSelector(
                    options: const ['ml', 'oz', 'lần'],
                    selected: _feedingUnit,
                    onSelected: (v) => setState(() => _feedingUnit = v),
                  ),
                ),
              ],
            ),
          ],
        );
      case LogType.sleep:
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Giấc ngủ',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: _onSurface,
              ),
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _sleepDurationCtrl,
              keyboardType: TextInputType.number,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 15,
                color: _onSurface,
              ),
              decoration: _inputDeco('Thời gian ngủ (phút)', hint: '60'),
            ),
            const SizedBox(height: 12),
            const Text(
              'Chất lượng',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 12,
                color: _onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 8),
            _ChipSelector(
              options: const ['Tốt', 'Trung bình', 'Kém'],
              selected: _sleepQuality,
              onSelected: (v) => setState(() => _sleepQuality = v),
            ),
          ],
        );
      case LogType.diaper:
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Trạng thái tã',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: _onSurface,
              ),
            ),
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: ['Khô', 'Tiểu', 'Đại tiện', 'Hỗn hợp'].map((s) {
                final selected = _diaperStatus == s;
                return GestureDetector(
                  onTap: () => setState(() => _diaperStatus = s),
                  child: AnimatedContainer(
                    duration: const Duration(milliseconds: 150),
                    padding: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 10,
                    ),
                    decoration: BoxDecoration(
                      color: selected ? _primary : _surface,
                      borderRadius: BorderRadius.circular(50),
                    ),
                    child: Text(
                      s,
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 13,
                        fontWeight: FontWeight.w600,
                        color: selected ? Colors.white : _onSurface,
                      ),
                    ),
                  ),
                );
              }).toList(),
            ),
          ],
        );
      case LogType.symptom:
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Chỉ số sức khỏe',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: _onSurface,
              ),
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _temperatureCtrl,
              keyboardType: const TextInputType.numberWithOptions(
                decimal: true,
              ),
              inputFormatters: [
                FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
              ],
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 15,
                color: _onSurface,
              ),
              decoration: _inputDeco('Nhiệt độ (°C)', hint: '37.0'),
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _symptomsCtrl,
              maxLines: 2,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                color: _onSurface,
              ),
              decoration: _inputDeco(
                'Triệu chứng',
                hint: 'Sốt nhẹ, ho, sổ mũi...',
              ),
            ),
          ],
        );
    }
  }

  Widget _buildNotesField() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: _primary.withAlpha(15),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: TextFormField(
        controller: _noteCtrl,
        maxLines: 3,
        maxLength: 500,
        style: const TextStyle(
          fontFamily: 'Lexend',
          fontSize: 14,
          color: _onSurface,
        ),
        decoration: _inputDeco('Ghi chú thêm', hint: 'Nhập ghi chú...'),
      ),
    );
  }

  Widget _buildActionButtons() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        ElevatedButton(
          onPressed: _isSaving ? null : _save,
          style: ElevatedButton.styleFrom(
            backgroundColor: _primary,
            foregroundColor: Colors.white,
            padding: const EdgeInsets.symmetric(vertical: 16),
            shape: const StadiumBorder(),
            elevation: 0,
          ),
          child: _isSaving
              ? const SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(
                    strokeWidth: 2.5,
                    color: Colors.white,
                  ),
                )
              : const Text(
                  'Lưu thay đổi',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                  ),
                ),
        ),
        const SizedBox(height: 12),
        OutlinedButton.icon(
          onPressed: _confirmDelete,
          icon: const Icon(Icons.delete_outline_rounded, size: 18),
          label: const Text(
            'Xóa nhật ký',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 15,
              fontWeight: FontWeight.w600,
            ),
          ),
          style: OutlinedButton.styleFrom(
            foregroundColor: Colors.red,
            side: const BorderSide(color: Colors.red, width: 1.5),
            padding: const EdgeInsets.symmetric(vertical: 14),
            shape: const StadiumBorder(),
          ),
        ),
      ],
    );
  }

  Widget _buildSuccessToast() {
    return Positioned.fill(
      child: Container(
        color: Colors.black.withAlpha(100),
        child: Center(
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 20),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(24),
              boxShadow: [
                BoxShadow(color: Colors.black.withAlpha(40), blurRadius: 20),
              ],
            ),
            child: const Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(Icons.check_circle_rounded, color: _primary, size: 48),
                SizedBox(height: 12),
                Text(
                  'Đã lưu thành công!',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                    color: _onSurface,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  InputDecoration _inputDeco(String label, {String? hint}) => InputDecoration(
    labelText: label,
    hintText: hint,
    labelStyle: const TextStyle(
      fontFamily: 'Lexend',
      fontSize: 12,
      color: _onSurfaceVariant,
    ),
    hintStyle: const TextStyle(
      fontFamily: 'Lexend',
      fontSize: 13,
      color: Color(0xFFBBA9A4),
    ),
    filled: true,
    fillColor: Colors.white,
    counterText: '',
    contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
    border: OutlineInputBorder(
      borderRadius: BorderRadius.circular(16),
      borderSide: const BorderSide(color: _surface, width: 2),
    ),
    enabledBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(16),
      borderSide: const BorderSide(color: _surface, width: 2),
    ),
    focusedBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(16),
      borderSide: const BorderSide(color: _primaryContainer, width: 2),
    ),
  );
}

class _DateTimeChip extends StatelessWidget {
  const _DateTimeChip({
    required this.label,
    required this.dateTime,
    required this.onTap,
  });

  final String label;
  final DateTime? dateTime;
  final VoidCallback onTap;

  static const _primary = Color(0xFF845143);
  static const _surface = Color(0xFFF2EAE4);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
        decoration: BoxDecoration(
          color: _surface,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: _surface, width: 1.5),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              label,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 10,
                color: _onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 4),
            Row(
              children: [
                const Icon(Icons.schedule_rounded, size: 14, color: _primary),
                const SizedBox(width: 4),
                Expanded(
                  child: Text(
                    dateTime != null
                        ? '${dateTime!.day.toString().padLeft(2, '0')}/${dateTime!.month.toString().padLeft(2, '0')} ${dateTime!.hour.toString().padLeft(2, '0')}:${dateTime!.minute.toString().padLeft(2, '0')}'
                        : 'Chọn giờ',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                      color: dateTime != null ? _onSurface : _onSurfaceVariant,
                    ),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _ChipSelector extends StatelessWidget {
  const _ChipSelector({
    required this.options,
    required this.selected,
    required this.onSelected,
  });

  final List<String> options;
  final String selected;
  final ValueChanged<String> onSelected;

  static const _primary = Color(0xFF845143);
  static const _surface = Color(0xFFF2EAE4);
  static const _onSurface = Color(0xFF271812);

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 8,
      children: options.map((o) {
        final isSelected = o == selected;
        return GestureDetector(
          onTap: () => onSelected(o),
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 150),
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
            decoration: BoxDecoration(
              color: isSelected ? _primary : _surface,
              borderRadius: BorderRadius.circular(50),
            ),
            child: Text(
              o,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 12,
                fontWeight: FontWeight.w600,
                color: isSelected ? Colors.white : _onSurface,
              ),
            ),
          ),
        );
      }).toList(),
    );
  }
}

class _DeleteConfirmSheet extends StatelessWidget {
  const _DeleteConfirmSheet({required this.onConfirm});

  final VoidCallback onConfirm;

  static const _primary = Color(0xFF845143);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(24, 24, 24, 16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: const Color(0xFFE0D8D5),
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            const SizedBox(height: 20),
            Container(
              width: 64,
              height: 64,
              decoration: BoxDecoration(
                color: const Color(0xFFFFEDEA),
                borderRadius: BorderRadius.circular(20),
              ),
              child: const Icon(
                Icons.error_outline_rounded,
                color: Colors.red,
                size: 32,
              ),
            ),
            const SizedBox(height: 16),
            const Text(
              'Xóa nhật ký này?',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 18,
                fontWeight: FontWeight.w700,
                color: _onSurface,
              ),
            ),
            const SizedBox(height: 8),
            const Text(
              'Hành động này không thể hoàn tác. Nhật ký sẽ bị xóa vĩnh viễn.',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 13,
                color: _onSurfaceVariant,
                height: 1.5,
              ),
            ),
            const SizedBox(height: 24),
            ElevatedButton(
              onPressed: onConfirm,
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.red,
                foregroundColor: Colors.white,
                minimumSize: const Size.fromHeight(52),
                shape: const StadiumBorder(),
                elevation: 0,
              ),
              child: const Text(
                'Xác nhận xóa',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 15,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
            const SizedBox(height: 8),
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text(
                'Hủy bỏ',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  color: _primary,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
