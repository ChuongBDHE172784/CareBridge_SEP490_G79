import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:geolocator/geolocator.dart';
import 'package:go_router/go_router.dart';
import 'package:trackasia_gl/trackasia_gl.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/features/aiTriage/models/triage_continuation.dart';
import 'package:untitled/features/aiTriage/services/triage_continuation_restore_coordinator.dart';
import 'package:untitled/features/aiTriage/services/triage_continuation_store.dart';
import 'package:untitled/features/aiTriage/services/triage_service.dart';
import 'package:untitled/features/emergency/models/emergency_session_model.dart';
import 'package:untitled/features/emergency/models/care_facility_model.dart';
import 'package:untitled/features/emergency/models/location_share_result.dart';
import 'package:untitled/features/emergency/screens/emergency_map_screen.dart';
import 'package:untitled/features/emergency/services/care_facility_service.dart';
import 'package:untitled/features/emergency/services/emergency_service.dart';
import 'package:untitled/features/safety/services/safety_permission_service.dart';

class _RecordingFacilityService extends CareFacilityService {
  _RecordingFacilityService([this.results = const []]);

  final List<CareFacility> results;
  int searchCalls = 0;

  @override
  Future<List<CareFacility>> searchNearby({
    required double latitude,
    required double longitude,
    int radiusMeters = 5000,
    String type = 'hospital',
  }) async {
    searchCalls++;
    return results;
  }
}

Position _position({double latitude = 10.76, double longitude = 106.66}) =>
    Position(
      longitude: longitude,
      latitude: latitude,
      timestamp: DateTime(2026),
      accuracy: 5,
      altitude: 0,
      altitudeAccuracy: 0,
      heading: 0,
      headingAccuracy: 0,
      speed: 0,
      speedAccuracy: 0,
    );

class _RecordingEmergencyService extends EmergencyService {
  int openCalls = 0;
  int activeCalls = 0;
  EmergencySession? activeSession = const EmergencySession(
    sessionId: 'triage-session',
    userId: 'mother',
    status: 'ACTIVE',
    triggerSource: 'AI_TRIAGE',
  );
  Object? activeError;
  Object? openError;
  Object? shareError;
  int shareCalls = 0;
  double? sharedLatitude;
  double? sharedLongitude;

  @override
  Future<EmergencySession?> getActive() async {
    activeCalls++;
    if (activeError != null) throw activeError!;
    return activeSession;
  }

  @override
  Future<EmergencySession> openFlow({
    required String triggerSource,
    double? latitude,
    double? longitude,
  }) async {
    openCalls++;
    if (openError != null) throw openError!;
    return EmergencySession(
      sessionId: 'manual-$openCalls',
      userId: 'mother',
      status: 'ACTIVE',
      triggerSource: triggerSource,
    );
  }

  @override
  Future<LocationShareResult> shareCurrentLocation({
    required double latitude,
    required double longitude,
  }) async {
    shareCalls++;
    sharedLatitude = latitude;
    sharedLongitude = longitude;
    if (shareError != null) throw shareError!;
    return LocationShareResult(
      shareId: 'share-$shareCalls',
      recipientCount: 2,
      pushDeliveredCount: 2,
      sharedAt: DateTime.utc(2026, 8, 11, 1, 2),
    );
  }
}

class _QueuedEmergencyService extends _RecordingEmergencyService {
  final List<Completer<EmergencySession?>> requests = [];

  @override
  Future<EmergencySession?> getActive() {
    activeCalls++;
    final request = Completer<EmergencySession?>();
    requests.add(request);
    return request.future;
  }
}

class _QueuedLocationShareService extends _RecordingEmergencyService {
  final List<Completer<LocationShareResult>> shareRequests = [];

  @override
  Future<LocationShareResult> shareCurrentLocation({
    required double latitude,
    required double longitude,
  }) {
    shareCalls++;
    sharedLatitude = latitude;
    sharedLongitude = longitude;
    final request = Completer<LocationShareResult>();
    shareRequests.add(request);
    return request.future;
  }
}

class _StubContinuationCoordinator
    extends TriageContinuationRestoreCoordinator {
  _StubContinuationCoordinator(this.decisions, {this.error})
    : assert(decisions.isNotEmpty),
      super(store: SecureTriageContinuationStore(), gateway: TriageService());

  final List<TriageContinuationDecision> decisions;
  final Object? error;
  int calls = 0;

  @override
  Future<TriageContinuationDecision> restoreForUser(
    String userId, {
    bool resumeRedEmergency = true,
  }) async {
    calls++;
    if (error != null) throw error!;
    final index = calls <= decisions.length ? calls - 1 : decisions.length - 1;
    return decisions[index];
  }
}

class _FakeMapRenderer {
  _FakeMapRenderer({this.emitMapCreated = true});

  final bool emitMapCreated;
  final List<Key> rendererKeys = [];
  final List<VoidCallback> styleLoadedCallbacks = [];
  final List<void Function(TrackAsiaMapController?)> mapCreatedCallbacks = [];
  final List<bool> myLocationEnabledValues = [];

  int get builds => rendererKeys.length;

  Widget build({
    required Key key,
    required String styleString,
    required CameraPosition initialCameraPosition,
    required bool myLocationEnabled,
    required void Function(TrackAsiaMapController? controller) onMapCreated,
    required VoidCallback onStyleLoaded,
  }) {
    if (!rendererKeys.contains(key)) {
      rendererKeys.add(key);
      styleLoadedCallbacks.add(onStyleLoaded);
      mapCreatedCallbacks.add(onMapCreated);
      myLocationEnabledValues.add(myLocationEnabled);
      if (emitMapCreated) {
        WidgetsBinding.instance.addPostFrameCallback((_) => onMapCreated(null));
      }
    }
    return ColoredBox(key: key, color: Colors.blueGrey);
  }
}

void main() {
  setUp(() async {
    FlutterSecureStorage.setMockInitialValues({});
    await AuthState.instance.clear();
    await AuthState.instance.setTokens(
      accessToken: 'test-access',
      refreshToken: 'test-refresh',
      userId: 'account-a',
      role: 'MOTHER',
    );
  });

  tearDown(() async => AuthState.instance.clear());

  testWidgets('location stays unread when mother cancels consent disclosure', (
    tester,
  ) async {
    var locationReads = 0;
    var grantCalls = 0;
    await tester.pumpWidget(
      MaterialApp(
        home: EmergencyMapScreen(
          emergencyService: _RecordingEmergencyService(),
          facilityService: _RecordingFacilityService(),
          locationConsentProbe: () async => false,
          locationConsentGrant:
              ({
                required dataType,
                required purpose,
                required recipient,
                required scope,
              }) async {
                grantCalls++;
              },
          permissionService: SafetyPermissionService(
            locationReader: () async {
              locationReads++;
              return _position();
            },
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(locationReads, 0);
    await tester.tap(find.byKey(const Key('location-consent-action')));
    await tester.pumpAndSettle();
    expect(
      find.byKey(const Key('location-consent-disclosure')),
      findsOneWidget,
    );

    await tester.tap(find.byKey(const Key('location-consent-cancel')));
    await tester.pumpAndSettle();

    expect(grantCalls, 0);
    expect(locationReads, 0);
    expect(find.byKey(const Key('location-consent-action')), findsOneWidget);
  });

  testWidgets('accepted disclosure grants exact scope before geolocation', (
    tester,
  ) async {
    var consentActive = false;
    var grantCompleted = false;
    var locationReads = 0;
    late Map<String, String> granted;
    final facilities = _RecordingFacilityService();
    await tester.pumpWidget(
      MaterialApp(
        home: EmergencyMapScreen(
          emergencyService: _RecordingEmergencyService(),
          facilityService: facilities,
          locationConsentProbe: () async => consentActive,
          locationConsentGrant:
              ({
                required dataType,
                required purpose,
                required recipient,
                required scope,
              }) async {
                granted = {
                  'dataType': dataType,
                  'purpose': purpose,
                  'recipient': recipient,
                  'scope': scope,
                };
                consentActive = true;
                grantCompleted = true;
              },
          permissionService: SafetyPermissionService(
            locationReader: () async {
              expect(grantCompleted, isTrue);
              locationReads++;
              return _position();
            },
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('location-consent-action')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('location-consent-confirm')));
    await tester.pumpAndSettle();

    expect(granted, {
      'dataType': 'LOCATION',
      'purpose': 'SHARE',
      'recipient': 'CAREBRIDGE_SAFETY',
      'scope': 'SAFETY_EMERGENCY_ALERT',
    });
    expect(locationReads, 1);
    expect(facilities.searchCalls, 1);
  });

  testWidgets('consent grant failure leaves location blocked and retryable', (
    tester,
  ) async {
    var locationReads = 0;
    await tester.pumpWidget(
      MaterialApp(
        home: EmergencyMapScreen(
          emergencyService: _RecordingEmergencyService(),
          facilityService: _RecordingFacilityService(),
          locationConsentProbe: () async => false,
          locationConsentGrant:
              ({
                required dataType,
                required purpose,
                required recipient,
                required scope,
              }) async => throw StateError('grant failed'),
          permissionService: SafetyPermissionService(
            locationReader: () async {
              locationReads++;
              return _position();
            },
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('location-consent-action')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('location-consent-confirm')));
    await tester.pumpAndSettle();

    expect(locationReads, 0);
    expect(find.textContaining('Không thể lưu đồng ý'), findsOneWidget);
    expect(find.byKey(const Key('location-consent-action')), findsOneWidget);
    expect(find.text('Gọi cấp cứu 115'), findsOneWidget);
  });

  testWidgets('location action sends a fresh position to family accounts', (
    tester,
  ) async {
    final service = _RecordingEmergencyService();
    var locationReads = 0;
    await tester.pumpWidget(
      MaterialApp(
        home: EmergencyMapScreen(
          emergencyService: service,
          facilityService: _RecordingFacilityService(),
          locationConsentProbe: () async => true,
          permissionService: SafetyPermissionService(
            locationReader: () async {
              locationReads++;
              return locationReads == 1
                  ? _position(latitude: 10.70, longitude: 106.60)
                  : _position(
                      latitude: 10.762622123456,
                      longitude: 106.660172123456,
                    );
            },
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Gửi vị trí'), findsOneWidget);
    expect(find.text('Báo động gia đình'), findsNothing);
    await tester.tap(find.byKey(const Key('emergency-family-alert')));
    await tester.pumpAndSettle();

    expect(locationReads, 2);
    expect(service.shareCalls, 1);
    expect(service.sharedLatitude, 10.7626221);
    expect(service.sharedLongitude, 106.6601721);
    expect(find.text('Đã gửi vị trí'), findsOneWidget);
    expect(
      find.text('Đã gửi vị trí hiện tại cho 2 người thân.'),
      findsOneWidget,
    );
  });

  testWidgets(
    'restored postpartum triage route GETs active and hides pediatric map',
    (tester) async {
      final service = _RecordingEmergencyService();
      await tester.pumpWidget(
        MaterialApp(
          home: EmergencyMapScreen(
            emergencyService: service,
            triageHandoff: true,
            stage: 'POSTPARTUM',
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(service.activeCalls, 1);
      expect(service.openCalls, 0);
      expect(
        find.byKey(const Key('emergency-maternal-call-115')),
        findsOneWidget,
      );
      expect(find.textContaining('Bệnh viện Nhi Đồng'), findsNothing);
      expect(find.text('Chỉ đường'), findsNothing);
      expect(find.byKey(const Key('emergency-session-status')), findsNothing);
    },
  );

  testWidgets('failed 115 launcher keeps explicit manual-call guidance', (
    tester,
  ) async {
    final service = _RecordingEmergencyService();
    await tester.pumpWidget(
      MaterialApp(
        home: EmergencyMapScreen(
          emergencyService: service,
          triageHandoff: true,
          stage: 'POSTPARTUM',
          emergencyDialer: () async => false,
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('emergency-maternal-call-115')));
    await tester.pumpAndSettle();

    expect(
      find.textContaining('Hãy tự gọi 115 hoặc nhờ người bên cạnh gọi giúp'),
      findsOneWidget,
    );
  });

  testWidgets('production 115 launcher false result shows manual fallback', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: EmergencyMapScreen(
          emergencyService: _RecordingEmergencyService(),
          triageHandoff: true,
          stage: 'POSTPARTUM',
          locationConsentProbe: () async => false,
          uriLauncher: (_) async => false,
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('emergency-maternal-call-115')));
    await tester.pumpAndSettle();

    expect(
      find.textContaining('Hãy tự gọi 115 hoặc nhờ người bên cạnh gọi giúp'),
      findsOneWidget,
    );
  });

  testWidgets('production 115 launcher exception shows manual fallback', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: EmergencyMapScreen(
          emergencyService: _RecordingEmergencyService(),
          triageHandoff: true,
          stage: 'POSTPARTUM',
          locationConsentProbe: () async => false,
          uriLauncher: (_) => Future<bool>.error(StateError('no dialer')),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('emergency-maternal-call-115')));
    await tester.pumpAndSettle();

    expect(
      find.textContaining('Hãy tự gọi 115 hoặc nhờ người bên cạnh gọi giúp'),
      findsOneWidget,
    );
  });

  testWidgets('continuation exit exposes retry then restores safe dashboard', (
    tester,
  ) async {
    final coordinator = _StubContinuationCoordinator(const [
      TriageContinuationDecision(
        destination: TriageContinuationDestination.none,
        continuationToken: 'continuation-token',
        generation: 0,
        isRecoverable: true,
        requiresRetry: true,
      ),
      TriageContinuationDecision(
        destination: TriageContinuationDestination.safeDashboard,
        continuationToken: null,
        generation: null,
      ),
    ]);
    final router = GoRouter(
      initialLocation: '/emergency',
      routes: [
        GoRoute(
          path: '/emergency',
          builder: (_, _) => EmergencyMapScreen(
            emergencyService: _RecordingEmergencyService(),
            continuationCoordinator: coordinator,
            existingSession: const EmergencySession(
              sessionId: 'triage-session',
              userId: 'mother',
              status: 'ACTIVE',
              triggerSource: 'AI_TRIAGE',
            ),
            stage: 'POSTPARTUM',
            locationConsentProbe: () async => false,
          ),
        ),
        GoRoute(
          path: '/mother-home',
          builder: (_, _) => const Scaffold(body: Text('safe-dashboard')),
        ),
      ],
    );
    addTearDown(router.dispose);
    await tester.pumpWidget(MaterialApp.router(routerConfig: router));
    await tester.pumpAndSettle();

    await tester.tap(find.byTooltip('Quay lại'));
    await tester.pumpAndSettle();
    expect(
      find.byKey(const Key('emergency-continuation-exit-error')),
      findsOneWidget,
    );

    await tester.tap(
      find.byKey(const Key('emergency-continuation-exit-retry')),
    );
    await tester.pumpAndSettle();

    expect(find.text('safe-dashboard'), findsOneWidget);
    expect(coordinator.calls, 2);
  });

  testWidgets(
    'continuation exception offers safe dashboard without acknowledgement',
    (tester) async {
      final coordinator = _StubContinuationCoordinator(const [
        TriageContinuationDecision.none(),
      ], error: StateError('offline'));
      final router = GoRouter(
        initialLocation: '/emergency',
        routes: [
          GoRoute(
            path: '/emergency',
            builder: (_, _) => EmergencyMapScreen(
              emergencyService: _RecordingEmergencyService(),
              continuationCoordinator: coordinator,
              existingSession: const EmergencySession(
                sessionId: 'triage-session',
                userId: 'mother',
                status: 'ACTIVE',
                triggerSource: 'AI_TRIAGE',
              ),
              stage: 'POSTPARTUM',
              locationConsentProbe: () async => false,
            ),
          ),
          GoRoute(
            path: '/mother-home',
            builder: (_, _) => const Scaffold(body: Text('safe-dashboard')),
          ),
        ],
      );
      addTearDown(router.dispose);
      await tester.pumpWidget(MaterialApp.router(routerConfig: router));
      await tester.pumpAndSettle();

      await tester.tap(find.byTooltip('Quay lại'));
      await tester.pumpAndSettle();
      expect(
        find.byKey(const Key('emergency-continuation-exit-error')),
        findsOneWidget,
      );

      await tester.tap(
        find.byKey(const Key('emergency-continuation-safe-dashboard')),
      );
      await tester.pumpAndSettle();

      expect(find.text('safe-dashboard'), findsOneWidget);
      expect(coordinator.calls, 1);
    },
  );

  testWidgets('failed restored triage load never POSTs MANUAL and can retry', (
    tester,
  ) async {
    final service = _RecordingEmergencyService()
      ..activeError = StateError('offline');
    await tester.pumpWidget(
      MaterialApp(
        home: EmergencyMapScreen(
          emergencyService: service,
          triageHandoff: true,
          stage: 'POSTPARTUM',
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(service.activeCalls, 1);
    expect(service.openCalls, 0);
    expect(find.byKey(const Key('emergency-session-retry')), findsOneWidget);

    service.activeError = null;
    await tester.tap(find.byKey(const Key('emergency-session-retry')));
    await tester.pumpAndSettle();

    expect(service.activeCalls, 2);
    expect(service.openCalls, 0);
    expect(find.byKey(const Key('emergency-session-status')), findsNothing);
  });

  testWidgets('manual nearby-care entry does not open an emergency session', (
    tester,
  ) async {
    final service = _RecordingEmergencyService();
    await tester.pumpWidget(
      MaterialApp(home: EmergencyMapScreen(emergencyService: service)),
    );
    await tester.pump();

    expect(service.openCalls, 0);
    expect(service.activeCalls, 0);
  });

  testWidgets('manual nearby-care entry ignores emergency session failures', (
    tester,
  ) async {
    final service = _RecordingEmergencyService()
      ..activeSession = null
      ..openError = StateError('offline');
    await tester.pumpWidget(
      MaterialApp(home: EmergencyMapScreen(emergencyService: service)),
    );
    await tester.pump();

    expect(find.text('Gửi vị trí'), findsOneWidget);
    expect(find.byKey(const Key('emergency-session-retry')), findsNothing);
    expect(service.openCalls, 0);
    expect(service.shareCalls, 0);
    expect(find.byKey(const Key('emergency-session-status')), findsNothing);
  });

  testWidgets('triage active-session reconciliation is single-flight', (
    tester,
  ) async {
    final service = _QueuedEmergencyService();
    await tester.pumpWidget(
      MaterialApp(
        home: EmergencyMapScreen(
          emergencyService: service,
          triageHandoff: true,
          stage: 'POSTPARTUM',
        ),
      ),
    );
    await tester.pump();
    expect(service.requests, hasLength(1));

    final actionWhileLoading = tester.widget<OutlinedButton>(
      find.byKey(const Key('family-alert')),
    );
    expect(actionWhileLoading.onPressed, isNull);

    service.requests.single.complete(
      const EmergencySession(
        sessionId: 'new-active',
        userId: 'mother',
        status: 'ACTIVE',
        triggerSource: 'AI_TRIAGE',
      ),
    );
    await tester.pumpAndSettle();

    expect(service.openCalls, 0);
    expect(find.byKey(const Key('emergency-session-retry')), findsNothing);
  });

  testWidgets(
    'late active-session response from previous account is discarded',
    (tester) async {
      FlutterSecureStorage.setMockInitialValues({});
      await AuthState.instance.clear();
      await AuthState.instance.setTokens(
        accessToken: 'access-a',
        refreshToken: 'refresh-a',
        userId: 'account-a',
        role: 'MOTHER',
      );
      addTearDown(AuthState.instance.clear);
      final service = _QueuedEmergencyService();
      await tester.pumpWidget(
        MaterialApp(
          home: EmergencyMapScreen(
            emergencyService: service,
            triageHandoff: true,
            stage: 'POSTPARTUM',
          ),
        ),
      );
      await tester.pump();
      expect(service.requests, hasLength(1));

      await AuthState.instance.setTokens(
        accessToken: 'access-b',
        refreshToken: 'refresh-b',
        userId: 'account-b',
        role: 'MOTHER',
      );
      service.requests.single.complete(
        const EmergencySession(
          sessionId: 'account-a-emergency',
          userId: 'account-a',
          status: 'ACTIVE',
          triggerSource: 'AI_TRIAGE',
        ),
      );
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 100));

      expect(find.byKey(const Key('emergency-session-retry')), findsNothing);
      expect(service.openCalls, 0);
      final action = tester.widget<OutlinedButton>(
        find.byKey(const Key('family-alert')),
      );
      expect(action.onPressed, isNull);
    },
  );

  testWidgets(
    'late location-share response from previous account is discarded',
    (tester) async {
      final service = _QueuedLocationShareService();
      await tester.pumpWidget(
        MaterialApp(
          home: EmergencyMapScreen(
            emergencyService: service,
            facilityService: _RecordingFacilityService(),
            permissionService: SafetyPermissionService(
              locationReader: () async => _position(),
            ),
            locationConsentProbe: () async => true,
            existingSession: const EmergencySession(
              sessionId: 'account-a-emergency',
              userId: 'account-a',
              status: 'ACTIVE',
              triggerSource: 'AI_TRIAGE',
            ),
            stage: 'POSTPARTUM',
          ),
        ),
      );
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const Key('emergency-family-alert')));
      await tester.pump();
      expect(service.shareRequests, hasLength(1));
      await AuthState.instance.setTokens(
        accessToken: 'access-b',
        refreshToken: 'refresh-b',
        userId: 'account-b',
        role: 'MOTHER',
      );
      service.shareRequests.single.complete(
        LocationShareResult(
          shareId: 'late-share',
          recipientCount: 2,
          pushDeliveredCount: 2,
          sharedAt: DateTime.utc(2026, 8, 11, 1, 2),
        ),
      );
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 100));

      expect(find.byKey(const Key('emergency-session-retry')), findsNothing);
      expect(find.byType(SnackBar), findsNothing);
      final action = tester.widget<OutlinedButton>(
        find.byKey(const Key('family-alert')),
      );
      expect(action.onPressed, isNull);
    },
  );

  testWidgets('triage session retry disables location action independently', (
    tester,
  ) async {
    final service = _QueuedEmergencyService();
    await tester.pumpWidget(
      MaterialApp(
        home: EmergencyMapScreen(
          emergencyService: service,
          triageHandoff: true,
          stage: 'POSTPARTUM',
        ),
      ),
    );
    await tester.pump();
    service.requests[0].complete(null);
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('emergency-session-retry')));
    await tester.pump();
    expect(service.requests, hasLength(2));

    final actionWhileLoading = tester.widget<OutlinedButton>(
      find.byKey(const Key('family-alert')),
    );
    final retryWhileLoading = tester.widget<TextButton>(
      find.byKey(const Key('emergency-session-retry')),
    );
    expect(actionWhileLoading.onPressed, isNull);
    expect(retryWhileLoading.onPressed, isNull);

    service.requests[1].complete(
      const EmergencySession(
        sessionId: 'new-active',
        userId: 'mother',
        status: 'ACTIVE',
        triggerSource: 'AI_TRIAGE',
      ),
    );
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 100));

    final action = tester.widget<OutlinedButton>(
      find.byKey(const Key('family-alert')),
    );
    expect(action.onPressed, isNotNull);
    expect(service.shareCalls, 0);
    expect(find.byKey(const Key('emergency-session-retry')), findsNothing);
  });

  testWidgets('pediatric triage load failure is visible and retryable', (
    tester,
  ) async {
    final service = _RecordingEmergencyService()
      ..activeError = StateError('offline');
    await tester.pumpWidget(
      MaterialApp(
        home: EmergencyMapScreen(
          emergencyService: service,
          triageHandoff: true,
          stage: 'TODDLER',
        ),
      ),
    );
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 100));

    expect(service.openCalls, 0);
    expect(find.byKey(const Key('emergency-session-status')), findsOneWidget);
    expect(find.byKey(const Key('emergency-session-retry')), findsOneWidget);

    service.activeError = null;
    await tester.tap(find.byKey(const Key('emergency-session-retry')));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 100));
    expect(service.activeCalls, 2);
    expect(service.openCalls, 0);
  });

  testWidgets('non-ACTIVE supplied triage session is revalidated by GET', (
    tester,
  ) async {
    final service = _RecordingEmergencyService();
    await tester.pumpWidget(
      MaterialApp(
        home: EmergencyMapScreen(
          emergencyService: service,
          existingSession: const EmergencySession(
            sessionId: 'resolved',
            userId: 'mother',
            status: 'RESOLVED',
            triggerSource: 'AI_TRIAGE',
          ),
          stage: 'POSTPARTUM',
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(service.activeCalls, 1);
    expect(service.openCalls, 0);
    expect(find.byKey(const Key('emergency-session-retry')), findsNothing);
  });

  testWidgets('supplied active AI triage session never opens MANUAL flow', (
    tester,
  ) async {
    final service = _RecordingEmergencyService()..activeSession = null;
    await tester.pumpWidget(
      MaterialApp(
        home: EmergencyMapScreen(
          emergencyService: service,
          facilityService: _RecordingFacilityService(),
          permissionService: SafetyPermissionService(
            locationReader: () async => _position(),
          ),
          locationConsentProbe: () async => true,
          existingSession: const EmergencySession(
            sessionId: 'triage-session',
            userId: 'mother',
            status: 'ACTIVE',
            triggerSource: 'AI_TRIAGE',
          ),
          stage: 'POSTPARTUM',
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(service.activeCalls, 0);
    expect(service.openCalls, 0);
    expect(service.shareCalls, 0);
    expect(find.byKey(const Key('emergency-session-retry')), findsNothing);
  });

  testWidgets('stage is normalized before maternal routing', (tester) async {
    final service = _RecordingEmergencyService();
    await tester.pumpWidget(
      MaterialApp(
        home: EmergencyMapScreen(
          emergencyService: service,
          existingSession: const EmergencySession(
            sessionId: 'triage-session',
            userId: 'mother',
            status: 'ACTIVE',
            triggerSource: 'AI_TRIAGE',
          ),
          stage: ' postpartum ',
        ),
      ),
    );
    await tester.pump();

    expect(
      find.byKey(const Key('emergency-maternal-call-115')),
      findsOneWidget,
    );
    expect(find.textContaining('Bệnh viện Nhi Đồng'), findsNothing);
  });

  testWidgets('unsupported stage fails closed', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: EmergencyMapScreen(
          emergencyService: _RecordingEmergencyService(),
          stage: 'UNKNOWN',
        ),
      ),
    );
    expect(tester.takeException(), isA<ArgumentError>());
  });

  testWidgets('nearby hospital panel can be collapsed and expanded', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: EmergencyMapScreen(
          emergencyService: _RecordingEmergencyService(),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('radius-5000')), findsOneWidget);
    expect(find.byKey(const Key('facility-panel-toggle')), findsOneWidget);

    await tester.tap(find.byKey(const Key('facility-panel-toggle')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('radius-5000')), findsNothing);
    expect(find.byKey(const Key('emergency-call')), findsOneWidget);

    await tester.tap(find.byKey(const Key('facility-panel-toggle')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('radius-5000')), findsOneWidget);
  });

  testWidgets('TrackAsia style success clears renderer loading state', (
    tester,
  ) async {
    final renderer = _FakeMapRenderer();
    final syncedFacilities = <CareFacility>[];
    const facility = CareFacility(
      facilityId: 'hospital-1',
      name: 'Bệnh viện Test',
      latitude: 10.77,
      longitude: 106.67,
      sourceType: 'TRACKASIA',
    );
    await tester.pumpWidget(
      MaterialApp(
        home: EmergencyMapScreen(
          emergencyService: _RecordingEmergencyService(),
          facilityService: _RecordingFacilityService(const [facility]),
          locationConsentProbe: () async => true,
          permissionService: SafetyPermissionService(
            locationReader: () async => _position(),
          ),
          trackAsiaMapKey: 'test-map-key',
          mapRenderer: renderer.build,
          mapStyleLoadTimeout: const Duration(minutes: 1),
          annotationSynchronizer:
              ({required position, required facilities, route}) async {
                syncedFacilities.addAll(facilities);
              },
        ),
      ),
    );
    await tester.pump();
    await tester.pump();
    await tester.pump();

    expect(renderer.builds, 1);
    expect(renderer.myLocationEnabledValues, everyElement(isFalse));
    expect(find.text('Đang mở bản đồ TrackAsia...'), findsOneWidget);

    renderer.styleLoadedCallbacks.single();
    await tester.pump();

    expect(syncedFacilities, const [facility]);
    expect(find.text('Đang mở bản đồ TrackAsia...'), findsNothing);
    expect(find.byKey(const Key('trackasia-map-load-error')), findsNothing);
  });

  testWidgets(
    'TrackAsia timeout retries only the renderer and ignores stale callbacks',
    (tester) async {
      final renderer = _FakeMapRenderer(emitMapCreated: false);
      final facilities = _RecordingFacilityService();
      final emergency = _RecordingEmergencyService();
      var locationReads = 0;
      await tester.pumpWidget(
        MaterialApp(
          home: EmergencyMapScreen(
            emergencyService: emergency,
            facilityService: facilities,
            locationConsentProbe: () async => true,
            permissionService: SafetyPermissionService(
              locationReader: () async {
                locationReads++;
                return _position();
              },
            ),
            trackAsiaMapKey: 'test-map-key',
            mapRenderer: renderer.build,
            mapStyleLoadTimeout: const Duration(milliseconds: 50),
          ),
        ),
      );
      await tester.pump();
      await tester.pump();
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 60));

      expect(find.byKey(const Key('trackasia-map-load-error')), findsOneWidget);
      expect(renderer.builds, 1);
      expect(locationReads, 1);
      expect(facilities.searchCalls, 1);
      expect(emergency.openCalls, 0);

      await tester.tap(find.byKey(const Key('trackasia-map-retry')));
      await tester.pump();

      expect(renderer.builds, 2);
      expect(renderer.myLocationEnabledValues, everyElement(isFalse));
      expect(locationReads, 1);
      expect(facilities.searchCalls, 1);
      expect(emergency.openCalls, 0);

      renderer.styleLoadedCallbacks.first();
      await tester.pump();
      expect(find.text('Đang mở bản đồ TrackAsia...'), findsOneWidget);

      renderer.styleLoadedCallbacks.last();
      await tester.pump();
      expect(find.text('Đang mở bản đồ TrackAsia...'), findsNothing);
      expect(find.byKey(const Key('trackasia-map-load-error')), findsNothing);
    },
  );

  testWidgets('TrackAsia watchdog is cancelled when screen is disposed', (
    tester,
  ) async {
    final renderer = _FakeMapRenderer();
    await tester.pumpWidget(
      MaterialApp(
        home: EmergencyMapScreen(
          emergencyService: _RecordingEmergencyService(),
          facilityService: _RecordingFacilityService(),
          locationConsentProbe: () async => true,
          permissionService: SafetyPermissionService(
            locationReader: () async => _position(),
          ),
          trackAsiaMapKey: 'test-map-key',
          mapRenderer: renderer.build,
          mapStyleLoadTimeout: const Duration(milliseconds: 50),
        ),
      ),
    );
    await tester.pump();
    await tester.pump();
    await tester.pump();
    await tester.pumpWidget(const SizedBox.shrink());
    await tester.pump(const Duration(milliseconds: 60));
    renderer.mapCreatedCallbacks.single(null);
    renderer.styleLoadedCallbacks.single();
    await tester.pump();

    expect(tester.takeException(), isNull);
  });
}
