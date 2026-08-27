import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../models/vaccination_model.dart';
import '../services/vaccination_service.dart';

/// Shared UC-229/UC-230 form. The form deliberately only sends fields owned
/// by the vaccination record DTO; status and reminder lifecycle stay outside
/// this screen.
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
        title: const Text('Tạo nhắc tiêm riêng?'),
        content: const Text(
          'Bạn có thể chọn ngày và giờ nhắc riêng cho mũi tiếp theo. Thao tác này không thay đổi hồ sơ tiêm.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: const Text('Để sau'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(dialogContext, true),
            child: const Text('Tạo nhắc'),
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
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: _primary),
          onPressed: _saving ? null : () => Navigator.pop(context),
        ),
        title: Text(
          _isEditing ? 'Sửa hồ sơ tiêm chủng' : 'Thêm lịch tiêm',
          style: const TextStyle(color: _primary, fontWeight: FontWeight.bold),
        ),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 12, 20, 32),
          children: [
            TextFormField(
              controller: _vaccineController,
              maxLength: 200,
              enabled: !_saving,
              decoration: const InputDecoration(
                labelText: 'Tên vaccine *',
                prefixIcon: Icon(Icons.vaccines_outlined),
              ),
              validator: (value) {
                final text = value?.trim() ?? '';
                if (text.isEmpty) return 'Vui lòng nhập tên vaccine';
                if (text.length > 200) return 'Tên vaccine tối đa 200 ký tự';
                return null;
              },
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _doseController,
              enabled: !_saving,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(
                labelText: 'Mũi tiêm *',
                prefixIcon: Icon(Icons.format_list_numbered),
              ),
              validator: (value) {
                final dose = int.tryParse(value?.trim() ?? '');
                if (dose == null || dose < 1 || dose > 32767) {
                  return 'Mũi tiêm phải là số nguyên từ 1 đến 32767';
                }
                return null;
              },
            ),
            const SizedBox(height: 12),
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading: const Icon(Icons.calendar_month_outlined),
              title: const Text('Ngày đã tiêm'),
              subtitle: Text(
                _administeredDate == null
                    ? (_isEditing ? 'Giữ nguyên ngày hiện tại' : 'Chưa chọn')
                    : _formatDate(_administeredDate!),
              ),
              onTap: _saving ? null : _pickDate,
            ),
            const SizedBox(height: 8),
            TextFormField(
              controller: _facilityController,
              enabled: !_saving,
              maxLength: 200,
              decoration: const InputDecoration(
                labelText: 'Cơ sở tiêm chủng',
                prefixIcon: Icon(Icons.local_hospital_outlined),
              ),
              validator: (value) => (value?.length ?? 0) > 200
                  ? 'Cơ sở tiêm chủng tối đa 200 ký tự'
                  : null,
            ),
            const SizedBox(height: 24),
            FilledButton.icon(
              onPressed: _saving ? null : () => _save(offerReminder: true),
              icon: _saving
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.save_outlined),
              label: Text(_isEditing ? 'Lưu thay đổi' : 'Lưu hồ sơ'),
              style: FilledButton.styleFrom(
                backgroundColor: _primaryContainer,
                minimumSize: const Size.fromHeight(52),
              ),
            ),
            if (!_isEditing) ...[
              const SizedBox(height: 10),
              const Text(
                'Sau khi lưu, bạn có thể tạo nhắc tiêm riêng với ngày và giờ cụ thể.',
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 12, color: Colors.grey),
              ),
            ],
          ],
        ),
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
