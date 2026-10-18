package net.mcreator.commandcookieselectronicmod.guide;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.mcreator.commandcookieselectronicmod.guide.GuideCue.Action;
import org.lwjgl.glfw.GLFW;

/**
 * CEM's setup assistant: an animated scene beside numbered step cards, with a segmented
 * progress bar and Back / Play-Pause / Next / Replay. Wide windows show the full step list;
 * narrow or short windows stack the scene above a single step card.
 */
public final class CemGuideScreen extends Screen {
    private static final String LANG = "commandcookies_electronic_mod.guide.";
    private static final int HEADER = 20, FOOTER = 26, PAD = 6;
    /** Back restarts the current step when it has been playing for longer than this. */
    private static final int RESTART_STEP_TICKS = 20;

    private final Screen parent;
    private final GuideLesson lesson;
    private final List<GuideLesson.Step> steps;
    private GuideScene scene;
    private GuideSceneView view;
    private boolean playing = true, dragging;
    private int scrollStep = -1, cardScroll, cardScrollMax;
    /** Fraction of a tick elapsed since the last tick, summed from vanilla's per-frame delta. */
    private float sinceTick, frozenPartial;

    private GuideButton close, back, play, next, replay;
    private int panelX, panelY, panelW, panelH;
    private boolean compact;
    /** Top edge of each clickable step row as last drawn; -1 when not shown. */
    private final int[] rowTops;
    private int sideX, sideY, sideW, sideH, barX, barY, barW;

    /** Opens the wall-mounted TV lesson, the guide's original scene. */
    public CemGuideScreen(Screen parent) {
        this(parent, GuideLessons.get(GuideLessons.WALL_TV));
    }

    public CemGuideScreen(Screen parent, GuideLesson lesson) {
        super(Component.translatable(LANG + "header"));
        this.parent = parent;
        this.lesson = lesson;
        steps = lesson.steps();
        rowTops = new int[steps.size()];
    }

    public GuideLesson lesson() {
        return lesson;
    }

    @Override
    protected void init() {
        if (scene == null) {
            scene = new GuideScene(lesson);
            view = new GuideSceneView(scene);
        }
        close = addRenderableWidget(new GuideButton(GuideButton.Icon.CLOSE, Component.translatable(LANG + "close"), null, this::onClose));
        back = addRenderableWidget(new GuideButton(GuideButton.Icon.BACK, Component.translatable(LANG + "back"), Component.translatable(LANG + "back.hint"), this::back));
        play = addRenderableWidget(new GuideButton(GuideButton.Icon.PAUSE, Component.translatable(LANG + "pause"), Component.translatable(LANG + "play.hint"), this::togglePlay).primary(true));
        next = addRenderableWidget(new GuideButton(GuideButton.Icon.NEXT, Component.translatable(LANG + "next"), Component.translatable(LANG + "next.hint"), this::next));
        replay = addRenderableWidget(new GuideButton(GuideButton.Icon.REPLAY, Component.translatable(LANG + "replay"), Component.translatable(LANG + "replay.hint"), this::replay));
        layout();
    }

    private void layout() {
        panelW = Math.min(width - 12, Math.min(960, Math.max(460, Math.round(width * .92f))));
        panelH = Math.min(height - 12, Math.min(540, Math.max(280, Math.round(height * .9f))));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        compact = panelW < 340 || panelH < 200;

        int bodyTop = panelY + HEADER + PAD, bodyBottom = panelY + panelH - FOOTER - PAD;
        int bodyH = bodyBottom - bodyTop;
        if (compact) {
            sideH = Mth.clamp(bodyH * 2 / 5, 52, 90);
            sideX = panelX + PAD;
            sideW = panelW - PAD * 2;
            sideY = bodyBottom - sideH;
            view.bounds(panelX + PAD, bodyTop, panelW - PAD * 2, bodyH - sideH - 4);
        } else {
            sideW = Mth.clamp(panelW * 37 / 100, 124, 260);
            sideX = panelX + panelW - PAD - sideW;
            sideY = bodyTop;
            sideH = bodyH;
            view.bounds(panelX + PAD, bodyTop, sideX - 6 - panelX - PAD, bodyH);
        }

        close.place(panelX + panelW - 17, panelY + 3, 14, 14, false);
        boolean icons = panelW < 300;
        int buttonW = icons ? 22 : 52, gap = 3, y = panelY + panelH - FOOTER + 5;
        int x = panelX + panelW - PAD - (buttonW * 4 + gap * 3);
        for (var button : List.of(back, play, next, replay)) {
            button.place(x, y, buttonW, 16, !icons);
            x += buttonW + gap;
        }
        barX = panelX + PAD;
        barY = y + 6;
        barW = back.getX() - 8 - barX;
        refreshPlayLabel();
    }

    private void refreshPlayLabel() {
        var icon = ended() ? GuideButton.Icon.REPLAY : playing ? GuideButton.Icon.PAUSE : GuideButton.Icon.PLAY;
        play.icon(icon, Component.translatable(LANG + (ended() ? "replay" : playing ? "pause" : "play")));
    }

    // Timeline -------------------------------------------------------------------------------

    private int stepCount() {
        return Math.min(steps.size(), scene.getKeyframeCount());
    }

    private int currentStep() {
        int step = 0;
        for (int i = 0; i < stepCount(); i++) if (scene.getKeyframeTime(i) <= scene.getCurrentTime()) step = i;
        return step;
    }

    private int stepStart(int i) {
        return i < stepCount() ? scene.getKeyframeTime(i) : scene.getTotalTime();
    }

    private boolean ended() {
        return scene.getCurrentTime() >= scene.getTotalTime();
    }

    /** Seeks the timeline; the scene derives its state from the time, in either direction. */
    private void seek(int time) {
        scene.seekToTime(Math.min(time, scene.getTotalTime()));
        sinceTick = 0;
        frozenPartial = 0;
        refreshPlayLabel();
    }

    private void jumpTo(int step) {
        seek(stepStart(Mth.clamp(step, 0, Math.max(0, stepCount() - 1))));
    }

    private void back() {
        int step = currentStep();
        jumpTo(scene.getCurrentTime() - stepStart(step) > RESTART_STEP_TICKS || step == 0 ? step : step - 1);
    }

    private void next() {
        int step = currentStep();
        if (step + 1 < stepCount()) jumpTo(step + 1);
        else seek(scene.getTotalTime());
    }

    private void replay() {
        scene.begin();
        sinceTick = 0;
        frozenPartial = 0;
        playing = true;
        refreshPlayLabel();
    }

    private void togglePlay() {
        if (ended()) {
            replay();
            return;
        }
        if (playing) {
            frozenPartial = Math.min(sinceTick, 1);
            playing = false;
        } else {
            // Resume from the frozen fraction so the scene does not jump.
            sinceTick = frozenPartial;
            playing = true;
        }
        refreshPlayLabel();
    }

    @Override
    public void tick() {
        super.tick();
        // The scene keeps ticking at the end so cables and pulses stay animated.
        if (playing) scene.tick();
        sinceTick = 0;
        back.active = scene.getCurrentTime() > 0;
        next.active = !ended();
        refreshPlayLabel();
    }

    private float partial() {
        return playing ? Math.min(sinceTick, 1) : frozenPartial;
    }

    // Rendering ------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        // Screen#render receives vanilla's real-time frame delta in ticks; Screen#tick resets the sum.
        if (playing) sinceTick += partialTick;
        float pt = partial();
        drawFrame(g);
        var v = viewRect();
        g.fill(v[0], v[1], v[0] + v[2], v[1] + v[3], GuideTheme.WELL);
        g.fillGradient(v[0], v[1] + v[3] / 2, v[0] + v[2], v[1] + v[3], GuideTheme.WELL, GuideTheme.WELL_DEEP);
        view.render(g, pt);
        g.renderOutline(v[0] - 1, v[1] - 1, v[2] + 2, v[3] + 2, GuideTheme.BORDER_DIM);
        drawMarker(g, pt);
        g.drawString(font, Component.translatable(LANG + "drag_hint"), v[0] + 4, v[1] + v[3] - 11, GuideTheme.MUTED, false);
        if (compact) drawCard(g); else drawStepList(g);
        drawProgress(g, pt);
        for (var widget : renderables) widget.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(g);
    }

    private int[] viewRect() {
        int top = panelY + HEADER + PAD;
        return compact
            ? new int[]{panelX + PAD, top, panelW - PAD * 2, sideY - 4 - top}
            : new int[]{panelX + PAD, top, sideX - 6 - panelX - PAD, sideH};
    }

    private void drawFrame(GuiGraphics g) {
        int x1 = panelX + panelW, y1 = panelY + panelH;
        g.fill(panelX + 3, panelY + 3, x1 + 3, y1 + 3, GuideTheme.SHADOW);
        g.fill(panelX, panelY, x1, y1, GuideTheme.PANEL);
        g.renderOutline(panelX, panelY, panelW, panelH, GuideTheme.BORDER);
        g.fill(panelX + 1, panelY + 1, x1 - 1, panelY + HEADER, GuideTheme.HEADER);
        g.fill(panelX + 1, panelY + HEADER, x1 - 1, panelY + HEADER + 2, GuideTheme.ACCENT);
        // Header: a small power-plug badge, the product label and the guide's name.
        g.fill(panelX + 6, panelY + 5, panelX + 16, panelY + 15, GuideTheme.ACCENT);
        g.fill(panelX + 8, panelY + 7, panelX + 10, panelY + 10, GuideTheme.ON_ACCENT);
        g.fill(panelX + 12, panelY + 7, panelX + 14, panelY + 10, GuideTheme.ON_ACCENT);
        g.fill(panelX + 9, panelY + 11, panelX + 13, panelY + 13, GuideTheme.ON_ACCENT);
        var header = Component.translatable(LANG + "header");
        g.drawString(font, header, panelX + 21, panelY + 6, GuideTheme.ACCENT, false);
        int nameX = panelX + 21 + font.width(header) + 8;
        int room = panelX + panelW - 22 - nameX;
        if (room > 20) g.drawString(font, font.plainSubstrByWidth(lesson.title().getString(), room), nameX, panelY + 6, GuideTheme.TEXT, false);
        g.fill(panelX + 1, panelY + panelH - FOOTER, x1 - 1, panelY + panelH - FOOTER + 1, GuideTheme.BORDER_DIM);
    }

    private void drawStepList(GuiGraphics g) {
        int current = currentStep(), count = stepCount();
        g.fill(sideX, sideY, sideX + sideW, sideY + sideH, GuideTheme.WELL);
        g.drawString(font, Component.translatable(LANG + "step_of", current + 1, count), sideX + 6, sideY + 5, GuideTheme.ACCENT, false);
        int y = sideY + 18, textX = sideX + 22, textW = sideW - 28;
        var body = font.split(steps.get(current).body(), textW);
        java.util.Arrays.fill(rowTops, -1);
        // Collapse to the current card when the list and its text cannot both fit.
        if ((count - 1) * 15 + 16 + body.size() * 10 + actionHeight(textW) + 6 > sideH - 22) {
            drawCurrent(g, sideX + 6, y, sideW - 12, current);
            return;
        }
        cardScrollMax = 0;
        for (int i = 0; i < count; i++) {
            if (i != current && i < rowTops.length) rowTops[i] = y;
            badge(g, sideX + 6, y, i, current);
            int color = i == current ? GuideTheme.TEXT : i < current ? GuideTheme.LABEL : GuideTheme.MUTED;
            var title = steps.get(i).title();
            g.drawString(font, font.plainSubstrByWidth(title.getString(), textW), textX, y + 2, color, false);
            if (i == current) {
                g.fill(sideX + 1, y - 2, sideX + 3, y + 14 + body.size() * 10 + 24, GuideTheme.ACCENT);
                int line = y + 15;
                for (var text : body) {
                    g.drawString(font, text, textX, line, GuideTheme.LABEL, false);
                    line += 10;
                }
                y = actionChip(g, textX, line + 3, textW) + 6;
            } else y += 15;
        }
    }

    /** Single-card layout for narrow windows: step badge, title, text and the live action. */
    private void drawCard(GuiGraphics g) {
        g.fill(sideX, sideY, sideX + sideW, sideY + sideH, GuideTheme.WELL);
        drawCurrent(g, sideX + 5, sideY + 5, sideW - 10, currentStep());
    }

    private void drawCurrent(GuiGraphics g, int x, int y, int w, int step) {
        badge(g, x, y, step, step);
        g.drawString(font, font.plainSubstrByWidth(steps.get(step).title().getString(), w - 18), x + 16, y + 2, GuideTheme.TEXT, false);
        int top = y + 15, bottom = sideY + sideH - 4, textW = w - 6;
        var lines = font.split(steps.get(step).body(), textW);
        int content = lines.size() * 10 + 3 + actionHeight(textW);
        if (scrollStep != step) { scrollStep = step; cardScroll = 0; }
        cardScrollMax = Math.max(0, content - Math.max(0, bottom - top));
        cardScroll = Mth.clamp(cardScroll, 0, cardScrollMax);
        g.enableScissor(x, top, x + w, bottom);
        try {
            int lineY = top - cardScroll;
            for (var text : lines) {
                g.drawString(font, text, x, lineY, GuideTheme.LABEL, false);
                lineY += 10;
            }
            actionChip(g, x, lineY + 3, textW);
        } finally { g.disableScissor(); }
        if (cardScrollMax > 0) {
            int available = Math.max(1, bottom - top), thumb = Math.max(6, available * available / Math.max(1, content));
            int thumbY = top + (available - thumb) * cardScroll / cardScrollMax;
            g.fill(x + w - 2, top, x + w, bottom, GuideTheme.CONTROL);
            g.fill(x + w - 2, thumbY, x + w, thumbY + thumb, GuideTheme.ACCENT);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (cardScrollMax > 0 && mx >= sideX && mx < sideX + sideW && my >= sideY && my < sideY + sideH) {
            cardScroll = Mth.clamp(cardScroll - (int) Math.round(vertical * 20), 0, cardScrollMax);
            return true;
        }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }

    private Component actionText(GuideCue cue) {
        var text = Component.translatable(LANG + "action." + cue.action().name().toLowerCase(java.util.Locale.ROOT));
        var detail = cue.label() != null ? cue.label() : cue.item().isEmpty() ? null : cue.item().getHoverName();
        return detail == null ? text : text.copy().append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY)).append(detail);
    }

    private int actionHeight(int width) {
        var cue = cue();
        if (cue == null || !cue.active() || cue.action() == Action.NONE) return 0;
        int iconsW = (cue.action().mouse ? 13 : 0) + (cue.item().isEmpty() ? 0 : 18);
        return Math.max(20, font.split(actionText(cue), Math.max(30, width - iconsW - 8)).size() * 10 + 6);
    }

    private void badge(GuiGraphics g, int x, int y, int step, int current) {
        boolean done = step < current, now = step == current;
        g.fill(x, y, x + 12, y + 12, now ? GuideTheme.ACCENT : done ? GuideTheme.GOOD : GuideTheme.CONTROL);
        if (!now && !done) g.renderOutline(x, y, 12, 12, GuideTheme.BORDER_DIM);
        String mark = done ? "✓" : String.valueOf(step + 1);
        g.drawString(font, mark, x + 6 - font.width(mark) / 2, y + 2, now || done ? GuideTheme.ON_ACCENT : GuideTheme.LABEL, false);
    }

    /** The action happening right now in the scene. Returns the bottom edge. */
    private int actionChip(GuiGraphics g, int x, int y, int w) {
        var cue = cue();
        if (cue == null || !cue.active() || cue.action() == Action.NONE) return y;
        var text = actionText(cue);
        int iconsW = (cue.action().mouse ? 13 : 0) + (cue.item().isEmpty() ? 0 : 18);
        var lines = font.split(text, Math.max(30, w - iconsW - 8));
        int h = Math.max(20, lines.size() * 10 + 6);
        g.fill(x, y, x + w, y + h, GuideTheme.CONTROL);
        g.renderOutline(x, y, w, h, GuideTheme.BORDER_DIM);
        int ix = x + 3;
        if (cue.action().mouse) {
            GuideTheme.mouse(g, ix, y + (h - 13) / 2, true, cue.action().left);
            ix += 12;
        }
        if (!cue.item().isEmpty()) {
            g.renderItem(cue.item(), ix, y + (h - 16) / 2);
            ix += 18;
        }
        int ty = y + (h - lines.size() * 10) / 2 + 1;
        for (var line : lines) {
            g.drawString(font, line, ix + 2, ty, GuideTheme.TEXT, false);
            ty += 10;
        }
        return y + h;
    }

    /** Yellow corner brackets on the scene target, with a small mouse/item tag beside it. */
    private void drawMarker(GuiGraphics g, float pt) {
        var cue = cue();
        if (cue == null || !cue.active()) return;
        var p = view.project(cue.target(), pt);
        var v = viewRect();
        int cx = Math.round(p.x), cy = Math.round(p.y);
        if (cx < v[0] || cy < v[1] || cx >= v[0] + v[2] || cy >= v[1] + v[3]) return;
        float wave = (cue.age() + pt) / 6f;
        int r = 6 + Math.round(2 * Mth.sin(wave));
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        GuideTheme.brackets(g, cx, cy, r, GuideTheme.ACCENT);
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, GuideTheme.ACCENT);
        int tagW = Math.max(12, 4 + (cue.action().mouse ? 11 : 0) + (cue.item().isEmpty() ? 0 : 17));
        int tx = Mth.clamp(cx + r + 4, v[0] + 2, v[0] + v[2] - tagW - 2), ty = Mth.clamp(cy - r - 20, v[1] + 2, v[1] + v[3] - 22);
        g.fill(tx, ty, tx + tagW, ty + 20, GuideTheme.PANEL);
        g.renderOutline(tx, ty, tagW, 20, GuideTheme.ACCENT);
        int ix = tx + 2;
        if (cue.action().mouse) {
            GuideTheme.mouse(g, ix, ty + 4, true, cue.action().left);
            if (cue.action().sneaks()) g.fill(ix, ty + 18, ix + 9, ty + 19, GuideTheme.ACCENT);
            ix += 11;
        }
        if (!cue.item().isEmpty()) g.renderItem(cue.item(), ix, ty + 2);
        g.pose().popPose();
    }

    /** One segment per step; the current one fills as its part of the timeline plays. */
    private void drawProgress(GuiGraphics g, float pt) {
        int count = Math.max(1, stepCount()), current = currentStep(), gap = 2;
        if (barW < count * 6) return;
        float time = Math.min(scene.getTotalTime(), scene.getCurrentTime() + (playing && !ended() ? pt : 0));
        int segment = (barW - gap * (count - 1)) / count;
        for (int i = 0; i < count; i++) {
            int x = barX + i * (segment + gap);
            g.fill(x, barY, x + segment, barY + 4, GuideTheme.CONTROL);
            float start = stepStart(i), end = stepStart(i + 1);
            float fraction = i < current ? 1 : i > current ? 0 : end <= start ? 1 : Mth.clamp((time - start) / (end - start), 0, 1);
            if (fraction > 0) g.fill(x, barY, x + Math.round(segment * fraction), barY + 4, i < current ? GuideTheme.GOOD : GuideTheme.ACCENT);
        }
    }

    private GuideCue cue() {
        return scene.cue();
    }

    // Input ----------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0) return false;
        int step = stepAt(mouseX, mouseY);
        if (step >= 0) {
            jumpTo(step);
            return true;
        }
        dragging = view.contains(mouseX, mouseY);
        return dragging;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging && button == 0) {
            view.rotate(dx, dy);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    /** Progress segments always, and step rows in the full list, jump to that step. */
    private int stepAt(double mx, double my) {
        int count = stepCount();
        if (my >= barY - 3 && my < barY + 7 && mx >= barX && mx < barX + barW && barW >= count * 6) {
            int segment = (barW - 2 * (count - 1)) / count;
            return Mth.clamp((int) ((mx - barX) / (segment + 2)), 0, count - 1);
        }
        if (compact || mx < sideX || mx >= sideX + sideW) return -1;
        for (int i = 0; i < rowTops.length; i++)
            if (rowTops[i] >= 0 && my >= rowTops[i] - 2 && my < rowTops[i] + 13) return i;
        return -1;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        switch (key) {
            case GLFW.GLFW_KEY_LEFT -> back();
            case GLFW.GLFW_KEY_RIGHT -> {
                if (!ended()) next();
            }
            case GLFW.GLFW_KEY_SPACE -> togglePlay();
            case GLFW.GLFW_KEY_R -> replay();
            default -> {
                return super.keyPressed(key, scan, modifiers);
            }
        }
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    /** Resizing only re-runs init and keeps the playhead and view angle; closing drops both. */
    @Override
    public void removed() {
        scene = null;
        view = null;
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
