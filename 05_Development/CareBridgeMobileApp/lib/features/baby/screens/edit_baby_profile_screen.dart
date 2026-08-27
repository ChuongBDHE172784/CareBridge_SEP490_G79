import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../models/baby_model.dart';
import '../services/baby_service.dart';

class EditBabyProfileScreen extends StatefulWidget {
  final String babyId;

  const EditBabyProfileScreen({super.key, required this.babyId});

  @override
  State<EditBabyProfileScreen> createState() => _EditBabyProfileScreenState();
}

class _EditBabyProfileScreenState extends State<EditBabyProfileScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surfaceContainer = Color(0xFFF2EAE4);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  final _formKey = GlobalKey<FormState>();
  final _nicknameCtrl = TextEditingController();
  final _weightCtrl = TextEditingController();
  final _lengthCtrl = TextEditingController();

  DateTime? _birthDate;
  BabyGender _gender = BabyGender.unknown;
  bool _isActive = true;

  bool _isLoading = true;
  bool _isSaving = false;
  bool _showSuccess = false;
  String? _errorMsg;

  final _service = BabyService();

  @override
  void initState() {
    super.initState();
    _loadProfile();
  }

  @override
  void dispose() {
    _nicknameCtrl.dispose();
    _weightCtrl.dispose();
    _lengthCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadProfile() async {
    try {
      final profile = await _service.getBabyProfile(widget.babyId);
      setState(() {
        _nicknameCtrl.text = profile.nickname;
        _birthDate = profile.birthDate;
        _gender = profile.gender;
        _isActive = profile.isActive;
        if (profile.birthWeightKg != null) {
          _weightCtrl.text = profile.birthWeightKg!.toStringAsFixed(2);
        }
        if (profile.birthLengthCm != null) {
          _lengthCtrl.text = profile.birthLengthCm!.toStringAsFixed(1);
        }
      });
    } catch (e) {
      setState(() => _errorMsg = 'Không thể tải thông tin bé.');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _pickBirthDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _birthDate ?? DateTime(DateTime.now().year - 1),
      firstDate: DateTime(DateTime.now().year - 10),
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
    if (picked != null) setState(() => _birthDate = picked);
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;
    if (_birthDate == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Vui lòng chọn ngày sinh.'),
          backgroundColor: _primary,
        ),
      );
      return;
    }

    setState(() => _isSaving = true);
    try {
      await _service.updateBabyProfile(
        widget.babyId,
        UpdateBabyProfileRequest(
          nickname: _nicknameCtrl.text.trim(),
          birthDate: _birthDate,
          gender: _gender == BabyGender.unknown ? null : _gender,
          birthWeightKg: double.tryParse(_weightCtrl.text.trim()),
          birthLengthCm: double.tryParse(_lengthCtrl.text.trim()),
        ),
      );
      setState(() => _showSuccess = true);
      await Future.delayed(const Duration(seconds: 2));
      if (mounted) {
        setState(() => _showSuccess = false);
        Navigator.of(context).pop(true);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Không thể lưu. Vui lòng thử lại.'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
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
        title: const Text(
          'CareBridge',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 18,
            fontWeight: FontWeight.w700,
            color: _primary,
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.close_rounded, color: _onSurfaceVariant),
            onPressed: () => Navigator.of(context).pop(),
          ),
        ],
      ),
      body: Stack(
        children: [
          if (_isLoading)
            const Center(
              child: CircularProgressIndicator(color: _primaryContainer),
            )
          else if (_errorMsg != null)
            Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(
                    Icons.error_outline_rounded,
                    color: _primaryContainer,
                    size: 48,
                  ),
                  const SizedBox(height: 12),
                  Text(
                    _errorMsg!,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 14,
                      color: _onSurfaceVariant,
                    ),
                  ),
                  const SizedBox(height: 16),
                  TextButton(
                    onPressed: _loadProfile,
                    child: const Text(
                      'Thử lại',
                      style: TextStyle(color: _primary),
                    ),
                  ),
                ],
              ),
            )
          else
            SingleChildScrollView(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 40),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    _buildFormCard(),
                    const SizedBox(height: 24),
                    _buildActionButtons(),
                  ],
                ),
              ),
            ),
          if (_showSuccess) _buildSuccessToast(),
        ],
      ),
    );
  }

  Widget _buildFormCard() {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(32),
        boxShadow: [
          BoxShadow(
            color: _primary.withAlpha(20),
            blurRadius: 20,
            offset: const Offset(0, 6),
          ),
          const BoxShadow(
            color: Color(0x0A000000),
            blurRadius: 4,
            offset: Offset(0, 1),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Text(
            'Chỉnh sửa hồ sơ bé',
            textAlign: TextAlign.center,
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 18,
              fontWeight: FontWeight.w700,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 24),
          _buildAvatarSection(),
          const SizedBox(height: 24),
          _buildNicknameField(),
          const SizedBox(height: 16),
          _buildActiveToggle(),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(child: _buildBirthDateField()),
              const SizedBox(width: 12),
              Expanded(child: _buildGenderDropdown()),
            ],
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: _buildNumberField(
                  controller: _weightCtrl,
                  label: 'Cân nặng lúc sinh (kg)',
                  hint: '3.5',
                  min: 0.5,
                  max: 10.0,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _buildNumberField(
                  controller: _lengthCtrl,
                  label: 'Chiều dài lúc sinh (cm)',
                  hint: '50',
                  min: 20.0,
                  max: 100.0,
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          _buildPrivacyNotice(),
        ],
      ),
    );
  }

  Widget _buildAvatarSection() {
    return Center(
      child: Stack(
        children: [
          Container(
            width: 128,
            height: 128,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: _surfaceContainer,
              border: Border.all(color: Colors.white, width: 4),
              boxShadow: [
                BoxShadow(
                  color: _primary.withAlpha(30),
                  blurRadius: 12,
                  offset: const Offset(0, 4),
                ),
              ],
            ),
            child: const Icon(
              Icons.child_care_rounded,
              color: _primaryContainer,
              size: 56,
            ),
          ),
          Positioned(
            bottom: 0,
            right: 0,
            child: Container(
              width: 36,
              height: 36,
              decoration: const BoxDecoration(
                color: _primary,
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.photo_camera_rounded,
                color: Colors.white,
                size: 18,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildNicknameField() {
    return TextFormField(
      controller: _nicknameCtrl,
      maxLength: 100,
      validator: (v) =>
          (v == null || v.trim().isEmpty) ? 'Vui lòng nhập biệt danh' : null,
      style: const TextStyle(
        fontFamily: 'Lexend',
        fontSize: 15,
        color: _onSurface,
      ),
      decoration: _inputDeco('Biệt danh (Nickname)'),
    );
  }

  Widget _buildActiveToggle() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFF8F4F2),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: _surfaceContainer, width: 1.5),
      ),
      child: Row(
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: _primaryContainer.withAlpha(30),
              borderRadius: BorderRadius.circular(12),
            ),
            child: const Icon(
              Icons.child_care_rounded,
              color: _primaryContainer,
              size: 22,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: const [
                Text(
                  'Đang hoạt động',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: _onSurface,
                  ),
                ),
                Text(
                  'Đặt làm hồ sơ chính',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 11,
                    color: _onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
          Switch(
            value: _isActive,
            onChanged: (v) => setState(() => _isActive = v),
            activeThumbColor: _primary,
            activeTrackColor: _primaryContainer.withAlpha(100),
          ),
        ],
      ),
    );
  }

  Widget _buildBirthDateField() {
    return GestureDetector(
      onTap: _pickBirthDate,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: _surfaceContainer, width: 2),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Ngày sinh',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 11,
                color: _onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              _birthDate != null
                  ? '${_birthDate!.day.toString().padLeft(2, '0')}/${_birthDate!.month.toString().padLeft(2, '0')}/${_birthDate!.year}'
                  : 'Chọn ngày',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                fontWeight: FontWeight.w600,
                color: _birthDate != null ? _onSurface : _onSurfaceVariant,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildGenderDropdown() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: _surfaceContainer, width: 2),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Giới tính',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 11,
              color: _onSurfaceVariant,
            ),
          ),
          DropdownButtonHideUnderline(
            child: DropdownButton<BabyGender>(
              value: _gender,
              isExpanded: true,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                color: _onSurface,
              ),
              items: const [
                DropdownMenuItem(
                  value: BabyGender.male,
                  child: Text('Bé Trai'),
                ),
                DropdownMenuItem(
                  value: BabyGender.female,
                  child: Text('Bé Gái'),
                ),
                DropdownMenuItem(
                  value: BabyGender.unknown,
                  child: Text('Khác'),
                ),
              ],
              onChanged: (v) {
                if (v != null) setState(() => _gender = v);
              },
              dropdownColor: _canvas,
              borderRadius: BorderRadius.circular(16),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildNumberField({
    required TextEditingController controller,
    required String label,
    required String hint,
    required double min,
    required double max,
  }) {
    return TextFormField(
      controller: controller,
      keyboardType: const TextInputType.numberWithOptions(decimal: true),
      inputFormatters: [FilteringTextInputFormatter.allow(RegExp(r'[0-9.]'))],
      validator: (v) {
        if (v == null || v.isEmpty) return null;
        final n = double.tryParse(v);
        if (n == null || n < min || n > max) return 'Không hợp lệ';
        return null;
      },
      style: const TextStyle(
        fontFamily: 'Lexend',
        fontSize: 14,
        color: _onSurface,
      ),
      decoration: _inputDeco(label, hint: hint),
    );
  }

  InputDecoration _inputDeco(String label, {String? hint}) {
    return InputDecoration(
      labelText: label,
      hintText: hint,
      labelStyle: const TextStyle(
        fontFamily: 'Lexend',
        fontSize: 12,
        color: _onSurfaceVariant,
      ),
      hintStyle: const TextStyle(
        fontFamily: 'Lexend',
        fontSize: 13,
        color: Color(0xFFBBA9A4),
      ),
      filled: true,
      fillColor: Colors.white,
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      counterText: '',
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
      errorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(20),
        borderSide: const BorderSide(color: Colors.red, width: 1.5),
      ),
    );
  }

  Widget _buildPrivacyNotice() {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFFF6F1EC),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: _surfaceContainer.withAlpha(120), width: 1),
      ),
      child: const Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(
            Icons.verified_user_outlined,
            color: _primaryContainer,
            size: 18,
          ),
          SizedBox(width: 10),
          Expanded(
            child: Text(
              'Thông tin hồ sơ bé được bảo mật và chỉ được chia sẻ với các thành viên trong nhóm chăm sóc của bạn.',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 11,
                color: _onSurfaceVariant,
                height: 1.5,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildActionButtons() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        ElevatedButton(
          onPressed: _isSaving ? null : _save,
          style: ElevatedButton.styleFrom(
            backgroundColor: _primary,
            foregroundColor: Colors.white,
            disabledBackgroundColor: _surfaceContainer,
            padding: const EdgeInsets.symmetric(vertical: 16),
            shape: const StadiumBorder(),
            elevation: 0,
          ),
          child: _isSaving
              ? const SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(
                    strokeWidth: 2.5,
                    color: Colors.white,
                  ),
                )
              : const Text(
                  'Lưu hồ sơ',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                  ),
                ),
        ),
        const SizedBox(height: 12),
        OutlinedButton(
          onPressed: () => Navigator.of(context).pop(),
          style: OutlinedButton.styleFrom(
            foregroundColor: _onSurfaceVariant,
            padding: const EdgeInsets.symmetric(vertical: 16),
            backgroundColor: _surfaceContainer,
            side: BorderSide.none,
            shape: const StadiumBorder(),
          ),
          child: const Text(
            'Hủy bỏ',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 16,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildSuccessToast() {
    return Positioned.fill(
      child: Container(
        color: Colors.black.withAlpha(100),
        child: Center(
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 20),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(24),
              boxShadow: [
                BoxShadow(color: Colors.black.withAlpha(40), blurRadius: 20),
              ],
            ),
            child: const Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(
                  Icons.check_circle_rounded,
                  color: Color(0xFF845143),
                  size: 48,
                ),
                SizedBox(height: 12),
                Text(
                  'Đã cập nhật hồ sơ thành công!',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                    color: Color(0xFF271812),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
