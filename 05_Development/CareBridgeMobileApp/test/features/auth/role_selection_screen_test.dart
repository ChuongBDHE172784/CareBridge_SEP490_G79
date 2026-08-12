import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/auth/screens/role_selection_screen.dart';

void main() {
  testWidgets('renders redesigned RoleSelectionScreen header and role cards', (
    tester,
  ) async {
    await tester.pumpWidget(const MaterialApp(home: RoleSelectionScreen()));
    await tester.pumpAndSettle();

    expect(find.text('Chọn vai trò'), findsOneWidget);
    expect(find.text('Mẹ bầu'), findsOneWidget);
    expect(find.text('Người thân'), findsOneWidget);
    expect(find.text('Chuyên gia'), findsOneWidget);
    expect(find.text('PHỔ BIẾN NHẤT'), findsOneWidget);
    expect(find.text('GIA ĐÌNH'), findsOneWidget);
    expect(find.text('Y TẾ & TƯ VẤN'), findsOneWidget);
    expect(find.text('Vui lòng chọn 1 vai trò để tiếp tục'), findsOneWidget);
  });

  testWidgets('selecting a role updates selection hint and enables button', (
    tester,
  ) async {
    await tester.pumpWidget(const MaterialApp(home: RoleSelectionScreen()));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Mẹ bầu'));
    await tester.pumpAndSettle();

    expect(find.text('Đã chọn: Mẹ bầu'), findsOneWidget);
    expect(find.text('Vui lòng chọn 1 vai trò để tiếp tục'), findsNothing);

    final button = tester.widget<FilledButton>(find.byType(FilledButton));
    expect(button.onPressed, isNotNull);
  });
}
