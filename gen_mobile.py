#!/usr/bin/env python3
import pathlib
src = pathlib.Path(r"d:\Do_aN\05_Development\CareBridgeMobileApp\lib\features\expert\screens\expert_identity_capture_screen.dart")
print("exists:", src.exists())
print("size:", src.stat().st_size if src.exists() else 0)
