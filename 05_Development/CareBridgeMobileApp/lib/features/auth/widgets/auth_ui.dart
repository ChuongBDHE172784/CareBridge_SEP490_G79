import 'package:flutter/material.dart';

/// Shared visual language for the unauthenticated CareBridge flow.
///
/// The auth screens intentionally keep their own state and navigation logic,
/// while this file owns the visual primitives so welcome, login, and register
/// remain one coherent product surface.
abstract final class AuthPalette {
  static const canvas = Color(0xFFF7F2EC);
  static const surface = Color(0xFFFFFCF9);
  static const surfaceSoft = Color(0xFFF2E8E1);
  static const ink = Color(0xFF3A2924);
  static const muted = Color(0xFF705F58);
  static const mutedStrong = Color(0xFF5A463F);
  static const accent = Color(0xFFC97867);
  static const accentDeep = Color(0xFF845143);
  static const line = Color(0xFFE6D8D0);
  static const error = Color(0xFF9D2B2B);
  static const errorSurface = Color(0xFFFCE9E5);
}

class AuthBackground extends StatelessWidget {
  const AuthBackground({super.key, required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return ColoredBox(
      color: AuthPalette.canvas,
      child: Stack(
        clipBehavior: Clip.none,
        children: [
          const Positioned(
            top: -108,
            right: -94,
            child: _AmbientShape(size: 236, color: Color(0xFFEBD8CF)),
          ),
          const Positioned(
            bottom: -142,
            left: -108,
            child: _AmbientShape(size: 286, color: Color(0xFFE9E6D9)),
          ),
          child,
        ],
      ),
    );
  }
}

class _AmbientShape extends StatelessWidget {
  const _AmbientShape({required this.size, required this.color});

  final double size;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return IgnorePointer(
      child: Container(
        width: size,
        height: size,
        decoration: BoxDecoration(
          color: color.withValues(alpha: 0.7),
          shape: BoxShape.circle,
        ),
      ),
    );
  }
}

class CareBridgeMark extends StatelessWidget {
  const CareBridgeMark({super.key, this.size = 64, this.compact = false});

  final double size;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final radius = compact ? size * 0.28 : size * 0.32;
    return Semantics(
      label: 'CareBridge',
      image: true,
      child: Container(
        width: size,
        height: size,
        decoration: BoxDecoration(
          color: AuthPalette.accentDeep,
          borderRadius: BorderRadius.circular(radius),
          boxShadow: const [
            BoxShadow(
              color: Color(0x1A845143),
              blurRadius: 22,
              offset: Offset(0, 10),
            ),
          ],
        ),
        child: Stack(
          alignment: Alignment.center,
          children: [
            Icon(
              Icons.favorite_rounded,
              size: size * 0.52,
              color: const Color(0xFFFFE8DF),
            ),
            Positioned(
              right: size * 0.12,
              top: size * 0.12,
              child: Container(
                width: size * 0.25,
                height: size * 0.25,
                decoration: const BoxDecoration(
                  color: AuthPalette.accent,
                  shape: BoxShape.circle,
                ),
                child: Icon(
                  Icons.add_rounded,
                  size: size * 0.2,
                  color: Colors.white,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class AuthTopBar extends StatelessWidget {
  const AuthTopBar({super.key, required this.title, required this.onBack});

  final String title;
  final VoidCallback onBack;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 68,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12),
        child: Row(
          children: [
            IconButton(
              tooltip: 'Quay lại',
              onPressed: onBack,
              icon: const Icon(Icons.arrow_back_rounded),
              color: AuthPalette.ink,
              iconSize: 24,
            ),
            Expanded(
              child: Center(
                child: Text(
                  title,
                  style: const TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 17,
                    fontWeight: FontWeight.w700,
                    color: AuthPalette.ink,
                    letterSpacing: -0.2,
                  ),
                ),
              ),
            ),
            const SizedBox(width: 48),
          ],
        ),
      ),
    );
  }
}

class AuthIntro extends StatelessWidget {
  const AuthIntro({
    super.key,
    required this.title,
    required this.subtitle,
    this.centered = false,
    this.eyebrow,
  });

  final String title;
  final String subtitle;
  final bool centered;
  final String? eyebrow;

  @override
  Widget build(BuildContext context) {
    final alignment = centered
        ? CrossAxisAlignment.center
        : CrossAxisAlignment.start;
    return Column(
      crossAxisAlignment: alignment,
      children: [
        if (eyebrow != null) ...[
          Text(
            eyebrow!,
            textAlign: centered ? TextAlign.center : TextAlign.start,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 11,
              fontWeight: FontWeight.w700,
              color: AuthPalette.accentDeep,
              letterSpacing: 1.7,
            ),
          ),
          const SizedBox(height: 12),
        ],
        Text(
          title,
          textAlign: centered ? TextAlign.center : TextAlign.start,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 27,
            height: 1.18,
            fontWeight: FontWeight.w700,
            color: AuthPalette.ink,
            letterSpacing: -0.7,
          ),
        ),
        const SizedBox(height: 10),
        Text(
          subtitle,
          textAlign: centered ? TextAlign.center : TextAlign.start,
          style: const TextStyle(
            fontFamily: 'Lexend',
            fontSize: 14,
            height: 1.55,
            color: AuthPalette.muted,
          ),
        ),
      ],
    );
  }
}

class AuthSurface extends StatelessWidget {
  const AuthSurface({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.all(20),
    this.color = AuthPalette.surface,
  });

  final Widget child;
  final EdgeInsetsGeometry padding;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: padding,
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: AuthPalette.line),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0D3A2924),
            blurRadius: 24,
            offset: Offset(0, 10),
          ),
        ],
      ),
      child: child,
    );
  }
}

class AuthTextField extends StatelessWidget {
  const AuthTextField({
    super.key,
    required this.controller,
    required this.label,
    required this.hint,
    this.obscureText = false,
    this.suffixIcon,
    this.keyboardType,
    this.onChanged,
    this.errorText,
    this.textInputAction,
  });

  final TextEditingController controller;
  final String label;
  final String hint;
  final bool obscureText;
  final Widget? suffixIcon;
  final TextInputType? keyboardType;
  final ValueChanged<String>? onChanged;
  final String? errorText;
  final TextInputAction? textInputAction;

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      obscureText: obscureText,
      keyboardType: keyboardType,
      onChanged: onChanged,
      textInputAction: textInputAction,
      style: const TextStyle(
        fontFamily: 'Lexend',
        fontSize: 16,
        color: AuthPalette.ink,
      ),
      decoration: InputDecoration(
        labelText: label,
        floatingLabelBehavior: FloatingLabelBehavior.always,
        hintText: hint,
        hintStyle: const TextStyle(
          fontFamily: 'Lexend',
          fontSize: 15,
          color: Color(0xFFA28E85),
        ),
        labelStyle: const TextStyle(
          fontFamily: 'Lexend',
          fontSize: 12,
          fontWeight: FontWeight.w600,
          color: AuthPalette.muted,
        ),
        errorText: errorText,
        errorStyle: const TextStyle(
          fontFamily: 'Lexend',
          fontSize: 12,
          color: AuthPalette.error,
        ),
        filled: true,
        fillColor: AuthPalette.surface,
        contentPadding: const EdgeInsets.fromLTRB(16, 18, 16, 14),
        suffixIcon: suffixIcon,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: const BorderSide(color: AuthPalette.line),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: const BorderSide(color: AuthPalette.line),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: const BorderSide(
            color: AuthPalette.accentDeep,
            width: 1.5,
          ),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: const BorderSide(color: AuthPalette.error),
        ),
        focusedErrorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14),
          borderSide: const BorderSide(color: AuthPalette.error, width: 1.5),
        ),
      ),
    );
  }
}

class AuthErrorBanner extends StatelessWidget {
  const AuthErrorBanner({super.key, required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      liveRegion: true,
      label: message,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
        decoration: BoxDecoration(
          color: AuthPalette.errorSurface,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: const Color(0xFFF2C9C3)),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Padding(
              padding: EdgeInsets.only(top: 1),
              child: Icon(
                Icons.error_outline_rounded,
                size: 19,
                color: AuthPalette.error,
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Text(
                message,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 13,
                  height: 1.45,
                  color: AuthPalette.error,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class AuthPrimaryButton extends StatelessWidget {
  const AuthPrimaryButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.buttonKey,
    this.isLoading = false,
  });

  final String label;
  final VoidCallback? onPressed;
  final Key? buttonKey;
  final bool isLoading;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 56,
      child: FilledButton(
        key: buttonKey,
        onPressed: isLoading ? null : onPressed,
        style: FilledButton.styleFrom(
          backgroundColor: AuthPalette.accentDeep,
          foregroundColor: Colors.white,
          disabledBackgroundColor: AuthPalette.accentDeep.withValues(
            alpha: 0.55,
          ),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
          padding: const EdgeInsets.symmetric(horizontal: 20),
        ),
        child: isLoading
            ? const SizedBox.square(
                dimension: 20,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  color: Colors.white,
                ),
              )
            : Text(
                label,
                style: const TextStyle(
                  fontFamily: 'Lexend',
                  fontSize: 15,
                  fontWeight: FontWeight.w700,
                ),
              ),
      ),
    );
  }
}

class AuthDivider extends StatelessWidget {
  const AuthDivider({super.key, this.label = 'hoặc tiếp tục với'});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        const Expanded(child: Divider(color: AuthPalette.line)),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12),
          child: Text(
            label,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 12,
              color: AuthPalette.muted,
            ),
          ),
        ),
        const Expanded(child: Divider(color: AuthPalette.line)),
      ],
    );
  }
}

class AuthSocialButton extends StatelessWidget {
  const AuthSocialButton({
    super.key,
    required this.label,
    required this.icon,
    required this.onPressed,
  });

  final String label;
  final Widget icon;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: SizedBox(
        height: 50,
        child: OutlinedButton.icon(
          onPressed: onPressed,
          icon: icon,
          label: Text(
            label,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(
              fontFamily: 'Lexend',
              fontSize: 13,
              fontWeight: FontWeight.w600,
              color: AuthPalette.ink,
            ),
          ),
          style: OutlinedButton.styleFrom(
            foregroundColor: AuthPalette.accentDeep,
            side: const BorderSide(color: AuthPalette.line),
            backgroundColor: AuthPalette.surface,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(14),
            ),
          ),
        ),
      ),
    );
  }
}

class AuthInfoCard extends StatelessWidget {
  const AuthInfoCard({super.key, required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return AuthSurface(
      color: AuthPalette.surfaceSoft,
      padding: const EdgeInsets.all(16),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 34,
            height: 34,
            decoration: const BoxDecoration(
              color: Color(0xFFE8D2C7),
              shape: BoxShape.circle,
            ),
            child: const Icon(
              Icons.auto_awesome_outlined,
              size: 18,
              color: AuthPalette.accentDeep,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              message,
              style: const TextStyle(
                fontFamily: 'Lexend',
                fontSize: 13,
                height: 1.55,
                color: AuthPalette.mutedStrong,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
