package com.aetherianartificer.townstead;

import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Applies the building-icon-swap mixins that match the running MCA generation.
 *
 * <p>MCA's floor-system rebuild removed {@code BlueprintScreen.drawBuildingIcon}
 * and moved icon drawing into {@code WidgetUtils}/{@code BlueprintMapRenderer}.
 * Townstead compiles against that newer API but still runs on the older 7.7.x
 * builds, so the three icon mixins are mutually exclusive:
 *
 * <ul>
 *   <li>new API → {@code WidgetUtilsBuildingIconMixin} + {@code BlueprintMapRendererIconMixin}</li>
 *   <li>legacy API → {@code BlueprintScreenLegacyIconMixin}</li>
 * </ul>
 *
 * <p>Gating by presence of {@code BlueprintMapRenderer} (new-only) also keeps the
 * legacy mixin from resolving a method that no longer exists (the crash that
 * motivated this split) and keeps the new mixins from resolving a class that does
 * not exist on old MCA. Detection uses a classpath resource lookup so no MCA
 * class is loaded — and therefore frozen — before its own transformers run.
 */
public class TownsteadMixinPlugin implements IMixinConfigPlugin {
    private static final String NEW_API_MARKER = "net/conczin/mca/client/gui/BlueprintMapRenderer.class";

    private Boolean newApi;

    private boolean isNewApi() {
        if (newApi == null) {
            newApi = TownsteadMixinPlugin.class.getClassLoader().getResource(NEW_API_MARKER) != null;
        }
        return newApi;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("WidgetUtilsBuildingIconMixin")
                || mixinClassName.endsWith("BlueprintMapRendererIconMixin")) {
            return isNewApi();
        }
        if (mixinClassName.endsWith("BlueprintScreenLegacyIconMixin")) {
            return !isNewApi();
        }
        // The floor-system map renderer rebuilds building geometry from real block
        // positions (Building.blocks2), so the wire snapshot must keep them. On
        // legacy MCA the client only read List<BlockPos>.size(), so slimming the
        // positions to save bandwidth was safe there. Applying the slimmer on the
        // new build strips the coordinates the renderer needs → empty map (no icons
        // or outlines) + "Not a list: {}" decode spam. Legacy only.
        if (mixinClassName.endsWith("GetVillageResponseSlimPayloadMixin")) {
            return !isNewApi();
        }
        return true;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName,
            IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName,
            IMixinInfo mixinInfo) {
    }
}
