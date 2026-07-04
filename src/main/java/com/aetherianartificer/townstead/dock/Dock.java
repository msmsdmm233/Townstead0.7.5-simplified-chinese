package com.aetherianartificer.townstead.dock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

/**
 * A detected dock/wharf structure. Bounds include a one-block horizontal
 * margin around the plank surface plus ~2 blocks above, so decorations sitting
 * just next to or above the deck fall inside the box.
 *
 * A dock is defined by its structure (planks, water, lights, railings) plus a
 * fisherman's barrel anchored on or beside the deck — see {@link DockScanner}
 * for the requirements ladder. The barrel is the keystone: a deck without one
 * is just shoreline flooring and won't register. The fisherman task decides
 * separately whether to actually work a given dock.
 *
 * plankCount is retained for debug output and future UI "you have X planks,
 * need Y" hints.
 */
public record Dock(BoundingBox bounds, int plankCount, int tier) {
    public boolean contains(BlockPos pos) {
        return pos.getX() >= bounds.minX() && pos.getX() <= bounds.maxX()
                && pos.getY() >= bounds.minY() && pos.getY() <= bounds.maxY()
                && pos.getZ() >= bounds.minZ() && pos.getZ() <= bounds.maxZ();
    }

    /**
     * Horizontal centroid of the dock bounds, Y-anchored just above the deck
     * so audio-visual recognition effects hover over the pier rather than
     * under it. Used by the recognition hooks in {@link DockScanner}.
     */
    public Vec3 centerVec() {
        double cx = (bounds.minX() + bounds.maxX() + 1) / 2.0;
        double cz = (bounds.minZ() + bounds.maxZ() + 1) / 2.0;
        return new Vec3(cx, bounds.maxY() - 1.0, cz);
    }
}
