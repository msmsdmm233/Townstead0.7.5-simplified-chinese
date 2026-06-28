package com.aetherianartificer.townstead.mixin;

import com.aetherianartificer.townstead.root.Buoyancy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Player counterpart to {@link LivingEntityAffectedByFluidsMixin}. {@code Player} overrides
 * {@code isAffectedByFluids} (returns {@code !flying}), so the LivingEntity injection never fires
 * for a player; this applies the same fluid nullification on the player path, on both sides so the
 * owning client predicts the land-movement underwater and does not rubber-band. Same hook both
 * branches (SRG name on Forge).
 */
@Mixin(net.minecraft.world.entity.player.Player.class)
public abstract class PlayerAffectedByFluidsMixin {

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
