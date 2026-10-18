# Native frontend build

NativeHost.c: Copyright 2026 CommandCookie, GPL-3.0-or-later.
libretro.h retains its original MIT notice.

Using Zig 0.15.2, run (replace TARGET and output path with the manifest entry):

`zig cc -target TARGET -O2 -std=c11 -s -I . NativeHost.c -o native-host`

Windows: add `-municode`, output `native-host.exe`.
Linux: add `-ldl -lm`. macOS: no extra flags.
The bundled frontend-manifest.json records targets, compiler version and hashes.
The frontend dynamically loads the supplied core without modifying it.
No game files or BIOS images are linked into the executable.

Windows file handling uses extended absolute Unicode paths for file reads, atomic save replacement, state restore and core loading. This supports the normal UUID/hash save layout beyond the traditional MAX_PATH limit.

N64 defaults: automatic RSP selection (including homebrew fallback), 2x Angrylion rendering, AA/dedither without VI blur, and the core's capped physical-core worker selection. The frontend supplies a cached software framebuffer, trims fixed overscan margins, and averages horizontally doubled VI samples before transmission. SNES options and framing are unchanged.

Windows file handling uses extended absolute Unicode paths for file reads, atomic save replacement, state restore and core loading. This supports the normal UUID/hash save layout beyond the traditional MAX_PATH limit.
