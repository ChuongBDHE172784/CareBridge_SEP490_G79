import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../core/network/api_client.dart';
import '../models/health_metric_model.dart';
import '../models/maternal_metric_lifecycle_policy.dart';
import '../services/health_metric_service.dart';

class EditHealthMetricScreen extends StatefulWidget {
  final String journeyId;
  final HealthMetricDetail metric;

  const EditHealthMetricScreen({
    super.key,
    required this.journeyId,
    required this.metric,
  });

  @override
  State<EditHealthMetricScreen> createState() => _EditHealthMetricScreenState();
}

class _EditHealthMetricScreenState extends State<EditHealthMetricScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surfaceContainer = Color(0xFFF2EAE4);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _errorContainer = Color(0xFFFFDAD6);
  static const _error = Color(0xFFBA1A1A);

  final _formKey = GlobalKey<FormState>();
  final _valuePrimaryCtrl = TextEditingController();
  final _valueSecondaryCtrl = TextEditingController();
  final _noteCtrl = TextEditingController();

  DateTime _measuredDate = DateTime.now();
  TimeOfDay _measuredTime = TimeOfDay.now();

  bool _isSaving = false;
  bool _isDeleting = false;
  bool _lifecycleLoaded = false;
  String? _journeyType;

  late bool _isDualValue;
  bool get _isBmi =>
      widget.metric.metricCode == 'BMI' ||
      widget.metric.metricType == MetricType.bmi;
  bool get _isGlucose => widget.metric.metricCode == 'BLOOD_GLUCOSE';
  bool get _isTemperature =>
      widget.metric.metricCode == 'TEMPERATURE' ||
      widget.metric.metricType == MetricType.temperature;
  bool get _isFetalMovement => const {
    'FETAL_MOVEMENT',
    'FETAL_MOVEMENT_COUNT',
    'FETAL_MOVEMENT_SESSION',
  }.contains(widget.metric.metricCode.toUpperCase());

  String? get _lifecycleRestrictionMessage {
    if (!_isFetalMovement || !_lifecycleLoaded) return null;
    return maternalMetricEntryRestrictionMessage(
      metricType: widget.metric.metricCode,
      journeyType: _journeyType,
    );
  }

  bool get _canEdit =>
      _isWithinEditWindow &&
      (!_isFetalMovement || _lifecycleLoaded) &&
      _lifecycleRestrictionMessage == null;

  String _glucoseContext = 'FASTING';
  String _glucoseUnit = 'mg/dL';
  static const _glucoseUnits = [
    DropdownMenuItem(
      value: 'mg/dL',
      child: Text('mg/dL (Máy đo đường huyết cá nhân)'),
    ),
    DropdownMenuItem(
      value: 'mmol/L',
      child: Text('mmol/L (Chuẩn xét nghiệm y khoa)'),
    ),
  ];
  static const _glucoseContexts = [
    DropdownMenuItem(value: 'FASTING', child: Text('Lúc đói')),
    DropdownMenuItem(value: 'PRE_MEAL', child: Text('Trước ăn')),
    DropdownMenuItem(value: 'POST_MEAL_1H', child: Text('Sau ăn 1 giờ')),
    DropdownMenuItem(value: 'POST_MEAL_2H', child: Text('Sau ăn 2 giờ')),
    DropdownMenuItem(value: 'RANDOM', child: Text('Ngẫu nhiên')),
    DropdownMenuItem(
      value: 'OTHER_APPROVED',
      child: Text('Khác (đã được duyệt)'),
    ),
  ];

  String _temperatureSite = 'ARMPIT';
  static const _temperatureSites = [
    DropdownMenuItem(value: 'ARMPIT', child: Text('Nách (Chuẩn phổ biến)')),
    DropdownMenuItem(value: 'FOREHEAD', child: Text('Trán (Cảm ứng nhiệt)')),
    DropdownMenuItem(value: 'ORAL', child: Text('Miệng')),
    DropdownMenuItem(value: 'EAR', child: Text('Tai (Màng nhĩ)')),
  ];

  final _service = HealthMetricService();

  @override
  void initState() {
    super.initState();
    final m = widget.metric;
    _isDualValue = m.valueSecondary != null || _isBmi;

    if (_isBmi) {
      final weight = m.context['weightKg'] as num?;
      final height = m.context['heightCm'] as num?;
      if (weight != null) {
        _valuePrimaryCtrl.text = weight % 1 == 0
            ? weight.toStringAsFixed(0)
            : weight.toStringAsFixed(1);
      }
      if (height != null) {
        _valueSecondaryCtrl.text = height % 1 == 0
            ? height.toStringAsFixed(0)
            : height.toStringAsFixed(1);
      }
    } else {
      _valuePrimaryCtrl.text = m.valueNumeric % 1 == 0
          ? m.valueNumeric.toStringAsFixed(0)
          : m.valueNumeric.toStringAsFixed(1);
      if (_isDualValue && m.valueSecondary != null) {
        _valueSecondaryCtrl.text = m.valueSecondary! % 1 == 0
            ? m.valueSecondary!.toStringAsFixed(0)
            : m.valueSecondary!.toStringAsFixed(1);
      }
    }
    if (_isGlucose) {
      if (m.unit == 'mmol/L' || m.unit == 'mg/dL') {
        _glucoseUnit = m.unit;
      }
      if (m.context['measurementContext'] != null) {
        _glucoseContext = m.context['measurementContext'].toString();
      }
    }
    if (_isTemperature && m.context['measurementSite'] != null) {
      _temperatureSite = m.context['measurementSite'].toString();
    }
    _noteCtrl.text = m.note ?? '';
    _measuredDate = m.measuredAt;
    _measuredTime = TimeOfDay.fromDateTime(m.measuredAt);
    _loadLifecyclePolicy();
  }

  Future<void> _loadLifecyclePolicy() async {
    if (!_isFetalMovement) {
      _lifecycleLoaded = true;
      return;
    }

    try {
      final response = await apiGet('/api/v1/journeys/me/dashboard');
      final data = response['data'];
      _journeyType = data is Map<String, dynamic>
          ? data['journeyType'] as String?
          : null;
    } catch (_) {
      _journeyType = null;
    } finally {
      if (mounted) {
        setState(() => _lifecycleLoaded = true);
      }
    }
  }

  @override
  void dispose() {
    _valuePrimaryCtrl.dispose();
    _valueSecondaryCtrl.dispose();
    _noteCtrl.dispose();
    super.dispose();
  }

  String get _metricTitle => widget.metric.metricType.displayLabel;
  String get _unit => _isGlucose ? _glucoseUnit : widget.metric.unit;

  bool get _isWithinEditWindow {
    final diff = DateTime.now().difference(widget.metric.createdAt);
    return diff.inHours < 24;
  }

  DateTime get _resolvedMeasuredAt {
    return DateTime(
      _measuredDate.year,
      _measuredDate.month,
      _measuredDate.day,
      _measuredTime.hour,
      _measuredTime.minute,
    );
  }

  Future<void> _pickDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _measuredDate,
      firstDate: DateTime.now().subtract(const Duration(days: 365)),
      lastDate: DateTime.now(),
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

  Future<void> _save() async {
    if (_isFetalMovement && !_lifecycleLoaded) {
      _showError('Đang kiểm tra giai đoạn hành trình. Vui lòng thử lại.');
      return;
    }
    final lifecycleRestriction = _lifecycleRestrictionMessage;
    if (lifecycleRestriction != null) {
      _showError(lifecycleRestriction);
      return;
    }
    if (!_formKey.currentState!.validate()) return;
    if (!_isWithinEditWindow) {
      _showError('Đã quá 24 giờ kể từ khi tạo. Không thể chỉnh sửa.');
      return;
    }

    double? primaryVal;
    double? secondaryVal;

    if (_isBmi) {
      final weightKg = double.tryParse(_valuePrimaryCtrl.text.trim());
      final heightCm = double.tryParse(_valueSecondaryCtrl.text.trim());
      if (weightKg == null ||
          weightKg < 20 ||
          weightKg > 300 ||
          heightCm == null ||
          heightCm < 100 ||
          heightCm > 230) {
        _showError('Nhập cân nặng 20–300 kg và chiều cao 100–230 cm.');
        return;
      }
      primaryVal = weightKg / ((heightCm / 100) * (heightCm / 100));
    } else if (_isTemperature) {
      primaryVal = double.tryParse(_valuePrimaryCtrl.text.trim());
      if (primaryVal == null || primaryVal < 30.0 || primaryVal > 45.0) {
        _showError('Nhập thân nhiệt hợp lệ từ 30.0 đến 45.0 °C.');
        return;
      }
    } else if (_isGlucose) {
      primaryVal = double.tryParse(_valuePrimaryCtrl.text.trim());
      if (_glucoseUnit == 'mg/dL') {
        if (primaryVal == null || primaryVal < 20.0 || primaryVal > 600.0) {
          _showError('Nhập chỉ số đường huyết hợp lệ từ 20 đến 600 mg/dL.');
          return;
        }
      } else {
        if (primaryVal == null || primaryVal < 1.0 || primaryVal > 35.0) {
          _showError('Nhập chỉ số đường huyết hợp lệ từ 1.0 đến 35.0 mmol/L.');
          return;
        }
      }
    } else {
      primaryVal = double.tryParse(_valuePrimaryCtrl.text.trim());
      if (primaryVal == null) {
        _showError('Giá trị chỉ số không hợp lệ.');
        return;
      }
      if (_isDualValue) {
        secondaryVal = double.tryParse(_valueSecondaryCtrl.text.trim());
        if (secondaryVal == null) {
          _showError('Giá trị thứ hai không hợp lệ.');
          return;
        }
      }
    }

    setState(() => _isSaving = true);
    try {
      final contextPayload = Map<String, dynamic>.from(widget.metric.context);
      if (_isBmi) {
        contextPayload['weightKg'] = double.tryParse(
          _valuePrimaryCtrl.text.trim(),
        );
        contextPayload['heightCm'] = double.tryParse(
          _valueSecondaryCtrl.text.trim(),
        );
        contextPayload['pregnancyBasis'] = 'CURRENT_MEASUREMENT';
      }
      if (_isGlucose) {
        contextPayload['measurementContext'] = _glucoseContext;
      }
      if (_isTemperature) {
        contextPayload['measurementSite'] = _temperatureSite;
      }

      await _service.updateMetric(
        widget.journeyId,
        widget.metric.id,
        UpdateMetricRequest(
          valueNumeric: primaryVal,
          valueSecondary: secondaryVal,
          unit: _isGlucose ? _glucoseUnit : widget.metric.unit,
          measuredAt: _resolvedMeasuredAt,
          note: _noteCtrl.text.trim().isEmpty ? null : _noteCtrl.text.trim(),
          context: contextPayload.isEmpty ? null : contextPayload,
        ),
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Đã cập nhật chỉ số thành công'),
            backgroundColor: _primary,
          ),
        );
        Navigator.of(context).pop(true);
      }
    } catch (e) {
      _showError('Không thể cập nhật. Vui lòng thử lại.');
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  Future<void> _delete() async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: _canvas,
        title: const Text(
          'Xóa bản ghi?',
          style: TextStyle(color: _onSurface, fontFamily: 'Lexend'),
        ),
        content: const Text(
          'Hành động này không thể hoàn tác.',
          style: TextStyle(color: _onSurfaceVariant),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text(
              'Hủy',
              style: TextStyle(color: _onSurfaceVariant),
            ),
          ),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Xóa', style: TextStyle(color: _error)),
          ),
        ],
      ),
    );
    if (confirm != true) return;

    setState(() => _isDeleting = true);
    try {
      await _service.deleteMetric(widget.metric.id);
      if (mounted) {
        Navigator.of(context).pop(true);
      }
    } catch (e) {
      _showError('Không thể xóa bản ghi. Vui lòng thử lại.');
    } finally {
      if (mounted) setState(() => _isDeleting = false);
    }
  }

  void _showError(String msg) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(msg), backgroundColor: _error));
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
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              _metricTitle,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 16,
                fontWeight: FontWeight.w600,
                color: _primary,
              ),
            ),
            Text(
              'Cập nhật ${_formatDateTime(widget.metric.measuredAt)}',
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 11,
                color: _onSurfaceVariant,
              ),
            ),
          ],
        ),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              _buildPreviousValueCard(),
              const SizedBox(height: 16),
              if (_isFetalMovement && !_lifecycleLoaded)
                const LinearProgressIndicator(color: _primaryContainer),
              if (_lifecycleRestrictionMessage case final message?)
                _buildLifecycleRestrictionWarning(message),
              if (!_isWithinEditWindow) _buildEditWindowWarning(),
              _buildFormCard(),
              const SizedBox(height: 16),
              _buildInfoCard(),
              const SizedBox(height: 24),
              _buildActionButtons(),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildPreviousValueCard() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: _primary.withAlpha(20),
            blurRadius: 16,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Row(
        children: [
          Container(
            width: 48,
            height: 48,
            decoration: BoxDecoration(
              color: _surfaceContainer,
              borderRadius: BorderRadius.circular(16),
            ),
            child: const Icon(Icons.history_rounded, color: _primary),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Lần đo trước',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    color: _onSurfaceVariant,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  widget.metric.valueDisplay,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 28,
                    fontWeight: FontWeight.w700,
                    color: _primary,
                  ),
                ),
              ],
            ),
          ),
          Text(
            _unit,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildEditWindowWarning() {
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: _errorContainer,
        borderRadius: BorderRadius.circular(16),
      ),
      child: const Row(
        children: [
          Icon(Icons.warning_rounded, color: _error, size: 20),
          SizedBox(width: 10),
          Expanded(
            child: Text(
              'Đã quá 24 giờ kể từ khi tạo. Chỉ số này không còn được chỉnh sửa.',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 13,
                color: _error,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildLifecycleRestrictionWarning(String message) {
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: _surfaceContainer,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        children: [
          const Icon(Icons.info_outline_rounded, color: _primary, size: 20),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              message,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 13,
                color: _onSurfaceVariant,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFormCard() {
    final enabled = _canEdit;
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: _surfaceContainer, width: 1.5),
        boxShadow: [
          BoxShadow(
            color: _primary.withAlpha(15),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Stack(
        children: [
          Positioned.fill(
            child: Align(
              alignment: Alignment.topRight,
              child: Padding(
                padding: const EdgeInsets.all(8),
                child: Icon(
                  Icons.monitor_heart_rounded,
                  size: 80,
                  color: _primary.withAlpha(13),
                ),
              ),
            ),
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (_isBmi) ...[
                Row(
                  children: [
                    Expanded(
                      child: _buildInput(
                        controller: _valuePrimaryCtrl,
                        label: 'Cân nặng',
                        suffix: 'kg',
                        enabled: enabled,
                        keyboardType: const TextInputType.numberWithOptions(
                          decimal: true,
                        ),
                        onChanged: (_) => setState(() {}),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: _buildInput(
                        controller: _valueSecondaryCtrl,
                        label: 'Chiều cao',
                        suffix: 'cm',
                        enabled: enabled,
                        keyboardType: const TextInputType.numberWithOptions(
                          decimal: true,
                        ),
                        onChanged: (_) => setState(() {}),
                      ),
                    ),
                  ],
                ),
                if (_bmiPreview != null) ...[
                  const SizedBox(height: 12),
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 14,
                      vertical: 10,
                    ),
                    decoration: BoxDecoration(
                      color: _surfaceContainer.withAlpha(80),
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: Text(
                      'BMI tự động tính: ${_bmiPreview!.toStringAsFixed(1)} kg/m²',
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 13,
                        fontWeight: FontWeight.w600,
                        color: _primary,
                      ),
                    ),
                  ),
                ],
              ] else if (_isDualValue) ...[
                Row(
                  children: [
                    Expanded(
                      child: _buildInput(
                        controller: _valuePrimaryCtrl,
                        label: 'Tâm thu',
                        suffix: _unit.split('/').first,
                        enabled: enabled,
                        keyboardType: TextInputType.number,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: _buildInput(
                        controller: _valueSecondaryCtrl,
                        label: 'Tâm trương',
                        suffix: _unit.split('/').last,
                        enabled: enabled,
                        keyboardType: TextInputType.number,
                      ),
                    ),
                  ],
                ),
              ] else ...[
                _buildInput(
                  controller: _valuePrimaryCtrl,
                  label: 'Chỉ số đo được',
                  suffix: _unit,
                  enabled: enabled,
                  keyboardType: const TextInputType.numberWithOptions(
                    decimal: true,
                  ),
                ),
              ],
              if (_isGlucose) ...[
                const SizedBox(height: 14),
                DropdownButtonFormField<String>(
                  initialValue:
                      _glucoseUnits.any((item) => item.value == _glucoseUnit)
                      ? _glucoseUnit
                      : 'mg/dL',
                  decoration: InputDecoration(
                    labelText: 'Đơn vị đo đường huyết',
                    labelStyle: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 13,
                      color: _onSurfaceVariant,
                    ),
                    filled: true,
                    fillColor: enabled
                        ? Colors.white
                        : _surfaceContainer.withAlpha(60),
                    contentPadding: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 14,
                    ),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(20),
                      borderSide: const BorderSide(
                        color: _surfaceContainer,
                        width: 2,
                      ),
                    ),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(20),
                      borderSide: const BorderSide(
                        color: _surfaceContainer,
                        width: 2,
                      ),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(20),
                      borderSide: const BorderSide(
                        color: _primaryContainer,
                        width: 2,
                      ),
                    ),
                  ),
                  items: _glucoseUnits,
                  onChanged: enabled
                      ? (val) {
                          if (val != null) setState(() => _glucoseUnit = val);
                        }
                      : null,
                ),
                const SizedBox(height: 14),
                DropdownButtonFormField<String>(
                  initialValue:
                      _glucoseContexts.any(
                        (item) => item.value == _glucoseContext,
                      )
                      ? _glucoseContext
                      : 'FASTING',
                  decoration: InputDecoration(
                    labelText: 'Bối cảnh đo',
                    labelStyle: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 13,
                      color: _onSurfaceVariant,
                    ),
                    filled: true,
                    fillColor: enabled
                        ? Colors.white
                        : _surfaceContainer.withAlpha(60),
                    contentPadding: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 14,
                    ),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(20),
                      borderSide: const BorderSide(
                        color: _surfaceContainer,
                        width: 2,
                      ),
                    ),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(20),
                      borderSide: const BorderSide(
                        color: _surfaceContainer,
                        width: 2,
                      ),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(20),
                      borderSide: const BorderSide(
                        color: _primaryContainer,
                        width: 2,
                      ),
                    ),
                  ),
                  items: _glucoseContexts,
                  onChanged: enabled
                      ? (val) {
                          if (val != null) {
                            setState(() => _glucoseContext = val);
                          }
                        }
                      : null,
                ),
              ],
              if (_isTemperature) ...[
                const SizedBox(height: 14),
                DropdownButtonFormField<String>(
                  initialValue:
                      _temperatureSites.any(
                        (item) => item.value == _temperatureSite,
                      )
                      ? _temperatureSite
                      : 'ARMPIT',
                  decoration: InputDecoration(
                    labelText: 'Vị trí đo thân nhiệt',
                    labelStyle: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 13,
                      color: _onSurfaceVariant,
                    ),
                    filled: true,
                    fillColor: enabled
                        ? Colors.white
                        : _surfaceContainer.withAlpha(60),
                    contentPadding: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 14,
                    ),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(20),
                      borderSide: const BorderSide(
                        color: _surfaceContainer,
                        width: 2,
                      ),
                    ),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(20),
                      borderSide: const BorderSide(
                        color: _surfaceContainer,
                        width: 2,
                      ),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(20),
                      borderSide: const BorderSide(
                        color: _primaryContainer,
                        width: 2,
                      ),
                    ),
                  ),
                  items: _temperatureSites,
                  onChanged: enabled
                      ? (val) {
                          if (val != null) {
                            setState(() => _temperatureSite = val);
                          }
                        }
                      : null,
                ),
              ],
              const SizedBox(height: 14),
              // Source readonly
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 14,
                ),
                decoration: BoxDecoration(
                  color: _surfaceContainer.withAlpha(80),
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(color: _surfaceContainer, width: 2),
                ),
                child: Row(
                  children: [
                    const Icon(
                      Icons.edit_note_rounded,
                      color: _onSurfaceVariant,
                      size: 20,
                    ),
                    const SizedBox(width: 10),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Nguồn dữ liệu',
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 11,
                            color: _onSurfaceVariant,
                          ),
                        ),
                        Text(
                          widget.metric.sourceType.displayLabel,
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 14,
                            color: _onSurface,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 14),
              Row(
                children: [
                  Expanded(child: _buildDateTile(enabled: enabled)),
                  const SizedBox(width: 12),
                  Expanded(child: _buildTimeTile(enabled: enabled)),
                ],
              ),
              const SizedBox(height: 14),
              _buildNoteField(enabled: enabled),
            ],
          ),
        ],
      ),
    );
  }

  double? get _bmiPreview {
    final weight = double.tryParse(_valuePrimaryCtrl.text.trim());
    final height = double.tryParse(_valueSecondaryCtrl.text.trim());
    if (weight == null || weight <= 0 || height == null || height <= 0) {
      return null;
    }
    return weight / ((height / 100) * (height / 100));
  }

  Widget _buildInput({
    required TextEditingController controller,
    required String label,
    required String suffix,
    required bool enabled,
    TextInputType keyboardType = TextInputType.text,
    ValueChanged<String>? onChanged,
  }) {
    return TextFormField(
      controller: controller,
      enabled: enabled,
      keyboardType: keyboardType,
      onChanged: onChanged,
      inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'[0-9.]'))],
      validator: (v) =>
          (v == null || v.isEmpty) ? 'Vui lòng nhập giá trị' : null,
      style: const TextStyle(
        fontFamily: 'Lexend',
        fontSize: 16,
        fontWeight: FontWeight.w600,
        color: _onSurface,
      ),
      decoration: InputDecoration(
        labelText: label,
        labelStyle: const TextStyle(
          fontFamily: 'Lexend',
          fontSize: 13,
          color: _onSurfaceVariant,
        ),
        suffixText: suffix,
        suffixStyle: const TextStyle(
          fontFamily: 'Lexend',
          fontSize: 13,
          color: _onSurfaceVariant,
        ),
        filled: true,
        fillColor: enabled ? Colors.white : _surfaceContainer.withAlpha(60),
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 16,
          vertical: 14,
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(20),
          borderSide: const BorderSide(color: _surfaceContainer, width: 2),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(20),
          borderSide: const BorderSide(color: _surfaceContainer, width: 2),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(20),
          borderSide: const BorderSide(color: _primaryContainer, width: 2),
        ),
        disabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(20),
          borderSide: const BorderSide(color: _surfaceContainer, width: 1),
        ),
      ),
    );
  }

  Widget _buildDateTile({required bool enabled}) {
    return GestureDetector(
      onTap: enabled ? _pickDate : null,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
        decoration: BoxDecoration(
          color: enabled ? Colors.white : _surfaceContainer.withAlpha(60),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: _surfaceContainer, width: 2),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Ngày đo',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 11,
                color: _onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 2),
            Text(
              '${_measuredDate.day.toString().padLeft(2, '0')}/'
              '${_measuredDate.month.toString().padLeft(2, '0')}/'
              '${_measuredDate.year}',
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                fontWeight: FontWeight.w600,
                color: _onSurface,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTimeTile({required bool enabled}) {
    return GestureDetector(
      onTap: enabled ? _pickTime : null,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
        decoration: BoxDecoration(
          color: enabled ? Colors.white : _surfaceContainer.withAlpha(60),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: _surfaceContainer, width: 2),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Giờ đo',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 11,
                color: _onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 2),
            Text(
              '${_measuredTime.hour.toString().padLeft(2, '0')}:${_measuredTime.minute.toString().padLeft(2, '0')}',
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                fontWeight: FontWeight.w600,
                color: _onSurface,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildNoteField({required bool enabled}) {
    return TextFormField(
      controller: _noteCtrl,
      enabled: enabled,
      maxLines: 3,
      maxLength: 2000,
      style: const TextStyle(
        fontFamily: 'Lexend',
        fontSize: 14,
        color: _onSurface,
      ),
      decoration: InputDecoration(
        labelText: 'Ghi chú (tùy chọn)',
        labelStyle: const TextStyle(
          fontFamily: 'Lexend',
          fontSize: 13,
          color: _onSurfaceVariant,
        ),
        filled: true,
        fillColor: enabled ? Colors.white : _surfaceContainer.withAlpha(60),
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 16,
          vertical: 14,
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(20),
          borderSide: const BorderSide(color: _surfaceContainer, width: 2),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(20),
          borderSide: const BorderSide(color: _surfaceContainer, width: 2),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(20),
          borderSide: const BorderSide(color: _primaryContainer, width: 2),
        ),
        disabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(20),
          borderSide: const BorderSide(color: _surfaceContainer, width: 1),
        ),
      ),
    );
  }

  Widget _buildInfoCard() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0x4DC98C7B).withAlpha(30),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: _primaryContainer.withAlpha(80), width: 1),
      ),
      child: const Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.info_outline_rounded, color: _primaryContainer, size: 20),
          SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Lời khuyên từ chuyên gia',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: _primary,
                  ),
                ),
                SizedBox(height: 4),
                Text(
                  'Chỉ số có thể được chỉnh sửa trong vòng 24 giờ sau khi tạo. Vui lòng đo đúng thời điểm và ghi chú bối cảnh để có kết quả chính xác.',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    color: _onSurfaceVariant,
                    height: 1.5,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildActionButtons() {
    final enabled = _canEdit;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        ElevatedButton.icon(
          onPressed: enabled && !_isSaving ? _save : null,
          icon: _isSaving
              ? const SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: Colors.white,
                  ),
                )
              : const Icon(Icons.save_rounded),
          label: const Text(
            'Lưu thay đổi',
            style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w600),
          ),
          style: ElevatedButton.styleFrom(
            backgroundColor: _primaryContainer,
            foregroundColor: Colors.white,
            disabledBackgroundColor: _surfaceContainer,
            padding: const EdgeInsets.symmetric(vertical: 16),
            shape: const StadiumBorder(),
            elevation: 0,
          ),
        ),
        const SizedBox(height: 10),
        OutlinedButton(
          onPressed: () => Navigator.of(context).pop(),
          style: OutlinedButton.styleFrom(
            foregroundColor: _onSurfaceVariant,
            padding: const EdgeInsets.symmetric(vertical: 16),
            side: const BorderSide(color: _surfaceContainer, width: 1.5),
            shape: const StadiumBorder(),
          ),
          child: const Text(
            'Hủy bỏ',
            style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w600),
          ),
        ),
        const SizedBox(height: 10),
        ElevatedButton.icon(
          onPressed: _isWithinEditWindow && !_isDeleting ? _delete : null,
          icon: _isDeleting
              ? const SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: _error,
                  ),
                )
              : const Icon(Icons.delete_forever_rounded),
          label: const Text(
            'Xóa bản ghi',
            style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w600),
          ),
          style: ElevatedButton.styleFrom(
            backgroundColor: _errorContainer,
            foregroundColor: _error,
            disabledBackgroundColor: _surfaceContainer,
            padding: const EdgeInsets.symmetric(vertical: 16),
            shape: const StadiumBorder(),
            elevation: 0,
          ),
        ),
      ],
    );
  }

  String _formatDateTime(DateTime dt) {
    final time =
        '${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';
    final date =
        '${dt.day.toString().padLeft(2, '0')}/${dt.month.toString().padLeft(2, '0')}/${dt.year}';
    return '$time, $date';
  }
}
