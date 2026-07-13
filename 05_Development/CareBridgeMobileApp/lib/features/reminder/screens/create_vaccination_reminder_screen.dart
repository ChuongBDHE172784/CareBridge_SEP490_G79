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
  static const _error = Color(0xFFBA1A1A);

  final _reminderService = ReminderService.instance;
  final _babyService = BabyService();
  final _vaccineNameController = TextEditingController();
  final _locationController = TextEditingController();

  List<BabyProfile> _babies = [];
  BabyProfile? _selectedBaby;
  DateTime _scheduledDate = DateTime.now().add(const Duration(days: 3));
  List<Map<String, dynamic>> _suggestions = [];
  bool _loadingBabies = true;
  bool _loadingSuggestions = false;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _loadBabies();
  }

  @override
  void dispose() {
    _vaccineNameController.dispose();
    _locationController.dispose();
    super.dispose();
  }

  Future<void> _loadBabies() async {
    try {
      final babies = await _babyService.listBabyProfiles();
      if (!mounted) return;
      setState(() {
        _babies = babies;
        _selectedBaby = babies.isEmpty ? null : babies.first;
      });
      if (babies.isNotEmpty) {
        await _loadSuggestions(babies.first.id);
      }
    } finally {
      if (mounted) setState(() => _loadingBabies = false);
    }
  }

  Future<void> _loadSuggestions(String babyId) async {
    setState(() => _loadingSuggestions = true);
    try {
      final suggestions = await _reminderService.getVaccinationSuggestions(
        babyId,
      );
      if (!mounted) return;
      setState(() => _suggestions = suggestions);
    } catch (_) {
      if (mounted) setState(() => _suggestions = []);
    } finally {
      if (mounted) setState(() => _loadingSuggestions = false);
    }
  }

  Future<void> _pickDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _scheduledDate,
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 365 * 3)),
    );
    if (picked != null) setState(() => _scheduledDate = picked);
  }

  Future<void> _save() async {
    if (_selectedBaby == null) {
      _showError('Vui lòng chọn bé.');
      return;
    }
    final vaccineName = _vaccineNameController.text.trim();
    if (vaccineName.isEmpty) {
      _showError('Vui lòng nhập tên vắc xin.');
      return;
    }

    final location = _locationController.text.trim();
    final title = location.isEmpty ? vaccineName : '$vaccineName - $location';
    setState(() => _saving = true);
    try {
      await _reminderService.createVaccinationReminder(
        babyId: _selectedBaby!.id,
        title: title,
        scheduledAt: _scheduledDate,
        recurrenceType: RecurrenceType.none,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Đã tạo nhắc lịch tiêm chủng.')),
      );
      Navigator.pop(context, true);
    } catch (e) {
      if (!mounted) return;
      _showError('Không thể lưu nhắc lịch tiêm chủng: $e');
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  void _showError(String message) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message), backgroundColor: _error));
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
          onPressed: () => Navigator.pop(context),
        ),
        title: const Text(
          'Nhắc lịch tiêm chủng',
          style: TextStyle(
            fontFamily: 'Lexend',
            color: _onSurface,
            fontWeight: FontWeight.w800,
          ),
        ),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 28),
        children: [
          _buildBabySelector(),
          const SizedBox(height: 14),
          _Section(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                TextField(
                  controller: _vaccineNameController,
                  maxLength: 255,
                  decoration: _inputDecoration('Tên vắc xin *'),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: _locationController,
                  maxLength: 120,
                  decoration: _inputDecoration('Địa điểm tiêm chủng'),
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),
          _Section(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                _DateButton(
                  label: 'Ngày tiêm dự kiến',
                  value: _formatDate(_scheduledDate),
                  onTap: _pickDate,
                ),
                const SizedBox(height: 12),
                const Text(
                  'Gợi ý bên dưới được lấy từ dữ liệu/lịch tiêm đã ghi nhận trong hệ thống. Vui lòng kiểm tra lại với nhân viên y tế khi cần.',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    color: _onSurfaceVariant,
                    height: 1.45,
                  ),
                ),
              ],
            ),
          ),
          if (_loadingSuggestions || _suggestions.isNotEmpty) ...[
            const SizedBox(height: 14),
            _buildSuggestions(),
          ],
          const SizedBox(height: 20),
          FilledButton.icon(
            onPressed: _saving ? null : _save,
            icon: _saving
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(
                      color: Colors.white,
                      strokeWidth: 2,
                    ),
                  )
                : const Icon(Icons.save_rounded),
            label: const Text(
              'Lưu nhắc lịch',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontWeight: FontWeight.w800,
              ),
            ),
            style: FilledButton.styleFrom(
              backgroundColor: _primary,
              minimumSize: const Size.fromHeight(54),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBabySelector() {
    if (_loadingBabies) {
      return const SizedBox(
        height: 84,
        child: Center(child: CircularProgressIndicator(color: _primary)),
      );
    }
    if (_babies.isEmpty) {
      return const _Section(
        child: Text(
          'Chưa có hồ sơ bé. Vui lòng thêm hồ sơ bé trước khi tạo nhắc lịch tiêm chủng.',
          textAlign: TextAlign.center,
          style: TextStyle(fontFamily: 'Lexend', color: _onSurfaceVariant),
        ),
      );
    }
    return SizedBox(
      height: 86,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: _babies.length,
        separatorBuilder: (_, _) => const SizedBox(width: 10),
        itemBuilder: (_, index) {
          final baby = _babies[index];
          final selected = _selectedBaby?.id == baby.id;
          return GestureDetector(
            onTap: () {
              setState(() => _selectedBaby = baby);
              _loadSuggestions(baby.id);
            },
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 180),
              width: 82,
              decoration: BoxDecoration(
                color: selected ? _primary : Colors.white,
                borderRadius: BorderRadius.circular(18),
                border: Border.all(color: selected ? _primary : _surface),
              ),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    Icons.child_care_rounded,
                    color: selected ? Colors.white : _primaryContainer,
                  ),
                  const SizedBox(height: 4),
                  Text(
                    baby.nickname,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontFamily: 'Lexend',
                      fontSize: 12,
                      fontWeight: FontWeight.w700,
                      color: selected ? Colors.white : _onSurface,
                    ),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _buildSuggestions() {
    return _Section(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Gợi ý lịch tiêm đã ghi nhận',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 16,
              fontWeight: FontWeight.w800,
              color: _onSurface,
            ),
          ),
          const SizedBox(height: 10),
          if (_loadingSuggestions)
            const Center(child: CircularProgressIndicator(color: _primary))
          else
            ..._suggestions.take(3).map(_buildSuggestionTile),
        ],
      ),
    );
  }

  Widget _buildSuggestionTile(Map<String, dynamic> suggestion) {
    final name = suggestion['vaccineName']?.toString() ?? 'Vắc xin';
    final date = suggestion['suggestedDate']?.toString();
    return InkWell(
      onTap: () => setState(() => _vaccineNameController.text = name),
      borderRadius: BorderRadius.circular(14),
      child: Container(
        margin: const EdgeInsets.only(bottom: 8),
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: _surface,
          borderRadius: BorderRadius.circular(14),
        ),
        child: Row(
          children: [
            const Icon(Icons.vaccines_rounded, color: _primaryContainer),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    name,
                    style: const TextStyle(
                      fontFamily: 'Lexend',
                      fontWeight: FontWeight.w800,
                      color: _onSurface,
                    ),
                  ),
                  if (date != null)
                    Text(
                      date,
                      style: const TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 12,
                        color: _onSurfaceVariant,
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

  InputDecoration _inputDecoration(String label) => InputDecoration(
    labelText: label,
    labelStyle: const TextStyle(fontFamily: 'Lexend', color: _onSurfaceVariant),
    filled: true,
    fillColor: Colors.white,
    border: OutlineInputBorder(borderRadius: BorderRadius.circular(14)),
    enabledBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: BorderSide(color: _primaryContainer.withAlpha(70)),
    ),
    focusedBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(14),
      borderSide: const BorderSide(color: _primary, width: 1.5),
    ),
  );

  static String _formatDate(DateTime value) {
    return '${value.day.toString().padLeft(2, '0')}/${value.month.toString().padLeft(2, '0')}/${value.year}';
  }
}

class _Section extends StatelessWidget {
  final Widget child;

  const _Section({required this.child});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(18),
        boxShadow: const [
          BoxShadow(
            color: Color(0x12000000),
            blurRadius: 14,
            offset: Offset(0, 6),
          ),
        ],
      ),
      child: child,
    );
  }
}

class _DateButton extends StatelessWidget {
  final String label;
  final String value;
  final VoidCallback onTap;

  const _DateButton({
    required this.label,
    required this.value,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return OutlinedButton.icon(
      onPressed: onTap,
      icon: const Icon(Icons.calendar_month_rounded),
      label: Row(
        children: [
          Text(label, style: const TextStyle(fontFamily: 'Lexend')),
          const Spacer(),
          Text(
            value,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontWeight: FontWeight.w700,
            ),
          ),
        ],
      ),
      style: OutlinedButton.styleFrom(
        foregroundColor: const Color(0xFF845143),
        side: const BorderSide(color: Color(0xFFC98C7B)),
        minimumSize: const Size.fromHeight(50),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      ),
    );
  }
}
