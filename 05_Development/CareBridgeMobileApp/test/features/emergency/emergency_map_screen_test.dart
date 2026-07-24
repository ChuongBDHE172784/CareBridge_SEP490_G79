import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/features/emergency/models/emergency_session_model.dart';
import 'package:untitled/features/emergency/screens/emergency_map_screen.dart';
import 'package:untitled/features/emergency/services/emergency_service.dart';

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

  testWidgets('manual open failure never reports a notification success', (
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
    expect(find.byType(SnackBar), findsNothing);
  });

  testWidgets('stale active-session response cannot overwrite newer ACTIVE', (
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

    await tester.tap(find.byKey(const Key('emergency-family-alert')));
    await tester.pump();
    expect(service.requests, hasLength(2));

    service.requests[1].complete(
      const EmergencySession(
        sessionId: 'new-active',
        userId: 'mother',
        status: 'ACTIVE',
        triggerSource: 'AI_TRIAGE',
      ),
    );
    await tester.pump();
    service.requests[0].complete(null);
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
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('emergency-session-retry')), findsOneWidget);
      expect(find.textContaining('Phiên đăng nhập'), findsOneWidget);
      expect(service.openCalls, 0);
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
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('emergency-session-retry')), findsOneWidget);
      expect(find.textContaining('Phiên đăng nhập'), findsOneWidget);
      expect(find.byType(SnackBar), findsNothing);
    },
  );

  testWidgets('newer retry cannot leave family-alert action locked', (
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
    await tester.tap(find.byKey(const Key('emergency-session-retry')));
    await tester.pump();
    expect(service.requests, hasLength(3));

    service.requests[2].complete(
      const EmergencySession(
        sessionId: 'new-active',
        userId: 'mother',
        status: 'ACTIVE',
        triggerSource: 'AI_TRIAGE',
      ),
    );
    service.requests[1].complete(null);
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 100));

    final action = tester.widget<OutlinedButton>(
      find.byKey(const Key('emergency-family-alert')),
    );
    expect(action.onPressed, isNotNull);
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
