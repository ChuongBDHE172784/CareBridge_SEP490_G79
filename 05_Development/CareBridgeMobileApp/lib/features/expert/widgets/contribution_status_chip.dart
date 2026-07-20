import 'package:flutter/material.dart';

import '../models/contribution_model.dart';

class ContributionStatusChip extends StatelessWidget {
  const ContributionStatusChip({
    super.key,
    required this.status,
    this.compact = false,
  });

  final ContributionStatus status;
  final bool compact;

  static const Map<ContributionStatus, _ChipStyle> _styles = {
    ContributionStatus.draft: _ChipStyle(
      backgroundColor: Color(0xFFF3F4F6),
      foregroundColor: Color(0xFF374151),
      borderColor: Color(0xFFD1D5DB),
    ),
    ContributionStatus.submitted: _ChipStyle(
      backgroundColor: Color(0xFFFFF3CD),
      foregroundColor: Color(0xFF856404),
      borderColor: Color(0xFFFFE69C),
    ),
    ContributionStatus.approved: _ChipStyle(
      backgroundColor: Color(0xFFD1E7DD),
      foregroundColor: Color(0xFF0F5132),
      borderColor: Color(0xFFA3CFBB),
    ),
    ContributionStatus.rejected: _ChipStyle(
      backgroundColor: Color(0xFFF8D7DA),
      foregroundColor: Color(0xFF842029),
      borderColor: Color(0xFFF5C2C7),
    ),
  };

  @override
  Widget build(BuildContext context) {
    final style = _styles[status] ?? _styles[ContributionStatus.draft]!;

    return Container(
      padding: EdgeInsets.symmetric(
        horizontal: compact ? 8 : 10,
        vertical: compact ? 3 : 5,
      ),
      decoration: BoxDecoration(
        color: style.backgroundColor,
        borderRadius: BorderRadius.circular(compact ? 8 : 12),
        border: Border.all(color: style.borderColor),
      ),
      child: Text(
        status.label,
        style: TextStyle(
          fontFamily: 'Lexend',
          fontSize: compact ? 10 : 11,
          fontWeight: FontWeight.w600,
          color: style.foregroundColor,
        ),
      ),
    );
  }
}

class _ChipStyle {
  final Color backgroundColor;
  final Color foregroundColor;
  final Color borderColor;

  const _ChipStyle({
    required this.backgroundColor,
    required this.foregroundColor,
    required this.borderColor,
  });
}