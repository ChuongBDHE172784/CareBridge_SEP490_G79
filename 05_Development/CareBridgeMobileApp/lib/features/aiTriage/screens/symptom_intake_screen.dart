import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';
import '../models/triage_intake_flow_model.dart';
import '../models/triage_result_model.dart';
import '../services/triage_service.dart';

class SymptomIntakeScreen extends StatefulWidget {
  const SymptomIntakeScreen({super.key});

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

  final _service = TriageService();
  final _initialController = TextEditingController();
  final Map<String, TextEditingController> _answerControllers = {};
  final List<_ChatMessage> _messages = [
    const _ChatMessage(
      role: _ChatRole.assistant,
      text: 'Hay mo ta trieu chung cua be. CareBridge se hoi them neu can va chi phan loai rui ro ban dau.',
    ),
  ];

  Map<String, dynamic> _currentIntake = _blankIntake();
  List<IntakeQuestion> _questions = [];
  final Map<String, dynamic> _answers = {};
  String? _sessionId;
  int _round = 1;
  bool _loading = false;
  TriageResult? _result;
  String? _error;

  static Map<String, dynamic> _blankIntake() => {
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
    final text = _initialController.text.trim();
    if (text.isEmpty) return;
    setState(() {
      _loading = true;
      _error = null;
      _messages.add(_ChatMessage(role: _ChatRole.user, text: text));
      _currentIntake = {
        ..._blankIntake(),
        'symptomList': <String>[text],
        'parentFreeText': text,
      };
    });
    try {
      final response = await _service.startConversation(
        initialText: text,
        currentIntake: _currentIntake,
      );
      _applyResponse(response);
    } catch (e) {
      if (mounted) setState(() => _error = 'Khong the gui trieu chung: $e');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _sendAnswers() async {
    if (_sessionId == null || _questions.isEmpty) return;
    final newAnswers = <String, dynamic>{};
    for (final question in _questions) {
      final value = _valueFor(question);
      if (value != null) {
        newAnswers[question.questionKey] = value;
      }
    }
    if (newAnswers.isEmpty) {
      setState(() => _error = 'Vui long tra loi it nhat mot cau hoi.');
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
      _messages.add(_ChatMessage(role: _ChatRole.user, text: _answersText(newAnswers)));
    });
    try {
      final response = await _service.continueConversation(
        intakeSessionId: _sessionId!,
        currentIntake: _currentIntake,
        newAnswers: newAnswers,
        round: _round,
      );
      _applyResponse(response);
    } catch (e) {
      if (mounted) setState(() => _error = 'Khong the gui cau tra loi: $e');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _applyResponse(IntakeFlowResponse response) {
    setState(() {
      _sessionId = response.intakeSessionId;
      _currentIntake = response.mergedIntake;
      _questions = response.questions;
      _round = response.round;
      _result = response.triageResult;
      _answers.clear();
      _answerControllers.clear();
      if ((response.assistantMessage ?? '').isNotEmpty) {
        _messages.add(_ChatMessage(role: _ChatRole.assistant, text: response.assistantMessage!));
      }
      if (response.questions.isNotEmpty) {
        _messages.add(_ChatMessage(
          role: _ChatRole.assistant,
          text: response.questions.map((q) => q.text).join('\n'),
        ));
      }
      if (response.status == 'TRIAGE_COMPLETE' && response.triageResult != null) {
        _messages.add(_ChatMessage(
          role: _ChatRole.assistant,
          text: response.triageResult!.summary ?? 'Da co ket qua phan loai rui ro.',
        ));
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
    return answers.entries.map((e) => '${e.key}: ${e.value}').join('\n');
  }

  Future<void> _openUrl(String url) async {
    final uri = Uri.tryParse(url);
    if (uri != null && await canLaunchUrl(uri)) {
      await launchUrl(uri, mode: LaunchMode.externalApplication);
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
        title: const Text('AI Symptom Intake'),
      ),
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  ..._messages.map(_buildBubble),
                  if (_result != null) _buildResult(_result!),
                  if (_questions.isNotEmpty && _result == null) _buildQuestions(),
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
            'Tra loi bo sung',
            style: TextStyle(fontWeight: FontWeight.w700, color: _onSurface),
          ),
          const SizedBox(height: 12),
          ..._questions.map(_buildQuestionInput),
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
              border: const OutlineInputBorder(),
            ),
          ),
        );
      case 'MULTI_CHOICE':
        final selected = (_answers[question.questionKey] as Set<String>?) ?? <String>{};
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
            onSelected: (value) => setState(() => _answers[question.questionKey] = value),
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
            'Risk: ${result.riskLevel ?? 'UNKNOWN'}',
            style: TextStyle(fontSize: 20, fontWeight: FontWeight.w800, color: color),
          ),
          if ((result.summary ?? '').isNotEmpty) ...[
            const SizedBox(height: 8),
            Text(result.summary!, style: const TextStyle(color: _onSurface, height: 1.35)),
          ],
          if ((result.recommendedAction ?? '').isNotEmpty) ...[
            const SizedBox(height: 12),
            Text(result.recommendedAction!, style: const TextStyle(color: _onVariant, height: 1.35)),
          ],
          if (result.redFlags.isNotEmpty) ...[
            const SizedBox(height: 12),
            Text('Red flags: ${result.redFlags.join(', ')}'),
          ],
          if ((result.warning ?? '').isNotEmpty) ...[
            const SizedBox(height: 12),
            Text(result.warning!, style: const TextStyle(color: Colors.orange)),
          ],
          if (result.citations.isNotEmpty) ...[
            const SizedBox(height: 16),
            const Text('Nguon tham khao chinh thong', style: TextStyle(fontWeight: FontWeight.w700)),
            const SizedBox(height: 8),
            ...result.citations.map(_buildCitation),
          ],
          if ((result.disclaimer ?? '').isNotEmpty) ...[
            const SizedBox(height: 16),
            Text(result.disclaimer!, style: const TextStyle(fontSize: 12, color: _onVariant)),
          ],
        ],
      ),
    );
  }

  Widget _buildCitation(TriageCitation citation) {
    return InkWell(
      onTap: citation.url.isEmpty ? null : () => _openUrl(citation.url),
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
            Text('${citation.organization ?? citation.source} - ${citation.title}',
                style: const TextStyle(fontWeight: FontWeight.w700)),
            if (citation.url.isNotEmpty)
              Text(citation.url, style: const TextStyle(color: _primary, decoration: TextDecoration.underline)),
            if (citation.matchedSymptoms.isNotEmpty)
              Text('Matched symptoms: ${citation.matchedSymptoms.join(', ')}'),
            Text('Status: ${citation.sourceStatus}'),
            if (citation.sourceStatus == 'PENDING_REVIEW')
              const Text('Nguon chinh thong duoc truy xuat tu dong, dang cho kiem duyet noi bo.',
                  style: TextStyle(fontSize: 12, color: _onVariant)),
          ],
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
                      hintText: 'Vi du: Be bi sot va ho...',
                      border: OutlineInputBorder(),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                IconButton.filled(
                  onPressed: _loading ? null : _start,
                  icon: _loading
                      ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                      : const Icon(Icons.send),
                ),
              ],
            )
          : SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                onPressed: _loading ? null : _sendAnswers,
                icon: _loading
                    ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                    : const Icon(Icons.send),
                label: const Text('Gui cau tra loi'),
              ),
            ),
    );
  }

  Widget _buildError() {
    return Padding(
      padding: const EdgeInsets.only(top: 12),
      child: Text(_error!, style: const TextStyle(color: Colors.red)),
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
        Text(question.text, style: const TextStyle(fontWeight: FontWeight.w600)),
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
