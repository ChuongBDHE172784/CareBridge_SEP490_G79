enum VaccinationStatus {
  scheduled,
  completed,
  postponed,
  overdue,
  missed,
  rescheduled,
  deleted,
  unknown,
}

extension VaccinationStatusExtension on VaccinationStatus {
  String get displayLabel => switch (this) {
    VaccinationStatus.scheduled => 'Đã lên lịch',
    VaccinationStatus.completed => 'Đã hoàn thành',
    VaccinationStatus.postponed => 'Đã hoãn',
    VaccinationStatus.overdue => 'Quá hạn',
    VaccinationStatus.missed => 'Bỏ lỡ',
    VaccinationStatus.rescheduled => 'Đã dời lịch',
    VaccinationStatus.deleted => 'Đã xoá',
    VaccinationStatus.unknown => 'Không rõ',
  };

  static VaccinationStatus fromApi(String? value) {
    switch (value?.trim().toUpperCase()) {
      case 'COMPLETED':
        return VaccinationStatus.completed;
      case 'POSTPONED':
        return VaccinationStatus.postponed;
      case 'OVERDUE':
        return VaccinationStatus.overdue;
      case 'MISSED':
        return VaccinationStatus.missed;
      case 'RESCHEDULED':
        return VaccinationStatus.rescheduled;
      case 'DELETED':
        return VaccinationStatus.deleted;
      case 'SCHEDULED':
        return VaccinationStatus.scheduled;
      default:
        return VaccinationStatus.unknown;
    }
  }
}

class VaccinationRecord {
  /// The API calls this field `id`; `vaccinationId` is retained as the
  /// navigation-facing name used by existing screens.
  final String vaccinationId;
  final String vaccineName;
  final VaccinationStatus status;
  final DateTime? plannedDate;
  final DateTime? actualDate;
  final int? doseNumber;
  final String? babyId;
  final String? facilityName;
  final String? facilityAddress;
  final String? childId;
  final String? childName;
  final DateTime? childBirthDate;
  final String? note;
  final String? postponeReason;
  final String? proofRecordId;

  const VaccinationRecord({
    required this.vaccinationId,
    required this.vaccineName,
    required this.status,
    required this.plannedDate,
    this.actualDate,
    this.doseNumber,
    this.babyId,
    this.facilityName,
    this.facilityAddress,
    this.childId,
    this.childName,
    this.childBirthDate,
    this.note,
    this.postponeReason,
    this.proofRecordId,
  });

  String get id => vaccinationId;
  DateTime? get scheduledDate => plannedDate;
  DateTime? get administeredDate => actualDate;

  factory VaccinationRecord.fromJson(Map<String, dynamic> json) {
    final id = (json['id'] ?? json['vaccinationId'])?.toString();
    final name = json['vaccineName']?.toString();
    if (id == null || id.isEmpty || name == null || name.isEmpty) {
      throw const FormatException('Invalid vaccination record payload');
    }
    final postponeReason = json['postponeReason']?.toString();
    return VaccinationRecord(
      vaccinationId: id,
      vaccineName: name,
      status: VaccinationStatusExtension.fromApi(json['status']?.toString()),
      plannedDate: _parseDate(json['scheduledDate'] ?? json['plannedDate']),
      actualDate: _parseDate(json['administeredDate'] ?? json['actualDate']),
      doseNumber: (json['doseNumber'] as num?)?.toInt(),
      babyId: (json['babyId'] ?? json['childId'])?.toString(),
      facilityName: json['facilityName'] as String?,
      facilityAddress: json['facilityAddress'] as String?,
      childId: json['childId'] as String?,
      childName: json['childName'] as String?,
      childBirthDate: _parseDate(json['childBirthDate']),
      note: (json['note'] ?? postponeReason) as String?,
      postponeReason: postponeReason,
      proofRecordId: json['proofRecordId']?.toString(),
    );
  }

  String get plannedDateLabel =>
      plannedDate == null ? '—' : _formatDate(plannedDate!);

  String get actualDateLabel =>
      actualDate == null ? '—' : _formatDate(actualDate!);

  String get childAgeLabel {
    if (childBirthDate == null) return '';
    final now = DateTime.now();
    final months =
        (now.year - childBirthDate!.year) * 12 +
        (now.month - childBirthDate!.month);
    if (months < 24) return '$months tháng tuổi';
    return '${months ~/ 12} tuổi';
  }

  String get childBirthLabel {
    if (childBirthDate == null) return '';
    return 'Sinh ngày: ${_formatDate(childBirthDate!)} ($childAgeLabel)';
  }

  static DateTime? _parseDate(dynamic value) {
    if (value is DateTime) return value;
    if (value is! String || value.isEmpty) return null;
    final parsed = DateTime.tryParse(value);
    if (parsed == null) {
      throw FormatException('Invalid vaccination date: $value');
    }
    return parsed.toLocal();
  }

  static String _formatDate(DateTime date) {
    final local = date.toLocal();
    return '${local.day.toString().padLeft(2, '0')}/'
        '${local.month.toString().padLeft(2, '0')}/${local.year}';
  }
}

class VaccinationDose {
  final String vaccineName;
  final int doseNumber;
  final DateTime? expectedDate;
  final DateTime? scheduledDate;
  final DateTime? administeredDate;
  final VaccinationStatus status;
  final String? postponeReason;

  const VaccinationDose({
    required this.vaccineName,
    required this.doseNumber,
    this.expectedDate,
    this.scheduledDate,
    this.administeredDate,
    required this.status,
    this.postponeReason,
  });

  DateTime? get effectiveDate => scheduledDate ?? expectedDate;

  factory VaccinationDose.fromJson(Map<String, dynamic> json) =>
      VaccinationDose(
        vaccineName: json['vaccineName']?.toString() ?? '',
        doseNumber: (json['doseNumber'] as num?)?.toInt() ?? 0,
        expectedDate: VaccinationRecord._parseDate(json['expectedDate']),
        scheduledDate: VaccinationRecord._parseDate(json['scheduledDate']),
        administeredDate: VaccinationRecord._parseDate(
          json['administeredDate'],
        ),
        status: VaccinationStatusExtension.fromApi(json['status']?.toString()),
        postponeReason: json['postponeReason'] as String?,
      );
}

class VaccinationSchedule {
  final String babyId;
  final String? babyNickname;
  final List<VaccinationDose> doses;

  const VaccinationSchedule({
    required this.babyId,
    this.babyNickname,
    required this.doses,
  });

  factory VaccinationSchedule.fromJson(Map<String, dynamic> json) {
    final rawDoses = json['doses'] as List? ?? const [];
    return VaccinationSchedule(
      babyId: json['babyId']?.toString() ?? '',
      babyNickname: json['babyNickname'] as String?,
      doses: rawDoses
          .whereType<Map>()
          .map(
            (item) => VaccinationDose.fromJson(Map<String, dynamic>.from(item)),
          )
          .where(
            (dose) => dose.vaccineName.trim().isNotEmpty && dose.doseNumber > 0,
          )
          .toList(growable: false),
    );
  }
}

/// Contract-safe payload builder shared by add and edit forms.
class VaccinationRecordPayload {
  const VaccinationRecordPayload._();

  static Map<String, dynamic> save({
    required String vaccineName,
    required int doseNumber,
    DateTime? administeredDate,
    String? facilityName,
  }) {
    return <String, dynamic>{
      'vaccineName': vaccineName.trim(),
      'doseNumber': doseNumber,
      if (administeredDate != null)
        'administeredDate': _dateOnly(administeredDate),
      if (facilityName != null) 'facilityName': facilityName.trim(),
    };
  }

  static String _dateOnly(DateTime value) {
    final local = value.toLocal();
    return '${local.year.toString().padLeft(4, '0')}-'
        '${local.month.toString().padLeft(2, '0')}-'
        '${local.day.toString().padLeft(2, '0')}';
  }
}
