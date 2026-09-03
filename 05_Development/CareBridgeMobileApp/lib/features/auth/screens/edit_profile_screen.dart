import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import '../../../core/auth/auth_state.dart';
import '../../../core/network/api_client.dart';
import '../../../shared/components/app_user_avatar.dart';
import '../../recommendation/models/recommendation_model.dart';
import '../../recommendation/models/recommendation_questionnaire.dart';
import '../../recommendation/screens/progressive_recommendation_profile_screen.dart';
import '../../recommendation/services/recommendation_service.dart';
import '../services/auth_service.dart';

class EditProfileScreen extends StatefulWidget {
  const EditProfileScreen({super.key, this.recommendationService});

  final RecommendationService? recommendationService;

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
  DateTime? _dateOfBirth;

  late final RecommendationService _recommendationService;
  RecommendationProfileResponse? _recommendationProfile;
  bool _isLoadingRecommendation = false;

  bool _isLoading = true;
  bool _isSaving = false;

  @override
  void initState() {
    super.initState();
    _recommendationService = widget.recommendationService ?? RecommendationService();
    _loadProfile();
    _loadRecommendationProfile();
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
      final rawDob = data['dateOfBirth'];
      DateTime? parsedDob;
      if (rawDob is String && rawDob.isNotEmpty) {
        try {
          parsedDob = DateTime.parse(rawDob);
        } catch (_) {}
      }
      if (!mounted) return;
      setState(() {
        _nameController.text = data['displayName'] as String? ?? '';
        _email = email;
        _phone = data['phoneNumber'] as String?;
        _phoneController.text = _phone ?? '';
        _avatarUrl = data['avatarUrl'] as String?;
        _selectedArea = data['area'] as String?;
        _dateOfBirth = parsedDob;
        _isLoading = false;
      });
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _loadRecommendationProfile() async {
    if (AuthState.instance.role != null && AuthState.instance.role != 'MOTHER') {
      return;
    }
    setState(() => _isLoadingRecommendation = true);
    try {
      final profile = await _recommendationService.getProfile();
      if (!mounted) return;
      setState(() {
        _recommendationProfile = profile;
        _isLoadingRecommendation = false;
      });
    } catch (_) {
      if (mounted) setState(() => _isLoadingRecommendation = false);
    }
  }

  Future<void> _save() async {
    if (_isSaving) return;
    setState(() => _isSaving = true);
    try {
      final body = <String, dynamic>{
        'displayName': _nameController.text.trim(),
        'phoneNumber': _phoneController.text.trim(),
        if (_dateOfBirth != null)
          'dateOfBirth':
              '${_dateOfBirth!.year.toString().padLeft(4, '0')}-${_dateOfBirth!.month.toString().padLeft(2, '0')}-${_dateOfBirth!.day.toString().padLeft(2, '0')}',
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

  Future<void> _openRecommendationQuestionnaire() async {
    await Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => const RecommendationProfileScreen(),
      ),
    );
    if (mounted) {
      _loadProfile();
    }
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
        _buildLabel('Ngày sinh'),
        const SizedBox(height: 8),
        _buildDateField(),
        const SizedBox(height: 20),
        _buildLabel('Khu vực'),
        const SizedBox(height: 8),
        _buildAreaField(),
        _buildPersonalizedProfileSection(),
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

  Widget _buildDateField() {
    final hasDob = _dateOfBirth != null;
    final formatted = hasDob
        ? '${_dateOfBirth!.day.toString().padLeft(2, '0')}/${_dateOfBirth!.month.toString().padLeft(2, '0')}/${_dateOfBirth!.year}'
        : 'Chọn ngày sinh...';
    return GestureDetector(
      onTap: _pickDateOfBirth,
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
                formatted,
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  color: hasDob ? _onSurface : _outlineVariant,
                ),
              ),
            ),
            const SizedBox(width: 8),
            const Icon(Icons.calendar_today_outlined, color: _onSurfaceVariant, size: 20),
          ],
        ),
      ),
    );
  }

  Future<void> _pickDateOfBirth() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _dateOfBirth ?? DateTime(now.year - 25, 1, 1),
      firstDate: DateTime(1940),
      lastDate: DateTime(now.year, now.month, now.day),
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            colorScheme: const ColorScheme.light(
              primary: _primaryColor,
              onPrimary: Colors.white,
              surface: Colors.white,
              onSurface: _onSurface,
            ),
          ),
          child: child!,
        );
      },
    );
    if (picked != null) {
      setState(() => _dateOfBirth = picked);
    }
  }

  Widget _buildPersonalizedProfileSection() {
    if (AuthState.instance.role != null && AuthState.instance.role != 'MOTHER') {
      return const SizedBox.shrink();
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const SizedBox(height: 32),
        Row(
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: _surfaceContainerLow,
                borderRadius: BorderRadius.circular(10),
              ),
              child: const Icon(
                Icons.auto_awesome,
                color: _primaryColor,
                size: 20,
              ),
            ),
            const SizedBox(width: 12),
            const Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Hồ sơ sức khoẻ cá nhân hoá',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 16,
                      fontWeight: FontWeight.w700,
                      color: _primaryColor,
                    ),
                  ),
                  SizedBox(height: 2),
                  Text(
                    'Dữ liệu gợi ý cẩm nang y tế & trợ lý AI Nurse',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      color: _onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
            _buildStatusBadge(),
          ],
        ),
        const SizedBox(height: 16),
        if (_isLoadingRecommendation)
          const Center(
            child: Padding(
              padding: EdgeInsets.symmetric(vertical: 24),
              child: CircularProgressIndicator(color: _primaryContainer, strokeWidth: 2.5),
            ),
          )
        else if (_recommendationProfile == null ||
            _recommendationProfile!.status == RecommendationProfileStatus.notStarted ||
            _recommendationProfile!.status == RecommendationProfileStatus.declined)
          _buildEmptyRecommendationCard()
        else
          _buildActiveRecommendationCard(),
      ],
    );
  }

  Widget _buildStatusBadge() {
    final status = _recommendationProfile?.status ?? RecommendationProfileStatus.notStarted;
    String label;
    Color bg;
    Color text;
    IconData icon;

    switch (status) {
      case RecommendationProfileStatus.active:
        label = 'Hoạt động';
        bg = const Color(0xFFE8F5E9);
        text = const Color(0xFF2E7D32);
        icon = Icons.check_circle_outline_rounded;
        break;
      case RecommendationProfileStatus.reviewRequired:
      case RecommendationProfileStatus.reconsentRequired:
        label = 'Cần cập nhật';
        bg = const Color(0xFFFFF3E0);
        text = const Color(0xFFE65100);
        icon = Icons.update_rounded;
        break;
      case RecommendationProfileStatus.declined:
        label = 'Đã từ chối';
        bg = const Color(0xFFECEFF1);
        text = const Color(0xFF546E7A);
        icon = Icons.block_flipped;
        break;
      case RecommendationProfileStatus.notStarted:
      default:
        label = 'Chưa khảo sát';
        bg = const Color(0xFFFFF1EC);
        text = _primaryColor;
        icon = Icons.info_outline_rounded;
        break;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: text.withValues(alpha: 0.2)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: text),
          const SizedBox(width: 4),
          Text(
            label,
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 12,
              fontWeight: FontWeight.w600,
              color: text,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildEmptyRecommendationCard() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surfaceContainerLowest,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: _outlineVariant),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.02),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                padding: const EdgeInsets.all(12),
                decoration: const BoxDecoration(
                  color: Color(0xFFFFF1EC),
                  shape: BoxShape.circle,
                ),
                child: const Icon(
                  Icons.health_and_safety_outlined,
                  color: _primaryColor,
                  size: 26,
                ),
              ),
              const SizedBox(width: 14),
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Cá nhân hoá trải nghiệm chăm sóc',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 15,
                        fontWeight: FontWeight.w600,
                        color: _onSurface,
                      ),
                    ),
                    SizedBox(height: 6),
                    Text(
                      'Khảo sát 9 nhóm thông tin sức khỏe giúp CareBridge chọn lọc cẩm nang y khoa phù hợp nhất với tuần thai & thể trạng của bạn.',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 13,
                        height: 1.4,
                        color: _onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 18),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton.icon(
              onPressed: _openRecommendationQuestionnaire,
              icon: const Icon(Icons.arrow_forward_rounded, size: 18),
              label: const Text(
                'Làm khảo sát cá nhân hoá ngay',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                ),
              ),
              style: ElevatedButton.styleFrom(
                backgroundColor: _primaryColor,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(14),
                ),
                elevation: 0,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildActiveRecommendationCard() {
    final profile = _recommendationProfile?.profile;
    final derived = _recommendationProfile?.derived;

    final bmiData = _extractBmi(profile);
    final repCodes = _extractCodes(profile, 'reproductiveHistory');
    final conditionCodes = _extractCodes(profile, 'underlyingConditions');
    final ls = _extractLifestyle(profile);
    final nutritionCodes = _extractCodes(profile, 'nutrition');
    final vacList = _extractVaccines(profile);
    final medCodes = _extractCodes(profile, 'currentMedications');
    final sexCodes = _extractCodes(profile, 'sexualHealth');

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surfaceContainerLowest,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: _outlineVariant),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.02),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Thể trạng & BMI
          if (bmiData != null) ...[
            _buildGroupHeader('Thể trạng & Chỉ số BMI', Icons.monitor_weight_outlined),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                if (bmiData['heightCm'] != null)
                  _buildChip('Chiều cao: ${bmiData['heightCm']} cm'),
                if (bmiData['weightKg'] != null)
                  _buildChip('Cân nặng: ${bmiData['weightKg']} kg'),
                if (derived?['bmi'] != null)
                  _buildChip(
                    'BMI: ${derived!['bmi']} (${_bmiCategoryLabel(derived['bmiCategory'] as String?)})',
                    bg: const Color(0xFFE8F5E9),
                    textColor: const Color(0xFF2E7D32),
                  ),
                if (bmiData['weightContext'] != null)
                  _buildChip(
                    RecommendationQuestionnaire.labelFor(bmiData['weightContext'].toString()),
                    bg: _surfaceContainerLow,
                  ),
              ],
            ),
            const SizedBox(height: 16),
          ],

          // Tiền sử thai kỳ
          if (repCodes.isNotEmpty) ...[
            _buildGroupHeader('Tiền sử sinh sản', Icons.child_care_outlined),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: repCodes
                  .map((code) => _buildChip(RecommendationQuestionnaire.labelFor(code)))
                  .toList(),
            ),
            const SizedBox(height: 16),
          ],

          // Bệnh nền
          if (conditionCodes.isNotEmpty) ...[
            _buildGroupHeader('Bệnh nền & Tình trạng sức khỏe', Icons.medical_services_outlined),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: conditionCodes
                  .map((code) => _buildChip(RecommendationQuestionnaire.labelFor(code)))
                  .toList(),
            ),
            const SizedBox(height: 16),
          ],

          // Lối sống
          if (ls != null) ...[
            _buildGroupHeader('Lối sống & Thói quen', Icons.spa_outlined),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                if (ls['smoking'] is Map && ls['smoking']['value'] != null)
                  _buildChip('Hút thuốc: ${RecommendationQuestionnaire.labelFor(ls['smoking']['value'].toString())}'),
                if (ls['alcohol'] is Map && ls['alcohol']['value'] != null)
                  _buildChip('Rượu bia: ${RecommendationQuestionnaire.labelFor(ls['alcohol']['value'].toString())}'),
                if (ls['physicalActivity'] is Map && ls['physicalActivity']['value'] != null)
                  _buildChip('Vận động: ${RecommendationQuestionnaire.labelFor(ls['physicalActivity']['value'].toString())}'),
                if (ls['sleep'] is Map && ls['sleep']['value'] != null)
                  _buildChip('Giấc ngủ: ${RecommendationQuestionnaire.labelFor(ls['sleep']['value'].toString())}'),
                if (ls['flags'] is List)
                  ...((ls['flags'] as List).map(
                    (f) => _buildChip(
                      RecommendationQuestionnaire.labelFor(f.toString()),
                      bg: const Color(0xFFFFF3E0),
                      textColor: const Color(0xFFE65100),
                    ),
                  )),
              ],
            ),
            const SizedBox(height: 16),
          ],

          // Dinh dưỡng & Vi chất
          if (nutritionCodes.isNotEmpty) ...[
            _buildGroupHeader('Dinh dưỡng & Vi chất', Icons.restaurant_outlined),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: nutritionCodes
                  .map((code) => _buildChip(RecommendationQuestionnaire.labelFor(code)))
                  .toList(),
            ),
            const SizedBox(height: 16),
          ],

          // Tiêm chủng
          if (vacList.isNotEmpty) ...[
            _buildGroupHeader('Tình trạng tiêm chủng', Icons.vaccines_outlined),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: vacList.map((v) {
                final name = RecommendationQuestionnaire.labelFor(v['code']?.toString() ?? '');
                final status = RecommendationQuestionnaire.labelFor(v['value']?.toString() ?? '');
                final isUpToDate = v['value'] == 'UP_TO_DATE';
                return _buildChip(
                  '$name: $status',
                  bg: isUpToDate ? const Color(0xFFE8F5E9) : const Color(0xFFFFF3E0),
                  textColor: isUpToDate ? const Color(0xFF2E7D32) : const Color(0xFFE65100),
                );
              }).toList(),
            ),
            const SizedBox(height: 16),
          ],

          // Thuốc
          if (medCodes.isNotEmpty) ...[
            _buildGroupHeader('Thuốc đang sử dụng', Icons.medication_outlined),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: medCodes
                  .map((code) => _buildChip(RecommendationQuestionnaire.labelFor(code)))
                  .toList(),
            ),
            const SizedBox(height: 16),
          ],

          // Sức khỏe tình dục
          if (sexCodes.isNotEmpty) ...[
            _buildGroupHeader('Sức khỏe tình dục', Icons.favorite_outline),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: sexCodes
                  .map((code) => _buildChip(RecommendationQuestionnaire.labelFor(code)))
                  .toList(),
            ),
            const SizedBox(height: 16),
          ],

          // Action button to update questionnaire
          const Divider(height: 24, thickness: 0.8),
          SizedBox(
            width: double.infinity,
            child: OutlinedButton.icon(
              onPressed: _openRecommendationQuestionnaire,
              icon: const Icon(Icons.edit_note_rounded, size: 20),
              label: const Text(
                'Cập nhật khảo sát cá nhân hoá',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                ),
              ),
              style: OutlinedButton.styleFrom(
                foregroundColor: _primaryColor,
                side: const BorderSide(color: _primaryColor),
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(14),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildGroupHeader(String title, IconData icon) {
    return Row(
      children: [
        Icon(icon, size: 16, color: _primaryColor),
        const SizedBox(width: 6),
        Text(
          title,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 13,
            fontWeight: FontWeight.w600,
            color: _onSurfaceVariant,
          ),
        ),
      ],
    );
  }

  Widget _buildChip(String label, {Color? bg, Color? textColor}) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
      decoration: BoxDecoration(
        color: bg ?? _surfaceContainerLow,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: _outlineVariant.withValues(alpha: 0.5)),
      ),
      child: Text(
        label,
        style: TextStyle(
          fontFamily: 'Lexend',
          fontSize: 13,
          fontWeight: FontWeight.w500,
          color: textColor ?? _onSurface,
        ),
      ),
    );
  }

  List<String> _extractCodes(Map<String, dynamic>? profile, String domain) {
    if (profile == null) return const [];
    final domainMap = profile[domain];
    if (domainMap is Map && domainMap['state'] == 'KNOWN') {
      final codes = domainMap['codes'];
      if (codes is List) {
        return codes.map((e) => e.toString()).toList();
      }
    }
    return const [];
  }

  Map<String, dynamic>? _extractBmi(Map<String, dynamic>? profile) {
    if (profile == null) return null;
    final bmi = profile['bmi'];
    if (bmi is Map && bmi['state'] == 'KNOWN') {
      return bmi.cast<String, dynamic>();
    }
    return null;
  }

  Map<String, dynamic>? _extractLifestyle(Map<String, dynamic>? profile) {
    if (profile == null) return null;
    final ls = profile['lifestyle'];
    if (ls is Map) return ls.cast<String, dynamic>();
    return null;
  }

  List<Map<String, dynamic>> _extractVaccines(Map<String, dynamic>? profile) {
    if (profile == null) return const [];
    final vac = profile['vaccination'];
    if (vac is Map && vac['answers'] is List) {
      return (vac['answers'] as List)
          .whereType<Map>()
          .map((e) => e.cast<String, dynamic>())
          .toList();
    }
    return const [];
  }

  String _bmiCategoryLabel(String? category) {
    switch (category) {
      case 'UNDERWEIGHT':
        return 'Nhẹ cân';
      case 'HEALTHY_RANGE':
        return 'Bình thường';
      case 'OVERWEIGHT':
        return 'Thừa cân';
      case 'OBESITY':
        return 'Béo phì';
      default:
        return category ?? 'Bình thường';
    }
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
