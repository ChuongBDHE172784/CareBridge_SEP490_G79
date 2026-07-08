import 'package:flutter/material.dart';
import '../models/baby_model.dart';
import '../services/baby_service.dart';
import 'add_baby_screen.dart';
import 'baby_profile_detail_screen.dart';

/// CB-010 — Baby Profiles (UC-31, UC-32, UC-33, UC-192, UC-193)
/// Lists all baby profiles for the current user. Active profile is highlighted.
/// Data: mock list (TODO: wire to GET /api/v1/babies when endpoint is available).
class BabyProfilesScreen extends StatefulWidget {
  const BabyProfilesScreen({super.key});

  @override
  State<BabyProfilesScreen> createState() => _BabyProfilesScreenState();
}

class _BabyProfilesScreenState extends State<BabyProfilesScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surfaceContainerLowest = Colors.white;
  static const _surfaceContainer = Color(0xFFFFE9E3);
  static const _surfaceVariant = Color(0xFFFADCD3);
  static const _secondaryContainer = Color(0xFFF6DACF);
  static const _onSecondaryContainer = Color(0xFF735E56);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _onBackground = Color(0xFF271812);

  final _service = BabyService();
  List<BabyProfile> _profiles = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadProfiles();
  }

  Future<void> _loadProfiles() async {
    setState(() { _loading = true; _error = null; });
    try {
      final list = await _service.listBabyProfiles();
      if (mounted) setState(() { _profiles = list; _loading = false; });
    } catch (e) {
      debugPrint('[BabyProfilesScreen] _loadProfiles error: $e');
      if (mounted) setState(() { _error = 'Không thể tải danh sách hồ sơ.\n$e'; _loading = false; });
    }
  }

  void _openAddBaby() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const AddBabyScreen()),
    ).then((_) => _loadProfiles());
  }

  void _openBabyDetail(BabyProfile profile) {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => BabyProfileDetailScreen(babyId: profile.id)),
    );
  }

  void _switchActiveBaby(BabyProfile profile) {
    setState(() {
      _profiles = _profiles.map((p) => BabyProfile(
        id: p.id,
        nickname: p.nickname,
        birthDate: p.birthDate,
        gender: p.gender,
        birthWeightKg: p.birthWeightKg,
        birthLengthCm: p.birthLengthCm,
        isActive: p.id == profile.id,
      )).toList();
    });
    // TODO: call PATCH /api/v1/babies/{id}/activate when endpoint is available (UC-193)
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildHeader(),
            Expanded(
              child: _loading
                  ? const Center(child: CircularProgressIndicator(color: _primaryContainer))
                  : _error != null
                      ? _buildErrorState()
                      : _buildContent(),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return SizedBox(
      height: 56,
      child: Row(
        children: [
          IconButton(
            onPressed: () => Navigator.of(context).pop(),
            icon: const Icon(Icons.arrow_back, color: _primary),
          ),
          const Expanded(
            child: Text(
              'Hồ sơ bé',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 24,
                fontWeight: FontWeight.w600,
                color: _primary,
                letterSpacing: -0.24,
              ),
            ),
          ),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildContent() {
    return RefreshIndicator(
      color: _primaryContainer,
      onRefresh: _loadProfiles,
      child: SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(24, 16, 24, 24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildAddButton(),
            const SizedBox(height: 24),
            if (_profiles.isEmpty)
              _buildEmptyState()
            else ...[
              const Text(
                'Hồ sơ đang theo dõi',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 20,
                  fontWeight: FontWeight.w600,
                  color: _onBackground,
                ),
              ),
              const SizedBox(height: 16),
              ...List.generate(_profiles.length, (i) => Padding(
                padding: EdgeInsets.only(bottom: i < _profiles.length - 1 ? 16 : 0),
                child: _BabyCard(
                  profile: _profiles[i],
                  onTap: () => _openBabyDetail(_profiles[i]),
                  onSwitch: () => _switchActiveBaby(_profiles[i]),
                  onMore: () => _showMoreMenu(_profiles[i]),
                ),
              )),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildAddButton() {
    return SizedBox(
      width: double.infinity,
      height: 52,
      child: FilledButton.icon(
        onPressed: _openAddBaby,
        style: FilledButton.styleFrom(
          backgroundColor: _primaryContainer,
          foregroundColor: Colors.white,
          shape: const StadiumBorder(),
          elevation: 0,
        ),
        icon: const Icon(Icons.add, size: 20),
        label: const Text(
          'Thêm hồ sơ bé',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 16,
            fontWeight: FontWeight.w600,
          ),
        ),
      ),
    );
  }

  Widget _buildEmptyState() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: _surfaceContainerLowest,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: const Color(0xFFD6C2BD), style: BorderStyle.solid),
      ),
      child: Column(
        children: [
          Container(
            width: 64,
            height: 64,
            decoration: const BoxDecoration(
              color: _surfaceVariant,
              shape: BoxShape.circle,
            ),
            child: const Icon(Icons.child_care, size: 32, color: _primary),
          ),
          const SizedBox(height: 12),
          const Text(
            'Chưa có hồ sơ bé',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 20,
              fontWeight: FontWeight.w600,
              color: _onBackground,
            ),
          ),
          const SizedBox(height: 8),
          const Text(
            'Thêm hồ sơ để bắt đầu theo dõi sức khỏe và sự phát triển của bé yêu.',
            textAlign: TextAlign.center,
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _onSurfaceVariant,
              height: 1.5,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildErrorState() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.error_outline, size: 48, color: Color(0xFFBA1A1A)),
          const SizedBox(height: 12),
          Text(
            _error!,
            style: const TextStyle(fontFamily: 'Lexend', fontSize: 14, color: _onSurfaceVariant),
          ),
          const SizedBox(height: 16),
          TextButton(
            onPressed: _loadProfiles,
            child: const Text(
              'Thử lại',
              style: TextStyle(fontFamily: 'Lexend', color: _primary),
            ),
          ),
        ],
      ),
    );
  }

  void _showMoreMenu(BabyProfile profile) {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (_) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 40, height: 4,
              margin: const EdgeInsets.only(top: 12, bottom: 8),
              decoration: BoxDecoration(
                color: const Color(0xFFD6C2BD),
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            ListTile(
              leading: const Icon(Icons.edit_outlined, color: _primary),
              title: Text('Chỉnh sửa ${profile.nickname}',
                  style: const TextStyle(fontFamily: 'Lexend')),
              onTap: () {
                Navigator.pop(context);
                // TODO: navigate to EditBabyScreen (UC-34/35)
              },
            ),
            ListTile(
              leading: const Icon(Icons.delete_outline, color: Color(0xFFBA1A1A)),
              title: const Text('Xóa hồ sơ',
                  style: TextStyle(fontFamily: 'Lexend', color: Color(0xFFBA1A1A))),
              onTap: () {
                Navigator.pop(context);
                // TODO: call DELETE /api/v1/babies/{id} (UC-33)
              },
            ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
  }
}

// ─── Baby Card ────────────────────────────────────────────────────────────────

class _BabyCard extends StatelessWidget {
  final BabyProfile profile;
  final VoidCallback onTap;
  final VoidCallback onSwitch;
  final VoidCallback onMore;

  const _BabyCard({
    required this.profile,
    required this.onTap,
    required this.onSwitch,
    required this.onMore,
  });

  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surfaceContainer = Color(0xFFFFE9E3);
  static const _surfaceVariant = Color(0xFFFADCD3);
  static const _secondaryContainer = Color(0xFFF6DACF);
  static const _onSecondaryContainer = Color(0xFF735E56);
  static const _onBackground = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(24),
          border: profile.isActive
              ? Border.all(color: _primaryContainer.withAlpha(51), width: 2)
              : null,
          boxShadow: [
            BoxShadow(
              color: const Color(0xFF5A463F).withAlpha(15),
              blurRadius: 20,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Stack(
          children: [
            Padding(
              padding: const EdgeInsets.all(20),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  _buildAvatar(),
                  const SizedBox(width: 16),
                  Expanded(child: _buildInfo()),
                  IconButton(
                    onPressed: onMore,
                    icon: const Icon(Icons.more_vert, color: _onSurfaceVariant),
                    splashRadius: 20,
                  ),
                ],
              ),
            ),
            if (profile.isActive) _buildActiveBadge(),
          ],
        ),
      ),
    );
  }

  Widget _buildAvatar() {
    return Container(
      width: 72,
      height: 72,
      decoration: const BoxDecoration(
        color: _surfaceContainer,
        shape: BoxShape.circle,
      ),
      child: const Icon(Icons.child_care, size: 36, color: _primary),
    );
  }

  Widget _buildInfo() {
    final genderLabel = profile.gender.displayLabel;
    final subtitle = genderLabel.isNotEmpty
        ? '${profile.ageLabel} • $genderLabel'
        : profile.ageLabel;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          profile.nickname,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 20,
            fontWeight: FontWeight.w600,
            color: _onBackground,
          ),
        ),
        const SizedBox(height: 2),
        Text(
          subtitle,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 14,
            color: _onSurfaceVariant,
          ),
        ),
        const SizedBox(height: 8),
        if (profile.isActive)
          _buildVaccineBadge()
        else
          _buildSwitchButton(),
      ],
    );
  }

  Widget _buildVaccineBadge() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: _secondaryContainer,
        borderRadius: BorderRadius.circular(99),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: const [
          Icon(Icons.vaccines, size: 14, color: _onSecondaryContainer),
          SizedBox(width: 4),
          Text(
            'Đủ mũi tiêm',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 12,
              fontWeight: FontWeight.w500,
              color: _onSecondaryContainer,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSwitchButton() {
    return GestureDetector(
      onTap: onSwitch,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: BoxDecoration(
          color: _surfaceVariant,
          borderRadius: BorderRadius.circular(99),
        ),
        child: const Text(
          'Chuyển sang bé này',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 12,
            fontWeight: FontWeight.w500,
            color: _onSurfaceVariant,
          ),
        ),
      ),
    );
  }

  Widget _buildActiveBadge() {
    return Positioned(
      top: 0,
      right: 0,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
        decoration: const BoxDecoration(
          color: _primaryContainer,
          borderRadius: BorderRadius.only(
            topRight: Radius.circular(24),
            bottomLeft: Radius.circular(12),
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: const [
            Icon(Icons.check_circle, size: 14, color: Colors.white),
            SizedBox(width: 4),
            Text(
              'Đang chọn',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 12,
                fontWeight: FontWeight.w500,
                color: Colors.white,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
