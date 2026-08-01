import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:go_router/go_router.dart';
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
    _load();
  }

  @override
  void dispose() {
    AuthState.instance.removeListener(_handleAuthChanged);
    super.dispose();
  }

  void _handleAuthChanged() {
    final current = AuthState.instance.userId;
    if (current == _accountId) return;
    _accountId = current;
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
          _loading = false;
          _notice =
              'Không có quyền vị trí. Bạn vẫn có thể gọi cấp cứu hoặc thử lại.';
        });
        return;
      }
      final results = await _facilities.searchNearby(
        latitude: position.latitude,
        longitude: position.longitude,
      );
      if (!mounted || generation != _loadGeneration) return;
      final selectionGeneration = ++_selectionGeneration;
      setState(() {
        _position = position;
        _results = results;
        _selected = results.isEmpty ? null : results.first;
        _loading = false;
        _notice = results.isEmpty
            ? 'Không tìm thấy cơ sở phù hợp. Hãy gọi cấp cứu khi cần.'
            : null;
      });
      if (_selected?.hasCoordinates == true) {
        await _loadRoute(_selected!, selectionGeneration);
      }
    } catch (_) {
      if (mounted && generation == _loadGeneration) {
        setState(() {
          _loading = false;
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
      );
      if (mounted &&
          selectionGeneration == _selectionGeneration &&
          identical(_selected, facility)) {
        setState(() => _route = route);
      }
    } catch (_) {
      // Directions can still be delegated to the installed navigation app.
    }
  }

  Future<void> _select(CareFacility facility) async {
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
    if (!facility.hasCoordinates) return;
    await _launch(
      Uri.parse(
        'https://www.google.com/maps/dir/?api=1&destination='
        '${facility.latitude},${facility.longitude}',
      ),
    );
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
          Expanded(
            child: _results.isEmpty
                ? const Center(
                    child: Icon(Icons.local_hospital_outlined, size: 72),
                  )
                : ListView.builder(
                    key: const Key('nearby-list'),
                    padding: const EdgeInsets.all(12),
                    itemCount: _results.length,
                    itemBuilder: (_, index) {
                      final facility = _results[index];
                      return Card(
                        child: ListTile(
                          key: Key('facility-$index'),
                          selected: identical(_selected, facility),
                          title: Text(facility.name),
                          subtitle: Text(
                            [
                              if (facility.address?.isNotEmpty == true)
                                facility.address!,
                              _distance(facility),
                              facility.sourceLabel,
                              facility.verificationLabel,
                            ].join('\n'),
                          ),
                          isThreeLine: true,
                          onTap: () => _select(facility),
                        ),
                      );
                    },
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
                    icon: const Icon(Icons.directions),
                    label: const Text('Chỉ đường'),
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
