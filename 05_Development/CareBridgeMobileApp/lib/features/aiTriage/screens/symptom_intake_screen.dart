import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../../core/auth/auth_state.dart';
import '../../../core/network/api_client.dart';
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
  static const _primary = Color(0xFFC98C7B);
  static const _primaryDark = Color(0xFF845143);
  static const _surface = Color(0xFFF6F1EC);
  static const _surfaceLow = Color(0xFFF2EAE4);
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
    if (!_stageConfirmed) {
      setState(
        () => _error =
            'Vui lòng chọn giai đoạn sức khỏe trước khi bắt đầu AI Triage.',
      );
      return;
    }
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
      } catch (error) {
        if (mounted) {
          setState(() => _error = _startFailureMessage(error));
        }
      }
    } catch (error) {
      if (mounted) {
        setState(() => _error = _startFailureMessage(error));
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  String _startFailureMessage(Object error) {
    if (error is ApiException && error.statusCode == 400) {
      return 'Thông tin triệu chứng chưa hợp lệ. Vui lòng kiểm tra lại và thử lại.';
    }
    if (error is ApiException && error.statusCode >= 500) {
      return 'Dịch vụ phân loại đang tạm thời gặp sự cố. Vui lòng thử lại sau ít phút.';
    }
    return 'Không thể gửi triệu chứng. Vui lòng thử lại.';
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
      'painSeverity': 'Mức độ đau',
      'urinarySymptoms': 'Triệu chứng tiểu tiện',
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
              final selected = _selectedStage == entry.key;
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
                    : (_) => setState(() {
                        _selectedStage = entry.key;
                        _stageConfirmed = true;
                        _currentIntake = _newIntake(stage: entry.key);
                      }),
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
          ? Row(
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
                    onPressed: _loading ? null : _start,
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
