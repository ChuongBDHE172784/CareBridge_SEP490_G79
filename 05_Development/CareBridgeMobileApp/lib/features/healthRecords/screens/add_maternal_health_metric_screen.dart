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
  final _protocolCtrl = TextEditingController(text: 'COUNT_10_MINUTES');
  final _gestationalAgeCtrl = TextEditingController();
  final _service = HealthMetricService();

  DateTime _measuredDate = DateTime.now();
  TimeOfDay _measuredTime = TimeOfDay.now();
  DateTime _periodStartDate = DateTime.now();
  TimeOfDay _periodStartTime = TimeOfDay.fromDateTime(
    DateTime.now().subtract(const Duration(minutes: 10)),
  );
  DateTime _periodEndDate = DateTime.now();
  TimeOfDay _periodEndTime = TimeOfDay.now();
  String _glucoseContext = 'FASTING';
  String _completionStatus = 'COMPLETED';
  List<MetricCapability> _capabilities = const [];
  bool _isLoadingCapabilities = true;
  bool _isSaving = false;
  String? _loadError;

  static const _glucoseContexts = <String, String>{
    'FASTING': 'Lúc đói',
    'PRE_MEAL': 'Trước ăn',
    'POST_MEAL_1H': 'Sau ăn 1 giờ',
    'POST_MEAL_2H': 'Sau ăn 2 giờ',
    'RANDOM': 'Ngẫu nhiên',
    'OTHER_APPROVED': 'Khác (đã được duyệt)',
  };

  static const _completionStatuses = <String, String>{
    'COMPLETED': 'Đã hoàn thành',
    'PARTIAL': 'Chưa hoàn thành',
  };

  @override
  void initState() {
    super.initState();
    _loadCapabilities();
  }

  String get _metricType => _canonicalMetricType(widget.initialMetricType);

  bool get _isBloodPressure => _metricType == 'BLOOD_PRESSURE';
  bool get _isGlucose => _metricType == 'BLOOD_GLUCOSE';
  bool get _isFetalMovement => _metricType == 'FETAL_MOVEMENT_SESSION';

  MetricCapability? get _capability {
    for (final capability in _capabilities) {
      if (capability.metricCode == _metricType) return capability;
    }
    return null;
  }

  bool get _isSupported => _capability?.manualEntrySupported == true;

  String get _title {
    if (_isBloodPressure) return 'Thêm huyết áp';
    if (_isFetalMovement) return 'Thêm cử động thai';
    return 'Thêm $_metricLabel';
  }

  String get _primaryLabel {
    if (_isBloodPressure) return 'Tâm thu (mmHg)';
    if (_isFetalMovement) return 'Số cử động';
    return '$_metricLabel ($_unit)';
  }

  String get _unit {
    switch (_metricType) {
      case 'BLOOD_PRESSURE':
        return 'mmHg';
      case 'BLOOD_GLUCOSE':
        return 'mg/dL';
      case 'MATERNAL_HEART_RATE':
        return 'bpm';
      case 'TEMPERATURE':
        return 'Cel';
      case 'FETAL_MOVEMENT_SESSION':
        return 'count';
      case 'WEIGHT':
      default:
        return _capability?.canonicalUnit.isNotEmpty == true
            ? _capability!.canonicalUnit
            : 'kg';
    }
  }

  String get _metricLabel {
    switch (_metricType) {
      case 'BLOOD_PRESSURE':
        return 'huyết áp';
      case 'BLOOD_GLUCOSE':
        return 'đường huyết';
      case 'MATERNAL_HEART_RATE':
        return 'nhịp tim';
      case 'TEMPERATURE':
        return 'nhiệt độ';
      case 'FETAL_MOVEMENT_SESSION':
        return 'cử động thai';
      case 'WEIGHT':
      default:
        return 'cân nặng';
    }
  }

  String _canonicalMetricType(String value) {
    switch (value) {
      case 'BLOOD_PRESSURE_SYSTOLIC':
      case 'BLOOD_PRESSURE_DIASTOLIC':
      case 'BLOOD_PRESSURE':
        return 'BLOOD_PRESSURE';
      case 'FETAL_MOVEMENT':
      case 'FETAL_MOVEMENT_COUNT':
      case 'FETAL_MOVEMENT_SESSION':
        return 'FETAL_MOVEMENT_SESSION';
      case 'HEART_RATE':
        return 'MATERNAL_HEART_RATE';
      default:
        return value;
    }
  }

  Future<void> _loadCapabilities() async {
    try {
      final capabilities = await _service.getCapabilities(widget.journeyId);
      if (!mounted) return;
      setState(() {
        _capabilities = capabilities;
        _isLoadingCapabilities = false;
        _loadError = capabilities.any((item) => item.metricCode == _metricType)
            ? null
            : 'Chỉ số này không được hỗ trợ nhập thủ công trong hành trình hiện tại.';
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _isLoadingCapabilities = false;
        _loadError = 'Không thể tải cấu hình chỉ số. Vui lòng thử lại.';
      });
    }
  }

  DateTime get _resolvedMeasuredAt => DateTime(
    _measuredDate.year,
    _measuredDate.month,
    _measuredDate.day,
    _measuredTime.hour,
    _measuredTime.minute,
  );

  DateTime get _resolvedPeriodStart => DateTime(
    _periodStartDate.year,
    _periodStartDate.month,
    _periodStartDate.day,
    _periodStartTime.hour,
    _periodStartTime.minute,
  );

  DateTime get _resolvedPeriodEnd => DateTime(
    _periodEndDate.year,
    _periodEndDate.month,
    _periodEndDate.day,
    _periodEndTime.hour,
    _periodEndTime.minute,
  );

  @override
  void dispose() {
    _primaryCtrl.dispose();
    _secondaryCtrl.dispose();
    _noteCtrl.dispose();
    _protocolCtrl.dispose();
    _gestationalAgeCtrl.dispose();
    super.dispose();
  }

  Future<DateTime?> _pickDate(DateTime initialDate) async {
    final now = DateTime.now();
    return showDatePicker(
      context: context,
      initialDate: initialDate,
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
  }

  Future<TimeOfDay?> _pickTime(TimeOfDay initialTime) async {
    return showTimePicker(
      context: context,
      initialTime: initialTime,
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
  }

  Future<void> _pickMeasuredDate() async {
    final picked = await _pickDate(_measuredDate);
    if (picked != null) setState(() => _measuredDate = picked);
  }

  Future<void> _pickMeasuredTime() async {
    final picked = await _pickTime(_measuredTime);
    if (picked != null) setState(() => _measuredTime = picked);
  }

  Future<void> _pickPeriodStartTime() async {
    final picked = await _pickTime(_periodStartTime);
    if (picked != null) setState(() => _periodStartTime = picked);
  }

  Future<void> _pickPeriodEndTime() async {
    final picked = await _pickTime(_periodEndTime);
    if (picked != null) setState(() => _periodEndTime = picked);
  }

  String? _requiredPositive(String? value) {
    final parsed = double.tryParse(value?.trim() ?? '');
    if (parsed == null || parsed <= 0) return 'Nhập giá trị hợp lệ';
    return null;
  }

  String? _requiredNonNegative(String? value) {
    final parsed = double.tryParse(value?.trim() ?? '');
    if (parsed == null || parsed < 0) return 'Nhập số hợp lệ';
    return null;
  }

  String? _requiredText(String? value) {
    if (value == null || value.trim().isEmpty) return 'Thông tin bắt buộc';
    return null;
  }

  Future<void> _save() async {
    if (!_isSupported) {
      _showError('Chỉ số này không được hỗ trợ nhập thủ công.');
      return;
    }
    if (!_formKey.currentState!.validate()) return;

    final measuredAt = _isFetalMovement
        ? _resolvedPeriodEnd
        : _resolvedMeasuredAt;
    if (measuredAt.isAfter(DateTime.now().add(const Duration(minutes: 5)))) {
      _showError('Thời điểm đo không được ở tương lai quá 5 phút.');
      return;
    }

    if (_isFetalMovement && !_resolvedPeriodEnd.isAfter(_resolvedPeriodStart)) {
      _showError('Thời điểm kết thúc phải sau thời điểm bắt đầu.');
      return;
    }

    setState(() => _isSaving = true);
    try {
      final contextPayload = <String, dynamic>{};
      if (_isGlucose) contextPayload['measurementContext'] = _glucoseContext;
      if (_isFetalMovement) {
        contextPayload['protocolCode'] = _protocolCtrl.text.trim();
        contextPayload['completionStatus'] = _completionStatus;
        contextPayload['gestationalAgeSnapshot'] = _gestationalAgeCtrl.text
            .trim();
      }

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
          context: contextPayload,
          periodStart: _isFetalMovement ? _resolvedPeriodStart : null,
          periodEnd: _isFetalMovement ? _resolvedPeriodEnd : null,
          definitionVersion: _capability?.version,
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
      _showError('Không thể lưu chỉ số. Vui lòng kiểm tra dữ liệu và thử lại.');
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
        child: _isLoadingCapabilities
            ? const Center(child: CircularProgressIndicator(color: _primary))
            : _loadError != null
            ? _buildErrorState()
            : SingleChildScrollView(
                padding: const EdgeInsets.all(20),
                child: Form(
                  key: _formKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      _buildMetricDescription(),
                      const SizedBox(height: 14),
                      TextFormField(
                        controller: _primaryCtrl,
                        keyboardType: const TextInputType.numberWithOptions(
                          decimal: true,
                        ),
                        inputFormatters: [
                          FilteringTextInputFormatter.allow(RegExp(r'[0-9.]')),
                        ],
                        validator: _isFetalMovement
                            ? _requiredNonNegative
                            : _requiredPositive,
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
                            FilteringTextInputFormatter.allow(
                              RegExp(r'[0-9.]'),
                            ),
                          ],
                          validator: _requiredPositive,
                          decoration: _inputDecoration('Tâm trương (mmHg)'),
                        ),
                      ],
                      if (_isGlucose) ...[
                        const SizedBox(height: 14),
                        _buildDropdown<String>(
                          label: 'Bối cảnh đo',
                          value: _glucoseContext,
                          items: _glucoseContexts,
                          onChanged: (value) {
                            if (value != null) {
                              setState(() => _glucoseContext = value);
                            }
                          },
                        ),
                      ],
                      if (_isFetalMovement) ..._buildFetalMovementFields(),
                      if (!_isFetalMovement) ...[
                        const SizedBox(height: 14),
                        Row(
                          children: [
                            Expanded(
                              child: _PickerTile(
                                label: 'Ngày đo',
                                value: _formatDate(_measuredDate),
                                onTap: _pickMeasuredDate,
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: _PickerTile(
                                label: 'Giờ đo',
                                value: _measuredTime.format(context),
                                onTap: _pickMeasuredTime,
                              ),
                            ),
                          ],
                        ),
                      ],
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

  Widget _buildErrorState() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.info_outline_rounded, color: _error, size: 44),
            const SizedBox(height: 12),
            Text(
              _loadError!,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontFamily: 'Lexend',
                color: _onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 12),
            TextButton(
              onPressed: _loadCapabilities,
              child: const Text('Thử lại'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMetricDescription() {
    return Text(
      'Dữ liệu được lưu để theo dõi, không thay thế chẩn đoán y khoa.',
      style: TextStyle(
        fontFamily: 'Lexend',
        fontSize: 12,
        color: _onSurfaceVariant,
      ),
    );
  }

  List<Widget> _buildFetalMovementFields() {
    return [
      const SizedBox(height: 14),
      TextFormField(
        controller: _protocolCtrl,
        validator: _requiredText,
        decoration: _inputDecoration('Mã quy trình'),
      ),
      const SizedBox(height: 14),
      TextFormField(
        controller: _gestationalAgeCtrl,
        validator: _requiredText,
        decoration: _inputDecoration('Tuổi thai tại thời điểm đo'),
      ),
      const SizedBox(height: 14),
      _buildDropdown<String>(
        label: 'Trạng thái phiên đo',
        value: _completionStatus,
        items: _completionStatuses,
        onChanged: (value) {
          if (value != null) setState(() => _completionStatus = value);
        },
      ),
      const SizedBox(height: 14),
      Row(
        children: [
          Expanded(
            child: _PickerTile(
              label: 'Bắt đầu',
              value:
                  '${_formatDate(_periodStartDate)} ${_periodStartTime.format(context)}',
              onTap: () async {
                final picked = await _pickDate(_periodStartDate);
                if (picked != null) setState(() => _periodStartDate = picked);
              },
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: _PickerTile(
              label: 'Kết thúc',
              value:
                  '${_formatDate(_periodEndDate)} ${_periodEndTime.format(context)}',
              onTap: () async {
                final picked = await _pickDate(_periodEndDate);
                if (picked != null) setState(() => _periodEndDate = picked);
              },
            ),
          ),
        ],
      ),
      const SizedBox(height: 8),
      Row(
        children: [
          Expanded(
            child: OutlinedButton(
              onPressed: _pickPeriodStartTime,
              child: Text('Giờ bắt đầu: ${_periodStartTime.format(context)}'),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: OutlinedButton(
              onPressed: _pickPeriodEndTime,
              child: Text('Giờ kết thúc: ${_periodEndTime.format(context)}'),
            ),
          ),
        ],
      ),
    ];
  }

  Widget _buildDropdown<T>({
    required String label,
    required T value,
    required Map<T, String> items,
    required ValueChanged<T?> onChanged,
  }) {
    return DropdownButtonFormField<T>(
      initialValue: value,
      decoration: _inputDecoration(label),
      items: items.entries
          .map(
            (entry) =>
                DropdownMenuItem<T>(value: entry.key, child: Text(entry.value)),
          )
          .toList(),
      onChanged: onChanged,
    );
  }

  String _formatDate(DateTime value) {
    return '${value.day.toString().padLeft(2, '0')}/${value.month.toString().padLeft(2, '0')}/${value.year}';
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
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
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
