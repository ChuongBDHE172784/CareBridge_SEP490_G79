enum RecordType { vaccination, metric, prescription, checkup, other }

extension RecordTypeExtension on RecordType {
  String get displayLabel {
    switch (this) {
      case RecordType.vaccination: return 'Tiêm chủng';
      case RecordType.metric: return 'Chỉ số';
      case RecordType.prescription: return 'Đơn thuốc';
      case RecordType.checkup: return 'Khám bệnh';
      case RecordType.other: return 'Khác';
    }
  }

  static RecordType fromApi(String? v) {
    switch (v) {
      case 'VACCINATION': return RecordType.vaccination;
      case 'METRIC': return RecordType.metric;
      case 'PRESCRIPTION': return RecordType.prescription;
      case 'CHECKUP': return RecordType.checkup;
      default: return RecordType.other;
    }
  }
}

class HealthRecord {
  final String id;
  final RecordType recordType;
  final String title;
  final DateTime recordDate;
  final String? facilityName;
  final String? status;
  final bool isShared;

  const HealthRecord({
    required this.id,
    required this.recordType,
    required this.title,
    required this.recordDate,
    this.facilityName,
    this.status,
    this.isShared = false,
  });

  factory HealthRecord.fromJson(Map<String, dynamic> json) {
    return HealthRecord(
      id: json['id'] as String,
      recordType: RecordTypeExtension.fromApi(json['recordType'] as String?),
      title: json['title'] as String,
      recordDate: DateTime.parse(json['recordDate'] as String),
      facilityName: json['facilityName'] as String?,
      status: json['status'] as String?,
      isShared: json['isShared'] as bool? ?? false,
    );
  }
}
