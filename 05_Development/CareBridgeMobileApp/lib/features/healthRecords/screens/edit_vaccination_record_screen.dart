import 'package:flutter/material.dart';

import '../models/vaccination_model.dart';
import 'vaccination_record_form_screen.dart';

class EditVaccinationRecordScreen extends StatelessWidget {
  final String babyId;
  final String recordId;
  final VaccinationRecord? initialRecord;

  const EditVaccinationRecordScreen({
    super.key,
    required this.babyId,
    required this.recordId,
    this.initialRecord,
  });

  @override
  Widget build(BuildContext context) => VaccinationRecordFormScreen(
    babyId: babyId,
    recordId: recordId,
    initialRecord: initialRecord,
  );
}
