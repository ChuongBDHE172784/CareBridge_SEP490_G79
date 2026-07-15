import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../models/direct_conversation.dart';
import '../services/direct_chat_service.dart';

class ConversationListScreen extends StatefulWidget {
  const ConversationListScreen({super.key});

  @override
  State<ConversationListScreen> createState() => _ConversationListScreenState();
}

class _ConversationListScreenState extends State<ConversationListScreen> {
  late Future<List<DirectConversationSummary>> _future;

  @override
  void initState() {
    super.initState();
    _future = DirectChatService.instance.listMyConversations();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Trò chuyện')),
      body: FutureBuilder<List<DirectConversationSummary>>(
        future: _future,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return Center(child: Text('Lỗi tải danh sách: ${snapshot.error}'));
          }
          final conversations = snapshot.data ?? const [];
          if (conversations.isEmpty) {
            return const Center(child: Text('Chưa có cuộc trò chuyện nào.'));
          }
          return ListView.separated(
            itemCount: conversations.length,
            separatorBuilder: (_, __) => const Divider(height: 1),
            itemBuilder: (context, index) {
              final conversation = conversations[index];
              return ListTile(
                leading: CircleAvatar(
                  child: Text(conversation.counterpartRole == 'EXPERT' ? 'BS' : 'M'),
                ),
                title: Text(
                  conversation.counterpartRole == 'EXPERT' ? 'Chuyên gia' : 'Mẹ',
                ),
                subtitle: conversation.expertAvailable
                    ? null
                    : const Text(
                        'Chuyên gia hiện không khả dụng',
                        style: TextStyle(color: Colors.orange),
                      ),
                onTap: () => context.push('/direct-chat/${conversation.conversationId}'),
              );
            },
          );
        },
      ),
    );
  }
}
