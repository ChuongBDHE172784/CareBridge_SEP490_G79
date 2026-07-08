import '../../../core/network/api_client.dart';
import '../models/care_group_model.dart';

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

  // UC-83: Owner invites a member by email. memberRole omitted -> backend defaults to MEMBER.
  Future<CareGroupMember> inviteMember(
    String groupId,
    String email, {
    String? memberRole,
  }) async {
    final data = await apiPost('/api/v1/care-groups/$groupId/invitations', {
      'email': email,
      'memberRole': ?memberRole,
    });
    return CareGroupMember.fromJson(data['data'] as Map<String, dynamic>);
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

  // TODO: DELETE /api/v1/care-groups/{id}/members/me when endpoint available (UC-72)
  Future<void> leaveGroup(String groupId) async {
    // placeholder
  }
}
