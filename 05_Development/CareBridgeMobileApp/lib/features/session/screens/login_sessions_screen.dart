import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';
import '../models/session_model.dart';
import '../services/session_service.dart';
import 'revoke_session_sheet.dart';

class LoginSessionsScreen extends StatefulWidget {
  const LoginSessionsScreen({super.key});

  @override
  State<LoginSessionsScreen> createState() => _LoginSessionsScreenState();
}

class _LoginSessionsScreenState extends State<LoginSessionsScreen> {
  static const _bgColor = Color(0xFFFFF8F6);
  static const _primaryColor = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outline = Color(0xFF84736F);
  static const _outlineVariant = Color(0xFFD6C2BD);
  static const _surfaceColor = Color(0xFFFFF8F6);
  static const _surfaceVariant = Color(0xFFFADCD3);
  static const _errorColor = Color(0xFFBA1A1A);
  static const _errorContainer = Color(0xFFFFDAD6);

  List<SessionInfo> _sessions = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadSessions();
  }

  Future<void> _loadSessions() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final sessions = await SessionService.instance.getSessions();
      if (!mounted) return;
      setState(() {
        _sessions = sessions;
        _isLoading = false;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _error = 'Lỗi tải phiên đăng nhập (${e.statusCode})';
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

  SessionInfo? get _currentSession {
    try {
      return _sessions.firstWhere((s) => s.isCurrent);
    } catch (_) {
      return null;
    }
  }

  List<SessionInfo> get _otherSessions =>
      _sessions.where((s) => !s.isCurrent).toList();

  Future<void> _showRevokeConfirmation(SessionInfo session) async {
    final revoked = await showRevokeSessionSheet(context, session);
    if (revoked == true) _loadSessions();
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
                          CircularProgressIndicator(color: _primaryContainer),
                    )
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
      height: 72,
      padding: const EdgeInsets.symmetric(horizontal: 24),
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
          Expanded(
            child: Text(
              'Phiên đăng nhập',
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 24,
                fontWeight: FontWeight.w600,
                color: _primaryColor,
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
            const Icon(Icons.error_outline, color: _errorColor, size: 48),
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
              onPressed: _loadSessions,
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

  Widget _buildContent() {
    return RefreshIndicator(
      color: _primaryContainer,
      onRefresh: _loadSessions,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
        children: [
          _buildIntroSection(),
          const SizedBox(height: 24),
          if (_currentSession != null) ...[
            _buildCurrentSessionCard(_currentSession!),
            const SizedBox(height: 16),
            Container(height: 1, color: _surfaceVariant.withValues(alpha: 0.5)),
            const SizedBox(height: 24),
          ],
          if (_otherSessions.isNotEmpty) ...[
            const Text(
              'Các thiết bị khác',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 20,
                fontWeight: FontWeight.w600,
                color: _onSurface,
              ),
            ),
            const SizedBox(height: 16),
            ..._otherSessions.map(_buildOtherSessionCard),
          ],
          if (_otherSessions.isEmpty && _currentSession != null)
            Padding(
              padding: const EdgeInsets.only(top: 24),
              child: Center(
                child: Text(
                  'Không có thiết bị nào khác đăng nhập',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    color: _onSurfaceVariant,
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildIntroSection() {
    return Column(
      children: [
        Container(
          width: 64,
          height: 64,
          decoration: BoxDecoration(
            color: _primaryContainer.withValues(alpha: 0.2),
            shape: BoxShape.circle,
          ),
          child: const Icon(Icons.security, color: _primaryColor, size: 32),
        ),
        const SizedBox(height: 12),
        const Padding(
          padding: EdgeInsets.symmetric(horizontal: 16),
          child: Text(
            'Quản lý các thiết bị đang đăng nhập vào tài khoản của bạn. '
            'Đăng xuất khỏi các thiết bị không xác định để bảo vệ tài khoản.',
            textAlign: TextAlign.center,
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              fontWeight: FontWeight.w400,
              color: _onSurfaceVariant,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildCurrentSessionCard(SessionInfo session) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surfaceColor,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: _surfaceVariant.withValues(alpha: 0.5)),
        boxShadow: const [
          BoxShadow(
            color: Color.fromRGBO(90, 70, 63, 0.06),
            blurRadius: 20,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Stack(
        children: [
          Positioned(
            left: 0,
            top: 0,
            bottom: 0,
            child: Container(
              width: 4,
              decoration: BoxDecoration(
                color: _primaryContainer,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.only(left: 8),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  width: 48,
                  height: 48,
                  margin: const EdgeInsets.only(top: 4),
                  decoration: BoxDecoration(
                    color: _primaryContainer.withValues(alpha: 0.2),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(Icons.smartphone,
                      color: _primaryColor, size: 24),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              session.displayName,
                              style: const TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 20,
                                fontWeight: FontWeight.w600,
                                color: _onSurface,
                              ),
                            ),
                          ),
                          Container(
                            padding: const EdgeInsets.symmetric(
                                horizontal: 8, vertical: 4),
                            decoration: BoxDecoration(
                              color: _primaryContainer.withValues(alpha: 0.2),
                              borderRadius: BorderRadius.circular(9999),
                            ),
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Icon(Icons.check_circle,
                                    color: _primaryColor, size: 14),
                                const SizedBox(width: 4),
                                const Text(
                                  'Đang hoạt động',
                                  style: TextStyle(
                                    fontFamily: 'Lexend',
                                    fontSize: 12,
                                    fontWeight: FontWeight.w500,
                                    letterSpacing: 0.6,
                                    color: _primaryColor,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 4),
                      if (session.location != null)
                        Row(
                          children: [
                            const Icon(Icons.location_on,
                                color: _onSurfaceVariant, size: 16),
                            const SizedBox(width: 4),
                            Text(
                              session.location!,
                              style: const TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 14,
                                fontWeight: FontWeight.w400,
                                color: _onSurfaceVariant,
                              ),
                            ),
                          ],
                        ),
                      const SizedBox(height: 4),
                      Text(
                        'Thiết bị hiện tại',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 12,
                          fontWeight: FontWeight.w500,
                          letterSpacing: 0.6,
                          color: _outline,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildOtherSessionCard(SessionInfo session) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: _surfaceColor,
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
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  width: 48,
                  height: 48,
                  decoration: const BoxDecoration(
                    color: _surfaceVariant,
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    _getDeviceIcon(session.deviceName),
                    color: _onSurfaceVariant,
                    size: 24,
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        session.displayName,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                          color: _onSurface,
                        ),
                      ),
                      const SizedBox(height: 4),
                      if (session.location != null)
                        Row(
                          children: [
                            const Icon(Icons.location_on,
                                color: _onSurfaceVariant, size: 16),
                            const SizedBox(width: 4),
                            Text(
                              session.location!,
                              style: const TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 14,
                                fontWeight: FontWeight.w400,
                                color: _onSurfaceVariant,
                              ),
                            ),
                          ],
                        ),
                      const SizedBox(height: 4),
                      Text(
                        session.lastActivityAt != null
                            ? 'Đăng nhập lần cuối: ${_formatTime(session.lastActivityAt!)}'
                            : 'Không rõ thời gian',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 12,
                          fontWeight: FontWeight.w500,
                          letterSpacing: 0.6,
                          color: _outline,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              height: 48,
              child: FilledButton.icon(
                onPressed: () => _showRevokeConfirmation(session),
                icon: const Icon(Icons.logout, size: 20),
                label: const Text(
                  'Đăng xuất',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                style: FilledButton.styleFrom(
                  backgroundColor: _errorContainer.withValues(alpha: 0.3),
                  foregroundColor: _errorColor,
                  shape: const StadiumBorder(),
                  elevation: 0,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  IconData _getDeviceIcon(String deviceName) {
    final lower = deviceName.toLowerCase();
    if (lower.contains('ipad') || lower.contains('tablet')) {
      return Icons.tablet_mac;
    }
    if (lower.contains('mac') ||
        lower.contains('desktop') ||
        lower.contains('pc') ||
        lower.contains('windows')) {
      return Icons.desktop_windows;
    }
    return Icons.smartphone;
  }

  String _formatTime(DateTime dateTime) {
    final now = DateTime.now();
    final diff = now.difference(dateTime);

    if (diff.inMinutes < 1) return 'Vừa xong';
    if (diff.inMinutes < 60) return '${diff.inMinutes} phút trước';
    if (diff.inHours < 24) return '${diff.inHours} giờ trước';
    if (diff.inDays == 1) return 'Hôm qua';
    if (diff.inDays < 7) return '${diff.inDays} ngày trước';

    return '${dateTime.day.toString().padLeft(2, '0')}/'
        '${dateTime.month.toString().padLeft(2, '0')}/'
        '${dateTime.year}';
  }
}
