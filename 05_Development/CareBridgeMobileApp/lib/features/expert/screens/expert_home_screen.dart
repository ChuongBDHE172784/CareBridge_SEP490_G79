import 'package:flutter/material.dart';

import 'expert_placeholder_screen.dart';

class ExpertHomeScreen extends StatelessWidget {
  const ExpertHomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const ExpertPlaceholderScreen(
      title: 'Trang chủ chuyên gia',
      subtitle: 'Dashboard chuyên gia đang chờ màn hình hoàn chỉnh.',
      icon: Icons.dashboard_outlined,
    );
  }
}
