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
    _noteController = TextEditingController(text: existing?.note ?? '');
  }

  @override
  void dispose() {
    _weightController.dispose();
    _heightController.dispose();
    _headController.dispose();
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

    final source = existing?.sourceType?.trim().isNotEmpty == true
        ? existing!.sourceType!.trim()
        : 'HOME_SCALE';

    final payload = <String, dynamic>{
      'measuredDate': _isoDate(_measuredDate),
      'sourceType': source,
    };
    final note = _noteController.text.trim();
    final existingNote = existing?.note?.trim() ?? '';
    if (existing == null || note != existingNote) {
      payload['note'] = note;
    }
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
              fontFamily: 'Lexend',
              color: Color(0xFF845143),
              fontSize: 20,
              fontWeight: FontWeight.bold,
            ),
          ),
        ),
        body: SafeArea(
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(20, 12, 20, 36),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                _buildCard(
                  title: 'Chỉ số đo lường',
                  subtitle: 'Ghi nhận ít nhất một chỉ số để theo dõi biểu đồ',
                  icon: Icons.straighten_rounded,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      // Ngày đo selector
                      const Text(
                        'NGÀY ĐO',
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                          color: Color(0xFF84736F),
                          letterSpacing: 0.5,
                        ),
                      ),
                      const SizedBox(height: 8),
                      InkWell(
                        key: const Key('growth-form-date'),
                        onTap: _isSaving ? null : _pickDate,
                        borderRadius: BorderRadius.circular(16),
                        child: Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 16,
                            vertical: 14,
                          ),
                          decoration: BoxDecoration(
                            color: const Color(0xFFFAF7F5),
                            borderRadius: BorderRadius.circular(16),
                            border: Border.all(color: const Color(0xFFE7E1DD)),
                          ),
                          child: Row(
                            children: [
                              const Icon(
                                Icons.calendar_today_rounded,
                                size: 20,
                                color: Color(0xFF845143),
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Text(
                                  _displayDate(_measuredDate),
                                  style: const TextStyle(
                                    fontSize: 15,
                                    fontWeight: FontWeight.w600,
                                    color: Color(0xFF1D1B19),
                                  ),
                                ),
                              ),
                              const Icon(
                                Icons.arrow_forward_ios_rounded,
                                size: 14,
                                color: Color(0xFFB0A5A0),
                              ),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(height: 20),

                      // Cân nặng
                      _buildMetricField(
                        key: const Key('growth-form-weight'),
                        controller: _weightController,
                        label: 'Cân nặng',
                        unit: 'kg',
                        hint: 'Ví dụ: 6.5',
                        icon: Icons.scale_outlined,
                      ),
                      const SizedBox(height: 16),

                      // Chiều cao
                      _buildMetricField(
                        key: const Key('growth-form-height'),
                        controller: _heightController,
                        label: 'Chiều cao',
                        unit: 'cm',
                        hint: 'Ví dụ: 62.0',
                        icon: Icons.straighten_outlined,
                      ),
                      const SizedBox(height: 16),

                      // Vòng đầu
                      _buildMetricField(
                        key: const Key('growth-form-head'),
                        controller: _headController,
                        label: 'Vòng đầu',
                        unit: 'cm',
                        hint: 'Ví dụ: 41.5',
                        icon: Icons.face_outlined,
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 20),

                _buildCard(
                  title: 'Ghi chú',
                  subtitle: 'Tình trạng hoặc lưu ý sức khỏe (tùy chọn)',
                  icon: Icons.edit_note_rounded,
                  child: TextFormField(
                    key: const Key('growth-form-note'),
                    controller: _noteController,
                    maxLines: 3,
                    maxLength: 1000,
                    style: const TextStyle(
                      fontSize: 15,
                      color: Color(0xFF1D1B19),
                    ),
                    decoration: InputDecoration(
                      hintText: 'Nhập ghi chú hoặc theo dõi tình trạng phát triển của bé...',
                      hintStyle: const TextStyle(
                        fontSize: 14,
                        color: Color(0xFFB0A5A0),
                      ),
                      filled: true,
                      fillColor: const Color(0xFFFAF7F5),
                      contentPadding: const EdgeInsets.all(16),
                      enabledBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(16),
                        borderSide: const BorderSide(color: Color(0xFFE7E1DD)),
                      ),
                      focusedBorder: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(16),
                        borderSide: const BorderSide(
                          color: Color(0xFFC98C7B),
                          width: 1.5,
                        ),
                      ),
                      counterStyle: const TextStyle(
                        fontSize: 12,
                        color: Color(0xFF84736F),
                      ),
                    ),
                  ),
                ),

                if (_validationError != null || _saveError != null) ...[
                  const SizedBox(height: 16),
                  Semantics(
                    liveRegion: true,
                    child: Container(
                      key: const Key('growth-form-error'),
                      padding: const EdgeInsets.symmetric(
                        horizontal: 16,
                        vertical: 12,
                      ),
                      decoration: BoxDecoration(
                        color: const Color(0xFFFFEDEA),
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(color: const Color(0xFFFFD5CE)),
                      ),
                      child: Row(
                        children: [
                          const Icon(
                            Icons.error_outline_rounded,
                            color: Color(0xFF9A2E25),
                            size: 20,
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Text(
                              _validationError ?? _saveError!,
                              style: const TextStyle(
                                fontSize: 13,
                                fontWeight: FontWeight.w500,
                                color: Color(0xFF9A2E25),
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ],

                const SizedBox(height: 28),
                ElevatedButton(
                  key: const Key('growth-form-save'),
                  onPressed: _isSaving ? null : _save,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF845143),
                    foregroundColor: Colors.white,
                    disabledBackgroundColor:
                        const Color(0xFF845143).withValues(alpha: 0.6),
                    elevation: 2,
                    shadowColor: const Color(0x33845143),
                    minimumSize: const Size.fromHeight(54),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                  ),
                  child: _isSaving
                      ? Semantics(
                          label: 'Đang lưu số đo',
                          child: const SizedBox(
                            width: 22,
                            height: 22,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: Colors.white,
                            ),
                          ),
                        )
                      : Text(
                          widget.isEdit ? 'LƯU THAY ĐỔI' : 'LƯU SỐ ĐO',
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 15,
                            fontWeight: FontWeight.w700,
                            letterSpacing: 0.5,
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

  Widget _buildCard({
    required String title,
    required String subtitle,
    required IconData icon,
    required Widget child,
  }) {
    return Container(
      padding: const EdgeInsets.all(20),
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
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Container(
                width: 36,
                height: 36,
                decoration: const BoxDecoration(
                  color: Color(0xFFF2EAE4),
                  shape: BoxShape.circle,
                ),
                child: Icon(icon, color: const Color(0xFF845143), size: 20),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                        color: Color(0xFF2D2A28),
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      subtitle,
                      style: const TextStyle(
                        fontSize: 12,
                        color: Color(0xFF84736F),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          child,
        ],
      ),
    );
  }

  Widget _buildMetricField({
    required Key key,
    required TextEditingController controller,
    required String label,
    required String unit,
    required String hint,
    required IconData icon,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label.toUpperCase(),
          style: const TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.bold,
            color: Color(0xFF84736F),
            letterSpacing: 0.5,
          ),
        ),
        const SizedBox(height: 8),
        TextFormField(
          key: key,
          controller: controller,
          keyboardType: const TextInputType.numberWithOptions(decimal: true),
          style: const TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w600,
            color: Color(0xFF1D1B19),
          ),
          decoration: InputDecoration(
            hintText: hint,
            hintStyle: const TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.normal,
              color: Color(0xFFB0A5A0),
            ),
            prefixIcon: Icon(icon, color: const Color(0xFF845143), size: 22),
            suffixText: unit,
            suffixStyle: const TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.bold,
              color: Color(0xFF845143),
            ),
            filled: true,
            fillColor: const Color(0xFFFAF7F5),
            contentPadding: const EdgeInsets.symmetric(
              horizontal: 16,
              vertical: 14,
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(16),
              borderSide: const BorderSide(color: Color(0xFFE7E1DD)),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(16),
              borderSide: const BorderSide(
                color: Color(0xFFC98C7B),
                width: 1.5,
              ),
            ),
          ),
        ),
      ],
    );
  }

  static String _displayDate(DateTime date) {
    final day = date.day.toString().padLeft(2, '0');
    final month = date.month.toString().padLeft(2, '0');
    return '$day/$month/${date.year}';
  }
}
