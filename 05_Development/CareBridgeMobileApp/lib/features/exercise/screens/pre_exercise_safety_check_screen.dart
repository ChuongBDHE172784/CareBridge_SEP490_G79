import 'package:flutter/material.dart';

class PreExerciseSafetyCheckScreen extends StatefulWidget {
  final String exerciseName;
  final String trimester;
  final int durationMinutes;

  const PreExerciseSafetyCheckScreen({
    super.key,
    this.exerciseName = 'Yoga bầu thư giãn',
    this.trimester = 'Tam cá nguyệt thứ 2',
    this.durationMinutes = 20,
  });

  @override
  State<PreExerciseSafetyCheckScreen> createState() => _PreExerciseSafetyCheckScreenState();
}

class _PreExerciseSafetyCheckScreenState extends State<PreExerciseSafetyCheckScreen> {
  static const _bgColor = Color(0xFFFFF8F6);
  static const _primaryColor = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);
  static const _surfaceContainerLowest = Color(0xFFFFFFFF);
  static const _surfaceContainerLow = Color(0xFFFFF1EC);
  static const _errorContainer = Color(0xFFFFDAD6);

  final _checks = [false, false, false, false];
  final _noteController = TextEditingController();

  final _checkLabels = [
    'Tôi không cảm thấy chóng mặt hay hoa mắt trong 24 giờ qua.',
    'Tôi không bị đau bụng hoặc gò tử cung bất thường.',
    'Tôi chưa bị ra máu hoặc rỉ ối.',
    'Tôi đã uống đủ nước và ăn nhẹ trước khi tập.',
  ];

  bool get _allChecked => _checks.every((c) => c);

  @override
  void dispose() { _noteController.dispose(); super.dispose(); }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _bgColor,
      body: SafeArea(
        child: Column(
          children: [
            _buildAppBar(),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.fromLTRB(24, 8, 24, 0),
                children: [
                  _buildExerciseInfo(),
                  const SizedBox(height: 16),
                  _buildSafetyBanner(),
                  const SizedBox(height: 16),
                  _buildHealthChecklist(),
                  const SizedBox(height: 16),
                  _buildNotesSection(),
                  const SizedBox(height: 24),
                ],
              ),
            ),
            _buildActionBar(),
          ],
        ),
      ),
    );
  }

  Widget _buildAppBar() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: SizedBox(
        height: 48,
        child: Row(
          children: [
            IconButton(onPressed: () => Navigator.of(context).pop(), icon: const Icon(Icons.arrow_back, color: _primaryColor)),
            const Expanded(child: Text('Kiểm tra an toàn', textAlign: TextAlign.center, style: TextStyle(fontFamily: 'Lexend', fontSize: 20, fontWeight: FontWeight.w600, color: _primaryColor))),
            IconButton(onPressed: () => Navigator.of(context).pop(), icon: const Icon(Icons.close, color: _onSurface)),
          ],
        ),
      ),
    );
  }

  Widget _buildExerciseInfo() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(color: _surfaceContainerLow, borderRadius: BorderRadius.circular(16)),
      child: Row(
        children: [
          Container(
            width: 48, height: 48,
            decoration: BoxDecoration(color: _surfaceContainerLowest, borderRadius: BorderRadius.circular(12)),
            child: const Icon(Icons.self_improvement, color: _primaryColor, size: 24),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(widget.exerciseName, style: const TextStyle(fontFamily: 'Lexend', fontSize: 16, fontWeight: FontWeight.w600, color: _onSurface)),
              const SizedBox(height: 2),
              Text('${widget.trimester} • ${widget.durationMinutes} phút', style: const TextStyle(fontFamily: 'Lexend', fontSize: 14, color: _onSurfaceVariant)),
            ]),
          ),
        ],
      ),
    );
  }

  Widget _buildSafetyBanner() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(color: _errorContainer.withValues(alpha: 0.4), borderRadius: BorderRadius.circular(16)),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.info_outline, color: _primaryColor, size: 20),
          const SizedBox(width: 12),
          const Expanded(
            child: Text('Để đảm bảo an toàn cho mẹ và bé, vui lòng trả lời trung thực các câu hỏi dưới đây trước khi bắt đầu bài tập.',
                style: TextStyle(fontFamily: 'Lexend', fontSize: 14, color: _onSurfaceVariant, height: 1.5)),
          ),
        ],
      ),
    );
  }

  Widget _buildHealthChecklist() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surfaceContainerLowest,
        borderRadius: BorderRadius.circular(16),
        boxShadow: const [BoxShadow(color: Color.fromRGBO(90, 70, 63, 0.06), blurRadius: 20, offset: Offset(0, 4))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Tình trạng sức khỏe hiện tại', style: TextStyle(fontFamily: 'Lexend', fontSize: 20, fontWeight: FontWeight.w600, color: _onSurface)),
          const SizedBox(height: 4),
          Container(height: 1, color: _outlineVariant),
          const SizedBox(height: 16),
          ...List.generate(_checkLabels.length, (i) => Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: InkWell(
              onTap: () => setState(() => _checks[i] = !_checks[i]),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    width: 24, height: 24, margin: const EdgeInsets.only(top: 2),
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      border: Border.all(color: _checks[i] ? _primaryContainer : _outlineVariant, width: 2),
                      color: _checks[i] ? _primaryContainer : Colors.transparent,
                    ),
                    child: _checks[i] ? const Icon(Icons.check, size: 14, color: Colors.white) : null,
                  ),
                  const SizedBox(width: 12),
                  Expanded(child: Text(_checkLabels[i], style: const TextStyle(fontFamily: 'Lexend', fontSize: 14, color: _onSurface, height: 1.5))),
                ],
              ),
            ),
          )),
        ],
      ),
    );
  }

  Widget _buildNotesSection() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surfaceContainerLowest,
        borderRadius: BorderRadius.circular(16),
        boxShadow: const [BoxShadow(color: Color.fromRGBO(90, 70, 63, 0.06), blurRadius: 20, offset: Offset(0, 4))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Ghi chú khác (Không bắt buộc)', style: TextStyle(fontFamily: 'Lexend', fontSize: 20, fontWeight: FontWeight.w600, color: _onSurface)),
          const SizedBox(height: 4),
          const Text('Có triệu chứng nào khác khiến bạn lo lắng không?', style: TextStyle(fontFamily: 'Lexend', fontSize: 14, color: _onSurfaceVariant)),
          const SizedBox(height: 12),
          TextField(
            controller: _noteController, maxLines: 3,
            style: const TextStyle(fontFamily: 'Lexend', fontSize: 14, color: _onSurface),
            decoration: InputDecoration(
              hintText: 'Ví dụ: Hơi đau lưng nhẹ...', hintStyle: TextStyle(fontFamily: 'Lexend', color: _outlineVariant),
              filled: true, fillColor: _surfaceContainerLowest,
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: BorderSide(color: _outlineVariant)),
              enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: BorderSide(color: _outlineVariant)),
              focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: BorderSide(color: _primaryContainer, width: 2)),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildActionBar() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 12, 24, 24),
      child: Row(
        children: [
          Expanded(
            child: SizedBox(
              height: 48,
              child: OutlinedButton(
                onPressed: () => Navigator.of(context).pop(),
                style: OutlinedButton.styleFrom(foregroundColor: _primaryColor, side: const BorderSide(color: _outlineVariant), shape: const StadiumBorder()),
                child: const Text('Quay lại', style: TextStyle(fontFamily: 'Lexend', fontSize: 16, fontWeight: FontWeight.w600)),
              ),
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            flex: 2,
            child: SizedBox(
              height: 48,
              child: FilledButton(
                onPressed: _allChecked ? () {
                  // TODO: navigate to exercise session (CB-153)
                  Navigator.of(context).pop(true);
                } : null,
                style: FilledButton.styleFrom(backgroundColor: _primaryContainer, disabledBackgroundColor: _primaryContainer.withValues(alpha: 0.4), shape: const StadiumBorder()),
                child: const Text('Bắt đầu buổi tập', style: TextStyle(fontFamily: 'Lexend', fontSize: 16, fontWeight: FontWeight.w600, color: Colors.white)),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
