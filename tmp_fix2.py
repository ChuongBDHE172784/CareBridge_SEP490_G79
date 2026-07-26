
backup = pathlib.Path(r"d:\Do_aN\05_Development\CareBridgeMobileApp\lib\features\expert\screens\expert_identity_capture_screen_backup.dart")
backup.write_text(orig, encoding="utf-8")
print(f"Backup saved: {backup}")
print(f"Original lines: {len(orig.splitlines())}")
