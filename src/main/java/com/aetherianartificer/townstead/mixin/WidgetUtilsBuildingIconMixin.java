package com.aetherianartificer.townstead.mixin;

//? if neoforge {
import com.aetherianartificer.townstead.compat.BuildingIconSwap;
import net.conczin.mca.client.gui.widget.WidgetUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * New floor-system MCA choke point for grouped (POI) building icons. MCA moved
 * icon drawing out of {@code BlueprintScreen.drawBuildingIcon} into this static
 * helper; we swap in a {@code townsteadNodeItem} here when the icon's
 * {@code (u, v)} slot maps to one.
 *
 * <p>Applied only when the runtime MCA exposes the new API. See
 * {@code TownsteadMixinPlugin} and the legacy counterpart
 * {@code BlueprintScreenLegacyIconMixin}.
 */
@Mixin(WidgetUtils.class)
public class WidgetUtilsBuildingIconMixin {
    @Inject(method = "drawBuildingIcon", remap = false, at = @At("HEAD"), cancellable = true)
    private static void townstead$swapBuildingIcon(GuiGraphics context, ResourceLocation texture,
            int x, int y, int u, int v, CallbackInfo ci) {
        if (BuildingIconSwap.render(context, x, y, u, v)) {
            ci.cancel();
        }
    }
}
//?} else {
/*public abstract class WidgetUtilsBuildingIconMixin {}
*///?}
