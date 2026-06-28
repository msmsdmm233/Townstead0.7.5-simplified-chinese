package com.aetherianartificer.townstead.mixin;

import com.aetherianartificer.townstead.root.Buoyancy;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps a {@code buoyancy} gene's bearer out of the swim (prone) state while it stands in a
 * nullified fluid: it treats the fluid as solid ground, so it walks the bottom rather than
 * sprint-swimming. Without this, sprinting in water flips the entity into swimming, which dodges
 * the land-movement path (and the {@code wade} slowdown that rides {@code movement_speed}). Targets
 * {@code Entity#updateSwimming}, which {@code Player} reaches via {@code super}, so it covers players
 * and mobs. Both sides so the owning client predicts the same state. SRG names on Forge.
 */
@Mixin(Entity.class)
public abstract class EntityNoSwimMixin {

    //? if neoforge {
    @Inject(method = "updateSwimming", at = @At("TAIL"))
    //?} else {
    /*@Inject(method = "m_5844_", at = @At("TAIL"), remap = false)
    *///?}
    private void townstead$noSwimWhileSinking(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof LivingEntity living && Buoyancy.ignoresCurrentFluid(living)) {
            self.setSwimming(false);
        }
    }
}
