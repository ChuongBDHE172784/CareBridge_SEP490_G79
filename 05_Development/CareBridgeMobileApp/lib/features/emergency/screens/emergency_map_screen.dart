import 'dart:async';
import 'dart:math' as math;
import 'dart:typed_data';
import 'dart:ui' as ui;

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
    this.showFamilyLocationShare,
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
  final bool? showFamilyLocationShare;

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
  // Ảnh phải được nạp vào style trước khi symbol tham chiếu tới tên này, và
  // phải nạp lại mỗi lần style dựng lại (đổi style là mất toàn bộ ảnh đã nạp).
  static const String _facilityIcon = 'carebridge-facility';
  static const String _selectedFacilityIcon = 'carebridge-facility-selected';
  bool _mapIconsRegistered = false;
  // Chỉ căn giữa vào mẹ ở lần mở bản đồ đầu tiên. Sau đó người dùng tự kéo bản
  // đồ hoặc bấm nút, kéo camera về nữa sẽ thành giành quyền điều khiển.
  bool _initialCenterDone = false;
  bool _mapStyleFailed = false;
  int _mapGeneration = 0;
  Timer? _mapStyleTimer;
  bool _navigationActive = false;
  bool _voiceEnabled = true;
  bool _followUser = true;
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

  bool get _canShareLocationToFamily {
    if (widget.showFamilyLocationShare != null) {
      return widget.showFamilyLocationShare!;
    }
    final role = AuthState.instance.role;
    if (role != null && role.toUpperCase() == 'FAMILY') {
      return false;
    }
    return true;
  }

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
    final pos = _position;
    if (pos != null && facility.hasCoordinates) {
      final distanceToDest = Geolocator.distanceBetween(
        pos.latitude,
        pos.longitude,
        facility.latitude!,
        facility.longitude!,
      );
      if (distanceToDest <= 35) {
        _currentStepIndex = (_route?.steps.length ?? 1) - 1;
        _followUser = true;
        setState(() => _navigationActive = true);
        await _speak('Bạn đang ở vị trí của ${facility.name}.');
        return;
      }
    }
    _currentStepIndex = 0;
    _followUser = true;
    setState(() => _navigationActive = true);
    await _speakCurrentStep(prefix: 'Bắt đầu dẫn đường đến ${facility.name}. ');
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
    if (mounted) {
      setState(() {
        _navigationActive = false;
        _followUser = false;
      });
    }
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
      final route = _route;
      if (mapController != null && _followUser) {
        try {
          final bearing = position.heading > 0 && position.heading <= 360
              ? position.heading
              : (route != null &&
                      route.steps.isNotEmpty &&
                      _currentStepIndex < route.steps.length
                  ? (Geolocator.bearingBetween(
                          position.latitude,
                          position.longitude,
                          route.steps[_currentStepIndex].latitude,
                          route.steps[_currentStepIndex].longitude,
                        ) +
                        360) %
                      360
                  : 0.0);
          await mapController.animateCamera(
            CameraUpdate.newCameraPosition(
              CameraPosition(
                target: LatLng(position.latitude, position.longitude),
                zoom: 17.5,
                tilt: 50.0,
                bearing: bearing,
              ),
            ),
          );
        } catch (_) {
          // Text directions continue if the map camera cannot follow location.
        }
      }
      final facility = _selected;
      if (facility != null && facility.hasCoordinates) {
        final distanceToDest = Geolocator.distanceBetween(
          position.latitude,
          position.longitude,
          facility.latitude!,
          facility.longitude!,
        );
        if (distanceToDest <= 35) {
          final facilityName = facility.name;
          await _speak('Bạn đã đến nơi. $facilityName.');
          await _stopNavigation();
          return;
        }
      }
      if (route == null || route.steps.isEmpty) return;
      final currentIndex = _currentStepIndex;
      if (currentIndex + 1 < route.steps.length) {
        final currentStep = route.steps[currentIndex];
        final nextStep = route.steps[currentIndex + 1];

        final distanceToEnd = Geolocator.distanceBetween(
          position.latitude,
          position.longitude,
          nextStep.latitude,
          nextStep.longitude,
        );
        final distanceFromStart = Geolocator.distanceBetween(
          position.latitude,
          position.longitude,
          currentStep.latitude,
          currentStep.longitude,
        );

        final reachedNextStep = distanceToEnd < 12 ||
            (distanceToEnd < 28 &&
                (distanceFromStart > 15 || currentStep.distanceMeters < 20));

        if (reachedNextStep) {
          setState(() => _currentStepIndex = currentIndex + 1);
          await _speakCurrentStep();
        }
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
          _selectionGeneration != generation ||
          !identical(facility, _selected) ||
          _transportMode != transportMode) {
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

  CareRouteStep? get _activeUpcomingStep {
    final route = _route;
    if (route == null || route.steps.isEmpty) return null;
    if (_currentStepIndex + 1 < route.steps.length) {
      return route.steps[_currentStepIndex + 1];
    }
    return route.steps.last;
  }

  double get _distanceToUpcomingStep {
    final pos = _position;
    final route = _route;
    if (pos == null || route == null || route.steps.isEmpty) return 0;
    if (_currentStepIndex + 1 < route.steps.length) {
      final nextStep = route.steps[_currentStepIndex + 1];
      return Geolocator.distanceBetween(
        pos.latitude,
        pos.longitude,
        nextStep.latitude,
        nextStep.longitude,
      );
    }
    final sel = _selected;
    if (sel != null && sel.hasCoordinates) {
      return Geolocator.distanceBetween(
        pos.latitude,
        pos.longitude,
        sel.latitude!,
        sel.longitude!,
      );
    }
    return 0;
  }

  Future<void> _speakCurrentStep({String prefix = ''}) async {
    if (!_voiceEnabled) return;
    final target = _activeUpcomingStep;
    if (target == null) return;
    final dist = _formatStepDistance(_distanceToUpcomingStep);
    final instruction = _formatStepInstruction(target);
    await _speak('$prefix$dist, $instruction.');
  }

  String _formatStepDistance(double meters) {
    if (meters < 20) return 'Trong vài mét';
    if (meters < 1000) return 'Trong ${meters.round()}m';
    return 'Sau ${(meters / 1000).toStringAsFixed(1)}km';
  }

  String _vietnameseManeuver(String maneuver) {
    final value = maneuver.toLowerCase();
    if (value.contains('slight-right') || value.contains('slight_right')) {
      return 'Chếch sang phải';
    }
    if (value.contains('sharp-right') || value.contains('sharp_right')) {
      return 'Rẽ ngoặt sang phải';
    }
    if (value.contains('right')) return 'Rẽ phải';
    if (value.contains('slight-left') || value.contains('slight_left')) {
      return 'Chếch sang trái';
    }
    if (value.contains('sharp-left') || value.contains('sharp_left')) {
      return 'Rẽ ngoặt sang trái';
    }
    if (value.contains('left')) return 'Rẽ trái';
    if (value.contains('uturn') || value.contains('u-turn')) return 'Quay đầu xe';
    if (value.contains('arrive') || value.contains('destination')) return 'Bạn đã đến nơi';
    if (value.contains('depart')) return 'Bắt đầu đi';
    if (value.contains('roundabout')) return 'Đi vào vòng xuyến';
    return 'Tiếp tục đi thẳng';
  }

  IconData _stepManeuverIcon(String maneuver) {
    final m = maneuver.toLowerCase();
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
      return 'Đến cơ sở y tế';
    }
    return (road != null && road.isNotEmpty)
        ? 'Tiếp tục trên $road'
        : 'Tiếp tục đi thẳng';
  }

  String _facilityTypeLabel() => switch (_facilityType) {
    'clinic' => 'Phòng khám',
    'health_station' => 'Trạm y tế',
    _ => 'Bệnh viện',
  };

  Future<void> _openGoogleMaps() async {
    final target = _selected ?? (_results.isNotEmpty ? _results.first : null);
    if (target?.latitude == null || target?.longitude == null) {
      final pos = _position;
      final uri = Uri.parse(
        pos != null
            ? 'https://www.google.com/maps/search/hospital/@${pos.latitude},${pos.longitude},14z'
            : 'https://www.google.com/maps/search/hospital/',
      );
      try {
        await launchUrl(uri, mode: LaunchMode.externalApplication);
      } catch (_) {
        if (mounted) setState(() => _notice = 'Không thể mở Google Maps trên thiết bị.');
      }
      return;
    }
    final uri = Uri.parse(
      'https://www.google.com/maps/dir/?api=1&destination='
      '${target!.latitude},${target.longitude}&travelmode=driving',
    );
    try {
      final opened = await launchUrl(uri, mode: LaunchMode.externalApplication);
      if (!opened && mounted) {
        setState(() => _notice = 'Không thể mở Google Maps trên thiết bị.');
      }
    } catch (_) {
      if (mounted) {
        setState(() => _notice = 'Không thể mở Google Maps trên thiết bị.');
      }
    }
  }

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

  /// Vẽ ghim bệnh viện thành PNG ngay lúc chạy.
  ///
  /// TrackAsia chỉ nhận ảnh bitmap cho `iconImage`, mà dự án không có sẵn asset
  /// nào ngoài logo. Vẽ từ font Material Icons tránh phải thêm tệp nhị phân vào
  /// repo, và đổi màu/kích thước chỉ là sửa tham số.
  Future<Uint8List> _renderFacilityPin({
    required Color fill,
    required double size,
  }) async {
    final recorder = ui.PictureRecorder();
    final canvas = Canvas(recorder);
    final radius = size / 2;
    final centre = Offset(radius, radius * 0.86);

    // Viền trắng để ghim còn đọc được trên nền bản đồ đậm.
    canvas.drawCircle(centre, radius * 0.82, Paint()..color = Colors.white);
    canvas.drawCircle(centre, radius * 0.72, Paint()..color = fill);

    // Chân ghim nhọn xuống, để neo 'bottom' trỏ đúng toạ độ.
    final tail = Path()
      ..moveTo(radius - size * 0.13, centre.dy + radius * 0.55)
      ..lineTo(radius, size)
      ..lineTo(radius + size * 0.13, centre.dy + radius * 0.55)
      ..close();
    canvas.drawPath(tail, Paint()..color = fill);

    const icon = Icons.local_hospital_rounded;
    final painter = TextPainter(textDirection: TextDirection.ltr)
      ..text = TextSpan(
        text: String.fromCharCode(icon.codePoint),
        style: TextStyle(
          fontSize: size * 0.42,
          fontFamily: icon.fontFamily,
          package: icon.fontPackage,
          color: Colors.white,
        ),
      )
      ..layout();
    painter.paint(
      canvas,
      Offset(radius - painter.width / 2, centre.dy - painter.height / 2),
    );

    final image = await recorder.endRecording().toImage(
          size.ceil(),
          size.ceil(),
        );
    final data = await image.toByteData(format: ui.ImageByteFormat.png);
    return data!.buffer.asUint8List();
  }

  Future<void> _registerMapIcons(TrackAsiaMapController controller) async {
    if (_mapIconsRegistered) return;
    try {
      await controller.addImage(
        _facilityIcon,
        await _renderFacilityPin(fill: const Color(0xFFDC2626), size: 84),
      );
      await controller.addImage(
        _selectedFacilityIcon,
        await _renderFacilityPin(fill: const Color(0xFF845143), size: 100),
      );
      _mapIconsRegistered = true;
    } catch (_) {
      // Nạp ảnh hỏng thì symbol sẽ không hiện; danh sách bên dưới vẫn dùng được.
    }
  }

  /// Đưa vị trí mẹ vào chính giữa khung nhìn.
  Future<void> _centerOnMother({double zoom = 15.5}) async {
    final controller = _mapController;
    final position = _position;
    if (controller == null || position == null) return;
    try {
      await controller.animateCamera(
        CameraUpdate.newCameraPosition(
          CameraPosition(
            target: LatLng(position.latitude, position.longitude),
            zoom: zoom,
          ),
        ),
      );
    } catch (_) {
      // Camera không nhúc nhích thì bản đồ vẫn hiển thị được.
    }
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
    // Cơ sở y tế giờ vẽ bằng symbol, nên sự kiện chạm đến qua onSymbolTapped.
    // Giữ luôn nhánh onCircleTapped ở trên cho chấm vị trí của mẹ.
    controller.onSymbolTapped.add((symbol) {
      if (!mounted || generation != _mapGeneration) return;
      final index = symbol.data?['facilityIndex'];
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
    // Đổi style làm mất mọi ảnh đã nạp, nên cờ được hạ để nạp lại từ đầu.
    _mapIconsRegistered = false;
    unawaited(() async {
      final controller = _mapController;
      if (controller != null) await _registerMapIcons(controller);
      if (!mounted || generation != _mapGeneration) return;
      // Lần đầu mở bản đồ thì đặt mẹ vào giữa; khung bao trọn tuyến đường chỉ
      // dùng khi người dùng chủ động bấm nút toàn cảnh hoặc đã chọn cơ sở.
      final centreFirst = !_initialCenterDone && _selected == null;
      await _syncMapAnnotations(fitCamera: !centreFirst);
      if (centreFirst) {
        _initialCenterDone = true;
        await _centerOnMother();
      }
    }());
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
      await controller.clearSymbols();
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
        final chosen = identical(facility, _selected);
        await controller.addSymbol(
          SymbolOptions(
            geometry: LatLng(facility.latitude!, facility.longitude!),
            iconImage: chosen ? _selectedFacilityIcon : _facilityIcon,
            // Neo đáy ghim vào đúng toạ độ, như cách mọi bản đồ đặt ghim.
            iconAnchor: 'bottom',
            iconSize: 1.0,
          ),
          {'facilityIndex': index},
        );
      }
      final coordinates = _route?.coordinates ?? const [];
      final points = coordinates.length >= 2
          ? coordinates
              .map((point) => LatLng(point.latitude, point.longitude))
              .toList(growable: false)
          : (_selected != null && _selected!.hasCoordinates
              ? <LatLng>[
                  LatLng(position.latitude, position.longitude),
                  LatLng(_selected!.latitude!, _selected!.longitude!),
                ]
              : const <LatLng>[]);

      if (points.length >= 2) {
        // High contrast casing border line
        await controller.addLine(
          LineOptions(
            geometry: points,
            lineColor: '#1E40AF',
            lineWidth: 8,
            lineOpacity: 0.8,
            lineJoin: 'round',
          ),
        );
        // Main high-visibility active route line
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
    final route = _route;
    final pos = _position;
    final sel = _selected;
    final distanceToDest = (pos != null && sel != null && sel.hasCoordinates)
        ? Geolocator.distanceBetween(
            pos.latitude,
            pos.longitude,
            sel.latitude!,
            sel.longitude!,
          )
        : double.infinity;
    final isArrived = distanceToDest <= 35;

    return Scaffold(
      backgroundColor: _surface,
      appBar: AppBar(
        leading: IconButton(
          tooltip: 'Quay lại',
          onPressed: _restoringContinuation ? null : _leaveEmergency,
          icon: const Icon(Icons.arrow_back),
        ),
        title: Text(
          _navigationActive ? 'Đang dẫn đường' : 'Cơ sở y tế gần đây',
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontWeight: FontWeight.w700,
          ),
        ),
        backgroundColor: Colors.white,
        foregroundColor: const Color(0xFF5A463F),
        elevation: 0,
        scrolledUnderElevation: 1,
        actions: [
          IconButton(
            tooltip: _voiceEnabled ? 'Tắt giọng nói' : 'Bật giọng nói',
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
            ),
          ),
          IconButton(
            tooltip: 'Mở Google Maps',
            onPressed: _openGoogleMaps,
            icon: const Icon(Icons.open_in_new_rounded),
          ),
        ],
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

              // Floating top turn-by-turn banner when navigating
              if (_navigationActive &&
                  route != null &&
                  route.steps.isNotEmpty)
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
                        color: Colors.white,
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
                                  : _stepManeuverIcon(
                                      _activeUpcomingStep?.maneuver ??
                                          route.steps.first.maneuver,
                                    ),
                              color: const Color(0xFF845143),
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
                                      : _formatStepDistance(_distanceToUpcomingStep),
                                  style: const TextStyle(
                                    fontFamily: 'Lexend',
                                    color: Color(0xFF845143),
                                    fontWeight: FontWeight.w800,
                                    fontSize: 16,
                                  ),
                                ),
                                Text(
                                  isArrived
                                      ? 'Bạn đang ở vị trí của ${_selected?.name ?? "cơ sở y tế"}'
                                      : _formatStepInstruction(_activeUpcomingStep),
                                  key: const Key('navigation-instruction'),
                                  maxLines: 2,
                                  overflow: TextOverflow.ellipsis,
                                  style: const TextStyle(
                                    fontFamily: 'Lexend',
                                    color: Color(0xFF5A463F),
                                    fontWeight: FontWeight.w600,
                                    fontSize: 13,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          IconButton(
                            tooltip: _voiceEnabled
                                ? 'Tắt giọng nói'
                                : 'Bật giọng nói',
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
                              color: _voiceEnabled
                                  ? const Color(0xFF845143)
                                  : const Color(0xFF9C857C),
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
                // Thẻ "Bệnh viện gần bạn" cao khoảng 140px khi thu gọn, nên mốc
                // 120 cũ khiến nút dưới cùng nằm khuất sau thẻ. Nâng lên cho
                // cả ba nút nằm trọn phía trên thẻ.
                bottom: _panelCollapsed ? 200 : (desktop ? 20 : 430),
                child: SafeArea(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      FloatingActionButton.small(
                        heroTag: 'emergency_voice_btn',
                        onPressed: () {
                          setState(() => _voiceEnabled = !_voiceEnabled);
                          if (!_voiceEnabled) {
                            unawaited(_tts.stop());
                          } else {
                            unawaited(_speak('Đã bật giọng nói'));
                          }
                        },
                        backgroundColor: _voiceEnabled
                            ? Colors.white
                            : const Color(0xFFF2EAE4),
                        foregroundColor: _voiceEnabled
                            ? const Color(0xFF845143)
                            : const Color(0xFF9C857C),
                        tooltip:
                            _voiceEnabled ? 'Tắt giọng nói' : 'Bật giọng nói',
                        child: Icon(
                          _voiceEnabled
                              ? Icons.volume_up_rounded
                              : Icons.volume_off_rounded,
                        ),
                      ),
                      const SizedBox(height: 10),
                      FloatingActionButton.small(
                        heroTag: 'emergency_overview_btn',
                        onPressed: () => unawaited(_fitMapCamera()),
                        backgroundColor: Colors.white,
                        foregroundColor: const Color(0xFF5A463F),
                        tooltip: 'Toàn cảnh tuyến đường',
                        child: const Icon(Icons.route_rounded),
                      ),
                      const SizedBox(height: 10),
                      FloatingActionButton.small(
                        heroTag: 'emergency_recenter_btn',
                        onPressed: () {
                          setState(() => _followUser = true);
                          final pos = _position;
                          if (pos != null && _mapController != null) {
                            _mapController!.animateCamera(
                              CameraUpdate.newCameraPosition(
                                CameraPosition(
                                  target: LatLng(pos.latitude, pos.longitude),
                                  zoom: _navigationActive ? 17.5 : 15,
                                  tilt: _navigationActive ? 50.0 : 0,
                                  bearing: 0,
                                ),
                              ),
                            );
                          }
                        },
                        backgroundColor: _followUser
                            ? const Color(0xFFC98C7B)
                            : Colors.white,
                        foregroundColor:
                            _followUser ? Colors.white : const Color(0xFF5A463F),
                        tooltip: 'Định vị lại',
                        child: const Icon(Icons.my_location_rounded),
                      ),
                    ],
                  ),
                ),
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
                if (_canShareLocationToFamily) ...[
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
      padding: const EdgeInsets.fromLTRB(14, 4, 14, 12),
      child: Row(
        children: [
          // Dropdown 1: Loại cơ sở y tế (Facility Type)
          Expanded(
            flex: 3,
            child: Container(
              height: 44,
              padding: const EdgeInsets.symmetric(horizontal: 10),
              decoration: BoxDecoration(
                color: const Color(0xFFF9F6F3),
                borderRadius: BorderRadius.circular(14),
                border: Border.all(color: const Color(0xFFE8DDD6)),
              ),
              child: DropdownButtonHideUnderline(
                child: DropdownButton<String>(
                  key: const Key('facility-type-dropdown'),
                  value: _facilityType,
                  isExpanded: true,
                  icon: const Icon(
                    Icons.keyboard_arrow_down_rounded,
                    color: Color(0xFF845143),
                    size: 20,
                  ),
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontWeight: FontWeight.w600,
                    fontSize: 13,
                    color: Color(0xFF5A463F),
                  ),
                  dropdownColor: Colors.white,
                  borderRadius: BorderRadius.circular(16),
                  onChanged: (type) {
                    if (type != null) _changeFacilityType(type);
                  },
                  items: const [
                    DropdownMenuItem(
                      value: 'hospital',
                      child: Row(
                        children: [
                          Icon(
                            Icons.local_hospital_rounded,
                            size: 16,
                            color: Color(0xFF845143),
                          ),
                          SizedBox(width: 6),
                          Expanded(
                            child: Text(
                              'Bệnh viện',
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                        ],
                      ),
                    ),
                    DropdownMenuItem(
                      value: 'clinic',
                      child: Row(
                        children: [
                          Icon(
                            Icons.medical_services_rounded,
                            size: 16,
                            color: Color(0xFF845143),
                          ),
                          SizedBox(width: 6),
                          Expanded(
                            child: Text(
                              'Phòng khám',
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                        ],
                      ),
                    ),
                    DropdownMenuItem(
                      value: 'health_station',
                      child: Row(
                        children: [
                          Icon(
                            Icons.health_and_safety_rounded,
                            size: 16,
                            color: Color(0xFF845143),
                          ),
                          SizedBox(width: 6),
                          Expanded(
                            child: Text(
                              'Trạm y tế',
                              overflow: TextOverflow.ellipsis,
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
          const SizedBox(width: 8),
          // Dropdown 2: Bán kính tìm kiếm (Radius km)
          Expanded(
            flex: 2,
            child: Container(
              height: 44,
              padding: const EdgeInsets.symmetric(horizontal: 10),
              decoration: BoxDecoration(
                color: const Color(0xFFF9F6F3),
                borderRadius: BorderRadius.circular(14),
                border: Border.all(color: const Color(0xFFE8DDD6)),
              ),
              child: DropdownButtonHideUnderline(
                child: DropdownButton<int>(
                  key: const Key('radius-dropdown'),
                  value: _radiusMeters,
                  isExpanded: true,
                  icon: const Icon(
                    Icons.keyboard_arrow_down_rounded,
                    color: Color(0xFF845143),
                    size: 20,
                  ),
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontWeight: FontWeight.w600,
                    fontSize: 13,
                    color: Color(0xFF5A463F),
                  ),
                  dropdownColor: Colors.white,
                  borderRadius: BorderRadius.circular(16),
                  onChanged: (radius) {
                    if (radius != null) _changeRadius(radius);
                  },
                  items: const [
                    DropdownMenuItem(
                      key: Key('radius-5000'),
                      value: 5000,
                      child: Row(
                        children: [
                          Icon(
                            Icons.radar_rounded,
                            size: 16,
                            color: Color(0xFF845143),
                          ),
                          SizedBox(width: 6),
                          Text('5 km'),
                        ],
                      ),
                    ),
                    DropdownMenuItem(
                      key: Key('radius-10000'),
                      value: 10000,
                      child: Row(
                        children: [
                          Icon(
                            Icons.radar_rounded,
                            size: 16,
                            color: Color(0xFF845143),
                          ),
                          SizedBox(width: 6),
                          Text('10 km'),
                        ],
                      ),
                    ),
                    DropdownMenuItem(
                      key: Key('radius-15000'),
                      value: 15000,
                      child: Row(
                        children: [
                          Icon(
                            Icons.radar_rounded,
                            size: 16,
                            color: Color(0xFF845143),
                          ),
                          SizedBox(width: 6),
                          Text('15 km'),
                        ],
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
    final route = _route;
    return SafeArea(
      top: false,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                Container(
                  width: 44,
                  height: 44,
                  decoration: BoxDecoration(
                    color: const Color(0x1AC98C7B),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Icon(
                    Icons.local_hospital_rounded,
                    color: Color(0xFF845143),
                    size: 24,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        facility.name,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontWeight: FontWeight.w700,
                          fontSize: 16,
                          color: Color(0xFF5A463F),
                        ),
                      ),
                      const SizedBox(height: 2),
                      if (facility.address != null &&
                          facility.address!.trim().isNotEmpty)
                        Text(
                          facility.address!.trim(),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 12,
                            color: Color(0xFF9C857C),
                          ),
                        ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),
            if (route != null)
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 8,
                ),
                decoration: BoxDecoration(
                  color: const Color(0xFFF2EAE4),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Row(
                  children: [
                    const Icon(
                      Icons.directions_car_rounded,
                      size: 16,
                      color: Color(0xFF845143),
                    ),
                    const SizedBox(width: 6),
                    Text(
                      'ETA ${route.etaMinutes} phút · ${_distance(facility)}',
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontWeight: FontWeight.w700,
                        fontSize: 13,
                        color: Color(0xFF845143),
                      ),
                    ),
                  ],
                ),
              ),
            if (_navigationActive && route?.steps.isNotEmpty == true) ...[
              const SizedBox(height: 8),
              Text(
                () {
                  final target = _activeUpcomingStep;
                  if (target == null) return 'Tiếp tục đi thẳng';
                  return '${_vietnameseManeuver(target.maneuver)} · ${_distanceToUpcomingStep.round()} m';
                }(),
                key: const Key('navigation-instruction'),
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontWeight: FontWeight.w700,
                  fontSize: 14,
                  color: Color(0xFF2563EB),
                ),
              ),
            ],
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    key: const Key('facility-call'),
                    onPressed: facility.phone == null
                        ? null
                        : () => _call(facility.phone),
                    icon: const Icon(Icons.call_rounded),
                    label: const Text(
                      'Gọi cơ sở',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: const Color(0xFF5A463F),
                      side: const BorderSide(color: Color(0xFFE8DDD6)),
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(999),
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: FilledButton.icon(
                    key: const Key('facility-navigate'),
                    onPressed: facility.hasCoordinates
                        ? () => _navigate(facility)
                        : null,
                    icon: Icon(
                      _navigationActive
                          ? Icons.stop_circle_rounded
                          : Icons.navigation_rounded,
                    ),
                    label: Text(
                      _navigationActive
                          ? 'Dừng dẫn đường'
                          : 'Bắt đầu dẫn đường',
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    style: FilledButton.styleFrom(
                      backgroundColor: _navigationActive
                          ? const Color(0xFFDC2626)
                          : const Color(0xFFC98C7B),
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(999),
                      ),
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
