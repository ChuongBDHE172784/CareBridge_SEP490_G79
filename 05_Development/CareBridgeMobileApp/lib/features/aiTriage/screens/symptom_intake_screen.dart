import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../../core/auth/auth_state.dart';
import '../../emergency/services/emergency_service.dart';
import '../models/triage_continuation.dart';
import '../models/triage_entry_context.dart';
import '../models/triage_intake_flow_model.dart';
import '../models/triage_result_model.dart';
import '../services/triage_service.dart';
import '../services/triage_continuation_restore_coordinator.dart';
import '../services/triage_continuation_store.dart';

class SymptomIntakeScreen extends StatefulWidget {
  final TriageService? triageService;
  final EmergencyService? emergencyService;
  final TriageEntryContext entryContext;
  final Future<bool> Function()? postpartumEmergencyLauncher;
  final TriageContinuationRestoreCoordinator? continuationCoordinator;

  const SymptomIntakeScreen({
    super.key,
    this.triageService,
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
  static const _surface = Color(0xFFFFF8F6);
  static const _surfaceLow = Color(0xFFFFF1EC);
  static const _onSurface = Color(0xFF271812);
  static const _onVariant = Color(0xFF524440);
  static const _outline = Color(0xFFD6C2BD);
  static const _maternalStages = {'PRECONCEPTION', 'PREGNANCY', 'POSTPARTUM'};

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
  int _round = 1;
  bool _loading = false;
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
    _emergencyService = widget.emergencyService ?? EmergencyService();
    _continuationCoordinator =
        widget.continuationCoordinator ??
        TriageContinuationRestoreCoordinator(
          store: SecureTriageContinuationStore(),
          gateway: _service,
        );
    _selectedStage = widget.entryContext.stage.apiValue;
    _currentIntake = _newIntake(stage: _selectedStage);
    _messages.add(
      _ChatMessage(
        role: _ChatRole.assistant,
        text: widget.entryContext.isMaternal
            ? 'Hãy mô tả dấu hiệu bạn đang gặp. CareBridge chỉ hỗ trợ phân loại rủi ro ban đầu theo giai đoạn sức khỏe hiện tại.'
            : 'Hãy mô tả triệu chứng của bé. CareBridge sẽ hỏi thêm nếu cần và chỉ phân loại rủi ro ban đầu.',
      ),
    );
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
                const SizedBox(height: 12),
                Text(
                  'Phiên bản: ${consent.currentVersion}',
                  style: Theme.of(dialogContext).textTheme.bodySmall,
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
    final text = _initialController.text.trim();
    if (text.isEmpty) return;
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
    try {
      final response = await _service.startConversation(
        initialText: text,
        currentIntake: _currentIntake,
      );
      if (mounted) _applyResponse(response, userMessage: text);
    } on TriageConsentRequiredFailure {
      bool accepted;
      try {
        accepted = await _requestTriageConsent(triageUserId);
      } catch (_) {
        if (mounted) {
          setState(() {
            _error =
                'Không thể xác nhận điều khoản AI Triage. Vui lòng thử lại.';
          });
        }
        return;
      }
      if (!accepted || !_guardConsentContext(triageUserId)) return;
      try {
        final response = await _service.startConversation(
          initialText: text,
          currentIntake: _currentIntake,
        );
        if (mounted) _applyResponse(response, userMessage: text);
      } on TriageConsentRequiredFailure {
        if (mounted) {
          setState(() {
            _error =
                'Không thể xác nhận điều khoản AI Triage. Vui lòng thử lại.';
          });
        }
      } catch (_) {
        if (mounted) {
          setState(
            () => _error = 'Không thể gửi triệu chứng. Vui lòng thử lại.',
          );
        }
      }
    } catch (_) {
      if (mounted) {
        setState(() => _error = 'Không thể gửi triệu chứng. Vui lòng thử lại.');
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
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
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final response = await _service.continueConversation(
        intakeSessionId: _sessionId!,
        currentIntake: _currentIntake,
        newAnswers: newAnswers,
        round: _round,
      );
      if (mounted) {
        _applyResponse(response, userMessage: _answersText(newAnswers));
      }
    } catch (_) {
      if (mounted) {
        setState(() => _error = 'Không thể gửi câu trả lời. Vui lòng thử lại.');
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

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
    final String? location = switch (decision.destination) {
      TriageContinuationDestination.motherJourney =>
        '/mother-home?tab=1&triageReturn=${Uri.encodeQueryComponent(_sessionId ?? DateTime.now().microsecondsSinceEpoch.toString())}',
      TriageContinuationDestination.babyProfile
          when decision.originReferenceId?.isNotEmpty == true =>
        '/babies/detail/${Uri.encodeComponent(decision.originReferenceId!)}',
      TriageContinuationDestination.safeDashboard => '/mother-home',
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
          decision.destination == TriageContinuationDestination.motherJourney ||
              decision.destination == TriageContinuationDestination.babyProfile
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
            'vomiting',
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

  String _answersText(Map<String, dynamic> answers) {
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
      'parentFreeText': 'Mô tả bổ sung',
    };
    return answers.entries
        .map((entry) => '${labels[entry.key] ?? entry.key}: ${entry.value}')
        .join('\n');
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
    return uri.scheme == 'https' &&
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _surface,
      appBar: AppBar(
        backgroundColor: _surface,
        elevation: 0,
        foregroundColor: _primary,
        title: Text(
          widget.entryContext.isPostpartum
              ? 'Hỗ trợ dấu hiệu sau sinh'
              : widget.entryContext.isMaternal
              ? 'Kiểm tra dấu hiệu an toàn'
              : 'Kiểm tra triệu chứng',
        ),
      ),
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  if (_sessionId == null && !widget.entryContext.lockStage)
                    _buildStageSelector(),
                  ..._messages.map(_buildBubble),
                  if (_result != null) _buildResult(_result!),
                  if (_questions.isNotEmpty && _result == null)
                    _buildQuestions(),
                  if (_error != null) _buildError(),
                ],
              ),
            ),
            _buildComposer(),
          ],
        ),
      ),
    );
  }

  Widget _buildBubble(_ChatMessage message) {
    final isUser = message.role == _ChatRole.user;
    return Align(
      alignment: isUser ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        constraints: const BoxConstraints(maxWidth: 320),
        margin: const EdgeInsets.only(bottom: 10),
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: isUser ? _primary : Colors.white,
          borderRadius: BorderRadius.circular(18),
          border: Border.all(color: isUser ? _primary : _outline),
        ),
        child: Text(
          message.text,
          style: TextStyle(
            color: isUser ? Colors.white : _onSurface,
            height: 1.35,
          ),
        ),
      ),
    );
  }

  Widget _buildQuestions() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: _outline),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Trả lời bổ sung',
            style: TextStyle(fontWeight: FontWeight.w700, color: _onSurface),
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
      'INFANT': 'Bé 0-12 tháng',
      'TODDLER': 'Bé 12-24 tháng',
    };
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: _outline),
      ),
      child: Wrap(
        spacing: 8,
        runSpacing: 8,
        children: stages.entries.map((entry) {
          return ChoiceChip(
            label: Text(entry.value),
            selected: _selectedStage == entry.key,
            onSelected: _loading
                ? null
                : (_) => setState(() {
                    _selectedStage = entry.key;
                    _currentIntake = _newIntake(stage: entry.key);
                  }),
          );
        }).toList(),
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
              border: const OutlineInputBorder(),
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
            'Mức rủi ro: ${result.riskLevel ?? 'CHƯA XÁC ĐỊNH'}',
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
                    backgroundColor: const Color(0xFFC98C7B),
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
                  backgroundColor: const Color(0xFFC98C7B),
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
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 10, 16, 16),
      decoration: const BoxDecoration(color: _surface),
      child: _questions.isEmpty
          ? Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _initialController,
                    minLines: 1,
                    maxLines: 4,
                    decoration: InputDecoration(
                      hintText: widget.entryContext.isMaternal
                          ? 'Ví dụ: Tôi thấy chóng mặt và khó thở...'
                          : 'Ví dụ: Bé bị sốt và ho...',
                      border: OutlineInputBorder(),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                IconButton.filled(
                  onPressed: _loading ? null : _start,
                  icon: _loading
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.send),
                ),
              ],
            )
          : SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                onPressed: _loading ? null : _sendAnswers,
                icon: _loading
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.send),
                label: const Text('Gửi câu trả lời'),
              ),
            ),
    );
  }

  Widget _buildError() {
    return Padding(
      padding: const EdgeInsets.only(top: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(_error!, style: const TextStyle(color: Colors.red)),
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
            final isSelected = selected.contains(option);
            return ChoiceChip(
              label: Text(option),
              selected: isSelected,
              onSelected: (_) => onSelected(option),
            );
          }).toList(),
        ),
      ],
    );
  }
}

enum _ChatRole { user, assistant }

class _ChatMessage {
  final _ChatRole role;
  final String text;

  const _ChatMessage({required this.role, required this.text});
}
