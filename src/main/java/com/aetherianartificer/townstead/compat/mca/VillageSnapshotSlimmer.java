package com.aetherianartificer.townstead.compat.mca;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * Trims the wire payload of MCA's {@code GetVillageResponse} by stripping the
 * per-position {@code x/y/z} ints from every building's {@code blocks2} entry.
 * Server-side {@code validateBlocks} consults the in-memory {@code blocks} map
 * and never re-reads the wire NBT, so dropping the coordinates does not affect
 * validation. The client never inspects individual {@link net.minecraft.core.BlockPos}
 * coordinates either; {@code BlueprintScreen} only calls
 * {@code List<BlockPos>.size()} for the hover tooltip count, so we keep the
 * list lengths intact by substituting empty {@link CompoundTag}s for the real
 * position records.
 *
 * <p>Why: with Townstead 0.6.0's {@code DockBuildingSync} and {@code
 * EnclosureBuildingSync} writing every plank / every interior column block
 * into {@code blocks2}, villages with a handful of pens easily blow past the
 * 2 MiB {@link net.minecraft.nbt.NbtAccounter} cap on the client decode path.
 * Trimming position payload server-side fixes the bloat at the source.</p>
 *
 * <p>The result is a deep-copied {@link CompoundTag}; the caller never mutates
 * the live {@code Village} snapshot used by the running server.</p>
 */
public final class VillageSnapshotSlimmer {
    private VillageSnapshotSlimmer() {}

    public static CompoundTag slim(CompoundTag villageData) {
        CompoundTag copy = new CompoundTag();
        for (String key : villageData.getAllKeys()) {
            if ("buildings".equals(key) && villageData.contains("buildings", Tag.TAG_LIST)) {
                copy.put("buildings", slimBuildings(villageData.getList("buildings", Tag.TAG_COMPOUND)));
                continue;
            }
            Tag value = villageData.get(key);
            if (value != null) copy.put(key, value.copy());
        }
        return copy;
    }

    private static ListTag slimBuildings(ListTag source) {
        ListTag buildings = new ListTag();
        for (int i = 0; i < source.size(); i++) {
            buildings.add(slimBuilding(source.getCompound(i)));
        }
        return buildings;
    }

    private static CompoundTag slimBuilding(CompoundTag source) {
        CompoundTag building = new CompoundTag();
        for (String key : source.getAllKeys()) {
            if ("blocks2".equals(key) && source.contains("blocks2", Tag.TAG_COMPOUND)) {
                building.put("blocks2", slimBlocks2(source.getCompound("blocks2")));
                continue;
            }
            Tag value = source.get(key);
            if (value != null) building.put(key, value.copy());
        }
        return building;
    }

    private static CompoundTag slimBlocks2(CompoundTag blocks2) {
        CompoundTag trimmed = new CompoundTag();
        for (String blockKey : blocks2.getAllKeys()) {
            ListTag positions = blocks2.getList(blockKey, Tag.TAG_COMPOUND);
            trimmed.put(blockKey, placeholders(positions.size()));
        }
        return trimmed;
    }

    private static ListTag placeholders(int count) {
        ListTag empties = new ListTag();
        for (int i = 0; i < count; i++) {
            empties.add(new CompoundTag());
        }
        return empties;
    }
}
