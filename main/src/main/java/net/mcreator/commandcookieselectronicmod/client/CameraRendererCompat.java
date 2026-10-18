package net.mcreator.commandcookieselectronicmod.client;

import java.lang.reflect.Method;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.fml.ModList;

/** Camera scene packets finish lighting without passing through ClientPacketListener. */
public final class CameraRendererCompat {
    private record LightingBridge(Method tracker, Method addStatus, int lightFlag) {}
    private static LightingBridge lighting;
    private static boolean initialized;

    private static LightingBridge findLightingBridge() {
        if (!ModList.get().isLoaded("sodium") && !ModList.get().isLoaded("embeddium")) return null;
        for (String prefix : new String[]{"net.caffeinemc.mods.sodium.client",
                "org.embeddedt.embeddium.impl", "me.jellysquid.mods.sodium.client"}) {
            try {
                var holder = Class.forName(prefix + ".render.chunk.map.ChunkTrackerHolder");
                var tracker = holder.getMethod("get", ClientLevel.class);
                var status = Class.forName(prefix + ".render.chunk.map.ChunkStatus");
                return new LightingBridge(tracker,
                        tracker.getReturnType().getMethod("onChunkStatusAdded", int.class, int.class, int.class),
                        status.getField("FLAG_HAS_LIGHT_DATA").getInt(null));
            } catch (ClassNotFoundException ignored) {
                // Renderer versions use different package names.
            } catch (ReflectiveOperationException | LinkageError failure) {
                throw new IllegalStateException("Camera renderer lighting API is unavailable", failure);
            }
        }
        throw new IllegalStateException("Camera renderer lighting API was not found");
    }

    public static void lightReady(ClientLevel level, int x, int z) {
        if (!initialized) {
            lighting = findLightingBridge();
            initialized = true;
        }
        if (lighting == null) return;
        try {
            var tracker = lighting.tracker().invoke(null, level);
            lighting.addStatus().invoke(tracker, x, z, lighting.lightFlag());
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot notify camera renderer about lighting", failure);
        }
    }

    private CameraRendererCompat() {}
}
