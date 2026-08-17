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
    sources:
        (json['sources'] as List<dynamic>?)
            ?.map((e) => e.toString())
            .toList() ??
        [],
    followups:
        (json['followups'] as List<dynamic>?)
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
    updatedAt:
        DateTime.tryParse(json['updatedAt'] as String? ?? '') ?? DateTime.now(),
    messages:
        (json['messages'] as List<dynamic>?)
            ?.map((e) => _Message.fromJson(e as Map<String, dynamic>))
            .toList() ??
        [],
  );
}

class RagChatScreen extends StatefulWidget {
  final String? initialPrompt;
  final Map<String, dynamic>? attachedHealthContext;
  final bool autoSendInitialPrompt;

  const RagChatScreen({
    super.key,
    this.initialPrompt,
    this.attachedHealthContext,
    this.autoSendInitialPrompt = false,
  });

  @override
  State<RagChatScreen> createState() => _RagChatScreenState();
}

class _RagChatScreenState extends State<RagChatScreen> {
  static const _primary = Color(0xFFC98C7B);
  static const _primaryDark = Color(0xFFA86F60);
  static const _surface = Color(0xFFFDFBF9);
  static const _bg = Color(0xFFF6F1EC);
  static const _storage = FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );

  final _inputCtrl = TextEditingController();
  final _scrollCtrl = ScrollController();
  final List<_Message> _messages = [];
  List<_ChatSession> _sessions = [];
  String _currentSessionId = '';
  bool _sending = false;
  bool _loadingHistory = true;
  Map<String, dynamic>? _attachedContext;
  String? _journeyType;
  int? _pregnancyWeek;

  String get _effectiveStage {
    final jt = _journeyType ??
        _attachedContext?['journeyType'] as String? ??
        (_attachedContext?['stage'] == 'PRECONCEPTION'
            ? 'PRE_PREGNANCY'
            : (_attachedContext?['stage'] == 'POSTPARTUM'
                ? 'POSTPARTUM'
                : (_attachedContext?['gestationalAge'] != null ||
                        _attachedContext?['gestationalWeeks'] != null
                    ? 'PREGNANCY'
                    : null)));

    if (jt == 'PRE_PREGNANCY' || jt == 'PRECONCEPTION') {
      return 'PRECONCEPTION';
    }
    if (jt == 'POSTPARTUM' || jt == 'BABY_CARE') {
      return 'POSTPARTUM';
    }
    if (jt == 'PREGNANCY') {
      return 'PREGNANCY';
    }
    return 'ALL';
  }

  List<String> get _quickPrompts {
    final stage = _effectiveStage;

    if (stage == 'PRECONCEPTION') {
      return const [
        'Bổ sung axit folic & vi chất thế nào trước khi mang thai?',
        'Cách tính ngày rụng trứng & thời điểm vàng thụ thai?',
        'Các loại vắc-xin cần tiêm phòng trước khi mang thai?',
        'Những xét nghiệm tiền hôn nhân & sức khỏe sinh sản quan trọng?',
      ];
    }

    if (stage == 'POSTPARTUM') {
      return const [
        'Hướng dẫn chăm sóc vết mổ / vết khâu phục hồi sau sinh?',
        'Cách kích sữa về dồi dào và phòng ngừa tắc tia sữa?',
        'Dấu hiệu nhận biết trầm cảm sau sinh (EPDS) và cách cân bằng?',
        'Lịch tiêm chủng và theo dõi tăng trưởng chuẩn WHO cho bé?',
      ];
    }

    final weekText = _pregnancyWeek != null ? ' (Tuần $_pregnancyWeek)' : '';
    return [
      'Mang thai 3 tháng đầu cần bổ sung vi chất gì?',
      'Dấu hiệu cảnh báo nguy hiểm trong thai kỳ$weekText cần đi viện ngay?',
      'Cách đếm và theo dõi cử động thai máy tại nhà?',
      'Chế độ dinh dưỡng và thực đơn vào con không vào mẹ?',
    ];
  }

  @override
  void initState() {
    super.initState();
    _currentSessionId = DateTime.now().millisecondsSinceEpoch.toString();
    _attachedContext = widget.attachedHealthContext;
    if (_attachedContext != null) {
      _journeyType = _attachedContext!['journeyType'] as String?;
      final week = _attachedContext!['gestationalAge'] ??
          _attachedContext!['gestationalWeeks'];
      if (week != null) {
        _pregnancyWeek = (week is int) ? week : int.tryParse(week.toString());
      }
    }
    if (widget.initialPrompt != null && widget.initialPrompt!.isNotEmpty) {
      _inputCtrl.text = widget.initialPrompt!;
    }
    _loadHistoryFromStorage();
    _loadJourneyContext();
  }

  Future<void> _loadJourneyContext() async {
    try {
      final res = await apiGet('/api/v1/journeys/me/dashboard');
      if (res is Map && mounted) {
        final data = (res['data'] is Map) ? res['data'] : res;
        final type = data['journeyType'] as String?;
        final week = data['pregnancyWeek'] ??
            data['completedGestationalWeek'] ??
            data['effectivePregnancyWeek'];
        setState(() {
          if (type != null) _journeyType = type;
          if (week != null) {
            _pregnancyWeek =
                (week is int) ? week : int.tryParse(week.toString());
          }
        });
      }
    } catch (_) {}
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
        final loadedSessions = decoded
            .map((e) => _ChatSession.fromJson(e as Map<String, dynamic>))
            .toList();
        if (mounted) {
          setState(() {
            _sessions = loadedSessions;
            // Tự động tải lại phiên trò chuyện gần nhất nếu màn hình đang trống và không có initialPrompt
            if (_messages.isEmpty &&
                _sessions.isNotEmpty &&
                widget.initialPrompt == null) {
              final latest = _sessions.first;
              _currentSessionId = latest.id;
              _messages.clear();
              _messages.addAll(latest.messages);
            }
          });
          _scrollToBottom();
        }
      }
    } catch (_) {}
    if (mounted) {
      setState(() => _loadingHistory = false);
      if (widget.autoSendInitialPrompt &&
          widget.initialPrompt != null &&
          widget.initialPrompt!.trim().isNotEmpty) {
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (mounted && !_sending) {
            _send(widget.initialPrompt!);
          }
        });
      }
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
    final firstUserMsg = _messages.firstWhere(
      (m) => m.isUser,
      orElse: () => _messages.first,
    );
    final title = firstUserMsg.text.length > 40
        ? '${firstUserMsg.text.substring(0, 37)}...'
        : firstUserMsg.text;

    final existingIndex = _sessions.indexWhere(
      (s) => s.id == _currentSessionId,
    );
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
    final list = <String>[];
    try {
      final uri = Uri.parse(apiBaseUrl);
      if (uri.host.isNotEmpty) {
        list.add('${uri.scheme}://${uri.host}:8001');
      }
    } catch (_) {}
    if (kIsWeb) {
      list.add('http://127.0.0.1:8001');
    } else if (Platform.isAndroid) {
      list.addAll(['http://10.0.2.2:8001', 'http://127.0.0.1:8001']);
    } else {
      list.addAll(['http://127.0.0.1:8001', 'http://localhost:8001']);
    }
    return list;
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
        .map(
          (m) => {'role': m.isUser ? 'user' : 'assistant', 'content': m.text},
        )
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
                'stage': _effectiveStage,
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
                final title = s['title'].toString().trim();
                final sec = s['section']?.toString().trim();
                final formatted =
                    (sec != null &&
                        sec.isNotEmpty &&
                        sec.toLowerCase() != title.toLowerCase())
                    ? '$title ($sec)'
                    : title;
                if (!sourcesList.contains(formatted)) {
                  sourcesList.add(formatted);
                }
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
        final data = await apiPost('/api/v1/rag/answer', {'query': question});

        final resData = (data is Map && data.containsKey('data'))
            ? data['data']
            : data;

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

  void _openAttachmentDetailsModal() {
    if (_attachedContext == null) return;
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => _AttachedHealthContextBottomSheet(
        contextData: _attachedContext!,
        onRemoveAttachment: () {
          Navigator.of(ctx).pop();
          setState(() => _attachedContext = null);
        },
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
                            ? (_effectiveStage == 'PRECONCEPTION'
                                ? 'Đồng hành Chuẩn bị mang thai • 24/7'
                                : (_effectiveStage == 'POSTPARTUM'
                                    ? 'Đồng hành Hậu sản & Chăm bé • 24/7'
                                    : (_pregnancyWeek != null
                                        ? 'Đồng hành cùng Mẹ bầu (Tuần $_pregnancyWeek) • 24/7'
                                        : 'Đồng hành cùng Mẹ bầu • 24/7')))
                            : 'Hỗ trợ Gia đình chăm sóc mẹ & bé',
                        style: const TextStyle(
                          fontSize: 11,
                          color: Colors.white70,
                        ),
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
            tooltip: 'Lịch sử tư vấn',
            onPressed: _openHistoryModal,
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
            )
          else
            Expanded(
              child: ListView.builder(
                controller: _scrollCtrl,
                padding: const EdgeInsets.fromLTRB(14, 12, 14, 8),
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
          if (_attachedContext != null)
            Container(
              margin: const EdgeInsets.symmetric(horizontal: 14, vertical: 4),
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [Color(0xFFFFF4F0), Color(0xFFFEEDEA)],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: BorderRadius.circular(14),
                border: Border.all(color: const Color(0xFFE5BDB3)),
                boxShadow: [
                  BoxShadow(
                    color: const Color(0xFFC98C7B).withValues(alpha: 0.1),
                    blurRadius: 6,
                    offset: const Offset(0, 2),
                  ),
                ],
              ),
              child: Material(
                color: Colors.transparent,
                child: InkWell(
                  borderRadius: BorderRadius.circular(14),
                  onTap: _openAttachmentDetailsModal,
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 9,
                    ),
                    child: Row(
                      children: [
                        Container(
                          padding: const EdgeInsets.all(6),
                          decoration: BoxDecoration(
                            color: const Color(0xFFC98C7B).withValues(alpha: 0.2),
                            shape: BoxShape.circle,
                          ),
                          child: const Icon(
                            Icons.assignment_outlined,
                            size: 18,
                            color: Color(0xFF845143),
                          ),
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Row(
                                children: [
                                  Flexible(
                                    child: Text(
                                      'Hồ sơ đính kèm: ${_attachedContext!['metricLabel'] ?? 'Chỉ số sức khỏe'}',
                                      style: const TextStyle(
                                        fontSize: 12.5,
                                        fontWeight: FontWeight.w700,
                                        color: Color(0xFF6B3A2D),
                                      ),
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  ),
                                  () {
                                    final st = _attachedContext!['stage'] ??
                                        _attachedContext!['journeyType'];
                                    String? stageTag;
                                    if (st == 'PRECONCEPTION' ||
                                        st == 'PRE_PREGNANCY') {
                                      stageTag = 'Chuẩn bị mang thai';
                                    } else if (st == 'POSTPARTUM' ||
                                        st == 'BABY_CARE') {
                                      stageTag = 'Hậu sản & Chăm bé';
                                    } else {
                                      final w = _attachedContext!['gestationalAge'] ??
                                          _attachedContext!['gestationalWeeks'];
                                      if (w != null) stageTag = 'Tuần $w';
                                    }

                                    if (stageTag == null) {
                                      return const SizedBox.shrink();
                                    }
                                    return Padding(
                                      padding: const EdgeInsets.only(left: 6),
                                      child: Container(
                                        padding: const EdgeInsets.symmetric(
                                          horizontal: 6,
                                          vertical: 1.5,
                                        ),
                                        decoration: BoxDecoration(
                                          color: const Color(0xFFC98C7B),
                                          borderRadius: BorderRadius.circular(8),
                                        ),
                                        child: Text(
                                          stageTag,
                                          style: const TextStyle(
                                            fontSize: 10,
                                            fontWeight: FontWeight.bold,
                                            color: Colors.white,
                                          ),
                                        ),
                                      ),
                                    );
                                  }(),
                                ],
                              ),
                              const SizedBox(height: 2),
                              const Row(
                                children: [
                                  Text(
                                    'Chạm để xem chi tiết sinh hiệu, survey...',
                                    style: TextStyle(
                                      fontSize: 11,
                                      fontWeight: FontWeight.w500,
                                      color: Color(0xFF9E6555),
                                    ),
                                  ),
                                  SizedBox(width: 4),
                                  Icon(
                                    Icons.arrow_forward_ios_rounded,
                                    size: 10,
                                    color: Color(0xFF9E6555),
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ),
                        IconButton(
                          icon: const Icon(Icons.close_rounded, size: 18),
                          color: const Color(0xFF845143),
                          tooltip: 'Gỡ đính kèm',
                          onPressed: () => setState(() => _attachedContext = null),
                          visualDensity: VisualDensity.compact,
                          padding: EdgeInsets.zero,
                          constraints: const BoxConstraints(),
                        ),
                      ],
                    ),
                  ),
                ),
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
                    Icon(
                      Icons.history_rounded,
                      color: Color(0xFFC98C7B),
                      size: 22,
                    ),
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
                    icon: const Icon(
                      Icons.delete_sweep,
                      size: 18,
                      color: Colors.redAccent,
                    ),
                    label: const Text(
                      'Xóa tất cả',
                      style: TextStyle(fontSize: 12, color: Colors.redAccent),
                    ),
                    onPressed: () {
                      showDialog(
                        context: context,
                        builder: (_) => AlertDialog(
                          title: const Text('Xóa tất cả lịch sử?'),
                          content: const Text(
                            'Toàn bộ các đoạn hội thoại AI đã lưu sẽ bị xóa vĩnh viễn.',
                          ),
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
                              child: const Text(
                                'Xóa hết',
                                style: TextStyle(color: Colors.red),
                              ),
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
                    Icon(
                      Icons.chat_bubble_outline,
                      size: 48,
                      color: Colors.grey.shade400,
                    ),
                    const SizedBox(height: 12),
                    Text(
                      'Chưa có lịch sử trò chuyện nào',
                      style: TextStyle(
                        color: Colors.grey.shade600,
                        fontSize: 14,
                      ),
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
                        fontWeight: isCurrent
                            ? FontWeight.bold
                            : FontWeight.w500,
                        color: const Color(0xFF2D2421),
                      ),
                    ),
                    subtitle: Text(
                      '${_formatSessionDate(s.updatedAt)} • ${s.messages.length} tin nhắn',
                      style: TextStyle(
                        fontSize: 11,
                        color: Colors.grey.shade600,
                      ),
                    ),
                    trailing: IconButton(
                      icon: const Icon(
                        Icons.close,
                        size: 18,
                        color: Colors.grey,
                      ),
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

  const _WelcomeView({required this.quickPrompts, required this.onPromptTap});

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
                  padding: const EdgeInsets.symmetric(
                    horizontal: 14,
                    vertical: 12,
                  ),
                  side: BorderSide(color: Colors.grey.shade300),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
                onPressed: () => onPromptTap(prompt),
                child: Row(
                  children: [
                    const Icon(
                      Icons.chat_bubble_outline,
                      size: 16,
                      color: Color(0xFFC98C7B),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        prompt,
                        style: const TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ),
                    const Icon(
                      Icons.chevron_right,
                      size: 18,
                      color: Colors.grey,
                    ),
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
                Expanded(child: _buildInlineSpans(content, isUser)),
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
                Expanded(child: _buildInlineSpans(content, isUser)),
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
      r'(\*\*\*([^*]+)\*\*\*|\*\*([^*]+)\*\*|\*([^*]+)\*|`([^`]+)`|([^*`]+))',
    );
    final matches = regex.allMatches(text);

    if (matches.isEmpty) {
      spans.add(
        TextSpan(
          text: text,
          style: TextStyle(
            color: isUser ? Colors.white : const Color(0xFF2D2421),
            fontSize: 13.5,
            height: 1.45,
          ),
        ),
      );
    } else {
      for (final m in matches) {
        if (m.group(2) != null) {
          // ***bold italic***
          spans.add(
            TextSpan(
              text: m.group(2),
              style: TextStyle(
                fontWeight: FontWeight.bold,
                fontStyle: FontStyle.italic,
                color: isUser ? Colors.white : const Color(0xFF2D2421),
                fontSize: 13.5,
              ),
            ),
          );
        } else if (m.group(3) != null) {
          // **bold**
          spans.add(
            TextSpan(
              text: m.group(3),
              style: TextStyle(
                fontWeight: FontWeight.bold,
                color: isUser ? Colors.white : const Color(0xFF2D2421),
                fontSize: 13.5,
              ),
            ),
          );
        } else if (m.group(4) != null) {
          // *italic*
          spans.add(
            TextSpan(
              text: m.group(4),
              style: TextStyle(
                fontStyle: FontStyle.italic,
                color: isUser ? Colors.white70 : const Color(0xFF5A463F),
                fontSize: 13.5,
              ),
            ),
          );
        } else if (m.group(5) != null) {
          // `code`
          spans.add(
            TextSpan(
              text: m.group(5),
              style: TextStyle(
                fontFamily: 'monospace',
                backgroundColor: isUser
                    ? Colors.white24
                    : const Color(0xFFF0EAE5),
                color: isUser ? Colors.white : const Color(0xFFC98C7B),
                fontSize: 12.5,
              ),
            ),
          );
        } else if (m.group(6) != null) {
          // normal text
          spans.add(
            TextSpan(
              text: m.group(6),
              style: TextStyle(
                color: isUser ? Colors.white : const Color(0xFF2D2421),
                fontSize: 13.5,
                height: 1.45,
              ),
            ),
          );
        }
      }
    }

    return RichText(text: TextSpan(children: spans));
  }

  @override
  Widget build(BuildContext context) {
    final isUser = message.isUser;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Column(
        crossAxisAlignment: isUser
            ? CrossAxisAlignment.end
            : CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: isUser
                ? MainAxisAlignment.end
                : MainAxisAlignment.start,
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
                  padding: const EdgeInsets.symmetric(
                    horizontal: 15,
                    vertical: 12,
                  ),
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
                                  Icon(
                                    Icons.menu_book,
                                    size: 13,
                                    color: Color(0xFFC98C7B),
                                  ),
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
                            horizontal: 10,
                            vertical: 6,
                          ),
                          decoration: BoxDecoration(
                            color: const Color(0xFFFFF3E0),
                            borderRadius: BorderRadius.circular(8),
                            border: Border.all(color: const Color(0xFFFFCC80)),
                          ),
                          child: const Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Icon(
                                Icons.info_outline,
                                size: 14,
                                color: Color(0xFFE65100),
                              ),
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
                          borderRadius: BorderRadius.circular(16),
                        ),
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
                    : const Icon(
                        Icons.arrow_upward,
                        color: Colors.white,
                        size: 22,
                      ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _AttachedHealthContextBottomSheet extends StatelessWidget {
  final Map<String, dynamic> contextData;
  final VoidCallback onRemoveAttachment;

  const _AttachedHealthContextBottomSheet({
    required this.contextData,
    required this.onRemoveAttachment,
  });

  @override
  Widget build(BuildContext context) {
    final metricLabel =
        contextData['metricLabel'] ?? contextData['metricType'] ?? 'Chỉ số sức khỏe';
    final displayValue =
        contextData['displayValue'] ?? contextData['value'] ?? '';
    final gestationalAge =
        contextData['gestationalAge'] ?? contextData['gestationalWeeks'];
    final note =
        (contextData['note'] ?? contextData['free_text_notes'])?.toString();
    final surveyRisks = (contextData['surveyRiskConditions'] as List?)
            ?.map((e) => e.toString())
            .toList() ??
        [];
    final riskFactors =
        (contextData['riskFactors'] ?? contextData['reasons'] as List?)
            ?.map((e) => e.toString())
            .toList() ??
        [];
    final latestVitals = contextData['latestVitals'];

    String getTrimester(dynamic week) {
      if (week == null) return '';
      final w = int.tryParse(week.toString());
      if (w == null) return '';
      if (w <= 13) return 'Tam cá nguyệt 1 (3 tháng đầu)';
      if (w <= 27) return 'Tam cá nguyệt 2 (3 tháng giữa)';
      return 'Tam cá nguyệt 3 (3 tháng cuối)';
    }

    String formatSurveyLabel(String raw) {
      switch (raw) {
        case 'PRIOR_PREECLAMPSIA':
          return 'Tiền sử Tiền sản giật';
        case 'CHRONIC_HYPERTENSION':
          return 'Tăng huyết áp mạn';
        case 'PREGESTATIONAL_DIABETES':
        case 'PRIOR_GDM':
          return 'Tiền sử ĐTĐ thai kỳ';
        default:
          return raw;
      }
    }

    return Container(
      constraints: BoxConstraints(
        maxHeight: MediaQuery.of(context).size.height * 0.85,
      ),
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      child: SafeArea(
        top: false,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Handle bar
            Container(
              margin: const EdgeInsets.only(top: 12, bottom: 8),
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: const Color(0xFFE0E0E0),
                borderRadius: BorderRadius.circular(2),
              ),
            ),

            // Header
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 8, 16, 12),
              child: Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: const Color(0xFFFFF1EC),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: const Icon(
                      Icons.health_and_safety_rounded,
                      color: Color(0xFF845143),
                      size: 22,
                    ),
                  ),
                  const SizedBox(width: 12),
                  const Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Hồ sơ Sức khỏe Đính kèm',
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFF333333),
                          ),
                        ),
                        SizedBox(height: 2),
                        Text(
                          'Ngữ cảnh lâm sàng cung cấp cho AI Nurse',
                          style: TextStyle(fontSize: 12, color: Colors.grey),
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    icon: const Icon(Icons.close, color: Colors.grey),
                    onPressed: () => Navigator.of(context).pop(),
                  ),
                ],
              ),
            ),
            const Divider(height: 1),

            // Content
            Flexible(
              child: ListView(
                padding: const EdgeInsets.all(20),
                children: [
                  // Thông tin Giai đoạn / Tuần thai
                  () {
                    final st = contextData['stage'] ?? contextData['journeyType'];
                    if (st == 'PRECONCEPTION' || st == 'PRE_PREGNANCY') {
                      return Container(
                        margin: const EdgeInsets.only(bottom: 16),
                        padding: const EdgeInsets.all(14),
                        decoration: BoxDecoration(
                          gradient: const LinearGradient(
                            colors: [Color(0xFFF1F8E9), Color(0xFFE8F5E9)],
                            begin: Alignment.topLeft,
                            end: Alignment.bottomRight,
                          ),
                          borderRadius: BorderRadius.circular(16),
                          border: Border.all(color: const Color(0xFFC8E6C9)),
                        ),
                        child: const Row(
                          children: [
                            Icon(
                              Icons.spa_rounded,
                              color: Color(0xFF2E7D32),
                              size: 28,
                            ),
                            SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    'Giai đoạn: Chuẩn bị mang thai',
                                    style: TextStyle(
                                      fontSize: 15,
                                      fontWeight: FontWeight.bold,
                                      color: Color(0xFF1B5E20),
                                    ),
                                  ),
                                  SizedBox(height: 2),
                                  Text(
                                    'Kế hoạch thụ thai, bổ sung vi chất & sàng lọc tiền sản',
                                    style: TextStyle(
                                      fontSize: 12,
                                      color: Color(0xFF2E7D32),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      );
                    }
                    if (st == 'POSTPARTUM' || st == 'BABY_CARE') {
                      return Container(
                        margin: const EdgeInsets.only(bottom: 16),
                        padding: const EdgeInsets.all(14),
                        decoration: BoxDecoration(
                          gradient: const LinearGradient(
                            colors: [Color(0xFFE3F2FD), Color(0xFFEDE7F6)],
                            begin: Alignment.topLeft,
                            end: Alignment.bottomRight,
                          ),
                          borderRadius: BorderRadius.circular(16),
                          border: Border.all(color: const Color(0xFFBBDEFB)),
                        ),
                        child: const Row(
                          children: [
                            Icon(
                              Icons.child_friendly_rounded,
                              color: Color(0xFF1565C0),
                              size: 28,
                            ),
                            SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    'Giai đoạn: Hậu sản & Chăm sóc bé',
                                    style: TextStyle(
                                      fontSize: 15,
                                      fontWeight: FontWeight.bold,
                                      color: Color(0xFF0D47A1),
                                    ),
                                  ),
                                  SizedBox(height: 2),
                                  Text(
                                    'Phục hồi thể chất, nuôi con bằng sữa mẹ & sức khỏe tinh thần',
                                    style: TextStyle(
                                      fontSize: 12,
                                      color: Color(0xFF1565C0),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      );
                    }
                    if (gestationalAge != null) {
                      return Container(
                        margin: const EdgeInsets.only(bottom: 16),
                        padding: const EdgeInsets.all(14),
                        decoration: BoxDecoration(
                          gradient: const LinearGradient(
                            colors: [Color(0xFFFFF8F5), Color(0xFFFFF1EC)],
                            begin: Alignment.topLeft,
                            end: Alignment.bottomRight,
                          ),
                          borderRadius: BorderRadius.circular(16),
                          border: Border.all(color: const Color(0xFFE5BDB3)),
                        ),
                        child: Row(
                          children: [
                            const Icon(
                              Icons.child_care_rounded,
                              color: Color(0xFF845143),
                              size: 28,
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    'Tuần thai hiện tại: Tuần $gestationalAge',
                                    style: const TextStyle(
                                      fontSize: 15,
                                      fontWeight: FontWeight.bold,
                                      color: Color(0xFF6B3A2D),
                                    ),
                                  ),
                                  if (getTrimester(gestationalAge).isNotEmpty) ...[
                                    const SizedBox(height: 2),
                                    Text(
                                      getTrimester(gestationalAge),
                                      style: const TextStyle(
                                        fontSize: 12,
                                        color: Color(0xFF845143),
                                      ),
                                    ),
                                  ],
                                ],
                              ),
                            ),
                          ],
                        ),
                      );
                    }
                    return const SizedBox.shrink();
                  }(),

                  // Chỉ số vừa đo
                  if (displayValue.toString().isNotEmpty) ...[
                    const Text(
                      'Chỉ số sinh hiệu vừa ghi nhận',
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFF555555),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Container(
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: const Color(0xFFF9F7F5),
                        borderRadius: BorderRadius.circular(14),
                        border: Border.all(color: const Color(0xFFEFEBE9)),
                      ),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Text(
                            metricLabel.toString(),
                            style: const TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.w600,
                              color: Color(0xFF333333),
                            ),
                          ),
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 10,
                              vertical: 4,
                            ),
                            decoration: BoxDecoration(
                              color: const Color(0xFF845143).withValues(alpha: 0.1),
                              borderRadius: BorderRadius.circular(8),
                            ),
                            child: Text(
                              displayValue.toString(),
                              style: const TextStyle(
                                fontSize: 14,
                                fontWeight: FontWeight.bold,
                                color: Color(0xFF845143),
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 16),
                  ],

                  // Ghi chú triệu chứng
                  if (note != null && note.trim().isNotEmpty) ...[
                    const Text(
                      'Ghi chú triệu chứng từ mẹ',
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFF555555),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: const Color(0xFFFFF9F7),
                        borderRadius: BorderRadius.circular(14),
                        border: Border.all(color: const Color(0xFFF0DDD6)),
                      ),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Icon(
                            Icons.edit_note_rounded,
                            color: Color(0xFF845143),
                            size: 20,
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              note,
                              style: const TextStyle(
                                fontSize: 13,
                                color: Color(0xFF444444),
                                height: 1.4,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 16),
                  ],

                  // Tiền sử / Bệnh nền (Survey)
                  if (surveyRisks.isNotEmpty) ...[
                    const Text(
                      'Tiền sử & Bệnh nền (từ Khảo sát)',
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFF555555),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: surveyRisks.map((r) {
                        return Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 12,
                            vertical: 6,
                          ),
                          decoration: BoxDecoration(
                            color: const Color(0xFFFAF1ED),
                            borderRadius: BorderRadius.circular(20),
                            border: Border.all(color: const Color(0xFFD6C2BD)),
                          ),
                          child: Text(
                            formatSurveyLabel(r),
                            style: const TextStyle(
                              fontSize: 12,
                              fontWeight: FontWeight.w600,
                              color: Color(0xFF845143),
                            ),
                          ),
                        );
                      }).toList(),
                    ),
                    const SizedBox(height: 16),
                  ],

                  // Cảnh báo AI phát hiện
                  if (riskFactors.isNotEmpty) ...[
                    const Text(
                      'Dấu hiệu AI lưu ý trong lần đo này',
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFF555555),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Container(
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: const Color(0xFFFFF3E0),
                        borderRadius: BorderRadius.circular(14),
                        border: Border.all(color: const Color(0xFFFFCC80)),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: riskFactors.map((rf) {
                          return Padding(
                            padding: const EdgeInsets.symmetric(vertical: 3),
                            child: Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                const Padding(
                                  padding: EdgeInsets.only(top: 4, right: 8),
                                  child: Icon(
                                    Icons.warning_amber_rounded,
                                    size: 14,
                                    color: Color(0xFFE65100),
                                  ),
                                ),
                                Expanded(
                                  child: Text(
                                    rf,
                                    style: const TextStyle(
                                      fontSize: 12.5,
                                      color: Color(0xFFBF360C),
                                      fontWeight: FontWeight.w500,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          );
                        }).toList(),
                      ),
                    ),
                    const SizedBox(height: 16),
                  ],

                  // Toàn bộ sinh hiệu gần nhất
                  if (latestVitals != null) ...[
                    const Text(
                      'Snapshot Toàn bộ Sinh hiệu Gần nhất',
                      style: TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFF555555),
                      ),
                    ),
                    const SizedBox(height: 8),
                    if (latestVitals is Map && latestVitals.isNotEmpty)
                      Container(
                        padding: const EdgeInsets.all(14),
                        decoration: BoxDecoration(
                          color: const Color(0xFFF9F7F5),
                          borderRadius: BorderRadius.circular(14),
                          border: Border.all(color: const Color(0xFFEFEBE9)),
                        ),
                        child: Column(
                          children: latestVitals.entries.map((e) {
                            return Padding(
                              padding: const EdgeInsets.symmetric(vertical: 4),
                              child: Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceBetween,
                                children: [
                                  Text(
                                    e.key.toString(),
                                    style: const TextStyle(
                                      fontSize: 13,
                                      color: Color(0xFF666666),
                                    ),
                                  ),
                                  Text(
                                    e.value.toString(),
                                    style: const TextStyle(
                                      fontSize: 13,
                                      fontWeight: FontWeight.bold,
                                      color: Color(0xFF333333),
                                    ),
                                  ),
                                ],
                              ),
                            );
                          }).toList(),
                        ),
                      )
                    else if (latestVitals is String && latestVitals.isNotEmpty)
                      Container(
                        width: double.infinity,
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: const Color(0xFFF9F7F5),
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(color: const Color(0xFFEFEBE9)),
                        ),
                        child: Text(
                          latestVitals,
                          style: const TextStyle(
                            fontSize: 12.5,
                            color: Color(0xFF444444),
                          ),
                        ),
                      ),
                    const SizedBox(height: 16),
                  ],

                  // Medical disclaimer note
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: const Color(0xFFF0F4F8),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: const Row(
                      children: [
                        Icon(
                          Icons.info_outline_rounded,
                          size: 16,
                          color: Color(0xFF5C6B73),
                        ),
                        SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            'Ngữ cảnh này được đính kèm tự động để AI Nurse Assistant nắm bắt đầy đủ thông tin và đưa ra lời khuyên phù hợp nhất.',
                            style: TextStyle(
                              fontSize: 11.5,
                              color: Color(0xFF5C6B73),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),

            // Bottom action buttons
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 10, 20, 16),
              child: Row(
                children: [
                  Expanded(
                    child: OutlinedButton.icon(
                      icon: const Icon(Icons.delete_outline, size: 18),
                      label: const Text('Gỡ đính kèm'),
                      style: OutlinedButton.styleFrom(
                        foregroundColor: const Color(0xFFBA1A1A),
                        side: const BorderSide(color: Color(0xFFFFCDD2)),
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                      onPressed: onRemoveAttachment,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: ElevatedButton(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0xFFC98C7B),
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        elevation: 0,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                      onPressed: () => Navigator.of(context).pop(),
                      child: const Text(
                        'Đã hiểu',
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                    ),
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
