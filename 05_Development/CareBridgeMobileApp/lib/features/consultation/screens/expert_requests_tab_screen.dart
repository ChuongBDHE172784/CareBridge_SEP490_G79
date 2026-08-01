import 'package:flutter/material.dart';

import 'expert_request_queue_screen.dart';

class ExpertRequestsTabScreen extends StatelessWidget {
  const ExpertRequestsTabScreen({super.key, this.showBackButton = false});

  final bool showBackButton;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Color(0xFFF6F1EC),
      appBar: AppBar(
        automaticallyImplyLeading: showBackButton,
        backgroundColor: Color(0xFFF6F1EC),
        surfaceTintColor: Color(0xFFF6F1EC),
        title: Text(
          'Yêu cầu tư vấn',
          style: TextStyle(
            color: Color(0xFF271812),
            fontFamily: 'Lexend',
            fontSize: 22,
            fontWeight: FontWeight.w800,
          ),
        ),
      ),
      body: const ExpertRequestQueueScreen(),
    );
  }
}
