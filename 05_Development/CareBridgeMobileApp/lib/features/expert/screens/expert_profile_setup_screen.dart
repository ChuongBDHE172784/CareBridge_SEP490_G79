import 'package:flutter/material.dart';

import 'expert_placeholder_screen.dart';

class ExpertProfileSetupScreen extends StatelessWidget {
  const ExpertProfileSetupScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const ExpertPlaceholderScreen(
      title: 'Thiết lập hồ sơ chuyên gia',
      subtitle:
          'Màn hình hồ sơ chuyên gia sẽ được hoàn thiện trong luồng expert.',
      icon: Icons.badge_outlined,
    );
  }
}
