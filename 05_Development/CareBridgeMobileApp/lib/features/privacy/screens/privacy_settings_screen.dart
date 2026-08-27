import 'package:flutter/material.dart';
import '../../../core/auth/auth_state.dart';
import '../../../core/network/api_client.dart';
import '../models/privacy_model.dart';
import '../services/privacy_service.dart';
import '../../recommendation/services/recommendation_service.dart';
import '../../recommendation/screens/recommendation_profile_screen.dart';

class PrivacySettingsScreen extends StatefulWidget {
  const PrivacySettingsScreen({super.key});

  @override
  State<PrivacySettingsScreen> createState() => _PrivacySettingsScreenState();
}

class _PrivacySettingsScreenState extends State<PrivacySettingsScreen> {
  static const _canvas = Color(0xFFFFF8F6);
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _surfaceLowest = Color(0xFFFFFFFF);
  static const _errorContainer = Color(0xFFFFDAD6);
  static const _onErrorContainer = Color(0xFF93000A);

  PrivacySettings? _settings;
  List<ConsentGrant> _consents = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
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

  Future<void> _patch(PrivacySettings updated) async {
    setState(() => _settings = updated);
    try {
      final result = await PrivacyService.instance.updateSettings(updated);
      if (mounted) setState(() => _settings = result);
    } on ApiException {
      if (mounted) _load();
    }
  }

  Future<void> _revoke(ConsentGrant grant) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text(
          'Thu hồi quyền truy cập',
          style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w600),
        ),
        content: Text(
          'Bạn có chắc muốn thu hồi quyền của ${grant.recipient}?',
          style: const TextStyle(fontFamily: 'Lexend'),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Hủy'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            style: FilledButton.styleFrom(backgroundColor: _primary),
            child: const Text(
              'Thu hồi',
              style: TextStyle(color: Colors.white, fontFamily: 'Lexend'),
            ),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    final expectedUserId = AuthState.instance.userId;
    try {
      await PrivacyService.instance.revokeConsent(grant.id);
      if (!mounted || AuthState.instance.userId != expectedUserId) return;
      if (grant.dataType == 'SENSITIVE_DATA' &&
          grant.purpose == 'PERSONALIZE' &&
          grant.scope == 'MOTHER_PERSONALIZED_CONTENT') {
        if (expectedUserId != null) {
          await RecommendationService().clearDraftFor(expectedUserId);
        }
        RecommendationService.notifyProfileChanged();
      }
      if (mounted) {
        setState(() => _consents.removeWhere((c) => c.id == grant.id));
      }
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Không thể thu hồi (${e.statusCode})')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: Column(
          children: [
            _buildAppBar(),
            Expanded(
              child: _isLoading
                  ? const Center(
                      child: CircularProgressIndicator(
                        color: _primaryContainer,
                      ),
                    )
                  : _error != null
                  ? _buildError()
                  : _buildBody(),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAppBar() {
    return Container(
      height: 56,
      padding: const EdgeInsets.symmetric(horizontal: 8),
      color: _canvas,
      child: Row(
        children: [
          SizedBox(
            width: 48,
            height: 48,
            child: IconButton(
              onPressed: () => Navigator.of(context).pop(),
              icon: const Icon(Icons.arrow_back, color: _primary),
              padding: EdgeInsets.zero,
            ),
          ),
          const SizedBox(width: 8),
          const Text(
            'Quyền riêng tư',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 24,
              fontWeight: FontWeight.w600,
              color: _primary,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBody() {
    final s = _settings!;
    return ListView(
      padding: const EdgeInsets.fromLTRB(24, 8, 24, 80),
      children: [
        const Text(
          'Quản lý cách dữ liệu của bạn được sử dụng và chia sẻ trong cộng đồng CareBridge.',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 16,
            fontWeight: FontWeight.w400,
            height: 1.5,
            color: _onSurfaceVariant,
          ),
        ),
        const SizedBox(height: 24),

        // Section 1 — Data Access Consents
        _sectionTitle('Quyền truy cập dữ liệu'),
        const SizedBox(height: 12),
        _card(
          children: _consents.isEmpty
              ? [
                  const Center(
                    child: Padding(
                      padding: EdgeInsets.symmetric(vertical: 8),
                      child: Text(
                        'Chưa có quyền truy cập nào được cấp',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 14,
                          color: _onSurfaceVariant,
                        ),
                      ),
                    ),
                  ),
                ]
              : _buildConsentItems(),
        ),
        if (AuthState.instance.role == 'MOTHER' &&
            !_consents.any(
              (consent) =>
                  consent.dataType == 'SENSITIVE_DATA' &&
                  consent.purpose == 'PERSONALIZE' &&
                  consent.scope == 'MOTHER_PERSONALIZED_CONTENT',
            )) ...[
          const SizedBox(height: 12),
          OutlinedButton.icon(
            onPressed: () => Navigator.of(context).push(
              MaterialPageRoute(
                builder: (_) => const RecommendationProfileScreen(),
              ),
            ),
            icon: const Icon(Icons.auto_awesome_outlined),
            label: const Text('Mở lại thiết lập cá nhân hóa'),
          ),
        ],
        const SizedBox(height: 24),

        // Section 2 — Community Visibility
        _sectionTitle('Hiển thị trong cộng đồng'),
        const SizedBox(height: 12),
        _card(
          children: [
            _toggleRow(
              title: 'Chia sẻ ẩn danh',
              subtitle:
                  'Bài viết của bạn trong diễn đàn sẽ không hiển thị tên thật.',
              value: s.analyticsConsent,
              onChanged: (v) => _patch(s.copyWith(analyticsConsent: v)),
            ),
            _divider(),
            _toggleRow(
              title: 'Hồ sơ công khai',
              subtitle:
                  'Cho phép các cha mẹ khác tìm thấy bạn qua tên hoặc email.',
              value: s.profileVisibility == 'PUBLIC',
              onChanged: (v) => _patch(
                s.copyWith(profileVisibility: v ? 'PUBLIC' : 'PRIVATE'),
              ),
            ),
          ],
        ),
        const SizedBox(height: 24),

        // Section 3 — Notification Privacy
        _sectionTitle('Thông báo'),
        const SizedBox(height: 12),
        _card(
          children: [
            _toggleRow(
              title: 'Ẩn nội dung trên màn hình khóa',
              subtitle:
                  'Bảo vệ thông tin sức khỏe nhạy cảm khỏi ánh nhìn lướt qua.',
              value: s.dataExportOptOut,
              onChanged: (v) => _patch(s.copyWith(dataExportOptOut: v)),
            ),
          ],
        ),
      ],
    );
  }

  List<Widget> _buildConsentItems() {
    final items = <Widget>[];
    for (var i = 0; i < _consents.length; i++) {
      final c = _consents[i];
      items.add(_consentRow(c));
      if (i < _consents.length - 1) items.add(_divider());
    }
    return items;
  }

  Widget _consentRow(ConsentGrant grant) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                grant.recipient,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: _onSurface,
                ),
              ),
              const SizedBox(height: 2),
              Text(
                grant.scopeLabel,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  fontWeight: FontWeight.w400,
                  color: _onSurfaceVariant,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(width: 16),
        GestureDetector(
          onTap: () => _revoke(grant),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            decoration: BoxDecoration(
              color: _errorContainer,
              borderRadius: BorderRadius.circular(9999),
            ),
            child: const Text(
              'Thu hồi',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 12,
                fontWeight: FontWeight.w500,
                letterSpacing: 0.6,
                color: _onErrorContainer,
              ),
            ),
          ),
        ),
      ],
    );
  }

  Widget _toggleRow({
    required String title,
    required String subtitle,
    required bool value,
    required ValueChanged<bool> onChanged,
  }) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: _onSurface,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                subtitle,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 14,
                  fontWeight: FontWeight.w400,
                  height: 1.5,
                  color: _onSurfaceVariant,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(width: 16),
        Checkbox(
          value: value,
          onChanged: (v) => onChanged(v ?? false),
          activeColor: const Color(0xFF1565C0),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(4)),
        ),
      ],
    );
  }

  Widget _card({required List<Widget> children}) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surfaceLowest,
        borderRadius: BorderRadius.circular(16),
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
        children: children,
      ),
    );
  }

  Widget _sectionTitle(String text) {
    return Text(
      text,
      style: const TextStyle(
        fontFamily: 'Lexend',
        fontSize: 20,
        fontWeight: FontWeight.w600,
        color: _primary,
      ),
    );
  }

  Widget _divider() {
    return Container(
      height: 1,
      margin: const EdgeInsets.symmetric(vertical: 16),
      color: Color.fromRGBO(214, 194, 189, 0.3),
    );
  }

  Widget _buildError() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, color: _primary, size: 48),
            const SizedBox(height: 16),
            Text(
              _error!,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                color: _onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 16),
            FilledButton(
              onPressed: _load,
              style: FilledButton.styleFrom(
                backgroundColor: _primaryContainer,
                shape: const StadiumBorder(),
              ),
              child: const Text(
                'Thử lại',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontWeight: FontWeight.w600,
                  color: Colors.white,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
