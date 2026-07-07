import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';
import 'risk_triage_result_screen.dart';

class SymptomIntakeScreen extends StatefulWidget {
  const SymptomIntakeScreen({super.key});

  @override
  State<SymptomIntakeScreen> createState() => _SymptomIntakeScreenState();
}

class _SymptomIntakeScreenState extends State<SymptomIntakeScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFF6F1EC); // ignored — bg is white per design
  static const _surface = Color(0xFFFFF8F6);
  static const _surfaceContainerLow = Color(0xFFFFF1EC);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);

  static const int _totalSteps = 5;

  // Step 1: who has symptoms
  static const _subjectOptions = [
    'Bé của tôi',
    'Mẹ (bản thân)',
    'Người thân khác',
  ];

  // Step 2: primary symptom (shown in CB-015 design)
  static const _primarySymptoms = [
    'Sốt cao (trên 38.5°C)',
    'Ho nhiều, khò khè',
    'Nôn trớ liên tục',
    'Tiêu chảy, phân bất thường',
    'Triệu chứng khác',
  ];

  // Step 3: duration
  static const _durations = [
    'Dưới 1 ngày',
    '1-3 ngày',
    '3-7 ngày',
    'Hơn 1 tuần',
  ];

  int _step = 2; // Start at step 2 per design
  int? _subjectIndex;
  int? _symptomIndex;
  int? _durationIndex;
  final _otherSymptomCtrl = TextEditingController();
  final _additionalCtrl = TextEditingController();
  bool _submitting = false;
  bool _showOtherInput = false;

  @override
  void dispose() {
    _otherSymptomCtrl.dispose();
    _additionalCtrl.dispose();
    super.dispose();
  }

  String _buildSymptomsText() {
    final parts = <String>[];
    if (_subjectIndex != null)
      parts.add('Đối tượng: ${_subjectOptions[_subjectIndex!]}');
    if (_symptomIndex != null) {
      final s = _symptomIndex == 4
          ? _otherSymptomCtrl.text.trim()
          : _primarySymptoms[_symptomIndex!];
      parts.add('Triệu chứng chính: $s');
    }
    if (_durationIndex != null)
      parts.add('Thời gian: ${_durations[_durationIndex!]}');
    if (_additionalCtrl.text.trim().isNotEmpty)
      parts.add('Chi tiết thêm: ${_additionalCtrl.text.trim()}');
    return parts.join('. ');
  }

  Future<void> _submit() async {
    if (_symptomIndex == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Vui lòng chọn triệu chứng chính')),
      );
      return;
    }
    final symptoms = _buildSymptomsText();
    if (symptoms.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Vui lòng mô tả triệu chứng')),
      );
      return;
    }
    setState(() => _submitting = true);
    try {
      final result = await apiPost('/api/v1/triage/intake', {
        'symptoms': symptoms,
      });
      final sessionId =
          (result['data'] as Map<String, dynamic>)['sessionId'] as String;
      if (mounted) {
        Navigator.of(context).pushReplacement(
          MaterialPageRoute(
            builder: (_) => RiskTriageResultScreen(sessionId: sessionId),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Lỗi: $e'), backgroundColor: Colors.red),
        );
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  void _nextStep() {
    if (_step < _totalSteps) {
      setState(() => _step++);
    } else {
      _submit();
    }
  }

  void _prevStep() {
    if (_step > 1) {
      setState(() => _step--);
    } else {
      Navigator.of(context).pop();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _surface,
      body: SafeArea(
        child: Column(
          children: [
            _buildTopBar(),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(24, 16, 24, 120),
                child: _buildStepContent(),
              ),
            ),
            _buildBottomAction(),
          ],
        ),
      ),
    );
  }

  Widget _buildTopBar() {
    return Container(
      color: _surface,
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 4),
      child: Row(
        children: [
          SizedBox(
            width: 48,
            height: 48,
            child: IconButton(
              padding: EdgeInsets.zero,
              icon: const Icon(Icons.arrow_back, color: _onSurfaceVariant),
              onPressed: _prevStep,
            ),
          ),
          const Spacer(),
          const Text(
            'Kiểm tra triệu chứng',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w600,
              color: _primary,
            ),
          ),
          const Spacer(),
          SizedBox(
            width: 48,
            height: 48,
            child: IconButton(
              padding: EdgeInsets.zero,
              icon: const Icon(Icons.help_outline, color: _onSurfaceVariant),
              onPressed: () => _showHelpDialog(),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStepContent() {
    switch (_step) {
      case 1:
        return _buildStep1();
      case 2:
        return _buildStep2();
      case 3:
        return _buildStep3();
      case 4:
        return _buildStep4();
      case 5:
        return _buildStep5();
      default:
        return _buildStep2();
    }
  }

  Widget _buildProgressBar() {
    return Row(
      children: List.generate(
        _totalSteps,
        (i) => Expanded(
          child: Container(
            height: 4,
            margin: EdgeInsets.only(right: i < _totalSteps - 1 ? 4 : 0),
            decoration: BoxDecoration(
              color: i < _step ? _primary : _outlineVariant,
              borderRadius: BorderRadius.circular(99),
            ),
          ),
        ),
      ),
    );
  }

  // Step 1: Who
  Widget _buildStep1() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildProgressBar(),
        const SizedBox(height: 24),
        const Text(
          'Ai đang có triệu chứng?',
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.w600,
            color: _onSurface,
            height: 1.3,
          ),
        ),
        const SizedBox(height: 8),
        const Text(
          'Chọn đối tượng để AI có thể phân tích phù hợp.',
          style: TextStyle(fontSize: 14, color: _onSurfaceVariant),
        ),
        const SizedBox(height: 24),
        ..._subjectOptions.asMap().entries.map(
          (e) => _OptionCard(
            label: e.value,
            selected: _subjectIndex == e.key,
            onTap: () => setState(() => _subjectIndex = e.key),
          ),
        ),
      ],
    );
  }

  // Step 2: Primary symptom (as shown in CB-015 design)
  Widget _buildStep2() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildProgressBar(),
        const SizedBox(height: 24),
        const Text(
          'Bé nhà bạn đang gặp triệu chứng gì nổi bật nhất?',
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.w600,
            color: _onSurface,
            height: 1.3,
          ),
        ),
        const SizedBox(height: 8),
        const Text(
          'Chọn một triệu chứng chính để AI có thể phân tích chính xác nhất.',
          style: TextStyle(fontSize: 14, color: _onSurfaceVariant),
        ),
        const SizedBox(height: 24),
        ..._primarySymptoms.asMap().entries.map(
          (e) => _OptionCard(
            label: e.value,
            selected: _symptomIndex == e.key,
            onTap: () {
              setState(() {
                _symptomIndex = e.key;
                _showOtherInput = e.key == 4;
              });
            },
          ),
        ),
        if (_showOtherInput) ...[
          const SizedBox(height: 8),
          TextField(
            controller: _otherSymptomCtrl,
            maxLines: 3,
            decoration: InputDecoration(
              hintText: 'Vui lòng mô tả ngắn gọn triệu chứng của bé...',
              hintStyle: const TextStyle(
                color: _onSurfaceVariant,
                fontSize: 14,
              ),
              filled: true,
              fillColor: _surface,
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: const BorderSide(color: _outlineVariant),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: const BorderSide(color: _primary),
              ),
              contentPadding: const EdgeInsets.all(16),
            ),
          ),
        ],
      ],
    );
  }

  // Step 3: Duration
  Widget _buildStep3() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildProgressBar(),
        const SizedBox(height: 24),
        const Text(
          'Triệu chứng xuất hiện bao lâu?',
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.w600,
            color: _onSurface,
            height: 1.3,
          ),
        ),
        const SizedBox(height: 8),
        const Text(
          'Thời gian giúp AI đánh giá mức độ nghiêm trọng.',
          style: TextStyle(fontSize: 14, color: _onSurfaceVariant),
        ),
        const SizedBox(height: 24),
        ..._durations.asMap().entries.map(
          (e) => _OptionCard(
            label: e.value,
            selected: _durationIndex == e.key,
            onTap: () => setState(() => _durationIndex = e.key),
          ),
        ),
      ],
    );
  }

  // Step 4: Additional details
  Widget _buildStep4() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildProgressBar(),
        const SizedBox(height: 24),
        const Text(
          'Bạn có muốn mô tả thêm không?',
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.w600,
            color: _onSurface,
            height: 1.3,
          ),
        ),
        const SizedBox(height: 8),
        const Text(
          'Thêm chi tiết giúp AI phân tích chính xác hơn. (Không bắt buộc)',
          style: TextStyle(fontSize: 14, color: _onSurfaceVariant),
        ),
        const SizedBox(height: 24),
        TextField(
          controller: _additionalCtrl,
          maxLines: 6,
          maxLength: 1000,
          decoration: InputDecoration(
            hintText:
                'Ví dụ: bé vẫn uống sữa được, không có phát ban, đã uống hạ sốt...',
            hintStyle: const TextStyle(color: _onSurfaceVariant, fontSize: 14),
            filled: true,
            fillColor: _surface,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: const BorderSide(color: _outlineVariant),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: const BorderSide(color: _primary),
            ),
            contentPadding: const EdgeInsets.all(16),
          ),
        ),
      ],
    );
  }

  // Step 5: Review & submit
  Widget _buildStep5() {
    final symptomLabel = _symptomIndex != null
        ? (_symptomIndex == 4
              ? _otherSymptomCtrl.text.trim()
              : _primarySymptoms[_symptomIndex!])
        : '—';
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildProgressBar(),
        const SizedBox(height: 24),
        const Text(
          'Xem lại thông tin',
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.w600,
            color: _onSurface,
            height: 1.3,
          ),
        ),
        const SizedBox(height: 8),
        const Text(
          'Kiểm tra lại trước khi gửi cho AI phân tích.',
          style: TextStyle(fontSize: 14, color: _onSurfaceVariant),
        ),
        const SizedBox(height: 24),
        _ReviewRow(
          label: 'Đối tượng',
          value: _subjectIndex != null ? _subjectOptions[_subjectIndex!] : '—',
        ),
        _ReviewRow(label: 'Triệu chứng chính', value: symptomLabel),
        _ReviewRow(
          label: 'Thời gian',
          value: _durationIndex != null ? _durations[_durationIndex!] : '—',
        ),
        if (_additionalCtrl.text.trim().isNotEmpty)
          _ReviewRow(
            label: 'Chi tiết thêm',
            value: _additionalCtrl.text.trim(),
          ),
      ],
    );
  }

  Widget _buildBottomAction() {
    final isLastStep = _step == _totalSteps;
    return Container(
      padding: const EdgeInsets.fromLTRB(24, 16, 24, 24),
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [Colors.transparent, Color(0xFFFFF8F6)],
        ),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Icon(
                Icons.psychology_alt_outlined,
                size: 16,
                color: _primary,
              ),
              const SizedBox(width: 6),
              Expanded(
                child: RichText(
                  text: const TextSpan(
                    style: TextStyle(fontSize: 12, color: _onSurfaceVariant),
                    children: [
                      TextSpan(
                        text: 'CareBridge AI hỗ trợ phân tích định hướng, ',
                      ),
                      TextSpan(
                        text: 'không',
                        style: TextStyle(
                          color: _onSurface,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      TextSpan(
                        text:
                            ' thay thế chẩn đoán y khoa chuyên nghiệp từ bác sĩ.',
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            height: 52,
            child: ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: _primaryContainer,
                foregroundColor: Colors.white,
                shape: const StadiumBorder(),
                elevation: 4,
                shadowColor: _primaryContainer.withValues(alpha: 0.4),
              ),
              onPressed: _submitting ? null : _nextStep,
              child: _submitting
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(
                        color: Colors.white,
                        strokeWidth: 2,
                      ),
                    )
                  : Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          isLastStep ? 'Gửi phân tích' : 'Tiếp theo',
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        const SizedBox(width: 8),
                        Icon(
                          isLastStep ? Icons.send : Icons.arrow_forward,
                          size: 18,
                        ),
                      ],
                    ),
            ),
          ),
        ],
      ),
    );
  }

  void _showHelpDialog() {
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Hướng dẫn'),
        content: const Text(
          'Hãy chọn triệu chứng nổi bật nhất của bé. AI sẽ đưa ra gợi ý định hướng để hỗ trợ bạn, không phải chẩn đoán y tế.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Đã hiểu'),
          ),
        ],
      ),
    );
  }
}

class _OptionCard extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;

  const _OptionCard({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        constraints: const BoxConstraints(minHeight: 56),
        margin: const EdgeInsets.only(bottom: 8),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
        decoration: BoxDecoration(
          color: selected ? const Color(0xFFFFF1EC) : const Color(0xFFFFF8F6),
          borderRadius: BorderRadius.circular(28),
          border: Border.all(
            color: selected ? const Color(0xFFC98C7B) : const Color(0xFFD6C2BD),
            width: selected ? 2 : 1,
          ),
          boxShadow: selected
              ? [
                  const BoxShadow(
                    color: Color(0x0A5A3F36),
                    blurRadius: 8,
                    offset: Offset(0, 4),
                  ),
                ]
              : null,
        ),
        child: Row(
          children: [
            Expanded(
              child: Text(
                label,
                style: TextStyle(
                  fontSize: 16,
                  color: selected
                      ? const Color(0xFF51271B)
                      : const Color(0xFF271812),
                  fontWeight: selected ? FontWeight.w500 : FontWeight.normal,
                ),
              ),
            ),
            AnimatedOpacity(
              opacity: selected ? 1 : 0,
              duration: const Duration(milliseconds: 200),
              child: const Icon(
                Icons.check_circle,
                color: Color(0xFF845143),
                size: 22,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ReviewRow extends StatelessWidget {
  final String label;
  final String value;

  const _ReviewRow({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF1EC),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: const TextStyle(
              fontSize: 12,
              color: Color(0xFF524440),
              fontWeight: FontWeight.w500,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            value,
            style: const TextStyle(
              fontSize: 15,
              color: Color(0xFF271812),
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }
}
