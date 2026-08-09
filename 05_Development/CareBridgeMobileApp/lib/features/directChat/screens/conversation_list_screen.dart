import 'dart:async';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../core/auth/auth_state.dart';
import '../calls/conversation_signal_hub.dart';
import '../models/direct_conversation.dart';
import '../services/direct_chat_service.dart';
import '../services/conversation_refresh_bus.dart';

/// Shared between MOTHER, FAMILY, and EXPERT roles (TDS §13.5).
class ConversationListScreen extends StatefulWidget {
  const ConversationListScreen({super.key});

  @override
  State<ConversationListScreen> createState() => _ConversationListScreenState();
}

class _ConversationListScreenState extends State<ConversationListScreen>
    with WidgetsBindingObserver {
  static const _primary = Color(0xFFC98C7B);
  static const _primaryDark = Color(0xFF845143);
  static const _surface = Color(0xFFF6F1EC);
  static const _onVariant = Color(0xFF524440);

  List<DirectConversationSummary> _conversations = [];
  bool _loading = true;
  String? _error;

  StreamSubscription? _signalSubscription;
  StreamSubscription<void>? _refreshSubscription;
  int _loadGeneration = 0;

  bool get _isExpert => AuthState.instance.role == 'EXPERT';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _load();
    _signalSubscription = ConversationSignalHub.instance.events.listen((_) {
      ConversationRefreshBus.notify();
    });
    _refreshSubscription = ConversationRefreshBus.events.listen((_) => _load());
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _signalSubscription?.cancel();
    _refreshSubscription?.cancel();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) _load();
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
      backgroundColor: _surface,
      appBar: AppBar(
        backgroundColor: _primaryDark,
        elevation: 0,
        foregroundColor: Colors.white,
        centerTitle: true,
        title: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text(
              'Trò chuyện Trực tiếp',
              style: TextStyle(fontSize: 17, fontWeight: FontWeight.w700),
            ),
            Text(
              _isExpert ? 'Kênh tư vấn & hỗ trợ' : 'Tư vấn Chuyên gia Y tế',
              style: const TextStyle(fontSize: 11, color: Colors.white70),
            ),
          ],
        ),
      ),
      body: RefreshIndicator(
        color: _primary,
        backgroundColor: Colors.white,
        onRefresh: _load,
        child: _buildBody(),
      ),
    );
  }

  Widget _buildBody() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator(color: _primary));
    }
    if (_error != null) {
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        children: [
          const SizedBox(height: 120),
          Center(
            child: Column(
              children: [
                const Icon(Icons.error_outline, size: 48, color: _primaryDark),
                const SizedBox(height: 12),
                Text(
                  _error!,
                  style: const TextStyle(color: _onVariant, fontSize: 14),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 16),
                FilledButton.icon(
                  onPressed: _load,
                  icon: const Icon(Icons.refresh),
                  label: const Text('Thử lại'),
                  style: FilledButton.styleFrom(backgroundColor: _primaryDark),
                ),
              ],
            ),
          ),
        ],
      );
    }
    if (_conversations.isEmpty) {
      return ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        children: [
          const SizedBox(height: 80),
          Center(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Container(
                    width: 76,
                    height: 76,
                    decoration: BoxDecoration(
                      color: _primary.withValues(alpha: 0.12),
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(
                      Icons.chat_bubble_outline_rounded,
                      size: 38,
                      color: _primary,
                    ),
                  ),
                  const SizedBox(height: 20),
                  Text(
                    _isExpert
                        ? 'Chưa có mẹ nào nhắn cho bạn'
                        : 'Bạn chưa có cuộc trò chuyện nào',
                    style: const TextStyle(
                      fontSize: 17,
                      fontWeight: FontWeight.w700,
                      color: _primaryDark,
                    ),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 8),
                  Text(
                    _isExpert
                        ? 'Danh sách cuộc trò chuyện sẽ xuất hiện ngay khi có mẹ hoặc gia đình liên hệ tư vấn.'
                        : 'Hãy kết nối ngay với bác sĩ và chuyên gia y tế đã xác thực để nhận lời khuyên an toàn.',
                    style: const TextStyle(
                      color: _onVariant,
                      fontSize: 13,
                      height: 1.4,
                    ),
                    textAlign: TextAlign.center,
                  ),
                  if (!_isExpert) ...[
                    const SizedBox(height: 24),
                    FilledButton.icon(
                      onPressed: () => context.push('/experts'),
                      icon: const Icon(Icons.search_rounded),
                      label: const Text('Tìm chuyên gia'),
                      style: FilledButton.styleFrom(
                        backgroundColor: _primaryDark,
                        padding: const EdgeInsets.symmetric(
                          horizontal: 24,
                          vertical: 12,
                        ),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(24),
                        ),
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ),
        ],
      );
    }
    return ListView.builder(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
      itemCount: _conversations.length,
      itemBuilder: (context, index) {
        final conversation = _conversations[index];
        return Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: _ConversationTile(
            conversation: conversation,
            isExpertViewer: _isExpert,
            onTap: () => _openConversation(conversation),
          ),
        );
      },
    );
  }
}

class _ConversationTile extends StatelessWidget {
  static const _primary = Color(0xFFC98C7B);
  static const _primaryDark = Color(0xFF845143);
  static const _surfaceLow = Color(0xFFF2EAE4);
  static const _onSurface = Color(0xFF271812);
  static const _onVariant = Color(0xFF524440);
  static const _outline = Color(0xFFD6C2BD);

  final DirectConversationSummary conversation;
  final bool isExpertViewer;
  final VoidCallback onTap;

  const _ConversationTile({
    required this.conversation,
    required this.isExpertViewer,
    required this.onTap,
  });

  String get _subtitle {
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
    if (diff.inMinutes < 60) return '${diff.inMinutes}m';
    if (diff.inHours < 24) return '${diff.inHours}h';
    if (diff.inDays < 7) return '${diff.inDays}d';
    return '${dt.day.toString().padLeft(2, '0')}/${dt.month.toString().padLeft(2, '0')}';
  }

  @override
  Widget build(BuildContext context) {
    final displayTime =
        conversation.lastMessageAt ?? conversation.lastActivityAt;
    final hasUnread = conversation.unreadCount > 0;

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(18),
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(18),
          border: Border.all(
            color: hasUnread
                ? _primary.withValues(alpha: 0.5)
                : _outline.withValues(alpha: 0.5),
            width: hasUnread ? 1.5 : 1,
          ),
          boxShadow: [
            BoxShadow(
              color: const Color(0x0C5A463F),
              blurRadius: 10,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Row(
          children: [
            Container(
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(
                  color: _primary.withValues(alpha: 0.4),
                  width: 2,
                ),
              ),
              child: CircleAvatar(
                radius: 26,
                backgroundColor: _surfaceLow,
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
                        style: const TextStyle(
                          color: _primaryDark,
                          fontWeight: FontWeight.w700,
                          fontSize: 18,
                        ),
                      )
                    : null,
              ),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Expanded(
                        child: Text(
                          conversation.counterpartDisplayName ??
                              (conversation.counterpartRole == 'EXPERT'
                                  ? 'Chuyên gia'
                                  : 'Mẹ'),
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: hasUnread
                                ? FontWeight.w700
                                : FontWeight.w600,
                            color: _onSurface,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      const SizedBox(width: 8),
                      Text(
                        _relativeTime(displayTime),
                        style: TextStyle(
                          fontSize: 12,
                          color: hasUnread
                              ? _primary
                              : _onVariant.withValues(alpha: 0.7),
                          fontWeight: hasUnread
                              ? FontWeight.w600
                              : FontWeight.normal,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Row(
                    children: [
                      Expanded(
                        child: !isExpertViewer && !conversation.expertAvailable
                            ? const Text(
                                'Chuyên gia hiện không khả dụng',
                                style: TextStyle(
                                  color: Color(0xFFD97706),
                                  fontSize: 13,
                                  fontWeight: FontWeight.w500,
                                ),
                              )
                            : Text(
                                _subtitle,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: TextStyle(
                                  fontSize: 13,
                                  color: hasUnread ? _onSurface : _onVariant,
                                  fontWeight: hasUnread
                                      ? FontWeight.w500
                                      : FontWeight.normal,
                                ),
                              ),
                      ),
                      if (hasUnread) ...[
                        const SizedBox(width: 8),
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 8,
                            vertical: 3,
                          ),
                          decoration: BoxDecoration(
                            color: _primary,
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Text(
                            conversation.unreadCount > 9
                                ? '9+'
                                : '${conversation.unreadCount}',
                            style: const TextStyle(
                              fontSize: 11,
                              fontWeight: FontWeight.bold,
                              color: Colors.white,
                            ),
                          ),
                        ),
                      ],
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
