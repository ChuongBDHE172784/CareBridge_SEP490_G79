import 'dart:async';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

import '../../../core/network/api_client.dart';
import '../../../shared/components/app_user_avatar.dart';
import '../../auth/models/auth_model.dart';
import '../../auth/services/auth_service.dart';

class ExpertProfilePageScreen extends StatefulWidget {
  const ExpertProfilePageScreen({super.key});

  @override
  State<ExpertProfilePageScreen> createState() => _ExpertProfilePageScreenState();
}

class _ExpertProfilePageScreenState extends State<ExpertProfilePageScreen> {
  static const _bgColor = Color(0xFFFFF8F6);
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surface = Colors.white;
  static const _surfaceLow = Color(0xFFFFF1EC);
  static const _onSurface = Color(0xFF271812);
  static const _outline = Color(0xFF84736F);
  static const _outlineVariant = Color(0xFFD6C2BD);
  static const _error = Color(0xFFBA1A1A);
  static const _errorContainer = Color(0xFFFFDAD6);

  static const _titles = [
    'Bác sĩ',
    'Thạc sĩ - Bác sĩ',
    'Tiến sĩ - Bác sĩ',
    'BS.CKI',
    'BS.CKII',
    'PGS.TS.BS',
    'GS.TS.BS',
    'Chuyên gia Tâm lý',
    'Chuyên gia Dinh dưỡng',
    'Điều dưỡng',
    'Kỹ thuật viên',
    'Chuyên gia Y tế khác',
  ];

  Map<String, dynamic>? _expertProfile;
  UserProfile? _userProfile;
  bool _loading = true;
  bool _saving = false;
  bool _avatarSaving = false;
  String? _errorMsg;
  String? _successMsg;

  final _specialtyCtrl = TextEditingController();
  final _experienceCtrl = TextEditingController();
  final _workplaceCtrl = TextEditingController();
  final _consultationScopeCtrl = TextEditingController();
  String? _selectedTitle;

  // TrackAsia hospital search
  List<Map<String, dynamic>> _hospitalResults = [];
  bool _searchingHospitals = false;
  Timer? _searchDebounce;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  @override
  void dispose() {
    _specialtyCtrl.dispose();
    _experienceCtrl.dispose();
    _workplaceCtrl.dispose();
    _consultationScopeCtrl.dispose();
    _searchDebounce?.cancel();
    super.dispose();
  }

  Future<void> _loadData() async {
    setState(() => _loading = true);
    try {
      final user = await AuthService.instance.getProfile();
      final res = await apiGet('/api/v1/expert/profiles/me');
      final profile = res['data'] as Map<String, dynamic>? ?? {};

      if (mounted) {
        setState(() {
          _userProfile = user;
          _expertProfile = profile;
          _specialtyCtrl.text = profile['specialty'] as String? ?? '';
          _selectedTitle = profile['professionalTitle'] as String?;
          if (_selectedTitle != null && !_titles.contains(_selectedTitle)) {
            _selectedTitle = null;
          }
          final exp = profile['experienceYears'];
          _experienceCtrl.text = exp != null ? '$exp' : '';
          _workplaceCtrl.text = profile['workplace'] as String? ?? '';
          _consultationScopeCtrl.text =
              profile['consultationScope'] as String? ?? '';
          _loading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _loading = false;
          _errorMsg = 'Không thể tải hồ sơ chuyên môn';
        });
      }
    }
  }

  void _onWorkplaceChanged(String query) {
    _searchDebounce?.cancel();
    if (query.trim().length < 2 || query == _expertProfile?['workplace']) {
      setState(() => _hospitalResults = []);
      return;
    }
    _searchDebounce = Timer(const Duration(milliseconds: 500), () async {
      setState(() => _searchingHospitals = true);
      try {
        final res = await apiGet(
          '/api/v1/master-data/hospitals/search/trackasia?q=${Uri.encodeComponent(query)}',
        );
        final list = (res['data'] as List? ?? [])
            .map((e) => e as Map<String, dynamic>)
            .toList();
        if (mounted) {
          setState(() {
            _hospitalResults = list;
            _searchingHospitals = false;
          });
        }
      } catch (_) {
        if (mounted) {
          setState(() {
            _hospitalResults = [];
            _searchingHospitals = false;
          });
        }
      }
    });
  }

  Future<void> _updateAvatar() async {
    final picker = ImagePicker();
    final picked = await picker.pickImage(
      source: ImageSource.gallery,
      maxWidth: 1024,
      maxHeight: 1024,
      imageQuality: 85,
    );
    if (picked == null) return;

    setState(() => _avatarSaving = true);
    try {
      final bytes = await picked.readAsBytes();
      final res = await apiMultipart(
        '/api/v1/files/upload/with-purpose',
        {
          'kind': 'IMAGE',
          'purpose': 'EXPERT_AVATAR',
          'accessMode': 'PUBLIC',
        },
        files: [
          MultipartUploadFile(
            fieldName: 'file',
            bytes: bytes,
            fileName: picked.name,
            mimeType: 'image/jpeg',
          ),
        ],
      );
      final presignedUrl =
          (res['data'] as Map<String, dynamic>)['presignedUrl'] as String;
      await apiPatch('/api/v1/users/me/profile', {'avatarUrl': presignedUrl});
      final updatedUser = await AuthService.instance.getProfile();
      if (mounted) {
        setState(() {
          _userProfile = updatedUser;
          _avatarSaving = false;
          _successMsg = 'Đã cập nhật ảnh đại diện!';
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _avatarSaving = false;
          _errorMsg = 'Lưu ảnh đại diện thất bại';
        });
      }
    }
  }

  Future<void> _saveProfile() async {
    if (_specialtyCtrl.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Vui lòng nhập chuyên khoa')),
      );
      return;
    }

    setState(() {
      _saving = true;
      _errorMsg = null;
      _successMsg = null;
    });

    try {
      final body = <String, dynamic>{
        'specialty': _specialtyCtrl.text.trim(),
        'professionalTitle': _selectedTitle ?? '',
        'workplace': _workplaceCtrl.text.trim(),
        'consultationScope': _consultationScopeCtrl.text.trim(),
      };
      if (_experienceCtrl.text.trim().isNotEmpty) {
        body['experienceYears'] = int.tryParse(_experienceCtrl.text.trim());
      }

      final res = await apiPatch('/api/v1/expert/profiles/me', body);
      if (mounted) {
        setState(() {
          _expertProfile = res['data'] as Map<String, dynamic>? ?? body;
          _saving = false;
          _successMsg = 'Đã cập nhật hồ sơ chuyên môn thành công!';
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _saving = false;
          _errorMsg = 'Lưu hồ sơ thất bại. Vui lòng thử lại.';
        });
      }
    }
  }

  Widget _buildStatusBadge(String? status) {
    late String label;
    late Color bg;
    late Color fg;

    if (status == 'APPROVED' || status == 'VERIFIED') {
      label = '✓ Đã xác minh';
      bg = const Color(0xFFE6F4EA);
      fg = const Color(0xFF137333);
    } else if (status == 'REJECTED') {
      label = 'Từ chối xác minh';
      bg = _errorContainer;
      fg = _error;
    } else {
      label = 'Đang chờ xét duyệt';
      bg = const Color(0xFFFFF3E0);
      fg = const Color(0xFFE65100);
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(
        label,
        style: TextStyle(
          fontFamily: 'Lexend',
          fontSize: 12,
          fontWeight: FontWeight.w700,
          color: fg,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final status = _expertProfile?['verificationStatus'] as String?;

    return Scaffold(
      backgroundColor: _bgColor,
      appBar: AppBar(
        backgroundColor: _bgColor,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: _primary),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: const Text(
          'Hồ sơ chuyên môn',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 20,
            fontWeight: FontWeight.w700,
            color: _primary,
          ),
        ),
        centerTitle: true,
      ),
      body: _loading
          ? const Center(
              child: CircularProgressIndicator(color: _primaryContainer),
            )
          : SafeArea(
              child: ListView(
                padding: const EdgeInsets.fromLTRB(24, 12, 24, 32),
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text(
                        'Thông tin cá nhân & tư vấn',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 14,
                          color: _outline,
                        ),
                      ),
                      _buildStatusBadge(status),
                    ],
                  ),
                  const SizedBox(height: 16),

                  if (_errorMsg != null) ...[
                    Container(
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: _errorContainer,
                        borderRadius: BorderRadius.circular(16),
                      ),
                      child: Text(
                        _errorMsg!,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          color: _error,
                          fontSize: 13,
                        ),
                      ),
                    ),
                    const SizedBox(height: 14),
                  ],

                  if (_successMsg != null) ...[
                    Container(
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: const Color(0xFFE6F4EA),
                        borderRadius: BorderRadius.circular(16),
                      ),
                      child: Text(
                        _successMsg!,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          color: Color(0xFF137333),
                          fontSize: 13,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                    const SizedBox(height: 14),
                  ],

                  // Avatar Card
                  Container(
                    padding: const EdgeInsets.all(18),
                    decoration: BoxDecoration(
                      color: _surface,
                      borderRadius: BorderRadius.circular(24),
                      boxShadow: const [
                        BoxShadow(
                          color: Color.fromRGBO(90, 70, 63, 0.06),
                          blurRadius: 20,
                          offset: Offset(0, 4),
                        ),
                      ],
                    ),
                    child: Row(
                      children: [
                        AppUserAvatar(
                          avatarUrl: _userProfile?.avatarUrl,
                          radius: 36,
                          backgroundColor: _surfaceLow,
                        ),
                        const SizedBox(width: 14),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                _userProfile?.name ?? 'Chuyên gia CareBridge',
                                style: const TextStyle(
                                  fontFamily: 'Lexend',
                                  fontSize: 18,
                                  fontWeight: FontWeight.w700,
                                  color: _onSurface,
                                ),
                              ),
                              const SizedBox(height: 3),
                              Text(
                                '${_selectedTitle ?? 'Chuyên gia'} ${_specialtyCtrl.text.isNotEmpty ? '• ${_specialtyCtrl.text}' : ''}',
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: const TextStyle(
                                  fontFamily: 'Lexend',
                                  fontSize: 13,
                                  color: _outline,
                                ),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(width: 8),
                        OutlinedButton.icon(
                          onPressed: _avatarSaving ? null : _updateAvatar,
                          icon: _avatarSaving
                              ? const SizedBox(
                                  width: 14,
                                  height: 14,
                                  child: CircularProgressIndicator(strokeWidth: 2),
                                )
                              : const Icon(Icons.camera_alt_outlined, size: 16),
                          label: Text(_avatarSaving ? 'Lưu...' : 'Đổi ảnh'),
                          style: OutlinedButton.styleFrom(
                            foregroundColor: _primary,
                            side: const BorderSide(color: _outlineVariant),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(20),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 20),

                  // Section 1: Professional Information
                  _buildSectionContainer(
                    title: 'Thông tin chuyên môn & Chức danh',
                    icon: Icons.medical_information_outlined,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        _buildLabel('Chuyên khoa', required: true),
                        TextField(
                          controller: _specialtyCtrl,
                          decoration: _inputDecoration(
                            hint: 'VD: Sản khoa, Nhi khoa, Dinh dưỡng...',
                          ),
                        ),
                        const SizedBox(height: 16),

                        _buildLabel('Chức danh chuyên môn'),
                        DropdownButtonFormField<String>(
                          value: _selectedTitle,
                          decoration: _inputDecoration(hint: '-- Chọn chức danh --'),
                          items: _titles
                              .map(
                                (t) => DropdownMenuItem(
                                  value: t,
                                  child: Text(t, style: const TextStyle(fontFamily: 'Lexend', fontSize: 14)),
                                ),
                              )
                              .toList(),
                          onChanged: (val) => setState(() => _selectedTitle = val),
                        ),
                        const SizedBox(height: 16),

                        _buildLabel('Số năm kinh nghiệm'),
                        TextField(
                          controller: _experienceCtrl,
                          keyboardType: TextInputType.number,
                          decoration: _inputDecoration(hint: 'Số năm...'),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 20),

                  // Section 2: Workplace & Hospital
                  _buildSectionContainer(
                    title: 'Nơi công tác & Cơ sở y tế',
                    icon: Icons.business_outlined,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        _buildLabel('Bệnh viện / Phòng khám'),
                        TextField(
                          controller: _workplaceCtrl,
                          onChanged: _onWorkplaceChanged,
                          decoration: _inputDecoration(
                            hint: 'Gõ tên bệnh viện (VD: Bệnh viện Từ Dũ)...',
                            suffixIcon: _searchingHospitals
                                ? const Padding(
                                    padding: EdgeInsets.all(12),
                                    child: CircularProgressIndicator(strokeWidth: 2),
                                  )
                                : null,
                          ),
                        ),
                        if (_hospitalResults.isNotEmpty)
                          Container(
                            constraints: const BoxConstraints(maxHeight: 200),
                            margin: const EdgeInsets.only(top: 6),
                            decoration: BoxDecoration(
                              color: _surface,
                              borderRadius: BorderRadius.circular(16),
                              border: Border.all(color: _outlineVariant),
                              boxShadow: const [
                                BoxShadow(
                                  color: Color.fromRGBO(90, 70, 63, 0.08),
                                  blurRadius: 16,
                                  offset: Offset(0, 4),
                                ),
                              ],
                            ),
                            child: ListView.separated(
                              shrinkWrap: true,
                              itemCount: _hospitalResults.length,
                              separatorBuilder: (_, __) => const Divider(height: 1),
                              itemBuilder: (_, index) {
                                final h = _hospitalResults[index];
                                final name = h['name'] as String? ?? '';
                                final addr = h['address'] as String? ?? '';
                                return ListTile(
                                  title: Text(
                                    name,
                                    style: const TextStyle(
                                      fontFamily: 'Lexend',
                                      fontSize: 14,
                                      fontWeight: FontWeight.w600,
                                    ),
                                  ),
                                  subtitle: Text(
                                    addr,
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: const TextStyle(fontSize: 12),
                                  ),
                                  onTap: () {
                                    setState(() {
                                      _workplaceCtrl.text = name;
                                      _hospitalResults = [];
                                    });
                                  },
                                );
                              },
                            ),
                          ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 20),

                  // Section 3: Consultation Scope
                  _buildSectionContainer(
                    title: 'Lĩnh vực & Phạm vi tư vấn',
                    icon: Icons.description_outlined,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        _buildLabel('Mô tả chi tiết lĩnh vực có thể tư vấn'),
                        TextField(
                          controller: _consultationScopeCtrl,
                          maxLines: 4,
                          decoration: _inputDecoration(
                            hint:
                                'Nhập phạm vi tư vấn sức khỏe thai kỳ, dinh dưỡng mẹ & bé, tư vấn tâm lý...',
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 28),

                  // Submit Button
                  SizedBox(
                    width: double.infinity,
                    height: 52,
                    child: ElevatedButton.icon(
                      onPressed: _saving ? null : _saveProfile,
                      icon: _saving
                          ? const SizedBox(
                              width: 18,
                              height: 18,
                              child: CircularProgressIndicator(
                                strokeWidth: 2,
                                color: Colors.white,
                              ),
                            )
                          : const Icon(Icons.save_outlined),
                      label: Text(
                        _saving ? 'Đang lưu...' : 'Lưu hồ sơ chuyên môn',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 16,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: _primary,
                        foregroundColor: Colors.white,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(26),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
    );
  }

  Widget _buildSectionContainer({
    required String title,
    required IconData icon,
    required Widget child,
  }) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(24),
        boxShadow: const [
          BoxShadow(
            color: Color.fromRGBO(90, 70, 63, 0.06),
            blurRadius: 20,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, color: _primary, size: 22),
              const SizedBox(width: 8),
              Text(
                title,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  fontWeight: FontWeight.w700,
                  color: _onSurface,
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          child,
        ],
      ),
    );
  }

  Widget _buildLabel(String text, {bool required = false}) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: RichText(
        text: TextSpan(
          text: text,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 13,
            fontWeight: FontWeight.w600,
            color: _outline,
          ),
          children: [
            if (required)
              const TextSpan(
                text: ' *',
                style: TextStyle(color: _error),
              ),
          ],
        ),
      ),
    );
  }

  InputDecoration _inputDecoration({required String hint, Widget? suffixIcon}) {
    return InputDecoration(
      hintText: hint,
      hintStyle: const TextStyle(fontFamily: 'Lexend', fontSize: 14, color: _outlineVariant),
      suffixIcon: suffixIcon,
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      filled: true,
      fillColor: _surface,
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: const BorderSide(color: _outlineVariant),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: const BorderSide(color: _primary, width: 1.5),
      ),
    );
  }
}
