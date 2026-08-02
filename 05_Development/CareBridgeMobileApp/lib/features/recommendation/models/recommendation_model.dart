enum RecommendationProfileStatus {
  notStarted,
  active,
  declined,
  reviewRequired,
  reconsentRequired,
  revoked;

  static RecommendationProfileStatus fromApi(String? value) {
    switch (value) {
      case 'ACTIVE':
        return active;
      case 'DECLINED':
        return declined;
      case 'REVIEW_REQUIRED':
        return reviewRequired;
      case 'RECONSENT_REQUIRED':
        return reconsentRequired;
      case 'REVOKED':
        return revoked;
      default:
        return notStarted;
    }
  }

  String get apiValue => switch (this) {
    notStarted => 'NOT_STARTED',
    active => 'ACTIVE',
    declined => 'DECLINED',
    reviewRequired => 'REVIEW_REQUIRED',
    reconsentRequired => 'RECONSENT_REQUIRED',
    revoked => 'REVOKED',
  };
}

class RecommendationProfileResponse {
  final RecommendationProfileStatus status;
  final bool requiresAction;
  final bool profileComplete;
  final int schemaVersion;
  final int profileRevision;
  final DateTime? completedAt;
  final Map<String, dynamic>? profile;
  final Map<String, dynamic>? derived;

  const RecommendationProfileResponse({
    required this.status,
    required this.requiresAction,
    required this.profileComplete,
    required this.schemaVersion,
    required this.profileRevision,
    required this.completedAt,
    required this.profile,
    required this.derived,
  });

  factory RecommendationProfileResponse.fromJson(Map<String, dynamic> json) {
    return RecommendationProfileResponse(
      status: RecommendationProfileStatus.fromApi(json['status'] as String?),
      requiresAction: json['requiresAction'] == true,
      profileComplete: json['profileComplete'] == true,
      schemaVersion: (json['schemaVersion'] as num?)?.toInt() ?? 0,
      profileRevision: (json['profileRevision'] as num?)?.toInt() ?? 0,
      completedAt: DateTime.tryParse(json['completedAt'] as String? ?? ''),
      profile: (json['profile'] as Map?)?.cast<String, dynamic>(),
      derived: (json['derived'] as Map?)?.cast<String, dynamic>(),
    );
  }
}

enum RecommendationSelectionType { targeted, fallback }

class RecommendationContentItem {
  final int rank;
  final RecommendationSelectionType selectionType;
  final String reasonCode;
  final String reasonLabel;
  final String id;
  final String title;
  final String? summary;
  final String stage;

  const RecommendationContentItem({
    required this.rank,
    required this.selectionType,
    this.reasonCode = 'LIFECYCLE_FALLBACK',
    required this.reasonLabel,
    required this.id,
    required this.title,
    required this.summary,
    required this.stage,
  });

  factory RecommendationContentItem.fromJson(Map<String, dynamic> json) {
    final content =
        (json['content'] as Map?)?.cast<String, dynamic>() ?? const {};
    final reasonCode = json['reasonCode'] as String? ?? 'LIFECYCLE_FALLBACK';
    return RecommendationContentItem(
      rank: (json['rank'] as num?)?.toInt() ?? 0,
      selectionType: json['selectionType'] == 'TARGETED'
          ? RecommendationSelectionType.targeted
          : RecommendationSelectionType.fallback,
      reasonCode: reasonCode,
      reasonLabel: reasonCode == 'PERSONALIZED_CONTEXT'
          ? 'Selected for your current care context'
          : 'Useful for your current stage',
      id: content['id'] as String? ?? '',
      title: content['title'] as String? ?? '',
      summary: content['summary'] as String?,
      stage: content['stage'] as String? ?? '',
    );
  }
}

class RecommendationContentResponse {
  final String stage;
  final int? pregnancyWeek;
  final String weekEligibilityMode;
  final RecommendationProfileStatus profileStatus;
  final String selectionMode;
  final String coverageStatus;
  final bool fallbackUsed;
  final List<RecommendationContentItem> items;

  const RecommendationContentResponse({
    required this.stage,
    required this.pregnancyWeek,
    required this.weekEligibilityMode,
    required this.profileStatus,
    required this.selectionMode,
    required this.coverageStatus,
    required this.fallbackUsed,
    required this.items,
  });

  factory RecommendationContentResponse.fromJson(
    Map<String, dynamic> json,
  ) => RecommendationContentResponse(
    stage: json['stage'] as String? ?? '',
    pregnancyWeek: (json['pregnancyWeek'] as num?)?.toInt(),
    weekEligibilityMode:
        json['weekEligibilityMode'] as String? ?? 'NOT_APPLICABLE',
    profileStatus: RecommendationProfileStatus.fromApi(
      json['profileStatus'] as String?,
    ),
    selectionMode: json['selectionMode'] as String? ?? 'EMPTY',
    coverageStatus: json['coverageStatus'] as String? ?? 'EMPTY',
    fallbackUsed: json['fallbackUsed'] == true,
    items: ((json['items'] as List?) ?? const [])
        .whereType<Map>()
        .map(
          (item) =>
              RecommendationContentItem.fromJson(item.cast<String, dynamic>()),
        )
        // A malformed/stale item must never create a card that can navigate
        // with an empty content id or render an empty title. The API is the
        // source of truth; the client fails closed for an unusable item.
        .where(
          (item) => item.id.trim().isNotEmpty && item.title.trim().isNotEmpty,
        )
        .toList(growable: false),
  );
}

class RecommendationProfileDraft {
  static const policyVersion = 'MOTHER_PERSONALIZED_CONTENT_V1';
  static const schemaVersion = 1;

  static Map<String, dynamic> empty() => {
    'age': {'state': 'UNKNOWN'},
    'bmi': {'state': 'UNKNOWN'},
    'reproductiveHistory': {'state': 'UNKNOWN'},
    'underlyingConditions': {'state': 'UNKNOWN'},
    'lifestyle': {
      'smoking': {'state': 'UNKNOWN'},
      'alcohol': {'state': 'UNKNOWN'},
      'physicalActivity': {'state': 'UNKNOWN'},
      'sleep': {'state': 'UNKNOWN'},
    },
    'nutrition': {'state': 'UNKNOWN'},
    'vaccination': <String, dynamic>{
      'answers': <Map<String, dynamic>>[
        for (final code in [
          'INFLUENZA',
          'COVID_19',
          'TDAP',
          'HEPATITIS_B',
          'RUBELLA_IMMUNITY',
        ])
          <String, dynamic>{'code': code, 'state': 'UNKNOWN'},
      ],
    },
    'currentMedications': {'state': 'UNKNOWN'},
    'sexualHealth': {'state': 'UNKNOWN'},
    'sti': {'state': 'UNKNOWN'},
  };

  static Map<String, dynamic> copyProfile(Map<String, dynamic>? source) {
    final result = _deepMerge(empty(), source ?? const {});
    return _repair(result);
  }

  /// Applies a recoverable local draft over the last server profile while
  /// retaining server values for domains that a partial draft did not touch.
  /// A malformed/empty draft therefore cannot erase an otherwise valid profile.
  static Map<String, dynamic> mergeProfiles(
    Map<String, dynamic>? server,
    Map<String, dynamic>? draft,
  ) {
    final result = _deepMerge(empty(), server ?? const {});
    final serverVaccination = result['vaccination'];
    final serverAnswers = serverVaccination is Map
        ? _copyAnswerList(serverVaccination['answers'])
        : <Map<String, dynamic>>[];
    if (draft != null && draft.isNotEmpty) {
      _deepMergeInto(result, draft);
      final draftVaccination = draft['vaccination'];
      if (draftVaccination is Map && draftVaccination.containsKey('answers')) {
        final targetVaccination = result['vaccination'];
        if (targetVaccination is Map && draftVaccination['answers'] is List) {
          targetVaccination['answers'] = _mergeAnswerLists(
            serverAnswers,
            _copyAnswerList(draftVaccination['answers']),
          );
        } else if (targetVaccination is Map) {
          // A malformed draft cannot erase server vaccination answers.
          targetVaccination['answers'] = serverAnswers;
        }
      }
    }
    return _repair(result);
  }

  static List<Map<String, dynamic>> _copyAnswerList(dynamic raw) {
    if (raw is! List) return <Map<String, dynamic>>[];
    return raw
        .whereType<Map>()
        .map((answer) => Map<String, dynamic>.from(answer))
        .where((answer) => answer['code'] is String)
        .toList();
  }

  static List<Map<String, dynamic>> _mergeAnswerLists(
    List<Map<String, dynamic>> base,
    List<Map<String, dynamic>> override,
  ) {
    final byCode = <String, Map<String, dynamic>>{
      for (final answer in base) answer['code'] as String: answer,
    };
    for (final answer in override) {
      byCode[answer['code'] as String] = answer;
    }
    return byCode.values.toList();
  }

  static Map<String, dynamic> _repair(Map<String, dynamic> result) {
    final vaccination = result['vaccination'];
    if (vaccination is Map) {
      final baseAnswers = (empty()['vaccination'] as Map)['answers'] as List;
      final provided = vaccination['answers'] is List
          ? (vaccination['answers'] as List)
                .whereType<Map>()
                .where((answer) => answer['code'] is String)
                .map((answer) => Map<String, dynamic>.from(answer))
                .toList()
          : <Map<String, dynamic>>[];
      final byCode = <String, Map<String, dynamic>>{
        for (final answer in provided) answer['code'] as String: answer,
      };
      vaccination['answers'] = baseAnswers
          .whereType<Map>()
          .map(
            (answer) =>
                byCode[answer['code']] ?? Map<String, dynamic>.from(answer),
          )
          .toList();
    }
    return result;
  }

  static void _deepMergeInto(
    Map<String, dynamic> target,
    Map<String, dynamic> override,
  ) {
    override.forEach((key, value) {
      final current = target[key];
      if (current is Map && value is Map) {
        final nested = <String, dynamic>{};
        current.forEach((nestedKey, nestedValue) {
          nested[nestedKey.toString()] = nestedValue;
        });
        _deepMergeInto(nested, value.map((k, v) => MapEntry(k.toString(), v)));
        target[key] = nested;
      } else if (current is Map && value is! Map) {
        // Keep the repairable server/default object for malformed draft data.
      } else {
        target[key] = value;
      }
    });
  }

  static Map<String, dynamic> _deepMerge(
    Map<String, dynamic> base,
    Map<String, dynamic> override,
  ) {
    final result = Map<String, dynamic>.from(base);
    override.forEach((key, value) {
      final current = result[key];
      if (current is Map && value is Map) {
        result[key] = _deepMerge(
          Map<String, dynamic>.from(current),
          value.map((k, v) => MapEntry(k.toString(), v)),
        );
      } else if (current is Map && value is! Map) {
        // Keep the repairable default for malformed nested draft data.
      } else {
        result[key] = value;
      }
    });
    return result;
  }
}

bool recommendationProfileHasAllDomains(Map<String, dynamic> profile) => const [
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
].every(profile.containsKey);
