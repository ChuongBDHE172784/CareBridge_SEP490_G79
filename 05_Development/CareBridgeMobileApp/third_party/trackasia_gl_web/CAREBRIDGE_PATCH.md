# CareBridge compatibility patch

This directory vendors the official `trackasia_gl_web` 2.0.1 package under its
original license. The only source change replaces the removed
`dart:ui.platformViewRegistry` access with `dart:ui_web.platformViewRegistry`
so the real TrackAsia Web implementation compiles on Flutter 3.38.
