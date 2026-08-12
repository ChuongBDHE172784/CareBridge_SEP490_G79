import 'dart:async';

import 'package:flutter/material.dart';
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
  });

  final double latitude;
  final double longitude;
  final String? label;

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
  static const _trackAsiaKey = String.fromEnvironment(
    'TRACKASIA_MAP_KEY',
    defaultValue: String.fromEnvironment('TRACKASIA_API_KEY'),
  );

  final CareFacilityService _routes = CareFacilityService();
  TrackAsiaMapController? _mapController;
  StreamSubscription<Position>? _positionSubscription;
  Timer? _styleWatchdog;
  Position? _position;
  CareRoute? _route;
  String? _error;
  bool _loading = true;
  bool _styleReady = false;
  bool _openingFallback = false;

  String get _title => widget.label?.trim().isNotEmpty == true
      ? widget.label!.trim()
      : 'Vị trí được chia sẻ';

  @override
  void initState() {
    super.initState();
    unawaited(_prepareNavigation());
  }

  @override
  void dispose() {
    _styleWatchdog?.cancel();
    unawaited(_positionSubscription?.cancel());
    super.dispose();
  }

  Future<void> _prepareNavigation() async {
    try {
      if (_trackAsiaKey.isEmpty) {
        await _fallbackToGoogleMaps();
        if (mounted) setState(() => _loading = false);
        return;
      }
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
      final route = await _routes.getRoute(
        fromLatitude: position.latitude,
        fromLongitude: position.longitude,
        toLatitude: widget.latitude,
        toLongitude: widget.longitude,
      );
      if (!mounted) return;
      setState(() {
        _position = position;
        _route = route;
        _loading = false;
      });
      _positionSubscription =
          Geolocator.getPositionStream(
            locationSettings: const LocationSettings(
              accuracy: LocationAccuracy.bestForNavigation,
              distanceFilter: 10,
            ),
          ).listen((next) {
            if (!mounted) return;
            setState(() => _position = next);
            unawaited(_syncMap());
          });
      _styleWatchdog = Timer(const Duration(seconds: 12), () {
        if (mounted && !_styleReady) unawaited(_fallbackToGoogleMaps());
      });
    } catch (error) {
      if (mounted) {
        setState(() {
          _error = error.toString().replaceFirst('FormatException: ', '');
          _loading = false;
        });
      }
      await _fallbackToGoogleMaps();
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

  Future<void> _syncMap() async {
    final controller = _mapController;
    final position = _position;
    if (!_styleReady || controller == null || position == null) return;
    try {
      await controller.clearCircles();
      await controller.clearLines();
      await controller.addCircle(
        CircleOptions(
          geometry: LatLng(position.latitude, position.longitude),
          circleRadius: 8,
          circleColor: '#2563EB',
          circleStrokeColor: '#FFFFFF',
          circleStrokeWidth: 3,
        ),
      );
      await controller.addCircle(
        CircleOptions(
          geometry: LatLng(widget.latitude, widget.longitude),
          circleRadius: 10,
          circleColor: '#C98C7B',
          circleStrokeColor: '#FFFFFF',
          circleStrokeWidth: 3,
        ),
      );
      final points =
          _route?.coordinates
              .map((point) => LatLng(point.latitude, point.longitude))
              .toList(growable: false) ??
          const <LatLng>[];
      if (points.length >= 2) {
        await controller.addLine(
          LineOptions(
            geometry: points,
            lineColor: '#C98C7B',
            lineWidth: 6,
            lineOpacity: 0.95,
            lineJoin: 'round',
          ),
        );
      }
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
          top: 100,
          right: 48,
          bottom: 230,
        ),
      );
    } catch (_) {
      await _fallbackToGoogleMaps();
    }
  }

  @override
  Widget build(BuildContext context) {
    final route = _route;
    return Scaffold(
      backgroundColor: _background,
      appBar: AppBar(
        backgroundColor: _background,
        surfaceTintColor: Colors.transparent,
        foregroundColor: _text,
        title: const Text(
          'Dẫn đường',
          style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w700),
        ),
        actions: [
          IconButton(
            tooltip: 'Mở Google Maps',
            onPressed: _fallbackToGoogleMaps,
            icon: const Icon(Icons.open_in_new_rounded),
          ),
        ],
      ),
      body: Stack(
        children: [
          if (_trackAsiaKey.isNotEmpty && _position != null)
            TrackAsiaMap(
              styleString:
                  'https://maps.track-asia.com/styles/v2/streets.json?key=${Uri.encodeQueryComponent(_trackAsiaKey)}',
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
            )
          else
            const ColoredBox(color: Color(0xFFF2EAE4)),
          if (_loading)
            const Center(child: CircularProgressIndicator(color: _accent)),
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
                          decoration: const BoxDecoration(
                            color: Color(0x1FC98C7B),
                            shape: BoxShape.circle,
                          ),
                          child: const Icon(
                            Icons.location_on_rounded,
                            color: _accentDark,
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                _title,
                                maxLines: 2,
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
                      child: FilledButton.icon(
                        onPressed: _fallbackToGoogleMaps,
                        style: FilledButton.styleFrom(
                          backgroundColor: _accent,
                          shape: const StadiumBorder(),
                        ),
                        icon: const Icon(Icons.navigation_rounded),
                        label: const Text(
                          'Mở Google Maps',
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
