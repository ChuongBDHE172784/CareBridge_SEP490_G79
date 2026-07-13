import 'package:flutter/material.dart';

import 'expert_placeholder_screen.dart';

class ExpertPublicProfileScreen extends StatelessWidget {
  final String expertProfileId;

  const ExpertPublicProfileScreen({super.key, required this.expertProfileId});

  @override
  Widget build(BuildContext context) {
    return ExpertPlaceholderScreen(
      title: 'Hồ sơ chuyên gia',
      subtitle:
          'Đang chuẩn bị hồ sơ công khai cho chuyên gia $expertProfileId.',
      icon: Icons.person_search_outlined,
    );
  }
}
