import 'package:flutter/material.dart';

import 'expert_placeholder_screen.dart';

class ExpertNearbySupportScreen extends StatelessWidget {
  const ExpertNearbySupportScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const ExpertPlaceholderScreen(
      title: 'Hỗ trợ gần bạn',
      subtitle:
          'Tính năng gợi ý chuyên gia gần vị trí người dùng đang được hoàn thiện.',
      icon: Icons.location_on_outlined,
    );
  }
}
