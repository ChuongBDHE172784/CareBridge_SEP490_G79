import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../models/baby_model.dart';
import '../services/baby_service.dart';
import '../../../core/network/api_client.dart';

/// Add Baby Profile screen — UC-31
/// Collects nickname, birthDate, gender, birthWeightKg, birthLengthCm.
/// Calls POST /api/v1/babies on submit.
class AddBabyScreen extends StatefulWidget {
  const AddBabyScreen({super.key});

  @override
  State<AddBabyScreen> createState() => _AddBabyScreenState();
}

class _AddBabyScreenState extends State<AddBabyScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);
  static const _surfaceContainerLow = Color(0xFFFFF1EC);
  static const _surfaceVariant = Color(0xFFFADCD3);
  static const _error = Color(0xFFBA1A1A);
  static const _errorContainer = Color(0xFFFFDAD6);

  final _formKey = GlobalKey<FormState>();
  final _nicknameCtrl = TextEditingController();
  final _dateCtrl = TextEditingController();
  final _weightCtrl = TextEditingController();
  final _lengthCtrl = TextEditingController();

  DateTime? _birthDate;
  BabyGender _gender = BabyGender.unknown;
  bool _loading = false;
  String? _errorMsg;

  final _service = BabyService();

  @override
  void dispose() {
    _nicknameCtrl.dispose();
    _dateCtrl.dispose();
    _weightCtrl.dispose();
    _lengthCtrl.dispose();
    super.dispose();
  }

  String _formatDate(DateTime d) =>
      '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

  String _displayDate(DateTime d) =>
      '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';

  Future<void> _pickDate() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _birthDate ?? now,
      firstDate: DateTime(now.year - 10),
      lastDate: now,
      builder: (ctx, child) => Theme(
        data: Theme.of(ctx).copyWith(
          colorScheme: const ColorScheme.light(
            primary: _primaryContainer,
            onPrimary: Colors.white,
            surface: _canvas,
            onSurface: _onSurface,
          ),
        ),
        child: child!,
      ),
    );
    if (picked != null) {
      setState(() {
        _birthDate = picked;
        _dateCtrl.text = _displayDate(picked);
      });
    }
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    if (_birthDate == null) {
      setState(() => _errorMsg = 'Vui lòng chọn ngày sinh của bé.');
      return;
    }
    setState(() {
      _loading = true;
      _errorMsg = null;
    });
    try {
      await _service.createBabyProfile(
        CreateBabyRequest(
          nickname: _nicknameCtrl.text.trim(),
          birthDate: _formatDate(_birthDate!),
          gender: _gender,
          birthWeightKg: _weightCtrl.text.trim().isNotEmpty
              ? double.tryParse(_weightCtrl.text.trim())
              : null,
          birthLengthCm: _lengthCtrl.text.trim().isNotEmpty
              ? double.tryParse(_lengthCtrl.text.trim())
              : null,
        ),
      );
      if (!mounted) return;
      Navigator.pop(context);
    } on ApiException catch (e) {
      setState(() => _errorMsg = 'Tạo hồ sơ thất bại (${e.statusCode}).');
    } catch (_) {
      setState(() => _errorMsg = 'Lỗi kết nối. Vui lòng thử lại.');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: Column(
          children: [
            _buildAppBar(),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(24, 8, 24, 32),
                child: Form(
                  key: _formKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      if (_errorMsg != null) ...[
                        _buildErrorBanner(),
                        const SizedBox(height: 16),
                      ],
                      _buildSection('Thông tin cơ bản', [
                        _buildNicknameField(),
                        const SizedBox(height: 16),
                        _buildDateField(),
                        const SizedBox(height: 16),
                        _buildGenderField(),
                      ]),
                      const SizedBox(height: 24),
                      _buildSection('Chỉ số lúc sinh (tuỳ chọn)', [
                        _buildWeightField(),
                        const SizedBox(height: 16),
                        _buildLengthField(),
                      ]),
                      const SizedBox(height: 32),
                      _buildSubmitButton(),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAppBar() {
    return SizedBox(
      height: 56,
      child: Row(
        children: [
          IconButton(
            onPressed: () => Navigator.of(context).pop(),
            icon: const Icon(Icons.arrow_back, color: _primary),
          ),
          const Expanded(
            child: Text(
              'Thêm hồ sơ bé',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 20,
                fontWeight: FontWeight.w600,
                color: _primary,
              ),
            ),
          ),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildErrorBanner() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: _errorContainer,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          const Icon(Icons.error_outline, color: _error, size: 20),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              _errorMsg!,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                color: _error,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSection(String title, List<Widget> children) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 16,
            fontWeight: FontWeight.w600,
            color: _onSurface,
          ),
        ),
        const SizedBox(height: 12),
        Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(20),
            boxShadow: [
              BoxShadow(
                color: const Color(0xFF5A463F).withAlpha(13),
                blurRadius: 16,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: children,
          ),
        ),
      ],
    );
  }

  Widget _buildNicknameField() {
    return TextFormField(
      controller: _nicknameCtrl,
      textCapitalization: TextCapitalization.words,
      style: const TextStyle(
        fontFamily: 'Lexend',
        fontSize: 16,
        color: _onSurface,
      ),
      decoration: _inputDecoration(
        label: 'Tên bé',
        hint: 'VD: Sushi, Mochi...',
        icon: Icons.child_care_outlined,
      ),
      validator: (v) =>
          (v == null || v.trim().isEmpty) ? 'Vui lòng nhập tên bé.' : null,
    );
  }

  Widget _buildDateField() {
    return GestureDetector(
      onTap: _pickDate,
      child: AbsorbPointer(
        child: TextFormField(
          controller: _dateCtrl,
          readOnly: true,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 16,
            color: _onSurface,
          ),
          decoration: _inputDecoration(
            label: 'Ngày sinh',
            hint: 'DD/MM/YYYY',
            icon: Icons.cake_outlined,
          ),
          validator: (_) =>
              _birthDate == null ? 'Vui lòng chọn ngày sinh.' : null,
        ),
      ),
    );
  }

  Widget _buildGenderField() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            const Icon(Icons.wc_outlined, size: 20, color: _onSurfaceVariant),
            const SizedBox(width: 8),
            const Text(
              'Giới tính',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                color: _onSurfaceVariant,
              ),
            ),
          ],
        ),
        const SizedBox(height: 10),
        Row(
          children: BabyGender.values.map((g) {
            final selected = _gender == g;
            final label = g == BabyGender.unknown
                ? 'Chưa biết'
                : g.displayLabel;
            final icon = g == BabyGender.male
                ? Icons.male
                : g == BabyGender.female
                ? Icons.female
                : Icons.device_unknown_outlined;
            return Expanded(
              child: Padding(
                padding: EdgeInsets.only(
                  right: g != BabyGender.unknown ? 8 : 0,
                ),
                child: GestureDetector(
                  onTap: () => setState(() => _gender = g),
                  child: AnimatedContainer(
                    duration: const Duration(milliseconds: 150),
                    padding: const EdgeInsets.symmetric(vertical: 10),
                    decoration: BoxDecoration(
                      color: selected
                          ? _primaryContainer
                          : _surfaceVariant.withAlpha(80),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(
                        color: selected
                            ? _primaryContainer
                            : _outlineVariant.withAlpha(100),
                        width: selected ? 2 : 1,
                      ),
                    ),
                    child: Column(
                      children: [
                        Icon(
                          icon,
                          size: 22,
                          color: selected ? Colors.white : _onSurfaceVariant,
                        ),
                        const SizedBox(height: 4),
                        Text(
                          label,
                          style: TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 12,
                            fontWeight: FontWeight.w500,
                            color: selected ? Colors.white : _onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            );
          }).toList(),
        ),
      ],
    );
  }

  Widget _buildWeightField() {
    return TextFormField(
      controller: _weightCtrl,
      keyboardType: const TextInputType.numberWithOptions(decimal: true),
      inputFormatters: [
        FilteringTextInputFormatter.allow(RegExp(r'^\d*\.?\d*')),
      ],
      style: const TextStyle(
        fontFamily: 'Lexend',
        fontSize: 16,
        color: _onSurface,
      ),
      decoration: _inputDecoration(
        label: 'Cân nặng lúc sinh',
        hint: 'VD: 3.2',
        icon: Icons.monitor_weight_outlined,
        suffix: 'kg',
      ),
      validator: (v) {
        if (v == null || v.trim().isEmpty) return null;
        final n = double.tryParse(v.trim());
        if (n == null || n <= 0 || n > 10)
          return 'Cân nặng không hợp lệ (0–10 kg).';
        return null;
      },
    );
  }

  Widget _buildLengthField() {
    return TextFormField(
      controller: _lengthCtrl,
      keyboardType: const TextInputType.numberWithOptions(decimal: true),
      inputFormatters: [
        FilteringTextInputFormatter.allow(RegExp(r'^\d*\.?\d*')),
      ],
      style: const TextStyle(
        fontFamily: 'Lexend',
        fontSize: 16,
        color: _onSurface,
      ),
      decoration: _inputDecoration(
        label: 'Chiều dài lúc sinh',
        hint: 'VD: 50',
        icon: Icons.straighten_outlined,
        suffix: 'cm',
      ),
      validator: (v) {
        if (v == null || v.trim().isEmpty) return null;
        final n = double.tryParse(v.trim());
        if (n == null || n <= 0 || n > 80)
          return 'Chiều dài không hợp lệ (0–80 cm).';
        return null;
      },
    );
  }

  Widget _buildSubmitButton() {
    return SizedBox(
      width: double.infinity,
      height: 56,
      child: FilledButton(
        onPressed: _loading ? null : _submit,
        style: FilledButton.styleFrom(
          backgroundColor: _primaryContainer,
          disabledBackgroundColor: _primaryContainer.withAlpha(100),
          shape: const StadiumBorder(),
        ),
        child: _loading
            ? const SizedBox(
                width: 24,
                height: 24,
                child: CircularProgressIndicator(
                  color: Colors.white,
                  strokeWidth: 2.5,
                ),
              )
            : const Text(
                'Lưu hồ sơ bé',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: Colors.white,
                ),
              ),
      ),
    );
  }

  InputDecoration _inputDecoration({
    required String label,
    required String hint,
    required IconData icon,
    String? suffix,
  }) {
    return InputDecoration(
      labelText: label,
      hintText: hint,
      labelStyle: const TextStyle(
        fontFamily: 'Lexend',
        color: _onSurfaceVariant,
      ),
      hintStyle: const TextStyle(fontFamily: 'Lexend', color: _outlineVariant),
      prefixIcon: Icon(icon, color: _onSurfaceVariant, size: 20),
      suffixText: suffix,
      suffixStyle: const TextStyle(
        fontFamily: 'Lexend',
        color: _onSurfaceVariant,
      ),
      filled: true,
      fillColor: _surfaceContainerLow,
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: _outlineVariant),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: _outlineVariant),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: _primaryContainer, width: 2),
      ),
      errorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: _error),
      ),
      focusedErrorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: _error, width: 2),
      ),
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
    );
  }
}
