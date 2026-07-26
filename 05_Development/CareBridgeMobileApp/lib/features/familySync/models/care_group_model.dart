class CareGroupMember {
  final String memberId;
  final String? userId;
  final String displayName;
  final String memberRole;
  final String inviteStatus;
  final DateTime? joinedAt;

  const CareGroupMember({
    required this.memberId,
    this.userId,
    required this.displayName,
    required this.memberRole,
    required this.inviteStatus,
    this.joinedAt,
  });

  factory CareGroupMember.fromJson(Map<String, dynamic> json) {
    return CareGroupMember(
      memberId: json['memberId'] as String,
      userId: json['userId'] as String?,
      displayName: json['displayName'] as String,
      memberRole: json['memberRole'] as String? ?? 'MEMBER',
      inviteStatus: json['inviteStatus'] as String? ?? 'ACCEPTED',
      joinedAt: json['joinedAt'] != null
          ? DateTime.parse(json['joinedAt'] as String)
          : null,
    );
  }

  bool get isAdmin => memberRole == 'ADMIN' || memberRole == 'OWNER';
  bool get isPending => inviteStatus == 'PENDING';

  String get roleLabel => careGroupRoleLabel(memberRole);
}

/// Shared role label mapping for OWNER/ADMIN/MEMBER/VIEWER (backend GroupMemberRole enum).
String careGroupRoleLabel(String role) {
  switch (role) {
    case 'ADMIN':
    case 'OWNER':
      return 'Quản trị';
    case 'MEMBER':
      return 'Thành viên';
    case 'VIEWER':
      return 'Người xem';
    default:
      return role;
  }
}

class CareGroup {
  final String id;
  final String groupName;
  final String? description;
  final bool isActive;
  final int memberCount;
  final String? myRole;
  final String? myPermission;
  final List<CareGroupMember> members;

  const CareGroup({
    required this.id,
    required this.groupName,
    this.description,
    this.isActive = true,
    this.memberCount = 0,
    this.myRole,
    this.myPermission,
    this.members = const [],
  });

  factory CareGroup.fromJson(Map<String, dynamic> json) {
    return CareGroup(
      id: json['groupId'] as String? ?? json['id'] as String,
      groupName: json['groupName'] as String,
      isActive: json['isActive'] as bool? ?? true,
      memberCount: json['totalMembers'] as int? ?? 0,
      myRole: json['myRole'] as String?,
      members:
          (json['members'] as List<dynamic>?)
              ?.map((m) => CareGroupMember.fromJson(m as Map<String, dynamic>))
              .toList() ??
          [],
    );
  }

  String get groupId => id;
}

/// UC-83 — a pending care-group invitation the current user has not yet
/// accepted/declined (maps to backend PendingInvitationDto).
class PendingInvitation {
  final String groupId;
  final String groupName;
  final String memberRole;
  final DateTime? invitedAt;

  const PendingInvitation({
    required this.groupId,
    required this.groupName,
    required this.memberRole,
    this.invitedAt,
  });

  factory PendingInvitation.fromJson(Map<String, dynamic> json) {
    return PendingInvitation(
      groupId: json['groupId'] as String,
      groupName: json['groupName'] as String? ?? '',
      memberRole: json['memberRole'] as String? ?? 'MEMBER',
      invitedAt: json['invitedAt'] != null
          ? DateTime.parse(json['invitedAt'] as String)
          : null,
    );
  }

  String get roleLabel => careGroupRoleLabel(memberRole);
}

class JoinRequest {
  final String memberId;
  final String userId;
  final String displayName;
  final String? email;
  final String? phone;
  final DateTime? requestedAt;

  const JoinRequest({
    required this.memberId,
    required this.userId,
    required this.displayName,
    this.email,
    this.phone,
    this.requestedAt,
  });

  factory JoinRequest.fromJson(Map<String, dynamic> json) {
    return JoinRequest(
      memberId: json['memberId'] as String,
      userId: json['userId'] as String,
      displayName: json['displayName'] as String? ?? 'Unknown',
      email: json['email'] as String?,
      phone: json['phone'] as String?,
      requestedAt: json['requestedAt'] != null
          ? DateTime.parse(json['requestedAt'] as String)
          : null,
    );
  }
}

