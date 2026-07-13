import 'dart:async';

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
  });

  final String? journeyId;
  final bool isEditMode;

  @override
  State<JourneySetupScreen> createState() => _JourneySetupScreenState();
}

enum _SetupStep {
  method,
  lmpDate,
  conceptionDate,
  gestationalAge,
  dueDate,
  cycleLength,
  dueDateResult,
  loading,
}

enum _DatingMethod { lmp, conception, gestationalAge, dueDate }

class _JourneySetupScreenState extends State<JourneySetupScreen> {
  static const _primary = Color(0xFFC98C7B);
  static const _primaryDark = Color(0xFF845143);
  static const _canvas = Color(0xFFF6F1EC);
  static const _surface = Colors.white;
  static const _text = Color(0xFF5A463F);
  static const _muted = Color(0xFF9C857C);
  static const _border = Color(0xFFE8DDD6);
  static const _errorBg = Color(0xFFFFDAD6);
  static const _errorText = Color(0xFF93000A);

  final _service = JourneyService();
  final List<_SetupStep> _history = [];

  _SetupStep _step = _SetupStep.method;
  _DatingMethod? _selectedMethod;
  DateTime? _lmpDate;
  DateTime? _conceptionDate;
  DateTime? _dueDate;
  int _gestationalWeeks = 4;
  int _gestationalDays = 0;
  int _cycleLength = 28;
  bool _cycleUnknown = false;
  bool _loading = false;
  String? _error;

  DateTime get _today {
    final now = DateTime.now();
    return DateTime(now.year, now.month, now.day);
  }

  int get _effectiveCycleLength => _cycleUnknown ? 28 : _cycleLength;

  int get _enteredGestationalAgeDays =>
      (_gestationalWeeks * 7) + _gestationalDays;

  DateTime? get _calculatedDueDate {
    switch (_selectedMethod) {
      case _DatingMethod.lmp:
        final lmp = _lmpDate;
        if (lmp == null) return null;
        return lmp.add(Duration(days: 280 + (_effectiveCycleLength - 28)));
      case _DatingMethod.conception:
        final conception = _conceptionDate;
        if (conception == null) return null;
        return conception.add(const Duration(days: 266));
      case _DatingMethod.gestationalAge:
        return _today.add(Duration(days: 280 - _enteredGestationalAgeDays));
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
      case _SetupStep.conceptionDate:
        return _conceptionDate != null;
      case _SetupStep.dueDate:
        return _dueDate != null;
      case _SetupStep.gestationalAge:
        return _enteredGestationalAgeDays > 0 &&
            _enteredGestationalAgeDays <= 294;
      case _SetupStep.cycleLength:
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
      case _SetupStep.conceptionDate:
      case _SetupStep.gestationalAge:
      case _SetupStep.dueDate:
        return 2;
      case _SetupStep.cycleLength:
        return 3;
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
    if (_selectedMethod == _DatingMethod.lmp) {
      parts.add('Cycle length: ${_cycleUnknown ? 'unknown' : _cycleLength}');
    }
    if (_lmpDate != null) parts.add('LMP: ${_formatApiDate(_lmpDate!)}');
    if (_conceptionDate != null) {
      parts.add('Conception: ${_formatApiDate(_conceptionDate!)}');
    }
    if (_dueDate != null) parts.add('Doctor EDD: ${_formatApiDate(_dueDate!)}');
    if (_selectedMethod == _DatingMethod.gestationalAge) {
      parts.add('Gestational age: ${_gestationalWeeks}w${_gestationalDays}d');
    }
    return parts.join('; ');
  }

  void _goTo(_SetupStep next) {
    setState(() {
      _history.add(_step);
      _step = next;
      _error = null;
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
    Navigator.of(context).maybePop();
  }

  void _handleContinue() {
    if (!_canContinue) return;
    switch (_step) {
      case _SetupStep.method:
        switch (_selectedMethod) {
          case _DatingMethod.lmp:
            _goTo(_SetupStep.lmpDate);
          case _DatingMethod.conception:
            _goTo(_SetupStep.conceptionDate);
          case _DatingMethod.gestationalAge:
            _goTo(_SetupStep.gestationalAge);
          case _DatingMethod.dueDate:
            _goTo(_SetupStep.dueDate);
          case null:
            break;
        }
      case _SetupStep.lmpDate:
        _goTo(_SetupStep.cycleLength);
      case _SetupStep.conceptionDate:
      case _SetupStep.gestationalAge:
      case _SetupStep.dueDate:
        _goTo(_SetupStep.dueDateResult);
      case _SetupStep.cycleLength:
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
            lastMenstrualDate: _lmpDate != null
                ? _formatApiDate(_lmpDate!)
                : null,
            estimatedDueDate: _formatApiDate(dueDate),
            notes: _notes(),
          ),
        );
      } else {
        await _service.createJourney(
          CreateJourneyRequest(
            journeyType: JourneyType.pregnancy,
            startDate: _formatApiDate(_today),
            lastMenstrualDate: _lmpDate != null
                ? _formatApiDate(_lmpDate!)
                : null,
            estimatedDueDate: _formatApiDate(dueDate),
            notes: _notes(),
          ),
        );
      }
      await AuthService.instance.refreshSession();
      if (!mounted) return;
      if (isUpdate) {
        context.pop();
      } else {
        context.go('/');
      }
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _step = _SetupStep.dueDateResult;
        _loading = false;
        _error = isUpdate
            ? 'Không thể cập nhật hành trình. Vui lòng thử lại.'
            : e.statusCode == 409
            ? 'Bạn đã có một hành trình đang hoạt động.'
            : 'Không thể tạo hành trình. Vui lòng thử lại.';
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _step = _SetupStep.dueDateResult;
        _loading = false;
        _error = isUpdate
            ? 'Lỗi kết nối. Không thể cập nhật hành trình.'
            : 'Lỗi kết nối. Vui lòng kiểm tra đường truyền.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop) _handleBack();
      },
      child: Scaffold(
        backgroundColor: _canvas,
        body: SafeArea(
          child: Stack(
            children: [
              Column(
                children: [
                  _buildTopBar(),
                  _buildProgressBar(),
                  Expanded(
                    child: AnimatedSwitcher(
                      duration: const Duration(milliseconds: 260),
                      switchInCurve: Curves.easeOutCubic,
                      switchOutCurve: Curves.easeInCubic,
                      child: SingleChildScrollView(
                        key: ValueKey(_step),
                        padding: const EdgeInsets.fromLTRB(24, 24, 24, 170),
                        child: _buildStepContent(),
                      ),
                    ),
                  ),
                ],
              ),
              Positioned(
                left: 0,
                right: 0,
                bottom: 0,
                child: _buildBottomBar(),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildTopBar() {
    return SizedBox(
      height: 56,
      child: Row(
        children: [
          const SizedBox(width: 8),
          IconButton(
            onPressed: _loading ? null : _handleBack,
            icon: const Icon(Icons.arrow_back_rounded),
            color: _text,
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
                color: _muted,
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
      padding: const EdgeInsets.fromLTRB(24, 4, 24, 8),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(99),
        child: Stack(
          children: [
            Container(height: 8, color: _border.withAlpha(150)),
            FractionallySizedBox(
              widthFactor: _progress,
              child: Container(height: 8, color: _primary),
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
      case _SetupStep.conceptionDate:
        return _buildDateStep(
          title: 'Ngày thụ thai',
          selectedDate: _conceptionDate,
          firstDate: _today.subtract(const Duration(days: 280)),
          lastDate: _today,
          onChanged: (date) => setState(() => _conceptionDate = date),
        );
      case _SetupStep.gestationalAge:
        return _buildGestationalAgeStep();
      case _SetupStep.dueDate:
        return _buildDateStep(
          title: 'Ngày sinh dự kiến',
          selectedDate: _dueDate,
          firstDate: _today.subtract(const Duration(days: 14)),
          lastDate: _today.add(const Duration(days: 294)),
          onChanged: (date) => setState(() => _dueDate = date),
        );
      case _SetupStep.cycleLength:
        return _buildCycleLengthStep();
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
          'Hãy xác định thời gian mang thai của bạn. Bạn đã biết gì rồi?',
          textAlign: TextAlign.center,
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 30,
            fontWeight: FontWeight.w800,
            color: _primaryDark,
            height: 1.22,
          ),
        ),
        const SizedBox(height: 34),
        _MethodCard(
          title: 'Tôi nhớ ngày đầu tiên của chu kỳ',
          subtitle: 'CareBridge sẽ tính dự sinh từ kỳ kinh cuối.',
          icon: Icons.event_note_rounded,
          selected: _selectedMethod == _DatingMethod.lmp,
          onTap: () => setState(() => _selectedMethod = _DatingMethod.lmp),
        ),
        const SizedBox(height: 14),
        _MethodCard(
          title: 'Tôi có thể chỉ định ngày thụ thai',
          subtitle: 'Phù hợp khi bạn nhớ mốc rụng trứng hoặc IVF.',
          icon: Icons.favorite_rounded,
          selected: _selectedMethod == _DatingMethod.conception,
          onTap: () =>
              setState(() => _selectedMethod = _DatingMethod.conception),
        ),
        const SizedBox(height: 14),
        _MethodCard(
          title: 'Tôi đã biết thời gian sản khoa của mình',
          subtitle: 'Nhập số tuần và số ngày thai hiện tại.',
          icon: Icons.timeline_rounded,
          selected: _selectedMethod == _DatingMethod.gestationalAge,
          onTap: () =>
              setState(() => _selectedMethod = _DatingMethod.gestationalAge),
        ),
        const SizedBox(height: 14),
        _MethodCard(
          title: 'Bác sĩ đã nói cho tôi ngày dự sinh dự kiến',
          subtitle: 'Dùng ngày dự sinh đã được tư vấn.',
          icon: Icons.medical_information_rounded,
          selected: _selectedMethod == _DatingMethod.dueDate,
          onTap: () => setState(() => _selectedMethod = _DatingMethod.dueDate),
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
            fontSize: 30,
            fontWeight: FontWeight.w800,
            color: _primaryDark,
            height: 1.22,
          ),
        ),
        const SizedBox(height: 34),
        Container(
          decoration: BoxDecoration(
            color: _surface,
            borderRadius: BorderRadius.circular(28),
            border: Border.all(color: _border.withAlpha(180)),
            boxShadow: [
              BoxShadow(
                color: _text.withAlpha(16),
                blurRadius: 32,
                offset: const Offset(0, 14),
              ),
            ],
          ),
          padding: const EdgeInsets.fromLTRB(12, 18, 12, 12),
          child: Theme(
            data: Theme.of(context).copyWith(
              colorScheme: const ColorScheme.light(
                primary: _primary,
                onPrimary: Colors.white,
                surface: _surface,
                onSurface: _text,
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
        const SizedBox(height: 20),
        _InfoPill(
          icon: Icons.calendar_today_rounded,
          text: selectedDate == null
              ? 'Chọn một ngày để tiếp tục'
              : 'Đã chọn ${_formatDisplayDate(selectedDate)}',
        ),
      ],
    );
  }

  Widget _buildGestationalAgeStep() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Thai kỳ',
          textAlign: TextAlign.center,
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 34,
            fontWeight: FontWeight.w800,
            color: _primaryDark,
          ),
        ),
        const SizedBox(height: 42),
        Container(
          height: 220,
          decoration: BoxDecoration(
            color: _surface,
            borderRadius: BorderRadius.circular(28),
            border: Border.all(color: _border.withAlpha(180)),
            boxShadow: [
              BoxShadow(
                color: _text.withAlpha(16),
                blurRadius: 32,
                offset: const Offset(0, 14),
              ),
            ],
          ),
          child: Row(
            children: [
              Expanded(
                child: _NumberWheel(
                  min: 0,
                  max: 42,
                  value: _gestationalWeeks,
                  label: 'tuần',
                  onChanged: (value) =>
                      setState(() => _gestationalWeeks = value),
                ),
              ),
              Container(width: 1, height: 160, color: _border),
              Expanded(
                child: _NumberWheel(
                  min: 0,
                  max: 6,
                  value: _gestationalDays,
                  label: 'ngày',
                  onChanged: (value) =>
                      setState(() => _gestationalDays = value),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 20),
        _InfoPill(
          icon: Icons.timeline_rounded,
          text:
              'Tuổi thai hiện tại: $_gestationalWeeks tuần và $_gestationalDays ngày',
        ),
      ],
    );
  }

  Widget _buildCycleLengthStep() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Chu kỳ kinh trung bình',
          textAlign: TextAlign.center,
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 30,
            fontWeight: FontWeight.w800,
            color: _primaryDark,
            height: 1.22,
          ),
        ),
        const SizedBox(height: 36),
        Container(
          height: 220,
          decoration: BoxDecoration(
            color: _surface,
            borderRadius: BorderRadius.circular(28),
            border: Border.all(color: _border.withAlpha(180)),
            boxShadow: [
              BoxShadow(
                color: _text.withAlpha(16),
                blurRadius: 32,
                offset: const Offset(0, 14),
              ),
            ],
          ),
          child: _NumberWheel(
            min: 21,
            max: 35,
            value: _cycleLength,
            label: 'ngày',
            onChanged: (value) => setState(() {
              _cycleLength = value;
              _cycleUnknown = false;
            }),
          ),
        ),
        const SizedBox(height: 20),
        TextButton(
          onPressed: () {
            setState(() => _cycleUnknown = true);
            _goTo(_SetupStep.dueDateResult);
          },
          child: const Text(
            'KHÔNG BIẾT',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 15,
              fontWeight: FontWeight.w800,
              letterSpacing: 1.2,
              color: _primary,
            ),
          ),
        ),
        const SizedBox(height: 6),
        _InfoPill(
          icon: Icons.lightbulb_rounded,
          text: _cycleUnknown
              ? 'CareBridge sẽ dùng chu kỳ chuẩn 28 ngày'
              : 'Chu kỳ trung bình: $_cycleLength ngày',
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
            fontSize: 30,
            fontWeight: FontWeight.w800,
            color: _primaryDark,
            height: 1.22,
          ),
        ),
        const SizedBox(height: 16),
        const Text(
          'Bạn đang mang thai',
          textAlign: TextAlign.center,
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 16,
            fontWeight: FontWeight.w700,
            color: _muted,
            height: 1.4,
          ),
        ),
        const SizedBox(height: 32),
        Container(
          padding: const EdgeInsets.fromLTRB(20, 24, 20, 22),
          decoration: BoxDecoration(
            color: _surface,
            borderRadius: BorderRadius.circular(28),
            border: Border.all(color: _border.withAlpha(180)),
            boxShadow: [
              BoxShadow(
                color: _text.withAlpha(16),
                blurRadius: 32,
                offset: const Offset(0, 14),
              ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Container(
                width: 72,
                height: 72,
                decoration: BoxDecoration(
                  color: _primary.withAlpha(30),
                  shape: BoxShape.circle,
                ),
                child: const Icon(
                  Icons.child_care_rounded,
                  color: _primary,
                  size: 38,
                ),
              ),
              const SizedBox(height: 22),
              RichText(
                textAlign: TextAlign.center,
                text: TextSpan(
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 20,
                    fontWeight: FontWeight.w800,
                    color: _text,
                    height: 1.25,
                  ),
                  children: [
                    TextSpan(
                      text: '$obstetricWeeks tuần ',
                      style: const TextStyle(
                        color: _primary,
                        fontSize: 34,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                    TextSpan(text: '$obstetricDays ngày'),
                  ],
                ),
              ),
              const SizedBox(height: 26),
              _ResultMetric(
                icon: Icons.timeline_rounded,
                label: 'Tuổi thai nhi',
                value: '$fetalWeeks tuần $fetalDays ngày',
              ),
              const SizedBox(height: 12),
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
        const SizedBox(height: 24),
        _InsightCard(
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
                    CircularProgressIndicator(
                      value: value,
                      strokeWidth: 14,
                      strokeCap: StrokeCap.round,
                      backgroundColor: _border,
                      color: _primary,
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
              color: _text,
              height: 1.3,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBottomBar() {
    if (_step == _SetupStep.loading) return const SizedBox.shrink();
    return Container(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.bottomCenter,
          end: Alignment.topCenter,
          colors: [_canvas, _canvas, Color(0x00F6F1EC)],
          stops: [0.0, 0.72, 1.0],
        ),
      ),
      padding: const EdgeInsets.fromLTRB(24, 44, 24, 24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (_error != null) ...[
            Container(
              width: double.infinity,
              margin: const EdgeInsets.only(bottom: 12),
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
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
            const SizedBox(height: 8),
          ],
          SizedBox(
            width: double.infinity,
            height: 56,
            child: FilledButton(
              onPressed: _canContinue ? _handleContinue : null,
              style: FilledButton.styleFrom(
                backgroundColor: _primary,
                foregroundColor: Colors.white,
                disabledBackgroundColor: _primary.withAlpha(115),
                shape: const StadiumBorder(),
                elevation: 0,
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
    );
  }
}

class _MethodCard extends StatelessWidget {
  const _MethodCard({
    required this.title,
    required this.subtitle,
    required this.icon,
    required this.selected,
    required this.onTap,
  });

  final String title;
  final String subtitle;
  final IconData icon;
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
    return AnimatedContainer(
      duration: const Duration(milliseconds: 220),
      decoration: BoxDecoration(
        color: selected ? _surfaceLow : _surface,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: selected ? _primary : _border, width: 2),
        boxShadow: [
          BoxShadow(
            color: _text.withAlpha(14),
            blurRadius: 26,
            offset: const Offset(0, 12),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(24),
        child: InkWell(
          borderRadius: BorderRadius.circular(24),
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Row(
              children: [
                Container(
                  width: 48,
                  height: 48,
                  decoration: BoxDecoration(
                    color: selected ? _primary : _surfaceLow,
                    shape: BoxShape.circle,
                  ),
                  child: Icon(icon, color: selected ? Colors.white : _primary),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        title,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 16,
                          fontWeight: FontWeight.w800,
                          color: _text,
                          height: 1.35,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        subtitle,
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 13,
                          color: _muted,
                          height: 1.35,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 10),
                AnimatedOpacity(
                  duration: const Duration(milliseconds: 160),
                  opacity: selected ? 1 : 0,
                  child: const Icon(
                    Icons.check_circle_rounded,
                    color: _primary,
                  ),
                ),
              ],
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

  static const _primary = Color(0xFFC98C7B);
  static const _text = Color(0xFF5A463F);
  static const _muted = Color(0xFF9C857C);
  static const _surfaceLow = Color(0xFFF2EAE4);

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
        Container(height: 58, color: _surfaceLow),
        CupertinoPicker(
          scrollController: FixedExtentScrollController(
            initialItem: initialItem,
          ),
          itemExtent: 58,
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
                        color: _muted,
                        fontSize: 28,
                        fontWeight: FontWeight.w700,
                      ),
                      children: [
                        TextSpan(
                          text: '$item',
                          style: TextStyle(
                            color: item == value
                                ? _primary
                                : _text.withAlpha(145),
                            fontSize: item == value ? 34 : 28,
                            fontWeight: FontWeight.w900,
                          ),
                        ),
                        TextSpan(
                          text: ' $label',
                          style: TextStyle(
                            color: item == value ? _primary : _muted,
                            fontSize: item == value ? 15 : 13,
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

  static const _surfaceLow = Color(0xFFF2EAE4);
  static const _text = Color(0xFF5A463F);
  static const _primary = Color(0xFFC98C7B);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: _surfaceLow,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, color: _primary, size: 18),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              text,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 13,
                fontWeight: FontWeight.w700,
                color: _text,
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

  static const _primary = Color(0xFFC98C7B);
  static const _surfaceLow = Color(0xFFF2EAE4);
  static const _text = Color(0xFF5A463F);
  static const _muted = Color(0xFF9C857C);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      decoration: BoxDecoration(
        color: _surfaceLow,
        borderRadius: BorderRadius.circular(18),
      ),
      child: Row(
        children: [
          Container(
            width: 38,
            height: 38,
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
                    fontSize: 13,
                    fontWeight: FontWeight.w700,
                    color: _muted,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  value,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 17,
                    fontWeight: FontWeight.w800,
                    color: _text,
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
  static const _surfaceLow = Color(0xFFF2EAE4);
  static const _text = Color(0xFF5A463F);
  static const _primary = Color(0xFFC98C7B);
  static const _border = Color(0xFFE8DDD6);

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(20, 26, 20, 20),
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(28),
        border: Border.all(color: _border.withAlpha(180)),
        boxShadow: [
          BoxShadow(
            color: _text.withAlpha(15),
            blurRadius: 28,
            offset: const Offset(0, 12),
          ),
        ],
      ),
      child: Column(
        children: [
          Container(
            width: 54,
            height: 54,
            decoration: const BoxDecoration(
              color: _surfaceLow,
              shape: BoxShape.circle,
            ),
            child: Icon(icon, color: _primary, size: 26),
          ),
          const SizedBox(height: 14),
          Text(
            text,
            textAlign: TextAlign.center,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 16,
              fontWeight: FontWeight.w700,
              color: _text,
              height: 1.55,
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
