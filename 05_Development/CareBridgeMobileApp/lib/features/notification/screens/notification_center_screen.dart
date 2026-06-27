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
  static const _canvasColor = Color(0xFFF6F1EC);
  static const _primaryColor = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);
  static const _surfaceColor = Color(0xFFFFF8F6);
  static const _surfaceContainerHigh = Color(0xFFFFE2D9);
  static const _surfaceContainerLow = Color(0xFFFFF1EC);
  static const _secondary = Color(0xFF6E5A52);

  final _filters = ['Tất cả', 'Chưa đọc', 'Hệ thống'];
  final _filterTypeMap = {
    'Tất cả': null,
    'Chưa đọc': 'UNREAD',
    'Hệ thống': 'SYSTEM',
  };

  int _selectedFilter = 0;
  List<NotificationRecord> _notifications = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadNotifications();
  }

  Future<void> _loadNotifications() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final type = _filterTypeMap[_filters[_selectedFilter]];
      final result =
          await NotificationService.instance.getNotifications(type: type);
      if (!mounted) return;
      setState(() {
        _notifications = result;
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

  void _onFilterTap(int index) {
    if (_selectedFilter == index) return;
    setState(() => _selectedFilter = index);
    _loadNotifications();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _canvasColor,
      body: SafeArea(
        child: Column(
          children: [
            _buildAppBar(),
            Expanded(
              child: _isLoading
                  ? const Center(
                      child:
                          CircularProgressIndicator(color: _primaryContainer))
                  : _error != null
                      ? _buildErrorState()
                      : _notifications.isEmpty
                          ? _buildEmptyState()
                          : _buildNotificationList(),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAppBar() {
    return Container(
      color: _canvasColor,
      height: 48,
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Row(
        children: [
          SizedBox(
            width: 48,
            height: 48,
            child: IconButton(
              onPressed: () => Navigator.of(context).pop(),
              icon: const Icon(Icons.arrow_back, color: _primaryColor),
              padding: EdgeInsets.zero,
            ),
          ),
          const Expanded(
            child: Text(
              'Thông báo',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontFamily: 'Lexend',
                fontSize: 24,
                fontWeight: FontWeight.w600,
                color: _primaryColor,
              ),
            ),
          ),
          SizedBox(
            width: 48,
            height: 48,
            child: IconButton(
              onPressed: () {
                // TODO: navigate to notification preferences (CB-113)
              },
              icon: const Icon(Icons.settings_outlined, color: _primaryColor),
              padding: EdgeInsets.zero,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildErrorState() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, color: _primaryColor, size: 48),
            const SizedBox(height: 16),
            Text(_error!,
                textAlign: TextAlign.center,
                style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 14,
                    color: _onSurfaceVariant)),
            const SizedBox(height: 16),
            FilledButton(
              onPressed: _loadNotifications,
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

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.notifications_none_rounded,
              color: _outlineVariant, size: 64),
          const SizedBox(height: 16),
          const Text('Chưa có thông báo nào',
              style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 16,
                  color: _onSurfaceVariant)),
        ],
      ),
    );
  }

  Widget _buildNotificationList() {
    return RefreshIndicator(
      color: _primaryContainer,
      onRefresh: _loadNotifications,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
        children: [
          _buildFilterRow(),
          const SizedBox(height: 16),
          ..._notifications.map(_buildNotificationCard),
        ],
      ),
    );
  }

  Widget _buildFilterRow() {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 16),
      child: Row(
        children: List.generate(_filters.length, (index) {
          final isSelected = _selectedFilter == index;
          return Padding(
            padding: EdgeInsets.only(right: index < _filters.length - 1 ? 8 : 0),
            child: GestureDetector(
              onTap: () => _onFilterTap(index),
              child: Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                decoration: BoxDecoration(
                  color: isSelected ? _primaryContainer : _surfaceColor,
                  borderRadius: BorderRadius.circular(9999),
                  border: isSelected
                      ? null
                      : Border.all(color: _outlineVariant),
                  boxShadow: [
                    BoxShadow(
                      color: Color.fromRGBO(
                          90, 70, 63, isSelected ? 0.12 : 0.06),
                      blurRadius: isSelected ? 24 : 20,
                      offset: Offset(0, isSelected ? 8 : 4),
                    ),
                  ],
                ),
                child: Text(
                  _filters[index],
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                    color: isSelected ? Colors.white : _onSurfaceVariant,
                  ),
                ),
              ),
            ),
          );
        }),
      ),
    );
  }

  Widget _buildNotificationCard(NotificationRecord notification) {
    final isUnread = notification.isUnread;

    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: GestureDetector(
        onTap: () async {
          await Navigator.of(context).push(
            MaterialPageRoute(
              builder: (_) =>
                  NotificationDetailScreen(notification: notification),
            ),
          );
          _loadNotifications();
        },
        child: Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: isUnread
                ? _surfaceColor
                : _surfaceColor.withValues(alpha: 0.6),
            borderRadius: BorderRadius.circular(24),
            border: isUnread
                ? null
                : Border.all(color: _outlineVariant.withValues(alpha: 0.3)),
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
                  left: 0,
                  top: 0,
                  bottom: 0,
                  child: Container(
                    width: 6,
                    decoration: BoxDecoration(
                      color: _primaryContainer,
                      borderRadius: BorderRadius.circular(3),
                    ),
                  ),
                ),
              Padding(
                padding: EdgeInsets.only(left: isUnread ? 14 : 0),
                child: Opacity(
                  opacity: isUnread ? 1.0 : 0.7,
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      _buildIcon(notification),
                      const SizedBox(width: 16),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Expanded(
                                  child: Text(
                                    notification.title,
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: TextStyle(
                                      fontFamily: 'Lexend',
                                      fontSize: 20,
                                      fontWeight: FontWeight.w600,
                                      color: _onSurface,
                                    ),
                                  ),
                                ),
                                const SizedBox(width: 8),
                                Text(
                                  _formatTime(notification.createdAt),
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
                              notification.body,
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(
                                fontFamily: 'Lexend',
                                fontSize: 14,
                                fontWeight: FontWeight.w400,
                                color: _onSurfaceVariant,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildIcon(NotificationRecord notification) {
    final isUnread = notification.isUnread;
    final type = notification.type.toUpperCase();

    IconData icon;
    switch (type) {
      case 'SYSTEM':
        icon = Icons.campaign;
        break;
      case 'COMMUNITY':
      case 'MESSAGE':
        icon = Icons.chat_bubble_outline;
        break;
      case 'REMINDER':
      case 'APPOINTMENT':
        icon = Icons.vaccines_outlined;
        break;
      case 'ACHIEVEMENT':
        icon = Icons.star_outline;
        break;
      default:
        icon = Icons.notifications_outlined;
    }

    return Container(
      width: 48,
      height: 48,
      decoration: BoxDecoration(
        color: isUnread ? _surfaceContainerHigh : _surfaceContainerLow,
        shape: BoxShape.circle,
      ),
      child: Icon(icon,
          color: isUnread ? _primaryColor : _secondary, size: 24),
    );
  }

  String _formatTime(DateTime dateTime) {
    final now = DateTime.now();
    final diff = now.difference(dateTime);
    if (diff.inMinutes < 1) return 'Vừa xong';
    if (diff.inMinutes < 60) return '${diff.inMinutes} phút trước';
    if (diff.inHours < 24) return '${diff.inHours} giờ trước';
    if (diff.inDays == 1) return 'Hôm qua';
    if (diff.inDays < 7) return '${diff.inDays} ngày trước';
    return '${dateTime.day.toString().padLeft(2, '0')}/${dateTime.month.toString().padLeft(2, '0')}/${dateTime.year}';
  }
}
