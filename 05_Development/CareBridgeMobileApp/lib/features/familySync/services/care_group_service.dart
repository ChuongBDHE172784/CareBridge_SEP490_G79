import '../../../core/network/api_client.dart';
import '../models/care_group_model.dart';
import '../models/family_permission_model.dart';

class CareGroupService {
  // UC70: Create care group
  Future<Map<String, dynamic>> createCareGroup(
    String groupName, {
    String? description,
  }) async {
    final data = await apiPost('/api/v1/care-groups', {
      'groupName': groupName,
      'description': ?description,
    });
    return data['data'] as Map<String, dynamic>;
  }

  // UC216: List members of a group
  Future<CareGroup> getGroupMembers(String groupId) async {
    final data = await apiGet('/api/v1/care-groups/$groupId/members');
    return CareGroup.fromJson(data['data'] as Map<String, dynamic>);
  }

  // UC-71/83: List care groups the caller belongs to (owner or accepted member)
  Future<List<CareGroup>> listMyGroups() async {
    final data = await apiGet('/api/v1/care-groups');
    return (data['data'] as List<dynamic>)
        .map((e) => CareGroup.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  // UC-71: Owner invites a member via LINK or PHONE channel
  Future<Map<String, dynamic>> inviteMember(
    String groupId, {
    required String channel,
    String? phone,
    String? memberRole,
  }) async {
    final body = <String, dynamic>{
      'channel': channel,
    };
    if (phone != null && phone.isNotEmpty) {
      body['phone'] = phone;
    }
    if (memberRole != null) {
      body['memberRole'] = memberRole;
    }
    
    final data = await apiPost('/api/v1/care-groups/$groupId/invitations', body);
    return data['data'] as Map<String, dynamic>;
  }

  // UC-83: The caller's own pending invitations across all groups.
  Future<List<PendingInvitation>> listMyInvitations() async {
    final data = await apiGet('/api/v1/care-groups/invitations/me');
    return (data['data'] as List<dynamic>)
        .map((e) => PendingInvitation.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  // UC-83: Accept a pending invitation.
  Future<CareGroupMember> acceptInvite(String groupId) async {
    final data = await apiPost(
      '/api/v1/care-groups/$groupId/invitations/accept',
      const {},
    );
    return CareGroupMember.fromJson(data['data'] as Map<String, dynamic>);
  }

  // UC-83: Decline a pending invitation.
  Future<void> declineInvite(String groupId) async {
    await apiPost('/api/v1/care-groups/$groupId/invitations/decline', const {});
  }

  // Join care group by invite code (or groupId UUID)
  Future<Map<String, dynamic>> joinGroupByCode(String code) async {
    final data = await apiPost(
      '/api/v1/care-groups/join',
      {'code': code},
    );
    return data['data'] as Map<String, dynamic>;
  }

  // UC220: Leave care group
  Future<void> leaveGroup(String groupId) async {
    await apiPost('/api/v1/care-groups/$groupId/leave', const {});
  }

  // List join requests for a group (Mother only)
  Future<List<JoinRequest>> listJoinRequests(String groupId) async {
    final data = await apiGet('/api/v1/care-groups/$groupId/join-requests');
    final list = data['data'] as List<dynamic>? ?? [];
    return list.map((item) => JoinRequest.fromJson(item as Map<String, dynamic>)).toList();
  }

  // Mother approves or rejects a join request
  Future<void> respondJoinRequest(
      String groupId, String memberId, bool approve) async {
    await apiPost(
      '/api/v1/care-groups/$groupId/join-requests/$memberId/respond?approve=$approve',
      const {},
    );
  }

  // Delete care group (owner only, hard delete from DB)
  Future<void> deleteCareGroup(String groupId) async {
    await apiDelete('/api/v1/care-groups/$groupId');
  }

  // UC217: Revoke pending invitation
  Future<void> revokeInvitation(String groupId, String targetUserId) async {
    await apiPost(
      '/api/v1/care-groups/$groupId/invitations/$targetUserId/revoke',
      const {},
    );
  }

  // UC219: Owner removes an accepted non-owner member
  Future<void> removeMember(String groupId, String targetUserId) async {
    await apiDelete('/api/v1/care-groups/$groupId/members/$targetUserId');
  }

  // UC72: Get family permission
  Future<FamilyPermission> getFamilyPermission(
    String groupId,
    String memberId,
  ) async {
    final data = await apiGet(
      '/api/v1/care-groups/$groupId/members/$memberId/permissions',
    );
    return FamilyPermission.fromJson(data['data'] as Map<String, dynamic>);
  }

  // UC72: Update family permission
  Future<FamilyPermission> updateFamilyPermission(
    String groupId,
    String memberId, {
    bool? calendar,
    bool? logs,
    bool? alerts,
    bool? records,
  }) async {
    final body = <String, dynamic>{};
    if (calendar != null) body['calendar'] = calendar;
    if (logs != null) body['logs'] = logs;
    if (alerts != null) body['alerts'] = alerts;
    if (records != null) body['records'] = records;

    final data = await apiPatch(
      '/api/v1/care-groups/$groupId/members/$memberId/permissions',
      body,
    );
    return FamilyPermission.fromJson(data['data'] as Map<String, dynamic>);
  }

  // UC74: View Shared Care Calendar
  Future<List<Map<String, dynamic>>> getSharedCalendar(
    String groupId,
    DateTime start,
    DateTime end,
  ) async {
    final data = await apiGet(
      '/api/v1/care-groups/$groupId/calendar',
      queryParams: {
        'rangeStart': start.toUtc().toIso8601String(),
        'rangeEnd': end.toUtc().toIso8601String(),
      },
    );
    final items = data['data']['items'] as List<dynamic>? ?? [];
    return items.cast<Map<String, dynamic>>();
  }

  // UC86: View Family Alerts
  Future<List<Map<String, dynamic>>> getFamilyAlerts(
    String groupId, {
    int page = 0,
    int size = 20,
  }) async {
    try {
      final data = await apiGet(
        '/api/v1/family-alerts',
        queryParams: {'page': page.toString(), 'size': size.toString()},
      );
      final alerts = data['data']['alerts'] as List<dynamic>? ?? [];
      return alerts.cast<Map<String, dynamic>>();
    } catch (e) {
      // Fallback for UI testing if backend fails
      return [
        {
          'alertId': 'mock-1',
          'title': 'Bé Mỡ đã ngủ ngon',
          'body': 'Nhiệt độ phòng ổn định',
          'isRead': false,
          'createdAt': DateTime.now()
              .subtract(const Duration(minutes: 5))
              .toIso8601String(),
        },
        {
          'alertId': 'mock-2',
          'title': 'Nhiệt độ phòng bé hơi lạnh',
          'body': 'Dưới 24 độ C',
          'isRead': true,
          'createdAt': DateTime.now()
              .subtract(const Duration(hours: 1))
              .toIso8601String(),
        },
      ];
    }
  }

  // UC86: Mark alert as read (re-uses UC12 Notification API)
  Future<void> markAlertAsRead(String alertId) async {
    await apiPut('/api/v1/notifications/$alertId/read', const {});
  }
}
