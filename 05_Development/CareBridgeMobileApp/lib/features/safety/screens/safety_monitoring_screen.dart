import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../models/safety_config_model.dart';
import '../services/fall_detection_sensor_service.dart';
import '../services/safety_service.dart';
import '../widgets/disable_fall_detection_sheet.dart';
import 'enable_fall_detection_screen.dart';
import '../../emergency/screens/emergency_map_screen.dart';

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
  final _fallSensorService = FallDetectionSensorService.instance;
  SafetyConfig? _config;
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
      if (config.fallDetectionEnabled) {
        await _fallSensorService.start();
      } else {
        await _fallSensorService.stop();
      }
      if (mounted) {
        setState(() {
          _config = config;
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
            onTap: () => Navigator.of(context).push(
              MaterialPageRoute(builder: (_) => const EmergencyMapScreen()),
            ),
          ),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: _BentoButton(
            icon: Icons.contacts,
            label: 'Liên hệ người thân',
            background: _secondaryContainer,
            foreground: _onSecondaryContainer,
            // UC-176 (dedicated emergency contacts) has no backend yet —
            // reuse the existing Care Groups screen as the closest "người
            // thân" contact list until that UC is implemented.
            onTap: () => context.push('/care-groups'),
          ),
        ),
      ],
    );
  }

  Widget _buildEventHistory() {
    // UC-139 (View Safety Event History) has no backend endpoint exposing
    // ISafetyEventRepository yet — showing mock data pending that work.
    final events = const [
      _SafetyEventDisplay(
        icon: Icons.warning,
        color: Color(0xFFE8A87C),
        title: 'Phát hiện chuyển động bất thường',
        description:
            'Hệ thống ghi nhận gia tốc tăng đột ngột. Vui lòng kiểm tra tình trạng bé.',
        time: '10:45 AM',
      ),
      _SafetyEventDisplay(
        icon: Icons.info,
        color: Color(0xFFD6C2BD),
        title: 'Bắt đầu theo dõi giấc ngủ',
        description: 'Chế độ giám sát ban đêm đã được kích hoạt.',
        time: '08:00 PM (Hôm qua)',
      ),
    ];
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
              onPressed: () => ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Tính năng đang được phát triển')),
              ),
              child: const Text(
                'Xem tất cả',
                style: TextStyle(color: _primary, fontWeight: FontWeight.w500),
              ),
            ),
          ],
        ),
        ...events.map(
          (e) => Container(
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
      ],
    );
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
                  'Bluetooth, Vị trí đã cấp quyền',
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
  final IconData icon;
  final Color color;
  final String title;
  final String description;
  final String time;

  const _SafetyEventDisplay({
    required this.icon,
    required this.color,
    required this.title,
    required this.description,
    required this.time,
  });
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
