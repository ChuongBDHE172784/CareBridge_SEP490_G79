import 'dart:async';
import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_tts/flutter_tts.dart';
import 'package:geolocator/geolocator.dart';
import 'package:go_router/go_router.dart';
import 'package:trackasia_gl/trackasia_gl.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../../core/auth/auth_state.dart';
import '../../../core/network/api_client.dart';
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
typedef LocationConsentGrant =
    Future<void> Function({
      required String dataType,
      required String purpose,
      required String recipient,
      required String scope,
    });
typedef TrackAsiaMapRenderer =
    Widget Function({
      required Key key,
      required String styleString,
      required CameraPosition initialCameraPosition,
      required bool myLocationEnabled,
      required void Function(TrackAsiaMapController? controller) onMapCreated,
      required VoidCallback onStyleLoaded,
    });
typedef MapAnnotationSynchronizer =
    Future<void> Function({
      required Position position,
      required List<CareFacility> facilities,
      CareRoute? route,
    });

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
    this.locationConsentGrant,
    this.existingSession,
    this.triageHandoff = false,
    this.stage = 'INFANT',
    this.emergencyDialer,
    this.continuationCoordinator,
    this.mapRenderer,
    this.mapStyleLoadTimeout = const Duration(seconds: 12),
    this.trackAsiaMapKey,
    this.annotationSynchronizer,
  });

  final CareFacilityService? facilityService;
  final SafetyPermissionService? permissionService;
  final EmergencyService? emergencyService;
  final EmergencyUriLauncher? uriLauncher;
  final LocationConsentProbe? locationConsentProbe;
  final LocationConsentGrant? locationConsentGrant;
  final EmergencySession? existingSession;
  final bool triageHandoff;
  final String stage;
  final Future<bool> Function()? emergencyDialer;
  final TriageContinuationRestoreCoordinator? continuationCoordinator;
  final TrackAsiaMapRenderer? mapRenderer;
  final Duration mapStyleLoadTimeout;
  final String? trackAsiaMapKey;
  final MapAnnotationSynchronizer? annotationSynchronizer;

  @override
  State<EmergencyMapScreen> createState() => _EmergencyMapScreenState();
}

class _EmergencyMapScreenState extends State<EmergencyMapScreen> {
  static const _surface = Color(0xFFFFF8F6);
  static const _emergencyNumber = '115';
  static const _configuredTrackAsiaKey = String.fromEnvironment(
    'TRACKASIA_MAP_KEY',
    defaultValue: String.fromEnvironment('TRACKASIA_API_KEY'),
  );

  String get _effectiveTrackAsiaKey {
    final overrideKey = widget.trackAsiaMapKey;
    if (overrideKey != null) return overrideKey;
    if (_configuredTrackAsiaKey.isNotEmpty) {
      return _configuredTrackAsiaKey;
    }
    if (WidgetsBinding.instance.runtimeType.toString().contains('Test')) {
      return '';
    }
    return 'd3e34fdc69a0d31780225041ffc88f4d4f';
  }

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
  late final LocationConsentGrant _grantLocationConsent =
      widget.locationConsentGrant ?? _defaultLocationConsentGrant;
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
  bool _sharingLocation = false;
  bool _locationShareFailed = false;
  bool _locationShareSent = false;
  bool _accountChanged = false;
  bool _restoringContinuation = false;
  bool _noticeIsDialFallback = false;
  bool _locationConsentRequired = false;
  bool _locationConsentDialogOpen = false;
  bool _grantingLocationConsent = false;
  bool _panelCollapsed = false;
  String? _notice;
  String? _continuationExitError;
  int _loadGeneration = 0;
  int _selectionGeneration = 0;
  int _sessionGeneration = 0;
  int _locationShareGeneration = 0;
  int _radiusMeters = 5000;
  String _facilityType = 'hospital';
  final String _transportMode = 'DRIVING';
  TrackAsiaMapController? _mapController;
  bool _mapStyleReady = false;
  bool _mapStyleFailed = false;
  int _mapGeneration = 0;
  Timer? _mapStyleTimer;
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
    _mapStyleTimer?.cancel();
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
    ++_locationShareGeneration;
    _resetMapRendererState();
    if (!mounted) return;
    setState(() {
      _accountChanged = true;
      _loading = false;
      _sendingFamilyAlert = false;
      _sharingLocation = false;
      _locationShareFailed = false;
      _locationShareSent = false;
      _restoringContinuation = false;
      _position = null;
      _results = const [];
      _selected = null;
      _route = null;
      _session = null;
      _continuationExitError = null;
      _noticeIsDialFallback = false;
      _locationConsentRequired = false;
      _grantingLocationConsent = false;
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
        _locationConsentRequired = false;
        _route = null;
      });
    }
    // The manual route is the nearby-care map, not an emergency trigger.
    // Only a triage handoff may create/reconcile an emergency session.
    if (_isTriageHandoff && !_isActiveSession(_session)) {
      await _ensureEmergencySession(showFeedback: false);
    }
    if (!mounted || generation != _loadGeneration || _accountChanged) return;

    try {
      if (!await _hasLocationConsent()) {
        if (mounted && generation == _loadGeneration) {
          _resetMapRendererState();
          setState(() {
            _position = null;
            _results = const [];
            _selected = null;
            _route = null;
            _loading = false;
            _locationConsentRequired = true;
            _notice =
                'CareBridge cần sự đồng ý của bạn để dùng vị trí hiện tại tìm cơ sở y tế gần đây. Bạn vẫn có thể gọi cấp cứu 115.';
          });
        }
        return;
      }
      if (!mounted || generation != _loadGeneration) return;
      setState(() => _locationConsentRequired = false);
      final position = await _permissions.readConsentedLocation();
      if (!mounted || generation != _loadGeneration) return;
      if (position == null) {
        _resetMapRendererState();
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
      _armMapStyleWatchdog(_mapGeneration);
      await _syncMapAnnotations();
    } catch (error, stackTrace) {
      // This used to be `catch (_)`, which threw the reason away. On 11/08/2026
      // the screen came up completely blank and there was nothing anywhere -
      // no message on screen, no line in the console - to say why. Keep the
      // reassuring text for the user, but let the reason reach the log so the
      // next blank screen can be diagnosed instead of guessed at.
      debugPrint('Emergency map: loading nearby facilities failed: $error');
      debugPrintStack(stackTrace: stackTrace, label: 'emergency-map-load');
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

  Future<void> _defaultLocationConsentGrant({
    required String dataType,
    required String purpose,
    required String recipient,
    required String scope,
  }) async {
    await PrivacyService.instance.grantConsent(
      dataType: dataType,
      purpose: purpose,
      recipient: recipient,
      scope: scope,
    );
  }

  Future<void> _requestLocationConsent() async {
    if (_grantingLocationConsent ||
        _locationConsentDialogOpen ||
        _accountChanged) {
      return;
    }
    _locationConsentDialogOpen = true;
    bool? accepted;
    try {
      accepted = await showDialog<bool>(
        context: context,
        builder: (dialogContext) => AlertDialog(
          key: const Key('location-consent-dialog'),
          title: const Text('Cho phép dùng vị trí?'),
          content: const Text(
            'CareBridge sẽ chia sẻ vị trí hiện tại của bạn với bộ phận an toàn CareBridge để tìm cơ sở y tế gần đây và hỗ trợ cảnh báo khẩn cấp. Vị trí chỉ được đọc sau khi bạn đồng ý.',
            key: Key('location-consent-disclosure'),
          ),
          actions: [
            TextButton(
              key: const Key('location-consent-cancel'),
              onPressed: () => Navigator.of(dialogContext).pop(false),
              child: const Text('Hủy'),
            ),
            FilledButton(
              key: const Key('location-consent-confirm'),
              onPressed: () => Navigator.of(dialogContext).pop(true),
              child: const Text('Đồng ý và tiếp tục'),
            ),
          ],
        ),
      );
    } finally {
      _locationConsentDialogOpen = false;
    }
    if (accepted != true || !mounted || _accountChanged) return;

    final accountId = _accountId;
    setState(() => _grantingLocationConsent = true);
    try {
      await _grantLocationConsent(
        dataType: 'LOCATION',
        purpose: 'SHARE',
        recipient: 'CAREBRIDGE_SAFETY',
        scope: 'SAFETY_EMERGENCY_ALERT',
      );
      if (!mounted || _accountChanged || _accountId != accountId) return;
      setState(() => _grantingLocationConsent = false);
      await _load();
    } catch (_) {
      if (!mounted || _accountChanged || _accountId != accountId) return;
      setState(() {
        _grantingLocationConsent = false;
        _locationConsentRequired = true;
        _notice =
            'Không thể lưu đồng ý chia sẻ vị trí. Vui lòng thử lại; bạn vẫn có thể gọi cấp cứu 115.';
      });
    }
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

  Future<void> _shareCurrentLocation() async {
    if (_sharingLocation || _accountChanged || _sendingFamilyAlert) return;
    final generation = ++_locationShareGeneration;
    final accountId = AuthState.instance.userId;
    setState(() {
      _sharingLocation = true;
      _locationShareFailed = false;
      _locationShareSent = false;
    });
    try {
      if (!await _hasLocationConsent()) {
        await _requestLocationConsent();
      }
      if (!mounted ||
          generation != _locationShareGeneration ||
          AuthState.instance.userId != accountId) {
        return;
      }
      if (!await _hasLocationConsent()) {
        throw StateError('Cần đồng ý chia sẻ vị trí trước khi gửi.');
      }
      final position = await _permissions.readConsentedLocation();
      if (position == null) {
        throw StateError(
          'Không thể lấy vị trí hiện tại. Hãy bật định vị và thử lại.',
        );
      }
      // Seven decimal places retain centimetre-level precision and remain
      // compatible with older API deployments that reject raw Dart doubles.
      final latitude = double.parse(position.latitude.toStringAsFixed(7));
      final longitude = double.parse(position.longitude.toStringAsFixed(7));
      final result = await _emergency.shareCurrentLocation(
        latitude: latitude,
        longitude: longitude,
      );
      if (!mounted ||
          generation != _locationShareGeneration ||
          AuthState.instance.userId != accountId) {
        return;
      }
      setState(() {
        _position = position;
        _locationShareSent = true;
        _locationShareFailed = false;
      });
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(
          SnackBar(
            content: Text(
              'Đã gửi vị trí hiện tại cho ${result.recipientCount} người thân.',
            ),
            backgroundColor: const Color(0xFF5A463F),
            behavior: SnackBarBehavior.floating,
            shape: const StadiumBorder(),
          ),
        );
    } catch (error) {
      if (!mounted ||
          generation != _locationShareGeneration ||
          AuthState.instance.userId != accountId) {
        return;
      }
      setState(() {
        _locationShareFailed = true;
        _locationShareSent = false;
      });
      final message = switch (error) {
        StateError() => error.message.toString(),
        ApiException(statusCode: 403) =>
          'Quyền chia sẻ vị trí đã hết hiệu lực. Hãy cấp quyền lại rồi thử gửi.',
        ApiException(statusCode: 409) =>
          'Chưa có tài khoản Family hợp lệ trong nhóm gia đình để nhận vị trí.',
        ApiException(statusCode: >= 500) =>
          'Máy chủ chưa thể lưu vị trí. Hãy thử gửi lại sau ít phút.',
        ApiException() => error.displayMessage,
        _ => 'Không thể gửi vị trí. Hãy kiểm tra kết nối và thử lại.',
      };
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(
          SnackBar(
            content: Text(message),
            backgroundColor: const Color(0xFF5A463F),
            behavior: SnackBarBehavior.floating,
            shape: const StadiumBorder(),
          ),
        );
    } finally {
      if (mounted &&
          generation == _locationShareGeneration &&
          AuthState.instance.userId == accountId) {
        setState(() => _sharingLocation = false);
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
    try {
      await _tts.stop();
    } catch (_) {}
    if (mounted) setState(() => _navigationActive = false);
  }

  Future<void> _handleNavigationPosition(Position position) async {
    if (!_navigationActive || !mounted || _handlingNavigationPosition) return;
    _handlingNavigationPosition = true;
    try {
      if (!await _navigationHasActiveConsent()) {
        await _stopNavigation();
        if (mounted) {
          _resetMapRendererState();
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
      final mapController = _mapController;
      if (mapController != null) {
        try {
          await mapController.animateCamera(
            CameraUpdate.newLatLng(
              LatLng(position.latitude, position.longitude),
            ),
          );
        } catch (_) {
          // Text directions continue if the map camera cannot follow location.
        }
      }
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

  String _distance(CareFacility facility) {
    final meters = identical(_selected, facility) && _route != null
        ? _route!.distanceMeters
        : facility.distanceMeters?.toDouble();
    if (meters == null) return 'Khoảng cách chưa xác định';
    return meters >= 1000
        ? '${(meters / 1000).toStringAsFixed(1)} km'
        : '${meters.round()} m';
  }

  void _onMapCreated(TrackAsiaMapController? controller, int generation) {
    if (!mounted || generation != _mapGeneration) return;
    _mapController = controller;
    if (controller == null) return;
    controller.onCircleTapped.add((circle) {
      if (!mounted || generation != _mapGeneration) return;
      final index = circle.data?['facilityIndex'];
      if (index is int && index >= 0 && index < _results.length) {
        unawaited(_select(_results[index]));
      }
    });
  }

  void _onMapStyleLoaded(int generation) {
    if (!mounted || generation != _mapGeneration) return;
    _mapStyleTimer?.cancel();
    setState(() {
      _mapStyleReady = true;
      _mapStyleFailed = false;
    });
    unawaited(_syncMapAnnotations());
  }

  void _retryMapRenderer() {
    setState(_resetMapRendererState);
    _armMapStyleWatchdog(_mapGeneration);
  }

  void _resetMapRendererState() {
    _mapStyleTimer?.cancel();
    _mapStyleTimer = null;
    _mapController = null;
    ++_mapGeneration;
    _mapStyleReady = false;
    _mapStyleFailed = false;
  }

  void _armMapStyleWatchdog(int generation) {
    _mapStyleTimer?.cancel();
    _mapStyleTimer = Timer(widget.mapStyleLoadTimeout, () {
      if (!mounted || generation != _mapGeneration || _mapStyleReady) return;
      setState(() => _mapStyleFailed = true);
    });
  }

  Future<void> _syncMapAnnotations({bool fitCamera = true}) async {
    if (!_mapStyleReady) return;
    final position = _position;
    if (position == null) return;
    final customSynchronizer = widget.annotationSynchronizer;
    if (customSynchronizer != null) {
      await customSynchronizer(
        position: position,
        facilities: _results,
        route: _route,
      );
      return;
    }
    final controller = _mapController;
    if (controller == null) return;
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
    final mapKey = _effectiveTrackAsiaKey;
    if (mapKey.isEmpty || position == null) {
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
    final generation = _mapGeneration;
    final rendererKey = ValueKey('trackasia-map-$generation');
    final styleString =
        'https://maps.track-asia.com/styles/v2/streets.json?key=${Uri.encodeQueryComponent(mapKey)}';
    final initialCameraPosition = CameraPosition(
      target: LatLng(position.latitude, position.longitude),
      zoom: 13,
    );
    final customRenderer = widget.mapRenderer;
    final map = customRenderer != null
        ? customRenderer(
            key: rendererKey,
            styleString: styleString,
            initialCameraPosition: initialCameraPosition,
            myLocationEnabled: false,
            onMapCreated: (controller) => _onMapCreated(controller, generation),
            onStyleLoaded: () => _onMapStyleLoaded(generation),
          )
        : TrackAsiaMap(
            key: rendererKey,
            styleString: styleString,
            initialCameraPosition: initialCameraPosition,
            myLocationEnabled: false,
            myLocationTrackingMode: MyLocationTrackingMode.none,
            myLocationRenderMode: MyLocationRenderMode.normal,
            onMapCreated: (controller) => _onMapCreated(controller, generation),
            onStyleLoadedCallback: () => _onMapStyleLoaded(generation),
          );
    return KeyedSubtree(key: const Key('trackasia-map'), child: map);
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
        backgroundColor: Colors.white,
        foregroundColor: const Color(0xFF17324D),
        elevation: 1,
      ),
      body: LayoutBuilder(
        builder: (context, constraints) {
          final desktop = constraints.maxWidth >= 720;
          return Stack(
            children: [
              Positioned.fill(child: _buildMapCanvas()),
              if (_loading)
                const Positioned(
                  left: 0,
                  right: 0,
                  top: 0,
                  child: LinearProgressIndicator(key: Key('nearby-loading')),
                ),
              if (desktop)
                Positioned(
                  left: 16,
                  top: 16,
                  bottom: _panelCollapsed ? null : 16,
                  width: 390,
                  child: _buildFacilityPanel(),
                )
              else
                Align(
                  alignment: Alignment.bottomCenter,
                  child: _panelCollapsed
                      ? _buildFacilityPanel(compact: true)
                      : SizedBox(
                          height: (constraints.maxHeight * 0.72).clamp(
                            380.0,
                            520.0,
                          ),
                          child: _buildFacilityPanel(compact: true),
                        ),
                ),
              _buildStatusOverlay(desktop: desktop),
              _buildMapRuntimeOverlay(),
            ],
          );
        },
      ),
    );
  }

  Widget _buildMapCanvas() {
    return ColoredBox(
      color: const Color(0xFFE8F1EC),
      child: Stack(
        children: [
          Positioned.fill(child: _buildTrackAsiaMap()),
          Positioned(
            right: 16,
            bottom: 16,
            child: DecoratedBox(
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(8),
                boxShadow: const [
                  BoxShadow(color: Color(0x22000000), blurRadius: 8),
                ],
              ),
              child: const Padding(
                padding: EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                child: Text(
                  'TrackAsia · OSM',
                  style: TextStyle(fontSize: 11, color: Color(0xFF50657A)),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMapRuntimeOverlay() {
    if (_position == null || _effectiveTrackAsiaKey.isEmpty || _mapStyleReady) {
      return const Positioned.fill(
        child: IgnorePointer(child: SizedBox.shrink()),
      );
    }
    return Positioned(
      top: _hasStatusBanner ? 180 : 16,
      left: 16,
      right: 16,
      child: Align(
        alignment: Alignment.topCenter,
        child: _mapStyleFailed
            ? Card(
                key: const Key('trackasia-map-load-error'),
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Text(
                        'Không thể mở bản đồ TrackAsia. Danh sách cơ sở và gọi 115 vẫn dùng được.',
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: 8),
                      FilledButton.tonal(
                        key: const Key('trackasia-map-retry'),
                        onPressed: _retryMapRenderer,
                        child: const Text('Thử lại bản đồ'),
                      ),
                    ],
                  ),
                ),
              )
            : const IgnorePointer(
                child: Card(
                  child: Padding(
                    padding: EdgeInsets.symmetric(horizontal: 18, vertical: 12),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        SizedBox.square(
                          dimension: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        ),
                        SizedBox(width: 10),
                        Text('Đang mở bản đồ TrackAsia...'),
                      ],
                    ),
                  ),
                ),
              ),
      ),
    );
  }

  bool get _hasStatusBanner =>
      (_familyAlertFailed && _isTriageHandoff) ||
      _continuationExitError != null ||
      _notice != null;

  Widget _buildStatusOverlay({required bool desktop}) {
    final banners = <Widget>[
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
                      (AuthState.instance.role ?? '').trim().toUpperCase() ==
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
            if (_locationConsentRequired &&
                !_accountChanged &&
                !_noticeIsDialFallback)
              TextButton(
                key: const Key('location-consent-action'),
                onPressed: _grantingLocationConsent
                    ? null
                    : _requestLocationConsent,
                child: const Text('Cho phép vị trí'),
              ),
            if (!_locationConsentRequired &&
                !_accountChanged &&
                !_noticeIsDialFallback)
              TextButton(onPressed: _load, child: const Text('Thử lại')),
            if (_accountChanged || _noticeIsDialFallback)
              const SizedBox.shrink(),
          ],
        ),
    ];
    if (banners.isEmpty) return const SizedBox.shrink();
    return Positioned(
      left: desktop ? 422 : 12,
      right: 12,
      top: 12,
      child: Card(
        elevation: 5,
        clipBehavior: Clip.antiAlias,
        child: Column(mainAxisSize: MainAxisSize.min, children: banners),
      ),
    );
  }

  Widget _buildFacilityPanel({bool compact = false}) {
    return Card(
      key: const Key('nearby-facility-panel'),
      margin: compact
          ? const EdgeInsets.fromLTRB(10, 0, 10, 10)
          : EdgeInsets.zero,
      elevation: 10,
      color: Colors.white,
      clipBehavior: Clip.antiAlias,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
      child: Column(
        mainAxisSize: _panelCollapsed ? MainAxisSize.min : MainAxisSize.max,
        children: [
          InkWell(
            onTap: () => setState(() => _panelCollapsed = !_panelCollapsed),
            child: Container(
              color: const Color(0xFF1479C9),
              padding: const EdgeInsets.fromLTRB(16, 12, 6, 12),
              child: Row(
                children: [
                  const CircleAvatar(
                    radius: 19,
                    backgroundColor: Colors.white,
                    child: Icon(Icons.local_hospital, color: Color(0xFF1479C9)),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Bệnh viện gần bạn',
                          style: TextStyle(
                            color: Colors.white,
                            fontSize: 18,
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                        Text(
                          '${_results.length} kết quả trong ${_radiusMeters ~/ 1000} km',
                          style: const TextStyle(
                            color: Color(0xFFDDEFFF),
                            fontSize: 12,
                          ),
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    tooltip: 'Tải lại',
                    onPressed: _loading ? null : _load,
                    color: Colors.white,
                    icon: const Icon(Icons.refresh),
                  ),
                  IconButton(
                    key: const Key('facility-panel-toggle'),
                    tooltip: _panelCollapsed ? 'Mở rộng bảng' : 'Thu gọn bảng',
                    onPressed: () =>
                        setState(() => _panelCollapsed = !_panelCollapsed),
                    color: Colors.white,
                    icon: Icon(
                      _panelCollapsed
                          ? Icons.keyboard_arrow_up
                          : Icons.keyboard_arrow_down,
                      size: 28,
                    ),
                  ),
                ],
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 10, 12, 8),
            child: Row(
              children: [
                Expanded(
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
                    style: FilledButton.styleFrom(
                      backgroundColor: const Color(0xFFD62828),
                      foregroundColor: Colors.white,
                    ),
                    icon: const Icon(Icons.call),
                    label: const Text('Gọi cấp cứu 115'),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: KeyedSubtree(
                    key: const Key('emergency-family-alert'),
                    child: OutlinedButton.icon(
                      key: const Key('family-alert'),
                      onPressed:
                          _sharingLocation ||
                              _sendingFamilyAlert ||
                              _accountChanged
                          ? null
                          : _shareCurrentLocation,
                      style: OutlinedButton.styleFrom(
                        foregroundColor: const Color(0xFF845143),
                        backgroundColor: _locationShareSent
                            ? const Color(0xFFF2EAE4)
                            : Colors.white,
                        side: const BorderSide(color: Color(0xFFE8DDD6)),
                        shape: const StadiumBorder(),
                      ),
                      icon: _sharingLocation
                          ? const SizedBox.square(
                              dimension: 16,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : Icon(
                              _locationShareFailed
                                  ? Icons.refresh_rounded
                                  : _locationShareSent
                                  ? Icons.check_circle_rounded
                                  : Icons.share_location_rounded,
                            ),
                      label: Text(
                        _sharingLocation
                            ? 'Đang gửi vị trí...'
                            : _locationShareFailed
                            ? 'Thử gửi lại vị trí'
                            : _locationShareSent
                            ? 'Đã gửi vị trí'
                            : 'Gửi vị trí',
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
          if (!_panelCollapsed) ...[
            _buildSearchFilters(),
            const Divider(height: 1),
            Expanded(child: _buildFacilityList()),
            if (_selected != null) ...[
              const Divider(height: 1),
              ConstrainedBox(
                constraints: BoxConstraints(maxHeight: compact ? 150 : 190),
                child: SingleChildScrollView(child: _buildSelected(_selected!)),
              ),
            ],
          ],
        ],
      ),
    );
  }

  Widget _buildSearchFilters() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 0, 12, 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: [
                const Icon(Icons.radar, size: 18, color: Color(0xFF50657A)),
                const SizedBox(width: 6),
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
          const SizedBox(height: 6),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: [
                for (final entry in const {
                  'hospital': 'Bệnh viện',
                  'clinic': 'Phòng khám',
                  'health_station': 'Trạm y tế',
                }.entries)
                  Padding(
                    padding: const EdgeInsets.only(right: 6),
                    child: ChoiceChip(
                      key: Key('facility-type-${entry.key}'),
                      avatar: Icon(
                        entry.key == 'hospital'
                            ? Icons.local_hospital_outlined
                            : Icons.medical_services_outlined,
                        size: 16,
                      ),
                      label: Text(entry.value),
                      selected: _facilityType == entry.key,
                      onSelected: (_) => _changeFacilityType(entry.key),
                    ),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFacilityList() {
    if (_results.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(
                Icons.local_hospital_outlined,
                size: 52,
                color: Color(0xFF78909C),
              ),
              const SizedBox(height: 10),
              Text(
                _loading
                    ? 'Đang tìm cơ sở y tế gần bạn...'
                    : 'Chưa tìm thấy cơ sở trong bán kính này',
                textAlign: TextAlign.center,
              ),
            ],
          ),
        ),
      );
    }
    return ListView.separated(
      key: const Key('nearby-list'),
      padding: const EdgeInsets.all(10),
      itemCount: _results.length,
      separatorBuilder: (_, _) => const SizedBox(height: 8),
      itemBuilder: (context, index) {
        final facility = _results[index];
        final selected = identical(_selected, facility);
        return Material(
          color: selected ? const Color(0xFFE6F3FC) : Colors.white,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
            side: BorderSide(
              color: selected
                  ? const Color(0xFF1479C9)
                  : const Color(0xFFDCE4EA),
            ),
          ),
          child: ListTile(
            key: Key('facility-$index'),
            contentPadding: const EdgeInsets.fromLTRB(12, 6, 6, 6),
            leading: CircleAvatar(
              backgroundColor: const Color(0xFFE1F0FB),
              child: Icon(
                _facilityType == 'hospital'
                    ? Icons.local_hospital
                    : Icons.medical_services,
                color: const Color(0xFF1479C9),
              ),
            ),
            title: Text(
              facility.name,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(fontWeight: FontWeight.w700),
            ),
            subtitle: Text(
              [
                if (facility.address?.isNotEmpty == true) facility.address!,
                _distance(facility),
                '${facility.sourceLabel} · ${facility.verificationLabel}',
              ].join('\n'),
              maxLines: 4,
              overflow: TextOverflow.ellipsis,
            ),
            trailing: IconButton.filledTonal(
              tooltip: facility.phone == null ? 'Xem chi tiết' : 'Gọi cơ sở',
              onPressed: facility.phone == null
                  ? () => _select(facility)
                  : () => _call(facility.phone),
              icon: Icon(
                facility.phone == null ? Icons.chevron_right : Icons.call,
              ),
            ),
            onTap: () => _select(facility),
          ),
        );
      },
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
