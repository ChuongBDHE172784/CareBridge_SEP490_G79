class OtpSendResponse {
  final String message;
  final int expiresIn;
  final String? userId;
  final String? otpExpiresAt;

  const OtpSendResponse({
    required this.message,
    required this.expiresIn,
    this.userId,
    this.otpExpiresAt,
  });

  factory OtpSendResponse.fromJson(Map<String, dynamic> json) {
    return OtpSendResponse(
      message: json['message'] as String? ?? '',
      expiresIn: json['expiresIn'] as int? ?? 0,
      userId: json['userId']?.toString(),
      otpExpiresAt: json['otpExpiresAt']?.toString(),
    );
  }
}

class UserProfile {
  final String id;
  final String? name;
  final String? email;
  final String? phone;
  final String? avatarUrl;
  final String role;
  final String? accountStatus;

  const UserProfile({
    required this.id,
    this.name,
    this.email,
    this.phone,
    this.avatarUrl,
    required this.role,
    this.accountStatus,
  });

  factory UserProfile.fromJson(Map<String, dynamic> json) {
    return UserProfile(
      id: json['id']?.toString() ?? '',
      name: json['name'] as String?,
      email: json['email'] as String?,
      phone: json['phone'] as String?,
      avatarUrl: json['avatarUrl'] as String?,
      role: json['role']?.toString() ?? '',
      accountStatus: json['accountStatus'] as String?,
    );
  }
}

class AuthResponse {
  final String accessToken;
  final String refreshToken;
  final UserProfile user;

  const AuthResponse({
    required this.accessToken,
    required this.refreshToken,
    required this.user,
  });

  factory AuthResponse.fromJson(Map<String, dynamic> json) {
    return AuthResponse(
      accessToken: json['accessToken'] as String? ?? '',
      refreshToken: json['refreshToken'] as String? ?? '',
      user: UserProfile.fromJson(json['user'] as Map<String, dynamic>? ?? {}),
    );
  }
}
