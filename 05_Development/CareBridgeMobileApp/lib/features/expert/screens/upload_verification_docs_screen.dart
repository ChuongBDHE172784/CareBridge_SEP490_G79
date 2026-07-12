import 'package:flutter/material.dart';

import 'expert_placeholder_screen.dart';

class UploadVerificationDocsScreen extends StatelessWidget {
  const UploadVerificationDocsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const ExpertPlaceholderScreen(
      title: 'Tải lên giấy tờ xác minh',
      subtitle: 'Khu vực xác minh chuyên gia đang chờ implementation đầy đủ.',
      icon: Icons.upload_file_outlined,
    );
  }
}
