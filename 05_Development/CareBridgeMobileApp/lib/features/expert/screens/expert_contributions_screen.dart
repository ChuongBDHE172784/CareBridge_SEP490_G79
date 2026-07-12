import 'package:flutter/material.dart';

import 'expert_placeholder_screen.dart';

class ExpertContributionsScreen extends StatelessWidget {
  const ExpertContributionsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const ExpertPlaceholderScreen(
      title: 'Đóng góp chuyên gia',
      subtitle: 'Danh sách câu trả lời và nội dung chuyên gia sẽ hiển thị tại đây.',
      icon: Icons.workspace_premium_outlined,
    );
  }
}
