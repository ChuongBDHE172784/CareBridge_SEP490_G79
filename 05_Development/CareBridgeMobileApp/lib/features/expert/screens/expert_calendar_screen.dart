import 'package:flutter/material.dart';

import 'expert_placeholder_screen.dart';

class ExpertCalendarScreen extends StatelessWidget {
  const ExpertCalendarScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const ExpertPlaceholderScreen(
      title: 'Lịch chuyên gia',
      subtitle: 'Lịch làm việc và khung giờ tư vấn sẽ được bổ sung sau.',
      icon: Icons.calendar_month_outlined,
    );
  }
}
