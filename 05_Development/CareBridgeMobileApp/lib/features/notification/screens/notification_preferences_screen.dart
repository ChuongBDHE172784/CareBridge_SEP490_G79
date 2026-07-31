import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';
import '../../reminder/models/appointment_notification_timing.dart';
import '../../reminder/widgets/appointment_notification_timing_editor.dart';
import '../services/notification_service.dart';
import '../models/notification_model.dart';

class NotificationPreferencesScreen extends StatefulWidget {
  const NotificationPreferencesScreen({super.key});

  @override
  State<NotificationPreferencesScreen> createState() =>
      _NotificationPreferencesScreenState();
}

class _NotificationPreferencesScreenState
    extends State<NotificationPreferencesScreen> {
  static const _canvas = Color(0xFFF6F1EC);
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surfaceLowest = Color(0xFFFFFFFF);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _surfaceVariant = Color(0xFFFADCD3);
  static const _outlineVariant = Color(0xFFD6C2BD);

  List<NotificationPreference> _prefs = [];
  List<int> _appointmentDefaults = AppointmentNotificationTiming.systemDefaults;
  bool _isLoading = true;
  String? _error;

  bool get _pushGlobal =>
      _prefs.any((p) => p.type != 'EMERGENCY' && (p.pushEnabled ?? false));
  bool get _emailGlobal =>
      _prefs.any((p) => p.type != 'EMERGENCY' && (p.emailEnabled ?? false));

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
      final result = await NotificationService.instance.getPreferences();
      if (!mounted) return;
      setState(() {
        _prefs = result.preferences;
        _appointmentDefaults = result.appointmentReminderDefaults;
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

  Future<void> _save(List<NotificationPreference> updated) async {
    setState(() => _prefs = updated);
    try {
      final result = await NotificationService.instance.updatePreferences(
        updated,
        appointmentReminderDefaults: _appointmentDefaults,
      );
      if (mounted) {
        setState(() {
          _prefs = result.preferences;
          _appointmentDefaults = result.appointmentReminderDefaults;
        });
      }
    } on ApiException {
      if (mounted) _load();
    }
  }

  void _setGlobalPush(bool value) {
    final updated = _prefs.map((p) {
      if (p.type == 'EMERGENCY') return p;
      return p.copyWith(pushEnabled: value);
    }).toList();
    _save(updated);
  }

  void _setGlobalEmail(bool value) {
    final updated = _prefs.map((p) {
      if (p.type == 'EMERGENCY') return p;
      return p.copyWith(emailEnabled: value);
    }).toList();
    _save(updated);
  }

  void _setTypeEnabled(String type, bool value) {
    final updated = _prefs.map((p) {
      if (p.type != type) return p;
      return p.copyWith(
        pushEnabled: value,
        emailEnabled: value,
        inAppEnabled: value,
      );
    }).toList();
    _save(updated);
  }

  Future<void> _saveAppointmentDefaults(List<int> values) async {
    final previous = _appointmentDefaults;
    setState(() => _appointmentDefaults = values);
    try {
      final result = await NotificationService.instance.updatePreferences(
        _prefs,
        appointmentReminderDefaults: values,
      );
      if (!mounted) return;
      setState(() {
        _prefs = result.preferences;
        _appointmentDefaults = result.appointmentReminderDefaults;
      });
    } on ApiException {
      if (!mounted) return;
      setState(() => _appointmentDefaults = previous);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Không thể lưu mốc nhắc lịch hẹn.')),
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
              icon: const Icon(Icons.arrow_back, color: _onSurface),
              padding: EdgeInsets.zero,
            ),
          ),
          const SizedBox(width: 4),
          const Text(
            'Cài đặt thông báo',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 20,
              fontWeight: FontWeight.w600,
              color: _onSurface,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBody() {
    return ListView(
      padding: const EdgeInsets.fromLTRB(24, 16, 24, 80),
      children: [
        // Section 1: Channels
        _sectionTitle('Kênh nhận thông báo'),
        const SizedBox(height: 4),
        const Text(
          'Chọn cách bạn muốn CareBridge liên hệ với bạn.',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 14,
            color: _onSurfaceVariant,
          ),
        ),
        const SizedBox(height: 16),
        _card(
          children: [
            _channelRow(
              icon: Icons.notifications_outlined,
              title: 'Thông báo đẩy (Push)',
              subtitle: 'Trực tiếp trên thiết bị',
              value: _pushGlobal,
              onChanged: _setGlobalPush,
            ),
            _divider(),
            _channelRow(
              icon: Icons.email_outlined,
              title: 'Email',
              subtitle: 'Bản tin và báo cáo chi tiết',
              value: _emailGlobal,
              onChanged: _setGlobalEmail,
            ),
            _divider(),
            _channelRow(
              icon: Icons.sms_outlined,
              title: 'Tin nhắn SMS',
              subtitle: 'Mã xác thực và cảnh báo khẩn',
              value: false,
              onChanged: null,
            ),
          ],
        ),
        const SizedBox(height: 32),

        // Section 2: Appointment notification defaults
        _sectionTitle('Mốc nhắc lịch hẹn mặc định'),
        const SizedBox(height: 4),
        const Text(
          'Áp dụng cho lịch hẹn mới. Bạn vẫn có thể thay đổi riêng từng lịch hẹn.',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 14,
            color: _onSurfaceVariant,
          ),
        ),
        const SizedBox(height: 16),
        _card(
          children: [
            AppointmentNotificationTimingEditor(
              values: _appointmentDefaults,
              onChanged: _saveAppointmentDefaults,
              title: 'Thông báo trước và sau lịch hẹn',
            ),
          ],
        ),
        const SizedBox(height: 32),

        // Section 3: Notification types
        _sectionTitle('Loại thông báo'),
        const SizedBox(height: 4),
        const Text(
          'Quản lý những gì bạn muốn nhận thông báo.',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 14,
            color: _onSurfaceVariant,
          ),
        ),
        const SizedBox(height: 16),
        _card(
          children: [
            _typeRow(
              title: 'Lịch hẹn & Nhắc nhở',
              subtitle: 'Lịch khám, tiêm phòng, lịch uống thuốc',
              type: 'REMINDER',
            ),
            _divider(),
            _typeRow(
              title: 'Cộng đồng & Tương tác',
              subtitle: 'Bình luận, lượt thích, tin nhắn mới',
              type: 'COMMUNITY_REPLY',
            ),
            _divider(),
            _typeRow(
              title: 'Khuyến mãi & Ưu đãi',
              subtitle: 'Chương trình đối tác, quà tặng',
              type: 'CONSULTATION',
            ),
            _divider(),
            _typeRowLocked(
              title: 'Hệ thống & An toàn',
              subtitle: 'Cập nhật quan trọng, bảo mật tài khoản',
            ),
          ],
        ),
      ],
    );
  }

  Widget _channelRow({
    required IconData icon,
    required String title,
    required String subtitle,
    required bool value,
    required ValueChanged<bool>? onChanged,
  }) {
    final locked = onChanged == null;
    return Row(
      children: [
        Container(
          width: 40,
          height: 40,
          decoration: const BoxDecoration(
            color: Color(0xFFFFE2D9),
            shape: BoxShape.circle,
          ),
          child: Icon(icon, color: _primaryContainer, size: 20),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: locked ? _onSurfaceVariant : _onSurface,
                ),
              ),
              const SizedBox(height: 2),
              Text(
                subtitle,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 13,
                  color: _onSurfaceVariant,
                ),
              ),
            ],
          ),
        ),
        Switch(
          value: value,
          onChanged: locked ? null : onChanged,
          activeThumbColor: _primaryContainer,
          activeTrackColor: _surfaceVariant,
          inactiveThumbColor: Colors.white,
          inactiveTrackColor: _outlineVariant,
          trackOutlineColor: WidgetStateProperty.all(Colors.transparent),
        ),
      ],
    );
  }

  Widget _typeRow({
    required String title,
    required String subtitle,
    required String type,
  }) {
    final pref = _prefs.where((p) => p.type == type).firstOrNull;
    final enabled =
        pref?.pushEnabled == true ||
        pref?.emailEnabled == true ||
        pref?.inAppEnabled == true;
    return Row(
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
              const SizedBox(height: 2),
              Text(
                subtitle,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 13,
                  color: _onSurfaceVariant,
                ),
              ),
            ],
          ),
        ),
        Switch(
          value: enabled,
          onChanged: (v) => _setTypeEnabled(type, v),
          activeThumbColor: _primaryContainer,
          activeTrackColor: _surfaceVariant,
          inactiveThumbColor: Colors.white,
          inactiveTrackColor: _outlineVariant,
          trackOutlineColor: WidgetStateProperty.all(Colors.transparent),
        ),
      ],
    );
  }

  Widget _typeRowLocked({required String title, required String subtitle}) {
    final pref = _prefs.where((p) => p.type == 'EMERGENCY').firstOrNull;
    final enabled = pref?.pushEnabled ?? true;
    return Row(
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
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
                  const SizedBox(width: 6),
                  const Icon(
                    Icons.shield_outlined,
                    color: _primaryContainer,
                    size: 14,
                  ),
                ],
              ),
              const SizedBox(height: 2),
              Text(
                subtitle,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 13,
                  color: _onSurfaceVariant,
                ),
              ),
            ],
          ),
        ),
        Switch(
          value: enabled,
          onChanged: null,
          activeThumbColor: _primaryContainer,
          activeTrackColor: const Color(0xFFE8D0CB),
          trackOutlineColor: WidgetStateProperty.all(Colors.transparent),
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
      color: const Color.fromRGBO(214, 194, 189, 0.3),
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
