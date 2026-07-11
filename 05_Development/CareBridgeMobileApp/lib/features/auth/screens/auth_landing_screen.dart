import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/auth/auth_state.dart';
import '../../journey/services/journey_service.dart';

/// Routes authenticated users to the right first screen after login.
///
/// Mothers without an active journey must complete CB-007 first. Once a
/// journey type exists in the dashboard, they can land directly on CB-008.
class AuthLandingScreen extends StatefulWidget {
  const AuthLandingScreen({super.key});

  @override
  State<AuthLandingScreen> createState() => _AuthLandingScreenState();
}

class _AuthLandingScreenState extends State<AuthLandingScreen> {
  final _journeyService = JourneyService();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _routeAfterLogin());
  }

  Future<void> _routeAfterLogin() async {
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
      context.go(dashboard.hasActiveJourney ? '/' : '/journey-setup');
    } catch (_) {
      if (!mounted) return;
      context.go('/journey-setup');
    }
  }

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      backgroundColor: Color(0xFFF6F1EC),
      body: Center(
        child: CircularProgressIndicator(color: Color(0xFFC98C7B)),
      ),
    );
  }
}
