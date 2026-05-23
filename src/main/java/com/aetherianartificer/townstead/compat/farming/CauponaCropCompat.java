package com.aetherianartificer.townstead.compat.farming;

import com.aetherianartificer.townstead.compat.ModCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Caupona compat. Caupona's snail blocks ({@code snail_block}, {@code snail_bait}) extend
 * {@code FruitBlock}, which extends vanilla {@code CropBlock} — so our generic class-based
 * detection flags their BlockItems as plantable seeds and they end up in the Field Post palette
 * (the snail block's loot table drops {@code plump_snail} at max age, so it even labels as
 * "Plump Snail"). They aren't real farmland crops, though: snails survive only on the
 * {@code SNAIL_GROWABLE_ON}-tagged block beneath them and grow by eating food blocks placed
 * around them, none of which the Field Post can set up. We exclude them so they never appear.
 */
public final class CauponaCropCompat implements FarmerCropCompat {
    private static final String MOD_ID = "caupona";

    @Override
    public String modId() { return MOD_ID; }

    @Override
    public boolean isSeed(ItemStack stack) { return false; }

    @Override
    public boolean excludeAsSeed(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation key = stack.getItem().builtInRegistryHolder().key().location();
        // Both are CropBlock subclasses but are not viable Field Post crops.
        if (ModCompat.matchesLoadedModPath(key, MOD_ID, "snail_block")) return true; // grows snails / "Plump Snail"
        return ModCompat.matchesLoadedModPath(key, MOD_ID, "snail_bait"); // ripens into a snail block
    }

    @Override
    public boolean shouldPartialHarvest(BlockState state) { return false; }

    @Override
    public List<ItemStack> doPartialHarvest(ServerLevel level, BlockPos pos, BlockState state) { return List.of(); }

    @Override
    public boolean isExistingFarmSoil(ServerLevel level, BlockPos pos) { return false; }

    @Override
    public boolean isPlantableSpot(ServerLevel level, BlockPos pos) { return false; }
}
