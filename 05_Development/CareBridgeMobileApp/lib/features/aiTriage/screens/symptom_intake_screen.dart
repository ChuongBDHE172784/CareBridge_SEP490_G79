import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../../core/auth/auth_state.dart';
import '../../emergency/services/emergency_service.dart';
import '../models/triage_continuation.dart';
import '../models/triage_entry_context.dart';
import '../models/triage_intake_flow_model.dart';
import '../models/triage_chat_adapter.dart';
import '../models/triage_session.dart';
import '../models/triage_result_model.dart';
import '../services/triage_service.dart';
import '../services/triage_continuation_restore_coordinator.dart';
import '../services/triage_continuation_store.dart';

/// Stages that describe the mother rather than the baby.
const maternalTriageStages = {'PRECONCEPTION', 'PREGNANCY', 'POSTPARTUM'};
const canonicalTriageStages = {...maternalTriageStages, 'INFANT', 'TODDLER'};

/// Canonical routing metadata shared by the screen and its contract tests.
String canonicalTriageTargetForStage(String stage) =>
    maternalTriageStages.contains(stage) ? 'MOTHER' : 'BABY';

String canonicalTriageStage(String stage) => switch (stage) {
  'PRECONCEPTION' => 'PRECONCEPTION',
  'PREGNANCY' => 'PREGNANCY',
  'POSTPARTUM' => 'POSTPARTUM_MOTHER',
  'INFANT' => 'INFANT_0_12M',
  'TODDLER' => 'TODDLER_12_24M',
  _ => throw ArgumentError.value(stage, 'stage', 'Unsupported triage stage'),
};

class SymptomIntakeScreen extends StatefulWidget {
  final TriageService? triageService;
  final TriageSessionService? triageSessionService;
  final EmergencyService? emergencyService;
  final TriageEntryContext entryContext;
  final Future<bool> Function()? postpartumEmergencyLauncher;
  final TriageContinuationRestoreCoordinator? continuationCoordinator;

  const SymptomIntakeScreen({
    super.key,
    this.triageService,
    this.triageSessionService,
    this.emergencyService,
    this.entryContext = const TriageEntryContext(),
    this.postpartumEmergencyLauncher,
    this.continuationCoordinator,
  });

  @override
  State<SymptomIntakeScreen> createState() => _SymptomIntakeScreenState();
}

class _SymptomIntakeScreenState extends State<SymptomIntakeScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryDark = Color(0xFF845143);
  static const _surface = Color(0xFFF8F5F1);
  static const _surfaceLow = Color(0xFFF8EEE9);
  static const _onSurface = Color(0xFF2A211D);
  static const _onVariant = Color(0xFF655650);
  static const _outline = Color(0xFFF1E6E0);
  static const _maternalStages = maternalTriageStages;

  late final TriageService _service;
  late final EmergencyService _emergencyService;
  late final TriageContinuationRestoreCoordinator _continuationCoordinator;
  final _initialController = TextEditingController();
  final Map<String, TextEditingController> _answerControllers = {};
  final List<_ChatMessage> _messages = [];

  Map<String, dynamic> _currentIntake = _blankIntake();
  List<IntakeQuestion> _questions = [];
  final Map<String, dynamic> _answers = {};
  late String _selectedStage;
  String? _sessionId;

  TriageSession? _canonicalSession;
  late final TriageSessionService _sessionService;
  int _round = 1;
  bool _loading = false;
  late bool _stageConfirmed;
  bool _openingEmergency = false;
  bool _dialing115 = false;
  bool _emergencyFailed = false;
  bool _postpartumManualCallRequired = false;
  bool _returningToOrigin = false;
  String? _dialerNotice;
  TriageResult? _result;
  String? _error;
  String? _returnNotice;

  @override
  void initState() {
    super.initState();
    _service = widget.triageService ?? TriageService();
    _sessionService = widget.triageSessionService ?? TriageSessionService();
    _emergencyService = widget.emergencyService ?? EmergencyService();
    _continuationCoordinator =
        widget.continuationCoordinator ??
        TriageContinuationRestoreCoordinator(
          store: SecureTriageContinuationStore(),
          gateway: _service,
        );
    _selectedStage = widget.entryContext.stage.apiValue;
    _stageConfirmed = !widget.entryContext.requiresStageSelection;
    _currentIntake = _newIntake(stage: _selectedStage);
    _messages.add(
      _ChatMessage(
        role: _ChatRole.assistant,
        text: widget.entryContext.requiresStageSelection
            ? 'Hãy chọn đúng giai đoạn trước khi mô tả triệu chứng. CareBridge sẽ giữ nguyên ngữ cảnh đó trong suốt phiên.'
            : widget.entryContext.isMaternal
            ? 'Hãy mô tả dấu hiệu bạn đang gặp. CareBridge chỉ hỗ trợ phân loại rủi ro ban đầu theo giai đoạn sức khỏe hiện tại.'
            : 'Hãy mô tả triệu chứng của bé. CareBridge sẽ hỏi thêm nếu cần và chỉ phân loại rủi ro ban đầu.',
      ),
    );
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _checkInitialConsent();
    });
  }

  Future<void> _checkInitialConsent() async {
    final triageUserId = AuthState.instance.userId;
    try {
      await _requestTriageConsent(triageUserId);
    } catch (_) {
      // Ignore background failure; user will be prompted again upon submission if unconsented.
    }
  }

  Map<String, dynamic> _newIntake({required String stage}) => {
    ..._blankIntake(stage: stage),
    ...widget.entryContext.toLifecycleBindingJson(),
  };

  static Map<String, dynamic> _blankIntake({String stage = 'INFANT'}) {
    final common = <String, dynamic>{
      'stage': stage,
      'symptomList': <String>[],
      'duration': null,
      'temperatureC': null,
      'breathingStatus': null,
      'consciousnessStatus': null,
      'seizure': null,
      'painSeverity': null,
      'urinarySymptoms': null,
      'parentFreeText': null,
    };
    if (_maternalStages.contains(stage)) return common;
    return {
      ...common,
      'childAgeMonths': null,
      'feedingStatus': null,
      'vomiting': null,
      'diarrhea': null,
      'rash': null,
      'dehydrationSigns': <String>[],
    };
  }

  @override
  void dispose() {
    _initialController.dispose();
    for (final controller in _answerControllers.values) {
      controller.dispose();
    }
    super.dispose();
  }

  bool _guardConsentContext(String? expectedUserId) {
    if (!mounted) return false;
    if (AuthState.instance.userId == expectedUserId) return true;
    setState(() {
      _error = 'Phiên đăng nhập đã thay đổi. Vui lòng thử lại.';
    });
    return false;
  }

  Future<bool> _requestTriageConsent(String? expectedUserId) async {
    if (!_guardConsentContext(expectedUserId)) return false;
    final consent = await _service.getConsentStatus();
    if (!_guardConsentContext(expectedUserId)) return false;
    if (consent.isAccepted) return true;
    if (!mounted) return false;

    final accepted = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) => PopScope(
        canPop: false,
        child: AlertDialog(
          key: const Key('triage-consent-dialog'),
          title: const Text('Xác nhận sử dụng AI Triage'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  consent.disclaimerText,
                  key: const Key('triage-consent-text'),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              key: const Key('triage-consent-cancel'),
              onPressed: () => Navigator.of(dialogContext).pop(false),
              child: const Text('Để sau'),
            ),
            FilledButton(
              key: const Key('triage-consent-accept'),
              onPressed: () => Navigator.of(dialogContext).pop(true),
              child: const Text('Tôi hiểu và đồng ý'),
            ),
          ],
        ),
      ),
    );
    if (!_guardConsentContext(expectedUserId)) return false;
    if (accepted != true) {
      setState(() {
        _error = 'Bạn cần đồng ý với thông tin sử dụng AI Triage để tiếp tục.';
      });
      return false;
    }

    final acceptedConsent = await _service.acceptConsent(
      policyVersion: consent.currentVersion,
    );
    if (!_guardConsentContext(expectedUserId)) return false;
    if (!acceptedConsent.isAccepted ||
        acceptedConsent.currentVersion != consent.currentVersion) {
      throw const FormatException('Invalid triage consent acceptance');
    }
    return true;
  }

  Future<void> _start() async {
    if (_loading) return;
    if (!_stageConfirmed) {
      setState(
        () => _error =
            'Vui lòng chọn giai đoạn sức khỏe trước khi bắt đầu AI Triage.',
      );
      return;
    }
    final text = _initialController.text.trim();
    if (text.isEmpty) return;
    final validationError = _implausibleMeasurementError(text);
    if (validationError != null) {
      setState(() => _error = validationError);
      return;
    }
    final triageUserId = AuthState.instance.userId;
    setState(() {
      _loading = true;
      _error = null;
      _currentIntake = {
        ..._newIntake(stage: _selectedStage),
        'symptomList': <String>[text],
        'parentFreeText': text,
      };
    });
    await _startCanonical(text, triageUserId);
  }

  Future<void> _sendAnswers() async {
    if (_loading) return;
    if (_sessionId == null || _questions.isEmpty) return;
    final newAnswers = <String, dynamic>{};
    for (final question in _questions) {
      final value = _valueFor(question);
      if (value != null) {
        newAnswers[question.questionKey] = value;
      }
    }
    if (newAnswers.isEmpty) {
      setState(() => _error = 'Vui lòng trả lời ít nhất một câu hỏi.');
      return;
    }
    final validationError = _validateQuestionAnswers(newAnswers);
    if (validationError != null) {
      setState(() => _error = validationError);
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
    });
    if (_canonicalSession == null) {
      setState(() {
        _loading = false;
        _error = 'Phiên phân loại không còn hợp lệ. Vui lòng bắt đầu lại.';
      });
      return;
    }
    await _continueCanonical(newAnswers);
  }

  /// Starts a canonical deterministic conversation through the single chat UI.
  ///
  /// Consent is handled the same way V1 handles it. Without this the first turn for an account
  /// that has not accepted the AI Triage terms failed into the generic "cannot complete" message,
  /// which tells the user nothing and hides a problem they could have fixed in one tap.
  Future<void> _startCanonical(String text, String? triageUserId) async {
    try {
      await _sendCanonicalStart(text);
    } on TriageConsentRequiredFailure {
      bool accepted;
      try {
        accepted = await _requestTriageConsent(triageUserId);
      } catch (_) {
        if (mounted) {
          setState(
            () => _error =
                'Không thể xác nhận điều khoản AI Triage. Vui lòng thử lại.',
          );
        }
        return;
      }
      if (!accepted || !_guardConsentContext(triageUserId)) return;
      try {
        await _sendCanonicalStart(text);
      } catch (_) {
        if (mounted) setState(() => _error = _canonicalUnavailableText);
      }
    } catch (_) {
      if (mounted) setState(() => _error = _canonicalUnavailableText);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  static const _canonicalUnavailableText =
      'Hiện chưa thể hoàn tất định hướng nguy cơ. Kết quả lỗi không được xem là mức an toàn.';

  Future<void> _sendCanonicalStart(String text) async {
    final target = canonicalTriageTargetForStage(_selectedStage);
    final profileId =
        target == 'BABY' &&
            widget.entryContext.origin == TriageOriginIntent.babyProfile
        ? widget.entryContext.originReferenceId
        : null;
    final session = await _sessionService.start(
      message: text,
      selectedTarget: target,
      selectedStage: canonicalTriageStage(_selectedStage),
      profileId: profileId,
      lifecycleBinding: widget.entryContext.toLifecycleBindingJson(),
    );
    if (!mounted) return;
    _canonicalSession = session;
    _applyResponse(_sessionResponse(session), userMessage: text);
  }

  /// Sends a whole round of answers as identifiers and renders the next turn.
  ///
  /// The chat collects up to three answers before submitting, so they travel together: one user
  /// action stays one request and one state version.
  Future<void> _continueCanonical(Map<String, dynamic> newAnswers) async {
    final current = _canonicalSession!;
    final answers = <TriageAnswer>[];
    for (final entry in newAnswers.entries) {
      TriageQuestion? question;
      for (final candidate in current.questionDetails) {
        if (candidate.id == entry.key) {
          question = candidate;
          break;
        }
      }
      if (question?.answerType == 'NUMBER') {
        final numericValue = int.tryParse(entry.value?.toString() ?? '');
        if (numericValue != null) {
          answers.add(
            TriageAnswer(questionId: entry.key, numericValue: numericValue),
          );
        }
        continue;
      }
      final optionCode = entry.value?.toString() ?? '';
      // Only option-coded answers are structured; free text stays in the message and is
      // extracted server-side, never guessed at here.
      if (optionCode.isEmpty) continue;
      final known = current.questionDetails
          .where((question) => question.id == entry.key)
          .expand((question) => question.optionCodes)
          .toSet();
      if (!known.contains(optionCode)) continue;
      answers.add(TriageAnswer(questionId: entry.key, optionCode: optionCode));
    }
    try {
      final session = await _sessionService.continueSession(
        session: current,
        message: _answersText(newAnswers),
        answers: answers,
      );
      if (!mounted) return;
      _canonicalSession = session;
      _applyResponse(
        _sessionResponse(session),
        userMessage: _answersText(newAnswers),
      );
    } on TriageSessionStaleVersionFailure {
      // Another device advanced this session. Re-read rather than replay a stale version.
      try {
        final refreshed = await _sessionService.get(current.sessionId);
        if (!mounted) return;
        _canonicalSession = refreshed;
        _applyResponse(_sessionResponse(refreshed));
        setState(
          () => _error =
              'Phiên đã được cập nhật ở nơi khác. Vui lòng kiểm tra và gửi lại.',
        );
      } catch (_) {
        if (mounted) {
          setState(
            () => _error = 'Không thể gửi câu trả lời. Vui lòng thử lại.',
          );
        }
      }
    } catch (_) {
      if (mounted) {
        setState(() => _error = 'Không thể gửi câu trả lời. Vui lòng thử lại.');
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  IntakeFlowResponse _sessionResponse(TriageSession session) =>
      TriageChatAdapter.toFlowResponse(
        session,
        fallbackStage: _selectedStage,
        round: _round + 1,
        mergedIntake: _currentIntake,
      );

  Future<void> _returnToValidatedOrigin() async {
    if (_returningToOrigin) return;
    final userId = AuthState.instance.userId;
    if (userId == null || userId.isEmpty) {
      setState(() => _returnNotice = 'Vui lòng đăng nhập lại để tiếp tục.');
      return;
    }
    setState(() {
      _returningToOrigin = true;
      _returnNotice = null;
    });
    TriageContinuationDecision decision;
    try {
      decision = await _continuationCoordinator.restoreForUser(userId);
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _returningToOrigin = false;
        _returnNotice =
            'Chưa thể khôi phục điểm quay lại. Dữ liệu vẫn được giữ; hãy thử lại.';
      });
      return;
    }
    if (!mounted) return;
    setState(() => _returningToOrigin = false);
    final isFamily =
        (AuthState.instance.role ?? '').trim().toUpperCase() == 'FAMILY';
    final String? location = switch (decision.destination) {
      TriageContinuationDestination.motherJourney when !isFamily =>
        '/mother-home?tab=1&triageReturn=${Uri.encodeQueryComponent(_sessionId ?? DateTime.now().microsecondsSinceEpoch.toString())}',
      TriageContinuationDestination.babyProfile
          when !isFamily && decision.originReferenceId?.isNotEmpty == true =>
        '/babies/detail/${Uri.encodeComponent(decision.originReferenceId!)}',
      TriageContinuationDestination.safeDashboard =>
        isFamily ? '/' : '/mother-home',
      _ => null,
    };
    if (location == null) {
      setState(() {
        _returnNotice =
            'Chưa thể khôi phục điểm quay lại. Dữ liệu tiếp tục vẫn được giữ để thử lại.';
      });
      return;
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
  }

  void _applyResponse(IntakeFlowResponse response, {String? userMessage}) {
    final mergedStage = response.mergedIntake['stage']?.toString();
    if (mergedStage != null && mergedStage != response.stage) {
      setState(() {
        _error =
            'Phản hồi intake không khớp giai đoạn đã chọn. Vui lòng thử lại.';
        _questions = [];
      });
      return;
    }
    if (response.triageResult != null &&
        response.triageResult!.stage != response.stage) {
      setState(() {
        _error =
            'Phản hồi phân loại không khớp giai đoạn đã chọn. Vui lòng thử lại.';
        _questions = [];
      });
      return;
    }
    if (widget.entryContext.lockStage &&
        response.stage != widget.entryContext.stage.apiValue) {
      setState(() {
        _error = widget.entryContext.isPostpartum
            ? 'Phản hồi không khớp với giai đoạn sau sinh. Vui lòng thử lại.'
            : 'Phản hồi không khớp với giai đoạn đã chọn. Vui lòng thử lại.';
        _questions = [];
      });
      return;
    }
    if (widget.entryContext.isMaternal &&
        response.questions.any(
          (question) => const {
            'childAgeMonths',
            'feedingStatus',
            'diarrhea',
            'rash',
            'dehydrationSigns',
          }.contains(question.questionKey),
        )) {
      setState(() {
        _error =
            'Không thể dùng bộ câu hỏi dành cho bé ở giai đoạn sức khỏe của mẹ. Vui lòng thử lại.';
        _questions = [];
      });
      return;
    }
    setState(() {
      if (userMessage != null) {
        _messages.add(_ChatMessage(role: _ChatRole.user, text: userMessage));
      }
      _sessionId = response.intakeSessionId;
      _selectedStage = response.stage;
      _currentIntake = response.mergedIntake;
      _questions = response.questions;
      _round = response.round;
      _result = response.triageResult;
      _answers.clear();
      for (final controller in _answerControllers.values) {
        controller.dispose();
      }
      _answerControllers.clear();
      if ((response.assistantMessage ?? '').isNotEmpty) {
        _messages.add(
          _ChatMessage(
            role: _ChatRole.assistant,
            text: response.assistantMessage!,
          ),
        );
      }
      if (response.questions.isNotEmpty) {
        _messages.add(
          _ChatMessage(
            role: _ChatRole.assistant,
            text: response.questions.map((q) => q.text).join('\n'),
          ),
        );
      }
      if (response.status == 'TRIAGE_COMPLETE' &&
          response.triageResult != null) {
        _messages.add(
          _ChatMessage(
            role: _ChatRole.assistant,
            text:
                response.triageResult!.summary ??
                'Đã có kết quả phân loại rủi ro.',
          ),
        );
      }
    });
  }

  dynamic _valueFor(IntakeQuestion question) {
    switch (question.answerType) {
      case 'NUMBER':
      case 'TEXT':
        return _answerControllers[question.questionKey]?.text.trim();
      case 'BOOLEAN':
      case 'SINGLE_CHOICE':
        return _answers[question.questionKey];
      case 'MULTI_CHOICE':
        return (_answers[question.questionKey] as Set<String>?)?.toList();
      default:
        return null;
    }
  }

  static const _vietnameseOptionLabels = {
    'UNSURE': 'Tôi không chắc',
    'NONE': 'Không có',
    'DANGER_NONE': 'Không có dấu hiệu nào',
    'SEIZURE': 'Co giật',
    'ALTERED_CONSCIOUSNESS': 'Lơ mơ, khó đánh thức hoặc ngất',
    'SEVERE_BREATHING': 'Khó thở nghiêm trọng',
    'SEVERE_BREATHING_DIFFICULTY': 'Khó thở nghiêm trọng',
    'CYANOSIS': 'Tím tái môi hoặc đầu ngón',
    'HEADACHE_SEVERE': 'Đau đầu dữ dội',
    'HEADACHE_MILD_OR_NONE': 'Đau nhẹ hoặc không đau',
    'VISUAL_CHANGE_YES': 'Có nhìn mờ hoặc thấy chớp sáng',
    'VISUAL_CHANGE_NO': 'Không nhìn mờ',
    'VISUAL_CHANGE_UNSURE': 'Tôi không chắc',
    'BLEEDING_HEAVY': 'Ra máu nhiều (ướt đẫm băng)',
    'BLEEDING_SPOTTING': 'Ra máu ít hoặc đốm nhỏ',
    'BLEEDING_NONE': 'Không ra máu',
    'BLEEDING_UNSURE': 'Tôi không chắc',
    'CLOTS_LARGE': 'Có cục máu đông lớn',
    'CLOTS_NONE': 'Không có cục máu đông',
    'CLOTS_UNSURE': 'Tôi không chắc',
    'DIZZINESS_YES': 'Có hoa mắt, chóng mặt',
    'DIZZINESS_NO': 'Không chóng mặt',
    'BREATHING_SEVERE': 'Khó thở nghiêm trọng',
    'BREATHING_NORMAL': 'Thở bình thường',
    'FEEDING_NORMAL': 'Bú/uống như bình thường',
    'FEEDING_POOR': 'Bú ít hơn bình thường',
    'FEEDING_NONE': 'Bỏ bú hoàn toàn, không uống được',
    'VOMITING_ALL': 'Nôn tất cả mọi thứ',
    'VOMITING_SOME': 'Thỉnh thoảng nôn',
    'VOMITING_NONE': 'Không nôn',
    'DIARRHEA_SEVERE': 'Đi ngoài nhiều lần (>5 lần/ngày)',
    'DIARRHEA_MILD': 'Đi phân lỏng 1-2 lần',
    'DIARRHEA_NONE': 'Không tiêu chảy',
    'DEHYDRATION_YES': 'Có dấu hiệu mất nước',
    'DEHYDRATION_NO': 'Không có dấu hiệu mất nước',
    'FEVER_HIGH': 'Sốt cao (trên 38.5°C)',
    'FEVER_MILD': 'Sốt nhẹ (37.5°C - 38.5°C)',
    'FEVER_NONE': 'Không sốt',
    'PREGNANT_YES': 'Que thử 2 vạch / đã khám xác định có thai',
    'PREGNANT_NO': 'Que thử 1 vạch / không có thai',
    'PREGNANT_UNSURE': 'Tôi không chắc',
    'PAIN_SEVERE': 'Đau dữ dội',
    'PAIN_MODERATE': 'Đau vừa',
    'PAIN_MILD': 'Đau nhẹ lâm râm',
    'PAIN_NONE': 'Không đau',
  };

  String _questionLabel(String key) {
    final intakeQ = _questions
        .where((q) => q.questionKey == key)
        .firstOrNull;
    if (intakeQ != null && intakeQ.text.isNotEmpty) {
      return intakeQ.text;
    }
    final canonicalQ = _canonicalSession?.questionDetails
        .where((q) => q.id == key)
        .firstOrNull;
    if (canonicalQ != null && canonicalQ.text.isNotEmpty) {
      return canonicalQ.text;
    }
    const labels = {
      'childAgeMonths': 'Tuổi của bé (tháng)',
      'breathingStatus': 'Tình trạng hô hấp',
      'consciousnessStatus': 'Tình trạng tỉnh táo',
      'seizure': 'Co giật',
      'feedingStatus': 'Khả năng bú/uống',
      'temperatureC': 'Nhiệt độ',
      'dehydrationSigns': 'Dấu hiệu mất nước',
      'vomiting': 'Nôn',
      'diarrhea': 'Tiêu chảy',
      'duration': 'Thời gian triệu chứng',
      'rash': 'Phát ban',
      'painSeverity': 'Mức độ đau',
      'urinarySymptoms': 'Triệu chứng tiểu tiện',
      'parentFreeText': 'Mô tả bổ sung',
      'Q_GLOBAL_DANGER': 'Dấu hiệu nguy hiểm',
      'Q_GESTATIONAL_WEEK': 'Tuổi thai (tuần)',
      'Q_POSTPARTUM_DAY': 'Số ngày sau sinh',
      'Q_BABY_AGE_MONTHS': 'Tuổi của bé (tháng)',
      'Q_PREGNANCY_TEST': 'Tình trạng mang thai',
      'Q_PAIN_SEVERITY': 'Mức độ đau',
      'Q_BLEEDING_AMOUNT': 'Lượng máu ra',
      'Q_DIZZINESS': 'Chóng mặt',
      'Q_HEADACHE_SEVERITY': 'Mức độ đau đầu',
      'Q_VISUAL_CHANGE': 'Rối loạn thị giác',
      'Q_BREATHING_SEVERITY': 'Tình trạng hô hấp',
      'Q_FEVER_SEVERITY': 'Mức độ sốt',
      'Q_BABY_FEEDING': 'Khả năng bú/uống của bé',
      'Q_BABY_VOMITING': 'Tình trạng nôn của bé',
      'Q_BABY_DIARRHEA': 'Tình trạng tiêu chảy của bé',
      'Q_DEHYDRATION_SIGNS': 'Dấu hiệu mất nước',
      'Q_BABY_COUGH': 'Tình trạng ho của bé',
      'Q_BABY_RASH': 'Phát ban ở bé',
      'Q_LOCHIA_SMELL': 'Mùi sản dịch',
      'Q_LEG_PAIN_SWELLING': 'Sưng đau bắp chân',
      'Q_POSTPARTUM_BLEEDING': 'Lượng sản dịch/ra máu',
      'Q_POSTPARTUM_FEVER': 'Sốt sau sinh',
    };
    return labels[key] ?? key;
  }

  String _answersText(Map<String, dynamic> answers) {
    if (answers.length == 1) {
      final entry = answers.entries.first;
      final displayVal = _displayAnswerValue(entry.key, entry.value);
      if (entry.key == 'Q_GESTATIONAL_WEEK' ||
          entry.key == 'Q_POSTPARTUM_DAY' ||
          entry.key == 'Q_BABY_AGE_MONTHS' ||
          entry.key == 'childAgeMonths' ||
          entry.key == 'temperatureC') {
        return '${_questionLabel(entry.key)}: $displayVal';
      }
      return displayVal;
    }
    return answers.entries
        .map(
          (entry) =>
              '${_questionLabel(entry.key)}: ${_displayAnswerValue(entry.key, entry.value)}',
        )
        .join('\n');
  }

  String _displayAnswerValue(String questionId, dynamic value) {
    final intakeQ = _questions
        .where((candidate) => candidate.questionKey == questionId)
        .firstOrNull;
    if (intakeQ != null && intakeQ.options.isNotEmpty) {
      String displayIntake(Object? raw) {
        final code = raw?.toString() ?? '';
        return intakeQ.options
                .where((opt) => opt.code == code)
                .map((opt) => opt.label)
                .firstOrNull ??
            _vietnameseOptionLabels[code] ??
            code;
      }

      if (value is Iterable) {
        return value.map(displayIntake).join(', ');
      }
      return displayIntake(value);
    }

    final question = _canonicalSession?.questionDetails
        .where((candidate) => candidate.id == questionId)
        .firstOrNull;
    if (question != null && question.options.isNotEmpty) {
      String displayOne(Object? raw) {
        final code = raw?.toString() ?? '';
        return question.options
                .where((option) => option.optionCode == code)
                .map((option) => option.displayText)
                .firstOrNull ??
            _vietnameseOptionLabels[code] ??
            code;
      }

      if (value is Iterable) {
        return value.map(displayOne).join(', ');
      }
      return displayOne(value);
    }

    final code = value?.toString() ?? '';
    return _vietnameseOptionLabels[code] ?? code;
  }

  String? _validateQuestionAnswers(Map<String, dynamic> answers) {
    for (final entry in answers.entries) {
      if (entry.key == 'Q_BABY_AGE_MONTHS') {
        final age = int.tryParse(entry.value.toString());
        if (age == null || age < 0 || age >= 24) {
          return 'Tuổi của bé phải là số tháng từ 0 đến 23. Vui lòng kiểm tra và nhập lại.';
        }
      }
      if (entry.key == 'Q_GESTATIONAL_WEEK') {
        final week = int.tryParse(entry.value.toString());
        if (week == null || week < 1 || week > 45) {
          return 'Tuổi thai phải là số tuần từ 1 đến 45. Vui lòng kiểm tra và nhập lại.';
        }
      }
      if (entry.key == 'Q_POSTPARTUM_DAY') {
        final day = int.tryParse(entry.value.toString());
        if (day == null || day < 0) {
          return 'Số ngày sau sinh không được âm. Vui lòng kiểm tra và nhập lại.';
        }
      }
    }
    return _implausibleMeasurementError(_answersText(answers));
  }

  static String? _implausibleMeasurementError(String message) {
    final normalized = message.toLowerCase().replaceAll(',', '.');
    final temperature = RegExp(
      r'(?:sốt|sot|nhiệt độ|nhiet do|thân nhiệt|than nhiet|đo|do)\D{0,24}(\d{1,3}(?:\.\d{1,2})?)\s*(?:°\s*c?|độ\s*c?|do\s*c?|c\b)',
      caseSensitive: false,
    ).firstMatch(normalized);
    final value = double.tryParse(temperature?.group(1) ?? '');
    if (value != null && (value < 30 || value > 45)) {
      return 'Nhiệt độ cơ thể theo °C phải trong khoảng 30–45°C. Vui lòng kiểm tra số đo và đơn vị (°C/°F).';
    }
    return null;
  }

  Future<void> _openUrl(TriageCitation citation) async {
    final uri = Uri.tryParse(citation.url);
    if (uri == null || !_isSafeCitationUri(uri, citation.domain)) return;
    try {
      final opened = await launchUrl(uri, mode: LaunchMode.externalApplication);
      if (!opened && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Không thể mở nguồn tham khảo trên thiết bị này.'),
          ),
        );
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Không thể mở nguồn tham khảo trên thiết bị này.'),
          ),
        );
      }
    }
  }

  bool _isSafeCitationUri(Uri uri, String? approvedDomain) {
    final domain = (approvedDomain ?? '').toLowerCase().replaceFirst(
      RegExp(r'^www\\.'),
      '',
    );
    final host = uri.host.toLowerCase().replaceFirst(RegExp(r'^www\\.'), '');
    final path = uri.path.replaceAll('/', '').trim().toLowerCase();
    final genericSearchHost = const {
      'google.com',
      'bing.com',
      'yahoo.com',
    }.contains(host);
    final genericSearchPath = RegExp(
      r'(^|/)(search|query|find)(/|$)',
    ).hasMatch(uri.path.toLowerCase());
    return uri.scheme == 'https' &&
        uri.host.isNotEmpty &&
        (uri.port == -1 || uri.port == 443) &&
        uri.userInfo.isEmpty &&
        host != 'localhost' &&
        host != '127.0.0.1' &&
        !genericSearchHost &&
        !genericSearchPath &&
        domain.isNotEmpty &&
        path.isNotEmpty &&
        path != 'vi' &&
        path != 'en' &&
        (host == domain || host.endsWith('.$domain'));
  }

  Future<void> _openEmergencyFlow() async {
    if (_openingEmergency) return;
    final requestUserId = AuthState.instance.userId;
    setState(() {
      _openingEmergency = true;
      _emergencyFailed = false;
      _error = null;
    });
    try {
      final session = await _emergencyService.getActive().timeout(
        const Duration(seconds: 8),
      );
      if (!mounted) return;
      if (AuthState.instance.userId != requestUserId) {
        setState(() {
          _emergencyFailed = true;
          _error =
              'Phiên đăng nhập đã thay đổi. Hãy tải lại phiên hỗ trợ cho tài khoản hiện tại.';
        });
        return;
      }
      if (session == null) {
        throw StateError('Missing backend-created emergency session');
      }
      final location = Uri(
        path: '/emergency/map',
        queryParameters: {
          'mode': 'triage',
          'stage': _result?.stage ?? _selectedStage,
        },
      ).toString();
      context.push(location, extra: session);
    } catch (_) {
      if (mounted) {
        setState(() {
          _emergencyFailed = true;
          _error =
              'Không thể tải phiên hỗ trợ lúc này. Phiên có thể vẫn đang được hệ thống xử lý.';
        });
      }
    } finally {
      if (mounted) setState(() => _openingEmergency = false);
    }
  }

  Future<void> _openPostpartumEmergency() async {
    if (_openingEmergency) return;
    final requestUserId = AuthState.instance.userId;
    setState(() {
      _openingEmergency = true;
      _emergencyFailed = false;
      _error = null;
    });
    try {
      final session = await _emergencyService.getActive().timeout(
        const Duration(seconds: 8),
      );
      if (!mounted) return;
      if (AuthState.instance.userId != requestUserId) {
        setState(() {
          _emergencyFailed = true;
          _error =
              'Phiên đăng nhập đã thay đổi. Hãy tải lại phiên hỗ trợ cho tài khoản hiện tại.';
        });
        return;
      }
      if (session == null) {
        throw StateError('Missing backend-created emergency session');
      }
      final location = Uri(
        path: '/emergency/map',
        queryParameters: {
          'mode': 'triage',
          'stage': _result?.stage ?? 'POSTPARTUM',
        },
      ).toString();
      context.push(location, extra: session);
    } catch (_) {
      if (mounted) {
        setState(() {
          _emergencyFailed = true;
          _error =
              'Không thể tải phiên hỗ trợ lúc này. Phiên có thể vẫn đang được hệ thống xử lý.';
        });
      }
    } finally {
      if (mounted) setState(() => _openingEmergency = false);
    }
  }

  Future<void> _call115() async {
    if (_dialing115 || !mounted) return;
    setState(() {
      _dialing115 = true;
      _postpartumManualCallRequired = false;
      _dialerNotice = null;
    });
    var opened = false;
    try {
      opened =
          await (widget.postpartumEmergencyLauncher?.call() ??
              launchUrl(
                Uri.parse('tel:115'),
                mode: LaunchMode.externalApplication,
              ));
    } catch (_) {
      opened = false;
    }
    if (!mounted) return;
    setState(() {
      _dialing115 = false;
      _postpartumManualCallRequired = !opened;
      _dialerNotice = opened ? 'Đang mở cuộc gọi cấp cứu 115.' : null;
    });
  }

  String _stageLabel(String stage) => switch (stage) {
    'PRECONCEPTION' => 'Chuẩn bị mang thai',
    'PREGNANCY' => 'Đang mang thai',
    'POSTPARTUM' => 'Sau sinh',
    'INFANT' => 'Bé 0–12 tháng',
    'TODDLER' => 'Bé 12–24 tháng',
    _ => 'Sức khỏe gia đình',
  };

  @override
  Widget build(BuildContext context) {
    final showWelcome =
        _sessionId == null &&
        _messages.length == 1 &&
        _questions.isEmpty &&
        _result == null &&
        _error == null;
    return Scaffold(
      backgroundColor: _surface,
      appBar: AppBar(
        backgroundColor: _primaryDark,
        elevation: 0,
        foregroundColor: Colors.white,
        centerTitle: true,
        title: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text(
              'CareBridge AI',
              style: TextStyle(fontSize: 17, fontWeight: FontWeight.w700),
            ),
            Text(
              'Trợ lý phân loại • ${_stageLabel(_selectedStage)}',
              style: const TextStyle(fontSize: 11, color: Colors.white70),
            ),
          ],
        ),
        actions: [
          IconButton(
            key: const Key('triage-history'),
            tooltip: 'Lịch sử AI Triage',
            onPressed: () => context.push('/triage/history'),
            icon: const Icon(Icons.history_rounded),
          ),
          IconButton(
            key: const Key('triage-ai-info'),
            tooltip: 'Thông tin an toàn AI Triage',
            onPressed: _showAiInfo,
            icon: const Icon(Icons.info_outline_rounded),
          ),
        ],
      ),
      body: SafeArea(
        child: Column(
          children: [
            const _MandatoryAiDisclaimer(),
            Expanded(
              child: showWelcome
                  ? _buildWelcome()
                  : SingleChildScrollView(
                      padding: const EdgeInsets.fromLTRB(16, 18, 16, 24),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          if (_sessionId == null &&
                              !widget.entryContext.lockStage)
                            _buildStageSelector(),
                          ..._messages.map(_buildBubble),
                          if (_result != null) _buildResult(_result!),
                          if (_questions.isNotEmpty && _result == null)
                            _buildQuestions(),
                          if (_error != null) _buildError(),
                        ],
                      ),
                    ),
            ),
            _buildComposer(),
          ],
        ),
      ),
    );
  }

  Future<void> _showAiInfo() async {
    await showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        backgroundColor: Colors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(28)),
        icon: const Icon(
          Icons.health_and_safety_outlined,
          color: _primary,
          size: 36,
        ),
        title: const Text(
          'Về CareBridge AI',
          textAlign: TextAlign.center,
          style: TextStyle(color: _onSurface, fontWeight: FontWeight.w700),
        ),
        content: const Text(
          'CareBridge AI hỗ trợ phân loại rủi ro ban đầu, không chẩn đoán, '
          'không kê đơn và không thay thế nhân viên y tế. Trong tình huống '
          'khẩn cấp, hãy liên hệ cấp cứu ngay.',
          textAlign: TextAlign.center,
          style: TextStyle(color: _onVariant, height: 1.5),
        ),
        actionsAlignment: MainAxisAlignment.center,
        actions: [
          FilledButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            style: FilledButton.styleFrom(
              backgroundColor: _primaryDark,
              shape: const StadiumBorder(),
              minimumSize: const Size(120, 48),
            ),
            child: const Text('Đã hiểu'),
          ),
        ],
      ),
    );
  }

  Widget _buildWelcome() {
    return LayoutBuilder(
      builder: (context, constraints) => SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(24, 24, 24, 20),
        child: ConstrainedBox(
          constraints: BoxConstraints(minHeight: constraints.maxHeight - 44),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Container(
                width: 82,
                height: 82,
                decoration: BoxDecoration(
                  color: _primary.withValues(alpha: 0.13),
                  shape: BoxShape.circle,
                  boxShadow: const [
                    BoxShadow(
                      color: Color(0x165A463F),
                      blurRadius: 22,
                      offset: Offset(0, 8),
                    ),
                  ],
                ),
                child: const Icon(
                  Icons.smart_toy_rounded,
                  size: 42,
                  color: _primary,
                ),
              ),
              const SizedBox(height: 22),
              const Text(
                'Trợ lý AI CareBridge',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 21,
                  fontWeight: FontWeight.w700,
                  color: _primaryDark,
                ),
              ),
              if (widget.entryContext.lockStage) ...[
                const SizedBox(height: 8),
                const Text(
                  'Kiểm tra dấu hiệu an toàn',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    color: _primary,
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
              const SizedBox(height: 10),
              Text(
                widget.entryContext.isPostpartum
                    ? 'Hãy mô tả dấu hiệu sau sinh để CareBridge hỗ trợ phân loại mức độ cần chú ý.'
                    : widget.entryContext.isMaternal
                    ? 'Hãy mô tả dấu hiệu bạn đang gặp để CareBridge hỗ trợ phân loại mức độ cần chú ý.'
                    : 'Hỏi về sức khỏe mẹ bầu, sau sinh hoặc mô tả triệu chứng và sự phát triển của bé.',
                textAlign: TextAlign.center,
                style: const TextStyle(
                  color: _onVariant,
                  fontSize: 14,
                  height: 1.5,
                ),
              ),
              const SizedBox(height: 8),
              const Text(
                'Phản hồi AI chỉ mang tính tham khảo, không thay thế tư vấn y tế.',
                textAlign: TextAlign.center,
                style: TextStyle(color: Color(0xFF9C857C), fontSize: 12),
              ),
              if (!widget.entryContext.lockStage) ...[
                const SizedBox(height: 28),
                _buildStageSelector(),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildBubble(_ChatMessage message) {
    final isUser = message.role == _ChatRole.user;
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Row(
        mainAxisAlignment: isUser
            ? MainAxisAlignment.end
            : MainAxisAlignment.start,
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          if (!isUser) ...[
            Container(
              width: 34,
              height: 34,
              decoration: BoxDecoration(
                color: _primary.withValues(alpha: 0.14),
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.smart_toy_rounded,
                size: 19,
                color: _primaryDark,
              ),
            ),
            const SizedBox(width: 8),
          ],
          Flexible(
            child: Container(
              constraints: const BoxConstraints(maxWidth: 320),
              padding: const EdgeInsets.symmetric(horizontal: 15, vertical: 12),
              decoration: BoxDecoration(
                color: isUser ? _primaryDark : Colors.white,
                borderRadius: BorderRadius.only(
                  topLeft: const Radius.circular(20),
                  topRight: const Radius.circular(20),
                  bottomLeft: Radius.circular(isUser ? 20 : 6),
                  bottomRight: Radius.circular(isUser ? 6 : 20),
                ),
                boxShadow: const [
                  BoxShadow(
                    color: Color(0x145A463F),
                    blurRadius: 16,
                    offset: Offset(0, 5),
                  ),
                ],
              ),
              child: Text(
                message.text,
                style: TextStyle(
                  color: isUser ? Colors.white : _onSurface,
                  height: 1.45,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildQuestions() {
    return Container(
      margin: const EdgeInsets.only(top: 4),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: const [
          BoxShadow(
            color: Color(0x145A463F),
            blurRadius: 20,
            offset: Offset(0, 6),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'CareBridge cần hỏi thêm',
            style: TextStyle(
              fontWeight: FontWeight.w700,
              color: _primaryDark,
              fontSize: 16,
            ),
          ),
          const SizedBox(height: 12),
          ..._questions.map(_buildQuestionInput),
        ],
      ),
    );
  }

  Widget _buildStageSelector() {
    const stages = {
      'PRECONCEPTION': 'Chuẩn bị mang thai',
      'PREGNANCY': 'Đang mang thai',
      'POSTPARTUM': 'Sau sinh',
      'INFANT': 'Bé 0-12 tháng',
      'TODDLER': 'Bé 12-24 tháng',
    };
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: _surfaceLow,
        borderRadius: BorderRadius.circular(22),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Padding(
            padding: EdgeInsets.only(left: 4, bottom: 10),
            child: Text(
              'Bạn đang hỏi cho giai đoạn nào?',
              style: TextStyle(color: _onSurface, fontWeight: FontWeight.w700),
            ),
          ),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: stages.entries.map((entry) {
              // The tick must mean "you chose this", not "this is our default".
              // _selectedStage always holds a real stage so the rest of the screen has
              // something to render, but until the user actually picks one the send is
              // refused — showing a chip as selected in that window told the user the
              // opposite of what the guard in _start() would do.
              final selected = _stageConfirmed && _selectedStage == entry.key;
              return ChoiceChip(
                label: Text(entry.value),
                selected: selected,
                selectedColor: _primaryDark,
                backgroundColor: Colors.white,
                side: BorderSide(color: selected ? _primary : _outline),
                labelStyle: TextStyle(
                  color: selected ? Colors.white : _onSurface,
                  fontWeight: FontWeight.w600,
                ),
                shape: const StadiumBorder(),
                onSelected: _loading
                    ? null
                    : (_) {
                        setState(() {
                          _selectedStage = entry.key;
                          _stageConfirmed = true;
                          _currentIntake = _newIntake(stage: entry.key);
                        });
                      },
              );
            }).toList(),
          ),
        ],
      ),
    );
  }

  Widget _buildQuestionInput(IntakeQuestion question) {
    switch (question.answerType) {
      case 'NUMBER':
      case 'TEXT':
        final controller = _answerControllers.putIfAbsent(
          question.questionKey,
          () => TextEditingController(),
        );
        return Padding(
          padding: const EdgeInsets.only(bottom: 14),
          child: TextField(
            controller: controller,
            keyboardType: question.answerType == 'NUMBER'
                ? const TextInputType.numberWithOptions(decimal: true)
                : TextInputType.multiline,
            maxLines: question.answerType == 'TEXT' ? 3 : 1,
            decoration: InputDecoration(
              labelText: question.text,
              filled: true,
              fillColor: _surface,
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(16),
                borderSide: BorderSide.none,
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(16),
                borderSide: const BorderSide(color: _primary, width: 1.5),
              ),
            ),
          ),
        );
      case 'MULTI_CHOICE':
        final selected =
            (_answers[question.questionKey] as Set<String>?) ?? <String>{};
        return Padding(
          padding: const EdgeInsets.only(bottom: 14),
          child: _ChoiceGroup(
            question: question,
            selected: selected,
            multi: true,
            onSelected: (value) {
              setState(() {
                final next = {...selected};
                next.contains(value) ? next.remove(value) : next.add(value);
                _answers[question.questionKey] = next;
              });
            },
          ),
        );
      case 'BOOLEAN':
      case 'SINGLE_CHOICE':
      default:
        return Padding(
          padding: const EdgeInsets.only(bottom: 14),
          child: _ChoiceGroup(
            question: question,
            selected: {_answers[question.questionKey]?.toString() ?? ''},
            multi: false,
            onSelected: (value) =>
                setState(() => _answers[question.questionKey] = value),
          ),
        );
    }
  }

  bool _canOpenYellowHandoff(TriageResult result) =>
      result.status == 'COMPLETED' &&
      result.riskLevel == 'YELLOW' &&
      const {
        'MOTHER_JOURNEY',
        'BABY_PROFILE',
      }.contains(result.originDashboard) &&
      (result.originDashboard == 'BABY_PROFILE' ||
          (result.journeyId ?? '').isNotEmpty) &&
      (result.originReferenceId ?? '').isNotEmpty;

  void _openYellowHandoff(TriageResult result) {
    if (!_canOpenYellowHandoff(result)) return;
    context.push('/triage/expert-handoff', extra: result.sessionId);
  }

  Widget _buildResult(TriageResult result) {
    final color = _riskColor(result.riskLevel);
    return Container(
      margin: const EdgeInsets.only(top: 8),
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: color.withValues(alpha: 0.45), width: 2),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Mức rủi ro: ${TriageChatAdapter.riskLabel(result.riskLevel)}',
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w800,
              color: color,
            ),
          ),
          if ((result.summary ?? '').isNotEmpty) ...[
            const SizedBox(height: 8),
            Text(
              result.summary!,
              style: const TextStyle(color: _onSurface, height: 1.35),
            ),
          ],
          if ((result.recommendedAction ?? '').isNotEmpty) ...[
            const SizedBox(height: 12),
            Text(
              result.recommendedAction!,
              style: const TextStyle(color: _onVariant, height: 1.35),
            ),
          ],
          if (result.redFlags.isNotEmpty) ...[
            const SizedBox(height: 12),
            Text('Dấu hiệu cảnh báo: ${result.redFlags.join(', ')}'),
          ],
          if (result.riskLevel == 'RED' || result.emergencyActionRequired) ...[
            const SizedBox(height: 16),
            if (result.stage == 'POSTPARTUM') ...[
              SizedBox(
                width: double.infinity,
                height: 52,
                child: ElevatedButton.icon(
                  key: const Key('triage-postpartum-call-115'),
                  onPressed: _dialing115 ? null : _call115,
                  icon: _dialing115
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: Colors.white,
                          ),
                        )
                      : const Icon(Icons.call),
                  label: const Text(
                    'Gọi cấp cứu 115 ngay',
                    style: TextStyle(fontWeight: FontWeight.w800),
                  ),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFFBA1A1A),
                    foregroundColor: Colors.white,
                    shape: const StadiumBorder(),
                    elevation: 3,
                  ),
                ),
              ),
              const SizedBox(height: 10),
            ],
            SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                key: const Key('triage-emergency-cta'),
                onPressed: result.stage == 'POSTPARTUM'
                    ? (_openingEmergency ? null : _openPostpartumEmergency)
                    : (_openingEmergency ? null : _openEmergencyFlow),
                icon: _openingEmergency
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.emergency),
                label: Text(
                  _emergencyFailed
                      ? 'Thử tải lại phiên hỗ trợ'
                      : 'Mở phiên hỗ trợ khẩn cấp',
                ),
                style: ElevatedButton.styleFrom(
                  minimumSize: const Size.fromHeight(52),
                  backgroundColor: const Color(0xFFBA1A1A),
                  foregroundColor: Colors.white,
                  shape: const StadiumBorder(),
                ),
              ),
            ),
            if (result.stage == 'POSTPARTUM' &&
                (_dialerNotice ?? '').isNotEmpty) ...[
              const SizedBox(height: 12),
              Semantics(
                liveRegion: true,
                child: Text(
                  _dialerNotice!,
                  style: const TextStyle(
                    color: Color(0xFF5A463F),
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
            ],
            if (result.stage == 'POSTPARTUM' &&
                _postpartumManualCallRequired) ...[
              const SizedBox(height: 12),
              Semantics(
                liveRegion: true,
                child: Container(
                  key: const Key('triage-postpartum-manual-call-guidance'),
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: const Color(0xFFFFF0ED),
                    borderRadius: BorderRadius.circular(16),
                    border: const Border(
                      left: BorderSide(color: Color(0xFFBA1A1A), width: 4),
                    ),
                  ),
                  child: const Text(
                    'Không thể mở ứng dụng gọi điện. Hãy tự gọi 115 ngay hoặc nhờ người bên cạnh gọi giúp.',
                    style: TextStyle(
                      color: Color(0xFF5A463F),
                      fontWeight: FontWeight.w700,
                      height: 1.4,
                    ),
                  ),
                ),
              ),
            ],
          ],
          if (result.riskLevel == 'YELLOW') ...[
            const SizedBox(height: 16),
            if (_canOpenYellowHandoff(result))
              SizedBox(
                width: double.infinity,
                height: 52,
                child: FilledButton.icon(
                  key: const Key('triage-inline-yellow-expert-handoff-cta'),
                  onPressed: () => _openYellowHandoff(result),
                  style: FilledButton.styleFrom(
                    backgroundColor: _primaryDark,
                    foregroundColor: Colors.white,
                    shape: const StadiumBorder(),
                  ),
                  icon: const Icon(Icons.support_agent),
                  label: const Text(
                    'Tìm chuyên gia đã xác thực',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.w800),
                  ),
                ),
              )
            else
              Semantics(
                liveRegion: true,
                child: Container(
                  key: const Key('triage-inline-yellow-handoff-unavailable'),
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: const Color(0xFFF2EAE4),
                    borderRadius: BorderRadius.circular(24),
                    border: const Border(
                      left: BorderSide(color: Color(0xFFC98C7B), width: 4),
                    ),
                  ),
                  child: const Text(
                    'Chưa thể chuyển ngữ cảnh cho chuyên gia vì kết quả chưa có đầy đủ liên kết hành trình. Hướng dẫn YELLOW vẫn được giữ an toàn; hãy thử tải lại từ nơi bắt đầu.',
                    style: TextStyle(
                      color: Color(0xFF5A463F),
                      fontSize: 16,
                      fontWeight: FontWeight.w700,
                      height: 1.4,
                    ),
                  ),
                ),
              ),
          ],
          if (result.riskLevel != 'RED' &&
              widget.entryContext.toLifecycleBindingJson().isNotEmpty) ...[
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: FilledButton.icon(
                key: const Key('triage-inline-return-to-origin'),
                onPressed: _returningToOrigin ? null : _returnToValidatedOrigin,
                icon: _returningToOrigin
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Colors.white,
                        ),
                      )
                    : const Icon(Icons.arrow_back_rounded),
                label: Text(
                  widget.entryContext.origin == TriageOriginIntent.babyProfile
                      ? 'Quay lại hồ sơ bé'
                      : 'Quay lại Hành trình của mẹ',
                ),
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(52),
                  backgroundColor: _primaryDark,
                  foregroundColor: Colors.white,
                  shape: const StadiumBorder(),
                ),
              ),
            ),
            if ((_returnNotice ?? '').isNotEmpty) ...[
              const SizedBox(height: 12),
              Semantics(
                liveRegion: true,
                child: Text(
                  _returnNotice!,
                  style: const TextStyle(
                    color: Color(0xFF5A463F),
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ],
          ],
          if ((result.warning ?? '').isNotEmpty) ...[
            const SizedBox(height: 12),
            Text(result.warning!, style: const TextStyle(color: Colors.orange)),
          ],
          if ((result.ragAnswer ?? '').isNotEmpty) ...[
            const SizedBox(height: 16),
            Container(
              key: const Key('triage-rag-guidance'),
              width: double.infinity,
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: _surfaceLow,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: _primary.withValues(alpha: 0.25)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Hướng dẫn tham khảo phù hợp với triệu chứng',
                    style: TextStyle(fontWeight: FontWeight.w700),
                  ),
                  const SizedBox(height: 8),
                  Text(result.ragAnswer!),
                  if ((result.ragDisclaimer ?? '').isNotEmpty) ...[
                    const SizedBox(height: 8),
                    Text(
                      result.ragDisclaimer!,
                      style: const TextStyle(fontSize: 12, color: _onVariant),
                    ),
                  ],
                  if (result.ragFallback == true) ...[
                    const SizedBox(height: 8),
                    const Text(
                      'Nguồn tham khảo hiện chưa sẵn sàng; kết quả phân loại vẫn được giữ nguyên.',
                      style: TextStyle(fontSize: 12, color: Colors.orange),
                    ),
                  ],
                ],
              ),
            ),
          ],
          if (result.citations.isNotEmpty) ...[
            const SizedBox(height: 16),
            const Text(
              'Nguồn tham khảo chính thống',
              style: TextStyle(fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 6,
              children: result.citations.indexed.map((entry) {
                final citation = entry.$2;
                return ActionChip(
                  key: Key('triage-source-chip-${citation.id ?? entry.$1}'),
                  label: Text(citation.organization ?? citation.source),
                  onPressed: () => _showCitationSheet(citation),
                );
              }).toList(),
            ),
            const SizedBox(height: 8),
            ...result.citations.indexed.map(
              (entry) => _buildCitation(entry.$2, entry.$1),
            ),
          ],
          if ((result.disclaimer ?? '').isNotEmpty) ...[
            const SizedBox(height: 16),
            Text(
              result.disclaimer!,
              style: const TextStyle(fontSize: 12, color: _onVariant),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildCitation(TriageCitation citation, int index) {
    final uri = Uri.tryParse(citation.url);
    final canOpen = uri != null && _isSafeCitationUri(uri, citation.domain);
    return InkWell(
      key: Key('triage-citation-${citation.id ?? citation.url}-$index'),
      onTap: canOpen ? () => _showCitationSheet(citation) : null,
      child: Container(
        margin: const EdgeInsets.only(bottom: 8),
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: _surfaceLow,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '${citation.organization ?? citation.source} - ${citation.title}',
              style: const TextStyle(fontWeight: FontWeight.w700),
            ),
            if (citation.url.isNotEmpty)
              Text(
                citation.url,
                style: const TextStyle(
                  color: _primary,
                  decoration: TextDecoration.underline,
                ),
              ),
            if (citation.matchedSymptoms.isNotEmpty)
              Text(
                'Triệu chứng phù hợp: ${citation.matchedSymptoms.join(', ')}',
              ),
            if (citation.sourceStatus == 'PENDING_REVIEW')
              const Text(
                'Nguồn chính thống được truy xuất tự động, đang chờ kiểm duyệt nội bộ.',
                style: TextStyle(fontSize: 12, color: _onVariant),
              ),
          ],
        ),
      ),
    );
  }

  void _showCitationSheet(TriageCitation citation) {
    showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      builder: (sheetContext) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                citation.organization ?? citation.source,
                style: const TextStyle(fontWeight: FontWeight.w700),
              ),
              const SizedBox(height: 8),
              Text(citation.title),
              if (citation.excerpt.isNotEmpty) ...[
                const SizedBox(height: 12),
                Text(citation.excerpt),
              ],
              if (citation.matchedRules.isNotEmpty) ...[
                const SizedBox(height: 8),
                Text('Quy tắc liên quan: ${citation.matchedRules.join(', ')}'),
              ],
              if (citation.sourceStatus == 'PENDING_REVIEW') ...[
                const SizedBox(height: 8),
                const Text('Nguồn đang chờ kiểm duyệt nội bộ.'),
              ],
              const SizedBox(height: 16),
              FilledButton.icon(
                onPressed: () => _openUrl(citation),
                icon: const Icon(Icons.open_in_new),
                label: const Text('Xem nguồn gốc'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildComposer() {
    if (_result != null) {
      return const SizedBox(height: 12);
    }
    // Same rule _start() enforces, moved to where the user can act on it. An unscoped entry
    // has no trusted stage, and INFANT is only a display default — sending it would triage a
    // mother's own symptom against paediatric rules. Refusing after the tap made that read as
    // a broken screen: the user had already typed a whole description before being told the
    // step they had missed. Refusing before the tap states the missing step instead.
    final stageMissing = !_stageConfirmed && !widget.entryContext.lockStage;
    return Container(
      padding: const EdgeInsets.fromLTRB(14, 10, 14, 14),
      decoration: const BoxDecoration(
        color: Colors.white,
        boxShadow: [
          BoxShadow(
            color: Color(0x175A463F),
            blurRadius: 18,
            offset: Offset(0, -4),
          ),
        ],
      ),
      child: _questions.isEmpty
          ? Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                if (stageMissing) _buildStageRequiredHint(),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        key: const Key('triage-chat-input'),
                        controller: _initialController,
                        enabled: !_loading,
                        minLines: 1,
                        maxLines: 4,
                        decoration: InputDecoration(
                          hintText: widget.entryContext.isMaternal
                              ? 'Mô tả dấu hiệu của mẹ...'
                              : 'Đặt câu hỏi sức khỏe...',
                          hintStyle: const TextStyle(color: Color(0xFF9C857C)),
                          filled: true,
                          fillColor: _surface,
                          contentPadding: const EdgeInsets.symmetric(
                            horizontal: 18,
                            vertical: 13,
                          ),
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(28),
                            borderSide: BorderSide.none,
                          ),
                          focusedBorder: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(28),
                            borderSide: const BorderSide(
                              color: _primary,
                              width: 1.5,
                            ),
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    SizedBox(
                      width: 50,
                      height: 50,
                      child: IconButton.filled(
                        key: const Key('triage-chat-send'),
                        style: IconButton.styleFrom(
                          backgroundColor: _primaryDark,
                          foregroundColor: Colors.white,
                          disabledBackgroundColor: _primaryDark.withValues(
                            alpha: 0.45,
                          ),
                        ),
                        onPressed: (_loading || stageMissing) ? null : _start,
                        icon: _loading
                            ? const SizedBox(
                                width: 20,
                                height: 20,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2,
                                  color: Colors.white,
                                ),
                              )
                            : const Icon(Icons.arrow_upward_rounded, size: 26),
                      ),
                    ),
                  ],
                ),
              ],
            )
          : SizedBox(
              height: 52,
              width: double.infinity,
              child: ElevatedButton.icon(
                onPressed: _loading ? null : _sendAnswers,
                style: ElevatedButton.styleFrom(
                  backgroundColor: _primaryDark,
                  foregroundColor: Colors.white,
                  shape: const StadiumBorder(),
                  elevation: 0,
                ),
                icon: _loading
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Colors.white,
                        ),
                      )
                    : const Icon(Icons.arrow_upward_rounded),
                label: const Text(
                  'Gửi câu trả lời',
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
              ),
            ),
    );
  }

  /// States the one missing step while the send is refused, next to the control it blocks.
  Widget _buildStageRequiredHint() {
    return Padding(
      key: const Key('triage-stage-required-hint'),
      padding: const EdgeInsets.only(left: 6, right: 6, bottom: 10),
      child: Row(
        children: [
          const Icon(Icons.arrow_upward_rounded, size: 17, color: _primaryDark),
          const SizedBox(width: 8),
          const Expanded(
            child: Text(
              'Chọn giai đoạn ở trên để bắt đầu.',
              style: TextStyle(
                color: _onVariant,
                fontSize: 13,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildError() {
    return Container(
      margin: const EdgeInsets.only(top: 12),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFFFFDAD6),
        borderRadius: BorderRadius.circular(18),
        border: const Border(
          left: BorderSide(color: Color(0xFFBA1A1A), width: 4),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Icon(Icons.error_outline_rounded, color: Color(0xFF93000A)),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  _error!,
                  style: const TextStyle(color: Color(0xFF93000A), height: 1.4),
                ),
              ),
            ],
          ),
          if (_emergencyFailed) ...[
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                SizedBox(
                  height: 48,
                  child: OutlinedButton.icon(
                    key: const Key('triage-emergency-retry'),
                    onPressed: _openingEmergency
                        ? null
                        : _result?.stage == 'POSTPARTUM'
                        ? _openPostpartumEmergency
                        : _openEmergencyFlow,
                    style: OutlinedButton.styleFrom(
                      shape: const StadiumBorder(),
                    ),
                    icon: const Icon(Icons.refresh),
                    label: const Text('Thử tải lại phiên hỗ trợ'),
                  ),
                ),
                SizedBox(
                  height: 48,
                  child: TextButton.icon(
                    key: const Key('triage-emergency-fallback-map'),
                    onPressed: () {
                      final location = Uri(
                        path: '/emergency/map',
                        queryParameters: {
                          'mode': 'triage',
                          'stage': _result?.stage ?? _selectedStage,
                        },
                      ).toString();
                      context.push(location);
                    },
                    style: TextButton.styleFrom(shape: const StadiumBorder()),
                    icon: const Icon(Icons.support_agent_outlined),
                    label: const Text('Mở trang hỗ trợ'),
                  ),
                ),
                if (_result?.stage == 'POSTPARTUM')
                  SizedBox(
                    height: 48,
                    child: TextButton.icon(
                      key: const Key('triage-emergency-call-115-fallback'),
                      onPressed: _dialing115 ? null : _call115,
                      style: TextButton.styleFrom(
                        foregroundColor: const Color(0xFFBA1A1A),
                        shape: const StadiumBorder(),
                      ),
                      icon: const Icon(Icons.call),
                      label: const Text('Gọi 115'),
                    ),
                  ),
              ],
            ),
          ],
        ],
      ),
    );
  }

  Color _riskColor(String? riskLevel) {
    switch (riskLevel) {
      case 'GREEN':
        return const Color(0xFF22C55E);
      case 'YELLOW':
        return const Color(0xFFFACC15);
      case 'RED':
        return const Color(0xFFEF4444);
      default:
        return const Color(0xFF9CA3AF);
    }
  }
}

class _ChoiceGroup extends StatelessWidget {
  final IntakeQuestion question;
  final Set<String> selected;
  final bool multi;
  final ValueChanged<String> onSelected;

  const _ChoiceGroup({
    required this.question,
    required this.selected,
    required this.multi,
    required this.onSelected,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          question.text,
          style: const TextStyle(fontWeight: FontWeight.w600),
        ),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: question.options.map((option) {
            final isSelected = selected.contains(option.code);
            return ChoiceChip(
              label: Text(option.label),
              selected: isSelected,
              onSelected: (_) => onSelected(option.code),
            );
          }).toList(),
        ),
      ],
    );
  }
}

class _MandatoryAiDisclaimer extends StatelessWidget {
  const _MandatoryAiDisclaimer();

  static const text =
      'Thông tin từ AI chỉ mang tính chất tham khảo, bạn cần tham vấn trực tiếp Bác sĩ/Chuyên gia Y tế khi có triệu chứng bất thường.';

  @override
  Widget build(BuildContext context) => Semantics(
    label: text,
    child: Container(
      key: const Key('triage-mandatory-disclaimer'),
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 9),
      color: const Color(0xFFFFF3E9),
      child: const Text(
        text,
        textAlign: TextAlign.center,
        style: TextStyle(fontSize: 12, color: Color(0xFF6F5147), height: 1.3),
      ),
    ),
  );
}

enum _ChatRole { user, assistant }

class _ChatMessage {
  final _ChatRole role;
  final String text;

  const _ChatMessage({required this.role, required this.text});
}
