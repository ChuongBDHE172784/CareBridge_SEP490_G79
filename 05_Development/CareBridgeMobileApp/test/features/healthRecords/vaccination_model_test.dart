import 'package:flutter_test/flutter_test.dart';

import 'package:untitled/features/healthRecords/models/vaccination_model.dart';

void main() {
  test('parses the live vaccination record field names', () {
    final record = VaccinationRecord.fromJson({
      'id': 'record-1',
      'babyId': 'baby-1',
      'vaccineName': '6 trong 1',
      'doseNumber': 2,
      'scheduledDate': '2026-08-12',
      'administeredDate': '2026-08-01',
      'status': 'COMPLETED',
      'facilityName': 'VNVC',
      'proofRecordId': 'proof-1',
    });

    expect(record.vaccinationId, 'record-1');
    expect(record.babyId, 'baby-1');
    expect(record.doseNumber, 2);
    expect(record.plannedDate, DateTime(2026, 8, 12));
    expect(record.actualDate, DateTime(2026, 8, 1));
    expect(record.status, VaccinationStatus.completed);
    expect(record.proofRecordId, 'proof-1');
  });

  test('builds add/edit payloads with date-only numeric contract', () {
    final payload = VaccinationRecordPayload.save(
      vaccineName: '  6 trong 1  ',
      doseNumber: 3,
      administeredDate: DateTime(2026, 8, 5, 23, 45),
      facilityName: '  VNVC  ',
    );

    expect(payload, {
      'vaccineName': '6 trong 1',
      'doseNumber': 3,
      'administeredDate': '2026-08-05',
      'facilityName': 'VNVC',
    });
    expect(payload, isNot(contains('actualDate')));
    expect(payload, isNot(contains('remindNext')));
  });
}
