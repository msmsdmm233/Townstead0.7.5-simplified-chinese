package com.aetherianartificer.townstead.mixin;

import com.aetherianartificer.townstead.root.Buoyancy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes a {@code buoyancy} gene's bearer treat a nullified fluid as not-there: forces
 * {@code isAffectedByFluids} false while it stands in that fluid, so vanilla {@code travel}
 * runs the land-movement branch instead of swimming (sinks under gravity, walks the bottom,
 * no drag or current). Covers villagers and other mobs; the {@code Player} override is handled
 * by {@link PlayerAffectedByFluidsMixin}. Same hook both branches (SRG name on Forge).
 */
@Mixin(net.minecraft.world.entity.LivingEntity.class)
public abstract class LivingEntityAffectedByFluidsMixin {

    //? if neoforge {
    @Inject(method = "isAffectedByFluids", at = @At("RETURN"), cancellable = true)
    //?} else {
    /*@Inject(method = "m_6129_", at = @At("RETURN"), cancellable = true, remap = false)
    *///?}
    private void townstead$nullifyFluid(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        if (Buoyancy.ignoresCurrentFluid((net.minecraft.world.entity.LivingEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
