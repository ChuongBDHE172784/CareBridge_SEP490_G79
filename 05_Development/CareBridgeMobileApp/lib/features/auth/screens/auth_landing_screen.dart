import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/auth/auth_state.dart';
import '../../journey/services/journey_service.dart';
import '../../journey/services/journey_onboarding_service.dart';

/// Routes authenticated users to the right first screen after login.
///
/// Mothers without an active journey choose their current stage before setup.
/// Once a journey type exists in the dashboard, they can land directly on CB-008.
class AuthLandingScreen extends StatefulWidget {
  const AuthLandingScreen({
    super.key,
    this.journeyService,
    this.onboardingService,
  });

  final JourneyService? journeyService;
  final JourneyOnboardingService? onboardingService;

  @override
  State<AuthLandingScreen> createState() => _AuthLandingScreenState();
}

class _AuthLandingScreenState extends State<AuthLandingScreen> {
  late final JourneyService _journeyService;
  late final JourneyOnboardingService _onboardingService;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _journeyService = widget.journeyService ?? JourneyService();
    _onboardingService = widget.onboardingService ?? JourneyOnboardingService();
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

    if (auth.role != 'MOTHER') {
      if (mounted) context.go('/');
      return;
    }

    try {
      final dashboard = await _journeyService.getDashboard();
      if (!mounted) return;
      if (dashboard.hasActiveJourney) {
        context.go('/mother-home');
        return;
      }
      final onboarding = await _onboardingService.getStatus();
      if (!mounted) return;
      context.go(
        onboarding.canStartJourney
            ? '/mother-stage-selection'
            : '/journey-onboarding',
      );
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error =
            'Không thể kiểm tra hành trình hiện tại. Vui lòng thử lại để tránh tạo trùng dữ liệu.';
      });
    }
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
