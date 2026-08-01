import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:geolocator/geolocator.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/features/aiTriage/models/triage_continuation.dart';
import 'package:untitled/features/aiTriage/services/triage_continuation_restore_coordinator.dart';
import 'package:untitled/features/aiTriage/services/triage_continuation_store.dart';
import 'package:untitled/features/aiTriage/services/triage_service.dart';
import 'package:untitled/features/emergency/models/emergency_session_model.dart';
import 'package:untitled/features/emergency/models/care_facility_model.dart';
import 'package:untitled/features/emergency/screens/emergency_map_screen.dart';
import 'package:untitled/features/emergency/services/care_facility_service.dart';
import 'package:untitled/features/emergency/services/emergency_service.dart';
import 'package:untitled/features/safety/services/safety_permission_service.dart';

class _RecordingFacilityService extends CareFacilityService {
  int searchCalls = 0;

  @override
  Future<List<CareFacility>> searchNearby({
    required double latitude,
    required double longitude,
    int radiusMeters = 5000,
    String type = 'hospital',
  }) async {
    searchCalls++;
    return const [];
  }
}

Position _position() => Position(
  longitude: 106.66,
  latitude: 10.76,
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

  testWidgets(
    'triage handoff checks notification request without claiming delivery',
    (tester) async {
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
          ),
        ),
      );
      await tester.pump();

      expect(service.openCalls, 0);
      await tester.tap(find.byKey(const Key('emergency-family-alert')));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 500));

      expect(service.openCalls, 0);
      expect(service.activeCalls, 1);
      expect(find.textContaining('đang được xử lý'), findsWidgets);
      expect(find.textContaining('Đã gửi báo động'), findsNothing);
    },
  );

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
      expect(find.textContaining('đang được xử lý'), findsOneWidget);
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
    expect(find.textContaining('đang được xử lý'), findsOneWidget);
  });

  testWidgets('manual entry still opens a MANUAL emergency session', (
    tester,
  ) async {
    final service = _RecordingEmergencyService();
    await tester.pumpWidget(
      MaterialApp(home: EmergencyMapScreen(emergencyService: service)),
    );
    await tester.pump();

    expect(service.openCalls, 1);
  });

  testWidgets('manual open failure shows explicit failure without success', (
    tester,
  ) async {
    final service = _RecordingEmergencyService()
      ..activeSession = null
      ..openError = StateError('offline');
    await tester.pumpWidget(
      MaterialApp(home: EmergencyMapScreen(emergencyService: service)),
    );
    await tester.pump();

    await tester.tap(find.byKey(const Key('emergency-family-alert')));
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 500));

    expect(service.openCalls, 2);
    expect(find.textContaining('đang được xử lý'), findsNothing);
    expect(find.text('Không thể gửi báo động. Hãy thử lại.'), findsOneWidget);
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
    expect(find.textContaining('đang hoạt động'), findsOneWidget);
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
    'late family-alert reconciliation from previous account is discarded',
    (tester) async {
      final service = _QueuedEmergencyService();
      await tester.pumpWidget(
        MaterialApp(
          home: EmergencyMapScreen(
            emergencyService: service,
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
      await tester.pump();

      await tester.tap(find.byKey(const Key('emergency-family-alert')));
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
      expect(find.byType(SnackBar), findsNothing);
      final action = tester.widget<OutlinedButton>(
        find.byKey(const Key('family-alert')),
      );
      expect(action.onPressed, isNull);
    },
  );

  testWidgets('single-flight retry unlocks family-alert action on completion', (
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

    await tester.tap(find.byKey(const Key('emergency-family-alert')));
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

  testWidgets('AI triage session never falls through to MANUAL open', (
    tester,
  ) async {
    final service = _RecordingEmergencyService()..activeSession = null;
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
          stage: 'POSTPARTUM',
        ),
      ),
    );
    await tester.pump();

    await tester.tap(find.byKey(const Key('emergency-family-alert')));
    await tester.pumpAndSettle();

    expect(service.activeCalls, 1);
    expect(service.openCalls, 0);
    expect(find.byKey(const Key('emergency-session-retry')), findsOneWidget);
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
}
