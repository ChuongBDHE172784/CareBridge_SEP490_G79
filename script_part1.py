SRC = r"d:\Do_aN\05_Development\CareBridgeMobileApp\lib\features\expert\screens\expert_identity_capture_screen.dart"
DST = r"d:\Do_aN\05_Development\CareBridgeMobileApp\lib\features\expert\screens\expert_identity_capture_screen.dart"
text = open(SRC, encoding="utf-8").read()
bak = SRC + ".bak"
open(bak, "w", encoding="utf-8").write(text)
print("backup ok, lines:", text.count("\n") + 1)
