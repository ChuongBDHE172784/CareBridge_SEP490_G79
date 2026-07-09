import 'package:flutter/material.dart';

import '../models/emergency_contact_model.dart';
import '../services/emergency_contact_service.dart';

class EditEmergencyContactScreen extends StatefulWidget {
  const EditEmergencyContactScreen({super.key, this.contact});

  final EmergencyContact? contact;

  @override
  State<EditEmergencyContactScreen> createState() =>
      _EditEmergencyContactScreenState();
}

class _EditEmergencyContactScreenState
    extends State<EditEmergencyContactScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryFixed = Color(0xFFFFDBD1);
  static const _secondaryContainer = Color(0xFFF6DACF);
  static const _surface = Color(0xFFFFF8F6);
  static const _surfaceContainerLowest = Color(0xFFFFFFFF);
  static const _surfaceContainerLow = Color(0xFFFFF1EC);
  static const _surfaceVariant = Color(0xFFFADCD3);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _error = Color(0xFFBA1A1A);

  final _formKey = GlobalKey<FormState>();
  final _service = EmergencyContactService();
  late final TextEditingController _nameController;
  late final TextEditingController _phoneController;
  late final TextEditingController _emailController;
  String _relationship = 'Vợ/Chồng';
  bool _primaryContact = true;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    final contact = widget.contact;
    _nameController = TextEditingController(text: contact?.name ?? '');
    _phoneController = TextEditingController(text: contact?.phone ?? '');
    _emailController = TextEditingController();
    _relationship = contact?.relationship?.isNotEmpty == true
        ? contact!.relationship!
        : 'Vợ/Chồng';
    _primaryContact = contact?.primaryContact ?? true;
  }

  @override
  void dispose() {
    _nameController.dispose();
    _phoneController.dispose();
    _emailController.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _saving = true);
    try {
      await _service.saveContact(
        EmergencyContact(
          id: widget.contact?.id,
          name: _nameController.text.trim(),
          phone: _phoneController.text.trim(),
          relationship: _relationship,
          primaryContact: _primaryContact,
        ),
      );
      if (mounted) Navigator.of(context).pop(true);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Không thể lưu liên hệ: $e'),
            backgroundColor: _error,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  void _deletePlaceholder() {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Backend chưa hỗ trợ xóa liên hệ khẩn cấp')),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _surface,
      body: SafeArea(
        child: Column(
          children: [
            _buildTopBar(),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(24, 20, 24, 24),
                child: Form(
                  key: _formKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      _buildIntro(),
                      const SizedBox(height: 28),
                      _buildFormCard(),
                      const SizedBox(height: 28),
                      _buildActions(),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTopBar() {
    return SizedBox(
      height: 56,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24),
        child: Row(
          children: [
            IconButton(
              icon: const Icon(Icons.arrow_back, color: _primary),
              onPressed: () => Navigator.of(context).pop(false),
            ),
            const SizedBox(width: 8),
            const Expanded(
              child: Text(
                'Liên hệ khẩn cấp',
                style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.w700,
                  color: _primary,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildIntro() {
    return Column(
      children: [
        Container(
          width: 88,
          height: 88,
          decoration: const BoxDecoration(
            color: _primaryFixed,
            shape: BoxShape.circle,
          ),
          child: const Icon(
            Icons.contact_emergency_outlined,
            color: _onSurface,
            size: 44,
          ),
        ),
        const SizedBox(height: 24),
        const Text(
          'Thông tin này sẽ được sử dụng trong trường hợp khẩn cấp khi không thể liên lạc với bạn.',
          textAlign: TextAlign.center,
          style: TextStyle(
            fontSize: 18,
            height: 1.45,
            color: _onSurfaceVariant,
          ),
        ),
      ],
    );
  }

  Widget _buildFormCard() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surfaceContainerLowest,
        borderRadius: BorderRadius.circular(32),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0F5A463F),
            blurRadius: 20,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _TextInput(
            label: 'Họ và tên',
            controller: _nameController,
            decoration: _inputDecoration(),
            validator: (value) => value == null || value.trim().isEmpty
                ? 'Vui lòng nhập họ tên'
                : null,
          ),
          const SizedBox(height: 20),
          const Text(
            'Mối quan hệ',
            style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w600,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 8),
          DropdownButtonFormField<String>(
            initialValue: _relationship,
            decoration: _inputDecoration(),
            borderRadius: BorderRadius.circular(20),
            items:
                const ['Vợ/Chồng', 'Cha', 'Mẹ', 'Anh/Chị/Em', 'Bạn bè', 'Khác']
                    .map(
                      (value) =>
                          DropdownMenuItem(value: value, child: Text(value)),
                    )
                    .toList(),
            onChanged: (value) =>
                setState(() => _relationship = value ?? _relationship),
          ),
          const SizedBox(height: 20),
          const Text(
            'Số điện thoại',
            style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w600,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              Expanded(
                child: TextFormField(
                  controller: _phoneController,
                  keyboardType: TextInputType.phone,
                  decoration: _inputDecoration(),
                  validator: (value) => value == null || value.trim().length < 8
                      ? 'Số điện thoại chưa hợp lệ'
                      : null,
                ),
              ),
              const SizedBox(width: 12),
              SizedBox(
                height: 56,
                child: ElevatedButton(
                  onPressed: () {},
                  style: ElevatedButton.styleFrom(
                    elevation: 0,
                    backgroundColor: _secondaryContainer,
                    foregroundColor: _primary,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(22),
                    ),
                  ),
                  child: const Text(
                    'Xác\nthực',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontWeight: FontWeight.w700),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          const Row(
            children: [
              Icon(Icons.verified_outlined, color: _primary, size: 16),
              SizedBox(width: 6),
              Text(
                'Số điện thoại đã xác thực',
                style: TextStyle(fontSize: 12, color: _primary),
              ),
            ],
          ),
          const SizedBox(height: 20),
          _TextInput(
            label: 'Email (Không bắt buộc)',
            controller: _emailController,
            decoration: _inputDecoration(),
            keyboardType: TextInputType.emailAddress,
          ),
          const SizedBox(height: 20),
          const Divider(color: _surfaceVariant),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            value: _primaryContact,
            activeThumbColor: Colors.white,
            activeTrackColor: _primary,
            onChanged: (value) => setState(() => _primaryContact = value),
            secondary: Container(
              width: 48,
              height: 48,
              decoration: BoxDecoration(
                color: _primary.withValues(alpha: 0.12),
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.star_border, color: _primary),
            ),
            title: const Text(
              'Liên hệ ưu tiên',
              style: TextStyle(fontWeight: FontWeight.w600, color: _onSurface),
            ),
            subtitle: const Text('Hiển thị đầu tiên khi khẩn cấp'),
          ),
        ],
      ),
    );
  }

  Widget _buildActions() {
    return Column(
      children: [
        SizedBox(
          width: double.infinity,
          height: 56,
          child: ElevatedButton.icon(
            onPressed: _saving ? null : _save,
            style: ElevatedButton.styleFrom(
              backgroundColor: _primary,
              foregroundColor: Colors.white,
              shape: const StadiumBorder(),
              elevation: 6,
            ),
            icon: _saving
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(
                      color: Colors.white,
                      strokeWidth: 2,
                    ),
                  )
                : const Icon(Icons.save_outlined),
            label: const Text(
              'Lưu thay đổi',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
            ),
          ),
        ),
        const SizedBox(height: 16),
        SizedBox(
          width: double.infinity,
          height: 56,
          child: OutlinedButton.icon(
            onPressed: _deletePlaceholder,
            style: OutlinedButton.styleFrom(
              foregroundColor: _error,
              side: BorderSide(color: _error.withValues(alpha: 0.15)),
              shape: const StadiumBorder(),
              backgroundColor: _surfaceContainerLow,
            ),
            icon: const Icon(Icons.delete_outline),
            label: const Text(
              'Xóa liên hệ',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
            ),
          ),
        ),
      ],
    );
  }

  InputDecoration _inputDecoration() {
    return InputDecoration(
      filled: true,
      fillColor: _surfaceContainerLow,
      contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 18),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(24),
        borderSide: const BorderSide(color: Color(0x33A4847C)),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(24),
        borderSide: const BorderSide(color: _primary),
      ),
      errorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(24),
        borderSide: const BorderSide(color: _error),
      ),
      focusedErrorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(24),
        borderSide: const BorderSide(color: _error),
      ),
    );
  }
}

class _TextInput extends StatelessWidget {
  const _TextInput({
    required this.label,
    required this.controller,
    required this.decoration,
    this.keyboardType,
    this.validator,
  });

  final String label;
  final TextEditingController controller;
  final InputDecoration decoration;
  final TextInputType? keyboardType;
  final String? Function(String?)? validator;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: const TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.w600,
            color: Color(0xFF271812),
          ),
        ),
        const SizedBox(height: 8),
        TextFormField(
          controller: controller,
          keyboardType: keyboardType,
          validator: validator,
          decoration: decoration,
        ),
      ],
    );
  }
}
