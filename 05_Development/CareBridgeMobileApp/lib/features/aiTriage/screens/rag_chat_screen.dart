import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';

class _Message {
  final String text;
  final bool isUser;
  final DateTime time;
  _Message({required this.text, required this.isUser, required this.time});
}

class RagChatScreen extends StatefulWidget {
  const RagChatScreen({super.key});

  @override
  State<RagChatScreen> createState() => _RagChatScreenState();
}

class _RagChatScreenState extends State<RagChatScreen> {
  static const _primary = Color(0xFFC98C7B);

  final _inputCtrl = TextEditingController();
  final _scrollCtrl = ScrollController();
  final List<_Message> _messages = [];
  bool _sending = false;

  @override
  void dispose() {
    _inputCtrl.dispose();
    _scrollCtrl.dispose();
    super.dispose();
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

  Future<void> _send() async {
    final question = _inputCtrl.text.trim();
    if (question.isEmpty || _sending) return;

    setState(() {
      _messages.add(_Message(text: question, isUser: true, time: DateTime.now()));
      _sending = true;
    });
    _inputCtrl.clear();
    _scrollToBottom();

    try {
      final data = await apiPost('/api/v1/rag/answer', {'question': question});
      final answer = data['answer'] as String? ?? data.toString();
      setState(() {
        _messages.add(_Message(text: answer, isUser: false, time: DateTime.now()));
        _sending = false;
      });
    } catch (e) {
      setState(() {
        _messages.add(_Message(
          text: 'Xin lỗi, tôi không thể trả lời lúc này. Vui lòng thử lại.',
          isUser: false,
          time: DateTime.now(),
        ));
        _sending = false;
      });
    }
    _scrollToBottom();
  }

  String _formatTime(DateTime dt) {
    final h = dt.hour.toString().padLeft(2, '0');
    final m = dt.minute.toString().padLeft(2, '0');
    return '$h:$m';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF6F1EC),
      appBar: AppBar(
        backgroundColor: _primary,
        foregroundColor: Colors.white,
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: const [
            Text('CareBridge AI', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
            Text('Trợ lý sức khoẻ', style: TextStyle(fontSize: 11, color: Colors.white70)),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.info_outline),
            tooltip: 'Phản hồi AI chỉ mang tính tham khảo',
            onPressed: () => showDialog(
              context: context,
              builder: (_) => AlertDialog(
                title: const Text('Về Trợ lý AI'),
                content: const Text(
                    'CareBridge AI chỉ cung cấp thông tin tham khảo, không thay thế tư vấn y tế chuyên nghiệp, chẩn đoán hoặc điều trị. Trong trường hợp khẩn cấp, hãy liên hệ ngay với dịch vụ cấp cứu.'),
                actions: [
                  TextButton(
                    onPressed: () => Navigator.of(context).pop(),
                    child: const Text('Đồng ý', style: TextStyle(color: Color(0xFFC98C7B))),
                  ),
                ],
              ),
            ),
          ),
        ],
        elevation: 0,
      ),
      body: Column(
        children: [
          if (_messages.isEmpty)
            Expanded(child: _WelcomeView()),
          if (_messages.isNotEmpty)
            Expanded(
              child: ListView.builder(
                controller: _scrollCtrl,
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 16),
                itemCount: _messages.length + (_sending ? 1 : 0),
                itemBuilder: (ctx, i) {
                  if (i == _messages.length) {
                    return const _TypingIndicator();
                  }
                  return _MessageBubble(message: _messages[i], formatTime: _formatTime);
                },
              ),
            ),
          _InputBar(
            controller: _inputCtrl,
            sending: _sending,
            onSend: _send,
          ),
        ],
      ),
    );
  }
}

class _WelcomeView extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 80,
              height: 80,
              decoration: BoxDecoration(
                color: const Color(0xFFC98C7B).withValues(alpha: 0.1),
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.health_and_safety, size: 40, color: Color(0xFFC98C7B)),
            ),
            const SizedBox(height: 20),
            const Text('Trợ lý AI CareBridge',
                style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Color(0xFFC98C7B))),
            const SizedBox(height: 12),
            const Text(
              'Hỏi tôi bất cứ điều gì về sức khoẻ mẹ bầu, thai kỳ, chăm sóc sau sinh hoặc sự phát triển của bé.',
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.black54, fontSize: 14),
            ),
            const SizedBox(height: 8),
            const Text(
              'Lưu ý: Phản hồi AI chỉ mang tính tham khảo, không thay thế tư vấn y tế.',
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.grey, fontSize: 12, fontStyle: FontStyle.italic),
            ),
          ],
        ),
      ),
    );
  }
}

class _MessageBubble extends StatelessWidget {
  final _Message message;
  final String Function(DateTime) formatTime;
  const _MessageBubble({required this.message, required this.formatTime});

  @override
  Widget build(BuildContext context) {
    final isUser = message.isUser;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: isUser ? MainAxisAlignment.end : MainAxisAlignment.start,
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          if (!isUser) ...[
            CircleAvatar(
              radius: 16,
              backgroundColor: const Color(0xFFC98C7B).withValues(alpha: 0.15),
              child: const Icon(Icons.health_and_safety, size: 16, color: Color(0xFFC98C7B)),
            ),
            const SizedBox(width: 8),
          ],
          Flexible(
            child: Container(
              constraints: BoxConstraints(
                maxWidth: MediaQuery.of(context).size.width * 0.72,
              ),
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
              decoration: BoxDecoration(
                color: isUser ? const Color(0xFFC98C7B) : Colors.white,
                borderRadius: BorderRadius.only(
                  topLeft: const Radius.circular(16),
                  topRight: const Radius.circular(16),
                  bottomLeft: Radius.circular(isUser ? 16 : 4),
                  bottomRight: Radius.circular(isUser ? 4 : 16),
                ),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withValues(alpha: 0.06),
                    blurRadius: 4,
                    offset: const Offset(0, 2),
                  ),
                ],
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    message.text,
                    style: TextStyle(
                      color: isUser ? Colors.white : Colors.black87,
                      fontSize: 14,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    formatTime(message.time),
                    style: TextStyle(
                      color: isUser ? Colors.white60 : Colors.grey,
                      fontSize: 10,
                    ),
                  ),
                ],
              ),
            ),
          ),
          if (isUser) const SizedBox(width: 8),
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
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          CircleAvatar(
            radius: 16,
            backgroundColor: const Color(0xFFC98C7B).withValues(alpha: 0.15),
            child: const Icon(Icons.health_and_safety, size: 16, color: Color(0xFFC98C7B)),
          ),
          const SizedBox(width: 8),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(16),
              boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.06), blurRadius: 4)],
            ),
            child: const SizedBox(
              width: 40,
              child: LinearProgressIndicator(
                color: Color(0xFFC98C7B),
                backgroundColor: Color(0xFFF2EAE4),
              ),
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
  const _InputBar({required this.controller, required this.sending, required this.onSend});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(12, 8, 12, 16),
      decoration: BoxDecoration(
        color: Colors.white,
        boxShadow: [
          BoxShadow(color: Colors.black.withValues(alpha: 0.08), blurRadius: 8, offset: const Offset(0, -2)),
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
                textInputAction: TextInputAction.newline,
                decoration: InputDecoration(
                  hintText: 'Đặt câu hỏi sức khoẻ...',
                  filled: true,
                  fillColor: const Color(0xFFF6F1EC),
                  contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
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
                  color: sending ? Colors.grey.shade300 : const Color(0xFFC98C7B),
                  shape: BoxShape.circle,
                ),
                child: sending
                    ? const Padding(
                        padding: EdgeInsets.all(10),
                        child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2))
                    : const Icon(Icons.send, color: Colors.white, size: 20),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
