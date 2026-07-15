import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../core/network/api_client.dart';
import '../services/direct_chat_service.dart';

/// Minimal entry point for UC-144D: "Mother chọn Verified Expert, nhấn Trò chuyện" (TDS
/// §ADR-DCC-001 bối cảnh). No standalone Expert-directory browsing screen existed anywhere
/// in the app before this feature (verified: no screen previously called
/// GET /api/v1/expert/directory) — this list is intentionally minimal (name/specialty +
/// button only), not a full profile-view page, to stay within this feature's scope.
class ExpertDirectoryScreen extends StatefulWidget {
  const ExpertDirectoryScreen({super.key});

  @override
  State<ExpertDirectoryScreen> createState() => _ExpertDirectoryScreenState();
}

class _ExpertDirectoryScreenState extends State<ExpertDirectoryScreen> {
  late Future<List<Map<String, dynamic>>> _future;
  String? _startingChatWithExpertProfileId;

  @override
  void initState() {
    super.initState();
    _future = _loadExperts();
  }

  Future<List<Map<String, dynamic>>> _loadExperts() async {
    final response = await apiGet(
      '/api/v1/expert/directory',
      queryParams: {'page': 0, 'size': 50},
    );
    final data = response['data'] as Map<String, dynamic>;
    return (data['experts'] as List? ?? []).cast<Map<String, dynamic>>();
  }

  Future<void> _startChat(Map<String, dynamic> expert) async {
    final expertProfileId = expert['expertProfileId'] as String;
    setState(() => _startingChatWithExpertProfileId = expertProfileId);
    try {
      final conversation =
          await DirectChatService.instance.findOrCreateConversation(expertProfileId);
      if (!mounted) return;
      context.push('/direct-chat/${conversation.conversationId}');
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Không thể mở cuộc trò chuyện: $e')),
      );
    } finally {
      if (mounted) setState(() => _startingChatWithExpertProfileId = null);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Chuyên gia đã xác thực')),
      body: FutureBuilder<List<Map<String, dynamic>>>(
        future: _future,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return Center(child: Text('Lỗi tải danh sách: ${snapshot.error}'));
          }
          final experts = snapshot.data ?? const [];
          if (experts.isEmpty) {
            return const Center(child: Text('Chưa có chuyên gia nào được xác thực.'));
          }
          return ListView.separated(
            itemCount: experts.length,
            separatorBuilder: (_, __) => const Divider(height: 1),
            itemBuilder: (context, index) {
              final expert = experts[index];
              final expertProfileId = expert['expertProfileId'] as String;
              final isStarting = _startingChatWithExpertProfileId == expertProfileId;
              return ListTile(
                title: Text(expert['professionalTitle'] as String? ?? 'Chuyên gia'),
                subtitle: Text(expert['specialty'] as String? ?? ''),
                trailing: FilledButton(
                  onPressed: isStarting ? null : () => _startChat(expert),
                  child: isStarting
                      ? const SizedBox(
                          width: 16,
                          height: 16,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Text('Trò chuyện'),
                ),
              );
            },
          );
        },
      ),
    );
  }
}
