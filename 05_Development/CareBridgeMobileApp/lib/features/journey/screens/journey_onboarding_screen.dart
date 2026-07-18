import 'dart:math';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/network/api_client.dart';
import '../models/journey_onboarding_model.dart';
import '../services/journey_onboarding_draft_storage.dart';
import '../services/journey_onboarding_service.dart';

class JourneyOnboardingScreen extends StatefulWidget {
  const JourneyOnboardingScreen({super.key, this.service, this.draftStorage});

  final JourneyOnboardingService? service;
  final JourneyOnboardingDraftStorage? draftStorage;

  @override
  State<JourneyOnboardingScreen> createState() =>
      _JourneyOnboardingScreenState();
}

class _JourneyOnboardingScreenState extends State<JourneyOnboardingScreen> {
  static const _accent = Color(0xFFC98C7B);
  static const _background = Color(0xFFF6F1EC);
  static const _surface = Color(0xFFFFFFFF);
  static const _nestedSurface = Color(0xFFF2EAE4);
  static const _text = Color(0xFF5A463F);
  static const _muted = Color(0xFF9C857C);

  late final JourneyOnboardingService _service;
  late final JourneyOnboardingDraftStorage _draftStorage;
  final _formKey = GlobalKey<FormState>();

  LifecycleGoal? _goal;
  final Set<SupportPreference> _preferences = {};
  bool _consentAccepted = false;
  bool _loading = false;
  String? _error;
  late String _submissionId;
  int _currentStep = 0;
  Future<void> _draftWriteQueue = Future<void>.value();

  @override
  void initState() {
    super.initState();
    _service = widget.service ?? JourneyOnboardingService();
    _draftStorage = widget.draftStorage ?? JourneyOnboardingDraftStorage();
    _submissionId = _newUuid();
    _restore();
  }

  Future<void> _restore() async {
    try {
      final status = await _service.getStatus();
      if (!mounted) return;
      if (status.canStartJourney) {
        context.go('/mother-stage-selection');
        return;
      }
    } catch (_) {
      // Offline users can continue editing the account-scoped draft.
    }
    final draft = await _draftStorage.read();
    if (!mounted || draft == null) return;
    final submissionId = draft['submissionId'];
    final lifecycleGoal = draft['lifecycleGoal'];
    final rawPreferences = draft['preferences'];
    final preferenceValues = rawPreferences is List
        ? rawPreferences.whereType<String>().toSet()
        : <String>{};
    setState(() {
      if (submissionId is String && _isUuid(submissionId)) {
        _submissionId = submissionId;
      }
      _goal = LifecycleGoal.values
          .where(
            (value) =>
                lifecycleGoal is String && value.apiValue == lifecycleGoal,
          )
          .firstOrNull;
      _preferences.clear();
      _preferences.addAll(
        SupportPreference.values.where(
          (value) => preferenceValues.contains(value.apiValue),
        ),
      );
      // Explicit consent is never restored from a cached checkbox.
      _consentAccepted = false;
    });
  }

  Future<void> _saveDraft() {
    final snapshot = <String, dynamic>{
      'submissionId': _submissionId,
      'lifecycleGoal': _goal?.apiValue,
      'preferences': _preferences.map((value) => value.apiValue).toList(),
    };
    _draftWriteQueue = _draftWriteQueue
        .catchError((_) {})
        .then((_) => _draftStorage.write(snapshot));
    return _draftWriteQueue;
  }

  Future<void> _continue() async {
    if (_currentStep == 0) {
      if (!_formKey.currentState!.validate()) return;
      await _saveDraft();
      if (!mounted) return;
      setState(() {
        _currentStep = 1;
        _error = null;
      });
      return;
    }
    if (!_consentAccepted) {
      setState(() => _error = 'Vui lòng đọc và đồng ý trước khi tiếp tục.');
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
    });
    await _saveDraft();
    try {
      final status = await _service.submit(
        JourneyOnboardingRequest(
          submissionId: _submissionId,
          lifecycleGoal: _goal!,
          locale: 'vi-VN',
          timeZone: 'Asia/Ho_Chi_Minh',
          preferences: _preferences.toList(),
          consentAccepted: _consentAccepted,
        ),
      );
      if (!mounted) return;
      if (!status.canStartJourney) {
        setState(() => _error = 'Không thể xác nhận đồng ý. Vui lòng thử lại.');
        return;
      }
      await _draftStorage.clear();
      if (mounted) context.go('/mother-stage-selection');
    } on ApiException catch (exception) {
      if (!mounted) return;
      setState(() {
        _error = exception.statusCode >= 500
            ? 'Dịch vụ đang tạm gián đoạn. Dữ liệu đã được giữ để bạn thử lại.'
            : 'Thông tin chưa hợp lệ. Vui lòng kiểm tra lại.';
      });
    } catch (_) {
      if (mounted) {
        setState(() => _error = 'Không thể kết nối. Dữ liệu đã được giữ lại.');
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _background,
      body: SafeArea(
        child: Form(
          key: _formKey,
          child: ListView(
            padding: const EdgeInsets.fromLTRB(24, 24, 24, 140),
            children: [
              _buildStepper(),
              const SizedBox(height: 28),
              const Text(
                'Thiết lập hỗ trợ ban đầu',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 30,
                  fontWeight: FontWeight.w800,
                  color: _text,
                ),
              ),
              const SizedBox(height: 10),
              const Text(
                'Chỉ cung cấp thông tin tối thiểu để CareBridge cá nhân hóa hành trình của bạn.',
                style: TextStyle(fontSize: 16, height: 1.5, color: _muted),
              ),
              const SizedBox(height: 24),
              if (_currentStep == 0) ...[
                _section(
                  title: 'Mục tiêu hiện tại',
                  child: DropdownButtonFormField<LifecycleGoal>(
                    key: const Key('baseline-goal'),
                    initialValue: _goal,
                    decoration: _inputDecoration('Chọn một mục tiêu'),
                    items: const [
                      DropdownMenuItem(
                        value: LifecycleGoal.preparingForPregnancy,
                        child: Text('Chuẩn bị mang thai'),
                      ),
                      DropdownMenuItem(
                        value: LifecycleGoal.currentlyPregnant,
                        child: Text('Đang mang thai'),
                      ),
                      DropdownMenuItem(
                        value: LifecycleGoal.postpartumRecovery,
                        child: Text('Phục hồi sau sinh'),
                      ),
                    ],
                    onChanged: (value) {
                      setState(() => _goal = value);
                      _saveDraft();
                    },
                    validator: (value) => value == null
                        ? 'Vui lòng chọn mục tiêu hiện tại.'
                        : null,
                  ),
                ),
                const SizedBox(height: 18),
                _section(
                  title: 'Bạn muốn nhận hỗ trợ về',
                  child: FormField<Set<SupportPreference>>(
                    initialValue: _preferences,
                    validator: (_) => _preferences.isEmpty
                        ? 'Chọn ít nhất một nội dung hỗ trợ.'
                        : null,
                    builder: (field) => Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Wrap(
                          spacing: 10,
                          runSpacing: 10,
                          children: SupportPreference.values.map((preference) {
                            final selected = _preferences.contains(preference);
                            return FilterChip(
                              key: Key('preference-${preference.apiValue}'),
                              label: Text(_preferenceLabel(preference)),
                              selected: selected,
                              onSelected: (value) {
                                setState(() {
                                  value
                                      ? _preferences.add(preference)
                                      : _preferences.remove(preference);
                                });
                                field.didChange(_preferences);
                                _saveDraft();
                              },
                              selectedColor: _accent,
                              checkmarkColor: Colors.white,
                              labelStyle: TextStyle(
                                color: selected ? Colors.white : _text,
                                fontWeight: FontWeight.w700,
                              ),
                              shape: const StadiumBorder(),
                            );
                          }).toList(),
                        ),
                        if (field.hasError) ...[
                          const SizedBox(height: 8),
                          Text(
                            field.errorText!,
                            style: const TextStyle(color: Color(0xFF93000A)),
                          ),
                        ],
                      ],
                    ),
                  ),
                ),
              ] else ...[
                const SizedBox(height: 18),
                _section(
                  title: 'Đồng ý xử lý dữ liệu hành trình',
                  child: Semantics(
                    container: true,
                    label: 'Đồng ý bắt buộc, chưa được chọn sẵn',
                    child: Material(
                      color: Colors.transparent,
                      child: CheckboxListTile(
                        key: const Key('lifecycle-consent'),
                        value: _consentAccepted,
                        onChanged: _loading
                            ? null
                            : (value) => setState(() {
                                _consentAccepted = value == true;
                                _error = null;
                              }),
                        activeColor: _accent,
                        controlAffinity: ListTileControlAffinity.leading,
                        contentPadding: EdgeInsets.zero,
                        title: const Text(
                          'Tôi đồng ý cho CareBridge lưu thông tin nền và cá nhân hóa hành trình theo Chính sách MOTHER_LIFECYCLE_V1.',
                          style: TextStyle(
                            fontSize: 16,
                            height: 1.45,
                            color: _text,
                          ),
                        ),
                        subtitle: const Padding(
                          padding: EdgeInsets.only(top: 8),
                          child: Text(
                            'Bạn có thể thu hồi đồng ý. Lịch sử đã tạo sẽ không bị xóa hoặc thay đổi âm thầm.',
                            style: TextStyle(height: 1.4, color: _muted),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 12),
                Align(
                  alignment: Alignment.centerLeft,
                  child: TextButton.icon(
                    key: const Key('onboarding-back'),
                    onPressed: _loading
                        ? null
                        : () => setState(() {
                            _currentStep = 0;
                            _consentAccepted = false;
                            _error = null;
                          }),
                    icon: const Icon(Icons.arrow_back_rounded),
                    label: const Text('Quay lại thông tin nền'),
                    style: TextButton.styleFrom(foregroundColor: _text),
                  ),
                ),
              ],
              if (_error != null) ...[
                const SizedBox(height: 16),
                Semantics(
                  liveRegion: true,
                  child: Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: _nestedSurface,
                      borderRadius: BorderRadius.circular(18),
                      border: const Border(
                        left: BorderSide(color: _accent, width: 4),
                      ),
                    ),
                    child: Text(_error!, style: const TextStyle(color: _text)),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
      bottomNavigationBar: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 12, 24, 18),
          child: SizedBox(
            height: 56,
            child: FilledButton(
              key: const Key('onboarding-continue'),
              onPressed: _loading ? null : _continue,
              style: FilledButton.styleFrom(
                backgroundColor: _accent,
                shape: const StadiumBorder(),
              ),
              child: _loading
                  ? const CircularProgressIndicator(color: Colors.white)
                  : Text(
                      _currentStep == 0
                          ? 'Tiếp tục đến đồng ý'
                          : 'Xác nhận và chọn giai đoạn',
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildStepper() => Semantics(
    label: 'Bước ${_currentStep + 1} trên 3',
    child: Row(
      children: [
        _step('1', 'Thông tin', _currentStep == 0, _currentStep > 0),
        _line(),
        _step('2', 'Đồng ý', _currentStep == 1, false),
        _line(),
        _step('3', 'Giai đoạn', false, false),
      ],
    ),
  );

  Widget _step(String number, String label, bool active, bool complete) =>
      Expanded(
        child: Column(
          children: [
            Container(
              width: 36,
              height: 36,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: active
                    ? _accent
                    : (complete ? _text : const Color(0xFFE8DDD6)),
                shape: BoxShape.circle,
                boxShadow: active
                    ? const [
                        BoxShadow(color: Color(0x33C98C7B), blurRadius: 10),
                      ]
                    : null,
              ),
              child: Text(
                number,
                style: TextStyle(
                  color: active || complete ? Colors.white : _muted,
                  fontWeight: FontWeight.w800,
                ),
              ),
            ),
            const SizedBox(height: 6),
            Text(label, style: const TextStyle(fontSize: 13, color: _text)),
          ],
        ),
      );

  Widget _line() => Container(
    width: 22,
    height: 4,
    margin: const EdgeInsets.only(bottom: 24),
    decoration: BoxDecoration(
      color: const Color(0xFFE8DDD6),
      borderRadius: BorderRadius.circular(999),
    ),
  );

  Widget _section({required String title, required Widget child}) => Container(
    padding: const EdgeInsets.all(20),
    decoration: BoxDecoration(
      color: _surface,
      borderRadius: BorderRadius.circular(28),
      boxShadow: const [
        BoxShadow(
          color: Color(0x0F5A463F),
          blurRadius: 32,
          offset: Offset(0, 12),
        ),
      ],
    ),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: const TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.w800,
            color: _text,
          ),
        ),
        const SizedBox(height: 14),
        child,
      ],
    ),
  );

  InputDecoration _inputDecoration(String hint) => InputDecoration(
    hintText: hint,
    filled: true,
    fillColor: _background,
    border: OutlineInputBorder(
      borderRadius: BorderRadius.circular(16),
      borderSide: BorderSide.none,
    ),
    enabledBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(16),
      borderSide: const BorderSide(color: Color(0x00FFFFFF), width: 2),
    ),
    focusedBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(16),
      borderSide: const BorderSide(color: _accent, width: 2),
    ),
  );

  String _preferenceLabel(SupportPreference value) => switch (value) {
    SupportPreference.nutrition => 'Dinh dưỡng',
    SupportPreference.mentalWellbeing => 'Tinh thần',
    SupportPreference.physicalActivity => 'Vận động',
    SupportPreference.appointmentReminders => 'Nhắc lịch',
  };

  String _newUuid() {
    final random = Random.secure();
    final bytes = List<int>.generate(16, (_) => random.nextInt(256));
    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    final hex = bytes
        .map((value) => value.toRadixString(16).padLeft(2, '0'))
        .join();
    return '${hex.substring(0, 8)}-${hex.substring(8, 12)}-'
        '${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20)}';
  }

  bool _isUuid(String value) => RegExp(
    r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$',
  ).hasMatch(value);
}
