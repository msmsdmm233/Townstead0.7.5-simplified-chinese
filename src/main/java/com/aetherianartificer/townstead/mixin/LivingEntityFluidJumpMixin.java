package com.aetherianartificer.townstead.mixin;

import com.aetherianartificer.townstead.root.Buoyancy;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Restores a normal ground jump for a {@code buoyancy} gene's bearer while it stands in a
 * nullified fluid. Vanilla gates its whole jump block on {@code isAffectedByFluids()}, which
 * {@link LivingEntityAffectedByFluidsMixin} forces false there, so without this a sinking entity
 * walks the bottom but can never jump. Mirrors vanilla: if it is holding jump and on the ground,
 * jump from the ground. Runs on both sides of {@code aiStep} so the owning client predicts it.
 * 1.20.1 Forge SRG: {@code f_20899_} jumping, {@code m_6135_} jumpFromGround, {@code m_8107_} aiStep.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityFluidJumpMixin {

    //? if neoforge {
    @Shadow protected boolean jumping;
    @Shadow protected abstract void jumpFromGround();
    //?} else {
    /*@Shadow(remap = false) protected boolean f_20899_;
    @Shadow(remap = false) protected abstract void m_6135_();
    *///?}

    private boolean townstead$jumping() {
        //? if neoforge {
        return this.jumping;
        //?} else {
        /*return this.f_20899_;
        *///?}
    }

    private void townstead$jumpFromGround() {
        //? if neoforge {
        this.jumpFromGround();
        //?} else {
        /*this.m_6135_();
        *///?}
    }

    //? if neoforge {
    @Inject(method = "aiStep", at = @At("TAIL"))
    //?} else {
    /*@Inject(method = "m_8107_", at = @At("TAIL"), remap = false)
    *///?}
    private void townstead$fluidJump(CallbackInfo ci) {
        if (!townstead$jumping()) return;
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.onGround() && Buoyancy.ignoresCurrentFluid(self)) {
            townstead$jumpFromGround();
        }
    }
}
