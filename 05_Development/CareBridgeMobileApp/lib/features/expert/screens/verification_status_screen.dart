import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../models/expert_onboarding_model.dart';
import '../services/expert_onboarding_service.dart';
import '../services/expert_onboarding_store.dart';

class VerificationStatusScreen extends StatefulWidget {
  const VerificationStatusScreen({super.key, this.service});

  final ExpertOnboardingService? service;

  @override
  State<VerificationStatusScreen> createState() =>
      _VerificationStatusScreenState();
}

class _VerificationStatusScreenState extends State<VerificationStatusScreen> {
  ExpertOnboardingState? _state;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final state = await (widget.service ?? ExpertOnboardingService.instance)
          .loadState();
      ExpertOnboardingStore.instance.update(state);
      if (mounted) setState(() => _state = state);
    } catch (_) {
      if (mounted) {
        setState(() => _error = 'Không thể cập nhật trạng thái xét duyệt.');
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF7F1ED),
      appBar: AppBar(
        title: const Text('Trạng thái xác minh'),
        backgroundColor: Colors.transparent,
      ),
      body: RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(20),
          children: [
            if (_loading)
              const Padding(
                padding: EdgeInsets.only(top: 180),
                child: Center(child: CircularProgressIndicator()),
              )
            else if (_error != null)
              _messageCard(
                Icons.cloud_off_rounded,
                'Chưa tải được trạng thái',
                _error!,
                const Color(0xFF93000A),
              )
            else ...[
              _statusHero(_state!),
              const SizedBox(height: 18),
              _progressCard(_state!),
              if (_state!.rejectionReason?.isNotEmpty == true) ...[
                const SizedBox(height: 16),
                _messageCard(
                  Icons.info_outline_rounded,
                  'Cần bổ sung',
                  _state!.rejectionReason!,
                  const Color(0xFF93000A),
                ),
              ],
              const SizedBox(height: 22),
              if (_state!.approved)
                FilledButton.icon(
                  onPressed: () => context.go('/expert-home'),
                  icon: const Icon(Icons.dashboard_rounded),
                  label: const Text('Vào trang chuyên gia'),
                )
              else if (_state!.nextStep != ExpertOnboardingStep.review)
                FilledButton.icon(
                  onPressed: () => context.go(_resumePath(_state!.nextStep)),
                  icon: const Icon(Icons.arrow_forward_rounded),
                  label: const Text('Tiếp tục hoàn thiện hồ sơ'),
                )
              else ...[
                OutlinedButton.icon(
                  onPressed: _load,
                  icon: const Icon(Icons.refresh_rounded),
                  label: const Text('Cập nhật trạng thái'),
                ),
                if (_state!.identityStatus == 'REJECTED' ||
                    _state!.identityStatus == 'MANUAL_REVIEW_REQUIRED') ...[
                  const SizedBox(height: 10),
                  FilledButton.tonalIcon(
                    onPressed: () => context.go('/expert/identity'),
                    icon: const Icon(Icons.badge_outlined),
                    label: const Text('Làm lại định danh'),
                  ),
                ],
                if (_state!.credentialStatus == 'REJECTED') ...[
                  const SizedBox(height: 10),
                  FilledButton.tonalIcon(
                    onPressed: () => context.go('/expert/credentials'),
                    icon: const Icon(Icons.workspace_premium_outlined),
                    label: const Text('Làm lại chứng chỉ'),
                  ),
                ],
              ],
            ],
          ],
        ),
      ),
    );
  }

  Widget _statusHero(ExpertOnboardingState state) {
    final approved = state.approved;
    final rejected = state.rejected;
    final color = approved
        ? const Color(0xFF287D55)
        : rejected
        ? const Color(0xFFB3261E)
        : const Color(0xFFC06F5A);
    final title = approved
        ? 'Đã xác minh chuyên gia'
        : rejected
        ? 'Hồ sơ cần bổ sung'
        : 'Đang chờ xét duyệt';
    final detail = approved
        ? 'Bạn đã có thể sử dụng các chức năng dành cho chuyên gia.'
        : rejected
        ? 'Xem lý do bên dưới và gửi lại phần được yêu cầu.'
        : 'Danh tính và giấy tờ chuyên môn đang được quản trị viên kiểm tra.';
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: color.withValues(alpha: .1),
        borderRadius: BorderRadius.circular(24),
      ),
      child: Column(
        children: [
          Icon(
            approved
                ? Icons.verified_rounded
                : rejected
                ? Icons.assignment_late_rounded
                : Icons.hourglass_top_rounded,
            size: 62,
            color: color,
          ),
          const SizedBox(height: 14),
          Text(
            title,
            textAlign: TextAlign.center,
            style: TextStyle(
              fontSize: 24,
              fontWeight: FontWeight.w800,
              color: color,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            detail,
            textAlign: TextAlign.center,
            style: const TextStyle(height: 1.5),
          ),
        ],
      ),
    );
  }

  Widget _progressCard(ExpertOnboardingState state) => Container(
    padding: const EdgeInsets.all(18),
    decoration: BoxDecoration(
      color: Colors.white,
      borderRadius: BorderRadius.circular(20),
    ),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'Tiến độ hồ sơ',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800),
        ),
        const SizedBox(height: 14),
        _row('Hồ sơ chuyên môn', state.profileComplete, 'Đã khai báo'),
        _row(
          'Danh tính & CCCD',
          state.identityComplete,
          _statusLabel(state.identityStatus),
        ),
        _row(
          'Giấy tờ chuyên môn',
          state.credentialComplete,
          _statusLabel(state.credentialStatus),
        ),
        _row(
          'Duyệt hồ sơ',
          state.approved,
          _statusLabel(state.verificationStatus),
        ),
      ],
    ),
  );

  Widget _row(String title, bool done, String subtitle) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 8),
    child: Row(
      children: [
        Icon(
          done ? Icons.check_circle : Icons.radio_button_unchecked,
          color: done ? const Color(0xFF287D55) : const Color(0xFF9C857C),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: const TextStyle(fontWeight: FontWeight.w700)),
              Text(
                subtitle,
                style: const TextStyle(fontSize: 12, color: Color(0xFF75635C)),
              ),
            ],
          ),
        ),
      ],
    ),
  );

  Widget _messageCard(
    IconData icon,
    String title,
    String message,
    Color color,
  ) => Container(
    padding: const EdgeInsets.all(18),
    decoration: BoxDecoration(
      color: Colors.white,
      borderRadius: BorderRadius.circular(20),
    ),
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, color: color),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: TextStyle(fontWeight: FontWeight.w800, color: color),
              ),
              const SizedBox(height: 5),
              Text(message, style: const TextStyle(height: 1.4)),
            ],
          ),
        ),
      ],
    ),
  );

  String _statusLabel(String status) {
    switch (status.toUpperCase()) {
      case 'APPROVED':
        return 'Đã duyệt';
      case 'REJECTED':
        return 'Bị từ chối — cần gửi lại';
      case 'MANUAL_REVIEW_REQUIRED':
        return 'Cần quản trị viên kiểm tra thủ công';
      case 'PENDING':
      case 'SUBMITTED':
      case 'UNDER_REVIEW':
        return 'Đang chờ xét duyệt';
      case 'RETRYABLE':
      case 'RETRYABLE_ERROR':
        return 'Tạm thời chưa xử lý được — vui lòng thử lại';
      case 'MISSING':
      case 'NOT_SUBMITTED':
      case 'REQUIRED':
        return 'Chưa gửi';
      default:
        return status.isEmpty ? 'Chưa có trạng thái' : status;
    }
  }

  String _resumePath(ExpertOnboardingStep step) {
    switch (step) {
      case ExpertOnboardingStep.profile:
        return '/expert-profile-setup';
      case ExpertOnboardingStep.identity:
        return '/expert/identity';
      case ExpertOnboardingStep.credential:
        return '/expert/credentials';
      case ExpertOnboardingStep.review:
      case ExpertOnboardingStep.complete:
        return '/expert-verification-status';
    }
  }
}
