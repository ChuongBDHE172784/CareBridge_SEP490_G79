import 'dart:async';
import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_tts/flutter_tts.dart';
import 'package:geolocator/geolocator.dart';
import 'package:go_router/go_router.dart';
import 'package:trackasia_gl/trackasia_gl.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../../core/auth/auth_state.dart';
import '../../aiTriage/models/triage_continuation.dart';
import '../../aiTriage/services/triage_continuation_restore_coordinator.dart';
import '../../aiTriage/services/triage_continuation_store.dart';
import '../../aiTriage/services/triage_service.dart';
import '../../privacy/services/privacy_service.dart';
import '../../safety/services/safety_permission_service.dart';
import '../models/care_facility_model.dart';
import '../models/emergency_session_model.dart';
import '../services/care_facility_service.dart';
import '../services/emergency_service.dart';

typedef EmergencyUriLauncher = Future<bool> Function(Uri uri);
typedef LocationConsentProbe = Future<bool> Function();

/// Emergency help remains available when location or the route provider is
/// unavailable. Nearby results are informational and never delay emergency
/// calling or opening the emergency session.
class EmergencyMapScreen extends StatefulWidget {
  const EmergencyMapScreen({
    super.key,
    this.facilityService,
    this.permissionService,
    this.emergencyService,
    this.uriLauncher,
    this.locationConsentProbe,
    this.existingSession,
    this.triageHandoff = false,
    this.stage = 'INFANT',
    this.emergencyDialer,
    this.continuationCoordinator,
  });

  final CareFacilityService? facilityService;
  final SafetyPermissionService? permissionService;
  final EmergencyService? emergencyService;
  final EmergencyUriLauncher? uriLauncher;
  final LocationConsentProbe? locationConsentProbe;
  final EmergencySession? existingSession;
  final bool triageHandoff;
  final String stage;
  final Future<bool> Function()? emergencyDialer;
  final TriageContinuationRestoreCoordinator? continuationCoordinator;

  @override
  State<EmergencyMapScreen> createState() => _EmergencyMapScreenState();
}

class _EmergencyMapScreenState extends State<EmergencyMapScreen> {
  static const _primary = Color(0xFF845143);
  static const _surface = Color(0xFFFFF8F6);
  static const _emergencyNumber = '115';
  static const _trackAsiaMapKey = String.fromEnvironment('TRACKASIA_MAP_KEY');
  static const _supportedStages = {
    'PRECONCEPTION',
    'PREGNANCY',
    'POSTPARTUM',
    'INFANT',
    'TODDLER',
  };

  late final CareFacilityService _facilities =
      widget.facilityService ?? CareFacilityService();
  late final SafetyPermissionService _permissions =
      widget.permissionService ?? SafetyPermissionService();
  late final EmergencyService _emergency =
      widget.emergencyService ?? EmergencyService();
  late final EmergencyUriLauncher _launch =
      widget.uriLauncher ?? ((uri) => launchUrl(uri));
  late final LocationConsentProbe _hasLocationConsent =
      widget.locationConsentProbe ?? _defaultLocationConsentProbe;
  late final TriageContinuationRestoreCoordinator _continuationCoordinator =
      widget.continuationCoordinator ??
      TriageContinuationRestoreCoordinator(
        store: SecureTriageContinuationStore(),
        gateway: TriageService(),
      );
  late final String _stage = widget.stage.trim().toUpperCase();
  late String? _accountId = AuthState.instance.userId;

  Position? _position;
  List<CareFacility> _results = const [];
  CareFacility? _selected;
  CareRoute? _route;
  EmergencySession? _session;
  bool _loading = true;
  bool _sendingFamilyAlert = false;
  bool _familyAlertFailed = false;
  bool _accountChanged = false;
  bool _restoringContinuation = false;
  bool _noticeIsDialFallback = false;
  String? _notice;
  String? _continuationExitError;
  int _loadGeneration = 0;
  int _selectionGeneration = 0;
  int _sessionGeneration = 0;
  int _radiusMeters = 5000;
  String _facilityType = 'hospital';
  String _transportMode = 'DRIVING';
  TrackAsiaMapController? _mapController;
  bool _mapStyleReady = false;
  bool _navigationActive = false;
  int _currentStepIndex = 0;
  StreamSubscription<Position>? _navigationSubscription;
  DateTime? _lastRerouteAt;
  DateTime? _lastNavigationConsentCheck;
  bool _navigationConsentValid = true;
  bool _handlingNavigationPosition = false;
  final FlutterTts _tts = FlutterTts();

  bool get _isTriageHandoff =>
      widget.triageHandoff ||
      widget.existingSession?.triggerSource.trim().toUpperCase() == 'AI_TRIAGE';

  bool _isActiveSession(EmergencySession? session) =>
      session?.status.trim().toUpperCase() == 'ACTIVE';

  @override
  void initState() {
    super.initState();
    if (!_supportedStages.contains(_stage)) {
      throw ArgumentError.value(widget.stage, 'stage', 'Unsupported stage');
    }
    _session = _isActiveSession(widget.existingSession)
        ? widget.existingSession
        : null;
    AuthState.instance.addListener(_handleAuthChanged);
    unawaited(_configureTts());
    _load();
  }

  @override
  void dispose() {
    AuthState.instance.removeListener(_handleAuthChanged);
    unawaited(_navigationSubscription?.cancel());
    unawaited(_tts.stop());
    super.dispose();
  }

  Future<void> _configureTts() async {
    try {
      await _tts.setLanguage('vi-VN');
      await _tts.setSpeechRate(0.48);
      await _tts.setVolume(1);
      await _tts.setAudioAttributesForNavigation();
    } catch (_) {
      // Text guidance remains available when speech is unsupported.
    }
  }

  void _handleAuthChanged() {
    final current = AuthState.instance.userId;
    if (current == _accountId) return;
    _accountId = current;
    unawaited(_stopNavigation());
    ++_loadGeneration;
    ++_selectionGeneration;
    ++_sessionGeneration;
    if (!mounted) return;
    setState(() {
      _accountChanged = true;
      _loading = false;
      _sendingFamilyAlert = false;
      _restoringContinuation = false;
      _position = null;
      _results = const [];
      _selected = null;
      _route = null;
      _session = null;
      _continuationExitError = null;
      _noticeIsDialFallback = false;
      _notice =
          'Phiên đăng nhập đã thay đổi. Dữ liệu khẩn cấp cũ đã được xóa khỏi màn hình.';
    });
  }

  Future<void> _load() async {
    final generation = ++_loadGeneration;
    ++_selectionGeneration;
    if (mounted) {
      setState(() {
        _loading = true;
        _notice = null;
        _noticeIsDialFallback = false;
        _route = null;
      });
    }
    if (!_isActiveSession(_session)) {
      await _ensureEmergencySession(showFeedback: false);
    }
    if (!mounted || generation != _loadGeneration || _accountChanged) return;

    try {
      if (!await _hasLocationConsent()) {
        if (mounted && generation == _loadGeneration) {
          setState(() {
            _position = null;
            _results = const [];
            _selected = null;
            _route = null;
            _loading = false;
            _notice =
                'Chưa có consent chia sẻ vị trí. Bạn vẫn có thể gọi cấp cứu.';
          });
        }
        return;
      }
      if (!mounted || generation != _loadGeneration) return;
      final position = await _permissions.readConsentedLocation();
      if (!mounted || generation != _loadGeneration) return;
      if (position == null) {
        setState(() {
          _position = null;
          _results = const [];
          _selected = null;
          _route = null;
          _loading = false;
          _notice =
              'Không có quyền vị trí. Bạn vẫn có thể gọi cấp cứu hoặc thử lại.';
        });
        return;
      }
      final results = await _facilities.searchNearby(
        latitude: position.latitude,
        longitude: position.longitude,
        radiusMeters: _radiusMeters,
        type: _facilityType,
      );
      if (!mounted || generation != _loadGeneration) return;
      setState(() {
        _position = position;
        _results = results;
        _selected = null;
        _loading = false;
        _notice = results.isEmpty
            ? _radiusMeters == 5000
                  ? 'Không có ${_facilityTypeLabel().toLowerCase()} trong 5 km. Hãy thử mở rộng lên 10 km.'
                  : _radiusMeters == 10000
                  ? 'Không có ${_facilityTypeLabel().toLowerCase()} trong 10 km. Hãy thử mở rộng lên 15 km.'
                  : 'Không có ${_facilityTypeLabel().toLowerCase()} trong 15 km. Hãy gọi 115 khi cần hỗ trợ khẩn cấp.'
            : null;
      });
      await _syncMapAnnotations();
    } catch (_) {
      if (mounted && generation == _loadGeneration) {
        setState(() {
          _loading = false;
          _results = const [];
          _selected = null;
          _route = null;
          _notice = 'Không thể tải cơ sở gần đây. Bạn vẫn có thể gọi cấp cứu.';
        });
      }
    }
  }

  Future<bool> _defaultLocationConsentProbe() async {
    final grants = await PrivacyService.instance.listConsents();
    return grants.any(
      (grant) =>
          grant.isActive &&
          grant.dataType == 'LOCATION' &&
          grant.purpose == 'SHARE' &&
          grant.recipient == 'CAREBRIDGE_SAFETY' &&
          grant.scope == 'SAFETY_EMERGENCY_ALERT',
    );
  }

  Future<void> _loadRoute(
    CareFacility facility,
    int selectionGeneration,
  ) async {
    final position = _position;
    if (position == null || !facility.hasCoordinates) return;
    try {
      final route = await _facilities.getRoute(
        fromLatitude: position.latitude,
        fromLongitude: position.longitude,
        toLatitude: facility.latitude!,
        toLongitude: facility.longitude!,
        transportMode: _transportMode,
      );
      if (mounted &&
          selectionGeneration == _selectionGeneration &&
          identical(_selected, facility)) {
        setState(() => _route = route);
        await _syncMapAnnotations();
      }
    } catch (_) {
      // Keep the facility selected so the TrackAsia route can be retried.
    }
  }

  Future<void> _select(CareFacility facility) async {
    if (_navigationActive) await _stopNavigation();
    final selectionGeneration = ++_selectionGeneration;
    setState(() {
      _selected = facility;
      _route = null;
    });
    var detail = facility;
    if (facility.facilityId != null) {
      try {
        detail = await _facilities.getFacility(facility.facilityId!);
        if (!mounted || selectionGeneration != _selectionGeneration) return;
        setState(() {
          _selected = detail;
          final index = _results.indexOf(facility);
          if (index >= 0) {
            _results = [..._results]..[index] = detail;
          }
        });
      } catch (_) {
        // The nearby item remains usable when detail refresh is unavailable.
      }
    }
    if (selectionGeneration == _selectionGeneration) {
      await _loadRoute(detail, selectionGeneration);
    }
  }

  Future<void> _ensureEmergencySession({bool showFeedback = true}) async {
    if (_sendingFamilyAlert || _accountChanged) return;
    final generation = ++_sessionGeneration;
    final accountId = AuthState.instance.userId;
    if (mounted) setState(() => _sendingFamilyAlert = true);
    try {
      final session = _isTriageHandoff || _isActiveSession(_session)
          ? await _emergency.getActive()
          : await _emergency.openFlow(triggerSource: 'MANUAL');
      if (!mounted ||
          generation != _sessionGeneration ||
          AuthState.instance.userId != accountId) {
        return;
      }
      setState(() {
        _session = _isActiveSession(session) ? session : null;
        _familyAlertFailed = session == null;
      });
      if (showFeedback && session != null) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
              'Yêu cầu hỗ trợ đã được gửi; thông báo gia đình đang được xử lý.',
            ),
          ),
        );
      }
    } catch (_) {
      if (!mounted ||
          generation != _sessionGeneration ||
          AuthState.instance.userId != accountId) {
        return;
      }
      setState(() => _familyAlertFailed = true);
      if (showFeedback) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Không thể gửi báo động. Hãy thử lại.')),
        );
      }
    } finally {
      if (mounted &&
          generation == _sessionGeneration &&
          AuthState.instance.userId == accountId) {
        setState(() => _sendingFamilyAlert = false);
      }
    }
  }

  Future<void> _leaveEmergency() async {
    if (!_isTriageHandoff) {
      if (mounted) Navigator.of(context).maybePop();
      return;
    }
    if (_restoringContinuation) return;
    final userId = AuthState.instance.userId;
    if (userId == null || userId.isEmpty) {
      if (mounted) {
        setState(() {
          _continuationExitError =
              'Không thể xác nhận tài khoản để khôi phục điểm quay lại.';
        });
      }
      return;
    }
    setState(() {
      _restoringContinuation = true;
      _continuationExitError = null;
    });
    try {
      final decision = await _continuationCoordinator.restoreForUser(
        userId,
        resumeRedEmergency: false,
      );
      if (!mounted || AuthState.instance.userId != userId) return;
      if (decision.requiresRetry) {
        setState(() {
          _continuationExitError =
              'Chưa thể khôi phục điểm quay lại. Hãy thử lại hoặc về trang chủ an toàn.';
        });
        return;
      }
      final isFamily =
          (AuthState.instance.role ?? '').trim().toUpperCase() == 'FAMILY';
      final location = switch (decision.destination) {
        TriageContinuationDestination.motherJourney when !isFamily =>
          '/mother-home?tab=1&triageReturn=${Uri.encodeQueryComponent(_session?.sessionId ?? DateTime.now().microsecondsSinceEpoch.toString())}',
        TriageContinuationDestination.babyProfile
            when !isFamily && decision.originReferenceId != null =>
          '/babies/detail/${Uri.encodeComponent(decision.originReferenceId!)}',
        TriageContinuationDestination.safeDashboard ||
        TriageContinuationDestination.none => isFamily ? '/' : '/mother-home',
        _ => null,
      };
      if (location == null) {
        setState(() {
          _continuationExitError =
              'Điểm quay lại không còn khả dụng. Hãy thử lại hoặc về trang chủ an toàn.';
        });
        return;
      }
      context.go(
        location,
        extra:
            !isFamily &&
                (decision.destination ==
                        TriageContinuationDestination.motherJourney ||
                    decision.destination ==
                        TriageContinuationDestination.babyProfile)
            ? TriageContinuationArrival(
                userId: userId,
                decision: decision,
                coordinator: _continuationCoordinator,
              )
            : null,
      );
    } catch (_) {
      if (!mounted || AuthState.instance.userId != userId) return;
      setState(() {
        _continuationExitError =
            'Chưa thể khôi phục điểm quay lại. Hãy thử lại hoặc về trang chủ an toàn.';
      });
    } finally {
      if (mounted && AuthState.instance.userId == userId) {
        setState(() => _restoringContinuation = false);
      }
    }
  }

  Future<void> _call(String? phone) async {
    try {
      final launched = phone == null && widget.emergencyDialer != null
          ? await widget.emergencyDialer!()
          : await _launch(Uri(scheme: 'tel', path: phone ?? _emergencyNumber));
      if (!launched && mounted) _showManualDialFallback(phone);
    } catch (_) {
      if (mounted) _showManualDialFallback(phone);
    }
  }

  void _showManualDialFallback(String? phone) {
    setState(() {
      _noticeIsDialFallback = true;
      _notice = phone == null
          ? 'Không thể mở ứng dụng gọi. Hãy tự gọi 115 hoặc nhờ người bên cạnh gọi giúp.'
          : 'Không thể mở ứng dụng gọi. Hãy tự nhập số $phone để gọi cơ sở y tế.';
    });
  }

  Future<void> _navigate(CareFacility facility) async {
    if (!facility.hasCoordinates || _position == null) return;
    if (_navigationActive) {
      await _stopNavigation();
      return;
    }
    if (_route == null) {
      await _loadRoute(facility, _selectionGeneration);
    }
    if (_route == null || _route!.steps.isEmpty) {
      if (mounted) {
        setState(
          () => _notice = 'TrackAsia chưa trả về chỉ dẫn cho tuyến này.',
        );
      }
      return;
    }
    if (!await _hasLocationConsent()) {
      if (mounted) {
        setState(() {
          _notice =
              'Consent vị trí không còn hiệu lực. Bạn vẫn có thể gọi cấp cứu 115.';
        });
      }
      return;
    }
    _navigationConsentValid = true;
    _lastNavigationConsentCheck = DateTime.now();
    _currentStepIndex = 0;
    setState(() => _navigationActive = true);
    await _speakCurrentStep();
    await _navigationSubscription?.cancel();
    _navigationSubscription =
        Geolocator.getPositionStream(
          locationSettings: const LocationSettings(
            accuracy: LocationAccuracy.bestForNavigation,
            distanceFilter: 10,
          ),
        ).listen(
          (position) => unawaited(_handleNavigationPosition(position)),
          onError: (_) => unawaited(_handleNavigationFailure()),
          onDone: () => unawaited(_handleNavigationFailure()),
        );
  }

  Future<void> _handleNavigationFailure() async {
    if (!_navigationActive) return;
    await _stopNavigation();
    if (mounted) {
      setState(() {
        _notice =
            'Đã dừng dẫn đường vì không còn nhận được vị trí. Gọi 115 vẫn luôn khả dụng.';
      });
    }
  }

  Future<void> _stopNavigation() async {
    await _navigationSubscription?.cancel();
    _navigationSubscription = null;
    await _tts.stop();
    if (mounted) setState(() => _navigationActive = false);
  }

  Future<void> _handleNavigationPosition(Position position) async {
    if (!_navigationActive || !mounted || _handlingNavigationPosition) return;
    _handlingNavigationPosition = true;
    try {
      if (!await _navigationHasActiveConsent()) {
        await _stopNavigation();
        if (mounted) {
          setState(() {
            _position = null;
            _results = const [];
            _selected = null;
            _route = null;
            _notice =
                'Đã dừng dẫn đường vì consent vị trí không còn hiệu lực. Gọi 115 vẫn luôn khả dụng.';
          });
        }
        return;
      }
      if (!_navigationActive || !mounted) return;
      setState(() => _position = position);
      final route = _route;
      if (route == null || route.steps.isEmpty) return;

      var nearestStepIndex = _currentStepIndex;
      var nearestStepDistance = double.infinity;
      for (var index = _currentStepIndex; index < route.steps.length; index++) {
        final candidate = route.steps[index];
        final distance = Geolocator.distanceBetween(
          position.latitude,
          position.longitude,
          candidate.latitude,
          candidate.longitude,
        );
        if (distance < nearestStepDistance) {
          nearestStepDistance = distance;
          nearestStepIndex = index;
        }
      }
      if (nearestStepIndex > _currentStepIndex) {
        setState(() => _currentStepIndex = nearestStepIndex);
        await _speakCurrentStep();
      }
      if (_currentStepIndex == route.steps.length - 1 &&
          nearestStepDistance <= 35) {
        await _stopNavigation();
        return;
      }
      if (route.coordinates.isEmpty) {
        await _reroute(position);
      } else {
        final distanceToRoute = _minimumDistanceToRoute(
          position,
          route.coordinates,
        );
        if (distanceToRoute > 80) await _reroute(position);
      }
      await _syncMapAnnotations(fitCamera: false);
    } finally {
      _handlingNavigationPosition = false;
    }
  }

  Future<bool> _navigationHasActiveConsent() async {
    final now = DateTime.now();
    if (_lastNavigationConsentCheck != null &&
        now.difference(_lastNavigationConsentCheck!) <
            const Duration(seconds: 30)) {
      return _navigationConsentValid;
    }
    _lastNavigationConsentCheck = now;
    try {
      _navigationConsentValid = await _hasLocationConsent();
    } catch (_) {
      _navigationConsentValid = false;
    }
    return _navigationConsentValid;
  }

  Future<void> _reroute(Position position) async {
    final now = DateTime.now();
    if (_lastRerouteAt != null &&
        now.difference(_lastRerouteAt!) < const Duration(seconds: 20)) {
      return;
    }
    final facility = _selected;
    if (facility == null || !facility.hasCoordinates) return;
    final generation = _selectionGeneration;
    final transportMode = _transportMode;
    _lastRerouteAt = now;
    try {
      final route = await _facilities.getRoute(
        fromLatitude: position.latitude,
        fromLongitude: position.longitude,
        toLatitude: facility.latitude!,
        toLongitude: facility.longitude!,
        transportMode: _transportMode,
      );
      if (!mounted ||
          !_navigationActive ||
          generation != _selectionGeneration ||
          !identical(facility, _selected) ||
          transportMode != _transportMode) {
        return;
      }
      setState(() {
        _route = route;
        _currentStepIndex = 0;
      });
      await _speakCurrentStep(prefix: 'Đang tính lại tuyến đường. ');
    } catch (_) {
      // Keep the last usable TrackAsia route and retry only after debounce.
    }
  }

  Future<void> _speakCurrentStep({String prefix = ''}) async {
    final route = _route;
    if (route == null || route.steps.isEmpty) return;
    final step =
        route.steps[_currentStepIndex.clamp(0, route.steps.length - 1)];
    final road = step.roadName?.trim();
    final instruction = _vietnameseManeuver(step.maneuver);
    try {
      await _tts.stop();
      await _tts.speak(
        '$prefix$instruction${road == null || road.isEmpty ? '' : ' vào $road'}',
      );
    } catch (_) {
      // Visual guidance remains active when TTS fails at runtime.
    }
  }

  String _vietnameseManeuver(String maneuver) {
    final value = maneuver.toLowerCase();
    if (value.contains('left')) return 'Rẽ trái';
    if (value.contains('right')) return 'Rẽ phải';
    if (value.contains('uturn')) return 'Quay đầu';
    if (value.contains('arrive')) return 'Bạn đã đến nơi';
    if (value.contains('depart')) return 'Bắt đầu đi';
    if (value.contains('roundabout')) return 'Đi vào vòng xuyến';
    return 'Tiếp tục đi thẳng';
  }

  String _facilityTypeLabel() => switch (_facilityType) {
    'clinic' => 'Phòng khám',
    'health_station' => 'Trạm y tế',
    _ => 'Bệnh viện',
  };

  double _minimumDistanceToRoute(
    Position position,
    List<CareRouteCoordinate> coordinates,
  ) {
    if (coordinates.length == 1) {
      return Geolocator.distanceBetween(
        position.latitude,
        position.longitude,
        coordinates.first.latitude,
        coordinates.first.longitude,
      );
    }
    const earthRadius = 6371000.0;
    final originLatRadians = position.latitude * 3.141592653589793 / 180;
    var minimum = double.infinity;
    for (var index = 0; index < coordinates.length - 1; index++) {
      final start = coordinates[index];
      final end = coordinates[index + 1];
      final startX =
          _normalizedLongitudeDelta(start.longitude - position.longitude) *
          3.141592653589793 /
          180 *
          earthRadius *
          math.cos(originLatRadians);
      final startY =
          (start.latitude - position.latitude) *
          3.141592653589793 /
          180 *
          earthRadius;
      final endX =
          _normalizedLongitudeDelta(end.longitude - position.longitude) *
          3.141592653589793 /
          180 *
          earthRadius *
          math.cos(originLatRadians);
      final endY =
          (end.latitude - position.latitude) *
          3.141592653589793 /
          180 *
          earthRadius;
      final segmentX = endX - startX;
      final segmentY = endY - startY;
      final lengthSquared = segmentX * segmentX + segmentY * segmentY;
      final projection = lengthSquared == 0
          ? 0.0
          : ((-startX * segmentX - startY * segmentY) / lengthSquared).clamp(
              0.0,
              1.0,
            );
      final closestX = startX + projection * segmentX;
      final closestY = startY + projection * segmentY;
      final distance = math.sqrt(closestX * closestX + closestY * closestY);
      if (distance < minimum) minimum = distance;
    }
    return minimum;
  }

  double _normalizedLongitudeDelta(double value) {
    if (value > 180) return value - 360;
    if (value < -180) return value + 360;
    return value;
  }

  Future<void> _changeRadius(int radius) async {
    if (radius == _radiusMeters || _loading) return;
    setState(() => _radiusMeters = radius);
    await _stopNavigation();
    await _load();
  }

  Future<void> _changeFacilityType(String type) async {
    if (type == _facilityType || _loading) return;
    setState(() => _facilityType = type);
    await _stopNavigation();
    await _load();
  }

  Future<void> _changeTransportMode(String mode) async {
    if (mode == _transportMode) return;
    setState(() => _transportMode = mode);
    await _stopNavigation();
    final selected = _selected;
    if (selected != null) {
      await _loadRoute(selected, ++_selectionGeneration);
    }
  }

  String _distance(CareFacility facility) {
    final meters = identical(_selected, facility) && _route != null
        ? _route!.distanceMeters
        : facility.distanceMeters?.toDouble();
    if (meters == null) return 'Khoảng cách chưa xác định';
    return meters >= 1000
        ? '${(meters / 1000).toStringAsFixed(1)} km'
        : '${meters.round()} m';
  }

  void _onMapCreated(TrackAsiaMapController controller) {
    _mapController = controller;
    controller.onCircleTapped.add((circle) {
      final index = circle.data?['facilityIndex'];
      if (index is int && index >= 0 && index < _results.length) {
        unawaited(_select(_results[index]));
      }
    });
  }

  Future<void> _syncMapAnnotations({bool fitCamera = true}) async {
    final controller = _mapController;
    if (!_mapStyleReady || controller == null) return;
    try {
      await controller.clearCircles();
      await controller.clearLines();
      final position = _position;
      if (position != null) {
        await controller.addCircle(
          CircleOptions(
            geometry: LatLng(position.latitude, position.longitude),
            circleRadius: 8,
            circleColor: '#2563EB',
            circleStrokeColor: '#FFFFFF',
            circleStrokeWidth: 3,
          ),
        );
      }
      for (var index = 0; index < _results.length; index++) {
        final facility = _results[index];
        if (!facility.hasCoordinates) continue;
        await controller.addCircle(
          CircleOptions(
            geometry: LatLng(facility.latitude!, facility.longitude!),
            circleRadius: identical(facility, _selected) ? 10 : 7,
            circleColor: identical(facility, _selected) ? '#845143' : '#DC2626',
            circleStrokeColor: '#FFFFFF',
            circleStrokeWidth: 2,
          ),
          {'facilityIndex': index},
        );
      }
      final coordinates = _route?.coordinates ?? const [];
      if (coordinates.length >= 2) {
        await controller.addLine(
          LineOptions(
            geometry: coordinates
                .map((point) => LatLng(point.latitude, point.longitude))
                .toList(growable: false),
            lineColor: '#845143',
            lineWidth: 6,
            lineOpacity: 0.9,
            lineJoin: 'round',
          ),
        );
      }
      if (fitCamera) await _fitMapCamera();
    } catch (_) {
      // The emergency list and call actions remain usable if map rendering fails.
    }
  }

  Future<void> _fitMapCamera() async {
    final controller = _mapController;
    final position = _position;
    if (controller == null || position == null) return;
    final points = <LatLng>[LatLng(position.latitude, position.longitude)];
    final routeCoordinates = _route?.coordinates ?? const [];
    if (routeCoordinates.isNotEmpty) {
      points.addAll(
        routeCoordinates.map(
          (point) => LatLng(point.latitude, point.longitude),
        ),
      );
    } else {
      points.addAll(
        _results
            .where((facility) => facility.hasCoordinates)
            .map((facility) => LatLng(facility.latitude!, facility.longitude!)),
      );
    }
    if (points.length == 1) {
      await controller.animateCamera(
        CameraUpdate.newLatLngZoom(points.first, 13),
      );
      return;
    }
    var minLat = points.first.latitude;
    var maxLat = minLat;
    var minLng = points.first.longitude;
    var maxLng = minLng;
    for (final point in points.skip(1)) {
      if (point.latitude < minLat) minLat = point.latitude;
      if (point.latitude > maxLat) maxLat = point.latitude;
      if (point.longitude < minLng) minLng = point.longitude;
      if (point.longitude > maxLng) maxLng = point.longitude;
    }
    await controller.animateCamera(
      CameraUpdate.newLatLngBounds(
        LatLngBounds(
          southwest: LatLng(minLat, minLng),
          northeast: LatLng(maxLat, maxLng),
        ),
        left: 40,
        top: 40,
        right: 40,
        bottom: 40,
      ),
    );
  }

  Widget _buildTrackAsiaMap() {
    final position = _position;
    if (_trackAsiaMapKey.isEmpty || position == null) {
      return Container(
        key: const Key('trackasia-map-unavailable'),
        color: const Color(0xFFF0E8E5),
        alignment: Alignment.center,
        child: Text(
          position == null
              ? 'Đang chờ vị trí để mở TrackAsia Map'
              : 'Thiếu TRACKASIA_MAP_KEY cho bản đồ TrackAsia',
          textAlign: TextAlign.center,
        ),
      );
    }
    return TrackAsiaMap(
      key: const Key('trackasia-map'),
      styleString:
          'https://maps.track-asia.com/styles/v2/streets.json?key=${Uri.encodeQueryComponent(_trackAsiaMapKey)}',
      initialCameraPosition: CameraPosition(
        target: LatLng(position.latitude, position.longitude),
        zoom: 13,
      ),
      myLocationEnabled: true,
      myLocationTrackingMode: _navigationActive
          ? MyLocationTrackingMode.trackingGps
          : MyLocationTrackingMode.none,
      myLocationRenderMode: _navigationActive
          ? MyLocationRenderMode.gps
          : MyLocationRenderMode.normal,
      onMapCreated: _onMapCreated,
      onStyleLoadedCallback: () {
        _mapStyleReady = true;
        unawaited(_syncMapAnnotations());
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _surface,
      appBar: AppBar(
        leading: IconButton(
          tooltip: 'Quay lại',
          onPressed: _restoringContinuation ? null : _leaveEmergency,
          icon: const Icon(Icons.arrow_back),
        ),
        title: const Text('Cơ sở y tế gần đây'),
        backgroundColor: _surface,
        foregroundColor: _primary,
      ),
      body: Column(
        children: [
          if (_loading)
            const LinearProgressIndicator(key: Key('nearby-loading')),
          if (_session != null)
            const MaterialBanner(
              content: Text(
                'Phiên hỗ trợ khẩn cấp đang hoạt động; yêu cầu thông báo đang được xử lý.',
              ),
              actions: [SizedBox.shrink()],
            ),
          if (_familyAlertFailed && _isTriageHandoff)
            MaterialBanner(
              key: const Key('emergency-session-status'),
              content: const Text(
                'Không thể xác nhận phiên hỗ trợ khẩn cấp. Vui lòng thử lại.',
              ),
              actions: [
                TextButton(
                  key: const Key('emergency-session-retry'),
                  onPressed: _sendingFamilyAlert
                      ? null
                      : () => _ensureEmergencySession(showFeedback: false),
                  child: const Text('Thử lại'),
                ),
              ],
            ),
          if (_continuationExitError != null)
            MaterialBanner(
              key: const Key('emergency-continuation-exit-error'),
              content: Text(_continuationExitError!),
              actions: [
                TextButton(
                  key: const Key('emergency-continuation-exit-retry'),
                  onPressed: _restoringContinuation ? null : _leaveEmergency,
                  child: const Text('Thử lại'),
                ),
                TextButton(
                  key: const Key('emergency-continuation-safe-dashboard'),
                  onPressed: _restoringContinuation
                      ? null
                      : () => context.go(
                          (AuthState.instance.role ?? '')
                                      .trim()
                                      .toUpperCase() ==
                                  'FAMILY'
                              ? '/'
                              : '/mother-home',
                        ),
                  child: const Text('Về trang chủ'),
                ),
              ],
            ),
          if (_notice != null)
            MaterialBanner(
              key: const Key('nearby-notice'),
              content: Text(_notice!),
              actions: [
                if (!_accountChanged && !_noticeIsDialFallback)
                  TextButton(onPressed: _load, child: const Text('Thử lại')),
                if (_accountChanged || _noticeIsDialFallback)
                  const SizedBox.shrink(),
              ],
            ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: SizedBox(
              width: double.infinity,
              child: FilledButton.icon(
                key: Key(
                  const {
                        'PRECONCEPTION',
                        'PREGNANCY',
                        'POSTPARTUM',
                      }.contains(_stage)
                      ? 'emergency-maternal-call-115'
                      : 'emergency-call',
                ),
                onPressed: () => _call(null),
                style: FilledButton.styleFrom(backgroundColor: Colors.red),
                icon: const Icon(Icons.call),
                label: const Text('Gọi cấp cứu 115'),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: SizedBox(
              width: double.infinity,
              child: KeyedSubtree(
                key: const Key('emergency-family-alert'),
                child: OutlinedButton.icon(
                  key: const Key('family-alert'),
                  onPressed: _sendingFamilyAlert || _accountChanged
                      ? null
                      : () => _ensureEmergencySession(),
                  icon: _sendingFamilyAlert
                      ? const SizedBox.square(
                          dimension: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : Icon(
                          _familyAlertFailed ? Icons.refresh : Icons.campaign,
                        ),
                  label: Text(
                    _familyAlertFailed
                        ? 'Thử gửi lại báo động gia đình'
                        : 'Báo động gia đình',
                  ),
                ),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 8, 12, 0),
            child: SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                children: [
                  const Text('Bán kính: '),
                  for (final radius in const [5000, 10000, 15000])
                    Padding(
                      padding: const EdgeInsets.only(right: 6),
                      child: ChoiceChip(
                        key: Key('radius-$radius'),
                        label: Text('${radius ~/ 1000} km'),
                        selected: _radiusMeters == radius,
                        onSelected: (_) => _changeRadius(radius),
                      ),
                    ),
                ],
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 4, 12, 0),
            child: SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                children: [
                  const Text('Cơ sở: '),
                  for (final entry in const {
                    'hospital': 'Bệnh viện',
                    'clinic': 'Phòng khám',
                    'health_station': 'Trạm y tế',
                  }.entries)
                    Padding(
                      padding: const EdgeInsets.only(right: 6),
                      child: ChoiceChip(
                        key: Key('facility-type-${entry.key}'),
                        label: Text(entry.value),
                        selected: _facilityType == entry.key,
                        onSelected: (_) => _changeFacilityType(entry.key),
                      ),
                    ),
                ],
              ),
            ),
          ),
          Expanded(
            child: Column(
              children: [
                Flexible(
                  flex: 2,
                  child: SizedBox.expand(child: _buildTrackAsiaMap()),
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 12),
                  child: Row(
                    children: [
                      const Text('Phương tiện: '),
                      Expanded(
                        child: SegmentedButton<String>(
                          key: const Key('transport-mode'),
                          segments: const [
                            ButtonSegment(
                              value: 'DRIVING',
                              icon: Icon(Icons.directions_car),
                              label: Text('Ô tô'),
                            ),
                            ButtonSegment(
                              value: 'MOTORCYCLE',
                              icon: Icon(Icons.two_wheeler),
                              label: Text('Xe máy'),
                            ),
                            ButtonSegment(
                              value: 'WALKING',
                              icon: Icon(Icons.directions_walk),
                              label: Text('Đi bộ'),
                            ),
                          ],
                          selected: {_transportMode},
                          showSelectedIcon: false,
                          onSelectionChanged: (selection) =>
                              _changeTransportMode(selection.first),
                        ),
                      ),
                    ],
                  ),
                ),
                Expanded(
                  flex: 3,
                  child: _results.isEmpty
                      ? const Center(
                          child: Icon(Icons.local_hospital_outlined, size: 56),
                        )
                      : SingleChildScrollView(
                          key: const Key('nearby-list'),
                          padding: const EdgeInsets.all(12),
                          child: Column(
                            children: [
                              for (
                                var index = 0;
                                index < _results.length;
                                index++
                              )
                                Card(
                                  child: ListTile(
                                    key: Key('facility-$index'),
                                    selected: identical(
                                      _selected,
                                      _results[index],
                                    ),
                                    title: Text(_results[index].name),
                                    subtitle: Text(
                                      [
                                        if (_results[index]
                                                .address
                                                ?.isNotEmpty ==
                                            true)
                                          _results[index].address!,
                                        _distance(_results[index]),
                                        _results[index].sourceLabel,
                                        _results[index].verificationLabel,
                                      ].join('\n'),
                                    ),
                                    isThreeLine: true,
                                    onTap: () => _select(_results[index]),
                                  ),
                                ),
                            ],
                          ),
                        ),
                ),
              ],
            ),
          ),
          if (_selected != null) _buildSelected(_selected!),
        ],
      ),
    );
  }

  Widget _buildSelected(CareFacility facility) {
    return SafeArea(
      top: false,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(facility.name, style: Theme.of(context).textTheme.titleMedium),
            if (_route != null)
              Text('ETA ${_route!.etaMinutes} phút · ${_distance(facility)}'),
            if (_navigationActive && _route?.steps.isNotEmpty == true)
              Text(
                () {
                  final step =
                      _route!.steps[_currentStepIndex.clamp(
                        0,
                        _route!.steps.length - 1,
                      )];
                  return '${_vietnameseManeuver(step.maneuver)} · ${step.distanceMeters} m';
                }(),
                key: const Key('navigation-instruction'),
                style: const TextStyle(fontWeight: FontWeight.w700),
              ),
            const SizedBox(height: 8),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    key: const Key('facility-call'),
                    onPressed: facility.phone == null
                        ? null
                        : () => _call(facility.phone),
                    icon: const Icon(Icons.call),
                    label: const Text('Gọi cơ sở'),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: FilledButton.icon(
                    key: const Key('facility-navigate'),
                    onPressed: facility.hasCoordinates
                        ? () => _navigate(facility)
                        : null,
                    icon: Icon(
                      _navigationActive ? Icons.stop_circle : Icons.navigation,
                    ),
                    label: Text(
                      _navigationActive
                          ? 'Dừng dẫn đường'
                          : 'Bắt đầu dẫn đường',
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
