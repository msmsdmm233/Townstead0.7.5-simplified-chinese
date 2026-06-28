package com.aetherianartificer.townstead.enclosure;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fence-like block checks shared by enclosure detection. Tags are the data-pack
 * friendly path; block classes catch modded fences that behave like fences but
 * forgot to join the vanilla tags.
 */
public final class EnclosureBlocks {
    private EnclosureBlocks() {}

    public static boolean isFence(BlockState state) {
        return state != null && (state.is(BlockTags.FENCES) || state.getBlock() instanceof FenceBlock);
    }

    public static boolean isFenceGate(BlockState state) {
        return state != null && (state.is(BlockTags.FENCE_GATES) || state.getBlock() instanceof FenceGateBlock);
    }

    public static boolean isWall(BlockState state) {
        return state != null && (state.is(BlockTags.WALLS) || state.getBlock() instanceof WallBlock);
    }

    public static boolean isPerimeter(BlockState state) {
        return isFence(state) || isFenceGate(state) || isWall(state);
    }
}
