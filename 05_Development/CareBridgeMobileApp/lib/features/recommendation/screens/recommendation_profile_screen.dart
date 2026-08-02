import 'dart:async';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:uuid/uuid.dart';

import '../../../core/auth/auth_state.dart';
import '../../../core/network/api_client.dart';
import '../../journey/services/journey_service.dart';
import '../models/recommendation_model.dart';
import '../services/recommendation_service.dart';

class RecommendationProfileScreen extends StatefulWidget {
  const RecommendationProfileScreen({
    super.key,
    this.service,
    this.journeyStage,
  });

  final RecommendationService? service;
  final String? journeyStage;

  @override
  State<RecommendationProfileScreen> createState() =>
      _RecommendationProfileScreenState();
}

class _RecommendationProfileScreenState
    extends State<RecommendationProfileScreen> {
  late final RecommendationService _service;
  final _height = TextEditingController();
  final _weight = TextEditingController();
  final _measuredOn = TextEditingController();
  Map<String, dynamic> _profile = RecommendationProfileDraft.empty();
  String _stage = 'PREGNANCY';
  String _submissionId = const Uuid().v4();
  String? _observedUserId;
  bool _loading = true;
  bool _consentDecisionMade = false;
  bool _saving = false;
  String? _error;
  int _loadGeneration = 0;
  bool _accountHasDateOfBirth = true;

  @override
  void initState() {
    super.initState();
    _service = widget.service ?? RecommendationService();
    _observedUserId = AuthState.instance.userId;
    AuthState.instance.addListener(_onAccountChanged);
    unawaited(_load());
  }

  @override
  void dispose() {
    _height.dispose();
    _weight.dispose();
    _measuredOn.dispose();
    AuthState.instance.removeListener(_onAccountChanged);
    super.dispose();
  }

  void _onAccountChanged() {
    final currentUserId = AuthState.instance.userId;
    final previousUserId = _observedUserId;
    if (currentUserId == previousUserId) return;
    _observedUserId = currentUserId;
    ++_loadGeneration;
    if (previousUserId != null) {
      unawaited(_service.clearDraftFor(previousUserId));
    }
    if (!mounted) return;
    setState(() {
      _profile = RecommendationProfileDraft.empty();
      _height.clear();
      _weight.clear();
      _measuredOn.clear();
      _loading = true;
      _consentDecisionMade = false;
      _error = null;
    });
    unawaited(_load());
  }

  Future<void> _load() async {
    final expectedUserId = AuthState.instance.userId;
    final generation = ++_loadGeneration;
    bool current() =>
        mounted &&
        generation == _loadGeneration &&
        AuthState.instance.userId == expectedUserId;
    if (mounted && AuthState.instance.userId == expectedUserId) {
      setState(() {
        _loading = true;
        _error = null;
      });
    }
    try {
      final dashboard = await JourneyService().getDashboard();
      if (!current()) return;
      if (dashboard.journeyType == 'BABY_CARE') {
        if (!mounted) return;
        context.go('/mother-home');
        return;
      }
      try {
        _accountHasDateOfBirth = await _service.accountHasDateOfBirth();
      } catch (_) {
        _accountHasDateOfBirth = false;
      }
      if (!current()) return;
      final response = await _service.getProfile();
      if (!current()) return;
      final draft = await _service.readDraft();
      if (!current()) return;
      _stage = dashboard.journeyType ?? widget.journeyStage ?? 'PREGNANCY';
      _profile = RecommendationProfileDraft.mergeProfiles(
        response.profile,
        draft,
      );
      if (!_accountHasDateOfBirth) {
        final age = _map('age');
        if (age['state'] == 'KNOWN') {
          age['state'] = 'UNKNOWN';
          _profile['age'] = age;
        }
      }
      _normalizeStageConditionedInputs();
      _syncBmiControllers();
      setState(() {
        _loading = false;
        _error = null;
      });
    } on ApiException catch (error) {
      if (current() && error.errorCode == 'RECOMMENDATION_JOURNEY_REQUIRED') {
        if (!mounted) return;
        context.go('/mother-stage-selection');
        return;
      }
      if (current()) {
        setState(() {
          _loading = false;
          _error = _messageForApi(
            error,
            fallback:
                'KhÃ´ng thá»ƒ táº£i thiáº¿t láº­p cÃ¡ nhÃ¢n hÃ³a. Vui lÃ²ng thá»­ láº¡i.',
          );
        });
      }
    } catch (_) {
      if (current()) {
        setState(() {
          _loading = false;
          _error = 'Không thể tải thiết lập cá nhân hóa. Vui lòng thử lại.';
        });
      }
    }
  }

  Map<String, dynamic> _safeMap(dynamic raw) {
    if (raw is! Map) return <String, dynamic>{};
    final result = <String, dynamic>{};
    raw.forEach((key, value) {
      if (key is String) result[key] = value;
    });
    return result;
  }

  List<String> _safeStrings(dynamic raw) {
    if (raw is! List) return const [];
    return raw.whereType<String>().toList(growable: false);
  }

  List<Map<String, dynamic>> _safeMapList(dynamic raw) {
    if (raw is! List) return const [];
    return raw.whereType<Map>().map(_safeMap).toList(growable: false);
  }

  String _safeOption(dynamic raw, List<String> options, {String? fallback}) {
    return raw is String && options.contains(raw)
        ? raw
        : fallback != null && options.contains(fallback)
        ? fallback
        : options.first;
  }

  String? _nullableOption(dynamic raw, List<String> options) {
    return raw is String && options.contains(raw) ? raw : null;
  }

  Map<String, dynamic> _map(String key) {
    final value = _safeMap(_profile[key]);
    if (!value.containsKey('state')) value['state'] = 'UNKNOWN';
    return value;
  }

  void _normalizeStageConditionedInputs() {
    final bmi = _map('bmi');
    if (bmi['state'] == 'KNOWN') {
      final allowed = switch (_stage) {
        'PRE_PREGNANCY' => const ['PRE_PREGNANCY', 'CURRENT_NON_PREGNANT'],
        'POSTPARTUM' => const ['PRE_PREGNANCY', 'CURRENT_POSTPARTUM'],
        _ => const ['PRE_PREGNANCY', 'CURRENT_PREGNANCY'],
      };
      final context = bmi['weightContext'];
      if (context is! String || !allowed.contains(context)) {
        // A prior-stage contextual measurement is not silently reclassified.
        // Require an explicit owner review before it can be submitted again.
        bmi['state'] = 'UNKNOWN';
        bmi.remove('heightCm');
        bmi.remove('weightKg');
        bmi.remove('weightContext');
        bmi.remove('measuredOn');
        _profile['bmi'] = bmi;
      }
    }
  }

  void _setDomain(String key, Map<String, dynamic> value) {
    _submissionId = const Uuid().v4();
    setState(() => _profile[key] = value);
    unawaited(_service.saveDraft(_profile));
  }

  void _setState(String key, String state, {bool allowNotApplicable = false}) {
    final value = _map(key);
    value['state'] = state;
    if (state != 'KNOWN') {
      value.remove('codes');
      value.remove('status');
      value.remove('infectionCodes');
      if (key == 'bmi') {
        value.remove('heightCm');
        value.remove('weightKg');
        value.remove('weightContext');
        value.remove('measuredOn');
      }
    } else if (key == 'reproductiveHistory') {
      final codes = _safeStrings(value['codes']);
      if (codes.isEmpty) {
        value.remove('codes');
      } else {
        value['codes'] = codes;
      }
    } else if (key == 'underlyingConditions') {
      final codes = _safeStrings(value['codes']);
      if (codes.isEmpty) {
        value.remove('codes');
      } else {
        value['codes'] = codes;
      }
    } else if (key == 'nutrition') {
      final codes = _safeStrings(value['codes']);
      if (codes.isEmpty) {
        value.remove('codes');
      } else {
        value['codes'] = codes;
      }
    } else if (key == 'currentMedications') {
      final codes = _safeStrings(value['codes']);
      if (codes.isEmpty) {
        value.remove('codes');
      } else {
        value['codes'] = codes;
      }
    } else if (key == 'sexualHealth') {
      final codes = _safeStrings(value['codes']);
      if (codes.isEmpty) {
        value.remove('codes');
      } else {
        value['codes'] = codes;
      }
    } else if (key == 'sti') {
      const statuses = {
        'NO_KNOWN_HISTORY',
        'SCREENING_INFORMATION',
        'PAST_HISTORY',
        'CURRENT_OR_UNDER_TREATMENT',
      };
      const infectionValues = {
        'HIV',
        'SYPHILIS',
        'HEPATITIS_B',
        'HEPATITIS_C',
        'CHLAMYDIA',
        'GONORRHEA',
        'HERPES',
        'HPV',
        'OTHER',
      };
      final status = value['status'];
      if (!statuses.contains(status)) {
        value.remove('status');
        value.remove('infectionCodes');
      } else if (status != 'PAST_HISTORY' &&
          status != 'CURRENT_OR_UNDER_TREATMENT') {
        value.remove('infectionCodes');
      } else {
        final infections = _safeStrings(
          value['infectionCodes'],
        ).where(infectionValues.contains).toSet().toList()..sort();
        if (infections.isEmpty) {
          value.remove('infectionCodes');
        } else {
          value['infectionCodes'] = infections.take(9).toList();
        }
      }
    } else if (key == 'bmi') {
      if (value['weightContext'] is! String) value.remove('weightContext');
      _syncBmiControllers(value);
    }
    _setDomain(key, value);
  }

  void _syncBmiControllers([Map<String, dynamic>? source]) {
    final bmi = source ?? _map('bmi');
    _height.text = bmi['heightCm']?.toString() ?? '';
    _weight.text = bmi['weightKg']?.toString() ?? '';
    _measuredOn.text = bmi['measuredOn']?.toString() ?? '';
  }

  void _updateBmiText() {
    final bmi = _map('bmi');
    final height = double.tryParse(_height.text);
    final weight = double.tryParse(_weight.text);
    if (height == null) {
      bmi.remove('heightCm');
    } else {
      bmi['heightCm'] = height;
    }
    if (weight == null) {
      bmi.remove('weightKg');
    } else {
      bmi['weightKg'] = weight;
    }
    final measuredOn = _measuredOn.text.trim();
    if (measuredOn.isEmpty) {
      bmi.remove('measuredOn');
    } else {
      bmi['measuredOn'] = measuredOn;
    }
    _setDomain('bmi', bmi);
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    if (_error != null) {
      return Scaffold(
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Focus(
                autofocus: true,
                child: Semantics(
                  liveRegion: true,
                  container: true,
                  label: _error!,
                  child: Text(_error!),
                ),
              ),
              const SizedBox(height: 16),
              FilledButton(onPressed: _load, child: const Text('Thử lại')),
            ],
          ),
        ),
      );
    }
    return Scaffold(
      appBar: AppBar(title: const Text('Hồ sơ nền cá nhân hóa')),
      body: SafeArea(
        child: _consentDecisionMade ? _buildForm() : _buildConsent(),
      ),
    );
  }

  Widget _buildConsent() => ListView(
    padding: const EdgeInsets.all(24),
    children: [
      const Icon(
        Icons.auto_awesome_rounded,
        size: 56,
        color: Color(0xFFC98C7B),
      ),
      const SizedBox(height: 16),
      const Text(
        'Nội dung phù hợp với hành trình của mẹ',
        style: TextStyle(fontSize: 24, fontWeight: FontWeight.w700),
      ),
      const SizedBox(height: 12),
      const Text(
        'Mẹ có thể chia sẻ thông tin nền theo từng mục để CareBridge chọn bài viết giáo dục an toàn, đúng thời điểm. Mẹ có thể bỏ qua hoặc không tiết lộ bất kỳ thông tin nhạy cảm nào.',
      ),
      const SizedBox(height: 12),
      const Text(
        'Đây là nội dung giáo dục, không thay thế chẩn đoán hoặc điều trị. Nhu cầu khẩn cấp luôn đi qua luồng an toàn hiện có.',
      ),
      const SizedBox(height: 28),
      FilledButton(
        onPressed: () => setState(() => _consentDecisionMade = true),
        child: const Text('Đồng ý và tiếp tục'),
      ),
      const SizedBox(height: 12),
      OutlinedButton(
        onPressed: _decline,
        child: const Text('Tiếp tục không cá nhân hóa'),
      ),
    ],
  );

  Widget _buildForm() => Form(
    child: ListView(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 32),
      children: [
        Text(
          'Giai đoạn: $_stage',
          style: const TextStyle(fontWeight: FontWeight.w600),
        ),
        const SizedBox(height: 8),
        _section('1. Tuổi', _ageSection()),
        _completionSummary(),
        _section('2. BMI và cân nặng nền', _bmiSection()),
        _section(
          '3. Tiền sử sinh sản',
          _codeSection('reproductiveHistory', 'Trạng thái', const [
            'NO_PRIOR_PREGNANCY',
            'PRIOR_LIVE_BIRTH',
            'PRIOR_PREGNANCY_LOSS',
            'PRIOR_STILLBIRTH',
            'PRIOR_PRETERM_BIRTH',
            'PRIOR_MULTIPLE_PREGNANCY',
            'OTHER_HISTORY',
          ]),
        ),
        _section(
          '4. Bệnh nền',
          _codeSection('underlyingConditions', 'Trạng thái', const [
            'NONE_KNOWN',
            'HYPERTENSION',
            'DIABETES',
            'THYROID_DISORDER',
            'CARDIOVASCULAR_DISEASE',
            'ASTHMA',
            'EPILEPSY',
            'KIDNEY_DISEASE',
            'AUTOIMMUNE_DISEASE',
            'MENTAL_HEALTH_CONDITION',
            'OTHER_CLINICIAN_CONFIRMED',
          ]),
        ),
        _section('5. Lối sống', _lifestyleSection()),
        _section(
          '6. Dinh dưỡng',
          _codeSection('nutrition', 'Trạng thái', const [
            'NO_CURRENT_CONCERN',
            'VEGETARIAN',
            'VEGAN',
            'FOOD_INSECURITY',
            'LOW_APPETITE',
            'NAUSEA_OR_VOMITING',
            'IRON_OR_FOLATE_CONCERN',
            'OTHER_NUTRITION_CONCERN',
          ]),
        ),
        _section('7. Tiêm chủng', _vaccinationSection()),
        _section(
          '8. Thuốc đang sử dụng',
          _codeSection('currentMedications', 'Trạng thái', const [
            'NONE',
            'PRENATAL_VITAMIN',
            'FOLIC_ACID',
            'IRON',
            'THYROID_MEDICATION',
            'DIABETES_MEDICATION',
            'ANTIHYPERTENSIVE',
            'ANTICOAGULANT',
            'ANTIEPILEPTIC',
            'MENTAL_HEALTH_MEDICATION',
            'OTHER_PRESCRIBED',
          ]),
        ),
        _section(
          '9. Sức khỏe tình dục',
          _codeSection('sexualHealth', 'Trạng thái', const [
            'NO_CURRENT_INFORMATION_NEED',
            'GENERAL_INFORMATION',
            'CONTRACEPTION_OR_FERTILITY',
            'INTIMACY_DURING_LIFECYCLE',
            'OTHER_NON_URGENT_INFORMATION',
          ], allowNotApplicable: true),
        ),
        _section('10. Sức khỏe tình dục và bệnh lây truyền', _stiSection()),
        if (_error != null)
          Focus(
            autofocus: true,
            child: Semantics(
              liveRegion: true,
              container: true,
              label: _error!,
              child: Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Text(_error!, style: const TextStyle(color: Colors.red)),
              ),
            ),
          ),
        const SizedBox(height: 12),
        FilledButton.icon(
          onPressed: _saving ? null : _submit,
          icon: const Icon(Icons.check),
          label: Text(_saving ? 'Đang lưu...' : 'Lưu hồ sơ và xem Home'),
        ),
      ],
    ),
  );

  Widget _section(String title, Widget child) => Card(
    margin: const EdgeInsets.only(bottom: 10),
    child: ExpansionTile(
      title: Text(title, style: const TextStyle(fontWeight: FontWeight.w600)),
      initiallyExpanded: true,
      childrenPadding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
      children: [child],
    ),
  );

  Widget _ageSection() => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      Text(
        _accountHasDateOfBirth
            ? 'Tuổi được suy ra từ ngày sinh trong hồ sơ tài khoản.'
            : 'Chưa có ngày sinh trong hồ sơ tài khoản; có thể chọn Không biết.',
      ),
      const SizedBox(height: 8),
      if (!_accountHasDateOfBirth)
        OutlinedButton.icon(
          onPressed: () => context.push('/mother-home?tab=4'),
          icon: const Icon(Icons.manage_accounts_outlined),
          label: const Text('Mở Hồ sơ để cập nhật ngày sinh'),
        ),
      _stateField(
        'age',
        'Trạng thái thông tin',
        allowKnown: _accountHasDateOfBirth,
      ),
    ],
  );

  Widget _completionSummary() {
    final domains = const [
      'age',
      'bmi',
      'reproductiveHistory',
      'underlyingConditions',
      'lifestyle',
      'nutrition',
      'vaccination',
      'currentMedications',
      'sexualHealth',
      'sti',
    ];
    final complete = domains.where((key) {
      final value = _map(key);
      if (key == 'lifestyle') {
        return [
          'smoking',
          'alcohol',
          'physicalActivity',
          'sleep',
        ].every((child) => _safeMap(value[child])['state'] is String);
      }
      if (key == 'vaccination') {
        return _safeMapList(value['answers']).length == 5;
      }
      return value['state'] is String;
    }).length;
    return Semantics(
      label: 'Đã hoàn tất $complete trên ${domains.length} mục',
      container: true,
      child: Card(
        color: const Color(0xFFFFF1EC),
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Text(
            'Tiến độ hồ sơ: $complete/${domains.length} mục đã có trạng thái',
            style: const TextStyle(fontWeight: FontWeight.w600),
          ),
        ),
      ),
    );
  }

  Widget _stateField(
    String key,
    String label, {
    bool allowNotApplicable = false,
    bool allowKnown = true,
  }) {
    final options = [
      if (allowKnown) 'KNOWN',
      'UNKNOWN',
      'PREFER_NOT_TO_SAY',
      if (allowNotApplicable) 'NOT_APPLICABLE',
    ];
    final state = _safeOption(_map(key)['state'], options, fallback: 'UNKNOWN');
    return DropdownButtonFormField<String>(
      initialValue: state,
      decoration: InputDecoration(labelText: label),
      items: options
          .map((value) => DropdownMenuItem(value: value, child: Text(value)))
          .toList(),
      onChanged: (value) {
        if (value != null) {
          _setState(key, value, allowNotApplicable: allowNotApplicable);
        }
      },
    );
  }

  Widget _codeSection(
    String key,
    String label,
    List<String> codes, {
    bool allowNotApplicable = false,
  }) {
    final value = _map(key);
    final state = _safeOption(value['state'], [
      'KNOWN',
      'UNKNOWN',
      'PREFER_NOT_TO_SAY',
      if (allowNotApplicable) 'NOT_APPLICABLE',
    ], fallback: 'UNKNOWN');
    final selected = _safeStrings(value['codes']);
    final stale = selected.where((code) => !codes.contains(code)).toList();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _stateField(key, label, allowNotApplicable: allowNotApplicable),
        if (state == 'KNOWN')
          Wrap(
            spacing: 6,
            runSpacing: 6,
            children: [
              ...stale.map(
                (code) => FilterChip(
                  label: Text('Invalid value: $code'),
                  selected: true,
                  onSelected: (_) {
                    final next = selected.where((item) => item != code).toList()
                      ..sort();
                    _setDomain(key, {...value, 'codes': next});
                  },
                ),
              ),
              ...codes.map(
                (code) => FilterChip(
                  label: Text(code),
                  selected: selected.contains(code),
                  onSelected: (_) {
                    final isNone =
                        code.startsWith('NO_') ||
                        code == 'NONE' ||
                        code == 'NONE_KNOWN';
                    final next = <String>{
                      ...selected,
                      if (!selected.contains(code)) code,
                    };
                    if (selected.contains(code)) {
                      next.remove(code);
                    }
                    if (isNone && next.contains(code)) {
                      next
                        ..clear()
                        ..add(code);
                    } else if (!isNone) {
                      next.removeWhere(
                        (item) =>
                            item.startsWith('NO_') ||
                            item == 'NONE' ||
                            item == 'NONE_KNOWN',
                      );
                      if (key == 'nutrition') {
                        // Vegetarian and vegan are mutually exclusive in V1;
                        // other nutrition concerns may be combined.
                        if (code == 'VEGETARIAN') next.remove('VEGAN');
                        if (code == 'VEGAN') next.remove('VEGETARIAN');
                      }
                    }
                    final nextCodes = next.toList()..sort();
                    final updated = {...value, 'codes': nextCodes};
                    _setDomain(key, updated);
                  },
                ),
              ),
            ],
          ),
      ],
    );
  }

  Widget _bmiSection() {
    final value = _map('bmi');
    final state = _safeOption(value['state'], [
      'KNOWN',
      'UNKNOWN',
      'PREFER_NOT_TO_SAY',
    ], fallback: 'UNKNOWN');
    final contexts = switch (_stage) {
      'PRE_PREGNANCY' => const ['PRE_PREGNANCY', 'CURRENT_NON_PREGNANT'],
      'POSTPARTUM' => const ['PRE_PREGNANCY', 'CURRENT_POSTPARTUM'],
      _ => const ['PRE_PREGNANCY', 'CURRENT_PREGNANCY'],
    };
    return Column(
      children: [
        _stateField('bmi', 'Trạng thái'),
        if (state == 'KNOWN') ...[
          TextField(
            controller: _height,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            decoration: const InputDecoration(labelText: 'Chiều cao (cm)'),
            onChanged: (_) => _updateBmiText(),
          ),
          TextField(
            controller: _weight,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            decoration: const InputDecoration(labelText: 'Cân nặng (kg)'),
            onChanged: (_) => _updateBmiText(),
          ),
          TextField(
            controller: _measuredOn,
            keyboardType: TextInputType.datetime,
            decoration: const InputDecoration(
              labelText: 'Ngày đo (YYYY-MM-DD)',
            ),
            onChanged: (_) => _updateBmiText(),
          ),
          DropdownButtonFormField<String>(
            initialValue: _nullableOption(value['weightContext'], contexts),
            hint: const Text('Chọn bối cảnh cân nặng'),
            decoration: const InputDecoration(labelText: 'Bối cảnh cân nặng'),
            items: contexts
                .map((item) => DropdownMenuItem(value: item, child: Text(item)))
                .toList(),
            onChanged: (item) {
              if (item != null) {
                _setDomain('bmi', {...value, 'weightContext': item});
              }
            },
          ),
          if (value['weightContext'] == 'CURRENT_PREGNANCY' ||
              value['weightContext'] == 'CURRENT_POSTPARTUM')
            const Text(
              'Thông tin được lưu làm nền nhưng không phát sinh tín hiệu BMI V1.',
            ),
        ],
      ],
    );
  }

  Widget _lifestyleSection() => Column(
    children: [
      _lifestyleAnswer('smoking', const ['NEVER', 'FORMER', 'CURRENT']),
      _lifestyleAnswer('alcohol', const [
        'NONE',
        'LESS_THAN_WEEKLY',
        'WEEKLY_OR_MORE',
      ]),
      _lifestyleAnswer('physicalActivity', const [
        'LOW',
        'MODERATE',
        'HIGH',
      ], allowNotApplicable: true),
      _lifestyleAnswer('sleep', const ['NO_CONCERN', 'CONCERN']),
    ],
  );

  Widget _lifestyleAnswer(
    String key,
    List<String> options, {
    bool allowNotApplicable = false,
  }) {
    final value = _safeMap(_map('lifestyle')[key]);
    final state = _safeOption(value['state'], [
      'KNOWN',
      'UNKNOWN',
      'PREFER_NOT_TO_SAY',
      if (allowNotApplicable) 'NOT_APPLICABLE',
    ], fallback: 'UNKNOWN');
    final states = [
      'KNOWN',
      'UNKNOWN',
      'PREFER_NOT_TO_SAY',
      if (allowNotApplicable) 'NOT_APPLICABLE',
    ];
    return Column(
      children: [
        DropdownButtonFormField<String>(
          initialValue: state,
          decoration: InputDecoration(labelText: key),
          items: states
              .map((item) => DropdownMenuItem(value: item, child: Text(item)))
              .toList(),
          onChanged: (item) {
            if (item == null) return;
            final lifestyle = _map('lifestyle');
            final answer = {...value, 'state': item};
            if (item != 'KNOWN') {
              answer.remove('value');
            }
            lifestyle[key] = answer;
            _setDomain('lifestyle', lifestyle);
          },
        ),
        if (state == 'KNOWN')
          DropdownButtonFormField<String>(
            initialValue: _nullableOption(value['value'], options),
            hint: const Text('Chọn giá trị'),
            decoration: const InputDecoration(labelText: 'Giá trị kiểm soát'),
            items: options
                .map((item) => DropdownMenuItem(value: item, child: Text(item)))
                .toList(),
            onChanged: (item) {
              if (item != null) {
                final lifestyle = _map('lifestyle');
                lifestyle[key] = {...value, 'value': item};
                _setDomain('lifestyle', lifestyle);
              }
            },
          ),
      ],
    );
  }

  Widget _vaccinationSection() {
    final vaccination = _map('vaccination');
    final answers = _safeMapList(vaccination['answers'])
        .where((answer) => (answer['code'] as String?)?.isNotEmpty == true)
        .toList(growable: false);
    return Column(
      children: answers.map((answer) {
        final state = _safeOption(answer['state'], const [
          'KNOWN',
          'UNKNOWN',
          'PREFER_NOT_TO_SAY',
          'NOT_APPLICABLE',
        ], fallback: 'UNKNOWN');
        const validValues = {'UP_TO_DATE', 'DUE', 'NOT_RECEIVED'};
        final hasInvalidValue =
            state == 'KNOWN' &&
            (answer['value'] is! String ||
                !validValues.contains(answer['value']));
        return Column(
          children: [
            DropdownButtonFormField<String>(
              initialValue: state,
              decoration: InputDecoration(labelText: answer['code'] as String),
              items: ['KNOWN', 'UNKNOWN', 'PREFER_NOT_TO_SAY', 'NOT_APPLICABLE']
                  .map(
                    (item) => DropdownMenuItem(value: item, child: Text(item)),
                  )
                  .toList(),
              onChanged: (item) {
                if (item == null) return;
                final next = answers.map((entry) {
                  if (entry['code'] != answer['code']) return entry;
                  final updated = {...entry, 'state': item};
                  if (item != 'KNOWN') updated.remove('value');
                  if (item == 'KNOWN' &&
                      !const [
                        'UP_TO_DATE',
                        'DUE',
                        'NOT_RECEIVED',
                      ].contains(updated['value'])) {
                    updated.remove('value');
                  }
                  return updated;
                }).toList();
                _setDomain('vaccination', {'answers': next});
              },
            ),
            if (state == 'KNOWN')
              DropdownButtonFormField<String>(
                initialValue:
                    answer['value'] is String &&
                        validValues.contains(answer['value'])
                    ? answer['value'] as String
                    : null,
                hint: const Text('Chọn trạng thái'),
                decoration: const InputDecoration(labelText: 'Trạng thái'),
                items: validValues
                    .toList()
                    .map(
                      (item) =>
                          DropdownMenuItem(value: item, child: Text(item)),
                    )
                    .toList(),
                onChanged: (item) {
                  if (item == null) return;
                  _setDomain('vaccination', {
                    'answers': answers
                        .map(
                          (entry) => entry['code'] == answer['code']
                              ? {...entry, 'value': item}
                              : entry,
                        )
                        .toList(),
                  });
                },
              ),
            if (hasInvalidValue)
              Align(
                alignment: Alignment.centerLeft,
                child: Text(
                  'Invalid vaccination value. Choose a valid status above.',
                  style: TextStyle(color: Colors.red.shade700, fontSize: 12),
                ),
              ),
          ],
        );
      }).toList(),
    );
  }

  Widget _stiSection() {
    final value = _map('sti');
    final state = _safeOption(value['state'], [
      'KNOWN',
      'UNKNOWN',
      'PREFER_NOT_TO_SAY',
      'NOT_APPLICABLE',
    ], fallback: 'UNKNOWN');
    const infectionCodes = {
      'HIV',
      'SYPHILIS',
      'HEPATITIS_B',
      'HEPATITIS_C',
      'CHLAMYDIA',
      'GONORRHEA',
      'HERPES',
      'HPV',
      'OTHER',
    };
    final selectedInfections = _safeStrings(value['infectionCodes']);
    final staleInfections = selectedInfections
        .where((code) => !infectionCodes.contains(code))
        .toList();
    return Column(
      children: [
        _stateField('sti', 'Trạng thái', allowNotApplicable: true),
        if (state == 'KNOWN')
          DropdownButtonFormField<String>(
            initialValue: _nullableOption(value['status'], const [
              'NO_KNOWN_HISTORY',
              'SCREENING_INFORMATION',
              'PAST_HISTORY',
              'CURRENT_OR_UNDER_TREATMENT',
            ]),
            hint: const Text('Chọn tình trạng'),
            decoration: const InputDecoration(labelText: 'Tình trạng'),
            items:
                [
                      'NO_KNOWN_HISTORY',
                      'SCREENING_INFORMATION',
                      'PAST_HISTORY',
                      'CURRENT_OR_UNDER_TREATMENT',
                    ]
                    .map(
                      (item) =>
                          DropdownMenuItem(value: item, child: Text(item)),
                    )
                    .toList(),
            onChanged: (item) {
              if (item != null) {
                final requiresInfections =
                    item == 'PAST_HISTORY' ||
                    item == 'CURRENT_OR_UNDER_TREATMENT';
                final updated = {...value, 'status': item};
                if (!requiresInfections) updated.remove('infectionCodes');
                _setDomain('sti', updated);
              }
            },
          ),
        if (state == 'KNOWN' &&
            (value['status'] == 'PAST_HISTORY' ||
                value['status'] == 'CURRENT_OR_UNDER_TREATMENT'))
          Padding(
            padding: const EdgeInsets.only(top: 8),
            child: Wrap(
              spacing: 6,
              runSpacing: 6,
              children: [
                ...staleInfections.map(
                  (code) => FilterChip(
                    label: Text('Invalid value: $code'),
                    selected: true,
                    onSelected: (_) {
                      final next =
                          selectedInfections
                              .where((item) => item != code)
                              .toList()
                            ..sort();
                      _setDomain('sti', {...value, 'infectionCodes': next});
                    },
                  ),
                ),
                ...[
                  'HIV',
                  'SYPHILIS',
                  'HEPATITIS_B',
                  'HEPATITIS_C',
                  'CHLAMYDIA',
                  'GONORRHEA',
                  'HERPES',
                  'HPV',
                  'OTHER',
                ].map((code) {
                  final selected = selectedInfections;
                  return FilterChip(
                    label: Text(code),
                    selected: selected.contains(code),
                    onSelected: (enabled) {
                      final next = {...selected};
                      if (enabled) {
                        next.add(code);
                      } else {
                        next.remove(code);
                      }
                      _setDomain('sti', {
                        ...value,
                        'infectionCodes': next.toList()..sort(),
                      });
                    },
                  );
                }),
              ],
            ),
          ),
      ],
    );
  }

  bool _validState(dynamic raw, {bool allowNotApplicable = false}) {
    final allowed = <String>{'KNOWN', 'UNKNOWN', 'PREFER_NOT_TO_SAY'};
    if (allowNotApplicable) allowed.add('NOT_APPLICABLE');
    return raw is String && allowed.contains(raw);
  }

  bool _validCodeDomain(
    String key,
    Set<String> allowed, {
    String? exclusive,
    bool allowNotApplicable = false,
  }) {
    final value = _map(key);
    if (!value.keys.every((field) => field == 'state' || field == 'codes')) {
      return false;
    }
    final state = value['state'];
    if (!_validState(state, allowNotApplicable: allowNotApplicable)) {
      return false;
    }
    if (state != 'KNOWN') return !value.containsKey('codes');
    final raw = value['codes'];
    if (raw is! List || raw.isEmpty) return false;
    final codes = raw.whereType<String>().toList();
    if (codes.length != raw.length || codes.toSet().length != codes.length) {
      return false;
    }
    if (!codes.every(allowed.contains)) return false;
    if (exclusive != null && codes.contains(exclusive) && codes.length > 1) {
      return false;
    }
    if (key == 'nutrition' &&
        codes.contains('VEGETARIAN') &&
        codes.contains('VEGAN')) {
      return false;
    }
    return true;
  }

  String? _validateForSubmit() {
    if (!_validState(_map('age')['state']) ||
        (!_accountHasDateOfBirth && _map('age')['state'] == 'KNOWN')) {
      return 'Age information is incomplete.';
    }
    if (!_validState(_map('bmi')['state'])) {
      return 'BMI state is incomplete.';
    }
    if (!_validCodeDomain('reproductiveHistory', {
      'NO_PRIOR_PREGNANCY',
      'PRIOR_LIVE_BIRTH',
      'PRIOR_PREGNANCY_LOSS',
      'PRIOR_STILLBIRTH',
      'PRIOR_PRETERM_BIRTH',
      'PRIOR_MULTIPLE_PREGNANCY',
      'OTHER_HISTORY',
    }, exclusive: 'NO_PRIOR_PREGNANCY')) {
      return 'Reproductive history is incomplete.';
    }
    if (!_validCodeDomain('underlyingConditions', {
      'NONE_KNOWN',
      'HYPERTENSION',
      'DIABETES',
      'THYROID_DISORDER',
      'CARDIOVASCULAR_DISEASE',
      'ASTHMA',
      'EPILEPSY',
      'KIDNEY_DISEASE',
      'AUTOIMMUNE_DISEASE',
      'MENTAL_HEALTH_CONDITION',
      'OTHER_CLINICIAN_CONFIRMED',
    }, exclusive: 'NONE_KNOWN')) {
      return 'Underlying conditions are incomplete.';
    }
    if (!_validCodeDomain('nutrition', {
      'NO_CURRENT_CONCERN',
      'VEGETARIAN',
      'VEGAN',
      'FOOD_INSECURITY',
      'LOW_APPETITE',
      'NAUSEA_OR_VOMITING',
      'IRON_OR_FOLATE_CONCERN',
      'OTHER_NUTRITION_CONCERN',
    }, exclusive: 'NO_CURRENT_CONCERN')) {
      return 'Nutrition information is incomplete.';
    }
    if (!_validCodeDomain('currentMedications', {
      'NONE',
      'PRENATAL_VITAMIN',
      'FOLIC_ACID',
      'IRON',
      'THYROID_MEDICATION',
      'DIABETES_MEDICATION',
      'ANTIHYPERTENSIVE',
      'ANTICOAGULANT',
      'ANTIEPILEPTIC',
      'MENTAL_HEALTH_MEDICATION',
      'OTHER_PRESCRIBED',
    }, exclusive: 'NONE')) {
      return 'Medication information is incomplete.';
    }
    if (!_validCodeDomain(
      'sexualHealth',
      {
        'NO_CURRENT_INFORMATION_NEED',
        'GENERAL_INFORMATION',
        'CONTRACEPTION_OR_FERTILITY',
        'INTIMACY_DURING_LIFECYCLE',
        'OTHER_NON_URGENT_INFORMATION',
      },
      exclusive: 'NO_CURRENT_INFORMATION_NEED',
      allowNotApplicable: true,
    )) {
      return 'Sexual-health information is incomplete.';
    }
    if (!recommendationProfileHasAllDomains(_profile)) {
      return 'Vui lòng mở lại các mục còn thiếu trong hồ sơ.';
    }
    final bmi = _map('bmi');
    if (bmi['state'] == 'KNOWN' &&
        (bmi['heightCm'] is! num ||
            bmi['weightKg'] is! num ||
            bmi['weightContext'] is! String ||
            bmi['measuredOn'] is! String ||
            (bmi['measuredOn'] as String).trim().isEmpty)) {
      return 'Vui lòng hoàn tất các trường BMI và ngày đo.';
    }
    if (bmi['state'] == 'KNOWN') {
      final height = bmi['heightCm'];
      final weight = bmi['weightKg'];
      final measuredOn = bmi['measuredOn'];
      final context = bmi['weightContext'];
      final allowedContexts = switch (_stage) {
        'PRE_PREGNANCY' => const {'PRE_PREGNANCY', 'CURRENT_NON_PREGNANT'},
        'POSTPARTUM' => const {'PRE_PREGNANCY', 'CURRENT_POSTPARTUM'},
        _ => const {'PRE_PREGNANCY', 'CURRENT_PREGNANCY'},
      };
      bool validDecimal(num value) {
        final text = value.toString();
        if (text.contains('e') || text.contains('E')) return false;
        final point = text.indexOf('.');
        return point < 0 || text.length - point - 1 <= 1;
      }

      bool validDate(dynamic raw) {
        if (raw is! String ||
            !RegExp(r'^\d{4}-\d{2}-\d{2}$').hasMatch(raw.trim())) {
          return false;
        }
        final parsed = DateTime.tryParse(raw.trim());
        if (parsed == null) return false;
        final now = DateTime.now();
        return !parsed.isAfter(DateTime(now.year, now.month, now.day));
      }

      if (height is! num ||
          weight is! num ||
          height < 100 ||
          height > 250 ||
          weight < 20 ||
          weight > 300 ||
          !validDecimal(height) ||
          !validDecimal(weight) ||
          context is! String ||
          !allowedContexts.contains(context) ||
          !validDate(measuredOn)) {
        return 'BMI information is outside the allowed range.';
      }
    }
    for (final key in [
      'reproductiveHistory',
      'underlyingConditions',
      'nutrition',
      'currentMedications',
      'sexualHealth',
    ]) {
      final value = _map(key);
      if (value['state'] == 'KNOWN' && _safeStrings(value['codes']).isEmpty) {
        return 'Vui lòng chọn ít nhất một giá trị trong mục $key.';
      }
    }
    final lifestyle = _map('lifestyle');
    const lifestyleOptions = {
      'smoking': {'NEVER', 'FORMER', 'CURRENT'},
      'alcohol': {'NONE', 'LESS_THAN_WEEKLY', 'WEEKLY_OR_MORE'},
      'physicalActivity': {'LOW', 'MODERATE', 'HIGH'},
      'sleep': {'NO_CONCERN', 'CONCERN'},
    };
    for (final key in ['smoking', 'alcohol', 'physicalActivity', 'sleep']) {
      final value = _safeMap(lifestyle[key]);
      if (!value.keys.every((field) => field == 'state' || field == 'value')) {
        return 'Lifestyle information is incomplete.';
      }
      final allowNotApplicable = key == 'physicalActivity';
      if (!_validState(
        value['state'],
        allowNotApplicable: allowNotApplicable,
      )) {
        return 'Lifestyle state is incomplete.';
      }
      if (value['state'] == 'KNOWN' &&
          (value['value'] is! String ||
              !lifestyleOptions[key]!.contains(value['value']))) {
        return 'Lifestyle value is incomplete.';
      }
      if (value['state'] != 'KNOWN' && value.containsKey('value')) {
        return 'Lifestyle value must be omitted when undisclosed.';
      }
    }
    final vaccination = _map('vaccination');
    final answers = _safeMapList(vaccination['answers']);
    const vaccineCodes = {
      'INFLUENZA',
      'COVID_19',
      'TDAP',
      'HEPATITIS_B',
      'RUBELLA_IMMUNITY',
    };
    const vaccineValues = {'UP_TO_DATE', 'DUE', 'NOT_RECEIVED'};
    final vaccineCodeList = answers
        .map((answer) => answer['code'])
        .whereType<String>()
        .toList();
    if (answers.length != 5 ||
        vaccineCodeList.length != answers.length ||
        vaccineCodeList.toSet().length != 5 ||
        vaccineCodeList.toSet().difference(vaccineCodes).isNotEmpty ||
        answers.any((answer) {
          if (!answer.keys.every(
            (field) => field == 'code' || field == 'state' || field == 'value',
          )) {
            return true;
          }
          final state = answer['state'];
          if (!_validState(state, allowNotApplicable: true)) return true;
          if (state == 'KNOWN') {
            return answer['value'] is! String ||
                !vaccineValues.contains(answer['value']);
          }
          return answer.containsKey('value');
        })) {
      return 'Vui lòng hoàn tất năm mục tiêm chủng.';
    }
    final sti = _map('sti');
    if (!_validState(sti['state'], allowNotApplicable: true)) {
      return 'STI state is incomplete.';
    }
    if (sti['state'] == 'KNOWN') {
      if (!sti.keys.every(
        (field) =>
            field == 'state' || field == 'status' || field == 'infectionCodes',
      )) {
        return 'STI information is incomplete.';
      }
      final status = sti['status'];
      const statuses = {
        'NO_KNOWN_HISTORY',
        'SCREENING_INFORMATION',
        'PAST_HISTORY',
        'CURRENT_OR_UNDER_TREATMENT',
      };
      if (status is! String || !statuses.contains(status)) {
        return 'STI status is incomplete.';
      }
      if ((status == 'PAST_HISTORY' ||
              status == 'CURRENT_OR_UNDER_TREATMENT') &&
          (_safeStrings(sti['infectionCodes']).isEmpty ||
              _safeStrings(sti['infectionCodes']).length > 9 ||
              _safeStrings(sti['infectionCodes']).toSet().length !=
                  _safeStrings(sti['infectionCodes']).length ||
              !_safeStrings(sti['infectionCodes']).every(
                {
                  'HIV',
                  'SYPHILIS',
                  'HEPATITIS_B',
                  'HEPATITIS_C',
                  'CHLAMYDIA',
                  'GONORRHEA',
                  'HERPES',
                  'HPV',
                  'OTHER',
                }.contains,
              ))) {
        return 'Vui lòng chọn ít nhất một nhóm STI.';
      }
      if (status != 'PAST_HISTORY' &&
          status != 'CURRENT_OR_UNDER_TREATMENT' &&
          sti.containsKey('infectionCodes')) {
        return 'STI infection codes must be omitted for this status.';
      }
    } else if (sti.containsKey('status') || sti.containsKey('infectionCodes')) {
      return 'STI values must be omitted when undisclosed.';
    }
    return null;
  }

  String _messageForApi(ApiException error, {required String fallback}) {
    switch (error.errorCode) {
      case 'RECOMMENDATION_SUBMISSION_CONFLICT':
        return 'Hồ sơ đã thay đổi ở phiên khác. Vui lòng tải lại rồi thử lại.';
      case 'RECOMMENDATION_POLICY_MISMATCH':
        return 'Chính sách cá nhân hóa đã thay đổi. Vui lòng tải lại biểu mẫu.';
      case 'RECOMMENDATION_PROFILE_INVALID':
        return 'Một số mục không hợp lệ. Vui lòng kiểm tra các lựa chọn bắt buộc.';
      case 'RECOMMENDATION_JOURNEY_REQUIRED':
        return 'Vui lòng hoàn tất thiết lập hành trình maternal trước khi mở hồ sơ này.';
      case 'RECOMMENDATION_PROFILE_REVIEW_REQUIRED':
        return 'Hồ sơ cần được xem lại theo giai đoạn hiện tại trước khi lưu.';
      case 'RECOMMENDATION_CONTEXT_UNAVAILABLE':
        return 'Chưa thể xác định ngữ cảnh hành trình an toàn. Vui lòng thử lại sau.';
      default:
        return error.statusCode >= 500
            ? 'Máy chủ tạm thời không sẵn sàng. Vui lòng thử lại.'
            : fallback;
    }
  }

  Future<void> _decline() async {
    if (_saving) return;
    final expectedUserId = AuthState.instance.userId;
    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      await _service.decline();
      if (mounted && AuthState.instance.userId == expectedUserId) {
        context.go('/mother-home');
      }
    } on ApiException catch (error) {
      if (mounted) {
        setState(
          () => _error = _messageForApi(
            error,
            fallback: 'Không thể lưu lựa chọn. Vui lòng thử lại.',
          ),
        );
      }
    } catch (_) {
      if (mounted) {
        setState(() => _error = 'Không thể lưu lựa chọn. Vui lòng thử lại.');
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  Future<void> _submit() async {
    if (_saving || AuthState.instance.userId == null) {
      return;
    }
    final validationError = _validateForSubmit();
    if (validationError != null) {
      setState(() => _error = validationError);
      return;
    }
    final expectedUser = AuthState.instance.userId;
    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      await _service.putProfile(profile: _profile, submissionId: _submissionId);
      if (!mounted || AuthState.instance.userId != expectedUser) return;
      if (expectedUser != null) await _service.clearDraftFor(expectedUser);
      if (!mounted || AuthState.instance.userId != expectedUser) return;
      context.go('/mother-home');
    } on ApiException catch (error) {
      if (mounted) {
        setState(
          () => _error = _messageForApi(
            error,
            fallback:
                'Hồ sơ chưa được lưu. Kiểm tra các mục bắt buộc rồi thử lại.',
          ),
        );
      }
    } catch (_) {
      if (mounted) {
        setState(
          () => _error =
              'Hồ sơ chưa được lưu. Kiểm tra các mục bắt buộc rồi thử lại.',
        );
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }
}
