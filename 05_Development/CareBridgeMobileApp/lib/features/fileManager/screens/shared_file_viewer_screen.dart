import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';
import '../models/file_model.dart';
import '../services/file_service.dart';

class SharedFileViewerScreen extends StatefulWidget {
  final String fileId;
  final String? expertName;
  final String? expertAvatarUrl;
  final String? patientName;
  final String? consultationLink;
  final DateTime? accessExpiry;
  final String? accessPurpose;

  const SharedFileViewerScreen({
    super.key,
    required this.fileId,
    this.expertName,
    this.expertAvatarUrl,
    this.patientName,
    this.consultationLink,
    this.accessExpiry,
    this.accessPurpose,
  });

  @override
  State<SharedFileViewerScreen> createState() => _SharedFileViewerScreenState();
}

class _SharedFileViewerScreenState extends State<SharedFileViewerScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _canvas = Color(0xFFFFF8F6);
  static const _surface = Color(0xFFF2EAE4);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);

  final _service = FileService();
  ViewFileResponse? _file;
  bool _isLoading = true;
  String? _error;
  bool _isExpired = false;

  @override
  void initState() {
    super.initState();
    _loadFile();
  }

  Future<void> _loadFile() async {
    setState(() { _isLoading = true; _error = null; });
    try {
      final f = await _service.getFile(widget.fileId);
      final expired = widget.accessExpiry != null && widget.accessExpiry!.isBefore(DateTime.now());
      setState(() { _file = f; _isExpired = expired; });
    } catch (_) {
      setState(() => _error = 'Không thể tải tài liệu. Vui lòng thử lại.');
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _openFullscreen() async {
    final url = _file?.presignedUrl;
    if (url == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Tài liệu chưa sẵn sàng.'), backgroundColor: _primary),
      );
      return;
    }
    final uri = Uri.parse(url);
    if (await canLaunchUrl(uri)) {
      await launchUrl(uri, mode: LaunchMode.externalApplication);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      appBar: AppBar(
        backgroundColor: _canvas,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.close_rounded, color: _onSurface),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: const Text(
          'Xem tài liệu',
          style: TextStyle(fontFamily: 'Lexend', fontSize: 16, fontWeight: FontWeight.w700, color: _onSurface),
        ),
        actions: [
          if (widget.expertAvatarUrl != null || widget.expertName != null)
            Padding(
              padding: const EdgeInsets.only(right: 16),
              child: _ExpertAvatar(name: widget.expertName, avatarUrl: widget.expertAvatarUrl),
            ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: _primaryContainer))
          : _error != null
              ? _buildError()
              : SingleChildScrollView(
                  padding: const EdgeInsets.fromLTRB(16, 8, 16, 40),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      _buildPreviewCard(),
                      const SizedBox(height: 16),
                      _buildMetadataCard(),
                      const SizedBox(height: 16),
                      _buildConsentCard(),
                      const SizedBox(height: 24),
                      _buildActions(),
                    ],
                  ),
                ),
    );
  }

  Widget _buildError() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.error_outline_rounded, color: _primaryContainer, size: 64),
          const SizedBox(height: 16),
          Text(_error!, style: const TextStyle(fontFamily: 'Lexend', fontSize: 13, color: _onSurfaceVariant)),
          const SizedBox(height: 16),
          TextButton(onPressed: _loadFile, child: const Text('Thử lại', style: TextStyle(color: _primary))),
        ],
      ),
    );
  }

  Widget _buildPreviewCard() {
    final f = _file!;
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [BoxShadow(color: _primary.withAlpha(15), blurRadius: 12, offset: const Offset(0, 4))],
      ),
      child: Column(
        children: [
          Container(
            height: 200,
            decoration: BoxDecoration(
              color: _surface,
              borderRadius: const BorderRadius.vertical(top: Radius.circular(24)),
            ),
            child: Stack(
              children: [
                Center(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Stack(
                        alignment: Alignment.center,
                        children: [
                          Icon(
                            f.isPdf ? Icons.picture_as_pdf_rounded : Icons.image_rounded,
                            color: _primaryContainer.withAlpha(60),
                            size: 80,
                          ),
                          Container(
                            width: 52,
                            height: 52,
                            decoration: BoxDecoration(
                              color: Colors.white,
                              shape: BoxShape.circle,
                              boxShadow: [BoxShadow(color: Colors.black.withAlpha(20), blurRadius: 8)],
                            ),
                            child: const Icon(Icons.lock_rounded, color: _primaryContainer, size: 24),
                          ),
                        ],
                      ),
                      const SizedBox(height: 12),
                      Text(
                        f.originalName,
                        style: const TextStyle(fontFamily: 'Lexend', fontSize: 14, fontWeight: FontWeight.w600, color: _onSurface),
                        textAlign: TextAlign.center,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: 4),
                      const Text(
                        'Bản xem trước được bảo vệ',
                        style: TextStyle(fontFamily: 'Lexend', fontSize: 11, color: _onSurfaceVariant),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(14),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
              decoration: BoxDecoration(color: _surface, borderRadius: BorderRadius.circular(50)),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(f.isPdf ? Icons.picture_as_pdf_rounded : Icons.image_rounded, color: _primary, size: 16),
                  const SizedBox(width: 6),
                  Text(f.sizeLabel, style: const TextStyle(fontFamily: 'Lexend', fontSize: 12, fontWeight: FontWeight.w600, color: _onSurface)),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMetadataCard() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        boxShadow: [BoxShadow(color: _primary.withAlpha(15), blurRadius: 12, offset: const Offset(0, 4))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Thông tin tài liệu', style: TextStyle(fontFamily: 'Lexend', fontSize: 13, fontWeight: FontWeight.w600, color: _onSurface)),
          const SizedBox(height: 14),
          if (widget.patientName != null) _infoRow(Icons.person_rounded, 'Bệnh nhân', widget.patientName!),
          if (widget.expertName != null) ...[
            const SizedBox(height: 10),
            _infoRow(Icons.support_agent_rounded, 'Nguồn cung cấp', widget.expertName!),
          ],
          if (widget.consultationLink != null) ...[
            const SizedBox(height: 10),
            _infoRow(Icons.link_rounded, 'Liên kết tư vấn', widget.consultationLink!, isLink: true),
          ],
        ],
      ),
    );
  }

  Widget _infoRow(IconData icon, String label, String value, {bool isLink = false}) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, size: 16, color: _primaryContainer),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(label, style: const TextStyle(fontFamily: 'Lexend', fontSize: 11, color: _onSurfaceVariant)),
              Text(
                value,
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: isLink ? _primary : _onSurface,
                  decoration: isLink ? TextDecoration.underline : null,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildConsentCard() {
    final expiry = widget.accessExpiry;
    final isExpired = _isExpired;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(
          color: isExpired ? Colors.red.withAlpha(80) : _primaryContainer.withAlpha(60),
          width: 1.5,
        ),
        boxShadow: [BoxShadow(color: _primary.withAlpha(12), blurRadius: 10, offset: const Offset(0, 3))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.verified_user_rounded, color: isExpired ? Colors.red : _primary, size: 18),
              const SizedBox(width: 8),
              Text(
                'Thông tin quyền truy cập',
                style: TextStyle(fontFamily: 'Lexend', fontSize: 13, fontWeight: FontWeight.w600, color: isExpired ? Colors.red : _onSurface),
              ),
            ],
          ),
          const SizedBox(height: 14),
          _consentRow('Quyền truy cập', isExpired ? 'Đã hết hạn' : 'Có thời hạn', isExpired ? Colors.red : const Color(0xFF4CAF50)),
          const SizedBox(height: 10),
          if (widget.accessPurpose != null) ...[
            _consentRow('Mục đích', widget.accessPurpose!, _onSurface),
            const SizedBox(height: 10),
          ],
          if (expiry != null)
            _consentRow(
              'Hết hạn',
              '${expiry.day.toString().padLeft(2, '0')}/${expiry.month.toString().padLeft(2, '0')}/${expiry.year}',
              isExpired ? Colors.red : _onSurfaceVariant,
            ),
        ],
      ),
    );
  }

  Widget _consentRow(String label, String value, Color valueColor) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label, style: const TextStyle(fontFamily: 'Lexend', fontSize: 12, color: _onSurfaceVariant)),
        Text(value, style: TextStyle(fontFamily: 'Lexend', fontSize: 12, fontWeight: FontWeight.w600, color: valueColor)),
      ],
    );
  }

  Widget _buildActions() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        ElevatedButton.icon(
          onPressed: _isExpired ? null : _openFullscreen,
          icon: const Icon(Icons.fullscreen_rounded, size: 20),
          label: const Text('Xem toàn màn hình', style: TextStyle(fontFamily: 'Lexend', fontSize: 15, fontWeight: FontWeight.w700)),
          style: ElevatedButton.styleFrom(
            backgroundColor: _primary,
            foregroundColor: Colors.white,
            disabledBackgroundColor: _surface,
            padding: const EdgeInsets.symmetric(vertical: 14),
            shape: const StadiumBorder(),
            elevation: 0,
          ),
        ),
        const SizedBox(height: 12),
        OutlinedButton.icon(
          onPressed: () {},
          icon: const Icon(Icons.support_agent_rounded, size: 18),
          label: const Text('Liên hệ người quản lý', style: TextStyle(fontFamily: 'Lexend', fontSize: 15, fontWeight: FontWeight.w600)),
          style: OutlinedButton.styleFrom(
            foregroundColor: _primary,
            side: const BorderSide(color: _primaryContainer, width: 1.5),
            padding: const EdgeInsets.symmetric(vertical: 14),
            shape: const StadiumBorder(),
          ),
        ),
      ],
    );
  }
}

class _ExpertAvatar extends StatelessWidget {
  const _ExpertAvatar({this.name, this.avatarUrl});

  final String? name;
  final String? avatarUrl;

  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surface = Color(0xFFF2EAE4);
  static const _onSurface = Color(0xFF271812);

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        CircleAvatar(
          radius: 18,
          backgroundColor: _surface,
          backgroundImage: avatarUrl != null ? NetworkImage(avatarUrl!) : null,
          child: avatarUrl == null ? const Icon(Icons.support_agent_rounded, color: _primaryContainer, size: 20) : null,
        ),
        if (name != null) ...[
          const SizedBox(width: 6),
          Text(
            name!,
            style: const TextStyle(fontFamily: 'Lexend', fontSize: 12, fontWeight: FontWeight.w600, color: _onSurface),
          ),
        ],
      ],
    );
  }
}
