import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../models/vaccination_model.dart';
import '../services/vaccination_service.dart';

/// Shared UC-229/UC-230 form for adding and editing vaccination records.
/// Styled with CareBridge Warm Claymorphism and card-based layout.
class VaccinationRecordFormScreen extends StatefulWidget {
  final String babyId;
  final String? recordId;
  final VaccinationRecord? initialRecord;
  final VaccinationService? vaccinationService;

  const VaccinationRecordFormScreen({
    super.key,
    required this.babyId,
    this.recordId,
    this.initialRecord,
    this.vaccinationService,
  });

  bool get isEditing => recordId != null;

  @override
  State<VaccinationRecordFormScreen> createState() =>
      _VaccinationRecordFormScreenState();
}

class _VaccinationRecordFormScreenState
    extends State<VaccinationRecordFormScreen> {
  static const _canvas = Color(0xFFFEF8F4);
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _onSurface = Color(0xFF1D1B19);
  static const _onSurfaceVariant = Color(0xFF84736F);
  static const _inputFill = Color(0xFFFAF7F5);
  static const _inputBorder = Color(0xFFE7E1DD);
  static const _hintColor = Color(0xFFB0A5A0);
  static const _error = Color(0xFFBA1A1A);

  final _formKey = GlobalKey<FormState>();
  late final VaccinationService _service;
  late final TextEditingController _vaccineController;
  late final TextEditingController _doseController;
  late final TextEditingController _facilityController;
  DateTime? _administeredDate;
  bool _saving = false;

  bool get _isEditing => widget.isEditing;

  @override
  void initState() {
    super.initState();
    _service = widget.vaccinationService ?? VaccinationService();
    final record = widget.initialRecord;
    _vaccineController = TextEditingController(text: record?.vaccineName ?? '');
    _doseController = TextEditingController(
      text: record?.doseNumber?.toString() ?? '',
    );
    _facilityController = TextEditingController(
      text: record?.facilityName ?? '',
    );
    _administeredDate = record?.administeredDate;
  }

  @override
  void dispose() {
    _vaccineController.dispose();
    _doseController.dispose();
    _facilityController.dispose();
    super.dispose();
  }

  Future<void> _pickDate() async {
    final today = _dateOnly(DateTime.now());
    final picked = await showDatePicker(
      context: context,
      initialDate: _clampDate(
        _administeredDate ?? today,
        DateTime(1900),
        today,
      ),
      firstDate: DateTime(1900),
      lastDate: today,
      helpText: 'Chọn ngày đã tiêm',
      cancelText: 'Hủy',
      confirmText: 'Chọn',
    );
    if (picked != null && mounted) {
      setState(() => _administeredDate = _dateOnly(picked));
    }
  }

  Future<void> _save({required bool offerReminder}) async {
    if (_saving || !_validate()) return;

    final dose = int.parse(_doseController.text.trim());
    final facility = _facilityController.text.trim();
    final originalFacility = widget.initialRecord?.facilityName?.trim();
    final facilityChanged = !_isEditing || facility != (originalFacility ?? '');
    final payload = VaccinationRecordPayload.save(
      vaccineName: _vaccineController.text,
      doseNumber: dose,
      administeredDate: _administeredDate,
      facilityName: facilityChanged ? facility : null,
    );

    setState(() => _saving = true);
    try {
      late final VaccinationRecord saved;
      if (_isEditing) {
        saved = await _service.updateVaccination(
          widget.babyId,
          widget.recordId!,
          payload,
        );
      } else {
        saved = await _service.addVaccinationRecord(widget.babyId, payload);
      }

      if (!mounted) return;
      if (_isEditing || !offerReminder) {
        Navigator.pop(context, true);
        return;
      }

      final createReminder = await _askForReminder();
      if (createReminder == true && mounted) {
        await context.push(
          '/reminders/vaccination/add?babyId=${Uri.encodeComponent(widget.babyId)}',
          extra: <String, dynamic>{
            'vaccinationRecordId': saved.vaccinationId,
            'vaccineName': saved.vaccineName,
            'doseNumber': saved.doseNumber ?? dose,
          },
        );
      }
      if (mounted) Navigator.pop(context, true);
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Không thể lưu hồ sơ tiêm chủng: $error'),
          backgroundColor: _error,
        ),
      );
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  bool _validate() {
    if (!_formKey.currentState!.validate()) return false;
    if (!_isEditing && _administeredDate == null) {
      _showError('Vui lòng chọn ngày đã tiêm.');
      return false;
    }
    if (_administeredDate != null &&
        _dateOnly(_administeredDate!).isAfter(_dateOnly(DateTime.now()))) {
      _showError('Ngày đã tiêm không được ở tương lai.');
      return false;
    }
    return true;
  }

  Future<bool?> _askForReminder() {
    return showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
        title: const Text(
          'Tạo nhắc tiêm riêng?',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontWeight: FontWeight.bold,
            color: _onSurface,
          ),
        ),
        content: const Text(
          'Bạn có thể chọn ngày và giờ nhắc riêng cho mũi tiếp theo. Thao tác này không thay đổi hồ sơ tiêm.',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 14,
            color: _onSurfaceVariant,
            height: 1.4,
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: const Text(
              'Để sau',
              style: TextStyle(
                fontFamily: 'Lexend',
                color: _onSurfaceVariant,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(dialogContext, true),
            style: FilledButton.styleFrom(
              backgroundColor: _primary,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
            ),
            child: const Text(
              'Tạo nhắc',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
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
        scrolledUnderElevation: 0,
        centerTitle: true,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: _onSurface),
          onPressed: _saving ? null : () => Navigator.pop(context),
        ),
        title: Text(
          _isEditing ? 'Sửa hồ sơ tiêm chủng' : 'Thêm hồ sơ tiêm chủng',
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 18,
            fontWeight: FontWeight.bold,
            color: _onSurface,
          ),
        ),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 12, 20, 36),
          children: [
            // Card 1: Thông tin vắc xin
            _buildCard(
              title: 'Thông tin vắc xin',
              subtitle: 'Tên vắc xin và số thứ tự mũi tiêm',
              icon: Icons.vaccines_rounded,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Text(
                    'TÊN VẮC XIN *',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                      color: _onSurfaceVariant,
                      letterSpacing: 0.5,
                    ),
                  ),
                  const SizedBox(height: 8),
                  TextFormField(
                    controller: _vaccineController,
                    maxLength: 200,
                    enabled: !_saving,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 15,
                      color: _onSurface,
                    ),
                    decoration: _buildInputDecoration(
                      hint: 'Ví dụ: 6 trong 1 (Hexaxim), Phế cầu...',
                      prefixIcon: Icons.vaccines_outlined,
                    ),
                    validator: (value) {
                      final text = value?.trim() ?? '';
                      if (text.isEmpty) return 'Vui lòng nhập tên vắc xin';
                      if (text.length > 200) return 'Tên vắc xin tối đa 200 ký tự';
                      return null;
                    },
                  ),
                  const SizedBox(height: 16),
                  const Text(
                    'MŨI TIÊM *',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                      color: _onSurfaceVariant,
                      letterSpacing: 0.5,
                    ),
                  ),
                  const SizedBox(height: 8),
                  TextFormField(
                    controller: _doseController,
                    enabled: !_saving,
                    keyboardType: TextInputType.number,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 15,
                      color: _onSurface,
                    ),
                    decoration: _buildInputDecoration(
                      hint: 'Nhập số mũi tiêm (Ví dụ: 1, 2, 3...)',
                      prefixIcon: Icons.format_list_numbered_rounded,
                    ),
                    validator: (value) {
                      final dose = int.tryParse(value?.trim() ?? '');
                      if (dose == null || dose < 1 || dose > 32767) {
                        return 'Mũi tiêm phải là số nguyên từ 1 đến 32767';
                      }
                      return null;
                    },
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // Card 2: Thời gian & Địa điểm tiêm
            _buildCard(
              title: 'Thời gian & Địa điểm',
              subtitle: 'Ngày thực hiện và nơi tiêm vắc xin',
              icon: Icons.event_available_rounded,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Text(
                    'NGÀY ĐÃ TIÊM *',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                      color: _onSurfaceVariant,
                      letterSpacing: 0.5,
                    ),
                  ),
                  const SizedBox(height: 8),
                  InkWell(
                    onTap: _saving ? null : _pickDate,
                    borderRadius: BorderRadius.circular(16),
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 16,
                        vertical: 14,
                      ),
                      decoration: BoxDecoration(
                        color: _inputFill,
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(color: _inputBorder),
                      ),
                      child: Row(
                        children: [
                          const Icon(
                            Icons.calendar_today_rounded,
                            size: 20,
                            color: _primary,
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Text(
                              _administeredDate == null
                                  ? (_isEditing
                                      ? 'Giữ nguyên ngày hiện tại'
                                      : 'Chưa chọn ngày')
                                  : _formatDate(_administeredDate!),
                              style: TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 15,
                                fontWeight: FontWeight.w600,
                                color: _administeredDate == null
                                    ? _hintColor
                                    : _onSurface,
                              ),
                            ),
                          ),
                          const Icon(
                            Icons.arrow_forward_ios_rounded,
                            size: 14,
                            color: _hintColor,
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  const Text(
                    'CƠ SỞ TIÊM CHỦNG',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                      color: _onSurfaceVariant,
                      letterSpacing: 0.5,
                    ),
                  ),
                  const SizedBox(height: 8),
                  TextFormField(
                    controller: _facilityController,
                    enabled: !_saving,
                    maxLength: 200,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 15,
                      color: _onSurface,
                    ),
                    decoration: _buildInputDecoration(
                      hint: 'Ví dụ: VNVC, Trạm Y tế, BV Nhi Đồng...',
                      prefixIcon: Icons.local_hospital_outlined,
                    ),
                    validator: (value) => (value?.length ?? 0) > 200
                        ? 'Cơ sở tiêm chủng tối đa 200 ký tự'
                        : null,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 28),

            // Nút Lưu
            ElevatedButton(
              onPressed: _saving ? null : () => _save(offerReminder: true),
              style: ElevatedButton.styleFrom(
                backgroundColor: _primary,
                foregroundColor: Colors.white,
                disabledBackgroundColor: _primary.withValues(alpha: 0.6),
                elevation: 2,
                shadowColor: const Color(0x33845143),
                minimumSize: const Size.fromHeight(54),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                ),
              ),
              child: _saving
                  ? const SizedBox(
                      width: 22,
                      height: 22,
                      child: CircularProgressIndicator(
                        strokeWidth: 2.2,
                        color: Colors.white,
                      ),
                    )
                  : Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Icon(Icons.check_circle_outline, size: 20),
                        const SizedBox(width: 8),
                        Text(
                          _isEditing ? 'Lưu thay đổi' : 'Lưu hồ sơ tiêm',
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ],
                    ),
            ),
            if (!_isEditing) ...[
              const SizedBox(height: 12),
              const Text(
                'Sau khi lưu, bạn có thể tạo nhắc tiêm riêng với ngày và giờ cụ thể.',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 12,
                  color: _onSurfaceVariant,
                ),
              ),
            ],
          ],
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
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: const Color(0xFFF0EAE6)),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0A000000),
            blurRadius: 16,
            offset: Offset(0, 4),
          ),
        ],
      ),
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: const Color(0xFFF7F1EE),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(icon, color: _primary, size: 22),
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
                        fontWeight: FontWeight.bold,
                        color: _onSurface,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      subtitle,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        color: _onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 18),
          child,
        ],
      ),
    );
  }

  InputDecoration _buildInputDecoration({
    required String hint,
    required IconData prefixIcon,
  }) {
    return InputDecoration(
      hintText: hint,
      hintStyle: const TextStyle(
        fontFamily: 'Lexend',
        fontSize: 14,
        color: _hintColor,
      ),
      prefixIcon: Icon(prefixIcon, color: _primary, size: 20),
      filled: true,
      fillColor: _inputFill,
      contentPadding: const EdgeInsets.symmetric(
        horizontal: 16,
        vertical: 14,
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: const BorderSide(color: _inputBorder),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: const BorderSide(
          color: _primaryContainer,
          width: 1.5,
        ),
      ),
      errorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: const BorderSide(color: _error),
      ),
      focusedErrorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: const BorderSide(color: _error, width: 1.5),
      ),
      counterStyle: const TextStyle(
        fontFamily: 'Lexend',
        fontSize: 12,
        color: _onSurfaceVariant,
      ),
    );
  }

  static DateTime _dateOnly(DateTime value) {
    final local = value.toLocal();
    return DateTime(local.year, local.month, local.day);
  }

  static DateTime _clampDate(
    DateTime value,
    DateTime firstDate,
    DateTime lastDate,
  ) {
    if (value.isBefore(firstDate)) return firstDate;
    if (value.isAfter(lastDate)) return lastDate;
    return value;
  }

  static String _formatDate(DateTime value) {
    final local = value.toLocal();
    return '${local.day.toString().padLeft(2, '0')}/'
        '${local.month.toString().padLeft(2, '0')}/${local.year}';
  }
}
