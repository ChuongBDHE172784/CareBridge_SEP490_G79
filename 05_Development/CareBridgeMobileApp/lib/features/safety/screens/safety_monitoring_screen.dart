import 'package:flutter/material.dart';
import '../models/safety_config_model.dart';
import '../services/fall_detection_sensor_service.dart';
import '../services/safety_service.dart';
import '../widgets/disable_fall_detection_sheet.dart';
import 'enable_fall_detection_screen.dart';
import '../../emergency/screens/emergency_contacts_screen.dart';
import '../../emergency/services/emergency_service.dart';

/// CB-023 — Safety Monitoring (UC-133..UC-141, UC-176)
/// Hub screen for fall detection, SOS, and safety event history.
/// Pushed from the Hành trình (Journey) tab, hence the small back affordance
/// added to the otherwise back-button-less design header.
class SafetyMonitoringScreen extends StatefulWidget {
  const SafetyMonitoringScreen({super.key});

  @override
  State<SafetyMonitoringScreen> createState() => _SafetyMonitoringScreenState();
}

class _SafetyMonitoringScreenState extends State<SafetyMonitoringScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surface = Color(0xFFFFF8F6);
  static const _surfaceContainerLowest = Color(0xFFFFFFFF);
  static const _surfaceContainerHighest = Color(0xFFFADCD3);
  static const _surfaceContainer = Color(0xFFFFE9E3);
  static const _surfaceVariant = Color(0xFFFADCD3);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _errorContainer = Color(0xFFFFDAD6);
  static const _onErrorContainer = Color(0xFF93000A);
  static const _secondaryContainer = Color(0xFFF6DACF);
  static const _onSecondaryContainer = Color(0xFF735E56);
  static const _tertiary = Color(0xFF625D59);

  final _safetyService = SafetyService();
  final _emergencyService = EmergencyService();
  final _fallSensorService = FallDetectionSensorService.instance;
  SafetyConfig? _config;
  List<SafetyEvent> _events = const [];
  bool _loading = true;
  // Local sensor-stream toggle backed by sensors_plus accelerometer/gyroscope
  // streams; backend fall detection remains controlled by SafetyConfig.
  bool _imuSensorActive = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final config = await _safetyService.getConfig();
      final events = await _safetyService.getSafetyEvents();
      if (config.fallDetectionEnabled) {
        await _fallSensorService.start();
      } else {
        await _fallSensorService.stop();
      }
      if (mounted) {
        setState(() {
          _config = config;
          _events = events;
          _imuSensorActive = _fallSensorService.isRunning;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(
          () => _config = const SafetyConfig(
            fallDetectionEnabled: false,
            sensitivityLevel: 'MEDIUM',
            emergencyAutoAlert: true,
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }

  Future<void> _onFallDetectionToggle(bool enable) async {
    if (enable) {
      final activated = await Navigator.of(context).push<bool>(
        MaterialPageRoute(builder: (_) => const EnableFallDetectionScreen()),
      );
      if (activated == true) await _load();
    } else {
      final disabled = await showDisableFallDetectionSheet(context);
      if (disabled == true) {
        await _fallSensorService.stop();
        await _load();
      }
    }
  }

  Future<void> _onImuSensorToggle(bool enable) async {
    if (enable) {
      if (!(_config?.fallDetectionEnabled ?? false)) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Vui lòng bật phát hiện ngã trước')),
        );
        return;
      }
      await _fallSensorService.start();
    } else {
      await _fallSensorService.stop();
    }
    if (mounted) {
      setState(() => _imuSensorActive = _fallSensorService.isRunning);
    }
  }

  Future<void> _triggerManualEmergency() async {
    try {
      await _emergencyService.openFlow(triggerSource: 'MANUAL');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
              'Đã kích hoạt hỗ trợ khẩn cấp và gửi cảnh báo người thân',
            ),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Không thể kích hoạt hỗ trợ khẩn cấp: $e')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final fallDetectionEnabled = _config?.fallDetectionEnabled ?? false;
    return Scaffold(
      backgroundColor: _surface,
      body: SafeArea(
        child: _loading
            ? const Center(
                child: CircularProgressIndicator(color: _primaryContainer),
              )
            : RefreshIndicator(
                color: _primaryContainer,
                onRefresh: _load,
                child: SingleChildScrollView(
                  padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      _buildTopBar(),
                      const SizedBox(height: 16),
                      const Text(
                        'Giám sát an toàn',
                        style: TextStyle(
                          fontSize: 24,
                          fontWeight: FontWeight.w600,
                          color: _onSurface,
                        ),
                      ),
                      const SizedBox(height: 4),
                      const Text(
                        'Theo dõi hoạt động và phát hiện sự cố theo thời gian thực.',
                        style: TextStyle(
                          fontSize: 14,
                          color: _onSurfaceVariant,
                        ),
                      ),
                      const SizedBox(height: 24),
                      _buildStatusCard(fallDetectionEnabled),
                      const SizedBox(height: 24),
                      _buildSosButtons(),
                      const SizedBox(height: 24),
                      _buildEventHistory(),
                      const SizedBox(height: 16),
                      _buildPermissionStatus(),
                    ],
                  ),
                ),
              ),
      ),
    );
  }

  Widget _buildTopBar() {
    return SizedBox(
      height: 56,
      child: Row(
        children: [
          SizedBox(
            width: 40,
            height: 40,
            child: IconButton(
              padding: EdgeInsets.zero,
              icon: const Icon(Icons.arrow_back, color: _primary, size: 20),
              onPressed: () => Navigator.of(context).pop(),
            ),
          ),
          const SizedBox(width: 8),
          Container(
            width: 40,
            height: 40,
            decoration: const BoxDecoration(
              color: _surfaceContainerHighest,
              shape: BoxShape.circle,
            ),
            child: const Center(
              child: Text(
                'N',
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w600,
                  color: _primary,
                ),
              ),
            ),
          ),
          const SizedBox(width: 12),
          const Expanded(
            child: Text(
              'CareBridge',
              style: TextStyle(
                fontSize: 32,
                fontWeight: FontWeight.w700,
                color: _primary,
                letterSpacing: -0.5,
              ),
            ),
          ),
          IconButton(
            icon: const Icon(Icons.notifications_outlined, color: _primary),
            onPressed: () {},
          ),
        ],
      ),
    );
  }

  Widget _buildStatusCard(bool fallDetectionEnabled) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surfaceContainerLowest,
        borderRadius: BorderRadius.circular(32),
        border: Border.all(color: _surfaceContainer),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0F5A463F),
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
                decoration: BoxDecoration(
                  color: _primaryContainer.withValues(alpha: 0.2),
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.sensors, color: _primary),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Cảm biến IMU',
                      style: TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.w600,
                        color: _onSurface,
                      ),
                    ),
                    Row(
                      children: [
                        Container(
                          width: 8,
                          height: 8,
                          decoration: const BoxDecoration(
                            color: _primary,
                            shape: BoxShape.circle,
                          ),
                        ),
                        const SizedBox(width: 4),
                        Text(
                          _imuSensorActive ? 'Đang hoạt động' : 'Đã tắt',
                          style: const TextStyle(
                            fontSize: 14,
                            color: _primary,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              Switch(
                value: _imuSensorActive,
                activeThumbColor: Colors.white,
                activeTrackColor: _primaryContainer,
                onChanged: _onImuSensorToggle,
              ),
            ],
          ),
          const Divider(height: 24, color: _surfaceVariant),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  const Icon(Icons.personal_injury_outlined, color: _tertiary),
                  const SizedBox(width: 8),
                  const Text(
                    'Phát hiện ngã (Fall Detection)',
                    style: TextStyle(fontSize: 14, color: _onSurface),
                  ),
                ],
              ),
              Switch(
                value: fallDetectionEnabled,
                activeThumbColor: Colors.white,
                activeTrackColor: _primaryContainer,
                onChanged: _onFallDetectionToggle,
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildSosButtons() {
    return Row(
      children: [
        Expanded(
          child: _BentoButton(
            icon: Icons.emergency,
            label: 'Gọi khẩn cấp SOS',
            background: _errorContainer,
            foreground: _onErrorContainer,
            onTap: _triggerManualEmergency,
          ),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: _BentoButton(
            icon: Icons.contacts,
            label: 'Liên hệ người thân',
            background: _secondaryContainer,
            foreground: _onSecondaryContainer,
            onTap: () => Navigator.of(context).push(
              MaterialPageRoute(
                builder: (_) => const EmergencyContactsScreen(),
              ),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildEventHistory() {
    final eventDisplays = _events.take(5).map(_toEventDisplay).toList();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text(
              'Lịch sử sự kiện',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w600,
                color: _onSurface,
              ),
            ),
            TextButton(
              onPressed: _load,
              child: const Text(
                'Xem tất cả',
                style: TextStyle(color: _primary, fontWeight: FontWeight.w500),
              ),
            ),
          ],
        ),
        const SizedBox(height: 8),
        if (eventDisplays.isEmpty)
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: _surfaceContainerLowest,
              borderRadius: BorderRadius.circular(28),
              boxShadow: const [
                BoxShadow(
                  color: Color(0x0F5A463F),
                  blurRadius: 20,
                  offset: Offset(0, 4),
                ),
              ],
            ),
            child: const Text(
              'Chưa có sự kiện an toàn nào được ghi nhận.',
              style: TextStyle(fontSize: 14, color: _onSurfaceVariant),
            ),
          )
        else
          ...eventDisplays.map(
            (e) => InkWell(
              onTap: e.event == null ? null : () => _showEventActions(e.event!),
              borderRadius: BorderRadius.circular(28),
              child: Container(
                margin: const EdgeInsets.only(bottom: 12),
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: _surfaceContainerLowest,
                  borderRadius: BorderRadius.circular(28),
                  border: Border(left: BorderSide(color: e.color, width: 4)),
                  boxShadow: const [
                    BoxShadow(
                      color: Color(0x0F5A463F),
                      blurRadius: 20,
                      offset: Offset(0, 4),
                    ),
                  ],
                ),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Padding(
                      padding: const EdgeInsets.only(top: 2),
                      child: Icon(e.icon, color: e.color, size: 20),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Expanded(
                                child: Text(
                                  e.title,
                                  style: const TextStyle(
                                    fontSize: 16,
                                    fontWeight: FontWeight.w600,
                                    color: _onSurface,
                                  ),
                                ),
                              ),
                              Text(
                                e.time,
                                style: const TextStyle(
                                  fontSize: 12,
                                  color: _onSurfaceVariant,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 4),
                          Text(
                            e.description,
                            style: const TextStyle(
                              fontSize: 14,
                              color: _onSurfaceVariant,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
      ],
    );
  }

  _SafetyEventDisplay _toEventDisplay(SafetyEvent event) {
    final isOpen = event.status == 'OPEN';
    final isFalsePositive = event.status == 'FALSE_POSITIVE';
    final isSafe = event.status == 'CONFIRMED_SAFE';
    final time = _formatEventTime(event.detectedAt);
    if (isFalsePositive) {
      return _SafetyEventDisplay(
        event: event,
        icon: Icons.info_outline,
        color: _surfaceVariant,
        title: 'Đã báo cáo cảnh báo sai',
        description: event.notes?.isNotEmpty == true
            ? event.notes!
            : 'Sự kiện đã được đánh dấu là phát hiện nhầm.',
        time: time,
      );
    }
    if (isSafe) {
      return _SafetyEventDisplay(
        event: event,
        icon: Icons.check_circle_outline,
        color: _primaryContainer,
        title: 'Đã xác nhận an toàn',
        description: event.notes?.isNotEmpty == true
            ? event.notes!
            : 'Người dùng đã xác nhận không cần hỗ trợ khẩn cấp.',
        time: time,
      );
    }
    return _SafetyEventDisplay(
      event: event,
      icon: isOpen ? Icons.warning_amber_outlined : Icons.emergency_outlined,
      color: isOpen ? const Color(0xFFE8A87C) : _onErrorContainer,
      title: event.eventType == 'SUSPECTED_IMPACT'
          ? 'Phát hiện va chạm nghi ngờ'
          : 'Phát hiện chuyển động bất thường',
      description:
          'Hệ thống ghi nhận gia tốc ${event.magnitude.toStringAsFixed(1)} m/s². Vui lòng kiểm tra tình trạng an toàn.',
      time: time,
    );
  }

  String _formatEventTime(DateTime? value) {
    if (value == null) return '--:--';
    final local = value.toLocal();
    final hour = local.hour.toString().padLeft(2, '0');
    final minute = local.minute.toString().padLeft(2, '0');
    return '$hour:$minute';
  }

  Future<void> _showEventActions(SafetyEvent event) async {
    if (event.status != 'OPEN') return;
    await showModalBottomSheet<void>(
      context: context,
      backgroundColor: _surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(32)),
      ),
      builder: (context) {
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(24, 20, 24, 24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Text(
                  'Xử lý cảnh báo an toàn',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.w700,
                    color: _onSurface,
                  ),
                ),
                const SizedBox(height: 16),
                _EventActionButton(
                  icon: Icons.check_circle_outline,
                  label: 'Tôi vẫn an toàn',
                  onTap: () => _handleEventAction(
                    event,
                    () => _safetyService.confirmSafetyCheck(
                      event.id,
                      note: 'Người dùng xác nhận an toàn từ ứng dụng.',
                    ),
                  ),
                ),
                _EventActionButton(
                  icon: Icons.report_gmailerrorred_outlined,
                  label: 'Báo cáo phát hiện sai',
                  onTap: () => _handleEventAction(
                    event,
                    () => _safetyService.reportFalsePositive(
                      event.id,
                      note: 'Người dùng báo cáo phát hiện sai.',
                    ),
                  ),
                ),
                _EventActionButton(
                  icon: Icons.emergency_outlined,
                  label: 'Gửi cảnh báo khẩn cấp',
                  color: _onErrorContainer,
                  onTap: () => _handleEventAction(
                    event,
                    () => _safetyService.sendEmergencyAlertForEvent(event.id),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Future<void> _handleEventAction(
    SafetyEvent event,
    Future<void> Function() action,
  ) async {
    Navigator.of(context).pop();
    try {
      await action();
      await _load();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Không thể cập nhật sự kiện: $e')),
        );
      }
    }
  }

  Widget _buildPermissionStatus() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: _surfaceContainer,
        borderRadius: BorderRadius.circular(28),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0F5A463F),
            blurRadius: 20,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Row(
        children: [
          const Icon(Icons.verified_user_outlined, color: _tertiary),
          const SizedBox(width: 12),
          const Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Quyền truy cập thiết bị',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: _onSurface,
                  ),
                ),
                Text(
                  'Cảm biến, thông báo đã sẵn sàng',
                  style: TextStyle(fontSize: 12, color: _tertiary),
                ),
              ],
            ),
          ),
          const Icon(Icons.check_circle, color: _primary),
        ],
      ),
    );
  }
}

class _SafetyEventDisplay {
  final SafetyEvent? event;
  final IconData icon;
  final Color color;
  final String title;
  final String description;
  final String time;

  const _SafetyEventDisplay({
    this.event,
    required this.icon,
    required this.color,
    required this.title,
    required this.description,
    required this.time,
  });
}

class _EventActionButton extends StatelessWidget {
  const _EventActionButton({
    required this.icon,
    required this.label,
    required this.onTap,
    this.color = const Color(0xFF845143),
  });

  final IconData icon;
  final String label;
  final VoidCallback onTap;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: OutlinedButton.icon(
        onPressed: onTap,
        icon: Icon(icon),
        label: Text(label),
        style: OutlinedButton.styleFrom(
          foregroundColor: color,
          minimumSize: const Size.fromHeight(52),
          shape: const StadiumBorder(),
        ),
      ),
    );
  }
}

class _BentoButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color background;
  final Color foreground;
  final VoidCallback onTap;

  const _BentoButton({
    required this.icon,
    required this.label,
    required this.background,
    required this.foreground,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(28),
      child: Container(
        height: 120,
        decoration: BoxDecoration(
          color: background,
          borderRadius: BorderRadius.circular(28),
          boxShadow: const [
            BoxShadow(
              color: Color(0x0F5A463F),
              blurRadius: 20,
              offset: Offset(0, 4),
            ),
          ],
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, color: foreground, size: 32),
            const SizedBox(height: 8),
            Text(
              label,
              textAlign: TextAlign.center,
              style: TextStyle(color: foreground, fontWeight: FontWeight.w600),
            ),
          ],
        ),
      ),
    );
  }
}
