import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../models/health_metric_model.dart';
import '../services/health_metric_service.dart';

class AddMaternalHealthMetricScreen extends StatefulWidget {
  final String journeyId;
  final String initialMetricType;

  const AddMaternalHealthMetricScreen({
    super.key,
    required this.journeyId,
    required this.initialMetricType,
  });

  @override
  State<AddMaternalHealthMetricScreen> createState() =>
      _AddMaternalHealthMetricScreenState();
}

class _AddMaternalHealthMetricScreenState
    extends State<AddMaternalHealthMetricScreen> {
  static const _primary = Color(0xFF845143);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Color(0xFFFFFFFF);
  static const _surfaceContainer = Color(0xFFF2EAE4);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outline = Color(0xFFD6C2BD);
  static const _error = Color(0xFFBA1A1A);

  final _formKey = GlobalKey<FormState>();
  final _primaryCtrl = TextEditingController();
  final _secondaryCtrl = TextEditingController();
  final _noteCtrl = TextEditingController();
  final _service = HealthMetricService();

  DateTime _measuredDate = DateTime.now();
  TimeOfDay _measuredTime = TimeOfDay.now();
  bool _isSaving = false;

  bool get _isBloodPressure =>
      widget.initialMetricType.startsWith('BLOOD_PRESSURE');

  String get _metricType {
    if (_isBloodPressure) return 'BLOOD_PRESSURE_SYSTOLIC';
    return _supportedMetricTypes.contains(widget.initialMetricType)
        ? widget.initialMetricType
        : 'WEIGHT';
  }

  String get _title =>
      _isBloodPressure ? 'Thêm huyết áp' : 'Thêm $_metricLabel';
  String get _primaryLabel =>
      _isBloodPressure ? 'Tâm thu (mmHg)' : '$_metricLabel ($_unit)';
  String get _unit {
    switch (_metricType) {
      case 'BLOOD_PRESSURE_SYSTOLIC':
        return 'mmHg';
      case 'HEART_RATE':
        return 'bpm';
      case 'BLOOD_GLUCOSE':
        return 'mg/dL';
      case 'TEMPERATURE':
        return '°C';
      case 'WEIGHT':
      default:
        return 'kg';
    }
  }

  String get _metricLabel {
    switch (_metricType) {
      case 'HEART_RATE':
        return 'nhịp tim';
      case 'BLOOD_GLUCOSE':
        return 'đường huyết';
      case 'TEMPERATURE':
        return 'nhiệt độ';
      case 'WEIGHT':
      default:
        return 'cân nặng';
    }
  }

  static const _supportedMetricTypes = {
    'WEIGHT',
    'HEART_RATE',
    'BLOOD_GLUCOSE',
    'TEMPERATURE',
  };

  DateTime get _resolvedMeasuredAt => DateTime(
    _measuredDate.year,
    _measuredDate.month,
    _measuredDate.day,
    _measuredTime.hour,
    _measuredTime.minute,
  );

  @override
  void dispose() {
    _primaryCtrl.dispose();
    _secondaryCtrl.dispose();
    _noteCtrl.dispose();
    super.dispose();
  }

  Future<void> _pickDate() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _measuredDate,
      firstDate: now.subtract(const Duration(days: 365)),
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
    if (picked != null) setState(() => _measuredDate = picked);
  }

  Future<void> _pickTime() async {
    final picked = await showTimePicker(
      context: context,
      initialTime: _measuredTime,
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
    if (picked != null) setState(() => _measuredTime = picked);
  }

  String? _requiredPositive(String? value) {
    final parsed = double.tryParse(value?.trim() ?? '');
    if (parsed == null || parsed <= 0) return 'Nhập giá trị hợp lệ';
    return null;
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;

    final measuredAt = _resolvedMeasuredAt;
    if (measuredAt.isAfter(DateTime.now().add(const Duration(minutes: 5)))) {
      _showError('Thời điểm đo không được ở tương lai quá 5 phút.');
      return;
    }

    setState(() => _isSaving = true);
    try {
      await _service.addMetric(
        widget.journeyId,
        AddMetricRequest(
          metricType: _metricType,
          valueNumeric: double.parse(_primaryCtrl.text.trim()),
          valueSecondary: _isBloodPressure
              ? double.parse(_secondaryCtrl.text.trim())
              : null,
          unit: _unit,
          measuredAt: measuredAt,
          note: _noteCtrl.text.trim().isEmpty ? null : _noteCtrl.text.trim(),
        ),
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Đã thêm chỉ số thành công'),
          backgroundColor: _primary,
        ),
      );
      Navigator.of(context).pop(true);
    } catch (_) {
      _showError('Không thể lưu chỉ số. Vui lòng thử lại.');
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  void _showError(String message) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message), backgroundColor: _error));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      appBar: AppBar(
        backgroundColor: _canvas,
        elevation: 0,
        foregroundColor: _onSurface,
        title: Text(
          _title,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 18,
            fontWeight: FontWeight.w700,
          ),
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                TextFormField(
                  controller: _primaryCtrl,
                  keyboardType: const TextInputType.numberWithOptions(
                    decimal: true,
                  ),
                  inputFormatters: [
                    FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
                  ],
                  validator: _requiredPositive,
                  decoration: _inputDecoration(_primaryLabel),
                ),
                if (_isBloodPressure) ...[
                  const SizedBox(height: 14),
                  TextFormField(
                    controller: _secondaryCtrl,
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: true,
                    ),
                    inputFormatters: [
                      FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
                    ],
                    validator: _requiredPositive,
                    decoration: _inputDecoration('Tâm trương (mmHg)'),
                  ),
                ],
                const SizedBox(height: 14),
                Row(
                  children: [
                    Expanded(
                      child: _PickerTile(
                        label: 'Ngày đo',
                        value:
                            '${_measuredDate.day.toString().padLeft(2, '0')}/${_measuredDate.month.toString().padLeft(2, '0')}/${_measuredDate.year}',
                        onTap: _pickDate,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: _PickerTile(
                        label: 'Giờ đo',
                        value: _measuredTime.format(context),
                        onTap: _pickTime,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 14),
                TextFormField(
                  controller: _noteCtrl,
                  minLines: 3,
                  maxLines: 5,
                  decoration: _inputDecoration('Ghi chú'),
                ),
                const SizedBox(height: 24),
                ElevatedButton(
                  onPressed: _isSaving ? null : _save,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: _primary,
                    foregroundColor: Colors.white,
                    minimumSize: const Size(double.infinity, 52),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(14),
                    ),
                  ),
                  child: _isSaving
                      ? const SizedBox(
                          width: 22,
                          height: 22,
                          child: CircularProgressIndicator(
                            strokeWidth: 2.4,
                            color: Colors.white,
                          ),
                        )
                      : const Text(
                          'Lưu chỉ số',
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  InputDecoration _inputDecoration(String label) {
    return InputDecoration(
      labelText: label,
      filled: true,
      fillColor: _surface,
      labelStyle: const TextStyle(color: _onSurfaceVariant),
      enabledBorder: OutlineInputBorder(
        borderSide: const BorderSide(color: _outline),
        borderRadius: BorderRadius.circular(14),
      ),
      focusedBorder: OutlineInputBorder(
        borderSide: const BorderSide(color: _primary, width: 1.5),
        borderRadius: BorderRadius.circular(14),
      ),
      errorBorder: OutlineInputBorder(
        borderSide: const BorderSide(color: _error),
        borderRadius: BorderRadius.circular(14),
      ),
      focusedErrorBorder: OutlineInputBorder(
        borderSide: const BorderSide(color: _error, width: 1.5),
        borderRadius: BorderRadius.circular(14),
      ),
    );
  }
}

class _PickerTile extends StatelessWidget {
  final String label;
  final String value;
  final VoidCallback onTap;

  const _PickerTile({
    required this.label,
    required this.value,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(14),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
        decoration: BoxDecoration(
          color: _AddMaternalHealthMetricScreenState._surfaceContainer,
          border: Border.all(
            color: _AddMaternalHealthMetricScreenState._outline,
          ),
          borderRadius: BorderRadius.circular(14),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              label,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 12,
                color: _AddMaternalHealthMetricScreenState._onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              value,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                fontWeight: FontWeight.w700,
                color: _AddMaternalHealthMetricScreenState._onSurface,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
