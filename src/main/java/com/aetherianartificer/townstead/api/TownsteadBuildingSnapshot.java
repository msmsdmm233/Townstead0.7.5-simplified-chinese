package com.aetherianartificer.townstead.api;

/** Serializable read-only MCA building state for one building. */
public record TownsteadBuildingSnapshot(
        int id,
        int villageId,
        String type,
        int size,
        int centerX,
        int centerY,
        int centerZ,
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ
) {}
