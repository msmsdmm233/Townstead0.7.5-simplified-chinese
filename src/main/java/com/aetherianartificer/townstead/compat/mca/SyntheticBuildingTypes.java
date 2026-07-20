package com.aetherianartificer.townstead.compat.mca;

import com.aetherianartificer.townstead.enclosure.EnclosureTypeIndex;

/**
 * Identifies MCA {@code Building} types that Townstead synthesizes itself
 * ({@code DockBuildingSync} / {@code EnclosureBuildingSync}) rather than letting
 * MCA discover through its flood-fill scan.
 *
 * <p>These types are open-air and made of generic structural materials, so they
 * must not participate in MCA's native block-claiming or auto-typing: their
 * requirement tags ({@code #townstead:dock_surfaces}, {@code #minecraft:fences})
 * would otherwise promote planks/stone/fences to globally trackable blocks, and
 * every ordinary house scan would then record its own walls (see
 * {@code BuildingTypeSyntheticBlockMixin}). Their lifecycle is owned by the
 * synthesizers, which set {@code isTypeForced} and rely on
 * {@code BuildingValidateOpenAirMixin} for validation.
 */
public final class SyntheticBuildingTypes {
    private SyntheticBuildingTypes() {}

    public static boolean isSynthetic(String buildingType) {
        return buildingType != null
                && (buildingType.startsWith("dock_") || EnclosureTypeIndex.isEnclosureType(buildingType));
    }
}
