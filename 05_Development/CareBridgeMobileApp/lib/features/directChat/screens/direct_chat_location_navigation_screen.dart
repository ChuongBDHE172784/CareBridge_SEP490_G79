import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_tts/flutter_tts.dart';
import 'package:geolocator/geolocator.dart';
import 'package:trackasia_gl/trackasia_gl.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../emergency/models/care_facility_model.dart';
import '../../emergency/services/care_facility_service.dart';

class DirectChatLocationNavigationScreen extends StatefulWidget {
  const DirectChatLocationNavigationScreen({
    super.key,
    required this.latitude,
    required this.longitude,
    this.label,
    this.careFacilityService,
  });

  final double latitude;
  final double longitude;
  final String? label;
  final CareFacilityService? careFacilityService;

  @override
  State<DirectChatLocationNavigationScreen> createState() =>
      _DirectChatLocationNavigationScreenState();
}

class _DirectChatLocationNavigationScreenState
    extends State<DirectChatLocationNavigationScreen> {
  static const _background = Color(0xFFF6F1EC);
  static const _surface = Color(0xFFFFFFFF);
  static const _accent = Color(0xFFC98C7B);
  static const _accentDark = Color(0xFF845143);
  static const _text = Color(0xFF5A463F);
  static const _muted = Color(0xFF9C857C);
  static const _stopRed = Color(0xFFDC2626);
  static const _configuredTrackAsiaKey = String.fromEnvironment(
    'TRACKASIA_MAP_KEY',
    defaultValue: String.fromEnvironment('TRACKASIA_API_KEY'),
  );

  String get _effectiveTrackAsiaKey {
    if (_configuredTrackAsiaKey.isNotEmpty) {
      return _configuredTrackAsiaKey;
    }
    if (WidgetsBinding.instance.runtimeType.toString().contains('Test')) {
      return '';
    }
    return 'd3e34fdc69a0d31780225041ffc88f4d4f';
  }

  late final CareFacilityService _routes;
  final FlutterTts _tts = FlutterTts();
  TrackAsiaMapController? _mapController;
  StreamSubscription<Position>? _positionSubscription;
  Timer? _styleWatchdog;
  Position? _position;
  CareRoute? _route;
  String? _error;
  bool _loading = true;
  bool _styleReady = false;
  bool _openingFallback = false;
  bool _isNavigating = false;
  bool _followUser = true;
  bool _voiceEnabled = true;
  bool _arrivedAnnounced = false;
  int _currentStepIndex = 0;

  String get _title => widget.label?.trim().isNotEmpty == true
      ? widget.label!.trim()
      : 'Vị trí được chia sẻ';

  @override
  void initState() {
    super.initState();
    _routes = widget.careFacilityService ?? CareFacilityService();
    unawaited(_configureTts());
    unawaited(_prepareNavigation());
  }

  @override
  void dispose() {
    _styleWatchdog?.cancel();
    unawaited(_positionSubscription?.cancel());
    unawaited(_tts.stop());
    super.dispose();
  }

  Future<void> _configureTts() async {
    try {
      await _tts.setLanguage('vi-VN');
      await _tts.setSpeechRate(0.48);
      await _tts.setVolume(1.0);
    } catch (_) {}
  }

  Future<void> _speak(String text) async {
    if (!_voiceEnabled || text.trim().isEmpty) return;
    try {
      await _tts.stop();
      await _tts.speak(text);
    } catch (_) {}
  }

  Future<void> _prepareNavigation() async {
    try {
      if (!await Geolocator.isLocationServiceEnabled()) {
        throw const FormatException('Dịch vụ vị trí đang tắt.');
      }
      var permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
      }
      if (permission == LocationPermission.denied ||
          permission == LocationPermission.deniedForever) {
        throw const FormatException('CareBridge chưa được cấp quyền vị trí.');
      }
      final position = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
        timeLimit: const Duration(seconds: 8),
      );
      CareRoute? route;
      try {
        route = await _routes.getRoute(
          fromLatitude: position.latitude,
          fromLongitude: position.longitude,
          toLatitude: widget.latitude,
          toLongitude: widget.longitude,
        );
      } catch (err) {
        // Route calculation fallback
      }
      if (!mounted) return;
      setState(() {
        _position = position;
        _route = route;
        _loading = false;
      });
      if (_styleReady) {
        unawaited(_syncMap());
      }
      _positionSubscription =
          Geolocator.getPositionStream(
            locationSettings: const LocationSettings(
              accuracy: LocationAccuracy.bestForNavigation,
              distanceFilter: 5,
            ),
          ).listen((next) {
            if (!mounted) return;
            setState(() {
              _position = next;
              _updateNavigationStep(next);
            });
            if (_isNavigating && _followUser) {
              unawaited(_trackUserCamera(next));
            } else {
              unawaited(_syncMarkersOnly());
            }
          });
      _styleWatchdog = Timer(const Duration(seconds: 12), () {
        if (mounted && !_styleReady && _effectiveTrackAsiaKey.isEmpty) {
          // Style timed out in keyless environment
        }
      });
    } catch (error) {
      if (mounted) {
        setState(() {
          _error = error.toString().replaceFirst('FormatException: ', '');
          _loading = false;
        });
      }
    }
  }

  Future<void> _fallbackToGoogleMaps() async {
    if (_openingFallback) return;
    _openingFallback = true;
    final uri = Uri.parse(
      'https://www.google.com/maps/dir/?api=1&destination='
      '${widget.latitude},${widget.longitude}&travelmode=driving',
    );
    try {
      final opened = await launchUrl(uri, mode: LaunchMode.externalApplication);
      if (!opened && mounted) {
        setState(() => _error = 'Không thể mở Google Maps trên thiết bị.');
      }
    } catch (_) {
      if (mounted) {
        setState(() => _error = 'Không thể mở Google Maps trên thiết bị.');
      }
    } finally {
      _openingFallback = false;
    }
  }

  void _updateNavigationStep(Position position) {
    final distanceToDest = Geolocator.distanceBetween(
      position.latitude,
      position.longitude,
      widget.latitude,
      widget.longitude,
    );
    if (distanceToDest < 35) {
      if (_isNavigating && !_arrivedAnnounced) {
        _arrivedAnnounced = true;
        unawaited(_speak('Bạn đã đến nơi. Vị trí của $_title.'));
      }
      return;
    }

    final steps = _route?.steps;
    if (steps == null || steps.isEmpty) return;
    final nextIndex = _currentStepIndex;
    if (nextIndex < steps.length) {
      final step = steps[nextIndex];
      final distance = Geolocator.distanceBetween(
        position.latitude,
        position.longitude,
        step.latitude,
        step.longitude,
      );
      if (distance < 25 && nextIndex + 1 < steps.length) {
        _currentStepIndex = nextIndex + 1;
        if (_isNavigating) {
          final nextStep = steps[_currentStepIndex];
          final stepDist = Geolocator.distanceBetween(
            position.latitude,
            position.longitude,
            nextStep.latitude,
            nextStep.longitude,
          );
          unawaited(
            _speak(
              '${_formatStepDistance(stepDist)}, ${_formatStepInstruction(nextStep)}.',
            ),
          );
        }
      }
    }
  }

  double _calculateBearing(Position position) {
    if (position.heading > 0 && position.heading <= 360) {
      return position.heading;
    }
    final steps = _route?.steps;
    if (steps != null && _currentStepIndex < steps.length) {
      final targetStep = steps[_currentStepIndex];
      return (Geolocator.bearingBetween(
            position.latitude,
            position.longitude,
            targetStep.latitude,
            targetStep.longitude,
          ) +
          360) %
          360;
    }
    return (Geolocator.bearingBetween(
          position.latitude,
          position.longitude,
          widget.latitude,
          widget.longitude,
        ) +
        360) %
        360;
  }

  Future<void> _trackUserCamera(Position position) async {
    final controller = _mapController;
    if (!_styleReady || controller == null) return;
    final bearing = _calculateBearing(position);
    await _syncMarkersOnly();
    await controller.animateCamera(
      CameraUpdate.newCameraPosition(
        CameraPosition(
          target: LatLng(position.latitude, position.longitude),
          zoom: 17.5,
          tilt: 50.0,
          bearing: bearing,
        ),
      ),
    );
  }

  Future<void> _syncMarkersOnly() async {
    final controller = _mapController;
    final position = _position;
    if (!_styleReady || controller == null || position == null) return;
    try {
      await controller.clearCircles();
      await controller.addCircle(
        CircleOptions(
          geometry: LatLng(position.latitude, position.longitude),
          circleRadius: 9,
          circleColor: '#2563EB',
          circleStrokeColor: '#FFFFFF',
          circleStrokeWidth: 3,
        ),
      );
      await controller.addCircle(
        CircleOptions(
          geometry: LatLng(widget.latitude, widget.longitude),
          circleRadius: 11,
          circleColor: '#DC2626',
          circleStrokeColor: '#FFFFFF',
          circleStrokeWidth: 3,
        ),
      );
    } catch (_) {}
  }

  Future<void> _syncMap() async {
    final controller = _mapController;
    final position = _position;
    if (!_styleReady || controller == null || position == null) return;
    try {
      await controller.clearCircles();
      await controller.clearLines();

      // User location marker
      await controller.addCircle(
        CircleOptions(
          geometry: LatLng(position.latitude, position.longitude),
          circleRadius: 8,
          circleColor: '#2563EB',
          circleStrokeColor: '#FFFFFF',
          circleStrokeWidth: 3,
        ),
      );

      // Destination marker
      await controller.addCircle(
        CircleOptions(
          geometry: LatLng(widget.latitude, widget.longitude),
          circleRadius: 10,
          circleColor: '#DC2626',
          circleStrokeColor: '#FFFFFF',
          circleStrokeWidth: 3,
        ),
      );

      // Extract route points or fallback to direct connection line
      final routeCoords = _route?.coordinates;
      final points = (routeCoords != null && routeCoords.length >= 2)
          ? routeCoords
              .map((point) => LatLng(point.latitude, point.longitude))
              .toList(growable: false)
          : <LatLng>[
              LatLng(position.latitude, position.longitude),
              LatLng(widget.latitude, widget.longitude),
            ];

      if (points.length >= 2) {
        // Casing/border line for high contrast
        await controller.addLine(
          LineOptions(
            geometry: points,
            lineColor: '#1E40AF',
            lineWidth: 8,
            lineOpacity: 0.8,
            lineJoin: 'round',
          ),
        );
        // Main high-visibility navigation line
        await controller.addLine(
          LineOptions(
            geometry: points,
            lineColor: '#3B82F6',
            lineWidth: 5,
            lineOpacity: 1.0,
            lineJoin: 'round',
          ),
        );
      }
      if (!_isNavigating) {
        await _fitOverview();
      }
    } catch (_) {}
  }

  Future<void> _fitOverview() async {
    final controller = _mapController;
    final position = _position;
    if (controller == null || position == null) return;
    await controller.animateCamera(
      CameraUpdate.newLatLngBounds(
        LatLngBounds(
          southwest: LatLng(
            position.latitude < widget.latitude
                ? position.latitude
                : widget.latitude,
            position.longitude < widget.longitude
                ? position.longitude
                : widget.longitude,
          ),
          northeast: LatLng(
            position.latitude > widget.latitude
                ? position.latitude
                : widget.latitude,
            position.longitude > widget.longitude
                ? position.longitude
                : widget.longitude,
          ),
        ),
        left: 48,
        top: 120,
        right: 48,
        bottom: 240,
      ),
    );
  }

  void _startInAppNavigation() {
    final pos = _position;
    final distanceToDest = _distanceToDestination;
    if (distanceToDest < 35) {
      setState(() {
        _isNavigating = true;
        _followUser = true;
        _currentStepIndex = (_route?.steps.length ?? 1) - 1;
        _arrivedAnnounced = true;
      });
      if (pos != null) {
        unawaited(_trackUserCamera(pos));
      }
      unawaited(_speak('Bạn đang ở cùng vị trí với $_title.'));
      return;
    }

    setState(() {
      _isNavigating = true;
      _followUser = true;
      _currentStepIndex = 0;
      _arrivedAnnounced = false;
    });
    if (pos != null) {
      unawaited(_trackUserCamera(pos));
    }
    final step = _currentStep;
    final dist = _formatStepDistance(_distanceToCurrentStep);
    final instruction = _formatStepInstruction(step);
    unawaited(_speak('Bắt đầu dẫn đường. $dist, $instruction.'));
  }

  void _stopInAppNavigation() {
    setState(() {
      _isNavigating = false;
      _followUser = false;
    });
    unawaited(_tts.stop());
    unawaited(_syncMap());
  }

  void _recenter() {
    final pos = _position;
    setState(() => _followUser = true);
    if (pos != null) {
      if (_isNavigating) {
        unawaited(_trackUserCamera(pos));
      } else {
        _mapController?.animateCamera(
          CameraUpdate.newCameraPosition(
            CameraPosition(
              target: LatLng(pos.latitude, pos.longitude),
              zoom: 15,
              tilt: 0,
              bearing: 0,
            ),
          ),
        );
      }
    }
  }

  double get _distanceToDestination {
    final pos = _position;
    if (pos == null) return 0;
    return Geolocator.distanceBetween(
      pos.latitude,
      pos.longitude,
      widget.latitude,
      widget.longitude,
    );
  }

  CareRouteStep? get _currentStep {
    final steps = _route?.steps;
    if (steps == null || steps.isEmpty) return null;
    if (_currentStepIndex < steps.length) {
      return steps[_currentStepIndex];
    }
    return steps.last;
  }

  double get _distanceToCurrentStep {
    final pos = _position;
    final step = _currentStep;
    if (pos == null || step == null) return 0;
    return Geolocator.distanceBetween(
      pos.latitude,
      pos.longitude,
      step.latitude,
      step.longitude,
    );
  }

  IconData _stepManeuverIcon(CareRouteStep? step) {
    if (step == null) return Icons.straight_rounded;
    final m = step.maneuver.toLowerCase();
    if (m.contains('uturn') || m.contains('u-turn')) {
      return Icons.u_turn_left_rounded;
    }
    if (m.contains('roundabout')) {
      return Icons.roundabout_right_rounded;
    }
    if (m.contains('arrive') || m.contains('destination')) {
      return Icons.location_on_rounded;
    }
    if (m.contains('right')) {
      return Icons.turn_right_rounded;
    }
    if (m.contains('left')) {
      return Icons.turn_left_rounded;
    }
    return Icons.straight_rounded;
  }

  String _formatStepInstruction(CareRouteStep? step) {
    if (step == null) return 'Đi thẳng theo tuyến đường';
    final m = step.maneuver.toLowerCase();
    final road = step.roadName?.trim();
    final roadSuffix = (road != null && road.isNotEmpty) ? ' vào $road' : '';
    if (m.contains('slight-right') || m.contains('slight_right')) {
      return 'Chếch sang phải$roadSuffix';
    }
    if (m.contains('sharp-right') || m.contains('sharp_right')) {
      return 'Rẽ ngoặt sang phải$roadSuffix';
    }
    if (m.contains('right')) {
      return 'Rẽ phải$roadSuffix';
    }
    if (m.contains('slight-left') || m.contains('slight_left')) {
      return 'Chếch sang trái$roadSuffix';
    }
    if (m.contains('sharp-left') || m.contains('sharp_left')) {
      return 'Rẽ ngoặt sang trái$roadSuffix';
    }
    if (m.contains('left')) {
      return 'Rẽ trái$roadSuffix';
    }
    if (m.contains('uturn') || m.contains('u-turn')) {
      return 'Quay đầu xe';
    }
    if (m.contains('roundabout')) {
      return 'Đi vào vòng xuyến';
    }
    if (m.contains('arrive') || m.contains('destination')) {
      return 'Đến điểm đích';
    }
    return (road != null && road.isNotEmpty)
        ? 'Tiếp tục trên $road'
        : 'Tiếp tục đi thẳng';
  }

  String _formatStepDistance(double meters) {
    if (meters < 30) return 'Trong vài mét';
    if (meters < 1000) return 'Trong ${meters.round()}m';
    return 'Sau ${(meters / 1000).toStringAsFixed(1)}km';
  }

  @override
  Widget build(BuildContext context) {
    final route = _route;
    final isArrived = _distanceToDestination < 35;
    final currentStep = isArrived ? null : _currentStep;
    final stepDistance = isArrived ? 0.0 : _distanceToCurrentStep;

    return Scaffold(
      backgroundColor: _background,
      appBar: AppBar(
        backgroundColor: _background,
        surfaceTintColor: Colors.transparent,
        foregroundColor: _text,
        title: Text(
          _isNavigating ? 'Đang dẫn đường' : 'Dẫn đường',
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontWeight: FontWeight.w700,
          ),
        ),
        actions: [
          IconButton(
            tooltip: _voiceEnabled ? 'Tắt giọng nói' : 'Bật giọng nói',
            onPressed: () {
              setState(() => _voiceEnabled = !_voiceEnabled);
              if (!_voiceEnabled) {
                unawaited(_tts.stop());
              } else {
                unawaited(_speak('Đã bật giọng nói hướng dẫn'));
              }
            },
            icon: Icon(
              _voiceEnabled
                  ? Icons.volume_up_rounded
                  : Icons.volume_off_rounded,
            ),
          ),
          IconButton(
            tooltip: 'Mở Google Maps',
            onPressed: _fallbackToGoogleMaps,
            icon: const Icon(Icons.open_in_new_rounded),
          ),
        ],
      ),
      body: Stack(
        children: [
          if (_effectiveTrackAsiaKey.isNotEmpty && _position != null)
            TrackAsiaMap(
              styleString:
                  'https://maps.track-asia.com/styles/v2/streets.json?key=${Uri.encodeQueryComponent(_effectiveTrackAsiaKey)}',
              initialCameraPosition: CameraPosition(
                target: LatLng(_position!.latitude, _position!.longitude),
                zoom: 13,
              ),
              myLocationEnabled: false,
              onMapCreated: (controller) => _mapController = controller,
              onStyleLoadedCallback: () {
                _styleWatchdog?.cancel();
                _styleReady = true;
                unawaited(_syncMap());
              },
              onCameraTrackingDismissed: () {
                if (_followUser) setState(() => _followUser = false);
              },
            )
          else
            const ColoredBox(color: Color(0xFFF2EAE4)),
          if (_loading)
            const Center(child: CircularProgressIndicator(color: _accent)),

          // Top turn-by-turn banner when navigating
          if (_isNavigating)
            Positioned(
              top: 12,
              left: 16,
              right: 16,
              child: SafeArea(
                child: Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 14,
                  ),
                  decoration: BoxDecoration(
                    color: _surface,
                    borderRadius: BorderRadius.circular(20),
                    boxShadow: const [
                      BoxShadow(
                        color: Color(0x205A463F),
                        blurRadius: 24,
                        offset: Offset(0, 8),
                      ),
                    ],
                  ),
                  child: Row(
                    children: [
                      Container(
                        width: 48,
                        height: 48,
                        decoration: const BoxDecoration(
                          color: Color(0x1FC98C7B),
                          shape: BoxShape.circle,
                        ),
                        child: Icon(
                          isArrived
                              ? Icons.location_on_rounded
                              : _stepManeuverIcon(currentStep),
                          color: _accentDark,
                          size: 28,
                        ),
                      ),
                      const SizedBox(width: 14),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Text(
                              isArrived
                                  ? 'Tại điểm đến'
                                  : _formatStepDistance(stepDistance),
                              style: const TextStyle(
                                fontFamily: 'Lexend',
                                color: _accentDark,
                                fontWeight: FontWeight.w800,
                                fontSize: 16,
                              ),
                            ),
                            Text(
                              isArrived
                                  ? 'Bạn đang ở vị trí của $_title'
                                  : _formatStepInstruction(currentStep),
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(
                                fontFamily: 'Lexend',
                                color: _text,
                                fontWeight: FontWeight.w600,
                                fontSize: 13,
                              ),
                            ),
                          ],
                        ),
                      ),
                      IconButton(
                        tooltip:
                            _voiceEnabled ? 'Tắt giọng nói' : 'Bật giọng nói',
                        onPressed: () {
                          setState(() => _voiceEnabled = !_voiceEnabled);
                          if (!_voiceEnabled) {
                            unawaited(_tts.stop());
                          } else {
                            unawaited(_speak('Đã bật giọng nói'));
                          }
                        },
                        icon: Icon(
                          _voiceEnabled
                              ? Icons.volume_up_rounded
                              : Icons.volume_off_rounded,
                          color: _voiceEnabled ? _accentDark : _muted,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),

          // Floating map buttons on the right
          Positioned(
            right: 16,
            bottom: 180,
            child: SafeArea(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  FloatingActionButton.small(
                    heroTag: 'nav_voice_btn',
                    onPressed: () {
                      setState(() => _voiceEnabled = !_voiceEnabled);
                      if (!_voiceEnabled) {
                        unawaited(_tts.stop());
                      } else {
                        unawaited(_speak('Đã bật giọng nói'));
                      }
                    },
                    backgroundColor: _voiceEnabled ? _surface : const Color(0xFFF2EAE4),
                    foregroundColor: _voiceEnabled ? _accentDark : _muted,
                    tooltip: _voiceEnabled ? 'Tắt giọng nói' : 'Bật giọng nói',
                    child: Icon(
                      _voiceEnabled
                          ? Icons.volume_up_rounded
                          : Icons.volume_off_rounded,
                    ),
                  ),
                  const SizedBox(height: 10),
                  FloatingActionButton.small(
                    heroTag: 'nav_overview_btn',
                    onPressed: _fitOverview,
                    backgroundColor: _surface,
                    foregroundColor: _text,
                    tooltip: 'Toàn cảnh tuyến đường',
                    child: const Icon(Icons.route_rounded),
                  ),
                  const SizedBox(height: 10),
                  FloatingActionButton.small(
                    heroTag: 'nav_recenter_btn',
                    onPressed: _recenter,
                    backgroundColor: _followUser ? _accent : _surface,
                    foregroundColor: _followUser ? Colors.white : _text,
                    tooltip: 'Định vị lại',
                    child: const Icon(Icons.my_location_rounded),
                  ),
                ],
              ),
            ),
          ),

          // Bottom card with navigation controls
          Positioned(
            left: 16,
            right: 16,
            bottom: 16,
            child: SafeArea(
              child: Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: _surface,
                  borderRadius: BorderRadius.circular(28),
                  boxShadow: const [
                    BoxShadow(
                      color: Color(0x185A463F),
                      blurRadius: 32,
                      offset: Offset(0, 12),
                    ),
                  ],
                ),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Container(
                          width: 44,
                          height: 44,
                          decoration: BoxDecoration(
                            color: _isNavigating
                                ? const Color(0x1F2563EB)
                                : const Color(0x1FC98C7B),
                            shape: BoxShape.circle,
                          ),
                          child: Icon(
                            _isNavigating
                                ? Icons.navigation_rounded
                                : Icons.location_on_rounded,
                            color: _isNavigating
                                ? const Color(0xFF2563EB)
                                : _accentDark,
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                _title,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: const TextStyle(
                                  fontFamily: 'Lexend',
                                  color: _text,
                                  fontWeight: FontWeight.w700,
                                  fontSize: 16,
                                ),
                              ),
                              Text(
                                route == null
                                    ? 'Đang chuẩn bị tuyến đường'
                                    : '${(route.distanceMeters / 1000).toStringAsFixed(1)} km • ${route.etaMinutes} phút',
                                style: const TextStyle(
                                  fontFamily: 'Lexend',
                                  color: _muted,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                    if (_error != null) ...[
                      const SizedBox(height: 12),
                      Text(
                        'TrackAsia chưa khả dụng: $_error',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          color: _muted,
                          fontSize: 12,
                        ),
                      ),
                    ],
                    const SizedBox(height: 16),
                    SizedBox(
                      width: double.infinity,
                      height: 50,
                      child: _isNavigating
                          ? FilledButton.icon(
                              key: const Key('stop-navigation-btn'),
                              onPressed: _stopInAppNavigation,
                              style: FilledButton.styleFrom(
                                backgroundColor: _stopRed,
                                shape: const StadiumBorder(),
                              ),
                              icon: const Icon(Icons.close_rounded),
                              label: const Text(
                                'Dừng dẫn đường',
                                style: TextStyle(
                                  fontFamily: 'Lexend',
                                  fontWeight: FontWeight.w700,
                                ),
                              ),
                            )
                          : FilledButton.icon(
                              key: const Key('start-navigation-btn'),
                              onPressed: _startInAppNavigation,
                              style: FilledButton.styleFrom(
                                backgroundColor: _accent,
                                shape: const StadiumBorder(),
                              ),
                              icon: const Icon(Icons.navigation_rounded),
                              label: const Text(
                                'Bắt đầu dẫn đường',
                                style: TextStyle(
                                  fontFamily: 'Lexend',
                                  fontWeight: FontWeight.w700,
                                ),
                              ),
                            ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
