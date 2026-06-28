package com.aetherianartificer.townstead.item;

import net.minecraft.world.item.ItemStack;

/**
 * Reads the dye color of a scarf {@link ItemStack}, cross-version. A scarf is greyscale and tinted by this
 * color (inventory icon + the worn 3D model), like leather armor. Undyed returns white, so the texture
 * shows as authored. Storage differs by version: the {@code DYED_COLOR} data component on 1.21.1, the
 * leather-style {@code display.color} NBT on 1.20.1.
 */
public final class ScarfColor {

    /** Undyed tint: white leaves the greyscale texture as drawn. */
    public static final int DEFAULT = 0xFFFFFF;

    private ScarfColor() {}

    public static int get(ItemStack stack) {
        //? if >=1.21 {
        net.minecraft.world.item.component.DyedItemColor c =
                stack.get(net.minecraft.core.component.DataComponents.DYED_COLOR);
        return c != null ? (0xFFFFFF & c.rgb()) : DEFAULT;
        //?} else {
        /*net.minecraft.nbt.CompoundTag display = stack.getTagElement("display");
        if (display != null && display.contains("color", 99)) return 0xFFFFFF & display.getInt("color");
        return DEFAULT;
        *///?}
    }
}
