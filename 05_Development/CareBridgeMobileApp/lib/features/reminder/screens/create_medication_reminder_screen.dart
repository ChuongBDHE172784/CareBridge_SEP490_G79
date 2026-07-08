import 'package:flutter/material.dart';
import '../models/reminder_model.dart';
import '../services/reminder_service.dart';

class CreateMedicationReminderScreen extends StatefulWidget {
  const CreateMedicationReminderScreen({super.key});

  @override
  State<CreateMedicationReminderScreen> createState() => _CreateMedicationReminderScreenState();
}

class _CreateMedicationReminderScreenState extends State<CreateMedicationReminderScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Color(0xFFF2EAE4);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  final _service = ReminderService.instance;
  final _titleCtrl = TextEditingController();
  final _instructionsCtrl = TextEditingController();

  ReminderAssignee _assignee = ReminderAssignee.mother;
  RecurrenceType _recurrence = RecurrenceType.daily;
  DateTime _startDate = DateTime.now();
  DateTime? _endDate;
  bool _notifyEnabled = true;
  bool _isSaving = false;
  bool _showSuccess = false;

  final List<DateTime> _reminderTimes = [];

  @override
  void initState() {
    super.initState();
    _reminderTimes.add(DateTime.now().copyWith(hour: 8, minute: 0, second: 0));
  }

  @override
  void dispose() {
    _titleCtrl.dispose();
    _instructionsCtrl.dispose();
    super.dispose();
  }

  Future<void> _pickStartDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _startDate,
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 365 * 2)),
      builder: _themePicker,
    );
    if (picked != null) setState(() => _startDate = picked);
  }

  Future<void> _pickEndDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _endDate ?? _startDate.add(const Duration(days: 7)),
      firstDate: _startDate,
      lastDate: DateTime.now().add(const Duration(days: 365 * 2)),
      builder: _themePicker,
    );
    if (picked != null) setState(() => _endDate = picked);
  }

  Future<void> _addReminderTime() async {
    final picked = await showTimePicker(
      context: context,
      initialTime: const TimeOfDay(hour: 12, minute: 0),
      builder: _themePicker,
    );
    if (picked != null) {
      setState(() {
        _reminderTimes.add(DateTime.now().copyWith(hour: picked.hour, minute: picked.minute, second: 0));
      });
    }
  }

  Widget Function(BuildContext, Widget?) get _themePicker => (ctx, child) => Theme(
    data: Theme.of(ctx).copyWith(
      colorScheme: const ColorScheme.light(primary: _primary, onPrimary: Colors.white, surface: _canvas),
    ),
    child: child!,
  );

  Future<void> _save() async {
    if (_titleCtrl.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Vui lòng nhập tên thuốc.'), backgroundColor: _primary),
      );
      return;
    }
    if (_reminderTimes.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Vui lòng thêm ít nhất 1 giờ nhắc.'), backgroundColor: _primary),
      );
      return;
    }
    setState(() => _isSaving = true);
    try {
      final firstTime = _reminderTimes.first;
      final scheduledAt = _startDate.copyWith(
        hour: firstTime.hour,
        minute: firstTime.minute,
        second: 0,
      );
      await _service.createMedicationReminder(
        title: _titleCtrl.text.trim(),
        scheduledAt: scheduledAt,
        recurrenceType: _recurrence,
        recurrenceEndDate: _endDate,
      );
      setState(() => _showSuccess = true);
      await Future.delayed(const Duration(seconds: 2));
      if (mounted) Navigator.of(context).pop(true);
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Không thể lưu. Vui lòng thử lại.'), backgroundColor: Colors.red),
        );
      }
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
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
          'Tạo nhắc uống thuốc',
          style: TextStyle(fontFamily: 'Lexend', fontSize: 18, fontWeight: FontWeight.w700, color: _onSurface),
        ),
      ),
      body: Stack(
        children: [
          SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 40),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                _buildMedIcon(),
                const SizedBox(height: 20),
                _buildDrugNameCard(),
                const SizedBox(height: 16),
                _buildSubjectCard(),
                const SizedBox(height: 16),
                _buildScheduleCard(),
                const SizedBox(height: 16),
                _buildInstructionsCard(),
                const SizedBox(height: 16),
                _buildNotificationToggle(),
                const SizedBox(height: 24),
                _buildSaveButton(),
              ],
            ),
          ),
          if (_showSuccess) _buildSuccessToast(),
        ],
      ),
    );
  }

  Widget _buildMedIcon() {
    return Center(
      child: Container(
        width: 72,
        height: 72,
        decoration: BoxDecoration(
          color: _primaryContainer.withAlpha(30),
          borderRadius: BorderRadius.circular(24),
        ),
        child: const Icon(Icons.medical_services_rounded, color: _primaryContainer, size: 36),
      ),
    );
  }

  Widget _buildDrugNameCard() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [BoxShadow(color: _primary.withAlpha(15), blurRadius: 12, offset: const Offset(0, 4))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          TextFormField(
            controller: _titleCtrl,
            maxLength: 255,
            style: const TextStyle(fontFamily: 'Lexend', fontSize: 15, color: _onSurface),
            decoration: _inputDeco('Tên thuốc / Vitamin *'),
          ),
          const SizedBox(height: 8),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            decoration: BoxDecoration(color: _surface, borderRadius: BorderRadius.circular(10)),
            child: const Row(
              children: [
                Icon(Icons.info_outline_rounded, size: 14, color: _onSurfaceVariant),
                SizedBox(width: 6),
                Expanded(
                  child: Text(
                    'Không cần kê đơn bác sĩ. Tự bổ sung vitamin hàng ngày.',
                    style: TextStyle(fontFamily: 'Lexend', fontSize: 11, color: _onSurfaceVariant),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSubjectCard() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [BoxShadow(color: _primary.withAlpha(15), blurRadius: 12, offset: const Offset(0, 4))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Đối tượng uống', style: TextStyle(fontFamily: 'Lexend', fontSize: 13, fontWeight: FontWeight.w600, color: _onSurface)),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(child: _buildAssigneeCard(ReminderAssignee.mother, Icons.person_rounded, 'Cho Mẹ')),
              const SizedBox(width: 12),
              Expanded(child: _buildAssigneeCard(ReminderAssignee.baby, Icons.child_care_rounded, 'Cho Bé')),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildAssigneeCard(ReminderAssignee a, IconData icon, String label) {
    final selected = _assignee == a;
    return GestureDetector(
      onTap: () => setState(() => _assignee = a),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 180),
        padding: const EdgeInsets.symmetric(vertical: 14),
        decoration: BoxDecoration(
          color: selected ? _primary : _surface,
          borderRadius: BorderRadius.circular(18),
        ),
        child: Column(
          children: [
            Icon(icon, color: selected ? Colors.white : _onSurfaceVariant, size: 24),
            const SizedBox(height: 6),
            Text(label, style: TextStyle(fontFamily: 'Lexend', fontSize: 13, fontWeight: FontWeight.w600, color: selected ? Colors.white : _onSurface)),
          ],
        ),
      ),
    );
  }

  Widget _buildScheduleCard() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [BoxShadow(color: _primary.withAlpha(15), blurRadius: 12, offset: const Offset(0, 4))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Lịch uống', style: TextStyle(fontFamily: 'Lexend', fontSize: 13, fontWeight: FontWeight.w600, color: _onSurface)),
          const SizedBox(height: 14),
          Row(
            children: [
              Expanded(child: _buildDateChip('Bắt đầu', _startDate, _pickStartDate)),
              const SizedBox(width: 12),
              Expanded(child: _buildDateChip('Kết thúc', _endDate, _pickEndDate, optional: true)),
            ],
          ),
          const SizedBox(height: 14),
          _buildFrequencyDropdown(),
          const SizedBox(height: 14),
          const Text('Giờ nhắc', style: TextStyle(fontFamily: 'Lexend', fontSize: 12, color: _onSurfaceVariant)),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              ..._reminderTimes.asMap().entries.map((entry) {
                final t = entry.value;
                return GestureDetector(
                  onLongPress: () => setState(() => _reminderTimes.removeAt(entry.key)),
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                    decoration: BoxDecoration(color: _surface, borderRadius: BorderRadius.circular(50)),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(Icons.schedule_rounded, size: 14, color: _primary),
                        const SizedBox(width: 4),
                        Text(
                          '${t.hour.toString().padLeft(2, '0')}:${t.minute.toString().padLeft(2, '0')}',
                          style: const TextStyle(fontFamily: 'Lexend', fontSize: 13, fontWeight: FontWeight.w600, color: _onSurface),
                        ),
                      ],
                    ),
                  ),
                );
              }),
              GestureDetector(
                onTap: _addReminderTime,
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                  decoration: BoxDecoration(
                    color: _primaryContainer.withAlpha(30),
                    borderRadius: BorderRadius.circular(50),
                    border: Border.all(color: _primaryContainer, width: 1.5),
                  ),
                  child: const Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.add_rounded, size: 16, color: _primary),
                      SizedBox(width: 4),
                      Text('Thêm giờ', style: TextStyle(fontFamily: 'Lexend', fontSize: 12, color: _primary, fontWeight: FontWeight.w600)),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildDateChip(String label, DateTime? date, VoidCallback onTap, {bool optional = false}) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
        decoration: BoxDecoration(
          color: _surface,
          borderRadius: BorderRadius.circular(16),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(label, style: const TextStyle(fontFamily: 'Lexend', fontSize: 10, color: _onSurfaceVariant)),
            const SizedBox(height: 4),
            Text(
              date != null
                  ? '${date.day.toString().padLeft(2, '0')}/${date.month.toString().padLeft(2, '0')}/${date.year}'
                  : optional ? 'Tuỳ chọn' : 'Chọn ngày',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: date != null ? _onSurface : _onSurfaceVariant,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildFrequencyDropdown() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Tần suất', style: TextStyle(fontFamily: 'Lexend', fontSize: 10, color: _onSurfaceVariant)),
          DropdownButtonHideUnderline(
            child: DropdownButton<RecurrenceType>(
              value: _recurrence,
              isExpanded: true,
              style: const TextStyle(fontFamily: 'Lexend', fontSize: 13, color: _onSurface),
              dropdownColor: Colors.white,
              borderRadius: BorderRadius.circular(16),
              items: RecurrenceType.values.map((r) => DropdownMenuItem(
                value: r,
                child: Text(r.displayLabel),
              )).toList(),
              onChanged: (v) { if (v != null) setState(() => _recurrence = v); },
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildInstructionsCard() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [BoxShadow(color: _primary.withAlpha(15), blurRadius: 12, offset: const Offset(0, 4))],
      ),
      child: TextFormField(
        controller: _instructionsCtrl,
        maxLines: 3,
        style: const TextStyle(fontFamily: 'Lexend', fontSize: 14, color: _onSurface),
        decoration: _inputDeco('Hướng dẫn sử dụng', hint: 'Uống sau bữa ăn, không nhai...'),
      ),
    );
  }

  Widget _buildNotificationToggle() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [BoxShadow(color: _primary.withAlpha(15), blurRadius: 12, offset: const Offset(0, 4))],
      ),
      child: Row(
        children: [
          Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(color: _surface, borderRadius: BorderRadius.circular(14)),
            child: const Icon(Icons.notifications_rounded, color: _primaryContainer, size: 22),
          ),
          const SizedBox(width: 14),
          const Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Bật thông báo', style: TextStyle(fontFamily: 'Lexend', fontSize: 14, fontWeight: FontWeight.w600, color: _onSurface)),
                Text('Nhận thông báo trước giờ uống', style: TextStyle(fontFamily: 'Lexend', fontSize: 11, color: _onSurfaceVariant)),
              ],
            ),
          ),
          Switch(
            value: _notifyEnabled,
            onChanged: (v) => setState(() => _notifyEnabled = v),
            activeThumbColor: _primary,
            activeTrackColor: _primaryContainer.withAlpha(100),
          ),
        ],
      ),
    );
  }

  Widget _buildSaveButton() {
    return ElevatedButton(
      onPressed: _isSaving ? null : _save,
      style: ElevatedButton.styleFrom(
        backgroundColor: _primary,
        foregroundColor: Colors.white,
        padding: const EdgeInsets.symmetric(vertical: 16),
        shape: const StadiumBorder(),
        elevation: 0,
      ),
      child: _isSaving
          ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2.5, color: Colors.white))
          : const Text('Lưu lịch nhắc', style: TextStyle(fontFamily: 'Lexend', fontSize: 16, fontWeight: FontWeight.w700)),
    );
  }

  Widget _buildSuccessToast() {
    return Positioned.fill(
      child: Container(
        color: Colors.black.withAlpha(100),
        child: Center(
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 20),
            decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(24), boxShadow: [BoxShadow(color: Colors.black.withAlpha(40), blurRadius: 20)]),
            child: const Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(Icons.check_circle_rounded, color: _primary, size: 48),
                SizedBox(height: 12),
                Text('Đã tạo lịch nhắc!', style: TextStyle(fontFamily: 'Lexend', fontSize: 15, fontWeight: FontWeight.w600, color: _onSurface)),
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
    labelStyle: const TextStyle(fontFamily: 'Lexend', fontSize: 12, color: _onSurfaceVariant),
    hintStyle: const TextStyle(fontFamily: 'Lexend', fontSize: 13, color: Color(0xFFBBA9A4)),
    filled: true,
    fillColor: Colors.white,
    counterText: '',
    contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
    border: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: const BorderSide(color: _surface, width: 2)),
    enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: const BorderSide(color: _surface, width: 2)),
    focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: const BorderSide(color: _primaryContainer, width: 2)),
  );
}
