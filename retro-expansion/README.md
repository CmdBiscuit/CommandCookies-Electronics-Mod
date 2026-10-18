# CommandCookies Retro Expansion 1.1.0



Minecraft 1.21.1 / NeoForge 21.1.232. Install this addon with CommandCookie's Electronic Mod 3.0.x on both client and server. SNES/N64 require the main-mod build containing those consoles and controllers.



## Playing



1. Power the console from a wall outlet. Supply the retro monitor with wall power and coax, then connect the console to it using the main mod's AV cable.

2. Craft the matching NES, SNES or N64 ROM Cartridge in the electronics workbench's **Cartridges** section, or obtain it from the creative electronics tab. Each costs 2 iron ingots, 1 Basic Circuit Board, 1 redstone dust and 1 paper. Crafting-table recipes also remain available.

3. Right-click the cartridge in the air. Paste the path of your local ROM and choose **Import**, or use **Open ROM folder**, copy the file there, and **Refresh**. Click a game to assign it to the cartridge.

4. Right-click the matching console with the configured cartridge. Hold that console's controller in either hand and use the linked monitor.

5. Space pauses. Escape closes and saves. Save/Reset buttons and volume are available. Crouch + right-click the console to eject the cartridge.



| System | ROM formats | Controls |

| --- | --- | --- |

| NES | `.nes` | Arrows: D-pad; Z: A; X: B; Enter: Start; right Shift: Select |

| SNES | `.sfc`, `.smc` | Arrows: D-pad; Z: A; X: B; A: Y; S: X; Q: L; W: R; Enter: Start; right Shift: Select |

| N64 | `.z64`, `.n64`, `.v64` | WASD: stick; left Shift: slower stick; arrows: D-pad; Z: A; X: B; C: Z trigger; Q/E: L/R; I/J/K/L: C-up/left/down/right; Enter: Start |



N64 uses 2× software rendering with automatic RSP selection, anti-aliasing/dedithering, a trimmed TV-safe viewport and smooth display scaling. Renderer workers follow the core's capped physical-core selection. The helper reduces redundant VI samples before transfer, and Minecraft decodes/updates N64 pictures in batches. Demanding games may still run slowly. This is single-controller local emulation. The addon does not stream the emulated game or sound to other players. Only one player may reserve a console at a time. Moving away, losing power, changing the cartridge/controller, or breaking its AV link closes the session.



## Local ROM libraries



No ROMs or BIOS files are included. Files are never sent to the server. Another player using your cartridge must have identical game bytes in their own library.



- NES: `config/commandcookies-retro/roms/nes/`. Existing `.nes` files in the old `roms/` folder are still found; duplicates are listed once.

- SNES: `config/commandcookies-retro/roms/snes/`.

- N64: `config/commandcookies-retro/roms/n64/`.



All three named ROM folders are created when the client starts. **Open ROM folder** opens the matching system folder. Use uncompressed ROMs. SNES headered `.smc` files and all three N64 byte orders are accepted. The limit is 16 MiB for NES/SNES and 128 MiB for N64. Importing a different game with an existing filename requires renaming it; the original file is preserved.



## Bundled emulators



The JAR includes Mesen for NES, bsnes for SNES, ParaLLEl N64 for N64, a Java/JNA NES frontend, and a native frontend for SNES/N64. Players do not need the RetroArch desktop app or a separate core download. Native cores run in separate helper processes, not inside Minecraft's Java process. SNES/N64 callbacks stay entirely in native code so internal emulator coroutine stacks cannot call into Java.



Windows x64, Linux x64/ARM64, and macOS Intel/Apple Silicon cores are included. The addon selects the matching 64-bit Java platform, verifies its SHA-256, and extracts a copy to `config/commandcookies-retro/cores/<platform>/<sha256>/`. Extraction works offline and repairs damaged cached copies. Unsupported platforms show an error. Linux/macOS binaries are packaged and inspected, but cannot be runtime-tested on this Windows machine.



An explicit custom core in `config/commandcookies-retro/cores/` takes precedence: `mesen_libretro`, `bsnes_libretro`, or `parallel_n64_libretro`, with the platform's `.dll`, `.so`, or `.dylib` extension. N64 custom cores must support ParaLLEl N64's software renderer/options. Optional game-specific auxiliary files can go under `config/commandcookies-retro/system/`; none are bundled.



## Saves



Local states/SRAM use the existing path under `config/commandcookies-retro/saves/<world UUID>/<console UUID>/<ROM SHA-256>/<core SHA-256>/`. Existing NES libraries and saves are preserved. States resume on reopening the same console, auto-save every 600 emulated frames, and save on normal closing. The Save button also writes a state. bsnes manages its own `.srm` battery files inside that same save folder and flushes them on closing. Saves are client-local. Replacing the console or changing the core binary creates a different save location. A sudden process termination can lose progress since the last save.



Per-session emulator logs are capped near 64 KiB. Startup has a 30-second watchdog; a stalled running core has 15 seconds. Startup/game errors appear in the monitor screen.



## Building and publishing

Build from the repository root with `./gradlew :retro-expansion:build`. The addon compiles against the sibling main project. See the root README for setup and release instructions.

Minecraft integration: MIT. Standalone helper: GPL-3.0-or-later. Mesen: GPL-3.0. bsnes: GPL-3.0-or-later, with original component notices. ParaLLEl N64 has GPL/LGPL and permissive component licenses; original license files and notices are retained. JNA 5.14.0 is distributed under its Apache-2.0 option.



Pinned upstream source snapshots, original notices, helper Java/C source, native frontend build instructions, license texts, and manifests with binary/source hashes and download URLs are included under `retro_bridge/`. See `NOTICE.txt`, `bsnes-manifest.json`, `parallel_n64-manifest.json`, `frontend-manifest.json`, `native-source/`, and the existing Mesen manifest. No upstream core binaries are modified. Keep these notices/source archives when redistributing the JAR.



Official projects: [Mesen](https://github.com/libretro/Mesen), [bsnes](https://github.com/libretro/bsnes-libretro), [ParaLLEl N64](https://github.com/libretro/parallel-n64), [JNA](https://github.com/java-native-access/jna).

