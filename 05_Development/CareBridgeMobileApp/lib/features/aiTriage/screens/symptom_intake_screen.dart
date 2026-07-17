import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../emergency/services/emergency_service.dart';
import '../models/triage_intake_flow_model.dart';
import '../models/triage_result_model.dart';
import '../services/triage_service.dart';

class SymptomIntakeScreen extends StatefulWidget {
  final TriageService? triageService;
  final EmergencyService? emergencyService;

  const SymptomIntakeScreen({
    super.key,
    this.triageService,
    this.emergencyService,
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

  late final TriageService _service;
  late final EmergencyService _emergencyService;
  final _initialController = TextEditingController();
  final Map<String, TextEditingController> _answerControllers = {};
  final List<_ChatMessage> _messages = [
    const _ChatMessage(
      role: _ChatRole.assistant,
      text:
          'Hãy mô tả triệu chứng của bé. CareBridge sẽ hỏi thêm nếu cần và chỉ phân loại rủi ro ban đầu.',
    ),
  ];

  Map<String, dynamic> _currentIntake = _blankIntake();
  List<IntakeQuestion> _questions = [];
  final Map<String, dynamic> _answers = {};
  String _selectedStage = 'INFANT';
  String? _sessionId;
  int _round = 1;
  bool _loading = false;
  bool _openingEmergency = false;
  bool _emergencyFailed = false;
  TriageResult? _result;
  String? _error;

  @override
  void initState() {
    super.initState();
    _service = widget.triageService ?? TriageService();
    _emergencyService = widget.emergencyService ?? EmergencyService();
  }

  static Map<String, dynamic> _blankIntake({String stage = 'INFANT'}) => {
    'stage': stage,
    'childAgeMonths': null,
    'symptomList': <String>[],
    'duration': null,
    'temperatureC': null,
    'feedingStatus': null,
    'breathingStatus': null,
    'consciousnessStatus': null,
    'vomiting': null,
    'diarrhea': null,
    'rash': null,
    'seizure': null,
    'dehydrationSigns': <String>[],
    'parentFreeText': null,
  };

  @override
  void dispose() {
    _initialController.dispose();
    for (final controller in _answerControllers.values) {
      controller.dispose();
    }
    super.dispose();
  }

  Future<void> _start() async {
    if (_loading) return;
    final text = _initialController.text.trim();
    if (text.isEmpty) return;
    setState(() {
      _loading = true;
      _error = null;
      _currentIntake = {
        ..._blankIntake(stage: _selectedStage),
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

  void _applyResponse(IntakeFlowResponse response, {String? userMessage}) {
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
    setState(() {
      _openingEmergency = true;
      _emergencyFailed = false;
      _error = null;
    });
    try {
      await _emergencyService
          .openFlow(triggerSource: 'AI_TRIAGE')
          .timeout(const Duration(seconds: 8));
      if (!mounted) return;
      context.push('/emergency/map');
    } catch (_) {
      if (mounted) {
        setState(() {
          _emergencyFailed = true;
          _error =
              'Không thể kích hoạt hỗ trợ khẩn cấp. Vui lòng gọi cấp cứu hoặc thử lại.';
        });
      }
    } finally {
      if (mounted) setState(() => _openingEmergency = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _surface,
      appBar: AppBar(
        backgroundColor: _surface,
        elevation: 0,
        foregroundColor: _primary,
        title: const Text('Kiểm tra triệu chứng'),
      ),
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  if (_sessionId == null) _buildStageSelector(),
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
                    _currentIntake = _blankIntake(stage: entry.key);
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
            SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                key: const Key('triage-emergency-cta'),
                onPressed: _openingEmergency ? null : _openEmergencyFlow,
                icon: _openingEmergency
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.emergency),
                label: const Text('Kích hoạt hỗ trợ khẩn cấp'),
              ),
            ),
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
                    decoration: const InputDecoration(
                      hintText: 'Ví dụ: Bé bị sốt và ho...',
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
          if (_emergencyFailed)
            TextButton.icon(
              key: const Key('triage-emergency-fallback-map'),
              onPressed: () => context.push('/emergency/map'),
              icon: const Icon(Icons.map_outlined),
              label: const Text('Vẫn mở bản đồ khẩn cấp'),
            ),
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
