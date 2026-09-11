import 'package:flutter/material.dart';
import '../models/milestone_model.dart';
import '../services/baby_log_service.dart';

class RecordMilestoneScreen extends StatefulWidget {
  final String babyId;

  const RecordMilestoneScreen({super.key, required this.babyId});

  @override
  State<RecordMilestoneScreen> createState() => _RecordMilestoneScreenState();
}

class _RecordMilestoneScreenState extends State<RecordMilestoneScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Color(0xFFF2EAE4);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  final _service = BabyLogService();
  final _noteCtrl = TextEditingController();

  MilestoneType? _selectedType;
  DateTime _achievedDate = DateTime.now();
  bool _isSaving = false;
  bool _showSuccess = false;

  @override
  void dispose() {
    _noteCtrl.dispose();
    super.dispose();
  }

  Future<void> _pickDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _achievedDate,
      firstDate: DateTime.now().subtract(const Duration(days: 365 * 3)),
      lastDate: DateTime.now(),
      builder: (ctx, child) => Theme(
        data: Theme.of(ctx).copyWith(
          colorScheme: const ColorScheme.light(
            primary: _primary,
            onPrimary: Colors.white,
            surface: _canvas,
          ),
        ),
        child: child!,
      ),
    );
    if (picked != null) setState(() => _achievedDate = picked);
  }

  Future<void> _save() async {
    if (_selectedType == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Vui lòng chọn loại mốc phát triển.'),
          backgroundColor: _primary,
        ),
      );
      return;
    }
    setState(() => _isSaving = true);
    try {
      await _service.addMilestone(
        widget.babyId,
        AddMilestoneRequest(
          milestoneType: _selectedType!,
          achievedDate: _achievedDate,
          note: _noteCtrl.text.trim().isEmpty ? null : _noteCtrl.text.trim(),
        ),
      );
      setState(() => _showSuccess = true);
      await Future.delayed(const Duration(seconds: 2));
      if (mounted) Navigator.of(context).pop(true);
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Không thể lưu. Vui lòng thử lại.'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      appBar: AppBar(
        backgroundColor: _canvas,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_rounded, color: _onSurface),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: const Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Mẹ ghi nhận',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 12,
                color: _onSurfaceVariant,
              ),
            ),
            Text(
              'Mốc phát triển mới',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 16,
                fontWeight: FontWeight.w700,
                color: _onSurface,
              ),
            ),
          ],
        ),
      ),
      body: Stack(
        children: [
          SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 40),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                _buildMilestoneGrid(),
                const SizedBox(height: 16),
                _buildDatePicker(),
                const SizedBox(height: 16),
                _buildNoteField(),
                const SizedBox(height: 24),
                _buildActionButtons(),
              ],
            ),
          ),
          if (_showSuccess) _buildSuccessToast(),
        ],
      ),
    );
  }

  Widget _buildMilestoneGrid() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: _primary.withAlpha(15),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Bé đã đạt được mốc gì?',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              fontWeight: FontWeight.w700,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 16),
          _buildChipRow([MilestoneType.roll, MilestoneType.crawl]),
          const SizedBox(height: 8),
          _buildChipRow([MilestoneType.walk, MilestoneType.talk]),
          const SizedBox(height: 8),
          _buildChipRow([MilestoneType.tooth, MilestoneType.solids]),
          const SizedBox(height: 8),
          _buildFullWidthChip(MilestoneType.other),
        ],
      ),
    );
  }

  Widget _buildChipRow(List<MilestoneType> types) {
    return Row(
      children: types
          .map(
            (t) => Expanded(
              child: Padding(
                padding: EdgeInsets.only(right: t == types.last ? 0 : 8),
                child: _buildMilestoneChip(t),
              ),
            ),
          )
          .toList(),
    );
  }

  Widget _buildFullWidthChip(MilestoneType type) {
    return _buildMilestoneChip(type, fullWidth: true);
  }

  Widget _buildMilestoneChip(MilestoneType type, {bool fullWidth = false}) {
    final selected = _selectedType == type;
    return GestureDetector(
      onTap: () => setState(() => _selectedType = type),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 180),
        width: fullWidth ? double.infinity : null,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        decoration: BoxDecoration(
          color: selected ? _primary : _surface,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: selected ? _primary : _surface, width: 1.5),
        ),
        child: Row(
          mainAxisAlignment: fullWidth
              ? MainAxisAlignment.center
              : MainAxisAlignment.center,
          children: [
            if (selected) ...[
              const Icon(
                Icons.check_circle_rounded,
                color: Colors.white,
                size: 16,
              ),
              const SizedBox(width: 6),
            ],
            Text(
              type.displayLabel,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                fontWeight: FontWeight.w600,
                color: selected ? Colors.white : _onSurface,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDatePicker() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: _primary.withAlpha(15),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: GestureDetector(
        onTap: _pickDate,
        child: Row(
          children: [
            Container(
              width: 48,
              height: 48,
              decoration: BoxDecoration(
                color: _surface,
                borderRadius: BorderRadius.circular(14),
              ),
              child: const Icon(
                Icons.calendar_today_rounded,
                color: _primary,
                size: 22,
              ),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Ngày đạt được',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 11,
                      color: _onSurfaceVariant,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    '${_achievedDate.day.toString().padLeft(2, '0')}/${_achievedDate.month.toString().padLeft(2, '0')}/${_achievedDate.year}',
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 15,
                      fontWeight: FontWeight.w600,
                      color: _onSurface,
                    ),
                  ),
                ],
              ),
            ),
            const Icon(Icons.chevron_right_rounded, color: _onSurfaceVariant),
          ],
        ),
      ),
    );
  }

  Widget _buildNoteField() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: _primary.withAlpha(15),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(
                Icons.favorite_border_rounded,
                size: 16,
                color: _primaryContainer,
              ),
              SizedBox(width: 8),
              Text(
                'Cảm xúc của Mẹ',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: _onSurface,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: _noteCtrl,
            maxLines: 4,
            maxLength: 1000,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 14,
              color: _onSurface,
            ),
            decoration: InputDecoration(
              hintText: 'Chia sẻ cảm xúc của mẹ khi bé đạt được mốc này...',
              hintStyle: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 13,
                color: Color(0xFFBBA9A4),
              ),
              filled: true,
              fillColor: const Color(0xFFFFF8F6),
              contentPadding: const EdgeInsets.all(14),
              counterText: '',
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(16),
                borderSide: const BorderSide(color: _surface, width: 2),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(16),
                borderSide: const BorderSide(color: _surface, width: 2),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(16),
                borderSide: const BorderSide(
                  color: _primaryContainer,
                  width: 2,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildActionButtons() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        ElevatedButton(
          onPressed: _isSaving ? null : _save,
          style: ElevatedButton.styleFrom(
            backgroundColor: _primary,
            foregroundColor: Colors.white,
            padding: const EdgeInsets.symmetric(vertical: 16),
            shape: const StadiumBorder(),
            elevation: 0,
          ),
          child: _isSaving
              ? const SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(
                    strokeWidth: 2.5,
                    color: Colors.white,
                  ),
                )
              : const Text(
                  'Lưu mốc phát triển',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                  ),
                ),
        ),
        const SizedBox(height: 12),
        OutlinedButton(
          onPressed: () => Navigator.of(context).pop(),
          style: OutlinedButton.styleFrom(
            foregroundColor: _onSurfaceVariant,
            backgroundColor: _surface,
            side: BorderSide.none,
            padding: const EdgeInsets.symmetric(vertical: 16),
            shape: const StadiumBorder(),
          ),
          child: const Text(
            'Hủy bỏ',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 16,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildSuccessToast() {
    return Positioned.fill(
      child: Container(
        color: Colors.black.withAlpha(100),
        child: Center(
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 20),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(24),
              boxShadow: [
                BoxShadow(color: Colors.black.withAlpha(40), blurRadius: 20),
              ],
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.star_rounded, color: _primary, size: 48),
                const SizedBox(height: 12),
                Text(
                  '${_selectedType?.displayLabel ?? 'Mốc'} đã được lưu! 🎉',
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                    color: _onSurface,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
