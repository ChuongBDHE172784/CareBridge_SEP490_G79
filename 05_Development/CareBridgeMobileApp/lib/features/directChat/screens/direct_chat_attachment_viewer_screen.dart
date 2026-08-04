import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../../core/network/api_client.dart';

class DirectChatAttachmentViewerScreen extends StatefulWidget {
  const DirectChatAttachmentViewerScreen({
    super.key,
    required this.conversationId,
    required this.messageId,
  });
  final String conversationId;
  final String messageId;

  @override
  State<DirectChatAttachmentViewerScreen> createState() =>
      _DirectChatAttachmentViewerScreenState();
}

class _DirectChatAttachmentViewerScreenState
    extends State<DirectChatAttachmentViewerScreen> {
  String? _url;
  String? _name;
  String? _mimeType;
  String? _error;
  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _error = null);
    try {
      final json = await apiGet(
        '/api/v1/direct-conversations/${widget.conversationId}/messages/${widget.messageId}/attachment',
      );
      if (!mounted) return;
      setState(() {
        _url = json['data']?['presignedUrl'] as String?;
        _name = json['data']?['originalName'] as String?;
        _mimeType = json['data']?['mimeType'] as String?;
      });
    } catch (_) {
      if (mounted)
        setState(() => _error = 'Không thể mở tệp. Vui lòng thử lại.');
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Text(_name ?? 'Đang tải tệp')),
    body: Center(
      child: _error != null
          ? Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(_error!),
                const SizedBox(height: 12),
                OutlinedButton(onPressed: _load, child: const Text('Thử lại')),
              ],
            )
          : _url == null
          ? const CircularProgressIndicator()
          : Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                if ((_mimeType ?? '').startsWith('image/'))
                  ConstrainedBox(
                    constraints: const BoxConstraints(maxHeight: 520),
                    child: InteractiveViewer(
                      child: Image.network(_url!, fit: BoxFit.contain),
                    ),
                  ),
                const SizedBox(height: 16),
                FilledButton.icon(
                  icon: const Icon(Icons.download_outlined),
                  label: const Text('Mở hoặc tải xuống'),
                  onPressed: () => launchUrl(
                    Uri.parse(_url!),
                    mode: LaunchMode.externalApplication,
                  ),
                ),
              ],
            ),
    ),
  );
}
