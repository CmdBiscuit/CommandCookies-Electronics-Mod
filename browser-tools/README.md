# Browser helper maintenance

The main mod contains two separate browser helpers. Their existing packaged bytes and signatures are preserved during migration.

- `standard/src/` contains the Java/JCEF helper matching the installed `cem-browser-helper.jar`.
- `media/` contains the four Electron source files matching `cem-media-browser.asar`.
- Main-mod dependency JARs, runtime manifests, signatures, and notices remain under `main/src/main/resources/assets/commandcookies_electronic_mod/browser/`.

Run `python browser-tools/rebuild-standard.py` with a Java 21 JDK on `JAVA_HOME` or `PATH`. It compiles into the ignored `build/browser-tools/` folder and checks every resulting class against the packaged helper. It does not replace the shipped helper.

If changing the Standard helper later, package its new classes and update that helper's SHA-256 in `runtime.json` together. Test startup, typing, scrolling, frame rates, profile persistence, and shutdown before replacing the packaged helper.

To rebuild Media source, use the official `@electron/asar` package to package only `package.json`, `main.js`, `preload.js`, and `protocol.js`. Update `media.helper.sha256` in `runtime.json`. Preserve the existing runtime version/hash and `electron.exe.sig` when the runtime executable itself is unchanged. Replacing or modifying the executable requires the appropriate castLabs signing workflow. Keep account credentials and verification codes outside the project.

The browser engines are installed on demand by the mod. They are not build dependencies and must not be copied from a personal browser profile into the release.
