import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';
import '../services/emergency_service.dart';

/// CB-017 — Emergency Map (UC-62, UC-63, UC-64, UC-65)
/// Opens (or resumes) an emergency session on entry, which server-side also
/// triggers the family alert (UC-65, event-driven — no dedicated endpoint).
/// Nearest-facility search (UC-63) has no backend endpoint yet, so the
/// facility shown below is mock data pending that implementation.
class EmergencyMapScreen extends StatefulWidget {
  const EmergencyMapScreen({super.key});

  @override
  State<EmergencyMapScreen> createState() => _EmergencyMapScreenState();
}

class _EmergencyMapScreenState extends State<EmergencyMapScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _onPrimaryContainer = Color(0xFF51271B);
  static const _surface = Color(0xFFFFF8F6);
  static const _surfaceContainer = Color(0xFFFFE9E3);
  static const _surfaceContainerHigh = Color(0xFFFFE2D9);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _error = Color(0xFFBA1A1A);
  static const _errorContainer = Color(0xFFFFDAD6);
  static const _onErrorContainer = Color(0xFF93000A);
  static const _secondaryContainer = Color(0xFFF6DACF);
  static const _onSecondaryContainer = Color(0xFF735E56);

  // Mock nearest facility — UC-63 "Find Nearest Healthcare Facility" has no
  // backend endpoint yet. TODO: replace with real search once implemented.
  static const _facilityName = 'Bệnh viện Nhi Đồng 1';
  static const _facilityAddress = '341 Sư Vạn Hạnh, Quận 10';
  static const _facilityPhone = '02839271119';
  static const _facilityDistanceKm = 1.2;
  static const _facilityEtaMin = 5;
  static const _facilityLat = 10.7626;
  static const _facilityLng = 106.6602;

  final _emergencyService = EmergencyService();
  bool _sendingAlert = false;

  @override
  void initState() {
    super.initState();
    _openFlow();
  }

  Future<void> _openFlow() async {
    try {
      await _emergencyService.openFlow(triggerSource: 'MANUAL');
    } catch (_) {
      // Non-fatal: the map/facility info still works without a session.
    }
  }

  Future<void> _sendFamilyAlert() async {
    setState(() => _sendingAlert = true);
    try {
      // Re-confirming open is idempotent server-side; family alert is sent
      // as an event side-effect of session creation (UC-65).
      await _emergencyService.openFlow(triggerSource: 'MANUAL');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Đã gửi báo động đến người thân')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Không thể gửi báo động: $e'), backgroundColor: _error),
        );
      }
    } finally {
      if (mounted) setState(() => _sendingAlert = false);
    }
  }

  Future<void> _call() async {
    final uri = Uri(scheme: 'tel', path: _facilityPhone);
    await launchUrl(uri);
  }

  Future<void> _openDirections() async {
    final uri = Uri.parse(
        'https://www.google.com/maps/dir/?api=1&destination=$_facilityLat,$_facilityLng');
    await launchUrl(uri, mode: LaunchMode.externalApplication);
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
              child: Stack(
                children: [
                  _buildMapBackground(),
                  Positioned(
                    top: 12,
                    right: 16,
                    child: _buildFamilyAlertButton(),
                  ),
                  Align(
                    alignment: Alignment.bottomCenter,
                    child: _buildFacilitySheet(),
                  ),
                ],
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
                onPressed: () => Navigator.of(context).pop(),
              ),
            ),
            const Expanded(
              child: Text('Bản đồ khẩn cấp',
                  textAlign: TextAlign.center,
                  style: TextStyle(fontSize: 20, fontWeight: FontWeight.w600, color: _primary)),
            ),
            const SizedBox(width: 48, height: 48),
          ],
        ),
      ),
    );
  }

  // Stylized static map background — no map SDK dependency in this app yet,
  // so this is a non-interactive placeholder matching the design's palette.
  Widget _buildMapBackground() {
    return Container(
      color: const Color(0xFFFBE9DF),
      child: CustomPaint(
        painter: _RoadGridPainter(),
        child: Stack(
          children: [
            Positioned(
              top: 90,
              left: 70,
              child: _buildMarker(
                icon: Icons.local_hospital,
                color: _error,
                label: 'BV Nhi Đồng',
              ),
            ),
            const Positioned(
              top: 220,
              right: 60,
              child: _SecondaryMarker(),
            ),
            const Align(
              alignment: Alignment.center,
              child: _UserLocationMarker(),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMarker({required IconData icon, required Color color, required String label}) {
    return Column(
      children: [
        Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: color,
            shape: BoxShape.circle,
            boxShadow: const [BoxShadow(color: Color(0x1F5A463F), blurRadius: 24, offset: Offset(0, 8))],
          ),
          child: Icon(icon, color: Colors.white, size: 20),
        ),
        const SizedBox(height: 4),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
          decoration: BoxDecoration(color: _surface, borderRadius: BorderRadius.circular(8)),
          child: Text(label, style: const TextStyle(fontSize: 12, color: _onSurface)),
        ),
      ],
    );
  }

  Widget _buildFamilyAlertButton() {
    return SizedBox(
      height: 48,
      child: ElevatedButton.icon(
        onPressed: _sendingAlert ? null : _sendFamilyAlert,
        style: ElevatedButton.styleFrom(
          backgroundColor: _error,
          foregroundColor: Colors.white,
          shape: const StadiumBorder(),
          padding: const EdgeInsets.symmetric(horizontal: 16),
          elevation: 4,
        ),
        icon: _sendingAlert
            ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2))
            : const Icon(Icons.campaign, size: 20),
        label: const Text('Báo động gia đình', style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600)),
      ),
    );
  }

  Widget _buildFacilitySheet() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(24, 12, 24, 24),
      decoration: const BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        boxShadow: [BoxShadow(color: Color(0x145A463F), blurRadius: 30, offset: Offset(0, -8))],
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(width: 48, height: 4, margin: const EdgeInsets.only(bottom: 16),
              decoration: BoxDecoration(color: _surfaceContainerHigh, borderRadius: BorderRadius.circular(99))),
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(_facilityName, style: TextStyle(fontSize: 20, fontWeight: FontWeight.w600, color: _onSurface)),
                    const SizedBox(height: 4),
                    Row(
                      children: const [
                        Icon(Icons.location_on, size: 16, color: _onSurfaceVariant),
                        SizedBox(width: 4),
                        Text(_facilityAddress, style: TextStyle(fontSize: 14, color: _onSurfaceVariant)),
                      ],
                    ),
                  ],
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(color: _errorContainer, borderRadius: BorderRadius.circular(99)),
                child: Column(
                  children: const [
                    Text('$_facilityEtaMin', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w600, color: _onErrorContainer, height: 1)),
                    Text('phút', style: TextStyle(fontSize: 12, color: _onErrorContainer, height: 1.2)),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(color: _surfaceContainer, borderRadius: BorderRadius.circular(6)),
                child: Row(mainAxisSize: MainAxisSize.min, children: const [
                  Icon(Icons.directions_car, size: 14, color: _onSurface),
                  SizedBox(width: 4),
                  Text('$_facilityDistanceKm km', style: TextStyle(fontSize: 12, color: _onSurface)),
                ]),
              ),
              const SizedBox(width: 8),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(color: _secondaryContainer, borderRadius: BorderRadius.circular(6)),
                child: Row(mainAxisSize: MainAxisSize.min, children: const [
                  Icon(Icons.check_circle, size: 14, color: _onSecondaryContainer),
                  SizedBox(width: 4),
                  Text('Mở cửa 24/7', style: TextStyle(fontSize: 12, color: _onSecondaryContainer)),
                ]),
              ),
            ],
          ),
          const SizedBox(height: 20),
          Row(
            children: [
              Expanded(
                child: SizedBox(
                  height: 52,
                  child: ElevatedButton.icon(
                    onPressed: _call,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: _surfaceContainerHigh,
                      foregroundColor: _onSurface,
                      shape: const StadiumBorder(),
                      elevation: 0,
                    ),
                    icon: const Icon(Icons.call),
                    label: const Text('Gọi điện', style: TextStyle(fontWeight: FontWeight.w600)),
                  ),
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: SizedBox(
                  height: 52,
                  child: ElevatedButton.icon(
                    onPressed: _openDirections,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: _primaryContainer,
                      foregroundColor: _onPrimaryContainer,
                      shape: const StadiumBorder(),
                      elevation: 2,
                    ),
                    icon: const Icon(Icons.directions),
                    label: const Text('Chỉ đường', style: TextStyle(fontWeight: FontWeight.w600)),
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

class _SecondaryMarker extends StatelessWidget {
  const _SecondaryMarker();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(8),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.85),
        shape: BoxShape.circle,
        border: Border.all(color: const Color(0xFFD6C2BD)),
      ),
      child: const Icon(Icons.medical_services_outlined, color: Color(0xFF845143), size: 18),
    );
  }
}

class _UserLocationMarker extends StatefulWidget {
  const _UserLocationMarker();

  @override
  State<_UserLocationMarker> createState() => _UserLocationMarkerState();
}

class _UserLocationMarkerState extends State<_UserLocationMarker> with SingleTickerProviderStateMixin {
  late final AnimationController _controller =
      AnimationController(vsync: this, duration: const Duration(seconds: 2))..repeat();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 48,
      height: 48,
      child: Stack(
        alignment: Alignment.center,
        children: [
          AnimatedBuilder(
            animation: _controller,
            builder: (_, __) {
              final scale = 0.6 + (_controller.value * 0.6);
              return Opacity(
                opacity: (1 - _controller.value).clamp(0.0, 1.0),
                child: Transform.scale(
                  scale: scale,
                  child: Container(
                    width: 48,
                    height: 48,
                    decoration: BoxDecoration(color: const Color(0xFF845143).withValues(alpha: 0.2), shape: BoxShape.circle),
                  ),
                ),
              );
            },
          ),
          Container(
            width: 16,
            height: 16,
            decoration: BoxDecoration(
              color: const Color(0xFF845143),
              shape: BoxShape.circle,
              border: Border.all(color: const Color(0xFFFFF8F6), width: 2),
            ),
          ),
        ],
      ),
    );
  }
}

class _RoadGridPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = const Color(0xFFF1D4CA)
      ..strokeWidth = 3;
    const gap = 56.0;
    for (double x = 0; x < size.width; x += gap) {
      canvas.drawLine(Offset(x, 0), Offset(x, size.height), paint);
    }
    for (double y = 0; y < size.height; y += gap) {
      canvas.drawLine(Offset(0, y), Offset(size.width, y), paint);
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
