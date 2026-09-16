import 'dart:async';

import 'package:flutter/cupertino.dart';
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

class _EditBabyDailyLogScreenState extends State<EditBabyDailyLogScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Color(0xFFF2EAE4);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  static const List<String> _commonSymptoms = [
    'Sốt',
    'Nôn trớ',
    'Ho',
    'Sổ mũi / Nghẹt mũi',
    'Tiêu chảy',
    'Táo bón',
    'Phát ban / Nổi mẩn',
    'Quấy khóc / Khó chịu',
    'Bỏ bú / Biếng ăn',
    'Khác',
  ];

  final _service = BabyLogService();

  LogType _selectedType = LogType.feeding;
  DateTime _startedAt = DateTime.now();

  // Feeding & Medicine fields
  final _quantityCtrl = TextEditingController();

  // Sleep fields
  int _sleepHours = 1;
  int _sleepMinutes = 0;

  // Diaper fields
  int _diaperCount = 1;

  // Symptom fields
  String _selectedSymptom = 'Sốt';
  final _symptomDescriptionCtrl = TextEditingController();

  // Common Notes
  final _noteCtrl = TextEditingController();

  bool _isSaving = false;
  bool _showSuccess = false;

  @override
  void initState() {
    super.initState();
    _prefillFromLog();
    if (widget.initialLog == null) unawaited(_loadInitialLog());
  }

  void _prefillFromLog() {
    final log = widget.initialLog;
    if (log == null) return;
    _applyLog(log);
  }

  Future<void> _loadInitialLog() async {
    try {
      final log = await _service.getDailyLogDetail(widget.babyId, widget.logId);
      if (log.babyId != widget.babyId) {
        throw const FormatException('Baby daily-log scope mismatch');
      }
      if (!mounted) return;
      setState(() => _applyLog(log));
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Không thể tải nhật ký để chỉnh sửa.')),
      );
    }
  }

  void _applyLog(BabyDailyLog log) {
    _selectedType = log.logType;
    _startedAt = log.startedAt ?? DateTime.now();

    if (log.quantity != null) {
      _quantityCtrl.text =
          log.quantity!.toStringAsFixed(log.quantity! % 1 == 0 ? 0 : 1);
    }

    if (log.logType == LogType.sleep) {
      final unit = log.unit?.trim().toLowerCase();
      final totalMin = (unit == 'giờ' ||
              unit == 'h' ||
              unit == 'hours' ||
              unit == 'hour')
          ? (log.quantity != null ? (log.quantity! * 60).round() : 60)
          : (log.quantity?.round() ?? 60);
      _sleepHours = (totalMin ~/ 60).clamp(0, 24);
      _sleepMinutes = (totalMin % 60).clamp(0, 59);
    } else if (log.logType == LogType.diaper) {
      _diaperCount = log.quantity?.toInt() ?? 1;
      if (_diaperCount < 1) _diaperCount = 1;
    } else if (log.logType == LogType.symptom ||
        log.logType == LogType.fever ||
        log.logType == LogType.vomiting) {
      final existingNote = log.note?.trim() ?? '';
      if (existingNote.isNotEmpty) {
        bool matched = false;
        for (final symptom in _commonSymptoms) {
          if (symptom == 'Khác') continue;
          if (existingNote == symptom) {
            _selectedSymptom = symptom;
            _symptomDescriptionCtrl.text = '';
            matched = true;
            break;
          } else if (existingNote.startsWith('$symptom: ')) {
            _selectedSymptom = symptom;
            _symptomDescriptionCtrl.text =
                existingNote.substring('$symptom: '.length).trim();
            matched = true;
            break;
          }
        }
        if (!matched) {
          if (existingNote.startsWith('Khác: ')) {
            _selectedSymptom = 'Khác';
            _symptomDescriptionCtrl.text =
                existingNote.substring('Khác: '.length).trim();
          } else {
            _selectedSymptom = 'Khác';
            _symptomDescriptionCtrl.text = existingNote;
          }
        }
      }
    }

    if (log.note != null &&
        log.logType != LogType.symptom &&
        log.logType != LogType.fever &&
        log.logType != LogType.vomiting) {
      _noteCtrl.text = log.note!;
    }
  }

  @override
  void dispose() {
    _quantityCtrl.dispose();
    _symptomDescriptionCtrl.dispose();
    _noteCtrl.dispose();
    super.dispose();
  }

  Future<void> _pickDateTime() async {
    final now = DateTime.now();
    final initial = _startedAt;
    final firstDate = now.subtract(const Duration(days: 30));
    final initialDate = initial.isBefore(firstDate)
        ? firstDate
        : initial.isAfter(now)
            ? now
            : initial;

    final date = await showDatePicker(
      context: context,
      initialDate: initialDate,
      firstDate: firstDate,
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
    if (time == null || !mounted) return;

    final dt = DateTime(
      date.year,
      date.month,
      date.day,
      time.hour,
      time.minute,
    );
    setState(() {
      _startedAt = dt;
    });
  }

  Future<void> _save() async {
    setState(() => _isSaving = true);
    try {
      final double? qty;
      final String? unit;
      final String? note;

      switch (_selectedType) {
        case LogType.feeding:
          final raw = _quantityCtrl.text.trim();
          qty = raw.isNotEmpty ? double.tryParse(raw) : null;
          unit = 'ml';
          note = _noteCtrl.text.trim().isEmpty ? null : _noteCtrl.text.trim();
          break;
        case LogType.sleep:
          final totalMinutes = _sleepHours * 60 + _sleepMinutes;
          if (totalMinutes <= 0) {
            setState(() => _isSaving = false);
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(
                content: Text('Vui lòng chọn thời gian ngủ lớn hơn 0 phút.'),
              ),
            );
            return;
          }
          qty = totalMinutes.toDouble();
          unit = 'phút';
          note = _noteCtrl.text.trim().isEmpty ? null : _noteCtrl.text.trim();
          break;
        case LogType.diaper:
          qty = _diaperCount.toDouble();
          unit = 'lần';
          note = _noteCtrl.text.trim().isEmpty ? null : _noteCtrl.text.trim();
          break;
        case LogType.medicine:
          final raw = _quantityCtrl.text.trim();
          qty = raw.isNotEmpty ? double.tryParse(raw) : null;
          unit = 'liều';
          note = _noteCtrl.text.trim().isEmpty ? null : _noteCtrl.text.trim();
          break;
        case LogType.symptom:
        case LogType.fever:
        case LogType.vomiting:
          qty = null;
          unit = null;
          final desc = _symptomDescriptionCtrl.text.trim();
          if (_selectedSymptom == 'Khác') {
            if (desc.isEmpty) {
              setState(() => _isSaving = false);
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  content: Text('Vui lòng nhập mô tả cho triệu chứng này'),
                ),
              );
              return;
            }
            note = 'Khác: $desc';
          } else {
            note = desc.isNotEmpty ? '$_selectedSymptom: $desc' : _selectedSymptom;
          }
          break;
      }

      await _service.updateDailyLog(
        widget.babyId,
        widget.logId,
        UpdateBabyDailyLogRequest(
          startedAt: _startedAt,
          endedAt: null,
          quantity: qty,
          unit: unit,
          note: note,
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

  Future<void> _confirmDelete() async {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
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

  IconData _iconForType(LogType type) {
    switch (type) {
      case LogType.feeding:
        return Icons.restaurant;
      case LogType.sleep:
        return Icons.bedtime;
      case LogType.diaper:
        return Icons.cleaning_services;
      case LogType.fever:
        return Icons.thermostat;
      case LogType.vomiting:
        return Icons.sick;
      case LogType.medicine:
        return Icons.medication;
      case LogType.symptom:
        return Icons.health_and_safety;
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
              _buildTypeHeader(),
              Expanded(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.fromLTRB(16, 16, 16, 40),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      _buildDateTimeSection(),
                      const SizedBox(height: 16),
                      _buildDynamicFields(),
                      if (_selectedType != LogType.symptom &&
                          _selectedType != LogType.fever &&
                          _selectedType != LogType.vomiting) ...[
                        const SizedBox(height: 16),
                        _buildNotesField(),
                      ],
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

  Widget _buildTypeHeader() {
    return Container(
      margin: const EdgeInsets.fromLTRB(16, 12, 16, 4),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: _primary.withAlpha(12),
            blurRadius: 10,
            offset: const Offset(0, 3),
          ),
        ],
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: _primary.withAlpha(25),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(_iconForType(_selectedType), color: _primary, size: 22),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Loại nhật ký',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 11,
                    color: _onSurfaceVariant,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  _selectedType.displayLabel,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                    color: _onSurface,
                  ),
                ),
              ],
            ),
          ),
        ],
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
          _DateTimeChip(
            label: 'Thời gian bắt đầu',
            dateTime: _startedAt,
            onTap: _pickDateTime,
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
              'Lượng sữa (ml)',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: _onSurface,
              ),
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _quantityCtrl,
              keyboardType:
                  const TextInputType.numberWithOptions(decimal: true),
              inputFormatters: [
                FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
              ],
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 15,
                color: _onSurface,
              ),
              decoration: _inputDeco('Lượng sữa (ml)', hint: '150'),
            ),
          ],
        );
      case LogType.sleep:
        return _buildSleepTimePicker();
      case LogType.diaper:
        return _buildDiaperCounter();
      case LogType.medicine:
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Liều lượng',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: _onSurface,
              ),
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _quantityCtrl,
              keyboardType:
                  const TextInputType.numberWithOptions(decimal: true),
              inputFormatters: [
                FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
              ],
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 15,
                color: _onSurface,
              ),
              decoration: _inputDeco('Liều lượng', hint: 'Ví dụ: 1'),
            ),
          ],
        );
      case LogType.symptom:
      case LogType.fever:
      case LogType.vomiting:
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Triệu chứng',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: _onSurface,
              ),
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              initialValue: _selectedSymptom,
              decoration: _inputDeco('Chọn triệu chứng'),
              items: _commonSymptoms
                  .map(
                    (s) => DropdownMenuItem(
                      value: s,
                      child: Text(
                        s,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 14,
                          color: _onSurface,
                        ),
                      ),
                    ),
                  )
                  .toList(),
              onChanged: (val) {
                if (val != null) {
                  setState(() => _selectedSymptom = val);
                }
              },
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _symptomDescriptionCtrl,
              maxLines: 3,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                color: _onSurface,
              ),
              decoration: _inputDeco(
                _selectedSymptom == 'Khác'
                    ? 'Mô tả triệu chứng *'
                    : 'Mô tả chi tiết (tùy chọn)',
                hint: _selectedSymptom == 'Khác'
                    ? 'Nhập triệu chứng cụ thể của bé...'
                    : 'Ví dụ: nhiệt độ, mức độ, biểu hiện của bé...',
              ),
            ),
          ],
        );
    }
  }

  Widget _buildSleepTimePicker() {
    final displayTime = _sleepHours > 0
        ? (_sleepMinutes > 0
            ? '$_sleepHours giờ $_sleepMinutes phút'
            : '$_sleepHours giờ')
        : '$_sleepMinutes phút';

    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: _primary.withAlpha(50)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Thời gian ngủ:',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                  color: _onSurface,
                ),
              ),
              Text(
                displayTime,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 15,
                  fontWeight: FontWeight.w700,
                  color: _primary,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          SizedBox(
            height: 120,
            child: Row(
              children: [
                Expanded(
                  child: CupertinoPicker(
                    scrollController: FixedExtentScrollController(
                      initialItem: _sleepHours.clamp(0, 24),
                    ),
                    itemExtent: 36,
                    selectionOverlay: Container(
                      decoration: BoxDecoration(
                        color: _primary.withAlpha(20),
                        borderRadius: BorderRadius.circular(8),
                      ),
                    ),
                    onSelectedItemChanged: (index) {
                      setState(() => _sleepHours = index);
                    },
                    children: List.generate(
                      25,
                      (i) => Center(
                        child: Text(
                          '$i giờ',
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 15,
                            color: _onSurface,
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: CupertinoPicker(
                    scrollController: FixedExtentScrollController(
                      initialItem: _sleepMinutes.clamp(0, 59),
                    ),
                    itemExtent: 36,
                    selectionOverlay: Container(
                      decoration: BoxDecoration(
                        color: _primary.withAlpha(20),
                        borderRadius: BorderRadius.circular(8),
                      ),
                    ),
                    onSelectedItemChanged: (index) {
                      setState(() => _sleepMinutes = index);
                    },
                    children: List.generate(
                      60,
                      (i) => Center(
                        child: Text(
                          '$i phút',
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 15,
                            color: _onSurface,
                          ),
                        ),
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

  Widget _buildDiaperCounter() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: _primary.withAlpha(50)),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          const Text(
            'Số lần thay tã',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              fontWeight: FontWeight.w600,
              color: _onSurface,
            ),
          ),
          Row(
            children: [
              IconButton.filledTonal(
                onPressed: _diaperCount > 1
                    ? () => setState(() => _diaperCount--)
                    : null,
                icon: const Icon(Icons.remove, size: 20),
                style: IconButton.styleFrom(
                  backgroundColor: _primary.withAlpha(25),
                  foregroundColor: _primary,
                ),
              ),
              Container(
                constraints: const BoxConstraints(minWidth: 44),
                alignment: Alignment.center,
                child: Text(
                  '$_diaperCount',
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 18,
                    fontWeight: FontWeight.w700,
                    color: _onSurface,
                  ),
                ),
              ),
              IconButton.filled(
                onPressed: () => setState(() => _diaperCount++),
                icon: const Icon(Icons.add, size: 20),
                style: IconButton.styleFrom(
                  backgroundColor: _primary,
                  foregroundColor: Colors.white,
                ),
              ),
            ],
          ),
        ],
      ),
    );
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
        contentPadding:
            const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
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

class _DeleteConfirmSheet extends StatelessWidget {
  const _DeleteConfirmSheet({required this.onConfirm});

  final VoidCallback onConfirm;

  static const _primary = Color(0xFF845143);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(24, 20, 24, 16),
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
