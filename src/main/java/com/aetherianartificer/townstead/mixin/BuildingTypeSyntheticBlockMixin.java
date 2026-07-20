package com.aetherianartificer.townstead.mixin;

import com.aetherianartificer.townstead.compat.mca.SyntheticBuildingTypes;
import net.conczin.mca.resources.data.BuildingType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops Townstead's synthesized building types ({@code dock_*}, enclosures) from
 * claiming their generic structural blocks through MCA's block-matching.
 *
 * <p>MCA decides whether a block is worth recording during a building scan with
 * {@code Building.isBuildingBlock}, which asks <em>every</em> registered
 * {@link BuildingType} whether it {@code matchesBlock}. Because docks require
 * {@code #townstead:dock_surfaces} (planks, stone, stairs, slabs, bricks...) and
 * pens require {@code #minecraft:fences}, those materials would be promoted to
 * globally trackable — so a plain MCA house scan starts recording its own walls,
 * and the blueprint tooltip lists "247 x Oak Planks" instead of just "1 x Bed".
 *
 * <p>Returning {@code false} for these types removes them from that global set.
 * It is safe because their lifecycle never depends on native matching: they are
 * synthesized with {@code isTypeForced}, validated by
 * {@code BuildingValidateOpenAirMixin} (which calls {@code validateBlocks}
 * directly — that compares stored positions to world block ids, not
 * {@code matchesBlock}), and MCA's only other {@code matchesBlock} caller
 * ({@code VillageManager}) is gated behind {@code grouped()}, which these types
 * are not. Townstead's own catalog "Needs" panel reads {@code getGroups()}
 * directly, so it is unaffected.
 */
@Mixin(BuildingType.class)
public abstract class BuildingTypeSyntheticBlockMixin {
    @Inject(method = "matchesBlock(Lnet/minecraft/world/level/block/state/BlockState;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void townstead$skipSyntheticState(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (SyntheticBuildingTypes.isSynthetic(((BuildingType) (Object) this).name())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "matchesBlock(Lnet/minecraft/resources/ResourceLocation;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void townstead$skipSyntheticId(ResourceLocation blockId, CallbackInfoReturnable<Boolean> cir) {
        if (SyntheticBuildingTypes.isSynthetic(((BuildingType) (Object) this).name())) {
            cir.setReturnValue(false);
        }
    }
}
