package net.mcreator.commandcookieselectronicmod.nes;

import java.util.ArrayDeque;

/** Bounded ordered inputs. Bursts carry into later server ticks instead of rolling prediction back. */
public final class NesInputQueue {
    public record Input(int sequence, int action, int value, int generation) {}
    private final ArrayDeque<Input> entries = new ArrayDeque<>();
    private int received;

    public int received() { return received; }
    public void reset() { entries.clear(); received = 0; }

    public boolean offer(int sequence, int action, int value, int generation) {
        if (action < 0 || action > 9 || sequence <= received || sequence > received + 128 || entries.size() >= 64) return false;
        entries.add(new Input(sequence, action, value, generation));
        received = sequence;
        return true;
    }

    public Input poll(int generation, int usedMask) {
        var next = entries.peek();
        if (next == null) return null;
        // Inputs for a piece that has already landed are acknowledged without touching its successor.
        boolean stale = next.action <= 4 && next.generation != generation;
        if (!stale && ((usedMask & (1 << next.action)) != 0 || Integer.bitCount(usedMask) >= 3)) return null;
        return entries.remove();
    }
}
