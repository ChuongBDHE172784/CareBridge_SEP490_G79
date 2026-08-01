import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/auth/auth_state.dart';
import '../../aiTriage/models/triage_continuation.dart';
import '../../aiTriage/services/triage_continuation_restore_coordinator.dart';
import '../../aiTriage/services/triage_continuation_store.dart';
import '../../aiTriage/services/triage_service.dart';
import '../../journey/services/journey_service.dart';

/// Routes authenticated users to the right first screen after login.
///
/// Mothers without an active journey choose their current stage before setup.
/// Once a journey type exists in the dashboard, they can land directly on CB-008.
class AuthLandingScreen extends StatefulWidget {
  const AuthLandingScreen({
    super.key,
    this.journeyService,
    this.continuationCoordinator,
  });

  final JourneyService? journeyService;
  final TriageContinuationRestoreCoordinator? continuationCoordinator;

  @override
  State<AuthLandingScreen> createState() => _AuthLandingScreenState();
}

class _AuthLandingScreenState extends State<AuthLandingScreen> {
  late final JourneyService _journeyService;
  late final TriageContinuationRestoreCoordinator _continuationCoordinator;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _journeyService = widget.journeyService ?? JourneyService();
    final continuationStore = SecureTriageContinuationStore();
    _continuationCoordinator =
        widget.continuationCoordinator ??
        TriageContinuationRestoreCoordinator(
          store: continuationStore,
          gateway: TriageService(continuationStore: continuationStore),
        );
    WidgetsBinding.instance.addPostFrameCallback((_) => _routeAfterLogin());
  }

  Future<void> _routeAfterLogin() async {
    if (mounted) {
      setState(() {
        _loading = true;
        _error = null;
      });
    }
    final auth = AuthState.instance;
    if (!auth.isAuthenticated) {
      if (mounted) context.go('/welcome');
      return;
    }

    final userId = auth.userId;
    final normalizedRole = auth.role?.trim().toUpperCase();
    if (normalizedRole == 'FAMILY') {
      if (userId != null &&
          userId.isNotEmpty &&
          await _restorePendingContinuation(userId)) {
        return;
      }
      if (mounted) context.go('/?triageChecked=true');
      return;
    }

    if (normalizedRole != 'MOTHER') {
      if (mounted) context.go('/');
      return;
    }

    if (userId != null &&
        userId.isNotEmpty &&
        await _restorePendingContinuation(userId)) {
      return;
    }

    try {
      final dashboard = await _journeyService.getDashboard();
      if (!mounted) return;
      if (dashboard.hasActiveJourney) {
        context.go('/mother-home');
        return;
      }
      context.go('/mother-stage-selection');
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error =
            'Không thể kiểm tra hành trình hiện tại. Vui lòng thử lại để tránh tạo trùng dữ liệu.';
      });
    }
  }

  Future<bool> _restorePendingContinuation(String userId) async {
    TriageContinuationDecision decision;
    try {
      decision = await _continuationCoordinator.restoreForUser(userId);
    } catch (_) {
      if (mounted && AuthState.instance.userId == userId) {
        setState(() {
          _loading = false;
          _error =
              'Chưa thể khôi phục kết quả kiểm tra an toàn. Vui lòng thử lại; dữ liệu tiếp tục vẫn được giữ an toàn.';
        });
      }
      return true;
    }
    if (!mounted || AuthState.instance.userId != userId) return true;
    if (decision.requiresRetry) {
      setState(() {
        _loading = false;
        _error =
            'Chưa thể khôi phục kết quả kiểm tra an toàn. Vui lòng thử lại; dữ liệu tiếp tục vẫn được giữ an toàn.';
      });
      return true;
    }

    final isFamily =
        (AuthState.instance.role ?? '').trim().toUpperCase() == 'FAMILY';
    final String? location = switch (decision.destination) {
      TriageContinuationDestination.motherJourney when !isFamily =>
        '/mother-home?tab=1',
      TriageContinuationDestination.babyProfile
          when !isFamily && decision.originReferenceId?.isNotEmpty == true =>
        '/babies/detail/${Uri.encodeComponent(decision.originReferenceId!)}',
      TriageContinuationDestination.emergency =>
        '/emergency/map?mode=triage&stage=${Uri.encodeComponent(decision.stage ?? 'INFANT')}',
      TriageContinuationDestination.safeDashboard
          when decision.continuationToken == null =>
        isFamily ? '/' : '/mother-home',
      TriageContinuationDestination.none => null,
      _ => null,
    };
    if (location == null) {
      if (decision.destination == TriageContinuationDestination.safeDashboard) {
        setState(() {
          _loading = false;
          _error =
              'Chưa thể khôi phục kết quả kiểm tra an toàn. Vui lòng thử lại; dữ liệu tiếp tục đã được giữ an toàn.';
        });
        return true;
      }
      return false;
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
          : decision.destination ==
                    TriageContinuationDestination.safeDashboard &&
                decision.isRecoverable
          ? const TriageContinuationRecoveryNotice()
          : null,
    );
    return true;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF6F1EC),
      body: SafeArea(
        child: Center(
          child: _loading
              ? const CircularProgressIndicator(color: Color(0xFFC98C7B))
              : Semantics(
                  liveRegion: true,
                  label: _error,
                  child: Container(
                    margin: const EdgeInsets.all(24),
                    padding: const EdgeInsets.all(24),
                    constraints: const BoxConstraints(maxWidth: 420),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(32),
                      border: Border.all(color: const Color(0xFFE8DDD6)),
                      boxShadow: const [
                        BoxShadow(
                          color: Color(0x0F5A463F),
                          blurRadius: 32,
                          offset: Offset(0, 12),
                        ),
                      ],
                    ),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Container(
                          width: 52,
                          height: 52,
                          decoration: const BoxDecoration(
                            color: Color(0xFFF2EAE4),
                            shape: BoxShape.circle,
                          ),
                          child: const Icon(
                            Icons.cloud_off_rounded,
                            color: Color(0xFFC98C7B),
                          ),
                        ),
                        const SizedBox(height: 18),
                        Text(
                          _error ?? 'Không thể tải hành trình.',
                          textAlign: TextAlign.center,
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 16,
                            fontWeight: FontWeight.w600,
                            height: 1.5,
                            color: Color(0xFF5A463F),
                          ),
                        ),
                        const SizedBox(height: 20),
                        FilledButton.icon(
                          key: const Key('auth-landing-retry'),
                          onPressed: _routeAfterLogin,
                          icon: const Icon(Icons.refresh_rounded),
                          label: const Text('Thử lại'),
                          style: FilledButton.styleFrom(
                            minimumSize: const Size.fromHeight(52),
                            backgroundColor: const Color(0xFFC98C7B),
                            foregroundColor: Colors.white,
                            shape: const StadiumBorder(),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
        ),
      ),
    );
  }
}
