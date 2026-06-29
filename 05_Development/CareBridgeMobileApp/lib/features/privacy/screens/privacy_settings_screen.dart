import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';
import '../models/privacy_model.dart';
import '../services/privacy_service.dart';

class PrivacySettingsScreen extends StatefulWidget {
  const PrivacySettingsScreen({super.key});

  @override
  State<PrivacySettingsScreen> createState() => _PrivacySettingsScreenState();
}

class _PrivacySettingsScreenState extends State<PrivacySettingsScreen> {
  static const _bgColor = Color(0xFFFFF8F6);
  static const _primaryColor = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);
  static const _surfaceContainerLowest = Color(0xFFFFFFFF);
  static const _surfaceContainerLow = Color(0xFFFFF1EC);
  static const _surfaceVariant = Color(0xFFFADCD3);
  static const _errorColor = Color(0xFFBA1A1A);
  static const _secondaryContainer = Color(0xFFF6DACF);
  static const _onSecondaryContainer = Color(0xFF735E56);
  static const _primaryFixedDim = Color(0xFFFAB7A4);
  static const _onPrimaryContainer = Color(0xFF51271B);
  static const _primaryFixed = Color(0xFFFFDBD1);
  static const _secondary = Color(0xFF6E5A52);

  PrivacySettings? _settings;
  List<ConsentGrant> _consents = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final results = await Future.wait([
        PrivacyService.instance.getSettings(),
        PrivacyService.instance.listConsents(),
      ]);
      if (!mounted) return;
      setState(() {
        _settings = results[0] as PrivacySettings;
        _consents = (results[1] as List<ConsentGrant>)
            .where((c) => c.isActive)
            .toList();
        _isLoading = false;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _error = 'Lỗi tải cài đặt (${e.statusCode})';
        _isLoading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _error = 'Không thể kết nối. Vui lòng thử lại.';
        _isLoading = false;
      });
    }
  }

  Future<void> _updateSetting(PrivacySettings updated) async {
    setState(() => _settings = updated);
    try {
      final result = await PrivacyService.instance.updateSettings(updated);
      if (mounted) setState(() => _settings = result);
    } on ApiException {
      if (mounted) _loadData();
    }
  }

  Future<void> _revokeConsent(ConsentGrant consent) async {
    try {
      await PrivacyService.instance.revokeConsent(consent.id);
      if (mounted) {
        setState(() => _consents.removeWhere((c) => c.id == consent.id));
      }
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Không thể thu hồi quyền (${e.statusCode})')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _bgColor,
      body: SafeArea(
        child: Column(
          children: [
            _buildAppBar(),
            Expanded(
              child: _isLoading
                  ? const Center(
                      child:
                          CircularProgressIndicator(color: _primaryContainer))
                  : _error != null
                      ? _buildErrorState()
                      : _buildContent(),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAppBar() {
    return Container(
      color: _bgColor,
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
      child: Row(
        children: [
          SizedBox(
            width: 48,
            height: 48,
            child: IconButton(
              onPressed: () => Navigator.of(context).pop(),
              icon: const Icon(Icons.arrow_back, color: _primaryColor),
              padding: EdgeInsets.zero,
            ),
          ),
          const Expanded(
            child: Text(
              'Quyền riêng tư',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 20,
                fontWeight: FontWeight.w600,
                color: _onSurface,
              ),
            ),
          ),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildErrorState() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, color: _primaryColor, size: 48),
            const SizedBox(height: 16),
            Text(_error!, textAlign: TextAlign.center,
                style: const TextStyle(fontFamily: 'Lexend', fontSize: 14, color: _onSurfaceVariant)),
            const SizedBox(height: 16),
            FilledButton(
              onPressed: _loadData,
              style: FilledButton.styleFrom(
                  backgroundColor: _primaryContainer, shape: const StadiumBorder()),
              child: const Text('Thử lại',
                  style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w600, color: Colors.white)),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildContent() {
    final s = _settings!;
    return ListView(
      padding: const EdgeInsets.fromLTRB(24, 0, 24, 80),
      children: [
        const Text(
          'Quản lý cách dữ liệu của bạn và bé được hiển thị và chia sẻ.',
          style: TextStyle(fontFamily: 'Lexend', fontSize: 14, color: _onSurfaceVariant),
        ),
        const SizedBox(height: 24),
        _buildSection(
          icon: Icons.handshake_outlined,
          title: 'Chia sẻ dữ liệu & Đồng thuận',
          children: [
            _buildToggle(
              'Cho phép chia sẻ ẩn danh',
              'Dữ liệu được dùng để cải thiện dịch vụ, không chứa thông tin định danh.',
              s.analyticsConsent,
              (v) => _updateSetting(s.copyWith(analyticsConsent: v)),
            ),
            const Divider(color: _surfaceVariant, height: 1),
            const SizedBox(height: 16),
            const Text('Quyền truy cập hiện tại',
                style: TextStyle(fontFamily: 'Lexend', fontSize: 16, fontWeight: FontWeight.w600, color: _onSurface)),
            const SizedBox(height: 4),
            const Text('Những người đang xem nhật ký của bé:',
                style: TextStyle(fontFamily: 'Lexend', fontSize: 14, color: _onSurfaceVariant)),
            const SizedBox(height: 16),
            ..._consents.map(_buildConsentItem),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: FilledButton.icon(
                onPressed: () {
                  // TODO: navigate to add consent person screen
                },
                icon: const Icon(Icons.add, size: 20),
                label: const Text('Thêm người được ủy quyền',
                    style: TextStyle(fontFamily: 'Lexend', fontSize: 16, fontWeight: FontWeight.w600)),
                style: FilledButton.styleFrom(
                  backgroundColor: _primaryFixed,
                  foregroundColor: _primaryColor,
                  shape: const StadiumBorder(),
                  elevation: 0,
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 24),
        _buildSection(
          icon: Icons.visibility_outlined,
          title: 'Hiển thị cộng đồng',
          children: [
            _buildToggle(
              'Hiển thị hồ sơ trong Cộng đồng',
              'Cho phép các mẹ khác tìm thấy bạn qua bài đăng.',
              s.profileVisibility == 'PUBLIC',
              (v) => _updateSetting(s.copyWith(
                  profileVisibility: v ? 'PUBLIC' : 'PRIVATE')),
            ),
            const Divider(color: _surfaceVariant, height: 1),
            _buildToggle(
              'Ẩn tên thật của bé',
              'Sử dụng biệt danh (VD: Cún, Miu) khi bình luận.',
              s.dataExportOptOut,
              (v) => _updateSetting(s.copyWith(dataExportOptOut: v)),
            ),
          ],
        ),
        const SizedBox(height: 24),
        _buildSection(
          icon: Icons.notifications_off_outlined,
          title: 'Quyền riêng tư thông báo',
          children: [
            _buildToggle(
              'Ẩn nội dung ngoài màn hình khóa',
              'Bảo vệ thông tin cá nhân của bé khi có thông báo đến.',
              s.locationSharingEnabled,
              (v) => _updateSetting(s.copyWith(locationSharingEnabled: v)),
            ),
          ],
        ),
        const SizedBox(height: 24),
        Center(
          child: TextButton(
            onPressed: () {
              // TODO: open privacy policy
            },
            child: const Text(
              'Xem Chính sách Quyền riêng tư chi tiết',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 12,
                fontWeight: FontWeight.w500,
                color: _secondary,
                decoration: TextDecoration.underline,
              ),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildSection({
    required IconData icon,
    required String title,
    required List<Widget> children,
  }) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surfaceContainerLowest,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: _outlineVariant.withValues(alpha: 0.3)),
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
              Icon(icon, color: _primaryColor, size: 20),
              const SizedBox(width: 8),
              Expanded(
                child: Text(title,
                    style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 20,
                        fontWeight: FontWeight.w600,
                        color: _primaryColor)),
              ),
            ],
          ),
          const SizedBox(height: 16),
          ...children,
        ],
      ),
    );
  }

  Widget _buildToggle(
      String label, String description, bool value, ValueChanged<bool> onChanged) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(label,
                    style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                        color: _onSurface)),
                const SizedBox(height: 4),
                Text(description,
                    style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 14,
                        color: _onSurfaceVariant)),
              ],
            ),
          ),
          const SizedBox(width: 16),
          Switch(
            value: value,
            onChanged: onChanged,
            activeColor: Colors.white,
            activeTrackColor: _primaryContainer,
            inactiveThumbColor: Colors.white,
            inactiveTrackColor: _surfaceVariant,
          ),
        ],
      ),
    );
  }

  Widget _buildConsentItem(ConsentGrant consent) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Container(
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
          color: _surfaceContainerLow,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Row(
          children: [
            Container(
              width: 40,
              height: 40,
              decoration: BoxDecoration(
                color: consent.scope?.toUpperCase() == 'MEDICAL'
                    ? _secondaryContainer
                    : _primaryFixedDim,
                shape: BoxShape.circle,
              ),
              child: Icon(
                consent.scope?.toUpperCase() == 'MEDICAL'
                    ? Icons.medical_services_outlined
                    : Icons.elderly_woman,
                color: consent.scope?.toUpperCase() == 'MEDICAL'
                    ? _onSecondaryContainer
                    : _onPrimaryContainer,
                size: 20,
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(consent.recipient,
                      style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                          color: _onSurface)),
                  Text(consent.scopeLabel,
                      style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 12,
                          fontWeight: FontWeight.w500,
                          letterSpacing: 0.6,
                          color: _onSurfaceVariant)),
                ],
              ),
            ),
            OutlinedButton(
              onPressed: () => _revokeConsent(consent),
              style: OutlinedButton.styleFrom(
                foregroundColor: _errorColor,
                side: BorderSide(color: _errorColor.withValues(alpha: 0.3)),
                shape: const StadiumBorder(),
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              ),
              child: const Text('Thu hồi',
                  style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      fontWeight: FontWeight.w500)),
            ),
          ],
        ),
      ),
    );
  }
}
