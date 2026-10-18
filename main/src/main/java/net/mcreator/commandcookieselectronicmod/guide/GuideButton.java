package net.mcreator.commandcookieselectronicmod.guide;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Flat CEM control: dark plate, thin border, yellow edge when hovered; a pixel icon and an optional label. */
final class GuideButton extends AbstractButton {
    enum Icon { BACK, PLAY, PAUSE, NEXT, REPLAY, CLOSE }

    private static final int ICON = 7;
    private final Runnable action;
    private Icon icon;
    private boolean primary, showLabel = true;

    GuideButton(Icon icon, Component label, Component tooltip, Runnable action) {
        super(0, 0, 40, 16, label);
        this.icon = icon;
        this.action = action;
        if (tooltip != null) setTooltip(Tooltip.create(tooltip));
    }

    GuideButton primary(boolean primary) {
        this.primary = primary;
        return this;
    }

    void icon(Icon icon, Component label) {
        this.icon = icon;
        setMessage(label);
    }

    void place(int x, int y, int width, int height, boolean showLabel) {
        setX(x);
        setY(y);
        this.width = width;
        this.height = height;
        this.showLabel = showLabel;
    }

    @Override
    public void onPress() {
        action.run();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean hot = active && isHoveredOrFocused();
        int fill = !active ? GuideTheme.WELL : primary ? (hot ? 0xFFF2CB6B : GuideTheme.ACCENT) : hot ? GuideTheme.CONTROL_HOVER : GuideTheme.CONTROL;
        g.fill(getX(), getY(), getX() + width, getY() + height, fill);
        g.renderOutline(getX(), getY(), width, height, active && hot ? GuideTheme.ACCENT : GuideTheme.BORDER_DIM);
        int color = !active ? GuideTheme.MUTED : primary ? GuideTheme.ON_ACCENT : GuideTheme.TEXT;
        var font = Minecraft.getInstance().font;
        String label = showLabel ? getMessage().getString() : "";
        int room = width - ICON - 10;
        if (font.width(label) > room) label = room > 12 ? font.plainSubstrByWidth(label, room) : "";
        int content = ICON + (label.isEmpty() ? 0 : 4 + font.width(label));
        int x = getX() + (width - content) / 2, y = getY() + (height - ICON) / 2;
        drawIcon(g, icon, x, y, color);
        if (!label.isEmpty()) g.drawString(font, label, x + ICON + 4, getY() + (height - 8) / 2, color, false);
    }

    static void drawIcon(GuiGraphics g, Icon icon, int x, int y, int c) {
        switch (icon) {
            case PLAY -> triangle(g, x + 1, y, true, c);
            case NEXT -> { triangle(g, x, y, true, c); g.fill(x + 5, y, x + 7, y + 7, c); }
            case BACK -> { g.fill(x, y, x + 2, y + 7, c); triangle(g, x + 3, y, false, c); }
            case PAUSE -> { g.fill(x + 1, y, x + 3, y + 7, c); g.fill(x + 4, y, x + 6, y + 7, c); }
            case CLOSE -> { for (int i = 0; i < 7; i++) { g.fill(x + i, y + i, x + i + 1, y + i + 1, c); g.fill(x + 6 - i, y + i, x + 7 - i, y + i + 1, c); } }
            case REPLAY -> {
                // An open ring with an arrowhead at its top-right end.
                g.fill(x + 1, y, x + 4, y + 1, c);
                g.fill(x, y + 1, x + 1, y + 6, c);
                g.fill(x + 1, y + 6, x + 6, y + 7, c);
                g.fill(x + 6, y + 3, x + 7, y + 6, c);
                g.fill(x + 4, y - 1, x + 5, y + 2, c);
                g.fill(x + 5, y, x + 6, y + 1, c);
            }
        }
    }

    private static void triangle(GuiGraphics g, int x, int y, boolean right, int c) {
        for (int r = 0; r < 7; r++) {
            int len = 4 - Math.abs(r - 3);
            if (right) g.fill(x, y + r, x + len, y + r + 1, c);
            else g.fill(x + 4 - len, y + r, x + 4, y + r + 1, c);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
