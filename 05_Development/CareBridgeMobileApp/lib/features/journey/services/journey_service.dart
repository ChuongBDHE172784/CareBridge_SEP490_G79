import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../../../core/auth/auth_state.dart';
import '../../../core/network/api_client.dart';
import '../models/journey_model.dart';

class JourneyDashboardReconciler {
  const JourneyDashboardReconciler._();

  static bool matches(JourneyDashboard dashboard, JourneyDashboard fallback) {
    if (!dashboard.hasActiveJourney ||
        dashboard.journeyId != fallback.journeyId ||
        dashboard.journeyType != fallback.journeyType) {
      return false;
    }
    return _sameDate(dashboard.estimatedDueDate, fallback.estimatedDueDate) &&
        _sameDate(dashboard.lastMenstrualDate, fallback.lastMenstrualDate);
  }

  static bool shouldUse(
    JourneyDashboard dashboard,
    JourneyDashboard? fallback, {
    required bool pendingSync,
  }) {
    if (fallback == null) return false;
    if (pendingSync) {
      if (!dashboard.hasActiveJourney) return true;
      if (dashboard.journeyId == fallback.journeyId &&
          !matches(dashboard, fallback)) {
        return true;
      }
    }
    if (!dashboard.hasActiveJourney) return false;
    return dashboard.journeyId == fallback.journeyId &&
        dashboard.isPregnancy &&
        fallback.isPregnancy &&
        dashboard.effectivePregnancyWeek == null &&
        fallback.effectivePregnancyWeek != null;
  }

  static DateTime? updatedLastMenstrualDate({
    required bool responseContainsField,
    required DateTime? responseValue,
    required DateTime? requestValue,
    required DateTime? requestEstimatedDueDate,
    required DateTime? fallbackValue,
  }) {
    if (responseContainsField) return responseValue;
    if (requestValue != null) return requestValue;
    if (requestEstimatedDueDate != null) return null;
    return fallbackValue;
  }

  static bool canApplyResponse(String? requestUserId, String? currentUserId) =>
      requestUserId != null && requestUserId == currentUserId;

  static bool _sameDate(DateTime? left, DateTime? right) =>
      _formatDate(left) == _formatDate(right);

  static String? _formatDate(DateTime? value) {
    if (value == null) return null;
    final y = value.year.toString().padLeft(4, '0');
    final m = value.month.toString().padLeft(2, '0');
    final d = value.day.toString().padLeft(2, '0');
    return '$y-$m-$d';
  }
}

class JourneyService {
  static const _storage = FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );
  static const _legacyOptimisticDashboardKey =
      'cb_journey_optimistic_dashboard';
  static const _optimisticDashboardKeyPrefix =
      'cb_journey_optimistic_dashboard';
  static final ValueNotifier<int> dashboardRevision = ValueNotifier<int>(0);

  static JourneyDashboard? _optimisticDashboard;
  static String? _optimisticDashboardUserId;
  static bool _optimisticDashboardPendingSync = false;

  static JourneyDashboard? get _scopedOptimisticDashboard {
    final userId = AuthState.instance.userId;
    if (userId == null || _optimisticDashboardUserId != userId) {
      _optimisticDashboard = null;
      _optimisticDashboardUserId = null;
      _optimisticDashboardPendingSync = false;
      return null;
    }
    return _optimisticDashboard;
  }

  static Future<void> cachePregnancyDates({
    DateTime? estimatedDueDate,
    DateTime? lastMenstrualDate,
  }) async {
    if (estimatedDueDate == null && lastMenstrualDate == null) return;
    await _saveOptimisticDashboard(
      JourneyDashboard(
        journeyType: 'PREGNANCY',
        status: 'ACTIVE_PREGNANCY',
        estimatedDueDate: estimatedDueDate,
        lastMenstrualDate: lastMenstrualDate,
      ),
      pendingSync: true,
    );
  }

  static Future<void> clearOptimisticDashboard() async {
    final userIds = <String>{
      ?AuthState.instance.userId,
      ?_optimisticDashboardUserId,
    };
    _optimisticDashboard = null;
    _optimisticDashboardUserId = null;
    _optimisticDashboardPendingSync = false;
    for (final userId in userIds) {
      await _storage.delete(key: _storageKeyForUser(userId));
    }
    await _storage.delete(key: _legacyOptimisticDashboardKey);
  }

  Future<CreateJourneyResponse> createJourney(
    CreateJourneyRequest request,
  ) async {
    final requestUserId = AuthState.instance.userId;
    final data = await apiPost('/api/v1/journeys', request.toJson());
    if (!_isCurrentUser(requestUserId)) {
      throw StateError('Authenticated account changed during journey create');
    }
    final body = data['data'] as Map<String, dynamic>;
    final response = CreateJourneyResponse.fromJson(body);
    if (request.journeyType.isMaternalLifecycle) {
      final fallback = _scopedOptimisticDashboard;
      await _saveOptimisticDashboard(
        JourneyDashboard(
          journeyId: response.id,
          journeyType: response.journeyType,
          status: _dashboardStatusForJourneyType(
            response.journeyType,
            response.status,
          ),
          startDate:
              _parseDate(response.startDate) ?? _parseDate(request.startDate),
          estimatedDueDate:
              _parseDate(response.estimatedDueDate) ??
              _parseDate(request.estimatedDueDate),
          lastMenstrualDate:
              _parseDate(response.lastMenstrualDate) ??
              _parseDate(request.lastMenstrualDate) ??
              fallback?.lastMenstrualDate,
          version: response.version,
          dateSource: response.dateSource ?? request.dateSource,
          dateConfidence: response.dateConfidence ?? request.dateConfidence,
        ),
        pendingSync: true,
        expectedUserId: requestUserId,
      );
    } else {
      await clearOptimisticDashboard();
    }
    _notifyDashboardChanged();
    return response;
  }

  // UC-24: Get dashboard data for Home and Journey screens
  Future<JourneyDashboard> getDashboard() async {
    final requestUserId = AuthState.instance.userId;
    try {
      final data = await apiGet('/api/v1/journeys/me/dashboard');
      if (!_isCurrentUser(requestUserId)) {
        throw StateError(
          'Authenticated account changed during dashboard request',
        );
      }
      final dashboard = JourneyDashboard.fromJson(
        data['data'] as Map<String, dynamic>,
      );
      final fallback =
          _scopedOptimisticDashboard ?? await _readOptimisticDashboard();

      if (fallback != null &&
          _dashboardMatchesOptimistic(dashboard, fallback)) {
        _optimisticDashboardPendingSync = false;
      }

      if (!dashboard.hasActiveJourney &&
          fallback != null &&
          !_optimisticDashboardPendingSync) {
        await clearOptimisticDashboard();
        return dashboard;
      }

      if (_shouldUseOptimisticDashboard(dashboard, fallback)) {
        return _mergeDashboard(
          dashboard,
          fallback!,
          preferFallback: _optimisticDashboardPendingSync,
        );
      }

      if (dashboard.hasActiveJourney) {
        await _saveOptimisticDashboard(
          dashboard,
          expectedUserId: requestUserId,
        );
      }
      return dashboard;
    } catch (_) {
      if (!_isCurrentUser(requestUserId)) rethrow;
      final fallback =
          _scopedOptimisticDashboard ?? await _readOptimisticDashboard();
      if (fallback != null) return fallback;
      rethrow;
    }
  }

  Future<List<JourneyTransition>> getHistory(String journeyId) async {
    final requestUserId = AuthState.instance.userId;
    final transitions = <JourneyTransition>[];
    var page = 0;
    var totalPages = 1;
    do {
      final data = await apiGet(
        '/api/v1/journeys/$journeyId/history?page=$page&size=100',
      );
      if (!_isCurrentUser(requestUserId)) {
        throw StateError(
          'Authenticated account changed during history request',
        );
      }
      final payload = data['data'];
      if (payload is List<dynamic>) {
        transitions.addAll(
          payload.map(
            (item) => JourneyTransition.fromJson(item as Map<String, dynamic>),
          ),
        );
        break;
      }
      final pageData = payload as Map<String, dynamic>? ?? const {};
      final items = pageData['items'] as List<dynamic>? ?? const [];
      transitions.addAll(
        items.map(
          (item) => JourneyTransition.fromJson(item as Map<String, dynamic>),
        ),
      );
      totalPages = (pageData['totalPages'] as num?)?.toInt() ?? 1;
      page++;
    } while (page < totalPages);
    return List.unmodifiable(transitions);
  }

  Future<PregnancyOutcomeResult> recordPregnancyOutcome(
    String journeyId,
    RecordPregnancyOutcomeRequest request,
  ) async {
    final requestUserId = AuthState.instance.userId;
    final data = await apiPost(
      '/api/v1/journeys/$journeyId/pregnancy-outcomes',
      request.toJson(),
    );
    if (!_isCurrentUser(requestUserId)) {
      throw StateError(
        'Authenticated account changed during pregnancy outcome update',
      );
    }
    final result = PregnancyOutcomeResult.fromJson(
      data['data'] as Map<String, dynamic>,
    );
    final prior =
        _scopedOptimisticDashboard ?? await _readOptimisticDashboard();
    await _saveOptimisticDashboard(
      JourneyDashboard(
        journeyId: result.journeyId,
        journeyType: result.journeyType,
        status: _dashboardStatusForJourneyType(
          result.journeyType,
          result.journeyType == 'POSTPARTUM'
              ? 'ACTIVE_POSTPARTUM'
              : 'ACTIVE_PREGNANCY',
        ),
        version: result.journeyVersion,
        estimatedDueDate: prior?.estimatedDueDate,
        lastMenstrualDate: prior?.lastMenstrualDate,
        startDate: prior?.startDate,
        dateSource: prior?.dateSource,
        dateConfidence: prior?.dateConfidence,
        pregnancyOutcome: result.outcomeType,
        pregnancyOutcomeDate: result.outcomeDate,
      ),
      pendingSync: true,
      expectedUserId: requestUserId,
    );
    _notifyDashboardChanged();
    return result;
  }

  // UC-23: Update Journey
  Future<void> updateJourney(
    String journeyId,
    UpdateJourneyRequest request,
  ) async {
    final requestUserId = AuthState.instance.userId;
    final data = await apiPut('/api/v1/journeys/$journeyId', request.toJson());
    if (!_isCurrentUser(requestUserId)) {
      throw StateError('Authenticated account changed during journey update');
    }
    final body = data?['data'] as Map<String, dynamic>?;
    final journeyType =
        body?['journeyType'] as String? ?? request.journeyType?.toApiValue();
    final estimatedDueDate =
        _parseDate(body?['estimatedDueDate'] as String?) ??
        _parseDate(request.estimatedDueDate);
    final responseContainsLmp = body?.containsKey('lastMenstrualDate') == true;
    final responseLmp = _parseDate(body?['lastMenstrualDate'] as String?);
    final requestLmp = _parseDate(request.lastMenstrualDate);
    if (estimatedDueDate != null || responseContainsLmp || requestLmp != null) {
      final fallback = _scopedOptimisticDashboard;
      final lastMenstrualDate =
          JourneyDashboardReconciler.updatedLastMenstrualDate(
            responseContainsField: responseContainsLmp,
            responseValue: responseLmp,
            requestValue: requestLmp,
            requestEstimatedDueDate: _parseDate(request.estimatedDueDate),
            fallbackValue: fallback?.lastMenstrualDate,
          );
      await _saveOptimisticDashboard(
        JourneyDashboard(
          journeyId:
              body?['journeyId'] as String? ??
              body?['id'] as String? ??
              journeyId,
          journeyType: journeyType ?? 'PREGNANCY',
          status: _dashboardStatusForJourneyType(
            journeyType,
            'ACTIVE_PREGNANCY',
          ),
          estimatedDueDate: estimatedDueDate ?? fallback?.estimatedDueDate,
          lastMenstrualDate: lastMenstrualDate,
          startDate:
              _parseDate(body?['startDate'] as String?) ?? fallback?.startDate,
          version: (body?['version'] as num?)?.toInt(),
          dateSource: body?['dateSource'] as String? ?? request.dateSource,
          dateConfidence:
              body?['dateConfidence'] as String? ?? request.dateConfidence,
        ),
        pendingSync: true,
        expectedUserId: requestUserId,
      );
    }
    _notifyDashboardChanged();
  }

  static void _notifyDashboardChanged() {
    dashboardRevision.value++;
  }

  static bool _shouldUseOptimisticDashboard(
    JourneyDashboard dashboard,
    JourneyDashboard? fallback,
  ) => JourneyDashboardReconciler.shouldUse(
    dashboard,
    fallback,
    pendingSync: _optimisticDashboardPendingSync,
  );

  static bool _dashboardMatchesOptimistic(
    JourneyDashboard dashboard,
    JourneyDashboard fallback,
  ) => JourneyDashboardReconciler.matches(dashboard, fallback);

  static String _dashboardStatusForJourneyType(
    String? journeyType,
    String fallback,
  ) {
    switch (journeyType) {
      case 'PREGNANCY':
        return 'ACTIVE_PREGNANCY';
      case 'POSTPARTUM':
        return 'ACTIVE_POSTPARTUM';
      case 'BABY_CARE':
        return 'BABY_CARE';
      case 'PRE_PREGNANCY':
        return 'PRE_PREGNANCY';
      default:
        return fallback;
    }
  }

  static JourneyDashboard _mergeDashboard(
    JourneyDashboard dashboard,
    JourneyDashboard fallback, {
    bool preferFallback = false,
  }) {
    final useFallbackPregnancyStage =
        preferFallback || (fallback.isPregnancy && !dashboard.isPregnancy);
    return JourneyDashboard(
      journeyId: dashboard.journeyId ?? fallback.journeyId,
      journeyType: useFallbackPregnancyStage
          ? fallback.journeyType
          : dashboard.journeyType ?? fallback.journeyType,
      status: useFallbackPregnancyStage
          ? fallback.status
          : dashboard.status ?? fallback.status,
      pregnancyWeek: dashboard.pregnancyWeek ?? fallback.pregnancyWeek,
      trimester: dashboard.trimester ?? fallback.trimester,
      daysUntilDue: dashboard.daysUntilDue ?? fallback.daysUntilDue,
      estimatedDueDate: preferFallback
          ? fallback.estimatedDueDate ?? dashboard.estimatedDueDate
          : dashboard.estimatedDueDate ?? fallback.estimatedDueDate,
      lastMenstrualDate: preferFallback
          ? fallback.lastMenstrualDate
          : dashboard.lastMenstrualDate ?? fallback.lastMenstrualDate,
      startDate: dashboard.startDate ?? fallback.startDate,
      version: dashboard.version ?? fallback.version,
      dateSource: dashboard.dateSource ?? fallback.dateSource,
      dateConfidence: dashboard.dateConfidence ?? fallback.dateConfidence,
      pregnancyOutcome: dashboard.pregnancyOutcome ?? fallback.pregnancyOutcome,
      pregnancyOutcomeDate:
          dashboard.pregnancyOutcomeDate ?? fallback.pregnancyOutcomeDate,
    );
  }

  static DateTime? _parseDate(String? value) {
    if (value == null) return null;
    return DateTime.tryParse(value);
  }

  static Future<void> _saveOptimisticDashboard(
    JourneyDashboard dashboard, {
    bool pendingSync = false,
    String? expectedUserId,
  }) async {
    final userId = expectedUserId ?? AuthState.instance.userId;
    if (userId == null || !_isCurrentUser(userId)) return;
    await _storage.write(
      key: _storageKeyForUser(userId),
      value: jsonEncode({
        'userId': userId,
        'journeyId': dashboard.journeyId,
        'journeyType': dashboard.journeyType,
        'status': dashboard.status,
        'pregnancyWeek': dashboard.pregnancyWeek,
        'trimester': dashboard.trimester,
        'daysUntilDue': dashboard.daysUntilDue,
        'estimatedDueDate': _formatDate(dashboard.estimatedDueDate),
        'lastMenstrualDate': _formatDate(dashboard.lastMenstrualDate),
        'startDate': _formatDate(dashboard.startDate),
        'version': dashboard.version,
        'dateSource': dashboard.dateSource,
        'dateConfidence': dashboard.dateConfidence,
        'pregnancyOutcome': dashboard.pregnancyOutcome?.apiValue,
        'pregnancyOutcomeDate': _formatDate(dashboard.pregnancyOutcomeDate),
        'pendingSync': pendingSync,
      }),
    );
    if (!_isCurrentUser(userId)) return;
    _optimisticDashboard = dashboard;
    _optimisticDashboardUserId = userId;
    _optimisticDashboardPendingSync = pendingSync;
  }

  static Future<JourneyDashboard?> _readOptimisticDashboard() async {
    final userId = AuthState.instance.userId;
    if (userId == null) return null;

    final raw = await _storage.read(key: _storageKeyForUser(userId));
    if (raw == null) return null;

    try {
      final data = jsonDecode(raw) as Map<String, dynamic>;
      if (data['userId'] != userId) {
        await clearOptimisticDashboard();
        return null;
      }

      final dashboard = JourneyDashboard(
        journeyId: data['journeyId'] as String?,
        journeyType: data['journeyType'] as String?,
        status: data['status'] as String?,
        pregnancyWeek: (data['pregnancyWeek'] as num?)?.toInt(),
        trimester: (data['trimester'] as num?)?.toInt(),
        daysUntilDue: (data['daysUntilDue'] as num?)?.toInt(),
        estimatedDueDate: _parseDate(data['estimatedDueDate'] as String?),
        lastMenstrualDate: _parseDate(data['lastMenstrualDate'] as String?),
        startDate: _parseDate(data['startDate'] as String?),
        version: (data['version'] as num?)?.toInt(),
        dateSource: data['dateSource'] as String?,
        dateConfidence: data['dateConfidence'] as String?,
        pregnancyOutcome: data['pregnancyOutcome'] == null
            ? null
            : PregnancyOutcome.fromApiValue(data['pregnancyOutcome'] as String),
        pregnancyOutcomeDate: _parseDate(
          data['pregnancyOutcomeDate'] as String?,
        ),
      );
      _optimisticDashboard = dashboard;
      _optimisticDashboardUserId = userId;
      _optimisticDashboardPendingSync = data['pendingSync'] == true;
      return dashboard;
    } catch (_) {
      await clearOptimisticDashboard();
      return null;
    }
  }

  static String? _formatDate(DateTime? value) {
    if (value == null) return null;
    final y = value.year.toString().padLeft(4, '0');
    final m = value.month.toString().padLeft(2, '0');
    final d = value.day.toString().padLeft(2, '0');
    return '$y-$m-$d';
  }

  static String _storageKeyForUser(String userId) =>
      '${_optimisticDashboardKeyPrefix}_$userId';

  static bool _isCurrentUser(String? expectedUserId) =>
      JourneyDashboardReconciler.canApplyResponse(
        expectedUserId,
        AuthState.instance.userId,
      );
}
