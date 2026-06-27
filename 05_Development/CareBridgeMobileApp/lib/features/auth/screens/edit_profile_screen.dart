import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';
import '../models/auth_model.dart';
import '../services/auth_service.dart';

class EditProfileScreen extends StatefulWidget {
  const EditProfileScreen({super.key});

  @override
  State<EditProfileScreen> createState() => _EditProfileScreenState();
}

class _EditProfileScreenState extends State<EditProfileScreen> {
  static const _bgColor = Color(0xFFFFF8F6);
  static const _primaryColor = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);
  static const _surfaceContainerLow = Color(0xFFFFF1EC);
  static const _surfaceContainerLowest = Color(0xFFFFFFFF);

  final _nameController = TextEditingController();
  final _bioController = TextEditingController();
  UserProfile? _profile;
  bool _isLoading = true;
  bool _isSaving = false;

  @override
  void initState() {
    super.initState();
    _loadProfile();
  }

  Future<void> _loadProfile() async {
    try {
      final profile = await AuthService.instance.getProfile();
      if (!mounted) return;
      setState(() {
        _profile = profile;
        _nameController.text = profile.name ?? '';
        _isLoading = false;
      });
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _save() async {
    if (_isSaving) return;
    setState(() => _isSaving = true);
    try {
      await apiPut('/api/v1/auth/profile', {'name': _nameController.text.trim()});
      if (mounted) Navigator.of(context).pop();
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Lỗi cập nhật hồ sơ (${e.statusCode})')),
      );
      setState(() => _isSaving = false);
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Không thể kết nối. Vui lòng thử lại.')),
      );
      setState(() => _isSaving = false);
    }
  }

  @override
  void dispose() {
    _nameController.dispose();
    _bioController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _bgColor,
      body: SafeArea(
        child: _isLoading
            ? const Center(child: CircularProgressIndicator(color: _primaryContainer))
            : Column(
                children: [
                  _buildAppBar(),
                  Expanded(child: _buildForm()),
                ],
              ),
      ),
    );
  }

  Widget _buildAppBar() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: SizedBox(
        height: 48,
        child: Row(
          children: [
            IconButton(
              onPressed: () => Navigator.of(context).pop(),
              icon: const Icon(Icons.close, color: _onSurface),
            ),
            const Expanded(
              child: Text('Chỉnh sửa hồ sơ', textAlign: TextAlign.center,
                  style: TextStyle(fontFamily: 'Lexend', fontSize: 20, fontWeight: FontWeight.w600, color: _primaryColor)),
            ),
            TextButton(
              onPressed: _isSaving ? null : _save,
              child: _isSaving
                  ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2, color: _primaryColor))
                  : const Text('Lưu', style: TextStyle(fontFamily: 'Lexend', fontSize: 16, fontWeight: FontWeight.w700, color: _primaryColor)),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildForm() {
    return ListView(
      padding: const EdgeInsets.fromLTRB(24, 24, 24, 32),
      children: [
        Center(
          child: Stack(
            children: [
              CircleAvatar(
                radius: 56,
                backgroundColor: _surfaceContainerLow,
                backgroundImage: _profile?.avatarUrl != null ? NetworkImage(_profile!.avatarUrl!) : null,
                child: _profile?.avatarUrl == null ? const Icon(Icons.person, size: 48, color: _primaryContainer) : null,
              ),
              Positioned(
                bottom: 0, right: 0,
                child: Container(
                  width: 36, height: 36,
                  decoration: BoxDecoration(color: _primaryContainer, shape: BoxShape.circle, border: Border.all(color: Colors.white, width: 2)),
                  child: const Icon(Icons.camera_alt, size: 18, color: Colors.white),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 32),
        _buildLabel('Họ và tên'),
        const SizedBox(height: 8),
        _buildTextField(_nameController, 'Nhập họ và tên'),
        const SizedBox(height: 20),
        _buildLabel('Số điện thoại'),
        const SizedBox(height: 8),
        _buildReadOnlyField(_profile?.phone ?? '', Icons.lock_outline),
        const SizedBox(height: 20),
        _buildLabel('Khu vực'),
        const SizedBox(height: 8),
        _buildDropdownField(),
        const SizedBox(height: 20),
        _buildLabel('Giới thiệu bản thân'),
        const SizedBox(height: 8),
        _buildTextArea(_bioController, 'Chia sẻ đôi điều về bạn...'),
      ],
    );
  }

  Widget _buildLabel(String text) {
    return Text(text, style: const TextStyle(fontFamily: 'Lexend', fontSize: 14, fontWeight: FontWeight.w500, color: _onSurfaceVariant));
  }

  Widget _buildTextField(TextEditingController controller, String hint) {
    return TextField(
      controller: controller,
      style: const TextStyle(fontFamily: 'Lexend', fontSize: 16, color: _onSurface),
      decoration: InputDecoration(
        hintText: hint,
        hintStyle: TextStyle(fontFamily: 'Lexend', fontSize: 16, color: _outlineVariant),
        filled: true, fillColor: _surfaceContainerLowest,
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: BorderSide(color: _outlineVariant)),
        enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: BorderSide(color: _outlineVariant)),
        focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: BorderSide(color: _primaryContainer, width: 2)),
        contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
      ),
    );
  }

  Widget _buildReadOnlyField(String value, IconData icon) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
      decoration: BoxDecoration(
        color: _surfaceContainerLowest,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: _outlineVariant),
      ),
      child: Row(
        children: [
          Expanded(child: Text(value, style: const TextStyle(fontFamily: 'Lexend', fontSize: 16, color: _onSurfaceVariant))),
          Icon(icon, color: _outlineVariant, size: 20),
        ],
      ),
    );
  }

  Widget _buildDropdownField() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 4),
      decoration: BoxDecoration(
        color: _surfaceContainerLowest,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: _outlineVariant),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<String>(
          value: 'q1',
          isExpanded: true,
          icon: const Icon(Icons.expand_more, color: _onSurfaceVariant),
          style: const TextStyle(fontFamily: 'Lexend', fontSize: 16, color: _onSurface),
          items: const [
            DropdownMenuItem(value: 'q1', child: Text('Quận 1, TP. HCM')),
            DropdownMenuItem(value: 'q2', child: Text('Quận 2, TP. HCM')),
            DropdownMenuItem(value: 'q3', child: Text('Quận 3, TP. HCM')),
          ],
          onChanged: (_) {},
        ),
      ),
    );
  }

  Widget _buildTextArea(TextEditingController controller, String hint) {
    return TextField(
      controller: controller,
      maxLines: 5,
      style: const TextStyle(fontFamily: 'Lexend', fontSize: 16, color: _onSurface),
      decoration: InputDecoration(
        hintText: hint,
        hintStyle: TextStyle(fontFamily: 'Lexend', fontSize: 16, color: _outlineVariant),
        filled: true, fillColor: _surfaceContainerLowest,
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: BorderSide(color: _outlineVariant)),
        enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: BorderSide(color: _outlineVariant)),
        focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: BorderSide(color: _primaryContainer, width: 2)),
        contentPadding: const EdgeInsets.all(20),
      ),
    );
  }
}
