import 'package:flutter/material.dart';
import '../../baby/models/baby_model.dart';
import '../../baby/services/baby_service.dart';
import '../models/reminder_model.dart';
import '../services/reminder_service.dart';

class CreateVaccinationReminderScreen extends StatefulWidget {
  const CreateVaccinationReminderScreen({super.key});

  @override
  State<CreateVaccinationReminderScreen> createState() =>
      _CreateVaccinationReminderScreenState();
}

class _CreateVaccinationReminderScreenState
    extends State<CreateVaccinationReminderScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Color(0xFFF2EAE4);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  final _reminderService = ReminderService.instance;
  final _babyService = BabyService();
  final _vaccineNameCtrl = TextEditingController();
  final _locationCtrl = TextEditingController();

  List<BabyProfile> _babies = [];
  BabyProfile? _selectedBaby;
  DateTime _scheduledDate = DateTime.now().add(const Duration(days: 3));
  int _leadTimeDays = 3;
  List<Map<String, dynamic>> _suggestions = [];

  bool _isLoadingBabies = true;
  bool _isLoadingSuggestions = false;
  bool _isSaving = false;
  bool _showSuccess = false;

  static const _leadTimeOptions = [1, 3, 5, 7];

  @override
  void initState() {
    super.initState();
    _loadBabies();
  }

  @override
  void dispose() {
    _vaccineNameCtrl.dispose();
    _locationCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadBabies() async {
    try {
      final babies = await _babyService.listBabyProfiles();
      setState(() {
        _babies = babies;
        if (babies.isNotEmpty) {
          _selectedBaby = babies.first;
          _loadSuggestions(babies.first.id);
        }
      });
    } catch (_) {
      // Non-critical failure — babies list can be empty
    } finally {
      if (mounted) setState(() => _isLoadingBabies = false);
    }
  }

  Future<void> _loadSuggestions(String babyId) async {
    setState(() => _isLoadingSuggestions = true);
    try {
      final s = await _reminderService.getVaccinationSuggestions(babyId);
      setState(() => _suggestions = s);
    } catch (_) {
      setState(() => _suggestions = []);
    } finally {
      if (mounted) setState(() => _isLoadingSuggestions = false);
    }
  }

  Future<void> _pickDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _scheduledDate,
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 365 * 3)),
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
    if (picked != null) setState(() => _scheduledDate = picked);
  }

  Future<void> _save() async {
    if (_selectedBaby == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Vui lòng chọn bé.'),
          backgroundColor: _primary,
        ),
      );
      return;
    }
    if (_vaccineNameCtrl.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Vui lòng nhập tên vắc xin.'),
          backgroundColor: _primary,
        ),
      );
      return;
    }
    setState(() => _isSaving = true);
    try {
      await _reminderService.createVaccinationReminder(
        babyId: _selectedBaby!.id,
        title: _vaccineNameCtrl.text.trim(),
        scheduledAt: _scheduledDate,
        recurrenceType: RecurrenceType.none,
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
        title: const Text(
          'Đặt nhắc lịch tiêm',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 18,
            fontWeight: FontWeight.w700,
            color: _onSurface,
          ),
        ),
      ),
      body: Stack(
        children: [
          SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 40),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                _buildBabySelector(),
                const SizedBox(height: 16),
                _buildVaccineNameCard(),
                const SizedBox(height: 16),
                _buildScheduleCard(),
                const SizedBox(height: 16),
                _buildLocationCard(),
                const SizedBox(height: 16),
                if (_suggestions.isNotEmpty || _isLoadingSuggestions) ...[
                  _buildSuggestionsCard(),
                  const SizedBox(height: 16),
                ],
                _buildTipCard(),
                const SizedBox(height: 24),
                _buildSaveButton(),
              ],
            ),
          ),
          if (_showSuccess) _buildSuccessToast(),
        ],
      ),
    );
  }

  Widget _buildBabySelector() {
    if (_isLoadingBabies) {
      return const SizedBox(
        height: 80,
        child: Center(
          child: CircularProgressIndicator(color: _primaryContainer),
        ),
      );
    }
    if (_babies.isEmpty) {
      return Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: _surface,
          borderRadius: BorderRadius.circular(20),
        ),
        child: const Text(
          'Chưa có hồ sơ bé. Vui lòng thêm bé trước.',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 13,
            color: _onSurfaceVariant,
          ),
          textAlign: TextAlign.center,
        ),
      );
    }
    return SizedBox(
      height: 90,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: _babies.length + 1,
        separatorBuilder: (_, _) => const SizedBox(width: 10),
        itemBuilder: (_, i) {
          if (i == _babies.length) return _buildAddBabyChip();
          final b = _babies[i];
          final selected = _selectedBaby?.id == b.id;
          return GestureDetector(
            onTap: () {
              setState(() => _selectedBaby = b);
              _loadSuggestions(b.id);
            },
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 180),
              width: 72,
              decoration: BoxDecoration(
                color: selected ? _primary : Colors.white,
                borderRadius: BorderRadius.circular(20),
                border: Border.all(
                  color: selected ? _primary : _surface,
                  width: 2,
                ),
                boxShadow: selected
                    ? [BoxShadow(color: _primary.withAlpha(40), blurRadius: 8)]
                    : [],
              ),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    Icons.child_care_rounded,
                    color: selected ? Colors.white : _primaryContainer,
                    size: 28,
                  ),
                  const SizedBox(height: 4),
                  Text(
                    b.nickname,
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 10,
                      fontWeight: FontWeight.w600,
                      color: selected ? Colors.white : _onSurface,
                    ),
                    textAlign: TextAlign.center,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _buildAddBabyChip() {
    return Container(
      width: 72,
      decoration: BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: _surface, width: 2),
      ),
      child: const Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.add_circle_outline_rounded,
            color: _onSurfaceVariant,
            size: 26,
          ),
          SizedBox(height: 4),
          Text(
            'Thêm bé',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 10,
              color: _onSurfaceVariant,
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }

  Widget _buildVaccineNameCard() {
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
      child: Row(
        children: [
          Expanded(
            child: TextFormField(
              controller: _vaccineNameCtrl,
              maxLength: 255,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                color: _onSurface,
              ),
              decoration: _inputDeco(
                'Tên vắc xin *',
                hint: 'Cúm mùa, Thủy đậu...',
              ),
            ),
          ),
          const SizedBox(width: 8),
          GestureDetector(
            onTap: () {
              // AI suggestion — would trigger an API call for vaccine name suggestions
              if (_suggestions.isNotEmpty) {
                final first = _suggestions.first;
                _vaccineNameCtrl.text = first['vaccineName']?.toString() ?? '';
              }
            },
            child: Container(
              width: 44,
              height: 44,
              decoration: BoxDecoration(
                color: _primaryContainer.withAlpha(30),
                borderRadius: BorderRadius.circular(14),
              ),
              child: const Icon(
                Icons.auto_awesome_rounded,
                color: _primaryContainer,
                size: 22,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildScheduleCard() {
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
            'Lịch tiêm',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 14),
          GestureDetector(
            onTap: _pickDate,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
              decoration: BoxDecoration(
                color: _surface,
                borderRadius: BorderRadius.circular(16),
              ),
              child: Row(
                children: [
                  const Icon(
                    Icons.calendar_today_rounded,
                    color: _primary,
                    size: 18,
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Ngày tiêm dự kiến',
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 10,
                            color: _onSurfaceVariant,
                          ),
                        ),
                        Text(
                          '${_scheduledDate.day.toString().padLeft(2, '0')}/${_scheduledDate.month.toString().padLeft(2, '0')}/${_scheduledDate.year}',
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 14,
                            fontWeight: FontWeight.w600,
                            color: _onSurface,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const Icon(
                    Icons.chevron_right_rounded,
                    color: _onSurfaceVariant,
                    size: 18,
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),
          const Text(
            'Nhắc trước',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 12,
              color: _onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            children: _leadTimeOptions.map((d) {
              final selected = _leadTimeDays == d;
              return GestureDetector(
                onTap: () => setState(() => _leadTimeDays = d),
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 150),
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 8,
                  ),
                  decoration: BoxDecoration(
                    color: selected ? _primary : _surface,
                    borderRadius: BorderRadius.circular(50),
                  ),
                  child: Text(
                    '$d ngày',
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                      color: selected ? Colors.white : _onSurface,
                    ),
                  ),
                ),
              );
            }).toList(),
          ),
        ],
      ),
    );
  }

  Widget _buildLocationCard() {
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
      child: TextFormField(
        controller: _locationCtrl,
        style: const TextStyle(
          fontFamily: 'Lexend',
          fontSize: 14,
          color: _onSurface,
        ),
        decoration:
            _inputDeco(
              'Địa điểm tiêm chủng',
              hint: 'VNVC, phòng khám...',
            ).copyWith(
              prefixIcon: const Icon(
                Icons.location_on_rounded,
                color: _primaryContainer,
                size: 20,
              ),
            ),
      ),
    );
  }

  Widget _buildSuggestionsCard() {
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
                Icons.auto_awesome_rounded,
                color: _primaryContainer,
                size: 16,
              ),
              SizedBox(width: 8),
              Text(
                'Lịch tiêm tham khảo',
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
          if (_isLoadingSuggestions)
            const Center(
              child: Padding(
                padding: EdgeInsets.all(16),
                child: CircularProgressIndicator(color: _primaryContainer),
              ),
            )
          else
            ...(_suggestions.take(3).map((s) => _buildSuggestionTile(s))),
        ],
      ),
    );
  }

  Widget _buildSuggestionTile(Map<String, dynamic> s) {
    final name = s['vaccineName']?.toString() ?? 'Vắc xin';
    final date = s['suggestedDate']?.toString();
    return GestureDetector(
      onTap: () => setState(() => _vaccineNameCtrl.text = name),
      child: Container(
        margin: const EdgeInsets.only(bottom: 8),
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: _surface,
          borderRadius: BorderRadius.circular(14),
        ),
        child: Row(
          children: [
            const Icon(
              Icons.vaccines_rounded,
              color: _primaryContainer,
              size: 20,
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    name,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                      color: _onSurface,
                    ),
                  ),
                  if (date != null)
                    Text(
                      date,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 11,
                        color: _onSurfaceVariant,
                      ),
                    ),
                ],
              ),
            ),
            const Icon(
              Icons.chevron_right_rounded,
              color: _onSurfaceVariant,
              size: 16,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTipCard() {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [
            _primaryContainer.withAlpha(35),
            _primaryContainer.withAlpha(15),
          ],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(20),
      ),
      child: const Row(
        children: [
          Icon(
            Icons.lightbulb_outline_rounded,
            color: _primaryContainer,
            size: 20,
          ),
          SizedBox(width: 10),
          Expanded(
            child: Text(
              'Tiêm chủng đầy đủ giúp bé phòng ngừa các bệnh truyền nhiễm nguy hiểm. Hãy theo dõi lịch tiêm định kỳ.',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 11,
                color: _onSurfaceVariant,
                height: 1.5,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSaveButton() {
    return ElevatedButton(
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
              'Lưu nhắc lịch',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 16,
                fontWeight: FontWeight.w700,
              ),
            ),
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
            child: const Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(Icons.vaccines_rounded, color: _primary, size: 48),
                SizedBox(height: 12),
                Text(
                  'Đã lưu lịch tiêm!',
                  style: TextStyle(
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

  InputDecoration _inputDeco(String label, {String? hint}) => InputDecoration(
    labelText: label,
    hintText: hint,
    labelStyle: const TextStyle(
      fontFamily: 'Lexend',
      fontSize: 12,
      color: _onSurfaceVariant,
    ),
    hintStyle: const TextStyle(
      fontFamily: 'Lexend',
      fontSize: 13,
      color: Color(0xFFBBA9A4),
    ),
    filled: true,
    fillColor: Colors.white,
    counterText: '',
    contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
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
      borderSide: const BorderSide(color: _primaryContainer, width: 2),
    ),
  );
}
