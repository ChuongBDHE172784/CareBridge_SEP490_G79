import 'package:flutter/material.dart';

import 'expert_placeholder_screen.dart';

class ExpertAnswerComposerScreen extends StatelessWidget {
  const ExpertAnswerComposerScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const ExpertPlaceholderScreen(
      title: 'Soạn câu trả lời',
      subtitle: 'Trình soạn câu trả lời chuyên gia sẽ được nối API sau.',
      icon: Icons.edit_note_outlined,
    );
  }
}
