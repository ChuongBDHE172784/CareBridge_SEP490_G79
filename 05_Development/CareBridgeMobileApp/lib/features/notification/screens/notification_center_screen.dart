import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';
import '../models/notification_model.dart';
import '../services/notification_service.dart';
import 'notification_detail_screen.dart';

class NotificationCenterScreen extends StatefulWidget {
  const NotificationCenterScreen({super.key});

  @override
  State<NotificationCenterScreen> createState() =>
      _NotificationCenterScreenState();
}

class _NotificationCenterScreenState extends State<NotificationCenterScreen> {
  static const _canvas = Color(0xFFF6F1EC);
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);
  static const _surfaceLowest = Color(0xFFFFFFFF);
  static const _surfaceContainerHigh = Color(0xFFFFE2D9);
  static const _surfaceContainerLow = Color(0xFFFFF1EC);
  static const _secondaryContainer = Color(0xFFF6DACF);
  static const _onSecondaryContainer = Color(0xFF735E56);

  bool _showUnreadOnly = false;
  List<NotificationRecord> _all = [];
  bool _isLoading = true;
  String? _error;

  List<NotificationRecord> get _displayed =>
      _showUnreadOnly ? _all.where((n) => n.isUnread).toList() : _all;

  int get _unreadCount => _all.where((n) => n.isUnread).length;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final result = await NotificationService.instance.getNotifications();
      if (!mounted) return;
      setState(() {
        _all = result;
        _isLoading = false;
      });
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() {
        _error = 'Lỗi tải thông báo (${e.statusCode})';
        _isLoading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _error = 'Không thể kết nối. Vui lòng thử lại.';
        _isLoading = false;
      });
    }
  }

  Future<void> _markAsRead(NotificationRecord n) async {
    if (!n.isUnread) return;
    try {
      await NotificationService.instance.markAsRead(n.id);
      await _load();
    } catch (_) {}
  }

  Future<void> _markAllAsRead() async {
    try {
      await NotificationService.instance.markAllAsRead();
      await _load();
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvas,
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: _isLoading
                  ? const Center(
                      child: CircularProgressIndicator(color: _primaryContainer))
                  : _error != null
                      ? _buildError()
                      : RefreshIndicator(
                          color: _primaryContainer,
                          onRefresh: _load,
                          child: CustomScrollView(
                            slivers: [
                              SliverToBoxAdapter(child: _buildHeader()),
                              SliverToBoxAdapter(child: _buildFilters()),
                              if (_displayed.isEmpty)
                                SliverFillRemaining(
                                    child: _buildEmpty())
                              else
                                SliverList(
                                  delegate: SliverChildBuilderDelegate(
                                    (_, i) => Padding(
                                      padding: const EdgeInsets.fromLTRB(
                                          16, 0, 16, 8),
                                      child: _buildCard(_displayed[i]),
                                    ),
                                    childCount: _displayed.length,
                                  ),
                                ),
                              const SliverToBoxAdapter(
                                  child: SizedBox(height: 24)),
                            ],
                          ),
                        ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 24, 24, 0),
      child: Row(
        children: [
          const Expanded(
            child: Text(
              'Thông báo của bạn',
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 20,
                fontWeight: FontWeight.w600,
                color: _onSurface,
              ),
            ),
          ),
          if (_unreadCount > 0)
            TextButton(
              onPressed: _markAllAsRead,
              child: const Text(
                'Đọc tất cả',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 12,
                  fontWeight: FontWeight.w500,
                  color: _primary,
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildFilters() {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 16, 24, 16),
      child: Row(
        children: [
          _chip(
            label: 'Tất cả',
            active: !_showUnreadOnly,
            onTap: () => setState(() => _showUnreadOnly = false),
          ),
          const SizedBox(width: 8),
          _chip(
            label: _unreadCount > 0
                ? 'Chưa đọc ($_unreadCount)'
                : 'Chưa đọc',
            active: _showUnreadOnly,
            onTap: () => setState(() => _showUnreadOnly = true),
          ),
        ],
      ),
    );
  }

  Widget _chip({
    required String label,
    required bool active,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 150),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
        decoration: BoxDecoration(
          color: active ? _primaryContainer : _surfaceLowest,
          borderRadius: BorderRadius.circular(9999),
          border: active ? null : Border.all(color: _outlineVariant),
          boxShadow: [
            BoxShadow(
              color: const Color.fromRGBO(90, 70, 63, 0.06),
              blurRadius: 20,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Text(
          label,
          style: TextStyle(
            fontFamily: 'Lexend',
            fontSize: 16,
            fontWeight: FontWeight.w600,
            color: active ? Colors.white : _onSurfaceVariant,
          ),
        ),
      ),
    );
  }

  Widget _buildCard(NotificationRecord n) {
    final isUnread = n.isUnread;
    return GestureDetector(
      onTap: () async {
        await _markAsRead(n);
        if (!mounted) return;
        await Navigator.of(context).push(
          MaterialPageRoute(
              builder: (_) => NotificationDetailScreen(notification: n)),
        );
        _load();
      },
      child: Opacity(
        opacity: isUnread ? 1.0 : 0.8,
        child: Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: _surfaceLowest,
            borderRadius: BorderRadius.circular(24),
            border: isUnread
                ? Border(
                    left: BorderSide(color: _primaryContainer, width: 4))
                : null,
            boxShadow: [
              BoxShadow(
                color: Color.fromRGBO(90, 70, 63, isUnread ? 0.06 : 0.04),
                blurRadius: 20,
                offset: const Offset(0, 4),
              ),
            ],
          ),
          child: Stack(
            children: [
              if (isUnread)
                Positioned(
                  top: 0,
                  right: 0,
                  child: Container(
                    width: 8,
                    height: 8,
                    decoration: const BoxDecoration(
                      color: _primaryContainer,
                      shape: BoxShape.circle,
                    ),
                  ),
                ),
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _buildIcon(n),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Expanded(
                              child: Text(
                                n.title,
                                maxLines: 2,
                                overflow: TextOverflow.ellipsis,
                                style: const TextStyle(
                                  fontFamily: 'Lexend',
                                  fontSize: 16,
                                  fontWeight: FontWeight.w600,
                                  color: _onSurface,
                                ),
                              ),
                            ),
                            const SizedBox(width: 8),
                            Text(
                              _relativeTime(n.createdAt),
                              style: const TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 12,
                                fontWeight: FontWeight.w500,
                                letterSpacing: 0.6,
                                color: _onSurfaceVariant,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 4),
                        Text(
                          n.body,
                          maxLines: 3,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 14,
                            fontWeight: FontWeight.w400,
                            height: 1.5,
                            color: _onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildIcon(NotificationRecord n) {
    final type = n.type.toUpperCase();
    final isUnread = n.isUnread;

    IconData icon;
    Color bgColor;
    Color iconColor;

    switch (type) {
      case 'VACCINATION':
      case 'REMINDER':
        icon = Icons.vaccines_outlined;
        bgColor = isUnread ? _surfaceContainerHigh : _surfaceContainerLow;
        iconColor = isUnread ? _primary : _onSurfaceVariant;
        break;
      case 'APPOINTMENT':
        icon = Icons.event_outlined;
        bgColor = isUnread ? _secondaryContainer : _surfaceContainerLow;
        iconColor = isUnread ? _onSecondaryContainer : _onSurfaceVariant;
        break;
      case 'COMMUNITY':
      case 'MESSAGE':
        icon = Icons.forum_outlined;
        bgColor = _surfaceContainerLow;
        iconColor = _onSurfaceVariant;
        break;
      case 'SYSTEM':
        icon = Icons.update_outlined;
        bgColor = _surfaceContainerLow;
        iconColor = _onSurfaceVariant;
        break;
      default:
        icon = Icons.notifications_outlined;
        bgColor = isUnread ? _surfaceContainerHigh : _surfaceContainerLow;
        iconColor = isUnread ? _primary : _onSurfaceVariant;
    }

    return Container(
      width: 48,
      height: 48,
      decoration: BoxDecoration(shape: BoxShape.circle, color: bgColor),
      child: Icon(icon, color: iconColor, size: 22),
    );
  }

  Widget _buildEmpty() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.notifications_none_rounded,
              color: _outlineVariant, size: 64),
          const SizedBox(height: 16),
          Text(
            _showUnreadOnly
                ? 'Không có thông báo chưa đọc'
                : 'Chưa có thông báo nào',
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 16,
              color: _onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildError() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, color: _primary, size: 48),
            const SizedBox(height: 16),
            Text(_error!,
                textAlign: TextAlign.center,
                style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    color: _onSurfaceVariant)),
            const SizedBox(height: 16),
            FilledButton(
              onPressed: _load,
              style: FilledButton.styleFrom(
                  backgroundColor: _primaryContainer,
                  shape: const StadiumBorder()),
              child: const Text('Thử lại',
                  style: TextStyle(
                      fontFamily: 'Lexend',
                      fontWeight: FontWeight.w600,
                      color: Colors.white)),
            ),
          ],
        ),
      ),
    );
  }

  String _relativeTime(DateTime dt) {
    final diff = DateTime.now().difference(dt);
    if (diff.inMinutes < 1) return 'Vừa xong';
    if (diff.inMinutes < 60) return '${diff.inMinutes} phút trước';
    if (diff.inHours < 24) return '${diff.inHours} giờ trước';
    if (diff.inDays == 1) return 'Hôm qua';
    if (diff.inDays < 7) return '${diff.inDays} ngày trước';
    return '${dt.day.toString().padLeft(2, '0')}/'
        '${dt.month.toString().padLeft(2, '0')}/'
        '${dt.year}';
  }
}
