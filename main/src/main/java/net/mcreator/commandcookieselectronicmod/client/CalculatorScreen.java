package net.mcreator.commandcookieselectronicmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.mcreator.commandcookieselectronicmod.calculator.CalculatorEngine;
import org.lwjgl.glfw.GLFW;

public final class CalculatorScreen extends Screen {
    private final CalculatorEngine calculator = new CalculatorEngine();
    private int left, top, panelWidth, panelHeight, keyHeight, stride;

    public CalculatorScreen() { super(Component.literal("Calculator")); }

    @Override protected void init() {
        panelWidth = Math.min(220, width - 12);
        keyHeight = Math.max(13, Math.min(23, (height - 108) / 5));
        stride = keyHeight + 3; panelHeight = 96 + stride * 5;
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2;
        int keyWidth = (panelWidth - 25) / 4;
        addRenderableWidget(new Key(left + panelWidth - 24, top + 5, 17, 15, "x", "Close calculator", 0xFF41474D, this::onClose));
        String[] memories = {"MC", "MR", "M+", "M-"};
        String[] tips = {"Clear memory", "Recall memory", "Add display to memory", "Subtract display from memory"};
        for (int col = 0; col < 4; col++) {
            String command = memories[col];
            addRenderableWidget(new Key(left + 8 + col * (keyWidth + 3), top + 69, keyWidth, 17, command, tips[col], 0xFF39434B, () -> press(command)));
        }
        String[][] commands = {{"C", "SIGN", "%", "/"}, {"7", "8", "9", "*"}, {"4", "5", "6", "-"}, {"1", "2", "3", "+"}, {"0", ".", "BACK", "="}};
        for (int row = 0; row < 5; row++) for (int col = 0; col < 4; col++) {
            String command = commands[row][col];
            String label = switch (command) { case "SIGN" -> "\u00b1"; case "/" -> "\u00f7"; case "*" -> "\u00d7"; case "BACK" -> "DEL"; default -> command; };
            String tip = switch (command) { case "C" -> "Clear calculation (Delete)"; case "SIGN" -> "Change sign"; case "%" -> "Percent"; case "BACK" -> "Delete last digit (Backspace)"; case "=" -> "Equals (Enter)"; default -> label; };
            int color = command.equals("C") ? 0xFF71514F : command.equals("=") ? 0xFF537B85 : col == 3 ? 0xFF4B5B65 : 0xFF42494F;
            addRenderableWidget(new Key(left + 8 + col * (keyWidth + 3), top + 90 + row * stride, keyWidth, keyHeight, label, tip, color, () -> press(command)));
        }
    }

    private void press(String command) { calculator.press(command); }

    @Override public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0x990E1116);
        g.fill(left + 3, top + 3, left + panelWidth + 3, top + panelHeight + 3, 0x99000000);
        g.fill(left, top, left + panelWidth, top + panelHeight, 0xFF292E34);
        g.fill(left + 1, top + 1, left + panelWidth - 1, top + 2, 0xFF646B71);
        g.fill(left + 1, top + panelHeight - 2, left + panelWidth - 1, top + panelHeight - 1, 0xFF191D22);
        for (int y = 4; y < panelHeight - 3; y += 8) for (int x = 3; x < panelWidth - 3; x += 9)
            g.fill(left + x, top + y, left + x + 1, top + y + 1, 0xFF31373D);
        g.drawString(font, "CALCULATOR", left + 9, top + 9, 0xFFCDD4D7, false);
        g.fill(left + 7, top + 26, left + panelWidth - 7, top + 65, 0xFF161D1A);
        g.fill(left + 9, top + 28, left + panelWidth - 9, top + 63, 0xFFA4B193);
        String history = font.plainSubstrByWidth(calculator.history(), panelWidth - 40);
        g.drawString(font, history, left + 13, top + 31, 0xFF455440, false);
        if (calculator.hasMemory()) g.drawString(font, "M", left + panelWidth - 20, top + 31, 0xFF344730, false);
        String value = calculator.display();
        float scale = Math.min(1.65f, (panelWidth - 28f) / Math.max(1, font.width(value)));
        g.pose().pushPose();
        g.pose().translate(left + panelWidth - 13, top + 45, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, value, -font.width(value), 0, calculator.isError() ? 0xFF813B36 : 0xFF223429, false);
        g.pose().popPose();
    }

    @Override public boolean charTyped(char character, int modifiers) {
        if (character >= '0' && character <= '9' || ".+-*/%=".indexOf(character) >= 0) { press(String.valueOf(character)); return true; }
        if (character == 'c' || character == 'C') { press("C"); return true; }
        return super.charTyped(character, modifiers);
    }
    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { press("="); return true; }
        if (key == GLFW.GLFW_KEY_BACKSPACE) { press("BACK"); return true; }
        if (key == GLFW.GLFW_KEY_DELETE) { press("C"); return true; }
        return super.keyPressed(key, scanCode, modifiers);
    }
    @Override public boolean isPauseScreen() { return false; }

    private static final class Key extends Button {
        private final int color;
        Key(int x, int y, int w, int h, String label, String tip, int color, Runnable action) {
            super(x, y, w, h, Component.literal(label), button -> action.run(), DEFAULT_NARRATION);
            this.color = color; setTooltip(Tooltip.create(Component.literal(tip)));
        }
        @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            g.fill(x, y, x + w, y + h, 0xFF1B2025);
            g.fill(x + 1, y + 1, x + w - 1, y + h - 2, color);
            g.fill(x + 1, y + 1, x + w - 1, y + 2, isHoveredOrFocused() ? 0xFFB0BDBF : 0xFF6B747C);
            if (isHoveredOrFocused()) g.renderOutline(x, y, w, h, 0xFFB4CDD0);
            g.drawCenteredString(Minecraft.getInstance().font, getMessage(), x + w / 2, y + (h - 8) / 2, 0xFFE8ECE6);
        }
    }
}
