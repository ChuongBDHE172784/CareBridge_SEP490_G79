import 'package:flutter/material.dart';

/// Standard CareBridge user avatar component.
///
/// Displays the network profile picture when [avatarUrl] is valid.
/// Automatically falls back to displaying the official CareBridge logo asset
/// (`assets/logo.png`) when [avatarUrl] is null, empty, or fails to load over the network.
class AppUserAvatar extends StatelessWidget {
  const AppUserAvatar({
    super.key,
    this.avatarUrl,
    this.radius = 24.0,
    this.onTap,
    this.backgroundColor,
    this.border,
    this.fallbackAsset = 'assets/logo.png',
  });

  final String? avatarUrl;
  final double radius;
  final VoidCallback? onTap;
  final Color? backgroundColor;
  final BoxBorder? border;
  final String fallbackAsset;

  @override
  Widget build(BuildContext context) {
    final size = radius * 2;
    final defaultBg = backgroundColor ?? const Color(0xFFF2E8E1);
    final hasUrl = avatarUrl != null && avatarUrl!.trim().isNotEmpty;

    Widget avatarContent;

    if (hasUrl) {
      avatarContent = Image.network(
        avatarUrl!.trim(),
        width: size,
        height: size,
        fit: BoxFit.cover,
        errorBuilder: (context, error, stackTrace) {
          return _buildLogoFallback(size);
        },
        loadingBuilder: (context, child, loadingProgress) {
          if (loadingProgress == null) return child;
          return _buildLoadingPlaceholder(size, defaultBg);
        },
      );
    } else {
      avatarContent = _buildLogoFallback(size);
    }

    Widget avatarWidget = Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: defaultBg,
        shape: BoxShape.circle,
        border: border,
      ),
      child: ClipOval(child: avatarContent),
    );

    if (onTap != null) {
      avatarWidget = GestureDetector(
        onTap: onTap,
        behavior: HitTestBehavior.opaque,
        child: avatarWidget,
      );
    }

    return avatarWidget;
  }

  Widget _buildLogoFallback(double size) {
    return Container(
      width: size,
      height: size,
      color: const Color(0xFFFFF8F6),
      padding: EdgeInsets.all(size * 0.12),
      child: Image.asset(
        fallbackAsset,
        width: size,
        height: size,
        fit: BoxFit.contain,
        errorBuilder: (context, error, stackTrace) {
          return Icon(
            Icons.person_rounded,
            size: size * 0.6,
            color: const Color(0xFF845143),
          );
        },
      ),
    );
  }

  Widget _buildLoadingPlaceholder(double size, Color bgColor) {
    return Container(
      width: size,
      height: size,
      color: bgColor,
      child: Center(
        child: SizedBox(
          width: size * 0.4,
          height: size * 0.4,
          child: const CircularProgressIndicator(
            strokeWidth: 2,
            color: Color(0xFF845143),
          ),
        ),
      ),
    );
  }
}
