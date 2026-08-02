import 'dart:async';
import 'dart:typed_data';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:file_picker/file_picker.dart';
import 'package:file_saver/file_saver.dart';
import 'package:image_gallery_saver_plus/image_gallery_saver_plus.dart';
import 'package:image_picker/image_picker.dart';
import 'package:mime/mime.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:http/http.dart' as http;
import 'package:uuid/uuid.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:universal_io/io.dart';
import '../../../core/auth/auth_state.dart';
import '../../../core/network/api_client.dart';
import 'direct_chat_attachment_viewer_screen.dart';
import '../calls/conversation_signal_hub.dart';
import '../calls/direct_call_host.dart';
import '../models/timeline_item.dart';
import '../services/direct_chat_service.dart';
import '../services/conversation_refresh_bus.dart';

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
  bool _initialLoadComplete = false;
  bool _syncingNewer = false;
  bool _pendingNewerSync = false;
  bool _expertAvailable = true;
  String? _nextCursor;
  String? _previousCursor;
  bool _hasMoreOlder = false;

  StreamSubscription? _signalSubscription;
  Timer? _markReadRetry;
  String? _scheduledReadMessageId;
  String? _lastMarkedReadMessageId;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _loadInitial();
    _signalSubscription = ConversationSignalHub.instance.events.listen((
      signal,
    ) {
      if (signal.conversationId == widget.conversationId) {
        _syncNewer();
      }
    });
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
      _scheduleMarkReadIfNeeded();
    } catch (e) {
      if (!mounted) return;
      setState(() => _loading = false);
      _showError('Không thể tải cuộc trò chuyện: $e');
    } finally {
      if (mounted) {
        _initialLoadComplete = true;
        if (_pendingNewerSync) {
          _pendingNewerSync = false;
          scheduleMicrotask(_syncNewer);
        }
      }
    }
  }

  /// Reconnect reconcile — DCC-TC-014/025: fetch every item strictly newer than the last
  /// cursor we hold, merge without duplication.
  Future<void> _syncNewer() async {
    if (!_initialLoadComplete || _syncingNewer) {
      _pendingNewerSync = true;
      return;
    }
    _syncingNewer = true;
    try {
      if (_nextCursor == null) {
        final page = await DirectChatService.instance.getTimeline(
          widget.conversationId,
        );
        if (!mounted) return;
        setState(() {
          _items = mergeTimelineItems(_items, page.items);
          _nextCursor = page.nextCursor;
          _previousCursor = page.previousCursor;
          _hasMoreOlder = page.hasMoreOlder;
        });
        _scheduleMarkReadIfNeeded();
        return;
      }
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
          _scheduleMarkReadIfNeeded();
        }
        final next = page.nextCursor;
        if (!page.hasMoreNewer || next == null || next == cursor) break;
        cursor = next;
      } while (mounted);
    } catch (_) {
      // best-effort background sync — surfaced errors would be noisy; next resume/pull retries.
    } finally {
      _syncingNewer = false;
      if (_pendingNewerSync && mounted) {
        _pendingNewerSync = false;
        scheduleMicrotask(_syncNewer);
      }
    }
  }

  /// TDS §13.6 — lastSeenMessageId is the newest MESSAGE item actually rendered on the
  /// client, never a server-side "latest" guess. No-op if the timeline has no MESSAGE item
  /// yet (call-only or empty) or the newest one is still an unconfirmed optimistic send.
  void _scheduleMarkReadIfNeeded() {
    TimelineItem? latestMessage;
    for (final item in _items.reversed) {
      if (item.kind == 'MESSAGE' && item.messageId != null) {
        latestMessage = item;
        break;
      }
    }
    if (latestMessage == null) return;
    final messageId = latestMessage.messageId!;
    if (messageId == _lastMarkedReadMessageId ||
        messageId == _scheduledReadMessageId) {
      return;
    }
    _scheduledReadMessageId = messageId;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted || _scheduledReadMessageId != messageId) return;
      if (ModalRoute.of(context)?.isCurrent != true) {
        _scheduledReadMessageId = null;
        return;
      }
      _performMarkRead(messageId, attempt: 0);
    });
  }

  Future<void> _performMarkRead(
    String messageId, {
    required int attempt,
  }) async {
    try {
      await DirectChatService.instance.markRead(
        widget.conversationId,
        messageId,
      );
      if (!mounted || _scheduledReadMessageId != messageId) return;
      _lastMarkedReadMessageId = messageId;
      _scheduledReadMessageId = null;
      ConversationRefreshBus.notify();
    } catch (_) {
      if (!mounted || _scheduledReadMessageId != messageId) return;
      if (attempt >= 2) {
        _scheduledReadMessageId = null;
        return;
      }
      _markReadRetry?.cancel();
      _markReadRetry = Timer(Duration(seconds: 1 << attempt), () {
        if (mounted && ModalRoute.of(context)?.isCurrent == true) {
          _performMarkRead(messageId, attempt: attempt + 1);
        } else {
          _scheduledReadMessageId = null;
        }
      });
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

  Future<void> _attachImage(ImageSource source) async {
    if (_sending || !_expertAvailable) return;
    try {
      final image = await ImagePicker().pickImage(
        source: source,
        imageQuality: 85,
      );
      if (image == null) return;
      final bytes = await image.readAsBytes();
      if (bytes.length > 10 * 1024 * 1024) {
        throw const FormatException('Ảnh phải nhỏ hơn 10 MB');
      }
      setState(() => _sending = true);
      final uploaded = await apiMultipart(
        '/api/v1/direct-conversations/${widget.conversationId}/attachments?kind=IMAGE',
        const {},
        files: [
          MultipartUploadFile(
            fieldName: 'file',
            bytes: bytes,
            fileName: image.name,
            mimeType: image.mimeType ?? 'image/jpeg',
          ),
        ],
      );
      final fileId = uploaded?['data']?['fileId'] as String?;
      if (fileId == null) throw const FormatException('Không thể tải ảnh lên');
      await DirectChatService.instance.sendMessage(
        widget.conversationId,
        clientMessageId: _uuid.v4(),
        messageType: 'IMAGE',
        attachmentId: fileId,
      );
      await _syncNewer();
    } catch (e) {
      if (mounted) _showError('Không thể gửi ảnh: $e');
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  Future<void> _attachDocument() async {
    if (_sending || !_expertAvailable) return;
    try {
      final picked = await FilePicker.platform.pickFiles(withData: true);
      final file = picked?.files.single;
      if (file == null || file.bytes == null) return;
      if (file.bytes!.length > 20 * 1024 * 1024) {
        throw const FormatException('Tài liệu phải nhỏ hơn 20 MB');
      }
      setState(() => _sending = true);
      final uploaded = await apiMultipart(
        '/api/v1/direct-conversations/${widget.conversationId}/attachments?kind=DOCUMENT',
        const {},
        files: [
          MultipartUploadFile(
            fieldName: 'file',
            bytes: file.bytes!,
            fileName: file.name,
            mimeType:
                lookupMimeType(file.name, headerBytes: file.bytes) ??
                'application/octet-stream',
          ),
        ],
      );
      final fileId = uploaded?['data']?['fileId'] as String?;
      if (fileId == null)
        throw const FormatException('Không thể tải tài liệu lên');
      await DirectChatService.instance.sendMessage(
        widget.conversationId,
        clientMessageId: _uuid.v4(),
        messageType: 'FILE',
        attachmentId: fileId,
      );
      await _syncNewer();
    } catch (e) {
      if (mounted) _showError('Không thể gửi tài liệu: $e');
    } finally {
      if (mounted) setState(() => _sending = false);
    }
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
    if (failedItem.clientMessageId == null || failedItem.messageBody == null) {
      return;
    }
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

  Future<void> _recall(TimelineItem item) async {
    if (item.messageId == null) return;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Thu hồi tin nhắn'),
        content: const Text(
          'Tin nhắn và tệp đính kèm sẽ không còn hiển thị cho người nhận.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Hủy'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Thu hồi'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    try {
      await DirectChatService.instance.recallMessage(
        widget.conversationId,
        item.messageId!,
      );
      await _syncNewer();
    } catch (e) {
      if (mounted) _showError('Không thể thu hồi tin nhắn: $e');
    }
  }

  Future<void> _placeCall(String callType) async {
    try {
      await DirectCallScope.of(
        context,
      ).initiate(widget.conversationId, callType);
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
    _markReadRetry?.cancel();
    _textController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  static const _primary = Color(0xFFC98C7B);
  static const _primaryDark = Color(0xFF845143);
  static const _surface = Color(0xFFF6F1EC);
  static const _surfaceLow = Color(0xFFF2EAE4);
  static const _onSurface = Color(0xFF271812);
  static const _onVariant = Color(0xFF524440);
  static const _outline = Color(0xFFD6C2BD);

  @override
  Widget build(BuildContext context) {
    final currentUserId = AuthState.instance.userId;
    return Scaffold(
      backgroundColor: _surface,
      appBar: AppBar(
        backgroundColor: _primaryDark,
        elevation: 0,
        foregroundColor: Colors.white,
        centerTitle: true,
        title: const Text(
          'Trò chuyện Trực tiếp',
          style: TextStyle(fontSize: 17, fontWeight: FontWeight.w700),
        ),
        actions: [
          IconButton(
            tooltip: 'Gọi thoại',
            icon: const Icon(Icons.phone_rounded),
            onPressed: _expertAvailable ? () => _placeCall('VOICE') : null,
          ),
          IconButton(
            tooltip: 'Gọi video',
            icon: const Icon(Icons.videocam_rounded),
            onPressed: _expertAvailable ? () => _placeCall('VIDEO') : null,
          ),
          const SizedBox(width: 4),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator(color: _primary))
          : Column(
              children: [
                if (!_expertAvailable)
                  Container(
                    width: double.infinity,
                    color: const Color(0xFFFEF3C7),
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                    child: Row(
                      children: [
                        const Icon(Icons.info_outline, color: Color(0xFFD97706), size: 20),
                        const SizedBox(width: 10),
                        const Expanded(
                          child: Text(
                            'Chuyên gia hiện không khả dụng. Bạn vẫn có thể xem lại lịch sử trò chuyện.',
                            style: TextStyle(color: Color(0xFF92400E), fontSize: 13, height: 1.3),
                          ),
                        ),
                      ],
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
                      padding: const EdgeInsets.fromLTRB(16, 16, 16, 16),
                      itemCount: _items.length + (_loadingOlder ? 1 : 0),
                      itemBuilder: (context, index) {
                        if (_loadingOlder && index == 0) {
                          return const Padding(
                            padding: EdgeInsets.symmetric(vertical: 8),
                            child: Center(
                              child: CircularProgressIndicator(strokeWidth: 2, color: _primary),
                            ),
                          );
                        }
                        final item = _items[index - (_loadingOlder ? 1 : 0)];
                        return _TimelineTile(
                          item: item,
                          conversationId: widget.conversationId,
                          isOwnMessage:
                              item.senderUserId != null &&
                              item.senderUserId == currentUserId,
                          onRetry: () => _retry(item),
                          onRecall: () => _recall(item),
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
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
          boxShadow: [
            BoxShadow(
              color: Color(0x105A463F),
              blurRadius: 16,
              offset: Offset(0, -4),
            ),
          ],
        ),
        child: Row(
          children: [
            Container(
              decoration: BoxDecoration(
                color: _surfaceLow,
                shape: BoxShape.circle,
              ),
              child: IconButton(
                icon: const Icon(Icons.add_photo_alternate_rounded, color: _primaryDark),
                onPressed: _sending
                    ? null
                    : () => showModalBottomSheet<void>(
                        context: context,
                        backgroundColor: Colors.white,
                        shape: const RoundedRectangleBorder(
                          borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
                        ),
                        builder: (sheetContext) => SafeArea(
                          child: Wrap(
                            children: [
                              Padding(
                                padding: const EdgeInsets.all(16),
                                child: Text(
                                  'Tệp đính kèm',
                                  style: TextStyle(
                                    fontSize: 16,
                                    fontWeight: FontWeight.bold,
                                    color: _primaryDark,
                                  ),
                                ),
                              ),
                              ListTile(
                                leading: const Icon(Icons.photo_library_outlined, color: _primary),
                                title: const Text('Chọn từ thư viện'),
                                onTap: () {
                                  Navigator.pop(sheetContext);
                                  _attachImage(ImageSource.gallery);
                                },
                              ),
                              ListTile(
                                leading: const Icon(Icons.attach_file_rounded, color: _primary),
                                title: const Text('Chọn tài liệu'),
                                onTap: () {
                                  Navigator.pop(sheetContext);
                                  _attachDocument();
                                },
                              ),
                              ListTile(
                                leading: const Icon(Icons.camera_alt_outlined, color: _primary),
                                title: const Text('Chụp ảnh'),
                                onTap: () {
                                  Navigator.pop(sheetContext);
                                  _attachImage(ImageSource.camera);
                                },
                              ),
                              const SizedBox(height: 12),
                            ],
                          ),
                        ),
                      ),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: Container(
                decoration: BoxDecoration(
                  color: _surface,
                  borderRadius: BorderRadius.circular(24),
                  border: Border.all(color: _outline.withValues(alpha: 0.6)),
                ),
                child: TextField(
                  controller: _textController,
                  minLines: 1,
                  maxLines: 4,
                  style: const TextStyle(color: _onSurface, fontSize: 14),
                  decoration: const InputDecoration(
                    hintText: 'Nhập tin nhắn...',
                    hintStyle: TextStyle(color: _onVariant, fontSize: 14),
                    contentPadding: EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                    border: InputBorder.none,
                  ),
                  onSubmitted: (_) => _send(),
                ),
              ),
            ),
            const SizedBox(width: 8),
            Container(
              decoration: const BoxDecoration(
                color: _primaryDark,
                shape: BoxShape.circle,
              ),
              child: IconButton(
                icon: const Icon(Icons.send_rounded, color: Colors.white, size: 20),
                onPressed: _sending ? null : _send,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _TimelineTile extends StatelessWidget {
  static const _primary = Color(0xFFC98C7B);
  static const _surfaceLow = Color(0xFFF2EAE4);
  static const _onSurface = Color(0xFF271812);
  static const _onVariant = Color(0xFF524440);
  static const _outline = Color(0xFFD6C2BD);

  final TimelineItem item;
  final String conversationId;
  final bool isOwnMessage;
  final VoidCallback onRetry;
  final VoidCallback onRecall;

  const _TimelineTile({
    required this.item,
    required this.conversationId,
    required this.isOwnMessage,
    required this.onRetry,
    required this.onRecall,
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
          InkWell(
            onLongPress:
                item.messageType == 'FILE' &&
                    item.messageId != null &&
                    item.recalledAt == null
                ? () => _showFileActions(context)
                : isOwnMessage &&
                      item.messageId != null &&
                      item.recalledAt == null &&
                      item.messageType != 'IMAGE'
                ? onRecall
                : null,
            onTap:
                item.messageType != 'FILE' ||
                    item.attachmentId == null ||
                    item.recalledAt != null
                ? null
                : () => Navigator.of(context).push(
                    MaterialPageRoute(
                      builder: (_) => DirectChatAttachmentViewerScreen(
                        conversationId: conversationId,
                        messageId: item.messageId!,
                      ),
                    ),
                  ),
            borderRadius: BorderRadius.circular(16),
            child: Container(
              margin: const EdgeInsets.symmetric(vertical: 4),
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 11),
              constraints: BoxConstraints(
                maxWidth: MediaQuery.of(context).size.width * 0.75,
              ),
              decoration: BoxDecoration(
                color: isOwnMessage ? _primary : Colors.white,
                border: isOwnMessage
                    ? null
                    : Border.all(color: _outline.withValues(alpha: 0.6)),
                borderRadius: BorderRadius.only(
                  topLeft: const Radius.circular(18),
                  topRight: const Radius.circular(18),
                  bottomLeft: Radius.circular(isOwnMessage ? 18 : 4),
                  bottomRight: Radius.circular(isOwnMessage ? 4 : 18),
                ),
                boxShadow: const [
                  BoxShadow(
                    color: Color(0x0A5A463F),
                    blurRadius: 8,
                    offset: Offset(0, 3),
                  ),
                ],
              ),
              child: item.recalledAt != null
                  ? Text(
                      'Tin nhắn đã được thu hồi',
                      style: TextStyle(
                        fontStyle: FontStyle.italic,
                        color: isOwnMessage ? Colors.white70 : _onVariant,
                        fontSize: 13,
                      ),
                    )
                  : item.messageType == 'IMAGE' && item.messageId != null
                  ? _InlineChatImage(
                      conversationId: conversationId,
                      messageId: item.messageId!,
                      canRecall: isOwnMessage,
                      onRecall: onRecall,
                    )
                  : item.messageType == 'FILE'
                  ? Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(
                          Icons.insert_drive_file_rounded,
                          color: isOwnMessage ? Colors.white : _primary,
                        ),
                        const SizedBox(width: 8),
                        Text(
                          'Tài liệu',
                          style: TextStyle(
                            color: isOwnMessage ? Colors.white : _onSurface,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ],
                    )
                  : Text(
                      item.messageBody ?? '',
                      style: TextStyle(
                        color: isOwnMessage ? Colors.white : _onSurface,
                        fontSize: 14,
                        height: 1.4,
                      ),
                    ),
            ),
          ),
          if (!failed && !sending)
            Padding(
              padding: const EdgeInsets.only(bottom: 6, left: 4, right: 4),
              child: Text(
                _formatTimestamp(item.createdAt),
                style: const TextStyle(fontSize: 11, color: _onVariant),
              ),
            ),
          if (failed)
            TextButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh, size: 14, color: Colors.red),
              label: const Text('Gửi lại', style: TextStyle(fontSize: 12, color: Colors.red)),
            )
          else if (sending)
            const Padding(
              padding: EdgeInsets.only(bottom: 6),
              child: Text(
                'Đang gửi...',
                style: TextStyle(fontSize: 11, color: _onVariant),
              ),
            ),
        ],
      ),
    );
  }

  String _formatTimestamp(DateTime? value) {
    if (value == null) return '';
    final local = value.toLocal();
    final now = DateTime.now();
    final isToday =
        local.year == now.year &&
        local.month == now.month &&
        local.day == now.day;
    final time =
        '${local.hour.toString().padLeft(2, '0')}:${local.minute.toString().padLeft(2, '0')}';
    return isToday
        ? time
        : '${local.day.toString().padLeft(2, '0')}/${local.month.toString().padLeft(2, '0')} · $time';
  }

  Future<void> _showFileActions(BuildContext context) async {
    try {
      final json = await apiGet(
        '/api/v1/direct-conversations/$conversationId/messages/${item.messageId}/attachment',
      );
      final data = json['data'] as Map<String, dynamic>?;
      final url = data?['presignedUrl'] as String?;
      final name = data?['originalName'] as String? ?? 'carebridge_document';
      final mime = data?['mimeType'] as String? ?? 'application/octet-stream';
      if (url == null || url.isEmpty)
        throw const FormatException('Missing file URL');
      if (!context.mounted) return;
      showModalBottomSheet<void>(
        context: context,
        builder: (sheetContext) => SafeArea(
          child: Wrap(
            children: [
              ListTile(
                leading: const Icon(Icons.download_outlined),
                title: const Text('Tải tài liệu xuống máy'),
                onTap: () async {
                  Navigator.of(sheetContext).pop();
                  await _downloadFile(context, url, name, mime);
                },
              ),
              if (isOwnMessage)
                ListTile(
                  leading: const Icon(Icons.undo_outlined, color: Colors.red),
                  title: const Text(
                    'Thu hồi tài liệu',
                    style: TextStyle(color: Colors.red),
                  ),
                  onTap: () {
                    Navigator.of(sheetContext).pop();
                    onRecall();
                  },
                ),
            ],
          ),
        ),
      );
    } catch (_) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Không thể mở tài liệu. Vui lòng thử lại.'),
          ),
        );
      }
    }
  }

  Future<void> _downloadFile(
    BuildContext context,
    String url,
    String originalName,
    String mimeType,
  ) async {
    try {
      final separator = originalName.lastIndexOf('.');
      final name = separator > 0
          ? originalName.substring(0, separator)
          : originalName;
      final extension = separator > 0
          ? originalName.substring(separator + 1)
          : '';
      await FileSaver.instance.saveAs(
        name: name,
        link: LinkDetails(link: url),
        fileExtension: extension,
        mimeType: MimeType.custom,
        customMimeType: mimeType,
      );
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Đã tải tài liệu xuống máy')),
        );
      }
    } catch (_) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Không thể tải tài liệu. Vui lòng thử lại.'),
          ),
        );
      }
    }
  }
}

/// Resolves a short-lived, participant-authorized URL and renders the image in
/// the chat itself. Tapping it opens a fullscreen dialog, retaining chat state.
class _InlineChatImage extends StatefulWidget {
  const _InlineChatImage({
    required this.conversationId,
    required this.messageId,
    required this.canRecall,
    required this.onRecall,
  });

  final String conversationId;
  final String messageId;
  final bool canRecall;
  final VoidCallback onRecall;

  @override
  State<_InlineChatImage> createState() => _InlineChatImageState();
}

class _InlineChatImageState extends State<_InlineChatImage> {
  Future<String>? _url;

  @override
  void initState() {
    super.initState();
    _url = _loadUrl();
  }

  Future<String> _loadUrl() async {
    final json = await apiGet(
      '/api/v1/direct-conversations/${widget.conversationId}/messages/${widget.messageId}/attachment',
    );
    final url = json['data']?['presignedUrl'] as String?;
    if (url == null || url.isEmpty)
      throw const FormatException('Missing image URL');
    return url;
  }

  @override
  Widget build(BuildContext context) => FutureBuilder<String>(
    future: _url,
    builder: (context, snapshot) {
      if (snapshot.hasError) {
        return const SizedBox(
          width: 220,
          height: 96,
          child: Center(child: Icon(Icons.broken_image_outlined)),
        );
      }
      if (!snapshot.hasData) {
        return const SizedBox(
          width: 220,
          height: 156,
          child: Center(child: CircularProgressIndicator()),
        );
      }
      final url = snapshot.data!;
      return GestureDetector(
        onTap: () => _showFullscreen(context, url),
        onLongPress: () => _showActions(context, url),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(8),
          child: Image.network(
            url,
            width: 220,
            height: 168,
            fit: BoxFit.cover,
            errorBuilder: (_, __, ___) => const SizedBox(
              width: 220,
              height: 96,
              child: Center(child: Icon(Icons.broken_image_outlined)),
            ),
          ),
        ),
      );
    },
  );

  void _showFullscreen(BuildContext context, String url) {
    showDialog<void>(
      context: context,
      barrierColor: Colors.black87,
      builder: (dialogContext) => GestureDetector(
        onTap: () => Navigator.of(dialogContext).pop(),
        child: Scaffold(
          backgroundColor: Colors.transparent,
          body: SafeArea(
            child: Stack(
              children: [
                Center(
                  child: InteractiveViewer(
                    minScale: 0.8,
                    maxScale: 4,
                    child: Image.network(url, fit: BoxFit.contain),
                  ),
                ),
                Positioned(
                  top: 8,
                  right: 8,
                  child: IconButton.filledTonal(
                    icon: const Icon(Icons.close),
                    onPressed: () => Navigator.of(dialogContext).pop(),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  void _showActions(BuildContext context, String url) {
    showModalBottomSheet<void>(
      context: context,
      builder: (sheetContext) => SafeArea(
        child: Wrap(
          children: [
            ListTile(
              leading: const Icon(Icons.download_outlined),
              title: const Text('Tải ảnh xuống máy'),
              onTap: () async {
                Navigator.of(sheetContext).pop();
                await _download(context, url);
              },
            ),
            if (widget.canRecall)
              ListTile(
                leading: const Icon(Icons.undo_outlined, color: Colors.red),
                title: const Text(
                  'Thu hồi ảnh',
                  style: TextStyle(color: Colors.red),
                ),
                onTap: () {
                  Navigator.of(sheetContext).pop();
                  widget.onRecall();
                },
              ),
          ],
        ),
      ),
    );
  }

  Future<void> _download(BuildContext context, String url) async {
    try {
      if (kIsWeb) {
        final opened = await launchUrl(
          Uri.parse(url),
          mode: LaunchMode.externalApplication,
        );
        if (!opened) throw const FormatException('Cannot open download URL');
      } else {
        if (Platform.isIOS) {
          final status = await Permission.photosAddOnly.request();
          if (!status.isGranted && !status.isLimited) {
            throw const FormatException('Photo library permission denied');
          }
        }
        final response = await http.get(Uri.parse(url));
        if (response.statusCode < 200 || response.statusCode >= 300) {
          throw const FormatException('Image download failed');
        }
        final result = await ImageGallerySaverPlus.saveImage(
          Uint8List.fromList(response.bodyBytes),
          quality: 100,
          name: 'carebridge_chat_${widget.messageId}',
        );
        if (result['isSuccess'] != true) {
          throw const FormatException('Unable to save image');
        }
      }
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Đã lưu ảnh vào thiết bị')),
        );
      }
    } catch (_) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Không thể tải ảnh. Vui lòng thử lại.')),
        );
      }
    }
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
          '${_describe()} · ${_formatTimestamp(item.initiatedAt)}',
          style: const TextStyle(fontSize: 12, color: Colors.grey),
        ),
      ),
    );
  }

  String _formatTimestamp(DateTime? value) {
    if (value == null) return '';
    final local = value.toLocal();
    return '${local.hour.toString().padLeft(2, '0')}:${local.minute.toString().padLeft(2, '0')} · ${local.day.toString().padLeft(2, '0')}/${local.month.toString().padLeft(2, '0')}';
  }
}
