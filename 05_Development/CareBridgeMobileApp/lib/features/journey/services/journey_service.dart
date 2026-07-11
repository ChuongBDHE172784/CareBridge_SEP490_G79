import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../../../core/auth/auth_state.dart';
import '../../../core/network/api_client.dart';
import '../models/journey_model.dart';

class JourneyService {
  static const _storage = FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );
  static const _optimisticDashboardKey = 'cb_journey_optimistic_dashboard';

  static JourneyDashboard? _optimisticDashboard;

  static Future<void> cachePregnancyDates({
    DateTime? estimatedDueDate,
    DateTime? lastMenstrualDate,
  }) async {
    if (estimatedDueDate == null && lastMenstrualDate == null) return;
    await _saveOptimisticDashboard(JourneyDashboard(
      journeyType: 'PREGNANCY',
      status: 'ACTIVE_PREGNANCY',
      estimatedDueDate: estimatedDueDate,
      lastMenstrualDate: lastMenstrualDate,
    ));
  }

  static Future<void> clearOptimisticDashboard() async {
    _optimisticDashboard = null;
    await _storage.delete(key: _optimisticDashboardKey);
  }

  Future<CreateJourneyResponse> createJourney(CreateJourneyRequest request) async {
    final data = await apiPost('/api/v1/journeys', request.toJson());
    final body = data['data'] as Map<String, dynamic>;
    final response = CreateJourneyResponse.fromJson(body);
    if (request.journeyType == JourneyType.pregnancy) {
      final fallback = _optimisticDashboard;
      await _saveOptimisticDashboard(JourneyDashboard(
        journeyId: response.id,
        journeyType: response.journeyType,
        status: response.status,
        estimatedDueDate:
            _parseDate(response.estimatedDueDate) ?? _parseDate(request.estimatedDueDate),
        lastMenstrualDate:
            _parseDate(response.lastMenstrualDate) ??
            _parseDate(request.lastMenstrualDate) ??
            fallback?.lastMenstrualDate,
      ));
    } else {
      await clearOptimisticDashboard();
    }
    return response;
  }

  // UC-24: Get dashboard data for Home and Journey screens
  Future<JourneyDashboard> getDashboard() async {
    try {
      final data = await apiGet('/api/v1/journeys/me/dashboard');
      final dashboard = JourneyDashboard.fromJson(data['data'] as Map<String, dynamic>);
      final fallback = _optimisticDashboard ?? await _readOptimisticDashboard();

      if (_shouldUseOptimisticDashboard(dashboard, fallback)) {
        return _mergeDashboard(dashboard, fallback!);
      }

      if (dashboard.hasActiveJourney) {
        await _saveOptimisticDashboard(dashboard);
      }
      return dashboard;
    } catch (_) {
      final fallback = _optimisticDashboard ?? await _readOptimisticDashboard();
      if (fallback != null) return fallback;
      rethrow;
    }
  }

  // UC-23: Update Journey
  Future<void> updateJourney(String journeyId, UpdateJourneyRequest request) async {
    await apiPut('/api/v1/journeys/$journeyId', request.toJson());
    final estimatedDueDate = _parseDate(request.estimatedDueDate);
    final lastMenstrualDate = _parseDate(request.lastMenstrualDate);
    if (estimatedDueDate != null || lastMenstrualDate != null) {
      await _saveOptimisticDashboard(JourneyDashboard(
        journeyId: journeyId,
        journeyType: 'PREGNANCY',
        status: 'ACTIVE_PREGNANCY',
        estimatedDueDate:
            estimatedDueDate ?? _optimisticDashboard?.estimatedDueDate,
        lastMenstrualDate: lastMenstrualDate ?? _optimisticDashboard?.lastMenstrualDate,
      ));
    }
  }

  static bool _shouldUseOptimisticDashboard(
    JourneyDashboard dashboard,
    JourneyDashboard? fallback,
  ) {
    if (fallback == null) return false;
    if (!dashboard.hasActiveJourney) return true;
    return dashboard.isPregnancy &&
        dashboard.effectivePregnancyWeek == null &&
        fallback.effectivePregnancyWeek != null;
  }

  static JourneyDashboard _mergeDashboard(
    JourneyDashboard dashboard,
    JourneyDashboard fallback,
  ) {
    return JourneyDashboard(
      journeyId: dashboard.journeyId ?? fallback.journeyId,
      journeyType: dashboard.journeyType ?? fallback.journeyType,
      status: dashboard.status ?? fallback.status,
      pregnancyWeek: dashboard.pregnancyWeek ?? fallback.pregnancyWeek,
      trimester: dashboard.trimester ?? fallback.trimester,
      daysUntilDue: dashboard.daysUntilDue ?? fallback.daysUntilDue,
      estimatedDueDate: dashboard.estimatedDueDate ?? fallback.estimatedDueDate,
      lastMenstrualDate: dashboard.lastMenstrualDate ?? fallback.lastMenstrualDate,
      startDate: dashboard.startDate ?? fallback.startDate,
    );
  }

  static DateTime? _parseDate(String? value) {
    if (value == null) return null;
    return DateTime.tryParse(value);
  }

  static Future<void> _saveOptimisticDashboard(JourneyDashboard dashboard) async {
    _optimisticDashboard = dashboard;
    final userId = AuthState.instance.userId;
    if (userId == null) return;
    await _storage.write(
      key: _optimisticDashboardKey,
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
      }),
    );
  }

  static Future<JourneyDashboard?> _readOptimisticDashboard() async {
    final userId = AuthState.instance.userId;
    if (userId == null) return null;

    final raw = await _storage.read(key: _optimisticDashboardKey);
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
      );
      _optimisticDashboard = dashboard;
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
}
