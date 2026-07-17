import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../directChat/services/direct_chat_service.dart';
import '../../consultation/screens/consultation_request_form_screen.dart';

class ExpertPublicProfileScreen extends StatefulWidget {
  final String expertProfileId;

  const ExpertPublicProfileScreen({super.key, required this.expertProfileId});

  @override
  State<ExpertPublicProfileScreen> createState() =>
      _ExpertPublicProfileScreenState();
}

class _ExpertPublicProfileScreenState extends State<ExpertPublicProfileScreen> {
  static const _canvas = Color(0xFFF6F1EC);
  static const _surface = Colors.white;
  static const _primary = Color(0xFF845143);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  late Future<Map<String, dynamic>> _future;
  bool _startingChat = false;

  @override
  void initState() {
    super.initState();
    _future = _loadProfile();
  }

  Future<Map<String, dynamic>> _loadProfile() =>
      DirectChatService.instance.getExpertProfile(widget.expertProfileId);

  Future<void> _startChat() async {
    setState(() => _startingChat = true);
    try {
      // UC-144D find-or-create — reuses the existing conversation if Mother already
      // messaged this Expert before (BR-DCC-002), never creates a duplicate.
      final conversation = await DirectChatService.instance
          .findOrCreateConversation(widget.expertProfileId);
      if (!mounted) return;
      context.push('/direct-chat/${conversation.conversationId}');
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Không thể mở cuộc trò chuyện: $e')),
      );
    } finally {
      if (mounted) setState(() => _startingChat = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      appBar: AppBar(
        backgroundColor: _surface,
        elevation: 0.5,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: _primary),
          onPressed: () => Navigator.of(context).maybePop(),
        ),
        title: const Text(
          'Hồ sơ chuyên gia',
          style: TextStyle(
            color: _primary,
            fontSize: 18,
            fontWeight: FontWeight.w700,
          ),
        ),
        centerTitle: true,
      ),
      body: FutureBuilder<Map<String, dynamic>>(
        future: _future,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return Center(child: Text('Lỗi tải hồ sơ: ${snapshot.error}'));
          }
          final profile = snapshot.data!;
          final isConsultationEligible =
              profile['isConsultationEligible'] == true;
          final displayName = profile['displayName'] as String?;
          final professionalTitle = profile['professionalTitle'] as String?;
          final expertDisplayName =
              displayName ?? professionalTitle ?? 'Chuyên gia';
          return Padding(
            padding: const EdgeInsets.all(24),
            child: Container(
              width: double.infinity,
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: _surface,
                borderRadius: BorderRadius.circular(16),
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    expertDisplayName,
                    style: const TextStyle(
                      color: _onSurface,
                      fontSize: 22,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  if (displayName != null &&
                      professionalTitle != null &&
                      professionalTitle != displayName) ...[
                    const SizedBox(height: 4),
                    Text(
                      professionalTitle,
                      style: const TextStyle(
                        color: _onSurfaceVariant,
                        fontSize: 15,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                  const SizedBox(height: 8),
                  Text(
                    profile['specialty'] as String? ?? '',
                    style: const TextStyle(
                      color: _onSurfaceVariant,
                      fontSize: 14,
                    ),
                  ),
                  if (profile['consultationScope'] != null) ...[
                    const SizedBox(height: 16),
                    Text(
                      profile['consultationScope'] as String,
                      style: const TextStyle(
                        color: _onSurfaceVariant,
                        fontSize: 13,
                        height: 1.4,
                      ),
                    ),
                  ],
                  const SizedBox(height: 24),
                  Row(
                    children: [
                      Expanded(
                        child: FilledButton(
                          onPressed: isConsultationEligible && !_startingChat
                              ? _startChat
                              : null,
                          child: _startingChat
                              ? const SizedBox(
                                  width: 18,
                                  height: 18,
                                  child: CircularProgressIndicator(
                                    strokeWidth: 2,
                                    color: Colors.white,
                                  ),
                                )
                              : const Text('Trò chuyện'),
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: OutlinedButton(
                          onPressed: isConsultationEligible
                              ? () => Navigator.of(context).push(
                                  MaterialPageRoute(
                                    builder: (_) =>
                                        ConsultationRequestFormScreen(
                                          expertProfileId:
                                              widget.expertProfileId,
                                          expertDisplayName: expertDisplayName,
                                        ),
                                  ),
                                )
                              : null,
                          child: const Text('Yêu cầu tư vấn'),
                        ),
                      ),
                    ],
                  ),
                  if (!isConsultationEligible) ...[
                    const SizedBox(height: 8),
                    const Text(
                      'Chuyên gia hiện chưa thể nhận tương tác tư vấn mới.',
                      style: TextStyle(color: _onSurfaceVariant, fontSize: 12),
                    ),
                  ],
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}
