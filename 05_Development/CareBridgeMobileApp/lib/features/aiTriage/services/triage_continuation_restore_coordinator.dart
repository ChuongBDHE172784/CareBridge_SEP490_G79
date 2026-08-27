import '../models/triage_continuation.dart';
import 'triage_continuation_store.dart';

typedef LoadAuthoritativeEmergency = Future<String?> Function();
typedef OpenEmergency = Future<String> Function();

class TriageContinuationArrival {
  const TriageContinuationArrival({
    required this.userId,
    required this.decision,
    required this.coordinator,
  });

  final String userId;
  final TriageContinuationDecision decision;
  final TriageContinuationRestoreCoordinator coordinator;

  Future<bool> acknowledge() => coordinator.acknowledgeAfterDestinationRendered(
    userId: userId,
    decision: decision,
  );
}

class TriageContinuationRestoreCoordinator {
  TriageContinuationRestoreCoordinator({
    required TriageContinuationStore store,
    required TriageContinuationGateway gateway,
    LoadAuthoritativeEmergency? loadAuthoritativeEmergency,
    OpenEmergency? openEmergency,
  }) : _store = store,
       _gateway = gateway,
       _loadAuthoritativeEmergency = loadAuthoritativeEmergency;

  final TriageContinuationStore _store;
  final TriageContinuationGateway _gateway;
  final LoadAuthoritativeEmergency? _loadAuthoritativeEmergency;
  final Map<String, _AcknowledgedContinuationCleanup>
  _acknowledgedPendingCleanup = {};

  Future<TriageContinuationDecision> restoreForUser(
    String userId, {
    bool resumeRedEmergency = true,
  }) async {
    final generation = _store.generationFor(userId);
    PendingTriageContinuation? pending;
    try {
      pending = await _store.read(userId);
    } catch (_) {
      return const TriageContinuationDecision(
        destination: TriageContinuationDestination.none,
        continuationToken: null,
        generation: null,
        isRecoverable: true,
        requiresRetry: true,
      );
    }
    if (pending == null || generation != _store.generationFor(userId)) {
      return const TriageContinuationDecision.none();
    }

    try {
      final resolution = await _gateway.resolve(pending.token);
      if (generation != _store.generationFor(userId)) {
        return const TriageContinuationDecision.none();
      }

      if (resolution.riskLevel.toUpperCase() == 'RED' && resumeRedEmergency) {
        final emergencyId = await _loadAuthoritativeEmergency?.call();
        if (generation != _store.generationFor(userId)) {
          return const TriageContinuationDecision.none();
        }
        return TriageContinuationDecision(
          destination: TriageContinuationDestination.emergency,
          continuationToken: pending.token,
          generation: generation,
          authoritativeEmergencyId: emergencyId,
          riskLevel: resolution.riskLevel,
          stage: resolution.stage,
        );
      }

      return TriageContinuationDecision(
        destination: _destinationFor(resolution.originDashboard),
        continuationToken: pending.token,
        generation: generation,
        originReferenceId: resolution.originReferenceId,
        riskLevel: resolution.riskLevel,
        stage: resolution.stage,
        showRecordedConfirmation: true,
        confirmationUsesRiskColorOnly: false,
      );
    } on TriageContinuationFailure catch (failure) {
      if (generation != _store.generationFor(userId)) {
        return const TriageContinuationDecision.none();
      }
      try {
        await _store.invalidateUser(userId);
      } catch (_) {
        final currentGeneration = _store.generationFor(userId);
        if (currentGeneration < generation ||
            currentGeneration > generation + 1) {
          return const TriageContinuationDecision.none();
        }
        return TriageContinuationDecision(
          destination: TriageContinuationDestination.none,
          continuationToken: pending.token,
          generation: currentGeneration,
          isRecoverable: true,
          requiresRetry: true,
        );
      }
      if (_store.generationFor(userId) != generation + 1) {
        return const TriageContinuationDecision.none();
      }
      return TriageContinuationDecision(
        destination: TriageContinuationDestination.safeDashboard,
        continuationToken: null,
        generation: null,
        isRecoverable: failure.kind == TriageContinuationFailureKind.conflict,
      );
    } catch (_) {
      if (generation != _store.generationFor(userId)) {
        return const TriageContinuationDecision.none();
      }
      return TriageContinuationDecision(
        destination: TriageContinuationDestination.none,
        continuationToken: pending.token,
        generation: generation,
        isRecoverable: true,
        requiresRetry: true,
      );
    }
  }

  Future<bool> acknowledgeAfterDestinationRendered({
    required String userId,
    required TriageContinuationDecision decision,
  }) async {
    final token = decision.continuationToken;
    final generation = decision.generation;
    if (token == null || generation == null) {
      return false;
    }

    final pendingCleanup = _acknowledgedPendingCleanup[userId];
    if (pendingCleanup != null) {
      if (pendingCleanup.token != token) return false;
      return _cleanupAcknowledgedContinuation(userId);
    }
    if (generation != _store.generationFor(userId)) return false;

    try {
      final current = await _store.read(userId);
      if (current?.token != token ||
          generation != _store.generationFor(userId)) {
        return false;
      }
      await _gateway.acknowledge(token);
      if (generation != _store.generationFor(userId)) return false;
      _acknowledgedPendingCleanup[userId] = _AcknowledgedContinuationCleanup(
        token: token,
        generation: generation,
      );
      return _cleanupAcknowledgedContinuation(userId);
    } catch (_) {
      return false;
    }
  }

  Future<bool> _cleanupAcknowledgedContinuation(String userId) async {
    final cleanup = _acknowledgedPendingCleanup[userId];
    if (cleanup == null) return false;
    if (_store.generationFor(userId) != cleanup.generation) {
      _acknowledgedPendingCleanup.remove(userId);
      return false;
    }

    try {
      final current = await _store.read(userId);
      if (current?.token != cleanup.token ||
          _store.generationFor(userId) != cleanup.generation) {
        _acknowledgedPendingCleanup.remove(userId);
        return false;
      }
      try {
        await _store.invalidateUser(userId);
      } catch (_) {
        final currentGeneration = _store.generationFor(userId);
        if (currentGeneration == cleanup.generation ||
            currentGeneration == cleanup.generation + 1) {
          _acknowledgedPendingCleanup[userId] =
              _AcknowledgedContinuationCleanup(
                token: cleanup.token,
                generation: currentGeneration,
              );
        } else {
          _acknowledgedPendingCleanup.remove(userId);
        }
        return false;
      }
      _acknowledgedPendingCleanup.remove(userId);
      return true;
    } catch (_) {
      return false;
    }
  }

  Future<void> onAccountSwitch({
    required String previousUserId,
    required String nextUserId,
  }) async {
    _acknowledgedPendingCleanup.remove(previousUserId);
    try {
      await _store.invalidateUser(previousUserId);
    } catch (_) {
      // Account state still changes immediately; stale generations are also
      // checked after every continuation await before any navigation.
    }
  }

  static TriageContinuationDestination _destinationFor(
    TriageOriginDashboard origin,
  ) => switch (origin) {
    TriageOriginDashboard.motherJourney =>
      TriageContinuationDestination.motherJourney,
    TriageOriginDashboard.babyProfile =>
      TriageContinuationDestination.babyProfile,
  };
}

class _AcknowledgedContinuationCleanup {
  const _AcknowledgedContinuationCleanup({
    required this.token,
    required this.generation,
  });

  final String token;
  final int generation;
}
