package com.aetherianartificer.townstead.compat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Renders a {@code townsteadNodeItem} in place of MCA's atlas building sprite.
 *
 * <p>MCA draws building icons from {@code textures/buildings.png} at a per-type
 * {@code (iconU, iconV)} slot. When a Townstead-known type maps that slot to an
 * item (via {@link BuildingIconResolver}), we draw the item instead. The draw
 * choke point differs by MCA version, so this helper is shared by all of them:
 * the legacy {@code BlueprintScreen.drawBuildingIcon} inject and the newer
 * floor-system {@code WidgetUtils.drawBuildingIcon} / {@code
 * BlueprintMapRenderer.drawScaledBuildingIcon} injects.
 */
public final class BuildingIconSwap {
    private BuildingIconSwap() {}

    private static Optional<ItemStack> resolve(int u, int v) {
        Optional<ResourceLocation> itemId = BuildingIconResolver.nodeItemForIconUv(u, v);
        if (itemId.isEmpty() || !BuiltInRegistries.ITEM.containsKey(itemId.get())) {
            return Optional.empty();
        }
        Item item = BuiltInRegistries.ITEM.get(itemId.get());
        if (item == null) {
            return Optional.empty();
        }
        ItemStack stack = new ItemStack(item);
        return stack.isEmpty() ? Optional.empty() : Optional.of(stack);
    }

    /**
     * Draws the node item over the icon at {@code (x, y)}. Mirrors the transform
     * MCA uses for its unscaled atlas blit. Returns {@code true} when an item was
     * drawn, signalling the caller to cancel MCA's atlas draw.
     */
    public static boolean render(GuiGraphics context, int x, int y, int u, int v) {
        Optional<ItemStack> stack = resolve(u, v);
        if (stack.isEmpty()) {
            return false;
        }
        context.pose().pushPose();
        context.pose().translate(x - 6.0, y - 6.0, 0.0);
        context.pose().scale(0.75f, 0.75f, 1.0f);
        context.renderItem(stack.get(), 0, 0);
        context.pose().popPose();
        return true;
    }

    /**
     * Scaled variant matching MCA's {@code drawScaledBuildingIcon}: footprint
     * icons carry a per-layer {@code scale} and floating-point map coordinates.
     */
    public static boolean renderScaled(GuiGraphics context, double x, double y, int u, int v, float scale) {
        Optional<ItemStack> stack = resolve(u, v);
        if (stack.isEmpty()) {
            return false;
        }
        context.pose().pushPose();
        context.pose().translate(x, y, 0.0);
        context.pose().scale(scale, scale, 1.0f);
        context.pose().translate(-6.0, -6.0, 0.0);
        context.pose().scale(0.75f, 0.75f, 1.0f);
        context.renderItem(stack.get(), 0, 0);
        context.pose().popPose();
        return true;
    }
}
