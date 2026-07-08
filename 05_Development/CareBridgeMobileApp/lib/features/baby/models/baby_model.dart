
enum BabyGender { male, female, unknown }

extension BabyGenderExtension on BabyGender {
  String get displayLabel {
    switch (this) {
      case BabyGender.male:
        return 'Nam';
      case BabyGender.female:
        return 'Nữ';
      case BabyGender.unknown:
        return '';
    }
  }

  String toApiValue() {
    switch (this) {
      case BabyGender.male:
        return 'MALE';
      case BabyGender.female:
        return 'FEMALE';
      case BabyGender.unknown:
        return 'UNKNOWN';
    }
  }

  static BabyGender fromApi(String? value) {
    switch (value) {
      case 'MALE':
        return BabyGender.male;
      case 'FEMALE':
        return BabyGender.female;
      default:
        return BabyGender.unknown;
    }
  }
}

class BabyProfile {
  final String id;
  final String nickname;
  final DateTime birthDate;
  final BabyGender gender;
  final double? birthWeightKg;
  final double? birthLengthCm;
  final bool isActive;

  const BabyProfile({
    required this.id,
    required this.nickname,
    required this.birthDate,
    required this.gender,
    this.birthWeightKg,
    this.birthLengthCm,
    required this.isActive,
  });

  String get ageLabel {
    final now = DateTime.now();
    final months = (now.year - birthDate.year) * 12 + now.month - birthDate.month;
    if (months < 1) {
      final days = now.difference(birthDate).inDays;
      return '$days ngày tuổi';
    }
    if (months < 12) return '$months tháng tuổi';
    final years = months ~/ 12;
    final rem = months % 12;
    if (rem == 0) return '$years tuổi';
    return '$years tuổi $rem tháng';
  }

  factory BabyProfile.fromJson(Map<String, dynamic> json) {
    return BabyProfile(
      id: json['id'] as String,
      nickname: json['nickname'] as String,
      birthDate: json['birthDate'] != null ? DateTime.parse(json['birthDate'] as String) : DateTime(2000),
      gender: BabyGenderExtension.fromApi(json['gender'] as String?),
      birthWeightKg: (json['birthWeightKg'] as num?)?.toDouble(),
      birthLengthCm: (json['birthLengthCm'] as num?)?.toDouble(),
      isActive: json['isActive'] as bool? ?? false,
    );
  }
}

// UC-32: Update baby profile request (mirrors backend UpdateBabyProfileRequest)
class UpdateBabyProfileRequest {
  final String? nickname;
  final DateTime? birthDate;
  final BabyGender? gender;
  final double? birthWeightKg;
  final double? birthLengthCm;

  const UpdateBabyProfileRequest({
    this.nickname,
    this.birthDate,
    this.gender,
    this.birthWeightKg,
    this.birthLengthCm,
  });

  Map<String, dynamic> toJson() => {
    if (nickname != null) 'nickname': nickname,
    if (birthDate != null) 'birthDate': birthDate!.toIso8601String().split('T')[0],
    if (gender != null) 'gender': gender!.toApiValue(),
    if (birthWeightKg != null) 'birthWeightKg': birthWeightKg,
    if (birthLengthCm != null) 'birthLengthCm': birthLengthCm,
  };
}

class CreateBabyRequest {
  final String nickname;
  final String birthDate;
  final BabyGender gender;
  final double? birthWeightKg;
  final double? birthLengthCm;
  final String? relatedJourneyId;

  const CreateBabyRequest({
    required this.nickname,
    required this.birthDate,
    required this.gender,
    this.birthWeightKg,
    this.birthLengthCm,
    this.relatedJourneyId,
  });

  Map<String, dynamic> toJson() => {
        'nickname': nickname,
        'birthDate': birthDate,
        'gender': gender.toApiValue(),
        if (birthWeightKg != null) 'birthWeightKg': birthWeightKg,
        if (birthLengthCm != null) 'birthLengthCm': birthLengthCm,
        if (relatedJourneyId != null) 'relatedJourneyId': relatedJourneyId,
      };
}
