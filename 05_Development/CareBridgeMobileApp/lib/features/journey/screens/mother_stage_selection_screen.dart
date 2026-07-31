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
  static const _primary = Color(0xFFC98C7B);
  static const _primaryDark = Color(0xFF845143);
  static const _canvas = Color(0xFFF6F1EC);
  static const _text = Color(0xFF5A463F);
  static const _errorBg = Color(0xFFFFDAD6);
  static const _errorText = Color(0xFF93000A);

  late final JourneyService _journeyService;
  late final JourneyOnboardingService _onboardingService;
  late final JourneyOnboardingDraftStorage _draftStorage;
  late final Future<void> Function() _refreshSession;

  _MotherStage? _selectedStage;
  final Set<SupportPreference> _preferences = {};
  bool _consentAccepted = false;
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
            _preferences.clear();
            _consentAccepted = false;
          });
        }
        return;
      }
      if (draft == null) return;
      final submissionId = draft['submissionId'];
      final rawPreferences = draft['preferences'];
      final preferenceValues = rawPreferences is List
          ? rawPreferences.whereType<String>().toSet()
          : <String>{};
      final stage = draft['stage'];
      final lifecycleGoal = draft['lifecycleGoal'];
      setState(() {
        if (submissionId is String && _isUuid(submissionId)) {
          _submissionId = submissionId;
        }
        _selectedStage = _stageFromDraft(stage, lifecycleGoal);
        _preferences
          ..clear()
          ..addAll(
            SupportPreference.values.where(
              (value) => preferenceValues.contains(value.apiValue),
            ),
          );
        // Consent must always be explicit and is never restored from storage.
        _consentAccepted = false;
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
      'preferences': _preferences.map((value) => value.apiValue).toList(),
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
      _consentAccepted = false;
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
    if (_preferences.isEmpty) {
      setState(() => _error = 'Vui lòng chọn ít nhất một nội dung hỗ trợ.');
      return false;
    }
    if (!_consentAccepted) {
      setState(() => _error = 'Vui lòng đọc và đồng ý trước khi tiếp tục.');
      return false;
    }
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
          preferences: _preferences.toList(),
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
      context.go('/');
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
          padding: const EdgeInsets.fromLTRB(24, 28, 24, 28),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _buildHeader(),
              const SizedBox(height: 28),
              _StageCard(
                key: const Key('mother-stage-planning'),
                icon: Icons.spa_rounded,
                title: 'Muốn mang thai',
                subtitle:
                    'Nhận nội dung chuẩn bị sức khỏe, dinh dưỡng và nhắc việc trước thai kỳ.',
                selected: _selectedStage == _MotherStage.planning,
                onTap: () => _selectStage(_MotherStage.planning),
              ),
              const SizedBox(height: 16),
              _StageCard(
                key: const Key('mother-stage-postpartum'),
                icon: Icons.self_improvement_rounded,
                title: 'Đang hồi phục hậu sản',
                subtitle:
                    'Theo dõi quá trình hồi phục của bạn mà không cần tạo hồ sơ em bé.',
                selected: _selectedStage == _MotherStage.postpartum,
                onTap: () => _selectStage(_MotherStage.postpartum),
              ),
              const SizedBox(height: 16),
              _StageCard(
                key: const Key('mother-stage-pregnant'),
                icon: Icons.favorite_rounded,
                title: 'Đang mang thai',
                subtitle:
                    'Tính tuổi thai, ngày dự sinh và cá nhân hóa hành trình theo tuần thai.',
                selected: _selectedStage == _MotherStage.pregnant,
                onTap: () => _selectStage(_MotherStage.pregnant),
              ),
              const SizedBox(height: 16),
              _StageCard(
                key: const Key('mother-stage-baby-care'),
                icon: Icons.child_care_rounded,
                title: 'Đang nuôi bé',
                subtitle:
                    'Tạo hồ sơ bé để theo dõi tăng trưởng, cột mốc và lịch chăm sóc hằng ngày.',
                selected: _selectedStage == _MotherStage.babyCare,
                onTap: () => _selectStage(_MotherStage.babyCare),
              ),
              if (_selectedStage != null &&
                  _selectedStage != _MotherStage.babyCare &&
                  !_onboardingComplete) ...[
                const SizedBox(height: 28),
                _buildSupportPreferences(),
                const SizedBox(height: 18),
                _buildConsentCard(),
              ],
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
          width: 56,
          height: 56,
          decoration: BoxDecoration(
            color: _primary.withAlpha(38),
            shape: BoxShape.circle,
          ),
          child: const Icon(Icons.auto_awesome_rounded, color: _primary),
        ),
        const SizedBox(height: 20),
        const Text(
          'Bạn đang ở giai đoạn nào?',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 31,
            fontWeight: FontWeight.w800,
            color: _primaryDark,
            height: 1.18,
          ),
        ),
        const SizedBox(height: 12),
        const Text(
          'CareBridge sẽ mở đúng hành trình chăm sóc cho mẹ và gia đình.',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 16,
            color: _text,
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
        decoration: const BoxDecoration(
          color: _canvas,
          border: Border(top: BorderSide(color: Color(0xFFE8DDD6))),
        ),
        padding: const EdgeInsets.fromLTRB(24, 12, 24, 16),
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
                  borderRadius: BorderRadius.circular(18),
                ),
                child: Text(
                  _error!,
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    color: _errorText,
                    height: 1.4,
                  ),
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
                  disabledBackgroundColor: _primary.withAlpha(112),
                  shape: const StadiumBorder(),
                  elevation: 0,
                ),
                child: _loading
                    ? const SizedBox(
                        width: 22,
                        height: 22,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Colors.white,
                        ),
                      )
                    : Text(
                        _buttonLabel,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 16,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSupportPreferences() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(28),
        border: Border.all(color: const Color(0xFFE8DDD6)),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0F5A463F),
            blurRadius: 30,
            offset: Offset(0, 12),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Bạn muốn nhận hỗ trợ về',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 18,
              fontWeight: FontWeight.w800,
              color: _text,
            ),
          ),
          const SizedBox(height: 14),
          Wrap(
            spacing: 10,
            runSpacing: 10,
            children: SupportPreference.values.map((preference) {
              final selected = _preferences.contains(preference);
              return FilterChip(
                key: Key('preference-${preference.apiValue}'),
                label: Text(_preferenceLabel(preference)),
                selected: selected,
                onSelected: _loading
                    ? null
                    : (value) {
                        setState(() {
                          value
                              ? _preferences.add(preference)
                              : _preferences.remove(preference);
                          _error = null;
                        });
                        _saveDraftInBackground();
                      },
                selectedColor: _primary,
                backgroundColor: const Color(0xFFF2EAE4),
                checkmarkColor: Colors.white,
                labelStyle: TextStyle(
                  color: selected ? Colors.white : _text,
                  fontWeight: FontWeight.w700,
                ),
                shape: const StadiumBorder(),
                side: BorderSide(
                  color: selected
                      ? Colors.transparent
                      : const Color(0xFFE8DDD6),
                  width: 2,
                ),
              );
            }).toList(),
          ),
        ],
      ),
    );
  }

  Widget _buildConsentCard() {
    return Semantics(
      container: true,
      label: 'Đồng ý bắt buộc, chưa được chọn sẵn',
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
          color: const Color(0xFFF2EAE4),
          borderRadius: BorderRadius.circular(24),
          border: Border.all(color: const Color(0xFFE8DDD6)),
        ),
        child: Material(
          color: Colors.transparent,
          child: CheckboxListTile(
            key: const Key('lifecycle-consent'),
            value: _consentAccepted,
            onChanged: _loading
                ? null
                : (value) => setState(() {
                    _consentAccepted = value == true;
                    _error = null;
                  }),
            activeColor: _primary,
            controlAffinity: ListTileControlAffinity.leading,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(20),
            ),
            title: const Text(
              'Tôi đồng ý cho CareBridge lưu thông tin nền và cá nhân hóa hành trình theo Chính sách MOTHER_LIFECYCLE_V1.',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 16,
                fontWeight: FontWeight.w600,
                height: 1.45,
                color: _text,
              ),
            ),
            subtitle: const Padding(
              padding: EdgeInsets.only(top: 8),
              child: Text(
                'Bạn có thể thu hồi đồng ý. Lịch sử đã tạo sẽ không bị thay đổi âm thầm.',
                style: TextStyle(
                  fontSize: 14,
                  height: 1.4,
                  color: Color(0xFF9C857C),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  String _preferenceLabel(SupportPreference value) => switch (value) {
    SupportPreference.nutrition => 'Dinh dưỡng',
    SupportPreference.mentalWellbeing => 'Tinh thần',
    SupportPreference.physicalActivity => 'Vận động',
    SupportPreference.appointmentReminders => 'Nhắc lịch',
  };

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
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.selected,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final bool selected;
  final VoidCallback onTap;

  static const _primary = Color(0xFFC98C7B);
  static const _surface = Colors.white;
  static const _surfaceLow = Color(0xFFF2EAE4);
  static const _text = Color(0xFF5A463F);
  static const _muted = Color(0xFF9C857C);
  static const _border = Color(0xFFE8DDD6);

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
          decoration: BoxDecoration(
            color: selected ? _surfaceLow : _surface,
            borderRadius: BorderRadius.circular(28),
            border: Border.all(
              color: selected ? _primary : _border.withAlpha(160),
              width: 2,
            ),
            boxShadow: [
              BoxShadow(
                color: _text.withAlpha(15),
                blurRadius: 30,
                offset: const Offset(0, 12),
              ),
            ],
          ),
          child: Material(
            color: Colors.transparent,
            borderRadius: BorderRadius.circular(28),
            child: InkWell(
              borderRadius: BorderRadius.circular(28),
              onTap: onTap,
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      width: 52,
                      height: 52,
                      decoration: BoxDecoration(
                        color: selected ? _primary : _surfaceLow,
                        shape: BoxShape.circle,
                      ),
                      child: Icon(
                        icon,
                        color: selected ? Colors.white : _primary,
                        size: 28,
                      ),
                    ),
                    const SizedBox(width: 16),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            title,
                            style: const TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 20,
                              fontWeight: FontWeight.w800,
                              color: _text,
                              height: 1.25,
                            ),
                          ),
                          const SizedBox(height: 7),
                          Text(
                            subtitle,
                            style: const TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 14,
                              color: _muted,
                              height: 1.45,
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 12),
                    AnimatedOpacity(
                      opacity: selected ? 1 : 0,
                      duration: const Duration(milliseconds: 160),
                      child: const Icon(
                        Icons.check_circle_rounded,
                        color: _primary,
                        size: 24,
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
