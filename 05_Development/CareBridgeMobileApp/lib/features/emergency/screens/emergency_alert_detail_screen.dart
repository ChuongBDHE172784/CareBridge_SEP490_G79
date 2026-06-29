import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';
import '../models/emergency_alert_model.dart';

class EmergencyAlertDetailScreen extends StatefulWidget {
  final EmergencyAlert? alert;
  final String? notificationId;

  const EmergencyAlertDetailScreen({
    super.key,
    this.alert,
    this.notificationId,
  });

  @override
  State<EmergencyAlertDetailScreen> createState() =>
      _EmergencyAlertDetailScreenState();
}

class _EmergencyAlertDetailScreenState
    extends State<EmergencyAlertDetailScreen> {
  static const _bgColor = Color(0xFFFFF8F6);
  static const _canvasColor = Color(0xFFF6F1EC);
  static const _primaryColor = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);
  static const _errorColor = Color(0xFFBA1A1A);
  static const _errorContainer = Color(0xFFFFDAD6);
  static const _surfaceContainerLow = Color(0xFFFFF1EC);
  static const _surfaceContainer = Color(0xFFFFE9E3);
  static const _secondaryContainer = Color(0xFFF6DACF);
  static const _onSecondaryContainer = Color(0xFF735E56);

  late EmergencyAlert _alert;
  bool _acknowledged = false;

  @override
  void initState() {
    super.initState();
    // MOCK — replace when backend endpoint is available
    _alert = widget.alert ?? EmergencyAlert.mock();
    _acknowledged = _alert.acknowledged;
  }

  String _formatTime(DateTime dateTime) {
    final diff = DateTime.now().difference(dateTime);
    if (diff.inMinutes < 1) return 'Vừa xong';
    if (diff.inMinutes < 60) return '${diff.inMinutes} phút trước';
    if (diff.inHours < 24) return '${diff.inHours} giờ trước';
    return '${dateTime.day.toString().padLeft(2, '0')}/${dateTime.month.toString().padLeft(2, '0')}/${dateTime.year}';
  }

  String get _alertTypeLabel {
    switch (_alert.alertType) {
      case 'FALL_DETECTED':
        return 'Phát hiện ngã!';
      case 'HEART_RATE':
        return 'Nhịp tim bất thường!';
      case 'SOS':
        return 'Cuộc gọi SOS!';
      default:
        return 'Cảnh báo khẩn cấp';
    }
  }

  Future<void> _makeCall() async {
    final phone = _alert.phoneNumber;
    if (phone == null) return;
    final uri = Uri.parse('tel:$phone');
    if (await canLaunchUrl(uri)) await launchUrl(uri);
  }

  Future<void> _openDirections() async {
    if (_alert.latitude == null || _alert.longitude == null) return;
    final uri = Uri.parse(
        'https://www.google.com/maps/dir/?api=1&destination=${_alert.latitude},${_alert.longitude}');
    if (await canLaunchUrl(uri)) await launchUrl(uri, mode: LaunchMode.externalApplication);
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
              child: ListView(
                padding: const EdgeInsets.fromLTRB(24, 8, 24, 0),
                children: [
                  _buildAlertBanner(),
                  const SizedBox(height: 24),
                  _buildMapPlaceholder(),
                  const SizedBox(height: 16),
                  _buildLocationCard(),
                  const SizedBox(height: 16),
                  _buildStatsRow(),
                  const SizedBox(height: 24),
                ],
              ),
            ),
            _buildActionBar(),
          ],
        ),
      ),
    );
  }

  Widget _buildAppBar() {
    return Container(
      color: _bgColor,
      height: 48,
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
          const Expanded(
            child: Text(
              'Cảnh báo khẩn cấp',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 20,
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

  Widget _buildAlertBanner() {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: _errorContainer,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: _errorColor.withValues(alpha: 0.1)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 48,
            height: 48,
            decoration: const BoxDecoration(
              color: _errorColor,
              shape: BoxShape.circle,
            ),
            child: const Icon(Icons.warning_rounded, color: Colors.white, size: 28),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      'KHẨN CẤP',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        fontWeight: FontWeight.w500,
                        letterSpacing: 0.6,
                        color: _errorColor,
                      ),
                    ),
                    Text(
                      _formatTime(_alert.createdAt),
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        fontWeight: FontWeight.w500,
                        letterSpacing: 0.6,
                        color: _onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                Text(
                  _alertTypeLabel,
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 24,
                    fontWeight: FontWeight.w600,
                    color: _errorColor,
                  ),
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    CircleAvatar(
                      radius: 12,
                      backgroundColor: _surfaceContainer,
                      child: const Icon(Icons.person, size: 16, color: _primaryColor),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        _alert.personName,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 14,
                          fontWeight: FontWeight.w600,
                          color: _onSurface,
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMapPlaceholder() {
    return Container(
      height: 220,
      decoration: BoxDecoration(
        color: _surfaceContainer,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: _outlineVariant),
      ),
      child: Stack(
        children: [
          Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  padding: const EdgeInsets.all(8),
                  decoration: const BoxDecoration(
                    color: _primaryColor,
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(Icons.person_pin_circle,
                      color: Colors.white, size: 24),
                ),
                const SizedBox(height: 8),
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(9999),
                    boxShadow: const [
                      BoxShadow(
                        color: Color.fromRGBO(90, 70, 63, 0.12),
                        blurRadius: 8,
                        offset: Offset(0, 2),
                      ),
                    ],
                  ),
                  child: const Text(
                    'Vị trí của Mẹ',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      fontWeight: FontWeight.w500,
                      color: _primaryColor,
                    ),
                  ),
                ),
              ],
            ),
          ),
          Positioned(
            bottom: 8,
            right: 8,
            child: Container(
              padding: const EdgeInsets.all(6),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(8),
                boxShadow: const [
                  BoxShadow(
                    color: Color.fromRGBO(0, 0, 0, 0.1),
                    blurRadius: 4,
                  ),
                ],
              ),
              child: const Icon(Icons.map_outlined,
                  color: _onSurfaceVariant, size: 20),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildLocationCard() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
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
          Container(
            width: 40,
            height: 40,
            decoration: const BoxDecoration(
              color: _surfaceContainerLow,
              shape: BoxShape.circle,
            ),
            child: const Icon(Icons.location_on, color: _primaryColor, size: 20),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Vị trí hiện tại',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    fontWeight: FontWeight.w500,
                    letterSpacing: 0.6,
                    color: _onSurfaceVariant,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  _alert.address ?? 'Không xác định',
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    fontWeight: FontWeight.w500,
                    color: _onSurface,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStatsRow() {
    return Row(
      children: [
        Expanded(child: _buildStatCard(Icons.favorite_outline, 'Nhịp tim',
            '${_alert.heartRate ?? '--'}', 'bpm', _primaryColor)),
        const SizedBox(width: 16),
        Expanded(child: _buildStatCard(Icons.battery_charging_full_outlined,
            'Thiết bị', '${_alert.deviceBattery ?? '--'}%', null, _onSurface)),
      ],
    );
  }

  Widget _buildStatCard(
      IconData icon, String label, String value, String? unit, Color valueColor) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
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
              Icon(icon, color: _primaryColor, size: 18),
              const SizedBox(width: 8),
              Text(label,
                  style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      fontWeight: FontWeight.w500,
                      letterSpacing: 0.6,
                      color: _onSurfaceVariant)),
            ],
          ),
          const SizedBox(height: 8),
          RichText(
            text: TextSpan(
              style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 20,
                  fontWeight: FontWeight.w600,
                  color: valueColor),
              children: [
                TextSpan(text: value),
                if (unit != null)
                  TextSpan(
                    text: ' $unit',
                    style: const TextStyle(
                        fontSize: 12, fontWeight: FontWeight.w400),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildActionBar() {
    return Container(
      padding: const EdgeInsets.fromLTRB(24, 16, 24, 24),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.8),
        border: Border(
            top: BorderSide(color: _outlineVariant.withValues(alpha: 0.2))),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          SizedBox(
            width: double.infinity,
            height: 48,
            child: FilledButton.icon(
              onPressed: _acknowledged
                  ? null
                  : () => setState(() => _acknowledged = true),
              icon: Icon(_acknowledged
                  ? Icons.check_circle
                  : Icons.check_circle_outline, size: 20),
              label: Text(
                _acknowledged
                    ? 'Đã tiếp nhận cảnh báo'
                    : 'Tiếp nhận cảnh báo',
                style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 16,
                    fontWeight: FontWeight.w600),
              ),
              style: FilledButton.styleFrom(
                backgroundColor:
                    _acknowledged ? _surfaceContainer : _surfaceContainer,
                foregroundColor: _onSecondaryContainer,
                disabledBackgroundColor: _surfaceContainer,
                disabledForegroundColor: _onSecondaryContainer,
                shape: const StadiumBorder(),
                elevation: 0,
              ),
            ),
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                flex: 3,
                child: SizedBox(
                  height: 48,
                  child: FilledButton.icon(
                    onPressed: _makeCall,
                    icon: const Icon(Icons.call, size: 22),
                    label: const Text('Gọi ngay',
                        style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 16,
                            fontWeight: FontWeight.w600)),
                    style: FilledButton.styleFrom(
                      backgroundColor: _primaryColor,
                      foregroundColor: Colors.white,
                      shape: const StadiumBorder(),
                      elevation: 2,
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                flex: 2,
                child: SizedBox(
                  height: 48,
                  child: FilledButton.icon(
                    onPressed: _openDirections,
                    icon: const Icon(Icons.directions, size: 22),
                    label: const Text('Chỉ đường',
                        style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 16,
                            fontWeight: FontWeight.w600)),
                    style: FilledButton.styleFrom(
                      backgroundColor: _secondaryContainer,
                      foregroundColor: _onSecondaryContainer,
                      shape: const StadiumBorder(),
                      elevation: 0,
                    ),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
