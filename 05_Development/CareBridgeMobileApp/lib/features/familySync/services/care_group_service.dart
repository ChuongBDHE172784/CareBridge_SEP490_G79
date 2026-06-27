import '../../../core/network/api_client.dart';
import '../models/care_group_model.dart';

class CareGroupService {
  // UC70: Create care group
  Future<Map<String, dynamic>> createCareGroup(String groupName, {String? description}) async {
    final data = await apiPost('/api/v1/care-groups', {
      'groupName': groupName,
      if (description != null) 'description': description,
    });
    return data['data'] as Map<String, dynamic>;
  }

  // UC216: List members of a group
  Future<CareGroup> getGroupMembers(String groupId) async {
    final data = await apiGet('/api/v1/care-groups/$groupId/members');
    return CareGroup.fromJson(data['data'] as Map<String, dynamic>);
  }

  // TODO: GET /api/v1/care-groups when list endpoint available (UC-71/83)
  Future<List<CareGroup>> listMyGroups() async {
    return [
      CareGroup(
        id: 'cg-1',
        groupName: 'Gia đình bé Mỡ',
        isActive: true,
        memberCount: 3,
        myRole: 'ADMIN',
        myPermission: 'Toàn quyền theo dõi',
        members: [
          CareGroupMember(memberId: 'm-1', displayName: 'Mẹ Linh', memberRole: 'ADMIN', inviteStatus: 'ACCEPTED'),
          CareGroupMember(memberId: 'm-2', displayName: 'Bố Tuấn', memberRole: 'MEMBER', inviteStatus: 'ACCEPTED'),
          CareGroupMember(memberId: 'm-3', displayName: 'Bà Ngoại', memberRole: 'MEMBER', inviteStatus: 'ACCEPTED'),
        ],
      ),
      CareGroup(
        id: 'cg-2',
        groupName: 'Nhà bé Đậu',
        isActive: false,
        memberCount: 4,
        myRole: 'MEMBER',
        myPermission: 'Chỉ xem lịch',
        members: [],
      ),
    ];
  }

  // TODO: POST /api/v1/care-groups/{id}/invite when endpoint available (UC-71)
  Future<void> inviteMember(String groupId, String phoneOrEmail) async {
    // placeholder
  }

  // TODO: DELETE /api/v1/care-groups/{id}/members/me when endpoint available (UC-72)
  Future<void> leaveGroup(String groupId) async {
    // placeholder
  }
}
