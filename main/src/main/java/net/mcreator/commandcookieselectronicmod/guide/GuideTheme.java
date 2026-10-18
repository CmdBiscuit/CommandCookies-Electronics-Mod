package net.mcreator.commandcookieselectronicmod.guide;

import net.minecraft.client.gui.GuiGraphics;

/** Colours shared with CEM's own machine screens (see GeneratorScreen), plus small drawing helpers. */
final class GuideTheme {
    static final int SHADOW = 0x66000000;
    static final int PANEL = 0xFF1B2A34;
    static final int HEADER = 0xFF293C46;
    static final int WELL = 0xFF131F27;
    static final int WELL_DEEP = 0xFF0D1820;
    static final int CONTROL = 0xFF263743;
    static final int CONTROL_HOVER = 0xFF33495A;
    static final int BORDER = 0xFF60717A;
    static final int BORDER_DIM = 0xFF3D535E;
    static final int ACCENT = 0xFFE6B84D;
    static final int GOOD = 0xFF82DFA3;
    static final int TEXT = 0xFFEAF1DE;
    static final int LABEL = 0xFFA8BFC4;
    static final int MUTED = 0xFF6F858C;
    static final int ON_ACCENT = 0xFF1B2A34;

    private GuideTheme() {}

    /** A two-button mouse, 9x13, with the right button lit. */
    static void mouse(GuiGraphics g, int x, int y, boolean lit) {
        mouse(g, x, y, lit, false);
    }

    /** As above; {@code left} lights the left button instead, for clicks on menu buttons. */
    static void mouse(GuiGraphics g, int x, int y, boolean lit, boolean left) {
        g.fill(x, y, x + 9, y + 13, BORDER);
        g.fill(x + 1, y + 1, x + 8, y + 12, WELL);
        g.fill(x + 1, y + 5, x + 8, y + 6, BORDER);
        g.fill(x + 4, y + 1, x + 5, y + 5, BORDER);
        g.fill(x + 1, y + 1, x + 4, y + 5, lit && left ? ACCENT : WELL);
        g.fill(x + 5, y + 1, x + 8, y + 5, lit && !left ? ACCENT : CONTROL);
    }

    /** Corner brackets around a point; {@code r} is the half-size. */
    static void brackets(GuiGraphics g, int cx, int cy, int r, int color) {
        int l = Math.max(2, r / 2);
        for (int sx : new int[]{-1, 1})
            for (int sy : new int[]{-1, 1}) {
                int x = cx + sx * r, y = cy + sy * r;
                g.fill(Math.min(x, x - sx * l), y, Math.max(x, x - sx * l) + 1, y + 1, color);
                g.fill(x, Math.min(y, y - sy * l), x + 1, Math.max(y, y - sy * l) + 1, color);
            }
    }
}
