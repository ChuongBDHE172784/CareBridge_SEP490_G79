import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../../../core/auth/auth_state.dart';
import '../../../core/network/api_client.dart';
import '../models/journey_model.dart';

typedef JourneyApiMutation =
    Future<dynamic> Function(String path, Map<String, dynamic> body);

typedef JourneyDashboardCacheWriter =
    Future<void> Function(
      JourneyDashboard dashboard, {
      bool pendingSync,
      String? expectedUserId,
    });

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
    // An active pregnancy response is authoritative even when dating is
    // unresolved or quarantined. Never resurrect a cached source week/Plan
    // after the server has fail-closed the dating result.
    if (dashboard.hasActiveJourney &&
        dashboard.isPregnancy &&
        (dashboard.datingQuarantineReason != null ||
            dashboard.sourceWeekNumber == null ||
            dashboard.plan == null)) {
      return false;
    }
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
  JourneyService({
    JourneyApiMutation? apiPostOverride,
    JourneyApiMutation? apiPutOverride,
    JourneyDashboardCacheWriter? dashboardCacheWriterOverride,
    Future<void> Function()? clearOptimisticDashboardOverride,
    String? Function()? currentUserIdProvider,
  }) : _apiPost = apiPostOverride ?? _postJourneyV2,
       _apiPut = apiPutOverride ?? _putJourneyV2,
       _dashboardCacheWriter =
           dashboardCacheWriterOverride ?? _saveOptimisticDashboard,
       _clearOptimisticDashboard =
           clearOptimisticDashboardOverride ?? clearOptimisticDashboard,
       _currentUserIdProvider =
           currentUserIdProvider ?? (() => AuthState.instance.userId);

  final JourneyApiMutation _apiPost;
  final JourneyApiMutation _apiPut;
  final JourneyDashboardCacheWriter _dashboardCacheWriter;
  final Future<void> Function() _clearOptimisticDashboard;
  final String? Function() _currentUserIdProvider;

  static const _storage = FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );
  static const _legacyOptimisticDashboardKey =
      'cb_journey_optimistic_dashboard';
  static const _optimisticDashboardKeyPrefix =
      'cb_journey_optimistic_dashboard';
  static final ValueNotifier<int> dashboardRevision = ValueNotifier<int>(0);

  static Future<dynamic> _postJourneyV2(
    String path,
    Map<String, dynamic> body,
  ) => apiPost(
    path,
    body,
    extraHeaders: const {'X-Checklist-Contract-Version': '2'},
  );

  static Future<dynamic> _putJourneyV2(
    String path,
    Map<String, dynamic> body,
  ) => apiPut(
    path,
    body,
    extraHeaders: const {'X-Checklist-Contract-Version': '2'},
  );

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

  /// [BƯỚC 1: GỬI REQUEST TẠO MỚI HÀNH TRÌNH & THIẾT LẬP TUỔI THAI]
  /// Gửi dữ liệu ngày kinh cuối (LMP) hoặc ngày dự sinh (EDD) lên API backend `POST /api/v1/journeys`.
  /// Sau khi tạo thành công, tiến hành ghi đè cache lạc quan (Optimistic Cache) để UI lập tức hiển thị tuần thai.
  Future<CreateJourneyResponse> createJourney(
    CreateJourneyRequest request,
  ) async {
    final requestUserId = _currentUserIdProvider();
    // Gửi POST request kèm Header Contract Version 2 (XOR LMP/EDD)
    final data = await _apiPost('/api/v1/journeys', request.toJson());
    final body = data['data'] as Map<String, dynamic>;
    final response = CreateJourneyResponse.fromJson(body);
    _ensureRequestUserCurrent(requestUserId, 'journey create');
    
    // Đồng bộ và lưu dữ liệu tuổi thai tính toán từ server vào bộ nhớ Secure Storage
    await _reconcileAfterCommit('journey create', () async {
      if (request.journeyType.isMaternalLifecycle) {
        final fallback = _scopedOptimisticDashboard;
        await _dashboardCacheWriter(
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
            datingBasis: response.datingBasis ?? request.datingBasis,
            canonicalLmp: _parseDate(response.canonicalLmp),
            completedGestationalWeek: response.completedGestationalWeek,
            completedGestationalDays: response.completedGestationalDays,
            sourceWeekNumber: response.sourceWeekNumber,
            plan: response.plan,
            datingQuarantineReason: response.datingQuarantineReason,
          ),
          pendingSync: true,
          expectedUserId: requestUserId,
        );
      } else {
        await _clearOptimisticDashboard();
      }
    });
    _ensureRequestUserCurrent(requestUserId, 'journey create delivery');
    _notifyDashboardChanged();
    return response;
  }

  /// [BƯỚC 1: LẤY THÔNG TIN DASHBOARD & TÍNH TOÁN TUẦN THAI HIỆN HÀNH]
  /// Gọi endpoint `GET /api/v1/journeys/me/dashboard` để lấy tuổi thai chuẩn hóa từ server,
  /// tự động đối soát (reconciliation) với cache cục bộ và giải quyết trường hợp ngoại tuyến (offline fallback).
  Future<JourneyDashboard> getDashboard() async {
    final requestUserId = AuthState.instance.userId;
    try {
      // Gửi GET request lấy dữ liệu dashboard cá nhân của Mẹ
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

      // Kiểm tra tính tương thích giữa dữ liệu server trả về và cache lạc quan
      if (fallback != null &&
          _dashboardMatchesOptimistic(dashboard, fallback)) {
        _optimisticDashboardPendingSync = false;
      }

      // Nếu không có hành trình hoạt động, dọn dẹp cache
      if (!dashboard.hasActiveJourney &&
          fallback != null &&
          !_optimisticDashboardPendingSync) {
        await clearOptimisticDashboard();
        return dashboard;
      }

      // Trộn dữ liệu nếu cache lạc quan đang chứa bản cập nhật mới hơn
      if (_shouldUseOptimisticDashboard(dashboard, fallback)) {
        return _mergeDashboard(
          dashboard,
          fallback!,
          preferFallback: _optimisticDashboardPendingSync,
        );
      }

      // Lưu trữ dashboard hợp lệ vào Secure Storage
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

  Future<JourneyTimelinePage> getTimeline(
    String journeyId, {
    int page = 0,
    int size = 20,
  }) async {
    final requestUserId = AuthState.instance.userId;
    final data = await apiGet(
      '/api/v1/journeys/$journeyId/timeline?page=$page&size=$size',
    );
    if (!_isCurrentUser(requestUserId)) {
      throw StateError('Authenticated account changed during timeline request');
    }
    final payload = data['data'] as Map<String, dynamic>? ?? const {};
    return JourneyTimelinePage.fromJson(payload);
  }

  Future<PregnancyOutcomeResult> recordPregnancyOutcome(
    String journeyId,
    RecordPregnancyOutcomeRequest request,
  ) async {
    final requestUserId = _currentUserIdProvider();
    final data = await _apiPost(
      '/api/v1/journeys/$journeyId/pregnancy-outcomes',
      request.toJson(),
    );
    final result = PregnancyOutcomeResult.fromJson(
      data['data'] as Map<String, dynamic>,
    );
    _ensureRequestUserCurrent(requestUserId, 'pregnancy outcome update');
    await _reconcileAfterCommit('pregnancy outcome', () async {
      final prior =
          _scopedOptimisticDashboard ?? await _readOptimisticDashboard();
      await _dashboardCacheWriter(
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
          datingBasis: prior?.datingBasis,
          canonicalLmp: prior?.canonicalLmp,
          completedGestationalWeek: prior?.completedGestationalWeek,
          completedGestationalDays: prior?.completedGestationalDays,
          sourceWeekNumber: prior?.sourceWeekNumber,
          plan: prior?.plan,
          datingQuarantineReason: prior?.datingQuarantineReason,
          pregnancyOutcome: result.outcomeType,
          pregnancyOutcomeDate: result.outcomeDate,
        ),
        pendingSync: true,
        expectedUserId: requestUserId,
      );
    });
    _ensureRequestUserCurrent(requestUserId, 'pregnancy outcome delivery');
    _notifyDashboardChanged();
    return result;
  }

  /// [BƯỚC 1: GỬI REQUEST CẬP NHẬT NGÀY THAI KỲ LÊN BACKEND]
  /// Gọi `PUT /api/v1/journeys/{journeyId}` để điều chỉnh ngày kinh cuối (LMP) hoặc ngày dự sinh (EDD).
  /// Nhận kết quả tuần thai mới được tính toán lại từ server và đồng bộ vào bộ nhớ tạm.
  Future<void> updateJourney(
    String journeyId,
    UpdateJourneyRequest request,
  ) async {
    final requestUserId = _currentUserIdProvider();
    // Gửi PUT request kèm Header Contract Version 2
    final data = await _apiPut('/api/v1/journeys/$journeyId', request.toJson());
    _ensureRequestUserCurrent(requestUserId, 'journey update');
    await _reconcileAfterCommit('journey update', () async {
      final body = data?['data'] as Map<String, dynamic>?;
      final journeyType =
          body?['journeyType'] as String? ?? request.journeyType?.toApiValue();
      final estimatedDueDate =
          _parseDate(body?['estimatedDueDate'] as String?) ??
          _parseDate(request.estimatedDueDate);
      final responseContainsLmp =
          body?.containsKey('lastMenstrualDate') == true;
      final responseLmp = _parseDate(body?['lastMenstrualDate'] as String?);
      final requestLmp = _parseDate(request.lastMenstrualDate);
      if (estimatedDueDate != null ||
          responseContainsLmp ||
          requestLmp != null) {
        final fallback = _scopedOptimisticDashboard;
        final lastMenstrualDate =
            JourneyDashboardReconciler.updatedLastMenstrualDate(
              responseContainsField: responseContainsLmp,
              responseValue: responseLmp,
              requestValue: requestLmp,
              requestEstimatedDueDate: _parseDate(request.estimatedDueDate),
              fallbackValue: fallback?.lastMenstrualDate,
            );
        // Lưu trữ dữ liệu tuổi thai cập nhật mới vào Secure Storage
        await _dashboardCacheWriter(
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
                _parseDate(body?['startDate'] as String?) ??
                fallback?.startDate,
            version: (body?['version'] as num?)?.toInt(),
            dateSource: body?['dateSource'] as String? ?? request.dateSource,
            dateConfidence:
                body?['dateConfidence'] as String? ?? request.dateConfidence,
            datingBasis:
                (body?['datingBasis'] ?? body?['gestationalDatingBasis'])
                    ?.toString() ??
                request.datingBasis,
            canonicalLmp: _parseDate(
              (body?['canonicalLmp'] ?? body?['canonicalLMP']) as String?,
            ),
            completedGestationalWeek:
                (body?['completedGestationalWeek'] as num?)?.toInt(),
            completedGestationalDays:
                (body?['completedGestationalDays'] as num?)?.toInt(),
            sourceWeekNumber: (body?['sourceWeekNumber'] as num?)?.toInt(),
            plan: (body?['plan'] as num?)?.toInt(),
            datingQuarantineReason:
                (body?['datingQuarantineReason'] ??
                        body?['gestationalDatingQuarantineReasonCode'] ??
                        body?['gestationalDatingQuarantineReason'])
                    ?.toString(),
          ),
          pendingSync: true,
          expectedUserId: requestUserId,
        );
      }
    });
    _ensureRequestUserCurrent(requestUserId, 'journey update delivery');
    _notifyDashboardChanged();
  }

  bool _isRequestUserCurrent(String? expectedUserId) =>
      expectedUserId != null && expectedUserId == _currentUserIdProvider();

  void _ensureRequestUserCurrent(String? expectedUserId, String operation) {
    if (!_isRequestUserCurrent(expectedUserId)) {
      throw StateError('Authenticated account changed during $operation');
    }
  }

  Future<void> _reconcileAfterCommit(
    String operation,
    Future<void> Function() reconcile,
  ) async {
    try {
      await reconcile();
    } catch (error) {
      debugPrint(
        '[JourneyService] $operation committed; local reconciliation will '
        'retry from the server (${error.runtimeType}).',
      );
    }
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
      pregnancyWeek: dashboard.hasActiveJourney
          ? dashboard.pregnancyWeek
          : dashboard.pregnancyWeek ?? fallback.pregnancyWeek,
      // Dating metadata is server-owned. For an active journey, preserve
      // explicit nulls from the response so unresolved/quarantined dating
      // cannot display a stale cached Plan. A pending no-journey response may
      // still use the optimistic create result until the next authoritative
      // dashboard read succeeds.
      completedGestationalWeek: dashboard.hasActiveJourney
          ? dashboard.completedGestationalWeek
          : dashboard.completedGestationalWeek ?? fallback.completedGestationalWeek,
      completedGestationalDays: dashboard.hasActiveJourney
          ? dashboard.completedGestationalDays
          : dashboard.completedGestationalDays ?? fallback.completedGestationalDays,
      sourceWeekNumber: dashboard.hasActiveJourney
          ? dashboard.sourceWeekNumber
          : dashboard.sourceWeekNumber ?? fallback.sourceWeekNumber,
      plan: dashboard.hasActiveJourney
          ? dashboard.plan
          : dashboard.plan ?? fallback.plan,
      trimester: dashboard.hasActiveJourney
          ? dashboard.trimester
          : dashboard.trimester ?? fallback.trimester,
      daysUntilDue: dashboard.hasActiveJourney
          ? dashboard.daysUntilDue
          : dashboard.daysUntilDue ?? fallback.daysUntilDue,
      estimatedDueDate: dashboard.hasActiveJourney
          ? dashboard.estimatedDueDate
          : preferFallback
              ? fallback.estimatedDueDate ?? dashboard.estimatedDueDate
              : dashboard.estimatedDueDate ?? fallback.estimatedDueDate,
      lastMenstrualDate: dashboard.hasActiveJourney
          ? dashboard.lastMenstrualDate
          : preferFallback
              ? fallback.lastMenstrualDate
              : dashboard.lastMenstrualDate ?? fallback.lastMenstrualDate,
      startDate: dashboard.startDate ?? fallback.startDate,
      version: dashboard.version ?? fallback.version,
      dateSource: dashboard.dateSource ?? fallback.dateSource,
      dateConfidence: dashboard.dateConfidence ?? fallback.dateConfidence,
      datingBasis: dashboard.hasActiveJourney
          ? dashboard.datingBasis
          : dashboard.datingBasis ?? fallback.datingBasis,
      datingQuarantineReason: dashboard.hasActiveJourney
          ? dashboard.datingQuarantineReason
          : dashboard.datingQuarantineReason ?? fallback.datingQuarantineReason,
      canonicalLmp: dashboard.hasActiveJourney
          ? dashboard.canonicalLmp
          : dashboard.canonicalLmp ?? fallback.canonicalLmp,
      gestationalDatingRevision: dashboard.hasActiveJourney
          ? dashboard.gestationalDatingRevision
          : dashboard.gestationalDatingRevision ?? fallback.gestationalDatingRevision,
      gestationalDatingEffectiveAt: dashboard.hasActiveJourney
          ? dashboard.gestationalDatingEffectiveAt
          : dashboard.gestationalDatingEffectiveAt ??
              fallback.gestationalDatingEffectiveAt,
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
        'completedGestationalWeek': dashboard.completedGestationalWeek,
        'completedGestationalDays': dashboard.completedGestationalDays,
        'sourceWeekNumber': dashboard.sourceWeekNumber,
        'plan': dashboard.plan,
        'trimester': dashboard.trimester,
        'daysUntilDue': dashboard.daysUntilDue,
        'estimatedDueDate': _formatDate(dashboard.estimatedDueDate),
        'lastMenstrualDate': _formatDate(dashboard.lastMenstrualDate),
        'startDate': _formatDate(dashboard.startDate),
        'version': dashboard.version,
        'dateSource': dashboard.dateSource,
        'dateConfidence': dashboard.dateConfidence,
        'datingBasis': dashboard.datingBasis,
        'datingQuarantineReason': dashboard.datingQuarantineReason,
        'canonicalLmp': _formatDate(dashboard.canonicalLmp),
        'gestationalDatingRevision': dashboard.gestationalDatingRevision,
        'gestationalDatingEffectiveAt':
            dashboard.gestationalDatingEffectiveAt?.toUtc().toIso8601String(),
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
        completedGestationalWeek:
            (data['completedGestationalWeek'] as num?)?.toInt(),
        completedGestationalDays:
            (data['completedGestationalDays'] as num?)?.toInt(),
        sourceWeekNumber: (data['sourceWeekNumber'] as num?)?.toInt(),
        plan: (data['plan'] as num?)?.toInt(),
        trimester: (data['trimester'] as num?)?.toInt(),
        daysUntilDue: (data['daysUntilDue'] as num?)?.toInt(),
        estimatedDueDate: _parseDate(data['estimatedDueDate'] as String?),
        lastMenstrualDate: _parseDate(data['lastMenstrualDate'] as String?),
        startDate: _parseDate(data['startDate'] as String?),
        version: (data['version'] as num?)?.toInt(),
        dateSource: data['dateSource'] as String?,
        dateConfidence: data['dateConfidence'] as String?,
        datingBasis: data['datingBasis'] as String?,
        datingQuarantineReason: data['datingQuarantineReason'] as String?,
        canonicalLmp: _parseDate(data['canonicalLmp'] as String?),
        gestationalDatingRevision:
            (data['gestationalDatingRevision'] as num?)?.toInt(),
        gestationalDatingEffectiveAt:
            _parseDate(data['gestationalDatingEffectiveAt'] as String?),
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
