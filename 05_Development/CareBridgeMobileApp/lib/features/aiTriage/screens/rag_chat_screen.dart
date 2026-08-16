import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:http/http.dart' as http;
import 'package:universal_io/io.dart';
import '../../../core/network/api_client.dart';
import '../../../core/auth/auth_state.dart';

class _Message {
  final String text;
  final bool isUser;
  final DateTime time;
  final List<String> sources;
  final List<String> followups;
  final bool isWarning;

  _Message({
    required this.text,
    required this.isUser,
    required this.time,
    this.sources = const [],
    this.followups = const [],
    this.isWarning = false,
  });

  Map<String, dynamic> toJson() => {
        'text': text,
        'isUser': isUser,
        'time': time.toIso8601String(),
        'sources': sources,
        'followups': followups,
        'isWarning': isWarning,
      };

  factory _Message.fromJson(Map<String, dynamic> json) => _Message(
        text: json['text'] as String? ?? '',
        isUser: json['isUser'] as bool? ?? false,
        time: DateTime.tryParse(json['time'] as String? ?? '') ?? DateTime.now(),
        sources: (json['sources'] as List<dynamic>?)
                ?.map((e) => e.toString())
                .toList() ??
            [],
        followups: (json['followups'] as List<dynamic>?)
                ?.map((e) => e.toString())
                .toList() ??
            [],
        isWarning: json['isWarning'] as bool? ?? false,
      );
}

class _ChatSession {
  final String id;
  final String title;
  final DateTime updatedAt;
  final List<_Message> messages;

  _ChatSession({
    required this.id,
    required this.title,
    required this.updatedAt,
    required this.messages,
  });

  Map<String, dynamic> toJson() => {
        'id': id,
        'title': title,
        'updatedAt': updatedAt.toIso8601String(),
        'messages': messages.map((m) => m.toJson()).toList(),
      };

  factory _ChatSession.fromJson(Map<String, dynamic> json) => _ChatSession(
        id: json['id'] as String? ?? '',
        title: json['title'] as String? ?? 'Cuộc trò chuyện',
        updatedAt: DateTime.tryParse(json['updatedAt'] as String? ?? '') ??
            DateTime.now(),
        messages: (json['messages'] as List<dynamic>?)
                ?.map((e) => _Message.fromJson(e as Map<String, dynamic>))
                .toList() ??
            [],
      );
}

class RagChatScreen extends StatefulWidget {
  const RagChatScreen({super.key});

  @override
  State<RagChatScreen> createState() => _RagChatScreenState();
}

class _RagChatScreenState extends State<RagChatScreen> {
  static const _primary = Color(0xFFC98C7B);
  static const _primaryDark = Color(0xFFA86F60);
  static const _surface = Color(0xFFFDFBF9);
  static const _bg = Color(0xFFF6F1EC);
  static const _storage = FlutterSecureStorage();

  final _inputCtrl = TextEditingController();
  final _scrollCtrl = ScrollController();
  final List<_Message> _messages = [];
  List<_ChatSession> _sessions = [];
  String _currentSessionId = '';
  bool _sending = false;
  bool _loadingHistory = true;

  final List<String> _quickPrompts = [
    'Cách tính tuần thai và ngày dự sinh chuẩn xác?',
    'Mang thai 3 tháng đầu cần bổ sung vi chất gì?',
    'Dấu hiệu cảnh báo nguy hiểm trong thai kỳ cần đi viện ngay?',
    'Cách đếm và theo dõi cử động thai máy tại nhà?',
    'Hướng dẫn chăm sóc vết mổ và gọi sữa mẹ về sau sinh?',
  ];

  @override
  void initState() {
    super.initState();
    _currentSessionId = DateTime.now().millisecondsSinceEpoch.toString();
    _loadHistoryFromStorage();
  }

  @override
  void dispose() {
    _inputCtrl.dispose();
    _scrollCtrl.dispose();
    super.dispose();
  }

  String get _storageKey {
    final uid = AuthState.instance.userId ?? 'anonymous';
    return 'carebridge_ai_rag_sessions_$uid';
  }

  Future<void> _loadHistoryFromStorage() async {
    try {
      final raw = await _storage.read(key: _storageKey);
      if (raw != null && raw.isNotEmpty) {
        final decoded = jsonDecode(raw) as List<dynamic>;
        setState(() {
          _sessions = decoded
              .map((e) => _ChatSession.fromJson(e as Map<String, dynamic>))
              .toList();
        });
      }
    } catch (_) {}
    if (mounted) {
      setState(() => _loadingHistory = false);
    }
  }

  Future<void> _saveSessionsToStorage() async {
    try {
      final encoded = jsonEncode(_sessions.map((s) => s.toJson()).toList());
      await _storage.write(key: _storageKey, value: encoded);
    } catch (_) {}
  }

  void _persistCurrentSession() {
    if (_messages.isEmpty) return;
    final firstUserMsg =
        _messages.firstWhere((m) => m.isUser, orElse: () => _messages.first);
    final title = firstUserMsg.text.length > 40
        ? '${firstUserMsg.text.substring(0, 37)}...'
        : firstUserMsg.text;

    final existingIndex =
        _sessions.indexWhere((s) => s.id == _currentSessionId);
    final session = _ChatSession(
      id: _currentSessionId,
      title: title,
      updatedAt: DateTime.now(),
      messages: List.from(_messages),
    );

    if (existingIndex >= 0) {
      _sessions[existingIndex] = session;
    } else {
      _sessions.insert(0, session);
    }
    _saveSessionsToStorage();
  }

  void _startNewChat() {
    if (_messages.isNotEmpty) {
      _persistCurrentSession();
    }
    setState(() {
      _currentSessionId = DateTime.now().millisecondsSinceEpoch.toString();
      _messages.clear();
    });
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Đã tạo cuộc trò chuyện mới'),
        duration: Duration(seconds: 2),
        behavior: SnackBarBehavior.floating,
      ),
    );
  }

  void _loadSession(_ChatSession session) {
    if (_messages.isNotEmpty) {
      _persistCurrentSession();
    }
    setState(() {
      _currentSessionId = session.id;
      _messages.clear();
      _messages.addAll(session.messages);
    });
    Navigator.of(context).pop();
    _scrollToBottom();
  }

  void _deleteSession(String sessionId) {
    setState(() {
      _sessions.removeWhere((s) => s.id == sessionId);
      if (_currentSessionId == sessionId) {
        _currentSessionId = DateTime.now().millisecondsSinceEpoch.toString();
        _messages.clear();
      }
    });
    _saveSessionsToStorage();
  }

  void _clearAllSessions() {
    setState(() {
      _sessions.clear();
      _currentSessionId = DateTime.now().millisecondsSinceEpoch.toString();
      _messages.clear();
    });
    _saveSessionsToStorage();
    Navigator.of(context).pop();
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollCtrl.hasClients) {
        _scrollCtrl.animateTo(
          _scrollCtrl.position.maxScrollExtent,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  List<String> get _pythonCandidates {
    if (kIsWeb) return ['http://127.0.0.1:8001'];
    if (Platform.isAndroid) {
      return ['http://10.0.2.2:8001', 'http://127.0.0.1:8001'];
    }
    return ['http://127.0.0.1:8001', 'http://localhost:8001'];
  }

  Future<void> _send([String? customPrompt]) async {
    final question = (customPrompt ?? _inputCtrl.text).trim();
    if (question.isEmpty || _sending) return;

    setState(() {
      _messages.add(
        _Message(text: question, isUser: true, time: DateTime.now()),
      );
      _sending = true;
    });
    if (customPrompt == null) {
      _inputCtrl.clear();
    }
    _scrollToBottom();

    String answerText = '';
    List<String> sourcesList = [];
    List<String> followupsList = [];
    bool isWarning = false;

    // 1. Prioritize direct Python FastAPI AI RAG Service (PGVector + Gemini + Citations)
    final historyPayload = _messages
        .take(_messages.length - 1)
        .map((m) => {
              'role': m.isUser ? 'user' : 'assistant',
              'content': m.text,
            })
        .toList();

    for (final base in _pythonCandidates) {
      try {
        final response = await http
            .post(
              Uri.parse('$base/api/v1/chat/message'),
              headers: {
                'Content-Type': 'application/json',
                'X-Internal-API-Key': 'carebridge',
              },
              body: jsonEncode({
                'message': question,
                'stage': 'ALL',
                'conversation_history': historyPayload,
              }),
            )
            .timeout(const Duration(seconds: 15));

        if (response.statusCode == 200) {
          final decoded = jsonDecode(utf8.decode(response.bodyBytes));
          answerText = decoded['answer']?.toString() ?? '';
          isWarning = decoded['has_critical_warning'] == true;

          if (decoded['sources'] is List) {
            for (final s in decoded['sources']) {
              if (s is Map && s['title'] != null) {
                final title = s['title'].toString();
                final sec = s['section']?.toString();
                sourcesList.add(sec != null ? '$title ($sec)' : title);
              }
            }
          }
          if (decoded['suggested_followups'] is List) {
            followupsList = (decoded['suggested_followups'] as List)
                .map((e) => e.toString())
                .toList();
          }
          if (answerText.isNotEmpty) {
            break;
          }
        }
      } catch (_) {}
    }

    // 2. Fallback to Spring Boot /api/v1/rag/answer if Python service was unreachable
    if (answerText.isEmpty) {
      try {
        final data = await apiPost('/api/v1/rag/answer', {
          'query': question,
        });

        final resData =
            (data is Map && data.containsKey('data')) ? data['data'] : data;

        if (resData is Map) {
          answerText = resData['answer']?.toString() ?? '';
          if (resData['sources'] is List) {
            for (final s in resData['sources']) {
              if (s is Map && s['title'] != null) {
                sourcesList.add(s['title'].toString());
              }
            }
          }
        } else if (resData is String) {
          answerText = resData;
        }
      } catch (_) {}
    }

    if (!mounted) return;

    setState(() {
      _messages.add(
        _Message(
          text: answerText.isNotEmpty
              ? answerText
              : 'Chào bạn, hiện tại hệ thống AI đang kết nối lại với cơ sở dữ liệu y tế. Bạn vui lòng thử lại sau giây lát hoặc liên hệ trực tiếp Bác sĩ/Cơ sở y tế nếu cần hỗ trợ khẩn cấp nhé!',
          isUser: false,
          time: DateTime.now(),
          sources: sourcesList.take(3).toList(),
          followups: followupsList.take(3).toList(),
          isWarning: isWarning,
        ),
      );
      _sending = false;
    });

    _persistCurrentSession();
    _scrollToBottom();
  }

  String _formatTime(DateTime dt) {
    final h = dt.hour.toString().padLeft(2, '0');
    final m = dt.minute.toString().padLeft(2, '0');
    return '$h:$m';
  }

  void _openHistoryModal() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => _HistoryBottomSheet(
        sessions: _sessions,
        currentSessionId: _currentSessionId,
        onSelectSession: _loadSession,
        onDeleteSession: _deleteSession,
        onClearAll: _clearAllSessions,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final role = AuthState.instance.role ?? 'MOTHER';
    final isMother = role.toUpperCase() == 'MOTHER';

    return Scaffold(
      backgroundColor: _bg,
      appBar: AppBar(
        backgroundColor: _primary,
        foregroundColor: Colors.white,
        elevation: 1,
        titleSpacing: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new, size: 20),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(6),
              decoration: const BoxDecoration(
                color: Colors.white24,
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.favorite, size: 18, color: Colors.white),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'CareBridge AI Nurse',
                    style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                  ),
                  Row(
                    children: [
                      Container(
                        width: 7,
                        height: 7,
                        decoration: const BoxDecoration(
                          color: Color(0xFF66BB6A),
                          shape: BoxShape.circle,
                        ),
                      ),
                      const SizedBox(width: 5),
                      Text(
                        isMother
                            ? 'Đồng hành cùng Mẹ bầu • 24/7'
                            : 'Hỗ trợ Gia đình chăm sóc mẹ & bé',
                        style: const TextStyle(
                            fontSize: 11, color: Colors.white70),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.add_comment_outlined),
            tooltip: 'Cuộc trò chuyện mới',
            onPressed: _startNewChat,
          ),
          IconButton(
            icon: const Icon(Icons.history_rounded),
            tooltip: 'Lịch sử trò chuyện',
            onPressed: _openHistoryModal,
          ),
          IconButton(
            icon: const Icon(Icons.info_outline),
            tooltip: 'Lưu ý y tế',
            onPressed: () => showDialog(
              context: context,
              builder: (_) => AlertDialog(
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16)),
                title: const Row(
                  children: [
                    Icon(Icons.shield_outlined, color: _primary),
                    SizedBox(width: 8),
                    Text('Về Trợ lý AI CareBridge',
                        style: TextStyle(fontSize: 16)),
                  ],
                ),
                content: const Text(
                  'CareBridge AI Nurse là trợ lý hỗ trợ tra cứu cẩm nang y tế từ Bộ Y Tế & WHO.\n\nThông tin chỉ mang tính chất tham khảo, không thay thế cho chẩn đoán hoặc chỉ định trực tiếp từ Bác sĩ chuyên khoa.',
                  style: TextStyle(fontSize: 13, height: 1.4),
                ),
                actions: [
                  TextButton(
                    onPressed: () => Navigator.of(context).pop(),
                    child: const Text('Đã hiểu',
                        style: TextStyle(color: _primary)),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
      body: Column(
        children: [
          if (_messages.isEmpty)
            Expanded(
              child: _WelcomeView(
                quickPrompts: _quickPrompts,
                onPromptTap: (p) => _send(p),
              ),
            ),
          if (_messages.isNotEmpty)
            Expanded(
              child: ListView.builder(
                controller: _scrollCtrl,
                padding: const EdgeInsets.symmetric(
                  horizontal: 14,
                  vertical: 16,
                ),
                itemCount: _messages.length + (_sending ? 1 : 0),
                itemBuilder: (ctx, i) {
                  if (i == _messages.length) {
                    return const _TypingIndicator();
                  }
                  return _MessageBubble(
                    message: _messages[i],
                    formatTime: _formatTime,
                    onFollowupTap: (p) => _send(p),
                  );
                },
              ),
            ),
          _InputBar(
            controller: _inputCtrl,
            sending: _sending,
            onSend: () => _send(),
          ),
        ],
      ),
    );
  }
}

class _HistoryBottomSheet extends StatelessWidget {
  final List<_ChatSession> sessions;
  final String currentSessionId;
  final ValueChanged<_ChatSession> onSelectSession;
  final ValueChanged<String> onDeleteSession;
  final VoidCallback onClearAll;

  const _HistoryBottomSheet({
    required this.sessions,
    required this.currentSessionId,
    required this.onSelectSession,
    required this.onDeleteSession,
    required this.onClearAll,
  });

  String _formatSessionDate(DateTime dt) {
    final now = DateTime.now();
    if (dt.day == now.day && dt.month == now.month && dt.year == now.year) {
      final h = dt.hour.toString().padLeft(2, '0');
      final m = dt.minute.toString().padLeft(2, '0');
      return 'Hôm nay lúc $h:$m';
    }
    return '${dt.day}/${dt.month}/${dt.year}';
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      constraints: BoxConstraints(
        maxHeight: MediaQuery.of(context).size.height * 0.75,
      ),
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      child: Column(
        children: [
          Container(
            margin: const EdgeInsets.symmetric(vertical: 10),
            width: 40,
            height: 4,
            decoration: BoxDecoration(
              color: Colors.grey.shade300,
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 8),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Row(
                  children: [
                    Icon(Icons.history_rounded,
                        color: Color(0xFFC98C7B), size: 22),
                    SizedBox(width: 8),
                    Text(
                      'Lịch sử trò chuyện AI',
                      style: TextStyle(
                        fontSize: 17,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFF5A463F),
                      ),
                    ),
                  ],
                ),
                if (sessions.isNotEmpty)
                  TextButton.icon(
                    icon: const Icon(Icons.delete_sweep,
                        size: 18, color: Colors.redAccent),
                    label: const Text('Xóa tất cả',
                        style: TextStyle(fontSize: 12, color: Colors.redAccent)),
                    onPressed: () {
                      showDialog(
                        context: context,
                        builder: (_) => AlertDialog(
                          title: const Text('Xóa tất cả lịch sử?'),
                          content: const Text(
                              'Toàn bộ các đoạn hội thoại AI đã lưu sẽ bị xóa vĩnh viễn.'),
                          actions: [
                            TextButton(
                              onPressed: () => Navigator.of(context).pop(),
                              child: const Text('Hủy'),
                            ),
                            TextButton(
                              onPressed: () {
                                Navigator.of(context).pop();
                                onClearAll();
                              },
                              child: const Text('Xóa hết',
                                  style: TextStyle(color: Colors.red)),
                            ),
                          ],
                        ),
                      );
                    },
                  ),
              ],
            ),
          ),
          const Divider(height: 1),
          if (sessions.isEmpty)
            Expanded(
              child: Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.chat_bubble_outline,
                        size: 48, color: Colors.grey.shade400),
                    const SizedBox(height: 12),
                    Text(
                      'Chưa có lịch sử trò chuyện nào',
                      style: TextStyle(
                          color: Colors.grey.shade600, fontSize: 14),
                    ),
                  ],
                ),
              ),
            ),
          if (sessions.isNotEmpty)
            Expanded(
              child: ListView.separated(
                padding: const EdgeInsets.symmetric(vertical: 8),
                itemCount: sessions.length,
                separatorBuilder: (_, __) =>
                    Divider(height: 1, indent: 64, color: Colors.grey.shade200),
                itemBuilder: (ctx, i) {
                  final s = sessions[i];
                  final isCurrent = s.id == currentSessionId;
                  return ListTile(
                    leading: CircleAvatar(
                      backgroundColor: isCurrent
                          ? const Color(0xFFC98C7B)
                          : const Color(0xFFF2EAE4),
                      child: Icon(
                        Icons.chat_outlined,
                        size: 18,
                        color: isCurrent
                            ? Colors.white
                            : const Color(0xFFC98C7B),
                      ),
                    ),
                    title: Text(
                      s.title,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight:
                            isCurrent ? FontWeight.bold : FontWeight.w500,
                        color: const Color(0xFF2D2421),
                      ),
                    ),
                    subtitle: Text(
                      '${_formatSessionDate(s.updatedAt)} • ${s.messages.length} tin nhắn',
                      style:
                          TextStyle(fontSize: 11, color: Colors.grey.shade600),
                    ),
                    trailing: IconButton(
                      icon: const Icon(Icons.close, size: 18, color: Colors.grey),
                      tooltip: 'Xóa đoạn chat này',
                      onPressed: () => onDeleteSession(s.id),
                    ),
                    onTap: () => onSelectSession(s),
                  );
                },
              ),
            ),
        ],
      ),
    );
  }
}

class _WelcomeView extends StatelessWidget {
  final List<String> quickPrompts;
  final ValueChanged<String> onPromptTap;

  const _WelcomeView({
    required this.quickPrompts,
    required this.onPromptTap,
  });

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(20, 24, 20, 16),
      child: Column(
        children: [
          Container(
            width: 72,
            height: 72,
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: [
                  const Color(0xFFC98C7B).withValues(alpha: 0.2),
                  const Color(0xFFC98C7B).withValues(alpha: 0.05),
                ],
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
              shape: BoxShape.circle,
            ),
            child: const Icon(
              Icons.health_and_safety,
              size: 38,
              color: Color(0xFFC98C7B),
            ),
          ),
          const SizedBox(height: 16),
          const Text(
            'Trợ lý AI CareBridge',
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: Color(0xFF5A463F),
            ),
          ),
          const SizedBox(height: 8),
          const Text(
            'Giải đáp 24/7 mọi thắc mắc về cẩm nang thai kỳ, dinh dưỡng, cách tính tuần thai và chăm sóc bé theo chuẩn Bộ Y Tế & WHO.',
            textAlign: TextAlign.center,
            style: TextStyle(color: Colors.black54, fontSize: 13, height: 1.4),
          ),
          const SizedBox(height: 24),
          Align(
            alignment: Alignment.centerLeft,
            child: Text(
              'GỢI Ý CÂU HỎI THƯỜNG GẶP:',
              style: TextStyle(
                fontSize: 11,
                fontWeight: FontWeight.bold,
                letterSpacing: 0.5,
                color: Colors.grey.shade600,
              ),
            ),
          ),
          const SizedBox(height: 12),
          ...quickPrompts.map(
            (prompt) => Container(
              width: double.infinity,
              margin: const EdgeInsets.only(bottom: 8),
              child: OutlinedButton(
                style: OutlinedButton.styleFrom(
                  backgroundColor: Colors.white,
                  foregroundColor: const Color(0xFF5A463F),
                  alignment: Alignment.centerLeft,
                  padding:
                      const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                  side: BorderSide(color: Colors.grey.shade300),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
                onPressed: () => onPromptTap(prompt),
                child: Row(
                  children: [
                    const Icon(Icons.chat_bubble_outline,
                        size: 16, color: Color(0xFFC98C7B)),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        prompt,
                        style: const TextStyle(
                            fontSize: 13, fontWeight: FontWeight.w500),
                      ),
                    ),
                    const Icon(Icons.chevron_right,
                        size: 18, color: Colors.grey),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _MessageBubble extends StatelessWidget {
  final _Message message;
  final String Function(DateTime) formatTime;
  final ValueChanged<String> onFollowupTap;

  const _MessageBubble({
    required this.message,
    required this.formatTime,
    required this.onFollowupTap,
  });

  Widget _buildFormattedContent(String raw, bool isUser) {
    final lines = raw.split('\n');
    final children = <Widget>[];
    final numberRegex = RegExp(r'^(\d+[\.\)])\s*(.*)');

    for (final line in lines) {
      final trimmed = line.trim();
      if (trimmed.isEmpty) {
        children.add(const SizedBox(height: 6));
        continue;
      }

      // Divider: --- or *** or ___
      if (trimmed == '---' ||
          trimmed == '***' ||
          trimmed == '___' ||
          trimmed == '- - -' ||
          trimmed == '* * *') {
        children.add(
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 6),
            child: Divider(height: 1, thickness: 1, color: Color(0xFFE8DFD8)),
          ),
        );
        continue;
      }

      // Headers: #, ##, ###, ####
      if (trimmed.startsWith('#### ')) {
        children.add(
          Padding(
            padding: const EdgeInsets.only(top: 8, bottom: 4),
            child: Text(
              trimmed.substring(5).replaceAll('**', '').replaceAll('***', ''),
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.bold,
                color: isUser ? Colors.white : const Color(0xFF5A463F),
              ),
            ),
          ),
        );
        continue;
      }
      if (trimmed.startsWith('### ')) {
        children.add(
          Padding(
            padding: const EdgeInsets.only(top: 10, bottom: 4),
            child: Text(
              trimmed.substring(4).replaceAll('**', '').replaceAll('***', ''),
              style: TextStyle(
                fontSize: 14.5,
                fontWeight: FontWeight.bold,
                color: isUser ? Colors.white : const Color(0xFF4A3731),
              ),
            ),
          ),
        );
        continue;
      }
      if (trimmed.startsWith('## ')) {
        children.add(
          Padding(
            padding: const EdgeInsets.only(top: 12, bottom: 6),
            child: Text(
              trimmed.substring(3).replaceAll('**', '').replaceAll('***', ''),
              style: TextStyle(
                fontSize: 15.5,
                fontWeight: FontWeight.bold,
                color: isUser ? Colors.white : const Color(0xFF3B2A25),
              ),
            ),
          ),
        );
        continue;
      }
      if (trimmed.startsWith('# ')) {
        children.add(
          Padding(
            padding: const EdgeInsets.only(top: 12, bottom: 6),
            child: Text(
              trimmed.substring(2).replaceAll('**', '').replaceAll('***', ''),
              style: TextStyle(
                fontSize: 16.5,
                fontWeight: FontWeight.bold,
                color: isUser ? Colors.white : const Color(0xFF3B2A25),
              ),
            ),
          ),
        );
        continue;
      }

      // Bullet points: * , - , + , •
      if (trimmed.startsWith('* ') ||
          trimmed.startsWith('- ') ||
          trimmed.startsWith('+ ') ||
          trimmed.startsWith('• ')) {
        final content = trimmed.substring(2);
        children.add(
          Padding(
            padding: const EdgeInsets.only(left: 4, bottom: 4),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '• ',
                  style: TextStyle(
                    color: isUser ? Colors.white : const Color(0xFFC98C7B),
                    fontWeight: FontWeight.bold,
                    fontSize: 14,
                  ),
                ),
                Expanded(
                  child: _buildInlineSpans(content, isUser),
                ),
              ],
            ),
          ),
        );
        continue;
      }

      // Numbered list: 1. , 2. , 1) ...
      final numMatch = numberRegex.firstMatch(trimmed);
      if (numMatch != null) {
        final numberPrefix = numMatch.group(1)!;
        final content = numMatch.group(2)!;
        children.add(
          Padding(
            padding: const EdgeInsets.only(left: 4, bottom: 4),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '$numberPrefix ',
                  style: TextStyle(
                    color: isUser ? Colors.white : const Color(0xFFC98C7B),
                    fontWeight: FontWeight.bold,
                    fontSize: 13.5,
                  ),
                ),
                Expanded(
                  child: _buildInlineSpans(content, isUser),
                ),
              ],
            ),
          ),
        );
        continue;
      }

      // Blockquotes: >
      if (trimmed.startsWith('> ')) {
        children.add(
          Container(
            margin: const EdgeInsets.symmetric(vertical: 4),
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
            decoration: BoxDecoration(
              color: isUser ? Colors.white12 : const Color(0xFFF7F2EE),
              borderRadius: BorderRadius.circular(6),
              border: Border(
                left: BorderSide(
                  color: isUser ? Colors.white70 : const Color(0xFFC98C7B),
                  width: 3,
                ),
              ),
            ),
            child: _buildInlineSpans(trimmed.substring(2), isUser),
          ),
        );
        continue;
      }

      // Normal paragraph line
      children.add(
        Padding(
          padding: const EdgeInsets.only(bottom: 3),
          child: _buildInlineSpans(line, isUser),
        ),
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: children,
    );
  }

  Widget _buildInlineSpans(String text, bool isUser) {
    final spans = <TextSpan>[];
    final regex = RegExp(
        r'(\*\*\*([^*]+)\*\*\*|\*\*([^*]+)\*\*|\*([^*]+)\*|`([^`]+)`|([^*`]+))');
    final matches = regex.allMatches(text);

    if (matches.isEmpty) {
      spans.add(TextSpan(
        text: text,
        style: TextStyle(
          color: isUser ? Colors.white : const Color(0xFF2D2421),
          fontSize: 13.5,
          height: 1.45,
        ),
      ));
    } else {
      for (final m in matches) {
        if (m.group(2) != null) {
          // ***bold italic***
          spans.add(TextSpan(
            text: m.group(2),
            style: TextStyle(
              fontWeight: FontWeight.bold,
              fontStyle: FontStyle.italic,
              color: isUser ? Colors.white : const Color(0xFF2D2421),
              fontSize: 13.5,
            ),
          ));
        } else if (m.group(3) != null) {
          // **bold**
          spans.add(TextSpan(
            text: m.group(3),
            style: TextStyle(
              fontWeight: FontWeight.bold,
              color: isUser ? Colors.white : const Color(0xFF2D2421),
              fontSize: 13.5,
            ),
          ));
        } else if (m.group(4) != null) {
          // *italic*
          spans.add(TextSpan(
            text: m.group(4),
            style: TextStyle(
              fontStyle: FontStyle.italic,
              color: isUser ? Colors.white70 : const Color(0xFF5A463F),
              fontSize: 13.5,
            ),
          ));
        } else if (m.group(5) != null) {
          // `code`
          spans.add(TextSpan(
            text: m.group(5),
            style: TextStyle(
              fontFamily: 'monospace',
              backgroundColor:
                  isUser ? Colors.white24 : const Color(0xFFF0EAE5),
              color: isUser ? Colors.white : const Color(0xFFC98C7B),
              fontSize: 12.5,
            ),
          ));
        } else if (m.group(6) != null) {
          // normal text
          spans.add(TextSpan(
            text: m.group(6),
            style: TextStyle(
              color: isUser ? Colors.white : const Color(0xFF2D2421),
              fontSize: 13.5,
              height: 1.45,
            ),
          ));
        }
      }
    }

    return RichText(
      text: TextSpan(children: spans),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isUser = message.isUser;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Column(
        crossAxisAlignment:
            isUser ? CrossAxisAlignment.end : CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment:
                isUser ? MainAxisAlignment.end : MainAxisAlignment.start,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (!isUser) ...[
                CircleAvatar(
                  radius: 16,
                  backgroundColor: const Color(0xFFC98C7B),
                  child: const Icon(
                    Icons.support_agent,
                    size: 18,
                    color: Colors.white,
                  ),
                ),
                const SizedBox(width: 8),
              ],
              Flexible(
                child: Container(
                  constraints: BoxConstraints(
                    maxWidth: MediaQuery.of(context).size.width * 0.82,
                  ),
                  padding:
                      const EdgeInsets.symmetric(horizontal: 15, vertical: 12),
                  decoration: BoxDecoration(
                    color: isUser ? const Color(0xFFC98C7B) : Colors.white,
                    borderRadius: BorderRadius.only(
                      topLeft: const Radius.circular(18),
                      topRight: const Radius.circular(18),
                      bottomLeft: Radius.circular(isUser ? 18 : 4),
                      bottomRight: Radius.circular(isUser ? 4 : 18),
                    ),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withValues(alpha: 0.05),
                        blurRadius: 6,
                        offset: const Offset(0, 2),
                      ),
                    ],
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      _buildFormattedContent(message.text, isUser),
                      if (!isUser && message.sources.isNotEmpty) ...[
                        const SizedBox(height: 10),
                        Container(
                          padding: const EdgeInsets.all(8),
                          decoration: BoxDecoration(
                            color: const Color(0xFFF9F6F3),
                            borderRadius: BorderRadius.circular(8),
                            border: Border.all(color: const Color(0xFFE8DFD8)),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Row(
                                children: [
                                  Icon(Icons.menu_book,
                                      size: 13, color: Color(0xFFC98C7B)),
                                  SizedBox(width: 4),
                                  Text(
                                    'Nguồn cẩm nang tham khảo:',
                                    style: TextStyle(
                                      fontSize: 11,
                                      fontWeight: FontWeight.bold,
                                      color: Color(0xFF7A5C52),
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 4),
                              ...message.sources.map(
                                (s) => Padding(
                                  padding: const EdgeInsets.only(top: 2),
                                  child: Text(
                                    '• $s',
                                    style: const TextStyle(
                                      fontSize: 11,
                                      color: Color(0xFF5A463F),
                                    ),
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                      if (!isUser) ...[
                        const SizedBox(height: 8),
                        Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 10, vertical: 6),
                          decoration: BoxDecoration(
                            color: const Color(0xFFFFF3E0),
                            borderRadius: BorderRadius.circular(8),
                            border: Border.all(color: const Color(0xFFFFCC80)),
                          ),
                          child: const Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Icon(Icons.info_outline,
                                  size: 14, color: Color(0xFFE65100)),
                              SizedBox(width: 6),
                              Expanded(
                                child: Text(
                                  'Lưu ý: Thông tin từ AI chỉ mang tính chất tham khảo và có thể có sai sót. Vui lòng tham khảo ý kiến bác sĩ hoặc đến ngay cơ sở y tế khi có dấu hiệu bất thường.',
                                  style: TextStyle(
                                    fontSize: 10.5,
                                    color: Color(0xFFBF360C),
                                    height: 1.3,
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                      const SizedBox(height: 6),
                      Align(
                        alignment: Alignment.bottomRight,
                        child: Text(
                          formatTime(message.time),
                          style: TextStyle(
                            color: isUser ? Colors.white70 : Colors.grey,
                            fontSize: 10,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              if (isUser) const SizedBox(width: 8),
            ],
          ),
          if (!isUser && message.followups.isNotEmpty) ...[
            const SizedBox(height: 8),
            Padding(
              padding: const EdgeInsets.only(left: 40),
              child: Wrap(
                spacing: 6,
                runSpacing: 6,
                children: message.followups
                    .map(
                      (f) => ActionChip(
                        label: Text(f, style: const TextStyle(fontSize: 12)),
                        backgroundColor: Colors.white,
                        side: const BorderSide(color: Color(0xFFDECFC8)),
                        shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(16)),
                        onPressed: () => onFollowupTap(f),
                      ),
                    )
                    .toList(),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _TypingIndicator extends StatelessWidget {
  const _TypingIndicator();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        children: [
          const CircleAvatar(
            radius: 16,
            backgroundColor: Color(0xFFC98C7B),
            child: Icon(Icons.support_agent, size: 18, color: Colors.white),
          ),
          const SizedBox(width: 8),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(16),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.05),
                  blurRadius: 4,
                ),
              ],
            ),
            child: const Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                SizedBox(
                  width: 14,
                  height: 14,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: Color(0xFFC98C7B),
                  ),
                ),
                SizedBox(width: 8),
                Text(
                  'AI Nurse đang tra cứu cẩm nang...',
                  style: TextStyle(fontSize: 12, color: Colors.grey),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _InputBar extends StatelessWidget {
  final TextEditingController controller;
  final bool sending;
  final VoidCallback onSend;

  const _InputBar({
    required this.controller,
    required this.sending,
    required this.onSend,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(14, 8, 14, 16),
      decoration: BoxDecoration(
        color: Colors.white,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.06),
            blurRadius: 8,
            offset: const Offset(0, -2),
          ),
        ],
      ),
      child: SafeArea(
        top: false,
        child: Row(
          children: [
            Expanded(
              child: TextField(
                controller: controller,
                enabled: !sending,
                maxLines: 4,
                minLines: 1,
                textInputAction: TextInputAction.send,
                onSubmitted: (_) => onSend(),
                decoration: InputDecoration(
                  hintText: 'Hỏi AI Nurse về thai kỳ, dinh dưỡng, bé...',
                  hintStyle: const TextStyle(fontSize: 13, color: Colors.grey),
                  filled: true,
                  fillColor: const Color(0xFFF6F1EC),
                  contentPadding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 11,
                  ),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(24),
                    borderSide: BorderSide.none,
                  ),
                ),
              ),
            ),
            const SizedBox(width: 8),
            GestureDetector(
              onTap: sending ? null : onSend,
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 200),
                width: 44,
                height: 44,
                decoration: BoxDecoration(
                  color: sending
                      ? Colors.grey.shade400
                      : const Color(0xFFC98C7B),
                  shape: BoxShape.circle,
                ),
                child: sending
                    ? const Padding(
                        padding: EdgeInsets.all(12),
                        child: CircularProgressIndicator(
                          color: Colors.white,
                          strokeWidth: 2,
                        ),
                      )
                    : const Icon(Icons.arrow_upward,
                        color: Colors.white, size: 22),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
