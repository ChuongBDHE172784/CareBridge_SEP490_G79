enum VaccinationStatus { scheduled, completed, missed, rescheduled }

extension VaccinationStatusExtension on VaccinationStatus {
  String get displayLabel {
    switch (this) {
      case VaccinationStatus.scheduled: return 'Đã lên lịch';
      case VaccinationStatus.completed: return 'Đã hoàn thành';
      case VaccinationStatus.missed: return 'Bỏ lỡ';
      case VaccinationStatus.rescheduled: return 'Đã dời lịch';
    }
  }

  static VaccinationStatus fromApi(String? v) {
    switch (v) {
      case 'COMPLETED': return VaccinationStatus.completed;
      case 'MISSED': return VaccinationStatus.missed;
      case 'RESCHEDULED': return VaccinationStatus.rescheduled;
      default: return VaccinationStatus.scheduled;
    }
  }
}

class VaccinationRecord {
  final String vaccinationId;
  final String vaccineName;
  final VaccinationStatus status;
  final DateTime plannedDate;
  final DateTime? actualDate;
  final String? facilityName;
  final String? facilityAddress;
  final String? childId;
  final String? childName;
  final DateTime? childBirthDate;
  final String? note;

  const VaccinationRecord({
    required this.vaccinationId,
    required this.vaccineName,
    required this.status,
    required this.plannedDate,
    this.actualDate,
    this.facilityName,
    this.facilityAddress,
    this.childId,
    this.childName,
    this.childBirthDate,
    this.note,
  });

  factory VaccinationRecord.fromJson(Map<String, dynamic> json) {
    return VaccinationRecord(
      vaccinationId: json['vaccinationId'] as String,
      vaccineName: json['vaccineName'] as String,
      status: VaccinationStatusExtension.fromApi(json['status'] as String?),
      plannedDate: DateTime.parse(json['plannedDate'] as String),
      actualDate: json['actualDate'] != null ? DateTime.parse(json['actualDate'] as String) : null,
      facilityName: json['facilityName'] as String?,
      facilityAddress: json['facilityAddress'] as String?,
      childId: json['childId'] as String?,
      childName: json['childName'] as String?,
      childBirthDate: json['childBirthDate'] != null ? DateTime.parse(json['childBirthDate'] as String) : null,
      note: json['note'] as String?,
    );
  }

  String _formatDate(DateTime d) =>
      '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';

  String get plannedDateLabel => _formatDate(plannedDate);
  String get actualDateLabel => actualDate != null ? _formatDate(actualDate!) : '—';

  String get childAgeLabel {
    if (childBirthDate == null) return '';
    final now = DateTime.now();
    final months = (now.year - childBirthDate!.year) * 12 + (now.month - childBirthDate!.month);
    if (months < 24) return '$months tháng tuổi';
    final years = months ~/ 12;
    return '$years tuổi';
  }

  String get childBirthLabel {
    if (childBirthDate == null) return '';
    return 'Sinh ngày: ${_formatDate(childBirthDate!)} ($childAgeLabel)';
  }
}
