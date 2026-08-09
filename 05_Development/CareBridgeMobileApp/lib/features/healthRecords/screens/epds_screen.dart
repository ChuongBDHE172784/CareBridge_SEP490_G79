import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../aiTriage/models/triage_entry_context.dart';
import '../models/health_metric_model.dart';
import '../services/health_metric_service.dart';

class EpdsOption {
  const EpdsOption(this.label, this.score);
  final String label;
  final int score;
}

class EpdsHistoryDetailScreen extends StatelessWidget {
  const EpdsHistoryDetailScreen({
    super.key,
    required this.completedAt,
    required this.totalScore,
    required this.question10Score,
    required this.answers,
  });

  final DateTime completedAt;
  final int totalScore;
  final int question10Score;
  final List<int> answers;

  static const _primary = Color(0xFF845143);
  static const _canvas = Color(0xFFFFF8F6);
  static const _selected = Color(0xFFF2EAE4);
  static const _danger = Color(0xFFBA1A1A);

  @override
  Widget build(BuildContext context) {
    final date =
        '${completedAt.day.toString().padLeft(2, '0')}/'
        '${completedAt.month.toString().padLeft(2, '0')}/${completedAt.year}';
    return Scaffold(
      backgroundColor: _canvas,
      appBar: AppBar(
        backgroundColor: _canvas,
        elevation: 0,
        foregroundColor: _primary,
        title: const Text(
          'Chi tiết sàng lọc EPDS',
          style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w700),
        ),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 8, 20, 28),
        children: [
          Container(
            padding: const EdgeInsets.all(18),
            decoration: BoxDecoration(
              color: _selected,
              borderRadius: BorderRadius.circular(18),
            ),
            child: Row(
              children: [
                CircleAvatar(
                  radius: 28,
                  backgroundColor: Colors.white,
                  child: Text(
                    '$totalScore',
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 20,
                      color: _primary,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Tổng điểm $totalScore/30',
                        style: const TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 17,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        'Hoàn thành ngày $date · Câu 10: $question10Score/3',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          color: question10Score > 0 ? _danger : _primary,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),
          const Text(
            'Câu hỏi và câu trả lời đã chọn',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 18,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 10),
          ...List.generate(epdsQuestions.length, (questionIndex) {
            final question = epdsQuestions[questionIndex];
            final selectedScore = answers[questionIndex];
            return Card(
              margin: const EdgeInsets.only(bottom: 12),
              color: Colors.white,
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Câu ${questionIndex + 1}. ${question.text}',
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontWeight: FontWeight.w700,
                        height: 1.4,
                      ),
                    ),
                    const SizedBox(height: 10),
                    ...question.options.map((option) {
                      final isSelected = option.score == selectedScore;
                      final highlightDanger =
                          questionIndex == 9 && selectedScore > 0;
                      return Container(
                        margin: const EdgeInsets.only(bottom: 7),
                        padding: const EdgeInsets.symmetric(
                          horizontal: 12,
                          vertical: 10,
                        ),
                        decoration: BoxDecoration(
                          color: isSelected ? _selected : _canvas,
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(
                            color: isSelected
                                ? (highlightDanger ? _danger : _primary)
                                : const Color(0xFFE6D8D3),
                            width: isSelected ? 1.5 : 1,
                          ),
                        ),
                        child: Row(
                          children: [
                            Icon(
                              isSelected
                                  ? Icons.check_circle_rounded
                                  : Icons.radio_button_unchecked_rounded,
                              size: 20,
                              color: isSelected
                                  ? (highlightDanger ? _danger : _primary)
                                  : const Color(0xFF8C7A74),
                            ),
                            const SizedBox(width: 10),
                            Expanded(
                              child: Text(
                                option.label,
                                style: TextStyle(
                                  fontFamily: 'Lexend',
                                  fontWeight: isSelected
                                      ? FontWeight.w700
                                      : FontWeight.w400,
                                ),
                              ),
                            ),
                            Text(
                              '${option.score}đ',
                              style: TextStyle(
                                fontFamily: 'Lexend',
                                color: isSelected ? _primary : Colors.grey,
                                fontWeight: isSelected
                                    ? FontWeight.w700
                                    : FontWeight.w400,
                              ),
                            ),
                          ],
                        ),
                      );
                    }),
                  ],
                ),
              ),
            );
          }),
        ],
      ),
    );
  }
}

class EpdsQuestion {
  const EpdsQuestion(this.text, this.options);
  final String text;
  final List<EpdsOption> options;
}

const epdsQuestions = <EpdsQuestion>[
  EpdsQuestion(
    'Tôi vẫn có thể cười và thấy được khía cạnh khôi hài của sự việc:',
    [
      EpdsOption('Vẫn như trước', 0),
      EpdsOption('Không nhiều như trước', 1),
      EpdsOption('Chắc chắn không nhiều như trước', 2),
      EpdsOption('Không có gì cả', 3),
    ],
  ),
  EpdsQuestion('Tôi vui vẻ trông chờ mọi việc:', [
    EpdsOption('Vẫn (nhiều) như trước', 0),
    EpdsOption('Ít hơn trước', 1),
    EpdsOption('Chắc chắn ít hơn trước', 2),
    EpdsOption('Gần như không có', 3),
  ]),
  EpdsQuestion(
    'Tôi trách mình một cách không cần thiết khi chuyện xảy ra không được như ý:',
    [
      EpdsOption('Có, phần lớn thời gian', 3),
      EpdsOption('Có, thỉnh thoảng', 2),
      EpdsOption('Không thường lắm', 1),
      EpdsOption('Không, không bao giờ', 0),
    ],
  ),
  EpdsQuestion(
    'Tôi cảm thấy không yên tâm hay lo lắng mà không có lý do chính đáng:',
    [
      EpdsOption('Không, không có', 0),
      EpdsOption('Gần như không có', 1),
      EpdsOption('Có, thỉnh thoảng', 2),
      EpdsOption('Có, rất thường', 3),
    ],
  ),
  EpdsQuestion(
    'Tôi cảm thấy sợ sệt hay hoảng hốt mà không có lý do thật chính đáng:',
    [
      EpdsOption('Có, khá nhiều', 3),
      EpdsOption('Có, thỉnh thoảng', 2),
      EpdsOption('Không, không nhiều', 1),
      EpdsOption('Không, hoàn toàn không có', 0),
    ],
  ),
  EpdsQuestion('Mọi việc trở nên quá sức chịu đựng của tôi:', [
    EpdsOption('Phải, tôi hầu như không thể đương đầu nổi', 3),
    EpdsOption('Phải, tôi đôi khi không đương đầu hiệu quả như mọi khi', 2),
    EpdsOption('Không, tôi hầu như đối phó được khá hiệu quả', 1),
    EpdsOption('Không, tôi vẫn đối phó được hiệu quả như mọi khi', 0),
  ]),
  EpdsQuestion('Tôi khổ sở đến nỗi khó ngủ:', [
    EpdsOption('Có, phần lớn thời gian', 3),
    EpdsOption('Có, thỉnh thoảng', 2),
    EpdsOption('Không, không thường lắm', 1),
    EpdsOption('Không, không bao giờ', 0),
  ]),
  EpdsQuestion('Tôi cảm thấy buồn hoặc đau khổ:', [
    EpdsOption('Có, phần lớn thời gian', 3),
    EpdsOption('Có, khá thường xuyên', 2),
    EpdsOption('Không thường lắm', 1),
    EpdsOption('Không, không bao giờ', 0),
  ]),
  EpdsQuestion('Tôi đã buồn đến mức phát khóc:', [
    EpdsOption('Có, phần lớn thời gian', 3),
    EpdsOption('Có, khá thường xuyên', 2),
    EpdsOption('Chỉ đôi khi', 1),
    EpdsOption('Không, không bao giờ', 0),
  ]),
  EpdsQuestion('Tôi đã từng nghĩ đến chuyện tự hại bản thân:', [
    EpdsOption('Có, khá thường xuyên', 3),
    EpdsOption('Thỉnh thoảng', 2),
    EpdsOption('Gần như không có', 1),
    EpdsOption('Chưa bao giờ', 0),
  ]),
];

int calculateEpdsScore(List<int?> answers) =>
    answers.whereType<int>().fold(0, (sum, score) => sum + score);

List<int>? parseEpdsAnswers(String? note) {
  if (note == null || note.isEmpty) return null;
  try {
    final decoded = jsonDecode(note);
    if (decoded is! Map<String, dynamic>) return null;
    final rawAnswers = decoded['answers'];
    if (rawAnswers is! List || rawAnswers.length != epdsQuestions.length) {
      return null;
    }
    final answers = rawAnswers.map((value) => (value as num).toInt()).toList();
    if (answers.any((score) => score < 0 || score > 3)) return null;
    return answers;
  } catch (_) {
    return null;
  }
}

class EpdsScreen extends StatefulWidget {
  const EpdsScreen({super.key, required this.journeyId});
  final String journeyId;

  @override
  State<EpdsScreen> createState() => _EpdsScreenState();
}

class _EpdsScreenState extends State<EpdsScreen> {
  static const _primary = Color(0xFF845143);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Color(0xFFFFFFFF);
  final _service = HealthMetricService();
  final _answers = List<int?>.filled(10, null);
  List<MetricDataPoint> _history = [];
  int _questionIndex = 0;
  bool _started = false;
  bool _saving = false;
  int? _result;

  @override
  void initState() {
    super.initState();
    _loadHistory();
  }

  Future<void> _loadHistory() async {
    try {
      final trend = await _service.getMetricTrend(
        journeyId: widget.journeyId,
        metricType: 'EPDS_SCORE',
        from: DateTime.now().subtract(const Duration(days: 365)),
        to: DateTime.now().add(const Duration(days: 1)),
      );
      if (mounted) {
        setState(() => _history = trend.dataPoints.reversed.toList());
      }
    } catch (_) {}
  }

  String _level(int score) {
    if (score >= 13) return 'Cần được đánh giá chuyên sâu';
    if (score >= 10) return 'Cần theo dõi và sàng lọc lại';
    return 'Nguy cơ hiện tại thấp';
  }

  String _guidance(int score) {
    if (_answers[9] != null && _answers[9]! > 0) {
      return 'Cần đánh giá sức khỏe tâm thần ngay và hỗ trợ khẩn nếu có ý nghĩ tự sát.';
    }
    if (score >= 13) {
      return 'Nên sắp xếp gặp bác sĩ hoặc chuyên gia tâm lý để được đánh giá chuyên sâu.';
    }
    if (score >= 10) return 'Theo dõi sát và thực hiện lại EPDS sau 2–4 tuần.';
    return 'Tiếp tục theo dõi và thực hiện lại theo lịch thai kỳ hoặc sau sinh.';
  }

  Future<void> _submit() async {
    if (_answers.any((answer) => answer == null)) return;
    final total = calculateEpdsScore(_answers);
    final question10 = _answers[9]!;
    setState(() => _saving = true);
    try {
      await _service.addMetric(
        widget.journeyId,
        AddMetricRequest(
          metricType: 'EPDS_SCORE',
          valueNumeric: total.toDouble(),
          valueSecondary: question10.toDouble(),
          unit: 'điểm',
          measuredAt: DateTime.now(),
          note: jsonEncode({
            'version': 'EPDS_VI_NSW_2023',
            'periodDays': 7,
            'answers': _answers,
          }),
        ),
      );
      if (!mounted) return;
      setState(() {
        _result = total;
        _started = false;
      });
      await _loadHistory();
      if (question10 > 0 && mounted) await _showUrgentSafetyDialog();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Không thể lưu kết quả EPDS. Vui lòng thử lại.'),
          ),
        );
        if (question10 > 0) await _showUrgentSafetyDialog();
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  Future<void> _showUrgentSafetyDialog() async {
    await showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) => AlertDialog(
        icon: const Icon(
          Icons.health_and_safety_rounded,
          color: Color(0xFFBA1A1A),
          size: 40,
        ),
        title: const Text('Bạn cần được hỗ trợ ngay'),
        content: const Text(
          'Câu trả lời cho thấy bạn từng nghĩ đến việc tự hại bản thân. Hãy ở cùng một người bạn tin cậy và liên hệ hỗ trợ y tế ngay bây giờ.',
        ),
        actions: [
          TextButton(
            onPressed: () async => launchUrl(Uri(scheme: 'tel', path: '115')),
            child: const Text('Gọi 115'),
          ),
          FilledButton(
            onPressed: () {
              Navigator.pop(dialogContext);
              context.push(
                '/triage/intake',
                extra: const TriageEntryContext(requiresStageSelection: true),
              );
            },
            child: const Text('Mở hỗ trợ khẩn cấp'),
          ),
        ],
      ),
    );
  }

  void _restart() => setState(() {
    _answers.fillRange(0, _answers.length, null);
    _questionIndex = 0;
    _result = null;
    _started = true;
  });

  void _openHistoryDetail(MetricDataPoint item) {
    final answers = parseEpdsAnswers(item.note);
    if (answers == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Lần sàng lọc này không có dữ liệu bộ đáp án.'),
        ),
      );
      return;
    }
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => EpdsHistoryDetailScreen(
          completedAt: item.measuredAt,
          totalScore: item.valueNumeric.round(),
          question10Score: item.valueSecondary?.round() ?? answers[9],
          answers: answers,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    backgroundColor: _canvas,
    appBar: AppBar(
      backgroundColor: _canvas,
      elevation: 0,
      foregroundColor: _primary,
      title: const Text(
        'Sàng lọc tâm trạng EPDS',
        style: TextStyle(fontFamily: 'Lexend', fontWeight: FontWeight.w700),
      ),
    ),
    body: SafeArea(child: _started ? _buildQuestion() : _buildOverview()),
  );

  Widget _buildQuestion() {
    final question = epdsQuestions[_questionIndex];
    return Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          LinearProgressIndicator(
            value: (_questionIndex + 1) / 10,
            color: _primary,
            backgroundColor: const Color(0xFFF2EAE4),
          ),
          const SizedBox(height: 14),
          Text(
            'Câu ${_questionIndex + 1}/10 · Trong 7 ngày qua',
            style: const TextStyle(
              fontFamily: 'Lexend',
              color: _primary,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 18),
          Text(
            question.text,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 21,
              height: 1.35,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 16),
          Expanded(
            child: RadioGroup<int>(
              groupValue: _answers[_questionIndex],
              onChanged: (value) =>
                  setState(() => _answers[_questionIndex] = value),
              child: ListView(
                children: List.generate(question.options.length, (index) {
                  final option = question.options[index];
                  return Card(
                    color: _surface,
                    child: RadioListTile<int>(
                      value: option.score,
                      title: Text(
                        option.label,
                        style: const TextStyle(fontFamily: 'Lexend'),
                      ),
                    ),
                  );
                }),
              ),
            ),
          ),
          Row(
            children: [
              if (_questionIndex > 0)
                Expanded(
                  child: OutlinedButton(
                    onPressed: () => setState(() => _questionIndex--),
                    child: const Text('Quay lại'),
                  ),
                ),
              if (_questionIndex > 0) const SizedBox(width: 12),
              Expanded(
                child: FilledButton(
                  onPressed: _answers[_questionIndex] == null || _saving
                      ? null
                      : (_questionIndex == 9
                            ? _submit
                            : () => setState(() => _questionIndex++)),
                  style: FilledButton.styleFrom(
                    backgroundColor: _primary,
                    minimumSize: const Size.fromHeight(50),
                  ),
                  child: _saving
                      ? const SizedBox(
                          width: 22,
                          height: 22,
                          child: CircularProgressIndicator(
                            color: Colors.white,
                            strokeWidth: 2,
                          ),
                        )
                      : Text(_questionIndex == 9 ? 'Hoàn tất' : 'Tiếp tục'),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildOverview() => ListView(
    padding: const EdgeInsets.all(20),
    children: [
      Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: const Color(0xFFF2EAE4),
          borderRadius: BorderRadius.circular(20),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Tâm trạng trong 7 ngày qua',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 22,
                fontWeight: FontWeight.w700,
                color: _primary,
              ),
            ),
            const SizedBox(height: 8),
            const Text(
              'EPDS gồm 10 câu hỏi ngắn dành cho giai đoạn mang thai và sau sinh. Đây là công cụ sàng lọc, không phải chẩn đoán.',
              style: TextStyle(fontFamily: 'Lexend', height: 1.5),
            ),
            if (_result != null) ...[
              const SizedBox(height: 14),
              Text(
                'Kết quả: $_result/30 · ${_level(_result!)}',
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 6),
              Text(
                _guidance(_result!),
                style: const TextStyle(fontFamily: 'Lexend', height: 1.4),
              ),
            ],
            const SizedBox(height: 18),
            FilledButton(
              onPressed: _restart,
              style: FilledButton.styleFrom(backgroundColor: _primary),
              child: Text(
                _history.isEmpty ? 'Bắt đầu sàng lọc' : 'Làm lại EPDS',
              ),
            ),
          ],
        ),
      ),
      const SizedBox(height: 24),
      const Text(
        'Lịch sử sàng lọc',
        style: TextStyle(
          fontFamily: 'Lexend',
          fontSize: 18,
          fontWeight: FontWeight.w700,
        ),
      ),
      const SizedBox(height: 8),
      if (_history.isEmpty)
        const Text(
          'Chưa có lần sàng lọc nào.',
          style: TextStyle(fontFamily: 'Lexend'),
        )
      else
        ..._history.map(
          (item) => Card(
            child: ListTile(
              onTap: () => _openHistoryDetail(item),
              leading: CircleAvatar(
                backgroundColor: const Color(0xFFF2EAE4),
                child: Text(
                  item.valueNumeric.toStringAsFixed(0),
                  style: const TextStyle(
                    color: _primary,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
              title: Text(
                _level(item.valueNumeric.round()),
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontWeight: FontWeight.w600,
                ),
              ),
              subtitle: Text(
                '${item.measuredAt.day.toString().padLeft(2, '0')}/${item.measuredAt.month.toString().padLeft(2, '0')}/${item.measuredAt.year}',
                style: const TextStyle(fontFamily: 'Lexend'),
              ),
              trailing: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  if (item.valueSecondary != null && item.valueSecondary! > 0)
                    const Padding(
                      padding: EdgeInsets.only(right: 6),
                      child: Icon(
                        Icons.warning_amber_rounded,
                        color: Color(0xFFBA1A1A),
                      ),
                    ),
                  const Icon(Icons.chevron_right_rounded),
                ],
              ),
            ),
          ),
        ),
    ],
  );
}
