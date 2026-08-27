import 'package:flutter/material.dart';

import '../models/care_group_model.dart';

class FamilyRelationshipSelection {
  const FamilyRelationshipSelection(this.role, this.customRole);
  final String role;
  final String? customRole;
}

Future<FamilyRelationshipSelection?> showFamilyRelationshipRolePicker(
  BuildContext context,
) {
  return showDialog<FamilyRelationshipSelection>(
    context: context,
    builder: (context) => const _FamilyRelationshipRolePickerContent(),
  );
}

class _FamilyRelationshipRolePickerContent extends StatefulWidget {
  const _FamilyRelationshipRolePickerContent();

  @override
  State<_FamilyRelationshipRolePickerContent> createState() =>
      _FamilyRelationshipRolePickerContentState();
}

class _FamilyRelationshipRolePickerContentState
    extends State<_FamilyRelationshipRolePickerContent> {
  String _role = 'CHONG';
  late final TextEditingController _customRoleCtrl;

  @override
  void initState() {
    super.initState();
    _customRoleCtrl = TextEditingController();
  }

  @override
  void dispose() {
    _customRoleCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Vai trò trong gia đình'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          DropdownButtonFormField<String>(
            initialValue: _role,
            items: familyRelationshipLabels.entries
                .map(
                  (entry) => DropdownMenuItem(
                    value: entry.key,
                    child: Text(entry.value),
                  ),
                )
                .toList(),
            onChanged: (value) {
              if (value != null) {
                setState(() => _role = value);
              }
            },
          ),
          if (_role == 'KHAC')
            TextField(
              controller: _customRoleCtrl,
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
            if (_role == 'KHAC' && _customRoleCtrl.text.trim().isEmpty) return;
            Navigator.pop(
              context,
              FamilyRelationshipSelection(_role, _customRoleCtrl.text.trim()),
            );
          },
          child: const Text('Xác nhận'),
        ),
      ],
    );
  }
}
