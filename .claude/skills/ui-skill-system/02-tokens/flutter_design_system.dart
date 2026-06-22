import 'package:flutter/material.dart';

/// MOMCARE CLAY DESIGN SYSTEM
/// Implementation of Warm Claymorphism aesthetic in Flutter
class ClayColors {
  static const Color primary = Color(0xFFC98C7B);
  static const Color primaryHover = Color(0xFFB67868);
  static const Color background = Color(0xFFF6F1EC);
  static const Color surface = Color(0xFFF2EAE4);
  static const Color card = Color(0xFFFFFFFF);
  static const Color textPrimary = Color(0xFF5A463F);
  static const Color textSecondary = Color(0xFF9C857C);
  static const Color success = Color(0xFF81C784);
  static const Color error = Color(0xFFE57373);
}

class ClayShadows {
  static List<BoxShadow> soft = [
    BoxShadow(
      color: Color(0x0A5A463F),
      blurRadius: 24,
      offset: Offset(0, 8),
    ),
  ];

  static List<BoxShadow> floating = [
    BoxShadow(
      color: Color(0x1F5A463F),
      blurRadius: 64,
      offset: Offset(0, 24),
    ),
  ];

  static List<BoxShadow> hover = [
    BoxShadow(
      color: Color(0x145A463F),
      blurRadius: 40,
      offset: Offset(0, 16),
    ),
  ];
}

class ClayShapes {
  static final BorderRadius card = BorderRadius.circular(32);
  static final BorderRadius pill = BorderRadius.circular(999);
  static final BorderRadius input = BorderRadius.circular(24);
}

// Example Wrapper for a Clay Card
class ClayCard extends StatelessWidget {
  final Widget child;
  final EdgeInsets? padding;
  final bool floating;

  const ClayCard({
    Key? key,
    required this.child,
    this.padding,
    this.floating = false,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: padding ?? const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: ClayColors.card,
        borderRadius: ClayShapes.card,
        boxShadow: floating ? ClayShadows.floating : ClayShadows.soft,
        border: Border.all(color: ClayColors.surface.withOpacity(0.5)),
      ),
      child: child,
    );
  }
}
