import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import '../services/safety_service.dart';
import '../services/safety_foreground_service.dart';
import '../services/safety_permission_service.dart';
import '../../privacy/services/privacy_service.dart';

/// CB-129 — Enable Fall Detection Confirmation (UC-134)
/// Consent + setup screen shown before activating fall detection.
/// Submits PUT /api/v1/safety/config then POST /api/v1/safety/fall-detection/enable.
class EnableFallDetectionScreen extends StatefulWidget {
  const EnableFallDetectionScreen({super.key});

  @override
  State<EnableFallDetectionScreen> createState() =>
      _EnableFallDetectionScreenState();
}

class _EnableFallDetectionScreenState extends State<EnableFallDetectionScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surface = Color(0xFFFFF8F6);
  static const _surfaceContainerLowest = Color(0xFFFFFFFF);
  static const _surfaceVariant = Color(0xFFFADCD3);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);
  static const _error = Color(0xFFBA1A1A);

  final _safetyService = SafetyService();
  final _permissionService = SafetyPermissionService();
  final _foregroundCoordinator = SafetyForegroundServiceCoordinator.instance;

  // Persisted by SafetyConfigRequest.countdownSeconds.
  static const int _countdownSeconds = 30;
  // Mapped to SafetyConfigRequest.emergencyAutoAlert.
  bool _autoFamilyAlert = true;
  bool _shareLocation = false;
  bool? _sensorPermissionGranted;
  bool? _locationPermissionGranted;
  bool _consentChecked = false;
  bool _submitting = false;

  Future<void> _enable() async {
    if (!_foregroundCoordinator.isSupported) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
            'Thiết bị hoặc nền tảng này chưa hỗ trợ giám sát phát hiện ngã.',
          ),
        ),
      );
      return;
    }
    if (!_consentChecked) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Vui lòng đồng ý điều kiện sử dụng trước khi tiếp tục'),
        ),
      );
      return;
    }
    setState(() => _submitting = true);
    var configurationEnabled = false;
    try {
      final sensorGranted = await _permissionService.attestSensorAccess();
      if (mounted) setState(() => _sensorPermissionGranted = sensorGranted);
      if (!sensorGranted) {
        throw StateError(
          'Không thể xác minh quyền truy cập cảm biến chuyển động trên thiết bị.',
        );
      }
      await _ensureConsent(
        dataType: 'SENSOR_DATA',
        purpose: 'CREATE',
        scope: 'SAFETY_FALL_DETECTION',
      );
      final foregroundPermissionsGranted = await _foregroundCoordinator
          .requestRequiredPermissions();
      if (!foregroundPermissionsGranted) {
        throw StateError(
          'Cần cấp quyền thông báo để CareBridge duy trì giám sát an toàn.',
        );
      }
      if (_shareLocation) {
        await _ensureConsent(
          dataType: 'LOCATION',
          purpose: 'SHARE',
          scope: 'SAFETY_EMERGENCY_ALERT',
        );
        final position = await _permissionService.readConsentedLocation();
        if (mounted) {
          setState(() => _locationPermissionGranted = position != null);
        }
      }
      await _safetyService.updateConfig(
        fallDetectionEnabled: true,
        sensitivityLevel: 'MEDIUM',
        emergencyAutoAlert: _autoFamilyAlert,
        countdownSeconds: _countdownSeconds,
        sensorPermissionGranted: true,
      );
      configurationEnabled = true;
      await _safetyService.enableFallDetection();
      await _foregroundCoordinator.reconcile();
      if (!_foregroundCoordinator.isRunning) {
        throw StateError(
          'Không thể khởi động dịch vụ giám sát an toàn trên thiết bị.',
        );
      }
      if (mounted) Navigator.of(context).pop(true);
    } catch (e) {
      if (configurationEnabled) {
        try {
          await _safetyService.updateConfig(
            fallDetectionEnabled: false,
            sensitivityLevel: 'MEDIUM',
            emergencyAutoAlert: _autoFamilyAlert,
            countdownSeconds: _countdownSeconds,
            sensorPermissionGranted: true,
          );
          await _safetyService.disableFallDetection();
          await _foregroundCoordinator.stop();
        } catch (_) {
          // Preserve the original setup error; the monitoring screen will
          // reconcile the fail-closed state on the next resume.
        }
      }
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Không thể bật phát hiện ngã: $e'),
            backgroundColor: _error,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  Future<void> _ensureConsent({
    required String dataType,
    required String purpose,
    required String scope,
  }) async {
    final grants = await PrivacyService.instance.listConsents();
    final alreadyGranted = grants.any(
      (grant) =>
          grant.isActive &&
          grant.dataType == dataType &&
          grant.purpose == purpose,
    );
    if (alreadyGranted) return;
    await PrivacyService.instance.grantConsent(
      dataType: dataType,
      purpose: purpose,
      recipient: 'CAREBRIDGE_SAFETY',
      scope: scope,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _surface,
      body: SafeArea(
        child: Column(
          children: [
            _buildTopBar(),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(24, 8, 24, 24),
                child: _buildContent(),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTopBar() {
    return SizedBox(
      height: 48,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24),
        child: Row(
          children: [
            SizedBox(
              width: 48,
              height: 48,
              child: IconButton(
                padding: EdgeInsets.zero,
                icon: const Icon(Icons.arrow_back, color: _primary),
                onPressed: () => Navigator.of(context).pop(false),
              ),
            ),
            const Expanded(
              child: Text(
                'Kích hoạt cảm biến',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w700,
                  color: _primary,
                ),
              ),
            ),
            const SizedBox(
              width: 48,
              height: 48,
              child: Icon(Icons.help_outline, color: Color(0x80845143)),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildContent() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const SizedBox(height: 8),
        Center(
          child: Container(
            width: 80,
            height: 80,
            decoration: BoxDecoration(
              color: _primaryContainer.withValues(alpha: 0.2),
              shape: BoxShape.circle,
            ),
            child: const Icon(Icons.sensors, color: _primary, size: 40),
          ),
        ),
        const SizedBox(height: 16),
        const Text(
          'Phát hiện Ngã & Khẩn cấp',
          textAlign: TextAlign.center,
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.w600,
            color: _onSurface,
          ),
        ),
        const SizedBox(height: 8),
        const Text(
          'CareBridge sử dụng cảm biến trên điện thoại để nhận diện các cú ngã mạnh và tự động liên hệ người thân.',
          textAlign: TextAlign.center,
          style: TextStyle(fontSize: 14, color: _onSurfaceVariant, height: 1.4),
        ),
        const SizedBox(height: 24),
        // Permissions card
        Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: _surfaceContainerLowest,
            borderRadius: BorderRadius.circular(28),
            border: Border.all(color: _surfaceVariant.withValues(alpha: 0.3)),
            boxShadow: const [
              BoxShadow(
                color: Color(0x0F5A463F),
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
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: _primary.withValues(alpha: 0.1),
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(
                      Icons.settings_input_antenna,
                      color: _primary,
                      size: 20,
                    ),
                  ),
                  const SizedBox(width: 16),
                  const Text(
                    'Cấp quyền truy cập',
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.w600,
                      color: _onSurface,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              _PermissionRow(
                'Cảm biến chuyển động & Gia tốc',
                granted: _sensorPermissionGranted,
              ),
              const SizedBox(height: 8),
              _PermissionRow(
                'Vị trí khi cảnh báo (tùy chọn)',
                granted: _shareLocation ? _locationPermissionGranted : false,
                optional: true,
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        Row(
          children: [
            Expanded(
              child: _buildInfoTile(
                Icons.battery_charging_full,
                'Thiết bị cần được bật nguồn và sạc đầy',
              ),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: _buildInfoTile(
                Icons.signal_cellular_alt,
                'Kết nối Internet (Wifi/4G) ổn định',
              ),
            ),
          ],
        ),
        const SizedBox(height: 16),
        // Countdown + emergency alert
        Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: _surfaceContainerLowest,
            borderRadius: BorderRadius.circular(28),
            border: Border.all(color: _surfaceVariant.withValues(alpha: 0.3)),
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
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Row(
                    children: const [
                      Icon(Icons.timer_outlined, color: _primary),
                      SizedBox(width: 8),
                      Text(
                        'Thời gian chờ',
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w600,
                          color: _onSurface,
                        ),
                      ),
                    ],
                  ),
                  const Text(
                    '30 giây',
                    style: TextStyle(
                      fontWeight: FontWeight.w600,
                      color: _onSurface,
                    ),
                  ),
                ],
              ),
              const Divider(height: 24, color: _outlineVariant),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Row(
                    children: const [
                      Icon(
                        Icons.notification_important_outlined,
                        color: _primary,
                      ),
                      SizedBox(width: 8),
                      Text(
                        'Tự động báo người thân',
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w600,
                          color: _onSurface,
                        ),
                      ),
                    ],
                  ),
                  Switch(
                    value: _autoFamilyAlert,
                    activeThumbColor: Colors.white,
                    activeTrackColor: _primary,
                    onChanged: (v) => setState(() => _autoFamilyAlert = v),
                  ),
                ],
              ),
              const Divider(height: 24, color: _outlineVariant),
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: const Text('Chia sẻ vị trí khi có cảnh báo'),
                subtitle: const Text(
                  'Chỉ gửi khi bạn bật tùy chọn này, cấp quyền hệ điều hành và consent LOCATION/SHARE còn hiệu lực.',
                ),
                value: _shareLocation,
                onChanged: (value) => setState(() => _shareLocation = value),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        // Warning card
        Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: _primary.withValues(alpha: 0.05),
            borderRadius: BorderRadius.circular(28),
            border: const Border(left: BorderSide(color: _primary, width: 4)),
          ),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Icon(Icons.warning_amber_outlined, color: _primary),
              const SizedBox(width: 8),
              Expanded(
                child: RichText(
                  text: TextSpan(
                    style: const TextStyle(
                      fontSize: 14,
                      color: _onSurfaceVariant,
                      height: 1.4,
                    ),
                    children: [
                      const TextSpan(
                        text: 'Cảnh báo quan trọng\n',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          color: _onSurface,
                        ),
                      ),
                      TextSpan(text: 'Tính năng này được thiết kế để hỗ trợ, '),
                      TextSpan(
                        text: 'đây không phải là chẩn đoán y khoa',
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                      TextSpan(
                        text:
                            ' hoặc hệ thống cứu hộ chuyên nghiệp. Độ chính xác phụ thuộc vào phần cứng thiết bị.',
                      ),
                      if (!kIsWeb &&
                          defaultTargetPlatform == TargetPlatform.iOS)
                        const TextSpan(
                          text:
                              '\n\nTrên iPhone, iOS quyết định thời điểm và thời lượng chạy nền nên giám sát không liên tục và không được đảm bảo. Đóng cưỡng bức ứng dụng sẽ dừng giám sát; SOS thủ công vẫn luôn sẵn sàng.',
                        ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        // Consent checkbox
        InkWell(
          onTap: () => setState(() => _consentChecked = !_consentChecked),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Checkbox(
                value: _consentChecked,
                activeColor: _primaryContainer,
                onChanged: (v) => setState(() => _consentChecked = v ?? false),
              ),
              const Expanded(
                child: Padding(
                  padding: EdgeInsets.only(top: 12),
                  child: Text(
                    'Tôi đã đọc kỹ, hiểu rõ các điều kiện sử dụng và đồng ý kích hoạt tính năng phát hiện ngã.',
                    style: TextStyle(fontSize: 14, color: _onSurface),
                  ),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        SizedBox(
          width: double.infinity,
          height: 52,
          child: ElevatedButton.icon(
            onPressed: _submitting ? null : _enable,
            style: ElevatedButton.styleFrom(
              backgroundColor: _primary,
              foregroundColor: Colors.white,
              shape: const StadiumBorder(),
              elevation: 4,
            ),
            icon: _submitting
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(
                      color: Colors.white,
                      strokeWidth: 2,
                    ),
                  )
                : const Icon(Icons.power_settings_new),
            label: const Text(
              'Bật phát hiện ngã',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
            ),
          ),
        ),
        const SizedBox(height: 4),
        SizedBox(
          width: double.infinity,
          height: 52,
          child: TextButton(
            onPressed: _submitting
                ? null
                : () => Navigator.of(context).pop(false),
            child: const Text(
              'Để sau',
              style: TextStyle(color: _onSurfaceVariant, fontSize: 16),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildInfoTile(IconData icon, String text) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: _surfaceContainerLowest,
        borderRadius: BorderRadius.circular(28),
        border: Border.all(color: _surfaceVariant.withValues(alpha: 0.3)),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0F5A463F),
            blurRadius: 20,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: _primary),
          const SizedBox(height: 8),
          Text(
            text,
            style: const TextStyle(fontSize: 12, color: _onSurfaceVariant),
          ),
        ],
      ),
    );
  }
}

class _PermissionRow extends StatelessWidget {
  final String label;
  final bool? granted;
  final bool optional;
  const _PermissionRow(
    this.label, {
    required this.granted,
    this.optional = false,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(
          granted == true
              ? Icons.check_circle
              : granted == false
              ? Icons.radio_button_unchecked
              : Icons.hourglass_empty,
          color: granted == true
              ? const Color(0xFF845143)
              : const Color(0xFF77706D),
          size: 18,
        ),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            label,
            style: const TextStyle(fontSize: 14, color: Color(0xFF524440)),
          ),
        ),
        if (optional)
          const Text(
            'Tùy chọn',
            style: TextStyle(fontSize: 12, color: Color(0xFF77706D)),
          ),
      ],
    );
  }
}
