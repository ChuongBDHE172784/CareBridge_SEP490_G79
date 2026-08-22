import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/auth/auth_state.dart';
import '../models/expert_onboarding_model.dart';
import '../services/expert_onboarding_service.dart';
import '../services/expert_onboarding_store.dart';

class ExpertOnboardingGateScreen extends StatefulWidget {
  const ExpertOnboardingGateScreen({super.key, this.service});

  final ExpertOnboardingService? service;

  @override
  State<ExpertOnboardingGateScreen> createState() =>
      _ExpertOnboardingGateScreenState();
}

class _ExpertOnboardingGateScreenState
    extends State<ExpertOnboardingGateScreen> {
  String? _error;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  Future<void> _load() async {
    setState(() => _error = null);
    try {
      final state = await (widget.service ?? ExpertOnboardingService.instance)
          .loadState();
      ExpertOnboardingStore.instance.update(state);
      if (!mounted) return;
      context.go(_pathFor(state.nextStep));
    } catch (_) {
      if (mounted) {
        setState(() => _error =
            'Không thể tải tiến độ đăng ký chuyên gia. Vui lòng kiểm tra kết nối và thử lại.');
      }
    }
  }

  String _pathFor(ExpertOnboardingStep step) {
    switch (step) {
      case ExpertOnboardingStep.profile:
        return '/expert-profile-setup';
      case ExpertOnboardingStep.expertType:
        return '/expert/type';
      case ExpertOnboardingStep.contract:
        return '/expert/contract';
      case ExpertOnboardingStep.availability:
        return '/expert-calendar';
      case ExpertOnboardingStep.identity:
        return '/expert/identity';
      case ExpertOnboardingStep.credential:
        return '/expert/credentials';
      case ExpertOnboardingStep.review:
        return '/expert-verification-status';
      case ExpertOnboardingStep.complete:
        return '/expert-home';
    }
  }

  @override
  Widget build(BuildContext context) {
    ExpertOnboardingStore.instance.bindUser(AuthState.instance.userId);
    return Scaffold(
      backgroundColor: const Color(0xFFF7F1ED),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: _error == null
              ? const Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    CircularProgressIndicator(color: Color(0xFFC98C7B)),
                    SizedBox(height: 20),
                    Text('Đang khôi phục tiến độ đăng ký chuyên gia…'),
                  ],
                )
              : Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.cloud_off_rounded,
                        size: 52, color: Color(0xFF845143)),
                    const SizedBox(height: 16),
                    Text(_error!, textAlign: TextAlign.center),
                    const SizedBox(height: 20),
                    FilledButton.icon(
                      onPressed: _load,
                      icon: const Icon(Icons.refresh_rounded),
                      label: const Text('Thử lại'),
                    ),
                  ],
                ),
        ),
      ),
    );
  }
}
