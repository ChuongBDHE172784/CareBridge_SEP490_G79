class EmergencyContact {
  final String? id;
  final String name;
  final String phone;
  final String? relationship;
  final bool primaryContact;
  final DateTime? updatedAt;

  const EmergencyContact({
    this.id,
    required this.name,
    required this.phone,
    this.relationship,
    required this.primaryContact,
    this.updatedAt,
  });

  factory EmergencyContact.fromJson(Map<String, dynamic> json) {
    return EmergencyContact(
      id: json['id'] as String?,
      name: json['name'] as String? ?? '',
      phone: json['phone'] as String? ?? '',
      relationship: json['relationship'] as String?,
      primaryContact: json['primaryContact'] as bool? ?? true,
      updatedAt: json['updatedAt'] != null
          ? DateTime.tryParse(json['updatedAt'] as String)
          : null,
    );
  }

  Map<String, dynamic> toRequestJson() {
    return {
      'name': name,
      'phone': phone,
      'relationship': relationship,
      'primaryContact': primaryContact,
    };
  }

  EmergencyContact copyWith({
    String? id,
    String? name,
    String? phone,
    String? relationship,
    bool? primaryContact,
    DateTime? updatedAt,
  }) {
    return EmergencyContact(
      id: id ?? this.id,
      name: name ?? this.name,
      phone: phone ?? this.phone,
      relationship: relationship ?? this.relationship,
      primaryContact: primaryContact ?? this.primaryContact,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }
}
