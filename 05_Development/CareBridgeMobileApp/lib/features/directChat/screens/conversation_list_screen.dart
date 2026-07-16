import 'dart:async';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../core/auth/auth_state.dart';
import '../../../integrations/firebaseRealtime/conversation_signaling_port.dart';
import '../../../integrations/firebaseRealtime/firebase_conversation_signaling_port.dart';
import '../models/direct_conversation.dart';
import '../services/direct_chat_service.dart';
import '../services/conversation_refresh_bus.dart';

/// Shared between MOTHER and EXPERT roles (TDS §13.5).
class ConversationListScreen extends StatefulWidget {
  const ConversationListScreen({super.key});

  @override
  State<ConversationListScreen> createState() => _ConversationListScreenState();
}

class _ConversationListScreenState extends State<ConversationListScreen>
    with WidgetsBindingObserver {
  List<DirectConversationSummary> _conversations = [];
  bool _loading = true;
  String? _error;

  ConversationSignalingPort? _signalingPort;
  StreamSubscription? _signalSubscription;
  StreamSubscription<void>? _refreshSubscription;
  int _loadGeneration = 0;

  bool get _isExpert => AuthState.instance.role == 'EXPERT';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _load();
    _connectSignaling();
    _refreshSubscription = ConversationRefreshBus.events.listen((_) => _load());
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _signalSubscription?.cancel();
    _refreshSubscription?.cancel();
    _signalingPort?.dispose();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) _load();
  }

  Future<void> _connectSignaling() async {
    final port = FirebaseConversationSignalingPort();
    _signalingPort = port;
    try {
      _signalSubscription = port.events.listen((_) {
        ConversationRefreshBus.notify();
      });
      await port.connect();
    } catch (_) {
      // Firebase unavailable/misconfigured — degrade gracefully, same as DirectChatScreen.
    }
  }

  Future<void> _load() async {
    if (!mounted) return;
    final generation = ++_loadGeneration;
    setState(() => _error = null);
    try {
      final conversations = await DirectChatService.instance
          .listMyConversations();
      if (!mounted || generation != _loadGeneration) return;
      setState(() {
        _conversations = conversations;
        _loading = false;
      });
    } catch (e) {
      if (!mounted || generation != _loadGeneration) return;
      setState(() {
        _error = 'Lỗi tải danh sách: $e';
        _loading = false;
      });
    }
  }

  Future<void> _openConversation(DirectConversationSummary conversation) async {
    await context.push('/direct-chat/${conversation.conversationId}');
    ConversationRefreshBus.notify();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Trò chuyện')),
      body: RefreshIndicator(onRefresh: _load, child: _buildBody()),
    );
  }

  Widget _buildBody() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_error != null) {
      return ListView(
        children: [
          const SizedBox(height: 120),
          Center(child: Text(_error!)),
        ],
      );
    }
    if (_conversations.isEmpty) {
      return ListView(
        children: [
          const SizedBox(height: 120),
          Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  _isExpert
                      ? 'Chưa có mẹ nào nhắn cho bạn — danh sách sẽ hiện khi có yêu cầu mới'
                      : 'Bạn chưa có cuộc trò chuyện nào',
                  textAlign: TextAlign.center,
                ),
                if (!_isExpert) ...[
                  const SizedBox(height: 12),
                  FilledButton(
                    onPressed: () => context.push('/experts'),
                    child: const Text('Tìm chuyên gia'),
                  ),
                ],
              ],
            ),
          ),
        ],
      );
    }
    return ListView.separated(
      itemCount: _conversations.length,
      separatorBuilder: (_, __) => const Divider(height: 1),
      itemBuilder: (context, index) {
        final conversation = _conversations[index];
        return _ConversationTile(
          conversation: conversation,
          isExpertViewer: _isExpert,
          onTap: () => _openConversation(conversation),
        );
      },
    );
  }
}

class _ConversationTile extends StatelessWidget {
  final DirectConversationSummary conversation;
  final bool isExpertViewer;
  final VoidCallback onTap;

  const _ConversationTile({
    required this.conversation,
    required this.isExpertViewer,
    required this.onTap,
  });

  String get _subtitle {
    // MOTHER viewing an EXPERT counterpart → show their specialty; EXPERT viewing a MOTHER
    // counterpart → no "safe context" field exists on the backend for that direction, fall
    // back to the last message preview (TDS §13.5 — deliberately not inventing a new field).
    if (!isExpertViewer && conversation.counterpartSpecialty != null) {
      return conversation.counterpartSpecialty!;
    }
    return conversation.lastMessagePreview ?? '';
  }

  String _relativeTime(DateTime? dt) {
    if (dt == null) return '';
    final rawDiff = DateTime.now().toUtc().difference(dt);
    final diff = rawDiff.isNegative ? Duration.zero : rawDiff;
    if (diff.inMinutes < 1) return 'Vừa xong';
    if (diff.inMinutes < 60) return '${diff.inMinutes} phút';
    if (diff.inHours < 24) return '${diff.inHours} giờ';
    if (diff.inDays < 7) return '${diff.inDays} ngày';
    return '${dt.day.toString().padLeft(2, '0')}/${dt.month.toString().padLeft(2, '0')}';
  }

  @override
  Widget build(BuildContext context) {
    final displayTime =
        conversation.lastMessageAt ?? conversation.lastActivityAt;
    return ListTile(
      leading: CircleAvatar(
        backgroundImage: conversation.counterpartAvatarUrl != null
            ? NetworkImage(conversation.counterpartAvatarUrl!)
            : null,
        child: conversation.counterpartAvatarUrl == null
            ? Text(
                (conversation.counterpartDisplayName?.isNotEmpty == true
                        ? conversation.counterpartDisplayName![0]
                        : (conversation.counterpartRole == 'EXPERT'
                              ? 'B'
                              : 'M'))
                    .toUpperCase(),
              )
            : null,
      ),
      title: Text(
        conversation.counterpartDisplayName ??
            (conversation.counterpartRole == 'EXPERT' ? 'Chuyên gia' : 'Mẹ'),
      ),
      subtitle: !isExpertViewer && !conversation.expertAvailable
          ? const Text(
              'Chuyên gia hiện không khả dụng',
              style: TextStyle(color: Colors.orange),
            )
          : Text(_subtitle, maxLines: 1, overflow: TextOverflow.ellipsis),
      trailing: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          Text(
            _relativeTime(displayTime),
            style: Theme.of(context).textTheme.bodySmall,
          ),
          if (conversation.unreadCount > 0) ...[
            const SizedBox(height: 4),
            CircleAvatar(
              radius: 10,
              backgroundColor: Theme.of(context).colorScheme.error,
              child: Text(
                conversation.unreadCount > 9
                    ? '9+'
                    : '${conversation.unreadCount}',
                style: const TextStyle(fontSize: 10, color: Colors.white),
              ),
            ),
          ],
        ],
      ),
      onTap: onTap,
    );
  }
}
