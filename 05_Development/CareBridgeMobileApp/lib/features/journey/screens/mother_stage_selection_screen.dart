import 'dart:math';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/auth/auth_state.dart';
import '../../../core/network/api_client.dart';
import '../../auth/services/auth_service.dart';
import '../models/journey_model.dart';
import '../models/journey_onboarding_model.dart';
import '../services/journey_onboarding_draft_storage.dart';
import '../services/journey_onboarding_service.dart';
import '../services/journey_service.dart';

/// First mother-specific onboarding gate before selecting the concrete setup flow.
class MotherStageSelectionScreen extends StatefulWidget {
  const MotherStageSelectionScreen({
    super.key,
    this.journeyService,
    this.onboardingService,
    this.draftStorage,
    this.refreshSession,
  });

  final JourneyService? journeyService;
  final JourneyOnboardingService? onboardingService;
  final JourneyOnboardingDraftStorage? draftStorage;
  final Future<void> Function()? refreshSession;

  @override
  State<MotherStageSelectionScreen> createState() =>
      _MotherStageSelectionScreenState();
}

enum _MotherStage { planning, postpartum, pregnant, babyCare }

class _MotherStageSelectionScreenState
    extends State<MotherStageSelectionScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryAccent = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _textDark = Color(0xFF2E211C);
  static const _textMuted = Color(0xFF6B5850);
  static const _errorBg = Color(0xFFFFDAD6);
  static const _errorText = Color(0xFF93000A);

  late final JourneyService _journeyService;
  late final JourneyOnboardingService _onboardingService;
  late final JourneyOnboardingDraftStorage _draftStorage;
  late final Future<void> Function() _refreshSession;

  _MotherStage? _selectedStage;
  bool _onboardingComplete = false;
  bool _restoring = true;
  bool _loading = false;
  String? _error;
  late String _submissionId;
  Future<void> _draftWriteQueue = Future<void>.value();

  bool get _canContinue => _selectedStage != null && !_loading && !_restoring;

  @override
  void initState() {
    super.initState();
    _journeyService = widget.journeyService ?? JourneyService();
    _onboardingService = widget.onboardingService ?? JourneyOnboardingService();
    _draftStorage = widget.draftStorage ?? JourneyOnboardingDraftStorage();
    _refreshSession =
        widget.refreshSession ?? AuthService.instance.refreshSession;
    _submissionId = _newUuid();
    _restore();
  }

  Future<void> _restore() async {
    final expectedUserId = AuthState.instance.userId;
    try {
      var onboardingComplete = false;
      try {
        final status = await _onboardingService.getStatus();
        onboardingComplete = status.canStartJourney;
      } catch (_) {
        // The account-scoped draft remains available when status is offline.
      }

      Map<String, dynamic>? draft;
      try {
        draft = await _draftStorage.read();
      } catch (_) {
        // Storage failures must not leave an unhandled async error on startup.
      }
      if (!mounted || AuthState.instance.userId != expectedUserId) return;
      if (onboardingComplete) {
        // A completed remote state is authoritative; discard stale local input.
        try {
          await _draftStorage.clear();
        } catch (_) {
          // Cleanup is best effort and must not block the already-valid state.
        }
        if (mounted && AuthState.instance.userId == expectedUserId) {
          setState(() {
            _onboardingComplete = true;
            _selectedStage = null;
          });
        }
        return;
      }
      if (draft == null) return;
      final submissionId = draft['submissionId'];
      final stage = draft['stage'];
      final lifecycleGoal = draft['lifecycleGoal'];
      setState(() {
        if (submissionId is String && _isUuid(submissionId)) {
          _submissionId = submissionId;
        }
        _selectedStage = _stageFromDraft(stage, lifecycleGoal);
      });
    } finally {
      if (mounted) setState(() => _restoring = false);
    }
  }

  Future<void> _saveDraft() {
    final expectedUserId = AuthState.instance.userId;
    final snapshot = <String, dynamic>{
      'submissionId': _submissionId,
      'stage': _selectedStage?.name,
      'lifecycleGoal': _goalForStage(_selectedStage)?.apiValue,
      'preferences': SupportPreference.values.map((value) => value.apiValue).toList(),
    };
    _draftWriteQueue = _draftWriteQueue.catchError((_) {}).then<void>((
      _,
    ) async {
      if (AuthState.instance.userId != expectedUserId) return;
      await _draftStorage.write(snapshot);
    });
    return _draftWriteQueue;
  }

  void _saveDraftInBackground() {
    _saveDraft().catchError((_) {
      // The final pre-submit save retries and reports persistence failures.
    });
  }

  void _selectStage(_MotherStage stage) {
    if (_restoring || _loading) return;
    setState(() {
      _selectedStage = stage;
      _error = null;
    });
    _saveDraftInBackground();
  }

  LifecycleGoal? _goalForStage(_MotherStage? stage) => switch (stage) {
    _MotherStage.planning => LifecycleGoal.preparingForPregnancy,
    _MotherStage.pregnant => LifecycleGoal.currentlyPregnant,
    _MotherStage.postpartum => LifecycleGoal.postpartumRecovery,
    _MotherStage.babyCare || null => null,
  };

  _MotherStage? _stageFromDraft(Object? stage, Object? lifecycleGoal) {
    if (stage is String) {
      for (final value in _MotherStage.values) {
        if (value.name == stage) return value;
      }
    }
    return switch (lifecycleGoal) {
      'PREPARING_FOR_PREGNANCY' => _MotherStage.planning,
      'CURRENTLY_PREGNANT' => _MotherStage.pregnant,
      'POSTPARTUM_RECOVERY' => _MotherStage.postpartum,
      _ => null,
    };
  }

  Future<bool> _ensureOnboarding(_MotherStage stage) async {
    final expectedUserId = AuthState.instance.userId;
    if (_onboardingComplete || stage == _MotherStage.babyCare) return true;
    final goal = _goalForStage(stage);
    if (goal == null) return false;

    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      await _saveDraft();
    } catch (_) {
      if (mounted) {
        setState(() {
          _loading = false;
          _error = 'Không thể lưu lựa chọn. Vui lòng thử lại.';
        });
      }
      return false;
    }
    if (!mounted || AuthState.instance.userId != expectedUserId) return false;
    try {
      final status = await _onboardingService.submit(
        JourneyOnboardingRequest(
          submissionId: _submissionId,
          lifecycleGoal: goal,
          locale: 'vi-VN',
          timeZone: 'Asia/Ho_Chi_Minh',
          preferences: SupportPreference.values,
          consentAccepted: true,
        ),
      );
      if (!mounted || AuthState.instance.userId != expectedUserId) return false;
      if (!status.canStartJourney) {
        setState(() => _error = 'Không thể xác nhận đồng ý. Vui lòng thử lại.');
        return false;
      }
      try {
        await _draftStorage.clear();
      } catch (_) {
        // Remote success is authoritative; stale local cleanup is best effort.
      }
      if (!mounted || AuthState.instance.userId != expectedUserId) return false;
      setState(() => _onboardingComplete = true);
      return true;
    } on ApiException catch (exception) {
      if (!mounted || AuthState.instance.userId != expectedUserId) return false;
      setState(() {
        _error = exception.statusCode >= 500
            ? 'Dịch vụ đang tạm gián đoạn. Lựa chọn của bạn đã được giữ lại.'
            : 'Thông tin chưa hợp lệ. Vui lòng kiểm tra lại.';
      });
      return false;
    } catch (_) {
      if (mounted && AuthState.instance.userId == expectedUserId) {
        setState(
          () => _error = 'Không thể kết nối. Lựa chọn của bạn đã được giữ lại.',
        );
      }
      return false;
    } finally {
      if (mounted && AuthState.instance.userId == expectedUserId) {
        setState(() => _loading = false);
      }
    }
  }

  String _formatApiDate(DateTime date) {
    final y = date.year.toString().padLeft(4, '0');
    final m = date.month.toString().padLeft(2, '0');
    final d = date.day.toString().padLeft(2, '0');
    return '$y-$m-$d';
  }

  String get _buttonLabel {
    switch (_selectedStage) {
      case _MotherStage.postpartum:
        return 'Thiết lập hành trình hậu sản';
      case _MotherStage.planning:
        return 'Tạo hành trình chuẩn bị';
      case _MotherStage.pregnant:
        return 'Tiếp tục tính thai kỳ';
      case _MotherStage.babyCare:
        return 'Thiết lập hồ sơ bé';
      case null:
        return 'Tiếp tục';
    }
  }

  Future<void> _continue() async {
    if (_restoring || _loading) return;
    final expectedUserId = AuthState.instance.userId;
    final stage = _selectedStage;
    if (stage == null) return;

    if (!await _ensureOnboarding(stage) ||
        !mounted ||
        AuthState.instance.userId != expectedUserId) {
      return;
    }

    if (stage == _MotherStage.pregnant) {
      context.go('/journey-setup');
      return;
    }

    if (stage == _MotherStage.postpartum) {
      context.go('/postpartum-recovery-setup');
      return;
    }

    if (stage == _MotherStage.babyCare) {
      context.go('/babies/add?entry=onboarding');
      return;
    }

    setState(() {
      _loading = true;
      _error = null;
    });

    final now = DateTime.now();
    const journeyType = JourneyType.prePregnancy;
    const notes = 'Selected mother stage: planning pregnancy';

    try {
      await _journeyService.createJourney(
        CreateJourneyRequest(
          journeyType: journeyType,
          startDate: _formatApiDate(DateTime(now.year, now.month, now.day)),
          dateSource: 'SELF_REPORTED',
          dateConfidence: 'ESTIMATED',
          changeReason: 'INITIAL_SETUP',
          effectiveAt: now.toUtc().toIso8601String(),
          notes: notes,
        ),
      );
      if (!mounted || AuthState.instance.userId != expectedUserId) return;
      await _refreshSession();
      if (!mounted || AuthState.instance.userId != expectedUserId) return;
      context.go('/recommendation-profile', extra: 'PRE_PREGNANCY');
    } on ApiException catch (e) {
      if (!mounted || AuthState.instance.userId != expectedUserId) return;
      setState(() {
        _error = e.statusCode == 409
            ? 'Bạn đã có hành trình đang hoạt động cho lựa chọn này.'
            : 'Không thể tạo hành trình. Vui lòng thử lại.';
      });
    } catch (_) {
      if (!mounted || AuthState.instance.userId != expectedUserId) return;
      setState(() => _error = 'Không thể kết nối đến máy chủ.');
    } finally {
      if (mounted && AuthState.instance.userId == expectedUserId) {
        setState(() => _loading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(24, 28, 24, 160),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _buildHeader(),
              const SizedBox(height: 28),
              _StageCard(
                key: const Key('mother-stage-planning'),
                badge: 'CHUẨN BỊ MANG THAI',
                icon: Icons.spa_rounded,
                title: 'Muốn mang thai',
                subtitle:
                    'Nhận nội dung chuẩn bị sức khỏe, dinh dưỡng và nhắc việc trước thai kỳ.',
                accentColor: const Color(0xFFE8927C),
                selectedBg: const Color(0xFFFAF2EF),
                selected: _selectedStage == _MotherStage.planning,
                onTap: () => _selectStage(_MotherStage.planning),
              ),
              const SizedBox(height: 16),
              _StageCard(
                key: const Key('mother-stage-pregnant'),
                badge: 'THEO DÕI THAI KỲ',
                icon: Icons.favorite_rounded,
                title: 'Mang thai',
                subtitle:
                    'Tính tuổi thai, ngày dự sinh và cá nhân hóa hành trình theo tuần thai.',
                accentColor: const Color(0xFFC98C7B),
                selectedBg: const Color(0xFFFAF0ED),
                selected: _selectedStage == _MotherStage.pregnant,
                onTap: () => _selectStage(_MotherStage.pregnant),
              ),
              const SizedBox(height: 16),
              _StageCard(
                key: const Key('mother-stage-postpartum'),
                badge: 'HỒI PHỤC HẬU SẢN',
                icon: Icons.self_improvement_rounded,
                title: 'Hồi phục hậu sản',
                subtitle:
                    'Theo dõi quá trình hồi phục của bạn mà không cần tạo hồ sơ em bé.',
                accentColor: const Color(0xFF5A9B8D),
                selectedBg: const Color(0xFFF1F8F6),
                selected: _selectedStage == _MotherStage.postpartum,
                onTap: () => _selectStage(_MotherStage.postpartum),
              ),
              const SizedBox(height: 16),
              _StageCard(
                key: const Key('mother-stage-baby-care'),
                badge: 'CHĂM SÓC EM BÉ',
                icon: Icons.child_care_rounded,
                title: 'Nuôi con',
                subtitle:
                    'Tạo hồ sơ bé để theo dõi tăng trưởng, cột mốc và lịch chăm sóc hằng ngày.',
                accentColor: const Color(0xFFD89B6A),
                selectedBg: const Color(0xFFFAF5EE),
                selected: _selectedStage == _MotherStage.babyCare,
                onTap: () => _selectStage(_MotherStage.babyCare),
              ),
            ],
          ),
        ),
      ),
      bottomNavigationBar: _buildBottomBar(),
    );
  }

  Widget _buildHeader() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
          decoration: BoxDecoration(
            color: _primaryAccent.withValues(alpha: 0.12),
            borderRadius: BorderRadius.circular(20),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: const [
              Icon(Icons.auto_awesome_rounded, size: 14, color: _primary),
              SizedBox(width: 6),
              Text(
                'BƯỚC 2 / 2 • CHỌN GIAI ĐOẠN',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 11,
                  fontWeight: FontWeight.w700,
                  letterSpacing: 0.8,
                  color: _primary,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        const Text(
          'Bạn đang ở giai đoạn nào?',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 31,
            fontWeight: FontWeight.w800,
            color: _textDark,
            height: 1.18,
            letterSpacing: -0.5,
          ),
        ),
        const SizedBox(height: 8),
        const Text(
          'CareBridge sẽ mở đúng hành trình chăm sóc dành riêng cho bạn và gia đình.',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 15,
            color: _textMuted,
            height: 1.5,
          ),
        ),
      ],
    );
  }

  Widget _buildBottomBar() {
    return SafeArea(
      top: false,
      child: Container(
        decoration: BoxDecoration(
          color: _canvas,
          boxShadow: [
            BoxShadow(
              color: const Color(0xFF5A463F).withValues(alpha: 0.08),
              blurRadius: 20,
              offset: const Offset(0, -6),
            ),
          ],
          border: const Border(top: BorderSide(color: Color(0xFFE8DDD6))),
        ),
        padding: const EdgeInsets.fromLTRB(24, 14, 24, 18),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (_error != null) ...[
              Container(
                width: double.infinity,
                margin: const EdgeInsets.only(bottom: 12),
                padding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 12,
                ),
                decoration: BoxDecoration(
                  color: _errorBg,
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: _errorText.withValues(alpha: 0.2)),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.error_outline, color: _errorText, size: 20),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        _error!,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 13,
                          fontWeight: FontWeight.w500,
                          color: _errorText,
                          height: 1.4,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
            SizedBox(
              width: double.infinity,
              height: 56,
              child: FilledButton(
                key: const Key('mother-stage-continue'),
                onPressed: _canContinue ? _continue : null,
                style: FilledButton.styleFrom(
                  backgroundColor: _primary,
                  foregroundColor: Colors.white,
                  disabledBackgroundColor: _primary.withValues(alpha: 0.40),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(28),
                  ),
                  elevation: _canContinue ? 4 : 0,
                  shadowColor: _primary.withValues(alpha: 0.35),
                ),
                child: _loading
                    ? const SizedBox(
                        width: 24,
                        height: 24,
                        child: CircularProgressIndicator(
                          strokeWidth: 2.5,
                          color: Colors.white,
                        ),
                      )
                    : Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Text(
                            _buttonLabel,
                            style: const TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 16,
                              fontWeight: FontWeight.w700,
                              letterSpacing: 0.3,
                            ),
                          ),
                          const SizedBox(width: 8),
                          const Icon(Icons.arrow_forward_rounded, size: 20),
                        ],
                      ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  String _newUuid() {
    final random = Random.secure();
    final bytes = List<int>.generate(16, (_) => random.nextInt(256));
    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    final hex = bytes
        .map((value) => value.toRadixString(16).padLeft(2, '0'))
        .join();
    return '${hex.substring(0, 8)}-${hex.substring(8, 12)}-'
        '${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20)}';
  }

  bool _isUuid(String value) => RegExp(
    r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$',
  ).hasMatch(value);
}

class _StageCard extends StatelessWidget {
  const _StageCard({
    super.key,
    required this.badge,
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.accentColor,
    required this.selectedBg,
    required this.selected,
    required this.onTap,
  });

  final String badge;
  final IconData icon;
  final String title;
  final String subtitle;
  final Color accentColor;
  final Color selectedBg;
  final bool selected;
  final VoidCallback onTap;

  static const _textDark = Color(0xFF2E211C);
  static const _textMuted = Color(0xFF7A6860);
  static const _borderNormal = Color(0xFFE8DDD6);

  @override
  Widget build(BuildContext context) {
    return Semantics(
      selected: selected,
      button: true,
      label: '$title. $subtitle',
      onTap: onTap,
      child: ExcludeSemantics(
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 220),
          curve: Curves.easeInOut,
          decoration: BoxDecoration(
            color: selected ? selectedBg : Colors.white,
            borderRadius: BorderRadius.circular(24),
            border: Border.all(
              color: selected
                  ? accentColor
                  : _borderNormal.withValues(alpha: 0.8),
              width: selected ? 2.5 : 1.2,
            ),
            boxShadow: [
              BoxShadow(
                color: selected
                    ? accentColor.withValues(alpha: 0.18)
                    : const Color(0xFF5A463F).withValues(alpha: 0.05),
                blurRadius: selected ? 24 : 16,
                offset: Offset(0, selected ? 10 : 6),
              ),
            ],
          ),
          child: Material(
            color: Colors.transparent,
            borderRadius: BorderRadius.circular(24),
            child: InkWell(
              borderRadius: BorderRadius.circular(24),
              onTap: onTap,
              splashColor: accentColor.withValues(alpha: 0.12),
              highlightColor: accentColor.withValues(alpha: 0.06),
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Container(
                          width: 48,
                          height: 48,
                          decoration: BoxDecoration(
                            color: selected
                                ? accentColor
                                : accentColor.withValues(alpha: 0.14),
                            borderRadius: BorderRadius.circular(16),
                          ),
                          child: Icon(
                            icon,
                            color: selected ? Colors.white : accentColor,
                            size: 26,
                          ),
                        ),
                        const SizedBox(width: 14),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Container(
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 8,
                                  vertical: 3,
                                ),
                                decoration: BoxDecoration(
                                  color: accentColor.withValues(alpha: 0.12),
                                  borderRadius: BorderRadius.circular(8),
                                ),
                                child: Text(
                                  badge,
                                  style: TextStyle(
                                    fontFamily: 'Lexend',
                                    fontSize: 10,
                                    fontWeight: FontWeight.w700,
                                    letterSpacing: 0.6,
                                    color: accentColor,
                                  ),
                                ),
                              ),
                              const SizedBox(height: 4),
                              Text(
                                title,
                                style: const TextStyle(
                                  fontFamily: 'Lexend',
                                  fontSize: 20,
                                  fontWeight: FontWeight.w700,
                                  color: _textDark,
                                  height: 1.2,
                                ),
                              ),
                            ],
                          ),
                        ),
                        AnimatedContainer(
                          duration: const Duration(milliseconds: 180),
                          width: 28,
                          height: 28,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: selected ? accentColor : Colors.transparent,
                            border: Border.all(
                              color: selected
                                  ? accentColor
                                  : _borderNormal.withValues(alpha: 0.9),
                              width: selected ? 0 : 2,
                            ),
                          ),
                          child: selected
                              ? const Icon(
                                  Icons.check_rounded,
                                  color: Colors.white,
                                  size: 18,
                                )
                              : null,
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    Text(
                      subtitle,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 14,
                        color: _textMuted,
                        height: 1.45,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
