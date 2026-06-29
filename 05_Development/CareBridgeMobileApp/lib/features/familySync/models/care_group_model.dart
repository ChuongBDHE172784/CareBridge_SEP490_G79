class CareGroupMember {
  final String memberId;
  final String displayName;
  final String memberRole;
  final String inviteStatus;
  final DateTime? joinedAt;

  const CareGroupMember({
    required this.memberId,
    required this.displayName,
    required this.memberRole,
    required this.inviteStatus,
    this.joinedAt,
  });

  factory CareGroupMember.fromJson(Map<String, dynamic> json) {
    return CareGroupMember(
      memberId: json['memberId'] as String,
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

  String get roleLabel {
    switch (memberRole) {
      case 'ADMIN': return 'Quản trị';
      case 'OWNER': return 'Quản trị';
      case 'MEMBER': return 'Thành viên';
      default: return memberRole;
    }
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
      members: (json['members'] as List<dynamic>?)
              ?.map((m) => CareGroupMember.fromJson(m as Map<String, dynamic>))
              .toList() ??
          [],
    );
  }
}
