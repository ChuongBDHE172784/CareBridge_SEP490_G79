import 'dart:async';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:uuid/uuid.dart';

import '../../../core/auth/auth_state.dart';
import '../../../core/network/api_client.dart';
import '../../journey/services/journey_service.dart';
import '../models/recommendation_model.dart';
import '../models/recommendation_questionnaire.dart';
import '../services/recommendation_service.dart';

/// Progressive, owner-facing recommendation profile questionnaire.
///
/// The server still owns the four-state compatibility contract. This screen
/// deliberately exposes only a completed answer or an explicit skip, keeping
/// technical states and raw catalog codes out of the user-facing flow.
class RecommendationProfileScreen extends StatefulWidget {
  const RecommendationProfileScreen({
    super.key,
    this.service,
    this.journeyService,
    this.journeyStage,
    this.now,
  });

  final RecommendationService? service;
  final JourneyService? journeyService;
  final String? journeyStage;
  final DateTime Function()? now;

  @override
  State<RecommendationProfileScreen> createState() =>
      _RecommendationProfileScreenState();
}

class _RecommendationProfileScreenState
    extends State<RecommendationProfileScreen> {
  late final RecommendationService _service;
  late final JourneyService _journeyService;

  final _dobController = TextEditingController();
  final _heightController = TextEditingController();
  final _weightController = TextEditingController();

  Map<String, dynamic> _profile = RecommendationProfileDraft.empty();
  String _stage = 'PREGNANCY';
  String? _accountDateOfBirth;
  String _submissionId = const Uuid().v4();
  String? _observedUserId;
  String? _error;
  int _loadGeneration = 0;
  int _questionIndex = 0;
  bool _loading = true;
  bool _saving = false;
  bool _consentDecisionMade = false;

  List<RecommendationQuestion> get _questions =>
      RecommendationQuestionnaire.questions;

  bool get _isReview => _questionIndex >= _questions.length;

  bool _isCurrentOperation(int generation, String? expectedUserId) =>
      mounted &&
      generation == _loadGeneration &&
      AuthState.instance.userId == expectedUserId;

  DateTime get _today {
    final value = (widget.now ?? DateTime.now).call();
    return DateTime(value.year, value.month, value.day);
  }

  @override
  void initState() {
    super.initState();
    _service = widget.service ?? RecommendationService();
    _journeyService = widget.journeyService ?? JourneyService();
    _observedUserId = AuthState.instance.userId;
    AuthState.instance.addListener(_onAccountChanged);
    unawaited(_load());
  }

  @override
  void dispose() {
    _dobController.dispose();
    _heightController.dispose();
    _weightController.dispose();
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
      unawaited(_clearDraftBestEffort(previousUserId));
    }
    if (!mounted) return;
    setState(() {
      _profile = RecommendationProfileDraft.empty();
      _accountDateOfBirth = null;
      _dobController.clear();
      _heightController.clear();
      _weightController.clear();
      _questionIndex = 0;
      _loading = true;
      _saving = false;
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

    if (mounted) {
      setState(() {
        _loading = true;
        _error = null;
      });
    }

    try {
      final dashboard = await _journeyService.getDashboard();
      if (!current()) return;
      if (dashboard.journeyType == 'BABY_CARE') {
        if (mounted) context.go('/mother-home');
        return;
      }

      String? dateOfBirth;
      try {
        dateOfBirth = await _service.getDateOfBirth();
      } catch (_) {
        // A transient account-profile read must not block the questionnaire.
        // The user can still enter a DOB directly; account switching remains
        // guarded by the generation/current-user check below.
        if (!current()) return;
      }
      if (!current()) return;
      final response = await _service.getProfile();
      if (!current()) return;
      final draft = await _service.readDraft();
      if (!current()) return;

      _stage = dashboard.journeyType ?? widget.journeyStage ?? 'PREGNANCY';
      _accountDateOfBirth = dateOfBirth;
      _profile = RecommendationQuestionnaire.normalizeProfile(
        RecommendationProfileDraft.mergeProfiles(response.profile, draft),
      );
      if (_accountDateOfBirth == null) {
        _profile['age'] = <String, dynamic>{'state': 'UNKNOWN'};
      }
      _normalizeStageConditionedInputs();
      _syncControllers();
      if (!current()) return;
      setState(() {
        _loading = false;
        _error = null;
      });
    } on ApiException catch (error) {
      if (!current()) return;
      if (error.errorCode == 'RECOMMENDATION_JOURNEY_REQUIRED') {
        if (!mounted) return;
        context.go('/mother-stage-selection');
        return;
      }
      setState(() {
        _loading = false;
        _error = _messageForApi(error);
      });
    } catch (_) {
      if (!current()) return;
      setState(() {
        _loading = false;
        _error = 'Không thể tải hồ sơ cá nhân hóa. Vui lòng thử lại.';
      });
    }
  }

  Map<String, dynamic> _map(String key) {
    final raw = _profile[key];
    return raw is Map
        ? raw.map((key, item) => MapEntry(key.toString(), item))
        : <String, dynamic>{};
  }

  Map<String, dynamic> _mapValue(dynamic raw) => raw is Map
      ? raw.map((key, value) => MapEntry(key.toString(), value))
      : <String, dynamic>{};

  Map<String, dynamic> _lifestyleAnswer(String key) {
    final lifestyle = _map('lifestyle');
    final raw = lifestyle[key];
    final value = raw is Map
        ? raw.map((nestedKey, item) => MapEntry(nestedKey.toString(), item))
        : <String, dynamic>{};
    value.putIfAbsent('state', () => 'UNKNOWN');
    return value;
  }

  Map<String, dynamic> _vaccineAnswer(String code) {
    final answers = _map('vaccination')['answers'];
    if (answers is List) {
      for (final raw in answers.whereType<Map>()) {
        if (raw['code'] == code) {
          return raw.map((key, item) => MapEntry(key.toString(), item));
        }
      }
    }
    return <String, dynamic>{'code': code, 'state': 'UNKNOWN'};
  }

  void _setProfile(Map<String, dynamic> value) {
    _submissionId = const Uuid().v4();
    setState(() => _profile = value);
    unawaited(_saveDraftBestEffort(value));
  }

  void _setDomain(String key, Map<String, dynamic> value) {
    final next = Map<String, dynamic>.from(_profile);
    next[key] = value;
    _setProfile(next);
  }

  void _setLifestyle(String key, Map<String, dynamic> value) {
    final next = Map<String, dynamic>.from(_profile);
    final lifestyle = _map('lifestyle');
    lifestyle[key] = value;
    next['lifestyle'] = lifestyle;
    _setProfile(next);
  }

  void _setVaccinationAnswer(String code, Map<String, dynamic> answer) {
    final next = Map<String, dynamic>.from(_profile);
    final vaccination = _map('vaccination');
    final answers =
        (vaccination['answers'] is List
                ? (vaccination['answers'] as List)
                      .whereType<Map>()
                      .map(
                        (raw) => raw.map(
                          (key, value) => MapEntry(key.toString(), value),
                        ),
                      )
                      .toList()
                : <Map<String, dynamic>>[])
            .map((entry) => entry['code'] == code ? answer : entry)
            .toList();
    vaccination['answers'] = answers;
    next['vaccination'] = vaccination;
    _setProfile(next);
  }

  void _syncControllers() {
    _dobController.text = _accountDateOfBirth ?? '';
    final bmi = _map('bmi');
    if (bmi['state'] == 'KNOWN') {
      _heightController.text = bmi['heightCm']?.toString() ?? '';
      _weightController.text = bmi['weightKg']?.toString() ?? '';
    } else {
      _heightController.clear();
      _weightController.clear();
    }
  }

  void _normalizeStageConditionedInputs() {
    final bmi = _map('bmi');
    if (bmi['state'] != 'KNOWN') return;
    if (!_allowedContexts.contains(bmi['weightContext'])) {
      _profile['bmi'] = <String, dynamic>{'state': 'UNKNOWN'};
    }
  }

  Set<String> get _allowedContexts => switch (_stage) {
    'PRE_PREGNANCY' => {'PRE_PREGNANCY', 'CURRENT_NON_PREGNANT'},
    'POSTPARTUM' => {'PRE_PREGNANCY', 'CURRENT_POSTPARTUM'},
    _ => {'PRE_PREGNANCY', 'CURRENT_PREGNANCY'},
  };

  String get _defaultWeightContext => switch (_stage) {
    'PRE_PREGNANCY' => 'CURRENT_NON_PREGNANT',
    'POSTPARTUM' => 'CURRENT_POSTPARTUM',
    _ => 'CURRENT_PREGNANCY',
  };

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    if (_error != null && !_consentDecisionMade) {
      return _buildErrorScaffold();
    }
    return Scaffold(
      appBar: AppBar(
        leading: _consentDecisionMade && _questionIndex > 0
            ? IconButton(
                tooltip: 'Quay lại',
                onPressed: _saving ? null : _goBack,
                icon: const Icon(Icons.arrow_back),
              )
            : null,
        title: const Text('Hồ sơ nền cá nhân hóa'),
      ),
      body: SafeArea(
        child: _consentDecisionMade ? _buildQuestionnaire() : _buildConsent(),
      ),
    );
  }

  Widget _buildErrorScaffold() => Scaffold(
    body: Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Semantics(
              liveRegion: true,
              child: Text(_error!, textAlign: TextAlign.center),
            ),
            const SizedBox(height: 16),
            FilledButton(onPressed: _load, child: const Text('Thử lại')),
          ],
        ),
      ),
    ),
  );

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
        'Mẹ có thể chia sẻ thông tin nền theo từng câu để CareBridge chọn bài viết giáo dục an toàn, đúng thời điểm. Mẹ có thể bỏ qua bất kỳ câu nào.',
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

  Widget _buildQuestionnaire() {
    final progress = _isReview ? 1.0 : (_questionIndex + 1) / _questions.length;
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
          child: Semantics(
            label: _isReview
                ? 'Đã hoàn tất các câu hỏi'
                : 'Câu ${_questionIndex + 1} trên ${_questions.length}',
            child: LinearProgressIndicator(value: progress),
          ),
        ),
        Expanded(
          child: ListView(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 16),
            children: [
              if (_isReview) _buildReview() else _buildCurrentQuestion(),
              if (_error != null && _consentDecisionMade)
                Padding(
                  padding: const EdgeInsets.only(top: 12),
                  child: Semantics(
                    liveRegion: true,
                    child: Text(
                      _error!,
                      style: TextStyle(color: Colors.red.shade700),
                    ),
                  ),
                ),
            ],
          ),
        ),
        _buildNavigation(),
      ],
    );
  }

  Widget _buildCurrentQuestion() {
    final question = _questions[_questionIndex];
    return Card(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 20, 16, 20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              question.title,
              style: const TextStyle(fontSize: 21, fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 8),
            Text(
              'Câu ${_questionIndex + 1}/${_questions.length}',
              style: TextStyle(color: Colors.grey.shade700),
            ),
            const SizedBox(height: 20),
            _buildQuestionBody(question),
          ],
        ),
      ),
    );
  }

  Widget _buildQuestionBody(RecommendationQuestion question) =>
      switch (question.kind) {
        RecommendationQuestionKind.age => _buildAgeQuestion(),
        RecommendationQuestionKind.bmi => _buildBmiQuestion(),
        RecommendationQuestionKind.codeSet => _buildCodeSetQuestion(question),
        RecommendationQuestionKind.lifestyle => _buildLifestyleQuestion(
          question,
        ),
        RecommendationQuestionKind.vaccination => _buildVaccinationQuestion(
          question,
        ),
        RecommendationQuestionKind.sti => _buildStiQuestion(),
      };

  Widget _buildAgeQuestion() => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      const Text(
        'Ngày sinh được lưu trong hồ sơ tài khoản và không gửi lại trong hồ sơ cá nhân hóa.',
      ),
      const SizedBox(height: 12),
      TextField(
        key: const Key('recommendation-dob-field'),
        controller: _dobController,
        keyboardType: TextInputType.datetime,
        decoration: InputDecoration(
          labelText: 'Ngày sinh (YYYY-MM-DD)',
          hintText: 'Ví dụ: 1995-08-21',
          border: const OutlineInputBorder(),
          suffixIcon: IconButton(
            tooltip: 'Chọn ngày sinh',
            onPressed: _pickDateOfBirth,
            icon: const Icon(Icons.calendar_today_outlined),
          ),
        ),
      ),
    ],
  );

  Widget _buildBmiQuestion() => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      TextField(
        key: const Key('recommendation-height-field'),
        controller: _heightController,
        keyboardType: const TextInputType.numberWithOptions(decimal: true),
        decoration: const InputDecoration(
          labelText: 'Chiều cao (cm)',
          border: OutlineInputBorder(),
        ),
      ),
      const SizedBox(height: 12),
      TextField(
        key: const Key('recommendation-weight-field'),
        controller: _weightController,
        keyboardType: const TextInputType.numberWithOptions(decimal: true),
        decoration: const InputDecoration(
          labelText: 'Cân nặng (kg)',
          border: OutlineInputBorder(),
        ),
      ),
    ],
  );

  Widget _buildCodeSetQuestion(RecommendationQuestion question) {
    final value = _map(question.domain);
    final selected = (value['codes'] is List
        ? (value['codes'] as List).whereType<String>()
        : const <String>[]);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text('Có thể chọn một hoặc nhiều lựa chọn.'),
        const SizedBox(height: 12),
        _choiceWrap(
          values:
              RecommendationQuestionnaire.codeOptions[question.domain] ??
              const [],
          selected: selected.toSet(),
          multiSelect: true,
          onSelected: (code) => _toggleCode(question.domain, code),
        ),
      ],
    );
  }

  Widget _buildLifestyleQuestion(RecommendationQuestion question) {
    if (question.code != null) {
      final key = question.code!;
      final value = _lifestyleAnswer(key);
      return _choiceWrap(
        values: RecommendationQuestionnaire.lifestyleOptions[key] ?? const [],
        selected: value['value'] as String?,
        onSelected: (option) {
          _setLifestyle(key, <String, dynamic>{
            'state': 'KNOWN',
            'value': option,
          });
        },
      );
    }
    final selected = _selectedLifestyleFlags();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text('Có thể chọn một hoặc nhiều lựa chọn.'),
        const SizedBox(height: 12),
        _choiceWrap(
          values: RecommendationQuestionnaire.lifestyleGroupOptions,
          selected: selected,
          multiSelect: true,
          onSelected: _toggleLifestyleFlag,
        ),
      ],
    );
  }

  Widget _buildVaccinationQuestion(RecommendationQuestion question) {
    if (question.code == null) {
      final selected = _selectedVaccinationFlags();
      return Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Text('Có thể chọn một hoặc nhiều lựa chọn.'),
          const SizedBox(height: 12),
          _choiceWrap(
            values: RecommendationQuestionnaire.vaccinationGroupOptions,
            selected: selected,
            multiSelect: true,
            onSelected: _toggleVaccinationFlag,
          ),
        ],
      );
    }
    final answer = _vaccineAnswer(question.code!);
    return _choiceWrap(
      values: const ['UP_TO_DATE', 'DUE', 'NOT_RECEIVED'],
      selected: answer['value'] as String?,
      onSelected: (option) => _setVaccinationAnswer(question.code!, {
        'code': question.code,
        'state': 'KNOWN',
        'value': option,
      }),
    );
  }

  Widget _buildStiQuestion() {
    final value = _map('sti');
    final status = value['status'] as String?;
    final selectedInfections = (value['infectionCodes'] is List
        ? (value['infectionCodes'] as List).whereType<String>()
        : const <String>[]);
    final needsInfections =
        status == 'PAST_HISTORY' || status == 'CURRENT_OR_UNDER_TREATMENT';
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _choiceWrap(
          values: const [
            'NO_KNOWN_HISTORY',
            'SCREENING_INFORMATION',
            'PAST_HISTORY',
            'CURRENT_OR_UNDER_TREATMENT',
          ],
          selected: status,
          onSelected: (option) {
            final next = _map('sti');
            next['state'] = 'KNOWN';
            next['status'] = option;
            if (option != 'PAST_HISTORY' &&
                option != 'CURRENT_OR_UNDER_TREATMENT') {
              next.remove('infectionCodes');
            }
            _setDomain('sti', next);
          },
        ),
        if (needsInfections) ...[
          const SizedBox(height: 16),
          const Text('Chọn nhóm nhiễm nếu phù hợp.'),
          const SizedBox(height: 8),
          _choiceWrap(
            values: const [
              'HIV',
              'SYPHILIS',
              'HEPATITIS_B',
              'HEPATITIS_C',
              'CHLAMYDIA',
              'GONORRHEA',
              'HERPES',
              'HPV',
              'OTHER',
            ],
            selected: selectedInfections.toSet(),
            multiSelect: true,
            onSelected: (code) {
              final next = _map('sti');
              final codes = selectedInfections.toSet();
              if (!codes.add(code)) codes.remove(code);
              next['state'] = 'KNOWN';
              next['infectionCodes'] = codes.toList()..sort();
              _setDomain('sti', next);
            },
          ),
        ],
      ],
    );
  }

  Widget _choiceWrap({
    required List<String> values,
    required dynamic selected,
    required ValueChanged<String> onSelected,
    bool multiSelect = false,
  }) {
    final selectedValues = selected is Set<String>
        ? selected
        : selected is String
        ? <String>{selected}
        : <String>{};
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: values
          .map(
            (value) => FilterChip(
              key: ValueKey('recommendation-option-$value'),
              label: Text(RecommendationQuestionnaire.labelFor(value)),
              selected: selectedValues.contains(value),
              onSelected: (_) => onSelected(value),
              showCheckmark: multiSelect,
            ),
          )
          .toList(),
    );
  }

  Set<String> _selectedLifestyleFlags() {
    final lifestyle = _map('lifestyle');
    final selected = <String>{};
    final rawFlags = lifestyle['flags'];
    if (rawFlags is List) {
      selected.addAll(
        rawFlags.whereType<String>().where(
          RecommendationQuestionnaire.lifestyleFlagValues.contains,
        ),
      );
    }
    final smoking = _mapValue(lifestyle['smoking']);
    final alcohol = _mapValue(lifestyle['alcohol']);
    final activity = _mapValue(lifestyle['physicalActivity']);
    final sleep = _mapValue(lifestyle['sleep']);
    if (smoking['value'] == 'CURRENT') selected.add('SMOKING');
    if (alcohol['value'] != null && alcohol['value'] != 'NONE') {
      selected.add('ALCOHOL_USE');
    }
    if (activity['value'] == 'LOW') selected.add('LOW_ACTIVITY');
    if (sleep['value'] == 'CONCERN') selected.add('SLEEP_CONCERN');
    if (selected.isEmpty &&
        [
          smoking,
          alcohol,
          activity,
          sleep,
        ].every((answer) => answer['state'] == 'KNOWN')) {
      selected.add('NONE_KNOWN_LIFESTYLE');
    }
    return selected;
  }

  void _toggleLifestyleFlag(String code) {
    final selected = _selectedLifestyleFlags();
    if (code == 'NONE_KNOWN_LIFESTYLE') {
      selected
        ..clear()
        ..add(code);
    } else if (!selected.remove(code)) {
      selected
        ..remove('NONE_KNOWN_LIFESTYLE')
        ..add(code);
    }
    final next = _map('lifestyle');
    if (selected.isEmpty) {
      for (final key in const [
        'smoking',
        'alcohol',
        'physicalActivity',
        'sleep',
      ]) {
        next[key] = <String, dynamic>{'state': 'UNKNOWN'};
      }
      next.remove('flags');
    } else if (selected.contains('NONE_KNOWN_LIFESTYLE')) {
      next
        ..['smoking'] = <String, dynamic>{'state': 'KNOWN', 'value': 'NEVER'}
        ..['alcohol'] = <String, dynamic>{'state': 'KNOWN', 'value': 'NONE'}
        ..['physicalActivity'] = <String, dynamic>{
          'state': 'KNOWN',
          'value': 'MODERATE',
        }
        ..['sleep'] = <String, dynamic>{'state': 'KNOWN', 'value': 'NO_CONCERN'}
        ..remove('flags');
    } else {
      next['smoking'] = <String, dynamic>{
        'state': 'KNOWN',
        'value': selected.contains('SMOKING') ? 'CURRENT' : 'NEVER',
      };
      next['alcohol'] = <String, dynamic>{
        'state': 'KNOWN',
        'value': selected.contains('ALCOHOL_USE') ? 'ANY_USE' : 'NONE',
      };
      next['physicalActivity'] = <String, dynamic>{
        'state': 'KNOWN',
        'value': selected.contains('LOW_ACTIVITY') ? 'LOW' : 'MODERATE',
      };
      next['sleep'] = <String, dynamic>{
        'state': 'KNOWN',
        'value': selected.contains('SLEEP_CONCERN') ? 'CONCERN' : 'NO_CONCERN',
      };
      final extraFlags =
          selected
              .where(
                (item) => const {
                  'SUBSTANCE_USE',
                  'STRESS',
                  'UNHEALTHY_DIET',
                }.contains(item),
              )
              .toList()
            ..sort();
      if (extraFlags.isEmpty) {
        next.remove('flags');
      } else {
        next['flags'] = extraFlags;
      }
    }
    _setDomain('lifestyle', next);
  }

  Set<String> _selectedVaccinationFlags() {
    final vaccination = _map('vaccination');
    final selected = <String>{};
    final rawFlags = vaccination['flags'];
    if (rawFlags is List) {
      selected.addAll(
        rawFlags.whereType<String>().where(
          RecommendationQuestionnaire.vaccinationFlags.contains,
        ),
      );
    }
    final answers = vaccination['answers'];
    if (answers is List) {
      final byCode = <String, Map<String, dynamic>>{};
      for (final raw in answers.whereType<Map>()) {
        final code = raw['code'];
        if (code is String) byCode[code] = _mapValue(raw);
      }
      if (byCode.values.every((answer) => answer['state'] == 'KNOWN')) {
        if (byCode['RUBELLA_IMMUNITY']?['value'] == 'NOT_RECEIVED') {
          selected.add('RUBELLA_NONIMMUNE');
        }
        if (byCode['HEPATITIS_B']?['value'] != 'UP_TO_DATE') {
          selected.add('HEPATITIS_B_INCOMPLETE');
        }
        if (byCode['INFLUENZA']?['value'] != 'UP_TO_DATE') {
          selected.add('INFLUENZA_DUE');
        }
        if (byCode['COVID_19']?['value'] != 'UP_TO_DATE') {
          selected.add('COVID_19_UPDATE');
        }
        if (selected.isEmpty) selected.add('NONE_KNOWN_VACCINATION');
      }
    }
    return selected;
  }

  void _toggleVaccinationFlag(String code) {
    final selected = _selectedVaccinationFlags();
    if (code == 'NOT_ASSESSED') {
      selected
        ..clear()
        ..add(code);
    } else if (code == 'NONE_KNOWN_VACCINATION') {
      selected
        ..clear()
        ..add(code);
    } else if (!selected.remove(code)) {
      selected
        ..remove('NOT_ASSESSED')
        ..remove('NONE_KNOWN_VACCINATION')
        ..add(code);
    }
    final next = _map('vaccination');
    final answers = <Map<String, dynamic>>[
      for (final vaccineCode in RecommendationQuestionnaire.vaccineCodes)
        <String, dynamic>{
          'code': vaccineCode,
          'state': selected.contains('NOT_ASSESSED') ? 'UNKNOWN' : 'KNOWN',
          if (!selected.contains('NOT_ASSESSED')) 'value': 'UP_TO_DATE',
        },
    ];
    if (selected.contains('RUBELLA_NONIMMUNE')) {
      answers.singleWhere(
        (answer) => answer['code'] == 'RUBELLA_IMMUNITY',
      )['value'] = 'NOT_RECEIVED';
    }
    if (selected.contains('HEPATITIS_B_INCOMPLETE')) {
      answers.singleWhere(
        (answer) => answer['code'] == 'HEPATITIS_B',
      )['value'] = 'DUE';
    }
    if (selected.contains('INFLUENZA_DUE')) {
      answers.singleWhere((answer) => answer['code'] == 'INFLUENZA')['value'] =
          'DUE';
    }
    if (selected.contains('COVID_19_UPDATE')) {
      answers.singleWhere((answer) => answer['code'] == 'COVID_19')['value'] =
          'DUE';
    }
    next['answers'] = answers;
    if (selected.contains('NOT_ASSESSED')) {
      next['flags'] = ['NOT_ASSESSED'];
    } else {
      next.remove('flags');
    }
    if (selected.isEmpty) {
      next['answers'] = [
        for (final vaccineCode in RecommendationQuestionnaire.vaccineCodes)
          <String, dynamic>{'code': vaccineCode, 'state': 'UNKNOWN'},
      ];
    }
    _setDomain('vaccination', next);
  }

  void _toggleSexualHealthCode(String code) {
    final value = _map('sexualHealth');
    final selected = (value['codes'] is List
        ? (value['codes'] as List).whereType<String>().toSet()
        : <String>{});
    if (!selected.add(code)) selected.remove(code);
    final next = <String, dynamic>{'state': 'KNOWN'};
    if (selected.isNotEmpty) {
      next['codes'] = selected.toList()..sort();
    } else {
      next['state'] = 'UNKNOWN';
    }
    final sti = <String, dynamic>{'state': 'UNKNOWN'};
    if (selected.contains('STI_SUSPECTED_OR_KNOWN')) {
      sti
        ..['state'] = 'KNOWN'
        ..['status'] = 'SUSPECTED_OR_KNOWN';
    } else if (selected.contains('STI_RISK')) {
      sti
        ..['state'] = 'KNOWN'
        ..['status'] = 'AT_RISK';
    }
    final profile = Map<String, dynamic>.from(_profile)
      ..['sexualHealth'] = next
      ..['sti'] = sti;
    _setProfile(profile);
  }

  void _toggleCode(String domain, String code) {
    if (domain == 'sexualHealth') {
      _toggleSexualHealthCode(code);
      return;
    }
    final value = _map(domain);
    final selected = (value['codes'] is List
        ? (value['codes'] as List).whereType<String>().toSet()
        : <String>{});
    if (!selected.add(code)) selected.remove(code);
    final isExclusiveCode = switch (domain) {
      'reproductiveHistory' => const {
        'NO_PRIOR_PREGNANCY',
        'NO_LISTED_REPRODUCTIVE_HISTORY',
      }.contains(code),
      'underlyingConditions' => code == 'NONE_KNOWN',
      'nutrition' => code == 'NO_CURRENT_CONCERN',
      'currentMedications' => code == 'NONE',
      'sexualHealth' => code == 'NO_CURRENT_INFORMATION_NEED',
      _ => false,
    };
    if (isExclusiveCode && selected.contains(code)) {
      selected
        ..clear()
        ..add(code);
    } else if (!isExclusiveCode) {
      selected.removeWhere(
        (item) => switch (domain) {
          'reproductiveHistory' => const {
            'NO_PRIOR_PREGNANCY',
            'NO_LISTED_REPRODUCTIVE_HISTORY',
          }.contains(item),
          'underlyingConditions' => item == 'NONE_KNOWN',
          'nutrition' => item == 'NO_CURRENT_CONCERN',
          'currentMedications' => item == 'NONE',
          'sexualHealth' => item == 'NO_CURRENT_INFORMATION_NEED',
          _ => false,
        },
      );
      if (domain == 'nutrition') {
        if (code == 'VEGETARIAN') selected.remove('VEGAN');
        if (code == 'VEGAN') selected.remove('VEGETARIAN');
      }
    }
    value['state'] = 'KNOWN';
    if (selected.isEmpty) {
      value.remove('codes');
    } else {
      value['codes'] = selected.toList()..sort();
    }
    _setDomain(domain, value);
  }

  Widget _buildReview() {
    final answered = _questions
        .where(
          (question) => RecommendationQuestionnaire.isComplete(
            _profile,
            question,
            today: _today,
          ),
        )
        .length;
    return Card(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 20, 16, 20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text(
              'Kiểm tra hồ sơ',
              style: TextStyle(fontSize: 22, fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 12),
            Text('Đã hoàn tất $answered/${_questions.length} câu hỏi.'),
            const SizedBox(height: 8),
            const Text(
              'Bạn có thể quay lại để chỉnh sửa. Hồ sơ chỉ được gửi một lần sau khi bạn bấm lưu.',
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildNavigation() {
    return SafeArea(
      top: false,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
        child: _isReview
            ? SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  onPressed: _saving ? null : _submit,
                  icon: const Icon(Icons.check),
                  label: Text(_saving ? 'Đang lưu...' : 'Lưu hồ sơ'),
                ),
              )
            : Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      key: const Key('recommendation-skip-button'),
                      onPressed: _saving ? null : _skipCurrent,
                      child: const Text('Bỏ qua'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: FilledButton(
                      key: const Key('recommendation-continue-button'),
                      onPressed: _saving ? null : _advance,
                      child: const Text('Tiếp tục'),
                    ),
                  ),
                ],
              ),
      ),
    );
  }

  void _goBack() {
    if (_questionIndex <= 0 || _saving) return;
    setState(() {
      _questionIndex--;
      _error = null;
    });
    _syncControllers();
  }

  Future<void> _skipCurrent() async {
    final question = _questions[_questionIndex];
    final next = RecommendationQuestionnaire.skipQuestion(_profile, question);
    _setProfile(next);
    setState(() {
      _error = null;
      _questionIndex++;
    });
    _syncControllers();
  }

  Future<void> _advance() async {
    if (_saving) return;
    final generation = _loadGeneration;
    final expectedUserId = AuthState.instance.userId;
    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      final valid = await _commitCurrentQuestion(
        generation: generation,
        expectedUserId: expectedUserId,
      );
      if (!valid || !_isCurrentOperation(generation, expectedUserId)) return;
      setState(() => _questionIndex++);
      _syncControllers();
    } finally {
      if (_isCurrentOperation(generation, expectedUserId)) {
        setState(() => _saving = false);
      }
    }
  }

  Future<bool> _commitCurrentQuestion({
    required int generation,
    required String? expectedUserId,
  }) async {
    final question = _questions[_questionIndex];
    if (question.kind == RecommendationQuestionKind.age) {
      final raw = _dobController.text.trim();
      if (!_isValidDate(raw, allowToday: false)) {
        setState(
          () => _error = 'Vui lòng nhập ngày sinh hợp lệ trước hôm nay.',
        );
        return false;
      }
      try {
        if (_accountDateOfBirth != raw) {
          await _service.updateDateOfBirth(raw);
          if (!_isCurrentOperation(generation, expectedUserId)) return false;
        }
      } on ApiException catch (error) {
        if (_isCurrentOperation(generation, expectedUserId)) {
          setState(() => _error = _messageForApi(error));
        }
        return false;
      } catch (_) {
        if (_isCurrentOperation(generation, expectedUserId)) {
          setState(
            () => _error = 'Không thể cập nhật ngày sinh. Vui lòng thử lại.',
          );
        }
        return false;
      }
      _accountDateOfBirth = raw;
      _setDomain('age', <String, dynamic>{'state': 'KNOWN'});
      return true;
    }

    if (question.kind == RecommendationQuestionKind.bmi) {
      final height = double.tryParse(_heightController.text.trim());
      final weight = double.tryParse(_weightController.text.trim());
      final measuredOn = _formatDate(_today);
      final context = _defaultWeightContext;
      if (height == null ||
          weight == null ||
          !height.isFinite ||
          !weight.isFinite ||
          height < 100 ||
          height > 250 ||
          weight < 20 ||
          weight > 300 ||
          !_oneDecimal(height) ||
          !_oneDecimal(weight) ||
          !_allowedContexts.contains(context)) {
        setState(() => _error = 'Vui lòng hoàn tất các trường BMI hợp lệ.');
        return false;
      }
      _setDomain('bmi', <String, dynamic>{
        'state': 'KNOWN',
        'heightCm': height,
        'weightKg': weight,
        'weightContext': context,
        'measuredOn': measuredOn,
      });
      return true;
    }

    if (!RecommendationQuestionnaire.isComplete(
      _profile,
      question,
      today: _today,
    )) {
      setState(() => _error = 'Vui lòng chọn một lựa chọn hoặc bấm Bỏ qua.');
      return false;
    }
    _profile = RecommendationQuestionnaire.normalizeProfile(_profile);
    unawaited(_saveDraftBestEffort(_profile));
    return true;
  }

  bool _oneDecimal(num value) {
    if (!value.isFinite) return false;
    final text = value.toString();
    final point = text.indexOf('.');
    return point < 0 || text.length - point - 1 <= 1;
  }

  bool _isValidDate(String value, {required bool allowToday}) {
    if (!RegExp(r'^\d{4}-\d{2}-\d{2}$').hasMatch(value)) return false;
    final parsed = DateTime.tryParse(value);
    final parts = value.split('-').map(int.parse).toList(growable: false);
    if (parsed == null ||
        parsed.year != parts[0] ||
        parsed.month != parts[1] ||
        parsed.day != parts[2] ||
        parsed.year < 1900) {
      return false;
    }
    if (allowToday) {
      return !parsed.isAfter(_today);
    }
    return parsed.isBefore(_today);
  }

  Future<void> _pickDateOfBirth() async {
    final generation = _loadGeneration;
    final expectedUserId = AuthState.instance.userId;
    final current = DateTime.tryParse(_dobController.text.trim());
    final firstDate = DateTime(1900);
    final lastDate = _today.subtract(const Duration(days: 1));
    final initial = current != null && current.year >= 1900
        ? current
        : DateTime(_today.year - 25, _today.month, _today.day);
    final picked = await showDatePicker(
      context: context,
      firstDate: firstDate,
      lastDate: lastDate,
      initialDate: initial.isBefore(firstDate)
          ? firstDate
          : initial.isAfter(lastDate)
          ? lastDate
          : initial,
    );
    if (picked == null || !_isCurrentOperation(generation, expectedUserId)) {
      return;
    }
    _dobController.text = _formatDate(picked);
  }

  String _formatDate(DateTime value) =>
      '${value.year.toString().padLeft(4, '0')}-'
      '${value.month.toString().padLeft(2, '0')}-'
      '${value.day.toString().padLeft(2, '0')}';

  Future<void> _decline() async {
    if (_saving) return;
    final expectedUserId = AuthState.instance.userId;
    final generation = _loadGeneration;
    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      await _service.decline();
      if (_isCurrentOperation(generation, expectedUserId)) {
        if (!mounted) return;
        context.go('/mother-home');
      }
    } on ApiException catch (error) {
      if (_isCurrentOperation(generation, expectedUserId)) {
        setState(() => _error = _messageForApi(error));
      }
    } catch (_) {
      if (_isCurrentOperation(generation, expectedUserId)) {
        setState(() => _error = 'Không thể lưu lựa chọn. Vui lòng thử lại.');
      }
    } finally {
      if (_isCurrentOperation(generation, expectedUserId)) {
        setState(() => _saving = false);
      }
    }
  }

  Future<void> _submit() async {
    final normalized = RecommendationQuestionnaire.normalizeProfile(_profile);
    final age = normalized['age'];
    if (age is Map &&
        age['state'] == 'KNOWN' &&
        !_isValidDate(_accountDateOfBirth ?? '', allowToday: false)) {
      setState(
        () => _error = 'Vui lòng cập nhật ngày sinh trước khi lưu hồ sơ.',
      );
      return;
    }
    if (!_questions.every(
      (question) => RecommendationQuestionnaire.isComplete(
        normalized,
        question,
        today: _today,
      ),
    )) {
      setState(() => _error = 'Vui lòng quay lại hoàn tất các câu còn thiếu.');
      return;
    }
    if (_saving || AuthState.instance.userId == null) return;
    final expectedUserId = AuthState.instance.userId;
    final generation = _loadGeneration;
    setState(() {
      _saving = true;
      _error = null;
      _profile = normalized;
    });
    try {
      await _service.putProfile(
        profile: normalized,
        submissionId: _submissionId,
      );
      if (!_isCurrentOperation(generation, expectedUserId)) return;
      if (expectedUserId != null) {
        try {
          await _service.clearDraftFor(expectedUserId);
        } catch (_) {
          // The server profile is already committed. A local cleanup failure
          // must not report a false submission failure or trigger a duplicate.
        }
      }
      if (!_isCurrentOperation(generation, expectedUserId)) return;
      if (!mounted) return;
      context.go('/mother-home');
    } on ApiException catch (error) {
      if (_isCurrentOperation(generation, expectedUserId)) {
        setState(() => _error = _messageForApi(error));
      }
    } catch (_) {
      if (_isCurrentOperation(generation, expectedUserId)) {
        setState(() => _error = 'Hồ sơ chưa được lưu. Vui lòng thử lại.');
      }
    } finally {
      if (_isCurrentOperation(generation, expectedUserId)) {
        setState(() => _saving = false);
      }
    }
  }

  String _messageForApi(ApiException error) {
    switch (error.errorCode) {
      case 'RECOMMENDATION_JOURNEY_REQUIRED':
        return 'Vui lòng hoàn tất thiết lập hành trình trước khi mở hồ sơ này.';
      case 'RECOMMENDATION_PROFILE_INVALID':
        return 'Một số lựa chọn không hợp lệ. Vui lòng kiểm tra lại.';
      case 'RECOMMENDATION_SUBMISSION_CONFLICT':
        return 'Hồ sơ đã thay đổi ở phiên khác. Vui lòng tải lại rồi thử lại.';
      case 'RECOMMENDATION_POLICY_MISMATCH':
        return 'Chính sách cá nhân hóa đã thay đổi. Vui lòng tải lại biểu mẫu.';
      case 'RECOMMENDATION_PROFILE_REVIEW_REQUIRED':
        return 'Hồ sơ cần được xem lại theo giai đoạn hiện tại trước khi lưu.';
      case 'RECOMMENDATION_CONTEXT_UNAVAILABLE':
        return 'Chưa thể xác định ngữ cảnh hành trình an toàn. Vui lòng thử lại sau.';
      case 'PRF-002':
        return 'Ngày sinh phải từ 01/01/1900 và trước hôm nay.';
      default:
        return error.statusCode >= 500
            ? 'Máy chủ tạm thời không sẵn sàng. Vui lòng thử lại.'
            : 'Không thể hoàn tất yêu cầu. Vui lòng thử lại.';
    }
  }

  Future<void> _saveDraftBestEffort(Map<String, dynamic> value) async {
    try {
      await _service.saveDraft(value);
    } catch (_) {
      // Draft persistence is best effort; the in-memory profile remains the
      // source for the current submission and API errors are handled there.
    }
  }

  Future<void> _clearDraftBestEffort(String userId) async {
    try {
      await _service.clearDraftFor(userId);
    } catch (_) {
      // Account changes must never surface a storage cleanup failure as a
      // questionnaire error for the newly authenticated account.
    }
  }
}
