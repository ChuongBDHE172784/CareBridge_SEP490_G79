import 'package:flutter/material.dart';
import '../services/vaccination_service.dart';

class AddVaccinationRecordScreen extends StatefulWidget {
  final String babyId;
  const AddVaccinationRecordScreen({super.key, required this.babyId});

  @override
  State<AddVaccinationRecordScreen> createState() =>
      _AddVaccinationRecordScreenState();
}

class _AddVaccinationRecordScreenState
    extends State<AddVaccinationRecordScreen> {
  final _formKey = GlobalKey<FormState>();
  final _vaccinationService = VaccinationService();

  bool _isLoading = false;
  String? _vaccineName;
  String? _doseNumber;
  DateTime? _vaccineDate;
  String? _facility;
  bool _remindNext = true;

  Future<void> _pickDate() async {
    final date = await showDatePicker(
      context: context,
      initialDate: _vaccineDate ?? DateTime.now(),
      firstDate: DateTime(2000),
      lastDate: DateTime(2100),
    );
    if (date != null) {
      setState(() => _vaccineDate = date);
    }
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    if (_vaccineDate == null) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('Vui lòng chọn ngày tiêm')));
      return;
    }

    _formKey.currentState!.save();
    setState(() => _isLoading = true);

    try {
      await _vaccinationService.addVaccinationRecord(widget.babyId, {
        'vaccineName': _vaccineName,
        'doseNumber': _doseNumber,
        'actualDate': _vaccineDate!.toIso8601String(),
        'facilityName': _facility,
        'remindNext': _remindNext,
      });
      if (mounted) {
        Navigator.pop(context, true); // Return true to indicate success
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Có lỗi xảy ra: $e')));
      }
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFEF8F4),
      appBar: AppBar(
        backgroundColor: const Color(0xFFFEF8F4),
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Color(0xFF845143)),
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'Thêm lịch tiêm',
          style: TextStyle(
            color: Color(0xFF845143),
            fontSize: 20,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
      body: Stack(
        children: [
          SingleChildScrollView(
            padding: const EdgeInsets.only(
              left: 16,
              right: 16,
              top: 16,
              bottom: 100,
            ),
            child: Container(
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
              padding: const EdgeInsets.all(20),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _buildLabel('Tên Vaccine', required: true),
                    const SizedBox(height: 8),
                    TextFormField(
                      decoration: _inputDecoration(
                        icon: Icons.vaccines_outlined,
                        hint: 'Vd: 6 trong 1 (Hexaxim)',
                      ),
                      validator: (val) => val == null || val.isEmpty
                          ? 'Vui lòng nhập tên vaccine'
                          : null,
                      onSaved: (val) => _vaccineName = val,
                    ),
                    const SizedBox(height: 20),

                    _buildLabel('Mũi tiêm thứ', required: true),
                    const SizedBox(height: 8),
                    DropdownButtonFormField<String>(
                      decoration: _inputDecoration(
                        icon: Icons.format_list_numbered,
                      ),
                      hint: const Text('Chọn mũi tiêm'),
                      items: const [
                        DropdownMenuItem(value: '1', child: Text('Mũi 1')),
                        DropdownMenuItem(value: '2', child: Text('Mũi 2')),
                        DropdownMenuItem(value: '3', child: Text('Mũi 3')),
                        DropdownMenuItem(value: '4', child: Text('Mũi 4')),
                        DropdownMenuItem(
                          value: 'booster',
                          child: Text('Mũi nhắc lại'),
                        ),
                      ],
                      validator: (val) =>
                          val == null ? 'Vui lòng chọn mũi tiêm' : null,
                      onChanged: (val) => setState(() => _doseNumber = val),
                    ),
                    const SizedBox(height: 20),

                    _buildLabel('Ngày tiêm', required: true),
                    const SizedBox(height: 8),
                    InkWell(
                      onTap: _pickDate,
                      child: IgnorePointer(
                        child: TextFormField(
                          key: ValueKey(_vaccineDate),
                          initialValue: _vaccineDate != null
                              ? '${_vaccineDate!.day.toString().padLeft(2, '0')}/${_vaccineDate!.month.toString().padLeft(2, '0')}/${_vaccineDate!.year}'
                              : null,
                          decoration: _inputDecoration(
                            icon: Icons.calendar_month_outlined,
                            hint: 'dd/mm/yyyy',
                          ),
                          validator: (val) => _vaccineDate == null
                              ? 'Vui lòng chọn ngày tiêm'
                              : null,
                        ),
                      ),
                    ),
                    const SizedBox(height: 20),

                    _buildLabel('Cơ sở tiêm chủng'),
                    const SizedBox(height: 8),
                    TextFormField(
                      decoration: _inputDecoration(
                        icon: Icons.local_hospital_outlined,
                        hint: 'Vd: VNVC, Trạm y tế phường...',
                      ),
                      onSaved: (val) => _facility = val,
                    ),
                    const SizedBox(height: 20),

                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text(
                          'Ảnh sổ tiêm / Giấy chứng nhận',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.w600,
                            color: Color(0xFF524F4C),
                          ),
                        ),
                        Text(
                          '(Tùy chọn)',
                          style: TextStyle(
                            fontSize: 12,
                            color: Colors.grey[600],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.symmetric(vertical: 24),
                      decoration: BoxDecoration(
                        color: const Color(0xFFF9F2EE),
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(
                          color: const Color(0xFFF2EAE4),
                          width: 2,
                          style: BorderStyle.solid,
                        ), // Dashed not supported natively without package
                      ),
                      child: const Column(
                        children: [
                          Icon(
                            Icons.add_photo_alternate_outlined,
                            color: Color(0xFFC98C7B),
                            size: 32,
                          ),
                          SizedBox(height: 8),
                          Text(
                            'Tải ảnh lên',
                            style: TextStyle(
                              color: Color(0xFF845143),
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          Text(
                            'PNG, JPG tối đa 5MB',
                            style: TextStyle(color: Colors.grey, fontSize: 12),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 24),
                    const Divider(color: Color(0xFFF2EAE4)),
                    const SizedBox(height: 12),

                    SwitchListTile(
                      contentPadding: EdgeInsets.zero,
                      activeThumbColor: const Color(0xFFC98C7B),
                      title: const Text(
                        'Nhắc lịch mũi tiếp theo',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 14,
                        ),
                      ),
                      subtitle: const Text(
                        'Hệ thống sẽ gửi thông báo trước 3 ngày',
                        style: TextStyle(fontSize: 12, color: Colors.grey),
                      ),
                      value: _remindNext,
                      onChanged: (val) => setState(() => _remindNext = val),
                    ),
                  ],
                ),
              ),
            ),
          ),

          // Sticky Bottom Action Area
          Positioned(
            bottom: 0,
            left: 0,
            right: 0,
            child: Container(
              padding: const EdgeInsets.only(
                left: 16,
                right: 16,
                top: 16,
                bottom: 32,
              ),
              decoration: BoxDecoration(
                color: const Color(0xFFFEF8F4).withValues(alpha: 0.95),
                border: const Border(top: BorderSide(color: Color(0xFFF2EAE4))),
              ),
              child: ElevatedButton.icon(
                onPressed: _isLoading ? null : _submit,
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFFC98C7B),
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(999),
                  ),
                ),
                icon: _isLoading
                    ? const SizedBox.shrink()
                    : const Icon(Icons.save),
                label: _isLoading
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(
                          color: Colors.white,
                          strokeWidth: 2,
                        ),
                      )
                    : const Text(
                        'Lưu thông tin',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildLabel(String text, {bool required = false}) {
    return RichText(
      text: TextSpan(
        text: text,
        style: const TextStyle(
          color: Color(0xFF524F4C),
          fontSize: 12,
          fontWeight: FontWeight.w600,
          fontFamily: 'Quicksand',
        ),
        children: [
          if (required)
            const TextSpan(
              text: ' *',
              style: TextStyle(color: Colors.red),
            ),
        ],
      ),
    );
  }

  InputDecoration _inputDecoration({required IconData icon, String? hint}) {
    return InputDecoration(
      hintText: hint,
      hintStyle: const TextStyle(color: Colors.grey),
      prefixIcon: Icon(icon, color: const Color(0xFF9E9A96)),
      filled: true,
      fillColor: Colors.white,
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: const BorderSide(color: Color(0xFFF2EAE4), width: 2),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: const BorderSide(color: Color(0xFFF2EAE4), width: 2),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: const BorderSide(color: Color(0xFFC98C7B), width: 2),
      ),
      contentPadding: const EdgeInsets.symmetric(vertical: 16),
    );
  }
}
