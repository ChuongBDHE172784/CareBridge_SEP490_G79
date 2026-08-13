import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../core/network/api_client.dart';
import '../models/notification_model.dart';
import '../services/notification_service.dart';
import 'notification_detail_screen.dart';
import '../../familySync/screens/care_group_invitation_screen.dart';
import '../../familySync/models/care_group_model.dart';
import '../routing/consultation_notification_routing.dart';
import '../notification_type_display.dart';

class NotificationsScreen extends StatefulWidget {
  const NotificationsScreen({super.key});

  @override
  State<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends State<NotificationsScreen> {
  static const _bgColor = Color(0xFFFFF8F6);
  static const _primaryColor = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _outlineVariant = Color(0xFFD6C2BD);
  static const _surfaceContainerLowest = Color(0xFFFFFFFF);
  static const _surfaceVariant = Color(0xFFFADCD3);
  static const _errorContainer = Color(0xFFFFDAD6);
  static const _onErrorContainer = Color(0xFF93000A);
  static const _primaryFixed = Color(0xFFFFDBD1);
  static const _onPrimaryFixed = Color(0xFF341006);

  final _filters = ['Tất cả', 'Chưa đọc', 'Sức khỏe', 'Lịch hẹn'];
  final _filterTypeMap = {
    'Tất cả': null,
    'Chưa đọc': 'UNREAD',
    'Sức khỏe': 'HEALTH',
    'Lịch hẹn': 'APPOINTMENT',
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
      final result = await NotificationService.instance.getNotifications(
        type: type,
      );
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

  Future<void> _markAllAsRead() async {
    await NotificationService.instance.markAllAsRead();
    _loadNotifications();
  }

  void _onFilterTap(int index) {
    if (_selectedFilter == index) return;
    setState(() => _selectedFilter = index);
    _loadNotifications();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _bgColor,
      body: SafeArea(
        child: Column(
          children: [
            _buildAppBar(),
            Expanded(
              child: _isLoading
                  ? const Center(
                      child: CircularProgressIndicator(
                        color: _primaryContainer,
                      ),
                    )
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
      color: _bgColor,
      padding: const EdgeInsets.symmetric(horizontal: 24),
      height: 48,
      child: Row(
        children: [
          SizedBox(
            width: 48,
            height: 48,
            child: IconButton(
              onPressed: () => Navigator.of(context).pop(),
              icon: const Icon(Icons.arrow_back, color: _onSurface),
              padding: EdgeInsets.zero,
            ),
          ),
          const SizedBox(width: 12),
          Text(
            'Thông báo',
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 24,
              fontWeight: FontWeight.w600,
              color: _primaryColor,
            ),
          ),
          const Spacer(),
          GestureDetector(
            onTap: _markAllAsRead,
            child: Text(
              'Đánh dấu đã đọc tất cả',
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 12,
                fontWeight: FontWeight.w500,
                color: _primaryColor,
                letterSpacing: 0.6,
              ),
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
            const Icon(Icons.error_outline, color: _onErrorContainer, size: 48),
            const SizedBox(height: 16),
            Text(
              _error!,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 14,
                color: _onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 16),
            FilledButton(
              onPressed: _loadNotifications,
              style: FilledButton.styleFrom(
                backgroundColor: _primaryContainer,
                shape: const StadiumBorder(),
              ),
              child: const Text(
                'Thử lại',
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontWeight: FontWeight.w600,
                  color: Colors.white,
                ),
              ),
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
          Icon(
            Icons.notifications_none_rounded,
            color: _outlineVariant,
            size: 64,
          ),
          const SizedBox(height: 16),
          const Text(
            'Chưa có thông báo nào',
            style: TextStyle(
              fontFamily: 'Lexend',
              fontSize: 16,
              color: _onSurfaceVariant,
            ),
          ),
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
          const SizedBox(height: 24),
          ..._notifications.map(_buildNotificationCard),
        ],
      ),
    );
  }

  Widget _buildFilterRow() {
    return SizedBox(
      height: 40,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: _filters.length,
        separatorBuilder: (_, _) => const SizedBox(width: 8),
        itemBuilder: (_, index) {
          final isSelected = _selectedFilter == index;
          return GestureDetector(
            onTap: () => _onFilterTap(index),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              decoration: BoxDecoration(
                color: isSelected ? _primaryContainer : _surfaceContainerLowest,
                borderRadius: BorderRadius.circular(9999),
                border: isSelected ? null : Border.all(color: _outlineVariant),
                boxShadow: isSelected
                    ? [
                        const BoxShadow(
                          color: Color.fromRGBO(90, 70, 63, 0.06),
                          blurRadius: 20,
                          offset: Offset(0, 4),
                        ),
                      ]
                    : null,
              ),
              alignment: Alignment.center,
              child: Text(
                _filters[index],
                style: TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 12,
                  fontWeight: FontWeight.w500,
                  letterSpacing: 0.6,
                  color: isSelected ? Colors.white : _onSurface,
                ),
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _buildNotificationCard(NotificationRecord notification) {
    final isUnread = notification.isUnread;

    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: GestureDetector(
        onTap: () {
          final consultationRoute = resolveNotificationRoute(notification);
          if (consultationRoute != null) {
            context.push(consultationRoute);
          } else if (notification.type == 'GROUP_INVITE' &&
              notification.referenceId != null) {
            final pendingInvite = PendingInvitation(
              groupId: notification.referenceId!,
              groupName: 'Nhóm gia đình', // Default fallback
              memberRole: 'MEMBER',
            );
            Navigator.of(context).push(
              MaterialPageRoute(
                builder: (_) =>
                    CareGroupInvitationScreen(invitation: pendingInvite),
              ),
            );
          } else {
            Navigator.of(context).push(
              MaterialPageRoute(
                builder: (_) =>
                    NotificationDetailScreen(notification: notification),
              ),
            );
          }
        },
        child: Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: isUnread
                ? _surfaceContainerLowest
                : _surfaceContainerLowest.withValues(alpha: 0.7),
            borderRadius: BorderRadius.circular(16),
            border: isUnread
                ? null
                : Border.all(color: _outlineVariant.withValues(alpha: 0.3)),
            boxShadow: isUnread
                ? [
                    const BoxShadow(
                      color: Color.fromRGBO(90, 70, 63, 0.06),
                      blurRadius: 20,
                      offset: Offset(0, 4),
                    ),
                  ]
                : null,
          ),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (isUnread)
                Container(
                  margin: const EdgeInsets.only(top: 4, right: 8),
                  width: 8,
                  height: 8,
                  decoration: const BoxDecoration(
                    color: _primaryContainer,
                    shape: BoxShape.circle,
                  ),
                ),
              _buildNotificationIcon(notification),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(
                          _getTypeLabel(notification.type),
                          style: const TextStyle(
                            fontFamily: 'Lexend',
                            fontSize: 12,
                            fontWeight: FontWeight.w500,
                            letterSpacing: 0.6,
                            color: _onSurfaceVariant,
                          ),
                        ),
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
                      notification.title,
                      style: TextStyle(
                        fontFamily: 'Lexend',
                        fontSize: isUnread ? 16 : 16,
                        fontWeight: isUnread
                            ? FontWeight.w600
                            : FontWeight.w400,
                        color: _onSurface,
                      ),
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
    );
  }

  Widget _buildNotificationIcon(NotificationRecord notification) {
    final isUnread = notification.isUnread;
    final type = notification.type.toUpperCase();

    IconData icon;
    Color bgColor;
    Color iconColor;

    switch (type) {
      case 'LOCATION_SHARE':
        icon = Icons.share_location_rounded;
        bgColor = isUnread ? _primaryFixed : _surfaceVariant;
        iconColor = isUnread ? _onPrimaryFixed : _onSurfaceVariant;
        break;
      case 'HEALTH':
      case 'HEALTH_ALERT':
        icon = Icons.warning_rounded;
        bgColor = isUnread ? _errorContainer : _surfaceVariant;
        iconColor = isUnread ? _onErrorContainer : _onSurfaceVariant;
        break;
      case 'GROUP_INVITE':
        icon = Icons.group_add;
        bgColor = isUnread ? _primaryFixed : _surfaceVariant;
        iconColor = isUnread ? _onPrimaryFixed : _onSurfaceVariant;
        break;
      case 'EPDS_RESULT':
        icon = Icons.psychology_outlined;
        bgColor = isUnread ? _primaryFixed : _surfaceVariant;
        iconColor = isUnread ? _onPrimaryFixed : _onSurfaceVariant;
        break;
      case 'APPOINTMENT':
      case 'REMINDER':
        icon = Icons.event_rounded;
        bgColor = isUnread ? _primaryFixed : _surfaceVariant;
        iconColor = isUnread ? _onPrimaryFixed : _onSurfaceVariant;
        break;
      case 'MESSAGE':
      case 'CHAT':
        icon = Icons.chat_rounded;
        bgColor = isUnread ? _surfaceVariant : _surfaceVariant;
        iconColor = _onSurfaceVariant;
        break;
      case 'CONSULTATION':
        icon = Icons.medical_services_outlined;
        bgColor = isUnread ? _primaryFixed : _surfaceVariant;
        iconColor = isUnread ? _onPrimaryFixed : _onSurfaceVariant;
        break;
      default:
        icon = Icons.notifications_rounded;
        bgColor = isUnread ? _primaryFixed : _surfaceVariant;
        iconColor = isUnread ? _onPrimaryFixed : _onSurfaceVariant;
    }

    return Container(
      width: 48,
      height: 48,
      decoration: BoxDecoration(color: bgColor, shape: BoxShape.circle),
      child: Icon(icon, color: iconColor, size: 24),
    );
  }

  String _getTypeLabel(String type) => notificationTypeLabel(type);

  String _formatTime(DateTime dateTime) {
    final now = DateTime.now();
    final diff = now.difference(dateTime);

    if (diff.inMinutes < 1) return 'Vừa xong';
    if (diff.inMinutes < 60) return '${diff.inMinutes} phút trước';
    if (diff.inHours < 24) return '${diff.inHours} giờ trước';
    if (diff.inDays == 1) return 'Hôm qua';
    if (diff.inDays < 7) return '${diff.inDays} ngày trước';

    return '${dateTime.day.toString().padLeft(2, '0')}/'
        '${dateTime.month.toString().padLeft(2, '0')}/'
        '${dateTime.year}';
  }
}
