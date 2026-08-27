import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:go_router/go_router.dart';
import 'package:http/http.dart' as http;
import 'package:universal_io/io.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../../core/network/api_client.dart';
import '../../../core/auth/auth_state.dart';
import '../../directChat/services/direct_chat_service.dart';
import '../../recommendation/models/recommendation_questionnaire.dart';

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
  Map<String, dynamic>? _attachedContext;
  String? _journeyType;
  int? _pregnancyWeek;
  Map<String, dynamic>? _surveyProfile;
  Map<String, dynamic>? _surveyDerived;
  String? _surveyStatus;

  String get _effectiveStage {
    final jt =
        _journeyType ??
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
    final role = (AuthState.instance.role ?? 'MOTHER').toUpperCase();
    final isMother = role == 'MOTHER';

    if (!isMother) {
      return const [
        'Chế độ dinh dưỡng và món ăn bồi bổ tốt nhất cho vợ mang thai?',
        'Dấu hiệu chuyển dạ và nguy hiểm của mẹ bầu mà gia đình cần đưa đi viện ngay?',
        'Cách chăm sóc và massage giúp mẹ bầu giảm đau lưng, căng thẳng?',
        'Người thân cần chuẩn bị và hỗ trợ gì khi mẹ sau sinh và chăm sóc bé sơ sinh?',
      ];
    }

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
      final week =
          _attachedContext!['gestationalAge'] ??
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
    final role = (AuthState.instance.role ?? 'MOTHER').toUpperCase();
    if (role != 'MOTHER') return;

    try {
      final res = await apiGet('/api/v1/journeys/me/dashboard');
      if (res is Map && mounted) {
        final data = (res['data'] is Map) ? res['data'] : res;
        final type = data['journeyType'] as String?;
        final week =
            data['pregnancyWeek'] ??
            data['completedGestationalWeek'] ??
            data['effectivePregnancyWeek'];
        setState(() {
          if (type != null) _journeyType = type;
          if (week != null) {
            _pregnancyWeek = (week is int)
                ? week
                : int.tryParse(week.toString());
          }
        });
      }
    } catch (_) {}

    try {
      final profileRes = await apiGet('/api/v1/recommendations/profile');
      if (profileRes is Map && mounted) {
        final data = (profileRes['data'] is Map)
            ? profileRes['data']
            : profileRes;
        final profile = (data['profile'] is Map)
            ? Map<String, dynamic>.from(data['profile'] as Map)
            : null;
        final derived = (data['derived'] is Map)
            ? Map<String, dynamic>.from(data['derived'] as Map)
            : null;
        final status = data['status'] as String?;
        if (mounted) {
          setState(() {
            _surveyProfile = profile;
            _surveyDerived = derived;
            _surveyStatus = status;
          });
        }
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
    final role = (AuthState.instance.role ?? 'MOTHER').toUpperCase();
    final isMother = role == 'MOTHER';

    final historyPayload = _messages
        .take(_messages.length - 1)
        .map(
          (m) => {'role': m.isUser ? 'user' : 'assistant', 'content': m.text},
        )
        .toList();

    final Map<String, dynamic> requestPayload = {
      'message': question,
      'stage': isMother ? _effectiveStage : 'ALL',
      'user_role': isMother ? 'MOTHER' : 'FAMILY',
      'conversation_history': historyPayload,
    };

    if (isMother) {
      if (_pregnancyWeek != null) {
        requestPayload['gestational_age_weeks'] = _pregnancyWeek;
      }
      if (_surveyProfile != null && _surveyProfile!.isNotEmpty) {
        requestPayload['survey_profile'] = _surveyProfile;
      }
      if (_attachedContext != null) {
        final metricType = _attachedContext!['metricType']
            ?.toString()
            .toUpperCase();
        final val = _attachedContext!['value'];
        final Map<String, dynamic> recentMetrics = {};
        if (metricType == 'BLOOD_PRESSURE' && val is Map) {
          if (val['systolic'] != null) {
            recentMetrics['systolic_bp'] = val['systolic'];
          }
          if (val['diastolic'] != null) {
            recentMetrics['diastolic_bp'] = val['diastolic'];
          }
        } else if (metricType == 'TEMPERATURE' ||
            metricType == 'BODY_TEMPERATURE') {
          if (val is num) recentMetrics['temperature'] = val.toDouble();
        } else if (metricType == 'BLOOD_GLUCOSE' || metricType == 'GLUCOSE') {
          if (val is num) recentMetrics['blood_glucose'] = val.toDouble();
        } else if (metricType == 'FETAL_MOVEMENT' ||
            metricType == 'FETAL_MOVEMENTS') {
          if (val is num) {
            recentMetrics['fetal_movements_count'] = val.toInt();
          }
        }
        final notes = _attachedContext!['notes']?.toString();
        if (notes != null && notes.isNotEmpty) {
          recentMetrics['symptoms'] = [notes];
        }
        if (recentMetrics.isNotEmpty) {
          requestPayload['recent_metrics'] = recentMetrics;
        }
      }
    }

    for (final base in _pythonCandidates) {
      try {
        final response = await http
            .post(
              Uri.parse('$base/api/v1/chat/message'),
              headers: {
                'Content-Type': 'application/json',
                'X-Internal-API-Key': 'carebridge',
              },
              body: jsonEncode(requestPayload),
            )
            .timeout(const Duration(seconds: 15));

        if (response.statusCode == 200) {
          final decoded = jsonDecode(utf8.decode(response.bodyBytes));
          answerText = decoded['answer']?.toString() ?? '';
          isWarning =
              decoded['has_critical_warning'] == true ||
              decoded['need_expert_consultation'] == true;

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

    if (isWarning && mounted) {
      Future.delayed(const Duration(milliseconds: 700), () {
        if (mounted) {
          _showNeedExpertConsultationDialog();
        }
      });
    }
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
    final fullContextData = Map<String, dynamic>.from(_attachedContext!);
    if (fullContextData['surveyProfile'] == null && _surveyProfile != null) {
      fullContextData['surveyProfile'] = _surveyProfile;
      fullContextData['surveyDerived'] = _surveyDerived;
      fullContextData['surveyStatus'] = _surveyStatus;
    }
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (ctx) => _AttachedHealthContextBottomSheet(
        contextData: fullContextData,
        onRemoveAttachment: () {
          Navigator.of(ctx).pop();
          setState(() => _attachedContext = null);
        },
      ),
    );
  }

  Future<void> _handleFollowupTap(String prompt) async {
    final lower = prompt.toLowerCase();

    // 1. Kích hoạt quay số khẩn cấp 115 (Zero-delay native dialer)
    if (lower.contains('115') ||
        (lower.contains('cấp cứu') && lower.contains('gọi'))) {
      final uri = Uri.parse('tel:115');
      try {
        if (await canLaunchUrl(uri)) {
          await launchUrl(uri);
        } else {
          await launchUrl(uri, mode: LaunchMode.externalApplication);
        }
      } catch (_) {
        await launchUrl(uri, mode: LaunchMode.externalApplication);
      }
      return;
    }

    // 2. Kích hoạt mở Bản đồ Cơ sở y tế / Bệnh viện phụ sản gần nhất (UC-63 / UC-141)
    if ((lower.contains('bệnh viện') ||
            lower.contains('cơ sở y tế') ||
            lower.contains('phụ sản')) &&
        (lower.contains('gần nhất') ||
            lower.contains('ở đâu') ||
            lower.contains('chỉ đường'))) {
      context.push('/emergency/map');
      return;
    }

    // 3. Với các câu hỏi khác, gửi tiếp tục hỏi AI Nurse
    _send(prompt);
  }

  Future<void> _navigateToDoctorOrExperts() async {
    try {
      final conversations = await DirectChatService.instance
          .listMyConversations();
      if (!mounted) return;
      if (conversations.isNotEmpty) {
        context.push('/direct-chats');
      } else {
        context.push('/experts');
      }
    } catch (_) {
      if (!mounted) return;
      context.push('/experts');
    }
  }

  /// Bước 11 trong Workflow: Khuyến nghị tham vấn Chuyên gia Y tế (Need Expert Consultation)
  void _showNeedExpertConsultationDialog({String? reason}) {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (dialogCtx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        titlePadding: const EdgeInsets.fromLTRB(20, 20, 20, 0),
        contentPadding: const EdgeInsets.fromLTRB(20, 14, 20, 10),
        actionsPadding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
        title: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: const Color(0xFFFFF3E0),
                borderRadius: BorderRadius.circular(10),
              ),
              child: const Icon(
                Icons.medical_services_rounded,
                color: Color(0xFFE65100),
                size: 24,
              ),
            ),
            const SizedBox(width: 12),
            const Expanded(
              child: Text(
                'Khuyến nghị Tham vấn Bác sĩ',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF2D2421),
                ),
              ),
            ),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              reason ??
                  'Dựa trên dấu hiệu sinh hiệu và tình trạng bạn vừa chia sẻ, hệ thống ghi nhận có các chỉ số cần được Bác sĩ / Chuyên gia sản phụ khoa đánh giá trực tiếp để đảm bảo an toàn tối đa cho mẹ và bé.',
              style: const TextStyle(
                fontSize: 13.5,
                color: Color(0xFF4A3E39),
                height: 1.45,
              ),
            ),
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: const Color(0xFFFAF6F2),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: const Color(0xFFE8DFD8)),
              ),
              child: const Row(
                children: [
                  Icon(
                    Icons.verified_user_outlined,
                    size: 16,
                    color: Color(0xFFC98C7B),
                  ),
                  SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      'Tư vấn trực tuyến qua Video / Chat cùng đội ngũ Bác sĩ Sản phụ khoa CareBridge.',
                      style: TextStyle(fontSize: 12, color: Color(0xFF6B4F46)),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
        actions: [
          Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  style: OutlinedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(vertical: 11),
                    side: const BorderSide(color: Color(0xFFD6C2BD)),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                  onPressed: () {
                    Navigator.of(dialogCtx).pop();
                    _showSelfTrackingDisclaimerDialog();
                  },
                  child: const Text(
                    'Tự theo dõi thêm',
                    style: TextStyle(
                      color: Color(0xFF7A5C52),
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: ElevatedButton.icon(
                  icon: const Icon(Icons.arrow_forward_rounded, size: 16),
                  label: const Text('Kết nối Bác sĩ'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFFC98C7B),
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 11),
                    elevation: 0,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                  onPressed: () {
                    Navigator.of(dialogCtx).pop();
                    _navigateToDoctorOrExperts();
                  },
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  /// Bước 12A trong Workflow: Tuyên bố miễn trừ trách nhiệm & Yêu cầu tự theo dõi (Self-tracking requirement & Disclaimer)
  void _showSelfTrackingDisclaimerDialog() {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (dialogCtx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        titlePadding: const EdgeInsets.fromLTRB(20, 20, 20, 0),
        contentPadding: const EdgeInsets.fromLTRB(20, 14, 20, 10),
        actionsPadding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
        title: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: const Color(0xFFFFEBEE),
                borderRadius: BorderRadius.circular(10),
              ),
              child: const Icon(
                Icons.warning_rounded,
                color: Color(0xFFC62828),
                size: 24,
              ),
            ),
            const SizedBox(width: 12),
            const Expanded(
              child: Text(
                'Lưu ý Trách nhiệm Y tế & Tự theo dõi',
                style: TextStyle(
                  fontSize: 15.5,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFFC62828),
                ),
              ),
            ),
          ],
        ),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: const Color(0xFFFFF8F7),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: const Color(0xFFFFCDD2)),
                ),
                child: const Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Icon(
                          Icons.gavel_rounded,
                          size: 16,
                          color: Color(0xFFC62828),
                        ),
                        SizedBox(width: 6),
                        Text(
                          'Tuyên bố miễn trừ trách nhiệm:',
                          style: TextStyle(
                            fontSize: 12.5,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFFC62828),
                          ),
                        ),
                      ],
                    ),
                    SizedBox(height: 6),
                    Text(
                      'Hệ thống AI Nurse chỉ cung cấp thông tin tham khảo dựa trên thuật toán, không thay thế cho chẩn đoán, điều trị hoặc lời khuyên y khoa chuyên nghiệp. Chúng tôi hoàn toàn miễn trừ trách nhiệm đối với bất kỳ hậu quả, tổn thất hoặc diễn biến xấu nào liên quan đến sức khỏe phát sinh từ quyết định từ chối khám y khoa của bạn.',
                      style: TextStyle(
                        fontSize: 12,
                        color: Color(0xFF424242),
                        height: 1.45,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 12),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: const Color(0xFFFFFDE7),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: const Color(0xFFFFF59D)),
                ),
                child: const Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Icon(
                          Icons.monitor_heart_rounded,
                          size: 16,
                          color: Color(0xFFF57F17),
                        ),
                        SizedBox(width: 6),
                        Text(
                          'Yêu cầu tự theo dõi:',
                          style: TextStyle(
                            fontSize: 12.5,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFFF57F17),
                          ),
                        ),
                      ],
                    ),
                    SizedBox(height: 6),
                    Text(
                      'Bạn phải tự chịu trách nhiệm theo dõi sát sao các chỉ số cơ thể, diễn biến triệu chứng và tình trạng sức khỏe hiện tại của mình. Nếu xuất hiện bất kỳ dấu hiệu bất thường hoặc chuyển biến nặng nào, bạn cần lập tức liên hệ với cơ sở y tế gần nhất hoặc gọi cấp cứu.',
                      style: TextStyle(
                        fontSize: 12,
                        color: Color(0xFF424242),
                        height: 1.45,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
        actions: [
          Column(
            children: [
              SizedBox(
                width: double.infinity,
                child: OutlinedButton.icon(
                  icon: const Icon(Icons.arrow_back_rounded, size: 16),
                  label: const Text('Thay đổi ý định, kết nối Bác sĩ'),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: const Color(0xFFC98C7B),
                    side: const BorderSide(color: Color(0xFFC98C7B)),
                    padding: const EdgeInsets.symmetric(vertical: 12),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                  onPressed: () {
                    Navigator.of(dialogCtx).pop();
                    _navigateToDoctorOrExperts();
                  },
                ),
              ),
              const SizedBox(height: 8),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF5A463F),
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 12),
                    elevation: 0,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                  onPressed: () {
                    Navigator.of(dialogCtx).pop();
                  },
                  child: const Text(
                    'Tôi đã hiểu và đồng ý',
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                ),
              ),
            ],
          ),
        ],
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
                      Expanded(
                        child: Text(
                          isMother
                              ? (_effectiveStage == 'PRECONCEPTION'
                                    ? 'Đồng hành Chuẩn bị mang thai • 24/7'
                                    : (_effectiveStage == 'POSTPARTUM'
                                          ? 'Đồng hành Hậu sản & Chăm bé • 24/7'
                                          : (_pregnancyWeek != null
                                                ? 'Đồng hành cùng Mẹ bầu (Tuần $_pregnancyWeek) • 24/7'
                                                : 'Đồng hành cùng Mẹ bầu • 24/7')))
                              : 'Hỗ trợ Gia đình chăm sóc mẹ & bé',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            fontSize: 11,
                            color: Colors.white70,
                          ),
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
                onPromptTap: (p) => _handleFollowupTap(p),
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
                    onFollowupTap: (p) => _handleFollowupTap(p),
                    onConsultDoctorTap: () =>
                        _showNeedExpertConsultationDialog(),
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
                  key: const Key('attachment_context_banner_tap'),
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
                            color: const Color(
                              0xFFC98C7B,
                            ).withValues(alpha: 0.2),
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
                                    final st =
                                        _attachedContext!['stage'] ??
                                        _attachedContext!['journeyType'];
                                    String? stageTag;
                                    if (st == 'PRECONCEPTION' ||
                                        st == 'PRE_PREGNANCY') {
                                      stageTag = 'Chuẩn bị mang thai';
                                    } else if (st == 'POSTPARTUM' ||
                                        st == 'BABY_CARE') {
                                      stageTag = 'Hậu sản & Chăm bé';
                                    } else {
                                      final w =
                                          _attachedContext!['gestationalAge'] ??
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
                                          borderRadius: BorderRadius.circular(
                                            8,
                                          ),
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
                                  Expanded(
                                    child: Text(
                                      'Chạm để xem chi tiết sinh hiệu, survey...',
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                      style: TextStyle(
                                        fontSize: 11,
                                        fontWeight: FontWeight.w500,
                                        color: Color(0xFF9E6555),
                                      ),
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
                          onPressed: () =>
                              setState(() => _attachedContext = null),
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
                separatorBuilder: (_, _) =>
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
                      icon: Icon(
                        Icons.delete_outline_rounded,
                        size: 20,
                        color: Colors.grey.shade500,
                      ),
                      tooltip: 'Xóa đoạn hội thoại này',
                      onPressed: () {
                        showDialog(
                          context: context,
                          builder: (dialogCtx) => AlertDialog(
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(16),
                            ),
                            title: const Row(
                              children: [
                                Icon(
                                  Icons.delete_outline_rounded,
                                  color: Colors.redAccent,
                                  size: 22,
                                ),
                                SizedBox(width: 8),
                                Text(
                                  'Xóa đoạn hội thoại?',
                                  style: TextStyle(
                                    fontSize: 16,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                              ],
                            ),
                            content: Text(
                              'Đoạn hội thoại "${s.title}" (${s.messages.length} tin nhắn) sẽ bị xóa vĩnh viễn khỏi lịch sử.',
                              style: const TextStyle(
                                fontSize: 13,
                                color: Color(0xFF4A3B32),
                              ),
                            ),
                            actions: [
                              TextButton(
                                onPressed: () => Navigator.of(dialogCtx).pop(),
                                child: const Text(
                                  'Hủy',
                                  style: TextStyle(color: Colors.grey),
                                ),
                              ),
                              ElevatedButton(
                                style: ElevatedButton.styleFrom(
                                  backgroundColor: const Color(0xFFBA1A1A),
                                  foregroundColor: Colors.white,
                                  elevation: 0,
                                  shape: RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(8),
                                  ),
                                ),
                                onPressed: () {
                                  Navigator.of(dialogCtx).pop();
                                  onDeleteSession(s.id);
                                },
                                child: const Text(
                                  'Xóa',
                                  style: TextStyle(fontWeight: FontWeight.bold),
                                ),
                              ),
                            ],
                          ),
                        );
                      },
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
  final VoidCallback? onConsultDoctorTap;

  const _MessageBubble({
    required this.message,
    required this.formatTime,
    required this.onFollowupTap,
    this.onConsultDoctorTap,
  });

  String _sanitizeMathAndLatex(String input) {
    if (input.isEmpty) return input;
    var res = input;
    res = res.replaceAll(RegExp(r'\$\\ge\s*([^$]*)\$'), r'≥ $1');
    res = res.replaceAll(RegExp(r'\$\\le\s*([^$]*)\$'), r'≤ $1');
    res = res.replaceAll(RegExp(r'\$\\geq\s*([^$]*)\$'), r'≥ $1');
    res = res.replaceAll(RegExp(r'\$\\leq\s*([^$]*)\$'), r'≤ $1');
    res = res.replaceAll(r'$\ge$', '≥');
    res = res.replaceAll(r'$\le$', '≤');
    res = res.replaceAll(r'$\geq$', '≥');
    res = res.replaceAll(r'$\leq$', '≤');
    res = res.replaceAll(r'$\ge', '≥');
    res = res.replaceAll(r'$\le', '≤');
    res = res.replaceAll(r'$\geq', '≥');
    res = res.replaceAll(r'$\leq', '≤');
    res = res.replaceAll(RegExp(r'\\ge\b'), '≥');
    res = res.replaceAll(RegExp(r'\\le\b'), '≤');
    res = res.replaceAll(RegExp(r'\\geq\b'), '≥');
    res = res.replaceAll(RegExp(r'\\leq\b'), '≤');
    res = res.replaceAll(r'$^\circ C$', '°C');
    res = res.replaceAll(r'^\circ C', '°C');
    res = res.replaceAll(r'$^\circ$', '°');
    res = res.replaceAll(r'^\circ', '°');
    res = res.replaceAll(r'$\approx$', '≈');
    res = res.replaceAll(RegExp(r'\\approx\b'), '≈');
    res = res.replaceAll(r'$\pm$', '±');
    res = res.replaceAll(RegExp(r'\\pm\b'), '±');
    res = res.replaceAll(r'$\times$', '×');
    res = res.replaceAll(RegExp(r'\\times\b'), '×');
    res = res.replaceAll(RegExp(r'\$([≥≤><=+\-\d\.\s/]+)\$'), r'$1');
    return res;
  }

  Widget _buildFormattedContent(String raw, bool isUser) {
    final sanitizedRaw = _sanitizeMathAndLatex(raw);
    final lines = sanitizedRaw.split('\n');
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
              Flexible(
                child: Container(
                  constraints: BoxConstraints(
                    maxWidth: MediaQuery.of(context).size.width * 0.88,
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
                      if (!isUser && message.isWarning) ...[
                        const SizedBox(height: 10),
                        InkWell(
                          onTap: onConsultDoctorTap,
                          borderRadius: BorderRadius.circular(10),
                          child: Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 12,
                              vertical: 9,
                            ),
                            decoration: BoxDecoration(
                              color: const Color(0xFFFBE9E7),
                              borderRadius: BorderRadius.circular(10),
                              border: Border.all(
                                color: const Color(0xFFFFAB91),
                              ),
                            ),
                            child: const Row(
                              children: [
                                Icon(
                                  Icons.medical_services_rounded,
                                  size: 16,
                                  color: Color(0xFFD84315),
                                ),
                                SizedBox(width: 8),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      Text(
                                        'Khuyến nghị từ hệ thống',
                                        style: TextStyle(
                                          fontSize: 10.5,
                                          fontWeight: FontWeight.bold,
                                          color: Color(0xFFD84315),
                                        ),
                                      ),
                                      Text(
                                        'Cần tham vấn Bác sĩ / Chuyên gia y tế',
                                        style: TextStyle(
                                          fontSize: 12,
                                          fontWeight: FontWeight.w600,
                                          color: Color(0xFFBF360C),
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                                Icon(
                                  Icons.arrow_forward_ios_rounded,
                                  size: 12,
                                  color: Color(0xFFD84315),
                                ),
                              ],
                            ),
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
            const SizedBox(height: 10),
            Padding(
              padding: const EdgeInsets.only(left: 2),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Padding(
                    padding: EdgeInsets.only(bottom: 6, left: 4),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(
                          Icons.auto_awesome,
                          size: 13,
                          color: Color(0xFFC98C7B),
                        ),
                        SizedBox(width: 4),
                        Text(
                          'Gợi ý câu hỏi tiếp theo:',
                          style: TextStyle(
                            fontSize: 11,
                            fontWeight: FontWeight.w600,
                            color: Color(0xFF8D6E63),
                          ),
                        ),
                      ],
                    ),
                  ),
                  ...message.followups.map((f) {
                    final lower = f.toLowerCase();
                    final is115 =
                        lower.contains('115') ||
                        (lower.contains('cấp cứu') && lower.contains('gọi'));
                    final isHospital =
                        (lower.contains('bệnh viện') ||
                            lower.contains('cơ sở y tế') ||
                            lower.contains('phụ sản')) &&
                        (lower.contains('gần nhất') ||
                            lower.contains('ở đâu') ||
                            lower.contains('chỉ đường'));

                    return Padding(
                      padding: const EdgeInsets.only(bottom: 6),
                      child: Material(
                        color: Colors.transparent,
                        child: InkWell(
                          onTap: () => onFollowupTap(f),
                          borderRadius: BorderRadius.circular(12),
                          child: Container(
                            width: double.infinity,
                            padding: const EdgeInsets.symmetric(
                              horizontal: 12,
                              vertical: 10,
                            ),
                            decoration: BoxDecoration(
                              color: is115
                                  ? const Color(0xFFFFF2F0)
                                  : (isHospital
                                        ? const Color(0xFFF0F7FF)
                                        : Colors.white),
                              borderRadius: BorderRadius.circular(12),
                              border: Border.all(
                                color: is115
                                    ? const Color(0xFFFFCCC7)
                                    : (isHospital
                                          ? const Color(0xFFBAE0FF)
                                          : const Color(0xFFE8DFD8)),
                                width: (is115 || isHospital) ? 1.2 : 1.0,
                              ),
                              boxShadow: [
                                BoxShadow(
                                  color: is115
                                      ? const Color(
                                          0xFFFF4D4F,
                                        ).withValues(alpha: 0.08)
                                      : Colors.black.withValues(alpha: 0.02),
                                  blurRadius: 3,
                                  offset: const Offset(0, 1),
                                ),
                              ],
                            ),
                            child: Row(
                              crossAxisAlignment: CrossAxisAlignment.center,
                              children: [
                                Icon(
                                  is115
                                      ? Icons.phone_in_talk_rounded
                                      : (isHospital
                                            ? Icons.local_hospital_rounded
                                            : Icons
                                                  .chat_bubble_outline_rounded),
                                  size: 15,
                                  color: is115
                                      ? const Color(0xFFCF1322)
                                      : (isHospital
                                            ? const Color(0xFF0958D9)
                                            : const Color(0xFFC98C7B)),
                                ),
                                const SizedBox(width: 8),
                                Expanded(
                                  child: Text(
                                    f,
                                    style: TextStyle(
                                      fontSize: 12.5,
                                      color: is115
                                          ? const Color(0xFFA8071A)
                                          : (isHospital
                                                ? const Color(0xFF003EB3)
                                                : const Color(0xFF4A3731)),
                                      height: 1.35,
                                      fontWeight: (is115 || isHospital)
                                          ? FontWeight.w600
                                          : FontWeight.w500,
                                    ),
                                    softWrap: true,
                                  ),
                                ),
                                const SizedBox(width: 6),
                                Icon(
                                  is115
                                      ? Icons.call_rounded
                                      : (isHospital
                                            ? Icons.near_me_rounded
                                            : Icons.arrow_forward_ios_rounded),
                                  size: (is115 || isHospital) ? 14 : 11,
                                  color: is115
                                      ? const Color(0xFFCF1322)
                                      : (isHospital
                                            ? const Color(0xFF0958D9)
                                            : const Color(0xFFBCAAA4)),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),
                    );
                  }),
                ],
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
        contextData['metricLabel'] ??
        contextData['metricType'] ??
        'Chỉ số sức khỏe';
    final displayValue =
        contextData['displayValue'] ?? contextData['value'] ?? '';
    final gestationalAge =
        contextData['gestationalAge'] ?? contextData['gestationalWeeks'];
    final note = (contextData['note'] ?? contextData['free_text_notes'])
        ?.toString();
    final rawSurvey = contextData['surveyRiskConditions'];
    final surveyRisks = (rawSurvey is Iterable)
        ? rawSurvey.map((e) => e.toString()).toList()
        : <String>[];
    final rawRisks = contextData['riskFactors'] ?? contextData['reasons'];
    final riskFactors = (rawRisks is Iterable)
        ? rawRisks.map((e) => e.toString()).toList()
        : <String>[];
    final latestVitals = contextData['latestVitals'];

    String getTrimester(dynamic week) {
      if (week == null) return '';
      final w = int.tryParse(week.toString());
      if (w == null) return '';
      if (w <= 13) return 'Tam cá nguyệt 1 (3 tháng đầu)';
      if (w <= 27) return 'Tam cá nguyệt 2 (3 tháng giữa)';
      return 'Tam cá nguyệt 3 (3 tháng cuối)';
    }

    final rawSurveyProfile = contextData['surveyProfile'];
    final surveyProfile = (rawSurveyProfile is Map)
        ? Map<String, dynamic>.from(rawSurveyProfile)
        : null;
    final rawSurveyDerived = contextData['surveyDerived'];
    final surveyDerived = (rawSurveyDerived is Map)
        ? Map<String, dynamic>.from(rawSurveyDerived)
        : null;
    final surveyStatus = contextData['surveyStatus'] as String?;
    String formatSurveyLabel(String raw) {
      switch (raw) {
        case 'PRIOR_PREECLAMPSIA':
          return 'Tiền sử Tiền sản giật';
        case 'CHRONIC_HYPERTENSION':
        case 'HYPERTENSION':
          return 'Tăng huyết áp mạn';
        case 'PREGESTATIONAL_DIABETES':
        case 'PRIOR_GDM':
        case 'PRIOR_GESTATIONAL_DIABETES':
        case 'DIABETES':
          return 'Tiền sử Đái tháo đường';
        case 'CARDIOVASCULAR_DISEASE':
          return 'Bệnh lý tim mạch';
        case 'THYROID_DISORDER':
          return 'Bệnh lý tuyến giáp';
        case 'ASTHMA':
          return 'Hen phế quản';
        case 'KIDNEY_DISEASE':
          return 'Bệnh lý thận';
        case 'AUTOIMMUNE_DISEASE':
          return 'Bệnh tự miễn';
        case 'ANEMIA':
          return 'Thiếu máu';
        case 'PRIOR_PRETERM_BIRTH':
          return 'Tiền sử sinh non';
        case 'PRIOR_STILLBIRTH':
          return 'Tiền sử thai lưu';
        case 'PRIOR_RECURRENT_PREGNANCY_LOSS':
          return 'Tiền sử sảy thai nhiều lần';
        case 'PRIOR_ECTOPIC_PREGNANCY':
          return 'Tiền sử thai ngoài tử cung';
        default:
          return raw;
      }
    }

    String translateCode(String code) {
      if (RecommendationQuestionnaire.labels.containsKey(code)) {
        return RecommendationQuestionnaire.labels[code]!;
      }
      return formatSurveyLabel(code);
    }

    Widget buildCategoryBlock({
      required IconData icon,
      required String title,
      required List<String> items,
      Color iconColor = const Color(0xFF845143),
      Color badgeBg = const Color(0xFFFAF1ED),
      Color badgeBorder = const Color(0xFFD6C2BD),
      Color badgeText = const Color(0xFF845143),
    }) {
      if (items.isEmpty) return const SizedBox.shrink();
      return Padding(
        padding: const EdgeInsets.only(bottom: 12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(icon, size: 15, color: iconColor),
                const SizedBox(width: 6),
                Text(
                  title,
                  style: const TextStyle(
                    fontSize: 12.5,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF555555),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 6),
            Wrap(
              spacing: 6,
              runSpacing: 6,
              children: items.map((text) {
                return Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 5,
                  ),
                  decoration: BoxDecoration(
                    color: badgeBg,
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(color: badgeBorder),
                  ),
                  child: Text(
                    text,
                    style: TextStyle(
                      fontSize: 11.5,
                      fontWeight: FontWeight.w600,
                      color: badgeText,
                    ),
                  ),
                );
              }).toList(),
            ),
          ],
        ),
      );
    }

    final bmiItems = <String>[];
    final reproItems = <String>[];
    final conditionItems = <String>[];
    final lifestyleItems = <String>[];
    final nutritionItems = <String>[];
    final vaccinationItems = <String>[];
    final medicationItems = <String>[];
    final sexualHealthItems = <String>[];

    if (surveyProfile != null) {
      // 1. BMI & Age
      final bmi = surveyProfile['bmi'] as Map?;
      final age = surveyProfile['age'] as Map?;
      if (bmi != null) {
        if (bmi['heightCm'] != null) {
          bmiItems.add('Chiều cao: ${bmi['heightCm']} cm');
        }
        if (bmi['weightKg'] != null) {
          final ctx = bmi['weightContext'] == 'PRE_PREGNANCY'
              ? ' (Trước mang thai)'
              : '';
          bmiItems.add('Cân nặng: ${bmi['weightKg']} kg$ctx');
        }
        final cat = surveyDerived?['bmiCategory'] ?? bmi['bmiCategory'];
        if (cat != null) {
          bmiItems.add('Thể trạng: ${translateCode(cat.toString())}');
        }
        if (bmi['calculatedBmi'] != null) {
          bmiItems.add('BMI: ${bmi['calculatedBmi']}');
        }
      }
      if (age != null) {
        final ageGroup = age['ageGroup']?.toString();
        if (ageGroup != null) {
          bmiItems.add('Nhóm tuổi: ${translateCode(ageGroup)}');
        } else if (age['dateOfBirth'] != null) {
          bmiItems.add('Ngày sinh: ${age['dateOfBirth']}');
        }
      }

      // 2. Reproductive History
      final repro = surveyProfile['reproductiveHistory'] as Map?;
      if (repro?['conditionCodes'] is List) {
        for (final c in repro!['conditionCodes']) {
          final s = c.toString();
          if (s != 'NONE_KNOWN' && s != 'NO_LISTED_REPRODUCTIVE_HISTORY') {
            reproItems.add(translateCode(s));
          }
        }
      }

      // 3. Underlying conditions
      final underlying = surveyProfile['underlyingConditions'] as Map?;
      if (underlying?['conditionCodes'] is List) {
        for (final c in underlying!['conditionCodes']) {
          final s = c.toString();
          if (s != 'NONE_KNOWN') {
            conditionItems.add(translateCode(s));
          }
        }
      }

      // 4. Lifestyle
      final lifestyle = surveyProfile['lifestyle'] as Map?;
      if (lifestyle != null) {
        final smoking = lifestyle['smoking'] is Map
            ? lifestyle['smoking']['answer']
            : lifestyle['smokingStatus'];
        if (smoking != null &&
            smoking != 'NEVER' &&
            smoking != 'UNKNOWN' &&
            smoking != 'NONE') {
          lifestyleItems.add('Hút thuốc: ${translateCode(smoking.toString())}');
        }
        final alcohol = lifestyle['alcohol'] is Map
            ? lifestyle['alcohol']['answer']
            : lifestyle['alcoholUse'];
        if (alcohol != null && alcohol != 'NONE' && alcohol != 'UNKNOWN') {
          lifestyleItems.add('Rượu bia: ${translateCode(alcohol.toString())}');
        }
        final activity = lifestyle['physicalActivity'] is Map
            ? lifestyle['physicalActivity']['answer']
            : lifestyle['physicalActivityLevel'];
        if (activity != null && activity != 'UNKNOWN') {
          lifestyleItems.add('Vận động: ${translateCode(activity.toString())}');
        }
        final sleep = lifestyle['sleep'] is Map
            ? lifestyle['sleep']['answer']
            : lifestyle['sleepConcern'];
        if (sleep != null && sleep == 'CONCERN') {
          lifestyleItems.add('Có lo lắng về giấc ngủ');
        }
        if (lifestyle['flags'] is List) {
          for (final f in lifestyle['flags']) {
            if (f != 'NONE_KNOWN_LIFESTYLE') {
              lifestyleItems.add(translateCode(f.toString()));
            }
          }
        }
      }

      // 5. Nutrition
      final nutrition = surveyProfile['nutrition'] as Map?;
      if (nutrition?['conditionCodes'] is List) {
        for (final c in nutrition!['conditionCodes']) {
          final s = c.toString();
          if (s != 'NO_CURRENT_CONCERN') {
            nutritionItems.add(translateCode(s));
          }
        }
      }

      // 6. Vaccination
      final vaccination = surveyProfile['vaccination'] as Map?;
      if (vaccination != null) {
        if (vaccination['flags'] is List) {
          for (final f in vaccination['flags']) {
            if (f != 'NONE_KNOWN_VACCINATION') {
              vaccinationItems.add(translateCode(f.toString()));
            }
          }
        }
        if (vaccination['answers'] is List) {
          for (final a in vaccination['answers']) {
            if (a is Map &&
                a['code'] != null &&
                a['status'] != null &&
                a['state'] == 'KNOWN') {
              vaccinationItems.add(
                '${translateCode(a['code'].toString())}: ${translateCode(a['status'].toString())}',
              );
            }
          }
        }
      }

      // 7. Medications
      final meds = surveyProfile['currentMedications'] as Map?;
      if (meds?['conditionCodes'] is List) {
        for (final m in meds!['conditionCodes']) {
          final s = m.toString();
          if (s != 'NONE_KNOWN_MEDICATION') {
            medicationItems.add(translateCode(s));
          }
        }
      }

      // 8. Sexual Health & STI
      final sexHealth = surveyProfile['sexualHealth'] as Map?;
      if (sexHealth?['conditionCodes'] is List) {
        for (final s in sexHealth!['conditionCodes']) {
          final code = s.toString();
          if (code != 'NO_CURRENT_INFORMATION_NEED') {
            sexualHealthItems.add(translateCode(code));
          }
        }
      }
      final sti = surveyProfile['sti'] as Map?;
      if (sti != null &&
          sti['status'] != null &&
          sti['status'] != 'NO_KNOWN_HISTORY') {
        sexualHealthItems.add(
          'STIs: ${translateCode(sti['status'].toString())}',
        );
      }
    }

    // Merge fallback surveyRisks if surveyProfile was null
    if (surveyProfile == null && surveyRisks.isNotEmpty) {
      for (final r in surveyRisks) {
        conditionItems.add(translateCode(r));
      }
    }

    final hasAnySurveyData =
        bmiItems.isNotEmpty ||
        reproItems.isNotEmpty ||
        conditionItems.isNotEmpty ||
        lifestyleItems.isNotEmpty ||
        nutritionItems.isNotEmpty ||
        vaccinationItems.isNotEmpty ||
        medicationItems.isNotEmpty ||
        sexualHealthItems.isNotEmpty;

    final maxHeight = MediaQuery.of(context).size.height * 0.82;
    return Container(
      height: maxHeight,
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      child: SafeArea(
        top: false,
        child: Column(
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
            Expanded(
              child: ListView(
                padding: const EdgeInsets.all(20),
                children: [
                  // Thông tin Giai đoạn / Tuần thai
                  () {
                    final st =
                        contextData['stage'] ?? contextData['journeyType'];
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
                                  if (getTrimester(
                                    gestationalAge,
                                  ).isNotEmpty) ...[
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
                              color: const Color(
                                0xFF845143,
                              ).withValues(alpha: 0.1),
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

                  // Tiền sử & Khảo sát cá nhân hóa (Survey)
                  const Text(
                    'Tiền sử & Bệnh nền (từ Khảo sát Onboarding)',
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.bold,
                      color: Color(0xFF555555),
                    ),
                  ),
                  const SizedBox(height: 8),
                  if (hasAnySurveyData) ...[
                    buildCategoryBlock(
                      icon: Icons.accessibility_new_rounded,
                      title: 'Thể trạng & Chỉ số nhân trắc',
                      items: bmiItems,
                      badgeBg: const Color(0xFFF3E5F5),
                      badgeBorder: const Color(0xFFE1BEE7),
                      badgeText: const Color(0xFF6A1B9A),
                    ),
                    buildCategoryBlock(
                      icon: Icons.pregnant_woman_rounded,
                      title: 'Tiền sử sản khoa',
                      items: reproItems,
                      badgeBg: const Color(0xFFFFF3E0),
                      badgeBorder: const Color(0xFFFFCC80),
                      badgeText: const Color(0xFFE65100),
                    ),
                    buildCategoryBlock(
                      icon: Icons.medical_services_outlined,
                      title: 'Bệnh lý nền & Mạn tính',
                      items: conditionItems,
                      badgeBg: const Color(0xFFFFEBEE),
                      badgeBorder: const Color(0xFFFFCDD2),
                      badgeText: const Color(0xFFC62828),
                    ),
                    buildCategoryBlock(
                      icon: Icons.self_improvement_rounded,
                      title: 'Lối sống & Thói quen',
                      items: lifestyleItems,
                      badgeBg: const Color(0xFFE8F5E9),
                      badgeBorder: const Color(0xFFC8E6C9),
                      badgeText: const Color(0xFF2E7D32),
                    ),
                    buildCategoryBlock(
                      icon: Icons.restaurant_rounded,
                      title: 'Dinh dưỡng & Vi chất',
                      items: nutritionItems,
                      badgeBg: const Color(0xFFFFFDE7),
                      badgeBorder: const Color(0xFFFFF59D),
                      badgeText: const Color(0xFFF57F17),
                    ),
                    buildCategoryBlock(
                      icon: Icons.vaccines_rounded,
                      title: 'Tiêm chủng & Miễn dịch',
                      items: vaccinationItems,
                      badgeBg: const Color(0xFFE0F7FA),
                      badgeBorder: const Color(0xFFB2EBF2),
                      badgeText: const Color(0xFF00838F),
                    ),
                    buildCategoryBlock(
                      icon: Icons.medication_rounded,
                      title: 'Thuốc đang sử dụng',
                      items: medicationItems,
                      badgeBg: const Color(0xFFEDE7F6),
                      badgeBorder: const Color(0xFFD1C4E9),
                      badgeText: const Color(0xFF4527A0),
                    ),
                    buildCategoryBlock(
                      icon: Icons.favorite_border_rounded,
                      title: 'Sức khỏe sinh sản & STIs',
                      items: sexualHealthItems,
                      badgeBg: const Color(0xFFFCE4EC),
                      badgeBorder: const Color(0xFFF8BBD0),
                      badgeText: const Color(0xFFAD1457),
                    ),
                  ] else if (surveyStatus == 'NOT_STARTED')
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: const Color(0xFFFBF4EE),
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: const Color(0xFFE8DFD8)),
                      ),
                      child: const Row(
                        children: [
                          Icon(
                            Icons.assignment_outlined,
                            size: 18,
                            color: Color(0xFF845143),
                          ),
                          SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              'Chưa có dữ liệu khảo sát cá nhân hóa • Bạn có thể thực hiện khảo sát tại phần Cài đặt / Khảo sát để AI hỗ trợ chính xác hơn',
                              style: TextStyle(
                                fontSize: 12,
                                color: Color(0xFF6B4F46),
                                fontWeight: FontWeight.w500,
                              ),
                            ),
                          ),
                        ],
                      ),
                    )
                  else
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.symmetric(
                        horizontal: 12,
                        vertical: 10,
                      ),
                      decoration: BoxDecoration(
                        color: const Color(0xFFF1F8E9),
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: const Color(0xFFC8E6C9)),
                      ),
                      child: const Row(
                        children: [
                          Icon(
                            Icons.check_circle_outline_rounded,
                            size: 18,
                            color: Color(0xFF2E7D32),
                          ),
                          SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              'Đã ghi nhận khảo sát • Không có tiền sử bệnh lý nguy cơ (Huyết áp mạn, Tiền sản giật, ĐTĐ)',
                              style: TextStyle(
                                fontSize: 12,
                                color: Color(0xFF1B5E20),
                                fontWeight: FontWeight.w500,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  const SizedBox(height: 16),

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
                                  Expanded(
                                    child: Text(
                                      e.key.toString(),
                                      style: const TextStyle(
                                        fontSize: 13,
                                        color: Color(0xFF666666),
                                      ),
                                    ),
                                  ),
                                  const SizedBox(width: 12),
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
