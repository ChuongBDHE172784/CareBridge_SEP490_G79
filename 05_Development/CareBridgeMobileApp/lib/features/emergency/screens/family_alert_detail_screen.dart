// ignore_for_file: curly_braces_in_flow_control_structures

import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../models/emergency_alert_model.dart';
import '../services/emergency_service.dart';

/// CB-032 - Family Alert Detail (UC-86, UC-161)
/// Family-facing emergency alert detail reached from FCM notification payload.
class FamilyAlertDetailScreen extends StatefulWidget {
  final String sessionId;

  const FamilyAlertDetailScreen({super.key, required this.sessionId});

  @override
  State<FamilyAlertDetailScreen> createState() =>
      _FamilyAlertDetailScreenState();
}

class _FamilyAlertDetailScreenState extends State<FamilyAlertDetailScreen> {
  static const _background = Color(0xFFFFF8F6);
  static const _surface = Color(0xFFFFFFFF);
  static const _surfaceContainer = Color(0xFFFFE9E3);
  static const _surfaceContainerLow = Color(0xFFFFF1EC);
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _onPrimaryContainer = Color(0xFF51271B);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);
  static const _error = Color(0xFFBA1A1A);
  static const _errorContainer = Color(0xFFFFDAD6);

  final _service = EmergencyService();
  EmergencyAlert? _alert;
  bool _loading = true;
  bool _acknowledged = false;
  String? _errorText;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _errorText = null;
    });
    try {
      final alert = await _service.getAlertDetail(widget.sessionId);
      if (!mounted) return;
      setState(() {
        _alert = alert;
        _acknowledged = alert.acknowledged;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() => _errorText = 'Không thể tải chi tiết cảnh báo: $error');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  String _formatTime(DateTime value) {
    final now = DateTime.now();
    final difference = now.difference(value);
    if (difference.inMinutes < 1) return 'Vừa xong';
    if (difference.inMinutes < 60) return '${difference.inMinutes} phút trước';
    if (difference.inHours < 24) return '${difference.inHours} giờ trước';
    return '${value.hour.toString().padLeft(2, '0')}:${value.minute.toString().padLeft(2, '0')}, ${value.day.toString().padLeft(2, '0')}/${value.month.toString().padLeft(2, '0')}/${value.year}';
  }

  String get _headline {
    final alert = _alert;
    if (alert == null) return 'Cảnh báo khẩn cấp';
    if (alert.alertType == 'FALL_DETECTED')
      return '${alert.personName} di chuyển bất thường';
    if (alert.alertType == 'SOS')
      return '${alert.personName} cần hỗ trợ khẩn cấp';
    return 'Bé Mỡ di chuyển bất thường';
  }

  Future<void> _callContact() async {
    final phone = _alert?.phoneNumber;
    if (phone == null || phone.isEmpty) return;
    final uri = Uri.parse('tel:$phone');
    if (await canLaunchUrl(uri)) await launchUrl(uri);
  }

  Future<void> _openDirections() async {
    final alert = _alert;
    if (alert?.latitude == null || alert?.longitude == null) return;
    final uri = Uri.parse(
      'https://www.google.com/maps/dir/?api=1&destination=${alert!.latitude},${alert.longitude}',
    );
    if (await canLaunchUrl(uri)) {
      await launchUrl(uri, mode: LaunchMode.externalApplication);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _background,
      body: SafeArea(
        child: Column(
          children: [
            _buildAppBar(context),
            Expanded(child: _buildBody()),
          ],
        ),
      ),
    );
  }

  Widget _buildAppBar(BuildContext context) {
    return Container(
      height: 56,
      padding: const EdgeInsets.symmetric(horizontal: 24),
      color: _background,
      child: Row(
        children: [
          SizedBox(
            width: 48,
            height: 48,
            child: IconButton(
              onPressed: () => Navigator.of(context).pop(),
              icon: const Icon(Icons.arrow_back, color: _onSurfaceVariant),
              padding: EdgeInsets.zero,
            ),
          ),
          const Expanded(
            child: Text(
              'Chi tiết cảnh báo',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                color: _onSurface,
                fontSize: 20,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildBody() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator(color: _primary));
    }
    if (_errorText != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                _errorText!,
                textAlign: TextAlign.center,
                style: const TextStyle(color: _error, fontFamily: 'Lexend'),
              ),
              const SizedBox(height: 16),
              FilledButton(onPressed: _load, child: const Text('Thử lại')),
            ],
          ),
        ),
      );
    }

    return ListView(
      padding: const EdgeInsets.fromLTRB(24, 8, 24, 24),
      children: [
        _buildAlertHeader(),
        const SizedBox(height: 24),
        _buildLocationCard(),
        const SizedBox(height: 24),
        _buildActions(),
        const SizedBox(height: 32),
        _buildPrivacyNote(),
      ],
    );
  }

  Widget _buildAlertHeader() {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: _errorContainer,
        borderRadius: BorderRadius.circular(28),
      ),
      child: Column(
        children: [
          Container(
            width: 64,
            height: 64,
            decoration: const BoxDecoration(
              color: _error,
              shape: BoxShape.circle,
            ),
            child: const Icon(
              Icons.warning_rounded,
              color: Colors.white,
              size: 36,
            ),
          ),
          const SizedBox(height: 16),
          Text(
            _headline,
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontFamily: 'Lexend',
              color: Color(0xFF93000A),
              fontSize: 24,
              height: 1.25,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 14),
          Wrap(
            alignment: WrapAlignment.center,
            crossAxisAlignment: WrapCrossAlignment.center,
            spacing: 8,
            runSpacing: 8,
            children: [
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 10,
                  vertical: 5,
                ),
                decoration: BoxDecoration(
                  color: _error.withValues(alpha: 0.1),
                  borderRadius: BorderRadius.circular(999),
                ),
                child: const Text(
                  'KHẨN CẤP',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    color: _error,
                    fontSize: 12,
                    fontWeight: FontWeight.w700,
                    letterSpacing: 0.6,
                  ),
                ),
              ),
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(
                    Icons.schedule,
                    color: _onSurfaceVariant,
                    size: 16,
                  ),
                  const SizedBox(width: 4),
                  Text(
                    _formatTime(_alert!.createdAt),
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      color: _onSurfaceVariant,
                      fontSize: 13,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildLocationCard() {
    final alert = _alert!;
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: _surface,
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
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.location_on, color: _primary),
              SizedBox(width: 8),
              Text(
                'Vị trí gần nhất',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 20,
                  color: _onSurface,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Container(
            height: 190,
            decoration: BoxDecoration(
              color: _surfaceContainer,
              borderRadius: BorderRadius.circular(20),
              border: Border.all(color: _outlineVariant),
            ),
            child: Stack(
              children: [
                Positioned.fill(child: CustomPaint(painter: _SoftMapPainter())),
                Center(
                  child: Container(
                    padding: const EdgeInsets.all(10),
                    decoration: const BoxDecoration(
                      color: _primary,
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(
                      Icons.location_pin,
                      color: Colors.white,
                      size: 28,
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),
          Text(
            alert.address?.isNotEmpty == true
                ? 'Hệ thống ghi nhận vị trí cuối cùng tại ${alert.address}.'
                : "Hệ thống ghi nhận người thân đã rời khỏi khu vực 'Nhà'. Vị trí cuối cùng được cập nhật cách đây vài phút.",
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              height: 1.45,
              color: _onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildActions() {
    return Column(
      children: [
        SizedBox(
          width: double.infinity,
          height: 52,
          child: FilledButton.icon(
            onPressed: _callContact,
            icon: const Icon(Icons.phone_in_talk),
            label: const Text('Gọi cho Mẹ Linh'),
            style: FilledButton.styleFrom(
              backgroundColor: _primaryContainer,
              foregroundColor: _onPrimaryContainer,
              shape: const StadiumBorder(),
              textStyle: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 16,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ),
        const SizedBox(height: 14),
        SizedBox(
          width: double.infinity,
          height: 52,
          child: FilledButton.icon(
            onPressed: _openDirections,
            icon: const Icon(Icons.directions),
            label: const Text('Chỉ đường đến vị trí'),
            style: FilledButton.styleFrom(
              backgroundColor: _surfaceContainer,
              foregroundColor: _onSurface,
              shape: const StadiumBorder(),
              elevation: 0,
              textStyle: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 16,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ),
        const SizedBox(height: 14),
        SizedBox(
          width: double.infinity,
          height: 52,
          child: OutlinedButton.icon(
            onPressed: _acknowledged
                ? null
                : () => setState(() => _acknowledged = true),
            icon: Icon(
              _acknowledged ? Icons.check_circle : Icons.check_circle_outline,
            ),
            label: Text(_acknowledged ? 'Đã xác nhận' : 'Xác nhận đã xem'),
            style: OutlinedButton.styleFrom(
              foregroundColor: _onSurfaceVariant,
              side: const BorderSide(color: _outlineVariant),
              shape: const StadiumBorder(),
              textStyle: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 16,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildPrivacyNote() {
    return const Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Icon(Icons.lock, color: _onSurfaceVariant, size: 16),
        SizedBox(width: 8),
        Flexible(
          child: Text(
            'Vị trí được mã hóa và chỉ chia sẻ trong nhóm gia đình.',
            textAlign: TextAlign.center,
            style: TextStyle(
              fontFamily: 'Lexend',
              color: _onSurfaceVariant,
              fontSize: 12,
              height: 1.35,
            ),
          ),
        ),
      ],
    );
  }
}

class _SoftMapPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final roadPaint = Paint()
      ..color = Colors.white.withValues(alpha: 0.8)
      ..strokeWidth = 5
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round;
    final thinRoadPaint = Paint()
      ..color = const Color(0xFFD6C2BD).withValues(alpha: 0.65)
      ..strokeWidth = 2
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round;

    canvas.drawColor(
      _FamilyAlertDetailScreenState._surfaceContainerLow,
      BlendMode.src,
    );

    for (var index = 0; index < 4; index++) {
      final y = size.height * (0.2 + index * 0.2);
      final path = Path()
        ..moveTo(0, y)
        ..cubicTo(
          size.width * 0.25,
          y - 32,
          size.width * 0.65,
          y + 28,
          size.width,
          y - 8,
        );
      canvas.drawPath(path, index.isEven ? roadPaint : thinRoadPaint);
    }

    for (var index = 0; index < 3; index++) {
      final x = size.width * (0.25 + index * 0.25);
      final path = Path()
        ..moveTo(x, 0)
        ..cubicTo(
          x + 24,
          size.height * 0.25,
          x - 28,
          size.height * 0.7,
          x + 12,
          size.height,
        );
      canvas.drawPath(path, thinRoadPaint);
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
