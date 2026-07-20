import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:uuid/uuid.dart';
import '../models/baby_model.dart';
import '../services/baby_service.dart';
import '../services/baby_create_intent_store.dart';
import '../../../core/network/api_client.dart';
import '../../../core/auth/auth_state.dart';
import '../../journey/services/journey_service.dart';

/// Add Baby Profile screen — UC-31
/// Collects nickname, birthDate, gender, birthWeightKg, birthLengthCm.
/// Calls POST /api/v1/babies on submit.
enum AddBabyEntryPoint {
  onboarding,
  profileList;

  bool get returnsHome => this == AddBabyEntryPoint.onboarding;
}

class AddBabyScreen extends StatefulWidget {
  final AddBabyEntryPoint entryPoint;
  final String? relatedJourneyId;
  final BabyService? service;
  final BabyCreateIntentStore? intentStore;
  final String? accountId;
  final Future<bool> Function(String journeyId)? eligibilityCheck;

  const AddBabyScreen({
    super.key,
    this.entryPoint = AddBabyEntryPoint.profileList,
    this.relatedJourneyId,
    this.service,
    this.intentStore,
    this.accountId,
    this.eligibilityCheck,
  });

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

  late final BabyService _service;
  late final BabyCreateIntentStore _intentStore;
  String? _submissionId;
  String? _accountId;
  int? _intentGeneration;
  bool _restoringIntent = false;
  bool? _linkEligible;

  @override
  void initState() {
    super.initState();
    _service = widget.service ?? BabyService();
    _intentStore = widget.intentStore ?? BabyCreateIntentStore();
    _accountId = widget.accountId ?? AuthState.instance.userId;
    if (_accountId != null) {
      _intentGeneration = _intentStore.generationFor(_accountId!);
    }
    _submissionId = widget.relatedJourneyId == null ? null : const Uuid().v4();
    if (widget.relatedJourneyId != null) _restoreLinkIntent();
  }

  Future<void> _restoreLinkIntent() async {
    final accountId = _accountId;
    final journeyId = widget.relatedJourneyId;
    if (accountId == null || journeyId == null) {
      if (mounted) setState(() => _linkEligible = false);
      return;
    }
    setState(() => _restoringIntent = true);
    try {
      final eligible = widget.eligibilityCheck != null
          ? await widget.eligibilityCheck!(journeyId)
          : await _serverEligibility(journeyId);
      if (!mounted ||
          accountId != (widget.accountId ?? AuthState.instance.userId)) {
        return;
      }
      if (!eligible) {
        setState(() => _linkEligible = false);
        return;
      }
      final intent = await _intentStore.read(accountId, journeyId);
      if (!mounted ||
          accountId != (widget.accountId ?? AuthState.instance.userId)) {
        return;
      }
      if (intent != null) {
        _submissionId = intent.submissionId;
        _nicknameCtrl.text = intent.nickname;
        _birthDate = DateTime.tryParse(intent.birthDate);
        if (_birthDate != null) _dateCtrl.text = _displayDate(_birthDate!);
        _gender = intent.gender;
        _weightCtrl.text = intent.birthWeightKg?.toString() ?? '';
        _lengthCtrl.text = intent.birthLengthCm?.toString() ?? '';
      }
      setState(() => _linkEligible = true);
    } catch (_) {
      if (mounted) setState(() => _linkEligible = false);
    } finally {
      if (mounted) setState(() => _restoringIntent = false);
    }
  }

  Future<bool> _serverEligibility(String journeyId) async {
    final dashboard = await JourneyService().getDashboard();
    return dashboard.babyActionsEligible && dashboard.journeyId == journeyId;
  }

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
    if (widget.relatedJourneyId != null && _linkEligible != true) return;
    if (!_formKey.currentState!.validate()) return;
    if (_birthDate == null) {
      setState(() => _errorMsg = 'Vui lòng chọn ngày sinh của bé.');
      return;
    }
    final birthWeightKg = _parseOptionalDecimal(_weightCtrl.text);
    if (_weightCtrl.text.trim().isNotEmpty &&
        !_isValidBirthWeight(birthWeightKg)) {
      setState(
        () => _errorMsg = 'Cân nặng lúc sinh phải trong khoảng 0.5–10 kg.',
      );
      return;
    }
    final birthLengthCm = _parseOptionalDecimal(_lengthCtrl.text);
    if (_lengthCtrl.text.trim().isNotEmpty &&
        !_isValidBirthLength(birthLengthCm)) {
      setState(
        () => _errorMsg = 'Chiều dài lúc sinh phải trong khoảng 20–100 cm.',
      );
      return;
    }
    setState(() {
      _loading = true;
      _errorMsg = null;
    });
    final accountId = _accountId;
    final journeyId = widget.relatedJourneyId;
    var submissionId = _submissionId;
    if (accountId != null && journeyId != null && submissionId != null) {
      var intent = BabyCreateIntent(
        submissionId: submissionId,
        nickname: _nicknameCtrl.text.trim(),
        birthDate: _formatDate(_birthDate!),
        gender: _gender,
        birthWeightKg: birthWeightKg,
        birthLengthCm: birthLengthCm,
      );
      final persisted = await _intentStore.read(accountId, journeyId);
      if (persisted != null && !persisted.hasSamePayload(intent)) {
        submissionId = const Uuid().v4();
        _submissionId = submissionId;
        intent = BabyCreateIntent(
          submissionId: submissionId,
          nickname: intent.nickname,
          birthDate: intent.birthDate,
          gender: intent.gender,
          birthWeightKg: intent.birthWeightKg,
          birthLengthCm: intent.birthLengthCm,
        );
      }
      await _intentStore.write(
        accountId,
        journeyId,
        intent,
        expectedGeneration: _intentGeneration,
      );
    }
    try {
      await _service.createBabyProfile(
        CreateBabyRequest(
          nickname: _nicknameCtrl.text.trim(),
          birthDate: _formatDate(_birthDate!),
          gender: _gender,
          birthWeightKg: birthWeightKg,
          birthLengthCm: birthLengthCm,
          relatedJourneyId: widget.relatedJourneyId,
          submissionId: submissionId,
        ),
      );
      if (!mounted) return;
      if (accountId != null && journeyId != null) {
        await _intentStore.clear(accountId, journeyId);
      }
      if (!mounted ||
          accountId != (widget.accountId ?? AuthState.instance.userId)) {
        return;
      }

      if (widget.entryPoint.returnsHome) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Đã tạo hồ sơ bé thành công.')),
        );
        context.go('/');
        return;
      }

      final navigator = Navigator.of(context);
      if (navigator.canPop()) {
        navigator.pop(true);
        return;
      }

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Đã tạo hồ sơ bé thành công.')),
      );
      context.go('/babies');
    } on ApiException catch (e) {
      setState(() => _errorMsg = _formatCreateBabyError(e));
    } catch (_) {
      setState(() => _errorMsg = 'Lỗi kết nối. Vui lòng thử lại.');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  String _formatCreateBabyError(ApiException error) {
    try {
      final decoded = jsonDecode(error.message);
      if (decoded is Map<String, dynamic>) {
        final details = decoded['details'];
        if (details is List && details.isNotEmpty) {
          final first = details.first;
          if (first is Map<String, dynamic>) {
            final field = first['field']?.toString();
            switch (field) {
              case 'birthWeightKg':
                return 'Cân nặng lúc sinh phải trong khoảng 0.5–10 kg.';
              case 'birthLengthCm':
                return 'Chiều dài lúc sinh phải trong khoảng 20–100 cm.';
              case 'birthDate':
                return 'Ngày sinh không hợp lệ. Vui lòng chọn ngày hôm nay hoặc trước đó.';
              case 'nickname':
                return 'Tên bé không được để trống và tối đa 100 ký tự.';
            }
            final message = first['message']?.toString();
            if (message != null && message.isNotEmpty) {
              return 'Tạo hồ sơ thất bại: $message';
            }
          }
        }
        final message = decoded['message']?.toString();
        if (message != null && message.isNotEmpty) {
          return 'Tạo hồ sơ thất bại: $message';
        }
      }
    } catch (_) {
      // Fall back to a generic message when the server body is not JSON.
    }
    return 'Tạo hồ sơ thất bại (${error.statusCode}).';
  }

  double? _parseOptionalDecimal(String raw) {
    final normalized = raw.trim().replaceAll(',', '.');
    if (normalized.isEmpty) return null;
    return double.tryParse(normalized);
  }

  bool _isValidBirthWeight(double? value) {
    if (value == null) return false;
    return value >= 0.5 && value <= 10;
  }

  bool _isValidBirthLength(double? value) {
    if (value == null) return false;
    return value >= 20 && value <= 100;
  }

  @override
  Widget build(BuildContext context) {
    if (_restoringIntent) {
      return const Scaffold(
        backgroundColor: _canvas,
        body: Center(child: CircularProgressIndicator()),
      );
    }
    if (widget.relatedJourneyId != null && _linkEligible == false) {
      return Scaffold(
        backgroundColor: _canvas,
        appBar: AppBar(backgroundColor: _canvas),
        body: const Center(
          child: Padding(
            padding: EdgeInsets.all(24),
            child: Text(
              'Không thể tạo và liên kết hồ sơ bé cho hành trình này. Vui lòng quay lại bảng điều khiển để làm mới trạng thái.',
              style: TextStyle(fontSize: 16, color: _onSurface),
            ),
          ),
        ),
      );
    }
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
        FilteringTextInputFormatter.allow(RegExp(r'^\d*[\.,]?\d*')),
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
        final n = _parseOptionalDecimal(v);
        if (!_isValidBirthWeight(n)) {
          return 'Cân nặng không hợp lệ (0.5–10 kg).';
        }
        return null;
      },
    );
  }

  Widget _buildLengthField() {
    return TextFormField(
      controller: _lengthCtrl,
      keyboardType: const TextInputType.numberWithOptions(decimal: true),
      inputFormatters: [
        FilteringTextInputFormatter.allow(RegExp(r'^\d*[\.,]?\d*')),
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
        final n = _parseOptionalDecimal(v);
        if (!_isValidBirthLength(n)) {
          return 'Chiều dài không hợp lệ (20–100 cm).';
        }
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
