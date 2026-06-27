enum JourneyType {
  prePregnancy,
  pregnancy,
  postpartum,
  babyCare;

  String toApiValue() {
    switch (this) {
      case JourneyType.prePregnancy:
        return 'PRE_PREGNANCY';
      case JourneyType.pregnancy:
        return 'PREGNANCY';
      case JourneyType.postpartum:
        return 'POSTPARTUM';
      case JourneyType.babyCare:
        return 'BABY_CARE';
    }
  }
}

class CreateJourneyRequest {
  final JourneyType journeyType;
  final String startDate;
  final String? estimatedDueDate;
  final String? notes;

  const CreateJourneyRequest({
    required this.journeyType,
    required this.startDate,
    this.estimatedDueDate,
    this.notes,
  });

  Map<String, dynamic> toJson() => {
        'journeyType': journeyType.toApiValue(),
        'startDate': startDate,
        if (estimatedDueDate != null) 'estimatedDueDate': estimatedDueDate,
        if (notes != null) 'notes': notes,
      };
}

class CreateJourneyResponse {
  final String id;
  final String journeyType;
  final String status;
  final String startDate;
  final String? estimatedDueDate;
  final String? notes;
  final String createdAt;

  const CreateJourneyResponse({
    required this.id,
    required this.journeyType,
    required this.status,
    required this.startDate,
    this.estimatedDueDate,
    this.notes,
    required this.createdAt,
  });

  factory CreateJourneyResponse.fromJson(Map<String, dynamic> json) {
    return CreateJourneyResponse(
      id: json['id'] as String,
      journeyType: json['journeyType'] as String,
      status: json['status'] as String,
      startDate: json['startDate'] as String,
      estimatedDueDate: json['estimatedDueDate'] as String?,
      notes: json['notes'] as String?,
      createdAt: json['createdAt'] as String,
    );
  }
}
