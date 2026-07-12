import 'package:flutter/material.dart';

import 'expert_placeholder_screen.dart';

class VerificationStatusScreen extends StatelessWidget {
  const VerificationStatusScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const ExpertPlaceholderScreen(
      title: 'Trạng thái xác minh',
      subtitle: 'Thông tin xét duyệt hồ sơ chuyên gia sẽ hiển thị tại đây.',
      icon: Icons.verified_outlined,
    );
  }
}
