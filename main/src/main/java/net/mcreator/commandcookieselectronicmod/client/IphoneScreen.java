package net.mcreator.commandcookieselectronicmod.client;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.mcreator.commandcookieselectronicmod.calculator.CalculatorEngine;
import net.mcreator.commandcookieselectronicmod.network.PhoneFlashlightPayload;
import net.mcreator.commandcookieselectronicmod.procedures.IphoneLogicProcedure;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public class IphoneScreen extends Screen {
    private enum Page { LOCK, HOME, CALCULATOR, NOTES, CLOCK, FLASHLIGHT, MUSIC, SOON }
    private Page page = Page.LOCK;
    private int homePage, musicPage;
    private final String color;
    private final String profileKey;
    private final boolean samsung;
    private final int[][] wallpaper = new int[28][14];
    private final PhoneAppState.Profile state;
    private final CalculatorEngine calculator;
    private String soonApp = "", noteStatus = "Saved on this computer";
    private int x, y, w, h;
    private MultiLineEditBox notes;
    private long noteChangedAt;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE, MMM d");

    public IphoneScreen(String color) { this(color, false, null); }
    public IphoneScreen(String color,java.util.UUID device){this(color,false,PhoneAppState.key(Minecraft.getInstance().player.getUUID(),device));}
    protected IphoneScreen(String color, boolean samsung) {this(color,samsung,null);}
    protected IphoneScreen(String color, boolean samsung,String profileKey) {
        super(Component.literal(samsung ? "Samsung S26 Ultra" : "iPhone 18 Pro Max"));
        this.color = color;
        this.samsung = samsung;
        this.profileKey=profileKey;
        state = profileKey==null?new PhoneAppState.Profile():PhoneAppState.INSTANCE.profile(profileKey);
        if(profileKey!=null)PhoneAppState.INSTANCE.activate(state);
        calculator = state.calculator; homePage = preferences().rememberPage ? state.homePage : preferences().startPage; musicPage = state.musicPage;
        if (!preferences().lockOnOpen) page = Page.HOME;
        makeWallpaper();
    }
    public java.util.UUID deviceId(){return profileKey==null?null:java.util.UUID.fromString(profileKey.substring(profileKey.lastIndexOf('/')+1));}
    public String profileKey(){return profileKey;}
    public PhonePreferences preferences() { return state.settings; }
    public PhoneAppState.Profile appState() { return state; }
    public int[][] originalWallpaper() { return wallpaper; }
    public String clockText(LocalDateTime time, boolean statusBar) {
        String pattern = preferences().clock24 ? "HH:mm" : "h:mm";
        if (preferences().clockSeconds && !statusBar) pattern += ":ss";
        return DateTimeFormatter.ofPattern(pattern).format(time);
    }
    @Override protected void init() {
        h = Math.min(340, height - 12); w = Math.min(180, Math.round(h * .54f));
        x = (width - w) / 2; y = (height - h) / 2;
        makeControls();
        PhoneMessagesClient.sync(deviceId());
    }
    private void navigate(Page next) {
        if (page == Page.NOTES) saveNotes();
        page = next;
        if (next == Page.MUSIC) PhoneMusic.restoreSelection(state.musicTrack);
        changed(); clearWidgets(); makeControls();
    }
    private void goHome() {
        homePage = 0;
        navigate(Page.HOME);
    }
    private void soon(String name) { soonApp = name; navigate(Page.SOON); }
    private void makeControls() {
        notes = null;
        if (page == Page.LOCK) {
            key(x + 14, y + h - 48, w - 28, 20, "Tap to unlock", 0x663D3344, () -> navigate(Page.HOME));
            return;
        }
        addRenderableWidget(new PhoneKey(x + w / 2 - 28, y + h - 20, 56, 14, "Home", "Home screen", 0, 2, this::goHome));
        if (page != Page.HOME) key(x + 9, y + 30, 19, 17, "<", 0xFF343945, this::goHome);
        if (page == Page.HOME) {
            String[] apps = homePage == 1 ? new String[]{"Music", "Security", "Chromium"} : new String[]{"Calculator", "Notes", "Clock", "Flashlight", "Settings", "Camera", "Messages", "Gallery"};
            int[] colors = {0xFF5B6975, 0xFFB49243, 0xFF343943, 0xFF537C83, 0xFF6D7380, 0xFF747983, 0xFF3F8D63, 0xFF986481};
            int stride = Math.min(53, (h - 103) / 4), cell = (w - 18) / 2;
            for (int i = 0; i < apps.length; i++) {
                String app = apps[i]; int index = i;
                addRenderableWidget(new PhoneKey(x + 8 + (i % 2) * cell, y + 51 + (i / 2) * stride, cell - 2, stride - 3, app, app, colors[i], 1, () -> {
                    if (homePage == 1) { if(index==0)navigate(Page.MUSIC);else if(index==1)minecraft.setScreen(new SecurityCameraScreen(this));else minecraft.setScreen(new ChromiumScreen(this));return; }
                    switch (index) {
                        case 0 -> navigate(Page.CALCULATOR); case 1 -> navigate(Page.NOTES);
                        case 2 -> navigate(Page.CLOCK); case 3 -> navigate(Page.FLASHLIGHT);
                        case 4 -> minecraft.setScreen(new PhoneSettingsScreen(this, samsung ? "Samsung S26 Ultra" : "iPhone 18 Pro Max"));
                        case 5 -> minecraft.setScreen(new PhoneCameraScreen(this, samsung ? "Samsung S26 Ultra" : "iPhone 18 Pro Max"));
                        case 6 -> minecraft.setScreen(new PhoneMessagesScreen(this));
                        case 7 -> minecraft.setScreen(new PhoneGalleryScreen(this, this, samsung ? "Samsung S26 Ultra" : "iPhone 18 Pro Max"));
                        default -> soon(app);
                    }
                }));
            }
            key(x + 12, y + h - 38, 24, 15, "<", 0x66353542, () -> { homePage = 1 - homePage; changed(); clearWidgets(); makeControls(); });
            key(x + w - 36, y + h - 38, 24, 15, ">", 0x66353542, () -> { homePage = 1 - homePage; changed(); clearWidgets(); makeControls(); });
        } else if (page == Page.MUSIC) {
            var tracks = PhoneMusic.tracks();
            int rows = Math.max(2, Math.min(5, (h - 164) / 21));
            int pages = Math.max(1, (tracks.size() + rows - 1) / rows);
            musicPage = Math.min(musicPage, pages - 1);
            for (int i = 0; i < rows && musicPage * rows + i < tracks.size(); i++) {
                var track = tracks.get(musicPage * rows + i);
                String label = font.plainSubstrByWidth(track.title(), w - 28);
                addRenderableWidget(new PhoneKey(x + 11, y + 89 + i * 21, w - 22, 19, label, track.song().description().getString(), 0xFF394654, 0, () -> { PhoneMusic.play(track, preferences()); rememberMusic(); clearWidgets(); makeControls(); }));
            }
            key(x + 11, y + h - 69, 28, 17, "<", 0xFF343945, () -> { musicPage = Math.floorMod(musicPage - 1, pages); changed(); clearWidgets(); makeControls(); });
            key(x + w - 39, y + h - 69, 28, 17, ">", 0xFF343945, () -> { musicPage = (musicPage + 1) % pages; changed(); clearWidgets(); makeControls(); });
            int cell = (w - 22) / 4;
            String[] labels = {"<<", "Play", "Stop", ">>"};
            for (int i = 0; i < 4; i++) {
                int action = i;
                key(x + 10 + cell * i, y + h - 47, cell - 1, 20, labels[i], i == 1 ? 0xFF427B69 : 0xFF485568, () -> {
                    switch (action) { case 0 -> PhoneMusic.skip(-1, preferences()); case 1 -> PhoneMusic.playSelected(preferences()); case 2 -> PhoneMusic.stop(); case 3 -> PhoneMusic.skip(1, preferences()); }
                    rememberMusic();
                });
            }
        } else if (page == Page.NOTES) {
            notes = addRenderableWidget(new MultiLineEditBox(font, x + 11, y + 53, w - 22, h - 114, Component.literal("Write a note..."), Component.literal("Phone notes")));
            notes.setCharacterLimit(4000); notes.setValue(state.notes());
            notes.setValueListener(value -> { state.setNotes(value);changed(); noteStatus = "Unsaved changes"; noteChangedAt = System.currentTimeMillis(); });
            key(x + 11, y + h - 44, w - 22, 17, "Save note", 0xFF8A723E, this::saveNotes);
        } else if (page == Page.CALCULATOR) {
            String[][] commands = {{"C", "SIGN", "%", "/"}, {"7", "8", "9", "*"}, {"4", "5", "6", "-"}, {"1", "2", "3", "+"}, {"0", ".", "BACK", "="}};
            int cell = (w - 21) / 4, stride = (h - 125) / 5;
            for (int row = 0; row < 5; row++) for (int col = 0; col < 4; col++) {
                String command = commands[row][col];
                String label = switch (command) { case "SIGN" -> "\u00b1"; case "*" -> "\u00d7"; case "/" -> "\u00f7"; case "BACK" -> "DEL"; default -> command; };
                int shade = command.equals("=") ? 0xFFB67842 : command.equals("C") ? 0xFF785554 : col == 3 ? 0xFF65717E : 0xFF3B414D;
                key(x + 8 + col * (cell + 2), y + 99 + row * stride, cell, stride - 2, label, shade, () -> pressCalculator(command));
            }
        } else if (page == Page.CLOCK) {
            int cell = (w - 24) / 3;
            key(x + 10, y + h - 66, cell, 20, state.running() ? "Pause" : "Start", 0xFF426C62, () -> {
                state.toggle(); changed(); PhoneAppState.INSTANCE.save(); clearWidgets(); makeControls();
            });
            key(x + 12 + cell, y + h - 66, cell, 20, "Lap", 0xFF466478, () -> {
                state.lap(); changed(); PhoneAppState.INSTANCE.save();
            });
            key(x + 14 + cell * 2, y + h - 66, cell, 20, "Reset", 0xFF5A5865, () -> {
                state.reset(); changed(); PhoneAppState.INSTANCE.save(); clearWidgets(); makeControls();
            });
        } else if (page == Page.FLASHLIGHT) {
            key(x + 13, y + h - 65, w - 26, 23, IphoneLogicProcedure.Client.flashlight ? "Turn off" : "Turn on", 0xFF47737A, () -> {
                if (minecraft.player != null && minecraft.getConnection() != null) {
                    IphoneLogicProcedure.Client.flashlight = !IphoneLogicProcedure.Client.flashlight;
                    PacketDistributor.sendToServer(new PhoneFlashlightPayload(IphoneLogicProcedure.Client.flashlight));
                    clearWidgets(); makeControls();
                }
            });
        }
    }
    private void key(int x, int y, int width, int height, String label, int color, Runnable action) {
        addRenderableWidget(new PhoneKey(x, y, width, height, label, label, color, 0, action));
    }
    private void saveNotes() { noteStatus = PhoneAppState.INSTANCE.save() ? "Saved on this computer" : "Save failed - try again"; }
    private void changed() {
        state.homePage = homePage; state.musicPage = musicPage; PhoneAppState.INSTANCE.changed(state);
    }
    private void pressCalculator(String command) { calculator.press(command); changed(); }
    private void rememberMusic() {
        if (PhoneMusic.selected() != null) state.musicTrack = PhoneMusic.selected().id().toString();
        changed();
    }
    @Override public void tick() {
        super.tick(); PhoneAppState.INSTANCE.flushIfReady();
        if (preferences().notesAutosave && page == Page.NOTES && noteChangedAt!=0 && System.currentTimeMillis() - noteChangedAt >= 750){saveNotes();noteChangedAt=0;}
    }
    @Override public void removed() { changed(); PhoneAppState.INSTANCE.save(); }
    @Override public void onClose() { saveNotes(); if (preferences().stopMusicOnClose) PhoneMusic.stop(); super.onClose(); }
    @Override public boolean isPauseScreen() { return false; }

    /** Shared phone casing and status bar for standalone phone apps. */
    public void renderAppFrame(GuiGraphics g,int x,int y,int w,int h){drawPhoneFrame(g,x,y,w,h,false,false);}
    private void drawPhoneFrame(GuiGraphics g,int x,int y,int w,int h,boolean wallpaperVisible,boolean locked){
        g.fill(0, 0, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight(), 0x88090B12);
        int frame = samsung ? samsungFrame() : switch (color) { case "silver" -> 0xFF92989E; case "glacier" -> 0xFF587B89; case "black" -> 0xFF2C3038; default -> 0xFF653448; };
        g.fill(x + 4, y + 4, x + w + 4, y + h + 4, 0x88000000);
        rounded(g, x, y, w, h, frame); rounded(g, x + 2, y + 2, w - 4, h - 4, 0xFF0D0E15);
        int sx = x + 5, sy = y + 5, sw = w - 10, sh = h - 10;
        if (wallpaperVisible) {
            PhoneWallpapers.draw(g, sx, sy, sw, sh, preferences(), locked, wallpaper);
        } else g.fill(sx, sy, sx + sw, sy + sh, preferences().panel());
        // Stepped corner masks and the compact Dynamic Island echo the item texture.
        g.fill(sx, sy, sx + 3, sy + 3, 0xFF0D0E15); g.fill(sx + sw - 3, sy, sx + sw, sy + 3, 0xFF0D0E15);
        if (samsung) {
            g.fill(x + w / 2 - 2, y + 11, x + w / 2 + 2, y + 15, 0xFF080C13);
            g.fill(x + w / 2, y + 12, x + w / 2 + 1, y + 13, 0xFF203344);
        } else {
            rounded(g, x + w / 2 - 15, y + 9, 30, 10, 0xFF080A10);
            g.fill(x + w / 2 + 8, y + 12, x + w / 2 + 10, y + 14, 0xFF20323F);
        }
        LocalDateTime now = LocalDateTime.now();
        if (!locked) g.drawString(font, clockText(now, true), x + 9, y + 12, wallpaperVisible ? 0xFFF0E8ED : preferences().ink(), false);
        g.renderOutline(x + w - 24, y + 12, 13, 7, 0xFFC4CBD0);
        g.fill(x + w - 22, y + 14, x + w - 13, y + 17, 0xFFD4DBD6);
        if (IphoneLogicProcedure.Client.flashlight) g.fill(x + w - 29, y + 13, x + w - 27, y + 17, 0xFFFFDC83);
    }
    @Override public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        drawPhoneFrame(g,x,y,w,h,page==Page.LOCK||page==Page.HOME,page==Page.LOCK);
        LocalDateTime now = LocalDateTime.now();
        if (page == Page.LOCK) {
            if (preferences().lockDate) centered(g, DATE.format(now), y + h / 5, 0xFFECDDE3, 1);
            centered(g, clockText(now, false), y + h / 5 + 19, 0xFFF1DDE5, 2.7f);
            centered(g, samsung ? "Samsung S26 Ultra" : "iPhone 18 Pro Max", y + h - 78, 0xFFD9CBD5, 1);
            g.fill(x + w / 2 - 21, y + h - 17, x + w / 2 + 21, y + h - 15, 0xFFF0E3EA);
        } else if (page == Page.HOME) {
            g.drawString(font, "Home", x + 12, y + 35, 0xFFF1E6EB, false);
            centered(g, (homePage + 1) + " / 2", y + h - 34, 0xFFF1E6EB, 1);
        }
        else {
            String title = page == Page.SOON ? soonApp : switch (page) { case CALCULATOR -> "Calculator"; case NOTES -> "Notes"; case CLOCK -> "Clock"; case FLASHLIGHT -> "Flashlight"; case MUSIC -> "Music"; default -> ""; };
            centered(g, title, y + 34, 0xFFE5E9EC, 1);
            if (page == Page.MUSIC) {
                var track = PhoneMusic.selected();
                centered(g, track == null ? "Choose a disc" : track.title(), y + 54, 0xFFE1EFE7, 1.1f);
                String status = PhoneMusic.playing() ? "Playing" : "Stopped";
                if (minecraft.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.RECORDS) == 0 || minecraft.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MASTER) == 0) status = "Sound is muted";
                if (preferences().silent || preferences().musicVolume == 0) status = "Phone sound muted";
                centered(g, status + (track == null ? "" : " - " + PhoneMusic.elapsed() + " / " + PhoneMusic.duration()), y + 70, 0xFFA6C0CB, .85f);
                int rows = Math.max(2, Math.min(5, (h - 164) / 21));
                int pages = Math.max(1, (PhoneMusic.tracks().size() + rows - 1) / rows);
                centered(g, (musicPage + 1) + " / " + pages, y + h - 64, 0xFFB6C4D0, .85f);
            } else if (page == Page.CALCULATOR) {
                g.fill(x + 9, y + 52, x + w - 9, y + 92, preferences().card());
                g.drawString(font, font.plainSubstrByWidth(calculator.history(), w - 25), x + 12, y + 56, preferences().muted(), false);
                centered(g, calculator.display(), y + 72, calculator.isError() ? 0xFFEA9292 : 0xFFF2E9DC, 1.8f);
            } else if (page == Page.NOTES) centered(g, noteStatus, y + h - 56, 0xFFADB8C0, .85f);
            else if (page == Page.CLOCK) {
                centered(g, clockText(now, false), y + 55, 0xFFE5E9EB, 1.8f);
                centered(g, DATE.format(now), y + 75, 0xFFA8B5C4, .9f);
                centered(g, "Stopwatch", y + 97, 0xFFB5C3D1, 1);
                centered(g, stopwatchTime(state.elapsedMillis()), y + 112, 0xFFD8E8E5, 1.55f);
                centered(g, state.running() ? "Running in background" : "Paused", y + 134, 0xFF86BBAA, .8f);
                var laps = state.laps();
                int rows = Math.max(0, (h - 221) / 15);
                for (int row = 0; row < Math.min(rows, laps.size()); row++) {
                    int lap = laps.size() - 1 - row;
                    centered(g, "Lap " + (lap + 1) + "   " + stopwatchTime(laps.get(lap)), y + 155 + row * 15, 0xFFB8C7D5, .9f);
                }
                if (preferences().worldClock && minecraft.level != null) {
                    long minutes = Math.floorMod(minecraft.level.getDayTime() + 6000, 24000) * 60 / 1000;
                    centered(g, String.format("World %02d:%02d", minutes / 60, minutes % 60), y + h - 38, 0xFFA4B4C0, .9f);
                }
            } else if (page == Page.FLASHLIGHT) {
                int beam = IphoneLogicProcedure.Client.flashlight ? 0xFFFFDE94 : 0xFF59616F;
                g.fill(x + w / 2 - 8, y + 69, x + w / 2 + 8, y + 82, beam);
                g.fill(x + w / 2 - 4, y + 82, x + w / 2 + 4, y + 101, 0xFFBAC8CC);
                centered(g, IphoneLogicProcedure.Client.flashlight ? "ON" : "OFF", y + 111, beam, 1.2f);
                g.drawWordWrap(font, Component.literal("Brightens your view while held."), x + 14, y + 133, w - 28, preferences().muted());
            } else if (page == Page.SOON) {
                centered(g, "Releasing Soon.", y + h / 2, 0xFFDED2E7, 1.15f);
            }
        }
    }
    private static String stopwatchTime(long millis) {
        return String.format(java.util.Locale.ROOT, "%02d:%02d.%02d", millis / 60000, millis / 1000 % 60, millis / 10 % 100);
    }
    private void centered(GuiGraphics g, String text, int yy, int color, float scale) {
        if (page != Page.HOME && page != Page.LOCK && !preferences().darkTheme && color != 0xFFEA9292) color = preferences().ink();
        scale = Math.min(scale * preferences().textScale(), (w - 22f) / Math.max(1, font.width(text)));
        g.pose().pushPose(); g.pose().translate(x + w / 2f, yy, 0); g.pose().scale(scale, scale, 1);
        g.drawString(font, text, -font.width(text) / 2, 0, color, false); g.pose().popPose();
    }
    private static void rounded(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x + 3, y, x + w - 3, y + h, color); g.fill(x, y + 3, x + w, y + h - 3, color);
    }
    @Override public boolean charTyped(char c, int modifiers) {
        if (page == Page.CALCULATOR) {
            if (c >= '0' && c <= '9' || ".+-*/%=".indexOf(c) >= 0) { pressCalculator(String.valueOf(c)); return true; }
            if (c == 'c' || c == 'C') { pressCalculator("C"); return true; }
        }
        return super.charTyped(c, modifiers);
    }
    @Override public boolean keyPressed(int key, int scan, int mods) {
        if (page == Page.HOME && (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT)) { homePage = 1 - homePage; changed(); clearWidgets(); makeControls(); return true; }
        if (key == GLFW.GLFW_KEY_ESCAPE && page != Page.HOME && page != Page.LOCK) { goHome(); return true; }
        if (page == Page.CALCULATOR) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { pressCalculator("="); return true; }
            if (key == GLFW.GLFW_KEY_BACKSPACE) { pressCalculator("BACK"); return true; }
            if (key == GLFW.GLFW_KEY_DELETE) { pressCalculator("C"); return true; }
        }
        return super.keyPressed(key, scan, mods);
    }
    private int samsungFrame() {
        return switch (color) {
            case "cobalt_violet" -> 0xFF51496C; case "sky_blue" -> 0xFF92AFC7;
            case "white" -> 0xFFCFD1D0; case "pink_gold" -> 0xFFBFA397;
            case "silver_shadow" -> 0xFF92979F; default -> 0xFF34373B;
        };
    }
    private void makeSamsungWallpaper() {
        int base = switch(color) {
            case "cobalt_violet" -> 0x756392; case "sky_blue" -> 0x8EB8D8;
            case "white" -> 0xBFC8CB; case "pink_gold" -> 0xC19A88;
            case "silver_shadow" -> 0x87949F; default -> 0x536173;
        };
        for (int row=0;row<28;row++) for(int col=0;col<14;col++) {
            double bend=Math.round(7+4*Math.sin((row/27.0-.13)*Math.PI*2));
            double light = (row<8 ? .88 : row<13 ? 1.13 : row<18 ? .95 : row<23 ? .66 : .46);
            if(col>=bend)light*=.70;
            if(Math.abs(col-bend)<1)light*=1.08;
            int r=Math.min(255,(int)(((base>>16)&255)*light));
            int green=Math.min(255,(int)(((base>>8)&255)*light));
            int b=Math.min(255,(int)((base&255)*light));
            wallpaper[row][col]=0xFF000000|r<<16|green<<8|b;
        }
    }
    private void makeWallpaper() {
        if(samsung){makeSamsungWallpaper();return;}
        int[][] palette = {{81,32,55},{132,55,80},{192,105,120},{107,52,79},{42,31,56}};
        for (int row = 0; row < 28; row++) for (int col = 0; col < 14; col++) {
            double curve = 5 + 3 * Math.sin((row / 27.0) * Math.PI * 1.4);
            int[] rgb = palette[Math.min(4, row / 6)].clone();
            if (col > curve) for (int i = 0; i < 3; i++) rgb[i] = (int)(rgb[i] * .68);
            if (Math.abs(col - curve) < 1) for (int i = 0; i < 3; i++) rgb[i] = Math.min(255, (int)(rgb[i] * 1.12));
            double light = rgb[0] * .2126 + rgb[1] * .7152 + rgb[2] * .0722;
            if (color.equals("black")) rgb = new int[]{(int)(light*.65),(int)(light*.67),(int)(light*.72)};
            if (color.equals("silver")) rgb = new int[]{(int)(light*.92),(int)(light*.95),(int)light};
            if (color.equals("glacier")) rgb = new int[]{(int)(light*.50),(int)(light*.81+8),(int)(light*.96+16)};
            wallpaper[row][col] = 0xFF000000 | rgb[0]<<16 | rgb[1]<<8 | rgb[2];
        }
    }

    private final class PhoneKey extends Button {
        private final int color, style;
        PhoneKey(int x, int y, int w, int h, String label, String tip, int color, int style, Runnable action) {
            super(x,y,w,h,Component.literal(label),button -> action.run(),DEFAULT_NARRATION);
            this.color=color; this.style=style; setTooltip(Tooltip.create(Component.literal(tip)));
        }
        @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { PhoneWallpapers.click(manager,preferences()); }
        @Override protected void renderWidget(GuiGraphics g, int mx, int my, float tick) {
            var font = Minecraft.getInstance().font;
            int x=getX(),y=getY(),w=getWidth(),h=getHeight();
            if (style==2) { rounded(g,x+7,y+6,w-14,3,isHoveredOrFocused()?0xFFFFFFFF:0xFFD3D6DD); return; }
            if (style==1) {
                int size=Math.min(32,h-12),cx=x+(w-size)/2;
                String label=getMessage().getString();
                // Native 16px sprites use integer scaling so every icon stays crisp.
                int iconSize = 16 * Math.max(1, size / 16);
                cx=x+(w-iconSize)/2;
                g.fill(cx+2,y+2,cx+iconSize+2,y+iconSize+2,0x44000000);
                PhoneAppIcons.draw(g,label,cx,y,iconSize/16);
                int unread=label.equals("Messages")?PhoneMessagesClient.unread(deviceId()):0;
                if(unread>0){String badge=unread>99?"99+":Integer.toString(unread);int bw=Math.max(9,font.width(badge)+4);g.fill(cx+iconSize-bw+2,y-2,cx+iconSize+3,y+8,0xFFCB4D5B);g.drawString(font,badge,cx+iconSize-bw+4,y-1,0xFFFFFFFF,false);}
                if (preferences().iconLabels) {
                    var message = Component.literal(label);
                    if (preferences().boldLabels) message.withStyle(net.minecraft.ChatFormatting.BOLD);
                    float scale = Math.min(preferences().textScale(), (w-4f)/Math.max(1,font.width(message)));
                    g.pose().pushPose(); g.pose().translate(x+w/2,y+h-9,0); g.pose().scale(scale,scale,1);
                    g.drawString(font,message,-font.width(message)/2,0,0xFFEEE9ED,false);g.pose().popPose();
                }
                if(isHoveredOrFocused())g.renderOutline(cx,y,iconSize,iconSize,0xFFE3D9EC);
            } else {
                int shade=page!=Page.HOME&&page!=Page.LOCK ? preferences().card() : color;
                rounded(g,x,y,w,h,shade);
                if(isHoveredOrFocused())g.renderOutline(x,y,w,h,0xFFCFD9E4);
                float scale=Math.min(preferences().textScale(),(w-4f)/Math.max(1,font.width(getMessage())));
                g.pose().pushPose();g.pose().translate(x+w/2,y+Math.round((h-font.lineHeight*scale)/2f),0);g.pose().scale(scale,scale,1);
                g.drawString(font,getMessage(),-font.width(getMessage())/2,0,page==Page.HOME||page==Page.LOCK?0xFFF0EAF1:preferences().ink(),false);g.pose().popPose();
            }
        }
    }
}
