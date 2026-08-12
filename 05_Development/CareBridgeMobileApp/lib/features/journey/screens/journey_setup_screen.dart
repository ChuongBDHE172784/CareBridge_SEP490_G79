import 'dart:async';
import 'dart:convert';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_client.dart';
import '../../auth/services/auth_service.dart';
import '../models/journey_model.dart';
import '../services/journey_service.dart';

/// CB-007 - Mother Journey Setup (UC-22)
/// Pregnancy dating wizard based on the setup mother journey screenflow.
class JourneySetupScreen extends StatefulWidget {
  const JourneySetupScreen({
    super.key,
    this.journeyId,
    this.isEditMode = false,
    this.isPrePregnancyTransition = false,
    this.service,
    this.refreshSession,
  });

  final String? journeyId;
  final bool isEditMode;
  final bool isPrePregnancyTransition;
  final JourneyService? service;
  final Future<void> Function()? refreshSession;

  @override
  State<JourneySetupScreen> createState() => _JourneySetupScreenState();
}

enum _SetupStep { method, lmpDate, dueDate, dueDateResult, loading }

enum _DatingMethod { lmp, dueDate }

class _JourneySetupScreenState extends State<JourneySetupScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryAccent = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Colors.white;
  static const _textDark = Color(0xFF2E211C);
  static const _textMuted = Color(0xFF6B5850);
  static const _borderNormal = Color(0xFFE8DDD6);
  static const _errorBg = Color(0xFFFFDAD6);
  static const _errorText = Color(0xFF93000A);

  late final JourneyService _service;
  final List<_SetupStep> _history = [];
  final ScrollController _scrollController = ScrollController();

  _SetupStep _step = _SetupStep.method;
  _DatingMethod? _selectedMethod;
  DateTime? _lmpDate;
  DateTime? _dueDate;
  bool _loading = false;
  bool _allowRoutePop = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _service = widget.service ?? JourneyService();
  }

  // The form is completed by the account holder. An EDD entered here is not
  // evidence of clinician confirmation, so preserve user-reported provenance
  // until a separately verified clinical source is supplied by the server.
  String get _dateSource => 'SELF_REPORTED';

  String get _dateConfidence => 'ESTIMATED';

  /// The backend accepts only a server-owned LMP or EDD authority.  Legacy
  /// wizard methods still calculate an EDD for compatibility, so they map to
  /// the EDD shape rather than sending a fabricated LMP.
  String get _datingBasis => switch (_selectedMethod) {
    _DatingMethod.lmp => 'LMP',
    _DatingMethod.dueDate => 'EDD',
    null => '',
  };

  String get _changeReason {
    if (widget.isPrePregnancyTransition) return 'PREGNANCY_CONFIRMED';
    if (widget.isEditMode) return 'DATE_CORRECTION';
    return 'INITIAL_SETUP';
  }

  DateTime get _today {
    final now = DateTime.now();
    return DateTime(now.year, now.month, now.day);
  }

  DateTime? get _calculatedDueDate {
    switch (_selectedMethod) {
      case _DatingMethod.lmp:
        final lmp = _lmpDate;
        if (lmp == null) return null;
        // Informational preview only. The server is authoritative and uses
        // LMP + 280 days for checklist dating resolution.
        return lmp.add(const Duration(days: 280));
      case _DatingMethod.dueDate:
        return _dueDate;
      case null:
        return null;
    }
  }

  int get _obstetricAgeDays {
    final dueDate = _calculatedDueDate;
    if (dueDate == null) return 0;
    final age = 280 - dueDate.difference(_today).inDays;
    return age.clamp(0, 294).toInt();
  }

  int get _fetalAgeDays => (_obstetricAgeDays - 14).clamp(0, 280).toInt();

  bool get _canContinue {
    if (_loading) return false;
    switch (_step) {
      case _SetupStep.method:
        return _selectedMethod != null;
      case _SetupStep.lmpDate:
        return _lmpDate != null;
      case _SetupStep.dueDate:
        return _dueDate != null;
      case _SetupStep.dueDateResult:
        return true;
      case _SetupStep.loading:
        return false;
    }
  }

  int get _totalProgressSteps => _selectedMethod == _DatingMethod.lmp ? 4 : 3;

  int get _progressIndex {
    switch (_step) {
      case _SetupStep.method:
        return 1;
      case _SetupStep.lmpDate:
      case _SetupStep.dueDate:
        return 2;
      case _SetupStep.dueDateResult:
      case _SetupStep.loading:
        return _totalProgressSteps;
    }
  }

  double get _progress =>
      (_progressIndex / _totalProgressSteps).clamp(0.12, 1.0);

  String _formatApiDate(DateTime date) {
    final y = date.year.toString().padLeft(4, '0');
    final m = date.month.toString().padLeft(2, '0');
    final d = date.day.toString().padLeft(2, '0');
    return '$y-$m-$d';
  }

  String _formatDisplayDate(DateTime date) =>
      '${date.day} tháng ${date.month}, ${date.year}';

  String _notes() {
    final parts = <String>[
      'Setup source: ${_selectedMethod?.name ?? 'unknown'}',
    ];
    if (_lmpDate != null) parts.add('LMP: ${_formatApiDate(_lmpDate!)}');
    if (_dueDate != null) parts.add('Doctor EDD: ${_formatApiDate(_dueDate!)}');
    return parts.join('; ');
  }

  void _goTo(_SetupStep next) {
    setState(() {
      _history.add(_step);
      _step = next;
      _error = null;
    });
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.jumpTo(0);
      }
    });
  }

  void _selectMethod(_DatingMethod method) {
    setState(() {
      _selectedMethod = method;
      _error = null;
      if (method != _DatingMethod.lmp) {
        _lmpDate = null;
      }
      if (method != _DatingMethod.dueDate) {
        _dueDate = null;
      }
    });
  }

  void _handleBack() {
    if (_loading) return;
    if (_history.isNotEmpty) {
      setState(() {
        _step = _history.removeLast();
        _error = null;
      });
      return;
    }
    _leaveSetup();
  }

  void _leaveSetup() {
    if (context.canPop()) {
      setState(() => _allowRoutePop = true);
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) Navigator.of(context).maybePop();
      });
      return;
    }

    context.go('/mother-stage-selection');
  }

  void _handleContinue() {
    if (!_canContinue) return;
    switch (_step) {
      case _SetupStep.method:
        switch (_selectedMethod) {
          case _DatingMethod.lmp:
            _goTo(_SetupStep.lmpDate);
          case _DatingMethod.dueDate:
            _goTo(_SetupStep.dueDate);
          case null:
            break;
        }
      case _SetupStep.lmpDate:
        _goTo(_SetupStep.dueDateResult);
      case _SetupStep.dueDate:
        _goTo(_SetupStep.dueDateResult);
      case _SetupStep.dueDateResult:
        unawaited(_submitJourney());
      case _SetupStep.loading:
        break;
    }
  }

  Future<void> _submitJourney() async {
    final dueDate = _calculatedDueDate;
    if (dueDate == null) return;
    final lastMenstrualDate =
        _selectedMethod == _DatingMethod.lmp && _lmpDate != null
        ? _formatApiDate(_lmpDate!)
        : null;
    // V2 accepts exactly one dating authority. LMP sends only LMP; EDD sends
    // only EDD. The server remains authoritative for week and plan resolution.
    final estimatedDueDate = _datingBasis == 'EDD'
        ? _formatApiDate(dueDate)
        : null;
    final journeyId = widget.journeyId;
    final isUpdate = widget.isEditMode && journeyId != null;
    setState(() {
      _history.add(_step);
      _step = _SetupStep.loading;
      _loading = true;
      _error = null;
    });

    try {
      await Future<void>.delayed(const Duration(milliseconds: 650));
      if (isUpdate) {
        await _service.updateJourney(
          journeyId,
          UpdateJourneyRequest(
            journeyType: JourneyType.pregnancy,
            lastMenstrualDate: lastMenstrualDate,
            estimatedDueDate: estimatedDueDate,
            datingBasis: _datingBasis,
            dateSource: _dateSource,
            dateConfidence: _dateConfidence,
            changeReason: _changeReason,
            notes: _notes(),
          ),
        );
      } else {
        await _service.createJourney(
          CreateJourneyRequest(
            journeyType: JourneyType.pregnancy,
            startDate: _formatApiDate(_today),
            lastMenstrualDate: lastMenstrualDate,
            estimatedDueDate: estimatedDueDate,
            datingBasis: _datingBasis,
            dateSource: _dateSource,
            dateConfidence: _dateConfidence,
            changeReason: _changeReason,
            notes: _notes(),
          ),
        );
      }
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _step = _SetupStep.dueDateResult;
        _loading = false;
        _error = _apiErrorMessage(e, isUpdate: isUpdate);
      });
      return;
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _step = _SetupStep.dueDateResult;
        _loading = false;
        _error = isUpdate
            ? 'Lỗi kết nối. Không thể cập nhật hành trình.'
            : 'Lỗi kết nối. Vui lòng kiểm tra đường truyền.';
      });
      return;
    }

    try {
      final refreshSession = widget.refreshSession;
      if (refreshSession == null) {
        await AuthService.instance.refreshSession();
      } else {
        await refreshSession();
      }
    } catch (error) {
      debugPrint(
        '[JourneySetup] Journey mutation committed; session refresh will '
        'reconcile later (${error.runtimeType}).',
      );
    }
    if (!mounted) return;
    if (isUpdate) {
      context.pop();
    } else {
      context.go('/recommendation-profile', extra: 'PREGNANCY');
    }
  }

  String _apiErrorMessage(ApiException error, {required bool isUpdate}) {
    final errorCode = _apiErrorCode(error.message);
    if (error.statusCode == 409 && errorCode == 'LIFECYCLE_CONSENT_INVALID') {
      return 'Quyền đồng ý cho hành trình này không còn hiệu lực. '
          'Vui lòng xác nhận lại quyền đồng ý rồi thử lại.';
    }
    if (error.statusCode == 409 && errorCode == 'LIFECYCLE_CONSENT_REQUIRED') {
      return 'Bạn cần xác nhận quyền đồng ý cho hành trình trước khi tiếp tục.';
    }
    if (isUpdate) {
      return 'Không thể cập nhật hành trình. Vui lòng thử lại.';
    }
    return error.statusCode == 409
        ? 'Bạn đã có một hành trình đang hoạt động.'
        : 'Không thể tạo hành trình. Vui lòng thử lại.';
  }

  String? _apiErrorCode(String body) {
    try {
      final decoded = jsonDecode(body);
      if (decoded is! Map<String, dynamic>) return null;
      final error = decoded['error'];
      if (error is String) return error;
      if (error is Map<String, dynamic>) return error['code']?.toString();
      return decoded['code']?.toString();
    } catch (_) {
      return null;
    }
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: _allowRoutePop,
      onPopInvokedWithResult: (didPop, _) {
        if (didPop) return;
        _handleBack();
      },
      child: Scaffold(
        backgroundColor: _canvas,
        body: SafeArea(
          child: Column(
            children: [
              _buildTopBar(),
              _buildProgressBar(),
              Expanded(
                child: AnimatedSwitcher(
                  duration: const Duration(milliseconds: 260),
                  switchInCurve: Curves.easeOutCubic,
                  switchOutCurve: Curves.easeInCubic,
                  child: SingleChildScrollView(
                    controller: _scrollController,
                    key: ValueKey(_step),
                    padding: const EdgeInsets.fromLTRB(20, 16, 20, 20),
                    child: _buildStepContent(),
                  ),
                ),
              ),
            ],
          ),
        ),
        bottomNavigationBar: _buildBottomBar(),
      ),
    );
  }

  Widget _buildTopBar() {
    return SizedBox(
      height: 52,
      child: Row(
        children: [
          const SizedBox(width: 8),
          IconButton(
            onPressed: _loading ? null : _handleBack,
            tooltip: 'Quay lại',
            icon: const Icon(Icons.arrow_back_rounded),
            color: _textDark,
          ),
          Expanded(
            child: Text(
              _step == _SetupStep.loading
                  ? 'ĐANG THIẾT LẬP'
                  : 'BƯỚC $_progressIndex / $_totalProgressSteps',
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 12,
                fontWeight: FontWeight.w700,
                letterSpacing: 1.1,
                color: _primaryAccent,
              ),
            ),
          ),
          const SizedBox(width: 56),
        ],
      ),
    );
  }

  Widget _buildProgressBar() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 2, 20, 4),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(99),
        child: Stack(
          children: [
            Container(height: 6, color: _borderNormal.withValues(alpha: 0.6)),
            FractionallySizedBox(
              widthFactor: _progress,
              child: Container(height: 6, color: _primary),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStepContent() {
    switch (_step) {
      case _SetupStep.method:
        return _buildMethodStep();
      case _SetupStep.lmpDate:
        return _buildDateStep(
          title: 'Ngày đầu tiên của chu kỳ',
          selectedDate: _lmpDate,
          firstDate: _today.subtract(const Duration(days: 294)),
          lastDate: _today,
          onChanged: (date) => setState(() => _lmpDate = date),
        );
      case _SetupStep.dueDate:
        return _buildDateStep(
          title: 'Ngày sinh dự kiến',
          selectedDate: _dueDate,
          firstDate: _today.subtract(const Duration(days: 14)),
          // The server rejects an EDD more than 280 days ahead because its
          // canonical LMP would be in the future. Keep the picker inside the
          // same accepted dating range.
          lastDate: _today.add(const Duration(days: 280)),
          onChanged: (date) => setState(() => _dueDate = date),
        );
      case _SetupStep.dueDateResult:
        return _buildDueDateResultStep();
      case _SetupStep.loading:
        return _buildLoadingStep();
    }
  }

  Widget _buildMethodStep() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Chọn một trong hai nguồn chuẩn để hệ thống tính tuần thai và chọn checklist.',
          textAlign: TextAlign.center,
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 13,
            height: 1.4,
            color: _textMuted,
          ),
        ),
        const SizedBox(height: 14),
        const Text(
          'Hãy xác định thời gian mang thai của bạn. Bạn đã biết gì rồi?',
          textAlign: TextAlign.center,
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 26,
            fontWeight: FontWeight.w800,
            color: _textDark,
            height: 1.22,
            letterSpacing: -0.5,
          ),
        ),
        const SizedBox(height: 20),
        _MethodCard(
          key: const Key('dating-method-lmp'),
          badge: 'PHỔ BIẾN NHẤT',
          title: 'Tôi nhớ ngày đầu tiên của chu kỳ',
          subtitle: 'CareBridge sẽ tính dự sinh từ kỳ kinh cuối.',
          icon: Icons.event_note_rounded,
          accentColor: const Color(0xFFE8927C),
          selectedBg: const Color(0xFFFAF2EF),
          selected: _selectedMethod == _DatingMethod.lmp,
          onTap: () => _selectMethod(_DatingMethod.lmp),
        ),
        const SizedBox(height: 10),
        _MethodCard(
          key: const Key('dating-method-due-date'),
          badge: 'EDD',
          title: 'Tôi biết ngày dự sinh (EDD)',
          subtitle: 'Nhập ngày dự sinh để hệ thống tính tuần thai.',
          icon: Icons.medical_information_rounded,
          accentColor: const Color(0xFF4A8B88),
          selectedBg: const Color(0xFFEFF7F6),
          selected: _selectedMethod == _DatingMethod.dueDate,
          onTap: () => _selectMethod(_DatingMethod.dueDate),
        ),
      ],
    );
  }

  Widget _buildDateStep({
    required String title,
    required DateTime? selectedDate,
    required DateTime firstDate,
    required DateTime lastDate,
    required ValueChanged<DateTime> onChanged,
  }) {
    final initial = selectedDate ?? _today.clampDate(firstDate, lastDate);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          title,
          textAlign: TextAlign.center,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 26,
            fontWeight: FontWeight.w800,
            color: _textDark,
            height: 1.22,
          ),
        ),
        const SizedBox(height: 20),
        Container(
          decoration: BoxDecoration(
            color: _surface,
            borderRadius: BorderRadius.circular(24),
            border: Border.all(color: _borderNormal.withValues(alpha: 0.8)),
            boxShadow: [
              BoxShadow(
                color: const Color(0xFF5A463F).withValues(alpha: 0.06),
                blurRadius: 28,
                offset: const Offset(0, 10),
              ),
            ],
          ),
          padding: const EdgeInsets.fromLTRB(12, 14, 12, 12),
          child: Theme(
            data: Theme.of(context).copyWith(
              colorScheme: const ColorScheme.light(
                primary: _primary,
                onPrimary: Colors.white,
                surface: _surface,
                onSurface: _textDark,
              ),
            ),
            child: CalendarDatePicker(
              initialDate: initial,
              firstDate: firstDate,
              lastDate: lastDate,
              onDateChanged: onChanged,
            ),
          ),
        ),
        const SizedBox(height: 16),
        _InfoPill(
          icon: Icons.calendar_today_rounded,
          text: selectedDate == null
              ? 'Chọn một ngày để tiếp tục'
              : 'Đã chọn ${_formatDisplayDate(selectedDate)}',
        ),
      ],
    );
  }

  Widget _buildDueDateResultStep() {
    final dueDate = _calculatedDueDate;
    final obstetricWeeks = _obstetricAgeDays ~/ 7;
    final obstetricDays = _obstetricAgeDays % 7;
    final fetalWeeks = _fetalAgeDays ~/ 7;
    final fetalDays = _fetalAgeDays % 7;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Kết quả',
          textAlign: TextAlign.center,
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 26,
            fontWeight: FontWeight.w800,
            color: _textDark,
            height: 1.22,
          ),
        ),
        const SizedBox(height: 6),
        const Text(
          'Bạn đang mang thai',
          textAlign: TextAlign.center,
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 15,
            fontWeight: FontWeight.w700,
            color: _textMuted,
            height: 1.4,
          ),
        ),
        const SizedBox(height: 24),
        Container(
          padding: const EdgeInsets.fromLTRB(18, 22, 18, 20),
          decoration: BoxDecoration(
            color: _surface,
            borderRadius: BorderRadius.circular(28),
            border: Border.all(
              color: _primaryAccent.withValues(alpha: 0.3),
              width: 1.5,
            ),
            boxShadow: [
              BoxShadow(
                color: _primaryAccent.withValues(alpha: 0.15),
                blurRadius: 28,
                offset: const Offset(0, 10),
              ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Center(
                child: Container(
                  width: 64,
                  height: 64,
                  decoration: BoxDecoration(
                    color: _primaryAccent.withValues(alpha: 0.16),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(
                    Icons.child_care_rounded,
                    color: _primary,
                    size: 34,
                  ),
                ),
              ),
              const SizedBox(height: 16),
              RichText(
                textAlign: TextAlign.center,
                text: TextSpan(
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 18,
                    fontWeight: FontWeight.w800,
                    color: _textDark,
                    height: 1.25,
                  ),
                  children: [
                    TextSpan(
                      text: '$obstetricWeeks tuần ',
                      style: const TextStyle(
                        color: _primary,
                        fontSize: 32,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                    TextSpan(text: '$obstetricDays ngày'),
                  ],
                ),
              ),
              const SizedBox(height: 20),
              _ResultMetric(
                icon: Icons.timeline_rounded,
                label: 'Tuổi thai nhi',
                value: '$fetalWeeks tuần $fetalDays ngày',
              ),
              const SizedBox(height: 10),
              _ResultMetric(
                icon: Icons.event_available_rounded,
                label: 'Ngày dự sinh',
                value: dueDate == null
                    ? 'Chưa có ngày dự sinh'
                    : _formatDisplayDate(dueDate),
              ),
            ],
          ),
        ),
        const SizedBox(height: 18),
        const _InsightCard(
          icon: Icons.verified_rounded,
          text:
              'CareBridge sẽ dùng mốc này để cá nhân hóa nội dung theo tuần thai.',
        ),
      ],
    );
  }

  Widget _buildLoadingStep() {
    return SizedBox(
      height: MediaQuery.of(context).size.height * 0.64,
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          TweenAnimationBuilder<double>(
            tween: Tween(begin: 0.12, end: 0.96),
            duration: const Duration(milliseconds: 900),
            builder: (context, value, child) {
              return SizedBox(
                width: 210,
                height: 210,
                child: Stack(
                  alignment: Alignment.center,
                  children: [
                    Positioned.fill(
                      child: CircularProgressIndicator(
                        value: value,
                        strokeWidth: 14,
                        strokeCap: StrokeCap.round,
                        backgroundColor: _borderNormal.withValues(alpha: 0.6),
                        color: _primary,
                      ),
                    ),
                    Text(
                      '${(value * 100).round()}%',
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 34,
                        fontWeight: FontWeight.w900,
                        color: _primary,
                      ),
                    ),
                  ],
                ),
              );
            },
          ),
          const SizedBox(height: 46),
          const Text(
            'Tạo chương trình cá nhân...',
            textAlign: TextAlign.center,
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 24,
              fontWeight: FontWeight.w800,
              color: _textDark,
              height: 1.3,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBottomBar() {
    if (_step == _SetupStep.loading) return const SizedBox.shrink();
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
        padding: const EdgeInsets.fromLTRB(20, 10, 20, 14),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (_error != null) ...[
              Semantics(
                container: true,
                liveRegion: true,
                label: _error,
                child: ExcludeSemantics(
                  child: Container(
                    width: double.infinity,
                    margin: const EdgeInsets.only(bottom: 10),
                    padding: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 10,
                    ),
                    decoration: BoxDecoration(
                      color: _errorBg,
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(
                        color: _errorText.withValues(alpha: 0.2),
                      ),
                    ),
                    child: Row(
                      children: [
                        const Icon(
                          Icons.error_outline,
                          color: _errorText,
                          size: 20,
                        ),
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
                ),
              ),
            ],
            if (_step == _SetupStep.dueDateResult) ...[
              TextButton(
                onPressed: () => _goTo(_SetupStep.method),
                child: const Text(
                  'TÍNH LẠI',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 15,
                    fontWeight: FontWeight.w800,
                    letterSpacing: 1.2,
                    color: _primary,
                  ),
                ),
              ),
              const SizedBox(height: 2),
            ],
            SizedBox(
              width: double.infinity,
              height: 52,
              child: FilledButton(
                onPressed: _canContinue ? _handleContinue : null,
                style: FilledButton.styleFrom(
                  backgroundColor: _primary,
                  foregroundColor: Colors.white,
                  disabledBackgroundColor: _primary.withValues(alpha: 0.40),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(26),
                  ),
                  elevation: _canContinue ? 4 : 0,
                  shadowColor: _primary.withValues(alpha: 0.35),
                ),
                child: Text(
                  _step == _SetupStep.dueDateResult
                      ? 'Tạo hành trình'
                      : 'Tiếp theo',
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 16,
                    fontWeight: FontWeight.w800,
                    letterSpacing: 0.9,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _MethodCard extends StatelessWidget {
  const _MethodCard({
    super.key,
    this.authorityHint,
    required this.badge,
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.accentColor,
    required this.selectedBg,
    required this.selected,
    required this.onTap,
  });

  final String badge;
  final String? authorityHint;
  final String title;
  final String subtitle;
  final IconData icon;
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
      label: [
        title,
        subtitle,
        if (authorityHint != null) authorityHint!,
      ].join('. '),
      onTap: onTap,
      child: ExcludeSemantics(
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 220),
          curve: Curves.easeInOut,
          decoration: BoxDecoration(
            color: selected ? selectedBg : Colors.white,
            borderRadius: BorderRadius.circular(20),
            border: Border.all(
              color: selected
                  ? accentColor
                  : _borderNormal.withValues(alpha: 0.8),
              width: selected ? 2.2 : 1.2,
            ),
            boxShadow: [
              BoxShadow(
                color: selected
                    ? accentColor.withValues(alpha: 0.16)
                    : const Color(0xFF5A463F).withValues(alpha: 0.04),
                blurRadius: selected ? 20 : 12,
                offset: Offset(0, selected ? 8 : 4),
              ),
            ],
          ),
          child: Material(
            color: Colors.transparent,
            borderRadius: BorderRadius.circular(20),
            child: InkWell(
              borderRadius: BorderRadius.circular(20),
              onTap: onTap,
              splashColor: accentColor.withValues(alpha: 0.12),
              highlightColor: accentColor.withValues(alpha: 0.06),
              child: Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 14,
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Container(
                          width: 44,
                          height: 44,
                          decoration: BoxDecoration(
                            color: selected
                                ? accentColor
                                : accentColor.withValues(alpha: 0.14),
                            borderRadius: BorderRadius.circular(14),
                          ),
                          child: Icon(
                            icon,
                            color: selected ? Colors.white : accentColor,
                            size: 24,
                          ),
                        ),
                        const SizedBox(width: 12),
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
                                  fontSize: 15,
                                  fontWeight: FontWeight.w700,
                                  color: _textDark,
                                  height: 1.25,
                                ),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(width: 8),
                        AnimatedContainer(
                          duration: const Duration(milliseconds: 180),
                          width: 26,
                          height: 26,
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
                                  size: 16,
                                )
                              : null,
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    Text(
                      [
                        subtitle,
                        if (authorityHint != null) authorityHint!,
                      ].join(' '),
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 13,
                        color: _textMuted,
                        height: 1.4,
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

class _NumberWheel extends StatelessWidget {
  const _NumberWheel({
    required this.min,
    required this.max,
    required this.value,
    required this.label,
    required this.onChanged,
  });

  final int min;
  final int max;
  final int value;
  final String label;
  final ValueChanged<int> onChanged;

  static const _primary = Color(0xFF845143);
  static const _textDark = Color(0xFF2E211C);
  static const _textMuted = Color(0xFF7A6860);
  static const _surfaceLow = Color(0xFFFAF2EF);

  @override
  Widget build(BuildContext context) {
    final values = List<int>.generate(max - min + 1, (index) => min + index);
    final initialItem = values
        .indexOf(value)
        .clamp(0, values.length - 1)
        .toInt();
    return Stack(
      alignment: Alignment.center,
      children: [
        Container(
          height: 52,
          decoration: BoxDecoration(
            color: _surfaceLow,
            borderRadius: BorderRadius.circular(14),
          ),
        ),
        CupertinoPicker(
          scrollController: FixedExtentScrollController(
            initialItem: initialItem,
          ),
          itemExtent: 52,
          magnification: 1.12,
          squeeze: 1.05,
          useMagnifier: true,
          selectionOverlay: const SizedBox.shrink(),
          onSelectedItemChanged: (index) => onChanged(values[index]),
          children: values
              .map(
                (item) => Center(
                  child: RichText(
                    text: TextSpan(
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        color: _textMuted,
                        fontSize: 26,
                        fontWeight: FontWeight.w700,
                      ),
                      children: [
                        TextSpan(
                          text: '$item',
                          style: TextStyle(
                            color: item == value
                                ? _primary
                                : _textDark.withValues(alpha: 0.55),
                            fontSize: item == value ? 32 : 26,
                            fontWeight: FontWeight.w900,
                          ),
                        ),
                        TextSpan(
                          text: ' $label',
                          style: TextStyle(
                            color: item == value ? _primary : _textMuted,
                            fontSize: item == value ? 14 : 12,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              )
              .toList(),
        ),
      ],
    );
  }
}

class _InfoPill extends StatelessWidget {
  const _InfoPill({required this.icon, required this.text});

  final IconData icon;
  final String text;

  static const _surfaceLow = Color(0xFFFAF2EF);
  static const _textDark = Color(0xFF2E211C);
  static const _primary = Color(0xFF845143);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
      decoration: BoxDecoration(
        color: _surfaceLow,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: _primary.withValues(alpha: 0.15)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, color: _primary, size: 18),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              text,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: _textDark,
                height: 1.3,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _ResultMetric extends StatelessWidget {
  const _ResultMetric({
    required this.icon,
    required this.label,
    required this.value,
  });

  final IconData icon;
  final String label;
  final String value;

  static const _primary = Color(0xFF845143);
  static const _surfaceLow = Color(0xFFFAF2EF);
  static const _textDark = Color(0xFF2E211C);
  static const _textMuted = Color(0xFF7A6860);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        color: _surfaceLow,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: _primary.withValues(alpha: 0.12)),
      ),
      child: Row(
        children: [
          Container(
            width: 36,
            height: 36,
            decoration: const BoxDecoration(
              color: Colors.white,
              shape: BoxShape.circle,
            ),
            child: Icon(icon, color: _primary, size: 20),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    color: _textMuted,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  value,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 15,
                    fontWeight: FontWeight.w800,
                    color: _textDark,
                    height: 1.25,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _InsightCard extends StatelessWidget {
  const _InsightCard({required this.icon, required this.text});

  final IconData icon;
  final String text;

  static const _surface = Colors.white;
  static const _surfaceLow = Color(0xFFFAF2EF);
  static const _textDark = Color(0xFF2E211C);
  static const _primary = Color(0xFF845143);
  static const _borderNormal = Color(0xFFE8DDD6);

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(18, 20, 18, 18),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: _borderNormal.withValues(alpha: 0.8)),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF5A463F).withValues(alpha: 0.05),
            blurRadius: 20,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: Column(
        children: [
          Container(
            width: 46,
            height: 46,
            decoration: const BoxDecoration(
              color: _surfaceLow,
              shape: BoxShape.circle,
            ),
            child: Icon(icon, color: _primary, size: 24),
          ),
          const SizedBox(height: 12),
          Text(
            text,
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: _textDark,
              height: 1.45,
            ),
          ),
        ],
      ),
    );
  }
}

extension on DateTime {
  DateTime clampDate(DateTime min, DateTime max) {
    if (isBefore(min)) return min;
    if (isAfter(max)) return max;
    return this;
  }
}
