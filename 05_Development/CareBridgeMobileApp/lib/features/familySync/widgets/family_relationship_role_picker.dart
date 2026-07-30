import 'package:flutter/material.dart';

import '../models/care_group_model.dart';

class FamilyRelationshipSelection {
  const FamilyRelationshipSelection(this.role, this.customRole);
  final String role;
  final String? customRole;
}

Future<FamilyRelationshipSelection?> showFamilyRelationshipRolePicker(
  BuildContext context,
) async {
  var role = 'CHONG';
  final customRole = TextEditingController();
  return showDialog<FamilyRelationshipSelection>(
    context: context,
    builder: (context) => StatefulBuilder(
      builder: (context, setState) => AlertDialog(
        title: const Text('Vai trò trong gia đình'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            DropdownButtonFormField<String>(
              value: role,
              items: familyRelationshipLabels.entries
                  .map(
                    (entry) => DropdownMenuItem(
                      value: entry.key,
                      child: Text(entry.value),
                    ),
                  )
                  .toList(),
              onChanged: (value) => setState(() => role = value!),
            ),
            if (role == 'KHAC')
              TextField(
                controller: customRole,
                decoration: const InputDecoration(
                  labelText: 'Vai trò tùy chỉnh',
                ),
              ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Hủy'),
          ),
          FilledButton(
            onPressed: () {
              if (role == 'KHAC' && customRole.text.trim().isEmpty) return;
              Navigator.pop(
                context,
                FamilyRelationshipSelection(role, customRole.text.trim()),
              );
            },
            child: const Text('Xác nhận'),
          ),
        ],
      ),
    ),
  );
}
