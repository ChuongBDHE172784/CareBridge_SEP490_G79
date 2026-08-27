import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import '../../../core/network/api_client.dart';
import '../../../shared/components/app_user_avatar.dart';
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
  final _phoneController = TextEditingController();
  String? _email;
  String? _phone;
  String? _avatarUrl;
  String? _selectedArea;

  bool _isLoading = true;
  bool _isSaving = false;

  @override
  void initState() {
    super.initState();
    _loadProfile();
  }

  Future<void> _loadProfile() async {
    try {
      final res = await apiGet('/api/v1/profile');
      String? email;
      try {
        email = (await AuthService.instance.getProfile()).email;
      } catch (_) {
        email = null;
      }
      final data = res['data'] as Map<String, dynamic>;
      if (!mounted) return;
      setState(() {
        _nameController.text = data['displayName'] as String? ?? '';
        _email = email;
        _phone = data['phoneNumber'] as String?;
        _phoneController.text = _phone ?? '';
        _avatarUrl = data['avatarUrl'] as String?;
        _selectedArea = data['area'] as String?;
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
      final body = <String, dynamic>{
        'displayName': _nameController.text.trim(),
        'phoneNumber': _phoneController.text.trim(),
        if (_selectedArea != null && _selectedArea!.isNotEmpty)
          'area': _selectedArea,
      };
      await apiPatch('/api/v1/profile', body);
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

  Future<void> _showAddressPicker() async {
    final result = await showModalBottomSheet<String>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (_) => const _AddressPickerSheet(),
    );
    if (result != null) setState(() => _selectedArea = result);
  }

  @override
  void dispose() {
    _nameController.dispose();
    _phoneController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _bgColor,
      body: SafeArea(
        child: _isLoading
            ? const Center(
                child: CircularProgressIndicator(color: _primaryContainer),
              )
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
              icon: const Icon(Icons.arrow_back_ios_new_rounded, color: _primaryColor, size: 20),
            ),
            const Expanded(
              child: Text(
                'Chỉnh sửa hồ sơ',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 20,
                  fontWeight: FontWeight.w600,
                  color: _primaryColor,
                ),
              ),
            ),
            TextButton(
              onPressed: _isSaving ? null : _save,
              child: _isSaving
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: _primaryColor,
                      ),
                    )
                  : const Text(
                      'Lưu',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                        color: _primaryColor,
                      ),
                    ),
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
              AppUserAvatar(
                avatarUrl: _avatarUrl,
                radius: 56,
                backgroundColor: _surfaceContainerLow,
              ),
              Positioned(
                bottom: 0,
                right: 0,
                child: Container(
                  width: 36,
                  height: 36,
                  decoration: BoxDecoration(
                    color: _primaryContainer,
                    shape: BoxShape.circle,
                    border: Border.all(color: Colors.white, width: 2),
                  ),
                  child: const Icon(
                    Icons.camera_alt,
                    size: 18,
                    color: Colors.white,
                  ),
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
        _buildLabel('Email'),
        const SizedBox(height: 8),
        _buildReadOnlyField(_email ?? '', Icons.lock_outline),
        const SizedBox(height: 20),
        _buildLabel('Số điện thoại'),
        const SizedBox(height: 8),
        _buildTextField(
          _phoneController,
          'Nhập số điện thoại',
          keyboardType: TextInputType.phone,
        ),
        const SizedBox(height: 20),
        _buildLabel('Khu vực'),
        const SizedBox(height: 8),
        _buildAreaField(),
      ],
    );
  }

  Widget _buildLabel(String text) {
    return Text(
      text,
      style: const TextStyle(
        fontFamily: 'Lexend',
        fontSize: 14,
        fontWeight: FontWeight.w500,
        color: _onSurfaceVariant,
      ),
    );
  }

  Widget _buildTextField(
    TextEditingController controller,
    String hint, {
    TextInputType keyboardType = TextInputType.text,
  }) {
    return TextField(
      controller: controller,
      keyboardType: keyboardType,
      style: const TextStyle(
        fontFamily: 'Lexend',
        fontSize: 16,
        color: _onSurface,
      ),
      decoration: InputDecoration(
        hintText: hint,
        hintStyle: TextStyle(
          fontFamily: 'Lexend',
          fontSize: 16,
          color: _outlineVariant,
        ),
        filled: true,
        fillColor: _surfaceContainerLowest,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide(color: _outlineVariant),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide(color: _outlineVariant),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: const BorderSide(color: _primaryContainer, width: 2),
        ),
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 20,
          vertical: 16,
        ),
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
          Expanded(
            child: Text(
              value.isNotEmpty ? value : 'Chưa cập nhật',
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 16,
                color: _onSurfaceVariant,
              ),
            ),
          ),
          Icon(icon, color: _outlineVariant, size: 20),
        ],
      ),
    );
  }

  Widget _buildAreaField() {
    final hasArea = _selectedArea != null && _selectedArea!.isNotEmpty;
    return GestureDetector(
      onTap: _showAddressPicker,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
        decoration: BoxDecoration(
          color: _surfaceContainerLowest,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: _outlineVariant),
        ),
        child: Row(
          children: [
            Expanded(
              child: Text(
                hasArea ? _selectedArea! : 'Chọn khu vực...',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  color: hasArea ? _onSurface : _outlineVariant,
                ),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
            ),
            const SizedBox(width: 8),
            const Icon(Icons.expand_more, color: _onSurfaceVariant, size: 20),
          ],
        ),
      ),
    );
  }
}

// ─── Address data models ───────────────────────────────────────────────────────

class _Province {
  final int code;
  final String name;
  const _Province(this.code, this.name);
}

class _District {
  final int code;
  final String name;
  const _District(this.code, this.name);
}

class _Ward {
  final int code;
  final String name;
  const _Ward(this.code, this.name);
}

// ─── Address picker bottom sheet ──────────────────────────────────────────────

class _AddressPickerSheet extends StatefulWidget {
  const _AddressPickerSheet();

  @override
  State<_AddressPickerSheet> createState() => _AddressPickerSheetState();
}

class _AddressPickerSheetState extends State<_AddressPickerSheet> {
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);
  static const _apiBase = 'https://provinces.open-api.vn/api';

  List<_Province> _provinces = [];
  List<_District> _districts = [];
  List<_Ward> _wards = [];

  _Province? _selectedProvince;
  _District? _selectedDistrict;
  _Ward? _selectedWard;

  bool _loadingProvinces = true;
  bool _loadingDistricts = false;
  bool _loadingWards = false;

  @override
  void initState() {
    super.initState();
    _loadProvinces();
  }

  Future<void> _loadProvinces() async {
    try {
      final res = await http.get(Uri.parse('$_apiBase/p/'));
      if (!mounted) return;
      final list = jsonDecode(utf8.decode(res.bodyBytes)) as List<dynamic>;
      setState(() {
        _provinces = list
            .map((e) => _Province(e['code'] as int, e['name'] as String))
            .toList();
        _loadingProvinces = false;
      });
    } catch (_) {
      if (mounted) setState(() => _loadingProvinces = false);
    }
  }

  Future<void> _loadDistricts(int provinceCode) async {
    setState(() {
      _loadingDistricts = true;
      _districts = [];
      _wards = [];
      _selectedDistrict = null;
      _selectedWard = null;
    });
    try {
      final res = await http.get(
        Uri.parse('$_apiBase/p/$provinceCode?depth=2'),
      );
      if (!mounted) return;
      final data =
          jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>;
      final list = data['districts'] as List<dynamic>;
      setState(() {
        _districts = list
            .map((e) => _District(e['code'] as int, e['name'] as String))
            .toList();
        _loadingDistricts = false;
      });
    } catch (_) {
      if (mounted) setState(() => _loadingDistricts = false);
    }
  }

  Future<void> _loadWards(int districtCode) async {
    setState(() {
      _loadingWards = true;
      _wards = [];
      _selectedWard = null;
    });
    try {
      final res = await http.get(
        Uri.parse('$_apiBase/d/$districtCode?depth=2'),
      );
      if (!mounted) return;
      final data =
          jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>;
      final list = data['wards'] as List<dynamic>;
      setState(() {
        _wards = list
            .map((e) => _Ward(e['code'] as int, e['name'] as String))
            .toList();
        _loadingWards = false;
      });
    } catch (_) {
      if (mounted) setState(() => _loadingWards = false);
    }
  }

  String? get _result {
    if (_selectedProvince == null) return null;
    final parts = <String>[
      if (_selectedWard != null) _selectedWard!.name,
      if (_selectedDistrict != null) _selectedDistrict!.name,
      _selectedProvince!.name,
    ];
    return parts.join(', ');
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      padding: EdgeInsets.fromLTRB(
        24,
        16,
        24,
        MediaQuery.of(context).viewInsets.bottom + 32,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Center(
            child: Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: _outlineVariant,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),
          const SizedBox(height: 16),
          const Text(
            'Chọn khu vực',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 20,
              fontWeight: FontWeight.w600,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 20),

          // Province
          _buildSectionLabel('Tỉnh / Thành phố'),
          const SizedBox(height: 6),
          _loadingProvinces
              ? _buildLoader()
              : _buildDropdown<_Province>(
                  items: _provinces,
                  value: _selectedProvince,
                  labelOf: (p) => p.name,
                  hint: 'Chọn tỉnh / thành phố',
                  onChanged: (p) {
                    setState(() => _selectedProvince = p);
                    if (p != null) _loadDistricts(p.code);
                  },
                ),

          if (_selectedProvince != null) ...[
            const SizedBox(height: 16),
            _buildSectionLabel('Quận / Huyện'),
            const SizedBox(height: 6),
            _loadingDistricts
                ? _buildLoader()
                : _buildDropdown<_District>(
                    items: _districts,
                    value: _selectedDistrict,
                    labelOf: (d) => d.name,
                    hint: 'Chọn quận / huyện',
                    onChanged: (d) {
                      setState(() => _selectedDistrict = d);
                      if (d != null) _loadWards(d.code);
                    },
                  ),
          ],

          if (_selectedDistrict != null) ...[
            const SizedBox(height: 16),
            _buildSectionLabel('Phường / Xã'),
            const SizedBox(height: 6),
            _loadingWards
                ? _buildLoader()
                : _buildDropdown<_Ward>(
                    items: _wards,
                    value: _selectedWard,
                    labelOf: (w) => w.name,
                    hint: 'Chọn phường / xã',
                    onChanged: (w) => setState(() => _selectedWard = w),
                  ),
          ],

          const SizedBox(height: 28),
          SizedBox(
            width: double.infinity,
            height: 52,
            child: FilledButton(
              onPressed: _result != null
                  ? () => Navigator.pop(context, _result)
                  : null,
              style: FilledButton.styleFrom(
                backgroundColor: _primaryContainer,
                disabledBackgroundColor: _outlineVariant,
                foregroundColor: Colors.white,
                shape: const StadiumBorder(),
              ),
              child: const Text(
                'Xác nhận',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSectionLabel(String text) {
    return Text(
      text,
      style: const TextStyle(
        fontFamily: 'Lexend',
        fontSize: 13,
        fontWeight: FontWeight.w500,
        color: _onSurfaceVariant,
      ),
    );
  }

  Widget _buildLoader() {
    return const Center(
      child: Padding(
        padding: EdgeInsets.symmetric(vertical: 12),
        child: SizedBox(
          width: 22,
          height: 22,
          child: CircularProgressIndicator(
            strokeWidth: 2,
            color: _primaryContainer,
          ),
        ),
      ),
    );
  }

  Widget _buildDropdown<T>({
    required List<T> items,
    required T? value,
    required String Function(T) labelOf,
    required String hint,
    required void Function(T?) onChanged,
  }) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: _outlineVariant),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<T>(
          value: value,
          isExpanded: true,
          hint: Text(
            hint,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 15,
              color: _outlineVariant,
            ),
          ),
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 15,
            color: _onSurface,
          ),
          icon: const Icon(Icons.expand_more, color: _onSurfaceVariant),
          items: items
              .map(
                (e) => DropdownMenuItem<T>(value: e, child: Text(labelOf(e))),
              )
              .toList(),
          onChanged: onChanged,
        ),
      ),
    );
  }
}
