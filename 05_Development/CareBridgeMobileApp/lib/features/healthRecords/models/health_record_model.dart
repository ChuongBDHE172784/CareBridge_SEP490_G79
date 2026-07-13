enum RecordType { vaccination, metric, prescription, checkup, other }

extension RecordTypeExtension on RecordType {
  String get displayLabel {
    switch (this) {
      case RecordType.vaccination:
        return 'Tiêm chủng';
      case RecordType.metric:
        return 'Chỉ số';
      case RecordType.prescription:
        return 'Đơn thuốc';
      case RecordType.checkup:
        return 'Khám bệnh';
      case RecordType.other:
        return 'Khác';
    }
  }

  static RecordType fromApi(String? v) {
    switch (v) {
      case 'VACCINATION':
        return RecordType.vaccination;
      case 'METRIC':
        return RecordType.metric;
      case 'PRESCRIPTION':
        return RecordType.prescription;
      case 'CHECKUP':
        return RecordType.checkup;
      default:
        return RecordType.other;
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

class FileAttachment {
  final String fileId;
  final String originalName;
  final String? mimeType;
  final String? presignedUrl;

  const FileAttachment({
    required this.fileId,
    required this.originalName,
    this.mimeType,
    this.presignedUrl,
  });

  factory FileAttachment.fromJson(Map<String, dynamic> json) {
    return FileAttachment(
      fileId: json['fileId']?.toString() ?? '',
      originalName: json['originalName'] as String? ?? '',
      mimeType: json['mimeType'] as String?,
      presignedUrl: json['presignedUrl'] as String?,
    );
  }

  bool get isPdf =>
      mimeType == 'application/pdf' ||
      originalName.toLowerCase().endsWith('.pdf');
}

class HealthRecordDetail extends HealthRecord {
  final List<FileAttachment> attachments;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  const HealthRecordDetail({
    required super.id,
    required super.recordType,
    required super.title,
    required super.recordDate,
    super.facilityName,
    super.status,
    super.isShared,
    this.attachments = const [],
    this.createdAt,
    this.updatedAt,
  });

  factory HealthRecordDetail.fromJson(Map<String, dynamic> json) {
    final rawAttachments = json['attachments'] as List<dynamic>? ?? [];
    return HealthRecordDetail(
      id: json['id']?.toString() ?? '',
      recordType: RecordTypeExtension.fromApi(json['recordType'] as String?),
      title: json['title'] as String? ?? '',
      recordDate: json['recordDate'] != null
          ? DateTime.parse(json['recordDate'] as String)
          : DateTime.now(),
      facilityName: json['facilityName'] as String?,
      status: json['status'] as String?,
      isShared: json['isShared'] as bool? ?? false,
      attachments: rawAttachments
          .map((e) => FileAttachment.fromJson(e as Map<String, dynamic>))
          .toList(),
      createdAt: json['createdAt'] != null
          ? DateTime.parse(json['createdAt'] as String)
          : null,
      updatedAt: json['updatedAt'] != null
          ? DateTime.parse(json['updatedAt'] as String)
          : null,
    );
  }
}

class UpdateHealthRecordRequest {
  final String? title;
  final String? recordType;
  final DateTime? recordDate;
  final String? sourceType;
  final String? sourceName;
  final String? fileUrl;

  const UpdateHealthRecordRequest({
    this.title,
    this.recordType,
    this.recordDate,
    this.sourceType,
    this.sourceName,
    this.fileUrl,
  });

  Map<String, dynamic> toJson() {
    final m = <String, dynamic>{};
    if (title != null) m['title'] = title;
    if (recordType != null) m['recordType'] = recordType;
    if (recordDate != null) {
      final d = recordDate!;
      m['recordDate'] =
          '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
    }
    if (sourceType != null) m['sourceType'] = sourceType;
    if (sourceName != null) m['sourceName'] = sourceName;
    if (fileUrl != null) m['fileUrl'] = fileUrl;
    return m;
  }
}
