<div align="center">

# CommandCookies Electronic Mod

**Minecraft with working electronics.**

Computers · Phones · TVs · Security Cameras · Retro Consoles

**Minecraft 1.21.1** · **NeoForge 21.1.232** · **Java 21**

[Download on Modrinth](https://modrinth.com/mod/commandcookieselectronicmod) · [Retro Expansion](retro-expansion/README.md) · [Build from source](#build-from-source)

</div>

---

CommandCookies Electronic Mod (**CEM**) adds functional electronics and decorative hardware to Minecraft. Build a computer setup, browse the web, display images on a TV, watch your security cameras, or connect a retro console to its monitor.

This repository contains the **3.0.0 main mod**, the optional **Retro Expansion**, and their editable source assets. It is a standalone Gradle project.

## Features

| Feature | What you can do |
| --- | --- |
| **Computers & laptops** | Use desktops and apps, connect gaming PCs to monitors with HDMI, and customize setups with keyboards and mice. |
| **Phones** | Use phone apps, access paired security cameras, and browse with Chromium. Media mode can keep audio playing after closing the phone. |
| **Chromium browser** | Browse real websites with tabs, saved browser data, Standard and Media modes, and 25 or 60 FPS settings. |
| **Image & GIF TVs** | Display Imgur images and animated GIFs on wall-mounted or standing TVs, with image-fitting controls. |
| **Security cameras** | Pair cameras privately to your phone and view them remotely. Two AA batteries power a camera for 15 real-world days. |
| **Power & cables** | Connect generators, outlets and devices using a wrench. HDMI and AV cables handle compatible display connections. |
| **Retro consoles** | Build NES, SNES and N64 setups with matching controllers. The optional Retro Expansion adds playback of your own local games. |
| **Headphones** | Wear AirPods Max in ten colours. |
| **Animated setup guides** | Learn device setup through interactive scenes with playback controls, action cues and a rotatable preview. |

## Installation

1. Install **NeoForge for Minecraft 1.21.1** using a compatible Java 21 installation.
2. Download the main mod from [Modrinth](https://modrinth.com/mod/commandcookieselectronicmod), or build it from this repository.
3. Put the main mod JAR in your Minecraft instance's `mods` folder.
4. To play local NES, SNES or N64 ROMs, also install the matching **Retro Expansion** build on both client and server.

Use one copy of the main mod at a time. Install the regular mod JARs, not source JARs or the repository folder.

### Retro Expansion

The base mod includes its own NES Tetris cartridge. SNES and N64 setups are decorative without Retro Expansion.

With the addon installed, use the matching ROM cartridge to select one of your own local games, insert it into its console, then interact with the linked monitor while holding the matching controller.

**No ROMs or BIOS files are included.** Local ROM files are not sent to the server.

See the [Retro Expansion guide](retro-expansion/README.md) for supported formats, controls, saves, platforms and third-party licenses.

## Getting started in game

### Power a setup

1. Place a **Generator**, open its menu and add furnace fuel.
2. Right-click the generator with a **Wrench**, then a compatible wall outlet to connect its feed.
3. Use the wrench on the outlet and a compatible device to connect its power cord.
4. Add the appropriate display connection: **HDMI** for a gaming PC and monitor, or **AV** for a retro console and retro monitor.

Generator-to-outlet connections reach up to **120 blocks**. Outlet-to-device connections reach up to **15 blocks**. HDMI and AV connections reach up to **16 blocks**.

A wall power outlet supplies two devices. A coax outlet has one socket. CRT TVs and retro monitors require both wall power and coax; gaming PCs and their monitors each need their own power connection. Security cameras use batteries, and laptops need no external power.

### Open a setup guide

Hover over a supported item and press **G**, or press G while holding it. The key can be changed under **Open Setup Guide** in Controls.

Guides cover generators, the wrench, both outlet types, TVs, gaming PC setups, HDMI and AV cables, security cameras, and NES/SNES/N64 setups. Related controllers and cartridges open their console's guide. Cameras and AA batteries open the camera guide; phones and laptops have no guide shortcut.

- Use **Back**, **Next**, **Play/Pause** and **Replay** to control the scene.
- Click a step or progress segment to jump through the lesson.
- Drag the preview to rotate it.
- Scroll the instruction card in smaller windows to read longer steps.

ROM-importing instructions appear only when Retro Expansion is installed. The guides use CEM's own rendering and playback code and require no additional tutorial mod.

### Customize a TV

Right-click an image TV to open its settings. Paste an Imgur link, choose **Whole image** or **Fill screen**, and apply it. Both image TV types require power.

Wall-mounted TVs also offer three orientation sliders:

| Control | Range |
| --- | --- |
| Horizontal swivel | −45° to +45° |
| Vertical tilt | −45° to +45° |
| Screen rotation | −90° to +90°, including portrait orientation |

At either portrait position, images stay upright and use the tall screen aspect ratio. Leave enough space around the TV for rotation. **Reset** returns all three angles to zero.

## Build from source

### Requirements

- A **Java 21 JDK**
- An internet connection for the first Gradle and dependency downloads

Open the repository as a Gradle project in your preferred Java editor. The included Gradle wrapper handles the Gradle version.

**Windows**

```powershell
.\gradlew.bat build release
```

**Linux / macOS**

```sh
sh ./gradlew build release
```

Installable files are collected in `build/release/`:

| File | Purpose |
| --- | --- |
| `commandcookies-electronic-mod-3.0.0.jar` | Main mod |
| `commandcookies-retro-expansion-1.1.0.jar` | Optional addon built for the main mod |

Version numbers are configured in [`gradle.properties`](gradle.properties). When changing the main mod's compatibility range, also update the addon's [mod metadata](retro-expansion/src/main/resources/META-INF/neoforge.mods.toml).

## Run for development

On Windows, the included launchers find a Java 21 JDK and start a development instance:

- **Run Main Mod.cmd** — main mod only
- **Run With Retro Expansion.cmd** — main mod and addon together

Leave the launcher terminal open while playing. You can also use Gradle directly:

| Environment | Windows command |
| --- | --- |
| Main mod client | `.\gradlew.bat :main:runClient` |
| Main mod + addon client | `.\gradlew.bat :retro-expansion:runClient` |
| Main mod server | `.\gradlew.bat :main:runServer` |
| Main mod + addon server | `.\gradlew.bat :retro-expansion:runServer` |

On Linux/macOS, replace `.\gradlew.bat` with `sh ./gradlew`. Dedicated servers require accepting Minecraft's EULA in their run folder.

Each module keeps its development worlds, settings, browser profiles, downloads and ROM libraries in its own ignored `run/` folder. These files are excluded from release JARs. Optional development JVM arguments can be supplied through `CEM_JVM_OPTIONS`.

## Repository layout

| Folder | Contents |
| --- | --- |
| [`main/`](main/) | Main mod: devices, apps, networking, screens, cables and power |
| [`main/src/main/resources/`](main/src/main/resources/) | Models, textures, recipes, sounds, translations and packaged browser helpers |
| [`retro-expansion/`](retro-expansion/) | Optional NES/SNES/N64 gameplay addon |
| [`retro-expansion/src/emulatorHelper/`](retro-expansion/src/emulatorHelper/) | Separate Java emulator helper |
| [`retro-expansion/src/main/resources/retro_bridge/`](retro-expansion/src/main/resources/retro_bridge/) | Emulator binaries, corresponding source archives, hashes and notices |
| [`model-sources/`](model-sources/) | Editable model files; not included in the mod JAR |
| [`browser-tools/`](browser-tools/) | Browser helper sources and maintenance instructions |

## Browser runtimes & third-party software

Chromium engine files are installed on demand, with an in-game download confirmation. Building the project does not download a browser engine. Browser helpers, runtime hashes, signatures and third-party notices are included where required.

Retro Expansion retains its bundled emulator cores, corresponding source archives and license notices. Consult its documentation for platform support and runtime limitations.

See [browser helper maintenance](browser-tools/README.md), [Retro Expansion documentation](retro-expansion/README.md), and the repository's [license](LICENSE) before modifying or redistributing those components.

## Development references

- [NeoForge setup documentation](https://docs.neoforged.net/docs/1.21.1/gettingstarted/)
- [ModDevGradle](https://github.com/neoforged/ModDevGradle)

## Credits

- **CommandCookie** — owner
- **nikaa** — artist and developer
- Published by **CookieByte Studios**

## License

CEM's own code is released under the [MIT License](LICENSE). Bundled third-party components, including Retro Expansion's emulator cores and helpers, keep their own licenses; see [`NOTICE.txt`](retro-expansion/src/main/resources/retro_bridge/NOTICE.txt) and the [Retro Expansion documentation](retro-expansion/README.md).

## Trademarks

CEM is a fan-made mod and is not affiliated with or endorsed by Mojang, Microsoft, Apple, Nintendo, Sony, Google, or any other company. All product names and trademarks belong to their respective owners.
