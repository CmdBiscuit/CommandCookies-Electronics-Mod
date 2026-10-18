package net.mcreator.commandcookieselectronicmod.mixin;

import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Sodium already centers terrain on the supplied camera and replaces setupRender. */
public final class CameraMixinPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.endsWith(".CameraTerrainCenterMixin")) return true;
        var mods = FMLLoader.getLoadingModList();
        return mods == null || (mods.getModFileById("sodium") == null
                && mods.getModFileById("embeddium") == null);
    }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass,
                                  String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass,
                                   String mixinClassName, IMixinInfo mixinInfo) {}
}
