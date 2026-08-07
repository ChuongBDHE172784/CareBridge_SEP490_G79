import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../models/triage_v2_session.dart';
import '../services/triage_v2_service.dart';

/// Internal-only V2 client. The router keeps this screen behind the compile-time
/// AI_TRIAGE_V2_INTERNAL_ENABLED flag; V1 remains user-facing by default.
class TriageV2Screen extends StatefulWidget {
  const TriageV2Screen({super.key, this.service});

  final TriageV2Service? service;

  @override
  State<TriageV2Screen> createState() => _TriageV2ScreenState();
}

class _TriageV2ScreenState extends State<TriageV2Screen> {
  late final TriageV2Service _service = widget.service ?? TriageV2Service();
  final _message = TextEditingController();
  TriageV2Session? _session;
  String _selectedTarget = 'UNKNOWN';
  String? _selectedOption;
  String? _selectedQuestionId;
  bool _bothRequested = false;
  String? _firstTarget;
  bool _busy = false;
  String? _error;

  @override
  void dispose() {
    _message.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final text = (_selectedOption ?? _message.text).trim();
    if (text.isEmpty || _busy) return;
    if (_selectedOption == 'CLARIFY_TARGET_BOTH') _bothRequested = true;
    if (_bothRequested &&
        {
          'CLARIFY_TARGET_MOTHER',
          'CLARIFY_TARGET_BABY',
        }.contains(_selectedOption)) {
      _firstTarget = _selectedOption == 'CLARIFY_TARGET_MOTHER'
          ? 'MOTHER'
          : 'BABY';
    }
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final next = _session == null
          ? await _service.start(message: text, selectedTarget: _selectedTarget)
          : await _service.continueSession(
              session: _session!,
              message: text,
              // Identifiers only. The server maps them to clinical meaning; the option code is
              // no longer smuggled through as free text for an extractor to guess at.
              answers: (_selectedOption == null || _selectedQuestionId == null)
                  ? const []
                  : [
                      TriageV2Answer(
                        questionId: _selectedQuestionId!,
                        optionCode: _selectedOption!,
                      ),
                    ],
            );
      if (!mounted) return;
      setState(() {
        _session = next;
        _selectedOption = null;
        _selectedQuestionId = null;
        _message.clear();
      });
    } on TriageV2StaleVersionFailure {
      final current = _session;
      if (current != null) {
        try {
          final refreshed = await _service.get(current.sessionId);
          if (mounted) {
            setState(() {
              _session = refreshed;
              _error =
                  'Phiên đã được cập nhật ở nơi khác. Vui lòng kiểm tra và gửi lại.';
            });
          }
        } catch (_) {
          if (mounted) setState(() => _error = _unavailableText);
        }
      }
    } catch (_) {
      if (mounted) setState(() => _error = _unavailableText);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  void _startOtherPerson() {
    setState(() {
      _selectedTarget = _firstTarget == 'MOTHER' ? 'BABY' : 'MOTHER';
      _session = null;
      _selectedOption = null;
      _selectedQuestionId = null;
      _bothRequested = false;
      _firstTarget = null;
      _error = null;
      _message.clear();
    });
  }

  Future<void> _cancel() async {
    final current = _session;
    if (current == null || _busy) return;
    setState(() => _busy = true);
    try {
      final cancelled = await _service.cancel(current);
      if (mounted) setState(() => _session = cancelled);
    } catch (_) {
      if (mounted) setState(() => _error = _unavailableText);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  static const _unavailableText =
      'Hiện chưa thể hoàn tất định hướng nguy cơ. Kết quả lỗi không được xem là mức an toàn.';

  @override
  Widget build(BuildContext context) {
    final session = _session;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Định hướng nguy cơ sức khỏe'),
        actions: [
          if (session != null && !session.stop)
            IconButton(
              key: const Key('triage-v2-cancel'),
              onPressed: _busy ? null : _cancel,
              tooltip: 'Hủy phiên',
              icon: const Icon(Icons.close),
            ),
        ],
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            const Text(
              'Công cụ học thuật hỗ trợ định hướng rủi ro, không chẩn đoán và không kê thuốc.',
              key: Key('triage-v2-governance'),
            ),
            const SizedBox(height: 16),
            if (session == null) _buildTargetSelector(),
            if (session != null) ...[
              _buildContext(session),
              const SizedBox(height: 12),
              // Action precedes all evidence on RED by construction.
              if (session.outcome == 'RED') _buildRedAction(session),
              if (session.outcome == 'YELLOW') _buildYellowAction(session),
              if (session.outcome == 'NEEDS_MORE_INFO') _buildNeedsMoreInfo(),
              if (session.outcome == 'OUT_OF_SCOPE') _buildOutOfScope(),
              if (session.isUnavailable) _buildUnavailable(),
              if (!session.stop && session.questionIds.isNotEmpty)
                ..._buildQuestions(session),
              if (session.outcome != 'RED' || !session.stop)
                ..._buildVerifiedSources(session),
              // RED citations, if any, are deliberately rendered only after action.
              if (session.outcome == 'RED' && session.stop)
                ..._buildVerifiedSources(session),
              const SizedBox(height: 12),
              Text(
                session.disclaimer,
                key: const Key('triage-v2-disclaimer'),
                style: Theme.of(context).textTheme.bodySmall,
              ),
              if (session.stop && _bothRequested && _firstTarget != null) ...[
                const SizedBox(height: 12),
                OutlinedButton(
                  key: const Key('triage-v2-start-other-person'),
                  onPressed: _startOtherPerson,
                  child: const Text('Mở phiên riêng cho người còn lại'),
                ),
              ],
            ],
            if (_error != null) ...[
              const SizedBox(height: 12),
              Text(
                _error!,
                key: const Key('triage-v2-error'),
                style: const TextStyle(color: Colors.red),
              ),
            ],
            if (session == null ||
                (!session.stop && _needsFreeText(session))) ...[
              const SizedBox(height: 16),
              TextField(
                key: const Key('triage-v2-message'),
                controller: _message,
                enabled: !_busy,
                maxLines: 4,
                decoration: const InputDecoration(
                  labelText: 'Mô tả dấu hiệu hoặc trả lời câu hỏi',
                  border: OutlineInputBorder(),
                ),
              ),
            ],
            if (session == null || !session.stop) ...[
              const SizedBox(height: 12),
              FilledButton(
                key: const Key('triage-v2-submit'),
                onPressed: _busy ? null : _submit,
                child: Text(_busy ? 'Đang xử lý…' : 'Gửi'),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildTargetSelector() => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      const Text(
        'Đánh giá cho ai?',
        style: TextStyle(fontWeight: FontWeight.bold),
      ),
      const SizedBox(height: 8),
      Wrap(
        spacing: 8,
        children: const {'UNKNOWN': 'Chưa rõ', 'MOTHER': 'Mẹ', 'BABY': 'Bé'}
            .entries
            .map(
              (entry) => ChoiceChip(
                key: Key('triage-v2-target-${entry.key}'),
                label: Text(entry.value),
                selected: _selectedTarget == entry.key,
                onSelected: (_) => setState(() => _selectedTarget = entry.key),
              ),
            )
            .toList(),
      ),
    ],
  );

  Widget _buildContext(TriageV2Session session) => Card(
    key: const Key('triage-v2-context'),
    child: Padding(
      padding: const EdgeInsets.all(12),
      child: Text(
        'Đối tượng: ${_targetLabel(session.target)}\nGiai đoạn: ${_stageLabel(session.stage)}',
      ),
    ),
  );

  Widget _buildRedAction(TriageV2Session session) => Card(
    key: const Key('triage-v2-red-action'),
    color: const Color(0xFFFFDAD6),
    child: const Padding(
      padding: EdgeInsets.all(16),
      child: Text(
        'Có dấu hiệu cần hỗ trợ khẩn cấp. Hãy liên hệ cấp cứu 115 hoặc đến cơ sở y tế gần nhất ngay.',
        style: TextStyle(fontWeight: FontWeight.bold),
      ),
    ),
  );

  Widget _buildYellowAction(TriageV2Session session) => const Card(
    key: Key('triage-v2-yellow-action'),
    child: Padding(
      padding: EdgeInsets.all(16),
      child: Text('Nên liên hệ nhân viên y tế để được đánh giá sớm.'),
    ),
  );

  Widget _buildNeedsMoreInfo() => const Card(
    key: Key('triage-v2-needs-more-info'),
    child: Padding(
      padding: EdgeInsets.all(16),
      child: Text('Cần thêm thông tin trước khi định hướng nguy cơ.'),
    ),
  );

  Widget _buildOutOfScope() => const Card(
    key: Key('triage-v2-out-of-scope'),
    child: Padding(
      padding: EdgeInsets.all(16),
      child: Text(
        'Nội dung này nằm ngoài phạm vi sức khỏe sinh sản/mẹ và bé của công cụ.',
      ),
    ),
  );

  Widget _buildUnavailable() => const Card(
    key: Key('triage-v2-unavailable'),
    child: Padding(padding: EdgeInsets.all(16), child: Text(_unavailableText)),
  );

  List<Widget> _buildQuestions(TriageV2Session session) => [
    const SizedBox(height: 12),
    ...session.questionIds.map((id) {
      final question = TriageV2Question.fromId(id);
      return Card(
        key: Key('triage-v2-question-$id'),
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(question.text),
              if (question.optionCodes.isNotEmpty) ...[
                const SizedBox(height: 8),
                Wrap(
                  spacing: 8,
                  children: question.optionCodes
                      .map(
                        (code) => ChoiceChip(
                          key: Key('triage-v2-option-$code'),
                          label: Text(_optionLabel(code)),
                          selected:
                              _selectedOption == code &&
                              _selectedQuestionId == id,
                          onSelected: (_) => setState(() {
                            _selectedOption = code;
                            _selectedQuestionId = id;
                          }),
                        ),
                      )
                      .toList(),
                ),
              ],
            ],
          ),
        ),
      );
    }),
  ];

  bool _needsFreeText(TriageV2Session session) =>
      session.questionIds.isEmpty ||
      session.questionIds
          .map(TriageV2Question.fromId)
          .any((question) => question.optionCodes.isEmpty);

  List<Widget> _buildVerifiedSources(TriageV2Session session) {
    if (session.citations.isEmpty) return const [];
    return [
      const SizedBox(height: 16),
      const Text(
        'Nguồn đã xác minh',
        style: TextStyle(fontWeight: FontWeight.bold),
      ),
      ...session.citations.map(
        (citation) => ListTile(
          key: Key('triage-v2-source-${citation.sourceId}'),
          title: Text(citation.title),
          subtitle: Text('${citation.organization} — ${citation.section}'),
          trailing: const Icon(Icons.open_in_new),
          onTap: () => launchUrl(Uri.parse(citation.url)),
        ),
      ),
    ];
  }
}

String _targetLabel(String value) => switch (value) {
  'MOTHER' => 'Mẹ',
  'BABY' => 'Bé',
  'CONFLICTED' => 'Cần chọn một người trước',
  _ => 'Chưa xác định',
};

String _stageLabel(String value) => switch (value) {
  'PRECONCEPTION' => 'Trước mang thai',
  'POSSIBLE_PREGNANCY' => 'Có thể mang thai',
  'PREGNANCY' => 'Đang mang thai',
  'POSTPARTUM_MOTHER' => 'Mẹ sau sinh',
  'INFANT_0_12M' => 'Bé 0–12 tháng',
  'TODDLER_12_24M' => 'Bé 12–24 tháng',
  'CONFLICTED' => 'Mâu thuẫn, cần làm rõ',
  _ => 'Chưa xác định',
};

String _optionLabel(String code) => switch (code) {
  'DANGER_NONE' => 'Không có dấu hiệu nào',
  'DANGER_SEIZURE' => 'Co giật',
  'DANGER_UNCONSCIOUS' => 'Lơ mơ, khó đánh thức hoặc ngất',
  'DANGER_BREATHING' => 'Khó thở nghiêm trọng',
  'DANGER_CYANOSIS' => 'Tím tái môi hoặc đầu ngón',
  'SELF_HARM_NONE' => 'Không có',
  'SELF_HARM_THOUGHTS' => 'Có ý nghĩ làm hại bản thân',
  'SELF_HARM_PLAN' => 'Có ý định hoặc kế hoạch cụ thể',
  'HARM_TO_BABY_THOUGHTS' => 'Có ý nghĩ làm hại em bé',
  'CANNOT_ENSURE_SAFETY' => 'Tôi không chắc mình giữ được an toàn lúc này',
  'FEEDING_NORMAL' => 'Bú/uống bình thường',
  'FEEDING_REDUCED' => 'Bú/uống kém hơn hẳn',
  'FEEDING_REFUSED' => 'Bỏ bú hoặc không uống được',
  'HYDRATION_NORMAL' => 'Không, bé vẫn tiểu bình thường',
  'HYDRATION_REDUCED' => 'Tiểu ít hơn, môi khô, khát nước',
  'HYDRATION_SEVERE' => 'Mắt trũng, da khô, rất ít hoặc không tiểu',
  'TEMP_LT_38' => 'Dưới 38 độ',
  'TEMP_38_TO_39' => 'Từ 38 đến dưới 39 độ',
  'TEMP_GTE_39' => 'Từ 39 độ trở lên',
  'HEADACHE_NONE' => 'Không đau đầu',
  'HEADACHE_MILD' => 'Đau nhẹ',
  'HEADACHE_SEVERE' => 'Đau dữ dội hoặc không đỡ khi nghỉ',
  'CLARIFY_TARGET_MOTHER' => 'Mẹ trước',
  'CLARIFY_TARGET_BABY' => 'Bé trước',
  'CLARIFY_TARGET_BOTH' => 'Cả mẹ và bé',
  'BLEEDING_NONE' => 'Không ra máu',
  'BLEEDING_SPOTTING' => 'Ra ít, chỉ thấm nhẹ',
  'BLEEDING_MODERATE' => 'Ra vừa',
  'BLEEDING_HEAVY' => 'Nhiều, thấm ướt băng trong thời gian ngắn',
  'CLOTS_LARGE' => 'Có, cục lớn',
  'CLOTS_NONE' => 'Không',
  'DIZZINESS_YES' || 'VISUAL_CHANGE_YES' || 'EPIGASTRIC_YES' || 'SWELLING_YES' => 'Có',
  'DIZZINESS_NO' || 'VISUAL_CHANGE_NO' || 'EPIGASTRIC_NO' || 'SWELLING_NO' => 'Không',
  'BP_GTE_140_90' => 'Từ 140/90 trở lên',
  'BP_LT_140_90' => 'Dưới 140/90',
  'NO_DEVICE_OR_UNAWARE' => 'Tôi không có thiết bị đo / không rõ',
  'PAIN_NONE' => 'Không đau',
  'PAIN_MILD' => 'Đau nhẹ',
  'PAIN_MODERATE' => 'Đau vừa',
  'PAIN_SEVERE' => 'Đau dữ dội',
  'TEST_POSITIVE' => 'Đã thử và dương tính',
  'TEST_NEGATIVE' => 'Đã thử và âm tính',
  'TEST_NOT_DONE' => 'Chưa thử',
  'INTENT_SYMPTOM_TRIAGE' => 'Tôi đang có triệu chứng và muốn được định hướng',
  'INTENT_GENERAL_INFO' => 'Tôi muốn tìm hiểu thông tin chung',
  'INTENT_SOURCE_LOOKUP' => 'Tôi muốn biết nguồn tham khảo',
  'UNSURE' => 'Tôi không chắc',
  _ => code,
};
