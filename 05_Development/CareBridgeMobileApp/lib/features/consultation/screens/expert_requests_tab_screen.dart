import 'package:flutter/material.dart';

import 'expert_request_queue_screen.dart';

class ExpertRequestsTabScreen extends StatelessWidget {
  const ExpertRequestsTabScreen({super.key, this.showBackButton = false});

  final bool showBackButton;

  @override
  Widget build(BuildContext context) {
    final topInset = MediaQuery.of(context).padding.top;
    final canPop = showBackButton || (ModalRoute.of(context)?.canPop ?? false);

    return Scaffold(
      backgroundColor: const Color(0xFFF8F5F1),
      body: SafeArea(
        top: false,
        child: Column(
          children: [
            Container(
              width: double.infinity,
              decoration: const BoxDecoration(
                color: Color(0xFFFFFCF9),
                borderRadius: BorderRadius.vertical(bottom: Radius.circular(24)),
                boxShadow: [
                  BoxShadow(
                    color: Color(0x0A845143),
                    blurRadius: 16,
                    offset: Offset(0, 4),
                  ),
                ],
              ),
              padding: EdgeInsets.fromLTRB(16, topInset + 12, 16, 16),
              child: Row(
                children: [
                  if (canPop) ...[
                    IconButton(
                      padding: EdgeInsets.zero,
                      constraints: const BoxConstraints(),
                      icon: const Icon(
                        Icons.arrow_back_ios_new_rounded,
                        color: Color(0xFF845143),
                        size: 20,
                      ),
                      onPressed: () => Navigator.of(context).pop(),
                    ),
                    const SizedBox(width: 12),
                  ] else
                    const SizedBox(width: 8),
                  const Expanded(
                    child: Text(
                      'Yêu cầu tư vấn',
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: 20,
                        fontWeight: FontWeight.w700,
                        color: Color(0xFF2A211D),
                        letterSpacing: -0.3,
                      ),
                    ),
                  ),
                ],
              ),
            ),
            const Expanded(child: ExpertRequestQueueScreen()),
          ],
        ),
      ),
    );
  }
}
