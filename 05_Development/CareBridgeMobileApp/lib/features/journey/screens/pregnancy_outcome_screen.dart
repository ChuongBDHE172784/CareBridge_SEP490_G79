import 'package:flutter/material.dart';
import 'package:uuid/uuid.dart';

import '../../../core/network/api_client.dart';
import '../../../core/auth/auth_state.dart';
import '../models/journey_model.dart';
import '../services/pregnancy_outcome_draft_store.dart';
import '../services/journey_service.dart';

typedef PregnancyOutcomeSubmit =
    Future<PregnancyOutcomeResult> Function(
      RecordPregnancyOutcomeRequest request,
    );

class PregnancyOutcomeScreen extends StatefulWidget {
  const PregnancyOutcomeScreen({
    super.key,
    required this.journeyId,
    required this.journeyVersion,
    this.currentOutcome,
    this.submitOutcome,
    this.draftStore,
    this.accountId,
  });

  final String journeyId;
  final int journeyVersion;
  final PregnancyOutcome? currentOutcome;
  final PregnancyOutcomeSubmit? submitOutcome;
  final PregnancyOutcomeDraftStore? draftStore;
  final String? accountId;

  @override
  State<PregnancyOutcomeScreen> createState() => _PregnancyOutcomeScreenState();
}

class _PregnancyOutcomeScreenState extends State<PregnancyOutcomeScreen> {
  static const _background = Color(0xFFF6F1EC);
  static const _surface = Colors.white;
  static const _nestedSurface = Color(0xFFF2EAE4);
  static const _accent = Color(0xFFC98C7B);
  static const _accentPressed = Color(0xFFB67868);
  static const _text = Color(0xFF5A463F);
  static const _secondaryText = Color(0xFF9C857C);
  static const _border = Color(0xFFE8DDD6);

  PregnancyOutcome? _selected;
  DateTime? _outcomeDate;
  DateTime? _effectiveAt;
  bool _correction = false;
  bool _submitting = false;
  String? _error;
  PregnancyOutcomeResult? _result;
  late String _submissionId;
  late final String? _accountId;
  late final PregnancyOutcomeDraftStore _draftStore;

  @override
  void initState() {
    super.initState();
    _submissionId = const Uuid().v4();
    _accountId = widget.accountId ?? AuthState.instance.userId;
    _draftStore =
        widget.draftStore ?? SecurePregnancyOutcomeDraftStore.instance;
    _restoreDraft();
  }

  Future<void> _restoreDraft() async {
    final accountId = _accountId;
    if (accountId == null) return;
    final draft = await _draftStore.read(accountId, widget.journeyId);
    if (!mounted || draft == null || !_sameAccount()) return;
    setState(() {
      _submissionId = draft.submissionId;
      _selected = draft.outcome;
      _outcomeDate = draft.outcomeDate;
      _effectiveAt = draft.effectiveAt;
      _correction = draft.correction;
    });
  }

  bool _sameAccount() =>
      widget.accountId != null || AuthState.instance.userId == _accountId;

  Future<void> _saveDraft() async {
    final accountId = _accountId;
    if (accountId == null || !_sameAccount()) return;
    await _draftStore.write(
      accountId,
      widget.journeyId,
      PregnancyOutcomeDraft(
        submissionId: _submissionId,
        outcome: _selected,
        outcomeDate: _outcomeDate,
        effectiveAt: _effectiveAt,
        correction: _correction,
      ),
    );
  }

  Future<PregnancyOutcomeResult> _submit(
    RecordPregnancyOutcomeRequest request,
  ) {
    final callback = widget.submitOutcome;
    if (callback != null) return callback(request);
    return JourneyService().recordPregnancyOutcome(widget.journeyId, request);
  }

  Future<void> _pickDate() async {
    final now = DateTime.now();
    final selected = await showDatePicker(
      context: context,
      initialDate: _outcomeDate ?? now,
      firstDate: DateTime(now.year - 2),
      lastDate: now,
      helpText: 'Chọn ngày',
      cancelText: 'Hủy',
      confirmText: 'Chọn',
    );
    if (selected != null && mounted) {
      setState(() {
        _outcomeDate = selected;
        _error = null;
      });
      await _saveDraft();
    }
  }

  Future<void> _continue() async {
    if (!_sameAccount()) {
      setState(
        () => _error = 'Tài khoản đã thay đổi. Vui lòng mở lại màn hình.',
      );
      return;
    }
    final selected = _selected;
    if (selected == null) {
      setState(() => _error = 'Vui lòng chọn một tình trạng');
      return;
    }
    if (selected.requiresDate && _outcomeDate == null) {
      setState(() => _error = 'Vui lòng chọn ngày');
      return;
    }

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: _surface,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(32)),
        title: const Text(
          'Xác nhận cập nhật',
          style: TextStyle(
            color: _text,
            fontSize: 22,
            fontWeight: FontWeight.w800,
          ),
        ),
        content: Text(
          _confirmationMessage(selected),
          style: const TextStyle(color: _text, fontSize: 16, height: 1.45),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Quay lại'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(true),
            style: FilledButton.styleFrom(
              backgroundColor: _accent,
              minimumSize: const Size(112, 48),
              shape: const StadiumBorder(),
            ),
            child: const Text('Xác nhận'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;

    final selectedCorrection = _requiresCorrection();
    final effectiveAt = _effectiveAt ?? DateTime.now().toUtc();
    setState(() {
      _effectiveAt = effectiveAt;
      _correction = selectedCorrection;
    });
    await _saveDraft();
    if (!mounted) return;

    setState(() {
      _submitting = true;
      _error = null;
    });
    try {
      final result = await _submit(
        RecordPregnancyOutcomeRequest(
          submissionId: _submissionId,
          expectedJourneyVersion: widget.journeyVersion,
          outcomeType: selected,
          outcomeDate: _outcomeDate,
          source: 'SELF_REPORTED',
          reason: 'MOTHER_OUTCOME_CONFIRMATION',
          effectiveAt: effectiveAt,
          correction: selectedCorrection,
        ),
      );
      if (!mounted) return;
      setState(() {
        _result = result;
        _submitting = false;
      });
      final accountId = _accountId;
      if (accountId != null) {
        await _draftStore.clear(accountId, widget.journeyId);
      }
    } on ApiException catch (error) {
      if (!mounted) return;
      setState(() {
        _error = _safeError(error);
        _submitting = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _error = 'Không thể cập nhật lúc này. Vui lòng thử lại.';
        _submitting = false;
      });
    }
  }

  String _confirmationMessage(PregnancyOutcome outcome) {
    if (outcome == PregnancyOutcome.liveBirth) {
      return 'Hành trình sẽ chuyển sang giai đoạn sau sinh. Bạn có thể tạo, liên kết hoặc để hồ sơ em bé cho lần sau.';
    }
    if (outcome == PregnancyOutcome.pregnancyLoss ||
        outcome == PregnancyOutcome.stillbirth) {
      return 'Hành trình sẽ chuyển sang hỗ trợ hồi phục và không yêu cầu tạo hồ sơ em bé.';
    }
    return 'Thông tin này sẽ được lưu vào lịch sử hành trình. Bạn có thể cập nhật lại khi có thông tin mới.';
  }

  bool _requiresCorrection() {
    final current = widget.currentOutcome;
    return current != null && current.transitionsToPostpartum;
  }

  String _safeError(ApiException error) {
    final message = error.message;
    if (message.contains('JOURNEY_VERSION_CONFLICT')) {
      return 'Hành trình vừa được cập nhật. Vui lòng tải lại và thử lại.';
    }
    if (message.contains('OUTCOME_SUBMISSION_CONFLICT')) {
      return 'Lần gửi này không còn khớp với dữ liệu hiện tại.';
    }
    if (message.contains('OUTCOME_STAGE_CONFLICT')) {
      return 'Tình trạng này không còn phù hợp với giai đoạn hiện tại.';
    }
    return 'Không thể cập nhật lúc này. Vui lòng thử lại.';
  }

  @override
  Widget build(BuildContext context) {
    if (_result != null) return _buildSuccess();
    return Scaffold(
      backgroundColor: _background,
      appBar: AppBar(
        backgroundColor: _background,
        foregroundColor: _text,
        elevation: 0,
        title: const Text(
          'Cập nhật tình trạng thai kỳ',
          style: TextStyle(fontWeight: FontWeight.w800),
        ),
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 12, 20, 32),
          children: [
            const Text(
              'Chọn thông tin phù hợp nhất với bạn lúc này',
              style: TextStyle(
                color: _text,
                fontSize: 20,
                fontWeight: FontWeight.w800,
              ),
            ),
            const SizedBox(height: 8),
            const Text(
              'Các lựa chọn có giá trị như nhau. CareBridge chỉ dùng thông tin này để điều chỉnh hỗ trợ.',
              style: TextStyle(
                color: _secondaryText,
                fontSize: 16,
                height: 1.45,
              ),
            ),
            const SizedBox(height: 24),
            ...PregnancyOutcome.values.map(_buildOutcomeChoice),
            if (_selected?.requiresDate == true) ...[
              const SizedBox(height: 8),
              Semantics(
                button: true,
                label: 'Chọn ngày em bé chào đời',
                child: OutlinedButton.icon(
                  onPressed: _submitting ? null : _pickDate,
                  icon: const Icon(Icons.calendar_month_rounded),
                  label: Text(
                    _outcomeDate == null
                        ? 'Chọn ngày'
                        : _formatDate(_outcomeDate!),
                  ),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: _text,
                    minimumSize: const Size.fromHeight(52),
                    side: const BorderSide(color: _border, width: 2),
                    shape: const StadiumBorder(),
                  ),
                ),
              ),
            ],
            if (_error != null) ...[
              const SizedBox(height: 16),
              Semantics(
                liveRegion: true,
                child: Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: _nestedSurface,
                    borderRadius: BorderRadius.circular(18),
                    border: const Border(
                      left: BorderSide(color: _accent, width: 4),
                    ),
                  ),
                  child: Text(
                    _error!,
                    style: const TextStyle(
                      color: _text,
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ),
            ],
            const SizedBox(height: 24),
            FilledButton(
              onPressed: _submitting ? null : _continue,
              style: FilledButton.styleFrom(
                backgroundColor: _accent,
                disabledBackgroundColor: _border,
                minimumSize: const Size.fromHeight(54),
                shape: const StadiumBorder(),
              ),
              child: _submitting
                  ? const SizedBox(
                      width: 24,
                      height: 24,
                      child: CircularProgressIndicator(
                        strokeWidth: 2.5,
                        color: _surface,
                      ),
                    )
                  : const Text(
                      'Tiếp tục',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildOutcomeChoice(PregnancyOutcome outcome) {
    final selected = _selected == outcome;
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Semantics(
        button: true,
        selected: selected,
        label: outcome.displayLabel,
        child: InkWell(
          onTap: _submitting
              ? null
              : () async {
                  setState(() {
                    _selected = outcome;
                    _error = null;
                    if (!outcome.requiresDate) _outcomeDate = null;
                  });
                  await _saveDraft();
                },
          borderRadius: BorderRadius.circular(24),
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 180),
            constraints: const BoxConstraints(minHeight: 64),
            padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 16),
            decoration: BoxDecoration(
              color: selected ? _nestedSurface : _surface,
              borderRadius: BorderRadius.circular(24),
              border: Border.all(color: selected ? _accent : _border, width: 2),
              boxShadow: const [
                BoxShadow(
                  color: Color(0x0F5A463F),
                  blurRadius: 24,
                  offset: Offset(0, 8),
                ),
              ],
            ),
            child: Row(
              children: [
                Icon(
                  selected
                      ? Icons.check_circle_rounded
                      : Icons.radio_button_unchecked_rounded,
                  color: selected ? _accentPressed : _secondaryText,
                  size: 28,
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Text(
                    outcome.displayLabel,
                    style: const TextStyle(
                      color: _text,
                      fontSize: 16,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildSuccess() {
    final result = _result!;
    return Scaffold(
      backgroundColor: _background,
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Container(
              padding: const EdgeInsets.all(28),
              decoration: BoxDecoration(
                color: _surface,
                borderRadius: BorderRadius.circular(32),
                boxShadow: const [
                  BoxShadow(
                    color: Color(0x125A463F),
                    blurRadius: 32,
                    offset: Offset(0, 12),
                  ),
                ],
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(
                    Icons.check_circle_outline_rounded,
                    color: _accent,
                    size: 56,
                  ),
                  const SizedBox(height: 18),
                  const Text(
                    'Đã cập nhật hành trình',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      color: _text,
                      fontSize: 24,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  const SizedBox(height: 10),
                  Text(
                    result.journeyType == 'POSTPARTUM'
                        ? 'Bạn có thể tiếp tục với hỗ trợ hồi phục. Hồ sơ em bé luôn là lựa chọn và có thể thực hiện sau.'
                        : 'Hành trình mang thai vẫn được giữ nguyên. Bạn có thể cập nhật khi có thông tin mới.',
                    textAlign: TextAlign.center,
                    style: const TextStyle(
                      color: _secondaryText,
                      fontSize: 16,
                      height: 1.45,
                    ),
                  ),
                  const SizedBox(height: 24),
                  FilledButton(
                    onPressed: () => Navigator.of(context).maybePop(result),
                    style: FilledButton.styleFrom(
                      backgroundColor: _accent,
                      minimumSize: const Size.fromHeight(52),
                      shape: const StadiumBorder(),
                    ),
                    child: const Text('Hoàn tất'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  String _formatDate(DateTime date) {
    final day = date.day.toString().padLeft(2, '0');
    final month = date.month.toString().padLeft(2, '0');
    return '$day/$month/${date.year}';
  }
}
