enum RecordType {
  ultrasound,
  labResult,
  prescription,
  vaccination,
  examinationResult,
  note,
}

DateTime? _parseOptionalDateTime(dynamic value) {
  if (value == null) return null;
  return DateTime.parse(value as String).toLocal();
}

extension RecordTypeExtension on RecordType {
  String get displayLabel {
    switch (this) {
      case RecordType.ultrasound:
        return 'Siêu âm';
      case RecordType.labResult:
        return 'Kết quả xét nghiệm';
      case RecordType.prescription:
        return 'Đơn thuốc';
      case RecordType.vaccination:
        return 'Tiêm chủng';
      case RecordType.examinationResult:
        return 'Kết quả khám';
      case RecordType.note:
        return 'Ghi chú';
    }
  }

  static RecordType fromApi(String? v) {
    switch (v) {
      case 'ULTRASOUND':
        return RecordType.ultrasound;
      case 'LAB_RESULT':
      case 'METRIC':
        return RecordType.labResult;
      case 'PRESCRIPTION':
        return RecordType.prescription;
      case 'VACCINATION':
      case 'VACCINATION_FORM':
        return RecordType.vaccination;
      case 'CHECKUP':
      case 'EXAMINATION_RESULT':
        return RecordType.examinationResult;
      case 'NOTE':
      default:
        return RecordType.note;
    }
  }

  String get apiValue {
    switch (this) {
      case RecordType.ultrasound:
        return 'ULTRASOUND';
      case RecordType.labResult:
        return 'LAB_RESULT';
      case RecordType.prescription:
        return 'PRESCRIPTION';
      case RecordType.vaccination:
        return 'VACCINATION_FORM';
      case RecordType.examinationResult:
        return 'EXAMINATION_RESULT';
      case RecordType.note:
        return 'NOTE';
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
  final DateTime? createdAt;

  const HealthRecord({
    required this.id,
    required this.recordType,
    required this.title,
    required this.recordDate,
    this.facilityName,
    this.status,
    this.isShared = false,
    this.createdAt,
  });

  factory HealthRecord.fromJson(Map<String, dynamic> json) {
    return HealthRecord(
      id: (json['id'] ?? json['healthRecordId']).toString(),
      recordType: RecordTypeExtension.fromApi(json['recordType'] as String?),
      title: json['title'] as String,
      recordDate: DateTime.parse(json['recordDate'] as String),
      facilityName: json['facilityName'] as String?,
      status: json['status'] as String?,
      isShared: json['isShared'] as bool? ?? false,
      createdAt: _parseOptionalDateTime(json['createdAt']),
    );
  }
}

class FileAttachment {
  final String fileId;
  final String originalName;
  final String? mimeType;
  final String? presignedUrl;
  final DateTime? createdAt;

  const FileAttachment({
    required this.fileId,
    required this.originalName,
    this.mimeType,
    this.presignedUrl,
    this.createdAt,
  });

  factory FileAttachment.fromJson(Map<String, dynamic> json) {
    return FileAttachment(
      fileId: json['fileId']?.toString() ?? '',
      originalName: json['originalName'] as String? ?? '',
      mimeType: json['mimeType'] as String?,
      presignedUrl: json['presignedUrl'] as String?,
      createdAt: _parseOptionalDateTime(json['createdAt']),
    );
  }

  bool get isPdf =>
      mimeType == 'application/pdf' ||
      originalName.toLowerCase().endsWith('.pdf');
}

class HealthRecordDetail extends HealthRecord {
  final List<FileAttachment> attachments;
  final DateTime? updatedAt;

  const HealthRecordDetail({
    required super.id,
    required super.recordType,
    required super.title,
    required super.recordDate,
    super.facilityName,
    super.status,
    super.isShared,
    super.createdAt,
    this.attachments = const [],
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
      createdAt: _parseOptionalDateTime(json['createdAt']),
      attachments: rawAttachments
          .map((e) => FileAttachment.fromJson(e as Map<String, dynamic>))
          .toList(),
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
