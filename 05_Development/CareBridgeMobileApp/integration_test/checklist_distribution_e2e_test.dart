import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/core/network/api_client.dart';
import 'package:untitled/features/home/screens/family_member_home_screen.dart';
import 'package:untitled/features/home/screens/mother_home_screen.dart';
import 'package:untitled/features/journey/models/journey_model.dart';
import 'package:untitled/features/reminder/models/today_task_model.dart';
import 'package:untitled/features/reminder/services/today_task_service.dart';
import 'package:uuid/uuid.dart';

/// Destructive live-API evidence harness for CHK-042/043.
///
/// All values, including session credentials, safety acknowledgements and the
/// expected API URL, must come from a gitignored JSON file:
///
/// `flutter test integration_test/checklist_distribution_e2e_test.dart`
/// ` -d <dedicated-device> --dart-define-from-file=<gitignored-json>`
///
/// The file must define CHK_API_E2E, CHK_E2E_ENVIRONMENT,
/// CHK_E2E_EXPECTED_API_BASE_URL, CHK_E2E_DEVICE_ACK,
/// CHK_E2E_SERVER_ENVIRONMENT_ID, CHK_E2E_CREDENTIAL_ARTIFACT_ACK and
/// access/refresh/user-id triples for CHK_CONTENT_ADMIN, CHK_ADMIN,
/// CHK_MOTHER, CHK_FAMILY and CHK_ISOLATION_FAMILY. HTTP additionally
/// requires the loopback-only CHK_E2E_ALLOW_LOOPBACK_HTTP acknowledgement.
///
/// Dart defines are compile-time constants: all supplied credentials are
/// embedded in the generated test artifact. A gitignored input file is not a
/// secret-safe artifact. Use short-lived disposable credentials, keep the
/// artifact on the dedicated device/runner only, and destroy both after use.
/// Never put the JSON file, compiled artifact or its values in Git/logs.
/// Release CI must additionally assert one executed test and zero skipped tests;
/// an ordinary suite run intentionally skips this test when CHK_API_E2E=false.
const _apiBacked =
    String.fromEnvironment('CHK_API_E2E', defaultValue: 'false') == 'true';
const _environmentMarker = String.fromEnvironment('CHK_E2E_ENVIRONMENT');
const _expectedApiBaseUrl = String.fromEnvironment(
  'CHK_E2E_EXPECTED_API_BASE_URL',
);
const _expectedServerEnvironmentId = String.fromEnvironment(
  'CHK_E2E_SERVER_ENVIRONMENT_ID',
);
const _deviceAcknowledgement = String.fromEnvironment('CHK_E2E_DEVICE_ACK');
const _credentialArtifactAcknowledgement = String.fromEnvironment(
  'CHK_E2E_CREDENTIAL_ARTIFACT_ACK',
);
const _loopbackHttpAcknowledgement = String.fromEnvironment(
  'CHK_E2E_ALLOW_LOOPBACK_HTTP',
);

const _requestTimeout = Duration(seconds: 15);
const _overallTimeout = Duration(minutes: 6);
const _pollInterval = Duration(milliseconds: 500);
const _maxPollAttempts = 60;
const _maxAuditPages = 5;

const _contentAdminSession = _LiveSession(
  accessToken: String.fromEnvironment('CHK_CONTENT_ADMIN_ACCESS_TOKEN'),
  refreshToken: String.fromEnvironment('CHK_CONTENT_ADMIN_REFRESH_TOKEN'),
  userId: String.fromEnvironment('CHK_CONTENT_ADMIN_USER_ID'),
  role: 'CONTENT_ADMIN',
  label: 'CHK_CONTENT_ADMIN',
);

const _adminSession = _LiveSession(
  accessToken: String.fromEnvironment('CHK_ADMIN_ACCESS_TOKEN'),
  refreshToken: String.fromEnvironment('CHK_ADMIN_REFRESH_TOKEN'),
  userId: String.fromEnvironment('CHK_ADMIN_USER_ID'),
  role: 'SYSTEM_ADMIN',
  label: 'CHK_ADMIN',
);

const _motherSession = _LiveSession(
  accessToken: String.fromEnvironment('CHK_MOTHER_ACCESS_TOKEN'),
  refreshToken: String.fromEnvironment('CHK_MOTHER_REFRESH_TOKEN'),
  userId: String.fromEnvironment('CHK_MOTHER_USER_ID'),
  role: 'MOTHER',
  label: 'CHK_MOTHER',
);

const _familySession = _LiveSession(
  accessToken: String.fromEnvironment('CHK_FAMILY_ACCESS_TOKEN'),
  refreshToken: String.fromEnvironment('CHK_FAMILY_REFRESH_TOKEN'),
  userId: String.fromEnvironment('CHK_FAMILY_USER_ID'),
  role: 'FAMILY',
  label: 'CHK_FAMILY',
);

const _isolationFamilySession = _LiveSession(
  accessToken: String.fromEnvironment('CHK_ISOLATION_FAMILY_ACCESS_TOKEN'),
  refreshToken: String.fromEnvironment('CHK_ISOLATION_FAMILY_REFRESH_TOKEN'),
  userId: String.fromEnvironment('CHK_ISOLATION_FAMILY_USER_ID'),
  role: 'FAMILY',
  label: 'CHK_ISOLATION_FAMILY',
);

class _LiveSession {
  const _LiveSession({
    required this.accessToken,
    required this.refreshToken,
    required this.userId,
    required this.role,
    required this.label,
  });

  final String accessToken;
  final String refreshToken;
  final String userId;
  final String role;
  final String label;

  void validate() {
    expect(
      accessToken,
      isNotEmpty,
      reason: '${label}_ACCESS_TOKEN dart-define is required.',
    );
    expect(
      refreshToken,
      isNotEmpty,
      reason: '${label}_REFRESH_TOKEN dart-define is required.',
    );
    expect(
      userId,
      isNotEmpty,
      reason: '${label}_USER_ID dart-define is required.',
    );
  }

  Future<void> activate() => AuthState.instance.setTokens(
    accessToken: accessToken,
    refreshToken: refreshToken,
    userId: userId,
    role: role,
  );
}

class _WallClockBudget {
  _WallClockBudget() : _stopwatch = Stopwatch()..start();

  final Stopwatch _stopwatch;

  Duration get remaining => _overallTimeout - _stopwatch.elapsed;

  void ensureRemaining(String operation) {
    if (remaining <= Duration.zero) {
      throw TimeoutException(
        '$operation exceeded the $_overallTimeout overall CHK E2E deadline.',
        _overallTimeout,
      );
    }
  }

  Future<T> request<T>(Future<T> Function() request, String operation) async {
    ensureRemaining(operation);
    final timeout = remaining < _requestTimeout ? remaining : _requestTimeout;
    final result = await request().timeout(
      timeout,
      onTimeout: () => throw TimeoutException(
        '$operation exceeded its per-request deadline.',
        timeout,
      ),
    );
    ensureRemaining('$operation completion');
    return result;
  }
}

class _TemplateFixture {
  const _TemplateFixture({
    required this.entityId,
    required this.lineageId,
    required this.versionId,
    required this.templateName,
    required this.taskTitle,
  });

  final String entityId;
  final String lineageId;
  final String versionId;
  final String templateName;
  final String taskTitle;
}

class _FamilyPermissionFixture {
  const _FamilyPermissionFixture({
    required this.groupId,
    required this.memberId,
    required this.previousChecklistView,
    required this.previousChecklistComplete,
  });

  final String groupId;
  final String memberId;
  final bool previousChecklistView;
  final bool previousChecklistComplete;
}

Map<String, dynamic> _map(dynamic value, String label) {
  if (value is! Map) throw FormatException('$label must be a JSON object.');
  return Map<String, dynamic>.from(value);
}

Map<String, dynamic> _data(dynamic response, String label) {
  final envelope = _map(response, label);
  return _map(envelope['data'], '$label.data');
}

String _requiredString(Map<String, dynamic> data, String key, String label) {
  final value = data[key]?.toString();
  if (value == null || value.isEmpty) {
    throw FormatException('$label.$key is required.');
  }
  return value;
}

String _normalizedBaseUrl(String raw, String label) {
  final value = raw.trim();
  if (value.isEmpty) throw FormatException('$label is required.');
  final uri = Uri.tryParse(value);
  if (uri == null ||
      !uri.hasAuthority ||
      (uri.scheme != 'http' && uri.scheme != 'https') ||
      uri.host.isEmpty ||
      uri.userInfo.isNotEmpty ||
      uri.hasQuery ||
      uri.hasFragment ||
      (uri.path.isNotEmpty && uri.path != '/')) {
    throw FormatException('$label must be an origin-only HTTP(S) URL.');
  }
  return uri.replace(path: '').toString().replaceFirst(RegExp(r'/$'), '');
}

bool _isLoopbackTestHost(String host) {
  final normalized = host.toLowerCase();
  return normalized == 'localhost' ||
      normalized == '127.0.0.1' ||
      normalized == '::1' ||
      normalized == '10.0.2.2';
}

void _validateFailClosedPreflight() {
  expect(
    _environmentMarker,
    'DISPOSABLE_NON_PRODUCTION',
    reason:
        'CHK_E2E_ENVIRONMENT must explicitly identify a disposable, non-production environment.',
  );
  expect(
    _deviceAcknowledgement,
    'DEDICATED_DEVICE_CONFIRMED',
    reason: 'CHK_E2E_DEVICE_ACK must confirm a dedicated test device/emulator.',
  );
  expect(
    _credentialArtifactAcknowledgement,
    'COMPILED_CREDENTIAL_ARTIFACT_ACCEPTED',
    reason:
        'CHK_E2E_CREDENTIAL_ARTIFACT_ACK must acknowledge that credentials '
        'are embedded in a disposable compiled test artifact.',
  );
  expect(kIsWeb, isFalse, reason: 'CHK live E2E requires a native device.');
  expect(
    defaultTargetPlatform == TargetPlatform.android ||
        defaultTargetPlatform == TargetPlatform.iOS,
    isTrue,
    reason: 'CHK live E2E requires a dedicated Android or iOS device.',
  );
  final compiledOrigin = _normalizedBaseUrl(apiBaseUrl, 'API_BASE_URL');
  final acknowledgedOrigin = _normalizedBaseUrl(
    _expectedApiBaseUrl,
    'CHK_E2E_EXPECTED_API_BASE_URL',
  );
  expect(
    compiledOrigin,
    acknowledgedOrigin,
    reason:
        'The compiled API_BASE_URL must exactly match the acknowledged disposable API origin.',
  );
  expect(
    _expectedServerEnvironmentId.trim(),
    isNotEmpty,
    reason:
        'CHK_E2E_SERVER_ENVIRONMENT_ID must match the disposable server attestation.',
  );
  final acknowledgedUri = Uri.parse(acknowledgedOrigin);
  if (acknowledgedUri.scheme != 'https') {
    expect(
      _isLoopbackTestHost(acknowledgedUri.host),
      isTrue,
      reason:
          'Credentialed CHK E2E requires HTTPS; HTTP is allowed only for a '
          'loopback test host (including the Android emulator host alias).',
    );
    expect(
      _loopbackHttpAcknowledgement,
      'LOOPBACK_ONLY_CONFIRMED',
      reason:
          'CHK_E2E_ALLOW_LOOPBACK_HTTP must explicitly acknowledge the '
          'loopback-only HTTP exception.',
    );
  }
}

Future<dynamic> _get(
  _WallClockBudget budget,
  String path, {
  Map<String, dynamic>? queryParams,
  Map<String, String>? extraHeaders,
}) => budget.request(
  () => apiGet(path, queryParams: queryParams, extraHeaders: extraHeaders),
  'GET $path',
);

Future<dynamic> _post(
  _WallClockBudget budget,
  String path,
  Map<String, dynamic> body,
) => budget.request(() => apiPost(path, body), 'POST $path');

Future<dynamic> _patch(
  _WallClockBudget budget,
  String path,
  Map<String, dynamic> body,
) => budget.request(() => apiPatch(path, body), 'PATCH $path');

Future<void> _verifyServerEnvironmentAttestation(
  _WallClockBudget budget,
) async {
  final attestation = _map(
    await _get(budget, '/api/v1/operations/checklist-e2e/attestation'),
    'checklist E2E server attestation',
  );
  expect(
    attestation['disposable'],
    true,
    reason: 'The live-E2E server must attest that its database is disposable.',
  );
  expect(
    attestation['environmentId'],
    _expectedServerEnvironmentId,
    reason:
        'The live-E2E server environment ID must match the independently acknowledged fixture.',
  );
}

TodayTaskService _todayService(_WallClockBudget budget) => TodayTaskService(
  getRequest: (path, {queryParams}) => _get(
    budget,
    path,
    queryParams: queryParams,
    extraHeaders: const {'X-User-Timezone': 'Asia/Ho_Chi_Minh'},
  ),
  postRequest: (path, body) => _post(budget, path, body),
);

Future<_TemplateFixture> _createTemplate(_WallClockBudget budget) async {
  final runId = const Uuid().v4();
  final templateName = '[CHK-E2E][$runId] Baby-care distribution';
  final taskTitle = '[CHK-E2E][$runId] Live checklist task';
  final response = await _post(budget, '/api/v1/admin/checklist-templates', {
    'name': templateName,
    'description': 'Disposable CHK-042/043 live-API evidence fixture.',
    'recipientRoles': ['MOTHER', 'FAMILY'],
    'stage': 'BABY_CARE',
    'substage': {
      'code': 'BABY_CARE_MONTH_0_3',
      'anchor': 'BIRTH_DATE',
      'startInclusive': 0,
      'endInclusive': 3,
      'unit': 'MONTH',
    },
    'items': [
      {
        'itemText': taskTitle,
        'order': 1,
        'isRequired': true,
        'targetSubject': 'BABY',
      },
    ],
  });
  final data = _data(response, 'create checklist template response');
  final items = data['items'] as List? ?? const [];
  expect(data['name'], templateName);
  expect(data['status'], 'DRAFT');
  expect(
    items.whereType<Map>().any(
      (item) =>
          item['itemText'] == taskTitle && item['targetSubject'] == 'BABY',
    ),
    isTrue,
    reason: 'The created version must retain the captured BABY task title.',
  );
  return _TemplateFixture(
    entityId: _requiredString(data, 'id', 'created template'),
    lineageId: _requiredString(data, 'lineageId', 'created template'),
    versionId: _requiredString(data, 'versionId', 'created template'),
    templateName: templateName,
    taskTitle: taskTitle,
  );
}

Future<_FamilyPermissionFixture> _findFamilyChecklistPermissionFixture(
  _WallClockBudget budget,
) async {
  final groupsEnvelope = _map(
    await _get(budget, '/api/v1/care-groups'),
    'care groups response',
  );
  final groups = groupsEnvelope['data'] as List? ?? const [];
  for (final rawGroup in groups.whereType<Map>()) {
    final group = Map<String, dynamic>.from(rawGroup);
    final groupId = group['groupId']?.toString();
    if (groupId == null || groupId.isEmpty) continue;
    final members =
        _data(
              await _get(budget, '/api/v1/care-groups/$groupId/members'),
              'care group members response',
            )['members']
            as List? ??
        const [];
    for (final rawMember in members.whereType<Map>()) {
      final member = Map<String, dynamic>.from(rawMember);
      if (member['userId']?.toString() != _familySession.userId ||
          member['inviteStatus'] != 'ACCEPTED') {
        continue;
      }
      final memberId = member['memberId']?.toString();
      if (memberId == null || memberId.isEmpty) continue;
      final permission = _data(
        await _get(
          budget,
          '/api/v1/care-groups/$groupId/members/$memberId/permissions',
        ),
        'current family checklist permission response',
      );
      return _FamilyPermissionFixture(
        groupId: groupId,
        memberId: memberId,
        previousChecklistView: permission['checklistView'] == true,
        previousChecklistComplete: permission['checklistComplete'] == true,
      );
    }
  }
  fail(
    'No accepted Family membership was found for the CHK fixture. '
    'Use the seeded mother4/family3 disposable pair or provision an equivalent pair.',
  );
}

Future<void> _grantFamilyChecklistPermissions(
  _WallClockBudget budget,
  _FamilyPermissionFixture fixture,
) async {
  final permission = _data(
    await _patch(
      budget,
      '/api/v1/care-groups/${fixture.groupId}/members/${fixture.memberId}/permissions',
      const {'checklistView': true, 'checklistComplete': true},
    ),
    'family checklist permission response',
  );
  expect(permission['checklistView'], true);
  expect(permission['checklistComplete'], true);
}

Future<void> _restoreFamilyChecklistPermissionsBestEffort(
  _FamilyPermissionFixture fixture,
) async {
  try {
    await _motherSession.activate();
    await apiPatch(
      '/api/v1/care-groups/${fixture.groupId}/members/${fixture.memberId}/permissions',
      {
        'checklistView': fixture.previousChecklistView,
        'checklistComplete': fixture.previousChecklistComplete,
      },
    ).timeout(_requestTimeout);
  } catch (error) {
    debugPrint(
      '[CHK-E2E] Best-effort permission restore failed: ${error.runtimeType}',
    );
  }
}

Future<void> _archiveTemplateBestEffort(_TemplateFixture fixture) async {
  try {
    await _contentAdminSession.activate();
    await apiPost(
      '/api/v1/admin/checklist-templates/${fixture.entityId}/archive',
      {'reason': 'Disposable CHK-042/043 E2E fixture cleanup.'},
    ).timeout(_requestTimeout);
  } catch (error) {
    debugPrint(
      '[CHK-E2E] Best-effort template archive failed: ${error.runtimeType}',
    );
  }
}

Future<void> _approveAndReconcile(
  WidgetTester tester,
  _WallClockBudget budget,
  _TemplateFixture fixture,
) async {
  final approval = _data(
    await _post(
      budget,
      '/api/v1/admin/checklist-templates/${fixture.lineageId}/versions/${fixture.versionId}/approve',
      const {},
    ),
    'approve checklist template response',
  );
  expect(approval['id'], fixture.entityId);
  expect(approval['previousStatus'], 'DRAFT');
  expect(approval['newStatus'], 'APPROVED');

  final reconciliation = _map(
    await _post(
      budget,
      '/api/v1/operations/checklist-reconciliation/runs',
      const {},
    ),
    'checklist reconciliation response',
  );
  expect(reconciliation['correlationId'], isNotNull);
  expect(reconciliation['failedCount'], 0);
  await _waitForApprovedVersion(tester, budget, fixture);
}

Future<void> _waitForApprovedVersion(
  WidgetTester tester,
  _WallClockBudget budget,
  _TemplateFixture fixture,
) async {
  Object? lastError;
  for (var attempt = 0; attempt < _maxPollAttempts; attempt++) {
    budget.ensureRemaining('poll approved checklist version');
    try {
      final data = _data(
        await _get(
          budget,
          '/api/v1/admin/checklist-templates/${fixture.entityId}',
        ),
        'checklist template detail response',
      );
      if (data['status'] == 'APPROVED' &&
          data['distributionEnabled'] == true &&
          data['lineageId'] == fixture.lineageId &&
          data['versionId'] == fixture.versionId &&
          data['name'] == fixture.templateName) {
        return;
      }
    } catch (error) {
      lastError = error;
    }
    await tester.pump(_pollInterval);
  }
  fail(
    'Timed out waiting for approved captured version ${fixture.versionId}.'
    '${lastError == null ? '' : ' Last error: $lastError'}',
  );
}

JourneyDashboard _dashboardFixture() => const JourneyDashboard(
  journeyId: 'chk-e2e-dashboard-fixture',
  journeyType: 'PREGNANCY',
  status: 'ACTIVE_PREGNANCY',
  pregnancyWeek: 24,
);

Finder _taskCard(String title) => find.byWidgetPredicate((widget) {
  if (widget is! Semantics) return false;
  final label = widget.properties.label;
  return label != null && label.startsWith('$title, ');
});

Finder _completeAction(String title) =>
    find.descendant(of: _taskCard(title), matching: find.byType(FilledButton));

Future<void> _waitFor(
  WidgetTester tester,
  _WallClockBudget budget,
  bool Function() condition,
  String failureMessage,
) async {
  for (var attempt = 0; attempt < _maxPollAttempts; attempt++) {
    budget.ensureRemaining(failureMessage);
    await tester.pump(_pollInterval);
    if (condition()) return;
  }
  fail(
    '$failureMessage after '
    '${_maxPollAttempts * _pollInterval.inMilliseconds} ms',
  );
}

Future<TodayTask> _waitForApiTask(
  WidgetTester tester,
  _WallClockBudget budget,
  TodayTaskService service, {
  required _TemplateFixture fixture,
  TodayTaskStatus? status,
}) async {
  Object? lastError;
  for (var attempt = 0; attempt < _maxPollAttempts; attempt++) {
    budget.ensureRemaining('poll Today task ${fixture.versionId}');
    TodayTasksSnapshot? snapshot;
    try {
      snapshot = await service.loadToday();
    } catch (error) {
      lastError = error;
    }
    if (snapshot != null) {
      final matches = snapshot.sections.all.where(
        (task) =>
            task.title == fixture.taskTitle &&
            task.templateVersionId == fixture.versionId &&
            (status == null || task.taskStatus == status),
      );
      if (matches.length == 1) return matches.single;
      if (matches.length > 1) {
        fail(
          'Expected one live task for version ${fixture.versionId}, '
          'found ${matches.length}.',
        );
      }
    }
    await tester.pump(_pollInterval);
  }
  fail(
    'Timed out waiting for live task ${fixture.taskTitle} from captured '
    'version ${fixture.versionId}'
    '${status == null ? '' : ' with status ${status.name}'}.'
    '${lastError == null ? '' : ' Last error: $lastError'}',
  );
}

Future<TodayTasksSnapshot> _waitForSnapshot(
  WidgetTester tester,
  _WallClockBudget budget,
  TodayTaskService service,
) async {
  Object? lastError;
  for (var attempt = 0; attempt < _maxPollAttempts; attempt++) {
    budget.ensureRemaining('poll live Today snapshot');
    try {
      return await service.loadToday();
    } catch (error) {
      lastError = error;
    }
    await tester.pump(_pollInterval);
  }
  fail(
    'Timed out waiting for the live Today snapshot.'
    '${lastError == null ? '' : ' Last error: $lastError'}',
  );
}

void _assertSnapshotExcludes(
  TodayTasksSnapshot snapshot,
  Iterable<String> taskIds,
  String reason,
) {
  final forbidden = taskIds.toSet();
  expect(
    snapshot.sections.all.where((task) => forbidden.contains(task.id)),
    isEmpty,
    reason: reason,
  );
}

void _assertSnapshotExcludesFixture(
  TodayTasksSnapshot snapshot,
  _TemplateFixture fixture,
  Iterable<String> knownTaskIds,
  String reason,
) {
  _assertSnapshotExcludes(snapshot, knownTaskIds, reason);
  expect(
    snapshot.sections.all.where(
      (task) =>
          task.templateVersionId == fixture.versionId ||
          task.title == fixture.taskTitle,
    ),
    isEmpty,
    reason:
        '$reason No task from the captured template version/run marker may exist.',
  );
}

Future<void> _expectDirectActionNotFound(
  _WallClockBudget budget,
  String taskId,
  String actorLabel,
) async {
  ApiException? rejection;
  try {
    await _post(budget, '/api/v1/tasks/CHECKLIST/$taskId/actions', {
      'action': 'COMPLETE',
      'clientRequestId': const Uuid().v4(),
      'reason': null,
    });
  } on ApiException catch (error) {
    rejection = error;
  }
  expect(
    rejection?.statusCode,
    404,
    reason:
        '$actorLabel direct action against task $taskId must hide existence.',
  );
}

void _assertSharedDistributionContext(
  TodayTask motherTask,
  TodayTask familyTask,
  _TemplateFixture fixture,
  String expectedCareGroupId,
) {
  expect(motherTask.title, fixture.taskTitle);
  expect(familyTask.title, fixture.taskTitle);
  expect(motherTask.templateVersionId, fixture.versionId);
  expect(familyTask.templateVersionId, fixture.versionId);
  expect(motherTask.taskStatus, TodayTaskStatus.pending);
  expect(familyTask.taskStatus, TodayTaskStatus.pending);
  expect(motherTask.id, isNot(familyTask.id));
  expect(motherTask.instanceId, isNotNull);
  expect(motherTask.instanceId, isNotEmpty);
  expect(familyTask.instanceId, isNotNull);
  expect(familyTask.instanceId, isNotEmpty);
  expect(motherTask.instanceId, isNot(familyTask.instanceId));
  expect(motherTask.careGroupId, isNotNull);
  expect(motherTask.careGroupId, isNotEmpty);
  expect(motherTask.careGroupId, expectedCareGroupId);
  expect(motherTask.careGroupId, familyTask.careGroupId);
  expect(motherTask.careContextType, 'BABY');
  expect(motherTask.careContextType, familyTask.careContextType);
  expect(motherTask.careContextId, isNotNull);
  expect(motherTask.careContextId, isNotEmpty);
  expect(motherTask.careContextId, familyTask.careContextId);
}

Future<void> _waitForTaskCard(
  WidgetTester tester,
  _WallClockBudget budget,
  String title,
) => _waitFor(
  tester,
  budget,
  () => _taskCard(title).evaluate().length == 1,
  'Timed out waiting for "$title" on the Home screen',
);

Future<void> _waitForTerminalCard(
  WidgetTester tester,
  _WallClockBudget budget,
  String title,
) => _waitFor(
  tester,
  budget,
  () =>
      _taskCard(title).evaluate().length == 1 &&
      _completeAction(title).evaluate().isEmpty,
  'Timed out waiting for terminal task "$title" on the Home screen',
);

Future<void> _triggerHomeRefresh(
  WidgetTester tester,
  _WallClockBudget budget,
) async {
  final refreshFinder = find.byType(RefreshIndicator);
  expect(refreshFinder, findsWidgets);
  final state = tester.state<RefreshIndicatorState>(refreshFinder.first);
  var completed = false;
  Object? refreshError;
  state.show().then<void>(
    (_) => completed = true,
    onError: (Object error, StackTrace _) {
      refreshError = error;
      completed = true;
    },
  );
  await _waitFor(
    tester,
    budget,
    () => completed,
    'Timed out waiting for the Home pull-to-refresh callback',
  );
  if (refreshError != null) fail('Home pull-to-refresh failed: $refreshError');
}

Future<void> _assertTaskContextRendered(
  WidgetTester tester,
  TodayTask task,
) async {
  final cardFinder = _taskCard(task.title);
  final semantics = tester.widget<Semantics>(cardFinder);
  final label = semantics.properties.label ?? '';
  expect(label, contains('System template'));
  expect(label, contains('Baby'));
  expect(task.careGroupLabel, isNotNull);
  expect(task.careGroupLabel, isNotEmpty);
  expect(task.careContextLabel, isNotNull);
  expect(task.careContextLabel, isNotEmpty);
  expect(
    find.descendant(of: cardFinder, matching: find.text(task.careGroupLabel!)),
    findsOneWidget,
  );
  expect(
    find.descendant(
      of: cardFinder,
      matching: find.text(task.careContextLabel!),
    ),
    findsOneWidget,
  );
}

Future<void> _completeFromHome(
  WidgetTester tester,
  _WallClockBudget budget,
  TodayTaskService service,
  _TemplateFixture fixture,
  TodayTask task,
) async {
  expect(task.taskStatus, TodayTaskStatus.pending);
  await _waitForTaskCard(tester, budget, task.title);
  await tester.ensureVisible(_taskCard(task.title));
  await tester.pump(const Duration(milliseconds: 250));
  await _assertTaskContextRendered(tester, task);

  final completeFinder = _completeAction(task.title);
  expect(completeFinder, findsOneWidget);
  await tester.tap(completeFinder);
  await _waitFor(
    tester,
    budget,
    () => find.byKey(const Key('today-action-result')).evaluate().isNotEmpty,
    'Timed out waiting for the Today action result',
  );
  await _waitForTerminalCard(tester, budget, task.title);

  final completed = await _waitForApiTask(
    tester,
    budget,
    service,
    fixture: fixture,
    status: TodayTaskStatus.completed,
  );
  expect(completed.id, task.id);
  expect(completed.instanceId, task.instanceId);
  expect(completed.allowedActions, isEmpty);

  await _triggerHomeRefresh(tester, budget);
  await _waitForTerminalCard(tester, budget, task.title);
  final stable = await _waitForApiTask(
    tester,
    budget,
    service,
    fixture: fixture,
    status: TodayTaskStatus.completed,
  );
  expect(stable.id, task.id);
  expect(stable.instanceId, task.instanceId);
}

Future<void> _unmount(WidgetTester tester) async {
  await tester.pumpWidget(const MaterialApp(home: SizedBox.shrink()));
  await tester.pump(const Duration(milliseconds: 250));
}

Future<void> _waitForAudit(
  WidgetTester tester,
  _WallClockBudget budget, {
  required String actorUserId,
  required String taskId,
  required DateTime fromDate,
}) async {
  Object? lastError;
  for (var attempt = 0; attempt < _maxPollAttempts; attempt++) {
    budget.ensureRemaining('poll CHECKLIST_COMPLETED audit');
    try {
      var exhausted = false;
      for (var page = 0; page < _maxAuditPages && !exhausted; page++) {
        final response = _map(
          await _get(
            budget,
            '/api/v1/admin/audit-logs',
            queryParams: {
              'userId': actorUserId,
              'action': 'CHECKLIST_COMPLETED',
              'fromDate': fromDate.toUtc().toIso8601String(),
              'page': page,
              'size': 100,
            },
          ),
          'audit page response',
        );
        final rows = (response['data'] as List? ?? const [])
            .whereType<Map>()
            .map((row) => Map<String, dynamic>.from(row));
        if (rows.any(
          (row) =>
              row['action'] == 'CHECKLIST_COMPLETED' &&
              row['resourceType'] == 'CHECKLIST_TASK_INSTANCE' &&
              row['resourceId'] == taskId &&
              row['userId'] == actorUserId,
        )) {
          return;
        }
        final totalPages = (response['totalPages'] as num?)?.toInt() ?? 0;
        exhausted = page + 1 >= totalPages;
      }
    } catch (error) {
      lastError = error;
    }
    await tester.pump(_pollInterval);
  }
  fail(
    'Timed out waiting for CHECKLIST_COMPLETED audit for task $taskId.'
    '${lastError == null ? '' : ' Last error: $lastError'}',
  );
}

Future<void> _runLiveScenario(WidgetTester tester) async {
  final budget = _WallClockBudget();
  _validateFailClosedPreflight();
  for (final session in const [
    _contentAdminSession,
    _adminSession,
    _motherSession,
    _familySession,
    _isolationFamilySession,
  ]) {
    session.validate();
  }
  expect(
    {
      _contentAdminSession.userId,
      _adminSession.userId,
      _motherSession.userId,
      _familySession.userId,
      _isolationFamilySession.userId,
    }.length,
    5,
    reason: 'Every CHK live role must use a distinct identity.',
  );
  addTearDown(AuthState.instance.clear);

  final auditFromDate = DateTime.now().toUtc().subtract(
    const Duration(minutes: 1),
  );
  await _adminSession.activate();
  await _verifyServerEnvironmentAttestation(budget);

  await _motherSession.activate();
  final familyPermissionFixture = await _findFamilyChecklistPermissionFixture(
    budget,
  );
  addTearDown(
    () => _restoreFamilyChecklistPermissionsBestEffort(familyPermissionFixture),
  );
  await _grantFamilyChecklistPermissions(budget, familyPermissionFixture);
  final expectedCareGroupId = familyPermissionFixture.groupId;

  await _contentAdminSession.activate();
  final fixture = await _createTemplate(budget);
  addTearDown(() => _archiveTemplateBestEffort(fixture));

  await _adminSession.activate();
  await _approveAndReconcile(tester, budget, fixture);

  final service = _todayService(budget);

  await _motherSession.activate();
  var motherTask = await _waitForApiTask(
    tester,
    budget,
    service,
    fixture: fixture,
    status: TodayTaskStatus.pending,
  );
  expect(motherTask.kind, TodayTaskKind.checklist);
  expect(motherTask.origin, TodayTaskOrigin.systemTemplate);
  expect(motherTask.target, TodayTaskTarget.baby);
  expect(motherTask.allowedActions, contains(TodayTaskAction.complete));

  await _familySession.activate();
  var familyTask = await _waitForApiTask(
    tester,
    budget,
    service,
    fixture: fixture,
    status: TodayTaskStatus.pending,
  );
  expect(familyTask.kind, TodayTaskKind.checklist);
  expect(familyTask.origin, TodayTaskOrigin.systemTemplate);
  expect(familyTask.target, TodayTaskTarget.baby);
  expect(familyTask.allowedActions, contains(TodayTaskAction.complete));
  _assertSharedDistributionContext(
    motherTask,
    familyTask,
    fixture,
    expectedCareGroupId,
  );
  _assertSnapshotExcludes(
    await _waitForSnapshot(tester, budget, service),
    [motherTask.id],
    'Family must not see the Mother-owned task instance.',
  );
  await _expectDirectActionNotFound(budget, motherTask.id, 'Configured Family');

  await _motherSession.activate();
  _assertSnapshotExcludes(
    await _waitForSnapshot(tester, budget, service),
    [familyTask.id],
    'Mother must not see the Family-owned task instance.',
  );
  await _expectDirectActionNotFound(budget, familyTask.id, 'Mother');
  motherTask = await _waitForApiTask(
    tester,
    budget,
    service,
    fixture: fixture,
    status: TodayTaskStatus.pending,
  );

  await _isolationFamilySession.activate();
  _assertSnapshotExcludesFixture(
    await _waitForSnapshot(tester, budget, service),
    fixture,
    [motherTask.id, familyTask.id],
    'An unrelated Family identity must not see either recipient instance.',
  );
  await _expectDirectActionNotFound(budget, motherTask.id, 'Unrelated Family');
  await _expectDirectActionNotFound(budget, familyTask.id, 'Unrelated Family');

  await _motherSession.activate();
  motherTask = await _waitForApiTask(
    tester,
    budget,
    service,
    fixture: fixture,
    status: TodayTaskStatus.pending,
  );
  await tester.pumpWidget(
    MaterialApp(
      home: MotherHomeScreen(
        todayTaskService: service,
        dashboardLoader: () async => _dashboardFixture(),
        reminderLoader: () async => const [],
      ),
    ),
  );
  await _completeFromHome(tester, budget, service, fixture, motherTask);
  await _unmount(tester);

  await _familySession.activate();
  familyTask = await _waitForApiTask(
    tester,
    budget,
    service,
    fixture: fixture,
    status: TodayTaskStatus.pending,
  );
  await tester.pumpWidget(
    MaterialApp(home: FamilyMemberHomeScreen(todayTaskService: service)),
  );
  await _completeFromHome(tester, budget, service, fixture, familyTask);
  await _unmount(tester);

  await _adminSession.activate();
  await _waitForAudit(
    tester,
    budget,
    actorUserId: _motherSession.userId,
    taskId: motherTask.id,
    fromDate: auditFromDate,
  );
  await _waitForAudit(
    tester,
    budget,
    actorUserId: _familySession.userId,
    taskId: familyTask.id,
    fromDate: auditFromDate,
  );
  budget.ensureRemaining('complete CHK-042/043 live scenario');
}

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets(
    'CHK-042/043 author, approve, distribute and complete isolated Mother/Family tasks via live API',
    (tester) => _runLiveScenario(tester).timeout(
      _overallTimeout,
      onTimeout: () => throw TimeoutException(
        'CHK-042/043 exceeded the $_overallTimeout overall deadline.',
        _overallTimeout,
      ),
    ),
    skip: !_apiBacked,
  );
}
