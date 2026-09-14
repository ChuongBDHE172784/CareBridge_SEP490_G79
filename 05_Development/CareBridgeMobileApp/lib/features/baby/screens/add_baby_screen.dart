import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:uuid/uuid.dart';
import '../models/baby_model.dart';
import '../services/baby_service.dart';
import '../../../core/auth/auth_state.dart';
import '../../../core/network/api_client.dart';
import '../../journey/models/journey_model.dart';
import '../../journey/services/journey_service.dart';

/// Add Baby Profile screen — UC-31
/// Collects nickname, birthDate, gender, birthWeightKg, birthLengthCm.
/// Calls POST /api/v1/babies on submit.
enum AddBabyEntryPoint {
  onboarding,
  profileList,
  liveBirthTransition;

  bool get returnsHome => this == AddBabyEntryPoint.onboarding;
  bool get returnsJourney => this == AddBabyEntryPoint.liveBirthTransition;
}

class AddBabyRouteArgs {
  const AddBabyRouteArgs({
    required this.entryPoint,
    this.journeyId,
    this.journeyVersion,
  });

  final AddBabyEntryPoint entryPoint;
  final String? journeyId;
  final int? journeyVersion;
}

typedef PregnancyOutcomeRecordCallback =
    Future<void> Function({
      required String journeyId,
      required int journeyVersion,
      required DateTime birthDate,
    });

class AddBabyScreen extends StatefulWidget {
  final AddBabyEntryPoint entryPoint;
  final BabyService? service;
  final JourneyService? journeyService;
  final String? journeyId;
  final int? journeyVersion;
  final PregnancyOutcomeRecordCallback? recordOutcome;
  final String? Function()? accountIdProvider;
  final String? Function()? accessTokenProvider;

  const AddBabyScreen({
    super.key,
    this.entryPoint = AddBabyEntryPoint.profileList,
    this.service,
    this.journeyService,
    this.journeyId,
    this.journeyVersion,
    this.recordOutcome,
    this.accountIdProvider,
    this.accessTokenProvider,
  });

  @override
  State<AddBabyScreen> createState() => _AddBabyScreenState();
}

class _AddBabyScreenState extends State<AddBabyScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _onSurface = Color(0xFF3D2E28);
  static const _onSurfaceVariant = Color(0xFF7A655C);
  static const _outlineVariant = Color(0xFFF0E4DD);
  static const _surfaceContainerLow = Color(0xFFFAF4EE);
  static const _surfaceVariant = Color(0xFFF2EAE4);
  static const _error = Color(0xFFBA1A1A);
  static const _errorContainer = Color(0xFFFFDAD6);

  final _formKey = GlobalKey<FormState>();
  final _nicknameCtrl = TextEditingController();
  final _dateCtrl = TextEditingController();
  final _weightCtrl = TextEditingController();
  final _lengthCtrl = TextEditingController();

  DateTime? _birthDate;
  BabyGender _gender = BabyGender.male;
  bool _loading = false;
  bool _deferring = false;
  String? _errorMsg;

  late final BabyService _service;
  late final JourneyService _journeyService;
  late final String? _requestAccountId;

  @override
  void initState() {
    super.initState();
    _service = widget.service ?? BabyService();
    _journeyService = widget.journeyService ?? JourneyService();
    _requestAccountId = _currentAccountId;
  }

  String? get _currentAccountId =>
      widget.accountIdProvider?.call() ?? AuthState.instance.userId;

  String? get _currentAccessToken =>
      widget.accessTokenProvider?.call() ?? AuthState.instance.accessToken;

  bool get _isCurrentAccount =>
      _requestAccountId != null && _requestAccountId == _currentAccountId;

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
    if (_loading || _deferring) return;
    final requestAccountId = _requestAccountId;
    if (requestAccountId == null ||
        requestAccountId.isEmpty ||
        !_isCurrentAccount) {
      setState(
        () => _errorMsg =
            'Phiên đăng nhập đã thay đổi. Vui lòng mở lại biểu mẫu.',
      );
      return;
    }
    final requestToken = _currentAccessToken;
    if (requestToken == null || requestToken.isEmpty || !_isCurrentAccount) {
      setState(
        () => _errorMsg =
            'Phiên đăng nhập đã thay đổi. Vui lòng mở lại biểu mẫu.',
      );
      return;
    }
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
    try {
      await _service.createBabyProfile(
        CreateBabyRequest(
          nickname: _nicknameCtrl.text.trim(),
          birthDate: _formatDate(_birthDate!),
          gender: _gender,
          birthWeightKg: birthWeightKg,
          birthLengthCm: birthLengthCm,
        ),
        token: requestToken,
        expectedAccountId: requestAccountId,
      );
      if (!mounted || !_isCurrentAccount) return;

      if (widget.entryPoint.returnsJourney) {
        await _recordLiveBirthOutcome(_birthDate!);
        if (!mounted || !_isCurrentAccount) return;

        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Đã tạo hồ sơ bé thành công.')),
        );
        context.go('/mother-home?tab=1');
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
      if (mounted && _isCurrentAccount) {
        setState(() => _errorMsg = _formatCreateBabyError(e));
      }
    } catch (_) {
      if (mounted && _isCurrentAccount) {
        setState(() => _errorMsg = 'Lỗi kết nối. Vui lòng thử lại.');
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _recordLiveBirthOutcome(DateTime birthDate) async {
    try {
      if (widget.recordOutcome != null) {
        await widget.recordOutcome!(
          journeyId: widget.journeyId ?? '',
          journeyVersion: widget.journeyVersion ?? 0,
          birthDate: birthDate,
        );
        return;
      }

      String? journeyId = widget.journeyId;
      int? journeyVersion = widget.journeyVersion;

      if (journeyId == null || journeyVersion == null) {
        final dashboard = await _journeyService.getDashboard();
        journeyId = dashboard.journeyId;
        journeyVersion = dashboard.version;
      }

      if (journeyId != null && journeyVersion != null) {
        await _journeyService.recordPregnancyOutcome(
          journeyId,
          RecordPregnancyOutcomeRequest(
            submissionId: const Uuid().v4(),
            expectedJourneyVersion: journeyVersion,
            outcomeType: PregnancyOutcome.liveBirth,
            outcomeDate: birthDate,
            source: 'SELF_REPORTED',
            reason: 'MOTHER_OUTCOME_CONFIRMATION',
            effectiveAt: DateTime.now().toUtc(),
            correction: false,
          ),
        );
      }
    } catch (_) {
      // Best-effort outcome recording; profile creation already succeeded.
    }
  }

  void _defer() {
    if (_loading || _deferring || !widget.entryPoint.returnsJourney) return;
    setState(() => _deferring = true);
    context.go('/mother-home?tab=1');
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
    return PopScope(
      canPop: !_loading && !_deferring,
      child: Scaffold(
        backgroundColor: _canvas,
        body: SafeArea(
          child: Column(
            children: [
              _buildAppBar(),
              Expanded(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.fromLTRB(20, 8, 20, 32),
                  child: Form(
                    key: _formKey,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        if (_errorMsg != null) ...[
                          _buildErrorBanner(),
                          const SizedBox(height: 16),
                        ],
                        _buildHeaderCard(),
                        const SizedBox(height: 20),
                        _buildSection(
                          'Thông tin cơ bản',
                          Icons.badge_outlined,
                          [
                            _buildNicknameField(),
                            const SizedBox(height: 16),
                            _buildDateField(),
                            const SizedBox(height: 16),
                            _buildGenderField(),
                          ],
                        ),
                        const SizedBox(height: 20),
                        _buildSection(
                          'Chỉ số lúc sinh',
                          Icons.monitor_weight_outlined,
                          [
                            _buildWeightField(),
                            const SizedBox(height: 16),
                            _buildLengthField(),
                          ],
                        ),
                        const SizedBox(height: 32),
                        _buildSubmitButton(),
                        if (widget.entryPoint.returnsJourney) ...[
                          const SizedBox(height: 12),
                          _buildDeferButton(),
                        ],
                      ],
                    ),
                  ),
                ),
              ),
            ],
          ),
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
            onPressed: _loading || _deferring
                ? null
                : () => Navigator.of(context).pop(),
            icon: const Icon(Icons.arrow_back, color: _onSurface),
          ),
          const Expanded(
            child: Text(
              'Thêm hồ sơ bé',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 18,
                fontWeight: FontWeight.w700,
                color: _onSurface,
              ),
            ),
          ),
          const SizedBox(width: 48),
        ],
      ),
    );
  }

  Widget _buildHeaderCard() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Colors.white, Color(0xFFFAF4EE)],
        ),
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: _outlineVariant),
        boxShadow: const [
          BoxShadow(
            color: Color(0x08000000),
            blurRadius: 16,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: _surfaceVariant,
                    borderRadius: BorderRadius.circular(99),
                  ),
                  child: const Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.child_care_rounded, size: 14, color: _primary),
                      SizedBox(width: 4),
                      Text(
                        'Hồ sơ của bé',
                        style: TextStyle(
                          fontFamily: 'Lexend',
                          fontSize: 11,
                          fontWeight: FontWeight.w600,
                          color: _primary,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 10),
                const Text(
                  'Chào đón thiên thần nhỏ',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 18,
                    fontWeight: FontWeight.w700,
                    color: _onSurface,
                  ),
                ),
                const SizedBox(height: 4),
                const Text(
                  'Điền thông tin để CareBridge đồng hành cùng hành trình phát triển của bé.',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 13,
                    color: _onSurfaceVariant,
                    height: 1.4,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 14),
          Container(
            width: 68,
            height: 68,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: _surfaceContainerLow,
              border: Border.all(color: _outlineVariant, width: 2),
              boxShadow: [
                BoxShadow(
                  color: const Color(0xFF5A463F).withValues(alpha: 0.08),
                  blurRadius: 12,
                  offset: const Offset(0, 4),
                ),
              ],
            ),
            child: const Center(
              child: Icon(
                Icons.child_care_rounded,
                size: 36,
                color: _primary,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildErrorBanner() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: _errorContainer,
        borderRadius: BorderRadius.circular(16),
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

  Widget _buildSection(String title, IconData icon, List<Widget> children) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Icon(icon, size: 18, color: _primary),
            const SizedBox(width: 8),
            Text(
              title,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 16,
                fontWeight: FontWeight.w700,
                color: _onSurface,
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(24),
            border: Border.all(color: _outlineVariant),
            boxShadow: const [
              BoxShadow(
                color: Color(0x08000000),
                blurRadius: 16,
                offset: Offset(0, 4),
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
      key: const Key('add-baby-nickname'),
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
      key: const Key('add-baby-birth-date'),
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
        const Row(
          children: [
            Icon(Icons.wc_outlined, size: 20, color: _primary),
            SizedBox(width: 8),
            Text(
              'Giới tính',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                fontWeight: FontWeight.w600,
                color: _onSurface,
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        Row(
          children: [BabyGender.male, BabyGender.female].map((g) {
            final selected = _gender == g;
            final label = g.displayLabel;
            final icon =
                g == BabyGender.male ? Icons.male_rounded : Icons.female_rounded;
            return Expanded(
              child: Padding(
                padding: EdgeInsets.only(right: g == BabyGender.male ? 10 : 0),
                child: Material(
                  color: Colors.transparent,
                  child: InkWell(
                    onTap: () => setState(() => _gender = g),
                    borderRadius: BorderRadius.circular(16),
                    child: AnimatedContainer(
                      duration: const Duration(milliseconds: 180),
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      decoration: BoxDecoration(
                        color: selected ? _primary : _surfaceContainerLow,
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(
                          color: selected ? _primary : _outlineVariant,
                          width: selected ? 1.5 : 1,
                        ),
                        boxShadow: selected
                            ? [
                                BoxShadow(
                                  color: _primary.withValues(alpha: 0.2),
                                  blurRadius: 8,
                                  offset: const Offset(0, 3),
                                ),
                              ]
                            : null,
                      ),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(
                            icon,
                            size: 22,
                            color: selected ? Colors.white : _onSurfaceVariant,
                          ),
                          const SizedBox(width: 8),
                          Text(
                            label,
                            style: TextStyle(
                              fontFamily: 'Lexend',
                              fontSize: 14,
                              fontWeight:
                                  selected ? FontWeight.w700 : FontWeight.w600,
                              color: selected ? Colors.white : _onSurfaceVariant,
                            ),
                          ),
                        ],
                      ),
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
      key: const Key('add-baby-birth-weight'),
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
      key: const Key('add-baby-birth-length'),
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
    return Container(
      width: double.infinity,
      height: 54,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(27),
        boxShadow: [
          BoxShadow(
            color: _primary.withValues(alpha: 0.2),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: FilledButton(
        key: const Key('add-baby-submit'),
        onPressed: _loading || _deferring ? null : _submit,
        style: FilledButton.styleFrom(
          backgroundColor: _primary,
          disabledBackgroundColor: _primary.withValues(alpha: 0.4),
          shape: const StadiumBorder(),
          elevation: 0,
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
                  fontWeight: FontWeight.w700,
                  color: Colors.white,
                ),
              ),
      ),
    );
  }

  Widget _buildDeferButton() {
    return SizedBox(
      width: double.infinity,
      height: 50,
      child: OutlinedButton(
        key: const Key('add-baby-defer'),
        onPressed: _loading || _deferring ? null : _defer,
        style: OutlinedButton.styleFrom(
          foregroundColor: _primary,
          side: const BorderSide(color: _outlineVariant, width: 1.2),
          shape: const StadiumBorder(),
        ),
        child: _deferring
            ? const SizedBox(
                width: 22,
                height: 22,
                child: CircularProgressIndicator(strokeWidth: 2.5),
              )
            : const Text(
                'Để sau',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 15,
                  fontWeight: FontWeight.w600,
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
        fontSize: 14,
        color: _onSurfaceVariant,
      ),
      hintStyle: TextStyle(
        fontFamily: 'Lexend',
        fontSize: 14,
        color: _onSurfaceVariant.withValues(alpha: 0.5),
      ),
      prefixIcon: Icon(icon, color: _primary, size: 20),
      suffixText: suffix,
      suffixStyle: const TextStyle(
        fontFamily: 'Lexend',
        fontWeight: FontWeight.w600,
        color: _onSurfaceVariant,
      ),
      filled: true,
      fillColor: _surfaceContainerLow,
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: const BorderSide(color: _outlineVariant),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: const BorderSide(color: _outlineVariant),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: const BorderSide(color: _primary, width: 1.5),
      ),
      errorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: const BorderSide(color: _error),
      ),
      focusedErrorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: const BorderSide(color: _error, width: 1.5),
      ),
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 15),
    );
  }
}
