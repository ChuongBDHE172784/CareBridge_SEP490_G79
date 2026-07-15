import 'dart:async';
import 'package:flutter/material.dart';
import 'package:uuid/uuid.dart';
import '../../../core/auth/auth_state.dart';
import '../../../integrations/firebaseRealtime/conversation_signaling_port.dart';
import '../../../integrations/firebaseRealtime/firebase_conversation_signaling_port.dart';
import '../models/timeline_item.dart';
import '../services/direct_chat_service.dart';

class DirectChatScreen extends StatefulWidget {
  final String conversationId;

  const DirectChatScreen({super.key, required this.conversationId});

  @override
  State<DirectChatScreen> createState() => _DirectChatScreenState();
}

class _DirectChatScreenState extends State<DirectChatScreen>
    with WidgetsBindingObserver {
  static const _uuid = Uuid();

  List<TimelineItem> _items = const [];
  final TextEditingController _textController = TextEditingController();
  final ScrollController _scrollController = ScrollController();

  bool _loading = true;
  bool _sending = false;
  bool _loadingOlder = false;
  bool _syncingNewer = false;
  bool _expertAvailable = true;
  String? _nextCursor;
  String? _previousCursor;
  bool _hasMoreOlder = false;

  ConversationSignalingPort? _signalingPort;
  StreamSubscription? _signalSubscription;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _loadInitial();
    _connectSignaling();
  }

  Future<void> _loadInitial() async {
    try {
      final conversation = await DirectChatService.instance.getConversation(
        widget.conversationId,
      );
      final page = await DirectChatService.instance.getTimeline(
        widget.conversationId,
      );
      if (!mounted) return;
      setState(() {
        _items = page.items;
        _nextCursor = page.nextCursor;
        _previousCursor = page.previousCursor;
        _hasMoreOlder = page.hasMoreOlder;
        _expertAvailable = conversation.expertAvailable;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() => _loading = false);
      _showError('Không thể tải cuộc trò chuyện: $e');
    }
  }

  Future<void> _connectSignaling() async {
    final port = FirebaseConversationSignalingPort();
    _signalingPort = port;
    try {
      await port.connect();
      _signalSubscription = port.events.listen((signal) {
        if (signal.conversationId == widget.conversationId) {
          _syncNewer();
        }
      });
    } catch (_) {
      // Firebase unavailable/misconfigured — degrade gracefully. Chat delivery is never
      // gated on Firebase (ADR-DCC-002/BR-DCC-007): reconnect-on-resume + pull-to-refresh
      // still reconcile via REST regardless of realtime signal availability.
    }
  }

  /// Reconnect reconcile — DCC-TC-014/025: fetch every item strictly newer than the last
  /// cursor we hold, merge without duplication.
  Future<void> _syncNewer() async {
    if (_nextCursor == null || _syncingNewer) return;
    _syncingNewer = true;
    try {
      var cursor = _nextCursor;
      do {
        final page = await DirectChatService.instance.getTimeline(
          widget.conversationId,
          after: cursor,
        );
        if (!mounted) return;
        if (page.items.isNotEmpty) {
          setState(() {
            _items = mergeTimelineItems(_items, page.items);
            _nextCursor = page.nextCursor;
          });
        }
        final next = page.nextCursor;
        if (!page.hasMoreNewer || next == null || next == cursor) break;
        cursor = next;
      } while (mounted);
    } catch (_) {
      // best-effort background sync — surfaced errors would be noisy; next resume/pull retries.
    } finally {
      _syncingNewer = false;
    }
  }

  Future<void> _loadOlder() async {
    if (_loadingOlder || !_hasMoreOlder || _previousCursor == null) return;
    setState(() => _loadingOlder = true);
    try {
      final page = await DirectChatService.instance.getTimeline(
        widget.conversationId,
        before: _previousCursor,
      );
      if (!mounted) return;
      setState(() {
        _items = mergeTimelineItems(_items, page.items);
        _previousCursor = page.previousCursor;
        _hasMoreOlder = page.hasMoreOlder;
      });
    } catch (e) {
      if (mounted) _showError('Không thể tải thêm lịch sử: $e');
    } finally {
      if (mounted) setState(() => _loadingOlder = false);
    }
  }

  Future<void> _send() async {
    final body = _textController.text.trim();
    if (body.isEmpty || _sending || !_expertAvailable) return;
    final clientMessageId = _uuid.v4();
    final currentUserId = AuthState.instance.userId ?? '';
    final optimistic = TimelineItem.optimisticMessage(
      clientMessageId: clientMessageId,
      senderUserId: currentUserId,
      messageBody: body,
    );
    setState(() {
      _items = mergeTimelineItems(_items, [optimistic]);
      _textController.clear();
      _sending = true;
    });
    await _sendWithClientId(clientMessageId, body);
  }

  Future<void> _sendWithClientId(String clientMessageId, String body) async {
    try {
      final confirmed = await DirectChatService.instance.sendMessage(
        widget.conversationId,
        clientMessageId: clientMessageId,
        messageBody: body,
      );
      if (!mounted) return;
      setState(() => _items = mergeTimelineItems(_items, [confirmed]));
    } catch (e) {
      if (!mounted) return;
      setState(() {
        final index = _items.indexWhere(
          (item) => item.clientMessageId == clientMessageId,
        );
        if (index != -1) {
          _items = List.of(_items)
            ..[index] = _items[index].copyWith(
              sendStatus: ChatSendStatus.failed,
            );
        }
      });
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  Future<void> _retry(TimelineItem failedItem) async {
    if (failedItem.clientMessageId == null || failedItem.messageBody == null)
      return;
    setState(() {
      final index = _items.indexWhere(
        (i) => i.clientMessageId == failedItem.clientMessageId,
      );
      if (index != -1) {
        _items = List.of(
          _items,
        )..[index] = _items[index].copyWith(sendStatus: ChatSendStatus.sending);
      }
      _sending = true;
    });
    // Same clientMessageId — idempotent retry (BR-DCC-005), server never creates a duplicate.
    await _sendWithClientId(
      failedItem.clientMessageId!,
      failedItem.messageBody!,
    );
  }

  Future<void> _placeCall(String callType) async {
    try {
      await DirectChatService.instance.initiateCall(
        widget.conversationId,
        callType: callType,
      );
      if (!mounted) return;
      // Approved scope (TDS §1.1, CB-CHAT-IMP-144D v1.2): call record + Firebase signaling
      // only — no live audio/video in this pass. Be explicit with the user, don't imply a
      // real call is connecting.
      showDialog<void>(
        context: context,
        builder: (context) => AlertDialog(
          title: const Text('Đã tạo yêu cầu gọi'),
          content: const Text(
            'Cuộc gọi đã được ghi nhận và gửi tín hiệu tới người nhận. '
            'Tính năng đàm thoại trực tiếp (âm thanh/hình ảnh) chưa được hỗ trợ trong bản này.',
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('Đã hiểu'),
            ),
          ],
        ),
      );
    } catch (e) {
      _showError('Không thể tạo cuộc gọi: $e');
    }
  }

  void _showError(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _syncNewer();
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _signalSubscription?.cancel();
    _signalingPort?.dispose();
    _textController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final currentUserId = AuthState.instance.userId;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Trò chuyện'),
        actions: [
          IconButton(
            icon: const Icon(Icons.call),
            onPressed: _expertAvailable ? () => _placeCall('VOICE') : null,
          ),
          IconButton(
            icon: const Icon(Icons.videocam),
            onPressed: _expertAvailable ? () => _placeCall('VIDEO') : null,
          ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : Column(
              children: [
                if (!_expertAvailable)
                  Container(
                    width: double.infinity,
                    color: Colors.orange.shade100,
                    padding: const EdgeInsets.all(12),
                    child: const Text(
                      'Chuyên gia hiện không khả dụng. Bạn vẫn có thể xem lại lịch sử trò chuyện.',
                      style: TextStyle(color: Colors.deepOrange),
                    ),
                  ),
                Expanded(
                  child: NotificationListener<ScrollNotification>(
                    onNotification: (notification) {
                      if (notification.metrics.pixels <= 40 &&
                          _hasMoreOlder &&
                          !_loadingOlder) {
                        _loadOlder();
                      }
                      return false;
                    },
                    child: ListView.builder(
                      controller: _scrollController,
                      padding: const EdgeInsets.all(12),
                      itemCount: _items.length + (_loadingOlder ? 1 : 0),
                      itemBuilder: (context, index) {
                        if (_loadingOlder && index == 0) {
                          return const Padding(
                            padding: EdgeInsets.symmetric(vertical: 8),
                            child: Center(
                              child: CircularProgressIndicator(strokeWidth: 2),
                            ),
                          );
                        }
                        final item = _items[index - (_loadingOlder ? 1 : 0)];
                        return _TimelineTile(
                          item: item,
                          isOwnMessage:
                              item.senderUserId != null &&
                              item.senderUserId == currentUserId,
                          onRetry: () => _retry(item),
                        );
                      },
                    ),
                  ),
                ),
                if (_expertAvailable) _buildInputRow(),
              ],
            ),
    );
  }

  Widget _buildInputRow() {
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.all(8),
        child: Row(
          children: [
            Expanded(
              child: TextField(
                controller: _textController,
                minLines: 1,
                maxLines: 4,
                decoration: const InputDecoration(
                  hintText: 'Nhập tin nhắn...',
                  border: OutlineInputBorder(),
                ),
                onSubmitted: (_) => _send(),
              ),
            ),
            const SizedBox(width: 8),
            IconButton(
              icon: const Icon(Icons.send),
              onPressed: _sending ? null : _send,
            ),
          ],
        ),
      ),
    );
  }
}

class _TimelineTile extends StatelessWidget {
  final TimelineItem item;
  final bool isOwnMessage;
  final VoidCallback onRetry;

  const _TimelineTile({
    required this.item,
    required this.isOwnMessage,
    required this.onRetry,
  });

  @override
  Widget build(BuildContext context) {
    if (item.kind == 'CALL_EVENT') {
      return _CallEventTile(item: item);
    }
    final failed = item.sendStatus == ChatSendStatus.failed;
    final sending = item.sendStatus == ChatSendStatus.sending;
    return Align(
      alignment: isOwnMessage ? Alignment.centerRight : Alignment.centerLeft,
      child: Column(
        crossAxisAlignment: isOwnMessage
            ? CrossAxisAlignment.end
            : CrossAxisAlignment.start,
        children: [
          Container(
            margin: const EdgeInsets.symmetric(vertical: 4),
            padding: const EdgeInsets.all(10),
            constraints: BoxConstraints(
              maxWidth: MediaQuery.of(context).size.width * 0.75,
            ),
            decoration: BoxDecoration(
              color: isOwnMessage ? Colors.blue.shade100 : Colors.grey.shade200,
              borderRadius: BorderRadius.circular(10),
            ),
            child: Text(item.messageBody ?? ''),
          ),
          if (failed)
            TextButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh, size: 14),
              label: const Text('Gửi lại', style: TextStyle(fontSize: 12)),
            )
          else if (sending)
            const Padding(
              padding: EdgeInsets.only(bottom: 6),
              child: Text(
                'Đang gửi...',
                style: TextStyle(fontSize: 11, color: Colors.grey),
              ),
            ),
        ],
      ),
    );
  }
}

class _CallEventTile extends StatelessWidget {
  final TimelineItem item;

  const _CallEventTile({required this.item});

  String _describe() {
    final kindLabel = item.callType == 'VIDEO'
        ? 'Cuộc gọi video'
        : 'Cuộc gọi thoại';
    switch (item.callStatus) {
      case 'ENDED':
        return '$kindLabel — ${item.durationSeconds ?? 0}s';
      case 'MISSED':
        return '$kindLabel nhỡ';
      case 'DECLINED':
        return '$kindLabel bị từ chối';
      case 'CANCELLED':
        return '$kindLabel đã huỷ';
      default:
        return kindLabel;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Container(
        margin: const EdgeInsets.symmetric(vertical: 8),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: BoxDecoration(
          color: Colors.grey.shade100,
          borderRadius: BorderRadius.circular(16),
        ),
        child: Text(
          _describe(),
          style: const TextStyle(fontSize: 12, color: Colors.grey),
        ),
      ),
    );
  }
}
