import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../checklist/services/user_checklist_service.dart';
import '../../checklist/widgets/add_user_checklist_task_button.dart';
import '../services/today_task_service.dart';
import '../widgets/today_tasks_panel.dart';

/// Unified Today Tasks destination for checklist, reminder and care-task work.
class TodayTasksScreen extends StatefulWidget {
  const TodayTasksScreen({
    super.key,
    this.service,
    this.audience = TodayTasksAudience.mother,
    this.journeyId,
    this.babyId,
    this.userChecklistService,
  });

  final TodayTaskService? service;
  final TodayTasksAudience audience;
  final String? journeyId;
  final String? babyId;
  final UserChecklistService? userChecklistService;

  @override
  State<TodayTasksScreen> createState() => _TodayTasksScreenState();
}

class _TodayTasksScreenState extends State<TodayTasksScreen> {
  final TodayTasksPanelController _todayTasksController =
      TodayTasksPanelController();

  @override
  Widget build(BuildContext context) {
    final hasCreateContext =
        (widget.journeyId != null && widget.journeyId!.isNotEmpty) !=
        (widget.babyId != null && widget.babyId!.isNotEmpty);
    return Scaffold(
      backgroundColor: const Color(0xFFF8F5F1),
      appBar: AppBar(
        backgroundColor: const Color(0xFFF8F5F1),
        foregroundColor: const Color(0xFF2A211D),
        elevation: 0,
        actions: [
          if (widget.audience == TodayTasksAudience.mother)
            IconButton(
              key: const Key('today-checklist-history-button'),
              tooltip: 'Lịch sử checklist',
              onPressed: () => context.push('/checklists/history'),
              icon: const Icon(Icons.history_rounded),
            ),
        ],
        title: const Text(
          'Việc hôm nay',
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 22,
            fontWeight: FontWeight.w700,
            color: Color(0xFF2A211D),
            letterSpacing: -0.3,
          ),
        ),
      ),
      body: SafeArea(
        top: false,
        child: RefreshIndicator(
          color: const Color(0xFFC98C7B),
          onRefresh: _todayTasksController.refresh,
          child: ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.fromLTRB(20, 16, 20, 40),
            children: [
              if (widget.audience == TodayTasksAudience.mother &&
                  hasCreateContext) ...[
                Align(
                  alignment: Alignment.centerRight,
                  child: AddUserChecklistTaskButton(
                    journeyId: widget.journeyId,
                    babyId: widget.babyId,
                    service: widget.userChecklistService,
                    onCreated: _todayTasksController.refresh,
                  ),
                ),
                const SizedBox(height: 16),
              ],
              TodayTasksPanel(
                service: widget.service,
                audience: widget.audience,
                showHeading: false,
                controller: _todayTasksController,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
