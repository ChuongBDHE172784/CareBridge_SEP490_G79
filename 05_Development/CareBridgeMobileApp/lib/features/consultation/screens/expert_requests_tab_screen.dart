import 'package:flutter/material.dart';

import '../../community/screens/expert_question_queue_screen.dart';
import 'expert_request_queue_screen.dart';

class ExpertRequestsTabScreen extends StatelessWidget {
  const ExpertRequestsTabScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          automaticallyImplyLeading: false,
          title: const Text('Yêu cầu tư vấn'),
          bottom: const TabBar(
            tabs: [
              Tab(text: 'Tư vấn'),
              Tab(text: 'Cộng đồng'),
            ],
          ),
        ),
        body: const TabBarView(
          children: [
            ExpertRequestQueueScreen(),
            ExpertQuestionQueueScreen(embeddedInShell: true),
          ],
        ),
      ),
    );
  }
}
