import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/safety/models/imu_diagnostics_model.dart';
import 'package:untitled/features/safety/models/safety_config_model.dart';
import 'package:untitled/features/safety/screens/safety_monitoring_screen.dart';
import 'package:untitled/features/safety/widgets/safety_countdown_sheet.dart';

void main() {
  test('persists a safety response before rearming fall detection', () async {
    final response = Completer<SafetyEvent>();
    final order = <String>[];
    const updated = SafetyEvent(
      id: 'fall-1',
      eventType: 'SUSPECTED_FALL',
      magnitude: 10,
      status: 'CONFIRMED_SAFE',
      responseType: 'CONFIRMED_SAFE',
    );

    final operation = persistSafetyResponseThenRearm(
      beginResponse: () => order.add('begin'),
      persistResponse: () {
        order.add('persist');
        return response.future;
      },
      applyPersistedResponse: (_) => order.add('apply'),
      rearmDetector: () => order.add('rearm'),
      resolveEmergency: (_) async => order.add('resolve'),
    );

    expect(order, ['begin', 'persist']);
    response.complete(updated);
    expect(await operation, same(updated));
    expect(order, ['begin', 'persist', 'apply', 'rearm', 'resolve']);
  });

  test('rearms fall detection if persisting the response fails', () async {
    final order = <String>[];

    await expectLater(
      persistSafetyResponseThenRearm(
        beginResponse: () => order.add('begin'),
        persistResponse: () async {
          order.add('persist');
          throw StateError('offline');
        },
        applyPersistedResponse: (_) => order.add('apply'),
        rearmDetector: () => order.add('rearm'),
        resolveEmergency: (_) async => order.add('resolve'),
      ),
      throwsStateError,
    );

    expect(order, ['begin', 'persist', 'rearm']);
  });

  test('every simulated outcome performs zero external writes', () async {
    for (final result in <SafetyCountdownResult>[
      const SafetyCountdownResult.safe(),
      const SafetyCountdownResult.falsePositive(
        reasonCode: 'EXERCISE',
        reason: 'Exercise',
      ),
      const SafetyCountdownResult.help(),
      const SafetyCountdownResult.timeout(),
    ]) {
      var writes = 0;
      await dispatchSafetyCountdownResult(
        result: result,
        simulated: true,
        onSafe: () async => writes++,
        onFalsePositive: (_, _) async => writes++,
        onEmergency: () async => writes++,
      );
      expect(writes, isZero, reason: '${result.action} must remain local');
    }
  });

  test('real outcomes retain the production callbacks', () async {
    var safeWrites = 0;
    await dispatchSafetyCountdownResult(
      result: const SafetyCountdownResult.safe(),
      simulated: false,
      onSafe: () async => safeWrites++,
      onFalsePositive: (_, _) async {},
      onEmergency: () async {},
    );
    expect(safeWrites, 1);
  });

  test('rejects an open fall delivered after its alert was answered', () {
    final respondedAt = DateTime.utc(2026, 8, 13, 10, 0, 10);
    final answered = SafetyEvent(
      id: 'answered-fall',
      eventType: 'SUSPECTED_FALL',
      magnitude: 30,
      status: 'CONFIRMED_SAFE',
      responseType: 'CONFIRMED_SAFE',
      detectedAt: respondedAt.subtract(const Duration(seconds: 2)),
    );
    final stale = SafetyEvent(
      id: 'stale-fall',
      eventType: 'SUSPECTED_FALL',
      magnitude: 30,
      status: 'OPEN',
      detectedAt: respondedAt.subtract(const Duration(seconds: 1)),
    );
    final laterFall = SafetyEvent(
      id: 'new-fall',
      eventType: 'SUSPECTED_FALL',
      magnitude: 30,
      status: 'OPEN',
      detectedAt: respondedAt.add(const Duration(seconds: 30)),
    );

    expect(
      isFallEventStaleAfterAlertResponse(
        stale,
        respondedAt,
        answeredEvent: answered,
      ),
      isTrue,
    );
    expect(
      isFallEventStaleAfterAlertResponse(
        laterFall,
        respondedAt,
        answeredEvent: answered,
      ),
      isFalse,
    );
  });

  test('never suppresses a fall because of client clock skew', () {
    // The API tolerates a client clock running minutes ahead of the server, so
    // a device-clock response marker must not decide that a backend-stamped
    // fall from a different incident is stale.
    final serverNow = DateTime.utc(2026, 8, 13, 10);
    final skewedResponseMarker = serverNow.add(const Duration(minutes: 1));
    final answered = SafetyEvent(
      id: 'answered-fall',
      eventType: 'SUSPECTED_FALL',
      magnitude: 30,
      status: 'CONFIRMED_SAFE',
      responseType: 'CONFIRMED_SAFE',
      detectedAt: serverNow,
    );
    final genuineNewFall = SafetyEvent(
      id: 'new-fall',
      eventType: 'SUSPECTED_FALL',
      magnitude: 30,
      status: 'OPEN',
      detectedAt: serverNow.add(const Duration(seconds: 20)),
    );

    expect(
      isFallEventStaleAfterAlertResponse(
        genuineNewFall,
        skewedResponseMarker,
        answeredEvent: answered,
        evaluatedAt: skewedResponseMarker,
      ),
      isFalse,
    );
  });

  test('stops suppressing once the response marker has expired', () {
    final respondedAt = DateTime.utc(2026, 8, 13, 10);
    final answered = SafetyEvent(
      id: 'answered-fall',
      eventType: 'SUSPECTED_FALL',
      magnitude: 30,
      status: 'CONFIRMED_SAFE',
      responseType: 'CONFIRMED_SAFE',
      detectedAt: respondedAt,
    );
    final sameIncident = SafetyEvent(
      id: 'stale-fall',
      eventType: 'SUSPECTED_FALL',
      magnitude: 30,
      status: 'OPEN',
      detectedAt: respondedAt.add(const Duration(seconds: 1)),
    );

    expect(
      isFallEventStaleAfterAlertResponse(
        sameIncident,
        respondedAt,
        answeredEvent: answered,
        evaluatedAt: respondedAt.add(const Duration(seconds: 5)),
      ),
      isTrue,
    );
    expect(
      isFallEventStaleAfterAlertResponse(
        sameIncident,
        respondedAt,
        answeredEvent: answered,
        evaluatedAt: respondedAt.add(
          safetyAlertResponseStaleWindow + const Duration(seconds: 1),
        ),
      ),
      isFalse,
    );
  });

  test('sensor rehearsal routes safe action to onSafe', () async {
    var safeCalled = false;
    await dispatchSensorSelfTestCountdownResult(
      result: const SafetyCountdownResult.safe(),
      onSafe: () async => safeCalled = true,
      onFalsePositive: (_, _) async {},
      onComplete: (_) async {},
    );
    expect(safeCalled, isTrue);
  });

  test('sensor rehearsal routes help to safe test completion only', () async {
    var emergencyWrites = 0;
    String? completedOutcome;

    await dispatchSensorSelfTestCountdownResult(
      result: const SafetyCountdownResult.help(),
      onSafe: () async {},
      onFalsePositive: (_, _) async {},
      onComplete: (outcome) async => completedOutcome = outcome,
    );

    expect(completedOutcome, 'NEED_HELP');
    expect(emergencyWrites, isZero);
  });

  test('sensor rehearsal timeout is persisted as test timeout', () async {
    String? completedOutcome;

    await dispatchSensorSelfTestCountdownResult(
      result: const SafetyCountdownResult.timeout(),
      onSafe: () async {},
      onFalsePositive: (_, _) async {},
      onComplete: (outcome) async => completedOutcome = outcome,
    );

    expect(completedOutcome, 'TIMEOUT');
  });

  test('TEST_OPEN rehearsal participates in pending countdown selection', () {
    final now = DateTime.utc(2026, 8, 13, 10);
    final rehearsal = SafetyEvent(
      id: 'self-test-1',
      eventType: 'SENSOR_SELF_TEST',
      magnitude: 17.2,
      status: 'TEST_OPEN',
      countdownDeadlineAt: now.add(const Duration(seconds: 30)),
    );

    expect(isPendingSafetyCountdown(rehearsal), isTrue);
    expect(
      selectNextOpenSafetyEvent([rehearsal], excludingId: 'other', now: now),
      same(rehearsal),
    );
  });

  test('selects a queued real OPEN event after local simulation closes', () {
    final now = DateTime.utc(2026, 8, 13, 10);
    const simulationId = 'local-simulation-1';
    const resolved = SafetyEvent(
      id: 'resolved',
      eventType: 'SUSPECTED_FALL',
      magnitude: 20,
      status: 'RESOLVED',
    );
    final simulation = SafetyEvent(
      id: simulationId,
      eventType: 'SUSPECTED_FALL',
      magnitude: 30,
      status: 'OPEN',
      countdownDeadlineAt: now.add(const Duration(seconds: 30)),
    );
    final realEvent = SafetyEvent(
      id: 'server-event-1',
      eventType: 'SUSPECTED_FALL',
      magnitude: 31,
      status: 'OPEN',
      countdownDeadlineAt: now.add(const Duration(seconds: 30)),
    );

    final next = selectNextOpenSafetyEvent(
      [resolved, simulation, realEvent],
      excludingId: simulationId,
      now: now,
    );

    expect(next, same(realEvent));
  });

  test('a completed countdown cannot release a newer countdown owner', () {
    expect(
      shouldReleaseSafetyCountdownOwnership(
        activeEventId: 'fall-2',
        completedEventId: 'fall-1',
      ),
      isFalse,
    );
    expect(
      shouldReleaseSafetyCountdownOwnership(
        activeEventId: 'fall-1',
        completedEventId: 'fall-1',
      ),
      isTrue,
    );
  });

  test('suppresses a second event produced by the same physical fall', () {
    final now = DateTime.utc(2026, 8, 13, 10);
    final first = SafetyEvent(
      id: 'fall-1',
      eventType: 'SUSPECTED_FALL',
      magnitude: 9,
      status: 'OPEN',
      detectedAt: DateTime.utc(2026, 8, 11, 10),
    );
    final duplicate = SafetyEvent(
      id: 'fall-2',
      eventType: 'SUSPECTED_FALL',
      magnitude: 10,
      status: 'OPEN',
      detectedAt: DateTime.utc(2026, 8, 11, 10, 0, 7),
    );

    expect(isLikelyDuplicateFallEvent(first, duplicate), isTrue);
    expect(
      selectNextOpenSafetyEvent(
        [duplicate],
        excludingId: first.id,
        suppressedIds: {duplicate.id},
        now: now,
      ),
      isNull,
    );
  });

  test('does not suppress a new fall after the previous alert is safe', () {
    final previous = SafetyEvent(
      id: 'fall-1',
      eventType: 'SUSPECTED_FALL',
      magnitude: 9,
      status: 'CONFIRMED_SAFE',
      responseType: 'CONFIRMED_SAFE',
      respondedAt: DateTime.utc(2026, 8, 13, 10, 0, 1),
      detectedAt: DateTime.utc(2026, 8, 13, 10),
    );
    final nextFall = SafetyEvent(
      id: 'fall-2',
      eventType: 'SUSPECTED_FALL',
      magnitude: 10,
      status: 'OPEN',
      detectedAt: DateTime.utc(2026, 8, 13, 10, 0, 2),
    );

    expect(isLikelyDuplicateFallEvent(previous, nextFall), isFalse);
  });

  test('countdown presentation rejects expired and terminal events', () {
    final now = DateTime.utc(2026, 8, 13, 10);
    final eligible = SafetyEvent(
      id: 'eligible',
      eventType: 'SUSPECTED_FALL',
      magnitude: 12,
      status: 'OPEN',
      countdownDeadlineAt: now.add(const Duration(seconds: 1)),
    );
    final expired = SafetyEvent(
      id: 'expired',
      eventType: 'SUSPECTED_FALL',
      magnitude: 12,
      status: 'OPEN',
      countdownDeadlineAt: now,
    );
    final terminal = SafetyEvent(
      id: 'terminal',
      eventType: 'SUSPECTED_FALL',
      magnitude: 12,
      status: 'CONFIRMED_SAFE',
      countdownDeadlineAt: now.add(const Duration(seconds: 30)),
    );
    const missingDeadline = SafetyEvent(
      id: 'missing-deadline',
      eventType: 'SUSPECTED_FALL',
      magnitude: 12,
      status: 'OPEN',
    );

    expect(isSafetyCountdownPresentationEligible(eligible, now), isTrue);
    expect(isSafetyCountdownPresentationEligible(expired, now), isFalse);
    expect(isSafetyCountdownPresentationEligible(terminal, now), isFalse);
    expect(
      isSafetyCountdownPresentationEligible(missingDeadline, now),
      isFalse,
    );
  });

  test('selection skips expired events and keeps a pending event', () {
    final now = DateTime.utc(2026, 8, 13, 10);
    final expired = SafetyEvent(
      id: 'expired',
      eventType: 'SUSPECTED_FALL',
      magnitude: 12,
      status: 'OPEN',
      countdownDeadlineAt: now.subtract(const Duration(milliseconds: 1)),
    );
    final pending = SafetyEvent(
      id: 'pending',
      eventType: 'SUSPECTED_FALL',
      magnitude: 12,
      status: 'OPEN',
      countdownDeadlineAt: now.add(const Duration(seconds: 20)),
    );

    expect(
      selectNextOpenSafetyEvent(
        [expired, pending],
        excludingId: 'completed',
        now: now,
      ),
      same(pending),
    );
  });

  test('selection favors the pending event with the nearest deadline', () {
    final now = DateTime.utc(2026, 8, 13, 10);
    final later = SafetyEvent(
      id: 'later',
      eventType: 'SUSPECTED_FALL',
      magnitude: 12,
      status: 'OPEN',
      countdownDeadlineAt: now.add(const Duration(seconds: 25)),
    );
    final urgent = SafetyEvent(
      id: 'urgent',
      eventType: 'SUSPECTED_FALL',
      magnitude: 12,
      status: 'OPEN',
      countdownDeadlineAt: now.add(const Duration(seconds: 5)),
    );

    expect(
      selectNextOpenSafetyEvent(
        [later, urgent],
        excludingId: 'completed',
        now: now,
      ),
      same(urgent),
    );
  });

  test(
    'closes a suppressed duplicate and resolves its emergency session',
    () async {
      const duplicate = SafetyEvent(
        id: 'fall-duplicate',
        eventType: 'SUSPECTED_FALL',
        magnitude: 10,
        status: 'OPEN',
      );
      const closed = SafetyEvent(
        id: 'fall-duplicate',
        eventType: 'SUSPECTED_FALL',
        magnitude: 10,
        status: 'FALSE_POSITIVE',
        responseType: 'FALSE_POSITIVE',
        emergencySessionId: 'emergency-1',
      );
      String? reportedEventId;
      String? reportedNote;
      SafetyEvent? resolvedEvent;

      final result = await closeDuplicateFallEvent(
        event: duplicate,
        reportFalsePositive: (eventId, {note}) async {
          reportedEventId = eventId;
          reportedNote = note;
          return closed;
        },
        resolveEmergency: (event) async => resolvedEvent = event,
      );

      expect(result, same(closed));
      expect(reportedEventId, duplicate.id);
      expect(reportedNote, contains('gộp bản ghi trùng'));
      expect(resolvedEvent, same(closed));
    },
  );

  test('queue uses refreshed canonical state and drops terminal snapshots', () {
    final now = DateTime.utc(2026, 8, 13, 10);
    final queued = SafetyEvent(
      id: 'server-event-2',
      eventType: 'SUSPECTED_FALL',
      magnitude: 32,
      status: 'OPEN',
      countdownDeadlineAt: now.add(const Duration(seconds: 30)),
    );
    final terminalRefresh = SafetyEvent(
      id: queued.id,
      eventType: queued.eventType,
      magnitude: queued.magnitude,
      status: 'CONFIRMED_SAFE',
      countdownDeadlineAt: queued.countdownDeadlineAt,
    );
    final queue = SafetyRealEventQueue()
      ..enqueue(queued)
      ..enqueue(queued);

    expect(
      queue.takeNext(
        authoritativeEvents: [terminalRefresh],
        excludingId: 'completed',
        now: now,
      ),
      isNull,
    );
    expect(
      queue.takeNext(
        authoritativeEvents: [queued],
        excludingId: 'completed',
        now: now,
      ),
      isNull,
      reason: 'terminal reconciliation must remove the stale queued snapshot',
    );
  });

  test(
    'queue returns refreshed canonical data instead of its old snapshot',
    () {
      final now = DateTime.utc(2026, 8, 13, 10);
      final queued = SafetyEvent(
        id: 'server-event-canonical',
        eventType: 'SUSPECTED_FALL',
        magnitude: 20,
        status: 'OPEN',
        countdownDeadlineAt: now.add(const Duration(seconds: 30)),
      );
      final refreshed = SafetyEvent(
        id: queued.id,
        eventType: queued.eventType,
        magnitude: 25,
        status: 'OPEN',
        countdownDeadlineAt: now.add(const Duration(seconds: 20)),
      );
      final queue = SafetyRealEventQueue()..enqueue(queued);

      expect(
        queue.takeNext(
          authoritativeEvents: [refreshed],
          excludingId: 'completed',
          now: now,
        ),
        same(refreshed),
      );
    },
  );

  test('authoritative refresh drops a pre-existing queue entry it omits', () {
    final now = DateTime.utc(2026, 8, 13, 10);
    final queued = SafetyEvent(
      id: 'queued-before-refresh',
      eventType: 'SUSPECTED_FALL',
      magnitude: 30,
      status: 'OPEN',
      countdownDeadlineAt: now.add(const Duration(seconds: 30)),
    );
    final queue = SafetyRealEventQueue()..enqueue(queued);
    final refreshWatermark = queue.snapshotIds();

    expect(
      queue.takeNext(
        authoritativeEvents: const [],
        excludingId: 'completed',
        now: now,
        requireCanonicalIds: refreshWatermark,
      ),
      isNull,
    );
  });

  test(
    'refresh preserves a streamed event that arrived after its watermark',
    () {
      final now = DateTime.utc(2026, 8, 13, 10);
      final queue = SafetyRealEventQueue();
      final refreshWatermark = queue.snapshotIds();
      final streamed = SafetyEvent(
        id: 'streamed-during-refresh',
        eventType: 'SUSPECTED_FALL',
        magnitude: 30,
        status: 'OPEN',
        countdownDeadlineAt: now.add(const Duration(seconds: 30)),
      );
      queue.enqueue(streamed);

      expect(
        queue.takeNext(
          authoritativeEvents: const [],
          excludingId: 'completed',
          now: now,
          requireCanonicalIds: refreshWatermark,
        ),
        same(streamed),
      );
    },
  );

  test('fallback queue favors the omitted event with the nearest deadline', () {
    final now = DateTime.utc(2026, 8, 13, 10);
    final later = SafetyEvent(
      id: 'streamed-later',
      eventType: 'SUSPECTED_FALL',
      magnitude: 30,
      status: 'OPEN',
      countdownDeadlineAt: now.add(const Duration(seconds: 25)),
    );
    final urgent = SafetyEvent(
      id: 'streamed-urgent',
      eventType: 'SUSPECTED_FALL',
      magnitude: 30,
      status: 'OPEN',
      countdownDeadlineAt: now.add(const Duration(seconds: 5)),
    );
    final queue = SafetyRealEventQueue()
      ..enqueue(later)
      ..enqueue(urgent);

    expect(
      queue.takeNext(
        authoritativeEvents: const [],
        excludingId: 'completed',
        now: now,
      ),
      same(urgent),
    );
  });

  test(
    'suppressed duplicate entries are removed from the real-event queue',
    () {
      final now = DateTime.utc(2026, 8, 13, 10);
      final duplicate = SafetyEvent(
        id: 'suppressed-duplicate',
        eventType: 'SUSPECTED_FALL',
        magnitude: 30,
        status: 'OPEN',
        countdownDeadlineAt: now.add(const Duration(seconds: 30)),
      );
      final queue = SafetyRealEventQueue()..enqueue(duplicate);

      expect(
        queue.takeNext(
          authoritativeEvents: [duplicate],
          excludingId: 'completed',
          now: now,
          suppressedIds: {duplicate.id},
        ),
        isNull,
      );
      expect(queue.hasPending, isFalse);
    },
  );

  test('excluded completed entry is removed from the real-event queue', () {
    final now = DateTime.utc(2026, 8, 13, 10);
    final completed = SafetyEvent(
      id: 'just-completed',
      eventType: 'SUSPECTED_FALL',
      magnitude: 30,
      status: 'OPEN',
      countdownDeadlineAt: now.add(const Duration(seconds: 30)),
    );
    final queue = SafetyRealEventQueue()..enqueue(completed);

    expect(
      queue.takeNext(
        authoritativeEvents: [completed],
        excludingId: completed.id,
        now: now,
      ),
      isNull,
    );
    expect(queue.hasPending, isFalse);
  });

  test('expired queue entries do not keep refresh retries eligible', () {
    final now = DateTime.utc(2026, 8, 13, 10);
    final expired = SafetyEvent(
      id: 'expired-retry',
      eventType: 'SUSPECTED_FALL',
      magnitude: 30,
      status: 'OPEN',
      countdownDeadlineAt: now,
    );
    final queue = SafetyRealEventQueue()..enqueue(expired);

    expect(queue.hasPresentableEvent(now), isFalse);
  });

  test('three sequential safe cycles select each event once and then stop', () {
    final now = DateTime.utc(2026, 8, 13, 10);
    SafetyEvent event(String id, String status) => SafetyEvent(
      id: id,
      eventType: 'SUSPECTED_FALL',
      magnitude: 20,
      status: status,
      countdownDeadlineAt: now.add(const Duration(seconds: 30)),
    );

    final fall1 = event('fall-1', 'OPEN');
    final fall2 = event('fall-2', 'OPEN');
    final fall3 = event('fall-3', 'OPEN');
    final queue = SafetyRealEventQueue()
      ..enqueue(fall1)
      ..enqueue(fall1)
      ..enqueue(fall2)
      ..enqueue(fall3);

    expect(
      queue
          .takeNext(
            authoritativeEvents: [fall1, fall2, fall3],
            excludingId: 'none',
            now: now,
          )
          ?.id,
      'fall-1',
    );
    expect(
      queue
          .takeNext(
            authoritativeEvents: [
              event('fall-1', 'CONFIRMED_SAFE'),
              fall2,
              fall3,
            ],
            excludingId: 'fall-1',
            now: now,
          )
          ?.id,
      'fall-2',
    );
    expect(
      queue
          .takeNext(
            authoritativeEvents: [
              event('fall-1', 'CONFIRMED_SAFE'),
              event('fall-2', 'CONFIRMED_SAFE'),
              fall3,
            ],
            excludingId: 'fall-2',
            now: now,
          )
          ?.id,
      'fall-3',
    );
    expect(
      queue.takeNext(
        authoritativeEvents: [
          event('fall-1', 'CONFIRMED_SAFE'),
          event('fall-2', 'CONFIRMED_SAFE'),
          event('fall-3', 'CONFIRMED_SAFE'),
        ],
        excludingId: 'fall-3',
        now: now,
      ),
      isNull,
    );
  });

  test('simulation requires a fresh sampling diagnostic', () {
    const config = SafetyConfig(
      fallDetectionEnabled: true,
      sensitivityLevel: 'MEDIUM',
      emergencyAutoAlert: true,
      sensorPermissionGranted: true,
    );
    final now = DateTime.utc(2026, 8, 4, 10);
    final fresh = ImuDiagnosticsSnapshot(
      generation: 1,
      state: ImuSamplingState.sampling,
      capturedAt: now.subtract(const Duration(milliseconds: 500)),
    );
    final stale = ImuDiagnosticsSnapshot(
      generation: 1,
      state: ImuSamplingState.sampling,
      capturedAt: now.subtract(const Duration(seconds: 3)),
    );

    expect(
      isSafeFallSimulationEligible(
        config: config,
        coordinatorRunning: true,
        diagnostics: fresh,
        now: now,
      ),
      isTrue,
    );
    expect(
      isSafeFallSimulationEligible(
        config: config,
        coordinatorRunning: true,
        diagnostics: stale,
        now: now,
      ),
      isFalse,
    );
  });

  test('product sensor self-test requires enabled and running IMU', () {
    const enabled = SafetyConfig(
      fallDetectionEnabled: true,
      sensitivityLevel: 'MEDIUM',
      emergencyAutoAlert: true,
      sensorPermissionGranted: true,
    );
    const missingPermission = SafetyConfig(
      fallDetectionEnabled: true,
      sensitivityLevel: 'MEDIUM',
      emergencyAutoAlert: true,
      sensorPermissionGranted: false,
    );

    expect(
      isSensorSelfTestEligible(config: enabled, coordinatorRunning: true),
      isTrue,
    );
    expect(
      isSensorSelfTestEligible(
        config: missingPermission,
        coordinatorRunning: true,
      ),
      isFalse,
    );
    expect(
      isSensorSelfTestEligible(config: enabled, coordinatorRunning: false),
      isFalse,
    );
  });

  test('sensor self-test accepts only results captured after arming', () {
    final armedAt = DateTime.utc(2026, 8, 4, 10);

    expect(
      shouldAcceptSensorSelfTestResult(
        armed: true,
        armedAt: armedAt,
        detectedAt: armedAt.add(const Duration(milliseconds: 20)),
      ),
      isTrue,
    );
    expect(
      shouldAcceptSensorSelfTestResult(
        armed: true,
        armedAt: armedAt,
        detectedAt: armedAt.subtract(const Duration(milliseconds: 1)),
      ),
      isFalse,
    );
    expect(
      shouldAcceptSensorSelfTestResult(
        armed: false,
        armedAt: armedAt,
        detectedAt: armedAt.add(const Duration(milliseconds: 20)),
      ),
      isFalse,
    );
  });
}
