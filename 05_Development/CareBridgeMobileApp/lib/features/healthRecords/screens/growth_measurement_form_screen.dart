import 'package:flutter/material.dart';

import '../models/growth_measurement_model.dart';
import '../services/growth_measurement_service.dart';

/// Shared form for creating and editing a baby growth measurement.
///
/// The backend accepts the same metric fields for POST and PATCH. Keeping the
/// validation and payload mapping here prevents the two flows from drifting.
class GrowthMeasurementFormScreen extends StatefulWidget {
  final String babyId;
  final GrowthMeasurement? measurement;
  final GrowthMeasurementService? service;
  final Future<void> Function(String babyId, Map<String, dynamic> payload)?
  onAdd;
  final Future<void> Function(
    String babyId,
    String measurementId,
    Map<String, dynamic> payload,
  )?
  onUpdate;

  const GrowthMeasurementFormScreen({
    super.key,
    required this.babyId,
    this.measurement,
    this.service,
    this.onAdd,
    this.onUpdate,
  });

  bool get isEdit => measurement != null;

  @override
  State<GrowthMeasurementFormScreen> createState() =>
      _GrowthMeasurementFormScreenState();
}

class _GrowthMeasurementFormScreenState
    extends State<GrowthMeasurementFormScreen> {
  late DateTime _measuredDate;
  late final TextEditingController _weightController;
  late final TextEditingController _heightController;
  late final TextEditingController _headController;
  late final TextEditingController _sourceController;
  late final TextEditingController _noteController;

  bool _isSaving = false;
  String? _validationError;
  String? _saveError;

  GrowthMeasurementService get _service =>
      widget.service ?? GrowthMeasurementService();

  @override
  void initState() {
    super.initState();
    final existing = widget.measurement;
    _measuredDate = existing?.measuredAt ?? DateUtils.dateOnly(DateTime.now());
    _weightController = TextEditingController(
      text: existing?.weightKg?.toString() ?? '',
    );
    _heightController = TextEditingController(
      text: existing?.heightCm?.toString() ?? '',
    );
    _headController = TextEditingController(
      text: existing?.headCircumferenceCm?.toString() ?? '',
    );
    _sourceController = TextEditingController(
      text: existing?.sourceType?.trim().isNotEmpty == true
          ? existing!.sourceType!.trim()
          : existing == null
          ? 'HOME_SCALE'
          : '',
    );
    _noteController = TextEditingController(text: existing?.note ?? '');
  }

  @override
  void dispose() {
    _weightController.dispose();
    _heightController.dispose();
    _headController.dispose();
    _sourceController.dispose();
    _noteController.dispose();
    super.dispose();
  }

  Future<void> _pickDate() async {
    final today = DateUtils.dateOnly(DateTime.now());
    final picked = await showDatePicker(
      context: context,
      initialDate: _measuredDate.isAfter(today) ? today : _measuredDate,
      firstDate: DateTime(1900),
      lastDate: today,
      helpText: 'Chọn ngày đo',
      cancelText: 'Hủy',
      confirmText: 'Chọn',
    );
    if (picked != null && mounted) {
      setState(() {
        _measuredDate = DateUtils.dateOnly(picked);
        _validationError = null;
      });
    }
  }

  double? _parseMetric(TextEditingController controller) {
    final text = controller.text.trim().replaceAll(',', '.');
    if (text.isEmpty) return null;
    final value = double.tryParse(text);
    return value != null && value.isFinite ? value : null;
  }

  Future<void> _save() async {
    if (_isSaving) return;
    FocusManager.instance.primaryFocus?.unfocus();
    setState(() {
      _validationError = null;
      _saveError = null;
    });

    final today = DateUtils.dateOnly(DateTime.now());
    final weight = _parseMetric(_weightController);
    final height = _parseMetric(_heightController);
    final head = _parseMetric(_headController);
    final hasInvalidNumber =
        (_weightController.text.trim().isNotEmpty && weight == null) ||
        (_heightController.text.trim().isNotEmpty && height == null) ||
        (_headController.text.trim().isNotEmpty && head == null);

    if (_measuredDate.isAfter(today)) {
      setState(() => _validationError = 'Ngày đo không được ở tương lai.');
      return;
    }
    if (hasInvalidNumber) {
      setState(() => _validationError = 'Nhập số đo hợp lệ.');
      return;
    }
    if ([weight, height, head].whereType<double>().any((value) => value < 0)) {
      setState(() => _validationError = 'Số đo không được là số âm.');
      return;
    }
    if (weight == null && height == null && head == null) {
      setState(
        () => _validationError =
            'Hãy nhập ít nhất một trong cân nặng, chiều cao hoặc vòng đầu.',
      );
      return;
    }

    final existing = widget.measurement;
    final clearsExistingMetric =
        existing != null &&
        ((existing.weightKg != null && _weightController.text.trim().isEmpty) ||
            (existing.heightCm != null &&
                _heightController.text.trim().isEmpty) ||
            (existing.headCircumferenceCm != null &&
                _headController.text.trim().isEmpty));
    if (clearsExistingMetric) {
      setState(
        () => _validationError =
            'Không thể xóa số đo hiện có; hãy giữ nguyên hoặc nhập giá trị mới.',
      );
      return;
    }

    final source = _sourceController.text.trim();
    if (widget.measurement == null && source.isEmpty) {
      setState(() => _validationError = 'Hãy nhập nguồn đo.');
      return;
    }

    final payload = <String, dynamic>{'measuredDate': _isoDate(_measuredDate)};
    final note = _noteController.text.trim();
    final existingNote = existing?.note?.trim();
    if (existing == null || note != existingNote) {
      payload['note'] = note;
    }
    if (source.isNotEmpty) payload['sourceType'] = source;
    if (weight != null) payload['weightKg'] = weight;
    if (height != null) payload['heightCm'] = height;
    if (head != null) payload['headCircumferenceCm'] = head;

    setState(() => _isSaving = true);
    try {
      if (existing == null) {
        if (widget.onAdd != null) {
          await widget.onAdd!(widget.babyId, payload).timeout(
            const Duration(seconds: 30),
          );
        } else {
          await _service
              .addGrowthMeasurement(widget.babyId, payload)
              .timeout(const Duration(seconds: 30));
        }
      } else {
        if (widget.onUpdate != null) {
          await widget.onUpdate!(widget.babyId, existing.id, payload).timeout(
            const Duration(seconds: 30),
          );
        } else {
          await _service
              .updateGrowthMeasurement(widget.babyId, existing.id, payload)
              .timeout(const Duration(seconds: 30));
        }
      }
      if (mounted) Navigator.of(context).pop(true);
    } catch (_) {
      if (mounted) {
        setState(() {
          _isSaving = false;
          _saveError = 'Không thể lưu số đo. Kiểm tra kết nối và thử lại.';
        });
      }
    }
  }

  static String _isoDate(DateTime date) {
    final year = date.year.toString().padLeft(4, '0');
    final month = date.month.toString().padLeft(2, '0');
    final day = date.day.toString().padLeft(2, '0');
    return '$year-$month-$day';
  }

  @override
  Widget build(BuildContext context) {
    final title = widget.isEdit ? 'Chỉnh sửa số đo' : 'Thêm số đo tăng trưởng';
    return PopScope(
      canPop: !_isSaving,
      child: Scaffold(
        key: const Key('growth-measurement-form-screen'),
        backgroundColor: const Color(0xFFFEF8F4),
        appBar: AppBar(
          backgroundColor: const Color(0xFFFEF8F4),
          elevation: 0,
          leading: IconButton(
            key: const Key('growth-form-back'),
            icon: const Icon(Icons.arrow_back, color: Color(0xFF845143)),
            onPressed: _isSaving ? null : () => Navigator.of(context).pop(),
          ),
          title: Text(
            title,
            style: const TextStyle(
              color: Color(0xFF845143),
              fontSize: 20,
              fontWeight: FontWeight.bold,
            ),
          ),
        ),
        body: SafeArea(
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                _sectionCard(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const Text(
                        'Ngày đo',
                        style: TextStyle(
                          color: Color(0xFF605E5A),
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: 8),
                      OutlinedButton.icon(
                        key: const Key('growth-form-date'),
                        onPressed: _isSaving ? null : _pickDate,
                        icon: const Icon(Icons.calendar_today),
                        label: Text(_displayDate(_measuredDate)),
                        style: _outlineButtonStyle(),
                      ),
                      const SizedBox(height: 16),
                      _numberField(
                        key: const Key('growth-form-weight'),
                        controller: _weightController,
                        label: 'Cân nặng (kg)',
                        icon: Icons.scale,
                      ),
                      const SizedBox(height: 12),
                      _numberField(
                        key: const Key('growth-form-height'),
                        controller: _heightController,
                        label: 'Chiều cao (cm)',
                        icon: Icons.height,
                      ),
                      const SizedBox(height: 12),
                      _numberField(
                        key: const Key('growth-form-head'),
                        controller: _headController,
                        label: 'Vòng đầu (cm)',
                        icon: Icons.face,
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 16),
                _sectionCard(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      _textField(
                        key: const Key('growth-form-source'),
                        controller: _sourceController,
                        label: 'Nguồn đo',
                        hint: 'Ví dụ: HOME_SCALE hoặc CLINIC',
                      ),
                      const SizedBox(height: 12),
                      _textField(
                        key: const Key('growth-form-note'),
                        controller: _noteController,
                        label: 'Ghi chú',
                        maxLines: 4,
                      ),
                    ],
                  ),
                ),
                if (_validationError != null || _saveError != null) ...[
                  const SizedBox(height: 12),
                  Semantics(
                    liveRegion: true,
                    child: Container(
                      key: const Key('growth-form-error'),
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: const Color(0xFFFFEDEA),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Text(
                        _validationError ?? _saveError!,
                        style: const TextStyle(color: Color(0xFF9A2E25)),
                      ),
                    ),
                  ),
                ],
                const SizedBox(height: 24),
                ElevatedButton(
                  key: const Key('growth-form-save'),
                  onPressed: _isSaving ? null : _save,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF845143),
                    foregroundColor: Colors.white,
                    minimumSize: const Size.fromHeight(52),
                    shape: const StadiumBorder(),
                  ),
                  child: _isSaving
                      ? Semantics(
                          label: 'Đang lưu số đo',
                          child: SizedBox(
                            width: 22,
                            height: 22,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: Colors.white,
                            ),
                          ),
                        )
                      : Text(widget.isEdit ? 'Lưu thay đổi' : 'Lưu số đo'),
                ),
                const SizedBox(height: 8),
                TextButton(
                  key: const Key('growth-form-cancel'),
                  onPressed: _isSaving
                      ? null
                      : () => Navigator.of(context).pop(),
                  child: const Text(
                    'Hủy bỏ',
                    style: TextStyle(color: Color(0xFF605E5A)),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _sectionCard({required Widget child}) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: const [
          BoxShadow(
            color: Color(0x14C98C7B),
            blurRadius: 20,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: child,
    );
  }

  Widget _numberField({
    required Key key,
    required TextEditingController controller,
    required String label,
    required IconData icon,
  }) {
    return TextFormField(
      key: key,
      controller: controller,
      keyboardType: const TextInputType.numberWithOptions(decimal: true),
      decoration: InputDecoration(
        labelText: label,
        prefixIcon: Icon(icon, color: const Color(0xFF845143)),
        border: const OutlineInputBorder(),
      ),
    );
  }

  Widget _textField({
    required Key key,
    required TextEditingController controller,
    required String label,
    String? hint,
    int maxLines = 1,
  }) {
    return TextFormField(
      key: key,
      controller: controller,
      maxLines: maxLines,
      maxLength: maxLines > 1 ? 1000 : 30,
      decoration: InputDecoration(
        labelText: label,
        hintText: hint,
        border: const OutlineInputBorder(),
      ),
    );
  }

  ButtonStyle _outlineButtonStyle() => OutlinedButton.styleFrom(
    foregroundColor: const Color(0xFF845143),
    alignment: Alignment.centerLeft,
    padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
    side: const BorderSide(color: Color(0xFFE7E1DD)),
    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
  );

  static String _displayDate(DateTime date) {
    final day = date.day.toString().padLeft(2, '0');
    final month = date.month.toString().padLeft(2, '0');
    return '$day/$month/${date.year}';
  }
}
