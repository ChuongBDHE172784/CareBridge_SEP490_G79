import 'package:flutter/material.dart';

import 'vaccination_record_form_screen.dart';

class AddVaccinationRecordScreen extends StatelessWidget {
  final String babyId;
  const AddVaccinationRecordScreen({super.key, required this.babyId});

  @override
  Widget build(BuildContext context) =>
      VaccinationRecordFormScreen(babyId: babyId);
}
